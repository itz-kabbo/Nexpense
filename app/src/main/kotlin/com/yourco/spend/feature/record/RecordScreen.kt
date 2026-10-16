package com.yourco.spend.feature.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.SheetValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.BigAmount
import com.yourco.spend.core.designsystem.components.EntryHandle
import com.yourco.spend.core.designsystem.components.EntryRow
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.data.db.entity.CategoryEntity
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(
    modifier: Modifier = Modifier,
    recordVm: RecordViewModel = hiltViewModel(),
    entryVm: EntryViewModel = hiltViewModel(),
    sheetState: androidx.compose.material3.SheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    ),
    onNavigateToSettings: (() -> Unit)? = null,
) {
    val state by recordVm.state.collectAsStateWithLifecycle()
    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }.collect { target ->
            if (target == SheetValue.Expanded) {
                focusRequester.requestFocus()
            } else {
                keyboard?.hide()
            }
        }
    }

    val isExpanded = sheetState.targetValue == SheetValue.Expanded || sheetState.currentValue == SheetValue.Expanded
    BackHandler(enabled = isExpanded) {
        scope.launch { sheetState.partialExpand() }
    }

    // VERIFY: Sheet peek height 56dp + navigation bar inset
    BottomSheetScaffold(
        modifier = modifier,
        scaffoldState = rememberBottomSheetScaffoldState(sheetState),
        sheetPeekHeight = 56.dp + bottomInset,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = AppTheme.colors.surface,
        sheetDragHandle = {
            EntryHandle(onClick = { scope.launch { sheetState.expand() } })
        },
        sheetContent = {
            EntrySheet(
                viewModel = entryVm,
                focusRequester = focusRequester,
                onSaved = { scope.launch { sheetState.partialExpand() } },
            )
        },
        containerColor = AppTheme.colors.background,
    ) { padding ->
        TodayContent(
            state = state,
            onSettingsClick = onNavigateToSettings ?: {},
            modifier = Modifier.padding(padding),
        )
    }
}

@Composable
fun TodayContent(
    state: RecordUiState,
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .background(AppTheme.colors.background),
    ) {
        // Top bar: date string and Settings icon button
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(
                    start = AppTheme.spacing.screenHorizontal,
                    end = AppTheme.spacing.s,
                    top = AppTheme.spacing.s,
                    bottom = AppTheme.spacing.s,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.todayDateLabel,
                style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
            )
            // VERIFY: Settings icon in top bar
            IconButton(
                onClick = onSettingsClick,
                modifier = Modifier.size(48.dp),
            ) {
                Icon(
                    imageVector = Icons.Rounded.Settings,
                    contentDescription = stringResource(R.string.cd_settings),
                    tint = AppTheme.colors.textPrimary,
                )
            }
        }

        // Center: BigAmount displaying today's total
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .padding(
                    horizontal = AppTheme.spacing.screenHorizontal,
                    vertical = AppTheme.spacing.xxl,
                ),
            contentAlignment = Alignment.Center,
        ) {
            BigAmount(
                amount = state.amountString,
                currencySymbol = state.currencySymbol,
                contentDescription = state.formattedTotal,
            )
        }

        // LazyColumn of today's expenses or Empty state
        if (state.expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(R.string.empty_today),
                    style = AppTheme.typography.bodySecondary.copy(
                        color = AppTheme.colors.textSecondary,
                    ),
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
            ) {
                items(
                    items = state.expenses,
                    key = { it.id },
                ) { expense ->
                    val categoryName = resolveCategoryDisplayName(expense.categoryId)
                    val note = expense.note.ifBlank { categoryName }
                    val formatted = formatMinor(expense.amountMinor, expense.currency)
                    EntryRow(
                        note = note,
                        category = categoryName,
                        amount = formatted,
                        showDivider = true,
                    )
                }
            }
        }
    }
}

@Composable
fun resolveCategoryDisplayName(categoryId: String): String {
    val resId = CategoryEntity.defaultNameRes(categoryId)
    return if (resId != null) stringResource(resId) else categoryId
}
