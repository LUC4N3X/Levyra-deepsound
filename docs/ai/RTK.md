# Levyra RTK and AI Efficiency Setup

## Goal

Levyra uses RTK as a command-output compression layer for supported coding
agents. Repository instructions and skills decide what to inspect, validate,
preserve, and review; RTK only reduces repetitive terminal output before it
reaches agent context.

The integration:

- keeps root/path `AGENTS.md` files as the source of truth;
- exposes `levyra-context-efficiency` and security/domain skills to supported
  runtimes;
- adds project filters in `.rtk/filters.toml`;
- automatically ensures the pinned official RTK build for Claude Code and Codex
  when an active Levyra lifecycle hook runs;
- keeps the earlier instruction-based Codex setup as a fallback when lifecycle
  hooks are unavailable, not yet materialized, untrusted, or disabled;
- never weakens sandbox or approval controls to make tooling bootstrap succeed;
- keeps exact security, signing, checksum, release, and decisive diagnostic
  evidence raw.

## What RTK changes

RTK can compact Gradle, tests, lint, Git/GitHub, broad searches, repeated logs,
CI diagnostics, adb output, dependency reports, and setup output. It measures
command-output reduction, not total billing reduction.

RTK is not test authority. Always verify exit status and final success/failure
markers. Rerun the exact original command raw whenever compact output is
incomplete or ambiguous.

## Pinned automatic ensure

Levyra pins the official `rtk-ai/rtk` revision:

```text
b34be37caf3796b69a50952a28e60e32b5daad43
```

The idempotent ensure scripts are:

```text
Windows PowerShell: scripts/ensure-rtk.ps1 -Quiet
Bash/WSL/Linux/macOS: ./scripts/ensure-rtk.sh --quiet
```

Each script first verifies raw `rtk --version` and `rtk gain`. If both succeed,
nothing is installed. If they fail and Cargo is available, the script installs
the pinned `rtk-ai/rtk` revision with `cargo install --git ... --rev ... --force`
and verifies it again.

If Cargo is unavailable, installation fails, or post-install verification fails,
the ensure script returns a failure and the agent continues with raw commands.
The bootstrap must not fall back to an unverified download or broader machine
permissions.

## Runtime behavior

### Claude Code

The tracked Claude configuration lives under `.agents/claude/`.
`scripts/sync_agent_runtime.py` materializes the ignored native `.claude/`
projection that Claude Code consumes.

`.agents/claude/settings.json` projects to `.claude/settings.json` and registers a
`SessionStart` hook for `startup|resume`. The hook calls canonical
`.agents/claude/hooks/session-start.sh`, which invokes `scripts/ensure-rtk.sh
--quiet` before normal work and injects a short environment note telling Claude
whether RTK is ready. The hook remains fail-open: an RTK bootstrap failure does
not block the Claude session.

Claude also receives repository-specific routing through projected
`.claude/CLAUDE.md`, `.claude/rules/context-efficiency.md`, and the canonical
prompt-submission hook. If the native projection is created only after an
already-running Claude process built its project inventory, start a new session
before treating those files as loaded.

### Codex

The tracked project hook contract lives at `.agents/codex/hooks.json` and is
projected to native `.codex/hooks.json`. Once that native project hook surface is
materialized and trusted, its `SessionStart` hook for `startup|resume` resolves
the Git root, runs the appropriate `scripts/ensure-rtk.*` helper, and exits
successfully even when RTK cannot be installed so Codex can continue raw.

Codex requires a one-time trust review for a new or changed non-managed
project-local command hook. This is a Codex security boundary, not a Levyra
setup step to bypass. The projected hook cannot bootstrap itself on a pristine
clone before `.codex/hooks.json` exists, so root `AGENTS.md` and the setup path
remain the non-circular fallback. Codex skill discovery itself is independent of
that projection because canonical Levyra skills live under `.agents/skills/`.

The previous **instruction-based Codex setup** remains deliberate: root
`AGENTS.md` and `.agents/rules/levyra-workspace.md` still tell Codex to run the
ensure helper before noisy work if project hooks are unavailable, disabled, or
not yet trusted. The broader setup scripts may also initialize RTK guidance with
`rtk init -g --codex`.

### Google Antigravity

Antigravity reads `.agents/rules/levyra-workspace.md` and shared skills under
`.agents/skills/`. On a shell-capable non-trivial task it runs the platform
ensure script automatically before noisy work. The broader setup scripts can
also initialize RTK's repository-local Antigravity integration.

### OpenCode and compatible runtimes

Compatible shell-capable runtimes follow root `AGENTS.md` and
`levyra-context-efficiency`: ensure RTK once when needed, then use it selectively.
The broader setup scripts still initialize supported RTK integrations for
runtimes they detect.

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
`docs/ai/CODEX_SECURITY.md`.

## Automatic repository discovery

Tracked sources:

```text
AGENTS.md
app/AGENTS.md
desktop/AGENTS.md
.github/AGENTS.md
docs/AGENTS.md
.agents/codex/hooks.json
.agents/claude/CLAUDE.md
.agents/claude/settings.json
.agents/claude/hooks/session-start.sh
.agents/claude/rules/context-efficiency.md
.agents/rules/levyra-workspace.md
.agents/skills/levyra-context-efficiency/SKILL.md
.agents/skills/levyra-security-review/SKILL.md
.rtk/filters.toml
scripts/sync_agent_runtime.py
scripts/ensure-rtk.ps1
scripts/ensure-rtk.sh
```

Generated native surfaces consumed by Claude/Codex when materialized:

```text
.claude/CLAUDE.md
.claude/settings.json
.claude/rules/
.claude/skills/
.codex/hooks.json
.codex/config.toml
```

Restart the coding agent or begin a new conversation after pulling changes to
instructions, rules, skills, hooks, or plugins when the runtime needs to rebuild
its project inventory. Codex may request trust review when a project-local
command hook is new or its definition changes.

## Broader setup and repair

The automatic session ensure is the normal path after native lifecycle hooks are
active. The broader setup scripts remain the deterministic path for first
projection, provisioning, repairing integrations, plugin setup, or validating
the complete AI environment.

Windows:

```powershell
.\scripts\setup-ai.ps1 -DryRun
.\scripts\setup-ai.ps1
.\scripts\setup-ai.ps1 -InstallRtk
.\scripts\setup-ai.ps1 -InstallRtk -Plugins
```

Linux/macOS/WSL:

```bash
./scripts/setup-ai.sh --dry-run
./scripts/setup-ai.sh
./scripts/setup-ai.sh --install-rtk
./scripts/setup-ai.sh --install-rtk --plugins
```

Use `--skip-hooks` or `-SkipHooks` when only validation/plugin setup is needed.
Missing required validation tooling is a blocked setup, not a pass.

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
rtk adb logcat -d -t 400
rtk adb -s <serial> logcat -d -t 400
rtk summary adb shell dumpsys <service>
```

The Claude Code hook (`rtk hook claude`) rewrites standalone commands and `&&`
chains only. A command piped into another one, such as `git log --stat | head`,
runs raw and is not compressed or tracked. Prefer the command's own bounding
flags over `| head`, or prefix the first pipeline stage with `rtk` explicitly.
`rtk hook check '<command>'` shows how the hook would rewrite a command.

Keep commands raw when exact evidence matters:

```text
gradlew.bat --stacktrace <task>
./gradlew --stacktrace <task>
git diff --check
sha256sum <artifact>
certutil -hashfile <artifact> SHA256
adb exec-out screencap -p > screenshot.png
```

Security scans and decisive reproductions also remain raw.

## Project-local filters

`.rtk/filters.toml` adds handling for repository-specific agent validators,
CodeRabbit review output, bounded `adb logcat`, and setup-script runs. Gradle,
Git, GitHub CLI, common tests, lint, Docker, searches, and standard logs continue
to use RTK's built-in handlers.

The logcat filter is intentionally narrow. Agents invoke it explicitly with
`rtk adb logcat ...` or `rtk adb -s <serial> logcat ...`; short deterministic ADB
queries stay raw, while large textual commands without a dedicated filter use
`rtk summary`. Binary or redirected payloads such as `adb exec-out screencap -p`
never go through a text filter.

### Trusting project filters

RTK custom filters are gated by an explicit local trust decision. A fresh clone
or any content change to `.rtk/filters.toml` invalidates the previously trusted
hash, so RTK skips the project filters until they are reviewed again.

From the Levyra repository root run:

```text
rtk trust
```

Use `rtk trust --yes` only after reviewing the current `.rtk/filters.toml` and
when a non-interactive trust action is deliberately intended. Do not add an
automatic lifecycle hook that silently trusts future filter changes; the hash
review is a security boundary, not setup noise.

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
to diagnose failures correctly. In particular, do not wrap tiny ADB commands
only to improve the adoption metric.

## Failure recovery

1. If automatic ensure fails, continue raw and report it once.
2. Rerun the exact command without RTK when compact output is incomplete.
3. Add stacktrace or debug detail only when needed.
4. Verify original exit status and success/failure markers.
5. Never treat empty filtered output as proof of success.
6. Keep raw security and release evidence in the final report.

## Validation

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_codex_hooks.py
python3 scripts/ai_quality_gate.py --profile fast
```

The AI quality gate also runs repository script tests that protect the Codex
hook contract and persistent-memory integration. PowerShell and Bash syntax are
checked when those files are part of the changed set and the required shell is
available in the validation environment.

## Levyra RTK Enhanced: reversible output context

`scripts/levyra_context_optimizer.py` provides an **optional**, local text-output
wrapper alongside RTK. It is **not** automatically inserted into every Claude,
Codex or Antigravity tool call; use it selectively for lengthy, finite commands
that RTK does not already compact. No model/provider settings are changed.

### Safety and fidelity

- **RTK first:** use RTK for supported commands. Do not wrap RTK's output again:
  the optimizer only recognizes its own `#levyra-raw:` or `[Levyra Context:`
  markers, not every format produced by RTK.
- **Failures first:** commands with nonzero exit status are printed uncompressed,
  with the original process exit status. The `run` wrapper combines stdout and
  stderr in their observed order, and decodes text using UTF-8 with replacement
  for invalid bytes. Use native commands when byte-level output, separate streams
  or interactive streaming are required.
- **Recovery:** successful command output is compressed only if the decoded
  original is saved in the bounded local cache and the **final** output, including
  its retrieval marker, saves at least 12% of estimated lexical tokens.
  Original text can be retrieved while its entry remains in the cache.
- **Fail open:** when output is too large for the 25 MB cache, storage fails,
  or savings are insufficient, it is displayed uncompressed. Cache entries
  are evicted on a least-recently-used basis, up to 150 entries.
- **Privacy:** cache files under `.levyra-tools/context-cache/` are local,
  gitignored plaintext. Never cache credentials, `.env` or other sensitive
  command output. For such commands, do not use the wrapper, or explicitly
  choose `--no-cache`. No credentials or telemetry are sent to Headroom.
- **No unsafe retry:** reading cached text does not rerun a state-changing
  command. If an entry has been evicted, do not rerun mutating commands just
  to recover the log.

### Optional bounded command execution

Inspired by [ChrisTitusTech/agent-proxy](https://github.com/ChrisTitusTech/agent-proxy),
the `run` wrapper supports a **user-selected wall-clock timeout**. When it
expires, the launched command is stopped, available combined output stays
visible without filtering, the process is never retried, and the wrapper
returns exit code **124**. Linux/macOS use an isolated process group; on
Windows, the wrapper attempts process-tree cleanup with `taskkill /T /F`.

No deadline is imposed by default. Specify one only for a finite command
whose duration is known; not for an interactive or long-running agent.
The wrapper still buffers text output in memory and should not handle
unbounded log streams, binary output, or sensitive data.

```powershell
python scripts/levyra_context_optimizer.py run --timeout 300 -- .\gradlew.bat testDebugUnitTest
```

### Read-only agent readiness

Inspired by non-spawning readiness checks in agent-proxy and the portable
configuration in [ChrisTitusTech/titus-ai](https://github.com/ChrisTitusTech/titus-ai),
Levyra now includes a toolchain doctor:

```bash
python3 scripts/agent_runtime_doctor.py
python3 scripts/agent_runtime_doctor.py --json
python3 scripts/agent_runtime_doctor.py --strict
```

It checks tracked repository agent contracts and optional executable names:
RTK, Claude, Codex, Antigravity, Headroom, and Grok. It **does not** run
agents, read credentials, verify logins or subscriptions, change models,
start servers, or install dependencies.

Missing optional tools are informational. `--strict` fails only for missing
required repository contracts or an unsupported Python version. This is a
configuration check, **not** live inference readiness.

### Commands

On Linux/macOS (Bash), for ordinary high-volume text output:

```bash
python3 scripts/levyra_context_optimizer.py run -- ./gradlew help
python3 scripts/levyra_context_optimizer.py run -- git diff HEAD~1
python3 scripts/levyra_context_optimizer.py run --no-cache -- git status
```

On Windows (PowerShell), run the same tool with `python` and native Gradle:

```powershell
python scripts/levyra_context_optimizer.py run -- .\gradlew.bat help
python scripts/levyra_context_optimizer.py run -- git diff HEAD~1
```

Retrieve all or part of a cached original output:

```text
python scripts/levyra_context_optimizer.py retrieve <cache_id>
python scripts/levyra_context_optimizer.py retrieve <cache_id> --lines 100:200
```

Inspect and clear the local cache:

```bash
python scripts/levyra_context_optimizer.py stats
python scripts/levyra_context_optimizer.py clear
```

The `process` subcommand accepts already-captured stdin. If it is used in a
pipeline, explicitly preserve the *actual upstream* exit status; shell `$?`
or `$LASTEXITCODE` expanded inside a running pipeline is **not** a reliable way
to do so. Prefer `run -- <command>` for exit-code safety.

### Benchmark interpretation

`scripts/benchmark_context_optimization.py` tests representative scenarios
plus a few live command/status paths. Reported token counts are **heuristic
lexical estimates**, not model tokenizer or billed-token measurements; no
improvement in agent coding quality has been demonstrated by this benchmark.

### Optional Headroom

`scripts/headroom_adapter.py` is an opt-in CLI/tool availability adapter.
It does not automatically activate Headroom compression, attach a background
proxy, or register an MCP server. See `docs/ai/HEADROOM.md` for accurate scope.

## Attribution

The workflow incorporates selected ideas from `ChrisTitusTech/titus-ai`, notably
portable scoped agent configuration, deterministic validation, readiness review,
and conservative tool bootstrap. Levyra keeps its own project-specific
architecture, least-privilege boundaries, skills, planning hierarchy, and
quality gates. RTK is developed by `rtk-ai/rtk` and distributed under
Apache-2.0.
