package com.nexusbench.nexpense.feature.insight

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDate
import java.util.Locale

class InsightPeriodTest {

    @Test
    fun periodBounds_dayMode() {
        val leapDay = LocalDate.of(2024, 2, 29)
        val boundsLeap = periodBounds(InsightMode.DAY, leapDay)
        assertEquals("2024-02-29", boundsLeap.startIso)
        assertEquals("2024-02-29", boundsLeap.endIso)

        val dec31 = LocalDate.of(2023, 12, 31)
        val boundsDec31 = periodBounds(InsightMode.DAY, dec31)
        assertEquals("2023-12-31", boundsDec31.startIso)
        assertEquals("2023-12-31", boundsDec31.endIso)

        val jan1 = LocalDate.of(2024, 1, 1)
        val boundsJan1 = periodBounds(InsightMode.DAY, jan1)
        assertEquals("2024-01-01", boundsJan1.startIso)
        assertEquals("2024-01-01", boundsJan1.endIso)
    }

    @Test
    fun periodBounds_monthMode_feb29LeapYearVsFeb28NonLeapYear() {
        // Leap year 2024 February: 29 days
        val feb2024 = LocalDate.of(2024, 2, 15)
        val boundsFeb2024 = periodBounds(InsightMode.MONTH, feb2024)
        assertEquals("2024-02-01", boundsFeb2024.startIso)
        assertEquals("2024-02-29", boundsFeb2024.endIso)

        // Non-leap year 2023 February: 28 days
        val feb2023 = LocalDate.of(2023, 2, 10)
        val boundsFeb2023 = periodBounds(InsightMode.MONTH, feb2023)
        assertEquals("2023-02-01", boundsFeb2023.startIso)
        assertEquals("2023-02-28", boundsFeb2023.endIso)
    }

    @Test
    fun periodBounds_monthMode_30DayMonths() {
        val april = LocalDate.of(2024, 4, 15)
        val boundsApril = periodBounds(InsightMode.MONTH, april)
        assertEquals("2024-04-01", boundsApril.startIso)
        assertEquals("2024-04-30", boundsApril.endIso)

        val june = LocalDate.of(2024, 6, 20)
        val boundsJune = periodBounds(InsightMode.MONTH, june)
        assertEquals("2024-06-01", boundsJune.startIso)
        assertEquals("2024-06-30", boundsJune.endIso)

        val sept = LocalDate.of(2024, 9, 5)
        val boundsSept = periodBounds(InsightMode.MONTH, sept)
        assertEquals("2024-09-01", boundsSept.startIso)
        assertEquals("2024-09-30", boundsSept.endIso)

        val nov = LocalDate.of(2024, 11, 28)
        val boundsNov = periodBounds(InsightMode.MONTH, nov)
        assertEquals("2024-11-01", boundsNov.startIso)
        assertEquals("2024-11-30", boundsNov.endIso)
    }

    @Test
    fun periodBounds_monthMode_31DayMonths() {
        val months31 = listOf(1, 3, 5, 7, 8, 10, 12)
        for (m in months31) {
            val date = LocalDate.of(2024, m, 15)
            val bounds = periodBounds(InsightMode.MONTH, date)
            val expectedStart = String.format(Locale.US, "2024-%02d-01", m)
            val expectedEnd = String.format(Locale.US, "2024-%02d-31", m)
            assertEquals(expectedStart, bounds.startIso)
            assertEquals(expectedEnd, bounds.endIso)
        }
    }

    @Test
    fun periodBounds_yearMode_leapYearVsNonLeapYear() {
        val leapYearDate = LocalDate.of(2024, 7, 10)
        val boundsLeap = periodBounds(InsightMode.YEAR, leapYearDate)
        assertEquals("2024-01-01", boundsLeap.startIso)
        assertEquals("2024-12-31", boundsLeap.endIso)

        val nonLeapYearDate = LocalDate.of(2023, 3, 10)
        val boundsNonLeap = periodBounds(InsightMode.YEAR, nonLeapYearDate)
        assertEquals("2023-01-01", boundsNonLeap.startIso)
        assertEquals("2023-12-31", boundsNonLeap.endIso)
    }

    @Test
    fun previous_dayMode_crossesYearBoundaryDec31AndJan1() {
        val jan1 = LocalDate.of(2024, 1, 1)
        val prev = previous(jan1, InsightMode.DAY)
        assertEquals(LocalDate.of(2023, 12, 31), prev)
    }

    @Test
    fun previous_dayMode_leapYearFeb29() {
        val mar1 = LocalDate.of(2024, 3, 1)
        val prev = previous(mar1, InsightMode.DAY)
        assertEquals(LocalDate.of(2024, 2, 29), prev)
    }

    @Test
    fun previous_dayMode_nonLeapYearFeb28() {
        val mar1 = LocalDate.of(2023, 3, 1)
        val prev = previous(mar1, InsightMode.DAY)
        assertEquals(LocalDate.of(2023, 2, 28), prev)
    }

    @Test
    fun previous_monthMode_crossesYearBoundary() {
        val jan2024 = LocalDate.of(2024, 1, 15)
        val prev = previous(jan2024, InsightMode.MONTH)
        assertEquals(LocalDate.of(2023, 12, 15), prev)
    }

    @Test
    fun previous_monthMode_fromMarch31ToFebruary() {
        // In leap year 2024: Mar 31 - 1 month = Feb 29
        val mar31Leap = LocalDate.of(2024, 3, 31)
        val prevLeap = previous(mar31Leap, InsightMode.MONTH)
        assertEquals(LocalDate.of(2024, 2, 29), prevLeap)

        // In non-leap year 2023: Mar 31 - 1 month = Feb 28
        val mar31NonLeap = LocalDate.of(2023, 3, 31)
        val prevNonLeap = previous(mar31NonLeap, InsightMode.MONTH)
        assertEquals(LocalDate.of(2023, 2, 28), prevNonLeap)
    }

    @Test
    fun previous_yearMode() {
        val leap2024 = LocalDate.of(2024, 2, 29)
        val prev = previous(leap2024, InsightMode.YEAR)
        assertEquals(LocalDate.of(2023, 2, 28), prev)
    }

    @Test
    fun next_dayMode_crossesYearBoundaryDec31AndJan1() {
        val dec31 = LocalDate.of(2023, 12, 31)
        val today = LocalDate.of(2024, 1, 10)
        val nextDate = next(dec31, InsightMode.DAY, today)
        assertEquals(LocalDate.of(2024, 1, 1), nextDate)
    }

    @Test
    fun next_dayMode_leapYearFeb28ToFeb29() {
        val feb28 = LocalDate.of(2024, 2, 28)
        val today = LocalDate.of(2024, 3, 1)
        val nextDate = next(feb28, InsightMode.DAY, today)
        assertEquals(LocalDate.of(2024, 2, 29), nextDate)
    }

    @Test
    fun next_monthMode_crossesYearBoundary() {
        val dec2023 = LocalDate.of(2023, 12, 15)
        val today = LocalDate.of(2024, 5, 1)
        val nextDate = next(dec2023, InsightMode.MONTH, today)
        assertEquals(LocalDate.of(2024, 1, 15), nextDate)
    }

    @Test
    fun next_clamping_cannotExceedCurrentPeriod() {
        val today = LocalDate.of(2024, 10, 16)

        // DAY: on today, next returns today
        val nextDay = next(today, InsightMode.DAY, today)
        assertEquals(today, nextDay)
        assertFalse(canGoForward(today, InsightMode.DAY, today))

        // MONTH: in October 2024, next returns anchor
        val midOct = LocalDate.of(2024, 10, 5)
        val nextMonth = next(midOct, InsightMode.MONTH, today)
        assertEquals(midOct, nextMonth)
        assertFalse(canGoForward(midOct, InsightMode.MONTH, today))

        // MONTH: September 2024 can advance to October
        val sept = LocalDate.of(2024, 9, 5)
        assertTrue(canGoForward(sept, InsightMode.MONTH, today))
        val advancedMonth = next(sept, InsightMode.MONTH, today)
        assertEquals(LocalDate.of(2024, 10, 5), advancedMonth)

        // YEAR: in 2024, next returns anchor
        val midYear = LocalDate.of(2024, 3, 1)
        val nextYear = next(midYear, InsightMode.YEAR, today)
        assertEquals(midYear, nextYear)
        assertFalse(canGoForward(midYear, InsightMode.YEAR, today))

        // YEAR: 2023 can advance to 2024
        val year2023 = LocalDate.of(2023, 3, 1)
        assertTrue(canGoForward(year2023, InsightMode.YEAR, today))
        val advancedYear = next(year2023, InsightMode.YEAR, today)
        assertEquals(LocalDate.of(2024, 3, 1), advancedYear)
    }

    @Test
    fun formatPeriodLabel_localesAndModes() {
        val today = LocalDate.of(2026, 10, 16)

        // DAY: English
        assertEquals("Today", formatPeriodLabel(InsightMode.DAY, today, today, Locale.US))
        assertEquals("Yesterday", formatPeriodLabel(InsightMode.DAY, today.minusDays(1), today, Locale.US))
        assertEquals("Oct 14, 2026", formatPeriodLabel(InsightMode.DAY, today.minusDays(2), today, Locale.US))

        // DAY: Traditional Chinese
        assertEquals("今天", formatPeriodLabel(InsightMode.DAY, today, today, Locale.TRADITIONAL_CHINESE))
        assertEquals("昨天", formatPeriodLabel(InsightMode.DAY, today.minusDays(1), today, Locale.TRADITIONAL_CHINESE))

        // MONTH
        val monthAnchor = LocalDate.of(2026, 10, 1)
        assertEquals("October 2026", formatPeriodLabel(InsightMode.MONTH, monthAnchor, today, Locale.US))
        assertEquals("2026年10月", formatPeriodLabel(InsightMode.MONTH, monthAnchor, today, Locale.TRADITIONAL_CHINESE))

        // YEAR
        val yearAnchor = LocalDate.of(2026, 1, 1)
        assertEquals("2026", formatPeriodLabel(InsightMode.YEAR, yearAnchor, today, Locale.US))
        assertEquals("2026年", formatPeriodLabel(InsightMode.YEAR, yearAnchor, today, Locale.TRADITIONAL_CHINESE))
    }
}
