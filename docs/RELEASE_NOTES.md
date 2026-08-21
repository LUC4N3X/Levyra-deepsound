# Levyra 2.3.22

## Highlights

Levyra 2.3.22 is an Android-focused reliability update built on top of 2.3.21. The main fix addresses the native-memory growth reported in issue #427, while the release also includes the YouTube playback hardening and Android experience work merged after 2.3.21.

## Playback and memory reliability

- Fixed runaway native-memory growth during normal music playback and navigation that could eventually let Android kill the process.
- Song mode no longer keeps an unnecessary video track selected when a muxed YouTube source is used for audio-only listening, avoiding needless decoder and buffer-pool pressure.
- Added a device-aware native-memory guard that can recycle the playback pipeline after sustained pressure while preserving the current item, playback position, play state and queue.
- Video Mode remains explicitly video-capable and is not downgraded by the music-playback memory fix.
- Added stronger playback-cache recovery and bounded recovery behavior so stale or rejected sources are less likely to trap playback in repeated failures.
- Improved strategy-health handling across the existing playback resolver so temporarily unhealthy YouTube paths can be deprioritized without bypassing Levyra's allowlisted compatibility policy.

## YouTube compatibility

- Updated the local YouTube player configuration data used by Levyra's decoder/extractor paths.
- Hardened playback and metadata extraction against current YouTube delivery behavior, including muxed audio identity, modern stream metadata and recovery from rejected sources.
- Improved handling around PoToken/session-backed playback paths while preserving Levyra's existing privacy and security boundaries.
- Strengthened the vendored LevyraExtractor integration for current YouTube stream, playlist and SABR metadata behavior.

## Android experience updates

- Expanded Listening Pulse with Replay-oriented listening views.
- Added local lyrics share cards using Levyra's scoped file-sharing path.
- Refined player ambience and Canvas transitions, including improved artwork-derived color treatment.
- Added an opt-in pure-black appearance mode while keeping the existing AMOLED theme behavior distinct.
- Centralized semantic haptic feedback behind a user-controlled setting.
- Improved launcher-mask resources for safer adaptive-icon rendering across Android launchers.

## Downloads and offline handling

- Improved offline export recovery and source validation around refreshed or rejected YouTube media URLs.
- Preserved isolation between download failures and the active playback source so an export problem does not incorrectly poison healthy live playback state.

## Notes

This is an Android-only version bump. Levyra Desktop remains independently versioned and is not changed by 2.3.22.

Existing favorites, playlists, listening history, downloads, settings and playback state remain part of the compatibility contract. No account system, advertising identifier, analytics pipeline or user-tracking system is introduced by this release.

## Versioning

- Version name: `2.3.22`
- Version code: `2032200`

## Validation

- Android `versionName` and `versionCode` follow Levyra's existing monotonic version formula and remain separate from Desktop versioning.
- `gradle.properties`, Android Gradle fallbacks, README version wiring, architecture metadata, the Android platform badge and Fastlane changelogs are aligned to 2.3.22.
- The separate "Latest release" badge intentionally remains on the most recently published GitHub release until 2.3.22 is actually published.
- The memory fix landed with focused policy coverage in the repository, and the broader Android playback work includes its corresponding regression tests where present in the merged history.
- This metadata bump does not claim a new physical-device, Android Auto, notification, PiP or OEM verification run that has not actually been performed for the final 2.3.22 artifact.

## Upgrade notes

No manual migration is required. Existing libraries, favorites, playlists, listening history, downloads, settings and playback state remain compatible. GitHub installations continue to use Levyra's signed GitHub release/update path, while F-Droid builds remain on the reproducible F-Droid channel and follow F-Droid's own build/index schedule.

## Final note

Levyra 2.3.22 is primarily about keeping long listening sessions stable while strengthening the playback layer underneath them. The most important change is the native-memory fix, backed by the YouTube resilience and Android experience improvements merged since 2.3.21.