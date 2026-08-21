# Levyra Active Tasks

## Active phase

**Name:** Android playback resource lifecycle hardening

**Roadmap tracks:** Track 1 - Playback critical path; Track 4 - Performance and efficiency

**Status:** Implementation, independent review, local full quality gate and unsigned release build complete; physical-device and exact-head CI checks pending
**Scope:** Make every playback resource owner and terminal path explicit, prevent the audio-mode transition player from creating video renderers, keep native-heap sampling off the service Main dispatcher, prefer existing audio-only Reel streams before muxed fallback, and bound long-lived resolver URL caches. Preserve playback modes, Canvas isolation, queue/session state, audio settings, offline playback, compatibility fallbacks, versions, signing and release behavior.

## Acceptance criteria

- The primary ExoPlayer, transition ExoPlayer, Canvas player, listeners, surfaces, jobs and audio processors each retain one clear owner and deterministic release path.
- Audio-mode crossfade never creates a video renderer or decoder, including when resolution falls back to a muxed stream.
- Music resolution prefers the existing audio-only Reel path, while preserving muxed and later compatibility fallbacks.
- Native-heap sampling runs outside the service Main dispatcher; recovery reuses that sample and preserves the current item, position and play intent.
- In-memory and persisted stream URL caches, failed-playback URL quarantine and video rejection quarantine prune expiry and remain within fixed bounds.
- Canvas remains decorative, muted and independently owned; native Video Mode retains its video renderer and explicit user choice.
- No new player, resolver, cache source of truth, dependency, permission, telemetry, version change, merge, tag or release.

## Work items

- [x] Audit current Levyra ownership and cleanup paths against the pinned vivi reference without importing its weaker lifecycle implementation.
- [x] Give primary audio processors service-instance ownership and retain the existing public settings route through the active service.
- [x] Remove video renderers from the audio-only transition player and centralize idempotent secondary cleanup.
- [x] Move native-heap sampling off Main and pass the measured value into recovery.
- [x] Put Reel audio-only before muxed fallback in bundled and repository compatibility policy revision `2026082101`.
- [x] Add deterministic expiry pruning and bounds for stream and failed/rejected URL caches.
- [x] Add focused lifecycle, cache-policy, compatibility-policy and offline isolation regression tests.
- [x] Resolve independent review findings: separate audio-only/muxed strategy responsibility, guarantee a new first-strategy canary, preserve pre-activation audio settings and remove malformed persisted cache records.
- [x] Run the complete Android debug unit-test suite on the implementation head.
- [x] Build the current unsigned F-Droid release APK and run the repository fast/full AI quality gates.
- [x] Complete independent diff review and resolve valid findings.
- [ ] Verify 50 queue transitions, crossfade on/off, Canvas enter/leave and repeated Music/Video switching on the owner's physical device, recording memory and codec evidence.
- [ ] Verify Draft PR workflows for the exact published head.

## Current validation evidence

- `:app:testDebugUnitTest` passes on the implementation worktree, including the new resource-lifecycle and expiring-cache policy tests.
- The focused lifecycle/cache/policy/offline contract set passes separately.
- The full AI quality gate passes, including Android release lint and an R8-minified unsigned F-Droid release build with JBR 21.
- The normal signed release task remains unavailable locally because `YOUTUBE_INNERTUBE_API_KEY` is not configured; no credential was invented or exposed.
- No device or emulator was connected during implementation. Existing APKs predate version 2.3.22 and are not accepted as runtime evidence.
- PR #431 historical measurements inform the risk model but are not treated as a baseline for this branch.

## Preserved behavior and explicit boundaries

- Primary audible playback and the MediaSession remain owned by `PlaybackService`; transition playback is still temporary and queue-identity checked.
- Resolver fallbacks remain available after the new audio-only-first attempt. The disk Media3 LRU cache is unchanged.
- Canvas already has a dedicated muted UI player with explicit listener/surface disposal, so this phase does not duplicate or move that owner.
- Memory recovery retains the existing ExoPlayer and queue engine rather than introducing a second restore path.
- Android and Desktop versions, packages, artifacts, signing, tags and releases remain unchanged.

## Rollback boundary

Revert the playback service lifecycle changes, compatibility-policy revision, bounded-cache helper/integration, related tests and architecture/task documentation. Persisted stream-cache entries remain disposable URL cache data; no user-owned library, queue or settings migration is involved.

## Update rule

Record CI, review, physical-device, merge and release status only from direct evidence. Replace this phase when a new reviewable task begins instead of accumulating unrelated work.
