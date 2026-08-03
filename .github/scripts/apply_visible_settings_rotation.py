from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text()
old = '''@Composable
private fun OccasionallyRotatingSettingsButton(
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
    Box(modifier = Modifier.graphicsLayer { rotationZ = rotation.value }) {
'''
new = '''@Composable
private fun OccasionallyRotatingSettingsButton(
    animationsEnabled: Boolean,
    busy: Boolean,
    contentDescription: String,
    loading: Boolean,
    onClick: () -> Unit
) {
    val rotation = remember { Animatable(0f) }
    val currentBusy by rememberUpdatedState(busy)

    suspend fun playAttentionSpin() {
        rotation.snapTo(0f)
        rotation.animateTo(
            targetValue = -18f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
        rotation.animateTo(
            targetValue = 26f,
            animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
        )
        rotation.animateTo(
            targetValue = 330f,
            animationSpec = tween(durationMillis = 950, easing = FastOutSlowInEasing)
        )
        rotation.animateTo(
            targetValue = 360f,
            animationSpec = tween(durationMillis = 180, easing = FastOutSlowInEasing)
        )
        rotation.snapTo(0f)
    }

    LaunchedEffect(animationsEnabled) {
        if (!animationsEnabled) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        delay(1_400L)
        while (true) {
            while (currentBusy) delay(400L)
            playAttentionSpin()
            delay(22_000L)
        }
    }
    Box(modifier = Modifier.graphicsLayer { rotationZ = rotation.value }) {
'''
count = text.count(old)
if count != 1:
    raise SystemExit(f"Expected one settings rotation block, found {count}")
path.write_text(text.replace(old, new))
