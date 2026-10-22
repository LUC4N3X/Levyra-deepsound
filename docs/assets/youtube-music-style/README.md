# YouTube Music Style skin

Unofficial skin. Levyra keeps its own audio engine, providers, data and features; this skin only changes the
presentation layer. Not affiliated with or endorsed by Google or YouTube. No proprietary code, logo, font or
image from the reference app is shipped: every component is original Kotlin/Compose written from measurements.

## Reference artifact

| Field | Value |
| --- | --- |
| Package | `com.google.android.apps.youtube.music` |
| Version | 9.42.52 (versionCode 94252240), minSdk 32, targetSdk 36, arm64-v8a |
| APKM SHA-256 | `29cfe9e83d835ea5470c48156560bd1eb34539ed3edc45938bc9cb4113b08bf7` |
| `base.apk` SHA-256 | `30d7ffe6eb25f3ad045a4b7739e43d5c3a08522727fc2b2dac1c5225736315a8` |
| Splits analysed | base, `split_config.xxhdpi`, `split_config.en`, `split_config.it` |

## Tools

- REA (github.com/morluto/rea) MCP server: `inspect_android_package` (engine jadx-headless-mcp 0.7.1,
  jar SHA-256 `6e5eacf5…ff75`, 35,936 classes, 2,799 resources) and `search_android_classes`.
- apktool 3.0.3 (`d -s`) for resource decoding; a local summariser resolved layout trees and dimens/colors.
- Android emulator `Levyra_Pixel8_API37` (1080×2400, 420 dpi) with the reference app installed from the APKM
  splits; `uiautomator dump` and `screencap` for runtime measurements (px ÷ 2.625 = dp).
- InnerTube responses (`FEmusic_home`, `FEmusic_explore`, continuations) inspected to map renderers.

## Findings

Code is fully R8-obfuscated (`search_android_classes "MiniPlayer"` returns 0 matches) and most screens are
server-driven (`libelements.so`), so geometry comes from resources plus runtime measurement.

| Area | Evidence | Value used |
| --- | --- | --- |
| Mini player | `dimen/mini_player_height`, `mini_player_watch_fragment_length`, `swipeable_mini_player_item_container` | 68 dp, square 48 dp art (corner 0), 16 dp margins, edge progress line |
| Bottom bar | `pivot_bar` (bg `ytm_color_grey_12`), `pivot_bar_cairo_height` | 56 dp, filled/outlined icons, 12 sp labels |
| Type scale | `TextAppearance.YouTubeMusic.*` | Display1 28 sp (−0.04 em), Title1 20 sp, Title2/Subtitle1 14 sp; runtime list text 16 sp |
| Colors | `ytm_color_*` | text #FFFFFE, secondary #AAAAAA / white 67 %, greys #EBEBEB to #131313 |
| Carousel header | `music_carousel_shelf_basic_header` | 16 dp gutter, optional 40 dp avatar + 16 dp gap, strapline above title |
| Two-row item | `music_two_row_item` | title 2 lines, subtitle 2 dp below |
| Home (runtime) | screenshots + dumps | chips 32 dp / gap 8 / radius 8, list rows 72 dp with 56 dp art, columns ~380 dp |
| Explore (runtime) | screenshots + `FEmusic_explore` | 3 tiles 101 dp radius 16, mood buttons with left colour stripe (`leftStripeColor`), ranked rows |
| Player (runtime) | screenshots | artwork inset 32 dp radius 8, title 24 sp bold, pill row, 72 dp white play, UP NEXT / LYRICS tabs |
| Collections (runtime) | screenshots | centered art ~220 dp, 34 sp title, 5-button row with 64 dp white play |

InnerTube renderers mapped by `YoutubeMusicRepository.parseHomeFeed`: `chipCloudChipRenderer`,
`musicCarouselShelfRenderer` (header strapline, thumbnail, browse id), `musicResponsiveListItemRenderer`,
`musicTwoRowItemRenderer` (song, 16:9 video, album, playlist, artist) and `musicNavigationButtonRenderer`.

## What the skin changes

- Home: InnerTube home (public, or the optional signed-in account), mood chips, Quick picks columns,
  community playlist cards with live previews, discovery cards, artist shelves, 16:9 videos, Explore shelves,
  plus Levyra Top 50 with country picker, My Soundtrack, Orbit, albums and artists.
- Explore: top bar, Levyra shortcuts as tiles, new releases, moods & genres stripe grid, ranked trending, videos.
- Library: category chips, liked/playlist/album/artist rows, New pill.
- Player, mini player, detail dock, bottom bar, album/playlist/artist headers and album track rows.

## Tests

- `YoutubeMusicHomeFeedParserTest` (chips, shelf styles, continuation, mood buttons) passes locally.
- Manual emulator passes for Home, Explore, Library, Search, album, artist, player and skin selection.

## Limitations

- The logged-out InnerTube home is shorter than the signed-in one; personal sections appear only with the
  optional account.
- The brand mark uses the Levyra logo; YouTube Sans is not redistributed, the system font is used.

## Screenshots (Levyra)

`home.png`, `home-videos.png`, `explore.png`, `library.png`, `album.png`, `artist.png`, `player.png`,
`skin-selector.png`.
