# Levyra Android Module Instructions

These instructions extend the root `AGENTS.md` for every file under `app/`. The root contract remains authoritative; when rules differ, follow the stricter safety or validation requirement.

## Module scope

The Android application is built with Kotlin, Jetpack Compose, AndroidX Media3, Room, WorkManager, OkHttp, Coil, and the existing extractor integration.

Before editing, load every matching native skill under `.agents/skills/` and the referenced domain procedure/rules under `.claude/`.

For any visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshot/reference recreation, or request to make the Android UI more premium, modern, distinctive, cohesive, or less AI-generated, automatically load both `levyra-compose` and `levyra-design-taste` before editing. The design-taste skill supplements Compose engineering rules; it never overrides accessibility, performance, lifecycle, localization, product behavior, or architecture.

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

## Persistence and compatibility

- Preserve user downloads, favorites, playlists, queue, lyrics, history, settings, onboarding, and backups unless the task explicitly changes them.
- Schema changes require an explicit non-destructive Room migration and migration tests.
- Keep canonical identity independent from mutable display text.

## Build and release safety

- Use the repository Gradle wrapper.
- Do not modify Android version values unless the task explicitly requests an Android release/version change.
- Do not add credentials, keystores, `local.properties`, APKs, ZIPs, or generated output.
- Release builds require the approved environment/CI inputs already documented by the project; missing inputs are blocked checks, not reasons to weaken validation.

## Validation

Start with focused unit tests for the affected class or feature. Then run applicable checks from the root `AGENTS.md`.

Manual playback, notification, Android Auto, PiP, emulator, device, background restriction, OEM behavior, visual polish, TalkBack, and measured UI-performance claims remain unverified unless directly tested and reported with evidence.