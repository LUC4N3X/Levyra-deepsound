# Levyra Claude Code Configuration

Everything is intentionally stored inside `.claude/`.

Claude Code officially supports `.claude/CLAUDE.md` as the project instruction file, `.claude/rules/` for modular path-aware rules, `.claude/skills/` for reusable procedures, and `.claude/agents/` for project-specific subagents.

## Structure

```text
.claude/
├── CLAUDE.md
├── README.md
├── agents/
│   ├── levyra-android-developer.md
│   └── levyra-reviewer.md
├── rules/
│   ├── architecture.md
│   ├── compose-ui.md
│   ├── data-room.md
│   ├── extractor-network.md
│   ├── localization.md
│   ├── player.md
│   ├── security.md
│   └── testing-release.md
└── skills/
    ├── levyra-compose/SKILL.md
    ├── levyra-database/SKILL.md
    ├── levyra-extractor/SKILL.md
    ├── levyra-motion-artwork/SKILL.md
    ├── levyra-player/SKILL.md
    ├── levyra-pr-review/SKILL.md
    ├── levyra-release-check/SKILL.md
    └── levyra-security-review/SKILL.md
```

## Usage

Start Claude Code from the repository root. Use `/context` to confirm that `.claude/CLAUDE.md` and the unconditional rules loaded.

Useful manual skills:

- `/levyra-player`
- `/levyra-extractor`
- `/levyra-motion-artwork`
- `/levyra-database`
- `/levyra-compose`
- `/levyra-security-review`
- `/levyra-pr-review`
- `/levyra-release-check`

Ask Claude to use `levyra-android-developer` for implementation work or `levyra-reviewer` for a read-only review.

If this is the first time the `agents/` or `skills/` directory exists during an already-running Claude Code session, restart the session once so every entry is discovered.

## Maintenance

Keep `.claude/CLAUDE.md` concise and reserve multi-step procedures for skills. Update the matching rule whenever a review identifies a project-specific mistake that should not recur.
