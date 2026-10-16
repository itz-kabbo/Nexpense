# Mote. for Android — AI Agent Build Guide

*Prepared October 15, 2026 from the public App Store listing and the developer’s website. Screenshots, preview videos and the app binary were not accessible, so pixel-level details are flagged for verification.*

## 0. How to use this guide

**At a glance:** native Android, Kotlin + Jetpack Compose, Room (SQLite), Hilt, DataStore, and a pure-Kotlin natural-language parser. Offline only, no account, no analytics, no INTERNET permission. Target APK under about 6 MB.

This document is meant to be handed to a coding agent (Claude Code or similar) and read by you. It has three layers:

1. **Spec** (sections 1–4): what the original does, what to build, and the stack.
2. **Blueprint** (sections 5–7): architecture, data model, and the parser, which is the app’s core.
3. **Runbook** (sections 8–10): milestones with acceptance checks, copy-paste agent prompts, and a parity checklist for matching the iOS app.

Evidence labels used throughout:

- **\[Confirmed\]** stated on the App Store listing or the developer’s site.
- **\[Inferred\]** reasoned from those sources (app size, OS requirements, wording). Likely, not proven.
- **\[Verify\]** a design or behavior detail that cannot be known without using the original. Capture it with the worksheet in section 10 before polishing.

Rules for the agent:

- Build one milestone at a time. After each, run the build and tests, then commit.
- Do not add features, permissions, SDKs or network calls that this guide does not list.
- When a line is marked \[Verify\], implement the stated default, leave a `// VERIFY:` comment, and record it in `PARITY.md`.
- Prefer fewer dependencies. The original is 3.3 MB; aim for an APK under about 6 MB after R8.

## 1. What the original app is (evidence)

### 1.1 Confirmed facts

| Item | Value |
| --- | --- |
| Name / tagline | Mote. — “Minimal Expense Tracker” |
| Developer | Yun Ju Tseng (site branded “Rice Tseng”), solo indie |
| Category / price | Finance, free, no ads, no analytics |
| Size | 3.3 MB |
| Platforms | iPhone and iPad; also runs on Apple-silicon Mac and Apple Vision. Requires iOS/iPadOS 18, macOS 15, visionOS 2 |
| Languages | English, Traditional Chinese |
| Account | None. The device’s Apple account is used only for iCloud sync |
| Data | Stored on the device; optional sync through the user’s personal iCloud; the developer runs no server and collects nothing |
| Age rating | 4+ |

Core features as the listing and site state them:

- Swipe up and type what you spent in plain words: “6 coffee”, “18 taxi”, “42 dinner”, “12.5 lunch”.
- The app reads the amount and sorts the entry into a category automatically. No forms, no setup.
- A **daily receipt**: a day’s entries totalled and shown like a printed shopping slip.
- **Insights**: “Today, this month, this year. One number up top, and the categories behind it.” Summaries by category, month and year.
- Light and dark mode. Clean, distraction-free design.
- Explicit non-goals: no budgeting pressure, no spreadsheets, no complicated dashboards.
- Records can be edited or deleted inside the app.

Brand and feel (site copy): “Every expense is a mote. Small alone. Meaningful together.” The site uses a white theme color (#ffffff), a short animated splash, and three demo clips named record, receipt and insight, which map to the app’s three main experiences.

### 1.2 Inferences

- **Native SwiftUI with SwiftData/CloudKit \[Inferred\].** The size, one binary reaching iPad, Mac and Vision, the iOS 18 floor and the iCloud sync all point to a pure SwiftUI app on Apple’s persistence stack.
- **Rule-based parsing, no ML model \[Inferred\].** At 3.3 MB there is no room for a bundled language model. Amount extraction plus a keyword dictionary (English and Traditional Chinese) fits the size and the examples.
- **One currency, device-locale formatting \[Inferred\].** Nothing mentions conversion or multiple currencies.
- **Expense-only \[Inferred\].** No income, budgets, accounts or recurring items are mentioned.

### 1.3 What could not be accessed

The screenshots and the three preview videos did not load as readable content, and an iOS binary cannot be inspected from here. Exact colors, type sizes, spacing, animation timing, haptics, empty states and category names are therefore **proposals** to be checked against a real iPhone (section 10). Do not claim pixel parity until that check is done.

## 2. Product spec for the Android version

### 2.1 Principles

1. **Zero friction.** From app open to a saved expense in under 3 seconds and two gestures.
2. **Quiet.** No badges, streaks, nudges, upsells or red numbers.
3. **Private by construction.** No account, no INTERNET permission in v1, no analytics, no ads. The Play “Data safety” form can truthfully say no data is collected or shared.
4. **Small and fast.** Cold start under 400 ms on a mid-range phone; APK under about 6 MB.

### 2.2 v1 scope (parity)

| # | Feature | Notes |
| --- | --- | --- |
| F1 | Quick entry by swipe-up sheet | One text field, keyboard opens instantly |
| F2 | Natural-language parsing | Amount plus description, either order |
| F3 | Auto-categorization | Dictionary in English and Traditional Chinese; learns from user corrections |
| F4 | Today view | Today’s total and entries |
| F5 | Daily receipt | Any past day, receipt-styled, shareable as an image |
| F6 | Insights | Day / Month / Year, one big total, category breakdown |
| F7 | Edit and delete | Tap to edit, swipe to delete with undo |
| F8 | Light / dark / system theme | Follows the system by default |
| F9 | Localization | English, Traditional Chinese |
| F10 | Backup and portability | Android Auto Backup plus manual JSON/CSV export and import |

### 2.3 Out of scope for v1

Budgets, income, bank or SMS reading, receipt scanning, currency conversion, accounts, cloud sync, social features, subscriptions, ads, analytics.

### 2.4 Platform differences to accept

- iCloud sync does not exist on Android. Auto Backup and file export cover v1. Cross-device sync is a later option (section 12) and will not interoperate with the iOS app’s iCloud store.
- The original is “Designed for iPad”. Android should adapt to tablets and foldables with window size classes, but the phone layout is the reference.

## 3. UX and UI spec

Every value below is a **proposed default \[Verify\]** unless the wording is quoted from the site.

### 3.1 Information architecture

Three experiences, matching the site’s sections Record, Receipt and Insight, plus a small Settings page.

- **Record (home):** today’s total, today’s entries, and the swipe-up entry handle.
- **Receipt:** the receipt for a chosen day.
- **Insight:** Day / Month / Year summaries.
- **Settings:** reached from a quiet icon on Record.

Default navigation: a horizontal pager with three pages (Record, Receipt, Insight) and a tiny three-dot indicator, with no bottom navigation bar. If the screenshots show something else, switch to it; the screens themselves do not change. \[Verify\]

### 3.2 Screens

**Splash.** Use the SplashScreen API (core-splashscreen) with the app mark on the theme background, then a 600–800 ms fade and scale of the wordmark into Record. No video; the original’s splash clip is the developer’s asset.

**Record screen.**

- Top: a small date label (“Thursday, Oct 15”) and a settings icon at the far end.
- Center: the day’s total as a large number (about 64sp, light weight), with the currency symbol smaller and baseline-aligned.
- Below: today’s entries, newest first. Each row has the description on the left, the category as quiet secondary text beneath it, and the amount on the right in tabular figures. Dividers no heavier than 0.5dp.
- Empty state: one line of secondary text such as “Nothing yet. Swipe up.”
- Bottom: a thin handle (36 × 4dp pill) with a short label. Dragging it up, or tapping it, opens the entry sheet.

**Entry sheet (the signature interaction).**

1. Drag the handle up; the sheet follows the finger with spring physics and snaps open past about 30% travel or on a fling.
2. The text field is focused and the keyboard rises in the same motion. Request focus as soon as the sheet’s target state becomes expanded, not after it settles, so there is no visible two-step.
3. As you type, a live preview line shows the parsed result: amount, cleaned description and a category chip. Typing “6 coffee” shows “6.00 · coffee · Food & Drink”.
4. The keyboard action is Done. On submit the entry is saved, a light confirm haptic fires, the sheet closes, and the new row slides into the list.
5. Tapping the category chip opens a compact picker. The choice is saved and remembered for that description.
6. Invalid input (no number found): the preview shows “Add an amount, e.g. 6 coffee” in secondary text and Done does nothing. No error dialogs.

**Receipt screen.**

- A paper card (light) or off-black card (dark) centered on the background, with perforated top and bottom edges drawn in Canvas.
- Monospaced type. Header: app name and the date. Body: one line per entry with leader dots between description and amount. A dashed rule, then TOTAL in larger type, then a short footer line.
- Swipe left or right, or use a date strip, to change the day. A share icon exports the receipt as a PNG.
- Optional print animation on open: lines revealed top to bottom over about 500 ms. \[Verify\]

**Insight screen.**

- Segmented control: Day / Month / Year. Default Month.
- One large total at the top for the selected period, with a small previous/next stepper beneath it.
- Below: category rows sorted by amount, each with name, amount, percentage and a thin proportional bar. No pie chart by default; the site promises “one number up top, and the categories behind it”.
- Year mode adds a minimal 12-bar chart of monthly totals. Month mode may add a day-by-day sparkline. \[Verify\]

**Edit sheet.** Tap a row to edit amount, description, category and date/time. Swipe a row to delete; a snackbar offers Undo for 5 seconds.

**Settings.** Appearance (System / Light / Dark), Language (System / English / 繁體中文), Currency (defaults to the device locale), Categories (rename, reorder, hide, add), Export / Import, About and privacy.

### 3.3 Design tokens (proposed) \[Verify\]

The site’s theme color is #ffffff, which suggests a pure white light background.

| Token | Light | Dark |
| --- | --- | --- |
| background | #FFFFFF | #000000 |
| surface (sheets) | #F6F6F4 | #121212 |
| paper (receipt) | #FBFAF6 | #1A1A1A |
| text primary | #111111 | #F2F2F2 |
| text secondary | #8A8A8E | #8E8E93 |
| hairline | #E5E5EA | #2C2C2E |
| accent | none; monochrome by default | none |

- **Type.** UI: a neutral grotesque such as Inter, or the system font at zero size cost. Do not ship Apple’s SF fonts; their license limits them to Apple platforms. Numbers: tabular figures (the `tnum` font feature). Receipt: a monospace such as IBM Plex Mono or JetBrains Mono, subset to digits, Latin and punctuation, with the system monospace as the Chinese fallback.
- **Shape.** Sheets 28dp top radius, chips fully rounded, cards 16dp.
- **Spacing.** 4dp grid; 24dp horizontal screen padding.
- **Motion.** Sheet spring damping about 0.85 with medium stiffness; content fades 150–200 ms; list insert 250 ms.
- **Haptics.** `HapticFeedbackType.Confirm` on save (API 30+, with a fallback to `TextHandleMove`), `SegmentTick` on segment change, nothing else. Check that these constants exist in your Compose version.
- **Icons.** Vector drawables exported from Material Symbols (rounded, weight 300), only the few used: settings, share, chevrons, close.
- **No dynamic color.** Turn Material You off so the monochrome look is not recolored by the wallpaper.

### 3.4 Android-specific requirements

- **Edge-to-edge is mandatory** when targeting API 36: draw behind the system bars and handle insets.
- **Predictive back:** use `PredictiveBackHandler` or `BackHandler` for the entry and edit sheets; the legacy `onBackPressed` path is not called at API 36.
- **Accessibility:** TalkBack labels on the handle (“Add expense”) and on receipt rows; no clipping at 200% font scale; 4.5:1 contrast for secondary text; 48dp touch targets (the visible handle can be thin, its hit area cannot).
- **Large screens:** use window size classes. On medium and expanded widths show Record and Receipt side by side and present the entry sheet as a centered, dialog-width sheet.

## 4. Stack decision

### 4.1 Recommended stack

| Layer | Choice | Why |
| --- | --- | --- |
| Language | Kotlin 2.x | Standard for Android; coroutines and Flow fit this app |
| UI | Jetpack Compose, Material 3 used only as a base theme | Gestures, springs, a Canvas receipt and haptics are far easier than in Views |
| Architecture | MVVM with unidirectional data flow (`ViewModel` + `StateFlow`) | Small app, easy to test |
| DI | Hilt | Official, little boilerplate. Koin is fine if you want fewer annotation processors |
| Database | Room (via KSP) over SQLite | Typed queries, migrations, Flow support |
| Settings | Preferences DataStore | Theme, language, currency, first-run flag |
| Parser | Pure Kotlin Gradle module, no Android imports | Unit-testable, reusable on other platforms later |
| Charts | Hand-drawn in Compose Canvas | Keeps the app tiny; no charting dependency |
| Background work | WorkManager, only if you add a daily reminder | Not needed for parity |
| Localization | Android resources `values/` and `values-zh-rTW/`, plus `localeConfig` | Native per-app language on Android 13+, AppCompat backport below |
| Backup | Auto Backup rules plus manual export/import through the Storage Access Framework | No network, no account |
| Testing | JUnit, Turbine, Compose UI tests, screenshot tests (Roborazzi or Paparazzi) | Catches visual drift from the spec |
| Build | Gradle Kotlin DSL, version catalog, R8 full mode | Small APK |

**SDK levels.** `compileSdk` and `targetSdk` 36. Google Play has required new apps and updates to target Android 16 (API 36) or higher since August 31, 2026. `minSdk` 26 keeps `java.time` available without desugaring and still covers the large majority of active devices. Resolve every library to its **latest stable** release when you build; never pin alpha or beta versions.

### 4.2 Why not the alternatives

- **Flutter or React Native.** They would give an identical look on both platforms, but add several MB of runtime, and the swipe-up sheet, haptics and keyboard choreography are where cross-platform layers usually feel off. The remake is Android-only, so native wins.
- **Kotlin Multiplatform.** Worth it only if you later want an iOS clone sharing logic. Keep the parser in a pure-Kotlin module so that stays possible, but do not add KMP now.
- **Views/XML.** Slower to build the custom receipt, sheet and motion.

### 4.3 Manifest and permission rules

- No `INTERNET` permission in v1. This keeps the privacy claim true. Add a build check that fails if the merged release manifest contains it.
- No storage permissions; exports use the system file picker.
- `android:allowBackup="true"` with explicit `dataExtractionRules` and `fullBackupContent` so the database is backed up.
- `android:localeConfig` pointing to the supported languages.

## 5. Architecture and project layout

Two Gradle modules: `:app` (Android) and `:parser` (pure Kotlin/JVM, no Android imports). Inside `:app`, package by feature. Use your own package and app name everywhere; `com.yourco.spend` and `AppTheme` below are placeholders.

```text
project/
├─ settings.gradle.kts
├─ gradle/libs.versions.toml
├─ AGENTS.md                      # agent rules (section 9)
├─ PARITY.md                      # every [Verify] decision and its status
├─ parser/
│  └─ src/
│     ├─ main/kotlin/.../parser/   ExpenseParser.kt, Lexicon.kt
│     └─ test/kotlin/.../parser/   ExpenseParserTest.kt (table-driven, Appendix A)
└─ app/src/main/
   ├─ AndroidManifest.xml
   ├─ res/values/, res/values-zh-rTW/
   ├─ res/xml/ locales_config.xml, backup_rules.xml, data_extraction_rules.xml
   └─ kotlin/.../
      ├─ App.kt                    # @HiltAndroidApp
      ├─ MainActivity.kt           # edge-to-edge, root composable
      ├─ core/designsystem/        # Color, Type, Shape, Motion, AppTheme, components/
      ├─ core/money/               # formatMinor, fraction digits
      ├─ core/time/                # Clock provider, date-range helpers
      ├─ data/db/                  # Database, entities, DAOs, migrations
      ├─ data/repo/                # Expense, Category and Settings repositories
      ├─ data/export/              # JSON, CSV, import
      ├─ feature/record/           # RecordScreen, RecordViewModel, EntrySheet
      ├─ feature/receipt/          # ReceiptScreen, ReceiptViewModel, ReceiptCard, share
      ├─ feature/insight/          # InsightScreen, InsightViewModel, charts/
      ├─ feature/edit/             # EditSheet, CategoryPicker
      ├─ feature/settings/
      └─ navigation/               # AppRoot: pager plus settings route
```

**Data flow.** UI event, then ViewModel, then Repository, then a Room DAO returning `Flow`, then `StateFlow<UiState>` back to the composable. Composables are stateless: they take a `UiState` and lambdas.

**Rules the agent must follow**

- Money is a `Long` in minor units. Never `Float` or `Double`.
- A day is stored as `local_date` text (for example 2026-10-15), computed in the device time zone at write time, so travel across time zones never moves history.
- Inject a `Clock` so tests control what “today” is.
- Deletes are soft (`deleted_at`) so Undo works and a future sync can merge.
- The parser runs on every keystroke on the main thread. It must stay allocation-light and finish in well under 1 ms.

## 6. Data model

### 6.1 Tables

**expense**

| Column | Type | Notes |
| --- | --- | --- |
| id | TEXT, primary key | UUID v4 string, so a future sync can merge |
| amount\_minor | INTEGER | Minor units; 12.50 is stored as 1250 |
| currency | TEXT | ISO 4217 code in force when the entry was made |
| note | TEXT | Cleaned description, for example coffee |
| raw\_text | TEXT | Exactly what the user typed |
| category\_id | TEXT | Stable key such as food |
| created\_at | INTEGER | Epoch milliseconds (UTC) |
| local\_date | TEXT | Day in the device time zone, ISO format |
| updated\_at | INTEGER | Epoch milliseconds |
| deleted\_at | INTEGER, nullable | Soft delete for Undo and future sync |

Indices: `(local_date)` and `(category_id, local_date)`.

**category:** `id` (stable key such as `food`), `name_override` (nullable; set only when the user renames a category or creates one), `icon`, `sort_order`, `is_hidden`, `is_builtin`. Built-in display names come from string resources (for example `category_food`) so localization works.

**learned\_mapping:** `normalized_note` (primary key), `category_id`, `hits`, `updated_at`. Written when the user corrects a category; read by the parser.

**Settings (DataStore):** `theme_mode`, `language_tag`, `currency_code`, `first_run_done`.

### 6.2 Built-in categories \[Verify\]

The original’s category list is not public. Seed with: Food & Drink (`food`), Groceries (`groceries`), Transport (`transport`), Shopping (`shopping`), Home & Bills (`home`), Health (`health`), Fun (`fun`), Travel (`travel`), Other (`other`). Rename to match the screenshots you capture in section 10.

### 6.3 Code skeleton

```kotlin
@Entity(
    tableName = "expense",
    indices = [Index("local_date"), Index(value = ["category_id", "local_date"])],
)
data class ExpenseEntity(
    @PrimaryKey val id: String,
    @ColumnInfo(name = "amount_minor") val amountMinor: Long,
    val currency: String,
    val note: String,
    @ColumnInfo(name = "raw_text") val rawText: String,
    @ColumnInfo(name = "category_id") val categoryId: String,
    @ColumnInfo(name = "created_at") val createdAt: Long,
    @ColumnInfo(name = "local_date") val localDate: String,
    @ColumnInfo(name = "updated_at") val updatedAt: Long,
    @ColumnInfo(name = "deleted_at") val deletedAt: Long? = null,
)

data class CategoryTotal(
    @ColumnInfo(name = "category_id") val categoryId: String,
    val total: Long,
    val count: Int,
)

data class BucketTotal(val bucket: String, val total: Long)

@Dao
interface ExpenseDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsert(e: ExpenseEntity)

    @Query("UPDATE expense SET deleted_at = :now WHERE id = :id")
    suspend fun softDelete(id: String, now: Long)

    @Query("UPDATE expense SET deleted_at = NULL WHERE id = :id")
    suspend fun restore(id: String)

    @Query("SELECT * FROM expense WHERE local_date = :date AND deleted_at IS NULL ORDER BY created_at DESC")
    fun observeDay(date: String): Flow<List<ExpenseEntity>>

    @Query("SELECT COALESCE(SUM(amount_minor), 0) FROM expense WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL")
    fun observeTotal(from: String, to: String): Flow<Long>

    @Query(
        "SELECT category_id, SUM(amount_minor) AS total, COUNT(*) AS count FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL " +
            "GROUP BY category_id ORDER BY total DESC"
    )
    fun observeByCategory(from: String, to: String): Flow<List<CategoryTotal>>

    // len = 7 groups by month (yyyy-MM), len = 10 groups by day
    @Query(
        "SELECT substr(local_date, 1, :len) AS bucket, SUM(amount_minor) AS total FROM expense " +
            "WHERE local_date BETWEEN :from AND :to AND deleted_at IS NULL GROUP BY bucket ORDER BY bucket"
    )
    fun observeBuckets(from: String, to: String, len: Int): Flow<List<BucketTotal>>
}
```

ISO date strings sort the same way dates do, so `BETWEEN` on `local_date` is correct and index-friendly.

Money formatting (minor units to a localized string):

```kotlin
fun formatMinor(minor: Long, currencyCode: String, locale: Locale): String {
    val cur = Currency.getInstance(currencyCode)
    val digits = cur.defaultFractionDigits.coerceAtLeast(0)
    val nf = NumberFormat.getCurrencyInstance(locale)
    nf.currency = cur
    return nf.format(BigDecimal.valueOf(minor, digits))
}
```

## 7. The natural-language parser (core)

### 7.1 Contract

Input: the raw text and the number of fraction digits for the active currency (`Currency.defaultFractionDigits`, floored at 0). Output: `ParseResult.Ok(amountMinor, note, categoryId, confidence)` or `ParseResult.NoAmount`. It is a pure function of its input plus an optional `learned` lookup, so it is trivial to test.

### 7.2 Rules

1. **Normalize.** Unicode NFKC (turns full-width “１２．５” into “12.5” and the full-width comma into a plain comma), strip currency marks (`$ ¥ € £ ₩ NT$ US$ 元 塊`), collapse spaces.
2. **Find amount candidates.** Digits with optional thousands separators and one optional decimal part. A number glued to letters or to a letter-and-hyphen (“7-eleven”, “3m”, “A5”) is not an amount.
3. **Pick the amount.** A candidate at the very start wins, then one at the very end, otherwise the first. So “6 coffee” and “coffee 6” are identical. Everything else becomes the note.
4. **Separators.** A dot is the decimal point. A comma followed by exactly three digits is a thousands separator (“1,200”). A comma followed by one or two digits is a decimal comma (“12,5”).
5. **Round** half-up to the currency’s fraction digits. Reject zero, negatives and anything above 10,000,000.
6. **Clean the note.** Trim punctuation at the ends, keep the user’s wording. If it is empty the category name is shown instead.
7. **Resolve the category, in this order:** (a) the learned mapping for the lowercased note, (b) the longest dictionary keyword found in the note, (c) a fuzzy match at edit distance 1 for words of 5+ letters, (d) `other` with low confidence. English keywords match on word boundaries so “tea” does not fire inside “steak”; Chinese keywords match as substrings because Chinese has no spaces.

### 7.3 Reference implementation (pure Kotlin)

```kotlin
enum class Confidence { HIGH, MEDIUM, LOW }

data class ParsedExpense(
    val amountMinor: Long,
    val note: String,
    val categoryId: String,
    val confidence: Confidence,
)

sealed interface ParseResult {
    data class Ok(val value: ParsedExpense) : ParseResult
    data object NoAmount : ParseResult
}

class ExpenseParser(
    private val lexicon: Lexicon = Lexicon.default,
    private val learned: (normalizedNote: String) -> String? = { null },
) {
    private val currencyMarks = Regex("""(?:NT|US)?[$¥€£₩]|[元塊]""", RegexOption.IGNORE_CASE)
    private val spaces = Regex("""[ ]+""")
    private val number = Regex(
        """(?<![A-Za-z0-9])(?<![A-Za-z]-)""" +
            """([0-9]{1,3}(?:,[0-9]{3})+|[0-9]+)(?:[.,]([0-9]{1,2}))?""" +
            """(?![A-Za-z0-9])(?!-[A-Za-z])"""
    )

    fun parse(input: String, fractionDigits: Int = 2): ParseResult {
        val text = Normalizer.normalize(input, Normalizer.Form.NFKC)
            .replace(currencyMarks, " ")
            .replace(spaces, " ")
            .trim()
        val hits = number.findAll(text).toList()
        val hit = hits.firstOrNull { it.range.first == 0 }
            ?: hits.firstOrNull { it.range.last == text.lastIndex }
            ?: hits.firstOrNull()
            ?: return ParseResult.NoAmount

        val whole = hit.groupValues[1].replace(",", "")
        val frac = hit.groupValues[2]
        val value = BigDecimal(if (frac.isEmpty()) whole else "$whole.$frac")
            .setScale(fractionDigits, RoundingMode.HALF_UP)
        if (value.signum() <= 0 || value > BigDecimal(10_000_000)) return ParseResult.NoAmount
        val minor = value.movePointRight(fractionDigits).longValueExact()

        val note = text.removeRange(hit.range)
            .replace(spaces, " ")
            .trim(' ', ',', ';', '-', '.')
        val (categoryId, confidence) = resolve(note.lowercase())
        return ParseResult.Ok(ParsedExpense(minor, note, categoryId, confidence))
    }

    private fun resolve(note: String): Pair<String, Confidence> {
        if (note.isBlank()) return "other" to Confidence.LOW
        learned(note)?.let { return it to Confidence.HIGH }
        val padded = " $note "
        lexicon.keywords.entries
            .filter { (key, _) -> if (key.isCjk()) note.contains(key) else padded.contains(" $key ") }
            .maxByOrNull { it.key.length }
            ?.let { return it.value to Confidence.HIGH }
        for (word in note.split(' ')) {
            if (word.length < 5) continue
            lexicon.keywords.entries
                .firstOrNull { it.key.length >= 5 && !it.key.isCjk() && editDistance(word, it.key) <= 1 }
                ?.let { return it.value to Confidence.LOW }
        }
        return "other" to Confidence.LOW
    }

    private fun String.isCjk() = any { it.code >= 0x2E80 }
}

internal fun editDistance(a: String, b: String): Int {
    if (kotlin.math.abs(a.length - b.length) > 1) return 2
    val dp = IntArray(b.length + 1) { it }
    for (i in 1..a.length) {
        var prev = dp[0]
        dp[0] = i
        for (j in 1..b.length) {
            val tmp = dp[j]
            dp[j] = minOf(dp[j] + 1, dp[j - 1] + 1, prev + if (a[i - 1] == b[j - 1]) 0 else 1)
            prev = tmp
        }
    }
    return dp[b.length]
}

class Lexicon(val keywords: Map<String, String>) {
    companion object {
        val default = Lexicon(
            buildMap<String, String> {
                fun add(category: String, vararg words: String) = words.forEach { put(it, category) }
                add("food", "coffee", "latte", "tea", "boba", "breakfast", "brunch", "lunch", "dinner",
                    "snack", "pizza", "burger", "ramen", "restaurant", "beer", "wine", "uber eats",
                    "咖啡", "拿鐵", "茶", "早餐", "午餐", "晚餐", "宵夜", "便當", "飲料", "手搖", "餐廳")
                add("groceries", "grocery", "groceries", "supermarket", "超市", "水果", "全聯")
                add("transport", "taxi", "uber", "bus", "train", "metro", "subway", "mrt", "gas", "fuel",
                    "parking", "toll", "計程車", "公車", "捷運", "高鐵", "火車", "加油", "停車", "過路費")
                add("shopping", "clothes", "shoes", "amazon", "gift", "衣服", "禮物", "網購")
                add("home", "rent", "electricity", "wifi", "utilities", "bill", "房租", "電費", "水費", "帳單")
                add("health", "doctor", "pharmacy", "medicine", "gym", "dentist", "看醫生", "健身", "牙醫", "診所")
                add("fun", "movie", "cinema", "game", "concert", "netflix", "spotify", "電影", "遊戲", "演唱會", "訂閱")
                add("travel", "flight", "hotel", "airbnb", "hostel", "機票", "飯店", "住宿", "旅行")
            }
        )
    }
}
```

The lexicon is a seed. Grow it from real usage and prefer longer keys to single characters, which cause false positives. Imports needed: `java.math.BigDecimal`, `java.math.RoundingMode`, `java.text.Normalizer`.

### 7.4 Optional multi-item entry \[Verify\]

Whether the original accepts several items in one entry is unknown. If you add it, split on newlines, semicolons, Chinese commas, and an ASCII comma followed by a space. Never split on a bare comma, because that breaks “1,200 rent” and “12,5 pizza”.

```kotlin
private val itemSeparator = Regex("""[;；，]|,[ ]+""")

fun ExpenseParser.parseMany(input: String, fractionDigits: Int = 2): List<ParsedExpense> =
    input.lines()
        .flatMap { it.split(itemSeparator) }
        .map { it.trim() }
        .filter { it.isNotEmpty() }
        .mapNotNull { (parse(it, fractionDigits) as? ParseResult.Ok)?.value }
```

### 7.5 Where “AI” fits

The original is almost certainly rule-based \[Inferred\]. Keep parsing deterministic and instant. If you later want smarter categories, put the dictionary behind a `CategoryClassifier` interface and add an optional on-device model (for example Gemini Nano on supported devices) as a fallback for low-confidence results only. Never send expense text off the device; that would break the privacy model.

## 8. Runbook, part 1: milestones M0 to M5

Each milestone lists the goal, the tasks, and a **Done when** gate. The agent must pass the gate before starting the next one.

### M0 — Bootstrap

**Goal:** an empty, themed app that builds in CI.

- Create the project (Kotlin DSL, Compose), package `com.yourco.spend`, `minSdk 26`, `compileSdk` and `targetSdk` 36. Add the `:parser` JVM module.
- Fill `gradle/libs.versions.toml` with the latest **stable** releases of: Android Gradle Plugin, Kotlin, KSP, Compose BOM, Material 3, lifecycle (runtime-compose, viewmodel-compose), activity-compose, Hilt and hilt-navigation-compose, Room (runtime, ktx, compiler), DataStore preferences, core-splashscreen, appcompat, coroutines, and test libraries (JUnit, Turbine, Compose UI test, Roborazzi).
- Release build: R8 minify and resource shrinking on. Add ktlint or Spotless.
- CI workflow running `./gradlew assembleDebug testDebugUnitTest lintDebug`.
- Add `AGENTS.md` (section 9) and an empty `PARITY.md`.
- Add a Gradle task that fails if the merged release manifest contains `android.permission.INTERNET`.

**Done when:** `assembleDebug` passes in CI, the app launches to a blank themed screen in light and dark, and the manifest check passes.

### M1 — Design system

**Goal:** tokens and primitives that every screen reuses.

- `Color.kt`, `Type.kt`, `Shape.kt`, `Motion.kt` from section 3.3; `AppTheme` exposing colors, type and spacing through CompositionLocals and mapping them onto `MaterialTheme`. Dynamic color off.
- Components: `BigAmount`, `EntryRow`, `CategoryChip`, `EntryHandle`, `LeaderRow`, `DashedRule`, `PerforatedShape`, `SegmentedControl`, `CategoryBar`.
- A debug-only gallery screen showing every component.

**Done when:** the gallery renders correctly in light, dark and at 2.0 font scale, and screenshot tests are recorded for it.

### M2 — Data layer

**Goal:** persistence and repositories, fully tested without UI.

- Entities, DAOs, `Database` (schema export on), migrations folder, category seeding on first run, `SettingsRepository` on DataStore, `Clock` injection, `formatMinor`.
- Repository methods: add, update, soft delete, restore, observe a day, observe totals, observe by category, observe buckets, learned-mapping read and write.

**Done when:** in-memory Room tests cover totals, category grouping, month and year buckets, soft delete and restore, and period boundaries.

### M3 — Parser

**Goal:** the core brain, written test-first.

- Write `ExpenseParserTest` from Appendix A first and watch it fail, then implement section 7.3 until it passes.
- Add a fuzz test (100,000 random strings never throw) and a micro-benchmark (10,000 parses in under 200 ms on the JVM).

**Done when:** every Appendix A row passes and the fuzz test is green.

### M4 — Record screen and entry sheet

**Goal:** the signature swipe-up, type, save loop.

- Use a standard bottom-sheet scaffold with a thin peek height, skipping the hidden state. Focus the text field the moment the sheet’s **target** state becomes expanded, so keyboard and sheet rise together. Compose API names move between releases; the agent must compile and adapt this skeleton.

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecordScreen(vm: RecordViewModel = hiltViewModel()) {
    val state by vm.state.collectAsStateWithLifecycle()
    val sheetState = rememberStandardBottomSheetState(
        initialValue = SheetValue.PartiallyExpanded,
        skipHiddenState = true,
    )
    val focus = remember { FocusRequester() }
    val keyboard = LocalSoftwareKeyboardController.current
    val scope = rememberCoroutineScope()
    val bottomInset = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()

    LaunchedEffect(sheetState) {
        snapshotFlow { sheetState.targetValue }.collect { target ->
            if (target == SheetValue.Expanded) focus.requestFocus() else keyboard?.hide()
        }
    }

    BottomSheetScaffold(
        scaffoldState = rememberBottomSheetScaffoldState(sheetState),
        sheetPeekHeight = 56.dp + bottomInset,
        sheetShape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
        sheetContainerColor = AppTheme.colors.surface,
        sheetDragHandle = { EntryHandle(onClick = { scope.launch { sheetState.expand() } }) },
        sheetContent = {
            EntrySheet(
                focusRequester = focus,
                onSaved = { scope.launch { sheetState.partialExpand() } },
            )
        },
        containerColor = AppTheme.colors.background,
    ) { padding -> TodayContent(state, Modifier.padding(padding)) }
}
```

- `EntryViewModel` exposes `text`, a `preview` (Valid with amount, note and category; NeedsAmount; Empty), `onTextChange`, `onCategoryPicked` and `submit()`. `submit()` computes `local_date` from the injected `Clock`, inserts the row, clears the text, and triggers the confirm haptic.
- Keep the typed text in `rememberSaveable` so rotation does not lose it. Handle back with `PredictiveBackHandler`: first back collapses the sheet.

**Done when:** a Compose UI test expands the sheet, types `6 coffee`, sees `6.00 · coffee · Food & Drink` in the preview, presses Done, and finds the new row; and on a physical device the sheet and keyboard rise as one motion.

### M5 — Today list, edit, delete, category learning

**Goal:** everything you can do to an entry after it exists.

- Today list bound to `observeDay`, total bound to `observeTotal`.
- Tap a row for the edit sheet (amount, description, category, date and time). Changing the date recomputes `local_date`.
- Swipe to delete with `SwipeToDismissBox`; a snackbar offers Undo for 5 seconds, which calls `restore`.
- Picking a different category upserts `learned_mapping` for that lowercased note, and the repository exposes the map as a `StateFlow` that the parser’s `learned` lambda reads.

**Done when:** DB and UI tests cover edit, delete, undo and learning (type `6 boba`, correct it to Groceries, type `9 boba` and see Groceries).

## 8b. Runbook, part 2: milestones M6 to M10

### M6 — Receipt

**Goal:** the daily receipt, the app’s most recognizable screen.

- `ReceiptViewModel(day)` exposes rows (note and formatted amount), the total, and previous/next day navigation. A horizontal pager over days works well, with today as the last page.
- Empty day: one line of secondary text, no card.
- `ReceiptCard` is a Column clipped to a perforated shape, filled with the `paper` token, in monospace. Skeleton for the two tricky parts (pass `toothPx` from `with(LocalDensity.current) { 6.dp.toPx() }`):

```kotlin
fun perforated(toothPx: Float) = GenericShape { size, _ ->
    val n = (size.width / toothPx).toInt().coerceAtLeast(1)
    val w = size.width / n
    moveTo(0f, 0f)
    for (i in 0 until n) {
        lineTo(i * w + w / 2, toothPx / 2)
        lineTo((i + 1) * w, 0f)
    }
    lineTo(size.width, size.height)
    for (i in n downTo 1) {
        lineTo(i * w - w / 2, size.height - toothPx / 2)
        lineTo((i - 1) * w, size.height)
    }
    close()
}

@Composable
fun LeaderRow(label: String, amount: String, style: TextStyle, modifier: Modifier = Modifier) {
    val dotColor = AppTheme.colors.textSecondary
    Row(modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
        Text(
            label, style = style, maxLines = 1, overflow = TextOverflow.Ellipsis,
            modifier = Modifier.weight(1f, fill = false),
        )
        Box(
            Modifier
                .weight(1f)
                .padding(horizontal = 6.dp)
                .height(14.dp)
                .drawBehind {
                    val y = size.height - 3.dp.toPx()
                    drawLine(
                        color = dotColor,
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1.dp.toPx(),
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(1.dp.toPx(), 4.dp.toPx())),
                    )
                },
        )
        Text(amount, style = style, maxLines = 1)
    }
}
```

- **Share as image.** Record the card with `rememberGraphicsLayer()`, convert to a bitmap, write a PNG into `cacheDir`, expose it through a `FileProvider`, and start an `ACTION_SEND` chooser with `image/png`. No storage permission needed.
- **Print animation (optional).** Reveal rows top to bottom with a 40 ms stagger, capped at about 500 ms total, and skip it when the system animator scale is 0.
- Very long days: the card scrolls inside the screen; the total stays on the card, not pinned.

**Done when:** receipt totals equal the database sums for sample days, sharing produces a PNG that opens in the gallery, and screenshot tests exist for light, dark and a 50-row receipt.

### M7 — Insight

**Goal:** Day / Month / Year summaries with one number and its categories.

- Period ranges as ISO `local_date` strings with `java.time`: Day is `[d, d]`; Month is `YearMonth.atDay(1)` to `atEndOfMonth()`; Year is `Year.atDay(1)` to `atDay(Year.length())`.
- `InsightViewModel(mode, anchor)` combines `observeTotal` and `observeByCategory`; Year adds `observeBuckets(len = 7)` for the monthly bars, Month adds `observeBuckets(len = 10)` for the daily sparkline.
- UI: segmented control, big total, previous/next stepper (next disabled beyond the current period), category rows with a thin proportional bar and whole-number percentage, a hand-drawn 12-bar chart in Year mode. Highlight the current month. Empty period: one line of secondary text.

**Done when:** unit tests cover Feb 29, Dec 31 to Jan 1, and months of 28, 30 and 31 days; category totals sum to the period total; charts handle all-zero and single-value data without dividing by zero.

### M8 — Settings, localization, export and backup

**Goal:** everything around the core loop.

- **Appearance:** System / Light / Dark stored in DataStore and fed to `AppTheme`.
- **Language:** `res/xml/locales_config.xml` listing `en` and `zh-TW`, referenced by `android:localeConfig`; switch with `AppCompatDelegate.setApplicationLocales` (the AppCompat backport covers pre-13 devices). Every user-visible string, including category names and receipt labels, lives in `values/strings.xml` and `values-zh-rTW/strings.xml`.
- **Currency:** default from the device locale with a fallback of USD; override in Settings. Each row keeps its own currency code, and the DAO queries gain `AND currency = :currency` so totals never mix units. Changing currency affects new entries only. \[Verify\]
- **Export and import:** JSON with a `schema` version number, categories and expenses; CSV with date, time, amount, currency, category and note. Use `CreateDocument` and `OpenDocument` contracts. Import de-duplicates by `id` and never overwrites a row with a newer `updated_at`.
- **Backup:** `data_extraction_rules.xml` and `backup_rules.xml` include the Room database and DataStore files and exclude caches. Test with `adb shell bmgr`.

**Done when:** switching language applies without a restart, export then wipe then import restores identical rows (round-trip test), and the backup rules pass lint.

### M9 — Polish and platform fit

**Goal:** make it feel as good as it looks.

- Splash via core-splashscreen; motion and haptics per section 3.3; edge-to-edge insets on every screen; predictive back on both sheets.
- Accessibility: content descriptions, merged row semantics that read as one phrase (for example, coffee, Food & Drink, 6.00), 200% font scale, contrast check, 48dp targets.
- Large screens: window size classes with the layouts from section 3.4.
- Midnight rollover: if the app is open when the date changes, Today must flip. Listen for the date-changed broadcast or drive “today” from a ticking flow.
- Performance: generate a Baseline Profile with Macrobenchmark; confirm cold start of 400 ms or better on a mid-range device; R8 full mode; inspect the APK in Android Studio’s APK Analyzer and drop unused fonts, icons and locales.

**Done when:** accessibility lint is clean, size and startup targets are met, screenshot tests are green, and one small phone, one large phone and one tablet have been checked by hand.

### M10 — QA and release

**Goal:** ship a build you trust.

- Test matrix: emulators for API 26, 30, 34 and 36 plus one physical device. Cases: first run, empty states, midnight rollover, time-zone change, locale change, dark mode switch, process death with the sheet open, and a 5,000-row database (smooth scroll, Insight updates in under 100 ms).
- Release: Play App Signing, an Android App Bundle, version policy, store screenshots made from your own UI, a privacy policy URL, the Data safety form (no data collected or shared, which stays true only while the no-INTERNET rule holds), content rating, and a not-for-kids audience setting. New personal developer accounts may have closed-testing requirements before production; check the Play Console for what applies now.

**Done when:** an internal-testing build installed from Play on a clean device passes the smoke test: first run, add three entries, view receipt, view insight, export, switch language.

## 9. Agent operating kit

### 9.1 `AGENTS.md` for the repository root

```markdown
# Agent rules
- Source of truth: docs/GUIDE.md. Do not invent features.
- Work one milestone at a time (M0 to M10). Do not start the next until its Done-when gate passes.
- After every milestone run ./gradlew assembleDebug testDebugUnitTest lintDebug, then commit as M<n>: <title>.
- No INTERNET permission, no analytics or ad SDKs, no network calls. Ask before adding any dependency the guide does not list.
- Money is Long minor units. Never Float or Double.
- Every user-visible string lives in strings.xml (en and zh-rTW).
- For each [Verify] item: implement the default, leave a // VERIFY: comment, add a row to PARITY.md.
- If an API in the guide does not compile on the installed versions, adapt it and record the change in DECISIONS.md.
- Stop and ask the human when a spec line contradicts itself, a gate cannot be met, or a decision would change permissions, data format or the stack.
```

### 9.2 Kickoff prompt

> You are building an Android expense tracker from docs/GUIDE.md. Read the whole guide, then AGENTS.md. Reply with five lines: the stack, the three screens, the parser contract, the privacy rules, and the milestone order. Then do M0 only. When its Done-when gate passes, report the commands you ran, their final output, the files you changed and any open \[Verify\] items, and wait for `go` before M1.

### 9.3 Per-milestone prompt

> Implement milestone M\<n> from the guide. First list the files you will create or change. Write tests first where the guide says so. Run the full check command and paste the last lines of its output. Finish with (1) the Done-when checklist marked pass or fail per item, (2) any deviation from the guide and why, (3) new \[Verify\] items added to PARITY.md.

### 9.4 Parity-pass prompt (after section 10)

> Here are screenshots, frame counts and a measurement table captured from the original app on an iPhone. Update the design tokens, copy, spacing and motion to match them. Change only the files named in the table’s Goes-into column. Re-record the screenshot tests and show a before and after for each screen. Do not copy any image, icon, font or marketing text from the original.

### 9.5 Working tips

- Export this doc as Markdown into `docs/GUIDE.md` so the agent reads the same text you do.
- Give the agent a running emulator (or `adb` to a device) and let it take screenshots after each UI milestone.
- Keep commits to one per milestone so any step is easy to roll back.
- After every milestone, glance at the diff for new dependencies and new permissions. Both are easy to miss and hard to undo later.

## 10. Parity: capturing what could not be read

This is the human step that turns the \[Verify\] proposals into facts. You need the original running on an iPhone (it also runs on Apple-silicon Macs).

1. **Record at 60 fps** (Control Center screen recording): launch and splash; swipe-up entry with several examples including a bad one; saving; the Today list; the receipt (opening, changing day, sharing if offered); each Insight segment and period change; edit and delete; settings. Repeat in dark mode and with large text.
2. **Screenshot every state** in light and dark.
3. **Extract frames** to measure motion: `ffmpeg -i clip.mp4 -vf fps=60 frames/%04d.png`. Duration in ms is the number of frames between first movement and rest, divided by 60, times 1000.
4. **Fill in this worksheet** and give it to the agent with the images.

| Capture | How | Goes into |
| --- | --- | --- |
| Colors: background, surface, text, secondary, hairline, receipt paper | Color picker on screenshots, light and dark | `Color.kt` |
| Typefaces and sizes | Identify by look; measure cap height; iPhone points map about 1:1 to dp and sp | `Type.kt` |
| Spacing and corner radii | Measure in screenshots, points to dp | `Shape.kt`, components |
| Category names, order and icons | Open the category picker | seed list, `strings.xml` |
| Copy: placeholders, empty states, receipt header and footer | Transcribe, then rewrite in your own words | `strings.xml` |
| Number format | Decimals shown, grouping, symbol position | `formatMinor` |
| Navigation model | Pager, tabs or buttons; gesture to change day | `AppRoot` |
| Sheet behavior | Snap points, fling threshold, dismiss, keyboard timing | `RecordScreen` |
| Animation timing and curve | Frame counts; spring or ease | `Motion.kt` |
| Haptics | Feel each interaction: light, soft, success, selection | haptics map |
| Day rollover and week start | Change the device date and observe | clock handling |
| Edge cases | Huge amount, zero, no number, very long note, 100 entries | tests |

5. **Side-by-side loop.** Put the iPhone next to the Android device, record both, and compare frame by frame. Tolerances: spacing within 2dp, colors exact, timing within 40 ms.

When every row is filled and `PARITY.md` has no open items, parity is real rather than assumed.

## 11. Brand, legal and store notes

- **Re-create, don’t copy, the assets.** The name “Mote.”, logo, icon, splash video, screenshots, marketing copy and site text belong to the developer. Interaction patterns (swipe-up entry, receipt view, category totals) are ideas; exact assets and wording are not. Use your own name, icon and words.
- **If this is a personal or learning project,** none of the store concerns below apply. **If you plan to publish,** a near-identical name, icon and copy can get a listing removed under Google Play’s impersonation and spam policies, and it is unkind to a solo developer. Differentiate, and search Play and trademark databases before choosing a name.
- **Fonts:** Apple’s SF fonts are licensed for Apple platforms. Use Inter, Roboto or the system font, and check the license of anything you bundle.
- **Privacy policy and Data safety form** must match reality. “No data collected” is accurate only while the app has no INTERNET permission and no SDKs that phone home.
- **Play target level:** new apps and updates must target API 36 or higher. Check the Play Console for any other requirements current at release time.

## 12. Roadmap beyond parity

- Home-screen widget (Glance) and an app shortcut or Quick Settings tile that opens the entry sheet directly.
- Share target: share text from any app to create an entry.
- Optional daily reminder with WorkManager at a time the user picks.
- Optional sync through a JSON snapshot in Google Drive’s app data folder, merged by `id` and `updated_at` (already in the schema). It cannot sync with the iOS app’s iCloud store, but the export format can be shared.
- Voice entry through `SpeechRecognizer` feeding the same parser.
- Search, tags, recurring entries, and a Wear OS quick-add.

## Appendix A — Parser test cases

Defaults: `fractionDigits = 2` and the seed lexicon, unless stated.

| Input | amountMinor | note | category |
| --- | --- | --- | --- |
| 6 coffee | 600 | coffee | food |
| 18 taxi | 1800 | taxi | transport |
| 42 dinner | 4200 | dinner | food |
| 12.5 lunch | 1250 | lunch | food |
| coffee 6 | 600 | coffee | food |
| $6.50 latte | 650 | latte | food |
| NT$120 便當 | 12000 | 便當 | food |
| 午餐 150 | 15000 | 午餐 | food |
| １２．５ lunch (full-width digits) | 1250 | lunch | food |
| 1,200 rent | 120000 | rent | home |
| 12,5 pizza | 1250 | pizza | food |
| 7-eleven snack 35 | 3500 | 7-eleven snack | food |
| uber eats 24 | 2400 | uber eats | food |
| uber 24 | 2400 | uber | transport |
| steak 30 (proves word boundaries; “tea” must not match) | 3000 | steak | other |
| 15 xyzzyq | 1500 | xyzzyq | other (low confidence) |
| 6 cofee (fuzzy match) | 600 | cofee | food (low confidence) |
| 12.5 lunch with `fractionDigits = 0` | 13 | lunch | food |
| taxi | NoAmount |  |  |
| 0 coffee | NoAmount |  |  |
| (empty string) | NoAmount |  |  |
| 99999999 gift | NoAmount (above the limit) |  |  |

## Appendix B — Starter strings (own wording)

These are original placeholders, not the developer’s copy. Edit after the parity pass.

| Key | English | 繁體中文 |
| --- | --- | --- |
| entry\_hint | Type what you spent | 輸入花費，例如 6 咖啡 |
| entry\_need\_amount | Add an amount, e.g. 6 coffee | 請加上金額，例如 6 咖啡 |
| empty\_today | Nothing yet. Swipe up. | 今天還沒有紀錄。向上滑動。 |
| receipt\_total | TOTAL | 合計 |
| receipt\_empty | No expenses on this day. | 這一天沒有支出。 |
| insight\_day / month / year | Day / Month / Year | 日 / 月 / 年 |
| deleted | Deleted | 已刪除 |
| undo | Undo | 復原 |
| settings\_appearance | Appearance (System, Light, Dark) | 外觀（系統、淺色、深色） |
| settings\_language | Language | 語言 |
| settings\_currency | Currency | 貨幣 |
| export / import | Export / Import | 匯出 / 匯入 |

## Appendix C — Project-level definition of done

- Every Appendix A case passes.
- The merged release manifest has no `INTERNET` permission.
- Universal APK around 6 MB or smaller; cold start 400 ms or better on a mid-range phone.
- All strings localized in English and Traditional Chinese.
- TalkBack walkthrough passes; 200% font scale has no clipping.
- `PARITY.md` has no open \[Verify\] items, and the section 10 worksheet is complete.
- Screenshot tests exist for every screen in light, dark and 2.0 font scale.
- A 5,000-row database scrolls smoothly and Insight updates in under 100 ms.
- The export, wipe, import round trip restores identical rows.
- Play Data safety form and privacy policy are complete and accurate.
