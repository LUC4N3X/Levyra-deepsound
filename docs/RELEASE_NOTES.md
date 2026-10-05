# Levyra 2.6.3

## Highlights

2.6.3 is built around the reports that came in after 2.6.2. The three user issues closed in this cycle are all covered here: playlist imports that could not find tracks which were actually on YouTube, offline downloads that could stop at 83% on older Android versions, and the missing Moods side of Moods & Genres.

Beyond those fixes, Explore is faster and steadier, motion artwork can recover from more catalog misses, and Levyra can use Android hardware audio offload when the active playback chain does not need PCM processing. There are also smaller UI and navigation fixes around Home, Library and returning from the Player.

## ✦ Reported issues fixed

- [#823 — Trying to find missing songs](https://github.com/LUC4N3X/Levyra-deepsound/issues/823): playlist import manual search now combines YouTube Music song results with wider YouTube search, prioritizes official audio and music-video candidates, and can retry with a cleaner title when noisy metadata gets in the way.
- [#825 — Offline downloads fail at 83% on Android 11 and below](https://github.com/LUC4N3X/Levyra-deepsound/issues/825): muxed MP4 downloads can now copy the existing AAC audio directly into the final M4A container instead of depending on device AAC re-encoding. The previous Transformer path remains as a controlled fallback when direct remuxing is not possible.
- [#831 — Moods](https://github.com/LUC4N3X/Levyra-deepsound/issues/831): Explore now has real provider-native mood and genre sections instead of showing only genres. Categories keep their localized names and provider routing data, and the follow-up navigation work keeps the selected Explore destination in place when the Player is opened and closed.

## ✦ Moods & Genres is finally both

Explore now reads the provider's real localized mood and genre taxonomy and keeps the exact category routing needed to open each destination. Category state and artwork are cached per language so the screen can come back quickly instead of rebuilding itself every time.

Editorial discovery has also been tightened. When a safe match is available, published category metadata can use official Spotify editorial information while YouTube Music remains the playback identity and runtime fallback. Weak artwork and fake provider-style album labels are filtered out, visible items can still use Levyra's existing metadata enrichment, and the Explore string bundle is complete across all 37 Android languages.

Opening a track from a mood or category no longer lets a hidden Explore Back handler consume the first Back press behind the expanded Player. Back belongs to the Player first, so collapsing it reveals the same category you were browsing.

## ✦ Playlist imports search wider

When an imported playlist contains an unresolved track, Levyra no longer gives up just because the narrow Songs search returned something. Manual and adaptive matching can combine song results with the wider YouTube result set, deduplicate candidates and prefer official recordings before user uploads.

Switching between Online and Local while reviewing a missing track refreshes the search immediately, and candidate rows are easier to select. The existing fresh-start behavior after completing one playlist import is unchanged.

## ✦ Downloads are safer on older Android

The 83% failure reported on older Android devices came from the audio-extraction stage for muxed MP4 streams. Levyra now uses Android's MediaExtractor and MediaMuxer to remux compatible AAC tracks without decoding and re-encoding them first.

That keeps the operation lossless for the existing AAC stream, avoids relying on older device encoders for the common MP4/AAC case, and still leaves a bounded Media3 Transformer fallback for formats that cannot be remuxed directly.

## ✦ Motion artwork reaches further

Motion Artwork can now use an on-demand Spotify Canvas resolver after local and Community Canvas catalog misses. Matching stays strict, successful and negative results are cached, in-flight work is isolated from rapid track changes, and an unresolved request falls through to the existing Apple, Tidal and static-artwork paths without blocking audio playback.

The editorial side also gained an additional Canvas recovery path and stronger cache behavior, while provider credentials stay out of the Android package.

## ✦ Smarter audio efficiency

Audio Settings now includes an automatic hardware audio-offload policy. Levyra can hand eligible playback to the device's offload path when nothing currently needs PCM processing, then move back to the normal software path when features such as EQ, AutoEQ, normalization, crossfade, speed or pitch changes, skip-silence, or other PCM-dependent processing require it.

Enhanced Audio remains conservative for lossy and unknown sources. When a source is positively identified as lossless and Enhanced Audio is already bypassing it, hardware offload can still be used safely instead of being blocked just because the feature is enabled.

## ✦ Small interface fixes that matter

The Home header gives the Levyra mark more room and lets long localized greetings or user names wrap instead of being cut off. Library Quick Picks also scale more cleanly with larger font sizes while keeping paired cards aligned.

## Validation

The issue fixes and feature work landed with focused regression coverage in their respective pull requests. The missing-track playlist import flow was exercised on an Android API 35 emulator. The 83% download path was validated with muxed MP4/AAC downloads on Android 10, 11 and 12 emulators, including offline playback after export. Canvas resolver behavior was exercised on an API 37 emulator, including catalog hits, resolver misses and rapid track switching.

Moods & Genres includes focused coverage for provider category routing, caching, metadata stabilization and localization, and the audio-offload policy has dedicated tests for its eligibility and blocker decisions.

The GitHub Android release workflow remains the publication gate for 2.6.3. It validates these notes and the version, runs release lint, builds the signed APK, verifies the APK version and signing certificate, writes the SHA-256 checksum, publishes the release, and downloads the published assets again for verification. If those checks fail, the release is not considered published.

## Versioning

- Version name: `2.6.3`
- Version code: `2060300`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

This release does not add a new database schema migration. Existing favorites, playlists, downloads, history, settings, import sessions and local library data stay on their current data paths.

GitHub users can update from the signed APK attached to this release once publication completes. F-Droid and other repositories follow their own build and publishing schedules.

## Final note

2.6.3 is a good example of the issue tracker shaping the release directly: all three reports closed since 2.6.2 are represented here, while the surrounding work makes discovery, downloads, motion artwork and playback a little more dependable at the same time.
