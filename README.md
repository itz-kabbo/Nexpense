# Nexpense

**A quiet, offline-first Android expense tracker.** Log an expense in plain language, review the day as a receipt, and understand spending over time.

[![Android CI](https://github.com/itz-kabbo/Nexpense/actions/workflows/ci.yml/badge.svg?branch=master)](https://github.com/itz-kabbo/Nexpense/actions/workflows/ci.yml)
[![License: MIT](https://img.shields.io/badge/License-MIT-blue.svg)](LICENSE)
[![Android 8.0+](https://img.shields.io/badge/Android-8.0%2B-3DDC84?logo=android&logoColor=white)](https://developer.android.com/about/versions/oreo)

Nexpense is a personal project made in my free time because I wanted a simple expense tracker on Android. I’m sharing it so anyone who finds it useful can use, study, or adapt it.

## Features

- **Quick natural-language entry:** type something like `6 coffee`; the local parser extracts the amount and description and suggests a category.
- **Daily receipt:** browse dated, itemized receipts and share a receipt image.
- **Spending insights:** review daily, monthly, and yearly totals with category breakdowns and trends.
- **Expense and category controls:** edit or remove entries, manage categories, and choose from supported currencies.
- **Personalization:** light, dark, or system appearance and localized UI with language choices.
- **Portable data:** export and import JSON backups, or export CSV for spreadsheet use. CSV is a lossy interchange format.

## Privacy and data

Expense records are stored in a local Room database. Nexpense does not request Android’s `INTERNET` permission and does not include an account, ads, analytics, or an Nexpense-operated sync service. The CI workflow checks that the release manifest does not gain Internet permission.

Android Auto Backup is enabled and may include local app data if system backup is enabled on the device. See [the privacy policy](docs/PRIVACY.md) and [data safety notes](docs/DATA_SAFETY.md) for details. The app also provides JSON and CSV export and a local data-wipe option.

## Build from source

### Requirements

- JDK 21
- Android SDK Platform 36 and the Android build tools installed
- Android Studio or another environment configured with the Android SDK

Clone the repository and open it in Android Studio, or build from a terminal.

**Windows PowerShell:**

```powershell
.\gradlew.bat :app:assembleDebug
```

**macOS or Linux:**

```bash
./gradlew :app:assembleDebug
```

The debug APK is written to `app/build/outputs/apk/debug/app-debug.apk`.

### Run checks

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug checkNoInternetPermission
```

The GitHub Actions workflow runs these checks for pushes and pull requests targeting `master` or `main`.

### Build a signed release APK

Release signing values are read from Gradle properties or the matching `RELEASE_*` environment variables. Keep the keystore and credentials out of Git.

On Windows, put the following in your user-level Gradle properties file at `%USERPROFILE%\.gradle\gradle.properties` (create the file if needed). On macOS or Linux, use `~/.gradle/gradle.properties`:

```properties
releaseStoreFile=C:/absolute/path/to/release.keystore
releaseStorePassword=your-private-store-password
releaseKeyAlias=your-private-key-alias
releaseKeyPassword=your-private-key-password
```

Use your real local values in that private file; do not paste them into the project’s tracked `gradle.properties`, commit them, or upload the keystore. Then run:

```powershell
.\gradlew.bat :app:assembleRelease
```

The signed APK is written to `app/build/outputs/apk/release/app-release.apk`. Keep a secure backup of the signing key: future updates to the same Android application ID must use the matching signing identity.

## Project details

| Item | Value |
| --- | --- |
| Application ID | `com.nexusbench.nexpense` |
| Minimum Android version | Android 8.0 (API 26) |
| Compile / target SDK | 36 / 36 |
| Main technologies | Kotlin, Jetpack Compose, Room, Hilt, DataStore |
| Build JDK | 21 |

## Inspiration and attribution

Nexpense was inspired by **[Mote. — Minimal Expense Tracker](https://apps.apple.com/au/app/mote/id6816360396)** by **Yun Ju Tseng** (Rice Tseng), an iOS app with a similarly simple approach to personal expense tracking. Nexpense is an independent Android project; it is not affiliated with or endorsed by Mote or its developer.

This project was made for my own use in my spare time and was **fully vibe-coded with Claude, Gemini, and ChatGPT**. These AI tools and their providers are not affiliated with or endorsing Nexpense.

## Documentation

- [Product and engineering guide](docs/GUIDE.md)
- [Privacy policy](docs/PRIVACY.md)
- [Data safety notes](docs/DATA_SAFETY.md)
- [Store listing draft](docs/STORE_LISTING.md)
- [UI/UX design specification](docs/UI.md)
- [Audit report](docs/AUDIT_REPORT.md)

## Contributing

Bug reports, thoughtful feature suggestions, and pull requests are welcome. Please read [CONTRIBUTING.md](CONTRIBUTING.md) first. For security issues, follow [SECURITY.md](SECURITY.md) and do not post sensitive financial data or vulnerability details in a public issue.

## License

Nexpense is distributed under the [MIT License](LICENSE).
