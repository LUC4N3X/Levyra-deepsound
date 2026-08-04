# Levyra Documentation

This directory is the canonical documentation hub for Levyra. Keep documents
grouped by purpose so product requirements, current architecture, AI workflows,
and visual assets remain easy to find and maintain.

## Documentation map

```text
docs/
├── README.md                 documentation index
├── AGENTS.md                 instructions for documentation changes
├── ARCHITECTURE.md           current Android and Desktop architecture
├── project/                  product planning and active engineering work
│   ├── README.md
│   ├── SPEC.md
│   ├── ROADMAP.md
│   └── TASKS.md
├── ai/                       supported AI-assisted engineering workflows
│   ├── README.md
│   ├── ANTIGRAVITY.md
│   ├── CHATGPT_PROJECT_INSTRUCTIONS.md
│   ├── OPENCLAW.md
│   └── WORKFLOW.md
└── assets/                   README badges, previews, and documentation media
```

## Project direction

- [`project/SPEC.md`](project/SPEC.md) defines durable owner-approved product
  and engineering requirements.
- [`project/ROADMAP.md`](project/ROADMAP.md) orders engineering outcomes, risks,
  and phase exit criteria.
- [`project/TASKS.md`](project/TASKS.md) records the current reviewable phase and
  its direct validation evidence.
- [`project/README.md`](project/README.md) explains how these documents work
  together.

## Architecture

- [`ARCHITECTURE.md`](ARCHITECTURE.md) describes the current implementation,
  ownership boundaries, and major data flows.
- [`../desktop/README.md`](../desktop/README.md) documents the independent
  Windows Desktop client, packaging, and runtime behavior.

## AI-assisted engineering

- [`ai/README.md`](ai/README.md) is the entry point for repository AI tooling.
- [`ai/WORKFLOW.md`](ai/WORKFLOW.md) defines the implementation, review, CI,
  manual verification, merge, and release lifecycle.
- [`ai/ANTIGRAVITY.md`](ai/ANTIGRAVITY.md) defines Google Antigravity workspace
  discovery, skill loading, verification, and troubleshooting.
- [`ai/OPENCLAW.md`](ai/OPENCLAW.md) defines OpenClaw workspace and delegation
  guidance.
- [`ai/CHATGPT_PROJECT_INSTRUCTIONS.md`](ai/CHATGPT_PROJECT_INSTRUCTIONS.md)
  contains the source instructions for the Levyra ChatGPT Project.

## Maintenance rules

- Keep one canonical document for each responsibility; link instead of
  duplicating whole sections.
- Update `project/SPEC.md` only for approved durable requirements or non-goals.
- Update `project/ROADMAP.md` only when priorities, outcomes, risks, or exit
  criteria change.
- Replace the active phase in `project/TASKS.md` rather than accumulating
  unrelated historical work.
- Update `ARCHITECTURE.md` when ownership, data flow, module boundaries,
  persistence, playback, networking, or release architecture changes.
- Store documentation images and generated badges under `assets/`; do not
  scatter them through planning or AI folders.
- Apply the scoped rules in [`AGENTS.md`](AGENTS.md) to every documentation
  change.
