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
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.drawText
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.rememberTextMeasurer
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.feature.insight.YearBarUiModel
import kotlin.math.min

// VERIFY: Year 12-bar chart hand-drawn aesthetic and current month highlight
/**
 * Canvas-based hand-drawn aesthetic 12-bar chart for Year mode:
 * - Baseline rule
 * - 12 vertical bars with heights proportional to max monthly bucket (if max is 0, bar height is 0)
 * - Highlight current month with solid primary color
 * - Monospace month labels beneath each bar
 * - Accessibility semantics
 */
@Composable
fun YearBarChart(
    bars: List<YearBarUiModel>,
    modifier: Modifier = Modifier,
) {
    val textMeasurer = rememberTextMeasurer()
    val hairlineColor = AppTheme.colors.hairline
    val primaryColor = AppTheme.colors.textPrimary
    val secondaryColor = AppTheme.colors.textSecondary
    val surfaceBarColor = AppTheme.colors.textSecondary.copy(alpha = 0.4f)

    val currentMonthBar = bars.firstOrNull { it.isCurrentMonth }
    val accessibilityDescription = buildString {
        append("12-month expense chart. ")
        if (currentMonthBar != null) {
            append("Current month is month ${currentMonthBar.monthIndex}. ")
        }
        val activeMonths = bars.count { it.totalMinor > 0 }
        append("$activeMonths of 12 months have recorded expenses.")
    }

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(130.dp)
            .padding(horizontal = AppTheme.spacing.screenHorizontal)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
    ) {
        val totalWidth = size.width
        val totalHeight = size.height
        val labelAreaHeight = 22.dp.toPx()
        val baselineY = totalHeight - labelAreaHeight
        val maxBarHeight = (baselineY - 12.dp.toPx()).coerceAtLeast(0f)

        // Baseline rule
        drawLine(
            color = hairlineColor,
            start = Offset(0f, baselineY),
            end = Offset(totalWidth, baselineY),
            strokeWidth = 1.dp.toPx(),
        )

        val barCount = if (bars.isEmpty()) 12 else bars.size
        val slotWidth = totalWidth / barCount
        val barWidth = min(slotWidth * 0.48f, 16.dp.toPx())
        val cornerRadius = CornerRadius(2.dp.toPx(), 2.dp.toPx())

        val currentMonthStyle = TextStyle(
            color = primaryColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.SemiBold,
        )
        val normalMonthStyle = TextStyle(
            color = secondaryColor,
            fontSize = 10.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Normal,
        )

        bars.forEachIndexed { index, bar ->
            val slotCenterX = (index + 0.5f) * slotWidth
            val left = slotCenterX - (barWidth / 2f)

            // Tactile ghost track (10% opacity) anchoring all 12 months
            val ghostTrackColor = if (bar.isCurrentMonth) {
                primaryColor.copy(alpha = 0.18f)
            } else {
                hairlineColor.copy(alpha = 0.5f)
            }
            val ghostPath = Path().apply {
                addRoundRect(
                    RoundRect(
                        left = left,
                        top = baselineY - maxBarHeight,
                        right = left + barWidth,
                        bottom = baselineY,
                        topLeftCornerRadius = cornerRadius,
                        topRightCornerRadius = cornerRadius,
                        bottomLeftCornerRadius = CornerRadius.Zero,
                        bottomRightCornerRadius = CornerRadius.Zero,
                    ),
                )
            }
            drawPath(ghostPath, color = ghostTrackColor)

            // Bar height safely clamped (no divide by zero, handles NaN)
            val fraction = if (bar.fraction.isNaN()) 0f else bar.fraction.coerceIn(0f, 1f)
            val barHeight = maxBarHeight * fraction

            if (barHeight > 0f) {
                val top = baselineY - barHeight
                val barColor = if (bar.isCurrentMonth) {
                    primaryColor
                } else {
                    surfaceBarColor
                }

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
            }

            // Month label beneath baseline
            val labelText = bar.label
            val labelStyle = if (bar.isCurrentMonth) currentMonthStyle else normalMonthStyle
            val textLayoutResult = textMeasurer.measure(
                text = labelText,
                style = labelStyle,
            )
            val textX = slotCenterX - (textLayoutResult.size.width / 2f)
            val textY = baselineY + 4.dp.toPx()

            drawText(
                textLayoutResult = textLayoutResult,
                topLeft = Offset(textX, textY),
            )
        }
    }
}
