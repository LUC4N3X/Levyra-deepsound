# Levyra workspace rule

@../../AGENTS.md

Treat the repository-root `AGENTS.md` as Levyra's authoritative operating
contract. Before investigating, editing, reviewing, or running commands:

1. apply the nearest path-specific `AGENTS.md` files for every file in scope;
2. read only relevant approved planning material under `docs/project/`;
3. discover and load every matching `levyra-*` skill under `.agents/skills/`;
4. read `../../docs/ai/AI_ENGINEERING_GUARDRAILS.md` before production-code
   implementation or broad review and apply its architecture-first, reuse-first,
   complexity-budget, scope-checkpoint, and diff-quality rules;
5. automatically load `levyra-real-engineering` for non-trivial features,
   architectural changes, unclear defects, or multi-step work where requirements
   and implementation should be separated; use only the stages needed and skip
   the full workflow for tiny, already-unambiguous changes;
6. automatically load `levyra-context-efficiency` for builds, tests, lint,
   logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, setup, or any
   command expected to produce large repetitive output;
7. automatically load `levyra-security-review` for vulnerability scans,
   attacker-controlled input, trust-boundary changes, authentication, secrets,
   permissions, privacy, dependency risk, update integrity, or security-related
   pull requests;
8. when RTK is available, use it selectively for noisy supported commands,
   verify exit status and success/failure markers, and rerun the exact command
   raw whenever compact output hides required evidence;
9. keep exploit evidence, security validation, hashes, signatures, secret scans,
   signing, and exact reproduction output raw;
10. inspect current code, tests, architecture, build files, dependencies, and
    workflows before relying on memory or previous agent output;
11. for security work, follow threat model, identification, safe validation,
    minimal remediation, human review, and revalidation;
12. identify the current architecture owner and expected production files before
    editing, then make the smallest coherent change and preserve unrelated
    behavior;
13. if the implementation crosses an AI guardrail scope checkpoint, re-evaluate
    and split the work instead of expanding autonomously unless the owner
    explicitly approves the larger scope;
14. run focused validation first, then
    `python3 scripts/ai_quality_gate.py --profile fast` before commit and
    `python3 scripts/ai_quality_gate.py --profile full` before push or PR;
15. inspect final diff statistics and the complete diff for unnecessary code
    growth, duplicate ownership, speculative abstractions, generated churn, and
    unrelated edits;
16. treat every blocked or skipped required gate check as not passed;
17. keep implementation, validation, review, publication, merge, and release as
    separate states.

For real-engineering work, follow
`../../.agents/skills/levyra-real-engineering/SKILL.md`. When the upstream Matt
Pocock package is available, load the exact stage skill selected by that adapter
instead of reconstructing it from memory. The adapter remains authoritative for
Levyra scope, architecture, validation, and publication boundaries.

RTK reduces command output; it is not validation authority and its savings are
not equal to total model-billing savings. Security-engine findings and generated
patches are proposals that require evidence, complete diff review, CI, and
explicit owner-controlled publication.

Never commit, push, open or merge a pull request, tag, publish, release, change
versions, or modify repository settings without explicit owner authorization for
the exact action and scope.

This lightweight bridge gives Codex, Antigravity, OpenCode, OpenClaw, and other
compatible workspaces one source of truth while discovering Levyra's real-
engineering, context-efficiency, AI engineering guardrails, and security-review
workflows automatically.
