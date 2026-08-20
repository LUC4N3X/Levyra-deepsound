from pathlib import Path

ROOT = Path(__file__).resolve().parents[2]


def replace_once(path: Path, old: str, new: str, label: str) -> None:
    text = path.read_text(encoding="utf-8")
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    path.write_text(text.replace(old, new, 1), encoding="utf-8")


app = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
motion_layer = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/MotionArtworkLayer.kt"
view_model = ROOT / "app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt"

replace_once(
    app,
    '''        val immersiveArtworkEnabled = state.motionArtworkEnabled &&
            state.animationsEnabled &&
            playerPane == LevyraPlayerPane.Stacked &&
            !state.isVideoMode &&
            track != null &&
            state.motionArtwork != null
        val immersiveMotionArtwork = state.motionArtwork.takeIf { immersiveArtworkEnabled }
        val immersiveMotionAlpha by animateFloatAsState(
            targetValue = if (immersiveArtworkEnabled) 1f else 0f,
            animationSpec = if (state.animationsEnabled) tween(520, easing = LinearOutSlowInEasing) else snap(),
            label = "player-immersive-motion-alpha"
        )

        PlayerImmersiveBackdrop(
            artworkUrl = artworkUrl,
''',
    '''        // Mount the immersive layer immediately in song mode. MotionArtworkLayer owns the
        // animated static bed and crossfades to a real Canvas only after its first frame, so the
        // player never has to flash a dead static state while remote artwork is resolving.
        val immersiveArtworkEnabled = state.motionArtworkEnabled &&
            state.animationsEnabled &&
            playerPane == LevyraPlayerPane.Stacked &&
            !state.isVideoMode &&
            track != null
        val immersiveMotionArtwork = state.motionArtwork.takeIf { immersiveArtworkEnabled }

        PlayerImmersiveBackdrop(
            // The immersive layer already draws the artwork bed. Avoid decoding/drawing a second
            // fullscreen copy behind it; keep only the palette backdrop as the safety bed.
            artworkUrl = if (immersiveArtworkEnabled) "" else artworkUrl,
''',
    "make immersive layer immediate"
)

replace_once(
    app,
    '''        AnimatedVisibility(
            visible = track != null && immersiveArtworkEnabled,
            enter = if (state.animationsEnabled) fadeIn(tween(520, easing = LinearOutSlowInEasing)) else EnterTransition.None,
            exit = if (state.animationsEnabled) fadeOut(tween(320, easing = LinearOutSlowInEasing)) else ExitTransition.None,
''',
    '''        AnimatedVisibility(
            visible = immersiveArtworkEnabled,
            // The generated artwork bed is the immediate first frame. A real Canvas performs its
            // own first-frame crossfade inside MotionArtworkLayer, so an outer enter fade only
            // reintroduces the static-to-motion flash we are trying to remove.
            enter = EnterTransition.None,
            exit = if (state.animationsEnabled) fadeOut(tween(260, easing = LinearOutSlowInEasing)) else ExitTransition.None,
''',
    "remove immersive enter flash"
)

replace_once(
    motion_layer,
    '''        MotionArtworkStaticFallback(
            animated = animateStatic,
            cornerRadius = cornerRadius,
''',
    '''        MotionArtworkStaticFallback(
            animated = animateStatic,
            presentation = presentation,
            cornerRadius = cornerRadius,
''',
    "pass presentation to static fallback"
)

replace_once(
    motion_layer,
    '''private fun MotionArtworkStaticFallback(
    animated: Boolean,
    cornerRadius: Dp,
''',
    '''private fun MotionArtworkStaticFallback(
    animated: Boolean,
    presentation: MotionArtworkPresentation,
    cornerRadius: Dp,
''',
    "extend static fallback signature"
)

replace_once(
    motion_layer,
    '''    LaunchedEffect(animated) {
        if (!animated) {
''',
    '''    val immersive = presentation == MotionArtworkPresentation.Immersive
    val zoomDurationMs = if (immersive) 14_000 else STATIC_ARTWORK_ZOOM_DURATION_MS
    val horizontalDurationMs = if (immersive) 18_000 else STATIC_ARTWORK_HORIZONTAL_DURATION_MS
    val verticalDurationMs = if (immersive) 21_000 else STATIC_ARTWORK_VERTICAL_DURATION_MS

    LaunchedEffect(animated, presentation) {
        if (!animated) {
''',
    "add immersive motion timing"
)

replace_once(
    motion_layer,
    '''                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    zoomPhase.animateTo(
                        0f,
                        tween(STATIC_ARTWORK_ZOOM_DURATION_MS, easing = FastOutSlowInEasing)
''',
    '''                        tween(zoomDurationMs, easing = FastOutSlowInEasing)
                    )
                    zoomPhase.animateTo(
                        0f,
                        tween(zoomDurationMs, easing = FastOutSlowInEasing)
''',
    "tune immersive zoom timing"
)

replace_once(
    motion_layer,
    '''                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    horizontalDrift.animateTo(
                        -1f,
                        tween(STATIC_ARTWORK_HORIZONTAL_DURATION_MS, easing = FastOutSlowInEasing)
''',
    '''                        tween(horizontalDurationMs, easing = FastOutSlowInEasing)
                    )
                    horizontalDrift.animateTo(
                        -1f,
                        tween(horizontalDurationMs, easing = FastOutSlowInEasing)
''',
    "tune immersive horizontal timing"
)

replace_once(
    motion_layer,
    '''                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
                    )
                    verticalDrift.animateTo(
                        1f,
                        tween(STATIC_ARTWORK_VERTICAL_DURATION_MS, easing = FastOutSlowInEasing)
''',
    '''                        tween(verticalDurationMs, easing = FastOutSlowInEasing)
                    )
                    verticalDrift.animateTo(
                        1f,
                        tween(verticalDurationMs, easing = FastOutSlowInEasing)
''',
    "tune immersive vertical timing"
)

replace_once(
    motion_layer,
    '''                    val amount = motionAmount
                    val scale = 1f + amount * (0.042f + zoomPhase.value * 0.022f)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * 0.016f * horizontalDrift.value * amount
                    translationY = artworkSize.height * 0.012f * verticalDrift.value * amount
''',
    '''                    val amount = motionAmount
                    val baseZoom = if (immersive) 0.064f else 0.042f
                    val pulseZoom = if (immersive) 0.030f else 0.022f
                    val horizontalTravel = if (immersive) 0.026f else 0.016f
                    val verticalTravel = if (immersive) 0.020f else 0.012f
                    val scale = 1f + amount * (baseZoom + zoomPhase.value * pulseZoom)
                    scaleX = scale
                    scaleY = scale
                    translationX = artworkSize.width * horizontalTravel * horizontalDrift.value * amount
                    translationY = artworkSize.height * verticalTravel * verticalDrift.value * amount
                    rotationZ = if (immersive) horizontalDrift.value * amount * 0.22f else 0f
''',
    "make immersive fallback cinematic"
)

replace_once(
    view_model,
    '''        motionArtworkPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            delay(450L)
''',
    '''        motionArtworkPrefetchJob = viewModelScope.launch(Dispatchers.IO) {
            // Yield briefly to audible playback, then warm the next Canvas early enough that a
            // queue transition can usually enter the immersive layer with the real asset ready.
            delay(180L)
''',
    "warm next canvas sooner"
)

print("PR #424 Canvas transition patch applied")
