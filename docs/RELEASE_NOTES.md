# Levyra 2.6.2

## Highlights

2.6.2 is a focused Android maintenance release for player navigation, artist playback and high-quality source matching. It keeps the screen you came from stable while playback starts, makes artist pages behave consistently with albums and playlists, and improves JioSaavn matching without weakening the existing identity checks.

The release also refreshes Levyra's in-app branding and Home header contrast, while keeping the app's data, playback architecture and release channels unchanged.

## ✦ Player navigation stays where you left it

Starting a song from an artist page no longer closes the page underneath the player. Artist video cards now play their own visible list instead of unexpectedly switching to the artist's top-songs queue, and the selected tab behind the expanded player survives activity recreation.

Artist pages now use the same detail-screen now-playing dock as albums and playlists. Tapping the artist name in Now Playing resolves and opens the artist above the collapsed player when the current item has a usable artist identity; live radio and placeholder credits remain excluded.

## ✦ More reliable JioSaavn matching

Artist top songs now take their album identity from the album link instead of the play-count column. Music-video titles are reduced to the underlying recording for chart display and matching queries, and the Russian band designator is normalized consistently with names beginning with “The”.

When a track's duration is not known yet, Levyra defers the JioSaavn lookup until the YouTube manifest supplies a verified duration, within the existing bounded wait. The strict title, artist, album and duration checks still decide whether the high-quality source is safe to use.

## ✦ Branding and readability polish

The in-app Levyra branding has been synchronized with the current identity, and the Home header contrast has been adjusted for clearer readability. The repository presentation artwork was refreshed separately and does not affect the Android package.

## Validation

The player/navigation and JioSaavn changes include focused regression tests for artist routes, player restoration, shared detail-screen player behavior, display-title cleanup and matching decisions. The release also keeps the existing F-Droid source-build and review-contract checks in the repository quality gate.

The GitHub Android release workflow remains the publication gate for 2.6.2. It validates these notes and the version, runs release lint, builds the signed APK, verifies the APK version and signing certificate, writes the SHA-256 checksum, publishes the release, and downloads the published assets again for verification. If those checks fail, the release is not considered published.

Physical-device playback, Android Auto, native-video mode and live-radio behavior have not been manually revalidated specifically for this release candidate.

## Versioning

- Version name: `2.6.2`
- Version code: `2060200`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

This patch does not add a new database schema migration. Existing favorites, playlists, downloads, history, settings and sessions stay on their current data paths.

GitHub users can update from the signed APK attached to this release once publication completes. F-Droid and other repositories follow their own build and publishing schedules.

## Final note

2.6.2 keeps the player experience coherent: the page behind playback stays put, artist actions open the right destination, and high-quality matching gets better inputs without becoming less strict.
