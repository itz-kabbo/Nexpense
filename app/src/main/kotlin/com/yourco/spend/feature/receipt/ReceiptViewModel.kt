package com.yourco.spend.feature.receipt

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class ReceiptViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    companion object {
        const val KEY_SELECTED_DATE = "selected_date"
    }

    private val today: LocalDate
        get() = LocalDate.now(clock)

    private val initialDate: LocalDate = savedStateHandle.get<String>(KEY_SELECTED_DATE)?.let {
        try {
            LocalDate.parse(it)
        } catch (_: Exception) {
            null
        }
    } ?: today

    private val _currentDate = MutableStateFlow(initialDate)

    // VERIFY: Receipt date header format pattern (defaults to "EEEE, MMM d, yyyy" in user's locale)
    private fun formatDate(date: LocalDate, locale: Locale = Locale.getDefault()): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d, yyyy", locale)
        return date.format(formatter)
    }

    val state: StateFlow<ReceiptUiState> = _currentDate
        .flatMapLatest { date ->
            val dateIso = date.toString()
            combine(
                expenseRepository.observeDay(dateIso),
                expenseRepository.observeTotal(dateIso, dateIso),
                settingsRepository.currencyCode,
            ) { expenses, totalFromDb, currencyCode ->
                val locale = Locale.getDefault()
                // VERIFY: Receipt items ordering chronological (oldest first like printed shopping slip)
                val items = expenses.reversed().map { expense ->
                    val categoryDefaultRes = CategoryEntity.defaultNameRes(expense.categoryId)
                    val fallbackNote = if (categoryDefaultRes != null) expense.categoryId else "other"
                    val displayNote = expense.note.ifBlank { fallbackNote }
                    ReceiptItemUiModel(
                        id = expense.id,
                        note = displayNote,
                        categoryId = expense.categoryId,
                        amountMinor = expense.amountMinor,
                        formattedAmount = formatMinor(expense.amountMinor, expense.currency, locale),
                    )
                }
                val currentDateIsBeforeToday = date.isBefore(today)
                ReceiptUiState(
                    selectedDate = dateIso,
                    date = date,
                    formattedDate = formatDate(date, locale),
                    items = items,
                    totalMinor = totalFromDb,
                    formattedTotal = formatMinor(totalFromDb, currencyCode, locale),
                    currencyCode = currencyCode,
                    isEmpty = items.isEmpty(),
                    canGoForward = currentDateIsBeforeToday,
                )
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = ReceiptUiState(
                selectedDate = initialDate.toString(),
                date = initialDate,
                formattedDate = formatDate(initialDate),
                isEmpty = true,
                canGoForward = initialDate.isBefore(today),
            ),
        )

    fun previousDay() {
        _currentDate.update { it.minusDays(1) }
        savedStateHandle[KEY_SELECTED_DATE] = _currentDate.value.toString()
    }

    fun nextDay() {
        val nowToday = today
        _currentDate.update { current ->
            if (current.isBefore(nowToday)) current.plusDays(1) else current
        }
        savedStateHandle[KEY_SELECTED_DATE] = _currentDate.value.toString()
    }

    fun setDate(date: LocalDate) {
        val nowToday = today
        _currentDate.value = if (date.isAfter(nowToday)) nowToday else date
        savedStateHandle[KEY_SELECTED_DATE] = _currentDate.value.toString()
    }

    fun setDate(dateIso: String) {
        try {
            setDate(LocalDate.parse(dateIso))
        } catch (_: Exception) {
            // Keep current date if invalid string
        }
    }
}
