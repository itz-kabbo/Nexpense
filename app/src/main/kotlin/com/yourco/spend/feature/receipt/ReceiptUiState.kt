package com.yourco.spend.feature.receipt

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * UI representation of a single expense line on the receipt.
 */
@Immutable
data class ReceiptItemUiModel(
    val id: String,
    val note: String,
    val categoryId: String = "",
    val amountMinor: Long,
    val formattedAmount: String,
)

/**
 * State for the Daily Receipt screen.
 * Money is Long minor units. Never Float or Double.
 */
@Immutable
data class ReceiptUiState(
    val selectedDate: String = "",
    val date: LocalDate = LocalDate.now(),
    val formattedDate: String = "",
    val items: List<ReceiptItemUiModel> = emptyList(),
    val totalMinor: Long = 0L,
    val formattedTotal: String = "",
    val currencyCode: String = "USD",
    val isEmpty: Boolean = true,
    val canGoForward: Boolean = false,
)
