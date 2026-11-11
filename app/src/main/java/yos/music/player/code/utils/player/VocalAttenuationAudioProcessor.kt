package com.lyricsflow.player.code.utils.player

import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.media3.common.C
import androidx.media3.common.audio.AudioProcessor
import androidx.media3.common.audio.AudioProcessor.AudioFormat
import androidx.media3.common.util.UnstableApi
import java.nio.ByteBuffer
import java.nio.ByteOrder
import kotlin.math.max
import kotlin.math.min

/**
 * Apple Music Sing Real-Time Vocal Attenuation Processor (`VocalAttenuationAudioProcessor`).
 * Performs stereo mid/side center-vocal cancellation (`L' = L - k * Mid`, `R' = R - k * Mid`)
 * while preserving low-frequency bass (< 180 Hz) so kick drums and bass guitar stay punchy.
 */
@UnstableApi
object VocalAttenuationAudioProcessor : AudioProcessor {
    //Compose-observable state for NowPlaying UI
    val singEnabled = mutableStateOf(false)
    // 0.0f = Full Vocals (0% attenuation), 1.0f = Maximum Vocal Removal (92% center attenuation)
    val attenuationLevel = mutableFloatStateOf(0.80f)

    @Volatile
    private var activeAttenuation: Float = 0f

    fun updateSingState(enabled: Boolean, level: Float = attenuationLevel.floatValue) {
        val clamped = level.coerceIn(0f, 1f)
        singEnabled.value = enabled
        attenuationLevel.floatValue = clamped
        activeAttenuation = if (enabled) clamped * 0.92f else 0f
    }

    private var inputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var outputAudioFormat: AudioFormat = AudioFormat.NOT_SET
    private var buffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var outputBuffer: ByteBuffer = AudioProcessor.EMPTY_BUFFER
    private var inputEnded = false

    // Single-pole low-pass filter state to preserve mono bass (< 180 Hz)
    private var bassState: Float = 0f

    override fun configure(inputAudioFormat: AudioFormat): AudioFormat {
        if (inputAudioFormat.channelCount != 2) {
            this.inputAudioFormat = AudioFormat.NOT_SET
            this.outputAudioFormat = AudioFormat.NOT_SET
            return AudioFormat.NOT_SET
        }
        if (inputAudioFormat.encoding != C.ENCODING_PCM_16BIT &&
            inputAudioFormat.encoding != C.ENCODING_PCM_FLOAT
        ) {
            this.inputAudioFormat = AudioFormat.NOT_SET
            this.outputAudioFormat = AudioFormat.NOT_SET
            return AudioFormat.NOT_SET
        }
        this.inputAudioFormat = inputAudioFormat
        this.outputAudioFormat = inputAudioFormat
        return outputAudioFormat
    }

    override fun isActive(): Boolean {
        return outputAudioFormat != AudioFormat.NOT_SET
    }

    override fun queueInput(inputBuffer: ByteBuffer) {
        if (!inputBuffer.hasRemaining()) return
        val remaining = inputBuffer.remaining()
        if (buffer.capacity() < remaining) {
            buffer = ByteBuffer.allocateDirect(remaining).order(ByteOrder.nativeOrder())
        } else {
            buffer.clear()
        }

        val k = activeAttenuation
        if (k <= 0.005f || inputAudioFormat.channelCount != 2) {
            buffer.put(inputBuffer)
            buffer.flip()
            outputBuffer = buffer
            return
        }

        // Alpha for ~180Hz low-pass bass preservation at sampleRate
        val sr = inputAudioFormat.sampleRate.coerceAtLeast(8000).toFloat()
        val dt = 1f / sr
        val rc = 1f / (2f * Math.PI.toFloat() * 180f)
        val alpha = (dt / (rc + dt)).coerceIn(0.005f, 0.25f)

        when (inputAudioFormat.encoding) {
            C.ENCODING_PCM_16BIT -> {
                while (inputBuffer.remaining() >= 4) {
                    val leftShort = inputBuffer.short
                    val rightShort = inputBuffer.short
                    val l = leftShort / 32768f
                    val r = rightShort / 32768f

                    val mid = (l + r) * 0.5f
                    bassState += alpha * (mid - bassState)
                    val vocalMid = mid - bassState

                    val outL = (l - k * vocalMid).coerceIn(-1f, 1f)
                    val outR = (r - k * vocalMid).coerceIn(-1f, 1f)

                    buffer.putShort((outL * 32767f).toInt().toShort())
                    buffer.putShort((outR * 32767f).toInt().toShort())
                }
            }
            C.ENCODING_PCM_FLOAT -> {
                while (inputBuffer.remaining() >= 8) {
                    val l = inputBuffer.float
                    val r = inputBuffer.float

                    val mid = (l + r) * 0.5f
                    bassState += alpha * (mid - bassState)
                    val vocalMid = mid - bassState

                    val outL = max(-1f, min(1f, l - k * vocalMid))
                    val outR = max(-1f, min(1f, r - k * vocalMid))

                    buffer.putFloat(outL)
                    buffer.putFloat(outR)
                }
            }
            else -> {
                buffer.put(inputBuffer)
            }
        }

        if (inputBuffer.hasRemaining()) {
            buffer.put(inputBuffer)
        }
        buffer.flip()
        outputBuffer = buffer
    }

    override fun queueEndOfStream() {
        inputEnded = true
    }

    override fun getOutput(): ByteBuffer {
        val out = outputBuffer
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        return out
    }

    override fun isEnded(): Boolean {
        return inputEnded && outputBuffer === AudioProcessor.EMPTY_BUFFER
    }

    override fun flush() {
        outputBuffer = AudioProcessor.EMPTY_BUFFER
        inputEnded = false
        bassState = 0f
    }

    override fun reset() {
        flush()
        buffer = AudioProcessor.EMPTY_BUFFER
        inputAudioFormat = AudioFormat.NOT_SET
        outputAudioFormat = AudioFormat.NOT_SET
    }
}
