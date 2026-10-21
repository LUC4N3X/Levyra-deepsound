# Apple Music Style runtime captures

These are unedited ADB screenshots from the Android Debug app
(`-PlevyraFdroidBuild=true`) on the Pixel 8 API 37 emulator (1080 x 2400,
420 dpi). Artwork and catalog content come from Levyra's providers, the local
library and the public `music.apple.com` pages. No mockups are used.

Apple Music Style is an unofficial theme. It is not affiliated with or endorsed
by Apple, and the app bundles no Apple code, icons, fonts, artwork or
screenshots. SF Pro is proprietary, so the skin uses Inter from Google Fonts
through the existing downloadable-font provider.

## How the reference was measured

- The visual reference was Apple Music for Android 7.0.0-beta (build 1611,
  APKM SHA-256 `EBC916EF…0B23`). Resources were decoded with Apktool 3.0.3
  and the code was read with JADX through the REA analysis tools.
- The `New`, `Radio` and `Search` tabs were measured at runtime with
  `uiautomator dump` on the emulator. Library, Home and the player sit behind
  the subscription screen, so their values come from the decoded layouts
  (`music_player.xml`, `constraint_player_media_controls.xml`,
  `player_progress_seek_bar.xml`, `mini_player.xml`,
  `epoxy_layout_collection_page_header.xml`, `view_motion_container.xml`) and
  from `PlayerMainFragment` / the motion-backdrop helper in the decompiled
  code. The paywall was not bypassed.
- The Home composition follows an owner-provided screenshot of the Italian
  `Novità` page, which matches the order of the public `/new` page.

| Token | Apple Music value | Source |
|---|---|---|
| Page gutter | 20 dp | runtime dump |
| Section header | 22 sp bold, 44 dp row, trailing chevron | runtime dump |
| Hero card | 3:2, corner 10 dp, peek of the next card | runtime dump |
| Square card | `(width - 81) / 2`, 12 dp gap, 13 sp labels | runtime dump |
| Song row | 56 dp, 48 dp artwork, corner 5 dp | runtime dump |
| Floating tab bar | 62 dp pill, 20 dp side margin, 11 sp labels | runtime dump |
| Mini player | 48 dp, 30 dp thumbnail, 13/12 sp text, 32 dp buttons | `mini_player.xml`, dimens |
| Player artwork | 24 dp side guides, corner 10 dp, shadow 15 dp (8 dp paused) | `music_player.xml`, dimens |
| Player title / artist | 20 sp medium / 20 sp at 45 % white | `music_player.xml`, styles |
| Seek bar | 7 dp bar, 32 dp side padding, 12 sp times | `player_progress_seek_bar.xml`, dimens |
| Transport | 40 / 38 / 40 dp icons, 11 dp padding, 40 dp apart | `constraint_player_media_controls.xml`, drawables |
| Full-screen canvas | 3:4 video at the top, mirrored blur 150 dp over its edge, #171717 legibility mask | `view_motion_container.xml`, `PlayerMainFragment`, motion helper |
| Collection header | artwork between 72 dp guides, 20 sp title, accent artist line, pill buttons 44 dp | `epoxy_layout_collection_page_header.xml`, `button_b_background.xml` |
| Accent | `#FA233B` light / `#FC3C44` dark | APK colour table, owner screenshot |

The 7.0 beta switched its key colour to blue (`#0088FF`); the skin keeps the
Apple Music red used by the owner screenshot and by the APK's own playlist
cover colour.

| Capture | Runtime view |
|---|---|
| `skin-selector.png` | Skin selector with the unofficial-theme notice |
| `home.png` | Large title, country chip, `/new` hero carousel, song columns |
| `home-shelves.png`, `home-orbit-playlists.png` | My Soundtrack, weekly releases, Your Orbit, two-row playlists |
| `player.png`, `player-menu.png` | Artwork-tinted player and the context menu |
| `explore.png`, `library.png`, `search.png` | Main destinations |
| `settings.png` | Grouped settings |
