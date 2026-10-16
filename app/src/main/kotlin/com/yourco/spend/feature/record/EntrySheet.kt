package com.yourco.spend.feature.record

import android.os.Build
import android.view.HapticFeedbackConstants
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.CategoryChip
import com.yourco.spend.data.db.entity.CategoryEntity

@Composable
fun EntrySheet(
    modifier: Modifier = Modifier,
    viewModel: EntryViewModel = hiltViewModel(),
    focusRequester: FocusRequester = remember { FocusRequester() },
    onSaved: () -> Unit = {},
) {
    val text by viewModel.text.collectAsStateWithLifecycle()
    val preview by viewModel.preview.collectAsStateWithLifecycle()
    val categories by viewModel.categories.collectAsStateWithLifecycle()
    val selectedCategoryOverride by viewModel.selectedCategoryOverride.collectAsStateWithLifecycle()
    var showCategoryPicker by remember { mutableStateOf(false) }

    val haptic = LocalHapticFeedback.current
    val view = LocalView.current

    LaunchedEffect(viewModel) {
        viewModel.confirmHapticEvents.collect {
            // GUIDE 3.3: HapticFeedbackType.Confirm on save (API 30+, fallback to TextHandleMove)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
            } else {
                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
            }
        }
    }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .widthIn(max = 480.dp)
            .imePadding()
            .padding(horizontal = AppTheme.spacing.screenHorizontal)
            .padding(bottom = AppTheme.spacing.xl, top = AppTheme.spacing.s),
    ) {
        // VERIFY: Entry sheet text field single-line, transparent container, headline style
        TextField(
            value = text,
            onValueChange = viewModel::onTextChange,
            modifier = Modifier
                .fillMaxWidth()
                .focusRequester(focusRequester),
            singleLine = true,
            placeholder = {
                Text(
                    text = stringResource(R.string.entry_hint),
                    style = AppTheme.typography.headline.copy(color = AppTheme.colors.textSecondary),
                )
            },
            textStyle = AppTheme.typography.headline.copy(color = AppTheme.colors.textPrimary),
            keyboardOptions = KeyboardOptions(
                keyboardType = KeyboardType.Text,
                imeAction = ImeAction.Done,
            ),
            keyboardActions = KeyboardActions(
                onDone = {
                    if (preview is EntryPreview.Valid) {
                        viewModel.submit()
                        onSaved()
                    }
                }
            ),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                disabledContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
                cursorColor = AppTheme.colors.textPrimary,
            ),
        )

        Spacer(modifier = Modifier.height(AppTheme.spacing.s))

        // Live preview line
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .defaultMinSize(minHeight = 40.dp),
            contentAlignment = Alignment.CenterStart,
        ) {
            when (val p = preview) {
                is EntryPreview.Empty -> {
                    // Empty preview state
                }
                is EntryPreview.NeedsAmount -> {
                    Text(
                        text = stringResource(R.string.entry_need_amount),
                        style = AppTheme.typography.bodySecondary.copy(
                            color = AppTheme.colors.textSecondary,
                        ),
                    )
                }
                is EntryPreview.Valid -> {
                    val categoryEntity = categories.firstOrNull { it.id == p.categoryId }
                    val categoryDisplayName = categoryEntity?.nameOverride
                        ?: categoryEntity?.defaultNameRes()?.let { stringResource(it) }
                        ?: p.categoryNameRes?.let { stringResource(it) }
                        ?: p.categoryName
                    val displayNote = p.note.ifBlank { categoryDisplayName }

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .semantics {
                                set(
                                    androidx.compose.ui.semantics.SemanticsProperties.Text,
                                    listOf(androidx.compose.ui.text.AnnotatedString("${p.formattedAmount} · $displayNote · $categoryDisplayName"))
                                )
                            },
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text(
                            text = "${p.formattedAmount} · $displayNote · ",
                            style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                        )
                        CategoryChip(
                            categoryName = categoryDisplayName,
                            onClick = { showCategoryPicker = true },
                        )
                    }
                }
            }
        }
    }

    if (showCategoryPicker) {
        com.yourco.spend.feature.edit.CategoryPickerDialog(
            categories = categories,
            selectedCategoryId = (preview as? EntryPreview.Valid)?.categoryId ?: selectedCategoryOverride,
            onCategoryPicked = { categoryId ->
                viewModel.onCategoryPicked(categoryId)
                showCategoryPicker = false
            },
            onDismiss = { showCategoryPicker = false },
        )
    }
}

