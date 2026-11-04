# pstack integration for Levyra

Levyra enables the official [pstack](https://github.com/cursor/plugins/tree/main/pstack) plugin for Claude Code through its canonical project settings. This imports the complete upstream plugin through its supported marketplace rather than copying or modifying third-party skill files. The upstream source was reviewed on 2026-11-04; its README lists 58 skills, 23 poteto-mode playbooks, 24 engineering principles, and optional subagents. The upstream package and its availability may change.

## Installation and activation

The tracked `.agents/claude/settings.json` declares the `cursor-plugins` marketplace source and enables `pstack@cursor-plugins`. Run `python3 scripts/sync_agent_runtime.py --runtime claude` (or the repository's `scripts/setup-ai.ps1` / `setup-ai.sh`) to refresh the ignored `.claude/` projection. The declaration is not proof that the plugin has been downloaded to the local Claude installation.

If the plugin is not present, install it using these commands inside Claude Code:

```text
/plugin marketplace add cursor/plugins
/plugin install pstack@cursor-plugins
```

Restart Claude Code if the commands are not immediately available. Check the installation with `/plugin`, then use `/pstack:poteto-help` or `/pstack:setup-pstack`. Configure only the model roles your machine/account actually supports. Claude Code prefixes commands with `pstack:`; for example, `/pstack:unslop`, `/pstack:tdd`, and `/pstack:interrogate`.

On Cursor, install the original plugin using `/add-plugin pstack`. Cursor-only capabilities are not automatically available in Claude Code. For Codex, there is no official pstack installation procedure documented by this upstream package; do not silently copy its Cursor-specific orchestration into Codex.

## Use with Levyra

Keep the root and nearest path-specific `AGENTS.md` authoritative. `docs/ai/ALWAYS_ON_AGENT_GUARDS.md`, the existing skill router, privacy and playback invariants, publication authorization, and mandatory quality gates are unchanged. pstack is an optional source of focused techniques, not a replacement agent policy.

The useful complementary tasks include:

- `/pstack:blast-radius` before a narrow but risky change.
- `/pstack:architect` for cross-boundary design.
- `/pstack:tdd` for a reproducible defect with suitable local tests.
- `/pstack:interrogate` for adversarial diff review when supported models and tools are available.
- `/pstack:benchmark-checklist` for credible performance comparisons.
- `/pstack:correct` or `/pstack:reflect` to turn recurring verified mistakes into checks.
- `/pstack:unslop` and `/pstack:technical-writing` for final documentation or PR prose. The existing `levyra-humanizer` remains the default for PR descriptions.
- The verification skill generators only after inspecting and successfully exercising the existing Android or desktop harness. A generated plan is not proof of a real device test.

Do not force pstack into each prompt. Use no more than two skill bodies in the same phase; if two Levyra domain skills are already needed, defer the pstack review to a later phase. Preserve the existing deterministic `scripts/agent_skill_router.py` selection.

## Unsupported or restricted actions

The full plugin can expose playbooks for autopilot, shipping, merging, background delegation, interacting with outside services, and multi-agent orchestration. These are **not** blanket authorization to run them in Levyra. Do not start autonomous merges, release publication, external messages, destructive cleanup, or new agents without the applicable owner authorization, supported tools, and project verification. In particular, invoking `/pstack:poteto-mode` does not override Levyra's boundaries, its two-skill cap, or its code/push/PR/merge/release state distinctions.

Cursor-only tools (such as specific Bugbot, automation or UI-control integrations), external model slugs, third-party MCP providers, and external services are optional; mark those checks `UNAVAILABLE` when absent. Never mark a check as passed merely because a skill is installed, a PR is green, or a model reports success.

No code, APK, dependency, version, build workflow, or data-access behavior changes as part of this integration.

## Update and provenance

pstack is published by Lauren Tan (poteto) under the MIT license. Levyra registers the official upstream marketplace without vendoring the plugin contents. Claude Code normally disables automatic updating for third-party marketplaces. To review and apply a new upstream version, use `claude plugin update pstack@cursor-plugins` (or the marketplace update controls), then inspect changed instructions and capabilities before accepting them into Levyra's workflows.

Upstream source: https://github.com/cursor/plugins/tree/main/pstack
Upstream usage and Claude Code adapter: https://github.com/cursor/plugins/blob/main/pstack/README.md
