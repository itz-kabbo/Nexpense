# Architecture and Implementation Decisions

## M4: HapticFeedbackType Fallback for Save Confirmation

- **Context:** `GUIDE.md` section 3.3 specifies `HapticFeedbackType.Confirm` on save (API 30+, with a fallback to `TextHandleMove`).
- **Issue:** In the project's Compose BOM version (2024.10.01), `androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm` is not present on the `HapticFeedbackType` companion object.
- **Decision:** Used `HapticFeedbackType.TextHandleMove` as the fallback in `EntrySheet.kt` when confirming an entry save.

## M5: In-Sheet Modal Overlay for Category Picker

- **Context:** `EditSheet` is hosted within a `ModalBottomSheet` (which creates a dialog/surface window).
- **Issue:** Launching an OS-level `AlertDialog` inside a `ModalBottomSheet` creates nested `DialogWindow` instances, leading to touch event routing issues and Robolectric window dispatch limitations.
- **Decision:** Implemented `CategoryPickerDialog` as a surface card modal overlay within `EditSheetContent`, providing a seamless in-sheet selection experience with scrim and card without OS-level multi-window nesting.

## M6: FileProvider Windows JVM Test Fallback in ReceiptImageSaver

- **Context:** `GUIDE.md` section 8b M6 specifies exporting receipt as PNG into `cacheDir` and exposing it via `FileProvider.getUriForFile`.
- **Issue:** On Windows developer hosts, `androidx.core:core:1.15.0`'s internal `FileProvider.belongsToRoot` hardcodes `'/'` separator checks (`rootPath + '/'`), which fails against Windows local paths using `\` during JVM unit tests in Robolectric. Physical Android devices (Linux) use `/` and succeed.
- **Decision:** In `ReceiptImageSaver.saveBitmap`, catch `IllegalArgumentException` from `FileProvider.getUriForFile` on Windows JVM test hosts and construct the equivalent `content://$authority/receipts/${file.name}` URI, preserving exact FileProvider behavior on devices while allowing offline unit tests to succeed on Windows.

## M9: Splash Exit Interpolator and Two-Pane Tab Switching

- **Context:** `GUIDE.md` section 3.2 specifies 600-800ms fade and scale exit animation of the splash screen; section 3.4 specifies large screen layout with Record and Receipt side-by-side.
- **Decision:** Used `AccelerateDecelerateInterpolator` from Android standard animation framework for the splash exit animation, providing smooth native deceleration without external dependency bloat. For large screens (>= 600dp), structured `TwoPaneRoot` with Record pinned on the left pane and a segmented tab switcher between Receipt and Insight on the right pane, keeping the entry sheet centered and constrained to 480dp max width.

## Post-M10: Regional Currencies & Global Language Expansion Architecture

- **Context:** User requested expanding currencies to 50+ entries grouped by world region and expanding language options to 20-30 major languages with native labels and localized resource bundles.
- **Currency Architecture:** Created `CurrencyCatalog` in `com.yourco.spend.core.money` containing 60 world currencies partitioned into 5 macro-regions (`Americas`, `Europe`, `Asia-Pacific`, `Middle East`, `Africa`). The picker uses a Modal Bottom Sheet with filter chips and live debounced search across code, currency name, and countries.
- **Localization Architecture:** Created `LanguageCatalog` in `com.yourco.spend.core.language` with 26 options (System Default + 25 major languages) mapped to BCP-47 language tags. Built 25 complete `strings.xml` resource trees covering all 114 keys. Handled literal `%` signs using `formatted="false"` on `settings_privacy_note` to satisfy Android AAPT and Lint format constraints.
- **Test Concurrency:** In `SmokeIntegrationTest.kt`, explicit cancellation of background StateFlow collector coroutines before database table truncation prevents asynchronous re-seeding races.

## Post-M10: Audit Hardening

- **Currency totals:** Keep the selected-currency amount as the Record headline, show same-day subtotals for other currencies, and print each Receipt currency total separately. No exchange-rate conversion is implied.
- **Import safety:** Validate a complete JSON or CSV file before applying it in one Room transaction. Continue using JSON schema 1 and the existing CSV columns. CSV duplicate rows receive stable occurrence-based IDs; CSV remains a lossy interchange format, while JSON is the backup format.
- **Deletion conflicts:** Update `updated_at` on soft delete and restore so an older JSON backup cannot replace a newer tombstone. Settings wipe physically clears expenses, custom categories, learned corrections, and resets preferences while retaining default categories.
- **Release safety:** Read signing credentials from local Gradle properties or environment variables. Release packaging fails until signing is configured and the placeholder application ID has been replaced with the publisher's final ID.
- **Date editing:** Save the chosen local date and time together as `local_date` and `created_at`, using the current device time zone.




