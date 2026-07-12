<div align="center">

<img src="https://i.ibb.co/mr2N5fv5/Levyra-Git-Hub-Banner-PRO.png" alt="Levyra" width="520" />

<br><br>

# ⌁ &nbsp;L E V Y R A &nbsp;⌁

### Your music. Your files. Your phone. Nobody else's business.

Levyra is a native Android music player built from the ground up in Kotlin — no webview, no wrapper, no tracking.
Stream instantly, download real tagged files to your own storage, and see your listening stats without a single byte leaving your device.

<br>

<p>
  <img alt="Kotlin" src="https://img.shields.io/badge/Kotlin-2.3.20-%237F52FF?style=for-the-badge&logo=kotlin&logoColor=white">
  <img alt="Compose" src="https://img.shields.io/badge/Compose-Material%203-%2306B6D4?style=for-the-badge&logo=jetpackcompose&logoColor=white">
  <img alt="Media3" src="https://img.shields.io/badge/Media3-ExoPlayer-%233DDC84?style=for-the-badge&logo=android&logoColor=white">
  <img alt="Offline" src="https://img.shields.io/badge/Offline-First-%2300E5FF?style=for-the-badge&logo=download&logoColor=black">
  <img alt="Zero Telemetry" src="https://img.shields.io/badge/Telemetry-Zero-%230b0f14?style=for-the-badge&logo=ghostery&logoColor=white">
  <img alt="License" src="https://img.shields.io/badge/GPL--3.0-%23111827?style=for-the-badge">
</p>

<br>

### ⬇️ &nbsp;[Download the latest APK](https://github.com/LUC4N3X/Levyra-deepsound/releases/latest) &nbsp;·&nbsp; [See what's new](https://github.com/LUC4N3X/Levyra-deepsound/releases) &nbsp;·&nbsp; [Report a bug](https://github.com/LUC4N3X/Levyra-deepsound/issues)

<br>

<a href="#-features">Features</a> &nbsp;•&nbsp;
<a href="#-screens">Screens</a> &nbsp;•&nbsp;
<a href="#-architecture">Architecture</a> &nbsp;•&nbsp;
<a href="#-tech-stack">Stack</a> &nbsp;•&nbsp;
<a href="#-getting-started">Build</a> &nbsp;•&nbsp;
<a href="#-privacy">Privacy</a> &nbsp;•&nbsp;
<a href="#-license--legal">License</a>

<br>

<img src="https://i.ibb.co/WNtLhLNz/Levyra-Git-Hub-Banner-Apple.png" alt="Levyra UI preview" width="90%" style="border-radius: 14px; box-shadow: 0px 14px 40px rgba(0,0,0,0.5);" />

</div>

<br>

---

<div align="center">

## Why you'll actually keep it installed

</div>

Most "free music" apps are a webview in a trench coat — slow, ad-riddled, and quietly logging everything you touch. Levyra is the opposite of that.

It's a real Android app. It resolves tracks through YouTube Music's **InnerTube** API, falls back to the **LevyraExtractor** engine the moment stream signatures change, and pushes every second of audio through an optimized **Media3 / ExoPlayer** service that keeps playing with the screen off. When you download a song, you get a genuine `.m4a` file — cover art, title, artist and album baked in — dropped straight into your public `Music/Levyra` folder. A file you own. Not a cache blob you'll never find again.

<table width="100%">
<tr>
<td width="33%" valign="top" align="center">

### 🪶
**Instant & light**

Native Kotlin + Compose. It opens fast, scrolls smooth, and stays out of your way.

</td>
<td width="33%" valign="top" align="center">

### 📥
**Downloads you own**

Real tagged files in your Music folder. No lock-in, no hidden database, no expiry.

</td>
<td width="33%" valign="top" align="center">

### 🔒
**Nothing leaves your phone**

Zero analytics. Zero tracking SDKs. Your stats are computed and stored on-device.

</td>
</tr>
</table>

```text
com.luc4n3x.levyra   ·   Android 8.0 → 15   ·   100% Kotlin   ·   Compose + Material 3   ·   Media3 / ExoPlayer
```

<br>

---

## ✦ Features

<table width="100%">
  <tr>
    <th width="50%" align="left">🎨 &nbsp;An interface that gets out of the way</th>
    <th width="50%" align="left">⚡ &nbsp;A playback engine you can trust</th>
  </tr>
  <tr>
    <td valign="top">
      <ul>
        <li><strong>Built for OLED.</strong> A dark-first, high-contrast theme that looks right in a dark room — and saves your battery while it's at it.</li>
        <li><strong>Fluid by design.</strong> Home, Search, Library and Player tied together with custom micro-animations. Nothing janky, nothing abrupt.</li>
        <li><strong>One player, two moods.</strong> Slide between a tucked-away mini-player and a full-screen, immersive now-playing view.</li>
        <li><strong>Your color, your call.</strong> Optional Material 3 dynamic color that follows your system palette — with an animation toggle if you like things still.</li>
      </ul>
    </td>
    <td valign="top">
      <ul>
        <li><strong>Plays with the screen off.</strong> A Media3 foreground service with real MediaSession controls — audio survives backgrounding and lock.</li>
        <li><strong>Controls that matter.</strong> Loop (all or single), shuffle, a speed tuner, and sleep timers at 15 / 30 / 60 minutes.</li>
        <li><strong>Tune the sound.</strong> In-app normalization, silence skipping, and quality selection (Auto / High / Low).</li>
        <li><strong>SponsorBlock, built in.</strong> Non-music and sponsored segments skipped automatically, in real time.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <th width="50%" align="left">📥 &nbsp;Downloads that don't fall apart</th>
    <th width="50%" align="left">🔍 &nbsp;Search & resolving that keeps up</th>
  </tr>
  <tr>
    <td valign="top">
      <ul>
        <li><strong>Real files, real folder.</strong> Straight to public <code>Music/Levyra</code> — no proprietary cache to reverse-engineer later.</li>
        <li><strong>Tagged in pure Kotlin.</strong> High-res cover art, title, album and artist embedded the moment a download finishes.</li>
        <li><strong>Survives reboots.</strong> A WorkManager queue that rides out network drops and restarts with smart retries.</li>
        <li><strong>No half-files.</strong> Strict Content-Length checks throw out corrupted downloads and reschedule them for you.</li>
      </ul>
    </td>
    <td valign="top">
      <ul>
        <li><strong>Dual-channel resolving.</strong> InnerTube first, LevyraExtractor as backup — smarter Opus/M4A selection and fresh-URL caching that holds up when YouTube shifts signatures.</li>
        <li><strong>Cached, not re-fetched.</strong> A TTL-based stream cache kills duplicate calls and loads tracks faster the second time.</li>
        <li><strong>Search that guesses right.</strong> Predictive suggestions, category filters, instant top-result matching.</li>
        <li><strong>Zero-gap playback.</strong> A prefetch engine loads charts and queued tracks ahead of time so nothing stalls between songs.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <th colspan="2" align="left">📊 &nbsp;Listening Pulse — stats that stay yours</th>
  </tr>
  <tr>
    <td colspan="2">
      <ul>
        <li><strong>Measured on-device.</strong> Real sessions counted second by second, stored in Room. No cloud, no telemetry, ever.</li>
        <li><strong>A dashboard, not a report.</strong> Total minutes, plays, day streak, completion rate, peak hour, and a 7-day rhythm chart — right inside your Library.</li>
        <li><strong>What you actually played.</strong> Top artists ranked by real playtime, plus a true history — not a list of things you searched and skipped.</li>
      </ul>
    </td>
  </tr>
  <tr>
    <th colspan="2" align="left">🎵 &nbsp;Lyrics, synced and offline-friendly</th>
  </tr>
  <tr>
    <td colspan="2">
      <ul>
        <li><strong>LRCLIB lookup.</strong> Synced or static lyrics pulled instantly from track metadata.</li>
        <li><strong>Line-by-line sync.</strong> Smooth scrolling that highlights each line against the live ExoPlayer position.</li>
        <li><strong>Never a dead end.</strong> Falls back gracefully to plain text when timestamped lyrics aren't available.</li>
      </ul>
    </td>
  </tr>
</table>

<br>

---

## ✦ Screens

<div align="center">

<em>A quick look at Levyra in motion. Drop your screenshots in <code>docs/screenshots/</code> and they'll show up right here.</em>

<br><br>

<table>
  <tr>
    <td align="center" width="25%"><img src="docs/screenshots/home.png" width="100%" alt="Home" style="border-radius:10px;" /><br><sub><strong>Home</strong></sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/player.png" width="100%" alt="Player" style="border-radius:10px;" /><br><sub><strong>Full Player</strong></sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/lyrics.png" width="100%" alt="Lyrics" style="border-radius:10px;" /><br><sub><strong>Synced Lyrics</strong></sub></td>
    <td align="center" width="25%"><img src="docs/screenshots/pulse.png" width="100%" alt="Listening Pulse" style="border-radius:10px;" /><br><sub><strong>Listening Pulse</strong></sub></td>
  </tr>
</table>

</div>

<br>

---

## ✦ Architecture

Levyra sticks to a strict unidirectional data flow. Compose does the rendering, a single central ViewModel owns the state, and everything downstream — repositories, services, storage — sits behind clean boundaries so network and database work never touches the main thread.

```mermaid
graph TD
    classDef ui fill:#06B6D4,stroke:#0891B2,stroke-width:2px,color:#001018;
    classDef vm fill:#7F52FF,stroke:#6200EE,stroke-width:2px,color:#fff;
    classDef core fill:#0F1B2D,stroke:#1E3A5F,stroke-width:2px,color:#E2E8F0;
    classDef engine fill:#3DDC84,stroke:#1DDB60,stroke-width:2px,color:#001810;
    classDef ext fill:#F9AB00,stroke:#EA8600,stroke-width:2px,color:#1A1200;

    UI["📱 Jetpack Compose UI"]:::ui --> VM["⚙️ Central LevyraViewModel"]:::vm

    VM --> Player["🔊 LevyraPlayer Controller"]:::core
    VM --> Resolver["🔗 PlaybackResolver"]:::core
    VM --> Repos["📂 Repositories · Music / Lyrics / Charts"]:::core
    VM --> Store["💾 Storage · Room / DataStore"]:::core
    VM --> Work["🔄 WorkManager Download Worker"]:::core

    Player --> Media3["🎵 AndroidX Media3 / ExoPlayer Service"]:::engine

    Resolver --> InnerTube["☁️ YT Music InnerTube API"]:::ext
    Resolver --> Extractor["🔌 LevyraExtractor Engine"]:::ext

    Work --> Exporter["📦 OfflineAudioExporter"]:::core
    Exporter --> MediaStore["💿 Android MediaStore API"]:::engine
    Exporter --> Tagger["🏷️ Pure-Kotlin M4A Tag Writer"]:::core
```

### Where things live

| Layer | What it does | Directory |
|:---|:---|:---|
| **UI** | Composable screens, mini-player, theme engine, layout triggers | `ui/` |
| **State** | Central ViewModel — single source of truth for UI state | `viewmodel/` |
| **Domain** | Entities, data models, validation boundaries | `domain/` |
| **Data & Network** | Endpoints, charts client, lyrics parser, preferences | `data/` |
| **Audio** | Media3 foreground service, HLS, prefetch queue | `player/` |
| **Exports** | WorkManager pipeline, tagging, MediaStore registration | `player/offline/` |
| **Local cache** | Room entities, SQLite, key-value preference stores | `data/local/` |

<br>

---

## ✦ Tech Stack

- **Language** — Kotlin 2.3.20
- **UI** — Jetpack Compose, Material 3, Compose BOM
- **Playback** — AndroidX Media3, ExoPlayer, HLS, MediaSession
- **Network** — OkHttp 5 with Brotli compression
- **Images** — Coil 3, tuned for async Compose loading
- **Persistence** — Room + DataStore Preferences
- **Background** — Android WorkManager
- **Serialization** — kotlinx.serialization (JSON)
- **Build** — Gradle Kotlin DSL, version catalogs (`libs.versions.toml`), KSP
- **Size guard** — Spotify Ruler workflow for bundle-size and dependency tracking
- **Player design** — a Mobius-inspired `Model / Event / Effect / Update` core for safe refactoring
- **Extraction** — InnerTube resolver + GPL-3.0 LevyraExtractor playback core via JitPack

<br>

---

## ✦ Getting Started

**You'll need**

- Android Studio Jellyfish or newer
- JDK 17
- Android SDK Platform 35 / 36
- Gradle 9.4.1 (used in CI via GitHub Actions)

**Build it**

```bash
# Clone
git clone https://github.com/LUC4N3X/Levyra-deepsound.git
cd Levyra-deepsound

# Debug build straight to a connected device
./gradlew installDebug

# Clean, optimized release build
./gradlew clean assembleRelease

# Inspect bundle size with Spotify Ruler
./gradlew :app:analyzeDebugBundle
```

The release APK lands at:

```text
app/build/outputs/apk/release/app-release.apk
```

Deeper notes on architecture and size control:

```text
docs/APK_SIZE_RULER.md
docs/PLAYER_MOBIUS_SAMPLE_ARCHITECTURE.md
```

**Versioning**

Version numbers live in one place — `gradle.properties`:

```properties
levyraVersionName=2.3.6
levyraVersionCode=2030600
```

The version code is derived so two builds can never collide:

```text
versionCode = major·1_000_000 + minor·10_000 + patch·100 + build
```

CI parses this schema, checks the target version with `aapt`, verifies structural integrity, builds the binary, names the artifact `LEVYRA-<version>.apk`, and pushes it straight to **GitHub Releases**.

<br>

---

## ✦ Privacy

Privacy isn't a feature bolted on afterward — it's the default. Levyra ships with **no analytics frameworks, no tracking SDKs, no third-party telemetry.** Nothing about how you listen ever leaves your phone.

```text
Manifest permissions — and why each one exists
├── INTERNET · ACCESS_NETWORK_STATE       Stream audio and fetch metadata
├── FOREGROUND_SERVICE_MEDIA_PLAYBACK     Keep playback alive in the background
├── POST_NOTIFICATIONS                    Show the Media3 media controls
├── WAKE_LOCK                             Prevent stutters when the CPU sleeps
└── WRITE_EXTERNAL_STORAGE (≤ SDK 28)     Legacy path for offline export
```

<br>

---

## ✦ Forking & Contributing

Planning to ship your own build? A few ground rules:

1. **Use your own keys.** Generate and rotate your own Android keystores before publishing anything public.
2. **Name it clearly.** Follow the `LEVYRA-<version>.apk` release schema instead of default Gradle output names.
3. **Keep the main thread clean.** Every disk write, database call, and network resolve runs on `Dispatchers.IO`. No exceptions.
4. **Stay resilient.** If a query times out, route it through the fallback channel — don't let it fail silently.

<br>

---

## ✦ Credits

<table>
  <tr>
    <td width="100" align="center">
      <a href="https://github.com/LUC4N3X">
        <img src="https://github.com/LUC4N3X.png" width="80" style="border-radius: 50%; border: 2px solid #06B6D4;" alt="LUC4N3X" />
      </a>
    </td>
    <td>
      <strong>LUC4N3X</strong> — <em>Creator & Lead Architect</em>
      <p>System architecture, ExoPlayer orchestration, the background WorkManager export queue, the automated release pipeline, and design direction.</p>
    </td>
  </tr>
</table>

*UI structure and modular styling take inspiration from the open-source [Metrolist](https://github.com/MetrolistGroup/Metrolist).*

*The extraction core is [LevyraExtractor](https://github.com/LUC4N3X/LevyraExtractor) — a GPL-3.0 fork of [PipePipeExtractor](https://github.com/InfinityLoop1308/PipePipeExtractor) from the NewPipe / PipePipe ecosystem.*

<br>

---

## ✦ License & Legal

> [!IMPORTANT]
> **Levyra is an independent, community-developed open-source project.** It is not affiliated with, authorized by, endorsed by, sponsored by, or connected to Google, YouTube, YouTube Music, Android, NewPipe, PipePipe, Metrolist, or any other third party. Third-party names, logos, and trademarks appear only for identification, compatibility, and attribution, and remain the property of their respective owners.

### No hosting, no content ownership

Levyra doesn't host media, upload anything to remote servers, sell access to content, or maintain a catalogue of its own. It's a client that runs on your device. When you ask it to, it talks to independent third-party services and works with whatever those services return. Availability, licensing, regional rules, and takedowns all stay under the control of those providers and rights holders. The presence of a search result, link, or stream inside the app does **not** mean the maintainers host, license, or control it.

### Your use, your responsibility

You are responsible for how you install, configure, modify, distribute, and use Levyra — and for making sure your use complies with:

- copyright, IP, privacy, computer-misuse, export, and telecom law;
- the terms of service and technical restrictions of any third-party provider;
- any territorial, contractual, subscription, age, or access requirements on the content you request;
- all permissions needed to download, store, convert, share, or redistribute content.

Levyra grants no license to access or exploit third-party content. Don't use it to infringe copyright, evade payment, bypass DRM, defeat access controls, or violate any law or agreement. Download features are general-purpose tools — their existence is not confirmation that a given item may lawfully be downloaded or kept.

### Third-party services & account risk

Levyra depends on unaffiliated services, APIs, and extraction components that can change, restrict, block, rate-limit, or disappear at any time without notice. The maintainers don't control that infrastructure and can't guarantee stream availability, metadata accuracy, account compatibility, or uninterrupted operation. Accessing those services may transmit the usual technical data (IP, headers, device info, cookies, account identifiers). The maintainers are not responsible for warnings, suspensions, bans, rate limits, regional blocks, expired links, or any enforcement action taken by a third party.

### No warranty

To the maximum extent permitted by law, Levyra and everything related to it — source, binaries, docs, workflows, dependencies, extraction logic, download features — is provided **"AS IS"** and **"AS AVAILABLE,"** without warranty of any kind, express or implied. No promise is made about merchantability, fitness for a purpose, title, non-infringement, reliability, security, accuracy, availability, error-free operation, or data preservation. You assume the full risk of installing, building, signing, modifying, distributing, and using it, and should verify downloaded files, permissions, backups, and the legality of every operation yourself.

### Limitation of liability

To the maximum extent permitted by law, the project owner, maintainers, contributors, copyright holders, upstream projects, and distributors are not liable for any direct, indirect, incidental, special, exemplary, punitive, or consequential damage arising from Levyra, including (without limitation):

- loss, corruption, deletion, or disclosure of data;
- lost revenue, profit, business, opportunity, or reputation;
- device malfunction, battery drain, storage exhaustion, or network charges;
- failed, incomplete, corrupted, or mistagged downloads;
- interruption or modification of third-party services;
- account suspension, rate limiting, or other provider enforcement;
- copyright, trademark, privacy, contractual, or regulatory claims;
- reliance on inaccurate metadata, results, lyrics, or artwork;
- forks, unofficial builds, repackaged APKs, or compromised signing keys produced by others.

These limits apply regardless of legal theory and even if a contributor was warned such damage was possible. Nothing here excludes liability that can't lawfully be excluded — including fraud, wilful misconduct, gross negligence, death or personal injury where applicable, or non-waivable consumer rights.

### Unofficial builds

Only source and releases published through the official Levyra repository are maintained here. The maintainers aren't responsible for forks, mirrors, modified builds, third-party stores, altered extraction logic, removed notices, bundled malware, or leaked signing material. Anyone redistributing a modified build must comply with the GPL-3.0, preserve notices, clearly mark their changes, use their own signing credentials, and must not imply official status.

### Rights-holder requests

No third-party media files are intentionally included in this repository. A rights holder who believes a repository asset infringes their rights may contact the project owner through the issue tracker with: identification of the work or right, the exact repository URL or path, evidence of ownership or authorization, a clear explanation of the alleged infringement, valid contact details, and a good-faith statement of accuracy. The maintainer may review, restrict, replace, or remove repository material where reasonable — and doing so is not an admission of liability or of control over externally hosted content.

### License scope

Levyra is released under the **GNU General Public License v3.0** — see [LICENSE](LICENSE) for the full terms. The GPL-3.0 covers the source code and does **not** grant rights to third-party trademarks, media, metadata, artwork, lyrics, APIs, or externally hosted content, which stay subject to their own licenses.

### Severability

If any part of this notice is found invalid or unenforceable, it will be limited to the minimum extent needed to make it enforceable, and the rest continues to apply. By downloading, building, installing, modifying, distributing, or using Levyra, you acknowledge the technical and legal risks described here and accept responsibility for lawful use, to the extent that acknowledgement is legally effective.

<br>

<div align="center">

**⌁ Built with care by [LUC4N3X](https://github.com/LUC4N3X) ⌁**

If Levyra earns a spot on your phone, a ⭐ on the repo means a lot.

<br>

<sub>GPL-3.0 · Android 8.0+ · No trackers, no ads, no compromises.</sub>

</div>
