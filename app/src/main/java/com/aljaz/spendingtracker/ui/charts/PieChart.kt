package com.aljaz.spendingtracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import kotlin.math.cos
import kotlin.math.sin

/**
 * One slice of the pie.
 *
 * @property fraction share of the whole, from 0.0 to 1.0
 * @property label text drawn on the slice (e.g. "25 %"), or null for none
 */
data class PieSlice(
    val fraction: Float,
    val color: Color,
    val label: String?
)

/**
 * A filled pie chart, starting at the top and going clockwise.
 * Labels are drawn in black or white, whichever reads better on the slice.
 */
@Composable
fun PieChart(
    slices: List<PieSlice>,
    modifier: Modifier = Modifier
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelMedium
    // Thin lines between slices, in the screen's background color.
    val separatorColor = MaterialTheme.colorScheme.background

    Canvas(modifier = modifier.aspectRatio(1f)) {
        val diameter = size.minDimension
        val radius = diameter / 2f
        val center = Offset(size.width / 2f, size.height / 2f)
        val topLeft = Offset(center.x - radius, center.y - radius)

        // 1. The slices. Angles are in degrees; -90 is the top of the circle.
        var startAngle = -90f
        slices.forEach { slice ->
            val sweep = slice.fraction * 360f
            drawArc(
                color = slice.color,
                startAngle = startAngle,
                sweepAngle = sweep,
                useCenter = true,
                topLeft = topLeft,
                size = Size(diameter, diameter)
            )
            startAngle += sweep
        }

        // 2. Separator lines between slices (none needed for a single slice).
        if (slices.size > 1) {
            var angle = -90f
            slices.forEach { slice ->
                val radians = Math.toRadians(angle.toDouble())
                drawLine(
                    color = separatorColor,
                    start = center,
                    end = Offset(
                        center.x + radius * cos(radians).toFloat(),
                        center.y + radius * sin(radians).toFloat()
                    ),
                    strokeWidth = 2.dp.toPx()
                )
                angle += slice.fraction * 360f
            }
        }

        // 3. Labels, placed in the middle of each slice.
        var labelAngle = -90f
        slices.forEach { slice ->
            val sweep = slice.fraction * 360f
            slice.label?.let { label ->
                val middle = Math.toRadians((labelAngle + sweep / 2f).toDouble())
                // Single slice: put the label in the center instead of on a ring.
                val distance = if (slices.size == 1) 0f else radius * 0.62f
                val position = Offset(
                    center.x + distance * cos(middle).toFloat(),
                    center.y + distance * sin(middle).toFloat()
                )
                val textColor = if (slice.color.luminance() > 0.5f) Color.Black else Color.White
                val layout = textMeasurer.measure(label, labelStyle.copy(color = textColor))
                drawText(
                    textLayoutResult = layout,
                    topLeft = position - Offset(layout.size.width / 2f, layout.size.height / 2f)
                )
            }
            labelAngle += sweep
        }
    }
}