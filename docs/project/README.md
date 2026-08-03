# Levyra Project Documents

This folder separates durable requirements, ordered engineering direction, and the current reviewable work phase.

## Source-of-truth roles

| Document | Responsibility | Update when |
| --- | --- | --- |
| [`SPEC.md`](SPEC.md) | Owner-approved product behavior, engineering boundaries, non-goals, and acceptance criteria | A durable requirement or boundary changes |
| [`ROADMAP.md`](ROADMAP.md) | Ordered outcomes, risks, priorities, and phase exit criteria | Engineering direction or prioritization changes |
| [`TASKS.md`](TASKS.md) | One current reviewable phase with scope, work items, evidence, and blockers | Work starts, validation changes, or the active phase is replaced |

## Operating model

1. Confirm that the requested outcome is compatible with `SPEC.md`.
2. Locate the relevant roadmap track and exit criteria in `ROADMAP.md`.
3. Record the current bounded implementation phase in `TASKS.md`.
4. Keep architecture details in [`../ARCHITECTURE.md`](../ARCHITECTURE.md), not in the planning documents.
5. Keep AI tooling procedures under [`../ai/`](../ai/), not in product requirements.

## Maintenance rules

- Do not turn the roadmap into a release calendar or an authorization mechanism.
- Do not use the task file as an infinite backlog or changelog.
- Do not copy architecture or domain playbooks into these files; link to the canonical source.
- Record validation from commands, CI runs, reviews, manual checks, or explicit owner decisions—not from an agent narrative.
- Keep Android and Desktop product, versioning, packaging, and release boundaries distinct.
