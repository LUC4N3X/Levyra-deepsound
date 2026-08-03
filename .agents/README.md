# Levyra OpenAI Agent Configuration

This directory contains Levyra's repository-local native skills for Codex and compatible OpenAI coding-agent workflows.

## Why `AGENTS.md` is in the repository root

Codex discovers project instructions by starting at the Git root and walking toward the current working directory. Therefore:

- root `AGENTS.md` is the automatic repository-wide contract;
- `app/AGENTS.md`, `desktop/AGENTS.md`, `.github/AGENTS.md`, and `docs/AGENTS.md` provide narrower path-specific instructions;
- `.agents/skills/` contains task procedures and must not replace the root instruction file.

Moving the root contract into `docs/` or `.agents/` would make it ordinary documentation rather than guaranteed repository-root guidance.

## Configuration layers

```text
AGENTS.md                         repository-wide Codex contract
app/AGENTS.md                     Android-specific rules
desktop/AGENTS.md                 Windows Desktop rules
.github/AGENTS.md                 CI and workflow security rules
docs/AGENTS.md                    documentation rules
.agents/skills/*/SKILL.md         native task skills
docs/ai/                          ChatGPT Project setup material
.claude/                          Claude Code configuration and detailed playbooks
```

## Native skills

| Skill | Primary use |
| --- | --- |
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

Focused work should use the most specific skill. Several skills may be loaded for one change.

## Codex workflow

Start Codex from the repository root or a subdirectory inside the repository.

Expected behavior:

1. Codex loads the root `AGENTS.md`.
2. It adds every nearer `AGENTS.md` covering the working directory.
3. It selects the matching native skill or skills.
4. The skill points to relevant current code, tests, architecture, and detailed Levyra playbooks under `.claude/`.
5. Codex makes the smallest coherent change and reports validation truthfully.

Example prompts:

```text
Use the levyra-player and levyra-extractor skills. Trace why playback sometimes resolves slowly, identify the root cause, and propose the smallest compatible fix before editing.
```

```text
Use levyra-pr-review and review the current diff. Put evidence-backed findings first and distinguish tested behavior from manual checks.
```

## ChatGPT

A normal ChatGPT conversation does not automatically treat repository files as persistent Project instructions. Create a Levyra ChatGPT Project, connect the repository, and paste the content of `docs/ai/CHATGPT_PROJECT_INSTRUCTIONS.md` into the Project instructions.

ChatGPT should use the repository for product decisions, investigation, architecture, planning, and review. Codex should perform implementation and publication work when authorized.

## Claude Code

Claude Code continues to use `.claude/CLAUDE.md`, `.claude/rules/`, `.claude/skills/`, `.claude/agents/`, `.claude/settings.json`, and `.claude/hooks/`.

The detailed `.claude/skills/` and `.claude/rules/` files remain useful as shared Levyra engineering playbooks. OpenAI skills reference them instead of duplicating their full content.

## Maintenance rules

- Keep repository-wide invariants concise in root `AGENTS.md`.
- Put path-specific constraints in the nearest `AGENTS.md`.
- Keep each native skill focused on one repeatable job.
- Put architecture in `docs/ARCHITECTURE.md`.
- Update the narrowest detailed playbook when a recurring project-specific failure is discovered.
- Do not duplicate entire instructions across assistant-specific trees.
- Verify every referenced file, command, version location, workflow, and artifact path after structural changes.
