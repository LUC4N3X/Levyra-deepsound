# Levyra 2.5.7

## Highlights

Levyra 2.5.7 introduces **Verified High-Quality Audio** on Android: a second, optional audio-resolution path designed to improve source quality without ever treating bitrate as more important than track identity.

When enabled, Levyra can resolve audio through JioSaavn as an external provider, validate that the candidate is the same recording the user requested, verify the stream that will actually be played, and use a genuine higher-quality source when it is available. If any part of that verification fails, normal Levyra/YouTube playback remains the fallback.

## ✦ Verified high-quality routing

The new high-quality resolver works inside Levyra's existing Media3 playback architecture rather than creating a second player. The original Levyra/YouTube track remains the logical source of truth for title, artist, album, artwork, lyrics, queue identity, listening history, favorites, recommendations and MediaSession metadata; only the audio transport can change.

Three user-facing modes are available:

- **Off** keeps the existing playback path untouched.
- **Automatic** uses an alternative source only when Levyra can verify that it is the same recording and meaningfully preferable.
- **Prefer 320 kbps** prioritizes a validated 320 kbps source when one genuinely exists, with lower validated tiers and the normal Levyra path retained as fallbacks.

## ✦ Identity before bitrate

Alternative candidates are not accepted just because their titles look similar. Levyra evaluates primary and featured artists, album relationship, duration, explicit state, ISRC when available, and recording/version markers before an alternative source can enter playback.

Remixes, live performances, acoustic versions, instrumentals, karaoke, covers, sped-up or slowed edits, incompatible remasters and ambiguous candidate sets are rejected when they do not match the requested recording strongly enough.

Incomplete credited-artist matches are also rejected. Levyra would rather keep the normal source than play the wrong song at a higher bitrate.

## ✦ Real stream validation and fallback

A provider result advertising 320 kbps is not trusted blindly. Levyra probes the resolved media, validates the response and checks the effective stream characteristics before accepting the tier.

If a requested 320 kbps stream is unavailable or does not validate as expected, Levyra can fall back through lower verified tiers. Provider failures, expired mappings, stale streams and playback failures return safely to the existing resolver path instead of poisoning the active queue.

Verified alternative streams use their own collision-resistant cache namespace, while provider mappings are refreshed and invalidated independently from short-lived media URLs.

## ✦ Reliability hardening

The high-quality path is bounded so optional provider work cannot overwhelm normal playback. Concurrent lookups use atomic admission limits, repeated requests for the same identity share in-flight work, failed mappings are removed immediately, and already-upgraded tracks do not trigger another provider lookup.

Provider destinations are restricted to approved HTTPS JioSaavn/CDN domains across redirect hops without changing the working redirect behavior used by the media path.

The high-quality mode is also part of Levyra's preference snapshot and backup/restore contract, so restoring a backup keeps the UI and playback resolver synchronized with the saved setting.

## ✦ Privacy and external provider behavior

High-quality alternative audio is optional. When enabled, Levyra contacts JioSaavn directly as an external third-party provider for the requests needed to resolve the alternative audio source.

Levyra is not affiliated with or endorsed by JioSaavn. Restricted, paywalled and Pro-only candidates are rejected before they can become playable alternatives.

## Validation

The 2.5.7 implementation includes focused automated coverage for strict track matching, ambiguity rejection, provider parsing, restricted-content rejection, request profiles, stream validation, quality fallback, manifest provenance, mapping persistence, cache isolation, preference restore and localization parity.

The PR also builds a verified PR Diagnostics APK through the repository workflow. Normal YouTube playback remains the fallback owner whenever the optional high-quality path cannot produce a verified source.

## Versioning

- Version name: `2.5.7`
- Version code: `2050700`

`gradle.properties`, the Android Gradle fallback, README version wiring, architecture metadata, release notes, release badge and Fastlane changelogs are aligned to 2.5.7. Levyra Desktop remains independently versioned and is not bumped by this Android release.

## Upgrade notes

No manual migration is required for 2.5.7. Existing local library data, playlists, favorites, history, queue and settings remain on the same owners. The high-quality audio preference participates in the normal Levyra backup and restore flow.

## Final note

Levyra 2.5.7 adds another route to better audio without weakening the rule that matters most: **the audio source may change; the song must not.**

---

# Levyra 2.5.6 (previous release)

## Why 2.5.6 replaces 2.5.5

Levyra 2.5.6 replaces the short-lived 2.5.5 release. After 2.5.5 shipped, a Home regression was identified in **Scelte rapide / Quick Picks**: background seed enrichment and filtering could rebuild the shelf at the wrong moment, and the paging code could drop a partial second page by rounding 11–19 available tracks down to only 10. The same large page grouping also made the Home scroll feel slightly less fluid than intended because Compose could materialize more cards than were actually visible.

2.5.6 fixes that regression without changing recommendation scoring or the visual language of the Home. Quick Picks now keeps partial pages instead of discarding them, preserves the intended two-column ordering and right-edge peek, keeps artist exclusions immediate, and renders the dense shelf in smaller lazy 5-row columns so less UI work is performed while scrolling. A final follow-up also fixes partial seed enrichment and Personal Orbit overlap: enriched tracks are combined with the startup fallback pool, deduplicated and backfilled so **Scelte rapide reaches 20 distinct tracks** instead of stopping at a partial result such as 7, 9, 13 or 18 items. Tracks already present in Personal Orbit remain avoided while alternatives exist, but can be used only as the last-resort backfill needed to keep all 20 Quick Picks slots populated.

The 2.5.5 GitHub release is withdrawn in favor of 2.5.6. Users on 2.5.5 should update directly to 2.5.6.

## Highlights

Levyra 2.5.6 brings together the latest Android work around the parts of the app that are touched most often: Home, playback, lyrics, downloads and everyday automation. Home is faster and more personal, cached playback can reopen with less work, lyrics and motion artwork have richer rendering paths, and the library gains new ways to import and organize music.

The update also strengthens the less visible paths underneath those features. Preference reads no longer depend on synchronous DataStore access in normal runtime hot paths, YouTube stream recovery has broader semantic fallback coverage, and offline content is handled more deliberately across Home, exports and backups.

## ✦ A cleaner, more responsive Home

Home has been rebuilt around a stronger visual hierarchy. The editorial hero now blends into the surrounding background instead of reading as a detached card while scrolling, and Personal Orbit uses a cleaner lead-and-satellite layout without decorative ranking numbers.

Quick Picks can populate from the first launch instead of waiting for a larger listening history, while official artwork enrichment continues in the background. Artist exclusions now invalidate the Home projection immediately, so choosing "Do not recommend this artist" removes that artist from Quick Picks without waiting for a manual refresh.

Levyra Collections keeps targeting six distinct smart cards, Home can switch to useful local content as soon as Android reports that the device is offline, and cached remote content is preserved rather than being replaced by an empty loading state.

## ✦ Faster preferences and playback recovery

The Android preference layer now serves normal synchronous reads from an in-memory snapshot while DataStore remains the persistent source of truth. Ordered writes are persisted in the background, initial reads have bounded recovery for transient I/O failures, and failed writes roll back optimistic state instead of leaving the UI pretending a value was saved.

Playback can reuse a verified, complete Media3 cache on relaunch when the recording identity and selected audio quality still match. The fallback path rejects mismatched recordings, keeps persistent source identity compatible, and preserves the separation between normal song playback and muxed video sources.

YouTube stream resolution also gains semantic player.js analysis with bounded discovery and legacy fallback preservation. Navigation headers, decoder provenance, visitor identity and failure generations are kept coherent so recovery work does not leak across unrelated playback attempts.

Fresh installs now open the player in Canvas Immersive by default. Existing stored visual-mode choices remain respected.

## ✦ Lyrics 2.0, Downloads Pro and Motion Artwork

Lyrics 2.0 improves synchronized playback with an interpolated playback clock, word-level karaoke progress, instrumental-gap handling and typography that adapts to the active line. Rendering work is kept bounded so the richer presentation does not require a second playback path.

Downloads Pro adds persistent bidirectional sorting by recent activity, title, artist, album and duration. Large offline exports also recover more reliably when metadata writing reaches the final stage instead of stalling near completion.

Motion Artwork Resolver 2.0 can resolve candidates progressively, arbitrate provider priority, cache successful results immediately and publish upgrades without replacing a better result with stale work. Video mode continues to suppress decorative motion artwork where appropriate.

## ✦ Import, sleep timer and playback automations

Spotify and Exportify-style CSV files can now be imported through the existing playlist import pipeline. Entries are parsed tolerantly, matched through Levyra's catalog search, and unmatched rows are reported rather than being force-matched to the wrong recording.

Sleep Timer 2.0 adds an optional end fade and a bedtime schedule. Cancelling, replacing or completing a fading timer restores the captured volume baseline, and scheduled timers follow local time and timezone changes.

The playback automation pack adds opt-in controls for resuming after an eligible Bluetooth route returns, pausing when the media stream is muted, downloading a track when it is liked, and skipping a track after the existing recovery stack classifies a failure as unrecoverable. The skip path remains bounded by the queue and consecutive-failure guards.

## ✦ Offline and backup reliability

Levyra can now restore offline-track records from backups and reconcile them with media that is still present on the device. Restored media deletion follows Android's consent model where required, while missing or stale records are cleaned up instead of silently becoming permanent broken entries.

The Room database moves from schema 19 to 20 with an additive recommendation-feedback table. Existing data is preserved through the registered migration.

## Validation

The 2.5.6 release content was prepared from the Android changes currently merged on `main` after the 2.5.3 release. The repository contains focused automated coverage for Home render stability and artist exclusions, preference-store recovery, playback cache identity and audio-quality isolation, semantic player analysis, YouTube stream recovery, Lyrics 2.0 timing and rendering helpers, motion-artwork upgrade behavior, library sorting, Spotify CSV parsing, sleep-timer scheduling, backup/offline reconciliation and the Room 19-to-20 migration.

This metadata commit does not claim a new physical-device, Android Auto, long-session, notification, Bluetooth, bedtime-alarm or final signed-APK validation run before publication. The signed release artifact is built and verified by the repository's existing `Publish Release APK` workflow after the version commit reaches `main`.

## Versioning

- Version name: `2.5.6`
- Version code: `2050600`

`gradle.properties`, the Android Gradle fallback, README version wiring, architecture metadata, release notes, release badge and Fastlane changelogs are aligned to 2.5.6. Levyra Desktop remains independently versioned and is not bumped by this Android release.

Version 2.5.4 was not published as a GitHub release. Version 2.5.5 was withdrawn after the Quick Picks and Home-scroll regression was identified; 2.5.6 was the published Android release after 2.5.3.

## Upgrade notes

No manual migration is required. Existing installations move from Room schema 19 to 20 through the registered additive migration, preserving the existing library and playback data while adding recommendation-feedback storage. Install the update normally over an existing Levyra installation from the same compatible signing channel.

## Final note

Levyra 2.5.6 makes the app feel more immediate without trading away the local-first behavior underneath it. Home reacts faster, playback has stronger recovery paths, and the new library and automation tools stay attached to the same data and playback owners already used by the app.

---

# Levyra 2.5.3 (previous release)

## Highlights

Levyra 2.5.3 expands discovery and library control while tightening playback recovery. Now Playing gains Similar Songs with queue and radio actions, Levyra Ambient adds an OLED-friendly playback surface, Home can rediscover older favorites, and playlists gain tags and hidden-library organization.

## ✦ More ways to keep listening

Now Playing adds a "You might also like" shelf based on the track that is actually playing. Recommendations avoid the current song, tracks already in the queue, obvious repeats and common re-uploads, and can be played immediately, added to the queue or used to start continuous radio. Jam actions follow the guest permissions already active for the session.

Home also gains Rediscover, built from favorites and listening history already stored on the device. Narrow-screen shelves have been tightened so titles, artwork and video metadata fit more reliably.

## ✦ Ambient and library control

Levyra Ambient reuses the existing playback session for an OLED-friendly artwork or Canvas view with the current lyric line and progress. It is available in-app, through a Quick Settings tile and as an Android DreamService without creating a second player.

Playlist tags and hidden playlists make larger libraries easier to organize, while artist exclusions keep selected artists out of personalized recommendations without blocking search or the artist page. These settings are included in Levyra Vault backups.

## ✦ Playback resilience

YouTube player configurations are verified before Levyra trusts them, and playback client policy can now be applied separately to player, streaming, browse and metadata capabilities. Stream requests also keep the identity of the client that resolved them instead of falling back to an unrelated request profile.

## Validation

The 2.5.3 release content was reviewed against the Android changes currently on `main` after the 2.5.2 version bump. The repository contains focused automated coverage for Similar Songs selection and Jam actions, the Room 18-to-19 migration, artist exclusions and playlist organization, player-config verification, playback compatibility policy, and stream client identity.

This version metadata update does not claim a new signed-artifact, physical-device, Android Auto or long-session validation run. The repository-required local quality-gate commands were not executed by this metadata edit environment.

## Versioning

- Version name: `2.5.3`
- Version code: `2050300`

`gradle.properties`, the Android Gradle fallback, README version wiring, architecture metadata, release notes and Fastlane changelogs are aligned to 2.5.3. Levyra Desktop continues to use its own version file and release tags.

## Upgrade notes

No manual migration is required. The Android database moves from schema 18 to 19 through the existing additive migration, preserving playlists, tracks and followed artists while adding playlist organization and artist exclusions. Install the update normally over an existing Levyra installation from the same compatible signing channel.

## Final note

Levyra 2.5.3 makes discovery easier to continue, gives the library more structure, and strengthens the playback paths underneath it without changing the app's local-first model.

---

# Levyra 2.5.2 (earlier release)

## Highlights

Levyra 2.5.2 is a Home and artist experience release. The personal radio now opens the Home screen with a real artist portrait, the artist page adopts a numbered popular list with a dedicated action bar, wide artwork keeps faces visible, and Explore genres return to the screen you came from.

## Home

- The personal radio hero moves to the top of Home, right after the greeting and mood row.
- The hero resolves a real artist portrait from followed, home and similar artists, with a final fallback so a portrait is used whenever one is available.
- A glass chip above the title shows the artist thumbnail with the RADIO label and the artist name.

## Artist page

- Monthly audience, Follow, shuffle and the large play action move into a dedicated action bar below the canvas hero.
- Popular tracks become a numbered list with the first five tracks visible and a Show all / Show less control up to ten.
- The floating top bar now draws an opaque background as it fades in, so the artist name no longer overlaps the list.

## Artwork and navigation

- Wide artwork slots crop with a top-biased alignment, so faces are no longer cut on the personal listening card.
- Opening a genre from Explore and pressing back returns to Explore instead of the standalone moods and genres screen. Opening a genre from that screen still returns to it.

## Versioning

- Version name: `2.5.2`
- Version code: `2050200`

`gradle.properties`, the Android Gradle fallback, README version wiring, architecture metadata, release notes and Fastlane changelogs are aligned to 2.5.2. Levyra Desktop continues to use its own version file and release tags.

## Upgrade notes

No manual migration is required. Install the update normally over an existing Levyra installation from the same compatible signing channel. Favorites, playlists, followed artists, history, queue and settings are preserved.

---

# Levyra 2.5.1 (earlier release)

## Highlights

Levyra 2.5.1 is a feature and reliability update focused on the parts of the app you use every day: getting music playing, moving it to another screen, recognizing a song, keeping your library safe, and making network behavior easier to control.

Google Cast is now available in the upstream Android build, music recognition has grown into a complete on-device flow, and Levyra Jam can synchronize listening across devices on the same local network. Playback also gains a new SABR delivery path and stronger recovery when a normal stream candidate cannot be used.

The release stays local-first. Listening data, recognition history, followed artists, settings and backups remain owned by the app on your device unless you explicitly use an external integration.

## ✦ Cast and shared listening

Levyra can now hand playback to Google Cast devices in the upstream build while keeping the existing player and queue as the source of truth for the handoff. The F-Droid flavor keeps its separate no-op Cast backend rather than pulling Google Play Cast dependencies into that build.

Levyra Jam adds another way to listen together without an account or Levyra cloud service. Devices on the same Wi-Fi can create or join a local session, synchronize playback and share queue changes according to the host permissions.

Jam sessions use session codes, host authority and an authenticated challenge-response handshake. Guest queue access can remain restricted or be opened for a collaborative session.

## ✦ Music recognition 2.0

Music recognition is now a full Levyra feature instead of a single recognition action.

- Shazam-compatible acoustic fingerprint matching can run without a user API credential.
- Recognition can use the microphone or, on supported Android versions, capture device playback through MediaProjection.
- An optional AudD fallback can be configured, with its credential stored through Android Keystore-backed storage.
- Recognition history is stored locally and can be surfaced through the recognition screen, notifications, Quick Settings and the home-screen widget.
- Catalog matching connects recognized tracks back to Levyra when a suitable result is available.

The recognition pipeline is layered so one provider failing does not automatically end the whole attempt.

## ✦ Playback and YouTube resilience

This release adds a native SABR delivery path alongside Levyra's existing stream handling. The player can assemble SABR segments, read the UMP transport and recover through alternate playback candidates when the first path is not usable.

Playback resolver and compatibility policy work was also tightened around current YouTube behavior. The goal is practical: fewer dead starts and fewer cases where a temporary stream or delivery failure becomes a permanent playback failure.

Queue persistence and radio continuation received additional guardrails, while direct playback remains higher priority than optional artwork, recognition or enrichment work.

## ✦ Network controls you can actually use

Levyra now exposes a dedicated network configuration layer instead of forcing every request through one fixed setup.

You can choose built-in DNS-over-HTTPS providers such as Cloudflare, Google, AdGuard and Quad9, configure a custom HTTPS resolver, or route compatible traffic through HTTP or SOCKS proxies. Proxy credentials are stored using Android Keystore-backed storage, and the app includes safeguards intended to avoid proxy loops.

A network test path is available from settings, and direct media streams can bypass the configured proxy when that option is enabled. These controls change routing only when you choose them; Levyra does not add an analytics or tracking proxy.

## ✦ Safer local backups

Levyra Vault has been expanded into a more complete local backup and restore system. Versioned `.levyra` archives can carry settings, favorites, playlists, followed artists, history and queue state without requiring an online account.

Restore now includes manifest and required-section validation, SHA-256 integrity checks, compatibility preview and rollback protection before existing local data is replaced. Automatic backup policy also supports manual, scheduled and pre-update snapshots, selectable retention and Android SAF destinations, with internal storage kept as the fallback.

Database migrations for the new local stores are included together with migration coverage. No manual data migration is required for an existing Levyra installation.

## ✦ Artists, artwork and audio setup

Followed-artist storage and Release Radar were strengthened so artist identity and new-release checks are less dependent on mutable display text. Artist and YouTube Music resolution also received additional identity handling for albums and artist pages.

Motion artwork now has broader Apple Music artwork selection and more defensive matching and URL validation. Static artwork remains the fallback, and motion artwork remains decorative in Song mode rather than replacing native Video mode.

Audio settings also gain AutoEQ import support for bringing compatible headphone EQ data into Levyra's existing equalizer setup. Settings search has been expanded so the growing audio, network and integration controls are easier to find.

## ✦ Integrations and privacy

Levyra now includes scrobbling support for Last.fm and ListenBrainz with bounded deduplication. These integrations are optional. Using them is an explicit user choice and does not change Levyra's default local listening model.

The app still does not add a Levyra account, advertising identifier or analytics pipeline. Local listening statistics, recognition history, playlists, queue state and backups remain local unless a feature explicitly requires a third-party request chosen by the user.

## Validation

The 2.5.1 release content was reviewed against the current Android `main` changes after tag `v2.5.0`, including the Cast source-set split, recognition module and providers, Jam transport and protocol, SABR playback path, network configuration, backup changes, Room schemas and migrations, AutoEQ importer, artist/release handling and motion-artwork updates.

The repository contains focused automated coverage for SABR parsing and assembly, playback candidate recovery, network configuration and testing, Jam protocol/security/session codes, recognition providers and fingerprinting, AutoEQ persistence/import, artist identity, backup policy and Room migrations.

The final signed artifact is not claimed as already validated by this document. The existing `Publish Release APK` workflow is responsible for release-note validation, Android lint, signed release assembly, APK version verification, signer-certificate verification, SHA-256 generation, GitHub Release publication and published-asset verification before the release job can complete successfully.

No new physical-device, Android Auto, Cast receiver, long-session memory or OEM-specific validation run is claimed as part of this version metadata update. Those areas remain unverified here unless separate direct evidence is attached elsewhere in the repository history.

## Notes

This is an Android-only release. Levyra Desktop remains independently versioned and is not changed by 2.5.1.

The GitHub/upstream Android build can include Google Cast support. F-Droid keeps its separate build flavor and does not inherit the Google Play Cast implementation.

## Versioning

- Version name: `2.5.1`
- Version code: `2050100`

`gradle.properties`, the Android Gradle fallback, README version wiring, architecture metadata, release notes and Fastlane changelogs are aligned to 2.5.1. Levyra Desktop continues to use its own version file and release tags.

## Upgrade notes

No manual migration is required. Install the update normally over an existing Levyra installation from the same compatible signing channel. The included Room migrations and backup compatibility work are designed to preserve existing favorites, playlists, followed artists, history, queue, settings and other local app state.

GitHub installations continue to use Levyra's signed GitHub release and update path. F-Droid, IzzyOnDroid and other distribution channels follow their own build or index schedules, so availability can lag behind the GitHub release.

## Final note

Levyra 2.5.1 makes the Android app more capable without moving ownership away from the user. You can cast to another screen, identify what is playing, listen together on a local network, recover from more playback failures and keep a stronger local backup of the app state.

The new network and integration options are there when you want them. The default remains the same: direct playback, local data and no Levyra account required.
