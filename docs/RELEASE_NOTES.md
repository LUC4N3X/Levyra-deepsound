# Levyra 2.5.11

## Highlights

2.5.11 is a smaller release than 2.5.10, and most of it comes from using Levyra every day and fixing what still felt wrong.

Live Radio got the biggest pass. Stations with dots, dashes, accents or apostrophes in their names are much easier to find, the station you actually typed now shows up first, and most stations finally have a real logo instead of two initials. Now Playing is cleaner too, and when a broadcaster marks a spot as an ad, Levyra now says so instead of showing it as a song.

Around that there is a new audio language preference for YouTube tracks, a player dock that gets out of the way while you scroll, favorites and multi-selection on artist pages, a steadier wavy seekbar and more accurate JioSaavn HQ matching.

## ✦ Live Radio you can actually search

Searching for a station used to be hit and miss. `181.fm salsa` returned nothing because the catalog calls it `181.FM - Salsa`, and a ranking bug pushed exact name matches below more popular stations that only matched a tag.

That is fixed. Search now ignores punctuation, accents and apostrophes, so `salsa clasica` finds `Salsa Clásica Éxitos` and `80s` finds `80's`. Exact names rank first, then names that start with what you typed, then names that contain every word. The same stream listed twice under slightly different names only appears once.

Playback is more careful as well:

- the stream URL policy and the ICY metadata request now apply to every redirect, not just the first request;
- a playlist file (`.pls`, `.m3u` and similar) is never picked as the fallback stream, because the player cannot open it;
- a short pause keeps the same connection, while a pause longer than 20 seconds reconnects to the live edge instead of playing old buffered audio;
- switching stations cancels any pending reconnect from the previous one.

## ✦ Cleaner Now Playing and honest ads

Some stations send messy metadata. Radio 105, for example, sends a whole record like `ARTIST~TITLE~~0~~131~date~date~Radio 105`. Levyra now turns that into `Artist - Title`, drops URL, UUID and hash fragments from titles, and stops showing the station's own name as if it were a song.

Stations that declare their ads in the stream metadata, like Virgin Radio Italia, now show "Advertisement" (localized in every supported language) while the spot is playing. Ads never end up as the current song title.

To be clear about the limit: some stations stitch the ad straight into the audio on the server and send no marker at all. 181.FM Salsa is one of them. Levyra cannot detect or skip those spots, and skipping a declared ad would not help either, because the server only sends the live audio once the ad time has passed. What Levyra does now is avoid opening extra sessions, so it does not trigger extra pre-rolls on its own.

## ✦ Real station logos

Radio Browser is missing a logo for a lot of stations, and many of the ones it has are broken links. Among the most voted Italian stations, most had no working logo at all.

When the catalog logo is missing or fails, Levyra now looks at the station's own website for its app icon, large icon, share image or favicon. It rejects SVG and non-image responses, caches the result, and does not keep retrying sites that are down. In testing, most of the popular Italian stations went from initials to their real logo.

In the full player the logo is now shown whole and centered on a card instead of being stretched across the screen, so a small icon no longer turns into a blurry wall of pixels. The same image reaches the notification and the lock screen. It is downloaded once through the same guarded connection used for radio, so the player never fetches station-provided image URLs through the general image loader.

## ✦ Audio language for YouTube tracks

Some YouTube videos carry several audio tracks: the original, dubbed versions and automatic AI dubs. Levyra now picks between them on purpose.

A new Audio language option in the audio settings lets you keep the original audio or prefer a specific language. Original human audio always beats an automatic dub, whatever the bitrate or codec. Streams in different languages no longer share a cache entry, so switching languages cannot play the wrong one back to you. JioSaavn HQ playback is not affected.

## ✦ A player dock that makes room

The mini player and the bottom tabs now share one dock. When you scroll down, it compacts in place: the artist line, the next and close buttons and the tab labels fold away. Scroll back up and it expands again.

On Android 12 and newer the dock uses a blurred glass surface over the page. Low-RAM devices, battery saver and older Android versions keep the solid background, and the dock does not compact while TalkBack touch exploration is on. Artist and album pages also get a soft color wash taken from their artwork, and the lyrics motion was refined.

## ✦ Artist pages, selection and the seekbar

Artist pages can now tell which of the artist's songs are already in your favorites, even when the same recording shows up with slightly different credits. Tracks can be multi-selected and added to or removed from your favorites in one go, and search results expose the same batch actions.

The wavy seekbar is steadier. It handles odd geometry edge cases without breaking, scrubbing state resets properly when the song changes, and the wave restarts with the new track instead of carrying over from the old one.

## ✦ More accurate JioSaavn HQ

JioSaavn HQ matching now uses the artist roles and release year that already come with search results, without extra requests. Composers, lyricists and actors listed as main artists no longer decide the lead-artist check, compilations of the same recording are no longer treated as a different song, and a solo version and a duet stay apart. Songs like Meri Aashiqui now get the HQ stream instead of falling back to YouTube.

Levyra also stopped fetching dislike estimates it does not need, and comments keep literal text like `<3` or `AT&T` intact.

## ✦ Smaller fixes

- Pressing Back from the expanded player now returns to Live Radio instead of closing it.
- Radio quality labels no longer show a literal `UNKNOWN` codec.
- Filipino and Estonian translations are complete.
- Some complex Home and listening-recap code was simplified without changing behavior.

## Validation

This release covers `v2.5.10` through `ceb6774`, 37 commits before the version commit. 21 of them are README badge refreshes, and several more are documentation updates and player-config syncs, so they are left out of the feature story above.

The commits in this range add focused unit tests for track multi-selection, liked-song artist matching, seekbar geometry, audio language ranking, caching and localization, and Live Radio search, metadata parsing, ad markers, artwork discovery and playlist handling.

The Live Radio changes were tested on a physical Android phone with a debug build during development: search, cold start, short and long pauses, station switching, declared ads, HLS and redirecting stations, station logos, the full player and the media notification. That was a development build, not the signed release APK.

The Android release pipeline publishes only from `main`. It checks the version and these release notes, runs release lint, builds the signed release APK, verifies the APK version and signing certificate, generates a SHA-256 checksum, publishes the GitHub release and downloads the assets again to verify them. The F-Droid path then builds its own reproducible variant.

This version commit does not claim a fresh manual pass on every device, Android Auto setup, Bluetooth route or background-restriction combination.

## Versioning

- Version name: `2.5.11`
- Version code: `2051100`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

Favorites, playlists, queues, listening history, local library data, radio favorites and recents, audio settings and other preferences carry over unchanged. The new audio language option starts on original audio.

GitHub users can update from the signed APK attached to this release. F-Droid and other repositories publish on their own schedule.

## Final note

2.5.11 is mostly about trust. When you search for a station it should be there, when a logo exists it should show up, and when something is an ad Levyra should not pretend it is a song.

Small things, fixed properly.
