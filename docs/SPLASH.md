# Nexpense Splash & Theme Architecture

## 1. Overview
The splash experience in Nexpense is a production-grade, two-stage system designed for seamless brand entry with zero flashes, theme-awareness, and calm financial motion restraint.

---

## 2. Architecture: Two-Stage Handoff

```
+--------------------------------------------------------------+
| Stage 1: System Splash (androidx.core.splashscreen)          |
| - Rendered by OS from XML theme (values/ & values-night/)    |
| - Background: #FFFFFF (Light) or #0B0B0C (Dark)              |
| - Icon: Fully transparent drawable (@drawable/splash_transparent_icon)
| - Held until Stage 2 reports its first frame is rendered     |
+--------------------------------------------------------------+
                              |
                              v (Zero-flash handoff)
+--------------------------------------------------------------+
| Stage 2: Animated In-App Splash (NexpenseSplash Composable)  |
| - Mounts matching the exact OS background color              |
| - Resolves chosen ThemeMode (SYSTEM, LIGHT, DARK)            |
| - If theme mismatch: 200ms FastOutSlowInEasing crossfade     |
| - 2.0s minimum display time (timeline shifts +200ms on mismatch)
| - Logo: Stroke draw-on, coin dot drop, staggered wordmark    |
| - Exit: 250ms fade and scale out (1.0 -> 0.96)               |
+--------------------------------------------------------------+
                              |
                              v
+--------------------------------------------------------------+
| Main App UI (MainScreen)                                     |
| - Already fully composed and rendered underneath the splash  |
+--------------------------------------------------------------+
```

### Stage 1: OS System Splash
- Rendered before any runtime/Compose code runs.
- Controlled via `Theme.Nexpense.Starting` in `values/themes.xml`, `values-night/themes.xml`, `values-v31/themes.xml`, and `values-night-v31/themes.xml`.
- Uses `@drawable/splash_transparent_icon` to avoid any static icon popping or jumping before the animated mark begins.
- `splashScreen.setKeepOnScreenCondition { !isStage2FirstFrameReady }` holds Stage 1 until Stage 2 draws frame 1.

### Stage 2: Animated Composable (`NexpenseSplash`)
- Cold start only: warm/hot starts and activity recreations skip Stage 2.
- Starts with the exact background color of the OS device theme, guaranteeing zero color flicker or flash on handoff.
- Executes the continuous stroke draw-on (`PathMeasure`), coin drop with squash/rebound, and letter-staggered wordmark reveal.

---

## 3. Theme Resolution (`resolveIsDark`)

Theme state is resolved via `resolveIsDark(mode: ThemeMode, systemIsDark: Boolean): Boolean`:

| User Setting (`ThemeMode`) | Device Theme (`systemIsDark`) | Resolved Theme (`isDark`) | Behavior |
| :--- | :--- | :--- | :--- |
| `SYSTEM` (Default) | Light (`false`) | Light (`false`) | Direct match, 0ms delay |
| `SYSTEM` (Default) | Dark (`true`) | Dark (`true`) | Direct match, 0ms delay |
| `LIGHT` | Light (`false`) | Light (`false`) | Direct match, 0ms delay |
| `LIGHT` | Dark (`true`) | Light (`false`) | **Mismatch**: 200ms crossfade dark->light |
| `DARK` | Light (`false`) | Dark (`true`) | **Mismatch**: 200ms crossfade light->dark |
| `DARK` | Dark (`true`) | Dark (`true`) | Direct match, 0ms delay |

### Synchronous Cold-Start Storage
- `ThemeRepository.getThemeModeSync()` reads synchronously from a `SharedPreferences` mirror (`nexpense_theme_prefs`) during `Application.onCreate` in `< 1ms`.
- On first install, no preference exists: defaults to `ThemeMode.SYSTEM` without writing any keys.
- When changed in Settings, both `DataStore` and `SharedPreferences` are updated, and `AppCompatDelegate.setDefaultNightMode` is dispatched immediately.

---

## 4. Brand Vectors & Asset Customization

All vector geometries are defined in `NexpenseBrandAssets`:
- **`N_STROKE_PATH_DATA`**:
  `"M 5,49 L 5,23 C 5,13.06 13.06,5 23,5 C 32.94,5 41,13.06 41,23 L 41,49"`
  A continuous single-stroke path from the bottom of the left stem, up and over the arch, and down the right stem.
- **`DOT_PATH_DATA`**:
  `"M 23,34 m -6.5,0 a 6.5,6.5 0 1,1 13,0 a 6.5,6.5 0 1,1 -13,0"`
  A 13-unit diameter circle centered at `(23, 34)` in the lower arch counter.
- **Stroke Width**: 10 units with `StrokeCap.Round` and `StrokeJoin.Round`.

### How to Swap the Logo Mark
1. Update `NexpenseBrandAssets.createNStrokePath()` with the new path coordinates.
2. Update `NexpenseBrandAssets.VIEWBOX_WIDTH` and `VIEWBOX_HEIGHT` to match the new viewBox.
3. If changing stroke or dot dimensions, adjust `STROKE_WIDTH` and `DOT_RADIUS`.

---

## 5. Timing & Animation Configuration

All constants are exposed in `SplashTimings`:

```kotlin
object SplashTimings {
    const val holdBlankEnd: Long = 100L          // Initial continuity pause
    const val drawOnStart: Long = 100L           // "n" stroke draw-on begins
    const val drawOnEnd: Long = 850L             // "n" stroke completes (750ms duration)
    const val dotDropStart: Long = 750L          // Coin dot drops into arch
    const val dotDropEnd: Long = 1150L           // Impact, squash & rebound settle
    const val wordmarkStart: Long = 1050L        // Staggered letters begin rising
    const val wordmarkEnd: Long = 1500L          // Wordmark fully visible
    const val wordmarkLetterStagger: Long = 25L  // Per-letter delay (8 letters)
    const val holdEnd: Long = 2000L              // Complete brand stillness
    const val minDisplayMs: Long = 2000L         // Hard floor before exit can begin
    const val exitDuration: Long = 250L          // Group fade and scale (1.0 -> 0.96)
    const val themeCrossfadeMs: Long = 200L      // Crossfade duration on theme mismatch
    const val reducedMotionHoldMs: Long = 600L   // Static logo hold for reduced motion
    const val reducedMotionFadeMs: Long = 200L   // Fade duration for reduced motion
}
```

### Color Tokens (`SplashPalette`)
- **Light Theme**:
  - `background`: `#FFFFFF`
  - `content`: `#111111`
  - Accent: Soft 12% contact shadow under dot on landing (fades over 200ms).
- **Dark Theme**:
  - `background`: `#0B0B0C`
  - `content`: `#F5F5F5` (off-white prevents OLED halation and glare)
  - Accents:
    - Glowing pen tip leading the stroke during draw-on (radius ~6dp, 35% alpha, fades by t=850ms).
    - Luminous ripple ring emitted on impact (scale 1.0 -> 2.2, stroke 1.5dp, alpha 40% -> 0% over 350ms).
  - Final frame: Pure flat `#F5F5F5` with no residual glow or blur.

---

## 6. Manual Test Checklist

- [ ] **Android Versions**:
  - [ ] Android 8.0 (API 26) - Verify core splashscreen compatibility and vector rendering.
  - [ ] Android 11 (API 30) - Verify WindowInsets and smooth Activity transition.
  - [ ] Android 12 & 13 (API 31-33) - Verify OS SplashScreen API handoff with transparent icon.
  - [ ] Android 14 & 15+ (API 34-36) - Verify predictive back, edge-to-edge insets, and high refresh rate (120Hz).
- [ ] **First Launch**:
  - [ ] Device set to Light: Splash displays pure white (`#FFFFFF`) with dark `#111111` mark.
  - [ ] Device set to Dark: Splash displays obsidian (`#0B0B0C`) with luminous `#F5F5F5` mark.
- [ ] **Theme Mismatch Handling**:
  - [ ] Device Dark + in-app set to Light: Starts in dark background, smoothly crossfades to white over 200ms, then animates mark.
  - [ ] Device Light + in-app set to Dark: Starts in light background, smoothly crossfades to obsidian over 200ms, then animates mark.
- [ ] **System Theme Dynamic Updates**:
  - [ ] Mode set to `SYSTEM`: changing system dark theme in Quick Settings immediately updates app UI and is respected on next cold start.
- [ ] **Timing & Performance**:
  - [ ] Verify exit transition does not start before `t = 2000ms` (or `t = 2200ms` on mismatch).
  - [ ] Total cold start time is ~2.25s on fast device.
  - [ ] Warm / hot start: App returns directly to `MainScreen` with zero Stage 2 splash.
- [ ] **Accessibility (Reduced Motion)**:
  - [ ] Enable "Remove animations" in Android Accessibility settings (`ANIMATOR_DURATION_SCALE = 0`).
  - [ ] Splash immediately shows the static final logo, holds for 600ms, and fades into the app over 200ms.
- [ ] **Recreation & Edge Cases**:
  - [ ] Rotate device during splash: does not crash, layout remains centered in safe insets.
  - [ ] Kill process in background: cold start restarts animation cleanly from t = 0.
