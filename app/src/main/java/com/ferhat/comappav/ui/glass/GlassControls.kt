package com.ferhat.comappav.ui.glass

import androidx.compose.animation.animateColorAsState
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.graphics.PathMeasure
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Switch
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.LinearEasing
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
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
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlurEffect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

// macOS push buttons are rounded rectangles, not pills.
private val ButtonCornerRadius = 10.dp
private val ButtonShape = RoundedCornerShape(ButtonCornerRadius)
private val ChipShape = RoundedCornerShape(8.dp)

/**
 * "Working on it": a short beam of white light running clockwise around the edge of whatever it
 * covers (a button, a row), over a soft glow. The beam is about a fifth of the way round. It
 * fades in when [active] turns on and out when it turns off; while off it draws nothing and
 * runs no animation.
 */
@Composable
fun OrbitingLight(active: Boolean, cornerRadius: Dp, modifier: Modifier = Modifier, color: Color = Color.White) {
    val strength by animateFloatAsState(
        targetValue = if (active) 1f else 0f,
        animationSpec = tween(durationMillis = 300),
        label = "orbitStrength",
    )
    if (strength == 0f) return
    val turn = rememberInfiniteTransition(label = "orbit")
    val progress by turn.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(durationMillis = ORBIT_MILLIS, easing = LinearEasing)),
        label = "orbitProgress",
    )
    val measure = remember { PathMeasure() }
    val piece = remember { Path() }
    Canvas(modifier) {
        val core = 2.dp.toPx()
        val inset = core / 2f
        val radius = (cornerRadius.toPx() - inset).coerceAtLeast(0f)
        val outline = Path().apply {
            addRoundRect(
                RoundRect(
                    left = inset, top = inset, right = size.width - inset, bottom = size.height - inset,
                    cornerRadius = CornerRadius(radius, radius),
                ),
                Path.Direction.Clockwise,
            )
        }
        measure.setPath(outline, true)
        val length = measure.length
        if (length <= 0f) return@Canvas
        val beam = length * ORBIT_BEAM_FRACTION
        val head = progress * length
        // Brightest at the head, fading to nothing at the tail.
        for (i in 0 until ORBIT_PIECES) {
            val from = head - beam * (i + 1) / ORBIT_PIECES
            val to = head - beam * i / ORBIT_PIECES
            val fade = (1f - i.toFloat() / ORBIT_PIECES) * strength
            for ((start, stop) in wrapSegment(from, to, length)) {
                piece.reset()
                if (!measure.getSegment(start, stop, piece, true)) continue
                drawPath(piece, color.copy(alpha = 0.22f * fade), style = Stroke(width = core * 3.5f, cap = StrokeCap.Round))
                drawPath(piece, color.copy(alpha = fade), style = Stroke(width = core, cap = StrokeCap.Round))
            }
        }
    }
}

/** [from, to] along a closed outline of [length], split in two where it crosses the start. */
private fun wrapSegment(from: Float, to: Float, length: Float): List<Pair<Float, Float>> {
    val a = ((from % length) + length) % length
    val b = ((to % length) + length) % length
    return if (a <= b) listOf(a to b) else listOf(a to length, 0f to b)
}

private const val ORBIT_MILLIS = 1600
private const val ORBIT_BEAM_FRACTION = 0.2f
private const val ORBIT_PIECES = 14

/**
 * A row like [GlassFolderRow] holding just a title and an on/off switch. [working] runs the
 * orbiting light around it while a change is on its way.
 */
@Composable
fun GlassSwitchRow(
    title: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    enabled: Boolean = true,
    working: Boolean = false,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(SwitchRowCornerRadius)
    Box(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(shape)
                .background(palette.chipFill, shape)
                .border(1.dp, Brush.linearGradient(listOf(palette.rimTop, palette.rimBottom)), shape)
                .padding(horizontal = 16.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(Modifier.weight(1f)) {
                Text(title, color = palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                if (subtitle != null) {
                    Text(subtitle, color = palette.inkMuted, fontSize = 12.sp)
                }
            }
            Switch(
                checked = checked,
                onCheckedChange = onCheckedChange,
                enabled = enabled,
                colors = SwitchDefaults.colors(
                    checkedTrackColor = accent,
                    checkedThumbColor = Color.White,
                    uncheckedTrackColor = palette.chipFill,
                ),
            )
        }
        OrbitingLight(active = working, cornerRadius = SwitchRowCornerRadius, modifier = Modifier.matchParentSize())
    }
}

private val SwitchRowCornerRadius = 16.dp

/**
 * A single line of text that shrinks its own font, step by step, until it fits - so a label in a
 * tight space loses size before it ever loses its last letters. Used wherever a fixed-width slot
 * (the bottom nav today) has to hold labels of very different lengths.
 */
@Composable
fun AutoSizeText(
    text: String,
    modifier: Modifier = Modifier,
    color: Color,
    maxFontSize: TextUnit,
    minFontSize: TextUnit = 7.sp,
    fontWeight: FontWeight? = null,
    textAlign: TextAlign = TextAlign.Center,
) {
    var fontSize by remember(text, maxFontSize) { mutableFloatStateOf(maxFontSize.value) }
    var settled by remember(text, maxFontSize) { mutableStateOf(false) }
    Text(
        text = text,
        modifier = modifier.drawWithContent { if (settled) drawContent() },
        color = color,
        fontSize = fontSize.sp,
        fontWeight = fontWeight,
        maxLines = 1,
        softWrap = false,
        textAlign = textAlign,
        onTextLayout = { result ->
            if (result.didOverflowWidth && fontSize > minFontSize.value) {
                fontSize = (fontSize - 1f).coerceAtLeast(minFontSize.value)
            } else {
                settled = true
            }
        },
    )
}

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
    working: Boolean = false,
) {
    Box(
        modifier
            .fillMaxWidth()
            .height(44.dp),
    ) {
        GlassButtonFace(text, onClick, enabled, loading, prominent, accent)
        // On top of the (possibly dimmed) face, so it shows at full strength on a disabled button.
        OrbitingLight(active = working, cornerRadius = ButtonCornerRadius, modifier = Modifier.matchParentSize())
    }
}

@Composable
private fun GlassButtonFace(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean,
    loading: Boolean,
    prominent: Boolean,
    accent: Color,
) {
    val palette = LocalGlassPalette.current
    Box(
        Modifier
            .fillMaxSize()
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

/**
 * Small frosted chip, for quick presets and secondary actions. A [selected] chip (one choice of
 * several) is filled with [accent], like a prominent button.
 */
@Composable
fun GlassChip(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .height(32.dp)
            .clip(ChipShape)
            .drawBehind {
                if (selected) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = palette.buttonTop), accent.copy(alpha = palette.buttonBottom)),
                        ),
                    )
                } else {
                    drawRect(palette.chipFill)
                }
            }
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
        Text(
            text = text,
            color = if (selected) Color.White else palette.ink,
            fontSize = 13.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Thin glass progress bar; [fraction] runs 0..1. */
@Composable
fun GlassProgressBar(
    fraction: Float,
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Blue,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(3.dp)
    Box(
        modifier
            .fillMaxWidth()
            .height(6.dp)
            .clip(shape)
            .background(palette.chipFill, shape),
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .background(
                    Brush.horizontalGradient(
                        listOf(accent.copy(alpha = palette.buttonBottom), accent.copy(alpha = palette.buttonTop)),
                    ),
                    shape,
                ),
        )
    }
}

private const val NAV_BLUR_DP = 24f

/**
 * Square icon-only button in the same glass material and height as [GlassChip], so the two sit
 * side by side naturally. While [active] it is filled with [accent] (like a pressed toggle).
 * [icon] draws the symbol in the given tint.
 */
@Composable
fun GlassIconButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    active: Boolean = false,
    accent: Color = GlassColors.Blue,
    icon: DrawScope.(tint: Color) -> Unit,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .size(32.dp)
            .clip(ChipShape)
            .drawBehind {
                if (active) {
                    drawRect(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = palette.buttonTop), accent.copy(alpha = palette.buttonBottom)),
                        ),
                    )
                } else {
                    drawRect(palette.chipFill)
                }
            }
            .border(1.dp, Brush.linearGradient(listOf(palette.rimTop, palette.rimBottom)), ChipShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        val tint = if (active) Color.White else palette.ink
        Canvas(Modifier.size(20.dp)) { icon(tint) }
    }
}

/**
 * Same small square glass button as [GlassIconButton], with any content inside instead of a
 * drawn symbol (an emoji flag, for instance).
 */
@Composable
fun GlassContentButton(
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit,
) {
    val palette = LocalGlassPalette.current
    Box(
        modifier
            .size(32.dp)
            .clip(ChipShape)
            .background(palette.chipFill)
            .border(1.dp, Brush.linearGradient(listOf(palette.rimTop, palette.rimBottom)), ChipShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        content()
    }
}

/**
 * "Reorder" symbol: three horizontal lines, with a chevron above them pointing up and one
 * below pointing down - a ">" / "<" turned 90 degrees.
 */
fun DrawScope.drawReorderIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val strokeWidth = 1.5.dp.toPx()
    for (y in listOf(0.36f, 0.5f, 0.64f)) {
        drawLine(
            color = tint,
            start = Offset(w * 0.2f, h * y),
            end = Offset(w * 0.8f, h * y),
            strokeWidth = strokeWidth,
            cap = StrokeCap.Round,
        )
    }
    val chevron = Stroke(width = strokeWidth, cap = StrokeCap.Round, join = StrokeJoin.Round)
    val up = Path().apply {
        moveTo(w * 0.33f, h * 0.21f)
        lineTo(w * 0.5f, h * 0.07f)
        lineTo(w * 0.67f, h * 0.21f)
    }
    val down = Path().apply {
        moveTo(w * 0.33f, h * 0.79f)
        lineTo(w * 0.5f, h * 0.93f)
        lineTo(w * 0.67f, h * 0.79f)
    }
    drawPath(path = up, color = tint, style = chevron)
    drawPath(path = down, color = tint, style = chevron)
}

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
    icon: DrawScope.(tint: Color) -> Unit,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(12.dp)
    val pill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.20f) else Color.Transparent,
        label = "navPill",
    )
    val tint = if (selected) accent else palette.inkMuted
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(pill, shape)
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp, horizontal = 2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Canvas(Modifier.size(20.dp)) { icon(tint) }
        Spacer(Modifier.height(3.dp))
        AutoSizeText(
            text = label,
            modifier = Modifier.fillMaxWidth(),
            color = if (selected) palette.ink else palette.inkMuted,
            maxFontSize = 10.sp,
            minFontSize = 7.sp,
            fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
        )
    }
}

/** Terminal prompt - ">" and a cursor underscore - for the Commands tab. */
fun DrawScope.drawCommandsIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    val chevron = Path().apply {
        moveTo(w * 0.22f, h * 0.28f)
        lineTo(w * 0.46f, h * 0.5f)
        lineTo(w * 0.22f, h * 0.72f)
    }
    drawPath(chevron, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(
        color = tint,
        start = Offset(w * 0.54f, h * 0.72f),
        end = Offset(w * 0.80f, h * 0.72f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** Power button symbol - a broken circle with a vertical tick through the gap - for Power. */
fun DrawScope.drawPowerIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    val cx = w / 2f
    val cy = h * 0.58f
    val r = w * 0.32f
    drawArc(
        color = tint,
        startAngle = -55f,
        sweepAngle = 290f,
        useCenter = false,
        topLeft = Offset(cx - r, cy - r),
        size = Size(r * 2, r * 2),
        style = Stroke(width = stroke, cap = StrokeCap.Round),
    )
    drawLine(
        color = tint,
        start = Offset(cx, h * 0.12f),
        end = Offset(cx, cy - r * 0.1f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** Three connected nodes - a tiny network graph - for Network. */
fun DrawScope.drawNetworkIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.4.dp.toPx()
    val top = Offset(w * 0.5f, h * 0.16f)
    val left = Offset(w * 0.20f, h * 0.80f)
    val right = Offset(w * 0.80f, h * 0.80f)
    drawLine(tint, top, left, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, top, right, strokeWidth = stroke, cap = StrokeCap.Round)
    drawLine(tint, left, right, strokeWidth = stroke, cap = StrokeCap.Round)
    val dotRadius = w * 0.09f
    for (node in listOf(top, left, right)) {
        drawCircle(tint, radius = dotRadius, center = node)
    }
}

/** A down arrow dropping into a tray - the standard download glyph - for Downloads. */
fun DrawScope.drawDownloadIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.6.dp.toPx()
    val cx = w / 2f
    drawLine(tint, Offset(cx, h * 0.12f), Offset(cx, h * 0.60f), strokeWidth = stroke, cap = StrokeCap.Round)
    val arrow = Path().apply {
        moveTo(w * 0.28f, h * 0.44f)
        lineTo(cx, h * 0.68f)
        lineTo(w * 0.72f, h * 0.44f)
    }
    drawPath(arrow, tint, style = Stroke(width = stroke, cap = StrokeCap.Round, join = StrokeJoin.Round))
    drawLine(
        color = tint,
        start = Offset(w * 0.18f, h * 0.86f),
        end = Offset(w * 0.82f, h * 0.86f),
        strokeWidth = stroke,
        cap = StrokeCap.Round,
    )
}

/** Log lines of different lengths with a trailing cursor dot - for Console. */
fun DrawScope.drawConsoleIcon(tint: Color) {
    val w = size.width
    val h = size.height
    val stroke = 1.4.dp.toPx()
    val rows = listOf(0.78f, 0.58f, 0.68f)
    val ys = listOf(0.26f, 0.5f, 0.74f)
    rows.forEachIndexed { index, endFraction ->
        drawLine(
            color = tint,
            start = Offset(w * 0.22f, h * ys[index]),
            end = Offset(w * endFraction, h * ys[index]),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
    drawCircle(tint, radius = w * 0.05f, center = Offset(w * 0.86f, h * 0.74f))
}

/**
 * A "folder" entry that opens something to its right - one level deeper, or a pane beside it:
 * accent dot, title, what's inside, and a chevron pointing that way.
 */
@Composable
fun GlassFolderRow(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    accent: Color = GlassColors.Blue,
    selected: Boolean = false,
) {
    val palette = LocalGlassPalette.current
    val shape = RoundedCornerShape(16.dp)
    // Selected (its content is open in another pane): tinted with the accent, like a selected
    // row in a macOS sidebar.
    val fill by animateColorAsState(
        targetValue = if (selected) accent.copy(alpha = 0.22f) else palette.chipFill,
        label = "folderRowFill",
    )
    Row(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(fill, shape)
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
