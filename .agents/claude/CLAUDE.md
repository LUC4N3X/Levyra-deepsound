# Levyra Claude Code Runtime

Root `CLAUDE.md` is Claude Code's native startup bridge and imports root
`AGENTS.md`, the cross-runtime source of truth. This file adds only
Claude-specific runtime behavior. Do not duplicate the full repository contract
here and do not preload large `docs/ai/` playbooks wholesale.

The generated `.claude/` projection may load this file, hooks, rules, agents, and
skills after `scripts/sync_agent_runtime.py` runs. The tracked root bridge means
Claude still receives the essential Levyra contract even when that projection
has not been created yet.

## Core Claude contract

- Current repository evidence outranks memory, stale comments, prior agent
  output, and old task status.
- Root/scoped `AGENTS.md` instructions remain authoritative.
- For production implementation or broad review, consult
  `docs/ai/AI_ENGINEERING_GUARDRAILS.md` when its detailed decision procedure is
  needed.
- Apply `docs/ai/EVIDENCE_GATED_COMPLETION.md` to non-trivial completion.
- `only this`, `solo questo`, and equivalents are hard scope boundaries.
- Do not add explanatory source-code comments. Prefer clear names and structure.
- Commit, push, PR, merge, tag, release, deployment, version changes, and
  repository settings remain owner-controlled.

## Immediate context budget

Before broad reading:

1. identify the likely owner/module and exact question the next read answers;
2. search path, symbol, filename, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already in context;
6. load only the skills routed for this task.

Use `levyra-context-efficiency` for noisy builds, tests, lint, logs, broad
searches, dependency listings, Git/GitHub/CI output, or other high-volume
context. RTK is optional optimization; rerun raw when exact diagnostics,
security/signing, Perfetto, or R8 evidence matters.

## Deterministic skill loading

`UserPromptSubmit` runs the shared router. Every item reported under
**Mandatory skill load** must be loaded before broad repository reading, editing,
or shell work. The owner never needs to name a skill.

Compatibility inventory for automatic routes:
`levyra-real-engineering`, `levyra-compose`, `levyra-design-taste`,
`levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, `levyra-release-check`.
The shared router may additionally select `levyra-player`, `levyra-extractor`,
`levyra-database`, `levyra-desktop`, `levyra-security-review`,
`levyra-project-manager`, `levyra-engineering`, `levyra-humanizer`, and other
focused Levyra skills.

Do not scan or preload the whole skill tree. Invoke only the routed skill bodies.

## Subagent token discipline

Subagents already receive project context. Delegate with only the goal, current
verified evidence, affected files/symbols, task-specific invariants, acceptance
checks, and real blockers. Do not paste the parent transcript, entire files,
broad repository summaries, or unused skill bodies into subagent prompts.

Use built-in Explore for broad read-only discovery when appropriate. Use custom
implementation/review agents only when isolated context materially improves the
work; do not spawn them ceremonially for tiny edits.

## Hooks and resilience

Claude lifecycle hooks are a second enforcement layer, not the only source of
instructions:

- `SessionStart` refreshes optional runtime projection/tooling and re-anchors
  active state;
- `UserPromptSubmit` injects the compact Levyra hard-contract reminder and
  deterministic skill routing on every user turn;
- mutation hooks enforce scoped-instruction/current-file freshness where
  supported;
- compaction hooks re-anchor open task state;
- the Stop audit checks evidence before completion.

If optional RTK, jCodeMunch, memory, or projection setup fails, continue with
native tools and report the limitation once. Never weaken safety or validation
to make optional tooling work.

## Review, validation, and PRs

After the latest material code edit, run focused validation, inspect the actual
final diff, and run `git diff --check`. For meaningful changes invoke
`/code-review` when available; otherwise use the repository review stage. Fix
valid findings and review the corrected diff again when needed.

Use:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

`fast` is required before commit and `full` before push/PR publication when those
actions are authorized. Missing prerequisites remain blocked, not passed.

When opening or updating a pull request, preserve the complete
`.github/pull_request_template.md`, keep every validation claim truthful, leave
unperformed checks unmarked, and apply `levyra-humanizer` as the final prose
pass without changing facts.
