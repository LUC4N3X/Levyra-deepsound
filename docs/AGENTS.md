# Levyra Documentation Instructions

These instructions extend the root `AGENTS.md` for documentation under `docs/`.

## Documentation standard

- Describe the current repository, not an abandoned design, planned feature, or remembered implementation.
- Verify file paths, commands, task names, version locations, workflow names, artifact names, and configuration keys against the current tree before documenting them.
- Keep Android and Desktop behavior, versioning, release tags, build commands, and artifact paths clearly separated.
- Do not expose credentials, private URLs, internal tokens, signing details, personal data, or security-sensitive operational values.
- Prefer concise authoritative guidance over duplicated procedure text. Link to the narrowest source of truth.
- Keep examples safe to copy: use placeholders for secrets, preserve shell quoting, and identify platform-specific commands.
- Update architecture documentation when a change alters ownership, data flow, module boundaries, persistence, playback, networking, or release architecture.
- Update credits and license documentation when external code, assets, models, libraries, or design references are added.

## Documentation layout

- `docs/README.md` is the canonical documentation index.
- `docs/ARCHITECTURE.md` describes current implementation ownership and data flow.
- `docs/project/` contains durable requirements, the engineering roadmap, and the active task phase.
- `docs/ai/` contains ChatGPT, Codex, Claude Code, and OpenClaw collaboration guidance.
- `docs/assets/` contains documentation media, README badges, and previews.
- Keep this `docs/AGENTS.md` file at the documentation root so its scoped instructions apply to every documentation subdirectory.

## Planning and AI documentation

- Root and nested `AGENTS.md` files define repository-wide and path-specific operating contracts.
- `docs/project/SPEC.md` defines durable owner-approved requirements and non-goals.
- `docs/project/ROADMAP.md` defines ordered outcomes, risks, and phase exit criteria; it does not authorize implementation or release.
- `docs/project/TASKS.md` records one active reviewable phase and direct validation evidence.
- `docs/project/README.md` explains how specification, roadmap, and active tasks work together.
- `.agents/skills/` contains the single canonical Levyra skill tree shared across supported runtimes.
- `.agents/claude/` contains the tracked Claude Code instructions, settings, agents, hooks, and rules. The native `.claude/` directory is an ignored local projection generated from `.agents/claude/` and `.agents/skills/`.
- `.agents/codex/` contains the tracked Codex project configuration projected locally to the ignored native `.codex/` directory.
- `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` is the source text for the Levyra ChatGPT Project; repository files cannot apply those Project instructions automatically.
- `docs/ai/WORKFLOW.md` defines the complete AI-assisted engineering lifecycle and its independent review, CI, manual testing, merge, and release gates.
- `docs/ai/OPENCLAW.md` defines the recommended OpenClaw workspace, delegation, tool, and publication boundaries.

Keep these documents synchronized with current repository behavior, but do not duplicate entire domain playbooks across them. When planning documents conflict with current code or tests, surface and correct the stale documentation rather than silently documenting the conflict as intended behavior.

## Validation

For documentation-only changes, verify referenced paths and commands, inspect Markdown headings and code fences, check links where possible, and inspect the final diff for accidental code, workflow, version, binary, or secret changes.

After changing project planning files, agent instructions, native skills, AI documentation, or their validation, run:

```bash
python3 scripts/validate_agent_config.py
```

Do not claim builds, tests, agent-configuration checks, CI, device checks, reviews, publication, merge, or release merely because a documented command appears correct.
