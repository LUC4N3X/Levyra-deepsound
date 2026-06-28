<p align="center">
  <img src="https://i.ibb.co/WWFLNYPh/levyra-icon-transparent-1.png" alt="Levyra Logo" width="120" height="120" />
</p>

<h1 align="center">LEVYRA</h1>

<p align="center">
  <strong>A premium, high-performance music streaming player for Android.</strong>
</p>

<p align="center">
  <a href="https://kotlinlang.org/"><img src="https://img.shields.io/badge/Kotlin-1.9.22-purple.svg?style=flat-square&logo=kotlin" alt="Kotlin" /></a>
  <a href="https://developer.android.com/jetpack/compose"><img src="https://img.shields.io/badge/UI-Jetpack%20Compose-blue.svg?style=flat-square&logo=jetpackcompose" alt="Jetpack Compose" /></a>
  <a href="https://developer.android.com/guide/topics/media/media3"><img src="https://img.shields.io/badge/Media-AndroidX%20Media3-green.svg?style=flat-square" alt="Media3" /></a>
  <a href="https://github.com/LUC4N3X/levyra-deepsound/blob/main/LICENSE"><img src="https://img.shields.io/badge/License-MIT-yellow.svg?style=flat-square" alt="License" /></a>
</p>

<p align="center">
  Designed for developers and music lovers who appreciate sleek, modern aesthetics. LEVYRA combines a <strong>Modern SaaS Landing Page & Developer Tool Dashboard</strong> design system with the clean, intuitive search UX of <strong>YouTube Music</strong>.
</p>

---

## ✨ Key Features

### 🎨 Premium Aesthetics & UI
- **Soft Glassmorphism**: Translucent panels with hairline borders and deep dark backdrops (`#030407`).
- **Dynamic Color Accents**: The background mesh gradient and UI highlights dynamically adapt to the color palette of the current track's album art.
- **SaaS Dashboard Layout**: A floating navigation bar, a Command-K search dock, and glowing progress indicators.

### 🔍 YouTube Music Search Experience
- **YTM-Inspired Search**: Clean search header featuring quick back navigation, microphone voice search, and visualizer indicators.
- **Ricerche Recenti (Recent Searches)**: A horizontal scrolling shelf displaying landscape cards of your recently played tracks.
- **Smart Completions & Suggestions**: A vertical list of trending artists and real-time search completions featuring diagonal autocomplete arrows (`↖`).

### ⚡ Advanced Playback Engine
- **AndroidX Media3 & ExoPlayer**: A robust foreground playback service supporting background play and lock screen media controls.
- **Aggressive Audio Prefetching**: Smart pre-buffering of upcoming tracks in your queue to ensure zero-latency, instant transitions.
- **Time-Synced Lyrics**: Dynamic, auto-scrolling lyrics overlay that perfectly tracks the song's position.
- **SponsorBlock Integration**: Automatically skips sponsored segments, intros, and non-music sections.
- **Skip Silence**: Intelligent audio processing to compress silent pauses in tracks.
- **Smart Sleep Timer**: Pauses your music automatically after a set duration.

---

## 🛠️ Tech Stack

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) (100% Declarative UI)
- **Audio Engine**: [AndroidX Media3](https://developer.android.com/guide/topics/media/media3) + [ExoPlayer](https://developer.android.com/guide/topics/media/exoplayer)
- **Concurrency**: Kotlin Coroutines & Reactive [StateFlow](https://kotlinlang.org/api/kotlinx.coroutines/kotlinx-coroutines-core/kotlinx.coroutines.flow/-state-flow/)
- **Image Pipeline**: [Coil](https://github.com/coil-kt/coil) (with custom low-memory RGB_565 caching)
- **Local Persistence**: Encrypted SharedPreferences + JSON Serialization
- **Network**: Retrofit & OkHttp

---

## 📐 Architecture

LEVYRA is built following **Clean Architecture** and **MVVM** principles to ensure modularity, testability, and performance:

```mermaid
graph TD
    A[Compose UI Screens] -->|Observe State| B[LevyraViewModel]
    B -->|User Intent| C[ExoPlayer / LevyraPlayer]
    B -->|Fetch Data| D[YoutubeMusicRepository]
    B -->|Fetch Lyrics| E[LyricsRepository]
    B -->|Persist State| F[LevyraPreferences]
    D -->|Audio Stream| G[PlaybackResolver]
```

- **Presentation Layer**: Declarative Compose components (`LevyraApp`, `HomeScreen`, `SearchScreen`, etc.) observing a single unified state.
- **Domain Layer**: Core business models (`Track`, `Mood`, `LyricLine`) and engines.
- **Data Layer**: Repositories managing remote APIs (YouTube Music, Apple Music Charts, LRCLIB) and local caching.

---

## 🚀 Getting Started

### Prerequisites
- Android Studio Jellyfish (or newer)
- Android SDK 34+
- JDK 17

### Building the Project
1. Clone the repository:
   ```bash
   git clone https://github.com/LUC4N3X/levyra-deepsound.git
   ```
2. Open the project in Android Studio.
3. Sync the Gradle files.
4. Run the app on an emulator or a physical device:
   ```bash
   ./gradlew installDebug
   ```

---

## 🤝 Credits & Acknowledgements

<p align="left">
  <a href="https://github.com/LUC4N3X">
    <img src="https://github.com/LUC4N3X.png" width="80" height="80" style="border-radius:50%; border:2px solid #00F5FF;" alt="LUC4N3X" />
  </a>
</p>

- **Creator & Lead Developer**: [LUC4N3X](https://github.com/LUC4N3X) — Architected the UI redesign, player integration, and caching pipelines.
- **Inspirations & Concept Credits**:
  - Special thanks to [Metrolist](https://github.com/MetrolistGroup/Metrolist) for inspiring the design paradigms, modular list flows, and seamless catalog navigation concepts.

---

## ⚖️ Legal Disclaimer (Esonero Responsabilità)

> [!WARNING]
> **PLEASE READ CAREFULLY**

LEVYRA is an open-source media client designed and developed strictly for **educational, personal, and research purposes**. 

- **No Media Hosting**: This application does not host, store, download, or distribute any copyrighted media files or audio streams. All audio resources are resolved dynamically and streamed directly from public third-party platforms (such as YouTube and YouTube Music) via public APIs.
- **Third-Party Terms**: The developer ([LUC4N3X](https://github.com/LUC4N3X)) is in no way affiliated with, authorized, maintained, sponsored, or endorsed by YouTube, Google LLC, or any of their affiliates or partners. Users are solely responsible for ensuring that their use of this application complies with applicable local laws and the Terms of Service of the respective streaming platforms.
- **Limitation of Liability**: Under no circumstances shall the developer be held liable for any copyright infringement, data usage, account suspensions, or legal disputes arising from the use or misuse of this software. The software is provided **"as is"**, without warranty of any kind, express or implied. Use of this application is entirely at your own risk.
