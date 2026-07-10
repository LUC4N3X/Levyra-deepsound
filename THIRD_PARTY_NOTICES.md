# THIRD_PARTY_NOTICES

This file records open-source acknowledgements and legal notices for Levyra.

Levyra is licensed under the GNU General Public License v3.0. Third-party libraries, services, assets, metadata providers, APIs and referenced projects retain their own copyright notices, license terms, trademarks and service terms.

## Project Owner

| Name | Role |
|:---|:---|
| LUC4N3X | Creator and lead maintainer of Levyra |

## Core Open-Source References

| Project | URL | Role | Notice |
|:---|:---|:---|:---|
| MetrolistExtractor | https://github.com/MetrolistGroup/MetrolistExtractor | Primary extractor playback core used by Levyra resolver logic, pinned to commit `3cd3341` | GPL-3.0 license and upstream notices must be preserved |
| Metrolist | https://github.com/MetrolistGroup/Metrolist | Android music client ecosystem reference | GPL-3.0 license notices must be preserved where code is reused |
| NewPipeExtractor | https://github.com/TeamNewPipe/NewPipeExtractor | Upstream extractor ecosystem reference | Original copyright and license notices remain with upstream authors |
| PipePipeExtractor | https://github.com/InfinityLoop1308/PipePipeExtractor | Upstream base of MetrolistExtractor | Original copyright and license notices remain with upstream authors |
| MusicApp-KMP | https://github.com/SEAbdulbasit/MusicApp-KMP | UI and modular styling inspiration only | No ownership claim is made over the original project |

## Runtime and Build Dependencies

| Ecosystem | Role |
|:---|:---|
| Kotlin | Primary programming language |
| Jetpack Compose | Native UI framework |
| AndroidX Media3 / ExoPlayer | Playback engine and media session layer |
| AndroidX Room | Local database |
| AndroidX DataStore | Local preference storage |
| AndroidX WorkManager | Background jobs and offline export pipeline |
| OkHttp | Network transport |
| Coil | Image loading |
| kotlinx.serialization | JSON serialization |
| Gradle / Android Gradle Plugin / KSP | Build and code generation pipeline |

Each dependency keeps its own upstream license. Dependency versions and package coordinates are declared in the Gradle version catalog and module build files.

## External Data and Metadata Services

| Service / Ecosystem | Role |
|:---|:---|
| LRCLIB-compatible lyrics metadata | Lyrics lookup where available |
| SponsorBlock-compatible segment metadata | Optional segment metadata where supported |
| Third-party music metadata/search endpoints | Search, metadata and playback resolving where configured by the app |

Levyra does not claim ownership over third-party metadata, album artwork, track names, artist names, lyrics, media content, logos, trademarks, or service names.

## Distribution Requirements

When distributing Levyra or a modified build:

```text
Keep LICENSE
Keep THIRD_PARTY_NOTICES.md
Keep upstream copyright notices
Keep upstream license notices
State visible modifications
Provide complete corresponding source code
Provide build scripts and dependency configuration
Release derivative source under GPL-3.0-compatible terms
```

## Modified-Version Notice

```text
This build is a modified version of Levyra maintained by LUC4N3X.
It includes changes to playback resolution, MetrolistExtractor integration, UI, offline export, caching, artwork handling and release automation.
The complete corresponding source code is available in this repository under the GNU General Public License v3.0.
```

## Trademark and Affiliation Notice

Levyra is independent and is not affiliated with, endorsed by, sponsored by, or officially connected to Google, YouTube, YouTube Music, Apple, Apple Music, Spotify, LRCLIB, SponsorBlock, Metrolist, NewPipe, PipePipe, or any other third-party service or project mentioned in the repository.

All trademarks and service marks belong to their respective owners.

## Content Notice

Levyra does not host, upload, sell, index, or provide copyrighted audio files from its own servers.

Users are responsible for using Levyra only where they have the legal right to access, stream, export, store, or play content, and only in compliance with applicable law and third-party service terms.

Levyra is not intended to bypass DRM, paywalls, authentication walls, geographic restrictions, subscription requirements, private content restrictions, or any other access-control mechanism.
