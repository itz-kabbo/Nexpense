package com.nexusbench.nexpense

import android.os.Bundle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.appcompat.app.AppCompatActivity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.nexusbench.nexpense.core.designsystem.AppTheme
import com.nexusbench.nexpense.data.repo.SettingsRepository
import com.nexusbench.nexpense.data.repo.ThemeMode
import com.nexusbench.nexpense.data.repo.ThemeRepository
import com.nexusbench.nexpense.data.repo.resolveIsDark
import com.nexusbench.nexpense.feature.splash.NexpenseSplash
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    @Inject
    lateinit var settingsRepository: SettingsRepository

    @Inject
    lateinit var themeRepository: ThemeRepository

    private var isStage2FirstFrameReady = false

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        val isColdStart = (savedInstanceState == null) && sAppColdStart
        if (isColdStart) {
            // Keep Stage 1 system splash on screen until Stage 2's first frame is ready
            splashScreen.setKeepOnScreenCondition {
                !isStage2FirstFrameReady
            }
        } else {
            splashScreen.setKeepOnScreenCondition { false }
        }

        enableEdgeToEdge()
        setContent {
            val initialThemeMode = remember { themeRepository.getThemeModeSync() }
            val themeMode by settingsRepository.themeMode.collectAsStateWithLifecycle(initialValue = initialThemeMode)

            val systemIsDark = isSystemInDarkTheme()
            val isDark = remember(themeMode, systemIsDark) {
                resolveIsDark(themeMode, systemIsDark)
            }

            var showSplash by remember { mutableStateOf(isColdStart) }
            var isAppReady by remember { mutableStateOf(true) }

            LaunchedEffect(Unit) {
                isAppReady = true
            }

            AppTheme(darkTheme = isDark) {
                Box(modifier = Modifier.fillMaxSize()) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        MainScreen()
                    }

                    if (showSplash) {
                        NexpenseSplash(
                            themeMode = themeMode,
                            isAppReady = isAppReady,
                            onFirstFrameDrawn = {
                                isStage2FirstFrameReady = true
                            },
                            onSplashComplete = {
                                showSplash = false
                                sAppColdStart = false
                            },
                        )
                    }
                }
            }
        }
    }

    companion object {
        /**
         * Process-level flag ensuring animated splash only plays on cold start.
         * Warm/hot starts or activity recreations skip Stage 2.
         */
        @Volatile
        var sAppColdStart: Boolean = true
    }
}
