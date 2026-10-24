package com.nexusbench.nexpense.core.designsystem.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.nexusbench.nexpense.R
import com.nexusbench.nexpense.core.designsystem.AppTheme

/**
 * Docked quick-entry bar:
 * 52dp height, 20dp corner radius (AppTheme.shapes.dock),
 * floating surface container with subtle pencil icon, hint text,
 * and upward expansion indicator.
 */
@Composable
fun EntryHandle(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    contentDescription: String? = null,
    hintText: String? = null,
) {
    val defaultDescription = stringResource(R.string.cd_add_expense)
    val description = contentDescription ?: defaultDescription
    val hint = hintText ?: stringResource(R.string.dock_entry_hint)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .shadow(
                elevation = if (AppTheme.colors.isDark) 0.dp else 2.dp,
                shape = AppTheme.shapes.dock,
                clip = false,
            )
            .clip(AppTheme.shapes.dock)
            .background(AppTheme.colors.surface)
            .border(
                width = 0.5.dp,
                color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                shape = AppTheme.shapes.dock,
            )
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = ripple(bounded = true),
                role = Role.Button,
                onClick = onClick,
                onClickLabel = description,
            )
            .semantics {
                this.contentDescription = description
            }
            .padding(horizontal = AppTheme.spacing.l, vertical = AppTheme.spacing.m),
        contentAlignment = Alignment.CenterStart,
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Edit,
                    contentDescription = null,
                    tint = AppTheme.colors.textSecondary,
                    modifier = Modifier.size(18.dp),
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.m))
                Text(
                    text = hint,
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Spacer(modifier = Modifier.width(AppTheme.spacing.s))
            Icon(
                imageVector = Icons.Rounded.KeyboardArrowUp,
                contentDescription = null,
                tint = AppTheme.colors.textTertiary,
                modifier = Modifier.size(20.dp),
            )
        }
    }
}
