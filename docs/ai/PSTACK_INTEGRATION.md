# pstack integration for Levyra

Levyra enables the official [pstack](https://github.com/cursor/plugins/tree/main/pstack) plugin for Claude Code through its canonical project settings. This imports the complete upstream plugin through its supported marketplace rather than copying or modifying third-party skill files. The upstream source was reviewed on 2026-11-04; its README lists 58 skills, 23 poteto-mode playbooks, 24 engineering principles, and optional subagents. The upstream package and its availability may change.

## Installation and activation

The tracked `.agents/claude/settings.json` declares the `cursor-plugins` marketplace source and enables `pstack@cursor-plugins`. Run `python3 scripts/sync_agent_runtime.py --runtime claude` (or the repository's `scripts/setup-ai.ps1` / `setup-ai.sh`) to refresh the ignored `.claude/` projection. The declaration is not proof that the plugin has been downloaded to the local Claude installation.

The normal `scripts/setup-ai.ps1` and `scripts/setup-ai.sh` now check the actual installed plugin record and cached skill file. If Claude Code is available and external skills are not skipped, they try an official marketplace install once. A failed install logs a warning and keeps Levyra's native routing. A running Claude session may need `/reload-plugins` or a new session to see a newly installed plugin.

If the plugin is not present, install it using these commands inside Claude Code:

```text
/plugin marketplace add cursor/plugins
/plugin install pstack@cursor-plugins
```

Restart Claude Code if the commands are not immediately available. Check the installation with `/plugin`, then use `/pstack:poteto-help` or `/pstack:setup-pstack`. Configure only the model roles your machine/account actually supports. Claude Code prefixes commands with `pstack:`; for example, `/pstack:unslop`, `/pstack:tdd`, and `/pstack:interrogate`.

On Cursor, install the original plugin using `/add-plugin pstack`. Cursor-only capabilities are not automatically available in Claude Code. For Codex, there is no official pstack installation procedure documented by this upstream package; do not silently copy its Cursor-specific orchestration into Codex.

## One workflow per phase

pstack makes its entire upstream skill collection available. Availability is not an instruction to run the collection. **Choose exactly one primary workflow for each phase, and load it only when relevant.** Do not automatically chain pstack with Levyra's generic workflow skills, Matt Pocock skills, or other marketplaces for the same task. Do not run multiple agents or playbooks to repeat the same review.

The canonical root and path-specific `AGENTS.md` files remain binding, including project invariants, authorization, privacy, validation and required test evidence. Domain-specific Levyra rules still apply to app code; they are not extra generic workflows to stack. The repository's maximum of two active skill bodies per phase is a *ceiling*, not a target. One is preferred. The second must address a genuinely separate concern, such as Compose and visual design, never two competing methods for one concern.

Claude Code's `UserPromptSubmit` hook calls `scripts/pstack_auto_route.py`. On each relevant prompt, it checks that `pstack@cursor-plugins` has a real installed-plugin record and an accessible local skill file, then selects **one** matching pstack skill for that phase. It replaces the ordinary Levyra generic-skill recommendation for that phase rather than stacking both. The selected skill is read from its installed `SKILL.md` because upstream pstack sets `disable-model-invocation` on most skill commands. No network request, automatic remote agent, or duplicate prose pass is required at prompt time.

The router covers focused writing, technical documentation, impact checks, TDD, architecture, benchmarking, adversarial review, code explanation, and several situational workflows. The **entire upstream package** stays available for an explicit request or a later justified phase. Automatic use means selective on-demand access, not executing all 58 skills. Ordinary Levyra Android, Windows, player, extractor, and security tasks keep their domain workflows when there is no better distinct pstack match. When the plugin is not installed, the hook falls back to `scripts/agent_skill_router.py` unchanged. An explicitly invoked `/pstack:<skill>` command already owns its phase, so neither router injects another workflow. Do not change methodology mid-task without a reason; keep planning, implementation, review, and writing separate.

## Writing is an exclusive choice

The owner wants human writing in Italian, English and Russian: direct, concise, natural punctuation, restrained formatting, and no marketing language or chatbot filler. Keep technical terms precise. Match real samples of the owner's voice when available. Never remove or invent test claims, version numbers, limitations, links, release facts, or mandatory PR sections.

- **With installed pstack:** short public prose, release notes, and PR descriptions select `unslop` automatically when the prompt is a writing task. It **replaces**, not precedes, `levyra-humanizer`.
- **For substantial technical documentation:** the router chooses `technical-writing` automatically. It already incorporates Unslop guidance internally. Never run `/pstack:unslop` separately as a second polishing pass.
- **If pstack is unavailable:** the original `levyra-humanizer` remains the fallback for PR descriptions; write other text directly in the same human style. Do not install an additional competing humanizer.
- **For explicit slash commands:** `/pstack:unslop` and `/pstack:technical-writing` take priority for that request. Do not append another writing skill.

The writing phase comes after the technical change and verification. Do one editing pass and check factual parity. The PR template is mandatory, and an attractive description does not prove that builds or tests passed.

## What to choose, not what to combine

| Task | Single primary approach |
| --- | --- |
| Specific Levyra playback, extraction, UI or desktop implementation | Existing narrow Levyra domain workflow |
| Cross-cutting design | `/pstack:architect` *or* `levyra-real-engineering`, not both |
| Reproducible bug | `/pstack:tdd` *or* Levyra's existing focused debug workflow |
| Change impact | `/pstack:blast-radius` *or* existing impact investigation |
| Pull-request review | `/pstack:interrogate` *or* `levyra-pr-review`; retain required review and quality gates |
| Benchmark planning | `/pstack:benchmark-checklist` *or* the established performance procedure |
| Human writing | `/pstack:unslop` *or* `levyra-humanizer` |
| Technical writing | `/pstack:technical-writing` instead of an additional writing pass |

Advanced orchestration such as `/pstack:poteto-mode`, swarm, arena, or autonomous modes should not be the default for Levyra. It is easy for them to bypass the single-owner model or silently add duplicate work. Use them only for an expressly requested, bounded task with supported tools and verified isolation.

The upstream verification generators may be evaluated separately after inspecting Levyra's real Android/Windows test harness. They do not replace `ai_quality_gate.py`, Android runtime testing, CI, or an independent review.

## Unsupported or restricted actions

The full plugin can expose playbooks for autopilot, shipping, merging, background delegation, interacting with outside services, and multi-agent orchestration. These are **not** blanket authorization to run them in Levyra. Do not start autonomous merges, release publication, external messages, destructive cleanup, or new agents without the applicable owner authorization, supported tools, and project verification. In particular, invoking `/pstack:poteto-mode` does not override Levyra's boundaries, its two-skill cap, or its code/push/PR/merge/release state distinctions.

Cursor-only tools (such as specific Bugbot, automation or UI-control integrations), external model slugs, third-party MCP providers, and external services are optional; mark those checks `UNAVAILABLE` when absent. Never mark a check as passed merely because a skill is installed, a PR is green, or a model reports success.

No code, APK, dependency, version, build workflow, or data-access behavior changes as part of this integration.

## Update and provenance

pstack is published by Lauren Tan (poteto) under the MIT license. Levyra registers the official upstream marketplace without vendoring the plugin contents. Claude Code normally disables automatic updating for third-party marketplaces. To review and apply a new upstream version, use `claude plugin update pstack@cursor-plugins` (or the marketplace update controls), then inspect changed instructions and capabilities before accepting them into Levyra's workflows.

Upstream source: https://github.com/cursor/plugins/tree/main/pstack
Upstream usage and Claude Code adapter: https://github.com/cursor/plugins/blob/main/pstack/README.md
