package com.lyricsflow.player.code.auth

import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.tencent.mmkv.MMKV
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class LyricsFlowUserProfile(
    val userId: String = "",
    val name: String = "Music Fan",
    val email: String = "",
    val avatarUrl: String = "",
    val token: String = "",
    val provider: String = "email"
) {
    val isLoggedIn: Boolean
        get() = token.isNotBlank()
}

/**
 * LyricsFlow User Authentication & Cloud Profile Manager
 * Ported from `main/js/user-profile.js` (`https://api.trylyricsflow.com/v1/auth/...`).
 */
@Stable
object LyricsFlowAuthManager {
    private const val MMKV_ID = "lyricsflow_user_auth"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_NAME = "user_name"
    private const val KEY_EMAIL = "user_email"
    private const val KEY_AVATAR = "user_avatar"
    private const val KEY_TOKEN = "user_token"
    private const val KEY_PROVIDER = "user_provider"

    private val mmkv by lazy { MMKV.mmkvWithID(MMKV_ID) }

    val currentUser = mutableStateOf(loadStoredProfile())

    private fun loadStoredProfile(): LyricsFlowUserProfile {
        return runCatching {
            LyricsFlowUserProfile(
                userId = mmkv.decodeString(KEY_USER_ID).orEmpty(),
                name = mmkv.decodeString(KEY_NAME).orEmpty().ifBlank { "Music Fan" },
                email = mmkv.decodeString(KEY_EMAIL).orEmpty(),
                avatarUrl = mmkv.decodeString(KEY_AVATAR).orEmpty(),
                token = mmkv.decodeString(KEY_TOKEN).orEmpty(),
                provider = mmkv.decodeString(KEY_PROVIDER).orEmpty().ifBlank { "email" }
            )
        }.getOrDefault(LyricsFlowUserProfile())
    }

    private fun saveProfile(profile: LyricsFlowUserProfile) {
        mmkv.encode(KEY_USER_ID, profile.userId)
        mmkv.encode(KEY_NAME, profile.name)
        mmkv.encode(KEY_EMAIL, profile.email)
        mmkv.encode(KEY_AVATAR, profile.avatarUrl)
        mmkv.encode(KEY_TOKEN, profile.token)
        mmkv.encode(KEY_PROVIDER, profile.provider)
        currentUser.value = profile
    }

    suspend fun login(emailOrUsername: String, password: String): Result<LyricsFlowUserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", emailOrUsername.trim())
                put("password", password)
            }
            val resp = postJson("${LyricsFlowApi.API_BASE}/v1/auth/login", payload)
            val token = resp.optString("token").ifBlank { resp.optString("access_token") }
            if (token.isBlank()) {
                val errMsg = resp.optString("error").ifBlank { resp.optString("message") }.ifBlank { "Invalid credentials" }
                throw IllegalStateException(errMsg)
            }
            val userObj = resp.optJSONObject("user") ?: resp
            val email = userObj.optString("email").ifBlank { emailOrUsername.trim() }
            val name = userObj.optString("name")
                .ifBlank { userObj.optString("username") }
                .ifBlank { email.substringBefore("@").ifBlank { "LyricsFlow User" } }
            val avatar = userObj.optString("pfp").ifBlank { userObj.optString("avatar_url") }
            val id = userObj.optString("id").ifBlank { userObj.optString("user_id") }
            val profile = LyricsFlowUserProfile(
                userId = id,
                name = name,
                email = email,
                avatarUrl = avatar,
                token = token,
                provider = "email"
            )
            withContext(Dispatchers.Main) { saveProfile(profile) }
            profile
        }
    }

    suspend fun signup(name: String, email: String, password: String): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("name", name.trim())
                put("email", email.trim())
                put("password", password)
            }
            val resp = postJson("${LyricsFlowApi.API_BASE}/v1/auth/signup", payload)
            val token = resp.optString("token").ifBlank { resp.optString("access_token") }
            if (token.isNotBlank()) {
                val profile = LyricsFlowUserProfile(
                    userId = resp.optString("user_id"),
                    name = name.trim().ifBlank { email.substringBefore("@") },
                    email = email.trim(),
                    token = token,
                    provider = "email"
                )
                withContext(Dispatchers.Main) { saveProfile(profile) }
                "signed_in"
            } else if (resp.optBoolean("requires_verification", false) || resp.optString("status") == "pending") {
                "verify_code"
            } else {
                val msg = resp.optString("message").ifBlank { "Verification code sent to $email" }
                msg
            }
        }
    }

    suspend fun verifyCode(email: String, code: String): Result<LyricsFlowUserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            val payload = JSONObject().apply {
                put("email", email.trim())
                put("code", code.trim())
            }
            val resp = postJson("${LyricsFlowApi.API_BASE}/v1/auth/verify-code", payload)
            val token = resp.optString("token").ifBlank { resp.optString("access_token") }
            if (token.isBlank()) {
                throw IllegalStateException(resp.optString("error").ifBlank { "Invalid verification code" })
            }
            val profile = LyricsFlowUserProfile(
                userId = resp.optString("user_id"),
                name = resp.optString("name").ifBlank { email.substringBefore("@") },
                email = email.trim(),
                token = token,
                provider = "email"
            )
            withContext(Dispatchers.Main) { saveProfile(profile) }
            profile
        }
    }

    suspend fun getOAuthUrl(provider: String): String = withContext(Dispatchers.IO) {
        runCatching {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL("${LyricsFlowApi.API_BASE}/v1/auth/oauth-url/$provider").openConnection() as HttpURLConnection).apply {
                    connectTimeout = 7000
                    readTimeout = 7000
                    requestMethod = "GET"
                }
                if (conn.responseCode in 200..299) {
                    val json = JSONObject(conn.inputStream.bufferedReader().use { it.readText() })
                    json.optString("url")
                } else ""
            } finally {
                conn?.disconnect()
            }
        }.getOrDefault("")
    }

    suspend fun logout() = withContext(Dispatchers.IO) {
        runCatching {
            postJson("${LyricsFlowApi.API_BASE}/v1/auth/logout", JSONObject())
        }
        withContext(Dispatchers.Main) {
            mmkv.clearAll()
            currentUser.value = LyricsFlowUserProfile()
        }
    }

    private fun postJson(urlString: String, body: JSONObject): JSONObject {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 9000
                readTimeout = 9000
                requestMethod = "POST"
                doOutput = true
                setRequestProperty("Content-Type", "application/json; charset=UTF-8")
                setRequestProperty("Accept", "application/json")
                setRequestProperty("User-Agent", "LyricsFlow-Android/2.1")
                val tok = currentUser.value.token
                if (tok.isNotBlank()) {
                    setRequestProperty("Authorization", if (tok.startsWith("Bearer ")) tok else "Bearer $tok")
                }
            }
            conn.outputStream.use { os ->
                os.write(body.toString().toByteArray(Charsets.UTF_8))
            }
            val stream = if (conn.responseCode in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (text.isNotBlank()) JSONObject(text) else JSONObject()
        } catch (e: Exception) {
            JSONObject().put("error", e.localizedMessage ?: "Network error")
        } finally {
            conn?.disconnect()
        }
    }
}
