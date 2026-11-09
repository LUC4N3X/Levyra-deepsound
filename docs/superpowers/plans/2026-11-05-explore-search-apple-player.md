# Explore, Search Browse and Apple Player Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking. Owner rule for this repository: no subagents; run tasks inline.

**Goal:** Make three surfaces feel like a real music app: (A) the Apple-style player blends the full-bleed cover into a backdrop built from the cover's own colours, with a device volume row; (B) Search owns browsing (new releases, moods, genres) on every skin that has no Explore of its own; (C) Explore becomes an editorial music page (news, the week's releases, radio and podcasts, Mood Journey, Levyra Mix) instead of a grid of tool tiles.

**Architecture:** All three phases reuse existing owners. The player keeps `AppleMusicPlayerScene` and `PlayerDeckSlots`. It only replaces the two-tone gradient and the floating card in portrait with a mesh backdrop and a dissolving full-bleed artwork. Search reuses `spotifyBrowseEntries` and the existing `SearchViewModel.openExploreZone/openExploreCategory`. Explore keeps `buildExploreRows` and `ExploreContentRow` with a new row set. News is the one new data source: a small RSS repository on `LevyraHttpClientFactory.feeds`, shaped like `QobuzEditorialRepository.magazine` (memory map, disk text cache, in-flight dedupe, TTL).

**Tech Stack:** Kotlin, Jetpack Compose Material 3, Coil 3 (`SingletonImageLoader`), OkHttp feeds client, `javax.xml.parsers` (platform, no dependency), JUnit.

**Evidence base (read 2026-11-05):**
- Levyra Apple player: `app/src/main/java/com/luc4n3x/levyra/ui/player/AppleMusicPlayer.kt:104-130`. The backdrop is `Brush.verticalGradient(field → deep)` from `rememberSpotifyCoverTone`. Artwork is a shadowed rounded card that scales to 0.82 on pause. There is no volume control.
- Reference (GPL-3.0, compatible): SimpMusic `013db77728e788ae9866d6fa763dfa5aa7025c1e`.
  - `composeApp/.../player/content/applemusic/AppleMusicMeshBackdrop.kt`: 6×6 cell averages of the cover, read bottom-up. Rows ≥1 get a seeded cyclic shift plus an optional mirror, then a smoothstep resample to 32×32. Saturation is lifted ×1.12 with a lightness floor of 0.045, and the result is blurred at 44dp. A 900ms crossfade runs between meshes, under a 6%→30% black scrim.
  - `NowPlayingContentAppleMusic.kt:400-520, 910-1010`: the artwork is full width with height `min(width, 0.62·h)` and starts under the status bar. It dissolves with a DstIn smoothstep mask (42% bottom, status-bar-height top). The mesh is "hung" from the artwork's bottom edge (`seam`): mesh row 0, which is the cover's bottom band, is held behind the artwork, so the join has no visible seam. A wash under the status bar runs from 16% to 65% black, driven by the cover's top-band luminance.
  - `expect/ui/DeviceVolumeController.android.kt`: STREAM_MUSIC slider with flag 0, so no system volume UI.
  - Radio: SimpMusic has no radio screen. Radio is "start radio from a seed": a Home row seeded from the user's songs plus the artist-page radio button. Levyra already has `startSongRadioFrom(track)`, `LiveRadioScreen`, `HomeFavoriteRadio` and `CustomRadioDialog`.
- Levyra already has a device volume implementation: `ui/player/PlayerDeckAura.kt:531` `PlayerOutputVolume` (ContentObserver, flag 0). Reuse it rather than port the poll loop.
- Explore dispatch: `ui/LevyraApp.kt:25703-25750`. Atelier, Qobuz and YTM have their own Explore. Every other skin goes through `buildExploreRows` (`ui/ExploreLayout.kt:113`).
- Search empty state: `ui/LevyraApp.kt:15304`. Spotify gets `spotifySearchBrowseItem`, Apple gets `appleMusicSearchLanding`, and all other skins only get `SearchTasteHints`.
- RSS probe (2026-11-05, HTTP 200): Pitchfork news (30 items, media), Stereogum (media), Consequence (media), NME (inline img), Billboard (inline img), Rockol (inline img), All Music Italia (partial media), Rolling Stone Italia (text only), Musikexpress (media), Les Inrocks musique (media), InterMedia RU (text only), Mondo Sonoro (text only).

## Owner decisions (2026-11-05)

- News source: public RSS feeds chosen by language. No login, no cookies, no tracking. Articles open in the browser.
- Scope of the new Explore and Search browse: every skin without its own Explore, which means `LevyraIsDefaultSkin`, Nocturne, Prism, Torque and Ivory. Spotify and Apple keep their existing Search browse and Explore. Atelier, Qobuz and YTM are untouched.
- Apple player: portrait mode matches the reference behaviour (mesh plus dissolving full-bleed cover plus volume row). Landscape, video and immersive Canvas keep their current layout but stand on the mesh.

## Owner revisions during implementation (2026-11-05, supersede Phase C rows below)

- Editorial Explore is purely editorial: masthead with edition date, news front page (lead + two stories), this week's releases grid with "See all", Daily Top 100, more stories filtered by source. `RadioHub`, `MoodJourney`, `MixTools` and "Tracks of the moment" are not part of editorial Explore.
- Daily Top 100 (Apple room `6504092645`) is added to Explore with Levyra-drawn covers.
- Levyra Mix and Mood Journey move to the Library overview for the same skins.
- Bottom navigation for the same skins: Home, Explore, Library, Radio. Search leaves the bar and opens from a search icon in each top-level header. The Radio tab hosts the existing live radio screen with "Radio for you" seeds and a Podcasts entry.
- Search browse shows every provider category grouped by provider section, plus Levyra zones, with New releases as a regular card; "artists you listen to" is removed for these skins.

## Global constraints

- Android only. No Desktop changes.
- Do not touch playback, queue, MediaSession or the critical tap→play path. Radio entry points call the existing `startSongRadioFrom`. Volume writes STREAM_MUSIC only.
- Mesh and RSS work runs off the main thread (`Dispatchers.Default` / `IO`), is cancellable, and keeps the previous result until the new one is ready. Never flash the fallback between tracks.
- Static content first: the cover renders immediately and the mesh fades in. News that is cached stays visible during refresh, with no shimmer over real content.
- All user-visible text goes in the localization catalog through a `localizedValueOrEnglish` copy object, following `ui/i18n/LevyraExploreForYouStrings.kt`.
- No code comments (owner rule). Never name reference apps in code, commits or PR text. The only place SimpMusic is named is `docs/THIRD_PARTY_NOTICES.md`, which already credits SimpMusic.
- 48dp touch targets. Respect `LocalAnimationsEnabled`. Use stable lazy keys.
- `LevyraUiState` is at the JVM parameter limit. Do not add fields to it. News state lives in its own small `StateFlow` on the Explore view model.
- Work in a dedicated worktree: `git worktree add ../Levyra-explore-player -b feat/explore-search-apple-player`.
- Local Gradle `:app:` tasks are blocked (Android SDK build-tools missing). Write the tests anyway. CI `.github/workflows/pr-check.yml` is the authority. The fast quality gate needs Python 3.14.

---

## Phase A: Apple player colour fusion and volume

### Task A1: Pure mesh builder

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/player/ArtworkMesh.kt`
- Test: `app/src/test/java/com/luc4n3x/levyra/ui/player/ArtworkMeshTest.kt`

The builder is pure Kotlin over ARGB `IntArray`, so it runs in JVM tests with no Android Bitmap.

- [ ] **Step 1: Write the failing test**

```kotlin
package com.luc4n3x.levyra.ui.player

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtworkMeshTest {
    private fun solid(color: Int, w: Int = 120, h: Int = 120) = IntArray(w * h) { color }

    @Test
    fun solidCoverYieldsUniformTexture() {
        val mesh = buildArtworkMesh(solid(0xFF2040A0.toInt()), 120, 120, seed = 7)!!
        assertEquals(ArtworkMeshTexture * ArtworkMeshTexture, mesh.texels.size)
        assertTrue(mesh.texels.all { it == mesh.texels[0] })
    }

    @Test
    fun seamRowIsTheCoverBottomBand() {
        val w = 120; val h = 120
        val pixels = IntArray(w * h) { i -> if (i / w >= h - h / 6) 0xFFFF0000.toInt() else 0xFF0000FF.toInt() }
        val mesh = buildArtworkMesh(pixels, w, h, seed = 3)!!
        val seam = mesh.texels[0]
        assertTrue(((seam shr 16) and 0xFF) > ((seam) and 0xFF))
    }

    @Test
    fun blackCoverStaysDarkButNotPureBlack() {
        val mesh = buildArtworkMesh(solid(0xFF000000.toInt()), 120, 120, seed = 1)!!
        val r = (mesh.texels[0] shr 16) and 0xFF
        assertTrue(r in 1..20)
    }

    @Test
    fun brightTopBandRaisesTopLuminance() {
        val bright = buildArtworkMesh(solid(0xFFFFFFFF.toInt()), 120, 120, 0)!!
        val dark = buildArtworkMesh(solid(0xFF101010.toInt()), 120, 120, 0)!!
        assertTrue(bright.topLuminance > 0.9f && dark.topLuminance < 0.1f)
    }

    @Test
    fun sameSeedIsDeterministic() {
        val pixels = IntArray(120 * 120) { it * 2654435761.toInt() or 0xFF000000.toInt() }
        assertTrue(buildArtworkMesh(pixels, 120, 120, 42)!!.texels.contentEquals(buildArtworkMesh(pixels, 120, 120, 42)!!.texels))
    }

    @Test
    fun emptyInputIsNull() {
        assertEquals(null, buildArtworkMesh(IntArray(0), 0, 0, 0))
    }
}
```

- [ ] **Step 2: Run it and confirm it fails.** `./gradlew :app:testDebugUnitTest --tests "com.luc4n3x.levyra.ui.player.ArtworkMeshTest"`. Expected: compilation failure because `buildArtworkMesh` is unresolved. Locally BLOCKED (SDK), so rely on CI.

- [ ] **Step 3: Implement**

```kotlin
package com.luc4n3x.levyra.ui.player

import androidx.compose.runtime.Immutable
import kotlin.math.abs
import kotlin.math.floor
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.random.Random

internal const val ArtworkMeshGrid = 6
internal const val ArtworkMeshTexture = 32
internal const val ArtworkMeshSourcePx = 120
private const val MeshVibrance = 1.12f
private const val MeshLightnessFloor = 0.045f

@Immutable
internal class ArtworkMeshTexels(val texels: IntArray, val topEdge: IntArray, val topLuminance: Float)

internal fun buildArtworkMesh(pixels: IntArray, width: Int, height: Int, seed: Int): ArtworkMeshTexels? {
    if (width < 1 || height < 1 || pixels.size < width * height) return null
    val cols = ArtworkMeshGrid
    val rows = ArtworkMeshGrid
    val cells = cols * rows
    val red = LongArray(cells); val green = LongArray(cells); val blue = LongArray(cells); val count = IntArray(cells)
    for (y in 0 until height) {
        val rowBase = ((height - 1 - y) * rows / height) * cols
        for (x in 0 until width) {
            val cell = rowBase + x * cols / width
            val p = pixels[y * width + x]
            red[cell] += ((p shr 16) and 0xFF).toLong()
            green[cell] += ((p shr 8) and 0xFF).toLong()
            blue[cell] += (p and 0xFF).toLong()
            count[cell]++
        }
    }
    val grid = IntArray(cells) { c ->
        val n = count[c].coerceAtLeast(1)
        lifted(argb((red[c] / n).toInt(), (green[c] / n).toInt(), (blue[c] / n).toInt()))
    }
    val topRow = (rows - 1) * cols until rows * cols
    val topPixels = topRow.sumOf { count[it].toLong() }.coerceAtLeast(1L)
    val topLuminance = (topRow.sumOf { 0.2126 * red[it] + 0.7152 * green[it] + 0.0722 * blue[it] } / topPixels / 255.0)
        .toFloat().coerceIn(0f, 1f)
    val texels = resample(rotateBelowSeam(grid, cols, rows, seed), cols, rows, ArtworkMeshTexture)
    val topEdge = resample(grid.copyOfRange((rows - 1) * cols, rows * cols), cols, 1, ArtworkMeshTexture).copyOfRange(0, ArtworkMeshTexture)
    return ArtworkMeshTexels(texels, topEdge, topLuminance)
}

private fun rotateBelowSeam(grid: IntArray, cols: Int, rows: Int, seed: Int): IntArray {
    val random = Random(seed)
    val mirror = random.nextBoolean()
    val shift = random.nextInt(cols)
    val out = grid.copyOf()
    for (row in 1 until rows) {
        val base = row * cols
        for (x in 0 until cols) {
            val src = if (mirror) cols - 1 - x else x
            out[base + x] = grid[base + (src + shift) % cols]
        }
    }
    return out
}

private fun resample(grid: IntArray, cols: Int, rows: Int, size: Int): IntArray {
    val out = IntArray(size * size)
    for (ty in 0 until size) {
        val fy = (ty + 0.5f) / size * rows - 0.5f
        val y0 = floor(fy).toInt().coerceIn(0, rows - 1)
        val y1 = (y0 + 1).coerceAtMost(rows - 1)
        val wy = smoothstep(fy - y0)
        for (tx in 0 until size) {
            val fx = (tx + 0.5f) / size * cols - 0.5f
            val x0 = floor(fx).toInt().coerceIn(0, cols - 1)
            val x1 = (x0 + 1).coerceAtMost(cols - 1)
            val wx = smoothstep(fx - x0)
            out[ty * size + tx] = lerpArgb(lerpArgb(grid[y0 * cols + x0], grid[y0 * cols + x1], wx), lerpArgb(grid[y1 * cols + x0], grid[y1 * cols + x1], wx), wy)
        }
    }
    return out
}

private fun smoothstep(t: Float): Float = t.coerceIn(0f, 1f).let { it * it * (3f - 2f * it) }

private fun lerpArgb(from: Int, to: Int, t: Float): Int {
    if (t <= 0f) return from
    if (t >= 1f) return to
    fun ch(shift: Int): Int {
        val a = (from shr shift) and 0xFF
        val b = (to shr shift) and 0xFF
        return (a + (b - a) * t).roundToInt().coerceIn(0, 255)
    }
    return argb(ch(16), ch(8), ch(0))
}

private fun argb(r: Int, g: Int, b: Int): Int = (0xFF shl 24) or (r shl 16) or (g shl 8) or b

private fun lifted(color: Int): Int {
    val r = ((color shr 16) and 0xFF) / 255f
    val g = ((color shr 8) and 0xFF) / 255f
    val b = (color and 0xFF) / 255f
    val maxC = max(r, max(g, b)); val minC = min(r, min(g, b))
    val lightness = (maxC + minC) / 2f
    val delta = maxC - minC
    if (delta == 0f) {
        val grey = (lightness.coerceAtLeast(MeshLightnessFloor) * 255f).roundToInt()
        return argb(grey, grey, grey)
    }
    val saturation = (delta / (1f - abs(2f * lightness - 1f)) * MeshVibrance).coerceIn(0f, 1f)
    val hue = when (maxC) {
        r -> ((g - b) / delta).mod(6f)
        g -> (b - r) / delta + 2f
        else -> (r - g) / delta + 4f
    } * 60f
    return hsl(hue, saturation, lightness.coerceIn(MeshLightnessFloor, 1f))
}

private fun hsl(h: Float, s: Float, l: Float): Int {
    val c = (1f - abs(2f * l - 1f)) * s
    val x = c * (1f - abs((h / 60f).mod(2f) - 1f))
    val m = l - c / 2f
    val (r, g, b) = when ((h / 60f).toInt().coerceIn(0, 5)) {
        0 -> Triple(c, x, 0f); 1 -> Triple(x, c, 0f); 2 -> Triple(0f, c, x)
        3 -> Triple(0f, x, c); 4 -> Triple(x, 0f, c); else -> Triple(c, 0f, x)
    }
    return argb(((r + m) * 255f).roundToInt().coerceIn(0, 255), ((g + m) * 255f).roundToInt().coerceIn(0, 255), ((b + m) * 255f).roundToInt().coerceIn(0, 255))
}
```

- [ ] **Step 4: Run the test** (CI). Expected: PASS.
- [ ] **Step 5: Commit** (owner-authorized only): `git add ...ArtworkMesh.kt ...ArtworkMeshTest.kt && git commit -m "Build artwork colour mesh for the player backdrop"`

### Task A2: Mesh loader and backdrop composable

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/player/ArtworkMeshBackdrop.kt`

**Spec:**
- `@Composable internal fun rememberArtworkMesh(track: Track): ArtworkMesh?`, where `ArtworkMesh(image: ImageBitmap, topEdge: ImageBitmap, topLuminance: Float)`.
  - Key: `ArtworkPaletteCache.key(track.id, track.thumbnailUrl, track.largeThumbnailUrl)`. URL: `LevyraArtworkCache.small(preferredPlayerArtworkUrl(track))`.
  - Load as in `ui/artwork/ArtworkPaletteState.kt:85-113`: `ImageRequest` at `.size(ArtworkMeshSourcePx, ArtworkMeshSourcePx)`, `allowHardware(false)`, ARGB_8888, disk cache on, memory cache off, on `Dispatchers.IO`. Then `bitmap.getPixels` → `buildArtworkMesh(..., seed = key.hashCode())` on `Dispatchers.Default`. Then `Bitmap.createBitmap(texels, 32, 32, ARGB_8888).asImageBitmap()`.
  - Hold the previous mesh in `remember { mutableStateOf<ArtworkMesh?>(null) }` and replace it only on success. Rethrow `CancellationException`. Log other failures with `Timber.d` and keep the old mesh.
  - Keep a small `LruCache<String, ArtworkMesh>(12)` in a top-level object so skipping back is instant (32×32 bitmaps are about 4KB each).
- `@Composable internal fun ArtworkMeshBackdrop(mesh: ArtworkMesh?, seam: Dp, topHold: Dp, fallback: Color, modifier: Modifier)`.
  - Port the draw logic of the reference `AppleMusicMeshBackdrop` (two-picture crossfade with `Animatable`, 900ms `FastOutSlowInEasing`, `0.ms` snap when `!LocalAnimationsEnabled.current`). Draw `topEdge` stretched from 0 to topHold, mesh row 0 from topHold to seam, and the full mesh from seam to the bottom with `FilterQuality.Low`.
  - Modifier order: `.background(fallback)` then `.blur(44.dp)` (default clamp edge, not Unbounded). On API < 31 `blur` is a no-op, and the smoothstep resample plus bilinear stretch still reads smooth enough. Do not add RenderScript.
  - Scrim at the end: reuse the repository's existing scrim helper if one exists (`grep -rn "fun smoothScrimBrush\|ScrimBrush" app/src/main/java`). Otherwise use a `Brush.verticalGradient` from `Black 6%` to `Black 30%`.
  - Read the fade value only inside the draw lambda, so the fade causes no recomposition.
- `internal fun Modifier.verticalFadeEdges(topFade: Dp, bottomFade: Dp)` and `internal fun Modifier.fadeBetween(transparentAt: Dp, opaqueAt: Dp)`: `graphicsLayer { compositingStrategy = Offscreen }.drawWithContent { drawContent(); drawRect(Brush.verticalGradient(smoothstep stops), blendMode = DstIn) }`. Build the stops from about 12 smoothstep samples so the fade has no visible start line.

- [ ] Implement.
- [ ] `./gradlew :app:compileDebugKotlin` (CI).
- [ ] Commit: `Add artwork mesh backdrop for the player`

### Task A3: Portrait Apple player on the mesh

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/player/AppleMusicPlayer.kt:104-195`

**Spec (portrait, not video, not immersive Canvas):**
1. `AppleMusicPlayerScene` drops the `field/deep` gradient background. It computes `mesh = rememberArtworkMesh(track)` and puts `ArtworkMeshBackdrop` first inside the `BoxWithConstraints`. `fallback` is the existing `field` tone, so the fallback colour stays cover-tinted before the first mesh.
2. Values: `topInset = WindowInsets.statusBars` top, `artworkHeight = min(maxWidth, maxHeight * 0.62f)`, `bottomFade = artworkHeight * 0.42f`, `seam = topInset + artworkHeight`, `topHold = topInset + topInset + ((artworkHeight - topInset - bottomFade) / 2).coerceAtLeast(0.dp)`.
3. New `AppleMusicFullBleedArtwork`: `Box(Modifier.padding(top = topInset).fillMaxWidth().height(artworkHeight))` containing `slots.stage(track, Modifier.fillMaxSize().verticalFadeEdges(topFade = topInset, bottomFade = bottomFade), 0.dp)`. No shadow, no corner and no pause scale. The full-bleed cover must not shrink. Pause feedback stays on the play/pause glyph only. Keep `alpha = 1f - canvasAlpha()`.
4. Status-bar wash: a `Box(fillMaxWidth().height(topInset + 32.dp))` with `verticalGradient(Black α → transparent)`, where α animates from `0.16f + 0.49f * (mesh?.topLuminance ?: 1f)` (900ms).
5. `AppleMusicPortraitDeck` gets an overlay layout. The grabber and header sit on top of the artwork, inside `statusBarsPadding`. A `Spacer(weight(1f))` pushes the controls down to the region where the cover has dissolved. Order: metadata, engagement, progress, transport, **volume row (A4)**, dock row. Use `Arrangement.Bottom` so the controls hug the bottom on tall screens. On short screens (`maxHeight < 640.dp`) cap `artworkHeight` at `maxHeight - controlsHeight` measured with `SubcomposeLayout` or `onSizeChanged` on the controls column, so the controls never overlap the opaque part of the cover.
6. Landscape, video and immersive Canvas: same `ArtworkMeshBackdrop` with `seam = 0.dp, topHold = 0.dp` (whole page is mesh). The existing card layout is unchanged.
7. Remove `lerpField` and the second `rememberSpotifyCoverTone` (`deep`) if they become unused.

- [ ] Implement.
- [ ] Compile (CI).
- [ ] Commit: `Blend the Apple player cover into its colour mesh`

### Task A4: Volume row

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/player/MusicStreamVolume.kt`
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/player/PlayerDeckAura.kt:531-560` (use the extracted state)
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/player/AppleMusicPlayer.kt` (add the row)

**Spec:**
- Move the AudioManager, ContentObserver and `apply` block from `PlayerOutputVolume` unchanged into:

```kotlin
@Stable
internal class MusicStreamVolume(private val level: State<Float>, private val apply: (Float) -> Unit) {
    val fraction: Float get() = level.value
    fun set(fraction: Float) = apply(fraction.coerceIn(0f, 1f))
}

@Composable
internal fun rememberMusicStreamVolume(): MusicStreamVolume? { /* existing AudioManager + ContentObserver code; null if service missing */ }
```

  `PlayerOutputVolume` then reads `rememberMusicStreamVolume()`, which must cause no behaviour change for Aura.
- `AppleMusicVolumeRow(volume: MusicStreamVolume)`: a `Row` with a small speaker icon (`Icons.AutoMirrored.Rounded.VolumeDown`), a slider and a large speaker icon (`VolumeUp`). Icons use the same muted tint as `AppleMusicSeekbar`. The slider uses the same thin track and the same colours as `AppleMusicSeekbar` (read `AppleMusicPlayer.kt:286-323` and reuse its track drawing; do not invent new tokens). The track thickens slightly while dragging, as the seekbar does if it has that behaviour. Horizontal inset is `AppleMusicDesign.playerSeekbarInset`. The touch target height is 48dp. `contentDescription = strings.volume`, and `stateDescription` is the percentage. Hide the row when `rememberMusicStreamVolume()` returns null. Show it only in the Apple portrait deck. In landscape, add it after transport only if the scroll column has room; landscape already scrolls.
- Guard the RTL case: wrap the row in `LayoutDirection.Ltr` like transport.

- [ ] Implement.
- [ ] Compile (CI).
- [ ] Commit: `Add device volume row to the Apple player`

### Task A5: Licence notice

**Files:** Modify `docs/THIRD_PARTY_NOTICES.md` (SimpMusic section near line 138).

- [ ] Add one sentence: the Apple-style player colour mesh (cell averaging, seeded row shift, smoothstep resample, seam-hung backdrop, edge dissolve) is adapted from SimpMusic revision `013db77728e788ae9866d6fa763dfa5aa7025c1e`, `AppleMusicMeshBackdrop.kt` and `NowPlayingContentAppleMusic.kt`, GPL-3.0.
- [ ] Commit: `Credit adapted player mesh in third-party notices`

---

## Phase B: Search owns browsing

### Task B1: Which skins get the browse grid

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt` (add a pure predicate)
- Test: `app/src/test/java/com/luc4n3x/levyra/ui/ExploreLayoutTest.kt`

- [ ] **Step 1: Failing test**

```kotlin
@Test
fun levyraSearchBrowseCoversOnlySkinsWithoutTheirOwnBrowse() {
    assertTrue(levyraSearchOwnsBrowse(spotify = false, apple = false, ownExplore = false))
    assertFalse(levyraSearchOwnsBrowse(spotify = true, apple = false, ownExplore = false))
    assertFalse(levyraSearchOwnsBrowse(spotify = false, apple = true, ownExplore = false))
    assertFalse(levyraSearchOwnsBrowse(spotify = false, apple = false, ownExplore = true))
}
```

- [ ] **Step 2:** Run it and confirm it fails (unresolved).
- [ ] **Step 3:**

```kotlin
internal fun levyraSearchOwnsBrowse(spotify: Boolean, apple: Boolean, ownExplore: Boolean): Boolean =
    !spotify && !apple && !ownExplore
```

  Call site: `levyraSearchOwnsBrowse(LevyraIsSpotify, LevyraIsAppleMusic, LevyraIsAtelier || LevyraIsQobuz || LevyraIsYouTubeMusic)`.
- [ ] **Step 4:** PASS (CI). **Step 5:** commit `Decide which skins browse from Search`.

### Task B2: Levyra browse section in Search

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/SearchBrowse.kt`
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraSearchBrowseStrings.kt` (copy object: `browseTitle`, `newReleases`, `newReleasesDetail`, `moodsAndGenres`). Follow `LevyraExploreForYouStrings.kt` and cover every language that file covers.
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt:15304-15320` (empty-query branch)

**Spec:**
- `LazyListScope.levyraSearchBrowseItems(state, viewModel, onOpenNewReleases)`, placed after `SearchTasteHints` when `queryClean.isEmpty()` and `levyraSearchOwnsBrowse(...)`.
- Content, top to bottom:
  1. Section header `browseTitle`, reusing the existing `ExploreSectionHeader` style.
  2. A **New releases** hero tile at full width and about 112dp tall. Its artwork mosaic comes from the first three `freshFeed.spotlight` covers. Tapping it opens `ExploreNewReleasesDestinationScreen`; reuse the Explore destination plumbing that `ExploreShortcut.NewReleases` uses today. Read `onShortcut` in `LevyraApp.kt:25634+` and lift the destination state only if Search cannot reach it. If the lift is needed, route through the existing `viewModel.openExploreNewReleases`, or add one thin forwarding method on `SearchViewModel` next to `openExploreZone`.
  3. **Moods and genres** as a 2-column grid of `ExploreDiscoveryCategoryCard` (`ExploreDestinationScreens.kt:1407`, already Levyra M3 Expressive). Entries come from `spotifyBrowseEntries(zones, categories, categoryArtwork, viewModel::openExploreZone, viewModel::openExploreCategory)` and the triggers are `LaunchedEffect(strings) { viewModel.ensureExplore(strings) }`, which is the same data path as `spotifySearchBrowseItem`. Request lazy artwork with `ensureExploreCategoryArtwork` as Apple does.
  4. Use stable keys `search-browse-<entry.key>`. Use `item(span)`-free rows chunked by 2 (LazyColumn), the same way other grids in the file are built.
- Loading: keep previous entries visible. Show `DiscoveryLoadingIndicator` only when no entries exist yet. Empty or offline: show nothing extra; the taste hints remain.

- [ ] Implement the strings, component and call site.
- [ ] Compile (CI).
- [ ] Commit: `Browse new releases, moods and genres from Search`

---

## Phase C: Explore as an editorial music page

New row order for skins where `levyraSearchOwnsBrowse(...)` is true:

| # | Row | Source | Notes |
|---|-----|--------|-------|
| 1 | `NewsHero` | RSS (C1–C2) | 1 large story and a horizontal pager of up to 5. Hidden when there is no news. |
| 2 | `Header(Fresh)` + `FreshScopes`/`FreshSpotlight`/`FreshMoment` | existing | Retitled "This week's releases" through existing copy. |
| 3 | `RadioHub` | existing | "Radio for you": up to 6 seed tracks from `mostPlayedTracks`/`recentListens` (distinct by artist), each tap → `startSongRadioFrom(track)`. Plus two wide tiles: **Live radio** (`onLiveRadioOpenChange(true)`) and **Podcasts** (existing `ExploreShortcut.Podcasts` handler). |
| 4 | `MoodJourney` | existing `MoodJourneyEntryCard` | One card, unchanged behaviour. |
| 5 | `MixTools` | existing | Unchanged. |
| 6 | `NewsList` | RSS | The next 8 stories as compact rows (thumbnail 72dp, source, relative time, title 3 lines). |

Removed for these skins: `Shortcuts` (new releases and moods now live in Search; podcasts move into `RadioHub`; samples stay reachable from Search for Spotify only, which is unchanged).

### Task C1: RSS parser

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/data/news/MusicNewsFeed.kt`
- Test: `app/src/test/java/com/luc4n3x/levyra/data/news/MusicNewsFeedParserTest.kt`

- [ ] **Step 1: Failing tests.** Fixtures are inline strings covering: (a) RSS 2.0 with `media:content url=`; (b) an item with only `<enclosure type="image/jpeg" url=>`; (c) an item with only `&lt;img src="https://…"&gt;` in `description`/`content:encoded`; (d) an `http://` link or image (must be dropped or blanked); (e) a DOCTYPE with an entity (must parse safely or return empty, never expand); (f) an Atom `<entry>` with `<link href>`; (g) HTML entities in the title decode to text; (h) `pubDate` RFC-822 and Atom ISO-8601 → epoch ms, with unparseable dates → 0.

```kotlin
@Test
fun readsMediaImageAndHttpsLink() {
    val xml = """<rss xmlns:media="http://search.yahoo.com/mrss/"><channel><item>
        <title>Album &amp; tour</title><link>https://example.com/a</link>
        <pubDate>Wed, 04 Nov 2026 10:00:00 GMT</pubDate>
        <media:content url="https://img.example.com/a.jpg" medium="image"/></item></channel></rss>"""
    val story = MusicNewsFeedParser.parse(xml, source = "Example").single()
    assertEquals("Album & tour", story.title)
    assertEquals("https://example.com/a", story.url)
    assertEquals("https://img.example.com/a.jpg", story.imageUrl)
    assertEquals(1793786400000L, story.publishedAtMs)
}
```

  (Write the remaining fixtures in the same shape.)
- [ ] **Step 2:** Confirm they fail.
- [ ] **Step 3: Implement.** `data class MusicNewsStory(url, title, source, imageUrl, publishedAtMs)`. `object MusicNewsFeedParser { fun parse(xml: String, source: String): List<MusicNewsStory> }` using `DocumentBuilderFactory.newInstance().apply { isNamespaceAware = true; isExpandEntityReferences = false; runCatching { setFeature("http://apache.org/xml/features/disallow-doctype-decl", true) } }`. Return an empty list on any parse exception. Read `item` (RSS) and `entry` (Atom). Image order: `media:content`/`media:thumbnail` → image `enclosure` → first `<img src>` regex in description or content. Strip tags from the title, decode entities through the existing HTML text helper if there is one (`grep -rn "fun text(" data/qobuz/QobuzCatalogParser.kt`; reuse or move it, don't duplicate), else `android.text.Html` is not JVM-testable, so use a tiny local decoder for `&amp; &lt; &gt; &quot; &#39; &#NNN;`. Keep only `https://` links and images. Cap at 30 items per feed. Add `MusicNewsCodec.encode/decode` with JSONObject like `QobuzMagazineCodec`, and re-validate https on decode.
- [ ] **Step 4:** PASS (CI). **Step 5:** commit `Parse music news feeds`.

### Task C2: News repository and feed table

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/data/news/MusicNewsRepository.kt`
- Test: `app/src/test/java/com/luc4n3x/levyra/data/news/MusicNewsSourcesTest.kt`

- [ ] **Feed table** (`internal object MusicNewsSources { fun forLanguage(code: String): List<MusicNewsSource> }`). Re-probe each URL with curl before committing:
  - `en` (and fallback): Pitchfork `https://pitchfork.com/feed/feed-news/rss`, Stereogum `https://www.stereogum.com/feed/`, Consequence `https://www.consequence.net/feed/`, NME `https://www.nme.com/news/music/feed`
  - `it`: Rockol `https://www.rockol.it/rss/news`, All Music Italia `https://www.allmusicitalia.it/feed`, Rolling Stone Italia `https://www.rollingstone.it/musica/feed/`
  - `de`: Musikexpress `https://www.musikexpress.de/feed/`
  - `fr`: Les Inrocks `https://www.lesinrocks.com/musique/feed/`
  - `es`: Mondo Sonoro `https://www.mondosonoro.com/feed/`
  - `ru`: InterMedia `https://www.intermedia.ru/rss/news.xml`
  - Any other language falls back to `en`. Non-English lists also append Pitchfork so the hero always has images.
  - Test: every URL is `https://`, every language list is non-empty, and an unknown code returns the English list.
- [ ] **Repository** in the shape of `QobuzEditorialRepository.magazine` (`data/qobuz/QobuzEditorialRepository.kt:98-122`): singleton `get(context)`, client `LevyraHttpClientFactory.feeds(appContext)` with a network interceptor that rejects redirects to non-https URLs, `SupervisorJob + IO` scope, in-flight dedupe per language, memory plus a `cacheDir/music-news/<lang>.json` text cache, TTL 30 min, max stale 3 days, and the existing `AppleCatalogBackoff`. Fetch feeds in parallel with `async`. One failing feed must not fail the set. Merge, dedupe by url and by normalized title, sort by `publishedAtMs` descending, take 30. Set the `User-Agent` to a generic `Levyra` client name; never send the user's email.
  - `suspend fun cached(lang): List<MusicNewsStory>?` and `suspend fun load(lang, force = false): List<MusicNewsStory>?`.
- [ ] Commit: `Load music news from public feeds`

### Task C3: Explore state for news

**Files:**
- Modify: the Explore view model that already owns `ensureExplore` (find it with `grep -n "fun ensureExplore" app/src/main/java/com/luc4n3x/levyra/viewmodel/*.kt`)

- [ ] Add `private val _news = MutableStateFlow<List<MusicNewsStory>>(emptyList()); val news: StateFlow<List<MusicNewsStory>>`. This is a separate flow, not a `LevyraUiState` field.
- [ ] `fun ensureNews(lang: String)`: emit `cached(lang)` immediately, then `load(lang)` in `viewModelScope`. Keep cached items on failure. A generation check (current lang) runs before publishing, so a stale language's result is dropped.
- [ ] `fun openNews(story)`: `Intent(ACTION_VIEW, Uri.parse(story.url))` with `FLAG_ACTIVITY_NEW_TASK`, only if the scheme is https. Catch `ActivityNotFoundException`.
- [ ] Commit: `Expose music news to Explore`

### Task C4: Row model

**Files:**
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt:59-149`
- Test: `app/src/test/java/com/luc4n3x/levyra/ui/ExploreLayoutTest.kt`

- [ ] **Failing tests**

```kotlin
@Test
fun editorialExploreOrdersNewsReleasesRadioJourneyMix() {
    val rows = buildExploreRows(zones(4), false, true, editorial = true, hasNews = true, hasRadioSeeds = true)
    assertEquals(
        listOf(ExploreRow.NewsHero, ExploreRow.Header(ExploreAnchor.Fresh), ExploreRow.FreshSpotlight, ExploreRow.RadioHub, ExploreRow.MoodJourney, ExploreRow.MixTools, ExploreRow.NewsList),
        rows
    )
}

@Test
fun editorialExploreWithoutNewsStillShowsMusicAndRadio() {
    val rows = buildExploreRows(emptyList(), false, false, editorial = true, hasNews = false, hasRadioSeeds = false)
    assertTrue(rows.none { it == ExploreRow.NewsHero || it == ExploreRow.NewsList || it == ExploreRow.Shortcuts })
    assertTrue(ExploreRow.RadioHub in rows && ExploreRow.MoodJourney in rows)
}
```

  Also run all the existing `ExploreLayoutTest` cases unchanged, because `editorial = false` keeps today's output.
- [ ] **Implement:** add `NewsHero`, `RadioHub` and `NewsList` data objects. Add params `editorial: Boolean = false, hasNews: Boolean = false, hasRadioSeeds: Boolean = false`. When `editorial` is true, build the table order above. `RadioHub` is always present because live radio and podcasts are always reachable; `hasRadioSeeds` only controls the seed rail inside it. Leave `MoodRail` out. `exploreAvailableAnchors` keeps working.
- [ ] Commit: `Order Explore as an editorial page`

### Task C5: Editorial rows UI

**Files:**
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/ExploreEditorial.kt` (`ExploreNewsHero`, `ExploreNewsList`, `ExploreRadioHub`)
- Create: `app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraExploreEditorialStrings.kt` (`newsTitle`, `newsMore`, `releasesTitle`, `radioTitle`, `radioForYou`, `liveRadio`, `podcasts`, `minutesAgo/hoursAgo/daysAgo` with placeholders consistent across languages)
- Modify: `app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt:25611-25632` (pass `editorial`, `hasNews`, `hasRadioSeeds`; collect `news`) and `ExploreContentRow` (three new branches)

**Design read (levyra-design-taste):** surface = Explore/discovery; job = "what's happening in music, and what do I play now"; dials: variance 5, motion 3, density 6. Typography and artwork carry the hierarchy, not cards in cards.
- **NewsHero:** a `HorizontalPager` with `contentPadding` peek of 24dp, a page aspect of 4:3 at full gutter width and a large corner from the existing Levyra shape tokens. The image fills the page, with a bottom `smoothScrim`-style gradient carrying source (labelMedium, 70% alpha), title (headlineSmall, 3 lines) and relative time. Without an image, render a typographic card on `surfaceContainerHigh` with the source initial as a large display glyph; no generated gradients. Page dots only if there are more than 1 story. 48dp tap target is the whole card. `contentDescription` = "source, title". No auto-advance.
- **NewsList:** a header plus up to 8 rows: 72dp rounded thumbnail (or the source glyph tile), and a text column with source · time and the title (titleSmall, 3 lines). The divider rhythm is the existing list spacing.
- **RadioHub:** header `radioTitle`. Then a `LazyRow` of seed tiles: 132dp square artwork with an M3 Expressive shape morph on press, using the existing `ExploreExpressiveShape` if it fits, plus the artist name and the "Radio" label. Tap → `startSongRadioFrom(track)`. Then two side-by-side wide tiles, **Live radio** and **Podcasts**, using the existing `LevyraExploreShortcutTile` (`LevyraApp.kt:26373`) so no new tile family is created. Seeds come from `remember(state.mostPlayedTracks, state.recentListens) { (mostPlayed + recent).distinctBy { it.artist.lowercase() }.take(6) }`.
- Relative time comes from a pure helper `exploreNewsAge(nowMs, publishedAtMs): Pair<Unit, Int>` with a unit test (0 → minutes; future timestamps clamp to 0).
- Images load through the existing `AsyncImage`/Levyra artwork component with crossfade. Never block the first frame.

- [ ] Implement, collect `news` with `collectAsStateWithLifecycle`, and call `ensureNews(strings.code)` from a `LaunchedEffect(strings.code)` when `editorial`.
- [ ] Compile (CI).
- [ ] Commit: `Redesign Explore as an editorial music page`

---

## Final verification

- [ ] `git diff --check` and a plain `git diff` review of the whole branch.
- [ ] `python3.14 scripts/ai_quality_gate.py --profile fast` (owner wants fast only).
- [ ] `/code-review` plus the local CodeRabbit CLI (owner rule: no review subagents).
- [ ] Emulator sweep in one pass at the end (owner starts the emulator; use the `.debug` package and `-PlevyraFdroidBuild=true`). Kill the app when done. Mark anything not exercised as UNVERIFIED:
  - Apple player: red, white, black and busy covers (join invisible, text readable, status-bar icons readable). Skip next/previous (mesh crossfade, no fallback flash). Pause (cover stays full-bleed). Hardware volume keys move the row. Dragging the row changes the volume with no system toast. Small screen (≤ 640dp height): controls don't cover the opaque cover. Landscape, video and Canvas still work.
  - Search on Default, Nocturne and Prism: browse appears on an empty query and disappears while typing. New releases opens. A mood or genre opens its destination. Spotify and Apple Search are unchanged.
  - Explore on Default, Nocturne, Prism, Torque and Ivory: news with IT and EN language. Offline shows cached news, or no news section and no crash. Atelier, Qobuz, YTM, Spotify and Apple Explore are unchanged.
  - Radio tab on the same skins: a radio seed starts a radio; Live radio and Podcasts open. Mood Journey opens from Library.
  - Large font, RU strings, light mode.
- [ ] Draft PR only when the owner authorizes it. Fill the complete PR template and run unslop on the prose. Do not mention reference apps in the PR.

## Risks

- `Modifier.blur` is API 31+. On API 26–30 the mesh is unblurred but still smoothstep-smooth; verify on an API 28 emulator image if one is available.
- RSS sources can change markup or go down. The parser is tolerant, one feed's failure is isolated, and cached stories survive for 3 days.
- RSS images hotlink publisher CDNs. That is fine for display, but the fixed feed table is a product choice the owner can edit later.
- `LevyraApp.kt` is very large. Keep new UI in new files and touch `LevyraApp.kt` only at the call sites listed.
