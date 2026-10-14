# Levyra 2.6.8

## Highlights

This update gives you more ways to make Levyra feel like your own. There are new visual options for Home, lyrics, themes, the mini player and the navigation bar. The designs you already use are still there, and the originals remain the defaults.

I've also worked on finding music, keeping Home stable while content loads and clearing up a few annoyances with playback and downloads. It's a mix of new tools and fixes to things you use every day.

## ✦ More room to make it yours

Home Studio adds alternative Home layouts, including expressive and editorial designs. You can choose how sections appear, change their order and adjust some visual settings. Original stays selected by default and keeps the existing Home layout.

Lyrics Studio gives you a separate place to customise how lyrics look. The new options are there if you want them; the previous presentation isn't being replaced.

MiniPlayer Studio adds five optional styles, including compact, floating and artwork-focused looks. Navigation Studio adds four styles for the bottom bar, with options for ordering destinations, labels and other details. Both keep their original interfaces as the default.

Theme Studio gains new presets and personal themes that you can create, edit and manage. These studio settings persist locally and are included in Levyra Vault backups, so restoring a backup can bring them back.

## ✦ Better discovery and a steadier Home

Search can now use Spotify's public catalogue to enrich song, album and artist metadata and artwork, then rely on other sources when needed. Results arrive progressively. The refreshed Material 3 Expressive search layout puts artwork and the most useful actions in clearer view.

The work also improves how duplicate recordings and official videos are presented. More bundled first-launch tracks now have official album covers instead of video frames. Existing Spotify covers are preserved as later artwork requests complete. Playback itself still uses Levyra's existing audio providers.

If you've connected YouTube Music, Your Orbit now keeps its account-specific content visible during session restoration. Home holds on to populated sections while new data is loading, rather than making them disappear or jump unexpectedly. There's also a pull-to-refresh gesture when you want to update the feed.

Top 50 chart content can appear sooner, with cover art filled in progressively. Some artist artwork used during setup, the search suggestion chips and the library category chips have also been improved.

## ✦ Playback, Explore and downloads

Editorial Discovery playback has a more dependable fallback when a recording doesn't arrive with a usable YouTube identity. The resolver can look for a regular matching result and retain the resolved audio source during retries.

Moods and genres have updated Material 3 Expressive screens. Category titles can use more space, and the Explore destination layouts account for the tab bar and mini player at the bottom.

The download progress indicator has a new design and can be minimised or dismissed from view without cancelling the actual download. It's positioned so it doesn't sit on top of the player controls. Velvet's landscape controls have received a small alignment and action fix too.

## ✦ More language options

Bengali, Kazakh, Serbian and Albanian are now included in Levyra's language catalogue. Setup copy and several translated music preference labels have also been improved. The Weblate references were removed from the repository while keeping its translation coverage graphics.

## Validation

This Android update covers changes merged after `v2.6.7`, through main commit `965fa80d5a8e8cd63867d133edbed9f353015c21`. These include Home stability and the download indicator, Lyrics Studio, Home Studio, the MiniPlayer/Theme/Navigation studios, Editorial Discovery and translation work, Spotify-first metadata and search, and the Velvet landscape adjustment.

The relevant code changes added or updated tests, but those tests are not evidence that the final signed 2.6.8 APK has passed. No Gradle build, emulator run or physical-device end-to-end check was performed as part of preparing this version bump.

The Android release workflow still needs to pass lint, assemble the signed APK, verify its version and signing certificate, generate the checksum and publish the release. These are pending steps, not completed checks. A full manual regression pass, Android Auto and device-specific playback remain unverified for the final artifact.

## Versioning

- Version name: `2.6.8`
- Version code: `2060800`

This release changes the Android version only. Levyra Desktop has a separate version and release process.

## Upgrade notes

No manual migration is required by this version bump. Your original Home, mini player and navigation designs remain selectable as before. The additional styles are optional and existing favourites, playlists, downloads and settings keep their normal storage paths.

The GitHub APK and its SHA-256 checksum become available after the signed release workflow finishes successfully. F-Droid and other stores follow their own publication schedules.

## Final note

The idea is simple: give you more freedom to change Levyra's appearance without taking away the one you're used to. And keep fixing the small loading and playback details that make a difference when you're listening every day.
