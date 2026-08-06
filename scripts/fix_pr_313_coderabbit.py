from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected one match, found {count}")
    return text.replace(old, new, 1)


def replace_between(text: str, start: str, end: str, replacement: str, label: str) -> str:
    start_index = text.find(start)
    if start_index < 0:
        raise RuntimeError(f"{label}: start marker not found")
    end_index = text.find(end, start_index)
    if end_index < 0:
        raise RuntimeError(f"{label}: end marker not found")
    return text[:start_index] + replacement + text[end_index:]


# Keep the immersive feed larger than the preview, but still explicitly bounded.
layout_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreLayout.kt")
layout = layout_path.read_text()
layout = replace_once(
    layout,
    "internal const val ExploreSampleLimit = 10\n",
    "internal const val ExploreSampleLimit = 10\ninternal const val ExploreImmersiveSampleLimit = 24\n",
    "add immersive Samples bound",
)
layout = replace_once(
    layout,
    "internal fun exploreSampleTracks(videos: List<Track>, limit: Int = Int.MAX_VALUE): List<Track> {",
    "internal fun exploreSampleTracks(videos: List<Track>, limit: Int = ExploreSampleLimit): List<Track> {",
    "restore bounded default",
)
layout_path.write_text(layout)


test_path = Path("app/src/test/java/com/luc4n3x/levyra/ui/ExploreLayoutTest.kt")
tests = test_path.read_text()
tests = replace_once(
    tests,
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
    '''    @Test
    fun samplesDefaultToPreviewBoundAndKeepTheImmersiveFeedBounded() {
        val videos = List(ExploreImmersiveSampleLimit + 4) { index -> track("id-$index") }

        assertEquals(ExploreSampleLimit, exploreSampleTracks(videos).size)
        assertEquals(
            ExploreImmersiveSampleLimit,
            exploreSampleTracks(videos, limit = ExploreImmersiveSampleLimit).size
        )
        assertEquals(3, exploreSampleTracks(videos, limit = 3).size)
        assertTrue(exploreSampleTracks(videos, limit = 0).isEmpty())
        assertTrue(exploreSampleTracks(videos, limit = -1).isEmpty())
    }
''',
    "update Samples bounds test",
)
test_path.write_text(tests)


# Allow an exact runtime queue snapshot to be restored after a scoped playback surface exits.
queue_path = Path("app/src/main/java/com/luc4n3x/levyra/player/queue/PersistentQueueEngine.kt")
queue = queue_path.read_text()
queue_marker = '''    fun select(index: Int, positionMs: Long = 0L, rememberCurrent: Boolean = true): Track? {
'''
queue_restore = '''    fun restoreSnapshot(snapshot: PlaybackQueueSnapshot): PlaybackQueueSnapshot =
        mutate(structural = true, immediatePersist = true) { current ->
            undoRemoval = null
            val tracks = snapshot.tracks.map { it.queueStoredCopy() }
            val currentIndex = if (tracks.isEmpty()) -1 else snapshot.currentIndex.coerceIn(0, tracks.lastIndex)
            val validShuffleOrder = snapshot.shuffleOrder
                .filter { it in tracks.indices }
                .distinct()
            val shuffleOrder = when {
                !snapshot.shuffleEnabled -> emptyList()
                validShuffleOrder.size == tracks.size -> validShuffleOrder
                else -> stableShuffleOrder(tracks, currentIndex, current.generation + 1L)
            }
            PlaybackQueueSnapshot(
                tracks = tracks,
                currentIndex = currentIndex,
                positionMs = snapshot.positionMs.coerceAtLeast(0L),
                shuffleEnabled = snapshot.shuffleEnabled,
                shuffleOrder = shuffleOrder,
                shuffleCursor = if (snapshot.shuffleEnabled) {
                    shuffleOrder.indexOf(currentIndex).coerceAtLeast(0)
                } else {
                    -1
                },
                history = snapshot.history.filter { it in tracks.indices }.takeLast(200),
                repeatMode = snapshot.repeatMode,
                radioEnabled = snapshot.radioEnabled,
                generation = current.generation + 1L,
                updatedAt = System.currentTimeMillis(),
                undoAvailable = false
            )
        }

'''
queue = replace_once(queue, queue_marker, queue_restore + queue_marker, "add queue snapshot restore")
queue_path.write_text(queue)


# Expose active player ownership as observable state so Compose can attach after service creation.
service_path = Path("app/src/main/java/com/luc4n3x/levyra/player/PlaybackService.kt")
service = service_path.read_text()
service = replace_once(
    service,
    "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\n",
    "import kotlinx.coroutines.launch\nimport kotlinx.coroutines.withContext\nimport kotlinx.coroutines.flow.MutableStateFlow\nimport kotlinx.coroutines.flow.StateFlow\nimport kotlinx.coroutines.flow.asStateFlow\n",
    "add player flow imports",
)
service = replace_once(
    service,
    '''        @Volatile
        var activePlayer: ExoPlayer? = null
            private set
''',
    '''        private val _activePlayerFlow = MutableStateFlow<ExoPlayer?>(null)
        val activePlayerFlow: StateFlow<ExoPlayer?> = _activePlayerFlow.asStateFlow()

        @Volatile
        var activePlayer: ExoPlayer? = null
            private set(value) {
                field = value
                _activePlayerFlow.value = value
            }
''',
    "publish active player flow",
)
service_path.write_text(service)


# Preserve the user's playback session while Samples temporarily owns playback.
view_model_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
view_model = view_model_path.read_text()
view_model = replace_once(
    view_model,
    "import com.luc4n3x.levyra.player.queue.PersistentQueueEngine\n",
    "import com.luc4n3x.levyra.player.queue.PersistentQueueEngine\nimport com.luc4n3x.levyra.player.queue.PlaybackQueueSnapshot\n",
    "import queue snapshot",
)
view_model = replace_once(
    view_model,
    '''private data class HomeArtistCandidate(
    val name: String,
    val browseId: String
)

''',
    '''private data class HomeArtistCandidate(
    val name: String,
    val browseId: String
)

private data class SamplesPlaybackSession(
    val queue: PlaybackQueueSnapshot,
    val currentTrack: Track?,
    val videoMode: Boolean,
    val loopOnCompletion: Boolean,
    val wasPlaying: Boolean,
    val positionMs: Long
)

''',
    "add Samples session snapshot",
)
view_model = replace_once(
    view_model,
    '''    private var queueIndex: Int = -1
    private var loopCurrentQueueOnCompletion: Boolean = false
''',
    '''    private var queueIndex: Int = -1
    private var loopCurrentQueueOnCompletion: Boolean = false
    private var samplesPlaybackSession: SamplesPlaybackSession? = null
    private var pauseAfterNextPlaybackStart: Boolean = false
''',
    "add Samples session state",
)
new_sample_methods = '''    fun playSample(list: List<Track>, track: Track) {
        if (list.isEmpty()) return
        val currentState = _state.value
        if (samplesPlaybackSession == null) {
            samplesPlaybackSession = SamplesPlaybackSession(
                queue = queueEngine.state.value,
                currentTrack = currentState.currentTrack,
                videoMode = currentState.isVideoMode,
                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = currentState.positionMs
            )
        }

        val selected = list.firstOrNull { candidate -> samePlayableTrack(candidate, track) } ?: track
        val alreadyActive = currentState.currentTrack?.let { current -> samePlayableTrack(current, selected) } == true &&
            currentState.isVideoMode

        loopCurrentQueueOnCompletion = false
        queueEngine.replace(listOf(selected), 0, keepPlaybackModes = true, radioEnabled = false)
        queueIndex = 0
        _state.update { current -> if (current.isVideoMode) current else current.copy(isVideoMode = true) }

        if (alreadyActive) {
            if (!currentState.isPlaying) togglePlay()
            return
        }
        startResolve(selected)
    }

    fun endSamplesPlayback() {
        val session = samplesPlaybackSession ?: return
        samplesPlaybackSession = null
        playRequestId++
        streamTransitionId++
        playJob?.cancel()
        cancelResolutionSideJobs()
        cancelBackgroundWarmups(cancelList = true)
        crossfadeJob?.cancel()
        crossfadeInProgress = false
        player.setVolume(1f)
        loopCurrentQueueOnCompletion = session.loopOnCompletion

        val restoredQueue = queueEngine.restoreSnapshot(session.queue)
        queueIndex = restoredQueue.currentIndex
        val restoredTrack = session.currentTrack
        if (restoredTrack == null) {
            player.stop()
            _state.update {
                it.copy(
                    isVideoMode = session.videoMode,
                    currentTrack = null,
                    isPlaying = false,
                    isResolving = false,
                    positionMs = 0L,
                    bufferedPositionMs = 0L,
                    durationMs = 0L,
                    playerError = null
                )
            }
            updateWidget()
            return
        }

        val durationMs = effectiveDuration(restoredTrack)
        val resumeMs = session.positionMs.coerceAtLeast(0L).let { position ->
            if (durationMs > 0L) position.coerceAtMost(durationMs) else position
        }
        _state.update { it.copy(isVideoMode = session.videoMode, playerError = null) }

        if (restoredTrack.streamUrl.isBlank()) {
            pendingSeekMs = resumeMs
            pauseAfterNextPlaybackStart = !session.wasPlaying
            startResolve(restoredTrack)
            return
        }

        repository.replace(restoredTrack)
        player.play(restoredTrack, session.videoMode)
        if (resumeMs > 0L) player.seekTo(resumeMs)
        if (!session.wasPlaying) player.pause()
        queueEngine.updatePosition(resumeMs)
        _state.update {
            it.copy(
                currentTrack = restoredTrack,
                isVideoMode = session.videoMode,
                isPlaying = session.wasPlaying,
                isResolving = false,
                positionMs = resumeMs,
                bufferedPositionMs = resumeMs,
                durationMs = durationMs,
                playerError = null
            )
        }
        refreshQueuePrefetch()
        updateWidget()
    }

'''
view_model = replace_between(
    view_model,
    "    fun playSample(list: List<Track>, track: Track) {\n",
    "    private fun startResolve(track: Track, preserveCrossfade: Boolean = false, autoRetryWhenOffline: Boolean = false) {\n",
    new_sample_methods,
    "replace Samples playback flow",
)
view_model = replace_once(
    view_model,
    '''    private fun startPlayback(playable: Track) {
        val selectedIndex = queueEngine.state.value.currentIndex
        if (selectedIndex >= 0) queueEngine.updateTrackAt(selectedIndex, playable)
        repository.replace(playable)
        player.play(playable, _state.value.isVideoMode)
        // Resume from the saved position when continuing the last session's track.
        val resumeMs = pendingSeekMs.takeIf { it > 1500L && it < playable.durationMs } ?: 0L
        if (resumeMs > 0L) player.seekTo(resumeMs)
        queueEngine.updatePosition(resumeMs)
        pendingSeekMs = 0L
        _state.update {
            it.copy(
                currentTrack = playable,
                tracks = mergeTracks(it.tracks, listOf(playable)),
                searchResults = mergeTracks(it.searchResults, listOf(playable)),
                activeLyric = lyricsEngine.currentLine(resumeMs, it.lyrics),
                isPlaying = true,
                isResolving = false,
                durationMs = effectiveDuration(playable),
                positionMs = resumeMs,
                cacheReport = repository.cacheReport(),
                playerError = null
            )
        }
''',
    '''    private fun startPlayback(playable: Track) {
        val selectedIndex = queueEngine.state.value.currentIndex
        if (selectedIndex >= 0) queueEngine.updateTrackAt(selectedIndex, playable)
        repository.replace(playable)
        val startPaused = pauseAfterNextPlaybackStart
        pauseAfterNextPlaybackStart = false
        player.play(playable, _state.value.isVideoMode)
        // Resume from the saved position when continuing the last session's track.
        val resumeMs = pendingSeekMs.takeIf { it > 1500L && it < playable.durationMs } ?: 0L
        if (resumeMs > 0L) player.seekTo(resumeMs)
        if (startPaused) player.pause()
        queueEngine.updatePosition(resumeMs)
        pendingSeekMs = 0L
        _state.update {
            it.copy(
                currentTrack = playable,
                tracks = mergeTracks(it.tracks, listOf(playable)),
                searchResults = mergeTracks(it.searchResults, listOf(playable)),
                activeLyric = lyricsEngine.currentLine(resumeMs, it.lyrics),
                isPlaying = !startPaused,
                isResolving = false,
                durationMs = effectiveDuration(playable),
                positionMs = resumeMs,
                cacheReport = repository.cacheReport(),
                playerError = null
            )
        }
''',
    "honor paused restoration",
)
view_model_path.write_text(view_model)


screen_models_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
screen_models = screen_models_path.read_text()
screen_models = replace_once(
    screen_models,
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
''',
    '''    fun ensureExplore(strings: LevyraStrings) = root.ensureExplore(strings)
    fun endSamplesPlayback() = root.endSamplesPlayback()
    fun playFrom(list: List<Track>, track: Track, loopOnCompletion: Boolean = false) = root.playFrom(list, track, loopOnCompletion)
''',
    "expose Samples session cleanup",
)
screen_models_path.write_text(screen_models)


# Integrate the bounded feed, scoped cleanup, and lower-complexity preview card.
app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text()
app = replace_once(
    app,
    "    val samples = remember(state.exploreVideos) { exploreSampleTracks(state.exploreVideos) }\n",
    "    val samples = remember(state.exploreVideos) {\n        exploreSampleTracks(state.exploreVideos, ExploreImmersiveSampleLimit)\n    }\n",
    "bound immersive Samples feed",
)
app = replace_once(
    app,
    '''                    onToggleFavorite = viewModel::toggleFavorite,
                    onDismiss = { samplesStartIndex = null }
''',
    '''                    onToggleFavorite = viewModel::toggleFavorite,
                    onDismiss = {
                        viewModel.endSamplesPlayback()
                        samplesStartIndex = null
                    }
''',
    "restore playback when Samples closes",
)
new_card = '''@Composable
private fun ExploreSampleCard(
    track: Track,
    isCurrent: Boolean,
    isPlaying: Boolean,
    onClick: () -> Unit
) {
    val strings = LocalLevyraStrings.current
    val accentStart = Color(track.accentStart)
    val accentEnd = Color(track.accentEnd)
    val shape = RoundedCornerShape(18.dp)
    val scrim = remember {
        Brush.verticalGradient(
            listOf(
                Color.Black.copy(alpha = 0.34f),
                Color.Transparent,
                Color.Black.copy(alpha = 0.52f),
                Color.Black.copy(alpha = 0.86f)
            )
        )
    }
    Box(
        modifier = Modifier
            .exploreSampleCardStyle(isCurrent, accentStart, accentEnd, shape)
            .semantics(mergeDescendants = true) {
                role = Role.Button
                selected = isCurrent
            }
            .pressable(onClick = onClick)
    ) {
        CoverImage(track = track, modifier = Modifier.fillMaxSize(), highRes = true)
        Box(modifier = Modifier.matchParentSize().background(scrim))
        ExploreSampleBadge(
            isCurrent = isCurrent,
            isPlaying = isPlaying,
            label = strings.exploreSamples,
            playingLabel = strings.playing,
            modifier = Modifier.align(Alignment.TopStart)
        )
        ExploreSamplePlaybackIndicator(
            isPlaying = isPlaying,
            modifier = Modifier.align(Alignment.Center)
        )
        ExploreSampleMetadata(
            track = track,
            modifier = Modifier.align(Alignment.BottomStart)
        )
    }
}

private fun Modifier.exploreSampleCardStyle(
    isCurrent: Boolean,
    accentStart: Color,
    accentEnd: Color,
    shape: RoundedCornerShape
): Modifier = this
    .width(156.dp)
    .aspectRatio(9f / 16f)
    .shadow(
        elevation = if (isCurrent) 22.dp else 14.dp,
        shape = shape,
        clip = false,
        ambientColor = accentStart.copy(alpha = if (isCurrent) 0.28f else 0.12f),
        spotColor = accentEnd.copy(alpha = if (isCurrent) 0.30f else 0.14f)
    )
    .clip(shape)
    .border(
        width = if (isCurrent) 1.25.dp else Dp.Hairline,
        color = if (isCurrent) accentStart.copy(alpha = 0.58f) else Color.White.copy(alpha = 0.12f),
        shape = shape
    )

@Composable
private fun ExploreSampleBadge(
    isCurrent: Boolean,
    isPlaying: Boolean,
    label: String,
    playingLabel: String,
    modifier: Modifier = Modifier
) {
    Surface(
        color = Color.Black.copy(alpha = 0.34f),
        border = BorderStroke(Dp.Hairline, Color.White.copy(alpha = 0.12f)),
        shape = CircleShape,
        modifier = modifier.padding(10.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 9.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            if (isCurrent) {
                ActiveTrackEqualizer(
                    modifier = if (isPlaying) {
                        Modifier.semantics { contentDescription = playingLabel }
                    } else {
                        Modifier
                    },
                    color = Color.White,
                    isPlaying = isPlaying,
                    width = 13.dp,
                    height = 10.dp
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.SlowMotionVideo,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.90f),
                    modifier = Modifier.size(12.dp)
                )
            }
            Text(
                text = label.uppercase(Locale.ROOT),
                color = Color.White.copy(alpha = 0.90f),
                fontSize = 9.5.sp,
                lineHeight = 11.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 0.9.sp,
                maxLines = 1
            )
        }
    }
}

@Composable
private fun ExploreSamplePlaybackIndicator(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .size(46.dp)
            .background(Color.Black.copy(alpha = 0.30f), CircleShape)
            .border(1.dp, Color.White.copy(alpha = 0.18f), CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = if (isPlaying) Icons.Rounded.Pause else Icons.Rounded.PlayArrow,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(23.dp)
        )
    }
}

@Composable
private fun ExploreSampleMetadata(
    track: Track,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier.padding(horizontal = 12.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text = track.title,
            color = Color.White,
            fontSize = 13.5.sp,
            lineHeight = 16.sp,
            fontWeight = FontWeight.Bold,
            maxLines = 2,
            overflow = TextOverflow.Ellipsis
        )
        Text(
            text = track.artist,
            color = Color.White.copy(alpha = 0.74f),
            fontSize = 11.5.sp,
            lineHeight = 14.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

'''
app = replace_between(
    app,
    "@Composable\nprivate fun ExploreSampleCard(\n",
    "@Composable\nprivate fun TrackGlassCard(\n",
    new_card,
    "split ExploreSampleCard",
)
app_path.write_text(app)


# Mount the surface before the service player exists and keep cleanup lifecycle-safe.
samples_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreSamplesScreen.kt")
samples = samples_path.read_text()
samples = replace_once(
    samples,
    "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.snapshotFlow\n",
    "import androidx.compose.runtime.Composable\nimport androidx.compose.runtime.DisposableEffect\nimport androidx.compose.runtime.LaunchedEffect\nimport androidx.compose.runtime.rememberUpdatedState\nimport androidx.compose.runtime.snapshotFlow\n",
    "add Samples lifecycle imports",
)
samples = replace_once(
    samples,
    "import androidx.compose.ui.viewinterop.AndroidView\n",
    "import androidx.compose.ui.viewinterop.AndroidView\nimport androidx.lifecycle.compose.collectAsStateWithLifecycle\n",
    "add active player state collection",
)
samples = replace_once(
    samples,
    "import com.luc4n3x.levyra.ui.theme.LevyraCyan\nimport com.luc4n3x.levyra.ui.theme.LevyraText\n",
    "import com.luc4n3x.levyra.ui.theme.LevyraCyan\n",
    "remove theme-dependent white icon color",
)
samples = replace_once(
    samples,
    '''    if (samples.isEmpty()) return
    BackHandler(onBack = onDismiss)

    val safeInitialPage = initialPage.coerceIn(0, samples.lastIndex)
''',
    '''    if (samples.isEmpty()) return
    BackHandler(onBack = onDismiss)
    val latestOnDismiss = rememberUpdatedState(onDismiss)
    DisposableEffect(Unit) {
        onDispose { latestOnDismiss.value() }
    }

    val safeInitialPage = initialPage.coerceIn(0, samples.lastIndex)
''',
    "restore Samples session on disposal",
)
samples = replace_once(
    samples,
    '''private fun ExploreSampleVideoSurface(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val player = PlaybackService.activePlayer ?: return

    AndroidView(
''',
    '''private fun ExploreSampleVideoSurface(
    isPlaying: Boolean,
    modifier: Modifier = Modifier
) {
    val player = PlaybackService.activePlayerFlow.collectAsStateWithLifecycle().value

    AndroidView(
''',
    "observe active player before attachment",
)
samples = replace_once(
    samples,
    "                tint = if (selected) LevyraCyan else LevyraText,\n",
    "                tint = if (selected) LevyraCyan else Color.White,\n",
    "fix action icon contrast",
)
samples_path.write_text(samples)
