# Levyra Always-On Agent Guards

These guards are not skills. They apply to every Levyra engineering task across Claude Code, Codex, ChatGPT, Antigravity, OpenCode, OpenClaw-delegated runtimes, and compatible agents. Runtime hooks may enforce them mechanically; repository validators remain the backstop.

## Scoped instructions

Apply root `AGENTS.md`, the nearest scoped `AGENTS.md`, current repository evidence, and only the skill bodies routed for the current phase. Never preload the skill tree. The owner never needs to name a skill. `scripts/agent_skill_router.py` is the deterministic Claude/Codex reference and enforces the active-skill budget.

## Current file before mutation

Ground an existing target in current repository content before editing it. Whole-file replacement needs a fresh full read when the runtime tracks reads; patch edits may use the smallest current region containing the anchors. Do not repeatedly reread an unchanged file whose current hash is already grounded. Never inject or mutate secrets, `.env`, `local.properties`, keystores, signing material, cookies, or private access values.

## Acceptance gates are always active

Code, build/configuration, migration, performance, security, CI, and agent-infrastructure changes use `docs/ai/EVIDENCE_GATED_COMPLETION.md`. Only direct evidence is `PASS`; `FAIL`, `BLOCKED`, and `UNRUN` stay open. After the final material edit, run focused validation, inspect the actual final diff, run `git diff --check` or equivalent, and perform the required code review. A missing tool/device/SDK may be `BLOCKED`; it is never a pass and must not cause an infinite retry loop.

## Critical regression locks

These are preserved product invariants, not suggestions. Any agent touching the affected code must verify that the invariant still holds and treat a regression as a blocker.

### Library catalog ANR lock

Regression provenance: issue #794 and PR #807.

- Do not reintroduce an `unmatched downloads × known tracks` scan in `buildLibraryCatalog` or an equivalent Library hot path on the main thread.
- Keep offline-download matching indexed by normalized identity such as `(title, artist)`, or use an equivalent approach whose cost does not grow quadratically with downloads and known tracks.
- Do not move regex/string normalization back into repeated per-download/per-track scans during composition or other UI-thread work.
- Preserve the matching semantics established by PR #807: direct id matches first, metadata fallback keeps original encounter order, album and duration checks still filter candidates, and the first valid candidate remains the selected one.
- Changes to Library catalog matching must run the focused Library catalog regression tests and must not claim performance safety without direct evidence when the hot path changes.

### Transient playback recovery lock

Regression provenance: PR #811.

- A transient stream/network interruption must not leave Levyra paused when the user intent is still to play.
- Do not clear `playWhenReady`, call `pause()`, or route a still-recoverable transient network failure through a terminal failure path merely because recovery is in progress.
- Preserve bounded retry/backoff and resume-in-place behavior where the existing recovery path supports it; permanent/content failures such as expired or invalid sources must still follow the resolver/failure path instead of being retried forever.
- A real user pause, route/noisy-device pause, or other intentional pause must always win over automatic recovery. Recovery must never force playback back on after that intent changed.
- Keep recovery ownership coordinated between player/UI and service code so they do not race by pausing and replaying over each other.
- Changes to `LevyraYoutubeDataSource`, `PlaybackCacheRecovery`, `LevyraPlaybackLoadErrorHandlingPolicy`, `LevyraPlayer.onPlayerError`, `recoverPlaybackStream`, `PlaybackService.attemptServiceRecovery`, or adaptive buffering/recovery policy must run the focused playback recovery tests and explicitly verify both cases: transient outage resumes automatically, intentional pause stays paused.

## Retry and task continuity

Keep only a compact local checkpoint for an active task: goal, changed paths/edit generation, validation/review state, blocker, and next action. Do not create tracked progress files just to satisfy the agent. An identical shell command may be retried once after its first failure; after two identical failures in the same edit generation, change the hypothesis, input, environment, or implementation before retrying again.

## Compaction and resume must re-anchor state

After compaction, resume, or a deliberate fresh session, restore only the latest owner outcome, hard scope boundaries, changed paths, verified rationale/root cause, acceptance state, current validation/review evidence, next action, and publication state. Current repository evidence always outranks summaries or memory. Use a fresh session at a natural boundary when old exploration no longer helps; do not abandon an active task just because context is large.

## Tool and context discipline

Use the smallest useful search/read and expand only for a concrete unanswered question. For structural ownership/reference work prefer project jCodeMunch, then an already-available LSP/AST tool, then bounded native search/read. RTK may compact routine terminal output, but rerun raw whenever exact failures, stack traces, security/signing evidence, Perfetto/R8 evidence, or ambiguous output matter. Install only a specific missing task-required tool from a trusted source; never perform broad upgrades, privilege escalation, sandbox weakening, or unrelated plugin installation.

## AI-comment slop is rejected

Do not add source comments that narrate the agent process, numbered implementation steps, obvious code, or tutorial prose. Preserve legally/mechanically required directives and genuinely non-obvious compatibility or safety contracts. The repository checker evaluates added source lines; it does not require unrelated comment cleanup.

## Structural navigation is deterministic

For symbol, call-flow, reference, ownership, or rename questions use the narrowest structural evidence available before broad text search. Structural tooling reduces discovery noise; it never replaces reading the current source, running tests, or examining exact diagnostics.

## Security and publication

Do not weaken security, validation, transport, signature/checksum, Android component, caller, URI-grant, or publication controls to make a task pass. Android artifact decompilation or compiled API analysis routes to `levyra-android-reverse-engineering`; pair security/R8 skills only when that phase actually requires them. Dynamic instrumentation is not an automatic continuation and must stay inside owner-authorized targets.

Commit, push, PR creation, merge, tag, release, deployment, version changes, external messages, and repository settings remain owner-controlled. Keep delivery states distinct: `planned -> edited -> locally validated -> final diff reviewed -> committed -> pushed -> pull request opened -> CI passed -> independently reviewed -> merged -> released`.
