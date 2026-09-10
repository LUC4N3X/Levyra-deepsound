# Levyra Engineering Instructions

## Purpose

This is Levyra's compact always-loaded engineering contract. Keep permanent
rules here and load detailed procedures only for the current task phase.

Instruction order:
1. root `AGENTS.md`;
2. nearest path-specific `AGENTS.md`;
3. approved `docs/project/` requirements when relevant;
4. at most the routed skill bodies needed for the current phase;
5. current code, tests, build files, workflows, and runtime evidence;
6. runtime-specific configuration under `.agents/claude/` or `.agents/codex/`.

Current repository evidence outranks memory, old comments, previous agent output,
and stale task status. Apply `docs/ai/ALWAYS_ON_AGENT_GUARDS.md` to every
engineering task. Use `docs/ai/AI_ENGINEERING_GUARDRAILS.md` when its detailed
production-code procedure is needed and `docs/ai/EVIDENCE_GATED_COMPLETION.md`
for non-trivial completion evidence.

## Execution contract

- Explicit owner execution cues such as `VAI`, `PROCEDI`, `INTERVIENI`, `FAI TU`
  are core behavior, not a reason to load `levyra-mode`.
- `only this`, `solo questo`, and equivalents are hard scope boundaries.
- Inspect the current implementation and nearby tests before changing behavior.
- Fix the root cause with the smallest coherent change and reuse existing owners.
- Do not perform unrelated refactors, dependency churn, version changes, renames,
  cleanup, or architecture work.
- Do not add explanatory source-code comments. Prefer clear names and structure.
- Investigation does not authorize implementation; implementation does not
  authorize publication.
- Ask only when an indispensable input is missing or an action is destructive,
  irreversible, security-sensitive, or outside existing authorization.

## Always-on context budget

Before broad reading:
1. identify the likely owner/module and the exact question the next read answers;
2. search path, symbol, filename, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already in context;
6. keep a maximum of two skill bodies active for the current phase.

The compact rules above replace automatic loading of `levyra-context-efficiency`.
This two-skill phase cap overrides older scoped, skill, or playbook wording that says to load every matching skill; defer additional skills until their phase begins.
Use that skill only when its extended RTK/log/context procedure is specifically
useful. Token savings come from less redundant context and output, never from
shallower reasoning, validation, or review.

## Repository map

- `app/`: Android client; apply `app/AGENTS.md`.
- `desktop/`: Windows client; apply `desktop/AGENTS.md`.
- `.github/`: CI/release automation; apply `.github/AGENTS.md`.
- `docs/`: documentation; apply `docs/AGENTS.md`.
- `docs/project/`: durable requirements, roadmap, and active tasks.
- `.agents/skills/`: the single canonical Levyra skill tree.
- `.agents/claude/`: canonical Claude-specific runtime configuration.
- `.agents/codex/`: canonical Codex project configuration.

Root `CLAUDE.md` is only a small Claude-native bridge importing this file.

## Automatic skill routing

`scripts/agent_skill_router.py` selects only the current phase. The owner never
needs to name a skill. It must route no more than two skill bodies at once.

Implementation/debugging loads the narrowest domain skill first. Ordinary bugs
do not automatically load the large `levyra-real-engineering` workflow; reserve
it for explicit architecture, cross-domain, multi-step, concurrency, root-cause,
specification, or similarly complex work. If implementation and PR/release work
are requested together, finish the implementation phase first and defer review,
release, and prose skills until their phase.

Core compatibility inventory:
`levyra-real-engineering`, `levyra-compose`, `levyra-design-taste`,
`levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, `levyra-release-check`,
`levyra-player`, `levyra-extractor`, `levyra-database`, `levyra-desktop`,
`levyra-security-review`, `levyra-project-manager`, `levyra-engineering`,
`levyra-openclaw-orchestrator`, `levyra-motion-artwork`,
`levyra-android-reverse-engineering`, `levyra-codex-bootstrap`,
`levyra-humanizer`, `levyra-mode`.

Phase examples:
- playback/Media3/queue -> `levyra-player`;
- extraction/InnerTube/network fallback -> `levyra-extractor`;
- Room/persistence -> `levyra-database`;
- Compose/accessibility/state -> `levyra-compose`;
- visual polish -> `levyra-design-taste` plus the matching UI skill when needed;
- Android performance/memory/jank -> `levyra-android-performance`;
- R8/Proguard -> `levyra-r8-proguard`;
- Intent/component security -> `levyra-android-intent-security` plus
  `levyra-security-review`;
- Windows Desktop -> `levyra-desktop`;
- CI/Gradle/Kotlin tooling -> `levyra-ci-workflows`;
- Claude/Codex agent tooling -> `levyra-codex-bootstrap`;
- final branch/PR review -> `levyra-pr-review`;
- release/runtime validation -> `levyra-release-check`;
- PR description -> `levyra-humanizer`;
- requirements/roadmap -> `levyra-project-manager`;
- cross-domain orientation -> `levyra-engineering`;
- OpenClaw coordination -> `levyra-openclaw-orchestrator`;
- Android binary analysis -> `levyra-android-reverse-engineering`.

Do not preload skill bodies. Read only routed
`.agents/skills/<name>/SKILL.md` files for the active phase. External workflows
in `docs/ai/MATT_POCOCK_SKILLS.md` are supplementary and opt-in; repository
rules remain authoritative.

## Product invariants

Protect playback reliability, responsiveness, privacy, user data, downloads,
favorites, playlists, queues, lyrics, history, settings, localization,
onboarding, sessions, and backups unless explicitly changed. Keep audio/song and
native-video modes distinct. Motion artwork is decorative and must never delay
audible playback. Keep playback, MediaSession, notification, Android Auto,
queue, and background service synchronized. Do not add account login, cookies,
private tokens, telemetry, or tracking unless explicitly requested. Android and
Desktop versions/releases remain independent.

## Work method

Use `Plan -> Execute -> Verify` for non-trivial implementation:
1. define outcome, scope, preserved behavior, and acceptance;
2. route only the current phase skills and inspect the current flow;
3. identify the root cause and smallest existing-owner solution;
4. make one coherent change without unrelated churn;
5. run the narrowest useful checks after the latest material edit;
6. inspect the complete final diff and run `git diff --check`;
7. report exactly what changed, passed, failed, blocked, or remains unverified.

Do not retry the same materially unchanged approach more than twice. After the
second failure, switch to a materially different diagnosis or report the blocker.
When stale context no longer helps, suggest a fresh session at the next natural
task boundary and provide a compact verified handoff. Never abandon an active
deliverable merely because context is large.

Keep blocking network, database, disk, parsing, decoding, extraction, and native
media work off UI threads. Preserve lifecycle, cancellation, identity/generation,
retry, timeout, concurrency, and bounded-storage semantics.

## RTK agent bootstrap

For shell-capable non-trivial work, the pinned `rtk-ai/rtk` bootstrap remains
owner-authorized. Verify `rtk --version` and `rtk gain`; use
`scripts/ensure-rtk.ps1 -Quiet` or `./scripts/ensure-rtk.sh --quiet` when needed.
Manual repair remains `scripts/setup-ai.ps1 -InstallRtk` or
`./scripts/setup-ai.sh --install-rtk`. If RTK is unavailable, continue raw.

Optional persistent memory and external skill integrations must never block
ordinary work or silently enlarge every session.

## Mandatory AI quality gate

Use repository wrappers, never a system Gradle installation. Before commit run:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before push or PR publication run:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Missing SDK/JDK/signing/device/libvlc/WiX/network/OS prerequisites are `BLOCKED`,
never `PASS`. ChatGPT or another runtime without command execution must not claim
checks ran. CodeRabbit and other reviewers are supplementary evidence only.

## Security and publication

Never expose or commit secrets, tokens, cookies, private URLs, keystores,
signing material, `.env`, or `local.properties`. Never weaken security checks to
make a case pass. Commit, push, PR creation, merge, tag, release, deployment,
version changes, external messages, and repository-setting changes require
explicit owner authorization for that action and scope.

When PR publication is authorized, use a dedicated branch and draft PR by
default, preserve the complete `.github/pull_request_template.md`, keep
validation claims truthful, and apply `levyra-humanizer` only in the PR-writing
phase without changing facts.

## Delivery contract

Keep these states distinct:
`planned -> edited -> locally validated -> final diff reviewed -> committed -> pushed -> pull request opened -> CI passed -> independently reviewed -> merged -> released`.

Report rationale/root cause, exact files changed, validation run, blocked/unrun
checks, remaining risk, and verified publication state.
