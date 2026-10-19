# Levyra 2.6.10

## Highlights

This release is mostly about making Levyra feel better in everyday use. I've spent a lot of time on the player, the library and the small things that can get in your way when you just want to listen to music.

There's a new player style, more freedom to customise the colours, and a new background that reacts gently to your music. But I also wanted to give some attention to what happens behind the scenes: keeping queues intact, getting radio right and making searches and imports more reliable.

A lot of these changes started with your reports and suggestions. Thank you for taking the time to send them.

## The player feels a little more alive

Levyra Flow is the new default player style for people who haven't chosen one yet. Its artwork changes shape as music plays, with softer motion and a layout built around the cover. If you've already picked a player style, that choice stays yours.

The previous default is still here as Sleeve, now with its own vinyl-inspired look. Late Night has been refreshed too, and Velvet keeps its simpler play/pause button in both song and video mode. Cover swipes work more consistently across the different player designs, including round and moving artwork.

There's also a video decoder fallback to help when Android's decoder stops working during playback. It won't fix every unsupported video, but it gives Levyra another way to recover.

## Meet Reactive Aura

Reactive Aura is a new optional player background that uses colours from the current cover and moves gently with the music. You can choose Calm, Flow or Pulse and adjust the intensity.

I've tried to keep it subtle enough that it doesn't compete with the lyrics or controls. It also respects pause, silence, animation settings and the app lifecycle. Dynamic remains the default background, so nothing changes unless you turn Reactive Aura on.

## More ways to make Levyra yours

The advanced colour picker now lets you choose a shade visually instead of guessing a HEX code. You can still enter an exact colour or pick a preset, and the new picker works across the supported theme, player and navigation customisation screens.

Expressive Glass has had another pass too, with cleaner surfaces and more control over blur, transparency, tint and refraction.

Home now gives its different styles a more distinct My Soundtrack section, while Explore has neater layouts and better artwork fallbacks for Moods and genres. The Library has a clearer section overview, a more useful search across your music and a weekly listening card that looks at a consistent seven-day period. I've also fixed some clipped text, awkward spacing and navigation issues in the studios and settings.

## Song credits you can actually explore

Where the information can be reliably matched to the recording, Levyra now shows song credits in the player. Tap the short credit line or open the player menu to see performers, composers, producers and available release details.

The important part is that it doesn't pretend to know credits it can't verify. If the information isn't available or can't be matched confidently, Levyra won't fill in the gaps with guesses.

## Playback, queues and radio fixes

Several reports pointed to problems with resuming music. A paused queue could lose the information it needed to move to the next track, and resuming after a restart could sometimes switch to Your Orbit instead of returning to the original queue. Both paths have been corrected.

Start Radio is more varied now. It works from the original song instead of drifting into repeated copies or tracks from the same artist, and it handles YouTube Music watch-radio responses more reliably. Live radio also remembers the selected station separately from your music queue and makes the existing favourite action accessible in the player.

There's an orderly exit option in Settings, and the Search back button now behaves properly when the search field is empty.

## Better discovery and imports

Spotify playlist imports can now continue beyond the first roughly 100 tracks when Spotify makes more pages available. Song order and intentional duplicates are preserved, and Levyra tells you when an import is incomplete rather than silently claiming everything was imported.

JioSaavn searches can look a little deeper when the first results miss the right recording. Matching is also more careful with soundtrack credits and different spellings of song titles.

YouTube search continuation, channel releases and some SABR connection recovery cases have been updated. Explore can also look beyond Spotify for genre-card artwork, using Apple Music, YouTube Music, Deezer and Tidal as fallbacks when needed. These changes improve the chances of finding available content; they don't guarantee that every source will work all the time.

## Android Auto, audio output and smaller improvements

Android Auto browsing now uses translated labels instead of fixed Italian text, including appropriate track-count plurals. Its Home folders also have separate identifiers so their labels don't get mixed up in the cache.

The audio output panel has a new expandable device card and a clearer Material 3 volume slider. You can see alternative outputs without losing sight of the device you're using. This is a visual change: the existing audio routing and its feedback are preserved. Settings categories and Studio cards also have more room to show their descriptions without cutting them off.

I've also made a number of smaller UI, extractor and dependency fixes, plus some updates to the project's screenshots and presentation.

## Validation

This release gathers Android changes merged after 2.6.9, including PRs #973, #975, #978, #979, #982, #986, #987, #988, #989, #991, #993, #995, #999, #1002, #1003 and #1005. Supporting extractor, dependency, documentation and release-pipeline changes are included too.

Individual PRs contain their own reviews and focused test evidence. In particular, PR #1005 reports a successful debug build and Pixel 8 API 37 emulator checks. Those checks do not, by themselves, verify the final signed 2.6.10 APK.

The release version bump is being published directly to main. A complete local release build, physical-device regression run and final signing verification have not been performed as part of writing these notes. The existing GitHub release workflow must still pass release lint, build and sign the Android APK, check the version and certificate, generate the SHA-256 checksum and verify the published files.

## Versioning

- Version name: `2.6.10`
- Version code: `2061000`

This is an Android release. The separately versioned Windows app is not being updated here.

## Upgrade notes

You shouldn't need to do anything special when updating. Your existing library, downloads, listening history, playlists and saved appearance settings continue to use their normal storage.

Previously selected player styles stay selected. Reactive Aura is optional and the existing Dynamic background remains the default. Some search, video, credits and music-provider features still depend on the availability of external services and the capabilities of your device.

The signed APK and checksum will appear on the GitHub release page once the publishing workflow succeeds. F-Droid and other distribution channels may update on a different schedule.

## Final note

I know some of you come to Levyra for the design, and others just want it to play the next song without a fuss. I want both. There's still plenty I want to improve, but every report, translation, contribution and bit of feedback helps me decide what to work on next.

Thanks for sticking with Levyra and helping it grow.
