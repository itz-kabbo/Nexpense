package com.yourco.spend.core.designsystem.components

import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import com.yourco.spend.core.designsystem.AppTheme
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class BigAmountTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun splitAmount_withTwoDecimals_splitsCorrectly() {
        val (intPart1, decPart1) = splitAmount("74.50", "USD")
        assertEquals("74", intPart1)
        assertEquals(".50", decPart1)

        val (intPart2, decPart2) = splitAmount("0.00", "USD")
        assertEquals("0", intPart2)
        assertEquals(".00", decPart2)

        val (intPart3, decPart3) = splitAmount("1,250.75", "USD")
        assertEquals("1,250", intPart3)
        assertEquals(".75", decPart3)

        val (intPart4, decPart4) = splitAmount("1.250,75", "EUR")
        assertEquals("1.250", intPart4)
        assertEquals(",75", decPart4)
    }

    @Test
    fun splitAmount_withZeroFractionDigits_omitsDecimals() {
        val (intPart1, decPart1) = splitAmount("1500", "JPY")
        assertEquals("1500", intPart1)
        assertEquals("", decPart1)

        // Even if formatted string had .00, JPY currencyCode forces decimal omission
        val (intPart2, decPart2) = splitAmount("1500.00", "JPY")
        assertEquals("1500", intPart2)
        assertEquals("", decPart2)

        val (intPart3, decPart3) = splitAmount("0", "JPY")
        assertEquals("0", intPart3)
        assertEquals("", decPart3)
    }

    @Test
    fun splitAmount_withoutCurrencyCode_splitsOnSeparatorIfPresent() {
        val (intPart1, decPart1) = splitAmount("42.50")
        assertEquals("42", intPart1)
        assertEquals(".50", decPart1)

        val (intPart2, decPart2) = splitAmount("100")
        assertEquals("100", intPart2)
        assertEquals("", decPart2)

        val (intPart3, decPart3) = splitAmount("")
        assertEquals("", intPart3)
        assertEquals("", decPart3)
    }

    @Test
    fun bigAmount_rendersCurrencyIntegerAndDecimals_forStandardCurrency() {
        composeTestRule.setContent {
            AppTheme {
                BigAmount(
                    amount = "42.50",
                    currencySymbol = "$",
                    currencyCode = "USD",
                    contentDescription = "$42.50",
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("$42.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("$").assertIsDisplayed()
        composeTestRule.onNodeWithText("42").assertIsDisplayed()
        composeTestRule.onNodeWithText(".50").assertIsDisplayed()
    }

    @Test
    fun bigAmount_omitsDecimals_forZeroFractionCurrency() {
        composeTestRule.setContent {
            AppTheme {
                BigAmount(
                    amount = "1500",
                    currencySymbol = "¥",
                    currencyCode = "JPY",
                    contentDescription = "¥1,500",
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("¥1,500").assertIsDisplayed()
        composeTestRule.onNodeWithText("¥").assertIsDisplayed()
        composeTestRule.onNodeWithText("1500").assertIsDisplayed()
        // Ensure no decimal node exists
        composeTestRule.onNodeWithText(".00").assertDoesNotExist()
    }
}
