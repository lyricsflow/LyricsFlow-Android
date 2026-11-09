package com.lyricsflow.player.ui.widgets.basic

import android.graphics.Matrix
import android.net.Uri
import android.view.TextureView
import android.view.View
import android.view.ViewGroup
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.viewinterop.AndroidView
import androidx.media3.common.C
import androidx.media3.common.MediaItem
import androidx.media3.common.PlaybackException
import androidx.media3.common.Player
import androidx.media3.common.VideoSize
import androidx.media3.exoplayer.ExoPlayer
import com.lyricsflow.player.code.AnimatedArtworkLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.YosMediaItem
import java.io.File
import kotlin.math.max

@Stable
class AnimatedAlbumCoverState internal constructor(
    internal val player: ExoPlayer?,
    internal val isPlaying: Boolean,
    val videoPrepared: Boolean,
    internal val videoWidth: Int = 0,
    internal val videoHeight: Int = 0
)

@Composable
fun rememberAnimatedAlbumCoverState(
    music: YosMediaItem?,
    isPlaying: Boolean,
    active: Boolean
): AnimatedAlbumCoverState {
    val context = LocalContext.current
    val animatedAlbumCovers = SettingsLibrary.AnimatedAlbumCovers
    val animatedAlbumCoversUseApi = SettingsLibrary.AnimatedAlbumCoversUseApi
    val animatedAlbumCoverBlacklist = SettingsLibrary.AnimatedAlbumCoverBlacklist
    var animatedArtworkFile by remember(
        music?.uri,
        music?.album,
        active,
        animatedAlbumCovers,
        animatedAlbumCoversUseApi,
        animatedAlbumCoverBlacklist
    ) {
        mutableStateOf<File?>(null)
    }

    LaunchedEffect(
        music?.uri,
        music?.album,
        active,
        animatedAlbumCovers,
        animatedAlbumCoversUseApi,
        animatedAlbumCoverBlacklist
    ) {
        animatedArtworkFile = if (!active || music == null) {
            null
        } else {
            AnimatedArtworkLibrary.resolveArtworkFile(context.applicationContext, music)
        }
    }

    val artworkFile = animatedArtworkFile
    if (artworkFile == null) {
        return AnimatedAlbumCoverState(null, isPlaying, false)
    }

    val artworkUri = remember(artworkFile) { Uri.fromFile(artworkFile) }
    val latestIsPlaying = rememberUpdatedState(isPlaying)
    var videoPrepared by remember(artworkFile) { mutableStateOf(false) }
    var videoWidth by remember(artworkFile) { mutableIntStateOf(0) }
    var videoHeight by remember(artworkFile) { mutableIntStateOf(0) }
    val player = remember(artworkFile) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(artworkUri))
            prepare()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoPrepared = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onPlayerError(error: PlaybackException) {
                videoPrepared = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED && latestIsPlaying.value) {
                    player.seekTo(0L)
                    player.play()
                }
            }
        }

        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    LaunchedEffect(player, isPlaying) {
        player.playWhenReady = isPlaying
        if (isPlaying) {
            player.play()
        } else {
            player.pause()
        }
    }

    return AnimatedAlbumCoverState(player, isPlaying, videoPrepared, videoWidth, videoHeight)
}

@Composable
fun rememberOnlineAnimatedVideoState(
    cacheKey: String,
    artistName: String = "",
    albumName: String = "",
    albumId: String? = null,
    directOrHlsUrl: String? = null,
    active: Boolean = true
): AnimatedAlbumCoverState {
    val context = LocalContext.current
    val animatedAlbumCovers = SettingsLibrary.AnimatedAlbumCovers
    var animatedArtworkFile by remember(cacheKey, albumId, directOrHlsUrl, active, animatedAlbumCovers) {
        mutableStateOf<File?>(null)
    }

    LaunchedEffect(cacheKey, albumId, directOrHlsUrl, active, animatedAlbumCovers) {
        animatedArtworkFile = if (!active || !animatedAlbumCovers) {
            null
        } else {
            AnimatedArtworkLibrary.resolveOnlineAnimatedArtFile(
                context = context.applicationContext,
                cacheKey = cacheKey,
                artistName = artistName,
                albumName = albumName,
                albumId = albumId,
                directOrHlsUrl = directOrHlsUrl
            )
        }
    }

    val artworkFile = animatedArtworkFile ?: return AnimatedAlbumCoverState(null, true, false)
    val artworkUri = remember(artworkFile) { Uri.fromFile(artworkFile) }
    var videoPrepared by remember(artworkFile) { mutableStateOf(false) }
    var videoWidth by remember(artworkFile) { mutableIntStateOf(0) }
    var videoHeight by remember(artworkFile) { mutableIntStateOf(0) }

    val player = remember(artworkFile) {
        ExoPlayer.Builder(context.applicationContext).build().apply {
            volume = 0f
            trackSelectionParameters = trackSelectionParameters
                .buildUpon()
                .setTrackTypeDisabled(C.TRACK_TYPE_AUDIO, true)
                .build()
            repeatMode = Player.REPEAT_MODE_ONE
            setMediaItem(MediaItem.fromUri(artworkUri))
            prepare()
            playWhenReady = true
            play()
        }
    }

    DisposableEffect(player) {
        val listener = object : Player.Listener {
            override fun onRenderedFirstFrame() {
                videoPrepared = true
            }

            override fun onVideoSizeChanged(videoSize: VideoSize) {
                videoWidth = videoSize.width
                videoHeight = videoSize.height
            }

            override fun onPlayerError(error: PlaybackException) {
                videoPrepared = false
            }

            override fun onPlaybackStateChanged(playbackState: Int) {
                if (playbackState == Player.STATE_ENDED) {
                    player.seekTo(0L)
                    player.play()
                }
            }
        }
        player.addListener(listener)
        onDispose {
            player.removeListener(listener)
            player.release()
        }
    }

    return AnimatedAlbumCoverState(player, true, videoPrepared, videoWidth, videoHeight)
}

private fun applyCenterCropTransform(
    textureView: TextureView,
    videoWidth: Int,
    videoHeight: Int
) {
    val viewWidth = textureView.width.toFloat()
    val viewHeight = textureView.height.toFloat()
    if (viewWidth <= 0f || viewHeight <= 0f || videoWidth <= 0 || videoHeight <= 0) return

    val scaleX = viewWidth / videoWidth.toFloat()
    val scaleY = viewHeight / videoHeight.toFloat()
    val maxScale = max(scaleX, scaleY)

    val scaledWidth = videoWidth * maxScale
    val scaledHeight = videoHeight * maxScale

    val dx = (viewWidth - scaledWidth) / 2f
    val dy = (viewHeight - scaledHeight) / 2f

    val matrix = Matrix()
    // TextureView default stretches video to (viewWidth, viewHeight);
    // first normalize back to video aspect, then apply maxScale around center
    matrix.setScale(scaledWidth / viewWidth, scaledHeight / viewHeight)
    matrix.postTranslate(dx, dy)
    textureView.setTransform(matrix)
}

@Composable
fun BoxScope.AnimatedAlbumCoverOverlay(state: AnimatedAlbumCoverState) {
    val player = state.player ?: return
    var textureView by remember(player) { mutableStateOf<TextureView?>(null) }
    val videoAlpha by animateFloatAsState(
        targetValue = if (state.videoPrepared) 1f else 0f,
        animationSpec = tween(durationMillis = 350, easing = FastOutSlowInEasing),
        label = "AnimatedAlbumCoverAlpha"
    )

    DisposableEffect(player) {
        onDispose {
            textureView?.let { player.clearVideoTextureView(it) }
        }
    }

    AndroidView(
        factory = { viewContext ->
            TextureView(viewContext).apply {
                layoutParams = ViewGroup.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT
                )
                addOnLayoutChangeListener { v, _, _, _, _, _, _, _, _ ->
                    if (v is TextureView) {
                        applyCenterCropTransform(v, state.videoWidth, state.videoHeight)
                    }
                }
            }
        },
        update = { view ->
            if (view.tag != player) {
                view.tag = player
                textureView = view
                player.setVideoTextureView(view)
            }
            applyCenterCropTransform(view, state.videoWidth, state.videoHeight)
            player.playWhenReady = state.isPlaying
            if (state.isPlaying) {
                player.play()
            } else {
                player.pause()
            }
        },
        modifier = Modifier
            .fillMaxSize()
            .graphicsLayer {
                alpha = videoAlpha
            }
    )
}

/**
 * Apple Music Full-Bleed Animated Cover Art with smooth bottom gradient fade
 * (matching the iOS Apple Music SZA "SOS" full-bleed motion cover aesthetic).
 * Bleeds edge-to-edge from the very top of the NowPlaying sheet and dissolves
 * smoothly into the ambient dark background right above the track title/artist.
 */
@Composable
fun FullBleedAnimatedAlbumCover(
    state: AnimatedAlbumCoverState,
    visible: Boolean,
    modifier: Modifier = Modifier
) {
    val videoAlpha by animateFloatAsState(
        targetValue = if (visible && state.videoPrepared && state.player != null) 1f else 0f,
        animationSpec = tween(durationMillis = 420, easing = FastOutSlowInEasing),
        label = "FullBleedAnimatedCoverAlpha"
    )
    if (videoAlpha <= 0.001f && (!visible || state.player == null)) return

    Box(
        modifier = modifier
            .fillMaxWidth()
            .fillMaxHeight(0.635f)
            .graphicsLayer {
                compositingStrategy = CompositingStrategy.Offscreen
                alpha = videoAlpha
            }
            .drawWithCache {
                val dstInFadeBrush = Brush.verticalGradient(
                    0.00f to Color.Black,
                    0.58f to Color.Black,
                    0.74f to Color.Black.copy(alpha = 0.72f),
                    0.86f to Color.Black.copy(alpha = 0.32f),
                    0.95f to Color.Black.copy(alpha = 0.08f),
                    1.00f to Color.Transparent
                )
                val topSubtleVignette = Brush.verticalGradient(
                    0.00f to Color.Black.copy(alpha = 0.22f),
                    0.14f to Color.Transparent
                )
                onDrawWithContent {
                    drawContent()
                    drawRect(brush = topSubtleVignette)
                    drawRect(
                        brush = dstInFadeBrush,
                        blendMode = BlendMode.DstIn
                    )
                }
            },
        contentAlignment = Alignment.TopCenter
    ) {
        AnimatedAlbumCoverOverlay(state = state)
    }
}
