package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.data.playlistimport.CatalogCandidate
import com.luc4n3x.levyra.data.playlistimport.PlaylistCandidateProvider
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportException
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportInput
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportPhase
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSession
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSessionStore
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSessionSummary
import com.luc4n3x.levyra.data.playlistimport.PlaylistImportSourceAdapter
import com.luc4n3x.levyra.data.playlistimport.PlaylistInputClassification
import com.luc4n3x.levyra.data.playlistimport.PlaylistReadProgress
import com.luc4n3x.levyra.data.playlistimport.classifyPlaylistInput
import com.luc4n3x.levyra.data.playlistimport.directTrack
import com.luc4n3x.levyra.data.playlistimport.playlistImportInputHash
import com.luc4n3x.levyra.data.playlistimport.toMatchCandidate
import com.luc4n3x.levyra.domain.Playlist
import com.luc4n3x.levyra.domain.PlaylistImportFailureKind
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.nexus.playlistimport.CandidateOrigin
import com.luc4n3x.levyra.nexus.playlistimport.ImportEntry
import com.luc4n3x.levyra.nexus.playlistimport.ImportEntryStatus
import com.luc4n3x.levyra.nexus.playlistimport.ImportReviewCounts
import com.luc4n3x.levyra.nexus.playlistimport.ImportReviewFilter
import com.luc4n3x.levyra.nexus.playlistimport.ImportedPlaylistDescriptor
import com.luc4n3x.levyra.nexus.playlistimport.MatchCandidate
import com.luc4n3x.levyra.nexus.playlistimport.MatchEvaluation
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportCompleteness
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportHealer
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportReview
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistImportSource
import com.luc4n3x.levyra.nexus.playlistimport.PlaylistMatchEngine
import com.luc4n3x.levyra.nexus.playlistimport.ResolutionPreference
import java.io.IOException
import java.util.UUID
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.joinAll
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

enum class PlaylistImportStep { INPUT, READING, INCOMPLETE, MATCHING, REVIEW, SAVING, SUMMARY, FAILED }

enum class PlaylistImportActivity { READING, MATCHING, CHECKING, PREPARING }

@Immutable
data class PlaylistImportSummary(
    val playlistId: String,
    val playlistName: String,
    val imported: Int,
    val skipped: Int,
    val unresolved: Int,
    val needsReview: Int,
    val manual: Int,
    val mergedRepeats: Int,
    val appended: Boolean
)

@Immutable
data class PlaylistManualSearchState(
    val position: Int,
    val query: String,
    val origin: CandidateOrigin,
    val loading: Boolean = false,
    val results: List<MatchEvaluation> = emptyList()
)

@Immutable
data class PlaylistImportUiState(
    val visible: Boolean = false,
    val step: PlaylistImportStep = PlaylistImportStep.INPUT,
    val input: String = "",
    val fileName: String? = null,
    val detectedSource: PlaylistImportSource? = null,
    val inputIssue: PlaylistImportFailureKind? = null,
    val descriptor: ImportedPlaylistDescriptor? = null,
    val completeness: PlaylistImportCompleteness = PlaylistImportCompleteness.Unknown,
    val activity: PlaylistImportActivity = PlaylistImportActivity.READING,
    val progressDone: Int = 0,
    val progressTotal: Int? = null,
    val entries: List<ImportEntry> = emptyList(),
    val counts: ImportReviewCounts = ImportReviewCounts(),
    val filter: ImportReviewFilter = ImportReviewFilter.ALL,
    val preference: ResolutionPreference = ResolutionPreference.SMART,
    val playlistName: String = "",
    val failure: PlaylistImportFailureKind? = null,
    val failureSource: PlaylistImportSource? = null,
    val summary: PlaylistImportSummary? = null,
    val resumable: List<PlaylistImportSessionSummary> = emptyList(),
    val focusedPosition: Int? = null,
    val manualSearch: PlaylistManualSearchState? = null,
    val committedPlaylistId: String? = null,
    val sessionId: String? = null
) {
    val busy: Boolean
        get() = step == PlaylistImportStep.READING || step == PlaylistImportStep.MATCHING || step == PlaylistImportStep.SAVING
}

interface PlaylistImportGateway {
    fun languageCode(): String

    suspend fun commit(name: String, tracks: List<Track>, playlistId: String): Playlist

    suspend fun append(playlistId: String, tracks: List<Track>)

    fun playlistsChanged()

    fun openPlaylist(playlistId: String)
}

class PlaylistImportController(
    private val scope: CoroutineScope,
    private val adapters: List<PlaylistImportSourceAdapter>,
    private val catalog: PlaylistCandidateProvider,
    private val store: PlaylistImportSessionStore,
    private val gateway: PlaylistImportGateway,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val clock: () -> Long = System::currentTimeMillis,
    private val workerCount: Int = RESOLUTION_WORKERS
) {
    private val _state = MutableStateFlow(PlaylistImportUiState())
    val state: StateFlow<PlaylistImportUiState> = _state.asStateFlow()

    private var job: Job? = null
    private var searchJob: Job? = null
    private var persistJob: Job? = null
    private var generation = 0L
    private var fileText: String? = null
    private var session: PlaylistImportSession? = null
    private var working: MutableList<ImportEntry> = mutableListOf()
    private val trackCache = HashMap<String, Track>()

    fun open(prefill: String? = null) {
        val current = _state.value
        if (current.committedPlaylistId != null && !current.busy) reset(keepVisible = true)
        _state.update { it.copy(visible = true) }
        refreshResumable()
        if (prefill != null && !_state.value.busy) {
            reset(keepVisible = true)
            updateInput(prefill)
            start()
        }
    }

    fun close() {
        val current = _state.value
        if (current.busy || current.step == PlaylistImportStep.REVIEW || current.step == PlaylistImportStep.INCOMPLETE) {
            _state.update { it.copy(visible = false, focusedPosition = null, manualSearch = null) }
        } else {
            finish()
        }
    }

    fun updateInput(text: String) {
        if (_state.value.busy) return
        fileText = null
        val classification = classifyPlaylistInput(PlaylistImportInput(text))
        _state.update {
            it.copy(
                input = text,
                fileName = null,
                detectedSource = (classification as? PlaylistInputClassification.Supported)?.source
                    ?: (classification as? PlaylistInputClassification.Unsupported)?.source,
                inputIssue = (classification as? PlaylistInputClassification.Unsupported)?.kind?.takeIf { text.isNotBlank() },
                failure = null
            )
        }
    }

    fun loadFile(text: String?, fileName: String, failure: PlaylistImportFailureKind = PlaylistImportFailureKind.TOO_LARGE) {
        if (_state.value.busy) return
        if (text == null) {
            _state.update { it.copy(visible = true, step = PlaylistImportStep.FAILED, failure = failure, fileName = fileName) }
            return
        }
        fileText = text
        val classification = classifyPlaylistInput(PlaylistImportInput(text, fileName))
        _state.update {
            it.copy(
                visible = true,
                input = "",
                fileName = fileName,
                detectedSource = (classification as? PlaylistInputClassification.Supported)?.source,
                inputIssue = (classification as? PlaylistInputClassification.Unsupported)?.kind,
                failure = null
            )
        }
        start()
    }

    fun setPreference(preference: ResolutionPreference) {
        if (_state.value.busy) return
        _state.update { it.copy(preference = preference) }
    }

    fun setPlaylistName(name: String) {
        _state.update { it.copy(playlistName = name.take(MAX_NAME_LENGTH)) }
        schedulePersist()
    }

    fun setFilter(filter: ImportReviewFilter) {
        _state.update { it.copy(filter = filter) }
    }

    fun start() {
        val current = _state.value
        if (current.busy) return
        val text = fileText ?: current.input
        val input = PlaylistImportInput(text, current.fileName)
        val detected = when (val classification = classifyPlaylistInput(input)) {
            PlaylistInputClassification.Empty -> return
            is PlaylistInputClassification.Unsupported -> {
                fail(classification.kind, classification.source)
                return
            }
            is PlaylistInputClassification.Supported -> classification
        }
        val adapter = adapters.firstOrNull { detected.source in it.sources }
        if (adapter == null) {
            fail(PlaylistImportFailureKind.UNSUPPORTED_SOURCE, detected.source)
            return
        }
        val hash = playlistImportInputHash("${current.fileName.orEmpty()}\n$text")
        val runGeneration = ++generation
        job?.cancel()
        job = scope.launch {
            val resumable = resumableSessions()
            if (runGeneration != generation) return@launch
            val existing = resumable.firstOrNull { it.inputHash == hash }
            if (existing != null) {
                resumeInternal(existing.id, runGeneration)
                return@launch
            }
            read(adapter, input, detected, hash, runGeneration)
        }
    }

    fun continueIncomplete() {
        if (_state.value.step != PlaylistImportStep.INCOMPLETE) return
        val runGeneration = ++generation
        job = scope.launch { match(runGeneration) }
    }

    fun cancel() {
        val runGeneration = ++generation
        job?.cancel()
        job = null
        searchJob?.cancel()
        val snapshot = session
        scope.launch {
            if (runGeneration != generation) return@launch
            if (snapshot != null && working.isNotEmpty()) persistNow()
            if (runGeneration != generation) return@launch
            clearWorking()
            val resumable = resumableSessions()
            if (runGeneration != generation) return@launch
            _state.value = PlaylistImportUiState(visible = true, resumable = resumable)
        }
    }

    fun resume(sessionId: String) {
        if (_state.value.busy) return
        val runGeneration = ++generation
        job?.cancel()
        job = scope.launch { resumeInternal(sessionId, runGeneration) }
    }

    fun discard(sessionId: String) {
        scope.launch {
            store.delete(sessionId)
            if (session?.id == sessionId) clearWorking()
            refreshResumable()
        }
    }

    fun retry() {
        val current = _state.value
        if (current.sessionId != null && working.isNotEmpty()) {
            val runGeneration = ++generation
            job = scope.launch { match(runGeneration) }
        } else {
            _state.update { it.copy(step = PlaylistImportStep.INPUT, failure = null) }
            start()
        }
    }

    fun openEntry(position: Int) {
        _state.update { it.copy(focusedPosition = position, manualSearch = null) }
    }

    fun closeEntry() {
        searchJob?.cancel()
        _state.update { it.copy(focusedPosition = null, manualSearch = null) }
    }

    fun chooseCandidate(position: Int, candidateId: String) = mutateEntry(position) { PlaylistImportReview.choose(it, candidateId) }

    fun skip(position: Int) = mutateEntry(position) { PlaylistImportReview.skip(it) }

    fun restoreAutomatic(position: Int) = mutateEntry(position) { PlaylistImportReview.restoreAutomatic(it) }

    fun acceptSuggestion(position: Int) = mutateEntry(position) { PlaylistImportReview.acceptSuggestion(it) }

    fun acceptAllSuggestions() {
        if (working.isEmpty()) return
        working = PlaylistImportReview.acceptAllSuggestions(working).toMutableList()
        publishEntries()
        schedulePersist()
    }

    fun chooseManual(position: Int, candidate: MatchCandidate) {
        mutateEntry(position) { PlaylistImportReview.chooseManual(it, candidate) }
        _state.update { it.copy(manualSearch = null) }
    }

    fun manualSearch(position: Int, query: String, origin: CandidateOrigin) {
        val identity = working.getOrNull(position)?.identity ?: return
        searchJob?.cancel()
        _state.update { it.copy(manualSearch = PlaylistManualSearchState(position, query, origin, loading = true)) }
        searchJob = scope.launch {
            val results = try {
                withContext(ioDispatcher) {
                    val found = catalog.search(query, origin)
                    remember(found)
                    found.map { PlaylistMatchEngine.evaluate(identity, it.candidate) }.sortedByDescending { it.score }
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: IOException) {
                Timber.w(error, "Manual playlist import search failed")
                emptyList()
            }
            _state.update { current ->
                if (current.manualSearch?.position != position) current
                else current.copy(manualSearch = PlaylistManualSearchState(position, query, origin, loading = false, results = results))
            }
        }
    }

    fun confirm() {
        val current = _state.value
        if (current.step != PlaylistImportStep.REVIEW || working.isEmpty()) return
        val selection = PlaylistImportReview.commitSelection(working)
        if (selection.isEmpty()) {
            _state.update { it.copy(failure = PlaylistImportFailureKind.NO_MATCHES) }
            return
        }
        val runGeneration = ++generation
        _state.update { it.copy(step = PlaylistImportStep.SAVING, failure = null) }
        job = scope.launch {
            val base = session ?: return@launch
            val existingId = base.committedPlaylistId.takeIf { base.phase == PlaylistImportPhase.COMMITTED }
            val playlistId = base.committedPlaylistId ?: UUID.randomUUID().toString()
            session = base.copy(committedPlaylistId = playlistId)
            try {
                persistNow()
                val tracks = selection.map(::trackFor)
                val name = current.playlistName.ifBlank { base.descriptor.title }.ifBlank { DEFAULT_NAME }
                val committed = if (existingId != null) {
                    gateway.append(existingId, tracks)
                    existingId
                } else {
                    gateway.commit(name, tracks, playlistId).id
                }
                if (runGeneration != generation) return@launch
                session = session?.copy(committedPlaylistId = committed)
                persistNow(PlaylistImportPhase.COMMITTED)
                gateway.playlistsChanged()
                val counts = PlaylistImportReview.counts(working)
                _state.update {
                    it.copy(
                        step = PlaylistImportStep.SUMMARY,
                        committedPlaylistId = committed,
                        summary = PlaylistImportSummary(
                            playlistId = committed,
                            playlistName = name,
                            imported = selection.size,
                            skipped = counts.skipped,
                            unresolved = counts.missing,
                            needsReview = counts.review,
                            manual = counts.manual,
                            mergedRepeats = counts.mergedRepeats,
                            appended = existingId != null
                        )
                    )
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Playlist import commit failed")
                _state.update { it.copy(step = PlaylistImportStep.REVIEW, failure = PlaylistImportFailureKind.STORAGE) }
            }
        }
    }

    fun viewPlaylist() {
        val id = _state.value.committedPlaylistId ?: return
        gateway.openPlaylist(id)
        finish()
    }

    fun importAnother() {
        if (_state.value.step != PlaylistImportStep.SUMMARY) return
        reset(keepVisible = true)
        refreshResumable()
    }

    fun fixMissing() {
        if (_state.value.step != PlaylistImportStep.SUMMARY) return
        _state.update { it.copy(step = PlaylistImportStep.REVIEW, filter = ImportReviewFilter.MISSING, summary = null) }
    }

    fun retryUnresolved() {
        if (working.isEmpty()) return
        val positions = working.indices.filter { working[it].status == ImportEntryStatus.MISSING }
        if (positions.isEmpty()) return
        val runGeneration = ++generation
        _state.update { it.copy(step = PlaylistImportStep.MATCHING, activity = PlaylistImportActivity.CHECKING, summary = null) }
        job = scope.launch {
            try {
                resolvePositions(positions, broad = true, runGeneration = runGeneration)
                if (runGeneration != generation) return@launch
                heal()
                enterReview(ImportReviewFilter.MISSING)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                handleFailure(error)
            }
        }
    }

    fun unresolvedReport(): String = working
        .filter { it.status == ImportEntryStatus.MISSING || it.status == ImportEntryStatus.SKIPPED || it.status == ImportEntryStatus.REVIEW }
        .joinToString("\n") { "${it.identity.position + 1}. ${it.identity.label}" }

    fun finish() {
        generation++
        job?.cancel()
        clearWorking()
        _state.value = PlaylistImportUiState()
        refreshResumable()
    }

    private suspend fun read(
        adapter: PlaylistImportSourceAdapter,
        input: PlaylistImportInput,
        detected: PlaylistInputClassification.Supported,
        hash: String,
        runGeneration: Long
    ) {
        if (runGeneration != generation) return
        clearWorking()
        _state.update {
            it.copy(
                step = PlaylistImportStep.READING,
                activity = PlaylistImportActivity.READING,
                detectedSource = detected.source,
                progressDone = 0,
                progressTotal = null,
                failure = null,
                failureSource = null,
                summary = null,
                committedPlaylistId = null
            )
        }
        try {
            catalog.refreshLocal()
            val result = withContext(ioDispatcher) {
                adapter.read(input, detected.detected, PlaylistReadProgress { read, declared ->
                    if (runGeneration == generation) _state.update { it.copy(progressDone = read, progressTotal = declared) }
                })
            }
            if (runGeneration != generation) return
            synchronized(trackCache) { trackCache.putAll(result.directTracks) }
            val parsed = result.playlist
            working = parsed.tracks.map { ImportEntry(it) }.toMutableList()
            val now = clock()
            val label = if (input.fileName != null) input.fileName.substringAfterLast('/') else input.text.trim().take(MAX_LABEL_LENGTH)
            session = PlaylistImportSession(
                id = UUID.randomUUID().toString(),
                createdAt = now,
                updatedAt = now,
                inputHash = hash,
                inputLabel = label,
                descriptor = parsed.descriptor,
                completeness = parsed.completeness,
                phase = PlaylistImportPhase.READ,
                preference = _state.value.preference,
                playlistName = parsed.descriptor.title,
                entries = working.toList(),
                tracks = emptyMap()
            )
            fileText = null
            _state.update {
                it.copy(
                    sessionId = session?.id,
                    descriptor = parsed.descriptor,
                    completeness = parsed.completeness,
                    playlistName = parsed.descriptor.title.ifBlank { it.playlistName },
                    progressDone = parsed.tracks.size,
                    progressTotal = parsed.descriptor.declaredTrackCount ?: parsed.tracks.size
                )
            }
            persistNow()
            if (parsed.completeness is PlaylistImportCompleteness.Incomplete) {
                _state.update { it.copy(step = PlaylistImportStep.INCOMPLETE) }
                return
            }
            match(runGeneration)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFailure(error)
        }
    }

    private suspend fun match(runGeneration: Long) {
        try {
            val pending = working.indices.filter { !working[it].resolved }
            _state.update {
                it.copy(
                    step = PlaylistImportStep.MATCHING,
                    activity = PlaylistImportActivity.MATCHING,
                    progressDone = working.size - pending.size,
                    progressTotal = working.size,
                    failure = null
                )
            }
            resolvePositions(pending, broad = false, runGeneration = runGeneration)
            if (runGeneration != generation) return
            _state.update { it.copy(activity = PlaylistImportActivity.CHECKING) }
            val healed = heal()
            if (healed.isNotEmpty()) {
                resolvePositions(healed.toList(), broad = true, runGeneration = runGeneration)
                if (runGeneration != generation) return
                heal()
            }
            _state.update { it.copy(activity = PlaylistImportActivity.PREPARING) }
            enterReview(ImportReviewFilter.ALL)
        } catch (error: CancellationException) {
            throw error
        } catch (error: Exception) {
            handleFailure(error)
        }
    }

    private suspend fun enterReview(filter: ImportReviewFilter) {
        publishEntries()
        _state.update { it.copy(step = PlaylistImportStep.REVIEW, filter = filter) }
        persistNow(if (session?.phase == PlaylistImportPhase.COMMITTED) PlaylistImportPhase.COMMITTED else PlaylistImportPhase.REVIEW)
    }

    private suspend fun resolvePositions(positions: List<Int>, broad: Boolean, runGeneration: Long) {
        if (positions.isEmpty()) return
        val preference = _state.value.preference
        var processed = 0
        var lastPublish = 0L
        coroutineScope {
            val queue = Channel<Pair<Int, ImportEntry>>(Channel.UNLIMITED)
            positions.forEach { queue.trySend(it to working[it]) }
            queue.close()
            val results = Channel<Pair<Int, ImportEntry>>(RESULT_BUFFER)
            val workers = List(workerCount) {
                launch(ioDispatcher) {
                    for ((position, entry) in queue) {
                        results.send(position to resolveEntry(entry, broad, preference))
                    }
                }
            }
            launch {
                workers.joinAll()
                results.close()
            }
            for ((position, entry) in results) {
                if (runGeneration != generation) continue
                working[position] = entry
                processed++
                val now = clock()
                if (now - lastPublish >= PROGRESS_INTERVAL_MS || processed == positions.size) {
                    lastPublish = now
                    val done = working.count { it.resolved }
                    _state.update { it.copy(progressDone = done, progressTotal = working.size) }
                }
                if (processed % PERSIST_EVERY == 0) persistNow(PlaylistImportPhase.MATCHING)
            }
        }
    }

    private suspend fun resolveEntry(entry: ImportEntry, broad: Boolean, preference: ResolutionPreference): ImportEntry {
        val identity = entry.identity
        val candidates = try {
            val direct = identity.directTrack(synchronized(trackCache) { trackCache[identity.directCatalogId] })
            val found = if (direct == null || broad) catalog.candidates(identity, broad) else emptyList()
            listOfNotNull(direct?.toMatchCandidate()) + found
        } catch (error: CancellationException) {
            throw error
        } catch (error: IOException) {
            Timber.d(error, "Candidate lookup failed for import entry %d", identity.position)
            emptyList<CatalogCandidate>()
        }
        val outcome = PlaylistMatchEngine.select(identity, candidates.map { it.candidate }, preference)
        outcome.selected?.candidate?.id?.let { id -> remember(candidates.filter { it.candidate.id == id }) }
        return if (broad && outcome.selected == null && entry.alternatives.isNotEmpty()) {
            entry.copy(resolved = true)
        } else {
            PlaylistImportReview.applyOutcome(entry, outcome)
        }
    }

    private fun remember(found: List<CatalogCandidate>) {
        synchronized(trackCache) {
            found.forEach { trackCache[it.candidate.id] = it.track }
        }
    }

    private fun heal(): Set<Int> {
        val result = PlaylistImportHealer.heal(working)
        working = result.entries.toMutableList()
        return result.reSearchPositions
    }

    private suspend fun resumeInternal(sessionId: String, runGeneration: Long) {
        val loaded = try {
            store.load(sessionId)
        } catch (error: IOException) {
            Timber.w(error, "Unable to load playlist import session")
            null
        }
        if (runGeneration != generation) return
        if (loaded == null) {
            refreshResumable()
            fail(PlaylistImportFailureKind.NOT_AVAILABLE, null)
            return
        }
        session = loaded
        working = loaded.entries.toMutableList()
        synchronized(trackCache) {
            trackCache.clear()
            trackCache.putAll(loaded.tracks)
        }
        _state.update {
            it.copy(
                visible = true,
                sessionId = loaded.id,
                descriptor = loaded.descriptor,
                completeness = loaded.completeness,
                detectedSource = loaded.descriptor.source,
                preference = loaded.preference,
                playlistName = loaded.playlistName,
                committedPlaylistId = loaded.committedPlaylistId,
                failure = null
            )
        }
        when {
            loaded.phase == PlaylistImportPhase.READ && loaded.completeness is PlaylistImportCompleteness.Incomplete -> {
                _state.update { it.copy(step = PlaylistImportStep.INCOMPLETE, progressDone = working.size) }
            }
            working.any { !it.resolved } -> match(runGeneration)
            else -> enterReview(ImportReviewFilter.ALL)
        }
    }

    private fun mutateEntry(position: Int, change: (ImportEntry) -> ImportEntry) {
        val entry = working.getOrNull(position) ?: return
        working[position] = change(entry)
        publishEntries()
        schedulePersist()
    }

    private fun publishEntries() {
        val snapshot = working.toList()
        _state.update { it.copy(entries = snapshot, counts = PlaylistImportReview.counts(snapshot)) }
    }

    private fun schedulePersist() {
        if (session == null) return
        persistJob?.cancel()
        persistJob = scope.launch {
            delay(PERSIST_DEBOUNCE_MS)
            persistNow()
        }
    }

    private suspend fun persistNow(phase: PlaylistImportPhase? = null) {
        val base = session ?: return
        val entries = working.toList()
        val selectedIds = entries.mapNotNullTo(HashSet()) { it.selected?.candidate?.id }
        val tracks = synchronized(trackCache) { trackCache.filterKeys { it in selectedIds } }
        val updated = base.copy(
            updatedAt = clock(),
            phase = phase ?: base.phase,
            preference = _state.value.preference,
            playlistName = _state.value.playlistName,
            entries = entries,
            tracks = tracks
        )
        session = updated
        try {
            store.save(updated)
        } catch (error: IOException) {
            Timber.w(error, "Unable to persist playlist import session")
        }
    }

    private fun trackFor(candidate: MatchCandidate): Track {
        val cached = synchronized(trackCache) { trackCache[candidate.id] }
        val track = cached ?: Track(
            id = candidate.id,
            title = candidate.title,
            artist = candidate.artistLine,
            album = candidate.album,
            durationMs = candidate.durationMs,
            streamUrl = "",
            videoUrl = if (candidate.origin == CandidateOrigin.ONLINE) "https://www.youtube.com/watch?v=${candidate.id}" else "",
            thumbnailUrl = candidate.artworkUrl,
            largeThumbnailUrl = candidate.artworkUrl,
            source = IMPORTED_SOURCE,
            moodTags = setOf("music", "imported"),
            energy = 50,
            vocal = 50,
            replayScore = 50,
            cacheScore = 50,
            accentStart = 0,
            accentEnd = 0,
            isrc = candidate.isrc
        )
        return if (candidate.origin == CandidateOrigin.ONLINE) track.copy(source = IMPORTED_SOURCE, streamUrl = "") else track
    }

    private fun handleFailure(error: Exception) {
        val kind = when (error) {
            is PlaylistImportException -> error.kind
            is IOException -> PlaylistImportFailureKind.NETWORK
            else -> PlaylistImportFailureKind.PROVIDER_CHANGED
        }
        Timber.w(error, "Playlist import failed: %s", kind)
        fail(kind, _state.value.detectedSource)
        refreshResumable()
    }

    private fun fail(kind: PlaylistImportFailureKind, source: PlaylistImportSource?) {
        _state.update { it.copy(visible = true, step = PlaylistImportStep.FAILED, failure = kind, failureSource = source) }
    }

    private suspend fun resumableSessions(): List<PlaylistImportSessionSummary> = try {
        store.resumable(clock())
    } catch (error: CancellationException) {
        throw error
    } catch (error: Exception) {
        Timber.w(error, "Unable to list resumable imports")
        emptyList()
    }

    private fun refreshResumable() {
        scope.launch {
            val resumable = resumableSessions()
            _state.update { it.copy(resumable = resumable) }
        }
    }

    private fun reset(keepVisible: Boolean) {
        generation++
        job?.cancel()
        clearWorking()
        _state.update { PlaylistImportUiState(visible = keepVisible, resumable = it.resumable) }
    }

    private fun clearWorking() {
        session = null
        working = mutableListOf()
        fileText = null
        synchronized(trackCache) { trackCache.clear() }
    }

    companion object {
        const val RESOLUTION_WORKERS = 4
        const val IMPORTED_SOURCE = "Imported playlist"
        private const val RESULT_BUFFER = 32
        private const val PROGRESS_INTERVAL_MS = 150L
        private const val PERSIST_EVERY = 100
        private const val PERSIST_DEBOUNCE_MS = 1_200L
        private const val MAX_NAME_LENGTH = 120
        private const val MAX_LABEL_LENGTH = 300
        private const val DEFAULT_NAME = "Imported playlist"
    }
}