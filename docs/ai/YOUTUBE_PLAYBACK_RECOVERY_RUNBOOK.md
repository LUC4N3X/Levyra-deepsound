# YouTube Playback Recovery Runbook

Use this when Levyra suddenly loses song playback, native video playback, or offline downloads after a YouTube-side change.

## First checks

1. Reproduce with at least one known public track and one native video.
2. Check whether the failure is limited to Wi-Fi, mobile data, or both.
3. Capture the sanitized playback diagnostic and the HTTP/player error. Useful classes are `PlaybackResolver`, `PlaybackResilienceEngine`, `YoutubeLocalDecoder`, `YoutubePlaybackSecurity`, and `OfflineAudioExporter`.
4. Do not start by changing several clients at once. Identify the failing strategy or client, then make the smallest policy/config change that restores service.

The common failure families are:

- `403`, `410`, `429`, `LOGIN_REQUIRED`, or “confirm you're not a bot”: treat as a rejected/risk-controlled stream and refresh the playback policy/security state.
- `signature`, `n-transform`, `PoToken`, or player-JS failures: inspect `player_configs.json`, the local decoder, and the current player configuration before changing client order.
- `404` or `416`: treat the current URL/range as stale and re-resolve it instead of repeatedly retrying the same URL.
- `5xx`, connection reset, or truncated body: first distinguish a transient network/server failure from a consistently broken strategy.

## Songs

The default working path is controlled by `config/playback_policy.json` under `audioStrategy`.

Current strategy names compiled into the app are:

- `REEL_MUXED`
- `REEL_AUDIO`
- `PERSISTED`
- `DIRECT`
- `SEARCH`

The remote policy can reorder or omit only strategies that the installed APK already knows. Unknown future strategy names are ignored; the server cannot inject executable code.

If one strategy consistently fails while another works, move the healthy strategy earlier or remove the broken strategy from the remote list, then increment `revision`.

For song mode, a muxed Reel source is still presented as audio-only: the player must not expose a video stream or change the music UI.

## Native video

Video order is controlled by `videoStrategy`.

Compiled strategy names are:

- `PERSISTED`
- `STANDARD`
- `REEL`

If standard video extraction starts failing but Reel still works, move `REEL` ahead of `STANDARD` and increment the policy revision. Do not change song strategy order just because video is affected.

## Downloads

Offline export intentionally accepts only complete Android Reel-derived sources. Do not re-enable the old anonymous MP4 fallback merely to make resolution succeed: an apparently valid URL that cannot serve the whole file can stall part-way through the download.

On a rejected download source:

1. abort retries against that exact rejected URL;
2. report the failure to the resolver/security recovery path;
3. obtain a fresh source;
4. resume only if the new source identity matches the existing partial file;
5. otherwise discard the partial file and restart safely.

Resume identity includes the source video ID, itag, content length, and MIME type. A newly signed URL for the same representation may resume; a different representation must not be appended to the old `.part` file.

## Remote playback policy

File:

`config/playback_policy.json`

The app fetches it from the Levyra repository and keeps a last-known-good policy. The built-in policy remains the final fallback when the remote file is unavailable, invalid, expired, rolled back, or outside the supported app-version range.

Relevant fields:

- `schema`: policy schema understood by the APK.
- `revision`: monotonically increasing policy revision. Increment this for every behavior change.
- `audioStrategy`: ordered song strategies.
- `videoStrategy`: ordered native-video strategies.
- `androidReelClientVersion`: allowlisted client version string used by Android Reel requests.
- `clients`: allowlisted per-client enable/priority/tier/PoToken/version overrides.
- `clients.<CLIENT>.capabilities`: optional per-capability overrides (`player`, `streaming`,
  `browse`, `metadata`). A capability that is not listed falls back to that client's `enabled`
  flag, so a policy without this block behaves exactly as before. Use it to keep a client usable
  for part of the pipeline while disabling another part, for example `ANDROID_VR` with
  `player: false` but `browse: true`. Unknown capability names are ignored; a non-boolean
  capability value rejects the whole payload. At least one client must keep `player` enabled.
- `expiresAt`: optional absolute epoch milliseconds; `0` means no expiry.
- `minSupportedAppVersion`: optional minimum Android version code; `0` means no lower bound.
- `maxSupportedAppVersion`: optional maximum Android version code; `0` means no upper bound.

Never place credentials, cookies, arbitrary endpoints, arbitrary headers, JavaScript, Kotlin, shell code, DEX, or executable payloads in this file.

### Safe policy update procedure

1. Start from the current `main` policy.
2. Change only the strategy/client setting proven to be broken.
3. Increment `revision`.
4. Keep at least one viable playback path enabled, including at least one client with the `player` and `streaming` capabilities.
5. Commit the policy change to `main` only after JSON/schema review.
6. Re-test song, native video, and a download on a real device.
7. If the change makes things worse, publish a new higher revision restoring the previous known-good values. Do not lower the revision.

## Player configuration / local decoder

If a rejection points to signature, `n-transform`, STS, or player JavaScript:

1. inspect the current Zemer/player config state;
2. on a decode failure, invalidate the stale decoder runtime, force the config refresh, invalidate the player source, then retry with a forced player refresh;
3. on a stream rejection, refresh the config first, clear decode cache only when the config changed, and apply runtime/player-source invalidation only while the rejection still matches the same successful decoder generation;
4. preserve cancellation and do not replace TLS validation or use `trustAll` workarounds.

A stream rejection that arrives after a newer successful decoder generation must not invalidate that newer generation.

## Strategy health and circuit breaker

The server policy defines what is allowed and its preferred order. Local strategy health may reorder only those allowed strategies.

Local health tracks success/failure history, latency, consecutive failures, and temporary quarantine. Repeated resolution failures can open a circuit. A hard runtime rejection such as `403`, `410`, `429`, `LOGIN_REQUIRED`, or a signature/PoToken failure can quarantine the strategy immediately so the next resolve prefers a healthy path.

An open strategy remains available as a last-resort fallback, but a mere URL-resolution success cannot close its circuit before the cooldown. After cooldown it becomes half-open; a successful canary resolution can return it to normal service.

All health data stays local. Do not add user telemetry for this mechanism.

## When a server-side change is enough

A new APK is normally not required when the installed app already contains a healthy strategy/client and the repair only needs:

- strategy reordering or disabling;
- an allowlisted client-version change;
- PoToken requirement/priority changes already supported by the policy;
- a refreshed player/cipher configuration already understood by the installed decoder.

## When a new APK is required

Ship code only when YouTube introduces behavior the installed app cannot express, such as a new protocol, new attestation mechanism, new request/response format, or a required strategy that is not already compiled into the APK.

Do not use remote configuration as remote code execution to avoid shipping an APK.

## Validation before merge/release

At minimum run the repository quality gate, extractor tests, Android unit tests, lint, release compile, and the F-Droid compile path. Then test on a real device:

- several songs start on the first attempt;
- native video still starts and renders;
- a download reaches 100%;
- a failed/rejected download can recover without corrupting the partial file;
- offline/local playback remains unaffected.
