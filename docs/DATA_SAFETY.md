# Google Play Data Safety Declaration

This document details the exact responses and justifications for the Google Play Data Safety form for **Nexpense**.

---

## 1. Executive Summary

- **App-controlled collection:** **No** (the app does not transmit user data to Nexpense servers or through app networking).
- **System backup:** Android Auto Backup is enabled and may transfer local app data when device backup is enabled. Confirm how this OS-managed transfer should be represented in the Play Console form before publication.
- **Network Permissions:** **None** (`android.permission.INTERNET` is absent from the manifest and enforced by CI/build checks).
- **Third-Party SDKs:** **None** (No analytics, no ad networks, no crash reporting, no tracking SDKs).
- **App Storage Model:** Local SQLite database via Jetpack Room; Android Auto Backup may copy the database and preferences off-device when enabled.

---

## 2. Play Console Questionnaire Responses

### Data Collection and Security
| Question | Answer | Details / Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **Needs publisher review** | The app has no network permission and does not transmit data to Nexpense servers. Android Auto Backup is enabled and may transfer the local database and preferences when system backup is enabled; confirm the Play Console classification for that OS-managed transfer. |
| **Is all user data collected by your app encrypted in transit?** | **Needs publisher review** | The app itself does not transmit data. Android system backup is separately managed and its encryption depends on Android version, screen-lock configuration, and device settings. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can remove individual expenses from active history, or tap **Wipe all data** in Settings to delete expenses, custom categories, learned category corrections, and reset saved preferences. The full wipe clears deletion markers too. Uninstalling the app removes the app's local sandbox; Android system backups are managed separately by Android. |

### Data Types Assessment
| Category | Data Type | Collected? | Shared? | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Location** | Approximate or Precise Location | No | No | Not accessed. |
| **Personal Info** | Name, Email, Address, Phone, IDs | No | No | Not collected or requested. |
| **Financial Info** | Expense records / purchase history | Needs publisher review | Needs publisher review | Expense notes and amounts are stored in local SQLite. Android system backup may include the database when enabled; confirm the Play Console classification before publication. |
| **Health and Fitness** | Health / Fitness info | No | No | Not accessed. |
| **Messages** | Emails, SMS, in-app messages | No | No | Not accessed. |
| **Photos and Videos** | Photos, Videos | No | No | Optional receipt image sharing is handled via standard Android platform share intent (`ACTION_SEND`) without app network access. |
| **Audio Files** | Voice recordings, Sound files | No | No | Not accessed. |
| **Files and Docs** | Files and Docs | No | No | Document export/import uses Storage Access Framework (`CreateDocument`, `OpenDocument`) directly to user-chosen storage without background collection. |
| **Calendar** | Calendar events | No | No | Not accessed. |
| **Contacts** | Contacts | No | No | Not accessed. |
| **App Activity** | App interactions, In-app search, Installed apps | No | No | No telemetry or session tracking. |
| **Web Browsing** | Web browsing history | No | No | Not accessed. |
| **App Info and Performance** | Crash logs, Diagnostics, Performance metrics | No | No | No third-party crash reporters or diagnostic trackers. |
| **Device or Other IDs** | Device ID, Advertising ID | No | No | No ad identifiers or device fingerprinting. |

---

## 3. Data Storage & System Backup

### Local SQLite Database
- All transaction records, expense entries, custom categories, and learned parser heuristics are stored locally in the app's private sandbox directory via Android Jetpack Room (`AppDatabase`).
- Sandboxed files are inaccessible to other applications without root privileges. Individually deleted expense rows remain locally as timestamped tombstones and in JSON backups for conflict protection; the full wipe removes those rows.

### Android Auto Backup
- Backup rules (`backup_rules.xml` and `data_extraction_rules.xml`) allow standard Android cloud backup of the SQLite database and DataStore preferences.
- When enabled by the user in Android system settings, Android handles backup storage. Android documents client-side, end-to-end encryption for cloud backups on Android 9 or later when a screen lock is set; backup behavior depends on device version and settings.
- No proprietary cloud services or external sync pipelines are used.
- The app itself does not upload data. Review the enabled Android backup behavior when completing the store's data disclosures; system backup is managed separately from Nexpense's app networking.

---

## 4. Architectural Verification

1. **Manifest Audit:** The application's `AndroidManifest.xml` does not declare `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, or `android.permission.ACCESS_WIFI_STATE`.
2. **Build Enforcement:** The release build pipeline includes an automated task (`checkNoInternetPermission`) that scans merged Android manifests and fails the build if `INTERNET` permission is introduced.
3. **Dependency Audit:** No transitive dependencies include Google Analytics, Firebase, Crashlytics, AdMob, AppsFlyer, or any telemetry SDK.
