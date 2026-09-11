# Roadmap

Levyra's roadmap describes engineering direction, not promised release dates.

The detailed source of truth lives in [docs/project/ROADMAP.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/project/ROADMAP.md).

## 1. Playback critical path

Priorities include:

- low-latency valid playback paths;
- bounded retry and fallback;
- synchronized player, queue, MediaSession, notifications and Android Auto;
- correct separation of audio and native-video modes;
- reliable gapless, crossfade and AutoMix behavior.

## 2. Persistence, offline use & recovery

- explicit safe database migrations;
- resumable and recoverable downloads;
- backward-compatible settings, backups, queues and playlists;
- local-first offline content;
- bounded and verifiable backup behavior.

## 3. Responsive & accessible interface

- fast startup;
- smooth scrolling and navigation;
- stable state identity;
- localization, RTL and reduced-motion support;
- optional visuals that never become correctness dependencies.

## 4. Remote-media resilience

- deliberate resolver fallback;
- bounded timeouts and redirects;
- safe caching;
- provider-failure isolation;
- validation before playback, caching or writing.

## 5. Windows Desktop reliability

- playback and native-resource ownership;
- downloads, deep links and media keys;
- update flow;
- installer and portable-package quality;
- strict separation from Android release semantics.

## 6. Distribution integrity

- reproducible evidence in pull requests;
- least-privilege CI;
- separated Android, F-Droid and Desktop artifacts;
- deliberate signing, checksums and release metadata.

## Priority rule

When priorities conflict:

1. safety, privacy and user data;
2. direct playback;
3. lifecycle and resource ownership;
4. offline reliability;
5. responsive and accessible UI;
6. release integrity;
7. optional enrichment and visual polish.

!!! note
    Active work is tracked separately in [TASKS.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/project/TASKS.md). The roadmap itself does not authorize implementation or promise a release date.
