# Levyra 2.6.1

## Highlights

2.6.1 is a focused Android maintenance release built around the problems reported after 2.6.0. The biggest changes are simple: Library should stop hanging on large local collections, playback should recover more cleanly after short network drops, and the playlist importer should stop reopening the playlist you just finished importing.

There are a few smaller improvements in the same build too. Android 16 gets Live Updates for playback and downloads, Settings search is more useful, downloaded M4A files can carry lyrics with them, and Levyra now uses the refreshed official app icon.

## ✦ Fewer freezes and fewer interrupted songs

Opening Library with a lot of downloads could spend far too long matching offline tracks on the main thread. That path has been reworked so the catalog no longer repeatedly scans and normalizes the entire known-track list for every download. This addresses the freeze / "Levyra isn't responding" report in [#794](https://github.com/LUC4N3X/Levyra-deepsound/issues/794).

Playback recovery was tightened as well. A short connection reset or temporary outage should no longer leave the current track paused waiting for you to press Play again. Levyra can resume the stream in place when possible, retry transient network failures with bounded backoff, and keep the original play intent through recovery. An intentional pause still wins and is not overridden.

A separate Home edge case was fixed at the same time: if the personal listening pager shrinks while an old page index is still active, Levyra now handles that stale index safely instead of risking an `IndexOutOfBoundsException`.

## ✦ Playlist import starts fresh again

After finishing an import, reopening the importer could bring the previous playlist back with no obvious way to start another one. A completed import now reopens on a clean input screen, and the summary includes an **Import another playlist** action.

This fixes [#810](https://github.com/LUC4N3X/Levyra-deepsound/issues/810). Unfinished imports still keep their resumable session behavior.

## ✦ Downloaded M4A tracks can carry lyrics

Downloaded M4A files can now embed the lyrics Levyra already has available. Synced lyrics keep their LRC timestamps, plain lyrics remain plain, and missing or instrumental lyrics do not block the download/export path.

This implements the request in [#793](https://github.com/LUC4N3X/Levyra-deepsound/issues/793) for lyrics that can also be read by compatible external music players.

## ✦ Android 16 and Settings polish

On Android 16, active playback and downloads can use the platform Live Update surface when the system allows promoted notifications. Older Android versions keep the existing notification path.

Settings search has also been rebuilt from the settings that actually exist in Levyra, with stable entries and useful aliases for things such as 320 kbps, JioSaavn, DNS, proxy, AutoEQ, Canvas and cache. Opening a result keeps the search context so Back can return to it.

The Android launcher icon has been refreshed across adaptive, legacy, round and themed icon variants, including the Fastlane store asset.

## Related issues

- [#794 — Consistent Crashing And Freezing](https://github.com/LUC4N3X/Levyra-deepsound/issues/794)
- [#810 — Importing Spotify playlist issue](https://github.com/LUC4N3X/Levyra-deepsound/issues/810)
- [#793 — Embedded Lyrics](https://github.com/LUC4N3X/Levyra-deepsound/issues/793)

## Validation

The Library fix has focused regression coverage for download matching and was exercised on an emulator with a large seeded library. The reported ANR path was reproduced before the fix and the same stress flow completed without an ANR after the change.

The playback recovery work has focused tests around stream resume, network classification and retry coordination, plus emulator checks covering connection reset, a longer network outage and an intentional pause during recovery. Physical-device, Android Auto, video-mode and live-radio validation were not part of that pass.

The playlist-import fix passed its focused controller/import/i18n test set, including regressions for reopening after a completed import and starting a second independent import. It was not manually tested on a device or emulator before merge.

The Android 16 Live Update and Settings search change had green PR CI, including unit tests, Android lint, release compile, F-Droid checks and diagnostics. The final user-facing Live Update behavior was not manually exercised on an Android 16 device during that change.

For M4A lyric embedding, the fast repository quality gate passed. The full local Gradle path was blocked in that authoring environment by a Java/Gradle loopback failure, and reading the embedded lyrics in an external player was not manually tested there.

The GitHub Android release workflow remains the publication gate for 2.6.1. It validates these notes and the version, runs release lint, builds the signed APK, verifies the APK version and signing certificate, writes the SHA-256 checksum, publishes the release, and downloads the published assets again for verification. If those checks fail, the release is not considered published.

## Versioning

- Version name: `2.6.1`
- Version code: `2060100`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

This patch does not add a new database schema migration. Existing favorites, playlists, downloads, history, settings and resumable playlist-import sessions stay on their current data paths.

GitHub users can update from the signed APK attached to this release once publication completes. F-Droid and other repositories follow their own build and publishing schedules.

## Final note

2.6.1 is mostly about removing friction from the places people actually hit every day: opening Library, surviving a shaky connection, importing another playlist, and keeping downloaded music useful outside Levyra too.
