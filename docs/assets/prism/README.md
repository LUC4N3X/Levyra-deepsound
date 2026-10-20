# Levyra Prism runtime captures

These are unedited ADB screenshots from the Android Debug app on the
owner-started Pixel 8 API 37 emulator. Artwork and catalog content come from the
app's existing providers and local library. No promotional mockups are used.

The visual reference was Mixified Pixel's [official Proxima gallery](https://sites.google.com/view/proxima-skin/screenshots).
All nine gallery images were inspected. Prism uses original Compose components;
no Proxima code, icons, artwork, screenshots, or branding are bundled in the app.
The existing [Nocturne 78 integration](https://github.com/LUC4N3X/Levyra-deepsound/pull/1010)
provided the technical model for global skin ownership and non-destructive
Home Studio and Player Deck restrictions.

| Capture | Runtime view |
|---|---|
| `home.png`, `explore.png`, `library.png`, `settings.png` | Main Prism destinations |
| `player-flow.png`, `player-focus.png` | Dedicated player compositions |
| `second-cover.png`, `miniplayer.png` | Different artwork and adaptive ambient colors |
| `light.png`, `skin-selector.png` | Light appearance and the three-skin selector |
| `home-studio-lock.png`, `deck-lock.png` | Skin-managed layout restrictions |
| `queue.png`, `lyrics.png`, `equalizer.png` | Existing functional panels with Prism styling |
| `search.png`, `artist.png` | Search results and artist details |
| `explore-scroll.png`, `font-1-6.png` | Scrolling and enlarged text |
| `default-home.png`, `nocturne-home.png`, `persistence.png` | Skin switching and force-stop persistence |

Playback was observed through MediaSession with an advancing position and no
playback error. Queue, synchronized lyrics, preset selection, Flow/Focus,
Night/Light, Reduced effects, font scale 1.6, and force-stop reopening were
exercised. Font scale was restored to 1.0. The emulator was never started,
stopped, restarted, or recreated by the agent.

Device backup/restore, missing-artwork playback, actual Canvas/video playback,
tablet/sidebar layouts, TalkBack, RTL runtime behavior, system-triggered
Automatic appearance, and sustained performance profiling remain unverified.
Preference and backup round trips, palette contrast, appearance selection, and
skin isolation have unit coverage. The pull request records the final build
and quality-gate results separately from these manual observations.
