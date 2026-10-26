package com.ferhat.comappav.ui.glass

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

/**
 * Everything that differs between the dark and the light glass look.
 * The backdrop uses only two hues (blue and violet) on a near-neutral base.
 */
@Immutable
class GlassPalette(
    val isDark: Boolean,
    val baseTop: Color,
    val baseMid: Color,
    val baseBottom: Color,
    val blobA: Color,
    val blobB: Color,
    val blobAlpha: Float,
    val bokeh: Color,
    val bokehAlpha: Float,
    /** Main text and icon color on glass. */
    val ink: Color,
    val frostTop: Color,
    val frostMid: Color,
    val frostBottom: Color,
    /** Edge light on the top-left of a glass edge and the shade on the bottom-right. */
    val rimTop: Color,
    val rimBottom: Color,
    val chipFill: Color,
    val fieldFill: Color,
    val shadow: Color,
    val accentGlow: Float,
    /** Opacity of the accent fill of prominent buttons, top and bottom. */
    val buttonTop: Float,
    val buttonBottom: Float,
) {
    val inkMuted: Color = ink.copy(alpha = 0.70f)
    val inkFaint: Color = ink.copy(alpha = 0.58f)
}

val DarkGlassPalette = GlassPalette(
    isDark = true,
    baseTop = Color(0xFF060A14),
    baseMid = Color(0xFF0B1226),
    baseBottom = Color(0xFF070C1B),
    blobA = Color(0xFF1E4FD8),
    blobB = Color(0xFF4B2FB8),
    blobAlpha = 0.55f,
    bokeh = Color(0xFFA8BCFF),
    bokehAlpha = 0.20f,
    ink = Color(0xFFE8ECF8),
    frostTop = Color.White.copy(alpha = 0.14f),
    frostMid = Color.White.copy(alpha = 0.04f),
    frostBottom = Color.White.copy(alpha = 0.08f),
    rimTop = Color.White.copy(alpha = 0.80f),
    rimBottom = Color.White.copy(alpha = 0.08f),
    chipFill = Color.White.copy(alpha = 0.09f),
    fieldFill = Color.White.copy(alpha = 0.06f),
    shadow = Color.Black.copy(alpha = 0.45f),
    accentGlow = 0.22f,
    buttonTop = 0.70f,
    buttonBottom = 0.35f,
)

val LightGlassPalette = GlassPalette(
    isDark = false,
    baseTop = Color(0xFFEEF2FA),
    baseMid = Color(0xFFE2E8F5),
    baseBottom = Color(0xFFD7DFF0),
    blobA = Color(0xFF7FA6FF),
    blobB = Color(0xFFA597F2),
    blobAlpha = 0.60f,
    bokeh = Color.White,
    bokehAlpha = 0.55f,
    ink = Color(0xFF0F172A),
    frostTop = Color.White.copy(alpha = 0.62f),
    frostMid = Color.White.copy(alpha = 0.34f),
    frostBottom = Color.White.copy(alpha = 0.46f),
    rimTop = Color.White.copy(alpha = 0.95f),
    rimBottom = Color(0xFF0F172A).copy(alpha = 0.14f),
    chipFill = Color.White.copy(alpha = 0.55f),
    fieldFill = Color.White.copy(alpha = 0.45f),
    shadow = Color(0xFF1E2A55).copy(alpha = 0.25f),
    accentGlow = 0.16f,
    buttonTop = 0.96f,
    buttonBottom = 0.80f,
)

val LocalGlassPalette = staticCompositionLocalOf { DarkGlassPalette }
