# Premium Home Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Rebalance Levyra Android Home so useful music appears sooner and `La tua orbita` becomes a dense, premium quick-pick shelf rather than a large 3×3 artwork grid.

**Architecture:** Keep the existing Home state projection, content sources, callbacks, lazy-list ownership, and playback behavior. Replace only the Personal Orbit presentation, tighten the hero proportion through existing `LevyraHomeDesign` tokens, and reuse the current compact track-row rhythm instead of introducing a second Home design system. Preserve the Levyra Collections implementation merged from PR #801.

**Tech Stack:** Kotlin, Jetpack Compose, existing Levyra Home components/tokens, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-01-premium-home-playlist-detail-design.md`

## Global Constraints

- Android only; no Windows changes.
- Preserve Home data sources, recommendation ordering, playback callbacks, navigation, cached-content behavior, and refresh policy.
- No new dependency, image loader, palette engine, blur system, or background work.
- Keep stable media identity for lazy content.
- Keep actionable controls at least 48dp even when the visual row is compact.
- Preserve the current Levyra Collections single-row contract and text/artwork separation.
- Do not change app version values.

## Review Focus

- Empty, one-track, partial-column, and full 20-track Orbit inputs must render without dropping items or empty columns.
- Duplicate recordings/works must remain deduplicated before layout, preserving the existing Personal Orbit semantics.
- Long title/artist text must stay beside artwork and never overlap the overflow action.
- A narrow phone must show one useful Orbit column plus a next-column peek without horizontal clipping.
- The existing Levyra Collections contract must still pass unchanged after Home rhythm adjustments.

---

### Task 1: Lock the Personal Orbit column contract

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/HomePersonalOrbitLayout.kt`
- Create: `app/src/test/java/com/luc4n3x/levyra/ui/HomePersonalOrbitLayoutTest.kt`

**Interfaces:**
- Produces: `internal fun homePersonalOrbitColumns(tracks: List<Track>, limit: Int = LevyraPersonalOrbit.DISPLAY_LIMIT): List<List<Track>>`
- Contract: deduplicate with `LevyraPersonalOrbit.distinctRecordings` + `distinctWorks`, cap at `limit`, then emit sequential columns of exactly `ORBIT_TRACKS_PER_PAGE = 4` except the final partial column.

- [ ] **Step 1: Write failing layout tests**

Add tests asserting:

```kotlin
assertEquals(listOf(listOf(0,1,2,3), listOf(4,5,6,7), listOf(8)), indexes(homePersonalOrbitColumns(tracks(9))))
assertEquals(5, homePersonalOrbitColumns(tracks(20)).size)
assertTrue(homePersonalOrbitColumns(emptyList()).isEmpty())
```

Also add one duplicate-recording/work case proving duplicates do not create extra rows.

- [ ] **Step 2: Run the focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.HomePersonalOrbitLayoutTest --console=plain`
Expected: FAIL because `homePersonalOrbitColumns` does not exist.

- [ ] **Step 3: Implement the pure layout helper**

Create `homePersonalOrbitColumns(tracks, limit)` with the exact signature above. Keep the 4-row column size as the single layout rule; do not move playback/UI logic into this file.

- [ ] **Step 4: Run the focused test and verify GREEN**

Run the same Gradle command.
Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "test(home): define premium Orbit layout"`

### Task 2: Replace the Orbit grid with compact horizontal columns

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt` in `PersonalListeningShelf` and the old `PersonalOrbitSpeedDialCard` area.
- Test: `app/src/test/java/com/luc4n3x/levyra/ui/HomePersonalOrbitLayoutTest.kt`
- Test: create `app/src/test/java/com/luc4n3x/levyra/ui/HomePremiumLayoutContractTest.kt`

**Interfaces:**
- Consumes: `homePersonalOrbitColumns(tracks)` from Task 1.
- Preserves: `onPlay`, `onPlayAll`, `onTrackActions`, current/playing/resolving state, Personal Orbit source order.
- Produces: a `LazyRow` of 4-row columns using stable media keys.

- [ ] **Step 1: Write the failing source/layout contract test**

The contract must isolate the `PersonalListeningShelf` block and assert it contains `LazyRow`, uses `homePersonalOrbitColumns`, retains `onPlayAll`, and no longer contains `HorizontalPager`, `SPEED_DIAL_COLUMNS`, or `PersonalOrbitSpeedDialCard`.

Also assert the Orbit row component keeps separate artwork/text/action regions and a minimum 48dp clickable height.

- [ ] **Step 2: Run the contract test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.HomePremiumLayoutContractTest --console=plain`
Expected: FAIL against the old pager/grid implementation.

- [ ] **Step 3: Implement the new Orbit shelf**

In `PersonalListeningShelf`:
- replace the 3×3 `HorizontalPager` grid with a horizontally scrolling `LazyRow`;
- use `homePersonalOrbitColumns` and `ORBIT_TRACKS_PER_PAGE = 4`;
- size each column from the existing dense-shelf bounds (`HOME_DENSE_SHELF_MIN_WIDTH = 286.dp`, `HOME_DENSE_SHELF_MAX_WIDTH = 338.dp`) and keep the existing peek/end-padding constants;
- render each row with 48dp artwork, title, artist, and the existing overflow callback;
- use a distinct minimal Orbit row treatment rather than copying the full `Scelte rapide` card surface;
- use a stable key based on `LevyraPersonalOrbit.identityKey(track)` plus column/row fallback only where necessary;
- keep current/playing/resolving indicators and touch targets.

Remove the now-unused `PersonalOrbitSpeedDialCard` implementation and pager-only imports/constants only if no other call site uses them.

- [ ] **Step 4: Run focused tests**

Run both `HomePersonalOrbitLayoutTest` and `HomePremiumLayoutContractTest`.
Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "style(home): turn Orbit into a premium quick shelf"`

### Task 3: Reduce hero dominance and unify Home rhythm

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/theme/HomeDesign.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt` only where hero padding/spacing is owned locally.
- Modify: `app/src/test/java/com/luc4n3x/levyra/ui/theme/HomeDesignTest.kt`
- Preserve: `app/src/test/java/com/luc4n3x/levyra/ui/HomeCollectionsLayoutContractTest.kt`

**Interfaces:**
- Produces: a compact hero token and unchanged 16dp horizontal content inset.
- Exact design target: change `LevyraHomeDesign.HeroHeight` from `472.dp` to `408.dp`; keep `TrackRowHeight = 60.dp`, `TrackThumbSize = 48.dp`, and current collection-card dimensions.

- [ ] **Step 1: Write the failing hierarchy test**

Add an assertion in `HomeDesignTest` that `HeroHeight == 408.dp`, while retaining the existing minimum hierarchy and touch-target assertions.

- [ ] **Step 2: Run `HomeDesignTest` and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.theme.HomeDesignTest --console=plain`
Expected: FAIL at the new hero-height assertion.

- [ ] **Step 3: Apply the minimal hero/rhythm change**

Set `HeroHeight` to `408.dp`. Adjust only hero-local vertical padding if needed so label/title/subtitle/play remain balanced inside the smaller height. Do not globally shrink album/video/artist artwork or the Collections cards.

- [ ] **Step 4: Run Home regression tests**

Run:
`./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.theme.HomeDesignTest --tests com.luc4n3x.levyra.ui.HomeCollectionsLayoutContractTest --tests com.luc4n3x.levyra.ui.HomeQuickAccessLayoutTest --tests com.luc4n3x.levyra.ui.HomePersonalOrbitLayoutTest --tests com.luc4n3x.levyra.ui.HomePremiumLayoutContractTest --console=plain`
Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "style(home): rebalance premium Home proportions"`

### Task 4: Home verification gate

**Files:**
- Review the complete Home diff only.

**Interfaces:**
- Produces: independently shippable premium Home redesign before playlist-detail work begins.

- [ ] **Step 1: Run the fast repository quality gate**

Run: `python3 scripts/ai_quality_gate.py --profile fast`
Expected: PASS; otherwise diagnose instead of weakening tests.

- [ ] **Step 2: Inspect the diff**

Run `git diff --check` and inspect all changed Home files. Confirm no recommendation/provider/playback/persistence logic changed and `HomeCollectionsLayoutContractTest` remains green.

- [ ] **Step 3: Manual visual check when runtime access exists**

Check phone portrait, large font, and one wider width. Verify the first viewport exposes content sooner; Orbit shows four readable rows per column with a visible next-column cue; scrolling does not clip overflow controls. If no device/emulator is available, report visual/accessibility runtime validation as unverified.

- [ ] **Step 4: Commit any test-only corrections, then stop at a clean Home checkpoint**
