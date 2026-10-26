package com.ferhat.comappav.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private const val FROST_BLUR_DP = 18f

/**
 * Frosted-glass surface in the style of macOS / iOS:
 *  1. a blurred copy of the backdrop, aligned with the real one so it looks see-through,
 *  2. a light frost tint, a soft accent glow and a vertical sheen,
 *  3. a specular rim plus a few fading inner strokes that read as the thick, curved glass lip.
 */
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Neutral,
    cornerRadius: Dp = 28.dp,
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(cornerRadius)
    val backdrop = LocalGlassBackdrop.current
    val palette = LocalGlassPalette.current
    var origin by remember { mutableStateOf(Offset.Zero) }

    Box(
        modifier
            .shadow(
                elevation = 20.dp,
                shape = shape,
                ambientColor = palette.shadow,
                spotColor = accent.copy(alpha = 0.35f),
            )
            .clip(shape)
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        if (backdrop != null) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val radius = FROST_BLUR_DP.dp.toPx()
                        renderEffect = BlurEffect(radius, radius, TileMode.Clamp)
                    },
            ) {
                val root = backdrop.rootSize.value
                translate(left = -origin.x, top = -origin.y) {
                    drawAurora(
                        phase = backdrop.phase.value,
                        area = Size(root.width.toFloat(), root.height.toFloat()),
                        palette = palette,
                    )
                }
            }
        } else {
            Box(Modifier.matchParentSize().background(palette.baseMid))
        }
        Box(Modifier.matchParentSize().drawBehind { drawFrost(accent, palette) })
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            content = content,
        )
        Box(Modifier.matchParentSize().drawBehind { drawRim(accent, cornerRadius.toPx(), palette) })
    }
}

private fun DrawScope.drawFrost(accent: Color, palette: GlassPalette) {
    drawRect(
        brush = Brush.verticalGradient(
            listOf(palette.frostTop, palette.frostMid, palette.frostBottom),
        ),
    )
    drawRect(
        brush = Brush.radialGradient(
            colors = listOf(accent.copy(alpha = palette.accentGlow), Color.Transparent),
            center = Offset.Zero,
            radius = size.maxDimension * 0.9f,
        ),
    )
}

private fun DrawScope.drawRim(accent: Color, corner: Float, palette: GlassPalette) {
    val outerStroke = 1.2.dp.toPx()
    val specular = Brush.linearGradient(
        colors = listOf(
            palette.rimTop,
            palette.rimBottom,
            accent.copy(alpha = 0.30f),
            palette.rimTop.copy(alpha = palette.rimTop.alpha * 0.5f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    drawRoundRect(
        brush = specular,
        topLeft = Offset(outerStroke / 2f, outerStroke / 2f),
        size = Size(size.width - outerStroke, size.height - outerStroke),
        cornerRadius = CornerRadius(corner),
        style = Stroke(width = outerStroke),
    )

    // Inner lip: strokes that fade towards the middle, brighter on the top-left edge.
    val lipBrush = Brush.linearGradient(
        colors = listOf(
            palette.rimTop.copy(alpha = palette.rimTop.alpha * 0.7f),
            palette.rimBottom.copy(alpha = palette.rimBottom.alpha * 0.5f),
            accent.copy(alpha = 0.20f),
        ),
        start = Offset.Zero,
        end = Offset(size.width, size.height),
    )
    val steps = 5
    val lipStroke = 1.6.dp.toPx()
    for (i in 0 until steps) {
        val inset = outerStroke + lipStroke * (i + 0.5f)
        drawRoundRect(
            brush = lipBrush,
            topLeft = Offset(inset, inset),
            size = Size(size.width - inset * 2f, size.height - inset * 2f),
            cornerRadius = CornerRadius((corner - inset).coerceAtLeast(0f)),
            alpha = 0.30f * (1f - i / steps.toFloat()),
            style = Stroke(width = lipStroke),
        )
    }
}

/** Glowing glass bead used as the icon of a command card. */
@Composable
fun GlassOrb(accent: Color, modifier: Modifier = Modifier, size: Dp = 40.dp) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .drawBehind {
                drawRect(
                    brush = Brush.radialGradient(
                        colors = listOf(
                            Color.White.copy(alpha = 0.75f),
                            accent.copy(alpha = 0.95f),
                            accent.copy(alpha = 0.55f),
                        ),
                        center = Offset(this.size.width * 0.32f, this.size.height * 0.28f),
                        radius = this.size.maxDimension * 0.9f,
                    ),
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(palette.rimTop, palette.rimBottom),
                ),
                CircleShape,
            ),
    )
}

/** Small monospace label such as the action name of a command. */
@Composable
fun GlassTag(text: String, modifier: Modifier = Modifier) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(50)
    Box(
        modifier
            .clip(shape)
            .background(palette.chipFill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(palette.rimTop, palette.rimBottom),
                ),
                shape,
            )
            .padding(horizontal = 10.dp, vertical = 4.dp),
    ) {
        Text(
            text = text,
            fontFamily = FontFamily.Monospace,
            fontSize = 11.sp,
            letterSpacing = 1.sp,
            color = palette.ink.copy(alpha = 0.85f),
        )
    }
}

/**
 * The shared template for every command: orb, title, short description and the action tag on
 * top, the command's own controls below. All commands look the same, only [accent] changes.
 */
@Composable
fun GlassCommandCard(
    title: String,
    tag: String,
    description: String,
    accent: Color,
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit,
) {
    val palette = LocalGlassPalette.current
    GlassCard(modifier = modifier.fillMaxWidth(), accent = accent) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            GlassOrb(accent)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodySmall,
                    color = palette.inkMuted,
                )
            }
            Spacer(Modifier.width(8.dp))
            GlassTag(tag)
        }
        Spacer(Modifier.height(4.dp))
        content()
    }
}
