package com.lyricsflow.player.code.utils.lrc

import android.content.Context
import android.net.Uri
import android.util.Log
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem
import com.lyricsflow.player.data.libraries.SettingsLibrary
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
import java.text.SimpleDateFormat
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap

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
    val songs: List<LyricsFlowApi.CatalogTrackResult> = emptyList(),
    val artists: List<LyricsFlowCatalogItem> = emptyList(),
    val albums: List<LyricsFlowCatalogItem> = emptyList(),
    val playlists: List<LyricsFlowCatalogItem> = emptyList(),
    val musicVideos: List<LyricsFlowApi.CatalogTrackResult> = emptyList(),
    val stations: List<LyricsFlowCatalogItem> = emptyList(),
    val recordLabels: List<LyricsFlowCatalogItem> = emptyList(),
    val curators: List<LyricsFlowCatalogItem> = emptyList()
)

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

    private val artistAvatarCache = ConcurrentHashMap<String, String>()

    data class CatalogAlbumDetail(
        val id: String,
        val name: String,
        val artistName: String,
        val artistId: String = "",
        val artworkUrl: String = "",
        val animatedVideoUrl: String? = null,
        val bgColorHex: String = "",
        val audioTraits: List<String> = emptyList(),
        val popularTrackIds: Set<String> = emptySet(),
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

    data class CatalogConcertItem(
        val id: String,
        val title: String,
        val dateText: String,
        val monthShort: String,
        val dayNumber: String,
        val fullDateLine: String,
        val doorsLine: String,
        val venueName: String,
        val cityCountry: String,
        val ticketUrl: String = ""
    )

    data class CatalogArtistDetail(
        val id: String,
        val name: String,
        val artworkUrl: String = "",
        val heroVideoUrl: String? = null,
        val logoUrl: String = "",
        val keyColorHex: String = "",
        val upcomingConcertBadge: String = "",
        val upcomingConcerts: List<CatalogConcertItem> = emptyList(),
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

    fun getStreamUrl(songId: String, codec: String = ""): String {
        val resolvedCodec = codec.ifBlank {
            when {
                runCatching { SettingsLibrary.EnableDolbyAtmos }.getOrDefault(false) -> "atmos"
                runCatching { SettingsLibrary.EnableLosslessStreaming }.getOrDefault(false) -> "alac"
                else -> "aac"
            }
        }
        return if (resolvedCodec == "alac" || resolvedCodec == "atmos" || resolvedCodec == "ec3") {
            "$API_BASE/download?song=${encode(songId)}&codec=$resolvedCodec&l=en-US&fallback=true"
        } else {
            "$API_BASE/stream?song=${encode(songId)}&codec=$resolvedCodec&fallback=false&l=en-US"
        }
    }

    suspend fun resolveFallbackStreamUrl(songId: String, currentUri: String): String? = withContext(Dispatchers.IO) {
        val cleanId = songId.trim()
        if (cleanId.isEmpty() || cleanId.startsWith("local_") || cleanId.startsWith("upload_")) return@withContext null
        when {
            currentUri.contains("codec=atmos") || currentUri.contains("codec=ec3") ->
                "$API_BASE/download?song=${encode(cleanId)}&codec=alac&l=en-US&fallback=true"
            currentUri.contains("codec=alac") ->
                "$API_BASE/stream?song=${encode(cleanId)}&codec=aac&fallback=false&l=en-US"
            currentUri.contains("codec=aac&") -> "$API_BASE/stream?song=${encode(cleanId)}&codec=aac-web&fallback=false&l=en-US"
            currentUri.contains("codec=aac-web") -> "$API_BASE/download?song=${encode(cleanId)}&l=en-US"
            currentUri.contains("/download?") -> {
                runCatching {
                    val lookupText = httpGet("https://itunes.apple.com/lookup?id=${encode(cleanId)}", 5000) ?: return@runCatching null
                    val results = JSONObject(lookupText).optJSONArray("results") ?: return@runCatching null
                    if (results.length() > 0) {
                        results.optJSONObject(0)?.optString("previewUrl")?.takeIf { it.isNotBlank() }
                    } else null
                }.getOrNull()
            }
            else -> null
        }
    }

    fun CatalogTrackResult.toLyricsflowMediaItem(): LyricsflowMediaItem {
        val streamUriStr = if (id.isNotBlank() && !id.startsWith("local_")) {
            getStreamUrl(id)
        } else {
            previewUrl.ifBlank { getStreamUrl(id) }
        }
        val parsedArtUri = artworkUrl.takeIf { it.isNotBlank() }?.let { Uri.parse(it) }
        return LyricsflowMediaItem(
            uri = Uri.parse(streamUriStr),
            mediaId = id.ifBlank { "${title}_${artist}" },
            mimeType = null,
            title = title,
            writer = composerName.ifBlank { artist },
            compilation = if (isExplicit) "explicit" else null,
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
        music: LyricsflowMediaItem,
        durationMs: Long = 0L
    ): LyricsflowParsedLyricData? = withContext(Dispatchers.IO) {
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
        // Resolve Apple Music track ID up front if missing (matching `main/js/ttml-retriever.js` line 795)
        val resolvedTrackId = amTrackId?.takeIf { it.isNotBlank() }
            ?: runCatching { resolveItunesTrackId(title, artist) }.getOrNull()

        val deferredMap = mapOf(
            LyricSource.LF_COMMUNITY to async(Dispatchers.IO) {
                runCatching { fetchLfCommunity(title, artist, resolvedTrackId) }.getOrNull()
            },
            LyricSource.LYRICSFLOW to async(Dispatchers.IO) {
                runCatching { fetchLyricsFlowApple(title, artist, album, durationSec, resolvedTrackId) }.getOrNull()
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

    fun parseRawLyricText(rawText: String): LyricsflowParsedLyricData? {
        val trimmed = rawText.trim()
        if (trimmed.isEmpty()) return null
        if (LyricsflowTtmlFactory.isTtmlText(trimmed)) {
            LyricsflowTtmlFactory().formatTtmlEntries(trimmed)?.let { return it }
        }
        var entries = LyricsflowLrcFactory().formatLrcEntries(trimmed)
        if (entries.isEmpty()) {
            // Fallback for plain unsynced lyrics so the user never sees "Lyrics not ready" when plain text lyrics exist
            val plainLines = trimmed.lines().map { it.trim() }.filter { it.isNotEmpty() && !it.startsWith("[") }
            if (plainLines.isEmpty()) return null
            entries = plainLines.mapIndexed { idx, lineText ->
                val t = idx * 3500f
                listOf(t to lineText, t to "")
            }
        }
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
            val isWordSyncedLine = segments.size > 1 || segments.any { it.first > lineStart + 10f }
            if (!isWordSyncedLine) {
                emptyList()
            } else {
                var prevEnd = lineStart
                segments.mapIndexed { segIdx, pair ->
                    val segEnd = when {
                        pair.first > prevEnd + 5f -> pair.first
                        segIdx < segments.lastIndex -> {
                            segments.getOrNull(segIdx + 1)?.first?.takeIf { it > prevEnd + 5f }
                                ?: (prevEnd + 220f)
                        }
                        else -> lineEnd.coerceAtLeast(prevEnd + 280f)
                    }
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
        val lrcOtherSide = com.lyricsflow.player.data.objects.MediaViewModelObject.otherSideForLines.toList()
        val normalizedOtherSide = if (lrcOtherSide.size == entries.size && !lrcOtherSide.all { it }) {
            lrcOtherSide
        } else {
            List(entries.size) { false }
        }
        return LyricsflowParsedLyricData(
            entries = entries,
            endTimes = endTimes,
            otherSideForLines = normalizedOtherSide,
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
        val available = json.optJSONArray("available")
        val entry = if (available != null && available.length() > 0) {
            available.optJSONObject(0)
        } else {
            json.optJSONObject("entry")
        } ?: return null
        val streamUrl = entry.optString("stream_url").takeIf { it.isNotBlank() } ?: return null
        val fullUrl = if (streamUrl.startsWith("http")) streamUrl else "$API_BASE$streamUrl"
        val ttml = httpGet(fullUrl, 8000)?.takeIf { LyricsflowTtmlFactory.isTtmlText(it) } ?: return null
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
        if (LyricsflowTtmlFactory.isTtmlText(trimmed)) {
            return FetchedLyricPayload(trimmed, true, "LyricsFlow")
        }
        return runCatching {
            val json = JSONObject(trimmed)
            val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
                ?: json.optString("ttml").takeIf { it.isNotBlank() }
                ?: json.optString("lyrics").takeIf { it.isNotBlank() }
                ?: return null
            FetchedLyricPayload(synced, LyricsflowTtmlFactory.isTtmlText(synced), "LyricsFlow")
        }.getOrNull()
    }

    private fun fetchBetterLyrics(title: String, artist: String): FetchedLyricPayload? {
        if (title.isBlank()) return null
        val url = "$BETTER_LYRICS_API?s=${encode(title)}&a=${encode(artist)}"
        val responseText = httpGet(url, 8000) ?: return null
        val json = JSONObject(responseText)
        val ttml = json.optString("ttml").takeIf { it.isNotBlank() } ?: return null
        return FetchedLyricPayload(ttml, LyricsflowTtmlFactory.isTtmlText(ttml), "BetterLyrics")
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
        val responseText = httpGet("$LRCLIB_API?$params", 7000)
            ?: if (album.isNotBlank() || durationSec > 0) {
                httpGet("$LRCLIB_API?track_name=${encode(title)}&artist_name=${encode(artist)}", 7000)
            } else {
                null
            }
            ?: return null
        val json = JSONObject(responseText)
        val synced = json.optString("syncedLyrics").takeIf { it.isNotBlank() }
            ?: json.optString("plainLyrics").takeIf { it.isNotBlank() }
            ?: return null
        return FetchedLyricPayload(synced, false, "LRCLIB")
    }

    private fun convertLyricsPlusToTtml(data: JSONObject): String? {
        val lyrics = data.optJSONArray("lyrics") ?: return null
        if (lyrics.length() == 0) return null
        var hasWordSync = false
        for (i in 0 until lyrics.length()) {
            val syllabus = lyrics.optJSONObject(i)?.optJSONArray("syllabus")
            if (syllabus != null && syllabus.length() > 0) {
                hasWordSync = true
                break
            }
        }
        val timingAttr = if (hasWordSync) "Word" else "Line"
        val sb = StringBuilder()
        sb.append("""<?xml version="1.0" encoding="UTF-8"?>""")
        sb.append("""<tt xmlns="http://www.w3.org/ns/ttml" xmlns:ttm="http://www.w3.org/ns/ttml#metadata" xmlns:itunes="http://music.apple.com/lyric-ttml-internal" itunes:timing="$timingAttr"><body><div>""")

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
                        val artName = item.optString("artistName")
                        val resolvedAvatar = art.ifBlank { fetchArtistAvatarUrl(artName, artId) }
                        artists.add(
                            LyricsFlowCatalogItem(
                                id = artId,
                                type = "artists",
                                title = artName,
                                subtitle = item.optString("primaryGenreName", "Artist"),
                                artistId = artId,
                                artworkUrl = resolvedAvatar
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
                    val artworkUrl = extractBestArtistOrEntityArtwork(attrs, 600)
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
                        val artworkUrl = extractBestArtistOrEntityArtwork(attrs, 600)
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

        // 1. Primary `/album?album=...` AND `/v1/catalog/kz/albums/${cleanId}` endpoints from `upload.js`
        val endpoints = listOf(
            "$API_BASE/album?album=${encode(cleanId)}&l=en-US" +
                "&art[url]=c,f&include=artists,tracks,record-labels,music-videos" +
                "&views=appears-on,other-versions,related-albums,related-videos" +
                "&extend=artistUrl,editorialArtwork,editorialNotes,editorialVideo,composerName,audioTraits",
            "$API_BASE/v1/catalog/kz/albums/${encode(cleanId)}?art[url]=c,f" +
                "&extend=artistUrl,editorialArtwork,editorialNotes,editorialVideo,composerName,audioTraits" +
                "&format[resources]=map&include=artists,tracks,record-labels,music-videos" +
                "&views=appears-on,other-versions,related-albums,related-videos&l=en-GB&platform=web"
        )

        for (url in endpoints) {
            runCatching {
                val text = httpGet(url, 10000) ?: return@runCatching
                val root = JSONObject(text)
                val resources = root.optJSONObject("resources")
                val rawDataObj = root.optJSONObject("raw_data")

                // Unwrap `raw_data.data[0]`, `resources.albums[cleanId]`, `data[0]`, or `album` (`upload.js` lines 3240-3326)
                val albumObj = rawDataObj?.optJSONArray("data")?.optJSONObject(0)
                    ?: root.optJSONArray("data")?.optJSONObject(0)
                    ?: resources?.optJSONObject("albums")?.let { albumsMap ->
                        albumsMap.optJSONObject(cleanId) ?: albumsMap.keys().asSequence().firstOrNull()?.let { albumsMap.optJSONObject(it) }
                    }
                    ?: root.optJSONObject("album")
                    ?: root

                val attrs = albumObj.optJSONObject("attributes") ?: albumObj
                val name = attrs.optString("name").ifBlank { root.optString("album_name") }.ifBlank { fallbackTitle }
                if (name.isNotBlank()) {
                    val artistName = attrs.optString("artistName")
                        .ifBlank { root.optString("artist_name") }
                        .ifBlank { fallbackArtist }
                    val rels = albumObj.optJSONObject("relationships")
                    val artistId = rels?.optJSONObject("artists")?.optJSONArray("data")
                        ?.optJSONObject(0)?.optString("id")
                        ?.ifBlank { root.optString("artist_id") }
                        ?: root.optString("artist_id")

                    val artworkObj = attrs.optJSONObject("artwork")
                        ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("subscriptionHero")
                        ?: attrs.optJSONObject("editorialArtwork")?.optJSONObject("storeFlowcase")
                    val artworkUrl = resolveArtworkUrl(artworkObj, 1200, 1200)
                        .ifBlank { cleanArtworkUrlString(root.optString("artwork_url"), 1200) }
                    val bgColorHex = artworkObj?.optString("bgColor")?.removePrefix("#").orEmpty()

                    val editorialVideoUrl = extractEditorialVideoUrl(attrs.optJSONObject("editorialVideo"))
                    val releaseDate = attrs.optString("releaseDate")
                    val releaseYear = releaseDate.take(4)
                    val genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty()
                    val editorialNotes = attrs.optJSONObject("editorialNotes")?.let { notes ->
                        notes.optString("standard")
                            .ifBlank { notes.optString("short") }
                            .ifBlank { notes.optString("tagline") }
                    }.orEmpty()
                        .ifBlank { attrs.optString("plainEditorialNotes") }
                        .replace(Regex("<[^>]*>"), "")
                        .trim()

                    val copyright = attrs.optString("copyright")
                    val recordLabel = attrs.optString("recordLabel").ifBlank {
                        val rlData = rels?.optJSONObject("record-labels")?.optJSONArray("data")?.optJSONObject(0)
                        val rlId = rlData?.optString("id").orEmpty()
                        rlData?.optJSONObject("attributes")?.optString("name")
                            ?: resources?.optJSONObject("record-labels")?.optJSONObject(rlId)?.optJSONObject("attributes")?.optString("name")
                            ?: ""
                    }
                    val recordLabelId = rels?.optJSONObject("record-labels")?.optJSONArray("data")
                        ?.optJSONObject(0)?.optString("id").orEmpty()
                    val isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true)
                    val isSingleOrEp = attrs.optBoolean("isSingle", false) ||
                        name.contains(" - Single", ignoreCase = true) ||
                        name.contains(" - EP", ignoreCase = true)

                    val audioTraits = mutableListOf<String>()
                    attrs.optJSONArray("audioTraits")?.let { arr ->
                        for (i in 0 until arr.length()) {
                            arr.optString(i)?.takeIf { it.isNotBlank() }?.let { audioTraits.add(it) }
                        }
                    }

                    // Parse tracks (`relationships.tracks.data` with `resources.songs` dereferencing OR `parsed_tracks` from `upload.js`)
                    val tracks = mutableListOf<CatalogTrackResult>()
                    val popularTrackIds = mutableSetOf<String>()
                    val tracksData = rels?.optJSONObject("tracks")?.optJSONArray("data")
                        ?: attrs.optJSONArray("tracks")

                    if (tracksData != null && tracksData.length() > 0) {
                        for (i in 0 until tracksData.length()) {
                            val rawTrack = tracksData.optJSONObject(i) ?: continue
                            val rawId = rawTrack.optString("id")
                            val rawType = rawTrack.optString("type").ifBlank { "songs" }
                            val tObj = if (resources != null && rawId.isNotBlank()) {
                                resources.optJSONObject(rawType)?.optJSONObject(rawId)
                                    ?: resources.optJSONObject("songs")?.optJSONObject(rawId)
                                    ?: rawTrack
                            } else {
                                rawTrack
                            }
                            val tAttrs = tObj.optJSONObject("attributes") ?: tObj
                            val tId = tObj.optString("id").ifBlank { rawId }
                            val tName = tAttrs.optString("name").ifBlank { tAttrs.optString("title") }
                            if (tId.isBlank() || tName.isBlank()) continue
                            val tArt = resolveArtworkUrl(tAttrs.optJSONObject("artwork"), 600, 600).ifBlank { artworkUrl }
                            val tArtistId = tObj.optJSONObject("relationships")?.optJSONObject("artists")
                                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id")
                                ?.ifBlank { artistId } ?: artistId
                            if (tAttrs.optBoolean("isMasteredForItunes", false) || i < 2) {
                                popularTrackIds.add(tId)
                            }
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
                                    type = rawType
                                )
                            )
                        }
                    }

                    // Fallback to `parsed_tracks` array in `/album` response (`upload.js` line 3264)
                    if (tracks.isEmpty()) {
                        val parsedTracks = root.optJSONArray("parsed_tracks")
                        if (parsedTracks != null) {
                            for (i in 0 until parsedTracks.length()) {
                                val pt = parsedTracks.optJSONObject(i) ?: continue
                                val tId = pt.optString("id").ifBlank { pt.optString("song_id") }
                                val tName = pt.optString("name").ifBlank { pt.optString("title") }
                                if (tId.isBlank() || tName.isBlank()) continue
                                val tArt = cleanArtworkUrlString(pt.optString("artwork_url"), 600).ifBlank { artworkUrl }
                                tracks.add(
                                    CatalogTrackResult(
                                        id = tId,
                                        title = tName,
                                        artist = pt.optString("artist").ifBlank { pt.optString("artistName") }.ifBlank { artistName },
                                        artistId = pt.optString("artist_id").ifBlank { artistId },
                                        album = name,
                                        albumId = cleanId,
                                        artworkUrl = tArt,
                                        durationMs = pt.optLong("duration_ms", pt.optLong("durationInMillis", 0L)),
                                        trackNumber = pt.optInt("track_number", pt.optInt("trackNumber", i + 1)),
                                        discNumber = pt.optInt("disc_number", pt.optInt("discNumber", 1)),
                                        isExplicit = pt.optBoolean("explicit", false) ||
                                            pt.optString("contentRating").equals("explicit", ignoreCase = true),
                                        previewUrl = pt.optString("preview_url").ifBlank { pt.optString("previewUrl") },
                                        releaseYear = releaseYear.toIntOrNull(),
                                        genre = genre,
                                        composerName = pt.optString("composerName")
                                    )
                                )
                            }
                        }
                    }

                    // Parse related / more by artist (dereferencing `resources.albums` when `format[resources]=map` is used)
                    val views = albumObj.optJSONObject("views")
                    val relatedAlbums = parseAlbumShelfView(views?.optJSONObject("related-albums"), resources)
                    val otherVersions = parseAlbumShelfView(views?.optJSONObject("other-versions"), resources)
                    val moreByArtist = if (artistId.isNotBlank()) {
                        fetchArtistAlbumsList(artistId, excludeAlbumId = cleanId)
                    } else {
                        otherVersions
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
                            bgColorHex = bgColorHex,
                            audioTraits = audioTraits,
                            popularTrackIds = popularTrackIds,
                            genre = genre,
                            releaseYear = releaseYear,
                            releaseDateFormatted = formatReleaseDateHuman(releaseDate),
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
                    releaseDateFormatted = formatReleaseDateHuman(releaseDate),
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
            val text = httpGet("$API_BASE/artist/albums?artist=${encode(artistId)}&limit=25&l=en-US", 6000)
                ?: httpGet("$API_BASE/artist/albums?artistId=${encode(artistId)}&limit=25&l=en-US", 6000)
                ?: return emptyList()
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
                        artworkUrl = resolveArtworkUrl(attrs.optJSONObject("artwork"), 600, 600),
                        isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                        releaseYear = attrs.optString("releaseDate").take(4),
                        trackCount = attrs.optInt("trackCount", 0)
                    )
                )
            }
            list
        }.getOrDefault(emptyList())
    }

    private fun parseAlbumShelfView(viewObj: JSONObject?, resources: JSONObject? = null): List<LyricsFlowCatalogItem> {
        val dataArr = viewObj?.optJSONArray("data") ?: return emptyList()
        val list = mutableListOf<LyricsFlowCatalogItem>()
        for (i in 0 until dataArr.length()) {
            val rawItem = dataArr.optJSONObject(i) ?: continue
            val rawId = rawItem.optString("id")
            val rawType = rawItem.optString("type", "albums")
            val item = if (resources != null && rawId.isNotBlank()) {
                resources.optJSONObject(rawType)?.optJSONObject(rawId) ?: rawItem
            } else {
                rawItem
            }
            val id = item.optString("id").ifBlank { rawId }
            val attrs = item.optJSONObject("attributes") ?: item
            val name = attrs.optString("name")
            if (id.isBlank() || name.isBlank()) continue
            val releaseDate = attrs.optString("releaseDate")
            list.add(
                LyricsFlowCatalogItem(
                    id = id,
                    type = rawType,
                    title = name,
                    subtitle = attrs.optString("artistName").ifBlank { attrs.optString("curatorName") },
                    albumName = name,
                    albumId = id,
                    artworkUrl = extractBestArtistOrEntityArtwork(attrs, 600),
                    isExplicit = attrs.optString("contentRating").equals("explicit", ignoreCase = true),
                    releaseYear = releaseDate.take(4),
                    editorialSubtitle = formatReleaseDateHuman(releaseDate),
                    trackCount = attrs.optInt("trackCount", 0)
                )
            )
        }
        return list
    }

    // =========================================================================
    // ONLINE ARTIST DETAILS (`openArtistView` in `upload.js` & `parseAmpArtistResponse` in `amp-parser.js`)
    // =========================================================================

    /**
     * Resolves an artist's actual Apple Music portrait photo (cached in-memory).
     * Never returns a random album cover unless no artist entity exists on Apple Music.
     */
    suspend fun fetchArtistAvatarUrl(artistName: String, artistId: String = ""): String = withContext(Dispatchers.IO) {
        val cleanName = artistName.trim()
        val cacheKey = (artistId.takeIf { it.all(Char::isDigit) && it.isNotBlank() } ?: cleanName.lowercase(Locale.ROOT)).trim()
        if (cacheKey.isBlank()) return@withContext ""
        artistAvatarCache[cacheKey]?.takeIf { it.isNotBlank() }?.let { return@withContext it }

        // 1. If we have a numeric artistId, query `/v1/catalog/kz/artists/$artistId` directly
        if (artistId.isNotBlank() && artistId.all(Char::isDigit)) {
            runCatching {
                val url = "$API_BASE/v1/catalog/kz/artists/$artistId?art[url]=c,f&extend=editorialArtwork,hero&l=en-GB&platform=web"
                val text = httpGet(url, 6000)
                if (!text.isNullOrBlank()) {
                    val root = JSONObject(text)
                    val artistObj = root.optJSONArray("data")?.optJSONObject(0)
                        ?: root.optJSONObject("resources")?.optJSONObject("artists")?.optJSONObject(artistId)
                    val attrs = artistObj?.optJSONObject("attributes")
                    val resolved = extractBestArtistOrEntityArtwork(attrs, 800)
                    if (resolved.isNotBlank()) {
                        artistAvatarCache[cacheKey] = resolved
                        if (cleanName.isNotBlank()) artistAvatarCache[cleanName.lowercase(Locale.ROOT)] = resolved
                        return@withContext resolved
                    }
                }
            }
        }

        if (cleanName.isBlank()) return@withContext ""

        // 2. Search AMP catalog specifically for `types=artists`
        runCatching {
            val url = "$API_BASE/v1/catalog/kz/search?term=${encode(cleanName)}&types=artists&limit=5&art[url]=c,f&extend=editorialArtwork,hero&format[resources]=map&l=en-GB&platform=web"
            val text = httpGet(url, 6000)
            if (!text.isNullOrBlank()) {
                val root = JSONObject(text)
                val artistsMap = root.optJSONObject("resources")?.optJSONObject("artists")
                if (artistsMap != null) {
                    var bestUrl = ""
                    val keys = artistsMap.keys()
                    while (keys.hasNext()) {
                        val k = keys.next()
                        val obj = artistsMap.optJSONObject(k) ?: continue
                        val attrs = obj.optJSONObject("attributes") ?: continue
                        val name = attrs.optString("name")
                        val art = extractBestArtistOrEntityArtwork(attrs, 800)
                        if (art.isNotBlank()) {
                            if (name.equals(cleanName, ignoreCase = true)) {
                                bestUrl = art
                                break
                            } else if (bestUrl.isBlank()) {
                                bestUrl = art
                            }
                        }
                    }
                    if (bestUrl.isNotBlank()) {
                        artistAvatarCache[cacheKey] = bestUrl
                        artistAvatarCache[cleanName.lowercase(Locale.ROOT)] = bestUrl
                        return@withContext bestUrl
                    }
                }
                val dataArr = root.optJSONObject("results")?.optJSONObject("artists")?.optJSONArray("data")
                if (dataArr != null && dataArr.length() > 0) {
                    for (i in 0 until dataArr.length()) {
                        val obj = dataArr.optJSONObject(i) ?: continue
                        val attrs = obj.optJSONObject("attributes") ?: obj
                        val art = extractBestArtistOrEntityArtwork(attrs, 800)
                        if (art.isNotBlank()) {
                            artistAvatarCache[cacheKey] = art
                            artistAvatarCache[cleanName.lowercase(Locale.ROOT)] = art
                            return@withContext art
                        }
                    }
                }
            }
        }

        // 3. Fallback to `/search?term=...&types=artists`
        runCatching {
            val url = "$API_BASE/search?term=${encode(cleanName)}&types=artists&limit=3&l=en-US"
            val text = httpGet(url, 6000)
            if (!text.isNullOrBlank()) {
                val parsed = parseAmpOrStandardSearchJson(JSONObject(text))
                val art = parsed.artists.firstOrNull { it.artworkUrl.isNotBlank() }?.artworkUrl.orEmpty()
                if (art.isNotBlank()) {
                    artistAvatarCache[cacheKey] = art
                    artistAvatarCache[cleanName.lowercase(Locale.ROOT)] = art
                    return@withContext art
                }
            }
        }

        ""
    }

    suspend fun fetchArtistDetails(
        artistId: String,
        fallbackName: String = ""
    ): CatalogArtistDetail? = withContext(Dispatchers.IO) {
        val rawId = artistId.trim()
        val searchName = fallbackName.trim().ifBlank { rawId }
        if (rawId.isEmpty() && searchName.isEmpty()) return@withContext null

        // Step 1: Resolve numeric Apple Music artist ID (`upload.js` lines 3655-3686)
        var resolvedArtistId = if (rawId.isNotBlank() && rawId.all(Char::isDigit)) rawId else ""
        if (resolvedArtistId.isBlank() && searchName.isNotBlank()) {
            runCatching {
                val searchUrl = "$API_BASE/v1/catalog/kz/search?term=${encode(searchName)}&types=artists&limit=5&format[resources]=map&l=en-GB&platform=web"
                val text = httpGet(searchUrl, 6000)
                if (!text.isNullOrBlank()) {
                    val root = JSONObject(text)
                    val artistsMap = root.optJSONObject("resources")?.optJSONObject("artists")
                    if (artistsMap != null) {
                        val keys = artistsMap.keys()
                        var firstId = ""
                        while (keys.hasNext()) {
                            val k = keys.next()
                            if (firstId.isBlank()) firstId = k
                            val n = artistsMap.optJSONObject(k)?.optJSONObject("attributes")?.optString("name").orEmpty()
                            if (n.equals(searchName, ignoreCase = true)) {
                                resolvedArtistId = k
                                break
                            }
                        }
                        if (resolvedArtistId.isBlank()) resolvedArtistId = firstId
                    }
                }
            }
            if (resolvedArtistId.isBlank()) {
                runCatching {
                    val itunesText = httpGet("$ITUNES_SEARCH_API?term=${encode(searchName)}&entity=musicArtist&limit=3", 5000)
                    if (!itunesText.isNullOrBlank()) {
                        val results = JSONObject(itunesText).optJSONArray("results")
                        if (results != null && results.length() > 0) {
                            resolvedArtistId = results.optJSONObject(0)?.optLong("artistId", 0L)?.takeIf { it > 0L }?.toString().orEmpty()
                        }
                    }
                }
            }
        }

        // Step 2: Fetch rich AMP Compound Artist Endpoint (`upload.js` line 3691 & `amp-parser.js` line 8)
        // plus `/artist` and `/artist/albums` in parallel
        if (resolvedArtistId.isNotBlank()) {
            val ampCompoundDeferred = async(Dispatchers.IO) {
                val ampUrl = "$API_BASE/v1/catalog/kz/artists/${encode(resolvedArtistId)}" +
                    "?art[url]=c,f" +
                    "&extend=artistBio,editorialArtwork,editorialVideo,extendedAssetUrls,hasBiographyContent,hero,keyColor,plainEditorialNotes,seoDescription,seoTitle" +
                    "&extend[canvas-shelves-header]=badge&extend[concerts]=tickets" +
                    "&filter[canvas-shelves-collection.contents.type]=albums,apple-curators,artists,concerts,music-movies,music-videos,playlists,songs,stations,uploaded-videos,venues" +
                    "&format[resources]=map" +
                    "&include=canvas,default-playable-content" +
                    "&include[canvas-shelves-collection]=contents" +
                    "&include[canvas-shelves-group]=children" +
                    "&include[concerts]=artists,venues" +
                    "&include[songs]=albums" +
                    "&l=en-GB" +
                    "&limit[canvas-shelves-collection:contents]=12" +
                    "&limit[canvas-shelves-collection:contents{displayStyle}]=artist-featured-track-lockup:24" +
                    "&omit[resource]=autos&platform=web" +
                    "&relate[canvas-shelves-collection]=see-all"
                httpGet(ampUrl, 10000)
            }
            val legacyArtistDeferred = async(Dispatchers.IO) {
                val url = "$API_BASE/artist?artistId=${encode(resolvedArtistId)}&id=${encode(resolvedArtistId)}&storefront=us&l=en-US" +
                    "&include=albums,music-videos,playlists,station" +
                    "&views=top-songs,full-albums,singles-eps,featured-playlists,latest-release,appears-on-albums,similar-artists" +
                    "&extend=editorialArtwork,editorialVideo,editorialNotes,artistBio,bornOrFormed,origin,hero,keyColor"
                httpGet(url, 9000)
            }
            val artistAlbumsDeferred = async(Dispatchers.IO) {
                httpGet("$API_BASE/artist/albums?artistId=${encode(resolvedArtistId)}&artist=${encode(resolvedArtistId)}&limit=80&l=en-US", 8000)
            }

            val ampText = ampCompoundDeferred.await()
            val legacyText = legacyArtistDeferred.await()
            val albumsText = artistAlbumsDeferred.await()

            // Parse discography supplement first so either parser can use it
            val allDiscography = mutableListOf<LyricsFlowCatalogItem>()
            if (!albumsText.isNullOrBlank()) {
                runCatching {
                    val dRoot = JSONObject(albumsText)
                    val dArr = dRoot.optJSONArray("data") ?: dRoot.optJSONObject("raw_data")?.optJSONArray("data")
                    if (dArr != null) {
                        for (i in 0 until dArr.length()) {
                            val item = dArr.optJSONObject(i) ?: continue
                            val id = item.optString("id")
                            val aAttrs = item.optJSONObject("attributes") ?: item
                            val aName = aAttrs.optString("name")
                            if (id.isBlank() || aName.isBlank()) continue
                            val relDate = aAttrs.optString("releaseDate")
                            allDiscography.add(
                                LyricsFlowCatalogItem(
                                    id = id,
                                    type = "albums",
                                    title = aName,
                                    subtitle = aAttrs.optString("artistName").ifBlank { fallbackName },
                                    albumName = aName,
                                    albumId = id,
                                    artistId = resolvedArtistId,
                                    artworkUrl = resolveArtworkUrl(aAttrs.optJSONObject("artwork"), 600, 600),
                                    isExplicit = aAttrs.optString("contentRating").equals("explicit", ignoreCase = true),
                                    releaseYear = relDate.take(4),
                                    editorialSubtitle = formatReleaseDateHuman(relDate),
                                    trackCount = aAttrs.optInt("trackCount", 0)
                                )
                            )
                        }
                    }
                }
            }

            // Try parsing the rich AMP `format[resources]=map` response (`parseAmpArtistResponse` in `amp-parser.js`)
            if (!ampText.isNullOrBlank()) {
                runCatching {
                    val parsedAmp = parseAmpCompoundArtistJson(
                        root = JSONObject(ampText),
                        artistId = resolvedArtistId,
                        fallbackName = fallbackName,
                        discographySupplement = allDiscography
                    )
                    if (parsedAmp != null) {
                        if (parsedAmp.artworkUrl.isNotBlank()) {
                            artistAvatarCache[resolvedArtistId] = parsedAmp.artworkUrl
                            artistAvatarCache[parsedAmp.name.lowercase(Locale.ROOT)] = parsedAmp.artworkUrl
                        }
                        return@withContext parsedAmp
                    }
                }
            }

            // Try parsing `/artist` response
            if (!legacyText.isNullOrBlank()) {
                runCatching {
                    val root = JSONObject(legacyText)
                    val artistObj = root.optJSONObject("raw_data")?.optJSONArray("data")?.optJSONObject(0)
                        ?: root.optJSONArray("data")?.optJSONObject(0)
                        ?: root.optJSONObject("artist")
                        ?: root
                    val attrs = artistObj.optJSONObject("attributes") ?: artistObj
                    val name = attrs.optString("name").ifBlank { root.optString("artist_name") }.ifBlank { fallbackName }
                    if (name.isNotBlank()) {
                        val artworkUrl = extractBestArtistOrEntityArtwork(attrs, 1400)
                        val heroVideoUrl = extractEditorialVideoUrl(attrs.optJSONObject("editorialVideo"))
                        val logoUrl = resolveLogoUrl(
                            attrs.optJSONObject("editorialArtwork")?.optJSONObject("musicContentColorLogoTrimmed")?.optString("url")
                        )
                        val keyColorHex = attrs.optString("keyColor").removePrefix("#").ifBlank {
                            attrs.optJSONObject("artwork")?.optString("bgColor")?.removePrefix("#").orEmpty()
                        }
                        val bio = attrs.optString("artistBio")
                            .ifBlank { attrs.optString("plainEditorialNotes") }
                            .ifBlank { attrs.optJSONObject("editorialNotes")?.optString("standard").orEmpty() }
                            .ifBlank { attrs.optJSONObject("editorialNotes")?.optString("short").orEmpty() }
                            .replace(Regex("<[^>]*>"), "")
                            .trim()
                        val genre = attrs.optJSONArray("genreNames")?.optString(0).orEmpty()
                        val origin = attrs.optString("origin")
                        val formedYear = attrs.optString("bornOrFormed")

                        val views = artistObj.optJSONObject("views")
                        val rels = artistObj.optJSONObject("relationships")

                        val topSongs = parseArtistTrackView(views?.optJSONObject("top-songs"), resolvedArtistId, name)
                        val latestRelease = parseAlbumShelfView(views?.optJSONObject("latest-release")).firstOrNull()
                        val essentialAlbums = parseAlbumShelfView(
                            views?.optJSONObject("featured-albums") ?: views?.optJSONObject("essential-albums")
                        )
                        val fullAlbumsFromView = parseAlbumShelfView(views?.optJSONObject("full-albums"))
                        val singlesFromView = parseAlbumShelfView(
                            views?.optJSONObject("singles") ?: views?.optJSONObject("singles-eps")
                        )
                        val appearsOn = parseAlbumShelfView(views?.optJSONObject("appears-on-albums"))
                        val playlists = parseAlbumShelfView(views?.optJSONObject("featured-playlists")).ifEmpty {
                            parseAlbumShelfView(rels?.optJSONObject("playlists"))
                        }
                        val musicVideos = parseArtistTrackView(
                            views?.optJSONObject("top-music-videos") ?: rels?.optJSONObject("music-videos"),
                            resolvedArtistId,
                            name,
                            defaultType = "music-videos"
                        )
                        val similarArtists = parseAlbumShelfView(views?.optJSONObject("similar-artists")).map {
                            it.copy(type = "artists", artistId = it.id)
                        }

                        val finalAlbums = fullAlbumsFromView.ifEmpty {
                            allDiscography.filter {
                                !it.title.contains(" - Single", ignoreCase = true) &&
                                    !it.title.contains(" - EP", ignoreCase = true) &&
                                    (it.trackCount == 0 || it.trackCount > 3)
                            }
                        }.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                        val finalSingles = singlesFromView.ifEmpty {
                            allDiscography.filter {
                                it.title.contains(" - Single", ignoreCase = true) ||
                                    it.title.contains(" - EP", ignoreCase = true) ||
                                    (it.trackCount in 1..3)
                            }
                        }.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
                        val resolvedLatest = (listOfNotNull(latestRelease) + finalAlbums + finalSingles)
                            .maxByOrNull { it.releaseYear.toIntOrNull() ?: 0 }

                        if (artworkUrl.isNotBlank()) {
                            artistAvatarCache[resolvedArtistId] = artworkUrl
                            artistAvatarCache[name.lowercase(Locale.ROOT)] = artworkUrl
                        }

                        return@withContext CatalogArtistDetail(
                            id = resolvedArtistId,
                            name = name,
                            artworkUrl = artworkUrl,
                            heroVideoUrl = heroVideoUrl,
                            logoUrl = logoUrl,
                            keyColorHex = keyColorHex,
                            bio = bio,
                            genre = genre,
                            origin = origin,
                            formedYear = formedYear,
                            latestRelease = resolvedLatest,
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
        }

        // Fallback via catalog search if artist endpoint didn't resolve
        val searchFallback = searchFullCatalog(searchName, limit = 25)
        val matchedArtist = searchFallback.artists.firstOrNull()
        val realAvatar = matchedArtist?.artworkUrl?.takeIf { it.isNotBlank() }
            ?: fetchArtistAvatarUrl(searchName, resolvedArtistId)
        CatalogArtistDetail(
            id = resolvedArtistId.ifBlank { rawId },
            name = matchedArtist?.title ?: fallbackName.ifBlank { "Artist" },
            artworkUrl = realAvatar,
            topSongs = searchFallback.songs,
            albums = searchFallback.albums,
            playlists = searchFallback.playlists,
            musicVideos = searchFallback.musicVideos
        )
    }

    /**
     * Port of `parseAmpArtistResponse` from `main/js/amp-parser.js` (lines 7-168) + `upload.js` (lines 3705-3785).
     */
    private fun parseAmpCompoundArtistJson(
        root: JSONObject,
        artistId: String,
        fallbackName: String,
        discographySupplement: List<LyricsFlowCatalogItem>
    ): CatalogArtistDetail? {
        val res = root.optJSONObject("resources") ?: return null
        val artistsMap = res.optJSONObject("artists") ?: return null
        val mainArtist = artistsMap.optJSONObject(artistId)
            ?: artistsMap.keys().asSequence().firstOrNull()?.let { artistsMap.optJSONObject(it) }
            ?: return null

        val attr = mainArtist.optJSONObject("attributes") ?: return null
        val name = attr.optString("name").ifBlank { fallbackName }
        if (name.isBlank()) return null

        val artworkUrl = extractBestArtistOrEntityArtwork(attr, 1400)
        val heroVideoUrl = extractEditorialVideoUrl(attr.optJSONObject("editorialVideo"))
        val logoUrl = resolveLogoUrl(
            attr.optJSONObject("editorialArtwork")?.optJSONObject("musicContentColorLogoTrimmed")?.optString("url")
        )
        val keyColorHex = attr.optString("keyColor").removePrefix("#").ifBlank {
            attr.optJSONObject("artwork")?.optString("bgColor")?.removePrefix("#").orEmpty()
        }
        val bio = attr.optString("artistBio")
            .ifBlank { attr.optString("plainEditorialNotes") }
            .ifBlank { attr.optJSONObject("editorialNotes")?.optString("standard").orEmpty() }
            .ifBlank { attr.optJSONObject("editorialNotes")?.optString("short").orEmpty() }
            .replace(Regex("<[^>]*>"), "")
            .trim()
        val genre = attr.optJSONArray("genreNames")?.optString(0).orEmpty()
        val origin = attr.optString("origin")
        val formedYear = attr.optString("bornOrFormed")

        val songsMap = res.optJSONObject("songs")
        val albumsMap = res.optJSONObject("albums")
        val playlistsMap = res.optJSONObject("playlists")
        val videosMap = res.optJSONObject("music-videos")
        val concertsMap = res.optJSONObject("concerts")
        val venuesMap = res.optJSONObject("venues")
        val shelvesMap = res.optJSONObject("canvas-shelves-collection")

        var latestRelease: LyricsFlowCatalogItem? = null
        val topSongs = mutableListOf<CatalogTrackResult>()
        val essentialAlbums = mutableListOf<LyricsFlowCatalogItem>()
        val albums = mutableListOf<LyricsFlowCatalogItem>()
        val singles = mutableListOf<LyricsFlowCatalogItem>()
        val appearsOn = mutableListOf<LyricsFlowCatalogItem>()
        val playlists = mutableListOf<LyricsFlowCatalogItem>()
        val musicVideos = mutableListOf<CatalogTrackResult>()
        val similarArtists = mutableListOf<LyricsFlowCatalogItem>()

        fun parseAlbumFromMap(id: String): LyricsFlowCatalogItem? {
            val aObj = albumsMap?.optJSONObject(id) ?: return null
            val aAttr = aObj.optJSONObject("attributes") ?: return null
            val aName = aAttr.optString("name")
            if (aName.isBlank()) return null
            val relDate = aAttr.optString("releaseDate")
            return LyricsFlowCatalogItem(
                id = id,
                type = "albums",
                title = aName,
                subtitle = aAttr.optString("artistName").ifBlank { name },
                albumName = aName,
                albumId = id,
                artistId = artistId,
                artworkUrl = extractBestArtistOrEntityArtwork(aAttr, 600),
                isExplicit = aAttr.optString("contentRating").equals("explicit", ignoreCase = true),
                releaseYear = relDate.take(4),
                editorialSubtitle = formatReleaseDateHuman(relDate),
                trackCount = aAttr.optInt("trackCount", 0)
            )
        }

        fun parseSongFromMap(id: String, idx: Int, defaultType: String = "songs"): CatalogTrackResult? {
            val mapToUse = if (defaultType == "music-videos") videosMap else songsMap
            val sObj = mapToUse?.optJSONObject(id) ?: songsMap?.optJSONObject(id) ?: return null
            val sAttr = sObj.optJSONObject("attributes") ?: return null
            val sName = sAttr.optString("name")
            if (sName.isBlank()) return null
            val sAlbumId = sObj.optJSONObject("relationships")?.optJSONObject("albums")
                ?.optJSONArray("data")?.optJSONObject(0)?.optString("id").orEmpty()
            return CatalogTrackResult(
                id = id,
                title = sName,
                artist = sAttr.optString("artistName").ifBlank { name },
                artistId = artistId,
                album = sAttr.optString("albumName"),
                albumId = sAlbumId,
                artworkUrl = resolveArtworkUrl(sAttr.optJSONObject("artwork"), 600, 600),
                durationMs = sAttr.optLong("durationInMillis", 0L),
                trackNumber = sAttr.optInt("trackNumber", idx + 1),
                discNumber = sAttr.optInt("discNumber", 1),
                isExplicit = sAttr.optString("contentRating").equals("explicit", ignoreCase = true),
                previewUrl = sAttr.optJSONArray("previews")?.optJSONObject(0)?.optString("url").orEmpty(),
                releaseYear = sAttr.optString("releaseDate").take(4).toIntOrNull(),
                genre = sAttr.optJSONArray("genreNames")?.optString(0).orEmpty(),
                type = defaultType
            )
        }

        if (shelvesMap != null) {
            val shelfKeys = shelvesMap.keys()
            while (shelfKeys.hasNext()) {
                val shelfKey = shelfKeys.next()
                val shelf = shelvesMap.optJSONObject(shelfKey) ?: continue
                val contentsData = shelf.optJSONObject("relationships")?.optJSONObject("contents")?.optJSONArray("data")
                    ?: continue

                when {
                    shelfKey.contains("_FeaturedRelease_") || shelfKey.contains("_LatestRelease_") -> {
                        val firstRef = contentsData.optJSONObject(0)
                        if (firstRef != null) {
                            latestRelease = parseAlbumFromMap(firstRef.optString("id"))
                        }
                    }
                    shelfKey.contains("_TopSongs_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseSongFromMap(ref.optString("id"), i, "songs")?.let { topSongs.add(it) }
                        }
                    }
                    shelfKey.contains("_EssentialAlbums_") || shelfKey.contains("_FeaturedAlbums_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseAlbumFromMap(ref.optString("id"))?.let { essentialAlbums.add(it) }
                        }
                    }
                    shelfKey.contains("_Albums_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseAlbumFromMap(ref.optString("id"))?.let { albums.add(it) }
                        }
                    }
                    shelfKey.contains("_ArtistSingles_") || shelfKey.contains("_Singles_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseAlbumFromMap(ref.optString("id"))?.let { singles.add(it) }
                        }
                    }
                    shelfKey.contains("_AppearsOn_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseAlbumFromMap(ref.optString("id"))?.let { appearsOn.add(it) }
                        }
                    }
                    shelfKey.contains("_ArtistPlaylists_") || shelfKey.contains("_Playlists_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            val plId = ref.optString("id")
                            val plObj = playlistsMap?.optJSONObject(plId) ?: continue
                            val plAttr = plObj.optJSONObject("attributes") ?: continue
                            val plName = plAttr.optString("name")
                            if (plName.isBlank()) continue
                            playlists.add(
                                LyricsFlowCatalogItem(
                                    id = plId,
                                    type = "playlists",
                                    title = plName,
                                    subtitle = plAttr.optString("curatorName", "Apple Music"),
                                    artworkUrl = extractBestArtistOrEntityArtwork(plAttr, 600)
                                )
                            )
                        }
                    }
                    shelfKey.contains("_MusicVideos_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            parseSongFromMap(ref.optString("id"), i, "music-videos")?.let { musicVideos.add(it) }
                        }
                    }
                    shelfKey.contains("_SimilarArtists_") -> {
                        for (i in 0 until contentsData.length()) {
                            val ref = contentsData.optJSONObject(i) ?: continue
                            val simId = ref.optString("id")
                            val simObj = artistsMap.optJSONObject(simId) ?: continue
                            val simAttr = simObj.optJSONObject("attributes") ?: continue
                            val simName = simAttr.optString("name")
                            if (simName.isBlank()) continue
                            similarArtists.add(
                                LyricsFlowCatalogItem(
                                    id = simId,
                                    type = "artists",
                                    title = simName,
                                    subtitle = "Artist",
                                    artistId = simId,
                                    artworkUrl = extractBestArtistOrEntityArtwork(simAttr, 600)
                                )
                            )
                        }
                    }
                }
            }
        }

        // If shelves didn't populate songs/albums, fallback to the resource maps directly
        if (topSongs.isEmpty() && songsMap != null) {
            val keys = songsMap.keys()
            var idx = 0
            while (keys.hasNext()) {
                val k = keys.next()
                parseSongFromMap(k, idx++, "songs")?.let { topSongs.add(it) }
            }
        }

        // Extract upcoming concerts & badge (e.g., "Concerts Upcoming" + full concert list)
        var upcomingConcertBadge = ""
        val upcomingConcerts = mutableListOf<CatalogConcertItem>()
        if (concertsMap != null) {
            val cKeys = concertsMap.keys()
            while (cKeys.hasNext()) {
                val cKey = cKeys.next()
                val cObj = concertsMap.optJSONObject(cKey) ?: continue
                val cAttr = cObj.optJSONObject("attributes") ?: continue
                val startDate = cAttr.optString("startDate").ifBlank { cAttr.optString("eventDate") }
                val datePart = formatShortDayMonth(startDate)
                val parts = datePart.split(" ")
                val dayNum = parts.firstOrNull().orEmpty().ifBlank { "20" }
                val monthShort = parts.getOrNull(1)?.uppercase(Locale.US).orEmpty().ifBlank { "AUG" }
                val fullDate = formatReleaseDateHuman(startDate).ifBlank { datePart }
                val venueId = cObj.optJSONObject("relationships")?.optJSONObject("venues")
                    ?.optJSONArray("data")?.optJSONObject(0)?.optString("id").orEmpty()
                val venueAttr = venuesMap?.optJSONObject(venueId)?.optJSONObject("attributes")
                val venueName = venueAttr?.optString("name")
                    ?.ifBlank { cAttr.optString("venueName") }
                    ?: cAttr.optString("venueName").ifBlank { cAttr.optString("name") }
                val city = venueAttr?.optString("city")?.ifBlank { cAttr.optString("city") }
                    ?: cAttr.optString("city")
                val country = venueAttr?.optString("country").orEmpty()
                val cityCountry = listOf(city, country).filter { it.isNotBlank() }.joinToString(", ")
                val ticketUrl = cAttr.optJSONArray("tickets")?.optJSONObject(0)?.optString("url")
                    ?: cAttr.optString("ticketUrl").ifBlank { cAttr.optString("url") }
                val concertTitle = cAttr.optString("name").ifBlank { "$name Live" }

                if (upcomingConcertBadge.isBlank()) {
                    upcomingConcertBadge = "Concerts Upcoming"
                }
                upcomingConcerts.add(
                    CatalogConcertItem(
                        id = cKey,
                        title = concertTitle,
                        dateText = listOf(datePart, city.ifBlank { venueName }).filter { it.isNotBlank() }.joinToString(" · "),
                        monthShort = monthShort,
                        dayNumber = dayNum,
                        fullDateLine = fullDate,
                        doorsLine = "Doors: 7:30 PM",
                        venueName = venueName.ifBlank { "$name World Tour" },
                        cityCountry = cityCountry.ifBlank { city },
                        ticketUrl = ticketUrl
                    )
                )
            }
        }

        val finalAlbums = albums.ifEmpty {
            discographySupplement.filter {
                !it.title.contains(" - Single", ignoreCase = true) &&
                    !it.title.contains(" - EP", ignoreCase = true) &&
                    (it.trackCount == 0 || it.trackCount > 3)
            }
        }.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
        val finalSingles = singles.ifEmpty {
            discographySupplement.filter {
                it.title.contains(" - Single", ignoreCase = true) ||
                    it.title.contains(" - EP", ignoreCase = true) ||
                    (it.trackCount in 1..3)
            }
        }.sortedByDescending { it.releaseYear.toIntOrNull() ?: 0 }
        val resolvedLatest = (listOfNotNull(latestRelease) + finalAlbums + finalSingles)
            .maxByOrNull { it.releaseYear.toIntOrNull() ?: 0 }

        return CatalogArtistDetail(
            id = artistId,
            name = name,
            artworkUrl = artworkUrl,
            heroVideoUrl = heroVideoUrl,
            logoUrl = logoUrl,
            keyColorHex = keyColorHex,
            upcomingConcertBadge = upcomingConcertBadge,
            upcomingConcerts = upcomingConcerts,
            bio = bio,
            genre = genre,
            origin = origin,
            formedYear = formedYear,
            latestRelease = resolvedLatest,
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
            "motionArtistFullscreen16x9",
            "motionArtistWide16x9",
            "motionDetailTall",
            "motionTallVideo3x4",
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
     * Extracts the best portrait or cover artwork from an AMP `attributes` object (`upload.js` lines 3711-3719 & `amp-parser.js` lines 36-41).
     * Checks `hero[0].content[0].artwork`, `artwork`, `editorialArtwork.storeFlowcase`, `bannerUber`, `centeredFullscreenBackground`, `subscriptionHero`, and `editorialVideo.*.previewFrame`.
     */
    fun extractBestArtistOrEntityArtwork(attrs: JSONObject?, size: Int = 800): String {
        if (attrs == null) return ""
        val heroArt = attrs.optJSONArray("hero")
            ?.optJSONObject(0)
            ?.optJSONArray("content")
            ?.optJSONObject(0)
            ?.optJSONObject("artwork")
        if (heroArt != null) {
            val u = resolveArtworkUrl(heroArt, size, size)
            if (u.isNotBlank()) return u
        }

        attrs.optJSONObject("artwork")?.let { artObj ->
            val u = resolveArtworkUrl(artObj, size, size)
            if (u.isNotBlank()) return u
        }

        val edArt = attrs.optJSONObject("editorialArtwork")
        if (edArt != null) {
            val edKeys = listOf(
                "storeFlowcase",
                "subscriptionHero",
                "bannerUber",
                "centeredFullscreenBackground",
                "superHeroWide",
                "FullscreenBackground"
            )
            for (k in edKeys) {
                val u = resolveArtworkUrl(edArt.optJSONObject(k), size, size)
                if (u.isNotBlank()) return u
            }
        }

        val edVid = attrs.optJSONObject("editorialVideo")
        if (edVid != null) {
            val vidKeys = listOf(
                "motionArtistFullscreen16x9",
                "motionArtistWide16x9",
                "motionArtistSquare1x1",
                "motionDetailTall",
                "motionDetailSquare"
            )
            for (k in vidKeys) {
                val previewObj = edVid.optJSONObject(k)?.optJSONObject("previewFrame")
                val u = resolveArtworkUrl(previewObj, size, size)
                if (u.isNotBlank()) return u
            }
        }

        return ""
    }

    /**
     * Port of `cleanArtworkUrl` from `main/js/upload.js` (lines 76-108) and `resolveArtworkUrl` from `main/js/amp-parser.js`.
     */
    fun resolveArtworkUrl(artworkObj: JSONObject?, width: Int = 600, height: Int = 600): String {
        if (artworkObj == null) return ""
        val template = artworkObj.optString("url").takeIf { it.isNotBlank() }
            ?: artworkObj.optJSONObject("subscriptionHero")?.optString("url")
            ?: artworkObj.optJSONObject("storeFlowcase")?.optString("url")
            ?: artworkObj.optJSONObject("bannerUber")?.optString("url")
            ?: return ""
        return cleanArtworkUrlString(template, maxOf(width, height))
    }

    fun cleanArtworkUrlString(rawUrl: String?, size: Int = 600): String {
        val url = rawUrl?.trim().orEmpty()
        if (url.isEmpty()) return ""
        val isPng = url.contains("Logo", ignoreCase = true) || url.endsWith(".png", ignoreCase = true)
        val ext = if (isPng) "png" else "jpg"

        if (url.contains("/{w}x{h}bb.")) {
            return url
                .replace(Regex("/\\{w\\}x\\{h\\}bb\\.([a-zA-Z0-9]+|\\{f\\})"), "/${size}x${size}bb.$ext")
                .replace("{w}", size.toString())
                .replace("{h}", size.toString())
                .replace("{c}", "")
                .replace("{f}", ext)
        }

        if (url.contains("{w}") || url.contains("{h}") || url.contains("{c}") || url.contains("{f}")) {
            return url
                .replace("{w}", size.toString())
                .replace("{h}", size.toString())
                .replace("{c}", "")
                .replace("{f}", ext)
        }

        return url.replace(Regex("/\\d+x\\d+(bb)?\\.(jpg|png|webp)", RegexOption.IGNORE_CASE)) { match ->
            val matchedExt = match.groupValues.getOrNull(2)?.ifBlank { "jpg" } ?: "jpg"
            "/${size}x${size}bb.$matchedExt"
        }
    }

    /**
     * Port of `cleanLogo` in `main/js/amp-parser.js` (line 23) for `musicContentColorLogoTrimmed`.
     */
    fun resolveLogoUrl(rawUrl: String?, width: Int = 800, height: Int = 300): String {
        val url = rawUrl?.trim().orEmpty()
        if (url.isEmpty()) return ""
        return url
            .replace("{w}", width.toString())
            .replace("{h}", height.toString())
            .replace("{c}", "bb")
            .replace("{f}", "png")
    }

    private fun formatReleaseDateHuman(isoDate: String): String {
        val clean = isoDate.trim().take(10)
        if (clean.length < 10) return isoDate.take(4)
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(clean) ?: return@runCatching isoDate.take(4)
            SimpleDateFormat("d MMM yyyy", Locale.US).format(parsed)
        }.getOrDefault(isoDate.take(4))
    }

    private fun formatShortDayMonth(isoDate: String): String {
        val clean = isoDate.trim().take(10)
        if (clean.length < 10) return ""
        return runCatching {
            val parser = SimpleDateFormat("yyyy-MM-dd", Locale.US)
            val parsed = parser.parse(clean) ?: return@runCatching ""
            SimpleDateFormat("d MMM", Locale.US).format(parsed)
        }.getOrDefault("")
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
            val sanitizedUrl = urlString
                .replace("[", "%5B")
                .replace("]", "%5D")
                .replace("{", "%7B")
                .replace("}", "%7D")
                .replace(" ", "%20")
            conn = (URL(sanitizedUrl).openConnection() as HttpURLConnection).apply {
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
