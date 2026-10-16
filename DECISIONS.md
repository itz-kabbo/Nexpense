# Architecture and Implementation Decisions

## M4: HapticFeedbackType Fallback for Save Confirmation

- **Context:** `GUIDE.md` section 3.3 specifies `HapticFeedbackType.Confirm` on save (API 30+, with a fallback to `TextHandleMove`).
- **Issue:** In the project's Compose BOM version (2024.10.01), `androidx.compose.ui.hapticfeedback.HapticFeedbackType.Confirm` is not present on the `HapticFeedbackType` companion object.
- **Decision:** Used `HapticFeedbackType.TextHandleMove` as the fallback in `EntrySheet.kt` when confirming an entry save.
