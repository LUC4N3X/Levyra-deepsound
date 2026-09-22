# Levyra Claude Code Runtime

Root `CLAUDE.md` is Claude Code's native startup bridge and imports root
`AGENTS.md`, the cross-runtime source of truth. This file adds only
Claude-specific runtime behavior. Do not duplicate the full repository contract
here and do not preload large `docs/ai/` playbooks wholesale.

The generated `.claude/` projection may load this file, hooks, rules, agents, and
skills after `scripts/sync_agent_runtime.py` runs. The tracked root bridge means
Claude still receives the essential Levyra contract even when that projection
has not been created yet.

## Core Claude contract

- Current repository evidence outranks memory, stale comments, prior agent
  output, and old task status.
- Root/scoped `AGENTS.md` instructions remain authoritative.
- For production implementation or broad review, consult
  `docs/ai/AI_ENGINEERING_GUARDRAILS.md` when its detailed decision procedure is
  needed.
- Apply `docs/ai/EVIDENCE_GATED_COMPLETION.md` to non-trivial completion.
- `only this`, `solo questo`, and equivalents are hard scope boundaries.
- Do not add explanatory source-code comments. Prefer clear names and structure.
- Commit, push, PR, merge, tag, release, deployment, version changes, and
  repository settings remain owner-controlled.

## Immediate context budget

Before broad reading:

1. identify the likely owner/module and exact question the next read answers;
2. search path, symbol, filename, or call site first;
3. read the smallest useful range, focused diff, or nearby test;
4. expand only when a concrete unanswered question remains;
5. do not reread unchanged evidence already in context;
6. load only the skills routed for this task.

Use `levyra-context-efficiency` for noisy builds, tests, lint, logs, broad
searches, dependency listings, Git/GitHub/CI output, or other high-volume
context. RTK is optional optimization; rerun raw when exact diagnostics,
security/signing, Perfetto, or R8 evidence matters.

## Jev decision acceleration

Treat Jev as the default accelerator for repetitive narrow judgments whenever it
is available. Before manually classifying, ranking, scoring, routing, or checking
more than a handful of independent items, consider Jev first.

Use Jev by default for:

- CI/test failure triage across multiple failures;
- ranking candidate files, symbols, search hits, or regressions by relevance;
- classifying multiple review findings, issues, logs, commits, or messages;
- checking batches of narrow yes/no claims against the same evidence;
- scoring severity, priority, confidence, or quality across many comparable
  items;
- screening large external result sets before deeper Claude analysis;
- verifying PR-description claims against the actual diff when several claims
  must be checked.

Do not use Jev when:

- a lookup, compiler, test, static analysis, or repository read can settle an
  exact fact directly;
- there is only one obvious item and Claude already has the deciding evidence;
- the answer requires prose, code generation, architecture, root-cause
  reasoning, or a multi-factor engineering decision;
- the judgment would be unsafe to reduce to a fast classifier.

### Jev evidence rules

When using Jev:

1. gather current raw evidence first from the repository, diff, logs, tests, CI,
   or source material;
2. send the minimum raw excerpt that contains the deciding evidence, not
   Claude's summary or conclusion;
3. frame one narrow judgment per question or item;
4. batch comparable items instead of making one call per item;
5. include an `other`, `unclear`, or equivalent catch-all when categories may
   not cover every case;
6. act automatically only on results explicitly returned as `decision: auto`
   or clear `yes/no` verdicts when the action is low risk;
7. manually inspect every `review`, `uncertain`, low-confidence, truncated,
   malformed, security-sensitive, destructive, or high-impact result;
8. raise thresholds or require manual verification when a wrong answer is
   expensive;
9. verify final conclusions against current code and Levyra invariants before
   editing, publishing, or claiming completion.

Jev confidence is evidence about the classifier's certainty, not proof that the
workflow or conclusion is correct. Exact validation still belongs to tests,
tooling, repository evidence, and Claude's engineering review.

### Jev privacy and safety

Jev sends only the payload supplied to it to TypeSafe. Treat that payload as
external data egress.

Never send:

- `TYPESAFE_API_KEY` or any credential, token, cookie, signing material, or
  secret;
- `local.properties`, `.env`, keystores, private tokens, or secret URLs;
- personal or unrelated private data;
- more repository content than the judgment actually needs.

Never write Jev credentials into tracked files, prompts intended for
publication, logs, PR bodies, or repository configuration.

If Jev is unavailable, misconfigured, rate-limited, or fails, continue with
native Claude tools without blocking the task. Retry an invalid or transient Jev
failure at most once before falling back. Never weaken validation, security, or
scope to make Jev work.

Jev may accelerate analysis but may never authorize destructive actions,
publication, merges, releases, version changes, repository setting changes, or
other owner-controlled actions.

## Deterministic skill loading

`UserPromptSubmit` runs the shared router. Every item reported under
**Mandatory skill load** must be loaded before broad repository reading, editing,
or shell work. The owner never needs to name a skill.

Compatibility inventory for automatic routes:
`levyra-real-engineering`, `levyra-compose`, `levyra-design-taste`,
`levyra-android-performance`, `levyra-r8-proguard`,
`levyra-android-intent-security`, `levyra-ci-workflows`,
`levyra-context-efficiency`, `levyra-pr-review`, `levyra-release-check`.
The shared router may additionally select `levyra-player`, `levyra-extractor`,
`levyra-database`, `levyra-desktop`, `levyra-security-review`,
`levyra-project-manager`, `levyra-engineering`, `levyra-humanizer`, and other
focused Levyra skills.

Do not scan or preload the whole skill tree. Invoke only the routed skill bodies.

## Subagent token discipline

Subagents already receive project context. Delegate with only the goal, current
verified evidence, affected files/symbols, task-specific invariants, acceptance
checks, and real blockers. Do not paste the parent transcript, entire files,
broad repository summaries, or unused skill bodies into subagent prompts.

Use built-in Explore for broad read-only discovery when appropriate. Use custom
implementation/review agents only when isolated context materially improves the
work; do not spawn them ceremonially for tiny edits.

## Hooks and resilience

Claude lifecycle hooks are a second enforcement layer, not the only source of
instructions:

- `SessionStart` refreshes optional runtime projection/tooling and re-anchors
  active state;
- `UserPromptSubmit` injects the compact Levyra hard-contract reminder,
  Jev-routing policy, and deterministic skill routing on every user turn;
- mutation hooks enforce scoped-instruction/current-file freshness where
  supported;
- compaction hooks re-anchor open task state;
- the Stop audit checks evidence before completion.

If optional RTK, jCodeMunch, memory, Jev, or projection setup fails, continue
with native tools and report the limitation once. Never weaken safety or
validation to make optional tooling work.

## Review, validation, and PRs

After the latest material code edit, run focused validation, inspect the actual
final diff, and run `git diff --check`. For meaningful changes invoke
`/code-review` when available; otherwise use the repository review stage. Fix
valid findings and review the corrected diff again when needed.

Use:

```bash
python3 scripts/ai_quality_gate.py --profile fast
python3 scripts/ai_quality_gate.py --profile full
```

`fast` is required before commit and `full` before push/PR publication when those
actions are authorized. Missing prerequisites remain blocked, not passed.

When opening or updating a pull request, preserve the complete
`.github/pull_request_template.md`, keep every validation claim truthful, leave
unperformed checks unmarked, and apply `levyra-humanizer` as the final prose
pass without changing facts.
