# Levyra 3.1.3

## Highlights

This is a big one. Most of 3.1.3 is about finding music you don't know yet. Explore now has charts for your city, Home has a Top 50 for 13 genres, a new shelf brings you a few unfamiliar tracks every day, and there are many more curated playlists to browse.

The rest of the release covers things people asked for. Levyra can play a song you already have on your phone instead of streaming it. A floating sidebar gives you another way to move between tabs. Japanese lyrics now read correctly, and lyrics have three new sources. Settings are easier to find, and every skin got a round of fixes.

- City charts in Explore for every skin, and a Top 50 chart for 13 genres on Home.
- A daily discovery shelf built from radios of songs you play, without the artists you already know.
- More curated playlists, including Decades and For every moment.
- An option to play a matching song from your device before streaming it. It is off by default.
- An optional floating navigation sidebar. It is also off by default.
- Better romanization for Japanese lyrics, an online fallback for translation, and NetEase, Kugou and JioSaavn as lyrics sources.
- Movable and groupable poles in the Orbit equalizer, and six more music styles in Mood Journey.
- Settings now have 13 categories instead of 15, and choices open in a bottom sheet.

## Charts for your city and your genres

Explore has a new city charts row right after the Daily Top 100. Each city chart lists what people in that city play the most, and each skin draws the city cards in its own style. The Default skin shows a skyline, Spotify Style shows the city badge with the name as a caption, and Prism, Torque, Ivory, Mono, YouTube Music Style and Atelier each have their own card. In Qobuz Style the magazine Home gets a Top 100 rail and a city charts rail after the featured carousel. If no city charts are available for your region, the row stays hidden, and Levyra tries the load again later instead of showing an empty space.

Home also has a Genre hits rail. It holds a Top 50 chart for 13 genres, built from Last.fm listening data and matched to tracks you can play. Tap a genre to open its full chart. Genre pages show more as well. They include curator shelves from Apple Music and a short description of the genre, plus its main artists and albums from Last.fm, without repeating the sections that were already there.

## Something new every day

The daily discovery shelf sits under the curated playlists on Home. Levyra takes a few songs you have been playing, asks YouTube Music for a radio based on each one, and keeps only what is new to you. It filters out tracks you already know, artists you are familiar with and artists from your current session, and it keeps one alternative track per artist so the list doesn't collapse to a single name. The shelf shows up in Default, Nocturne 78, Prism, Torque, Ivory, Glacier and Atelier.

The provider-style skins get the same idea in their own form. YouTube Music Style and Spotify Style show a Discover for you section under your orbit, with swipeable cards, and Apple Music Style and Qobuz Style draw it in their own layout.

## More curated playlists

Curated playlists grew a lot. There are Decades and For every moment shelves, many more playlists taken from the moods and genres on YouTube Music, and a real New Release Mix. The extra shelves live inside Show all, so Home doesn't get longer. Every skin now has a Show all page, and it no longer freezes when it opens.

## Play what you already have

A new setting lets Levyra play a song from your device library before it looks for an online stream. It is off by default.

Levyra only uses a local file when it is sure it is the same recording. The title, artist and duration must match, and so must version tags such as live, remix, remaster, acoustic, clean or explicit. When album information is weak, the duration check gets stricter. If anything is uncertain, the song streams as before.

The song keeps its normal identity, so favorites, the queue and history still point to the online track and never store the local file path. If the file can't be read or fails during playback, Levyra switches to the online stream at the same position. Cast, video mode, live radio and SponsorBlock always use the online version. This request came from issue #1208.

## Lyrics

Japanese songs no longer have their kanji read as Chinese pinyin, and Levyra no longer romanizes lyrics written in your own script. When your phone has no on-device translator, translation and Japanese readings fall back to an online service.

NetEase, Kugou and JioSaavn are new lyrics sources. Lyrics that are synced by line now get estimated word timing, so the karaoke highlight moves through each line instead of jumping a whole line at once. The karaoke layers and the Apple-style highlight now line up. The Glacier and Apple Music Style skins can now switch away from the Apple-style viewer, and you can import lyrics fonts up to 128 MB, up from 20 MB.

## Orbit equalizer and Mood Journey

In the Orbit equalizer you can now move poles and group them, so a single drag raises several tones at once. Grouped poles add up. If you never move a pole, your current Orbit sounds exactly the same as before, because grouping only applies to poles you have moved.

Mood Journey lets you select several mood points and move them together, and it has six more music styles to choose from.

## Navigation and settings

You can turn on a floating navigation sidebar in settings. It sits on the left or right edge of the screen, you can drag it up and down, and dragging it across the screen moves it to the other side. Tap it to open the same tabs as the bottom bar. Levyra remembers its position and side, and your backup keeps them. It respects the system setting for disabled animations. This came from issue #1243.

Settings now have 13 categories instead of 15. Ambient and Lyrics moved under Player, Network moved under System, and Jam is now a row inside Player. A new Content category holds content country, audio language, taste profile and excluded artists. Skip silence, SponsorBlock and skip unrecoverable tracks moved to the Audio playback tab. Bluetooth resume and pause on mute moved to Smart Listening. Downloads keeps its technical options in an Advanced section.

Rows with several choices now show the current value and open a bottom sheet, instead of a block of chips. Long titles scroll instead of being cut off. The sleep timer is a tile now, and its sheet is translated into every app language.

## Skins

YouTube Music Style got the most work. It has genre hits, curated playlists, a moods page with colored tiles, an editorial Explore, a Top 100 and a most discussed tracks card. The player gets action pills and an up next handle. Home no longer jumps while you scroll, and the artist tabs pin in place without shifting the page.

Spotify Style has new albums, genre hits, Discover for you and a page for your top tracks of the year, with copy in your language. Two-digit ranks in popular tracks stay visible at large font sizes.

In Apple Music Style the album page takes its colors from the artwork. It has a round shuffle button, a play pill and a round save button, with a footer for the release date, track count, duration and copyright, followed by the related shelves from the Apple album page. Album tracks now take the album artist and artwork when a track doesn't list its own. Before, those tracks came out as Various Artists without a cover and could fail to play. Apple canvases open on their preview frame, so the player starts in its final colors and doesn't change them once the video begins. The transport controls are larger.

Atelier no longer shows Your orbit twice on Home. The libraries in Atelier, Qobuz Style, Spotify Style and YouTube Music Style now have an entry that opens Listening Insights, drawn in each skin's style. The Torque player fits the screen without scrolling, and Glacier has a mood section with playlist artwork.

On the Default skin, Your orbit fills up to three full pages, and the Home rails share one swipe motion. The Top 50 podium has a light parallax, and the trending artists row keeps a fixed height.

## Player

- The full player opens straight into its final layout with a short fade.
- Levyra asks for the highest refresh rate your display supports, except when battery saver is on.
- MP4 canvases show their first frame before the video starts.
- Motion artwork now updates when the queue moves to the next song while Levyra is in the background. Before, the old artwork could stay on screen after you came back (issue #1254).

## Fixes

- Flip and shake gestures in Smart Listening stopped working after the first use. They keep working now.
- Track numbers in charts and lists no longer wrap and lose their second digit.
- Bottom bar labels, Orbit pole labels and audio panel segments stay on one line in every language.
- Radio language pickers are sorted alphabetically, with names in the app language.
- Search finds artists whose YouTube Music name is written in another script. For example, with Russian as the app language, Yngwie Malmsteen now opens his full artist page with albums instead of a short profile with 12 songs.
- Downloads saved without embedded tags keep their cover in the library. This affects verified lossless FLAC files and files over 32 MB. Levyra stores the original cover address, HTTPS only, and uses it only when the file has no cover of its own.
- Tagging M4A downloads uses less memory.
- The crash report now opens a GitHub issue with the logs already filled in. When the logs are too long for a link, Levyra trims the middle and copies the full report to the clipboard.
- Artist pages no longer list playlists that can't be opened.
- Levyra Vault is translated into every supported language.

## Validation

The release covers the changes merged after 3.1.2, from `221d38c7a` up to `5ea903d49`. Each change came in through its own pull request with unit tests and the repository PR checks, and the release commit passed the quality gate.

## Versioning

- Version name: `3.1.3`
- Version code: `3010300`

This is an Android release. Levyra Desktop is versioned and released separately. Windows 1.3.0 shipped on its own during this cycle, and this update doesn't change the Windows app.

## Upgrade notes

No manual migration is needed. Your library, playlists, favorites, downloads, listening history, queue, lyrics style and skin stay as they are. Settings keep their values, but some of them now sit in a different category, so check the list above if you can't find one.

The app database moves from version 29 to 31. The update adds one field for canvas preview frames and one for the cover address of downloads. Downloads from before 3.1.3 start with an empty cover address, so an older download with no embedded cover still shows without one until you download it again. Older backups still restore, and those fields stay empty.

The local audio option and the floating sidebar stay off until you turn them on. Your Orbit equalizer sounds the same until you move a pole. After the update Levyra shows a short thank-you sheet once, and it won't appear again.

City charts, genre charts, discovery and curated playlists come from external services, so what you see depends on your region and changes over time. The first time you open Home and Explore after updating, these shelves can take a moment to load.

Once the publishing workflow finishes, the signed APK and its checksum will be on the GitHub release page. F-Droid and other channels may take a little longer.

## Final note

3.1.3 is mostly about discovery. There is more to find on Home and Explore, and less of it is music you have already heard. If a city chart is missing for you, a local file doesn't match when it should, or a lyrics source gets a song wrong, open an issue with the song, your country and the skin you use, and I'll look into it.
