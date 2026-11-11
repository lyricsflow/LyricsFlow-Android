package com.lyricsflow.player.code.rpc

import android.os.Build
import android.util.Base64
import android.util.Log
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.artistsName
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONArray
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.util.concurrent.TimeUnit

/**
 * Discord Rich Presence ("LyricsFlow Mobile") & Account Linking Manager
 * Ported from `player-core/src/main/discordRpc.js` & BitChord's `DiscordRPC.kt` + `SuperProperties.kt`.
 * - APPLICATION_ID = "1505997500341944380"
 * - Activity Name = "LyricsFlow Mobile" (Type 2: Listening to LyricsFlow Mobile)
 * - Animated GIF cover art: `https://api.trylyricsflow.com/animatedart/gif?song=${trackId}&for_rpc=1`
 * - Buttons: "Listen on LyricsFlow" & "Play on Apple Music"
 */
@Stable
object DiscordRpcManager {
    const val APPLICATION_ID = "1505997500341944380"
    const val ACTIVITY_NAME = "LyricsFlow Mobile"
    private const val GATEWAY_URL = "wss://gateway.discord.gg/?v=10&encoding=json"
    private const val MMKV_ID = "lyricsflow_discord_rpc"
    private const val KEY_TOKEN = "discord_user_token"
    private const val KEY_USERNAME = "discord_username"
    private const val KEY_AVATAR = "discord_avatar"
    private const val KEY_ENABLED = "discord_rpc_enabled"

    private val mmkv by lazy { MMKV.mmkvWithID(MMKV_ID) }
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    val isEnabled = mutableStateOf(mmkv.decodeBool(KEY_ENABLED, true))
    val discordUsername = mutableStateOf(mmkv.decodeString(KEY_USERNAME).orEmpty())
    val discordAvatarUrl = mutableStateOf(mmkv.decodeString(KEY_AVATAR).orEmpty())
    val isConnected = mutableStateOf(false)

    private var webSocket: WebSocket? = null
    private var heartbeatJob: Job? = null
    private var lastSequence: Int? = null
    private var pendingActivityPayload: JSONObject? = null

    private val httpClient by lazy {
        OkHttpClient.Builder()
            .connectTimeout(12, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS)
            .build()
    }

    fun hasToken(): Boolean = mmkv.decodeString(KEY_TOKEN).orEmpty().isNotBlank()

    fun setRpcEnabled(enabled: Boolean) {
        isEnabled.value = enabled
        mmkv.encode(KEY_ENABLED, enabled)
        if (!enabled) {
            clearActivity()
            disconnect()
        } else if (hasToken()) {
            connectIfNeeded()
        }
    }

    fun saveDiscordToken(token: String) {
        val clean = token.trim().removeSurrounding("\"")
        if (clean.isBlank()) return
        mmkv.encode(KEY_TOKEN, clean)
        isEnabled.value = true
        mmkv.encode(KEY_ENABLED, true)
        disconnect()
        connectIfNeeded()
    }

    fun unlinkDiscord() {
        clearActivity()
        disconnect()
        mmkv.removeValueForKey(KEY_TOKEN)
        mmkv.removeValueForKey(KEY_USERNAME)
        mmkv.removeValueForKey(KEY_AVATAR)
        discordUsername.value = ""
        discordAvatarUrl.value = ""
        isConnected.value = false
    }

    fun updateNowPlayingActivity(
        song: LyricsflowMediaItem?,
        isPlaying: Boolean,
        positionMs: Long,
        durationMs: Long
    ) {
        if (!isEnabled.value || !hasToken()) return
        if (song == null || song.title.isNullOrBlank()) {
            clearActivity()
            return
        }

        scope.launch {
            val title = song.title.orEmpty().trim().let { if (it.length == 1) "$it\u200B" else it }.take(128)
            val rawArtist = song.artistsName.orEmpty().trim().ifBlank { "Unknown Artist" }
            val stateText = if (isPlaying) rawArtist else "Paused • $rawArtist"
            val albumName = song.album.orEmpty().trim()
            val codecLabel = when {
                runCatching { SettingsLibrary.EnableDolbyAtmos }.getOrDefault(false) -> "Dolby Atmos"
                runCatching { SettingsLibrary.EnableLosslessStreaming }.getOrDefault(false) -> "ALAC"
                else -> "AAC"
            }
            val largeText = when {
                albumName.isNotBlank() -> "$albumName • $codecLabel"
                else -> "LyricsFlow • $codecLabel"
            }.take(128)

            val trackId = song.mediaId?.trim().orEmpty()
            val isNumericSongId = trackId.isNotEmpty() && trackId.all { it.isDigit() }

            val rawImageUrl = if (isNumericSongId) {
                "https://api.trylyricsflow.com/animatedart/gif?song=$trackId&for_rpc=1"
            } else {
                val thumbStr = song.thumb?.toString().orEmpty()
                if (thumbStr.startsWith("http")) thumbStr else "https://trylyricsflow.com/images/icon.png"
            }

            val discordExternalImage = resolveExternalAsset(rawImageUrl)

            val activity = JSONObject().apply {
                put("name", ACTIVITY_NAME)
                put("application_id", APPLICATION_ID)
                put("type", 2) // Listening to LyricsFlow Mobile
                put("details", title)
                put("state", stateText.take(128))
                val assets = JSONObject().apply {
                    if (discordExternalImage != null) {
                        put("large_image", discordExternalImage)
                    }
                    put("large_text", largeText)
                    put("small_text", if (isPlaying) ACTIVITY_NAME else "Paused")
                }
                put("assets", assets)

                if (isPlaying && durationMs > 0L) {
                    val now = System.currentTimeMillis()
                    val startTs = now - positionMs.coerceAtLeast(0L)
                    val endTs = startTs + durationMs
                    put(
                        "timestamps",
                        JSONObject().apply {
                            put("start", startTs)
                            put("end", endTs)
                        }
                    )
                }

                val buttons = JSONArray()
                val metadataUrls = JSONArray()
                if (isNumericSongId) {
                    buttons.put("Listen on LyricsFlow")
                    metadataUrls.put("https://trylyricsflow.com/song/$trackId")
                    buttons.put("Play on Apple Music")
                    metadataUrls.put("https://music.apple.com/song/$trackId")
                } else {
                    buttons.put("Get LyricsFlow")
                    metadataUrls.put("https://trylyricsflow.com")
                }
                put("buttons", buttons)
                put("metadata", JSONObject().put("button_urls", metadataUrls))
            }

            val presencePayload = JSONObject().apply {
                put("op", 3)
                put(
                    "d",
                    JSONObject().apply {
                        put("since", 0)
                        put("activities", JSONArray().put(activity))
                        put("status", "online")
                        put("afk", false)
                    }
                )
            }

            pendingActivityPayload = presencePayload
            if (isConnected.value && webSocket != null) {
                webSocket?.send(presencePayload.toString())
            } else {
                connectIfNeeded()
            }
        }
    }

    fun clearActivity() {
        pendingActivityPayload = null
        if (!isConnected.value || webSocket == null) return
        val clearPayload = JSONObject().apply {
            put("op", 3)
            put(
                "d",
                JSONObject().apply {
                    put("since", 0)
                    put("activities", JSONArray())
                    put("status", "online")
                    put("afk", false)
                }
            )
        }
        runCatching { webSocket?.send(clearPayload.toString()) }
    }

    private fun connectIfNeeded() {
        val token = mmkv.decodeString(KEY_TOKEN).orEmpty()
        if (token.isBlank() || !isEnabled.value || webSocket != null) return

        val request = Request.Builder()
            .url(GATEWAY_URL)
            .header("User-Agent", "Discord-Android/212009;RNA")
            .build()

        webSocket = httpClient.newWebSocket(request, object : WebSocketListener() {
            override fun onMessage(webSocket: WebSocket, text: String) {
                runCatching {
                    val json = JSONObject(text)
                    val op = json.optInt("op", -1)
                    if (!json.isNull("s")) {
                        lastSequence = json.optInt("s")
                    }
                    when (op) {
                        10 -> { // Hello
                            val interval = json.optJSONObject("d")?.optLong("heartbeat_interval", 41250L) ?: 41250L
                            startHeartbeat(interval)
                            sendIdentify(token)
                        }
                        0 -> { // Dispatch
                            val t = json.optString("t")
                            if (t == "READY") {
                                val userObj = json.optJSONObject("d")?.optJSONObject("user")
                                if (userObj != null) {
                                    val uname = userObj.optString("global_name").ifBlank { userObj.optString("username") }
                                    val uid = userObj.optString("id")
                                    val avHash = userObj.optString("avatar")
                                    val avUrl = if (uid.isNotBlank() && avHash.isNotBlank()) {
                                        "https://cdn.discordapp.com/avatars/$uid/$avHash.png?size=256"
                                    } else ""
                                    discordUsername.value = uname
                                    discordAvatarUrl.value = avUrl
                                    mmkv.encode(KEY_USERNAME, uname)
                                    mmkv.encode(KEY_AVATAR, avUrl)
                                }
                                isConnected.value = true
                                pendingActivityPayload?.let { webSocket.send(it.toString()) }
                            }
                        }
                    }
                }
            }

            override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
                Log.w("DiscordRPC", "Gateway connection failed: ${t.message}")
                isConnected.value = false
                heartbeatJob?.cancel()
                this@DiscordRpcManager.webSocket = null
            }

            override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
                isConnected.value = false
                heartbeatJob?.cancel()
                this@DiscordRpcManager.webSocket = null
            }
        })
    }

    private fun startHeartbeat(intervalMs: Long) {
        heartbeatJob?.cancel()
        heartbeatJob = scope.launch {
            while (isActive) {
                delay(intervalMs)
                val hb = JSONObject().apply {
                    put("op", 1)
                    put("d", lastSequence ?: JSONObject.NULL)
                }
                webSocket?.send(hb.toString())
            }
        }
    }

    private fun sendIdentify(token: String) {
        val superProps = JSONObject().apply {
            put("os", "Android")
            put("browser", "Discord Android")
            put("device", Build.MODEL ?: "Android")
            put("system_locale", "en-US")
            put("client_version", "212.9 - rn")
            put("release_channel", "googleRelease")
            put("device_vendor_id", "lyricsflow-mobile")
        }
        val identify = JSONObject().apply {
            put("op", 2)
            put(
                "d",
                JSONObject().apply {
                    put("token", token)
                    put("capabilities", 16381)
                    put("properties", superProps)
                    put("compress", false)
                }
            )
        }
        webSocket?.send(identify.toString())
    }

    private fun resolveExternalAsset(imageUrl: String): String? {
        val token = mmkv.decodeString(KEY_TOKEN).orEmpty()
        if (token.isBlank() || imageUrl.isBlank()) return null
        return runCatching {
            val url = URL("https://discord.com/api/v9/applications/$APPLICATION_ID/external-assets")
            val conn = (url.openConnection() as HttpURLConnection).apply {
                connectTimeout = 6000
                readTimeout = 6000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Authorization", token)
                setRequestProperty("Content-Type", "application/json")
                setRequestProperty(
                    "X-Super-Properties",
                    Base64.encodeToString("{\"os\":\"Android\",\"browser\":\"Discord Android\"}".toByteArray(), Base64.NO_WRAP)
                )
            }
            val body = JSONObject().put("urls", JSONArray().put(imageUrl)).toString()
            conn.outputStream.use { it.write(body.toByteArray(Charsets.UTF_8)) }
            if (conn.responseCode in 200..299) {
                val arr = JSONArray(conn.inputStream.bufferedReader().use { it.readText() })
                val extPath = arr.optJSONObject(0)?.optString("external_asset_path").orEmpty()
                if (extPath.isNotBlank()) "mp:$extPath" else null
            } else null
        }.getOrNull()
    }

    private fun disconnect() {
        heartbeatJob?.cancel()
        runCatching { webSocket?.close(1000, "Closed") }
        webSocket = null
        isConnected.value = false
    }
}
