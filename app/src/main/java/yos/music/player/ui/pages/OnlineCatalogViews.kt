package com.lyricsflow.player.ui.pages

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.OpenableColumns
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.automirrored.rounded.KeyboardArrowRight
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Album
import androidx.compose.material.icons.rounded.ArrowDownward
import androidx.compose.material.icons.rounded.AudioFile
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ConfirmationNumber
import androidx.compose.material.icons.rounded.ContentCopy
import androidx.compose.material.icons.rounded.Description
import androidx.compose.material.icons.rounded.Download
import androidx.compose.material.icons.rounded.GraphicEq
import androidx.compose.material.icons.rounded.Info
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material.icons.rounded.MusicNote
import androidx.compose.material.icons.rounded.PlayArrow
import androidx.compose.material.icons.rounded.Refresh
import androidx.compose.material.icons.rounded.Share
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
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
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
import com.lyricsflow.player.ui.theme.isFlamingoInDarkMode
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.layerBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.rememberLayerBackdrop
import com.lyricsflow.player.ui.widgets.basic.AnimatedAlbumCoverOverlay
import com.lyricsflow.player.ui.widgets.basic.rememberOnlineAnimatedVideoState
import com.lyricsflow.player.ui.widgets.glass.LiquidGlassCircleButton
import com.lyricsflow.player.ui.widgets.glass.LiquidGlassPillGroup
import com.lyricsflow.player.ui.widgets.glass.iosPressScale
import com.lyricsflow.player.ui.widgets.glass.liquidGlass
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.util.Locale

private val AppleCrimson = Color(0xFFFA2D48)

private fun parseHexColorOrNull(hex: String?): Color? {
    val clean = hex?.trim()?.removePrefix("#").orEmpty()
    if (clean.length != 6 && clean.length != 8) return null
    return runCatching {
        val longVal = clean.toLong(16)
        if (clean.length == 6) {
            Color(0xFF000000L or longVal)
        } else {
            Color(longVal)
        }
    }.getOrNull()
}

private fun darkenForAmbientBackground(color: Color?, fallbackDark: Color = Color(0xFF0F0D0B)): Color {
    if (color == null) return fallbackDark
    return Color(
        red = (color.red * 0.24f).coerceIn(0.04f, 0.22f),
        green = (color.green * 0.24f).coerceIn(0.04f, 0.22f),
        blue = (color.blue * 0.24f).coerceIn(0.04f, 0.22f),
        alpha = 1f
    )
}

private fun shareCatalogEntity(context: Context, title: String, subtitle: String, type: String, id: String) {
    runCatching {
        val shareText = buildString {
            append(title)
            if (subtitle.isNotBlank()) append(" — ").append(subtitle)
            if (id.isNotBlank()) append("\nhttps://music.apple.com/us/$type/$id")
        }
        val intent = Intent(Intent.ACTION_SEND).apply {
            this.type = "text/plain"
            putExtra(Intent.EXTRA_TEXT, shareText)
        }
        context.startActivity(Intent.createChooser(intent, "Share $title"))
    }
}

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

    val isDark = isFlamingoInDarkMode()
    val parsedKeyBg = remember(albumDetail?.bgColorHex) {
        parseHexColorOrNull(albumDetail?.bgColorHex)
    }
    val bgColor = if (isDark) {
        darkenForAmbientBackground(parsedKeyBg, Color(0xFF14110E))
    } else {
        Color(0xFFF8F8FA)
    }
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.60f)
    val cardBg = Color(0x12000000) withNight Color(0x1EFFFFFF)

    val resolvedTitle = albumDetail?.name?.ifBlank { fallbackTitle } ?: fallbackTitle.ifBlank { "Album" }
    val resolvedArtist = albumDetail?.artistName?.ifBlank { fallbackArtist } ?: fallbackArtist
    val resolvedArtwork = albumDetail?.artworkUrl?.ifBlank { fallbackArtwork } ?: fallbackArtwork
    val editorial = albumDetail?.editorialNotes.orEmpty()
    var isSavedAlbum by remember(albumId, resolvedTitle) {
        mutableStateOf(SettingsLibrary.isAlbumSaved(albumId.ifBlank { resolvedTitle }))
    }

    val animatedCoverState = rememberOnlineAnimatedVideoState(
        cacheKey = "album_$albumId",
        artistName = resolvedArtist,
        albumName = resolvedTitle,
        albumId = albumId,
        directOrHlsUrl = albumDetail?.animatedVideoUrl,
        active = true
    )

    val heroBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        // Full-bleed iOS 27 Apple Music Animated Album Header with Liquid Glass Controls (Screenshot 3)
        item(key = "online_album_hero") {
            val tracks = albumDetail?.tracks.orEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(475.dp)
            ) {
                // Static high-res cover + animated cover video with smooth bottom fade into ambient bgColor
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(heroBackdrop)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.48f to Color.Black,
                                0.72f to Color.Black.copy(alpha = 0.72f),
                                0.88f to Color.Black.copy(alpha = 0.25f),
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
                    } else {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(
                                    Brush.verticalGradient(
                                        listOf(Color(0xFF2C2520), bgColor)
                                    )
                                )
                        )
                    }
                    AnimatedAlbumCoverOverlay(state = animatedCoverState)
                }

                // Floating iOS 27 Top Liquid Glass Navigation Bar: Left Circle `<` + Right Shared Capsule (`Share` + `•••`)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassCircleButton(
                        onClick = {
                            Vibrator.click(context)
                            navController.popBackStack()
                        },
                        size = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LiquidGlassPillGroup(
                        height = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    shareCatalogEntity(context, resolvedTitle, resolvedArtist, "album", albumId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share Album",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    val key = albumId.ifBlank { resolvedTitle }
                                    SettingsLibrary.toggleAlbumSaved(key)
                                    isSavedAlbum = SettingsLibrary.isAlbumSaved(key)
                                    Toast.makeText(
                                        context,
                                        if (isSavedAlbum) "Added to Library" else "Removed from Library",
                                        Toast.LENGTH_SHORT
                                    ).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More Options",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Overlay: Centered Album Title, Artist Name, Lossless Metadata + 3 iOS 27 Action Buttons (Shuffle, White Pill Play, Download)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 10.dp),
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
                            fontSize = 24.sp,
                            lineHeight = 29.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (albumDetail?.isExplicit == true) {
                            Spacer(modifier = Modifier.width(6.dp))
                            ExplicitBadge()
                        }
                    }

                    Spacer(modifier = Modifier.height(3.dp))

                    Text(
                        text = resolvedArtist,
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 18.sp,
                        color = Color.White.copy(alpha = 0.92f),
                        textAlign = TextAlign.Center,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.iosPressScale {
                            Vibrator.click(context)
                            val targetArtId = albumDetail?.artistId.orEmpty()
                            LibraryObject.openOnlineArtist(
                                artistId = targetArtId,
                                name = resolvedArtist,
                                artworkUrl = ""
                            )
                            navController.toUI(UI.OnlineArtistInfo)
                        }
                    )

                    val audioQualityLabel = remember(albumDetail?.audioTraits) {
                        val traits = albumDetail?.audioTraits.orEmpty()
                        when {
                            traits.any { it.contains("hi-res", ignoreCase = true) } -> "Hi-Res Lossless"
                            traits.any { it.contains("atmos", ignoreCase = true) } -> "Dolby Atmos · Lossless"
                            else -> "Hi-Res Lossless"
                        }
                    }
                    val metaParts = buildList {
                        albumDetail?.genre?.takeIf { it.isNotBlank() }?.let { add(it) }
                        albumDetail?.releaseYear?.takeIf { it.isNotBlank() }?.let { add(it) }
                    }
                    Spacer(modifier = Modifier.height(5.dp))
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        if (metaParts.isNotEmpty()) {
                            Text(
                                text = metaParts.joinToString(" · ") + " · ",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Medium,
                                fontSize = 12.sp,
                                color = Color.White.copy(alpha = 0.68f)
                            )
                        }
                        Icon(
                            imageVector = Icons.Rounded.GraphicEq,
                            contentDescription = null,
                            tint = Color.White.copy(alpha = 0.72f),
                            modifier = Modifier.size(13.dp)
                        )
                        Spacer(modifier = Modifier.width(3.dp))
                        Text(
                            text = audioQualityLabel,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.68f)
                        )
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    // iOS 27 Centered 3-Button Row: Circle Liquid Glass Shuffle | Wide White Pill Play | Circle Liquid Glass Download
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                val randomTrack = tracks.randomOrNull() ?: return@LiquidGlassCircleButton
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(
                                        track = randomTrack,
                                        contextTracks = tracks,
                                        shuffleModeEnabled = true,
                                        appendSimilar = true
                                    )
                                }
                            },
                            size = 48.dp,
                            enabled = tracks.isNotEmpty(),
                            backdrop = heroBackdrop
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Shuffle,
                                contentDescription = "Shuffle",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }

                        // Center Wide White Pill Play Button (`▶ Play`)
                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .width(166.dp)
                                .height(50.dp)
                                .iosPressScale(enabled = tracks.isNotEmpty(), pressedScale = 0.95f) {
                                    Vibrator.click(context)
                                    val first = tracks.firstOrNull() ?: return@iosPressScale
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
                                    tint = Color.Black,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(5.dp))
                                Text(
                                    text = "Play",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 17.sp,
                                    color = Color.Black
                                )
                            }
                        }

                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                val key = albumId.ifBlank { resolvedTitle }
                                SettingsLibrary.toggleAlbumSaved(key)
                                isSavedAlbum = SettingsLibrary.isAlbumSaved(key)
                                Toast.makeText(
                                    context,
                                    if (isSavedAlbum) "Saved $resolvedTitle" else "Removed $resolvedTitle",
                                    Toast.LENGTH_SHORT
                                ).show()
                            },
                            size = 48.dp,
                            backdrop = heroBackdrop
                        ) {
                            Icon(
                                imageVector = if (isSavedAlbum) Icons.Rounded.Check else Icons.Rounded.ArrowDownward,
                                contentDescription = "Save or Download Album",
                                tint = Color.White,
                                modifier = Modifier.size(21.dp)
                            )
                        }
                    }
                }
            }
        }

        // iOS 27 Editorial Quote Preview with Inline `MORE` Button (Screenshot 3)
        if (editorial.isNotBlank()) {
            item(key = "online_album_editorial") {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showEditorialSheet = true }
                        .padding(horizontal = 20.dp, vertical = 10.dp)
                ) {
                    Text(
                        text = editorial,
                        fontFamily = SFProDisplay,
                        fontSize = 14.5.sp,
                        lineHeight = 20.sp,
                        color = textColor.copy(alpha = 0.72f),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.padding(end = 52.dp)
                    )
                    Text(
                        text = "MORE",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.5.sp,
                        color = textColor,
                        modifier = Modifier
                            .align(Alignment.BottomEnd)
                            .background(bgColor.copy(alpha = 0.92f), RoundedCornerShape(4.dp))
                            .padding(start = 6.dp)
                    )
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
            val popularIds = albumDetail?.popularTrackIds.orEmpty()
            val hasMultipleDiscs = tracks.map { it.discNumber }.distinct().size > 1

            // Top divider before track 1 (Screenshot 3)
            if (tracks.isNotEmpty()) {
                item(key = "online_album_top_divider") {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp)
                            .height(0.5.dp)
                            .background(secondaryColor.copy(alpha = 0.22f))
                    )
                }
            }

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
                val isPopular = track.id in popularIds || index < 2
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
                        .padding(start = 10.dp, end = 20.dp, top = 13.dp, bottom = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Popularity Dot (`•`) + Track Number (Screenshot 3)
                    Box(
                        modifier = Modifier.width(14.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (isPopular) {
                            Box(
                                modifier = Modifier
                                    .size(5.dp)
                                    .clip(CircleShape)
                                    .background(secondaryColor.copy(alpha = 0.8f))
                            )
                        }
                    }

                    Text(
                        text = "${track.trackNumber}",
                        fontFamily = SFProDisplay,
                        fontWeight = if (isPlayingThis) FontWeight.Bold else FontWeight.Medium,
                        fontSize = 15.5.sp,
                        color = if (isPlayingThis) AppleCrimson else secondaryColor,
                        modifier = Modifier.width(28.dp)
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
                                fontSize = 16.5.sp,
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

                    Icon(
                        imageVector = Icons.Rounded.MoreHoriz,
                        contentDescription = "Track Options",
                        tint = textColor.copy(alpha = 0.75f),
                        modifier = Modifier
                            .size(20.dp)
                            .clickable {
                                Vibrator.click(context)
                                shareCatalogEntity(context, track.title, track.artist, "song", track.id)
                            }
                    )
                }

                if (index < tracks.lastIndex) {
                    Spacer(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 52.dp, end = 20.dp)
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
// 2. APPLE MUSIC iOS 27 ONLINE ARTIST VIEW (`openArtistView` in `upload.js` — Screenshot 2)
// ============================================================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OnlineArtistView(navController: NavController) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    val artistId by LibraryObject.targetOnlineArtistId
    val fallbackName by LibraryObject.targetOnlineArtistName
    val fallbackArtwork by LibraryObject.targetOnlineArtistArtwork

    var artistDetail by remember(artistId, fallbackName) { mutableStateOf<LyricsFlowApi.CatalogArtistDetail?>(null) }
    var isLoading by remember(artistId, fallbackName) { mutableStateOf(true) }
    var showBioSheet by remember { mutableStateOf(false) }
    var showAllTopSongs by remember(artistId, fallbackName) { mutableStateOf(false) }

    BackHandler { navController.popBackStack() }

    LaunchedEffect(artistId, fallbackName) {
        isLoading = true
        artistDetail = LyricsFlowApi.fetchArtistDetails(
            artistId = artistId.ifBlank { fallbackName },
            fallbackName = fallbackName
        )
        isLoading = false
    }

    val isDark = isFlamingoInDarkMode()
    val bgColor = Color(0xFFF8F8FA) withNight Color(0xFF0A0E14)
    val textColor = Color.Black withNight Color.White
    val secondaryColor = (Color.Black withNight Color.White).copy(alpha = 0.58f)
    val cardBg = Color(0x12000000) withNight Color(0xFF1A2029)

    val resolvedName = artistDetail?.name?.ifBlank { fallbackName } ?: fallbackName.ifBlank { "Artist" }
    val resolvedArtwork = artistDetail?.artworkUrl?.ifBlank { fallbackArtwork } ?: fallbackArtwork
    val resolvedLogoUrl = artistDetail?.logoUrl.orEmpty()
    val concertBadge = artistDetail?.upcomingConcertBadge.orEmpty()

    // Artist signature keyColor (`attr.keyColor`, e.g. #DF3478 hot pink in Screenshot 2)
    val artistAccentColor = remember(artistDetail?.keyColorHex) {
        val parsed = parseHexColorOrNull(artistDetail?.keyColorHex)
        if (parsed != null && parsed.luminance() > 0.08f) parsed else Color(0xFFE03177)
    }

    var isFollowed by remember(resolvedName) {
        mutableStateOf(SettingsLibrary.isArtistFollowed(resolvedName))
    }

    val heroVideoState = rememberOnlineAnimatedVideoState(
        cacheKey = "artist_hero_${artistId.ifBlank { resolvedName }}",
        artistName = resolvedName,
        albumName = "hero",
        directOrHlsUrl = artistDetail?.heroVideoUrl,
        active = !artistDetail?.heroVideoUrl.isNullOrBlank()
    )

    val heroBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        // Apple Music iOS 27 Full-Bleed Artist Hero Banner with Motion Video, Stylized Logo, Concert Pill & 3 Centered Circular Buttons (Screenshot 2)
        item(key = "online_artist_hero") {
            val topSongs = artistDetail?.topSongs.orEmpty()
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(470.dp)
            ) {
                // Full-bleed Portrait Artwork / Motion Video + Ambient Blur Fade at Bottom
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(heroBackdrop)
                        .graphicsLayer {
                            compositingStrategy = CompositingStrategy.Offscreen
                        }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.52f to Color.Black,
                                0.78f to Color.Black.copy(alpha = 0.62f),
                                0.92f to Color.Black.copy(alpha = 0.20f),
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
                                    Brush.verticalGradient(
                                        listOf(artistAccentColor.copy(alpha = 0.75f), bgColor)
                                    )
                                )
                        )
                    }
                    AnimatedAlbumCoverOverlay(state = heroVideoState)
                }

                // Ambient glow behind the artist logo & buttons
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(210.dp)
                        .align(Alignment.BottomCenter)
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color.Transparent,
                                    artistAccentColor.copy(alpha = 0.22f),
                                    bgColor
                                )
                            )
                        )
                )

                // Floating Top iOS 27 Liquid Glass Nav Bar: Left Circle `<` + Right Shared Capsule (`Share` + `•••`)
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassCircleButton(
                        onClick = {
                            Vibrator.click(context)
                            navController.popBackStack()
                        },
                        size = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LiquidGlassPillGroup(
                        height = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    shareCatalogEntity(context, resolvedName, "", "artist", artistDetail?.id ?: artistId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share Artist",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    if (!artistDetail?.bio.isNullOrBlank()) {
                                        showBioSheet = true
                                    } else {
                                        SettingsLibrary.toggleArtistFollowed(resolvedName)
                                        isFollowed = SettingsLibrary.isArtistFollowed(resolvedName)
                                    }
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }

                // Bottom Centered Stack: Concert Badge Pill -> Stylized Artist Logo / Name -> 3 Circular Buttons (`i`, Giant Play, `☆`)
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 20.dp, vertical = 12.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    // Optional Concert Badge (`🗓 20 Aug · Hasselt` in Screenshot 2)
                    if (concertBadge.isNotBlank()) {
                        Row(
                            modifier = Modifier
                                .padding(bottom = 10.dp)
                                .clip(RoundedCornerShape(100.dp))
                                .background(Color.Black.copy(alpha = 0.48f))
                                .border(BorderStroke(0.5.dp, Color.White.copy(alpha = 0.16f)), RoundedCornerShape(100.dp))
                                .padding(horizontal = 12.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.ConfirmationNumber,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(13.dp)
                            )
                            Text(
                                text = concertBadge,
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 12.5.sp,
                                color = Color.White
                            )
                        }
                    }

                    // Stylized Artist Logo (`musicContentColorLogoTrimmed`) OR Bold Centered Title
                    if (resolvedLogoUrl.isNotBlank()) {
                        AsyncImage(
                            model = resolvedLogoUrl,
                            contentDescription = resolvedName,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .height(68.dp)
                                .widthIn(max = 280.dp)
                                .padding(bottom = 14.dp)
                        )
                    } else {
                        Text(
                            text = resolvedName,
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 36.sp,
                            lineHeight = 40.sp,
                            color = Color.White,
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(bottom = 14.dp)
                        )
                    }

                    // 3 Centered Circular Hero Action Buttons (`i` Bio, Giant `artistAccentColor` Play, `☆` Favorite — Screenshot 2)
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(24.dp)
                    ) {
                        // Left: Circular Liquid Glass `i` Info Button
                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                if (!artistDetail?.bio.isNullOrBlank()) {
                                    showBioSheet = true
                                } else {
                                    Toast.makeText(context, resolvedName, Toast.LENGTH_SHORT).show()
                                }
                            },
                            size = 44.dp,
                            backdrop = heroBackdrop
                        ) {
                            Text(
                                text = "i",
                                fontFamily = FontFamily.Serif,
                                fontWeight = FontWeight.Bold,
                                fontSize = 18.sp,
                                color = artistAccentColor
                            )
                        }

                        // Center: Giant 64.dp Solid `artistAccentColor` Circular Play Button
                        Surface(
                            shape = CircleShape,
                            color = artistAccentColor,
                            shadowElevation = 8.dp,
                            modifier = Modifier
                                .size(64.dp)
                                .iosPressScale(enabled = topSongs.isNotEmpty(), pressedScale = 0.93f) {
                                    Vibrator.click(context)
                                    val first = topSongs.firstOrNull() ?: return@iosPressScale
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
                                    tint = Color(0xFF1A0812),
                                    modifier = Modifier.size(34.dp)
                                )
                            }
                        }

                        // Right: Circular Liquid Glass `☆` / `★` Favorite Button
                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                SettingsLibrary.toggleArtistFollowed(resolvedName)
                                isFollowed = SettingsLibrary.isArtistFollowed(resolvedName)
                            },
                            size = 44.dp,
                            backdrop = heroBackdrop
                        ) {
                            Icon(
                                imageVector = if (isFollowed) Icons.Rounded.Star else Icons.Rounded.StarBorder,
                                contentDescription = "Favorite Artist",
                                tint = artistAccentColor,
                                modifier = Modifier.size(22.dp)
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
                    CircularProgressIndicator(color = artistAccentColor)
                }
            }
        } else {
            // 1. Featured / Latest Release Rounded Glass Card (Screenshot 2: `1 May 2026`, `Midnight Sun: Girls Trip [E]`, `20 songs`, right `+` circle)
            artistDetail?.latestRelease?.let { latest ->
                item(key = "online_artist_latest_release") {
                    var isLatestSaved by remember(latest.id) {
                        mutableStateOf(SettingsLibrary.isAlbumSaved(latest.id))
                    }
                    Surface(
                        shape = RoundedCornerShape(24.dp),
                        color = cardBg,
                        border = BorderStroke(0.5.dp, Color.White.copy(alpha = 0.08f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp)
                            .iosPressScale(pressedScale = 0.98f) {
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
                            modifier = Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = latest.artworkUrl,
                                contentDescription = latest.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(96.dp)
                                    .clip(RoundedCornerShape(14.dp))
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .padding(horizontal = 16.dp),
                                verticalArrangement = Arrangement.spacedBy(3.dp)
                            ) {
                                val releaseLabel = latest.editorialSubtitle.ifBlank { latest.releaseYear }
                                if (releaseLabel.isNotBlank()) {
                                    Text(
                                        text = releaseLabel,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 13.sp,
                                        color = secondaryColor
                                    )
                                }
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Text(
                                        text = latest.title,
                                        fontFamily = SFProDisplay,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 16.5.sp,
                                        color = textColor,
                                        maxLines = 2,
                                        overflow = TextOverflow.Ellipsis,
                                        modifier = Modifier.weight(1f, fill = false)
                                    )
                                    if (latest.isExplicit) {
                                        Spacer(modifier = Modifier.width(6.dp))
                                        ExplicitBadge()
                                    }
                                }
                                if (latest.trackCount > 0) {
                                    Text(
                                        text = "${latest.trackCount} songs",
                                        fontFamily = SFProDisplay,
                                        fontSize = 14.sp,
                                        color = secondaryColor
                                    )
                                }
                            }
                            // Right circular `+` / `✓` button (Screenshot 2)
                            Surface(
                                shape = CircleShape,
                                color = Color.White.copy(alpha = if (isDark) 0.12f else 0.08f),
                                modifier = Modifier
                                    .size(36.dp)
                                    .iosPressScale {
                                        Vibrator.click(context)
                                        SettingsLibrary.toggleAlbumSaved(latest.id)
                                        isLatestSaved = SettingsLibrary.isAlbumSaved(latest.id)
                                    }
                            ) {
                                Box(contentAlignment = Alignment.Center) {
                                    Icon(
                                        imageVector = if (isLatestSaved) Icons.Rounded.Check else Icons.Rounded.Add,
                                        contentDescription = "Add Album",
                                        tint = textColor,
                                        modifier = Modifier.size(20.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // 2. Top Songs > Section (Screenshot 2)
            val topSongs = artistDetail?.topSongs.orEmpty()
            if (topSongs.isNotEmpty()) {
                item(key = "online_artist_top_songs_header") {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { showAllTopSongs = !showAllTopSongs }
                            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "Top Songs",
                            fontFamily = SFProDisplay,
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = textColor
                        )
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                            contentDescription = "See All Top Songs",
                            tint = secondaryColor,
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }

                val displayedSongs = if (showAllTopSongs) topSongs else topSongs.take(8)
                itemsIndexed(displayedSongs, key = { idx, s -> "artist_top_song_${s.id}_$idx" }) { index, song ->
                    Column {
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
                                .padding(horizontal = 20.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            AsyncImage(
                                model = song.artworkUrl,
                                contentDescription = song.title,
                                contentScale = ContentScale.Crop,
                                modifier = Modifier
                                    .size(52.dp)
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
                                val subParts = buildList {
                                    song.album.takeIf { it.isNotBlank() }?.let { add(it) }
                                    song.releaseYear?.let { add(it.toString()) }
                                }
                                Text(
                                    text = subParts.joinToString(" · ").ifBlank { resolvedName },
                                    fontFamily = SFProDisplay,
                                    fontSize = 13.5.sp,
                                    color = secondaryColor,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More",
                                tint = textColor.copy(alpha = 0.72f),
                                modifier = Modifier
                                    .size(20.dp)
                                    .clickable {
                                        Vibrator.click(context)
                                        shareCatalogEntity(context, song.title, song.artist, "song", song.id)
                                    }
                            )
                        }
                        if (index < displayedSongs.lastIndex) {
                            Spacer(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(start = 86.dp, end = 20.dp)
                                    .height(0.5.dp)
                                    .background(secondaryColor.copy(alpha = 0.18f))
                            )
                        }
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
                            shape = RoundedCornerShape(20.dp),
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
                                    text = "MORE",
                                    fontFamily = SFProDisplay,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 12.sp,
                                    color = artistAccentColor
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
                                        .iosPressScale {
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

    val heroBackdrop = rememberLayerBackdrop {
        drawRect(bgColor)
        drawContent()
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentPadding = PaddingValues(bottom = 185.dp)
    ) {
        val tracks = playlistDetail?.tracks.orEmpty()
        item(key = "online_playlist_hero") {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(450.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .layerBackdrop(heroBackdrop)
                        .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                        .drawWithCache {
                            val fadeBrush = Brush.verticalGradient(
                                0.00f to Color.Black,
                                0.52f to Color.Black,
                                0.80f to Color.Black.copy(alpha = 0.45f),
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
                        .padding(horizontal = 16.dp, vertical = 10.dp)
                        .align(Alignment.TopCenter),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    LiquidGlassCircleButton(
                        onClick = {
                            Vibrator.click(context)
                            navController.popBackStack()
                        },
                        size = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }

                    LiquidGlassPillGroup(
                        height = 42.dp,
                        backdrop = heroBackdrop
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    shareCatalogEntity(context, resolvedTitle, resolvedCurator, "playlist", playlistId)
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.Share,
                                contentDescription = "Share Playlist",
                                tint = Color.White,
                                modifier = Modifier.size(18.dp)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .iosPressScale {
                                    Vibrator.click(context)
                                    Toast.makeText(context, resolvedTitle, Toast.LENGTH_SHORT).show()
                                },
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Rounded.MoreHoriz,
                                contentDescription = "More",
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
                        .padding(horizontal = 22.dp, vertical = 10.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = resolvedTitle,
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Bold,
                        fontSize = 25.sp,
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
                        modifier = Modifier.padding(top = 3.dp, bottom = 14.dp)
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(18.dp)
                    ) {
                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                val rand = tracks.randomOrNull() ?: return@LiquidGlassCircleButton
                                scope.launch(Dispatchers.IO) {
                                    MediaController.playOnlineTrack(rand, tracks, shuffleModeEnabled = true)
                                }
                            },
                            size = 48.dp,
                            enabled = tracks.isNotEmpty(),
                            backdrop = heroBackdrop
                        ) {
                            Icon(Icons.Rounded.Shuffle, "Shuffle", tint = Color.White, modifier = Modifier.size(21.dp))
                        }

                        Surface(
                            shape = RoundedCornerShape(100.dp),
                            color = Color.White,
                            shadowElevation = 6.dp,
                            modifier = Modifier
                                .width(166.dp)
                                .height(50.dp)
                                .iosPressScale(enabled = tracks.isNotEmpty(), pressedScale = 0.95f) {
                                    Vibrator.click(context)
                                    val first = tracks.firstOrNull() ?: return@iosPressScale
                                    scope.launch(Dispatchers.IO) {
                                        MediaController.playOnlineTrack(first, tracks, shuffleModeEnabled = false)
                                    }
                                }
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.Center
                            ) {
                                Icon(Icons.Rounded.PlayArrow, null, tint = Color.Black, modifier = Modifier.size(24.dp))
                                Spacer(modifier = Modifier.width(5.dp))
                                Text("Play", fontFamily = SFProDisplay, fontWeight = FontWeight.Bold, fontSize = 17.sp, color = Color.Black)
                            }
                        }

                        LiquidGlassCircleButton(
                            onClick = {
                                Vibrator.click(context)
                                Toast.makeText(context, "Saved $resolvedTitle", Toast.LENGTH_SHORT).show()
                            },
                            size = 48.dp,
                            backdrop = heroBackdrop
                        ) {
                            Icon(Icons.Rounded.ArrowDownward, "Save", tint = Color.White, modifier = Modifier.size(21.dp))
                        }
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
