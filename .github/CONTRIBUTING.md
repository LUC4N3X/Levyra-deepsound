# Contributing to Levyra

Thank you for contributing to Levyra.

Levyra moves quickly, so focused pull requests are much easier to review and test than large PRs that touch multiple subsystems at once. You do not need to understand the entire codebase to contribute; just keep the scope well-defined and test your changes.

## Before you start

| Change | What to do |
| --- | --- |
| Small bug fix, documentation, translations, or UI polish | Open a focused PR directly |
| Feature touching multiple areas | Discuss it in an issue first |
| Major architecture, database changes, or new modules | Open an RFC issue first |
| Security vulnerability | Follow [SECURITY.md](SECURITY.md) and report it privately |

Please keep unrelated cleanups out of functional PRs. In particular, avoid broad refactors, reformatting sweeps, dependency bumps, or moving files unless directly relevant to the change.

Key guidelines to keep in mind:

- Preserve existing behavior and user data outside the scope of your change.
- Work with existing architecture and patterns rather than introducing parallel systems.
- Do not mark checks as passed unless they actually ran and passed.
- Never commit credentials, private tokens, or sensitive personal data.
- Maintain Levyra's distinct design and interaction identity.

Helpful links: [Development guide](../docs/site/development.md) | [AGENTS.md](../AGENTS.md) | [Security policy](SECURITY.md) | [Code of Conduct](CODE_OF_CONDUCT.md)

---

<details>
<summary><strong>Development setup</strong></summary>

<br>

Fork the repository, clone your fork, and create a branch from `main`:

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Always use the Gradle wrapper scripts provided in the repository:

Android:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

For complete Windows Desktop toolchain requirements, MSI packaging commands, and ADB device qualification scripts, refer to the [development guide](../docs/site/development.md).

</details>

<details>
<summary><strong>Code and UI guidelines</strong></summary>

<br>

Follow the architectural patterns and ownership conventions already present in the modules you touch. Avoid refactoring stable, working code purely for aesthetic reasons.

Exercise extra care around core audio subsystems: Media3 playback, queue management, Room migrations, playlists, favorites, offline downloads, credentials, provider fallbacks, and lifecycle state. Regressions in these areas can have widespread side effects.

For UI changes, respect existing spacing, typography, corner radii, motion tokens, and accessibility labels. Avoid cluttering screens with unnecessary controls, and be mindful of recomposition performance and UI thread overhead.

Include screenshots or brief screen recordings for visual changes.

Never reset or overwrite user databases or preferences to simplify an implementation.

</details>

<details>
<summary><strong>Testing</strong></summary>

<br>

Run targeted checks first: a focused unit test, module build, manual verification on device, or desktop test depending on your changes.

Before committing changes, run the fast quality gate:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing your branch or opening a PR, run the full quality gate:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

If an environment-specific check cannot run locally, state clearly what was skipped and why.

For Android changes touching playback, MediaSession, startup, lifecycle, or memory performance, run the device test harness:

```powershell
.\scripts\levyra-device-qualification.ps1
```

</details>

<details>
<summary><strong>Pull requests</strong></summary>

<br>

Keep your pull request description concise and informative:

- What problem is being solved
- What was changed
- User-facing impacts
- What was tested
- Any checks that were skipped or could not run

For UI changes, include before-and-after screenshots. For bug fixes, include reproduction steps if helpful.

If code review reveals an unrelated issue, handle it in a separate issue or PR rather than expanding the current one.

</details>

<details>
<summary><strong>AI-assisted contributions</strong></summary>

<br>

Using AI coding tools is fine, but you remain responsible for the code you submit.

Review all generated code before committing, remove unnecessary changes, verify APIs against the current codebase, and run tests. Ensure that no proprietary code, fake APIs, or license violations are introduced.

An AI model claiming code works is not a substitute for running actual tests.

</details>

<details>
<summary><strong>Security and licensing</strong></summary>

<br>

Do not commit API tokens, passwords, keystores, private signing keys, populated `.env` or `local.properties` files, or user data.

Potential security issues should always be reported via [SECURITY.md](SECURITY.md), never in public PRs or issues.

Levyra is licensed under the [GNU GPL v3.0](../LICENSE). If adapting code from external projects, verify that the license is GPL-3.0 compatible and retain all required upstream copyright and license notices.

</details>

---

## Pre-submission checklist

- [ ] The change has a single, well-defined purpose.
- [ ] Unrelated refactors and generated files are omitted.
- [ ] Existing behavior and user data are preserved.
- [ ] Targeted tests and quality gates were executed.
- [ ] Any skipped or unverified checks are noted in the PR description.
- [ ] Visual UI changes include screenshots or recordings.
- [ ] No secrets or sensitive data are committed.
- [ ] Reused code includes proper license compatibility and attribution.

Please keep technical discussions constructive and respectful. Our [Code of Conduct](CODE_OF_CONDUCT.md) applies across all project interactions.