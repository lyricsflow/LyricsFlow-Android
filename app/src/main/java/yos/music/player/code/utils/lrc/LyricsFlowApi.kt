package com.lyricsflow.player.code.utils.lrc

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lyricsflow.player.data.libraries.YosMediaItem
import com.lyricsflow.player.data.libraries.artistsName
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLEncoder
import java.security.MessageDigest
import java.util.Locale

data class LyricsFlowCatalogItem(
    val id: String,
    val type: String, // "songs", "albums", "artists", "playlists", "music-videos", "stations", "record-labels", "curators"
    val title: String,
    val subtitle: String,
    val albumName: String = "",
    val albumId: String = "",
    val artistId: String = "",
    val artworkUrl: String = "",
    val animatedVideoUrl: String? = null,
    val durationMs: Long = 0L,
    val isExplicit: Boolean = false,
    val releaseYear: String = "",
    val trackCount: Int = 0,
    val editorialBadge: String = "",
    val editorialSubtitle: String = ""
)

data class LyricsFlowCatalogSearchResponse(
    val topResults: List<LyricsFlowCatalogItem> = emptyList(),
    val songs: List<LyricsFlowCatalog.CatalogTrackResult> = emptyList(),
    val artists: List<LyricsFlowCatalogItem> = emptyList(),
    val albums: List<LyricsFlowCatalogItem> = emptyList(),
    val playlists: List<LyricsFlowCatalogItem> = emptyList(),
    val musicVideos: List<LyricsFlowCatalog.CatalogTrackResult> = emptyList(),
    val stations: List<LyricsFlowCatalogItem> = emptyList(),
    val recordLabels: List<LyricsFlowCatalogItem> = emptyList(),
    val curators: List<LyricsFlowCatalogItem> = emptyList()
)

object LyricsFlowCatalog {
    data class CatalogTrackResult(
        val id: String,
        val title: String,
        val artist: String,
        val artistId: String = "",
        val album: String = "",
        val albumId: String = "",
        val artworkUrl: String = "",
        val durationMs: Long = 0L,
        val trackNumber: Int = 1,
        val discNumber: Int = 1,
        val isExplicit: Boolean = false,
        val previewUrl: String = "",
        val releaseYear: Int? = null,
        val genre: String = "",
        val composerName: String = "",
        val hasLyrics: Boolean = true,
        val playCount: Int = 0,
        val type: String = "songs"
    ) {
        fun toCatalogItem(): LyricsFlowCatalogItem = LyricsFlowCatalogItem(
            id = id,
            type = type,
            title = title,
            subtitle = artist,
            albumName = album,
            albumId = albumId,
            artistId = artistId,
            artworkUrl = artworkUrl,
            durationMs = durationMs,
            isExplicit = isExplicit,
            releaseYear = releaseYear?.toString().orEmpty()
        )
    }

    data class CatalogAlbumDetail(
        val id: String,
        val name: String,
        val artistName: String,
        val artistId: String = "",
        val artworkUrl: String = "",
        val animatedVideoUrl: String? = null,
        val genre: String = "",
        val releaseYear: String = "",
        val releaseDateFormatted: String = "",
        val editorialNotes: String = "",
        val copyright: String = "",
        val recordLabel: String = "",
        val recordLabelId: String = "",
        val isExplicit: Boolean = false,
        val isSingleOrEp: Boolean = false,
        val tracks: List<CatalogTrackResult> = emptyList(),
        val moreByArtist: List<LyricsFlowCatalogItem> = emptyList(),
        val relatedAlbums: List<LyricsFlowCatalogItem> = emptyList()
    )

    data class CatalogArtistDetail(
        val id: String,
        val name: String,
        val artworkUrl: String = "",
        val heroVideoUrl: String? = null,
        val bio: String = "",
        val genre: String = "",
        val origin: String = "",
        val formedYear: String = "",
        val latestRelease: LyricsFlowCatalogItem? = null,
        val topSongs: List<CatalogTrackResult> = emptyList(),
        val essentialAlbums: List<LyricsFlowCatalogItem> = emptyList(),
        val albums: List<LyricsFlowCatalogItem> = emptyList(),
        val singlesAndEps: List<LyricsFlowCatalogItem> = emptyList(),
        val compilations: List<LyricsFlowCatalogItem> = emptyList(),
        val appearsOn: List<LyricsFlowCatalogItem> = emptyList(),
        val playlists: List<LyricsFlowCatalogItem> = emptyList(),
        val musicVideos: List<CatalogTrackResult> = emptyList(),
        val similarArtists: List<LyricsFlowCatalogItem> = emptyList()
    )

    data class CatalogPlaylistDetail(
        val id: String,
        val name: String,
        val curatorName: String = "Apple Music",
        val curatorId: String = "",
        val description: String = "",
        val artworkUrl: String = "",
        val animatedVideoUrl: String? = null,
        val updatedText: String = "",
        val tracks: List<CatalogTrackResult> = emptyList(),
        val suggestedSongs: List<CatalogTrackResult> = emptyList()
    )

    data class CatalogSongDetail(
        val track: CatalogTrackResult,
        val composerName: String = "",
        val genreNames: List<String> = emptyList(),
        val releaseDate: String = "",
        val isrc: String = "",
        val recordLabel: String = "",
        val copyright: String = "",
        val editorialNotes: String = "",
        val moreByArtist: List<CatalogTrackResult> = emptyList()
    )

    data class EditorialShelf(
        val id: String,
        val title: String,
        val subtitle: String = "",
        val isHeroShowcase: Boolean = false,
        val items: List<LyricsFlowCatalogItem> = emptyList()
    )
}

/**
 * LyricsFlow Multi-Provider API & AMP Catalog Client
 * Ported from `main/js/ttml-retriever.js`, `main/js/amp-parser.js`, and `main/js/upload.js`.
 */
object LyricsFlowApi {
    const val API_BASE = "https://api.trylyricsflow.com"
    private const val BETTER_LYRICS_API = "https://lyrics-api.boidu.dev/getLyrics"
    private const val LYRICS_PLUS_API = "https://lyricsplus.prjktla.my.id/v2/lyrics/get"
    private const val LRCLIB_API = "https://lrclib.net/api/get"
    private const val ITUNES_SEARCH_API = "https://itunes.apple.com/search"
    private const val ITUNES_LOOKUP_API = "https://itunes.apple.com/lookup"
    private const val CACHE_DIR_NAME = "lyricsflow_ttml_cache"

    typealias CatalogTrackResult = LyricsFlowCatalog.CatalogTrackResult
    typealias CatalogAlbumDetail = LyricsFlowCatalog.CatalogAlbumDetail
    typealias CatalogArtistDetail = LyricsFlowCatalog.CatalogArtistDetail
    typealias CatalogPlaylistDetail = LyricsFlowCatalog.CatalogPlaylistDetail
    typealias CatalogSongDetail = LyricsFlowCatalog.CatalogSongDetail
    typealias EditorialShelf = LyricsFlowCatalog.EditorialShelf

    enum class LyricSource(val id: String) {
        LF_COMMUNITY("lfcommunity"),
        LYRICSFLOW("lyricsflow"),
        BETTER_LYRICS("betterlyrics"),
        LYRICS_PLUS("lyricsplus"),
        MUSIXMATCH("musixmatch"),
        LRCLIB("lrclib"),
        NETEASE("netease")
    }

    data class FetchedLyricPayload(
        val content: String,
        val isTtml: Boolean,
        val source: String
    )

    fun getStreamUrl(songId: String): String {
        return "$API_BASE/stream?song=${encode(songId)}&l=en-US"
    }

    fun CatalogTrackResult.toYosMediaItem(): YosMediaItem {
        val streamUriStr = if (id.isNotBlank() && !id.startsWith("local_")) {
            getStreamUrl(id)
        } else {
            previewUrl.ifBlank { getStreamUrl(id) }
        }
        val parsedArtUri = artworkUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        return YosMediaItem(
            uri = Uri.parse(streamUriStr),
            mediaId = id.ifBlank { "${title}_${artist}" },
            mimeType = "audio/mp4",
            title = title,
            writer = composerName.ifBlank { artist },
            compilation = null,
            composer = composerName.ifBlank { null },
            artists = artist,
            album = album.ifBlank { "$title - Single" },
            albumArtists = artist,
            thumb = parsedArtUri,
            trackNumber = trackNumber,
            discNumber = discNumber,
            genre = genre.ifBlank { null },
            recordingDay = null,
            recordingMonth = null,
            recordingYear = releaseYear,
            releaseYear = releaseYear,
            artistId = artistId.toLongOrNull(),
            albumId = albumId.toLongOrNull(),
            genreId = null,
            author = artist,
            addDate = System.currentTimeMillis(),
            duration = durationMs.coerceAtLeast(0L),
            modifiedDate = System.currentTimeMillis(),
            cdTrackNumber = trackNumber
        )
    }

    suspend fun fetchAndParseLyrics(
        context: Context,
        music: YosMediaItem,
        durationMs: Long = 0L
    ): YosParsedLyricData? = withContext(Dispatchers.IO) {
        val title = music.title?.trim()?.takeIf { it.isNotEmpty() } ?: return@withContext null
        val artist = music.artistsName?.trim()?.takeIf { it.isNotEmpty() }
            ?: music.albumArtists?.trim().orEmpty()
        val album = music.album?.trim().orEmpty()
        val durationSec = if (durationMs > 0L) (durationMs / 1000L).toInt() else (music.duration / 1000L).toInt()
        val amTrackId = music.mediaId?.takeIf { it.all { ch -> ch.isDigit() } && it.length >= 6 }

        // 1. Check local disk cache first
        val cachedFile = getCacheFile(context, title, artist, album)
        if (cachedFile.exists() && cachedFile.length() > 16L) {
            val cachedText = runCatching { cachedFile.readText(Charsets.UTF_8) }.getOrNull()
            if (!cachedText.isNullOrBlank()) {
                parseRawLyricText(cachedText)?.let { return@withContext it }
            }
        }

        // 2. Query multi-provider cascade in parallel and pick highest-priority rich result
        val payload = autoFetchLyrics(
            title = title,
            artist = artist,
            album = album,
            durationSec = durationSec,
            amTrackId = amTrackId
        ) ?: return@withContext null

        val parsed = parseRawLyricText(payload.content)
        if (parsed != null && parsed.entries.isNotEmpty()) {
            runCatching {
                cachedFile.parentFile?.mkdirs()
                cachedFile.writeText(payload.content, Charsets.UTF_8)
            }
            Log.d("LyricsFlowApi", "Resolved lyrics from [${payload.source}] lines=${parsed.entries.size} isTtml=${parsed.isTtml}")
            return@withContext parsed
        }

        null
    }

    /**
     * Downloads raw TTML or LRC content for the TTML Downloader tool (`upload.js` `initTtmlDownloader`).
     */
    suspend fun downloadRawLyricsForTrack(
        songIdOrTitle: String,
        artistName: String = ""
    ): Pair<String, String>? = withContext(Dispatchers.IO) {
        val cleanInput = songIdOrTitle.trim()
        if (cleanInput.isEmpty()) return@withContext null

        val isNumericId = cleanInput.all { it.isDigit() } && cleanInput.length >= 5
        if (isNumericId) {
            // Try direct song ID endpoint
            val direct = httpGet("$API_BASE/lyrics?song=${encode(cleanInput)}", 9000)
            extractSyncedLyricsFromResponse(direct)?.let { payload ->
                val ext = if (payload.isTtml) "ttml" else "lrc"
                return@withContext "lyrics_${cleanInput}.$ext" to payload.content
            }
            // Try community lyrics by ID
            fetchLfCommunity("", "", cleanInput)?.let { payload ->
                return@withContext "lyrics_${cleanInput}.ttml" to payload.content
            }
        }

        val payload = autoFetchLyrics(
            title = cleanInput,
            artist = artistName,
            amTrackId = if (isNumericId) cleanInput else null
        ) ?: return@withContext null

        val safeName = "${cleanInput}_${artistName}".replace(Regex("[^a-zA-Z0-9_-]"), "_").take(50)
        val ext = if (payload.isTtml) "ttml" else "lrc"
        "${safeName}.$ext" to payload.content
    }

    suspend fun autoFetchLyrics(
        title: String,
        artist: String,
        album: String = "",
        durationSec: Int = 0,
        amTrackId: String? = null
    ): FetchedLyricPayload? = coroutineScope {
        val deferredMap = mapOf(
            LyricSource.LF_COMMUNITY to async(Dispatchers.IO) {
                runCatching { fetchLfCommunity(title, artist, amTrackId) }.getOrNull()
            },
            LyricSource.LYRICSFLOW to async(Dispatchers.IO) {
                runCatching { fetchLyricsFlowApple(title, artist, album, durationSec, amTrackId) }.getOrNull()
            },
            LyricSource.BETTER_LYRICS to async(Dispatchers.IO) {
                runCatching { fetchBetterLyrics(title, artist) }.getOrNull()
            },
            LyricSource.LYRICS_PLUS to async(Dispatchers.IO) {
                runCatching { fetchLyricsPlus(title, artist, album, durationSec) }.getOrNull()
            },
            LyricSource.MUSIXMATCH to async(Dispatchers.IO) {
                runCatching { fetchMusixmatch(title, artist, album, durationSec) }.getOrNull()
            },
            LyricSource.LRCLIB to async(Dispatchers.IO) {
                runCatching { fetchLrcLib(title, artist, album, durationSec) }.getOrNull()
            }
        )

        val priorityOrder = listOf(
            LyricSource.LF_COMMUNITY,
            LyricSource.LYRICSFLOW,
            LyricSource.BETTER_LYRICS,
            LyricSource.LYRICS_PLUS,
            LyricSource.MUSIXMATCH,
            LyricSource.LRCLIB
        )

        for (source in priorityOrder) {
            val result = deferredMap[source]?.await()
            if (result != null && result.content.isNotBlank()) {
                return@coroutineScope result
            }
        }
        null
    }

    fun parseRawLyricText(rawText: String): YosParsedLyricData? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null
        if (YosTtmlFactory.isTtmlText(trimmed)) {
            YosTtmlFactory().formatTtmlEntries(trimmed)?.let { return it }
        }
        val entries = YosLrcFactory().formatLrcEntries(trimmed)
        if (entries.isEmpty()) return null
        val endTimes = entries.mapIndexed { index, line ->
            entries.getOrNull(index + 1)?.firstOrNull()?.first
                ?: line.lastOrNull()?.first
                ?: line.firstOrNull()?.first
                ?: 0f
        }
        val syllablesForLines = entries.mapIndexed { lineIdx, line ->
            val lineStart = line.firstOrNull()?.first ?: 0f
            val lineEnd = endTimes.getOrElse(lineIdx) { lineStart + 2500f }
            val segments = if (line.size > 2) line.subList(1, line.size - 1) else emptyList()
            if (segments.isEmpty()) {
                emptyList()
            } else {
                var prevEnd = lineStart
                segments.mapIndexed { segIdx, pair ->
                    val segEnd = pair.first.coerceAtLeast(prevEnd + 40f)
                    val rawSeg = pair.second
                    val cleanSeg = rawSeg.trim()
                    val isPartOfWord = !rawSeg.endsWith(" ") && segIdx < segments.lastIndex
                    val syl = AmlSyllable(
                        text = cleanSeg.ifEmpty { rawSeg },
                        startTimeMs = prevEnd,
                        endTimeMs = segEnd.coerceAtMost(lineEnd.coerceAtLeast(segEnd)),
                        isPartOfWord = isPartOfWord
                    )
                    prevEnd = segEnd
                    syl
                }
            }
        }
        val subtitles = entries.map { line ->
            line.lastOrNull()?.second?.takeIf { it.isNotBlank() }
        }
        val emptyTranslit = List(entries.size) { null as String? }
        return YosParsedLyricData(
            entries = entries,
            endTimes = endTimes,
            otherSideForLines = List(entries.size) { false },
            transliterations = emptyTranslit,
            subtitles = subtitles,
            isTtml = false,
            syllablesForLines = syllablesForLines
        )
    }

    private fun fetchLfCommunity(title: String, artist: String, amTrackId: String?): FetchedLyricPayload? {
        val songId = amTrackId?.takeIf { it.isNotBlank() } ?: resolveItunesTrackId(title, artist) ?: return null
        val jsonText = httpGet("$API_BASE/api/community/lyrics/${encode(songId)}", 6000) ?: return null
        val json = JSONObject(jsonText)
        val entry = json.optJSONObject("entry") ?: return null
        val streamUrl = entry.optString("stream_url").takeIf { it.isNotBlank() } ?: return null
        val fullUrl = if (streamUrl.startsWith("http")) streamUrl else "$API_BASE$streamUrl"
        val ttml = httpGet(fullUrl, 8000)?.takeIf { YosTtmlFactory.isTtmlText(it) } ?: return null
        return FetchedLyricPayload(ttml, true, "LyricsFlow Community")
    }

    private fun fetchLyricsFlowApple(
        title: String,
        artist: String,
        album: String,
        durationSec: Int,
        amTrackId: String?
    ): FetchedLyricPayload? {
        if (!amTrackId.isNullOrBlank()) {
            val byIdText = httpGet("$API_BASE/lyrics?song=${encode(amTrackId)}", 8000)
            extractSyncedLyricsFromResponse(byIdText)?.let { payload ->
                return payload.copy(source = "LyricsFlow Apple Music")
            }
        }

        if (title.isBlank()) return null
        val params = buildString {
            append("title=").append(encode(title))
            append("&artist=").append(encode(artist))
            if (album.isNotBlank()) append("&album=").append(encode(album))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val responseText = httpGet("$API_BASE/lyrics/get?$params", 9000)
        return extractSyncedLyricsFromResponse(responseText)?.copy(source = "LyricsFlow API")
    }

    private fun extractSyncedLyricsFromResponse(responseText: String?): FetchedLyricPayload? {
        if (responseText.isNullOrBlank()) return null
        val trimmed = responseText.trim()
        if (YosTtmlFactory.isTtmlText(trimmed)) {
            return FetchedLyricPayload(trimmed, true, "LyricsFlow")
        }
        return runCatching {
            val json = JSONObject(trimmed)
            val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                ?: json.optString("ttml").takeIf { it.isNotBlank() }
                ?: json.optString("lyrics").takeIf { it.isNotBlank() }
                ?: return null
            FetchedLyricPayload(synced, YosTtmlFactory.isTtmlText(synced), "LyricsFlow")
        }.getOrNull()
    }

    private fun fetchBetterLyrics(title: String, artist: String): FetchedLyricPayload? {
        if (title.isBlank()) return null
        val url = "$BETTER_LYRICS_API?s=${encode(title)}&a=${encode(artist)}"
        val responseText = httpGet(url, 8000) ?: return null
        val json = JSONObject(responseText)
        val ttml = json.optString("ttml").takeIf { it.isNotBlank() } ?: return null
        return FetchedLyricPayload(ttml, YosTtmlFactory.isTtmlText(ttml), "BetterLyrics")
    }

    private fun fetchLyricsPlus(
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ): FetchedLyricPayload? {
        if (title.isBlank()) return null
        val params = buildString {
            append("title=").append(encode(title))
            append("&artist=").append(encode(artist))
            if (album.isNotBlank()) append("&album=").append(encode(album))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val responseText = httpGet("$LYRICS_PLUS_API?$params", 8000) ?: return null
        val json = JSONObject(responseText)
        val lyricsArr = json.optJSONArray("lyrics") ?: return null
        if (lyricsArr.length() == 0) return null
        val convertedTtml = convertLyricsPlusToTtml(json) ?: return null
        return FetchedLyricPayload(convertedTtml, true, "LyricsPlus")
    }

    private fun fetchMusixmatch(
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ): FetchedLyricPayload? {
        if (title.isBlank()) return null
        val params = buildString {
            append("format=json&namespace=lyrics_richsynched&subtitle_format=mxm&app_id=web-desktop-app-v1.0")
            append("&q_track=").append(encode(title))
            append("&q_artist=").append(encode(artist))
            if (album.isNotBlank()) append("&q_album=").append(encode(album))
            if (durationSec > 0) append("&q_duration=").append(durationSec)
        }
        val responseText = httpGet("$API_BASE/musixmatch?$params", 9000) ?: return null
        val json = JSONObject(responseText)
        val calls = json.optJSONObject("message")
            ?.optJSONObject("body")
            ?.optJSONObject("macro_calls") ?: return null

        val richsyncBody = calls.optJSONObject("matcher.track.get")
            ?.optJSONObject("message")?.optJSONObject("body")
            ?: calls.optJSONObject("track.richsync.get")
                ?.optJSONObject("message")?.optJSONObject("body")
                ?.optJSONObject("richsync")
        val richsyncStr = richsyncBody?.optString("richsync_body")?.takeIf { it.isNotBlank() }
        if (richsyncStr != null) {
            convertMusixmatchRichsyncToTtml(richsyncStr)?.let { ttml ->
                return FetchedLyricPayload(ttml, true, "Musixmatch RichSync")
            }
        }

        val subtitleBody = calls.optJSONObject("track.subtitles.get")
            ?.optJSONObject("message")?.optJSONObject("body")
            ?.optJSONArray("subtitle_list")?.optJSONObject(0)
            ?.optJSONObject("subtitle")?.optString("subtitle_body")
            ?.takeIf { it.isNotBlank() }
        if (subtitleBody != null) {
            return FetchedLyricPayload(subtitleBody, false, "Musixmatch Subtitles")
        }

        return null
    }

    private fun fetchLrcLib(
        title: String,
        artist: String,
        album: String,
        durationSec: Int
    ): FetchedLyricPayload? {
        if (title.isBlank()) return null
        val params = buildString {
            append("track_name=").append(encode(title))
            append("&artist_name=").append(encode(artist))
            if (album.isNotBlank()) append("&album_name=").append(encode(album))
            if (durationSec > 0) append("&duration=").append(durationSec)
        }
        val responseText = httpGet("$LRCLIB_API?$params", 7000) ?: return null
        val json = JSONObject(responseText)
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() } ?: return null
        return FetchedLyricPayload(synced, false, "LRCLIB")
    }

    private fun convertLyricsPlusToTtml(data: JSONObject): String? {
        val lyrics = data.optJSONArray("lyrics") ?: return null
        if (lyrics.length() == 0) return null
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Word"><body><div>""")

        for (i in 0 until lyrics.length()) {
            val line = lyrics.optJSONObject(i) ?: continue
            val startSec = line.optDouble("time", 0.0) / 1000.0
            val durSec = line.optDouble("duration", 2000.0) / 1000.0
            val endSec = startSec + durSec
            val singer = line.optJSONObject("element")?.optString("singer").orEmpty()
            val agentAttr = if (singer.equals("v2", ignoreCase = true) || singer.equals("v2000", ignoreCase = true)) {
                """ ttm:agent="v2""""
            } else {
                """ ttm:agent="v1""""
            }

            sb.append("""<p begin="${formatSec(startSec)}" end="${formatSec(endSec)}" itunes:key="L${i + 1}"$agentAttr>""")
            val syllabus = line.optJSONArray("syllabus")
            if (syllabus != null && syllabus.length() > 0) {
                for (j in 0 until syllabus.length()) {
                    val syl = syllabus.optJSONObject(j) ?: continue
                    val sStart = syl.optDouble("time", startSec * 1000.0) / 1000.0
                    val sDur = syl.optDouble("duration", 200.0) / 1000.0
                    val sEnd = sStart + sDur
                    val rawText = syl.optString("text", "")
                    val isBg = syl.optBoolean("isBackground", false)
                    val roleAttr = if (isBg) """ ttm:role="x-bg"""" else ""
                    sb.append("""<span begin="${formatSec(sStart)}" end="${formatSec(sEnd)}"$roleAttr>${escapeXml(rawText)}</span>""")
                }
            } else {
                sb.append(escapeXml(line.optString("text", "")))
            }
            sb.append("</p>")
        }

        sb.append("</div></body></tt>")
        return sb.toString()
    }

    private fun convertMusixmatchRichsyncToTtml(richsyncStr: String): String? {
        return runCatching {
            val arr = JSONArray(richsyncStr)
            if (arr.length() == 0) return null
            val sb = StringBuilder()
            sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
            sb.append("""<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="Word"><body><div>""")

            for (i in 0 until arr.length()) {
                val lineObj = arr.optJSONObject(i) ?: continue
                val ts = lineObj.optDouble("ts", 0.0)
                val te = lineObj.optDouble("te", ts + 2.5)
                val words = lineObj.optJSONArray("l") ?: continue
                sb.append("""<p begin="${formatSec(ts)}" end="${formatSec(te)}" itunes:key="L${i + 1}" ttm:agent="v1">""")
                for (j in 0 until words.length()) {
                    val w = words.optJSONObject(j) ?: continue
                    val text = w.optString("c", "")
                    if (text.isEmpty()) continue
                    val offset = w.optDouble("o", 0.0)
                    val nextOffset = if (j + 1 < words.length()) {
                        words.optJSONObject(j + 1)?.optDouble("o", offset + 0.25) ?: (offset + 0.25)
                    } else {
                        (te - ts).coerceAtLeast(offset + 0.2)
                    }
                    val wStart = ts + offset
                    val wEnd = (ts + nextOffset).coerceAtLeast(wStart + 0.05)
                    sb.append("""<span begin="${formatSec(wStart)}" end="${formatSec(wEnd)}">${escapeXml(text)}</span>""")
                }
                sb.append("</p>")
            }

            sb.append("</div></body></tt>")
            sb.toString()
        }.getOrNull()
    }

    // =========================================================================
    // FULL CATALOG SEARCH (`performCatalogSearch` in `upload.js` & `amp-parser.js`)
    // =========================================================================

    suspend fun searchCatalog(query: String, limit: Int = 25): List<CatalogTrackResult> {
        return searchFullCatalog(query, limit).songs
    }

    suspend fun searchFullCatalog(query: String, limit: Int = 25): LyricsFlowCatalogSearchResponse = withContext(Dispatchers.IO) {
        val cleanQuery = query.trim()
        if (cleanQuery.isEmpty()) return@withContext LyricsFlowCatalogSearchResponse()

        // 1. Primary AMP Catalog Search (`v1/catalog/kz/search` with `format[resources]=map`)
        runCatching {
            val encodedTerm = encode(cleanQuery)
            val ampUrl = "$API_BASE/v1/catalog/kz/search?art[music-videos:url]=c&art[url]=f&extend=artistUrl" +
                "&fields[albums]=artistName,artistUrl,artwork,contentRating,editorialArtwork,editorialNotes,name,playParams,releaseDate,url,trackCount" +
                "&fields[artists]=url,name,artwork&format[resources]=map" +
                "&include[albums]=artists&include[music-videos]=artists&include[songs]=artists&include[stations]=radio-show" +
                "&l=en-GB&limit=$limit&omit[resource]=autos&platform=web" +
                "&relate[albums]=artists&relate[songs]=albums&term=$encodedTerm" +
                "&types=activities,albums,apple-curators,artists,curators,editorial-items,music-movies,music-videos,playlists,record-labels,songs,stations,tv-episodes,uploaded-videos" +
                "&with=lyricHighlights,lyrics,naturalLanguage,serverBubbles,subtitles"
            val text = httpGet(ampUrl, 9000)
            if (!text.isNullOrBlank()) {
                val parsed = parseAmpOrStandardSearchJson(JSONObject(text))
                if (parsed.songs.isNotEmpty() || parsed.albums.isNotEmpty() || parsed.artists.isNotEmpty() || parsed.playlists.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        }

        // 2. Secondary LyricsFlow `/search` endpoint
        runCatching {
            val url = "$API_BASE/search?term=${encode(cleanQuery)}&types=songs,albums,artists,playlists,music-videos&limit=$limit&l=en-US"
            val text = httpGet(url, 8000)
            if (!text.isNullOrBlank()) {
                val parsed = parseAmpOrStandardSearchJson(JSONObject(text))
                if (parsed.songs.isNotEmpty() || parsed.albums.isNotEmpty() || parsed.artists.isNotEmpty()) {
                    return@withContext parsed
                }
            }
        }

        // 3. Fallback to iTunes Search API
        runCatching {
            val itunesUrl = "$ITUNES_SEARCH_API?term=${encode(cleanQuery)}&entity=song,album,musicArtist&limit=$limit"
            val text = httpGet(itunesUrl, 8000) ?: return@withContext LyricsFlowCatalogSearchResponse()
            val results = JSONObject(text).optJSONArray("results") ?: return@withContext LyricsFlowCatalogSearchResponse()
            val songs = mutableListOf<CatalogTrackResult>()
            val albums = mutableListOf<LyricsFlowCatalogItem>()
            val artists = mutableListOf<LyricsFlowCatalogItem>()

            for (i in 0 until results.length()) {
                val item = results.optJSONObject(i) ?: continue
                val wrapperType = item.optString("wrapperType")
                val art = item.optString("artworkUrl100").replace("100x100bb", "600x600bb")
                when (wrapperType) {
                    "track" -> {
                        val trackId = item.optLong("trackId", 0L).takeIf { it > 0L }?.toString() ?: continue
                        val releaseYear = item.optString("releaseDate").take(4).toIntOrNull()
                        songs.add(
                            CatalogTrackResult(
                                id = trackId,
                                title = item.optString("trackName"),
                                artist = item.optString("artistName"),
                                artistId = item.optLong("artistId", 0L).takeIf { it > 0L }?.toString().orEmpty(),
                                album = item.optString("collectionName"),
                                albumId = item.optLong("collectionId", 0L).takeIf { it > 0L }?.toString().orEmpty(),
                                artworkUrl = art,
                                durationMs = item.optLong("trackTimeMillis", 0L),
                                trackNumber = item.optInt("trackNumber", 1),
                                discNumber = item.optInt("discNumber", 1),
                                isExplicit = item.optString("trackExplicitness").equals("explicit", ignoreCase = true),
                                previewUrl = item.optString("previewUrl"),
                                releaseYear = releaseYear,
                                genre = item.optString("primaryGenreName")
                            )
                        )
                    }
                    "collection" -> {
                        val colId = item.optLong("collectionId", 0L).takeIf { it > 0L }?.toString() ?: continue
                        albums.add(
                            LyricsFlowCatalogItem(
                                id = colId,
                                type = "albums",
                                title = item.optString("collectionName"),
                                subtitle = item.optString("artistName"),
                                albumName = item.optString("collectionName"),
                                albumId = colId,
                                artistId = item.optLong("artistId", 0L).takeIf { it > 0L }?.toString().orEmpty(),
                                artworkUrl = art,
                                isExplicit = item.optString("collectionExplicitness").equals("explicit", ignoreCase = true),
                                releaseYear = item.optString("releaseDate").take(4),
                                trackCount = item.optInt("trackCount", 0)
                            )
                        )
                    }
                    "artist" -> {
                        val artId = item.optLong("artistId", 0L).takeIf { it > 0L }?.toString() ?: continue
                        artists.add(
                            LyricsFlowCatalogItem(
                                id = artId,
                                type = "artists",
                                title = item.optString("artistName"),
                                subtitle = item.optString("primaryGenreName", "Artist"),
                                artistId = artId,
                                artworkUrl = art
                            )
                        )
                    }
                }
            }
            val top = buildList {
                artists.firstOrNull()?.let { add(it) }
                songs.firstOrNull()?.let { add(it.toCatalogItem()) }
                albums.firstOrNull()?.let { add(it) }
                songs.drop(1).firstOrNull()?.let { add(it.toCatalogItem()) }
            }
            return@withContext LyricsFlowCatalogSearchResponse(
                topResults = top,
                songs = songs,
                artists = artists,
                albums = albums
            )
        }

        LyricsFlowCatalogSearchResponse()
    }

    private fun parseAmpOrStandardSearchJson(root: JSONObject): LyricsFlowCatalogSearchResponse {
        val resultsObj = root.optJSONObject("results") ?: root
        val resourcesObj = root.optJSONObject("resources")

        fun dereference(rawItem: JSONObject, fallbackType: String): JSONObject {
            val id = rawItem.optString("id")
            val type = rawItem.optString("type").ifBlank { fallbackType }
            if (resourcesObj != null && id.isNotBlank()) {
                resourcesObj.optJSONObject(type)?.optJSONObject(id)?.let { return it }
            }
            return rawItem
        }

        fun extractRelationshipId(entity: JSONObject, relName: String): String {
            val rels = entity.optJSONObject("relationships") ?: return ""
            val dataArr = rels.optJSONObject(relName)?.optJSONArray("data") ?: return ""
            return dataArr.optJSONObject(0)?.optString("id").orEmpty()
        }

        fun parseTrackEntity(rawItem: JSONObject, defaultType: String = "songs"): CatalogTrackResult? {
            val entity = dereference(rawItem, defaultType)
            val id = entity.optString("id").ifBlank { rawItem.optString("id") }
            val attrs = entity.optJSONObject("attributes") ?: entity
            val name = attrs.optString("name").ifBlank { attrs.optString("title") }
            if (id.isBlank() || name.isBlank()) return null

            val artistName = attrs.optString("artistName").ifBlank { "Unknown Artist" }
            val albumName = attrs.optString("albumName")
            val artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork") ?: attrs.optJSONObject("editorialArtwork"))
            val durationMs = attrs.optLong("durationInMillis", 0L)
            val isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true)
            val previewUrl = attrs.optJSONArray("previews")?.optJSONObject(0)?.optString("url").orEmpty()
            val releaseYear = attrs.optString("releaseDate").take(4).toIntOrNull()
            val genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty()
            val composer = attrs.optString("composerName")
            val artistId = extractRelationshipId(entity, "artists")
            val albumId = extractRelationshipId(entity, "albums").ifBlank {
                attrs.optString("url").substringAfter("/album/", "").substringAfter("/", "").substringBefore("?").takeIf { it.all(Char::isDigit) }.orEmpty()
            }

            return CatalogTrackResult(
                id = id,
                title = name,
                artist = artistName,
                artistId = artistId,
                album = albumName,
                albumId = albumId,
                artworkUrl = artworkUrl,
                durationMs = durationMs,
                trackNumber = attrs.optInt("trackNumber", 1),
                discNumber = attrs.optInt("discNumber", 1),
                isExplicit = isExplicit,
                previewUrl = previewUrl,
                releaseYear = releaseYear,
                genre = genre,
                composerName = composer,
                hasLyrics = attrs.optBoolean("hasLyrics", true),
                type = defaultType
            )
        }

        fun parseCatalogGroup(groupKeys: List<String>, defaultType: String): List<LyricsFlowCatalogItem> {
            val items = mutableListOf<LyricsFlowCatalogItem>()
            for (groupKey in groupKeys) {
                val group = resultsObj.optJSONObject(groupKey)
                val dataArr = group?.optJSONArray("data") ?: resultsObj.optJSONArray(groupKey) ?: continue
                for (i in 0 until dataArr.length()) {
                    val rawItem = dataArr.optJSONObject(i) ?: continue
                    val rawType = rawItem.optString("type").ifBlank { defaultType }
                    val entity = dereference(rawItem, rawType)
                    val id = entity.optString("id").ifBlank { rawItem.optString("id") }
                    val attrs = entity.optJSONObject("attributes") ?: entity
                    val name = attrs.optString("name").ifBlank { attrs.optString("title") }
                    if (id.isBlank() || name.isBlank()) continue
                    val subtitle = attrs.optString("artistName")
                        .ifBlank { attrs.optString("curatorName") }
                        .ifBlank { attrs.optJSONArray("genreNames")?.optString(0).orEmpty() }
                    val albumName = attrs.optString("albumName", if (rawType == "albums") name else "")
                    val artworkUrl = resolveArtworkUrl(
                        attrs.optJSONObject("artwork")
                            ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("subscriptionHero")
                            ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("storeFlowcase")
                    )
                    val durationMs = attrs.optLong("durationInMillis", 0L)
                    val isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true)
                    val releaseYear = attrs.optString("releaseDate").take(4)
                    val artistId = extractRelationshipId(entity, "artists").ifBlank { if (rawType == "artists") id else "" }
                    val albumId = extractRelationshipId(entity, "albums").ifBlank { if (rawType == "albums") id else "" }

                    items.add(
                        LyricsFlowCatalogItem(
                            id = id,
                            type = rawType,
                            title = name,
                            subtitle = subtitle,
                            albumName = albumName,
                            albumId = albumId,
                            artistId = artistId,
                            artworkUrl = artworkUrl,
                            durationMs = durationMs,
                            isExplicit = isExplicit,
                            releaseYear = releaseYear,
                            trackCount = attrs.optInt("trackCount", 0)
                        )
                    )
                }
            }
            // Also check resourcesObj directly if resultsObj omitted the group
            if (items.isEmpty() && resourcesObj != null) {
                val mapObj = resourcesObj.optJSONObject(defaultType)
                if (mapObj != null) {
                    val keys = mapObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        val entity = mapObj.optJSONObject(key) ?: continue
                        val attrs = entity.optJSONObject("attributes") ?: continue
                        val name = attrs.optString("name")
                        if (name.isBlank()) continue
                        val subtitle = attrs.optString("artistName").ifBlank { attrs.optString("curatorName") }
                        val artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"))
                        items.add(
                            LyricsFlowCatalogItem(
                                id = entity.optString("id", key),
                                type = defaultType,
                                title = name,
                                subtitle = subtitle,
                                albumName = if (defaultType == "albums") name else attrs.optString("albumName"),
                                albumId = if (defaultType == "albums") key else extractRelationshipId(entity, "albums"),
                                artistId = if (defaultType == "artists") key else extractRelationshipId(entity, "artists"),
                                artworkUrl = artworkUrl,
                                isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                                releaseYear = attrs.optString("releaseDate").take(4),
                                trackCount = attrs.optInt("trackCount", 0)
                            )
                        )
                    }
                }
            }
            return items
        }

        fun parseTrackGroup(groupKey: String, resourceType: String): List<CatalogTrackResult> {
            val list = mutableListOf<CatalogTrackResult>()
            val group = resultsObj.optJSONObject(groupKey)
            val dataArr = group?.optJSONArray("data") ?: resultsObj.optJSONArray(groupKey)
            if (dataArr != null) {
                for (i in 0 until dataArr.length()) {
                    val raw = dataArr.optJSONObject(i) ?: continue
                    parseTrackEntity(raw, resourceType)?.let { list.add(it) }
                }
            }
            if (list.isEmpty() && resourcesObj != null) {
                val mapObj = resourcesObj.optJSONObject(resourceType)
                if (mapObj != null) {
                    val keys = mapObj.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val raw = mapObj.optJSONObject(k) ?: continue
                        parseTrackEntity(raw, resourceType)?.let { list.add(it) }
                    }
                }
            }
            return list
        }

        val songs = parseTrackGroup("songs", "songs")
        val musicVideos = parseTrackGroup("music_videos", "music-videos").ifEmpty {
            parseTrackGroup("music-videos", "music-videos")
        }
        val albums = parseCatalogGroup(listOf("albums"), "albums")
        val artists = parseCatalogGroup(listOf("artists"), "artists")
        val playlists = parseCatalogGroup(listOf("playlists"), "playlists")
        val stations = parseCatalogGroup(listOf("stations"), "stations")
        val recordLabels = parseCatalogGroup(listOf("record_labels", "record-labels"), "record-labels")
        val curators = parseCatalogGroup(listOf("curators", "apple_curators", "apple-curators"), "curators")

        val topResults = parseCatalogGroup(listOf("top", "topResults"), "songs").ifEmpty {
            buildList {
                artists.firstOrNull()?.let { add(it) }
                songs.firstOrNull()?.let { add(it.toCatalogItem()) }
                albums.firstOrNull()?.let { add(it) }
                playlists.firstOrNull()?.let { add(it) }
                songs.drop(1).firstOrNull()?.let { add(it.toCatalogItem()) }
            }
        }

        return LyricsFlowCatalogSearchResponse(
            topResults = topResults,
            songs = songs,
            artists = artists,
            albums = albums,
            playlists = playlists,
            musicVideos = musicVideos,
            stations = stations,
            recordLabels = recordLabels,
            curators = curators
        )
    }

    // =========================================================================
    // ONLINE ALBUM DETAILS (`fetchAlbumDetails` in `upload.js`)
    // =========================================================================

    suspend fun fetchAlbumDetails(
        albumId: String,
        fallbackTitle: String = "",
        fallbackArtist: String = ""
    ): CatalogAlbumDetail? = withContext(Dispatchers.IO) {
        val cleanId = albumId.trim()
        if (cleanId.isEmpty()) return@withContext null

        // Fetch animated cover art in parallel (`${API_BASE}/animatedart?album=${albumId}&l=en-US`)
        val animatedArtDeferred = async(Dispatchers.IO) {
            fetchAlbumAnimatedArtUrl(cleanId)
        }

        // 1. Primary `/album?album=...` endpoint from `upload.js`
        runCatching {
            val url = "$API_BASE/album?album=${encode(cleanId)}&l=en-US" +
                "&include=artists,tracks,record-labels,music-videos" +
                "&views=appears-on,other-versions,related-albums,related-videos" +
                "&extend=artistUrl,editorialArtwork,editorialNotes,editorialVideo,composerName"
            val text = httpGet(url, 10000)
            if (!text.isNullOrBlank()) {
                val root = JSONObject(text)
                val dataArr = root.optJSONArray("data")
                val albumObj = dataArr?.optJSONObject(0) ?: root.optJSONObject("album") ?: root
                val attrs = albumObj.optJSONObject("attributes") ?: albumObj
                val name = attrs.optString("name").ifBlank { fallbackTitle }
                if (name.isNotBlank()) {
                    val artistName = attrs.optString("artistName").ifBlank { fallbackArtist }
                    val rels = albumObj.optJSONObject("relationships")
                    val artistId = rels?.optJSONObject("artists")?.optJSONArray("data")
                        ?.optJSONObject(0)?.optString("id").orEmpty()
                    val artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 1000, 1000)
                    val editorialVideoUrl = extractEditorialVideoUrl(attrs.optJSONObject("editorialVideo"))
                    val releaseDate = attrs.optString("releaseDate")
                    val releaseYear = releaseDate.take(4)
                    val genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty()
                    val editorialNotes = attrs.optJSONObject("editorialNotes")?.optString("standard")
                        ?.ifBlank { attrs.optJSONObject("editorialNotes")?.optString("short").orEmpty() }
                        ?.replace(Regex("<[^>]*>"), "")
                        .orEmpty()
                    val copyright = attrs.optString("copyright")
                    val recordLabel = attrs.optString("recordLabel").ifBlank {
                        rels?.optJSONObject("record-labels")?.optJSONArray("data")
                            ?.optJSONObject(0)?.optJSONObject("attributes")?.optString("name").orEmpty()
                    }
                    val recordLabelId = rels?.optJSONObject("record-labels")?.optJSONArray("data")
                        ?.optJSONObject(0)?.optString("id").orEmpty()
                    val isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true)
                    val isSingleOrEp = attrs.optBoolean("isSingle", false) ||
                        name.contains(" - Single", ignoreCase = true) ||
                        name.contains(" - EP", ignoreCase = true)

                    // Parse tracks
                    val tracksData = rels?.optJSONObject("tracks")?.optJSONArray("data")
                        ?: attrs.optJSONArray("tracks")
                    val tracks = mutableListOf<CatalogTrackResult>()
                    if (tracksData != null) {
                        for (i in 0 until tracksData.length()) {
                            val tObj = tracksData.optJSONObject(i) ?: continue
                            val tAttrs = tObj.optJSONObject("attributes") ?: tObj
                            val tId = tObj.optString("id")
                            val tName = tAttrs.optString("name")
                            if (tId.isBlank() || tName.isBlank()) continue
                            val tArt = resolveArtworkUrl(tAttrs.optJSONObject("artwork"), 600, 600).ifBlank { artworkUrl }
                            val tArtistId = tObj.optJSONObject("relationships")?.optJSONObject("artists")
                                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id")
                                ?.ifBlank { artistId } ?: artistId
                            tracks.add(
                                CatalogTrackResult(
                                    id = tId,
                                    title = tName,
                                    artist = tAttrs.optString("artistName").ifBlank { artistName },
                                    artistId = tArtistId,
                                    album = name,
                                    albumId = cleanId,
                                    artworkUrl = tArt,
                                    durationMs = tAttrs.optLong("durationInMillis", 0L),
                                    trackNumber = tAttrs.optInt("trackNumber", i + 1),
                                    discNumber = tAttrs.optInt("discNumber", 1),
                                    isExplicit = tAttrs.optString("contentRating").equals("explicit", ignoreCase = true),
                                    previewUrl = tAttrs.optJSONArray("previews")?.optJSONObject(0)?.optString("url").orEmpty(),
                                    releaseYear = releaseYear.toIntOrNull(),
                                    genre = genre,
                                    composerName = tAttrs.optString("composerName"),
                                    hasLyrics = tAttrs.optBoolean("hasLyrics", true),
                                    type = tObj.optString("type", "songs")
                                )
                            )
                        }
                    }

                    // Parse related / more by artist
                    val views = albumObj.optJSONObject("views")
                    val relatedAlbums = parseAlbumShelfView(views?.optJSONObject("related-albums"))
                    val moreByArtist = if (artistId.isNotBlank()) {
                        fetchArtistAlbumsList(artistId, excludeAlbumId = cleanId)
                    } else {
                        emptyList()
                    }

                    val resolvedAnimatedUrl = editorialVideoUrl ?: animatedArtDeferred.await()

                    if (tracks.isNotEmpty()) {
                        return@withContext CatalogAlbumDetail(
                            id = cleanId,
                            name = name,
                            artistName = artistName,
                            artistId = artistId,
                            artworkUrl = artworkUrl,
                            animatedVideoUrl = resolvedAnimatedUrl,
                            genre = genre,
                            releaseYear = releaseYear,
                            releaseDateFormatted = releaseDate,
                            editorialNotes = editorialNotes,
                            copyright = copyright,
                            recordLabel = recordLabel,
                            recordLabelId = recordLabelId,
                            isExplicit = isExplicit,
                            isSingleOrEp = isSingleOrEp,
                            tracks = tracks,
                            moreByArtist = moreByArtist,
                            relatedAlbums = relatedAlbums
                        )
                    }
                }
            }
        }

        // 2. Fallback to `/itunes/lookup?id=${albumId}&entity=song` (`upload.js` line 4088)
        for (lookupBase in listOf("$API_BASE/itunes/lookup", ITUNES_LOOKUP_API)) {
            runCatching {
                val text = httpGet("$lookupBase?id=${encode(cleanId)}&entity=song&l=en-US", 8000) ?: return@runCatching
                val results = JSONObject(text).optJSONArray("results") ?: return@runCatching
                if (results.length() == 0) return@runCatching
                val collection = results.optJSONObject(0) ?: return@runCatching
                val albumName = collection.optString("collectionName").ifBlank { fallbackTitle }
                val artistName = collection.optString("artistName").ifBlank { fallbackArtist }
                val artistId = collection.optLong("artistId", 0L).takeIf { it > 0L }?.toString().orEmpty()
                val artworkUrl = collection.optString("artworkUrl100").replace("100x100bb", "1000x1000bb")
                val releaseDate = collection.optString("releaseDate").take(10)
                val releaseYear = releaseDate.take(4)
                val genre = collection.optString("primaryGenreName")
                val copyright = collection.optString("copyright")
                val isExplicit = collection.optString("collectionExplicitness").equals("explicit", ignoreCase = true)

                val tracks = mutableListOf<CatalogTrackResult>()
                for (i in 1 until results.length()) {
                    val item = results.optJSONObject(i) ?: continue
                    if (item.optString("wrapperType") != "track") continue
                    val tId = item.optLong("trackId", 0L).takeIf { it > 0L }?.toString() ?: continue
                    tracks.add(
                        CatalogTrackResult(
                            id = tId,
                            title = item.optString("trackName"),
                            artist = item.optString("artistName").ifBlank { artistName },
                            artistId = item.optLong("artistId", 0L).takeIf { it > 0L }?.toString() ?: artistId,
                            album = albumName,
                            albumId = cleanId,
                            artworkUrl = item.optString("artworkUrl100").replace("100x100bb", "600x600bb").ifBlank { artworkUrl },
                            durationMs = item.optLong("trackTimeMillis", 0L),
                            trackNumber = item.optInt("trackNumber", i),
                            discNumber = item.optInt("discNumber", 1),
                            isExplicit = item.optString("trackExplicitness").equals("explicit", ignoreCase = true),
                            previewUrl = item.optString("previewUrl"),
                            releaseYear = releaseYear.toIntOrNull(),
                            genre = genre
                        )
                    )
                }

                return@withContext CatalogAlbumDetail(
                    id = cleanId,
                    name = albumName,
                    artistName = artistName,
                    artistId = artistId,
                    artworkUrl = artworkUrl,
                    animatedVideoUrl = animatedArtDeferred.await(),
                    genre = genre,
                    releaseYear = releaseYear,
                    releaseDateFormatted = releaseDate,
                    copyright = copyright,
                    isExplicit = isExplicit,
                    tracks = tracks
                )
            }
        }

        null
    }

    suspend fun fetchAlbumAnimatedArtUrl(albumId: String): String? = withContext(Dispatchers.IO) {
        if (albumId.isBlank()) return@withContext null
        runCatching {
            val text = httpGet("$API_BASE/animatedart?album=${encode(albumId)}&l=en-US", 6000) ?: return@runCatching
            val json = JSONObject(text)
            val url = json.optString("videoUrl").ifBlank { json.optString("url") }.ifBlank { json.optString("video") }
            if (url.isNotBlank()) return@withContext url
        }
        null
    }

    private fun fetchArtistAlbumsList(artistId: String, excludeAlbumId: String = ""): List<LyricsFlowCatalogItem> {
        return runCatching {
            val text = httpGet("$API_BASE/artist/albums?artist=${encode(artistId)}&limit=25&l=en-US", 6000) ?: return emptyList()
            val root = JSONObject(text)
            val dataArr = root.optJSONArray("data") ?: return emptyList()
            val list = mutableListOf<LyricsFlowCatalogItem>()
            for (i in 0 until dataArr.length()) {
                val item = dataArr.optJSONObject(i) ?: continue
                val id = item.optString("id")
                if (id.isBlank() || id == excludeAlbumId) continue
                val attrs = item.optJSONObject("attributes") ?: item
                val name = attrs.optString("name")
                if (name.isBlank()) continue
                list.add(
                    LyricsFlowCatalogItem(
                        id = id,
                        type = "albums",
                        title = name,
                        subtitle = attrs.optString("artistName"),
                        albumName = name,
                        albumId = id,
                        artistId = artistId,
                        artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 500, 500),
                        isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                        releaseYear = attrs.optString("releaseDate").take(4),
                        trackCount = attrs.optInt("trackCount", 0)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun parseAlbumShelfView(viewObj: JSONObject?): List<LyricsFlowCatalogItem> {
        val dataArr = viewObj?.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<LyricsFlowCatalogItem>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: item
            val name = attrs.optString("name")
            if (id.isBlank() || name.isBlank()) continue
            list.add(
                LyricsFlowCatalogItem(
                    id = id,
                    type = item.optString("type", "albums"),
                    title = name,
                    subtitle = attrs.optString("artistName"),
                    albumName = name,
                    albumId = id,
                    artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 500, 500),
                    isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                    releaseYear = attrs.optString("releaseDate").take(4)
                )
            )
        }
        return list
    }

    // =========================================================================
    // ONLINE ARTIST DETAILS (`openArtistView` in `upload.js`)
    // =========================================================================

    suspend fun fetchArtistDetails(
        artistId: String,
        fallbackName: String = ""
    ): CatalogArtistDetail? = withContext(Dispatchers.IO) {
        val cleanId = artistId.trim()
        if (cleanId.isEmpty()) return@withContext null

        // 1. Fetch `/artist?id=$cleanId...` & `/artist/albums?artist=$cleanId` in parallel
        val artistViewsDeferred = async(Dispatchers.IO) {
            val url = "$API_BASE/artist?id=${encode(cleanId)}&storefront=us&l=en-US" +
                "&include=albums,music-videos,playlists,station" +
                "&views=top-songs,full-albums,singles-eps,featured-playlists,latest-release,appears-on-albums,similar-artists" +
                "&extend=editorialArtwork,editorialVideo,editorialNotes,artistBio,bornOrFormed,origin"
            httpGet(url, 10000)
        }
        val artistAlbumsDeferred = async(Dispatchers.IO) {
            httpGet("$API_BASE/artist/albums?artist=${encode(cleanId)}&limit=80&l=en-US", 8000)
        }

        val primaryText = artistViewsDeferred.await()
        val albumsText = artistAlbumsDeferred.await()

        if (!primaryText.isNullOrBlank()) {
            runCatching {
                val root = JSONObject(primaryText)
                val artistObj = root.optJSONArray("data")?.optJSONObject(0) ?: root.optJSONObject("artist") ?: root
                val attrs = artistObj.optJSONObject("attributes") ?: artistObj
                val name = attrs.optString("name").ifBlank { fallbackName }
                if (name.isNotBlank()) {
                    val artworkUrl = resolveArtworkUrl(
                        attrs.optJSONObject("editorialArtwork")?.optJSONObject("subscriptionHero")
                            ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("bannerUber")
                            ?: attrs.optJSONObject("artwork"),
                        1200,
                        1200
                    )
                    val heroVideoUrl = extractEditorialVideoUrl(attrs.optJSONObject("editorialVideo"))
                    val bio = attrs.optString("artistBio")
                        .ifBlank { attrs.optJSONObject("editorialNotes")?.optString("standard").orEmpty() }
                        .ifBlank { attrs.optJSONObject("editorialNotes")?.optString("short").orEmpty() }
                        .replace(Regex("<[^>]*>"), "")
                    val genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty()
                    val origin = attrs.optString("origin")
                    val formedYear = attrs.optString("bornOrFormed")

                    val views = artistObj.optJSONObject("views")
                    val rels = artistObj.optJSONObject("relationships")

                    val topSongs = parseArtistTrackView(views?.optJSONObject("top-songs"), cleanId, name)
                    val latestRelease = parseAlbumShelfView(views?.optJSONObject("latest-release")).firstOrNull()
                    val essentialAlbums = parseAlbumShelfView(views?.optJSONObject("essential-albums"))
                    val fullAlbumsFromView = parseAlbumShelfView(views?.optJSONObject("full-albums"))
                    val singlesFromView = parseAlbumShelfView(views?.optJSONObject("singles-eps"))
                    val appearsOn = parseAlbumShelfView(views?.optJSONObject("appears-on-albums"))
                    val playlists = parseAlbumShelfView(views?.optJSONObject("featured-playlists")).ifEmpty {
                        parseAlbumShelfView(rels?.optJSONObject("playlists"))
                    }
                    val musicVideos = parseArtistTrackView(
                        views?.optJSONObject("top-music-videos") ?: rels?.optJSONObject("music-videos"),
                        cleanId,
                        name,
                        defaultType = "music-videos"
                    )
                    val similarArtists = parseAlbumShelfView(views?.optJSONObject("similar-artists")).map {
                        it.copy(type = "artists", artistId = it.id)
                    }

                    // Supplement albums and singles from `/artist/albums` if needed
                    val allDiscography = mutableListOf<LyricsFlowCatalogItem>()
                    if (!albumsText.isNullOrBlank()) {
                        runCatching {
                            val dArr = JSONObject(albumsText).optJSONArray("data")
                            if (dArr != null) {
                                for (i in 0 until dArr.length()) {
                                    val item = dArr.optJSONObject(i) ?: continue
                                    val id = item.optString("id")
                                    val aAttrs = item.optJSONObject("attributes") ?: item
                                    val aName = aAttrs.optString("name")
                                    if (id.isBlank() || aName.isBlank()) continue
                                    allDiscography.add(
                                        LyricsFlowCatalogItem(
                                            id = id,
                                            type = "albums",
                                            title = aName,
                                            subtitle = aAttrs.optString("artistName").ifBlank { name },
                                            albumName = aName,
                                            albumId = id,
                                            artistId = cleanId,
                                            artworkUrl = resolveArtworkUrl(aAttrs.optJSONObject("artwork"), 500, 500),
                                            isExplicit = aAttrs.optString("contentRating").equals("explicit", ignoreCase = true),
                                            releaseYear = aAttrs.optString("releaseDate").take(4),
                                            trackCount = aAttrs.optInt("trackCount", 0)
                                        )
                                    )
                                }
                            }
                        }
                    }

                    val finalAlbums = fullAlbumsFromView.ifEmpty {
                        allDiscography.filter {
                            !it.title.contains(" - Single", ignoreCase = true) &&
                                !it.title.contains(" - EP", ignoreCase = true) &&
                                (it.trackCount == 0 || it.trackCount > 3)
                        }
                    }
                    val finalSingles = singlesFromView.ifEmpty {
                        allDiscography.filter {
                            it.title.contains(" - Single", ignoreCase = true) ||
                                it.title.contains(" - EP", ignoreCase = true) ||
                                (it.trackCount in 1..3)
                        }
                    }

                    return@withContext CatalogArtistDetail(
                        id = cleanId,
                        name = name,
                        artworkUrl = artworkUrl,
                        heroVideoUrl = heroVideoUrl,
                        bio = bio,
                        genre = genre,
                        origin = origin,
                        formedYear = formedYear,
                        latestRelease = latestRelease ?: finalAlbums.firstOrNull() ?: finalSingles.firstOrNull(),
                        topSongs = topSongs,
                        essentialAlbums = essentialAlbums,
                        albums = finalAlbums,
                        singlesAndEps = finalSingles,
                        appearsOn = appearsOn,
                        playlists = playlists,
                        musicVideos = musicVideos,
                        similarArtists = similarArtists
                    )
                }
            }
        }

        // Fallback via catalog search if artist endpoint didn't resolve
        val searchFallback = searchFullCatalog(fallbackName.ifBlank { cleanId }, limit = 25)
        val matchedArtist = searchFallback.artists.firstOrNull()
        CatalogArtistDetail(
            id = cleanId,
            name = matchedArtist?.title ?: fallbackName.ifBlank { "Artist" },
            artworkUrl = matchedArtist?.artworkUrl ?: searchFallback.songs.firstOrNull()?.artworkUrl.orEmpty(),
            topSongs = searchFallback.songs,
            albums = searchFallback.albums,
            playlists = searchFallback.playlists,
            musicVideos = searchFallback.musicVideos
        )
    }

    private fun parseArtistTrackView(
        viewObj: JSONObject?,
        artistId: String,
        artistName: String,
        defaultType: String = "songs"
    ): List<CatalogTrackResult> {
        val dataArr = viewObj?.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<CatalogTrackResult>()
        for (i in 0 until dataArr.length()) {
            val item = dataArr.optJSONObject(i) ?: continue
            val id = item.optString("id")
            val attrs = item.optJSONObject("attributes") ?: item
            val name = attrs.optString("name")
            if (id.isBlank() || name.isBlank()) continue
            val albumId = item.optJSONObject("relationships")?.optJSONObject("albums")
                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id").orEmpty()
            list.add(
                CatalogTrackResult(
                    id = id,
                    title = name,
                    artist = attrs.optString("artistName").ifBlank { artistName },
                    artistId = artistId,
                    album = attrs.optString("albumName"),
                    albumId = albumId,
                    artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 600, 600),
                    durationMs = attrs.optLong("durationInMillis", 0L),
                    trackNumber = attrs.optInt("trackNumber", i + 1),
                    discNumber = attrs.optInt("discNumber", 1),
                    isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                    previewUrl = attrs.optJSONArray("previews")?.optJSONObject(0)?.optString("url").orEmpty(),
                    releaseYear = attrs.optString("releaseDate").take(4).toIntOrNull(),
                    genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty(),
                    type = defaultType
                )
            )
        }
        return list
    }

    // =========================================================================
    // ONLINE PLAYLIST DETAILS (`openRemotePlaylistView` in `upload.js`)
    // =========================================================================

    suspend fun fetchPlaylistDetails(
        playlistId: String,
        fallbackName: String = ""
    ): CatalogPlaylistDetail? = withContext(Dispatchers.IO) {
        val cleanId = playlistId.trim()
        if (cleanId.isEmpty()) return@withContext null

        runCatching {
            val url = "$API_BASE/playlist?playlist=${encode(cleanId)}&storefront=us&l=en-US" +
                "&extend=editorialVideo,editorialArtwork,editorialNotes,trackCount,extendedAssetUrls" +
                "&include=tracks,curator&include[tracks]=artists,albums,composers&views=animated-artwork"
            val text = httpGet(url, 10000) ?: return@runCatching
            val root = JSONObject(text)
            val plObj = root.optJSONArray("data")?.optJSONObject(0) ?: root.optJSONObject("playlist") ?: root
            val attrs = plObj.optJSONObject("attributes") ?: plObj
            val name = attrs.optString("name").ifBlank { fallbackName }
            if (name.isBlank()) return@runCatching
            val curatorName = attrs.optString("curatorName").ifBlank { "Apple Music" }
            val description = attrs.optJSONObject("description")?.optString("standard")
                ?.ifBlank { attrs.optJSONObject("editorialNotes")?.optString("standard").orEmpty() }
                ?.replace(Regex("<[^>]*>"), "")
                .orEmpty()
            val artworkUrl = resolveArtworkUrl(
                attrs.optJSONObject("artwork")
                    ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("subscriptionHero"),
                1000,
                1000
            )
            val animatedVideoUrl = extractEditorialVideoUrl(attrs.optJSONObject("editorialVideo"))
            val lastModified = attrs.optString("lastModifiedDate").take(10)

            val tracksData = plObj.optJSONObject("relationships")?.optJSONObject("tracks")?.optJSONArray("data")
            val tracks = mutableListOf<CatalogTrackResult>()
            if (tracksData != null) {
                for (i in 0 until tracksData.length()) {
                    val tObj = tracksData.optJSONObject(i) ?: continue
                    val tId = tObj.optString("id")
                    val tAttrs = tObj.optJSONObject("attributes") ?: tObj
                    val tName = tAttrs.optString("name")
                    if (tId.isBlank() || tName.isBlank()) continue
                    val tRels = tObj.optJSONObject("relationships")
                    val tArtistId = tRels?.optJSONObject("artists")?.optJSONArray("data")?.optJSONObject(0)?.optString("id").orEmpty()
                    val tAlbumId = tRels?.optJSONObject("albums")?.optJSONArray("data")?.optJSONObject(0)?.optString("id").orEmpty()
                    tracks.add(
                        CatalogTrackResult(
                            id = tId,
                            title = tName,
                            artist = tAttrs.optString("artistName"),
                            artistId = tArtistId,
                            album = tAttrs.optString("albumName"),
                            albumId = tAlbumId,
                            artworkUrl = resolveArtworkUrl(tAttrs.optJSONObject("artwork"), 500, 500).ifBlank { artworkUrl },
                            durationMs = tAttrs.optLong("durationInMillis", 0L),
                            trackNumber = i + 1,
                            isExplicit = tAttrs.optString("contentRating").equals("explicit", ignoreCase = true),
                            previewUrl = tAttrs.optJSONArray("previews")?.optJSONObject(0)?.optString("url").orEmpty()
                        )
                    )
                }
            }

            // Suggested songs based on first track
            val suggested = tracks.firstOrNull()?.let { first ->
                fetchSimilarSongs(first.id, first.artist).take(6)
            }.orEmpty()

            return@withContext CatalogPlaylistDetail(
                id = cleanId,
                name = name,
                curatorName = curatorName,
                description = description,
                artworkUrl = artworkUrl,
                animatedVideoUrl = animatedVideoUrl,
                updatedText = lastModified,
                tracks = tracks,
                suggestedSongs = suggested
            )
        }

        null
    }

    // =========================================================================
    // SIMILAR SONGS & TRENDING & EDITORIAL NEW (`upload.js`)
    // =========================================================================

    suspend fun fetchSimilarSongs(songId: String, artistName: String = ""): List<CatalogTrackResult> = withContext(Dispatchers.IO) {
        if (songId.isNotBlank()) {
            runCatching {
                val url = "$API_BASE/v1/catalog/kz/songs/${encode(songId)}?views=similar-songs&l=en-US"
                val text = httpGet(url, 7000)
                if (!text.isNullOrBlank()) {
                    val root = JSONObject(text)
                    val songObj = root.optJSONArray("data")?.optJSONObject(0)
                    val simView = songObj?.optJSONObject("views")?.optJSONObject("similar-songs")
                    val list = parseArtistTrackView(simView, "", artistName)
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        }
        if (artistName.isNotBlank()) {
            return@withContext searchCatalog(artistName, limit = 15).filter { it.id != songId }
        }
        emptyList()
    }

    suspend fun fetchTrendingTracks(limit: Int = 35): List<CatalogTrackResult> = withContext(Dispatchers.IO) {
        runCatching {
            val text = httpGet("$API_BASE/v1/catalog/trending?limit=$limit", 8000)
            if (!text.isNullOrBlank()) {
                val root = JSONObject(text)
                val arr = root.optJSONArray("trending") ?: root.optJSONArray("data")
                if (arr != null && arr.length() > 0) {
                    val list = mutableListOf<CatalogTrackResult>()
                    for (i in 0 until arr.length()) {
                        val item = arr.optJSONObject(i) ?: continue
                        val id = item.optString("song_id").ifBlank { item.optString("id") }
                        val title = item.optString("title").ifBlank { item.optString("name") }
                        val artist = item.optString("artist").ifBlank { item.optString("artistName") }
                        if (id.isBlank() || title.isBlank()) continue
                        val art = item.optString("artwork_url")
                            .ifBlank { resolveArtworkUrl(item.optJSONObject("artwork"), 600, 600) }
                        list.add(
                            CatalogTrackResult(
                                id = id,
                                title = title,
                                artist = artist,
                                album = item.optString("album").ifBlank { item.optString("albumName") },
                                albumId = item.optString("album_id"),
                                artistId = item.optString("artist_id"),
                                artworkUrl = art,
                                playCount = item.optInt("play_count", 0)
                            )
                        )
                    }
                    if (list.isNotEmpty()) return@withContext list
                }
            }
        }
        // Fallback to top hits catalog search
        searchCatalog("Top Hits 2025", limit = 20)
    }

    suspend fun fetchEditorialNewShelves(): List<EditorialShelf> = withContext(Dispatchers.IO) {
        val shelves = mutableListOf<EditorialShelf>()
        runCatching {
            val url = "$API_BASE/v1/editorial/kz/groupings?art%5Bmusic-videos%3Aurl%5D=c&art%5Burl%5D=f" +
                "&extend=editorialArtwork,editorialNotes,editorialVideo&format%5Bresources%5D=map" +
                "&l=en-GB&name=music&omit%5Bresource%5D=autos&platform=web"
            val text = httpGet(url, 9000)
            if (!text.isNullOrBlank()) {
                val root = JSONObject(text)
                val resources = root.optJSONObject("resources")
                if (resources != null) {
                    // Extract albums, playlists, and songs from editorial resources
                    val editorialAlbums = mutableListOf<LyricsFlowCatalogItem>()
                    resources.optJSONObject("albums")?.let { albumsMap ->
                        val keys = albumsMap.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val obj = albumsMap.optJSONObject(k) ?: continue
                            val attrs = obj.optJSONObject("attributes") ?: continue
                            val name = attrs.optString("name")
                            if (name.isBlank()) continue
                            editorialAlbums.add(
                                LyricsFlowCatalogItem(
                                    id = k,
                                    type = "albums",
                                    title = name,
                                    subtitle = attrs.optString("artistName"),
                                    albumName = name,
                                    albumId = k,
                                    artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 600, 600),
                                    isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                                    releaseYear = attrs.optString("releaseDate").take(4)
                                )
                            )
                        }
                    }

                    val editorialPlaylists = mutableListOf<LyricsFlowCatalogItem>()
                    resources.optJSONObject("playlists")?.let { plMap ->
                        val keys = plMap.keys()
                        while (keys.hasNext()) {
                            val k = keys.next()
                            val obj = plMap.optJSONObject(k) ?: continue
                            val attrs = obj.optJSONObject("attributes") ?: continue
                            val name = attrs.optString("name")
                            if (name.isBlank()) continue
                            editorialPlaylists.add(
                                LyricsFlowCatalogItem(
                                    id = k,
                                    type = "playlists",
                                    title = name,
                                    subtitle = attrs.optString("curatorName", "Apple Music"),
                                    artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 600, 600),
                                    editorialSubtitle = attrs.optJSONObject("editorialNotes")?.optString("short").orEmpty()
                                )
                            )
                        }
                    }

                    if (editorialAlbums.isNotEmpty()) {
                        shelves.add(
                            EditorialShelf(
                                id = "new_releases",
                                title = "New Releases",
                                subtitle = "Updated Daily on Apple Music",
                                items = editorialAlbums.take(20)
                            )
                        )
                    }
                    if (editorialPlaylists.isNotEmpty()) {
                        shelves.add(
                            EditorialShelf(
                                id = "featured_playlists",
                                title = "Featured Playlists",
                                subtitle = "Curated for LyricsFlow",
                                items = editorialPlaylists.take(20)
                            )
                        )
                    }
                }
            }
        }
        shelves
    }

    private fun extractEditorialVideoUrl(editorialVideo: JSONObject?): String? {
        if (editorialVideo == null) return null
        val candidateKeys = listOf(
            "motionDetailTall",
            "motionArtistFullscreen16x9",
            "motionArtistWide16x9",
            "motionDetailSquare",
            "motionSquareVideo1x1",
            "motionArtistSquare1x1"
        )
        for (key in candidateKeys) {
            val videoUrl = editorialVideo.optJSONObject(key)?.optString("video")?.takeIf { it.isNotBlank() }
            if (videoUrl != null) return videoUrl
        }
        return null
    }

    /**
     * Port of `resolveArtworkUrl` from `main/js/amp-parser.js`.
     */
    fun resolveArtworkUrl(artworkObj: JSONObject?, width: Int = 600, height: Int = 600): String {
        if (artworkObj == null) return ""
        val template = artworkObj.optString("url").takeIf { it.isNotBlank() }
            ?: artworkObj.optJSONObject("subscriptionHero")?.optString("url")
            ?: artworkObj.optJSONObject("storeFlowcase")?.optString("url")
            ?: return ""
        return template
            .replace("{w}", width.toString())
            .replace("{h}", height.toString())
            .replace("{c}", "bb")
            .replace("{f}", "jpg")
    }

    private fun resolveItunesTrackId(title: String, artist: String): String? {
        if (title.isBlank()) return null
        return runCatching {
            val term = encode("$title $artist")
            val text = httpGet("$ITUNES_SEARCH_API?term=$term&entity=song&limit=1", 5000) ?: return null
            val results = JSONObject(text).optJSONArray("results") ?: return null
            if (results.length() > 0) {
                val id = results.optJSONObject(0)?.optLong("trackId", 0L) ?: 0L
                if (id > 0L) id.toString() else null
            } else {
                null
            }
        }.getOrNull()
    }

    private fun getCacheFile(context: Context, title: String, artist: String, album: String): File {
        val key = "${title.lowercase(Locale.ROOT)}|${artist.lowercase(Locale.ROOT)}|${album.lowercase(Locale.ROOT)}"
        val hash = MessageDigest.getInstance("SHA-256")
            .digest(key.toByteArray(Charsets.UTF_8))
            .joinToString("") { "%02x".format(Locale.ROOT, it) }
        return File(File(context.filesDir, CACHE_DIR_NAME), "$hash.lyric")
    }

    private fun httpGet(urlString: String, timeoutMs: Int = 8000): String? {
        var conn: HttpURLConnection? = null
        return try {
            conn = (URL(urlString).openConnection() as HttpURLConnection).apply {
                connectTimeout = timeoutMs
                readTimeout = timeoutMs
                requestMethod = "GET"
                setRequestProperty("User-Agent", "LyricsFlow-Android/2.1")
                setRequestProperty("Accept", "application/json, application/xml, text/plain, */*")
            }
            if (conn.responseCode !in 200..299) return null
            conn.inputStream.bufferedReader(Charsets.UTF_8).use { it.readText() }
        } catch (_: Exception) {
            null
        } finally {
            conn?.disconnect()
        }
    }

    private fun encode(value: String): String = URLEncoder.encode(value, "UTF-8")

    private fun formatSec(sec: Double): String = String.format(Locale.US, "%.3f", sec.coerceAtLeast(0.0))

    private fun escapeXml(str: String): String = buildString(str.length) {
        for (ch in str) {
            when (ch) {
                '&' -> append("&amp;")
                '<' -> append("&lt;")
                '>' -> append("&gt;")
                '"' -> append("&quot;")
                '\'' -> append("&apos;")
                else -> append(ch)
            }
        }
    }
}
