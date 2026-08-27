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
```

Keep commands raw when exact evidence matters:

```text
gradlew.bat --stacktrace <task>
./gradlew --stacktrace <task>
git diff --check
sha256sum <artifact>
certutil -hashfile <artifact> SHA256
```

Security scans and decisive reproductions also remain raw.

## Project-local filters

`.rtk/filters.toml` adds handling for repository-specific agent validators,
CodeRabbit review output, bounded `adb logcat`, and setup-script runs. Gradle,
Git, GitHub CLI, common tests, lint, Docker, searches, and standard logs continue
to use RTK's built-in handlers.

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

## Attribution

The workflow incorporates selected ideas from `ChrisTitusTech/titus-ai`, notably
portable scoped agent configuration, deterministic validation, readiness review,
and conservative tool bootstrap. Levyra keeps its own project-specific
architecture, least-privilege boundaries, skills, planning hierarchy, and
quality gates. RTK is developed by `rtk-ai/rtk` and distributed under
Apache-2.0.
