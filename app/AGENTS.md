# Levyra Android Module Instructions

These instructions extend the root `AGENTS.md` for every file under `app/`. The root contract remains authoritative; when rules differ, follow the stricter safety or validation requirement.

## Module scope

The Android application is built with Kotlin, Jetpack Compose, AndroidX Media3, Room, WorkManager, OkHttp, Coil, and the existing extractor integration.

Before editing, load every matching native skill under `.agents/skills/` and the referenced domain procedure/rules under `.claude/`.

For any visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshot/reference recreation, or request to make the Android UI more premium, modern, distinctive, cohesive, or less AI-generated, automatically load both `levyra-compose` and `levyra-design-taste` before editing. The design-taste skill supplements Compose engineering rules; it never overrides accessibility, performance, lifecycle, localization, product behavior, or architecture.

For Android jank, frame misses, latency, startup, Perfetto/System Trace, CPU/thread-state, blocking, memory, I/O, power, or other measured runtime-performance work, automatically load `levyra-android-performance` together with the affected domain skill. Do not turn a debug-only trace or a long slice into a release-performance conclusion without direct evidence.

For R8, Proguard, minification, resource shrinking, keep/consumer rules, mapping files, missing classes, reflection/serialization/JNI shrinker issues, APK-size work, or a failure that appears only in a minified release build, automatically load `levyra-r8-proguard` and `levyra-release-check`; also load `levyra-ci-workflows` when build tooling/configuration changes.

For Android Intent/deep-link/PendingIntent/component boundary work, automatically load `levyra-security-review`. This includes exported activities/services/receivers/providers, incoming/nested Intents, URI grants, mutable PendingIntents, FileProvider/ContentProvider exposure, signature permissions, `onNewIntent`, and caller identity/permission checks.

## Architecture boundaries

- Preserve unidirectional data flow from user intent through ViewModel/controller and repository/player operations into immutable UI state.
- Keep playback ownership in the existing player/service/MediaSession architecture; composables must not become a second playback controller.
- Keep network, database, parsing, decoding, file, and metadata work off the main thread.
- Reuse existing clients, stores, caches, scopes, dispatchers, queue state, and lifecycle owners.
- Preserve explicit audio/song and native-video modes.
- Preserve synchronization among the audible player, MediaSession, notification, Android Auto, queue, and background service.
- Keep optional enrichment behind direct playback in priority.

## Compose and resources

- Keep orchestration outside composables and observe the smallest stable state required by each screen.
- Use stable keys for lazy content and correctly keyed effects with deterministic cleanup.
- Add every user-facing string to the localization system and verify long text, RTL, font scaling, accessibility, and restoration.
- Preserve cached or real content during refresh when safe; avoid blank loading regressions.
- Treat existing UI as a redesign by default: preserve behavior, navigation, gestures and state ownership, then improve hierarchy, rhythm and visual consistency before adding decorative effects.
- Reuse Levyra theme tokens and existing components before introducing one-off colors, radii, spacing values, visual primitives or dependencies.
- Trace edge-to-edge/system-bar/IME inset ownership before adding padding. Apply each inset once, keep critical controls tappable, and use list `contentPadding` when parent padding would clip scroll-behind behavior.
- Check relevant larger widths/form factors for layout changes, but do not force Navigation 3, multi-pane scenes, Compose Styles, Grid/FlexBox/MediaQuery, or another experimental API as collateral modernization.
- Reuse existing screenshot/preview infrastructure when visual regression evidence is needed; inspect golden diffs before accepting new baselines.

## Android component security

- Prefer explicit intents for internal component launches. Treat incoming implicit/deep-link data and nested intents as untrusted.
- Do not launch or forward an attacker-controlled nested Intent without allowlisting/sanitizing the allowed target/action/data/extras and rejecting unsafe URI permission grants.
- Default PendingIntents to immutable. If mutability is genuinely required, bind the base Intent to an explicit trusted component/package and keep the mutable surface minimal.
- Keep internal activities/services/receivers/providers non-exported unless external access is part of the feature contract; protect exported privileged components with the narrowest suitable permission/caller validation.
- Apply the same validation to `onNewIntent`/warm-reuse paths as initial intent handling.
- For providers and URI sharing, grant only the access actually required and preserve existing FileProvider/ContentProvider authority boundaries.
- A build passing does not prove an exported-component or intent boundary safe; require a concrete trust-boundary review and regression verification for security changes.

## Persistence and compatibility

- Preserve user downloads, favorites, playlists, queue, lyrics, history, settings, onboarding, and backups unless the task explicitly changes them.
- Schema changes require an explicit non-destructive Room migration and migration tests.
- Keep canonical identity independent from mutable display text.

## Build and release safety

- Use the repository Gradle wrapper.
- Do not modify Android version values unless the task explicitly requests an Android release/version change.
- Do not add credentials, keystores, `local.properties`, APKs, ZIPs, or generated output.
- Release builds require the approved environment/CI inputs already documented by the project; missing inputs are blocked checks, not reasons to weaken validation.
- Release minification/resource shrinking are part of the production contract. Do not disable them to hide a shrinker regression; diagnose the actual keep/resource/runtime requirement and verify the minified path.

## Validation

Start with focused unit tests for the affected class or feature. Then run applicable checks from the root `AGENTS.md`.

Manual playback, notification, Android Auto, PiP, emulator, device, background restriction, OEM behavior, visual polish, TalkBack, intent/deep-link security, and measured UI-performance claims remain unverified unless directly tested and reported with evidence.