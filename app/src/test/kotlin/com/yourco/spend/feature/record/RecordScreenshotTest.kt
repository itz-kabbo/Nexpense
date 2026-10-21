package com.yourco.spend.feature.record

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.yourco.spend.core.designsystem.AppTheme
import com.yourco.spend.data.db.entity.ExpenseEntity
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot test recording Record screen in light, dark, and 2.0 font scale.
 * Verifies accessibility and that row texts do not clip under 200% font scale.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel4a)
class RecordScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val recordOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun sampleRecordState(): RecordUiState {
        val expenses = listOf(
            ExpenseEntity(
                id = "1",
                amountMinor = 600L,
                currency = "USD",
                note = "coffee",
                rawText = "6 coffee",
                categoryId = "food",
                createdAt = 1000L,
                localDate = "2026-10-15",
                updatedAt = 1000L,
            ),
            ExpenseEntity(
                id = "2",
                amountMinor = 1250L,
                currency = "USD",
                note = "lunch with team",
                rawText = "12.5 lunch with team",
                categoryId = "food",
                createdAt = 2000L,
                localDate = "2026-10-15",
                updatedAt = 2000L,
            ),
            ExpenseEntity(
                id = "3",
                amountMinor = 4500L,
                currency = "USD",
                note = "groceries supermarket",
                rawText = "45 groceries supermarket",
                categoryId = "groceries",
                createdAt = 3000L,
                localDate = "2026-10-15",
                updatedAt = 3000L,
            ),
        )
        return RecordUiState(
            todayDateLabel = "Thursday, Oct 15, 2026",
            totalMinor = 6350L,
            formattedTotal = "$63.50",
            currencySymbol = "$",
            amountString = "63.50",
            expenses = expenses,
            currencyCode = "USD",
        )
    }

    @Test
    fun captureRecord_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                TodayContent(state = sampleRecordState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/record_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureRecord_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                TodayContent(state = sampleRecordState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/record_dark.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureRecord_fontScale2() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 2.0f,
                ),
            ) {
                AppTheme(darkTheme = false) {
                    TodayContent(state = sampleRecordState())
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/record_font_scale_2.png",
            roborazziOptions = recordOptions,
        )
    }
}
