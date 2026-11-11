package com.lyricsflow.player.ui.pages.settings.others

import android.util.Base64
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import com.blankj.utilcode.util.AppUtils
import com.lyricsflow.player.R
import com.lyricsflow.player.ui.pages.settings.DefaultItem
import com.lyricsflow.player.ui.pages.settings.GroupSpacer
import com.lyricsflow.player.ui.pages.settings.LabelItem
import com.lyricsflow.player.ui.pages.settings.ListHeader
import com.lyricsflow.player.ui.pages.settings.SettingBackground
import com.lyricsflow.player.ui.pages.settings.startWeb
import com.lyricsflow.player.ui.widgets.basic.RoundColumn
import com.lyricsflow.player.ui.widgets.basic.Title

@Composable
fun About(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_others_about),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        val context = LocalContext.current

                        ListHeader(content = stringResource(id = R.string.settings_others_about_info))
                        val appVersion = remember("About_appVersion") {
                            mutableStateOf(AppUtils.getAppVersionName())
                        }
                        val telegramLink = "aHR0cHM6Ly90Lm1lL0ZsYW1pbmdvVXBkYXRlQW5kTmV3cw=="

                        RoundColumn {
                            DefaultItem(
                                title = stringResource(id = R.string.app_name),
                                onClick = null,
                                desc = appVersion.value
                            )
                        }

                        GroupSpacer()

                        ListHeader(content = stringResource(id = R.string.settings_others_about_developers))
                        RoundColumn {
                            LabelItem(
                                title = "LyricsFlow Team",
                                desc = stringResource(id = R.string.settings_others_about_developers_lyricsflow_x)
                            ) {
                                startWeb(
                                    url = "https://github.com/lyricsflow",
                                    context
                                )
                            }
                        }

                        GroupSpacer()

                        RoundColumn {
                            LabelItem(
                                title = "LyricsFlow GitHub",
                                superLink = true
                            ) {
                                startWeb(
                                    url = "https://github.com/lyricsflow/LyricsFlow-Android",
                                    context
                                )
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_others_about_contact_us_desc))
                    }
                }
            }
        )
    }