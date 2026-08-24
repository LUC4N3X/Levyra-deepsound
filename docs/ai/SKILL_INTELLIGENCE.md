# Levyra Skill Intelligence

Levyra does not improve its coding agents by installing every available skill catalog. The goal is higher correctness per unit of context: the smallest set of skills that materially improves implementation, debugging, review, validation, or safety.

## Intake rule

Before adopting an external skill, plugin, agent, or workflow:

1. Map the useful behavior to an existing Levyra owner skill whenever possible.
2. Prefer rewriting a small Levyra-native rule over adding a new always-discoverable skill.
3. Keep detailed references and scripts on demand rather than in always-loaded instructions.
4. Do not install a large catalog merely because some entries are useful.
5. Reject duplicate abstractions, parallel project rules, and external toolchain assumptions that conflict with Levyra.
6. Preserve the current Claude context budget and automatic routing guarantees.

A new standalone skill is justified only when it owns a distinct recurring task that cannot be represented cleanly by an existing Levyra skill.

## Routing evaluation

Run:

```bash
python3 scripts/evaluate_skill_routing.py
```

The evaluator uses deterministic repository-local cases. It does not call Claude, another model, or an external API in CI.

Each case can assert:

- required skills that must trigger;
- forbidden skills that must not trigger;
- a maximum selected-skill count;
- a maximum additional-context byte budget.

The corpus must include ordinary positive examples, difficult near-misses, and collision cases. A new route is incomplete until at least one realistic positive case and one plausible near-miss are covered.

Infrastructure or evaluator errors are `ERROR`, never a successful negative trigger. Do not turn a timeout, subprocess failure, unavailable model, or parser failure into evidence that a skill correctly stayed inactive.

## Model-assisted benchmarks

When changing a substantial skill body or considering a new third-party skill, a model-assisted benchmark may be useful outside deterministic CI. Compare representative tasks with the candidate guidance against the existing Levyra baseline and record, when observable:

- correctness and acceptance-criteria coverage;
- focused tests/validation passed;
- regression or review findings;
- files/symbols changed and unrelated churn;
- prompt/context tokens;
- wall-clock/tool-call cost;
- whether the skill triggered on the intended tasks and stayed quiet on near-misses.

A candidate that materially increases token or tool cost without improving correctness, scope discipline, validation, or review quality should not be adopted.

## Debugging and test discipline

For non-trivial defects, `levyra-real-engineering` owns the workflow:

```text
reproduce -> isolate -> hypothesis -> smallest experiment -> fix -> prevent
```

Classify a failing test/check before reacting to it: product regression, test defect, environment/infrastructure, flaky/intermittent, or unknown. Do not rerun an unchanged command repeatedly to fish for green output.

Hypotheses and findings have lifecycle. When evidence disproves a hypothesis, retract it explicitly and stop using it downstream. New evidence beats an older agent handoff or previous review comment.

## Finding confidence and evidence hygiene

`levyra-security-review` and `levyra-pr-review` distinguish suspected findings from validated findings. A warning, status code, static pattern, linter message, or model suspicion is not enough by itself.

Before evidence leaves the local/private working context, redact or replace secrets, bearer tokens, cookies, signed URLs, private endpoints, account identifiers, and unrelated PII. Preserve the structure needed to reproduce the finding: status codes, request IDs, hashes, timestamps, relevant headers with sensitive values replaced, and minimal synthetic fixtures.

If sanitization would destroy the evidence, report that the sensitive artifact was withheld instead of publishing it raw.

## Source material

This policy selectively adapts useful ideas from:

- `anthropics/skills`: progressive disclosure and trigger/evaluation discipline;
- `Jeffallan/claude-skills`: hypothesis-driven debugging and test/review discipline;
- `elementalsouls/Claude-BugHunter`: validation gates, explicit retraction, and evidence hygiene;
- `ChrisTitusTech/titus-ai`: selective output compression and minimal always-loaded guidance.

No external catalog is vendored or made authoritative. Levyra's repository rules, architecture, tests, owner decisions, and direct evidence always take precedence.
