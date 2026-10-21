package com.yourco.spend.qa

import android.content.Context
import android.content.res.Configuration
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.yourco.spend.R
import com.yourco.spend.core.time.TodayObserver
import com.yourco.spend.data.db.AppDatabase
import com.yourco.spend.data.db.entity.CategoryEntity
import com.yourco.spend.data.export.DataImporter
import com.yourco.spend.data.export.JsonExporter
import com.yourco.spend.data.repo.CategoryRepositoryImpl
import com.yourco.spend.data.repo.ExpenseRepositoryImpl
import com.yourco.spend.data.repo.LearnedMappingRepositoryImpl
import com.yourco.spend.data.repo.SettingsRepositoryImpl
import com.yourco.spend.feature.insight.InsightMode
import com.yourco.spend.feature.insight.InsightViewModel
import com.yourco.spend.feature.receipt.ReceiptViewModel
import com.yourco.spend.feature.record.EntryViewModel
import com.yourco.spend.feature.record.RecordViewModel
import com.yourco.spend.parser.ExpenseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.util.Locale

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SmokeIntegrationTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private class MutableClock(
        private var currentInstant: Instant,
        private val zone: ZoneId = ZoneId.of("UTC"),
    ) : Clock() {
        override fun getZone(): ZoneId = zone
        override fun withZone(zone: ZoneId?): Clock = MutableClock(currentInstant, zone ?: this.zone)
        override fun instant(): Instant = currentInstant

        fun advanceBy(seconds: Long) {
            currentInstant = currentInstant.plusSeconds(seconds)
        }
    }

    private val clock = MutableClock(Instant.parse("2026-10-15T12:00:00Z"))
    private val todayDate = LocalDate.parse("2026-10-15")
    private val todayObserver = TodayObserver.fixed(todayDate)

    private lateinit var context: Context
    private lateinit var database: AppDatabase
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var learnedMappingRepo: LearnedMappingRepositoryImpl
    private lateinit var settingsRepo: SettingsRepositoryImpl
    private lateinit var parser: ExpenseParser
    private lateinit var jsonExporter: JsonExporter
    private lateinit var dataImporter: DataImporter

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        expenseRepo = ExpenseRepositoryImpl(database.expenseDao(), clock)
        categoryRepo = CategoryRepositoryImpl(database.categoryDao())
        learnedMappingRepo = LearnedMappingRepositoryImpl(database.learnedMappingDao(), clock)

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("smoke_settings.preferences_pb") },
        )
        settingsRepo = SettingsRepositoryImpl(testDataStore)

        parser = ExpenseParser()
        jsonExporter = JsonExporter(expenseRepo, categoryRepo, clock)
        dataImporter = DataImporter(database.expenseDao(), database.categoryDao(), clock)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun endToEndSmokeWorkflow() = runTest(testDispatcher) {
        // (a) First run category seeding
        assertEquals(0, database.categoryDao().getAll().size)
        categoryRepo.seedDefaultsIfEmpty()
        val seededCategories = database.categoryDao().getAll()
        assertEquals(9, seededCategories.size)
        assertEquals(CategoryEntity.DEFAULT_CATEGORIES.map { it.id }.toSet(), seededCategories.map { it.id }.toSet())

        // (b) Add 3 entries via EntryViewModel: "6 coffee", "18 taxi", "42 dinner"
        val entryVm = EntryViewModel(
            expenseParser = parser,
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            settingsRepository = settingsRepo,
            clock = clock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )

        entryVm.onTextChange("6 coffee")
        val submitResult1 = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            entryVm.submitResultEvents.first()
        }
        val submitted1 = entryVm.submit()
        assertTrue("Submit '6 coffee' failed", submitted1)
        clock.advanceBy(10)
        assertTrue("Saving '6 coffee' failed", submitResult1.await())

        entryVm.onTextChange("18 taxi")
        val submitResult2 = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            entryVm.submitResultEvents.first()
        }
        val submitted2 = entryVm.submit()
        assertTrue("Submit '18 taxi' failed", submitted2)
        clock.advanceBy(10)
        assertTrue("Saving '18 taxi' failed", submitResult2.await())

        entryVm.onTextChange("42 dinner")
        val submitResult3 = backgroundScope.async(UnconfinedTestDispatcher(testScheduler)) {
            entryVm.submitResultEvents.first()
        }
        val submitted3 = entryVm.submit()
        assertTrue("Submit '42 dinner' failed", submitted3)
        clock.advanceBy(10)
        assertTrue("Saving '42 dinner' failed", submitResult3.await())

        // (c) Today list shows 3 entries and total of 6600 ($66.00)
        val recordVm = RecordViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            clock = clock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )

        val recordJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            recordVm.state.collect {}
        }

        val recordState = recordVm.state.first { it.expenses.size == 3 && it.totalMinor == 6600L }
        assertEquals(3, recordState.expenses.size)
        assertEquals(6600L, recordState.totalMinor)
        assertEquals("$66.00", recordState.formattedTotal)

        // (d) Receipt screen shows 3 items and matching sum of 6600
        val receiptVm = ReceiptViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            clock = clock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )

        val receiptJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            receiptVm.state.collect {}
        }

        val receiptState = receiptVm.state.first { it.items.size == 3 && it.totalMinor == 6600L }
        assertEquals(3, receiptState.items.size)
        assertEquals(6600L, receiptState.totalMinor)
        assertEquals("$66.00", receiptState.formattedTotal)
        // Verify chronological order (oldest first: coffee -> taxi -> dinner)
        assertEquals("coffee", receiptState.items[0].note)
        assertEquals(600L, receiptState.items[0].amountMinor)
        assertEquals("taxi", receiptState.items[1].note)
        assertEquals(1800L, receiptState.items[1].amountMinor)
        assertEquals("dinner", receiptState.items[2].note)
        assertEquals(4200L, receiptState.items[2].amountMinor)

        // (e) Insight screen shows Month/Day total of 6600 and category breakdown summing to 6600
        val insightVm = InsightViewModel(
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            clock = clock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )

        val insightJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            insightVm.state.collect {}
        }

        // Mode DAY
        insightVm.setMode(InsightMode.DAY)
        val dayState = insightVm.state.first { it.mode == InsightMode.DAY && it.totalMinor == 6600L }
        assertEquals(6600L, dayState.totalMinor)

        // Mode MONTH
        insightVm.setMode(InsightMode.MONTH)
        val monthState = insightVm.state.first { it.mode == InsightMode.MONTH && it.totalMinor == 6600L && it.categories.size == 2 }
        assertEquals(6600L, monthState.totalMinor)
        assertEquals(2, monthState.categories.size)

        val foodCategory = monthState.categories.find { it.categoryId == CategoryEntity.CATEGORY_FOOD }
        assertNotNull(foodCategory)
        assertEquals(4800L, foodCategory?.totalMinor) // 600 + 4200

        val transportCategory = monthState.categories.find { it.categoryId == CategoryEntity.CATEGORY_TRANSPORT }
        assertNotNull(transportCategory)
        assertEquals(1800L, transportCategory?.totalMinor)

        val categorySum = monthState.categories.sumOf { it.totalMinor }
        assertEquals(6600L, categorySum)

        // (f) JSON export produces valid schema 1 with 3 expenses
        val exportedJson = jsonExporter.exportJson()
        assertTrue(exportedJson.contains("\"schema\": 1"))
        assertTrue(exportedJson.contains("\"coffee\""))
        assertTrue(exportedJson.contains("\"taxi\""))
        assertTrue(exportedJson.contains("\"dinner\""))

        // Cancel background collectors before database wipe so that background flows don't attempt
        // to re-seed categories or process database truncation concurrently
        recordJob.cancel()
        receiptJob.cancel()
        insightJob.cancel()
        testDispatcher.scheduler.advanceUntilIdle()

        // (g) DB wipe
        database.clearAllTables()
        assertEquals(0, database.expenseDao().getAll().size)
        assertEquals(0, database.categoryDao().getAll().size)

        // (h) JSON import restores 3 identical expenses
        val importResult = dataImporter.importJson(exportedJson)
        assertTrue(importResult.success)
        assertEquals(3, importResult.expensesImported)

        val restoredExpenses = database.expenseDao().getAll()
        assertEquals(3, restoredExpenses.size)
        val restoredNotes = restoredExpenses.map { it.note }.toSet()
        assertEquals(setOf("coffee", "taxi", "dinner"), restoredNotes)
        assertEquals(6600L, restoredExpenses.sumOf { it.amountMinor })

        // (i) Switch language to "zh-TW" and verify localized category name resolution
        val config = Configuration(context.resources.configuration)
        config.setLocale(Locale.forLanguageTag("zh-TW"))
        val localizedContext = context.createConfigurationContext(config)

        for (expense in restoredExpenses) {
            val resId = CategoryEntity.defaultNameRes(expense.categoryId)
            assertNotNull(resId)
            val localizedName = localizedContext.getString(resId!!)
            when (expense.categoryId) {
                CategoryEntity.CATEGORY_FOOD -> assertEquals("餐飲", localizedName)
                CategoryEntity.CATEGORY_TRANSPORT -> assertEquals("交通", localizedName)
            }
        }
    }
}
