# Nexpense Product and Engineering Guide

This guide describes the current Nexpense app, its architecture, and how to build it. The project was created collaboratively by [luciferx05](https://github.com/luciferx05) and [itz-kabbo](https://github.com/itz-kabbo).

Nexpense is an independent Android expense tracker inspired by [Mote. — Minimal Expense Tracker](https://apps.apple.com/au/app/mote/id6816360396) by Yun Ju Tseng (Rice Tseng). It has its own name, implementation, and visual identity.

## Product overview

Nexpense is designed for quick, private expense tracking:

- Enter an expense in plain language and review the parser's amount, note, and category.
- Browse and edit daily expenses, then view or share a receipt-style summary.
- Review daily, monthly, and yearly totals with category breakdowns.
- Manage categories, choose a supported currency, and select an appearance and language.
- Export or restore a JSON backup, or export CSV for spreadsheet use.

The natural-language parser runs locally. Expenses are not sent to an Nexpense server.

## Architecture

The Android app is in the app module. Its main code areas are:

- Feature screens and ViewModels for record entry, editing, receipts, insights, settings, and splash.
- A Compose design system shared by those screens.
- Repository and data layers backed by Room for expenses, categories, and learned category mappings.
- DataStore for appearance, language, and currency preferences.
- Hilt modules for dependency injection.

The parser module contains the pure Kotlin expense parser and its tests. It has no Android UI dependency.

Amounts are stored as integer minor currency units to avoid floating-point rounding. The application ID is com.nexusbench.nexpense, and the minimum Android version is API 26.

## Local data and privacy

Nexpense does not request Android's INTERNET permission and does not include an account, ads, analytics, or an Nexpense-operated sync service. Expense records are stored in the app's local Room database.

Android Auto Backup is enabled and may include app data when device backup is enabled. This system-managed behavior is described in the privacy policy and data safety notes. JSON is the full-fidelity expense and category backup format. CSV is a lossy interchange format and does not preserve every internal identifier or history field.

The DataStore file name mote_settings is retained for compatibility with existing installations and Android backup rules. Do not rename it without adding and verifying a data migration.

## Build and checks

Use JDK 21 and Android SDK Platform 36. The app targets API 36 and supports Android 8.0 (API 26) and later.

From the repository root, use the Gradle wrapper for these checks:

- Debug APK: :app:assembleDebug
- Unit tests: testDebugUnitTest
- Android lint: lintDebug
- Release manifest privacy guard: checkNoInternetPermission

GitHub Actions runs the build, unit tests, lint, and Internet-permission guard for pushes and pull requests.

Release signing values belong in user-level Gradle properties or environment variables. Keep the keystore and passwords outside the repository. The release application ID and signing identity must remain stable for Android to accept future updates.

## Documentation

- UI overview: UI.md
- Privacy policy: PRIVACY.md
- Data safety notes: DATA_SAFETY.md
- Store listing draft: STORE_LISTING.md
- Security reporting: ../SECURITY.md
