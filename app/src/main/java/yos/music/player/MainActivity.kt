@file:OptIn(ExperimentalSharedTransitionApi::class)

package com.lyricsflow.player

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothClass
import android.bluetooth.BluetoothDevice
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.ExperimentalSharedTransitionApi
import androidx.compose.animation.SharedTransitionLayout
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.FiniteAnimationSpec
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandHorizontally
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkHorizontally
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.material.ripple
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.lerp
import androidx.core.content.ContextCompat
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.viewModelScope
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.google.accompanist.insets.ProvideWindowInsets
import com.google.accompanist.insets.navigationBarsHeight
import com.google.accompanist.navigation.animation.AnimatedNavHost
import com.google.accompanist.systemuicontroller.rememberSystemUiController
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.haze
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import com.lyricsflow.player.code.ListenHistoryManager
import com.lyricsflow.player.code.ListenStatsManager
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.SystemMediaControlResolver
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.code.utils.player.FadeExo.fadePause
import com.lyricsflow.player.code.utils.player.FadeExo.fadePlay
import com.lyricsflow.player.data.libraries.MusicLibrary
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.models.ImageViewModel
import com.lyricsflow.player.data.models.MainViewModel
import com.lyricsflow.player.data.models.MediaViewModel
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.ui.UI
import com.lyricsflow.player.ui.UI.Settings.Companion.ExoplayerSetting
import com.lyricsflow.player.ui.pages.HomeNav
import com.lyricsflow.player.ui.pages.LocalUploadAndTtmlToolsView
import com.lyricsflow.player.ui.pages.NowPlaying
import com.lyricsflow.player.ui.pages.NowPlayingPage.Album
import com.lyricsflow.player.ui.pages.OnlineAlbumView
import com.lyricsflow.player.ui.pages.OnlineArtistView
import com.lyricsflow.player.ui.pages.OnlinePlaylistView
import com.lyricsflow.player.ui.pages.StatsAlbums
import com.lyricsflow.player.ui.pages.StatsArtists
import com.lyricsflow.player.ui.pages.StatsTracks
import com.lyricsflow.player.ui.pages.library.Library
import com.lyricsflow.player.ui.pages.library.NormalMusic
import com.lyricsflow.player.ui.pages.library.albums.AlbumInfo
import com.lyricsflow.player.ui.pages.library.albums.LocalAlbums
import com.lyricsflow.player.ui.pages.library.artists.ArtistInfo
import com.lyricsflow.player.ui.pages.library.artists.ArtistSongs
import com.lyricsflow.player.ui.pages.library.artists.LocalArtists
import com.lyricsflow.player.ui.pages.library.playlists.PlayLists
import com.lyricsflow.player.ui.pages.settings.Settings
import com.lyricsflow.player.ui.pages.settings.audio.exoPlayer.ExoPlayerSettings
import com.lyricsflow.player.ui.pages.settings.audio.exoPlayer.MediaCodec
import com.lyricsflow.player.ui.pages.settings.extend.statusBarLyric.LyricGetter
import com.lyricsflow.player.ui.pages.settings.library.ArtistSplitSetting
import com.lyricsflow.player.ui.pages.settings.library.LibraryOverview
import com.lyricsflow.player.ui.pages.settings.others.About
import com.lyricsflow.player.ui.pages.settings.performance.LyricSetting
import com.lyricsflow.player.ui.pages.settings.performance.NotificationSetting
import com.lyricsflow.player.ui.pages.settings.performance.userinterface.AnimatedAlbumCoverBlacklistSetting
import com.lyricsflow.player.ui.pages.settings.performance.userinterface.AnimatedAlbumCoversSetting
import com.lyricsflow.player.ui.pages.settings.performance.userinterface.ScreenCornerSetDialog
import com.lyricsflow.player.ui.pages.settings.performance.userinterface.UserInterfaceSetting
import com.lyricsflow.player.ui.theme.YosMusicTheme
import com.lyricsflow.player.ui.theme.YosRoundedCornerShape
import com.lyricsflow.player.ui.theme.isFlamingoInDarkMode
import com.lyricsflow.player.ui.theme.withNight
import androidx.compose.runtime.CompositionLocalProvider
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.layerBackdrop
import com.lyricsflow.player.ui.widgets.basic.BottomNavigator
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.NavItem
import com.lyricsflow.player.ui.widgets.basic.ShadowImageWithCache
import com.lyricsflow.player.ui.widgets.basic.YosWrapper
import com.lyricsflow.player.ui.widgets.glass.LocalAppBackdrop
import com.lyricsflow.player.ui.widgets.glass.LyricsFlowFloatingMiniPlayer
import com.lyricsflow.player.ui.widgets.glass.LyricsFlowFloatingTabBar
import com.lyricsflow.player.ui.widgets.glass.rememberLiquidGlassBackdrop
import java.io.File
import kotlin.math.abs

/*//MediaPlayer全局控制器
var mediaController = com.lyricsflow.player.code.MediaController*/

class MainActivity : BaseActivity() {

    private val mediaViewModel: MediaViewModel by viewModels()
    private val mainViewModel: MainViewModel by viewModels()

    private val imageViewModel: ImageViewModel by viewModels()

    @Suppress("DEPRECATION")
    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalHazeMaterialsApi::class, ExperimentalSharedTransitionApi::class
    )
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            YosMusicTheme {
                ProvideWindowInsets {
                    val context = LocalContext.current
                    val density = LocalDensity.current

                    LaunchedEffect(Unit) {
                        withFrameNanos {}
                        MediaController.ensureInitialized(context)
                        ListenHistoryManager.loadHistory()
                        ListenStatsManager.loadEvents()
                    }

                    val offsetY = remember("MainActivity_offsetY") { Animatable(0f) }
                    val parentHeight =
                        remember("MainActivity_parentHeight") { mutableIntStateOf(0) }
                    val screenCorner = remember("MainActivity_screenCorner") {
                        val corner = SettingsLibrary.ScreenCorner.toInt()
                        if (corner == 0) 1 else corner
                    }

                    val scope = rememberCoroutineScope()

                    /*val parentHeightDp = remember(parentHeight.intValue) {
                        with(density) {
                            parentHeight.intValue.toDp()
                        }
                    }*/

                    /*Surface(
                        modifier = Modifier
                            .fillMaxSize(),
                        color = Color.Transparent,
                        contentColor = Color.Black withNight Color.White
                    ) {*/
                        val miniPlayerHeight = 62.dp
                        val height = remember("MainActivity_height") { mutableIntStateOf(0) }

                        val navHeight = remember("MainActivity_navHeight") {
                            with(density) {
                                height.intValue.toDp().plus(miniPlayerHeight)
                            }
                        }
                        // 逻辑初始化区域
                        val navController = rememberNavController()
                        val navSpec = spring(
                            stiffness = 400f,
                            dampingRatio = 1f,
                            visibilityThreshold = 0.001f
                        )
                        // val scaffoldState = rememberBottomSheetScaffoldState()
                        val route = rememberSaveable(key = "MainActivity_route") {
                            mutableStateOf(UI.HomePage)
                        }
                        // 记录当前路线

                        YosWrapper {
                            val backstackEntry =
                                navController.currentBackStackEntryAsState()
                            route.value =
                                backstackEntry.value?.destination?.route ?: UI.HomePage
                        }

                        // 显示控制区域
                        val yosBottomSheetConfig = object {
                            val progress
                                get() = if (parentHeight.intValue == 0) 0f else abs(offsetY.value / parentHeight.intValue).coerceIn(
                                    0f,
                                    1f
                                )
                            val menuAlpha
                                get() = 1f - progress
                            val mainContainerCardScale
                                get() = 0.9f + (0.1f * menuAlpha)
                            val thisShowCorner
                                get() = progress > 0f

                            val barShowCorner
                                get() = progress < 1f
                            val showMenu
                                get() = progress < 1f

                            val barShapeValue
                                get() = lerp(12, screenCorner, progress)

                            val RTCorner
                                get() = screenCorner == 0
                        }

                        val showNowPlaying = remember("MainActivity_showNowPlaying") {
                            derivedStateOf {
                                yosBottomSheetConfig.menuAlpha < 0.3f
                            }
                        }

                        val nowPageNowPlaying =
                            rememberSaveable(key = "MainActivity_nowPageNowPlaying") {
                                mutableStateOf(Album)
                            }

                        val hazeState = remember { HazeState() }
                        YosWrapper {
                            val isNight = isFlamingoInDarkMode()
                            if (showNowPlaying.value) {
                                rememberSystemUiController().run {
                                    setNavigationBarColor(
                                        Color.Transparent,
                                        darkIcons = false,
                                        navigationBarContrastEnforced = false
                                    )
                                    setStatusBarColor(Color.Transparent, darkIcons = false)
                                    var activity = LocalContext.current as? Activity
                                    DisposableEffect(Unit) {
                                        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                        onDispose {
                                            activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
                                            activity = null
                                        }
                                    }
                                }
                            } else {
                                rememberSystemUiController().run {
                                    setNavigationBarColor(
                                        color = Color.Transparent,
                                        darkIcons = !isNight,
                                        navigationBarContrastEnforced = false
                                    )

                                    setStatusBarColor(Color.Transparent, darkIcons = !isNight)
                                }
                            }
                        }

                        // 导航区域
                        val defaultHome = "Home"

                        val nowLabel = rememberSaveable(key = "MainActivity_nowLabel") {
                            mutableStateOf(defaultHome)
                        }

                        val pagerState = rememberPagerState(pageCount = { 5 })
                        val appBackdrop = rememberLiquidGlassBackdrop()

                        // 以下为实际显示

                        // 主界面
                        CompositionLocalProvider(LocalAppBackdrop provides appBackdrop) {
                        YosWrapper {
                            Surface(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        val thisMainContainerCardScale =
                                            yosBottomSheetConfig.mainContainerCardScale
                                        scaleX = thisMainContainerCardScale
                                        scaleY = thisMainContainerCardScale
                                    }
                                    .graphicsLayer {
                                        //compositingStrategy = CompositingStrategy.Offscreen
                                        //transformOrigin = TransformOrigin(0.5f, 1f)

                                        if (yosBottomSheetConfig.thisShowCorner && !yosBottomSheetConfig.RTCorner) {
                                            compositingStrategy = CompositingStrategy.Offscreen
                                            clip = true
                                            shape = YosRoundedCornerShape(screenCorner.dp)
                                        } else {
                                            compositingStrategy = CompositingStrategy.ModulateAlpha
                                            clip = false
                                        }

                                        if (!yosBottomSheetConfig.barShowCorner) {
                                            alpha = 0f
                                        }
                                    }

                                ,
                                color = MaterialTheme.colorScheme.background,
                                contentColor = MaterialTheme.colorScheme.onBackground
                            ) {
                                // 主界面本体
                                SharedTransitionLayout {
                                    YosWrapper {
                                        BackHandler(showNowPlaying.value) {
                                            // println("isVisible: ${showNowPlaying.value}, nowPageNowPlaying.value: ${nowPageNowPlaying.value}")
                                            if (nowPageNowPlaying.value != Album) {
                                                nowPageNowPlaying.value =
                                                    Album
                                            } else {
                                                scope.launch {
                                                    // scaffoldState.bottomSheetState.partialExpand()
                                                    offsetY.animateTo(0f, animationSpec = navSpec)
                                                }
                                            }
                                        }
                                    }

                                    YosWrapper {
                                        val animateSpeed = 340
                                        val animationSpec: FiniteAnimationSpec<IntSize> =
                                            tween(
                                                durationMillis = animateSpeed,
                                                easing = EaseOutQuart
                                            )
                                        val fadeAnimationSpec: FiniteAnimationSpec<Float> =
                                            tween(
                                                durationMillis = animateSpeed - 160,
                                                easing = EaseOutQuart
                                            )
                                        AnimatedNavHost(
                                            modifier = Modifier
                                                .layerBackdrop(appBackdrop)
                                                .then(
                                                    if (SettingsLibrary.BarBlurEffect && !showNowPlaying.value) {
                                                        Modifier.haze(state = hazeState)
                                                    } else {
                                                        Modifier
                                                    }
                                                ),
                                            navController = navController,
                                            startDestination = UI.HomePage,
                                            enterTransition = {
                                                fadeIn(animationSpec = fadeAnimationSpec) + expandHorizontally(
                                                    animationSpec = animationSpec,
                                                    clip = false,
                                                    expandFrom = Alignment.Start
                                                ) {
                                                    -it / 2
                                                }
                                            },
                                            exitTransition = {
                                                fadeOut(animationSpec = fadeAnimationSpec) + shrinkHorizontally(
                                                    animationSpec = animationSpec,
                                                    clip = false,
                                                    shrinkTowards = Alignment.End
                                                ) {
                                                    it / 2
                                                }
                                            },
                                            popEnterTransition = {
                                                fadeIn(animationSpec = fadeAnimationSpec) + expandHorizontally(
                                                    animationSpec = animationSpec,
                                                    clip = false,
                                                    expandFrom = Alignment.End
                                                ) {
                                                    it / 2
                                                }
                                            },
                                            popExitTransition = {
                                                fadeOut(animationSpec = fadeAnimationSpec) + shrinkHorizontally(
                                                    animationSpec = animationSpec,
                                                    clip = false,
                                                    shrinkTowards = Alignment.Start
                                                ) {
                                                    -it / 2
                                                }
                                            }) {

                                            composable(UI.HomePage) {
                                                HomeNav(
                                                    navController,
                                                    pagerState,
                                                    imageViewModel
                                                ) {
                                                    nowLabel.value = it
                                                }
                                            }
                                            composable(UI.Library) {
                                                Library(
                                                    navController
                                                )
                                            }

                                            composable(UI.PlayLists) {
                                                PlayLists(navController)
                                            }
                                            composable(UI.NormalMusic) {
                                                NormalMusic(navController)
                                            }
                                            composable(UI.LocalAlbums) {
                                                LocalAlbums(
                                                    navController,
                                                    this@SharedTransitionLayout,
                                                    this@composable
                                                )
                                            }
                                            composable(UI.LocalArtists) {
                                                LocalArtists(navController)
                                            }
                                            composable(UI.ArtistInfo) {
                                                ArtistInfo(navController = navController)
                                            }
                                            composable(UI.ArtistSongs) {
                                                ArtistSongs(navController)
                                            }

                                            composable(UI.AlbumInfo) {
                                                AlbumInfo(
                                                    navController = navController,
                                                    sharedTransitionScope = this@SharedTransitionLayout,
                                                    animatedContentScope = this@composable,
                                                )
                                            }

                                            composable(UI.OnlineAlbumInfo) {
                                                OnlineAlbumView(navController = navController)
                                            }
                                            composable(UI.OnlineArtistInfo) {
                                                OnlineArtistView(navController = navController)
                                            }
                                            composable(UI.OnlinePlaylistInfo) {
                                                OnlinePlaylistView(navController = navController)
                                            }
                                            composable(UI.LocalUploadTools) {
                                                LocalUploadAndTtmlToolsView(navController = navController)
                                            }

                                            composable(UI.StatsArtists) {
                                                StatsArtists(navController)
                                            }
                                            composable(UI.StatsAlbums) {
                                                StatsAlbums(navController)
                                            }
                                            composable(UI.StatsTracks) {
                                                StatsTracks(navController)
                                            }

                                            composable(UI.Settings.Main) {
                                                Settings(
                                                    navController
                                                )
                                            }
                                            composable(UI.Settings.LibraryOverview) {
                                                LibraryOverview(navController)
                                            }
                                            composable(UI.Settings.ArtistSplit) {
                                                ArtistSplitSetting(navController)
                                            }
                                            composable(UI.Settings.LyricGetter) {
                                                LyricGetter(navController)
                                            }
                                            composable(ExoplayerSetting) {
                                                ExoPlayerSettings(navController)
                                            }
                                            composable(UI.Settings.About) {
                                                About(
                                                    navController
                                                )
                                            }
                                            composable(UI.Settings.MediaCodec) {
                                                MediaCodec(navController)
                                            }

                                            composable(UI.Settings.LyricSetting) {
                                                LyricSetting(navController)
                                            }
                                            composable(UI.Settings.UserInterfaceSetting) {
                                                UserInterfaceSetting(navController)
                                            }
                                            composable(UI.Settings.AnimatedAlbumCoversSetting) {
                                                AnimatedAlbumCoversSetting(navController)
                                            }
                                            composable(UI.Settings.AnimatedAlbumCoverBlacklist) {
                                                AnimatedAlbumCoverBlacklistSetting(navController)
                                            }
                                            composable(UI.Settings.NotificationSetting) {
                                                NotificationSetting(navController)
                                            }
                                        }


                                    }
                                }

                                // Bottom gradient behind floating tab bar
                                YosWrapper {
                                    val color =
                                        Color(0xFFF5F5F5) withNight Color.Black
                                    Box(
                                        Modifier.fillMaxSize(),
                                        contentAlignment = Alignment.BottomCenter
                                    ) {
                                        Spacer(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .navigationBarsHeight(104.dp)
                                                .background(
                                                    brush = Brush.verticalGradient(
                                                        colors = listOf(
                                                            Color.Transparent,
                                                            color.copy(alpha = 0.22f),
                                                            color.copy(alpha = 0.52f)
                                                        )
                                                    )
                                                )
                                        )
                                    }
                                }
                            }

                            // 弹窗
                            YosWrapper {
                                val showCornerSetDialog =
                                    remember("MainActivity_showCornerSetDialog") {
                                        mutableStateOf(!SettingsLibrary.ScreenCornerSet)
                                    }

                                if (showCornerSetDialog.value) {
                                    ScreenCornerSetDialog {
                                        showCornerSetDialog.value = false
                                    }
                                } else {
                                    CheckAndRequestPermission()
                                }
                            }
                        }

                        // 播放条&播放界面
                        YosWrapper {
                            if (height.intValue == 0) return@YosWrapper

                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .onSizeChanged {
                                        parentHeight.intValue = it.height
                                    }
                                    .graphicsLayer {
                                        this.scaleX = 1f
                                        this.scaleY = 1f
                                        this.translationY =
                                            -(height.intValue + 6) * (yosBottomSheetConfig.menuAlpha)
                                        this.transformOrigin = TransformOrigin(0.5f, 1f)
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                YosWrapper {
                                    val miniPlayerHeightPx = remember("MainActivity_miniPlayerHeightPx") {
                                        with(density) {
                                            miniPlayerHeight.toPx()
                                        }
                                    }

                                    val miniPlayerShadow = remember("MainActivity_miniPlayerShadow") {
                                        with(density) {
                                            7.5.dp.toPx()
                                        }
                                    }

                                    val dragState = rememberDraggableState { delta ->
                                        scope.launch {
                                            offsetY.snapTo(offsetY.value + delta)
                                        }
                                    }

                                    Surface(
                                        modifier = Modifier
                                            .graphicsLayer {
                                                translationY = if (yosBottomSheetConfig.progress <= 0.01f) {
                                                    0f
                                                } else {
                                                    ((parentHeight.intValue - miniPlayerHeightPx) * (1 - yosBottomSheetConfig.progress))
                                                }

                                                if (yosBottomSheetConfig.barShowCorner && yosBottomSheetConfig.progress > 0.04f) {
                                                    shape = YosRoundedCornerShape(
                                                        roundSize = yosBottomSheetConfig.barShapeValue.dp,
                                                        mSize = Size(
                                                            width = size.width,
                                                            height = (miniPlayerHeightPx + (parentHeight.intValue - miniPlayerHeightPx) * yosBottomSheetConfig.progress)
                                                        )
                                                    )
                                                    shadowElevation = miniPlayerShadow
                                                    spotShadowColor = Color.Black.copy(alpha = 0.2f)
                                                }
                                            }
                                            .graphicsLayer {
                                                if (yosBottomSheetConfig.barShowCorner && yosBottomSheetConfig.progress > 0.04f) {
                                                    compositingStrategy = CompositingStrategy.Offscreen
                                                    clip = true
                                                    shape = YosRoundedCornerShape(
                                                        roundSize = yosBottomSheetConfig.barShapeValue.dp,
                                                        mSize = Size(
                                                            width = size.width,
                                                            height = (miniPlayerHeightPx + (parentHeight.intValue - miniPlayerHeightPx) * yosBottomSheetConfig.progress)
                                                        )
                                                    )
                                                } else {
                                                    compositingStrategy = CompositingStrategy.ModulateAlpha
                                                    clip = false
                                                }
                                            }
                                            .draggable(
                                                reverseDirection = true,
                                                orientation = Orientation.Vertical,
                                                state = dragState,
                                                onDragStopped = { velocity ->
                                                    offsetY.updateBounds(
                                                        0f,
                                                        parentHeight.intValue.toFloat()
                                                    )
                                                    scope.launch {
                                                        if (velocity < 0f) {
                                                            offsetY.animateTo(
                                                                0f,
                                                                initialVelocity = velocity
                                                            )
                                                        } else {
                                                            offsetY.animateTo(
                                                                parentHeight.intValue.toFloat(),
                                                                initialVelocity = velocity
                                                            )
                                                        }
                                                    }
                                                }
                                            )
                                            .layout { measurable, constraints ->
                                                val targetH = if (yosBottomSheetConfig.progress <= 0.01f) {
                                                    miniPlayerHeightPx.toInt().coerceAtLeast(1)
                                                } else {
                                                    parentHeight.intValue
                                                }
                                                val placeable = measurable.measure(
                                                    constraints.copy(
                                                        minHeight = targetH,
                                                        maxHeight = targetH
                                                    )
                                                )
                                                layout(
                                                    placeable.width,
                                                    placeable.height
                                                ) {
                                                    placeable.placeRelative(0, 0)
                                                }
                                            },
                                        color = Color.Transparent
                                    ) {
                                        val isPlaying =
                                            rememberSaveable(key = "MainActivity_isPlaying") {
                                                MediaViewModelObject.isPlaying
                                            }

                                        // NowPlaying (only composed when expanding/expanded so it never intercepts touches when minimized)
                                        YosWrapper {
                                            if (yosBottomSheetConfig.progress > 0.01f) {
                                                Box(
                                                    modifier = Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer {
                                                            alpha = yosBottomSheetConfig.progress.coerceIn(0f, 1f)
                                                        }
                                                ) {
                                                    NowPlaying(
                                                        mainViewModel = mainViewModel,
                                                        mediaViewModel = mediaViewModel,
                                                        navController = navController,
                                                        onMinimizeNowPlaying = {
                                                            nowPageNowPlaying.value = Album
                                                            offsetY.animateTo(0f, animationSpec = navSpec)
                                                        },
                                                        isPlayingStatusLambda = { isPlaying.value },
                                                        isPlayingOnChanged = {
                                                            isPlaying.value = it
                                                        },
                                                        nowPageLambda = { nowPageNowPlaying.value },
                                                        showNowPlaying = { showNowPlaying.value },
                                                        showMiniPlayer = { yosBottomSheetConfig.showMenu }
                                                    ) {
                                                        nowPageNowPlaying.value = it
                                                    }
                                                }
                                            }
                                        }

                                        // iOS 26 / BitChord Floating Liquid Glass MiniPlayer (Android 13+ Liquid Glass, Android < 13 normal bar)
                                        YosWrapper {
                                            if (yosBottomSheetConfig.showMenu) {
                                                Column(
                                                    Modifier
                                                        .fillMaxSize()
                                                        .graphicsLayer {
                                                            compositingStrategy =
                                                                CompositingStrategy.ModulateAlpha
                                                            this.alpha =
                                                                yosBottomSheetConfig.menuAlpha
                                                        }
                                                ) {
                                                    LyricsFlowFloatingMiniPlayer(
                                                        hazeState = hazeState,
                                                        onExpandClick = {
                                                            scope.launch {
                                                                offsetY.animateTo(
                                                                    parentHeight.intValue.toFloat(),
                                                                    animationSpec = navSpec
                                                                )
                                                            }
                                                        }
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        // 底部导航栏 (iOS 26 / BitChord Liquid Glass Floating Tab Bar on Android 13+, normal bar on Android < 13)
                        if (yosBottomSheetConfig.menuAlpha > 0.01f) {
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        alpha = yosBottomSheetConfig.menuAlpha
                                        translationY = height.intValue * yosBottomSheetConfig.progress
                                    },
                                contentAlignment = Alignment.BottomCenter
                            ) {
                                LyricsFlowFloatingTabBar(
                                    selectedIndex = pagerState.currentPage,
                                    onTabSelected = { selectedIdx ->
                                        nowLabel.value = when (selectedIdx) {
                                            0 -> "Home"
                                            1 -> "New"
                                            2 -> "Radio"
                                            3 -> "Library"
                                            4 -> "Search"
                                            else -> "Home"
                                        }
                                        if (route.value != UI.HomePage) {
                                            navController.popBackStack(
                                                UI.HomePage,
                                                false
                                            )
                                        }
                                        scope.launch {
                                            pagerState.scrollToPage(selectedIdx)
                                        }
                                    },
                                    hazeState = hazeState,
                                    modifier = Modifier.onSizeChanged {
                                        height.intValue = it.height
                                    }
                                )
                            }
                        }

                        // Activity-level host for the playlist
                        // delete-undo snackbar.
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.BottomCenter,
                        ) {
                            com.lyricsflow.player.ui.pages.library.playlists.UndoSnackbarHost(
                                bottomOffset = miniPlayerHeight,
                            )
                        }
                        }
                    /*}*/
                }
            }
    }
}

@Composable
private fun rememberBluetoothAudioConnected(): Boolean
{
    val context = LocalContext.current
    val bluetoothAdapter = remember { BluetoothAdapter.getDefaultAdapter() }
    val bluetoothAudioConnected = remember("MainActivity_bluetoothAudioConnected") {
        mutableStateOf(false)
    }

    DisposableEffect(context, bluetoothAdapter) {
        val refreshBluetoothAudioConnected = {
            bluetoothAudioConnected.value = if (ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.BLUETOOTH_CONNECT,
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                false
            } else {
                bluetoothAdapter
                    ?.bondedDevices
                    ?.any { device ->
                        device.bluetoothClass.majorDeviceClass == BluetoothClass.Device.Major.AUDIO_VIDEO &&
                            device.isConnected()
                    } == true
            }
        }
        val receiver = object : BroadcastReceiver()
        {
            override fun onReceive(context: Context?, intent: Intent?)
            {
                val action = intent?.action
                if (action == BluetoothDevice.ACTION_ACL_CONNECTED ||
                    action == BluetoothDevice.ACTION_ACL_DISCONNECTED ||
                    action == "com.lyricsflow.player.BLUETOOTH_STATUS_REFRESH") {
                    refreshBluetoothAudioConnected()
                }
            }
        }
        val filter = IntentFilter(BluetoothDevice.ACTION_ACL_CONNECTED).apply {
            addAction(BluetoothDevice.ACTION_ACL_DISCONNECTED)
            addAction("com.lyricsflow.player.BLUETOOTH_STATUS_REFRESH")
        }

        ContextCompat.registerReceiver(
            context,
            receiver,
            filter,
            ContextCompat.RECEIVER_EXPORTED,
        )
        refreshBluetoothAudioConnected()

        onDispose {
            runCatching {
                context.unregisterReceiver(receiver)
            }
        }
    }

    return bluetoothAudioConnected.value
}

private fun BluetoothDevice.isConnected(): Boolean
{
    return runCatching {
        val isConnectedMethod = BluetoothDevice::class.java.getMethod("isConnected")
        isConnectedMethod.isAccessible = true
        isConnectedMethod.invoke(this) as Boolean
    }.getOrDefault(false)
}

        @Composable
    fun CheckAndRequestPermission() {
        val context = LocalContext.current
        val requestPermissionLauncher = rememberLauncherForActivityResult(
            ActivityResultContracts.RequestMultiplePermissions()
        ) { permissions ->
            val audioPermissionGranted = MusicLibrary.hasAudioPermission(context)
            if (audioPermissionGranted) {
                loadMusic(context, enforce = true)
            }

            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.S || permissions[Manifest.permission.BLUETOOTH_CONNECT] != false) {
                sendBroadcast(Intent("com.lyricsflow.player.BLUETOOTH_STATUS_REFRESH"))
            }
        }

        YosWrapper {
            LaunchedEffect(Unit) {

                var permissions = emptyArray<String>()

                if (!MusicLibrary.hasAudioPermission(context)) {
                    permissions += if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        Manifest.permission.READ_MEDIA_AUDIO
                    } else {
                        Manifest.permission.READ_EXTERNAL_STORAGE
                    }
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        permissions += Manifest.permission.BLUETOOTH_CONNECT
                }

                if (ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                    permissions += Manifest.permission.POST_NOTIFICATIONS
                }

                if (permissions.isNotEmpty()) {
                    requestPermissionLauncher.launch(permissions)
                }
                else {
                    loadMusic(context)
                }
            }
        }
    }


    /*LaunchedEffect(permissionState.value) {
            val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_MEDIA_AUDIO
                ) == PackageManager.PERMISSION_GRANTED
            } else {
                ContextCompat.checkSelfPermission(
                    context,
                    Manifest.permission.READ_EXTERNAL_STORAGE
                ) == PackageManager.PERMISSION_GRANTED
            }

            if (hasPermission) {
                permissionState.value = PermissionState.DENIED
                loadMusic(context)
            }
        }

        val showDialog = remember("CheckPermission_showDialog") {
            derivedStateOf { permissionState.value != PermissionState.DENIED  }
        }

        if (showDialog.value) {
            val dialogProperties = ModalBottomSheetProperties(
                securePolicy = SecureFlagPolicy.Inherit,
                isFocusable = true,
                shouldDismissOnBackPress = false
            )

            val bottomSheetState = rememberModalBottomSheetState()
            val scope = rememberCoroutineScope()

            YosWrapper {
                OptionDialog(
                    title = stringResource(id = R.string.permission_grant_title),
                    subTitle = stringResource(id = R.string.permission_grant_subtitle),
                    content = {
                        Text(text = stringResource(id = R.string.permission_grant_desc))
                    },
                    positiveContent = stringResource(id = R.string.permission_grant_button_positive),
                    properties = dialogProperties,
                    bottomSheetState = bottomSheetState,
                    onPositive = {
                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                                    requestPermissionLauncher.launch(Manifest.permission.READ_MEDIA_AUDIO)
                                } else {
                                    requestPermissionLauncher.launch(Manifest.permission.READ_EXTERNAL_STORAGE)
                                }
                                permissionState.value = PermissionState.DENIED
                            }
                        }
                    },
                    negativeContent = stringResource(id = R.string.permission_grant_button_negative),
                    onNegative = {
                        scope.launch { bottomSheetState.hide() }.invokeOnCompletion {
                            if (!bottomSheetState.isVisible) {
                                mainMusicList.value = mutableListOf()
                                permissionState.value = PermissionState.DENIED
                            }
                        }
                    }
                ) {
                    mainMusicList.value = mutableListOf()
                    permissionState.value = PermissionState.DENIED
                }
            }
        }*/

    /*enum class PermissionState {
        UNKNOWN,
        GRANTED,
        DENIED
    }*/

    private fun loadMusic(context: Context, enforce: Boolean = false) {
        if (!MusicLibrary.hasAudioPermission(context)) {
            return
        }

        val needRefresh = SettingsLibrary.RefreshEveryTime
        if (needRefresh || enforce) {
            mediaViewModel.viewModelScope.launch(Dispatchers.IO) {
                // Application中已还原，这里算是后台扫描
                // mainMusicList.value = MusicScanner(context).getMusicList()
                MusicLibrary.scanMedia(context)
            }
        }
    }

}

fun readFile(path: String): String? {
    return try {
        File(path).readText()
    } catch (e: Exception) {
        //e.printStackTrace()
        null
    }
}
