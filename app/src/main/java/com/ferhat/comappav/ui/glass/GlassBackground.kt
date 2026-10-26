package com.ferhat.comappav.ui.glass

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Stable
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.unit.IntSize
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

/**
 * Shared description of the animated backdrop. Glass cards draw their own blurred copy of it,
 * so they need the animation phase and the size of the root area.
 */
@Stable
class GlassBackdrop(val phase: State<Float>, val rootSize: State<IntSize>)

val LocalGlassBackdrop = staticCompositionLocalOf<GlassBackdrop?> { null }

private class Blob(
    val useSecondHue: Boolean,
    val x: Float,
    val y: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val speed: Int,
    val offset: Float,
)

/** A soft, blurry ring of light: a radial gradient that peaks at the ring radius and fades both ways. */
private class Halo(
    val x: Float,
    val y: Float,
    val radius: Float,
    val drift: Float,
    val speed: Int,
    val offset: Float,
)

private val AuroraBlobs = listOf(
    Blob(false, 0.15f, 0.12f, 0.80f, 0.10f, 0.08f, 1, 0.00f),
    Blob(true, 0.90f, 0.35f, 0.75f, 0.12f, 0.10f, 1, 0.33f),
    Blob(false, 0.85f, 0.88f, 0.65f, 0.08f, 0.10f, 2, 0.15f),
    Blob(true, 0.10f, 0.85f, 0.60f, 0.08f, 0.08f, 2, 0.80f),
)

// Large, faint rings. They have no hard edge anywhere, so they read as bands of light
// and turn into an even softer glow behind a glass card.
private val Halos = listOf(
    Halo(0.22f, 0.20f, 0.40f, 0.04f, 1, 0.10f),
    Halo(0.85f, 0.45f, 0.55f, 0.05f, 1, 0.50f),
    Halo(0.30f, 0.82f, 0.50f, 0.05f, 2, 0.30f),
)

/**
 * Draws the backdrop into [area]. [phase] runs 0..1 and loops seamlessly because every
 * element uses a whole number of cycles per loop.
 */
internal fun DrawScope.drawAurora(phase: Float, area: Size, palette: GlassPalette) {
    if (area.width <= 0f || area.height <= 0f) return
    val unit = minOf(area.width, area.height)
    val twoPi = 2f * PI.toFloat()

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(palette.baseTop, palette.baseMid, palette.baseBottom),
            startY = 0f,
            endY = area.height,
        ),
        size = area,
    )

    for (blob in AuroraBlobs) {
        val hue = if (blob.useSecondHue) palette.blobB else palette.blobA
        val angle = twoPi * (phase * blob.speed + blob.offset)
        val center = Offset(
            x = area.width * blob.x + cos(angle) * unit * blob.driftX,
            y = area.height * blob.y + sin(angle) * unit * blob.driftY,
        )
        val radius = unit * blob.radius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(hue.copy(alpha = palette.blobAlpha), hue.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    for (halo in Halos) {
        val angle = twoPi * (phase * halo.speed + halo.offset)
        val center = Offset(
            x = area.width * halo.x + cos(angle) * unit * halo.drift,
            y = area.height * halo.y + sin(angle) * unit * halo.drift,
        )
        // Two concentric bands per halo; the inner one is fainter.
        for ((scale, strength) in listOf(1.0f to 1.0f, 0.68f to 0.55f)) {
            val ringRadius = unit * halo.radius * scale
            val gradientRadius = ringRadius * 1.5f
            val mid = ringRadius / gradientRadius
            val color = palette.bokeh.copy(alpha = palette.bokehAlpha * strength)
            drawCircle(
                brush = Brush.radialGradient(
                    colorStops = arrayOf(
                        0f to Color.Transparent,
                        (mid - 0.20f) to Color.Transparent,
                        mid to color,
                        (mid + 0.20f) to Color.Transparent,
                        1f to Color.Transparent,
                    ),
                    center = center,
                    radius = gradientRadius,
                ),
                radius = gradientRadius,
                center = center,
            )
        }
    }
}

/** Full-screen animated backdrop. Everything inside it can use [GlassCard]. */
@Composable
fun GlassScreen(
    modifier: Modifier = Modifier,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable BoxScope.() -> Unit,
) {
    val palette = if (darkTheme) DarkGlassPalette else LightGlassPalette
    val transition = rememberInfiniteTransition(label = "aurora")
    val phase = transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 60_000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart,
        ),
        label = "auroraPhase",
    )
    val rootSize = remember { mutableStateOf(IntSize.Zero) }
    val backdrop = remember { GlassBackdrop(phase, rootSize) }

    CompositionLocalProvider(
        LocalGlassBackdrop provides backdrop,
        LocalGlassPalette provides palette,
        LocalContentColor provides palette.ink,
    ) {
        Box(
            modifier
                .fillMaxSize()
                .onSizeChanged { rootSize.value = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawAurora(phase.value, size, palette)
            }
            content()
        }
    }
}
