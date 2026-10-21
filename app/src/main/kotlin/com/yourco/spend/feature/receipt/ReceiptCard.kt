package com.yourco.spend.feature.receipt

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.DashedRule
import com.yourco.spend.core.designsystem.components.LeaderRow
import com.yourco.spend.core.designsystem.components.rememberPerforatedShape
import java.util.Locale

/**
 * Daily Receipt card: a Column clipped to PerforatedShape, filled with AppTheme.colors.paper,
 * styled with monospace typography throughout. Persistently rendered in both empty and populated states.
 */
@Composable
fun ReceiptCard(
    state: ReceiptUiState,
    modifier: Modifier = Modifier,
) {
    val perforatedShape = rememberPerforatedShape(toothWidth = 6.dp)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .shadow(
                elevation = if (AppTheme.colors.isDark) 0.dp else 4.dp,
                shape = perforatedShape,
                clip = false,
            )
            .clip(perforatedShape)
            .background(AppTheme.colors.paper)
            .border(
                width = 0.5.dp,
                color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else Color(0xFFDDDDCF),
                shape = perforatedShape,
            )
            .padding(
                horizontal = AppTheme.spacing.l,
                vertical = AppTheme.spacing.xl,
            ),
        contentAlignment = Alignment.TopCenter,
    ) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Header: App name and formatted date
            Text(
                text = stringResource(R.string.app_name).uppercase(Locale.getDefault()),
                style = AppTheme.typography.receiptTitle.copy(color = AppTheme.colors.textPrimary),
            )
            Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
            Text(
                text = state.formattedDate,
                style = AppTheme.typography.receipt.copy(color = AppTheme.colors.textSecondary),
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.l))
            DashedRule(modifier = Modifier.padding(vertical = AppTheme.spacing.s))
            Spacer(modifier = Modifier.height(AppTheme.spacing.s))

            if (state.items.isEmpty()) {
                // Empty state within persistent slip: Minimal line-art illustration + stamped text
                Spacer(modifier = Modifier.height(AppTheme.spacing.m))
                EmptyReceiptIllustration()
                Spacer(modifier = Modifier.height(AppTheme.spacing.l))
                Text(
                    text = stringResource(R.string.receipt_empty_stamped),
                    style = AppTheme.typography.receipt.copy(
                        color = AppTheme.colors.textTertiary,
                        letterSpacing = 1.sp,
                    ),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = AppTheme.spacing.m),
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.l))
            } else {
                // Body: one line per entry with LeaderRow
                state.items.forEach { item ->
                    LeaderRow(
                        label = item.noteRes?.let { stringResource(it) } ?: item.note,
                        amount = item.formattedAmount,
                        style = AppTheme.typography.receipt,
                        modifier = Modifier.padding(vertical = AppTheme.spacing.xxs),
                    )
                }

                Spacer(modifier = Modifier.height(AppTheme.spacing.m))
                DashedRule(modifier = Modifier.padding(vertical = AppTheme.spacing.s))
                Spacer(modifier = Modifier.height(AppTheme.spacing.s))

                val totals = state.currencyTotals.ifEmpty {
                    listOf(ReceiptCurrencyTotalUiModel(state.currencyCode, state.formattedTotal))
                }
                totals.forEach { total ->
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = if (totals.size == 1) {
                                stringResource(R.string.receipt_total)
                            } else {
                                stringResource(R.string.receipt_total_currency, total.currencyCode)
                            },
                            style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
                        )
                        Text(
                            text = total.formattedTotal,
                            style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.m))
            DashedRule(modifier = Modifier.padding(vertical = AppTheme.spacing.s))
            Spacer(modifier = Modifier.height(AppTheme.spacing.l))

            // Footer line
            Text(
                text = stringResource(R.string.receipt_footer),
                style = AppTheme.typography.receipt.copy(color = AppTheme.colors.textSecondary),
            )
        }
    }
}

@Composable
fun EmptyReceiptIllustration(
    modifier: Modifier = Modifier,
    color: Color = AppTheme.colors.textTertiary,
) {
    Canvas(
        modifier = modifier
            .size(width = 44.dp, height = 52.dp)
            .padding(vertical = AppTheme.spacing.xs),
    ) {
        val strokeWidth = 1.dp.toPx()
        val cornerRadius = 4.dp.toPx()

        drawRoundRect(
            color = color,
            topLeft = Offset(0f, 0f),
            size = Size(size.width, size.height),
            cornerRadius = CornerRadius(cornerRadius, cornerRadius),
            style = Stroke(width = strokeWidth),
        )

        val lineMargin = 8.dp.toPx()
        val lineY1 = 14.dp.toPx()
        val lineY2 = 24.dp.toPx()
        val lineY3 = 34.dp.toPx()

        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(lineMargin, lineY1),
            end = Offset(size.width - lineMargin, lineY1),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = color.copy(alpha = 0.6f),
            start = Offset(lineMargin, lineY2),
            end = Offset(size.width - lineMargin, lineY2),
            strokeWidth = strokeWidth,
        )
        drawLine(
            color = color.copy(alpha = 0.4f),
            start = Offset(lineMargin, lineY3),
            end = Offset(size.width - lineMargin * 2, lineY3),
            strokeWidth = strokeWidth,
        )
    }
}
