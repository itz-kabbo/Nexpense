package com.yourco.spend.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Close
import androidx.compose.material.icons.rounded.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.language.LanguageCatalog
import com.yourco.spend.core.language.LanguageItem

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LanguagePickerBottomSheet(
    selectedLanguageTag: String,
    onLanguageSelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }

    val allLanguages = remember { LanguageCatalog.getAllLanguages() }
    val filteredLanguages = remember(searchQuery) {
        if (searchQuery.isBlank()) {
            allLanguages
        } else {
            val query = searchQuery.trim().lowercase()
            allLanguages.filter { it.searchKeywords.contains(query) }
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        sheetState = sheetState,
        shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        containerColor = AppTheme.colors.surface,
        dragHandle = {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = AppTheme.spacing.m, bottom = AppTheme.spacing.s),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(width = 36.dp, height = 4.dp)
                        .clip(AppTheme.shapes.pill)
                        .background(AppTheme.colors.hairline),
                )
            }
        },
        modifier = modifier,
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight(0.85f)
                .padding(horizontal = AppTheme.spacing.screenHorizontal)
                .navigationBarsPadding(),
        ) {
            // Title
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(R.string.settings_select_language),
                    style = AppTheme.typography.headline.copy(
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.Medium,
                    ),
                )
                IconButton(onClick = onDismissRequest) {
                    Icon(
                        imageVector = Icons.Rounded.Close,
                        contentDescription = stringResource(R.string.cd_close),
                        tint = AppTheme.colors.textSecondary,
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.m))

            // Search bar
            OutlinedTextField(
                value = searchQuery,
                onValueChange = { searchQuery = it },
                modifier = Modifier.fillMaxWidth(),
                placeholder = {
                    Text(
                        text = stringResource(R.string.language_search_placeholder),
                        style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textTertiary),
                    )
                },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Rounded.Search,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary,
                    )
                },
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { searchQuery = "" }) {
                            Icon(
                                imageVector = Icons.Rounded.Close,
                                contentDescription = null,
                                tint = AppTheme.colors.textSecondary,
                            )
                        }
                    }
                },
                singleLine = true,
                shape = AppTheme.shapes.pill,
                colors = OutlinedTextFieldDefaults.colors(
                    focusedContainerColor = AppTheme.colors.surfaceVariant,
                    unfocusedContainerColor = AppTheme.colors.surfaceVariant,
                    focusedBorderColor = AppTheme.colors.textPrimary,
                    unfocusedBorderColor = AppTheme.colors.hairline,
                    focusedTextColor = AppTheme.colors.textPrimary,
                    unfocusedTextColor = AppTheme.colors.textPrimary,
                ),
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.m))

            // Language list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                items(filteredLanguages, key = { it.tag }) { item ->
                    val isSelected = if (item.tag.isEmpty()) {
                        selectedLanguageTag.isEmpty()
                    } else {
                        selectedLanguageTag.equals(item.tag, ignoreCase = true)
                    }

                    LanguageListItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            onLanguageSelected(item.tag)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun LanguageListItem(
    item: LanguageItem,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val rowBackground = if (isSelected) {
        AppTheme.colors.surfaceVariant
    } else {
        Color.Transparent
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 52.dp)
            .clip(RoundedCornerShape(12.dp))
            .background(rowBackground)
            .clickable(role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = item.nativeName,
                style = AppTheme.typography.title.copy(
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )
            if (item.nativeName != item.englishName && item.tag.isNotEmpty()) {
                Text(
                    text = item.englishName,
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                )
            }
        }

        if (isSelected) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(CircleShape)
                    .background(AppTheme.colors.textPrimary),
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Rounded.Check,
                    contentDescription = null,
                    tint = AppTheme.colors.background,
                    modifier = Modifier.size(16.dp),
                )
            }
        } else {
            Spacer(modifier = Modifier.size(24.dp))
        }
    }
}
