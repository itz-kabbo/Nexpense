package com.nexusbench.nexpense

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate
import com.nexusbench.nexpense.data.repo.ThemeMode
import com.nexusbench.nexpense.data.repo.ThemeRepository
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class App : Application() {

    @Inject
    lateinit var themeRepository: ThemeRepository

    override fun onCreate() {
        super.onCreate()
        // Synchronous read of saved theme mode on cold start (< 1ms, non-blocking)
        val initialMode = themeRepository.getThemeModeSync()
        val nightMode = when (initialMode) {
            ThemeMode.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
            ThemeMode.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
            ThemeMode.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        }
        AppCompatDelegate.setDefaultNightMode(nightMode)
    }
}
