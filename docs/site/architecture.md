# Architecture

Levyra keeps Android and Windows native where platform behavior matters, while sharing overarching product principles and data schemas.

## High-level model

```text
User
 │
 ▼
UI
 │
 ▼
State / ViewModels
 ├──────────────► Data & resolution ─► Remote/local sources
 │
 └──────────────► Playback runtime ──► Audio/video output
```

## Android

The main Android codebase lives under:

```text
app/src/main/java/com/luc4n3x/levyra/
├── ui/
├── viewmodel/
├── data/
└── player/
```

### UI layer

Jetpack Compose renders application state and forwards user interactions. UI composables do not own persistent state or perform blocking operations.

### State and ViewModels

ViewModels coordinate UI state, repository calls, and player commands using unidirectional data flow. Screens observe focused UI state projections to avoid unnecessary recomposition passes.

### Data and resolution

The data layer handles metadata caching, search queries, lyrics retrieval, stream resolution, and local Room database access.

### Playback pipeline

Android playback is powered by AndroidX Media3 / ExoPlayer, backed by a persistent `PlaybackService` and MediaSession integration. The player, queue state, system notifications, and Android Auto interfaces remain synchronized at all times.

## Windows Desktop

The Windows client lives under:

```text
desktop/
├── app/
├── player/
├── core/
└── packaging/
```

- `app/`: Compose Multiplatform UI and desktop window lifecycle.
- `player/`: Isolated libvlc playback runtime.
- `core/`: Stream resolution, downloads, and desktop application services.
- `packaging/`: Windows MSI and portable distribution packages.

Android and Windows Desktop versions are released independently.

## Playback before decoration

Artwork motion, full-screen Canvas videos, and decorative visual effects are strictly optional:

```text
Track requested
      │
      ▼
Resolve playable media
      │
      ▼
Start audio
      │
      ├── Artwork
      ├── Canvas
      └── Motion visuals
```

A visual rendering failure or network error must never prevent audible playback from starting.

## Local-first data architecture

All user data (such as playlists, favorites, play history, queue state, settings, and listening stats) is stored locally on the device using SQLite. Hot playback and UI paths remain non-blocking and avoid synchronous database transactions.

## Reliability principles

Code changes must preserve:

- Responsive playback and stable queue management
- MediaSession, notification shade, and Android Auto controls
- Downloaded files and offline playback availability
- User playlists, favorites, and listening history
- Settings, configuration, and backup restoration
- Accessibility and right-to-left layout support
- User privacy and on-device data ownership

For deeper technical documentation on extractors and caching, see the repository [docs directory](https://github.com/LUC4N3X/Levyra-deepsound/tree/main/docs).
