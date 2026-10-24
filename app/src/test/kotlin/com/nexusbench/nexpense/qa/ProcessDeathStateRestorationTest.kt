package com.nexusbench.nexpense.qa

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexusbench.nexpense.core.time.TodayObserver
import com.nexusbench.nexpense.data.db.AppDatabase
import com.nexusbench.nexpense.data.db.entity.ExpenseEntity
import com.nexusbench.nexpense.data.repo.CategoryRepositoryImpl
import com.nexusbench.nexpense.data.repo.ExpenseRepositoryImpl
import com.nexusbench.nexpense.data.repo.LearnedMappingRepositoryImpl
import com.nexusbench.nexpense.data.repo.SettingsRepositoryImpl
import com.nexusbench.nexpense.feature.insight.InsightMode
import com.nexusbench.nexpense.feature.insight.InsightViewModel
import com.nexusbench.nexpense.feature.receipt.ReceiptViewModel
import com.nexusbench.nexpense.feature.record.EntryPreview
import com.nexusbench.nexpense.feature.record.EntryViewModel
import com.nexusbench.nexpense.feature.record.RecordViewModel
import com.nexusbench.nexpense.parser.ExpenseParser
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.filterNotNull
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
import org.junit.Assert.assertNull
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ProcessDeathStateRestorationTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))
    private val todayDate = LocalDate.parse("2026-10-15")
    private val todayObserver = TodayObserver.fixed(todayDate)

    private lateinit var database: AppDatabase
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var learnedMappingRepo: LearnedMappingRepositoryImpl
    private lateinit var settingsRepo: SettingsRepositoryImpl
    private lateinit var parser: ExpenseParser

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        expenseRepo = ExpenseRepositoryImpl(database.expenseDao(), fixedClock)
        categoryRepo = CategoryRepositoryImpl(database.categoryDao())
        learnedMappingRepo = LearnedMappingRepositoryImpl(database.learnedMappingDao(), fixedClock)

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("state_restoration_settings.preferences_pb") },
        )
        settingsRepo = SettingsRepositoryImpl(testDataStore)
        parser = ExpenseParser()
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun entryViewModel_preservesDraftTextAcrossProcessDeath() = runTest(testDispatcher) {
        categoryRepo.seedDefaultsIfEmpty()
        val savedStateHandle = SavedStateHandle()

        // 1. Initial ViewModel typing draft text
        val vm1 = EntryViewModel(
            expenseParser = parser,
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        vm1.onTextChange("25 groceries")
        assertEquals("25 groceries", savedStateHandle[EntryViewModel.KEY_TYPED_TEXT])

        // 2. Simulate process death & recreation with same SavedStateHandle
        val vmRestored = EntryViewModel(
            expenseParser = parser,
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        assertEquals("25 groceries", vmRestored.text.value)
        val preview = vmRestored.preview.value
        assertTrue("Restored preview should be valid", preview is EntryPreview.Valid)
        val validPreview = preview as EntryPreview.Valid
        assertEquals(2500L, validPreview.amountMinor)
        assertEquals("groceries", validPreview.note)

        // 3. Submitting clears SavedStateHandle
        val submitResult = async(UnconfinedTestDispatcher(testScheduler)) {
            vmRestored.submitResultEvents.first()
        }
        assertTrue(vmRestored.submit())
        assertTrue(submitResult.await())
        assertEquals("", savedStateHandle[EntryViewModel.KEY_TYPED_TEXT])
    }

    @Test
    fun receiptViewModel_preservesSelectedDateAcrossProcessDeath() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()

        val vm1 = ReceiptViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        vm1.setDate("2026-05-10")
        assertEquals("2026-05-10", savedStateHandle[ReceiptViewModel.KEY_SELECTED_DATE])

        // Simulate process death & recreation
        val vmRestored = ReceiptViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vmRestored.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals("2026-05-10", vmRestored.state.value.selectedDate)
        assertEquals(LocalDate.parse("2026-05-10"), vmRestored.state.value.date)
    }

    @Test
    fun insightViewModel_preservesModeAndAnchorDateAcrossProcessDeath() = runTest(testDispatcher) {
        val savedStateHandle = SavedStateHandle()

        val vm1 = InsightViewModel(
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        vm1.setMode(InsightMode.YEAR)
        vm1.setAnchorDate(LocalDate.parse("2024-08-20"))

        assertEquals("YEAR", savedStateHandle[InsightViewModel.KEY_MODE])
        assertEquals("2024-08-20", savedStateHandle[InsightViewModel.KEY_ANCHOR])

        // Simulate process death & recreation
        val vmRestored = InsightViewModel(
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vmRestored.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        assertEquals(InsightMode.YEAR, vmRestored.state.value.mode)
        assertEquals(LocalDate.parse("2024-08-20"), vmRestored.state.value.anchorDate)
    }

    @Test
    fun recordViewModel_preservesEditingStateAcrossProcessDeath() = runTest(testDispatcher) {
        val expense = ExpenseEntity(
            id = "exp-edit-test-1",
            amountMinor = 1500L,
            currency = "USD",
            note = "sushi lunch",
            rawText = "15 sushi lunch",
            categoryId = "food",
            createdAt = 1729000000000L,
            localDate = "2026-10-15",
            updatedAt = 1729000000000L,
            deletedAt = null,
        )
        expenseRepo.add(expense)

        val savedStateHandle = SavedStateHandle()

        val vm1 = RecordViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        vm1.startEditing(expense)
        assertEquals("exp-edit-test-1", savedStateHandle[RecordViewModel.KEY_EDITING_EXPENSE_ID])

        // Simulate process death & recreation
        val vmRestored = RecordViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            clock = fixedClock,
            savedStateHandle = savedStateHandle,
            todayObserver = todayObserver,
        )

        backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            vmRestored.state.collect {}
        }
        val editingExpense = vmRestored.editingExpense.filterNotNull().first()
        assertEquals("exp-edit-test-1", editingExpense.id)
        assertEquals("sushi lunch", editingExpense.note)

        // Clear editing removes from SavedStateHandle
        vmRestored.clearEditing()
        assertNull(savedStateHandle.get<String>(RecordViewModel.KEY_EDITING_EXPENSE_ID))
    }
}
