# Cast abstraction status

This document records the Cast-related code that exists in Levyra in this change.
It intentionally does not prescribe a future Google Cast implementation.

## Delivered abstraction

The shared `app/src/main/java/com/luc4n3x/levyra/feature/cast/` package contains six plain-Kotlin files:

- `RemotePlaybackModels.kt` — remote device, availability, and playback-state models.
- `RemotePlaybackBackend.kt` — backend-neutral discovery, connection, load, and transport contract.
- `NoOpCastBackend.kt` — unavailable/no-op implementation used by the current app.
- `CastHandoff.kt` — bounded queue-window conversion for local-to-remote and remote-to-local handoff.
- `CastDspSuspension.kt` — suspend/restore policy for local-only DSP without changing stored preferences.
- `CastStreamResolutionPolicy.kt` — bounded stream-resolution policy, URL staleness checks, and the rule that resolved signed URLs are never persisted.

These files do not import Google Cast APIs, Google Play Services, or Media3 `CastPlayer`, and they are JVM-unit-testable.

## Current application state

- Levyra does not currently ship a real Google Cast backend.
- No Google Cast or `media3-cast` dependency is added by this change.
- No Cast button is exposed because the only shipped backend reports `available = false`.
- Normal local playback, MediaSession, Android Auto, downloads, and the existing player remain unchanged.
- The remote-playback abstraction does not persist resolved stream URLs.

## Current F-Droid constraint

The repository currently uses the `levyraFdroidBuild` switch to add either `src/fdroid/java` or `src/upstream/java` to the main Kotlin source set. The existing `PlaybackNetworkStack.kt` implementations demonstrate that current source-set split. This change does not modify that build topology and does not add proprietary Cast dependencies to either path.

Any real Cast implementation, dependency choice, service wiring, or UI entry point is outside the code delivered by this PR and requires a separately scoped change.
