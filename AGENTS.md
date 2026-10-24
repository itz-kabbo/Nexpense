# Project Instructions

- Treat the implementation and the current documentation as the source of truth. Keep docs/GUIDE.md aligned with shipped behavior.
- Keep Nexpense offline-first. Do not add Internet access, analytics, advertising, or network calls without explicit project approval.
- Preserve the application ID com.nexusbench.nexpense and the existing signing identity. Do not change database formats, persisted preference names, or backup paths without a tested migration.
- Store money as integer minor units. Do not use floating-point values for expense amounts.
- Keep user-facing text in Android string resources and update the supported locales consistently.
- Use JDK 21 and Android SDK Platform 36. Run the Gradle wrapper build, unit tests, lint, and the release Internet-permission check before a release.
- Keep private expense data, personal signing credentials, and keystores out of source control and public issue reports.
