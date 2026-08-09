# Levyra Engineering Instructions

## Purpose and hierarchy

This file is the repository-wide operating contract for coding agents. Codex,
Google Antigravity, OpenClaw, and compatible runtimes should read it from the
Git root, then apply any nearer `AGENTS.md` for the files in scope.

Instruction order:

1. root `AGENTS.md`;
2. nearer path-specific `AGENTS.md` files;
3. approved requirements and active planning in `docs/project/SPEC.md`,
   `docs/project/ROADMAP.md`, and `docs/project/TASKS.md`;
4. matching native skills under `.agents/skills/`;
5. current architecture, implementation, tests, build files, and workflows;
6. detailed Levyra playbooks under `.claude/skills/` and `.claude/rules/`.

Current repository evidence always overrides remembered behavior, old
discussions, stale comments, previous agent output, or stale task status.
Surface conflicts between planning files and implementation before editing.

## Repository map

- `docs/README.md`: canonical documentation index.
- `docs/project/SPEC.md`: durable product and engineering requirements.
- `docs/project/ROADMAP.md`: ordered outcomes, risks, and phase exit criteria.
- `docs/project/TASKS.md`: one active reviewable phase and its truthful
  validation state.
- `app/`: Android client; additional rules in `app/AGENTS.md`.
- `desktop/`: independent Windows client; additional rules in
  `desktop/AGENTS.md`.
- `.github/`: CI and release automation; additional rules in
  `.github/AGENTS.md`.
- `docs/`: project documentation; additional rules in `docs/AGENTS.md`.
- `docs/project/`: product specification, engineering roadmap, and active task
  phase.
- `docs/ai/`: ChatGPT, Codex, Google Antigravity, Claude Code, and OpenClaw
  collaboration guidance.
- `.agents/rules/`: lightweight workspace rules that link to this canonical
  contract.
- `.agents/skills/`: native Codex, Antigravity, OpenAI, and OpenClaw-compatible
  skills.
- `.claude/`: Claude Code configuration plus reusable Levyra engineering
  playbooks.

## Agent runtime discovery

- Root `AGENTS.md` is the canonical repository context for supported coding
  agents.
- Google Antigravity discovers workspace skills from
  `.agents/skills/<skill-name>/SKILL.md` when a conversation starts.
- `.agents/rules/levyra-workspace.md` links back to this file with a relative
  `@../../AGENTS.md` reference instead of maintaining a duplicate contract.
- Open the repository root as the workspace. Starting only from `app/`,
  `desktop/`, or another nested folder may hide repository-level agent
  configuration.
- Start a new agent conversation after pulling instruction or skill changes so
  the runtime can rebuild its workspace inventory.
- See `docs/ai/ANTIGRAVITY.md` for Antigravity setup and verification.

Runtime-specific discovery must not create separate sources of truth. This file,
nearest scoped instructions, approved planning, matching skills, and current
repository evidence remain the shared hierarchy.

## RTK agent bootstrap

The repository owner authorizes supported coding agents to bootstrap only the
official `rtk-ai/rtk` command-output tool when working in this repository.
Before the first noisy shell task in a session:

1. run `rtk --version` and `rtk gain` without RTK wrapping;
2. if either command fails, run `scripts/setup-ai.ps1 -InstallRtk` on Windows or
   `./scripts/setup-ai.sh --install-rtk` on Linux/macOS;
3. let the setup script install the pinned RTK revision through Cargo and initialize
   integrations only for detected supported agents;
4. if Cargo is unavailable or installation fails, report the blocked bootstrap
   once and continue with raw commands instead of downloading an unverified
   executable or weakening validation.

This standing authorization applies only to the pinned `rtk-ai/rtk` bootstrap.
Plugins, other executables, unrestricted sandboxing, approval bypasses, commit,
push, pull request, merge, tag, release, and repository settings still require
their normal explicit authorization. Keep security, signing, checksum, secret,
and exact reproduction evidence raw.

## Product invariants

- Protect playback reliability, responsiveness, privacy, user data, and
  existing user choices before visual polish.
- Android users explicitly choose song/audio mode or native-video mode. Never
  remove, merge, hide, or silently override that choice.
- Motion artwork is decorative, muted, and limited to song/audio mode. It must
  never replace native video, produce audible output, or delay playback.
- Static artwork is the immediate and permanent fallback.
- Android audible playback, MediaSession, notification, Android Auto, queue,
  and background service must remain synchronized.
- Direct playback is the critical path. Artwork, lyrics, refresh, diagnostics,
  prefetch, and enrichment must yield to it.
- Preserve downloads, favorites, playlists, queues, lyrics, history, settings,
  localization, onboarding, sessions, and backups unless explicitly changed.
- Do not add account login, cookies, private tokens, scraping, telemetry, or
  tracking unless explicitly requested.
- Android and Desktop versions, packages, tags, artifacts, and releases remain
  independent.

## Native skill routing

Load every matching skill before reading widely or editing. Prefer focused
skills over the general coordinator.

| Task | Skill |
| --- | --- |
| Requirements, roadmap, active phase, acceptance criteria, task status, implementation handoff | `levyra-project-manager` |
| OpenClaw delegation, coding-runtime coordination, evidence collection, safe publication handoff | `levyra-openclaw-orchestrator` |
| Android playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video mode | `levyra-player` |
| InnerTube, extraction, stream resolution, runtime configuration, retry, cache, fallback | `levyra-extractor` |
| Room, DAO, migration, schema, cache, store, backup, persistent personal data | `levyra-database` |
| Android Compose UI, state, navigation, animation, lifecycle, accessibility, RTL, localization | `levyra-compose` |
| Decorative motion artwork | `levyra-motion-artwork` |
| Windows Desktop, Compose Multiplatform, libvlc, downloads, mini player, deep links, updates, packaging | `levyra-desktop` |
| Secrets, URLs, redirects, SSRF, MIME, permissions, privacy, update integrity | `levyra-security-review` |
| GitHub Actions, CI, F-Droid, configuration sync, artifacts, build/release automation | `levyra-ci-workflows` |
| Branch, commit, patch, or pull request review | `levyra-pr-review` |
| Pre-merge or pre-release validation, versions, signing, checksums, packaging | `levyra-release-check` |
| Genuine cross-domain work or initial architecture orientation | `levyra-engineering` |

Several skills may apply. A playback change that modifies stream resolution
uses player and extractor skills; provider-controlled media normally also
requires security review. A tracked multi-phase task additionally uses
`levyra-project-manager`; OpenClaw coordination additionally uses
`levyra-openclaw-orchestrator`.

## Planning documents

Read only the planning material relevant to the task, but do not ignore an
active phase:

- `docs/project/SPEC.md` defines approved durable requirements and non-goals.
- `docs/project/ROADMAP.md` orders outcomes, risks, and phase exit criteria; it
  is not release authorization.
- `docs/project/TASKS.md` records one active reviewable phase, validation
  evidence, and owner checkpoints.
- `docs/ARCHITECTURE.md` describes current implementation ownership and data
  flow.
- `docs/ai/WORKFLOW.md` defines the complete AI-assisted lifecycle.
- `docs/ai/ANTIGRAVITY.md` defines Antigravity discovery, workspace, and
  verification guidance.
- `docs/ai/OPENCLAW.md` defines the recommended OpenClaw role and tool
  boundaries.

Do not mark task status from an agent report. Update it only from a direct
command, CI result, review, device check, or owner decision.

## Engineering rules

- Preserve unidirectional data flow: user intent -> controller/ViewModel ->
  repository/player operation -> immutable state -> UI.
- Keep network, database, parsing, decoding, file, metadata, and blocking native
  work off UI threads.
- Reuse existing clients, stores, caches, scopes, dispatchers, queues, lifecycle
  owners, extractors, players, and persistence.
- Do not create a second source of truth for playback, queue, persistence,
  localization, update state, release state, requirements, or active task
  status.
- Make ownership explicit for coroutines, players, callbacks, receivers,
  surfaces, native handles, decoders, files, caches, and in-flight work.
- One caller cancelling shared work must not cancel work still required by
  another caller.
- Use identity and generation checks when older asynchronous work can publish
  after newer work.
- Re-throw `CancellationException`; never cache or report cancellation as a
  normal miss.
- Distinguish conclusive no-match from timeout, transport, server, parsing,
  verification, and stale-configuration failures.
- Do not negative-cache inconclusive failures.
- Bound retries, timeouts, concurrency, response sizes, cache/storage growth,
  downloads, and prefetch.
- Keep durable identity independent from mutable display text.

## Work method

1. Define the exact requested outcome and scope.
2. Read `docs/project/SPEC.md`, the relevant roadmap track, and the active
   `docs/project/TASKS.md` phase when applicable.
3. Identify behavior and compatibility that must remain unchanged.
4. Inspect the complete current control/data flow and nearby tests.
5. Identify the root cause before editing.
6. Make the smallest coherent change compatible with current architecture.
7. Avoid unrelated cleanup, formatting churn, dependency upgrades, renames,
   and broad refactors.
8. Add or update regression tests for defects, migrations, matching, security
   boundaries, lifecycle, and concurrency when applicable.
9. Run focused checks first, then applicable broader checks.
10. Inspect the complete final diff for unrelated edits, generated files,
    secrets, binaries, conflict markers, and accidental version changes.
11. Synchronize `docs/project/SPEC.md`, `docs/project/ROADMAP.md`,
    `docs/project/TASKS.md`, architecture, and user documentation when the
    approved requirement or architecture changes.
12. Report exactly what changed, what ran, what passed, what failed, and what
    remains unverified.

When the owner says "only this", modify only the named behavior or files unless
an additional change is strictly required for correctness. State that dependency
before expanding scope.

## Validation

Use repository wrappers, never a system Gradle installation.

Agent configuration checks from the repository root:

```bash
python3 scripts/validate_agent_config.py
```

Android checks from the repository root:

```bash
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:lintRelease
./gradlew --no-daemon --no-configuration-cache assembleRelease
git diff --check
```

Desktop checks from `desktop/`:

```bash
./gradlew check
./gradlew assemble check
```

On Windows use `gradlew.bat`.

Start with the narrowest relevant test. Missing SDKs, JDKs, signing inputs,
libvlc, WiX, network, CI, emulator, device, or OS support are blocked checks,
not passes. Never claim build, playback, device, Android Auto, notification,
PiP, installer, update, protocol, media-key, native VLC, agent-configuration,
review, or CI success without direct evidence.

## Security and repository safety

- Never commit or expose passwords, secrets, tokens, cookies, private URLs,
  keystores, signing material, `.env`, or `local.properties`.
- Never commit APKs, installers, ZIPs, build output, IDE state, native runtime
  bundles, or temporary diagnostics unless explicitly required and accepted by
  repository policy.
- Validate provider-controlled URLs across scheme, host, port, user info,
  DNS/IP destination, every redirect hop, MIME, timeout, filename/path, and
  response-size bounds.
- Preserve least privilege in Android permissions, GitHub workflow permissions,
  coding-agent tools, and OpenClaw agent allowlists.
- Do not weaken transport, redirect, MIME, checksum, signature, or host
  validation to make one response pass.
- Treat fork code, workflow inputs, downloaded artifacts, deep links, update
  metadata, filenames, local IPC, and third-party skills as untrusted where
  applicable.
- Update credits and licenses when adding external code, assets, models,
  libraries, native files, or design references.

## Versions, releases, and external actions

- Do not change Android or Desktop version values unless the task explicitly
  requests that platform's release/version change.
- Do not commit, push, open a pull request, merge, tag, publish, release, or
  change repository settings without explicit authorization.
- When publication is authorized, use a dedicated branch and draft pull request
  by default. Push directly to `main` only when explicitly requested for the
  exact scope.
- OpenClaw or any delegated coding runtime must not infer publication, merge,
  tag, or release permission from an implementation request.
- Keep PR descriptions and checklists truthful; leave manual/device checks
  unmarked until actually performed.

## Delivery contract

Report:

- root cause or rationale;
- exact files changed;
- behavior preserved;
- tests and checks run with results;
- skipped or blocked checks and why;
- remaining risks and manual validation;
- professional commit message when requested;
- verified branch, commit, PR, merge, or release state when applicable.

Never represent a plan as an applied patch or an unverified result as completed.
Distinguish planned, edited, locally validated, committed, pushed, pull request
opened, CI passed, reviewed, merged, and released.
