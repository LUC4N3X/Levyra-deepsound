---
name: levyra-codex-bootstrap
description: Use when Codex tooling, jCodeMunch, RTK, Matt Pocock skills, automatic setup, context efficiency, or Codex MCP/bootstrap behavior for Levyra is being inspected, repaired, or changed. Do not load for ordinary coding tasks unless the bootstrap itself is relevant.
---

# Levyra Codex bootstrap

The repository owner explicitly authorizes Levyra's checked-in Codex bootstrap to install and run only the pinned/allowlisted tooling defined by the repository:

- the existing pinned `rtk-ai/rtk` revision;
- jCodeMunch `1.108.279` from the exact release wheel and SHA-256 declared in `scripts/codex_jcodemunch.py`;
- the focused Matt Pocock Codex skill allowlist already declared by Levyra;
- the existing claude-mem integration only under its separate on-demand policy.

The automatic bootstrap must remain idempotent and fail-open. A missing package manager, unavailable network, failed install, failed MCP handshake, or stale index must never disable Codex's native repository tools or block normal coding. Do not add read/search blocking rules. Prefer jCodeMunch for targeted exploration, then expand to native reads/search whenever control flow, lifecycle, concurrency, state, tests, generated behavior, or correctness requires broader evidence.

Do not add Token Optimizer or another always-on compression proxy without a separate owner decision. Token savings never override correctness, validation, security evidence, or repository architecture.

Relevant files:

- `.codex/config.toml`
- `.codex/hooks.json`
- `scripts/codex_jcodemunch.py`
- `scripts/ensure-codex-tooling.ps1`
- `scripts/ensure-codex-tooling.sh`
- `docs/ai/CODEX_AUTO_BOOTSTRAP.md`
