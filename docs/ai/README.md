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
docs/agents/                      issue-tracker/domain config for agent skills
docs/ARCHITECTURE.md              current implementation ownership and flow
.agents/skills/*/SKILL.md         canonical repository-native skills
.agents/rules/levyra-workspace.md shared workspace-routing bridge
.agents/config/codex-plugins.txt  verified opt-in plugin manifest
.claude/                          Claude Code rules, skills, agents, and hooks
.rtk/filters.toml                 Levyra-specific RTK filters
scripts/setup-ai.ps1              Windows setup and validation
scripts/setup-ai.sh               Linux/macOS setup and validation
docs/ai/RTK.md                    RTK routing, measurement, and raw fallback
docs/ai/CODEX_SECURITY.md         shared cross-runtime security workflow
docs/ai/MATT_POCOCK_SKILLS.md     real-engineering workflow and runtime setup
docs/ai/ANTIGRAVITY.md            Antigravity discovery and routing
docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md
                                  ChatGPT Project instructions source
docs/ai/OPENCLAW.md               OpenClaw workspace and delegation
docs/ai/WORKFLOW.md               clarification-to-release lifecycle
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
- `levyra-real-engineering`
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

Automatically load `levyra-real-engineering` for non-trivial features,
architectural changes, unclear defects, or multi-step work. It routes only the
Matt Pocock stages actually needed: `grill-with-docs`, `wayfinder`, `to-spec`,
`to-tickets`, `implement`, `tdd`, `diagnosing-bugs`, `code-review`, and
`domain-modeling`. Tiny, already-unambiguous changes keep the normal Levyra work
method.

Automatically load `levyra-context-efficiency` for verbose builds, tests, lint,
logs, searches, dependencies, Git/GitHub, CI, CodeRabbit, adb, and setup work.

Automatically load `levyra-security-review` for vulnerability scans,
attacker-controlled input, trust-boundary changes, authentication, secrets,
permissions, privacy, dependency/supply-chain risk, update integrity, or
security-related pull requests.

## Shared real-engineering method

Matt Pocock's skills supplement Levyra rather than becoming a second source of
truth. `AGENTS.md`, approved planning, current architecture, focused Levyra
skills, tests, and owner publication controls always take precedence.

```text
ambiguous feature
→ grill-with-docs
→ to-spec
→ to-tickets only when needed
→ implement + tdd
→ code-review + levyra-pr-review

large unresolved problem
→ wayfinder
→ continue from the appropriate stage

unclear defect
→ diagnosing-bugs
→ minimal fix + regression test
→ code-review + levyra-pr-review
```

Repository-specific issue-tracker and domain-doc configuration lives under
`docs/agents/`. Specs and tickets do not imply permission to publish GitHub
issues. See [`MATT_POCOCK_SKILLS.md`](MATT_POCOCK_SKILLS.md).

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
5. For non-trivial work, enter through `levyra-real-engineering` and load the
   exact installed Matt Pocock stage selected by that adapter.
6. Inspect current code and tests.
7. Make the smallest coherent change.
8. Report exact validation and publication state.

When both `codex` and `npx` are available, the setup scripts install the focused
`mattpocock/skills` allowlist globally for Codex with the Agent Skills CLI. Use
`-SkipMattSkills` or `--skip-matt-skills` to opt out on a machine.

RTK uses an **instruction-based Codex setup**. `rtk init -g --codex` installs
instructions that teach Codex when to invoke RTK commands; it is not a transparent native
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

The project enables `mattpocock-skills@claude-plugins-official`. The local
`levyra-real-engineering` bridge and `UserPromptSubmit` routing decide when to
invoke the upstream stages while preserving Levyra precedence and keeping the
full workflow out of tiny fixes.

The same hook routes security, vulnerability, CVE, trust-boundary, dependency,
supply-chain, leak, integrity, signature, and workflow-security work to
`levyra-security-review`. Path-specific security rules enforce the same
threat-model and revalidation method.

The setup scripts initialize RTK's Claude hook when `claude` is installed.

## ChatGPT Project

Repository files do not automatically become persistent Project instructions.

1. Create a ChatGPT Project named `Levyra`.
2. Connect `LUC4N3X/Levyra-deepsound` through the available GitHub integration.
3. Copy `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into Project instructions.
4. Start a new Project conversation after changing those instructions.

The Project instructions require ChatGPT to load `levyra-real-engineering` for
non-trivial work, use exact upstream stage bodies when accessible, fall back to
the repository adapter otherwise, and load `levyra-security-review` for
security-sensitive analysis. ChatGPT must distinguish assumptions, suspected
findings, validated findings, proposed patches, applied patches, CI, review,
merge, and release state.

## Google Antigravity

Open the Git repository root as the workspace. Antigravity reads
`.agents/rules/levyra-workspace.md` and exposes skills under `.agents/skills/`.
Keep the workspace rule **Always On** when activation controls are available.

The rule automatically loads real-engineering, context-efficiency, and
security-review skills as applicable. The repository-native
`levyra-real-engineering` adapter means Antigravity does not need a parallel
`.gemini/skills/` tree just to obtain the workflow. Exact security evidence
remains raw and follows the shared revalidation cycle. See
[`ANTIGRAVITY.md`](ANTIGRAVITY.md).

## OpenCode and OpenClaw

OpenCode uses the same root/path instructions and shared skill tree; the setup
scripts initialize its RTK integration when detected.

OpenClaw should use a dedicated Levyra workspace and explicit delegation. A
coding runtime handles substantial implementation, while a fresh reviewer
checks the latest diff. Merge, tag, release, store upload, credential rotation,
and repository settings remain separate owner actions. See [`OPENCLAW.md`](OPENCLAW.md).

## RTK and agent setup

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

- support the automatic agent bootstrap of the pinned `rtk-ai/rtk` build;
- configure detected supported runtimes;
- install the focused Matt Pocock skill allowlist for Codex when Codex and
  `npx` are available;
- install verified `.agents/config/codex-plugins.txt` entries only when requested;
- run all three repository AI-configuration validators;
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
| Codex | Focused implementation, tests, validation, and authorized repository delivery | root/path `AGENTS.md`, shared skills, focused Matt skills, automatic pinned RTK |
| Claude Code | Implementation and independent review with automatic skill routing | `.claude/` plus official Matt plugin and shared planning/skills |
| Antigravity/OpenCode | Workspace implementation and review using shared rules and skills | `.agents/`, root/path instructions, repository real-engineering adapter |
| OpenClaw | Explicit delegation, status collection, and handoff | dedicated workspace, shared skills, narrow tool policy |
| RTK | Compact non-sensitive terminal output and measure reductions | executable, supported runtime integration, `.rtk/filters.toml` |
| Security engine | Additional threat modeling, scanning, validation, and patch proposals | official setup plus `levyra-security-review` |
| Owner | Scope, publication, merge, release, credentials, and settings | direct human decision |

## Complete workflow

```text
repository orientation
→ resolve genuine ambiguity when needed
→ spec when useful
→ reviewable tickets when needed
→ one reviewable implementation
→ focused validation
→ repository gate
→ complete diff inspection
→ independent/security review
→ publication only when authorized
→ CI and manual checks
→ owner-controlled merge and release
```

Do not collapse implementation, validation, review, CI, merge, and release into
one "done" state.

## Validation

Every supported coding runtime, including ChatGPT when it has command access,
uses the repository quality gate:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

Run the fast profile before commit and the full profile before push or pull
request publication. Without command access, ChatGPT must require the commands
in its implementation handoff and must not report them as passed.

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_matt_skills.py
```

Run all three after changing instructions, skills, hooks, AI docs, RTK filters,
setup scripts, plugins, real-engineering routing, security routing, or
dependency-review configuration.

## Maintenance rules

- Update root/path `AGENTS.md` for operating invariants.
- Update one focused native skill for a repeatable workflow.
- Keep `levyra-real-engineering` as the stable Levyra adapter instead of
  vendoring a parallel fork of Matt Pocock's package.
- Keep runtime-specific files as thin routing bridges.
- Update `RTK.md`, `.rtk/filters.toml`, and setup scripts together.
- Update `MATT_POCOCK_SKILLS.md`, real-engineering routing, setup scripts, and
  validator together when upstream integration behavior changes.
- Update `CODEX_SECURITY.md`, `levyra-security-review`, Claude routing, ChatGPT
  instructions, and Antigravity routing together when security behavior changes.
- Keep the pinned RTK bootstrap and focused Matt integration least-privileged;
  every other unapproved executable and plugin remains opt-in.
- Verify paths, commands, skill names, hooks, workflows, artifacts, and
  publication state after structural changes.
