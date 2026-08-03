# Levyra Engineering Instructions

## Purpose and scope

This file is the repository-level operating contract for coding agents working on Levyra. It applies to the entire repository unless a more specific `AGENTS.md` is added closer to the files being changed.

Use these instructions for analysis, implementation, review, testing, documentation, and release preparation. Repository code, tests, build files, and current documentation always take precedence over assumptions or remembered behavior.

## Sources of truth

Read the following in this order before making a non-trivial change:

1. This `AGENTS.md`.
2. `docs/ARCHITECTURE.md`.
3. The relevant implementation and nearby tests.
4. The matching rule files under `.claude/rules/`.
5. The matching workflow under `.claude/skills/` or `.agents/skills/levyra-engineering/SKILL.md`.
6. Existing CI and release workflows when the task touches build, signing, packaging, extraction configuration, or publication.

Do not treat README copy, old discussions, previous agent output, or stale review comments as more authoritative than the current repository.

## Product mission

Levyra is a native Android music application built with Kotlin, Jetpack Compose, AndroidX Media3, Room, WorkManager, OkHttp, and Coil. Protect playback reliability, responsiveness, privacy, and the user's existing choices before adding visual polish or speculative optimization.

## Non-negotiable product behavior

- The user controls whether playback uses song/audio mode or native video mode. Never remove, hide, merge, or silently override that choice.
- Motion artwork is decorative and belongs to song/audio mode only. It must never replace native video mode, produce audible output, or delay playback.
- Static artwork is the immediate and permanent fallback. Artwork failures must never leave a blank player or interrupt audio.
- The audible player, MediaSession, notification, Android Auto, queue, and background service must remain synchronized.
- Direct playback requests are the critical path. Home refresh, artwork, lyrics, diagnostics, prefetch, and enrichment must yield to playback.
- Existing downloads, favorites, playlists, queue state, lyrics, history, settings, localization, and backup behavior must be preserved unless the task explicitly changes them.
- Do not add Spotify endpoints, account login, cookies, GraphQL, tokens, scraping, telemetry, or tracking unless the repository owner explicitly requests that integration.

## Architecture and concurrency rules

- Preserve unidirectional data flow: user intent -> ViewModel or controller -> repository or player operation -> immutable state -> Compose.
- Keep network, database, decoding, file, metadata, and parsing work off the main thread.
- Reuse the existing OkHttp, Coil, Media3, Room, queue, cache, coroutine, and lifecycle infrastructure instead of creating parallel stacks.
- Make ownership explicit for coroutines, players, callbacks, receivers, surfaces, decoders, cache entries, and in-flight work.
- Shared asynchronous work must not depend on the lifecycle of its first caller. Cancellation by one waiter must not cancel work still required by another waiter.
- Protect asynchronous publication with identity and generation checks whenever an older job can finish after a newer one.
- Treat cancellation separately from failure. Re-throw `CancellationException`; never cache or report it as a normal miss.
- Distinguish conclusive no-match results from transient transport, timeout, server, parsing, or verification failures.
- Do not negative-cache inconclusive failures.
- Bound retries, timeouts, concurrency, response sizes, storage growth, and prefetch work.
- Keep durable identity independent from mutable display text.

## Work method

1. Restate the requested scope internally and identify behavior that must remain unchanged.
2. Inspect the complete current path through the relevant UI, state, repository, player, database, service, workflow, and tests.
3. Identify the root cause before editing. Do not hide a defect with retries, delays, broad exception handling, or duplicated state.
4. Make the smallest coherent change that fixes the cause and fits the existing architecture.
5. Avoid unrelated cleanup, formatting churn, dependency upgrades, renames, and refactors.
6. Add or update regression tests for bugs, matching logic, security boundaries, migrations, and concurrency behavior when applicable.
7. Run the narrowest useful checks first, then the applicable project checks.
8. Inspect the final diff for unrelated changes, generated files, secrets, credentials, binary artifacts, conflict markers, and accidental version changes.
9. Report exactly what changed, what ran, what passed, what failed, and what remains unverified.

When the owner says "only this", modify only the named behavior or files unless an additional change is strictly required for correctness. State that requirement before expanding scope.

## Build and validation

Use the repository Gradle wrapper, never a system Gradle installation.

```bash
./gradlew --no-daemon :app:testDebugUnitTest
./gradlew --no-daemon :app:lintRelease
./gradlew --no-daemon --no-configuration-cache assembleRelease
git diff --check
```

Guidance:

- Start with focused tests for the affected module or class.
- Android release tasks require the inputs documented in `app/build.gradle.kts` and mirrored by `.github/workflows/pr-check.yml`.
- Do not add real credentials, a keystore, `local.properties`, or private configuration to make a local build pass.
- A missing Android SDK, unavailable signing input, absent device, or blocked network is a blocked check, not a pass.
- Never claim a build, emulator, device, Android Auto, notification, PiP, or playback check succeeded without evidence.
- Documentation-only changes do not require an Android build unless they alter executable examples, workflow behavior, or build instructions. Validate paths, commands, Markdown, and the final diff instead.

## Task routing

Before reading widely or editing, load the matching procedure. Several procedures may apply to one task.

| Task touches | Read and follow |
| --- | --- |
| Playback, queue, Media3, MediaSession, notification, Android Auto, prefetch, audio/video mode | `.claude/skills/levyra-player/SKILL.md` |
| InnerTube, extractor, stream resolution, player-config sync, tokens, retries, network fallback | `.claude/skills/levyra-extractor/SKILL.md` |
| Room entities, DAOs, migrations, schema, caches, stores, backup | `.claude/skills/levyra-database/SKILL.md` |
| Compose screens, state projections, animation, lifecycle, accessibility, localization | `.claude/skills/levyra-compose/SKILL.md` |
| Decorative motion artwork | `.claude/skills/levyra-motion-artwork/SKILL.md` |
| Secrets, remote URLs, redirects, SSRF, MIME handling, permissions, privacy, workflow exposure | `.claude/skills/levyra-security-review/SKILL.md` |
| Reviewing a branch, commit, patch, or pull request | `.claude/skills/levyra-pr-review/SKILL.md` |
| Pre-merge or pre-release validation, version values, signing, APK output, release workflows | `.claude/skills/levyra-release-check/SKILL.md` |

The `.claude/skills/` procedures are shared engineering playbooks despite their location. Codex and ChatGPT should read them as repository documentation; they do not require Claude-specific tooling to be useful.

## Security and repository safety

- Never commit or expose secrets, passwords, tokens, cookies, private URLs, keystores, signing material, `.env` files, or `local.properties`.
- Never commit APKs, ZIPs, generated build output, IDE state, or temporary diagnostics unless explicitly required and already accepted by repository policy.
- Validate every provider-controlled media URL across scheme, host, port, user info, DNS/IP destination, redirect hops, MIME type, timeout, and response-size bounds.
- Preserve least privilege in Android permissions and GitHub workflow permissions.
- Do not weaken transport, redirect, MIME, signature, or host validation to make one provider response pass.
- Update credits and licenses when adding external code, assets, models, components, or dependencies.

## Versioning, releases, and external actions

- Do not change `levyraVersionName` or `levyraVersionCode` unless the task is explicitly a release or version task.
- Do not tag, publish, release, merge, enable auto-merge, modify repository settings, or update store metadata without explicit approval.
- Do not commit, push, or open a pull request unless the user explicitly asks for that external action.
- When publication is requested, use a dedicated branch and a draft pull request by default. Never push directly to `main` unless the owner explicitly requests it after reviewing the exact scope.
- Keep pull request descriptions and checklists truthful. Leave manual and device-only checks unmarked until they are actually completed.

## Review standard

For reviews, prioritize findings over summaries. Each finding must include:

- severity and confidence;
- exact file and line or symbol;
- triggering scenario;
- user or system consequence;
- smallest compatible fix;
- missing regression coverage.

Do not report speculative issues without a concrete failure path. Ignore comments that no longer apply to the current code.

## Delivery contract

Every completed implementation should include:

- concise summary of the root cause and solution;
- files changed;
- behavior preserved;
- tests and checks run with results;
- skipped or blocked checks;
- remaining risks or manual validation;
- a professional commit message when requested.

Never claim that a file, test, build, push, pull request, merge, or release exists unless it was actually created or verified.