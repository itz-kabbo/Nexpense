package com.yourco.spend.feature.record

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.core.money.currencySymbol
import com.yourco.spend.core.money.formatAmountDigits
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.core.time.TodayObserver
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
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
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
    private val todayObserver: TodayObserver = TodayObserver.from(clock),
) : ViewModel() {

    companion object {
        const val KEY_EDITING_EXPENSE_ID = "editing_expense_id"
    }

    // VERIFY: Today date format pattern (defaults to "EEEE, MMM d, yyyy" in user's locale)
    private fun formatTodayDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale)
        return date.format(formatter)
    }

    val state: StateFlow<RecordUiState> = combine(
        todayObserver.todayFlow,
        settingsRepository.currencyCode,
        ::Pair,
    ).flatMapLatest { (today, currencyCode) ->
        val todayIso = today.toString()
        combine(
            expenseRepository.observeDay(todayIso),
            expenseRepository.observeTotal(todayIso, todayIso, currency = currencyCode),
            expenseRepository.observeDailyCurrencyTotals(todayIso),
        ) { expenses, totalMinor, currencyTotals ->
            val locale = Locale.getDefault()
            val otherCurrencies = currencyTotals.filter { it.currencyCode != currencyCode }
            val otherCurrencyTotals = if (otherCurrencies.isEmpty()) {
                null
            } else {
                otherCurrencies.joinToString(" · ") { total ->
                    "${total.currencyCode} ${formatMinor(total.totalMinor, total.currencyCode, locale)}"
                }
            }
            RecordUiState(
                todayDateLabel = formatTodayDate(today, locale),
                totalMinor = totalMinor,
                formattedTotal = formatMinor(totalMinor, currencyCode, locale),
                currencySymbol = currencySymbol(currencyCode, locale),
                amountString = formatAmountDigits(totalMinor, currencyCode, locale),
                expenses = expenses,
                currencyCode = currencyCode,
                otherCurrencyTotals = otherCurrencyTotals,
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = RecordUiState(
            todayDateLabel = formatTodayDate(todayObserver.currentLocalDate()),
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

    val allCategories: StateFlow<List<CategoryEntity>> = categoryRepository.observeAll()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    private val _editingExpense = MutableStateFlow<ExpenseEntity?>(null)
    val editingExpense: StateFlow<ExpenseEntity?> = _editingExpense.asStateFlow()

    private val _updateResultEvents = MutableSharedFlow<Boolean>(extraBufferCapacity = 1)
    val updateResultEvents = _updateResultEvents.asSharedFlow()

    private var isUpdatingExpense = false

    init {
        viewModelScope.launch {
            categoryRepository.seedDefaultsIfEmpty()
        }
        val restoredEditingId = savedStateHandle.get<String>(KEY_EDITING_EXPENSE_ID)
        if (restoredEditingId != null) {
            viewModelScope.launch {
                val expense = expenseRepository.getById(restoredEditingId)
                if (expense != null) {
                    _editingExpense.value = expense
                } else {
                    savedStateHandle.remove<String>(KEY_EDITING_EXPENSE_ID)
                }
            }
        }
    }

    fun startEditing(expense: ExpenseEntity) {
        _editingExpense.value = expense
        savedStateHandle[KEY_EDITING_EXPENSE_ID] = expense.id
    }

    fun clearEditing() {
        _editingExpense.value = null
        savedStateHandle.remove<String>(KEY_EDITING_EXPENSE_ID)
    }

    fun updateExpense(updated: ExpenseEntity) {
        if (isUpdatingExpense) return
        val original = _editingExpense.value
        val now = clock.millis()
        isUpdatingExpense = true
        viewModelScope.launch {
            try {
                expenseRepository.update(updated.copy(updatedAt = now))
                if (updated.note.isNotBlank() && (original == null || original.categoryId != updated.categoryId)) {
                    runCatching { learnedMappingRepository.recordMapping(updated.note.lowercase(), updated.categoryId) }
                }
                _editingExpense.value = null
                savedStateHandle.remove<String>(KEY_EDITING_EXPENSE_ID)
                _updateResultEvents.tryEmit(true)
            } catch (_: Exception) {
                _updateResultEvents.tryEmit(false)
            } finally {
                isUpdatingExpense = false
            }
        }
    }

    fun deleteExpense(expense: ExpenseEntity) {
        if (_editingExpense.value?.id == expense.id) {
            clearEditing()
        }
        viewModelScope.launch {
            expenseRepository.softDelete(expense.id, clock.millis())
        }
    }

    fun undoDelete(id: String) {
        viewModelScope.launch {
            expenseRepository.restore(id, clock.millis())
        }
    }
}
