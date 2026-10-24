# Nexpense Repository Audit

**Review date:** 2026-10-24
**Release reviewed:** v1.0.1
**Scope:** source packages, data storage and import/export, release configuration, repository documentation, and CI status.

## Current project status

- The application ID is com.nexusbench.nexpense.
- Release signing values are read from local Gradle properties or environment variables. No keystore or signing password is tracked in the repository.
- The app does not request Android's INTERNET permission. CI checks the release manifest for that permission.
- Expense data stays in the local Room database unless the user exports it or Android system backup is enabled. See the privacy policy and data safety notes for the backup behavior.
- The published v1.0.1 GitHub release includes a signed APK and a SHA-256 checksum. The v1.0.0 release remains available.

## Existing product safeguards

- Expense and receipt summaries handle multiple currencies without implying currency conversion.
- Entry and edit screens retain drafts until database writes succeed.
- JSON and CSV imports validate their input before applying changes in a Room transaction.
- Delete and restore events update conflict timestamps so older backups cannot overwrite newer deletion state.
- CSV export escapes cells and protects formula-like text.
- Settings provides a confirmed local data wipe, and privacy copy describes its scope.
- Date and time edits are saved consistently in the current time zone.
- Custom categories resolve to their display names in records and receipts.
- Store and privacy documentation explains that Android Auto Backup can include local app data.

## Verification

The local Gradle verification completed successfully on 2026-10-24: `assembleDebug`, `parser:test`, `testDebugUnitTest`, `lintDebug`, and `checkNoInternetPermission`. The test reports contain 35 parser tests and 173 app tests, with no failures. The final Gradle invocation reused up-to-date build and test results where inputs had not changed.

Lint completed without errors and reported 77 warnings: 51 dependency-version advisories, 16 unused-resource warnings, and 10 other Android lint warnings. The Gradle build also reports deprecated features that will need review before a Gradle 9 upgrade. The dependency advisories need coordinated compatibility testing; the warning count is not a zero-warning claim.

Screenshot tests are configured to record output rather than compare against reviewed baselines. A physical-device pass across compact and wide screens has not been recorded here.

## Maintainer follow-up

- Review dependency update pull requests individually. Some proposed versions require a coordinated Kotlin/Gradle or Android SDK upgrade; do not merge them solely because they are newer.
- Review Play Console Data Safety answers against the enabled Android Auto Backup behavior before publishing through Google Play.
- Establish reviewed screenshot baselines before treating screenshot recording as regression verification.
