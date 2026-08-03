---
name: levyra-openclaw-orchestrator
description: Coordinate Levyra work through OpenClaw using a dedicated repository workspace, explicit agent delegation, project planning under docs/project/, domain skills, coding runtimes, independent review, evidence collection, and owner-controlled publication.
---

# Levyra OpenClaw orchestration workflow

## Role

OpenClaw coordinates bounded work. It does not replace Levyra requirements,
domain skills, coding runtimes, CI, independent review, or owner approval.

Use a dedicated `levyra` agent whose workspace is the real Levyra checkout.
Delegate with an explicit target agent and repository working directory.

## Before delegation

1. Read root and applicable nested `AGENTS.md`.
2. Read `docs/project/SPEC.md`, `docs/project/ROADMAP.md`, and
   `docs/project/TASKS.md`.
3. Load `levyra-project-manager` and every matching domain skill.
4. Inspect current branch, worktree, open pull request, review state, and
   validation evidence.
5. Confirm the exact outcome, scope boundary, and publication authorization in
   the current owner request.

Do not reuse stale branch, PR, review, or CI assumptions.

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

## Execution sequence

1. Delegate orientation or planning to the `levyra` agent.
2. Use one coding runtime for one reviewable implementation phase.
3. Run focused checks and inspect the diff.
4. Run applicable repository gates.
5. Delegate independent latest-commit review to a fresh reviewer.
6. Return actionable findings to the implementation runtime.
7. Re-run affected checks after every change.
8. Publish only to a dedicated branch and draft PR when authorized.
9. Return exact status to the coordinator and owner.
10. Stop before merge, tag, release, store upload, or repository-setting changes
    unless separately authorized for that exact action.

## Tool policy

The Levyra agent may use repository, command, Git, build/test, GitHub PR, review,
and CI tools needed for the current task.

Keep personal messaging, email, calendar, unrelated browser accounts,
system-wide administration, release credentials, signing material, and
repository settings outside the Levyra agent unless the owner explicitly
requires and authorizes a narrowly scoped action.

Prefer separate agents for research and system administration. Do not use a
wildcard target allowlist when a narrow list is sufficient.

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

Never collapse several states into "done".

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
