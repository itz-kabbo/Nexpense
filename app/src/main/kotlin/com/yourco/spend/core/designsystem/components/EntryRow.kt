package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.yourco.spend.core.designsystem.AppTheme

/**
 * EntryRow displaying an expense entry:
 * - Description on the left
 * - Category as quiet secondary text beneath
 * - Amount on the right in tabular figures
 * - 0.5dp hairline divider at the bottom
 */
@Composable
fun EntryRow(
    note: String,
    category: String,
    amount: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    showDivider: Boolean = true,
) {
    val accessibilityDescription = "$note, $category, $amount"
    val clickableModifier = if (onClick != null) {
        Modifier.clickable(role = Role.Button, onClick = onClick)
    } else {
        Modifier
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(clickableModifier)
            .semantics(mergeDescendants = true) {
                contentDescription = accessibilityDescription
            },
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.spacing.screenHorizontal,
                    vertical = AppTheme.spacing.m,
                ),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(
                modifier = Modifier.weight(1f),
            ) {
                Text(
                    text = note,
                    style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.xxs))
                Text(
                    text = category,
                    style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spacing.m))
            Text(
                text = amount,
                style = AppTheme.typography.amount.copy(color = AppTheme.colors.textPrimary),
                maxLines = 1,
            )
        }
        if (showDivider) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = AppTheme.spacing.screenHorizontal)
                    .height(0.5.dp)
                    .background(AppTheme.colors.hairline),
            )
        }
    }
}
