#!/usr/bin/env python3
"""Preserve Top 50 artwork through playback and refine the Now Playing artwork card."""

from pathlib import Path

PLAYBACK = Path("app/src/main/java/com/luc4n3x/levyra/data/PlaybackResolver.kt")
APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
TEST = Path("app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt")

playback = PLAYBACK.read_text(encoding="utf-8")
app = APP.read_text(encoding="utf-8")

old_lock = '''internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {
    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags
'''
new_lock = '''internal fun preserveEditorialArtwork(presented: Track, resolved: Track): Track {
    val artworkLocked = presented.source.equals("Levyra Editorial", ignoreCase = true) ||
        EDITORIAL_ARTWORK_LOCK_TAG in presented.moodTags ||
        presented.moodTags.any { it.equals("chart", ignoreCase = true) } ||
        presented.source.contains("Charts", ignoreCase = true)
'''
if playback.count(old_lock) != 1:
    raise SystemExit(f"Expected one playback artwork lock block, found {playback.count(old_lock)}")
playback = playback.replace(old_lock, new_lock, 1)

old_card_motion = '''    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.965f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 28.dp else 30.dp,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 32f else 14f,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "artwork-shadow"
    )
    val artOffset by animateDpAsState(
        targetValue = if (state.isPlaying) 0.dp else 5.dp,
        animationSpec = tween(210, easing = FastOutSlowInEasing),
        label = "artwork-offset"
    )'''
new_card_motion = '''    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.972f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 26.dp else 28.dp,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 27f else 17f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-shadow"
    )
    val artOffset by animateDpAsState(
        targetValue = if (state.isPlaying) 0.dp else 3.dp,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "artwork-offset"
    )'''
if app.count(old_card_motion) != 1:
    raise SystemExit(f"Expected one whole-card motion block, found {app.count(old_card_motion)}")
app = app.replace(old_card_motion, new_card_motion, 1)

start = app.index("@Composable\nprivate fun PlayerArtworkCanvas(")
end = app.index("\n@Composable\nprivate fun PlayerModeSwitch(", start)
old_artwork_function = app[start:end]
new_artwork_function = '''@Composable
private fun PlayerArtworkCanvas(
    track: Track,
    artworkUrl: String,
    motionArtwork: com.luc4n3x.levyra.feature.motion.MotionArtwork?,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val haloScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.015f else 0.995f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-scale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.58f else 0.42f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-alpha"
    )
    val artworkShadow by animateFloatAsState(
        targetValue = if (isPlaying) 27f else 17f,
        animationSpec = tween(190, easing = FastOutSlowInEasing),
        label = "player-artwork-shadow"
    )
    val primary = Color(track.accentStart)
    val secondary = Color(track.accentEnd)
    val artworkShape = RoundedCornerShape(cornerRadius)

    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Box(
            modifier = Modifier
                .fillMaxSize(0.94f)
                .graphicsLayer {
                    scaleX = haloScale
                    scaleY = haloScale
                    alpha = haloAlpha
                }
                .background(
                    Brush.radialGradient(
                        listOf(
                            primary.playerMix(Color.White, 0.10f).copy(alpha = 0.34f),
                            secondary.copy(alpha = 0.18f),
                            primary.playerMix(Color.Black, 0.68f).copy(alpha = 0.08f),
                            Color.Transparent
                        )
                    ),
                    RoundedCornerShape(cornerRadius + 22.dp)
                )
        )
        Box(
            modifier = Modifier
                .fillMaxSize(0.91f)
                .graphicsLayer {
                    shadowElevation = artworkShadow
                    shape = artworkShape
                    clip = true
                }
                .background(Color.Black.copy(alpha = 0.16f), artworkShape)
                .border(
                    width = 1.dp,
                    color = Color.White.copy(alpha = 0.14f),
                    shape = artworkShape
                )
        ) {
            MotionArtworkLayer(
                artwork = motionArtwork,
                enabled = animationsEnabled,
                isPlaying = isPlaying,
                cornerRadius = cornerRadius,
                modifier = Modifier.fillMaxSize()
            ) {
                if (artworkUrl.isNotBlank()) {
                    AsyncImage(
                        model = ImageRequest.Builder(context)
                            .data(artworkUrl)
                            .crossfade(true)
                            .diskCachePolicy(CachePolicy.ENABLED)
                            .memoryCachePolicy(CachePolicy.ENABLED)
                            .build(),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    InstantArtworkPlaceholder(track = track, modifier = Modifier.fillMaxSize())
                }
            }
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            listOf(
                                Color.White.copy(alpha = 0.07f),
                                Color.Transparent,
                                Color.Transparent,
                                Color.Black.copy(alpha = 0.11f)
                            )
                        )
                    )
            )
        }
    }
}
'''
if "fillMaxSize(0.865f)" not in old_artwork_function:
    raise SystemExit("Unexpected PlayerArtworkCanvas body; refusing broad replacement")
app = app[:start] + new_artwork_function + app[end:]

TEST.write_text('''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Test

class EditorialArtworkContinuityTest {
    @Test
    fun playerKeepsTheArtworkPresentedInTheTop50Row() {
        val presented = track(
            source = "Levyra Editorial",
            thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",
            moodTags = setOf("chart"),
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
        ).copy(id = "abcdefghijk", videoUrl = "https://music.youtube.com/watch?v=abcdefghijk")

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
        assertEquals(resolved.videoUrl, result.videoUrl)

        val recovered = preserveEditorialArtwork(
            result,
            resolved.copy(thumbnailUrl = "https://i.ytimg.com/vi/abcdefghijk/maxresdefault.jpg")
        )
        assertEquals(presented.thumbnailUrl, recovered.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, recovered.largeThumbnailUrl)
    }

    @Test
    fun fallbackChartSourcesAlsoKeepTheArtworkTheUserOpened() {
        val presented = track(
            source = "YouTube Music Charts",
            thumbnail = "https://charts.example.test/red-cover.jpg",
            moodTags = setOf("chart"),
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/white-cover.jpg",
        )

        val result = preserveEditorialArtwork(presented, resolved)

        assertEquals(presented.thumbnailUrl, result.thumbnailUrl)
        assertEquals(presented.thumbnailUrl, result.largeThumbnailUrl)
    }

    @Test
    fun chartSourceNameLocksArtworkEvenAfterTagsAreLostInTransit() {
        val presented = track(
            source = "Apple Music Charts",
            thumbnail = "https://charts.example.test/presented-cover.jpg",
        )
        val resolved = track(
            source = "YouTube Music",
            thumbnail = "https://charts.example.test/resolved-cover.jpg",
        )

        assertEquals(
            presented.thumbnailUrl,
            preserveEditorialArtwork(presented, resolved).thumbnailUrl,
        )
    }

    @Test
    fun normalTracksKeepTheResolvedArtwork() {
        val presented = track(source = "Search", thumbnail = "https://example.test/old.jpg")
        val resolved = track(source = "YouTube Music", thumbnail = "https://example.test/new.jpg")

        assertEquals(resolved, preserveEditorialArtwork(presented, resolved))
    }

    private fun track(
        source: String,
        thumbnail: String,
        moodTags: Set<String> = emptySet(),
    ): Track = Track(
        id = "chart-id",
        title = "Titolo",
        artist = "Artista",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "",
        thumbnailUrl = thumbnail,
        largeThumbnailUrl = thumbnail,
        source = source,
        moodTags = moodTags,
        energy = 70,
        vocal = 55,
        replayScore = 90,
        cacheScore = 80,
        accentStart = 0,
        accentEnd = 0,
    )
}
''', encoding="utf-8")

PLAYBACK.write_text(playback, encoding="utf-8")
APP.write_text(app, encoding="utf-8")
print("Applied Top 50 artwork continuity and understated player artwork polish")
