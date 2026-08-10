# Levyra Agent Configuration

Levyra keeps one repository-native instruction and skill tree for Codex, Claude
Code, ChatGPT Projects, Google Antigravity, OpenCode, OpenClaw, and compatible
coding agents. The same project rules, domain skills, context-efficiency policy,
real-engineering workflow, and evidence-based security workflow apply across
runtimes.

## Configuration hierarchy

```text
AGENTS.md                         repository-wide operating contract
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI and workflow rules
docs/AGENTS.md                    documentation rules
docs/project/                     specification, roadmap, and active tasks
docs/agents/                      Matt skills issue-tracker/domain configuration
docs/ARCHITECTURE.md              current architecture and ownership
docs/ai/                          runtime, RTK, workflow, and security guidance
.agents/rules/                    shared workspace-routing bridges
.agents/skills/*/SKILL.md         canonical repository-native skills
.agents/config/codex-plugins.txt  verified opt-in Codex plugin identifiers
.claude/                          Claude Code rules, skills, agents, and hooks
.rtk/filters.toml                 Levyra-specific RTK output filters
scripts/setup-ai.ps1              Windows setup and validation
scripts/setup-ai.sh               Linux/macOS setup and validation
```

Planning files, skills, plugins, and scans do not replace current code, direct
validation, human review, or owner decisions.

## Cross-runtime discovery

### Codex

Codex reads root and path-specific `AGENTS.md` files and discovers matching
skills under `.agents/skills/`. High-output work matches
`levyra-context-efficiency`; non-trivial ambiguous or multi-step engineering
matches `levyra-real-engineering`; security-sensitive work matches
`levyra-security-review`.

The setup scripts automatically install the focused Matt Pocock engineering
skills for Codex when both `codex` and `npx` are detected. Use
`-SkipMattSkills` on Windows or `--skip-matt-skills` on Linux/macOS to opt out.
The repository-native adapter remains authoritative if the upstream package is
unavailable.

RTK uses an **instruction-based Codex setup**. `rtk init -g --codex` installs
instructions that teach Codex when to invoke RTK commands; it is not a native
transparent shell-rewrite hook. Codex must rerun the original command raw when
compact output is insufficient.

Codex Security is an optional security engine enabled through the official
Codex Security setup. It complements the shared Levyra security skill with a
repository-specific threat model, safe validation, minimal remediation
proposals, human review, and revalidation. See
`docs/ai/CODEX_SECURITY.md`.

### Claude Code

Claude Code uses `.claude/CLAUDE.md`, path rules, skills, and the
`UserPromptSubmit` hook. The project enables
`mattpocock-skills@claude-plugins-official` from Claude Code's official
marketplace. The local `levyra-real-engineering` bridge selects the appropriate
upstream stage without letting it override Levyra.

The hook routes security, vulnerability, secrets, trust-boundary, dependency,
update-integrity, and privacy work to `levyra-security-review` before editing.
Claude follows the same closed-loop security method documented in
`docs/ai/CODEX_SECURITY.md`.

### ChatGPT Project

Copy `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Levyra Project
instructions and connect the repository. Those instructions require ChatGPT to
load `levyra-real-engineering` for non-trivial work and
`levyra-security-review` for security-sensitive analysis, and to distinguish
suspected findings, validated findings, proposed patches, applied patches, CI,
and publication state.

### Google Antigravity

Antigravity reads `.agents/rules/levyra-workspace.md` and exposes skills under
`.agents/skills/`. The workspace rule routes non-trivial work through
`levyra-real-engineering`, security work through `levyra-security-review`, keeps
exact security evidence raw, and applies the same threat-model and revalidation
workflow. No parallel `.gemini/skills/` tree is required.

### OpenCode and OpenClaw

OpenCode uses the same root/path instructions and workspace skills. OpenClaw
should use a dedicated Levyra workspace and delegate substantial implementation
to a coding runtime while preserving the same skills, evidence, and publication
boundaries.

## Native skills

| Skill | Primary use |
| --- | --- |
| `levyra-project-manager` | Specification, roadmap, active phase, acceptance criteria, and handoff |
| `levyra-openclaw-orchestrator` | OpenClaw delegation, coding-runtime coordination, review, and evidence |
| `levyra-context-efficiency` | RTK routing, focused context selection, measured savings, and raw-output fallback |
| `levyra-real-engineering` | Matt Pocock-style clarify/spec/tickets/implement/review routing for non-trivial work |
| `levyra-player` | Android playback, queue, Media3, MediaSession, notification, and audio/video modes |
| `levyra-extractor` | InnerTube, extraction, stream resolution, fallback, retry, and cache |
| `levyra-database` | Room, migrations, stores, backup, and persistent user data |
| `levyra-compose` | Compose UI, state, navigation, accessibility, RTL, and localization |
| `levyra-motion-artwork` | Decorative motion artwork and muted playback boundaries |
| `levyra-desktop` | Windows Desktop, libvlc, downloads, mini player, updates, and packaging |
| `levyra-security-review` | Cross-runtime threat modeling, vulnerability validation, minimal remediation, privacy, supply chain, and revalidation |
| `levyra-ci-workflows` | GitHub Actions, CI, F-Droid, artifacts, and automation |
| `levyra-pr-review` | Evidence-based branch, commit, patch, and pull-request review |
| `levyra-release-check` | Pre-merge and pre-release validation |
| `levyra-engineering` | Genuine cross-domain coordination |

Load every matching focused skill. Coordinator, real-engineering,
context-efficiency, and security skills do not replace the applicable
product-domain skill.

## Automatic routing

Load `levyra-real-engineering` for non-trivial features, architectural changes,
unclear defects, or multi-step work where requirements and implementation should
be separated. Use the lightest stage necessary and skip the full workflow for
tiny, already-unambiguous changes. See `docs/ai/MATT_POCOCK_SKILLS.md`.

Load `levyra-context-efficiency` for verbose builds, tests, lint, logs, searches,
dependencies, Git/GitHub, CI, CodeRabbit, and setup work.

Load `levyra-security-review` for vulnerability scans, attacker-controlled
input, trust-boundary changes, authentication, tokens, cookies, signing,
secrets, URLs, redirects, SSRF, MIME, paths, permissions, privacy, dependency
risk, workflow security, artifacts, updates, and security-related pull requests.

All runtimes use this security cycle:

```text
threat model
→ identification
→ safe validation
→ minimal remediation
→ human review
→ revalidation
```

A suspicion is not a confirmed vulnerability until evidence supports the attack
path or concrete security failure.

## RTK safety

Use RTK selectively for noisy supported commands. Keep exact output, exploit
evidence, security validation, hashes, signatures, secret scans, signing,
release evidence, and incomplete failure diagnostics raw. Verify exit status and
success/failure markers, and rerun the exact original command raw whenever
compact output hides required evidence.

## Setup

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

When Codex and `npx` are available, setup also installs the focused
`mattpocock/skills` workflow globally for Codex. Claude Code discovers the
project-enabled official plugin from `.claude/settings.json`. Antigravity and
ChatGPT use the repository-native adapter whether or not the upstream package is
installed.

The scripts do not configure Ollama/local-model profiles, unrestricted
sandboxing, or silent approval bypasses. Root `AGENTS.md` explicitly authorizes
only the pinned RTK bootstrap and the focused Matt Pocock integration described
there; every other unapproved executable or plugin remains opt-in.

## Validation

```bash
python3 scripts/validate_agent_config.py
python3 scripts/validate_ai_efficiency.py
python3 scripts/validate_matt_skills.py
```

The validators check shared discovery, skill inventory, RTK TOML, setup
behavior, real-engineering integration, cross-runtime security routing,
dependency review, plugin scope, and absence of unapproved local-model profiles.

## Maintenance rules

- Keep `AGENTS.md` as the canonical repository contract.
- Keep one canonical Levyra skill tree under `.agents/skills/`; runtime-specific
  files may be thin routing bridges only.
- Keep `levyra-real-engineering` as a routing adapter, not a copied fork of the
  external skill package.
- Keep RTK as an output layer, never validation authority.
- Keep security findings evidence-based and revalidate every remediation.
- Keep the pinned RTK bootstrap and focused Matt integration narrow; every other
  unapproved plugin or executable installation remains opt-in.
- Keep commit, push, PR, merge, tag, release, upload, and repository settings
  under explicit owner authorization.
- Verify paths, commands, skills, workflows, and documentation after structural
  changes.
