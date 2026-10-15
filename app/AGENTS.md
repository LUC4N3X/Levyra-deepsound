# Levyra Android Module Instructions

These instructions extend the root `AGENTS.md` for every file under `app/`. The root contract remains authoritative; when rules differ, follow the stricter safety or validation requirement.

## Module scope

The Android application is built with Kotlin, Jetpack Compose, AndroidX Media3, Room, WorkManager, OkHttp, Coil, and the existing extractor integration.

Before editing, route the matching native skills under `.agents/skills/` and read the relevant canonical rules under `.agents/claude/rules/`. Load no more than two skill bodies per active phase (root `AGENTS.md`); advance through separate design, implementation, and review phases when extra skills apply. The external upstream skills are installed by the project tooling, not vendored or copied into `.agents/skills/`. The ignored `.claude/` runtime projection is generated from these tracked sources and is not a second source of truth.

For any visual redesign, UI polish, hierarchy, spacing, typography, color, shape, motion, screenshot/reference recreation, or request to make the Android UI more premium, modern, distinctive, cohesive, or less AI-generated, load `levyra-compose` and `levyra-design-taste` in the implementation phase. In the focused UI design/compliance phase also read the upstream `material-3` skill and only the relevant references, without exceeding two simultaneous skill bodies. The design-taste skill supplements Compose engineering rules; it never overrides accessibility, performance, lifecycle, localization, product behavior, or architecture.

For Android jank, frame misses, latency, startup, Perfetto/System Trace, CPU/thread-state, graphics, Binder/IPC, blocking, memory, I/O, power, or other measured runtime-performance work, automatically load `levyra-android-performance` together with the affected domain skill. Validate Perfetto schemas/queries rather than guessing them, and do not turn a debug-only trace or a long slice into a release-performance conclusion without direct evidence.

For R8, Proguard, minification, resource shrinking, keep/consumer rules, mapping files, missing classes, reflection/serialization/JNI shrinker issues, APK-size work, or a failure that appears only in a minified release build, automatically load `levyra-r8-proguard` and `levyra-release-check`; also load `levyra-ci-workflows` when build tooling/configuration changes.

For Android Intent/deep-link/PendingIntent/component-boundary work, automatically load both `levyra-android-intent-security` and `levyra-security-review`, plus the affected Android domain skill. This includes exported activities/services/receivers/providers, incoming or nested Intents, URI grants, mutable PendingIntents, FileProvider/ContentProvider exposure, signature permissions, `onNewIntent`, and caller identity/permission checks.

## Material 3 Expressive design contract

- New or materially changed Android UI must follow Material 3 Expressive, adapted to Levyra's existing identity and the Material3 APIs already available in the project.
- Before drawing a new surface, inspect the surrounding screen, Levyra theme tokens, reusable components, typography, shapes, artwork, navigation, and current interaction pattern.
- Favor a music-first, premium, calm interface with meaningful color hierarchy, coherent spacing, expressive but restrained shapes and motion, and obvious primary actions. Rich visual treatment must serve discovery or playback, not decoration.
- Avoid harsh neon, arbitrary saturated green/red status blocks, muddy gradients, excessive pills and badges, generic card grids, cheap glass effects, unnecessary shadows, and random colors. Colorful is allowed when purposeful and consistent with the theme.
- Prefer native Material3/Expressive components and semantic theme roles where supported; do not add experimental dependencies or new APIs just to imitate a reference. Reuse existing UI primitives before inventing another component family.
- Keep dark/light and dynamic-color behavior coherent; ensure readable contrast, meaningful semantics, 48dp touch targets, focus order, and support for long translations, RTL, large text, insets and screen-size changes.
- Protect interaction and navigation behavior, offline/error/loading/empty states, gestures, account-optional paths, and user preferences. Never redesign unrelated surfaces to polish one change.
- If the result works but looks inconsistent, inspect an actual preview/screenshot or device rendering and polish the assigned surface before calling it complete. Never claim a screenshot or emulator was tested unless it was.
- During UI design/compliance, consult the installed [hamen/material-3-skill](https://github.com/hamen/material-3-skill) skill `material-3` and applicable references. Cross-check API availability against Levyra's current Compose dependencies; the upstream skill is guidance, not authority to migrate the stack.

## Kotlin footgun review

- For Kotlin/Compose/Media3/Room/coroutines/network or Desktop-KMP code, consult the applicable area index of [maxrave-dev/kotlin-footguns](https://github.com/maxrave-dev/kotlin-footguns) during the relevant review phase, then inspect only the traps matching changed code.
- Pick by ownership: `compose-screens-footguns` / `compose-visuals-footguns` for UI, `media-playback-footguns` for audio, `data-layer-footguns` for Room/cache, `state-and-background-footguns` for Flow/services, `remote-api-footguns` for auth/network, `kmp-architecture-footguns` for Kotlin architecture, `desktop-and-build-footguns` for Desktop, and `engineering-method-footguns` for tooling/verification.
- Use `footgun-scan` on a meaningful Kotlin diff when installed and practical; investigate actual hits, not blindly rewrite code for heuristics. Keep Levyra architecture and confirmed working behavior authoritative.
- Missing upstream installation is `UNAVAILABLE`, not evidence that the footgun/design review passed. Follow `docs/ai/UPSTREAM_ANDROID_SKILLS.md` to install it; continue other work safely if unavailable, without repeated downloads or excessive context.

## Architecture boundaries

- Preserve unidirectional data flow from user intent through ViewModel/controller and repository/player operations into immutable UI state.
- Keep playback ownership in the existing player/service/MediaSession architecture; composables must not become a second playback controller.
- Keep network, database, parsing, decoding, file, and metadata work off the main thread.
- Reuse existing clients, stores, caches, scopes, dispatchers, queue state, and lifecycle owners.
- Preserve explicit audio/song and native-video modes.
- Preserve synchronization among the audible player, MediaSession, notification, Android Auto, queue, and background service.
- Keep optional enrichment behind direct playback in priority.

## Memory regression guard

The stability reached after issue #427 is a preserved product invariant. Any Android change that touches playback, recommendations, resolver/extractor loops, DSP/audio processors, Media3 track selection, prefetch, artwork/Canvas decoding, caches, buffering, coroutines/listeners, or other repeated hot paths must be reviewed for memory behavior before completion.

- Treat continuously rising memory during steady playback as a regression until evidence explains and bounds it. A one-time warm-up peak or device-specific high RSS is not by itself proof of a leak; the trend matters.
- Never compile `Regex`/`Pattern`, call `.toRegex()`, construct parsers, or create equivalent reusable matchers inside per-item, recommendation-candidate, polling, playback-tick, frame, sample, or audio-buffer loops. Hoist immutable/reusable instances or prove why reuse is unsafe. Issue #427 demonstrated that repeated regex compilation can create extreme native allocation churn even when the Java heap looks acceptable.
- Avoid per-sample/per-buffer/per-frame allocations in DSP and playback callbacks. Reuse existing `ByteBuffer`, arrays, scratch buffers, processors, decoders, and immutable helpers when ownership permits; do not add copying merely for convenience.
- Keep caches, prefetch sets, recommendation state, queues, histories, maps keyed by media IDs/URLs, decoded artwork, and retry bookkeeping explicitly bounded with eviction or lifecycle cleanup. No collection may grow with playback duration without a documented finite bound.
- Cancel obsolete coroutines/jobs, unregister listeners/callbacks, close response bodies/files/cursors, and release Media3 players/controllers/decoders/surfaces only at the owning lifecycle boundary. Identity/generation checks must prevent stale work from retaining large graphs after track changes.
- Song/audio-only mode must not keep a video decoder, video track, surface, or video-sized buffer pool alive unless that mode explicitly requires it. Preserve native-video mode separately rather than weakening it globally.
- Do not introduce `System.gc()`, periodic player/service recycling, arbitrary pause/restart loops, cache purges, or process restarts as the primary fix for unexplained growth. Find and remove the allocation/retention source first; resilience guards may remain a fallback only when independently justified.
- Artwork and Canvas work must avoid duplicate full-resolution bitmaps, unbounded animated-frame retention, and unnecessary intermediate render targets. Decode/retain only what the visible lifecycle needs.
- A memory-sensitive change is not validated by Java/Kotlin heap inspection alone. When the task can affect native/media/graphics memory, inspect at least native heap plus process PSS/RSS (for example via `dumpsys meminfo`, Perfetto/heapprofd, or equivalent device profiling) and distinguish allocation churn from retention.
- For materially memory-sensitive playback changes, prefer a sustained real-playback validation after warm-up with normal track transitions; 20–30 minutes on a physical device is the target when practical. Include screen/background transitions when the changed path remains active there. If this cannot be run, report the memory regression check as `BLOCKED`/unverified rather than claiming stability.
- Acceptance is a stable plateau or bounded oscillation appropriate to the workload, with no persistent monotonic climb across track transitions. Do not hard-code one universal MB threshold across devices/OEMs.

## Compose and resources

- Keep orchestration outside composables and observe the smallest stable state required by each screen.
- Use stable keys for lazy content and correctly keyed effects with deterministic cleanup.
- Add every user-facing string to the localization system and verify long text, RTL, font scaling, accessibility, and restoration.
- Preserve cached or real content during refresh when safe; avoid blank loading regressions.
- Treat existing UI as a redesign by default: preserve behavior, navigation, gestures and state ownership, then improve hierarchy, rhythm and visual consistency before adding decorative effects.
- Reuse Levyra theme tokens and existing components before introducing one-off colors, radii, spacing values, visual primitives or dependencies.
- Trace edge-to-edge/system-bar/IME inset ownership before adding padding. Apply each inset once, keep critical controls tappable, and use list `contentPadding` when parent padding would clip scroll-behind behavior.
- Horizontal carousels (`LazyRow`, `Row` + `horizontalScroll`) placed inside a parent with horizontal padding or `contentPadding` must scroll edge to edge: bleed the scroll container out by the parent gutter (reuse an existing bleed `layout` modifier such as `recentSearchesBleed`) and restore alignment with matching horizontal `contentPadding` or inner padding. Never leave a horizontally scrolling row clipped at the parent gutter.
- Check relevant larger widths/form factors for layout changes, but do not force Navigation 3, multi-pane scenes, Compose Styles, Grid/FlexBox/MediaQuery, or another experimental API as collateral modernization.
- Reuse existing screenshot/preview infrastructure when visual regression evidence is needed; inspect golden diffs before accepting new baselines.

## Android component security

- Prefer explicit intents for internal component launches. Treat incoming implicit/deep-link data and nested Intents as untrusted.
- Do not launch or forward an attacker-controlled nested Intent without allowlisting or sanitizing the allowed target, action, data, type, categories, extras, and flags; reject unsafe URI permission grants.
- Default PendingIntents to immutable. If mutability is genuinely required, bind the base Intent to an explicit trusted component/package and keep the mutable surface minimal.
- Preserve PendingIntent request-code/update semantics so a hardening change does not accidentally alias unrelated notification/media actions.
- Keep internal activities/services/receivers/providers non-exported unless external access is part of the feature contract; protect exported privileged components with the narrowest suitable permission/caller validation.
- Apply the same validation to `onNewIntent` and other warm-reuse paths as initial intent handling.
- For providers and URI sharing, grant only the access actually required and preserve existing FileProvider/ContentProvider authority boundaries.
- A build passing does not prove an exported-component or intent boundary safe; require a concrete attacker-controlled path, trust-boundary review, negative test where practical, and revalidation after remediation.

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

Manual playback, notification, Android Auto, PiP, emulator, device, background restriction, OEM behavior, visual polish, TalkBack, Intent/deep-link/component security, memory stability, and measured UI-performance claims remain unverified unless directly tested and reported with evidence.
