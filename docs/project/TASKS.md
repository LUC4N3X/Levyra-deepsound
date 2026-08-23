# Levyra Active Tasks

## Active phase

**Name:** Playback native-memory churn fix for issue #427

**Roadmap tracks:** Track 1 - Playback critical path; Track 4 - Performance and efficiency

**Status:** Root cause identified and fixed with heapprofd evidence on `Levyra_Pixel8_API37` / `emulator-5554`; reporter diagnostics retained for the mandatory physical Pixel 8 confirmation; issue #427 stays open until that confirmation arrives
**Scope:** Remove the per-call `Regex` compilation that ran on playback-driven hot paths and drove multi-GB native allocator churn, without disabling or degrading recommendations, lyrics, Motion Artwork, playback, queue or resolver behavior. Keep one bounded local diagnostics CSV so the reporter can validate the fix on the physical device.

## Acceptance criteria

- Every regex corrected in this phase keeps an identical pattern, identical flags and identical escaping; only the compilation site moves to process-lifetime state.
- Recommendations, lyrics parsing, Motion Artwork identity and playback candidate scoring keep producing the same results.
- `PlaybackService` remains the single diagnostic owner and samples every five seconds without calling `System.gc`, `dumpsys`, heap-dump APIs or player-reset paths.
- Sampling reads `/proc/self/status`, `/proc/self/smaps_rollup`, `/proc/self/fd`, `Debug.getNativeHeapAllocatedSize()` and JVM heap state off the Main dispatcher.
- The CSV is local-only, capped at 512 KiB, contains no URL, token or title, and is pulled with an explicit-serial PowerShell command.
- No `System.gc`, player restart, auto-pause, service restart, memory cap workaround or device-specific behavior is introduced.
- Music Mode, Video Mode, Canvas, lyrics, queue, MediaSession, background playback, prefetch, resolver, downloads/cache, DSP, versions, permissions, signing and release behavior remain unchanged.

## Work items

- [x] Reconcile issue #427 with merged PR #436 and confirm the physical Pixel stock-ROM failure remains open.
- [x] Quantify `VisualizerAudioProcessor` from source and run an A/B build with the processor removed from the `DefaultAudioSink` chain; exclude it as the principal root cause.
- [x] Enumerate every `ByteBuffer.allocateDirect` in the custom `AudioProcessor` chain and confirm all six processors reuse a grow-only buffer instead of allocating per PCM buffer.
- [x] Attribute the growing region with `/proc/<pid>/smaps` to `[anon:scudo:primary]` rather than the Dalvik heap.
- [x] Capture heapprofd and identify `refreshListeningPulse` -> `rankAlbumRecommendations` -> `albumRecommendationTextKey` -> `Regex.<init>` -> ICU `RegexPattern::compile` as the dominant native allocator.
- [x] Precompile the recommendation, artist-identity, playback-candidate and source-identity regexes and memoize `albumRecommendationTextKey`.
- [x] Re-profile, then precompile the second tier surfaced by heapprofd: `normalizeMotionText`, `splitArtists`, `LyricsSectionDetector.normalize`, `LevyraLocalIntelligence.normalize`, `LyricsCleaner.normalizeText` and the per-line lyrics whitespace regexes.
- [x] Add focused normalization and memoization regression tests.
- [x] Run the Android debug unit suite and the repository fast AI quality gate.
- [ ] Collect the reporter's physical Pixel 8 `PLAY -> PAUSE 15 s -> PLAY` CSV over 20-30 minutes of playback and confirm memory no longer climbs toward 1-4 GB.
- [ ] Keep #427 open, and continue from the reporter's CSV if growth persists on the physical device.

## Current validation evidence

- heapprofd on `emulator-5554`, 80 s windows, sampling interval 4096 B, allocations attributed to the nearest Levyra frame:
  - before: `albumRecommendationTextKey` 3936 MB / 324,376 allocations; all Levyra-attributed allocation about 3958 MB per 80 s (about 2969 MiB/min).
  - after the recommendation fix: `albumRecommendationTextKey` 12.5 MB / 3,134 allocations; all Levyra-attributed allocation about 15.7 MB per 80 s.
  - ICU `UnicodeSet::compact` fell from 171.0 MB / 29,794 allocations to zero; total `malloc` leaf fell from 4246.9 MB to 756.1 MB per 80 s.
  - after the second tier: all Levyra-attributed allocation about 3.3 MB per 80 s (about 2.5 MiB/min), with total `malloc` outstanding at 2.6 MB. Remaining leaders are libhwui/Skia, Binder and libsync with zero outstanding bytes.
- `/proc/<pid>/smaps` diff during growth attributed the increase to `[anon:scudo:primary]` (+23 MB between snapshots) while the Dalvik heap moved +2 MB, confirming native allocator churn rather than a managed-heap leak.
- Pre-fix sampling showed `RssAnon` climbing about 2.2 MB every 4 s (about 33 MiB/min) with roughly 50 MB reclaims every 50 s. Post-fix, a 25.9-minute run moved `RssAnon` from 281 MB to 242 MB (slope -1.19 MiB/min) and the final ten minutes stayed inside a 6 MB band.
- The post-fix 15-second pause window changed `RssAnon` by about 1 MB, so there is no longer a large reclaimable pool that pausing releases.
- A/B on the emulator with `VisualizerAudioProcessor` removed from the audio-processor array showed no meaningful difference, and its computed budget is about 1.3 MiB/min of short-lived managed garbage. It is not the root cause.
- All six custom `AudioProcessor` implementations allocate their direct `ByteBuffer` only when capacity must grow and reuse it afterwards, so they are not a per-buffer native allocation source.
- `:app:testDebugUnitTest` passes with 1161 tests, 0 failures, 0 errors. The repository fast AI quality gate passes, including script tests and PowerShell syntax validation.
- No physical device was used. Every ADB command named `emulator-5554`; the owner's Samsung remained untouched.

## Preserved behavior and explicit boundaries

- Every regex keeps its original pattern, flags and escaping; only the compilation site changed.
- `albumRecommendationTextKey` memoization is bounded to 1024 entries with a 256-character input limit and falls back to direct computation beyond it, so results are unchanged.
- Primary playback, MediaSession, queue, transition player and memory recovery remain unchanged and owned by `PlaybackService`.
- The diagnostic path performs fixed local reads and an append on `Dispatchers.IO`; it does not mutate player, resolver, cache or queue state.
- The CSV never leaves the device automatically and contains no URL, token, cookie, credential or private request payload.
- Android and Desktop versions, dependencies, packages, artifacts, signing, tags and releases remain unchanged.

## Rollback boundary

Revert the precompiled regex declarations and the memoization helper, the playback diagnostic log, the `PlaybackService` sampler, the resolver active-count accessor, the focused tests and the pull script. No user-owned library, queue, settings or database migration is involved.

## Update rule

Record CI, review, physical-device, merge and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
