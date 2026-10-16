package com.yourco.spend.feature.insight

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.yourco.spend.R
import com.yourco.spend.core.money.currencySymbol
import com.yourco.spend.core.money.formatAmountDigits
import com.yourco.spend.core.money.formatMinor
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.SettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import java.time.Clock
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.TextStyle
import java.util.Locale
import javax.inject.Inject

@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class InsightViewModel @Inject constructor(
    private val expenseRepository: ExpenseRepository,
    private val categoryRepository: CategoryRepository,
    private val settingsRepository: SettingsRepository,
    private val clock: Clock,
    private val savedStateHandle: SavedStateHandle = SavedStateHandle(),
) : ViewModel() {

    companion object {
        const val KEY_MODE = "insight_mode"
        const val KEY_ANCHOR = "insight_anchor"
    }

    private val today: LocalDate
        get() = LocalDate.now(clock)

    private val initialMode: InsightMode = savedStateHandle.get<String>(KEY_MODE)?.let {
        runCatching { InsightMode.valueOf(it) }.getOrNull()
    } ?: InsightMode.MONTH

    private val initialAnchor: LocalDate = savedStateHandle.get<String>(KEY_ANCHOR)?.let {
        runCatching { LocalDate.parse(it) }.getOrNull()
    } ?: today

    private val _mode = MutableStateFlow(initialMode)
    private val _anchorDate = MutableStateFlow(initialAnchor)

    private data class QueryKey(val mode: InsightMode, val anchor: LocalDate)

    val state: StateFlow<InsightUiState> = combine(_mode, _anchorDate) { mode, anchor ->
        QueryKey(mode, anchor)
    }.flatMapLatest { (mode, anchor) ->
        val bounds = periodBounds(mode, anchor)
        val from = bounds.startIso
        val to = bounds.endIso
        val bucketsFlow = when (mode) {
            InsightMode.YEAR -> expenseRepository.observeBuckets(from, to, len = 7)
            InsightMode.MONTH -> expenseRepository.observeBuckets(from, to, len = 10)
            InsightMode.DAY -> flowOf(emptyList())
        }

        combine(
            expenseRepository.observeTotal(from, to),
            expenseRepository.observeByCategory(from, to),
            bucketsFlow,
            settingsRepository.currencyCode,
            categoryRepository.observeAll(),
        ) { totalMinor, categoryTotals, buckets, currencyCode, categoriesList ->
            val locale = Locale.getDefault()
            val todayDate = today
            val categoryEntityMap = categoriesList.associateBy { it.id }

            val categoryUiModels = categoryTotals.map { catTotal ->
                val entity = categoryEntityMap[catTotal.categoryId]
                val nameOverride = entity?.nameOverride
                val defaultRes = CategoryEntity.defaultNameRes(catTotal.categoryId)
                val name = when {
                    !nameOverride.isNullOrBlank() -> nameOverride
                    catTotal.categoryId == CategoryEntity.CATEGORY_FOOD -> "Food & Drink"
                    catTotal.categoryId == CategoryEntity.CATEGORY_GROCERIES -> "Groceries"
                    catTotal.categoryId == CategoryEntity.CATEGORY_TRANSPORT -> "Transport"
                    catTotal.categoryId == CategoryEntity.CATEGORY_SHOPPING -> "Shopping"
                    catTotal.categoryId == CategoryEntity.CATEGORY_HOME -> "Home & Bills"
                    catTotal.categoryId == CategoryEntity.CATEGORY_HEALTH -> "Health"
                    catTotal.categoryId == CategoryEntity.CATEGORY_FUN -> "Fun"
                    catTotal.categoryId == CategoryEntity.CATEGORY_TRAVEL -> "Travel"
                    catTotal.categoryId == CategoryEntity.CATEGORY_OTHER -> "Other"
                    else -> catTotal.categoryId.replaceFirstChar { if (it.isLowerCase()) it.titlecase(locale) else it.toString() }
                }
                val fraction = if (totalMinor > 0L) {
                    (catTotal.total.toDouble() / totalMinor.toDouble()).toFloat().coerceIn(0f, 1f)
                } else {
                    0f
                }
                val percentage = if (totalMinor > 0L) {
                    kotlin.math.round((catTotal.total.toDouble() / totalMinor.toDouble()) * 100).toInt().coerceIn(0, 100)
                } else {
                    0
                }
                InsightCategoryUiModel(
                    categoryId = catTotal.categoryId,
                    name = name,
                    nameRes = if (nameOverride.isNullOrBlank()) defaultRes else null,
                    totalMinor = catTotal.total,
                    formattedAmount = formatMinor(catTotal.total, currencyCode, locale),
                    percentage = percentage,
                    fraction = fraction,
                )
            }

            val yearBars = if (mode == InsightMode.YEAR) {
                val year = anchor.year
                val maxMonthlyTotal = buckets.maxOfOrNull { it.total } ?: 0L
                (1..12).map { monthIndex ->
                    val bucketKey = String.format(Locale.US, "%04d-%02d", year, monthIndex)
                    val monthTotal = buckets.firstOrNull { it.bucket == bucketKey }?.total ?: 0L
                    val fraction = if (maxMonthlyTotal > 0L) {
                        (monthTotal.toDouble() / maxMonthlyTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val isCurrentMonth = (year == todayDate.year && monthIndex == todayDate.monthValue)
                    val monthLabel = java.time.Month.of(monthIndex).getDisplayName(TextStyle.NARROW, locale)
                    YearBarUiModel(
                        monthIndex = monthIndex,
                        label = monthLabel,
                        totalMinor = monthTotal,
                        fraction = fraction,
                        isCurrentMonth = isCurrentMonth,
                    )
                }
            } else {
                emptyList()
            }

            val monthPoints = if (mode == InsightMode.MONTH) {
                val ym = YearMonth.from(anchor)
                val daysInMonth = ym.lengthOfMonth()
                val maxDailyTotal = buckets.maxOfOrNull { it.total } ?: 0L
                (1..daysInMonth).map { day ->
                    val bucketKey = String.format(Locale.US, "%04d-%02d-%02d", ym.year, ym.monthValue, day)
                    val dayTotal = buckets.firstOrNull { it.bucket == bucketKey }?.total ?: 0L
                    val fraction = if (maxDailyTotal > 0L) {
                        (dayTotal.toDouble() / maxDailyTotal.toDouble()).toFloat().coerceIn(0f, 1f)
                    } else {
                        0f
                    }
                    val isCurrentDay = (ym.year == todayDate.year && ym.monthValue == todayDate.monthValue && day == todayDate.dayOfMonth)
                    MonthPointUiModel(
                        dayOfMonth = day,
                        totalMinor = dayTotal,
                        fraction = fraction,
                        isCurrentDay = isCurrentDay,
                    )
                }
            } else {
                emptyList()
            }

            val canGoForward = canGoForward(anchor, mode, todayDate)
            val periodLabel = formatPeriodLabel(mode, anchor, todayDate, locale)
            val periodLabelRes = when {
                mode == InsightMode.DAY && anchor.isEqual(todayDate) -> R.string.insight_today
                mode == InsightMode.DAY && anchor.isEqual(todayDate.minusDays(1)) -> R.string.insight_yesterday
                else -> null
            }

            InsightUiState(
                mode = mode,
                anchorDate = anchor,
                periodLabel = periodLabel,
                periodLabelRes = periodLabelRes,
                totalMinor = totalMinor,
                formattedTotal = formatMinor(totalMinor, currencyCode, locale),
                currencyCode = currencyCode,
                canGoForward = canGoForward,
                isEmpty = totalMinor == 0L && categoryTotals.isEmpty(),
                categories = categoryUiModels,
                yearBars = yearBars,
                monthPoints = monthPoints,
                currencySymbol = currencySymbol(currencyCode, locale),
                amountDigits = formatAmountDigits(totalMinor, currencyCode, locale),
            )
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = initialUiState(),
    )

    private fun initialUiState(): InsightUiState {
        val locale = Locale.getDefault()
        val todayDate = today
        val ym = YearMonth.from(initialAnchor)
        val daysInMonth = ym.lengthOfMonth()
        val initialPoints = if (initialMode == InsightMode.MONTH) {
            (1..daysInMonth).map { day ->
                MonthPointUiModel(
                    dayOfMonth = day,
                    totalMinor = 0L,
                    fraction = 0f,
                    isCurrentDay = (ym.year == todayDate.year && ym.monthValue == todayDate.monthValue && day == todayDate.dayOfMonth),
                )
            }
        } else {
            emptyList()
        }

        val initialBars = if (initialMode == InsightMode.YEAR) {
            (1..12).map { monthIndex ->
                YearBarUiModel(
                    monthIndex = monthIndex,
                    label = java.time.Month.of(monthIndex).getDisplayName(TextStyle.NARROW, locale),
                    totalMinor = 0L,
                    fraction = 0f,
                    isCurrentMonth = (initialAnchor.year == todayDate.year && monthIndex == todayDate.monthValue),
                )
            }
        } else {
            emptyList()
        }

        val initialPeriodLabelRes = when {
            initialMode == InsightMode.DAY && initialAnchor.isEqual(todayDate) -> R.string.insight_today
            initialMode == InsightMode.DAY && initialAnchor.isEqual(todayDate.minusDays(1)) -> R.string.insight_yesterday
            else -> null
        }

        return InsightUiState(
            mode = initialMode,
            anchorDate = initialAnchor,
            periodLabel = formatPeriodLabel(initialMode, initialAnchor, todayDate, locale),
            periodLabelRes = initialPeriodLabelRes,
            totalMinor = 0L,
            formattedTotal = formatMinor(0L, "USD", locale),
            currencyCode = "USD",
            canGoForward = canGoForward(initialAnchor, initialMode, todayDate),
            isEmpty = true,
            categories = emptyList(),
            yearBars = initialBars,
            monthPoints = initialPoints,
            currencySymbol = currencySymbol("USD", locale),
            amountDigits = formatAmountDigits(0L, "USD", locale),
        )
    }

    fun setMode(mode: InsightMode) {
        _mode.value = mode
        savedStateHandle[KEY_MODE] = mode.name
    }

    fun setAnchorDate(anchor: LocalDate) {
        val todayDate = today
        val safeAnchor = if (anchor.isAfter(todayDate)) todayDate else anchor
        _anchorDate.value = safeAnchor
        savedStateHandle[KEY_ANCHOR] = safeAnchor.toString()
    }

    fun previousPeriod() {
        val currentAnchor = _anchorDate.value
        val newAnchor = previous(currentAnchor, _mode.value)
        _anchorDate.value = newAnchor
        savedStateHandle[KEY_ANCHOR] = newAnchor.toString()
    }

    fun nextPeriod() {
        val todayDate = today
        val currentAnchor = _anchorDate.value
        val currentMode = _mode.value
        if (canGoForward(currentAnchor, currentMode, todayDate)) {
            val newAnchor = next(currentAnchor, currentMode, todayDate)
            _anchorDate.value = newAnchor
            savedStateHandle[KEY_ANCHOR] = newAnchor.toString()
        }
    }
}
