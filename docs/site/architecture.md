# Architecture

Levyra keeps Android and Windows native where platform behavior matters, while sharing product and engineering principles.

## High-level model

```text
User
 │
 ▼
UI
 │
 ▼
State / ViewModels
 ├──────────────► Data & resolution ─► remote/local sources
 │
 └──────────────► Playback runtime ──► audio/video output
```

## Android

The main Android implementation lives under:

```text
app/src/main/java/com/luc4n3x/levyra/
├── ui/
├── viewmodel/
├── data/
└── player/
```

### UI

Jetpack Compose renders application state and forwards user actions. UI code should not own persistent runtime state or perform expensive blocking work.

### State

ViewModels and state owners coordinate UI, data and runtime systems using predictable ownership and unidirectional flow.

### Data & resolution

The data layer handles responsibilities such as metadata, search, lyrics, stream resolution and persistent data access.

### Playback

Android playback is centered on Media3 / ExoPlayer, PlaybackService and MediaSession integration.

Queue, player, notification, Android Auto and background service behavior must remain synchronized.

## Windows

The Windows implementation lives under:

```text
desktop/
├── app/
├── player/
├── core/
└── packaging/
```

- `app/` — Compose Multiplatform UI and desktop lifecycle.
- `player/` — isolated libvlc playback.
- `core/` — resolution, downloads and desktop application services.
- `packaging/` — Windows distribution packaging.

Android and Desktop versions and releases remain independent.

## Playback before decoration

Canvas, artwork motion and visual enrichment are optional.

```text
Track requested
      │
      ▼
Resolve playable media
      │
      ▼
Start audio
      │
      ├── artwork
      ├── Canvas
      └── motion visuals
```

A visual failure must not become a playback failure.

## Local-first data

Important application data is designed around local ownership, including playlists, favorites, history, queue state, settings and listening insights.

Persistent storage remains the durable source of truth, while hot runtime paths should avoid synchronous disk or database work.

## Reliability principles

Changes should protect:

- playback and queue state;
- MediaSession, notifications and Android Auto;
- downloads and offline media;
- playlists, favorites and history;
- settings and backups;
- localization and accessibility;
- user privacy and data integrity.

For deeper implementation documentation, see the repository's [docs directory](https://github.com/LUC4N3X/Levyra-deepsound/tree/main/docs).
