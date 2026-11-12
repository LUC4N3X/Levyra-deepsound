# Contributing to Levyra

I maintain Levyra's Android and Windows apps and review contributions here. You can help with a bug fix, a translation, documentation or UI work. You don't need to work on both apps.

## Before writing code

For a small fix, documentation, translation or UI polish, you can open a PR directly. If a feature touches several parts of the app, discuss it in an issue first. For architecture changes, database changes or new modules, open an RFC issue before starting.

Keep each PR about one problem. Leave unrelated refactors, formatting, dependency updates, file moves and generated files out of it.

## Working on a change

Fork the repository and branch from `main`. The [development guide](../docs/site/development.md) covers setup and the Windows Desktop toolchain. Always use the repository's Gradle wrapper.

To build Android, run `./gradlew assembleDebug` in a Unix shell or `.\gradlew.bat assembleDebug` in Windows PowerShell. The [Desktop README](../desktop/README.md) has the Windows app's build steps.

Follow the patterns in the module you're changing. Preserve behavior and user data outside your fix. Take particular care with playback, queues, Room migrations, downloads, playlists, credentials, provider fallbacks and lifecycle state. Never reset a database or preferences to make an implementation easier.

For UI changes, follow Levyra's existing spacing, typography, shapes, motion and accessibility labels. Check recomposition and work on the UI thread. Include screenshots or a short recording.

The repository rules are in [AGENTS.md](../AGENTS.md).

## Testing

Start with the checks that cover your change, such as a unit test, module build or device check.

Before committing:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or opening a PR:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

For Android changes to playback, MediaSession, startup, lifecycle or memory performance, also run `.\scripts\levyra-device-qualification.ps1` in PowerShell.

If a check can't run in your environment, say which one and why. Only mark checks as passed if they ran and passed.

## Opening a PR

Fill in the PR template. Tell me what was broken, what you changed, what the user will notice and how you tested it. List anything you couldn't verify. Add reproduction steps for bugs when useful, and before-and-after screenshots for UI changes.

If review turns up an unrelated problem, open a separate issue or PR for it.

## AI tools

You can use AI coding tools, but you're responsible for every line you submit. Read the output, remove unrelated changes, check APIs against the codebase, and run the tests. Don't submit invented APIs, proprietary code or code with an incompatible license. A model saying it works doesn't count as a test.

## Security and licensing

Don't commit tokens, passwords, keystores, signing keys, populated `.env` or `local.properties` files, or user data. Report vulnerabilities privately through [SECURITY.md](SECURITY.md).

Levyra uses the [GNU GPL v3.0](../LICENSE). Check that any code you reuse is GPL-3.0 compatible, and keep the required copyright notices and attribution.

Keep discussions respectful. The [Code of Conduct](CODE_OF_CONDUCT.md) applies throughout the project.
