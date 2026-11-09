package com.lyricsflow.player.ui.widgets.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeChild
import dev.chrisbanes.haze.materials.ExperimentalHazeMaterialsApi
import dev.chrisbanes.haze.materials.HazeMaterials
import kotlinx.coroutines.launch
import com.lyricsflow.player.R
import com.lyricsflow.player.code.MediaController
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.code.utils.player.FadeExo.fadePause
import com.lyricsflow.player.code.utils.player.FadeExo.fadePlay
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.libraries.YosMediaItem
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.isFlamingoInDarkMode
import com.lyricsflow.player.ui.theme.withNight
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.layerBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.rememberLayerBackdrop
import com.lyricsflow.player.ui.widgets.basic.ImageQuality
import com.lyricsflow.player.ui.widgets.basic.ShadowImageWithCache
import kotlin.math.abs
import kotlin.math.roundToInt

private val AppleMusicRed = Color(0xFFFA233B)
private val BAR_GUTTER = 12.dp
private val PILL_INSET = 4.dp
private val TAB_VERTICAL_PADDING = 6.dp
private val TAB_ICON_SIZE = 24.dp
private val TAB_ICON_LABEL_GAP = 2.dp

data class AppleMusicTabItem(
    val index: Int,
    val label: String,
    val iconType: AppleMusicTabIconType
)

enum class AppleMusicTabIconType {
    HOME,
    NEW_GRID,
    RADIO,
    LIBRARY,
    SEARCH
}

val DefaultAppleMusicTabs = listOf(
    AppleMusicTabItem(0, "Home", AppleMusicTabIconType.HOME),
    AppleMusicTabItem(1, "New", AppleMusicTabIconType.NEW_GRID),
    AppleMusicTabItem(2, "Radio", AppleMusicTabIconType.RADIO),
    AppleMusicTabItem(3, "Library", AppleMusicTabIconType.LIBRARY),
    AppleMusicTabItem(4, "Search", AppleMusicTabIconType.SEARCH)
)

/**
 * Floating Tab Bar ported from BitChord (`GlassNavBar.kt`, `FloatingTabBar.kt`, `FloatingBottomBar.kt`, `GlassSelectionPill.kt`).
 * - On Android 13+ (API 33+): Uses full Liquid Glass (`Modifier.liquidGlass` with AGSL `lens` refraction + interactive lifted `GlassSelectionPill`).
 * - On Android < 13: Automatically falls back to the normal non-liquid-glass bar (`FloatingBottomBar` style with `HazeMaterials.thin` or solid surface + `FlatSelectionPill`).
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun LyricsFlowFloatingTabBar(
    selectedIndex: Int,
    onTabSelected: (Int) -> Unit,
    modifier: Modifier = Modifier,
    hazeState: HazeState? = null,
    tabs: List<AppleMusicTabItem> = DefaultAppleMusicTabs
) {
    val selectedTabIndex = selectedIndex
    val context = LocalContext.current
    val isDark = isFlamingoInDarkMode()
    // Liquid Glass strictly requires Android 13+ (API 33 / TIRAMISU)
    val glassSupported = isGlassSupported() && SettingsLibrary.BarBlurEffect
    val coroutineScope = rememberCoroutineScope()
    val pill = remember { GlassPillMotion(coroutineScope, selectedTabIndex.coerceIn(0, tabs.lastIndex)) }

    val barGlass = rememberLayerBackdrop()
    val tabRow = rememberLayerBackdrop()

    var rowSize by remember { mutableStateOf(IntSize.Zero) }
    var lastHapticTab by remember { mutableIntStateOf(selectedTabIndex) }
    var lastSelectedTabIndex by remember { mutableIntStateOf(selectedTabIndex) }

    val density = LocalDensity.current
    val layoutDirection = LocalLayoutDirection.current
    val isRtl = layoutDirection == LayoutDirection.Rtl
    val tabCount = tabs.size
    val tabWidthPx = if (rowSize.width > 0 && tabCount > 0) {
        rowSize.width.toFloat() / tabCount
    } else {
        0f
    }
    val tabStepPx = tabWidthPx
    pill.stepDp = tabStepPx / density.density

    val currentTabStepPx by rememberUpdatedState(tabStepPx)
    val currentSelectedTabIndex by rememberUpdatedState(selectedTabIndex)
    val currentIsRtl by rememberUpdatedState(isRtl)

    val contentPadding = PaddingValues(PILL_INSET)
    val padLeftPx = with(density) { contentPadding.calculateLeftPadding(layoutDirection).toPx() }
    val padTopPx = with(density) { contentPadding.calculateTopPadding().toPx() }
    val padBottomPx = with(density) { contentPadding.calculateBottomPadding().toPx() }
    val rowHeightPx = rowSize.height.toFloat()
    val restSize = Size(tabWidthPx, rowHeightPx)
    val liftedHeightPx = rowHeightPx + padTopPx + padBottomPx + with(density) { PILL_GROW_HEIGHT.toPx() }
    val liftedSize = if (rowHeightPx > 0f) {
        Size(liftedHeightPx * tabWidthPx / rowHeightPx, liftedHeightPx)
    } else {
        restSize
    }

    val pillCenterInRow: (Float) -> Offset = { position ->
        val x = position * tabStepPx + tabWidthPx / 2f
        Offset(if (isRtl) rowSize.width - x else x, rowHeightPx / 2f)
    }
    val showPill = selectedTabIndex in tabs.indices && tabWidthPx > 0f

    LaunchedEffect(selectedTabIndex) {
        if (selectedTabIndex in tabs.indices) {
            if (lastSelectedTabIndex < 0) {
                pill.snapTo(selectedTabIndex)
            } else {
                pill.animateTo(selectedTabIndex)
            }
        }
        lastSelectedTabIndex = selectedTabIndex
        lastHapticTab = selectedTabIndex
    }

    val barShape = RoundedCornerShape(percent = 50)
    val indicatorColor = if (isDark) {
        Color.White.copy(alpha = 0.12f)
    } else {
        Color.Black.copy(alpha = 0.08f)
    }

    val navBottomPadding = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    Box(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BAR_GUTTER)
            .padding(bottom = (navBottomPadding - 2.dp).coerceAtLeast(8.dp))
    ) {
        CompositionLocalProvider(LocalGlassExport provides barGlass) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(62.dp)
                    .then(
                        if (glassSupported) {
                            Modifier.liquidGlass(
                                shape = barShape,
                                blurRadius = 8.dp,
                                refractionHeight = 20.dp,
                                refractionAmount = 24.dp,
                                surfaceTint = if (isDark) {
                                    Color(0xFF1A1A1A).copy(alpha = 0.45f)
                                } else {
                                    Color(0xFFF6F6FA).copy(alpha = 0.55f)
                                }
                            )
                        } else {
                            // Normal non-liquid-glass bar for Android < 13 (ported from BitChord FloatingBottomBar)
                            Modifier
                                .clip(barShape)
                                .then(
                                    if (SettingsLibrary.BarBlurEffect && hazeState != null) {
                                        Modifier.hazeChild(
                                            state = hazeState,
                                            style = HazeMaterials.thin(
                                                if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                                            )
                                        )
                                    } else {
                                        Modifier
                                            .shadow(elevation = 8.dp, shape = barShape)
                                            .background(
                                                color = if (isDark) Color(0xFF1E1E22).copy(alpha = 0.95f) else Color(0xFFF2F2F7).copy(alpha = 0.95f),
                                                shape = barShape
                                            )
                                    }
                                )
                                .border(
                                    width = 0.5.dp,
                                    color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                                    shape = barShape
                                )
                        }
                    )
                    .padding(contentPadding)
            ) {
                if (showPill && (!glassSupported || pill.isFlat)) {
                    FlatSelectionPill(
                        color = indicatorColor,
                        modifier = Modifier
                            .matchParentSize()
                            .pillPlacement(
                                center = { pillCenterInRow(pill.position) },
                                size = { pill.liveSize(restSize, liftedSize) }
                            )
                    )
                }

                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .onSizeChanged { rowSize = it }
                        .layerBackdrop(tabRow)
                        .pointerInput(Unit) {
                            var totalDrag = 0f
                            var dragStartIndex = currentSelectedTabIndex
                            var direction = 1f
                            var step = 0f
                            var holding = false
                            detectHorizontalDragGestures(
                                onDragStart = {
                                    step = currentTabStepPx
                                    holding = currentSelectedTabIndex >= 0 && step > 0f
                                    if (!holding) return@detectHorizontalDragGestures
                                    direction = if (currentIsRtl) -1f else 1f
                                    totalDrag = 0f
                                    dragStartIndex = currentSelectedTabIndex
                                    pill.startDrag()
                                },
                                onDragCancel = {
                                    if (holding) pill.release(currentSelectedTabIndex)
                                    holding = false
                                },
                                onDragEnd = {
                                    if (!holding) return@detectHorizontalDragGestures
                                    holding = false
                                    val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                        .coerceIn(0f, tabs.lastIndex.toFloat())
                                    val newIndex = absoluteIndexFloat.roundToInt()
                                    pill.release(newIndex)
                                    if (newIndex != currentSelectedTabIndex) {
                                        Vibrator.click(context)
                                        onTabSelected(newIndex)
                                    }
                                },
                                onHorizontalDrag = { _, delta ->
                                    if (!holding) return@detectHorizontalDragGestures
                                    totalDrag += delta * direction
                                    val absoluteIndexFloat = (dragStartIndex + totalDrag / step)
                                        .coerceIn(0f, tabs.lastIndex.toFloat())
                                    pill.dragTo(absoluteIndexFloat)
                                    val approximateTab = absoluteIndexFloat.roundToInt()
                                    if (approximateTab != lastHapticTab) {
                                        Vibrator.click(context)
                                        lastHapticTab = approximateTab
                                    }
                                }
                            )
                        },
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    tabs.forEach { tab ->
                        val isSelected = tab.index == selectedTabIndex
                        val iconScale by animateFloatAsState(
                            targetValue = if (isSelected) 1.08f else 1f,
                            animationSpec = spring(
                                dampingRatio = 0.7f,
                                stiffness = 350f
                            ),
                            label = "glassTabScale"
                        )
                        val activeColor = AppleMusicRed
                        val inactiveColor = if (isDark) Color.White.copy(alpha = 0.85f) else Color.Black.copy(alpha = 0.75f)
                        val tintColor = if (isSelected) activeColor else inactiveColor

                        Column(
                            modifier = Modifier
                                .weight(1f)
                                .fillMaxHeight()
                                .clip(RoundedCornerShape(percent = 50))
                                .clickable(
                                    interactionSource = remember { MutableInteractionSource() },
                                    indication = null
                                ) {
                                    if (selectedTabIndex != tab.index) {
                                        Vibrator.click(context)
                                        onTabSelected(tab.index)
                                    }
                                }
                                .padding(vertical = TAB_VERTICAL_PADDING),
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.Center
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(TAB_ICON_SIZE)
                                    .graphicsLayer {
                                        scaleX = iconScale
                                        scaleY = iconScale
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                AppleMusicTabIcon(
                                    type = tab.iconType,
                                    tint = tintColor,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }
                            Spacer(modifier = Modifier.height(TAB_ICON_LABEL_GAP))
                            Text(
                                text = tab.label,
                                fontFamily = SFProDisplay,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                                fontSize = 10.sp,
                                lineHeight = 12.sp,
                                letterSpacing = (-0.1).sp,
                                color = tintColor,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }
            }
        }

        if (glassSupported && showPill && !pill.isFlat) {
            GlassSelectionPill(
                motion = pill,
                fillColor = indicatorColor,
                barGlass = barGlass,
                tabRow = tabRow,
                modifier = Modifier
                    .matchParentSize()
                    .pillPlacement(
                        center = { pillCenterInRow(pill.position) + Offset(padLeftPx, padTopPx) },
                        size = { pill.liveSize(restSize, liftedSize) }
                    )
            )
        }
    }
}

@Composable
private fun AppleMusicTabIcon(
    type: AppleMusicTabIconType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (type) {
        AppleMusicTabIconType.HOME -> {
            Canvas(modifier = modifier.padding(1.5.dp)) {
                val w = size.width
                val h = size.height
                val path = androidx.compose.ui.graphics.Path().apply {
                    moveTo(w * 0.5f, h * 0.10f)
                    lineTo(w * 0.10f, h * 0.44f)
                    lineTo(w * 0.18f, h * 0.44f)
                    lineTo(w * 0.18f, h * 0.88f)
                    lineTo(w * 0.41f, h * 0.88f)
                    lineTo(w * 0.41f, h * 0.58f)
                    lineTo(w * 0.59f, h * 0.58f)
                    lineTo(w * 0.59f, h * 0.88f)
                    lineTo(w * 0.82f, h * 0.88f)
                    lineTo(w * 0.82f, h * 0.44f)
                    lineTo(w * 0.90f, h * 0.44f)
                    close()
                }
                drawPath(path = path, color = tint)
            }
        }
        AppleMusicTabIconType.LIBRARY -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_uitabbar_library),
                contentDescription = "Library",
                tint = tint,
                modifier = modifier
            )
        }
        AppleMusicTabIconType.SEARCH -> {
            Icon(
                painter = painterResource(id = R.drawable.ic_uitabbar_search),
                contentDescription = "Search",
                tint = tint,
                modifier = modifier.padding(1.dp)
            )
        }
        AppleMusicTabIconType.NEW_GRID -> {
            Canvas(modifier = modifier.padding(2.dp)) {
                val gap = size.width * 0.14f
                val cell = (size.width - gap) / 2f
                val radius = CornerRadius(cell * 0.24f, cell * 0.24f)
                drawRoundRect(color = tint, topLeft = Offset(0f, 0f), size = Size(cell, cell), cornerRadius = radius)
                drawRoundRect(color = tint, topLeft = Offset(cell + gap, 0f), size = Size(cell, cell), cornerRadius = radius)
                drawRoundRect(color = tint, topLeft = Offset(0f, cell + gap), size = Size(cell, cell), cornerRadius = radius)
                drawRoundRect(color = tint, topLeft = Offset(cell + gap, cell + gap), size = Size(cell, cell), cornerRadius = radius)
            }
        }
        AppleMusicTabIconType.RADIO -> {
            Canvas(modifier = modifier.padding(1.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                drawCircle(color = tint, radius = size.minDimension * 0.11f, center = center)
                val strokeWidth = size.minDimension * 0.085f
                val innerR = size.minDimension * 0.26f
                val outerR = size.minDimension * 0.44f
                drawArc(
                    color = tint,
                    startAngle = 140f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerR, center.y - innerR),
                    size = Size(innerR * 2, innerR * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -40f,
                    sweepAngle = 80f,
                    useCenter = false,
                    topLeft = Offset(center.x - innerR, center.y - innerR),
                    size = Size(innerR * 2, innerR * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = 135f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2, outerR * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
                drawArc(
                    color = tint,
                    startAngle = -45f,
                    sweepAngle = 90f,
                    useCenter = false,
                    topLeft = Offset(center.x - outerR, center.y - outerR),
                    size = Size(outerR * 2, outerR * 2),
                    style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                )
            }
        }
    }
}

/**
 * Floating Player Bar ported from BitChord (`GlassNavBar.kt` `GlassNowPlaying` + `MiniPlayer.kt`).
 * - On Android 13+ (API 33+): Uses Liquid Glass (`Modifier.liquidGlass`) with press-scale spring (`1.04f`),
 *   horizontal swipe-to-skip, and right-edge fading text overflow.
 * - On Android < 13: Falls back to BitChord's `MiniPlayer` frosted/solid capsule without liquid glass.
 */
@OptIn(ExperimentalHazeMaterialsApi::class)
@Composable
fun LyricsFlowFloatingMiniPlayer(
    hazeState: HazeState? = null,
    onExpandClick: () -> Unit = {},
    track: YosMediaItem? = MediaViewModelObject.nowPlayingMediaItem.value,
    defaultTitle: String = com.lyricsflow.player.data.libraries.defaultTitle,
    isPlaying: Boolean = MediaViewModelObject.isPlaying.value,
    onTogglePlay: () -> Unit = {
        val mc = MediaController.mediaControl
        if (mc != null) {
            if (MediaViewModelObject.isPlaying.value) {
                mc.fadePause()
            } else {
                mc.fadePlay()
            }
        }
    },
    onExpand: () -> Unit = onExpandClick,
    enabledDrag: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val isDark = isFlamingoInDarkMode()
    val glassSupported = isGlassSupported() && SettingsLibrary.BarBlurEffect
    val pillShape = RoundedCornerShape(percent = 50)

    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = if (isPressed) 1.04f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "accessoryPressScale"
    )

    val miniPlayerWidth = remember { mutableIntStateOf(0) }
    val horizontalOffset = remember { Animatable(0f) }
    val actionDirection = remember { mutableIntStateOf(0) }
    val trackKey = track?.mediaId ?: track?.uri?.toString() ?: ""

    val draggableState = rememberDraggableState { delta ->
        if (!enabledDrag || actionDirection.intValue != 0) return@rememberDraggableState
        val maxOffset = (miniPlayerWidth.intValue * 0.35f).coerceAtLeast(1f)
        scope.launch {
            horizontalOffset.snapTo(
                (horizontalOffset.value + delta).coerceIn(-maxOffset, maxOffset)
            )
        }
    }

    LaunchedEffect(trackKey) {
        if (actionDirection.intValue == 0) return@LaunchedEffect
        val widthPx = miniPlayerWidth.intValue.toFloat().coerceAtLeast(1f)
        horizontalOffset.snapTo(-actionDirection.intValue * widthPx)
        horizontalOffset.animateTo(
            0f,
            animationSpec = tween(durationMillis = 65, easing = EaseOutQuart)
        )
        actionDirection.intValue = 0
    }

    val contentColor = Color.Black withNight Color.White
    val secondaryColor = contentColor.copy(alpha = 0.68f)

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = BAR_GUTTER)
            .height(56.dp)
            .graphicsLayer {
                scaleX = pressScale
                scaleY = pressScale
            }
            .then(
                if (glassSupported) {
                    Modifier.liquidGlass(
                        shape = pillShape,
                        blurRadius = 8.dp,
                        refractionHeight = 20.dp,
                        refractionAmount = 24.dp,
                        surfaceTint = if (isDark) {
                            Color(0xFF1A1A1A).copy(alpha = 0.45f)
                        } else {
                            Color(0xFFF6F6FA).copy(alpha = 0.55f)
                        }
                    )
                } else {
                    // Normal non-liquid-glass player bar for Android < 13 (ported from BitChord MiniPlayer.kt)
                    Modifier
                        .clip(pillShape)
                        .then(
                            if (SettingsLibrary.BarBlurEffect && hazeState != null) {
                                Modifier.hazeChild(
                                    state = hazeState,
                                    style = HazeMaterials.thin(
                                        if (isDark) Color(0xFF1C1C1E) else Color(0xFFF2F2F7)
                                    )
                                )
                            } else {
                                Modifier
                                    .shadow(elevation = 8.dp, shape = pillShape)
                                    .background(
                                        color = if (isDark) Color(0xFF202024).copy(alpha = 0.96f) else Color(0xFFF4F4F8).copy(alpha = 0.96f),
                                        shape = pillShape
                                    )
                            }
                        )
                        .border(
                            width = 0.5.dp,
                            color = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f),
                            shape = pillShape
                        )
                }
            )
            .clickable(
                interactionSource = pressSource,
                indication = null,
                onClick = onExpand
            )
            .onSizeChanged { miniPlayerWidth.intValue = it.width }
            .draggable(
                orientation = Orientation.Horizontal,
                state = draggableState,
                enabled = enabledDrag,
                onDragStopped = { velocity ->
                    scope.launch {
                        val widthPx = miniPlayerWidth.intValue.toFloat().coerceAtLeast(1f)
                        val threshold = widthPx * 0.18f
                        val velocityThreshold = 1400f
                        val dragOffset = horizontalOffset.value
                        val shouldTrigger = abs(dragOffset) > threshold || abs(velocity) > velocityThreshold
                        if (!shouldTrigger || track == null) {
                            horizontalOffset.animateTo(0f, tween(55, easing = EaseOutQuart))
                            return@launch
                        }
                        val dir = if (abs(velocity) > velocityThreshold) {
                            if (velocity < 0f) -1 else 1
                        } else if (dragOffset < 0f) -1 else 1
                        val mediaControl = MediaController.mediaControl
                        val canSkip = if (dir < 0) {
                            mediaControl?.hasNextMediaItem() == true
                        } else {
                            mediaControl?.hasPreviousMediaItem() == true
                        }
                        if (!canSkip) {
                            horizontalOffset.animateTo(0f, tween(55, easing = EaseOutQuart))
                            return@launch
                        }
                        Vibrator.click(context)
                        actionDirection.intValue = dir
                        horizontalOffset.animateTo(dir * widthPx, tween(45, easing = EaseOutQuart))
                        if (dir < 0) {
                            mediaControl?.seekToNextMediaItem()
                        } else {
                            mediaControl?.seekToPreviousMediaItem()
                        }
                    }
                }
            )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 12.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ShadowImageWithCache(
                dataLambda = { MediaViewModelObject.bitmap.value },
                contentDescription = null,
                modifier = Modifier
                    .size(40.dp)
                    .border(
                        width = 0.5.dp,
                        color = Color.White.copy(alpha = 0.12f),
                        shape = RoundedCornerShape(8.dp)
                    ),
                cornerRadius = 8.dp,
                shadowAlpha = 0f,
                imageQuality = ImageQuality.LOW
            )

            Spacer(modifier = Modifier.width(10.dp))

            Column(
                modifier = Modifier
                    .weight(1f)
                    .graphicsLayer {
                        val widthPx = miniPlayerWidth.intValue.toFloat().coerceAtLeast(1f)
                        translationX = horizontalOffset.value
                        alpha = 1f - (abs(horizontalOffset.value) / widthPx).coerceIn(0f, 0.35f)
                        compositingStrategy = CompositingStrategy.Offscreen
                    }
                    .drawWithContent {
                        drawContent()
                        val fadeWidth = 20.dp.toPx()
                        if (size.width > fadeWidth * 2) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(Color.Black, Color.Transparent),
                                    startX = size.width - fadeWidth,
                                    endX = size.width
                                ),
                                blendMode = BlendMode.DstIn
                            )
                        }
                    },
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = track?.title ?: defaultTitle,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    lineHeight = 17.sp,
                    letterSpacing = 0.15.sp,
                    maxLines = 1,
                    softWrap = false,
                    overflow = TextOverflow.Clip,
                    color = contentColor
                )
                if (!track?.artist.isNullOrBlank()) {
                    Text(
                        text = track?.artist ?: "",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Normal,
                        fontSize = 12.sp,
                        lineHeight = 15.sp,
                        letterSpacing = 0.25.sp,
                        maxLines = 1,
                        softWrap = false,
                        overflow = TextOverflow.Clip,
                        color = secondaryColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(6.dp))

            IconButton(
                onClick = {
                    Vibrator.click(context)
                    onTogglePlay()
                },
                modifier = Modifier.size(40.dp)
            ) {
                AnimatedContent(
                    targetState = isPlaying,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                            scaleOut(targetScale = 0.3f) + fadeOut()
                        )
                    },
                    label = "miniPlayPause"
                ) { playing ->
                    Icon(
                        painter = painterResource(
                            id = if (playing) R.drawable.ic_nowplaying_mp_pause else R.drawable.ic_nowplaying_mp_play
                        ),
                        contentDescription = if (playing) "Pause" else "Play",
                        modifier = Modifier.size(29.dp),
                        tint = contentColor
                    )
                }
            }

            Spacer(modifier = Modifier.width(4.dp))

            IconButton(
                onClick = {
                    Vibrator.click(context)
                    MediaController.mediaControl?.seekToNextMediaItem()
                },
                modifier = Modifier.size(40.dp)
            ) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_nowplaying_mp_fforward),
                    contentDescription = "Next",
                    modifier = Modifier
                        .size(25.dp)
                        .graphicsLayer { scaleY = 0.88f },
                    tint = contentColor
                )
            }
        }
    }
}
