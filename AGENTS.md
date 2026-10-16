# Agent rules
- Source of truth: docs/GUIDE.md. Do not invent features.
- Work one milestone at a time (M0 to M10). Do not start the next until its Done-when gate passes.
- After every milestone run ./gradlew assembleDebug testDebugUnitTest lintDebug, then commit as M<n>: <title>.
- No INTERNET permission, no analytics or ad SDKs, no network calls. Ask before adding any dependency the guide does not list.
- Money is Long minor units. Never Float or Double.
- Every user-visible string lives in strings.xml (en and zh-rTW).
- For each [Verify] item: implement the default, leave a // VERIFY: comment, add a row to PARITY.md.
- If an API in the guide does not compile on the installed versions, adapt it and record the change in DECISIONS.md.
- Stop and ask the human when a spec line contradicts itself, a gate cannot be met, or a decision would change permissions, data format or the stack.
