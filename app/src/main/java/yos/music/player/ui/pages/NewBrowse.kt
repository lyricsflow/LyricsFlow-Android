package com.lyricsflow.player.ui.pages

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.insets.statusBarsPadding
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.artistsName
import com.lyricsflow.player.data.libraries.defaultAlbum
import com.lyricsflow.player.data.libraries.defaultArtistsName
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.ProfileButton
import com.lyricsflow.player.ui.widgets.basic.ShadowImageWithCache
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Composable
fun NewBrowse(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val allSongs = remember(MusicLibrary.songs.size) { MusicLibrary.songs }
    val allAlbumNames = remember(allSongs.size) { MusicLibrary.albums }

    var trendingTracks by remember { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }
    var editorialShelves by remember { mutableStateOf<List<LyricsFlowApi.EditorialShelf>>(emptyList()) }
    var isLoadingOnline by remember { mutableStateOf(true) }

    LaunchedEffect(Unit) {
        val trendingJob = launch(Dispatchers.IO) {
            trendingTracks = LyricsFlowApi.fetchTrendingTracks(limit = 30)
        }
        val editorialJob = launch(Dispatchers.IO) {
            editorialShelves = LyricsFlowApi.fetchEditorialNewShelves()
        }
        trendingJob.join()
        editorialJob.join()
        isLoadingOnline = false
    }

    val textColor = Color.Black withNight Color.White
    val secondaryTextColor = (Color.Black withNight Color.White).copy(alpha = 0.56f)
    val appleCrimson = Color(0xFFFA2D48)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding(),
        contentPadding = PaddingValues(bottom = 175.dp)
    ) {
        // Apple Music "New" Header
        item(key = "new_header") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(start = 20.dp, end = 20.dp, top = 26.dp, bottom = 14.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "New",
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 34.sp,
                    lineHeight = 38.sp,
                    color = textColor
                )
                ProfileButton { navController.toUI(UI.Settings.Main) }
            }
        }

        // Hero Editorial Showcases
        item(key = "new_hero_cards") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(horizontal = 20.dp, vertical = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                EditorialHeroCard(
                    eyebrow = "LYRICSFLOW STUDIO",
                    title = "Upload Audio & TTML Tools",
                    subtitle = "Pair local tracks with .ttml files or download Apple Music TTML",
                    gradientColors = listOf(Color(0xFFFA2D48), Color(0xFF7928CA)),
                    onClick = {
                        Vibrator.click(context)
                        navController.toUI(UI.LocalUploadTools)
                    }
                )
                EditorialHeroCard(
                    eyebrow = "SPATIAL & MOTION",
                    title = "Animated Cover Art",
                    subtitle = "Full-bleed canvas motion powered by LyricsFlow API",
                    gradientColors = listOf(Color(0xFF0A84FF), Color(0xFF1E293B)),
                    onClick = {
                        Vibrator.click(context)
                        val firstTrending = trendingTracks.firstOrNull()
                        if (firstTrending != null) {
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(firstTrending, trendingTracks)
                            }
                        } else if (allSongs.isNotEmpty()) {
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(allSongs.first(), allSongs)
                            }
                        }
                    }
                )
            }
        }

        // 1. Live Trending on LyricsFlow (`renderTrendingShelf` in `upload.js`)
        if (trendingTracks.isNotEmpty()) {
            item(key = "new_trending_title") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = "Trending on LyricsFlow",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = textColor
                        )
                        Text(
                            text = "Most played word-synced tracks right now",
                            fontFamily = SFProDisplay,
                            fontSize = 13.sp,
                            color = secondaryTextColor
                        )
                    }
                }
            }

            itemsIndexed(trendingTracks.take(15), key = { idx, t -> "trending_${t.id}_$idx" }) { index, track ->
                Row(
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
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${index + 1}",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 15.sp,
                        color = if (index < 3) appleCrimson else secondaryTextColor,
                        modifier = Modifier.width(28.dp)
                    )
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(50.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = track.title,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = track.artist,
                            fontFamily = SFProDisplay,
                            fontSize = 13.sp,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Icon(
                        imageVector = Icons.Rounded.PlayArrow,
                        contentDescription = "Play",
                        tint = appleCrimson,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        } else if (isLoadingOnline) {
            item(key = "new_online_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 28.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = appleCrimson, modifier = Modifier.size(26.dp))
                }
            }
        }

        // 2. Live Apple Music Editorial Shelves (`renderNewPage` in `upload.js`)
        items(editorialShelves, key = { "editorial_shelf_${it.id}" }) { shelf ->
            OnlineAlbumHorizontalShelf(
                title = shelf.title,
                items = shelf.items,
                textColor = textColor,
                secondaryColor = secondaryTextColor,
                onAlbumClick = { item ->
                    Vibrator.click(context)
                    if (item.type == "playlists") {
                        LibraryObject.openOnlinePlaylist(
                            playlistId = item.id,
                            name = item.title,
                            artworkUrl = item.artworkUrl
                        )
                        navController.toUI(UI.OnlinePlaylistInfo)
                    } else {
                        LibraryObject.openOnlineAlbum(
                            albumId = item.id,
                            title = item.title,
                            artist = item.subtitle,
                            artworkUrl = item.artworkUrl
                        )
                        navController.toUI(UI.OnlineAlbumInfo)
                    }
                }
            )
        }

        // 3. Featured Albums in Local Library
        if (allAlbumNames.isNotEmpty()) {
            item(key = "new_albums_title") {
                Text(
                    text = "Albums in Your Library",
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = textColor,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 10.dp)
                )
            }
            item(key = "new_albums_row") {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    allAlbumNames.take(12).forEach { albumName ->
                        val songs = MusicLibrary.Album[albumName]
                        val rep = songs.firstOrNull()
                        Column(
                            modifier = Modifier
                                .width(152.dp)
                                .clickable {
                                    Vibrator.click(context)
                                    LibraryObject.setTargetAlbumName(albumName)
                                    navController.toUI(UI.AlbumInfo)
                                }
                        ) {
                            ShadowImageWithCache(
                                dataLambda = { rep?.thumb },
                                contentDescription = albumName,
                                modifier = Modifier.size(152.dp),
                                cornerRadius = 12.dp,
                                shadowAlpha = 0f,
                                imageQuality = ImageQuality.MID
                            )
                            Spacer(modifier = Modifier.height(7.dp))
                            Text(
                                text = albumName,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = rep?.artistsName ?: defaultArtistsName,
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryTextColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        // 4. Local Songs Section
        if (allSongs.isNotEmpty()) {
            item(key = "new_top_songs_title") {
                Text(
                    text = "Songs in Your Library",
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 21.sp,
                    color = textColor,
                    modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 24.dp, bottom = 8.dp)
                )
            }

            items(allSongs.take(15), key = { "new_song_${it.uri}" }) { song ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.prepare(song, allSongs)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ShadowImageWithCache(
                        dataLambda = { song.thumb },
                        contentDescription = song.title,
                        modifier = Modifier.size(50.dp),
                        cornerRadius = 8.dp,
                        shadowAlpha = 0f,
                        imageQuality = ImageQuality.LOW
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Text(
                            text = song.title ?: defaultTitle,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 16.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = "${song.artistsName ?: defaultArtistsName} · ${song.album ?: defaultAlbum}",
                            fontFamily = SFProDisplay,
                            fontSize = 13.sp,
                            color = secondaryTextColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun EditorialHeroCard(
    eyebrow: String,
    title: String,
    subtitle: String,
    gradientColors: List<Color>,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        modifier = Modifier
            .width(300.dp)
            .height(186.dp)
            .clip(RoundedCornerShape(16.dp))
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(gradientColors))
                .padding(18.dp)
        ) {
            Column(
                modifier = Modifier.align(Alignment.TopStart)
            ) {
                Text(
                    text = eyebrow,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 0.8.sp,
                    color = Color.White.copy(alpha = 0.78f)
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = title,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    lineHeight = 26.sp,
                    color = Color.White
                )
            }
            Text(
                text = subtitle,
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.Medium,
                fontSize = 13.5.sp,
                lineHeight = 18.sp,
                color = Color.White.copy(alpha = 0.88f),
                modifier = Modifier.align(Alignment.BottomStart)
            )
        }
    }
}
