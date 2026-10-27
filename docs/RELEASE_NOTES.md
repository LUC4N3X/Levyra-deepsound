# Levyra 3.0.3

## Highlights

This release started with a small idea for the mini player and grew from there. The mini player can now slide into the navigation bar while you scroll, FLAC is here as an experimental option, and a single song download asks which quality you want before it starts.

The Spotify Style skin has been rebuilt, the Apple Music Style skin has a new Search, and a few things that kept getting in the way are fixed: playlist imports that stalled, a Back gesture that went somewhere odd, and FLAC that took its time right after you opened the app.

## ✦ A dock that moves with you

Mini player studio has a new Scroll effect setting with three choices.

Tide is what you already know: the mini player and navigation bar shrink a little while you scroll down and come back when you scroll up. It stays the default, so nothing changes unless you pick something else.

Eclipse is the new one. As you scroll down, the mini player slips into the navigation row next to Home and keeps the cover, the title and Play/Pause within reach. Scroll back up and the two separate again. With Liquid Capsule, Search keeps its own round button on the right. The other styles in the default skin get a small Search button that appears on the right while the dock is merged.

If you'd rather the dock stayed still, choose Off.

The setting works in the default skin, Nocturne 78, Prism, Torque and Ivory, and each one keeps its own look. In those skins you'll find it in Design settings. Liquid Capsule now sits second in the Mini player and Navigation studio lists.

## ✦ FLAC, for now an experiment

You can now ask Levyra to prefer lossless playback, and it can also save songs in FLAC. Please treat it as experimental. It depends on outside sources, and I can't promise yet how stable it will be in everyday use. If a lossless version can't be found or checked, playback stays on the normal stream.

FLAC should also show up more reliably right after you open the app. Until now, the tracks Levyra prepares in the background could crowd out the song you actually tapped, and a single "too many requests" reply from a source could switch FLAC off for a minute and a half. Now the song you play always goes first, background lookups take turns, and Levyra waits only as long as a source asks before trying again. If you stay on 320 kbps, none of this changes anything for you.

## ✦ Pick the quality before you download

Pressing Download on a song now opens a small sheet instead of starting straight away. Levyra checks the sources you have enabled (lossless FLAC, HQ 320 kbps AAC and the standard stream), keeps up to three that pass verification and preselects the best one.

Your choice sticks if the download has to retry or resume. If that source fails, you get a clear message rather than a quieter, lower-quality file. Album, playlist, batch, Smart Offline and automatic favorite downloads work as they did before.

## ✦ Skins

The Spotify Style skin has been rebuilt around music discovery. It has real Canvas clips, every official Top 50 chart, podcast, radio and credits pages, and Sample previews.

In the Apple Music Style skin, Search now has categories, moods and genres, which moved there from Explore. Typing a search still uses the same engine, with Apple-style tabs and rows.

## ✦ The everyday fixes

- Playlists have a Like all action in the overflow menu, and you can select several tracks at once.
- Playlist imports no longer stall when YouTube Music slows things down. Searches back off and a failed lookup is retried once.
- Back behaves again: a collapsed player no longer sits in the back history, and leaving search results takes you to the start of Search first.
- Samples stay in the Samples feed. Playing one no longer adds to your history, listening sessions or taste profile, so they don't turn up in Your Orbit.
- Spotify Style sample canvases are prepared ahead of time, so the video is ready when the audio starts.
- When Prefer audio is on, a music video now looks up the catalog version of the song.
- Home pins no longer have a limit. This release also brings the Liquid Capsule dock style, a music video preference and an immersive mode.

## Validation

These notes cover what has been committed to main since 3.0.2, plus the dock, FLAC start-up and README changes in this release pull request. They describe the code and how it's meant to behave; they are not a claim that the 3.0.3 APK has already passed release testing.

New unit tests cover the Scroll effect setting and its backup, the translations, and the FLAC lookup and rate-limit changes. They passed locally, along with the full Android unit test suite. The local fast AI quality gate stopped on a known line-ending issue in a file this release doesn't touch, so its later steps did not run. I did not run the full quality gate, an Android release build, a physical-device regression pass, Android Auto checks or signing verification. Their status is unverified, not passed.

Eclipse was checked on an emulator with the Original and Liquid Capsule styles. The other styles and skins haven't been checked visually yet. The FLAC start-up change was checked against emulator logs and confirmed on a phone.

The GitHub release workflow must still pass its release-note checks and Android lint, build and sign the APK, verify the version and signing certificate, generate the SHA-256 checksum and publish the checked files. A GitHub release and a downloadable APK should only be treated as available after that workflow succeeds.

## Versioning

- Version name: `3.0.3`
- Version code: `3000300`

This is an Android release. Levyra Desktop is versioned and released separately; this update does not change the Windows app.

## Upgrade notes

No manual migration is expected. Your existing music library, playlists, favourites, downloads, listening history, settings, selected skin and player style stay in place. The new Scroll effect starts on Tide, which is the behaviour you already had. FLAC only comes into play if you choose lossless.

Music catalogues, regional releases, lyrics, search, streaming video and lossless sources rely in part on external services; results and availability can vary by region and over time.

Once the publishing workflow finishes successfully, the signed Android APK and its checksum will be on the GitHub release page. F-Droid and other distribution channels may update later.

## Final note

3.0.3 is a lot of small things that add up. The dock gets out of the way when you're browsing, your downloads land in the quality you picked, and FLAC is there for anyone who wants to try it, rough edges and all.

Thanks for listening, and for telling me when something feels off.
