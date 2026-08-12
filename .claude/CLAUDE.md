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
 
1. Read the relevant implementation, nearby tests, `docs/ARCHITECTURE.md`, `docs/ai/AI_ENGINEERING_GUARDRAILS.md`, and the matching files under `.claude/rules/`.
2. Write down the behavior that must remain unchanged before editing.
3. State material assumptions and unresolved tradeoffs; inspect repository evidence first when it can resolve them.
4. Prefer the simplest existing-owner/reuse path that meets the acceptance criteria; do not add speculative flexibility or one-off abstractions.
5. Define the verification target for every non-trivial step.
6. Make the smallest coherent change that fixes the root cause.
7. Add or update regression tests for bugs and matching/security rules.
8. Run the narrowest useful tests first, then the applicable project checks.
9. Run `git diff --check` and inspect the final diff for unrelated edits, generated files, and secrets.
10. Report exactly what ran, what passed, and what could not run. Never claim a build or device test succeeded without evidence.
 
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
| Android jank, frame misses, latency, startup, Perfetto/System Trace, CPU/thread state, graphics, Binder/IPC, blocking, memory, I/O, power, or measured runtime-performance investigation | `levyra-android-performance` plus the affected domain skill |
| R8, Proguard, minification, resource shrinking, keep/consumer rules, release-only shrinker crashes, mapping/missing classes, reflection/serialization/JNI shrinker issues, or APK-size work | `levyra-r8-proguard` plus `levyra-release-check`; add `levyra-ci-workflows` for build-tooling changes |
| Android Intent, deep link, PendingIntent, exported activity/service/receiver/provider, nested Intent, `onNewIntent`, URI grant, FileProvider/ContentProvider, caller/signature verification, or component-boundary audit | `levyra-android-intent-security` plus `levyra-security-review` and the affected Android domain skill |
| Visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshot/reference work, premium/modern/cohesive/anti-AI-slop requests | `levyra-design-taste` plus the matching Android/Desktop UI skill |
| Decorative motion artwork | `levyra-motion-artwork` |
| GitHub Actions, CI, F-Droid, Gradle/AGP/Kotlin/KSP, build performance/cache, artifacts, or workflow automation | `levyra-ci-workflows` |
| Builds, tests, lint, logs, broad searches, dependencies, Git/GitHub, CI, CodeRabbit, setup, or other noisy command output | `levyra-context-efficiency` |
| Secrets, remote URLs, redirects, SSRF, MIME confusion, permissions, privacy, workflow exposure, or other security-sensitive work | `levyra-security-review` |
| Reviewing a branch, commit, diff, or pull request | `levyra-pr-review` |
| Emulator/device runtime verification, pre-merge/pre-release validation, `levyraVersionName`/`levyraVersionCode`, signing, APK/package output | `levyra-release-check` |

When several rows match, invoke each of them. A slow Gradle build normally uses both `levyra-ci-workflows` and `levyra-context-efficiency`; a playback bug uses `levyra-real-engineering` plus `levyra-player`; a Compose jank issue uses `levyra-real-engineering`, `levyra-compose`, and `levyra-android-performance`; R8/Proguard changes use `levyra-r8-proguard` and require release/minified validation; Android Intent/PendingIntent/exported-component work uses `levyra-android-intent-security` plus `levyra-security-review`; a visual Compose redesign uses `levyra-design-taste` plus `levyra-compose`; a Desktop visual redesign uses `levyra-design-taste` plus `levyra-desktop`; a PR review uses `levyra-pr-review` plus all affected domain/security skills. If a skill turns out not to apply once read, say so in one line and continue rather than silently skipping it.

`levyra-design-taste` is a thin bridge to the canonical skill under `.agents/skills/`. It is a product-design quality layer for Levyra, not a web stack or a replacement design system. Use it to infer the surface's role, preserve current behavior, reject generic AI defaults, reuse Levyra tokens/components, justify motion, and run the visual pre-flight review. Accessibility, performance, lifecycle, localization, platform UI guidance and current architecture always take precedence over decorative novelty.

`levyra-android-performance` is a thin bridge to the canonical Android performance skill under `.agents/skills/`. Use it for evidence-first Perfetto/System Trace and runtime profiling. Keep verified timestamps, `upid`/`utid`, thread states, frame evidence, blocking dependencies, metrics, and measurements separate from hypotheses; validate Perfetto schema/module/query assumptions before relying on SQL results, and never infer a root cause from a long slice or debug-only jank alone.

`levyra-r8-proguard` is a thin bridge to the canonical shrinker skill under `.agents/skills/`. Levyra release builds are minified and resource-shrunk, so keep-rule changes must be treated as release correctness work. Prefer the official R8 configuration analyzer when the installed AGP exposes it, inspect actual consumer rules and runtime mechanisms, avoid blanket keep rules, and validate the affected minified release path instead of disabling shrinking.

`levyra-android-intent-security` is a thin bridge to the canonical Android component-boundary skill under `.agents/skills/`. Pair it with `levyra-security-review`. Treat incoming/deep-link/nested Intents, PendingIntents, URI grants, provider access, and caller identity as untrusted until the actual boundary is verified; apply the same checks to cold-start and `onNewIntent` paths, default PendingIntents to immutable unless mutability is required, constrain mutable tokens to explicit trusted targets, and do not call an exposure pattern a vulnerability without a concrete attacker-controlled path.

`levyra-real-engineering` is a thin bridge to the canonical adapter under `.agents/skills/`. Use only the stages needed: clarify genuine ambiguity, resolve a large decision map when necessary, write a spec only after intent is settled, split oversized work into reviewable tickets, implement one ticket at a time, and finish with independent review. Skip this ceremony for tiny, already-unambiguous changes. For bugs, regressions, test/build failures, races, crashes, and unexpected behavior, use its hypothesis-driven debugging lane before stacking speculative fixes. When the project-enabled `mattpocock-skills` plugin is available, invoke the exact upstream stage named by the bridge instead of paraphrasing it from memory. Levyra's architecture, focused domain skills, tests, quality gates, and publication rules always win on conflicts.

A `UserPromptSubmit` hook restates the matching rows for each request, so this table is enforced rather than merely documented. The table remains authoritative if that hook is unavailable.
