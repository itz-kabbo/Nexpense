# Privacy Policy for Mote

*Last updated: October 2026*

Mote ("the Application") is an offline-first expense tracking application developed with an uncompromising commitment to user privacy and digital sovereignty.

---

## 1. Zero Network Access Policy

Mote does not request, declare, or utilize the `android.permission.INTERNET` permission in its Android application manifest.
- The Application cannot connect to the internet, remote servers, cloud infrastructure, or third-party networks.
- No personal data, expense amounts, transaction descriptions, location information, or device telemetry can ever be transmitted off your device by Mote.

---

## 2. Information We Do Not Collect

Because Mote operates completely offline, the developer has no technical means or desire to collect, access, log, monitor, or sell any information about you. Specifically:
- **No Personal Identifiers:** We do not collect names, email addresses, phone numbers, postal addresses, or government identification.
- **No Financial Account Data:** We do not collect bank credentials, credit card details, account numbers, or payment processing tokens.
- **No Behavioral Analytics:** We do not record screens viewed, button taps, interaction timings, or usage habits.
- **No Device Telemetry:** We do not track IP addresses, hardware identifiers, advertising IDs (GAID), MAC addresses, or network interfaces.
- **No Third-Party SDKs:** The Application contains zero advertising SDKs, zero analytics trackers, zero attribution frameworks, and zero remote logging services.

---

## 3. Data Storage and User Ownership

- **Local SQLite Database:** All transactions, notes, categories, timestamps, and personalized categorization preferences are stored in a private SQLite database inside the application's isolated sandbox on your device.
- **User Ownership & Control:** You own 100% of your data. You can inspect, modify, or delete any individual expense record at any time.
- **Export & Portability:** You can export your entire expense history and custom categories at will into open, standard JSON or CSV files via Android's native Storage Access Framework.
- **Permanent Data Deletion:** Selecting **Wipe all data** in the in-app settings immediately deletes all local database tables and preferences. Uninstalling the Application clears all remaining local sandboxed files.

---

## 4. Operating System Services (Android Backups)

- If you have enabled Android's system-level backup service (Google Drive Backup / Android Auto Backup) in your device settings, Android may include Mote's local database in your encrypted cloud backups.
- These backups are governed by your agreement with Google and are end-to-end encrypted under your device screen lock credentials. Mote does not operate or manage any backup servers.

---

## 5. Children's Privacy

Mote does not knowingly collect any personal information from children under the age of 13 (or under 16 in certain jurisdictions), nor does it collect information from anyone of any age. The application is completely offline and does not contain advertisements, external web views, or tracking features.

---

## 6. Open Source and Verifiability

Mote's security and privacy claims are verifiable directly through its build artifacts and source code:
- The compiled APK and Android App Bundle contain no network permissions.
- Automated static checks enforce that `android.permission.INTERNET` is never introduced into the release manifest.

---

## 7. Contact Information

If you have questions, feedback, or inquiries regarding this Privacy Policy or Mote's offline architecture, please reach out via the project's repository issue tracker or developer contact channel.
