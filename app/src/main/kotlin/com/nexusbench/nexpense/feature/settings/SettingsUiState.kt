package com.nexusbench.nexpense.feature.settings

import com.nexusbench.nexpense.data.repo.ThemeMode

data class CategoryUiItem(
    val id: String,
    val name: String,
    val nameRes: Int?,
    val icon: String,
    val isHidden: Boolean,
    val isBuiltin: Boolean,
)

data class SettingsUiState(
    val themeMode: ThemeMode = ThemeMode.SYSTEM,
    val languageTag: String = "",
    val currencyCode: String = "USD",
    val categories: List<CategoryUiItem> = emptyList(),
    val appVersion: String = "1.0.0",
    val message: String? = null,
)
