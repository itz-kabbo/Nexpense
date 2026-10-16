package com.yourco.spend.feature.receipt

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.yourco.spend.R
import com.yourco.spend.core.designsystem.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.LocalDate

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ReceiptCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun receiptCard_rendersLocalizedCategoryWhenNoteResProvided() {
        val state = ReceiptUiState(
            selectedDate = "2026-10-15",
            date = LocalDate.parse("2026-10-15"),
            formattedDate = "Thursday, Oct 15, 2026",
            items = listOf(
                ReceiptItemUiModel(
                    id = "1",
                    note = "",
                    noteRes = R.string.category_food,
                    amountMinor = 500,
                    formattedAmount = "$5.00",
                ),
                ReceiptItemUiModel(
                    id = "2",
                    note = "Custom Lunch Note",
                    noteRes = null,
                    amountMinor = 1200,
                    formattedAmount = "$12.00",
                ),
            ),
            totalMinor = 1700,
            formattedTotal = "$17.00",
            isEmpty = false,
        )

        composeTestRule.setContent {
            AppTheme {
                ReceiptCard(state = state)
            }
        }

        // Resolves R.string.category_food ("Food & Drink")
        composeTestRule.onNodeWithText("Food & Drink").assertIsDisplayed()
        // Displays custom note
        composeTestRule.onNodeWithText("Custom Lunch Note").assertIsDisplayed()
    }
}
