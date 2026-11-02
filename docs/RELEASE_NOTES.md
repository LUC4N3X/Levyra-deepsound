# Levyra 3.0.6

## Highlights

This one got big. I meant to ship a small update after 3.0.5 and then a week went by and there were podcasts, a new equalizer, two skins, a new player deck and a pile of smart listening stuff sitting on main. So here it all is.

If you only read one section, read the Orbit one. It's the thing I'm happiest with in this release, and it's the first time the equalizer feels like something you play with instead of something you configure.

## ✦ Orbit, a new way to shape the sound

In the audio panel the equalizer now has a third tab next to Graphic EQ and Parametric EQ: Orbit.

It's one point inside a circle. Left is Warm, right is Bright, up is Forward (voices and guitars come closer), down is Open (more space, a bit of a V shape). The further you move from the center, the stronger it gets, and the corners mix the two, so up-left is warm and close, down-right is airy and wide. The center is exactly flat.

Some details I cared about:

- You grab the point and drag it, or tap anywhere in the circle and it glides there. Swiping elsewhere on the circle still scrolls the panel, so you can't change your sound by accident while scrolling.
- You hear it while you move. Changes are smoothed, so moving the point doesn't click or pop.
- It never clips. Levyra works out the headroom for every position and lowers the level just enough, and it shows you how much.
- "Hold to compare" plays the flat sound at a matched level, so you're comparing the tone and not just "louder vs quieter".
- "Save as profile" keeps the position as one of your parametric profiles, and your saved orbits show up as chips under the circle.
- The switch turns it off without forgetting where you were.

Under the hood it's the same parametric engine that already runs your AutoEQ profiles, so it works with every source, FLAC included, and there's no second equalizer stacked on top of the first. TalkBack users can move the point with actions (Warm, Bright, Forward, Open, Center) and hear where it is.

## ✦ Podcasts

There's now a podcast library with subscriptions, listening history and resume from where you stopped.

## ✦ Smarter listening

- Mood Journey, Break the Bubble, Smart Resume and Adaptive Listening.
- A smart listening suite on top of that.
- Songs you dislike are skipped in automatic playback.
- Smart Audio Profiles, YouTube chapters, Night Listening and lyrics search.
- The mood arc curve, an instant Top 50 and covers for Levyra Mix.

## ✦ Looks and library

- New player deck: Ember.
- New skins: Atelier, with a 3D shelf and a turntable player, and Qobuz Style.
- Each skin can have its own Home mode, and language, country and audio preferences are now separate settings.
- Playlists can hold whole albums, there's a new HSL/HSV color picker, and you can sort the tracks inside a playlist.
- Onboarding now asks for a few artists you love and has a new welcome.
- You can choose where downloads go, downloads get proper artwork, there's a keep-screen-on option and Brazilian Portuguese is in.

## ✦ FLAC and performance

Qobuz is now an optional FLAC source, and lossless resolution checks the stream more carefully before using it, with better fallbacks when a source fails. Shared audio credentials were removed from the app. Lossless is still experimental and still depends on outside services, same as before. Startup is lighter too.

The parametric equalizer also keeps its filter memory in plain flat arrays now, which is a little lighter on the audio thread. The sound math didn't change.

## ✦ Thanks

Again I didn't do all of this alone. Thanks to @mvanhorn for two features I use every day now: looping a section of the current song, and sharing a song from the exact moment you're listening to. And thanks to everyone translating on Weblate, Mickaël Binos (@Mickael81) included, for the updates to Chinese, German, Spanish, French, Japanese, Korean, Portuguese and Russian.

## Validation

These notes cover what was merged into main after `v3.0.5`, plus the Orbit equalizer that ships with this version bump. They describe the code; they don't mean the 3.0.6 APK has already passed release testing.

For Orbit I ran the new unit tests, which push audio through the real equalizer processor: the measured response matches the designed one within 0.01 dB, distortion at 1 kHz is about -142 dB, the center position is bit-exact, a 0.99 full-scale signal stays below 0.6 full scale at every pole, and moving the point while music plays produced no clicks. The existing equalizer tests and the translation checks for all 42 languages passed too. On an emulator, with a minified release-type build, the playback thread went from 2.1% to 2.5% of one core with Orbit on. On a Galaxy S25+, in a debug build, the same processing ran at 18 times real time; debug builds are much slower, so that number is an upper bound, not the real cost. I checked the Orbit screen and its drag on the emulator.

I didn't do a long listening session on a physical phone, and I didn't check Bluetooth, USB DAC output, TalkBack, Android Auto or a full regression pass. Those are unverified, not passed.

The GitHub release workflow still has to pass the release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the files. Treat the GitHub release and the downloadable APK as available only once that workflow succeeds.

## Versioning

- Version name: `3.0.6`
- Version code: `3000600`

This is an Android release. Levyra Desktop is versioned and released separately; this update doesn't change the Windows app.

## Upgrade notes

No manual migration is expected. Your library, playlists, favorites, downloads, listening history, settings, skin and player style stay as they are. Your graphic or parametric equalizer setup stays as it is. Orbit only takes over when you move it.

Catalogs, regional releases, lyrics, search, video, podcasts and lossless sources depend in part on external services: results and availability can vary by region and over time.

Once the publishing workflow finishes successfully, the signed Android APK and its checksum will be on the GitHub release page. F-Droid and other channels may update later.

## Final note

Go open the audio panel, put on a song you know really well and drag the point around for a minute. Then tell me where you ended up, I'm curious.

Thanks for listening, and for telling me when something feels off.
