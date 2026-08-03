# AI Assistant Setup for Levyra

This guide explains how Levyra uses ChatGPT, Codex and Claude Code without conflicting instruction trees.

## Architecture at a glance

```text
AGENTS.md                         shared repository contract
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI/workflow rules
docs/AGENTS.md                    documentation rules
.agents/skills/*/SKILL.md         native Codex/OpenAI skills
docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md
                                  ChatGPT Project instructions source
.claude/                          Claude Code configuration and detailed playbooks
```

`AGENTS.md` belongs in the Git root because Codex discovers project instructions from the project root toward the current working directory. Files under `.agents/skills/` are task skills, not replacements for root project guidance.

## Codex setup

1. Open or clone the repository.
2. Start Codex from the repository root or a directory inside it.
3. Codex loads root `AGENTS.md` and any nearer path-specific `AGENTS.md`.
4. Use the most specific native skill or skills for the task.
5. Inspect current code, tests and detailed playbooks before editing.
6. Make the smallest coherent change and report validation truthfully.

Native skills:

- `levyra-player`
- `levyra-extractor`
- `levyra-database`
- `levyra-compose`
- `levyra-motion-artwork`
- `levyra-desktop`
- `levyra-security-review`
- `levyra-ci-workflows`
- `levyra-pr-review`
- `levyra-release-check`
- `levyra-engineering` for genuine cross-domain coordination

Recommended orientation prompt:

```text
Read the applicable AGENTS.md files and use the most specific Levyra native skills. Inspect current code and tests before making assumptions. Describe the verified behavior, likely root cause, intended files, risks and validation plan before editing.
```

## ChatGPT setup

Repository files do not automatically become persistent ChatGPT Project instructions.

1. Create a ChatGPT Project named `Levyra`.
2. Connect or select `LUC4N3X/Levyra-deepsound` through the available GitHub integration.
3. Copy the full contents of `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Project instructions.
4. Keep Levyra planning, investigation, architecture, PR review and release preparation inside that Project.
5. Use Codex for implementation, tests and repository publication when authorized.

The Project instructions direct ChatGPT to read the root and path-specific `AGENTS.md` files, select native skills, inspect current code/tests and distinguish verified repository state from assumptions.

## Claude Code setup

Claude Code continues to use `.claude/README.md` and the complete `.claude/` configuration:

- `.claude/CLAUDE.md`
- `.claude/rules/`
- `.claude/skills/`
- `.claude/agents/`
- `.claude/hooks/`
- `.claude/settings.json`

The OpenAI configuration references detailed Levyra playbooks under `.claude/` instead of duplicating them.

## Responsibility split

| Assistant | Primary role | Main configuration |
| --- | --- | --- |
| ChatGPT Project | Product decisions, investigation, architecture, planning, PR interpretation, Codex task preparation | Project instructions copied from `CHATGPT_PROJECT_INSTRUCTIONS.md` plus connected repository |
| Codex | Implementation, tests, validation, commits, branches and pull requests when authorized | root/path `AGENTS.md` plus `.agents/skills/` |
| Claude Code | Implementation and review using Claude-specific hooks, agents, permissions and plugins | `.claude/` |

## Keeping instructions consistent

- Update root `AGENTS.md` for shared repository-wide invariants.
- Update the nearest nested `AGENTS.md` for platform/path constraints.
- Update one native skill for a repeatable task workflow.
- Update `docs/ARCHITECTURE.md` for architecture or ownership changes.
- Update the narrowest detailed `.claude/rules/` or `.claude/skills/` playbook for recurring domain-specific failures.
- Update `CHATGPT_PROJECT_INSTRUCTIONS.md` only when ChatGPT collaboration behavior changes.
- Prefer routing and references over duplicated prose.
- Verify paths, commands, task names, version locations, workflow names and artifact paths after structural changes.
