package com.yourco.spend.feature.record

import com.yourco.spend.core.time.todayLocalDate
import com.yourco.spend.data.db.dao.BucketTotal
import com.yourco.spend.data.db.dao.CategoryTotal
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.SettingsRepository
import com.yourco.spend.data.repo.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class RecordViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    // 2026-10-15T10:00:00Z -> Thursday, Oct 15
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T10:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var viewModel: RecordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeExpenseRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        viewModel = RecordViewModel(
            expenseRepository = fakeExpenseRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialState_emptyExpenses_totalZero() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val state = viewModel.state.value
        assertTrue(state.todayDateLabel.contains("Thursday") || state.todayDateLabel.contains("Oct 15") || state.todayDateLabel.contains("15"))
        assertEquals(0L, state.totalMinor)
        assertTrue(state.expenses.isEmpty())
        assertEquals("USD", state.currencyCode)
    }

    @Test
    fun withExpenses_computesTotal_andFormatsCurrency() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        val today = fixedClock.todayLocalDate()
        val expense1 = ExpenseEntity(
            id = "e1",
            amountMinor = 600,
            currency = "USD",
            note = "coffee",
            rawText = "6 coffee",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = today,
            updatedAt = fixedClock.millis(),
        )
        val expense2 = ExpenseEntity(
            id = "e2",
            amountMinor = 1250,
            currency = "USD",
            note = "lunch",
            rawText = "12.5 lunch",
            categoryId = "food",
            createdAt = fixedClock.millis() + 1000,
            localDate = today,
            updatedAt = fixedClock.millis() + 1000,
        )

        fakeExpenseRepo.setDayExpenses(listOf(expense2, expense1))
        fakeExpenseRepo.setTotal(1850)
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals(1850L, state.totalMinor)
        assertEquals(2, state.expenses.size)
        assertEquals("e2", state.expenses[0].id)
        assertEquals("18.50", state.amountString)
    }

    @Test
    fun currencyCodeChange_updatesUiState() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(kotlinx.coroutines.test.UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        fakeSettingsRepo.setCurrencyCode("EUR")
        testDispatcher.scheduler.advanceUntilIdle()

        val state = viewModel.state.value
        assertEquals("EUR", state.currencyCode)
    }
}

private class FakeExpenseRepository : ExpenseRepository {
    private val dayFlow = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    private val totalFlow = MutableStateFlow(0L)

    fun setDayExpenses(expenses: List<ExpenseEntity>) {
        dayFlow.value = expenses
    }

    fun setTotal(total: Long) {
        totalFlow.value = total
    }

    override suspend fun add(expense: ExpenseEntity) {
        val current = dayFlow.value.toMutableList()
        current.add(0, expense)
        dayFlow.value = current
        totalFlow.value += expense.amountMinor
    }

    override suspend fun update(expense: ExpenseEntity) {}
    override suspend fun softDelete(id: String, now: Long?) {}
    override suspend fun restore(id: String) {}
    override fun observeDay(date: String): Flow<List<ExpenseEntity>> = dayFlow
    override fun observeTotal(from: String, to: String): Flow<Long> = totalFlow
    override fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>> = MutableStateFlow(emptyList())
    override fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>> = MutableStateFlow(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = null
}

private class FakeSettingsRepository : SettingsRepository {
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
