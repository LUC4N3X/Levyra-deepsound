# Levyra Agent Configuration

`.agents/` is Levyra's tracked control center for runtime-specific coding-agent
configuration and the canonical skill tree. Root `AGENTS.md` is the compact
cross-runtime contract.

Claude Code is the deliberate exception to the otherwise generated native
surfaces: tracked root `CLAUDE.md` is a tiny native bootstrap that imports
`AGENTS.md`. This removes the clean-clone/startup dependency on a pre-existing
`.claude/` projection. It must stay small and must not duplicate the contract.

## Configuration hierarchy

```text
AGENTS.md                         compact cross-runtime contract
CLAUDE.md                         tiny Claude-native @AGENTS.md bridge
app/AGENTS.md                     Android scoped rules
desktop/AGENTS.md                 Windows Desktop scoped rules
.github/AGENTS.md                 CI/workflow scoped rules
docs/AGENTS.md                    documentation scoped rules

.agents/
├── README.md                     this inventory
├── config/                       shared agent/plugin manifests
├── rules/                        cross-runtime workspace bridges
├── skills/                       one canonical Levyra skill tree
├── claude/                       canonical Claude settings/hooks/agents/rules
│   ├── CLAUDE.md                 Claude-specific runtime guidance
│   ├── settings.json
│   ├── agents/
│   ├── hooks/
│   └── rules/
└── codex/                        canonical Codex project config/hooks
    ├── config.toml
    └── hooks.json
```

Generated `.claude/` and `.codex/` directories remain ignored by Git and are
never sources of truth.

## Claude Code reliability

Claude Code natively reads root `CLAUDE.md`, which imports `AGENTS.md` at session
startup. That bootstrap works before any repository setup script or generated
runtime projection exists.

When `.claude/settings.json` is available, `UserPromptSubmit` adds a second
layer: it re-anchors a compact hard contract on every prompt and runs the shared
skill router. Session/mutation/compaction/stop hooks add evidence and safety
guards, but correctness must not depend on optional tooling being available.

The generated `.claude/` projection still supplies project settings, hooks,
agents, rules, and the native skill view. Refresh it with:

```powershell
.\scripts\setup-ai.ps1
```

or:

```bash
./scripts/setup-ai.sh
```

The projection manager is `scripts/sync_agent_runtime.py`.

## Automatic skill discovery

`.agents/skills/` is the only tracked Levyra skill tree.

- **Codex** discovers `.agents/skills/` directly and keeps the existing
  instruction-based Codex setup.
- **Claude Code** receives the root contract natively and discovers the generated
  `.claude/skills/` projection when runtime setup is active.
- **Google Antigravity** uses `.agents/rules/levyra-workspace.md` and the same
  repository-native skills.
- **ChatGPT Project** routes through `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md`.
- OpenClaw and compatible runtimes use the same contracts where supported.

Skill descriptions are routing metadata. Load `SKILL.md` bodies only when the
active task matches them; never preload the whole tree.

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

## Context efficiency

`AGENTS.md` intentionally stays concise. Detailed multi-step procedure belongs in
path-scoped instructions, `docs/ai/`, or a matching skill. Use
`levyra-context-efficiency` when command output or repository exploration would
otherwise flood the active context. RTK/jCodeMunch are optimization layers only;
rerun raw when compact output hides decisive evidence.

## Security

Use `levyra-security-review` for secrets, authentication, provider URLs,
redirects, SSRF, permissions, privacy, workflow trust boundaries, dependency
risk, update verification, or other security-sensitive changes. The shared
**Codex Security** lifecycle is documented in `docs/ai/CODEX_SECURITY.md`.
Never weaken security merely to make a test or provider response pass.

## Maintenance contract

When changing agent infrastructure:

1. keep `AGENTS.md` compact and root `CLAUDE.md` as a tiny import bridge;
2. keep `.agents/skills/` as the only tracked Levyra skill tree;
3. keep Claude-specific canonical runtime configuration under `.agents/claude/`;
4. keep Codex-specific canonical configuration under `.agents/codex/`;
5. never commit generated `.claude/`, `.codex/`, or
   `.agents/claude/skills/` trees;
6. keep runtime projection idempotent and non-destructive to unrelated local
   files;
7. update hooks, validators, and docs together when discovery behavior changes;
8. run `python3 scripts/ai_quality_gate.py --profile fast` for focused agent
   changes and the required full gate before publication/merge;
9. never claim automatic discovery, builds, tests, device checks, or security
   validation without direct evidence.
