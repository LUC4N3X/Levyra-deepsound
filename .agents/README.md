# Levyra Agent Configuration

`.agents/` is Levyra's single tracked control center for coding agents.

The repository keeps one canonical skill tree and one canonical set of
runtime-specific configuration sources. Native `.claude/` and `.codex/`
directories are generated locally when a runtime needs them and are ignored by
Git.

There is intentionally no tracked root `CLAUDE.md`.

## Configuration hierarchy

```text
AGENTS.md                         repository-wide operating contract
app/AGENTS.md                     Android rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI and workflow rules
docs/AGENTS.md                    documentation rules

.agents/
├── README.md                     this inventory and maintenance contract
├── config/                       shared agent/plugin manifests
├── rules/                        cross-runtime workspace rules
├── skills/                       one canonical Levyra skill tree
├── claude/                       canonical Claude Code config sources
│   ├── CLAUDE.md
│   ├── settings.json
│   ├── agents/
│   ├── hooks/
│   └── rules/
└── codex/                        canonical Codex project config sources
    ├── config.toml
    └── hooks.json

scripts/sync_agent_runtime.py     native runtime projection manager
```

## Native runtime projection

Claude Code and Codex still receive the paths they expect. Levyra does not ask
those tools to understand a custom unsupported runtime path.

`scripts/sync_agent_runtime.py` materializes:

```text
.agents/claude/...  -> .claude/...
.agents/skills/...  -> .claude/skills/...
.agents/codex/...   -> .codex/...
```

The generated `.claude/` and `.codex/` directories are ignored by Git. The
synchronizer keeps a manifest of files it owns, removes only stale managed
entries, and preserves unrelated machine-specific local files.

The normal setup commands refresh both projections:

```powershell
.\scripts\setup-ai.ps1
```

```bash
./scripts/setup-ai.sh
```

After the native runtime config is active, Claude Code and Codex refresh their
projection again on startup/resume. The project jCodeMunch launcher also performs
a best-effort Claude projection refresh as an additional clean-clone bootstrap.

## Automatic skill discovery

`.agents/skills/` is the only tracked Levyra skill tree.

- **Codex** discovers `.agents/skills/` directly.
- **Claude Code** discovers the generated `.claude/skills/` projection, which is
  copied directly from `.agents/skills/`.
- **Google Antigravity** uses `.agents/rules/levyra-workspace.md` and the same
  repository-native skills.
- **ChatGPT Project** instructions route to the same skills through
  `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md`.
- OpenClaw and compatible coding agents use the same canonical contracts where
  supported.

This keeps automatic routing while preventing skill drift: edit a Levyra skill
once under `.agents/skills/`; Codex uses it directly and Claude sees the synced
native projection.

## Canonical skill inventory

- `levyra-android-intent-security`
- `levyra-android-performance`
- `levyra-android-reverse-engineering`
- `levyra-ci-workflows`
- `levyra-codex-bootstrap`
- `levyra-compose`
- `levyra-context-efficiency`
- `levyra-database`
- `levyra-design-taste`
- `levyra-desktop`
- `levyra-engineering`
- `levyra-extractor`
- `levyra-humanizer`
- `levyra-motion-artwork`
- `levyra-openclaw-orchestrator`
- `levyra-player`
- `levyra-pr-review`
- `levyra-project-manager`
- `levyra-r8-proguard`
- `levyra-real-engineering`
- `levyra-release-check`
- `levyra-security-review`

Skill descriptions are routing metadata; `SKILL.md` bodies are loaded only when
the task requires them. Keep descriptions narrow enough to avoid unnecessary
context expansion.

## Cross-runtime routing

The main automatic routes remain:

- substantial implementation: `levyra-real-engineering`;
- Compose/UI: `levyra-compose` and, when visual quality is central,
  `levyra-design-taste`;
- performance/memory/jank: `levyra-android-performance`;
- R8/minification: `levyra-r8-proguard`;
- Intent/component boundaries: `levyra-android-intent-security` plus
  `levyra-security-review`;
- CI/workflows: `levyra-ci-workflows`;
- noisy command-output work: `levyra-context-efficiency`;
- reviews: `levyra-pr-review`;
- releases: `levyra-release-check`;
- security-sensitive changes: `levyra-security-review`.

Security work follows the **Codex Security** closed-loop workflow documented in
`docs/ai/CODEX_SECURITY.md`: threat model, identification, validation,
remediation, human review, and revalidation.

## Codex

Codex uses root `AGENTS.md` as its repository operating contract and discovers
the canonical `.agents/skills/` tree directly. Levyra keeps the existing
instruction-based Codex setup for optional tooling and plugins.

Canonical project config and lifecycle hooks live under `.agents/codex/` and are
projected to ignored `.codex/` native paths. Do not maintain a second tracked
Codex config tree.

## Claude Code

Canonical Claude Code instructions, settings, subagents, hooks, and rules live
under `.agents/claude/`. Claude's native `.claude/` projection is generated
locally. `.claude/skills/` comes directly from the shared `.agents/skills/` tree;
there is no tracked `.agents/claude/skills/` bridge.

See `.agents/claude/README.md` for the detailed projection and bootstrap contract.

## Context efficiency

Use `levyra-context-efficiency` when large command output would otherwise waste
context. RTK/jCodeMunch are optimization layers only: correctness, exact failure
evidence, security output, and validation results must remain complete when
needed. Re-run raw commands whenever compressed output hides decisive evidence.

## Security

Use `levyra-security-review` for secrets, authentication, provider URLs,
redirects, SSRF, permissions, privacy, workflow trust boundaries, dependency
risk, update verification, or other security-sensitive changes. Never weaken
security merely to make a test or provider response pass.

## Maintenance contract

When changing agent infrastructure:

1. keep `.agents/skills/` as the only tracked Levyra skill tree;
2. keep Claude-specific canonical configuration under `.agents/claude/`;
3. keep Codex-specific canonical configuration under `.agents/codex/`;
4. never commit root `CLAUDE.md`, `.claude/`, `.codex/`, or
   `.agents/claude/skills/`;
5. keep `scripts/sync_agent_runtime.py` idempotent and non-destructive to
   unrelated local files;
6. update runtime setup/hooks and validators together when a native path changes;
7. run `python3 scripts/ai_quality_gate.py --profile fast` for focused agent
   changes and the repository-required full gate before publication/merge;
8. do not claim automatic discovery, a build, test, device check, or security
   validation unless the current evidence proves it.
