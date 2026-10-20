# Optional Headroom Integration in Levyra

## Overview

Levyra includes an optional, zero-daemon adapter for [Headroom](https://github.com/headroomlabs-ai/headroom), inspired by solutions explored in Free Claude Code. Headroom specializes in structural AST-aware diffing, code metrics, and Model Context Protocol (MCP) integrations.

In accordance with Levyra's strict engineering rules:
- **No background resident daemons**: The integration does not run persistent background proxy servers or services.
- **Optional and reversible**: Disabled by default. The adapter probes tool availability and can request a structural diff. Existing RTK/native command routes remain independent of Headroom.
- **Local & Git-clean Configuration**: Configuration state is stored locally in `.levyra-tools/headroom.json` (gitignored). Enabling or disabling Headroom never pollutes the shared Git working tree.
- **No model or provider tampering**: Does not alter AI models, endpoints, credentials, or sandboxes.
- **Diagnostic evidence preservation**: The Headroom adapter does not rewrite compiler or test output; Levyra's separate optimizer uses conservative filtering with raw-output fallback.

## Architecture

Headroom operates alongside Levyra's native context infrastructure:

```text
┌────────────────────────────────────────────────────────┐
│                   Coding Agent                         │
│           (Claude Code / Codex / Antigravity)          │
└──────────────────────────┬─────────────────────────────┘
                           │
             ┌─────────────┴─────────────┐
             ▼                           ▼
┌────────────────────────┐  ┌────────────────────────┐
│  Levyra RTK Filter     │  │  Levyra Context        │
│  (.rtk/filters.toml)   │  │  Optimizer (CCR Cache) │
│  - fast terminal noise │  │  - sha256 raw cache    │
│  - standard commands   │  │  - on-demand retrieval │
└────────────┬───────────┘  └────────────┬───────────┘
             │                           │
             └─────────────┬─────────────┘
                           ▼
            ┌─────────────────────────────┐
            │   Optional Headroom CLI     │
            │  (.levyra-tools/headroom)   │
            │  - difftastic AST diffing   │
            │  - scc code statistics     │
            │  - ast-grep query engine    │
            └─────────────────────────────┘
```

### Why Levyra Does Not Force Full HTTP Proxying

Headroom's full proxy server (`headroom mcp serve`) by default expects an active HTTP proxy daemon (`http://127.0.0.1:8787`). Forcing a persistent proxy daemon introduces several risks in local agent workflows:
1. Zombie background processes that survive session termination.
2. Fragile network routing and localhost port collisions.
3. Silent interception of credentials or request payloads.

Instead, Levyra adopts:
1. **Repository-Native CCR**: Implemented in `scripts/levyra_context_optimizer.py` as a fast, stateless local cache that requires no daemon.
2. **Adapter Interface**: `scripts/headroom_adapter.py` probes Headroom CLI capabilities, supports optional structural diffs, and prints a *suggested* MCP configuration; it does not register or start the server. `headroom` itself must be installed separately.

## Managing the Integration

### Check Status & Health

```bash
python scripts/headroom_adapter.py status
python scripts/headroom_adapter.py doctor
```

Output details whether Headroom is installed, detected version, configuration status (`.levyra-tools/headroom.json`), bundled tools (`difftastic`, `scc`, `ast-grep`), and real MCP availability.

### Enabling Headroom Locally

Through PowerShell:
```powershell
.\scripts\setup-ai.ps1 -Headroom
```

Through Bash:
```bash
./scripts/setup-ai.sh --headroom
```

Direct CLI:
```bash
python scripts/headroom_adapter.py enable
```

### Disabling Headroom Locally

Through PowerShell:
```powershell
.\scripts\setup-ai.ps1 -DisableHeadroom
```

Through Bash:
```bash
./scripts/setup-ai.sh --disable-headroom
```

Direct CLI:
```bash
python scripts/headroom_adapter.py disable
```

### Inspecting MCP Server Configuration

For MCP-compatible environments (such as Antigravity, Claude Code, or Codex), view the suggested configuration stanza:

```bash
python scripts/headroom_adapter.py mcp-config
```

## Validation & Benchmark

Headroom adapter test suite:
```bash
python -m unittest scripts/tests/test_headroom_adapter.py
```

Context optimization benchmark:
```bash
python scripts/benchmark_context_optimization.py
```
