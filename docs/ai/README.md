# AI Assistant Setup for Levyra

Levyra uses one repository-native engineering contract across ChatGPT Projects,
Codex, Claude Code, Google Antigravity, OpenCode, OpenClaw, RTK, and optional
security tooling. Runtime-specific files route into the same root instructions,
planning documents, skills, evidence standards, and publication boundaries.

## Architecture at a glance

```text
AGENTS.md                         canonical repository contract
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI/workflow rules
docs/AGENTS.md                    documentation rules
docs/project/                     specification, roadmap, and active tasks
docs/ARCHITECTURE.md              current implementation ownership and flow
.agents/skills/*/SKILL.md         canonical repository-native skills
.agents/rules/levyra-workspace.md shared workspace-routing bridge
.claude/                          Claude Code rules, skills, agents, and hooks
.rtk/filters.toml                 Levyra-specific RTK filters
codex-plugins.txt                 verified opt-in plugin manifest
scripts/setup-ai.ps1              Windows setup and validation
scripts/setup-ai.sh               Linux/macOS setup and validation
docs/ai/RTK.md                    RTK routing, measurement, and raw fallback
docs/ai/CODEX_SECURITY.md         shared cross-runtime security workflow
docs/ai/ANTIGRAVITY.md            Antigravity discovery and routing
docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md
                                  ChatGPT Project instructions source
docs/ai/OPENCLAW.md               OpenClaw workspace and delegation
docs/ai/WORKFLOW.md               implementation-to-release lifecycle
```

Planning files remain distinct:

- `docs/project/SPEC.md` defines durable owner-approved requirements;
- `docs/project/ROADMAP.md` orders outcomes, risks, and exit criteria;
- `docs/project/TASKS.md` records one active reviewable phase and direct
  validation evidence.

## Shared skill routing

Every runtime loads the most specific matching `levyra-*` skills. Several may
apply to one task.

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
- `levyra-engineering`

Automatically load `levyra-context-efficiency` for verbose builds, tests, lint,
logs, searches, dependencies, Git/GitHub, CI, CodeRabbit, adb, and setup work.

Automatically load `levyra-security-review` for vulnerability scans,
attacker-controlled input, trust-boundary changes, authentication, secrets,
permissions, privacy, dependency/supply-chain risk, update integrity, or
security-related pull requests.

## Shared security method

Codex, Claude Code, ChatGPT Projects, and Antigravity use the same closed-loop
workflow:

```text
threat model
→ identification
→ safe validation
→ minimal remediation
→ human review
→ revalidation
```

A suspected issue is not a confirmed vulnerability until evidence supports a
concrete attack path or security failure. Keep security scans, exploit evidence,
hashes, signatures, secret scans, signing evidence, and exact reproductions raw.
See [`CODEX_SECURITY.md`](CODEX_SECURITY.md); despite the file name, the method
is runtime-independent.

## Codex

1. Start Codex from the repository root or a nested project directory.
2. Read the root and nearest path-specific `AGENTS.md` files.
3. Read relevant planning and architecture material.
4. Load every matching native skill before broad investigation or editing.
5. Inspect current code and tests.
6. Make the smallest coherent change.
7. Report exact validation and publication state.

RTK uses an **instruction-based Codex setup**. `rtk init -g --codex` installs
instructions that teach Codex when to invoke RTK; it is not a transparent native
shell-rewrite hook.

Codex Security may be enabled through its official setup and used alongside the
shared `levyra-security-review` skill. Levyra does not invent an unverified CLI
manifest identifier for it.

## Claude Code

Claude uses:

- `.claude/CLAUDE.md`;
- `.claude/rules/`;
- `.claude/skills/`;
- `.claude/agents/`;
- `.claude/hooks/`;
- `.claude/settings.json`.

The `UserPromptSubmit` hook routes security, vulnerability, CVE, trust-boundary,
dependency, supply-chain, leak, integrity, signature, and workflow-security work
to `levyra-security-review`. Path-specific security rules enforce the same
threat-model and revalidation method.

The setup scripts initialize RTK's Claude hook when `claude` is installed.

## ChatGPT Project

Repository files do not automatically become persistent Project instructions.

1. Create a ChatGPT Project named `Levyra`.
2. Connect `LUC4N3X/Levyra-deepsound` through the available GitHub integration.
3. Copy `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into Project instructions.
4. Start a new Project conversation after changing those instructions.

The Project instructions require ChatGPT to read matching native skills,
including `levyra-security-review`, inspect current repository evidence, and
distinguish assumptions, suspected findings, validated findings, proposed
patches, applied patches, CI, review, merge, and release state.

## Google Antigravity

Open the Git repository root as the workspace. Antigravity reads
`.agents/rules/levyra-workspace.md` and exposes skills under `.agents/skills/`.
Keep the workspace rule **Always On** when activation controls are available.

The rule automatically loads both context-efficiency and security-review skills,
keeps exact security evidence raw, and applies the shared revalidation cycle.
See [`ANTIGRAVITY.md`](ANTIGRAVITY.md).

## OpenCode and OpenClaw

OpenCode uses the same root/path instructions and shared skill tree; the setup
scripts initialize its RTK integration when detected.

OpenClaw should use a dedicated Levyra workspace and explicit delegation. A
coding runtime handles substantial implementation, while a fresh reviewer
checks the latest diff. Merge, tag, release, store upload, credential rotation,
and repository settings remain separate owner actions. See [`OPENCLAW.md`](OPENCLAW.md).

## RTK setup

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

The scripts:

- optionally install RTK from `rtk-ai/rtk`;
- configure detected supported runtimes;
- install verified `codex-plugins.txt` entries only when requested;
- run both repository validators;
- fail closed when required Python validation cannot run;
- do not install Ollama or local-model profiles;
- do not enable unrestricted sandboxing or silent approval bypasses.

Measure command-output reduction with:

```text
rtk gain
rtk discover --all --since 7
rtk session
```

RTK savings are not equal to total token billing. See [`RTK.md`](RTK.md).

## Dependency security

`.github/workflows/dependency-review.yml` performs a Dependency Graph preflight.
When Dependency Graph is unavailable, the actual Dependency Review job is
**skipped** and the workflow summary records **blocked, not passed**. Once the
Graph is available, newly introduced known vulnerabilities at high or critical
severity remain blocking.

## Responsibility split

| Runtime | Primary role | Main configuration |
| --- | --- | --- |
| ChatGPT Project | Requirements, investigation, architecture, planning, PR interpretation, and task preparation | Project instructions plus connected repository |
| Codex | Focused implementation, tests, validation, and authorized repository delivery | root/path `AGENTS.md`, planning files, shared skills, instruction-based RTK, optional tools |
| Claude Code | Implementation and independent review with automatic skill routing | `.claude/` plus shared planning and skills |
| Antigravity/OpenCode | Workspace implementation and review using shared rules and skills | `.agents/`, root/path instructions, optional RTK integration |
| OpenClaw | Explicit delegation, status collection, and handoff | dedicated workspace, shared skills, narrow tool policy |
| RTK | Compact non-sensitive terminal output and measure reductions | executable, supported runtime integration, `.rtk/filters.toml` |
| Security engine | Additional threat modeling, scanning, validation, and patch proposals | official setup plus `levyra-security-review` |
| Owner | Scope, publication, merge, release, credentials, and settings | direct human decision |

## Complete workflow

```text
requirements
→ active phase
→ focused plan
→ one reviewable implementation
→ focused validation
→ repository gate
→ complete diff inspection
→ independent/security review
→ pull request when authorized
→ CI and manual checks
→ owner-controlled merge and release
```

Do not collapse implementation, validation, review, CI, merge, and release into
one "done" state.

## Validation

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
```

Run both after changing instructions, skills, hooks, AI docs, RTK filters, setup
scripts, plugins, security routing, or dependency-review configuration.

## Maintenance rules

- Update root/path `AGENTS.md` for operating invariants.
- Update one focused native skill for a repeatable workflow.
- Keep runtime-specific files as thin routing bridges.
- Update `RTK.md`, `.rtk/filters.toml`, and setup scripts together.
- Update `CODEX_SECURITY.md`, `levyra-security-review`, Claude routing, ChatGPT
  instructions, and Antigravity routing together when security behavior changes.
- Keep executable/plugin installation opt-in and permissions least-privileged.
- Verify paths, commands, skill names, hooks, workflows, artifacts, and
  publication state after structural changes.
