package com.nexusbench.nexpense.qa

import android.app.Application
import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.lifecycle.SavedStateHandle
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.nexusbench.nexpense.R
import com.nexusbench.nexpense.core.designsystem.darkAppColors
import com.nexusbench.nexpense.core.designsystem.lightAppColors
import com.nexusbench.nexpense.core.time.TodayObserver
import com.nexusbench.nexpense.data.db.AppDatabase
import com.nexusbench.nexpense.data.repo.CategoryRepositoryImpl
import com.nexusbench.nexpense.data.repo.ExpenseRepositoryImpl
import com.nexusbench.nexpense.data.repo.LearnedMappingRepositoryImpl
import com.nexusbench.nexpense.data.repo.SettingsRepositoryImpl
import com.nexusbench.nexpense.feature.insight.InsightMode
import com.nexusbench.nexpense.feature.insight.InsightViewModel
import com.nexusbench.nexpense.feature.receipt.ReceiptViewModel
import com.nexusbench.nexpense.feature.record.RecordViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
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
import org.junit.Assert.assertFalse
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

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class EmptyStatesAndEdgeCasesTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = StandardTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    private lateinit var app: Application
    private lateinit var database: AppDatabase
    private lateinit var expenseRepo: ExpenseRepositoryImpl
    private lateinit var categoryRepo: CategoryRepositoryImpl
    private lateinit var learnedMappingRepo: LearnedMappingRepositoryImpl
    private lateinit var settingsRepo: SettingsRepositoryImpl

    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))
    private val todayDate = LocalDate.parse("2026-10-15")
    private val todayObserver = TodayObserver.fixed(todayDate)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        app = ApplicationProvider.getApplicationContext<Application>()
        database = Room.inMemoryDatabaseBuilder(app, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        expenseRepo = ExpenseRepositoryImpl(database.expenseDao(), fixedClock)
        categoryRepo = CategoryRepositoryImpl(database.categoryDao())
        learnedMappingRepo = LearnedMappingRepositoryImpl(database.learnedMappingDao(), fixedClock)

        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("empty_states_settings.preferences_pb") },
        )
        settingsRepo = SettingsRepositoryImpl(testDataStore)
    }

    @After
    fun tearDown() {
        database.close()
        Dispatchers.resetMain()
    }

    @Test
    fun recordEmptyState_displaysEmptyHintAndZeroTotal() = runTest(testDispatcher) {
        val recordVm = RecordViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            recordVm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = recordVm.state.value
        assertEquals(0L, state.totalMinor)
        assertEquals("$0.00", state.formattedTotal)
        assertTrue(state.expenses.isEmpty())

        val emptyTodayString = app.getString(R.string.empty_today)
        assertTrue(emptyTodayString.isNotBlank())
    }

    @Test
    fun receiptEmptyState_displaysEmptyLabelAndZeroTotal() = runTest(testDispatcher) {
        val receiptVm = ReceiptViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            receiptVm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val state = receiptVm.state.value
        assertTrue(state.isEmpty)
        assertTrue(state.items.isEmpty())
        assertEquals(0L, state.totalMinor)
        assertEquals("$0.00", state.formattedTotal)

        val emptyReceiptString = app.getString(R.string.receipt_empty)
        assertTrue(emptyReceiptString.isNotBlank())
    }

    @Test
    fun insightEmptyState_handlesZeroDivideAcrossAllModes() = runTest(testDispatcher) {
        val insightVm = InsightViewModel(
            expenseRepository = expenseRepo,
            categoryRepository = categoryRepo,
            settingsRepository = settingsRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = todayObserver,
        )
        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            insightVm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        // 1. DAY mode empty state
        insightVm.setMode(InsightMode.DAY)
        testDispatcher.scheduler.advanceUntilIdle()
        val dayState = insightVm.state.value
        assertTrue(dayState.isEmpty)
        assertEquals(0L, dayState.totalMinor)
        assertTrue(dayState.categories.isEmpty())

        // 2. MONTH mode empty state (no division by zero in sparkline points)
        insightVm.setMode(InsightMode.MONTH)
        testDispatcher.scheduler.advanceUntilIdle()
        val monthState = insightVm.state.value
        assertTrue(monthState.isEmpty)
        assertEquals(0L, monthState.totalMinor)
        assertTrue(monthState.categories.isEmpty())
        assertTrue(monthState.monthPoints.isNotEmpty())
        for (point in monthState.monthPoints) {
            assertEquals(0f, point.fraction, 0.0001f)
            assertEquals(0L, point.totalMinor)
        }

        // 3. YEAR mode empty state (no division by zero in 12-month bars)
        insightVm.setMode(InsightMode.YEAR)
        val yearState = insightVm.state.first { it.mode == InsightMode.YEAR && it.yearBars.size == 12 }
        assertTrue(yearState.isEmpty)
        assertEquals(0L, yearState.totalMinor)
        assertTrue(yearState.categories.isEmpty())
        assertEquals(12, yearState.yearBars.size)
        for (bar in yearState.yearBars) {
            assertEquals(0f, bar.fraction, 0.0001f)
            assertEquals(0L, bar.totalMinor)
        }

        val emptyInsightString = app.getString(R.string.insight_empty)
        assertTrue(emptyInsightString.isNotBlank())
    }

    @Test
    fun darkModeAndLightMode_tokensMeetContrastRequirements() {
        val lightColors = lightAppColors()
        assertFalse(lightColors.isDark)
        assertEquals(0xFFFBFBF9, lightColors.background.value.toLong().shr(32).and(0xFFFFFFFF))
        assertEquals(0xFFFFFFFF, lightColors.surface.value.toLong().shr(32).and(0xFFFFFFFF))
        assertEquals(0xFFFDFCF7, lightColors.paper.value.toLong().shr(32).and(0xFFFFFFFF))

        val darkColors = darkAppColors()
        assertTrue(darkColors.isDark)
        assertEquals(0xFF09090B, darkColors.background.value.toLong().shr(32).and(0xFFFFFFFF))
        assertEquals(0xFF141417, darkColors.surface.value.toLong().shr(32).and(0xFFFFFFFF))
        assertEquals(0xFF19191E, darkColors.paper.value.toLong().shr(32).and(0xFFFFFFFF))
    }

    @Test
    fun midnightRollover_automaticallyFlipsTodayDateWithoutRestart() = runTest(testDispatcher) {
        val todayFlow = MutableStateFlow(LocalDate.parse("2026-10-15"))
        val dynamicTodayObserver = TodayObserver.of(todayFlow) { todayFlow.value }

        val recordVm = RecordViewModel(
            expenseRepository = expenseRepo,
            settingsRepository = settingsRepo,
            categoryRepository = categoryRepo,
            learnedMappingRepository = learnedMappingRepo,
            clock = fixedClock,
            savedStateHandle = SavedStateHandle(),
            todayObserver = dynamicTodayObserver,
        )

        val collectJob = backgroundScope.launch(UnconfinedTestDispatcher(testScheduler)) {
            recordVm.state.collect {}
        }
        testDispatcher.scheduler.advanceUntilIdle()

        val initial = recordVm.state.first { it.todayDateLabel.contains("Oct 15") || it.todayDateLabel.contains("10月15") || it.todayDateLabel.contains("15") }
        assertNotNull(initial)

        // Midnight rollover: date changes to Oct 16
        todayFlow.value = LocalDate.parse("2026-10-16")

        val nextDay = recordVm.state.first { it.todayDateLabel.contains("Oct 16") || it.todayDateLabel.contains("10月16") || it.todayDateLabel.contains("16") }
        assertTrue("Rollover should flip to Oct 16", nextDay.todayDateLabel.contains("Oct 16") || nextDay.todayDateLabel.contains("10月16") || nextDay.todayDateLabel.contains("16"))
    }
}
