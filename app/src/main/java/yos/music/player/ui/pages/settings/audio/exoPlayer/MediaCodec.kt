package com.lyricsflow.player.ui.pages.settings.audio.exoPlayer

import android.media.MediaCodecList
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.lyricsflow.player.R
import com.lyricsflow.player.ui.pages.settings.DefaultItem
import com.lyricsflow.player.ui.pages.settings.SettingBackground
import com.lyricsflow.player.ui.widgets.basic.Title
import com.lyricsflow.player.ui.widgets.basic.lyricsflowRoundColumn

@Composable
fun MediaCodec(navController: NavController) =
    SettingBackground {
        val codecInfos = remember("MediaCodec_codecInfos") {
            mutableStateOf(MediaCodecList(MediaCodecList.ALL_CODECS).codecInfos.filter { !it.isEncoder })
        }
        Title(title = stringResource(id = R.string.settings_audio_exoplayer_support_mediacodec),
            onBack = {
                navController.popBackStack()
            },
            content = {
                lyricsflowRoundColumn {
                    itemsIndexed(
                        codecInfos.value,
                        key = { _, codec -> codec.name }/*,
                        contentType = { it.name }*/
                    ) { index, it ->
                        DefaultItem(
                            title = it.supportedTypes.joinToString(", "),
                            onClick = null,
                            desc = it.name
                        )

                        key(index) {
                            val needDivider = index < codecInfos.value.size - 1
                            if (needDivider) {
                                com.lyricsflow.player.ui.pages.settings.Divider()
                            }
                        }
                    }
                }
            }
        )
    }
