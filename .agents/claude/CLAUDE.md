# Levyra Claude Code

This is Claude's compact projection of the Levyra engineering contract. Root
`AGENTS.md` remains the cross-runtime source of truth, but do not preload it or
large `docs/ai/` playbooks wholesale on ordinary coding turns. Load the specific
canonical document only when the active question requires details that are not
already enforced by hooks or scoped rules.

The project lifecycle hooks keep `docs/ai/ALWAYS_ON_AGENT_GUARDS.md` active and
inject applicable scoped `AGENTS.md`/current-file evidence around mutations.
Path-scoped files under `.claude/rules/` load when relevant code is touched.

## Core coding contract

- Current repository evidence outranks memory, stale comments, previous agent
  output, and old task status.
- Inspect the current implementation and nearby tests before changing behavior.
- Make the smallest coherent root-cause fix. Reuse existing owners, clients,
  players, caches, stores, state models, and policies before adding structure.
- Protect playback reliability, explicit song/audio versus native-video choice,
  MediaSession/notification/Android Auto/queue synchronization, privacy, and
  persisted user data before optional polish.
- Preserve cancellation, lifecycle, concurrency, transient-failure, no-match,
  persistence, and compatibility semantics.
- Keep blocking network/database/disk/extraction/media work off UI threads.
- Do not add explanatory source-code comments. Prefer clear names and structure;
  preserve only required license/generated/lint/compatibility comments.
- `only this`, `solo questo`, and equivalents are hard scope boundaries.
- Commit, push, PR, merge, tag, release, deployment, version changes, and
  repository settings remain owner-controlled.

For a non-trivial production change or broad review, consult
`docs/ai/AI_ENGINEERING_GUARDRAILS.md` only when its detailed decision procedure
is needed. Apply `docs/ai/EVIDENCE_GATED_COMPLETION.md` at completion; the Stop
hook enforces the critical evidence state mechanically.

## Immediate context budget

Apply before broad reading on every real coding task:

1. identify the likely owner/module and the exact question the next read answers;
2. search path, symbol, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already present in the active context;
6. load only the skills routed for this task, never the whole skill tree.

For read-only discovery that would otherwise flood the main conversation, prefer
Claude's built-in Explore agent. Use the custom implementation agent only when
isolated implementation context is actually useful; tiny/local fixes stay in the
main agent.

For noisy builds, tests, lint, logs, broad searches, dependency listings, GitHub
or CI output, invoke `levyra-context-efficiency`. Prefer RTK only when its
filtered output is sufficient; rerun raw when exact stdout/stderr, stack traces,
security/signing evidence, Perfetto/R8 evidence, or full diagnostics matter.
Token savings never override correctness.

## Subagent token discipline

Custom subagents already receive this project context automatically. Never tell
a subagent to reread `.claude/CLAUDE.md` merely as bootstrap.

Delegate with one compact handoff containing only:

- goal and verified/current evidence;
- affected files or symbols;
- invariants/constraints that matter to this task;
- acceptance checks and any real blocker.

Do not paste the parent conversation, broad repository summaries, whole files,
or unrelated findings into a delegation message. Do not preload skill bodies in
subagent frontmatter; let the subagent invoke the routed skill through `Skill`
when it actually needs that procedure.

Keep `model: inherit` and high reasoning for Levyra coding/review agents. Save
tokens by reducing duplicate context and unused tools, not by lowering coding
quality.

## Deterministic skill loading

The `UserPromptSubmit` hook executes `scripts/agent_skill_router.py`. Every name
under **Mandatory skill load** must be invoked before broad repository reading,
editing, or shell work. The hook is the routing source of truth; do not maintain
or scan a second full routing table in this always-loaded file.

Compatibility inventory for the core automatic routes:
`levyra-real-engineering`, `levyra-compose`, `levyra-design-taste`,
`levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, `levyra-release-check`.
The shared router may select additional focused Levyra skills and companions.

## Delegation policy

- Do not spawn `levyra-android-developer` for a trivial, already-local one-file
  fix that the main agent can safely implement with focused validation.
- Use `levyra-android-developer` for substantial Android implementation/debugging
  where isolated code-reading/editing context keeps the main conversation clean.
- Use `levyra-reviewer` after meaningful or risky changes, before merge-quality
  handoff, or when an independent read-only pass can find regressions. Do not
  spawn it ceremonially for a tiny edit with direct focused evidence.
- Use built-in Explore for broad read-only repository discovery whenever its
  lower startup context is sufficient.

## Claude context hygiene

Use `/clear` at safe task boundaries, not mid-debugging. If the next owner
request is unrelated and the prior task is complete/checkpointed, prefer a fresh
context when the stop audit recommends it. Use `/compact` when the same task
still needs summarized history. Never claim a hook executed a slash command.

## Review, validation, delivery

For code-bearing work, run focused validation after the latest material edit,
inspect the actual final diff, and run `git diff --check`. Invoke `/code-review`
when available for meaningful changes; otherwise use the installed review stage
through `levyra-real-engineering`. Fix actionable findings and review the final
corrected diff again when the fix materially changes it.

Use the repository quality gate when applicable:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

Run `fast` before commit and `full` before push/PR publication. Missing or
blocked checks are not passes. Report exactly what ran and what remains
unverified.

When opening or updating a pull request, keep the complete
`.github/pull_request_template.md` structure, fill it with verified evidence,
leave unperformed checks unmarked, and invoke `levyra-humanizer` as the final
prose pass without changing any claim.
