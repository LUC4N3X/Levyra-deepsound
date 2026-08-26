# Levyra Engineering Instructions

## Purpose and hierarchy

This file is the repository-wide operating contract for coding agents. Claude
Code, Codex, ChatGPT when operating through a repository-capable runtime,
Google Antigravity, OpenCode, OpenClaw, and compatible runtimes should read it
from the Git root, then apply any nearer `AGENTS.md` for the files in scope.

Instruction order:

1. root `AGENTS.md`;
2. nearer path-specific `AGENTS.md` files;
3. approved requirements and active planning in `docs/project/SPEC.md`,
   `docs/project/ROADMAP.md`, and `docs/project/TASKS.md`;
4. matching native skills under `.agents/skills/`;
5. current architecture, implementation, tests, build files, and workflows;
6. runtime-specific canonical configuration under `.agents/claude/` and
   `.agents/codex/` when that runtime is in use.

Current repository evidence always overrides remembered behavior, old
discussions, stale comments, previous agent output, or stale task status.
Surface conflicts between planning files and implementation before editing.

For every engineering task apply `docs/ai/ALWAYS_ON_AGENT_GUARDS.md`. For
production-code implementation or broad review, also read
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
- `.agents/skills/`: the single canonical Levyra skill tree shared by supported
  coding runtimes.
- `.agents/claude/`: tracked Claude Code instructions, settings, agents, hooks,
  and rules.
- `.agents/codex/`: tracked Codex project configuration and lifecycle hooks.
- `.claude/` and `.codex/`: ignored native runtime projections generated
  locally from `.agents/`; never treat them as tracked sources of truth.

## Agent runtime discovery

- Root `AGENTS.md` is the canonical repository context for supported coding
  agents.
- Google Antigravity discovers workspace skills from
  `.agents/skills/<skill-name>/SKILL.md` when a conversation starts.
- Codex discovers the canonical `.agents/skills/` tree directly. Its projected
  `.codex/` surface supplies project-specific configuration and lifecycle hooks
  after the native projection is materialized.
- Claude Code consumes the native `.claude/` surface. Levyra generates that
  ignored projection from `.agents/claude/` and projects `.agents/skills/`
  directly to `.claude/skills/`; `scripts/setup-ai.*` is the deterministic
  bootstrap and the project jCodeMunch launcher provides an additional
  best-effort clean-clone projection path.
- When the Claude/Codex native hook surface is active, user prompts execute
  `scripts/agent_skill_router.py` automatically. Other supported runtimes must
  infer the same routes from the current task and canonical table below without
  requiring the owner to name a skill; shell-capable runtimes may invoke the
  shared router directly.
- A native runtime may need a new session after its projection or skill inventory
  first appears so the runtime can rebuild discovery. Do not represent a file
  generated mid-session as proof that the already-running process loaded it.
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

## Execution-first operating mode

This policy is shared by every supported coding runtime. It is not a
Claude-specific preference.

- When the owner asks to `fix`, `update`, `address`, `implement`, `refactor`, or
  otherwise change code, execute the requested work directly inside the
  authorized scope. Do not turn an implementation request into an unsolicited
  design discussion, tutorial, list of alternatives, or approval pause.
- Do not ask routine confirmation questions when the requested outcome,
  repository evidence, and existing architecture already determine the next
  safe action.
- Ask only when an indispensable input is missing, materially different
  interpretations would produce meaningfully different results, or the next
  action is destructive, irreversible, security-sensitive, or outside the
  owner's existing authorization.
- If the requested approach conflicts with current repository evidence,
  security, data integrity, compatibility, product invariants, or deterministic
  validation, surface the conflict briefly and choose the smallest safe
  correction when it remains inside scope. Never silently execute a known-bad
  approach merely to appear compliant.
- For code-bearing work: inspect the current implementation and nearby tests, make the
  smallest coherent change, run focused validation, fix regressions introduced
  by the change, inspect the final diff, then report the result, files changed,
  checks run, and any real blocker.
- Treat `only this`, `solo questo`, and equivalent wording as a hard scope
  boundary. Do not perform opportunistic refactors, unrelated cleanup,
  dependency churn, version changes, or speculative architecture work.
- Prefer execution over commentary. Explain decisions only when they materially
  affect correctness, safety, compatibility, validation, or the owner's next
  action.
- Execution-first behavior never grants permission to commit, push, open or
  merge a pull request, tag, publish, release, deploy, change repository
  settings, bypass approvals, weaken sandboxing, or skip required validation.
  Those actions keep their existing owner-controlled authorization rules.

## Always-on context budget

Codex and every compatible runtime must apply this before broad repository
reading on every non-trivial task:

1. identify the likely owner/module and the exact question the next read must
   answer;
2. search path, filename, symbol, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already present in context;
6. load only matching skills, never the whole skill tree.

Use `levyra-context-efficiency` immediately when the task needs non-trivial
repository exploration, useful cross-session continuity, or high-volume output.
Tiny, already-local edits keep the same baseline without loading extra skill
text.

Keep security, Perfetto, R8, signing, exact failures, and decisive diagnostics
raw whenever compacting them could change the conclusion. Token savings never
justify missing evidence.

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

The repository owner gives standing authorization to supported shell-capable
coding agents to bootstrap the pinned official `rtk-ai/rtk` command-output tool
when working in this repository. The bootstrap is automatic and idempotent.

At the start of the first non-trivial shell-capable task in a session, without
asking the owner to run setup manually:

1. on Windows PowerShell run `scripts/ensure-rtk.ps1 -Quiet`;
2. on Bash, WSL, Linux, or macOS run `./scripts/ensure-rtk.sh --quiet`;
3. let the ensure script verify raw `rtk --version` and `rtk gain` and install
   only the pinned RTK revision through Cargo when those checks fail;
4. if Cargo is unavailable, installation fails, or verification still fails,
   report the blocked bootstrap once and continue with raw commands instead of
   downloading an unverified executable, weakening sandboxing, or weakening
   validation.

The broader manual setup remains available through
`scripts/setup-ai.ps1 -InstallRtk` or `./scripts/setup-ai.sh --install-rtk` for
repair or explicit environment setup. Do not reinstall RTK repeatedly once the
raw readiness checks pass.

## Persistent agent memory bootstrap

The repository owner also gives standing authorization to the pinned
`thedotmack/claude-mem` integration defined in `docs/ai/CLAUDE_MEM.md` for
Claude Code, Codex CLI, and Google Antigravity.

Use it automatically only when prior-session context can materially reduce
repeated investigation or restore continuity. When the runtime already exposes
claude-mem memory tools, query them directly with progressive disclosure. When
a local shell-capable supported runtime needs that context but the memory tools
are absent, it may run one automatic pinned setup attempt through
`scripts/setup-claude-mem.ps1` on Windows or `./scripts/setup-claude-mem.sh` on
Bash/WSL/Linux/macOS, then retry the focused lookup.

If setup, worker health, or MCP discovery fails, continue without memory. Never
block implementation, review, tests, or answers waiting for memory; never enable
cloud sync or experimental semantic injection implicitly; never store secrets,
keystores, `.env`, cookies, tokens, signing material, private URLs, or
`local.properties` as project memory. Current repository evidence always
outranks stored observations.

ChatGPT uses claude-mem only when a compatible MCP app is actually connected and
the memory tools are available. Repository configuration alone must never be
represented as a successful ChatGPT-to-local-worker connection.

This standing authorization applies to the pinned `rtk-ai/rtk` bootstrap, the
pinned claude-mem integration, the focused Matt Pocock skill bootstrap documented
below, and task-required tools installed under
`docs/ai/ALWAYS_ON_AGENT_GUARDS.md`. A shell-capable runtime may install a missing
useful tool automatically when the active task genuinely requires it, using a
trusted upstream and the narrowest practical user/project-local installation,
then verifying the installed version. Broad package/system upgrades, unrelated
plugins or daemons, telemetry, administrator/root elevation, unrestricted
sandboxing, approval bypasses, commit, push, pull request, merge, tag, release,
deployment, external messages, and repository settings still require their
normal explicit authorization. Keep security, signing, checksum, secret, and
exact reproduction evidence raw.

## Matt Pocock skills bootstrap

The repository owner authorizes the exact `mattpocock/skills` engineering
integration defined by this repository:

- Codex: `scripts/setup-ai.ps1` / `scripts/setup-ai.sh` may use the official
  `skills` CLI through `npx` to install only the focused allowlist declared in
  those scripts, globally for the Codex agent. Use `-SkipMattSkills` or
  `--skip-matt-skills` to opt out on a machine.
- Claude Code: `.agents/claude/settings.json`, projected locally to native
  `.claude/settings.json`, enables `mattpocock-skills@claude-plugins-official`;
  Claude's normal project trust and plugin installation controls remain intact.
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

Select every matching skill automatically from the task before reading widely or
editing. The owner never needs to name a skill. Claude Code and Codex use the
shared prompt router when their native hook surface is active; other runtimes
must apply this table directly. Prefer focused skills over the general
coordinator and never preload the whole skill tree.

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
| APK/XAPK/AAB/DEX/JAR/AAR decompilation, jadx/smali, compiled Android API extraction, binary call-flow tracing, or Kotlin/R8 metadata recovery | `levyra-android-reverse-engineering` plus `levyra-security-review`; add `levyra-r8-proguard` when obfuscation/shrinker behavior is material |
| R8, Proguard, minification, resource shrinking, keep/consumer rules, release-only shrinker crashes, mapping/missing classes, reflection/serialization/JNI shrinker issues, or measured APK-size work | `levyra-r8-proguard` plus `levyra-release-check`; add `levyra-ci-workflows` for build-tooling changes |
| Android Intent, deep link, PendingIntent, exported activity/service/receiver/provider, nested Intent, `onNewIntent`, URI grant, FileProvider/ContentProvider, or caller/signature verification | `levyra-android-intent-security` plus `levyra-security-review` and the affected Android domain skill |
| Visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshots/references, premium/modern/cohesive/anti-AI-slop requests | `levyra-design-taste` plus the matching Android/Desktop UI skill |
| Decorative motion artwork | `levyra-motion-artwork` |
| Windows Desktop, Compose Multiplatform, libvlc, downloads, mini player, deep links, updates, packaging | `levyra-desktop` |
| Secrets, URLs, redirects, SSRF, MIME, permissions, privacy, update integrity, or other security-sensitive work | `levyra-security-review` |
| GitHub Actions, CI, F-Droid, configuration sync, artifacts, build/release automation | `levyra-ci-workflows` |
| Non-trivial repository exploration; builds, tests, lint, logs, broad searches, dependency reports, Git/GitHub, CI diagnostics, or other noisy context | `levyra-context-efficiency` |
| Branch, commit, patch, or pull request review | `levyra-pr-review` |
| Pre-merge or pre-release validation, versions, signing, checksums, packaging | `levyra-release-check` |
| Genuine cross-domain work or initial architecture orientation | `levyra-engineering` |

Several skills may apply. A playback change that modifies stream resolution
uses player and extractor skills; provider-controlled media normally also
requires security review. A tracked multi-phase task additionally uses
`levyra-project-manager`; non-trivial ambiguous/multi-step work additionally
uses `levyra-real-engineering`; non-trivial repository exploration additionally
uses `levyra-context-efficiency`; visual Android work uses `levyra-design-taste`
plus `levyra-compose`; visual Desktop work uses `levyra-design-taste` plus
`levyra-desktop`; Android runtime-performance work uses
`levyra-android-performance` plus the affected domain skill; Android artifact
reverse-engineering uses `levyra-android-reverse-engineering` plus security
review and R8 guidance when obfuscation is material; R8/Proguard work uses
`levyra-r8-proguard` plus release validation and build-tooling guidance when
applicable; Android component-boundary security uses
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
- `docs/ai/ALWAYS_ON_AGENT_GUARDS.md` defines non-optional execution, automatic
  skill routing, checkpoint/retry discipline, narrow tool installation, and
  context-hygiene rules shared by all coding runtimes.
- `docs/ai/AI_ENGINEERING_GUARDRAILS.md` defines the anti-overengineering,
  assumption/tradeoff, goal-verification, surgical-edit, and complexity rules
  shared by all coding runtimes.
- `docs/ai/CLAUDE_MEM.md` defines the optional persistent-memory integration,
  privacy boundary, automatic-on-need bootstrap, and fail-open behavior.
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
- Treat user-provided screenshots and direct runtime observations as acceptance
  evidence. Reconcile visible failures even when automated checks are green.
- Match the requested action mode. `inspect`, `review`, `diagnose`, and `report`
  authorize investigation and reporting, not implementation. `fix`, `update`,
  `address`, and `implement` authorize the requested change and relevant
  validation, but never imply publication, merge, release, deployment, or an
  external message.
- Do not add explanatory source-code comments. Prefer clear names, small
  functions, and explicit structure. Preserve comments that are legally or
  mechanically required, including license headers, generated/tool directives,
  lint/suppression markers, and compatibility comments whose removal would
  change or obscure a required contract.

## Work method

For non-trivial implementation use `Plan -> Execute -> Verify`: make the smallest
evidence-based plan, implement one coherent slice, verify its success criterion,
and only then expand. Do not insert a ceremonial approval pause unless the owner
explicitly reserved one.

1. Define the exact requested outcome, action mode, and scope.
2. At the start of a non-trivial shell-capable task, ensure the pinned RTK
   bootstrap automatically; continue raw if it is unavailable.
3. Route and load every matching native skill automatically from the task, then
   apply the always-on context budget before broad reading; load
   `levyra-context-efficiency` when exploration or cross-session continuity is
   non-trivial.
4. When prior-session context materially matters, use focused claude-mem
   retrieval if available or the one-attempt owner-authorized bootstrap if it is
   not; verify every remembered conclusion against the current repository.
5. Read `docs/project/SPEC.md`, the relevant roadmap track, and the active
   `docs/project/TASKS.md` phase when applicable.
6. Identify behavior and compatibility that must remain unchanged.
7. State material assumptions and unresolved tradeoffs; inspect the repository
   first when evidence can resolve them.
8. Identify a simpler existing-owner/reuse path before adding abstraction or
   configurability.
9. Inspect the complete current control/data flow and nearby tests.
10. Identify the root cause before editing.
11. Define the verification target for each non-trivial step.
12. Make the smallest coherent change compatible with current architecture.
13. Avoid unrelated cleanup, formatting churn, dependency upgrades, renames,
    and broad refactors.
14. Add or update regression tests for defects, migrations, matching, security
    boundaries, lifecycle, and concurrency when applicable.
15. Run focused checks first, then applicable broader checks.
16. Inspect the complete final diff for unrelated edits, generated files,
    secrets, binaries, conflict markers, and accidental version changes.
17. Synchronize `docs/project/SPEC.md`, `docs/project/ROADMAP.md`,
    `docs/project/TASKS.md`, architecture, and user documentation when the
    approved requirement or architecture changes.
18. Report exactly what changed, what ran, what passed, what failed, and what
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
python3 scripts/validate_claude_mem.py
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
