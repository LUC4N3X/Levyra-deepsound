---
name: levyra-openclaw-orchestrator
description: Coordinate Levyra work through OpenClaw using a dedicated repository workspace, compact delegation, specialized review and CI agents, project-native skills, evidence collection, durable memory, and owner-controlled publication.
---

# Levyra OpenClaw orchestration workflow

## Role

OpenClaw coordinates bounded work. It does not replace Levyra requirements,
domain skills, coding runtimes, CI, independent review, or owner approval.

Use the existing Levyra worker as the primary implementation/orchestration agent.
Use `levyra-reviewer` for independent review and `levyra-ci` for CI, PR-state,
logs, and validation evidence. Do not add more agents unless a repeated workload
has a distinct context, permission, or evidence boundary that these three cannot
cover cleanly.

## Context economy

Apply `levyra-context-efficiency` before delegation.

- Primary work keeps requirements, current architecture, root cause, changed
  files, and focused validation.
- Reviewer handoff contains the requested outcome, latest diff or commit, the
  smallest relevant surrounding code, invariants, and known test evidence.
- CI handoff contains PR/SHA, required checks, failing job/step, and only the log
  ranges that determine the conclusion.
- Do not send full chat history, repeated repository orientation, successful
  repetitive logs, or unchanged files to another agent.
- Expand context only when the receiving agent identifies a concrete unanswered
  question.
- Keep security, signing, R8, Perfetto, release, protocol, and exact failure
  evidence raw whenever compression could change the conclusion.

A fresh specialized agent with a compact verified handoff is preferred over
carrying a long exploratory session into review or CI diagnosis.

## Memory policy

Memory is evidence, not a second source of truth.

Promote only durable, verified information such as recurring failure patterns,
stable architecture ownership, validated diagnostic techniques, and explicit
owner preferences. Keep transient branch heads, current PR state, CI status,
temporary hypotheses, generated logs, secrets, credentials, signing material,
and release tokens out of long-term memory.

Use dated memory for current work and `MEMORY.md` only for durable knowledge.
OpenClaw Dreaming may consolidate memory, but every promoted project fact remains
subordinate to current repository evidence.

## Before delegation

1. Read root and applicable nested `AGENTS.md`.
2. Read only the relevant sections of `docs/project/SPEC.md`,
   `docs/project/ROADMAP.md`, and `docs/project/TASKS.md`.
3. Load `levyra-project-manager`, `levyra-context-efficiency`, and every matching
   domain skill required by the task.
4. Inspect current branch, worktree, open pull request, review state, and
   validation evidence.
5. Confirm the exact outcome, scope boundary, and publication authorization in
   the current owner request.
6. Stop discovery when enough verified evidence exists to implement or review
   the requested outcome.

Do not reuse stale branch, PR, review, CI, model-summary, or memory assumptions.

## Delegated task contract

Every coding-runtime task must state:

- repository and working directory;
- objective and acceptance criteria;
- matching `AGENTS.md` files and native skills;
- verified current behavior and root cause or rationale;
- behavior that must remain unchanged;
- prohibited changes;
- expected files or modules;
- focused and broader checks;
- manual checks;
- required delivery evidence;
- whether branch, commit, push, or draft PR creation is authorized.

For `levyra-reviewer` and `levyra-ci`, every handoff must also include the exact target SHA and must tell the specialist to use its own workspace `./repo` checkout. Parent-agent relative paths and `spawnedCwd` are context only; they are never the specialist's evidence root. The specialist may fetch refs in its own checkout to resolve the requested SHA, but it must remain read-only.

A reviewer handoff must not assume a PR exists. A CI handoff must not assume GitHub Actions evidence is PR-scoped: when given a SHA, the CI agent must query Actions by that exact SHA and include push-triggered runs before concluding that CI is absent or unverifiable.

Keep the contract compact. Link or name canonical repository files instead of
copying large instruction blocks into the handoff.

## Execution sequence

1. Use the existing Levyra worker for orientation and implementation.
2. Use one coding runtime for one reviewable implementation phase.
3. Run focused checks and inspect the diff.
4. Run applicable repository gates.
5. Before code is presented as final, run the required `code-review` stage.
6. Delegate independent latest-diff review to `levyra-reviewer` with a fresh,
   bounded handoff that includes the exact target SHA and its own-checkout rule.
7. Return actionable findings to the implementation runtime and re-run affected
   checks after every material change.
8. Delegate CI, PR-state, and log diagnosis to `levyra-ci` with the exact target
   SHA; do not pollute the implementation context with broad CI output.
9. Publish only to an authorized branch/PR or directly to an explicitly
   authorized target.
10. Return exact status to the coordinator and owner.
11. Stop before merge, tag, release, store upload, or repository-setting changes
    unless separately authorized for that exact action.

## Specialized agents

### Primary Levyra worker

Owns implementation and orchestration. It may edit only within the requested
scope and may delegate review/CI work. It must never ask another agent to bypass
repository publication controls.

### `levyra-reviewer`

Independent and read-only by default. It reviews the latest requested diff and
surrounding ownership from its own `./repo` checkout. A PR is optional: commit
reviews must resolve the requested SHA and reconstruct the local patch when no
PR exists. It reports severity, confidence, exact location, triggering scenario,
consequence, smallest compatible fix, and missing regression coverage. It does
not implement its own findings or certify stale commits.

### `levyra-ci`

Read-only by default. It inspects PR/SHA state, required checks, Actions jobs,
exact failing steps, logs, review state, and reproducible validation evidence
from its own `./repo` checkout. For a SHA, it checks Actions runs keyed by exact
`head_sha`, including push runs, before reporting that CI is absent. It separates
stale runs from current-head evidence and returns only actionable state changes.

## Tool policy

The primary Levyra worker may use repository, command, Git, build/test, GitHub
PR, review, and CI tools needed for the current authorized task.

`levyra-reviewer` and `levyra-ci` should use read-only filesystem policies and
agent-scoped read-only sandboxes when the VPS supports them. They may use shell
commands only for inspection and non-mutating validation.

Keep personal messaging, email, calendar, unrelated browser accounts,
system-wide administration, release credentials, signing material, and
repository settings outside Levyra agents unless the owner explicitly requires
and authorizes a narrowly scoped action.

## Recurring audit

A twice-daily `levyra-ci` audit may inspect open PRs, required CI, unresolved
review threads, and stale branches. Use isolated `light-context` runs so routine
monitoring does not load the full workspace memory. The audit is read-only and
must not edit code, create branches, push, merge, release, or change settings.

Use `scripts/setup-openclaw-levyra.sh` to provision or refresh this profile on
the OpenClaw VPS. The script preserves an existing `levyra-worker` or `levyra`
primary agent and adds only the specialized reviewer and CI agents.

## Review rules

- A reviewer must inspect the latest diff and surrounding code.
- CodeRabbit is a signal, not the source of requirements.
- The implementation agent does not independently certify its own work.
- Every finding needs severity, confidence, exact location, triggering scenario,
  consequence, smallest compatible fix, and missing regression coverage.
- Stale findings are closed only after checking the current commit.
- Remaining findings require an evidence-backed explanation.

## Status vocabulary

Use these states precisely:

- planned;
- edited;
- locally validated;
- committed;
- pushed;
- pull request opened;
- CI passed;
- independently reviewed;
- merged;
- released.

Never collapse several states into `done`.

## Final handoff

Return:

- delegated agents and runtimes used;
- final scope;
- files changed;
- checks and exact results;
- blocked or skipped checks;
- review findings and resolution;
- branch and commit;
- pull request URL and current state;
- manual checks still required;
- explicit statement that merge and release did or did not occur.
