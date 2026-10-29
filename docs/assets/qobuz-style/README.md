# Qobuz Style skin: reverse-engineering notes

Qobuz Style is an unofficial Levyra skin. It recreates the layout and visual language of the Qobuz Android app in original Compose code. No Qobuz code, fonts, images or other assets ship in Levyra, and playback always goes through Levyra's own providers.

## Analysed artifact

| Item | Value |
| --- | --- |
| Package | `com.qobuz.music` |
| Version | 10.1.2.0 (versionCode 26100110) |
| APK | universal (arm64-v8a, armeabi-v7a, x86, x86_64), nodpi, minSdk 23, targetSdk 36 |
| SHA-256 | `539d68cc3e0211349568533a69c4dd62c97e0a0e86506df6320517d72fc7aa0c` |
| Signer (v2) cert SHA-256 | `20c8212f9713a62ac5baee54f5c9c716cd01912fab47cde217a929b00bd6b6a9` (`apksigner verify`, exit 0) |
| Classes / resources | 33,291 classes, 3,621 resources (REA `inspect_android_package`) |

## Tools

- REA MCP: `inspect_android_package` (engine jadx-headless-mcp 0.7.1).
- jadx 1.5.6 CLI for full decompilation (573 methods fell back to bad-code output, as expected with R8).
- apktool 3.0.3 for resources (`values*`, fonts, drawables).
- Android SDK build-tools 37.0.0 `apksigner`.
- Emulator `Levyra_Pixel8_API37` (1080x2400, 420 dpi) for runtime comparison against owner-supplied Qobuz screenshots.

The UI is entirely Jetpack Compose, so the XML layouts only cover widgets and libraries. R8 keeps composable names inside `ComposerKt.traceEventStart` strings, which made it possible to map obfuscated classes back to their source files (`DiscoverFeed.kt`, `WelcomeMixCard.kt`, `PlayerWidget.kt` and so on) and to read dp/sp constants per composable.

## Findings used by the skin

### Discover home order
`DiscoverViewModel` (`tn3.Z0`) builds the feed from `DiscoverUiSectionItemHolder` (`mn3`):

- Home: welcome mix (only without recent activity), recent activity, new releases, Qobuz playlists, playlists by category, release watch or artists to follow, mixes, similar to, player history, content to follow, ideal discography, Qobuzissime, album of the week, most-streamed albums, press awards.
- Our picks: new releases, playlists, categories, ideal discography, Qobuzissime, album of the week, press awards.
- For you: mixes, transfer, release watch or artists to follow, radios, similar to, rediscover library, content to follow, essentials, similar to, spotlight.
- Tapping the selected chip returns to Home (`tn3.t1`).

The skin keeps that order. "Most-streamed albums" is replaced by Levyra Top 50, laid out like Qobuz's ranked pager (three numbered rows per page).

### Layout constants
- `DiscoverSizes` (phone): content padding 8dp, section padding 16dp, 40dp between sections, grid card 156dp (170dp above 600dp, 196dp above 780dp), playlist tags on two lines.
- Album sections (`uj3.c`): new releases two rows, press awards one row of 266dp cards, ideal discography two rows, Qobuzissime one row of 294dp cards, most-streamed as a ranked pager.
- `WelcomeMixCard`: corner 12dp, 1dp border, 14dp padding, 135dp artwork, 40dp avatars, play button.
- `PlaylistTagGridCard`: 171dp wide, corner 8dp, 16dp padding, 16sp bold title, vinyl decoration (redrawn in Canvas, not copied).
- Filter chips: 18x10dp padding, 14sp semibold. Bottom bar: 30dp icons, filled/outlined pairs.
- Player (`PlaybackScreen`, `PlayerPlaybackProgressBar`, `TrackMetadataAndActionsBar`): 16dp padding, 6dp seek track inside a 14dp touch area, 18sp bold title with explicit badge, 24dp heart and overflow, controls spaced 16/30/14dp, 48dp skip icons, bottom tabs explore, lyrics, credits, queue.
- Player background (`v20`): palette vibrant, then dominant, then light vibrant swatch, converted to HSL with saturation at least 0.4 and lightness clamped to 0.2..0.35. `qobuzPlayerField` applies the same rule.
- Artist header (`AppCollapsingHeaderLarge`): 200dp circular image, 28sp bold name, 14sp tag, actions 48dp with a 56dp play button and 20dp spacing.
- Magazine (`magazineItems`): gradient story carousel (16:9 image), rubric rows of 216dp cards, "picked for you" header, adaptive story grid.

### Colour tokens (`ColorPalette`, `z82`)
Light: surfaces `#FFFFFF/#F3F3F3/#EDEDED/#D7D7D7`, text `#242424/#767575`, accent `#9E6301`, nav `#F0FFFFFF`, mini player `#EDEDED`.
Dark: surfaces `#121212/#242424/#323232/#424242`, text `#FFFFFF/#BDBDBD`, accent `#FFCB74`, nav `#F0121212`.
Over artwork: white with 0.7/0.5/0.1/0.2 alpha steps.

### Typography
Qobuz Sans (© Xandrie SA) is proprietary and is not shipped. The skin uses Albert Sans from Google Fonts with Qobuz's scale: 56/48/32/28/22/18sp display, 16/14/12sp body and labels, 12sp caption with 0.48 letter spacing. Weights follow the call sites (titles SemiBold/Bold, subtitles Medium).

## Public data sources

Editorial content is read from public `www.qobuz.com` pages, which `robots.txt` allows for generic clients (cart, sign-in, order and tools paths are excluded and never requested). `open.qobuz.com` and `play.qobuz.com` disallow crawling and are not used. No API keys, app IDs, tokens or private endpoints from the APK are used.

| Section | Page |
| --- | --- |
| New releases | `/{store}/shop`, items with the `homepage` filter |
| Editorial playlists | `/{store}/playlists` and `/{store}/playlists/{slug}/{id}` |
| Press awards | `/{store}/shop/press-awards/download-streaming-albums` |
| Qobuzissime | `/{store}/awards/qobuz/qobuzissime/download-streaming-albums` |
| Ideal discography | `/{store}/awards/qobuz/discotheque_ideale/download-streaming-albums` |
| Magazine | `/{store}/magazine` and `/{store}/magazine/section/{news,panoramas,interviews,hi-fi}` |

The store follows the app language (`QobuzStore.locale`). Pages are cached on disk for 6 hours (stale copies kept up to 7 days), playlist details for 3 hours, requests are de-duplicated, HTTP 429/403 triggers backoff, and redirects off `www.qobuz.com` are rejected. Playlist pages contain signed preview URLs; the parser never reads them. Tracks are matched and streamed by Levyra's providers. Magazine stories open on qobuz.com in the browser; no article text is copied.

Playlist category names and colours only exist behind Qobuz's private API, so the category grid uses Levyra's moods and genres with a warm palette sampled from the screenshots. Album-of-the-week has no public source and is not shown.

## Levyra integrations

My Soundtrack takes the WelcomeQ slot, "Your mixes" holds My Soundtrack, Your orbit and Quick picks, "Similar to" shows Levyra's album recommendations, and Levyra Top 50 replaces most-streamed albums. Explore becomes the Magazine tab while keeping access to Levyra moods, genres, new releases and samples. Advanced library tools stay reachable from the Library chips.

## Validation

- `QobuzCatalogParserTest` covers editorial filtering, cover derivation, ignoring stream URLs, store locale and host checks.
- Runtime checks on the emulator: skin selection, Home with live editorial data, category page, Magazine, Search, Library, artist page, playlist and album headers, player, queue with reordering, credits, lyrics, settings.
