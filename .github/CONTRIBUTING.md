# Contributing to Levyra

Thanks for wanting to help with Levyra.

Levyra changes quickly, so focused contributions are much easier to review and maintain than large PRs that touch everything at once. You do not need to know the whole codebase before contributing; just keep the scope clear and test the part you changed.

## Before you start

| Change | What to do |
| --- | --- |
| Small bug fix, docs, translation, UI polish | Open a focused PR |
| Feature touching several areas | Discuss it first |
| Architecture, database, or new subsystem | Open an issue first |
| Security issue | Follow [SECURITY.md](SECURITY.md) and keep the details private |

Please keep unrelated cleanup out of the same PR. In particular, avoid broad refactors, formatting sweeps, dependency updates, file moves, or version bumps unless they are actually needed for the change.

A few things are worth keeping in mind:

- preserve existing behavior and user data outside the scope of the change;
- use the code and architecture already in place instead of building a second system beside it;
- do not report a test as passing if it was not run;
- never commit secrets, credentials, signing material, or private data;
- other apps are useful references, but Levyra should still look and behave like Levyra.

[Development guide](../docs/site/development.md) · [AGENTS.md](../AGENTS.md) · [Security](SECURITY.md) · [Code of Conduct](CODE_OF_CONDUCT.md)

---

<details>
<summary><strong>Development setup</strong></summary>

<br>

Fork the repository, clone your fork, and create a branch from a current `main`.

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Use the Gradle wrappers included in the repository.

Android:

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

Windows Desktop requirements, packaging commands, device qualification, and current toolchain requirements are documented in the [development guide](../docs/site/development.md).

</details>

<details>
<summary><strong>Code and UI</strong></summary>

<br>

Try to follow the style and ownership already used around the code you are changing. There is usually no reason to rewrite stable code just to make it look different.

Be especially careful around playback, queues, Media3, Room migrations, playlists, favorites, history, downloads, settings, credentials, provider fallbacks, lifecycle behavior, and Windows playback. Changes in those areas can have effects well outside the file being edited.

For UI work, keep Levyra's existing spacing, typography, shapes, motion, localization, and accessibility behavior in mind. Avoid adding controls simply because there is empty space, and watch for unnecessary recomposition or work on the UI thread.

Screenshots or a short recording are useful when the change is visual.

Do not silently reset or discard user data to make an implementation easier.

</details>

<details>
<summary><strong>Testing</strong></summary>

<br>

Run the smallest useful test first: a targeted unit test, module task, debug build, manual reproduction, device/emulator check, or desktop test depending on what changed.

Before committing meaningful code changes:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or opening a PR:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Use `python` instead of `python3` if that is how Python is configured on your system.

If a required check could not run because of your local setup, say what was skipped and why. A skipped check is not a passing check.

For Android changes involving playback, MediaSession, startup, lifecycle, memory, or device-specific behavior, the repository also includes:

```powershell
.\scripts\levyra-device-qualification.ps1
```

</details>

<details>
<summary><strong>Pull requests</strong></summary>

<br>

Keep the PR description practical. It should be enough for someone else to understand the change without reading the entire diff first.

Include:

- what was wrong or missing;
- what you changed;
- anything users will notice;
- what you tested;
- anything you could not test.

For UI changes, add before/after screenshots when they help. For behavior changes, include reproduction or test steps if the reviewer would otherwise have to work them out from scratch.

If review uncovers a different problem, it is usually better handled in another issue or PR than added to the current one.

</details>

<details>
<summary><strong>AI-assisted work</strong></summary>

<br>

Using a coding assistant is fine. The person opening the PR is still responsible for the result.

Read generated code before committing it, remove unrelated churn, check APIs and paths against the actual repository, and run real tests. Also check generated work for secrets, made-up references, copied proprietary material, or licensing problems.

An assistant saying the change works is not test evidence.

</details>

<details>
<summary><strong>Security and licensing</strong></summary>

<br>

Do not commit API keys, tokens, cookies, passwords, signing keys, keystores, private certificates, populated `.env` or `local.properties` files, private service URLs, or sensitive user information.

Possible vulnerabilities should be reported through [SECURITY.md](SECURITY.md), not in a public issue or PR.

Levyra is licensed under the [GNU GPL v3.0](../LICENSE). If you adapt code from another project, check that the license is compatible and keep any copyright, attribution, or license notices that are required.

</details>

---

## Before you open a PR

- [ ] The change has one clear purpose.
- [ ] Unrelated refactors and generated files are not included.
- [ ] Existing behavior and user data are preserved outside the intended scope.
- [ ] The relevant tests or manual checks were run.
- [ ] Anything that could not be tested is mentioned in the PR.
- [ ] UI changes include screenshots or a recording when useful.
- [ ] No secrets or private data are included.
- [ ] Reused code or assets have compatible licensing and the required attribution.

Please keep discussions technical and respectful. The [Code of Conduct](CODE_OF_CONDUCT.md) applies to project discussions and contributions.

Thanks for contributing.