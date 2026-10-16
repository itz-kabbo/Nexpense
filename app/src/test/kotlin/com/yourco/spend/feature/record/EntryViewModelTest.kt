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
import com.yourco.spend.parser.Confidence
import com.yourco.spend.parser.ExpenseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
class EntryViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))

    private lateinit var fakeExpenseRepo: FakeExpenseRepo
    private lateinit var fakeCategoryRepo: FakeCategoryRepo
    private lateinit var fakeLearnedMappingRepo: FakeLearnedMappingRepo
    private lateinit var fakeSettingsRepo: FakeSettingsRepo
    private lateinit var parser: ExpenseParser
    private lateinit var viewModel: EntryViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        parser = ExpenseParser()
        fakeExpenseRepo = FakeExpenseRepo()
        fakeCategoryRepo = FakeCategoryRepo()
        fakeLearnedMappingRepo = FakeLearnedMappingRepo()
        fakeSettingsRepo = FakeSettingsRepo()

        viewModel = EntryViewModel(
            expenseParser = parser,
            expenseRepository = fakeExpenseRepo,
            categoryRepository = fakeCategoryRepo,
            learnedMappingRepository = fakeLearnedMappingRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
        )
        testDispatcher.scheduler.advanceUntilIdle()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun initialText_isEmpty_andPreviewIsEmpty() {
        assertEquals("", viewModel.text.value)
        assertTrue(viewModel.preview.value is EntryPreview.Empty)
    }

    @Test
    fun textWithoutAmount_producesNeedsAmount() {
        viewModel.onTextChange("coffee")
        assertTrue(viewModel.preview.value is EntryPreview.NeedsAmount)
    }

    @Test
    fun validText_producesValidPreview() {
        viewModel.onTextChange("6 coffee")
        val preview = viewModel.preview.value
        assertTrue(preview is EntryPreview.Valid)
        val valid = preview as EntryPreview.Valid
        assertEquals(600L, valid.amountMinor)
        assertEquals("coffee", valid.note)
        assertEquals("food", valid.categoryId)
        assertEquals("Food & Drink", valid.categoryName)
        assertEquals(Confidence.HIGH, valid.confidence)
    }

    @Test
    fun categoryPicker_overridesCategory() {
        viewModel.onTextChange("6 coffee")
        viewModel.onCategoryPicked("groceries")

        val preview = viewModel.preview.value
        assertTrue(preview is EntryPreview.Valid)
        val valid = preview as EntryPreview.Valid
        assertEquals("groceries", valid.categoryId)
        assertEquals("Groceries", valid.categoryName)
    }

    @Test
    fun learnedMapping_updatesParserInstantly() = runTest(testDispatcher) {
        // Initially "latte" -> "food"
        viewModel.onTextChange("5 latte")
        assertEquals("food", (viewModel.preview.value as EntryPreview.Valid).categoryId)

        // Learn that latte is "fun"
        fakeLearnedMappingRepo.recordMapping("latte", "fun")
        testDispatcher.scheduler.advanceUntilIdle()

        // Re-type or check preview
        viewModel.onTextChange("5 latte")
        assertEquals("fun", (viewModel.preview.value as EntryPreview.Valid).categoryId)
    }

    @Test
    fun submit_insertsExpense_clearsText_andRecordsOverriddenCategory() = runTest(testDispatcher) {
        viewModel.onTextChange("6 coffee")
        viewModel.onCategoryPicked("groceries")

        val submitted = viewModel.submit()
        assertTrue(submitted)
        testDispatcher.scheduler.advanceUntilIdle()

        // Text should be cleared
        assertEquals("", viewModel.text.value)
        assertTrue(viewModel.preview.value is EntryPreview.Empty)

        // Expense should be in repository
        val expenses = fakeExpenseRepo.addedExpenses
        assertEquals(1, expenses.size)
        val expense = expenses[0]
        assertEquals(600L, expense.amountMinor)
        assertEquals("coffee", expense.note)
        assertEquals("6 coffee", expense.rawText)
        assertEquals("groceries", expense.categoryId)
        assertEquals("2026-10-15", expense.localDate)
        assertEquals("USD", expense.currency)

        // Learned mapping recorded for "coffee" -> "groceries"
        assertEquals("groceries", fakeLearnedMappingRepo.getMapping("coffee"))
    }
}

private class FakeExpenseRepo : ExpenseRepository {
    val addedExpenses = mutableListOf<ExpenseEntity>()

    override suspend fun add(expense: ExpenseEntity) {
        addedExpenses.add(expense)
    }

    override suspend fun update(expense: ExpenseEntity) {}
    override suspend fun softDelete(id: String, now: Long?) {}
    override suspend fun restore(id: String) {}
    override fun observeDay(date: String): Flow<List<ExpenseEntity>> = MutableStateFlow(addedExpenses)
    override fun observeTotal(from: String, to: String): Flow<Long> = MutableStateFlow(addedExpenses.sumOf { it.amountMinor })
    override fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>> = MutableStateFlow(emptyList())
    override fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>> = MutableStateFlow(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = addedExpenses.find { it.id == id }
}

private class FakeCategoryRepo : CategoryRepository {
    private val categories = MutableStateFlow(CategoryEntity.DEFAULT_CATEGORIES)

    override fun observeAll(): Flow<List<CategoryEntity>> = categories
    override fun observeActive(): Flow<List<CategoryEntity>> = categories
    override suspend fun getById(id: String): CategoryEntity? = categories.value.find { it.id == id }
    override suspend fun seedDefaultsIfEmpty() {}
    override suspend fun updateNameOverride(id: String, nameOverride: String?) {}
    override suspend fun setHidden(id: String, isHidden: Boolean) {}
    override suspend fun upsert(category: CategoryEntity) {}
}

private class FakeLearnedMappingRepo : LearnedMappingRepository {
    private val mapFlow = MutableStateFlow<Map<String, String>>(emptyMap())

    override fun observeAll(): Flow<List<LearnedMappingEntity>> = MutableStateFlow(emptyList())
    override fun observeAllAsMap(): Flow<Map<String, String>> = mapFlow
    override suspend fun getMapping(normalizedNote: String): String? = mapFlow.value[normalizedNote.lowercase().trim()]
    override suspend fun recordMapping(normalizedNote: String, categoryId: String) {
        val current = mapFlow.value.toMutableMap()
        current[normalizedNote.lowercase().trim()] = categoryId
        mapFlow.value = current
    }
}

private class FakeSettingsRepo : SettingsRepository {
    private val _currency = MutableStateFlow("USD")
    override val themeMode: Flow<ThemeMode> = MutableStateFlow(ThemeMode.SYSTEM)
    override val languageTag: Flow<String> = MutableStateFlow("")
    override val currencyCode: Flow<String> = _currency
    override val firstRunDone: Flow<Boolean> = MutableStateFlow(true)
    override suspend fun setThemeMode(mode: ThemeMode) {}
    override suspend fun setLanguageTag(tag: String) {}
    override suspend fun setCurrencyCode(code: String) { _currency.value = code }
    override suspend fun setFirstRunDone(done: Boolean) {}
}
