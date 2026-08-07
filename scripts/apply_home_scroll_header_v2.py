from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]
APP = ROOT / "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt"
TEST = ROOT / "app/src/test/java/com/luc4n3x/levyra/ui/HomeScrollHeaderTest.kt"

text = APP.read_text(encoding="utf-8")


def replace_once(old: str, new: str, label: str) -> None:
    global text
    count = text.count(old)
    if count != 1:
        raise SystemExit(f"{label}: expected one match, found {count}")
    text = text.replace(old, new, 1)


# 1. Small pure helper: easy to test, no Compose state involved.
home_marker = "@Composable\nprivate fun HomeScreen(\n"
helpers = """private val HomeScrollHeaderTopPadding = 8.dp
private val HomeScrollHeaderExpandedGap = 16.dp
private val HomeScrollHeaderCollapsedGap = 6.dp
private val HomeScrollHeaderCollapseDistance = 88.dp
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
replace_once(home_marker, helpers, "insert scroll helper")

# 2. Build a lambda that reads LazyListState only from layout/draw callbacks.
old_inset = """    val homeBottomInset = tabBarBottomContentInset(
        miniPlayerVisible = state.currentTrack != null,
        animationsEnabled = state.animationsEnabled
    )
    Box(modifier = Modifier.fillMaxSize()) {
"""
new_inset = """    val homeBottomInset = tabBarBottomContentInset(
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
replace_once(old_inset, new_inset, "add scroll progress lambda")

# 3. The feed starts below the expanded overlay. The header is no longer a LazyColumn item.
old_lazy = """        LazyColumn(
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
new_lazy = """        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(
                top = HomeScrollHeaderExpandedHeight + homeSectionGap,
                bottom = homeBottomInset + LevyraBottomContentGap
            ),
            verticalArrangement = Arrangement.spacedBy(homeSectionGap)
        ) {
"""
replace_once(old_lazy, new_lazy, "move header outside LazyColumn")

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
replace_once(old_header_item, "", "remove old Home header item")

# 4. Attach the overlay at the exact HomeScreen tail, not a generic brace pair.
old_tail = """            if (state.homeError != null || state.playerError != null) {
                item(key = \"home-status\", contentType = \"home-card\") {
                    HomeSectionInset { StatusBlock(state) }
                }
            }
        }
    }

    addTarget?.let { track ->
"""
new_tail = """            if (state.homeError != null || state.playerError != null) {
                item(key = \"home-status\", contentType = \"home-card\") {
                    HomeSectionInset { StatusBlock(state) }
                }
            }
        }

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
replace_once(old_tail, new_tail, "attach scroll header overlay")

# 5. New header. Expanded and compact variants are always composed; only layout/draw reads scroll.
logo_marker = "@Composable\nprivate fun LevyraLogoMark(size: Dp = 58.dp) {\n"
header = """@Composable
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
    val strings = LocalLevyraStrings.current
    val surface = if (LevyraIsLight) Color(0xFFF8FAFF) else Color(0xFF050609)
    val divider = if (LevyraIsLight) Color(0x2011131F) else Color.White.copy(alpha = 0.09f)

    Box(
        modifier = modifier
            .fillMaxWidth()
            .drawWithContent {
                val progress = collapseProgress().coerceIn(0f, 1f)
                if (progress > 0f) {
                    drawRect(surface.copy(alpha = 0.92f * progress))
                }
                drawContent()
                if (progress > 0.08f) {
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
                        onSearch = onSearch,
                        onSettings = onSettings
                    )
                }

                HomeSectionInset {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .graphicsLayer {
                                val progress = collapseProgress().coerceIn(0f, 1f)
                                alpha = ((progress - 0.22f) / 0.78f).coerceIn(0f, 1f)
                                val scale = 0.96f + 0.04f * progress
                                scaleX = scale
                                scaleY = scale
                                transformOrigin = TransformOrigin(0f, 0.5f)
                            },
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        LevyraLogoMark(size = 32.dp)
                        Text(
                            text = \"LEVYRA\",
                            color = LevyraText,
                            fontSize = 18.sp,
                            lineHeight = 20.sp,
                            fontWeight = FontWeight.Black,
                            letterSpacing = (-0.55).sp,
                            maxLines = 1,
                            modifier = Modifier.weight(1f)
                        )
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

                MoodRow(
                    moods = moods,
                    selectedId = selectedMoodId,
                    onSelect = onSelectMood
                )
            }
        ) { measurables, constraints ->
            val expanded = measurables[0].measure(constraints.copy(minHeight = 0))
            val compact = measurables[1].measure(constraints.copy(minHeight = 0))
            val moods = measurables[2].measure(constraints.copy(minHeight = 0))
            val progress = collapseProgress().coerceIn(0f, 1f)
            val expandedGap = HomeScrollHeaderExpandedGap.roundToPx()
            val collapsedGap = HomeScrollHeaderCollapsedGap.roundToPx()
            val gap = (expandedGap + (collapsedGap - expandedGap) * progress).toInt()
            val titleHeight = (expanded.height + (compact.height - expanded.height) * progress).toInt()
            val layoutHeight = titleHeight + gap + moods.height

            layout(constraints.maxWidth, layoutHeight) {
                expanded.placeRelativeWithLayer(0, 0) {
                    alpha = (1f - progress * 1.35f).coerceIn(0f, 1f)
                    translationY = -expanded.height * 0.16f * progress
                }
                compact.placeRelative(0, 0)
                moods.placeRelative(0, titleHeight + gap)
            }
        }
    }
}

"""
replace_once(logo_marker, header + logo_marker, "insert scroll header composable")

APP.write_text(text, encoding="utf-8")

TEST.parent.mkdir(parents=True, exist_ok=True)
TEST.write_text(
    """package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class HomeScrollHeaderTest {
    @Test
    fun staysExpandedAtTheTop() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 88), 0.0001f)
    }

    @Test
    fun followsOffsetAndClamps() {
        assertEquals(0.5f, homeHeaderCollapseProgress(0, 44, 88), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 200, 88), 0.0001f)
        assertEquals(0f, homeHeaderCollapseProgress(0, -10, 88), 0.0001f)
    }

    @Test
    fun collapsesWhenFirstItemLeavesViewport() {
        assertEquals(1f, homeHeaderCollapseProgress(1, 0, 88), 0.0001f)
    }

    @Test
    fun handlesInvalidDistance() {
        assertEquals(0f, homeHeaderCollapseProgress(0, 0, 0), 0.0001f)
        assertEquals(1f, homeHeaderCollapseProgress(0, 1, 0), 0.0001f)
    }
}
""",
    encoding="utf-8",
)

updated = APP.read_text(encoding="utf-8")
for required in (
    "private fun HomeScrollReactiveHeader(",
    "homeHeaderCollapseProgress(",
    "collapseProgress = homeHeaderProgress",
    "top = HomeScrollHeaderExpandedHeight + homeSectionGap",
):
    if required not in updated:
        raise SystemExit(f"missing expected marker: {required}")
if 'item(key = "home-top"' in updated:
    raise SystemExit("legacy Home header item is still present")

print("Applied Home scroll-reactive header")
