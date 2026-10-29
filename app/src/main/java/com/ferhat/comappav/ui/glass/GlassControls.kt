package com.ferhat.comappav.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.TextFieldColors
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// macOS push buttons are rounded rectangles, not pills.
private val ButtonShape = RoundedCornerShape(10.dp)
private val ChipShape = RoundedCornerShape(8.dp)

/**
 * Glass push button in the macOS style. [prominent] buttons are filled with the accent color, the others are
 * plain frosted glass.
 */
@Composable
fun GlassButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
    prominent: Boolean = true,
    accent: Color = GlassColors.Neutral,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(ButtonShape)
            .drawBehind {
                if (prominent) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(
                                accent.copy(alpha = palette.buttonTop),
                                accent.copy(alpha = palette.buttonBottom),
                            ),
                        ),
                    )
                } else {
                    drawRect(palette.chipFill)
                }
                // Subtle top sheen, like an aqua push button.
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.18f), Color.Transparent),
                        endY = size.height * 0.50f,
                    ),
                )
            }
            .border(
                1.dp,
                Brush.linearGradient(
                    colors = listOf(palette.rimTop, palette.rimBottom),
                    start = Offset.Zero,
                    end = Offset(400f, 200f),
                ),
                ButtonShape,
            )
            .clickable(enabled = enabled && !loading, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        if (loading) {
            CircularProgressIndicator(
                modifier = Modifier.size(22.dp),
                strokeWidth = 2.dp,
                color = Color.White,
            )
        } else {
            Text(
                text = text,
                color = if (prominent) Color.White else palette.ink,
                fontWeight = FontWeight.Medium,
                fontSize = 14.sp,
            )
        }
    }
}

/** Small frosted chip, for quick presets and secondary actions. */
@Composable
fun GlassChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .height(32.dp)
            .clip(ChipShape)
            .background(palette.chipFill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(palette.rimTop, palette.rimBottom),
                ),
                ChipShape,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
    }
}

private const val NAV_BLUR_DP = 24f

/**
 * Frosted-glass dock for the bottom navigation bar, in the same macOS style as [GlassCard]: a
 * blurred copy of the aurora backdrop (aligned with the real one, so it reads as see-through
 * glass) under the same thin frost tint. It sits flush against the screen's left, right and
 * bottom edges, so the only edge it marks is the top one - a soft hairline that fades out
 * towards the sides, like a macOS toolbar's separator. No frame, no flat fill.
 */
@Composable
fun GlassNavSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val backdrop = LocalGlassBackdrop.current
    val palette = LocalGlassPalette.current
    var origin by remember { mutableStateOf(Offset.Zero) }
    Box(
        modifier
            .clipToBounds()
            .onGloballyPositioned { origin = it.positionInRoot() },
    ) {
        if (backdrop != null) {
            Canvas(
                Modifier
                    .matchParentSize()
                    .graphicsLayer {
                        val radius = NAV_BLUR_DP.dp.toPx()
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
            // Same fallback GlassCard uses when there is no backdrop to blur.
            Box(Modifier.matchParentSize().background(palette.baseMid))
        }
        Box(
            Modifier
                .matchParentSize()
                .drawBehind {
                    drawRect(
                        Brush.verticalGradient(listOf(palette.frostTop, palette.frostMid, palette.frostBottom)),
                    )
                    val hairline = palette.rimTop.copy(alpha = palette.rimTop.alpha * 0.55f)
                    drawRect(
                        brush = Brush.horizontalGradient(listOf(Color.Transparent, hairline, hairline, Color.Transparent)),
                        topLeft = Offset.Zero,
                        size = Size(size.width, 1.dp.toPx()),
                    )
                },
        )
        content()
    }
}

/**
 * One destination in a [GlassNavSurface] bottom bar. The selected one sits on a soft tinted
 * pill, like a selected row in a macOS sidebar; the tint fades in and out when switching.
 */
@Composable
fun GlassBottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(12.dp)
    val pill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.20f) else Color.Transparent,
        label = "navPill",
    )
    Row(
        modifier
            .clip(shape)
            .background(pill, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(7.dp)
                .clip(CircleShape)
                .background(if (selected) accent else palette.inkFaint, CircleShape),
        )
        Spacer(Modifier.width(7.dp))
        Text(
            text = label,
            color = if (selected) palette.ink else palette.inkMuted,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/**
 * A "folder" entry that opens one level deeper: accent dot, title, what's inside, and a
 * chevron pointing the way it will slide.
 */
@Composable
fun GlassFolderRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(16.dp)
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(palette.chipFill, shape)
            .border(1.dp, Brush.linearGradient(listOf(palette.rimTop, palette.rimBottom)), shape)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Box(
            Modifier
                .size(10.dp)
                .clip(CircleShape)
                .background(accent, CircleShape),
        )
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(text = title, color = palette.ink, fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
            Text(text = subtitle, color = palette.inkMuted, fontSize = 12.sp)
        }
        Spacer(Modifier.width(8.dp))
        Text(text = "›", color = palette.inkMuted, fontSize = 22.sp)
    }
}

/** Text field colors that stay readable on glass in both themes. */
@Composable
fun glassTextFieldColors(): TextFieldColors {
    val p = LocalGlassPalette.current
    return OutlinedTextFieldDefaults.colors(
        focusedTextColor = p.ink,
        unfocusedTextColor = p.ink,
        disabledTextColor = p.ink.copy(alpha = 0.5f),
        cursorColor = p.ink,
        focusedBorderColor = p.ink.copy(alpha = 0.70f),
        unfocusedBorderColor = p.ink.copy(alpha = 0.25f),
        focusedLabelColor = p.ink.copy(alpha = 0.85f),
        unfocusedLabelColor = p.ink.copy(alpha = 0.60f),
        focusedContainerColor = p.fieldFill,
        unfocusedContainerColor = p.fieldFill,
        disabledContainerColor = p.fieldFill.copy(alpha = p.fieldFill.alpha * 0.6f),
    )
}

val GlassFieldShape = RoundedCornerShape(10.dp)
