package com.nexusbench.nexpense.feature.settings

import com.nexusbench.nexpense.data.db.dao.BucketTotal
import com.nexusbench.nexpense.data.db.dao.CategoryTotal
import com.nexusbench.nexpense.data.db.entity.CategoryEntity
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import com.nexusbench.nexpense.data.export.CsvExporter
import com.nexusbench.nexpense.data.export.DataImporter
import com.nexusbench.nexpense.data.export.ImportResult
import com.nexusbench.nexpense.data.export.JsonExporter
import com.nexusbench.nexpense.data.repo.CategoryRepository
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
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SettingsViewModelTest {

    private val testDispatcher = StandardTestDispatcher()
    private lateinit var fakeSettingsRepo: FakeSettingsRepository
    private lateinit var fakeCategoryRepo: FakeCategoryRepository
    private lateinit var fakeExpenseRepo: FakeExpenseRepository
    private lateinit var jsonExporter: JsonExporter
    private lateinit var csvExporter: CsvExporter
    private lateinit var dataImporter: FakeDataImporter
    private lateinit var viewModel: SettingsViewModel

    private val fixedClock = Clock.fixed(Instant.ofEpochMilli(1729000000000L), ZoneId.of("UTC"))

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        fakeSettingsRepo = FakeSettingsRepository()
        fakeCategoryRepo = FakeCategoryRepository()
        fakeExpenseRepo = FakeExpenseRepository()

        jsonExporter = JsonExporter(fakeExpenseRepo, fakeCategoryRepo, fixedClock)
        csvExporter = CsvExporter(fakeExpenseRepo, fixedClock)
        dataImporter = FakeDataImporter(fixedClock)

        viewModel = SettingsViewModel(
            settingsRepository = fakeSettingsRepo,
            categoryRepository = fakeCategoryRepo,
            expenseRepository = fakeExpenseRepo,
            dataImporter = dataImporter as DataImporter,
            jsonExporter = jsonExporter,
            csvExporter = csvExporter,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun themeMode_canBeUpdated() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.SYSTEM, viewModel.state.value.themeMode)

        viewModel.setThemeMode(ThemeMode.DARK)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.DARK, viewModel.state.value.themeMode)

        viewModel.setThemeMode(ThemeMode.LIGHT)
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(ThemeMode.LIGHT, viewModel.state.value.themeMode)
    }

    @Test
    fun languageTag_canBeUpdated() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("", viewModel.state.value.languageTag)

        viewModel.setLanguageTag("zh-TW")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("zh-TW", viewModel.state.value.languageTag)

        viewModel.setLanguageTag("en")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("en", viewModel.state.value.languageTag)
    }

    @Test
    fun currencyCode_canBeUpdated() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("USD", viewModel.state.value.currencyCode)

        viewModel.setCurrencyCode("TWD")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("TWD", viewModel.state.value.currencyCode)

        viewModel.setCurrencyCode("EUR")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals("EUR", viewModel.state.value.currencyCode)
    }

    @Test
    fun categoryManagement_renameHideAndAdd() = runTest(testDispatcher) {
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            viewModel.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(9, viewModel.state.value.categories.size)

        // Rename category
        viewModel.renameCategory(CategoryEntity.CATEGORY_FOOD, "Food & Drinks Special")
        testDispatcher.scheduler.advanceUntilIdle()
        val foodCat = viewModel.state.value.categories.find { it.id == CategoryEntity.CATEGORY_FOOD }
        assertEquals("Food & Drinks Special", foodCat?.name)

        // Toggle category hidden
        viewModel.toggleCategoryHidden(CategoryEntity.CATEGORY_FOOD, currentHidden = false)
        testDispatcher.scheduler.advanceUntilIdle()
        val foodHidden = viewModel.state.value.categories.find { it.id == CategoryEntity.CATEGORY_FOOD }
        assertTrue(foodHidden?.isHidden == true)

        // Add custom category
        viewModel.addCustomCategory("Subscription")
        testDispatcher.scheduler.advanceUntilIdle()
        assertEquals(10, viewModel.state.value.categories.size)
        val custom = viewModel.state.value.categories.find { it.name == "Subscription" }
        assertNotNull(custom)
        assertFalse(custom?.isBuiltin ?: true)
    }

    @Test
    fun exportAndImport_delegatesProperly() = runTest {
        fakeExpenseRepo.add(
            ExpenseEntity(
                id = "exp-1",
                amountMinor = 1200L,
                currency = "USD",
                note = "Test",
                rawText = "12 Test",
                categoryId = "food",
                createdAt = 1000L,
                localDate = "2026-10-15",
                updatedAt = 1000L,
                deletedAt = null,
            )
        )

        // Test exportJson
        val jsonOut = ByteArrayOutputStream()
        viewModel.exportJson(jsonOut)
        val jsonStr = jsonOut.toString(Charsets.UTF_8.name())
        assertTrue(jsonStr.contains("\"schema\": 1"))
        assertTrue(jsonStr.contains("exp-1"))

        // Test exportCsv
        val csvOut = ByteArrayOutputStream()
        viewModel.exportCsv(csvOut)
        val csvStr = csvOut.toString(Charsets.UTF_8.name())
        assertTrue(csvStr.contains("date,time,amount,currency,category,note"))
        assertTrue(csvStr.contains("12.00,USD,food,Test"))

        // Test importData
        val result = viewModel.importData(ByteArrayInputStream("sample content".toByteArray()))
        assertTrue(result.success)
        assertEquals(5, result.expensesImported)
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
        override suspend fun updateNameOverride(id: String, nameOverride: String?) {
            val list = categories.value.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(nameOverride = nameOverride)
                categories.value = list
            }
        }
        override suspend fun setHidden(id: String, isHidden: Boolean) {
            val list = categories.value.toMutableList()
            val idx = list.indexOfFirst { it.id == id }
            if (idx >= 0) {
                list[idx] = list[idx].copy(isHidden = isHidden)
                categories.value = list
            }
        }
        override suspend fun upsert(category: CategoryEntity) {
            val list = categories.value.toMutableList()
            val idx = list.indexOfFirst { it.id == category.id }
            if (idx >= 0) list[idx] = category else list.add(category)
            categories.value = list
        }
    }

    private class FakeExpenseRepository : ExpenseRepository {
        private val list = mutableListOf<ExpenseEntity>()
        override suspend fun add(expense: ExpenseEntity) { list.add(expense) }
        override suspend fun update(expense: ExpenseEntity) {}
        override suspend fun softDelete(id: String, now: Long?) {}
        override suspend fun restore(id: String) {}
        override fun observeDay(date: String): Flow<List<ExpenseEntity>> = flowOf(list)
        override fun observeTotal(from: String, to: String, currency: String): Flow<Long> = flowOf(list.sumOf { it.amountMinor })
        override fun observeByCategory(from: String, to: String, currency: String): Flow<List<CategoryTotal>> = flowOf(emptyList())
        override fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<BucketTotal>> = flowOf(emptyList())
        override suspend fun getById(id: String): ExpenseEntity? = list.find { it.id == id }
        override suspend fun getAll(): List<ExpenseEntity> = list
    }

    private class FakeDataImporter(clock: Clock) : DataImporter(
        expenseDao = object : com.nexusbench.nexpense.data.db.dao.ExpenseDao {
            override suspend fun upsert(e: ExpenseEntity) {}
            override suspend fun upsertAll(entities: List<ExpenseEntity>) {}
            override suspend fun upsertCategories(categories: List<CategoryEntity>) {}
            override suspend fun deleteAllExpenses() {}
            override suspend fun deleteCustomCategories() {}
            override suspend fun deleteLearnedMappings() {}
            override suspend fun softDelete(id: String, now: Long) {}
            override suspend fun restore(id: String, now: Long) {}
            override fun observeDay(date: String): Flow<List<ExpenseEntity>> = flowOf(emptyList())
            override fun observeDailyCurrencyTotals(date: String): Flow<List<com.nexusbench.nexpense.data.db.dao.CurrencyTotal>> = flowOf(emptyList())
            override fun observeDayWithCategoryNames(date: String): Flow<List<com.nexusbench.nexpense.data.db.dao.ExpenseWithCategoryName>> = flowOf(emptyList())
            override fun observeTotal(from: String, to: String, currency: String): Flow<Long> = flowOf(0L)
            override fun observeByCategory(from: String, to: String, currency: String): Flow<List<com.nexusbench.nexpense.data.db.dao.CategoryTotal>> = flowOf(emptyList())
            override fun observeBuckets(from: String, to: String, len: Int, currency: String): Flow<List<com.nexusbench.nexpense.data.db.dao.BucketTotal>> = flowOf(emptyList())
            override suspend fun getById(id: String): ExpenseEntity? = null
            override suspend fun getAll(): List<ExpenseEntity> = emptyList()
        },
        categoryDao = object : com.nexusbench.nexpense.data.db.dao.CategoryDao {
            override fun observeAll(): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override fun observeActive(): Flow<List<CategoryEntity>> = flowOf(emptyList())
            override suspend fun getAll(): List<CategoryEntity> = emptyList()
            override suspend fun upsert(category: CategoryEntity) {}
            override suspend fun upsertAll(categories: List<CategoryEntity>) {}
            override suspend fun getById(id: String): CategoryEntity? = null
            override suspend fun count(): Int = 0
            override suspend fun updateNameOverride(id: String, nameOverride: String?) {}
            override suspend fun setHidden(id: String, isHidden: Boolean) {}
        },
        clock = clock,
    ) {
        override suspend fun importFromStream(inputStream: java.io.InputStream): ImportResult {
            return ImportResult(expensesImported = 5, categoriesImported = 0, success = true)
        }
    }
}
