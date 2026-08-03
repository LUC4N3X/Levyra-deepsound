# Levyra OpenAI Agent Configuration

This directory contains Levyra's repository-local native skills for Codex,
OpenClaw, and compatible OpenAI coding-agent workflows.

## Configuration hierarchy

```text
AGENTS.md                         repository-wide operating contract
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI and workflow rules
docs/AGENTS.md                    documentation rules
docs/README.md                    documentation index
docs/project/SPEC.md              durable requirements and non-goals
docs/project/ROADMAP.md           ordered outcomes, risks, exit criteria
docs/project/TASKS.md             active phase and validation state
docs/ARCHITECTURE.md              current implementation ownership and flow
docs/ai/                          AI workflow and orchestration guidance
.agents/skills/*/SKILL.md         native task skills
.claude/                          Claude Code configuration and playbooks
```

Keep `AGENTS.md` in the repository root so agents discover the repository-wide
contract automatically. Project planning belongs under `docs/project/`; skills
do not replace either layer.

## Native skills

| Skill | Primary use |
| --- | --- |
| `levyra-project-manager` | Specification, roadmap, active phase, acceptance criteria, and handoff |
| `levyra-openclaw-orchestrator` | OpenClaw delegation, coding-runtime coordination, review, and evidence |
| `levyra-player` | Android playback, queue, Media3, MediaSession, notification, and audio/video modes |
| `levyra-extractor` | InnerTube, extraction, stream resolution, fallback, retry, and cache |
| `levyra-database` | Room, migrations, stores, backup, and persistent user data |
| `levyra-compose` | Compose UI, state, navigation, accessibility, RTL, and localization |
| `levyra-motion-artwork` | Decorative motion artwork and muted playback boundaries |
| `levyra-desktop` | Windows Desktop, libvlc, downloads, mini player, updates, and packaging |
| `levyra-security-review` | URLs, redirects, permissions, privacy, and update integrity |
| `levyra-ci-workflows` | GitHub Actions, CI, F-Droid, artifacts, and automation |
| `levyra-pr-review` | Evidence-based branch, commit, patch, and pull-request review |
| `levyra-release-check` | Pre-merge and pre-release validation |
| `levyra-engineering` | Cross-domain coordination |

Load every matching focused skill. Planning and OpenClaw skills coordinate
other skills; they do not replace domain procedures.

## Expected workflow

1. Load the root and nearest path-specific `AGENTS.md` files.
2. Read `docs/project/SPEC.md`, the relevant roadmap track, and the active
   `docs/project/TASKS.md` phase when applicable.
3. Load every matching native skill.
4. Inspect current code, tests, architecture, build files, and workflows.
5. Make the smallest coherent change and report validation truthfully.
6. Treat publication, merge, tag, and release as separately authorized actions.

For OpenClaw, use a dedicated `levyra` agent whose workspace is the real
repository checkout. See `docs/ai/OPENCLAW.md`.

For a ChatGPT Project, use
`docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` as the source instructions.

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`,
`.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and
`.claude/hooks/`.

## Validation

Run from the repository root after changing planning files, instructions,
skills, AI documentation, or agent validation:

```bash
python3 scripts/validate_agent_config.py
```

The validator checks required files, skill metadata, documented skill
references, and this inventory.

## Maintenance rules

- Keep durable requirements in `docs/project/SPEC.md`.
- Keep ordered outcomes and risks in `docs/project/ROADMAP.md`.
- Keep one active reviewable phase in `docs/project/TASKS.md`.
- Keep architecture in `docs/ARCHITECTURE.md`.
- Keep each native skill focused on one repeatable job.
- Link to canonical sources instead of duplicating complete instructions.
- Verify all paths and commands after structural changes.
