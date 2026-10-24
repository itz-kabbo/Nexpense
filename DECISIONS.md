# Engineering Decisions

- **Offline-first:** the app has no account, analytics, ads, sync service, or INTERNET permission.
- **Expense storage:** Room is the source of truth for expenses, categories, and learned category mappings.
- **Money representation:** amounts are stored as integer minor units; no floating-point amounts or implicit exchange-rate conversion.
- **Preferences:** DataStore stores display preferences. The existing mote_settings file name remains stable for installed users and backup compatibility.
- **Parsing:** natural-language parsing is a pure Kotlin module, separate from Android UI and storage.
- **Backup formats:** JSON preserves the app's backup data; CSV is documented as lossy interchange.
- **Release identity:** the application ID is com.nexusbench.nexpense. Release signing values stay outside Git, and the signer must remain stable for future app updates.
