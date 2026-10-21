# Implementation Plan: UI Polish, Regional Currencies & 25+ Global Languages

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Fix all verified UI and contrast inconsistencies from the device screenshots, implement 50+ world currencies grouped by region in a searchable modal bottom sheet, and expand app localization to 25+ global languages with complete `strings.xml` resource bundles.

---

## Task 1: Core UI Inconsistencies & Visual Bugs Fixes

### Files touched:
- `app/src/main/kotlin/com/yourco/spend/core/designsystem/AppTheme.kt`
- `app/src/main/kotlin/com/yourco/spend/core/designsystem/components/SegmentedControl.kt`
- `app/src/main/kotlin/com/yourco/spend/feature/receipt/ReceiptScreen.kt`
- `app/src/main/kotlin/com/yourco/spend/feature/insight/InsightScreen.kt`
- `app/src/main/kotlin/com/yourco/spend/feature/record/RecordViewModel.kt`
- `app/src/main/kotlin/com/yourco/spend/feature/settings/SettingsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/test/kotlin/com/yourco/spend/feature/record/RecordViewModelTest.kt`

### Steps:
- [ ] In `AppTheme.kt`, add a `DisposableEffect(darkTheme)` to update `WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme` and `isAppearanceLightNavigationBars = !darkTheme`.
- [ ] In `SegmentedControl.kt`, update track container background to `AppTheme.colors.surfaceVariant` with `0.5.dp` hairline border, and selected indicator thumb to `AppTheme.colors.surface`.
- [ ] In `ReceiptScreen.kt` and `InsightScreen.kt`, update disabled forward stepper chevron tint from `copy(alpha = 0.25f)` to `copy(alpha = 0.45f)` for visible contrast.
- [ ] In `ReceiptScreen.kt`, redesign the top header using an anchored `Box` layout so the date title `"Tuesday, Oct 20, 2026"` is perfectly centered on the screen regardless of button counts.
- [ ] In `RecordViewModel.kt`, update `formatTodayDate` pattern from `"EEEE, MMM d"` to `"EEEE, MMM d, yyyy"`.
- [ ] Update `RecordViewModelTest.kt` to verify the updated date pattern including the year.
- [ ] In `SettingsScreen.kt`, update the outer section header to `stringResource(R.string.settings_section_preferences)` and the inner card labels to "Theme", "Language", "Currency". Add string resources to `strings.xml` and `values-zh-rTW/strings.xml`.
- [ ] In `ReceiptScreen.kt`, add a subtle 1dp border/shadow on the receipt slip card to enhance perforated edge contrast against the `#F8F8F5` background in Light Mode.
- [ ] Run `./gradlew testDebugUnitTest` and `./gradlew lintDebug` to verify all tests pass.
- [ ] Commit changes as: `fix(ui): core visual inconsistencies, status bar insets, and header alignment`.

---

## Task 2: Regional Currencies & Searchable Bottom Sheet

### Files touched:
- `core/common/src/main/kotlin/com/yourco/spend/model/CurrencyData.kt` (new)
- `app/src/main/kotlin/com/yourco/spend/feature/settings/CurrencyPickerBottomSheet.kt` (new)
- `app/src/main/kotlin/com/yourco/spend/feature/settings/SettingsScreen.kt`
- `app/src/main/res/values/strings.xml`
- `app/src/main/res/values-zh-rTW/strings.xml`
- `app/src/test/kotlin/com/yourco/spend/feature/settings/CurrencyDataTest.kt` (new)

### Steps:
- [ ] Create `CurrencyData.kt` with `CurrencyRegion` (AMERICAS, EUROPE, ASIA_PACIFIC, MIDDLE_EAST, AFRICA) and 50+ currencies with ISO codes, symbols, English display names, and fraction digits.
- [ ] Write unit test `CurrencyDataTest.kt` verifying region grouping, code lookup, and fallback handling.
- [ ] Implement `CurrencyPickerBottomSheet.kt` with a search bar, region section headers/chips, currency rows (code, name, symbol), and selection state.
- [ ] In `SettingsScreen.kt`, replace the horizontal row of 6 currencies with a currency summary row that opens `CurrencyPickerBottomSheet`.
- [ ] Add all user-facing strings (region names, search placeholder, dialog titles) to `strings.xml` and `values-zh-rTW/strings.xml`.
- [ ] Run `./gradlew testDebugUnitTest` to verify currency data and selection tests pass.
- [ ] Commit changes as: `feat(currency): regional grouped currencies with searchable bottom sheet`.

---

## Task 3: 25+ Global Languages & Searchable Bottom Sheet

### Files touched:
- `core/common/src/main/kotlin/com/yourco/spend/model/SupportedLanguages.kt` (new)
- `app/src/main/kotlin/com/yourco/spend/feature/settings/LanguagePickerBottomSheet.kt` (new)
- `app/src/main/kotlin/com/yourco/spend/feature/settings/SettingsScreen.kt`
- Localized `strings.xml` files for 24 new languages under `app/src/main/res/values-<lang>/strings.xml`:
  `zh-rCN`, `es`, `fr`, `de`, `ja`, `ko`, `pt`, `it`, `ru`, `hi`, `bn`, `ar`, `tr`, `vi`, `id`, `th`, `nl`, `pl`, `sv`, `uk`, `cs`, `el`, `he`.
- `app/src/test/kotlin/com/yourco/spend/feature/settings/LanguageDataTest.kt` (new)

### Steps:
- [ ] Create `SupportedLanguages.kt` defining the 26 language options (System Default + 25 major languages) with BCP-47 codes, native names, and English names.
- [ ] Create all 24 localized `strings.xml` files with accurate translations for all ~150 app strings, preserving string key parity.
- [ ] Write unit test `LanguageDataTest.kt` verifying language list and locale resolution.
- [ ] Implement `LanguagePickerBottomSheet.kt` with a search bar, native language names, English names, and checkmark selection.
- [ ] In `SettingsScreen.kt`, replace the 3-item SegmentedControl for Language with a language summary row that opens `LanguagePickerBottomSheet`.
- [ ] Verify locale switching via `AppCompatDelegate.setApplicationLocales`.
- [ ] Run `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- [ ] Commit changes as: `feat(l10n): expand to 25+ global languages with searchable picker`.

---

## Task 4: Full Release Build & End-to-End Verification

### Steps:
- [ ] Run `./gradlew assembleRelease` to produce release APK and AAB.
- [ ] Verify release signing with `apksigner verify`.
- [ ] Verify zero INTERNET permission in `AndroidManifest.xml`.
- [ ] Update `PARITY.md` and `docs/UI.md` to reflect the new regional currency and language capabilities.
- [ ] Commit as: `release: verified build with regional currencies, 25+ languages, and visual fixes`.
