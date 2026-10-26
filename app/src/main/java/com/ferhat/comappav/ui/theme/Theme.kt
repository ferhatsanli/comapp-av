package com.ferhat.comappav.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The glass look does not use dynamic color on purpose: the palette is fixed (blue/violet).
private val GlassDarkColorScheme = darkColorScheme(
    primary = Color(0xFF8FB0FF),
    onPrimary = Color(0xFF0A1B45),
    secondary = Color(0xFFA9A4F5),
    tertiary = Color(0xFFB7A4F0),
    background = Color(0xFF060A14),
    onBackground = Color(0xFFE8ECF8),
    surface = Color(0xFF111833),
    onSurface = Color(0xFFE8ECF8),
    error = Color(0xFFF87171),
)

private val GlassLightColorScheme = lightColorScheme(
    primary = Color(0xFF2F5BD0),
    onPrimary = Color.White,
    secondary = Color(0xFF5B54D6),
    tertiary = Color(0xFF6D45C9),
    background = Color(0xFFEEF2FA),
    onBackground = Color(0xFF0F172A),
    surface = Color(0xFFF4F6FC),
    onSurface = Color(0xFF0F172A),
    error = Color(0xFFC62F3A),
)

@Composable
fun ComAppAVTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) GlassDarkColorScheme else GlassLightColorScheme,
        typography = Typography,
        content = content
    )
}
