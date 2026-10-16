package com.yourco.spend.feature.edit

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.CategoryChip
import com.yourco.spend.core.money.fractionDigits
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import java.math.BigDecimal
import java.math.RoundingMode
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/**
 * Parses user input into Long minor units.
 * Never uses Float or Double.
 */
fun parseAmountToMinor(input: String, currencyCode: String): Long? {
    val trimmed = input.trim()
    if (trimmed.isEmpty()) return null
    val digits = fractionDigits(currencyCode)
    return try {
        val normalized = trimmed.replace(",", ".")
        val bd = BigDecimal(normalized).setScale(digits, RoundingMode.HALF_UP)
        if (bd.signum() <= 0 || bd > BigDecimal(10_000_000)) {
            null
        } else {
            bd.movePointRight(digits).longValueExact()
        }
    } catch (_: Exception) {
        null
    }
}

/**
 * ModalBottomSheet presenting the edit sheet for an existing expense.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheet(
    expense: ExpenseEntity,
    categories: List<CategoryEntity>,
    onSave: (ExpenseEntity) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)

    // VERIFY: EditSheet uses 28dp top radius and surface background token
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        shape = AppTheme.shapes.sheet,
        containerColor = AppTheme.colors.surface,
        modifier = modifier,
    ) {
        EditSheetContent(
            expense = expense,
            categories = categories,
            onSave = onSave,
            onCancel = onDismiss,
        )
    }
}

/**
 * The inner content of the EditSheet, exposed for direct testing and modularity.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditSheetContent(
    expense: ExpenseEntity,
    categories: List<CategoryEntity>,
    onSave: (ExpenseEntity) -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val digits = fractionDigits(expense.currency)
    val initialAmountText = remember(expense.id, expense.amountMinor) {
        if (digits > 0) {
            BigDecimal.valueOf(expense.amountMinor, digits).toPlainString()
        } else {
            expense.amountMinor.toString()
        }
    }

    var amountText by remember(expense.id) { mutableStateOf(initialAmountText) }
    var noteText by remember(expense.id) { mutableStateOf(expense.note) }
    var selectedCategoryId by remember(expense.id) { mutableStateOf(expense.categoryId) }
    var selectedLocalDate by remember(expense.id) { mutableStateOf(expense.localDate) }

    var showCategoryPicker by remember { mutableStateOf(false) }
    var showDatePicker by remember { mutableStateOf(false) }

    val parsedMinor = parseAmountToMinor(amountText, expense.currency)
    val isSaveEnabled = parsedMinor != null

    val categoryEntity = categories.firstOrNull { it.id == selectedCategoryId }
    val categoryDisplayName = categoryEntity?.nameOverride
        ?: categoryEntity?.defaultNameRes()?.let { stringResource(it) }
        ?: selectedCategoryId

    Column(
        modifier = modifier
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = AppTheme.spacing.screenHorizontal)
            .padding(bottom = AppTheme.spacing.xl)
            .imePadding()
            .navigationBarsPadding(),
    ) {
        // Top bar with Cancel, Title, and Save
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.spacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            TextButton(onClick = onCancel) {
                Text(
                    text = stringResource(R.string.cancel),
                    style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary),
                )
            }

            Text(
                text = stringResource(R.string.edit_expense),
                style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
            )

            TextButton(
                onClick = {
                    if (parsedMinor != null) {
                        val updated = expense.copy(
                            amountMinor = parsedMinor,
                            note = noteText.trim(),
                            categoryId = selectedCategoryId,
                            localDate = selectedLocalDate,
                        )
                        onSave(updated)
                    }
                },
                enabled = isSaveEnabled,
            ) {
                Text(
                    text = stringResource(R.string.save),
                    style = AppTheme.typography.body.copy(
                        color = if (isSaveEnabled) AppTheme.colors.textPrimary else AppTheme.colors.textSecondary,
                    ),
                )
            }
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.m))

        // Amount Input Field
        OutlinedTextField(
            value = amountText,
            onValueChange = { amountText = it },
            label = {
                Text(
                    text = stringResource(R.string.amount),
                    style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
            textStyle = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.textPrimary,
                unfocusedBorderColor = AppTheme.colors.hairline,
                focusedLabelColor = AppTheme.colors.textPrimary,
                unfocusedLabelColor = AppTheme.colors.textSecondary,
                cursorColor = AppTheme.colors.textPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.m))

        // Note Input Field
        OutlinedTextField(
            value = noteText,
            onValueChange = { noteText = it },
            label = {
                Text(
                    text = stringResource(R.string.note),
                    style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                )
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Text),
            textStyle = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
            modifier = Modifier.fillMaxWidth(),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = AppTheme.colors.textPrimary,
                unfocusedBorderColor = AppTheme.colors.hairline,
                focusedLabelColor = AppTheme.colors.textPrimary,
                unfocusedLabelColor = AppTheme.colors.textSecondary,
                cursorColor = AppTheme.colors.textPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.l))

        // Category Selection Row
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = AppTheme.spacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.category),
                style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary),
            )
            CategoryChip(
                categoryName = categoryDisplayName,
                onClick = { showCategoryPicker = true },
            )
        }

        Spacer(modifier = Modifier.height(AppTheme.spacing.s))

        // Date Selection Row
        // VERIFY: Date selection row displays ISO date and opens DatePicker
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 48.dp)
                .clickable(
                    role = Role.Button,
                    onClick = { showDatePicker = true },
                )
                .padding(vertical = AppTheme.spacing.s),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = stringResource(R.string.date),
                style = AppTheme.typography.body.copy(color = AppTheme.colors.textSecondary),
            )
            Text(
                text = selectedLocalDate,
                style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                modifier = Modifier.semantics {
                    contentDescription = selectedLocalDate
                },
            )
        }
    }

    if (showCategoryPicker) {
        CategoryPickerDialog(
            categories = categories,
            selectedCategoryId = selectedCategoryId,
            onCategoryPicked = { categoryId ->
                selectedCategoryId = categoryId
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }

    if (showDatePicker) {
        val initialDate = try {
            LocalDate.parse(selectedLocalDate)
        } catch (_: Exception) {
            LocalDate.now()
        }
        val initialMillis = initialDate.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
        val datePickerState = rememberDatePickerState(initialSelectedDateMillis = initialMillis)

        DatePickerDialog(
            onDismissRequest = { showDatePicker = false },
            confirmButton = {
                TextButton(
                    onClick = {
                        datePickerState.selectedDateMillis?.let { millis ->
                            val newDate = Instant.ofEpochMilli(millis).atZone(ZoneOffset.UTC).toLocalDate()
                            selectedLocalDate = newDate.toString()
                        }
                        showDatePicker = false
                    },
                ) {
                    Text(
                        text = stringResource(R.string.save),
                        style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary),
                    )
                }
            },
            dismissButton = {
                TextButton(onClick = { showDatePicker = false }) {
                    Text(
                        text = stringResource(R.string.cancel),
                        style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
                    )
                }
            },
            shape = AppTheme.shapes.card,
            colors = androidx.compose.material3.DatePickerDefaults.colors(
                containerColor = AppTheme.colors.surface,
            ),
        ) {
            DatePicker(state = datePickerState)
        }
    }
}
