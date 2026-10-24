package com.nexusbench.nexpense.feature.insight

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import com.nexusbench.nexpense.R
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
class InsightScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val recordOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)

    private fun sampleCategories(): List<InsightCategoryUiModel> = listOf(
        InsightCategoryUiModel(
            categoryId = "food",
            name = "Food & Drink",
            nameRes = R.string.category_food,
            totalMinor = 4500L,
            formattedAmount = "$45.00",
            percentage = 60,
            fraction = 0.60f,
        ),
        InsightCategoryUiModel(
            categoryId = "groceries",
            name = "Groceries",
            nameRes = R.string.category_groceries,
            totalMinor = 1800L,
            formattedAmount = "$18.00",
            percentage = 24,
            fraction = 0.24f,
        ),
        InsightCategoryUiModel(
            categoryId = "transport",
            name = "Transport",
            nameRes = R.string.category_transport,
            totalMinor = 1150L,
            formattedAmount = "$11.50",
            percentage = 15,
            fraction = 0.15f,
        ),
    )

    private fun sampleMonthPoints(): List<MonthPointUiModel> = (1..31).map { day ->
        val total = when (day) {
            3, 10, 15, 22 -> 1500L
            5, 12, 28 -> 3200L
            1, 8, 20 -> 800L
            else -> 0L
        }
        val fraction = total / 3200f
        MonthPointUiModel(
            dayOfMonth = day,
            totalMinor = total,
            fraction = fraction,
            isCurrentDay = (day == 15),
        )
    }

    private fun sampleYearBars(): List<YearBarUiModel> {
        val labels = listOf("J", "F", "M", "A", "M", "J", "J", "A", "S", "O", "N", "D")
        val totals = listOf(35000L, 42000L, 28000L, 51000L, 62000L, 49000L, 71000L, 58000L, 64000L, 89200L, 0L, 0L)
        val maxTotal = 89200f
        return (1..12).map { i ->
            YearBarUiModel(
                monthIndex = i,
                label = labels[i - 1],
                totalMinor = totals[i - 1],
                fraction = totals[i - 1] / maxTotal,
                isCurrentMonth = (i == 10),
            )
        }
    }

    private fun monthState(): InsightUiState = InsightUiState(
        mode = InsightMode.MONTH,
        anchorDate = LocalDate.parse("2026-10-15"),
        periodLabel = "October 2026",
        totalMinor = 7450L,
        formattedTotal = "$74.50",
        currencyCode = "USD",
        currencySymbol = "$",
        amountDigits = "74.50",
        canGoForward = false,
        isEmpty = false,
        categories = sampleCategories(),
        monthPoints = sampleMonthPoints(),
        yearBars = emptyList(),
    )

    private fun yearState(): InsightUiState = InsightUiState(
        mode = InsightMode.YEAR,
        anchorDate = LocalDate.parse("2026-10-15"),
        periodLabel = "2026",
        totalMinor = 89200L,
        formattedTotal = "$892.00",
        currencyCode = "USD",
        currencySymbol = "$",
        amountDigits = "892.00",
        canGoForward = false,
        isEmpty = false,
        categories = sampleCategories(),
        monthPoints = emptyList(),
        yearBars = sampleYearBars(),
    )

    private fun dayState(): InsightUiState = InsightUiState(
        mode = InsightMode.DAY,
        anchorDate = LocalDate.parse("2026-10-15"),
        periodLabel = "Today",
        totalMinor = 2250L,
        formattedTotal = "$22.50",
        currencyCode = "USD",
        currencySymbol = "$",
        amountDigits = "22.50",
        canGoForward = false,
        isEmpty = false,
        categories = sampleCategories().take(2),
        monthPoints = emptyList(),
        yearBars = emptyList(),
    )

    private fun emptyState(): InsightUiState = InsightUiState(
        mode = InsightMode.MONTH,
        anchorDate = LocalDate.parse("2026-10-15"),
        periodLabel = "October 2026",
        totalMinor = 0L,
        formattedTotal = "$0.00",
        currencyCode = "USD",
        currencySymbol = "$",
        amountDigits = "0.00",
        canGoForward = false,
        isEmpty = true,
        categories = emptyList(),
        monthPoints = (1..31).map { day ->
            MonthPointUiModel(
                dayOfMonth = day,
                totalMinor = 0L,
                fraction = 0f,
                isCurrentDay = (day == 15),
            )
        },
        yearBars = emptyList(),
    )

    @Test
    fun captureInsight_month_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                InsightScreenContent(state = monthState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_month_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_month_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                InsightScreenContent(state = monthState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_month_dark.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_year_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                InsightScreenContent(state = yearState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_year_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_year_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                InsightScreenContent(state = yearState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_year_dark.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_day_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                InsightScreenContent(state = dayState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_day_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_empty_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                InsightScreenContent(state = emptyState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_empty_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureInsight_empty_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                InsightScreenContent(state = emptyState())
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/insight_empty_dark.png",
            roborazziOptions = recordOptions,
        )
    }
}
