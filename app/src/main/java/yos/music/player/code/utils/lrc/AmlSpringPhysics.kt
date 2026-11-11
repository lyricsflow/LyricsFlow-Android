package com.lyricsflow.player.code.utils.lrc

import androidx.compose.runtime.Immutable
import kotlin.math.PI
import kotlin.math.abs
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.exp
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sin
import kotlin.math.sqrt

/**
 * Direct port of LyricsFlow's `main/js/spring.js`, `main/js/spline.js`, `main/js/is-rtl.js`, and
 * `main/js/lyrics-animator.js` (Apple Music / AMLL / yourLyrics physics curves).
 */
@Immutable
data class AmlSpringParams(
    val stiffness: Float = 100f,
    val damping: Float = 10f,
    val mass: Float = 1f,
    val soft: Boolean = false
)

class AmlSpring(
    initialPosition: Float = 0f,
    stiffness: Float = 100f,
    damping: Float = 10f,
    mass: Float = 1f
) {
    var targetPosition: Float = initialPosition
        private set
    var currentPosition: Float = initialPosition
        private set
    private var currentTime: Float = 0f
    var params: AmlSpringParams = AmlSpringParams(stiffness, damping, mass)
        private set

    private var currentSolver: (Float) -> Float = { targetPosition }
    private var getV: (Float) -> Float = { 0f }
    private var getV2: (Float) -> Float = { 0f }

    private data class QueuedParams(val params: AmlSpringParams, var time: Float)
    private data class QueuedPosition(val position: Float, var time: Float)

    private var queueParams: QueuedParams? = null
    private var queuePosition: QueuedPosition? = null

    val velocity: Float
        get() = getV(currentTime)

    private fun derivative(f: (Float) -> Float): (Float) -> Float {
        val h = 0.001f
        return { x -> (f(x + h) - f(x - h)) / (2f * h) }
    }

    private fun solveSpring(
        from: Float,
        velocity: Float,
        to: Float,
        delay: Float = 0f,
        p: AmlSpringParams
    ): (Float) -> Float {
        val soft = p.soft
        val stiffness = p.stiffness
        val damping = p.damping
        val mass = p.mass
        val delta = to - from

        if (soft || 1.0f <= damping / (2.0f * sqrt(stiffness * mass))) {
            val angularFrequency = -sqrt(stiffness / mass)
            val leftover = -angularFrequency * delta - velocity
            return solver@{ rawT ->
                val t = rawT - delay
                if (t < 0f) return@solver from
                to - (delta + t * leftover) * exp(t * angularFrequency)
            }
        }

        val dampingFrequency = sqrt(max(0.0001f, 4.0f * mass * stiffness - damping * damping))
        val leftover = (damping * delta - 2.0f * mass * velocity) / dampingFrequency
        val dfm = (0.5f * dampingFrequency) / mass
        val dm = -(0.5f * damping) / mass

        return solver@{ rawT ->
            val t = rawT - delay
            if (t < 0f) return@solver from
            to - (cos(t * dfm) * delta + sin(t * dfm) * leftover) * exp(t * dm)
        }
    }

    private fun resetSolver() {
        val curV = getV(currentTime)
        currentTime = 0f
        currentSolver = solveSpring(
            from = currentPosition,
            velocity = curV,
            to = targetPosition,
            delay = 0f,
            p = params
        )
        getV = derivative(currentSolver)
        getV2 = derivative(getV)
    }

    fun arrived(): Boolean {
        return abs(targetPosition - currentPosition) < 0.01f &&
            abs(getV(currentTime)) < 0.01f &&
            abs(getV2(currentTime)) < 0.01f &&
            queueParams == null &&
            queuePosition == null
    }

    fun setPosition(pos: Float) {
        targetPosition = pos
        currentPosition = pos
        currentSolver = { targetPosition }
        getV = { 0f }
        getV2 = { 0f }
        queuePosition = null
        queueParams = null
    }

    fun updateParams(newParams: AmlSpringParams, delaySecs: Float = 0f) {
        if (delaySecs > 0f) {
            queueParams = QueuedParams(newParams, delaySecs)
        } else {
            queueParams = null
            params = newParams
            resetSolver()
        }
    }

    fun setTargetPosition(target: Float, delaySecs: Float = 0f) {
        if (delaySecs <= 0f && abs(targetPosition - target) < 0.001f) {
            queuePosition = null
            return
        }
        if (delaySecs > 0f) {
            queuePosition = QueuedPosition(target, delaySecs)
        } else {
            queuePosition = null
            targetPosition = target
            resetSolver()
        }
    }

    fun update(dtSecs: Float): Float {
        if (dtSecs <= 0f) return currentPosition
        currentTime += dtSecs
        currentPosition = currentSolver(currentTime)

        queueParams?.let { qp ->
            qp.time -= dtSecs
            if (qp.time <= 0f) {
                queueParams = null
                updateParams(qp.params, 0f)
            }
        }

        queuePosition?.let { qpos ->
            qpos.time -= dtSecs
            if (qpos.time <= 0f) {
                queuePosition = null
                setTargetPosition(qpos.position, 0f)
            }
        }

        if (arrived()) {
            setPosition(targetPosition)
        }

        return currentPosition
    }
}

/**
 * Natural cubic spline interpolator ported from `main/js/spline.js`.
 */
class AmlSpline(
    private val xs: FloatArray,
    private val ys: FloatArray
) {
    private val ks: FloatArray = computeNaturalKs(xs, ys)

    private fun computeNaturalKs(xs: FloatArray, ys: FloatArray): FloatArray {
        val n = xs.size - 1
        val matrix = Array(n + 1) { FloatArray(3) }
        val result = FloatArray(n + 1)

        for (i in 1 until n) {
            val dx1 = xs[i] - xs[i - 1]
            val dx2 = xs[i + 1] - xs[i]
            matrix[i][0] = 1f / dx1
            matrix[i][1] = 2f * (1f / dx1 + 1f / dx2)
            matrix[i][2] = 1f / dx2
            result[i] = 3f * ((ys[i] - ys[i - 1]) / (dx1 * dx1) + (ys[i + 1] - ys[i]) / (dx2 * dx2))
        }

        matrix[0][1] = 2f / (xs[1] - xs[0])
        matrix[0][2] = 1f / (xs[1] - xs[0])
        result[0] = 3f * (ys[1] - ys[0]) / ((xs[1] - xs[0]) * (xs[1] - xs[0]))

        matrix[n][0] = 1f / (xs[n] - xs[n - 1])
        matrix[n][1] = 2f / (xs[n] - xs[n - 1])
        result[n] = 3f * (ys[n] - ys[n - 1]) / ((xs[n] - xs[n - 1]) * (xs[n] - xs[n - 1]))

        for (i in 1..n) {
            val m = matrix[i][0] / matrix[i - 1][1]
            matrix[i][1] -= m * matrix[i - 1][2]
            result[i] -= m * result[i - 1]
        }

        val ks = FloatArray(n + 1)
        ks[n] = result[n] / matrix[n][1]
        for (i in n - 1 downTo 0) {
            ks[i] = (result[i] - matrix[i][2] * ks[i + 1]) / matrix[i][1]
        }
        return ks
    }

    fun at(x: Float): Float {
        if (x <= xs.first()) return ys.first()
        if (x >= xs.last()) return ys.last()

        var i = 0
        while (i < xs.size - 1) {
            if (xs[i + 1] >= x) break
            i++
        }

        val dx = xs[i + 1] - xs[i]
        val t = (x - xs[i]) / dx
        val a = ks[i] * dx - (ys[i + 1] - ys[i])
        val b = -ks[i + 1] * dx + (ys[i + 1] - ys[i])

        return (1f - t) * ys[i] + t * ys[i + 1] + t * (1f - t) * ((1f - t) * a + t * b)
    }
}

@Immutable
data class CharEmphasisVisualState(
    val dxEm: Float = 0f,
    val dyPx: Float = 0f,
    val scale: Float = 1f,
    val glowAlpha: Float = 0f
)

@Immutable
data class InterludeDotsVisualState(
    val visible: Boolean = false,
    val groupScale: Float = 0f,
    val groupAlpha: Float = 0f,
    val dotAlphas: Triple<Float, Float, Float> = Triple(0f, 0f, 0f)
)

object AmlAnimationCurves {
    // ── Word Rise Curve (-9f peak ~ -3.5dp, 2000ms duration, 64 frames) from `lyrics-animator.js` & `yourLyrics` ──
    const val WORD_RISE_PEAK_PX = -9f
    private const val WORD_RISE_DURATION_MS = 2000f
    private const val WORD_RISE_FRAMES = 64

    const val CHAR_LIFT_PX = 6f
    const val CHAR_SPREAD_EM = 0.05f
    const val CHAR_SCALE = 0.10f
    const val CHAR_GLOW_PEAK = 0.45f
    private const val CHAR_GLOW_RAMP_MS = 500f

    private const val TARGET_BREATHE_DURATION_MS = 4500f

    private val wordRiseOffsets: FloatArray by lazy {
        val seconds = WORD_RISE_DURATION_MS / 1000f
        val frequency = sqrt(14f)
        FloatArray(WORD_RISE_FRAMES + 1) { frame ->
            val rise = if (frame == WORD_RISE_FRAMES) {
                1f
            } else {
                1f - springPosition((seconds * frame) / WORD_RISE_FRAMES, frequency, 3.5f)
            }
            WORD_RISE_PEAK_PX * rise
        }
    }

    private val lineSpringSamples = arrayOf(
        floatArrayOf(70.1839f, 13.069f),
        floatArrayOf(120.332f, 18.8406f),
        floatArrayOf(130.412f, 19.8431f),
        floatArrayOf(137.937f, 20.5664f),
        floatArrayOf(167.556f, 23.2377f),
        floatArrayOf(170.301f, 23.4728f),
        floatArrayOf(171.347f, 23.5619f)
    )

    private val fastStiffness = lineSpringSamples.last()[0]
    private val slowStiffness = lineSpringSamples.first()[0]

    fun springPosition(time: Float, frequency: Float, damping: Float): Float {
        val omega = sqrt(max(0.0001f, frequency * frequency - damping * damping))
        val decay = exp(-damping * time)
        return decay * (cos(omega * time) + (damping / omega) * sin(omega * time))
    }

    fun wordRiseOffsetPx(elapsedMs: Float): Float {
        if (elapsedMs <= 0f) return 0f
        val progress = min(elapsedMs / WORD_RISE_DURATION_MS, 1f) * WORD_RISE_FRAMES
        val frame = floor(progress).toInt().coerceIn(0, WORD_RISE_FRAMES)
        val next = min(frame + 1, WORD_RISE_FRAMES)
        val mix = progress - frame
        return wordRiseOffsets[frame] + (wordRiseOffsets[next] - wordRiseOffsets[frame]) * mix
    }

    fun springProgress(timeSec: Float, responseSec: Float): Float {
        if (timeSec <= 0f) return 0f
        val phase = (2f * PI.toFloat() * timeSec) / max(0.001f, responseSec)
        return 1f - (1f + phase) * exp(-phase)
    }

    fun charGlowLevel(progress: Float, peak: Float): Float {
        if (peak <= 0f || progress <= 0f || progress >= 1f) return 0f
        if (progress < 0.15f) {
            return peak * 0.8f * (progress / 0.15f)
        }
        if (progress < 0.6f) {
            return peak * (0.8f + 0.2f * ((progress - 0.15f) / 0.45f))
        }
        return peak * (1f - (progress - 0.6f) / 0.4f)
    }

    private fun isCjkWord(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code in 0x3040..0x30FF ||
                code in 0x3400..0x4DBF ||
                code in 0x4E00..0x9FFF ||
                code in 0xF900..0xFAFF ||
                code in 0xFF66..0xFF9F
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Direct port of `main/js/is-rtl.js`: detects if text is RTL (Arabic, Hebrew, Persian, Urdu, etc.).
     */
    fun isRtlText(text: String): Boolean {
        if (text.isEmpty()) return false
        for (ch in text) {
            if (ch.isDigit() || ch.isWhitespace() || ch in ",.;:?!()[]{}\"'\\/<>@#$%^&*_=+-") {
                continue
            }
            val code = ch.code
            return code in 0x0590..0x05FF ||
                code in 0x0600..0x06FF ||
                code in 0x0700..0x08FF ||
                code in 0xFB1D..0xFDFB ||
                code in 0xFE70..0xFEFC
        }
        return false
    }

    fun isArabicCursiveText(text: String): Boolean {
        for (ch in text) {
            val code = ch.code
            if (code in 0x0600..0x06FF ||
                code in 0x0750..0x077F ||
                code in 0x08A0..0x08FF ||
                code in 0xFB50..0xFDFF ||
                code in 0xFE70..0xFEFF
            ) {
                return true
            }
        }
        return false
    }

    /**
     * Long-timed word character emphasis is strictly disabled for RTL / Arabic text
     * so RTL words never split or lift letter-by-letter.
     */
    fun shouldEmphasizeWord(wordText: String, wordDurationMs: Float): Boolean {
        val trimmed = wordText.trim()
        if (trimmed.isEmpty() || isRtlText(trimmed) || isArabicCursiveText(trimmed)) return false
        val charCount = trimmed.length
        return wordDurationMs >= 1000f && charCount in 2..7
    }

    /**
     * Port of yourLyrics / AML character emphasis curve for long LTR words (`>= 1000ms`, `2..7` chars).
     */
    fun computeCharEmphasisState(
        wordText: String,
        wordStartTimeMs: Float,
        wordEndTimeMs: Float,
        currentPositionMs: Float,
        charIndex: Int,
        charTotal: Int
    ): CharEmphasisVisualState {
        if (isRtlText(wordText) || isArabicCursiveText(wordText)) {
            return CharEmphasisVisualState()
        }
        val anchor = max(1, charTotal)
        val durationMs = max(1000f, wordEndTimeMs - wordStartTimeMs)
        val durationSec = durationMs / 1000f
        val holdSec = (2f * durationSec) / anchor
        val responseSec = min(3f, durationSec)
        val cjk = isCjkWord(wordText)
        val emphasis = ((durationSec - 1f).coerceIn(0f, 1f))
        val glowPeak = CHAR_GLOW_PEAK * ((durationSec - 1f) / (CHAR_GLOW_RAMP_MS / 1000f)).coerceIn(0f, 1f)
        val spanDurationSec = max(holdSec + responseSec * 2f, if (cjk) 3f else 0f)
        val spanMs = spanDurationSec * 1000f
        val staggerMs = durationMs / 2.5f / anchor

        val wordElapsedMs = currentPositionMs - wordStartTimeMs
        val delayMs = charIndex * staggerMs
        val localMs = wordElapsedMs - delayMs

        val centerOffset = if (charTotal <= 1) {
            0f
        } else {
            val mid = (charTotal - 1) * 0.5f
            (charIndex - mid) / max(1f, mid)
        }

        if (localMs >= spanMs) {
            return CharEmphasisVisualState(
                dxEm = 0f,
                dyPx = WORD_RISE_PEAK_PX,
                scale = 1f,
                glowAlpha = 0f
            )
        }
        if (localMs <= 0f) {
            return CharEmphasisVisualState()
        }

        val timeSec = localMs / 1000f
        val rise = if (cjk) {
            1f - springPosition(timeSec, sqrt(14f), 3.5f)
        } else {
            springProgress(timeSec, responseSec)
        }
        val envelope = if (emphasis > 0f) {
            springProgress(timeSec, responseSec) * (1f - springProgress(timeSec - holdSec, responseSec))
        } else {
            0f
        }

        val dyPx = WORD_RISE_PEAK_PX * rise - CHAR_LIFT_PX * emphasis * envelope
        val dxEm = centerOffset * CHAR_SPREAD_EM * emphasis * envelope
        val scale = 1f + CHAR_SCALE * emphasis * envelope
        val glowAlpha = charGlowLevel(localMs / spanMs, glowPeak)

        return CharEmphasisVisualState(
            dxEm = dxEm,
            dyPx = dyPx,
            scale = scale,
            glowAlpha = glowAlpha
        )
    }

    private fun easeOutExpo(x: Float): Float {
        val clamped = x.coerceIn(0f, 1f)
        return if (clamped >= 1f) 1f else 1f - 2f.pow(-10f * clamped)
    }

    private fun easeInOutBack(x: Float): Float {
        val clamped = x.coerceIn(0f, 1f)
        val c1 = 1.70158f
        val c2 = c1 * 1.525f
        return if (clamped < 0.5f) {
            ((2f * clamped).pow(2) * ((c2 + 1f) * 2f * clamped - c2)) / 2f
        } else {
            ((2f * clamped - 2f).pow(2) * ((c2 + 1f) * (clamped * 2f - 2f) + c2) + 2f) / 2f
        }
    }

    /**
     * Port of `advanceInterludeDots` from `main/js/lyrics-animator.js`.
     */
    fun computeInterludeDotsState(
        positionMs: Float,
        startTimeMs: Float,
        endTimeMs: Float
    ): InterludeDotsVisualState {
        if (positionMs < startTimeMs || positionMs >= endTimeMs) {
            return InterludeDotsVisualState(visible = false)
        }
        val interludeDuration = endTimeMs - startTimeMs
        val currentDuration = positionMs - startTimeMs
        if (interludeDuration <= 0f || currentDuration > interludeDuration) {
            return InterludeDotsVisualState(visible = false)
        }

        val breatheCycles = max(1f, ceil(interludeDuration / TARGET_BREATHE_DURATION_MS))
        val breatheDuration = interludeDuration / breatheCycles
        var scale = 1f
        var globalOpacity = 1f

        // Sine breathing around baseline, ±5%
        scale *= sin(1.5f * PI.toFloat() - (currentDuration / breatheDuration) * 2f * PI.toFloat()) / 20f + 1f

        // Entrance zoom (first 2s) + fade-in (0.5-1s)
        if (currentDuration < 2000f) {
            scale *= easeOutExpo(currentDuration / 2000f)
        }
        if (currentDuration < 500f) {
            globalOpacity = 0f
        } else if (currentDuration < 1000f) {
            globalOpacity *= (currentDuration - 500f) / 500f
        }

        // Exit: shrink with bounce (last 750ms), fade out (last 375ms)
        val remaining = interludeDuration - currentDuration
        if (remaining < 750f) {
            scale *= 1f - easeInOutBack(((750f - remaining) / 750f) / 2f)
        }
        if (remaining < 375f) {
            globalOpacity *= (remaining / 375f).coerceIn(0f, 1f)
        }

        val dotsDuration = max(0f, interludeDuration - 750f)
        scale = max(0f, scale) * 0.82f

        fun dotOpacityFor(index: Int): Float {
            val xi = if (dotsDuration > 0f) {
                (((currentDuration - (dotsDuration / 3f) * index) * 3f) / dotsDuration) * 0.75f
            } else {
                0.25f
            }
            val rawDotOpacity = if (dotsDuration > 0f) xi.coerceIn(0.25f, 1f) else 0.25f
            return (globalOpacity * rawDotOpacity).coerceIn(0f, 1f)
        }

        return InterludeDotsVisualState(
            visible = globalOpacity > 0.005f && scale > 0.005f,
            groupScale = scale,
            groupAlpha = globalOpacity.coerceIn(0f, 1f),
            dotAlphas = Triple(dotOpacityFor(0), dotOpacityFor(1), dotOpacityFor(2))
        )
    }

    private fun dampingForStiffness(stiffness: Float): Float {
        for (i in 1 until lineSpringSamples.size) {
            val k1 = lineSpringSamples[i][0]
            val c1 = lineSpringSamples[i][1]
            if (stiffness <= k1) {
                val k0 = lineSpringSamples[i - 1][0]
                val c0 = lineSpringSamples[i - 1][1]
                return c0 + (c1 - c0) * ((stiffness - k0) / (k1 - k0))
            }
        }
        return lineSpringSamples.last()[1]
    }

    fun getPosYSpringPolicy(
        isSeeking: Boolean,
        isInterludeActive: Boolean,
        intervalMs: Float
    ): AmlSpringParams {
        if (isSeeking || isInterludeActive || !intervalMs.isFinite() || intervalMs <= 0f) {
            return AmlSpringParams(
                stiffness = slowStiffness,
                damping = lineSpringSamples.first()[1],
                mass = 1f
            )
        }
        val blend = ((intervalMs - 800f) / (5000f - 800f)).coerceIn(0f, 1f)
        val stiffness = fastStiffness + (slowStiffness - fastStiffness) * blend
        return AmlSpringParams(
            stiffness = stiffness,
            damping = dampingForStiffness(stiffness),
            mass = 1f
        )
    }
}
