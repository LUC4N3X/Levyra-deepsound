<div align="center">

<img src="https://i.ibb.co/mr2N5fv5/Levyra-Git-Hub-Banner-PRO.png" alt="Levyra Logo" width="490" />

# 🎶

### Stream everything. Keep what you love. Own every note.

<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest"><img src="https://img.shields.io/github/v/release/LUC4N3X/Levyra-deepsound?label=Download&style=for-the-badge&labelColor=0d1117&color=7F52FF" alt="Download latest release"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases"><img src="https://img.shields.io/github/downloads/LUC4N3X/Levyra-deepsound/total?style=for-the-badge&labelColor=0d1117&color=0A84FF" alt="Total downloads"></a>
<a href="LICENSE"><img src="https://img.shields.io/github/license/LUC4N3X/Levyra-deepsound?style=for-the-badge&labelColor=0d1117&color=3DDC84" alt="GPL-3.0 License"></a>
<a href="https://github.com/LUC4N3X/Levyra-deepsound"><img src="https://img.shields.io/github/stars/LUC4N3X/Levyra-deepsound?style=for-the-badge&label=Stars&labelColor=0d1117&color=F9AB00&logo=github&logoColor=white" alt="Star Levyra"></a>

<br>

<a href="https://github.com/LUC4N3X/Levyra-deepsound/releases/latest">
  <img src="docs/assets/levyra-github-download.svg" alt="Download the latest Levyra APK from GitHub Releases" width="400" />
</a>

<sub>**One APK. No Play Store. No account. No ads.** · Official signed build · Android 8.0+</sub>

</div>

---

## ✦ About Levyra

<div align="center">

🎵 **Levyra** isn't another website wearing an app icon. It's a native Android music client,<br>
written from the ground up in 100% Kotlin, for people who are tired of renting their own music library.

It streams instantly, plays flawlessly in the background, and when you hit download it gives you<br>
something almost no app dares to anymore: a **real M4A file**, tagged and covered, sitting in<br>
`Music/Levyra` where it belongs. **Yours. In any player. Forever.**

<sub>*Every screen, every animation, every retry-on-bad-wifi was built by one developer who actually uses this app every day — and it shows.*</sub>

<br>

🛡️ &nbsp;**Privacy by Default** &nbsp;—&nbsp; <sub>Listening stats stay in a local database. No trackers, no telemetry, no analytics.</sub>

📥 &nbsp;**Real Files, Really Yours** &nbsp;—&nbsp; <sub>Tagged M4A exports with embedded artwork, not cache blobs locked inside the app.</sub>

⚡ &nbsp;**Native Audio Engine** &nbsp;—&nbsp; <sub>Media3 / ExoPlayer foreground service. Screen off, pocket, music never stops.</sub>

<br>

`100% Kotlin` &nbsp;·&nbsp; `Jetpack Compose` &nbsp;·&nbsp; `Material 3`

</div>

---

## ✦ Features

<table width="100%">
<tr>
<td width="50%" valign="top">

### 🎨 Expressive Interface

**Dark-First, OLED-True:** Deep blacks and high contrast, built dark from day one — not dimmed as an afterthought.
**Fluid Navigation:** Home, Search, Library and Player tied together by custom micro-animations.
**Dual-State Player:** A discreet mini-player one swipe away from an immersive fullscreen experience.
**Dynamic Material 3:** Optional system-wide dynamic color so the app matches your phone, not the other way around.

</td>
<td width="50%" valign="top">

### ⚡ Rock-Solid Playback

**True Background Service:** Media3 + MediaSession keep playing with the screen off and the app minimized.
**Total Control:** Loop all/single, shuffle, playback speed tuning, sleep timers (15/30/60m).
**Audio Tuning:** In-app normalization, silence skipping, quality selectors (Auto/High/Low).
**SponsorBlock Built In:** Non-music and sponsored segments skipped automatically, in real time.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📥 Downloads Done Right

**Real Media Files:** Exported straight to the public `Music/Levyra` directory. Move them, back them up, keep them.
**Pure-Kotlin Tagging:** High-res covers, titles, albums and artists embedded on completion.
**Unstoppable Pipeline:** WorkManager downloads survive reboots and network drops, then retry on their own.
**Truncation Shield:** Strict Content-Length checks discard corrupted files and re-queue them automatically.

</td>
<td width="50%" valign="top">

### 🔍 Search & Stream Resolving

**Dual-Channel Resolver:** InnerTube + LevyraExtractor with smart Opus/M4A selection — when YouTube changes signatures, Levyra doesn't flinch.
**Intelligent Caching:** TTL-based stream cache cuts duplicate requests and loads tracks before you finish tapping.
**Predictive Search:** Live suggestions, categorized filters, instant top-result matching.
**Prefetching Engine:** Charts and queued songs load ahead of time. Zero-gap playback, every time.

</td>
</tr>
<tr>
<td width="50%" valign="top">

### 📊 Listening Pulse

**On-Device Stats:** Every Pulse metric is stored locally in Room — never uploaded as analytics or telemetry.
**Pulse Dashboard:** Total minutes, plays, day streak, completion rate, peak hour and a 7-day rhythm chart.
**True History:** Top artists ranked by real playtime, plus what you actually played — not what you searched once.

</td>
<td width="50%" valign="top">

### 🎵 Synced Lyrics

**LRCLIB Integration:** Synced and static lyrics fetched instantly from track metadata.
**Live Tracker:** Karaoke-precise scrolling locked to the ExoPlayer position.
**Graceful Fallback:** No timestamps? Clean static text. Never a blank screen.

</td>
</tr>
</table>

---

## ✦ A Look Inside

<div align="center">

<br>

<img src="https://i.ibb.co/WNtLhLNz/Levyra-Git-Hub-Banner-Apple.png" alt="Levyra UI Preview" width="89%" />

<sub>*Home · Top 50 charts · L'Abisso genre zones · Artist pages — dark-first, OLED-true.*</sub>

<br>

</div>

---

## ✦ Architecture

Levyra runs on strict unidirectional data flow: Compose renders, a central ViewModel owns state, and decoupled repositories and services make sure no network call or database write ever touches the main thread.

```text
📦 Application Specifications
├── Package Name      com.luc4n3x.levyra
├── Target SDK        35 (Android 15)
├── Min SDK           26 (Android 8.0)
├── Primary Language  100% Kotlin
├── UI Framework      Jetpack Compose + Material 3 (M3)
└── Audio Foundation  AndroidX Media3 / ExoPlayer Engine
```

```mermaid
graph TD
    %% Custom Styling Theme
    classDef ui fill:#4285F4,stroke:#1A73E8,stroke-width:2px,color:#fff;
    classDef vm fill:#7F52FF,stroke:#6200EE,stroke-width:2px,color:#fff;
    classDef core fill:#202124,stroke:#3C4043,stroke-width:2px,color:#fff;
    classDef engine fill:#3DDC84,stroke:#1DDB60,stroke-width:2px,color:#000;
    classDef ext fill:#F9AB00,stroke:#EA8600,stroke-width:2px,color:#000;

    UI["📱 Jetpack Compose UI"]:::ui --> VM["⚙️ Central LevyraViewModel"]:::vm
    
    VM --> Player["🔊 LevyraPlayer Controller"]:::core
    VM --> Resolver["🔗 PlaybackResolver Link"]:::core
    VM --> Repos["📂 Data Repositories (Music/Lyrics/Charts)"]:::core
    VM --> Store["💾 Storage System (Room / DataStore)"]:::core
    VM --> Work["🔄 WorkManager Download Worker"]:::core
    
    Player --> Media3["🎵 AndroidX Media3 / ExoPlayer Service"]:::engine
    
    Resolver --> InnerTube["☁️ YT Music InnerTube API"]:::ext
    Resolver --> Extractor["🔌 LevyraExtractor Engine"]:::ext
    
    Work --> Exporter["📦 OfflineAudioExporter"]:::core
    Exporter --> MediaStore["💿 Android MediaStore API"]:::engine
    Exporter --> Tagger["🏷️ Pure-Kotlin M4A Tag Writer"]:::core
```

| Layer | Responsibility | Project Directory |
|:---|:---|:---|
| **UI Presentation** | Composable screens, mini-player layouts, layout triggers, theme engines | [`ui/`](app/src/main/java/com/luc4n3x/levyra/ui) |
| **State Management** | Centralized ViewModel orchestrating single-source UI state | [`viewmodel/`](app/src/main/java/com/luc4n3x/levyra/viewmodel) |
| **Domain Logic** | Abstract domain entities, data models, validation boundaries | [`domain/`](app/src/main/java/com/luc4n3x/levyra/domain) |
| **Data & Network** | Web endpoints, charts API client, lyrics parser, preferences config | [`data/`](app/src/main/java/com/luc4n3x/levyra/data) |
| **Audio Pipeline** | Media3 foreground service, HLS, prefetching queue control | [`player/`](app/src/main/java/com/luc4n3x/levyra/player) |
| **Background Exports** | WorkManager pipeline, metadata tagging, MediaStore registrations | [`player/offline/`](app/src/main/java/com/luc4n3x/levyra/player/offline) |
| **Local Cache** | SQLite database, Room entities, and key-value preference stores | [`data/local/`](app/src/main/java/com/luc4n3x/levyra/data/local) |

---

## ✦ Technical Stack

*   **Language:** Kotlin 2.4.0
*   **User Interface:** Jetpack Compose, Material 3 Design Components, Compose BOM
*   **Media Playback:** AndroidX Media3, ExoPlayer, HLS Playback, MediaSession
*   **Network Transport:** OkHttp 5, Brotli compression module
*   **Image Caching:** Coil 3 (Compose-optimized asynchronous image loading)
*   **Data Persistence:** Room Database, DataStore Preferences
*   **Background Jobs:** Android WorkManager Daemon
*   **Serialization:** kotlinx.serialization (JSON)
*   **Build Pipeline:** Gradle Kotlin DSL (`.gradle.kts`), Version Catalogs (`libs.versions.toml`), KSP (Kotlin Symbol Processing)
*   **APK Size Guard:** Spotify Ruler report workflow for bundle size analysis and dependency weight tracking
*   **Player Architecture:** Mobius-sample-inspired `Model / Event / Effect / Update` foundation for safe player refactoring
*   **Extraction Layer:** InnerTube resolver plus GPL-3.0 LevyraExtractor playback core via JitPack

---

## ✦ Building from Source

### Prerequisites
*   Android Studio Jellyfish (or newer)
*   Java Development Kit (JDK) 17
*   Android SDK Platform 37 (`compileSdk = 37`, `targetSdk = 35`)
*   Gradle 9.6.1 through the repository Gradle Wrapper

### Build & Install

```bash
# Clone the repository
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound

# Build and install the debug app on your connected device
./gradlew installDebug

# Compile a clean, optimized release build
./gradlew clean assembleRelease

# Analyze bundle size with Spotify Ruler
./gradlew :app:analyzeDebugBundle
```

The release APK lands in `app/build/outputs/apk/release/app-release.apk`.

Architecture and size-control notes live in `docs/APK_SIZE_RULER.md` and `docs/PLAYER_MOBIUS_SAMPLE_ARCHITECTURE.md`.

### Versioning & CI

Version numbering is centralized in `gradle.properties`:

```properties
levyraVersionName=2.3.11
levyraVersionCode=2031100
```

`versionCode = major * 1_000_000 + minor * 10_000 + patch * 100 + build` — calculated sequentially so no two deployments ever collide. The APK Artifact workflow parses this schema, verifies target versions with `aapt`, checks structural integrity, compiles the signed binary and publishes it as `LEVYRA-<version>.apk`.

---

## ✦ Privacy & Data Collection

Your listening habits are nobody's business — including mine.

- **No analytics frameworks.** No tracking SDKs, no developer-operated telemetry. None.
- **Pulse stays home.** Every listening statistic is computed and stored on your device, in a local Room database.
- **Honest about the rest.** Search, artwork, lyrics, playback, SponsorBlock and optional account features contact third-party services, which may receive ordinary request data — IP address, HTTP headers, client/device info and, where applicable, cookies or account identifiers.

```text
🛡️ DECLARED MANIFEST PERMISSIONS
├── INTERNET & ACCESS_NETWORK_STATE       Streams music data and queries metadata
├── FOREGROUND_SERVICE_MEDIA_PLAYBACK     Ensures audio playback survives app backgrounding
├── POST_NOTIFICATIONS                     Displays the Media3 media controller notification
├── WAKE_LOCK                              Prevents playback stutters when the CPU goes to sleep
└── WRITE_EXTERNAL_STORAGE (≤ SDK 28)     Legacy permission for offline file export
```

---

## ✦ Contributing

Bug reports, feature requests and pull requests are all welcome:

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

**Distributing your own builds?** A few house rules:

- **Signing Keys:** Generate and rotate your own Android keystores before publishing public packages.
- **Build Name:** Follow the release schema `LEVYRA-<version>.apk` instead of default Gradle outputs.
- **Execution Offloading:** All database, disk and network work runs on background dispatchers (`Dispatchers.IO`). Keep UI threads clear.
- **Resiliency:** API queries must route through the fallback channel on timeout.

---

## ✦ Credits

<table align="center">
  <tr>
    <td align="center" width="130">
      <a href="https://github.com/LUC4N3X">
        <img src="https://images.weserv.nl/?url=github.com/LUC4N3X.png&h=192&w=192&fit=cover&mask=circle" width="96" alt="LUC4N3X Avatar" />
      </a>
    </td>
    <td>
      <h3>LUC4N3X</h3>
      <strong>Creator · Lead Architect · Design Lead</strong>
      <br>
      <sub>System architecture · ExoPlayer orchestration · WorkManager export pipeline · Automated release CI · UI/UX</sub>
      <br>
      <sub><em>One developer. No team, no shortcuts — every line, every pixel.</em></sub>
      <br><br>
      <a href="https://github.com/LUC4N3X"><img src="https://img.shields.io/badge/GitHub-LUC4N3X-7F52FF?style=flat-square&logo=github&logoColor=white&labelColor=0d1117" alt="LUC4N3X on GitHub"></a>
    </td>
  </tr>
</table>

<div align="center">

#### Standing on the Shoulders of Open Source

</div>

<table align="center">
  <tr>
    <th align="left">Project</th>
    <th align="left">Contribution</th>
  </tr>
  <tr>
    <td><a href="https://github.com/MetrolistGroup/Metrolist"><strong>Metrolist</strong></a></td>
    <td>Structural inspiration for UI conventions and modular styling</td>
  </tr>
  <tr>
    <td><a href="https://github.com/LUC4N3X/Levyra-deepsound/tree/main/third_party/LevyraExtractor"><strong>LevyraExtractor</strong></a></td>
    <td>Stream extraction core — GPL-3.0 fork maintained for Levyra</td>
  </tr>
  <tr>
    <td><a href="https://github.com/InfinityLoop1308/PipePipeExtractor"><strong>PipePipeExtractor</strong></a></td>
    <td>Upstream extractor foundation from the NewPipe/PipePipe ecosystem</td>
  </tr>
</table>

---

## 📜 Disclaimer

> [!WARNING]
> **Educational and Research Purposes Only**
> Levyra is an open-source client and does not host, upload, or index copyrighted files. The app interacts solely with public, third-party content endpoints. The user takes full responsibility for any usage that may violate local laws or third-party terms of service. The developers assume no liability for service changes, system blocks, or client misuse.

This project is **not affiliated with, funded, authorized, endorsed by, or in any way associated with** YouTube, Google LLC, or any of their affiliates and subsidiaries. All trademarks referenced belong to their respective owners.


---

<div align="center">

**Built independently. Engineered with purpose. Refined for listeners who expect more.**

⭐ **If Levyra has earned a place in your daily listening, support its development by starring the repository.**

</div>

