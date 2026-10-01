# Levyra 2.6.0

## Highlights

2.6.0 is a big Levyra update, but the point is not to make the app feel busier. It is the opposite: more of the things you actually use are easier to reach, easier to understand, and harder to break.

Home has been reworked around denser music discovery and a much more personal **La tua orbita**. Playlist import is now a proper flow instead of a small utility. Lyrics gained sharing, landscape layouts and a stronger parser. The player adapts properly when the phone is sideways, system media controls know the real Like/Shuffle/Repeat state, and audio routing no longer has to start outside Levyra.

There is quite a lot underneath that too: Mix Lab, Enhanced Audio, safer rapid skipping, smarter artwork requests, adaptive buffering and a long list of fixes around queues, playback state and local music.

## ✦ Home feels more like your music

Home has been tightened up so it shows more music without turning into a wall of tiny cards. The spacing is denser, the artwork has a cleaner shape, and the important shelves have more room to breathe where it matters.

**La tua orbita** is now a real personal quick-pick area: compact 3×3 artwork pages on phones, multiple pages side by side on wider screens, with the listener name and an optional profile photo. It keeps the fast tap-to-play flow, while the current track and track actions still behave like the rest of Levyra.

The rest of Home moved in the same direction:

- the Radio hero can blend from the artwork into the page instead of sitting inside a separate card;
- the video shelf uses wide 16:9 music-video frames and keeps the video experience visually separate from ordinary song cards;
- Top 50 no longer makes you drag through a long row of countries: the current market opens a searchable selector and the choice is remembered;
- album and catalog-playlist pages now use artwork-led, palette-aware headers instead of feeling disconnected from the music they contain;
- closing the full player can take you back to the album, artist or playlist you came from instead of dropping you on Home.

There is also a global Liquid Glass interface system behind supported surfaces. It is a real persisted setting, not just a decorative switch: when the effect is disabled, Levyra avoids doing the unnecessary backdrop work as well.

## ✦ Bring playlists with you

Playlist import has been rebuilt into a full reviewable flow.

Levyra can detect supported playlist links, shared links and common playlist files without making you pick the source first. The importer understands public playlist data from YouTube, Spotify, Deezer, Apple Music, JioSaavn and Bandcamp, together with formats such as M3U/M3U8, PLS, XSPF, CSV/TSV, JSON and plain text.

Instead of silently accepting the first search result, imported tracks go through one matching engine. Clear matches can move straight through, uncertain ones are shown for review, and unresolved entries stay visible rather than being quietly replaced by the wrong recording. You can inspect alternatives, search manually and decide what should actually be saved.

Large imports can be resumed, and nothing is written as the final playlist until you confirm it. Existing playlist entries also get a **Change match** action, so a bad match can be replaced in place without rebuilding the playlist from scratch.

## ✦ Mix Lab gives you another way to discover music

Mix Lab is a new way to build a queue when you know the kind of session you want but not the exact songs.

You can shape a mix around familiarity versus discovery, recency, duration, track count, genres, artists and mood. Levyra builds the result from music and metadata it already knows, then lets you play it, shuffle it, add it to the queue or save it as a playlist.

The engine deliberately works around missing metadata instead of inventing it. It also limits repeated artists/albums and obvious duplicate recordings so the result behaves more like a mix than a search dump.

## ✦ Lyrics do more than sit on screen

Lyrics picked up several changes in 2.6.0.

There is now a dedicated sharing flow. Pick up to five contiguous lyric lines, preview the result and export a 4:5 image card using Artwork, Gradient or Minimal styling. Translation and romanization can be included when the selected lyrics actually contain them.

Turn a phone sideways and lyrics get a proper two-pane view: artwork, track information and playback controls on one side, lyrics on the other. It uses the same timing and lyric state as portrait mode rather than running a second lyrics system in parallel.

The parsing and matching side was tightened too. LRC, TTML, YRC, QRC and KRC handling is more defensive, word-timed text keeps spacing and CJK text intact, and a candidate with strong wrong-song evidence is no longer promoted just because it has richer timing data.

Local music benefits from the same work. The file tag editor can now change or remove embedded artwork and edit embedded lyrics for supported MP3, M4A/MP4 and FLAC files. The save path keeps the existing copy/edit/replace/rollback approach, and successful edits can be reflected back into the library and current playback metadata without forcing a track restart.

## ✦ The player now fits the way you hold the phone

Levyra can now follow the system rotation setting across the app instead of treating landscape like a special case reserved for one screen.

On a landscape phone, Now Playing becomes a side-by-side layout with the visual stage on one side and the controls on the other. Video uses a proper 16:9 fit, the bottom dock becomes much shorter, and major surfaces account for side cutouts and navigation bars. Portrait and larger-screen layouts keep their own behavior.

Canvas Immersive was cleaned up at the same time. Bright fullscreen Canvas clips get a readability layer behind the controls, the artwork-to-player dissolve is smoother, and opening or closing the player is easier to follow instead of snapping through the transition too quickly.

## ✦ Better control outside the player too

System media surfaces now follow Levyra's real playback state instead of showing static actions.

Like, Shuffle and Repeat are derived from the active queue and favorites state, then republished to the existing MediaSession when that state changes. Changes made from supported system surfaces can flow back into the running app as well.

Inside Now Playing there is also a new **Audio Route Center**. It shows the current local output and available routes, can request direct route changes on supported Android versions, and falls back safely when a route cannot be selected.

Queue behavior received a smaller but important fix: consecutive **Play next** batches now keep FIFO intent instead of reversing or scrambling the order as new batches are inserted.

## ✦ Audio gets smarter without pretending to be something it is not

2.6.0 adds **Levyra Enhanced Audio**, an optional real-time restoration stage for lossy playback. It looks for a clear high-frequency cutoff and, only when the signal gives enough evidence, reconstructs part of the missing upper band.

It does not relabel the source. AAC is still reported as AAC, and a lossy stream does not suddenly become “lossless” because enhancement is active. Full-band input, known lossless input, remote playback, CPU-protection states and uncertain detections can bypass the processor.

Playback buffering also gained an adaptive stability mode underneath the player. Normal conditions keep the normal buffering profile. Repeated real instability can request a more conservative profile at a track boundary, with hysteresis and cooldown instead of constantly changing the buffer while a song is playing.

## ✦ Fast skipping is much harder to confuse

A lot can finish in the background after you have already moved to another song: lyrics, motion artwork, source resolution, SponsorBlock data and high-quality audio lookup among them.

2.6.0 gives playback transitions a generation identity so late work from an older transition can be discarded before it lands on the new track. That matters most when you hit Next several times quickly or when the same recording appears more than once in a queue.

Artwork requests are more disciplined too. Small rows ask supported providers for smaller sources, cards use a middle tier, and large detail/player surfaces can still request the full artwork. The goal is simple: stop downloading a player-sized cover for a tiny list row.

Search also merges duplicate song results more carefully when two payloads describe the same recording with complementary metadata, instead of showing both just because one result is missing a duration or another field.

## Validation

These notes were written from the current Android source and the merged change set that makes up 2.6.0, with documentation-only work and repository presentation changes kept out of the product story.

The feature work in this release includes focused automated coverage around playlist import and matching, Mix Lab, lyrics parsing and sharing, local metadata editing, playback generations, artwork resolution, adaptive buffering, Enhanced Audio, system media actions, audio routing, landscape layout behavior, Liquid Glass policy and queue ordering. Individual merged changes also carry their own CI and manual-test evidence; those checks are not presented here as if one single device ran every 2.6.0 path end to end.

The GitHub Android release is gated by the existing `main` release workflow. Before 2.6.0 can be published, that workflow validates the version and these notes, runs release lint, builds the signed release APK, checks the APK version and signing certificate, writes a SHA-256 checksum, publishes the GitHub release and downloads the published assets again for verification. If those release gates fail, the release is not considered published.

No claim is made here that every Android device, Bluetooth/USB route, Android Auto setup, OEM background policy or visual combination received a fresh physical-device pass on the final 2.6.0 artifact.

## Versioning

- Version name: `2.6.0`
- Version code: `2060000`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

Levyra includes the Room migration needed by the newer playlist-entry model. Existing playlist rows are copied into the new entry-based structure automatically, allowing repeated source entries and the newer import/match workflow without asking the user to recreate playlists.

The new interface and audio features keep their state through the existing settings system where applicable. Features such as playlist import, Mix Lab, lyric sharing and landscape layouts do not require a separate account or setup step before they can be opened.

GitHub users can update from the signed APK attached to this release once publication completes. F-Droid and other repositories follow their own build and publishing schedules.

## Final note

2.6.0 is less about one headline feature and more about Levyra growing up in several places at once.

Home is quicker to read. Playlists are much easier to bring in. Lyrics are useful beyond the lyrics screen. The player makes sense sideways. System controls and audio outputs feel connected to the app instead of bolted on. And underneath all of that, a lot of work went into making sure stale background work, awkward queues and unstable connections have fewer chances to interrupt the song.

That is the version: more capable, but also a little calmer to use.
