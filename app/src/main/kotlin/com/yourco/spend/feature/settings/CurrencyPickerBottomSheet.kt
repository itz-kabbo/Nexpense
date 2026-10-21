package com.yourco.spend.feature.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
import androidx.compose.ui.unit.sp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.money.CurrencyCatalog
import com.yourco.spend.core.money.CurrencyItem
import com.yourco.spend.core.money.CurrencyRegion

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CurrencyPickerBottomSheet(
    selectedCurrencyCode: String,
    onCurrencySelected: (String) -> Unit,
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var searchQuery by remember { mutableStateOf("") }
    var selectedRegionFilter by remember { mutableStateOf<CurrencyRegion?>(null) }

    val allCurrencies = remember { CurrencyCatalog.getAllCurrencies() }
    val filteredCurrencies = remember(searchQuery, selectedRegionFilter) {
        val baseList = if (selectedRegionFilter != null) {
            allCurrencies.filter { it.region == selectedRegionFilter }
        } else {
            allCurrencies
        }
        if (searchQuery.isBlank()) {
            baseList
        } else {
            val query = searchQuery.trim().lowercase()
            baseList.filter { it.searchKeywords.contains(query) }
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
                    text = stringResource(R.string.settings_select_currency),
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
                        text = stringResource(R.string.currency_search_placeholder),
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

            Spacer(modifier = Modifier.height(AppTheme.spacing.s))

            // Region filter chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                item {
                    val isAllSelected = selectedRegionFilter == null
                    RegionFilterChip(
                        label = "All",
                        isSelected = isAllSelected,
                        onClick = { selectedRegionFilter = null },
                    )
                }
                items(CurrencyRegion.entries) { region ->
                    val isSelected = selectedRegionFilter == region
                    RegionFilterChip(
                        label = stringResource(region.titleRes),
                        isSelected = isSelected,
                        onClick = {
                            selectedRegionFilter = if (isSelected) null else region
                        },
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.m))

            // Currency list
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.xs),
            ) {
                items(filteredCurrencies, key = { it.code }) { item ->
                    val isSelected = item.code.equals(selectedCurrencyCode, ignoreCase = true)
                    CurrencyListItem(
                        item = item,
                        isSelected = isSelected,
                        onClick = {
                            onCurrencySelected(item.code)
                            onDismissRequest()
                        },
                    )
                }
            }
        }
    }
}

@Composable
private fun RegionFilterChip(
    label: String,
    isSelected: Boolean,
    onClick: () -> Unit,
) {
    val bgColor = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.surfaceVariant
    val textColor = if (isSelected) AppTheme.colors.background else AppTheme.colors.textSecondary
    val borderColor = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.hairline

    Box(
        modifier = Modifier
            .defaultMinSize(minHeight = 32.dp)
            .clip(AppTheme.shapes.pill)
            .background(bgColor)
            .border(width = 0.5.dp, color = borderColor, shape = AppTheme.shapes.pill)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = AppTheme.spacing.m, vertical = 6.dp),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = AppTheme.typography.label.copy(
                color = textColor,
                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                fontSize = 12.sp,
            ),
        )
    }
}

@Composable
private fun CurrencyListItem(
    item: CurrencyItem,
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.code,
                    style = AppTheme.typography.title.copy(
                        color = AppTheme.colors.textPrimary,
                        fontWeight = FontWeight.SemiBold,
                    ),
                )
                Spacer(modifier = Modifier.width(AppTheme.spacing.s))
                Text(
                    text = item.name,
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                )
            }
        }

        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
        ) {
            Text(
                text = item.symbol,
                style = AppTheme.typography.title.copy(
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )

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
}
