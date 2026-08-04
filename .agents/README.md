# Levyra Agent Configuration

This directory contains Levyra's repository-local agent configuration for
Codex, Google Antigravity, OpenClaw, and other compatible coding-agent
workflows. The goal is one canonical instruction tree, focused reusable skills,
and explicit publication boundaries across runtimes.

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
docs/ai/                          AI workflow and runtime guidance
.agents/rules/                    workspace rules and canonical-contract bridges
.agents/skills/*/SKILL.md         repository-local task skills
.claude/                          Claude Code configuration and playbooks
```

Keep `AGENTS.md` in the repository root so supported coding agents discover the
repository-wide contract from the Git root. Project planning belongs under
`docs/project/`; rules and skills do not replace either layer.

## Runtime discovery

### Codex

Codex reads root and path-specific `AGENTS.md` files and uses the matching
repository-local skills under `.agents/skills/`.

### Google Antigravity

Antigravity automatically reads workspace context from the repository root and
exposes workspace skills from `.agents/skills/<skill-folder>/SKILL.md` when a
conversation starts. The file `.agents/rules/levyra-workspace.md` provides a
lightweight workspace-rule bridge to `AGENTS.md` without duplicating the
contract.

Open the repository root, start a new conversation after pulling configuration
changes, and verify the `levyra-*` inventory through the Antigravity skills
panel or `/skills`. See `docs/ai/ANTIGRAVITY.md` for the complete setup and
troubleshooting guide.

### OpenClaw and compatible runtimes

Use a dedicated `levyra` agent whose workspace is the real repository checkout.
The runtime should read the same root contract, planning files, and matching
skills rather than maintaining a separate project instruction tree.

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
| `levyra-engineering` | Genuine cross-domain coordination |

Load every matching focused skill. Planning and orchestration skills coordinate
other skills; they do not replace domain procedures.

## Expected workflow

1. Load the root and nearest path-specific `AGENTS.md` files.
2. Read `docs/project/SPEC.md`, the relevant roadmap track, and the active
   `docs/project/TASKS.md` phase when applicable.
3. Load every matching native skill.
4. Inspect current code, tests, architecture, build files, and workflows.
5. Make the smallest coherent change and report validation truthfully.
6. Treat publication, merge, tag, and release as separately authorized actions.

For Antigravity, see `docs/ai/ANTIGRAVITY.md`.

For OpenClaw, see `docs/ai/OPENCLAW.md`.

For a ChatGPT Project, use `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` as the
source instructions.

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`,
`.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and
`.claude/hooks/`.

## Validation

Run from the repository root after changing planning files, instructions,
rules, skills, AI documentation, or agent validation:

```bash
python3 scripts/validate_agent_config.py
```

The validator checks required files, the Antigravity bridge, skill metadata,
documented skill references, and this inventory.

## Maintenance rules

- Keep durable requirements in `docs/project/SPEC.md`.
- Keep ordered outcomes and risks in `docs/project/ROADMAP.md`.
- Keep one active reviewable phase in `docs/project/TASKS.md`.
- Keep architecture in `docs/ARCHITECTURE.md`.
- Keep each native skill focused on one repeatable job.
- Keep `AGENTS.md` as the canonical repository contract.
- Use `.agents/rules/` as a thin bridge, not a duplicate instruction tree.
- Link to canonical sources instead of duplicating complete instructions.
- Verify all paths and commands after structural changes.
