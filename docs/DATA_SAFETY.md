# Google Play Data Safety Declaration

This document details the exact responses and justifications for the Google Play Data Safety form for **Mote**.

---

## 1. Executive Summary

- **Data Collected:** **No** (0 data collected).
- **Data Shared:** **No** (0 data shared).
- **Network Permissions:** **None** (`android.permission.INTERNET` is absent from the manifest and enforced by CI/build checks).
- **Third-Party SDKs:** **None** (No analytics, no ad networks, no crash reporting, no tracking SDKs).
- **Storage Model:** 100% local on-device SQLite database via Jetpack Room.

---

## 2. Play Console Questionnaire Responses

### Data Collection and Security
| Question | Answer | Details / Justification |
| :--- | :--- | :--- |
| **Does your app collect or share any of the required user data types?** | **No** | The app operates entirely on the device. It has no network capabilities and transmits no data to external servers. |
| **Is all user data collected by your app encrypted in transit?** | **Not applicable** | No data is collected or transmitted over any network. |
| **Do you provide a way for users to request that their data be deleted?** | **Yes** | Users can delete individual items, clear historical periods, or tap **Wipe all data** in Settings to instantly purge the local database and preferences. Uninstalling the app also deletes all local application storage. |

### Data Types Assessment
| Category | Data Type | Collected? | Shared? | Purpose |
| :--- | :--- | :--- | :--- | :--- |
| **Location** | Approximate or Precise Location | No | No | Not accessed. |
| **Personal Info** | Name, Email, Address, Phone, IDs | No | No | Not collected or requested. |
| **Financial Info** | User payment info, Purchase history, Credit score | No | No | Expense notes and amounts entered by the user are stored strictly in local SQLite and never transmitted. |
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
- Sandboxed files are inaccessible to other applications without root privileges.

### Encrypted Android Auto Backup
- Backup rules (`backup_rules.xml` and `data_extraction_rules.xml`) allow standard Android cloud backup of the SQLite database and DataStore preferences.
- When enabled by the user in Android system settings, backups are encrypted using Android's end-to-end encrypted backup infrastructure via the user's Google account and lockscreen credentials.
- No proprietary cloud services or external sync pipelines are used.

---

## 4. Architectural Verification

1. **Manifest Audit:** The application's `AndroidManifest.xml` does not declare `android.permission.INTERNET`, `android.permission.ACCESS_NETWORK_STATE`, or `android.permission.ACCESS_WIFI_STATE`.
2. **Build Enforcement:** The release build pipeline includes an automated task (`checkNoInternetPermission`) that scans merged Android manifests and fails the build if `INTERNET` permission is introduced.
3. **Dependency Audit:** No transitive dependencies include Google Analytics, Firebase, Crashlytics, AdMob, AppsFlyer, or any telemetry SDK.
