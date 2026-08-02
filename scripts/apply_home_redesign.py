from pathlib import Path

APP_PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
EXPERIENCE_PATH = Path("app/src/main/java/com/luc4n3x/levyra/ui/HomeExperience.kt")


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly one match, found {count}")
    return text.replace(old, new, 1)


def extract_scope(text: str, start_marker: str, end_marker: str, label: str) -> tuple[int, int, str]:
    try:
        start = text.index(start_marker)
        end = text.index(end_marker, start)
    except ValueError as exc:
        raise RuntimeError(f"{label}: scope marker not found") from exc
    return start, end, text[start:end]


app = APP_PATH.read_text(encoding="utf-8")

# HomeExperience is committed before this script runs; correct the nullable artwork fallback in-place.
experience = EXPERIENCE_PATH.read_text(encoding="utf-8")
experience = replace_once(
    experience,
    """    val artworkUrl = item.track?.thumbnailUrl
        ?.ifBlank { item.track.largeThumbnailUrl }
        .orEmpty()
""",
    """    val artworkUrl = item.track
        ?.let { track -> track.thumbnailUrl.ifBlank { track.largeThumbnailUrl } }
        .orEmpty()
""",
    "Home quick-access artwork fallback",
)
EXPERIENCE_PATH.write_text(experience, encoding="utf-8")

# Work inside HomeScreen first so moving blocks cannot affect similarly named components elsewhere.
home_start, home_end, home = extract_scope(
    app,
    "@Composable\nprivate fun HomeScreen(",
    "\nprivate fun homeSpotlightBadge",
    "HomeScreen",
)

home = replace_once(
    home,
    """    val visiblePersonalTracks = remember(personalTracks) {
    LevyraPersonalOrbit.distinctRecordings(personalTracks)
        .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
}
val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
""",
    """    val visiblePersonalTracks = remember(personalTracks) {
        LevyraPersonalOrbit.distinctRecordings(personalTracks)
            .take(LevyraPersonalOrbit.DISPLAY_LIMIT)
    }
    val visibleEditorialCollections = remember(editorialCollections, spotlightCandidate?.track?.id) {
""",
    "Home derived content indentation",
)

home = replace_once(
    home,
    """    val homeBottomInset = tabBarBottomContentInset(
""",
    """    val homeMixTracks = remember(quickPicks, visiblePersonalTracks, resonanceTracks) {
        quickPicks?.tracks?.takeIf { it.isNotEmpty() }
            ?: visiblePersonalTracks.takeIf { it.isNotEmpty() }
            ?: resonanceTracks
    }
    val homeReleaseTracks = remember(newReleases) { newReleases?.tracks.orEmpty() }
    val homeBottomInset = tabBarBottomContentInset(
""",
    "Home quick-access sources",
)

home = replace_once(
    home,
    """        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(430.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(
                            animatedHomeAccentStart.copy(alpha = 0.28f),
                            animatedHomeAccentEnd.copy(alpha = 0.14f),
                            Color.Transparent
                        )
                    )
                )
        )
""",
    """        LevyraHomeAtmosphere(
            accentStart = animatedHomeAccentStart,
            accentEnd = animatedHomeAccentEnd,
            isLight = LevyraIsLight,
            animationsEnabled = state.animationsEnabled,
            modifier = Modifier.fillMaxWidth()
        )
""",
    "Home atmosphere",
)

home = replace_once(
    home,
    """        LazyColumn(
        state = homeListState,
        modifier = Modifier.fillMaxSize().statusBarsPadding(),
        contentPadding = PaddingValues(top = 8.dp, bottom = homeBottomInset),
        verticalArrangement = Arrangement.spacedBy(if (state.interfaceSettings.compactHome) 14.dp else 26.dp)
    ) {
""",
    """        LazyColumn(
            state = homeListState,
            modifier = Modifier.fillMaxSize().statusBarsPadding(),
            contentPadding = PaddingValues(top = 10.dp, bottom = homeBottomInset),
            verticalArrangement = Arrangement.spacedBy(
                if (state.interfaceSettings.compactHome) 12.dp else 22.dp
            )
        ) {
""",
    "Home list layout",
)

home = replace_once(
    home,
    """                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                    GreetingBar(state.userName, state.isResolving, onSettings = viewModel::openSettings)
                    MoodRow(moods = state.moods, selectedId = state.selectedMood?.id, onSelect = viewModel::selectMood)
                }
""",
    """                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    GreetingBar(state.userName, state.isResolving, onSettings = viewModel::openSettings)
                    MoodRow(moods = state.moods, selectedId = state.selectedMood?.id, onSelect = viewModel::selectMood)
                }
""",
    "Home header spacing",
)

quick_access_block = """        item(key = "home-quick-access", contentType = "home-quick-access") {
            HomeSectionInset {
                LevyraHomeQuickAccessGrid(
                    currentTrack = state.currentTrack,
                    mixTrack = homeMixTracks.firstOrNull(),
                    favoriteTrack = state.favorites.firstOrNull(),
                    releaseTrack = homeReleaseTracks.firstOrNull(),
                    chartTrack = state.charts.firstOrNull(),
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    hasMix = homeMixTracks.isNotEmpty(),
                    hasFavorites = state.favorites.isNotEmpty(),
                    hasNewReleases = homeReleaseTracks.isNotEmpty(),
                    hasCharts = state.charts.isNotEmpty(),
                    isLight = LevyraIsLight,
                    onContinue = viewModel::togglePlay,
                    onMix = { viewModel.playAll(homeMixTracks) },
                    onFavorites = { viewModel.playAll(state.favorites) },
                    onNewReleases = { viewModel.playAll(homeReleaseTracks) },
                    onCharts = { viewModel.playAll(state.charts) },
                    onSearch = { viewModel.searchNow() }
                )
            }
        }
"""
home = replace_once(
    home,
    """        spotlightCandidate?.let { candidate ->
""",
    quick_access_block + """        spotlightCandidate?.let { candidate ->
""",
    "Home quick-access placement",
)

continue_block = """        if (state.currentTrack != null && !state.isPlaying && !state.isResolving) {
            item(key = "home-continue", contentType = "home-card") {
                HomeContinueListeningCard(
                    viewModel = viewModel,
                    track = state.currentTrack
                )
            }
        }
"""
home = replace_once(home, continue_block, "", "Remove old continue-listening placement")
home = replace_once(
    home,
    """        if (state.interfaceSettings.showPersonalOrbit && visiblePersonalTracks.isNotEmpty()) {
""",
    continue_block + """        if (state.interfaceSettings.showPersonalOrbit && visiblePersonalTracks.isNotEmpty()) {
""",
    "Continue-listening placement",
)

quick_picks_block = """        if (showDeferredHomeSections && quickPicks != null && quickPicks.tracks.isNotEmpty()) {
            item(key = "home-quick-picks", contentType = "home-dense-shelf") {
                HomeQuickPicksShelf(
                    title = quickPicks.title.ifBlank { strings.quickPicks },
                    tracks = quickPicks.tracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(quickPicks.tracks, track) },
                    onPlayAll = { viewModel.playAll(quickPicks.tracks) }
                )
            }
        }
"""
home = replace_once(home, quick_picks_block, "", "Remove old quick-picks placement")
home = replace_once(
    home,
    """        if (showDeferredHomeSections && homeVideoTracks.isNotEmpty()) {
""",
    quick_picks_block + """        if (showDeferredHomeSections && homeVideoTracks.isNotEmpty()) {
""",
    "Quick-picks placement",
)

video_block = """        if (showDeferredHomeSections && homeVideoTracks.isNotEmpty()) {
            item(key = "home-music-videos", contentType = "home-horizontal-row") {
                HomeMusicVideoShelf(
                    title = strings.exploreNewVideos,
                    tracks = homeVideoTracks,
                    currentId = state.currentTrack?.id,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    onPlay = { track -> viewModel.playFrom(homeVideoTracks, track) },
                    onPlayAll = { viewModel.playAll(homeVideoTracks) }
                )
            }
        }
"""
home = replace_once(home, video_block, "", "Remove old music-video placement")
home = replace_once(
    home,
    """        if (showDeferredHomeSections) otherSections.forEachIndexed { sectionIndex, section ->
""",
    video_block + """        if (showDeferredHomeSections) otherSections.forEachIndexed { sectionIndex, section ->
""",
    "Music-video placement",
)

app = app[:home_start] + home + app[home_end:]

# Compact the identity header without changing account/settings behaviour.
greeting_start, greeting_end, greeting = extract_scope(
    app,
    "@Composable\nprivate fun GreetingBar(",
    "\n@Composable\nprivate fun MetroHeroDeck",
    "GreetingBar",
)
greeting = replace_once(
    greeting,
    "horizontalArrangement = Arrangement.spacedBy(16.dp)",
    "horizontalArrangement = Arrangement.spacedBy(12.dp)",
    "Greeting row spacing",
)
greeting = replace_once(
    greeting,
    "verticalArrangement = Arrangement.spacedBy(10.dp)",
    "verticalArrangement = Arrangement.spacedBy(8.dp)",
    "Greeting column spacing",
)
greeting = replace_once(
    greeting,
    "val settingsElevation = if (isLight) 3.dp else 10.dp",
    "val settingsElevation = if (isLight) 2.dp else 8.dp",
    "Greeting settings elevation",
)
greeting = replace_once(
    greeting,
    "LevyraLogoMark(size = 46.dp)",
    "LevyraLogoMark(size = 40.dp)",
    "Greeting logo size",
)
greeting = replace_once(
    greeting,
    "LevyraWordmark(fontSize = 31.sp, dotSize = 5.dp)",
    "LevyraWordmark(fontSize = 29.sp, dotSize = 4.dp)",
    "Greeting wordmark size",
)
greeting = replace_once(
    greeting,
    ".size(46.dp)",
    ".size(42.dp)",
    "Greeting settings size",
)
app = app[:greeting_start] + greeting + app[greeting_end:]

# Mood chips remain readable but no longer dominate the first viewport.
mood_start, mood_end, mood = extract_scope(
    app,
    "@Composable\nprivate fun MoodRow(",
    "\n@Composable\nprivate fun SectionHeaderAction",
    "MoodRow",
)
mood = replace_once(
    mood,
    "horizontalArrangement = Arrangement.spacedBy(9.dp)",
    "horizontalArrangement = Arrangement.spacedBy(8.dp)",
    "Mood spacing",
)
mood = replace_once(
    mood,
    "modifier = Modifier.padding(horizontal = 17.dp, vertical = 11.dp)",
    "modifier = Modifier.padding(horizontal = 14.dp, vertical = 9.dp)",
    "Mood padding",
)
app = app[:mood_start] + mood + app[mood_end:]

# Keep a single hero, but make it calmer and leave more room for useful content above the fold.
spotlight_start = app.index("@Composable\nprivate fun HomeEditorialSpotlight(")
spotlight_end = app.index("\n@Composable\nprivate fun ", spotlight_start + 24)
spotlight = app[spotlight_start:spotlight_end]
spotlight = replace_once(
    spotlight,
    "val shape = RoundedCornerShape(28.dp)",
    "val shape = RoundedCornerShape(22.dp)",
    "Spotlight radius",
)
spotlight = replace_once(
    spotlight,
    ".height(232.dp)",
    ".height(216.dp)",
    "Spotlight height",
)
spotlight = replace_once(
    spotlight,
    "elevation = 18.dp",
    "elevation = 12.dp",
    "Spotlight elevation",
)
spotlight = replace_once(
    spotlight,
    "val artworkWidth = maxWidth * 0.66f",
    "val artworkWidth = maxWidth * 0.62f",
    "Spotlight artwork width",
)
app = app[:spotlight_start] + spotlight + app[spotlight_end:]

APP_PATH.write_text(app, encoding="utf-8")
print("Levyra Home redesign patch applied successfully")
