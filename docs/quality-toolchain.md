# Quality gates and verification

## Existing Android checks

The repository already has Android CI and a full-repository audit workflow.
The Android CI runs JVM unit tests, assembles the debug APK, then runs
instrumented tests in an Android emulator with a real Ollama server and the
`llama3.2:1b` model. Test reports are uploaded as workflow artifacts.
The full audit runs Gradle lint/tests/builds and additional language checks
where matching project files exist.

These are automated CI checks, not proof that the APK works on a physical
phone. Emulator rendering and device-specific behavior can differ.

## Added independent review gates

- **Semgrep** scans source for security and bug patterns on pull requests,
  pushes to main, manual dispatch, and weekly schedule.
- **CodeRabbit** review guidance focuses review on Kotlin/Compose behavior,
  offline guarantees, CI security, and release signing. This file does not
  install or enable the CodeRabbit GitHub App; repository access and provider
  availability must be confirmed separately.

## Local Ollama

The Android app's optional AI endpoint defaults to loopback
(`http://127.0.0.1:11434/api/chat`) and the CI emulator uses
`adb reverse` to reach the runner's Ollama service. A GitHub-hosted runner
cannot directly reach Ollama on a developer's phone. Never expose an
unauthenticated local Ollama server to the public internet just to make CI pass.

Local preflight in Termux:

```sh
ollama list
curl -fsS http://127.0.0.1:11434/api/tags
```

## Recommended merge/release sequence

1. Require the Android CI and static-analysis checks to finish successfully.
2. Review findings and test that the checks catch seeded, harmless defects
   before relying on them as enforcement.
3. Install the produced APK on a physical Android device and exercise the
   main user flows, import/export, offline paths, and configured AI endpoint.
4. Verify release signing independently. CI debug APKs are not release builds.
5. Report results as REAL + VERIFIED only for checks actually executed.
   Mark emulator-only, unrun, or provider-dependent results UNVERIFIED.

## Tool selection notes

AI reviewers are advisory and can miss defects or report false positives.
Do not add a second overlapping Android build workflow without a demonstrated
gap. GHAGGA or other local Ollama review tools should be evaluated in a
separate disposable test branch: verify the actual repository, license,
maintained release, supported model/provider interface, and detection of a
known seeded defect before adding them to normal CI.
