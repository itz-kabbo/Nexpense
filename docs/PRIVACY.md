# Privacy Policy for Nexpense

*Last updated: October 2026*

Nexpense ("the Application") is an offline-first expense tracking application developed with an uncompromising commitment to user privacy and digital sovereignty.

---

## 1. Zero Network Access Policy

Nexpense does not request, declare, or utilize the `android.permission.INTERNET` permission in its Android application manifest.
- The Application cannot connect to the internet, remote servers, cloud infrastructure, or third-party networks.
- Nexpense does not send personal data, expense amounts, transaction descriptions, location information, or device telemetry over a network. If you export a file, Android's document picker lets you choose its destination; the selected storage provider may be local or remote.

---

## 2. Information We Do Not Collect

Because Nexpense operates completely offline, the developer has no technical means or desire to collect, access, log, monitor, or sell any information about you. Specifically:
- **No Personal Identifiers:** We do not collect names, email addresses, phone numbers, postal addresses, or government identification.
- **No Financial Account Data:** We do not collect bank credentials, credit card details, account numbers, or payment processing tokens.
- **No Behavioral Analytics:** We do not record screens viewed, button taps, interaction timings, or usage habits.
- **No Device Telemetry:** We do not track IP addresses, hardware identifiers, advertising IDs (GAID), MAC addresses, or network interfaces.
- **No Third-Party SDKs:** The Application contains zero advertising SDKs, zero analytics trackers, zero attribution frameworks, and zero remote logging services.

---

## 3. Data Storage and User Ownership

- **Local SQLite Database:** All transactions, notes, categories, timestamps, and personalized categorization preferences are stored in a private SQLite database inside the application's isolated sandbox on your device.
- **User Ownership & Control:** You own 100% of your data. You can inspect and edit expense records, remove them from active history with the undo option, or permanently clear all local user data from Settings. Individually deleted expense rows remain in the local database as timestamped tombstones and are included in JSON backups so an older backup cannot silently restore a removed entry.
- **Export & Portability:** You can export your entire expense history and custom categories at will into open, standard JSON or CSV files via Android's native Storage Access Framework.
- **Permanent Data Deletion:** Selecting **Wipe all data** in Settings deletes expenses, custom categories, and learned category corrections, restores built-in categories, and resets saved preferences. Uninstalling the Application clears the remaining app sandbox files. Android system backups are managed separately by Android and your device settings.

---

## 4. Operating System Services (Android Backups)

- If you have enabled Android's system-level backup service (Google Drive Backup / Android Auto Backup) in your device settings, Android may include Nexpense's local database in its cloud backup. Android documents client-side, end-to-end encryption for cloud backups on Android 9 or later when a screen lock is set; backup behavior depends on device version and settings.
- Android backup storage is governed by the device and backup provider. Nexpense does not operate or manage backup servers.

---

## 5. Children's Privacy

Nexpense does not knowingly collect any personal information from children under the age of 13 (or under 16 in certain jurisdictions), nor does it collect information from anyone of any age. The application is completely offline and does not contain advertisements, external web views, or tracking features.

---

## 6. Open Source and Verifiability

Nexpense's security and privacy claims are verifiable directly through its build artifacts and source code:
- The compiled APK and Android App Bundle contain no network permissions.
- Automated static checks enforce that `android.permission.INTERNET` is never introduced into the release manifest.

---

## 7. Contact Information

If you have questions, feedback, or inquiries regarding this Privacy Policy or Nexpense's offline architecture, please reach out via the project's repository issue tracker or developer contact channel.
