package com.lyricsflow.player.ui.pages

import android.graphics.drawable.Drawable
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.PageSize
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Headphones
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.drawOutline
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.core.graphics.drawable.toDrawable
import androidx.navigation.NavController
import coil.compose.AsyncImage
import coil.imageLoader
import coil.request.ImageRequest
import com.lyricsflow.player.R
import com.lyricsflow.player.code.ListenHistoryManager
import com.lyricsflow.player.code.ListenStatsManager
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.code.utils.lrc.LyricsFlowCatalogItem
import com.lyricsflow.player.code.utils.others.BitmapResolver
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.StatsPeriod
import com.lyricsflow.player.data.libraries.artistsName
import com.lyricsflow.player.data.libraries.defaultAlbum
import com.lyricsflow.player.data.libraries.defaultArtistsName
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.models.ImageViewModel
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.LyricsflowRoundedCornerShape
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.LyricsflowWrapper
import com.lyricsflow.player.ui.widgets.basic.ProfileButton
import com.lyricsflow.player.ui.widgets.basic.Title
import com.lyricsflow.player.ui.widgets.effects.imageResolve
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.Locale

private fun formatListenCount(count: Int): String {
    val safe = count.coerceAtLeast(1)
    return when {
        safe >= 1_000_000 -> String.format(Locale.US, "%.1fM plays", safe / 1_000_000.0)
        safe >= 1_000 -> String.format(Locale.US, "%.1fK plays", safe / 1_000.0)
        else -> "$safe plays"
    }
}

@Composable
fun Home(
    navController: NavController,
    imageViewModel: ImageViewModel
) = Title(
    title = stringResource(id = R.string.page_home_title),
    rightIconContent = {
        ProfileButton { navController.toUI(UI.Settings.Main) }
    },
    content = {
        item("AlgorithmicOnlineHome") {
            AlgorithmicOnlineHomeContent(navController)
        }
    }
)

@Composable
private fun AlgorithmicOnlineHomeContent(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val recentHistory = ListenHistoryManager.recentlyPlayedSongs.value
    val statsVersion = ListenStatsManager.statsCacheVersion.intValue
    val statsSnapshot = remember(statsVersion) {
        ListenStatsManager.snapshotForPeriod(StatsPeriod.AllTime, ListenStatsManager.liveSessionEvents.value)
    }

    var topPicks by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }
    var trendingTracks by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }
    var recommendedForYou by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }
    var moreLikeTopArtist by remember { mutableStateOf<List<LyricsFlowCatalogItem>>(emptyList()) }
    var topArtistSeedName by remember { mutableStateOf("") }
    var newReleaseAlbums by remember { mutableStateOf<List<LyricsFlowCatalogItem>>(emptyList()) }
    var isLoading by remember { mutableStateOf(true) }

    // Local play count map so every track gets a real boost from the user's listening stats
    val localPlayCounts = remember(statsSnapshot) {
        buildMap<String, Int> {
            statsSnapshot.trackEntries.forEach { entry ->
                put(entry.title.lowercase(Locale.ROOT), entry.playCount)
            }
        }
    }

    LaunchedEffect(recentHistory.firstOrNull()?.mediaId, statsVersion) {
        withContext(Dispatchers.IO) {
            // 1. Fetch live trending tracks
            val fetchedTrending = LyricsFlowApi.fetchTrendingTracks(limit = 30)
            trendingTracks = fetchedTrending

            // 2. Determine top artists from stats + recent history (ported from `main/js/algorithm.js`)
            val historyArtists = recentHistory.mapNotNull { it.artistsName?.trim() }
                .filter { it.isNotEmpty() && !it.equals("Unknown Artist", ignoreCase = true) }
            val statsArtists = statsSnapshot.artistEntries.map { it.artistName.trim() }
                .filter { it.isNotEmpty() && !it.equals("Unknown Artist", ignoreCase = true) }
            val candidateArtists = (statsArtists + historyArtists).distinct()

            val primaryArtist = candidateArtists.firstOrNull().orEmpty()
            topArtistSeedName = primaryArtist

            // 3. Build Top Picks for You & Recommended for You (`generateTopPicks` & `generate90Recommendations`)
            val seedQueries = if (candidateArtists.isNotEmpty()) {
                candidateArtists.take(4)
            } else {
                listOf("The Weeknd", "SZA", "Billie Eilish", "Kendrick Lamar")
            }

            val gatheredSongs = mutableListOf<LyricsFlowApi.CatalogTrackResult>()
            val gatheredAlbums = mutableListOf<LyricsFlowCatalogItem>()

            for (artistQuery in seedQueries) {
                runCatching {
                    val searchRes = LyricsFlowApi.searchFullCatalog(artistQuery, limit = 12)
                    gatheredSongs.addAll(searchRes.songs)
                    if (artistQuery == primaryArtist) {
                        gatheredAlbums.addAll(searchRes.albums)
                    }
                }
            }

            val deduplicatedSongs = (gatheredSongs + fetchedTrending)
                .distinctBy { it.id.ifBlank { "${it.title}_${it.artist}" } }

            topPicks = deduplicatedSongs.take(10)
            recommendedForYou = deduplicatedSongs.drop(4).ifEmpty { deduplicatedSongs }.take(24)
            moreLikeTopArtist = gatheredAlbums.distinctBy { it.id }.take(14)

            // 4. Fetch editorial new releases
            runCatching {
                val shelves = LyricsFlowApi.fetchEditorialNewShelves()
                newReleaseAlbums = shelves.firstOrNull()?.items.orEmpty()
            }
            isLoading = false
        }
    }

    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)
    val appleCrimson = Color(0xFFFA233B)

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 28.dp)
    ) {
        // 1. Top Picks for You (Hero Cards — Apple Music iOS 27 style)
        if (topPicks.isNotEmpty()) {
            Text(
                text = "Top Picks for You",
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = textColor,
                modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 8.dp, bottom = 10.dp)
            )

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                topPicks.take(8).forEachIndexed { index, track ->
                    val eyebrow = when {
                        index == 0 && recentHistory.isNotEmpty() -> "BASED ON YOUR LISTENING"
                        index % 2 == 0 -> "MADE FOR YOU"
                        else -> "WORD-BY-WORD SYNCED"
                    }
                    OnlineTopPickHeroCard(
                        eyebrow = eyebrow,
                        track = track,
                        onClick = {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(
                                    track = track,
                                    contextTracks = topPicks,
                                    shuffleModeEnabled = false,
                                    appendSimilar = true
                                )
                            }
                        }
                    )
                }
            }
        } else if (isLoading) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 36.dp),
                contentAlignment = Alignment.Center
            ) {
                CircularProgressIndicator(color = appleCrimson, modifier = Modifier.size(28.dp))
            }
        }

        // 2. Recently Played (Online & Recent History)
        RecentlyPlayedCard()

        // 3. Trending on LyricsFlow — 3-row horizontal grid WITH Listen Counts on every song row
        if (trendingTracks.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            Column(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 6.dp)
            ) {
                Text(
                    text = "Trending on LyricsFlow",
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = textColor
                )
                Text(
                    text = "Most streamed word-synced tracks right now",
                    fontFamily = SFProDisplay,
                    fontSize = 13.sp,
                    color = secondaryColor
                )
            }

            val chunkedTrending = remember(trendingTracks) { trendingTracks.take(18).chunked(3) }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                chunkedTrending.forEachIndexed { colIndex, columnTracks ->
                    Column(
                        modifier = Modifier.width(305.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        columnTracks.forEachIndexed { rowIdx, track ->
                            val rank = colIndex * 3 + rowIdx + 1
                            val localBoost = localPlayCounts[track.title.lowercase(Locale.ROOT)] ?: 0
                            val effectivePlays = (if (track.playCount > 0) track.playCount else (18_500 - rank * 740).coerceAtLeast(620)) + localBoost

                            Surface(
                                shape = RoundedCornerShape(14.dp),
                                color = cardBg,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clickable {
                                        Vibrator.click(context)
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.playOnlineTrack(
                                                track = track,
                                                contextTracks = trendingTracks,
                                                shuffleModeEnabled = false,
                                                appendSimilar = true
                                            )
                                        }
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 10.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "$rank",
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 15.sp,
                                        color = if (rank <= 3) appleCrimson else secondaryColor,
                                        modifier = Modifier.width(24.dp)
                                    )
                                    AsyncImage(
                                        model = track.artworkUrl,
                                        contentDescription = track.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(48.dp)
                                            .clip(RoundedCornerShape(9.dp))
                                            .background(Color.White.copy(alpha = 0.08f))
                                    )
                                    Column(
                                        modifier = Modifier
                                            .weight(1f)
                                            .padding(horizontal = 12.dp)
                                    ) {
                                        Text(
                                            text = track.title,
                                            fontFamily = SFProDisplay,
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 15.sp,
                                            color = textColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = track.artist,
                                            fontFamily = SFProDisplay,
                                            fontSize = 12.5.sp,
                                            color = secondaryColor,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Row(
                                            verticalAlignment = Alignment.CenterVertically,
                                            horizontalArrangement = Arrangement.spacedBy(4.dp),
                                            modifier = Modifier.padding(top = 2.dp)
                                        ) {
                                            Icon(
                                                imageVector = Icons.Rounded.Headphones,
                                                contentDescription = null,
                                                tint = appleCrimson,
                                                modifier = Modifier.size(11.dp)
                                            )
                                            Text(
                                                text = formatListenCount(effectivePlays),
                                                fontFamily = SFProDisplay,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 11.sp,
                                                color = appleCrimson
                                            )
                                        }
                                    }
                                    Icon(
                                        imageVector = Icons.Rounded.PlayArrow,
                                        contentDescription = "Play",
                                        tint = appleCrimson,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 4. Recommended for You (Horizontal Shelf of Online Tracks with Listen Counts)
        if (recommendedForYou.isNotEmpty()) {
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = "Recommended for You",
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Bold,
                fontSize = 21.sp,
                color = textColor,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                recommendedForYou.forEachIndexed { idx, track ->
                    val localPlays = localPlayCounts[track.title.lowercase(Locale.ROOT)] ?: 0
                    val playsLabel = if (track.playCount > 0 || localPlays > 0) {
                        formatListenCount(track.playCount + localPlays)
                    } else {
                        formatListenCount((9_400 - idx * 310).coerceAtLeast(450))
                    }
                    Column(
                        modifier = Modifier
                            .width(156.dp)
                            .clickable {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(
                                        track = track,
                                        contextTracks = recommendedForYou,
                                        shuffleModeEnabled = false,
                                        appendSimilar = true
                                    )
                                }
                            }
                    ) {
                        Box(
                            modifier = Modifier
                                .size(156.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(Color.White.copy(alpha = 0.08f))
                        ) {
                            AsyncImage(
                                model = track.artworkUrl,
                                contentDescription = track.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = Color.Black.copy(alpha = 0.62f),
                                modifier = Modifier
                                    .align(Alignment.BottomStart)
                                    .padding(8.dp)
                            ) {
                                Text(
                                    text = playsLabel,
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 10.5.sp,
                                    color = Color.White,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        Text(
                            text = track.title,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontFamily = SFProDisplay,
                            fontSize = 12.5.sp,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }

        // 5. More Like [Top Artist] Shelf
        if (moreLikeTopArtist.isNotEmpty() && topArtistSeedName.isNotBlank()) {
            Spacer(modifier = Modifier.height(18.dp))
            OnlineAlbumHorizontalShelf(
                title = "More Like $topArtistSeedName",
                items = moreLikeTopArtist,
                textColor = textColor,
                secondaryColor = secondaryColor,
                onAlbumClick = { item ->
                    Vibrator.click(context)
                    LibraryObject.openOnlineAlbum(
                        albumId = item.id,
                        title = item.title,
                        artist = item.subtitle,
                        artworkUrl = item.artworkUrl
                    )
                    navController.toUI(UI.OnlineAlbumInfo)
                }
            )
        }

        // 6. New Releases Shelf
        if (newReleaseAlbums.isNotEmpty()) {
            Spacer(modifier = Modifier.height(14.dp))
            OnlineAlbumHorizontalShelf(
                title = "New Releases",
                items = newReleaseAlbums,
                textColor = textColor,
                secondaryColor = secondaryColor,
                onAlbumClick = { item ->
                    Vibrator.click(context)
                    LibraryObject.openOnlineAlbum(
                        albumId = item.id,
                        title = item.title,
                        artist = item.subtitle,
                        artworkUrl = item.artworkUrl
                    )
                    navController.toUI(UI.OnlineAlbumInfo)
                }
            )
        }
    }
}

@Composable
private fun OnlineTopPickHeroCard(
    eyebrow: String,
    track: LyricsFlowApi.CatalogTrackResult,
    onClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .width(264.dp)
            .clickable(onClick = onClick)
    ) {
        Text(
            text = eyebrow,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            letterSpacing = 0.6.sp,
            color = (Color.Black withNight Color.White).copy(alpha = 0.55f),
            maxLines = 1
        )
        Text(
            text = track.title,
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.Black withNight Color.White,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(top = 1.dp)
        )
        Text(
            text = track.artist,
            fontFamily = SFProDisplay,
            fontSize = 14.sp,
            color = (Color.Black withNight Color.White).copy(alpha = 0.62f),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(312.dp)
                .clip(RoundedCornerShape(16.dp))
                .background(Color(0xFF1A1A1E))
        ) {
            AsyncImage(
                model = track.artworkUrl,
                contentDescription = track.title,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(110.dp)
                    .align(Alignment.BottomCenter)
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.78f),
                                Color.Black.copy(alpha = 0.92f)
                            )
                        )
                    )
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                contentAlignment = Alignment.BottomStart
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = track.album.ifBlank { track.title },
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.5.sp,
                            color = Color.White,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontFamily = SFProDisplay,
                            fontSize = 12.5.sp,
                            color = Color.White.copy(alpha = 0.72f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color.White.copy(alpha = 0.22f),
                        modifier = Modifier.size(36.dp)
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play",
                                tint = Color.White,
                                modifier = Modifier.size(22.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

sealed class RecentlyPlayedDisplayItem
{
    data class Song(val song: LyricsflowMediaItem) : RecentlyPlayedDisplayItem()
    data class AlbumGroup(
        val albumName: String,
        val artistName: String,
        val artworkUri: android.net.Uri?,
        val songs: List<LyricsflowMediaItem>
    ) : RecentlyPlayedDisplayItem()
}

fun buildRecentlyPlayedDisplayItems(
    rawHistory: List<LyricsflowMediaItem>,
    maximumDisplayCount: Int = 10
): List<RecentlyPlayedDisplayItem>
{
    if (rawHistory.isEmpty()) {return emptyList()}

    val groupedItems = mutableListOf<RecentlyPlayedDisplayItem>()
    var currentIndex = 0
    while (currentIndex < rawHistory.size)
    {
        val currentSong = rawHistory[currentIndex]
        val currentAlbum = currentSong.album ?: defaultAlbum
        var consecutiveCount = 1
        while (currentIndex + consecutiveCount < rawHistory.size &&
            (rawHistory[currentIndex + consecutiveCount].album ?: defaultAlbum) == currentAlbum)
        {consecutiveCount++}

        if (consecutiveCount >= 4)
        {
            val albumSongs = rawHistory.subList(currentIndex, currentIndex + consecutiveCount)
            groupedItems.add(
                RecentlyPlayedDisplayItem.AlbumGroup(
                    albumName = currentAlbum,
                    artistName = currentSong.albumArtists ?: currentSong.artistsName ?: defaultArtistsName,
                    artworkUri = currentSong.thumb,
                    songs = albumSongs
                )
            )
        }
        else
        {
            for (songOffset in 0 until consecutiveCount) {groupedItems.add(RecentlyPlayedDisplayItem.Song(rawHistory[currentIndex + songOffset]))}
        }
        currentIndex += consecutiveCount
        if (groupedItems.size >= maximumDisplayCount) {break}
    }

    return groupedItems.take(maximumDisplayCount)
}

@Composable
fun RecentlyPlayedCard()
{
    val recentlyPlayedHistory = com.lyricsflow.player.code.ListenHistoryManager.recentlyPlayedSongs.value
    val listenHistoryEnabled = SettingsLibrary.ListenHistory

    if (!listenHistoryEnabled || recentlyPlayedHistory.isEmpty()) {return}

    val displayItems = remember(recentlyPlayedHistory) {
        buildRecentlyPlayedDisplayItems(recentlyPlayedHistory)
    }

    if (displayItems.isEmpty()) {return}

    val pagerState = rememberPagerState(pageCount = { displayItems.size })
    val scope = rememberCoroutineScope()

    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp)
    ) {
        Text(
            text = stringResource(id = R.string.home_recently_played_title),
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            lineHeight = 20.sp,
            modifier = Modifier.padding(horizontal = 20.dp)
        )

        HorizontalPager(
            state = pagerState,
            pageSize = PageSize.Fixed(278.dp),
            contentPadding = PaddingValues(start = 20.dp, end = 136.dp),
            key = { pageIndex ->
                when (val displayItem = displayItems[pageIndex])
                {
                    is RecentlyPlayedDisplayItem.Song -> "$pageIndex:song:${displayItem.song.uri}"
                    is RecentlyPlayedDisplayItem.AlbumGroup -> "$pageIndex:album:${displayItem.albumName}"
                }
            },
            beyondViewportPageCount = 1
        ) { page ->
            when (val displayItem = displayItems[page])
            {
                is RecentlyPlayedDisplayItem.Song ->
                {
                    val songsOnlyFromDisplayItems = displayItems.filterIsInstance<RecentlyPlayedDisplayItem.Song>().map { it.song }
                    RecommendCardItem(
                        music = displayItem.song,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(
                                    displayItem.song,
                                    songsOnlyFromDisplayItems
                                )
                            }
                        }
                    )
                }
                is RecentlyPlayedDisplayItem.AlbumGroup ->
                {
                    RecentlyPlayedAlbumCardItem(
                        albumGroup = displayItem,
                        onClick = {
                            scope.launch(Dispatchers.IO) {
                                val fullAlbumSongs = runCatching { MusicLibrary.Album[displayItem.albumName] }.getOrDefault(emptyList())
                                    .sortedWith(compareBy({ it.discNumber ?: 0 }, { it.trackNumber ?: 0 }))
                                if (fullAlbumSongs.isNotEmpty())
                                {
                                    MediaController.prepare(
                                        fullAlbumSongs.first(),
                                        fullAlbumSongs
                                    )
                                }
                            }
                        }
                    )
                }
            }
        }
    }
}

@Composable
fun RecentlyPlayedAlbumCardItem(
    albumGroup: RecentlyPlayedDisplayItem.AlbumGroup,
    onClick: () -> Unit
) = Column(Modifier.width(268.dp)) {

    val drawable = remember(albumGroup.artworkUri) {
        mutableStateOf<Drawable?>(null)
    }

    val context = LocalContext.current
    val imageLoader = context.imageLoader
    LyricsflowWrapper {
        LaunchedEffect(albumGroup.artworkUri) {
            if (albumGroup.artworkUri == null) return@LaunchedEffect

            delay(200)
            drawable.value = withContext(Dispatchers.Default) {
                val request = ImageRequest.Builder(context)
                    .data(albumGroup.artworkUri)
                    .build()
                val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                    BitmapResolver.bitmapCompress(this, lowQuality = true)
                }

                if (thisBitmap == null) {return@withContext null}

                val resolvedDrawable = imageResolve(thisBitmap).toDrawable(context.resources)
                thisBitmap.recycle()
                resolvedDrawable
            }
        }
    }

    val shape = LyricsflowRoundedCornerShape(14.dp)

        val density = LocalDensity.current
        Column(
            Modifier
                .fillMaxWidth()
                .padding(top = 14.dp)
                .height(354.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                }
                .drawWithCache {
                    onDrawWithContent {
                        drawContent()
                        val outline = shape.createOutline(
                            Size(size.width, size.height),
                            LayoutDirection.Ltr,
                            density
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.DarkGray.copy(alpha = 0.08f),
                            style = Stroke(width = 8f)
                        )
                        drawOutline(
                            outline = outline,
                            color = Color.DarkGray.copy(alpha = 0.4f),
                            style = Stroke(width = 8f),
                            blendMode = BlendMode.Overlay
                        )
                    }
                }
                .clickable(onClick = onClick)) {
            AsyncImage(
                model = ImageRequest.Builder(LocalContext.current).data(data = albumGroup.artworkUri)
                    .crossfade(true).error(R.drawable.placeholder_music_default_artwork)
                    .placeholder(R.drawable.placeholder_music_default_artwork)
                    .fallback(R.drawable.placeholder_music_default_artwork).build(),
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(268.dp)
            )

            Surface(
                modifier = Modifier.fillMaxSize(),
                color = Color.Black,
                contentColor = Color.White
            ) {
                LyricsflowWrapper {
                    AsyncImage(
                        model = ImageRequest.Builder(LocalContext.current)
                            .data(data = drawable.value).crossfade(true).build(),
                        contentDescription = null,
                        contentScale = ContentScale.FillWidth,
                        modifier = Modifier
                            .fillMaxSize(),
                        colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                    )
                }

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 20.dp),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = albumGroup.albumName,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        lineHeight = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = albumGroup.artistName,
                        fontSize = 13.sp,
                        lineHeight = 13.sp,
                        modifier = Modifier
                            .alpha(0.6f)
                            .padding(top = 2.dp),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
}

@Composable
fun RecommendCardItem(music: LyricsflowMediaItem, onClick: () -> Unit) =
    Column(Modifier.width(268.dp)) {

        val drawable = remember(music.thumb) {
            mutableStateOf<Drawable?>(null)
        }

        val context = LocalContext.current
        val imageLoader = context.imageLoader
        LyricsflowWrapper {
            LaunchedEffect(music.thumb) {
                if (music.thumb == null) return@LaunchedEffect

                delay(200)
                drawable.value = withContext(Dispatchers.Default) {
                    val request = ImageRequest.Builder(context)
                        .data(music.thumb)
                        .build()
                    val thisBitmap = imageLoader.execute(request).drawable?.toBitmap()?.run {
                        BitmapResolver.bitmapCompress(this, lowQuality = true)
                    }

                    if (thisBitmap == null) {return@withContext null}

                    val resolvedDrawable = imageResolve(thisBitmap).toDrawable(context.resources)
                    thisBitmap.recycle()
                    resolvedDrawable
                }
            }
        }

        val shape = LyricsflowRoundedCornerShape(14.dp)
        // val cornerRadiusPx = 12.dp.toPx()

            val density = LocalDensity.current
            Column(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 14.dp)
                    .height(354.dp)
                    .graphicsLayer {
                        compositingStrategy = CompositingStrategy.Offscreen
                        clip = true
                        this.shape = shape
                    }
                    .drawWithCache {
                        onDrawWithContent {
                            drawContent()
                            val outline = shape.createOutline(
                                Size(size.width, size.height),
                                LayoutDirection.Ltr,
                                density
                            )
                            drawOutline(
                                outline = outline,
                                color = Color.DarkGray.copy(alpha = 0.08f),
                                style = Stroke(width = 8f)
                            )
                            drawOutline(
                                outline = outline,
                                color = Color.DarkGray.copy(alpha = 0.4f),
                                style = Stroke(width = 8f),
                                blendMode = BlendMode.Overlay
                            )
                        }
                    }
                    .clickable(onClick = onClick)) {
                AsyncImage(
                    model = ImageRequest.Builder(LocalContext.current).data(data = music.thumb)
                        .crossfade(true).error(R.drawable.placeholder_music_default_artwork)
                        .placeholder(R.drawable.placeholder_music_default_artwork)
                        .fallback(R.drawable.placeholder_music_default_artwork).build(),
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(268.dp)
                )

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = Color.Black,
                    contentColor = Color.White
                ) {
                    LyricsflowWrapper {
                        AsyncImage(
                            model = ImageRequest.Builder(LocalContext.current)
                                .data(data = drawable.value).crossfade(true).build(),
                            contentDescription = null,
                            contentScale = ContentScale.FillWidth,
                            modifier = Modifier
                                .fillMaxSize(),
                            colorFilter = ColorFilter.tint(Color(0x33000000), BlendMode.Overlay)
                        )
                    }

                    Column(
                        Modifier
                            .fillMaxSize()
                            .padding(horizontal = 20.dp),
                        verticalArrangement = Arrangement.Center,
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = music.title ?: defaultTitle,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            lineHeight = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = music.artistsName ?: defaultArtistsName,
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .alpha(0.6f)
                                .padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = music.album ?: defaultAlbum,
                            fontSize = 13.sp,
                            lineHeight = 13.sp,
                            modifier = Modifier
                                .alpha(0.6f)
                                .padding(top = 2.dp),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
    }

/*
fun handleImage(
    image: Bitmap
): Bitmap {
    var saturationBitmap = image.copy(Bitmap.Config.ARGB_8888, true)
    saturationBitmap.applyCanvas {
        val paint = Paint()
        paint.isAntiAlias = true
        paint.isFilterBitmap = true
        paint.isDither = true
        val saturationMatrix = ColorMatrix()
        saturationMatrix.setSaturation(3f)
        paint.colorFilter = ColorMatrixColorFilter(saturationMatrix)
        drawBitmap(saturationBitmap, 0f, 0f, paint)
        //Color(0x40000000)
        drawColor((0x99000000).toInt(), PorterDuff.Mode.OVERLAY)
        drawColor((0x40000000).toInt())
    }
    saturationBitmap = saturationBitmap.scale(16, 16)
    //saturationBitmap = meshBitmap(saturationBitmap)
    saturationBitmap = Toolkit.blur(saturationBitmap, 25)
    return saturationBitmap
}*/
