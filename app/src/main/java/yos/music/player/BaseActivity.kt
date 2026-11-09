package com.lyricsflow.player

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.viewModels
import androidx.core.view.WindowCompat
import androidx.lifecycle.ViewModelProvider
import com.tencent.mmkv.MMKV
import com.lyricsflow.player.data.models.MainViewModel
import com.lyricsflow.player.data.models.MediaViewModel

abstract class BaseActivity : ComponentActivity() /*{
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        WindowCompat.setDecorFitsSystemWindows(window, false)
    }
}*/

