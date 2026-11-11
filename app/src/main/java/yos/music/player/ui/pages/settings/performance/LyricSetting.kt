package com.lyricsflow.player.ui.pages.settings.performance

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.lyricsflow.player.R
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.ui.pages.settings.GroupSpacer
import com.lyricsflow.player.ui.pages.settings.GroupSpacerMedium
import com.lyricsflow.player.ui.pages.settings.ListHeader
import com.lyricsflow.player.ui.pages.settings.SelectItem
import com.lyricsflow.player.ui.pages.settings.SettingBackground
import com.lyricsflow.player.ui.pages.settings.SwitchItem
import com.lyricsflow.player.ui.widgets.basic.RoundColumn
import com.lyricsflow.player.ui.widgets.basic.Title
import kotlin.math.roundToInt

@Composable
fun LyricSetting(navController: NavController) =
    SettingBackground {
        Title(title = stringResource(id = R.string.settings_performance_lyric_title),
            onBack = {
                navController.popBackStack()
            },
            content = {
                item("settings") {
                    Column(Modifier.fillMaxSize()) {
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style))

                        RoundColumn {
                            SelectItem(
                                title = stringResource(id = R.string.settings_performance_lyric_style_font_weight),
                                items = listOf(
                                    "Light",
                                    "Regular",
                                    "Medium",
                                    "SemiBold",
                                    "Bold",
                                    "ExtraBold",
                                    "Black"
                                ),
                                value = SettingsLibrary.LyricFontWeight,
                                onValueChange = {
                                    SettingsLibrary.LyricFontWeight = it
                                    SettingsLibrary.LyricFontWeightNumeric = when (it) {
                                        "Light" -> 300
                                        "Regular" -> 400
                                        "Medium" -> 500
                                        "SemiBold" -> 600
                                        "Bold" -> 700
                                        "ExtraBold" -> 800
                                        "Black" -> 900
                                        else -> 800
                                    }
                                }
                            )
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Font Weight (Variable)",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${SettingsLibrary.LyricFontWeightNumeric}",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Slider(
                                    value = SettingsLibrary.LyricFontWeightNumeric.toFloat(),
                                    onValueChange = { raw ->
                                        val stepped = ((raw / 100f).roundToInt() * 100).coerceIn(300, 900)
                                        SettingsLibrary.LyricFontWeightNumeric = stepped
                                        SettingsLibrary.LyricFontWeight = when (stepped) {
                                            300 -> "Light"
                                            400 -> "Regular"
                                            500 -> "Medium"
                                            600 -> "SemiBold"
                                            700 -> "Bold"
                                            800 -> "ExtraBold"
                                            900 -> "Black"
                                            else -> "ExtraBold"
                                        }
                                    },
                                    valueRange = 300f..900f,
                                    steps = 5,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                            Column(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 20.dp, vertical = 10.dp)
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "Font Width",
                                        fontSize = 15.sp,
                                        fontWeight = FontWeight.Medium
                                    )
                                    Text(
                                        text = "${SettingsLibrary.LyricFontWidth}%",
                                        fontSize = 14.sp,
                                        color = MaterialTheme.colorScheme.primary,
                                        fontWeight = FontWeight.SemiBold
                                    )
                                }
                                Slider(
                                    value = SettingsLibrary.LyricFontWidth.toFloat(),
                                    onValueChange = { raw ->
                                        SettingsLibrary.LyricFontWidth = raw.roundToInt().coerceIn(75, 125)
                                    },
                                    valueRange = 75f..125f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = MaterialTheme.colorScheme.primary,
                                        activeTrackColor = MaterialTheme.colorScheme.primary
                                    )
                                )
                            }
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_style_font_weight_desc))

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = "Show Translation",
                                onClick = {
                                    SettingsLibrary.LyricShowTranslation = !SettingsLibrary.LyricShowTranslation
                                },
                                checkedLambda = { SettingsLibrary.LyricShowTranslation }
                            )
                            SwitchItem(
                                title = "Show Pronunciation (Romanization)",
                                onClick = {
                                    SettingsLibrary.LyricShowPronunciation = !SettingsLibrary.LyricShowPronunciation
                                },
                                checkedLambda = { SettingsLibrary.LyricShowPronunciation }
                            )
                            SwitchItem(
                                title = "Replace Original with Pronunciation",
                                onClick = {
                                    SettingsLibrary.LyricSwapOriginalWithPronunciation = !SettingsLibrary.LyricSwapOriginalWithPronunciation
                                },
                                checkedLambda = { SettingsLibrary.LyricSwapOriginalWithPronunciation }
                            )
                            SwitchItem(
                                title = "Hide Swapped Original in Pronunciation Bar",
                                onClick = {
                                    SettingsLibrary.LyricHideOriginalInPronunciationBar = !SettingsLibrary.LyricHideOriginalInPronunciationBar
                                },
                                checkedLambda = { SettingsLibrary.LyricHideOriginalInPronunciationBar }
                            )
                        }

                        GroupSpacerMedium()

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_line_balance),
                                onClick = {
                                    SettingsLibrary.LyricLineBalance =
                                        !SettingsLibrary.LyricLineBalance
                                },
                                checkedLambda = { SettingsLibrary.LyricLineBalance }
                            )
                        }
                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_line_balance_desc))

                        GroupSpacer()

                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_others))

                        RoundColumn {
                            SwitchItem(
                                title = stringResource(id = R.string.settings_performance_lyric_blur_effect),
                                onClick = {
                                    SettingsLibrary.LyricBlurEffect =
                                        !SettingsLibrary.LyricBlurEffect
                                },
                                checkedLambda = { SettingsLibrary.LyricBlurEffect }
                            )
                        }

                        ListHeader(content = stringResource(id = R.string.settings_performance_lyric_blur_effect_desc))
                        GroupSpacer()
                    }
                }
            }
        )
    }