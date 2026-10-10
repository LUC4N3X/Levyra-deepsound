# Levyra Claude Code Configuration

Levyra keeps Claude-specific tracked runtime sources under `.agents/claude/` and
shared project skills under `.agents/skills/`.

A tiny tracked root `CLAUDE.md` is intentionally present because Claude Code
natively reads `CLAUDE.md`, not `AGENTS.md`. The root bridge imports `AGENTS.md`
so the essential Levyra contract is available immediately in a fresh clone,
before any generated `.claude/` projection exists.

## Canonical structure

```text
CLAUDE.md                      native startup bridge -> @AGENTS.md
AGENTS.md                      compact cross-runtime contract

.agents/
├── claude/
│   ├── CLAUDE.md              Claude-specific runtime guidance
│   ├── README.md
│   ├── settings.json
│   ├── agents/
│   ├── hooks/
│   └── rules/
└── skills/
    └── */SKILL.md
```

The generated `.claude/` directory remains local and ignored. There is still no
tracked duplicate `.agents/claude/skills/` tree.

## Native Claude projection

Runtime setup materializes the optional native surface:

```text
.claude/
├── CLAUDE.md        <- .agents/claude/CLAUDE.md
├── settings.json    <- .agents/claude/settings.json
├── agents/          <- .agents/claude/agents/
├── rules/           <- .agents/claude/rules/
└── skills/          <- .agents/skills/
```

The projected `.claude/CLAUDE.md` contains Claude-specific runtime guidance only;
root `CLAUDE.md` + imported `AGENTS.md` remain the reliable startup contract.

Run setup after a fresh clone or after agent-infrastructure changes:

```powershell
.\scripts\setup-ai.ps1
```

or:

```bash
./scripts/setup-ai.sh
```

Both invoke `scripts/sync_agent_runtime.py`. SessionStart/resume refreshes the
projection again once native project settings are active. The project
jCodeMunch launcher also performs a best-effort refresh.

Manual Claude-only refresh/check:

```bash
python3 scripts/sync_agent_runtime.py --runtime claude --quiet
python3 scripts/sync_agent_runtime.py --runtime claude --check
```

## Automatic instruction and skill loading

The reliability chain is deliberately layered:

1. root `CLAUDE.md` loads natively at startup and imports `AGENTS.md`;
2. generated `.claude/CLAUDE.md` adds only Claude-specific runtime guidance;
3. `UserPromptSubmit` re-anchors a compact hard contract on every prompt;
4. the shared router selects only matching Levyra skills;
5. path-scoped rules/instructions load when relevant files are touched.

This avoids depending on Claude voluntarily searching for repository rules and
also avoids stuffing every specialized procedure into startup context.

`.agents/skills/` remains the only tracked Levyra skill tree. Claude discovers
the generated `.claude/skills/` view; Codex reads the canonical tree directly.

## Settings and hooks

Tracked settings live at `.agents/claude/settings.json`; generated
`.claude/settings.json` is what Claude Code consumes locally.

The settings preserve permission guardrails, project plugins, SessionStart
environment checks, jCodeMunch bootstrap, prompt routing, always-on harness,
checkpointing, comment guard, compaction re-anchoring, and completion audit.

`UserPromptSubmit` must always inject the compact hard contract even if skill
routing returns no specialized match. If Python is unavailable, the hook emits a
minimal static fallback rather than silently providing no context.

Optional tooling is fail-open: unavailable RTK, jCodeMunch, or memory tooling
must not block normal coding. Validation claims remain evidence-based.

## Personal local overrides

Generated `.claude/` is ignored by Git. The synchronizer owns only files listed
in its runtime manifest and preserves unrelated machine-specific local files.

## Maintenance

- keep root `CLAUDE.md` tiny and importing `@AGENTS.md`;
- keep `AGENTS.md` concise enough for reliable startup adherence;
- edit Claude runtime sources under `.agents/claude/`;
- edit Levyra skills only under `.agents/skills/`;
- never commit generated `.claude/`, `.codex/`, or
  `.agents/claude/skills/` trees;
- keep `scripts/sync_agent_runtime.py` idempotent and non-destructive;
- keep prompt-hook fallback behavior and validators in sync with discovery;
- run the AI quality gate after structural agent changes.
