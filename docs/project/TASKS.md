# Levyra Active Tasks

## Active phase

**Name:** Local intelligence, automatic recovery, templated car UI, and real audio transitions

**Roadmap tracks:** Track 1 - Playback critical path; Track 2 - Persistence, offline use, and recovery; Track 3 - Responsive, accessible interface

**Status:** Implementation and available local checks complete; draft PR pending
**Scope:** Add local smart playlists, opt-in automatic backups, an Android Auto Car App templated surface, and service-owned real crossfade/AutoMix. Preserve classic Android Auto, the explicit audio/video choice, the persistent queue, existing backup compatibility, user data, Android/Desktop versions, signing, packaging, and release behavior.

## Verified current behavior and rationale

- The Library already exposed recent and most-played smart cards, but recent
  history was deduplicated before `LibraryCatalog` counted it. Every play count
  was therefore one and the most-played ordering was not meaningful.
- Manual backup/restore already produced checksum-verified archives and restored
  transactionally, but no periodic scheduler, atomic automatic destination, or
  retention policy existed.
- `AndroidAutoLibrary` already exposed a rich Media3 browse tree through
  `PlaybackService`; no Car App templated UI consumed it.
- ViewModel crossfade faded one player down, changed source, and faded the same
  player up. The streams never overlapped, so it was not a real crossfade.

## Acceptance criteria

- Most-played ranks 30-day local listening time, play count, and recency without
  persisting resolved stream URLs or introducing a second catalog.
- Recent, favorites, offline, playlists, downloads, and existing Library actions
  remain unchanged.
- Automatic backup is opt-in; frequency, charging constraint, and bounded
  retention are user configurable.
- Automatic archives reuse the existing payload/checksum format, exclude audio,
  finalize atomically in app-private storage, and keep manual export/restore
  backward compatible.
- Classic MediaBrowser Android Auto remains available.
- The templated Car App surface exposes Home, Download, Favorites, Playlists,
  browse, search, queue, and now-playing using the existing MediaSession and
  `AndroidAutoLibrary`.
- Release car hosts are validated; arbitrary hosts are permitted only in debug.
- Audio-mode crossfade overlaps two real ExoPlayers with equal-power gains.
- AutoMix adapts bounded duration only from local energy/vocal metadata.
- Queue generation and identity prevent stale transition publication.
- Pause, seek, queue mutation, repeat-one, native-video mode, low-RAM pressure,
  and lifecycle cleanup cancel the secondary player.
- MediaSession, notification, queue, background service, and Android Auto return
  to the primary player after handoff.
- No Room migration, account, cookie, private token, telemetry, tracking,
  permission expansion, version change, Desktop change, merge, tag, or release.

## Work items

- [x] Inspect SimpMusic Car App, automatic backup, and crossfade architecture.
- [x] Credit the GPL-3.0 architectural reference in third-party notices.
- [x] Implement and test deterministic local most-played ranking.
- [x] Implement DataStore backup settings and backward-compatible serialization.
- [x] Implement WorkManager scheduling, atomic archive creation, and retention.
- [x] Add automatic-backup settings UI.
- [x] Add Car App dependencies, manifest service, platform-token handshake, tabs,
  browse, search, queue, and now-playing templates.
- [x] Implement service-owned dual-player equal-power transition and AutoMix
  planning with independent DSP processors.
- [x] Remove the old single-player pseudo-crossfade path.
- [x] Add focused smart-playlist, backup-retention, and AutoMix tests.
- [x] Run focused unit tests and debug Kotlin compilation.
- [x] Run the full Android unit suite and record its four out-of-scope failures.
- [x] Run debug lint and debug assembly.
- [ ] Run release assembly where local signing/configuration permits.
- [x] Complete final security and diff review.
- [x] Prepare the authorized branch and draft pull-request handoff.
- [ ] Verify Android Auto templates on DHU or a compatible head unit.
- [ ] Verify real audio overlap, pause/seek/skip, notification, lock screen,
  repeat, shuffle, EQ/normalization, low-memory cancellation, and native video
  on a physical device.

## Current validation evidence

- `:app:compileDebugKotlin`: passed locally with Android SDK configured through
  `ANDROID_HOME`.
- Focused `:app:testDebugUnitTest` selectors for smart playlists, automatic
  backup retention, AutoMix, and Library catalog: passed locally.
- Full `:app:testDebugUnitTest`: 624 tests executed, with four failures in
  untouched areas (`AlbumRecommendationPolicyTest`, two
  `YoutubeLocalDecoderTest` fixtures, and `PlayerExpansionTest`).
- `:app:lintDebug`: passed locally.
- `:app:assembleDebug`: passed locally.
- `:app:lintRelease`: blocked before execution by the repository's required
  `YOUTUBE_INNERTUBE_API_KEY` gate. The F-Droid release lint path is blocked by
  the unavailable JDK 21 toolchain.
- `git diff --check`: passed at the current working state.
- Physical-device audio, Android Auto host, notification/lock-screen, memory
  pressure, and automatic WorkManager execution checks remain unverified.
- The previous Home identity phase recorded implementation completion but its
  reported final-head CI/device checks were not reclassified as passed here.

## Preserved behavior

- The explicit song/audio versus native-video selection is unchanged.
- Static artwork, downloads, favorites, playlists, queue, lyrics, history,
  settings, localization, onboarding, sessions, and backup restore remain.
- Direct playback remains the critical path; transition preparation is bounded,
  cancellable, and disabled on low-RAM devices and native video.
- Android and Desktop versions, packages, artifacts, signing, tags, and releases
  remain independent and unchanged.

## Rollback boundary

Revert the smart-playlist projection, automatic-backup settings/worker, Car App
surface/dependencies, dual-player transition planner/service integration, tests,
attribution, and planning documentation as one Android-only change. No Room
migration or durable data rollback is required; unknown DataStore keys are
ignored by older builds and existing backup schema readers remain compatible.

## Update rule

Record CI, review, DHU, physical-device, merge, and release status only from
direct evidence. Replace this phase when a new reviewable task begins instead
of accumulating unrelated work.
