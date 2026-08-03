---
name: levyra-engineering
description: Analyze, implement, debug, review, or validate changes in the Levyra Android music application. Use for any task involving Levyra code, architecture, playback, extraction, Compose, Room, networking, security, CI, pull requests, or releases.
---

# Levyra engineering workflow

## Start here

1. Read the root `AGENTS.md` completely.
2. Read `docs/ARCHITECTURE.md`.
3. Classify the task using the routing table below.
4. Read every matching procedure and rule before editing.
5. Inspect the relevant implementation and nearby tests.

Do not rely on a remembered version of Levyra. Current repository content is authoritative.

## Route the task

Several routes may apply at the same time. A playback change that also modifies stream resolution must use both the player and extractor procedures. A remote-media change may also require the security review procedure.

| Domain | Procedure | Supporting rules |
| --- | --- | --- |
| Playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video mode | `.claude/skills/levyra-player/SKILL.md` | `.claude/rules/player.md`, `.claude/rules/architecture.md` |
| InnerTube, extractor, stream resolution, player-config synchronization, retry, tokens, network fallback | `.claude/skills/levyra-extractor/SKILL.md` | `.claude/rules/extractor-network.md`, `.claude/rules/security.md` |
| Room entities, DAOs, migrations, schema, caches, stores, backup | `.claude/skills/levyra-database/SKILL.md` | `.claude/rules/data-room.md`, `.claude/rules/architecture.md` |
| Compose screens, state projections, animation, lifecycle, accessibility, localization | `.claude/skills/levyra-compose/SKILL.md` | `.claude/rules/compose-ui.md`, `.claude/rules/localization.md` |
| Decorative motion artwork | `.claude/skills/levyra-motion-artwork/SKILL.md` | `.claude/rules/player.md`, `.claude/rules/security.md` |
| Secrets, URLs, redirects, SSRF, MIME handling, permissions, privacy, workflow exposure | `.claude/skills/levyra-security-review/SKILL.md` | `.claude/rules/security.md` |
| Review of a branch, commit, patch, or pull request | `.claude/skills/levyra-pr-review/SKILL.md` | All rules relevant to the changed files |
| Pre-merge or pre-release validation, version values, signing, APK output, release workflows | `.claude/skills/levyra-release-check/SKILL.md` | `.claude/rules/testing-release.md` |

The procedure files are plain repository instructions. Ignore Claude-only metadata such as agent or tool declarations when the current environment does not support them; follow the engineering steps and safety requirements themselves.

## Establish the change contract

Before editing, identify:

- the exact user-visible or developer-visible problem;
- the current behavior and control flow;
- behavior that must remain unchanged;
- the likely root cause;
- the smallest set of files that should change;
- tests or evidence needed to prove the fix;
- checks that require an Android SDK, credentials, CI, emulator, physical device, Android Auto, notification access, or external network.

Do not expand the task into broad refactoring unless the existing architecture makes the requested fix impossible without it.

## Implement safely

- Keep playback requests ahead of artwork, lyrics, enrichment, prefetch, refresh, and diagnostics.
- Preserve the user's explicit audio/song versus native-video choice.
- Preserve Media3, MediaSession, notification, Android Auto, queue, and background-service synchronization.
- Keep I/O and orchestration outside composables.
- Keep blocking work off the main thread.
- Reuse existing clients, stores, caches, scopes, dispatchers, and lifecycle owners.
- Use identity and generation checks for late asynchronous results.
- Re-throw `CancellationException`.
- Do not turn transient failures into permanent misses.
- Preserve user data with explicit Room migrations.
- Validate provider-controlled URLs and redirects at every hop.
- Do not change version values, signing, publication, or workflow permissions unless explicitly requested.

## Validate proportionally

Run the narrowest relevant tests first. Then run the applicable repository checks from `AGENTS.md` when the environment supports them.

For documentation-only changes:

- verify all referenced repository paths exist;
- verify command examples match current build files and workflows;
- check Markdown structure and code fences;
- inspect the complete diff for accidental code, version, workflow, or generated-file changes.

For code changes:

- add regression tests that fail for the original defect and pass with the fix when practical;
- run focused tests before broad suites;
- use the repository Gradle wrapper;
- run `git diff --check`;
- report blocked and skipped checks explicitly.

## Review before delivery

Inspect the final diff and confirm:

- only intended files changed;
- no credentials, keystores, private configuration, APKs, ZIPs, or build output were added;
- no unrelated formatting or dependency churn occurred;
- no version value changed accidentally;
- tests match the changed behavior;
- comments and documentation describe the current implementation rather than an abandoned approach;
- manual checks are not represented as completed without evidence.

## Report

Provide:

1. root cause or rationale;
2. concise implementation summary;
3. files changed;
4. important behavior preserved;
5. tests and checks run with results;
6. checks not run and why;
7. residual risks and manual validation;
8. a professional commit message when requested.

Do not commit, push, open a pull request, merge, tag, or release unless the user explicitly authorizes that external action.
