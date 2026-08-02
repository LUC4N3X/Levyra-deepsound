# Levyra Claude Code Configuration

Everything is intentionally stored inside `.claude/`.

Claude Code officially supports `.claude/CLAUDE.md` as the project instruction file, `.claude/rules/` for modular path-aware rules, `.claude/skills/` for reusable procedures, `.claude/agents/` for project-specific subagents, `.claude/settings.json` for shared project settings, and `.claude/hooks/` for scripts those settings invoke.

## Structure

```text
.claude/
├── CLAUDE.md
├── README.md
├── settings.json
├── agents/
│   ├── levyra-android-developer.md
│   └── levyra-reviewer.md
├── hooks/
│   └── session-start.sh
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
    ├── levyra-desktop/SKILL.md
    ├── levyra-extractor/SKILL.md
    ├── levyra-motion-artwork/SKILL.md
    ├── levyra-player/SKILL.md
    ├── levyra-pr-review/SKILL.md
    ├── levyra-release-check/SKILL.md
    └── levyra-security-review/SKILL.md
```

## settings.json

`settings.json` is checked in and applies to everyone who opens the repository in Claude Code.

- `permissions.allow` pre-approves the read-only git commands and the Gradle verification tasks from `CLAUDE.md`, so routine checks do not stop for a prompt.
- `permissions.deny` blocks reads and writes of `local.properties`, keystores, and `.env` files. This is a guardrail, not a substitute for the release-safety rules in `CLAUDE.md`.
- `hooks.SessionStart` runs `hooks/session-start.sh`.
- `extraKnownMarketplaces` and `enabledPlugins` opt this repository into three external skill marketplaces: [`chrisbanes/skills`](https://github.com/chrisbanes/skills) (Android and Compose), [`Kotlin/kotlin-agent-skills`](https://github.com/Kotlin/kotlin-agent-skills) (Kotlin), and [`multica-ai/andrej-karpathy-skills`](https://github.com/multica-ai/andrej-karpathy-skills) (general coding discipline). These are third-party repositories that Claude Code fetches on demand; they supplement the Levyra skills but never override the rules in `CLAUDE.md`. Remove the entries to opt out.

Personal overrides belong in `.claude/settings.local.json`, which is git-ignored.

## hooks/session-start.sh

`CLAUDE.md` instructs Claude to verify work with `./gradlew :app:testDebugUnitTest`, `:app:lintRelease`, and `assembleRelease`. All three need an Android SDK, and cloud and CI containers frequently do not have one. The hook probes the environment at session start and reports the JDK, whether an Android SDK is present, whether the JVM-only desktop build is usable, and the current branch and dirty-path count.

When no SDK is found the hook tells Claude to say so plainly rather than claim a Gradle result it cannot produce, and points at `.github/workflows/pr-check.yml` as the authority. The hook always exits 0 and always prints valid JSON, so a probe failure can never break a session.

Run it directly to check its output:

```bash
CLAUDE_PROJECT_DIR="$PWD" ./.claude/hooks/session-start.sh
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
