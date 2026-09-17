# Contributing to Levyra

> **Build carefully. Test honestly. Keep Levyra fast.**

Thanks for taking the time to contribute to **Levyra**.

Levyra is a fast-moving open-source music player for **Android** and **Windows**. Contributions are welcome — from a one-line documentation fix to a carefully designed feature — as long as they fit the project, preserve existing behavior, and come with honest validation.

The best contribution is not necessarily the biggest one. It is the one that solves a real problem without creating three new ones.

---

### Quick navigation

[Before you start](#-before-you-start) · [Setup](#-development-setup) · [Code quality](#-code-quality) · [UI & UX](#-ui--ux) · [Testing](#-testing--validation) · [Pull requests](#-pull-requests) · [AI-assisted work](#-ai-assisted-contributions) · [Security](#-security--sensitive-data) · [Checklist](#-final-checklist)

---

## 👋 Before You Start

Check the existing **issues** and **pull requests** before beginning work. Someone may already be solving the same problem, or there may be context that changes the best implementation approach.

| Your change | Recommended path |
| --- | --- |
| Small bug fix | Open a focused PR |
| Documentation or translation fix | Open a focused PR |
| Small UI polish | Open a focused PR |
| New feature | Discuss first if it affects several areas |
| Architecture / database change | Open an issue first |
| New subsystem or major dependency | Open an issue first |
| Security vulnerability | **Do not post details publicly — follow [SECURITY.md](SECURITY.md)** |

A good rule: **one pull request, one clear purpose.**

Do not bundle unrelated cleanup, refactors, dependency updates, or release work into a change just because you noticed them along the way.

---

## 🧭 Project Map

Levyra currently has two native application targets:

| Target | Main stack |
| --- | --- |
| **Android** | Kotlin · Jetpack Compose · AndroidX Media3 / ExoPlayer · Room |
| **Windows** | Kotlin · Compose Multiplatform · libvlc |

```text
app/                 Android client
desktop/             Windows client
baselineprofile/     Android baseline-profile support
levyra-recognition/  Recognition-related module
docs/                Project documentation
scripts/             Validation and repository tooling
```

Useful references:

- [Development guide](../docs/site/development.md) — requirements, builds, device qualification and packaging
- [AGENTS.md](../AGENTS.md) — repository engineering contract and agent-specific rules
- [SECURITY.md](SECURITY.md) — responsible vulnerability reporting
- [CODE_OF_CONDUCT.md](CODE_OF_CONDUCT.md) — community standards

---

## 🛠️ Development Setup

Fork the repository, clone your fork, and branch from a current `main`.

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Use the Gradle wrappers committed to the repository. Do **not** rely on a globally installed Gradle version.

### Android

**Linux / macOS**

```bash
./gradlew assembleDebug
```

**Windows**

```powershell
.\gradlew.bat assembleDebug
```

Debug APKs are written to:

```text
app/build/outputs/apk/debug/
```

To install a debug build on a connected device:

```bash
./gradlew installDebug
```

or on Windows:

```powershell
.\gradlew.bat installDebug
```

### Windows Desktop

Windows-specific requirements and packaging commands live in the [development guide](../docs/site/development.md).

If your change affects desktop behavior, validate the **desktop target itself**. Android behavior is not proof that the Windows implementation works.

---

## 🎯 Keep Changes Focused

A pull request should be easy to explain in one or two sentences.

### Good

- fixing one playback regression;
- adding one well-scoped feature;
- polishing one screen;
- improving one provider or fallback path;
- updating documentation for a real behavior change.

### Avoid

- unrelated refactors;
- repository-wide formatting sweeps;
- speculative cleanup;
- unnecessary file moves or renames;
- unrelated dependency upgrades;
- release/version bumps outside release work;
- rewriting stable code only to match a personal preference.

Small, focused diffs are easier to understand, test, review, revert, and maintain.

If a wider refactor is genuinely required, explain **why** and keep it limited to what the implementation needs.

---

## 🛡️ Preserve Existing Behavior

Levyra has several tightly connected systems. A change in one place can easily affect another.

Take extra care around:

- playback, queue state and MediaSession behavior;
- Media3 / ExoPlayer integration;
- local and remote media;
- playlists, favorites, history and library data;
- Room entities, migrations and persisted user data;
- downloads and offline behavior;
- settings and stored preferences;
- external-service credentials and authentication state;
- networking, providers and fallback logic;
- localization;
- Android lifecycle and background work;
- Windows playback and libvlc integration;
- cleanup of resources, jobs and sessions.

> **Never silently discard or reset user data just to make an implementation easier.**

If compatibility cannot be preserved, make that explicit before merge.

---

## 🧱 Code Quality

Follow the patterns already used by the surrounding code unless there is a concrete reason to improve them.

Prefer code that is:

- **clear** — another contributor should understand it without reverse-engineering your intent;
- **focused** — each component should have a narrow responsibility;
- **consistent** — fit the architecture already owning that behavior;
- **defensive** — external services and malformed data will fail eventually;
- **explicit** — failure states should not disappear silently;
- **testable** — important behavior should be verifiable where practical;
- **simple** — avoid abstraction that exists only to look clever.

Do not add a new service layer, wrapper hierarchy, architecture, helper system, or dependency when Levyra already has a suitable owner for the behavior.

A finished PR should not contain dead code, temporary debug shortcuts, unexplained feature flags, commented-out implementations, or abandoned experiments.

---

## ✨ UI & UX

Levyra has its own visual identity. Other apps can be useful references, but they are **references — not templates to copy blindly**.

For visible changes:

- match Levyra's spacing, typography, shapes, motion and interaction language;
- preserve usability on compact and larger layouts where relevant;
- do not overcrowd a screen simply because there is unused space;
- keep important actions discoverable;
- preserve accessibility semantics and useful content descriptions;
- avoid unnecessary animations, jank and expensive UI-thread work;
- watch for excessive recomposition in Compose;
- use the existing localization system for user-facing text;
- check light/dark behavior when the surface supports both;
- include screenshots or a short recording when visual review benefits from them.

A beautiful screen that makes playback harder to reach is still a regression.

---

## 📦 Dependencies

Every dependency becomes part of Levyra's maintenance surface.

Before adding one, ask whether the platform, standard library, or an existing project dependency already solves the problem well enough.

If a new dependency is justified:

- explain what it solves;
- prefer actively maintained projects;
- verify license compatibility;
- consider APK/binary size, startup cost and memory use;
- inspect transitive dependencies;
- consider Android and Windows compatibility where relevant;
- avoid pulling in a large library for a tiny task;
- keep unrelated dependency upgrades out of the same PR.

---

## 🧪 Testing & Validation

Start with the **smallest meaningful test**, then expand validation according to the risk of the change.

Useful evidence can include:

- a targeted unit test;
- a module-specific Gradle task;
- an Android debug build;
- reproducing the original bug before and after the fix;
- playback testing on a device or emulator;
- Windows checks for desktop-specific changes;
- screenshots or recordings for UI work.

### Repository quality gates

Before committing meaningful code changes:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or publishing a pull request:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

If your environment uses `python` instead of `python3`, use the equivalent command.

> A skipped, blocked, unavailable, or never-run check is **not** a passing check.

If something cannot run because of a missing SDK, JDK, device, signing setup, native dependency, network requirement, or environment limitation, state exactly what was not run and why.

### Physical-device qualification

For Android work involving playback, MediaSession behavior, startup, lifecycle, memory, or device-specific integration, Levyra includes a Windows ADB qualification harness:

```powershell
.\scripts\levyra-device-qualification.ps1
```

Use it when it adds meaningful evidence. A documentation-only change does not need a full device qualification run.

More details are available in the [development guide](../docs/site/development.md).

---

## 🚀 Pull Requests

Use a title that says what the change actually does.

A strong PR description answers five questions:

| Section | What reviewers need to know |
| --- | --- |
| **Problem** | What is wrong, missing, confusing or unreliable? |
| **Solution** | What changed, and why this approach? |
| **User impact** | What will users notice? |
| **Validation** | What exactly did you build, test or verify? |
| **Limitations** | What remains untested or intentionally out of scope? |

Keep claims proportional to the evidence.

Avoid statements such as **“fully tested”**, **“production ready”**, **“fixed everywhere”**, or **“no regressions”** unless you genuinely have evidence supporting them.

For UI changes, include before/after visuals when practical. For behavior changes, include reproduction or testing steps when they save reviewers from having to discover the workflow themselves.

If review uncovers a separate problem, prefer a new issue or PR instead of letting the current one grow forever.

---

## 🔎 Make Reviews Easy

A reviewer should not need detective skills to understand the patch.

Good pull requests usually:

- touch only files relevant to the change;
- keep generated noise out of the diff;
- explain non-obvious tradeoffs;
- include tests for important behavior where reasonable;
- make migrations and compatibility behavior explicit;
- separate verified facts from assumptions;
- remove temporary debugging code;
- update documentation when behavior changes;
- include enough evidence to reproduce the validation.

Review comments are about the code and the health of the project. Technical disagreement is normal; keep it focused on the work.

---

## 🐛 Bug Reports

A useful bug report gives another person enough information to reproduce the problem.

Include where relevant:

- Levyra version;
- Android or Windows version;
- device or PC model;
- official release, debug build, or custom build;
- exact reproduction steps;
- expected behavior;
- actual behavior;
- whether it happens consistently or intermittently;
- useful logs, screenshots, or recordings.

Remove personal data, cookies, API keys, tokens, private URLs, account identifiers, and unrelated log content before posting anything publicly.

If the issue may be a security vulnerability, **stop and follow [SECURITY.md](SECURITY.md)** instead.

---

## 💡 Feature Requests

Start with the **user problem**, not the implementation.

A useful proposal explains:

1. what problem it solves;
2. who benefits;
3. how it fits existing Levyra behavior;
4. whether a similar capability already exists;
5. meaningful UX, privacy, performance or compatibility implications.

References to other applications are welcome when they explain an interaction or idea. **“Copy this app” is not a complete design specification.**

---

## 📝 Documentation & Localization

Documentation changes are first-class contributions.

Keep documentation accurate, maintainable, explicit about platform differences, and free of claims that cannot be verified.

If you change a command, workflow, configuration path, build requirement, or contributor-facing behavior, update the relevant documentation in the same PR when practical.

For user-facing strings:

- use Levyra's localization infrastructure;
- keep source wording concise and unambiguous;
- preserve placeholders and formatting tokens;
- avoid hardcoding layout assumptions into text;
- do not machine-edit unrelated translations just to make a diff look complete;
- do not delete translations unless the underlying string is actually removed.

---

## 🤖 AI-Assisted Contributions

AI-assisted development is allowed. **Responsibility is not delegated to the tool.**

The person opening the PR is responsible for every submitted line.

If you use a coding assistant:

- read the generated code before committing it;
- understand the behavior you are changing;
- remove unrelated churn and unnecessary abstractions;
- verify APIs, imports, paths, configuration and assumptions against the real repository;
- run actual tests;
- never present the assistant's explanation as test evidence;
- check for secrets, fabricated references, copied proprietary material and incompatible licensing;
- keep the final implementation aligned with Levyra's architecture.

> **“The AI said it works” is not validation.**

---

## 🔐 Security & Sensitive Data

Never commit or publish:

- API keys or access tokens;
- cookies or session data;
- passwords;
- signing keys or keystores;
- private certificates;
- populated `.env` files;
- populated `local.properties` files;
- private service URLs;
- credentials copied from logs or device storage;
- sensitive user information.

Suspected vulnerabilities belong in the process described by **[SECURITY.md](SECURITY.md)**, not in public issues or pull requests.

---

## ⚖️ Third-Party Code & Licensing

Levyra is licensed under the [GNU General Public License v3.0](../LICENSE).

By contributing, you agree that your contribution can be distributed under the same project license.

When adapting code from another project:

- verify license compatibility;
- preserve copyright, attribution and license notices where required;
- identify substantial reused or adapted code when the origin would otherwise be unclear;
- do not copy code from proprietary or incompatible sources;
- never remove upstream attribution to make imported code look native to Levyra.

Ideas, UX patterns and architectural concepts can often be reimplemented independently without copying source code. When in doubt, prefer a clean implementation designed for Levyra.

---

## 🧹 Repository Hygiene

Before publishing a PR, inspect your own diff.

Watch for accidental additions such as:

- build outputs;
- IDE metadata;
- temporary files;
- local reports;
- crash dumps;
- downloaded binaries;
- device logs;
- credentials;
- unrelated generated artifacts.

A clean diff is one of the easiest ways to prevent regressions and secret leaks.

---

## ✅ Final Checklist

Before submitting your pull request:

- [ ] The change has **one clear purpose**.
- [ ] The branch started from a reasonably current `main`.
- [ ] Unrelated refactors and formatting churn are excluded.
- [ ] Existing behavior and user data are preserved outside the intended scope.
- [ ] Relevant tests or manual checks were completed.
- [ ] `ai_quality_gate.py --profile fast` was run for meaningful code changes.
- [ ] `ai_quality_gate.py --profile full` was run before PR publication when required.
- [ ] Skipped or unavailable checks are disclosed honestly.
- [ ] UI changes include visual evidence when useful.
- [ ] User-facing text uses the localization system.
- [ ] No secrets or sensitive data are included.
- [ ] Third-party code and assets have compatible licensing and required attribution.
- [ ] Documentation was updated where behavior or workflow changed.
- [ ] The final diff was reviewed before submission.

---

## 🤝 Community

Be respectful to maintainers, contributors, and users.

Technical disagreement is expected in an active project. Personal attacks, harassment, and hostile behavior are not.

Please read the [Code of Conduct](CODE_OF_CONDUCT.md) before participating in project discussions.

---

**Thanks for helping make Levyra better.**