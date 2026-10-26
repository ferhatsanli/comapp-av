package com.ferhat.comappav.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// The glass look is built for a dark, colorful backdrop, so the app always uses this scheme.
private val GlassColorScheme = darkColorScheme(
    primary = Color(0xFF8FD8FF),
    onPrimary = Color(0xFF00263A),
    secondary = Color(0xFFFF9AD0),
    tertiary = Color(0xFFB9A8FF),
    background = Color(0xFF0A0F24),
    onBackground = Color.White,
    surface = Color(0xFF151A33),
    onSurface = Color.White,
    error = Color(0xFFFF8A9A),
)

@Composable
fun ComAppAVTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = GlassColorScheme,
        typography = Typography,
        content = content
    )
}
