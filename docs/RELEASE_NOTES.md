# Levyra 2.6.6

## Highlights

Levyra 2.6.6 gives you more ways to make the player your own, and more control over the music you keep ready to play. The player has new looks and backgrounds, Home and Explore have been refined again, and the transition between the mini-player and full player is smoother.

This update also adds optional advanced preloading, Smart Offline collections, personal M3U and JSON music libraries, and a better way to fix lyrics timing for individual songs. These features build on the existing music library and playback experience rather than replacing it.

## ✦ More ways to make the player yours

The player now offers six visual styles: Studio, Velvet, Liner Notes, Bloom, Gallery and Late Night. Studio is the default if you have never selected a style; your saved choice is preserved. Velvet brings artwork closer to the controls, Liner Notes puts the music details first, and Late Night has a vinyl presentation that stops spinning when playback is paused.

You can also choose Aurora or Gradient player backgrounds, and the new wavy progress control moves only while music is playing. The mini-player opens into the full player more naturally, while playback controls remain in the regular Media3 music notification instead of an additional live-update notification.

The Material 3 Expressive treatment reaches the queue, Audio panel, artist and album pages, search, listening insights and collections. The first-run setup is more visual too, with artist artwork in genre selection. Home, Explore and Levyra Mix have clearer controls and more consistent spacing.

## ✦ Advanced preloading, only if you want it

In Audio settings, **Advanced preloading** can prepare the next 5, 10 or 15 songs in your actual queue using Levyra's existing playback disk cache. It is **off by default**, so nothing changes for listeners who leave it disabled.

When enabled, preloading runs on unmetered connections, follows cache and storage limits, and works with supported streams that can be fully cached. This is intended to reduce interruptions when a connection becomes unstable. It is not a guarantee of complete offline playback: some tracks still need their source resolved before they can play.

The existing buffering controls and manual downloads keep their own behaviour. The new setting is saved with your preferences and backups.

## ✦ Smart Offline and personal collections

**Smart Offline** can keep frequently played and favourite tracks ready within a storage limit you choose. Its background management distinguishes automatically cached tracks from your own manual downloads, so automatic cleanup does not prune songs you downloaded yourself. Settings and the Downloads library provide controls and a status view.

You can now also import personal music collections from M3U or Levyra JSON, using a local file or an HTTP(S) source. Imported collections support refresh, artwork, queue playback and offline downloads. Remote credentials remain in encrypted app storage; plain HTTP and local-network access have explicit consent checks. Removing a collection does not silently remove tracks you downloaded from it.

## ✦ Fix lyrics without shifting every song

Lyrics timing now has a correction for each song, from -30 to +30 seconds, available directly in the lyrics screen and in a dedicated timing sheet. This is separate from global or Bluetooth output latency.

A correction to one track no longer changes the timing of every other track. Existing latency settings remain compatible, and saved per-song corrections follow the established backup flow.

## ✦ Smoother screens and everyday fixes

An artist-page rendering issue that could freeze the interface has been addressed by removing an expensive decorative shadow. Home's decorative backdrop no longer redraws continuously while idle, reducing unnecessary rendering work.

Home's quick discovery chips now use the same neutral Material 3 tonal surface instead of individual genre-coloured gradients, while keeping their expressive pressed shape and existing tap targets. Levyra Collections cards also trade saturated backgrounds for quieter tonal surfaces, letting the artwork stand out.

The comments sheet has a clearer layout and improved author-avatar handling. Like and dislike controls no longer accidentally trigger the player's surrounding gestures. Home's personal-orbit heading now has a more compact Play All button so the title can stay on one line. The update banner, navigation and several other controls have been polished as part of the same visual work.

## Validation

The changes described above are grounded in Android pull requests merged into `main` after the publication of `v2.6.5`, including #880, #889, #892, #895, #896, #897, #900, #904, #906 and #913. Individual changes reported focused automated tests and emulator checks. In particular, the player visual updates and comments sheet were exercised on an API 37 emulator, and the artist-page and idle-Home rendering fixes have targeted emulator evidence.

These are checks reported for the individual changes, **not** proof that the final signed 2.6.6 APK has passed end-to-end testing. The release workflow must still run its Android release lint and signed APK build, verify the package version and signing certificate, generate a SHA-256 checksum, publish the release, and check the resulting assets. No outcome of those 2.6.6 release checks is claimed in advance.

A full manual regression pass of the final 2.6.6 APK, including physical-device playback, Android Auto, downloads, background operation and every supported Android version, is not yet verified by this version update.

## Versioning

- Version name: `2.6.6`
- Version code: `2060600`

This release is for Android. Levyra Desktop retains its independent version and publication schedule.

## Upgrade notes

No manual migration is expected. The app includes database migrations for Smart Offline ownership and personal music collections. Existing manual downloads retain their ownership, while old installations keep Advanced preloading disabled until it is explicitly selected.

Your saved music, favourites, playlists, listening history, existing playback preferences and backups continue through the established application data paths. GitHub's signed APK and checksum appear only after the release workflow completes successfully. F-Droid and other distribution channels follow separate publication schedules.

## Final note

2.6.6 is about choice without extra friction. You can change the look of the player, keep more music close at hand, and correct the details that matter to you, while the familiar listening experience stays in place.
