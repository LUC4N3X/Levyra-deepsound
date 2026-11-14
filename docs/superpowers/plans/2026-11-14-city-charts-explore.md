# City Charts in Explore Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Show Apple's city charts ("Top 25: <city>") as a rail right after the Daily Top 100 in the Explore of every non-Apple skin, and add both rails to Qobuz Style.

**Architecture:** City charts are parsed from the Apple "New" catalog page the repository already fetches and caches. `DailyTop100ViewModel` loads them next to the Top 100 and shares one open/retry/close flow and one chart dialog. Each skin renders a city card derived from its Top 100 card and reuses its Top 100 chart screen with the badge number instead of "100".

**Tech Stack:** Kotlin, Jetpack Compose Material 3 (Expressive), AndroidX ViewModel + StateFlow, JUnit unit tests.

**Spec:** `docs/superpowers/specs/2026-11-14-city-charts-explore-design.md`

## Global Constraints

- Apple Music Style skin unchanged; no city charts on Home in any skin.
- No new dependencies, HTTP clients, caches or storage. Data only from `AppleCatalogPath.New` via existing `page(...)` / `cachedPage(...)`.
- No blocking work on the main thread; Top 100 never waits for cities; cached content stays visible during refresh.
- Empty city list renders nothing (no header, no spacer).
- City list: Apple page order, `distinctBy { playlistId }`, cap 30, country Top 100 charts excluded.
- Copy in all 42 languages of `LevyraDailyTop100Strings.kt`; EN "City charts" / "What each city is playing right now"; IT "Classifiche città" / "Cosa ascolta ogni città in questo momento". City names and badge come from Apple untranslated.
- No source-code comments. Stable lazy keys. 48dp targets, `contentDescription` = "<section title>, <city>", city names max 2 lines with ellipsis.
- Do not reproduce Apple's coloured grid tile outside the Apple skin.
- Commit steps run only with owner authorization; no Claude co-author trailer.
- Unit tests: `./gradlew.bat :app:testDebugUnitTest --tests <class>`; before commit `py -3.14 scripts/ai_quality_gate.py --profile fast`.

## Review Focus

- Storefront without city shelves (e.g. `ru`): rail absent, Top 100 unaffected. Pinned by `AppleCityChartsTest.noCityShelvesGivesEmptyList` (Task 1) and the empty-render rule in Task 3.
- New page fetch fails while Top 100 succeeds (and vice versa): the surviving list stays visible. Pinned by `DailyChartLoadTest.cityFailureKeepsTop100` / `top100FailureKeepsCities` (Task 2).
- Same playlist in two shelves, or "Top 100: Italia" also on the New page: no duplicates, no country chart in the city rail. Pinned in Task 1.
- Opening a city chart then a country chart quickly: only the last one publishes. Pinned by `DailyChartLoadTest.staleOpenDoesNotPublish` (Task 2).
- Long city names and font scale 2.0: text ellipsizes, badge never clipped. Checked in the emulator sweep (Task 7).

---

### Task 1: City chart parsing and repository access

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/data/apple/AppleMusicCatalogPage.kt` (next to `AppleDailyChart` / `AppleDailyTop100`, ~line 120)
- Modify: `app/src/main/java/com/luc4n3x/levyra/data/apple/AppleMusicEditorialRepository.kt:178-186`
- Test: `app/src/test/java/com/luc4n3x/levyra/data/apple/AppleCityChartsTest.kt`

**Interfaces:**
- Produces: `data class AppleCityChart(val playlistId: String, val badge: String, val name: String)`; `internal object AppleCityCharts { fun charts(page: AppleCatalogPage?): List<AppleCityChart> }`; `fun isCountryChart(playlistId: String): Boolean` on `AppleDailyTop100` (true for `GLOBAL_PLAYLIST_ID` or any `countryCharts` value); repository `suspend fun cityCharts(storefront: String, language: String): List<AppleCityChart>` and `suspend fun cachedCityCharts(storefront: String, language: String): List<AppleCityChart>`.

- [ ] **Step 1: Write the failing tests** in `AppleCityChartsTest`, building `AppleCatalogPage("it", shelves, 0L)` with `AppleCatalogItem(id, AppleCatalogItemKind.Playlist, title, subtitle)`:
  - `keepsCityCharts`: "Top 25: Milano" (`pl.aaa`), "Top 25: Roma" (`pl.bbb`) -> `[AppleCityChart("pl.aaa", "Top 25", "Milano"), AppleCityChart("pl.bbb", "Top 25", "Roma")]`.
  - `excludesCountryCharts`: "Top 100: Italia" with id `pl.737e067787df485a8062e2c4927d94db`, and "Top 100: Mondo" with an unknown id -> both absent.
  - `removesDuplicatesKeepsOrder`: `pl.bbb` in shelf 1 and shelf 2, `pl.aaa` second in shelf 1 -> ids `[pl.bbb, pl.aaa]`.
  - `capsAtThirty`: 40 distinct city items -> size 30.
  - `noCityShelvesGivesEmptyList`: `null` page, and a page of non-chart playlists -> `emptyList()`.
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests com.luc4n3x.levyra.data.apple.AppleCityChartsTest` — Expected: FAIL (unresolved `AppleCityCharts`).
- [ ] **Step 3: Implement** the types and `AppleCityCharts.charts` per the spec Data section (id via `AppleCatalogIds.playlist(item.id) ?: AppleCatalogIds.playlistIdFromUrl(item.url)`, parts via `AppleMusicHomeShelves.chartParts`, drop badge number 100 and `isCountryChart` ids), and both repository methods on `AppleCatalogPath.New`.
- [ ] **Step 4: Run** the same command, then `--tests com.luc4n3x.levyra.data.apple.AppleDailyTop100Test` — Expected: PASS both.
- [ ] **Step 5: Commit** `feat(explore): parse Apple city charts from the New page`.

### Task 2: Shared chart state in DailyTop100ViewModel

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/viewmodel/DailyTop100ViewModel.kt`
- Test: `app/src/test/java/com/luc4n3x/levyra/viewmodel/DailyChartLoadTest.kt`

**Interfaces:**
- Consumes: Task 1 types and repository methods.
- Produces: `sealed interface DailyChartRef { val playlistId: String }` with `data class Country(val chart: AppleDailyChart)` and `data class City(val chart: AppleCityChart)`; `DailyTop100UiState.cities: List<AppleCityChart> = emptyList()`; `DailyTop100ChartUiState.chart: DailyChartRef`; `fun open(chart: DailyChartRef)`; `retry()` / `close()` unchanged; pure helpers `internal fun mergeCharts(state: DailyTop100UiState, key: String, top100: List<AppleDailyChart>?, cities: List<AppleCityChart>?): DailyTop100UiState` (null = no new data, keep current) and `internal fun publishOpen(state: DailyTop100UiState, chart: DailyChartRef, tracks: List<Track>?): DailyTop100UiState`.

- [ ] **Step 1: Write the failing tests:**
  - `cityFailureKeepsTop100`: state with 3 Top 100 charts, `mergeCharts(state, state.key, top100 = null, cities = emptyList())` -> Top 100 unchanged, cities empty.
  - `top100FailureKeepsCities`: symmetric with 2 cities.
  - `staleKeyIgnored`: `mergeCharts` with another key -> state unchanged.
  - `staleOpenDoesNotPublish`: open = `City(pl.aaa)`, `publishOpen(state, Country(x), tracks)` -> state unchanged.
- [ ] **Step 2: Run** `./gradlew.bat :app:testDebugUnitTest --tests com.luc4n3x.levyra.viewmodel.DailyChartLoadTest` — Expected: FAIL.
- [ ] **Step 3: Implement.** `load` starts Top 100 and cities as two child coroutines of `loadJob` (cached then fresh each), both publishing through `mergeCharts`; `loading` clears when both finish. `open` uses `DailyChartRef.playlistId`, `MAX_TRACKS` unchanged, publishes through `publishOpen`.
- [ ] **Step 4: Run** the test — Expected: PASS. Compile errors remain in `ExploreDailyTop100.kt` until Task 3.
- [ ] **Step 5: Commit** together with Task 3, since the tree does not compile in between.

### Task 3: City rail, shared dialog, Default/Nocturne M3 Expressive card

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreCityCharts.kt`
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraCityChartsStrings.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreDailyTop100.kt:109-265` (dialog extraction, `DailyChartRef`, badge numeral in `Top100Numeral`)
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt` (`ExploreRow.CityCharts`; `buildExploreRows` inserts it right after `DailyTop100`)
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreEditorial.kt:105` and `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt:25205` (route `ExploreRow.CityCharts` like `DailyTop100`)
- Test: the existing `buildExploreRows` test (find with `rtk grep "buildExploreRows" app/src/test`)

**Interfaces:**
- Consumes: Task 2.
- Produces: `internal data class CityChartsCopy(val title: String, val subtitle: String)`; `internal fun cityChartsCopy(code: String): CityChartsCopy` via `localizedValueOrEnglish`; `ExploreRow.CityCharts` (key `explore-city-charts`); `@Composable internal fun ExploreCityCharts(onPlay: (List<Track>, Track) -> Unit, onTrackActions: (Track) -> Unit, modifier: Modifier = Modifier)`; `@Composable internal fun DailyChartDialog(onPlay: (List<Track>, Track) -> Unit, onTrackActions: (Track) -> Unit)`, rendered once from `ExploreDailyTop100`; chart screens take `badge: String` ("100" for countries, the digits of `AppleCityChart.badge` for cities).

- [ ] **Step 1: Write the failing test** `editorialRowsPlaceCityChartsAfterTop100`: `buildExploreRows(..., editorial = true)` -> index of `ExploreRow.CityCharts` == index of `ExploreRow.DailyTop100` + 1.
- [ ] **Step 2: Run** it — Expected: FAIL.
- [ ] **Step 3: Implement** row, routing, 42-language copy (same language keys as `dailyTop100Copies`), and `ExploreCityCharts`: emit nothing when `state.cities` is empty; header `LevyraSectionHeader(title, subtitle)` as Top 100; `LazyRow` keyed by `playlistId`, `contentType = "city-chart"`, `Top100CoverSize`, 12dp gap. Default/Nocturne `CityChartCover`: `colorScheme.tertiaryContainer` / `onTertiaryContainer`, city name in the Top 100 display style, small badge label, one `coverShapes()[slot % 4]` mark rotated `slot * 15f` with slots from `dailyTop100HueSlots` over city ids, `levyraPressable(role = Role.Button, pressedScale = LevyraPressScale.Tile)`.
- [ ] **Step 4: Run** the test and `./gradlew.bat :app:compileDebugKotlin` — Expected: PASS / BUILD SUCCESSFUL.
- [ ] **Step 5: Commit** (with Task 2) `feat(explore): city charts rail after Daily Top 100`.

### Task 4: City cards for Prism, Torque, Ivory, Mono, Spotify

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreCityCharts.kt`
- Modify: files defining `PrismTop100Card`, `TorqueTop100Card`, `IvoryTop100Card`, `MonoTop100Cover` and their chart screens (find with `rtk grep "fun (Prism|Torque|Ivory|Mono)Top100" app/src/main`), only to accept `badge`.
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/MonoExplore.kt:263` (item `mono-explore-city-charts` after the Top 100 item).

**Interfaces:**
- Consumes: Task 3 `ExploreCityCharts` and `badge`.
- Produces: per-skin `@Composable private fun <Skin>CityCard(chart: AppleCityChart, slot: Int, modifier: Modifier)`, same size as that skin's Top 100 card.

- [ ] **Step 1:** Branch in `ExploreCityCharts` exactly as `ExploreDailyTop100` does (`LevyraIsMono`, `LevyraIsIvory`, `LevyraIsTorque`, `LevyraIsPrismOnly`, `LevyraIsSpotify`); headers as Top 100 (`MonoSectionHeader`, `SkinSectionTitle(..., TorqueGutter)`).
- [ ] **Step 2:** Each city card reuses its skin's Top 100 materials, type and shape; city name leads; badge replaces the country numeral treatment.
- [ ] **Step 3: Run** `./gradlew.bat :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat(explore): skin city cards for Prism, Torque, Ivory, Mono, Spotify`.

### Task 5: YouTube Music and Atelier

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreCityCharts.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/YouTubeMusicExplore.kt:67` (add `ExploreRow.CityCharts` after `DailyTop100`)
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/AtelierExplore.kt:303` (call `ExploreCityCharts` after `ExploreDailyTop100`)
- Modify: files defining `YouTubeMusicTop100Shelf`, `AtelierTop100Shelf` and their chart screens, only to accept `badge`.

**Interfaces:**
- Consumes: Task 3.
- Produces: `YouTubeMusicCityShelf(...)` and `AtelierCityShelf(...)` mirroring their Top 100 shelf signatures with `List<AppleCityChart>`.

- [ ] **Step 1:** Implement both shelves from their Top 100 shelf, city-first composition.
- [ ] **Step 2: Run** `./gradlew.bat :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 3: Commit** `feat(explore): city charts for YouTube Music and Atelier`.

### Task 6: Qobuz Style Top 100 and city charts

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/QobuzCharts.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/QobuzMagazine.kt:105-115` (items `qobuz-top100` and `qobuz-city-charts` after `mag-carousel`, only when `state.section.isBlank()`)
- Modify: `ExploreDailyTop100.kt` and `ExploreCityCharts.kt` (`LevyraIsQobuz` branches, dialog branch)

**Interfaces:**
- Consumes: Tasks 2-3; `QobuzMagazineInputs.onPlay` and `onTrackActions` (already present, lines 68-69).
- Produces: `QobuzTop100Card`, `QobuzCityCard`, `QobuzChartScreen(open: DailyTop100ChartUiState, label: String, badge: String, copy: DailyTop100Copy, onBack: () -> Unit, onRetry: () -> Unit, onPlay: (List<Track>, Track) -> Unit, onTrackActions: (Track) -> Unit)` built from `QobuzDesign`, `QobuzText`, `QobuzRowPadding` and Qobuz track rows.

- [ ] **Step 1:** Implement cards and chart screen in `QobuzCharts.kt` from existing Qobuz components; Qobuz section header style.
- [ ] **Step 2:** Wire the two magazine items; each hides when its list is empty.
- [ ] **Step 3: Run** `./gradlew.bat :app:compileDebugKotlin` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 4: Commit** `feat(qobuz): Daily Top 100 and city charts in the magazine`.

### Task 7: Validation sweep

- [ ] **Step 1: Run** `./gradlew.bat :app:testDebugUnitTest` — Expected: BUILD SUCCESSFUL.
- [ ] **Step 2: Run** `py -3.14 scripts/ai_quality_gate.py --profile fast` — Expected: PASS (missing prerequisites reported BLOCKED).
- [ ] **Step 3:** Install a debug build with `-PlevyraFdroidBuild=true` on the owner-started emulator. One sweep across Default, Nocturne 78, Prism, Torque, Ivory, Spotify, Mono, YouTube Music, Atelier, Qobuz: city rail right after Top 100, tap opens a chart showing "25", play works, track menu works, back closes. A storefront without city shelves (e.g. `ru`): rail absent. Default at font scale 2.0, in a long language, in light and dark.
- [ ] **Step 4:** Review the whole branch with `git diff`, run `git diff --check`, then local CodeRabbit CLI; fix findings.
- [ ] **Step 5:** Report PASS/FAIL/BLOCKED/UNRUN per check; stop the debug app on the emulator.
