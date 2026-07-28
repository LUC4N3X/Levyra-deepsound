# Levyra Desktop

Levyra's desktop client for Windows, written in Kotlin with Compose Multiplatform.

It shares the Android project's YouTube extractor (`third_party/LevyraExtractor`), reuses the same localization catalog, and plays audio through libvlc.

## Features

- Music-first Home screen
- Guided first-run setup for language, display name, music preferences, and content country
- Complete Library with playlists, favorites, offline downloads, and listening history
- Persistent audio downloads with progress, cancellation, resume, retry, and deletion
- Automatic local-file playback whenever a track is already available offline
- YouTube Music search for songs, videos, albums, playlists, and artists, with suggestions and pagination
- Quick suggestions, artists, and moods localized according to the selected language and country
- Top 50 charts selectable by country flag and country name
- Synced lyrics from LRCLIB, with the current line highlighted during playback
- Queue with shuffle and repeat, plus automatic radio when the queue ends
- Next-track preloading that resolves the upcoming stream before the current track ends
- Playback speed between 0.75x and 2x, applied to the current track and kept across sessions
- Sleep timer with presets or end-of-track mode, with the remaining time shown in the player
- System-wide media keys on Windows, so playback stays controllable from the tray or the background
- Full desktop player with a close action and direct offline-download control
- Separate resizable mini player that can stay always on top
- Single-instance protection: opening Levyra again brings the existing window to the foreground
- `levyra://` protocol support and direct opening of YouTube and YouTube Music links
- Automatic Windows update checks with SHA-256 verification
- Local crash reports that can be copied directly from the error dialog
- Artwork-derived accent color, light and dark themes, and a coordinated Windows title bar
- Official Levyra icon in the window, tray, sidebar, and installer

## First Launch

When no completed profile exists, Levyra displays the initial setup before opening Home:

1. choose the app language;
2. enter the display name used in the app;
3. select at least three music preferences;
4. choose the country used for content and Top 50 charts.

The profile is stored locally. Name, language, and country can be changed later in Settings, and the onboarding questionnaire can be reopened without deleting the Library, playlists, downloads, or listening history.

## Library

The Library item in the sidebar groups the app's four personal areas:

- local playlists;
- favorite tracks;
- offline downloads;
- listening history.

Each section displays its own count and allows users to play, add to the queue, add to a playlist, or manage content without returning to Home.

## Offline Downloads

The download action is available from every track-row menu and directly from the player. Downloads are stored in `%APPDATA%\Levyra\offline` and tracked in `downloads.json`.

The download engine:

- runs no more than two transfers at the same time;
- persists progress to disk;
- uses temporary `.part` files;
- resumes interrupted transfers through HTTP Range requests;
- finalizes files with an atomic move;
- supports cancellation, retry, and deletion;
- verifies at startup that completed files still exist;
- automatically prefers the local file during playback.

Incomplete downloads remain available for resuming after the app is closed or restarted. Completed files can be played without resolving the online stream again.

## Mini Player and Desktop Lifecycle

The mini player is a separate always-on-top window. It shares the main player's state in real time, remembers its position and size, and supports:

- play and pause;
- previous and next track;
- favorites;
- track progress;
- opening the main window;
- Space, Left Arrow, Right Arrow, and Esc shortcuts.

Levyra keeps only one active instance. A second launch does not initialize the Library, downloads, or player again. Instead, it sends a local request to the already-running instance and brings it to the foreground.

## Deep Links

On Windows, Levyra registers the user-level `levyra://` protocol without requiring administrator privileges.

Examples:

```text
levyra://open?url=https%3A%2F%2Fmusic.youtube.com%2Fplaylist%3Flist%3D...
levyra://search?q=artist
levyra://watch?v=VIDEO_ID
```

Direct YouTube, YouTube Music, and `youtu.be` URLs are also accepted. When Levyra is already running, the link is forwarded to the existing instance.

## Windows Updates

Android and Desktop use completely separate versioning.

The Windows version is changed only in:

```properties
# desktop/version.properties
levyraDesktopVersion=1.0.0
```

The Android `levyraVersionName` value in the root `gradle.properties` file does not control, trigger, or publish the Desktop build.

Windows releases use independent tags:

```text
desktop-v1.0.0
desktop-v1.0.1
desktop-v1.1.0
```

The app checks only releases whose tag starts with `desktop-v`. When a newer version is available, Levyra:

1. displays a translated update notification;
2. downloads the correct Windows MSI;
3. verifies the `.sha256` file published with the installer;
4. closes the app;
5. installs the new version over the existing installation;
6. reopens Levyra.

The stable `upgradeUuid` identifies every Windows version as the same installed application.

Desktop releases are created with `--latest=false`, so the Android release remains the repository's Latest release.

## Languages

The desktop version compiles the same localization catalog used by the Android APK. Twenty-six languages are supported:

- English
- Italiano
- Español
- Français
- Deutsch
- Português
- Nederlands
- Polski
- Română
- Ελληνικά
- Svenska
- Dansk
- Čeština
- Українська
- Русский
- Türkçe
- العربية
- 简体中文
- 日本語
- 한국어
- हिन्दी
- Bahasa Indonesia
- Tiếng Việt
- ไทย
- Filipino
- עברית

Arabic and Hebrew enable RTL layout in both onboarding and the entire desktop interface.

## Requirements

| Component | Version |
|---|---|
| JDK | 21 (Temurin recommended) |
| Gradle | Included wrapper (9.6.1) |
| VLC | 64-bit 3.0.x, or a libvlc runtime bundled with the app |
| WiX Toolset | 3.14, required only to generate `.msi` and `.exe` packages |

The Desktop build is independent from Android. The `desktop/` directory has its own `settings.gradle.kts`, dependency catalog, Gradle wrapper, and version file.

## Project Structure

```text
desktop/
  version.properties
  core/      models, YouTube extractor, stream resolution, downloads, and persistence
  player/    audio-player abstraction, libvlc implementation, and playback queue
  app/       Compose UI, onboarding, Library, lifecycle, updates, and Windows packaging
  packaging/ Windows icon used by jpackage
```

- `core` does not depend on Compose. It is pure Kotlin/JVM and contains testable application logic.
- `player` exposes `AudioPlayer` and `PlayerQueue`; `VlcAudioPlayer` is the only class that accesses native APIs.
- `app` connects everything through `AppContainer`, reuses the Android i18n catalog, and contains the desktop interface.

## Development

```bash
cd desktop
./gradlew run
./gradlew check
./gradlew assemble check
```

On Windows, use `gradlew.bat` instead of `./gradlew`.

## Windows Packages

```bash
cd desktop
./gradlew createReleaseDistributable
./gradlew packageReleaseMsi
./gradlew packageReleaseExe
```

Artifacts are generated in `app/build/compose/binaries/main-release/`. The workflow automatically reads `levyraDesktopVersion` from `desktop/version.properties`.

After a merge into `main`, the MSI, EXE, portable ZIP, and SHA-256 checksums are published to the independent `desktop-v<version>` release. Android `v<version>` releases remain separate and are never modified by the Desktop workflow. Pull requests generate only temporary workflow artifacts.

## VLC Runtime

When playback starts, Levyra looks for libvlc in this order:

1. the directory selected in Settings (`vlcDirectory`);
2. the `vlc` directory bundled with the application;
3. the `LEVYRA_VLC_PATH` and `VLC_HOME` environment variables;
4. standard installations under `Program Files\VideoLAN\VLC`.

To distribute Levyra without requiring users to install VLC, copy `libvlc.dll`, `libvlccore.dll`, and the `plugins` directory from a 64-bit VLC installation into `desktop/app/resources/windows-x64/vlc/` before packaging.

## Local Data

Preferences, Library data, downloads, and artwork cache are stored in `%APPDATA%\Levyra` on Windows:

| Path | Contents |
|---|---|
| `settings.json` | profile, onboarding, music preferences, audio settings, theme, language, country, and VLC path |
| `library.json` | favorites, local playlists, listening history, and recent searches |
| `downloads.json` | offline download queue, status, progress, and metadata |
| `offline/` | completed audio files and resumable temporary files |
| `updates/` | temporary installers, checksums, and update logs |
| `crash-reports/` | local crash reports |
| `session.json` | queue and position from the last session |
| `window.json` | main-window size and position |
| `cache/artwork` | on-disk artwork cache |

## Keyboard Shortcuts

| Key | Action |
|---|---|
| `Space` | play or pause |
| `Ctrl` + `→` / `←` | next or previous track in the main window |
| `→` / `←` | seek forward or backward by 5 seconds |
| `Ctrl` + `↑` / `↓` | volume up or down by 5 |
| `Ctrl` + `M` | mute or unmute |
| `Ctrl` + `Shift` + `M` | open or close the mini player |
| `Ctrl` + `S` | shuffle |
| `Ctrl` + `R` | repeat mode |
| `Ctrl` + `Q` | queue panel |
| `Ctrl` + `P` | Now Playing |
| `Ctrl` + `F` or `Ctrl` + `K` | Search |
| `→` | next track in the mini player |
| `←` | previous track in the mini player |
| `Esc` | close the mini player |

The hardware media keys (play/pause, next, previous, stop) are registered system wide on Windows while `globalMediaKeys` is enabled, so playback stays controllable when Levyra is in the tray or behind another window. Windows assigns each media key to a single process: if another player already owns them, Levyra keeps working with its in-window shortcuts.

The complete list is also shown in Settings, under the keyboard shortcuts section.

## Technical References

The Desktop lifecycle hardening used the GPL-3.0 SimpMusic project as an architectural reference, especially for single-instance handling, the mini player, crash handling, deep links, and desktop packaging patterns. Levyra's implementation was rewritten and adapted to its own state model, libvlc player, and persistence system.

## Design Constraints

The interface preserves the official Levyra icon, localized onboarding, country menus with real flags and native country names, RTL layout for Arabic and Hebrew, the integrated Library, and a closable player.

It does not introduce manual country-code fields. Every string shared with the Android APK keeps coming from the shared localization catalog; the Desktop module only owns the labels for features that exist on Windows alone (sleep timer, playback speed, global media keys, keyboard shortcuts), so no shared text can drift between the two clients.
