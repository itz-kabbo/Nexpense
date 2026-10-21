package com.yourco.spend.feature.insight.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.RoundRect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.feature.insight.MonthPointUiModel
import kotlin.math.min

// VERIFY: Month sparkline daily mini-bars distribution and day indicator aesthetic
/**
 * Canvas-based daily sparkline or mini-bars for the selected month:
 * - Plots daily bucket totals across 28..31 days
 * - Safe normalization (handles all-zero and single-value without divide by zero)
 * - Subtle monochrome aesthetic matching the design system
 * - Monospace landmark labels (day 1, 15, end of month)
 */
@Composable
fun MonthSparkline(
    points: List<MonthPointUiModel>,
    modifier: Modifier = Modifier,
) {
    if (points.isEmpty()) return

    val textMeasurer = rememberTextMeasurer()
    val hairlineColor = AppTheme.colors.hairline
    val primaryColor = AppTheme.colors.textPrimary
    val secondaryColor = AppTheme.colors.textSecondary
    val barColorSecondary = AppTheme.colors.textSecondary.copy(alpha = 0.45f)

    val currentDayPoint = points.firstOrNull { it.isCurrentDay }
    val accessibilityDescription = buildString {
        append("Daily expense chart for ${points.size} days. ")
        if (currentDayPoint != null) {
            append("Today is day ${currentDayPoint.dayOfMonth}. ")
        }
        val activeDays = points.count { it.totalMinor > 0 }
        append("$activeDays days with expenses.")
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(80.dp)
            .padding(horizontal = AppTheme.spacing.screenHorizontal)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val labelAreaHeight = 18.dp.toPx()
        val baselineY = totalHeight - labelAreaHeight
        val maxBarHeight = (baselineY - 8.dp.toPx()).coerceAtLeast(0f)

        // Baseline rule
        drawLine(
            color = hairlineColor,
            start = Offset(0f, baselineY),
            end = Offset(totalWidth, baselineY),
            strokeWidth = 1.dp.toPx(),
        )

        val slotWidth = totalWidth / points.size
        val barWidth = min(slotWidth * 0.55f, 4.dp.toPx())
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        val currentDayStyle = TextStyle(
            color = primaryColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )
        val normalDayStyle = TextStyle(
            color = secondaryColor,
            fontSize = 9.sp,
            fontFamily = FontFamily.Monospace,
        )

        points.forEachIndexed { index, point ->
            val slotCenterX = (index + 0.5f) * slotWidth
            val left = slotCenterX - (barWidth / 2f)

            // Tactile ghost track (10% opacity) anchoring timeline
            val ghostTrackColor = if (point.isCurrentDay) {
                primaryColor.copy(alpha = 0.25f)
            } else {
                hairlineColor.copy(alpha = 0.5f)
            }
            drawLine(
                color = ghostTrackColor,
                start = Offset(slotCenterX, baselineY),
                end = Offset(slotCenterX, baselineY - maxBarHeight),
                strokeWidth = 1.dp.toPx(),
            )

            val fraction = if (point.fraction.isNaN()) 0f else point.fraction.coerceIn(0f, 1f)
            val barHeight = maxBarHeight * fraction

            if (barHeight > 0f) {
                val top = baselineY - barHeight
                val barColor = if (point.isCurrentDay) primaryColor else barColorSecondary

                val path = Path().apply {
                    addRoundRect(
                        RoundRect(
                            left = left,
                            top = top,
                            right = left + barWidth,
                            bottom = baselineY,
                            topLeftCornerRadius = cornerRadius,
                            topRightCornerRadius = cornerRadius,
                            bottomLeftCornerRadius = CornerRadius.Zero,
                            bottomRightCornerRadius = CornerRadius.Zero,
                        ),
                    )
                }
                drawPath(path, color = barColor)
            } else if (point.isCurrentDay) {
                // Subtle indicator dot for current day if 0 spending
                drawCircle(
                    color = primaryColor,
                    radius = 2.dp.toPx(),
                    center = Offset(slotCenterX, baselineY - 4.dp.toPx()),
                )
            }

            // Landmark day labels: 1, 15, and last day
            val isLandmark = point.dayOfMonth == 1 ||
                point.dayOfMonth == 15 ||
                point.dayOfMonth == points.size

            if (isLandmark) {
                val labelText = point.dayOfMonth.toString()
                val labelStyle = if (point.isCurrentDay) currentDayStyle else normalDayStyle
                val textLayoutResult = textMeasurer.measure(
                    text = labelText,
                    style = labelStyle,
                )
                val textX = (slotCenterX - (textLayoutResult.size.width / 2f))
                    .coerceIn(0f, totalWidth - textLayoutResult.size.width)
                val textY = baselineY + 3.dp.toPx()

                drawText(
                    textLayoutResult = textLayoutResult,
                    topLeft = Offset(textX, textY),
                )
            }
        }
    }
}
