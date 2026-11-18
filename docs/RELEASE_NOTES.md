# Levyra 3.2.0

## Highlights

3.2.0 is about two things: music that keeps playing without a connection, and a Default skin that looks finished. Downloads now show what is happening and in what order, the Offline tab is a real place to listen from, and Home, Radio, Library and the bottom bar got a full Material 3 Expressive pass.

Around that there is a lot from the last few weeks. Unison replaces Jam, playlist imports keep going in the background, artist pages can play and download a whole catalog, and album and artist pages were redesigned.

- Preload more of your queue, up to 50 tracks, with your own number if the presets don't fit.
- A download queue split into Preload, Downloads and Smart Offline, with a percentage for each track and for each group.
- Preload goes first, then your manual downloads, then Smart Offline.
- An Offline tab with a shuffle button for everything you saved, plus offline mixes, artists and playlists.
- A new Default Home, Radio and floating bottom bar, with search next to the bar.
- Unison, background playlist import and artist biographies in your language.
- Play and download an artist's whole catalog, cancel a running batch, and keep a sync file in your cloud storage.

## Downloads you can follow

Advanced preload now goes up to 50 upcoming tracks. Next to the presets there is a Custom option where you type any number from 1 to 50. If you had already picked a value, it stays as it was.

The download screen splits the queue into three groups. Preload shows the tracks Levyra is caching ahead of the current song. Downloads holds what you saved yourself. Smart Offline holds what Levyra saves for you. Each track shows its own percentage and each group shows how far the whole group has come. Long groups show the first few rows and expand when you tap Show all.

Downloads also follow an order now. The first three tracks of a preload window go first, so the next songs are ready in time. After that your manual downloads go ahead of Smart Offline. A preload never holds your downloads back for its whole window, only for those first three tracks. Album, playlist and artist batches that failed keep their retry and cancel buttons even when nothing else is downloading.

## The Offline tab

Library > Offline opens with a summary of what you have saved and how much space it takes, a play button and a shuffle button for everything. Below it are three rows you can tap to filter the list.

Offline mixes groups your downloads into Recently downloaded, Favorites, Most played and Smart Offline. Artists collects the downloads of each artist, and the same artist written in two ways counts once. Playlists shows every playlist with at least one downloaded song. Pick one and the list, play and shuffle use only those songs. Favorites and playlists match downloads by track first, then by title, artist and length, so a different recording with the same name doesn't slip in.

## A new Default skin

Home in the Default skin was redesigned around Material 3 Expressive. Every section header has its own shaped medallion and color. My soundtrack has an artist portrait that blends into the page, a radio label, a short animated wave and a shaped play button. The greeting row has Rewind and Settings as two shaped buttons, and Settings stays tappable while Home refreshes. Daily discovery cards fill the full width, the Top 50 uses small rank badges instead of large numbers, and Your orbit keeps a clean two-line caption on every tile.

The bottom bar floats above the content as a rounded bar with its labels. Search sits in its own round button joined to the bar, and both shrink together when you scroll. The selected tab has a filled indicator that changes shape when you press it. Long tab names in languages such as Russian or Serbian now get smaller to fit instead of being cut off.

Radio in the Default skin was rebuilt as well. The station on air sits in a single card with its artwork, details, a live wave and a joined Play, Favorite and Shuffle group. Categories are a grid of tiles, each with its own shape and color. The station chart is a grouped list with rank badges instead of large cards, and the duplicate search button at the top is gone.

The Library uses calmer colors. Favorites sits on a neutral card with the blue kept for its icon and button, and the soft glow behind Library, Explore and Settings now has a light shaped accent that fades out gradually as you scroll.

## Unison and background import

Jam is now called Unison. Playlist import runs in the background with a progress notification, so matching keeps going when you leave the app or the system closes it. The import screen shows a percentage and live match counts.

Artist biographies come from Apple Music in your language when available, then Wikipedia in your language, then the English versions.

## Artists and albums

Play and shuffle on an artist page now cover the artist's whole song catalog, and every artist menu can download all songs as one batch. If an album, playlist or artist download is still running, tapping download again asks whether to cancel it.

In the Default skin, album pages take the cover color, show the canvas at its own size and end with suggestions from the artist's discography, related albums and similar artists. Artist pages have a full-width hero, listener and subscriber counts and a latest release card. Other skins show the same suggestions in their own style.

Backup & restore can keep a sync file in Google Drive or another cloud app without signing in, and a second device can restore from it.

## Discover and search

Discover for you in the Default skin is a full-width card with the cover, the reason it was picked and the next picks stacked behind it. Skip, preview and save have labels, and a filters sheet sets the lens and how many new picks you get. Search in the Default skin has a new landing page, and the floating sidebar can now hide the bottom bar.

Decades go from the 1950s to the 2010s and each one opens a full page. Decade tiles sit after the moods in Search, and listening stats are easier to reach from Home.

## Fixes

- Pausing or seeking during a playback recovery no longer interrupts the song or loses its position.
- Apple catalog tracks, especially classical ones, play instead of failing with a generic error.
- The line behind the mini player is gone.
- Section titles and the Home greeting stay whole on narrow screens.
- The artist follow button, buffer chips and Tempo and Pitch controls stay usable with display zoom, and Tempo and Pitch have minus and plus buttons.
- Home scrolls more smoothly and the app starts with a leaner startup profile.
- Crash reports open in the browser even when no default browser is set.
- A guest in Unison can no longer replace the shared queue by playing downloads.

## Validation

The release covers the changes merged after 3.1.3, from `6a4f777a8` up to the current main, plus this pull request. Each change came in through its own pull request with unit tests and the repository PR checks.

## Versioning

- Version name: `3.2.0`
- Version code: `3020000`

This is an Android release. Levyra Desktop is versioned and released separately. The Linux build of Levyra Desktop shipped on its own during this cycle, and this update doesn't change the desktop apps.

## Upgrade notes

No manual migration is needed. Your library, playlists, favorites, downloads, listening history, queue, lyrics style, skin and settings stay as they are. The database version doesn't change. A preload value you picked before stays selected, and the new Custom option only appears when you choose it.

Once the publishing workflow finishes, the signed APK and its checksum will be on the GitHub release page. F-Droid and other channels may take a little longer.

## Final note

This release started from a list of requests on 4PDA about preload, the download queue and the Offline tab, and grew into a full pass on the Default skin. If a download sits in the wrong group, an offline mix picks the wrong songs, or a label is cut off in your language, open an issue with your skin and language and I'll look into it.
