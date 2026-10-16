package com.yourco.spend.feature.record

import androidx.activity.ComponentActivity
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SheetState
import androidx.compose.material3.SheetValue
import androidx.compose.material3.rememberStandardBottomSheetState
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performImeAction
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
import androidx.compose.runtime.rememberCoroutineScope
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
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
    override suspend fun getById(id: String): ExpenseEntity? = dayFlow.value.find { it.id == id }
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
