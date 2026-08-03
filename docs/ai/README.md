# AI Assistant Setup for Levyra

This guide explains how to use Levyra with ChatGPT, Codex, and Claude Code without creating conflicting instruction trees.

## Repository contract

`AGENTS.md` is the shared repository-level engineering contract. It defines product invariants, architecture rules, validation expectations, repository safety, release boundaries, and delivery requirements.

Specialized Levyra rules and procedures remain under `.claude/rules/` and `.claude/skills/`. They are written as repository documentation and may be read by any assistant. Claude-specific execution settings, permissions, hooks, plugins, and subagents remain isolated under `.claude/`.

## Codex setup

1. Open or clone the repository.
2. Start Codex from the repository root.
3. Confirm that the root `AGENTS.md` is in scope.
4. Use the repository skill at `.agents/skills/levyra-engineering/SKILL.md` for Levyra implementation, debugging, review, and release-preparation tasks.
5. Let the skill route the task to the relevant procedure under `.claude/skills/`.

Recommended first prompt:

```text
Read AGENTS.md and use the levyra-engineering skill. Inspect the current repository before making assumptions. Do not modify or publish anything until you have described the root cause, intended files, risks, and validation plan.
```

## ChatGPT setup

ChatGPT can use a connected GitHub repository to search and analyze current code and documentation. Repository files are not a replacement for ChatGPT Project instructions, so configure the Project explicitly.

1. Create a ChatGPT Project named `Levyra`.
2. Connect or select the `LUC4N3X/Levyra-deepsound` repository through the GitHub app available in your ChatGPT experience.
3. Copy the full contents of `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Project instructions.
4. Keep Levyra planning, architecture discussions, bug analysis, PR review, and release preparation inside that Project so the instructions remain active.
5. Use Codex when the task requires editing, testing, committing, pushing, or opening a pull request.

The standard ChatGPT GitHub connection is primarily for repository search and analysis. Access and write capabilities can vary by product experience, so never assume that a change was published unless the resulting branch, commit, or pull request was verified on GitHub.

## Claude Code setup

Claude Code continues to use `.claude/README.md` and the complete `.claude/` configuration.

Start Claude Code from the repository root and follow the usage notes in `.claude/README.md`.

## Responsibility split

| Assistant | Primary role | Main configuration |
| --- | --- | --- |
| ChatGPT Project | Product decisions, planning, architecture discussion, repository analysis, PR interpretation | Project instructions copied from `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` plus connected GitHub repository |
| Codex | Implementation, tests, local validation, commits, branches, pull requests when authorized | `AGENTS.md` and `.agents/skills/levyra-engineering/SKILL.md` |
| Claude Code | Implementation and review using Claude-specific hooks, agents, permissions, and skills | `.claude/` |

## Keeping instructions consistent

- Update `AGENTS.md` for shared repository-wide rules.
- Update `docs/ARCHITECTURE.md` for architecture.
- Update the narrowest matching file under `.claude/rules/` or `.claude/skills/` for domain-specific procedures.
- Update `CHATGPT_PROJECT_INSTRUCTIONS.md` only when ChatGPT collaboration behavior changes.
- Do not copy an entire rule or skill into several assistant-specific directories.
- Prefer links and routing over duplicated prose.
