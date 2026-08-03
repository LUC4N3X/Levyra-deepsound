from pathlib import Path

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
text = path.read_text()
old = '''    val rotation = remember { Animatable(0f) }
    val currentBusy by rememberUpdatedState(busy)

    suspend fun playAttentionSpin() {
'''
new = '''    val rotation = remember { Animatable(0f) }

    suspend fun playAttentionSpin() {
'''
if text.count(old) != 1:
    raise SystemExit(f"Expected one rotation state block, found {text.count(old)}")
text = text.replace(old, new)

old = '''    LaunchedEffect(animationsEnabled) {
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
'''
new = '''    LaunchedEffect(animationsEnabled, busy) {
        if (!animationsEnabled || busy) {
            rotation.snapTo(0f)
            return@LaunchedEffect
        }
        delay(1_400L)
        while (true) {
            playAttentionSpin()
            delay(22_000L)
        }
    }
'''
if text.count(old) != 1:
    raise SystemExit(f"Expected one attention spin loop, found {text.count(old)}")
path.write_text(text.replace(old, new))
