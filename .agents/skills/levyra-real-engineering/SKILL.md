---
name: levyra-real-engineering
description: Route non-trivial Levyra engineering through the Matt Pocock real-engineering workflow: clarify ambiguous work, build shared domain language, create a spec, split large work into vertical tickets, implement with focused tests, then independently review. Use for features, architectural changes, unclear bugs, multi-step work, or requests that risk AI slop; skip the ceremony for tiny obvious edits.
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

### 1. Ambiguous product or architecture: `grill-with-docs`

Use when the requested behavior, trade-offs, ownership, compatibility, or user-visible outcome is genuinely unclear.

- Inspect the repository first and answer codebase questions from evidence instead of asking the owner.
- Ask only decisions that cannot be resolved from the repository.
- Maintain shared vocabulary in the project's domain context only when a term is genuinely reusable.
- Record an ADR only for a durable architectural decision with a real trade-off and meaningful reversal cost.
- Stop grilling once the implementation contract is clear.

### 2. Large unknown problem: `wayfinder`

Use before spec work when there are several unresolved architectural or product decisions and prematurely writing a spec would encode guesses.

Resolve the decision map first, then return to the normal pipeline.

### 3. Settled intent: `to-spec`

Convert the resolved conversation and repository evidence into an implementation-ready specification.

A Levyra spec must state:

- desired behavior and non-goals;
- existing behavior that must remain unchanged;
- architecture ownership and reuse points;
- security, lifecycle, persistence, concurrency, localization, and compatibility implications when relevant;
- acceptance criteria;
- focused automated validation and required manual checks.

Do not invent requirements that the owner did not approve.

### 4. Work too large for one reviewable change: `to-tickets`

Split the spec into independently reviewable vertical slices, not horizontal layer tasks.

Each ticket must be self-contained enough for a fresh agent context and include scope, preserved behavior, dependencies, acceptance criteria, relevant files/owners, and validation. Prefer a few coherent tickets over dozens of micro-tasks.

Creating GitHub issues is a publication action: do it only when the owner explicitly asks for issues/tickets to be published. Otherwise keep the decomposition in the current handoff or approved planning files.

### 5. Implementation: `implement` + `tdd`

Implement one ticket or one reviewable phase at a time.

- Start from a fresh context when moving to a new independent ticket when practical.
- Read the applicable Levyra domain skills before editing.
- For defects, establish a concrete failing path first; use `diagnosing-bugs` when the root cause is unclear.
- Prefer red -> green -> refactor for logic that can be covered deterministically.
- Do not force artificial tests around trivial declarative changes; still validate the narrowest relevant behavior.
- Make the smallest coherent change and avoid unrelated cleanup.

### 6. Review: `code-review` + `levyra-pr-review`

Matt's code review is supplementary to Levyra's repository review contract.

Review correctness, regression risk, tests, architecture fit, security, concurrency/lifecycle, performance, and code smells. A smell is a judgment signal, not proof of a defect. Levyra's documented architecture and invariants override generic style advice.

Use a fresh reviewer context/model when practical. Every actionable finding needs a concrete failure path or maintainability consequence and the smallest compatible fix.

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

## Context discipline

- Keep the current ticket/phase small enough to review.
- Prefer fresh context between independent tickets.
- Carry forward the approved spec, domain vocabulary, ADRs, exact ticket, and direct validation evidence; do not carry stale exploratory chatter as authority.
- Current repository evidence always overrides remembered conversation context.

## Publication and quality gates

Matt's workflow never grants repository publication permission.

Before commit:

```bash
python3 scripts/ai_quality_gate.py --profile fast
```

Before push or pull-request publication:

```bash
python3 scripts/ai_quality_gate.py --profile full
```

Commit, push, pull request, merge, tag, release, deployment, version changes, and repository settings remain owner-controlled exactly as defined by `AGENTS.md`.
