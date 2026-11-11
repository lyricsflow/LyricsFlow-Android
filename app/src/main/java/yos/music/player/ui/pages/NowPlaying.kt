@file:Suppress("DEPRECATION")

package com.lyricsflow.player.ui.pages

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import android.widget.Toast
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.animate
import androidx.compose.animation.core.tween
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.Crossfade
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.LocalOverscrollConfiguration
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.material.ripple
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.SliderPositions
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.lerp
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastMap
import androidx.compose.ui.zIndex
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.media3.common.Player.REPEAT_MODE_ALL
import androidx.media3.common.Player.REPEAT_MODE_OFF
import androidx.media3.common.Player.REPEAT_MODE_ONE
import androidx.media3.common.util.UnstableApi
import androidx.navigation.NavController
import com.blankj.utilcode.util.TimeUtils
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.insets.statusBarsHeight
import com.google.accompanist.insets.statusBarsPadding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import com.lyricsflow.player.R
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.MediaController.mediaControl
import com.lyricsflow.player.code.MediaController.musicPlaying
import com.lyricsflow.player.code.MediaController.playingMusicList
import com.lyricsflow.player.code.MediaController.queueShuffleEnabled
import com.lyricsflow.player.code.SystemMediaControlResolver
import com.lyricsflow.player.code.VolumeChangeReceiver
import com.lyricsflow.player.code.LyricsflowPlaybackService
import com.lyricsflow.player.code.utils.lrc.LyricsflowMediaEvent
import com.lyricsflow.player.code.utils.lrc.LyricsflowUIConfig
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.code.utils.player.FadeExo.fadePause
import com.lyricsflow.player.code.utils.player.FadeExo.fadePlay
import com.lyricsflow.player.code.utils.player.VocalAttenuationAudioProcessor
import com.lyricsflow.player.ui.widgets.glass.liquidGlass
import com.lyricsflow.player.ui.widgets.glass.liquidGlassCircle
import com.lyricsflow.player.data.libraries.FavPlayListLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem
import com.lyricsflow.player.data.libraries.artistsList
import com.lyricsflow.player.data.libraries.artistsName
import com.lyricsflow.player.data.libraries.defaultArtistsName
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.models.MainViewModel
import com.lyricsflow.player.data.models.MediaViewModel
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.data.objects.LibraryObject
import com.lyricsflow.player.ui.pages.NowPlayingPage.Album
import com.lyricsflow.player.ui.pages.NowPlayingPage.Lyric
import com.lyricsflow.player.ui.pages.NowPlayingPage.PlayingList
import com.lyricsflow.player.ui.theme.LyricsflowRoundedCornerShape
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.markNextNavigationFromNowPlaying
import com.lyricsflow.player.ui.toUI
import com.lyricsflow.player.ui.widgets.LyricsflowLyricView
import com.lyricsflow.player.ui.widgets.effects.LyricsflowFloatingLight
import com.lyricsflow.player.ui.widgets.audio.MusicQualityIndicator
import coil.compose.AsyncImage
import coil.request.ImageRequest
import com.lyricsflow.player.ui.widgets.basic.ActionItem
import com.lyricsflow.player.ui.widgets.basic.SheetAnimatedContent
import com.lyricsflow.player.ui.widgets.basic.SheetNavigationBackward
import com.lyricsflow.player.ui.widgets.basic.SheetNavigationForward
import com.lyricsflow.player.ui.widgets.basic.ActionSheetBody
import com.lyricsflow.player.ui.widgets.basic.AnimatedAlbumCoverState
import com.lyricsflow.player.ui.widgets.basic.AnimatedAlbumCoverOverlay
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.LyricsflowBottomSheetDialog
import com.lyricsflow.player.ui.widgets.basic.rememberAnimatedAlbumCoverState
import com.lyricsflow.player.ui.widgets.playlist.PlayListPickerContent
import com.lyricsflow.player.ui.widgets.sleeptimer.SleepTimerContent
import com.lyricsflow.player.code.SleepTimer
import com.lyricsflow.player.code.SleepTimerState
import com.lyricsflow.player.ui.widgets.basic.ShadowImageWithCache
import com.lyricsflow.player.ui.widgets.basic.LyricsflowWrapper
import com.lyricsflow.player.ui.widgets.effects.ShadowType
import com.lyricsflow.player.ui.widgets.effects.overlayEffect
import sh.calvin.reorderable.ReorderableItem
import sh.calvin.reorderable.rememberReorderableLazyListState


@Stable
object NowPlayingPage {
    const val Album = "Album"
    const val PlayingList = "PlayingList"
    const val Lyric = "Lyric"
}

private const val AnimDurationMillis = 300
private const val ShareAlbumKey = "album"
private val QueueRowHeight = 64.dp
private val QueueDraggingItemShape = RoundedCornerShape(0.dp)

private data class QueueReorderTarget(
    val nextInQueue: Boolean,
    val index: Int,
)

private fun resolveQueueReorderTarget(
    lazyListIndex: Int,
    nextInQueueSize: Int,
    upNextSize: Int,
): QueueReorderTarget? {
    var cursor = 1

    if (nextInQueueSize > 0) {
        cursor += 1

        if (lazyListIndex in cursor until cursor + nextInQueueSize) {
            return QueueReorderTarget(true, lazyListIndex - cursor)
        }

        cursor += nextInQueueSize
    }

    if (upNextSize > 0) {
        cursor += 1

        if (lazyListIndex in cursor until cursor + upNextSize) {
            return QueueReorderTarget(false, lazyListIndex - cursor)
        }
    }

    return null
}

private fun queueItemKey(
    sectionKey: String,
    music: LyricsflowMediaItem,
    index: Int,
    sectionItems: List<LyricsflowMediaItem>,
): String {
    val duplicateOrdinal = sectionItems
        .take(index + 1)
        .count { it.uri == music.uri && it.mediaId == music.mediaId }

    return "$sectionKey:${music.uri}:${music.mediaId}:$duplicateOrdinal"
}

/*
private val MaterialFadeInTransitionSpec
    get() = SharedElementsTransitionSpec(
        pathMotionFactory = LinearMotionFactory,
        durationMillis = AnimDurationMillis,
        fadeMode = FadeMode.In,
        easing = EaseOutQuart
    )

private val MaterialFadeOutTransitionSpec
    get() = SharedElementsTransitionSpec(
        pathMotionFactory = LinearMotionFactory,
        durationMillis = AnimDurationMillis,
        fadeMode = FadeMode.Out,
        easing = EaseOutQuart
    )
*/

@ExperimentalSharedTransitionApi
@androidx.annotation.OptIn(UnstableApi::class)
@Composable
fun NowPlaying(
    mainViewModel: MainViewModel,
    mediaViewModel: MediaViewModel,
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    isPlayingStatusLambda: () -> Boolean,
    isPlayingOnChanged: (Boolean) -> Unit,
    nowPageLambda: () -> String,
    showNowPlaying: () -> Boolean,
    showMiniPlayer: () -> Boolean,
    nowPageOnChanged: (String) -> Unit
) =
    Surface(
        modifier = Modifier.fillMaxSize(),
        contentColor = Color.White,
        color = Color.Transparent
    ) {
        val context = LocalContext.current

        val lrcEntries: MutableState<List<List<Pair<Float, String>>>> =
            MediaViewModelObject.lrcEntries
        val bitmap: MutableState<Uri?> = MediaViewModelObject.bitmap
        val lyricLineEndTimes = MediaViewModelObject.lyricLineEndTimes
        val lyricLineTransliterations = MediaViewModelObject.lyricLineTransliterations
        val lyricLineSubtitles = MediaViewModelObject.lyricLineSubtitles
        val isTtmlLyrics = MediaViewModelObject.isTtmlLyrics

        val thisMusicPlaying = remember("NowPlaying_thisMusicPlaying") {
            musicPlaying
        }
        val lifecycleState = LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()
        val animatedAlbumCoverState = rememberAnimatedAlbumCoverState(
            music = thisMusicPlaying.value,
            isPlaying = isPlayingStatusLambda(),
            active = nowPageLambda() == Album &&
                showNowPlaying() &&
                lifecycleState.value.isAtLeast(Lifecycle.State.STARTED)
        )

        val lastClickTime = rememberSaveable(key = "NowPlaying_lastClickTime") {
            mutableLongStateOf(0L)
        }

        val showControl = rememberSaveable(key = "NowPlaying_showControl") {
            mutableStateOf(true)
        }

        val translation = rememberSaveable(key = "NowPlaying_translation") {
            mutableStateOf(SettingsLibrary.NowPlayingTranslation)
        }

        val shuffleModeEnabled = rememberSaveable(key = "NowPlaying_shuffleModeEnabled") {
            mutableStateOf(queueShuffleEnabled.value)
        }
        val repeatMode = rememberSaveable(key = "NowPlaying_repeatMode") {
            mutableIntStateOf(mediaControl?.repeatMode ?: REPEAT_MODE_OFF)
        }

        /*val nowPage = rememberSaveable(key = "NowPlaying_nowPage") {
            MainViewModelObject.nowPage
        }*/

        // 触摸超时
        LyricsflowWrapper {
            LaunchedEffect(showControl.value, nowPageLambda(), lastClickTime.longValue) {
                if (nowPageLambda() != Lyric && !showControl.value) {
                    showControl.value = true
                }
                if (showControl.value) {
                    val time = 2500L
                    delay(time)
                    withContext(Dispatchers.Main) {
                        if (TimeUtils.getNowMills() - lastClickTime.longValue >= time && nowPageLambda() == Lyric) {
                            showControl.value = false
                        }
                    }
                }
            }
        }


        // 背景流光
        LyricsflowWrapper {
            /*BlendBackgroundView(
        bitmapLambda = { bitmap.value },
        isPlayingLambda = { isPlaying.value },
        nowPage = { nowPage.value }
    )*/
            LyricsflowFloatingLight(
                album = { bitmap.value },
                isPlaying = isPlayingStatusLambda,
                modifier = Modifier.fillMaxSize(),
                nowPage = { nowPageLambda() },
                showMiniPlayer = showMiniPlayer
            )

            // Apple Music Full-Bleed Animated Cover Art with smooth bottom fade
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.TopCenter
            ) {
                com.lyricsflow.player.ui.widgets.basic.FullBleedAnimatedAlbumCover(
                    state = animatedAlbumCoverState,
                    visible = nowPageLambda() == Album
                )
            }
        }


        // 实际显示区
        LyricsflowWrapper {
            /*
        val controlAlpha = animateFloatAsState(
            targetValue = if (showControl.value) 1f else 0f,
            tween(200)
        )

        val buttonEnabled = remember("NowPlaying_buttonEnabled") {
            derivedStateOf { controlAlpha.value != 0f }
        }

        val translationButtonEnabled = remember("NowPlaying_translationButtonEnabled") {
            derivedStateOf { buttonEnabled.value && alpha.value != 0f }
        }*/

            val scope = rememberCoroutineScope()

            val alphaAnim = remember { Animatable(0f) }

            LyricsflowWrapper {
                LaunchedEffect(nowPageLambda()) {
                    val targetAlpha = if (nowPageLambda() == Lyric) 1f else 0f
                    scope.launch {
                        alphaAnim.animateTo(targetAlpha)
                    }
                }
            }

            val lyricSecondaryTextAvailable = remember("NowPlaying_lyricSecondaryTextAvailable") {
                derivedStateOf {
                    if (isTtmlLyrics.value) {
                        lyricLineTransliterations.any { !it.isNullOrBlank() } ||
                                lyricLineSubtitles.any { !it.isNullOrBlank() }
                    } else {
                        lrcEntries.value.any { it.lastOrNull()?.second?.isNotBlank() == true }
                    }
                }
            }

            val translationButtonEnabled = remember("NowPlaying_translationButtonEnabled") {
                derivedStateOf {
                    showControl.value && alphaAnim.value != 0f && lyricSecondaryTextAvailable.value
                }
            }

            // 歌词
            LyricsflowWrapper {

                Column(
                    Modifier
                        .fillMaxSize()
                        .graphicsLayer {
                            compositingStrategy =
                                CompositingStrategy.ModulateAlpha
                            this.alpha = alphaAnim.value
                        }
                ) {
                    Lyric(
                        lrcEntries = { lrcEntries.value },
                        lineEndTimes = { lyricLineEndTimes },
                        lineTransliterations = { lyricLineTransliterations },
                        lineSubtitles = { lyricLineSubtitles },
                        isTtmlLyrics = { isTtmlLyrics.value },
                        weightLambda = { showControl.value },
                        translationLambda = { translation.value },
                        onBackClick = {
                            showControl.value = true
                            lastClickTime.longValue =
                                TimeUtils.getNowMills()
                        },
                        mainViewModel = mainViewModel,
                        mediaViewModel = mediaViewModel
                    )
                }
            }

            // 这是小把手
            LyricsflowWrapper {
                Column(Modifier.fillMaxWidth()) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .statusBarsPadding()
                            .padding(top = 20.dp), contentAlignment = Alignment.Center
                    ) {
                        Box(
                            Modifier
                                .overlayEffect()
                                .size(
                                    width = 32.dp,
                                    height = 4.5.dp
                                )
                                .background(Color(0x4DFFFFFF), RoundedCornerShape(2.25.dp))
                                .clip(RoundedCornerShape(2.25.dp))
                        )
                    }
                }
            }

            // 主 View
            LyricsflowWrapper {
                SharedTransitionLayout {
                    Crossfade(
                        targetState = nowPageLambda(),
                        modifier = Modifier
                            .fillMaxSize()
                            .statusBarsPadding()
                            .padding(top = 22.dp)
                    ) {
                        //println("nowPage: ${nowPageLambda()}")
                        //println("nowPageIt: $it")
                        when (it) {
                            Album ->
                                Column(
                                    Modifier
                                        .fillMaxSize()
                                        .clickable(enabled = false, onClick = {})
                                ) {
                                    LyricsflowWrapper {
                                        Column(Modifier.fillMaxHeight(0.595f)) {
                                            val isVisible = nowPageLambda() == Album

                                            Album(
                                                modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                    sharedContentState = rememberSharedContentState(
                                                        key = ShareAlbumKey
                                                    ),
                                                    visible = isVisible
                                                ),
                                                music = { thisMusicPlaying.value },
                                                animatedAlbumCoverState = animatedAlbumCoverState,
                                                isPlaying = isPlayingStatusLambda
                                            )
                                            AnimatedContent(
                                                targetState = thisMusicPlaying.value,
                                                transitionSpec = {
                                                    fadeIn() togetherWith fadeOut()
                                                }, modifier = Modifier.padding(horizontal = 32.dp)
                                            ) {
                                                val subtitleMenuExpanded = remember { mutableStateOf(false) }
                                                val navScope = rememberCoroutineScope()
                                                Row(
                                                    Modifier
                                                        .fillMaxWidth(),
                                                    verticalAlignment = Alignment.CenterVertically
                                                ) {
                                                    Box(
                                                        Modifier
                                                            .fillMaxWidth()
                                                            .weight(1f)
                                                            .padding(end = 15.dp)
                                                    ) {
                                                        Column(Modifier.fillMaxWidth()) {
                                                            Row(verticalAlignment = Alignment.CenterVertically) {
                                                                Text(
                                                                    text = it?.title ?: defaultTitle,
                                                                    fontSize = 19.5.sp,
                                                                    maxLines = 1,
                                                                    overflow = TextOverflow.Ellipsis,
                                                                    fontWeight = FontWeight.SemiBold,
                                                                    modifier = Modifier.weight(1f, fill = false)
                                                                )
                                                                val isExplicitTitle = remember(it) {
                                                                    it?.title?.contains("(Explicit", ignoreCase = true) == true ||
                                                                        it?.compilation == "explicit"
                                                                }
                                                                if (isExplicitTitle) {
                                                                    Spacer(Modifier.width(6.dp))
                                                                    Box(
                                                                        modifier = Modifier
                                                                            .size(16.dp)
                                                                            .background(
                                                                                Color.White.copy(alpha = 0.28f),
                                                                                RoundedCornerShape(3.5.dp)
                                                                            ),
                                                                        contentAlignment = Alignment.Center
                                                                    ) {
                                                                        Text(
                                                                            text = "E",
                                                                            color = Color.Black.copy(alpha = 0.85f),
                                                                            fontSize = 9.5.sp,
                                                                            fontWeight = FontWeight.ExtraBold,
                                                                            lineHeight = 10.sp
                                                                        )
                                                                    }
                                                                }
                                                            }
                                                            val subtitleText = remember(it) {
                                                                val artistStr = it?.artistsName ?: defaultArtistsName
                                                                val albumStr = it?.album?.takeIf { alb -> alb.isNotBlank() && alb != artistStr }
                                                                if (albumStr != null) "$artistStr — $albumStr" else artistStr
                                                            }
                                                            Text(
                                                                text = subtitleText,
                                                                fontSize = 17.sp,
                                                                modifier = Modifier
                                                                    .overlayEffect()
                                                                    .clickable(
                                                                        indication = null,
                                                                        interactionSource = remember { MutableInteractionSource() }
                                                                    ) {
                                                                        Vibrator.click(context)
                                                                        subtitleMenuExpanded.value = true
                                                                    },
                                                                maxLines = 1,
                                                                overflow = TextOverflow.Ellipsis,
                                                                color = Color.White.copy(alpha = 0.55f)
                                                            )
                                                        }

                                                        DropdownMenu(
                                                            expanded = subtitleMenuExpanded.value,
                                                            onDismissRequest = { subtitleMenuExpanded.value = false },
                                                            modifier = Modifier.background(Color(0xEE1C1C22))
                                                        ) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        text = "Go to Artist (${it?.artistsName ?: defaultArtistsName})",
                                                                        color = Color.White,
                                                                        fontSize = 14.sp,
                                                                        fontWeight = FontWeight.Medium
                                                                    )
                                                                },
                                                                leadingIcon = {
                                                                    Icon(
                                                                        painter = painterResource(id = R.drawable.ic_library_link_icon_artists),
                                                                        contentDescription = null,
                                                                        tint = Color(0xFFFA233B),
                                                                        modifier = Modifier.size(20.dp)
                                                                    )
                                                                },
                                                                onClick = {
                                                                    subtitleMenuExpanded.value = false
                                                                    val song = it ?: return@DropdownMenuItem
                                                                    val artistName = song.artistsName ?: return@DropdownMenuItem
                                                                    val isOnline = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                                                                    navScope.launch { onMinimizeNowPlaying() }
                                                                    if (isOnline) {
                                                                        LibraryObject.openOnlineArtist(
                                                                            artistId = song.artistId?.toString() ?: artistName,
                                                                            name = artistName,
                                                                            artworkUrl = song.thumb?.toString().orEmpty()
                                                                        )
                                                                        navController.toUI(UI.OnlineArtistInfo)
                                                                    } else {
                                                                        LibraryObject.setTargetArtistName(artistName)
                                                                        LibraryObject.setArtistSongsSearchOnOpen(false)
                                                                        navController.markNextNavigationFromNowPlaying()
                                                                        navController.toUI(UI.ArtistInfo)
                                                                    }
                                                                }
                                                            )
                                                            if (!it?.album.isNullOrBlank()) {
                                                                DropdownMenuItem(
                                                                    text = {
                                                                        Text(
                                                                            text = "Go to Album (${it?.album.orEmpty()})",
                                                                            color = Color.White,
                                                                            fontSize = 14.sp,
                                                                            fontWeight = FontWeight.Medium
                                                                        )
                                                                    },
                                                                    leadingIcon = {
                                                                        Icon(
                                                                            painter = painterResource(id = R.drawable.ic_library_link_icon_album),
                                                                            contentDescription = null,
                                                                            tint = Color(0xFFFA233B),
                                                                            modifier = Modifier.size(20.dp)
                                                                        )
                                                                    },
                                                                    onClick = {
                                                                        subtitleMenuExpanded.value = false
                                                                        val song = it ?: return@DropdownMenuItem
                                                                        val albumName = song.album ?: return@DropdownMenuItem
                                                                        val isOnline = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                                                                        navScope.launch { onMinimizeNowPlaying() }
                                                                        if (isOnline && (song.albumId ?: 0L) > 0L) {
                                                                            LibraryObject.openOnlineAlbum(
                                                                                albumId = song.albumId.toString(),
                                                                                title = albumName,
                                                                                artist = song.artists.orEmpty(),
                                                                                artworkUrl = song.thumb?.toString().orEmpty()
                                                                            )
                                                                            navController.toUI(UI.OnlineAlbumInfo)
                                                                        } else {
                                                                            LibraryObject.setTargetAlbumName(albumName)
                                                                            navController.markNextNavigationFromNowPlaying()
                                                                            navController.toUI(UI.AlbumInfo)
                                                                        }
                                                                    }
                                                                )
                                                            }
                                                        }
                                                    }

                                                    LyricsflowWrapper {
                                                        ActionButtonsRow(navController, onMinimizeNowPlaying) {
                                                            it
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                            Lyric ->
                                Column(Modifier.fillMaxSize()) {
                                    LyricsflowWrapper {
                                        val isVisible = nowPageLambda() == Lyric
                                        PlayingBar(
                                            modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = ShareAlbumKey
                                                ),
                                                visible = isVisible
                                            ),
                                            navController = navController,
                                            onMinimizeNowPlaying = onMinimizeNowPlaying,
                                            albumUrlLambda = {
                                                thisMusicPlaying.value?.thumb
                                            },
                                            musicPlayingLambda = { thisMusicPlaying.value }) {
                                            nowPageOnChanged(Album)
                                        }
                                    }
                                }

                            PlayingList ->
                                LyricsflowWrapper {
                                    Column(
                                        Modifier
                                            .fillMaxSize()
                                            .clickable(enabled = false, onClick = {})
                                    ) {
                                        val isVisible = nowPageLambda() == PlayingList
                                        PlayingBar(
                                            modifier = Modifier.sharedElementWithCallerManagedVisibility(
                                                sharedContentState = rememberSharedContentState(
                                                    key = ShareAlbumKey
                                                ),
                                                visible = isVisible
                                            ),
                                            navController = navController,
                                            onMinimizeNowPlaying = onMinimizeNowPlaying,
                                            albumUrlLambda = {
                                                thisMusicPlaying.value?.thumb
                                            },
                                            musicPlayingLambda = { thisMusicPlaying.value }) {
                                            nowPageOnChanged(Album)
                                        }
                                    }
                                }
                        }
                    }
                }
            }

            LyricsflowWrapper {
                AnimatedVisibility(
                    visible = nowPageLambda() == PlayingList,
                    enter = fadeIn(tween(AnimDurationMillis)),
                    exit = fadeOut(tween(AnimDurationMillis)),
                    modifier = Modifier
                        .fillMaxSize()
                        .statusBarsPadding()
                        .padding(top = 114.dp)
                ) {
                    Column(
                        Modifier
                            .fillMaxSize()
                            .clickable(enabled = false, onClick = {})
                    ) {
                        PlayingList(
                            shuffleModeEnabledLambda = { shuffleModeEnabled.value },
                            shuffleModeOnChanged = { shuffleModeSet ->
                                shuffleModeEnabled.value = shuffleModeSet
                            },
                            repeatModeLambda = { repeatMode.intValue },
                            repeatModeOnChanged = { repeatModeSet ->
                                repeatMode.intValue = repeatModeSet
                            },
                            thisMusicPlayingLambda = { thisMusicPlaying.value }
                        )
                    }
                }
            }

            // 音乐控制
            LyricsflowWrapper {
                Column(
                    Modifier
                        .fillMaxSize()
                        .statusBarsPadding(), verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            /*.fillMaxHeight(0.385f)*/
                            .fillMaxHeight(0.437f)
                            .fillMaxWidth()
                    ) {
                        LyricsflowWrapper {
                            if (showControl.value) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(top = 40.dp)
                                        .clickable(
                                            interactionSource = remember { MutableInteractionSource() },
                                            indication = null,
                                            onClick = {
                                                //showControl.value = true
                                                /*lastClickTime.longValue =
                                                TimeUtils.getNowMills()*/
                                            })
                                )
                            }
                        }

                        LyricsflowWrapper {
                            Column(
                                Modifier.fillMaxSize(),
                                verticalArrangement = Arrangement.Bottom
                            ) {
                                AnimatedVisibility(
                                    visible = showControl.value,
                                    enter = fadeIn() + expandVertically(
                                        expandFrom = Alignment.Top,
                                        initialHeight = { (it / 1.4).toInt() }),
                                    exit = fadeOut() + shrinkVertically(
                                        shrinkTowards = Alignment.Top,
                                        targetHeight = { (it / 1.4).toInt() })
                                ) {
                                    LyricsflowWrapper {
                                        val showTranslationDropdown = remember { mutableStateOf(false) }
                                        val showSingSlider = remember { mutableStateOf(false) }
                                        val singEnabledState = VocalAttenuationAudioProcessor.singEnabled
                                        val singLevelState = VocalAttenuationAudioProcessor.attenuationLevel

                                        Row(
                                            Modifier
                                                .fillMaxWidth()
                                                .padding(horizontal = 30.dp)
                                                .graphicsLayer {
                                                    compositingStrategy =
                                                        CompositingStrategy.ModulateAlpha
                                                    this.alpha = alphaAnim.value
                                                },
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.Bottom
                                        ) {
                                            // Left circular Translation & Pronunciation Dropdown Button
                                            LyricsflowWrapper {
                                                Box {
                                                    val anySubActive = translation.value &&
                                                        (SettingsLibrary.LyricShowTranslation || SettingsLibrary.LyricShowPronunciation)
                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .liquidGlassCircle(
                                                                surfaceTint = if (anySubActive) Color.White.copy(alpha = 0.26f)
                                                                else Color.White.copy(alpha = 0.12f)
                                                            )
                                                            .clickable(
                                                                enabled = showControl.value && alphaAnim.value != 0f,
                                                                onClick = {
                                                                    Vibrator.click(context)
                                                                    showTranslationDropdown.value = true
                                                                    showControl.value = true
                                                                    lastClickTime.longValue = TimeUtils.getNowMills()
                                                                },
                                                                indication = null,
                                                                interactionSource = remember { MutableInteractionSource() }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painterResource(
                                                                id = if (anySubActive) R.drawable.ic_nowplaying_translateon
                                                                else R.drawable.ic_nowplaying_translate
                                                            ),
                                                            contentDescription = "Translation & Pronunciation",
                                                            tint = Color.Unspecified,
                                                            modifier = Modifier.size(24.dp)
                                                        )
                                                    }

                                                    DropdownMenu(
                                                        expanded = showTranslationDropdown.value,
                                                        onDismissRequest = { showTranslationDropdown.value = false },
                                                        modifier = Modifier
                                                            .width(265.dp)
                                                            .background(Color(0xF21B1B20))
                                                    ) {
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    "Show Translation",
                                                                    color = Color.White,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            },
                                                            trailingIcon = {
                                                                Switch(
                                                                    checked = translation.value && SettingsLibrary.LyricShowTranslation,
                                                                    onCheckedChange = { checked ->
                                                                        Vibrator.click(context)
                                                                        SettingsLibrary.LyricShowTranslation = checked
                                                                        if (checked) {
                                                                            translation.value = true
                                                                            SettingsLibrary.NowPlayingTranslation = true
                                                                        }
                                                                    },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFFFA233B)
                                                                    )
                                                                )
                                                            },
                                                            onClick = {
                                                                val next = !(translation.value && SettingsLibrary.LyricShowTranslation)
                                                                SettingsLibrary.LyricShowTranslation = next
                                                                if (next) {
                                                                    translation.value = true
                                                                    SettingsLibrary.NowPlayingTranslation = true
                                                                }
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    "Show Pronunciation",
                                                                    color = Color.White,
                                                                    fontSize = 14.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            },
                                                            trailingIcon = {
                                                                Switch(
                                                                    checked = SettingsLibrary.LyricShowPronunciation,
                                                                    onCheckedChange = { checked ->
                                                                        Vibrator.click(context)
                                                                        SettingsLibrary.LyricShowPronunciation = checked
                                                                    },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFFFA233B)
                                                                    )
                                                                )
                                                            },
                                                            onClick = {
                                                                SettingsLibrary.LyricShowPronunciation = !SettingsLibrary.LyricShowPronunciation
                                                            }
                                                        )
                                                        DropdownMenuItem(
                                                            text = {
                                                                Text(
                                                                    "Replace Original with Pronunciation",
                                                                    color = Color.White,
                                                                    fontSize = 13.5.sp,
                                                                    fontWeight = FontWeight.Medium
                                                                )
                                                            },
                                                            trailingIcon = {
                                                                Switch(
                                                                    checked = SettingsLibrary.LyricSwapOriginalWithPronunciation,
                                                                    onCheckedChange = { checked ->
                                                                        Vibrator.click(context)
                                                                        SettingsLibrary.LyricSwapOriginalWithPronunciation = checked
                                                                    },
                                                                    colors = SwitchDefaults.colors(
                                                                        checkedThumbColor = Color.White,
                                                                        checkedTrackColor = Color(0xFFFA233B)
                                                                    )
                                                                )
                                                            },
                                                            onClick = {
                                                                SettingsLibrary.LyricSwapOriginalWithPronunciation =
                                                                    !SettingsLibrary.LyricSwapOriginalWithPronunciation
                                                            }
                                                        )
                                                        if (SettingsLibrary.LyricSwapOriginalWithPronunciation) {
                                                            DropdownMenuItem(
                                                                text = {
                                                                    Text(
                                                                        "Hide Original in Pronunciation Bar",
                                                                        color = Color.White.copy(alpha = 0.85f),
                                                                        fontSize = 13.sp,
                                                                        fontWeight = FontWeight.Normal
                                                                    )
                                                                },
                                                                trailingIcon = {
                                                                    Switch(
                                                                        checked = SettingsLibrary.LyricHideOriginalInPronunciationBar,
                                                                        onCheckedChange = { checked ->
                                                                            Vibrator.click(context)
                                                                            SettingsLibrary.LyricHideOriginalInPronunciationBar = checked
                                                                        },
                                                                        colors = SwitchDefaults.colors(
                                                                            checkedThumbColor = Color.White,
                                                                            checkedTrackColor = Color(0xFFFA233B)
                                                                        )
                                                                    )
                                                                },
                                                                onClick = {
                                                                    SettingsLibrary.LyricHideOriginalInPronunciationBar =
                                                                        !SettingsLibrary.LyricHideOriginalInPronunciationBar
                                                                }
                                                            )
                                                        }
                                                    }
                                                }
                                            }

                                            // Right circular Apple Music Sing Button & Expandable Vertical Vocal Attenuation Pill
                                            LyricsflowWrapper {
                                                Column(
                                                    horizontalAlignment = Alignment.CenterHorizontally,
                                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                                ) {
                                                    AnimatedVisibility(
                                                        visible = showSingSlider.value && singEnabledState.value,
                                                        enter = fadeIn() + expandVertically(expandFrom = Alignment.Bottom),
                                                        exit = fadeOut() + shrinkVertically(shrinkTowards = Alignment.Bottom)
                                                    ) {
                                                        Box(
                                                            modifier = Modifier
                                                                .width(42.dp)
                                                                .height(132.dp)
                                                                .liquidGlass(
                                                                    shape = RoundedCornerShape(21.dp),
                                                                    surfaceTint = Color.White.copy(alpha = 0.18f)
                                                                )
                                                                .draggable(
                                                                    orientation = Orientation.Vertical,
                                                                    state = rememberDraggableState { deltaPx ->
                                                                        val nextLevel = (singLevelState.floatValue - (deltaPx / 240f)).coerceIn(0.05f, 1f)
                                                                        VocalAttenuationAudioProcessor.updateSingState(true, nextLevel)
                                                                        showControl.value = true
                                                                        lastClickTime.longValue = TimeUtils.getNowMills()
                                                                    }
                                                                )
                                                                .padding(6.dp),
                                                            contentAlignment = Alignment.BottomCenter
                                                        ) {
                                                            val fillFraction = singLevelState.floatValue.coerceIn(0.08f, 1f)
                                                            Box(
                                                                modifier = Modifier
                                                                    .fillMaxWidth()
                                                                    .fillMaxHeight(fillFraction)
                                                                    .background(
                                                                        Color.White.copy(alpha = 0.88f),
                                                                        RoundedCornerShape(16.dp)
                                                                    )
                                                            )
                                                            Icon(
                                                                painter = painterResource(id = R.drawable.ic_vocal_sing),
                                                                contentDescription = null,
                                                                tint = if (fillFraction > 0.25f) Color.Black else Color.White,
                                                                modifier = Modifier
                                                                    .padding(bottom = 8.dp)
                                                                    .size(18.dp)
                                                            )
                                                        }
                                                    }

                                                    Box(
                                                        modifier = Modifier
                                                            .size(40.dp)
                                                            .liquidGlassCircle(
                                                                surfaceTint = if (singEnabledState.value) Color.White.copy(alpha = 0.88f)
                                                                else Color.White.copy(alpha = 0.14f)
                                                            )
                                                            .clickable(
                                                                enabled = showControl.value && alphaAnim.value != 0f,
                                                                onClick = {
                                                                    Vibrator.click(context)
                                                                    if (!singEnabledState.value) {
                                                                        VocalAttenuationAudioProcessor.updateSingState(true, singLevelState.floatValue)
                                                                        showSingSlider.value = true
                                                                    } else if (!showSingSlider.value) {
                                                                        showSingSlider.value = true
                                                                    } else {
                                                                        VocalAttenuationAudioProcessor.updateSingState(false, singLevelState.floatValue)
                                                                        showSingSlider.value = false
                                                                    }
                                                                    showControl.value = true
                                                                    lastClickTime.longValue = TimeUtils.getNowMills()
                                                                },
                                                                indication = null,
                                                                interactionSource = remember { MutableInteractionSource() }
                                                            ),
                                                        contentAlignment = Alignment.Center
                                                    ) {
                                                        Icon(
                                                            painter = painterResource(id = R.drawable.ic_vocal_sing),
                                                            contentDescription = "Apple Music Sing",
                                                            tint = if (singEnabledState.value) Color.Black else Color.White,
                                                            modifier = Modifier.size(21.dp)
                                                        )
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    PlayerControl(
                                        isPlayingLambda = isPlayingStatusLambda,
                                        isPlayingOnChanged = isPlayingOnChanged,
                                        onPrevious = {
                                            mediaControl?.seekToPreviousMediaItem()
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onStatus = { status ->
                                            if (status) {
                                                mediaControl?.fadePlay()
                                            } else {
                                                mediaControl?.fadePause()
                                            }
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onNext = {
                                            mediaControl?.seekToNextMediaItem()
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        onSeek = { position ->
                                            mediaControl?.seekTo(position.toLong())
                                        },
                                        onLyrics = {
                                            if (nowPageLambda() == Lyric) {
                                                nowPageOnChanged(Album)
                                            } else {
                                                nowPageOnChanged(Lyric)
                                            }
                                        },
                                        onPlaylist = {
                                            if (nowPageLambda() == PlayingList) {
                                                nowPageOnChanged(Album)
                                            } else {
                                                nowPageOnChanged(PlayingList)
                                            }
                                        },
                                        nowPage = {
                                            nowPageLambda()
                                        },
                                        onSlider = {
                                            showControl.value = true
                                            lastClickTime.longValue = TimeUtils.getNowMills()
                                        },
                                        modifier = Modifier
                                            /*.graphicsLayer {
                                                compositingStrategy =
                                                    CompositingStrategy.Offscreen
                                                //this.alpha = controlAlpha.value
                                            }*/
                                            .padding(top = 52.dp),
                                        onWhile = {
                                            shuffleModeEnabled.value =
                                                queueShuffleEnabled.value
                                            repeatMode.intValue =
                                                mediaControl?.repeatMode ?: REPEAT_MODE_OFF
                                        })
                                }
                            }
                        }
                    }
                }
            }

        }
    }

@Composable
private fun ColumnScope.Album(
    modifier: Modifier,
    music: () -> LyricsflowMediaItem?,
    animatedAlbumCoverState: AnimatedAlbumCoverState,
    isPlaying: () -> Boolean
) = Box(
    Modifier
        .weight(1f)
        .padding(top = 20.dp)
        .padding(horizontal = 15.dp)
        .padding(bottom = 33.dp),
    contentAlignment = Alignment.BottomCenter
) {
    val springSpec: AnimationSpec<Float> = remember("Album_springSpec") {
        SpringSpec(stiffness = 300f, dampingRatio = 1f, visibilityThreshold = 0.001f)
    }

    val tweenSpec: AnimationSpec<Float> = remember("Album_tweenSpec") {
        TweenSpec(durationMillis = 350, easing = EaseOutQuart)
    }

    val scale = animateFloatAsState(
        targetValue = if (isPlaying()) 0f else 1f,
        animationSpec = if (isPlaying()) springSpec else tweenSpec,
        visibilityThreshold = 0.001f
    )

    val staticCoverAlpha = animateFloatAsState(
        targetValue = if (animatedAlbumCoverState.videoPrepared) 0f else 1f,
        animationSpec = tween(durationMillis = 360, easing = EaseOutQuart),
        label = "StaticCoverFadeOut"
    )

    LyricsflowWrapper {
        val dp = (7 + (27 * scale.value)).dp
        ShadowImageWithCache(
            dataLambda = { music()?.thumb }, contentDescription = null, modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                    alpha = staticCoverAlpha.value
                    // scaleX = scale.value
                    // scaleY = scale.value
                }
                .padding(start = dp, end = dp, bottom = dp)
                .then(modifier),
            imageQuality = ImageQuality.RAW,
            shadowOverlay = true
        )
    }
}


@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun PlayingList(
    shuffleModeEnabledLambda: () -> Boolean,
    shuffleModeOnChanged: (Boolean) -> Unit,
    repeatModeLambda: () -> Int,
    repeatModeOnChanged: (Int) -> Unit,
    thisMusicPlayingLambda: () -> LyricsflowMediaItem?
) {
    val context = LocalContext.current

    Spacer(modifier = Modifier.height(12.dp))

    val musicList = remember("PlayingList_musicList") {
        playingMusicList
    }
    val nextInQueueList = remember("PlayingList_nextInQueueList") {
        MediaController.nextInQueueMusicList
    }
    val scope = rememberCoroutineScope()

    LyricsflowWrapper {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.545f),
        ) {
            val hide = remember("PlayingList_hide") {
                derivedStateOf {
                    nextInQueueList.value.isEmpty() && musicList.value.isNullOrEmpty()
                }
            }

            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 30.dp)
                    .padding(top = 10.dp)
                    .height(65.dp), verticalAlignment = Alignment.CenterVertically
            ) {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .weight(1f)
                ) {
                    Text(
                        text = stringResource(id = R.string.page_library_playlists),
                        fontSize = 16.5.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier/*.padding(top = 10.dp)*/
                    )
                    Text(
                        text = stringResource(
                            id = R.string.page_library_playlists_music_total,
                            nextInQueueList.value.size + (musicList.value?.size ?: 0)
                        ),
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Medium,
                        modifier = Modifier
                            .padding(top = 2.dp)
                            .overlayEffect()
                            .alpha(0.35f)
                    )
                }

                Row(
                    modifier = Modifier
                        .overlayEffect()
                        .alpha(0.6f)
                ) {
                    val dp = 36.dp
                    LyricsflowWrapper {
                        val shuffleBackgroundAlpha =
                            animateFloatAsState(targetValue = if (shuffleModeEnabledLambda()) 0.9f else 0f)
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        shuffleModeOnChanged(!shuffleModeEnabledLambda())
                                        scope.launch(Dispatchers.IO) {
                                            MediaController.toggleShuffleMode()
                                        }
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() })
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = shuffleBackgroundAlpha.value),
                                    shape = LyricsflowRoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            LyricsflowWrapper {
                                val shuffleIconTint =
                                    animateColorAsState(targetValue = if (shuffleModeEnabledLambda()) Color.Black else Color.White)
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_shuffle),
                                    contentDescription = null,
                                    modifier = Modifier
                                        .size(dp),
                                    tint = shuffleIconTint.value
                                )
                            }
                        }
                    }
                    LyricsflowWrapper {
                        val repeatHighlight =
                            repeatModeLambda() == REPEAT_MODE_ALL || repeatModeLambda() == REPEAT_MODE_ONE
                        val repeatBackgroundAlpha =
                            animateFloatAsState(targetValue = if (repeatHighlight) 0.9f else 0f)
                        Box(
                            modifier = Modifier
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        val targetMode = when (repeatModeLambda()) {
                                            REPEAT_MODE_OFF -> {
                                                REPEAT_MODE_ALL
                                            }

                                            REPEAT_MODE_ALL -> {
                                                REPEAT_MODE_ONE
                                            }

                                            else -> {
                                                REPEAT_MODE_OFF
                                            }
                                        }
                                        mediaControl?.repeatMode = targetMode
                                        mediaControl?.let { LyricsflowPlaybackService().setCustomButtons(it) }
                                        repeatModeOnChanged(targetMode)
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() })
                                .padding(start = 10.dp)
                                .size(36.dp)
                                .background(
                                    Color.White.copy(alpha = repeatBackgroundAlpha.value),
                                    shape = LyricsflowRoundedCornerShape(10.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            LyricsflowWrapper {
                                AnimatedContent(targetState = repeatModeLambda(), transitionSpec = {
                                    fadeIn() togetherWith fadeOut()
                                }) {
                                    when (it) {
                                        REPEAT_MODE_ONE -> Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_repeatone),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(dp),
                                            tint = animateColorAsState(targetValue = if (repeatHighlight) Color.Black else Color.White).value
                                        )

                                        else -> Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_repeat),
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(dp),
                                            tint = animateColorAsState(targetValue = if (repeatHighlight) Color.Black else Color.White).value
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }


            if (hide.value) {
                Column(
                    Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_uitabbar_library),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(70.dp)
                            .alpha(0.6f)
                    )
                    Text(
                        text = stringResource(id = R.string.playlist_unavailable_title),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(top = 18.dp, bottom = 12.dp)
                    )
                    LyricsflowWrapper {
                        Text(
                            text = stringResource(id = R.string.playlist_unavailable_desc),
                            fontSize = 16.sp,
                            color = Color.White,
                            modifier = Modifier
                                .overlayEffect()
                                .alpha(0.4f)
                        )
                    }
                }
            } else {
                val state = rememberLazyListState(
                    initialFirstVisibleItemIndex = 0,
                    initialFirstVisibleItemScrollOffset = -15
                )
                val nextInQueue = nextInQueueList.value
                val upNext = musicList.value ?: emptyList()
                var draggingQueueItemKey by remember {
                    mutableStateOf<String?>(null)
                }
                val reorderableState = rememberReorderableLazyListState(state) { from, to ->
                    val source = resolveQueueReorderTarget(
                        from.index,
                        nextInQueue.size,
                        upNext.size,
                    ) ?: return@rememberReorderableLazyListState
                    val destination = resolveQueueReorderTarget(
                        to.index,
                        nextInQueue.size,
                        upNext.size,
                    ) ?: return@rememberReorderableLazyListState

                    if (source.nextInQueue != destination.nextInQueue || source.index == destination.index) {
                        return@rememberReorderableLazyListState
                    }

                    Vibrator.click(context)
                    if (source.nextInQueue) {
                        MediaController.moveNextInQueueItemDuringDrag(source.index, destination.index)
                    } else {
                        MediaController.moveUpNextItemDuringDrag(source.index, destination.index)
                    }
                }

                LyricsflowWrapper {
                    CompositionLocalProvider(LocalOverscrollConfiguration provides null) {

                        LazyColumn(state = state, modifier = Modifier
                            .fillMaxSize()
                            .graphicsLayer {
                                compositingStrategy = CompositingStrategy.Offscreen
                            }
                            .drawWithCache {
                                onDrawWithContent {
                                    val colors = listOf(
                                        Color.Transparent,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Black,
                                        Color.Transparent
                                    )

                                    drawContent()

                                    drawRect(
                                        brush = Brush.verticalGradient(colors),
                                        blendMode = BlendMode.DstIn
                                    )
                                }
                            }/*, contentPadding = PaddingValues(vertical = 12.dp)*/
                        ) {
                            item("blank_before") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                            if (nextInQueue.isNotEmpty()) {
                                item("next_in_queue_header") {
                                    QueueSectionHeader(
                                        title = stringResource(id = R.string.queue_next_in_queue),
                                        onClear = {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.clearNextInQueue()
                                            }
                                        },
                                    )
                                }

                                itemsIndexed(
                                    nextInQueue,
                                    key = { indexOfMusic, music -> queueItemKey("next", music, indexOfMusic, nextInQueue) }
                                ) { indexOfMusic, music ->
                                    val itemKey = queueItemKey("next", music, indexOfMusic, nextInQueue)

                                    ReorderableItem(reorderableState, key = itemKey, animateItemModifier = Modifier) { isDragging ->
                                        QueueMusicListItem(
                                            music = music,
                                            reorderEnabled = nextInQueue.size > 1,
                                            isDragging = isDragging || draggingQueueItemKey == itemKey,
                                            reorderHandleModifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    draggingQueueItemKey = itemKey
                                                    Vibrator.longClick(context)
                                                },
                                                onDragStopped = {
                                                    draggingQueueItemKey = null
                                                    Vibrator.click(context)
                                                    scope.launch(Dispatchers.IO) {
                                                        MediaController.saveQueueState()
                                                    }
                                                },
                                            ),
                                            onMoveToNextQueue = null,
                                            onRemove = {
                                                MediaController.removeNextInQueueItem(indexOfMusic)
                                            },
                                        ) {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.skipToNextInQueueItem(indexOfMusic)
                                            }
                                        }
                                    }
                                }
                            }

                            if (upNext.isNotEmpty()) {
                                item("up_next_header") {
                                    QueueSectionHeader(title = stringResource(id = R.string.queue_up_next))
                                }

                                itemsIndexed(
                                    upNext,
                                    key = { indexOfMusic, music -> queueItemKey("up", music, indexOfMusic, upNext) }
                                ) { indexOfMusic, music ->
                                    val itemKey = queueItemKey("up", music, indexOfMusic, upNext)

                                    ReorderableItem(reorderableState, key = itemKey, animateItemModifier = Modifier) { isDragging ->
                                        QueueMusicListItem(
                                            music = music,
                                            reorderEnabled = upNext.size > 1,
                                            isDragging = isDragging || draggingQueueItemKey == itemKey,
                                            reorderHandleModifier = Modifier.draggableHandle(
                                                onDragStarted = {
                                                    draggingQueueItemKey = itemKey
                                                    Vibrator.longClick(context)
                                                },
                                                onDragStopped = {
                                                    draggingQueueItemKey = null
                                                    Vibrator.click(context)
                                                    scope.launch(Dispatchers.IO) {
                                                        MediaController.saveQueueState()
                                                    }
                                                },
                                            ),
                                            onMoveToNextQueue = {
                                                MediaController.moveUpNextToNextQueue(indexOfMusic)
                                            },
                                            onRemove = {
                                                MediaController.removeUpNextItem(indexOfMusic)
                                            },
                                        ) {
                                            scope.launch(Dispatchers.IO) {
                                                MediaController.skipToUpNextItem(indexOfMusic)
                                            }
                                        }
                                    }
                                }
                            }

                            item("blank_after") {
                                Spacer(modifier = Modifier.height(12.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun QueueSectionHeader(
    title: String,
    onClear: (() -> Unit)? = null,
) {
    val context = LocalContext.current

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 30.dp)
            .padding(top = 12.dp, bottom = 6.dp)
            .overlayEffect(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(
            text = title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White,
            modifier = Modifier.weight(1f),
        )

        if (onClear != null) {
            Text(
                text = stringResource(id = R.string.queue_clear),
                fontSize = 16.sp,
                fontWeight = FontWeight.Medium,
                color = Color.White.copy(alpha = 0.64f),
                modifier = Modifier.clickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = null,
                ) {
                    Vibrator.click(context)
                    onClear()
                },
            )
        }
    }
}

@Composable
private fun QueueMusicListItem(
    music: LyricsflowMediaItem,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    reorderHandleModifier: Modifier,
    onMoveToNextQueue: (suspend () -> Boolean)?,
    onRemove: (suspend () -> Boolean)?,
    itemClick: () -> Unit,
) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val coroutineScope = rememberCoroutineScope()
    val addedToQueueToast = stringResource(id = R.string.queue_added_toast)
    var rowWidthPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var rowHeightPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var swipeOffsetPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var deleteHeightPx by remember(music.uri, music.mediaId) {
        mutableFloatStateOf(0f)
    }
    var resetAnimationJob by remember(music.uri, music.mediaId) {
        mutableStateOf<Job?>(null)
    }
    var deleteAnimating by remember(music.uri, music.mediaId) {
        mutableStateOf(false)
    }
    var deleteCollapsing by remember(music.uri, music.mediaId) {
        mutableStateOf(false)
    }

    val swipeRightEnabled = onMoveToNextQueue != null
    val swipeLeftEnabled = onRemove != null
    val triggerOffsetPx = rowWidthPx * 0.20f
    val minSwipeOffsetPx = if (swipeLeftEnabled) -rowWidthPx else 0f
    val maxSwipeOffsetPx = if (swipeRightEnabled) rowWidthPx else 0f
    val absoluteSwipeOffsetPx = if (swipeOffsetPx < 0f) -swipeOffsetPx else swipeOffsetPx
    val swipeProgress = if (rowWidthPx > 0f) {
        (absoluteSwipeOffsetPx / rowWidthPx).coerceIn(0f, 1f)
    } else {
        0f
    }
    val rowHeight = with(density) {
        rowHeightPx.toDp()
    }
    val swipeRevealWidth = with(density) {
        absoluteSwipeOffsetPx.toDp()
    }
    val deleteHeight = with(density) {
        deleteHeightPx.coerceAtLeast(0f).toDp()
    }

    val swipeModifier = if ((swipeRightEnabled || swipeLeftEnabled) && !deleteAnimating) {
        Modifier.draggable(
            orientation = Orientation.Horizontal,
            state = rememberDraggableState { delta ->
                if (rowWidthPx <= 0f || deleteAnimating) {
                    return@rememberDraggableState
                }

                resetAnimationJob?.cancel()

                val wasPastAddThreshold = swipeOffsetPx >= triggerOffsetPx
                val wasPastDeleteThreshold = swipeOffsetPx <= -triggerOffsetPx
                swipeOffsetPx = (swipeOffsetPx + delta).coerceIn(minSwipeOffsetPx, maxSwipeOffsetPx)
                val isPastAddThreshold = swipeOffsetPx >= triggerOffsetPx
                val isPastDeleteThreshold = swipeOffsetPx <= -triggerOffsetPx

                if (wasPastAddThreshold != isPastAddThreshold || wasPastDeleteThreshold != isPastDeleteThreshold) {
                    if (isPastAddThreshold || isPastDeleteThreshold) {
                        Vibrator.longClick(context)
                    } else {
                        Vibrator.click(context)
                    }
                }
            },
            onDragStopped = {
                val shouldMoveToNextQueue = triggerOffsetPx > 0f && swipeOffsetPx >= triggerOffsetPx
                val shouldRemove = triggerOffsetPx > 0f && swipeOffsetPx <= -triggerOffsetPx

                resetAnimationJob?.cancel()
                resetAnimationJob = coroutineScope.launch {
                    if (shouldRemove && onRemove != null) {
                        deleteAnimating = true
                        deleteHeightPx = if (rowHeightPx > 0f) {
                            rowHeightPx
                        } else {
                            with(density) { QueueRowHeight.toPx() }
                        }

                        animate(
                            initialValue = swipeOffsetPx,
                            targetValue = -rowWidthPx,
                            animationSpec = tween(
                                durationMillis = 110,
                                easing = EaseOutQuart,
                            ),
                        ) { value, _ ->
                            swipeOffsetPx = value
                        }

                        swipeOffsetPx = 0f
                        deleteCollapsing = true

                        animate(
                            initialValue = deleteHeightPx,
                            targetValue = 0f,
                            animationSpec = tween(
                                durationMillis = 170,
                                easing = EaseOutQuart,
                            ),
                        ) { value, _ ->
                            deleteHeightPx = value
                        }

                        onRemove.invoke()
                        return@launch
                    }

                    if (shouldMoveToNextQueue && onMoveToNextQueue?.invoke() == true) {
                        Toast.makeText(context, addedToQueueToast, Toast.LENGTH_SHORT).show()
                    }

                    val animationStart = swipeOffsetPx

                    animate(
                        initialValue = animationStart,
                        targetValue = 0f,
                        animationSpec = SpringSpec(
                            dampingRatio = 0.72f,
                            stiffness = 420f,
                            visibilityThreshold = 0.5f,
                        ),
                    ) { value, _ ->
                        swipeOffsetPx = value
                    }

                }
            },
        )
    } else {
        Modifier
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .zIndex(if (isDragging || deleteAnimating) 1f else 0f)
            .then(if (deleteCollapsing) Modifier.height(deleteHeight) else Modifier)
            .onSizeChanged {
                rowWidthPx = it.width.toFloat()
                if (!deleteCollapsing) {
                    rowHeightPx = it.height.toFloat()
                }
            }
    ) {
        if (deleteCollapsing) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(deleteHeight)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_delete),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(28.dp),
                )
            }
        } else if (swipeRightEnabled && swipeOffsetPx > 0f) {
            Box(
                modifier = Modifier
                    .width(swipeRevealWidth)
                    .height(rowHeight)
                    .background(MaterialTheme.colorScheme.primary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_queue),
                    contentDescription = null,
                    tint = Color.Black,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            val iconScale = 0.82f + (swipeProgress * 0.18f)
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }

        if (!deleteCollapsing && swipeLeftEnabled && swipeOffsetPx < 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.CenterEnd)
                    .width(swipeRevealWidth)
                    .height(rowHeight)
                    .background(Color(0xFFD32F2F)),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_swipe_delete),
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier
                        .size(28.dp)
                        .graphicsLayer {
                            val iconScale = 0.82f + (swipeProgress * 0.18f)
                            scaleX = iconScale
                            scaleY = iconScale
                        },
                )
            }
        }

        if (!deleteCollapsing) {
            SmallMusicListItem(
                music = music,
                reorderEnabled = reorderEnabled,
                isDragging = isDragging,
                modifier = Modifier
                    .graphicsLayer {
                        translationX = swipeOffsetPx
                    }
                    .then(swipeModifier),
                reorderHandleModifier = reorderHandleModifier,
                itemClick = itemClick,
            )
        }
    }
}

@Composable
private fun SmallMusicListItem(
    music: LyricsflowMediaItem,
    reorderEnabled: Boolean,
    isDragging: Boolean,
    modifier: Modifier = Modifier,
    reorderHandleModifier: Modifier,
    itemClick: () -> Unit,
) {
    val draggedItemBackground by animateColorAsState(
        targetValue = if (isDragging) { Color.White.copy(alpha = 0.08f) } else { Color.Transparent },
        label = "QueueDraggedItemBackground",
    )
    Surface(
        modifier = modifier
            .height(QueueRowHeight)
            .fillMaxWidth(),
        color = draggedItemBackground,
        contentColor = Color.White,
        shape = QueueDraggingItemShape,
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .clickable {
                    itemClick()
                }
                .padding(horizontal = 30.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            println("重组：播放界面歌曲列表 ${music.title}")
            ShadowImageWithCache(
                dataLambda = { music.thumb },
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                cornerRadius = 4.dp,
                shadowAlpha = 0f,
                imageQuality = ImageQuality.LOW
            )

            Column(
                Modifier
                    .weight(1f)
                    .padding(start = 14.dp, end = 12.dp)
            ) {
                Text(
                    text = music.title ?: defaultTitle,
                    modifier = Modifier.padding(bottom = 1.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 16.sp,
                    lineHeight = 16.sp,
                )

                Text(
                    text = music.artistsName ?: defaultArtistsName,
                    modifier = Modifier.alpha(0.5f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 11.5.sp,
                    lineHeight = 11.5.sp,
                )
            }

            if (reorderEnabled) {
                Box(
                    modifier = Modifier
                        .size(42.dp)
                        .then(reorderHandleModifier),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        painter = painterResource(id = R.drawable.ic_queue_reorder),
                        contentDescription = null,
                        tint = Color.White.copy(alpha = 0.34f),
                        modifier = Modifier.size(24.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun Lyric(
    lrcEntries: () -> List<List<Pair<Float, String>>>,
    lineEndTimes: () -> List<Float>,
    lineTransliterations: () -> List<String?>,
    lineSubtitles: () -> List<String?>,
    isTtmlLyrics: () -> Boolean,
    weightLambda: () -> Boolean,
    translationLambda: () -> Boolean,
    mainViewModel: MainViewModel,
    mediaViewModel: MediaViewModel,
    onBackClick: () -> Unit
) = LyricsflowWrapper {

    val context = LocalContext.current

    Column(
        Modifier
            .fillMaxSize()
    ) {
        LyricsflowWrapper {
            Spacer(modifier = Modifier.statusBarsHeight(110.dp))

            LyricsflowLyricView(
                //mediaViewModel = mediaViewModel,
                lrcEntriesLambda = lrcEntries,
                lineEndTimesLambda = lineEndTimes,
                lineTransliterationsLambda = lineTransliterations,
                lineSubtitlesLambda = lineSubtitles,
                isTtmlLyricsLambda = isTtmlLyrics,
                liveTimeLambda = {
                    (mediaControl?.currentPosition ?: 0).toInt()
                },
                mediaEvent = object : LyricsflowMediaEvent {
                    override fun onSeek(position: Int) {
                        mediaControl?.seekTo(position.toLong())
                    }
                },
                translationLambda = translationLambda,
                blurLambda = {
                    SettingsLibrary.LyricBlurEffect
                },
                uiConfig = LyricsflowUIConfig(
                    noLrcText = stringResource(id = R.string.tip_no_lyrics)
                ),
                weightLambda = weightLambda,
                modifier = Modifier.drawWithCache {
                    onDrawWithContent {
                        val overlayPaint = Paint().apply {
                            blendMode = BlendMode.Plus
                        }
                        val rect = Rect(0f, 0f, size.width, size.height)
                        val canvas = this.drawContext.canvas

                        canvas.saveLayer(rect, overlayPaint)

                        val colors = if (weightLambda()) {
                            listOf(
                                Color.Transparent,
                                Color(0x59000000),
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color(0x59000000),
                                Color(0x21000000),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent,
                                Color.Transparent
                            )
                        } else {
                            listOf(
                                Color.Transparent,
                                Color(0x59000000),
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                /*Color(0xD9000000),
                                Color(0xA6000000),
                                Color(0x73000000),
                                Color(0x59000000),
                                Color(0x3F000000),
                                Color(0x21000000),
                                Color(0x0C000000),*/
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black,
                                Color.Black
                            )
                        }

                        drawContent()

                        drawRect(
                            brush = Brush.verticalGradient(colors),
                            blendMode = BlendMode.DstIn
                        )

                        canvas.restore()
                    }
                },
                onBackClick = onBackClick
            )
        }
    }
}

@Composable
private fun ActionButtonsRow(
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    musicPlayingLambda: () -> LyricsflowMediaItem?,
) {
    Row(
        modifier = Modifier
            .overlayEffect(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val dp = 28.dp

        val context = LocalContext.current

        // Overflow menu state. The sheet renders only when this is true; the icon
        // swaps to its filled variant while open (PRD §5.1 FR-OM-2).
        val overflowSheetOpen = remember { mutableStateOf(false) }
        // Snapshot of the song that was playing when the sheet was opened
        // (PRD §5.3 FR-OM-8). All actions in the sheet refer to this item.
        val snapshotSong = remember { mutableStateOf<LyricsflowMediaItem?>(null) }
        val actionButtonScope = rememberCoroutineScope()

        NowPlayingOverflowSheet(
            isOpen = overflowSheetOpen,
            song = snapshotSong.value,
            navController = navController,
            onOpenLibraryTarget = {
                actionButtonScope.launch {
                    onMinimizeNowPlaying()
                }
                navController.toUI(it.route)
            },
        )

        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        //println("收藏 开始")
                        val musicPlaying = musicPlayingLambda()
                        //println("收藏 $musicPlaying")
                        if (musicPlaying != null) {
                            Vibrator.click(context)
                            //println("收藏 切换状态
                            if (musicPlaying.let { FavPlayListLibrary.isFavorite(it) }) {
                                FavPlayListLibrary.removeMusic(musicPlaying)
                            } else {
                                FavPlayListLibrary.addMusic(musicPlaying)
                            }
                            //println("收藏 完毕")
                        }
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                )
                .size(dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = musicPlayingLambda()?.let { FavPlayListLibrary.isFavorite(it) }
                    ?: false,
                transitionSpec = {
                    fadeIn() togetherWith fadeOut()
                }) {
                if (it) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_favorited),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dp)
                    )
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_favorite),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(dp)
                    )
                }
            }
        }

        Spacer(modifier = Modifier.width(14.dp))

        // No graphicsLayer rotation here: the underlying drawable
        // (ic_nowplaying_more / _fill) is drawn natively as a horizontal
        // three-dot ellipsis. A previous version applied a 90deg
        // graphicsLayer rotation to a vertical drawable; the rotation
        // changed the visible orientation but not the layout hitbox,
        // shifting the tap target to the right of the icon. Both the
        // drawable and hitbox are now horizontal and aligned.
        Box(
            modifier = Modifier
                .clickable(
                    onClick = {
                        // Snapshot the currently-playing song at open time
                        // (PRD FR-OM-8) and surface the sheet. The sheet wrapper
                        // handles its own haptic on open.
                        snapshotSong.value = musicPlayingLambda()
                        overflowSheetOpen.value = true
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() })
                .size(dp),
            contentAlignment = Alignment.Center
        ) {
            AnimatedContent(
                targetState = overflowSheetOpen.value,
                // Match the Material3 ModalBottomSheet close animation
                // (~300ms) so the icon de-highlights exactly as the sheet
                // finishes sliding away. The default fadeIn/fadeOut uses a
                // spring spec that's noticeably slower than the sheet,
                // which leaves the icon visibly highlighted after the
                // sheet has already closed.
                transitionSpec = {
                    fadeIn(animationSpec = tween(durationMillis = 300)) togetherWith
                        fadeOut(animationSpec = tween(durationMillis = 300))
                }) {
                if (it) {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_more_fill),
                        contentDescription = null,
                        modifier = Modifier
                            .size(dp)
                    )
                } else {
                    Icon(
                        painterResource(id = R.drawable.ic_nowplaying_more),
                        contentDescription = null,
                        modifier = Modifier
                            .overlayEffect()
                            .size(dp)
                    )
                }
            }
        }
    }
}

/**
 * Now Playing overflow menu. Opened from the three-dots icon in [ActionButtonsRow].
 *
 * Per PRD §5.3 FR-OM-8 this composable receives a *snapshot* of the song that
 * was playing when the sheet was opened — it does not re-subscribe to
 * [MediaController.musicPlaying] for the header, so subsequent track changes
 * while the sheet is open do not mutate the displayed song.
 *
 * Layout uses a single bottom sheet that swaps its body between three
 * internal screens (Menu / Playlist picker / Sleep timer). Selecting a row
 * doesn't dismiss + re-open a sub-sheet — the same sheet stays mounted and
 * its content changes in place, avoiding the visible close + reopen
 * animation that two separate sheets would cause.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun NowPlayingOverflowSheet(
    isOpen: MutableState<Boolean>,
    song: LyricsflowMediaItem?,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
) {
    if (!isOpen.value) return

    var screen by remember { mutableStateOf(OverflowScreen.Menu) }
    val navigationDirection = remember {
        mutableIntStateOf(SheetNavigationForward)
    }

    // Skip the partially-expanded state so the sheet always settles at the
    // height of its current content. The body swaps between screens of
    // very different heights (Menu ~3 rows, SleepTimer ~9 rows); with the
    // default partial state the sheet stays at the previously-settled
    // height and clips the taller content's bottom corners off-screen.
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    val onDismiss: () -> Unit = {
        isOpen.value = false
        screen = OverflowScreen.Menu
        navigationDirection.intValue = SheetNavigationForward
    }

    LyricsflowBottomSheetDialog(
        bottomSheetState = sheetState,
        blurred = true,
        onDismissRequest = onDismiss,
    ) {
        SheetAnimatedContent(
            targetState = screen,
            navigationDirection = navigationDirection.intValue,
            modifier = Modifier.fillMaxWidth(),
            label = "NowPlayingOverflowSheet",
        ) { currentScreen ->
            when (currentScreen) {
                OverflowScreen.Menu -> OverflowMenuBody(
                    song = song,
                    navController = navController,
                    onOpenLibraryTarget = onOpenLibraryTarget,
                    onDismiss = onDismiss,
                    onPickPlaylist = {
                        navigationDirection.intValue = SheetNavigationForward
                        screen = OverflowScreen.Playlist
                    },
                    onPickSleepTimer = {
                        navigationDirection.intValue = SheetNavigationForward
                        screen = OverflowScreen.SleepTimer
                    },
                )

                OverflowScreen.Playlist -> PlayListPickerContent(
                    songToAdd = song,
                    onDone = onDismiss,
                    onBack = {
                        navigationDirection.intValue = SheetNavigationBackward
                        screen = OverflowScreen.Menu
                    },
                )

                OverflowScreen.SleepTimer -> SleepTimerContent(
                    onDone = {},
                    onBack = {
                        navigationDirection.intValue = SheetNavigationBackward
                        screen = OverflowScreen.Menu
                    },
                )
            }
        }
    }
}

private enum class OverflowScreen { Menu, Playlist, SleepTimer }

private enum class OverflowLibraryTarget(val route: String)
{
    Artist(UI.ArtistInfo),
    Album(UI.AlbumInfo),
    OnlineArtist(UI.OnlineArtistInfo),
    OnlineAlbum(UI.OnlineAlbumInfo),
}

@Composable
private fun OverflowMenuBody(
    song: LyricsflowMediaItem?,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
    onDismiss: () -> Unit,
    onPickPlaylist: () -> Unit,
    onPickSleepTimer: () -> Unit,
) {
    val addToPlaylistLabel = stringResource(R.string.now_playing_overflow_add_to_playlist)
    val sleepTimerLabel = stringResource(R.string.now_playing_overflow_sleep_timer)

    // PRD FR-OM-13: highlight the Sleep Timer row when a timer is running.
    // No subtitle is shown — only the tint changes.
    val sleepTimerActive = SleepTimer.state.value is SleepTimerState.Active
    val accent = MaterialTheme.colorScheme.primary

    val items = remember(
        addToPlaylistLabel, sleepTimerLabel, sleepTimerActive, accent,
        onPickPlaylist, onPickSleepTimer,
    ) {
        listOf(
            ActionItem(
                iconRes = R.drawable.ic_action_add,
                label = addToPlaylistLabel,
                onClick = onPickPlaylist,
            ),
            ActionItem(
                iconRes = R.drawable.ic_setting_moon,
                label = sleepTimerLabel,
                tint = if (sleepTimerActive) accent else null,
                onClick = onPickSleepTimer,
            ),
        )
    }

    ActionSheetBody(
        header = if (song != null) {
            {
                NowPlayingOverflowHeader(
                    song = song,
                    navController = navController,
                    onOpenLibraryTarget = onOpenLibraryTarget,
                    onDismiss = onDismiss,
                )
            }
        } else null,
        items = items,
    )
}

/**
 * Header row for the Now Playing overflow sheet.
 *
 * 64dp rounded album thumbnail + three lines (title bold, artist, album).
 * Receives a snapshot [song] — content does NOT update if the playing track
 * changes while the sheet is open (PRD §5.3 FR-OM-8).
 */
@Composable
private fun NowPlayingOverflowHeader(
    song: LyricsflowMediaItem,
    navController: NavController,
    onOpenLibraryTarget: (OverflowLibraryTarget) -> Unit,
    onDismiss: () -> Unit,
) {
    val context = LocalContext.current
    val shape = LyricsflowRoundedCornerShape(8.dp)
    val targetArtistNames = remember(song) {
        song.artistsList.orEmpty().filter { it.isNotBlank() }
    }
    val targetAlbumName = remember(song) {
        song.album?.takeIf { it.isNotBlank() }
    }

    Row(
        modifier = Modifier
            .fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        AsyncImage(
            model = ImageRequest.Builder(context)
                .data(song.thumb)
                .crossfade(true)
                .build(),
            contentDescription = null,
            modifier = Modifier
                .size(64.dp)
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.Offscreen
                    clip = true
                    this.shape = shape
                },
        )
        Spacer(modifier = Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = song.title.orEmpty(),
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
            )
            if (targetArtistNames.isNotEmpty()) {
                val artistAnnotatedText = remember(targetArtistNames) {
                    buildAnnotatedString {
                        targetArtistNames.forEachIndexed { index, artistName ->
                            pushStringAnnotation(tag = "artist", annotation = artistName)
                            append(artistName)
                            pop()
                            if (index < targetArtistNames.lastIndex) {
                                append("、")
                            }
                        }
                    }
                }

                ClickableText(
                    text = artistAnnotatedText,
                    style = androidx.compose.ui.text.TextStyle(
                        color = Color.White.copy(alpha = 0.7f),
                        fontSize = 13.5.sp,
                        fontWeight = FontWeight.Normal,
                    ),
                    modifier = Modifier.padding(top = 3.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    onClick = { offset ->
                        val artistName = artistAnnotatedText
                            .getStringAnnotations(tag = "artist", start = offset, end = offset)
                            .firstOrNull()
                            ?.item
                            ?: return@ClickableText

                        val isOnlineTrack = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                        onDismiss()
                        if (isOnlineTrack) {
                            LibraryObject.openOnlineArtist(
                                artistId = song.artistId?.toString() ?: artistName,
                                name = artistName,
                                artworkUrl = song.thumb?.toString().orEmpty()
                            )
                            onOpenLibraryTarget(OverflowLibraryTarget.OnlineArtist)
                        } else {
                            LibraryObject.setTargetArtistName(artistName)
                            LibraryObject.setArtistSongsSearchOnOpen(false)
                            navController.markNextNavigationFromNowPlaying()
                            onOpenLibraryTarget(OverflowLibraryTarget.Artist)
                        }
                    },
                )
            } else if (!song.artists.isNullOrBlank()) {
                Text(
                    text = song.artists,
                    fontSize = 13.5.sp,
                    modifier = Modifier
                        .padding(top = 3.dp)
                        .alpha(0.7f),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            if (!song.album.isNullOrBlank()) {
                Text(
                    text = song.album,
                    fontSize = 12.5.sp,
                    modifier = Modifier
                        .padding(top = 2.dp)
                        .alpha(0.5f)
                        .clickable(
                            enabled = targetAlbumName != null,
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() },
                        ) {
                            val albumName = targetAlbumName ?: return@clickable
                            val isOnlineTrack = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                            onDismiss()
                            if (isOnlineTrack && (song.albumId ?: 0L) > 0L) {
                                LibraryObject.openOnlineAlbum(
                                    albumId = song.albumId.toString(),
                                    title = albumName,
                                    artist = song.artists.orEmpty(),
                                    artworkUrl = song.thumb?.toString().orEmpty()
                                )
                                onOpenLibraryTarget(OverflowLibraryTarget.OnlineAlbum)
                            } else {
                                LibraryObject.setTargetAlbumName(albumName)
                                navController.markNextNavigationFromNowPlaying()
                                onOpenLibraryTarget(OverflowLibraryTarget.Album)
                            }
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

@Composable
private fun PlayingBar(
    modifier: Modifier,
    navController: NavController,
    onMinimizeNowPlaying: suspend () -> Unit,
    albumUrlLambda: () -> Uri?,
    musicPlayingLambda: () -> LyricsflowMediaItem?,
    onAlbumClick: () -> Unit
) = LyricsflowWrapper {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(horizontal = 28.5.dp)
            .padding(top = 22.dp)
            .height(70.dp), verticalAlignment = Alignment.CenterVertically
    ) {
        ShadowImageWithCache(
            dataLambda = albumUrlLambda, contentDescription = null, modifier = modifier
                .size(69.dp)
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() },
                    onClick = {
                        onAlbumClick()
                    }), cornerRadius = 5.dp,
            imageQuality = ImageQuality.LOW,
            shadowType = ShadowType.Small,
            shadowOverlay = true
        )
        val subtitleMenuExpanded = remember { mutableStateOf(false) }
        val navScope = rememberCoroutineScope()
        Box(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(start = 12.dp, end = 15.dp)
        ) {
            val item = musicPlayingLambda()
            Column(Modifier.fillMaxWidth()) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = item?.title ?: defaultTitle,
                        fontSize = 16.5.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        fontWeight = FontWeight.SemiBold,
                        lineHeight = 16.5.sp,
                        modifier = Modifier.weight(1f, fill = false)
                    )
                    val isExplicitTitle = remember(item) {
                        item?.title?.contains("(Explicit", ignoreCase = true) == true ||
                            item?.compilation == "explicit"
                    }
                    if (isExplicitTitle) {
                        Spacer(Modifier.width(5.dp))
                        Box(
                            modifier = Modifier
                                .size(14.dp)
                                .background(
                                    Color.White.copy(alpha = 0.28f),
                                    RoundedCornerShape(3.dp)
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "E",
                                color = Color.Black.copy(alpha = 0.85f),
                                fontSize = 8.5.sp,
                                fontWeight = FontWeight.ExtraBold,
                                lineHeight = 9.sp
                            )
                        }
                    }
                }
                val subtitleText = remember(item) {
                    val artistStr = item?.artistsName ?: defaultArtistsName
                    val albumStr = item?.album?.takeIf { alb -> alb.isNotBlank() && alb != artistStr }
                    if (albumStr != null) "$artistStr — $albumStr" else artistStr
                }
                Text(
                    text = subtitleText,
                    fontSize = 14.5.sp,
                    modifier = Modifier
                        .overlayEffect()
                        .clickable(
                            indication = null,
                            interactionSource = remember { MutableInteractionSource() }
                        ) {
                            subtitleMenuExpanded.value = true
                        },
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    color = Color.White.copy(alpha = 0.55f)
                )
            }

            DropdownMenu(
                expanded = subtitleMenuExpanded.value,
                onDismissRequest = { subtitleMenuExpanded.value = false },
                modifier = Modifier.background(Color(0xEE1C1C22))
            ) {
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "Go to Artist (${item?.artistsName ?: defaultArtistsName})",
                            color = Color.White,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Medium
                        )
                    },
                    leadingIcon = {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_library_link_icon_artists),
                            contentDescription = null,
                            tint = Color(0xFFFA233B),
                            modifier = Modifier.size(20.dp)
                        )
                    },
                    onClick = {
                        subtitleMenuExpanded.value = false
                        val song = item ?: return@DropdownMenuItem
                        val artistName = song.artistsName ?: return@DropdownMenuItem
                        val isOnline = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                        navScope.launch { onMinimizeNowPlaying() }
                        if (isOnline) {
                            LibraryObject.openOnlineArtist(
                                artistId = song.artistId?.toString() ?: artistName,
                                name = artistName,
                                artworkUrl = song.thumb?.toString().orEmpty()
                            )
                            navController.toUI(UI.OnlineArtistInfo)
                        } else {
                            LibraryObject.setTargetArtistName(artistName)
                            LibraryObject.setArtistSongsSearchOnOpen(false)
                            navController.markNextNavigationFromNowPlaying()
                            navController.toUI(UI.ArtistInfo)
                        }
                    }
                )
                if (!item?.album.isNullOrBlank()) {
                    DropdownMenuItem(
                        text = {
                            Text(
                                text = "Go to Album (${item?.album.orEmpty()})",
                                color = Color.White,
                                fontSize = 14.sp,
                                fontWeight = FontWeight.Medium
                            )
                        },
                        leadingIcon = {
                            Icon(
                                painter = painterResource(id = R.drawable.ic_library_link_icon_album),
                                contentDescription = null,
                                tint = Color(0xFFFA233B),
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        onClick = {
                            subtitleMenuExpanded.value = false
                            val song = item ?: return@DropdownMenuItem
                            val albumName = song.album ?: return@DropdownMenuItem
                            val isOnline = song.uri?.scheme == "http" || song.uri?.scheme == "https"
                            navScope.launch { onMinimizeNowPlaying() }
                            if (isOnline && (song.albumId ?: 0L) > 0L) {
                                LibraryObject.openOnlineAlbum(
                                    albumId = song.albumId.toString(),
                                    title = albumName,
                                    artist = song.artists.orEmpty(),
                                    artworkUrl = song.thumb?.toString().orEmpty()
                                )
                                navController.toUI(UI.OnlineAlbumInfo)
                            } else {
                                LibraryObject.setTargetAlbumName(albumName)
                                navController.markNextNavigationFromNowPlaying()
                                navController.toUI(UI.AlbumInfo)
                            }
                        }
                    )
                }
            }
        }

        LyricsflowWrapper {
            ActionButtonsRow(navController, onMinimizeNowPlaying, musicPlayingLambda)
        }
    }

}

@Composable
fun RowScope.AirPlay() {
    val contextCompose = LocalContext.current
    val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    val connectedDevices =
        remember("AirPlay_connectedDevices") { mutableStateOf<List<BluetoothDevice>>(emptyList()) }
    val audioDeviceName = remember("AirPlay_audioDeviceName") { mutableStateOf("") }
    val showName = remember("AirPlay_showName") { mutableStateOf(false) }

    LyricsflowWrapper {
        DisposableEffect(Unit) {
            val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
                addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
                addAction("com.lyricsflow.player.BLUETOOTH_STATUS_REFRESH")
            }
            val receiver = object : BroadcastReceiver() {
                override fun onReceive(context: Context?, intent: Intent?) {
                    val action = intent?.action
                    if (action == BluetoothDevice.ACTION_ACL_CONNECTED || action == BluetoothDevice.ACTION_ACL_DISCONNECTED || action == "com.lyricsflow.player.BLUETOOTH_STATUS_REFRESH") {
                        if (ActivityCompat.checkSelfPermission(
                                contextCompose,
                                Manifest.permission.BLUETOOTH_CONNECT
                            ) != PackageManager.PERMISSION_GRANTED
                        ) {
                            return
                        }
                        connectedDevices.value =
                            bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()

                        val thisName =
                            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                                connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.alias
                            } else {
                                connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.name
                            }
                        showName.value = thisName != null
                        if (thisName != null) {
                            audioDeviceName.value = thisName.trim()
                        }
                    }
                }
            }
            // Use ContextCompat to handle the Android-14+ requirement that
            // registerReceiver carry an explicit export flag, transparently
            // on older API levels. Mirrors the existing TIRAMISU branch's
            // RECEIVER_EXPORTED choice to preserve behavior.
            ContextCompat.registerReceiver(
                contextCompose,
                receiver,
                filter,
                ContextCompat.RECEIVER_EXPORTED,
            )

            if (ActivityCompat.checkSelfPermission(
                    contextCompose,
                    Manifest.permission.BLUETOOTH_CONNECT
                ) == PackageManager.PERMISSION_GRANTED
            ) {
                connectedDevices.value = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
                val thisName =
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                        connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.alias
                    } else {
                        connectedDevices.value.firstOrNull { it.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO && it.isConnected() }?.name
                    }
                showName.value = thisName != null
                if (thisName != null) {
                    audioDeviceName.value = thisName.trim()
                }
            }

            onDispose {
                runCatching {
                    contextCompose.unregisterReceiver(receiver)
                }
            }
        }
    }

    LyricsflowWrapper {
        val context = LocalContext.current

        val systemMediaControlResolver = SystemMediaControlResolver(context)

        Column(
            modifier = Modifier
                .heightIn(min = 53.dp)
                .navigationBarsHeight(48.dp)
                .weight(1f)
                .clickable(
                    onClick = {
                        systemMediaControlResolver.intentSystemMediaDialog()
                    },
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .liquidGlassCircle(
                        surfaceTint = if (showName.value) Color.White.copy(alpha = 0.24f)
                        else Color.White.copy(alpha = 0.10f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    painterResource(id = R.drawable.ic_nowplaying_airplay),
                    contentDescription = "AirPlay & Audio Output",
                    tint = Color.White,
                    modifier = Modifier.size(21.5.dp)
                )
            }

            AnimatedVisibility(showName.value, enter = scaleIn(initialScale = 0.3f) + fadeIn(), exit = scaleOut(
                targetScale = 0.3f
            ) + fadeOut()) {
                Text(
                    text = audioDeviceName.value,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 11.sp,
                    lineHeight = 11.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

private fun BluetoothDevice.isConnected(): Boolean {
    return runCatching {
        val isConnectedMethod =
            BluetoothDevice::class.java.getMethod("isConnected")
        isConnectedMethod.isAccessible = true
        isConnectedMethod.invoke(this) as Boolean
    }.getOrDefault(false)
}

@SuppressLint("UnusedBoxWithConstraintsScope")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun PlayerControl(
    isPlayingLambda: () -> Boolean,
    isPlayingOnChanged: (Boolean) -> Unit,
    onPrevious: () -> Unit,
    onStatus: (Boolean) -> Unit,
    onNext: () -> Unit,
    onSeek: (Float) -> Unit,
    onLyrics: () -> Unit,
    onPlaylist: () -> Unit,
    nowPage: () -> String,
    onSlider: () -> Unit,
    onWhile: suspend () -> Unit,
    modifier: Modifier
) {
    val playingDuration = rememberSaveable(key = "PlayerControl_playingDuration") {
        mutableLongStateOf(0L)
    }
    val playingPosition = rememberSaveable(key = "PlayerControl_playingPosition") {
        mutableLongStateOf(0L)
    }
    val context = LocalContext.current
    val playedTime = rememberSaveable(key = "PlayerControl_playedTime") { mutableStateOf("0:00") }
    val remainingTime =
        rememberSaveable(key = "PlayerControl_remainingTime") { mutableStateOf("-0:00") }
    val sliderPosition = remember("PlayerControl_sliderPosition") { mutableFloatStateOf(0f) }
    val isSliding = remember("PlayerControl_isSliding") {
        mutableStateOf(false)
    }

    LyricsflowWrapper {
        Column(
            modifier = modifier
                .fillMaxWidth()
                .padding(horizontal = 25.dp)
                .padding(bottom = 15.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            LyricsflowWrapper {
                // 启动作用
                LyricsflowWrapper {
                    val lifecycleState =
                        LocalLifecycleOwner.current.lifecycle.currentStateFlow.collectAsState()

                    LaunchedEffect(Unit) {
                        var lastPosition = 0L
                        while (true) {
                            //isPlaying.value = /*mediaControl?.isPlaying ?: false*/ FadeExo.targetStatus != 0
                            if (lifecycleState.value.isAtLeast(Lifecycle.State.RESUMED)) {
                                playingDuration.longValue = mediaControl?.duration ?: 0
                                playingPosition.longValue = mediaControl?.currentPosition ?: 0

                                if (!isSliding.value && playingDuration.longValue > 0L) {
                                    val totalSeconds =
                                        playingPosition.longValue.coerceAtLeast(0) / 1000
                                    if (totalSeconds != lastPosition) {
                                        playedTime.value = formatTime(totalSeconds)

                                        sliderPosition.floatValue =
                                            playingPosition.longValue.coerceAtLeast(0).toFloat()

                                        val remainingSeconds =
                                            playingDuration.longValue.coerceAtLeast(0) / 1000 - totalSeconds
                                        remainingTime.value = "-${formatTime(remainingSeconds)}"
                                        lastPosition = totalSeconds
                                    }
                                }

                                onWhile()
                            }

                            delay(1000)
                        }
                    }
                }

                // 进度条
                LyricsflowWrapper {
                    //println("重组：控制区域内部 - 进度条")
                    Slider(
                        value = sliderPosition.floatValue,
                        onValueChange = { newValue ->
                            isSliding.value = true

                            sliderPosition.floatValue = newValue
                            val newTotalSeconds = newValue.toLong() / 1000
                            playedTime.value = formatTime(newTotalSeconds)

                            val newRemainingSeconds =
                                playingDuration.longValue / 1000 - newTotalSeconds
                            remainingTime.value = "-${formatTime(newRemainingSeconds)}"

                            onSlider()
                        },
                        onValueChangeFinished = {
                            Vibrator.longClick(context)
                            onSeek(sliderPosition.floatValue)
                            isSliding.value = false
                        },
                        valueRange = 0f..playingDuration.longValue.toFloat().coerceAtLeast(0f),
                        colors = SliderDefaults.colors(
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color(0x0DFFFFFF)
                        ),
                        modifier = Modifier
                            .overlayEffect()
                            .alpha(0.45f)
                            .height(14.dp),
                        thumb = {
                        },
                        track = {
                            Track(
                                sliderPositions = SliderPositions(
                                    initialActiveRange = 0f..(sliderPosition.floatValue / playingDuration.longValue)
                                ), height = 7.dp
                            )
                        }
                    )
                }

                // 控制按钮&进度文本
                LyricsflowWrapper {
                    //println("重组：控制区域内部 - 控制按钮&进度文本")
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp, horizontal = 7.dp)
                            .height(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = playedTime.value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.overlayEffect()
                            )
                            Text(
                                text = remainingTime.value,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium,
                                letterSpacing = 0.3.sp,
                                color = Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.overlayEffect()
                            )
                        }

                        MusicQualityIndicator()
                    }


                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Row(
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(61.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            onPrevious()
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_rewind),
                                    contentDescription = "Previous",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }

                            Spacer(modifier = Modifier.width(43.dp))

                            Box(
                                modifier = Modifier
                                    .size(58.5.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            isPlayingOnChanged(!isPlayingLambda())
                                            onStatus(isPlayingLambda())
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                val isBuffering = MediaViewModelObject.isBuffering.value
                                AnimatedContent(
                                    targetState = if (isBuffering) -1 else if (isPlayingLambda()) 1 else 0,
                                    transitionSpec = {
                                        (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                                            scaleOut(targetScale = 0.3f) + fadeOut()
                                        )
                                    }
                                ) { state ->
                                    if (state == -1) {
                                        com.lyricsflow.player.ui.widgets.glass.LyricsFlowAeroSpinner(
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(11.dp),
                                            color = Color.White
                                        )
                                    } else if (state == 1) {
                                        Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_pause),
                                            contentDescription = "Pause",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(10.dp)
                                        )
                                    } else {
                                        Icon(
                                            painterResource(id = R.drawable.ic_nowplaying_play),
                                            contentDescription = "Play",
                                            modifier = Modifier
                                                .fillMaxSize()
                                                .padding(9.dp)
                                        )
                                    }
                                }
                            }
                            Spacer(modifier = Modifier.width(43.dp))
                            Box(
                                modifier = Modifier
                                    .size(61.dp)
                                    .clickable(
                                        interactionSource = remember { MutableInteractionSource() },
                                        indication = ripple(bounded = false),
                                        onClick = {
                                            Vibrator.click(context)
                                            onNext()
                                        }),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    painterResource(id = R.drawable.ic_nowplaying_fforward),
                                    contentDescription = "Next",
                                    modifier = Modifier
                                        .fillMaxSize()
                                        .padding(10.dp)
                                )
                            }
                        }
                    }
                }
            }

            // 音量调节
            LyricsflowWrapper {
                if (SettingsLibrary.NowPlayingShowVolumeBar) {
                    VolumeSlider(context = context, onSlider)
                }
            }

            // 底部 歌词&播放列表 (Circular Liquid Glass Buttons)
            LyricsflowWrapper {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    val isLyricActive = nowPage() == Lyric
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .liquidGlassCircle(
                                    surfaceTint = if (isLyricActive) Color.White.copy(alpha = 0.88f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        onLyrics()
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(
                                    id = if (isLyricActive) R.drawable.ic_nowplaying_lyricson
                                    else R.drawable.ic_nowplaying_lyrics
                                ),
                                contentDescription = "Lyrics",
                                tint = if (isLyricActive) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }

                    AirPlay()

                    val isQueueActive = nowPage() == PlayingList
                    Box(
                        modifier = Modifier
                            .weight(1f),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .liquidGlassCircle(
                                    surfaceTint = if (isQueueActive) Color.White.copy(alpha = 0.88f)
                                    else Color.White.copy(alpha = 0.12f)
                                )
                                .clickable(
                                    onClick = {
                                        Vibrator.click(context)
                                        onPlaylist()
                                    },
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() }
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                painterResource(
                                    id = if (isQueueActive) R.drawable.ic_nowplaying_queueon
                                    else R.drawable.ic_nowplaying_queue
                                ),
                                contentDescription = "Playing Next Queue",
                                tint = if (isQueueActive) Color.Black else Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }

            // 边距填充
            /*LyricsflowWrapper {
                Spacer(modifier = Modifier.navigationBarsHeight(5.dp))
            }*/
            // 为显示设备名称，迁移到 AirPlay 底部处理
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun VolumeSlider(context: Context, onSlider: () -> Unit) {
    val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
    val maxVolume = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
    val currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    val sliderPosition =
        remember("VolumeSlider_sliderPosition") { mutableFloatStateOf(currentVolume / maxVolume.toFloat()) }
    val sliding = remember("VolumeSlider_sliding") {
        mutableStateOf(false)
    }

    val volumeChangeReceiver = remember("VolumeSlider_volumeChangeReceiver") {
        VolumeChangeReceiver { newVolume ->
            sliderPosition.floatValue = newVolume / maxVolume.toFloat()
        }
    }
    val intentFilter = IntentFilter("android.media.VOLUME_CHANGED_ACTION")

    DisposableEffect(Unit) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            context.registerReceiver(
                volumeChangeReceiver,
                intentFilter,
                Context.RECEIVER_NOT_EXPORTED
            )
        } else {
            context.registerReceiver(volumeChangeReceiver, intentFilter)
        }

        onDispose {
            context.unregisterReceiver(volumeChangeReceiver)
        }
    }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .padding(end = 1.5.dp)
            .padding(horizontal = 8.dp)
            .padding(top = 4.dp, bottom = 2.5.dp)
            .overlayEffect()
            .alpha(0.45f)
    ) {
        Icon(
            painter = painterResource(id = R.drawable.ic_nowplaying_volume),
            contentDescription = "Mute",
            modifier = Modifier.size(20.dp)
        )

        LyricsflowWrapper {
            val animatedProgress = if (sliding.value) {
                sliderPosition
            } else {
                animateFloatAsState(
                    targetValue = sliderPosition.floatValue,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    visibilityThreshold = 0.0001f
                )
            }

            Slider(
                value = (animatedProgress.value * maxVolume),
                onValueChange = { newValue ->
                    sliding.value = true
                    sliderPosition.floatValue = newValue / maxVolume
                    val volume = newValue.toInt()
                    audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, volume, 0)
                    onSlider()
                },
                valueRange = 0f..maxVolume.toFloat(),
                colors = SliderDefaults.colors(
                    activeTrackColor = Color.White,
                    inactiveTrackColor = Color(0x0DFFFFFF)
                ),
                modifier = Modifier
                    .weight(1f)
                    .padding(start = 1.5.dp, end = 5.dp),
                thumb = {
                },
                track = {
                    Track(
                        sliderPositions = SliderPositions(
                            initialActiveRange = 0f..animatedProgress.value
                        ), height = 7.dp
                    )
                },
                onValueChangeFinished = {
                    Vibrator.longClick(context)
                    sliding.value = false
                }
            )
        }
        Icon(
            painter = painterResource(id = R.drawable.ic_nowplaying_volume_full),
            contentDescription = "Max Volume",
            modifier = Modifier.size(20.dp)
        )
    }
}

@Composable
private fun Track(
    sliderPositions: SliderPositions,
    modifier: Modifier = Modifier,
    height: Dp
) = LyricsflowWrapper {
    val inactiveTrackColor = Color.White.copy(alpha = 0.5f)
    val activeTrackColor = Color.White
    val inactiveTickColor = Color.White.copy(alpha = 0.5f)
    val activeTickColor = Color.White
    Canvas(
        modifier
            .fillMaxWidth()
            .height(height)
    ) {
        val isRtl = layoutDirection == LayoutDirection.Rtl
        val sliderLeft = Offset(0f, center.y)
        val sliderRight = Offset(size.width, center.y)
        val sliderStart = if (isRtl) sliderRight else sliderLeft
        val sliderEnd = if (isRtl) sliderLeft else sliderRight
        val tickSize = 2.0.dp.toPx()
        val trackStrokeWidth = height.toPx()
        drawLine(
            inactiveTrackColor,
            sliderStart,
            sliderEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        val sliderValueEnd = Offset(
            sliderStart.x +
                    (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.endInclusive,
            center.y
        )

        val sliderValueStart = Offset(
            sliderStart.x +
                    (sliderEnd.x - sliderStart.x) * sliderPositions.activeRange.start,
            center.y
        )

        drawLine(
            activeTrackColor,
            sliderValueStart,
            sliderValueEnd,
            trackStrokeWidth,
            StrokeCap.Round
        )
        sliderPositions.tickFractions.groupBy {
            it > sliderPositions.activeRange.endInclusive ||
                    it < sliderPositions.activeRange.start
        }.forEach { (outsideFraction, list) ->
            drawPoints(
                list.fastMap {
                    Offset(lerp(sliderStart, sliderEnd, it).x, center.y)
                },
                PointMode.Points,
                (if (outsideFraction) inactiveTickColor else activeTickColor),
                tickSize,
                StrokeCap.Round
            )
        }
    }
}

fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return "$minutes:${if (secs < 10) "0$secs" else "$secs"}"
}
