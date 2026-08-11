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
 
Rules under `.claude/rules/` load automatically from their `paths:` frontmatter. Skills do not load themselves. Invoke every matching skill with the Skill tool **automatically before reading widely, editing, or running a large command**, without waiting for the owner to name the skill or type a slash command.

| The task touches | Invoke |
| --- | --- |
| Non-trivial feature, architecture, bug/regression, test/build failure, unexpected behavior, specification/ticket split, or multi-step engineering | `levyra-real-engineering` |
| Playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video mode | `levyra-player` |
| InnerTube, extractor, stream resolution, player-config sync, tokens, network fallback | `levyra-extractor` |
| Room entities, DAOs, migrations, schema, caches, stores, backup | `levyra-database` |
| Compose screens, state projections, jank/scrolling/recomposition, Layout Inspector/Perfetto, animation, lifecycle, accessibility/TalkBack/semantics, RTL, localization | `levyra-compose` |
| Decorative motion artwork | `levyra-motion-artwork` |
| GitHub Actions, CI, F-Droid, Gradle/AGP/Kotlin/KSP, build performance/cache, artifacts, or workflow automation | `levyra-ci-workflows` |
| Builds, tests, lint, logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, setup, or other noisy command output | `levyra-context-efficiency` |
| Secrets, remote URLs, redirects, SSRF, MIME confusion, permissions, privacy, workflow exposure | `levyra-security-review` |
| Reviewing a branch, commit, diff, or pull request | `levyra-pr-review` |
| Emulator/device runtime verification, pre-merge/pre-release validation, `levyraVersionName`/`levyraVersionCode`, signing, APK/package output | `levyra-release-check` |

When several rows match, invoke each of them. A slow Gradle build normally uses both `levyra-ci-workflows` and `levyra-context-efficiency`; a playback bug uses `levyra-real-engineering` plus `levyra-player`; a Compose jank issue uses `levyra-real-engineering` plus `levyra-compose`; a PR review uses `levyra-pr-review` plus all affected domain/security skills. If a skill turns out not to apply once read, say so in one line and continue rather than silently skipping it.

`levyra-real-engineering` is a thin bridge to the canonical adapter under `.agents/skills/`. Use only the stages needed: clarify genuine ambiguity, resolve a large decision map when necessary, write a spec only after intent is settled, split oversized work into reviewable tickets, implement one ticket at a time, and finish with independent review. Skip this ceremony for tiny, already-unambiguous changes. For bugs, regressions, test/build failures, races, crashes, and unexpected behavior, use its hypothesis-driven debugging lane before stacking speculative fixes. When the project-enabled `mattpocock-skills` plugin is available, invoke the exact upstream stage named by the bridge instead of paraphrasing it from memory. Levyra's architecture, focused domain skills, tests, quality gates, and publication rules always win on conflicts.

A `UserPromptSubmit` hook restates the matching rows for each request, so this table is enforced rather than merely documented. The table remains authoritative if that hook is unavailable.
