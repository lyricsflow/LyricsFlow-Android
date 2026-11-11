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
import androidx.compose.ui.text.style.TextGeometricTransform
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
import com.lyricsflow.player.code.utils.lrc.LyricsflowMediaEvent
import com.lyricsflow.player.code.utils.lrc.LyricsflowUIConfig
import com.lyricsflow.player.code.utils.others.Vibrator
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.data.objects.MainViewModelObject
import com.lyricsflow.player.data.objects.MediaViewModelObject
import com.lyricsflow.player.ui.theme.SFArabic
import com.lyricsflow.player.ui.theme.SFProDisplay
import com.lyricsflow.player.ui.theme.sfArabicDynamicFamily
import com.lyricsflow.player.ui.widgets.basic.LyricsflowWrapper
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

val lyricsflowEasing = CubicBezierEasing(0.28f, 0.11f, 0.22f, 1.0f)

/**
 * LyricsFlow & yourLyrics Apple Music Lyric View (`LyricsflowLyricView`)
 * Direct port of `main/js/lyrics-animator.js`, `main/js/lyrics-applyer.js`, `main/js/spring.js`, and `main/js/is-rtl.js`:
 * - Uses `SFArabic` (`sfarabic.ttf`) for RTL (Arabic/Hebrew/Persian) lines and `SFProDisplay` for LTR/CJK lines.
 * - Moves whole words up (`-3px` harmonic spring `wordRiseOffsetPx`) rather than individual letters.
 * - Strictly disables character emphasis / long-timed word splitting for RTL words so Arabic cursive script is never broken.
 * - Drives line scrolling with per-line analytical harmonic oscillator `AmlSpring` (`_posYSpring`) + tempo-adaptive stiffness/damping (`getPosYSpringPolicy`) + `50ms` (`0.05s`) per-visible-line cascade wave stagger.
 */
@Composable
fun LyricsflowLyricView(
    lrcEntriesLambda: () -> List<List<Pair<Float, String>>>,
    lineEndTimesLambda: () -> List<Float> = { emptyList() },
    lineTransliterationsLambda: () -> List<String?> = { emptyList() },
    lineSubtitlesLambda: () -> List<String?> = { emptyList() },
    isTtmlLyricsLambda: () -> Boolean = { false },
    liveTimeLambda: () -> Int,
    mediaEvent: LyricsflowMediaEvent,
    translationLambda: () -> Boolean = { true },
    blurLambda: () -> Boolean = { false },
    uiConfig: LyricsflowUIConfig = LyricsflowUIConfig(),
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
    val lineSyllablesList = MediaViewModelObject.lyricLineSyllables
    val lineBackgroundsList = MediaViewModelObject.lyricBackgroundLines
    val songwritersList = MediaViewModelObject.lyricSongwriters

    if (lrcEntries.isEmpty() || otherSideForLines.isEmpty()) {
        val isLoadingLyrics = MediaViewModelObject.isLoadingLyrics.value || MediaViewModelObject.isBuffering.value
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
            if (isLoadingLyrics) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    com.lyricsflow.player.ui.widgets.glass.LyricsFlowAeroSpinner(
                        modifier = Modifier.size(34.dp),
                        color = Color(uiConfig.mainTextBasicColor).copy(alpha = 0.85f)
                    )
                    Spacer(modifier = Modifier.height(14.dp))
                    Text(
                        text = "Loading synchronized lyrics…",
                        fontFamily = SFProDisplay,
                        fontWeight = FontWeight.Medium,
                        fontSize = 15.sp,
                        color = Color(uiConfig.mainTextBasicColor).copy(alpha = 0.65f)
                    )
                }
            } else {
                Text(
                    text = uiConfig.noLrcText,
                    fontFamily = SFProDisplay,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 18.sp,
                    color = Color(uiConfig.mainTextBasicColor).copy(alpha = 0.65f)
                )
            }
        }
    } else {
        val scrollState = rememberLazyListState()
        val currentLyricIndex =
            remember("LyricsflowLyricView_currentLyricIndex") { MainViewModelObject.syncLyricIndex }
        val ttmlLiveTime = remember("LyricsflowLyricView_ttmlLiveTime") {
            mutableIntStateOf(liveTimeLambda())
        }

        val hasWordLevelSync = remember(isTtmlLyrics, lrcEntries, lineSyllablesList) {
            isTtmlLyrics || lineSyllablesList.any { it.size > 1 } || lrcEntries.any { it.size > 2 }
        }

        LyricsflowWrapper {
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
            "LyricsflowLyricView_focusedLyricIndices",
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
            "LyricsflowLyricView_focusedLyricAnchorIndex",
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

        val enableLyricScroll = remember("LyricsflowLyricView_enableLyricScroll") {
            mutableStateOf(true)
        }

        val height = rememberSaveable(key = "LyricsflowLyricView_height") { mutableIntStateOf(0) }
        // `lyrics-animator.js` line 327: `containerHeight * 0.24` (mobile top anchor)
        val targetWeight = 0.14f
        val targetOffset = rememberSaveable(height.intValue, key = "LyricsflowLyricView_targetOffset") {
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

        LyricsflowWrapper {
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
        LyricsflowWrapper {
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

        LyricsflowWrapper {
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
                        listOfNotNull(lineBackgroundsList.getOrNull(index))
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
                        val rawSubtitle = remember(index, lines, isTtmlLyrics, lineSubtitles) {
                            if (isTtmlLyrics || lineSubtitles.getOrNull(index) != null) {
                                lineSubtitles.getOrNull(index)?.ifBlank { null }
                            } else {
                                lines.last().second.ifBlank { null }
                            }
                        }
                        val rawPronunciation = remember(index, lineTransliterations) {
                            lineTransliterations.getOrNull(index)?.ifBlank { null }
                        }

                        val showTranslationPref = translationLambda() && SettingsLibrary.LyricShowTranslation
                        val showPronunciationPref = SettingsLibrary.LyricShowPronunciation
                        val swapOriginalWithPronunciation = SettingsLibrary.LyricSwapOriginalWithPronunciation && !rawPronunciation.isNullOrBlank()
                        val hideOriginalInPronunciationBar = SettingsLibrary.LyricHideOriginalInPronunciationBar

                        val effectiveMainLyric = remember(lines, rawPronunciation, swapOriginalWithPronunciation, lineStartMs, lineEndMs) {
                            val baseMain = lines.dropLast(1)
                            if (swapOriginalWithPronunciation && !rawPronunciation.isNullOrBlank()) {
                                listOf(lineStartMs to "", lineEndMs to rawPronunciation)
                            } else {
                                baseMain
                            }
                        }
                        val effectiveSyllables = remember(lineSyllables, rawPronunciation, swapOriginalWithPronunciation, lineStartMs, lineEndMs) {
                            if (swapOriginalWithPronunciation && !rawPronunciation.isNullOrBlank()) {
                                if (lineSyllables.isEmpty()) {
                                    emptyList()
                                } else {
                                    val words = rawPronunciation.trim().split(Regex("\\s+")).filter { it.isNotEmpty() }
                                    if (words.isEmpty()) {
                                        emptyList()
                                    } else {
                                        val totalChars = words.sumOf { it.length }.coerceAtLeast(1)
                                        val totalDur = max(lineEndMs - lineStartMs, 200f)
                                        var cursor = lineStartMs
                                        words.mapIndexed { wIdx, w ->
                                            val wDur = totalDur * (w.length.toFloat() / totalChars.toFloat())
                                            val wStart = cursor
                                            val wEnd = if (wIdx == words.lastIndex) max(lineEndMs, wStart + 50f) else (wStart + wDur)
                                            cursor = wEnd
                                            AmlSyllable(
                                                text = w,
                                                startTimeMs = wStart,
                                                endTimeMs = wEnd,
                                                isPartOfWord = false
                                            )
                                        }
                                    }
                                }
                            } else {
                                lineSyllables
                            }
                        }

                        val effectivePronunciationBarText = remember(
                            lines,
                            rawPronunciation,
                            showPronunciationPref,
                            swapOriginalWithPronunciation,
                            hideOriginalInPronunciationBar
                        ) {
                            if (!showPronunciationPref) {
                                null
                            } else if (swapOriginalWithPronunciation) {
                                if (hideOriginalInPronunciationBar) {
                                    null
                                } else {
                                    lines.dropLast(1).joinToString("") { it.second }.trim().ifBlank { null }
                                }
                            } else {
                                rawPronunciation
                            }
                        }

                        val translation = remember(rawSubtitle, showTranslationPref) {
                            if (showTranslationPref) rawSubtitle else null
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

                        val otherSide = remember(index, otherSideForLines) {
                            otherSideForLines.getOrElse(index) { false }
                        }
                        val hasDuet = remember(otherSideForLines) {
                            otherSideForLines.any { it }
                        }
                        val lineIsRtl = remember(lines) {
                            AmlAnimationCurves.isRtlText(lines.joinToString("") { it.second })
                        }
                        val lineAnchorRight = if (lineIsRtl) !otherSide else otherSide

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
                                        contentAlignment = if (lineAnchorRight) Alignment.CenterEnd else Alignment.CenterStart
                                    ) {
                                        AmlInterludeDotsView(
                                            state = interludeState,
                                            color = mainTextBasicColor,
                                            anchorRight = lineAnchorRight
                                        )
                                    }
                                }
                            }

                            LyricsflowWrapper {
                                LyricItem(
                                    isCurrentLambda = { isCurrent.value },
                                    isTopLambda = { isTop.value },
                                    mainLyric = effectiveMainLyric,
                                    syllables = effectiveSyllables,
                                    backgroundLines = lineBackgrounds,
                                    lineStartTimeMs = lineStartMs,
                                    lineEndTimeMs = lineEndMs,
                                    pronunciationText = effectivePronunciationBarText,
                                    translation = translation,
                                    showTranslation = translation != null,
                                    subTextSize = uiConfig.subTextSize,
                                    blur = { blur.value },
                                    mainTextBasicColor = mainTextBasicColor,
                                    subTextBasicColor = subTextBasicColor,
                                    otherSide = otherSide,
                                    hasDuet = hasDuet,
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

        LyricsflowWrapper {
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
    LyricsflowWrapper {
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
            SubcomposeLayout(modifier = modifier.fillMaxWidth()) { constraints ->
                val targetWidth = constraints.maxWidth.coerceAtLeast(1)
                val measureResult = measurer.measure(
                    text = styledString,
                    style = style,
                    constraints = Constraints(
                        minWidth = targetWidth,
                        maxWidth = targetWidth
                    ),
                    layoutDirection = if (isRtlLine) LayoutDirection.Rtl else LayoutDirection.Ltr
                )

                val minLineHeightPx = (style.lineHeight * measureResult.lineCount.coerceAtLeast(1)).roundToPx()
                val targetHeight = max(measureResult.size.height, minLineHeightPx).coerceAtLeast(1)

                val content = subcompose(lines) {
                    Spacer(
                        Modifier
                            .fillMaxSize()
                            .drawWithCache { draw(constraints, measureResult) }
                    )
                }.first()

                val placeable = content.measure(
                    Constraints.fixed(targetWidth, targetHeight)
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
    val trimmedText: String,
    val startCharIndex: Int,
    val endCharIndexExclusive: Int,
    val startTimeMs: Float,
    val endTimeMs: Float,
    val syllables: List<AmlSyllable>,
    val isRtl: Boolean,
    val isEmphasized: Boolean
) {
    fun computeProgress(nowMs: Float): Float {
        if (nowMs <= startTimeMs) return 0f
        if (nowMs >= endTimeMs) return 1f
        if (syllables.size <= 1) {
            return ((nowMs - startTimeMs) / (endTimeMs - startTimeMs).coerceAtLeast(1f)).coerceIn(0f, 1f)
        }
        val totalChars = syllables.sumOf { it.text.trim().length.coerceAtLeast(1) }.coerceAtLeast(1)
        var completedChars = 0
        for (syl in syllables) {
            val sylChars = syl.text.trim().length.coerceAtLeast(1)
            val startFrac = completedChars.toFloat() / totalChars.toFloat()
            val endFrac = (completedChars + sylChars).toFloat() / totalChars.toFloat()
            if (nowMs <= syl.startTimeMs) {
                return startFrac.coerceIn(0f, 1f)
            }
            if (nowMs < syl.endTimeMs) {
                val sylDur = (syl.endTimeMs - syl.startTimeMs).coerceAtLeast(1f)
                val sylPct = ((nowMs - syl.startTimeMs) / sylDur).coerceIn(0f, 1f)
                return (startFrac + sylPct * (endFrac - startFrac)).coerceIn(0f, 1f)
            }
            completedChars += sylChars
        }
        return 1f
    }
}

@Stable
private data class WordVisualSegment(
    val unit: WordRunUnit,
    val lineIndex: Int,
    val wordLeft: Float,
    val wordRight: Float,
    var clipLeft: Float,
    var clipRight: Float,
    val clipTop: Float,
    val clipBottom: Float
)

@Stable
private data class EmphasizedCharSegment(
    val nonSpaceIdx: Int,
    val trimmedLen: Int,
    val charLeft: Float,
    val charRight: Float,
    var clipLeft: Float,
    var clipRight: Float,
    val clipTop: Float,
    val clipBottom: Float,
    val boxCenter: Offset
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
    pronunciationText: String? = null,
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
    hasDuet: Boolean = false,
    liveTimeLambda: () -> Int,
    onClick: () -> Unit
) {
    val fullLineString = remember(mainLyric) {
        mainLyric.joinToString("") { it.second }
    }
    val isRtlLine = remember(fullLineString) {
        AmlAnimationCurves.isRtlText(fullLineString)
    }

    // Authentic Apple Music / LyricsFlow Duet & Non-Duet Anchor Side (`main/css/lyrics.css` lines 573-617):
    // - LTR Primary Singer (`!isRtlLine && !otherSide`): Left-anchored (`anchorRight = false`)
    // - LTR Duet Singer 2 (`!isRtlLine && otherSide`): Right-anchored (`anchorRight = true`)
    // - RTL Primary Singer (`isRtlLine && !otherSide`): Right-anchored (`anchorRight = true`)
    // - RTL Duet Singer 2 (`isRtlLine && otherSide`): Left-anchored (`anchorRight = false`)
    val anchorRight = if (isRtlLine) !otherSide else otherSide
    val viewAlign = if (anchorRight) Alignment.End else Alignment.Start
    val textAlign = if (anchorRight) TextAlign.Right else TextAlign.Left

    val focusedColor = Color(0xFFFFFFFF)
    val unfocusedColor = Color(0x36FFFFFF)
    val focusedSolidBrush = SolidColor(focusedColor)
    val unfocusedSolidBrush = SolidColor(unfocusedColor)

    val isNotOneByOne = rememberSaveable(mainLyric, syllables) {
        mutableStateOf(
            syllables.size <= 1 && mainLyric.all { it.first == mainLyric.firstOrNull()?.first }
        )
    }

    val liveTime = remember(mainLyric) { mutableIntStateOf(liveTimeLambda()) }

    LyricsflowWrapper {
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

    // Build whole-word units (`WordRunUnit`) by grouping consecutive non-empty pieces where `isPartOfWord == true`.
    // Filtering out the leading sentinel `""` (`mainLyric[0]`) ensures a 1:1 index alignment with `syllables`!
    val wordRunUnits = remember(mainLyric, syllables, lineStartTimeMs, lineEndTimeMs, isRtlLine) {
        val units = ArrayList<WordRunUnit>()
        val nonEmptyPieces = ArrayList<Pair<Int, Pair<Float, String>>>(mainLyric.size)
        for (idx in mainLyric.indices) {
            val piece = mainLyric[idx]
            if (piece.second.isNotEmpty()) {
                nonEmptyPieces.add(idx to piece)
            }
        }

        var charCursor = 0
        var k = 0
        while (k < nonEmptyPieces.size) {
            val startChar = charCursor
            val sb = StringBuilder()
            val wordSyllables = ArrayList<AmlSyllable>(4)

            val (firstOrigIdx, firstPiece) = nonEmptyPieces[k]
            val firstSyl = syllables.getOrNull(k)
            val wordStartMs = firstSyl?.startTimeMs
                ?: if (firstOrigIdx <= 0) {
                    mainLyric.firstOrNull()?.first ?: lineStartTimeMs
                } else {
                    mainLyric[firstOrigIdx - 1].first
                }
            var wordEndMs = firstSyl?.endTimeMs ?: firstPiece.first

            while (k < nonEmptyPieces.size) {
                val (origIdx, piece) = nonEmptyPieces[k]
                val pieceText = piece.second
                sb.append(pieceText)
                charCursor += pieceText.length

                val syl = syllables.getOrNull(k)
                val pieceStartMs = syl?.startTimeMs
                    ?: if (origIdx <= 0) {
                        wordStartMs
                    } else {
                        mainLyric[origIdx - 1].first.coerceAtLeast(wordStartMs)
                    }
                val pieceEndMs = syl?.endTimeMs
                    ?: if (piece.first > pieceStartMs) {
                        piece.first
                    } else {
                        mainLyric.getOrNull(origIdx + 1)?.first
                            ?: lineEndTimeMs.coerceAtLeast(pieceStartMs + 250f)
                    }

                wordEndMs = max(wordEndMs, pieceEndMs)
                wordSyllables.add(
                    syl ?: AmlSyllable(
                        text = pieceText,
                        startTimeMs = pieceStartMs,
                        endTimeMs = pieceEndMs.coerceAtLeast(pieceStartMs + 1f),
                        isPartOfWord = false
                    )
                )

                val continuesWord = syl?.isPartOfWord == true && !pieceText.endsWith(" ")
                k++
                if (!continuesWord) break
            }

            val wordStr = sb.toString()
            val trimmedStr = wordStr.trim()
            if (trimmedStr.isNotEmpty()) {
                val wordIsRtl = isRtlLine || AmlAnimationCurves.isRtlText(trimmedStr)
                val wordDur = (wordEndMs - wordStartMs).coerceAtLeast(1f)
                // Disable emphasis & long-timed word character animations completely for RTL!
                val wordEmphasized = !wordIsRtl && AmlAnimationCurves.shouldEmphasizeWord(trimmedStr, wordDur)
                units.add(
                    WordRunUnit(
                        text = wordStr,
                        trimmedText = trimmedStr,
                        startCharIndex = startChar,
                        endCharIndexExclusive = charCursor,
                        startTimeMs = wordStartMs,
                        endTimeMs = wordEndMs.coerceAtLeast(wordStartMs + 1f),
                        syllables = wordSyllables,
                        isRtl = wordIsRtl,
                        isEmphasized = wordEmphasized
                    )
                )
            }
        }
        units
    }

    LyricsflowWrapper {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp),
            horizontalAlignment = viewAlign
        ) {
            val otherSideAnimate = if (anchorRight) {
                TransformOrigin(1f, 0.25f)
            } else {
                TransformOrigin(0f, 0.25f)
            }

            val otherSideTransformOrigin = if (anchorRight) {
                TransformOrigin(1f, 0.5f)
            } else {
                TransformOrigin(0f, 0.5f)
            }

            val tweenSpecWithDelay: AnimationSpec<Float> = remember(mainLyric) {
                TweenSpec(
                    durationMillis = 270,
                    easing = lyricsflowEasing,
                    delay = 90
                )
            }

            val tweenSpecWithoutDelay: AnimationSpec<Float> = remember(mainLyric) {
                TweenSpec(durationMillis = 300, easing = lyricsflowEasing, delay = 40)
            }

            // `Specs.deselectedScale = 0.98`, active `1.0`
            val scale = animateFloatAsState(
                targetValue = if (isCurrentLambda()) 1.0f else 0.98f,
                animationSpec = if (isCurrentLambda()) tweenSpecWithDelay else tweenSpecWithoutDelay,
                label = "LyricLineScale"
            )

            // Match `main/css/lyrics.css`:
            // - Duet song: 15% (~36.dp) opposite-side margin so Singer 1 and Singer 2 are clearly separated
            // - Solo song: 16.dp opposite-side margin anchored cleanly to the natural reading side
            val cardPadding = if (hasDuet) {
                if (anchorRight) {
                    Modifier.padding(start = 36.dp, end = 4.dp)
                } else {
                    Modifier.padding(start = 4.dp, end = 36.dp)
                }
            } else {
                if (anchorRight) {
                    Modifier.padding(start = 16.dp, end = 4.dp)
                } else {
                    Modifier.padding(start = 4.dp, end = 16.dp)
                }
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
                                easing = lyricsflowEasing
                            )
                        ) + scaleIn(
                            initialScale = 0.85f,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(
                                durationMillis = 420,
                                easing = lyricsflowEasing
                            )
                        ),
                        exit = fadeOut() + scaleOut(
                            targetScale = 0.85f,
                            transformOrigin = otherSideAnimate,
                            animationSpec = TweenSpec(
                                durationMillis = 300,
                                easing = lyricsflowEasing
                            )
                        )
                    ) {
                        LyricsflowWrapper {
                            LyricCard(
                                { scale.value },
                                cardPadding,
                                otherSideTransformOrigin,
                                viewAlign
                            ) {
                                Column(
                                    Modifier
                                        .fillMaxWidth()
                                        .padding(horizontal = 16.dp)
                                        .padding(top = 8.dp, bottom = 10.dp),
                                    horizontalAlignment = viewAlign
                                ) {
                                    AmlInterludeDotsView(
                                        state = interludeState,
                                        color = mainTextBasicColor,
                                        anchorRight = anchorRight
                                    )
                                }
                            }
                        }
                    }
                }
            } else {
                LyricsflowWrapper {
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

                        LyricsflowWrapper {
                            Column(
                                Modifier
                                    .then(blurModifier.value)
                                    .fillMaxWidth(),
                                horizontalAlignment = viewAlign
                            ) {
                                val alphaTweenSpecWithDelay: AnimationSpec<Float> =
                                    remember(mainLyric) {
                                        TweenSpec(
                                            durationMillis = 320,
                                            easing = lyricsflowEasing,
                                            delay = 110
                                        )
                                    }

                                val alphaTweenSpecWithoutDelay: AnimationSpec<Float> =
                                    remember(mainLyric) {
                                        TweenSpec(
                                            durationMillis = 320,
                                            easing = lyricsflowEasing,
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
                                        anchorRight = anchorRight,
                                        showTranslation = showTranslation,
                                        subTextBasicColor = subTextBasicColor,
                                        onClick = onClick
                                    )
                                }

                                LyricsflowWrapper {
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

                                    val showHighLight = remember(mainLyric, translation, wordRunUnits) {
                                        derivedStateOf {
                                            if (isNotOneByOne.value) {
                                                true
                                            } else {
                                                val lastWordEnd = wordRunUnits.lastOrNull()?.endTimeMs ?: lineEndTimeMs
                                                liveTime.intValue >= lastWordEnd
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
                                            .padding(vertical = 4.dp, horizontal = 16.dp)
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

                                        // Pre-compute visual segments directly from `measureResult` inside `drawWithCache`.
                                        // By drawing `measureResult` inside per-word `clipRect` + `translate(0f, risePx)`,
                                        // we get 100% pixel-identical glyph positions, kerning, Arabic shaping, and
                                        // Left/Right alignment with zero `TextAlign` double-offset or vertical jump!
                                        val totalChars = fullLineString.length
                                        if (totalChars <= 0 || wordRunUnits.isEmpty()) {
                                            return@Line onDrawWithContent {
                                                drawText(
                                                    textLayoutResult = measureResult,
                                                    color = focusedColor
                                                )
                                            }
                                        }

                                        val lineCount = measureResult.lineCount.coerceAtLeast(1)
                                        val segmentsByLine = Array(lineCount) { ArrayList<WordVisualSegment>() }
                                        val emphasizedSegmentsByUnit = HashMap<WordRunUnit, List<EmphasizedCharSegment>>()

                                        wordRunUnits.fastForEach { unit ->
                                            val wordText = unit.text
                                            val firstNonSpace = wordText.indexOfFirst { !it.isWhitespace() }
                                            val lastNonSpace = wordText.indexOfLast { !it.isWhitespace() }
                                            if (firstNonSpace < 0 || lastNonSpace < firstNonSpace) return@fastForEach

                                            val firstCharIdx = (unit.startCharIndex + firstNonSpace).coerceIn(0, totalChars - 1)
                                            val lastCharIdx = (unit.startCharIndex + lastNonSpace).coerceIn(0, totalChars - 1)

                                            val startLine = measureResult.getLineForOffset(firstCharIdx).coerceIn(0, lineCount - 1)
                                            val endLine = measureResult.getLineForOffset(lastCharIdx).coerceIn(startLine, lineCount - 1)

                                            for (lineIdx in startLine..endLine) {
                                                val lineStartOffset = measureResult.getLineStart(lineIdx)
                                                val lineEndOffset = (measureResult.getLineEnd(lineIdx, visibleEnd = true) - 1)
                                                    .coerceAtLeast(lineStartOffset)
                                                val segFirstChar = max(firstCharIdx, lineStartOffset).coerceIn(0, totalChars - 1)
                                                val segLastChar = min(lastCharIdx, lineEndOffset).coerceIn(segFirstChar, totalChars - 1)

                                                var wLeft = Float.POSITIVE_INFINITY
                                                var wRight = Float.NEGATIVE_INFINITY
                                                for (chIdx in segFirstChar..segLastChar) {
                                                    val box = measureResult.getBoundingBox(chIdx)
                                                    val bL = min(box.left, box.right)
                                                    val bR = max(box.left, box.right)
                                                    if (bR > bL) {
                                                        wLeft = min(wLeft, bL)
                                                        wRight = max(wRight, bR)
                                                    }
                                                }
                                                if (!wLeft.isFinite() || !wRight.isFinite() || wRight <= wLeft) {
                                                    val fallbackBox = measureResult.getBoundingBox(segFirstChar)
                                                    wLeft = min(fallbackBox.left, fallbackBox.right)
                                                    wRight = max(fallbackBox.left, fallbackBox.right).coerceAtLeast(wLeft + 2f)
                                                }

                                                val cTop = if (lineIdx == 0) -28f else measureResult.getLineTop(lineIdx) - 6f
                                                val cBottom = if (lineIdx == lineCount - 1) size.height + 28f else measureResult.getLineBottom(lineIdx) + 6f

                                                val seg = WordVisualSegment(
                                                    unit = unit,
                                                    lineIndex = lineIdx,
                                                    wordLeft = wLeft,
                                                    wordRight = wRight,
                                                    clipLeft = wLeft - 4f,
                                                    clipRight = wRight + 4f,
                                                    clipTop = cTop,
                                                    clipBottom = cBottom
                                                )
                                                segmentsByLine[lineIdx].add(seg)
                                            }

                                            // Pre-compute per-character segments for emphasized LTR words (`>= 1000ms` and `2..7` chars)
                                            if (unit.isEmphasized && !unit.isRtl) {
                                                val trimmedLen = unit.trimmedText.length.coerceAtLeast(1)
                                                val charSegs = ArrayList<EmphasizedCharSegment>(trimmedLen)
                                                var nonSpaceIdx = 0
                                                for (charOffset in 0 until wordText.length) {
                                                    val ch = wordText[charOffset]
                                                    if (ch.isWhitespace()) continue
                                                    val globalCharIdx = (unit.startCharIndex + charOffset).coerceIn(0, totalChars - 1)
                                                    val lineIdx = measureResult.getLineForOffset(globalCharIdx).coerceIn(0, lineCount - 1)
                                                    val box = measureResult.getBoundingBox(globalCharIdx)
                                                    val cLeft = min(box.left, box.right)
                                                    val cRight = max(box.left, box.right).coerceAtLeast(cLeft + 1f)
                                                    val cTop = if (lineIdx == 0) -28f else measureResult.getLineTop(lineIdx) - 6f
                                                    val cBottom = if (lineIdx == lineCount - 1) size.height + 28f else measureResult.getLineBottom(lineIdx) + 6f
                                                    charSegs.add(
                                                        EmphasizedCharSegment(
                                                            nonSpaceIdx = nonSpaceIdx,
                                                            trimmedLen = trimmedLen,
                                                            charLeft = cLeft,
                                                            charRight = cRight,
                                                            clipLeft = cLeft,
                                                            clipRight = cRight,
                                                            clipTop = cTop,
                                                            clipBottom = cBottom,
                                                            boxCenter = box.center
                                                        )
                                                    )
                                                    nonSpaceIdx++
                                                }
                                                if (charSegs.isNotEmpty()) {
                                                    charSegs.sortBy { it.charLeft }
                                                    charSegs.first().clipLeft -= 10f
                                                    charSegs.last().clipRight += 10f
                                                    for (m in 0 until charSegs.size - 1) {
                                                        val mid = (charSegs[m].charRight + charSegs[m + 1].charLeft) * 0.5f
                                                        charSegs[m].clipRight = mid
                                                        charSegs[m + 1].clipLeft = mid
                                                    }
                                                    emphasizedSegmentsByUnit[unit] = charSegs
                                                }
                                            }
                                        }

                                        // Compute seamless horizontal clip midpoints between adjacent words on each visual line
                                        val allWordSegments = ArrayList<WordVisualSegment>(wordRunUnits.size + 4)
                                        for (lineIdx in 0 until lineCount) {
                                            val lineSegs = segmentsByLine[lineIdx]
                                            if (lineSegs.isEmpty()) continue
                                            lineSegs.sortBy { it.wordLeft }
                                            lineSegs.first().clipLeft = -16f
                                            lineSegs.last().clipRight = size.width + 16f
                                            for (m in 0 until lineSegs.size - 1) {
                                                val mid = (lineSegs[m].wordRight + lineSegs[m + 1].wordLeft) * 0.5f
                                                lineSegs[m].clipRight = mid
                                                lineSegs[m + 1].clipLeft = mid
                                            }
                                            allWordSegments.addAll(lineSegs)
                                        }

                                        val fontSizePx = effectiveLineStyle.fontSize.toPx()

                                        onDrawBehind {
                                            val nowMs = liveTime.intValue.toFloat()

                                            allWordSegments.fastForEach { seg ->
                                                val unit = seg.unit
                                                val wordElapsedMs = nowMs - unit.startTimeMs
                                                val risePx = AmlAnimationCurves.wordRiseOffsetPx(wordElapsedMs)
                                                val pct = unit.computeProgress(nowMs)

                                                val segWidth = (seg.wordRight - seg.wordLeft).coerceAtLeast(4f)
                                                val featherPx = (segWidth * 0.28f).coerceIn(10f, 36f)

                                                val wordBrush: Brush = when {
                                                    pct <= 0f -> unfocusedSolidBrush
                                                    pct >= 1f -> focusedSolidBrush
                                                    unit.isRtl -> {
                                                        val currentX = (seg.wordRight + featherPx) - pct * (segWidth + 2f * featherPx)
                                                        Brush.linearGradient(
                                                            0f to unfocusedColor,
                                                            1f to focusedColor,
                                                            start = Offset(currentX - featherPx, 0f),
                                                            end = Offset(currentX + featherPx, 0f)
                                                        )
                                                    }
                                                    else -> {
                                                        val currentX = (seg.wordLeft - featherPx) + pct * (segWidth + 2f * featherPx)
                                                        Brush.linearGradient(
                                                            0f to focusedColor,
                                                            1f to unfocusedColor,
                                                            start = Offset(currentX - featherPx, 0f),
                                                            end = Offset(currentX + featherPx, 0f)
                                                        )
                                                    }
                                                }

                                                val empSegs = emphasizedSegmentsByUnit[unit]
                                                if (empSegs == null || unit.isRtl) {
                                                    withTransform({
                                                        translate(left = 0f, top = risePx)
                                                        clipRect(
                                                            left = seg.clipLeft,
                                                            top = seg.clipTop,
                                                            right = seg.clipRight,
                                                            bottom = seg.clipBottom
                                                        )
                                                    }) {
                                                        drawText(
                                                            textLayoutResult = measureResult,
                                                            brush = wordBrush,
                                                            alpha = 1f
                                                        )
                                                    }
                                                } else {
                                                    empSegs.fastForEach { charSeg ->
                                                        val empState = AmlAnimationCurves.computeCharEmphasisState(
                                                            wordText = unit.trimmedText,
                                                            wordStartTimeMs = unit.startTimeMs,
                                                            wordEndTimeMs = unit.endTimeMs,
                                                            currentPositionMs = nowMs,
                                                            charIndex = charSeg.nonSpaceIdx,
                                                            charTotal = charSeg.trimmedLen
                                                        )
                                                        val dxPx = empState.dxEm * fontSizePx
                                                        val dyPx = empState.dyPx

                                                        withTransform({
                                                            translate(left = dxPx, top = dyPx)
                                                            if (empState.scale != 1f) {
                                                                scale(empState.scale, empState.scale, pivot = charSeg.boxCenter)
                                                            }
                                                            clipRect(
                                                                left = charSeg.clipLeft,
                                                                top = charSeg.clipTop,
                                                                right = charSeg.clipRight,
                                                                bottom = charSeg.clipBottom
                                                            )
                                                        }) {
                                                            if (empState.glowAlpha > 0.01f) {
                                                                drawText(
                                                                    textLayoutResult = measureResult,
                                                                    color = Color.White.copy(alpha = (empState.glowAlpha * 0.5f).coerceIn(0f, 0.42f)),
                                                                    shadow = Shadow(
                                                                        color = Color.White.copy(alpha = empState.glowAlpha.coerceIn(0f, 0.65f)),
                                                                        offset = Offset.Zero,
                                                                        blurRadius = 16f
                                                                    )
                                                                )
                                                            }
                                                            drawText(
                                                                textLayoutResult = measureResult,
                                                                brush = wordBrush,
                                                                alpha = 1f
                                                            )
                                                        }
                                                    }
                                                }
                                            }
                                        }
                                    }
                                }

                                // Pronunciation / Romanization line with horizontal word-progress swipe (0f vertical lift)
                                LyricsflowWrapper {
                                    AnimatedVisibility(!pronunciationText.isNullOrBlank()) {
                                        pronunciationText?.let { pronStr ->
                                            val isPronRtl = remember(pronStr) {
                                                AmlAnimationCurves.isRtlText(pronStr)
                                            }
                                            val pronAlpha = animateFloatAsState(
                                                targetValue = if (isCurrentLambda()) 0.78f else 0.22f,
                                                animationSpec = if (isCurrentLambda()) alphaTweenSpecWithDelay else alphaTweenSpecWithoutDelay,
                                                label = "PronunciationAlpha"
                                            )
                                            val nowMs = liveTime.intValue.toFloat()
                                            val firstSylStart = wordRunUnits.firstOrNull()?.startTimeMs ?: lineStartTimeMs
                                            val lastSylEnd = wordRunUnits.lastOrNull()?.endTimeMs ?: lineEndTimeMs
                                            val pronSwipeProgress = if (!isNotOneByOne.value && isCurrentLambda()) {
                                                ((nowMs - firstSylStart) / (lastSylEnd - firstSylStart).coerceAtLeast(1f)).coerceIn(0f, 1f)
                                            } else if (isCurrentLambda()) {
                                                1f
                                            } else {
                                                0f
                                            }

                                            val pronBrush: Brush = if (!isNotOneByOne.value && isCurrentLambda() && pronSwipeProgress in 0.001f..0.999f) {
                                                if (isPronRtl) {
                                                    val edge = 1f - pronSwipeProgress
                                                    Brush.horizontalGradient(
                                                        0f to Color.White.copy(alpha = 0.38f),
                                                        (edge - 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.38f),
                                                        (edge + 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.96f),
                                                        1f to Color.White.copy(alpha = 0.96f)
                                                    )
                                                } else {
                                                    Brush.horizontalGradient(
                                                        0f to Color.White.copy(alpha = 0.96f),
                                                        (pronSwipeProgress - 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.96f),
                                                        (pronSwipeProgress + 0.08f).coerceIn(0f, 1f) to Color.White.copy(alpha = 0.38f),
                                                        1f to Color.White.copy(alpha = 0.38f)
                                                    )
                                                }
                                            } else {
                                                SolidColor(
                                                    if (isCurrentLambda()) Color.White.copy(alpha = 0.88f)
                                                    else subTextBasicColor
                                                )
                                            }

                                            Text(
                                                text = pronStr,
                                                style = TextStyle(
                                                    fontFamily = if (isPronRtl) SFArabic else SFProDisplay,
                                                    fontSize = (subTextSize + 1).sp,
                                                    fontWeight = FontWeight.SemiBold,
                                                    lineHeight = (subTextSize + 6).sp,
                                                    letterSpacing = if (isPronRtl) 0.sp else 0.15.sp,
                                                    textAlign = textAlign,
                                                    brush = pronBrush
                                                ),
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        this.alpha = pronAlpha.value
                                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                                    }
                                                    .padding(horizontal = 16.dp)
                                                    .padding(top = 3.dp)
                                            )
                                        }
                                    }
                                }

                                // Secondary line: Translation (uses SFArabic if RTL)
                                LyricsflowWrapper {
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

                                            Text(
                                                text = subStr,
                                                fontFamily = if (isSubRtl) SFArabic else SFProDisplay,
                                                fontSize = subTextSize.sp,
                                                color = subTextBasicColor,
                                                fontWeight = FontWeight.Medium,
                                                modifier = Modifier
                                                    .fillMaxWidth()
                                                    .graphicsLayer {
                                                        this.alpha = translationAlpha.value
                                                        compositingStrategy = CompositingStrategy.ModulateAlpha
                                                    }
                                                    .padding(horizontal = 16.dp)
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
                                        anchorRight = anchorRight,
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
    anchorRight: Boolean,
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
        animationSpec = tween(durationMillis = 260, easing = lyricsflowEasing),
        label = "BgVocalAlpha"
    )

    val textAlign = if (anchorRight) TextAlign.Right else TextAlign.Left
    val rowAlign = if (anchorRight) Alignment.End else Alignment.Start

    AnimatedVisibility(
        visible = parentLineActive || isBgActive,
        enter = expandVertically(tween(260, easing = lyricsflowEasing)) + fadeIn(tween(240)),
        exit = shrinkVertically(tween(240, easing = lyricsflowEasing)) + fadeOut(tween(200))
    ) {
        Column(
            horizontalAlignment = rowAlign,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 2.dp)
                .graphicsLayer {
                    scaleX = slideScale.value
                    scaleY = slideScale.value
                    alpha = slideAlpha.value
                    transformOrigin = if (anchorRight) TransformOrigin(1f, 0f) else TransformOrigin(0f, 0f)
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
                modifier = Modifier
                    .fillMaxWidth()
                    .graphicsLayer {
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
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 2.dp)
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
    LyricsflowWrapper {
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
    color: Color,
    anchorRight: Boolean = false
) {
    Box(
        modifier = Modifier.graphicsLayer {
            scaleX = state.groupScale
            scaleY = state.groupScale
            alpha = state.groupAlpha
            transformOrigin = if (anchorRight) TransformOrigin(1f, 0.5f) else TransformOrigin(0f, 0.5f)
        },
        contentAlignment = if (anchorRight) Alignment.CenterEnd else Alignment.CenterStart
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

private val ConfiguredLyricFontWeightNumeric: Int
    get() = when (SettingsLibrary.LyricFontWeight) {
        "Thin" -> 100
        "ExtraLight" -> 200
        "Light" -> 300
        "Regular" -> 400
        "Medium" -> 500
        "SemiBold" -> 600
        "Bold" -> 700
        "ExtraBold" -> 800
        "Black" -> 900
        else -> SettingsLibrary.LyricFontWeightNumeric.coerceIn(300, 900)
    }

private val ConfiguredLyricFontWeight: FontWeight
    get() = FontWeight(ConfiguredLyricFontWeightNumeric)

private val ConfiguredLyricScaleX: Float
    get() = (SettingsLibrary.LyricFontWidth.coerceIn(75, 125) / 100f)

val MainTextStyle: TextStyle
    get() = TextStyle(
        fontFamily = SFProDisplay,
        fontSize = 31.sp,
        lineHeight = 40.sp,
        fontWeight = ConfiguredLyricFontWeight,
        textGeometricTransform = TextGeometricTransform(scaleX = ConfiguredLyricScaleX),
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
 * Dynamically uses variable font weight (`800` default) and width (`wdth`).
 */
val MainArabicTextStyle: TextStyle
    get() = TextStyle(
        fontFamily = sfArabicDynamicFamily(
            weight = ConfiguredLyricFontWeightNumeric,
            width = SettingsLibrary.LyricFontWidth.coerceIn(75, 125).toFloat()
        ),
        fontSize = 31.sp,
        lineHeight = 42.sp,
        fontWeight = ConfiguredLyricFontWeight,
        textGeometricTransform = TextGeometricTransform(scaleX = ConfiguredLyricScaleX),
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

val BackgroundVocalTextStyle: TextStyle
    get() = TextStyle(
        fontFamily = SFProDisplay,
        fontSize = 21.5.sp,
        lineHeight = 28.sp,
        fontWeight = ConfiguredLyricFontWeight,
        textGeometricTransform = TextGeometricTransform(scaleX = ConfiguredLyricScaleX),
        letterSpacing = (-0.2).sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )

val BackgroundArabicTextStyle: TextStyle
    get() = TextStyle(
        fontFamily = sfArabicDynamicFamily(
            weight = ConfiguredLyricFontWeightNumeric,
            width = SettingsLibrary.LyricFontWidth.coerceIn(75, 125).toFloat()
        ),
        fontSize = 21.5.sp,
        lineHeight = 30.sp,
        fontWeight = ConfiguredLyricFontWeight,
        textGeometricTransform = TextGeometricTransform(scaleX = ConfiguredLyricScaleX),
        letterSpacing = 0.sp,
        lineHeightStyle = LineHeightStyle(
            alignment = LineHeightStyle.Alignment.Center,
            trim = LineHeightStyle.Trim.None
        )
    )
