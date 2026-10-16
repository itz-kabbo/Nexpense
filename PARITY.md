# Parity Checklist

Every design and behavior detail between the Android implementation and the specification has been verified and confirmed.
Implementation defaults are backed by unit tests, screenshot regression tests, and verification comments (`// VERIFY:`) in code.

| Area | Feature / Element | Proposed Default | Verification Status | Notes |
| --- | --- | --- | --- | --- |
| Navigation | Screen Model | Horizontal pager (Record, Receipt, Insight) with 3-dot indicator | Verified | Verified in `AppRoot.kt` with `HorizontalPager(state = pagerState)`. Tested in `AppRootTest.kt`. |
| Splash | Animation & Timing | System splash with 600-800ms fade/scale wordmark | Verified | Implemented via `androidx.core.splashscreen:splashscreen` in `MainActivity.kt` and `res/values/themes.xml`. |
| Theme | Colors | Light: #FFFFFF bg, #F6F6F4 surface, #FBFAF6 paper. Dark: #000000 bg, #121212 surface, #1A1A1A paper | Verified | Verified in `Color.kt` and `AppTheme.kt`. Dynamic color disabled. Screenshot verified. |
| Typography | Fonts & Sizes | Inter / system sans-serif, tabular numbers (`tnum`), IBM Plex Mono / JetBrains Mono for receipt | Verified | Verified in `Type.kt` and `ReceiptCard.kt`. Monospace font used for simulated physical receipt. |
| Components | Corner Radii | Sheets: 28dp, Cards: 16dp, Chips: fully rounded | Verified | Verified in `Shape.kt` with 28dp top radii for bottom sheets, 16dp for cards, 999dp for chips. |
| Motion | Animation Curves | Spring damping ~0.85, medium stiffness; content fade 150-200ms; list insert 250ms | Verified | Verified in `Motion.kt`. Constants configured for physics-based spring damping and timing. |
| Categories | Built-in Seed List | Food & Drink, Groceries, Transport, Shopping, Home & Bills, Health, Fun, Travel, Other | Verified | Verified in `CategoryEntity.DEFAULT_CATEGORIES` and seeded on first run in `CategoryDaoAndRepoTest.kt`. |
| Receipt | Print Animation | Rows revealed top-to-bottom over ~500ms | Verified | Verified in `ReceiptCard.kt` with staggered alpha and vertical translation animation. |
| Multi-item | Multiple entries | Split on newlines, semicolons, Chinese commas, ASCII comma + space | Verified | Verified in `ExpenseParser.kt` and tested across extensive unit tests in `parser` module. |
| Currency | Changing currency | Default from locale, affects new entries only | Verified | Verified in `SettingsRepositoryImpl.kt` and `FormatMinorTest.kt`. Each expense row retains its own currency. |
| Spacing | Grid & Margins | 4dp grid, 24dp screen horizontal padding | Verified | Verified in `Spacing.kt`. Padding 24dp across screens and 4dp base increments. |
| Receipt | Perforation Teeth | 6dp tooth width with triangular teeth | Verified | Verified in `ReceiptPerforation.kt` and `ReceiptCardTest.kt`. Measured Canvas path geometry. |
| Record | Today Date Format | "EEEE, MMM d" in user locale | Verified | Verified in `RecordViewModel.kt` (`formatTodayDate`) with tests in `RecordViewModelTest.kt`. |
| Record | Sheet Peek Height | 56dp + navigation bar bottom inset | Verified | Verified in `RecordScreen.kt` with edge-to-edge system insets and bottom sheet peek. |
| Entry | Text Field Style | Single line, transparent background, headline font | Verified | Verified in `EntrySheet.kt` using transparent container colors and custom indicator line. |
| Top Bar | Settings Icon | Material Symbols rounded Settings icon | Verified | Verified in `RecordScreen.kt` top bar action icon linking to Settings dialog/screen. |
| Record | Swipe to Delete Style | Hairline background with secondary text Delete icon | Verified | Verified in `RecordScreen.kt` with `SwipeToDismissBox` and `undoDelete` snackbar action. |
| Edit | Sheet Layout | ModalBottomSheet with 28dp top radius, surface background | Verified | Verified in `EditSheet.kt` with `ModalBottomSheet` and 28dp top shape. |
| Edit | Date Selection | Clickable row displaying ISO date, opens DatePicker | Verified | Verified in `EditSheet.kt` with `DatePickerDialog` integration. Tested in `EditSheetTest.kt`. |
| Receipt | Items Ordering | Chronological (oldest first like printed shopping slip) | Verified | Verified in `ReceiptViewModel.kt` reversing query results to oldest-first. Tested in `ReceiptViewModelTest.kt`. |
| Receipt | Date Header Format | "EEEE, MMM d, yyyy" in user locale | Verified | Verified in `ReceiptViewModel.kt` (`formatDate`). Tested in `ReceiptViewModelTest.kt`. |
| Receipt | Footer Text | "Every expense is a mote." brand copy | Verified | Verified in `ReceiptCard.kt` and strings resources (`receipt_footer_brand_text`). |
| Insight | Period Labels | "Today"/"Yesterday"/"MMM d, yyyy", "MMMM yyyy", "yyyy" (or localized Chinese patterns) | Verified | Verified in `InsightPeriod.kt` with localized formatting strings. Tested in `InsightPeriodTest.kt`. |
| Insight | Stepper Layout | Centered below BigAmount with `<` Previous, label, `>` Next | Verified | Verified in `InsightScreen.kt` with Previous/Next buttons and bounds-checking `canGoForward`. |
| Insight | Year 12-Bar Chart | Canvas-based 12 monthly bars with current month highlighted in primary text | Verified | Verified in `InsightScreen.kt` (`YearChart`) and tested in `InsightViewModelTest.kt`. |
| Insight | Month Sparkline | Canvas-based daily mini-bars across 28..31 days with current day highlight | Verified | Verified in `InsightScreen.kt` (`MonthSparkline`) and tested in `InsightViewModelTest.kt`. |
| Large Screens | Two-Pane Layout | Record and Receipt side-by-side with segmented tab to Insight on >= 600dp | Verified | Verified in `AppRoot.kt` using window size class width >= 600dp. Tested in `AppRootTest.kt`. |
| Large Screens | Sheet Width | EntrySheet and EditSheet centered dialog-width sheet capped at 480dp | Verified | Verified in `EntrySheet.kt` and `EditSheet.kt` using `widthIn(max = 480.dp)`. |
| Midnight Rollover | Ticking Flow & Broadcast | Automatic today date flip without restart via TodayObserver | Verified | Verified in `TodayObserver.kt` listening to `ACTION_DATE_CHANGED` & `ACTION_TIME_TICK`. Tested in `TodayObserverTest.kt`. |
