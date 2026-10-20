# Codex automatic tooling bootstrap

Levyra treats Codex setup as repository infrastructure rather than a manual checklist. Once the project-local `.codex` layer is trusted, a Codex `SessionStart` hook checks the owner-authorized tooling automatically on `startup` and `resume`.

## What is automatic

- RTK is verified through the existing pinned Levyra bootstrap and installed only when missing.
- jCodeMunch `1.108.279` is installed into a versioned user cache from the exact GitHub release wheel pinned by SHA-256, then the Levyra index is refreshed.
- The focused Matt Pocock Codex skills are installed only when one or more expected global skills are missing.
- Repository-native `levyra-*` skills need no installation; Codex discovers them directly from `.agents/skills`.
- claude-mem keeps its existing automatic-on-need policy and is not forced into every session.

## jCodeMunch MCP

`.codex/config.toml` registers jCodeMunch as a project-scoped optional MCP server. `scripts/codex_jcodemunch.py` verifies the pinned version before starting the server and keeps bootstrap chatter off MCP stdout so the JSON-RPC handshake is not contaminated.

The MCP is deliberately `required = false`. If Python, networking, installation, indexing, or the MCP handshake fails, Codex continues with its native repository tools. jCodeMunch is a first-pass navigator, not a replacement for full-file reads or native search when broader context is necessary.

## Command rules

`scripts/sync_agent_runtime.py` projects `.agents/codex/rules/levyra.rules`
to `.codex/rules/levyra.rules`. The rules allow routine Git/GitHub inspection
and RTK diagnostics. Claude uses the corresponding explicit Bash permissions
in `.agents/claude/settings.json`. These selected command rules follow the
approach in [titus-ai](https://github.com/ChrisTitusTech/titus-ai), adapted to
Levyra's existing runtime projection.

Shells, generic wrappers (`rtk proxy`, `rtk run`), arbitrary `gh api` calls,
installs, commits, pushes, PR changes, and releases receive no new allow rule.
Existing sandbox, trust, secret-denial, and owner-authorization requirements
still apply. Restart the agents after refreshing the projections to load the
updated configuration.

## First trust

Codex requires review/trust for non-managed project hooks. That trust decision is intentionally not bypassed. After setup, review and trust the project hooks with `/hooks`, then start a new Codex session so project config and lifecycle hooks are loaded from startup. After that first trust, normal sessions require no manual setup command. Changing the hook may cause Codex to request trust again.

## Repair and verification

Windows:

```powershell
.\scripts\ensure-codex-tooling.ps1
```

Bash/WSL/Linux/macOS:

```bash
./scripts/ensure-codex-tooling.sh
```

Dry-run without installs:

```powershell
.\scripts\ensure-codex-tooling.ps1 -DryRun
```

```bash
./scripts/ensure-codex-tooling.sh --dry-run
```

A bootstrap failure is diagnostic, not permission to weaken sandboxing, bypass hook trust, disable validation, or block normal coding.

Repository contract check:

```bash
python3 scripts/validate_codex_bootstrap.py
```
