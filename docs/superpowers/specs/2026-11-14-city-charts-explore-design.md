# City charts in Explore for every skin

Date: 2026-11-14
Status: approved by owner 2026-11-14
Branch / worktree: `feat/city-charts-explore` / `Levyra-city-charts`

## Intent

The Apple Music Style skin shows Apple's city charts ("Top 25: Milano", "Top 25: Roma", ...) on its Home. No other skin can reach them. The owner wants every other skin to offer them, presented in an orderly, polished way: Material 3 Expressive in Default, and native to each skin's own visual language elsewhere.

## Goals

- A "city charts" rail in Explore, placed directly after the Daily Top 100 rail, in every skin that shows the Daily Top 100 today, plus Qobuz Style, which gains both rails.
- Tapping a city opens the chart with the same screen, playback and track-menu behavior as a Daily Top 100 chart in that skin.
- Each skin gets a city card that belongs to its style and is visibly different from its country Top 100 card, so the two adjacent rails never read as duplicates.
- Copy in all 42 supported languages.

## Non-goals

- No change to the Apple Music Style skin (its Home keeps its own city charts).
- No city charts on Home in any skin.
- No new data source beyond Apple's public catalog pages already used by the app.
- No user setting to pick a city or reorder cities.

## Anti-goals (failure even if it works)

- Two rails that look identical except for the label.
- A blank gap, spinner or empty header where a storefront has no city charts.
- Explore becoming slower to show its first content, or extra network work on every Explore visit.
- Card designs that clone Apple's city chart artwork (the coloured grid tile) outside the Apple skin.

## Current state (verified in code, base `origin/main` 437039983)

- City charts come from the Apple "New" catalog page: `AppleMusicEditorialRepository.newMusic` / `cachedNewMusic` (`AppleCatalogPath.New`). `AppleMusicHomeShelves.chartParts(title)` recognises titles matching `^Top\s+\d+\s*[:：]` and splits them into badge ("Top 25") and name ("Milano"). `AppleMusicChartCard` draws them in the Apple skin.
- Daily Top 100: `AppleMusicEditorialRepository.dailyTop100` / `cachedDailyTop100` -> `AppleDailyTop100.charts` -> `DailyTop100ViewModel` (cache-then-network load keyed `storefront:language`, `open` / `retry` / `close`, max 100 tracks) -> `ExploreDailyTop100` in `ui/ExploreDailyTop100.kt`.
- Top 100 reaches skins through:
  - `buildExploreRows(editorial = true)` -> `ExploreRow.DailyTop100` -> `ExploreEditorialRow` for Default, Nocturne 78, Prism, Torque, Ivory and Spotify (`editorialExplore` in `LevyraApp.kt`);
  - `MonoExplore.kt` (explicit item);
  - `YouTubeMusicExplore.kt` (adds `ExploreRow.DailyTop100`);
  - `AtelierExplore.kt` (calls `ExploreDailyTop100` directly).
  - Qobuz Style has no Top 100 in its Explore today.
- Per-skin Top 100 variants already exist for cards and chart screens: Default/Nocturne (`Top100Cover`, `Top100ChartScreen`), Prism, Torque, Ivory, Mono, Spotify (card only), YouTube Music, Atelier.
- Copy: `LevyraDailyTop100Strings.kt`, `DailyTop100Copy`, 42 languages, English fallback via `localizedValueOrEnglish`.

## Approaches considered

- **A. Second rail on the Daily Top 100 pipeline (chosen by owner).** New repository read of city charts from the New page, a second list in `DailyTop100ViewModel` sharing open/retry/close, a new `ExploreRow.CityCharts` right after `DailyTop100`, per-skin city cards, chart screens reused. Least new code, identical behavior to Top 100, empty/offline states already handled. Cost: one more Explore row.
- B. One section with a "Countries | Cities" button group in the Top 100 header. Shorter Explore, but cities hidden behind a tap and every skin's header must change.
- C. Separate ViewModel and screens. Most design freedom, but duplicates loading, opening and error handling per skin.

## Design

### Data

- `AppleCityChart(playlistId, badge, name)` data class next to `AppleDailyChart` in `data/apple/`.
- `AppleCityCharts.charts(page: AppleCatalogPage?): List<AppleCityChart>` (pure, unit-testable) in `data/apple/`:
  - take playlist items from all shelves of the page;
  - keep items where `AppleMusicHomeShelves.chartParts(title) != null`;
  - drop country charts: exclude any playlist id known to `AppleDailyTop100` (global + `countryCharts`) and any badge whose number is 100;
  - resolve id with `AppleCatalogIds.playlist(item.id) ?: AppleCatalogIds.playlistIdFromUrl(item.url)`;
  - `distinctBy { playlistId }`, keep Apple's page order (the editorial order Apple shows), cap at 30.
- Repository: `cityCharts(storefront, language)` and `cachedCityCharts(storefront, language)` built on the existing `page(AppleCatalogPath.New, ...)` / `cachedPage(...)`. The New page is already cached and stored by the repository: no new HTTP client, cache or storage.

### ViewModel

- Extend `DailyTop100UiState` with `cities: List<AppleCityChart>`.
- `load(...)` fetches the city list in the same job as the Top 100: cached first, then fresh, with the same key guard. The Top 100 publishes as soon as it arrives and never waits for cities; a failure on one side keeps the other list.
- The open chart becomes a shared shape so cities reuse `open` / `retry` / `close`: `DailyTop100ChartUiState` carries a chart reference that is either a country chart or a city chart (sealed interface `DailyChartRef` with `playlistId` and display label). Track loading stays `repository.playlist(storefront, playlistId, language)`, max 100 tracks.

### UI

- `ExploreRow.CityCharts` with key `explore-city-charts`, added in `buildExploreRows` right after `ExploreRow.DailyTop100`, and in `YouTubeMusicExplore.kt`, `MonoExplore.kt` and `AtelierExplore.kt` right after their Top 100 item.
- `ExploreCityCharts(onPlay, onTrackActions, modifier)` in a new file `ui/ExploreCityCharts.kt`, reading the same `DailyTop100ViewModel` instance as `ExploreDailyTop100` (same `viewModel<DailyTop100ViewModel>()` owner), so one load serves both rails.
- When `cities` is empty the composable emits nothing: no header, no spacer.
- The open chart dialog is rendered once. Today `ExploreDailyTop100` owns it; it moves to a small shared composable so either rail can open it and only one dialog exists.
- Chart screens: reuse each skin's existing Top 100 chart screen. Where a screen paints the "100" numeral (`Top100Numeral` in Default, and any equivalent in other skins) it takes the badge number instead, so a city chart shows "25".
- Per-skin city cards, same rail metrics (size, gaps, insets) as that skin's Top 100 rail so the two rails line up:
  - **Default / Nocturne 78 (M3 Expressive):** tonal card on the scheme's tertiary container family (Top 100 uses its own palette slots), city name as hero text in the existing display style, small "Top 25" label, one Material expressive shape from `coverShapes()` as a decorative mark rotated per city. Press feedback via `levyraPressable(..., LevyraPressScale.Tile)`.
  - **Prism, Torque, Ivory, Mono, Spotify, YouTube Music, Atelier:** a city variant derived from that skin's Top 100 card: same materials, type and shape, but the city name leads and the country numeral treatment is replaced by the badge.
- Accessibility: card `contentDescription` = "<section title>, <city>"; 48dp minimum targets; long city names ellipsize after two lines; font scale handled as Top 100 cards handle it.

### Copy

- New `CityChartsCopy(title, subtitle)` next to `LevyraDailyTop100Strings.kt`, all 42 languages. English: "City charts" / "What each city is playing right now". Italian: "Classifiche città" / "Cosa ascolta ogni città in questo momento". Failed/retry copy reuses `DailyTop100Copy`.
- City names and "Top 25" come from Apple, localized by storefront/language; Levyra does not translate them.

## Constraints

- No blocking work on the main thread; repository calls stay in `viewModelScope`.
- Cached content stays visible during refresh; no shimmer replacing real cards.
- Must not delay the Top 100 rail or the first Explore frame.
- No new dependencies, HTTP clients or caches.
- No source-code comments (repo rule).
- Unit test for `AppleCityCharts.charts`: city kept, Top 100 excluded, duplicates removed, null/empty page gives empty list.

## Left to the builder

- Exact visual details of each non-Default city card within that skin's existing tokens.
- Whether the shared dialog lives in `ExploreDailyTop100.kt` or the new file.
- How to thread the badge number into each skin's chart screen with the least churn.

## Qobuz Style (owner decision: add both Top 100 and city charts)

- Qobuz Explore is `QobuzMagazineScreen` (`ui/QobuzMagazine.kt`). In its Home rubric (`state.section.isBlank()`) it shows `mag-carousel`, then `mag-picked` and a long story list, then `levyra-explore` (`QobuzLevyraRubrics`).
- Placement (my call): two items right after `mag-carousel` and before `mag-picked`, only in the Home rubric: `qobuz-top100` then `qobuz-city-charts`. After the long story list they would sit too far down; above the carousel they would push out the magazine's lead story.
- Qobuz gets its own variants in its existing tokens (`QobuzDesign`, `QobuzText`, `QobuzRowPadding`): a Top 100 card, a city card and a chart screen built from Qobuz components and track rows. Same rules as the other skins: city card visibly different from the country card, same rail metrics.
- `QobuzMagazineInputs` must carry `onPlay` / `onTrackActions` for the chart screen if it does not already.
- Both rails hide when their list is empty, as everywhere else.
