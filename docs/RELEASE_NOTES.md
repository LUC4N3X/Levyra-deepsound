# Levyra 3.1.2

## Highlights

A smaller release, but a personal one. You can now bring your own fonts into the lyrics, the music video shelf on the Default home finally shows what people are actually watching, and a handful of bugs that some of you hit every day are gone.

- Lyrics Studio can import TTF and OTF fonts straight from your phone and use them in every lyrics view.
- The Default home has a new Trending music videos shelf, built from the official music video chart for your country, with a new folding swipe motion.
- Search, Cyrillic playlists, non-Latin artist pages, the Atelier library and older Android versions each got a fix.

## ✦ Your fonts in your lyrics

This started from a request in the issue tracker: someone wanted bigger lyrics in a font they actually like reading. Lyrics Studio now has a My fonts section where you can import `.ttf` and `.otf` files from your phone, preview them, rename them, swap the file underneath, or delete them.

The font you pick is used everywhere lyrics appear: synced karaoke lines, translations, romanization, the Spotify-style view and the Apple-style view. If you never import a font, nothing changes, and every skin keeps its own lyrics typography.

- Variable fonts follow the weight you pick in the Studio, from Medium to Black.
- Static fonts still get bold and black weights, drawn by the system.
- Text sizes now go up to Huge, 50% larger than the standard size.
- Characters a font doesn't cover, such as Chinese, Japanese, Korean, Cyrillic or Arabic in a Latin-only font, fall back to the system fonts, so a line never turns into empty boxes.
- Each file is checked before it's accepted. Corrupt files, font collections and anything larger than 20 MB are rejected with a clear message. If a font goes missing later, lyrics quietly return to the built-in font.
- Imported fonts are included in your Levyra backup and come back when you restore it. A restore adds them to the fonts already on your phone rather than wiping them.

## ✦ Trending music videos

The video shelf on the Default home used to list new uploads, which often meant videos nobody was watching yet. It now follows the official most-played music video chart for the country you picked in Charts, up to 20 videos in chart order.

Every chart entry is matched to the official video on YouTube Music by title and artist. Remixes, live cuts, covers and lyric videos lose out unless the chart itself names that version, and fan uploads lose out to the artist's own channel. When the only copies are fan uploads, as often happens with full-EP films, Levyra skips that entry instead of playing something unofficial. If the chart can't be reached, the shelf falls back to new music videos as before.

Swiping the shelf also feels different. The card leaving the screen stays pinned to the edge and folds away behind a rounded mask while its artwork stays centered, and the next card grows in to take its place. Like the other row motions, it switches off when animations are off or the phone is saving power, and it mirrors itself in right-to-left languages.

## ✦ Fixes

- Search no longer crashes when the same recording appears more than once in recent searches.
- Playlists with Cyrillic titles play correctly again, and the queue recovers more reliably when a track fails to load.
- Pinned artists with non-Latin names open their artist page again, and the Glacier popular list shows every track instead of cutting off early.
- The Atelier library scrolls when you start the gesture on its header.
- The living artwork effect no longer tries to load its shader on Android versions older than 13, where it isn't supported.

## Validation

These notes describe the code that went into 3.1.2. They don't mean the APK has already passed release testing.

Unit tests for the font parser, the font library (import, rename, replace, delete, invalid and oversized files, backup and restore), the trending video chart parser and matcher, the fold motion maths, and the existing backup, vault and localization suites pass locally. On an API 37 emulator I tested font import with real TTF and OTF files, font selection, track changes, an app restart and a backup restore. On the same emulator I compared the trending shelf with the Italian music video chart and checked the fold motion by hand. I haven't tested the Apple and Spotify lyrics views with a custom font on a device, light theme, landscape, TalkBack, right-to-left languages or Android 8 and 9. Those are unverified, not passed.

The GitHub release workflow still has to pass the release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the files. Treat the GitHub release and the APK as available only once that workflow succeeds.

## Versioning

- Version name: `3.1.2`
- Version code: `3010200`

This is an Android release. Levyra Desktop is versioned and released separately, and this update doesn't touch the Windows app.

## Upgrade notes

No manual migration is needed. Your library, playlists, favorites, downloads, listening history, settings, skin and lyrics style stay as they are. Custom fonts start empty, so the lyrics look exactly like before until you import one.

The trending shelf needs a moment the first time you open Home after updating, while it reads the chart and finds the official videos. It refreshes when you change the chart country or the app language. The chart and the video matches come from external services, so what you see can vary by region and over time.

Once the publishing workflow finishes, the signed APK and its checksum will be on the GitHub release page. F-Droid and other channels may take a little longer.

## Final note

3.1.2 is about making Levyra feel like yours. The lyrics can look the way you like reading them, and the video shelf shows the videos people are actually watching. If a font won't import or a chart entry lands on the wrong video, open an issue with the file name or the song and your country, and I'll track it down.
