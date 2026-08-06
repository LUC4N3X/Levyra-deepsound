# AI Assistant Setup for Levyra

This guide explains how Levyra uses ChatGPT, Codex, Claude Code, Google
Antigravity, OpenCode, OpenClaw, RTK, and optional Codex plugins without
conflicting instruction trees or publication permissions.

## Architecture at a glance

```text
AGENTS.md                         shared repository contract
docs/README.md                    documentation index
docs/project/SPEC.md              approved requirements and non-goals
docs/project/ROADMAP.md           ordered outcomes, risks and exit criteria
docs/project/TASKS.md             active reviewable phase and validation state
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI/workflow rules
docs/AGENTS.md                    documentation rules
.agents/skills/*/SKILL.md         native Codex/OpenAI/OpenClaw skills
.agents/rules/levyra-workspace.md automatic workspace routing bridge
.rtk/filters.toml                 Levyra-specific RTK filters
codex-plugins.txt                 opt-in Codex plugin manifest
scripts/setup-ai.ps1              Windows automatic setup
scripts/setup-ai.sh               Linux/macOS automatic setup
docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md
                                  ChatGPT Project instructions source
docs/ai/WORKFLOW.md               complete AI-assisted engineering lifecycle
docs/ai/RTK.md                    RTK, token efficiency, hooks, and fallback
docs/ai/OPENCLAW.md               OpenClaw workspace and delegation guide
.claude/                          Claude Code configuration and detailed playbooks
```

`AGENTS.md` belongs in the Git root because Codex discovers project instructions
from the project root toward the current working directory. Files under
`.agents/skills/` are task skills, not replacements for root project guidance.

The planning files have distinct responsibilities:

- `docs/project/SPEC.md` states durable approved behavior.
- `docs/project/ROADMAP.md` orders outcomes and risks but does not authorize
  implementation or release.
- `docs/project/TASKS.md` tracks one active reviewable phase and direct
  validation evidence.

See [`../README.md`](../README.md) for the complete documentation map,
[`../project/README.md`](../project/README.md) for the planning-document model,
and [`RTK.md`](RTK.md) for automatic context-efficiency setup.

## Codex setup

1. Open or clone the repository.
2. Start Codex from the repository root or a directory inside it.
3. Codex loads root `AGENTS.md` and any nearer path-specific `AGENTS.md`.
4. Read `docs/project/SPEC.md`, the relevant roadmap track, and active task
   phase.
5. Use the most specific native skill or skills for the task.
6. Automatically include `levyra-context-efficiency` for builds, tests, lint,
   logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, or setup.
7. Inspect current code, tests and detailed playbooks before editing.
8. Make the smallest coherent change and report validation truthfully.
9. Publish only when the current owner request explicitly authorizes it.

Native skills:

- `levyra-project-manager`
- `levyra-openclaw-orchestrator`
- `levyra-context-efficiency`
- `levyra-player`
- `levyra-extractor`
- `levyra-database`
- `levyra-compose`
- `levyra-motion-artwork`
- `levyra-desktop`
- `levyra-security-review`
- `levyra-ci-workflows`
- `levyra-pr-review`
- `levyra-release-check`
- `levyra-engineering` for genuine cross-domain coordination

Recommended orientation prompt:

```text
Read the applicable AGENTS.md files, docs/project/SPEC.md,
docs/project/ROADMAP.md and docs/project/TASKS.md. Use the most specific Levyra
native skills. Load levyra-context-efficiency automatically for noisy shell
work. Inspect current code and tests before making assumptions. Describe the
verified behavior, root cause or rationale, intended files, preserved behavior,
risks and validation plan before editing.
```

## RTK and automatic runtime setup

RTK compresses repetitive command output before it enters model context. It is
especially useful for Levyra's Gradle builds, tests, lint, Git/GitHub output,
CodeRabbit reviews, broad searches, CI diagnostics, adb output, extractor logs,
and Desktop build logs.

The project-local skill and workspace rule tell compatible agents to use RTK
automatically when it is available, but require a raw rerun whenever compact
output is incomplete. Exact output, exit codes, signatures, checksums, signing,
security evidence, and unresolved diagnostics remain raw.

Windows:

```powershell
.\scripts\setup-ai.ps1 -DryRun
.\scripts\setup-ai.ps1
.\scripts\setup-ai.ps1 -InstallRtk -Plugins
```

Linux/macOS:

```bash
./scripts/setup-ai.sh --dry-run
./scripts/setup-ai.sh
./scripts/setup-ai.sh --install-rtk --plugins
```

The scripts detect installed Codex, Claude Code, and OpenCode commands,
initialize the repository-local Antigravity integration, optionally install RTK
through Cargo, optionally install `codex-plugins.txt`, and run both agent
validators. No Ollama or local-model profile is installed or configured.

Restart the coding agent or begin a new conversation after setup or after
pulling skill/rule changes. Measure real command-output reductions with:

```text
rtk gain
rtk discover --all --since 7
rtk session
```

See [`RTK.md`](RTK.md) for the complete routing, installation, fallback,
measurement, attribution, and security policy.

## ChatGPT setup

Repository files do not automatically become persistent ChatGPT Project
instructions.

1. Create a ChatGPT Project named `Levyra`.
2. Connect or select `LUC4N3X/Levyra-deepsound` through the available GitHub
   integration.
3. Copy the full contents of `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the
   Project instructions.
4. Keep Levyra requirements, planning, investigation, architecture, PR review
   and release preparation inside that Project.
5. Use Codex for implementation, tests and repository publication when
   authorized.

The Project instructions direct ChatGPT to read the planning files, root and
path-specific `AGENTS.md`, native skills, current code/tests, and to distinguish
verified repository state from assumptions.

## Claude Code setup

Claude Code continues to use `.claude/README.md` and the complete `.claude/`
configuration:

- `.claude/CLAUDE.md`
- `.claude/rules/`
- `.claude/skills/`
- `.claude/agents/`
- `.claude/hooks/`
- `.claude/settings.json`

The OpenAI configuration references detailed Levyra playbooks under `.claude/`
instead of duplicating them. `scripts/setup-ai.ps1` and `scripts/setup-ai.sh`
initialize RTK's global Claude hook automatically when the `claude` command is
detected.

## Google Antigravity and OpenCode setup

Antigravity reads `.agents/rules/levyra-workspace.md` and exposes skills under
`.agents/skills/`. The setup scripts initialize RTK's repository-local
Antigravity integration so shell commands can be compacted without duplicating
Levyra's instruction tree.

When `opencode` is detected, the setup scripts initialize RTK's OpenCode
integration. OpenCode must still follow Levyra's root/path instructions,
planning files, domain skills, validation, and publication boundaries.

## OpenClaw setup

Use OpenClaw as the coordinator and status layer around a dedicated `levyra`
agent. Its workspace should be the real Levyra checkout so project instructions,
planning files, Git state, `.agents/skills/`, and `.rtk/filters.toml` are
available together.

Use explicit agent targets and narrow tool access. For substantial
implementation, delegate to a configured coding runtime such as Codex, Claude
Code, or OpenCode. Use a fresh reviewer for the latest diff. OpenClaw may
coordinate a branch and draft PR only when the owner explicitly authorizes
publication; merge, tag, release, store upload, and repository settings remain
separate owner actions.

See `docs/ai/OPENCLAW.md`.

## Responsibility split

| Assistant | Primary role | Main configuration |
| --- | --- | --- |
| ChatGPT Project | Requirements, investigation, architecture, planning, PR interpretation and coding-task preparation | Project instructions plus connected repository |
| Codex | Focused implementation, tests, validation, commits, branches and pull requests when authorized | root/path `AGENTS.md`, `docs/project/`, `.agents/skills/`, RTK hook, and opt-in plugins |
| Claude Code | Implementation and independent review using Claude-specific hooks, agents, permissions and plugins | `.claude/`, repository planning files, and optional RTK hook |
| Antigravity/OpenCode | Workspace coding and review using shared Levyra skills and rules | `.agents/`, root/path instructions, and optional RTK integration |
| OpenClaw | Explicit delegation, status collection, recurring read-only checks and handoff between configured agents | dedicated Levyra workspace, project skills and narrow tool policy |
| RTK | Compact supported terminal output and report measured savings | installed executable, runtime hook/integration, `.rtk/filters.toml`, and `levyra-context-efficiency` |
| Owner | Scope, publication authorization, merge, release and repository settings | Direct human decision |

## Complete workflow

The complete lifecycle is documented in `docs/ai/WORKFLOW.md`:

```text
requirements
→ active phase
→ focused plan
→ one reviewable implementation
→ focused validation
→ repository gate
→ complete diff inspection
→ independent review
→ draft PR when authorized
→ CI and manual checks
→ owner-controlled merge and release
```

Implementation, review, CI, manual testing, merge, and release are separate
states. An agent must not collapse them into "done". RTK output compression is
also separate from test success or review approval.

## Validation

After changing planning files, agent instructions, native skills, AI
documentation, RTK filters, setup scripts, plugins, or validation:

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

The PR workflow runs both commands before the Android gate.

## Keeping instructions consistent

- Update root `AGENTS.md` for shared repository-wide invariants.
- Update the nearest nested `AGENTS.md` for platform/path constraints.
- Update `docs/project/SPEC.md` when approved durable requirements change.
- Update `docs/project/ROADMAP.md` when ordered outcomes, risks, or exit criteria
  change.
- Replace the active phase in `docs/project/TASKS.md` when new work begins.
- Update one native skill for a repeatable task workflow.
- Update `.rtk/filters.toml` only for verified project-specific output patterns.
- Update `docs/ai/RTK.md` when setup, hook, fallback, or measurement behavior
  changes.
- Update `docs/ARCHITECTURE.md` for architecture or ownership changes.
- Update the narrowest detailed `.claude/rules/` or `.claude/skills/` playbook
  for recurring domain-specific failures.
- Update `CHATGPT_PROJECT_INSTRUCTIONS.md` only when ChatGPT collaboration
  behavior changes.
- Update `OPENCLAW.md` only when OpenClaw workspace, delegation, or tool
  boundaries change.
- Prefer routing and references over duplicated prose.
- Keep executable/plugin installation opt-in and permissions least-privileged.
- Verify paths, commands, task names, version locations, workflow names,
  artifact paths, skill names, hooks, plugin identifiers, and publication state
  after structural changes.
