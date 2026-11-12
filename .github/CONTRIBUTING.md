# Contributing to Levyra

I maintain the Android and Windows apps and review contributions here. A small, focused PR is easier to review and test. You don't need to know the whole codebase to fix one problem.

## Before you start

| Change | What to do |
| --- | --- |
| Small bug fix, documentation, translations, or UI polish | Open a focused PR directly |
| Feature touching multiple areas | Discuss it in an issue first |
| Major architecture, database changes, or new modules | Open an RFC issue first |
| Security vulnerability | Follow [SECURITY.md](SECURITY.md) and report it privately |

Keep unrelated cleanup out of your PR. Refactors, formatting changes, dependency updates and file moves should serve the change you're making.

- Preserve behavior and user data outside your change.
- Use the architecture and owners already in the module.
- Only mark a check as passed if you ran it and it passed.
- Don't commit credentials, private tokens or personal data.
- Follow Levyra's existing design and interactions.

The [development guide](../docs/site/development.md) covers setup. [AGENTS.md](../AGENTS.md) describes the repository rules. The [security policy](SECURITY.md) and [Code of Conduct](CODE_OF_CONDUCT.md) also apply.

<details>
<summary>Development setup</summary>

Fork the repository, clone your fork, and branch from `main`:

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Use the repository's Gradle wrapper.

To build Android from a Unix shell:

```bash
./gradlew assembleDebug
```

To build Android from Windows PowerShell:

```powershell
.\gradlew.bat assembleDebug
```

For the Windows Desktop toolchain, MSI packaging and ADB device qualification, see the [development guide](../docs/site/development.md).

</details>

<details>
<summary>Code and UI guidelines</summary>

Follow the patterns in the module you touch. Leave working code alone unless your change needs it.

Take extra care with playback, Media3, queues, Room migrations, playlists, favorites, downloads, credentials, provider fallbacks and lifecycle state. A regression here can stop playback or lose someone's data. Don't reset databases or preferences to make a change easier to implement.

For UI work, follow the existing spacing, typography, corner radii, motion and accessibility labels. Keep controls useful and check recomposition and work on the UI thread. Include screenshots or a short recording for visual changes.

</details>

<details>
<summary>Testing</summary>

Start with the checks that cover your change. That might be a unit test, a module build, a device check or a Desktop test.

Before committing:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or opening a PR:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

If a check can't run in your environment, say which one and why. Don't count it as passed.

For Android changes to playback, MediaSession, startup, lifecycle or memory performance, also run the device harness:

```powershell
.\scripts\levyra-device-qualification.ps1
```

</details>

<details>
<summary>Pull requests</summary>

Fill in the repository's PR template. Tell me what problem you fixed, what changed for the user, what you tested, and what you couldn't verify.

Include before-and-after screenshots for UI changes. For bugs, add reproduction steps when they help explain the fix.

If review turns up an unrelated problem, open a separate issue or PR for it.

</details>

<details>
<summary>AI-assisted contributions</summary>

You can use AI coding tools. You're still responsible for every line you submit.

Read the generated code, remove unrelated changes, check APIs against the current codebase, and run the tests. Don't submit proprietary code, invented APIs or code with an incompatible license.

A model saying the code works doesn't count as a test.

</details>

<details>
<summary>Security and licensing</summary>

Don't commit API tokens, passwords, keystores, private signing keys, populated `.env` or `local.properties` files, or user data.

Report vulnerabilities through [SECURITY.md](SECURITY.md), never with details in a public issue or PR.

Levyra uses the [GNU GPL v3.0](../LICENSE). Before adapting code from another project, check that its license is GPL-3.0 compatible and keep the required copyright and license notices.

</details>

## Before submitting

- [ ] The PR has one clear purpose.
- [ ] There are no unrelated refactors or generated files.
- [ ] Existing behavior and user data are preserved.
- [ ] I ran the relevant tests and quality gates.
- [ ] I listed any skipped or unverified checks in the PR description.
- [ ] I included screenshots or recordings for visual changes.
- [ ] I haven't committed secrets or sensitive data.
- [ ] Reused code has a compatible license and the required attribution.

Keep technical discussions respectful. The [Code of Conduct](CODE_OF_CONDUCT.md) applies throughout the project.
