package com.nexusbench.nexpense.feature.insight

import androidx.compose.runtime.Immutable
import java.time.LocalDate

/**
 * Category breakdown item in Insight screen.
 * [fraction] is clamped to 0f..1f with safe divide-by-zero check.
 * [percentage] is whole number 0..100.
 */
@Immutable
data class InsightCategoryUiModel(
    val categoryId: String,
    val name: String,
    val nameRes: Int? = null,
    val totalMinor: Long,
    val formattedAmount: String,
    val percentage: Int,
    val fraction: Float,
)

/**
 * Monthly bar item for 12-bar chart in Year mode.
 */
@Immutable
data class YearBarUiModel(
    val monthIndex: Int,
    val label: String,
    val totalMinor: Long,
    val fraction: Float,
    val isCurrentMonth: Boolean,
)

/**
 * Daily point/mini-bar item for sparkline in Month mode.
 */
@Immutable
data class MonthPointUiModel(
    val dayOfMonth: Int,
    val totalMinor: Long,
    val fraction: Float,
    val isCurrentDay: Boolean,
)

/**
 * Complete immutable UI state for the Insight screen.
 */
@Immutable
data class InsightUiState(
    val mode: InsightMode = InsightMode.MONTH,
    val anchorDate: LocalDate,
    val periodLabel: String = "",
    val periodLabelRes: Int? = null,
    val totalMinor: Long = 0L,
    val formattedTotal: String = "",
    val currencyCode: String = "USD",
    val canGoForward: Boolean = false,
    val isEmpty: Boolean = true,
    val categories: List<InsightCategoryUiModel> = emptyList(),
    val yearBars: List<YearBarUiModel> = emptyList(),
    val monthPoints: List<MonthPointUiModel> = emptyList(),
    val currencySymbol: String = "$",
    val amountDigits: String = "0.00",
)
