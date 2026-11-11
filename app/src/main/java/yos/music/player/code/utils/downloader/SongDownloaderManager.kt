package com.lyricsflow.player.code.utils.downloader

import android.content.Context
import android.media.MediaScannerConnection
import android.os.Environment
import android.widget.Toast
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder

/**
 * Song, Lyrics (.ttml / .lrc), and Cover Art Downloader
 * Ported from `main/js/song-downloader.js` & `main/js/ttml-downloader.js`.
 * Saves downloads into `Downloads/lyricsflow/downloads/{Artist - Album}/`
 * and automatically scans files into Android MediaStore + refreshes `MusicLibrary`.
 */
object SongDownloaderManager {

    private fun sanitizeFileName(raw: String): String {
        return raw.trim()
            .replace(Regex("[\\\\/:*?\"<>|]"), "_")
            .ifBlank { "LyricsFlow_Track" }
    }

    fun getRootDownloadDir(context: Context): File {
        val publicDownloads = runCatching {
            Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS)
        }.getOrNull()
        val base = if (publicDownloads != null && (publicDownloads.exists() || publicDownloads.mkdirs())) {
            File(publicDownloads, "lyricsflow/downloads")
        } else {
            File(context.getExternalFilesDir(Environment.DIRECTORY_MUSIC) ?: context.filesDir, "lyricsflow/downloads")
        }
        if (!base.exists()) base.mkdirs()
        return base
    }

    fun downloadTrackAsync(
        context: Context,
        scope: CoroutineScope,
        track: LyricsFlowApi.CatalogTrackResult,
        albumTitleFallback: String = ""
    ) {
        val appContext = context.applicationContext
        Toast.makeText(appContext, "Downloading ${track.title}...", Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            val savedFile = downloadSingleTrackInternal(appContext, track, albumTitleFallback)
            withContext(Dispatchers.Main) {
                if (savedFile != null) {
                    Toast.makeText(
                        appContext,
                        "Saved to Downloads/lyricsflow/downloads/${savedFile.parentFile?.name}/${savedFile.name}",
                        Toast.LENGTH_LONG
                    ).show()
                } else {
                    Toast.makeText(appContext, "Download failed for ${track.title}", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    fun downloadFullAlbumAsync(
        context: Context,
        scope: CoroutineScope,
        albumTitle: String,
        artistName: String,
        artworkUrl: String,
        tracks: List<LyricsFlowApi.CatalogTrackResult>
    ) {
        if (tracks.isEmpty()) return
        val appContext = context.applicationContext
        Toast.makeText(appContext, "Downloading album $albumTitle (${tracks.size} tracks)...", Toast.LENGTH_SHORT).show()
        scope.launch(Dispatchers.IO) {
            var successCount = 0
            for (track in tracks) {
                val trackWithAlbum = if (track.album.isBlank()) track.copy(album = albumTitle) else track
                val trackWithArt = if (trackWithAlbum.artworkUrl.isBlank()) trackWithAlbum.copy(artworkUrl = artworkUrl) else trackWithAlbum
                val out = downloadSingleTrackInternal(appContext, trackWithArt, albumTitle)
                if (out != null) successCount++
            }
            withContext(Dispatchers.Main) {
                Toast.makeText(
                    appContext,
                    "Downloaded $successCount/${tracks.size} tracks from $albumTitle to Downloads/lyricsflow/downloads",
                    Toast.LENGTH_LONG
                ).show()
            }
        }
    }

    private suspend fun downloadSingleTrackInternal(
        context: Context,
        track: LyricsFlowApi.CatalogTrackResult,
        albumTitleFallback: String
    ): File? = withContext(Dispatchers.IO) {
        val cleanArtist = sanitizeFileName(track.artist.ifBlank { "Unknown Artist" })
        val cleanAlbum = sanitizeFileName(track.album.ifBlank { albumTitleFallback }.ifBlank { "${track.title} - Single" })
        val cleanTitle = sanitizeFileName(track.title)

        val folder = File(getRootDownloadDir(context), "$cleanArtist - $cleanAlbum")
        if (!folder.exists()) folder.mkdirs()

        val codec = when {
            runCatching { SettingsLibrary.EnableDolbyAtmos }.getOrDefault(false) -> "atmos"
            runCatching { SettingsLibrary.EnableLosslessStreaming }.getOrDefault(false) -> "alac"
            else -> "aac"
        }
        val ext = "m4a"
        val baseName = "$cleanArtist - $cleanTitle"
        val audioFile = File(folder, "$baseName.$ext")

        val encodedId = URLEncoder.encode(track.id, "UTF-8")
        val primaryUrl = "${LyricsFlowApi.API_BASE}/download?song=$encodedId&codec=$codec&l=en-US&fallback=true"
        val fallbackStreamUrl = "${LyricsFlowApi.API_BASE}/stream?song=$encodedId&codec=aac&fallback=false&l=en-US"

        val downloadedAudio = downloadUrlToFile(primaryUrl, audioFile)
            || downloadUrlToFile(fallbackStreamUrl, audioFile)
            || (track.previewUrl.isNotBlank() && downloadUrlToFile(track.previewUrl, audioFile))

        if (!downloadedAudio) return@withContext null

        // 2. Download high-res cover art (`cover.jpg`) if not already present in album folder
        val coverFile = File(folder, "cover.jpg")
        if (!coverFile.exists() && track.artworkUrl.isNotBlank()) {
            val hiResArt = LyricsFlowApi.cleanArtworkUrlString(track.artworkUrl, 1200)
            downloadUrlToFile(hiResArt, coverFile)
        }

        // 3. Download word-by-word TTML / LRC lyrics alongside the audio file
        runCatching {
            val lyrResult = LyricsFlowApi.downloadRawLyricsForTrack(track.id.ifBlank { track.title }, track.artist)
            if (lyrResult != null && lyrResult.second.isNotBlank()) {
                val lyrExt = if (lyrResult.first.endsWith(".lrc", ignoreCase = true)) "lrc" else "ttml"
                val lyricFile = File(folder, "$baseName.$lyrExt")
                lyricFile.writeText(lyrResult.second, Charsets.UTF_8)
            }
        }

        // 4. Trigger MediaScannerConnection so the track immediately appears in Android MediaStore & Library
        runCatching {
            MediaScannerConnection.scanFile(
                context,
                arrayOf(audioFile.absolutePath),
                arrayOf("audio/mp4")
            ) { _, _ ->
                scope.launch {
                    runCatching { MusicLibrary.scanMedia(context) }
                }
            }
        }

        audioFile
    }

    private fun downloadUrlToFile(urlString: String, targetFile: File): Boolean {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15000
                readTimeout = 30000
                instanceFollowRedirects = true
                requestMethod = "GET"
                setRequestProperty("User-Agent", "LyricsFlow-Android/2.1")
            }
            if (conn.responseCode !in 200..299) return false
            conn.inputStream.use { input ->
                targetFile.outputStream().use { output ->
                    input.copyTo(output)
                }
            }
            targetFile.exists() && targetFile.length() > 1024L
        } catch (_: Exception) {
            false
        } finally {
            conn?.disconnect()
        }
    }
}
