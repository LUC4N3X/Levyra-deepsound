# Levyra Evidence-Gated Completion

This rule adds a lightweight completion discipline for AI-assisted engineering.
It complements `AGENTS.md` and `docs/ai/AI_ENGINEERING_GUARDRAILS.md`; it does
not replace architecture, tests, current repository evidence, or owner decisions.

## When to use it

Apply this rule to non-trivial implementation, debugging, refactoring,
performance, security, build/CI, migration, or cross-domain work.

Tiny, already-local edits may use one implicit acceptance gate plus the focused
check that proves it. Do not create ceremony for a one-line fix.

## Acceptance gates

Before editing, convert the requested outcome into the smallest useful set of
observable acceptance gates, normally two to six.

Each gate has three parts:

1. **Condition** — the behavior or property that must be true.
2. **Check** — the command, test, inspection, reproduction, trace, diff review,
   or direct runtime observation that can prove it.
3. **Evidence** — the actual result from that check.

Use only gates that matter to the owner's request or to a real correctness,
compatibility, security, lifecycle, persistence, performance, or publication
boundary. Generic busywork is not a gate.

## Truthful status

A gate may be:

- `PASS` — direct evidence proves the condition;
- `FAIL` — direct evidence disproves it;
- `BLOCKED` — the required environment, dependency, credential, device, SDK,
  service, or permission is unavailable;
- `UNRUN` — the check has not been executed.

Only `PASS` means passed. Intention, confidence, compilation alone, another
agent's narrative, stale CI, or a previous run on different code is not evidence
for the current gate.

If a fix materially changes code covered by a passed gate, rerun that gate when
the change could invalidate its evidence.

## Completion rule

Do not present non-trivial work as complete while a required gate is `FAIL`,
`BLOCKED`, or `UNRUN`. Report the unresolved state precisely instead.

After all required gates pass:

1. inspect the complete final diff for unrelated churn and accidental scope
   expansion;
2. run the mandatory pre-delivery code review;
3. rerun any gate invalidated by review fixes;
4. stop adding speculative work once the requested outcome is proven.

Build, lint, tests, runtime checks, review, push, PR, merge, and release are
separate states. Never collapse them into `done`.

## Keep it lightweight

Do not create `GATES.md`, tracking files, extra abstractions, or permanent
framework code merely to implement this rule. Persist gates in
`docs/project/TASKS.md` only when the work is already a tracked multi-phase
project and that document is the established owner.

The goal is stronger evidence with less premature completion, not more process
or more generated code.

## Delivery

For substantial work, map each requested outcome to its direct evidence and
report:

- passed gates and the exact checks that proved them;
- failed, blocked, or unrun gates;
- final diff/review state;
- publication state separately from implementation state.

If a required check cannot run, say so plainly and leave the corresponding gate
open.
