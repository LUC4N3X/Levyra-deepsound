<div align="center">

<img src="https://i.ibb.co/mr2N5fv5/Levyra-Git-Hub-Banner-PRO.png" alt="Levyra Banner" width="490" />

# Levyra

**A native music player, stream extractor, and private offline vault for Android & Windows.**

<p align="center">
  <a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="docs/assets/levyra-release.svg" alt="Latest release"></a>
  <a href="https://github.com/LUC4N3X/Levyra-deepsound/releases"><img src="docs/assets/levyra-downloads.svg" alt="Total downloads"></a>
  <a href="LICENSE"><img src="docs/assets/levyra-license.svg" alt="GPL-3.0 License"></a>
  <a href="https://github.com/LUC4N3X/Levyra-deepsound/stargazers"><img src="docs/assets/levyra-stars.svg" alt="GitHub Stars"></a>
</p>

### ✦ Download

<p align="center">
  <a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="docs/assets/levyra-github-download.svg" alt="Download APK" width="365" /></a>&nbsp;&nbsp;
  <a href="https://f-droid.org/packages/com.luc4n3x.levyra/"><img src="docs/assets/levyra-fdroid.svg" alt="Get Levyra on F-Droid" width="365" /></a>
</p>
<p align="center">
  <a href="https://apps.obtainium.imranr.dev/redirect.html?r=obtainium://add/https://github.com/LUC4N3X/Levyra-deepsound"><img src="docs/assets/levyra-obtainium-download.svg" alt="Install via Obtainium" width="365" /></a>&nbsp;&nbsp;
  <a href="https://github.com/LUC4N3X/Levyra-deepsound/releases?q=Levyra+Desktop&expanded=true"><img src="docs/assets/levyra-windows-download.svg" alt="Download Windows Desktop" width="365" /></a>
</p>

<p align="center">
  <code>100% Kotlin</code> &nbsp;·&nbsp; <code>Jetpack Compose</code> &nbsp;·&nbsp; <code>Compose Multiplatform</code> &nbsp;·&nbsp; <code>AndroidX Media3</code> &nbsp;·&nbsp; <code>libvlc</code> &nbsp;·&nbsp; <code>Zero Tracking</code>
</p>

</div>

---

## ✦ The Pure Listening Philosophy

Modern music streaming applications often treat music as temporary, disposable bandwidth—trapped behind proprietary walled gardens, mandatory subscriptions, battery-draining telemetry, and transient cache files that evaporate the moment your device goes offline.

**Levyra is built differently.** It treats music as art to be experienced with absolute acoustic precision and true digital ownership:

* 💿 **True Offline Ownership**: Downloads are standard, high-bitrate M4A audio files saved directly into your device storage (`Music/Levyra`). Album artwork, artists, and lyrics are embedded directly into the file container so your music plays flawlessly in your car, DAP, hi-fi system, or any media player.
* ⚡ **Tuned Low-Latency Audio Engines**: Android leverages a low-overhead **AndroidX Media3 & ExoPlayer** foreground audio pipeline for instant buffering, seamless gapless playback, and volume normalization. Windows runs a standalone **libvlc** engine with global keyboard shortcuts and system tray controls.
* 🎙️ **Live Karaoke Synced Lyrics**: Real-time scrolling lyrics powered by LRCLIB, featuring line-by-line vocal highlighting and interactive tap-to-seek scrubbing.
* ✂️ **Zero-Distraction Flow**: Native **SponsorBlock** automatically detects and skips music video intros, non-musical banter, skit dialogue, and silence—jumping straight into the sound.
* 🛡️ **100% Private On-Device Intelligence**: Your listening time, daily streaks, play counts, and playlists are calculated locally within a private SQLite database. Zero analytics, zero cookies, zero user profiling.

---

## ✦ Interface & Experience Showcase

<div align="center">

<img src="docs/assets/showcase/00_levyra_hero_showcase.webp" alt="Levyra Panoramic Experience Showcase" width="100%" />

<br><br>

| | |
| :---: | :---: |
| <img src="docs/assets/showcase/01_playback_and_lyrics.webp" alt="Immersive Playback & Live Synced Lyrics" width="100%"> | <img src="docs/assets/showcase/02_home_and_charts.webp" alt="Smart Orbit Feed & Global Top Charts" width="100%"> |
| **Immersive Playback & Live Synced Lyrics**<br><sub>ExoPlayer audio engine · Line-by-line LRCLIB synced lyrics · Song/Video toggle · SponsorBlock</sub> | **Smart Orbit Feed & Global Charts**<br><sub>Dynamic 'Your Orbit' rotation · Live Top 50 international charts · Mood chips · Instant prefetch</sub> |
| <img src="docs/assets/showcase/03_search_and_artist.webp" alt="Deep Search & Artist Immersion" width="100%"> | <img src="docs/assets/showcase/04_library_and_pulse.webp" alt="Offline M4A Vault & Listening Pulse" width="100%"> |
| **Deep Search & Artist Immersion**<br><sub>Live search with artist bubbles · Wikipedia biography cards · Complete chronological discography</sub> | **Offline M4A Vault & Listening Pulse**<br><sub>Real tagged M4A files with art · On-device 7-day listening analytics · 100% private SQLite</sub> |
| <img src="docs/assets/showcase/05_video_and_samples.webp" alt="Samples Clips & Comment Energy" width="100%"> | <img src="docs/assets/showcase/06_collections_and_genres.webp" alt="Editorial Playlists & Genre Matrix" width="100%"> |
| **Samples Clips & Comment Energy**<br><sub>Vertical video teaser clips · Live music video feeds · Real-time 73% comment energy resonance</sub> | **Editorial Playlists & Genre Matrix**<br><sub>4-tile dynamic editorial collections · Expansive mood spectrum (Rap, Lo-Fi, Anime, Electronic)</sub> |

</div>

<br>

<details>
<summary><b>✦ Browse Complete Screen-by-Screen Gallery (15 High-Res Views)</b></summary>
<br>

<div align="center">

#### 🎵 Immersive Playback & Lyrics
| Now Playing Canvas | Synchronized Lyrics |
| :---: | :---: |
| <img src="docs/assets/screenshots/player_nowplaying.webp" width="380" alt="Now Playing Canvas"> | <img src="docs/assets/screenshots/lyrics_synced.webp" width="380" alt="Synchronized Lyrics"> |

#### 🪐 Home & Discovery
| Home Selection & Orbit | Global Top 50 Charts | Editorial Collections |
| :---: | :---: | :---: |
| <img src="docs/assets/screenshots/home_orbit.webp" width="260" alt="Home Orbit"> | <img src="docs/assets/screenshots/home_top50.webp" width="260" alt="Global Top 50"> | <img src="docs/assets/screenshots/home_collections.webp" width="260" alt="Editorial Collections"> |

#### 🔍 Search & Artist Universe
| Search Discovery & Recent | Live Artist Results | Artist Profile & Bio | Artist Discography |
| :---: | :---: | :---: | :---: |
| <img src="docs/assets/screenshots/search_recent.webp" width="200" alt="Search History"> | <img src="docs/assets/screenshots/search_artist_avatars.webp" width="200" alt="Artist Search"> | <img src="docs/assets/screenshots/artist_bio.webp" width="200" alt="Artist Bio"> | <img src="docs/assets/screenshots/artist_discography.webp" width="200" alt="Discography"> |

#### 📊 Offline Vault & Listening Pulse
| Library Quick Picks | Listening Pulse Analytics | Playlist Details |
| :---: | :---: | :---: |
| <img src="docs/assets/screenshots/library_quickpicks.webp" width="260" alt="Library Quick Picks"> | <img src="docs/assets/screenshots/library_pulse.webp" width="260" alt="Listening Pulse"> | <img src="docs/assets/screenshots/playlist_recent.webp" width="260" alt="Playlist Details"> |

#### 🎬 Explore & Video Engine
| Vertical Samples Clips | Moods & Subgenre Matrix | Video Feed & Energy |
| :---: | :---: | :---: |
| <img src="docs/assets/screenshots/explore_samples.webp" width="260" alt="Samples Clips"> | <img src="docs/assets/screenshots/explore_genres.webp" width="260" alt="Genres Matrix"> | <img src="docs/assets/screenshots/video_energy.webp" width="260" alt="Video Feed"> |

</div>

</details>

---

## ✦ Acoustic Architecture & Features

<table>
<tr>
<td width="50%" valign="top">

### 🎚️ Pure Audio & Playback Engine

* **Dual Native Engines**: AndroidX Media3 foreground audio service on Android; isolated libvlc pipeline on Windows.
* **Acoustic Normalization**: Intelligent volume leveling smooths loudness jumps between studio tracks and live recordings.
* **Smart Silence & Sponsor Skipping**: Automatic detection of video intros, banter, sponsors, and outro silence.
* **Playback Controls**: True gapless playback, dynamic queue shuffle, single/queue repeat, custom pitch/speed, and sleep timers (15, 30, 60 min).
* **Audio / Video Seamless Toggle**: Switch between high-efficiency Opus/AAC audio streams and full HD video playback with one tap.

</td>
<td width="50%" valign="top">

### 💾 Offline Audio Vault & File Exports

* **Standard Tagged M4A Files**: Exported audio files include embedded high-res cover art, artist, album, and lyrics in standard ID3/MP4 metadata.
* **Permanent Local Storage**: Files are saved into standard Android `Music/Levyra` directory, accessible by all apps and external DACs.
* **Local-First Routing**: If a track exists offline, Levyra plays the local copy instantly with 0ms network latency.
* **Resilient Download Manager**: Chunked downloading with HTTP Range headers, automatic resume, and atomic verification.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 🔍 Stream Extraction & Discovery

* **High-Fidelity Stream Pipeline**: Dual resolver selecting highest available Opus (160 kbps) or AAC (128/256 kbps) streams.
* **Proactive Queue Prefetch**: Next songs pre-buffer silently in the background for zero-latency track skipping.
* **Global Top Charts**: Real-time country-specific Top 50 charts (Italia, USA, UK, España, Global).
* **Smart Orbit Rotation**: Adaptive recommendations that rotate with your listening mood and time of day.

</td>
<td width="50%" valign="top">

### 📊 Karaoke Lyrics & Listening Pulse

* **Real-Time Synced Lyrics**: Millisecond-accurate scrolling lyrics powered by LRCLIB with line-by-line glow.
* **Interactive Lyric Scrubbing**: Tap any lyric line to jump the audio track directly to that timestamp.
* **Listening Pulse Dashboard**: 100% private on-device statistics—minutes played, top artists, completion rates, and peak hours.
* **Real-Play Scoring**: Artists and tracks are ranked purely by actual playback duration, filtering out accidental skips.

</td>
</tr>
</table>

---

## ✦ Architecture & Engineering Blueprint

Levyra is built from the ground up as a native, modular audio suite for Android and Windows.

<table>
<tr>
<td width="50%" valign="top">

### 📱 Android Native Suite (`app/`)
* 🎨 **[`ui/`](app/src/main/java/com/luc4n3x/levyra/ui)** — Fluid Jetpack Compose & Material 3 screens, gesture-driven canvas, and dynamic OLED palettes.
* 🧠 **[`viewmodel/`](app/src/main/java/com/luc4n3x/levyra/viewmodel)** — Central state machine coordinating unidirectional immutable UI state.
* 🎧 **[`player/`](app/src/main/java/com/luc4n3x/levyra/player)** — Low-latency AndroidX Media3 / ExoPlayer foreground audio service.
* 💾 **[`player/offline/`](app/src/main/java/com/luc4n3x/levyra/player/offline)** — WorkManager background export engine with atomic M4A atom tagging & artwork.
* ⚡ **[`data/`](app/src/main/java/com/luc4n3x/levyra/data)** — Dual stream resolver, LRCLIB synchronized lyrics client, and smart prefetch queues.
* 🔒 **[`data/local/`](app/src/main/java/com/luc4n3x/levyra/data/local)** — Zero-telemetry Room SQLite database for playback metrics and favorites.

</td>
<td width="50%" valign="top">

### 💻 Windows Desktop Suite (`desktop/`)
* 🖥️ **[`desktop/app/`](desktop/app)** — Compose Multiplatform desktop UI with window management & auto-updater.
* 🔊 **[`desktop/player/`](desktop/player)** — libvlc audio output engine with hardware acceleration, system tray, and global hotkeys.
* 🌐 **[`desktop/core/`](desktop/core)** — Shared InnerTube stream resolver, download manager, and `%APPDATA%\Levyra` storage.
* 📦 **[`desktop/packaging/`](desktop/packaging)** — WiX Toolset MSI installer and standalone portable distributions.

</td>
</tr>
</table>

### ✦ Under the Hood
* **Unidirectional State Flow**: User actions update immutable state in the ViewModel, dispatching synchronized changes across the Compose UI, MediaSession notification, and Android Auto.
* **Dual Stream Resolver**: Queries InnerTube and LevyraExtractor with automatic fallback, selecting the highest-fidelity Opus or AAC stream while prefetching upcoming queue tracks.
* **Standard File Vault**: Offline tracks save as tagged M4A files directly into `Music/Levyra`, playable in external DAPs, car stereos, and third-party media players.

---

## ✦ Building from Source

<table>
<tr>
<td width="50%" valign="top">

### 📱 Android Build
**Prerequisites**: JDK 17, Android SDK 37, Gradle 9.7.0.

```bash
# Clone repository
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound

# Install debug APK to connected device
./gradlew installDebug

# Assemble release APK
./gradlew clean assembleRelease
```
<sub>Output: `app/build/outputs/apk/release/app-release.apk`</sub>

</td>
<td width="50%" valign="top">

### 💻 Windows Desktop Build
**Prerequisites**: JDK 21 LTS, VLC 3.0.x / libvlc, WiX Toolset 3.14.

```powershell
cd desktop
.\gradlew.bat check
.\gradlew.bat createReleaseDistributable
.\gradlew.bat packageReleaseMsi packageReleaseExe
```
<sub>Output: `desktop/app/build/compose/binaries/main-release/`</sub>

</td>
</tr>
</table>

<details>
<summary><b>✦ F-Droid Reproducible Build & Versioning Contract</b></summary>
<br>

**F-Droid Reproducible Verification:**
```bash
./gradlew --no-daemon -PlevyraFdroidBuild=true :app:assembleRelease
```

**Version Wiring Contract:**
```properties
# Android: gradle.properties
levyraVersionName=2.3.20
levyraVersionCode=2032000

# Windows: desktop/version.properties
levyraDesktopVersion=1.2.0
```
<sub>Version code formula: <code>major * 1_000_000 + minor * 10_000 + patch * 100 + build</code>.</sub>

</details>

---

## ✦ Privacy Blueprint & Permissions

Levyra is built on strict zero-tracking principles: no advertising identifiers, no user profiling, and no background telemetry.

<table>
<tr>
<td width="50%" valign="top">

### 🌐 Network Transparency
* **Stream Endpoints**: Direct connections to YouTube / YT Music CDN servers without proxy tracking.
* **Synchronized Lyrics**: Secure queries to LRCLIB and YouTube Music with title and artist.
* **Artwork & Metadata**: Public catalog queries across Deezer, Apple Music, and Tidal.
* **SponsorBlock**: Optional query with truncated video ID hash prefixes.
* **Update Verification**: Startup release checks on GitHub (disabled in F-Droid).

</td>
<td width="50%" valign="top">

### 🛡️ Android Permissions
* `INTERNET` & `ACCESS_NETWORK_STATE`: Stream audio and resolve album metadata.
* `FOREGROUND_SERVICE_MEDIA_PLAYBACK`: Continuous background playback with notification controls.
* `POST_NOTIFICATIONS`: Playback controls on lockscreen and download updates.
* `WAKE_LOCK`: Prevents CPU sleep while playing audio with the screen off.
* `WRITE_EXTERNAL_STORAGE` (Android ≤ 9): Exporting tagged M4A files to `Music/Levyra`.

</td>
</tr>
</table>

---

## ✦ Contributing

Contributions, optimizations, and translations are warmly welcomed:

<table>
<tr>
<td width="25%" align="center">
<strong>🌿 1. Branch</strong><br>
<sub><code>git checkout -b feature/audio-tuner</code></sub>
</td>
<td width="25%" align="center">
<strong>⚡ 2. Develop</strong><br>
<sub>Clean code & unidirectional data flow</sub>
</td>
<td width="25%" align="center">
<strong>🧪 3. Quality Gate</strong><br>
<sub><code>python scripts/ai_quality_gate.py --profile fast</code></sub>
</td>
<td width="25%" align="center">
<strong>🚀 4. Pull Request</strong><br>
<sub>Open PR with test verification</sub>
</td>
</tr>
</table>

---

## ✦ Author & Acknowledgments

<table>
<tr>
<td width="120" align="center">
  <a href="https://github.com/LUC4N3X">
    <img src="https://images.weserv.nl/?url=github.com/LUC4N3X.png&h=160&w=160&fit=cover&mask=circle" width="80" alt="LUC4N3X" />
  </a>
</td>
<td>
  <strong>LUC4N3X</strong> — <em>Creator & Lead Developer</em><br>
  <sub>Audio engine architecture, stream extraction pipeline, Jetpack Compose UI, and offline vault.</sub><br><br>
  <a href="https://github.com/LUC4N3X"><img src="https://img.shields.io/badge/GitHub-LUC4N3X-7F52FF?style=flat-square&logo=github&logoColor=white&labelColor=0d1117" alt="LUC4N3X on GitHub"></a>
</td>
</tr>
</table>

### Open Source Foundations
* 💎 [**Metrolist**](https://github.com/MetrolistGroup/Metrolist) — Design inspiration for Compose UI architecture and theming.
* ⚡ [**LevyraExtractor**](https://github.com/LUC4N3X/Levyra-deepsound/tree/main/third_party/LevyraExtractor) — Custom stream extraction engine maintained for Levyra.
* 🧬 [**PipePipeExtractor**](https://github.com/InfinityLoop1308/PipePipeExtractor) — Extractor foundation from the NewPipe and PipePipe open-source communities.
* 🎙️ [**LRCLIB**](https://lrclib.net/) — Open synchronized lyric database powering real-time karaoke sync.

---

## ✦ License & Legal Notice

> [!NOTE]
> **Educational & Personal Use**
> Levyra is an open-source client application. It does not host, store, or distribute copyrighted media files. All streams and metadata are retrieved directly from public third-party endpoints.

This project is licensed under the **GNU General Public License v3.0 (GPL-3.0)**. See the [LICENSE](LICENSE) file for details.

This project is not affiliated with, sponsored by, or endorsed by Google LLC, YouTube, or Alphabet Inc.

<div align="center">
  <sub>Crafted for sovereign sound. If you enjoy Levyra, consider starring the repository on GitHub. ⭐</sub>
</div>
