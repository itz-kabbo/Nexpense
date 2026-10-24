package com.nexusbench.nexpense.feature.receipt

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexusbench.nexpense.core.designsystem.AppTheme
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode
import java.time.LocalDate

@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel4a)
class ReceiptScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val recordOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun sampleState(): ReceiptUiState {
        val items = listOf(
            ReceiptItemUiModel(
                id = "1",
                note = "Coffee",
                categoryId = "food",
                amountMinor = 450,
                formattedAmount = "$4.50",
            ),
            ReceiptItemUiModel(
                id = "2",
                note = "Lunch",
                categoryId = "food",
                amountMinor = 1800,
                formattedAmount = "$18.00",
            ),
            ReceiptItemUiModel(
                id = "3",
                note = "Groceries",
                categoryId = "groceries",
                amountMinor = 5200,
                formattedAmount = "$52.00",
            ),
        )
        return ReceiptUiState(
            selectedDate = "2026-10-15",
            date = LocalDate.parse("2026-10-15"),
            formattedDate = "Thursday, Oct 15, 2026",
            items = items,
            totalMinor = 7450,
            formattedTotal = "$74.50",
            currencyCode = "USD",
            isEmpty = false,
            canGoForward = false,
        )
    }

    private fun fiftyRowState(): ReceiptUiState {
        val items = (1..50).map { i ->
            val minor = (i * 100).toLong()
            ReceiptItemUiModel(
                id = "item_$i",
                note = "Expense Item #$i",
                categoryId = if (i % 2 == 0) "food" else "shopping",
                amountMinor = minor,
                formattedAmount = "$${i}.00",
            )
        }
        val totalMinor = items.sumOf { it.amountMinor }
        return ReceiptUiState(
            selectedDate = "2026-10-15",
            date = LocalDate.parse("2026-10-15"),
            formattedDate = "Thursday, Oct 15, 2026",
            items = items,
            totalMinor = totalMinor,
            formattedTotal = "$${totalMinor / 100}.00",
            currencyCode = "USD",
            isEmpty = false,
            canGoForward = false,
        )
    }

    @Test
    fun captureReceipt_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                ReceiptScreenContent(
                    state = sampleState(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/receipt_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureReceipt_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                ReceiptScreenContent(
                    state = sampleState(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/receipt_dark.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureReceipt_50_rows() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                ReceiptScreenContent(
                    state = fiftyRowState(),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/receipt_50_rows.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureReceipt_empty() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                ReceiptScreenContent(
                    state = ReceiptUiState(
                        selectedDate = "2026-10-15",
                        date = LocalDate.parse("2026-10-15"),
                        formattedDate = "Thursday, Oct 15, 2026",
                        items = emptyList(),
                        totalMinor = 0L,
                        formattedTotal = "$0.00",
                        currencyCode = "USD",
                        isEmpty = true,
                        canGoForward = false,
                    ),
                )
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/receipt_empty.png",
            roborazziOptions = recordOptions,
        )
    }
}
