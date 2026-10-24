package com.nexusbench.nexpense.feature.record

import androidx.compose.runtime.Immutable
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity

@Immutable
data class RecordUiState(
    val todayDateLabel: String = "",
    val totalMinor: Long = 0L,
    val formattedTotal: String = "$0.00",
    val currencySymbol: String = "$",
    val amountString: String = "0.00",
    val expenses: List<ExpenseEntity> = emptyList(),
    val currencyCode: String = "USD",
    val otherCurrencyTotals: String? = null,
) {
    val formattedDate: String get() = todayDateLabel
    val todayExpenses: List<ExpenseEntity> get() = expenses
}
