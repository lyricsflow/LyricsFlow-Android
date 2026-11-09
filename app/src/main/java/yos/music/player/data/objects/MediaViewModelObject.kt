package com.lyricsflow.player.data.objects

import android.net.Uri
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.Stable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import com.lyricsflow.player.code.utils.lrc.AmlBackgroundLine
import com.lyricsflow.player.code.utils.lrc.AmlSyllable

@Stable
object MediaViewModelObject {
    val lrcEntries: MutableState<List<List<Pair<Float, String>>>> = mutableStateOf(listOf())
    val otherSideForLines = mutableStateListOf<Boolean>()
    val lyricLineEndTimes = mutableStateListOf<Float>()
    val lyricLineTransliterations = mutableStateListOf<String?>()
    val lyricLineSubtitles = mutableStateListOf<String?>()
    val isTtmlLyrics = mutableStateOf(false)

    // Rich AML Lyric Metadata ported from main/js/ttml-parser.js
    val lyricLineSyllables = mutableStateListOf<List<AmlSyllable>>()
    val lyricBackgroundLines = mutableStateListOf<AmlBackgroundLine?>()
    val lyricSongwriters = mutableStateListOf<String>()
    val lyricSections = mutableStateMapOf<Int, String>()

    val bitmap: MutableState<Uri?> = mutableStateOf(null)

    val isPlaying: MutableState<Boolean> = mutableStateOf(false)

    val bitrate = mutableIntStateOf(0)
    val samplingRate = mutableIntStateOf(0)
    val isDolby = mutableStateOf(false)

    fun updateLyricMetadata(
        endTimes: List<Float>,
        transliterations: List<String?>,
        subtitles: List<String?>,
        ttmlLyrics: Boolean,
        syllables: List<List<AmlSyllable>> = emptyList(),
        backgrounds: List<AmlBackgroundLine?> = emptyList(),
        songwriters: List<String> = emptyList(),
        sections: Map<Int, String> = emptyMap()
    ) {
        lyricLineEndTimes.clear()
        lyricLineEndTimes.addAll(endTimes)
        lyricLineTransliterations.clear()
        lyricLineTransliterations.addAll(transliterations)
        lyricLineSubtitles.clear()
        lyricLineSubtitles.addAll(subtitles)
        isTtmlLyrics.value = ttmlLyrics

        lyricLineSyllables.clear()
        lyricLineSyllables.addAll(syllables)
        lyricBackgroundLines.clear()
        lyricBackgroundLines.addAll(backgrounds)
        lyricSongwriters.clear()
        lyricSongwriters.addAll(songwriters)
        lyricSections.clear()
        lyricSections.putAll(sections)
    }
}
