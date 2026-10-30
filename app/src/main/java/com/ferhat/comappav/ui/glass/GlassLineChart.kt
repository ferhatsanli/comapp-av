package com.ferhat.comappav.ui.glass

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** One value at one moment, for [GlassLineChart]. */
data class ChartPoint(val timeMs: Long, val value: Float)

/**
 * A small line chart in the glass style: the line in [accent] over a soft fill, three faint
 * guide lines, and a dot on the newest value. Gaps longer than [gapMs] (the server was asleep
 * or off) break the line instead of drawing a straight bridge across them.
 *
 * The value axis runs from 0 to [fixedMax] (CPU usage: 100) or, without one, to a little above
 * the highest value shown. The header reads "<title>    now X · max Y"; under the chart are the
 * start and end of the time range.
 */
@Composable
fun GlassLineChart(
    title: String,
    points: List<ChartPoint>,
    startMs: Long,
    endMs: Long,
    accent: Color,
    format: (Float) -> String,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier,
    fixedMax: Float? = null,
    gapMs: Long = 5 * 60_000L,
) {
    val palette = LocalGlassPalette.current
    val shown = points.filter { it.timeMs in startMs..endMs }.sortedBy { it.timeMs }
    val highest = shown.maxOfOrNull { it.value }
    val top = fixedMax ?: ((highest ?: 0f) * 1.15f).coerceAtLeast(1f)
    Column(modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth()) {
            Text(title, color = palette.ink, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, modifier = Modifier.weight(1f))
            Text(
                if (shown.isEmpty()) "no data yet" else "now ${format(shown.last().value)} · max ${format(highest ?: 0f)}",
                color = palette.inkMuted,
                fontSize = 12.sp,
            )
        }
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(110.dp),
        ) {
            val guide = palette.inkFaint.copy(alpha = 0.35f)
            val dash = PathEffect.dashPathEffect(floatArrayOf(4.dp.toPx(), 4.dp.toPx()))
            for (fraction in listOf(0f, 0.5f, 1f)) {
                val y = size.height * fraction
                drawLine(guide, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f, pathEffect = dash)
            }
            if (shown.isEmpty() || endMs <= startMs) return@Canvas
            fun x(t: Long) = size.width * (t - startMs).toFloat() / (endMs - startMs).toFloat()
            fun y(v: Float) = size.height * (1f - (v / top).coerceIn(0f, 1f))

            // Split into runs wherever the samples stop for longer than gapMs.
            val runs = mutableListOf<MutableList<ChartPoint>>()
            for (point in shown) {
                val run = runs.lastOrNull()
                if (run == null || point.timeMs - run.last().timeMs > gapMs) runs.add(mutableListOf(point)) else run.add(point)
            }
            val fill = Brush.verticalGradient(listOf(accent.copy(alpha = 0.32f), accent.copy(alpha = 0f)))
            val line = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
            for (run in runs) {
                if (run.size == 1) {
                    drawCircle(accent, radius = 2.dp.toPx(), center = Offset(x(run[0].timeMs), y(run[0].value)))
                    continue
                }
                val path = Path().apply {
                    moveTo(x(run[0].timeMs), y(run[0].value))
                    for (point in run.drop(1)) lineTo(x(point.timeMs), y(point.value))
                }
                val area = Path().apply {
                    addPath(path)
                    lineTo(x(run.last().timeMs), size.height)
                    lineTo(x(run[0].timeMs), size.height)
                    close()
                }
                drawPath(area, fill)
                drawPath(path, accent, style = line)
            }
            val last = shown.last()
            drawCircle(Color.White, radius = 3.5.dp.toPx(), center = Offset(x(last.timeMs), y(last.value)))
            drawCircle(accent, radius = 2.5.dp.toPx(), center = Offset(x(last.timeMs), y(last.value)))
        }
        Row(Modifier.fillMaxWidth()) {
            Text(startLabel, color = palette.inkFaint, fontSize = 11.sp, modifier = Modifier.weight(1f))
            Text(endLabel, color = palette.inkFaint, fontSize = 11.sp, textAlign = TextAlign.End)
        }
    }
}
