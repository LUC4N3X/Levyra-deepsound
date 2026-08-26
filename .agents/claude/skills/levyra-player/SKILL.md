---
name: Levyra Player Change
description: Safely implements or reviews Levyra playback, queue, Media3, MediaSession, audio/video mode, prefetch, and lifecycle changes. Use for player bugs or features.
---

# Levyra player workflow

1. Read `.claude/rules/player.md`, `docs/ARCHITECTURE.md`, and the relevant player, resolver, service, ViewModel, queue, and test files.
2. Describe the current path from user intent to Media3 and list behavior that must not change.
3. Confirm the existing user-controlled audio/song and native-video modes remain separate.
4. Identify ownership of each coroutine, player, decoder, callback, surface, and cache entry.
5. Fix the root cause without putting secondary work on the playback critical path.
6. Add regression coverage for rapid track changes, cancellation, same-identity replacement, queue mutation, mode switch, and lifecycle when relevant.
7. Run focused unit tests, `git diff --check`, and applicable Gradle checks.
8. Report device-only playback, Android Auto, notification, and PiP checks as unverified unless actually tested.
