# Levyra Active Tasks

## Active phase

**Name:** Desktop owned-music playback and Android surface control

**Roadmap tracks:** Track 1 - Playback critical path; Track 2 - Persistence, offline use, and recovery; Track 3 - Responsive, accessible interface; Track 5 - Windows Desktop reliability

**Status:** Implementation and available local checks complete; pull request open, device checks pending
**Scope:** Add Desktop audio output-device selection, equalizer presets, a local music library, M3U transfer and a dual-player transition, plus an Android pure black mode and one haptic owner. Preserve the online catalog, downloads, queue, session restore, favorites, playlists, history, settings, localization, Android/Desktop versions, signing, packaging and release behavior.

## Verified current behavior and rationale

- Desktop played through whatever output libvlc selected; there was no device
  list, no persisted choice and no recovery when a device disappeared.
- The Desktop equalizer exposed ten raw sliders with no curated curves and no
  preamp headroom rule.
- Desktop had no notion of owned local files: only resolved online streams and
  its own downloads were playable.
- Desktop track changes stopped the media player and opened the next stream, so
  every change paid the resolve and open latency and no overlap was possible.
- Android shipped an AMOLED palette, but choosing it replaced the accents of the
  palette the user had selected; there was no orthogonal pure black option.
- Android haptics were raw `performHapticFeedback` calls in two files with no
  user control and inconsistent feedback types.

## Acceptance criteria

- The Desktop output device list, selection, persistence, live apply, fallback
  to the system default and recovery all work without restarting the app.
- Equalizer presets never exceed the available headroom and a manual band edit
  resolves to Custom without a second stored source of truth.
- The local library indexes owned files, survives moves by marking vanished
  files unavailable, and never routes a local track through the online
  resolver.
- M3U import matches files against the local index first and export round-trips
  back into the same entries.
- The dual-player transition advances the queue only when the prepared track
  still matches the queue, and cancels on seek, pause, manual change, queue
  mutation and shutdown.
- Android pure black leaves the normal dark theme untouched and is opt-in.
- Haptic feedback has one owner and one user preference.
- No Room migration, account, cookie, private token, telemetry, tracking,
  permission expansion, version change, merge, tag or release.

## Work items

- [x] Desktop audio output device selection with availability watch.
- [x] Desktop equalizer presets with derived preamp headroom.
- [x] Desktop local music library: tag reader, scanner, watcher, index, screen.
- [x] Desktop M3U import and export.
- [x] Desktop dual-player transition with equal-power crossfade.
- [x] Android pure black mode and centralized haptics.
- [x] Focused unit tests for the new pure logic.
- [x] `desktop/gradlew check` after every Desktop change.
- [x] `:app:compileDebugKotlin` and `:app:testDebugUnitTest`.
- [x] `python scripts/ai_quality_gate.py --profile fast` before every commit.
- [ ] Run `python scripts/ai_quality_gate.py --profile full`.
- [ ] Run `:app:lintRelease` and release assembly where signing inputs permit.
- [ ] Verify real libvlc output-device switching, hot-plug and crossfade on
  Windows with a physical audio device.
- [ ] Verify a large local library scan, the filesystem watcher and playback of
  every supported container on Windows.
- [ ] Verify Android pure black and haptics on a physical device.

## Current validation evidence

- `desktop/gradlew check` with a JDK 21 toolchain: passed after every Desktop
  commit.
- `:app:compileDebugKotlin`: passed locally.
- `:app:testDebugUnitTest`: 1108 tests, 1 failure
  (`DirectAudioFallbackContractTest.strictCandidateProbeIsLimitedToNormalAudioFallback`),
  identical to the pre-change baseline recorded on `main`.
- `python scripts/ai_quality_gate.py --profile fast`: passed before each commit.
- `:app:lintRelease` and release assembly: not run.
- Real libvlc playback, output-device hot-plug, crossfade audio, large library
  scans, the filesystem watcher, Android device rendering and haptics remain
  unverified.

## Preserved behavior

- Online catalog, search, downloads, queue, session restore, favorites,
  playlists, lyrics, history, settings, localization and onboarding are
  unchanged.
- Direct playback stays the critical path: scanning, watching, tag reading and
  transition preparation are bounded, cancellable and off the UI thread.
- Android and Desktop versions, packages, artifacts, signing, tags and releases
  remain independent and unchanged.

## Rollback boundary

Revert the Desktop `localmusic` package, the output-device and transition code
in the player module and playback controller, the Desktop settings additions,
the Android pure black and haptics change, tests and this planning file as one
change. Local data is additive: `localmusic.json` is a new file and unknown
preference keys are ignored by older builds.

## Update rule

Record CI, review, physical-device, merge and release status only from direct
evidence. Replace this phase when a new reviewable task begins instead of
accumulating unrelated work.
