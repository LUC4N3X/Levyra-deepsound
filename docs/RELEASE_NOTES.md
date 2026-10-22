# Levyra 3.0.0

## Highlights

This is the biggest update Levyra has had so far, which is why it's called 3.0.0.

The headline is skins. Until now you could change colours, player styles and a lot of small details, but the app always kept the same basic shape. Skins change that. Each one restyles Home, Explore, Library, Settings, the mini player and the full player, so switching skin feels like switching to a different app that still has all your music in it.

There's also personalised discovery on Home, a much better radio section, smarter matching when you import playlists from other services, and a player that handles Canvas and video more gracefully.

## Seven new skins

You'll find them under Settings → Design → Skin. Levyra Default is still the default, and your customisations are kept when you switch back and forth.

- Nocturne 78 brings the warmth of a vintage hi-fi.
- Prism pairs deep black with raspberry accents.
- Torque goes for bold graphite shapes and sharp angles.
- Ivory is soft and bright, with a dark mode of its own.
- Spotify Style, Apple Music Style and YouTube Music Style follow the layouts people already know from those apps.

## The player

Velvet, in the Default skin, now uses the same player as Apple Music Style: same artwork, controls, spacing and motion, in portrait, in landscape and on the inner screen of a foldable.

When a song has a vertical Canvas, it now plays full screen behind the controls on phones. Before, this only worked when the Canvas came with its size already known, which wasn't often. Songs without a Canvas keep the classic artwork view. Under the seek bar you'll now see the format that is actually playing, such as AAC and its bitrate, or Dolby Atmos and Lossless when the stream really is one of those.

Video mode follows the same layout too. Switching from video back to the song now returns to the audio source the song was using before, for example JioSaavn, instead of looking for a new one. When Levyra looks for a music video, it now prefers the official one over fan uploads with lyrics or translations on top.

## Home, radio and search

Home can now suggest new music around your taste in every Default home style, and it can greet you by name. Radio supports your own stream URLs, paginated search and favourite stations on Home. Search can browse an artist's full discography with paging, and your YouTube Music library can show up on Home.

## Imports and matching

Playlist imports and song matching across providers have had a lot of work, including fixes that came straight from real Spotify playlists that didn't import cleanly before.

## Smaller things

The launcher icon has a refined background, Settings hides the bottom navigation while keeping the mini player, the compact Settings cards keep their titles readable, and the Italian Android Auto plural forms are complete.

## Validation

This release gathers the Android changes merged after 2.6.10, plus the player work in this release's pull request.

For the player changes, focused unit tests passed locally and the debug build was checked by hand on a foldable emulator (Android 37): portrait on the cover screen, the unfolded inner screen in both orientations, full-screen Canvas, song to video to song with JioSaavn audio, and a full scroll of Home. Video frames could not be checked on the emulator because its H.264 decoder fails, so video playback on a real phone still needs a check.

A full local release build, a physical-device regression run and final signing verification have not been done while writing these notes. The GitHub release workflow still has to pass release lint, build and sign the APK, check the version and certificate, create the SHA-256 checksum and verify the published files.

## Versioning

- Version name: `3.0.0`
- Version code: `3000000`

This is an Android release. The separately versioned Windows app is not part of it.

## Upgrade notes

You don't need to do anything special to update. Your library, downloads, listening history, playlists and appearance settings keep using their normal storage. Levyra Default stays selected unless you pick a skin, and the player style you chose before is still yours.

Some search, video, Canvas, credits and provider features depend on external services and on what your device can decode.

The signed APK and checksum will appear on the GitHub release page once the publishing workflow succeeds. F-Droid and other channels may update on their own schedule.

## Final note

Skins started as a small experiment and turned into the thing I've enjoyed building most. I hope at least one of them feels like home to you. If something looks off or a song plays the wrong way, tell me. Most of what's in this release started with one of your reports.

Thanks for sticking with Levyra.
