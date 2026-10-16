package com.yourco.spend.feature.record

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
import androidx.compose.ui.test.performTextClearance
import androidx.compose.ui.test.performTextInput
import com.yourco.spend.core.designsystem.AppTheme
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
import com.yourco.spend.parser.ExpenseParser
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.time.Clock
import java.time.Instant
import java.time.ZoneId

@OptIn(ExperimentalMaterial3Api::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34], qualifiers = com.github.takahirom.roborazzi.RobolectricDeviceQualifiers.Pixel4a)
class RecordScreenUiTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val fixedClock = Clock.fixed(Instant.parse("2026-10-15T12:00:00Z"), ZoneId.of("UTC"))
    private lateinit var fakeExpenseRepo: UiFakeExpenseRepo
    private lateinit var fakeCategoryRepo: UiFakeCategoryRepo
    private lateinit var fakeLearnedMappingRepo: UiFakeLearnedMappingRepo
    private lateinit var fakeSettingsRepo: UiFakeSettingsRepo
    private lateinit var recordVm: RecordViewModel
    private lateinit var entryVm: EntryViewModel

    @Before
    fun setUp() {
        fakeExpenseRepo = UiFakeExpenseRepo()
        fakeCategoryRepo = UiFakeCategoryRepo()
        fakeLearnedMappingRepo = UiFakeLearnedMappingRepo()
        fakeSettingsRepo = UiFakeSettingsRepo()

        recordVm = RecordViewModel(
            expenseRepository = fakeExpenseRepo,
            settingsRepository = fakeSettingsRepo,
            categoryRepository = fakeCategoryRepo,
            learnedMappingRepository = fakeLearnedMappingRepo,
            clock = fixedClock,
        )
        entryVm = EntryViewModel(
            expenseParser = ExpenseParser(),
            expenseRepository = fakeExpenseRepo,
            categoryRepository = fakeCategoryRepo,
            learnedMappingRepository = fakeLearnedMappingRepo,
            settingsRepository = fakeSettingsRepo,
            clock = fixedClock,
        )
    }

    @Test
    fun doneWhen_expandSheet_typeCoffee_assertPreview_pressDone_findsRowInList() {
        var sheetState: SheetState? = null
        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            val state = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
            sheetState = state
            scope = rememberCoroutineScope()
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                    sheetState = state,
                )
            }
        }

        // Initially empty state is visible
        composeTestRule.onNodeWithText("Nothing yet. Swipe up.").assertIsDisplayed()

        // 1. Expand the sheet
        composeTestRule.runOnIdle {
            scope?.launch {
                sheetState?.expand()
            }
        }
        composeTestRule.waitForIdle()

        // 2. Type "6 coffee"
        composeTestRule.onNode(hasSetTextAction()).performTextInput("6 coffee")
        composeTestRule.waitForIdle()

        // 3. Sees "6.00 · coffee · Food & Drink" in the preview
        composeTestRule.onNodeWithText("6.00 · coffee · Food & Drink", substring = true)
            .assertIsDisplayed()

        // 4. Press Done
        composeTestRule.onNode(hasSetTextAction()).performImeAction()
        composeTestRule.waitForIdle()

        // 5. Finds the new row in the list
        composeTestRule.onNodeWithText("coffee").assertIsDisplayed()
        composeTestRule.onNodeWithText("Food & Drink").assertIsDisplayed()
        composeTestRule.onNodeWithText("$6.00", substring = true).assertIsDisplayed()
    }

    @Test
    fun categoryPicker_allowsChangingCategory_andUpdatesPreview() {
        var sheetState: SheetState? = null
        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            val state = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
            sheetState = state
            scope = rememberCoroutineScope()
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                    sheetState = state,
                )
            }
        }

        composeTestRule.runOnIdle {
            scope?.launch {
                sheetState?.expand()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("15 dinner")
        composeTestRule.waitForIdle()

        // Pick "fun" category
        entryVm.onCategoryPicked("fun")
        composeTestRule.waitForIdle()

        // Preview should now show Fun
        composeTestRule.onNodeWithText("15.00 · dinner · Fun", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun doneWhen_learningLoop_type6Boba_correctToGroceries_type9Boba_seesGroceries() {
        var sheetState: SheetState? = null
        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            val state = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
            sheetState = state
            scope = rememberCoroutineScope()
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                    sheetState = state,
                )
            }
        }

        // 1. Expand sheet and type "6 boba"
        composeTestRule.runOnIdle {
            scope?.launch {
                sheetState?.expand()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("6 boba")
        composeTestRule.waitForIdle()

        // Preview initially predicts "Food & Drink"
        composeTestRule.onNodeWithText("6.00 · boba · Food & Drink", substring = true)
            .assertIsDisplayed()

        // 2. Correct category to "groceries"
        entryVm.onCategoryPicked("groceries")
        composeTestRule.waitForIdle()

        // Preview now shows Groceries
        composeTestRule.onNodeWithText("6.00 · boba · Groceries", substring = true)
            .assertIsDisplayed()

        // 3. Press Done to save entry
        composeTestRule.onNode(hasSetTextAction()).performImeAction()
        composeTestRule.waitForIdle()

        // Assert learned mapping was recorded
        runBlocking {
            assertEquals("groceries", fakeLearnedMappingRepo.getMapping("boba"))
        }

        // 4. Expand sheet again and type "9 boba"
        composeTestRule.runOnIdle {
            scope?.launch {
                sheetState?.expand()
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNode(hasSetTextAction()).performTextInput("9 boba")
        composeTestRule.waitForIdle()

        // 5. Assert preview category is immediately Groceries!
        composeTestRule.onNodeWithText("9.00 · boba · Groceries", substring = true)
            .assertIsDisplayed()
    }

    @Test
    fun editExpense_tapRow_changeFields_save_updatesRepositoryAndUi() {
        // Seed initial expense
        val expense = ExpenseEntity(
            id = "e1",
            amountMinor = 600,
            currency = "USD",
            note = "coffee",
            rawText = "6 coffee",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = "2026-10-15",
            updatedAt = fixedClock.millis(),
        )
        runBlocking {
            fakeExpenseRepo.add(expense)
        }

        composeTestRule.setContent {
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                )
            }
        }

        // Initially "coffee" is displayed
        composeTestRule.onNodeWithText("coffee").assertIsDisplayed()

        // Tap the row to open EditSheet
        composeTestRule.onNodeWithText("coffee").performClick()
        composeTestRule.waitForIdle()

        // EditSheet should be displayed
        composeTestRule.onNodeWithText("Edit Expense").assertIsDisplayed()

        // Modify note and amount via recordVm to verify save updates UI
        val updated = expense.copy(
            amountMinor = 750,
            note = "latte",
            categoryId = "fun",
        )
        recordVm.updateExpense(updated)
        composeTestRule.waitForIdle()

        // In repository, expense should be updated
        val retrieved = runBlocking { fakeExpenseRepo.getById("e1") }
        assertNotNull(retrieved)
        assertEquals(750L, retrieved?.amountMinor)
        assertEquals("latte", retrieved?.note)
        assertEquals("fun", retrieved?.categoryId)

        // In UI, row should now show "latte" and "Fun"
        composeTestRule.onNodeWithText("latte").assertIsDisplayed()
        composeTestRule.onNodeWithText("Fun").assertIsDisplayed()
        composeTestRule.onNodeWithText("$7.50", substring = true).assertIsDisplayed()
    }

    @Test
    fun deleteExpense_andUndo_restoresExpense() {
        val expense = ExpenseEntity(
            id = "e1",
            amountMinor = 1200,
            currency = "USD",
            note = "lunch",
            rawText = "12 lunch",
            categoryId = "food",
            createdAt = fixedClock.millis(),
            localDate = "2026-10-15",
            updatedAt = fixedClock.millis(),
        )
        runBlocking {
            fakeExpenseRepo.add(expense)
        }

        composeTestRule.setContent {
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                )
            }
        }

        composeTestRule.onNodeWithText("lunch").assertIsDisplayed()

        // Delete expense
        recordVm.deleteExpense(expense)
        composeTestRule.waitForIdle()

        // Should be empty now
        composeTestRule.onNodeWithText("Nothing yet. Swipe up.").assertIsDisplayed()

        // Undo delete
        recordVm.undoDelete("e1")
        composeTestRule.waitForIdle()

        // Should be restored
        composeTestRule.onNodeWithText("lunch").assertIsDisplayed()
    }

    @Test
    fun backPress_collapsesExpandedSheet() {
        var sheetState: SheetState? = null
        var scope: CoroutineScope? = null

        composeTestRule.setContent {
            val state = rememberStandardBottomSheetState(
                initialValue = SheetValue.PartiallyExpanded,
                skipHiddenState = true,
            )
            sheetState = state
            scope = rememberCoroutineScope()
            AppTheme {
                RecordScreen(
                    recordVm = recordVm,
                    entryVm = entryVm,
                    sheetState = state,
                )
            }
        }

        composeTestRule.runOnIdle {
            scope?.launch {
                sheetState?.expand()
            }
        }
        composeTestRule.waitForIdle()
        assertEquals(SheetValue.Expanded, sheetState?.currentValue)

        // Trigger back
        composeTestRule.activity.onBackPressedDispatcher.onBackPressed()
        composeTestRule.waitForIdle()

        // Sheet should be collapsing/collapsed to PartiallyExpanded
        val target = sheetState?.targetValue
        val current = sheetState?.currentValue
        assertTrue(target == SheetValue.PartiallyExpanded || current == SheetValue.PartiallyExpanded)
    }
}

private class UiFakeExpenseRepo : ExpenseRepository {
    val dayFlow = MutableStateFlow<List<ExpenseEntity>>(emptyList())
    val totalFlow = MutableStateFlow(0L)
    val allExpenses = mutableListOf<ExpenseEntity>()

    override suspend fun add(expense: ExpenseEntity) {
        allExpenses.add(0, expense)
        updateFlows()
    }

    override suspend fun update(expense: ExpenseEntity) {
        val index = allExpenses.indexOfFirst { it.id == expense.id }
        if (index != -1) {
            allExpenses[index] = expense
            updateFlows()
        }
    }

    override suspend fun softDelete(id: String, now: Long?) {
        val index = allExpenses.indexOfFirst { it.id == id }
        if (index != -1) {
            allExpenses[index] = allExpenses[index].copy(deletedAt = now ?: 1000L)
            updateFlows()
        }
    }

    override suspend fun restore(id: String) {
        val index = allExpenses.indexOfFirst { it.id == id }
        if (index != -1) {
            allExpenses[index] = allExpenses[index].copy(deletedAt = null)
            updateFlows()
        }
    }

    private fun updateFlows() {
        val activeToday = allExpenses.filter { it.deletedAt == null && it.localDate == "2026-10-15" }
        dayFlow.value = activeToday
        totalFlow.value = activeToday.sumOf { it.amountMinor }
    }

    override fun observeDay(date: String): Flow<List<ExpenseEntity>> = dayFlow
    override fun observeTotal(from: String, to: String): Flow<Long> = totalFlow
    override fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>> = MutableStateFlow(emptyList())
    override fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>> = MutableStateFlow(emptyList())
    override suspend fun getById(id: String): ExpenseEntity? = allExpenses.find { it.id == id }
}

private class UiFakeCategoryRepo : CategoryRepository {
    private val categories = MutableStateFlow(CategoryEntity.DEFAULT_CATEGORIES)

    override fun observeAll(): Flow<List<CategoryEntity>> = categories
    override fun observeActive(): Flow<List<CategoryEntity>> = categories
    override suspend fun getById(id: String): CategoryEntity? = categories.value.find { it.id == id }
    override suspend fun seedDefaultsIfEmpty() {}
    override suspend fun updateNameOverride(id: String, nameOverride: String?) {}
    override suspend fun setHidden(id: String, isHidden: Boolean) {}
    override suspend fun upsert(category: CategoryEntity) {}
}

private class UiFakeLearnedMappingRepo : LearnedMappingRepository {
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

private class UiFakeSettingsRepo : SettingsRepository {
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
