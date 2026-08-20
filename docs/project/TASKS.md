# Levyra Active Tasks

## Active phase

**Name:** Desktop owned-music playback and Android immersion/replay

**Roadmap tracks:** Track 1 - Playback critical path; Track 2 - Persistence, offline use, and recovery; Track 3 - Responsive, accessible interface; Track 5 - Windows Desktop reliability

**Status:** Implementation complete; latest-head CI and native/device checks pending
**Scope:** Add Desktop audio output-device selection, equalizer presets, an owned local-music library, M3U transfer and dual-player transitions. Add Android pure-black surfaces, centralized haptics, local Listening Replay, high-resolution lyrics share cards and artwork-driven ambient chroma. Preserve the online catalog, downloads, queue, session restore, favorites, playlists, lyrics, history, settings, localization, Android/Desktop versions, signing, packaging and release behavior.

## Acceptance criteria

- Desktop output-device selection persists, applies live, falls back to the system default on device loss and recovers when the selected device returns.
- Equalizer presets preserve headroom; manual band edits resolve to Custom without a second source of truth.
- The local library indexes owned files, preserves missing-file identity, keeps scans/search off the UI path and never routes local tracks through the online resolver.
- M3U import is host-independent and export is atomic and overwrite-safe.
- Dual-player handoff/crossfade advances only a still-matching prepared track and cancels safely on seek, pause, manual changes, queue mutation, sleep/end-of-track and shutdown.
- Android Pure Black is opt-in and remains distinct from the existing AMOLED palette.
- Haptic feedback has one semantic owner and a user preference.
- Listening Replay is computed locally from existing listen events, with rolling 30/365-day summaries, completion, streaks and top music; no new telemetry or Room migration.
- Lyrics sharing preserves the existing verse-selection flow and can create a bounded high-resolution image in private cache, using cached artwork when available and a narrow non-exported FileProvider grant.
- Ambient chroma derives player surfaces from existing artwork/palette data while preserving readable content hierarchy and fallback behavior.
- No account, cookie, private token, telemetry, tracking, new dangerous/storage permission, version change, merge, tag or release.

## Work items

- [x] Desktop audio output-device selection with availability watch.
- [x] Desktop equalizer presets with derived preamp headroom.
- [x] Desktop local music library: tag reader, scanner, watcher, index and UI.
- [x] Desktop M3U/M3U8 import and atomic export.
- [x] Desktop dual-player gapless handoff and equal-power crossfade.
- [x] Android Pure Black and centralized haptics.
- [x] Android Listening Replay using the existing local 365-day event store.
- [x] Android lyrics share cards with private cache, scoped FileProvider and text fallback.
- [x] Android artwork-driven ambient chroma/mesh integrated from the already-reviewed player ambience work, including its contrast/palette tests.
- [x] Review current Canvas architecture: no refactor required because MotionArtworkEngine already owns multi-provider ordering, timeout, cache, ranking, URL verification and in-flight deduplication.
- [x] Review Desktop ReplayGain/spectrum boundary: do not ship a fake spectrum or silently change global libVLC audio behavior when the current engine has no clean runtime API for those features.
- [x] Review hardening for player lifecycle, transition failure paths, local path identity, scan serialization, output-device semantics, parser bounds, large embedded artwork and narrow-width playlist actions.
- [x] Focused regression tests for parser, path, transition, Pure Black, Replay and ambient-color logic.
- [ ] Latest-head GitHub PR workflows all complete successfully.
- [ ] Verify real libVLC output-device switching, hot-plug and audible crossfade/gapless handoff on Windows hardware.
- [ ] Verify a large real local library, watcher behavior and representative supported containers on Windows.
- [ ] Verify Android Pure Black, haptics, Replay layout, lyrics share output and ambient chroma on a physical device.

## Current validation evidence

- Earlier branch heads passed the complete Desktop Windows workflow (compile/tests plus installer), Android lint/full unit/release/F-Droid checks, signed APK artifact, APK size, dependency review and editorial checks. Those results are not treated as proof for a later head.
- Latest-head GitHub PR workflows are the authoritative automated validation target. Record completion only after every workflow for the exact final SHA succeeds.
- Focused coverage includes M3U Windows/UNC paths across hosts, path-boundary identity, output-module/device persistence, Opus multi-page embedded artwork and pre-skip semantics, equalizer presets, scanner behavior, AMOLED versus explicit Pure Black, Replay rolling windows/streaks/top day and player ambience contrast/palette behavior.
- Lyrics share output is bounded to private cache files, uses a read-only URI grant and falls back to text sharing if card generation fails.

## Preserved behavior and explicit boundaries

- Existing online catalog, discovery, downloads, queue, session restore, favorites, playlists, lyrics, history, settings, localization and onboarding remain under their existing owners.
- Direct playback stays the critical path; file scanning, metadata parsing, local search, artwork work and transition preparation remain bounded/cancellable and off UI/native critical paths where applicable.
- The current MotionArtworkEngine already provides Community, Apple and Tidal providers with policy, verification, caching, ranking and concurrency controls; duplicating that provider layer would add churn without capability.
- A real Desktop PCM spectrum would require taking audio samples through the libVLC callback/output path. Runtime ReplayGain is likewise not exposed as a clean per-track toggle by the current player abstraction. Neither is simulated or enabled globally by surprise.
- Android and Desktop versions, packages, artifacts, signing, tags and releases remain independent and unchanged.

## Rollback boundary

Revert the Desktop local-music/output-device/transition/settings additions and the Android Pure Black/haptics/Replay/lyrics-share/ambient additions with their tests and this planning file. Local Desktop data is additive (`localmusic.json`), Android Replay reuses existing listen events, and lyrics share files live only in app cache.

## Update rule

Record CI, review, physical-device, merge and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
