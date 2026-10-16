package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.yourco.spend.core.designsystem.AppTheme

/**
 * Fully rounded pill chip displaying category name, clickable.
 */
@Composable
fun CategoryChip(
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
) {
    val backgroundColor = if (selected) {
        AppTheme.colors.textPrimary
    } else {
        AppTheme.colors.surface
    }
    val textColor = if (selected) {
        AppTheme.colors.background
    } else {
        AppTheme.colors.textPrimary
    }
    val borderColor = if (selected) {
        AppTheme.colors.textPrimary
    } else {
        AppTheme.colors.hairline
    }

    Box(
        modifier = modifier
            .defaultMinSize(minHeight = 36.dp, minWidth = 48.dp)
            .clip(AppTheme.shapes.chip)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = borderColor, shape = AppTheme.shapes.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics {
                role = Role.Tab
                this.selected = selected
            }
            .padding(horizontal = AppTheme.spacing.l, vertical = AppTheme.spacing.s),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = categoryName,
            style = AppTheme.typography.label.copy(color = textColor),
            maxLines = 1,
        )
    }
}
