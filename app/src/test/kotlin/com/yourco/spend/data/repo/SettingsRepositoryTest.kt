package com.yourco.spend.data.repo

import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import app.cash.turbine.test
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder

@OptIn(ExperimentalCoroutinesApi::class)
class SettingsRepositoryTest {

    @get:Rule
    val tmpFolder: TemporaryFolder = TemporaryFolder()

    private val testDispatcher = UnconfinedTestDispatcher()
    private val testScope = TestScope(testDispatcher)

    @Test
    fun defaultSettingsValues() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings_1.preferences_pb") },
        )
        val repository = SettingsRepositoryImpl(testDataStore)

        repository.themeMode.test {
            assertEquals(ThemeMode.SYSTEM, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.languageTag.test {
            assertEquals("", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.currencyCode.test {
            assertEquals(SettingsRepositoryImpl.defaultCurrencyCode(), awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.firstRunDone.test {
            assertFalse(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun updateSettingsValues() = runTest {
        val testDataStore = PreferenceDataStoreFactory.create(
            scope = testScope,
            produceFile = { tmpFolder.newFile("test_settings_2.preferences_pb") },
        )
        val repository = SettingsRepositoryImpl(testDataStore)

        repository.setThemeMode(ThemeMode.DARK)
        repository.themeMode.test {
            assertEquals(ThemeMode.DARK, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.setLanguageTag("zh-TW")
        repository.languageTag.test {
            assertEquals("zh-TW", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.setCurrencyCode("EUR")
        repository.currencyCode.test {
            assertEquals("EUR", awaitItem())
            cancelAndIgnoreRemainingEvents()
        }

        repository.setFirstRunDone(true)
        repository.firstRunDone.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
