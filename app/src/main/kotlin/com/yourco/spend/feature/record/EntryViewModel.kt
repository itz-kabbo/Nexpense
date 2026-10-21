package com.yourco.spend.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.core.money.fractionDigits
import com.yourco.spend.core.time.todayLocalDate
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.SettingsRepository
import com.yourco.spend.parser.ExpenseParser
import com.yourco.spend.parser.ParseResult
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.time.Clock
import java.util.Locale
import java.util.UUID
import javax.inject.Inject

@HiltViewModel
class EntryViewModel @Inject constructor(
    private val expenseParser: ExpenseParser,
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val learnedMappingRepository: LearnedMappingRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val todayObserver: com.yourco.spend.core.time.TodayObserver = com.yourco.spend.core.time.TodayObserver.from(clock),
) : ViewModel() {

    companion object {
        const val KEY_TYPED_TEXT = "typed_text"
        const val KEY_SELECTED_CATEGORY_OVERRIDE = "selected_category_override"
    }

    val text: StateFlow<String> = savedStateHandle.getStateFlow(KEY_TYPED_TEXT, "")

    private val _preview = MutableStateFlow<EntryPreview>(EntryPreview.Empty)
    val preview: StateFlow<EntryPreview> = _preview.asStateFlow()

    val selectedCategoryOverride: StateFlow<String?> = savedStateHandle.getStateFlow(KEY_SELECTED_CATEGORY_OVERRIDE, null)

    private val _isSubmitting = MutableStateFlow(false)
    val isSubmitting: StateFlow<Boolean> = _isSubmitting.asStateFlow()

    private val _categories = MutableStateFlow<List<CategoryEntity>>(emptyList())
    val categories: StateFlow<List<CategoryEntity>> = _categories.asStateFlow()

    private val _currencyCode = MutableStateFlow("USD")
    val currencyCode: StateFlow<String> = _currencyCode.asStateFlow()

    private val _confirmHapticEvents = MutableSharedFlow<Unit>(extraBufferCapacity = 1)
    val confirmHapticEvents: SharedFlow<Unit> = _confirmHapticEvents.asSharedFlow()

    private val _submitResultEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val submitResultEvents: SharedFlow<Boolean> = _submitResultEvents.asSharedFlow()

    private var activeParser: ExpenseParser = expenseParser

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
        }
        viewModelScope.launch {
            categoryRepository.observeActive().collect { activeCategories ->
                _categories.value = activeCategories
                recomputePreview(text.value)
            }
        }
        viewModelScope.launch {
            settingsRepository.currencyCode.collect { code ->
                _currencyCode.value = code
                recomputePreview(text.value)
            }
        }
        viewModelScope.launch {
            learnedMappingRepository.observeAllAsMap().collect { map ->
                activeParser = ExpenseParser(
                    lexicon = expenseParser.lexicon,
                    learned = { note -> map[note] ?: expenseParser.learned(note) },
                )
                recomputePreview(text.value)
            }
        }
        if (text.value.isNotBlank()) {
            recomputePreview(text.value)
        }
    }

    fun onTextChange(newText: String) {
        savedStateHandle[KEY_TYPED_TEXT] = newText
        if (newText.isBlank()) {
            savedStateHandle[KEY_SELECTED_CATEGORY_OVERRIDE] = null
        }
        recomputePreview(newText)
    }

    fun onCategoryPicked(categoryId: String) {
        savedStateHandle[KEY_SELECTED_CATEGORY_OVERRIDE] = categoryId
        recomputePreview(text.value)
    }

    fun submit(): Boolean {
        if (_isSubmitting.value) return false
        val currentPreview = _preview.value
        if (currentPreview !is EntryPreview.Valid) return false

        val raw = text.value
        val amountMinor = currentPreview.amountMinor
        val note = currentPreview.note
        val categoryId = currentPreview.categoryId
        val override = selectedCategoryOverride.value
        val currency = _currencyCode.value
        val now = clock.millis()
        val localDate = todayObserver.currentLocalDate().toString()

        val entity = ExpenseEntity(
            id = UUID.randomUUID().toString(),
            amountMinor = amountMinor,
            currency = currency,
            note = note,
            rawText = raw,
            categoryId = categoryId,
            createdAt = now,
            localDate = localDate,
            updatedAt = now,
            deletedAt = null,
        )

        _isSubmitting.value = true
        viewModelScope.launch {
            try {
                expenseRepository.add(entity)
                savedStateHandle[KEY_TYPED_TEXT] = ""
                savedStateHandle[KEY_SELECTED_CATEGORY_OVERRIDE] = null
                _preview.value = EntryPreview.Empty
                if (override != null && note.isNotBlank()) {
                    runCatching { learnedMappingRepository.recordMapping(note, override) }
                }
                _confirmHapticEvents.tryEmit(Unit)
                _submitResultEvents.tryEmit(true)
            } catch (_: Exception) {
                _submitResultEvents.tryEmit(false)
            } finally {
                _isSubmitting.value = false
            }
        }
        return true
    }

    private fun recomputePreview(input: String) {
        val trimmed = input.trim()
        if (trimmed.isEmpty()) {
            _preview.value = EntryPreview.Empty
            return
        }

        val digits = fractionDigits(_currencyCode.value)
        val result = activeParser.parse(trimmed, digits)
        when (result) {
            is ParseResult.NoAmount -> {
                _preview.value = EntryPreview.NeedsAmount()
            }
            is ParseResult.Ok -> {
                val parsed = result.value
                val categoryId = selectedCategoryOverride.value ?: parsed.categoryId
                val categoryEntity = _categories.value.firstOrNull { it.id == categoryId }
                val categoryName = categoryEntity?.nameOverride ?: defaultCategoryName(categoryId)
                val formattedAmount = formatMinor(parsed.amountMinor, _currencyCode.value)

                _preview.value = EntryPreview.Valid(
                    amountMinor = parsed.amountMinor,
                    formattedAmount = formattedAmount,
                    note = parsed.note,
                    categoryId = categoryId,
                    categoryName = categoryName,
                    confidence = parsed.confidence,
                    categoryNameRes = categoryEntity?.defaultNameRes() ?: CategoryEntity.defaultNameRes(categoryId),
                )
            }
        }
    }

    private fun defaultCategoryName(categoryId: String): String {
        return when (categoryId) {
            CategoryEntity.CATEGORY_FOOD -> "Food & Drink"
            CategoryEntity.CATEGORY_GROCERIES -> "Groceries"
            CategoryEntity.CATEGORY_TRANSPORT -> "Transport"
            CategoryEntity.CATEGORY_SHOPPING -> "Shopping"
            CategoryEntity.CATEGORY_HOME -> "Home & Bills"
            CategoryEntity.CATEGORY_HEALTH -> "Health"
            CategoryEntity.CATEGORY_FUN -> "Fun"
            CategoryEntity.CATEGORY_TRAVEL -> "Travel"
            CategoryEntity.CATEGORY_OTHER -> "Other"
            else -> categoryId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(Locale.ROOT) else it.toString() }
        }
    }
}
