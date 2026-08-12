---
name: levyra-pr-review
description: Review a Levyra branch, commit, patch, or pull request for correctness, regressions, concurrency, lifecycle, security, data safety, UI behavior, CI, release risk, and missing tests.
---

# Levyra pull request review workflow

## Required context

1. Read the root `AGENTS.md` and every nearest `AGENTS.md` covering changed files.
2. Read `.claude/skills/levyra-pr-review/SKILL.md` and all domain skills/rules relevant to the diff.
3. Inspect the complete diff, surrounding implementation, tests, build files, workflows, and current review discussion.
4. Ignore comments that no longer apply to the current head revision.

## Commit and branch review evidence

A review does not require a pull request. When the handoff names a commit or branch:

1. Work from the reviewer agent's own `./repo` checkout, not a parent agent's relative path or inherited working directory.
2. Fetch remote refs as needed and resolve the exact requested SHA before reviewing it.
3. For a normal commit, inspect the complete patch from its parent to the requested SHA plus the surrounding implementation and tests. For a root commit or explicitly supplied patch, use the available complete patch representation instead of assuming a parent exists.
4. Use local Git evidence such as `git show <sha>` and `git diff <sha>^ <sha>` when there is no PR. A missing PR association is never by itself a reason to mark the review blocked.
5. If a PR is supplied, verify that its current head still matches the requested SHA before using PR comments or checks as current evidence.
6. If the requested SHA cannot be resolved even after a bounded fetch, report that exact failure and the fetch/ref evidence. Do not replace it with a generic `diff not verified` conclusion.

## Review priorities

Check, as applicable:

- user-visible behavior and backward compatibility;
- playback, audio/video mode, queue, MediaSession, notification, Android Auto, and service synchronization;
- coroutine ownership, cancellation, stale publication, lifecycle cleanup, and resource leaks;
- extractor fallback, cache semantics, runtime configuration, and network bounds;
- Room migrations, data preservation, backup compatibility, and identity;
- Compose state scope, effect keys, stable lists, accessibility, RTL, and localization;
- secrets, remote URLs, redirects, MIME handling, permissions, privacy, and workflow trust boundaries;
- Android/Desktop version separation, signing, packaging, artifacts, and release behavior;
- whether tests actually fail for the original defect and cover the changed behavior.

## Output format

Put findings before the summary and order them by severity. Each finding must include:

- severity and confidence;
- exact file/line or symbol;
- triggering scenario;
- consequence;
- smallest compatible fix;
- missing regression coverage.

Do not report speculative findings without a concrete failure path. Finish with validation observed, validation missing, and remaining manual checks.
