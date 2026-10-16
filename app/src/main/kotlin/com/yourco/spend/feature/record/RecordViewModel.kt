package com.yourco.spend.feature.record

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.core.money.currencySymbol
import com.yourco.spend.core.money.formatAmountDigits
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.core.time.todayLocalDate
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale
import javax.inject.Inject

@HiltViewModel
class RecordViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
) : ViewModel() {

    // VERIFY: Today date format pattern (defaults to "EEEE, MMM d" in user's locale)
    private fun formatTodayDate(locale: Locale = Locale.getDefault()): String {
        val today = LocalDate.now(clock)
        val formatter = DateTimeFormatter.ofPattern("EEEE, MMM d", locale)
        return today.format(formatter)
    }

    private val todayIso: String = clock.todayLocalDate()

    val state: StateFlow<RecordUiState> = combine(
        expenseRepository.observeDay(todayIso),
        expenseRepository.observeTotal(todayIso, todayIso),
        settingsRepository.currencyCode,
    ) { expenses, totalMinor, currencyCode ->
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
}
