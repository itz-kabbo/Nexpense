package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourco.spend.core.designsystem.AppTheme

/**
 * Clamps category bar fraction between 0f and 1f, safely handling NaN as 0f.
 */
fun clampCategoryFraction(fraction: Float): Float =
    if (fraction.isNaN()) 0f else fraction.coerceIn(0f, 1f)

/**
 * Category row with name, amount, percentage, and thin proportional bar.
 * Based on GUIDE.md sections 3.2 and 8b M7.
 */
@Composable
fun CategoryBar(
    name: String,
    amount: String,
    percentage: Int,
    fraction: Float,
    modifier: Modifier = Modifier,
) {
    val clampedFraction = clampCategoryFraction(fraction)
    val percentageString = "$percentage%"
    val accessibilityText = "$name, $amount, $percentageString"

    Column(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityText
            }
            .padding(
                horizontal = 16.dp,
                vertical = 12.dp,
            ),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = name,
                style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.weight(1f),
            )
            Text(
                text = percentageString,
                style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                modifier = Modifier.padding(end = AppTheme.spacing.s),
            )
            Text(
                text = amount,
                style = AppTheme.typography.amount.copy(color = AppTheme.colors.textPrimary),
                maxLines = 1,
            )
        }
        Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
        // Pill-shaped progress capsule (6dp height)
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(6.dp)
                .clip(AppTheme.shapes.pill)
                .background(AppTheme.colors.surfaceVariant),
        ) {
            if (clampedFraction > 0f) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(clampedFraction)
                        .height(6.dp)
                        .clip(AppTheme.shapes.pill)
                        .background(AppTheme.colors.textPrimary),
                )
            }
        }
    }
}
