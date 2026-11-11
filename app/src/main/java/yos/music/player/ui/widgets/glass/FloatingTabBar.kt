package com.lyricsflow.player.ui.widgets.glass

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.EaseOutQuart
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
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
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.addPathNodes
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
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
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem
import com.lyricsflow.player.data.libraries.defaultTitle
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.isLyricsFlowInDarkMode
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
    AppleMusicTabItem(2, "Stats", AppleMusicTabIconType.RADIO),
    AppleMusicTabItem(3, "Library", AppleMusicTabIconType.LIBRARY),
    AppleMusicTabItem(4, "Search", AppleMusicTabIconType.SEARCH)
)

/**
 * Scroll connection ported from BitChord (`FloatingTabBarScrollConnection`)
 * that collapses/shrinks the floating tab bar and miniplayer when scrolling down
 * and expands them when scrolling up.
 */
class FloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    private val scrollThresholdPx: Float
) : NestedScrollConnection {
    var isInline by mutableStateOf(initialIsInline)
        private set

    private var accumulatedScroll = 0f

    fun expand() {
        isInline = false
        accumulatedScroll = 0f
    }

    fun inline() {
        isInline = true
        accumulatedScroll = 0f
    }

    override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
        val scrollDelta = available.y
        if ((accumulatedScroll > 0 && scrollDelta < 0) || (accumulatedScroll < 0 && scrollDelta > 0)) {
            accumulatedScroll = 0f
        }
        accumulatedScroll += scrollDelta
        if (accumulatedScroll <= -scrollThresholdPx && !isInline) {
            isInline = true
            accumulatedScroll = 0f
        } else if (accumulatedScroll >= scrollThresholdPx && isInline) {
            isInline = false
            accumulatedScroll = 0f
        }
        return Offset.Zero
    }
}

@Composable
fun rememberFloatingTabBarScrollConnection(
    initialIsInline: Boolean = false,
    scrollThreshold: Dp = 44.dp
): FloatingTabBarScrollConnection = with(LocalDensity.current) {
    val scrollThresholdPx = scrollThreshold.toPx()
    remember(scrollThresholdPx, initialIsInline) {
        FloatingTabBarScrollConnection(initialIsInline, scrollThresholdPx)
    }
}

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
    scrollConnection: FloatingTabBarScrollConnection? = null,
    tabs: List<AppleMusicTabItem> = DefaultAppleMusicTabs
) {
    val selectedTabIndex = selectedIndex
    val isInline = scrollConnection?.isInline == true
    val inlineScale by animateFloatAsState(
        targetValue = if (isInline) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tabBarInlineScale"
    )
    val inlineAlpha by animateFloatAsState(
        targetValue = if (isInline) 0.88f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "tabBarInlineAlpha"
    )
    val context = LocalContext.current
    val isDark = isLyricsFlowInDarkMode()
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
    val liftedHeightPx = (rowHeightPx * PILL_LIFT_SCALE).coerceAtLeast(
        rowHeightPx + padTopPx + padBottomPx + with(density) { PILL_GROW_HEIGHT.toPx() }
    )
    val liftedSize = if (rowHeightPx > 0f) {
        Size(tabWidthPx * PILL_LIFT_SCALE, liftedHeightPx)
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
            .graphicsLayer {
                scaleX = inlineScale
                scaleY = inlineScale
                alpha = inlineAlpha
            }
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
                        val isHoveredByPill = kotlin.math.abs(pill.position - tab.index) < 0.5f
                        val isSelected = tab.index == selectedTabIndex || isHoveredByPill
                        val iconScale by animateFloatAsState(
                            targetValue = if (isHoveredByPill && !pill.isFlat) 1.15f else if (isSelected) 1.08f else 1f,
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
                                    Vibrator.click(context)
                                    scrollConnection?.expand()
                                    onTabSelected(tab.index)
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

// Exact Apple Music Sidebar SVG icons from `main/index.html` (`am-sidebar-nav`)
private val MainSidebarHomeVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "MainSidebarHome",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M5.93 20.16a1.94 1.94 0 0 1-1.43-.502c-.334-.335-.502-.794-.502-1.393v-7.142c0-.362.062-.688.177-.953.123-.264.326-.529.6-.75l6.145-5.157c.176-.141.344-.247.52-.318q.263-.105.564-.105c.194 0 .388.035.565.105s.352.177.52.318l6.146 5.158c.273.23.467.476.59.75.124.264.177.59.177.96v7.134c0 .59-.159 1.058-.503 1.393q-.503.503-1.428.503zm12.14-1.172q.334 0 .547-.212a.69.69 0 0 0 .22-.511v-7.142c0-.177-.026-.344-.087-.459a1 1 0 0 0-.265-.353l-6.154-5.149a.8.8 0 0 0-.177-.115.4.4 0 0 0-.15-.035.4.4 0 0 0-.158.035l-.177.115-6.145 5.15a1 1 0 0 0-.274.352 1.1 1.1 0 0 0-.088.468v7.133c0 .203.08.379.23.511a.74.74 0 0 0 .546.212zm-8.323-4.7c0-.176.062-.326.177-.432a.6.6 0 0 1 .423-.159h3.315c.176 0 .326.053.432.16s.159.255.159.431v4.973H9.756v-4.973Z"
        ),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.White)
    ).build()
}

private val MainSidebarNewGridVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "MainSidebarNewGrid",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M9.92 11.354c.966 0 1.453-.487 1.453-1.49v-3.4c0-1.004-.487-1.483-1.453-1.483H6.452C5.487 4.981 5 5.46 5 6.464v3.4c0 1.003.487 1.49 1.452 1.49zm7.628 0c.965 0 1.452-.487 1.452-1.49v-3.4c0-1.004-.487-1.483-1.452-1.483h-3.46c-.974 0-1.46.479-1.46 1.483v3.4c0 1.003.486 1.49 1.46 1.49zm-7.65-1.073h-3.43q-.397.002-.396-.418v-3.4q-.002-.41.396-.41h3.43c.265 0 .402.137.402.41v3.4c0 .281-.137.418-.403.418m7.634 0h-3.43c-.273 0-.402-.137-.402-.418v-3.4c0-.273.129-.41.403-.41h3.43q.396 0 .395.41v3.4q.002.42-.396.418m-7.612 8.7c.966 0 1.453-.48 1.453-1.483v-3.407c0-.996-.487-1.483-1.453-1.483H6.452c-.965 0-1.452.487-1.452 1.483v3.407c0 1.004.487 1.483 1.452 1.483zm7.628 0c.965 0 1.452-.48 1.452-1.483v-3.407c0-.996-.487-1.483-1.452-1.483h-3.46c-.974 0-1.46.487-1.46 1.483v3.407c0 1.004.486 1.483 1.46 1.483zm-7.65-1.072h-3.43q-.397 0-.396-.41v-3.4c0-.282.13-.418.396-.418h3.43c.265 0 .402.136.402.418v3.4c0 .273-.137.41-.403.41m7.634 0h-3.43c-.273 0-.402-.137-.402-.41v-3.4c0-.282.129-.418.403-.418h3.43c.265 0 .395.136.395.418v3.4q.002.41-.396.41"
        ),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.White)
    ).build()
}

private val MainSidebarSearchVector: ImageVector by lazy {
    ImageVector.Builder(
        name = "MainSidebarSearch",
        defaultWidth = 24.dp,
        defaultHeight = 24.dp,
        viewportWidth = 24f,
        viewportHeight = 24f
    ).addPath(
        pathData = addPathNodes(
            "M17.979 18.553c.476 0 .813-.366.813-.835a.8.8 0 0 0-.235-.586l-3.45-3.457a5.6 5.6 0 0 0 1.158-3.413c0-3.098-2.535-5.633-5.633-5.633C7.542 4.63 5 7.156 5 10.262c0 3.098 2.534 5.632 5.632 5.632a5.6 5.6 0 0 0 3.274-1.055l3.472 3.472a.84.84 0 0 0 .6.242m-7.347-3.875c-2.417 0-4.416-2-4.416-4.416s2-4.417 4.416-4.417 4.417 2 4.417 4.417-2 4.416-4.417 4.416"
        ),
        pathFillType = PathFillType.EvenOdd,
        fill = SolidColor(Color.White)
    ).build()
}

@Composable
private fun AppleMusicTabIcon(
    type: AppleMusicTabIconType,
    tint: Color,
    modifier: Modifier = Modifier
) {
    when (type) {
        AppleMusicTabIconType.HOME -> {
            Icon(
                imageVector = MainSidebarHomeVector,
                contentDescription = "Home",
                tint = tint,
                modifier = modifier
            )
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
                imageVector = MainSidebarSearchVector,
                contentDescription = "Search",
                tint = tint,
                modifier = modifier
            )
        }
        AppleMusicTabIconType.NEW_GRID -> {
            Icon(
                imageVector = MainSidebarNewGridVector,
                contentDescription = "New",
                tint = tint,
                modifier = modifier
            )
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
    scrollConnection: FloatingTabBarScrollConnection? = null,
    onExpandClick: () -> Unit = {},
    track: LyricsflowMediaItem? = MediaController.musicPlaying.value,
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
    val isDark = isLyricsFlowInDarkMode()
    val glassSupported = isGlassSupported() && SettingsLibrary.BarBlurEffect
    val pillShape = RoundedCornerShape(percent = 50)
    val isInline = scrollConnection?.isInline == true
    val inlineScale by animateFloatAsState(
        targetValue = if (isInline) 0.92f else 1f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessMediumLow),
        label = "miniPlayerInlineScale"
    )

    val pressSource = remember { MutableInteractionSource() }
    val isPressed by pressSource.collectIsPressedAsState()
    val pressScale by animateFloatAsState(
        targetValue = (if (isPressed) 1.04f else 1f) * inlineScale,
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
                if (!track?.artists.isNullOrBlank()) {
                    Text(
                        text = track?.artists ?: "",
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
                val isBuffering = MediaViewModelObject.isBuffering.value
                AnimatedContent(
                    targetState = if (isBuffering) -1 else if (isPlaying) 1 else 0,
                    transitionSpec = {
                        (scaleIn(initialScale = 0.3f) + fadeIn()).togetherWith(
                            scaleOut(targetScale = 0.3f) + fadeOut()
                        )
                    },
                    label = "miniPlayPause"
                ) { state ->
                    if (state == -1) {
                        LyricsFlowAeroSpinner(
                            modifier = Modifier.size(24.dp),
                            color = contentColor
                        )
                    } else {
                        val playing = state == 1
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

/**
 * Exact port of LyricsFlow's 8-spoke Apple/Aero loading spinner (`aero-spinner-svg` in `main/player.html` lines 1870-1881).
 */
@Composable
fun LyricsFlowAeroSpinner(
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    val infiniteTransition = rememberInfiniteTransition(label = "aeroSpinnerTransition")
    val stepFloat by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 800, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "aeroSpinnerStep"
    )
    val activeStep = stepFloat.toInt() % 8
    val opacities = remember {
        floatArrayOf(1.0f, 0.875f, 0.75f, 0.625f, 0.5f, 0.375f, 0.25f, 0.15f)
    }

    Canvas(modifier = modifier) {
        val scale = size.minDimension / 100f
        val rectWidth = 10f * scale
        val rectHeight = 28f * scale
        val rectTop = 6f * scale
        val rectLeft = (size.width - rectWidth) / 2f
        val cornerRadius = CornerRadius(5f * scale, 5f * scale)

        for (i in 0 until 8) {
            val spokeAngle = i * 45f
            val relativeIdx = (activeStep - i + 8) % 8
            val alpha = opacities[relativeIdx]
            rotate(degrees = spokeAngle, pivot = center) {
                drawRoundRect(
                    color = color.copy(alpha = alpha),
                    topLeft = Offset(rectLeft, rectTop),
                    size = Size(rectWidth, rectHeight),
                    cornerRadius = cornerRadius
                )
            }
        }
    }
}
