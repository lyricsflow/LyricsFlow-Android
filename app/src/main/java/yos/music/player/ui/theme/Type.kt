package com.lyricsflow.player.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.lyricsflow.player.R

@OptIn(ExperimentalTextApi::class)
val SFProDisplay = FontFamily(
    Font(
        resId = R.font.sf_pro_display_regular,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        resId = R.font.sf_pro_display_regular,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.sf_pro_display_medium,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.sf_pro_display_semibold,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.sf_pro_display_bold,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.sf_pro_display_heavy,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        resId = R.font.sf_pro_display_heavy,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

/**
 * Apple SF Arabic variable font family (`sfarabic.ttf` from `main/fonts/sfarabic.ttf`),
 * containing all weights (100-900) and widths in one variable font file.
 */
@OptIn(ExperimentalTextApi::class)
val SFArabic = FontFamily(
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Thin,
        variationSettings = FontVariation.Settings(FontVariation.weight(100))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.ExtraLight,
        variationSettings = FontVariation.Settings(FontVariation.weight(200))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Light,
        variationSettings = FontVariation.Settings(FontVariation.weight(300))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Normal,
        variationSettings = FontVariation.Settings(FontVariation.weight(400))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Medium,
        variationSettings = FontVariation.Settings(FontVariation.weight(500))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.SemiBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(600))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Bold,
        variationSettings = FontVariation.Settings(FontVariation.weight(700))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.ExtraBold,
        variationSettings = FontVariation.Settings(FontVariation.weight(800))
    ),
    Font(
        resId = R.font.sfarabic,
        weight = FontWeight.Black,
        variationSettings = FontVariation.Settings(FontVariation.weight(900))
    )
)

/**
 * Creates a dynamic variable [FontFamily] for [SFArabic] at the exact numeric [weight] (300..900)
 * and [width] percentage (75f..125f).
 */
@OptIn(ExperimentalTextApi::class)
fun sfArabicDynamicFamily(weight: Int, width: Float = 100f): FontFamily {
    val w = weight.coerceIn(100, 900)
    val wdth = width.coerceIn(75f, 125f)
    return FontFamily(
        Font(
            resId = R.font.sfarabic,
            weight = FontWeight(w),
            variationSettings = FontVariation.Settings(
                FontVariation.weight(w),
                FontVariation.width(wdth)
            )
        )
    )
}

val SFProRounded = FontFamily(
    Font(R.font.sf_pro_rounded_semibold, FontWeight.SemiBold),
    Font(R.font.sf_pro_rounded_semibold, FontWeight.Bold)
)

val Typography = Typography(
    displayLarge = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        letterSpacing = (-0.4).sp
    ),
    displayMedium = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 28.sp,
        lineHeight = 34.sp,
        letterSpacing = (-0.3).sp
    ),
    headlineLarge = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 30.sp,
        letterSpacing = (-0.25).sp
    ),
    headlineMedium = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 20.sp,
        lineHeight = 25.sp,
        letterSpacing = (-0.2).sp
    ),
    titleLarge = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Bold,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        letterSpacing = (-0.2).sp
    ),
    titleMedium = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.15).sp
    ),
    titleSmall = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 15.sp,
        lineHeight = 20.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = (-0.1).sp
    ),
    bodyMedium = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 14.sp,
        lineHeight = 19.sp,
        letterSpacing = (-0.05).sp
    ),
    bodySmall = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Normal,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelLarge = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
        letterSpacing = (-0.05).sp
    ),
    labelMedium = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        letterSpacing = 0.sp
    ),
    labelSmall = TextStyle(
        fontFamily = SFProDisplay,
        fontWeight = FontWeight.Medium,
        fontSize = 10.sp,
        lineHeight = 13.sp,
        letterSpacing = 0.sp
    )
)
