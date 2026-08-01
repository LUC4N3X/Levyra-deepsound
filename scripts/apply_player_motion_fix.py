#!/usr/bin/env python3
"""Apply the focused Now Playing motion fix, then remove this one-shot helper."""

from pathlib import Path

PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = PATH.read_text(encoding="utf-8")

replacements = [
    (
        '''    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.975f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 30.dp else 22.dp,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 30f else 16f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
        label = "artwork-shadow"
    )''',
        '''    val artScale by animateFloatAsState(
        targetValue = if (state.isPlaying) 1f else 0.992f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "artwork-scale"
    )
    val artCorner by animateDpAsState(
        targetValue = if (state.isPlaying) 28.dp else 26.dp,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "artwork-corner"
    )
    val artShadow by animateFloatAsState(
        targetValue = if (state.isPlaying) 28f else 22f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "artwork-shadow"
    )''',
        "outer artwork transition",
    ),
    (
        '''    val artworkScale by animateFloatAsState(
        targetValue = if (isPlaying) 1f else 0.965f,
        animationSpec = spring(dampingRatio = 0.78f, stiffness = Spring.StiffnessLow),
        label = "player-artwork-stage-scale"
    )
    val haloScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.04f else 0.97f,
        animationSpec = spring(dampingRatio = 0.82f, stiffness = Spring.StiffnessLow),
        label = "player-artwork-halo-scale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.92f else 0.64f,
        animationSpec = tween(500, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-alpha"
    )''',
        '''    val haloScale by animateFloatAsState(
        targetValue = if (isPlaying) 1.025f else 0.995f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-scale"
    )
    val haloAlpha by animateFloatAsState(
        targetValue = if (isPlaying) 0.88f else 0.72f,
        animationSpec = tween(220, easing = FastOutSlowInEasing),
        label = "player-artwork-halo-alpha"
    )
    val artworkShadow by animateFloatAsState(
        targetValue = if (isPlaying) 30f else 22f,
        animationSpec = tween(180, easing = FastOutSlowInEasing),
        label = "player-artwork-shadow"
    )''',
        "inner artwork transition",
    ),
    (
        '''                .graphicsLayer {
                    scaleX = artworkScale
                    scaleY = artworkScale
                    shadowElevation = if (isPlaying) 34f else 18f
                    shape = artworkShape
                    clip = true
                }''',
        '''                .graphicsLayer {
                    shadowElevation = artworkShadow
                    shape = artworkShape
                    clip = true
                }''',
        "inner artwork graphics layer",
    ),
]

for old, new, label in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} block, found {count}")
    text = text.replace(old, new, 1)

required = [
    "MotionArtworkLayer(",
    "artwork = state.motionArtwork",
    "enabled = state.animationsEnabled && !state.isVideoMode",
]
for marker in required:
    if marker not in text:
        raise SystemExit(f"Now Playing motion integration marker missing: {marker}")

PATH.write_text(text, encoding="utf-8")
print("Applied focused Now Playing motion fix")
