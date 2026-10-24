package com.nexusbench.nexpense.core.money

import androidx.annotation.StringRes
import com.nexusbench.nexpense.R
import java.util.Currency
import java.util.Locale

enum class CurrencyRegion(@StringRes val titleRes: Int) {
    AMERICAS(R.string.region_americas),
    EUROPE(R.string.region_europe),
    ASIA_PACIFIC(R.string.region_asia_pacific),
    MIDDLE_EAST(R.string.region_middle_east),
    AFRICA(R.string.region_africa),
}

data class CurrencyItem(
    val code: String,
    val name: String,
    val symbol: String,
    val region: CurrencyRegion,
) {
    val searchKeywords: String = "$code $name $symbol".lowercase(Locale.ROOT)

    fun displayLabel(): String = "$code · $name ($symbol)"
}

object CurrencyCatalog {

    private val ALL_CURRENCIES: List<CurrencyItem> = listOf(
        // Americas
        CurrencyItem("USD", "US Dollar", "$", CurrencyRegion.AMERICAS),
        CurrencyItem("CAD", "Canadian Dollar", "CA$", CurrencyRegion.AMERICAS),
        CurrencyItem("MXN", "Mexican Peso", "MX$", CurrencyRegion.AMERICAS),
        CurrencyItem("BRL", "Brazilian Real", "R$", CurrencyRegion.AMERICAS),
        CurrencyItem("ARS", "Argentine Peso", "AR$", CurrencyRegion.AMERICAS),
        CurrencyItem("CLP", "Chilean Peso", "CL$", CurrencyRegion.AMERICAS),
        CurrencyItem("COP", "Colombian Peso", "COL$", CurrencyRegion.AMERICAS),
        CurrencyItem("PEN", "Peruvian Sol", "S/", CurrencyRegion.AMERICAS),
        CurrencyItem("UYU", "Uruguayan Peso", "\$U", CurrencyRegion.AMERICAS),
        CurrencyItem("CRC", "Costa Rican Colón", "₡", CurrencyRegion.AMERICAS),
        CurrencyItem("DOP", "Dominican Peso", "RD$", CurrencyRegion.AMERICAS),
        CurrencyItem("GTQ", "Guatemalan Quetzal", "Q", CurrencyRegion.AMERICAS),
        CurrencyItem("PAB", "Panamanian Balboa", "B/.", CurrencyRegion.AMERICAS),
        CurrencyItem("JMD", "Jamaican Dollar", "J$", CurrencyRegion.AMERICAS),

        // Europe
        CurrencyItem("EUR", "Euro", "€", CurrencyRegion.EUROPE),
        CurrencyItem("GBP", "British Pound", "£", CurrencyRegion.EUROPE),
        CurrencyItem("CHF", "Swiss Franc", "CHF", CurrencyRegion.EUROPE),
        CurrencyItem("SEK", "Swedish Krona", "kr", CurrencyRegion.EUROPE),
        CurrencyItem("NOK", "Norwegian Krone", "kr", CurrencyRegion.EUROPE),
        CurrencyItem("DKK", "Danish Krone", "kr", CurrencyRegion.EUROPE),
        CurrencyItem("PLN", "Polish Zloty", "zł", CurrencyRegion.EUROPE),
        CurrencyItem("CZK", "Czech Koruna", "Kč", CurrencyRegion.EUROPE),
        CurrencyItem("HUF", "Hungarian Forint", "Ft", CurrencyRegion.EUROPE),
        CurrencyItem("RON", "Romanian Leu", "lei", CurrencyRegion.EUROPE),
        CurrencyItem("BGN", "Bulgarian Lev", "лв", CurrencyRegion.EUROPE),
        CurrencyItem("ISK", "Icelandic Króna", "kr", CurrencyRegion.EUROPE),
        CurrencyItem("RSD", "Serbian Dinar", "din", CurrencyRegion.EUROPE),
        CurrencyItem("BAM", "Bosnia-Herzegovina Mark", "KM", CurrencyRegion.EUROPE),
        CurrencyItem("UAH", "Ukrainian Hryvnia", "₴", CurrencyRegion.EUROPE),

        // Asia-Pacific
        CurrencyItem("JPY", "Japanese Yen", "¥", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("CNY", "Chinese Yuan", "¥", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("TWD", "New Taiwan Dollar", "NT$", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("HKD", "Hong Kong Dollar", "HK$", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("KRW", "South Korean Won", "₩", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("SGD", "Singapore Dollar", "S$", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("INR", "Indian Rupee", "₹", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("AUD", "Australian Dollar", "A$", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("NZD", "New Zealand Dollar", "NZ$", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("IDR", "Indonesian Rupiah", "Rp", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("THB", "Thai Baht", "฿", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("VND", "Vietnamese Dong", "₫", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("MYR", "Malaysian Ringgit", "RM", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("PHP", "Philippine Peso", "₱", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("BDT", "Bangladeshi Taka", "৳", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("PKR", "Pakistani Rupee", "Rs", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("LKR", "Sri Lankan Rupee", "Rs", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("NPR", "Nepalese Rupee", "Rs", CurrencyRegion.ASIA_PACIFIC),
        CurrencyItem("KZT", "Kazakhstani Tenge", "₸", CurrencyRegion.ASIA_PACIFIC),

        // Middle East
        CurrencyItem("AED", "UAE Dirham", "AED", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("SAR", "Saudi Riyal", "SAR", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("ILS", "Israeli New Shekel", "₪", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("QAR", "Qatari Riyal", "QR", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("KWD", "Kuwaiti Dinar", "KD", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("BHD", "Bahraini Dinar", "BD", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("OMR", "Omani Rial", "OMR", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("JOD", "Jordanian Dinar", "JD", CurrencyRegion.MIDDLE_EAST),
        CurrencyItem("TRY", "Turkish Lira", "₺", CurrencyRegion.MIDDLE_EAST),

        // Africa
        CurrencyItem("ZAR", "South African Rand", "R", CurrencyRegion.AFRICA),
        CurrencyItem("EGP", "Egyptian Pound", "E£", CurrencyRegion.AFRICA),
        CurrencyItem("NGN", "Nigerian Naira", "₦", CurrencyRegion.AFRICA),
        CurrencyItem("KES", "Kenyan Shilling", "KSh", CurrencyRegion.AFRICA),
        CurrencyItem("GHS", "Ghanaian Cedi", "GH₵", CurrencyRegion.AFRICA),
        CurrencyItem("MAD", "Moroccan Dirham", "MAD", CurrencyRegion.AFRICA),
        CurrencyItem("ETB", "Ethiopian Birr", "Br", CurrencyRegion.AFRICA),
        CurrencyItem("TZS", "Tanzanian Shilling", "TSh", CurrencyRegion.AFRICA),
        CurrencyItem("UGX", "Ugandan Shilling", "USh", CurrencyRegion.AFRICA),
        CurrencyItem("DZD", "Algerian Dinar", "DA", CurrencyRegion.AFRICA),
    )

    private val CODE_MAP: Map<String, CurrencyItem> = ALL_CURRENCIES.associateBy { it.code }

    fun getAllCurrencies(): List<CurrencyItem> = ALL_CURRENCIES

    fun getCurrenciesByRegion(): Map<CurrencyRegion, List<CurrencyItem>> {
        return ALL_CURRENCIES.groupBy { it.region }
    }

    fun findCurrency(code: String): CurrencyItem? {
        val uppercaseCode = code.uppercase(Locale.ROOT)
        CODE_MAP[uppercaseCode]?.let { return it }

        // Fallback for valid ISO currencies not explicitly in the curated list
        return try {
            val cur = Currency.getInstance(uppercaseCode)
            CurrencyItem(
                code = cur.currencyCode,
                name = cur.getDisplayName(Locale.ENGLISH),
                symbol = cur.getSymbol(Locale.ENGLISH),
                region = CurrencyRegion.AMERICAS,
            )
        } catch (_: Exception) {
            CODE_MAP["USD"]
        }
    }

    fun searchCurrencies(query: String): List<CurrencyItem> {
        val trimmed = query.trim().lowercase(Locale.ROOT)
        if (trimmed.isEmpty()) return ALL_CURRENCIES
        return ALL_CURRENCIES.filter { it.searchKeywords.contains(trimmed) }
    }
}
