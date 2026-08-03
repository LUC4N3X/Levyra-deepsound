---
name: levyra-project-manager
description: Turn Levyra requirements and roadmap outcomes into one reviewable active phase using SPEC.md, ROADMAP.md, TASKS.md, acceptance criteria, validation, owner checkpoints, and domain-skill routing.
---

# Levyra project management workflow

## Use this skill when

- defining or updating a tracked engineering phase;
- reconciling requirements, roadmap, tasks, architecture, and code;
- preparing a precise implementation plan for Codex, Claude Code, or OpenClaw;
- deciding whether a change is ready to hand to PR review;
- updating task status from direct validation evidence.

Do not use this skill instead of a domain skill. Load every relevant player,
extractor, database, Compose, Desktop, security, CI, review, or release skill.

## Required context

1. Read the root and every applicable nested `AGENTS.md`.
2. Read `SPEC.md`, `ROADMAP.md`, and `TASKS.md`.
3. Read `docs/ARCHITECTURE.md` and relevant platform documentation.
4. Inspect the current branch, worktree, complete affected control/data flow,
   nearby tests, build files, workflows, and active review discussion.
5. Load every matching native domain skill and referenced detailed Levyra
   playbook.

Current repository evidence overrides stale task text. Surface conflicts between
requirements, roadmap, tasks, architecture, and implementation before editing.

## Phase definition

Create or revise one active phase with:

- exact outcome;
- roadmap track;
- scope and non-goals;
- behavior that must remain unchanged;
- verified current behavior and root cause when fixing a defect;
- acceptance criteria;
- expected files and modules;
- lifecycle, concurrency, persistence, security, localization, and release
  implications;
- focused automated checks;
- applicable broader checks;
- required manual checks;
- rollback or revert boundary;
- explicit owner checkpoints.

Remove irrelevant template material. Do not invent requirements to make a plan
look complete.

## Execution

1. Present the plan and stop only when the owner reserved approval or requested
   planning without implementation.
2. Execute one reviewable phase at a time.
3. Make the smallest coherent change compatible with current architecture.
4. Run focused checks during iteration.
5. Inspect the complete diff before broader validation.
6. Run applicable repository gates.
7. Update `TASKS.md` only from direct command, CI, review, device, or owner
   evidence.
8. Hand the latest diff to `levyra-pr-review`.
9. Use `levyra-release-check` only when merge or release readiness is actually in
   scope.
10. Publish a branch or draft pull request only when explicitly authorized.

## Safety rules

- Never rewrite owner-approved requirements without surfacing the change.
- Never activate several unrelated phases in `TASKS.md`.
- Never mark a check passed from an agent's narrative.
- Never treat blocked validation as passed.
- Never infer permission to push, open a PR, merge, tag, publish, release, or
  change repository settings.
- Never let planning files become a second source of implementation truth.
- Keep Android and Desktop version and release work independent.
- Keep project-specific facts in repository docs; keep this skill focused on the
  reusable process.

## Handoff

Return:

- requirement and roadmap mapping;
- final scope and non-goals;
- root cause or rationale;
- exact files changed;
- behavior preserved;
- checks with exact results;
- blocked and skipped checks;
- open risks and manual validation;
- `TASKS.md` status changes and evidence;
- branch, commit, and PR state;
- explicit merge/release state.
