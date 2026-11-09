package com.lyricsflow.player.ui.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Shuffle
import androidx.compose.material.icons.rounded.Star
import androidx.compose.material.icons.rounded.StarBorder
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
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
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import coil.compose.AsyncImage
import com.google.accompanist.insets.statusBarsPadding
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.lrc.LyricsFlowApi
import com.lyricsflow.player.code.utils.lrc.LyricsFlowCatalogItem
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.basic.AnimatedAlbumCoverOverlay
import com.lyricsflow.player.ui.widgets.basic.rememberOnlineAnimatedVideoState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val AppleCrimson = Color(0xFFFA2D48)

// ============================================================================
// 1. APPLE MUSIC ONLINE ALBUM VIEW (`fetchAlbumDetails` in `upload.js`)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineAlbumView(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val albumId by LibraryObject.targetOnlineAlbumId
    val fallbackTitle by LibraryObject.targetOnlineAlbumTitle
    val fallbackArtist by LibraryObject.targetOnlineAlbumArtist
    val fallbackArtwork by LibraryObject.targetOnlineAlbumArtwork

    var albumDetail by remember(albumId) { mutableStateOf<LyricsFlowApi.CatalogAlbumDetail?>(null) }
    var isLoading by remember(albumId) { mutableStateOf(true) }
    var showEditorialSheet by remember { mutableStateOf(false) }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(albumId) {
        if (albumId.isBlank()) {
            isLoading = false
            return@LaunchedEffect
        }
        isLoading = true
        albumDetail = LyricsFlowApi.fetchAlbumDetails(
            albumId = albumId,
            fallbackTitle = fallbackTitle,
            fallbackArtist = fallbackArtist
        )
        isLoading = false
    }

    val bgColor = Color(0xFFF8F8FA) withNight Color(0xFF0A0A0C)
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)

    val resolvedTitle = albumDetail?.name?.ifBlank { fallbackTitle } ?: fallbackTitle.ifBlank { "Album" }
    val resolvedArtist = albumDetail?.artistName?.ifBlank { fallbackArtist } ?: fallbackArtist
    val resolvedArtwork = albumDetail?.artworkUrl?.ifBlank { fallbackArtwork } ?: fallbackArtwork
    val editorial = albumDetail?.editorialNotes.orEmpty()

    val animatedCoverState = rememberOnlineAnimatedVideoState(
        cacheKey = "album_$albumId",
        artistName = resolvedArtist,
        albumName = resolvedTitle,
        albumId = albumId,
        directOrHlsUrl = albumDetail?.animatedVideoUrl,
        active = true
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        // Full-bleed Apple Music Animated Album Header with bottom gradient fade
        item(key = "online_album_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(430.dp)
            ) {
                // Static high-res cover + animated cover video with smooth bottom fade
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.58f to Color.Black,
                                0.78f to Color.Black.copy(alpha = 0.65f),
                                0.92f to Color.Black.copy(alpha = 0.18f),
                                1.00f to Color.Transparent
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                            }
                        }
                ) {
                    if (resolvedArtwork.isNotBlank()) {
                        AsyncImage(
                            model = resolvedArtwork,
                            contentDescription = resolvedTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AnimatedAlbumCoverOverlay(state = animatedCoverState)
                }

                // Floating Top Bar
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.42f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                Vibrator.click(context)
                                navController.popBackStack()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Album Title, Artist Link, Metadata at bottom of hero fade
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 22.dp, vertical = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = resolvedTitle,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 25.sp,
                            lineHeight = 30.sp,
                            color = textColor,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (albumDetail?.isExplicit == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            ExplicitBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(4.dp))

                    Text(
                        text = resolvedArtist,
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 18.sp,
                        color = AppleCrimson,
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.clickable {
                            Vibrator.click(context)
                            val targetArtId = albumDetail?.artistId.orEmpty()
                            if (targetArtId.isNotBlank()) {
                                LibraryObject.openOnlineArtist(
                                    artistId = targetArtId,
                                    name = resolvedArtist,
                                    artworkUrl = resolvedArtwork
                                )
                                navController.toUI(UI.OnlineArtistInfo)
                            }
                        }
                    )

                    val metaParts = buildList {
                        albumDetail?.genre?.takeIf { it.isNotBlank() }?.uppercase(Locale.ROOT)?.let { add(it) }
                        albumDetail?.releaseYear?.takeIf { it.isNotBlank() }?.let { add(it) }
                        add("LOSSLESS")
                    }
                    if (metaParts.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = metaParts.joinToString(" · "),
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            letterSpacing = 0.6.sp,
                            color = secondaryColor
                        )
                    }
                }
            }
        }

        // Apple Music Play & Shuffle Pill Buttons
        item(key = "online_album_actions") {
            val tracks = albumDetail?.tracks.orEmpty()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppleCrimson,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(enabled = tracks.isNotEmpty()) {
                            Vibrator.click(context)
                            val first = tracks.firstOrNull() ?: return@clickable
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(
                                    track = first,
                                    contextTracks = tracks,
                                    shuffleModeEnabled = false,
                                    appendSimilar = true
                                )
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = Color.White,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Play",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = Color.White
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(enabled = tracks.isNotEmpty()) {
                            Vibrator.click(context)
                            val randomTrack = tracks.randomOrNull() ?: return@clickable
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(
                                    track = randomTrack,
                                    contextTracks = tracks,
                                    shuffleModeEnabled = true,
                                    appendSimilar = true
                                )
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.Shuffle,
                            contentDescription = "Shuffle",
                            tint = AppleCrimson,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = "Shuffle",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            color = AppleCrimson
                        )
                    }
                }
            }
        }

        // Editorial Notes Preview Card
        if (editorial.isNotBlank()) {
            item(key = "online_album_editorial") {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 6.dp)
                        .clickable { showEditorialSheet = true }
                ) {
                    Column(modifier = Modifier.padding(14.dp)) {
                        Text(
                            text = editorial,
                            fontFamily = SFProDisplay,
                            fontSize = 13.5.sp,
                            lineHeight = 19.sp,
                            color = secondaryColor,
                            maxLines = 3,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "MORE",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 11.5.sp,
                            color = AppleCrimson
                        )
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "online_album_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleCrimson)
                }
            }
        } else {
            val tracks = albumDetail?.tracks.orEmpty()
            val hasMultipleDiscs = tracks.map { it.discNumber }.distinct().size > 1

            itemsIndexed(tracks, key = { idx, t -> "album_track_${t.id}_$idx" }) { index, track ->
                val prevDisc = tracks.getOrNull(index - 1)?.discNumber
                if (hasMultipleDiscs && track.discNumber != prevDisc) {
                    Text(
                        text = "DISC ${track.discNumber}",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        letterSpacing = 0.8.sp,
                        color = secondaryColor,
                        modifier = Modifier.padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 6.dp)
                    )
                }

                val isPlayingThis = MediaController.musicPlaying.value?.mediaId == track.id
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(
                                    track = track,
                                    contextTracks = tracks,
                                    shuffleModeEnabled = false,
                                    appendSimilar = true
                                )
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "${track.trackNumber}",
                        fontFamily = SFProDisplay,
                        fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.sp,
                        color = if (isPlayingThis) AppleCrimson else secondaryColor,
                        modifier = Modifier.width(30.dp)
                    )

                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(end = 10.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 16.sp,
                                color = if (isPlayingThis) AppleCrimson else textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (track.isExplicit) {
                                Spacer(modifier = Modifier.width(6.dp))
                                ExplicitBadge()
                            }
                        }
                        if (track.artist.isNotBlank() && !track.artist.equals(resolvedArtist, ignoreCase = true)) {
                            Text(
                                text = track.artist,
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }

                    if (track.durationMs > 0L) {
                        Text(
                            text = formatDurationMs(track.durationMs),
                            fontFamily = SFProDisplay,
                            fontSize = 13.sp,
                            color = secondaryColor
                        )
                    }
                }

                if (index < tracks.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 50.dp, end = 20.dp)
                            .height(0.5.dp)
                            .background(secondaryColor.copy(alpha = 0.2f))
                    )
                }
            }

            // Album Footer Metadata (Release Date, Track Count, Duration, Copyright, Record Label)
            item(key = "online_album_footer") {
                val totalMin = (tracks.sumOf { it.durationMs } / 60000L).coerceAtLeast(1L)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    albumDetail?.releaseDateFormatted?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontFamily = SFProDisplay,
                            fontSize = 12.5.sp,
                            color = secondaryColor
                        )
                    }
                    if (tracks.isNotEmpty()) {
                        Text(
                            text = "${tracks.size} ${if (tracks.size == 1) "Song" else "Songs"}, $totalMin Minutes",
                            fontFamily = SFProDisplay,
                            fontSize = 12.5.sp,
                            color = secondaryColor
                        )
                    }
                    albumDetail?.copyright?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontFamily = SFProDisplay,
                            fontSize = 12.sp,
                            color = secondaryColor
                        )
                    }
                    albumDetail?.recordLabel?.takeIf { it.isNotBlank() }?.let {
                        Text(
                            text = it,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = AppleCrimson
                        )
                    }
                }
            }

            // More by Artist Shelf
            val moreByArtist = albumDetail?.moreByArtist.orEmpty()
            if (moreByArtist.isNotEmpty()) {
                item(key = "online_album_more_by_artist") {
                    OnlineAlbumHorizontalShelf(
                        title = "More by $resolvedArtist",
                        items = moreByArtist,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlineAlbum(
                                albumId = item.id,
                                title = item.title,
                                artist = item.subtitle.ifBlank { resolvedArtist },
                                artworkUrl = item.artworkUrl
                            )
                        }
                    )
                }
            }

            // Related Albums Shelf
            val related = albumDetail?.relatedAlbums.orEmpty()
            if (related.isNotEmpty()) {
                item(key = "online_album_related") {
                    OnlineAlbumHorizontalShelf(
                        title = "You Might Also Like",
                        items = related,
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
                        }
                    )
                }
            }
        }
    }

    if (showEditorialSheet && editorial.isNotBlank()) {
        ModalBottomSheet(
            onDismissRequest = { showEditorialSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF161618),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Text(
                    text = resolvedTitle,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 22.sp,
                    color = Color.White
                )
                Text(
                    text = resolvedArtist,
                    fontFamily = SFProDisplay,
                    fontSize = 15.sp,
                    color = Color.White.copy(alpha = 0.65f),
                    modifier = Modifier.padding(top = 2.dp, bottom = 14.dp)
                )
                Text(
                    text = editorial,
                    fontFamily = SFProDisplay,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

// ============================================================================
// 2. APPLE MUSIC ONLINE ARTIST VIEW (`openArtistView` in `upload.js`)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineArtistView(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val artistId by LibraryObject.targetOnlineArtistId
    val fallbackName by LibraryObject.targetOnlineArtistName
    val fallbackArtwork by LibraryObject.targetOnlineArtistArtwork

    var artistDetail by remember(artistId) { mutableStateOf<LyricsFlowApi.CatalogArtistDetail?>(null) }
    var isLoading by remember(artistId) { mutableStateOf(true) }
    var showBioSheet by remember { mutableStateOf(false) }
    var showAllTopSongs by remember(artistId) { mutableStateOf(false) }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(artistId, fallbackName) {
        isLoading = true
        artistDetail = LyricsFlowApi.fetchArtistDetails(
            artistId = artistId.ifBlank { fallbackName },
            fallbackName = fallbackName
        )
        isLoading = false
    }

    val bgColor = Color(0xFFF8F8FA) withNight Color(0xFF0A0A0C)
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)

    val resolvedName = artistDetail?.name?.ifBlank { fallbackName } ?: fallbackName.ifBlank { "Artist" }
    val resolvedArtwork = artistDetail?.artworkUrl?.ifBlank { fallbackArtwork } ?: fallbackArtwork
    val isFollowed = SettingsLibrary.isArtistFollowed(resolvedName)

    val heroVideoState = rememberOnlineAnimatedVideoState(
        cacheKey = "artist_hero_${artistId.ifBlank { resolvedName }}",
        artistName = resolvedName,
        albumName = "hero",
        directOrHlsUrl = artistDetail?.heroVideoUrl,
        active = !artistDetail?.heroVideoUrl.isNullOrBlank()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        // Apple Music Full-Bleed Artist Hero Banner with Motion Video & Bottom Gradient Fade
        item(key = "online_artist_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.55f to Color.Black,
                                0.80f to Color.Black.copy(alpha = 0.55f),
                                1.00f to Color.Transparent
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                            }
                        }
                ) {
                    if (resolvedArtwork.isNotBlank()) {
                        AsyncImage(
                            model = resolvedArtwork,
                            contentDescription = resolvedName,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.linearGradient(
                                        listOf(Color(0xFFFA2D48), Color(0xFF1F1F24))
                                    )
                                )
                        )
                    }
                    AnimatedAlbumCoverOverlay(state = heroVideoState)
                }

                // Floating Top Action Bar (Back, Favorite Star, Info Bio)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.45f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                Vibrator.click(context)
                                navController.popBackStack()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = CircleShape,
                            color = Color.Black.copy(alpha = 0.45f),
                            modifier = Modifier
                                .size(38.dp)
                                .clickable {
                                    Vibrator.click(context)
                                    SettingsLibrary.toggleArtistFollowed(resolvedName)
                                }
                        ) {
                            Box(contentAlignment = Alignment.Center) {
                                Icon(
                                    imageVector = if (isFollowed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                    contentDescription = "Favorite Artist",
                                    tint = if (isFollowed) AppleCrimson else Color.White,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        }

                        if (!artistDetail?.bio.isNullOrBlank()) {
                            Surface(
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.45f),
                                modifier = Modifier
                                    .size(38.dp)
                                    .clickable {
                                        Vibrator.click(context)
                                        showBioSheet = true
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = Icons.Rounded.Info,
                                        contentDescription = "About Artist",
                                        tint = Color.White,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }

                // Bottom Overlay: Artist Name + Crimson Circular Play Button
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = resolvedName,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 34.sp,
                            lineHeight = 38.sp,
                            color = textColor,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        artistDetail?.genre?.takeIf { it.isNotBlank() }?.let { g ->
                            Text(
                                text = g,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                color = secondaryColor
                            )
                        }
                    }

                    val topSongs = artistDetail?.topSongs.orEmpty()
                    Surface(
                        shape = CircleShape,
                        color = AppleCrimson,
                        modifier = Modifier
                            .size(50.dp)
                            .clickable(enabled = topSongs.isNotEmpty()) {
                                Vibrator.click(context)
                                val first = topSongs.firstOrNull() ?: return@clickable
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(
                                        track = first,
                                        contextTracks = topSongs,
                                        shuffleModeEnabled = false,
                                        appendSimilar = true
                                    )
                                }
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.Rounded.PlayArrow,
                                contentDescription = "Play Artist",
                                tint = Color.White,
                                modifier = Modifier.size(28.dp)
                            )
                        }
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "online_artist_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleCrimson)
                }
            }
        } else {
            // 1. Latest Release Card (`upload.js` `artist-latest-card`)
            artistDetail?.latestRelease?.let { latest ->
                item(key = "online_artist_latest_release") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "Latest Release",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(14.dp),
                            color = cardBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    Vibrator.click(context)
                                    LibraryObject.openOnlineAlbum(
                                        albumId = latest.id,
                                        title = latest.title,
                                        artist = resolvedName,
                                        artworkUrl = latest.artworkUrl
                                    )
                                    navController.toUI(UI.OnlineAlbumInfo)
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                AsyncImage(
                                    model = latest.artworkUrl,
                                    contentDescription = latest.title,
                                    contentScale = ContentScale.Crop,
                                    modifier = Modifier
                                        .size(86.dp)
                                        .clip(RoundedCornerShape(10.dp))
                                )
                                Column(
                                    modifier = Modifier
                                        .weight(1f)
                                        .padding(horizontal = 14.dp)
                                ) {
                                    if (latest.releaseYear.isNotBlank()) {
                                        Text(
                                            text = latest.releaseYear,
                                            fontFamily = SFProDisplay,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 11.sp,
                                            color = AppleCrimson
                                        )
                                    }
                                    Text(
                                        text = latest.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 17.sp,
                                        color = textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    if (latest.trackCount > 0) {
                                        Text(
                                            text = "${latest.trackCount} songs",
                                            fontFamily = SFProDisplay,
                                            fontSize = 13.sp,
                                            color = secondaryColor,
                                            modifier = Modifier.padding(top = 2.dp)
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // 2. Top Songs Section
            val topSongs = artistDetail?.topSongs.orEmpty()
            if (topSongs.isNotEmpty()) {
                item(key = "online_artist_top_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Songs",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = textColor
                        )
                        if (topSongs.size > 6) {
                            Text(
                                text = if (showAllTopSongs) "Show Less" else "See All",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = AppleCrimson,
                                modifier = Modifier.clickable {
                                    showAllTopSongs = !showAllTopSongs
                                }
                            )
                        }
                    }
                }

                val displayedSongs = if (showAllTopSongs) topSongs else topSongs.take(6)
                itemsIndexed(displayedSongs, key = { idx, s -> "artist_top_song_${s.id}_$idx" }) { index, song ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(
                                        track = song,
                                        contextTracks = topSongs,
                                        shuffleModeEnabled = false,
                                        appendSimilar = true
                                    )
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = song.artworkUrl,
                            contentDescription = song.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(50.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 14.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = song.title,
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 16.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis,
                                    modifier = Modifier.weight(1f, fill = false)
                                )
                                if (song.isExplicit) {
                                    Spacer(modifier = Modifier.width(6.dp))
                                    ExplicitBadge()
                                }
                            }
                            Text(
                                text = song.album.ifBlank { resolvedName },
                                fontFamily = SFProDisplay,
                                fontSize = 13.sp,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.PlayArrow,
                            contentDescription = "Play",
                            tint = AppleCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }

            // 3. Essential Albums Shelf
            val essentials = artistDetail?.essentialAlbums.orEmpty()
            if (essentials.isNotEmpty()) {
                item(key = "online_artist_essential_albums") {
                    OnlineAlbumHorizontalShelf(
                        title = "Essential Albums",
                        items = essentials,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlineAlbum(
                                albumId = item.id,
                                title = item.title,
                                artist = resolvedName,
                                artworkUrl = item.artworkUrl
                            )
                            navController.toUI(UI.OnlineAlbumInfo)
                        }
                    )
                }
            }

            // 4. Full Albums Shelf
            val albums = artistDetail?.albums.orEmpty()
            if (albums.isNotEmpty()) {
                item(key = "online_artist_albums") {
                    OnlineAlbumHorizontalShelf(
                        title = "Albums",
                        items = albums,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlineAlbum(
                                albumId = item.id,
                                title = item.title,
                                artist = resolvedName,
                                artworkUrl = item.artworkUrl
                            )
                            navController.toUI(UI.OnlineAlbumInfo)
                        }
                    )
                }
            }

            // 5. Singles & EPs Shelf
            val singles = artistDetail?.singlesAndEps.orEmpty()
            if (singles.isNotEmpty()) {
                item(key = "online_artist_singles") {
                    OnlineAlbumHorizontalShelf(
                        title = "Singles & EPs",
                        items = singles,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlineAlbum(
                                albumId = item.id,
                                title = item.title,
                                artist = resolvedName,
                                artworkUrl = item.artworkUrl
                            )
                            navController.toUI(UI.OnlineAlbumInfo)
                        }
                    )
                }
            }

            // 6. Featured Playlists Shelf
            val playlists = artistDetail?.playlists.orEmpty()
            if (playlists.isNotEmpty()) {
                item(key = "online_artist_playlists") {
                    OnlineAlbumHorizontalShelf(
                        title = "$resolvedName Playlists",
                        items = playlists,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlinePlaylist(
                                playlistId = item.id,
                                name = item.title,
                                artworkUrl = item.artworkUrl
                            )
                            navController.toUI(UI.OnlinePlaylistInfo)
                        }
                    )
                }
            }

            // 7. Appears On Shelf
            val appearsOn = artistDetail?.appearsOn.orEmpty()
            if (appearsOn.isNotEmpty()) {
                item(key = "online_artist_appears_on") {
                    OnlineAlbumHorizontalShelf(
                        title = "Appears On",
                        items = appearsOn,
                        textColor = textColor,
                        secondaryColor = secondaryColor,
                        onAlbumClick = { item ->
                            Vibrator.click(context)
                            LibraryObject.openOnlineAlbum(
                                albumId = item.id,
                                title = item.title,
                                artist = item.subtitle.ifBlank { resolvedName },
                                artworkUrl = item.artworkUrl
                            )
                            navController.toUI(UI.OnlineAlbumInfo)
                        }
                    )
                }
            }

            // 8. About Artist Editorial Card
            val bio = artistDetail?.bio.orEmpty()
            if (bio.isNotBlank()) {
                item(key = "online_artist_bio_card") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 18.dp)
                    ) {
                        Text(
                            text = "About $resolvedName",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = textColor,
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        Surface(
                            shape = RoundedCornerShape(16.dp),
                            color = cardBg,
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { showBioSheet = true }
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = bio,
                                    fontFamily = SFProDisplay,
                                    fontSize = 14.5.sp,
                                    lineHeight = 21.sp,
                                    color = secondaryColor,
                                    maxLines = 5,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = "READ MORE",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = AppleCrimson
                                )
                            }
                        }
                    }
                }
            }

            // 9. Similar Artists Circular Shelf
            val similar = artistDetail?.similarArtists.orEmpty()
            if (similar.isNotEmpty()) {
                item(key = "online_artist_similar") {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 14.dp, bottom = 10.dp)
                    ) {
                        Text(
                            text = "Similar Artists",
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
                            horizontalArrangement = Arrangement.spacedBy(16.dp)
                        ) {
                            similar.forEach { simArtist ->
                                Column(
                                    modifier = Modifier
                                        .width(108.dp)
                                        .clickable {
                                            Vibrator.click(context)
                                            LibraryObject.openOnlineArtist(
                                                artistId = simArtist.id,
                                                name = simArtist.title,
                                                artworkUrl = simArtist.artworkUrl
                                            )
                                        },
                                    horizontalAlignment = Alignment.CenterHorizontally
                                ) {
                                    AsyncImage(
                                        model = simArtist.artworkUrl,
                                        contentDescription = simArtist.title,
                                        contentScale = ContentScale.Crop,
                                        modifier = Modifier
                                            .size(104.dp)
                                            .clip(CircleShape)
                                            .background(cardBg)
                                    )
                                    Spacer(modifier = Modifier.height(6.dp))
                                    Text(
                                        text = simArtist.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.5.sp,
                                        color = textColor,
                                        textAlign = TextAlign.Center,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    if (showBioSheet && !artistDetail?.bio.isNullOrBlank()) {
        ModalBottomSheet(
            onDismissRequest = { showBioSheet = false },
            sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true),
            containerColor = Color(0xFF161618),
            contentColor = Color.White
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp)
            ) {
                Text(
                    text = resolvedName,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp,
                    color = Color.White
                )
                val meta = buildList {
                    artistDetail?.genre?.takeIf { it.isNotBlank() }?.let { add("Genre: $it") }
                    artistDetail?.origin?.takeIf { it.isNotBlank() }?.let { add("Hometown: $it") }
                    artistDetail?.formedYear?.takeIf { it.isNotBlank() }?.let { add("Formed: $it") }
                }
                if (meta.isNotEmpty()) {
                    Text(
                        text = meta.joinToString(" · "),
                        fontFamily = SFProDisplay,
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.6f),
                        modifier = Modifier.padding(top = 4.dp, bottom = 14.dp)
                    )
                }
                Text(
                    text = artistDetail?.bio.orEmpty(),
                    fontFamily = SFProDisplay,
                    fontSize = 15.sp,
                    lineHeight = 22.sp,
                    color = Color.White.copy(alpha = 0.9f),
                    modifier = Modifier.padding(bottom = 32.dp)
                )
            }
        }
    }
}

// ============================================================================
// 3. APPLE MUSIC ONLINE PLAYLIST VIEW (`openRemotePlaylistView` in `upload.js`)
// ============================================================================

@Composable
fun OnlinePlaylistView(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val playlistId by LibraryObject.targetOnlinePlaylistId
    val fallbackName by LibraryObject.targetOnlinePlaylistName
    val fallbackArtwork by LibraryObject.targetOnlinePlaylistArtwork

    var playlistDetail by remember(playlistId) { mutableStateOf<LyricsFlowApi.CatalogPlaylistDetail?>(null) }
    var isLoading by remember(playlistId) { mutableStateOf(true) }
    var suggestedSongs by remember(playlistId) { mutableStateOf<List<LyricsFlowApi.CatalogTrackResult>>(emptyList()) }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(playlistId) {
        isLoading = true
        val fetched = LyricsFlowApi.fetchPlaylistDetails(playlistId, fallbackName)
        playlistDetail = fetched
        suggestedSongs = fetched?.suggestedSongs.orEmpty()
        isLoading = false
    }

    val bgColor = Color(0xFFF8F8FA) withNight Color(0xFF0A0A0C)
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)

    val resolvedTitle = playlistDetail?.name?.ifBlank { fallbackName } ?: fallbackName.ifBlank { "Playlist" }
    val resolvedCurator = playlistDetail?.curatorName ?: "Apple Music"
    val resolvedArtwork = playlistDetail?.artworkUrl?.ifBlank { fallbackArtwork } ?: fallbackArtwork

    val animatedCoverState = rememberOnlineAnimatedVideoState(
        cacheKey = "playlist_$playlistId",
        artistName = resolvedCurator,
        albumName = resolvedTitle,
        directOrHlsUrl = playlistDetail?.animatedVideoUrl,
        active = !playlistDetail?.animatedVideoUrl.isNullOrBlank()
    )

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        item(key = "online_playlist_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(410.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.58f to Color.Black,
                                0.85f to Color.Black.copy(alpha = 0.35f),
                                1.00f to Color.Transparent
                            )
                            onDrawWithContent {
                                drawContent()
                                drawRect(brush = fadeBrush, blendMode = BlendMode.DstIn)
                            }
                        }
                ) {
                    if (resolvedArtwork.isNotBlank()) {
                        AsyncImage(
                            model = resolvedArtwork,
                            contentDescription = resolvedTitle,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }
                    AnimatedAlbumCoverOverlay(state = animatedCoverState)
                }

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Surface(
                        shape = CircleShape,
                        color = Color.Black.copy(alpha = 0.42f),
                        modifier = Modifier
                            .size(38.dp)
                            .clickable {
                                Vibrator.click(context)
                                navController.popBackStack()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 22.dp, vertical = 8.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resolvedTitle,
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = textColor,
                        textAlign = TextAlign.Center,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = resolvedCurator,
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.SemiBold,
                        fontSize = 16.sp,
                        color = AppleCrimson,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        }

        val tracks = playlistDetail?.tracks.orEmpty()
        item(key = "online_playlist_actions") {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 10.dp),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = AppleCrimson,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(enabled = tracks.isNotEmpty()) {
                            Vibrator.click(context)
                            val first = tracks.firstOrNull() ?: return@clickable
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(first, tracks, shuffleModeEnabled = false)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.PlayArrow, null, tint = Color.White, modifier = Modifier.size(22.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Play", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                    }
                }

                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = cardBg,
                    modifier = Modifier
                        .weight(1f)
                        .height(48.dp)
                        .clickable(enabled = tracks.isNotEmpty()) {
                            Vibrator.click(context)
                            val rand = tracks.randomOrNull() ?: return@clickable
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(rand, tracks, shuffleModeEnabled = true)
                            }
                        }
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Icon(Icons.Rounded.Shuffle, null, tint = AppleCrimson, modifier = Modifier.size(20.dp))
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("Shuffle", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = AppleCrimson)
                    }
                }
            }
        }

        if (isLoading) {
            item(key = "online_playlist_loading") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 48.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = AppleCrimson)
                }
            }
        } else {
            itemsIndexed(tracks, key = { idx, t -> "pl_track_${t.id}_$idx" }) { _, track ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable {
                            Vibrator.click(context)
                            scope.launch(Dispatchers.IO) {
                                MediaController.playOnlineTrack(track, tracks)
                            }
                        }
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    AsyncImage(
                        model = track.artworkUrl,
                        contentDescription = track.title,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .size(48.dp)
                            .clip(RoundedCornerShape(8.dp))
                    )
                    Column(
                        modifier = Modifier
                            .weight(1f)
                            .padding(horizontal = 14.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = track.title,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.5.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.weight(1f, fill = false)
                            )
                            if (track.isExplicit) {
                                Spacer(modifier = Modifier.width(6.dp))
                                ExplicitBadge()
                            }
                        }
                        Text(
                            text = track.artist,
                            fontFamily = SFProDisplay,
                            fontSize = 13.sp,
                            color = secondaryColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    if (track.durationMs > 0L) {
                        Text(
                            text = formatDurationMs(track.durationMs),
                            fontFamily = SFProDisplay,
                            fontSize = 12.5.sp,
                            color = secondaryColor
                        )
                    }
                }
            }

            // Suggested Songs Section (`playlistSuggestedSongs` in `upload.js`)
            if (suggestedSongs.isNotEmpty()) {
                item(key = "online_playlist_suggested_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 22.dp, bottom = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                text = "Suggested Songs",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 19.sp,
                                color = textColor
                            )
                            Text(
                                text = "Based on songs in this playlist",
                                fontFamily = SFProDisplay,
                                fontSize = 12.5.sp,
                                color = secondaryColor
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Refresh,
                            contentDescription = "Refresh Suggestions",
                            tint = AppleCrimson,
                            modifier = Modifier
                                .size(22.dp)
                                .clickable {
                                    val seed = tracks.randomOrNull() ?: return@clickable
                                    scope.launch(Dispatchers.IO) {
                                        suggestedSongs = LyricsFlowApi.fetchSimilarSongs(seed.id, seed.artist).take(6)
                                    }
                                }
                        )
                    }
                }

                itemsIndexed(suggestedSongs, key = { idx, s -> "pl_sug_${s.id}_$idx" }) { _, sug ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                Vibrator.click(context)
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(sug, suggestedSongs)
                                }
                            }
                            .padding(horizontal = 20.dp, vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        AsyncImage(
                            model = sug.artworkUrl,
                            contentDescription = sug.title,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .size(44.dp)
                                .clip(RoundedCornerShape(8.dp))
                        )
                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .padding(horizontal = 12.dp)
                        ) {
                            Text(
                                text = sug.title,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 15.sp,
                                color = textColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = sug.artist,
                                fontFamily = SFProDisplay,
                                fontSize = 12.5.sp,
                                color = secondaryColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.Add,
                            contentDescription = "Play Next",
                            tint = AppleCrimson,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                }
            }
        }
    }
}

// ============================================================================
// 4. LOCAL UPLOAD AUDIO + TTML PAIRING & TTML DOWNLOADER (`upload.js`)
// ============================================================================

@Composable
fun LocalUploadAndTtmlToolsView(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    var selectedAudioUri by remember { mutableStateOf<Uri?>(null) }
    var selectedAudioName by remember { mutableStateOf("") }
    var selectedLyricText by remember { mutableStateOf<String?>(null) }
    var selectedLyricName by remember { mutableStateOf("") }

    var downloaderInput by remember { mutableStateOf("") }
    var downloaderArtist by remember { mutableStateOf("") }
    var isDownloadingTtml by remember { mutableStateOf(false) }
    var downloadedFileName by remember { mutableStateOf("") }
    var downloadedTtmlContent by remember { mutableStateOf("") }

    BackHandler { navController.popBackStack() }

    val audioLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedAudioUri = uri
            selectedAudioName = resolveFileName(context, uri).ifBlank { "Uploaded Audio" }
        }
    }

    val lyricLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) {
            selectedLyricName = resolveFileName(context, uri).ifBlank { "lyrics.ttml" }
            scope.launch(Dispatchers.IO) {
                val content = runCatching {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
                }.getOrNull()
                selectedLyricText = content
            }
        }
    }

    val bgColor = Color(0xFFF8F8FA) withNight Color(0xFF0A0A0C)
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .statusBarsPadding(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 185.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        item(key = "upload_tools_header") {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Surface(
                    shape = CircleShape,
                    color = cardBg,
                    modifier = Modifier
                        .size(38.dp)
                        .clickable { navController.popBackStack() }
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.AutoMirrored.Rounded.ArrowBack, "Back", tint = textColor, modifier = Modifier.size(20.dp))
                    }
                }
                Column {
                    Text(
                        text = "LyricsFlow Studio & Tools",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 26.sp,
                        color = textColor
                    )
                    Text(
                        text = "Pair local audio with .ttml files or download Apple Music TTML",
                        fontFamily = SFProDisplay,
                        fontSize = 13.sp,
                        color = secondaryColor
                    )
                }
            }
        }

        // Card 1: Local Audio + TTML Pairing (`pairAndLoadFiles` in `upload.js`)
        item(key = "local_upload_pairing_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Upload & Pair Local Track + TTML",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = textColor
                    )
                    Text(
                        text = "Select any audio file (.mp3, .flac, .m4a, .wav) and an optional .ttml or .lrc file to play with Apple Music word-synced animations.",
                        fontFamily = SFProDisplay,
                        fontSize = 13.5.sp,
                        color = secondaryColor
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleCrimson.copy(alpha = 0.16f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { audioLauncher.launch("audio/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.AudioFile, null, tint = AppleCrimson, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedAudioName.ifBlank { "Select Audio" },
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = AppleCrimson.copy(alpha = 0.16f),
                            modifier = Modifier
                                .weight(1f)
                                .clickable { lyricLauncher.launch("*/*") }
                        ) {
                            Row(
                                modifier = Modifier.padding(12.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(Icons.Rounded.Description, null, tint = AppleCrimson, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = selectedLyricName.ifBlank { "Select .TTML/.LRC" },
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 13.5.sp,
                                    color = textColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = if (selectedAudioUri != null) AppleCrimson else AppleCrimson.copy(alpha = 0.35f),
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable(enabled = selectedAudioUri != null) {
                                val uri = selectedAudioUri ?: return@clickable
                                val cleanTitle = selectedAudioName.substringBeforeLast(".")
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playUploadedLocalTrack(
                                        audioUri = uri,
                                        title = cleanTitle,
                                        artist = "LyricsFlow Studio",
                                        pairedLyricContent = selectedLyricText
                                    )
                                }
                                Toast.makeText(context, "Playing $cleanTitle with LyricsFlow Engine", Toast.LENGTH_SHORT).show()
                            }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = "Play Paired Track Now",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.5.sp,
                                color = Color.White
                            )
                        }
                    }
                }
            }
        }

        // Card 2: TTML Downloader (`initTtmlDownloader` in `upload.js`)
        item(key = "ttml_downloader_card") {
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = cardBg,
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Apple Music TTML Lyrics Downloader",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 19.sp,
                        color = textColor
                    )
                    Text(
                        text = "Enter an Apple Music Song ID (e.g. 1658934092) or Song Title + Artist to fetch word-by-word TTML lyrics.",
                        fontFamily = SFProDisplay,
                        fontSize = 13.5.sp,
                        color = secondaryColor
                    )

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (downloaderInput.isEmpty()) {
                            Text(
                                text = "Apple Music Song ID or Track Title",
                                fontFamily = SFProDisplay,
                                fontSize = 15.sp,
                                color = secondaryColor
                            )
                        }
                        BasicTextField(
                            value = downloaderInput,
                            onValueChange = { downloaderInput = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = SFProDisplay,
                                fontSize = 15.sp,
                                color = textColor
                            ),
                            cursorBrush = SolidColor(AppleCrimson),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .background(Color.Black.copy(alpha = 0.2f))
                            .padding(horizontal = 14.dp, vertical = 12.dp)
                    ) {
                        if (downloaderArtist.isEmpty()) {
                            Text(
                                text = "Artist Name (optional if using Song ID)",
                                fontFamily = SFProDisplay,
                                fontSize = 15.sp,
                                color = secondaryColor
                            )
                        }
                        BasicTextField(
                            value = downloaderArtist,
                            onValueChange = { downloaderArtist = it },
                            singleLine = true,
                            textStyle = TextStyle(
                                fontFamily = SFProDisplay,
                                fontSize = 15.sp,
                                color = textColor
                            ),
                            cursorBrush = SolidColor(AppleCrimson),
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = AppleCrimson,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(46.dp)
                            .clickable(enabled = !isDownloadingTtml && downloaderInput.isNotBlank()) {
                                isDownloadingTtml = true
                                scope.launch {
                                    val result = LyricsFlowApi.downloadRawLyricsForTrack(downloaderInput, downloaderArtist)
                                    isDownloadingTtml = false
                                    if (result != null) {
                                        downloadedFileName = result.first
                                        downloadedTtmlContent = result.second
                                        // Save to app external/cache downloads
                                        withContext(Dispatchers.IO) {
                                            runCatching {
                                                val outFile = File(context.getExternalFilesDir(null) ?: context.filesDir, result.first)
                                                outFile.writeText(result.second, Charsets.UTF_8)
                                            }
                                        }
                                        Toast.makeText(context, "Fetched ${result.first}", Toast.LENGTH_SHORT).show()
                                    } else {
                                        Toast.makeText(context, "No TTML/LRC found for this query", Toast.LENGTH_SHORT).show()
                                    }
                                }
                            }
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.Center
                        ) {
                            if (isDownloadingTtml) {
                                CircularProgressIndicator(
                                    color = Color.White,
                                    strokeWidth = 2.dp,
                                    modifier = Modifier.size(18.dp)
                                )
                            } else {
                                Icon(Icons.Rounded.Download, null, tint = Color.White, modifier = Modifier.size(20.dp))
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = "Fetch TTML Lyrics",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.5.sp,
                                    color = Color.White
                                )
                            }
                        }
                    }

                    if (downloadedTtmlContent.isNotBlank()) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = downloadedFileName,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp,
                                color = AppleCrimson
                            )
                            Surface(
                                shape = RoundedCornerShape(50),
                                color = AppleCrimson.copy(alpha = 0.16f),
                                modifier = Modifier.clickable {
                                    val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                                    cm?.setPrimaryClip(ClipData.newPlainText("TTML Lyrics", downloadedTtmlContent))
                                    Toast.makeText(context, "Copied TTML to clipboard", Toast.LENGTH_SHORT).show()
                                }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(Icons.Rounded.ContentCopy, null, tint = AppleCrimson, modifier = Modifier.size(14.dp))
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text("Copy XML", fontFamily = SFProDisplay, fontWeight = FontWeight.SemiBold, fontSize = 12.sp, color = AppleCrimson)
                                }
                            }
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp)
                                .clip(RoundedCornerShape(10.dp))
                                .background(Color.Black.copy(alpha = 0.3f))
                                .padding(10.dp)
                        ) {
                            Text(
                                text = downloadedTtmlContent.take(1500),
                                fontFamily = SFProDisplay,
                                fontSize = 11.5.sp,
                                lineHeight = 15.sp,
                                color = secondaryColor
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// SHARED APPLE MUSIC SHELF & BADGE COMPONENTS
// ============================================================================

@Composable
fun OnlineAlbumHorizontalShelf(
    title: String,
    items: List<LyricsFlowCatalogItem>,
    textColor: Color,
    secondaryColor: Color,
    onAlbumClick: (LyricsFlowCatalogItem) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 18.dp, bottom = 6.dp)
    ) {
        Text(
            text = title,
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
            items.forEach { item ->
                Column(
                    modifier = Modifier
                        .width(150.dp)
                        .clickable { onAlbumClick(item) }
                ) {
                    Box(
                        modifier = Modifier
                            .size(150.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color.White.copy(alpha = 0.08f))
                    ) {
                        if (item.artworkUrl.isNotBlank()) {
                            AsyncImage(
                                model = item.artworkUrl,
                                contentDescription = item.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier.fillMaxSize()
                            )
                        } else {
                            Icon(
                                imageVector = Icons.Rounded.Album,
                                contentDescription = null,
                                tint = secondaryColor,
                                modifier = Modifier
                                    .size(42.dp)
                                    .align(Alignment.Center)
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = item.title,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 14.5.sp,
                            color = textColor,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        if (item.isExplicit) {
                            Spacer(modifier = Modifier.width(4.dp))
                            ExplicitBadge()
                        }
                    }
                    val sub = item.releaseYear.ifBlank { item.subtitle }
                    if (sub.isNotBlank()) {
                        Text(
                            text = sub,
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
    }
}

@Composable
fun ExplicitBadge() {
    Box(
        modifier = Modifier
            .size(16.dp)
            .clip(RoundedCornerShape(3.dp))
            .background(Color.Gray.copy(alpha = 0.35f)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "E",
            fontFamily = SFProDisplay,
            fontWeight = FontWeight.Bold,
            fontSize = 9.5.sp,
            color = Color.White.copy(alpha = 0.85f)
        )
    }
}

private fun formatDurationMs(ms: Long): String {
    val totalSec = (ms / 1000L).coerceAtLeast(0L)
    val min = totalSec / 60L
    val sec = totalSec % 60L
    return String.format(Locale.US, "%d:%02d", min, sec)
}

private fun resolveFileName(context: Context, uri: Uri): String {
    return runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) cursor.getString(idx) else null
        }
    }.getOrNull() ?: uri.lastPathSegment.orEmpty()
}
