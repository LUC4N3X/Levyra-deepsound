# Levyra Claude Code Runtime

Root `CLAUDE.md` imports `AGENTS.md`, the cross-runtime source of truth. This
file adds Claude-specific behavior only. Current repository evidence and
root/scoped `AGENTS.md` outrank memory, stale comments, and prior agent output.
Use `docs/ai/AI_ENGINEERING_GUARDRAILS.md` when its detailed procedure is
needed and `docs/ai/EVIDENCE_GATED_COMPLETION.md` for non-trivial completion.

## Immediate context budget

- Treat `only this`, `solo questo`, and equivalents as hard scope boundaries.
- Inspect current code/tests before edits and use the smallest coherent root-cause fix.
- Do not add explanatory source-code comments.
- Publication, merge, release, version, deployment, and repository-setting
  actions remain owner-controlled.
- Before broad reading, search narrowly, read the smallest useful range, avoid
  rereading unchanged evidence, and load only routed skills.
- RTK and other optional tooling may save context but never reduce validation.

## Jev decision acceleration

When Jev is available, prefer it before manually classifying, ranking, scoring,
routing, or batch-checking more than a handful of independent items. Good uses
include CI triage, ranking candidate files or regressions, classifying review
findings, scoring comparable items, and checking batches of narrow claims.

Do not use Jev when an exact repository read, compiler, test, or static analysis
can settle the fact directly, or for architecture, implementation, root-cause
reasoning, security-sensitive multi-factor decisions, or final code review.

Jev rules:

1. gather current raw evidence first;
2. send the minimum deciding excerpt, not Claude's summary;
3. batch comparable items and keep questions narrow;
4. include an `other`/`unclear` path when categories may be incomplete;
5. auto-act only on low-risk results explicitly returned as `decision: auto`
   or clear yes/no verdicts;
6. manually inspect `review`, `uncertain`, low-confidence, truncated,
   malformed, destructive, security-sensitive, or high-impact results;
7. verify final conclusions against current code and Levyra invariants.

Jev confidence is not proof. Never send credentials, tokens, cookies, signing
material, secret URLs, `local.properties`, `.env`, keystores, personal data,
or unrelated private content. Jev payloads are external data egress. If Jev is
unavailable or fails, retry at most once and fall back to native Claude tools.
Jev never authorizes destructive or publication actions.

## Deterministic skill loading

`UserPromptSubmit` runs the shared router. Every item under **Mandatory skill load**
must be loaded before broad repository reading, editing, or shell work. Do not
scan or preload the full skill tree.

Compatibility inventory:
`levyra-real-engineering`, `levyra-compose`, `levyra-design-taste`,
`levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, `levyra-release-check`,
`levyra-player`, `levyra-extractor`, `levyra-database`, `levyra-desktop`,
`levyra-security-review`, `levyra-project-manager`, `levyra-engineering`,
`levyra-humanizer`.

## Subagent token discipline

Subagents already receive project context. Delegate only the goal, verified
evidence, affected files/symbols, invariants, acceptance checks, and blockers.
Do not paste the parent transcript, whole files, broad summaries, or unused
skills. Use custom agents only when isolated context materially helps.

## Hooks and resilience

`SessionStart` refreshes optional runtime tooling. `UserPromptSubmit`
re-anchors the hard contract, Jev policy, and skill routing. Mutation,
compaction, and Stop hooks preserve scope and evidence. If optional RTK,
jCodeMunch, memory, Jev, or projection setup fails, continue with native tools.

## Review, validation, and PRs

After the latest material edit, run focused validation, inspect the final diff,
run `git diff --check`, and use `/code-review` when available.

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

`fast` is required before commit and `full` before push/PR publication when
those actions are authorized. Missing prerequisites are blocked, not passed.
Preserve `.github/pull_request_template.md`, keep validation claims truthful, and apply
`levyra-humanizer` only as the final prose pass.
