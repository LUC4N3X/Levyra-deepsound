# Levyra workspace rule

@../../AGENTS.md

This is a thin Antigravity-specific bridge. The imported root `AGENTS.md` is
authoritative for the instruction hierarchy, the complete "Native skill
routing" table, the work method, the quality gate, and publication
boundaries — do not restate that content here. Apply
`../../docs/ai/AI_ENGINEERING_GUARDRAILS.md` before production-code
implementation or broad review, apply
`../../docs/ai/EVIDENCE_GATED_COMPLETION.md` to non-trivial work, and apply
`../../docs/ai/ALWAYS_ON_AGENT_GUARDS.md` to every engineering task.

## Non-optional harness contract

The always-on guard document is not a skill and must never depend on Gemini or
Antigravity deciding whether it is relevant. Keep this workspace rule **Always
On**. Root/scoped instructions, automatic matching-skill routing,
current-file-before-mutation, acceptance gates, final-diff review,
anti-AI-comment discipline, structural navigation ordering, and
compaction/resume re-anchoring apply continuously.

When the runtime exposes lifecycle enforcement equivalent to pre/post tool,
compaction, or stop hooks, use the checked-in Levyra harness rather than
reimplementing the policy from memory. When it does not, follow the same
contract directly and treat repository validators/CI as the enforcement
backstop.

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

For any other task-required tool, follow the narrow installation authorization
in `ALWAYS_ON_AGENT_GUARDS.md`: verify it is missing, install only that useful
dependency from a trusted upstream, prefer user/project scope, verify it after
installation, and never turn this into a broad package upgrade or admin/root
escalation.

When prior-session context is materially useful and claude-mem tools are
absent, run the pinned dedicated claude-mem setup once automatically, then
retry the focused lookup. If setup or health checks fail, continue without
memory. See `../../docs/ai/CLAUDE_MEM.md`.

## Skills discovered here

Antigravity reads skills from `.agents/skills/<skill-name>/SKILL.md`; open the
repository root as the workspace so discovery is complete, and keep this rule
**Always On** when an activation control is available.

Select every matching skill automatically from the task itself using root
`AGENTS.md`'s "Native skill routing" table. Never require the owner to name a
skill. When shell execution is appropriate, `scripts/agent_skill_router.py` is
the shared deterministic reference used by Claude and Codex; Antigravity should
produce equivalent matches. Follow each referenced
`.agents/skills/*/SKILL.md` procedure directly rather than reconstructing it
from memory.

The shared automatic routes include `levyra-real-engineering`, `levyra-compose`,
`levyra-design-taste`, `levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, and `levyra-release-check`.

This includes `levyra-android-reverse-engineering` for APK/XAPK/AAB/DEX/JAR/AAR,
jadx/smali, compiled API extraction, binary call-flow, or Kotlin/R8 metadata
analysis; pair it with `levyra-security-review` and with `levyra-r8-proguard`
when obfuscation/shrinker behavior matters. The canonical
`levyra-real-engineering`, `levyra-design-taste`, and reverse-engineering adapters
remain authoritative over upstream packages.

For security-sensitive work, load `levyra-security-review` (paired with
`levyra-android-intent-security` for Android component-boundary work) and
follow the shared cycle: threat model, identification, safe validation, minimal
remediation, human review, revalidation. RTK reduces command output; it is not
validation authority — rerun the exact command raw whenever compact output
hides required evidence. Keep exploit evidence, hashes, signatures, secret
scans, and signing evidence raw.

## Evidence-gated completion

For non-trivial work, define the smallest useful acceptance gates before editing.
Each required gate needs a condition, a proving check, and direct evidence.
`FAIL`, `BLOCKED`, and `UNRUN` are not passes. Re-run a gate when later edits can
invalidate its evidence, then inspect the final diff and perform the required
pre-delivery review before calling implementation complete.

Keep this lightweight: do not create tracking files or abstractions merely to
record gates. Use `docs/project/TASKS.md` only when the work is already a tracked
multi-phase project.

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
