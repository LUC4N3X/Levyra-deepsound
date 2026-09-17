# Contributing to Levyra

Thanks for taking the time to contribute to Levyra.

Levyra is a fast-moving open-source music player with native Android and Windows targets. Contributions are welcome, whether they fix a small bug, improve documentation, refine the UI, strengthen reliability, or add a well-scoped feature.

The best contributions are not necessarily the biggest ones. They are the ones that solve a real problem, fit the existing architecture, are easy to review, and come with honest validation.

This guide explains how to contribute without creating unnecessary churn or regressions.

## Before You Start

Please check the existing issues and pull requests before beginning work. Someone may already be fixing the same problem, or there may be useful context that changes the best implementation approach.

As a general rule:

- small bug fixes, documentation changes, tests, and focused UI improvements can usually go directly to a pull request;
- larger features, architectural changes, new subsystems, database changes, or work that touches several major areas should be discussed first;
- unrelated fixes should not be bundled together just because they were discovered at the same time;
- security vulnerabilities must **not** be disclosed in public issues, discussions, pull requests, or comments. Follow [SECURITY.md](SECURITY.md) instead.

If you are unsure whether an idea is too large for a direct pull request, opening an issue first is usually the safest option.

## Project Structure

The main application targets are:

- **Android** — Kotlin, Jetpack Compose, AndroidX Media3 / ExoPlayer, Room, and related Android components;
- **Windows** — Kotlin, Compose Multiplatform, and libvlc.

Important top-level directories include:

```text
app/                 Android client
desktop/             Windows client
baselineprofile/     Android baseline-profile support
levyra-recognition/  Recognition-related module
docs/                Project documentation
scripts/             Validation and repository tooling
```

For the current development requirements and build instructions, see [docs/site/development.md](../docs/site/development.md).

For the repository's engineering contract and agent-specific rules, see [AGENTS.md](../AGENTS.md).

## Development Setup

Fork the repository, clone your fork, and create a branch from the latest `main`.

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Use the Gradle wrappers committed to the repository. Do not rely on a globally installed Gradle version.

### Android

Current Android development requirements are documented in [docs/site/development.md](../docs/site/development.md). A normal debug build is:

```bash
./gradlew assembleDebug
```

On Windows:

```powershell
.\gradlew.bat assembleDebug
```

Debug APKs are produced under:

```text
app/build/outputs/apk/debug/
```

To install a debug build on a connected Android device:

```bash
./gradlew installDebug
```

On Windows:

```powershell
.\gradlew.bat installDebug
```

### Windows Desktop

Windows-specific requirements and packaging commands are maintained in [docs/site/development.md](../docs/site/development.md). If your change affects desktop behavior, validate it against the desktop target instead of assuming Android behavior proves the desktop implementation works.

## Keep Changes Focused

A pull request should have one clear purpose.

Please avoid mixing the requested change with:

- unrelated refactors;
- broad formatting changes;
- dependency upgrades that are not required by the change;
- file moves or renames with no functional reason;
- speculative cleanup;
- version bumps or release work unless the pull request is specifically about a release;
- rewrites of stable code simply to match a personal preference.

Small diffs are easier to understand, validate, review, revert, and maintain.

When a change genuinely requires a wider refactor, explain why in the pull request description and keep the refactor limited to what the implementation actually needs.

## Preserve Existing Behavior

Levyra has several systems that interact with each other. Changes should preserve behavior outside their intended scope.

Take particular care around:

- playback and queue state;
- Media3 integration and media sessions;
- local and remote media handling;
- playlists, favorites, history, and library data;
- Room entities, migrations, and persisted user data;
- downloads and offline behavior;
- app settings and stored preferences;
- authentication tokens and external-service credentials;
- networking and provider fallbacks;
- localization;
- Android lifecycle behavior;
- Windows-specific playback behavior;
- background work and resource cleanup.

Do not silently discard or reset user data to make a migration or feature easier to implement.

If compatibility cannot be preserved, make that explicit before the change is merged.

## Code Quality

Follow the structure, naming, and patterns already used by the surrounding code unless there is a concrete reason to improve them.

Prefer code that is:

- easy to understand;
- narrow in responsibility;
- consistent with nearby architecture;
- defensive around external services and malformed data;
- explicit about failure states;
- testable where practical;
- free of unnecessary abstraction.

Avoid introducing a new architecture, wrapper, service layer, helper hierarchy, or dependency when the existing project already has a suitable owner for the behavior.

Do not leave dead code, debug-only shortcuts, unexplained feature flags, temporary workarounds, or commented-out implementations in a finished pull request.

## UI and UX Contributions

Levyra has its own visual identity. External applications can be useful references, but new UI should be adapted to Levyra rather than copied blindly.

For visible changes:

- match the existing design language, spacing, typography, motion, shapes, and component behavior;
- preserve usability in both compact and larger layouts where relevant;
- avoid overcrowding screens with controls simply because space is available;
- keep interaction states clear and predictable;
- preserve accessibility semantics and content descriptions where they are needed;
- avoid introducing jank, excessive recomposition, unnecessary animations, or heavy work on the UI thread;
- use the existing localization system for user-facing text;
- check dark/light behavior when the affected surface supports both;
- include screenshots or a short recording in the pull request when a visual comparison would help reviewers.

A UI change should improve the product without weakening navigation, playback access, or discoverability elsewhere.

## Dependencies

New dependencies have a long-term maintenance cost. Add one only when it provides clear value that is difficult to achieve safely with the platform, standard library, or dependencies Levyra already uses.

When adding or replacing a dependency:

- explain why it is needed;
- prefer actively maintained projects;
- check that its license is compatible with Levyra;
- consider binary size, startup cost, memory use, transitive dependencies, and platform compatibility;
- avoid adding a large library for a very small task;
- do not upgrade unrelated dependencies in the same pull request unless required.

## Testing and Validation

Validate the smallest relevant surface first, then run the repository quality gates required for publication.

Examples of useful focused validation include:

- a targeted unit test;
- a module-specific Gradle task;
- an Android debug build;
- manual reproduction of the bug being fixed;
- playback testing on a real device or emulator;
- desktop checks for Windows-specific changes;
- screenshots or recordings for UI work.

### Repository quality gates

Before committing meaningful code changes, run:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or publishing a pull request, repository instructions require:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

On systems where `python` rather than `python3` is the configured launcher, use the equivalent command for that environment.

A skipped, blocked, or unavailable required check is **not** the same as a passing check.

If validation cannot run because of a missing Android SDK, JDK, device, signing configuration, native dependency, network requirement, or another environment limitation, say exactly what was not run and why. Do not report a check as passing unless it actually completed successfully.

### Physical-device qualification

For Android changes involving playback, MediaSession behavior, startup, memory, lifecycle, or device-specific integration, the repository includes a Windows ADB qualification harness:

```powershell
.\scripts\levyra-device-qualification.ps1
```

Additional modes and output locations are documented in [docs/site/development.md](../docs/site/development.md).

Use this when it adds meaningful evidence; not every documentation or isolated unit change needs a full device qualification run.

## Pull Requests

Use a clear title that describes the actual change.

A strong pull request description should explain:

- **Problem** — what was wrong, missing, confusing, or unreliable;
- **Solution** — what changed and why this approach was chosen;
- **User impact** — what users will notice, if anything;
- **Validation** — the exact tests, builds, or manual checks that were completed;
- **Limitations** — anything relevant that could not be tested or remains intentionally out of scope.

Keep the description factual. Do not claim that something is "fully tested", "production ready", "fixed everywhere", or regression-free unless the evidence actually supports that statement.

For UI changes, include before/after screenshots or a short recording when practical.

For behavior changes, include clear reproduction steps or testing instructions when reviewers would otherwise have to discover them themselves.

If review feedback uncovers a separate issue, prefer a separate issue or pull request instead of expanding the current change indefinitely.

## Review-Friendly Changes

Help reviewers understand your work without reverse-engineering your intent.

Good pull requests usually:

- keep generated noise out of the diff;
- avoid touching files that are unrelated to the change;
- explain non-obvious tradeoffs;
- include tests for important behavior when reasonably possible;
- keep migration and compatibility behavior explicit;
- distinguish verified behavior from assumptions;
- resolve temporary debugging code before review;
- update documentation when user-facing or developer-facing behavior changes.

Review comments are about the code and the health of the project. Technical disagreement is normal and welcome when it stays focused on the work.

## Bug Reports

A useful bug report should give someone else enough information to reproduce the problem.

Include, where relevant:

- Levyra version;
- Android or Windows version;
- device or PC model;
- whether the build is an official release, debug build, or custom build;
- exact reproduction steps;
- expected behavior;
- actual behavior;
- whether the problem happens consistently or intermittently;
- logs, screenshots, or recordings that materially help diagnosis.

Please remove personal data, authentication information, cookies, API keys, tokens, private URLs, account identifiers, and unrelated log content before posting anything publicly.

If the report contains a potential security vulnerability, stop and follow [SECURITY.md](SECURITY.md) instead of posting technical details publicly.

## Feature Requests

Feature requests are welcome, but the strongest proposals start with the user problem rather than the implementation.

A useful proposal explains:

- what problem the feature solves;
- who benefits from it;
- how it should fit into existing Levyra behavior;
- whether a similar capability already exists;
- any meaningful compatibility, privacy, performance, or UX implications.

References to other applications are useful for explaining an interaction or idea, but "copy this app" is not a complete design specification.

## Documentation Contributions

Documentation changes are first-class contributions.

Please keep documentation:

- accurate for the current codebase;
- concise enough to remain maintainable;
- explicit about platform differences;
- free of claims that cannot be verified;
- consistent with the repository's existing terminology and command examples.

If you change a command, workflow, configuration path, build requirement, or contributor-facing behavior, update the relevant documentation in the same pull request when practical.

## Localization

User-facing text should use Levyra's existing localization infrastructure rather than introducing unnecessary hardcoded strings.

When changing or adding strings:

- keep the source wording concise and unambiguous;
- avoid embedding layout assumptions into the text;
- preserve placeholders and formatting tokens;
- do not machine-edit unrelated translations just to make the diff look complete;
- avoid deleting existing translations unless the underlying string is actually being removed.

## AI-Assisted Contributions

AI-assisted development is allowed. The contributor opening the pull request remains responsible for every line that is submitted.

If you use a coding assistant:

- review the generated code before committing it;
- understand what the change does;
- remove unrelated churn and unnecessary abstractions;
- verify imports, APIs, paths, configuration, and assumptions against the actual repository;
- run real tests instead of relying on the assistant's description of what should work;
- do not present generated explanations as test evidence;
- check generated code for secrets, copied proprietary material, incompatible licensing, or fabricated references;
- keep the final implementation consistent with Levyra's architecture rather than the assistant's preferred architecture.

"The AI said it works" is not validation.

## Security and Sensitive Data

Never commit or publish secrets, credentials, signing material, private endpoints, or sensitive user information.

This includes, but is not limited to:

- API keys and access tokens;
- cookies and session data;
- passwords;
- signing keys or keystores;
- private certificates;
- `.env` files containing secrets;
- populated `local.properties` files;
- private service URLs;
- credentials copied from logs or device storage.

If you discover a suspected vulnerability, follow [SECURITY.md](SECURITY.md). Do not include exploit details or sensitive proof-of-concept material in a public issue or pull request.

## Third-Party Code and Licensing

Levyra is licensed under the [GNU General Public License v3.0](../LICENSE).

By submitting a contribution, you agree that your contribution can be distributed under the same project license.

When adapting code from another project:

- confirm that the source license is compatible with Levyra;
- preserve copyright, attribution, and license notices where required;
- identify substantial reused or adapted code when the origin would otherwise be unclear;
- do not copy code from proprietary, source-available, or otherwise incompatible sources;
- do not remove upstream attribution simply to make imported code look native to Levyra.

Ideas, UX patterns, and architectural concepts can often be reimplemented independently without copying source code. When in doubt, prefer a clean implementation that fits Levyra's architecture.

## Generated Files and Repository Hygiene

Do not commit local or generated files unless the repository intentionally tracks them.

Before opening a pull request, check for accidental additions such as:

- build outputs;
- IDE metadata;
- temporary files;
- local reports;
- crash dumps;
- downloaded binaries;
- device logs;
- credentials;
- test artifacts that are not part of the repository contract.

Review your own diff before publication. A clean diff is one of the easiest ways to prevent accidental regressions and secret leaks.

## What Maintainers May Ask You to Change

A contribution may need revision when it:

- changes unrelated behavior;
- duplicates an existing subsystem instead of integrating with it;
- introduces avoidable complexity;
- weakens reliability, security, privacy, accessibility, or performance;
- breaks platform parity without a clear reason;
- adds a dependency without enough justification;
- lacks necessary migration handling;
- includes unsupported claims or incomplete validation;
- conflicts with the direction of the project.

A technically working implementation can still need changes if its maintenance cost or integration risk is too high.

## Contribution Checklist

Before submitting a pull request, make sure that:

- [ ] the change has one clear purpose;
- [ ] the branch started from a reasonably current `main`;
- [ ] unrelated refactors and formatting churn are excluded;
- [ ] user data and existing behavior are preserved outside the intended scope;
- [ ] relevant tests or manual checks were completed;
- [ ] `ai_quality_gate.py --profile fast` was run for meaningful code changes;
- [ ] `ai_quality_gate.py --profile full` was run before PR publication when required by the repository instructions;
- [ ] skipped or unavailable checks are disclosed honestly;
- [ ] UI changes include visual evidence when useful;
- [ ] user-facing text uses the existing localization system;
- [ ] no secrets or sensitive data are included;
- [ ] third-party code and assets have compatible licensing and required attribution;
- [ ] documentation was updated where behavior or contributor workflow changed;
- [ ] the final diff was reviewed before submission.

## Community

Be respectful to maintainers, contributors, and users.

Technical disagreement is expected in an active project. Personal attacks, harassment, and hostile behavior are not.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating in project discussions.

Thanks for helping make Levyra better.