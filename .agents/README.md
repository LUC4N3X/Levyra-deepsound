# Levyra Agent Configuration

This directory contains Levyra's repository-local agent configuration for
Codex, Google Antigravity, OpenClaw, and other compatible coding-agent
workflows. The goal is one canonical instruction tree, focused reusable skills,
automatic context-efficient command routing, and explicit publication
boundaries across runtimes.

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
docs/ai/                          AI workflow, runtime, and RTK guidance
.agents/rules/                    workspace rules and canonical-contract bridges
.agents/skills/*/SKILL.md         repository-local task skills
.rtk/filters.toml                 Levyra-specific RTK command-output filters
codex-plugins.txt                 opt-in recommended Codex plugin manifest
scripts/setup-ai.ps1              Windows agent/RTK/plugin bootstrap
scripts/setup-ai.sh               Linux/macOS agent/RTK/plugin bootstrap
.claude/                          Claude Code configuration and playbooks
```

Keep `AGENTS.md` in the repository root so supported coding agents discover the
repository-wide contract from the Git root. Project planning belongs under
`docs/project/`; rules and skills do not replace either layer.

## Runtime discovery

### Codex

Codex reads root and path-specific `AGENTS.md` files and uses the matching
repository-local skills under `.agents/skills/`. Build, test, lint, log, search,
Git/GitHub, CI, and setup tasks match `levyra-context-efficiency`
automatically. When RTK is installed and its Codex hook is initialized, noisy
shell commands are rewritten to compact RTK equivalents before their output
enters model context.

### Google Antigravity

Antigravity automatically reads workspace context from the repository root and
exposes workspace skills from `.agents/skills/<skill-folder>/SKILL.md` when a
conversation starts. The file `.agents/rules/levyra-workspace.md` provides a
lightweight workspace-rule bridge to `AGENTS.md` without duplicating the
contract. The repository setup script initializes Antigravity's local RTK
integration when RTK is available.

Open the repository root, start a new conversation after pulling configuration
changes, and verify the `levyra-*` inventory through the Antigravity skills
panel or `/skills`. See `docs/ai/ANTIGRAVITY.md` and `docs/ai/RTK.md` for setup
and troubleshooting.

### Claude Code, OpenCode, OpenClaw, and compatible runtimes

Claude Code continues to use its existing `.claude/` configuration and Levyra
playbooks. The setup scripts detect `claude` and `opencode` and initialize their
RTK integrations when installed.

Use a dedicated `levyra` OpenClaw agent whose workspace is the real repository
checkout. The runtime should read the same root contract, planning files, and
matching skills rather than maintaining a separate project instruction tree.

## Native skills

| Skill | Primary use |
| --- | --- |
| `levyra-project-manager` | Specification, roadmap, active phase, acceptance criteria, and handoff |
| `levyra-openclaw-orchestrator` | OpenClaw delegation, coding-runtime coordination, review, and evidence |
| `levyra-context-efficiency` | Automatic RTK routing, focused context selection, savings measurement, and safe raw-output fallback |
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

Load every matching focused skill. Planning, orchestration, and context-
efficiency skills coordinate other skills; they do not replace domain
procedures.

## Automatic context efficiency

`levyra-context-efficiency` is designed to match automatically when a task is
likely to generate large or repetitive shell output. It prefers RTK for
supported builds, tests, lint, logs, searches, dependency reports, Git/GitHub,
CI, and CodeRabbit output.

The skill requires the agent to:

- keep short or exact-output commands raw;
- rerun commands raw when compact output hides required evidence;
- verify exit status and final success/failure markers;
- retain complete security, signing, checksum, and release evidence;
- report which commands were filtered and which were rerun raw;
- never equate RTK command-output savings with total billing savings.

Project-local filters live in `.rtk/filters.toml`. RTK's dedicated built-in
handlers remain responsible for Gradle, Git, GitHub CLI, common tests, lint,
Docker, searches, and standard logs.

See `docs/ai/RTK.md` for installation, automatic runtime detection, fallback,
measurement, and safety rules.

## Setup

Preview on Windows:

```powershell
.\scripts\setup-ai.ps1 -DryRun
```

Configure already-installed RTK and detected agents:

```powershell
.\scripts\setup-ai.ps1
```

Install RTK through Cargo and the opt-in plugin manifest:

```powershell
.\scripts\setup-ai.ps1 -InstallRtk -Plugins
```

Linux/macOS equivalents:

```bash
./scripts/setup-ai.sh --dry-run
./scripts/setup-ai.sh
./scripts/setup-ai.sh --install-rtk --plugins
```

The setup scripts do not install or configure Ollama/local model profiles and
do not enable unrestricted sandboxing or silent approval bypasses.

## Expected workflow

1. Load the root and nearest path-specific `AGENTS.md` files.
2. Read `docs/project/SPEC.md`, the relevant roadmap track, and the active
   `docs/project/TASKS.md` phase when applicable.
3. Load every matching native skill, including `levyra-context-efficiency` for
   noisy command-driven work.
4. Inspect current code, tests, architecture, build files, and workflows.
5. Use RTK selectively and rerun raw whenever complete evidence is needed.
6. Make the smallest coherent change and report validation truthfully.
7. Treat publication, merge, tag, and release as separately authorized actions.

For Antigravity, see `docs/ai/ANTIGRAVITY.md`.

For RTK and automatic agent setup, see `docs/ai/RTK.md`.

For OpenClaw, see `docs/ai/OPENCLAW.md`.

For a ChatGPT Project, use `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` as the
source instructions.

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`,
`.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and
`.claude/hooks/`.

## Validation

Run from the repository root after changing planning files, instructions,
rules, skills, AI documentation, RTK configuration, setup scripts, plugins, or
agent validation:

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

The validators check required files, the Antigravity bridge, skill metadata,
documented skill references, the skill inventory, RTK filters, setup scripts,
plugin scope, and the absence of unapproved local-model profiles. The PR
workflow runs both validators.

## Maintenance rules

- Keep durable requirements in `docs/project/SPEC.md`.
- Keep ordered outcomes and risks in `docs/project/ROADMAP.md`.
- Keep one active reviewable phase in `docs/project/TASKS.md`.
- Keep architecture in `docs/ARCHITECTURE.md`.
- Keep each native skill focused on one repeatable job.
- Keep `AGENTS.md` as the canonical repository contract.
- Use `.agents/rules/` as a thin bridge, not a duplicate instruction tree.
- Use RTK only as a context/output layer, never as validation authority.
- Keep plugin and executable installation opt-in.
- Link to canonical sources instead of duplicating complete instructions.
- Verify all paths and commands after structural changes.
