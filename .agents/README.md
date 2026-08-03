# Levyra OpenAI Agent Configuration

This directory contains the repository-local configuration intended for Codex and other OpenAI coding-agent workflows.

## Design goals

- Keep one authoritative engineering contract in the root `AGENTS.md`.
- Reuse Levyra's existing, battle-tested procedures under `.claude/rules/` and `.claude/skills/` instead of copying them into a second tree.
- Give Codex a discoverable repository skill that routes work to the correct Levyra procedure.
- Give ChatGPT a clear project setup document without pretending that a `.chatgpt/` directory is loaded automatically.
- Keep Claude-specific hooks, settings, permissions, agents, and marketplaces isolated under `.claude/`.

## How each assistant uses the repository

### Codex

Codex should start from the repository root so it can discover `AGENTS.md` and `.agents/skills/levyra-engineering/SKILL.md`.

Expected flow:

1. Read `AGENTS.md`.
2. Load the `levyra-engineering` skill for implementation, review, debugging, or release-preparation work.
3. Follow the matching procedure under `.claude/skills/` and rule files under `.claude/rules/`.
4. Inspect current code and tests before making assumptions.
5. Make the smallest coherent change and report validation truthfully.

### ChatGPT

The normal GitHub connection in ChatGPT is used to search and analyze repository content. It does not turn a repository folder into automatic global instructions, and the standard GitHub connection is not a substitute for Codex when code must be modified and published.

Create a ChatGPT Project for Levyra, connect or select this repository, and paste the contents of `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Project instructions.

That Project configuration directs ChatGPT to use:

- `AGENTS.md` as the engineering contract;
- `docs/ARCHITECTURE.md` as the architectural overview;
- `.claude/rules/` as path- and domain-specific rules;
- `.claude/skills/` as task procedures;
- current code, tests, workflows, and pull requests as live evidence.

### Claude Code

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`, `.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and `.claude/hooks/`.

The root `AGENTS.md` does not replace Claude's own configuration. It provides a shared repository contract so the product invariants and validation standards stay consistent across assistants.

## Directory structure

```text
AGENTS.md
.agents/
├── README.md
└── skills/
    └── levyra-engineering/
        └── SKILL.md
docs/
└── ai/
    ├── README.md
    └── CHATGPT_PROJECT_INSTRUCTIONS.md
.claude/
├── CLAUDE.md
├── rules/
├── skills/
├── agents/
├── hooks/
└── settings.json
```

## Maintenance rules

- Put repository-wide product and engineering invariants in `AGENTS.md`.
- Put architecture detail in `docs/ARCHITECTURE.md`.
- Keep specialized procedures in `.claude/skills/` and route to them from the OpenAI skill.
- Keep Claude-only permissions, hooks, plugins, and subagents in `.claude/`.
- Update the ChatGPT Project instructions only when the collaboration contract changes.
- Do not copy entire procedure files into `.agents/`; duplication creates drift and conflicting instructions.
- When a review identifies a recurring project-specific failure, update the narrowest authoritative rule or procedure rather than adding the same warning everywhere.
