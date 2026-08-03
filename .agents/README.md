# Levyra OpenAI Agent Configuration

This directory contains Levyra's repository-local native skills for Codex,
OpenClaw, and compatible OpenAI coding-agent workflows.

## Why `AGENTS.md` is in the repository root

Codex discovers project instructions by starting at the Git root and walking
toward the current working directory. Therefore:

- root `AGENTS.md` is the automatic repository-wide contract;
- `app/AGENTS.md`, `desktop/AGENTS.md`, `.github/AGENTS.md`, and
  `docs/AGENTS.md` provide narrower path-specific instructions;
- `.agents/skills/` contains task procedures and must not replace the root
  instruction file.

Moving the root contract into `docs/` or `.agents/` would make it ordinary
documentation rather than guaranteed repository-root guidance.

## Planning layers

```text
AGENTS.md                         durable operating rules
SPEC.md                           approved requirements and non-goals
ROADMAP.md                        ordered outcomes, risks, exit criteria
TASKS.md                          one active phase and validation state
docs/ARCHITECTURE.md              current implementation ownership and flow
docs/ai/WORKFLOW.md               complete AI-assisted lifecycle
docs/ai/OPENCLAW.md               OpenClaw workspace and delegation guidance
```

The planning files do not replace current code or tests. When they conflict with
repository evidence, the agent must surface the conflict and correct the stale
planning material rather than implementing around it silently.

## Configuration layers

```text
AGENTS.md                         repository-wide Codex contract
app/AGENTS.md                     Android-specific rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI and workflow security rules
docs/AGENTS.md                    documentation rules
.agents/skills/*/SKILL.md         native task skills
docs/ai/                          ChatGPT, Codex, Claude and OpenClaw guidance
.claude/                          Claude Code configuration and detailed playbooks
```

## Native skills

| Skill | Primary use |
| --- | --- |
| `levyra-project-manager` | Requirements, roadmap, active phase, acceptance criteria, validation and implementation handoff |
| `levyra-openclaw-orchestrator` | Dedicated OpenClaw workspace, explicit delegation, coding runtimes, review and evidence handoff |
| `levyra-player` | Android playback, queue, Media3, MediaSession, notification, Android Auto, audio/video modes |
| `levyra-extractor` | InnerTube, extraction, stream resolution, runtime configuration, fallback and cache behavior |
| `levyra-database` | Room, DAO, migrations, schemas, caches, stores, backups and persistent personal data |
| `levyra-compose` | Android Compose UI, state, navigation, lifecycle, accessibility, RTL and localization |
| `levyra-motion-artwork` | Decorative motion artwork, provider matching, muted playback and remote-media safety |
| `levyra-desktop` | Windows Desktop, Compose Multiplatform, libvlc, downloads, mini player, deep links and updates |
| `levyra-security-review` | Secrets, URLs, redirects, SSRF, MIME, permissions, privacy and update integrity |
| `levyra-ci-workflows` | GitHub Actions, CI, F-Droid, configuration sync, artifacts and automation security |
| `levyra-pr-review` | Evidence-based review of branches, commits, patches and pull requests |
| `levyra-release-check` | Pre-merge/release validation, versions, signing, checksums, packaging and artifacts |
| `levyra-engineering` | Cross-domain coordination when no single specialized skill is sufficient |

Focused work should use the most specific skill. Several skills may be loaded
for one change. Planning and OpenClaw orchestration skills coordinate domain
skills; they do not replace them.

## Codex workflow

Start Codex from the repository root or a subdirectory inside the repository.

Expected behavior:

1. Codex loads the root `AGENTS.md`.
2. It adds every nearer `AGENTS.md` covering the working directory.
3. It reads `SPEC.md`, the relevant `ROADMAP.md` track, and active `TASKS.md`
   phase when applicable.
4. It selects the matching native skill or skills.
5. The skill points to relevant current code, tests, architecture, and detailed
   Levyra playbooks under `.claude/`.
6. Codex makes the smallest coherent change and reports validation truthfully.
7. Publication remains a separate explicitly authorized action.

Example prompts:

```text
Use levyra-project-manager, levyra-player and levyra-extractor. Trace why
playback sometimes resolves slowly, identify the root cause, map the change to
the active phase, and propose the smallest compatible fix before editing.
```

```text
Use levyra-pr-review and review the current diff. Put evidence-backed findings
first and distinguish tested behavior from manual checks.
```

## OpenClaw

Use a dedicated `levyra` agent whose workspace is the real repository checkout.
The project-local `.agents/skills/` tree is then available to that workspace.

Load `levyra-openclaw-orchestrator` for delegation. Use explicit agent targets,
narrow tool access, a coding runtime for substantial implementation, and a fresh
reviewer for the latest diff. OpenClaw may coordinate a branch and draft PR only
when the owner explicitly authorizes publication; it must not infer permission
to merge, tag, release, or change repository settings.

See `docs/ai/OPENCLAW.md`.

## ChatGPT

A normal ChatGPT conversation does not automatically treat repository files as
persistent Project instructions. Create a Levyra ChatGPT Project, connect the
repository, and paste the content of
`docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Project instructions.

ChatGPT should use the repository for product decisions, investigation,
architecture, planning, and review. Codex should perform implementation and
publication work when authorized.

## Claude Code

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`,
`.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and
`.claude/hooks/`.

The detailed `.claude/skills/` and `.claude/rules/` files remain useful as shared
Levyra engineering playbooks. OpenAI skills reference them instead of
duplicating their full content.

## Validation

Run from the repository root after changing planning files, agent instructions,
native skills, AI documentation, or agent validation:

```bash
python3 scripts/validate_agent_config.py
```

The validator checks required files, native skill front matter, skill-directory
names, documented skill references, and the inventory in this file. The
existing pull-request workflow runs the same command.

## Maintenance rules

- Keep repository-wide invariants concise in root `AGENTS.md`.
- Put path-specific constraints in the nearest `AGENTS.md`.
- Keep durable requirements in `SPEC.md`.
- Keep ordered outcomes and risks in `ROADMAP.md`.
- Keep one active reviewable phase and truthful evidence in `TASKS.md`.
- Keep each native skill focused on one repeatable job.
- Put architecture in `docs/ARCHITECTURE.md`.
- Update the narrowest detailed playbook when a recurring project-specific
  failure is discovered.
- Do not duplicate entire instructions across assistant-specific trees.
- Do not mark task or validation status from an agent narrative.
- Verify every referenced file, command, version location, workflow, artifact
  path, skill name, and publication state after structural changes.
