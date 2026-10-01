# Premium Playlist Detail Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make normal Levyra playlists and Levyra Collections open into an artwork-led, premium detail screen with Spotify-like hierarchy while preserving every existing playlist and playback behavior.

**Architecture:** Keep `LevyraPlaylistDetailScreen` as the state/orchestration owner and `PlaylistDetailHeader` as the presentation/action owner. Reuse Levyra's already-shipped playlist/album visual primitives — `ArtworkPaletteCache`, `rememberArtworkPalette`, `ArtworkBackdropWash`, `immersiveMediaColors`, `animatedImmersiveMediaColors`, `ImmersiveMediaActionRow`, and `ImmersiveMediaTopBar` — instead of adding a new palette or blur system. Keep `LibraryTrackRow`, selection, reorder, download, search, queue, and ViewModel callbacks intact.

**Tech Stack:** Kotlin, Jetpack Compose, Coil-backed existing artwork cache, Levyra immersive-media components, JUnit.

**Spec:** `docs/superpowers/specs/2026-10-01-premium-home-playlist-detail-design.md`

## Global Constraints

- Android only; no navigation-model change.
- Preserve all existing playlist actions: back, Play, Shuffle, download/export, search, rename/edit, reorder/save order, cover management, overflow actions, selection, queue, and track playback.
- Do not add a new queue/player controller in Compose.
- Reuse the process-wide artwork/palette cache; no new image-processing dependency and no synchronous decode in composition.
- Static content must render with the neutral fallback palette before optional artwork colors arrive.
- Keep icon actions at least 48dp and preserve localization/content descriptions.
- No database/provider/recommendation/playback/version changes.

## Review Focus

- A playlist with no custom cover must still get a stable artwork source/fallback and must never crash with an empty track list.
- Very long playlist titles/metadata and large font scale must not cover the Play action or artwork.
- Search/selection/reorder modes must keep their existing back-button semantics after the visual shell changes.
- Palette loading/failure must fall back to Levyra neutral colors without blanking the screen or delaying actions.
- A currently playing track, downloads, selection and reorder rows must retain their existing state indicators and callbacks.

---

### Task 1: Define playlist artwork/palette selection as pure presentation logic

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/library/PlaylistDetailPresentation.kt`
- Create: `app/src/test/java/com/luc4n3x/levyra/ui/library/PlaylistDetailPresentationTest.kt`

**Interfaces:**
- Produces: `internal fun playlistDetailArtworkUrl(playlist: Playlist): String`
- Produces: `internal fun playlistDetailPaletteKey(playlist: Playlist, artworkUrl: String): String`
- Artwork precedence: non-blank `playlist.coverUrl` when `playlist.coverMode` is `PlaylistCoverMode.CUSTOM` (matching `PlaylistCoverArt`), then first track `largeThumbnailUrl`, then first track `thumbnailUrl`, else empty string.
- Palette key: empty when artwork URL is empty; otherwise `ArtworkPaletteCache.key(trackId = "playlist:${playlist.id}", thumbnailUrl = artworkUrl, largeThumbnailUrl = artworkUrl)`.

- [ ] **Step 1: Write failing tests**

Cover custom cover, a non-blank `coverUrl` with `coverMode = AUTO` falling back to the first track, first-track fallback, empty playlist, and deterministic palette-key behavior.

- [ ] **Step 2: Run focused test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.library.PlaylistDetailPresentationTest --console=plain`
Expected: FAIL because the helpers do not exist.

- [ ] **Step 3: Implement the two pure helpers**

Keep these helpers free of Compose state and I/O.

- [ ] **Step 4: Run focused test and verify GREEN**

Run the same command.
Expected: PASS.

- [ ] **Step 5: Commit**

`git commit -m "test(library): define playlist detail artwork contract"`

### Task 2: Build the artwork-led playlist shell with existing palette infrastructure

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt` in `LevyraPlaylistDetailScreen`.
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryActions.kt` in `PlaylistDetailHeader`.
- Create: `app/src/test/java/com/luc4n3x/levyra/ui/library/PlaylistDetailPremiumContractTest.kt`

**Interfaces:**
- Consumes: `playlistDetailArtworkUrl`, `playlistDetailPaletteKey` from Task 1.
- Reuses: `ArtworkPalette`, `rememberArtworkPalette`, `ArtworkBackdropWash`, `immersiveMediaColors`, `animatedImmersiveMediaColors`, `ImmersiveMediaColors`.
- Preserves the existing `PlaylistDetailHeader` callbacks and `LevyraPlaylistDetailScreen` ViewModel calls.

- [ ] **Step 1: Write failing premium-shell contract tests**

The contract test must isolate `LevyraPlaylistDetailScreen`/`PlaylistDetailHeader` and assert:
- the screen uses `rememberArtworkPalette` and `ArtworkBackdropWash`;
- colors flow through `immersiveMediaColors`/`animatedImmersiveMediaColors`;
- the header renders `PlaylistCoverArt` rather than replacing playlist cover modes with a one-off remote image;
- the primary action path still contains Play, Shuffle and download callbacks;
- rename/reorder/search/cover/overflow callbacks remain in the header contract.

- [ ] **Step 2: Run the new contract test and verify RED**

Run: `./gradlew :app:testDebugUnitTest --tests com.luc4n3x.levyra.ui.library.PlaylistDetailPremiumContractTest --console=plain`
Expected: FAIL against the current isolated-card header.

- [ ] **Step 3: Add palette/background ownership to `LevyraPlaylistDetailScreen`**

Inside the existing screen:
- compute `artworkUrl` and `paletteKey` from Task 1;
- use the same neutral fallback colors already used by `PlaylistHitOverlay` (`AlbumNeutralPaletteStart`/`AlbumNeutralPaletteEnd`);
- resolve palette with `rememberArtworkPalette`;
- map it through `immersiveMediaColors` and `animatedImmersiveMediaColors`, keyed by current playlist and `state.animationsEnabled`;
- wrap the existing detail list in `BoxWithConstraints`/background ownership and render `ArtworkBackdropWash(artworkUrl, colors.fieldTop, colors.base)` behind it;
- preserve the existing `LazyListState`, content padding, selection bar and now-playing dock ownership.

- [ ] **Step 4: Recompose `PlaylistDetailHeader` as one continuous hero**

Keep the current callback surface. Replace the old isolated card treatment with:
- centered square `PlaylistCoverArt` in portrait, capped around 260–280dp and never wider than the content area;
- side-by-side artwork/metadata on wide layouts where current responsive helpers already support it;
- title up to 2 lines, metadata/track count + formatted duration below it;
- no hard card boundary between header and track list;
- palette-driven content colors from `ImmersiveMediaColors`.

Do not hide existing playlist management actions; relocate them into the top/action/overflow areas.

- [ ] **Step 5: Run the focused tests**

Run `PlaylistDetailPresentationTest` and `PlaylistDetailPremiumContractTest`.
Expected: PASS.

- [ ] **Step 6: Commit**

`git commit -m "style(library): add artwork-led playlist detail"`

### Task 3: Premium action hierarchy and collapsing top bar

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryActions.kt`
- Reuse: `app/src/main/java/com/luc4n3x/levyra/ui/media/ImmersiveMediaHero.kt` primitives without changing existing default behavior for Album/PlaylistHit callers.
- Test: `app/src/test/java/com/luc4n3x/levyra/ui/library/PlaylistDetailPremiumContractTest.kt`

**Interfaces:**
- Reuses: `ImmersiveMediaActionRow`, `ImmersiveMediaPrimaryAction`, `ImmersiveMediaTopBar`.
- Preserves: existing `onBack` semantics, including search/selection/reorder handling before closing the playlist.

- [ ] **Step 1: Extend the failing contract**

Require the playlist detail to use `ImmersiveMediaActionRow` with Play as primary, Shuffle as secondary, and existing download behavior; require `ImmersiveMediaTopBar` for back/title/overflow presentation.

- [ ] **Step 2: Run the contract test and verify RED**

Expected: FAIL before the new action/top-bar composition exists.

- [ ] **Step 3: Implement action hierarchy**

Use `ImmersiveMediaActionRow` for Play + Shuffle + Download. Keep search and management controls in a compact secondary rail/top-bar menu with the existing localized labels and callbacks. Do not duplicate actions in both places unless the existing mode requires it.

- [ ] **Step 4: Implement collapsed title/back behavior**

Derive collapsed state from the existing playlist `LazyListState` like `PlaylistHitOverlay` does. Overlay `ImmersiveMediaTopBar` using the same palette colors and route its back callback through the existing playlist-detail back logic, not directly to `closePlaylist()`.

- [ ] **Step 5: Verify empty/one-track behavior**

Play is disabled for an empty playlist; Shuffle is enabled only when more than one track exists; download follows the existing eligibility path.

- [ ] **Step 6: Run focused tests and commit**

Run both playlist-detail focused test classes.
Commit: `git commit -m "style(library): refine playlist actions and top bar"`

### Task 4: Preserve track-list behavior under the new visual shell

**Files:**
- Modify only if required: `app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt`
- Reuse unchanged where possible: `app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryRows.kt`
- Tests: existing playlist drag/selection/library tests plus the new premium contract tests.

**Interfaces:**
- Consumes: existing `LibraryTrackRow`, selection state, reorder frame loop, download/favorite/queue callbacks.
- Produces: the same behavior with a visually connected palette background.

- [ ] **Step 1: Keep `LibraryTrackRow` as the row implementation**

Do not create a new track-row component merely for the redesign. Adjust only outer padding/background if needed for the continuous detail composition.

- [ ] **Step 2: Verify stateful paths**

Run the existing playlist/library tests that cover selection and reorder, plus `PlaylistDetailPremiumContractTest`.
Expected: PASS.

- [ ] **Step 3: Inspect callback preservation**

Confirm the diff still routes every play/shuffle/download/search/rename/reorder/cover/selection/queue action through the same existing ViewModel methods.

- [ ] **Step 4: Commit only if code changed in this task**

`git commit -m "style(library): connect playlist tracks to immersive detail"`

### Task 5: Playlist-detail verification gate

**Files:**
- Review the complete playlist-detail diff only.

**Interfaces:**
- Produces: independently testable premium playlist detail suitable for the same final PR as the Home plan.

- [ ] **Step 1: Run focused playlist tests**

Run the two new test classes plus relevant existing library/playlist drag-selection tests.

- [ ] **Step 2: Run repository quality gate**

Run: `python3 scripts/ai_quality_gate.py --profile fast`
Expected: PASS; diagnose failures rather than weakening checks.

- [ ] **Step 3: Inspect final diff and whitespace**

Run `git diff --check`. Verify no new dependency, provider/player/database/version change, and no duplicate artwork cache/palette implementation.

- [ ] **Step 4: Manual runtime targets when available**

Open both a normal playlist and a Levyra Collection; check portrait, large font and one wider layout; verify Play, Shuffle, Download, Search, More, selection and reorder entry points. If runtime/device access is unavailable, report these as unverified rather than passed.

### Task 6: Whole-branch review and PR publication

**Files:**
- Review both plan outputs and the design spec.

- [ ] Run `python3 scripts/ai_quality_gate.py --profile full` before publication when the environment permits.
- [ ] Run the complete applicable Android unit/lint/release checks available locally; otherwise rely on PR CI and label missing prerequisites as blocked/unverified.
- [ ] Inspect the complete branch diff against `main` and run `git diff --check` after the last material edit.
- [ ] Open one dedicated draft PR from `feat/premium-home-playlist-detail` to `main` with truthful validation notes.
- [ ] Review CodeRabbit findings individually; fix only still-valid issues, rerun affected checks, and leave no unresolved valid thread before declaring merge-ready.
