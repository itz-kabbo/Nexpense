# Nexpense Audit and Remediation Report

**Audit date:** 2026-10-21
**Scope:** Android app and parser source, persistence and import/export paths, build configuration, tests as written, and project documentation.
**Method:** Static review of source, persistence/import paths, build configuration, tests, and documentation. Remediation changes are included below. Build, unit tests, and lint were run after the changes.

## Summary

The audit found data integrity, persistence, release configuration, localization, and documentation issues. Source changes address the mixed-currency summaries, failed-save data loss, partial imports, tombstone timestamps, unsafe signing configuration, missing wipe controls, validation gaps, date/time consistency, custom-category names, time-zone updates, and documented feature mismatches. The publisher supplied `com.nexusbench.nexpense` as the final application ID. A signed release APK has been built and its v2 signature verified. The required debug build, unit tests, and lint passed; an additional forced stability run exposed one environment-sensitive parser benchmark miss. Screenshot regression checks remain open because no reviewed baselines are committed.

## Remediation results

| Finding | Result | Change |
| --- | --- | --- |
| Record and receipt mixed-currency totals | Resolved in source | Record shows other-currency subtotals; Receipt shows one total per currency. |
| Entry and edit forms close before save success | Resolved in source | Drafts remain until Room writes succeed; failures show a localized retry message. |
| Imports partially apply or silently skip rows | Resolved in source | JSON and CSV are validated before a single Room transaction. |
| Soft delete can be overwritten by an older backup | Resolved in source | Delete and restore advance `updated_at`. |
| Release credentials and placeholder application ID | Resolved for local release build | Credentials remain external to the repository. The publisher supplied `com.nexusbench.nexpense`; `assembleRelease` produced `app/build/outputs/apk/release/app-release.apk`, and `apksigner verify` confirmed its APK v2 signature. Confirm that this signing certificate matches the publisher's registered Play upload key before upload. |
| Privacy documentation promises an unavailable wipe | Resolved in source | Settings now offers a confirmed wipe; privacy and store text describe its actual scope. |
| Store copy said financial data never leaves the device while Android backup is enabled | Resolved in documentation | Listing now discloses that Android system backup may include local app data when enabled. |
| Backup privacy copy overstated universal encryption | Resolved in documentation | Privacy and Data Safety copy now qualify Android backup encryption by Android version, screen lock, and device settings. [Android Auto Backup documentation](https://developer.android.com/identity/data/autobackup) |
| Play Data Safety draft says “No” despite enabled Android Auto Backup | Open publisher review | The app itself does not transmit data, but OS-managed backup may transfer local data. The draft now flags the Play Console classification for publisher confirmation; see [Google Play Data Safety guidance](https://support.google.com/googleplay/android-developer/answer/10787469). |
| CSV duplicates and lossy format | Partially resolved | Same-file duplicate rows get stable occurrence IDs. CSV remains lossy by design and this is documented; JSON remains the full expense/category backup. |
| Spreadsheet formula injection through exported notes/categories | Resolved in source | CSV export prefixes formula-like text fields with an apostrophe before escaping the CSV cell. |
| Imported record validation | Resolved in source | Schema version, ID, date, timestamp, amount, currency, and category references are checked. |
| Entry category override lost after process death | Resolved in source | The selected category is stored in `SavedStateHandle`. |
| Date edit leaves old timestamp | Resolved in source | Edit UI now includes a time picker and saves `local_date` with matching `created_at`. |
| Device time-zone change ignored | Resolved in source | The injected system clock reads the current zone when used. |
| Custom category displays raw ID | Resolved in source | Record resolves all categories, including hidden ones; Receipt joins the category name. |
| Release INTERNET check not wired | Resolved in source | Release package tasks depend on the manifest guard. |
| Screenshot tests record instead of compare | Open | Tests still use recording mode and no committed screenshot baselines were found. A baseline generation/review run is needed. |
| CSV test expectation matches atomic import | Resolved in test source | The test now verifies that a malformed CSV rejects the whole file and makes no partial writes. |
| In-app locale list differs from Android settings | Resolved in source | Locale configuration now lists the app's supported languages. |
| Guide and parity checklist disagree with implementation | Resolved in source, runtime unverified | Category reorder and wide-screen entry dialog were added; guide and parity claims were corrected. |

**Verification:** The required `assembleDebug testDebugUnitTest lintDebug` run completed successfully. It ran the app's 173 unit tests and the parser module's 35 tests with no failures. Two additional forced app-suite passes each completed successfully (173 tests per pass). Three isolated passes of the parser fuzz/benchmark class passed; the latest benchmark measurement was 79 ms. One extra forced combined rebuild run did fail the parser's documented `<200 ms` benchmark threshold at 220 ms while build work was concurrent. This indicates a host-sensitive timing assertion; the parser test class passed in the three isolated reruns. Lint reported 0 errors and 79 warnings: 51 Gradle dependency update notices, 18 unused resources, 3 Android Gradle Plugin version notices, 2 locale-folder notices, and 5 individual warnings (`IconLauncherShape`, `ModifierParameter`, `ObsoleteSdkInt`, `PluralsCandidate`, and `UnusedAttribute`). Screenshot comparison and on-device verification were not performed; screenshot tests remain in recording mode without reviewed committed baselines.

The findings below describe the state observed before remediation. Refer to the result table above for the current source status.

## Findings

### P1 — Record and receipt totals can disagree with the rows shown

`ExpenseDao.observeDay()` returns every currency for a date, while `observeTotal()` filters by the currently selected currency (`app/src/main/kotlin/com/yourco/spend/data/db/dao/ExpenseDao.kt`). Both `RecordViewModel` and `ReceiptViewModel` combine those unfiltered rows with the filtered total (`feature/record/RecordViewModel.kt`, `feature/receipt/ReceiptViewModel.kt`). The row amounts retain their original currencies.

After a user switches currency, the Record page and receipt can show, for example, a USD expense beside a zero EUR total. The receipt total also no longer equals the amounts printed above it. This conflicts with the stated behavior that currency changes apply to new entries only.

**Recommendation:** Show a separate total for each currency, or make each screen consistently filter its rows and indicate that the selected currency is being shown. Do not add conversion without an explicit exchange-rate feature.

### P1 — Entry and edit forms close before persistence succeeds

`EntryViewModel.submit()` launches the database insert in `viewModelScope`, then clears the saved text and returns success immediately (`feature/record/EntryViewModel.kt`). `EntrySheet` closes on that success return (`feature/record/EntrySheet.kt`). `RecordViewModel.updateExpense()` uses the same asynchronous pattern and clears the edit state before the update completes (`feature/record/RecordViewModel.kt`).

If Room throws or the write otherwise fails, the user sees a closed sheet and the entry or edit is lost, with no failure state or retry path.

**Recommendation:** Keep the form state until the repository confirms success; surface a localized failure and let the user retry. Apply the same rule to edit saves.

### P1 — Import can leave a partial database change while reporting failure

`DataImporter.importJson()` writes categories and expenses one at a time and only catches exceptions after those writes (`data/export/DataImporter.kt`). There is no transaction around the import. A malformed later row can therefore leave earlier rows committed while the result says the import failed with zero imported records. CSV import silently skips malformed rows with `continue` and can still return `success = true` (`data/export/DataImporter.kt`).

**Recommendation:** Parse and validate the full document before writing, then apply it in a Room transaction. Return an explicit partial-import result if partial import is intentional; do not report full success after skipped rows.

### P1 — A soft-deleted row can be resurrected by importing a newer stale copy

The DAO sets `deleted_at` but does not update `updated_at` (`data/db/dao/ExpenseDao.kt`). JSON import chooses the incoming row whenever its `updated_at` is greater than the stored row (`data/export/DataImporter.kt`). Thus, if a stale export was made after the last edit but before deletion, its timestamp can be greater than the tombstone row’s unchanged timestamp, and importing it will replace the tombstone with a live expense.

**Recommendation:** Advance `updated_at` on delete and restore, using the event time, so conflict resolution treats the deletion as the newest change.

### P1 — Release signing passwords are embedded in source

The release signing configuration contains literal store and key passwords in `app/build.gradle.kts`. The local keystore is ignored by Git, but anyone who obtains that file can use the published source password to access it. The same configuration also uses the generic `com.yourco.spend` namespace and application ID (`app/build.gradle.kts`), which the guide identifies as placeholders.

**Recommendation:** Load signing values from protected environment or local release properties, and replace the placeholder application ID before distributing a release. Do not include the secret value in issue reports or release notes.

### P1 — Published deletion claims are not implemented

`docs/PRIVACY.md` claims Settings has “Wipe all data”; `docs/DATA_SAFETY.md` also claims users can clear historical periods and wipe all data; `docs/STORE_LISTING.md` advertises a local database wipe. The Settings screen and view model provide theme, language, currency, category rename/hide/add, export, and import, but no wipe or clear-history action (`feature/settings/SettingsScreen.kt`, `feature/settings/SettingsViewModel.kt`). The DAO likewise has no clear-all operation.

These are user-facing privacy and store claims that the current app cannot satisfy.

**Recommendation:** Either implement the deletion controls and verify that they clear the database and preferences, or remove these claims before publishing.

### P2 — CSV export/import is lossy, and duplicate rows can collapse

CSV export writes time only to seconds (`data/export/CsvExporter.kt,29-38`), omits expense IDs and update/deletion metadata, and excludes soft-deleted rows (`:22-23`). CSV import generates IDs from date, second-resolution time, amount, currency, category, and note (`data/export/DataImporter.kt`). Two distinct expenses with identical values created in the same second therefore receive the same ID; the second is skipped by the conflict check. A CSV round trip also cannot preserve original IDs, raw input, or sub-second timestamps.

**Recommendation:** Document CSV as a lossy interchange format and reserve exact backup/restore for JSON, or include enough identity and timestamp data in a versioned CSV format to preserve distinct entries.

### P2 — CSV text fields could be interpreted as spreadsheet formulas

Expense notes and imported category identifiers are user-controlled text. Before remediation, `CsvExporter` quoted commas and quotes but left cells beginning with formula markers such as `=`, `+`, `-`, or `@` unchanged. A user opening an exported file in a spreadsheet could cause those cells to be evaluated.

**Status:** Resolved in source. The exporter prefixes formula-like text cells with an apostrophe before applying CSV quoting. JSON backup contents are unchanged.

### P2 — Import does not enforce the expense invariants used by the app

JSON import accepts `amount_minor` directly and only rejects schema versions below 1 (`data/export/DataImporter.kt,71-94`). CSV import parses and stores zero or negative values and does not apply the parser’s upper amount limit (`:159-182`). It also accepts arbitrary currency strings. The entry parser and edit form reject non-positive amounts, so imported records can violate the normal app data contract and create negative or nonsensical totals.

**Recommendation:** Reject unsupported future schema versions and validate amount, currency, date, ID, and category references before any database write. Keep those checks consistent with entry and edit validation.

### P2 — Category corrections are lost on process death

The typed entry text is persisted in `SavedStateHandle` (`feature/record/EntryViewModel.kt`), but the manually chosen category is only held in an in-memory `MutableStateFlow` (`:51-55`). After Android kills and restores the app with the entry sheet open, the text can return while its category override is gone; submitting then uses the parser’s default category.

**Recommendation:** Persist the category override alongside the typed text, or clear the draft consistently and explain that it was discarded.

### P2 — Editing an expense date leaves its timestamp on the old date

The edit sheet updates `localDate` but preserves `createdAt` (`feature/edit/EditSheet.kt`); `RecordViewModel` only changes `updatedAt` when saving (`feature/record/RecordViewModel.kt`). CSV export later combines the edited `localDate` with a time derived from the original `createdAt` (`data/export/CsvExporter.kt`). The guide specifies editing date and time, while the current UI only edits a date.

**Recommendation:** Define whether editing the date should move the stored instant while preserving its local time, or whether date is independent metadata. Keep `localDate`, `createdAt`, display order, and exported date/time aligned, and add the missing time control if the guide remains authoritative.

### P2 — Time-zone change broadcasts cannot update the injected clock’s zone

The clock is created once with `Clock.systemDefaultZone()` (`core/time/ClockProvider.kt`). `TodayObserver` reacts to `ACTION_TIMEZONE_CHANGED`, but computes the date from that injected clock (`core/time/TodayObserver.kt`). A `Clock.systemDefaultZone()` instance retains the zone selected when it was created, so after a time-zone change the broadcast and 30-second tick can continue producing the old-zone date. New entries may then be written under the wrong `local_date` until the process restarts.

**Recommendation:** Resolve the current system zone when calculating today, while continuing to preserve each saved `local_date` value for historical records.

### P2 — Custom category IDs can appear as category names on Record

The Record list resolves categories only through built-in string resources and otherwise displays the raw ID (`feature/record/RecordScreen.kt`). Custom categories are created with IDs such as `custom_<uuid>` and a user-facing `nameOverride` (`feature/settings/SettingsViewModel.kt`). The Record screen does not pass those category entities to its display helper. A blank-note custom-category expense on a receipt also falls back to the built-in “Other” resource (`feature/receipt/ReceiptViewModel.kt`).

**Recommendation:** Resolve category IDs through the category repository/entity map on Record and Receipt, using the override before a built-in fallback.

### P2 — The no-INTERNET guard is not part of the normal release checks

`checkNoInternetPermission` is registered in `app/build.gradle.kts`, but no standard `check`, `assembleRelease`, or release task depends on it. The manifest currently has no INTERNET permission, but the guard will not protect routine builds unless someone explicitly invokes the custom task. `docs/DATA_SAFETY.md` describes this as release build-pipeline enforcement.

**Recommendation:** Wire the task into the release verification path or CI, and keep the documentation aligned with the task that actually runs.

### P2 — Screenshot tests record new images instead of checking baselines

The screenshot test classes construct `RoborazziOptions(taskType = RoborazziTaskType.Record)` (for example, `app/src/test/kotlin/com/yourco/spend/feature/record/RecordScreenshotTest.kt` and `feature/insight/InsightScreenshotTest.kt`). The Gradle test configuration also sets `roborazzi.test.record=true` (`app/build.gradle.kts`). These tests capture screenshots but do not fail when the rendered UI drifts from an approved baseline, despite `PARITY.md` describing screenshot regression coverage.

**Recommendation:** Keep explicit recording as a maintainer action and configure normal test runs to compare against committed baselines.

### P2 — The in-app language list and Android locale configuration disagree

`LanguageCatalog` exposes 26 choices (`core/language/SupportedLanguages.kt`) and the project contains matching translated resource directories, but `res/xml/locales_config.xml` advertises only English and Traditional Chinese. Users can select the additional languages in-app, but Android’s per-app language settings will only offer the two declared locales.

**Recommendation:** Decide whether the supported scope is two languages or all 26, then align the picker, locale configuration, and release documentation.

### P2 — Several guide features and the parity checklist do not match the implementation

- The guide asks for category rename, reorder, hide, and add; the UI/view model implement rename, hide, and add, with no reorder action (`feature/settings/SettingsViewModel.kt`).
- The guide asks to edit date/time; the edit sheet only has a date picker (`feature/edit/EditSheet.kt`).
- The guide asks for a centered dialog-width entry sheet on large screens. `AppRoot` selects a two-pane layout at 600dp, but `RecordScreen` still hosts the entry sheet in its bottom-sheet scaffold (`navigation/AppRoot.kt,132-135`; `feature/record/RecordScreen.kt`). A 480dp width cap alone does not make it a centered dialog.
- `PARITY.md` claims all behavior is verified (`PARITY.md`) but records a 56dp sheet peek while the code uses 104dp plus insets (`feature/record/RecordScreen.kt`), names a `ReceiptPerforation.kt` source that is absent, says `ACTION_TIME_TICK` is observed though the observer registers date/time/time-zone broadcasts and a timer, and attributes a `DisposableEffect` to `AppTheme.kt` where the implementation uses `SideEffect`.

**Recommendation:** Reconcile the guide, implementation, and parity table. Mark items “implemented,” “verified,” or “not implemented” based on evidence; do not call source inspection or screenshot capture a verified parity pass.

## Remaining follow-up

1. Confirm the release APK signing certificate matches the publisher's registered Play upload key before upload.
2. Generate and review screenshot baselines, then switch routine screenshot tests to compare against those approved baselines.
3. Confirm the source-reviewed UI changes on compact and wide Android devices.
4. Confirm Play Console Data Safety answers for Android Auto Backup before publication.
5. Make the parser's `<200 ms` performance assertion robust under concurrent build load while preserving the guide's performance target.
