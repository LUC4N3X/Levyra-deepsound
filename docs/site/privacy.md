# Privacy & Data

Levyra is designed around local-first data, transparent network access and user-controlled integrations.

## Core principles

- No mandatory Levyra account.
- No advertising profile.
- No developer-operated analytics profile.
- Listening history and listening insights are stored locally by default.
- User data should remain portable.
- Optional integrations stay optional.

## Local data

Depending on the features you use, Levyra can store information such as:

- playlists and favorites;
- followed artists;
- listening history and play counts;
- listening statistics;
- queue state;
- settings;
- download information;
- cached metadata and lyrics;
- recognition history;
- backup data.

## Network access

Levyra connects to external services when a feature requires them. Examples can include:

- YouTube / YouTube Music infrastructure for playback and metadata;
- lyric providers such as LRCLIB;
- public music metadata and artwork providers;
- SponsorBlock;
- GitHub-hosted project data and release information;
- optional scrobbling or recognition providers.

Those third-party services operate under their own terms and privacy policies and receive normal network information such as the device IP address when contacted.

## Android permissions

The current Android app declares permissions supporting features such as:

| Area | Examples |
| --- | --- |
| Network | `INTERNET`, `ACCESS_NETWORK_STATE`, `ACCESS_WIFI_STATE` |
| Playback | foreground media playback services, `WAKE_LOCK` |
| Notifications | `POST_NOTIFICATIONS` |
| Local music | `READ_MEDIA_AUDIO` and legacy storage permissions where applicable |
| Recognition | `RECORD_AUDIO`, microphone foreground service |
| Internal playback capture | MediaProjection foreground service support |
| Background data work | data-sync foreground service |
| App updates | `REQUEST_INSTALL_PACKAGES` for supported update builds |

Android remains responsible for runtime authorization where the platform requires it.

## Music recognition

Microphone recognition is optional. Internal playback recognition uses Android's MediaProjection system on compatible versions and requires the platform authorization flow.

## Offline files

Supported exported music is designed to remain standard user-owned media. Removing the app does not necessarily remove deliberately exported files from public storage.

## Open source transparency

You can inspect the current permission surface directly in [AndroidManifest.xml](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/app/src/main/AndroidManifest.xml).

For legal, third-party and liability information, read [LEGAL.md](https://github.com/LUC4N3X/Levyra-deepsound/blob/main/LEGAL.md).

!!! warning "Security reports"
    Never post secrets, tokens, private credentials or other sensitive information in a public GitHub issue.
