package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.yourco.spend.core.designsystem.AppTheme

/**
 * Large amount display (~64sp light weight) with smaller baseline-aligned currency symbol
 * and tabular figures.
 */
@Composable
fun BigAmount(
    amount: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
) {
    val rowModifier = if (contentDescription != null) {
        modifier.semantics { this.contentDescription = contentDescription }
    } else {
        modifier
    }

    Row(
        modifier = rowModifier,
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = currencySymbol,
            style = AppTheme.typography.currencySymbol.copy(color = AppTheme.colors.textSecondary),
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(AppTheme.spacing.xs))
        Text(
            text = amount,
            style = AppTheme.typography.bigAmount.copy(color = AppTheme.colors.textPrimary),
            modifier = Modifier.alignByBaseline(),
        )
    }
}
