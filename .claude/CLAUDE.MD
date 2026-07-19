# Levyra Project Instructions
 
## Mission
 
Levyra is a native Android music player built with Kotlin, Jetpack Compose, AndroidX Media3, Room, WorkManager, OkHttp, and Coil. Protect playback reliability, responsiveness, privacy, and the user's existing choices before adding visual polish or speculative optimization.
 
## Non-negotiable product behavior
 
- The user controls whether to use song/audio mode or the native video mode. Never remove, hide, or silently override that choice.
- Motion artwork is decorative artwork for song/audio mode only. It must never replace the native music-video mode, add audible output, or delay playback.
- Static artwork is always the immediate fallback. Network artwork failures must never leave a blank player or stop audio.
- The audible player, MediaSession, notification, Android Auto, queue, and background service must remain synchronized.
- A direct user playback request is the critical path. Home refresh, artwork, lyrics, diagnostics, prefetch, and enrichment are secondary work and must yield to it.
- Do not add Spotify endpoints, login, cookies, GraphQL, tokens, or scraping unless the repository owner explicitly requests that integration.
 
## Architecture rules
 
- Preserve unidirectional data flow: user intent -> ViewModel/controller -> repository/player operation -> immutable state -> Compose.
- Keep network, database, decoding, file, and metadata work off the main thread.
- Reuse existing shared OkHttp, Coil, Media3, Room, queue, and cache infrastructure instead of creating parallel stacks.
- Keep shared asynchronous work independent from the first caller's lifecycle. A cancelled prefetch must not cancel the current track's waiter.
- Protect publication of asynchronous results with identity and generation checks when an older job can finish after a newer one.
- Treat cancellation separately from failure. Re-throw `CancellationException`; do not convert it into a cached miss.
- Do not negative-cache timeouts, transport failures, server failures, parsing failures, or inconclusive URL verification.
 
## Work method
 
1. Read the relevant implementation, nearby tests, `docs/ARCHITECTURE.md`, and the matching files under `.claude/rules/`.
2. Write down the behavior that must remain unchanged before editing.
3. Make the smallest coherent change that fixes the root cause.
4. Add or update regression tests for bugs and matching/security rules.
5. Run the narrowest useful tests first, then the applicable project checks.
6. Run `git diff --check` and inspect the final diff for unrelated edits, generated files, and secrets.
7. Report exactly what ran, what passed, and what could not run. Never claim a build or device test succeeded without evidence.
 
## Build and test
 
Use the repository wrapper, never a system Gradle installation.
 
```bash
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:lintRelease
./gradlew --no-daemon --no-configuration-cache assembleRelease
git diff --check
```
 
Release tasks require the inputs documented in `app/build.gradle.kts` and mirrored by `.github/workflows/pr-check.yml`. Do not commit real credentials or a keystore to make a local build pass. Use repository secrets in GitHub Actions and temporary CI-only test signing material where the existing workflow already does so.
 
## Release and repository safety
 
- Do not change `levyraVersionName` or `levyraVersionCode` unless the task is explicitly a release/version task.
- Do not commit APKs, ZIPs, keystores, `local.properties`, credentials, tokens, cookies, or private configuration.
- Do not duplicate existing release, artifact, extractor-sync, or validation workflows.
- Update credits and licenses when adding external code or components.
- Keep PR descriptions and checkboxes truthful. Leave device-only checks unchecked until they are actually performed.
 
## Specialized guidance
 
Claude Code automatically loads topic and path-specific rules from `.claude/rules/`. Reusable procedures are available under `.claude/skills/`, including player, extractor, database, Compose, release, security, motion-artwork, and PR-review workflows.
