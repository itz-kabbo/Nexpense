package com.yourco.spend.data.repo

import android.content.Context
import android.content.SharedPreferences
import androidx.appcompat.app.AppCompatDelegate
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Resolves whether dark theme should be active given the user's [ThemeMode] choice
 * and the system/device dark theme setting.
 *
 * This function is the single source of truth used by both the splash screen and
 * the main application theme to guarantee they never disagree.
 */
fun resolveIsDark(mode: ThemeMode, systemIsDark: Boolean): Boolean {
    return when (mode) {
        ThemeMode.SYSTEM -> systemIsDark
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
}

interface ThemeRepository {
    val themeMode: Flow<ThemeMode>

    /**
     * Synchronous read of the saved theme mode for cold start initialization.
     * Guaranteed never to block the main thread for more than a few milliseconds via
     * a SharedPreferences mirror.
     *
     * Returns [ThemeMode.SYSTEM] if no explicit preference has been set.
     */
    fun getThemeModeSync(): ThemeMode

    /**
     * Persists the user's theme mode selection to DataStore and mirrors it
     * synchronously to SharedPreferences for fast cold-start reads.
     */
    suspend fun setThemeMode(mode: ThemeMode)
}

@Singleton
class ThemeRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataStore: DataStore<Preferences>,
) : ThemeRepository {

    private val sharedPreferences: SharedPreferences by lazy {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
    }

    override val themeMode: Flow<ThemeMode> = dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { preferences ->
            val stored = preferences[PreferencesKeys.THEME_MODE]
            if (stored != null) {
                ThemeMode.fromString(stored)
            } else {
                // If not in DataStore, fall back to synchronous SharedPreferences mirror or SYSTEM
                getThemeModeSync()
            }
        }

    override fun getThemeModeSync(): ThemeMode {
        val stored = sharedPreferences.getString(KEY_THEME_MODE, null)
        return if (stored != null) {
            ThemeMode.fromString(stored)
        } else {
            ThemeMode.SYSTEM
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        // 1. Mirror synchronously to SharedPreferences for immediate cold-start availability
        sharedPreferences.edit()
            .putString(KEY_THEME_MODE, mode.name)
            .apply()

        // 2. Persist to DataStore
        dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }

        // 3. Update AppCompat default night mode
        val nightMode = when (mode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }

    companion object {
        const val PREFS_NAME = "nexpense_theme_prefs"
        const val KEY_THEME_MODE = "theme_mode"
    }
}
