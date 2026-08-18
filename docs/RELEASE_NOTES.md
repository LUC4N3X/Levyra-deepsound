# Levyra 2.3.21

## Highlights

Levyra 2.3.21 is a broad Android update that brings together the work merged after 2.3.20: a redesigned Now Playing experience, substantially improved Canvas presentation, richer Search, album and playlist batch downloads, a cleaner Library, serverless playlist sharing, optional DeArrow video metadata, major offline-download fixes, and a stronger YouTube playback compatibility layer.

The release also hardens Levyra against recent YouTube stream-delivery, HTTP 403 and PoToken changes. Playback now has more reliable source selection and recovery paths, while a bounded server-driven compatibility policy can adjust already-implemented resolver/client choices without downloading executable code or silently changing Levyra's privacy and security model.

## What's New

### Player, Canvas and Mini Player

- Reworked Now Playing around a single artwork-derived ambience so artwork, Canvas, metadata, timeline and controls read as one composition instead of stacked layers.
- Refined the mini player with a calmer ambience-tinted surface, lighter chrome and a more deliberate progress treatment.
- Improved transport hierarchy, spacing, contrast and player action placement.
- Added Levyra's signature playback-wave polish to the timeline and media presentation while keeping animation tied to actual playback state.
- Canvas now preserves the source aspect ratio instead of stretching motion artwork to the display.
- Improved center-crop limits for unusually shaped Canvas sources so video remains proportional and blends into the surrounding ambience.
- Increased the quality ceiling for immersive Canvas presentation while keeping a lighter budget for smaller artwork surfaces.
- Improved selection of high-resolution Apple editorial motion artwork and matching for singles that have missing or generic album metadata.
- Added smoother static-artwork to Canvas transitions and improved contrast behind title, artist, timeline and transport controls.
- Added persistent Canvas quality preferences, including Auto, Data Saver and High.
- Added persistent Canvas source preferences for the supported motion-artwork providers.
- Added a hardened community/Spotify Canvas catalog path alongside Apple and Tidal fallbacks, with static artwork remaining the permanent fallback when no safe match exists.

### Search and Discovery

- Search now treats songs, music videos, albums, artists and playlists as real separate entities.
- Added Playlists and Videos as dedicated result categories.
- Entity classification now follows YouTube Music navigation/page metadata instead of localized subtitle words, improving behavior across languages.
- Added independent per-section pagination and loading/error state so "Show all" can expand one result type without inflating or blocking the whole search screen.
- Improved canonical deduplication so the same album or entity returned through different endpoints does not appear repeatedly, while genuinely distinct releases remain separate.
- Improved artist result handling, album deduplication and video artwork fallbacks.
- Isolated artist verification, track enrichment and album refinement so a failure in one enrichment path does not erase otherwise valid search sections.

### Album and Playlist Batch Downloads

- Album and playlist downloads are now grouped into persistent batches instead of behaving like unrelated per-track jobs.
- Batch state includes aggregate progress, retry and cancellation behavior.
- Batch children reuse the existing Room and WorkManager persistence path so progress can survive process recreation.
- Completed and cancelled batches are kept out of the active Library surface while failed batches remain visible for retry.
- Fixed batch membership and continuation edge cases found during review, including repeated continuation tokens and overlapping ownership of the same track.

### Downloads and Offline Reliability

- Fixed downloads that could stall around 4% before media bytes were actually transferred.
- Fixed tracks that downloaded only on the second attempt because a rejected probe incorrectly discarded an otherwise valid export source.
- Levyra can now reuse a complete progressive muxed MP4 when YouTube does not provide a usable audio-only stream, then extract the audio track locally with Media3 Transformer before normal tagging and MediaStore registration.
- Improved HTTP range validation, stale-source rejection and bounded range planning for offline exports.
- Fixed first-attempt download failures that could also interrupt or stall the track currently playing.
- Offline export now uses cache/failure handling isolated from live playback so a failed download cannot incorrectly quarantine the healthy source used by the player.
- Concurrent exports of the same track are coalesced around the cache writer instead of pulling the same bytes from upstream multiple times.
- Improved cleanup and staging-space handling when a muxed source must be reduced to audio.

### Library

- Redesigned the Android Library around a clearer, more compact hierarchy while preserving existing actions.
- Simplified the header, category navigation, search, sort and selection controls.
- Rebuilt track-row hierarchy so title, artist and technical metadata are easier to scan.
- Consolidated secondary actions into a cleaner overflow flow while preserving play, queue, playlist, download and delete behavior.
- Improved single-download removal with confirmation and clearer empty states for an empty Library versus an empty search result.
- Simplified the offline-download summary and fixed search-field clipping/layout issues.
- Fixed Listening Pulse header wrapping and weekly-chart scaling issues.

### Playlist Sharing

- Added serverless Levyra playlist sharing through a versioned Levyra payload and `levyra://playlist` deep link.
- Shared playlists carry bounded playlist metadata and track identities rather than private playback URLs.
- Received playlists reuse Levyra's existing preview/import resolution flow and can be resolved against current playable sources before import.
- Added integrity and size limits to keep shared payloads bounded and deterministic.

### Video Metadata

- Added an optional DeArrow-backed video metadata enhancement path.
- When enabled, supported video items can use community-improved titles and thumbnails.
- The setting is opt-in and is scoped to video metadata rather than rewriting normal music-track identity.

## Playback Reliability

### YouTube stream selection and recovery

- Levyra now rejects adaptive GoogleVideo audio sources that cannot reliably serve a complete track instead of allowing playback to stop after the initial buffered portion.
- When needed, playback can fall back to a complete progressive source while remaining in Song mode.
- Improved handling of failed or no-op YouTube `n` transformations so a throttled URL is not published as if it were healthy.
- Added/refined VisionOS and Android Reel paths and bounded Android Reel responses.
- Improved quarantine behavior so the URL that actually failed is rejected on the next resolution attempt instead of immediately looping back into the same 403 source.
- Fixed cases where native Video mode reused an audio-only cached source and produced audio over a blank video surface.
- Preserved original song identity across Song -> Video -> Song round trips.

### PoToken compatibility

- Restored playback against newer YouTube anti-bot behavior using Levyra's existing PoToken infrastructure.
- PoToken-capable/attested playback paths are preferred when the current response requires them.
- Android Reel can act as a proven song fallback when ordinary audio candidates are stale or rejected.
- The local YouTube player configuration and decoder paths have been synchronized with the current playback strategy.

### Server-driven compatibility policy

Levyra now includes a bounded server-driven playback compatibility policy. It may adjust only behavior that already exists in the APK, such as resolver strategy order, supported client enable/disable state, client priority, selected client versions and PoToken requirements for implemented clients.

The policy cannot download executable code, inject arbitrary endpoints, add credentials, provide cookies or introduce a new playback implementation. Invalid policies are rejected and the last known-good policy remains available locally. Playback failures can request an early refresh so selected upstream compatibility changes do not always require an emergency APK.

### Queue precache and crossfade

- Improved reuse between queue precache, manual skip, auto-advance and crossfade preparation.
- Already resolved upcoming tracks can be reused instead of resolving the same queue item repeatedly.
- Crossfade preparation can reuse warmed data while the main MediaSession player remains the authoritative playback owner.

## Platform and Dependency Updates

- AndroidX Media3 moved from 1.10.1 to 1.11.0 across the Android media stack.
- Compose BOM moved to the 2026.08 generation.
- Baseline Profile tooling moved to the 1.5.0 release-candidate line.
- Existing Android package identity, signing identity and Android/Desktop version separation remain unchanged.

## Foundations Included in This Build

The 2.3.21 development cycle also adds provider-abstracted foundations for music recognition and remote/Cast playback handoff. These are deliberately not advertised as active end-user services in the default build: the recognition path still requires a compatible backend, and the standard/F-Droid build does not ship a proprietary Cast backend. The abstractions are present so a future implementation can integrate without duplicating Levyra's player, queue or DSP state architecture.

## F-Droid and Repository Hardening

- Hardened the F-Droid dependency/network disclosure contract and added regression coverage against metadata drift.
- Added the F-Droid download surface to the project documentation and refreshed the GitHub presentation assets.
- Continued to keep the F-Droid build path separate from the signed GitHub update channel while sharing the same Android source version.
- Added an autonomous YouTube canary/repair workflow foundation that can prepare repository repair work when repeated public playback evidence shows a genuine upstream regression; it does not automatically merge, tag or release an APK.

## Notes

This is an Android-only version bump. Levyra Desktop remains independently versioned and released. No account system, advertising identifier, analytics pipeline or user-tracking system is introduced by 2.3.21.

Existing favorites, playlists, listening history, downloads, settings and playback state remain part of the compatibility contract. The batch-download persistence changes use the existing non-destructive Room migration path.

## Versioning

- Version name: `2.3.21`
- Version code: `2032100`

## Validation

- The Android version name/code follow Levyra's existing monotonic formula and remain separate from Desktop versioning.
- Gradle version fallbacks, README version wiring, release badges and Fastlane changelogs are aligned to 2.3.21 in the release bump.
- Levyra's Release Guard continues to verify Gradle, README, curated release-notes, F-Droid review-contract and Android publishing-workflow consistency before a release tag is accepted.
- Feature changes summarized above were merged with their focused tests/review fixes where present in the repository history.
- This metadata bump does not claim a new physical-device, Android Auto, notification, PiP or OEM verification run that has not actually been performed for the final 2.3.21 artifact.

## Upgrade notes

No manual migration is required. Existing libraries, favorites, playlists, listening history, downloads, settings and playback state remain compatible. GitHub installations continue to use Levyra's signed GitHub release/update path, while F-Droid builds remain on the reproducible F-Droid channel and follow F-Droid's own build/index schedule.

## Final note

Levyra 2.3.21 is primarily about making the Android experience feel more complete without sacrificing the reliability underneath it: a cleaner player and Library, richer discovery and offline management, better Canvas presentation, and a substantially stronger playback layer for a YouTube environment that keeps changing.
