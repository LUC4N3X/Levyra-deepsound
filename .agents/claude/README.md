# Levyra Claude Code Configuration

Levyra keeps the tracked Claude Code source under `.agents/claude/` and the shared
project skills under `.agents/skills/`.

The native `.claude/` directory is a generated local runtime projection. It is
ignored by Git and must never become a second source of truth.

## Canonical structure

```text
.agents/
├── claude/
│   ├── CLAUDE.md
│   ├── README.md
│   ├── settings.json
│   ├── agents/
│   ├── hooks/
│   └── rules/
└── skills/
    └── */SKILL.md
```

There is intentionally no tracked root `CLAUDE.md`, `.claude/`, or duplicate
`.agents/claude/skills/` tree.

## Native Claude projection

Claude Code still receives the paths it expects locally:

```text
.claude/
├── CLAUDE.md        <- .agents/claude/CLAUDE.md
├── settings.json    <- .agents/claude/settings.json
├── agents/          <- .agents/claude/agents/
├── rules/           <- .agents/claude/rules/
└── skills/          <- .agents/skills/
```

Run the normal repository setup once after a fresh clone or after pulling this
migration:

```powershell
.\scripts\setup-ai.ps1
```

or:

```bash
./scripts/setup-ai.sh
```

Both setup scripts invoke `scripts/sync_agent_runtime.py`, which materializes the
ignored native Claude and Codex projections from `.agents/` without deleting
unrelated local files. The projection is also refreshed on Claude
`SessionStart`/resume after the native settings are active. The project-scoped
jCodeMunch launcher performs a best-effort Claude projection refresh as an
additional clean-clone bootstrap path.

To refresh or verify only Claude manually:

```bash
python3 scripts/sync_agent_runtime.py --runtime claude --quiet
python3 scripts/sync_agent_runtime.py --runtime claude --check
```

On Windows, `python` or `py` can be used when `python3` is not available.

## Automatic skill loading

`.agents/skills/` is the only tracked Levyra skill tree.

- Codex discovers `.agents/skills/` directly.
- Claude Code discovers the generated `.claude/skills/` projection.
- Every projected Claude skill comes directly from the same canonical
  `.agents/skills/<skill>/SKILL.md` file.
- `UserPromptSubmit` continues routing matching requests to the required Levyra
  skills before editing.
- `settings.json` keeps the project plugins and lifecycle hooks active.

This means adding or changing a Levyra skill requires editing it once under
`.agents/skills/`. The next runtime sync updates Claude's native view; Codex uses
the canonical file directly.

## Settings and hooks

The tracked settings file is `.agents/claude/settings.json`. Its generated
`.claude/settings.json` counterpart is what Claude Code consumes locally.

The settings preserve the existing permission guardrails, project plugins,
SessionStart environment/tooling checks, jCodeMunch bootstrap, prompt routing,
always-on agent harness, checkpointing, comment guard, and completion audit.

Canonical hook scripts stay under `.agents/claude/hooks/`. The generated
settings call those canonical scripts directly, so hook logic is not duplicated
inside `.claude/`.

The hooks are fail-open for optional tooling and must never turn an unavailable
optimizer or indexer into a blocked coding session. Validation evidence remains
truthful: a setup probe is a precondition check, not proof that a build or test
passed.

## Personal local overrides

Generated `.claude/` is ignored by Git. The runtime synchronizer only owns files
listed in its `.levyra-runtime-manifest.json`; it does not delete unrelated
machine-specific files. Local-only Claude configuration can therefore coexist
with the generated project projection.

## Usage

After setup, start Claude Code normally from the repository. No custom skill path
is required. Claude sees the normal `.claude` runtime surface while the GitHub
repository remains organized under `.agents/`.

If a new skill is added while Claude Code is already running, start a new session
when necessary so Claude refreshes its discovered skill inventory.

## Maintenance

- edit canonical Claude configuration only under `.agents/claude/`;
- edit Levyra skills only under `.agents/skills/`;
- never commit root `CLAUDE.md`, `.claude/`, `.codex/`, or
  `.agents/claude/skills/`;
- keep `scripts/sync_agent_runtime.py` idempotent and non-destructive to unknown
  local files;
- preserve Claude's native automatic discovery by keeping the generated
  `.claude` projection compatible with the current runtime;
- run the AI quality gate after structural agent changes.
