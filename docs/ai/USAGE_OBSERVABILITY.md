# AI Usage Observability

Levyra keeps token observability and Claude rate-limit visibility project-scoped.
The integration is tooling-only: it does not change Android or Desktop runtime
dependencies, playback behavior, application telemetry, or release artifacts.

## Components

- **RTK** remains the output-reduction layer already managed by Levyra.
- **CodeBurn 0.9.20** reads local coding-agent session data and reports usage,
  cost, model, tool, retry, and waste patterns. Levyra runs it through pinned
  `npx` wrappers and always adds the `Levyra-deepsound` project filter.
- **Headroom v0.3.0** provides Claude Code's status line for model, context,
  spend, and usage headroom. Levyra installs its verified release binary under
  `/.levyra-tools/headroom/` and does not let the upstream installer rewrite
  global Claude settings or the user's PATH.

The project-local tool directory is ignored by Git. No downloaded binary is
committed.

## Setup

Windows:

```powershell
.\scripts\setup-usage-tools.ps1 -DryRun
.\scripts\setup-usage-tools.ps1
```

Linux/macOS:

```bash
bash ./scripts/setup-usage-tools.sh --dry-run
bash ./scripts/setup-usage-tools.sh
```

The setup downloads the pinned Headroom installer from its matching release tag,
uses Headroom's `NoWire`/`--no-wire` and `NoPath`/`--no-path` modes, installs the
binary only inside Levyra's ignored tool directory, then verifies the binary.
If `npx` is available it also verifies the pinned CodeBurn package without a
global npm installation.

Claude's tracked source of truth remains `.agents/claude/settings.json`. Its
`statusLine` calls `scripts/claude-statusline.sh`, which forwards the status-line
payload to Levyra's local Headroom binary when present and otherwise exits
cleanly. Run the normal Levyra runtime projection after changing tracked Claude
settings:

```powershell
.\scripts\setup-ai.ps1
```

or:

```bash
./scripts/setup-ai.sh
```

## CodeBurn

Windows:

```powershell
.\scripts\codeburn-levyra.ps1
.\scripts\codeburn-levyra.ps1 optimize --provider claude
.\scripts\codeburn-levyra.ps1 models --by-agent
```

Linux/macOS:

```bash
bash ./scripts/codeburn-levyra.sh
bash ./scripts/codeburn-levyra.sh optimize --provider claude
bash ./scripts/codeburn-levyra.sh models --by-agent
```

With no arguments the wrapper shows the last week. Explicit commands remain
scoped to Levyra by appending `--project Levyra-deepsound`.

Treat `codeburn optimize` findings as evidence to review, not as automatic
authorization to rewrite Levyra configuration. Do not use `--apply` until each
suggested change has been checked against current `AGENTS.md`, the applicable
skills, and Levyra's validation rules.

CodeBurn reads agent session files from the local machine. The Levyra wrapper
does not add an API key, proxy, telemetry hook, or MCP server. CodeBurn may fetch
public model-pricing metadata used for cost estimates.

## Headroom

Headroom is deliberately not wired through its global `--install` behavior.
Levyra owns the status line in tracked project settings so a setup run cannot
replace another project's or the user's global Claude status line.

If the local binary is absent, Claude Code continues without the Headroom line.
Re-run the project setup to restore it:

```powershell
.\scripts\setup-usage-tools.ps1
```

The status line refreshes every 10 seconds. Headroom can still show its core
context and limit information without optional spend tooling; upstream optional
dependencies determine which extra spend segments are available.

## Validation

Run:

```bash
python3 scripts/validate_usage_observability.py
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

For a PR, also run Levyra's required full quality gate and inspect the complete
final diff. A missing Node.js/npm, network connection, Python runtime, or
platform-compatible Headroom binary is `BLOCKED`, not `PASS`.
