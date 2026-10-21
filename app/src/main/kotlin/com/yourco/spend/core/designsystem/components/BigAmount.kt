package com.yourco.spend.core.designsystem.components

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.core.money.fractionDigits

/**
 * Splits an amount string (e.g., "74.50", "0.00", "1,200.50", "1500") into integer and decimal parts.
 * When [currencyCode] has 0 fraction digits (e.g., "JPY", "KRW"), the decimal part is omitted.
 */
internal fun splitAmount(amount: String, currencyCode: String? = null): Pair<String, String> {
    if (amount.isEmpty()) return "" to ""

    // If currency has 0 fraction digits, omit decimals completely
    if (currencyCode != null && fractionDigits(currencyCode) == 0) {
        val sepIndex = amount.lastIndexOfAny(charArrayOf('.', ','))
        val intPart = if (sepIndex >= 0) amount.substring(0, sepIndex) else amount
        return intPart to ""
    }

    val sepIndex = amount.lastIndexOfAny(charArrayOf('.', ','))
    return if (sepIndex >= 0 && sepIndex > amount.length - 5) {
        amount.substring(0, sepIndex) to amount.substring(sepIndex)
    } else {
        amount to ""
    }
}

/**
 * Optical Hero Amount display with baseline-aligned components:
 * - 20sp currency symbol (60% alpha)
 * - 52sp light integer digits
 * - 34sp normal decimal digits (dynamically omitted for 0-fraction currencies like JPY)
 */
@Composable
fun BigAmount(
    amount: String,
    currencySymbol: String,
    modifier: Modifier = Modifier,
    currencyCode: String? = null,
    contentDescription: String? = null,
) {
    val (integerPart, decimalPart) = splitAmount(amount, currencyCode)
    val a11yDescription = contentDescription ?: "$currencySymbol$amount"

    Row(
        modifier = modifier.semantics(mergeDescendants = true) {
            this.contentDescription = a11yDescription
        },
        verticalAlignment = Alignment.Bottom,
    ) {
        Text(
            text = currencySymbol,
            style = AppTheme.typography.bigAmountCurrency.copy(
                color = AppTheme.colors.textPrimary.copy(alpha = 0.60f),
            ),
            modifier = Modifier.alignByBaseline(),
        )
        Spacer(modifier = Modifier.width(AppTheme.spacing.xs))
        Text(
            text = integerPart,
            style = AppTheme.typography.bigAmountInteger.copy(
                color = AppTheme.colors.textPrimary,
            ),
            modifier = Modifier.alignByBaseline(),
        )
        if (decimalPart.isNotEmpty()) {
            Text(
                text = decimalPart,
                style = AppTheme.typography.bigAmountDecimals.copy(
                    color = AppTheme.colors.textPrimary,
                ),
                modifier = Modifier.alignByBaseline(),
            )
        }
    }
}
