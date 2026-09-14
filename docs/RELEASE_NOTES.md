# Levyra 2.5.8

## Highlights

Levyra 2.5.8 is a broad Android update focused on the parts you notice every day: how music sounds, how reliably playback recovers, how you discover something new, and how much of your listening history the app can turn into something useful.

Audio Intelligence 3.0 brings more accurate volume normalization, better continuity for albums that are meant to flow without a break, and a searchable AutoEQ headphone catalog. Listening Recap and Listening Insights make local history far more useful, while Explore, Live Radio, Search, Home, playlists, downloads and Motion Artwork all received meaningful upgrades.

This is still Levyra: no account requirement was added, personal listening stats remain on-device, and the Android and Desktop release lines stay independent.

## ✦ Audio Intelligence 3.0

Volume normalization now uses YouTube loudness information correctly instead of treating every track as if it needed the same adjustment.

Album playback is also smarter. Consecutive tracks from the same release can continue cleanly even when crossfade is enabled, which matters for live albums, DJ mixes and records designed as one continuous listen. When playback leaves that album sequence, normal crossfade behavior remains available.

AutoEQ is easier to use too: the headphone correction catalog is searchable, so finding and applying a profile no longer depends on knowing exactly where a model sits in the catalog.

## ✦ Listening Recap and deeper Insights

Levyra can now turn your local listening history into a much richer picture of what you have actually been playing.

Listening Recap adds a more visual summary of your listening, while Listening Insights brings period-based analytics, chronological 24-hour activity, listening rhythm, top music, discovery highlights and searchable history.

The important part is where this data lives: the feature is built on Levyra's existing local listening history. It does not require a Levyra account or a new analytics service.

## ✦ Explore, Live Radio and Search

Explore has been reorganized to make discovery easier to scan and faster to use. Levyra Live Radio now sits inside that experience with worldwide and locale-based station discovery.

Home has also been polished with a new Personal Orbit layout and lighter scrolling work, while Search gets a stronger top-result card with direct actions and track context instead of feeling like a plain result list.

Together these changes make discovery feel less like a collection of separate screens and more like one connected path from curiosity to playback.

## ✦ Better playlists, downloads and artwork

Playlist Pro adds fast local search across title, artist and album, stable multi-select actions, ordered queue operations and custom square playlist covers. Custom covers are preserved through Levyra backup and restore.

Downloads can now be saved to a folder you choose, including supported SD-card locations, so storage is no longer tied to one fixed destination.

Motion Artwork has a stronger fallback chain for Canvas sources, with an optional Wi-Fi-only policy for people who want tighter control over mobile data usage. Normal behavior remains available without forcing Wi-Fi-only loading.

## ✦ Playback resilience and high-quality audio

The high-quality audio path has been hardened further. JioSaavn resolution is more resilient, startup work is tighter, and new installs now default to High audio quality.

The YouTube player-config path is also harder to break. Levyra now validates an ordered source chain and keeps last-known-good data before falling back to the bundled configuration, so a bad or unreachable remote config cannot simply replace a working one.

These are deliberately fallback-oriented changes: optional providers and remote configuration should improve playback when they work, not become new single points of failure.

## Validation

Levyra 2.5.8 is published through the repository's signed Android release workflow. Before GitHub can publish the release, that workflow validates the release metadata and required note structure, runs Android release lint, builds the signed release APK, checks the APK version name and version code, verifies the signing certificate, generates a SHA-256 checksum, uploads both artifacts and verifies the published download again.

The feature work included in 2.5.8 also carries focused automated coverage across audio intelligence behavior, AutoEQ catalog handling, listening recap and analytics, playlist operations and backup behavior, player-config fallback, high-quality audio resilience and Motion Artwork policies.

These notes do not claim a new full physical-device matrix, Android Auto pass, Bluetooth matrix or long-session OEM test for every feature in the release.

## Versioning

- Version name: `2.5.8`
- Version code: `2050800`

This is an Android release. Levyra Desktop remains independently versioned and is not bumped by 2.5.8.

## Upgrade notes

No manual migration is required.

Existing favorites, playlists, custom playlist covers, listening history, queue state, settings and other supported local data continue through Levyra's normal database migration and backup/restore paths.

GitHub users can update through the normal signed Levyra release. F-Droid, IzzyOnDroid and other third-party distribution channels follow their own build and indexing schedules, so 2.5.8 may appear there later than on GitHub.

## Final note

2.5.8 makes Levyra feel more complete without changing what the project is built around: better playback, more useful local data, stronger discovery and fewer fragile paths between pressing Play and hearing the right track.
