package com.nexusbench.nexpense.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Locale

class FormatMinorTest {

    @Test
    fun formatMinor_usd_twoFractionDigits() {
        val result = formatMinor(1250L, "USD", Locale.US)
        assertEquals("$12.50", result)
    }

    @Test
    fun formatMinor_usd_zeroAmount() {
        val result = formatMinor(0L, "USD", Locale.US)
        assertEquals("$0.00", result)
    }

    @Test
    fun formatMinor_usd_singleCent() {
        val result = formatMinor(1L, "USD", Locale.US)
        assertEquals("$0.01", result)
    }

    @Test
    fun formatMinor_usd_negativeAmount() {
        val result = formatMinor(-1250L, "USD", Locale.US)
        assertEquals("-$12.50", result)
    }

    @Test
    fun formatMinor_jpy_zeroFractionDigits() {
        val result = formatMinor(500L, "JPY", Locale.US)
        // In US locale, JPY is formatted with ¥ symbol or JPY
        assertTrue("Expected JPY format with 500 but was: $result", result.contains("500") && (result.contains("¥") || result.contains("JPY")))
    }

    @Test
    fun formatMinor_largeAmount() {
        val result = formatMinor(10_000_000_00L, "USD", Locale.US)
        assertEquals("$10,000,000.00", result)
    }

    @Test
    fun formatMinor_invalidCurrency_fallsBackToUsd() {
        val result = formatMinor(1250L, "INVALID_CODE", Locale.US)
        assertEquals("$12.50", result)
    }

    @Test
    fun fractionDigits_returnsCorrectDigits() {
        assertEquals(2, fractionDigits("USD"))
        assertEquals(0, fractionDigits("JPY"))
        assertEquals(3, fractionDigits("BHD"))
        assertEquals(2, fractionDigits("UNKNOWN"))
    }
}
