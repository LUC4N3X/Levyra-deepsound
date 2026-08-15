# Levyra workspace rule

@../../AGENTS.md

This is a thin Antigravity-specific bridge. The imported root `AGENTS.md` is
authoritative for the instruction hierarchy, the complete "Native skill
routing" table, the work method, the quality gate, and publication
boundaries — do not restate that content here. Apply
`../../docs/ai/AI_ENGINEERING_GUARDRAILS.md` before production-code
implementation or broad review.

## Antigravity-specific automation

Ensure the owner-authorized pinned RTK build before the first noisy
shell-capable command, without asking the owner to run setup:

- Windows PowerShell: `./scripts/ensure-rtk.ps1 -Quiet`;
- Bash/WSL/Linux/macOS: `./scripts/ensure-rtk.sh --quiet`.

These scripts are idempotent: they only install `rtk-ai/rtk` when
`rtk --version` and `rtk gain` do not already prove the tool is ready. If Cargo
is unavailable or installation fails, continue with raw commands and report the
limitation once. Never weaken sandboxing or approval controls to make RTK
install.

When prior-session context is materially useful and claude-mem tools are
absent, run the pinned dedicated claude-mem setup once automatically, then
retry the focused lookup. If setup or health checks fail, continue without
memory. See `../../docs/ai/CLAUDE_MEM.md`.

## Skills discovered here

Antigravity reads skills from `.agents/skills/<skill-name>/SKILL.md`; open the
repository root as the workspace so discovery is complete, and keep this rule
**Always On** when an activation control is available.

Select every matching skill from the task itself using root `AGENTS.md`'s
"Native skill routing" table, including `levyra-real-engineering`,
`levyra-compose`, `levyra-design-taste`, `levyra-android-performance`,
`levyra-r8-proguard`, `levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, and `levyra-release-check`.
Never require the owner to name a skill. Follow the referenced
`.agents/skills/*/SKILL.md` procedure directly; do not reconstruct it from
memory. The canonical `levyra-real-engineering` and `levyra-design-taste`
adapters remain authoritative over any upstream package.

For security-sensitive work, load `levyra-security-review` (paired with
`levyra-android-intent-security` for Android component-boundary work) and
follow the shared cycle: threat model, identification, safe validation, minimal
remediation, human review, revalidation. RTK reduces command output; it is not
validation authority — rerun the exact command raw whenever compact output
hides required evidence. Keep exploit evidence, hashes, signatures, secret
scans, and signing evidence raw.

## Validation and publication

Run focused validation first, then
`python3 scripts/ai_quality_gate.py --profile fast` before commit and
`python3 scripts/ai_quality_gate.py --profile full` before push or PR. Treat
every blocked or skipped required check as not passed, and keep
implementation, validation, review, publication, merge, and release as
separate states.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization
for the exact action and scope.
