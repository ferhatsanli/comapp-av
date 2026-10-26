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
    val color: Color,
    val x: Float,
    val y: Float,
    val radius: Float,
    val driftX: Float,
    val driftY: Float,
    val speed: Int,
    val offset: Float,
)

private class Bokeh(
    val color: Color,
    val x: Float,
    val y: Float,
    val radius: Float,
    val drift: Float,
    val speed: Int,
    val offset: Float,
)

private val AuroraBlobs = listOf(
    Blob(Color(0xFF2BD9FF), 0.15f, 0.12f, 0.75f, 0.10f, 0.08f, 1, 0.00f),
    Blob(Color(0xFFFF3DA5), 0.90f, 0.30f, 0.70f, 0.12f, 0.10f, 1, 0.33f),
    Blob(Color(0xFF7C5CFF), 0.30f, 0.60f, 0.80f, 0.10f, 0.12f, 2, 0.60f),
    Blob(Color(0xFFFFA64D), 0.85f, 0.85f, 0.65f, 0.08f, 0.10f, 1, 0.15f),
    Blob(Color(0xFF2BFFB4), 0.10f, 0.92f, 0.55f, 0.08f, 0.08f, 2, 0.80f),
)

// Small, crisp shapes. They stay sharp on the open background and turn into soft
// bokeh behind a glass card, which is what makes the frosted effect readable.
private val BokehOrbs = listOf(
    Bokeh(Color(0xFFFFFFFF), 0.20f, 0.22f, 0.050f, 0.04f, 1, 0.10f),
    Bokeh(Color(0xFFFF8FD0), 0.78f, 0.14f, 0.070f, 0.05f, 1, 0.45f),
    Bokeh(Color(0xFF7FE8FF), 0.62f, 0.38f, 0.045f, 0.06f, 2, 0.20f),
    Bokeh(Color(0xFFFFFFFF), 0.12f, 0.50f, 0.080f, 0.05f, 1, 0.70f),
    Bokeh(Color(0xFFB9A8FF), 0.88f, 0.58f, 0.060f, 0.04f, 2, 0.90f),
    Bokeh(Color(0xFFFFD58A), 0.45f, 0.74f, 0.070f, 0.05f, 1, 0.30f),
    Bokeh(Color(0xFF7FFFD0), 0.74f, 0.90f, 0.050f, 0.06f, 2, 0.55f),
    Bokeh(Color(0xFFFFFFFF), 0.30f, 0.95f, 0.040f, 0.04f, 1, 0.05f),
)

/**
 * Draws the colorful backdrop into [area]. [phase] runs 0..1 and loops seamlessly because every
 * element uses a whole number of cycles per loop.
 */
internal fun DrawScope.drawAurora(phase: Float, area: Size) {
    if (area.width <= 0f || area.height <= 0f) return
    val unit = minOf(area.width, area.height)
    val twoPi = 2f * PI.toFloat()

    drawRect(
        brush = Brush.verticalGradient(
            colors = listOf(Color(0xFF0A0F24), Color(0xFF191039), Color(0xFF0B1B30)),
            startY = 0f,
            endY = area.height,
        ),
        size = area,
    )

    for (blob in AuroraBlobs) {
        val angle = twoPi * (phase * blob.speed + blob.offset)
        val center = Offset(
            x = area.width * blob.x + cos(angle) * unit * blob.driftX,
            y = area.height * blob.y + sin(angle) * unit * blob.driftY,
        )
        val radius = unit * blob.radius
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(blob.color.copy(alpha = 0.85f), blob.color.copy(alpha = 0f)),
                center = center,
                radius = radius,
            ),
            radius = radius,
            center = center,
        )
    }

    for (orb in BokehOrbs) {
        val angle = twoPi * (phase * orb.speed + orb.offset)
        val center = Offset(
            x = area.width * orb.x + cos(angle) * unit * orb.drift,
            y = area.height * orb.y + sin(angle) * unit * orb.drift,
        )
        val radius = unit * orb.radius
        drawCircle(color = orb.color.copy(alpha = 0.50f), radius = radius, center = center)
        drawCircle(
            color = Color.White.copy(alpha = 0.55f),
            radius = radius,
            center = center,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = unit * 0.006f),
        )
    }
}

/** Full-screen animated backdrop. Everything inside it can use [GlassCard]. */
@Composable
fun GlassScreen(
    modifier: Modifier = Modifier,
    content: @Composable BoxScope.() -> Unit,
) {
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
        LocalContentColor provides Color.White,
    ) {
        Box(
            modifier
                .fillMaxSize()
                .onSizeChanged { rootSize.value = it },
        ) {
            Canvas(Modifier.fillMaxSize()) {
                drawAurora(phase.value, size)
            }
            content()
        }
    }
}
