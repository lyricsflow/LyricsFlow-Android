package com.lyricsflow.player.ui.widgets.glass

import android.os.Build
import androidx.annotation.ChecksSdkIntAtLeast
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.lyricsflow.player.data.libraries.SettingsLibrary
import com.lyricsflow.player.ui.theme.isFlamingoInDarkMode
import com.lyricsflow.player.ui.widgets.backdrop.Backdrop
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.LayerBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.backdrops.rememberLayerBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.drawBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.effects.blur
import com.lyricsflow.player.ui.widgets.backdrop.effects.colorControls
import com.lyricsflow.player.ui.widgets.backdrop.effects.lens
import com.lyricsflow.player.ui.widgets.backdrop.emptyBackdrop
import com.lyricsflow.player.ui.widgets.backdrop.highlight.Highlight
import com.lyricsflow.player.ui.widgets.backdrop.shadow.InnerShadow
import com.lyricsflow.player.ui.widgets.backdrop.shadow.Shadow

/**
 * Ambient [Backdrop] capturing the main content layer behind floating chrome
 * (bottom nav, miniplayer, floating pills, sheets). Provided at the root of
 * the screen scaffold; consumers fall back to [emptyBackdrop] when not set.
 */
val LocalAppBackdrop = staticCompositionLocalOf<Backdrop> { emptyBackdrop() }

/**
 * When a [liquidGlass] surface sits inside a scope that provides a
 * [LayerBackdrop] here, it exports its own rendered glass into it so a child
 * lens (such as the tab bar's selection pill) can refract the bar's glass
 * without re-running the bar's effects.
 */
internal val LocalGlassExport = staticCompositionLocalOf<LayerBackdrop?> { null }

/**
 * Liquid glass is enabled ONLY on Android 13+ (API 33 / TIRAMISU), where AGSL
 * [android.graphics.RuntimeShader] refraction lenses are supported.
 * On Android < 13, falls back to normal non-liquid-glass bars.
 */
@ChecksSdkIntAtLeast(api = Build.VERSION_CODES.TIRAMISU)
fun isGlassSupported(): Boolean =
    Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

private const val FULL_BACKDROP_SCALE = 0.33f
private const val PERF_BACKDROP_SCALE = 0.25f

private val GlassBorderDark = Color.White.copy(alpha = 0.16f)
private val GlassBorderLight = Color.White.copy(alpha = 0.55f)

private val GlassTintDark = Color(0xFF1A1A1A).copy(alpha = 0.45f)
private val GlassTintLight = Color(0xFFF6F6FA).copy(alpha = 0.55f)

private val FrostTintDark = Color(0xFF1A1A1A).copy(alpha = 0.58f)
private val FrostTintLight = Color(0xFFF6F6FA).copy(alpha = 0.65f)

/**
 * Applies the Liquid Glass backdrop effect on Android 13+ (API 33+).
 * On Android < 13 or when blur is disabled, falls back to a normal non-liquid-glass surface.
 */
@Composable
fun Modifier.liquidGlass(
    backdrop: Backdrop = LocalAppBackdrop.current,
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 20.dp,
    refractionAmount: Dp = 24.dp,
    chromaticAberration: Boolean = false,
    depthEffect: Boolean = true,
    surfaceTint: Color? = null,
    highlight: Highlight? = Highlight.Default,
    shadow: Shadow? = Shadow.Default,
    innerShadow: InnerShadow? = null,
    onDrawSurface: (DrawScope.() -> Unit)? = null,
): Modifier {
    val reduceBlur = !SettingsLibrary.BarBlurEffect
    val isDark = isFlamingoInDarkMode()

    if (reduceBlur || !isGlassSupported()) {
        val fallbackColor = surfaceTint ?: if (isDark) {
            Color(0xFF1E1E22).copy(alpha = 0.94f)
        } else {
            Color(0xFFF2F2F7).copy(alpha = 0.94f)
        }
        val borderColor = if (isDark) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.08f)
        return this
            .clip(shape)
            .background(fallbackColor, shape)
            .border(BorderStroke(0.5.dp, borderColor), shape)
    }

    val exportedBackdrop = LocalGlassExport.current
    val scale = FULL_BACKDROP_SCALE
    val tint = surfaceTint ?: if (isDark) GlassTintDark else GlassTintLight

    return this.drawBackdrop(
        backdrop = backdrop,
        shape = { shape },
        backdropScale = scale,
        exportedBackdrop = exportedBackdrop,
        effects = {
            colorControls(saturation = 1.5f)
            blur(radius = blurRadius.toPx() * scale)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
                refractionHeight.value > 0f &&
                refractionAmount.value > 0f
            ) {
                lens(
                    refractionHeight = refractionHeight.toPx() * scale,
                    refractionAmount = refractionAmount.toPx() * scale,
                    depthEffect = depthEffect,
                    chromaticAberration = chromaticAberration,
                )
            }
        },
        highlight = { highlight },
        shadow = { shadow },
        innerShadow = { innerShadow },
        onDrawSurface = {
            drawRect(tint)
            onDrawSurface?.invoke(this)
        },
    )
}

/**
 * Convenience wrapper for circular liquid-glass buttons.
 */
@Composable
fun Modifier.liquidGlassCircle(
    backdrop: Backdrop = LocalAppBackdrop.current,
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 18.dp,
    surfaceTint: Color? = null,
    shadow: Shadow? = Shadow.Default,
): Modifier = liquidGlass(
    backdrop = backdrop,
    shape = CircleShape,
    blurRadius = blurRadius,
    refractionHeight = refractionHeight,
    refractionAmount = refractionAmount,
    chromaticAberration = false,
    depthEffect = false,
    surfaceTint = surfaceTint,
    shadow = shadow,
)

@Composable
fun LiquidGlassSurface(
    modifier: Modifier = Modifier,
    backdrop: Backdrop = LocalAppBackdrop.current,
    shape: Shape = RoundedCornerShape(28.dp),
    blurRadius: Dp = 8.dp,
    refractionHeight: Dp = 20.dp,
    refractionAmount: Dp = 24.dp,
    chromaticAberration: Boolean = false,
    surfaceTint: Color? = null,
    shadow: Shadow? = Shadow.Default,
    contentAlignment: Alignment = Alignment.Center,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier.liquidGlass(
            backdrop = backdrop,
            shape = shape,
            blurRadius = blurRadius,
            refractionHeight = refractionHeight,
            refractionAmount = refractionAmount,
            chromaticAberration = chromaticAberration,
            surfaceTint = surfaceTint,
            shadow = shadow,
        ),
        contentAlignment = contentAlignment,
        content = content,
    )
}

@Composable
fun rememberLiquidGlassBackdrop(): LayerBackdrop {
    val bg = MaterialTheme.colorScheme.background
    return rememberLayerBackdrop {
        drawRect(bg)
        drawContent()
    }
}
