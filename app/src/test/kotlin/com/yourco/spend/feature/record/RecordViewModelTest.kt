package com.yourco.spend.feature.record

import com.yourco.spend.core.time.todayLocalDate
import com.yourco.spend.data.db.dao.BucketTotal
import com.yourco.spend.data.db.dao.CategoryTotal
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.db.entity.ExpenseEntity
import com.yourco.spend.data.db.entity.LearnedMappingEntity
import com.yourco.spend.data.repo.CategoryRepository
import com.yourco.spend.data.repo.ExpenseRepository
import com.yourco.spend.data.repo.LearnedMappingRepository
import com.yourco.spend.data.repo.SettingsRepository
import com.yourco.spend.data.repo.ThemeMode
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
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
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeLearnedMappingRepo: FakeLearnedMappingRepository
    private lateinit var viewModel: RecordViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        fakeExpenseRepo = FakeExpenseRepository()
        fakeSettingsRepo = FakeSettingsRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeLearnedMappingRepo = FakeLearnedMappingRepository()

        viewModel = RecordViewModel(
            expenseRepository = fakeExpenseRepo,
            settingsRepository = fakeSettingsRepo,
            categoryRepository = fakeCategoryRepo,
            learnedMappingRepository = fakeLearnedMappingRepo,
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
        assertEquals(state.todayDateLabel, state.formattedDate)
        assertEquals(0L, state.totalMinor)
        assertTrue(state.expenses.isEmpty())
        assertTrue(state.todayExpenses.isEmpty())
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

    @Test
    fun editingExpense_startAndClear() = runTest(testDispatcher) {
        assertNull(viewModel.editingExpense.value)

        val expense = ExpenseEntity(
            id = "e1",
            amountMinor = 600,
            currency = "USD",
            note = "coffee",
            rawText = "6 coffee",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = fixedClock.todayLocalDate(),
            updatedAt = fixedClock.millis(),
        )

        viewModel.startEditing(expense)
        assertEquals(expense, viewModel.editingExpense.value)

        viewModel.clearEditing()
        assertNull(viewModel.editingExpense.value)
    }

    @Test
    fun updateExpense_updatesRepository_andUpsertsLearnedMappingWhenCategoryChanged() = runTest(testDispatcher) {
        val original = ExpenseEntity(
            id = "e1",
            amountMinor = 600,
            currency = "USD",
            note = "boba",
            rawText = "6 boba",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = fixedClock.todayLocalDate(),
            updatedAt = fixedClock.millis(),
        )
        viewModel.startEditing(original)

        val updated = original.copy(
            amountMinor = 700,
            note = "boba",
            categoryId = "groceries",
            localDate = "2026-10-14",
        )
        viewModel.updateExpense(updated)
        testDispatcher.scheduler.advanceUntilIdle()

        // Assert editingExpense cleared
        assertNull(viewModel.editingExpense.value)

        // Assert updated in repository
        val inRepo = fakeExpenseRepo.updatedExpenses.find { it.id == "e1" }
        assertNotNull(inRepo)
        assertEquals(700L, inRepo?.amountMinor)
        assertEquals("boba", inRepo?.note)
        assertEquals("groceries", inRepo?.categoryId)
        assertEquals("2026-10-14", inRepo?.localDate)
        assertEquals(fixedClock.millis(), inRepo?.updatedAt)

        // Assert learned mapping recorded
        assertEquals("groceries", fakeLearnedMappingRepo.getMapping("boba"))
    }

    @Test
    fun deleteExpense_callsSoftDelete_andUndoDeleteCallsRestore() = runTest(testDispatcher) {
        val expense = ExpenseEntity(
            id = "e1",
            amountMinor = 600,
            currency = "USD",
            note = "coffee",
            rawText = "6 coffee",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = fixedClock.todayLocalDate(),
            updatedAt = fixedClock.millis(),
        )

        viewModel.deleteExpense(expense)
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeExpenseRepo.softDeletedIds.contains("e1"))

        viewModel.undoDelete("e1")
        testDispatcher.scheduler.advanceUntilIdle()

        assertTrue(fakeExpenseRepo.restoredIds.contains("e1"))
    }
}

private class FakeExpenseRepository : ExpenseRepository {
    private val dayFlow = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    private val totalFlow = MutableStateFlow(0L)
    val updatedExpenses = mutableListOf<ExpenseEntity>()
    val softDeletedIds = mutableListOf<String>()
    val restoredIds = mutableListOf<String>()

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

    override suspend fun update(expense: ExpenseEntity) {
        updatedExpenses.add(expense)
    }

    override suspend fun softDelete(id: String, now: Long?) {
        softDeletedIds.add(id)
    }

    override suspend fun restore(id: String) {
        restoredIds.add(id)
    }

    override fun observeDay(date: String): Flow<List<ExpenseEntity>> = dayFlow
    override fun observeTotal(from: String, to: String, currency: String): Flow<Long> = totalFlow
    override fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>> = MutableStateFlow(emptyList())
    override fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>> = MutableStateFlow(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = null
    override suspend fun getAll(): List<ExpenseEntity> = emptyList()
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

private class FakeCategoryRepository : CategoryRepository {
    private val categories = MutableStateFlow(CategoryEntity.DEFAULT_CATEGORIES)
    override fun observeAll(): Flow<List<CategoryEntity>> = categories
    override fun observeActive(): Flow<List<CategoryEntity>> = categories
    override suspend fun getAll(): List<CategoryEntity> = categories.value
    override suspend fun getById(id: String): CategoryEntity? = categories.value.find { it.id == id }
    override suspend fun seedDefaultsIfEmpty() {}
    override suspend fun updateNameOverride(id: String, nameOverride: String?) {}
    override suspend fun setHidden(id: String, isHidden: Boolean) {}
    override suspend fun upsert(category: CategoryEntity) {}
}

private class FakeLearnedMappingRepository : LearnedMappingRepository {
    private val map = mutableMapOf<String, String>()
    private val mapFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeAll(): Flow<List<LearnedMappingEntity>> = MutableStateFlow(emptyList())
    override fun observeAllAsMap(): Flow<Map<String, String>> = mapFlow
    override suspend fun getMapping(normalizedNote: String): String? = map[normalizedNote.lowercase().trim()]
    override suspend fun recordMapping(normalizedNote: String, categoryId: String) {
        val key = normalizedNote.lowercase().trim()
        map[key] = categoryId
        mapFlow.value = map.toMap()
    }
}
