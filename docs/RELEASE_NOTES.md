# Levyra 3.1.0

## Highlights

This one took longer than a point release usually does, so I bumped the minor version. I went through every skin and fixed the small things one by one, with most of the work going into Default and Glacier, a cleaner Search screen, and a home feed that feels less like a list of boxes.

- Lossless FLAC streaming and downloads. I tightened the matching for music videos, misspelled titles and featured artists, and every FLAC download is now checked against the recording it was matched to.
- Explicit badges are back where they belong, and the explicit and clean editions of a song no longer get mixed up.
- Player focus mode hides everything except the cover, the title and the controls. If the app crashes, it now keeps a local report you can read and share yourself. Nothing is sent anywhere.
- Levyra Jam sessions can be shared and joined by scanning a QR code.
- The Default skin got curated playlists in place of the old collections, a New albums and singles shelf, trending artists with their monthly listeners, and a light scroll motion on every horizontal row. Cards near the edge settle back a little and the artwork drifts a few pixels as you scroll. It turns itself off when animations are off or the phone is in a low-power mode.
- Glacier gained Most discussed tracks on Home, This week's releases in Explore, the same row motion tuned to its quieter style, and a touch of its blue in Search and Settings.
- Trending artists in every skin now follow the official music charts for your language, in chart order, with real monthly listeners shown in your language.
- Curated playlists include a New Release Mix. It opens with the first tracks right away and stays cached for a few hours, so reopening it is instant.
- Search is tidier. Sample cards show the full cover instead of a stretched crop, recent searches look cleaner, and removing a track from recent searches makes it disappear straight away.
- Trending songs for you is gone from Home in every skin.
- Closing the mini player now sticks. It no longer flashes back for a moment when you reopen the app.

## Validation

These notes describe the code that went into 3.1.0. They don't mean the APK has already passed release testing.

I tested the Default and Glacier home screens, Search, Explore, the comment cards and the row motion on an emulator and on a phone. The new unit tests for comment counts, artist audience parsing and curated playlist ordering pass, along with the existing localization tests. I didn't measure frame timing with real profiling, and I haven't checked the Prism, Torque, Ivory and Atelier skins on a real device. I also haven't tested right-to-left languages, very large fonts or Android Auto. Those are unverified, not passed.

The GitHub release workflow still has to pass the release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the files. Treat the GitHub release and the APK as available only once that workflow succeeds.

## Versioning

- Version name: `3.1.0`
- Version code: `3010000`

This is an Android release. Levyra Desktop is versioned and released separately, and this update doesn't touch the Windows app.

## Upgrade notes

No manual migration is needed. Your library, playlists, favorites, downloads, listening history, settings, skin and player style stay as they are.

The trending artists shelf and the curated playlists refresh on their own the first time you open Home after updating, so give them a moment. Charts, playlists, lyrics, comments and lossless audio depend partly on external services, so what you see can vary by region and over time.

Once the publishing workflow finishes, the signed APK and its checksum will be on the GitHub release page. F-Droid and other channels may take a little longer.

## Final note

The thread running through 3.1.0 is consistency. The same song, the same artist and the same scroll should behave the same way whichever skin you pick, and in whatever language you use the app. If something still feels off in yours, open an issue with the skin name, your language and a screenshot. That's usually enough for me to find the exact line of code behind it.
