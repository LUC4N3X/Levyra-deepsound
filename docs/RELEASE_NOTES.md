# Levyra 3.0.2

## Highlights

There's a lot more to this update than the version number suggests. Lyrics have a new way to come alive, your listening history has a story of its own, and the downloads you keep for offline days are easier to check and repair.

We've also spent time on the quieter annoyances: a lyric line that feels a little late, a video that asks too much of a modest phone, an album that won't open in your region, or a Back gesture that takes you somewhere unexpected. Those things matter every time you listen.

## ✦ Lyrics that feel closer to the music

There's now an Apple Music-style lyrics experience with a dedicated full-screen layout, word-level karaoke where timing is available, and controls that stay out of the way when you don't need them. It's the default lyrics view in the Apple Music Style skin, but you can choose it in the other skins too. The choice is saved in Lyrics Studio, so you don't have to set it again.

You can adjust automatic scrolling, original lyrics and romanization from the lyrics options. A song uploaded under a channel name instead of its real artist also has a better chance of finding the right lyrics: Levyra can read titles written as either “Song - Artist” or “Artist - Song”, without confusing a remaster label for an artist.

The existing lyrics view hasn't been left behind. During playback, synchronized lines follow the player's live position more closely. The default skin, Nocturne 78, Prism and Torque now have a softer artwork-coloured backdrop. Text is kept readable, decorative movement respects your animation setting, and blur is reduced on devices where it would be expensive.

## ✦ Your year, in your own music

Levyra Rewind turns the listening history already on your device into a sequence you can browse. There's time listened, favourite artists, tracks and albums, listening rhythms, an activity timeline and a closing poster you can share.

It uses the local listening records Levyra already keeps. If there isn't enough history for a meaningful recap, it doesn't pretend otherwise. You can change the year and move through the chapters at your own pace.

## ✦ Downloads you can trust

The new Download Health Center checks your offline library for missing, empty, unreadable or incomplete audio files, and makes problems visible instead of leaving you to discover them when you press Play.

You can scan the library, see which items need attention and confirm a repair for one download or several. Repair uses the existing download queue, with progress, retry and cancellation controls. If storage access is the problem, Levyra points you toward the relevant settings rather than promising a repair it can't perform.

## ✦ The everyday fixes

A few changes are less visible, but worth calling out:

- The sleep timer now lets you pick a duration between 5 minutes and 4 hours, in five-minute steps, alongside the familiar presets.
- Automatic video quality takes lower-memory phones and sustained dropped frames into account, rather than waiting for repeated buffering. Manually selected quality remains your choice.
- Exact artist searches can show a fuller verified discography. New-release albums and editorial categories do a better job using the right regional catalogue, with a fallback when a source can't provide the page.
- Moving between Library categories no longer leaves stale rows behind or lets a Library Back handler steal a gesture from an open player.
- When a prefetched high-quality track is already cached, Levyra can reuse that source without requiring a fresh network lookup. This is not a replacement for downloading music for offline use.

There's also a small detail you may notice before any of the above: the Android launcher now says “Levyra”, not “LEVYRA”.

## Validation

These notes cover changes already committed to main since the published 3.0.1 release. They describe the code and its intended behavior; they are not a claim that the 3.0.2 APK has already passed release testing.

New automated test coverage has been added for Rewind data, download health and repair, and the new lyrics behavior. During preparation of these notes, I did not run the repository's local fast or full AI quality gate, an Android release build, a physical-device regression pass, Android Auto checks or signing verification. Their status is unverified, not passed.

The GitHub release workflow must still pass its release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the checked files. A GitHub release and a downloadable APK should only be treated as available after that workflow succeeds.

## Versioning

- Version name: `3.0.2`
- Version code: `3000200`

This is an Android release. Levyra Desktop is versioned and released separately; this update does not change the Windows app.

## Upgrade notes

No manual migration is expected. Your existing music library, playlists, favourites, downloads, listening history, settings, selected skin and player style stay in place. Rewind builds its story from the listening history available locally, so its results will depend on what you've already played.

Music catalogues, regional releases, lyrics, search and streaming video rely in part on external services; results and availability can vary by region. Repairing a damaged download may require network access or storage permission.

Once the publishing workflow finishes successfully, the signed Android APK and its checksum will be on the GitHub release page. F-Droid and other distribution channels may update later.

## Final note

3.0.2 is about making Levyra feel more like your own place to listen. The lyrics can look the way you want. The hours you've spent with music have somewhere to live. And when something doesn't work, from a download to an album page, the app should give you a better way forward.

Thanks for listening, and for pointing out the little things worth fixing.
