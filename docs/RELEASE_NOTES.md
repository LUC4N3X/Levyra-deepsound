# Levyra 2.6.5

## Highlights

Levyra looks and feels quite different in this update. Home is fuller without feeling crowded, Explore is easier to browse, and Library puts the music you've saved back at the centre of the screen. The new Material 3 Expressive styling runs through the app, with more consistent shapes, spacing, typography and controls.

There's more than a visual refresh here. You can choose how much audio Levyra buffers, import more headphone EQ presets, and do more with the tracks in your queue. Motion Artwork has also gained new sources and smoother fallbacks. Several fixes address problems that showed up in real use, including an Explore crash caused by unnecessary repeated catalog loading.

This release covers the Android changes merged after 2.6.3. The Windows version follows its own release schedule.

## ✦ A more comfortable Home and Explore

Home has been reorganised so you can reach your music sooner. The main feature area takes up less of the first screen, sections have a clearer rhythm, and album artwork carries more of the visual weight.

- A more compact greeting and header, cleaner shortcuts, and less wasted space between section titles and their content.
- Richer editorial collections and Quick Picks, with clearer artwork and more useful browsing layouts.
- Genre shortcuts that open the matching Explore destination, rather than stopping at a decorative label.
- A new presentation for the Top 50, with a podium-style top ten and compact entries for the rest.
- A subtle Home background influenced by the artwork currently playing, with motion governed by the existing visual settings.

Explore follows the same calmer design. Moods and genres use artwork-led cards and more coherent destination screens; fresh music, radio, mixes and category shortcuts are easier to tell apart. Some long labels, contrast issues and crowded controls have been refined too. These changes keep the existing discovery and playback actions rather than replacing them.

## ✦ Your Library, with the music first

The Library has a more useful starting point: favourites come first, other saved collections are easier to scan, and playlists have a clearer, artwork-focused shelf.

The weekly listening chart has also been redrawn with slimmer bars, a quieter grid and more readable labels. Search, sorting, playlist import and empty states use the same visual language as the rest of Levyra. Existing playlist actions, saved tracks and navigation are retained.

## ✦ A cleaner Player and better queue tools

The Player, mini-player, album and playlist surfaces have received a shared visual polish. Typography, control spacing, selection states and accents feel more consistent, while the Player Deck offers clearer style previews and selection feedback. Artist credits and recent-search metadata have also had small usability fixes.

Queue tools are more capable now, including actions to manage tracks and save queue content as a playlist. The changes build on the existing queue engine rather than replacing playback or saved library data.

Material 3 Expressive now reaches more of the controls you actually touch, including contextual actions, playlist playback and multi-selection. The aim is simple: clearer controls and a more consistent feel from one screen to the next.

## ✦ Playback buffering, now your choice

Levyra's original buffering behaviour is still the default. If you never open the new setting, nothing changes.

For anyone who wants finer control, Audio Settings now offers an **Automatic** mode and a **Custom** mode with Reduced, Balanced and High presets, plus manual adjustments within safe limits.

Your choice is saved with your other preferences and included in backups. New values take effect when the player is created again, so adjusting a setting does not restart the track you're listening to. The controls and explanations are available across Levyra's 37 supported Android languages.

## ✦ More headphone presets work with AutoEQ

AutoEQ importing is more flexible and more careful about files it cannot represent accurately. Alongside GraphicEQ, Wavelet and Equalizer APO / Peace formats, the importer handles supported ParametricEQ and FixedBandEQ configurations, AutoEq correction CSV files, and compatible SoundSource and Rockbox exports.

File detection now looks at the content rather than trusting the extension alone. It also handles more real-world encoding, separator and formatting differences.

Importing shouldn't silently throw away filters, overwrite duplicate points or clip an out-of-range correction. When a preset needs conversion, Levyra identifies the detected format and explains any approximation. Unsupported or incomplete configurations produce an explicit error instead of an apparently successful but inaccurate import.

Levyra's graphic equaliser still has ten bands: converting a high-resolution correction curve is an approximation, not a promise of identical output to a convolution filter or a different EQ engine. Saved presets and the existing preset-selection workflow remain in place.

## ✦ Motion Artwork with fewer rough edges

Motion Artwork has gained Apple Music artwork support on album and artist pages, alongside improvements to the existing Canvas lookup, caching and refresh paths. Static artwork remains available while a suitable animation is being resolved, helping avoid distracting gaps when moving between tracks.

The Player's artwork colours are also handled more consistently, including improved tonal mapping and better fallback accents when a cover has very little colour.

## ✦ Stability and smaller fixes

One important fix is in Explore: the editorial catalog could be read and parsed repeatedly by concurrent requests during a cold start, causing excessive memory use and, in some cases, an out-of-memory crash. The catalog is now shared across those requests instead of being loaded over and over.

Other refinements include more readable text and contrast, tidier recent-search cards, cleaner navigation and updated extractor player-configuration data. Where upstream configurations disagree, the existing conservative validation and fallback rules remain part of the configuration pipeline.

## Validation

The changes in this release come from merged work on `main` after tag `v2.6.3`, including focused unit and UI contract tests added alongside buffering, AutoEQ, the queue, Motion Artwork, Explore and the Material 3 refresh.

There is direct manual evidence from individual changes: the buffering modes and preset controls were checked on a Samsung device using Wi-Fi debugging; the Explore cold-start memory fix and several Home/Player Deck interactions were checked on an API 37 emulator. These are tests of the relevant changes, not an end-to-end test of the final 2.6.5 APK.

The Android release workflow is the publication gate. It must validate the version and these notes, run Android release lint, assemble a signed APK, check the package version and signing certificate, generate a SHA-256 checksum, publish the GitHub release and verify the downloaded release assets. None of those final 2.6.5 checks is claimed as passed in advance.

A full manual regression pass of the final signed 2.6.5 APK on physical devices, Android Auto, background playback, downloads and every supported Android version has not been completed specifically for this release.

## Versioning

- Version name: `2.6.5`
- Version code: `2060500`

This is an Android release. Levyra Desktop has a separate version and release line.

## Upgrade notes

No manual migration is required. This update does not introduce a new Room database schema or require you to rebuild your Library. Existing favourites, playlists, downloads, listening history, settings and backups remain on their established data paths. The new buffering preference defaults to Automatic for existing users.

Once the signed APK and checksum have passed the GitHub release workflow, they will appear on this release page. F-Droid and other distribution channels follow their own build and publication schedules.

## Final note

2.6.5 is about making Levyra more pleasant to use every day. Your music is easier to find, the screens feel more connected, and there are useful new controls when you want them. Just as importantly, the default listening experience stays familiar.
