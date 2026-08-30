<div align="center">

<img src="docs/assets/levyra-github-banner.webp" alt="Levyra — Advanced Music Application" width="100%" />

# Hear every layer. No limits.

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
  <a href="https://apt.izzysoft.de/fdroid/index/apk/com.luc4n3x.levyra?repo=main"><img src="docs/assets/levyra-izzyondroid.svg" alt="Get Levyra on IzzyOnDroid" width="365" /></a>
</p>
<p align="center">
  <a href="https://appteka.store/apps/b5br313609"><img src="docs/assets/levyra-appteka.svg" alt="Get Levyra on Appteka" width="365" /></a>&nbsp;&nbsp;
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

<div align="center">
  <h3>🎧 <b>Everything in the signal path. Nothing in the way.</b></h3>
  <p><sub>Playback, discovery, lyrics, offline ownership, recognition, local sync, privacy, and resilient networking — engineered as one native audio system.</sub></p>
  <p>
    <code>10 CORE SYSTEMS</code> &nbsp;·&nbsp;
    <code>ANDROID + WINDOWS</code> &nbsp;·&nbsp;
    <code>LOCAL-FIRST</code> &nbsp;·&nbsp;
    <code>ZERO TRACKING</code>
  </p>
</div>

<br>

<table align="center" width="100%">
  <tr valign="top">
    <td width="50%">
      <h3>🎚️ <b>Playback Core</b></h3>
      <p><b>Fast, native, interruption-free.</b><br><sub>The listening path is tuned around low overhead, predictable behavior, and direct playback control on both platforms.</sub></p>
      <ul>
        <li><b>Native Engines:</b> Media3 / ExoPlayer on Android and isolated libvlc on Windows.</li>
        <li><b>Gapless & Queue:</b> Seamless track transitions, shuffle/repeat, and queue Undo.</li>
        <li><b>Tempo & Timing:</b> Speed/pitch controls plus a built-in sleep timer.</li>
        <li><b>Android Auto:</b> Playback integrates with the car-focused Android media experience.</li>
        <li><b>Volume Normalization:</b> Keeps perceived loudness more consistent between tracks.</li>
        <li><b>SponsorBlock:</b> Automatically skips supported non-musical segments during playback.</li>
        <li><b>Audio / Video Mode:</b> Switch between listening and native-video playback when available.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>💾 <b>Offline Vault</b></h3>
      <p><b>Your library stays yours.</b><br><sub>Portable audio files and versioned local backups without a Levyra account.</sub></p>
      <ul>
        <li><b>Tagged M4A Files:</b> Cover art, artist, album, and lyrics embedded directly in the file.</li>
        <li><b>Local-First Playback:</b> Existing offline files play instantly from <code>Music/Levyra</code>.</li>
        <li><b>Levyra Vault:</b> Versioned <code>.levyra</code> backups protect settings, favorites, playlists, followed artists, history, and queue without an online account.</li>
        <li><b>Verified Restore:</b> Manifest validation, SHA-256 checksums, compatibility preview, required-section checks, and rollback protection before local data is replaced.</li>
        <li><b>Automatic Protection:</b> Manual, scheduled, and pre-update backups with 3/5/10 retention and optional Android SAF destinations; internal storage remains the safe fallback.</li>
      </ul>
    </td>
  </tr>
  <tr valign="top">
    <td width="50%">
      <h3>🔍 <b>Discovery & Extraction</b></h3>
      <p><b>Find it, resolve it, start it.</b><br><sub>Discovery and stream resolution are designed as one continuous path into playback.</sub></p>
      <ul>
        <li><b>Dual Resolver:</b> Highest-fidelity Opus or AAC with automatic fallback.</li>
        <li><b>Queue Prefetch:</b> Upcoming tracks buffer quietly before the skip.</li>
        <li><b>Discovery Engine:</b> Global Top 50, Smart Orbit recommendations, and Samples feed.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>🎙️ <b>Lyrics & Visual Immersion</b></h3>
      <p><b>Lyrics that move with the music.</b><br><sub>Synced text, shareable moments, artwork motion, and Canvas live inside the player.</sub></p>
      <ul>
        <li><b>Synced Lyrics:</b> Millisecond-aligned LRCLIB lyrics with tap-to-seek highlighting.</li>
        <li><b>Lyric Sharing:</b> Select verses, copy, share, or export a 1080×1080 Levyra card.</li>
        <li><b>Living Artwork:</b> Artwork-driven motion fallback when a real Canvas is unavailable.</li>
        <li><b>Canvas & Preview:</b> In-player Canvas control plus full-screen zoomable artwork preview.</li>
      </ul>
    </td>
  </tr>
  <tr valign="top">
    <td width="50%">
      <h3>📈 <b>Listening Intelligence</b></h3>
      <p><b>Useful insight, computed locally.</b><br><sub>Your listening patterns become readable without turning into telemetry.</sub></p>
      <ul>
        <li><b>Accurate Listening:</b> Playback time and counted plays respect pause, seek, skip, and completion.</li>
        <li><b>Listening Pulse:</b> Weekly activity, 24-hour rhythm, and artist distribution.</li>
        <li><b>Your Sound:</b> 7-day, 30-day, 6-month, and All Time analysis stored on device.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>🔤 <b>Typography & Readability</b></h3>
      <p><b>Dense when useful. Clear everywhere.</b><br><sub>Type rhythm and layout rules keep metadata readable across languages and font scales.</sub></p>
      <ul>
        <li><b>Levyra Type Rhythm:</b> Shared line-height scale for titles, metadata, and multi-line text.</li>
        <li><b>Accessible Layout:</b> Role-aware tracking with safe accents, descenders, and large font scales.</li>
        <li><b>Private by Default:</b> Listening statistics are computed locally and never leave the device.</li>
      </ul>
    </td>
  </tr>
  <tr valign="top">
    <td width="50%">
      <h3>🎛️ <b>Music Recognition 2.0</b></h3>
      <p><b>Identify what is playing without an account.</b><br><sub>Recognition works from the microphone or supported internal playback capture.</sub></p>
      <ul>
        <li><b>Shazam-Compatible:</b> Credential-free acoustic fingerprint matching directly on device.</li>
        <li><b>Dual Audio Capture:</b> Listens via microphone or internal device playback (Android 10+ MediaProjection).</li>
        <li><b>AudD Fallback & History:</b> Optional secondary provider with Keystore token, plus local recognition history.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>📻 <b>Levyra Jam</b></h3>
      <p><b>Shared listening, kept on the local network.</b><br><sub>Sync playback and queues across nearby devices without turning the session into a cloud service.</sub></p>
      <ul>
        <li><b>Local Network Listening:</b> Synchronized playback and shared queues across devices on the same Wi-Fi.</li>
        <li><b>Host Authority & Permissions:</b> Granular controls for host-only, guest queue additions, or collaborative sessions.</li>
        <li><b>Mutual Auth Security:</b> Cryptographic challenge-response handshake over HMAC-SHA256 with deep link invites.</li>
      </ul>
    </td>
  </tr>
  <tr valign="top">
    <td width="50%">
      <h3>🛡️ <b>Network Resilience</b></h3>
      <p><b>Flexible routing without hidden behavior.</b><br><sub>DNS, proxy controls, bypasses, and diagnostics stay visible and user-controlled.</sub></p>
      <ul>
        <li><b>DNS-over-HTTPS:</b> Built-in presets for Cloudflare, Google, AdGuard, and Quad9, plus custom HTTPS resolvers.</li>
        <li><b>HTTP & SOCKS Proxies:</b> Authenticated proxy routing with Keystore-encrypted credentials and anti-loop safety.</li>
        <li><b>Stream Bypass & Diagnostics:</b> Option to bypass proxies for direct audio streams, and an in-app network probe.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>🔗 <b>Integrations & Control</b></h3>
      <p><b>Useful connections, never mandatory ones.</b><br><sub>External services remain optional while core playback stays independent.</sub></p>
      <ul>
        <li><b>Scrobbling:</b> Last.fm and ListenBrainz integration with bounded deduplication.</li>
        <li><b>Searchable Settings:</b> Instant search across settings, titles, and keywords in 26 languages.</li>
        <li><b>Video Subtitles:</b> Selectable multi-language subtitles during native-video playback.</li>
      </ul>
    </td>
  </tr>
</table>

<div align="center">
  <sub>One listening stack — built to stay fast, portable, private, and recoverable online or offline.</sub>
</div>

---

## ✦ Architecture & Engineering Blueprint

Levyra keeps Android and Windows native where platform behavior matters, while following the same core architectural principles across both clients: explicit state ownership, resilient stream resolution, and local-first data.

<table align="center" width="100%">
  <tr valign="top">
    <td width="50%">
      <h3><b>Android</b> <code>app/</code></h3>
      <p><sub>Jetpack Compose interface with Media3 / ExoPlayer playback and Room-backed local state.</sub></p>
      <ul>
        <li><b><a href="app/src/main/java/com/luc4n3x/levyra/ui"><code>ui/</code></a></b> — Compose screens, gestures, Canvas, and visual presentation.</li>
        <li><b><a href="app/src/main/java/com/luc4n3x/levyra/viewmodel"><code>viewmodel/</code></a></b> — Immutable UI state and unidirectional coordination.</li>
        <li><b><a href="app/src/main/java/com/luc4n3x/levyra/player"><code>player/</code></a></b> — Foreground playback service, MediaSession, and ExoPlayer ownership.</li>
        <li><b><a href="app/src/main/java/com/luc4n3x/levyra/data"><code>data/</code></a></b> — Stream resolution, lyrics, prefetch, and persistent local data.</li>
        <li><b><a href="app/src/main/java/com/luc4n3x/levyra/player/offline"><code>player/offline/</code></a></b> — Offline exports, M4A tagging, and artwork.</li>
      </ul>
    </td>
    <td width="50%">
      <h3><b>Windows</b> <code>desktop/</code></h3>
      <p><sub>Compose Multiplatform interface with an independent libvlc playback core and desktop-native controls.</sub></p>
      <ul>
        <li><b><a href="desktop/app"><code>app/</code></a></b> — Desktop UI, windows, application lifecycle, and updater.</li>
        <li><b><a href="desktop/player"><code>player/</code></a></b> — libvlc playback, hardware acceleration, tray controls, and global hotkeys.</li>
        <li><b><a href="desktop/core"><code>core/</code></a></b> — Stream resolution, downloads, and local application storage.</li>
        <li><b><a href="desktop/packaging"><code>packaging/</code></a></b> — WiX MSI packaging and portable distributions.</li>
      </ul>
    </td>
  </tr>
</table>

### Core engineering principles

**State ownership.** Immutable state keeps the interface, playback session, and platform controls synchronized without spreading mutable playback state across the UI.

**Resolver resilience.** InnerTube and LevyraExtractor coordinate fidelity selection, fallback, and queue prefetch so playback does not depend on a single resolution path.

**Portable data.** Standard tagged M4A files remain usable outside Levyra, while versioned <code>.levyra</code> archives protect local application state without requiring a Levyra account or cloud service.

---

## ✦ Building from Source

### 📱 Android Build
**Prerequisites**: JDK 17, Android SDK Platform 37, Gradle 9.7.0.

```bash
# Clone the repository
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound

# Build and install debug APK to a connected device
./gradlew installDebug

# Compile optimized release APK
./gradlew clean assembleRelease
```
<sub>Output: `app/build/outputs/apk/release/app-release.apk`</sub>

### 💻 Windows Desktop Build
**Prerequisites**: JDK 21 LTS, Windows x64, VLC 3.0.x / libvlc, and WiX Toolset 3.14.

```powershell
cd desktop
.\gradlew.bat check
.\gradlew.bat createReleaseDistributable
.\gradlew.bat packageReleaseMsi packageReleaseExe
```
<sub>Output: `desktop/app/build/compose/binaries/main-release/`</sub>

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
levyraVersionName=2.5.1
levyraVersionCode=2050100

# Windows: desktop/version.properties
levyraDesktopVersion=1.2.0
```
<sub>Version code formula: <code>major * 1_000_000 + minor * 10_000 + patch * 100 + build</code>.</sub>

</details>

---

## ✦ Privacy Blueprint & Permissions

<div align="center">
  <h3>🛡️ <b>Private by default. Transparent by design.</b></h3>
  <p>
    <code>NO ADS</code> &nbsp;·&nbsp;
    <code>NO ANALYTICS</code> &nbsp;·&nbsp;
    <code>LOCAL LISTENING DATA</code> &nbsp;·&nbsp;
    <code>DIRECT CONNECTIONS</code>
  </p>
</div>

<table align="center" width="100%">
  <tr valign="top">
    <td width="50%">
      <h3>🌐 <b>Network Transparency</b></h3>
      <p><b>Only what playback needs.</b><br><sub>Connections go directly to the services Levyra uses—never through a Levyra tracking proxy.</sub></p>
      <ul>
        <li><b>🎵 Streams:</b> Direct YouTube / YT Music CDN connections.</li>
        <li><b>📝 Lyrics:</b> LRCLIB and YouTube Music metadata queries.</li>
        <li><b>🖼️ Artwork:</b> Public Deezer, Apple Music, and Tidal catalog lookups.</li>
        <li><b>✂️ SponsorBlock:</b> Optional truncated video-ID hash requests.</li>
        <li><b>🚀 Updates:</b> GitHub release checks; disabled in F-Droid builds.</li>
      </ul>
    </td>
    <td width="50%">
      <h3>🔐 <b>Permission Surface</b></h3>
      <p><b>Access stays narrow.</b><br><sub>Android permissions are limited to playback, notifications, optional recognition, and offline export.</sub></p>
      <ul>
        <li><b><code>INTERNET</code> / <code>ACCESS_<wbr>NETWORK_STATE</code></b><br><sub>Streaming and metadata.</sub></li>
        <li><b><code>FOREGROUND_<wbr>SERVICE_<wbr>MEDIA_<wbr>PLAYBACK</code></b><br><sub>Background playback and media controls.</sub></li>
        <li><b><code>POST_<wbr>NOTIFICATIONS</code></b><br><sub>Lockscreen controls and download updates.</sub></li>
        <li><b><code>RECORD_<wbr>AUDIO</code></b><br><sub>Optional music recognition only.</sub></li>
        <li><b><code>WAKE_<wbr>LOCK</code></b><br><sub>Keeps playback active with the screen off.</sub></li>
        <li><b><code>WRITE_<wbr>EXTERNAL_<wbr>STORAGE</code></b> <sub>Android ≤ 9</sub><br><sub>Tagged M4A export to <code>Music/Levyra</code>.</sub></li>
      </ul>
    </td>
  </tr>
</table>

---

## ✦ Contributing
We welcome community contributions, bug fixes, localization, and performance enhancements.

### Engineering Workflow

1. **Fork & Branch**: Create a focused topic branch from `main`:
   ```bash
   git checkout -b feature/your-feature-name
   ```
2. **Architecture Contract**: Preserve unidirectional data flow (UDF), immutable Compose state, and low-latency audio pipelines.
3. **Quality Gate**: Run repository validation before submitting:
   ```bash
   python scripts/ai_quality_gate.py --profile fast
   ```
4. **Pull Request**: Open a PR with a concise description of changes and test verification evidence.

> [!TIP]
> **Scope & Guidelines**: Keep pull requests focused on a single concern. For major architectural changes or new feature proposals, please open an [Issue](https://github.com/LUC4N3X/Levyra-deepsound/issues) first to coordinate the implementation approach.

---

## ✦ Author & Credits

<table width="100%">
  <tr>
    <td width="125" align="center" valign="middle">
      <a href="https://github.com/LUC4N3X">
        <img src="https://images.weserv.nl/?url=github.com/LUC4N3X.png&h=260&w=260&fit=cover&mask=circle&maxage=7d" width="88" height="88" alt="LUC4N3X" />
      </a>
    </td>
    <td valign="middle">
      <h3 style="margin: 0 0 6px 0;">
        <a href="https://github.com/LUC4N3X">LUC4N3X</a> &nbsp;·&nbsp; <code>Lead Systems Architect & Creator</code>
      </h3>
      <p style="margin: 0 0 10px 0;">
        Creator and lead systems architect of Levyra. Engineering low-latency Android audio pipelines (Media3 & ExoPlayer), standalone libvlc desktop cores, zero-telemetry SQLite vaults, and synchronized LRCLIB karaoke engines.
      </p>
      <div>
        <a href="https://github.com/LUC4N3X"><img src="https://img.shields.io/badge/GitHub-@LUC4N3X-7F52FF?style=flat-square&logo=github&logoColor=white&labelColor=0d1117" alt="GitHub Profile" /></a>&nbsp;
        <img src="https://img.shields.io/badge/Kotlin-Multiplatform-7F52FF?style=flat-square&logo=kotlin&logoColor=white&labelColor=0d1117" alt="Kotlin Multiplatform" />&nbsp;
        <img src="https://img.shields.io/badge/Android-Media3%20%2F%20Compose-38BDF8?style=flat-square&logo=android&logoColor=white&labelColor=0d1117" alt="Android Media3" />&nbsp;
        <img src="https://img.shields.io/badge/Desktop-libvlc%20Core-FF8800?style=flat-square&logo=vlcmediaplayer&logoColor=white&labelColor=0d1117" alt="libvlc Desktop" />&nbsp;
      </div>
    </td>
  </tr>
</table>

<br>

### Built With & Inspired By

* **[Metrolist](https://github.com/MetrolistGroup/Metrolist)** — Reference for Levyra's renderer-recovery backoff policy and BetterLyrics TTML parsing behavior, reimplemented for Levyra.
* **[PipePipeExtractor](https://github.com/InfinityLoop1308/PipePipeExtractor)** — LevyraExtractor is maintained as a Levyra-specific fork of this project, with additional stream-resolution, playback-reliability, diagnostics, and fallback work.
* **[NewPipe & PipePipe Communities](https://github.com/TeamNewPipe/NewPipeExtractor)** — Foundational extractor architecture, service implementations, parser protocols, and downstream ecosystem work retained in LevyraExtractor.
* **[LRCLIB](https://lrclib.net/)** — Community synchronized lyric database and open API powering real-time karaoke synchronization.

---

## ✦ Legal Notice & License

> [!IMPORTANT]
> **Independent open-source software — lawful use only.** Levyra is client-side software and does not operate a centralized media-hosting or content-distribution service. Users are responsible for ensuring that their use of third-party services and content is lawful and authorized.
>
> Levyra is provided **"AS IS"** under the **[GNU GPL v3.0](LICENSE)**, to the maximum extent permitted by applicable law. Third-party trademarks, services, APIs, media, metadata, and copyrighted works remain subject to their respective owners, licences, terms, and applicable law.
>
> **Read the complete [Legal Notice](LEGAL.md)** for no-affiliation terms, third-party rights, user responsibility, warranty and liability limitations, rights-holder notices, and mandatory-law provisions.

<div align="center">
  <sub>Crafted for sovereign sound. If you enjoy Levyra, consider starring the repository on GitHub. ⭐</sub>
  <br><br>
  <img src="https://capsule-render.vercel.app/api?type=waving&height=120&section=footer&color=0:4A00E0,35:6C5CE7,70:7F52FF,100:00D2FF" width="480" alt="Levyra Acoustic Waves" />
</div>