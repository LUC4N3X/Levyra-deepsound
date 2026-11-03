# Levyra 3.0.7

## Highlights

A small fix release. A few of you reported things that were broken or missing, so this one is mostly about sorting those out.

- Smart resume now shows how far it rewinds. The amount used to get cut off on smaller screens and in some languages, and now it uses your language's own unit for seconds.
- In the Qobuz skin, the Home and Our picks tabs actually change what's on screen, even when you've already scrolled down.
- Pinned items and favourite radio stations show up on the Qobuz and Atelier home screens again.
- Live Radio and Levyra Jam can be opened from Explore in the Qobuz and Atelier skins. Before, there was no way to reach them there.
- Favourite station artwork no longer goes missing on custom home layouts.
- In Mood Journey, tapping a song in the list plays from that song, and Play opens the player instead of seemingly doing nothing.
- The Mood Journey slider that used to say Brightness is now called Tone, with a short line explaining what it changes. It only picks songs; it never changes speed or pitch.
- Levyra Jam now follows the look of whatever skin you're using.

## Validation

These notes cover the fixes in this version. They describe the code; they don't mean the 3.0.7 APK has already passed release testing.

I checked the Smart resume labels, the Qobuz tabs, pinned items and favourite radio on both skins, and the Live Radio and Jam entry points on an emulator. The new and existing unit tests for these areas pass. I didn't test on a physical phone, in right-to-left languages, with very large fonts or on Android Auto. Those are unverified, not passed.

The GitHub release workflow still has to pass the release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the files. Treat the GitHub release and the downloadable APK as available only once that workflow succeeds.

## Versioning

- Version name: `3.0.7`
- Version code: `3000700`

This is an Android release. Levyra Desktop is versioned and released separately; this update doesn't change the Windows app.

## Upgrade notes

No manual migration is expected. Your library, playlists, favorites, downloads, listening history, settings, skin and player style stay as they are. A saved Mood Journey keeps its curve and length.

Catalogs, regional releases, lyrics, search, video, podcasts and lossless sources depend in part on external services: results and availability can vary by region and over time.

Once the publishing workflow finishes successfully, the signed Android APK and its checksum will be on the GitHub release page. F-Droid and other channels may update later.

## Final note

Thanks to everyone who took the time to write down what wasn't working. Keep it coming, it's how these get fixed.
