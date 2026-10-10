# Premium Home + Playlist Detail Design

Date: 2026-10-01
Owner: Levyra Android
Branch: `feat/premium-home-playlist-detail`

## Goal

Make Levyra's Android Home and playlist detail feel proportioned, intentional, and premium without copying YouTube Music or Spotify verbatim and without changing playback, recommendation, persistence, or navigation ownership.

The Home should surface more useful music earlier, with stronger hierarchy and less oversized empty space. Opening a playlist, including a Levyra Collection, should transition into an immersive detail page with artwork-led color, clear actions, and a compact track list.

## Reference intent

The supplied references are used for principles, not pixel cloning.

Useful ideas to carry into Levyra:

- YouTube Music: dense discovery shelves, compact quick-pick rows, consistent artwork proportions, predictable section rhythm, visible next-column peek.
- Spotify Home: disciplined card proportions and section hierarchy.
- Spotify playlist detail: large centered artwork, artwork-derived tonal background, clear title/metadata block, compact action rail, prominent play action, and track rows that begin naturally below the header.

Levyra keeps its own typography, colors, navigation, icons, interactions, playback model, and collection identity.

## Design dials

### Home

- Visual variance: 6/10
- Motion intensity: 3/10
- Information density: 7/10

### Playlist detail

- Visual variance: 7/10
- Motion intensity: 3/10
- Information density: 5/10

## Scope

### 1. Home composition

Preserve the current section set and existing data sources. Rebalance presentation only.

#### Header and hero

- Keep the Levyra brand header, search/settings entry points, and mood chips.
- Reduce the visual dominance of the current hero so useful content appears sooner on common phone heights.
- Keep the hero artwork-led and immersive, but reduce vertical occupation and simplify spacing around its label, title, subtitle, and play action.
- Preserve the existing hero data, action callbacks, navigation, and playback behavior.

#### La tua orbita

Owner decision (revised): render Orbit as a paged 3x3 artwork grid modeled on YouTube Music's "Selezione rapida", so it stays visually distinct from the compact Scelte rapide rows. Proportions follow measurements of the YouTube Music screen: about 121dp tiles on a 412dp phone, 7dp corners, 4.5dp gaps, 15sp tile titles, a 24sp section title and 8dp page indicators.

The Orbit presentation should:

- show a header with the listener's profile photo (or initial) and name above the `La tua orbita` title only when a name is set, and keep `Riproduci tutto`; the photo is picked in Settings, cropped to 256px and stored only in app-private storage;
- lay tracks out as 3x3 pages of square artwork tiles with a single-line bold title over a bottom scrim;
- page horizontally with page indicators on compact widths;
- place pages side by side on wide windows (600dp and up) instead of enlarging the tiles;
- keep long-press for the existing track actions and the now-playing indicator on the current track;
- preserve stable track identity and the existing Orbit source/order;
- show only full pages once there is more than one page.

#### Scelte rapide and other shelves

- Keep existing content and behavior.
- Align track row height, thumbnail size, section title spacing, horizontal gaps, and edge insets with the new Orbit rhythm.
- Preserve section differentiation so Orbit and Scelte rapide are not visually identical duplicates.
- Keep album, artist, video, radio, and Levyra Collections layouts recognizable.

#### Levyra Collections

Preserve the existing single-row editorial card direction merged from PR #801.

- Keep colored collection cards, `LEVYRA` label, title, metadata, and artwork treatment.
- Only adjust surrounding spacing or sizing if needed for the new global Home rhythm.
- Do not regress text/artwork separation or reintroduce ellipsis where the current contract intentionally allows wrapping.

### 2. Playlist detail redesign

Redesign the existing Android playlist detail presentation using the existing playlist state and actions.

#### Header composition

The top of the screen should contain:

- back navigation in the existing navigation flow;
- a large, centered playlist cover with a sensible maximum width;
- an artwork-derived tonal background or gradient that transitions into Levyra's dark canvas;
- playlist title, optional description/metadata, creator/source where available, track count/duration where already known;
- the existing action set, reorganized into a cleaner action rail;
- a prominent primary Play button;
- Shuffle available as a secondary playback action;
- existing download, search, rename/edit, reorder, cover, and overflow behavior retained where currently supported.

The header should collapse visually into the track list through spacing and gradient rather than through a hard card boundary.

#### Track list

- Keep the current track source, selection, reorder, download, and overflow behavior.
- Use compact but readable rows with artwork where the current data supports it.
- Maintain clear current/playing state.
- Preserve long-press, selection, and reorder semantics.
- Avoid introducing a second queue or playback controller in Compose.

#### Artwork-derived color

Use the existing artwork/theme infrastructure where possible.

- Prefer a lightweight sampled/accent color already available from existing image/palette utilities.
- Do not add a new heavyweight blur or image-processing dependency.
- If a palette cannot be resolved, fall back to Levyra's existing dark surface tokens.
- The tonal treatment must not delay static content or playback actions.

### 3. Navigation and behavior

No navigation model change.

Opening a Levyra Collection should continue through the existing collection/playlist detail path. The visual result should use the same premium playlist-detail language where technically compatible, rather than introducing a separate one-off screen.

All existing playback, download, queue, playlist management, selection, search, reorder, and state-restoration behavior remains owned by current ViewModels/controllers.

## Reuse and implementation boundaries

Primary expected files:

- `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt`
- `app/src/main/java/com/luc4n3x/levyra/ui/theme/HomeDesign.kt`
- `app/src/main/java/com/luc4n3x/levyra/ui/library/LevyraLibraryScreen.kt`
- `app/src/main/java/com/luc4n3x/levyra/ui/library/LibraryActions.kt`
- nearby Android UI tests / layout contract tests

Additional theme or reusable UI files may be touched only if the same visual primitive is needed by at least two real call sites.

Do not modify provider logic, playback resolution, Media3 ownership, database schema, recommendation ranking, home refresh policy, downloads, or app version.

## State and performance constraints

- Keep Home's existing reduced screen projection and avoid broad playback-tick recomposition.
- Keep lazy-list keys stable and based on media identity.
- Do not perform palette extraction, file reads, network work, or image decoding synchronously in composition.
- Reuse the process-wide Coil image loader and existing artwork caches.
- Do not add full-screen blur, backdrop capture, animated mesh gradients, or unbounded decoded artwork retention.
- Cached content stays visible during refresh.
- Static artwork/content should render before optional tonal decoration.

## Accessibility and localization

- No new hardcoded user-facing strings.
- Preserve or improve content descriptions for icon-only actions.
- Keep actionable touch targets at least 48dp even when rows look visually compact.
- Verify long titles, artist names, playlist names, and translations do not overlap artwork or actions.
- Keep layout RTL-safe and avoid directional padding assumptions where possible.
- Large font scale may expand header text vertically; it must not clip primary controls.
- Preserve logical focus/traversal order.

## Motion

Motion is restrained.

Allowed:

- existing press feedback;
- modest artwork/header transition already supported by navigation;
- subtle tonal/background crossfade when artwork color becomes available.

Not in scope:

- parallax hero motion;
- scroll-linked blur;
- large-scale spring choreography;
- continuous decorative animation.

## Testing strategy

### Focused tests

Add or update UI/layout contract tests to verify:

- Orbit uses the paged 3x3 artwork grid contract;
- Orbit tiles keep playback, long-press actions and the now-playing indicator;
- stable item keys remain present;
- Home section spacing/tokens remain within the intended hierarchy;
- playlist header retains Play and Shuffle actions;
- playlist-detail header uses an artwork-led immersive container rather than the previous isolated card treatment;
- collection-card readability contracts from PR #801 remain intact.

### Broader checks

Before PR publication:

- run focused unit tests for changed UI contracts;
- run repository AI quality gate full profile where the execution environment permits;
- run Android unit tests / lint / release compile through the existing PR CI;
- inspect the final diff and `git diff --check` equivalent;
- review CodeRabbit findings individually and only change still-valid issues.

### Manual validation targets

Where runtime/device access is available:

- phone portrait Home at normal and large font scale;
- at least one wider width / landscape check;
- long Italian/English text and one RTL locale if available;
- open a normal playlist and a Levyra Collection;
- verify Play, Shuffle, download, search, overflow, selection, and reorder entry points remain functional;
- scroll Home and playlist detail for obvious jank or clipping.

Runtime visual quality, TalkBack behavior, and frame performance remain unverified unless directly exercised on a running app/device.

## Acceptance criteria

The change is accepted when:

1. The first Home viewport exposes useful music sooner than the current oversized composition.
2. `La tua orbita` reads as a YouTube Music style "Selezione rapida" grid that stays readable on compact and wide windows.
3. Section spacing, thumbnails, titles, and peeking behavior feel internally consistent across Home.
4. Levyra Collections remains visually distinct and readable.
5. Opening a playlist presents a large artwork-led header with tonal background, clear metadata, compact actions, and a prominent Play control.
6. Track lists remain fully functional and visually connected to the header.
7. No playback/provider/persistence behavior changes are introduced.
8. Focused tests, lint, and applicable CI checks pass after the final edit.
9. No unresolved valid CodeRabbit review finding remains before merge readiness.

## Out of scope

- redesigning Now Playing;
- changing bottom navigation structure;
- changing recommendation algorithms;
- adding Spotify/YouTube account integration;
- copying proprietary assets or exact branded UI geometry;
- adding new third-party UI/image-processing dependencies;
- changing Windows UI;
- release/version bump.
