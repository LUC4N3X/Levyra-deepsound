# Spotify Style runtime captures

These are unedited ADB screenshots from the Android Debug app
(`-PlevyraFdroidBuild=true`) on the Pixel 8 API 37 emulator. Artwork and catalog
content come from the app's existing providers and local library. No mockups are
used.

Spotify Style is an unofficial theme. It is not affiliated with or endorsed by
Spotify, and the app bundles no Spotify code, icons, fonts, artwork, or
screenshots. The visual reference was the public Spotify Android app: colour,
spacing, radius and type-size values were read from the resource table of the
official APK (version 9.1.42.2058) with `aapt2 dump resources`, and compared with
an owner-provided screenshot of the artist page. The official app could not be
inspected at runtime because it stops at its login screen and blocks screen
capture there. Spotify's own typeface is proprietary, so the skin uses Figtree
from Google Fonts through the existing downloadable-font provider.

| Capture | Runtime view |
|---|---|
| `skin-selector.png` | Skin selector with the unofficial-theme notice |
| `home.png`, `home-shortcuts.png` | Home header, picked card, chips, carousels, mixes and the 2-column shortcut grid |
| `search.png`, `explore.png`, `library.png` | Main destinations |
| `player.png`, `lyrics.png`, `queue.png` | Cover-tinted player, lyrics on the cover colour, queue |
| `album.png`, `album-scrolled.png`, `artist.png` | Detail pages with cover-tinted headers, the collapsed title bar and track rows |
| `settings.png`, `equalizer.png`, `action-sheet.png` | Secondary surfaces |
| `font-1-6.png` | Home at font scale 1.6 |
| `prism-switch.png` | Prism after switching away; playback and queue carried over |

Playback was observed through MediaSession (PLAYING, advancing position,
shuffle and repeat custom actions in sync) and continued in the background.
Next, shuffle, repeat, lyrics, queue, long-press selection, the track action
sheet, skin switching in both directions, force-stop persistence and
font scale 1.6 were exercised. Font scale was restored to 1.0.

Tablet and landscape layouts, TalkBack, RTL runtime behaviour, Android Auto,
downloads end to end, and release-build performance remain unverified.
