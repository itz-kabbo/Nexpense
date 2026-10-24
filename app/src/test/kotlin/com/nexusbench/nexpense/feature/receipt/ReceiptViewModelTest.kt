package com.nexusbench.nexpense.feature.receipt

import androidx.lifecycle.SavedStateHandle
import com.nexusbench.nexpense.data.db.dao.BucketTotal
import com.nexusbench.nexpense.data.db.dao.CategoryTotal
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import com.nexusbench.nexpense.data.repo.ExpenseRepository
import com.nexusbench.nexpense.data.repo.SettingsRepository
import com.nexusbench.nexpense.data.repo.ThemeMode
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
import com.nexusbench.nexpense.R
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
class ReceiptViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    // 2026-10-15T10:00:00Z -> Thursday, Oct 15, 2026
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeExpenseRepo: FakeReceiptExpenseRepository
    private lateinit var fakeSettingsRepo: FakeReceiptSettingsRepository
    private lateinit var viewModel: ReceiptViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeReceiptExpenseRepository()
        fakeSettingsRepo = FakeReceiptSettingsRepository()

        viewModel = ReceiptViewModel(
            expenseRepository = fakeExpenseRepo,
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
    fun initialState_emptyDay_showsEmptyStateAndNoForwardNavigation() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("2026-10-15", state.selectedDate)
        assertTrue(state.formattedDate.contains("Oct 15") || state.formattedDate.contains("15"))
        assertTrue(state.isEmpty)
        assertTrue(state.items.isEmpty())
        assertEquals(0L, state.totalMinor)
        assertFalse(state.canGoForward) // Cannot go past today
    }

    @Test
    fun receiptTotalsEqualDatabaseSumsForSampleDays() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val date = "2026-10-15"
        val sampleExpenses = listOf(
            ExpenseEntity(
                id = "e1",
                amountMinor = 450,
                currency = "USD",
                note = "coffee",
                rawText = "4.50 coffee",
                categoryId = "food",
                createdAt = 1000L,
                localDate = date,
                updatedAt = 1000L,
            ),
            ExpenseEntity(
                id = "e2",
                amountMinor = 1800,
                currency = "USD",
                note = "lunch",
                rawText = "18 lunch",
                categoryId = "food",
                createdAt = 2000L,
                localDate = date,
                updatedAt = 2000L,
            ),
            ExpenseEntity(
                id = "e3",
                amountMinor = 5200,
                currency = "USD",
                note = "groceries",
                rawText = "52 groceries",
                categoryId = "groceries",
                createdAt = 3000L,
                localDate = date,
                updatedAt = 3000L,
            ),
        )

        val expectedTotal = 450L + 1800L + 5200L // 7450
        fakeExpenseRepo.setDayData(date, sampleExpenses.reversed(), expectedTotal) // DAO returns newest first
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertFalse(state.isEmpty)
        assertEquals(expectedTotal, state.totalMinor)
        assertEquals(3, state.items.size)

        // Verifying chronological ordering: e1 (1000L), e2 (2000L), e3 (3000L)
        assertEquals("coffee", state.items[0].note)
        assertEquals(450L, state.items[0].amountMinor)
        assertEquals("lunch", state.items[1].note)
        assertEquals(1800L, state.items[1].amountMinor)
        assertEquals("groceries", state.items[2].note)
        assertEquals(5200L, state.items[2].amountMinor)

        // Total equals sum of items and database sum
        assertEquals(state.items.sumOf { it.amountMinor }, state.totalMinor)
        assertTrue(state.formattedTotal.contains("74.50"))
    }

    @Test
    fun dayNavigation_previousDay_andNextDay() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Initially today: 2026-10-15
        assertEquals("2026-10-15", viewModel.state.value.selectedDate)
        assertFalse(viewModel.state.value.canGoForward)

        // Go to previous day -> 2026-10-14
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-14", viewModel.state.value.selectedDate)
        assertTrue(viewModel.state.value.canGoForward) // Can now go forward back to today

        // Go to previous day again -> 2026-10-13
        viewModel.previousDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-13", viewModel.state.value.selectedDate)
        assertTrue(viewModel.state.value.canGoForward)

        // Next day -> 2026-10-14
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-14", viewModel.state.value.selectedDate)
        assertTrue(viewModel.state.value.canGoForward)

        // Next day -> 2026-10-15 (today)
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-15", viewModel.state.value.selectedDate)
        assertFalse(viewModel.state.value.canGoForward)

        // Next day when already today -> remains today, does not exceed today
        viewModel.nextDay()
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-15", viewModel.state.value.selectedDate)
        assertFalse(viewModel.state.value.canGoForward)
    }

    @Test
    fun setDate_clampsFutureDatesToToday() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Set past date
        viewModel.setDate(LocalDate.parse("2026-09-01"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-09-01", viewModel.state.value.selectedDate)
        assertTrue(viewModel.state.value.canGoForward)

        // Set future date (e.g. 2026-10-20) -> should clamp to today (2026-10-15)
        viewModel.setDate(LocalDate.parse("2026-10-20"))
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-15", viewModel.state.value.selectedDate)
        assertFalse(viewModel.state.value.canGoForward)
    }

    @Test
    fun currencyCodeChange_updatesFormattedTotal() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val date = "2026-10-15"
        fakeExpenseRepo.setDayData(
            date = date,
            expenses = listOf(
                ExpenseEntity(
                    id = "e1",
                    amountMinor = 1500,
                    currency = "EUR",
                    note = "museum",
                    rawText = "15 museum",
                    categoryId = "fun",
                    createdAt = 1000L,
                    localDate = date,
                    updatedAt = 1000L,
                ),
            ),
            total = 1500L,
        )
        fakeSettingsRepo.setCurrencyCode("EUR")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("EUR", state.currencyCode)
        assertEquals(1500L, state.totalMinor)
        assertTrue(state.formattedTotal.contains("15") || state.formattedTotal.contains("€"))
    }

    @Test
    fun savedStateHandle_futureDate_clampsToToday() = runTest(testDispatcher) {
        val vm = ReceiptViewModel(
            expenseRepository = fakeExpenseRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(mapOf(ReceiptViewModel.KEY_SELECTED_DATE to "2026-10-25")),
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // Clock is fixed to 2026-10-15, so 2026-10-25 is clamped to 2026-10-15
        assertEquals("2026-10-15", vm.state.value.selectedDate)
        assertFalse(vm.state.value.canGoForward)
    }

    @Test
    fun savedStateHandle_pastDate_restoresPastDate() = runTest(testDispatcher) {
        val vm = ReceiptViewModel(
            expenseRepository = fakeExpenseRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(mapOf(ReceiptViewModel.KEY_SELECTED_DATE to "2026-10-10")),
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-10-10", vm.state.value.selectedDate)
        assertTrue(vm.state.value.canGoForward)
    }

    @Test
    fun blankExpenseNote_resolvesToCategoryStringResource() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val date = "2026-10-15"
        val expenses = listOf(
            ExpenseEntity(
                id = "e1",
                amountMinor = 1200,
                currency = "USD",
                note = "",
                rawText = "12",
                categoryId = "food",
                createdAt = 1000L,
                localDate = date,
                updatedAt = 1000L,
            ),
            ExpenseEntity(
                id = "e2",
                amountMinor = 3000,
                currency = "USD",
                note = "   ",
                rawText = "30",
                categoryId = "unknown_custom",
                createdAt = 2000L,
                localDate = date,
                updatedAt = 2000L,
            ),
            ExpenseEntity(
                id = "e3",
                amountMinor = 500,
                currency = "USD",
                note = "coffee",
                rawText = "5 coffee",
                categoryId = "food",
                createdAt = 3000L,
                localDate = date,
                updatedAt = 3000L,
            ),
        )
        fakeExpenseRepo.setDayData(date, expenses.reversed(), 4700L)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(3, state.items.size)

        // e1: blank note -> noteRes is R.string.category_food
        assertEquals("", state.items[0].note)
        assertEquals(R.string.category_food, state.items[0].noteRes)

        // e2: unknown category and blank note -> localized "Other" fallback
        assertEquals("", state.items[1].note)
        assertEquals(R.string.category_other, state.items[1].noteRes)

        // e3: non-blank note -> noteRes is null, note preserved
        assertEquals("coffee", state.items[2].note)
        assertNull(state.items[2].noteRes)
    }
}

private class FakeReceiptExpenseRepository : ExpenseRepository {
    private val dayFlows = mutableMapOf<String, MutableStateFlow<List<ExpenseEntity>>>()
    private val totalFlows = mutableMapOf<String, MutableStateFlow<Long>>()

    fun setDayData(date: String, expenses: List<ExpenseEntity>, total: Long) {
        val dayFlow = dayFlows.getOrPut(date) { MutableStateFlow(emptyList()) }
        dayFlow.value = expenses
        val totalFlow = totalFlows.getOrPut(date) { MutableStateFlow(0L) }
        totalFlow.value = total
    }

    override suspend fun add(expense: ExpenseEntity) {}
    override suspend fun update(expense: ExpenseEntity) {}
    override suspend fun softDelete(id: String, now: Long?) {}
    override suspend fun restore(id: String) {}

    override fun observeDay(date: String): Flow<List<ExpenseEntity>> {
        return dayFlows.getOrPut(date) { MutableStateFlow(emptyList()) }
    }

    override fun observeTotal(from: String, to: String, currency: String): Flow<Long> {
        return totalFlows.getOrPut(from) { MutableStateFlow(0L) }
    }

    override fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>> = flowOf(emptyList())
    override fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>> = flowOf(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = null
    override suspend fun getAll(): List<ExpenseEntity> = emptyList()
}

private class FakeReceiptSettingsRepository : SettingsRepository {
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
