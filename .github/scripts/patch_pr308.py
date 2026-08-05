from pathlib import Path
import re

path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
source = path.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global source
    count = source.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    source = source.replace(old, new, 1)


replace_once(
    '''    val currentLocale = LocalLocale.current.platformLocale
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(OnboardingStep.Intro) }
    var languageCode by remember(selectedLanguageCode) { mutableStateOf(LevyraLanguageCatalog.normalize(selectedLanguageCode)) }
''',
    '''    val currentLocale = LocalLocale.current.platformLocale
    val deviceLanguageCode = remember(currentLocale, selectedLanguageCode) {
        LevyraLanguageCatalog.normalize(
            currentLocale.language.ifBlank { selectedLanguageCode }
        )
    }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var name by remember { mutableStateOf("") }
    var step by remember { mutableStateOf(OnboardingStep.Intro) }
    var languageCode by remember(deviceLanguageCode) { mutableStateOf(deviceLanguageCode) }
''',
    "device locale initialization",
)

replace_once(
    '''                .fillMaxSize()
                .background(Color(0xFF030304))
                .consumeOverlayTouches()
''',
    '''                .fillMaxSize()
                .background(Color(0xFF030304))
''',
    "onboarding touch interceptor",
)

replace_once(
    "import com.luc4n3x.levyra.ui.player.playerSurfaceAlpha\n",
    '''import com.luc4n3x.levyra.ui.player.playerSurfaceAlpha
import com.luc4n3x.levyra.ui.player.playerSurfaceLiftFraction
import com.luc4n3x.levyra.ui.player.playerSurfaceScale
''',
    "player motion imports",
)

replace_once(
    '''                        .fillMaxSize()
                        .graphicsLayer { alpha = playerSurfaceAlpha(expansionProvider()) }
''',
    '''                        .fillMaxSize()
                        .graphicsLayer {
                            val expansion = expansionProvider()
                            alpha = playerSurfaceAlpha(expansion)
                            val surfaceScale = playerSurfaceScale(expansion)
                            scaleX = surfaceScale
                            scaleY = surfaceScale
                            translationY = playerSurfaceLiftFraction(expansion) * expansionTravelPx
                            transformOrigin = TransformOrigin(0.5f, 1f)
                        }
''',
    "full player surface motion",
)

open_spring = "spring(dampingRatio = 0.88f, stiffness = 420f)"
open_count = source.count(open_spring)
if open_count != 2:
    raise RuntimeError(f"opening springs: expected two matches, found {open_count}")
source = source.replace(open_spring, "spring(dampingRatio = 0.82f, stiffness = 360f)")

close_spring = "spring(dampingRatio = 0.9f, stiffness = 460f)"
close_count = source.count(close_spring)
if close_count != 2:
    raise RuntimeError(f"closing springs: expected two matches, found {close_count}")
source = source.replace(close_spring, "spring(dampingRatio = 0.86f, stiffness = 430f)")

intro = '''@Composable
private fun OnboardingIntroStage(strings: LevyraStrings, onStart: () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val pulse = if (animationsEnabled) {
        val transition = rememberInfiniteTransition(label = "intro-pulse")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(
                animation = tween(3_600, easing = LinearEasing),
                repeatMode = RepeatMode.Restart
            ),
            label = "intro-pulse-value"
        )
    } else {
        null
    }
    val features = remember(strings) {
        listOf(
            Triple(Icons.Rounded.GraphicEq, strings.introFeatureSound, LevyraCyan),
            Triple(Icons.Rounded.TextFields, strings.introFeatureLyrics, LevyraViolet),
            Triple(Icons.Rounded.Download, strings.introFeatureOffline, LevyraCyan)
        )
    }
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 28.dp,
            end = 28.dp,
            top = 18.dp,
            bottom = 32.dp
        ),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        item {
            Spacer(modifier = Modifier.height(18.dp))
            Box(contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .size(190.dp)
                        .graphicsLayer {
                            val halo = pulse?.value?.let { value ->
                                1f + 0.06f * kotlin.math.sin(value * 2f * Math.PI.toFloat())
                            } ?: 1f
                            scaleX = halo
                            scaleY = halo
                        }
                        .blur(46.dp)
                        .background(
                            Brush.radialGradient(
                                listOf(
                                    LevyraCyan.copy(alpha = 0.30f),
                                    LevyraViolet.copy(alpha = 0.20f),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                LevyraLogoMark(size = 92.dp)
            }
        }
        item {
            Spacer(modifier = Modifier.height(22.dp))
            LevyraWordmark(fontSize = 34.sp, dotSize = 6.dp)
            Spacer(modifier = Modifier.height(18.dp))
            Text(
                text = strings.introHeadline,
                color = LevyraText,
                fontSize = 38.sp,
                lineHeight = 42.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = (-1.3).sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = strings.introBody,
                color = LevyraMuted,
                fontSize = 16.sp,
                lineHeight = 23.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Column(
                modifier = Modifier.fillMaxWidth(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                features.forEach { (icon, label, tint) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(LevyraAdaptiveCardDeep, RoundedCornerShape(20.dp))
                            .border(1.dp, LevyraAdaptiveHairline, RoundedCornerShape(20.dp))
                            .padding(horizontal = 16.dp, vertical = 15.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(38.dp)
                                .background(tint.copy(alpha = 0.14f), RoundedCornerShape(13.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = label,
                            color = LevyraText,
                            fontSize = 14.5.sp,
                            lineHeight = 19.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        item {
            Spacer(modifier = Modifier.height(28.dp))
            Surface(
                color = LevyraCyan,
                shape = CircleShape,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(58.dp)
                    .pressable(onClick = onStart)
            ) {
                Row(
                    modifier = Modifier.fillMaxSize(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = strings.introStart,
                        color = LevyraBlack,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Black
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        Icons.AutoMirrored.Rounded.KeyboardArrowRight,
                        contentDescription = null,
                        tint = LevyraBlack,
                        modifier = Modifier.size(21.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun OnboardingLanguageStage'''

pattern = re.compile(
    r"@Composable\nprivate fun OnboardingIntroStage\(strings: LevyraStrings, onStart: \(\) -> Unit\) \{.*?\n\}\n\n@Composable\nprivate fun OnboardingLanguageStage",
    re.DOTALL,
)
source, count = pattern.subn(intro, source, count=1)
if count != 1:
    raise RuntimeError(f"intro stage replacement: expected one match, found {count}")

path.write_text(source, encoding="utf-8")
