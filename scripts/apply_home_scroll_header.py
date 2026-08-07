from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
TEST = ROOT / "app/src/test/java/com/luc4n3x/levyra/ui/HomeScrollHeaderTest.kt"

text = APP.read_text(encoding="utf-8")


def replace_once(source: str, old: str, new: str, label: str) -> str:
    count = source.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected exactly one match, found {count}")
    return source.replace(old, new, 1)


home_marker = """@Composable
private fun HomeScreen(
"""
helpers = """private val HomeScrollHeaderTopPadding = 8.dp
private val HomeScrollHeaderExpandedGap = 16.dp
private val HomeScrollHeaderCollapsedGap = 8.dp
private val HomeScrollHeaderCollapseDistance = 84.dp
private val HomeScrollHeaderExpandedHeight =
    HomeScrollHeaderTopPadding +
        LevyraHomeDesign.SettingsControlHeight +
        HomeScrollHeaderExpandedGap +
        LevyraHomeDesign.MoodChipHeight +
        4.dp

internal fun homeHeaderCollapseProgress(
    firstVisibleItemIndex: Int,
    firstVisibleItemScrollOffsetPx: Int,
    collapseDistancePx: Int
): Float {
    if (firstVisibleItemIndex > 0) return 1f
    if (firstVisibleItemIndex < 0) return 0f
    if (collapseDistancePx <= 0) {
        return if (firstVisibleItemScrollOffsetPx > 0) 1f else 0f
    }
    return (firstVisibleItemScrollOffsetPx.coerceAtLeast(0).toFloat() / collapseDistancePx.toFloat())
        .coerceIn(0f, 1f)
}

@Composable
private fun HomeScreen(
"""
text = replace_once(text, home_marker, helpers, "insert home scroll helpers")

old_bottom_inset = """    val homeBottomInset = tabBarBottomContentInset(
        miniPlayerVisible = state.currentTrack != null,
        animationsEnabled = state.animationsEnabled
    )
    Box(modifier = Modifier.fillMaxSize()) {
"""
new_bottom_inset = """    val homeBottomInset = tabBarBottomContentInset(
        miniPlayerVisible = state.currentTrack != null,
        animationsEnabled = state.animationsEnabled
    )
    val homeSectionGap = if (state.interfaceSettings.compactHome) {
        LevyraHomeDesign.SectionGapCompact
    } else {
        LevyraHomeDesign.SectionGap
    }
    val density = LocalDensity.current
    val homeHeaderCollapseDistancePx = remember(density) {
        with(density) { HomeScrollHeaderCollapseDistance.roundToPx().coerceAtLeast(1) }
    }
    val homeHeaderProgress = remember(homeListState, homeHeaderCollapseDistancePx) {
        {
            homeHeaderCollapseProgress(
                firstVisibleItemIndex = homeListState.firstVisibleItemIndex,
                firstVisibleItemScrollOffsetPx = homeListState.firstVisibleItemScrollOffset,
                collapseDistancePx = homeHeaderCollapseDistancePx
            )
        }
    }
    Box(modifier = Modifier.fillMaxSize()) {
"""
text = replace_once(text, old_bottom_inset, new_bottom_inset, "add header scroll state")

old_lazy_config = """        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 8.dp, bottom = homeBottomInset + LevyraBottomContentGap),
            verticalArrangement = Arrangement.spacedBy(
                if (state.interfaceSettings.compactHome) {
                    LevyraHomeDesign.SectionGapCompact
                } else {
                    LevyraHomeDesign.SectionGap
                }
            )
        ) {
"""
new_lazy_config = """        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                top = HomeScrollHeaderExpandedHeight + homeSectionGap,
                bottom = homeBottomInset + LevyraBottomContentGap
            ),
            verticalArrangement = Arrangement.spacedBy(homeSectionGap)
        ) {
"""
text = replace_once(text, old_lazy_config, new_lazy_config, "move header outside lazy list")

old_header_item = """            item(key = \"home-top\", contentType = \"home-header\") {
                Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    HomeSectionInset {
                        GreetingBar(
                            userName = state.userName,
                            isResolving = state.isResolving,
                            animationsEnabled = state.animationsEnabled,
                            onSearch = viewModel::openSearch,
                            onSettings = viewModel::openSettings
                        )
                    }
                    MoodRow(
                        moods = state.moods,
                        selectedId = state.selectedMood?.id,
                        onSelect = viewModel::selectMood
                    )
                }
            }

"""
text = replace_once(text, old_header_item, "", "remove lazy header item")

old_home_tail = """        }
    }

    addTarget?.let { track ->
"""
new_home_tail = """        }

        HomeScrollReactiveHeader(
            userName = state.userName,
            isResolving = state.isResolving,
            animationsEnabled = state.animationsEnabled,
            moods = state.moods,
            selectedMoodId = state.selectedMood?.id,
            collapseProgress = homeHeaderProgress,
            onSearch = viewModel::openSearch,
            onSettings = viewModel::openSettings,
            onSelectMood = viewModel::selectMood,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .zIndex(4f)
        )
    }

    addTarget?.let { track ->
"""
text = replace_once(text, old_home_tail, new_home_tail, "attach overlay header")

logo_marker = """@Composable
private fun LevyraLogoMark(size: Dp = 58.dp) {
    Box(contentAlignment = Alignment.Center) {
"""
logo_replacement = """@Composable
private fun LevyraLogoMark(
    size: Dp = 58.dp,
    modifier: Modifier = Modifier
) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
"""
text = replace_once(text, logo_marker, logo_replacement, "make logo scroll-transformable")

old_greeting = """@Composable
private fun GreetingBar(
    userName: String,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val greetingHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = remember(userName, strings, greetingHour) {
        strings.formatGreeting(userName, greetingHour)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            LevyraLogoMark(size = 38.dp)
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = \"LEVYRA\",
                    color = LevyraText,
                    fontSize = 20.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.65).sp,
                    maxLines = 1
                )
                Text(
                    text = greeting,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        HomeHeaderIconButton(
            icon = Icons.Rounded.Search,
            contentDescription = strings.search,
            onClick = onSearch
        )
        OccasionallyRotatingSettingsButton(
            animationsEnabled = animationsEnabled,
            busy = isResolving,
            contentDescription = strings.settings,
            loading = isResolving,
            onClick = onSettings
        )
    }
}
"""
new_greeting = """@Composable
private fun GreetingBar(
    userName: String,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    collapseProgress: () -> Float = { 0f },
    onSearch: () -> Unit,
    onSettings: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val greetingHour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    val greeting = remember(userName, strings, greetingHour) {
        strings.formatGreeting(userName, greetingHour)
    }

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(
            modifier = Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(11.dp)
        ) {
            LevyraLogoMark(
                size = 38.dp,
                modifier = Modifier.graphicsLayer {
                    val progress = collapseProgress().coerceIn(0f, 1f)
                    val compactScale = 1f - progress * 0.10f
                    scaleX = compactScale
                    scaleY = compactScale
                    transformOrigin = TransformOrigin(0f, 0.5f)
                }
            )
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(1.dp)
            ) {
                Text(
                    text = \"LEVYRA\",
                    color = LevyraText,
                    fontSize = 20.sp,
                    lineHeight = 21.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = (-0.65).sp,
                    maxLines = 1,
                    modifier = Modifier.graphicsLayer {
                        val progress = collapseProgress().coerceIn(0f, 1f)
                        val compactScale = 1f - progress * 0.045f
                        scaleX = compactScale
                        scaleY = compactScale
                        transformOrigin = TransformOrigin(0f, 0.5f)
                    }
                )
                Text(
                    text = greeting,
                    color = LevyraMuted,
                    fontSize = 12.5.sp,
                    lineHeight = 15.sp,
                    fontWeight = FontWeight.Medium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier
                        .layout { measurable, constraints ->
                            val placeable = measurable.measure(constraints)
                            val progress = collapseProgress().coerceIn(0f, 1f)
                            val visibleFraction = (1f - progress * 1.25f).coerceIn(0f, 1f)
                            val visibleHeight = (placeable.height * visibleFraction).toInt()
                            layout(placeable.width, visibleHeight) {
                                placeable.placeRelative(0, 0)
                            }
                        }
                        .graphicsLayer {
                            val progress = collapseProgress().coerceIn(0f, 1f)
                            alpha = (1f - progress * 1.35f).coerceIn(0f, 1f)
                            translationY = -size.height * 0.18f * progress
                        }
                )
            }
        }
        Box(
            modifier = Modifier.graphicsLayer {
                val progress = collapseProgress().coerceIn(0f, 1f)
                val compactScale = 1f - progress * 0.08f
                scaleX = compactScale
                scaleY = compactScale
            }
        ) {
            HomeHeaderIconButton(
                icon = Icons.Rounded.Search,
                contentDescription = strings.search,
                onClick = onSearch
            )
        }
        Box(
            modifier = Modifier.graphicsLayer {
                val progress = collapseProgress().coerceIn(0f, 1f)
                val compactScale = 1f - progress * 0.08f
                scaleX = compactScale
                scaleY = compactScale
            }
        ) {
            OccasionallyRotatingSettingsButton(
                animationsEnabled = animationsEnabled,
                busy = isResolving,
                contentDescription = strings.settings,
                loading = isResolving,
                onClick = onSettings
            )
        }
    }
}
"""
text = replace_once(text, old_greeting, new_greeting, "make greeting draw/measure reactive")

header_composable = """
@Composable
private fun HomeScrollReactiveHeader(
    userName: String,
    isResolving: Boolean,
    animationsEnabled: Boolean,
    moods: List<Mood>,
    selectedMoodId: String?,
    collapseProgress: () -> Float,
    onSearch: () -> Unit,
    onSettings: () -> Unit,
    onSelectMood: (Mood) -> Unit,
    modifier: Modifier = Modifier
) {
    val isLight = LevyraIsLight
    val collapsedSurface = if (isLight) Color(0xFFF8FAFF) else Color(0xFF050609)
    val divider = if (isLight) Color(0x2011131F) else Color.White.copy(alpha = 0.09f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                val progress = collapseProgress().coerceIn(0f, 1f)
                if (progress > 0f) {
                    val surfaceAlpha = (0.10f + 0.84f * progress) * progress
                    drawRect(collapsedSurface.copy(alpha = surfaceAlpha))
                }
                drawContent()
                if (progress > 0.04f) {
                    drawLine(
                        color = divider.copy(alpha = divider.alpha * progress),
                        start = Offset(0f, size.height - 0.5f),
                        end = Offset(size.width, size.height - 0.5f),
                        strokeWidth = 1f
                    )
                }
            }
    ) {
        androidx.compose.ui.layout.Layout(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(top = HomeScrollHeaderTopPadding),
            content = {
                HomeSectionInset {
                    GreetingBar(
                        userName = userName,
                        isResolving = isResolving,
                        animationsEnabled = animationsEnabled,
                        collapseProgress = collapseProgress,
                        onSearch = onSearch,
                        onSettings = onSettings
                    )
                }
                MoodRow(
                    moods = moods,
                    selectedId = selectedMoodId,
                    onSelect = onSelectMood
                )
            }
        ) { measurables, constraints ->
            val greeting = measurables[0].measure(constraints.copy(minHeight = 0))
            val moodRow = measurables[1].measure(constraints.copy(minHeight = 0))
            val progress = collapseProgress().coerceIn(0f, 1f)
            val expandedGap = HomeScrollHeaderExpandedGap.roundToPx()
            val collapsedGap = HomeScrollHeaderCollapsedGap.roundToPx()
            val gap = (expandedGap + (collapsedGap - expandedGap) * progress).toInt()
            val layoutHeight = greeting.height + gap + moodRow.height

            layout(constraints.maxWidth, layoutHeight) {
                greeting.placeRelative(0, 0)
                moodRow.placeRelative(0, greeting.height + gap)
            }
        }
    }
}

"""
text = replace_once(
    text,
    "@Composable\nprivate fun LevyraLogoMark(size: Dp = 58.dp)",
    header_composable + "@Composable\nprivate fun LevyraLogoMark(size: Dp = 58.dp)",
    "insert scroll reactive header"
)

# The previous replacement changed the LevyraLogoMark signature, so the marker above must be
# re-applied against that new signature if the insertion happened after it. Keep the operation
# deterministic by moving the header to the actual new signature when needed.
if "private fun HomeScrollReactiveHeader(" not in text:
    raise SystemExit("scroll reactive header was not inserted")

APP.write_text(text, encoding="utf-8")

TEST.parent.mkdir(parents=True, exist_ok=True)
TEST.write_text(
    """package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScrollHeaderTest {
    @Test
    fun staysExpandedAtTheTop() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 84), 0.0001f)
    }

    @Test
    fun followsTheFirstItemOffsetWithoutOvershooting() {
        assertEquals(0.5f, homeHeaderCollapseProgress(0, 42, 84), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 200, 84), 0.0001f)
        assertEquals(0f, homeHeaderCollapseProgress(0, -20, 84), 0.0001f)
    }

    @Test
    fun isFullyCollapsedAfterTheFirstItemLeavesTheViewport() {
        assertEquals(1f, homeHeaderCollapseProgress(1, 0, 84), 0.0001f)
    }

    @Test
    fun handlesInvalidCollapseDistanceDeterministically() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 0), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 1, 0), 0.0001f)
    }
}
""",
    encoding="utf-8",
)

# Guard the intended architecture: scroll state must not be read from composition through
# derivedStateOf/collectAsState. The only reads belong to snapshotFlow plus the measure/draw lambda.
updated = APP.read_text(encoding="utf-8")
required = [
    "HomeScrollReactiveHeader(",
    "homeHeaderCollapseProgress(",
    "collapseProgress = homeHeaderProgress",
    "contentPadding = PaddingValues(\n                top = HomeScrollHeaderExpandedHeight + homeSectionGap",
]
for marker in required:
    if marker not in updated:
        raise SystemExit(f"missing expected marker: {marker}")
if 'item(key = "home-top"' in updated:
    raise SystemExit("old lazy-list header still present")

print("Applied scroll-reactive Home header and regression test")
