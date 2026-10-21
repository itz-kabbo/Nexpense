# UI/UX Redesign Specification: Tactile Paper & Editorial Minimal

- **Date:** 2026-10-20
- **Status:** Approved by User
- **Target Architecture:** Android Jetpack Compose, Material 3, Monochromatic Design System
**Source of Truth:** [GUIDE.md](GUIDE.md)

> This file is a visual design proposal. Use `GUIDE.md` for current feature behavior and `../PARITY.md` for source-reviewed implementation status; this document is not evidence of runtime or pixel parity.

---

## 1. Executive Summary & Design Vision

This specification describes a proposed UI/UX redesign for Nexpense, an independent Android expense tracker inspired by Mote. — Minimal Expense Tracker by [Yun Ju Tseng](https://apps.apple.com/au/app/mote/id6816360396). Nexpense has its own name and visual identity; this document does not authorize use of Mote branding or assets. The proposal is guided by Nexpense empty-state screenshots and calls out the main visual gaps:
1. **Unanchored Empty Voids:** Zero-expense states feature plain, unstyled gray text strings floating in large empty expanses.
2. **Disappearing Signature Identity:** On the Daily Receipt screen, having no expenses causes the entire physical paper receipt slip to disappear completely, replacing it with plain text.
3. **Flat Contrast & Harsh Glare:** Light mode relies on stark `#FFFFFF` pure white, while dark mode lacks depth, layer separation, and specular lighting.
4. **Hero Sizing Tension:** The `A$ 0.00` hero total uses uniform thin typography across currency symbols, integers, and decimals, creating visual float.

### The Redesign Approach: "Tactile Paper & Editorial Minimal"
The redesigned experience embraces tactile paper craftsmanship, editorial typography hierarchy, and intentional zero-state design. The app retains 100% of its minimalist, offline-only, distraction-free ethos while elevating every micro-interaction to feel crafted and deliberate (reminiscent of high-end tools like Apple Weather, Linear, and Teenage Engineering).

---

## 2. Design System Tokens & Foundation

### 2.1 Monochromatic Color Palette

| Token Name | Light Mode | Dark Mode | Semantic Purpose |
| :--- | :--- | :--- | :--- |
| `background` | `#FBFBF9` (Warm Porcelain) | `#09090B` (Obsidian Base) | Window background; eliminates sterile glare |
| `surface` | `#FFFFFF` (Crisp Paper) | `#141417` (Graphite Layer) | Elevated cards, bottom sheets, dialog surfaces |
| `surfaceVariant` / `dock` | `#F2F2EC` (Dock Container) | `#1D1D22` (Elevated Dock) | Floating quick-entry dock, segmented control track |
| `paper` | `#FDFCF7` (Thermal Slip) | `#19191E` (Dark Thermal Slip) | Physical receipt card background |
| `textPrimary` | `#18181B` (Deep Ink) | `#F4F4F5` (Crisp White) | Main headings, amounts, active items |
| `textSecondary` | `#71717A` (Slate Gray) | `#A1A1AA` (Medium Slate) | Metadata, secondary labels (WCAG AA ≥ 4.5:1) |
| `textTertiary` | `#A1A1AA` (Muted Gray) | `#52525B` (Dark Muted) | Placeholders, inactive landmarks |
| `hairline` | `#E8E8E2` (Paper Crease) | `#27272A` (Subtle Divider) | Subtle 0.5dp section lines and card borders |
| `rimHighlight` | `Color.Transparent` | `#FFFFFF18` (10% White) | 0.5dp top-edge specular highlight for beveled depth |

### 2.2 Typography Hierarchy & Optical Hero Numbers

* **Font Families:**
  * Sans-Serif: System default sans-serif / Inter for headlines, body, labels, and chips.
  * Monospace: IBM Plex Mono (`Font(R.font.ibm_plex_mono)`) for receipt titles, item rows, total line, and chart landmark labels.
* **Optical Hero Sizing Architecture (`BigAmount.kt`):**
  * **Currency Symbol (`A$` / `$` / `NT$`):** 20sp, `FontWeight.Medium`, baseline-aligned with `0.60f` alpha opacity.
  * **Whole Digits (`0`):** 52sp, `FontWeight.Light`, tabular figures (`FontFeatureSettings = "tnum"`).
  * **Decimal Point & Cent Digits (`.00`):** 34sp (65% scale), `FontWeight.Normal`, baseline-pinned, tabular figures.
  * *Result:* Eliminates layout jumping when values change and creates rapid financial scanning hierarchy.
* **Receipt Monospace Styling:** Tightened letter-spacing (`-0.5sp`), uppercase titles with `+1.2sp` tracking for stamped physical aesthetic.

### 2.3 Shapes, Elevation & Motion Curves

* **Corner Radii:**
  * Modal Sheets: 28dp top corners (`RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp)`).
  * Floating Quick-Entry Dock: 20dp fully rounded pill dock.
  * Surface Cards (Settings, Insights breakdown): 16dp.
  * Interactive Chips & Steppers: 999dp circular / capsule.
  * Receipt Perforation: 6dp triangular zig-zag cutouts (`PerforatedShape`).
* **Spatial Grid:** 8dp harmonious scale (4dp micro, 8dp default, 16dp container padding, 24dp screen horizontal margin, 32dp section gap, 48dp hero vertical padding).
* **Motion Specs (`Motion.kt`):**
  * Springs: Physics-based spring animations with `dampingRatio = 0.85f` (smooth settling, zero cartoonish bounce).
  * Fades: 150ms–200ms content cross-fades.
  * Thermal Print Reveal: 350ms staggered vertical translation & alpha fade for receipt items.

---

## 3. Screen Specifications

### 3.1 Screen 1: Record Screen & Docked Quick-Entry Sheet

Files:
* [`RecordScreen.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/record/RecordScreen.kt)
* [`EntrySheet.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/record/EntrySheet.kt)

#### Layout Structure
```
EMPTY STATE                                  POPULATED STATE
+------------------------------------+       +------------------------------------+
| Monday, Oct 19                 (⚙) |       | Monday, Oct 19                 (⚙) |
|                                    |       |                                    |
|              A$ 0.00               |       |              A$ 42.50              |
|                                    |       |                                    |
|                                    |       |   Coffee             [Food]  $6.00 |
|          No expenses yet           |       |   -------------------------------- |
|     Swipe up to record your        |       |   Lunch              [Food] $18.50 |
|           first expense               |       |   -------------------------------- |
|                                    |       |   Uber          [Transport] $18.00 |
|    Simple records. Clearer days.|      |                                    |
|                                    |       |                                    |
|      [ ✏️ Type what you spent ]    |       |      [ ✏️ Type what you spent ]    |
|               •  ·  ·              |       |               •  ·  ·              |
+------------------------------------+       +------------------------------------+
```

#### Detailed Interactions & Components
1. **Top Bar:** Formatted date (16sp medium, `textPrimary`) and a 36dp circular settings button container.
2. **Hero Total:** Optically proportioned `BigAmount` displaying today's total spend.
3. **Empty State Composition (User Approved):**
   * Eliminates bounding boxes.
   * Uses clean, centered, calm typographic hierarchy:
     * Title: *"No expenses yet"* (16sp medium, `textPrimary`).
     * Subtitle: *"Swipe up to record your first expense"* (14sp regular, `textSecondary`).
     * Nexpense helper text: *"Simple records. Clearer days."* (13sp italic, `textTertiary`).
4. **Populated List (`EntryRow`):**
   * 56dp row height with minimum 48dp touch targets.
   * Left: Note description in 16sp regular (`textPrimary`).
   * Center: Monochrome category pill chip (`CategoryChip`) in `#F2F2EC` light / `#1D1D22` dark with `0.5dp` hairline outline.
   * Right: Tabular amount aligned right.
   * Dividers: 0.5dp hairline rule (`#E8E8E2` light / `#27272A` dark) with 24dp screen padding insets.
   * Swipe-to-delete: Reveals circular trash icon with tactile haptic feedback and an undo `Snackbar`.
   * Tap-to-edit: Launches `EditSheet`.
5. **Docked Quick-Entry Bar (`EntrySheet.kt`):**
   * **Peek Dock:** 52dp height, 20dp corner radius, floating `#FFFFFF` (light) / `#141417` (dark) dock container. Features a subtle pencil glyph, hint text *"Type what you spent (e.g. 6 coffee)"*, and an upward expansion indicator.
   * **Expanded Bottom Sheet:**
     * Seamless spring transition (`dampingRatio = 0.85f`).
     * Automatically requests focus and opens soft keyboard (IME).
     * Single-line headline input field.
     * **Interactive Parser Badge:** When valid input is detected (e.g. `6 coffee`), dynamically displays an interactive pill: `[ Food & Drink ▾ ]  $6.00`. Tapping the pill triggers in-sheet category override.
     * Keyboard "Done" triggers confirm haptic (`HapticFeedbackConstants.CONFIRM`), persists expense to Room, and collapses the sheet.

---

### 3.2 Screen 2: Daily Receipt Screen & Persistent Slip

Files:
* [`ReceiptScreen.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/receipt/ReceiptScreen.kt)
* [`ReceiptCard.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/receipt/ReceiptCard.kt)

#### Layout Structure
```
RECEIPT EMPTY STATE                          RECEIPT POPULATED STATE
+------------------------------------+       +------------------------------------+
| (‹)  Monday, Oct 19, 2026  (›) [↑] |       | (‹)  Monday, Oct 19, 2026  (›) [↑] |
|                                    |       |                                    |
|     /\/\/\/\/\/\/\/\/\/\/\/\/\     |       |     /\/\/\/\/\/\/\/\/\/\/\/\/\     |
|    |                          |    |       |    |                          |    |
|    |          NEXPENSE           |    |       |    |          NEXPENSE           |    |
|    |   Monday, Oct 19, 2026   |    |       |    |   Monday, Oct 19, 2026   |    |
|    |                          |    |       |    |                          |    |
|    |  - - - - - - - - - - - - |    |       |    | Coffee.............$6.00 |    |
|    |                          |    |       |    | Lunch.............$18.50 |    |
|    |         [ 🧾 ]           |    |       |    | Taxi..............$18.00 |    |
|    |   (Minimal Line-art      |    |       |    |                          |    |
|    |      Ledger Icon)        |    |       |    |  - - - - - - - - - - - - |    |
|    |                          |    |       |    |  TOTAL            $42.50 |    |
|    |   No expenses recorded   |    |       |    |                          |    |
|    |      for this date       |    |       |    |                          |    |
|    |                          |    |       |    |                          |    |
|    |  - - - - - - - - - - - - |    |       |    |  - - - - - - - - - - - - |    |
|    | Your day, item by item. |    |       |    | Your day, item by item. |    |
|    |                          |    |       |    |                          |    |
|     \/\/\/\/\/\/\/\/\/\/\/\/\/     |       |     \/\/\/\/\/\/\/\/\/\/\/\/\/     |
|                                    |       |                                    |
|               ·  •  ·              |       |               ·  •  ·              |
+------------------------------------+       +------------------------------------+
```

#### Detailed Interactions & Components
1. **Top Stepper Bar:** Circular `(‹)` and `(›)` buttons (36dp touch targets). The forward `(›)` button auto-locks and dims to 25% opacity when viewing today.
2. **Persistent Receipt Card (`ReceiptCard.kt`):**
   * Never disappears. Whether 0 or 20 expenses exist, the physical receipt is always drawn.
   * Clipped to `PerforatedShape` with 6dp triangular zig-zag teeth top and bottom.
   * Surface: `AppTheme.colors.paper` (`#FDFCF7` light / `#19191E` dark) with a soft 1.5dp elevation shadow in light mode, and a 0.5dp hairline border in dark mode.
3. **Empty Receipt Content (User Approved):**
   * Centered header: `NEXPENSE` in bold monospace title.
   * Localized date in monospace secondary.
   * Perforated dashed divider (`DashedRule`).
   * **Minimal Line-Art Vector Illustration:** Clean, delicate hairline vector outline of a quiet receipt ledger / empty shopping slip.
   * Stamped receipt note: *"NO EXPENSES RECORDED FOR THIS DATE"* in monospace small caps with `+1.2sp` tracking.
   * Perforated dashed divider.
   * Brand footer: *"Your day, item by item."*.
4. **Populated Receipt Content:**
   * `LeaderRow` items with dot leaders bridging note descriptions to tabular amounts.
   * `DashedRule` perforated divider.
   * Bold `TOTAL` row.
   * Brand footer text.
5. **Thermal Print Animation:** When switching days, rows reveal via a staggered 350ms top-to-bottom slide & fade.
6. **Receipt Sharing:** Captures the receipt card via `GraphicsLayer` to high-resolution PNG, saved via `ReceiptImageSaver`, and invokes Android's system share sheet (`ACTION_SEND`).

---

### 3.3 Screen 3: Insight Screen & Visualizations

Files:
* [`InsightScreen.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/insight/InsightScreen.kt)
* [`MonthSparkline.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/insight/charts/MonthSparkline.kt)
* [`YearBarChart.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/insight/charts/YearBarChart.kt)

#### Layout Structure
```
INSIGHT (MONTH EMPTY)                        INSIGHT (MONTH POPULATED)
+------------------------------------+       +------------------------------------+
|      ( Day |  Month  | Year )      |       |      ( Day |  Month  | Year )      |
|                                    |       |                                    |
|              A$ 0.00               |       |             A$ 348.00              |
|         (‹) October 2026 (›)       |       |         (‹) October 2026 (›)       |
|                                    |       |                                    |
|   · · · · · · · · · · · · · · ·    |       |   | | ||| || | |||| || | | || |    |
|   -----------------------------    |       |   -----------------------------    |
|   1              15             31 |       |   1              15             31 |
|                                    |       |                                    |
|          No spending yet           |       | +--------------------------------+ |
|      recorded for this month       |       | | Food & Drink      54%  $188.00 | |
|                                    |       | | [==================          ] | |
|                                    |       | | ------------------------------ | |
|                                    |       | | Transport         30%  $104.00 | |
|                                    |       | | [===========                 ] | |
|                                    |       | +--------------------------------+ |
|               ·  ·  •              |       |               ·  ·  •              |
+------------------------------------+       +------------------------------------+
```

#### Detailed Interactions & Components
1. **Segmented Control:** Capsule track (`#F2F2EC` light / `#1D1D22` dark) with a spring-sliding thumb indicator (`#FFFFFF` light / `#2A2A32` dark).
2. **Hero Amount & Period Stepper:** Optically proportioned `BigAmount` with circular `(‹)` and `(›)` buttons.
3. **Tactile Canvas "Ghost Tracks" (User Approved):**
   * **Month Mode (`MonthSparkline.kt`):**
     * Empty State: 28–31 subtle ghost hairline tracks (10% opacity) anchoring the month's timeline, with today's marker highlighted.
     * Populated State: Proportional vertical bars with 2dp rounded tops and smooth spring height growth. Current day rendered in full `textPrimary`.
     * Landmarks: Days `1`, `15`, `31` aligned in monospace.
   * **Year Mode (`YearBarChart.kt`):**
     * Empty State: 12 subtle ghost vertical pill tracks (10% opacity) representing the months, with the active month pill highlighted.
     * Populated State: 12 proportional vertical bars with rounded tops.
     * Monospace month letters (`J F M A M J J A S O N D`) centered beneath each pillar.
4. **Category Breakdown (`CategoryBar.kt`):**
   * Housed within an elevated card (`16dp` radius, `#FFFFFF` light / `#141417` dark, `0.5dp` border).
   * Proportional horizontal fill bars (6dp height, rounded capsule ends) animating smoothly with spring physics.
   * Category label (14sp medium), percentage badge (e.g. `54%`), and right-aligned tabular amount.
5. **Empty State Prompt:** Clean centered message: *"No spending yet recorded for this period"* (14sp `textSecondary`) centered beneath the ghost chart.

---

### 3.4 Screen 4: Settings Screen & Modal Sheets

Files:
* [`SettingsScreen.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/settings/SettingsScreen.kt)
* [`EditSheet.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/edit/EditSheet.kt)
* [`CategoryPicker.kt`](file:///c:/Users/rkbln/Documents/antigravity/calm-borg/app/src/main/kotlin/com/yourco/spend/feature/edit/CategoryPicker.kt)

#### Layout Structure
```
SETTINGS SCREEN                                EDIT EXPENSE SHEET
+------------------------------------+         +------------------------------------+
| (←)            Settings            |         |              Edit Expense          |
|                                    |         |                                    |
| APPEARANCE & LOCALE                |         | Amount                             |
| +--------------------------------+ |         | $ 42.50                            |
| | Theme      (System|Light|Dark) | |         |                                    |
| | ------------------------------ | |         | Note                               |
| | Language          English    › | |         | Dinner with team                   |
| | ------------------------------ | |         |                                    |
| | Currency          USD ($)    › | |         | Category             [ Food ▾ ]    |
| +--------------------------------+ |         |                                    |
|                                    |         | Date                 2026-10-19  📅|
| DATA & BACKUP                      |         |                                    |
| +--------------------------------+ |         |  [ Cancel ]          [  Save  ]    |
| | 📥 Export Data (JSON / CSV)  › | |         +------------------------------------+
| | ------------------------------ | |
| | 📤 Import Data (JSON / CSV)  › | |
| +--------------------------------+ |
|                                    |
| All data is stored locally.        |
+------------------------------------+
```

#### Detailed Interactions & Components
1. **Settings Grouped Cards:**
   * 16dp rounded card containers with 0.5dp hairline borders.
   * Section 1: Appearance (System/Light/Dark capsule switch), Language (the supported language catalog), and Currency selector (`USD`, `TWD`, `EUR`, `JPY`, etc.).
   * Section 2: Categories (custom addition, rename, reorder, visibility toggle).
   * Section 3: Data & Backup (JSON backup, lossy CSV interchange, import, and a confirmed local data wipe).
   * Section 3: Data & Backup (SAF JSON backup with non-destructive `updated_at` merge, and CSV export/import).
   * Footer: Offline privacy badge (*"All data stays on this device. No servers, no tracking, no accounts."*).
2. **Edit Expense Sheet (`EditSheet.kt`):**
   * 28dp top radius modal sheet constrained to 480dp width.
   * Amount input operating strictly in `Long` minor units without floating-point inaccuracies.
   * In-sheet `CategoryPicker` overlay avoiding nested OS dialog lags.
   * Clickable ISO date row opening Material 3 `DatePickerDialog`.
   * High-contrast primary `Save` button and secondary `Cancel` button with predictive back gesture dismiss.

---

## 4. Edge Cases, System Integrity & Compatibility

1. **Midnight and Time-Zone Changes:** `TodayObserver` listens for date, time, and time-zone broadcasts and also checks periodically. The system clock reads the current device zone when calculating new local dates.
2. **200% Font Scale & Display Zoom:** Every text container uses flexible column arrangements and ellipsis where appropriate. The optical hero amount scales gracefully without clipping off-screen.
3. **Screen Reader (TalkBack) Accessibility:**
   * All touch targets meet or exceed 48×48dp.
   * `EntryRow` merges child semantics so TalkBack announces: *"Coffee, Food & Drink, 6 dollars"*.
   * Empty states read poetic titles clearly.
   * Canvas charts provide semantic accessibility descriptions summarizing the period and active spend days.
4. **Offline & Permission Invariant:** The entire redesign maintains zero `INTERNET` permission, zero third-party UI libraries, and zero network dependencies.

---

## 5. Implementation Roadmap & Verification Strategy

The implementation will be executed in four disciplined, incremental phases:
* **Phase 1: Foundation & Tokens:** Update `Color.kt`, `Type.kt`, `Shape.kt`, `Spacing.kt`, and `BigAmount.kt`.
* **Phase 2: Record Screen & Docked Entry:** Implement the docked peek bar, centered empty quote state, and optical hero integration.
* **Phase 3: Receipt Screen & Persistent Slip:** Update `ReceiptScreen.kt` and `ReceiptCard.kt` to persistently draw the perforated slip with minimal line-art illustration when empty.
* **Phase 4: Insight Screen & Settings Cards:** Implement Canvas ghost tracks in `MonthSparkline.kt` / `YearBarChart.kt`, grouped card container in `CategoryBar.kt`, and grouped cards in `SettingsScreen.kt`.
* **Verification Gate:** Run `./gradlew assembleDebug testDebugUnitTest lintDebug` and screenshot tests after each phase to guarantee zero regressions.
