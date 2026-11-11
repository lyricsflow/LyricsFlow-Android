package com.lyricsflow.player.data.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.lyricsflow.player.data.libraries.LyricsflowMediaItem

class ImageViewModel : ViewModel() {
    val recommendMusicList: MutableState<List<LyricsflowMediaItem>> = mutableStateOf(emptyList())
}
