# Roadmap

Levyra's roadmap outlines our ongoing engineering direction and architectural priorities rather than fixed release dates.

The primary project tracking document is maintained in [docs/project/ROADMAP.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/project/ROADMAP.md).

## 1. Playback critical path

- Fast audio startup and low-latency stream resolution.
- Bounded retries and automatic fallback to alternative audio sources.
- Tight synchronization between player, queue state, MediaSession, notifications, and Android Auto.
- Clean separation between audio-only streams and native video playback.
- Reliable gapless transitions, crossfade, and AutoMix playback.

## 2. Local persistence, offline media, and recovery

- Safe Room database migrations with explicit schema verification.
- Resumable background downloads that recover gracefully from network interruptions.
- Backward-compatible schemas for user settings, backups, queues, and playlists.
- True offline-first access for downloaded media and metadata.
- Verifiable export and import through Levyra Vault.

## 3. Responsive and accessible interface

- Fast app startup with minimal cold-start overhead.
- Smooth list scrolling and fluid navigation transitions.
- Stable Compose state trees that avoid unnecessary recompositions.
- Complete localization, right-to-left layout support, and reduced-motion respect.
- Decorative visuals that never block or degrade playback.

## 4. Remote media resilience

- Intelligent stream resolution with multi-client InnerTube fallback.
- Bounded network timeouts and redirect handling.
- Deterministic stream caching and eviction policies.
- Isolation of third-party provider failures to avoid app-wide stalls.
- Validation of audio streams and artwork before caching or playing.

## 5. Windows Desktop quality

- Proper native resource cleanup and playback stability.
- Support for downloads, deep links, and hardware media keys.
- Streamlined update checks and installation flows.
- Clean MSI installer and portable package generation.
- Complete release independence from the Android client.

## 6. Distribution integrity

- Detailed, reproducible evidence on pull requests.
- Secure, least-privilege CI workflows.
- Clean separation of Android, F-Droid, and Windows build artifacts.
- Deterministic signing, release checksums, and artifact verification.

## Priority order

When engineering requirements conflict, we follow this hierarchy:

1. User data safety, privacy, and integrity
2. Core audio playback reliability
3. Process lifecycle and resource cleanup
4. Offline storage and download reliability
5. Responsive, accessible user interface
6. Build and distribution integrity
7. Decorative visuals and cosmetic polish

!!! note
    Active implementation tasks are tracked in [TASKS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/project/TASKS.md). The roadmap guides engineering priorities but does not promise delivery timelines.
