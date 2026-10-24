package com.nexusbench.nexpense.feature.settings

import android.net.Uri
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.ChevronRight
import androidx.compose.material.icons.rounded.Edit
import androidx.compose.material.icons.rounded.KeyboardArrowDown
import androidx.compose.material.icons.rounded.KeyboardArrowUp
import androidx.compose.material.icons.rounded.Visibility
import androidx.compose.material.icons.rounded.VisibilityOff
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexusbench.nexpense.R
import com.nexusbench.nexpense.core.designsystem.AppTheme
import com.nexusbench.nexpense.core.designsystem.components.SegmentedControl
import com.nexusbench.nexpense.core.language.LanguageCatalog
import com.nexusbench.nexpense.core.money.CurrencyCatalog
import com.nexusbench.nexpense.data.repo.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: SettingsViewModel = hiltViewModel(),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    BackHandler { onNavigateBack() }

    val exportSuccessMsg = stringResource(R.string.settings_export_success)
    val exportFailedMsg = stringResource(R.string.settings_export_failed)
    val importFailedMsg = stringResource(R.string.settings_import_failed)
    val wipeSucceededMsg = stringResource(R.string.settings_wipe_success)
    val wipeFailedMsg = stringResource(R.string.settings_wipe_failed)

    LaunchedEffect(viewModel, snackbarHostState) {
        viewModel.wipeResultEvents.collect { succeeded ->
            snackbarHostState.showSnackbar(if (succeeded) wipeSucceededMsg else wipeFailedMsg)
        }
    }

    // File pickers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                        if (stream != null) {
                            stream.use { output ->
                                viewModel.exportJson(output)
                            }
                            true
                        } else {
                            false
                        }
                    }
                    if (success) {
                        snackbarHostState.showSnackbar(exportSuccessMsg)
                    } else {
                        snackbarHostState.showSnackbar(exportFailedMsg)
                    }
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(exportFailedMsg)
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val success = withContext(Dispatchers.IO) {
                        val stream = context.contentResolver.openOutputStream(uri)
                        if (stream != null) {
                            stream.use { output ->
                                viewModel.exportCsv(output)
                            }
                            true
                        } else {
                            false
                        }
                    }
                    if (success) {
                        snackbarHostState.showSnackbar(exportSuccessMsg)
                    } else {
                        snackbarHostState.showSnackbar(exportFailedMsg)
                    }
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(exportFailedMsg)
                }
            }
        }
    }

    val importLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    val result = withContext(Dispatchers.IO) {
                        context.contentResolver.openInputStream(uri)?.use { stream ->
                            viewModel.importData(stream)
                        }
                    }
                    if (result != null && result.success) {
                        snackbarHostState.showSnackbar(
                            context.getString(
                                R.string.settings_import_success,
                                result.expensesImported,
                                result.categoriesImported,
                            )
                        )
                    } else {
                        snackbarHostState.showSnackbar(importFailedMsg)
                    }
                } catch (_: Exception) {
                    snackbarHostState.showSnackbar(importFailedMsg)
                }
            }
        }
    }

    // Category dialog states
    var categoryToRename by remember { mutableStateOf<CategoryUiItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var showWipeConfirmation by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }
    var showCurrencyPicker by rememberSaveable { mutableStateOf(false) }
    var showLanguagePicker by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppTheme.spacing.screenHorizontal, vertical = AppTheme.spacing.m),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Box(
                    modifier = Modifier.size(48.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(AppTheme.colors.surfaceVariant)
                            .border(
                                width = 0.5.dp,
                                color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                                shape = CircleShape,
                            )
                            .clickable(onClick = onNavigateBack),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                            tint = AppTheme.colors.textPrimary,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
                Spacer(modifier = Modifier.width(AppTheme.spacing.m))
                Text(
                    text = stringResource(R.string.settings_title),
                    style = AppTheme.typography.title.copy(color = AppTheme.colors.textPrimary),
                )
            }
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppTheme.spacing.screenHorizontal)
                .navigationBarsPadding(),
        ) {
            // 1. Appearance & Preferences Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_preferences))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.card)
                    .background(AppTheme.colors.surface)
                    .border(
                        width = 0.5.dp,
                        color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                        shape = AppTheme.shapes.card,
                    )
                    .padding(AppTheme.spacing.m),
            ) {
                // Theme Mode
                Text(
                    text = stringResource(R.string.settings_theme),
                    style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
                    modifier = Modifier.padding(bottom = AppTheme.spacing.s),
                )
                val sysTheme = stringResource(R.string.theme_system)
                val lightTheme = stringResource(R.string.theme_light)
                val darkTheme = stringResource(R.string.theme_dark)
                val appearanceOptions = remember(sysTheme, lightTheme, darkTheme) {
                    listOf(sysTheme, lightTheme, darkTheme)
                }
                val selectedThemeIndex = when (state.themeMode) {
                    ThemeMode.SYSTEM -> 0
                    ThemeMode.LIGHT -> 1
                    ThemeMode.DARK -> 2
                }
                SegmentedControl(
                    items = appearanceOptions,
                    selectedIndex = selectedThemeIndex,
                    onSelectedIndexChange = { index ->
                        when (index) {
                            0 -> viewModel.setThemeMode(ThemeMode.SYSTEM)
                            1 -> viewModel.setThemeMode(ThemeMode.LIGHT)
                            2 -> viewModel.setThemeMode(ThemeMode.DARK)
                        }
                    },
                )

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppTheme.spacing.m),
                    thickness = 0.5.dp,
                    color = AppTheme.colors.hairline,
                )

                // Language
                Text(
                    text = stringResource(R.string.settings_section_language),
                    style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
                    modifier = Modifier.padding(bottom = AppTheme.spacing.s),
                )

                val currentLanguageItem = remember(state.languageTag) {
                    LanguageCatalog.findLanguage(state.languageTag)
                }
                val languageDisplay = currentLanguageItem?.displayLabel() ?: stringResource(R.string.language_system)

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.pill)
                        .background(AppTheme.colors.surfaceVariant)
                        .border(width = 0.5.dp, color = AppTheme.colors.hairline, shape = AppTheme.shapes.pill)
                        .clickable(
                            role = Role.Button,
                            onClick = { showLanguagePicker = true },
                            onClickLabel = stringResource(R.string.settings_change_language),
                        )
                        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(AppTheme.shapes.pill)
                                .background(AppTheme.colors.surface)
                                .border(
                                    width = 0.5.dp,
                                    color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                                    shape = AppTheme.shapes.pill,
                                )
                                .padding(horizontal = AppTheme.spacing.s, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = if (state.languageTag.isEmpty()) "SYS" else state.languageTag.uppercase(),
                                style = AppTheme.typography.label.copy(
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                    fontSize = 11.sp,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.width(AppTheme.spacing.s))
                        Text(
                            text = languageDisplay,
                            style = AppTheme.typography.body.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }

                HorizontalDivider(
                    modifier = Modifier.padding(vertical = AppTheme.spacing.m),
                    thickness = 0.5.dp,
                    color = AppTheme.colors.hairline,
                )

                // Currency
                Text(
                    text = stringResource(R.string.settings_section_currency),
                    style = AppTheme.typography.label.copy(color = AppTheme.colors.textSecondary),
                    modifier = Modifier.padding(bottom = AppTheme.spacing.s),
                )

                val currentCurrencyItem = remember(state.currencyCode) {
                    CurrencyCatalog.findCurrency(state.currencyCode)
                }
                val currencyDisplay = currentCurrencyItem?.displayLabel() ?: state.currencyCode

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(AppTheme.shapes.pill)
                        .background(AppTheme.colors.surfaceVariant)
                        .border(width = 0.5.dp, color = AppTheme.colors.hairline, shape = AppTheme.shapes.pill)
                        .clickable(
                            role = Role.Button,
                            onClick = { showCurrencyPicker = true },
                            onClickLabel = stringResource(R.string.settings_change_currency),
                        )
                        .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.weight(1f),
                    ) {
                        Box(
                            modifier = Modifier
                                .clip(AppTheme.shapes.pill)
                                .background(AppTheme.colors.surface)
                                .border(
                                    width = 0.5.dp,
                                    color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                                    shape = AppTheme.shapes.pill,
                                )
                                .padding(horizontal = AppTheme.spacing.s, vertical = 4.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            Text(
                                text = currentCurrencyItem?.symbol ?: "$",
                                style = AppTheme.typography.label.copy(
                                    color = AppTheme.colors.textPrimary,
                                    fontWeight = FontWeight.SemiBold,
                                ),
                            )
                        }
                        Spacer(modifier = Modifier.width(AppTheme.spacing.s))
                        Text(
                            text = currencyDisplay,
                            style = AppTheme.typography.body.copy(
                                color = AppTheme.colors.textPrimary,
                                fontWeight = FontWeight.Medium,
                            ),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }

                    Icon(
                        imageVector = Icons.Rounded.ChevronRight,
                        contentDescription = null,
                        tint = AppTheme.colors.textSecondary,
                        modifier = Modifier.size(18.dp),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 2. Categories Section
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                SettingsSectionHeader(title = stringResource(R.string.settings_section_categories))
                TextButton(
                    onClick = {
                        newCategoryText = ""
                        showAddCategoryDialog = true
                    },
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Add,
                        contentDescription = null,
                        modifier = Modifier.size(18.dp),
                        tint = AppTheme.colors.textPrimary,
                    )
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.settings_add_category),
                        style = AppTheme.typography.label.copy(color = AppTheme.colors.textPrimary),
                    )
                }
            }

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.card)
                    .background(AppTheme.colors.surface)
                    .border(
                        width = 0.5.dp,
                        color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                        shape = AppTheme.shapes.card,
                    )
                    .padding(vertical = AppTheme.spacing.xs),
            ) {
                state.categories.forEachIndexed { index, category ->
                    val displayName = category.nameRes?.let { stringResource(it) } ?: category.name
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable {
                                categoryToRename = category
                                renameText = displayName
                            }
                            .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.m),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.weight(1f),
                        ) {
                            Text(
                                text = displayName,
                                style = AppTheme.typography.body.copy(
                                    color = if (category.isHidden) AppTheme.colors.textSecondary else AppTheme.colors.textPrimary,
                                ),
                            )
                            if (category.isHidden) {
                                Spacer(modifier = Modifier.width(AppTheme.spacing.s))
                                Text(
                                    text = "(${stringResource(R.string.settings_hide)})",
                                    style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                                )
                            }
                        }

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            IconButton(
                                onClick = { viewModel.moveCategory(category.id, -1) },
                                enabled = index > 0,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowUp,
                                    contentDescription = stringResource(R.string.settings_move_category_up),
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.moveCategory(category.id, 1) },
                                enabled = index < state.categories.lastIndex,
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.KeyboardArrowDown,
                                    contentDescription = stringResource(R.string.settings_move_category_down),
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = {
                                    categoryToRename = category
                                    renameText = displayName
                                },
                            ) {
                                Icon(
                                    imageVector = Icons.Rounded.Edit,
                                    contentDescription = stringResource(R.string.settings_rename_category),
                                    tint = AppTheme.colors.textSecondary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                            IconButton(
                                onClick = { viewModel.toggleCategoryHidden(category.id, category.isHidden) },
                            ) {
                                Icon(
                                    imageVector = if (category.isHidden) Icons.Rounded.VisibilityOff else Icons.Rounded.Visibility,
                                    contentDescription = stringResource(R.string.cd_toggle_hidden),
                                    tint = if (category.isHidden) AppTheme.colors.textSecondary else AppTheme.colors.textPrimary,
                                    modifier = Modifier.size(20.dp),
                                )
                            }
                        }
                    }
                    if (index < state.categories.lastIndex) {
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = AppTheme.spacing.m),
                            thickness = 0.5.dp,
                            color = AppTheme.colors.hairline,
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 3. Data & Backup Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_data))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.card)
                    .background(AppTheme.colors.surface)
                    .border(
                        width = 0.5.dp,
                        color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                        shape = AppTheme.shapes.card,
                    )
                    .padding(AppTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                OutlinedButton(
                    onClick = {
                        val today = LocalDate.now().toString()
                        exportJsonLauncher.launch("nexpense_backup_$today.json")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.pill,
                ) {
                    Text(
                        text = stringResource(R.string.settings_export_json),
                        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                    )
                }

                OutlinedButton(
                    onClick = {
                        val today = LocalDate.now().toString()
                        exportCsvLauncher.launch("nexpense_expenses_$today.csv")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.pill,
                ) {
                    Text(
                        text = stringResource(R.string.settings_export_csv),
                        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                    )
                }

                Button(
                    onClick = {
                        importLauncher.launch(
                            arrayOf("application/json", "text/csv", "text/comma-separated-values", "text/plain", "*/*")
                        )
                    },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.pill,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = AppTheme.colors.textPrimary,
                        contentColor = AppTheme.colors.background,
                    ),
                ) {
                    Text(
                        text = stringResource(R.string.settings_import_data),
                        style = AppTheme.typography.body.copy(fontWeight = FontWeight.Medium),
                    )
                }

                OutlinedButton(
                    onClick = { showWipeConfirmation = true },
                    modifier = Modifier.fillMaxWidth(),
                    shape = AppTheme.shapes.pill,
                ) {
                    Text(
                        text = stringResource(R.string.settings_wipe_data),
                        style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                    )
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 4. About & Privacy Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.card)
                    .background(AppTheme.colors.surface)
                    .border(
                        width = 0.5.dp,
                        color = if (AppTheme.colors.isDark) AppTheme.colors.rimHighlight else AppTheme.colors.hairline,
                        shape = AppTheme.shapes.card,
                    )
                    .padding(AppTheme.spacing.m),
            ) {
                Text(
                    text = stringResource(R.string.settings_version, state.appVersion),
                    style = AppTheme.typography.body.copy(color = AppTheme.colors.textPrimary),
                )
                Spacer(modifier = Modifier.height(AppTheme.spacing.xs))
                Text(
                    text = stringResource(R.string.settings_privacy_note),
                    style = AppTheme.typography.bodySecondary.copy(color = AppTheme.colors.textSecondary),
                )
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xxl))
        }
    }

    // Rename Dialog
    if (categoryToRename != null) {
        val cat = categoryToRename!!
        AlertDialog(
            onDismissRequest = { categoryToRename = null },
            title = {
                Text(
                    text = stringResource(R.string.settings_rename_category),
                    style = AppTheme.typography.title,
                )
            },
            text = {
                OutlinedTextField(
                    value = renameText,
                    onValueChange = { renameText = it },
                    label = { Text(stringResource(R.string.settings_category_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.textPrimary,
                        cursorColor = AppTheme.colors.textPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCategory(cat.id, renameText)
                        categoryToRename = null
                    },
                ) {
                    Text(stringResource(R.string.save), color = AppTheme.colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { categoryToRename = null }) {
                    Text(stringResource(R.string.cancel), color = AppTheme.colors.textSecondary)
                }
            },
            containerColor = AppTheme.colors.surface,
        )
    }

    // Add Category Dialog
    if (showAddCategoryDialog) {
        AlertDialog(
            onDismissRequest = { showAddCategoryDialog = false },
            title = {
                Text(
                    text = stringResource(R.string.settings_add_category),
                    style = AppTheme.typography.title,
                )
            },
            text = {
                OutlinedTextField(
                    value = newCategoryText,
                    onValueChange = { newCategoryText = it },
                    label = { Text(stringResource(R.string.settings_category_name)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = AppTheme.colors.textPrimary,
                        cursorColor = AppTheme.colors.textPrimary,
                    ),
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.addCustomCategory(newCategoryText)
                        showAddCategoryDialog = false
                    },
                ) {
                    Text(stringResource(R.string.save), color = AppTheme.colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddCategoryDialog = false }) {
                    Text(stringResource(R.string.cancel), color = AppTheme.colors.textSecondary)
                }
            },
            containerColor = AppTheme.colors.surface,
        )
    }

    if (showWipeConfirmation) {
        AlertDialog(
            onDismissRequest = { showWipeConfirmation = false },
            title = { Text(stringResource(R.string.settings_wipe_title), style = AppTheme.typography.title) },
            text = { Text(stringResource(R.string.settings_wipe_message), style = AppTheme.typography.body) },
            confirmButton = {
                TextButton(onClick = {
                    showWipeConfirmation = false
                    viewModel.wipeAllData()
                }) {
                    Text(stringResource(R.string.settings_wipe_confirm), color = AppTheme.colors.textPrimary)
                }
            },
            dismissButton = {
                TextButton(onClick = { showWipeConfirmation = false }) {
                    Text(stringResource(R.string.cancel), color = AppTheme.colors.textSecondary)
                }
            },
            containerColor = AppTheme.colors.surface,
        )
    }

    if (showCurrencyPicker) {
        CurrencyPickerBottomSheet(
            selectedCurrencyCode = state.currencyCode,
            onCurrencySelected = { code ->
                viewModel.setCurrencyCode(code)
                showCurrencyPicker = false
            },
            onDismissRequest = { showCurrencyPicker = false },
        )
    }

    if (showLanguagePicker) {
        LanguagePickerBottomSheet(
            selectedLanguageTag = state.languageTag,
            onLanguageSelected = { tag ->
                viewModel.setLanguageTag(tag)
                showLanguagePicker = false
            },
            onDismissRequest = { showLanguagePicker = false },
        )
    }
}

@Composable
private fun SettingsSectionHeader(title: String) {
    Text(
        text = title,
        style = AppTheme.typography.title.copy(fontSize = 18.sp),
        color = AppTheme.colors.textPrimary,
        modifier = Modifier.padding(bottom = AppTheme.spacing.s),
    )
}
