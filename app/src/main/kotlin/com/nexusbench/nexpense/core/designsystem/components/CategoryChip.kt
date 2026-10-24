package com.nexusbench.nexpense.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.ArrowDropDown
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.unit.dp
import com.nexusbench.nexpense.core.designsystem.AppTheme

/**
 * Fully rounded pill chip displaying category name, clickable.
 * Monochromatic surfaceVariant with 0.5dp hairline border.
 */
@Composable
fun CategoryChip(
    categoryName: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    selected: Boolean = false,
    hasDropdown: Boolean = false,
) {
    val backgroundColor = if (selected) {
        AppTheme.colors.textPrimary
    } else {
        AppTheme.colors.surfaceVariant
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
            .defaultMinSize(minHeight = 28.dp)
            .clip(AppTheme.shapes.chip)
            .background(backgroundColor)
            .border(width = 0.5.dp, color = borderColor, shape = AppTheme.shapes.chip)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.xs),
        contentAlignment = Alignment.Center,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                text = categoryName,
                style = AppTheme.typography.label.copy(color = textColor),
                maxLines = 1,
            )
            if (hasDropdown) {
                Spacer(modifier = Modifier.width(2.dp))
                Icon(
                    imageVector = Icons.Rounded.ArrowDropDown,
                    contentDescription = null,
                    tint = textColor,
                    modifier = Modifier.size(16.dp),
                )
            }
        }
    }
}
