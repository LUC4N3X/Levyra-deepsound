# Levyra Active Tasks

## Active phase

**Name:** Desktop owned-music playback and Android surface control

**Roadmap tracks:** Track 1 - Playback critical path; Track 2 - Persistence, offline use, and recovery; Track 3 - Responsive, accessible interface; Track 5 - Windows Desktop reliability

**Status:** Implementation and review hardening complete; latest-head CI and native/device checks pending
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
  back into the same entries without replacing an existing export with a
  partial write.
- The dual-player transition advances the queue only when the prepared track
  still matches the queue, and cancels on seek, pause, manual change, queue
  mutation, end-of-track sleep and shutdown.
- Android pure black leaves both normal dark and the existing AMOLED palette
  semantics untouched unless the explicit pure-black preference is enabled.
- Haptic feedback has one owner and one user preference.
- No Room migration, account, cookie, private token, telemetry, tracking,
  permission expansion, version change, merge, tag or release.

## Work items

- [x] Desktop audio output device selection with availability watch.
- [x] Desktop equalizer presets with derived preamp headroom.
- [x] Desktop local music library: tag reader, scanner, watcher, index, screen.
- [x] Desktop M3U import and atomic export.
- [x] Desktop dual-player transition with equal-power crossfade.
- [x] Android pure black mode and centralized haptics.
- [x] Review hardening for player lifecycle, transition failure paths, local
  path identity, scan serialization, output-device semantics, parser bounds,
  large embedded artwork and narrow-width playlist actions.
- [x] Focused regression tests for the new pure logic and parser edge cases.
- [ ] Latest-head GitHub PR workflows all complete successfully.
- [ ] Run `python scripts/ai_quality_gate.py --profile full` where the full
  environment is available.
- [ ] Verify real libvlc output-device switching, hot-plug and crossfade on
  Windows with a physical audio device.
- [ ] Verify a large local library scan, the filesystem watcher and playback of
  representative supported containers on Windows.
- [ ] Verify Android pure black and haptics on a physical device.

## Current validation evidence

- Earlier iterations of this phase exercised Desktop `check`, Android compile
  and unit tests, and the fast AI quality gate, but review hardening changed the
  branch afterward. Those older results are not treated as latest-head proof.
- GitHub PR workflows are the current automated validation target for the exact
  final branch head. Record them as complete only after all runs for that SHA
  finish successfully.
- Focused tests now cover M3U Windows/UNC paths across hosts, path-boundary
  identity, output-module/device persistence, Opus embedded artwork including a
  multi-page packet, Opus pre-skip duration semantics, equalizer presets,
  scanner behavior and Android AMOLED versus explicit pure-black state.
- Real libvlc playback, output-device hot-plug, audible crossfade/gapless handoff,
  large real-library scans, filesystem watcher behavior, Android device
  rendering and haptic feel remain manual/native checks until directly tested.

## Preserved behavior

- Online catalog, search, downloads, queue, session restore, favorites,
  playlists, lyrics, history, settings, localization and onboarding remain under
  their existing owners; the phase extends rather than replaces them.
- Direct playback stays the critical path: scanning, watching, tag reading,
  local search and transition preparation are bounded/cancellable and kept off
  the Compose UI path where applicable.
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
