package com.yourco.spend.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.core.money.currencySymbol
import com.yourco.spend.core.money.formatAmountDigits
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.core.time.todayLocalDate
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class RecordViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val categoryRepository: CategoryRepository,
    private val learnedMappingRepository: LearnedMappingRepository,
    private val clock: Clock,
) : ViewModel() {

    // VERIFY: Today date format pattern (defaults to "EEEE, MMM d" in user's locale)
    private fun formatTodayDate(locale: Locale = Locale.getDefault()): String {
        val today = LocalDate.now(clock)
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", locale)
        return today.format(formatter)
    }

    private val todayIso: String = clock.todayLocalDate()

    val state: StateFlow<RecordUiState> = settingsRepository.currencyCode
        .flatMapLatest { currencyCode ->
            combine(
                expenseRepository.observeDay(todayIso),
                expenseRepository.observeTotal(todayIso, todayIso, currency = currencyCode),
            ) { expenses, totalMinor ->
                val locale = Locale.getDefault()
                RecordUiState(
                    todayDateLabel = formatTodayDate(locale),
                    totalMinor = totalMinor,
                    formattedTotal = formatMinor(totalMinor, currencyCode, locale),
                    currencySymbol = currencySymbol(currencyCode, locale),
                    amountString = formatAmountDigits(totalMinor, currencyCode, locale),
                    expenses = expenses,
                    currencyCode = currencyCode,
                )
            }
        }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordUiState(
            todayDateLabel = formatTodayDate(),
            currencyCode = "USD",
        ),
    )

    val uiState: StateFlow<RecordUiState>
        get() = state

    val categories: StateFlow<List<CategoryEntity>> = categoryRepository.observeActive()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _editingExpense = MutableStateFlow<ExpenseEntity?>(null)
    val editingExpense: StateFlow<ExpenseEntity?> = _editingExpense.asStateFlow()

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
        }
    }

    fun startEditing(expense: ExpenseEntity) {
        _editingExpense.value = expense
    }

    fun clearEditing() {
        _editingExpense.value = null
    }

    fun updateExpense(updated: ExpenseEntity) {
        val original = _editingExpense.value
        val now = clock.millis()
        viewModelScope.launch {
            expenseRepository.update(updated.copy(updatedAt = now))
            if (updated.note.isNotBlank() && (original == null || original.categoryId != updated.categoryId)) {
                learnedMappingRepository.recordMapping(updated.note.lowercase(), updated.categoryId)
            }
        }
        _editingExpense.value = null
    }

    fun deleteExpense(expense: ExpenseEntity) {
        viewModelScope.launch {
            expenseRepository.softDelete(expense.id, clock.millis())
        }
    }

    fun undoDelete(id: String) {
        viewModelScope.launch {
            expenseRepository.restore(id)
        }
    }
}
