# Levyra 2.5.0

## Highlights

Levyra 2.5.0 is a substantial Android release built on the 126 commits merged after 2.3.33. It expands discovery and personal listening features, strengthens playback recovery and resume behavior, adds new system-level quick controls, upgrades the visual experience around artwork and lyrics, and keeps the privacy-first local data model intact.

## Discovery, search and personal listening

- Added Levyra Mix with multiple mix styles, a familiar-to-discovery balance, bounded candidate ranking, deduplication, cancellation-safe generation and playback through the existing queue engine.
- Added Your Sound, a fully on-device listening view covering 7 days, 30 days, 6 months and All Time, derived from existing listening history rather than a remote profile.
- Listening Pulse now includes a 24-hour listening rhythm and artist-distribution views, with lifetime aggregates that remain accurate after raw events are pruned.
- Search suggestions and YouTube Music discovery were hardened so empty structural responses no longer stop fallback handling and provider labels are not exposed as fake artist credits.
- Artist resolution now prefers authoritative identities when names collide, and the search top result can expose relevant artist tracks and play-count context without duplicating the primary result.
- Automatic song matching now scores real candidates instead of trusting an unrelated first result, while preserving existing fallbacks when confidence is insufficient.

## Playback, queue and system controls

- Added a service-owned sleep timer with 15, 30, 45 and 60 minute presets, end-of-track behavior and explicit cancellation.
- Added a music-recognition Quick Settings tile and launcher shortcuts for Search, Library and recognition, reusing the existing navigation and recognition owners.
- Queue rows can now be removed with a swipe and restored through a bounded single-entry Undo path.
- Playback resume state now keeps the saved position across controller connection, queue reconstruction, UI resubscription and real MediaSession item resolution instead of restarting at zero.
- Playback recovery now handles eligible remote and timeout failures more consistently while keeping fatal-error handling separate.
- Crossfade timing follows playback speed and keeps transition-player playback parameters synchronized with the primary player.
- Download task projections avoid loading full track payloads when only lightweight active-task state is required.

## Artwork, lyrics and interface

- Added Living Artwork as the Song Mode fallback when Canvas is enabled but no real Canvas exists, using AGSL on Android 13+ and a Compose fallback below it.
- Added a direct Canvas/Living Artwork toggle in Now Playing while preserving real Canvas priority and native Video Mode separation.
- Added a full-screen zoomable artwork preview with save-to-gallery through MediaStore and safer bitmap/file handling across supported Android versions.
- Lyrics can be selected with long-press and exported as a 1080x1080 Levyra share card; TalkBack actions remain available for the same flow.
- Fixed karaoke word spacing for providers that return trailing whitespace in word-level timing data.
- Introduced a shared typography rhythm with role-specific line heights and letter spacing so accented characters, descenders and multi-line metadata render more reliably.
- Connected list surfaces, playing indicators, press feedback and shimmer behavior were consolidated into reusable Levyra UI primitives instead of parallel one-off implementations.

## Audio and Android Auto

- Reworked the Android Audio settings screen into grouped Streaming Quality, Equalizer, Spatial, Dynamics and Playback sections.
- Added an interactive 10-band equalizer curve with per-band accessibility semantics, reset behavior and existing Levyra audio-state ownership.
- Android Auto browsing now honors bounded page/page-size requests down to the data layer instead of relying on the previous fixed list cut, including stable paging for downloads.
- Google-hosted artwork upscaling now uses one validated helper and avoids corrupting signed/query-bearing URLs.

## Local data and compatibility

- Counted plays now require meaningful listening time, or a genuine completion for short tracks, instead of treating very short listens as full plays.
- Lifetime listening aggregates use non-destructive Room migrations and an idempotent backfill so All Time statistics survive raw-event pruning.
- Existing favorites, playlists, queue, downloads, settings, history and backups remain within the compatibility contract.
- No account system, advertising identifier, analytics pipeline or user-tracking system is introduced by this release.

## YouTube and build maintenance

- Refreshed the bundled YouTube player configuration and player-date metadata used by Levyra's extractor paths.
- Kept current resolver/privacy boundaries and compatibility fallbacks while improving search and metadata parsing around current YouTube Music responses.
- Updated the Gradle wrapper and selected Android/networking dependencies already merged on `main`, including the current Android Gradle plugin and OkHttp line.

## Validation

- This release metadata is based on the current `main` history from Android 2.3.33 commit `e293fc63` through pre-release head `e8a0acc1`, a range of 126 commits.
- The merged feature work includes focused regression coverage for playback resume, recovery classification, sleep timer behavior, queue-removal Undo, search identity and suggestions, Android Auto pagination, counted-play policy, lifetime listening projections, Living Artwork support, artwork preview behavior, localization and related UI/domain contracts.
- The final signed 2.5.0 artifact has not been claimed as validated before publication. The existing release workflow remains responsible for release-note validation, Android lint, signed release assembly, APK metadata verification, signer-certificate verification, checksum generation and published-asset verification.
- Physical-device behavior, Android Auto host behavior and long-session runtime characteristics remain represented only by the direct evidence already attached to the underlying merged changes; this version bump does not invent a new device test run.

## Notes

This is an Android-only release. Levyra Desktop remains independently versioned and is not changed by 2.5.0.

## Versioning

- Version name: `2.5.0`
- Version code: `2050000`

`gradle.properties`, the Android Gradle fallbacks, README version wiring, architecture metadata, Android platform badge and Fastlane changelogs are aligned to 2.5.0. The separate "Latest release" badge intentionally remains on the most recently published GitHub release until 2.5.0 is actually published.

## Upgrade notes

No manual migration is required. The Room migrations included in the merged Android changes are non-destructive and are designed to preserve existing libraries, favorites, playlists, listening history, downloads, settings, playback state and backups. GitHub installations continue to use Levyra's signed GitHub release/update path, while F-Droid builds remain on the reproducible F-Droid channel and follow F-Droid's own build and index schedule.

## Final note

Levyra 2.5.0 moves the Android app forward as one coherent release: richer local discovery, more trustworthy listening insights, stronger playback recovery, direct system controls, a more distinctive artwork and lyric experience, and continued separation between user-facing features and the low-latency playback owners underneath them.
