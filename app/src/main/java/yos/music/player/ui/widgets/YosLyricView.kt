package com.lyricsflow.player.ui.widgets

import android.os.Build
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.AnimationSpec
import androidx.compose.animation.core.CubicBezierEasing
import androidx.compose.animation.core.EaseInOutQuad
import androidx.compose.animation.core.Easing
import androidx.compose.animation.core.SnapSpec
import androidx.compose.animation.core.TweenSpec
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.withFrameNanos
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.BlurredEdgeTreatment
import androidx.compose.ui.draw.CacheDrawScope
import androidx.compose.ui.draw.DrawResult
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.layout.SubcomposeLayout
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextLayoutResult
import androidx.compose.ui.text.TextMeasurer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.text.style.LineBreak
import androidx.compose.ui.text.style.LineHeightStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Constraints
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.util.fastForEach
import com.lyricsflow.player.code.utils.lrc.AmlAnimationCurves
import com.lyricsflow.player.code.utils.lrc.AmlBackgroundLine
import com.lyricsflow.player.code.utils.lrc.AmlSpring
import com.lyricsflow.player.code.utils.lrc.AmlSyllable
import com.lyricsflow.player.code.utils.lrc.YosMediaEvent
import com.lyricsflow.player.code.utils.lrc.YosUIConfig
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.objects.MainViewModelObject
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.ui.theme.SFArabic
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.widgets.basic.YosWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val yosEasing = CubicBezierEasing(0.28f, 0.11f, 0.22f, 1.0f)

/**
 * LyricsFlow & yourLyrics Apple Music Lyric View (`YosLyricView`)
 * Direct port of `main/js/lyrics-animator.js`, `main/js/lyrics-applyer.js`, `main/js/spring.js`, and `main/js/is-rtl.js`:
 * - Uses `SFArabic` (`sfarabic.ttf`) for RTL (Arabic/Hebrew/Persian) lines and `SFProDisplay` for LTR/CJK lines.
 * - Moves whole words up (`-3px` harmonic spring `wordRiseOffsetPx`) rather than individual letters.
 * - Strictly disables character emphasis / long-timed word splitting for RTL words so Arabic cursive script is never broken.
 * - Drives line scrolling with per-line analytical harmonic oscillator `AmlSpring` (`_posYSpring`) + tempo-adaptive stiffness/damping (`getPosYSpringPolicy`) + `50ms` (`0.05s`) per-visible-line cascade wave stagger.
 */
@Composable
fun YosLyricView(
    lrcEntriesLambda: () -> List<List<Pair<Float, String>>>,
    lineEndTimesLambda: () -> List<Float> = { emptyList() },
    lineTransliterationsLambda: () -> List<String?> = { emptyList() },
    lineSubtitlesLambda: () -> List<String?> = { emptyList() },
    isTtmlLyricsLambda: () -> Boolean = { false },
    liveTimeLambda: () -> Int,
    mediaEvent: YosMediaEvent,
    translationLambda: () -> Boolean = { true },
    blurLambda: () -> Boolean = { false },
    uiConfig: YosUIConfig = YosUIConfig(),
    weightLambda: () -> Boolean,
    modifier: Modifier,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current
    val mainTextBasicColor = Color(uiConfig.mainTextBasicColor)
    val subTextBasicColor = Color(uiConfig.subTextBasicColor)
    val otherSideForLines = MediaViewModelObject.otherSideForLines

    val lrcEntries = lrcEntriesLambda()
    val lineEndTimes = lineEndTimesLambda()
    val lineTransliterations = lineTransliterationsLambda()
    val lineSubtitles = lineSubtitlesLambda()
    val isTtmlLyrics = isTtmlLyricsLambda()
    val lineSyllablesList = MediaViewModelObject.lyricLineSyllables.value
    val lineBackgroundsList = MediaViewModelObject.lyricBackgroundLines.value
    val songwritersList = MediaViewModelObject.lyricSongwriters.value

    if (lrcEntries.isEmpty() || otherSideForLines.isEmpty()) {
        Box(
            contentAlignment = Alignment.Center,
            modifier = Modifier
                .fillMaxHeight(if (weightLambda()) 0.56f else 1f)
                .fillMaxWidth()
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onBackClick()
                }
        ) {
            Text(
                text = uiConfig.noLrcText,
                fontFamily = SFProDisplay,
                fontWeight = FontWeight.SemiBold,
                fontSize = 18.sp,
                color = Color(uiConfig.mainTextBasicColor).copy(alpha = 0.65f)
            )
        }
    } else {
        val scrollState = rememberLazyListState()
        val currentLyricIndex =
            remember("YosLyricView_currentLyricIndex") { MainViewModelObject.syncLyricIndex }
        val ttmlLiveTime = remember("YosLyricView_ttmlLiveTime") {
            mutableIntStateOf(liveTimeLambda())
        }

        val hasWordLevelSync = remember(isTtmlLyrics, lrcEntries, lineSyllablesList) {
            isTtmlLyrics || lineSyllablesList.any { it.size > 1 } || lrcEntries.any { it.size > 2 }
        }

        YosWrapper {
            LaunchedEffect(hasWordLevelSync, lrcEntries) {
                while (true) {
                    ttmlLiveTime.intValue = liveTimeLambda()
                    delay(10L)
                }
            }
        }

        // Port of `getEffectiveStart` + overlapping vocal hold + 380ms early scroll lead-in from `main/js/lyrics-animator.js`
        val lastFocusedScrollIdx = remember(lrcEntries) { mutableIntStateOf(-1) }

        val focusedLyricIndices = remember(
            "YosLyricView_focusedLyricIndices",
            hasWordLevelSync,
            lrcEntries,
            lineEndTimes
        ) {
            derivedStateOf {
                val liveTime = ttmlLiveTime.intValue.toFloat()
                if (!hasWordLevelSync) {
                    return@derivedStateOf listOf(currentLyricIndex.intValue)
                }

                fun getEffectiveStart(index: Int): Float {
                    val lineStart = lrcEntries.getOrNull(index)?.firstOrNull()?.first ?: 0f
                    if (index <= 0) return lineStart
                    val prevStart = lrcEntries.getOrNull(index - 1)?.firstOrNull()?.first ?: 0f
                    val prevEnd = lineEndTimes.getOrNull(index - 1) ?: lineStart
                    val prevEmpty = lrcEntries.getOrNull(index - 1)?.all { it.second.isBlank() } == true
                    if (prevEmpty) {
                        return max(prevStart, lineStart - 1000f)
                    }
                    val gap = lineStart - prevEnd
                    return when {
                        gap >= 1000f -> lineStart - 1000f
                        gap > 0f -> prevEnd
                        else -> lineStart
                    }
                }

                val activeIndices = lrcEntries.mapIndexedNotNull { index, line ->
                    val lineStart = line.firstOrNull()?.first ?: return@mapIndexedNotNull null
                    val lineEnd = lineEndTimes.getOrNull(index)
                        ?: lrcEntries.getOrNull(index + 1)?.firstOrNull()?.first
                        ?: (lineStart + 3000f)
                    val effStart = getEffectiveStart(index)

                    if (liveTime >= effStart && liveTime <= lineEnd.coerceAtLeast(lineStart + 1f)) {
                        index
                    } else {
                        null
                    }
                }.toMutableList()

                // Overlapping line hold from `lyrics-animator.js` lines 1160-1180:
                // When line A ends but an overlapping line B that started during line A is still singing,
                // keep line A in activeIndices so it stays white until B finishes!
                val prevFocused = lastFocusedScrollIdx.intValue
                if (prevFocused in lrcEntries.indices) {
                    val curStart = lrcEntries[prevFocused].firstOrNull()?.first ?: 0f
                    val curEnd = lineEndTimes.getOrNull(prevFocused) ?: curStart
                    var overlappingStillSinging = false
                    for (j in lrcEntries.indices) {
                        if (j != prevFocused) {
                            val otherStart = lrcEntries[j].firstOrNull()?.first ?: continue
                            val otherEnd = lineEndTimes.getOrNull(j) ?: otherStart
                            if (otherStart in curStart..curEnd && liveTime in otherStart..otherEnd) {
                                overlappingStillSinging = true
                                break
                            }
                        }
                    }
                    if (overlappingStillSinging && !activeIndices.contains(prevFocused)) {
                        activeIndices.add(0, prevFocused)
                    }
                }

                activeIndices.ifEmpty {
                    if (currentLyricIndex.intValue >= 0) listOf(currentLyricIndex.intValue) else emptyList()
                }
            }
        }

        // Scroll anchor index with `lyrics-animator.js` 380ms early lead-in for tight gaps (< 1000ms)
        val focusedLyricAnchorIndex = remember(
            "YosLyricView_focusedLyricAnchorIndex",
            focusedLyricIndices,
            lrcEntries,
            lineEndTimes
        ) {
            derivedStateOf {
                val liveTime = ttmlLiveTime.intValue.toFloat()
                val baseIdx = focusedLyricIndices.value.firstOrNull() ?: currentLyricIndex.intValue
                if (!hasWordLevelSync || baseIdx !in lrcEntries.indices) {
                    return@derivedStateOf baseIdx
                }

                val nextIdx = baseIdx + 1
                if (nextIdx in lrcEntries.indices) {
                    val curStart = lrcEntries[baseIdx].firstOrNull()?.first ?: 0f
                    val curEnd = lineEndTimes.getOrNull(baseIdx) ?: curStart
                    val nextStart = lrcEntries[nextIdx].firstOrNull()?.first ?: curEnd
                    val nextIsEmpty = lrcEntries[nextIdx].all { it.second.isBlank() }
                    val gap = nextStart - curEnd

                    if (gap < 1000f && !nextIsEmpty) {
                        val leadThreshold = min(nextStart - 380f, curEnd - 380f)
                        if (liveTime >= leadThreshold) {
                            return@derivedStateOf nextIdx
                        }
                    }
                }
                baseIdx
            }
        }

        val blankSpacer: (LazyListScope.() -> Unit) = {
            item {
                Box(modifier = Modifier.height(uiConfig.blankHeight.dp))
            }
        }

        val enableLyricScroll = remember("YosLyricView_enableLyricScroll") {
            mutableStateOf(true)
        }

        val height = rememberSaveable(key = "YosLyricView_height") { mutableIntStateOf(0) }
        // `lyrics-animator.js` line 327: `containerHeight * 0.24` (mobile top anchor)
        val targetWeight = 0.14f
        val targetOffset = rememberSaveable(height.intValue, key = "YosLyricView_targetOffset") {
            height.intValue * targetWeight
        }

        val measurer = rememberTextMeasurer(cacheSize = 96)
        val supportBlur = rememberSaveable(key = "supportBlur") {
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.S
        }

        // Per-line analytical harmonic oscillator `AmlSpring` (`_posYSpring` from `main/js/lyrics-animator.js`)
        val lineSprings = remember(lrcEntries) { HashMap<Int, AmlSpring>() }
        val lineSpringOffsetsPx = remember(lrcEntries) { mutableStateMapOf<Int, Float>() }
        val springLoopActive = remember(lrcEntries) { mutableStateOf(false) }

        // Frame-accurate spring ticker (`tickLineSprings` from `main/js/lyrics-animator.js`)
        LaunchedEffect(springLoopActive.value, lrcEntries) {
            if (!springLoopActive.value) return@LaunchedEffect
            var lastFrameNs = 0L
            while (springLoopActive.value) {
                withFrameNanos { nowNs ->
                    val dtSecs = if (lastFrameNs == 0L) {
                        0.016f
                    } else {
                        ((nowNs - lastFrameNs) / 1_000_000_000f).coerceIn(0.001f, 0.064f)
                    }
                    lastFrameNs = nowNs

                    var anyMoving = false
                    for ((lineIdx, springObj) in lineSprings) {
                        if (!springObj.arrived()) {
                            val y = springObj.update(dtSecs)
                            lineSpringOffsetsPx[lineIdx] = y
                            anyMoving = true
                        } else if (lineSpringOffsetsPx[lineIdx] != 0f) {
                            lineSpringOffsetsPx[lineIdx] = 0f
                        }
                    }
                    if (!anyMoving) {
                        springLoopActive.value = false
                    }
                }
            }
        }

        val isUserScrolling = remember { mutableStateOf(false) }
        val nestedScrollConnection = remember {
            @Stable
            object : NestedScrollConnection {
                override fun onPreScroll(available: Offset, source: NestedScrollSource): Offset {
                    isUserScrolling.value = true
                    return Offset.Zero
                }

                override suspend fun onPostFling(
                    consumed: Velocity,
                    available: Velocity
                ): Velocity {
                    isUserScrolling.value = false
                    return super.onPostFling(consumed, available)
                }
            }
        }

        YosWrapper {
            LaunchedEffect(isUserScrolling.value) {
                if (isUserScrolling.value) {
                    enableLyricScroll.value = false
                } else {
                    delay(1600)
                    enableLyricScroll.value = true
                }
            }
        }

        // `setLineAnimTargets` from `main/js/lyrics-animator.js` (lines 269-422):
        // Calculates tempo-adaptive spring policy (`getPosYSpringPolicy`) and 50ms (`0.05s`) per-visible-line cascade stagger!
        YosWrapper {
            LaunchedEffect(focusedLyricAnchorIndex.value, translationLambda(), enableLyricScroll.value) {
                try {
                    if (!enableLyricScroll.value) return@LaunchedEffect
                    val anchorIdx = focusedLyricAnchorIndex.value
                    if (anchorIdx < 0 || anchorIdx >= lrcEntries.size) return@LaunchedEffect

                    val prevAnchor = lastFocusedScrollIdx.intValue
                    lastFocusedScrollIdx.intValue = anchorIdx

                    val visibleInfo = scrollState.layoutInfo.visibleItemsInfo
                    val targetListIndex = anchorIdx + 1
                    val targetItemInfo = visibleInfo.find { it.index == targetListIndex }
                    val detectedSeeking = prevAnchor != -1 && abs(anchorIdx - prevAnchor) > 2

                    val intervalMs = if (anchorIdx > 0) {
                        val curStart = lrcEntries[anchorIdx].firstOrNull()?.first ?: 0f
                        val prevStart = lrcEntries[anchorIdx - 1].firstOrNull()?.first ?: 0f
                        (curStart - prevStart).coerceAtLeast(0f)
                    } else {
                        2000f
                    }
                    val isInterlude = lrcEntries[anchorIdx].all { it.second.isBlank() }
                    val springPolicy = AmlAnimationCurves.getPosYSpringPolicy(
                        isSeeking = detectedSeeking,
                        isInterludeActive = isInterlude,
                        intervalMs = intervalMs
                    )

                    if (targetItemInfo != null && !detectedSeeking) {
                        val scrollDeltaPx = (targetItemInfo.offset - targetOffset).toFloat()
                        if (abs(scrollDeltaPx) > 1f) {
                            // Snap the list to the new target item and let each visible line's `AmlSpring`
                            // absorb `+scrollDeltaPx -> 0f` with the authentic 50ms (`0.05s`) cascade stagger!
                            val firstVisibleLineIdx = (scrollState.firstVisibleItemIndex - 1).coerceAtLeast(0)
                            val startCascadeIdx = max(0, min(firstVisibleLineIdx, anchorIdx - 2))
                            val endCascadeIdx = min(lrcEntries.lastIndex, anchorIdx + 12)

                            var delaySecs = 0f
                            for (lineIdx in startCascadeIdx..endCascadeIdx) {
                                val sp = lineSprings.getOrPut(lineIdx) {
                                    AmlSpring(
                                        initialPosition = 0f,
                                        stiffness = springPolicy.stiffness,
                                        damping = springPolicy.damping,
                                        mass = springPolicy.mass
                                    )
                                }
                                val currentSpringY = sp.currentPosition
                                sp.setPosition(currentSpringY + scrollDeltaPx)
                                sp.updateParams(springPolicy, 0f)
                                sp.setTargetPosition(0f, delaySecs)
                                lineSpringOffsetsPx[lineIdx] = currentSpringY + scrollDeltaPx
                                delaySecs += 0.05f // Specs.lineDelay = 0.05s (50ms cascade stagger)
                            }

                            scrollState.scrollToItem(
                                index = targetListIndex.coerceAtLeast(0),
                                scrollOffset = -targetOffset.toInt()
                            )
                            springLoopActive.value = true
                        }
                    } else {
                        scrollState.animateScrollToItem(
                            index = targetListIndex.coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                    }
                } catch (_: Exception) {
                }
            }
        }

        YosWrapper {
            LazyColumn(
                state = scrollState,
                contentPadding = PaddingValues(vertical = 18.dp),
                modifier = modifier
                    .fillMaxSize()
                    .clickable(
                        indication = null,
                        interactionSource = remember { MutableInteractionSource() }
                    ) {
                        onBackClick()
                    }
                    .nestedScroll(nestedScrollConnection)
                    .onSizeChanged {
                        if (height.intValue == 0 && it.height != 0) {
                            height.intValue = it.height
                        }
                    }
            ) {
                blankSpacer()
                itemsIndexed(
                    items = lrcEntries,
                    key = { index, _ -> "lyric_line_$index" }
                ) { index, lines ->
                    val isCurrent = remember(lines, index) {
                        derivedStateOf {
                            focusedLyricIndices.value.contains(index)
                        }
                    }

                    val isTop = remember(lines, index) {
                        derivedStateOf {
                            index == (focusedLyricAnchorIndex.value - 1)
                        }
                    }

                    val showStateAnimation = remember(index) {
                        derivedStateOf {
                            focusedLyricAnchorIndex.value >= 0 && enableLyricScroll.value
                        }
                    }

                    val isLyricEmpty = rememberSaveable(lines) {
                        mutableStateOf(lines.all { it.second.isBlank() })
                    }

                    val lineSyllables = remember(index, lineSyllablesList) {
                        lineSyllablesList.getOrNull(index).orEmpty()
                    }

                    val lineBackgrounds = remember(index, lineBackgroundsList) {
                        lineBackgroundsList.getOrNull(index).orEmpty()
                    }

                    val lineStartMs = remember(index, lines) {
                        lines.firstOrNull()?.first ?: 0f
                    }
                    val lineEndMs = remember(index, lines, lineEndTimes) {
                        lineEndTimes.getOrNull(index)
                            ?: lrcEntries.getOrNull(index + 1)?.firstOrNull()?.first
                            ?: (lineStartMs + 3000f)
                    }
                    val prevLineEndMs = remember(index, lines, lineEndTimes) {
                        if (index <= 0) {
                            0f
                        } else {
                            lineEndTimes.getOrNull(index - 1)
                                ?: lines.firstOrNull()?.first
                                ?: 0f
                        }
                    }

                    val springYOffsetPx = lineSpringOffsetsPx[index] ?: 0f

                    key(index, lines) {
                        val translation = remember(
                            index,
                            lines,
                            isTtmlLyrics,
                            translationLambda(),
                            lineTransliterations,
                            lineSubtitles
                        ) {
                            if (isTtmlLyrics || lineSubtitles.getOrNull(index) != null || lineTransliterations.getOrNull(index) != null) {
                                val secondaryText = if (translationLambda()) {
                                    lineSubtitles.getOrNull(index) ?: lineTransliterations.getOrNull(index)
                                } else {
                                    lineTransliterations.getOrNull(index)
                                }
                                secondaryText?.ifBlank { null }
                            } else {
                                val str = lines.last().second
                                str.ifBlank { null }
                            }
                        }

                        // AMLL / LyricsFlow Blur calculation (`setLineAnimTargets` lines 385-411)
                        val blur = remember(index) {
                            derivedStateOf {
                                if (!showStateAnimation.value ||
                                    focusedLyricIndices.value.contains(index) ||
                                    !blurLambda() ||
                                    !supportBlur
                                ) {
                                    0f
                                } else {
                                    val anchor = focusedLyricAnchorIndex.value
                                    val distance = if (index < anchor) {
                                        abs(anchor - index) + 1
                                    } else {
                                        abs(index - anchor)
                                    }
                                    ((1 + distance) * 0.85f).coerceIn(1.2f, 5.0f)
                                }
                            }
                        }

                        val otherSide = remember(index) {
                            otherSideForLines.getOrElse(index) { false }
                        }

                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .graphicsLayer {
                                    translationY = springYOffsetPx
                                }
                        ) {
                            // Pre-line interlude 3-dot indicator when gap before this line >= 4.0s
                            val gapBeforeLineMs = lineStartMs - prevLineEndMs
                            if (!isLyricEmpty.value && gapBeforeLineMs >= 4000f) {
                                val interludeState = AmlAnimationCurves.computeInterludeDotsState(
                                    positionMs = ttmlLiveTime.intValue.toFloat(),
                                    startTimeMs = prevLineEndMs,
                                    endTimeMs = lineStartMs
                                )
                                AnimatedVisibility(
                                    visible = interludeState.visible,
                                    enter = fadeIn(tween(240)) + expandVertically(tween(280)),
                                    exit = fadeOut(tween(200)) + shrinkVertically(tween(240))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 28.dp, vertical = 10.dp),
                                        contentAlignment = if (otherSide) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        AmlInterludeDotsView(
                                            state = interludeState,
                                            color = mainTextBasicColor
                                        )
                                    }
                                }
                            }

                            YosWrapper {
                                LyricItem(
                                    isCurrentLambda = { isCurrent.value },
                                    isTopLambda = { isTop.value },
                                    mainLyric = lines.dropLast(1),
                                    syllables = lineSyllables,
                                    backgroundLines = lineBackgrounds,
                                    lineStartTimeMs = lineStartMs,
                                    lineEndTimeMs = lineEndMs,
                                    translation = translation,
                                    showTranslation = if (isTtmlLyrics) translation != null else translationLambda(),
                                    subTextSize = uiConfig.subTextSize,
                                    blur = { blur.value },
                                    mainTextBasicColor = mainTextBasicColor,
                                    subTextBasicColor = subTextBasicColor,
                                    otherSide = otherSide,
                                    liveTimeLambda = { ttmlLiveTime.intValue },
                                    measurer = measurer,
                                    isLyricEmpty = { isLyricEmpty.value },
                                    nextTime = {
                                        if (index + 1 > lrcEntries.size - 1) {
                                            lineEndMs
                                        } else {
                                            lrcEntries[index + 1].first().first
                                        }
                                    }
                                ) {
                                    Vibrator.doubleClick(context)
                                    currentLyricIndex.intValue = index
                                    mediaEvent.onSeek(lines.first().first.toInt())
                                }
                            }
                        }
                    }
                }

                // Songwriter credits footer (`renderCredits` from `main/js/lyrics-applyer.js`)
                if (songwritersList.isNotEmpty()) {
                    item(key = "lyricsflow_songwriters_footer") {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 28.dp, vertical = 24.dp)
                        ) {
                            Text(
                                text = "Written by ${songwritersList.joinToString(", ")}",
                                fontFamily = SFProDisplay,
                                fontWeight = FontWeight.Medium,
                                fontSize = 14.sp,
                                lineHeight = 20.sp,
                                color = mainTextBasicColor.copy(alpha = 0.42f)
                            )
                        }
                    }
                }

                blankSpacer()
                item("extra_blank") {
                    Spacer(modifier = Modifier.height(500.dp))
                }
            }
        }

        YosWrapper {
            LaunchedEffect(Unit) {
                try {
                    if (currentLyricIndex.intValue != -1) {
                        return@LaunchedEffect
                    }
                    val liveTime = liveTimeLambda()
                    val nextIndex = lrcEntries.indexOfFirst { line ->
                        line.first().first > liveTime
                    }

                    if (nextIndex != -1 && nextIndex - 1 != currentLyricIndex.intValue) {
                        scrollState.scrollToItem(
                            index = nextIndex.coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                        currentLyricIndex.intValue = nextIndex - 1
                    } else if (nextIndex == -1 && currentLyricIndex.intValue != lrcEntries.size - 1) {
                        scrollState.scrollToItem(
                            index = lrcEntries.size.coerceAtLeast(0),
                            scrollOffset = -targetOffset.toInt()
                        )
                        currentLyricIndex.intValue = lrcEntries.size - 1
                    }
                } catch (_: Exception) {
                }
            }
        }
    }
}

@Composable
fun Float.toDp(): Dp {
    val density = LocalDensity.current
    return (this / density.density).dp
}

@Composable
private fun LazyItemScope.Line(
    lines: List<Pair<Float, String>>,
    style: TextStyle,
    isRtlLine: Boolean,
    measurer: TextMeasurer,
    modifier: Modifier,
    viewAlign: Alignment.Horizontal,
    draw: CacheDrawScope.(Constraints, TextLayoutResult) -> DrawResult
) =
    YosWrapper {
        val styledString = remember(style, lines) {
            buildString {
                lines.forEach { char ->
                    if (char.second.isNotEmpty()) {
                        append(char.second)
                    }
                }
            }
        }

        Column(
            horizontalAlignment = viewAlign,
            modifier = Modifier
                .fillMaxWidth()
                .graphicsLayer {
                    compositingStrategy = CompositingStrategy.ModulateAlpha
                }
        ) {
            SubcomposeLayout(modifier = modifier) { constraints ->
                val measureResult = measurer.measure(
                    text = styledString,
                    style = style,
                    constraints = Constraints(
                        minWidth = 0,
                        maxWidth = constraints.maxWidth
                    ),
                    layoutDirection = if (isRtlLine) LayoutDirection.Rtl else LayoutDirection.Ltr
                )

                val height = (style.lineHeight * measureResult.lineCount)

                val width = runCatching {
                    if (isRtlLine) {
                        constraints.maxWidth.toFloat()
                    } else {
                        (0 until measureResult.lineCount).maxOf {
                            measureResult.getBoundingBox(
                                (measureResult.getLineEnd(it, visibleEnd = true) - 1).coerceAtLeast(0)
                            ).right
                        }
                    }
                }.getOrDefault(constraints.maxWidth.toFloat()).coerceAtLeast(1f)

                val content = subcompose(lines) {
                    Spacer(
                        Modifier
                            .fillMaxSize()
                            .drawWithCache { draw(constraints, measureResult) }
                    )
                }.first()

                val placeable = content.measure(
                    Constraints.fixed(width.roundToInt(), height.roundToPx())
                )

                layout(placeable.width, placeable.height) {
                    placeable.place(0, 0)
                }
            }
        }
    }

val easing: Easing = EaseInOutQuad

/**
 * Represents a whole word (formed by 1 or more syllables joined via `isPartOfWord`)
 * so that the entire word rises together (`wordRiseOffsetPx`) and is never broken into disconnected letters.
 */
@Stable
private data class WordRunUnit(
    val text: String,
    val startCharIndex: Int,
    val endCharIndexExclusive: Int,
    val startTimeMs: Float,
    val endTimeMs: Float,
    val isRtl: Boolean,
    val isEmphasized: Boolean
)

@Composable
@OptIn(ExperimentalFoundationApi::class)
fun LazyItemScope.LyricItem(
    isCurrentLambda: () -> Boolean,
    isTopLambda: () -> Boolean,
    mainLyric: List<Pair<Float, String>>,
    syllables: List<AmlSyllable> = emptyList(),
    backgroundLines: List<AmlBackgroundLine> = emptyList(),
    lineStartTimeMs: Float = 0f,
    lineEndTimeMs: Float = 0f,
    translation: String?,
    showTranslation: Boolean,
    subTextSize: Int,
    blur: () -> Float,
    mainTextBasicColor: Color,
    subTextBasicColor: Color,
    measurer: TextMeasurer,
    isLyricEmpty: () -> Boolean,
    nextTime: () -> Float,
    otherSide: Boolean,
    liveTimeLambda: () -> Int,
    onClick: () -> Unit
) {
    val fullLineString = remember(mainLyric) {
        mainLyric.joinToString("") { it.second }
    }
    val isRtlLine = remember(fullLineString) {
        AmlAnimationCurves.isRtlText(fullLineString)
    }

    // In RTL lines, natural reading alignment is End (Right) unless opposite-aligned duet
    val viewAlign = when {
        otherSide -> Alignment.End
        isRtlLine -> Alignment.End
        else -> Alignment.Start
    }

    val focusedColor = Color(0xFFFFFFFF)
    val unfocusedColor = Color(0x36FFFFFF)
    val unfocusedSolidBrush = SolidColor(unfocusedColor)

    val isNotOneByOne = rememberSaveable(mainLyric, syllables) {
        mutableStateOf(
            syllables.size <= 1 && mainLyric.all { it.first == mainLyric.firstOrNull()?.first }
        )
    }

    val liveTime = remember(mainLyric) { mutableIntStateOf(liveTimeLambda()) }

    YosWrapper {
        val launch = remember(mainLyric, backgroundLines) {
            derivedStateOf {
                isLyricEmpty() || !isNotOneByOne.value || backgroundLines.isNotEmpty()
            }
        }
        if (launch.value) {
            LaunchedEffect(Unit) {
                while (true) {
                    withContext(Dispatchers.Main) {
                        liveTime.intValue = liveTimeLambda()
                    }
                    delay(10L)
                }
            }
        }
    }

    // Build whole-word units (`WordRunUnit`) by grouping consecutive syllables where `isPartOfWord == true`.
    // This guarantees that entire words move up together (`-3px` `wordRiseOffsetPx`) and RTL words are never split!
    val wordRunUnits = remember(mainLyric, syllables, lineStartTimeMs, lineEndTimeMs, isRtlLine) {
        val units = ArrayList<WordRunUnit>()
        var charCursor = 0
        var i = 0
        while (i < mainLyric.size) {
            val startIdx = i
            val startChar = charCursor
            val sb = StringBuilder()
            val firstSyl = syllables.getOrNull(i)
            val wordStartMs = firstSyl?.startTimeMs
                ?: if (i == 0) {
                    mainLyric.first().first
                } else {
                    mainLyric[i - 1].first
                }
            var wordEndMs = firstSyl?.endTimeMs ?: mainLyric[i].first

            while (i < mainLyric.size) {
                val pieceText = mainLyric[i].second
                sb.append(pieceText)
                charCursor += pieceText.length
                val syl = syllables.getOrNull(i)
                wordEndMs = syl?.endTimeMs
                    ?: if (mainLyric[i].first > wordStartMs) {
                        mainLyric[i].first
                    } else {
                        mainLyric.getOrNull(i + 1)?.first
                            ?: lineEndTimeMs.coerceAtLeast(wordStartMs + 350f)
                    }
                val continuesWord = syl?.isPartOfWord == true && !pieceText.endsWith(" ")
                i++
                if (!continuesWord) break
            }

            val wordStr = sb.toString()
            if (wordStr.isNotEmpty()) {
                val wordIsRtl = isRtlLine || AmlAnimationCurves.isRtlText(wordStr)
                val wordDur = (wordEndMs - wordStartMs).coerceAtLeast(1f)
                // Disable emphasis & long-timed word character animations completely for RTL!
                val wordEmphasized = !wordIsRtl && AmlAnimationCurves.shouldEmphasizeWord(wordStr, wordDur)
                units.add(
                    WordRunUnit(
                        text = wordStr,
                        startCharIndex = startChar,
                        endCharIndexExclusive = charCursor,
                        startTimeMs = wordStartMs,
                        endTimeMs = wordEndMs.coerceAtLeast(wordStartMs + 1f),
                        isRtl = wordIsRtl,
                        isEmphasized = wordEmphasized
                    )
                )
            } else if (i == startIdx) {
                i++
            }
        }
        units
    }

    YosWrapper {
        Column(
            Modifier.padding(horizontal = 9.dp),
            horizontalAlignment = viewAlign
        ) {
            val otherSideAnimate = if (viewAlign == Alignment.End) {
                TransformOrigin(1f, 0.25f)
            } else {
                TransformOrigin(0f, 0.25f)
            }

            val otherSideTransformOrigin = if (viewAlign == Alignment.End) {
                TransformOrigin(1f, 0.5f)
            } else {
                TransformOrigin(0f, 0.5f)
            }

            val tweenSpecWithDelay: AnimationSpec<Float> = remember(mainLyric) {
                TweenSpec(
                    durationMillis = 270,
                    easing = yosEasing,
                    delay = 90
                )
            }

            val tweenSpecWithoutDelay: AnimationSpec<Float> = remember(mainLyric) {
                TweenSpec(durationMillis = 300, easing = yosEasing, delay = 40)
            }

            // `Specs.deselectedScale = 0.98`, active `1.0`
            val scale = animateFloatAsState(
                targetValue = if (isCurrentLambda()) 1.0f else 0.98f,
                animationSpec = if (isCurrentLambda()) tweenSpecWithDelay else tweenSpecWithoutDelay,
                label = "LyricLineScale"
            )

            val cardPadding = if (otherSide) {
                Modifier.padding(start = 26.dp)
            } else {
                Modifier.padding(end = 26.dp)
            }

            if (isLyricEmpty()) {
                Column(Modifier.animateContentSize()) {
                    val startMs = mainLyric.firstOrNull()?.first ?: lineStartTimeMs
                    val endMs = nextTime().coerceAtLeast(startMs + 1f)
                    val interludeState = AmlAnimationCurves.computeInterludeDotsState(
                        positionMs = liveTime.intValue.toFloat(),
                        startTimeMs = startMs,
                        endTimeMs = endMs
                    )
                    val show = remember(mainLyric, interludeState.visible) {
                        derivedStateOf { isLyricEmpty() && isCurrentLambda() && interludeState.visible }
                    }
                    AnimatedVisibility(
                        show.value,
                        enter = fadeIn(
                            animationSpec = TweenSpec(
                                durationMillis = 420,
                                easing = yosEasing
                            )
                        ) + scaleIn(
                            initialScale = 0.85f,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(
                                durationMillis = 420,
                                easing = yosEasing
                            )
                        ),
                        exit = fadeOut() + scaleOut(
                            targetScale = 0.85f,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(
                                durationMillis = 300,
                                easing = yosEasing
                            )
                        )
                    ) {
                        YosWrapper {
                            LyricCard(
                                { scale.value },
                                cardPadding,
                                otherSideTransformOrigin,
                                viewAlign
                            ) {
                                Column(
                                    Modifier
                                        .padding(start = 20.dp, end = 20.dp)
                                        .padding(top = 8.dp, bottom = 10.dp),
                                    horizontalAlignment = viewAlign
                                ) {
                                    AmlInterludeDotsView(
                                        state = interludeState,
                                        color = mainTextBasicColor
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                YosWrapper {
                    LyricCard(
                        { scale.value },
                        cardPadding,
                        otherSideTransformOrigin,
                        viewAlign
                    ) {
                        val blurValue = animateDpAsState(
                            blur().dp,
                            SnapSpec(delay = if (isTopLambda()) 220 else 0),
                            label = "LyricLineBlur"
                        )

                        val blurModifier = remember(mainLyric) {
                            derivedStateOf {
                                val thisBlur = blur()
                                if (thisBlur == 0f) {
                                    Modifier
                                } else {
                                    Modifier.blur(
                                        blurValue.value,
                                        edgeTreatment = BlurredEdgeTreatment.Unbounded
                                    )
                                }
                            }
                        }

                        YosWrapper {
                            Column(
                                Modifier
                                    .then(blurModifier.value)
                                    .fillMaxWidth(),
                                horizontalAlignment = viewAlign
                            ) {
                                val textAlign = when {
                                    otherSide -> TextAlign.End
                                    isRtlLine -> TextAlign.Right
                                    else -> TextAlign.Start
                                }

                                val alphaTweenSpecWithDelay: AnimationSpec<Float> =
                                    remember(mainLyric) {
                                        TweenSpec(
                                            durationMillis = 320,
                                            easing = yosEasing,
                                            delay = 110
                                        )
                                    }

                                val alphaTweenSpecWithoutDelay: AnimationSpec<Float> =
                                    remember(mainLyric) {
                                        TweenSpec(
                                            durationMillis = 320,
                                            easing = yosEasing,
                                            delay = 60
                                        )
                                    }

                                // Background Vocals placed ABOVE the main vocal line (`placement == "above"`)
                                val aboveBgLines = remember(backgroundLines) {
                                    backgroundLines.filter { it.placement == "above" }
                                }
                                aboveBgLines.fastForEach { bgLine ->
                                    AmlBackgroundVocalRow(
                                        bgLine = bgLine,
                                        parentLineActive = isCurrentLambda(),
                                        liveTimeMs = liveTime.intValue.toFloat(),
                                        otherSide = otherSide || isRtlLine,
                                        showTranslation = showTranslation,
                                        subTextBasicColor = subTextBasicColor,
                                        onClick = onClick
                                    )
                                }

                                YosWrapper {
                                    val thisAlphaAnimated = animateFloatAsState(
                                        targetValue = if (isCurrentLambda()) 1f else 0.22f,
                                        animationSpec = if (isCurrentLambda()) alphaTweenSpecWithDelay else alphaTweenSpecWithoutDelay,
                                        label = "LyricLineAlpha"
                                    )

                                    val thisAlpha = remember(mainLyric) {
                                        derivedStateOf {
                                            if (isNotOneByOne.value) {
                                                thisAlphaAnimated.value
                                            } else {
                                                1f
                                            }
                                        }
                                    }

                                    val otherSidePadding = remember(mainLyric) {
                                        derivedStateOf {
                                            if (otherSide) {
                                                Modifier.padding(
                                                    start = 20.dp,
                                                    end = if (mainLyric.lastOrNull()?.second?.endsWith("：") == true) 3.dp else 20.dp
                                                )
                                            } else {
                                                Modifier.padding(start = 20.dp, end = 20.dp)
                                            }
                                        }
                                    }

                                    val showHighLight = remember(mainLyric, translation) {
                                        derivedStateOf {
                                            if (isNotOneByOne.value) {
                                                true
                                            } else {
                                                val highlightIndex = (mainLyric.size - if (translation != null && mainLyric.size >= 3) 3 else 1)
                                                    .coerceIn(0, mainLyric.lastIndex.coerceAtLeast(0))
                                                val endCheck = mainLyric.getOrNull(highlightIndex)?.first ?: lineEndTimeMs
                                                liveTime.intValue >= endCheck
                                            }
                                        }
                                    }

                                    // Select SFArabic for RTL lines and SFProDisplay for LTR/CJK lines
                                    val baseLineTextStyle = if (isRtlLine) MainArabicTextStyle else MainTextStyle
                                    val effectiveLineStyle = baseLineTextStyle.copy(textAlign = textAlign)

                                    Line(
                                        lines = mainLyric,
                                        style = effectiveLineStyle,
                                        isRtlLine = isRtlLine,
                                        measurer = measurer,
                                        modifier = Modifier
                                            .graphicsLayer {
                                                this.alpha = thisAlpha.value
                                                compositingStrategy = CompositingStrategy.ModulateAlpha
                                            }
                                            .padding(vertical = 4.dp)
                                            .then(otherSidePadding.value)
                                            .clickable(
                                                indication = null,
                                                interactionSource = remember { MutableInteractionSource() }
                                            ) {
                                                onClick()
                                            },
                                        viewAlign = viewAlign
                                    ) { _, measureResult ->
                                        if (isNotOneByOne.value) {
                                            return@Line onDrawWithContent {
                                                drawText(
                                                    textLayoutResult = measureResult,
                                                    color = focusedColor
                                                )
                                            }
                                        }

                                        if (!isCurrentLambda()) {
                                            if (showHighLight.value) {
                                                return@Line onDrawWithContent {
                                                    drawText(
                                                        textLayoutResult = measureResult,
                                                        color = focusedColor,
                                                        topLeft = Offset(0f, AmlAnimationCurves.WORD_RISE_PEAK_PX)
                                                    )
                                                }
                                            } else {
                                                return@Line onDrawWithContent {
                                                    drawText(
                                                        textLayoutResult = measureResult,
                                                        color = unfocusedColor
                                                    )
                                                }
                                            }
                                        }

                                        // Active Line Rendering:
                                        // 1. Every normal word (both LTR and RTL) is drawn as a WHOLE WORD (`WordRunUnit`)
                                        //    and rises as a whole word by `wordRiseOffsetPx(elapsed)` (`-3px` peak).
                                        // 2. RTL words NEVER have character emphasis or letter splitting.
                                        // 3. Only LTR words with `isEmphasized == true` (`>= 1000ms` and `2..7` chars)
                                        //    use per-character emphasis (`computeCharEmphasisState`).
                                        val totalChars = fullLineString.length.coerceAtLeast(1)
                                        val nowMs = liveTime.intValue.toFloat()
                                        val drawPasses = ArrayList<DrawWord>(wordRunUnits.size * 2)

                                        wordRunUnits.fastForEach { unit ->
                                            val wordText = unit.text
                                            if (wordText.isEmpty()) return@fastForEach

                                            val wordStartMs = unit.startTimeMs
                                            val wordEndMs = unit.endTimeMs
                                            val wordDurationMs = (wordEndMs - wordStartMs).coerceAtLeast(1f)
                                            val wordElapsedMs = nowMs - wordStartMs
                                            val wholeWordRisePx = AmlAnimationCurves.wordRiseOffsetPx(wordElapsedMs)

                                            if (!unit.isEmphasized || unit.isRtl) {
                                                // Whole-word rendering: the ENTIRE WORD moves up together!
                                                val firstNonSpaceOffset = wordText.indexOfFirst { !it.isWhitespace() }
                                                    .let { if (it < 0) 0 else it }
                                                val lastNonSpaceOffset = wordText.indexOfLast { !it.isWhitespace() }
                                                    .let { if (it < 0) wordText.lastIndex else it }

                                                val firstCharIdx = (unit.startCharIndex + firstNonSpaceOffset).coerceIn(0, totalChars - 1)
                                                val lastCharIdx = (unit.startCharIndex + lastNonSpaceOffset).coerceIn(0, totalChars - 1)

                                                val firstBox = measureResult.getBoundingBox(firstCharIdx)
                                                val lastBox = measureResult.getBoundingBox(lastCharIdx)
                                                val boxLeft = min(firstBox.left, lastBox.left)
                                                val boxTop = min(firstBox.top, lastBox.top)

                                                val trimmedWord = wordText.substring(firstNonSpaceOffset, lastNonSpaceOffset + 1)
                                                if (trimmedWord.isEmpty()) return@fastForEach

                                                val wordLayout = measurer.measure(
                                                    text = trimmedWord,
                                                    style = effectiveLineStyle,
                                                    constraints = measureResult.layoutInput.constraints,
                                                    layoutDirection = if (unit.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
                                                )

                                                val topLeftOffset = Offset(
                                                    x = boxLeft,
                                                    y = boxTop + wholeWordRisePx
                                                )

                                                drawPasses += DrawWord(
                                                    time = wordEndMs,
                                                    word = trimmedWord,
                                                    layout = wordLayout,
                                                    topLeft = topLeftOffset,
                                                    scale = 1f,
                                                    glowAlpha = 0f,
                                                    brush = { feather, percent ->
                                                        if (unit.isRtl) {
                                                            // Right-to-left continuous gradient wipe for RTL words
                                                            val rtlEdge = 1f - percent
                                                            when {
                                                                percent <= 0f -> unfocusedSolidBrush
                                                                percent >= 1f -> SolidColor(focusedColor)
                                                                else -> Brush.horizontalGradient(
                                                                    0f to unfocusedColor,
                                                                    (rtlEdge - feather).coerceIn(0f, 1f) to unfocusedColor,
                                                                    (rtlEdge + feather).coerceIn(0f, 1f) to focusedColor,
                                                                    1f to focusedColor
                                                                )
                                                            }
                                                        } else {
                                                            // Left-to-right continuous gradient wipe for LTR words
                                                            when {
                                                                percent <= 0f -> unfocusedSolidBrush
                                                                percent >= 1f -> SolidColor(focusedColor)
                                                                else -> Brush.horizontalGradient(
                                                                    0f to focusedColor,
                                                                    (percent - feather).coerceIn(0f, 1f) to focusedColor,
                                                                    (percent + feather).coerceIn(0f, 1f) to unfocusedColor,
                                                                    1f to unfocusedColor
                                                                )
                                                            }
                                                        }
                                                    },
                                                    percent = {
                                                        ((liveTime.intValue - wordStartMs) / wordDurationMs)
                                                    }
                                                )
                                            } else {
                                                // Emphasized LTR word (`>= 1000ms` and `2..7` chars): yourLyrics character bloom & lift
                                                val trimmedLen = wordText.trim().length.coerceAtLeast(1)
                                                val charDur = wordDurationMs / wordText.length.coerceAtLeast(1)
                                                var nonSpaceIdx = 0

                                                wordText.forEachIndexed { charOffset, ch ->
                                                    if (ch.isWhitespace()) return@forEachIndexed
                                                    val chStr = ch.toString()
                                                    val globalCharIdx = (unit.startCharIndex + charOffset).coerceIn(0, totalChars - 1)
                                                    val baseBox = measureResult.getBoundingBox(globalCharIdx)

                                                    val empState = AmlAnimationCurves.computeCharEmphasisState(
                                                        wordText = wordText.trim(),
                                                        wordStartTimeMs = wordStartMs,
                                                        wordEndTimeMs = wordEndMs,
                                                        currentPositionMs = nowMs,
                                                        charIndex = nonSpaceIdx,
                                                        charTotal = trimmedLen
                                                    )
                                                    nonSpaceIdx++

                                                    val chLayout = measurer.measure(
                                                        text = chStr,
                                                        style = effectiveLineStyle,
                                                        constraints = measureResult.layoutInput.constraints
                                                    )

                                                    val dxPx = empState.dxEm * effectiveLineStyle.fontSize.toPx()
                                                    val dyPx = empState.dyPx
                                                    val charStartTime = wordStartMs + charOffset * charDur

                                                    drawPasses += DrawWord(
                                                        time = charStartTime + charDur,
                                                        word = chStr,
                                                        layout = chLayout,
                                                        topLeft = Offset(
                                                            x = baseBox.topLeft.x + dxPx,
                                                            y = baseBox.topLeft.y + dyPx
                                                        ),
                                                        scale = empState.scale,
                                                        glowAlpha = empState.glowAlpha,
                                                        brush = { feather, percent ->
                                                            when {
                                                                percent <= 0f -> unfocusedSolidBrush
                                                                percent >= 1f -> SolidColor(focusedColor)
                                                                else -> Brush.horizontalGradient(
                                                                    0f to focusedColor,
                                                                    (percent - feather).coerceIn(0f, 1f) to focusedColor,
                                                                    (percent + feather).coerceIn(0f, 1f) to unfocusedColor,
                                                                    1f to unfocusedColor
                                                                )
                                                            }
                                                        },
                                                        percent = {
                                                            (liveTime.intValue - charStartTime) / charDur.coerceAtLeast(1f)
                                                        }
                                                    )
                                                }
                                            }
                                        }

                                        onDrawBehind {
                                            drawPasses.fastForEach { l ->
                                                val pct = l.percent()
                                                val pivot = Offset(
                                                    x = l.topLeft.x + l.layout.size.width * 0.5f,
                                                    y = l.topLeft.y + l.layout.size.height * 0.5f
                                                )
                                                withTransform({
                                                    if (l.scale != 1f) {
                                                        scale(l.scale, l.scale, pivot)
                                                    }
                                                }) {
                                                    if (l.glowAlpha > 0.01f) {
                                                        drawText(
                                                            textLayoutResult = l.layout,
                                                            topLeft = l.topLeft,
                                                            color = Color.White.copy(alpha = (l.glowAlpha * 0.55f).coerceIn(0f, 0.45f)),
                                                            shadow = Shadow(
                                                                color = Color.White.copy(alpha = l.glowAlpha.coerceIn(0f, 0.7f)),
                                                                offset = Offset.Zero,
                                                                blurRadius = 18f
                                                            )
                                                        )
                                                    }
                                                    drawText(
                                                        textLayoutResult = l.layout,
                                                        topLeft = l.topLeft,
                                                        brush = l.brush(0.24f, pct)
                                                    )
                                                }
                                            }
                                        }
                                    }
                                }

                                // Secondary line: Translation or Romanization/Transliteration (uses SFArabic if RTL)
                                YosWrapper {
                                    AnimatedVisibility(showTranslation && translation != null) {
                                        translation?.let { subStr ->
                                            val isSubRtl = remember(subStr) {
                                                AmlAnimationCurves.isRtlText(subStr)
                                            }
                                            val translationAlpha = animateFloatAsState(
                                                targetValue = if (isCurrentLambda()) 0.58f else 0.16f,
                                                animationSpec = if (isCurrentLambda()) alphaTweenSpecWithDelay else alphaTweenSpecWithoutDelay,
                                                label = "TranslationAlpha"
                                            )

                                            val translationOtherSidePadding = Modifier.padding(
                                                start = 20.dp,
                                                end = 20.dp
                                            )

                                            Text(
                                                text = subStr,
                                                fontFamily = if (isSubRtl) SFArabic else SFProDisplay,
                                                fontSize = subTextSize.sp,
                                                color = subTextBasicColor,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .graphicsLayer {
                                                        this.alpha = translationAlpha.value
                                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                                    }
                                                    .then(translationOtherSidePadding)
                                                    .padding(top = 4.dp),
                                                lineHeight = (subTextSize + 5).sp,
                                                letterSpacing = if (isSubRtl) 0.sp else 0.2.sp,
                                                textAlign = textAlign
                                            )
                                        }
                                    }
                                }

                                // Background Vocals placed BELOW the main vocal line (`placement != "above"`)
                                val belowBgLines = remember(backgroundLines) {
                                    backgroundLines.filter { it.placement != "above" }
                                }
                                belowBgLines.fastForEach { bgLine ->
                                    AmlBackgroundVocalRow(
                                        bgLine = bgLine,
                                        parentLineActive = isCurrentLambda(),
                                        liveTimeMs = liveTime.intValue.toFloat(),
                                        otherSide = otherSide || isRtlLine,
                                        showTranslation = showTranslation,
                                        subTextBasicColor = subTextBasicColor,
                                        onClick = onClick
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

/**
 * Animated Background Vocal line (`ttm:role="x-bg"` or extracted parenthesized background vocal).
 * Port of `advanceBgSlide` and `.background-vocal-wrapper` from `main/js/lyrics-animator.js`.
 */
@Composable
private fun AmlBackgroundVocalRow(
    bgLine: AmlBackgroundLine,
    parentLineActive: Boolean,
    liveTimeMs: Float,
    otherSide: Boolean,
    showTranslation: Boolean,
    subTextBasicColor: Color,
    onClick: () -> Unit
) {
    val isBgRtl = remember(bgLine.text) {
        AmlAnimationCurves.isRtlText(bgLine.text)
    }
    val isBgActive = parentLineActive ||
        (liveTimeMs >= bgLine.startTimeMs - 300f && liveTimeMs <= bgLine.endTimeMs + 350f)

    val slideScale = animateFloatAsState(
        targetValue = if (isBgActive) 1.0f else 0.86f,
        animationSpec = spring(stiffness = 120f, dampingRatio = 0.82f),
        label = "BgVocalScale"
    )
    val slideAlpha = animateFloatAsState(
        targetValue = if (isBgActive) 0.78f else 0.18f,
        animationSpec = tween(durationMillis = 260, easing = yosEasing),
        label = "BgVocalAlpha"
    )

    val alignRight = otherSide || isBgRtl
    val textAlign = if (alignRight) TextAlign.End else TextAlign.Start
    val rowAlign = if (alignRight) Alignment.End else Alignment.Start

    AnimatedVisibility(
        visible = parentLineActive || isBgActive,
        enter = expandVertically(tween(260, easing = yosEasing)) + fadeIn(tween(240)),
        exit = shrinkVertically(tween(240, easing = yosEasing)) + fadeOut(tween(200))
    ) {
        Column(
            horizontalAlignment = rowAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 2.dp)
                .graphicsLayer {
                    scaleX = slideScale.value
                    scaleY = slideScale.value
                    alpha = slideAlpha.value
                    transformOrigin = if (alignRight) TransformOrigin(1f, 0f) else TransformOrigin(0f, 0f)
                }
                .clickable(
                    indication = null,
                    interactionSource = remember { MutableInteractionSource() }
                ) {
                    onClick()
                }
        ) {
            val bgProgress = ((liveTimeMs - bgLine.startTimeMs) /
                (bgLine.endTimeMs - bgLine.startTimeMs).coerceAtLeast(1f)).coerceIn(0f, 1f)
            val liftY = -2.5f * (1f - (1f - bgProgress) * (1f - bgProgress) * (1f - bgProgress))
            val baseBgStyle = if (isBgRtl) BackgroundArabicTextStyle else BackgroundVocalTextStyle

            val bgBrush = if (isBgRtl) {
                val rtlEdge = 1f - bgProgress
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.36f),
                    (rtlEdge - 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.36f),
                    (rtlEdge + 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.92f),
                    1f to Color.White.copy(alpha = 0.92f)
                )
            } else {
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.92f),
                    (bgProgress - 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.92f),
                    (bgProgress + 0.12f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.36f),
                    1f to Color.White.copy(alpha = 0.36f)
                )
            }

            Text(
                text = bgLine.text,
                style = baseBgStyle.copy(
                    textAlign = textAlign,
                    brush = bgBrush
                ),
                modifier = Modifier.graphicsLayer {
                    translationY = liftY
                }
            )

            val bgSecondary = if (showTranslation) {
                bgLine.translation ?: bgLine.transliteration
            } else {
                bgLine.transliteration
            }
            if (!bgSecondary.isNullOrBlank()) {
                val isSecRtl = AmlAnimationCurves.isRtlText(bgSecondary)
                Text(
                    text = bgSecondary,
                    fontFamily = if (isSecRtl) SFArabic else SFProDisplay,
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp,
                    lineHeight = 18.sp,
                    color = subTextBasicColor.copy(alpha = 0.55f),
                    textAlign = textAlign,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun LyricCard(
    scale: () -> Float,
    cardPadding: Modifier,
    otherSideTransformOrigin: TransformOrigin,
    viewAlign: Alignment.Horizontal,
    content: @Composable () -> Unit,
) =
    YosWrapper {
        Column(
            modifier = Modifier
                .graphicsLayer {
                    val scaleValue = scale()
                    scaleX = scaleValue
                    scaleY = scaleValue
                    transformOrigin = otherSideTransformOrigin
                }
                .fillMaxWidth()
                .then(cardPadding)
                .padding(top = 8.dp, bottom = 8.dp),
            horizontalAlignment = viewAlign
        ) {
            content()
        }
    }

/**
 * Authentic AMLL 3-Dot Interlude Indicator ported from `advanceInterludeDots` in `main/js/lyrics-animator.js`.
 */
@Composable
fun AmlInterludeDotsView(
    state: com.lyricsflow.player.code.utils.lrc.InterludeDotsVisualState,
    color: Color
) {
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = state.groupScale
            scaleY = state.groupScale
            alpha = state.groupAlpha
            transformOrigin = TransformOrigin(0f, 0.5f)
        },
        contentAlignment = Alignment.CenterStart
    ) {
        Row(
            horizontalArrangement = Arrangement.spacedBy(9.dp),
            modifier = Modifier.padding(horizontal = 4.dp, vertical = 4.dp)
        ) {
            val alphas = listOf(
                state.dotAlphas.first,
                state.dotAlphas.second,
                state.dotAlphas.third
            )
            alphas.forEach { dotAlpha ->
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .background(
                            color = color.copy(alpha = dotAlpha.coerceIn(0f, 1f)),
                            shape = CircleShape
                        )
                )
            }
        }
    }
}

@Composable
fun CountdownAnimation(progress: () -> Float, colorLambda: () -> Color) {
    val pct = progress().coerceIn(0f, 1f)
    val state = AmlAnimationCurves.computeInterludeDotsState(
        positionMs = pct * 5000f,
        startTimeMs = 0f,
        endTimeMs = 5000f
    )
    AmlInterludeDotsView(state = state, color = colorLambda())
}

private val ConfiguredLyricFontWeight: FontWeight
    get() = when (SettingsLibrary.LyricFontWeight) {
        "Thin" -> FontWeight.Thin
        "ExtraLight" -> FontWeight.ExtraLight
        "Light" -> FontWeight.Light
        "Regular" -> FontWeight.Normal
        "Medium" -> FontWeight.Medium
        "SemiBold" -> FontWeight.SemiBold
        "Bold" -> FontWeight.Bold
        "ExtraBold" -> FontWeight.ExtraBold
        "Black" -> FontWeight.Black
        else -> FontWeight.Bold
    }

val MainTextStyle = TextStyle(
    fontFamily = SFProDisplay,
    fontSize = 31.sp,
    lineHeight = 40.sp,
    fontWeight = ConfiguredLyricFontWeight,
    letterSpacing = (-0.3).sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    ),
    lineBreak = LineBreak(
        strategy = if (SettingsLibrary.LyricLineBalance) LineBreak.Strategy.Balanced else LineBreak.Strategy.Simple,
        LineBreak.Strictness.Default,
        LineBreak.WordBreak.Default
    )
)

/**
 * Apple SF Arabic lyric style (`sfarabic.ttf`), with zero letter-spacing so Arabic cursive ligatures stay connected.
 */
val MainArabicTextStyle = TextStyle(
    fontFamily = SFArabic,
    fontSize = 31.sp,
    lineHeight = 42.sp,
    fontWeight = ConfiguredLyricFontWeight,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    ),
    lineBreak = LineBreak(
        strategy = if (SettingsLibrary.LyricLineBalance) LineBreak.Strategy.Balanced else LineBreak.Strategy.Simple,
        LineBreak.Strictness.Default,
        LineBreak.WordBreak.Default
    )
)

val BackgroundVocalTextStyle = TextStyle(
    fontFamily = SFProDisplay,
    fontSize = 21.5.sp,
    lineHeight = 28.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = (-0.2).sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

val BackgroundArabicTextStyle = TextStyle(
    fontFamily = SFArabic,
    fontSize = 21.5.sp,
    lineHeight = 30.sp,
    fontWeight = FontWeight.Bold,
    letterSpacing = 0.sp,
    lineHeightStyle = LineHeightStyle(
        alignment = LineHeightStyle.Alignment.Center,
        trim = LineHeightStyle.Trim.None
    )
)

@Stable
private data class DrawWord(
    val time: Float,
    val word: String,
    val layout: TextLayoutResult,
    val topLeft: Offset,
    val scale: Float = 1f,
    val glowAlpha: Float = 0f,
    val brush: (px: Float, percent: Float) -> Brush,
    val percent: () -> Float
)
