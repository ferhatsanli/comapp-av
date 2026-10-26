package com.ferhat.comappav.ui.glass

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Pill = RoundedCornerShape(50)

/**
 * Glass pill button. [prominent] buttons are filled with the accent color, the others are
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
            .height(52.dp)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(Pill)
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
                // Top highlight so the pill looks slightly domed.
                drawRect(
                    Brush.verticalGradient(
                        colors = listOf(Color.White.copy(alpha = 0.28f), Color.Transparent),
                        endY = size.height * 0.55f,
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
                Pill,
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
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                letterSpacing = 0.4.sp,
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
            .height(36.dp)
            .clip(Pill)
            .background(palette.chipFill)
            .border(
                1.dp,
                Brush.linearGradient(
                    listOf(palette.rimTop, palette.rimBottom),
                ),
                Pill,
            )
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(text = text, color = palette.ink, fontSize = 13.sp, fontWeight = FontWeight.Medium)
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

val GlassFieldShape = RoundedCornerShape(16.dp)
