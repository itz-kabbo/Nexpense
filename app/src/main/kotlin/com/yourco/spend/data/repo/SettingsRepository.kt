package com.yourco.spend.data.repo

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
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
}

@Singleton
class SettingsRepositoryImpl @Inject constructor(
    private val dataStore: DataStore<Preferences>,
) : SettingsRepository {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val LANGUAGE_TAG = stringPreferencesKey("language_tag")
        val CURRENCY_CODE = stringPreferencesKey("currency_code")
        val FIRST_RUN_DONE = booleanPreferencesKey("first_run_done")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            ThemeMode.fromString(preferences[PreferencesKeys.THEME_MODE])
        }

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
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
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
