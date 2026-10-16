package com.yourco.spend.feature.insight

import androidx.lifecycle.SavedStateHandle
import com.yourco.spend.R
import com.yourco.spend.data.db.dao.BucketTotal
import com.yourco.spend.data.db.dao.CategoryTotal
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.SettingsRepository
import com.yourco.spend.data.repo.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class InsightViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    // Fixed clock: 2026-10-15T12:00:00Z -> Thursday, Oct 15, 2026
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeExpenseRepo: FakeInsightExpenseRepository
    private lateinit var fakeCategoryRepo: FakeInsightCategoryRepository
    private lateinit var fakeSettingsRepo: FakeInsightSettingsRepository
    private lateinit var viewModel: InsightViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeInsightExpenseRepository()
        fakeCategoryRepo = FakeInsightCategoryRepository()
        fakeSettingsRepo = FakeInsightSettingsRepository()

        viewModel = InsightViewModel(
            expenseRepository = fakeExpenseRepo,
            categoryRepository = fakeCategoryRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(),
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_monthMode_defaultsToCurrentMonth() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(InsightMode.MONTH, state.mode)
        assertEquals(LocalDate.of(2026, 10, 15), state.anchorDate)
        assertEquals("October 2026", state.periodLabel)
        assertEquals(0L, state.totalMinor)
        assertEquals("$0.00", state.formattedTotal)
        assertTrue(state.isEmpty)
        assertFalse(state.canGoForward) // Cannot go beyond current month
        assertTrue(state.categories.isEmpty())
        assertEquals(31, state.monthPoints.size) // October has 31 days
    }

    @Test
    fun categoryTotals_sumToPeriodTotal_andPercentagesCalculated() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        // October 2026 bounds: 2026-10-01 to 2026-10-31
        val from = "2026-10-01"
        val to = "2026-10-31"

        val catTotals = listOf(
            CategoryTotal(categoryId = "food", total = 1500L, count = 3),
            CategoryTotal(categoryId = "groceries", total = 3500L, count = 2),
            CategoryTotal(categoryId = "transport", total = 1000L, count = 1),
        )
        val periodTotal = 6000L // 1500 + 3500 + 1000

        fakeExpenseRepo.setRangeData(from, to, periodTotal, catTotals)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isEmpty)
        assertEquals(periodTotal, state.totalMinor)

        // Verify category totals sum exactly to the period total
        val categoriesSum = state.categories.sumOf { it.totalMinor }
        assertEquals(state.totalMinor, categoriesSum)

        assertEquals(3, state.categories.size)

        // Food: 1500 / 6000 = 25% (0.25f)
        val food = state.categories.first { it.categoryId == "food" }
        assertEquals(1500L, food.totalMinor)
        assertEquals(25, food.percentage)
        assertEquals(0.25f, food.fraction, 0.001f)
        assertEquals(R.string.category_food, food.nameRes)

        // Groceries: 3500 / 6000 = 58.33% -> 58%
        val groceries = state.categories.first { it.categoryId == "groceries" }
        assertEquals(3500L, groceries.totalMinor)
        assertEquals(58, groceries.percentage)
        assertEquals(3500f / 6000f, groceries.fraction, 0.001f)

        // Transport: 1000 / 6000 = 16.67% -> 17%
        val transport = state.categories.first { it.categoryId == "transport" }
        assertEquals(1000L, transport.totalMinor)
        assertEquals(17, transport.percentage)
        assertEquals(1000f / 6000f, transport.fraction, 0.001f)
    }

    @Test
    fun charts_handleAllZeroData_safelyWithoutDividingByZero() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Month mode with 0 data
        val monthState = viewModel.state.value
        assertEquals(31, monthState.monthPoints.size)
        monthState.monthPoints.forEach { point ->
            assertEquals(0L, point.totalMinor)
            assertEquals(0f, point.fraction, 0f)
            assertFalse(point.fraction.isNaN())
            assertFalse(point.fraction.isInfinite())
        }
        // Current day (15th) should be marked
        assertTrue(monthState.monthPoints[14].isCurrentDay)

        // Switch to Year mode with 0 data
        viewModel.setMode(InsightMode.YEAR)
        testDispatcher.scheduler.advanceUntilIdle()

        val yearState = viewModel.state.value
        assertEquals(InsightMode.YEAR, yearState.mode)
        assertEquals(12, yearState.yearBars.size)
        yearState.yearBars.forEach { bar ->
            assertEquals(0L, bar.totalMinor)
            assertEquals(0f, bar.fraction, 0f)
            assertFalse(bar.fraction.isNaN())
            assertFalse(bar.fraction.isInfinite())
        }
        // Current month (October, monthIndex 10) should be marked
        assertTrue(yearState.yearBars[9].isCurrentMonth)
        assertFalse(yearState.yearBars[0].isCurrentMonth)
    }

    @Test
    fun charts_handleSingleValueData_safelyWithoutDividingByZero() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        // Test Month mode with single day having expenses
        val monthBuckets = listOf(
            BucketTotal(bucket = "2026-10-15", total = 2500L),
        )
        fakeExpenseRepo.setBuckets("2026-10-01", "2026-10-31", 10, monthBuckets)
        testDispatcher.scheduler.advanceUntilIdle()

        val monthState = viewModel.state.value
        assertEquals(31, monthState.monthPoints.size)
        // Day 15 should have fraction 1.0f
        val day15 = monthState.monthPoints[14]
        assertEquals(15, day15.dayOfMonth)
        assertEquals(2500L, day15.totalMinor)
        assertEquals(1f, day15.fraction, 0.001f)
        assertTrue(day15.isCurrentDay)

        // All other days should have fraction 0f
        monthState.monthPoints.filter { it.dayOfMonth != 15 }.forEach { point ->
            assertEquals(0L, point.totalMinor)
            assertEquals(0f, point.fraction, 0f)
        }

        // Test Year mode with single month having expenses
        viewModel.setMode(InsightMode.YEAR)
        val yearBuckets = listOf(
            BucketTotal(bucket = "2026-06", total = 8800L),
        )
        fakeExpenseRepo.setBuckets("2026-01-01", "2026-12-31", 7, yearBuckets)
        testDispatcher.scheduler.advanceUntilIdle()

        val yearState = viewModel.state.value
        assertEquals(12, yearState.yearBars.size)
        // Month 6 (June) should have fraction 1.0f
        val june = yearState.yearBars[5]
        assertEquals(6, june.monthIndex)
        assertEquals(8800L, june.totalMinor)
        assertEquals(1f, june.fraction, 0.001f)

        // Other months have fraction 0f
        yearState.yearBars.filter { it.monthIndex != 6 }.forEach { bar ->
            assertEquals(0L, bar.totalMinor)
            assertEquals(0f, bar.fraction, 0f)
        }
    }

    @Test
    fun periodNavigation_monthMode() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially in October 2026
        assertEquals("October 2026", viewModel.state.value.periodLabel)
        assertFalse(viewModel.state.value.canGoForward)

        // Previous -> September 2026
        viewModel.previousPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("September 2026", viewModel.state.value.periodLabel)
        assertTrue(viewModel.state.value.canGoForward)

        // Previous -> August 2026
        viewModel.previousPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("August 2026", viewModel.state.value.periodLabel)
        assertTrue(viewModel.state.value.canGoForward)

        // Next -> September 2026
        viewModel.nextPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("September 2026", viewModel.state.value.periodLabel)
        assertTrue(viewModel.state.value.canGoForward)

        // Next -> October 2026 (current month)
        viewModel.nextPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("October 2026", viewModel.state.value.periodLabel)
        assertFalse(viewModel.state.value.canGoForward)

        // Next again -> cannot exceed October 2026
        viewModel.nextPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("October 2026", viewModel.state.value.periodLabel)
        assertFalse(viewModel.state.value.canGoForward)
    }

    @Test
    fun modeSwitching_updatesModeAndLabels() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Month mode
        assertEquals(InsightMode.MONTH, viewModel.state.value.mode)
        assertEquals("October 2026", viewModel.state.value.periodLabel)

        // Switch to YEAR
        viewModel.setMode(InsightMode.YEAR)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(InsightMode.YEAR, viewModel.state.value.mode)
        assertEquals("2026", viewModel.state.value.periodLabel)
        assertEquals(12, viewModel.state.value.yearBars.size)
        assertTrue(viewModel.state.value.monthPoints.isEmpty())

        // Switch to DAY
        viewModel.setMode(InsightMode.DAY)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(InsightMode.DAY, viewModel.state.value.mode)
        assertEquals("Today", viewModel.state.value.periodLabel)
        assertTrue(viewModel.state.value.yearBars.isEmpty())
        assertTrue(viewModel.state.value.monthPoints.isEmpty())
    }

    @Test
    fun monthPoints_matchesMonthDays_28_29_30_31() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }

        // October 2026 has 31 days
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(31, viewModel.state.value.monthPoints.size)

        // September 2026 has 30 days
        viewModel.previousPeriod()
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("September 2026", viewModel.state.value.periodLabel)
        assertEquals(30, viewModel.state.value.monthPoints.size)

        // Feb 2024 (leap year) has 29 days
        viewModel.setAnchorDate(LocalDate.of(2024, 2, 10))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("February 2024", viewModel.state.value.periodLabel)
        assertEquals(29, viewModel.state.value.monthPoints.size)

        // Feb 2023 (non-leap year) has 28 days
        viewModel.setAnchorDate(LocalDate.of(2023, 2, 10))
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("February 2023", viewModel.state.value.periodLabel)
        assertEquals(28, viewModel.state.value.monthPoints.size)
    }

    @Test
    fun categoryNameOverride_respectsUserRename() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val from = "2026-10-01"
        val to = "2026-10-31"

        fakeCategoryRepo.setCustomCategory(
            CategoryEntity(
                id = "food",
                nameOverride = "Fine Dining",
                icon = "restaurant",
                sortOrder = 0,
            )
        )
        fakeExpenseRepo.setRangeData(
            from = from,
            to = to,
            total = 5000L,
            catTotals = listOf(CategoryTotal(categoryId = "food", total = 5000L, count = 1)),
        )
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        val food = state.categories.first { it.categoryId == "food" }
        assertEquals("Fine Dining", food.name)
        assertNull(food.nameRes)
    }

    @Test
    fun savedStateHandle_restoresModeAndAnchor() = runTest(testDispatcher) {
        val handle = SavedStateHandle(
            mapOf(
                InsightViewModel.KEY_MODE to InsightMode.YEAR.name,
                InsightViewModel.KEY_ANCHOR to "2024-05-10",
            )
        )
        val vm = InsightViewModel(
            expenseRepository = fakeExpenseRepo,
            categoryRepository = fakeCategoryRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
            savedStateHandle = handle,
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(InsightMode.YEAR, vm.state.value.mode)
        assertEquals(LocalDate.of(2024, 5, 10), vm.state.value.anchorDate)
        assertEquals("2024", vm.state.value.periodLabel)
        assertTrue(vm.state.value.canGoForward) // 2024 is before today (2026)
    }
}

// --- Fakes ---

private class FakeInsightExpenseRepository : ExpenseRepository {
    private val totalFlows = mutableMapOf<String, MutableStateFlow<Long>>()
    private val categoryFlows = mutableMapOf<String, MutableStateFlow<List<CategoryTotal>>>()
    private val bucketFlows = mutableMapOf<String, MutableStateFlow<List<BucketTotal>>>()

    fun setRangeData(from: String, to: String, total: Long, catTotals: List<CategoryTotal>) {
        val key = "$from:$to"
        totalFlows.getOrPut(key) { MutableStateFlow(0L) }.value = total
        categoryFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.value = catTotals
    }

    fun setBuckets(from: String, to: String, len: Int, buckets: List<BucketTotal>) {
        val key = "$from:$to:$len"
        bucketFlows.getOrPut(key) { MutableStateFlow(emptyList()) }.value = buckets
    }

    override fun observeTotal(from: String, to: String): Flow<Long> {
        val key = "$from:$to"
        return totalFlows.getOrPut(key) { MutableStateFlow(0L) }
    }

    override fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>> {
        val key = "$from:$to"
        return categoryFlows.getOrPut(key) { MutableStateFlow(emptyList()) }
    }

    override fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>> {
        val key = "$from:$to:$len"
        return bucketFlows.getOrPut(key) { MutableStateFlow(emptyList()) }
    }

    override suspend fun add(expense: ExpenseEntity) {}
    override suspend fun update(expense: ExpenseEntity) {}
    override suspend fun softDelete(id: String, now: Long?) {}
    override suspend fun restore(id: String) {}
    override fun observeDay(date: String): Flow<List<ExpenseEntity>> = flowOf(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = null
}

private class FakeInsightCategoryRepository : CategoryRepository {
    private val categoriesFlow = MutableStateFlow<List<CategoryEntity>>(CategoryEntity.DEFAULT_CATEGORIES)

    fun setCustomCategory(category: CategoryEntity) {
        val current = categoriesFlow.value.toMutableList()
        val index = current.indexOfFirst { it.id == category.id }
        if (index >= 0) {
            current[index] = category
        } else {
            current.add(category)
        }
        categoriesFlow.value = current
    }

    override fun observeAll(): Flow<List<CategoryEntity>> = categoriesFlow
    override fun observeActive(): Flow<List<CategoryEntity>> = categoriesFlow
    override suspend fun getById(id: String): CategoryEntity? = categoriesFlow.value.firstOrNull { it.id == id }
    override suspend fun seedDefaultsIfEmpty() {}
    override suspend fun updateNameOverride(id: String, nameOverride: String?) {}
    override suspend fun setHidden(id: String, isHidden: Boolean) {}
    override suspend fun upsert(category: CategoryEntity) {}
}

private class FakeInsightSettingsRepository : SettingsRepository {
    private val _theme = MutableStateFlow(ThemeMode.SYSTEM)
    private val _lang = MutableStateFlow("")
    private val _currency = MutableStateFlow("USD")
    private val _firstRun = MutableStateFlow(true)

    override val themeMode: Flow<ThemeMode> = _theme
    override val languageTag: Flow<String> = _lang
    override val currencyCode: Flow<String> = _currency
    override val firstRunDone: Flow<Boolean> = _firstRun

    override suspend fun setThemeMode(mode: ThemeMode) { _theme.value = mode }
    override suspend fun setLanguageTag(tag: String) { _lang.value = tag }
    override suspend fun setCurrencyCode(code: String) { _currency.value = code }
    override suspend fun setFirstRunDone(done: Boolean) { _firstRun.value = done }
}
