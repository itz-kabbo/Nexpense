package com.yourco.spend.core.money

import java.math.BigDecimal
import java.text.NumberFormat
import java.util.Currency
import java.util.Locale

/**
 * Formats a minor unit amount into a localized currency string.
 * Money is Long minor units. Never Float or Double.
 */
fun formatMinor(
    minor: Long,
    currencyCode: String,
    locale: Locale = Locale.getDefault(),
): String {
    val cur = try {
        Currency.getInstance(currencyCode)
    } catch (_: Exception) {
        Currency.getInstance("USD")
    }
    val digits = cur.defaultFractionDigits.coerceAtLeast(0)
    val nf = NumberFormat.getCurrencyInstance(locale)
    nf.currency = cur
    return nf.format(BigDecimal.valueOf(minor, digits))
}

/**
 * Returns the default number of fraction digits for the given currency code.
 * Defaults to 2 if the currency code cannot be resolved.
 */
fun fractionDigits(currencyCode: String): Int {
    return try {
        Currency.getInstance(currencyCode).defaultFractionDigits.coerceAtLeast(0)
    } catch (_: Exception) {
        2
    }
}
