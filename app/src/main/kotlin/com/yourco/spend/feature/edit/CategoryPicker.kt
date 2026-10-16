package com.yourco.spend.feature.edit

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.data.db.entity.CategoryEntity

@Composable
fun CategoryPickerDialog(
    categories: List<CategoryEntity>,
    selectedCategoryId: String?,
    onCategoryPicked: (String) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = stringResource(R.string.picker_select_category),
                style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
            ) {
                categories.forEach { category ->
                    val displayName = category.nameOverride
                        ?: category.defaultNameRes()?.let { stringResource(it) }
                        ?: category.id
                    val isSelected = category.id == selectedCategoryId

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .defaultMinSize(minHeight = 48.dp)
                            .selectable(
                                selected = isSelected,
                                role = Role.RadioButton,
                                onClick = { onCategoryPicked(category.id) },
                            )
                            .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = displayName,
                            style = AppTheme.typography.body.copy(
                                color = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                                fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Normal,
                            ),
                        )
                        if (isSelected) {
                            Text(
                                text = "✓",
                                style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                            )
                        }
                    }
                }
            }
        },
        confirmButton = {},
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(
                    text = stringResource(R.string.cd_close),
                    style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
                )
            }
        },
        containerColor = AppTheme.colors.surface,
        shape = AppTheme.shapes.card,
    )
}
