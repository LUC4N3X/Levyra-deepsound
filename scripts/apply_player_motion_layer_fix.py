#!/usr/bin/env python3
"""Move motion artwork inside the stable Now Playing frame."""

from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = PATH.read_text(encoding="utf-8")

replacements = [
    (
        '''private fun PlayerArtworkCanvas(
    track: Track,
    artworkUrl: String,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {''',
        '''private fun PlayerArtworkCanvas(
    track: Track,
    artworkUrl: String,
    motionArtwork: com.luc4n3x.levyra.feature.motion.MotionArtwork?,
    animationsEnabled: Boolean,
    isPlaying: Boolean,
    cornerRadius: Dp,
    modifier: Modifier = Modifier
) {''',
        "PlayerArtworkCanvas signature",
    ),
    (
        '''            if (artworkUrl.isNotBlank()) {
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
            }''',
        '''            MotionArtworkLayer(
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
            }''',
        "artwork image body",
    ),
    (
        '''                            MotionArtworkLayer(
                                artwork = state.motionArtwork,
                                enabled = state.animationsEnabled && !state.isVideoMode,
                                isPlaying = state.isPlaying,
                                cornerRadius = artCorner,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                    }
                            ) {
                                PlayerArtworkCanvas(
                                    track = track,
                                    artworkUrl = artworkUrl,
                                    isPlaying = state.isPlaying,
                                    cornerRadius = artCorner,
                                    modifier = Modifier.fillMaxSize()
                                )
                            }''',
        '''                            PlayerArtworkCanvas(
                                track = track,
                                artworkUrl = artworkUrl,
                                motionArtwork = state.motionArtwork,
                                animationsEnabled = state.animationsEnabled && !state.isVideoMode,
                                isPlaying = state.isPlaying,
                                cornerRadius = artCorner,
                                modifier = Modifier
                                    .fillMaxSize()
                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                    }
                            )''',
        "Now Playing artwork call",
    ),
]

for old, new, label in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} block, found {count}")
    text = text.replace(old, new, 1)

if text.count("MotionArtworkLayer(") != 1:
    raise SystemExit("Expected exactly one nested MotionArtworkLayer usage in LevyraApp.kt")
if "motionArtwork = state.motionArtwork" not in text:
    raise SystemExit("Now Playing motion artwork parameter was not wired")

PATH.write_text(text, encoding="utf-8")
print("Moved motion artwork inside the stable player frame")
