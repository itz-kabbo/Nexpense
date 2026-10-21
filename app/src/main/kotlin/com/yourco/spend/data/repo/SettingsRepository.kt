package com.yourco.spend.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import java.util.Currency
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        fun fromString(value: String?): ThemeMode =
            entries.firstOrNull { it.name.equals(value, ignoreCase = true) } ?: SYSTEM
    }
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val languageTag: Flow<String>
    val currencyCode: Flow<String>
    val firstRunDone: Flow<Boolean>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setLanguageTag(tag: String)
    suspend fun setCurrencyCode(code: String)
    suspend fun setFirstRunDone(done: Boolean)
    suspend fun clearAllSettings() = Unit
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
    private val themeRepository: ThemeRepository,
) : SettingsRepository {

    constructor(dataStore: DataStore<Preferences>) : this(
        dataStore = dataStore,
        themeRepository = object : ThemeRepository {
            private val _theme = MutableStateFlow(ThemeMode.SYSTEM)
            override val themeMode: Flow<ThemeMode> = _theme
            override fun getThemeModeSync(): ThemeMode = _theme.value
            override suspend fun setThemeMode(mode: ThemeMode) { _theme.value = mode }
        },
    )

    private object PreferencesKeys {
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
    }

    override val themeMode: Flow<ThemeMode> = themeRepository.themeMode

    override val languageTag: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.LANGUAGE_TAG] ?: ""
        }

    // VERIFY: Default currency from device locale with USD fallback
    override val currencyCode: Flow<String> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] ?: defaultCurrencyCode()
        }

    override val firstRunDone: Flow<Boolean> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            preferences[PreferencesKeys.FIRST_RUN_DONE] ?: false
        }

    override suspend fun setThemeMode(mode: ThemeMode) {
        themeRepository.setThemeMode(mode)
    }

    override suspend fun setLanguageTag(tag: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.LANGUAGE_TAG] = tag
        }
    }

    override suspend fun setCurrencyCode(code: String) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.CURRENCY_CODE] = code
        }
    }

    override suspend fun setFirstRunDone(done: Boolean) {
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.FIRST_RUN_DONE] = done
        }
    }

    override suspend fun clearAllSettings() {
        dataStore.edit { preferences -> preferences.clear() }
        themeRepository.setThemeMode(ThemeMode.SYSTEM)
    }

    companion object {
        // VERIFY: Default currency from device locale with USD fallback
        fun defaultCurrencyCode(): String {
            return try {
                Currency.getInstance(Locale.getDefault()).currencyCode
            } catch (_: Exception) {
                "USD"
            }
        }
    }
}
