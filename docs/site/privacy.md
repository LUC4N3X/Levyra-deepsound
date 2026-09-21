# Privacy & Data

Levyra is built on local-first principles: your listening habits belong on your device, network access is direct and transparent, and optional integrations remain strictly optional.

## Core principles

- No mandatory accounts. You never need to sign up to use the app.
- No advertising profiles or behavioral tracking.
- No remote telemetry or analytics collection.
- Listening history, play counts, and statistics are stored locally on your device.
- All library data, playlists, and settings can be exported as a portable backup.
- Third-party integrations (like scrobbling or song recognition) only run when you explicitly use them.

## Local storage

Levyra saves data locally on your device to enable offline playback and manage your library:

- Playlists, favorite tracks, and followed artists
- Listening history, play counts, and listening analytics
- Active and saved queue state
- Downloaded audio files and cache directories
- User preferences and audio settings
- Locally stored song recognition history
- Backup archives created with Levyra Vault

This data remains stored on your device and is never uploaded to Levyra servers.

## Network requests

Levyra connects to external services only when necessary to perform features you trigger:

- Streaming audio and metadata endpoints (such as YouTube Music infrastructure)
- Lyric databases such as LRCLIB
- Public metadata and album artwork providers
- SponsorBlock servers for crowd-sourced segment skipping
- GitHub API endpoints for version checks and release assets
- Optional scrobbling endpoints (Last.fm or ListenBrainz) if configured by you

When contacting these third-party services, standard HTTP request metadata (such as your IP address and User-Agent) is transmitted to the respective providers according to their own privacy policies.

## Android permissions

Levyra requests only the permissions required for core audio functionality:

| Area | Permissions | Purpose |
| --- | --- | --- |
| Network | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` | Streaming music, fetching lyrics, and checking updates |
| Playback | Foreground media service, `WAKE_LOCK` | Keeping playback running smoothly with screen turned off |
| Notifications | `POST_NOTIFICATIONS` | Displaying media controls in the Android notification shade |
| Local files | `READ_MEDIA_AUDIO` (or storage access on older Android) | Reading and organizing local music files stored on device |
| Song recognition | `RECORD_AUDIO`, microphone foreground service | Listening for audio snippets during music identification |
| Internal capture | MediaProjection foreground service | Capturing internal audio on supported Android releases |
| Background tasks | Data-sync foreground service | Completing background downloads and offline sync |
| Updates | `REQUEST_INSTALL_PACKAGES` | Installing APK updates directly for standalone builds |

All permissions requiring user consent are requested at runtime only when the corresponding feature is used.

## Song recognition

Using the microphone for song recognition is entirely optional and only runs when you press the identify button. Audio recorded during recognition is processed solely for fingerprinting and is not stored permanently or shared for other purposes.

## Exported media

Tracks downloaded through Levyra are standard M4A files saved to your device storage. Uninstalling the app does not delete your exported media files from public storage directories.

## Open source transparency

The complete source code and permission manifest are open for inspection in [AndroidManifest.xml](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/AndroidManifest.xml).

For additional legal and licensing details, see [LEGAL.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/docs/legal/LEGAL.md).

!!! warning "Security reports"
    Never post API keys, credentials, or private sensitive data in public GitHub issues or discussions.
