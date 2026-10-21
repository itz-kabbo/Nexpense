package com.yourco.spend.feature.record

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Delete
import androidx.compose.material.icons.rounded.Settings
import androidx.compose.material3.BottomSheetScaffold
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.SheetValue
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult
import androidx.compose.material3.Surface
import androidx.compose.material3.SwipeToDismissBox
import androidx.compose.material3.SwipeToDismissBoxValue
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.material3.rememberSwipeToDismissBoxState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.BigAmount
import com.yourco.spend.core.designsystem.components.EntryHandle
import com.yourco.spend.core.designsystem.components.EntryRow
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.feature.edit.EditSheet
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
    forceCenteredEntryDialog: Boolean = false,
) {
    val state by recordVm.state.collectAsStateWithLifecycle()
    val editingExpense by recordVm.editingExpense.collectAsStateWithLifecycle()
    val categories by recordVm.categories.collectAsStateWithLifecycle()
    val allCategories by recordVm.allCategories.collectAsStateWithLifecycle()

    val focusRequester = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
    val snackbarHostState = remember { SnackbarHostState() }
    val deletedMessage = stringResource(R.string.deleted)
    val undoLabel = stringResource(R.string.undo)
    val saveFailedMessage = stringResource(R.string.save_failed)

    LaunchedEffect(recordVm, snackbarHostState) {
        recordVm.updateResultEvents.collect { saved ->
            if (!saved) snackbarHostState.showSnackbar(saveFailedMessage)
        }
    }

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

    BoxWithConstraints(modifier = modifier.fillMaxSize()) {
    val useCenteredEntryDialog = forceCenteredEntryDialog || maxWidth >= 600.dp

    // Docked quick-entry bar (52dp) floats above pager indicator (40dp) and navigation bar insets
    BottomSheetScaffold(
        modifier = Modifier.fillMaxSize(),
        scaffoldState = rememberBottomSheetScaffoldState(
            bottomSheetState = sheetState,
            snackbarHostState = snackbarHostState,
        ),
        sheetPeekHeight = 104.dp + bottomInset,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = Color.Transparent,
        sheetShadowElevation = 0.dp,
        sheetTonalElevation = 0.dp,
        sheetDragHandle = {
            if (isExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = AppTheme.spacing.s, bottom = AppTheme.spacing.xs),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(width = 36.dp, height = 4.dp)
                            .clip(AppTheme.shapes.pill)
                            .background(AppTheme.colors.hairline),
                    )
                }
            } else {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = AppTheme.spacing.screenHorizontal),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    EntryHandle(
                        modifier = Modifier.widthIn(max = 480.dp),
                        onClick = { scope.launch { sheetState.expand() } },
                    )
                    Spacer(modifier = Modifier.height(52.dp + bottomInset))
                }
            }
        },
        sheetContent = {
            if (useCenteredEntryDialog) {
                Spacer(modifier = Modifier.height(1.dp))
            } else {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            color = AppTheme.colors.surface,
                            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
                        ),
                    contentAlignment = Alignment.Center,
                ) {
                    EntrySheet(
                        modifier = Modifier.widthIn(max = 480.dp),
                        viewModel = entryVm,
                        focusRequester = focusRequester,
                        onSaved = { scope.launch { sheetState.partialExpand() } },
                        onSaveFailed = { scope.launch { snackbarHostState.showSnackbar(saveFailedMessage) } },
                    )
                }
            }
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        containerColor = AppTheme.colors.background,
    ) { padding ->
        TodayContent(
            state = state,
            categories = allCategories,
            onSettingsClick = onNavigateToSettings ?: {},
            onExpenseClick = { expense -> recordVm.startEditing(expense) },
            onDeleteExpense = { expense ->
                recordVm.deleteExpense(expense)
                scope.launch {
                    snackbarHostState.currentSnackbarData?.dismiss()
                    val result = snackbarHostState.showSnackbar(
                        message = deletedMessage,
                        actionLabel = undoLabel,
                        duration = SnackbarDuration.Short,
                    )
                    if (result == SnackbarResult.ActionPerformed) {
                        recordVm.undoDelete(expense.id)
                    }
                }
            },
            modifier = Modifier.padding(padding),
        )
    }

    if (useCenteredEntryDialog && isExpanded) {
        Dialog(onDismissRequest = { scope.launch { sheetState.partialExpand() } }) {
            Surface(
                modifier = Modifier.widthIn(max = 480.dp),
                shape = AppTheme.shapes.sheet,
                color = AppTheme.colors.surface,
                tonalElevation = 6.dp,
            ) {
                EntrySheet(
                    modifier = Modifier.widthIn(max = 480.dp),
                    viewModel = entryVm,
                    focusRequester = focusRequester,
                    onSaved = { scope.launch { sheetState.partialExpand() } },
                    onSaveFailed = { scope.launch { snackbarHostState.showSnackbar(saveFailedMessage) } },
                )
            }
        }
    }

    if (editingExpense != null) {
        EditSheet(
            expense = editingExpense!!,
            categories = allCategories,
            onSave = { updated ->
                recordVm.updateExpense(updated)
            },
            onDismiss = {
                recordVm.clearEditing()
            },
        )
    }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TodayContent(
    state: RecordUiState,
    categories: List<CategoryEntity> = emptyList(),
    modifier: Modifier = Modifier,
    onSettingsClick: () -> Unit = {},
    onExpenseClick: (ExpenseEntity) -> Unit = {},
    onDeleteExpense: (ExpenseEntity) -> Unit = {},
) {
    val settingsCd = stringResource(R.string.cd_settings)

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
                    end = AppTheme.spacing.screenHorizontal,
                    top = AppTheme.spacing.s,
                    bottom = AppTheme.spacing.s,
                ),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = state.todayDateLabel,
                style = AppTheme.typography.title.copy(
                    color = AppTheme.colors.textPrimary,
                    fontWeight = FontWeight.Medium,
                ),
            )
            // 36dp circular container with 48dp accessible touch target
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clickable(
                        role = Role.Button,
                        onClick = onSettingsClick,
                        onClickLabel = settingsCd,
                    )
                    .semantics {
                        contentDescription = settingsCd
                    },
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(AppTheme.colors.surfaceVariant)
                        .border(width = 0.5.dp, color = AppTheme.colors.hairline, shape = CircleShape),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Settings,
                        contentDescription = null,
                        tint = AppTheme.colors.textPrimary,
                        modifier = Modifier.size(18.dp),
                    )
                }
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
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                BigAmount(
                    amount = state.amountString,
                    currencySymbol = state.currencySymbol,
                    currencyCode = state.currencyCode,
                    contentDescription = state.formattedTotal,
                )
                state.otherCurrencyTotals?.let { totals ->
                    Text(
                        text = stringResource(R.string.record_other_currency_totals, totals),
                        style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                    )
                }
            }
        }

        // LazyColumn of today's expenses or Empty state (clean centered typographic composition)
        if (state.expenses.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .padding(horizontal = AppTheme.spacing.screenHorizontal),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = stringResource(R.string.record_empty_title),
                        style = AppTheme.typography.title.copy(
                            color = AppTheme.colors.textPrimary,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                    Text(
                        text = stringResource(R.string.record_empty_subtitle),
                        style = AppTheme.typography.bodySecondary.copy(
                            color = AppTheme.colors.textSecondary,
                        ),
                    )
                    Spacer(modifier = Modifier.height(AppTheme.spacing.l))
                    Text(
                        text = stringResource(R.string.brand_motto),
                        style = AppTheme.typography.bodySecondary.copy(
                            color = AppTheme.colors.textTertiary,
                            fontStyle = FontStyle.Italic,
                            fontSize = 13.sp,
                        ),
                    )
                }
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
                    val currentExpense by rememberUpdatedState(expense)
                    val currentOnDeleteExpense by rememberUpdatedState(onDeleteExpense)

                    val dismissState = rememberSwipeToDismissBoxState(
                        confirmValueChange = { dismissValue ->
                            if (dismissValue == SwipeToDismissBoxValue.EndToStart || dismissValue == SwipeToDismissBoxValue.StartToEnd) {
                                currentOnDeleteExpense(currentExpense)
                                true
                            } else {
                                false
                            }
                        }
                    )

                    LaunchedEffect(expense.id) {
                        if (dismissState.currentValue != SwipeToDismissBoxValue.Settled) {
                            dismissState.snapTo(SwipeToDismissBoxValue.Settled)
                        }
                    }

                    SwipeToDismissBox(
                        state = dismissState,
                        backgroundContent = {
                            val alignment = if (dismissState.dismissDirection == SwipeToDismissBoxValue.StartToEnd) {
                                Alignment.CenterStart
                            } else {
                                Alignment.CenterEnd
                            }
                            Box(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .background(AppTheme.colors.hairline)
                                    .padding(horizontal = AppTheme.spacing.screenHorizontal),
                                contentAlignment = alignment,
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(36.dp)
                                        .clip(CircleShape)
                                        .background(AppTheme.colors.surfaceVariant),
                                    contentAlignment = Alignment.Center,
                                ) {
                                    Icon(
                                        imageVector = Icons.Rounded.Delete,
                                        contentDescription = stringResource(R.string.cd_delete),
                                        tint = AppTheme.colors.textPrimary,
                                        modifier = Modifier.size(20.dp),
                                    )
                                }
                            }
                        },
                        content = {
                            val categoryName = resolveCategoryDisplayName(expense.categoryId, categories)
                            val note = expense.note.ifBlank { categoryName }
                            val formatted = formatMinor(expense.amountMinor, expense.currency)
                            EntryRow(
                                note = note,
                                category = categoryName,
                                amount = formatted,
                                onClick = { onExpenseClick(expense) },
                                showDivider = true,
                                modifier = Modifier.background(AppTheme.colors.background),
                            )
                        },
                    )
                }
            }
        }
    }
}

@Composable
fun resolveCategoryDisplayName(categoryId: String, categories: List<CategoryEntity> = emptyList()): String {
    val customName = categories.firstOrNull { it.id == categoryId }?.nameOverride
    if (!customName.isNullOrBlank()) return customName
    val resId = CategoryEntity.defaultNameRes(categoryId)
    return if (resId != null) stringResource(resId) else categoryId
}
