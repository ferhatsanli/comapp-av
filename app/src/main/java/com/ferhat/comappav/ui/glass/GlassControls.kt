package com.ferhat.comappav.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.RectangleShape
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

/**
 * Flat frosted dock for navigation - a bottom bar in portrait, a sidebar in landscape. Lighter
 * than [GlassCard]: no blur backdrop or floating shadow, since it's a fixed dock, not a card
 * that floats over the aurora background.
 */
@Composable
fun GlassNavSurface(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            // frostTop/Mid/Bottom are a thin tint meant to sit on top of something opaque
            // (GlassCard layers them over a blurred backdrop) - without a base fill under it,
            // this dock would be almost see-through, especially in dark mode.
            .background(palette.baseMid)
            .background(
                Brush.verticalGradient(listOf(palette.frostTop, palette.frostMid, palette.frostBottom)),
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(listOf(palette.rimTop, palette.rimBottom)),
                shape = RectangleShape,
            ),
    ) {
        content()
    }
}

/** One destination in a [GlassNavSurface] bottom bar: a small accent bar above the label. */
@Composable
fun GlassBottomNavItem(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    Column(
        modifier
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(
            Modifier
                .size(width = 28.dp, height = 3.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(if (selected) accent else Color.Transparent),
        )
        Spacer(Modifier.height(6.dp))
        Text(
            text = label,
            color = if (selected) accent else palette.inkMuted,
            fontSize = 12.sp,
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
