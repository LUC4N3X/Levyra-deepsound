#!/usr/bin/env python3
"""Apply the premium artwork motion pass, then get removed by the one-shot workflow."""

from pathlib import Path

APP = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
MOTION = Path("app/src/main/java/com/luc4n3x/levyra/ui/MotionArtworkLayer.kt")

app = APP.read_text(encoding="utf-8")
motion = MOTION.read_text(encoding="utf-8")

app_replacements = [
    (
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
        '''    val artScale by animateFloatAsState(
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
    )''',
        "whole-card play/pause animation",
    ),
    (
        '''                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }''',
        '''                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                        clip = true
                                    }''',
        "video artwork card transform",
    ),
    (
        '''                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                    }
                            )''',
        '''                                    .graphicsLayer {
                                        scaleX = artScale
                                        scaleY = artScale
                                        translationY = artOffset.toPx()
                                        shadowElevation = artShadow
                                        shape = RoundedCornerShape(artCorner)
                                    }
                            )''',
        "audio artwork card transform",
    ),
]

for old, new, label in app_replacements:
    count = app.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label} block, found {count}")
    app = app.replace(old, new, 1)

motion_replacements = [
    ("import androidx.compose.animation.core.LinearEasing\n", "", "unused LinearEasing import"),
    ("import kotlin.math.PI\nimport kotlin.math.cos\nimport kotlin.math.sin\n", "", "obsolete trigonometry imports"),
    (
        '''    val transition = rememberInfiniteTransition(label = "static-artwork-motion")
    val phase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STATIC_ARTWORK_MOTION_DURATION_MS,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "static-artwork-motion-phase"
    )''',
        '''    val transition = rememberInfiniteTransition(label = "static-artwork-motion")
    val zoomPhase by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STATIC_ARTWORK_ZOOM_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "static-artwork-zoom"
    )
    val horizontalDrift by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STATIC_ARTWORK_HORIZONTAL_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "static-artwork-horizontal-drift"
    )
    val verticalDrift by transition.animateFloat(
        initialValue = 1f,
        targetValue = -1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STATIC_ARTWORK_VERTICAL_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "static-artwork-vertical-drift"
    )
    val tiltPhase by transition.animateFloat(
        initialValue = -1f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = STATIC_ARTWORK_TILT_DURATION_MS,
                easing = FastOutSlowInEasing
            ),
            repeatMode = RepeatMode.Reverse
        ),
        label = "static-artwork-tilt"
    )''',
        "static artwork motion phases",
    ),
    (
        '''    val angle = phase * (2f * PI.toFloat())
    val horizontalWave = sin(angle.toDouble()).toFloat()
    val verticalWave = cos((angle * 2f).toDouble()).toFloat()
    val breathingWave = ((sin((angle * 2f).toDouble()) + 1.0) * 0.5).toFloat()
    val scale = 1f + motionAmount * (0.040f + breathingWave * 0.014f)''',
        '''    val scale = 1f + motionAmount * (0.032f + zoomPhase * 0.028f)''',
        "static artwork transform calculation",
    ),
    (
        '''                    translationX = artworkSize.width * 0.016f * horizontalWave * motionAmount
                    translationY = artworkSize.height * 0.011f * verticalWave * motionAmount
                    rotationZ = 0.12f * horizontalWave * motionAmount''',
        '''                    translationX = artworkSize.width * 0.018f * horizontalDrift * motionAmount
                    translationY = artworkSize.height * 0.014f * verticalDrift * motionAmount
                    rotationZ = 0.16f * tiltPhase * motionAmount''',
        "static artwork graphics transform",
    ),
    (
        '''        animationSpec = tween(durationMillis = 320),
        label = "motion-artwork-alpha"''',
        '''        animationSpec = tween(durationMillis = 300, easing = FastOutSlowInEasing),
        label = "motion-artwork-alpha"''',
        "canvas first-frame fade",
    ),
    (
        '''private const val STATIC_ARTWORK_MOTION_DURATION_MS = 18_000
private const val STATIC_ARTWORK_MOTION_ENTER_MS = 420
private const val STATIC_ARTWORK_MOTION_EXIT_MS = 220''',
        '''private const val STATIC_ARTWORK_ZOOM_DURATION_MS = 12_000
private const val STATIC_ARTWORK_HORIZONTAL_DURATION_MS = 15_000
private const val STATIC_ARTWORK_VERTICAL_DURATION_MS = 18_000
private const val STATIC_ARTWORK_TILT_DURATION_MS = 21_000
private const val STATIC_ARTWORK_MOTION_ENTER_MS = 480
private const val STATIC_ARTWORK_MOTION_EXIT_MS = 220''',
        "premium artwork timing constants",
    ),
]

for old, new, label in motion_replacements:
    count = motion.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one {label}, found {count}")
    motion = motion.replace(old, new, 1)

required_app = [
    "targetValue = if (state.isPlaying) 1f else 0.965f",
    "translationY = artOffset.toPx()",
    "shadowElevation = artShadow",
]
for marker in required_app:
    if marker not in app:
        raise SystemExit(f"Missing app marker after patch: {marker}")

required_motion = [
    "repeatMode = RepeatMode.Reverse",
    "STATIC_ARTWORK_TILT_DURATION_MS",
    "durationMillis = 300, easing = FastOutSlowInEasing",
]
for marker in required_motion:
    if marker not in motion:
        raise SystemExit(f"Missing motion marker after patch: {marker}")

APP.write_text(app, encoding="utf-8")
MOTION.write_text(motion, encoding="utf-8")
print("Applied premium whole-card and artwork motion")
