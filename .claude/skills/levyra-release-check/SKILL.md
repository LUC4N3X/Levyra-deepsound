---
name: Levyra Release Check
description: Runs the Levyra pre-merge or pre-release validation checklist and reports evidence without publishing or releasing anything.
disable-model-invocation: true
allowed-tools: Read, Grep, Glob, Bash
---

# Levyra validation

Do not publish, tag, release, merge, or change version values.

1. Run `git status --short` and `git diff --check`.
2. Inspect changed files for secrets, keystores, APKs, ZIPs, conflict markers, HTML pasted into source, and unrelated changes.
3. Run the narrowest affected unit tests.
4. Run `./gradlew --no-daemon :app:testDebugUnitTest` when available.
5. Run `./gradlew --no-daemon :app:lintRelease` and `./gradlew --no-daemon --no-configuration-cache assembleRelease` only with the required release inputs available through the approved local/CI mechanism.
6. Compare workflow changes against existing release, APK, extractor, config-sync, and duplicate guards.
7. Verify `levyraVersionName` and `levyraVersionCode` changed only for an explicit release task.
8. Report each command, exit result, and any skipped manual checks. Never convert a skipped or blocked check into a pass.
