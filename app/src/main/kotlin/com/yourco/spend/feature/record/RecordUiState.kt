package com.yourco.spend.feature.record

import androidx.compose.runtime.Immutable
import com.yourco.spend.data.db.entity.ExpenseEntity

@Immutable
data class RecordUiState(
    val todayDateLabel: String = "",
    val totalMinor: Long = 0L,
    val formattedTotal: String = "$0.00",
    val currencySymbol: String = "$",
    val amountString: String = "0.00",
    val expenses: List<ExpenseEntity> = emptyList(),
    val currencyCode: String = "USD",
)
