package com.yourco.spend.feature.settings

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material.icons.rounded.Add
import androidx.compose.material.icons.rounded.Check
import androidx.compose.material.icons.rounded.Edit
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.designsystem.components.SegmentedControl
import com.yourco.spend.data.repo.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate

private val SUPPORTED_CURRENCIES = listOf("USD", "TWD", "EUR", "JPY", "GBP", "CNY", "CAD", "AUD", "HKD", "SGD", "KRW")

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

    val exportSuccessMsg = stringResource(R.string.settings_export_success)
    val exportFailedMsg = stringResource(R.string.settings_export_failed)
    val importFailedMsg = stringResource(R.string.settings_import_failed)

    // File pickers
    val exportJsonLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri: Uri? ->
        if (uri != null) {
            scope.launch {
                try {
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            viewModel.exportJson(stream)
                        }
                    }
                    snackbarHostState.showSnackbar(exportSuccessMsg)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: exportFailedMsg)
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
                    withContext(Dispatchers.IO) {
                        context.contentResolver.openOutputStream(uri)?.use { stream ->
                            viewModel.exportCsv(stream)
                        }
                    }
                    snackbarHostState.showSnackbar(exportSuccessMsg)
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: exportFailedMsg)
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
                        snackbarHostState.showSnackbar(result?.error ?: importFailedMsg)
                    }
                } catch (e: Exception) {
                    snackbarHostState.showSnackbar(e.message ?: importFailedMsg)
                }
            }
        }
    }

    // Category dialog states
    var categoryToRename by remember { mutableStateOf<CategoryUiItem?>(null) }
    var renameText by remember { mutableStateOf("") }
    var showAddCategoryDialog by remember { mutableStateOf(false) }
    var newCategoryText by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        containerColor = AppTheme.colors.background,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onNavigateBack) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Rounded.ArrowBack,
                        contentDescription = stringResource(R.string.cd_back),
                        tint = AppTheme.colors.textPrimary,
                    )
                }
                Spacer(modifier = Modifier.width(AppTheme.spacing.s))
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
            // 1. Appearance Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_appearance))
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

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 2. Language Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_language))
            val sysLang = stringResource(R.string.language_system)
            val enLang = stringResource(R.string.language_en)
            val zhTwLang = stringResource(R.string.language_zh_tw)
            val languageOptions = remember(sysLang, enLang, zhTwLang) {
                listOf(sysLang, enLang, zhTwLang)
            }
            val selectedLangIndex = when (state.languageTag) {
                "en" -> 1
                "zh-TW" -> 2
                else -> 0
            }
            SegmentedControl(
                items = languageOptions,
                selectedIndex = selectedLangIndex,
                onSelectedIndexChange = { index ->
                    when (index) {
                        0 -> viewModel.setLanguageTag("")
                        1 -> viewModel.setLanguageTag("en")
                        2 -> viewModel.setLanguageTag("zh-TW")
                    }
                },
            )

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 3. Currency Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_currency))
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                SUPPORTED_CURRENCIES.forEach { code ->
                    val isSelected = code == state.currencyCode
                    val bgColor = if (isSelected) AppTheme.colors.textPrimary else AppTheme.colors.surface
                    val textColor = if (isSelected) AppTheme.colors.background else AppTheme.colors.textPrimary
                    Box(
                        modifier = Modifier
                            .clip(AppTheme.shapes.pill)
                            .background(bgColor)
                            .clickable { viewModel.setCurrencyCode(code) }
                            .padding(horizontal = AppTheme.spacing.m, vertical = AppTheme.spacing.s),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = code,
                            style = AppTheme.typography.label.copy(
                                color = textColor,
                                fontWeight = if (isSelected) FontWeight.Medium else FontWeight.Normal,
                            ),
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 4. Categories Section
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

            // 5. Data & Backup Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_data))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(AppTheme.shapes.card)
                    .background(AppTheme.colors.surface)
                    .padding(AppTheme.spacing.m),
                verticalArrangement = Arrangement.spacedBy(AppTheme.spacing.s),
            ) {
                OutlinedButton(
                    onClick = {
                        val today = LocalDate.now().toString()
                        exportJsonLauncher.launch("mote_backup_$today.json")
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
                        exportCsvLauncher.launch("mote_expenses_$today.csv")
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
            }

            Spacer(modifier = Modifier.height(AppTheme.spacing.xl))

            // 6. About & Privacy Section
            SettingsSectionHeader(title = stringResource(R.string.settings_section_about))
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = AppTheme.spacing.s),
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
