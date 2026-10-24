package com.nexusbench.nexpense.core.designsystem

import androidx.activity.ComponentActivity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.github.takahirom.roborazzi.ExperimentalRoborazziApi
import com.github.takahirom.roborazzi.RobolectricDeviceQualifiers
import com.github.takahirom.roborazzi.RoborazziOptions
import com.github.takahirom.roborazzi.RoborazziTaskType
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

/**
 * Roborazzi screenshot test recording ComponentGalleryScreen in light, dark, and 2.0 font scale.
 * Verified during ./gradlew testDebugUnitTest.
 */
@OptIn(ExperimentalRoborazziApi::class)
@RunWith(RobolectricTestRunner::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(sdk = [34], qualifiers = RobolectricDeviceQualifiers.Pixel4a)
class ComponentGalleryScreenshotTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val recordOptions = RoborazziOptions(taskType = RoborazziTaskType.Record)

    @Test
    fun captureGallery_light() {
        composeTestRule.setContent {
            AppTheme(darkTheme = false) {
                ComponentGalleryScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/gallery_light.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureGallery_dark() {
        composeTestRule.setContent {
            AppTheme(darkTheme = true) {
                ComponentGalleryScreen()
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/gallery_dark.png",
            roborazziOptions = recordOptions,
        )
    }

    @Test
    fun captureGallery_fontScale2() {
        composeTestRule.setContent {
            val currentDensity = LocalDensity.current
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = currentDensity.density,
                    fontScale = 2.0f,
                ),
            ) {
                AppTheme(darkTheme = false) {
                    ComponentGalleryScreen()
                }
            }
        }
        composeTestRule.onRoot().captureRoboImage(
            filePath = "build/outputs/roborazzi/gallery_font_scale_2.png",
            roborazziOptions = recordOptions,
        )
    }
}
