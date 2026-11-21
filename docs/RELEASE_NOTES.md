# Levyra 3.2.2

## Highlights

I've spent most of this update on the parts of Levyra you see every day. The Default Library has been rebuilt around your music, Home has a new look, and Search is much better at finding the right recording of a song.

There's also a new way to discover trending music through Quick picks, support for importing radio station lists, and playlist import and export with ListenBrainz.

## ✦ Your Library, redesigned

The Default Library now opens with a dedicated Favorites card and a play button for your saved favorites. Recent listening and smart collections have their own cards, while listening history, playlists and albums are presented in shelves with their actual artwork.

The rest of the Library is organized into a two-column category grid instead of repeating the same navigation chips. A soft celeste accent, pastel details and stacked vinyl covers give the screen its own character without making every card a different color.

Smart collection details and category pages follow the same design. Empty album and artist sections now explain that there's nothing there yet instead of showing an unnecessary toolbar. The playlist creation screen and cover style selector have also been simplified to fit the new Library.

## ✦ Home feels more personal

My Soundtrack is now called Your sound today. You can enter your name in Home interface settings to personalize the greeting, and the card picks a curated artist for the day based on your app language. The artist you see is the artist the card plays.

The Default Home logo is now a little vinyl record. Tap it to reveal Rewind and Settings; it also spins while Home is loading. The soundtrack card has a larger, expressive play button that turns while the music is playing.

Podcasts, Moods and genres, and Samples are available from the Home browse rail. The translucent chips scroll all the way to the edges and behind the logo, and pulling down reveals the refresh indicator. Explore's new releases sit in a single horizontal shelf, and news stories without feed artwork can use the image from the article itself.

## ✦ Quick picks and better artwork

Quick picks now start with trending songs from the Apple Music storefront corresponding to your app language. Levyra uses the catalog duration and ISRC to look for matching official audio on YouTube, with stricter matching to avoid covers, live versions and different edits. Explicit tracks retain their explicit label.

Track covers, Search results, trending and genre artist portraits now prefer Apple artwork where the match is reliable, with Spotify and the existing sources available as fallbacks. Missing artwork can be retried instead of leaving an incomplete result permanently.

Artist pages also make better use of the portrait or animated canvas. The artwork is no longer stretched just to fill a tall header, the surrounding color follows the visible image, and artist motion can start without waiting for the Apple artist page to finish loading. A decoder retry issue that could delay the animated canvas has been fixed.

## ✦ Finding the right song

The Default Search screen has a new glass search field, updated suggestions and a Top result card styled like an artist's record sleeve. The record can play its track and pause or resume it without starting over.

Song results prefer genuine YouTube play counts where available. Matching has been tightened to avoid uploads from unrelated channels and swapping one edition of a track for another. Artist results can fill in missing playlists, longer artist names have more room, and the song menu gives you quick access to favorites, playlists and downloads.

Discover for you has an updated header and controls. Its ranking now favors proper cover artwork over video thumbnails, and Daily Discover can use playable regional chart songs to get started even on a fresh installation.

## ✦ Listening Insights and Settings

Listening Insights has a redesigned main card, clearer statistics tiles and a translucent scrolling header. Larger titles adapt to the available space, and the dark hero card keeps its text readable.

Settings categories are grouped into connected rows, with colored icons and a soft accent glow. Searching Settings can take you directly to the matching option and highlight it. Search is also available from the scrolled Settings header.

Theme, Home, Mini player, Navigation, Lyrics and Widget studios have more compact headers and simpler grouped controls. The visual changes also account for the reduced-motion setting.

## ✦ Bring your radio stations with you

The Add station sheet has been redesigned and now accepts M3U and M3U8 radio station lists of up to 1 MB. Imported stations can include their names, stream URLs and logos. Duplicate or invalid entries are skipped, station limits are respected, and the import reports how many entries were added or skipped.

This is for radio station lists, rather than HLS streaming manifests.

## ✦ ListenBrainz playlists

If you have a connected ListenBrainz account, you can browse your own playlists and the playlists generated for you, then import them into Levyra.

You can also export Levyra playlists to ListenBrainz. Tracks are matched using MusicBrainz recording IDs, and every match is sent in batches rather than stopping after the first 200 tracks. Matching can be cancelled. If an export fails after some tracks have already been saved, Levyra shows the partial result, the confirmed track count and a link to the playlist so you can still find it.

Playlist refreshes are discarded if you switch accounts while they are running.

## ✦ Other fixes

- Fixed the unreadable album track lists and playlist descriptions in Qobuz light mode.
- Fixed a crash when sharing content on Android 17.
- Corrected artist portraits in the daily soundtrack card, including Qobuz pages that previously returned a share image instead of the artist photo.
- Updated the YouTube extractor player configuration.
- Updated the project screenshots and README to reflect the current design.

## Validation

These notes cover the Android changes merged since v3.2.1. The Android publishing workflow validates the release version and notes, runs release lint, builds the signed APK, verifies its signature and generates a SHA-256 checksum before publishing. The workflow results for this version and additional manual device testing have not been verified at the time these notes were written.

## Versioning

- Version name: `3.2.2`
- Version code: `3020200`

Levyra Desktop keeps its own version and release channel.

## Upgrade notes

Install the update through the same channel you used for your current copy, either GitHub or F-Droid. You can find the new radio import option in Add station, and ListenBrainz playlist options with your connected account and playlist export controls.

## Final note

I wanted the Default skin to feel more consistent from one screen to the next without losing the details that make Levyra recognizable. The new Library is a big part of that. Thanks to everyone who keeps sending suggestions and reporting the little things that don't work quite right.
