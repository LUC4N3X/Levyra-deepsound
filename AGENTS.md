# Levyra Engineering Instructions

## Purpose
This is Levyra's compact, always-loaded engineering contract. It is a router,
not a handbook: keep permanent rules here and load detailed procedures only
when the active task needs them.

Instruction order:
1. root `AGENTS.md`;
2. the nearest path-specific `AGENTS.md` for files in scope;
3. applicable approved planning in `docs/project/`;
4. matching native skills under `.agents/skills/`;
5. current implementation, tests, build files, workflows, and runtime evidence;
6. runtime-specific canonical configuration under `.agents/claude/` or `.agents/codex/`.

Current repository evidence outranks memory, stale comments, previous agent
output, and old task status. Surface real conflicts before editing.
For every engineering task apply `docs/ai/ALWAYS_ON_AGENT_GUARDS.md`. For
production-code implementation or broad review also apply
`docs/ai/AI_ENGINEERING_GUARDRAILS.md`. Use
`docs/ai/EVIDENCE_GATED_COMPLETION.md` for non-trivial completion evidence.

## Execution contract
- When the owner asks to fix, update, implement, refactor, or otherwise change
  code, execute the requested work directly inside the authorized scope.
- `only this`, `solo questo`, and equivalents are hard scope boundaries.
- Inspect the current implementation and nearby tests before changing behavior.
- Fix the root cause with the smallest coherent change. Reuse existing owners,
  clients, players, caches, stores, state models, dispatchers, and policies.
- Do not perform opportunistic refactors, dependency churn, version changes,
  renames, cleanup, or architecture work unrelated to the request.
- Do not add explanatory source-code comments. Prefer clear names and structure;
  preserve only legally or mechanically required comments and genuinely
  non-obvious compatibility/safety contracts.
- Ask only when an indispensable input is missing, materially different valid
  interpretations would change the result, or an action is destructive,
  irreversible, security-sensitive, or outside existing authorization.
- Investigation verbs such as inspect, review, diagnose, and report do not
  authorize implementation. Implementation verbs do not authorize publication.

## Always-on context budget
Before broad repository reading on every non-trivial task:
1. identify the likely owner/module and the exact question the next read answers;
2. search path, filename, symbol, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already present in context;
6. load only matching skills, never the whole skill tree.

Use `levyra-context-efficiency` for noisy builds, tests, lint, logs, dependency
reports, Git/GitHub output, broad searches, or non-trivial repository
exploration. RTK is an optimization layer only; rerun raw whenever compressed
output could hide decisive diagnostics, security, signing, Perfetto, or R8 evidence.

## Repository map
- `app/`: Android client; apply `app/AGENTS.md`.
- `desktop/`: Windows client; apply `desktop/AGENTS.md`.
- `.github/`: CI/release automation; apply `.github/AGENTS.md`.
- `docs/`: documentation; apply `docs/AGENTS.md`.
- `docs/project/`: `SPEC.md`, `ROADMAP.md`, and the active `TASKS.md` phase.
- `docs/ai/`: detailed cross-runtime engineering procedures.
- `.agents/skills/`: the single canonical Levyra skill tree.
- `.agents/claude/`: canonical Claude-specific settings, hooks, agents, and rules.
- `.agents/codex/`: canonical Codex-specific configuration and hooks.
- `.claude/` and `.codex/`: generated local runtime projections; never sources of truth.

Claude Code has a tracked root `CLAUDE.md` whose sole purpose is reliable native
startup discovery and import of this file. Do not duplicate this contract there.

## Automatic skill routing
Select matching skills automatically from the task. The owner never needs to
name a skill. Claude/Codex hooks use `scripts/agent_skill_router.py`; compatible
runtimes should follow the same routing behavior. Several skills may apply.

Core automatic routes:
- substantial or ambiguous engineering -> `levyra-real-engineering`;
- Android playback/Media3/queue -> `levyra-player`;
- extraction/InnerTube/network fallback -> `levyra-extractor`;
- Room/persistence/backup -> `levyra-database`;
- Compose/state/navigation/accessibility -> `levyra-compose`;
- visual redesign/polish -> `levyra-design-taste` plus the matching UI skill;
- Android performance/memory/jank -> `levyra-android-performance`;
- R8/Proguard/shrinking -> `levyra-r8-proguard` plus `levyra-release-check`;
- Intent/deep-link/component security -> `levyra-android-intent-security` plus `levyra-security-review`;
- Windows Desktop -> `levyra-desktop`;
- CI/workflows/build tooling -> `levyra-ci-workflows`;
- branch/commit/PR review -> `levyra-pr-review`;
- PR creation or description -> `levyra-pr-review` plus `levyra-humanizer`;
- release/pre-merge evidence -> `levyra-release-check`;
- requirements/roadmap/task status -> `levyra-project-manager`;
- genuine cross-domain orientation -> `levyra-engineering`;
- OpenClaw coordination -> `levyra-openclaw-orchestrator`;
- Android artifact decompilation -> `levyra-android-reverse-engineering` plus `levyra-security-review`;
- secrets/privacy/trust boundaries -> `levyra-security-review`;
- decorative Canvas/motion artwork -> `levyra-motion-artwork`.

Do not preload skill bodies. Read the matching `.agents/skills/<name>/SKILL.md` only when routed.

## Product invariants
- Protect playback reliability, responsiveness, privacy, user data, and existing choices before optional polish.
- Keep explicit song/audio mode and native-video mode distinct; never silently remove, merge, hide, or override the user's choice.
- Motion artwork is decorative, muted, song/audio-only, and must never delay audible playback; static artwork remains the immediate fallback.
- Keep audible playback, MediaSession, notification, Android Auto, queue, and background service synchronized.
- Direct playback is the critical path; artwork, lyrics, diagnostics, prefetch, refresh, and enrichment must yield to it.
- Preserve downloads, favorites, playlists, queues, lyrics, history, settings, localization, onboarding, sessions, and backups unless explicitly changed.
- Do not add account login, cookies, private tokens, telemetry, or tracking unless explicitly requested.
- Android and Desktop versions, packages, artifacts, tags, and releases remain independent.

## Work method
Use `Plan -> Execute -> Verify` for non-trivial implementation:
1. define exact outcome, action mode, scope, preserved behavior, and acceptance;
2. route only matching skills and inspect the current control/data flow;
3. identify root cause and the simplest existing-owner solution;
4. make one minimal coherent change without unrelated churn;
5. run the narrowest useful checks after the latest material edit;
6. inspect the complete final diff and run `git diff --check`;
7. report exactly what changed, what passed, what failed, and what is unverified.

Keep blocking network, database, disk, parsing, decoding, extraction, and native
media work off UI threads. Preserve lifecycle, cancellation, shared-work,
generation/identity, failure classification, retry, timeout, concurrency, and
bounded-storage semantics. Do not negative-cache inconclusive failures.

## RTK agent bootstrap
For shell-capable non-trivial tasks, the pinned `rtk-ai/rtk` bootstrap is
owner-authorized. Verify `rtk --version` and `rtk gain`; use
`scripts/ensure-rtk.ps1 -Quiet` on Windows or `./scripts/ensure-rtk.sh --quiet`
elsewhere when needed. Manual repair remains available through
`scripts/setup-ai.ps1 -InstallRtk` or `./scripts/setup-ai.sh --install-rtk`.
If RTK is unavailable, continue raw rather than weakening validation or safety.
See `docs/ai/RTK.md` for details.

Optional persistent memory and external skill integrations are defined in
`docs/ai/CLAUDE_MEM.md` and `docs/ai/MATT_POCOCK_SKILLS.md`; use them only when
relevant and never let optional tooling block implementation.

## Mandatory AI quality gate
Use repository wrappers, never a system Gradle installation. Start with focused checks. Before commit run:
```bash
python3 scripts/ai_quality_gate.py --profile fast
```
Before push or PR publication run:
```bash
python3 scripts/ai_quality_gate.py --profile full
```
On Windows use the repository's `.bat` wrappers where applicable. Missing SDK,
JDK, signing input, device/emulator, libvlc, WiX, network, or OS support is
`BLOCKED`, never `PASS`. ChatGPT or another runtime without command execution
must not claim these checks ran. CodeRabbit and other reviewers are
supplementary evidence, not substitutes for deterministic validation.

## Security and publication
- Never expose or commit secrets, tokens, cookies, private URLs, keystores, signing material, `.env`, or `local.properties`.
- Never weaken transport, redirect, MIME, checksum, signature, host, Android component, caller, or URI-grant validation just to make one case pass.
- Do not commit generated APKs/installers/archives/build output unless explicitly required by repository policy and the task.
- Commit, push, PR creation, merge, tag, release, deployment, version changes, external messages, and repository-setting changes require explicit owner authorization for the exact action and scope.
- When PR publication is authorized, use a dedicated branch and draft PR by default, preserve `.github/pull_request_template.md`, keep checks truthful, and apply `levyra-humanizer` without changing facts.

## Delivery contract
Keep these states distinct:
`planned -> edited -> locally validated -> final diff reviewed -> committed -> pushed -> pull request opened -> CI passed -> independently reviewed -> merged -> released`
Report rationale/root cause, exact files changed, behavior preserved, validation
run, blocked/unrun checks, remaining risk, and verified publication state. Never
represent a plan as an applied patch or an unverified result as complete.
