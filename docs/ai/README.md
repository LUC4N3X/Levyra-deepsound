# AI Assistant Setup for Levyra

This guide explains how Levyra uses ChatGPT, Codex, Claude Code, and OpenClaw
without conflicting instruction trees or publication permissions.

## Architecture at a glance

```text
AGENTS.md                         shared repository contract
SPEC.md                           approved requirements and non-goals
ROADMAP.md                        ordered outcomes, risks and exit criteria
TASKS.md                          active reviewable phase and validation state
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI/workflow rules
docs/AGENTS.md                    documentation rules
.agents/skills/*/SKILL.md         native Codex/OpenAI/OpenClaw skills
docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md
                                  ChatGPT Project instructions source
docs/ai/WORKFLOW.md               complete AI-assisted engineering lifecycle
docs/ai/OPENCLAW.md               OpenClaw workspace and delegation guide
.claude/                          Claude Code configuration and detailed playbooks
```

`AGENTS.md` belongs in the Git root because Codex discovers project instructions
from the project root toward the current working directory. Files under
`.agents/skills/` are task skills, not replacements for root project guidance.

The planning files have distinct responsibilities:

- `SPEC.md` states durable approved behavior.
- `ROADMAP.md` orders outcomes and risks but does not authorize implementation
  or release.
- `TASKS.md` tracks one active reviewable phase and direct validation evidence.

## Codex setup

1. Open or clone the repository.
2. Start Codex from the repository root or a directory inside it.
3. Codex loads root `AGENTS.md` and any nearer path-specific `AGENTS.md`.
4. Read `SPEC.md`, the relevant roadmap track, and active task phase.
5. Use the most specific native skill or skills for the task.
6. Inspect current code, tests and detailed playbooks before editing.
7. Make the smallest coherent change and report validation truthfully.
8. Publish only when the current owner request explicitly authorizes it.

Native skills:

- `levyra-project-manager`
- `levyra-openclaw-orchestrator`
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
Read the applicable AGENTS.md files, SPEC.md, ROADMAP.md and TASKS.md. Use the
most specific Levyra native skills. Inspect current code and tests before making
assumptions. Describe the verified behavior, root cause or rationale, intended
files, preserved behavior, risks and validation plan before editing.
```

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
instead of duplicating them.

## OpenClaw setup

Use OpenClaw as the coordinator and status layer around a dedicated `levyra`
agent. Its workspace should be the real Levyra checkout so project instructions,
planning files, Git state, and `.agents/skills/` are available together.

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
| Codex | Focused implementation, tests, validation, commits, branches and pull requests when authorized | root/path `AGENTS.md`, planning files and `.agents/skills/` |
| Claude Code | Implementation and independent review using Claude-specific hooks, agents, permissions and plugins | `.claude/` plus repository planning files |
| OpenClaw | Explicit delegation, status collection, recurring read-only checks and handoff between configured agents | dedicated Levyra workspace, project skills and narrow tool policy |
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
states. An agent must not collapse them into "done".

## Validation

After changing planning files, agent instructions, native skills, AI
documentation, or validation:

```bash
python3 scripts/validate_agent_config.py
```

The existing PR workflow runs the same command before the Android gate.

## Keeping instructions consistent

- Update root `AGENTS.md` for shared repository-wide invariants.
- Update the nearest nested `AGENTS.md` for platform/path constraints.
- Update `SPEC.md` when approved durable requirements change.
- Update `ROADMAP.md` when ordered outcomes, risks, or exit criteria change.
- Replace the active phase in `TASKS.md` when new work begins.
- Update one native skill for a repeatable task workflow.
- Update `docs/ARCHITECTURE.md` for architecture or ownership changes.
- Update the narrowest detailed `.claude/rules/` or `.claude/skills/` playbook
  for recurring domain-specific failures.
- Update `CHATGPT_PROJECT_INSTRUCTIONS.md` only when ChatGPT collaboration
  behavior changes.
- Update `OPENCLAW.md` only when OpenClaw workspace, delegation, or tool
  boundaries change.
- Prefer routing and references over duplicated prose.
- Verify paths, commands, task names, version locations, workflow names,
  artifact paths, skill names, and publication state after structural changes.
