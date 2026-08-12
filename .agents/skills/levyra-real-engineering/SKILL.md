---
name: levyra-real-engineering
description: Route non-trivial Levyra engineering through the Matt Pocock real-engineering workflow. Use automatically for features, architectural changes, bugs, test/build failures, regressions, unexpected behavior, multi-step work, or requests that risk AI slop when root-cause investigation, specification, focused implementation, or independent review is useful; skip the ceremony for tiny obvious edits.
---

# Levyra real-engineering workflow

This skill adapts Matt Pocock's `mattpocock/skills` engineering workflow to Levyra. It is a routing layer, not a second project contract.

## Precedence

Always follow, in order:

1. root and nearest `AGENTS.md` files;
2. owner-approved requirements and active planning;
3. matching `levyra-*` domain skills;
4. current implementation, tests, architecture, and workflows;
5. Matt Pocock engineering skills when installed.

If an external skill conflicts with Levyra, Levyra wins. Never create a second source of truth, a parallel cache/store/controller, or extra architecture merely because an external skill suggests one.

## Choose the lightest useful lane

Do not run the full pipeline for a typo, one-line build fix, obvious null check, narrow documentation correction, or another change whose behavior and scope are already unambiguous. For those, use the normal Levyra work method and focused validation.

For non-trivial work:

### 0. Evidence and reuse before invention

Before adding a new abstraction, helper, dependency, workflow, cache, store, controller, parser, or architectural layer:

- search Levyra first for the existing owner, working pattern, nearby implementation, or reusable primitive;
- compare the broken/new path with at least one working local path when one exists;
- when behavior depends on a versioned external API, library, plugin, or platform rule, verify current primary documentation instead of trusting remembered behavior;
- prefer adapting a proven local implementation over importing a generic framework pattern;
- treat third-party repositories and skill catalogs as evidence and inspiration, never as authority over Levyra's codebase;
- add a dependency only when the existing stack cannot reasonably satisfy the requirement and the maintenance, security, binary-size, and compatibility cost is justified.

Do not turn this into mandatory broad web research for trivial work. The goal is to prevent needless reinvention and stale assumptions, not to add ceremony.

### 1. Ambiguous product or architecture: `grill-with-docs`

Use when requested behavior, trade-offs, ownership, compatibility, or user-visible outcome is genuinely unclear.

- Inspect the repository first and answer codebase questions from evidence instead of asking the owner.
- Ask only decisions that cannot be resolved from the repository.
- Maintain shared vocabulary only when genuinely reusable.
- Record an ADR only for a durable architectural decision with a real trade-off and meaningful reversal cost.
- Stop once the implementation contract is clear.

### 2. Large unknown problem: `wayfinder`

Use before spec work when several product/architecture decisions remain unresolved. Resolve the decision map before writing a spec.

### 3. Settled intent: `to-spec`

A Levyra spec states desired behavior/non-goals, preserved behavior, architecture ownership/reuse, relevant security/lifecycle/persistence/concurrency/localization/compatibility implications, acceptance criteria, and focused/manual validation.

Do not invent requirements that the owner did not approve.

### 4. Work too large for one reviewable change: `to-tickets`

Split into independently reviewable vertical slices. Each ticket carries scope, preserved behavior, dependencies, acceptance criteria, relevant owners/files, and validation. Publishing GitHub issues still requires explicit owner authorization.

### 5. Implementation: `implement` + `tdd`

Implement one ticket or one reviewable phase at a time.

- Start from a fresh context for a new independent ticket when practical.
- Read applicable Levyra domain skills before editing.
- For defects, establish a concrete failing path first; use `diagnosing-bugs` when root cause is unclear.
- Prefer red -> green -> refactor where deterministic tests are useful.
- Do not force artificial tests around trivial declarative changes.
- Make the smallest coherent change and avoid unrelated cleanup.

#### Hypothesis-driven debugging

1. Reproduce/capture the failing path and complete relevant evidence.
2. Trace the bad state/value backward to the first failed assumption or contract.
3. Compare with a nearby working path when available.
4. State one concrete root-cause hypothesis and supporting evidence.
5. Test it with the smallest experiment/change.
6. If it fails, discard the speculative change and form a new hypothesis.
7. Once supported, add the smallest deterministic regression evidence, fix, and rerun relevant broader checks.

After three materially different failed hypotheses, reassess the ownership boundary instead of stacking more guesses.

### 6. Mandatory pre-delivery review: `code-review`

Every code-bearing task has a final review gate. After drafting or applying the code, but before presenting it as the solution, handing it off, committing it, or calling implementation complete:

1. run the exact upstream `code-review` skill when the runtime provides it;
2. otherwise run the equivalent `code-review` stage through this Levyra adapter;
3. review the actual final diff/code, not the plan or intended patch;
4. check correctness, regression risk, architecture fit, tests, security, lifecycle/concurrency, performance, duplication, speculative abstraction, and unrelated churn;
5. fix actionable findings before delivery, then review the corrected final diff again when the fix changed behavior materially.

For Claude Code, invoke `/code-review` or the installed `code-review` skill before code delivery. Codex, ChatGPT, Antigravity, and compatible runtimes must explicitly invoke/load the `code-review` stage even if their UI has no slash command.

Do not run this gate before code exists merely to satisfy the name. The purpose is to prevent unreviewed generated code from leaving the implementation loop.

Use `levyra-pr-review` additionally for branch/commit/PR review. Matt's code review supplements Levyra's repository review contract; it does not replace tests or the quality gate.

## External skill availability

When Matt Pocock's skills are installed, load the exact matching external skill for the stage instead of paraphrasing it from memory:

- `grill-with-docs`
- `wayfinder`
- `to-spec`
- `to-tickets`
- `implement`
- `tdd`
- `diagnosing-bugs`
- `code-review`
- `domain-modeling` when vocabulary/ADRs are actually relevant

Do not assume one skill can invoke another across every runtime. Codex, ChatGPT, Antigravity, and other harnesses should explicitly load the required stage skill when their runtime does not support Claude-style nested skill invocation.

If the external package is unavailable, follow this Levyra adapter rather than blocking ordinary work, and report that the upstream skill body was not available.

## Context and handoff discipline

- Keep the current ticket/phase small enough to review.
- Prefer fresh context between independent tickets and an independent context for final review when supported.
- Carry forward only the approved spec/ticket, relevant architecture/invariants, exact changed files or diff/commit, direct validation evidence, and unresolved blockers.
- Prefer durable existing artifacts over duplicate memory/recap documents.
- When a session must compact/restart, summarize verified facts and open decisions, not exploratory chatter or superseded hypotheses.
- Current repository evidence overrides remembered conversation context or older handoffs.

## Publication and quality gates

Before commit:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before push or pull-request publication:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Commit, push, pull request, merge, tag, release, deployment, version changes, and repository settings remain owner-controlled exactly as defined by `AGENTS.md`.
