# Levyra AI Engineering Guardrails

These rules exist to prevent AI-assisted work from making Levyra larger, harder
to reason about, or less maintainable while still appearing to "work".

They apply to ChatGPT, Codex, Claude Code, Google Antigravity, OpenCode,
OpenClaw-delegated runtimes, and any other coding agent used on this repository.
They supplement `AGENTS.md`; they do not replace repository architecture,
path-specific instructions, tests, or owner decisions.

## Core rule

A successful AI change is not the change that produces the most code or the most
features. It is the smallest reviewable change that solves the requested problem,
fits the existing architecture, preserves unrelated behavior, and can be verified
with direct evidence.

Green compilation is necessary when applicable, but it is not proof that the
architecture is sound.

## Before implementation

Before editing production code:

1. state the exact requested outcome in one or two sentences;
2. identify the current owner of the behavior in the existing architecture;
3. list the expected production files or modules likely to change;
4. identify existing clients, repositories, stores, caches, managers, services,
   controllers, helpers, scopes, and abstractions that can be reused;
5. state the behavior that must remain unchanged;
6. choose focused validation that can prove the change.

Do not start from a blank-slate design when Levyra already has an owner for the
behavior.

## Assumptions, tradeoffs, and simpler alternatives

Do not silently convert uncertainty into implementation.

- State material assumptions before coding when they can change behavior,
  compatibility, architecture, security, persistence, performance, or scope.
- If current repository evidence can resolve an uncertainty, inspect it before
  asking the owner to repeat information or choosing an interpretation.
- When two materially different interpretations remain, surface the tradeoff and
  use the least surprising interpretation only when existing requirements make
  it clear. Otherwise request the missing owner decision before broadening scope.
- If a simpler implementation satisfies the same acceptance criteria, prefer it
  and explain why the extra abstraction or configurability is unnecessary.
- Push back on speculative flexibility, future-proofing, compatibility layers,
  or defensive branches that have no current caller, requirement, or realistic
  failure mode.
- Do not hide uncertainty behind confident prose. Separate verified facts,
  assumptions, hypotheses, and owner decisions.

These rules adapt the useful anti-overengineering discipline from the
MIT-licensed `multica-ai/andrej-karpathy-skills` project. Levyra's repository
rules and current evidence remain authoritative; the external project is a
reference, not a runtime dependency.

## Architecture-first rules

- Reuse before creating.
- Extend an existing owner before introducing a parallel owner.
- Never add a second source of truth for playback, queue, persistence, cache,
  settings, localization, update state, requirements, task state, or release
  state.
- A new `Manager`, `Repository`, `Cache`, `Service`, `Coordinator`, `Engine`,
  wrapper, registry, event bus, or equivalent abstraction requires a concrete
  reason why the existing owner cannot safely handle the responsibility.
- Do not create abstractions for hypothetical future requirements.
- Do not duplicate an existing flow merely because a new implementation is
  easier for the agent to generate.
- Prefer deleting or simplifying obsolete code when the new behavior genuinely
  replaces it instead of keeping both implementations alive.
- Do not hide architectural duplication behind compatibility wrappers unless a
  real compatibility boundary requires them.

## Scope and complexity budget

One pull request should normally have one primary engineering outcome.

Treat any of the following as a mandatory scope checkpoint before continuing:

- the implementation needs production files well outside the originally
  identified ownership path;
- more than three unexpected production files become necessary;
- the non-generated production diff approaches 1,000 added lines;
- the change touches roughly 15 or more production files;
- more than two new architectural abstractions are being introduced;
- a requested feature starts turning into a platform-wide refactor;
- an agent proposes a second cache, second state holder, second resolver,
  parallel data path, or replacement architecture to avoid understanding the
  existing one.

At a scope checkpoint, do not keep expanding automatically. Re-evaluate the
architecture and split the work into smaller reviewable phases unless the owner
explicitly approves the larger scope with a concrete reason.

Generated localization, schema snapshots, lock files, or other mechanically
produced files may be excluded from the numeric heuristic, but they still require
normal review for correctness and accidental churn.

The thresholds above are review triggers, not permission to generate code up to
the limit.

## Goal-driven execution

Convert implementation requests into verifiable outcomes before editing.

For each non-trivial step, state the result that would prove it succeeded and use
that result to decide whether to continue, revise, or stop. Examples:

- a bug fix -> reproduce the failure or define the exact failing path, apply the
  smallest correction, then rerun the same reproduction or focused regression
  test;
- validation -> define invalid and valid cases, then prove both outcomes;
- refactoring -> establish behavior/tests before the refactor and verify the same
  contract afterward;
- performance work -> record a representative baseline, change one material
  variable, then remeasure the same path;
- security remediation -> reproduce the safe failure path, patch the root cause,
  then revalidate that exact boundary.

A multi-step plan should therefore be a sequence of `step -> verification`, not
an activity checklist such as "inspect, code, test" with no success criterion.
Do not keep stacking speculative fixes after the verification target has failed;
return to the hypothesis or requirement instead.

## Large features and feature parity

Do not implement broad "feature parity", "improve everything", or "make it like
project X" requests as one giant autonomous patch.

For large work:

1. map the target behavior against Levyra's current architecture;
2. separate required behavior from optional inspiration;
3. divide the work into independently reviewable phases;
4. implement one coherent phase at a time;
5. inspect the complete diff and validation result before starting the next
   phase;
6. keep later phases free to change based on evidence from earlier ones.

External projects are references, not architectures to copy wholesale. Import
ideas selectively and adapt them to Levyra's existing ownership model.

## Surgical-edit discipline

Every changed production line should be explainable by the requested outcome or
by a correctness dependency of that outcome.

- Match the existing style and ownership model instead of rewriting adjacent
  code into the agent's preferred style.
- Do not refactor, rename, reformat, reorder, or delete unrelated code while
  touching a nearby file.
- Remove imports, variables, helpers, or branches made obsolete by the current
  change, but do not turn that cleanup into a pre-existing dead-code sweep.
- Mention unrelated problems separately instead of silently folding their fixes
  into the active patch.
- If the implementation grows far beyond the expected footprint, stop and ask
  whether the design can be simplified before normalizing the larger diff.

## Diff quality gate

Before commit, push, or PR publication, inspect the complete diff and answer:

- Does every changed production file contribute directly to the requested
  outcome?
- Did the change introduce a new owner for behavior that already had one?
- Did any helper, wrapper, manager, cache, or layer appear only because it was
  convenient to generate?
- Is there dead, fallback, compatibility, or speculative code with no current
  caller or acceptance criterion?
- Could the same result be achieved with fewer moving parts?
- Are failure, cancellation, lifecycle, concurrency, persistence, and security
  semantics still explicit?
- Are tests proving the requested behavior rather than only exercising the new
  implementation?
- Did generated output or formatting churn hide meaningful changes?

If the answer reveals unnecessary complexity, simplify the diff before treating
it as ready.

## Agent delegation

Multiple agents do not remove the need for one coherent architecture.

When several agents or workers are used:

- give each worker a narrow ownership boundary;
- do not let different workers independently invent competing infrastructure;
- reconcile shared types and ownership before merging their patches;
- review the combined diff as a new artifact rather than assuming individually
  valid patches compose safely;
- require an independent review for broad or cross-domain changes.

Agent count, token count, elapsed compute, generated line count, or number of
implemented features are never quality metrics.

## Validation discipline

Use the existing Levyra quality gates from `AGENTS.md`.

For AI-generated changes also record the final diff summary when possible:

```bash
git diff --stat <base>...HEAD
git diff --numstat <base>...HEAD
```

Unexpected code growth is a reason to inspect the design, not a reason to praise
productivity.

A build that passes while the change duplicates ownership, leaks resources,
creates unbounded work, or makes future maintenance unsafe is still a failed
engineering result.

## ChatGPT activation phrase

When a ChatGPT conversation has access to this repository and the owner says
"lavoriamo su Levyra", "work on Levyra", or an obvious equivalent, treat that as
an instruction to enter Levyra engineering mode before technical work:

1. open `LUC4N3X/Levyra-deepsound`;
2. read the current root `AGENTS.md` and every nearer scoped instruction;
3. read this guardrail document;
4. load the relevant project planning, architecture, and native skills;
5. inspect current repository evidence before relying on previous chat memory;
6. apply the smallest-change and complexity-budget rules above.

The activation phrase grants repository context only. It does not grant permission
to commit, push, open or merge a PR, tag, release, publish, change versions, or
modify repository settings.

## Delivery

For every substantial AI-assisted implementation, report:

- requested outcome;
- architecture owner reused or changed;
- exact production files changed;
- new abstractions introduced and why each is necessary;
- material assumptions or unresolved tradeoffs;
- verification target for each non-trivial step;
- diff size/statistics when available;
- focused and broader validation performed;
- blocked or unverified checks;
- remaining risk;
- publication state.

Never present code volume, autonomous agent activity, or a green build alone as
proof of a good change.
