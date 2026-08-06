from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


# ViewModel: load cache synchronously, prefetch early, preserve cached rows while refreshing.
vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text(encoding="utf-8")
vm = replace_once(
    vm,
    "import com.luc4n3x.levyra.data.YoutubeShortsRepository\n",
    "import com.luc4n3x.levyra.data.YoutubeShortsRepository\nimport com.luc4n3x.levyra.data.YoutubeShortsCache\n",
    "shorts cache import",
)
vm = replace_once(
    vm,
    "    private val shortsRepository = YoutubeShortsRepository(application.applicationContext)\n",
    "    private val shortsRepository = YoutubeShortsRepository(application.applicationContext)\n    private val shortsCache = YoutubeShortsCache(application.applicationContext)\n",
    "shorts cache field",
)
vm = replace_once(
    vm,
    "        val settings = startupSettings\n",
    "        val settings = startupSettings\n        val startupShortsSnapshot = shortsCache.load(settings.languageCode)\n        val startupShorts = startupShortsSnapshot.tracks\n",
    "startup Shorts cache load",
)
vm = replace_once(
    vm,
    "                charts = startupCharts,\n                selectedChartId = defaultChartRegion.id,\n",
    "                charts = startupCharts,\n                exploreVideos = startupShorts,\n                selectedChartId = defaultChartRegion.id,\n",
    "publish cached Shorts at startup",
)
vm = replace_once(
    vm,
    "        scheduleColdStartRefresh(initialTracks)\n        LevyraWidgetBridge.onToggle = { togglePlay() }\n",
    "        scheduleColdStartRefresh(initialTracks)\n        viewModelScope.launch {\n            delay(if (startupShortsSnapshot.isFresh()) 1_200L else 80L)\n            ensureMusicVideosLoaded()\n        }\n        LevyraWidgetBridge.onToggle = { togglePlay() }\n",
    "early Shorts prefetch",
)
vm = replace_once(
    vm,
    '''            _state.update { current ->
                if (current.languageCode == languageCode) {
                    current.copy(exploreVideos = feedResult.tracks)
                } else {
                    current
                }
            }
''',
    '''            if (feedResult.tracks.isNotEmpty()) {
                _state.update { current ->
                    if (current.languageCode == languageCode) {
                        current.copy(exploreVideos = feedResult.tracks)
                    } else {
                        current
                    }
                }
                withContext(Dispatchers.IO) {
                    shortsCache.save(languageCode, feedResult.tracks)
                }
            }
''',
    "stale while revalidate publish",
)
vm_path.write_text(vm, encoding="utf-8")

# Explore entry: opening Samples can never be a no-op.
app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text(encoding="utf-8")
app = replace_once(
    app,
    '''        if (shortcut == ExploreShortcut.Samples) {
            samplesStartIndex = 0
            if (samples.isNotEmpty()) {
                viewModel.beginSamplesPlayback()
            } else {
                viewModel.refreshSamples()
            }
        } else {
''',
    '''        if (shortcut == ExploreShortcut.Samples) {
            samplesStartIndex = 0
            viewModel.beginSamplesPlayback()
            if (samples.isEmpty()) viewModel.refreshSamples()
        } else {
''',
    "Samples shortcut opens immediately",
)
app = replace_once(
    app,
    '''        samplesStartIndex
            ?.takeIf { samples.isNotEmpty() }
            ?.let { initialPage ->
                ExploreSamplesScreen(
''',
    '''        samplesStartIndex?.let { initialPage ->
                ExploreSamplesScreen(
''',
    "mount Samples overlay while feed is empty",
)
app = replace_once(
    app,
    '''                    onTogglePlay = viewModel::togglePlay,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDismiss = {
''',
    '''                    onTogglePlay = viewModel::togglePlay,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onRequestFeed = viewModel::refreshSamples,
                    onDismiss = {
''',
    "Samples loading callback",
)
app_path.write_text(app, encoding="utf-8")

# Samples screen: show a real destination immediately and transition into the pager when data arrives.
screen_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreSamplesScreen.kt")
screen = screen_path.read_text(encoding="utf-8")
screen = replace_once(
    screen,
    "import androidx.compose.material.icons.rounded.PlayArrow\n",
    "import androidx.compose.material.icons.rounded.PlayArrow\nimport androidx.compose.material.icons.rounded.Refresh\n",
    "Refresh icon import",
)
screen = replace_once(
    screen,
    '''    onTogglePlay: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onDismiss: () -> Unit
) {
    if (samples.isEmpty()) return
    BackHandler(onBack = onDismiss)
''',
    '''    onTogglePlay: () -> Unit,
    onToggleFavorite: (Track) -> Unit,
    onRequestFeed: () -> Unit,
    onDismiss: () -> Unit
) {
    BackHandler(onBack = onDismiss)
''',
    "Samples screen signature and empty handling",
)
screen = replace_once(
    screen,
    '''    DisposableEffect(Unit) {
        onDispose { latestOnDismiss.value() }
    }

    val safeInitialPage = initialPage.coerceIn(0, samples.lastIndex)
''',
    '''    DisposableEffect(Unit) {
        onDispose { latestOnDismiss.value() }
    }

    if (samples.isEmpty()) {
        LaunchedEffect(Unit) { onRequestFeed() }
        ExploreSamplesLoadingSurface(
            strings = strings,
            onRetry = onRequestFeed,
            onDismiss = onDismiss
        )
        return
    }

    val safeInitialPage = initialPage.coerceIn(0, samples.lastIndex)
''',
    "render Samples loading destination",
)
screen = replace_once(
    screen,
    '''    LaunchedEffect(pagerState, samples) {
        snapshotFlow { pagerState.settledPage }
''',
    '''    val sampleIdentity = samples.joinToString(separator = "|") { track -> track.id }
    LaunchedEffect(pagerState, sampleIdentity) {
        snapshotFlow { pagerState.settledPage }
''',
    "stable Samples pager key",
)
insert_before = "\n@Composable\nprivate fun ExploreSamplePage(\n"
loading_surface = '''

@Composable
private fun ExploreSamplesLoadingSurface(
    strings: LevyraStrings,
    onRetry: () -> Unit,
    onDismiss: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .zIndex(30f)
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .statusBarsPadding()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(42.dp)
                    .background(Color.Black.copy(alpha = 0.48f), CircleShape)
                    .clickable(onClick = onDismiss),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Close,
                    contentDescription = strings.close,
                    tint = Color.White,
                    modifier = Modifier.size(21.dp)
                )
            }
            Text(
                text = strings.exploreSamples,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.Black,
                modifier = Modifier.weight(1f)
            )
        }

        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            CircularProgressIndicator(
                color = Color.White,
                strokeWidth = 3.dp,
                modifier = Modifier.size(44.dp)
            )
            Text(
                text = strings.exploreSamplesSubtitle,
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            Box(
                modifier = Modifier
                    .background(Color.White.copy(alpha = 0.12f), CircleShape)
                    .clickable(onClick = onRetry)
                    .padding(horizontal = 18.dp, vertical = 10.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Refresh,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                    Text(
                        text = strings.exploreSamples,
                        color = Color.White,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
'''
if insert_before not in screen:
    raise RuntimeError("loading surface insertion point not found")
screen = screen.replace(insert_before, loading_surface + insert_before, 1)
screen_path.write_text(screen, encoding="utf-8")

print("Applied instant-open, cached, stale-while-revalidate Samples flow")
