package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.discovery.DiscoveryCard
import com.luc4n3x.levyra.feature.discovery.DiscoveryDecision
import com.luc4n3x.levyra.feature.discovery.DiscoveryDeck
import com.luc4n3x.levyra.feature.discovery.DiscoveryEngine
import com.luc4n3x.levyra.feature.discovery.DiscoveryInteractionStore
import com.luc4n3x.levyra.feature.discovery.DiscoveryInteractions
import com.luc4n3x.levyra.feature.discovery.DiscoveryPreviewPlayer
import com.luc4n3x.levyra.feature.discovery.DiscoveryPreviewState
import com.luc4n3x.levyra.feature.discovery.DiscoveryPreviewStatus
import com.luc4n3x.levyra.feature.discovery.DiscoveryStep
import com.luc4n3x.levyra.feature.discovery.DiscoveryWeek
import com.luc4n3x.levyra.ui.i18n.discoveryCopy
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import timber.log.Timber

enum class DiscoveryFeedStatus { Idle, Loading, Ready, Empty, Error }

enum class DiscoveryMessageKind { Saved, SaveFailed, PreviewUnavailable }

@Immutable
data class DiscoveryMessage(val id: Long, val kind: DiscoveryMessageKind)

@Immutable
data class DiscoveryUiState(
    val status: DiscoveryFeedStatus = DiscoveryFeedStatus.Idle,
    val deck: DiscoveryDeck = DiscoveryDeck(),
    val preview: DiscoveryPreviewState = DiscoveryPreviewState(),
    val refreshing: Boolean = false,
    val weeklyPlaylistId: String = "",
    val savedThisWeek: Int = 0,
    val message: DiscoveryMessage? = null
) {
    val teaser: List<DiscoveryCard> get() = listOfNotNull(deck.current) + deck.upcoming
}

class DiscoveryViewModel internal constructor(private val root: LevyraViewModel) : ViewModel() {
    private val interactionStore = DiscoveryInteractionStore(root.getApplication<android.app.Application>())
    private val previewPlayer = DiscoveryPreviewPlayer(root.getApplication<android.app.Application>())
    private val engine = DiscoveryEngine(root.discoverySources())
    private val feed = MutableStateFlow(DiscoveryUiState())
    private val resolved = object : LinkedHashMap<String, Track>(RESOLVED_CACHE_SIZE, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, Track>?): Boolean = size > RESOLVED_CACHE_SIZE
    }
    private val failedKeys = HashSet<String>()
    private val saveMutex = Mutex()
    private var interactions = DiscoveryInteractions()
    private var loadJob: Job? = null
    private var moreJob: Job? = null
    private var previewJob: Job? = null
    private var prefetchJob: Job? = null
    private var loadedLanguage = ""
    private var loadedAtMs = 0L
    private var rotation = 0L
    private var sessionActive = false
    private var previewArmed = false
    private var mainResumeToken: String? = null
    private var messageId = 0L

    val state: StateFlow<DiscoveryUiState> = combine(
        feed,
        previewPlayer.state,
        root.state.map { snapshot -> snapshot.playlists.firstOrNull { it.id == currentWeek().id }?.tracks?.size ?: 0 }.distinctUntilChanged()
    ) { current, preview, saved ->
        current.copy(preview = preview, weeklyPlaylistId = currentWeek().id, savedThisWeek = saved)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000L), DiscoveryUiState())

    init {
        viewModelScope.launch {
            root.state.map { it.isPlaying }.distinctUntilChanged().collect { mainPlaying ->
                if (mainPlaying && (previewArmed || previewPlayer.state.value.audible)) {
                    previewArmed = false
                    mainResumeToken = null
                    previewJob?.cancel()
                    previewPlayer.stop()
                }
            }
        }
        viewModelScope.launch {
            previewPlayer.state.map { it.key to it.status }.distinctUntilChanged().collect { (key, status) ->
                if (status == DiscoveryPreviewStatus.Failed && key.isNotBlank()) {
                    failedKeys += key
                    resolved.remove(key)
                    post(DiscoveryMessageKind.PreviewUnavailable)
                }
            }
        }
    }

    fun warm() {
        if (loadJob?.isActive == true) return
        val language = languageCode()
        val stale = System.currentTimeMillis() - loadedAtMs > FEED_TTL_MS
        val current = feed.value
        val needsLoad = current.status == DiscoveryFeedStatus.Idle || current.status == DiscoveryFeedStatus.Error ||
            language != loadedLanguage || (stale && !sessionActive)
        if (needsLoad) load(keepVisible = current.deck.current != null && language == loadedLanguage)
    }

    fun refresh() {
        rotation++
        load(keepVisible = true)
    }

    fun retry() = load(keepVisible = false)

    fun openSession() {
        sessionActive = true
        warm()
        prefetchAhead()
    }

    fun closeSession() {
        sessionActive = false
        previewArmed = false
        previewJob?.cancel()
        prefetchJob?.cancel()
        previewPlayer.release()
        resolved.clear()
        failedKeys.clear()
        mainResumeToken?.let(root::resumeMainPlaybackAfterDiscovery)
        mainResumeToken = null
    }

    fun onBackground() {
        previewArmed = false
        previewJob?.cancel()
        previewPlayer.pause()
    }

    fun togglePreview() {
        val card = feed.value.deck.current ?: return
        val preview = previewPlayer.state.value
        when {
            preview.isFor(card.key) && preview.audible -> {
                previewArmed = false
                previewJob?.cancel()
                previewPlayer.pause()
            }
            preview.isFor(card.key) && previewPlayer.resume(card.key) -> previewArmed = true
            else -> {
                previewArmed = true
                startPreview(card)
            }
        }
    }

    fun save() = decide(DiscoveryDecision.Saved)

    fun dismiss() = decide(DiscoveryDecision.Dismissed)

    fun undo() {
        val (deck, step) = feed.value.deck.undo() ?: return
        feed.update { it.copy(deck = deck) }
        viewModelScope.launch {
            interactions = runCatchingSafely { interactionStore.revert(step) } ?: interactions
            if (step.decision == DiscoveryDecision.Saved && step.playlistId.isNotBlank()) {
                runCatchingSafely { root.removeDiscoveryTrack(step.playlistId, step.savedTrackId.ifBlank { step.card.track.id }) }
            }
        }
        onCardChanged()
    }

    fun playFull() {
        val deck = feed.value.deck
        val card = deck.current ?: return
        previewArmed = false
        mainResumeToken = null
        previewJob?.cancel()
        previewPlayer.stop()
        val lead = savedTrackFor(card)
        root.playAudioFrom(listOf(lead) + deck.upcoming.map(::savedTrackFor), lead)
    }

    fun openSavedPlaylist() {
        val id = currentWeek().id
        if (root.state.value.playlists.any { it.id == id }) root.openPlaylist(id)
    }

    fun consumeMessage(id: Long) {
        feed.update { if (it.message?.id == id) it.copy(message = null) else it }
    }

    private fun savedTrackFor(card: DiscoveryCard): Track = resolved[card.key]?.copy(streamUrl = "") ?: card.track

    private fun decide(decision: DiscoveryDecision) {
        val card = feed.value.deck.current ?: return
        val week = currentWeek()
        val track = savedTrackFor(card)
        val saving = decision == DiscoveryDecision.Saved
        feed.update { it.copy(deck = it.deck.decide(decision, if (saving) week.id else "", if (saving) track.id else "")) }
        viewModelScope.launch {
            interactions = runCatchingSafely { interactionStore.record(card, decision) } ?: interactions
            if (decision != DiscoveryDecision.Saved) return@launch
            val added = saveMutex.withLock { runCatchingSafely { root.saveDiscoveryTrack(week.id, weeklyPlaylistName(week), track) } }
            if (added == null) {
                interactions = runCatchingSafely { interactionStore.revert(DiscoveryStep(card, decision)) } ?: interactions
                markStepNotOwned(card)
                post(DiscoveryMessageKind.SaveFailed)
            } else {
                if (!added) markStepNotOwned(card)
                post(DiscoveryMessageKind.Saved)
            }
        }
        onCardChanged()
    }

    private fun markStepNotOwned(card: DiscoveryCard) {
        feed.update { state ->
            state.copy(deck = state.deck.copy(history = state.deck.history.map { step ->
                if (step.card.key == card.key && step.decision == DiscoveryDecision.Saved) step.copy(playlistId = "") else step
            }))
        }
    }

    private fun onCardChanged() {
        val deck = feed.value.deck
        val next = deck.current
        when {
            next == null -> {
                previewJob?.cancel()
                previewPlayer.stop()
            }
            previewArmed && sessionActive -> startPreview(next)
            previewPlayer.state.value.key != next.key -> {
                previewJob?.cancel()
                previewPlayer.stop()
            }
        }
        if (deck.remaining <= REFILL_THRESHOLD) loadMore()
        prefetchAhead()
    }

    private fun startPreview(card: DiscoveryCard) {
        if (mainResumeToken == null) mainResumeToken = root.pauseMainPlaybackForDiscovery()
        previewJob?.cancel()
        previewPlayer.markLoading(card.key)
        if (card.key in failedKeys) {
            previewPlayer.markFailed(card.key)
            return
        }
        previewJob = viewModelScope.launch {
            val track = resolved[card.key] ?: root.resolveDiscoveryAudio(card.track)?.also { resolved[card.key] = it }
            if (feed.value.deck.current?.key != card.key) return@launch
            if (track == null) previewPlayer.markFailed(card.key) else previewPlayer.play(card.key, track)
        }
    }

    private fun prefetchAhead() {
        if (!sessionActive) return
        val targets = feed.value.deck.let { deck -> deck.upcoming.take(PREFETCH_AHEAD) }
            .filter { it.key !in resolved && it.key !in failedKeys }
        if (targets.isEmpty()) return
        prefetchJob?.cancel()
        prefetchJob = viewModelScope.launch {
            for (card in targets) {
                if (!sessionActive) return@launch
                val track = root.resolveDiscoveryAudio(card.track)
                if (track != null) resolved[card.key] = track
            }
        }
    }

    private fun load(keepVisible: Boolean) {
        loadJob?.cancel()
        moreJob?.cancel()
        val language = languageCode()
        feed.update { current ->
            if (keepVisible && current.deck.current != null) current.copy(refreshing = true)
            else current.copy(status = DiscoveryFeedStatus.Loading, refreshing = false)
        }
        loadJob = viewModelScope.launch {
            try {
                interactions = runCatchingSafely { interactionStore.load() } ?: interactions
                val result = engine.build(root.discoveryProfile(), interactions, FEED_SIZE, dayRotation())
                loadedLanguage = language
                loadedAtMs = System.currentTimeMillis()
                failedKeys.clear()
                feed.update { current ->
                    when {
                        result.cards.isNotEmpty() -> current.copy(status = DiscoveryFeedStatus.Ready, deck = DiscoveryDeck(result.cards), refreshing = false)
                        keepVisible && current.deck.current != null -> current.copy(refreshing = false)
                        result.allSourcesFailed -> current.copy(status = DiscoveryFeedStatus.Error, deck = DiscoveryDeck(), refreshing = false)
                        else -> current.copy(status = DiscoveryFeedStatus.Empty, deck = DiscoveryDeck(), refreshing = false)
                    }
                }
                onCardChanged()
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Throwable) {
                Timber.w(error, "Discovery feed build failed")
                feed.update { current ->
                    if (keepVisible && current.deck.current != null) current.copy(refreshing = false)
                    else current.copy(status = DiscoveryFeedStatus.Error, refreshing = false)
                }
            }
        }
    }

    private fun loadMore() {
        if (moreJob?.isActive == true || loadJob?.isActive == true) return
        moreJob = viewModelScope.launch {
            val known = feed.value.deck.cards.associate { it.key to 0L }
            val result = runCatchingSafely {
                engine.build(root.discoveryProfile(), interactions.copy(dismissed = interactions.dismissed + known), FEED_SIZE, dayRotation() + 1)
            } ?: return@launch
            if (result.cards.isNotEmpty()) {
                feed.update { it.copy(deck = it.deck.appended(result.cards), status = DiscoveryFeedStatus.Ready) }
                prefetchAhead()
            } else if (feed.value.deck.current == null) {
                feed.update { it.copy(status = DiscoveryFeedStatus.Empty) }
            }
        }
    }

    private fun post(kind: DiscoveryMessageKind) {
        messageId++
        feed.update { it.copy(message = DiscoveryMessage(messageId, kind)) }
    }

    private fun languageCode(): String = LevyraLanguageCatalog.normalize(root.state.value.languageCode)

    private fun dayRotation(): Long = LocalDate.now().toEpochDay() * 7L + rotation

    private fun currentWeek(): DiscoveryWeek = DiscoveryWeek.of(LocalDate.now())

    private fun weeklyPlaylistName(week: DiscoveryWeek): String {
        val code = languageCode()
        val date = runCatching {
            week.monday.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(code)))
        }.getOrDefault(week.monday.toString())
        return "${discoveryCopy(code).weeklyPlaylist} · $date"
    }

    private suspend fun <T> runCatchingSafely(block: suspend () -> T): T? = try {
        block()
    } catch (cancellation: CancellationException) {
        throw cancellation
    } catch (error: Throwable) {
        Timber.w(error, "Discovery operation failed")
        null
    }

    override fun onCleared() {
        previewJob?.cancel()
        prefetchJob?.cancel()
        previewPlayer.release()
        super.onCleared()
    }

    private companion object {
        const val FEED_SIZE = 30
        const val REFILL_THRESHOLD = 5
        const val PREFETCH_AHEAD = 2
        const val RESOLVED_CACHE_SIZE = 16
        const val FEED_TTL_MS = 6L * 60L * 60L * 1000L
    }
}
