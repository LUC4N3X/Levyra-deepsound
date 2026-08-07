from pathlib import Path


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise RuntimeError(f"{label}: expected exactly 1 match, found {count}")
    return text.replace(old, new, 1)


def function_bounds(text: str, marker: str) -> tuple[int, int]:
    start = text.find(marker)
    if start < 0:
        raise RuntimeError(f"function marker missing: {marker}")
    brace = text.find("{", start)
    if brace < 0:
        raise RuntimeError(f"function opening brace missing: {marker}")
    depth = 0
    in_string = False
    escaped = False
    for index in range(brace, len(text)):
        ch = text[index]
        if in_string:
            if escaped:
                escaped = False
            elif ch == "\\":
                escaped = True
            elif ch == '"':
                in_string = False
            continue
        if ch == '"':
            in_string = True
            continue
        if ch == "{":
            depth += 1
        elif ch == "}":
            depth -= 1
            if depth == 0:
                return start, index + 1
    raise RuntimeError(f"function closing brace missing: {marker}")


def replace_function(text: str, marker: str, replacement: str) -> str:
    start, end = function_bounds(text, marker)
    return text[:start] + replacement.rstrip() + text[end:]


def insert_after_function(text: str, marker: str, insertion: str) -> str:
    _, end = function_bounds(text, marker)
    return text[:end] + "\n\n" + insertion.rstrip() + text[end:]


def insert_before_function_close(text: str, marker: str, code: str) -> str:
    start, end = function_bounds(text, marker)
    function = text[start:end]
    closing = function.rfind("}")
    if closing < 0:
        raise RuntimeError(f"cannot insert before close: {marker}")
    patched = function[:closing].rstrip() + "\n" + code.rstrip() + "\n    }"
    return text[:start] + patched + text[end:]


# ---------------------------------------------------------------------------
# 1) Persistent queue: Samples must never overwrite the durable queue.
# ---------------------------------------------------------------------------
queue_path = Path("app/src/main/java/com/luc4n3x/levyra/player/queue/PersistentQueueEngine.kt")
queue = queue_path.read_text()

if "internal fun queuePersistenceAllowed(" not in queue:
    queue = replace_once(
        queue,
        "import timber.log.Timber\n\nclass PersistentQueueEngine private constructor(context: Context) {",
        "import timber.log.Timber\n\n"
        "internal fun queuePersistenceAllowed(transientPlaybackActive: Boolean): Boolean =\n"
        "    !transientPlaybackActive\n\n"
        "class PersistentQueueEngine private constructor(context: Context) {",
        "queue persistence policy helper",
    )

queue = replace_once(
    queue,
    "    private var undoRemoval: QueueRemoval? = null\n",
    "    private var undoRemoval: QueueRemoval? = null\n"
    "    @Volatile private var transientPlaybackActive: Boolean = false\n",
    "queue transient state",
)

restore_marker = "    suspend fun restore(\n"
restore_start, restore_end = function_bounds(queue, restore_marker)
restore_fn = queue[restore_start:restore_end]
if "transientPlaybackActive = false" not in restore_fn:
    open_brace = restore_fn.find("{")
    restore_fn = restore_fn[: open_brace + 1] + "\n        transientPlaybackActive = false" + restore_fn[open_brace + 1 :]
    queue = queue[:restore_start] + restore_fn + queue[restore_end:]

if "fun beginTransientPlayback(" not in queue:
    transient_methods = '''    fun beginTransientPlayback(
        preservedSnapshot: PlaybackQueueSnapshot,
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long = 0L
    ): PlaybackQueueSnapshot {
        persistJob?.cancel()
        positionPersistJob?.cancel()
        val durableSnapshot = preservedSnapshot.toPersistent()
        transientPlaybackActive = true
        scope.launch {
            runCatching { store.save(durableSnapshot) }
                .onFailure { Timber.w(it, "Unable to preserve durable queue before transient playback") }
        }
        return replaceTransient(tracks, currentIndex, positionMs)
    }

    fun replaceTransient(
        tracks: List<Track>,
        currentIndex: Int,
        positionMs: Long = 0L
    ): PlaybackQueueSnapshot {
        check(transientPlaybackActive) { "Transient playback must be started before replacement" }
        return mutate(structural = true, immediatePersist = false) { current ->
            val normalized = tracks.filter { it.title.isNotBlank() }.distinctBy(::playbackQueueIdentity)
            val safeIndex = if (normalized.isEmpty()) -1 else currentIndex.coerceIn(0, normalized.lastIndex)
            buildSnapshot(
                tracks = normalized,
                currentIndex = safeIndex,
                positionMs = positionMs,
                repeatMode = RepeatMode.Off,
                shuffleEnabled = false,
                radioEnabled = false,
                generation = current.generation + 1L
            )
        }
    }

    fun endTransientPlayback(snapshot: PlaybackQueueSnapshot): PlaybackQueueSnapshot {
        transientPlaybackActive = false
        return restoreSnapshot(snapshot)
    }'''
    queue = insert_after_function(queue, "    fun replace(\n", transient_methods)

flush_start, flush_end = function_bounds(queue, "    suspend fun flush()")
flush_fn = queue[flush_start:flush_end]
if "queuePersistenceAllowed(transientPlaybackActive)" not in flush_fn:
    flush_fn = replace_once(
        flush_fn,
        "    suspend fun flush() {\n",
        "    suspend fun flush() {\n"
        "        if (!queuePersistenceAllowed(transientPlaybackActive)) return\n",
        "transient flush guard",
    )
    queue = queue[:flush_start] + flush_fn + queue[flush_end:]

schedule_start, schedule_end = function_bounds(queue, "    private fun schedulePersist(")
schedule_fn = queue[schedule_start:schedule_end]
if "queuePersistenceAllowed(transientPlaybackActive)" not in schedule_fn:
    brace = schedule_fn.find("{")
    schedule_fn = schedule_fn[: brace + 1] + "\n        if (!queuePersistenceAllowed(transientPlaybackActive)) return" + schedule_fn[brace + 1 :]
    queue = queue[:schedule_start] + schedule_fn + queue[schedule_end:]

position_start, position_end = function_bounds(queue, "    private fun schedulePositionPersist(")
position_fn = queue[position_start:position_end]
if "queuePersistenceAllowed(transientPlaybackActive)" not in position_fn:
    brace = position_fn.find("{")
    position_fn = position_fn[: brace + 1] + "\n        if (!queuePersistenceAllowed(transientPlaybackActive)) return" + position_fn[brace + 1 :]
    queue = queue[:position_start] + position_fn + queue[position_end:]

queue_path.write_text(queue)


# ---------------------------------------------------------------------------
# 2) Clean Fresh Currents source: editorial release feed, safe chart fallback.
# ---------------------------------------------------------------------------
charts_path = Path("app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt")
charts = charts_path.read_text()

if "internal fun selectFreshCurrentTracks(" not in charts:
    charts = replace_once(
        charts,
        "class ChartsRepository(context: Context) {",
        '''internal fun selectFreshCurrentTracks(
    editorial: List<Track>,
    fallback: List<Track>,
    limit: Int
): List<Track> {
    if (limit <= 0) return emptyList()
    fun eligible(track: Track): Boolean =
        track.id.isNotBlank() &&
            track.title.isNotBlank() &&
            track.artist.isNotBlank() &&
            (track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()) &&
            !isYoutubeShortTrack(track)

    val editorialClean = editorial.filter(::eligible)
    val source = editorialClean.ifEmpty { fallback.filter(::eligible) }
    return source
        .distinctBy { track ->
            track.isrc.ifBlank {
                "${track.artist.trim().lowercase()}|${track.title.trim().lowercase()}"
            }
        }
        .take(limit)
}

class ChartsRepository(context: Context) {''',
        "fresh currents selector",
    )

if "suspend fun freshCurrentTracks(" not in charts:
    fresh_method = '''    suspend fun freshCurrentTracks(country: String, limit: Int = 24): List<Track> = withContext(Dispatchers.IO) {
        val safeLimit = limit.coerceIn(1, 40)
        val editorial = editorialCharts.newReleaseTracks(country, safeLimit)
        if (editorial.isNotEmpty()) {
            return@withContext selectFreshCurrentTracks(editorial, emptyList(), safeLimit)
        }
        val fallback = topSongs(country, safeLimit)
        selectFreshCurrentTracks(emptyList(), fallback, safeLimit)
    }'''
    charts = insert_after_function(charts, "    suspend fun newReleaseAlbums(", fresh_method)

charts_path.write_text(charts)

editorial_path = Path("app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt")
editorial = editorial_path.read_text()
if "suspend fun newReleaseTracks(country: String" not in editorial:
    method = '''    suspend fun newReleaseTracks(country: String, limit: Int): List<Track> = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val snapshot = usableSnapshot(now) ?: refreshAsync().await() ?: return@withContext emptyList()
        if (snapshot.needsRefresh(now)) warm()
        snapshot.newReleases(country, limit)
    }'''
    editorial = insert_after_function(editorial, "    suspend fun cachedNewReleaseTracks(", method)
editorial_path.write_text(editorial)


# ---------------------------------------------------------------------------
# 3) Shorts cache API: no blank-signature startup read can silently succeed.
# ---------------------------------------------------------------------------
shorts_cache_path = Path("app/src/main/java/com/luc4n3x/levyra/data/YoutubeShortsCache.kt")
shorts_cache = shorts_cache_path.read_text()
shorts_cache = replace_once(
    shorts_cache,
    '    fun load(languageCode: String, profileSignature: String = ""): YoutubeShortsCacheSnapshot {',
    '    fun load(languageCode: String, profileSignature: String): YoutubeShortsCacheSnapshot {',
    "shorts cache explicit profile signature",
)
shorts_cache_path.write_text(shorts_cache)


# ---------------------------------------------------------------------------
# 4) Dedicated state for Fresh Currents and Samples.
# ---------------------------------------------------------------------------
state_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraUiState.kt")
state = state_path.read_text()
state = replace_once(
    state,
    "    val exploreTracks: List<Track> = emptyList(),\n"
    "    val exploreNewReleases: List<AlbumHit> = emptyList(),\n"
    "    val exploreVideos: List<Track> = emptyList(),\n",
    "    val exploreTracks: List<Track> = emptyList(),\n"
    "    val exploreFreshTracks: List<Track> = emptyList(),\n"
    "    val exploreNewReleases: List<AlbumHit> = emptyList(),\n"
    "    val exploreVideos: List<Track> = emptyList(),\n"
    "    val exploreSamples: List<Track> = emptyList(),\n",
    "dedicated explore feed fields",
)
state = replace_once(
    state,
    "    val isNewReleasesLoading: Boolean = false,\n",
    "    val isFreshCurrentsLoading: Boolean = false,\n"
    "    val isNewReleasesLoading: Boolean = false,\n",
    "fresh currents loading field",
)
state_path.write_text(state)

screen_vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraScreenViewModels.kt")
screen_vm = screen_vm_path.read_text()
screen_vm = replace_once(
    screen_vm,
    "    val exploreTracks: List<Track>,\n"
    "    val exploreNewReleases: List<AlbumHit>,\n"
    "    val exploreVideos: List<Track>,\n",
    "    val exploreTracks: List<Track>,\n"
    "    val exploreFreshTracks: List<Track>,\n"
    "    val exploreNewReleases: List<AlbumHit>,\n"
    "    val exploreVideos: List<Track>,\n"
    "    val exploreSamples: List<Track>,\n",
    "explore projection feed fields",
)
screen_vm = replace_once(
    screen_vm,
    "    val isExploreLoading: Boolean,\n"
    "    val isNewReleasesLoading: Boolean,\n",
    "    val isExploreLoading: Boolean,\n"
    "    val isFreshCurrentsLoading: Boolean,\n"
    "    val isNewReleasesLoading: Boolean,\n",
    "explore projection fresh loading",
)
screen_vm = replace_once(
    screen_vm,
    "    exploreTracks = state.exploreTracks,\n"
    "    exploreNewReleases = state.exploreNewReleases,\n"
    "    exploreVideos = state.exploreVideos,\n",
    "    exploreTracks = state.exploreTracks,\n"
    "    exploreFreshTracks = state.exploreFreshTracks,\n"
    "    exploreNewReleases = state.exploreNewReleases,\n"
    "    exploreVideos = state.exploreVideos,\n"
    "    exploreSamples = state.exploreSamples,\n",
    "explore projection feed mapping",
)
screen_vm = replace_once(
    screen_vm,
    "    isExploreLoading = state.isExploreLoading,\n"
    "    isNewReleasesLoading = state.isNewReleasesLoading,\n",
    "    isExploreLoading = state.isExploreLoading,\n"
    "    isFreshCurrentsLoading = state.isFreshCurrentsLoading,\n"
    "    isNewReleasesLoading = state.isNewReleasesLoading,\n",
    "explore projection loading mapping",
)
if "fun ensureSamples()" not in screen_vm:
    screen_vm = replace_once(
        screen_vm,
        "    fun refreshSamples() = root.refreshExploreSamples()\n",
        "    fun ensureSamples() = root.ensureExploreSamples()\n"
        "    fun refreshSamples() = root.refreshExploreSamples()\n",
        "ExploreViewModel ensure samples",
    )
screen_vm_path.write_text(screen_vm)


# ---------------------------------------------------------------------------
# 5) ViewModel: isolate feeds, cancel stale requests, remove cold-start Samples,
#    use transient queue, and defer playback side effects for paused restore.
# ---------------------------------------------------------------------------
vm_path = Path("app/src/main/java/com/luc4n3x/levyra/viewmodel/LevyraViewModel.kt")
vm = vm_path.read_text()

if "internal fun LevyraUiState.withPublishedSamples(" not in vm:
    helper_marker = "internal fun shouldStartPlaybackPaused(\n"
    helper_start = vm.index(helper_marker)
    _, helper_end = function_bounds(vm, helper_marker)
    helpers = '''

internal fun LevyraUiState.withPublishedSamples(
    tracks: List<Track>,
    loading: Boolean = false,
    failed: Boolean = false
): LevyraUiState = copy(
    exploreSamples = tracks,
    isSamplesLoading = loading,
    samplesLoadFailed = failed
)

internal fun shouldDispatchPlaybackStartSideEffects(startPaused: Boolean): Boolean = !startPaused
'''
    vm = vm[:helper_end] + helpers + vm[helper_end:]

vm = replace_once(
    vm,
    "    private var samplesPlaybackSession: SamplesPlaybackSession? = null\n",
    "    private var samplesPlaybackSession: SamplesPlaybackSession? = null\n"
    "    private var deferredPlaybackStartSideEffectsKey: String? = null\n",
    "deferred playback side effects state",
)

old_jobs = '''    private var musicVideosLoadedLanguage = ""
    private var musicVideosLoadedProfileSignature = ""
    private var musicVideosRetryLanguage = ""
    private var musicVideosRetryAfterMs = 0L
    private var musicVideosFailureCount = 0
    private var musicVideosJob: Job? = null
    private var newReleasesLoadedLanguage = ""
    private var newReleasesLoadedProfileSignature = ""
    private var newReleasesJob: Job? = null
    private var exploreJob: Job? = null
'''
new_jobs = '''    private var musicVideosLoadedLanguage = ""
    private var musicVideosLoadedProfileSignature = ""
    private var musicVideosRequestLanguage = ""
    private var musicVideosRequestProfileSignature = ""
    private var musicVideosRequestGeneration = 0L
    private var musicVideosRetryLanguage = ""
    private var musicVideosRetryAfterMs = 0L
    private var musicVideosFailureCount = 0
    private var musicVideosJob: Job? = null
    private var freshCurrentsLoadedLanguage = ""
    private var freshCurrentsRequestGeneration = 0L
    private var freshCurrentsJob: Job? = null
    private var newReleasesLoadedLanguage = ""
    private var newReleasesLoadedProfileSignature = ""
    private var newReleasesRequestLanguage = ""
    private var newReleasesRequestProfileSignature = ""
    private var newReleasesRequestGeneration = 0L
    private var newReleasesJob: Job? = null
    private var exploreJob: Job? = null
'''
vm = replace_once(vm, old_jobs, new_jobs, "explore request state")

# Dead startup cache read and eager Samples discovery.
vm = replace_once(
    vm,
    "        val startupShortsSnapshot = shortsCache.load(settings.languageCode)\n"
    "        val startupShorts = startupShortsSnapshot.tracks\n",
    "",
    "remove dead startup Shorts cache read",
)
vm = replace_once(
    vm,
    "                exploreVideos = startupShorts,\n",
    "",
    "remove startup Samples publication into exploreVideos",
)
vm = replace_once(
    vm,
    "        viewModelScope.launch {\n"
    "            delay(if (startupShortsSnapshot.isFresh()) 1_200L else 80L)\n"
    "            ensureMusicVideosLoaded()\n"
    "        }\n",
    "",
    "remove cold-start Samples discovery",
)

# Metadata enrichment must keep the new feeds enriched too.
vm = replace_once(
    vm,
    "            val exploreTracks = current.exploreTracks.map(::withArtwork)\n"
    "            val exploreVideos = current.exploreVideos.map(::withArtwork)\n",
    "            val exploreTracks = current.exploreTracks.map(::withArtwork)\n"
    "            val exploreFreshTracks = current.exploreFreshTracks.map(::withArtwork)\n"
    "            val exploreVideos = current.exploreVideos.map(::withArtwork)\n"
    "            val exploreSamples = current.exploreSamples.map(::withArtwork)\n",
    "enrich dedicated explore feeds",
)
vm = replace_once(
    vm,
    "                exploreTracks = exploreTracks,\n"
    "                exploreVideos = exploreVideos,\n",
    "                exploreTracks = exploreTracks,\n"
    "                exploreFreshTracks = exploreFreshTracks,\n"
    "                exploreVideos = exploreVideos,\n"
    "                exploreSamples = exploreSamples,\n",
    "publish enriched explore feeds",
)

# Samples playback: preserve durable queue, then stay fully transient.
play_sample_replacement = '''    fun playSample(list: List<Track>, track: Track) {
        val selected = selectYoutubeShortSample(list, track) ?: return
        val currentState = _state.value
        val actualPositionMs = player.positionMs.coerceAtLeast(0L).takeIf { it > 0L }
            ?: currentState.positionMs
        val startingTransientSession = samplesPlaybackSession == null
        if (startingTransientSession) {
            samplesPlaybackSession = SamplesPlaybackSession(
                queue = queueEngine.state.value.copy(positionMs = actualPositionMs),
                currentTrack = currentState.currentTrack,
                videoMode = currentState.isVideoMode,
                loopOnCompletion = loopCurrentQueueOnCompletion,
                wasPlaying = currentState.isPlaying,
                positionMs = actualPositionMs
            )
        }

        beginSamplesPlayback()
        val alreadyActive = currentState.currentTrack?.let { current -> samePlayableTrack(current, selected) } == true &&
            currentState.isVideoMode

        loopCurrentQueueOnCompletion = false
        val session = samplesPlaybackSession ?: return
        if (startingTransientSession) {
            queueEngine.beginTransientPlayback(session.queue, listOf(selected), 0)
        } else {
            queueEngine.replaceTransient(listOf(selected), 0)
        }
        queueIndex = 0
        _state.update { current ->
            current.copy(isVideoMode = true, isSamplesOpen = true)
        }

        if (alreadyActive) {
            if (!currentState.isPlaying) togglePlay()
            return
        }
        pendingSeekMs = youtubeMusicSamplePreviewStartMs(selected)
        startResolve(selected)
    }'''
vm = replace_function(vm, "    fun playSample(list: List<Track>, track: Track)", play_sample_replacement)

end_samples_start, end_samples_end = function_bounds(vm, "    fun endSamplesPlayback()")
end_samples_fn = vm[end_samples_start:end_samples_end]
end_samples_fn = replace_once(
    end_samples_fn,
    "        val restoredQueue = queueEngine.restoreSnapshot(session.queue)\n",
    "        val restoredQueue = queueEngine.endTransientPlayback(session.queue)\n",
    "restore durable queue after Samples",
)
vm = vm[:end_samples_start] + end_samples_fn + vm[end_samples_end:]

# Samples loading/request lifecycle.
ensure_samples = '''    private fun ensureMusicVideosLoaded(force: Boolean = false) {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        val profileSignature = samplesDiscoveryProfileSignature(snapshot)
        val sameActiveRequest = musicVideosRequestLanguage == languageCode &&
            musicVideosRequestProfileSignature == profileSignature
        if (musicVideosJob?.isActive == true) {
            if (!force && sameActiveRequest) return
            musicVideosJob?.cancel()
        }
        if (
            !force &&
            musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature &&
            snapshot.exploreSamples.isNotEmpty()
        ) return
        if (musicVideosRetryLanguage != languageCode) {
            musicVideosRetryLanguage = languageCode
            musicVideosRetryAfterMs = 0L
            musicVideosFailureCount = 0
        }
        if (!force && System.currentTimeMillis() < musicVideosRetryAfterMs) return

        val requestGeneration = ++musicVideosRequestGeneration
        musicVideosRequestLanguage = languageCode
        musicVideosRequestProfileSignature = profileSignature
        val keepVisibleSamples = musicVideosLoadedLanguage == languageCode &&
            musicVideosLoadedProfileSignature == profileSignature
        updateSamplesState(languageCode) { current ->
            current.copy(
                exploreSamples = if (keepVisibleSamples) current.exploreSamples else emptyList(),
                isSamplesLoading = true,
                samplesLoadFailed = false
            )
        }
        musicVideosJob = viewModelScope.launch {
            try {
                if (!force && publishCachedSamples(languageCode, profileSignature, requestGeneration)) return@launch
                refreshSamplesFeed(languageCode, profileSignature, requestGeneration)
            } finally {
                if (musicVideosRequestGeneration == requestGeneration) {
                    updateSamplesState(languageCode) { current ->
                        if (!current.isSamplesLoading) current else current.copy(isSamplesLoading = false)
                    }
                    musicVideosJob = null
                }
            }
        }
    }'''
vm = replace_function(vm, "    private fun ensureMusicVideosLoaded()", ensure_samples)

publish_cache = '''    private suspend fun publishCachedSamples(
        languageCode: String,
        profileSignature: String,
        requestGeneration: Long
    ): Boolean {
        if (_state.value.exploreSamples.isNotEmpty()) return false
        val cached = withContext(Dispatchers.IO) { shortsCache.load(languageCode, profileSignature) }
        if (cached.tracks.isEmpty()) return false
        if (musicVideosRequestGeneration != requestGeneration || _state.value.languageCode != languageCode) return false
        updateSamplesState(languageCode) { current ->
            current.withPublishedSamples(cached.tracks, loading = !cached.isFresh(), failed = false)
        }
        if (!cached.isFresh()) return false
        musicVideosLoadedLanguage = languageCode
        musicVideosLoadedProfileSignature = profileSignature
        return true
    }'''
vm = replace_function(vm, "    private suspend fun publishCachedSamples(", publish_cache)

refresh_samples = '''    private suspend fun refreshSamplesFeed(
        languageCode: String,
        profileSignature: String,
        requestGeneration: Long
    ) {
        val resolvedFeedTracks = resolveSamplesFeed(samplesDiscoveryInput(_state.value), languageCode)
        if (musicVideosRequestGeneration != requestGeneration || _state.value.languageCode != languageCode) return
        if (resolvedFeedTracks == null) {
            updateSamplesState(languageCode) { current ->
                current.copy(isSamplesLoading = false, samplesLoadFailed = true)
            }
            registerShortsFeedFailure(languageCode)
            return
        }

        musicVideosLoadedLanguage = languageCode
        musicVideosLoadedProfileSignature = profileSignature
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
        updateSamplesState(languageCode) { current ->
            current.withPublishedSamples(resolvedFeedTracks, loading = false, failed = false)
        }
        withContext(Dispatchers.IO) {
            shortsCache.save(
                languageCode = languageCode,
                tracks = resolvedFeedTracks,
                profileSignature = profileSignature
            )
        }
    }'''
vm = replace_function(vm, "    private suspend fun refreshSamplesFeed(", refresh_samples)

# Fresh Currents loader.
if "private fun ensureFreshCurrentsLoaded(" not in vm:
    fresh_loader = '''    private fun ensureFreshCurrentsLoaded(force: Boolean = false) {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        if (freshCurrentsJob?.isActive == true) {
            if (!force && freshCurrentsLoadedLanguage == languageCode) return
            freshCurrentsJob?.cancel()
        }
        if (!force && freshCurrentsLoadedLanguage == languageCode && snapshot.exploreFreshTracks.isNotEmpty()) return

        val requestGeneration = ++freshCurrentsRequestGeneration
        _state.update { current ->
            if (current.languageCode != languageCode) current
            else current.copy(
                exploreFreshTracks = if (freshCurrentsLoadedLanguage == languageCode) current.exploreFreshTracks else emptyList(),
                isFreshCurrentsLoading = true
            )
        }
        freshCurrentsJob = viewModelScope.launch {
            try {
                val market = ChartsCatalog.defaultRegionForLanguage(languageCode).country
                val tracks = try {
                    chartsRepository.freshCurrentTracks(country = market, limit = 24)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.w(error, "Fresh currents failed for %s", market)
                    emptyList()
                }
                if (freshCurrentsRequestGeneration != requestGeneration || _state.value.languageCode != languageCode) return@launch
                if (tracks.isNotEmpty()) freshCurrentsLoadedLanguage = languageCode
                _state.update { current ->
                    if (current.languageCode != languageCode) current
                    else current.copy(
                        exploreFreshTracks = tracks,
                        exploreTracks = if (current.exploreZoneId == ExploreCatalog.NEW_RELEASES_ZONE_ID) tracks else current.exploreTracks,
                        isFreshCurrentsLoading = false,
                        isExploreLoading = if (current.exploreZoneId == ExploreCatalog.NEW_RELEASES_ZONE_ID) false else current.isExploreLoading
                    )
                }
                if (tracks.isNotEmpty()) refreshOfficialMetadataBatch(tracks, 8)
            } finally {
                if (freshCurrentsRequestGeneration == requestGeneration) {
                    _state.update { current ->
                        if (current.languageCode == languageCode && current.isFreshCurrentsLoading) {
                            current.copy(isFreshCurrentsLoading = false)
                        } else current
                    }
                    freshCurrentsJob = null
                }
            }
        }
    }'''
    official_start = vm.index("    private fun ensureOfficialNewReleasesLoaded(")
    vm = vm[:official_start] + fresh_loader + "\n\n" + vm[official_start:]

# New Releases request lifecycle: old language/profile job cannot own new UI state.
new_releases = '''    private fun ensureOfficialNewReleasesLoaded(force: Boolean = false) {
        val snapshot = _state.value
        val languageCode = snapshot.languageCode
        val preferredArtists = discoveryPreferredArtists(snapshot, 24)
        val profileSignature = buildString {
            append(LevyraLanguageCatalog.normalize(languageCode))
            append('|').append(preferredArtists.joinToString("|"))
        }
        val sameActiveRequest = newReleasesRequestLanguage == languageCode &&
            newReleasesRequestProfileSignature == profileSignature
        if (newReleasesJob?.isActive == true) {
            if (!force && sameActiveRequest) return
            newReleasesJob?.cancel()
        }
        if (
            !force &&
            newReleasesLoadedLanguage == languageCode &&
            newReleasesLoadedProfileSignature == profileSignature &&
            snapshot.exploreNewReleases.isNotEmpty()
        ) return

        val requestGeneration = ++newReleasesRequestGeneration
        newReleasesRequestLanguage = languageCode
        newReleasesRequestProfileSignature = profileSignature
        _state.update { current ->
            if (current.languageCode != languageCode) current
            else current.copy(
                exploreNewReleases = if (
                    newReleasesLoadedLanguage == languageCode &&
                    newReleasesLoadedProfileSignature == profileSignature
                ) current.exploreNewReleases else emptyList(),
                isNewReleasesLoading = true,
                newReleasesLoadFailed = false
            )
        }
        newReleasesJob = viewModelScope.launch {
            try {
                val market = ChartsCatalog.defaultRegionForLanguage(languageCode).country
                val editorialReleases = try {
                    chartsRepository.newReleaseAlbums(country = market, limit = 48)
                } catch (error: CancellationException) {
                    throw error
                } catch (error: Throwable) {
                    Timber.w(error, "Localized editorial releases failed for %s", market)
                    emptyList()
                }
                val releases = prioritizeNewReleasesForUser(
                    releases = editorialReleases,
                    preferredArtists = preferredArtists,
                    limit = 48
                ).ifEmpty {
                    try {
                        repository.newReleases(
                            languageCode = languageCode,
                            limit = 48,
                            preferredArtists = preferredArtists
                        )
                    } catch (error: CancellationException) {
                        throw error
                    } catch (error: Throwable) {
                        Timber.w(error, "Personalized new releases fallback failed for %s", languageCode)
                        emptyList()
                    }
                }
                if (newReleasesRequestGeneration != requestGeneration || _state.value.languageCode != languageCode) return@launch
                if (releases.isNotEmpty()) {
                    newReleasesLoadedLanguage = languageCode
                    newReleasesLoadedProfileSignature = profileSignature
                }
                _state.update { current ->
                    if (current.languageCode != languageCode) current
                    else current.copy(
                        exploreNewReleases = releases,
                        isNewReleasesLoading = false,
                        newReleasesLoadFailed = releases.isEmpty()
                    )
                }
            } finally {
                if (newReleasesRequestGeneration == requestGeneration) {
                    _state.update { current ->
                        if (current.languageCode == languageCode && current.isNewReleasesLoading) {
                            current.copy(isNewReleasesLoading = false)
                        } else current
                    }
                    newReleasesJob = null
                }
            }
        }
    }'''
vm = replace_function(vm, "    private fun ensureOfficialNewReleasesLoaded(", new_releases)

ensure_explore = '''    fun ensureExplore(strings: LevyraStrings) {
        if (_state.value.exploreZoneId == null) {
            selectExploreZone(ExploreCatalog.getZones(strings).first())
        }
        ensureFreshCurrentsLoaded()
        ensureOfficialNewReleasesLoaded()
        ensureMusicVideosLoaded()
    }

    fun ensureExploreSamples() {
        ensureMusicVideosLoaded()
    }'''
vm = replace_function(vm, "    fun ensureExplore(strings: LevyraStrings)", ensure_explore)

refresh_samples_public = '''    fun refreshExploreSamples() {
        musicVideosLoadedLanguage = ""
        musicVideosLoadedProfileSignature = ""
        musicVideosRetryLanguage = ""
        musicVideosRetryAfterMs = 0L
        musicVideosFailureCount = 0
        ensureMusicVideosLoaded(force = true)
    }'''
vm = replace_function(vm, "    fun refreshExploreSamples()", refresh_samples_public)

select_zone = '''    fun selectExploreZone(zone: ExploreZone) {
        if (zone.id == ExploreCatalog.NEW_RELEASES_ZONE_ID) {
            _state.update { current ->
                current.copy(
                    exploreZoneId = zone.id,
                    exploreTracks = current.exploreFreshTracks,
                    isExploreLoading = current.isFreshCurrentsLoading
                )
            }
            ensureFreshCurrentsLoaded()
            return
        }
        _state.update { it.copy(exploreZoneId = zone.id) }
        exploreCache[zone.id]?.let { cached ->
            _state.update { it.copy(exploreTracks = cached, isExploreLoading = false) }
            refreshOfficialMetadataBatch(cached, 8)
            return
        }
        exploreJob?.cancel()
        _state.update { it.copy(exploreTracks = emptyList(), isExploreLoading = true) }
        val languageCode = _state.value.languageCode
        exploreJob = viewModelScope.launch {
            val results = runCatching {
                repository.exploreZone(zone.id, zone.query, languageCode, 24)
            }.getOrDefault(emptyList())
            if (results.isNotEmpty()) exploreCache[zone.id] = results
            if (_state.value.exploreZoneId != zone.id || _state.value.languageCode != languageCode) return@launch
            _state.update { it.copy(exploreTracks = results, isExploreLoading = false) }
            refreshOfficialMetadataBatch(results, 8)
        }
    }'''
vm = replace_function(vm, "    fun selectExploreZone(zone: ExploreZone)", select_zone)

# Paused restore must not fabricate listening/history or SponsorBlock work.
start_playback_start, start_playback_end = function_bounds(vm, "    private fun startPlayback(playable: Track, request: PlaybackResolveRequest)")
start_playback = vm[start_playback_start:start_playback_end]
start_playback = replace_once(
    start_playback,
    "        recordPlaybackHistory(playable)\n"
    "        beginListenSession(playable)\n"
    "        recordSmartPlayback(playable)\n"
    "        fetchSponsorSegments(playable)\n",
    "        handlePlaybackStartSideEffects(playable, startPaused)\n",
    "defer paused playback side effects",
)
vm = vm[:start_playback_start] + start_playback + vm[start_playback_end:]

if "private fun handlePlaybackStartSideEffects(" not in vm:
    side_effect_helpers = '''    private fun handlePlaybackStartSideEffects(track: Track, startPaused: Boolean) {
        val key = playbackIdentity(track)
        if (!shouldDispatchPlaybackStartSideEffects(startPaused)) {
            deferredPlaybackStartSideEffectsKey = key
            return
        }
        deferredPlaybackStartSideEffectsKey = null
        dispatchPlaybackStartSideEffects(track)
    }

    private fun commitDeferredPlaybackStartSideEffectsIfNeeded(track: Track) {
        val key = playbackIdentity(track)
        if (deferredPlaybackStartSideEffectsKey != key) return
        deferredPlaybackStartSideEffectsKey = null
        dispatchPlaybackStartSideEffects(track)
    }

    private fun dispatchPlaybackStartSideEffects(track: Track) {
        recordPlaybackHistory(track)
        beginListenSession(track)
        recordSmartPlayback(track)
        fetchSponsorSegments(track)
    }'''
    vm = insert_after_function(vm, "    private fun startPlayback(playable: Track, request: PlaybackResolveRequest)", side_effect_helpers)

# When a paused restored track is actually resumed, dispatch the deferred work once.
toggle_start, toggle_end = function_bounds(vm, "    fun togglePlay()")
toggle_fn = vm[toggle_start:toggle_end]
if "commitDeferredPlaybackStartSideEffectsIfNeeded" not in toggle_fn:
    toggle_fn = toggle_fn[:-1].rstrip() + (
        "\n        if (_state.value.isPlaying) {\n"
        "            _state.value.currentTrack?.let(::commitDeferredPlaybackStartSideEffectsIfNeeded)\n"
        "        }\n"
        "    }"
    )
    vm = vm[:toggle_start] + toggle_fn + vm[toggle_end:]

vm_path.write_text(vm)


# ---------------------------------------------------------------------------
# 6) Explore UI: dedicated feeds, language-keyed ensure, correct Samples UX.
# ---------------------------------------------------------------------------
app_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt")
app = app_path.read_text()
app = replace_once(
    app,
    "    LaunchedEffect(Unit) { viewModel.ensureExplore(strings) }\n",
    "    LaunchedEffect(strings.code) { viewModel.ensureExplore(strings) }\n",
    "Explore language keyed loading",
)
app = replace_once(
    app,
    "    val freshTracks = state.exploreTracks\n"
    "    val samples = remember(state.exploreVideos) {\n"
    "        exploreSampleTracks(state.exploreVideos, ExploreImmersiveSampleLimit)\n"
    "    }\n",
    "    val freshTracks = state.exploreFreshTracks\n"
    "    val samples = remember(state.exploreSamples) {\n"
    "        exploreSampleTracks(state.exploreSamples, ExploreImmersiveSampleLimit)\n"
    "    }\n",
    "Explore dedicated Fresh/Samples feeds",
)
app = replace_once(
    app,
    "            isFreshLoading = state.isExploreLoading,\n",
    "            isFreshLoading = state.isFreshCurrentsLoading,\n",
    "Fresh currents loading state",
)
app = replace_once(
    app,
    "                if (samples.isEmpty()) viewModel.refreshSamples()\n",
    "                if (samples.isEmpty()) viewModel.ensureSamples()\n",
    "Samples shortcut non-destructive ensure",
)
app = replace_once(
    app,
    "                            subtitle = selectedZone?.label,\n",
    "                            subtitle = strings.exploreNewReleases,\n",
    "Fresh currents fixed clean subtitle",
)
app_path.write_text(app)

samples_ui_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/ExploreSamplesScreen.kt")
samples_ui = samples_ui_path.read_text()
if "internal enum class SamplesLoadSurfaceState" not in samples_ui:
    insertion = '''
internal enum class SamplesLoadSurfaceState {
    Loading,
    Error,
    Empty
}

internal fun samplesLoadSurfaceState(isLoading: Boolean, loadFailed: Boolean): SamplesLoadSurfaceState = when {
    isLoading -> SamplesLoadSurfaceState.Loading
    loadFailed -> SamplesLoadSurfaceState.Error
    else -> SamplesLoadSurfaceState.Empty
}
'''
    marker = "@Composable\ninternal fun ExploreSamplesScreen("
    samples_ui = replace_once(samples_ui, marker, insertion + "\n" + marker, "Samples load surface state helper")

samples_ui = replace_once(
    samples_ui,
    "        LaunchedEffect(Unit) { onRequestFeed() }\n",
    "",
    "remove duplicate Samples forced request",
)

old_surface_body = '''        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (isLoading || !loadFailed) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = if (loadFailed) strings.exploreEmpty else strings.exploreSamplesSubtitle,
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
        }'''
new_surface_body = '''        val surfaceState = samplesLoadSurfaceState(isLoading, loadFailed)
        Column(
            modifier = Modifier.align(Alignment.Center).padding(horizontal = 32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            if (surfaceState == SamplesLoadSurfaceState.Loading) {
                CircularProgressIndicator(
                    color = Color.White,
                    strokeWidth = 3.dp,
                    modifier = Modifier.size(44.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Rounded.Refresh,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(44.dp)
                )
            }
            Text(
                text = if (surfaceState == SamplesLoadSurfaceState.Error) {
                    strings.exploreSamplesError
                } else {
                    strings.exploreSamplesSubtitle
                },
                color = Color.White.copy(alpha = 0.78f),
                fontSize = 15.sp,
                lineHeight = 20.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis
            )
            if (surfaceState != SamplesLoadSurfaceState.Loading) {
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
                            text = strings.exploreSamplesRetry,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }'''
samples_ui = replace_once(samples_ui, old_surface_body, new_surface_body, "Samples loading/error UI")
samples_ui_path.write_text(samples_ui)


# ---------------------------------------------------------------------------
# 7) Localized Samples error/retry copy for every supported language.
# ---------------------------------------------------------------------------
explore_strings_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraExploreStrings.kt")
explore_strings = explore_strings_path.read_text()
explore_strings = replace_once(
    explore_strings,
    '    "exploreSamplesSubtitle"\n',
    '    "exploreSamplesSubtitle",\n    "exploreSamplesError",\n    "exploreSamplesRetry"\n',
    "Explore localization keys",
)

translations = {
    "en": ("Moods & genres", "Samples", "Vertical clips from the videos of the moment", "Samples are unavailable right now. Try again shortly.", "Retry"),
    "it": ("Mood e generi", "Samples", "Clip verticali dai video del momento", "Samples non disponibili al momento. Riprova tra poco.", "Riprova"),
    "es": ("Estados de ánimo y géneros", "Samples", "Clips verticales de los vídeos del momento", "Los Samples no están disponibles ahora. Inténtalo de nuevo en breve.", "Reintentar"),
    "fr": ("Ambiances et genres", "Samples", "Clips verticaux tirés des vidéos du moment", "Les Samples sont indisponibles pour le moment. Réessayez dans un instant.", "Réessayer"),
    "de": ("Stimmungen & Genres", "Samples", "Vertikale Clips aus den Videos der Stunde", "Samples sind gerade nicht verfügbar. Versuche es gleich noch einmal.", "Erneut versuchen"),
    "pt": ("Ambientes e géneros", "Samples", "Clipes verticais dos vídeos do momento", "Os Samples não estão disponíveis agora. Tenta novamente em breve.", "Tentar novamente"),
    "nl": ("Stemmingen en genres", "Samples", "Verticale clips uit de video's van het moment", "Samples zijn nu niet beschikbaar. Probeer het zo opnieuw.", "Opnieuw proberen"),
    "pl": ("Nastroje i gatunki", "Samples", "Pionowe klipy z najnowszych teledysków", "Samples są teraz niedostępne. Spróbuj ponownie za chwilę.", "Spróbuj ponownie"),
    "ro": ("Stări și genuri", "Samples", "Clipuri verticale din videoclipurile momentului", "Samples nu sunt disponibile momentan. Încearcă din nou în curând.", "Încearcă din nou"),
    "el": ("Διαθέσεις και είδη", "Δείγματα", "Κάθετα κλιπ από τα βίντεο της στιγμής", "Τα Samples δεν είναι διαθέσιμα αυτή τη στιγμή. Δοκιμάστε ξανά σε λίγο.", "Δοκιμή ξανά"),
    "sv": ("Stämningar och genrer", "Samples", "Vertikala klipp från stundens videor", "Samples är inte tillgängliga just nu. Försök igen om en stund.", "Försök igen"),
    "da": ("Stemninger og genrer", "Samples", "Lodrette klip fra øjeblikkets videoer", "Samples er ikke tilgængelige lige nu. Prøv igen om lidt.", "Prøv igen"),
    "cs": ("Nálady a žánry", "Ukázky", "Svislé klipy z aktuálních videoklipů", "Samples teď nejsou dostupné. Zkuste to za chvíli znovu.", "Zkusit znovu"),
    "uk": ("Настрої та жанри", "Семпли", "Вертикальні кліпи з актуальних відео", "Семпли зараз недоступні. Спробуйте ще раз трохи пізніше.", "Спробувати знову"),
    "ru": ("Настроения и жанры", "Сэмплы", "Вертикальные клипы из актуальных видео", "Сэмплы сейчас недоступны. Попробуйте ещё раз чуть позже.", "Повторить"),
    "tr": ("Ruh halleri ve türler", "Örnekler", "Anın videolarından dikey klipler", "Örnekler şu anda kullanılamıyor. Kısa süre sonra tekrar deneyin.", "Tekrar dene"),
    "ar": ("الأجواء والأنواع", "مقتطفات", "مقاطع عمودية من فيديوهات اللحظة", "المقتطفات غير متاحة الآن. أعد المحاولة بعد قليل.", "إعادة المحاولة"),
    "zh": ("心情与流派", "音乐短片", "来自当下热门视频的竖屏短片", "音乐短片暂时不可用，请稍后重试。", "重试"),
    "ja": ("ムードとジャンル", "サンプル", "話題のビデオから生まれた縦型クリップ", "サンプルは現在利用できません。しばらくしてからもう一度お試しください。", "再試行"),
    "ko": ("무드 및 장르", "샘플", "지금 뜨는 영상에서 뽑은 세로형 클립", "샘플을 지금 사용할 수 없습니다. 잠시 후 다시 시도하세요.", "다시 시도"),
    "hi": ("मूड और शैलियाँ", "सैंपल", "इस पल के वीडियो से वर्टिकल क्लिप", "सैंपल अभी उपलब्ध नहीं हैं। थोड़ी देर बाद फिर कोशिश करें।", "फिर कोशिश करें"),
    "id": ("Suasana dan genre", "Sampel", "Klip vertikal dari video terkini", "Sampel belum tersedia saat ini. Coba lagi sebentar lagi.", "Coba lagi"),
    "vi": ("Tâm trạng và thể loại", "Mẫu nhạc", "Clip dọc từ những video đang hot", "Mẫu nhạc hiện chưa khả dụng. Hãy thử lại sau ít phút.", "Thử lại"),
    "th": ("อารมณ์และแนวเพลง", "ตัวอย่างเพลง", "คลิปแนวตั้งจากวิดีโอที่กำลังมาแรง", "ตัวอย่างเพลงยังไม่พร้อมใช้งานในขณะนี้ โปรดลองอีกครั้งในอีกสักครู่", "ลองอีกครั้ง"),
    "fil": ("Mood at genre", "Samples", "Mga vertical na clip mula sa mga video ngayon", "Hindi available ang Samples ngayon. Subukan ulit maya-maya.", "Subukan ulit"),
    "he": ("מצבי רוח וז'אנרים", "דגימות", "קליפים אנכיים מתוך הסרטונים של הרגע", "הדגימות אינן זמינות כרגע. נסו שוב בעוד רגע.", "נסו שוב"),
}

for code, values in translations.items():
    old_prefix = f'    "{code}" to explore('
    line_start = explore_strings.find(old_prefix)
    if line_start < 0:
        raise RuntimeError(f"Explore bundle missing for {code}")
    line_end = explore_strings.find("\n", line_start)
    old_line = explore_strings[line_start:line_end]
    escaped = []
    for value in values:
        escaped_value = value.replace("\\", "\\\\").replace('"', '\\"')
        escaped.append(f'"{escaped_value}"')
    new_line = f'    "{code}" to explore({", ".join(escaped)}),'
    explore_strings = explore_strings[:line_start] + new_line + explore_strings[line_end:]

explore_strings_path.write_text(explore_strings)

strings_path = Path("app/src/main/java/com/luc4n3x/levyra/ui/i18n/LevyraStrings.kt")
strings = strings_path.read_text()
strings = replace_once(
    strings,
    '    val exploreSamplesSubtitle: String get() = value("exploreSamplesSubtitle")\n',
    '    val exploreSamplesSubtitle: String get() = value("exploreSamplesSubtitle")\n'
    '    val exploreSamplesError: String get() = value("exploreSamplesError")\n'
    '    val exploreSamplesRetry: String get() = value("exploreSamplesRetry")\n',
    "Explore Samples error/retry properties",
)
strings_path.write_text(strings)


# ---------------------------------------------------------------------------
# 8) Regression tests.
# ---------------------------------------------------------------------------
Path("app/src/test/java/com/luc4n3x/levyra/player/queue/PersistentQueueTransientPolicyTest.kt").write_text('''package com.luc4n3x.levyra.player.queue

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class PersistentQueueTransientPolicyTest {
    @Test
    fun normalQueueMutationsRemainDurable() {
        assertTrue(queuePersistenceAllowed(transientPlaybackActive = false))
    }

    @Test
    fun samplesTransientMutationsCanNeverBePersisted() {
        assertFalse(queuePersistenceAllowed(transientPlaybackActive = true))
    }
}
''')

Path("app/src/test/java/com/luc4n3x/levyra/data/FreshCurrentsPolicyTest.kt").write_text('''package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class FreshCurrentsPolicyTest {
    @Test
    fun officialEditorialFeedWinsOverFallback() {
        val editorial = track("editorial", "Official release", "Artist")
        val fallback = track("fallback", "Chart fallback", "Artist")
        assertEquals(listOf(editorial), selectFreshCurrentTracks(listOf(editorial), listOf(fallback), 12))
    }

    @Test
    fun samplesCanNeverLeakIntoFreshCurrents() {
        val short = track("short", "Vertical clip", "Artist", source = YOUTUBE_SHORTS_SOURCE, videoType = "SHORTS")
        val clean = track("clean", "Clean song", "Artist")
        val result = selectFreshCurrentTracks(listOf(short), listOf(clean), 12)
        assertEquals(listOf(clean), result)
        assertFalse(result.any(::isYoutubeShortTrack))
    }

    private fun track(id: String, title: String, artist: String, source: String = "Editorial", videoType: String = ""): Track = Track(
        id = id,
        title = title,
        artist = artist,
        album = title,
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = source,
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0,
        videoType = videoType
    )
}
''')

Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/SamplesStateIsolationTest.kt").write_text('''package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.domain.Track
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class SamplesStateIsolationTest {
    @Test
    fun publishingSamplesDoesNotReplaceHomeMusicVideos() {
        val homeVideo = track("home-video", "Music video")
        val sample = track("sample", "Vertical sample")
        val initial = LevyraUiState(exploreVideos = listOf(homeVideo))

        val published = initial.withPublishedSamples(listOf(sample))

        assertEquals(listOf(homeVideo), published.exploreVideos)
        assertEquals(listOf(sample), published.exploreSamples)
    }

    @Test
    fun pausedRestoreDefersPlaybackSideEffects() {
        assertFalse(shouldDispatchPlaybackStartSideEffects(startPaused = true))
    }

    private fun track(id: String, title: String): Track = Track(
        id = id,
        title = title,
        artist = "Artist",
        album = "Album",
        durationMs = 180_000L,
        streamUrl = "",
        videoUrl = "https://www.youtube.com/watch?v=abcdefghijk",
        thumbnailUrl = "https://example.test/$id.jpg",
        largeThumbnailUrl = "https://example.test/$id-large.jpg",
        source = "Test",
        moodTags = emptySet(),
        energy = 50,
        vocal = 50,
        replayScore = 70,
        cacheScore = 70,
        accentStart = 0,
        accentEnd = 0
    )
}
''')

Path("app/src/test/java/com/luc4n3x/levyra/ui/SamplesLoadSurfaceStateTest.kt").write_text('''package com.luc4n3x.levyra.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class SamplesLoadSurfaceStateTest {
    @Test
    fun onlyActiveLoadingShowsSpinnerState() {
        assertEquals(SamplesLoadSurfaceState.Loading, samplesLoadSurfaceState(isLoading = true, loadFailed = false))
        assertEquals(SamplesLoadSurfaceState.Error, samplesLoadSurfaceState(isLoading = false, loadFailed = true))
        assertEquals(SamplesLoadSurfaceState.Empty, samplesLoadSurfaceState(isLoading = false, loadFailed = false))
    }
}
''')

# Extend projection regression coverage for newly separated feeds.
projection_path = Path("app/src/test/java/com/luc4n3x/levyra/viewmodel/ExploreProjectionTest.kt")
projection = projection_path.read_text()
projection = replace_once(
    projection,
    "        assertFalse(baseline.isSamplesLoading)\n",
    "        assertFalse(baseline.isFreshCurrentsLoading)\n"
    "        assertFalse(baseline.isSamplesLoading)\n",
    "projection fresh loading assertion",
)
projection = replace_once(
    projection,
    "        assertTrue(exploreProjection(LevyraUiState(isSamplesLoading = true)).isSamplesLoading)\n",
    "        assertTrue(exploreProjection(LevyraUiState(isFreshCurrentsLoading = true)).isFreshCurrentsLoading)\n"
    "        assertTrue(exploreProjection(LevyraUiState(isSamplesLoading = true)).isSamplesLoading)\n",
    "projection fresh loading mutation",
)
projection_path.write_text(projection)


# ---------------------------------------------------------------------------
# Final structural assertions: fail before touching the branch if any invariant
# is not represented exactly as intended.
# ---------------------------------------------------------------------------
assert "queueEngine.replace(listOf(selected)" not in vm, "Samples still uses durable queue replace"
assert "queueEngine.beginTransientPlayback(session.queue" in vm, "Samples transient queue start missing"
assert "queueEngine.endTransientPlayback(session.queue)" in vm, "Samples transient queue restoration missing"
assert "startupShortsSnapshot" not in vm, "dead startup Shorts cache path remains"
assert "delay(if (startupShortsSnapshot.isFresh())" not in vm, "cold-start Samples trigger remains"
assert "exploreVideos = resolvedFeedTracks" not in vm, "Samples still contaminates Home video state"
assert "exploreSamples =" in vm, "dedicated Samples state is not published"
assert "chartsRepository.freshCurrentTracks" in vm, "Fresh Currents is not using clean feed"
assert "handlePlaybackStartSideEffects(playable, startPaused)" in vm, "paused restore side effects not gated"
assert "strings.exploreSamplesRetry" in samples_ui, "Retry copy not wired"
assert "if (isLoading || !loadFailed)" not in samples_ui, "infinite idle spinner condition remains"

print("PR313_REVIEW_FIXES_PATCH_OK")
