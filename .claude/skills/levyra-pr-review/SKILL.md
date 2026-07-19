---
name: Levyra PR Review
description: Reviews the current Levyra diff before merge and produces prioritized, actionable findings plus a validation summary.
context: fork
agent: levyra-reviewer
---

# PR review procedure

1. Inspect `git status --short`, the complete diff, and surrounding implementations.
2. Read the relevant `.claude/rules/` files.
3. Check playback/audio-video behavior, concurrency, lifecycle, security, Room, Compose, localization, CI, release, and licenses.
4. Verify tests actually cover the changed behavior and distinguish unit, CI, and device evidence.
5. Ignore outdated review comments already fixed by current code.
6. Output findings ordered by severity. Each finding must include file/line, scenario, consequence, and smallest fix.
7. Finish with checks run, checks not run, and remaining manual validation.
