# Levyra RTK and AI Efficiency Setup

## Goal

Levyra uses RTK as an optional command-output compression layer for coding
agents. RTK filters repetitive shell output before it reaches the model context,
while Levyra's repository-local instructions and skills decide what must be
read, validated, reviewed, and preserved.

This integration is intentionally repository-specific:

- it keeps root and path-specific `AGENTS.md` as the source of truth;
- it automatically exposes `levyra-context-efficiency` with the other native
  skills under `.agents/skills/`;
- it adds project-local filters in `.rtk/filters.toml`;
- it detects installed supported coding agents through setup scripts;
- it keeps plugin installation opt-in through `codex-plugins.txt`;
- it does not install or configure Ollama or any local model profile;
- it does not enable unrestricted sandboxing or bypass owner approvals.

## What RTK changes

RTK wraps supported terminal commands and sends a compact representation to the
coding agent. Typical examples include:

- collapsing successful Gradle tasks while preserving failures and summaries;
- showing failed tests instead of every passing test;
- compacting Git status, logs, diffs, and GitHub CLI output;
- grouping broad search results by file;
- deduplicating repeated log lines;
- bounding dependency, Docker, CI, adb, and server output.

RTK measures reductions in command output. Those numbers are not an equal
reduction in the total model bill because prompts, system instructions,
conversation history, tool metadata, and generated output remain separate.

## Automatic repository discovery

Supported agents discover Levyra through the existing repository structure:

```text
AGENTS.md
app/AGENTS.md
desktop/AGENTS.md
.github/AGENTS.md
docs/AGENTS.md
.agents/rules/levyra-workspace.md
.agents/skills/levyra-context-efficiency/SKILL.md
.claude/rules/context-efficiency.md
.rtk/filters.toml
```

The context-efficiency skill description is deliberately broad enough to be
automatically selected for builds, tests, lint, logs, searches, CI, Git/GitHub,
agent setup, and other high-output work. It must be combined with the relevant
product-domain skill, such as `levyra-player`, `levyra-extractor`,
`levyra-compose`, `levyra-desktop`, or `levyra-ci-workflows`.

Claude Code receives an additional lightweight rule under `.claude/rules/` that
links back to the canonical Levyra skill instead of maintaining a second
workflow. Antigravity receives the same routing through
`.agents/rules/levyra-workspace.md`.

After pulling changes to instructions, skills, rules, plugins, or hooks, restart
the coding agent or begin a new conversation so its inventory is rebuilt.

## Windows setup

From the repository root in PowerShell:

```powershell
.\scripts\setup-ai.ps1 -DryRun
.\scripts\setup-ai.ps1
```

When RTK is missing and Cargo is installed:

```powershell
.\scripts\setup-ai.ps1 -InstallRtk
```

To also install the explicitly listed Codex plugins:

```powershell
.\scripts\setup-ai.ps1 -InstallRtk -Plugins
```

The script:

1. detects RTK;
2. optionally installs RTK from `rtk-ai/rtk` through Cargo;
3. initializes the Codex hook when `codex` is detected;
4. initializes Claude Code when `claude` is detected;
5. initializes OpenCode when `opencode` is detected;
6. initializes the repository-local Antigravity integration;
7. optionally installs `codex-plugins.txt` entries;
8. runs the general agent-configuration validator;
9. runs the RTK/AI-efficiency validator;
10. prints the restart and measurement commands.

Use `-SkipHooks` when only validation or plugin installation is required.

RTK also publishes official pre-built Windows binaries. A manually installed
`rtk.exe` must be placed on `PATH` before running the setup script.

## Linux and macOS setup

From the repository root:

```bash
chmod +x scripts/setup-ai.sh
./scripts/setup-ai.sh --dry-run
./scripts/setup-ai.sh
```

When RTK is missing and Cargo is installed:

```bash
./scripts/setup-ai.sh --install-rtk
```

To install the listed Codex plugins as well:

```bash
./scripts/setup-ai.sh --install-rtk --plugins
```

Use `--skip-hooks` when hook initialization is not wanted.

## Plugin manifest

`codex-plugins.txt` contains the repository's recommended opt-in Codex plugins.
The initial selection is:

```text
superpowers@openai-curated
```

The plugin is intended to complement Levyra's project-specific planning,
debugging, testing, and delivery skills. It does not replace repository
requirements, domain skills, current code/tests, independent review, or owner
approval. Plugin availability and permissions still depend on the active Codex
or workspace configuration.

## Recommended command routing

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

On Windows, RTK's Gradle integration uses `gradlew.bat` when the wrapper is
present.

Keep commands raw when exact or complete evidence matters:

```text
gradlew.bat --stacktrace <task>
./gradlew --stacktrace <task>
git diff --check
sha256sum <artifact>
certutil -hashfile <artifact> SHA256
```

Also rerun raw whenever compact output is insufficient to identify a failure.

## Project-local filters

`.rtk/filters.toml` adds Levyra-specific handling for:

- `scripts/validate_agent_config.py`;
- local CodeRabbit review output;
- bounded `adb logcat` output;
- setup-script dry runs.

Gradle, Git, GitHub CLI, tests, lint, Docker, broad searches, and common logs use
RTK's dedicated built-in handlers. Project-local filters should not replace
those handlers unless a verified Levyra-specific need exists.

## Measuring real savings

After several coding sessions:

```text
rtk gain
rtk gain --daily
rtk gain --history
rtk gain --graph
rtk discover --all --since 7
rtk session
```

Use the results to find commands still producing avoidable output. Do not add a
filter merely to improve a percentage: retain enough evidence to diagnose
failures correctly.

## Failure recovery

1. Rerun the exact command without RTK when output is incomplete.
2. Add `--stacktrace`, `--full-stacktrace`, `--info`, or `--debug` only when
   needed.
3. Check the original exit status and success/failure marker.
4. Do not treat empty filtered output as a passing check.
5. Keep the raw command and relevant evidence in the final report when a failure
   was diagnosed outside RTK.

## Security and permission boundaries

The Levyra integration deliberately does not copy a global
`danger-full-access` configuration or broad automatic command allowlist.

RTK and Superpowers must not:

- expose tokens, cookies, keystores, passwords, signing material, private URLs,
  `.env`, or `local.properties`;
- weaken URL, redirect, MIME, checksum, signature, permission, or release
  validation;
- infer permission to commit, push, open a PR, merge, tag, release, upload, or
  change repository settings;
- replace CI, independent review, device testing, Android Auto checks, release
  checks, or owner decisions.

## Validation

After changing RTK configuration, setup scripts, plugins, native skills, or AI
documentation:

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

On Windows:

```powershell
py scripts\validate_agent_config.py
py scripts\validate_ai_efficiency.py
```

The first validator checks Levyra's shared instruction and skill inventory. The
second checks RTK documentation, filters, setup scripts, plugin scope,
cross-runtime discovery, CI integration, and the absence of unapproved local-
model profiles. The PR workflow runs both validators.

## Attribution

The workflow is inspired by the portable configuration and context-efficiency
approach used by `ChrisTitusTech/titus-ai`. RTK is developed separately by
`rtk-ai/rtk` and distributed under the Apache-2.0 license. Levyra keeps its own
project-specific wording, safety boundaries, domain skills, and validation.
