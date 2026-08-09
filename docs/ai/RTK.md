# Levyra RTK and AI Efficiency Setup

## Goal

Levyra uses RTK as a command-output compression layer for supported coding
agents. Repository
instructions and skills decide what to inspect, validate, preserve, and review;
RTK only reduces repetitive terminal output before it reaches agent context.

The integration:

- keeps root/path `AGENTS.md` files as the source of truth;
- exposes `levyra-context-efficiency` and `levyra-security-review` to supported
  runtimes;
- adds project filters in `.rtk/filters.toml`;
- detects installed Codex, Claude Code, OpenCode, and Antigravity integrations;
- automatically bootstraps the pinned official RTK build for coding agents;
- keeps every other executable and plugin installation opt-in;
- excludes Ollama and other local-model profiles;
- keeps exact security, signing, checksum, and release evidence raw.

## What RTK changes

RTK can compact Gradle, tests, lint, Git/GitHub, broad searches, repeated logs,
CI diagnostics, adb output, dependency reports, and setup output. It measures
command-output reduction, not total billing reduction.

RTK is not test authority. Always verify exit status and final success/failure
markers. Rerun the exact original command raw whenever compact output is
incomplete or ambiguous.

## Runtime behavior

### Codex

Codex uses an **instruction-based Codex setup**. Running:

```text
rtk init -g --codex
```

installs guidance that teaches Codex when to invoke RTK. It does not install a
transparent native shell-rewrite hook. Codex follows those instructions and
selects RTK commands explicitly.

### Claude Code

Claude Code supports an RTK hook initialized by the setup scripts when the
`claude` command is present. Claude also receives repository-specific routing
through `.claude/rules/context-efficiency.md` and its prompt-submission hook.

### Google Antigravity

The setup scripts initialize RTK's repository-local Antigravity integration.
Antigravity reads `.agents/rules/levyra-workspace.md` and the shared skills under
`.agents/skills/`.

### OpenCode and compatible runtimes

When `opencode` is detected, the setup scripts initialize its supported RTK
integration. Every runtime must still follow Levyra's root/path instructions,
domain skills, validation, review, and publication boundaries.

## Security routing across runtimes

RTK must never compact exact exploit evidence, vulnerability validation output,
secret scans, hashes, signatures, signing evidence, or security reproductions.
Those commands remain raw.

Security-sensitive work automatically loads `levyra-security-review` in Codex,
Claude Code, ChatGPT Project instructions, and Antigravity. The shared method is:

```text
threat model
→ identification
→ safe validation
→ minimal remediation
→ human review
→ revalidation
```

Codex Security may be enabled through its official setup and used alongside the
same repository-native skill. It is intentionally documented separately in
`docs/ai/CODEX_SECURITY.md`; the setup scripts do not invent or install an
unverified plugin identifier.

## Automatic repository discovery

```text
AGENTS.md
app/AGENTS.md
desktop/AGENTS.md
.github/AGENTS.md
docs/AGENTS.md
.agents/rules/levyra-workspace.md
.agents/skills/levyra-context-efficiency/SKILL.md
.agents/skills/levyra-security-review/SKILL.md
.claude/rules/context-efficiency.md
.claude/rules/security.md
.rtk/filters.toml
```

Restart the coding agent or begin a new conversation after pulling changes to
instructions, rules, skills, hooks, or plugins.

## Automatic agent bootstrap

Root `AGENTS.md` gives supported coding agents standing permission to verify
RTK before their first noisy shell task. Agents run `rtk --version` and
`rtk gain` raw. If RTK is missing, or if another project named `rtk` is on the
path, the agent runs the platform setup script with `-InstallRtk` or
`--install-rtk`.

The scripts install `rtk-ai/rtk` from commit
`b34be37caf3796b69a50952a28e60e32b5daad43`, the revision published for the
official `v0.45.0` release, through Cargo, verify that `rtk gain` is available,
and configure only detected supported runtimes. They do not silently install
plugins. If Cargo is unavailable or the install fails, the agent reports the
blocked bootstrap and continues with raw commands; it must not fall back to an
unverified download.

## Windows setup

```powershell
.\scripts\setup-ai.ps1 -DryRun
.\scripts\setup-ai.ps1
.\scripts\setup-ai.ps1 -InstallRtk
.\scripts\setup-ai.ps1 -InstallRtk -Plugins
```

The script detects and validates the correct RTK, installs the pinned revision
when an agent invokes the authorized install flag, configures supported
runtimes, installs verified `.agents/config/codex-plugins.txt` entries when
requested, and runs both repository validators.

Missing Python blocks validation and returns a nonzero exit status. Setup is not
reported complete when required validation cannot run.

## Linux and macOS setup

```bash
chmod +x scripts/setup-ai.sh
./scripts/setup-ai.sh --dry-run
./scripts/setup-ai.sh
./scripts/setup-ai.sh --install-rtk
./scripts/setup-ai.sh --install-rtk --plugins
```

Use `--skip-hooks` or `-SkipHooks` when only validation/plugin setup is needed.

## Verified plugin manifest

`.agents/config/codex-plugins.txt` contains only verified CLI-installable
identifiers. The current opt-in entry is:

```text
superpowers@openai-curated
```

Codex Security remains a separate official setup because no stable CLI manifest
identifier is assumed by this repository. See `docs/ai/CODEX_SECURITY.md`.

## Recommended routing

Use RTK for noisy commands:

```text
rtk gradlew :app:testDebugUnitTest
rtk gradlew :app:lintRelease
rtk gradlew --no-daemon --no-configuration-cache assembleRelease
rtk git status
rtk git diff
rtk git log -n 20
rtk gh pr view <number>
rtk gh run list
rtk grep <pattern> <path>
rtk find <pattern> <path>
rtk log <file>
rtk test <command>
rtk err <command>
```

Keep commands raw when exact evidence matters:

```text
gradlew.bat --stacktrace <task>
./gradlew --stacktrace <task>
git diff --check
sha256sum <artifact>
certutil -hashfile <artifact> SHA256
```

Security scans and reproductions also remain raw.

## Project-local filters

`.rtk/filters.toml` adds handling for:

- `scripts/validate_agent_config.py`;
- local CodeRabbit review output;
- bounded `adb logcat` output;
- direct and interpreter-prefixed setup-script runs.

Gradle, Git, GitHub CLI, common tests, lint, Docker, searches, and standard logs
continue to use RTK's built-in handlers.

## Measuring real savings

```text
rtk gain
rtk gain --daily
rtk gain --history
rtk gain --graph
rtk discover --all --since 7
rtk session
```

Do not add filters merely to inflate a percentage. Preserve enough information
to diagnose failures correctly.

## Failure recovery

1. Rerun the exact command without RTK when output is incomplete.
2. Add stacktrace or debug detail only when needed.
3. Verify the original exit status and success/failure marker.
4. Do not treat empty filtered output as a passing check.
5. Keep raw security and release evidence in the final report.

## Validation

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

The second validator parses `.rtk/filters.toml` as TOML, tests documented setup
command matching, verifies fail-closed setup behavior, cross-runtime security
routing, dependency-review configuration, plugin scope, and the absence of
unapproved local-model profiles.

## Attribution

The workflow is inspired by `ChrisTitusTech/titus-ai`. RTK is developed by
`rtk-ai/rtk` and distributed under Apache-2.0. Levyra keeps its own project-
specific instructions, security boundaries, domain skills, and validation.
