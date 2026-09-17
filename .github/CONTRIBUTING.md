# Contributing to Levyra

> **Build carefully. Test honestly. Keep Levyra fast.**

Thanks for helping improve **Levyra**.

Levyra is an open-source music player for **Android** and **Windows**. Contributions are welcome when they solve a clear problem, fit the existing architecture, preserve working behavior, and include honest validation.

## Start here

| Change | Best path |
| --- | --- |
| Small bug fix, docs, translation, UI polish | Open a focused PR |
| New feature touching several areas | Discuss it first |
| Architecture, database, or subsystem change | Open an issue first |
| Security vulnerability | Follow **[SECURITY.md](SECURITY.md)** — do not disclose details publicly |

A few rules matter more than everything else:

- **One PR, one purpose.** Keep unrelated refactors and cleanup out.
- **Preserve user data and existing behavior** outside the intended scope.
- **Follow the architecture already owning the feature** instead of adding parallel systems.
- **Do not claim tests passed unless they actually ran and passed.**
- **Never commit secrets, credentials, signing material, or private data.**
- Other apps are useful references, but Levyra should keep its own UI and identity.

Useful references: [Development guide](../docs/site/development.md) · [AGENTS.md](../AGENTS.md) · [Security](SECURITY.md) · [Code of Conduct](CODE_OF_CONDUCT.md)

---

<details>
<summary><strong>🛠 Development setup</strong></summary>

<br>

Fork the repository, clone your fork, and branch from a current `main`.

```bash
git clone https://github.com/<your-user>/Levyra-deepsound.git
cd Levyra-deepsound
git checkout main
git pull --ff-only
git checkout -b <your-branch-name>
```

Use the Gradle wrappers committed to the repository.

**Android**

```bash
./gradlew assembleDebug
```

Windows:

```powershell
.\gradlew.bat assembleDebug
```

For Windows Desktop requirements, packaging, device qualification, and current toolchain requirements, use the [development guide](../docs/site/development.md).

</details>

<details>
<summary><strong>Code, architecture & UI</strong></summary>

<br>

Keep changes small enough to understand and review. Avoid unrelated dependency upgrades, formatting sweeps, file moves, speculative cleanup, or rewrites of stable code.

Take particular care around playback, queues, Media3, Room migrations, playlists, favorites, history, downloads, settings, credentials, provider fallbacks, localization, lifecycle behavior, and Windows playback.

For UI work:

- match Levyra's spacing, typography, motion, shapes, and interaction language;
- keep important actions discoverable;
- preserve accessibility and localization;
- avoid unnecessary recomposition, jank, and heavy UI-thread work;
- include screenshots or a short recording when visual review benefits from them.

**Never silently reset or discard user data to simplify an implementation.**

</details>

<details>
<summary><strong>🧪 Testing & validation</strong></summary>

<br>

Start with the smallest meaningful validation: a focused unit test, module task, debug build, manual reproduction, device/emulator check, or desktop test depending on the change.

Before committing meaningful code changes:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before pushing or publishing a pull request:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Use `python` instead of `python3` if that is the configured launcher on your system.

A skipped, blocked, unavailable, or never-run check is **not** a passing check. If something could not be tested, say exactly what and why.

For Android work involving playback, MediaSession, startup, lifecycle, memory, or device-specific behavior, Levyra also includes:

```powershell
.\scripts\levyra-device-qualification.ps1
```

</details>

<details>
<summary><strong>Pull request standard</strong></summary>

<br>

A good PR should make five things obvious:

| | |
| --- | --- |
| **Problem** | What was wrong, missing, or unreliable? |
| **Solution** | What changed and why? |
| **User impact** | What will users notice? |
| **Validation** | What exactly was tested or built? |
| **Limitations** | What remains untested or intentionally out of scope? |

Keep claims proportional to the evidence. Avoid phrases such as "fully tested", "production ready", "fixed everywhere", or "no regressions" unless the evidence genuinely supports them.

If review uncovers a separate problem, prefer a separate issue or PR instead of expanding the current one indefinitely.

</details>

<details>
<summary><strong>AI-assisted contributions</strong></summary>

<br>

AI-assisted development is allowed. The contributor opening the PR is still responsible for every submitted line.

Review generated code, understand what it changes, remove unnecessary churn, verify APIs and paths against the real repository, and run real tests.

Do not treat an assistant's explanation as validation, and check generated work for fabricated references, secrets, proprietary material, or incompatible licensing.

> **"The AI said it works" is not validation.**

</details>

<details>
<summary><strong>🔐 Security, secrets & licensing</strong></summary>

<br>

Never commit API keys, tokens, cookies, passwords, signing keys, keystores, private certificates, populated `.env` or `local.properties` files, private service URLs, or sensitive user information.

Suspected vulnerabilities belong in **[SECURITY.md](SECURITY.md)**, not in public issues or pull requests.

Levyra is licensed under the [GNU GPL v3.0](../LICENSE). When adapting third-party code, verify license compatibility and preserve required copyright, attribution, and license notices. Do not copy proprietary or incompatible source code.

</details>

---

## ✅ Before you open a PR

- [ ] The change has one clear purpose.
- [ ] Unrelated refactors and generated noise are excluded.
- [ ] Existing behavior and user data are preserved outside the intended scope.
- [ ] Relevant tests or manual checks were completed.
- [ ] Required quality gates were run, or unavailable checks are disclosed honestly.
- [ ] UI changes include visual evidence when useful.
- [ ] No secrets or sensitive data are included.
- [ ] Third-party code and assets have compatible licensing and attribution.

Be respectful to maintainers, contributors, and users. Technical disagreement is normal; personal attacks and harassment are not. See the [Code of Conduct](CODE_OF_CONDUCT.md).

**Thanks for helping make Levyra better.**