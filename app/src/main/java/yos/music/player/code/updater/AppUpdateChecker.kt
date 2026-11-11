package com.lyricsflow.player.code.updater

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableStateOf
import com.lyricsflow.player.data.libraries.SettingsLibrary
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

data class AppReleaseInfo(
    val tagName: String,
    val title: String,
    val changelog: String,
    val apkDownloadUrl: String,
    val htmlUrl: String
)

/**
 * In-App GitHub Release Update Checker
 * Checks `https://api.github.com/repos/lyricsflow/LyricsFlow-Android/releases/latest`
 * and supports Update Now, Update Later, and Don't Update (Skip Version).
 */
@Stable
object AppUpdateChecker {
    private const val RELEASES_API = "https://api.github.com/repos/lyricsflow/LyricsFlow-Android/releases/latest"
    const val CURRENT_VERSION = "2.1.0"

    val availableUpdate = mutableStateOf<AppReleaseInfo?>(null)
    val isChecking = mutableStateOf(false)

    suspend fun checkForUpdates(forceShowEvenIfSkipped: Boolean = false): AppReleaseInfo? = withContext(Dispatchers.IO) {
        withContext(Dispatchers.Main) { isChecking.value = true }
        val result = runCatching {
            var conn: HttpURLConnection? = null
            try {
                conn = (URL(RELEASES_API).openConnection() as HttpURLConnection).apply {
                    connectTimeout = 8000
                    readTimeout = 8000
                    requestMethod = "GET"
                    setRequestProperty("Accept", "application/vnd.github+json")
                    setRequestProperty("User-Agent", "LyricsFlow-Android/$CURRENT_VERSION")
                }
                if (conn.responseCode !in 200..299) return@runCatching null
                val text = conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
                val json = JSONObject(text)
                val tag = json.optString("tag_name").removePrefix("v").trim()
                if (tag.isBlank() || !isNewerVersion(tag, CURRENT_VERSION)) {
                    return@runCatching null
                }
                if (!forceShowEvenIfSkipped && SettingsLibrary.SkippedUpdateVersion == tag) {
                    return@runCatching null
                }
                val assets = json.optJSONArray("assets")
                var apkUrl = ""
                if (assets != null) {
                    for (i in 0 until assets.length()) {
                        val asset = assets.optJSONObject(i) ?: continue
                        val dl = asset.optString("browser_download_url")
                        if (dl.endsWith(".apk", ignoreCase = true)) {
                            apkUrl = dl
                            break
                        }
                    }
                }
                val htmlUrl = json.optString("html_url").ifBlank { "https://github.com/lyricsflow/LyricsFlow-Android/releases" }
                AppReleaseInfo(
                    tagName = tag,
                    title = json.optString("name").ifBlank { "LyricsFlow v$tag" },
                    changelog = json.optString("body").ifBlank { "Bug fixes, Apple Music iOS 27 Liquid Glass refinements, and performance improvements." },
                    apkDownloadUrl = apkUrl.ifBlank { htmlUrl },
                    htmlUrl = htmlUrl
                )
            } finally {
                conn?.disconnect()
            }
        }.getOrNull()

        withContext(Dispatchers.Main) {
            isChecking.value = false
            if (result != null) {
                availableUpdate.value = result
            }
        }
        result
    }

    fun dismissForLater() {
        availableUpdate.value = null
    }

    fun skipVersion(tagName: String) {
        SettingsLibrary.SkippedUpdateVersion = tagName
        availableUpdate.value = null
    }

    fun openUpdateDownload(context: Context, release: AppReleaseInfo) {
        runCatching {
            val url = release.apkDownloadUrl.ifBlank { release.htmlUrl }
            context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        }
        availableUpdate.value = null
    }

    private fun isNewerVersion(remote: String, current: String): Boolean {
        val rParts = remote.split(".", "-").mapNotNull { it.toIntOrNull() }
        val cParts = current.split(".", "-").mapNotNull { it.toIntOrNull() }
        val maxLen = maxOf(rParts.size, cParts.size)
        for (i in 0 until maxLen) {
            val r = rParts.getOrElse(i) { 0 }
            val c = cParts.getOrElse(i) { 0 }
            if (r > c) return true
            if (r < c) return false
        }
        return false
    }
}
