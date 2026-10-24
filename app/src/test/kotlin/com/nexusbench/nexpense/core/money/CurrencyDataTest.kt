package com.nexusbench.nexpense.core.money

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CurrencyDataTest {

    @Test
    fun testAllCurrenciesNotEmpty() {
        val all = CurrencyCatalog.getAllCurrencies()
        assertTrue("Should have at least 50 currencies", all.size >= 50)
    }

    @Test
    fun testRegionalGrouping() {
        val grouped = CurrencyCatalog.getCurrenciesByRegion()
        assertEquals(5, grouped.keys.size)
        assertTrue(grouped.containsKey(CurrencyRegion.AMERICAS))
        assertTrue(grouped.containsKey(CurrencyRegion.EUROPE))
        assertTrue(grouped.containsKey(CurrencyRegion.ASIA_PACIFIC))
        assertTrue(grouped.containsKey(CurrencyRegion.MIDDLE_EAST))
        assertTrue(grouped.containsKey(CurrencyRegion.AFRICA))

        grouped.forEach { (region, list) ->
            assertTrue("Region $region should have currencies", list.isNotEmpty())
        }
    }

    @Test
    fun testFindCurrency() {
        val usd = CurrencyCatalog.findCurrency("USD")
        assertNotNull(usd)
        assertEquals("USD", usd?.code)
        assertEquals(CurrencyRegion.AMERICAS, usd?.region)

        val jpy = CurrencyCatalog.findCurrency("JPY")
        assertNotNull(jpy)
        assertEquals("JPY", jpy?.code)
        assertEquals(CurrencyRegion.ASIA_PACIFIC, jpy?.region)

        val unknown = CurrencyCatalog.findCurrency("XYZXYZ")
        assertNotNull(unknown) // fallback to USD or creates safe fallback
    }

    @Test
    fun testSearchCurrencies() {
        val dollarResults = CurrencyCatalog.searchCurrencies("dollar")
        assertTrue(dollarResults.any { it.code == "USD" })
        assertTrue(dollarResults.any { it.code == "CAD" })

        val jpyResult = CurrencyCatalog.searchCurrencies("yen")
        assertTrue(jpyResult.any { it.code == "JPY" })

        val euroCode = CurrencyCatalog.searchCurrencies("EUR")
        assertTrue(euroCode.any { it.code == "EUR" })
    }
}
