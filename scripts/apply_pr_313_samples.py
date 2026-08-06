from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


layout_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt")
layout = layout_path.read_text()
layout = replace_once(
    layout,
    "internal fun exploreSampleTracks(videos: List<Track>, limit: Int = ExploreSampleLimit): List<Track> {",
    "internal fun exploreSampleTracks(videos: List<Track>, limit: Int = Int.MAX_VALUE): List<Track> {",
    "expose complete Samples feed",
)
layout_path.write_text(layout)


test_path = Path("app/src/test/java/com/luc4n3x/levyra/ui/ExploreLayoutTest.kt")
tests = test_path.read_text()
tests = replace_once(
    tests,
    '''    @Test
    fun samplesRespectTheRequestedBound() {
        val videos = List(ExploreSampleLimit + 4) { index -> track("id-$index") }

        assertEquals(ExploreSampleLimit, exploreSampleTracks(videos).size)
        assertEquals(3, exploreSampleTracks(videos, limit = 3).size)
        assertTrue(exploreSampleTracks(videos, limit = 0).isEmpty())
        assertTrue(exploreSampleTracks(videos, limit = -1).isEmpty())
    }
''',
    '''    @Test
    fun samplesExposeTheFullFeedAndRespectPreviewBounds() {
        val videos = List(ExploreSampleLimit + 4) { index -> track("id-$index") }

        assertEquals(videos.size, exploreSampleTracks(videos).size)
        assertEquals(ExploreSampleLimit, exploreSampleTracks(videos, limit = ExploreSampleLimit).size)
        assertEquals(3, exploreSampleTracks(videos, limit = 3).size)
        assertTrue(exploreSampleTracks(videos, limit = 0).isEmpty())
        assertTrue(exploreSampleTracks(videos, limit = -1).isEmpty())
    }
''',
    "update Samples feed test",
)
test_path.write_text(tests)


screens_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
screens = screens_path.read_text()
screens = replace_once(
    screens,
    '''class ExploreViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::exploreProjection) {
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun selectExploreZone(zone: ExploreZone) = root.selectExploreZone(zone)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
}
''',
    '''class ExploreViewModel(root: LevyraViewModel) : LevyraScreenViewModel(root, ::exploreProjection) {
    fun addToPlaylist(playlistId: String, track: Track) = root.addToPlaylist(playlistId, track)
    fun createPlaylist(name: String, firstTrack: Track? = null) = root.createPlaylist(name, firstTrack)
    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
    fun playSample(list: List<Track>, track: Track) = root.playSample(list, track)
    fun selectExploreZone(zone: ExploreZone) = root.selectExploreZone(zone)
    fun toggleFavorite(track: Track) = root.toggleFavorite(track)
    fun togglePlay() = root.togglePlay()
}
''',
    "expose Samples player actions",
)
screens_path.write_text(screens)


view_model_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
view_model = view_model_path.read_text()
view_model = replace_once(
    view_model,
    '''    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) {
        if (list.isEmpty()) return
        loopCurrentQueueOnCompletion = loopOnCompletion
        val index = list.indexOfFirst { samePlayableTrack(it, track) }.coerceAtLeast(0)
        queueEngine.replace(list, index, keepPlaybackModes = true, radioEnabled = queueEngine.state.value.radioEnabled)
        queueIndex = index
        startResolve(list.getOrElse(index) { track })
    }
''',
    '''    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) {
        if (list.isEmpty()) return
        loopCurrentQueueOnCompletion = loopOnCompletion
        val index = list.indexOfFirst { samePlayableTrack(it, track) }.coerceAtLeast(0)
        queueEngine.replace(list, index, keepPlaybackModes = true, radioEnabled = queueEngine.state.value.radioEnabled)
        queueIndex = index
        startResolve(list.getOrElse(index) { track })
    }

    fun playSample(list: List<Track>, track: Track) {
        if (list.isEmpty()) return
        val selected = list.firstOrNull { candidate -> samePlayableTrack(candidate, track) } ?: track
        val snapshot = _state.value
        val alreadyActive = snapshot.currentTrack?.let { current -> samePlayableTrack(current, selected) } == true &&
            snapshot.isVideoMode

        loopCurrentQueueOnCompletion = true
        queueEngine.replace(listOf(selected), 0, keepPlaybackModes = true, radioEnabled = false)
        queueIndex = 0
        _state.update { current -> if (current.isVideoMode) current else current.copy(isVideoMode = true) }

        if (alreadyActive) {
            if (!snapshot.isPlaying) togglePlay()
            return
        }
        startResolve(selected)
    }
''',
    "add video-first Samples playback",
)
view_model_path.write_text(view_model)


app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text()
app = replace_once(
    app,
    '''    var addToPlaylistTarget by remember { mutableStateOf<Track?>(null) }

    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
''',
    '''    var addToPlaylistTarget by remember { mutableStateOf<Track?>(null) }
    var samplesStartIndex by rememberSaveable { mutableStateOf<Int?>(null) }

    val zones = remember(strings) { ExploreCatalog.getZones(strings) }
''',
    "add Samples screen state",
)
app = replace_once(
    app,
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        shortcut.zoneId
            ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
            ?.takeIf { zone -> zone.id != state.exploreZoneId }
            ?.let(viewModel::selectExploreZone)
        val target = exploreAnchorIndex(rows, shortcut.anchor)
        if (target >= 0) {
            scrollScope.launch {
                if (animationsEnabled) listState.animateScrollToItem(target) else listState.scrollToItem(target)
            }
        }
    }
''',
    '''    val onShortcut: (ExploreShortcut) -> Unit = { shortcut ->
        if (shortcut == ExploreShortcut.Samples) {
            samplesStartIndex = 0
        } else {
            shortcut.zoneId
                ?.let { zoneId -> zones.firstOrNull { zone -> zone.id == zoneId } }
                ?.takeIf { zone -> zone.id != state.exploreZoneId }
                ?.let(viewModel::selectExploreZone)
            val target = exploreAnchorIndex(rows, shortcut.anchor)
            if (target >= 0) {
                scrollScope.launch {
                    if (animationsEnabled) listState.animateScrollToItem(target) else listState.scrollToItem(target)
                }
            }
        }
    }
''',
    "open Samples shortcut",
)
app = replace_once(
    app,
    '''    val onPlaySamples: (() -> Unit)? = samples.firstOrNull()?.let { first ->
        { viewModel.playFrom(samples, first) }
    }
''',
    '''    val onPlaySamples: (() -> Unit)? = if (samples.isNotEmpty()) {
        { samplesStartIndex = 0 }
    } else {
        null
    }
''',
    "open Samples header action",
)
app = replace_once(
    app,
    '''                    ExploreRow.Samples -> ExploreSamplesRow(
                        samples = samples,
                        currentTrackId = state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        onPlay = { track -> viewModel.playFrom(samples, track) }
                    )
''',
    '''                    ExploreRow.Samples -> ExploreSamplesRow(
                        samples = samples.take(ExploreSampleLimit),
                        currentTrackId = state.currentTrack?.id,
                        isPlaying = state.isPlaying,
                        onOpen = { track ->
                            samplesStartIndex = samples.indexOfFirst { candidate -> candidate.id == track.id }
                                .coerceAtLeast(0)
                        }
                    )
''',
    "make Samples cards open dedicated screen",
)
app = replace_once(
    app,
    '''        addToPlaylistTarget?.let { target ->
            AddToPlaylistDialog(
                track = target,
                playlists = state.playlists,
                onDismiss = { addToPlaylistTarget = null },
                onAddTo = { playlistId ->
                    viewModel.addToPlaylist(playlistId, target)
                    addToPlaylistTarget = null
                },
                onCreateWith = { name ->
                    viewModel.createPlaylist(name, target)
                    addToPlaylistTarget = null
                }
            )
        }
    }
}
''',
    '''        addToPlaylistTarget?.let { target ->
            AddToPlaylistDialog(
                track = target,
                playlists = state.playlists,
                onDismiss = { addToPlaylistTarget = null },
                onAddTo = { playlistId ->
                    viewModel.addToPlaylist(playlistId, target)
                    addToPlaylistTarget = null
                },
                onCreateWith = { name ->
                    viewModel.createPlaylist(name, target)
                    addToPlaylistTarget = null
                }
            )
        }

        samplesStartIndex
            ?.takeIf { samples.isNotEmpty() }
            ?.let { initialPage ->
                ExploreSamplesScreen(
                    samples = samples,
                    initialPage = initialPage,
                    currentTrack = state.currentTrack,
                    isPlaying = state.isPlaying,
                    isResolving = state.isResolving,
                    isVideoMode = state.isVideoMode,
                    favoriteIds = state.favoriteIds,
                    strings = strings,
                    onPlaySample = viewModel::playSample,
                    onTogglePlay = viewModel::togglePlay,
                    onToggleFavorite = viewModel::toggleFavorite,
                    onDismiss = { samplesStartIndex = null }
                )
            }
    }
}
''',
    "render dedicated Samples screen",
)

old_samples_row = '''@Composable
private fun ExploreSamplesRow(
    samples: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onPlay: (Track) -> Unit
) {
    val rowState = rememberLazyListState()
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        flingBehavior = rememberSnapFlingBehavior(rowState)
    ) {
        items(samples, key = { track -> "ex-sample-${track.id}" }) { track ->
            val isCurrent = track.id == currentTrackId
            ExploreSampleCard(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isPlaying && isCurrent,
                onClick = { onPlay(track) }
            )
        }
    }
}
'''
new_samples_row = '''@Composable
private fun ExploreSamplesRow(
    samples: List<Track>,
    currentTrackId: String?,
    isPlaying: Boolean,
    onOpen: (Track) -> Unit
) {
    val rowState = rememberLazyListState()
    LazyRow(
        state = rowState,
        contentPadding = PaddingValues(horizontal = 24.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        flingBehavior = rememberSnapFlingBehavior(rowState)
    ) {
        items(samples, key = { track -> "ex-sample-${track.id}" }) { track ->
            val isCurrent = track.id == currentTrackId
            ExploreSampleCard(
                track = track,
                isCurrent = isCurrent,
                isPlaying = isPlaying && isCurrent,
                onClick = { onOpen(track) }
            )
        }
    }
}
'''
app = replace_once(app, old_samples_row, new_samples_row, "rename Samples preview action")

old_mood = '''@Composable
private fun RowScope.ExploreMoodCard(zone: ExploreZone, isSelected: Boolean, onClick: () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val accentStart = Color(zone.accentStart)
    val accentEnd = Color(zone.accentEnd)
    val shape = RoundedCornerShape(10.dp)
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            accentStart.copy(alpha = if (LevyraIsLight) 0.14f else 0.18f)
        } else {
            LevyraAdaptiveCardDeep
        },
        animationSpec = if (animationsEnabled) tween(220) else snap(),
        label = "explore-mood-background"
    )
    val edgeBrush = remember(accentStart, accentEnd) {
        Brush.verticalGradient(listOf(accentStart, accentEnd))
    }
    Row(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 58.dp)
            .clip(shape)
            .background(background)
            .border(
                width = if (isSelected) 1.dp else Dp.Hairline,
                color = if (isSelected) accentStart.copy(alpha = 0.72f) else LevyraAdaptiveHairline,
                shape = shape
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
            }
            .pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(6.dp)
                .background(edgeBrush)
        )
        Text(
            text = zone.label,
            modifier = Modifier.padding(horizontal = 12.dp),
            color = LevyraText,
            fontSize = 13.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
    }
}
'''
new_mood = '''@Composable
private fun RowScope.ExploreMoodCard(zone: ExploreZone, isSelected: Boolean, onClick: () -> Unit) {
    val animationsEnabled = LocalAnimationsEnabled.current
    val accentStart = Color(zone.accentStart)
    val accentEnd = Color(zone.accentEnd)
    val shape = RoundedCornerShape(8.dp)
    val background by animateColorAsState(
        targetValue = if (isSelected) {
            accentStart.copy(alpha = if (LevyraIsLight) 0.12f else 0.18f)
        } else {
            if (LevyraIsLight) LevyraAdaptiveCardDeep else Color(0xFF18181D)
        },
        animationSpec = if (animationsEnabled) tween(180) else snap(),
        label = "explore-mood-background"
    )
    val edgeBrush = remember(accentStart, accentEnd) {
        Brush.verticalGradient(listOf(accentStart, accentEnd))
    }
    Row(
        modifier = Modifier
            .weight(1f)
            .heightIn(min = 50.dp)
            .clip(shape)
            .background(background)
            .border(
                width = if (isSelected) 1.dp else Dp.Hairline,
                color = if (isSelected) accentStart.copy(alpha = 0.56f) else LevyraAdaptiveHairline,
                shape = shape
            )
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isSelected
            }
            .pressable(onClick = onClick),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .fillMaxHeight()
                .width(7.dp)
                .background(edgeBrush)
        )
        Text(
            text = zone.label,
            modifier = Modifier
                .weight(1f)
                .padding(horizontal = 12.dp),
            color = LevyraText,
            fontSize = 14.sp,
            lineHeight = 18.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
'''
app = replace_once(app, old_mood, new_mood, "restyle mood chips")
app_path.write_text(app)
