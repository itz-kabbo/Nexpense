package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourco.spend.core.designsystem.AppTheme

/**
 * Monospace leader row with dashed leader line between description and amount.
 * From GUIDE.md section 8b M6 skeleton.
 */
@Composable
fun LeaderRow(
    label: String,
    amount: String,
    modifier: Modifier = Modifier,
    style: TextStyle = AppTheme.typography.receipt,
) {
    val dotColor = AppTheme.colors.textSecondary
    val accessibilityText = "$label, $amount"

    Row(
        modifier = modifier
            .fillMaxWidth()
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityText
            },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = label,
            style = style.copy(color = AppTheme.colors.textPrimary),
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .height(14.dp)
                .drawBehind {
                    val y = size.height - 3.dp.toPx()
                    drawLine(
                        color = dotColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx())),
                    )
                },
        )
        Text(
            text = amount,
            style = style.copy(color = AppTheme.colors.textPrimary),
            maxLines = 1,
        )
    }
}
