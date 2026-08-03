from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text()

replacements = [
    (
        '''        OccasionallyRotatingSettingsButton(
            enabled = animationsEnabled && !isResolving,
            contentDescription = strings.settings,
            loading = isResolving,
            onClick = onSettings
        )
''',
        '''        OccasionallyRotatingSettingsButton(
            animationsEnabled = animationsEnabled,
            busy = isResolving,
            contentDescription = strings.settings,
            loading = isResolving,
            onClick = onSettings
        )
'''
    ),
    (
        '''private fun OccasionallyRotatingSettingsButton(
    enabled: Boolean,
    contentDescription: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    LaunchedEffect(enabled) {
        rotation.snapTo(0f)
        if (!enabled) return@LaunchedEffect
        delay(6_000L)
        while (true) {
            rotation.animateTo(
                targetValue = 360f,
                animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
            )
            rotation.snapTo(0f)
            delay(28_000L)
        }
    }
''',
        '''private fun OccasionallyRotatingSettingsButton(
    animationsEnabled: Boolean,
    busy: Boolean,
    contentDescription: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    val currentBusy by rememberUpdatedState(busy)
    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        delay(6_000L)
        while (true) {
            if (!currentBusy) {
                rotation.animateTo(
                    targetValue = 360f,
                    animationSpec = tween(durationMillis = 780, easing = FastOutSlowInEasing)
                )
                rotation.snapTo(0f)
            }
            delay(28_000L)
        }
    }
'''
    )
]

for old, new in replacements:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match, found {count}\n--- OLD ---\n{old}")
    text = text.replace(old, new)

path.write_text(text)
