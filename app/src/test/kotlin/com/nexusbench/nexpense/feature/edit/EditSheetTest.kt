package com.nexusbench.nexpense.feature.edit

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.nexusbench.nexpense.core.designsystem.AppTheme
import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EditSheetTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun parseAmountToMinor_variousFormats() {
        // Standard integers and decimals
        assertEquals(600L, parseAmountToMinor("6", "USD"))
        assertEquals(600L, parseAmountToMinor("6.00", "USD"))
        assertEquals(650L, parseAmountToMinor("6.5", "USD"))
        assertEquals(650L, parseAmountToMinor("6.50", "USD"))

        // Decimal comma support
        assertEquals(650L, parseAmountToMinor("6,50", "USD"))
        assertEquals(1250L, parseAmountToMinor("12,5", "USD"))

        // Rejections: 0, negative, invalid, over 10M
        assertNull(parseAmountToMinor("0", "USD"))
        assertNull(parseAmountToMinor("-5", "USD"))
        assertNull(parseAmountToMinor("", "USD"))
        assertNull(parseAmountToMinor("abc", "USD"))
        assertNull(parseAmountToMinor("10000001", "USD"))

        // 0 fraction digits currency
        assertEquals(500L, parseAmountToMinor("500", "JPY"))
    }

    @Test
    fun editSheetContent_initialValuesAndSave() {
        val initialExpense = ExpenseEntity(
            id = "e1",
            amountMinor = 1250,
            currency = "USD",
            note = "lunch",
            rawText = "12.5 lunch",
            categoryId = "food",
            createdAt = 1000L,
            localDate = "2026-10-15",
            updatedAt = 1000L,
        )

        var savedExpense: ExpenseEntity? = null
        var cancelled = false

        composeTestRule.setContent {
            AppTheme {
                EditSheetContent(
                    expense = initialExpense,
                    categories = CategoryEntity.DEFAULT_CATEGORIES,
                    onSave = { savedExpense = it },
                    onCancel = { cancelled = true },
                )
            }
        }

        // Check initial display
        composeTestRule.onNodeWithText("Edit Expense").assertIsDisplayed()
        composeTestRule.onNodeWithText("12.50").assertIsDisplayed()
        composeTestRule.onNodeWithText("lunch").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Drink").assertIsDisplayed()
        composeTestRule.onNodeWithText("2026-10-15").assertIsDisplayed()

        // Modify note
        composeTestRule.onNodeWithText("lunch").performTextClearance()
        composeTestRule.onNodeWithText("").performTextInput("tacos")
        composeTestRule.waitForIdle()

        // Click Save
        composeTestRule.onNodeWithText("Save").performClick()
        composeTestRule.waitForIdle()

        // Assert saved
        assertNotNull(savedExpense)
        assertEquals("tacos", savedExpense?.note)
        assertEquals(1250L, savedExpense?.amountMinor)
        assertEquals("food", savedExpense?.categoryId)
        assertEquals("2026-10-15", savedExpense?.localDate)
    }

    @Test
    fun editSheetContent_cancel() {
        val initialExpense = ExpenseEntity(
            id = "e1",
            amountMinor = 1250,
            currency = "USD",
            note = "lunch",
            rawText = "12.5 lunch",
            categoryId = "food",
            createdAt = 1000L,
            localDate = "2026-10-15",
            updatedAt = 1000L,
        )

        var cancelled = false

        composeTestRule.setContent {
            AppTheme {
                EditSheetContent(
                    expense = initialExpense,
                    categories = CategoryEntity.DEFAULT_CATEGORIES,
                    onSave = {},
                    onCancel = { cancelled = true },
                )
            }
        }

        composeTestRule.onNodeWithText("Cancel").performClick()
        composeTestRule.waitForIdle()

        assertTrue(cancelled)
    }
}
