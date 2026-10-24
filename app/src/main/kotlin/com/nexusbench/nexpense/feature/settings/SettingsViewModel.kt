package com.nexusbench.nexpense.feature.settings

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.export.CsvExporter
import com.nexusbench.nexpense.data.export.DataImporter
import com.nexusbench.nexpense.data.export.ImportResult
import com.nexusbench.nexpense.data.export.JsonExporter
import com.nexusbench.nexpense.data.repo.CategoryRepository
import com.nexusbench.nexpense.data.repo.ExpenseRepository
import com.nexusbench.nexpense.data.repo.SettingsRepository
import com.nexusbench.nexpense.data.repo.ThemeMode
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val expenseRepository: ExpenseRepository,
    private val dataImporter: DataImporter,
    private val jsonExporter: JsonExporter,
    private val csvExporter: CsvExporter,
) : ViewModel() {

    private val _messageEvents = MutableSharedFlow<String>(extraBufferCapacity = 1)
    val messageEvents: SharedFlow<String> = _messageEvents.asSharedFlow()

    private val _wipeResultEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val wipeResultEvents = _wipeResultEvents.asSharedFlow()

    val state: StateFlow<SettingsUiState> = combine(
        settingsRepository.themeMode,
        settingsRepository.languageTag,
        settingsRepository.currencyCode,
        categoryRepository.observeAll(),
    ) { theme, lang, currency, categories ->
        val categoryItems = categories.map { cat ->
            val nameRes = if (cat.nameOverride.isNullOrBlank()) CategoryEntity.defaultNameRes(cat.id) else null
            val name = when {
                !cat.nameOverride.isNullOrBlank() -> cat.nameOverride
                cat.id == CategoryEntity.CATEGORY_FOOD -> "Food & Drink"
                cat.id == CategoryEntity.CATEGORY_GROCERIES -> "Groceries"
                cat.id == CategoryEntity.CATEGORY_TRANSPORT -> "Transport"
                cat.id == CategoryEntity.CATEGORY_SHOPPING -> "Shopping"
                cat.id == CategoryEntity.CATEGORY_HOME -> "Home & Bills"
                cat.id == CategoryEntity.CATEGORY_HEALTH -> "Health"
                cat.id == CategoryEntity.CATEGORY_FUN -> "Fun"
                cat.id == CategoryEntity.CATEGORY_TRAVEL -> "Travel"
                cat.id == CategoryEntity.CATEGORY_OTHER -> "Other"
                else -> cat.id
            }
            CategoryUiItem(
                id = cat.id,
                name = name,
                nameRes = nameRes,
                icon = cat.icon,
                isHidden = cat.isHidden,
                isBuiltin = cat.isBuiltin,
            )
        }
        SettingsUiState(
            themeMode = theme,
            languageTag = lang,
            currencyCode = currency,
            categories = categoryItems,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = SettingsUiState(),
    )

    fun setThemeMode(mode: ThemeMode) {
        viewModelScope.launch {
            settingsRepository.setThemeMode(mode)
        }
    }

    fun setLanguageTag(tag: String) {
        viewModelScope.launch {
            settingsRepository.setLanguageTag(tag)
            val locales = if (tag.isBlank()) {
                LocaleListCompat.getEmptyLocaleList()
            } else {
                LocaleListCompat.forLanguageTags(tag)
            }
            AppCompatDelegate.setApplicationLocales(locales)
        }
    }

    // VERIFY: Changing currency affects new entries only
    fun setCurrencyCode(code: String) {
        viewModelScope.launch {
            settingsRepository.setCurrencyCode(code)
        }
    }

    fun renameCategory(id: String, newName: String) {
        viewModelScope.launch {
            val trimmed = newName.trim()
            categoryRepository.updateNameOverride(id, if (trimmed.isBlank()) null else trimmed)
        }
    }

    fun toggleCategoryHidden(id: String, currentHidden: Boolean) {
        viewModelScope.launch {
            categoryRepository.setHidden(id, !currentHidden)
        }
    }

    fun moveCategory(id: String, offset: Int) {
        viewModelScope.launch {
            val categories = categoryRepository.getAll().sortedBy { it.sortOrder }.toMutableList()
            val from = categories.indexOfFirst { it.id == id }
            val to = from + offset
            if (from !in categories.indices || to !in categories.indices) return@launch
            val moved = categories.removeAt(from)
            categories.add(to, moved)
            categories.forEachIndexed { index, category ->
                if (category.sortOrder != index) {
                    categoryRepository.upsert(category.copy(sortOrder = index))
                }
            }
        }
    }

    fun addCustomCategory(name: String) {
        val trimmed = name.trim()
        if (trimmed.isBlank()) return
        viewModelScope.launch {
            val all = categoryRepository.getAll()
            val sortOrder = (all.maxOfOrNull { it.sortOrder } ?: 0) + 1
            val id = "custom_" + UUID.randomUUID().toString().take(8)
            val newCategory = CategoryEntity(
                id = id,
                nameOverride = trimmed,
                icon = "category",
                sortOrder = sortOrder,
                isHidden = false,
                isBuiltin = false,
            )
            categoryRepository.upsert(newCategory)
        }
    }

    fun wipeAllData() {
        viewModelScope.launch {
            try {
                expenseRepository.clearUserData()
                settingsRepository.clearAllSettings()
                AppCompatDelegate.setApplicationLocales(LocaleListCompat.getEmptyLocaleList())
                _wipeResultEvents.tryEmit(true)
            } catch (_: Exception) {
                _wipeResultEvents.tryEmit(false)
            }
        }
    }

    suspend fun exportJson(outputStream: OutputStream) {
        jsonExporter.export(outputStream)
    }

    suspend fun exportCsv(outputStream: OutputStream) {
        csvExporter.export(outputStream)
    }

    suspend fun importData(inputStream: InputStream): ImportResult {
        return dataImporter.importFromStream(inputStream)
    }
}
