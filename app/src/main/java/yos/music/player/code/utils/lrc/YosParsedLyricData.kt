package com.lyricsflow.player.code.utils.lrc

import androidx.compose.runtime.Immutable

@Immutable
data class AmlSyllable(
    val text: String,
    val startTimeMs: Float,
    val endTimeMs: Float,
    val isPartOfWord: Boolean = false,
    val romanizedText: String? = null
)

@Immutable
data class AmlBackgroundLine(
    val startTimeMs: Float,
    val endTimeMs: Float,
    val syllables: List<AmlSyllable>,
    val placeAbove: Boolean = false,
    val translatedText: String? = null,
    val romanizedText: String? = null
) {
    val fullText: String
        get() = buildString {
            syllables.forEachIndexed { idx, syl ->
                append(syl.text)
                if (!syl.isPartOfWord && idx < syllables.lastIndex) {
                    append(' ')
                }
            }
        }

    val text: String
        get() = fullText

    val placement: String
        get() = if (placeAbove) "above" else "below"

    val translation: String?
        get() = translatedText

    val transliteration: String?
        get() = romanizedText
}

data class LyricsflowParsedLyricData(
    val entries: List<List<Pair<Float, String>>>,
    val endTimes: List<Float>,
    val otherSideForLines: List<Boolean>,
    val transliterations: List<String?>,
    val subtitles: List<String?>,
    val isTtml: Boolean,
    val syllablesForLines: List<List<AmlSyllable>> = emptyList(),
    val backgroundLines: List<AmlBackgroundLine?> = emptyList(),
    val songwriters: List<String> = emptyList(),
    val sections: Map<Int, String> = emptyMap()
)
