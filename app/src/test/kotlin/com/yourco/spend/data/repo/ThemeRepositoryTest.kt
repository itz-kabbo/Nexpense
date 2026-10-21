package com.yourco.spend.data.repo

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.test.core.app.ApplicationProvider
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class ThemeRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)
    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        // Clear test preferences before each run
        context.getSharedPreferences(ThemeRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    @Test
    fun `resolveIsDark correctly evaluates all mode and system combinations`() {
        // SYSTEM follows device setting
        assertTrue(resolveIsDark(ThemeMode.SYSTEM, systemIsDark = true))
        assertFalse(resolveIsDark(ThemeMode.SYSTEM, systemIsDark = false))

        // LIGHT is always false regardless of system setting
        assertFalse(resolveIsDark(ThemeMode.LIGHT, systemIsDark = true))
        assertFalse(resolveIsDark(ThemeMode.LIGHT, systemIsDark = false))

        // DARK is always true regardless of system setting
        assertTrue(resolveIsDark(ThemeMode.DARK, systemIsDark = true))
        assertTrue(resolveIsDark(ThemeMode.DARK, systemIsDark = false))
    }

    @Test
    fun `first launch without stored preference defaults to SYSTEM synchronously`() {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_theme_1.preferences_pb") },
        )
        val repository = ThemeRepositoryImpl(context, testDataStore)

        // Must read SYSTEM synchronously on cold start with no stored pref
        assertEquals(ThemeMode.SYSTEM, repository.getThemeModeSync())

        // Verify no preference was written to SharedPreferences prior to explicit user choice
        val prefs = context.getSharedPreferences(ThemeRepositoryImpl.PREFS_NAME, Context.MODE_PRIVATE)
        assertFalse(prefs.contains(ThemeRepositoryImpl.KEY_THEME_MODE))
    }

    @Test
    fun `explicit theme selection persists to DataStore and mirrors to SharedPreferences`() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_theme_2.preferences_pb") },
        )
        val repository = ThemeRepositoryImpl(context, testDataStore)

        repository.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())

            repository.setThemeMode(ThemeMode.DARK)
            assertEquals(ThemeMode.DARK, awaitItem())
            assertEquals(ThemeMode.DARK, repository.getThemeModeSync())

            repository.setThemeMode(ThemeMode.LIGHT)
            assertEquals(ThemeMode.LIGHT, awaitItem())
            assertEquals(ThemeMode.LIGHT, repository.getThemeModeSync())

            repository.setThemeMode(ThemeMode.SYSTEM)
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            assertEquals(ThemeMode.SYSTEM, repository.getThemeModeSync())

            cancelAndIgnoreRemainingEvents()
        }
    }
}
