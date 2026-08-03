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

## AI documentation

- `AGENTS.md` files define repository and path-specific operating contracts.
- `.agents/skills/` contains native OpenAI/Codex task skills.
- `.claude/` contains Claude Code configuration and shared Levyra engineering playbooks.
- `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` is the source text for the Levyra ChatGPT Project; repository files cannot apply those Project instructions automatically.

## Validation

For documentation-only changes, verify referenced paths and commands, inspect Markdown headings and code fences, check links where possible, and inspect the final diff for accidental code, workflow, version, binary, or secret changes.

Do not claim builds or tests ran merely because a documented command appears correct.
