package com.yourco.spend.feature.receipt

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.DashedRule
import com.yourco.spend.core.designsystem.components.LeaderRow
import com.yourco.spend.core.designsystem.components.rememberPerforatedShape
import java.util.Locale

/**
 * Daily Receipt card: a Column clipped to PerforatedShape, filled with AppTheme.colors.paper,
 * styled with monospace typography throughout.
 */
@Composable
fun ReceiptCard(
    state: ReceiptUiState,
    modifier: Modifier = Modifier,
) {
    val perforatedShape = rememberPerforatedShape(toothWidth = 6.dp)

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(perforatedShape)
            .background(AppTheme.colors.paper)
            .padding(
                horizontal = AppTheme.spacing.l,
                vertical = AppTheme.spacing.xl,
            ),
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

        // Body: one line per entry with LeaderRow (leader dots between note and formatted amount)
        state.items.forEach { item ->
            LeaderRow(
                label = item.note,
                amount = item.formattedAmount,
                style = AppTheme.typography.receipt,
                modifier = Modifier.padding(vertical = AppTheme.spacing.xxs),
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.m))

        // Dashed divider
        DashedRule(modifier = Modifier.padding(vertical = AppTheme.spacing.s))

        Spacer(modifier = Modifier.height(AppTheme.spacing.s))

        // Total row in larger receiptTotal monospace type
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.receipt_total),
                style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
            )
            Text(
                text = state.formattedTotal,
                style = AppTheme.typography.receiptTotal.copy(color = AppTheme.colors.textPrimary),
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.l))

        // Footer line
        // VERIFY: Receipt footer text proposed default
        Text(
            text = stringResource(R.string.receipt_footer),
            style = AppTheme.typography.receipt.copy(color = AppTheme.colors.textSecondary),
        )
    }
}
