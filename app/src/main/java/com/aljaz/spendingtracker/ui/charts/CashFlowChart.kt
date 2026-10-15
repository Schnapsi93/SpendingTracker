package com.aljaz.spendingtracker.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import com.aljaz.spendingtracker.core.MoneyFormatter
import com.aljaz.spendingtracker.domain.charts.AxisScale
import com.aljaz.spendingtracker.domain.charts.CashFlowPoint
import java.time.format.DateTimeFormatter

/** Line colors of the cash flow chart, also used by its legend and table. */
object CashFlowColors {
    val Income = Color(0xFF2E7D32) // green
    val Expense = Color(0xFFC62828) // red
}

/**
 * Line chart with one point per period: income in green, expenses in red.
 * The Y axis starts at 0 and ends at a round value (see [AxisScale]).
 *
 * @param points one point per period, oldest first
 */
@Composable
fun CashFlowChart(
    points: List<CashFlowPoint>,
    modifier: Modifier = Modifier,
    currencyCode: String = "EUR"
) {
    val textMeasurer = rememberTextMeasurer()
    val labelStyle = MaterialTheme.typography.labelSmall.copy(
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val monthFormat = DateTimeFormatter.ofPattern("LLL")

    val axisMaxMinor = AxisScale.niceMaxMinor(
        points.maxOfOrNull { maxOf(it.incomeMinor, it.expenseMinor) } ?: 0L
    )

    Canvas(modifier = modifier) {
        if (points.isEmpty()) return@Canvas

        // ----- Measure the labels to know how much room the axes need -----
        val steps = AxisScale.STEPS
        val yLabels = (0..steps).map { i ->
            textMeasurer.measure(
                MoneyFormatter.formatWhole(axisMaxMinor * i / steps, currencyCode),
                labelStyle
            )
        }
        val xLabels = points.map { textMeasurer.measure(it.month.format(monthFormat), labelStyle) }
        val gap = 6.dp.toPx()

        val chartLeft = yLabels.maxOf { it.size.width } + gap
        val chartRight = size.width - xLabels.last().size.width / 2f // room for the last label
        val chartTop = yLabels.last().size.height / 2f                // room for the top label
        val chartBottom = size.height - xLabels.maxOf { it.size.height } - gap
        val chartWidth = chartRight - chartLeft
        val chartHeight = chartBottom - chartTop

        /** X position of point [index]; a single point sits in the middle. */
        fun xOf(index: Int): Float =
            if (points.size == 1) chartLeft + chartWidth / 2f
            else chartLeft + chartWidth * index / (points.size - 1)

        /** Y position of an amount; 0 is at the bottom. */
        fun yOf(amountMinor: Long): Float =
            chartBottom - chartHeight * (amountMinor.toFloat() / axisMaxMinor)

        // ----- Horizontal grid lines with euro labels -----
        for (i in 0..steps) {
            val y = chartBottom - chartHeight * i / steps
            drawLine(gridColor, Offset(chartLeft, y), Offset(chartRight, y), strokeWidth = 1.dp.toPx())
            val label = yLabels[i]
            drawText(
                textLayoutResult = label,
                topLeft = Offset(chartLeft - gap - label.size.width, y - label.size.height / 2f)
            )
        }

        // ----- Month labels; every second one if they would overlap -----
        val spacing = if (points.size > 1) chartWidth / (points.size - 1) else chartWidth
        val every = if (spacing < xLabels.maxOf { it.size.width } + 4.dp.toPx()) 2 else 1
        xLabels.forEachIndexed { index, label ->
            // Count from the end, so the newest (selected) period is always labeled.
            if ((points.size - 1 - index) % every == 0) {
                drawText(
                    textLayoutResult = label,
                    topLeft = Offset(xOf(index) - label.size.width / 2f, chartBottom + gap)
                )
            }
        }

        // ----- The two lines, each with a dot per period -----
        fun drawSeries(values: List<Long>, color: Color) {
            val path = Path()
            values.forEachIndexed { index, value ->
                val x = xOf(index)
                val y = yOf(value)
                if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
            }
            drawPath(
                path = path,
                color = color,
                style = Stroke(
                    width = 3.dp.toPx(),
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
            values.forEachIndexed { index, value ->
                drawCircle(color = color, radius = 4.dp.toPx(), center = Offset(xOf(index), yOf(value)))
            }
        }

        drawSeries(points.map { it.expenseMinor }, CashFlowColors.Expense)
        drawSeries(points.map { it.incomeMinor }, CashFlowColors.Income) // drawn last, on top
    }
}