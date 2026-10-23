# Levyra 3.0.1

## Highlights

A smaller release after 3.0.0, mostly made of things you told me about. Home gets a calmer Discover card, the Spotify Style and YouTube Music Style skins finally have proper mood and genre pages, and thirteen languages that were quietly showing English almost everywhere now speak their own language.

## Discover for you

The Discover card on Home used to sit right under My soundtrack and fight it for attention, with its own blue button, two fake save and skip bubbles you couldn't actually tap, and an animation that never stopped.

It's been redrawn to match the rest of Home. The colours now come from the cover at the front, the reason a song was picked (a new release, an artist you follow) sits in a small label on that cover, and the covers turn like a deck a couple of times and then settle down. If you've turned animations off, nothing moves.

It also moved further down, after Your orbit. That puts the genre chips (New releases and the rest) back under My soundtrack, where they used to be.

## Mood and genre pages in two skins

Opening a mood or genre in the Spotify Style skin now shows the page that skin is known for: a coloured header, a big title and rows of playlist covers grouped by theme, with the artists underneath.

The YouTube Music Style skin gets its own version, with section titles you can open to see every playlist in that group. Before this release, tapping a mood in that skin loaded the songs in the background but never actually opened a page.

If a category can't be loaded, you still get the usual list of songs instead of an empty screen.

## Languages

Slovak, Croatian, Serbian, Albanian, Bulgarian, Hungarian, Norwegian, Catalan, Kazakh, Persian, Traditional Chinese, Bengali and Malay only had their welcome screens and tab names translated. Settings, the player, your library, search and downloads were all in English. Those parts are now translated in all thirteen. A few feature screens, like the audio tools and playlist import, are still in English for these languages and will follow.

A Russian user also noticed that the "LEVYRA is up to date" message stayed in Italian whatever language you picked. That message, the "new version available" one and the error you see when the check fails now follow your language in all 41 languages Levyra supports.

## Also in this release

These were fixed on the main branch after 3.0.0 and ship with this version:

- Albums that YouTube Music labels as an EP or a single open correctly, and artist pages show both the artist's own playlists and the ones they appear on.
- In the YouTube Music Style skin, live radio shows its play and pause button again.
- Playback from Explore is more reliable, and the Apple Music Style skin has editorial genre pages.
- Playlist imports find the right version of a song more often, by looking at the original album, ISRC codes and matching lengths.
- Android Auto search results can be cancelled cleanly and no longer reset the queue you were listening to.

## Validation

Focused unit tests passed locally for the new mood and genre page parser and for localization, and the debug build was installed on a Samsung Galaxy S25+ over wireless debugging. On that phone I checked the new Home order and the Discover card layout. The turning animation, the two new mood and genre pages, the light theme, large text and TalkBack still need a proper look on a device.

The translations were written and reviewed by me, not by native speakers, so corrections from people who speak these languages are very welcome.

A full local release build, a physical-device regression run and final signing verification have not been done while writing these notes. The GitHub release workflow still has to pass release lint, build and sign the APK, check the version and certificate, create the SHA-256 checksum and verify the published files.

## Versioning

- Version name: `3.0.1`
- Version code: `3000100`

This is an Android release. The separately versioned Windows app is not part of it.

## Upgrade notes

Nothing special to do. Your library, downloads, history, playlists and settings stay where they are, and your skin and player style don't change.

Mood and genre pages, search and video depend on external services, so what you see can vary by region.

The signed APK and checksum will appear on the GitHub release page once the publishing workflow succeeds. F-Droid and other channels may update on their own schedule.

## Final note

Most of this release came from your messages, including the one about the update toast in the wrong language. Keep them coming, and if your language still shows English somewhere, tell me where.

Thanks for sticking with Levyra.
