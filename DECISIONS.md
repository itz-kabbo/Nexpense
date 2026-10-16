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


