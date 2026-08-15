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

## First trust

Codex requires review/trust for non-managed project hooks. That trust decision is intentionally not bypassed. After the project hook is trusted, normal sessions require no manual setup command. Changing the hook may cause Codex to request trust again.

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
