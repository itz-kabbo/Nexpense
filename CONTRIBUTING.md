# Contributing to Nexpense

Thanks for taking an interest in Nexpense. Bug reports, focused improvements, and documentation fixes are welcome.

## Before opening a pull request

1. Search existing issues and pull requests for related work.
2. For a larger change, open an issue first so the scope can be discussed.
3. Keep changes focused and follow the project guidance in [`AGENTS.md`](AGENTS.md) and [`docs/GUIDE.md`](docs/GUIDE.md).
4. Do not add networking, analytics, advertising, tracking, or new dependencies without discussing the change first. Nexpense is designed to work offline and without an Internet permission.
5. Never include real expense records, account details, signing credentials, keystores, or other private data in an issue, screenshot, test fixture, or commit.
6. Avoid publishing a personal email in Git commit metadata. Use the GitHub-provided noreply address from your account's email settings.

## Build and checks

Use JDK 21 and Android SDK Platform 36. From the repository root, run:

```powershell
.\gradlew.bat assembleDebug testDebugUnitTest lintDebug checkNoInternetPermission
```

On macOS or Linux, use `./gradlew` with the same tasks. Include the relevant result in your pull request description. If a check cannot run in your environment, say which one and why.

## Pull request checklist

- [ ] The change has a clear purpose and stays within the agreed scope.
- [ ] User-visible strings are localized using the project’s existing resources.
- [ ] Data changes preserve the project’s local-first and privacy behavior.
- [ ] Tests or documentation were added or updated where useful.
- [ ] The listed build and quality checks pass, or any limitation is explained.
- [ ] Screenshots use fictional sample data only.
