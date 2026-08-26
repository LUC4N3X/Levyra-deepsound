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
│   ├── session-start.sh
│   └── user-prompt-submit.sh
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
    ├── levyra-real-engineering/SKILL.md
    ├── levyra-compose/SKILL.md
    ├── levyra-database/SKILL.md
    ├── levyra-extractor/SKILL.md
    ├── levyra-motion-artwork/SKILL.md
    ├── levyra-player/SKILL.md
    ├── levyra-pr-review/SKILL.md
    ├── levyra-release-check/SKILL.md
    └── levyra-security-review/SKILL.md
```

## settings.json

`settings.json` is checked in and applies to everyone who opens the repository in Claude Code.

- `permissions.allow` pre-approves the read-only git commands and the Gradle verification tasks from `CLAUDE.md`, so routine checks do not stop for a prompt. The git entries are deliberately narrow: `git branch` is allowed only as `--show-current` and `--list`, since a broader wildcard would also pre-approve `git branch -D`, `-M`, and `-f`, which mutate or delete refs.
- `permissions.deny` blocks reads and writes of `local.properties`, keystores, and `.env` files. This is a guardrail, not a substitute for the release-safety rules in `CLAUDE.md`.
- `hooks.SessionStart` runs `hooks/session-start.sh`; `hooks.UserPromptSubmit` runs `hooks/user-prompt-submit.sh`.
- `extraKnownMarketplaces` opts this repository into three external skill marketplaces: [`chrisbanes/skills`](https://github.com/chrisbanes/skills) (Android and Compose), [`Kotlin/kotlin-agent-skills`](https://github.com/Kotlin/kotlin-agent-skills) (Kotlin), and [`multica-ai/andrej-karpathy-skills`](https://github.com/multica-ai/andrej-karpathy-skills) (general coding discipline).
- `enabledPlugins` also project-enables `mattpocock-skills@claude-plugins-official`, using Claude Code's official marketplace. The upstream skills supplement Levyra; the local `levyra-real-engineering` bridge and repository rules decide when and how they are used.

Personal overrides belong in `.claude/settings.local.json`, which is git-ignored.

## hooks/session-start.sh

`CLAUDE.md` instructs Claude to verify work with `./gradlew :app:testDebugUnitTest`, `:app:lintRelease`, and `assembleRelease`. All three need an Android SDK, and cloud and CI containers frequently do not have one. The hook probes the environment at session start and reports the JDK, the state of the Android SDK, whether the JVM-only desktop build is usable, and the current branch and dirty-path count.

A `platforms/` directory alone is not accepted as proof of a usable SDK, because a partial or stale install still fails at Gradle configuration time. The hook reads `compileSdk` from `app/build.gradle.kts` and requires both `platforms/android-<compileSdk>/android.jar` and a `build-tools` install, so it distinguishes three states: usable, incomplete (naming the missing packages), and absent. Reading `compileSdk` from the build file keeps the check correct when that value is bumped.

Even in the usable case the hook says the tasks *should configure* — a precondition, not a result. Claude must still run the task and report only what actually ran. When the SDK is unusable the hook says so plainly and points at `.github/workflows/pr-check.yml` as the authority.

The hook always exits 0 and always prints valid JSON, including when it cannot enter the project directory, so a probe failure can never break a session.

Run it directly to check its output:

```bash
CLAUDE_PROJECT_DIR="$PWD" ./.claude/hooks/session-start.sh
```

## hooks/user-prompt-submit.sh

Three loading mechanisms behave differently, and only two of them are automatic:

| Layer | Loads |
| --- | --- |
| `CLAUDE.md` | Always, every session |
| `rules/*.md` | Automatically, when a file matching the rule's `paths:` is in play |
| `skills/*/SKILL.md` | Only the `description` is visible up front; the body loads when the skill is invoked |

That third row is the gap. A description is a hint the model may or may not act on, so a skill could be skipped on exactly the request it was written for. This hook closes the gap: it matches each incoming request against the topics the skills cover and states, as an instruction, which ones to invoke before editing. Patterns cover Italian as well as English terms. Several skills can match at once — a non-trivial player feature can route both `levyra-real-engineering` and `levyra-player`.

For `levyra-real-engineering`, the local bridge reads the canonical adapter under `.agents/skills/` and then invokes the exact Matt Pocock stage from the official plugin when available. Tiny unambiguous changes deliberately bypass the full clarify/spec/tickets pipeline.

The hook stays silent when nothing matches, when the payload is unreadable, and when `python3` is absent, so an unrelated request costs nothing. It always exits 0.

It also stays silent on automated payloads — GitHub webhook activity and wrapped external data arrive as user turns but are bot prose, not requests. Without that guard a CodeRabbit rate-limit notice matches `review`, `release`, `security`, and `ui` at once and routes four skills for a message asking no work at all.

`CLAUDE.md` carries the same routing table, so the behavior degrades to documented-but-unenforced rather than disappearing if the hook cannot run.

To see what a given request would route to:

```bash
printf '{"prompt":"design a new playback feature across multiple modules"}' \
  | ./.claude/hooks/user-prompt-submit.sh
```

## Usage

Start Claude Code from the repository root. Use `/context` to confirm that `.claude/CLAUDE.md` and the unconditional rules loaded.

Skills are invoked automatically via the routing table above. Invoke one by hand when you want it regardless of wording:

- `/levyra-real-engineering`
- `/levyra-player`
- `/levyra-extractor`
- `/levyra-motion-artwork`
- `/levyra-database`
- `/levyra-compose`
- `/levyra-security-review`
- `/levyra-pr-review`
- `/levyra-release-check`

The upstream plugin stages are namespaced by Claude Code. Prefer the local `/levyra-real-engineering` entry point for Levyra work so repository precedence, issue publication rules, and quality gates are applied before an upstream stage runs.

Ask Claude to use `levyra-android-developer` for implementation work or `levyra-reviewer` for a read-only review.

If this is the first time the `agents/` or `skills/` directory exists during an already-running Claude Code session, restart the session once so every entry is discovered. Reload plugins after Claude reports an upstream plugin update.

## Maintenance

Keep `.claude/CLAUDE.md` concise and reserve multi-step procedures for skills. Update the matching rule whenever a review identifies a project-specific mistake that should not recur.
