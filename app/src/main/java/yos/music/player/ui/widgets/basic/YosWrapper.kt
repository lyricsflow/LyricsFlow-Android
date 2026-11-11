package com.lyricsflow.player.ui.widgets.basic

import androidx.compose.runtime.Composable
import androidx.compose.runtime.NonRestartableComposable
import androidx.compose.runtime.key

@Composable
fun LyricsflowWrapper(content: @Composable () -> Unit) =
    key(content.hashCode()) {
        content()
    }