# Levyra 2.5.9

## Highlights

2.5.9 is one of those releases where a lot changed under the hood, but the goal is simple: make Levyra feel better to use every day.

Queues are no longer something you lose when you move between listening sessions. Local music is treated like a real part of the library instead of a side feature. Search starts returning useful results sooner. The player has more ways to look and behave without splitting into separate playback systems. There is also a fair amount of work here that you will hopefully never notice directly: safer audio fallback, better recovery paths, stricter queue handling and more defensive playback code.

## Queue Spaces and Local Library 2.0

Levyra can now keep multiple persistent Queue Spaces. You can move between them without throwing away what was already queued, and empty queues are preserved instead of silently disappearing.

Local Library 2.0 is a much larger pass over local music support. Levyra now keeps a proper local catalog, reads and reconciles metadata more carefully, handles local artwork, and has stronger support for editing and preserving tags. Local tracks fit into the same library and playback flow more naturally instead of feeling bolted on.

## Player Deck and Playlist Studio

The player now has Player Deck: a single place to switch between Levyra's player presentations with live previews. Editorial and Pulse join the existing layouts while still using the same playback state, queue, actions, lyrics and gestures underneath.

Playlist Studio is the new full-screen playlist editor. It brings cover previews, generated artwork, local-library search, drag reorder, undo, playlist stats and a safer save flow. Playlist edits are committed together so a failed save does not leave half of the playlist changed.

## Faster search and better discovery flow

Search has been rebuilt around a dedicated Levyra search engine. Local matching, request pooling, memoization and progressive merging let the screen publish useful results earlier instead of waiting for every source to finish.

The goal here is not to make search look busier. It is to make it feel immediate, especially when the answer is already in your local library or cache.

## Audio and playback

2.5.9 adds an optional native audio path for upstream builds using Oboe/AAudio. It stays opt-in and falls back to the normal Android audio path if the native output cannot be used safely.

There is also an FFmpeg decoder fallback for audio formats that the device decoder fails to handle. Levyra can retry through FFmpeg instead of simply giving up on the track.

High-quality audio routing has been tightened as well. Provider failures are isolated more cleanly, stale mappings are handled more carefully and the fallback path is less likely to get stuck retrying the same bad source.

## Sleep Timer, WaveSeek and output controls

Sleep Timer 2.0 adds a cleaner timer flow with optional fade-out and scheduling support.

WaveSeek adds a waveform-backed seek experience built around captured playback envelopes rather than a decorative fake waveform.

The new Output Hub also gives audio routing and output-related controls a clearer home instead of scattering them around the player and settings.

## Jam, Ambient and Theme Studio

Levyra Jam now has real host moderation. Guests can wait for approval, hosts can accept or reject them, remove or block participants, lock a session and keep track of who added music to the queue.

Ambient has grown into a proper standby surface with multiple layouts, optional clock and playback information, OLED-friendly presentation and artwork-driven accents.

Theme Studio moves theme selection into its own screen with live preview and a larger preset/accent system, while keeping existing saved themes compatible.

## Lyrics, artwork and smaller improvements

Lyrics handling gained broader romanization support and latency profiles, plus more work around sharing and rendering lyric cards.

Motion Artwork received more defensive source handling and priority behavior. Player visuals, artwork transitions, haptics and motion tokens were also cleaned up so the different player surfaces behave more consistently.

There are many smaller fixes across Android Auto, backups, playlists, downloads, localization, radio, artwork caching and playback recovery. They are not glamorous individually, but together they remove quite a few rough edges.

## Validation

The 2.5.9 release uses Levyra's normal signed Android release pipeline. The workflow validates the release metadata, runs release lint, builds the signed APK, checks the APK version and signing certificate, generates the SHA-256 checksum and verifies the published artifact.

Feature work in this release also includes focused automated coverage for persistent queues, local-library reconciliation and tags, search behavior, Playlist Studio recovery, Jam moderation, WaveSeek policies, native audio fallback, Motion Artwork and backup/restore behavior.

As usual, automated coverage is not the same thing as testing every phone, ROM, Bluetooth device or Android Auto setup in existence. If something behaves differently on your device, opening an issue with the device and Android version is genuinely useful.

## Versioning

- Version name: `2.5.9`
- Version code: `2050900`

This is an Android release. Levyra Desktop keeps its own independent release line.

## Upgrade notes

No manual migration is required.

Existing favorites, playlists, queue state, listening history, settings and other supported local data continue through Levyra's normal migrations and backup/restore paths.

GitHub users can update with the signed APK from the release page. Third-party stores and repositories publish on their own schedules.

## Final note

2.5.9 is mostly about making the pieces Levyra already has work together more naturally: online and local music, queues that survive, a player that can change shape without changing behavior, and playback that has more than one way to recover when Android or a provider gets in the way.

That is the direction I want Levyra to keep moving in.
