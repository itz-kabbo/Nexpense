package com.yourco.spend.feature.insight

import java.time.LocalDate
import java.time.Year
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Insight period mode: DAY, MONTH, YEAR.
 */
enum class InsightMode {
    DAY,
    MONTH,
    YEAR,
}

/**
 * Closed bounds for an insight period.
 * ISO strings match Room query format: local_date BETWEEN :from AND :to.
 */
data class PeriodBounds(
    val startDate: LocalDate,
    val endDate: LocalDate,
) {
    val startIso: String get() = startDate.toString()
    val endIso: String get() = endDate.toString()
}

/**
 * Returns the date bounds for the given mode and anchor date.
 * - DAY: [anchor, anchor]
 * - MONTH: YearMonth.atDay(1) to atEndOfMonth()
 * - YEAR: Year.atDay(1) to atDay(Year.length())
 */
fun periodBounds(mode: InsightMode, anchor: LocalDate): PeriodBounds = when (mode) {
    InsightMode.DAY -> PeriodBounds(anchor, anchor)
    InsightMode.MONTH -> {
        val ym = YearMonth.from(anchor)
        PeriodBounds(ym.atDay(1), ym.atEndOfMonth())
    }
    InsightMode.YEAR -> {
        val y = Year.from(anchor)
        PeriodBounds(y.atDay(1), y.atDay(y.length()))
    }
}

/**
 * Stepper function to advance backward by one period.
 */
fun previous(anchor: LocalDate, mode: InsightMode): LocalDate = when (mode) {
    InsightMode.DAY -> anchor.minusDays(1)
    InsightMode.MONTH -> anchor.minusMonths(1)
    InsightMode.YEAR -> anchor.minusYears(1)
}

/**
 * Returns true if the user can navigate forward without exceeding today's period.
 */
fun canGoForward(anchor: LocalDate, mode: InsightMode, today: LocalDate): Boolean = when (mode) {
    InsightMode.DAY -> anchor.isBefore(today)
    InsightMode.MONTH -> YearMonth.from(anchor).isBefore(YearMonth.from(today))
    InsightMode.YEAR -> Year.from(anchor).isBefore(Year.from(today))
}

/**
 * Stepper function to advance forward by one period, clamped so it cannot exceed the current period.
 */
fun next(anchor: LocalDate, mode: InsightMode, today: LocalDate): LocalDate {
    if (!canGoForward(anchor, mode, today)) return anchor
    return when (mode) {
        InsightMode.DAY -> if (anchor.plusDays(1).isAfter(today)) today else anchor.plusDays(1)
        InsightMode.MONTH -> {
            val candidate = anchor.plusMonths(1)
            if (YearMonth.from(candidate).isAfter(YearMonth.from(today))) anchor else candidate
        }
        InsightMode.YEAR -> {
            val candidate = anchor.plusYears(1)
            if (Year.from(candidate).isAfter(Year.from(today))) anchor else candidate
        }
    }
}

// VERIFY: Date formatting for period labels in Insight screen
/**
 * Formats a user-facing label for the period.
 * For DAY mode, relative labels like "Today" and "Yesterday" are also exposed via
 * [InsightUiState.periodLabelRes] so UI composables can resolve localized strings safely.
 */
fun formatPeriodLabel(
    mode: InsightMode,
    anchor: LocalDate,
    today: LocalDate = LocalDate.now(),
    locale: Locale = Locale.getDefault(),
): String = when (mode) {
    InsightMode.DAY -> when {
        anchor.isEqual(today) -> if (locale.language == "zh") "今天" else "Today"
        anchor.isEqual(today.minusDays(1)) -> if (locale.language == "zh") "昨天" else "Yesterday"
        locale.language == "zh" -> anchor.format(DateTimeFormatter.ofPattern("yyyy年M月d日", locale))
        else -> anchor.format(DateTimeFormatter.ofPattern("MMM d, yyyy", locale))
    }
    InsightMode.MONTH -> {
        val pattern = if (locale.language == "zh") "yyyy年M月" else "MMMM yyyy"
        YearMonth.from(anchor).format(DateTimeFormatter.ofPattern(pattern, locale))
    }
    InsightMode.YEAR -> {
        val pattern = if (locale.language == "zh") "yyyy年" else "yyyy"
        Year.from(anchor).format(DateTimeFormatter.ofPattern(pattern, locale))
    }
}
