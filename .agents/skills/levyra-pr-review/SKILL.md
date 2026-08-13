---
name: levyra-pr-review
description: Review a Levyra branch, commit, patch, or pull request for correctness, regressions, concurrency, lifecycle, security, data safety, UI behavior, CI, release risk, missing tests, and merge-readiness evidence.
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

## Local readiness loop

When the task is to prepare implementation for publication or determine merge
readiness, run focused checks and Levyra's required local gate before review.
When Codex is available and the active agent is not already operating in review
mode, run:

```bash
codex review --uncommitted
```

Verify every finding against the current diff. Fix only actionable defects,
rerun affected validation, and repeat the built-in review until no actionable
findings remain. Do not invoke a nested Codex review when the current task is
itself a code-review session. If the built-in review is required for readiness
but unavailable or fails, report that as a readiness blocker instead of
substituting an unrelated third-party review command.

For published PR readiness, require checks and review evidence against the
latest commit, inspect unresolved thread state, and require a fresh independent
review. Builder self-review and green CI do not replace independent review.
Required manual testing remains incomplete until it is actually performed on the
target environment.

Treat screenshots and direct user/runtime observations as acceptance evidence.
A visible regression must be reconciled even when automation is green.

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

Do not report speculative findings without a concrete failure path. Finish with validation observed, validation missing, manual-test state, latest-head evidence when relevant, and remaining blockers.
