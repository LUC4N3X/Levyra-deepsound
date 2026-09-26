# Levyra 2.5.12

## Highlights

2.5.12 is a pretty big player update, but most of it shows up in simple ways when you actually use Levyra.

You can flip the player into synced lyrics without leaving the artwork, choose the video quality instead of being stuck on the first stream YouTube gives back, open playlists from Search before deciding what to do with them, and swipe around the player without a tiny accidental flick skipping the track.

The queue and discovery side got some attention too. Smart Orbit has a better memory of what you listen to, radio is less likely to hand you another upload of the same recording, and removing an auto-added song now actually means “don’t give me this again” for the rest of that queue session. There is also a new multi-link flow for people who share or paste several YouTube links at once.

## ✦ Lyrics right on the player

The artwork card can now turn into a compact synced-lyrics view. Swipe across the title and artist area to bring it in, swipe back to return to the cover, and the normal full Lyrics screen is still there when you want it.

The compact card follows the same playback clock and lyric timing Levyra already uses. Tap a line to seek to it, or scroll by hand and auto-follow gives you a moment before taking over again.

It is a small interaction, but it makes checking a line of lyrics feel much less like leaving the player to open another screen.

## ✦ Video quality is finally a real choice

Video mode now exposes the quality ladder that YouTube actually makes available instead of collapsing to the 360p fallback in cases where adaptive streams exist.

Split audio and video streams are handled correctly, including the black-screen case that could happen when the merged source lost the active video identity. Quality changes also keep their own short grace period so switching resolution does not get mistaken for a playback failure.

If a video only has limited qualities, Levyra still shows only what is genuinely available. It does not invent a 1080p option where the source does not provide one.

## ✦ A queue that takes the hint

Continuous radio now does a better job of recognising alternate uploads of a song already in the queue. Official video, lyric, audio, remaster, Topic and VEVO-style variants no longer keep bouncing the same recording back at you, while real remixes, live versions and different artists stay separate.

There is also a new anti-boomerang rule for radio-added tracks. Remove one and Levyra remembers that choice for the current queue space, so a later refill does not immediately put the same recording back. Adding it yourself or choosing Play next clears that temporary block.

Track swipes are less trigger-happy as well. A fast little movement that was really just a tap no longer counts as Next or Previous unless the gesture actually travelled in that direction.

And if you would rather keep background preparation to a minimum, **Preload next track** can now be turned off. It stays on by default.

## ✦ Several links in, one clean result

Share or paste a block of text containing multiple YouTube or YouTube Music links and Levyra can now collect them as one request.

Links are normalised and deduplicated, resolution is bounded instead of firing everything at once, and the result sheet tells you what was found, duplicated or not recognised. From there the tracks can be played, queued, downloaded or saved as a playlist.

Single links still use the normal single-item flow. Bulk mode only steps in when there is actually more than one distinct supported link.

## ✦ Smart Orbit 2.0

Smart Orbit now builds a small local discovery pool from tracks related to music you have genuinely listened to.

Candidates connected to several of your listened tracks gain more weight, while music you already heard, artists you tend to skip early, excluded artists, explicit dislikes and tracks you removed from radio are held back. The same local co-occurrence signal also helps radio and Similar Songs choose better candidates.

The important part has not changed: this listening model stays on the device. It does not need a new account or a new tracking service.

## ✦ Playlists from Search behave like playlists

Tapping a playlist in Search now opens its details instead of immediately starting the whole thing.

You can look through the tracks first, then choose Play, Shuffle or Download. Longer playlists continue loading as you reach the end, and the result shows the real track count rather than treating a view count as if it were the number of songs.

It sounds obvious when written down. It feels much better in use.

## ✦ Visuals that can get out of the way

There is a new **Full / Auto / Smooth** visual performance setting under Design.

Full keeps the complete motion treatment and remains the default. Auto scales the decorative work back when the device reports low memory or battery saver. Smooth removes heavier blur, depth transitions and decorative loops without touching playback, providers or the actual feature set.

The mini player transitions were cleaned up, the seekbar wave keeps a steadier shape as progress moves, and the active crossfade curve is now visible instead of being an invisible audio setting.

## ✦ Experimental network compatibility

2.5.12 adds an experimental ByeDPI path for YouTube together with an optional US region profile.

When enabled, Levyra can route YouTube traffic through the local desync tunnel while resolving destinations through its DoH chain. HTTPS still keeps the original hostname for certificate verification, and JioSaavn routing is left alone.

This is deliberately marked experimental. It is there for restrictive networks where the normal YouTube path does not work reliably, not as something everyone should switch on. The native ByeDPI component ships in the upstream build and is excluded from the F-Droid variant.

## ✦ A few other things worth mentioning

- Lyrics provider priority is now respected more consistently, including cached fallbacks.
- A resume-playback shortcut can restore the queue before playback continues.
- Search gets rotating taste-based prompts built from local listening signals.
- Radio duplicate detection was tightened without collapsing real live cuts or remixes.
- Queue prefetch now respects the new preload setting all the way down to the playback service.
- The player, video and network changes picked up a long list of smaller review fixes along the way.

## Validation

The 2.5.12 release range contains 90 commits before the version wiring changes. The product notes above are based on the complete GitHub comparison and the current `main` implementation, with badge refreshes, documentation-only changes, player-config syncs and other maintenance left out of the feature story.

The code in this release adds focused regression coverage around Smart Orbit, bulk link capture, radio deduplication, queue prefetch, player gestures, video quality, lyrics provider ordering, visual performance behavior and the restricted-network path.

The GitHub release is published only from `main`. Before an APK can become the 2.5.12 release, the release workflow validates the version and these notes, runs `lintRelease`, builds the signed release APK, checks the APK version and signing certificate, writes a SHA-256 checksum, publishes the GitHub release and downloads the published assets again for verification.

This version bump does not claim a fresh manual pass across every Android device, Android Auto setup, Bluetooth route or OEM background restriction.

## Versioning

- Version name: `2.5.12`
- Version code: `2051200`

This is an Android release. Levyra Desktop keeps its own independent version line.

## Upgrade notes

No manual migration is required.

The new player, discovery and network options pick up safe defaults automatically. Existing local library data, favorites, playlists, queues, listening history and settings do not need to be recreated for this update.

The experimental ByeDPI path is opt-in. If normal playback already works on your network, there is no reason to enable it.

GitHub users can update from the signed APK attached to this release. F-Droid and other repositories publish on their own schedule.

## Final note

2.5.12 makes Levyra feel less eager to get in your way.

Fewer accidental skips. Fewer duplicate versions sneaking back into radio. Lyrics closer to the song. Video controls that behave like actual controls. Search playlists you can inspect before pressing play.

That is the kind of polish this release is about.
