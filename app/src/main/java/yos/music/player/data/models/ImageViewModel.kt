package com.lyricsflow.player.data.models

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import com.lyricsflow.player.data.libraries.YosMediaItem

class ImageViewModel : ViewModel() {
    val recommendMusicList: MutableState<List<YosMediaItem>> = mutableStateOf(emptyList())
}
