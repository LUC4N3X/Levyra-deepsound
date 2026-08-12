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

For production-code implementation or broad review, also read
`docs/ai/AI_ENGINEERING_GUARDRAILS.md` and apply its reuse-first, explicit
assumption/tradeoff, simpler-alternative, goal-verification, surgical-edit,
complexity-budget, and diff-quality rules.

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
- `docs/agents/`: repository-specific configuration consumed by external agent
  skills, including issue-tracker and domain-document conventions.
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

## Agent skills

### Issue tracker

Levyra tracks issues and publishable engineering tickets in GitHub Issues for
`LUC4N3X/Levyra-deepsound`. Reads may use the runtime's authenticated GitHub
integration or `gh`; issue writes require explicit owner authorization. See
`docs/agents/issue-tracker.md`.

### Domain docs

Levyra uses a single shared domain-language context. Read root `CONTEXT.md` and
relevant `docs/adr/` entries when they exist; create either lazily only when a
reusable term or durable architectural decision actually warrants it. See
`docs/agents/domain.md`.

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

This standing authorization applies to the pinned `rtk-ai/rtk` bootstrap and
the focused Matt Pocock skill bootstrap documented below. Other plugins,
executables, unrestricted sandboxing, approval bypasses, commit, push, pull
request, merge, tag, release, and repository settings still require their normal
explicit authorization. Keep security, signing, checksum, secret, and exact
reproduction evidence raw.

## Matt Pocock skills bootstrap

The repository owner authorizes the exact `mattpocock/skills` engineering
integration defined by this repository:

- Codex: `scripts/setup-ai.ps1` / `scripts/setup-ai.sh` may use the official
  `skills` CLI through `npx` to install only the focused allowlist declared in
  those scripts, globally for the Codex agent. Use `-SkipMattSkills` or
  `--skip-matt-skills` to opt out on a machine.
- Claude Code: `.claude/settings.json` enables
  `mattpocock-skills@claude-plugins-official`; Claude's normal project trust and
  plugin installation controls remain intact.
- ChatGPT and Antigravity: use the repository-native
  `levyra-real-engineering` adapter whether or not an upstream package is
  installed in the runtime.

The one-time upstream repository configuration is represented by this
`## Agent skills` block plus `docs/agents/issue-tracker.md` and
`docs/agents/domain.md`. `triage` is intentionally not installed or configured.
Do not rerun `setup-matt-pocock-skills` during ordinary work unless the owner
wants to change the issue tracker or domain-doc layout.

External skills are supplementary. `AGENTS.md`, approved planning, current
architecture, focused `levyra-*` skills, tests, quality gates, and explicit
owner publication controls always take precedence. See
`docs/ai/MATT_POCOCK_SKILLS.md`.

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
| Non-trivial feature, architectural change, unclear defect, or multi-step work needing clarification/spec/tickets/implementation/review separation | `levyra-real-engineering` |
| Android playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video mode | `levyra-player` |
| InnerTube, extraction, stream resolution, runtime configuration, retry, cache, fallback | `levyra-extractor` |
| Room, DAO, migration, schema, cache, store, backup, persistent personal data | `levyra-database` |
| Android Compose UI, state, navigation, animation, lifecycle, accessibility, RTL, localization | `levyra-compose` |
| Android jank, frame misses, latency, startup, Perfetto/System Trace, CPU/thread state, graphics, Binder/IPC, blocking, memory, I/O, power, or measured runtime-performance investigation | `levyra-android-performance` plus the affected domain skill |
| R8, Proguard, minification, resource shrinking, keep/consumer rules, release-only shrinker crashes, mapping/missing classes, reflection/serialization/JNI shrinker issues, or measured APK-size work | `levyra-r8-proguard` plus `levyra-release-check`; add `levyra-ci-workflows` for build-tooling changes |
| Android Intent, deep link, PendingIntent, exported activity/service/receiver/provider, nested Intent, `onNewIntent`, URI grant, FileProvider/ContentProvider, or caller/signature verification | `levyra-android-intent-security` plus `levyra-security-review` and the affected Android domain skill |
| Visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshots/references, premium/modern/cohesive/anti-AI-slop requests | `levyra-design-taste` plus the matching Android/Desktop UI skill |
| Decorative motion artwork | `levyra-motion-artwork` |
| Windows Desktop, Compose Multiplatform, libvlc, downloads, mini player, deep links, updates, packaging | `levyra-desktop` |
| Secrets, URLs, redirects, SSRF, MIME, permissions, privacy, update integrity, or other security-sensitive work | `levyra-security-review` |
| GitHub Actions, CI, F-Droid, configuration sync, artifacts, build/release automation | `levyra-ci-workflows` |
| Builds, tests, lint, logs, broad searches, dependency reports, Git/GitHub, CI diagnostics, or other noisy command output | `levyra-context-efficiency` |
| Branch, commit, patch, or pull request review | `levyra-pr-review` |
| Pre-merge or pre-release validation, versions, signing, checksums, packaging | `levyra-release-check` |
| Genuine cross-domain work or initial architecture orientation | `levyra-engineering` |

Several skills may apply. A playback change that modifies stream resolution
uses player and extractor skills; provider-controlled media normally also
requires security review. A tracked multi-phase task additionally uses
`levyra-project-manager`; non-trivial ambiguous/multi-step work additionally
uses `levyra-real-engineering`; visual Android work uses `levyra-design-taste`
plus `levyra-compose`; visual Desktop work uses `levyra-design-taste` plus
`levyra-desktop`; Android runtime-performance work uses
`levyra-android-performance` plus the affected domain skill; R8/Proguard work
uses `levyra-r8-proguard` plus release validation and build-tooling guidance
when applicable; Android component-boundary security uses
`levyra-android-intent-security` plus `levyra-security-review` and the affected
domain skill; OpenClaw coordination additionally uses
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
- `docs/ai/AI_ENGINEERING_GUARDRAILS.md` defines the anti-overengineering,
  assumption/tradeoff, goal-verification, surgical-edit, and complexity rules
  shared by all coding runtimes.
- `docs/ai/MATT_POCOCK_SKILLS.md` defines the real-engineering stage routing and
  runtime-specific upstream skill installation.
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
4. State material assumptions and unresolved tradeoffs; inspect the repository
   first when evidence can resolve them.
5. Identify a simpler existing-owner/reuse path before adding abstraction or
   configurability.
6. Inspect the complete current control/data flow and nearby tests.
7. Identify the root cause before editing.
8. Define the verification target for each non-trivial step.
9. Make the smallest coherent change compatible with current architecture.
10. Avoid unrelated cleanup, formatting churn, dependency upgrades, renames,
    and broad refactors.
11. Add or update regression tests for defects, migrations, matching, security
    boundaries, lifecycle, and concurrency when applicable.
12. Run focused checks first, then applicable broader checks.
13. Inspect the complete final diff for unrelated edits, generated files,
    secrets, binaries, conflict markers, and accidental version changes.
14. Synchronize `docs/project/SPEC.md`, `docs/project/ROADMAP.md`,
    `docs/project/TASKS.md`, architecture, and user documentation when the
    approved requirement or architecture changes.
15. Report exactly what changed, what ran, what passed, what failed, and what
    remains unverified.

When the owner says "only this", modify only the named behavior or files unless
an additional change is strictly required for correctness. State that dependency
before expanding scope.

## Mandatory AI quality gate

Every coding runtime working on Levyra, including Codex, ChatGPT, Claude Code,
Google Antigravity, OpenCode, and OpenClaw-delegated runtimes, must use the same
repository gate:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

- Run `fast` before creating a commit.
- Run `full` before push or pull-request publication. It selects Android,
  Desktop, extractor, Bash, PowerShell, and repository checks from the complete
  diff against the base branch.
- ChatGPT or another runtime without command execution must include the exact
  gate commands in its implementation handoff and must not claim validation;
  the implementing runtime must run them.
- `.github/workflows/pr-check.yml` runs the fast gate again before build jobs.
- Missing tools, failed checks, unresolved findings, and skipped required
  evidence are blocked, not passed.
- CodeRabbit and other reviewers are supplementary. Convert every valid review
  finding into a deterministic test or validator whenever practical.

Do not bypass the gate with `--no-verify`, remove checks to obtain a green run,
or treat an AI-authored review as independent proof. Publication authorization
remains separate from validation.

## Validation

Use repository wrappers, never a system Gradle installation.

Agent configuration checks from the repository root:

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_matt_skills.py
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
- Treat Android Intent/deep-link/PendingIntent/component boundaries as untrusted
  input paths and load `levyra-android-intent-security` plus
  `levyra-security-review` before changing or auditing them.
- Preserve least privilege in Android permissions, GitHub workflow permissions,
  coding-agent tools, and OpenClaw agent allowlists.
- Do not weaken transport, redirect, MIME, checksum, signature, host, component,
  caller, or URI-grant validation to make one response pass.
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
- material assumptions/tradeoffs and simpler alternatives considered when
  relevant;
- tests and checks run with results;
- skipped or blocked checks and why;
- remaining risks and manual validation;
- professional commit message when requested;
- verified branch, commit, PR, merge, or release state when applicable.

Never represent a plan as an applied patch or an unverified result as completed.
Distinguish planned, edited, locally validated, committed, pushed, pull request
opened, CI passed, reviewed, merged, and released.
