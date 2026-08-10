# Levyra Active Tasks

## Active phase

**Name:** Native video reliability and seamless song/video handoff

**Roadmap tracks:** Track 1 - Playback critical path; Track 4 - Extractor and remote-media resilience

**Status:** Implementation in review; CI and physical-device verification pending

**Scope:** Stabilize native YouTube video playback without changing the working song/audio path. Keep Brano and Video explicit, use a video-specific client/fallback policy, reject stale or incomplete video sources, prefer the most reliable muxed source when available, and preserve live playback intent across Brano <-> Video handoffs.

## Verified current behavior and rationale

- Song/audio playback is working and is not the target of this phase.
- Native-video resolution previously reused the same client ladder and YouTube Music ATV request context as audio playback.
- Split video URLs could be reused while only the audio URL was checked for freshness.
- Video warmup previously wrote a bounded video fragment into the normal playback cache; video warmup is now cacheless and the old video cache namespace is rotated.
- PipePipe's current anonymous YouTube path exposes VisionOS and prefers a dedicated video playback path. Levyra adopts only the compatible direct-player ideas here; it does not copy PipePipe's full player or SABR stack into this hotfix.
- Levyra does not currently provide PipePipe's `SabrDashMediaSource` transport. Non-URL SABR streams therefore remain outside this phase rather than being partially wired into a progressive Media3 path.

## Acceptance criteria

- Brano keeps its current resolver order, YouTube Music audio context, cache behavior, DSP, queue, crossfade, and offline behavior.
- Video uses an independent client order with VisionOS as the preferred anonymous direct-player profile and bounded fallbacks.
- Native-video player requests do not carry the audio-only `MUSIC_VIDEO_TYPE_ATV` context.
- A video result is accepted only when its selected manifest actually contains muxed video, a selected video stream, or HLS.
- A stale separate video URL is never reused merely because its paired audio URL is still fresh.
- A stable compatible muxed stream is preferred over split audio/video when one is available; split playback remains a fallback.
- Video warmup never writes a partial video resource into the normal playback cache.
- Brano -> Video and Video -> Brano keep the current timestamp and current play/pause intent.
- If the user seeks while the alternate mode is resolving, the handoff follows the latest user position rather than the position captured when the button was tapped.
- Recovery preserves the requested recovery point and does not publish stale mode-switch work.
- Official audio/video recording identity remains verified before switching to a counterpart.
- No account cookie, private credential, version change, Desktop change, merge, tag, or release is part of this phase.

## Work items

- [x] Remove partial video writes from playback warmup and rotate the video cache namespace.
- [x] Preserve same-track handoff position without leaking position between different tracks.
- [x] Preserve live play/pause and backward-seek intent while a mode switch resolves.
- [x] Keep the audio resolver ladder unchanged and introduce a video-only client ladder.
- [x] Add VisionOS as the preferred native-video direct-player profile.
- [x] Remove YouTube Music ATV request context from native-video player requests.
- [x] Reject stale split-video URLs and incomplete video manifests.
- [x] Prefer stable muxed video over split audio/video when both are available.
- [x] Keep the reported Bresh - Da Dio official-video identity as a regression fixture.
- [ ] Pass the current PR quality gate, unit tests, lint, and release compilation.
- [ ] Verify Brano playback regression-free on a physical device.
- [ ] Verify Brano -> Video -> Brano at multiple timestamps on a physical device.
- [ ] Verify reported black-screen, few-second stall, and operation-failed cases on a physical device.
- [ ] If direct playback remains insufficient, open a separate scoped phase for a real SABR Media3 transport instead of mixing a partial SABR implementation into this PR.

## Validation evidence

- PR #341 contains focused regression coverage for cache namespace, source selection, recording identity, and handoff position policy.
- GitHub CI for the latest head is the source of truth for repository checks; checks still running are not treated as passed.
- Physical-device playback remains unverified until an APK built from the latest head is tested.

## Preserved behavior

- Song/audio resolution and its existing provider order remain unchanged.
- Audio DSP, queue, crossfade/AutoMix, downloads, favorites, playlists, lyrics, history, settings, localization, backups, Android Auto, and offline playback are outside the change unless required to preserve playback state.
- Android and Desktop versions, packages, signing, tags, and releases remain unchanged.

## Rollback boundary

Revert the native-video resolver policy, selector reliability preference, video cache/warmup changes, handoff policy, focused tests, and this task phase as one Android-only change. No Room migration or user-data rollback is required.

## Update rule

Record CI and physical-device results only from direct evidence. Keep a full SABR transport as a separate reviewable phase if it becomes necessary.
