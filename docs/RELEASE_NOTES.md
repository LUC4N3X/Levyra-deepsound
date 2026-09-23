# Levyra 2.5.10

## Highlights

2.5.10 puts a lot of work into the parts of Levyra you notice every day: playback recovery, audio control, search, lyrics, the player, and the little shortcuts that make the app quicker to use.

The biggest change is not a single screen. Levyra now has more ways to recover when a YouTube player configuration or an alternative audio source goes bad, while keeping a clearer line between verified data and emergency fallbacks. On top of that, this release adds Parametric EQ with AutoEQ support, ReplayGain 2.0, a proper technical audio panel, Home Speed Dial, more personal search results, followed-artist release alerts, Playlist PRO improvements, better lyric translation and sync controls, and a Quick Settings playback tile.

There is also a substantial visual pass across the player and artist pages. Motion is more consistent, artwork transitions are cleaner, queue interactions feel less abrupt, and long track titles in listening picks no longer disappear into cramped layouts.

## ✦ Playback recovery with more than one way out

The player configuration system now uses multiple sources instead of depending on a single upstream path.

Levyra keeps the repository-built configuration as the verified source of truth, compares upstream registries before publishing new data, preserves the last known good configuration when sources disagree, and can use bounded provisional data as an emergency recovery path without letting it overwrite verified state.

That recovery work also closes several edge cases around player identity, aliases, stale data and partial writes. Configuration and metadata are committed together, temporary files are cleaned up on failure, and ambiguous entries fail safely instead of silently replacing a known-good player.

Video identity handling was tightened at the same time. Audio-only identities are no longer accidentally reused as native-video candidates, and likes, dislikes and comments can resolve the real paired video when a trustworthy counterpart is available.

## ✦ JioSaavn HQ is more accurate and more resilient

The alternative high-quality audio path received another large pass.

Levyra now tries the high-quality JioSaavn variant whenever the track match is safe, even when the provider metadata does not advertise the 320 flag. The flag is treated as a hint, not proof. The selected stream is measured, so a file that is really around 250 kbps is shown as roughly 250 kbps instead of being called 320.

The resolver now:

- probes the decrypted CDN URL first;
- requests an auth token only when the direct stream needs it;
- falls back through 160 and 96 kbps on the same trusted CDN path;
- keeps approved JioSaavn CDN hosts on HTTPS and rejects unexpected hosts, ports or user information;
- hydrates the selected candidate with song details when needed and rechecks the match before playback;
- handles re-releases, soundtrack decorations, featured artists, language differences and small duration drift more carefully;
- keeps already cached playback available when the HQ lookup pool is busy instead of unnecessarily blocking the song.

The first-play path was also fixed so a cached normal stream does not start before the first JioSaavn lookup has a chance to resolve. This HQ path and the first-play behavior were tested on a physical Android phone during development.

## ✦ Parametric EQ, AutoEQ and ReplayGain 2.0

Levyra now has a real parametric equalizer path with persistence, profile handling and an AutoEQ importer/catalog flow.

You can build and save your own parametric profiles, switch between them without the old stale-profile edge cases, and use imported AutoEQ data through the same audio pipeline. The player-side processor was also tightened so profile transitions and DSP state stay in sync with what the UI shows.

ReplayGain has been expanded into ReplayGain 2.0. It now supports selectable modes, preamp control and clipping protection, reads ReplayGain information from local tags, carries the metadata through playback, and preserves the related settings in backups.

Album context, shuffle order, duplicate queue occurrences and local album-artist metadata received extra handling so smart ReplayGain selection does not drift to the wrong track or album.

## ✦ Technical Audio Info that tells you what is really playing

The Now Playing screen now has a much more useful technical audio panel.

It can show live codec, bitrate, sample rate, channels, MIME type, codec ID, provider, transport, container, stream quality and verified HQ information. It also separates source information from what Levyra is doing locally, including ReplayGain, normalization, equalizer, limiter, virtualizer and preamp state.

Output information includes the current route, volume, playback engine and local audio path. Cast playback is treated separately so Levyra does not pretend local decoder or DSP information belongs to the remote receiver.

Muxed YouTube streams are handled more carefully too, avoiding cases where video codec data or a combined audio/video bitrate could be presented as if it were audio-only information.

## ✦ Search and Home are more personal

Search can now use listening history more directly instead of treating every query as if Levyra knew nothing about you.

Personalized search, local matching and deduplication were tightened, and the "Based on your listening" area now has its own picks grid. Long titles use marquee behavior instead of being clipped into unreadable text.

Home also gains Speed Dial. Frequently used destinations and media can be pinned for quicker access, with cleanup logic that removes stale local pins after the library has actually finished scanning instead of deleting them too early.

Settings search was expanded alongside this work, and artist pages now load portrait and biography data more defensively, cancel stale work correctly and preserve useful language aliases for biography lookups.

## ✦ A smoother player and better artist pages

2.5.10 introduces a shared motion vocabulary across the player instead of a collection of unrelated animations.

The mini player to full player artwork transition now aims at the real resting frame, bridges from cached artwork, hides the duplicate mini cover during the flight and prewarms the destination so the opening animation is not swallowed by first composition.

The artist hero has also been rebuilt with artwork-driven atmosphere, parallax, a docking title and a stable Follow control. The player adds a morphing play/pause glyph, clearer download-state motion, smoother queue-item changes and a more immediate drag handle.

The goal is simple: movement should explain where something went, not make the interface feel busy.

## ✦ Lyrics and system controls are easier to live with

Lyrics fusion and translation were reworked so Levyra can combine sources more cleanly and use on-device translation with better cancellation and failure handling.

Tracks that cannot be translated are no longer retried pointlessly, and stale lyric requests are less likely to leak into the next song.

A manual lyrics offset stepper makes small sync corrections faster, while the new Quick Settings playback tile gives Android a proper system-level play/pause shortcut. Idle tile taps open Levyra without waking the playback service just to do it, and ended playback is handled separately from an active paused session.

## ✦ Followed releases and Playlist PRO

Levyra can now notify you about new releases from artists you follow.

The release radar work includes its own persistence and delivery policy instead of being tied to a screen lifecycle, with additional safeguards around duplicate or stale delivery.

Playlist PRO also gained search and multi-select tools, making larger playlists easier to manage without replacing the existing playlist model.

## ✦ Live radio compatibility is broader

Some real-world radio stations still serve streams over plain HTTP. Levyra can now handle those legacy streams without opening cleartext networking for the rest of the app.

The exception is scoped to live radio, search matching is more tolerant of human station names, stale search results can remain playable when appropriate, and returning from the player keeps the radio screen and destination state instead of dropping you somewhere unexpected.

## ✦ Localization, dependencies and project polish

Finnish and Estonian received full localization passes, smaller gaps were closed in other locales, and the project now documents support across 37 languages.

Android core and Media3-related dependencies were refreshed during this cycle. PR validation also gained more caching and parallel work so normal development checks spend less time repeating the same setup.

There was a large documentation cleanup too: clearer architecture notes, more natural README copy, simpler legal and privacy wording, corrected download links and a cleaner separation between store listings and independent media coverage. Those changes matter to the project, but they are kept out of the main feature story here because they do not change Android playback by themselves.

## Validation

The feature range was reviewed from `v2.5.9` through commit `371ccd4`, covering 360 commits before this release-version commit. Repetitive README badge refreshes were treated as repository housekeeping rather than padded into the product changelog.

This range includes focused automated coverage for the areas changed in 2.5.10, including player-config recovery, YouTube identity handling, lyrics parsing and translation, Quick Settings playback state, lyrics timing, ReplayGain selection and DSP, Technical Audio Info, followed-release policy, Playlist PRO logic, live-radio transport, Parametric EQ and AutoEQ import, personalized search, Settings search, Speed Dial, HQ audio resolution, JioSaavn parsing and matching, localization and queue identity behavior.

The Android release pipeline publishes only from `main`. It validates the version and release-note wiring, runs release lint, builds the signed release APK, verifies the APK version and canonical signing certificate, generates a SHA-256 checksum, creates the GitHub release from this curated note and downloads the published assets again for verification. The F-Droid path then builds and checks its separate reproducible variant.

The JioSaavn HQ and first-play path were also exercised on a physical Android phone during development. This version-bump commit itself does not claim a fresh manual pass of every OEM, Bluetooth device, Android Auto setup or background-restriction combination.

## Versioning

- Version name: `2.5.10`
- Version code: `2051000`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required for this version bump.

Existing supported favorites, playlists, queues, listening history, local-library data, audio settings and other persisted preferences continue through Levyra's existing storage and migration paths. New audio and personalization settings use the same backup and preference infrastructure where supported.

GitHub users can update from the signed APK attached to the release. Third-party stores and repositories may publish the update on their own schedule.

## Final note

2.5.10 is a fairly dense update, but the direction is straightforward: playback should recover more gracefully, audio controls should tell the truth about what they are doing, and the app should get out of your way faster.

A lot of the work here is the kind you only notice when it is missing. That is exactly the point.
