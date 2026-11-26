package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.catalog.artistCreditMatches
import com.luc4n3x.levyra.desktop.core.model.Track
import com.luc4n3x.levyra.desktop.core.storage.LibraryData
import com.luc4n3x.levyra.desktop.core.storage.JsonFileStore
import com.luc4n3x.levyra.desktop.core.storage.LibraryStore
import com.luc4n3x.levyra.desktop.player.AudioPlayer
import com.luc4n3x.levyra.desktop.player.PlayerEvent
import java.time.DayOfWeek
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.FormatStyle
import java.time.temporal.IsoFields
import java.time.temporal.TemporalAdjusters
import java.util.Locale
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull

enum class DiscoveryPickReason { NewRelease, Editorial, SimilarTo }

data class DiscoveryPick(val track: Track, val reason: DiscoveryPickReason, val seed: Track? = null) {
    val key: String get() = discoveryPickKey(track)
}

data class DiscoveryDeckStep(val pick: DiscoveryPick, val saved: Boolean)

enum class DiscoveryPreviewStatus { Idle, Loading, Playing }

enum class DiscoveryNoticeKind { Saved, SaveFailed, PreviewUnavailable }

data class DiscoveryNotice(val id: Long, val kind: DiscoveryNoticeKind)

data class DiscoveryDeckState(
    val picks: List<DiscoveryPick> = emptyList(),
    val index: Int = 0,
    val history: List<DiscoveryDeckStep> = emptyList(),
    val previewKey: String = "",
    val previewStatus: DiscoveryPreviewStatus = DiscoveryPreviewStatus.Idle,
    val notice: DiscoveryNotice? = null
) {
    val current: DiscoveryPick? get() = picks.getOrNull(index)
    val upcoming: List<DiscoveryPick> get() = picks.drop(index + 1).take(UPCOMING)
    val canUndo: Boolean get() = history.isNotEmpty()

    fun previewStatusFor(pick: DiscoveryPick?): DiscoveryPreviewStatus =
        if (pick != null && pick.key == previewKey) previewStatus else DiscoveryPreviewStatus.Idle

    fun decided(saved: Boolean): DiscoveryDeckState {
        val pick = current ?: return this
        return copy(index = index + 1, history = (history + DiscoveryDeckStep(pick, saved)).takeLast(MAX_HISTORY))
    }

    fun undone(): Pair<DiscoveryDeckState, DiscoveryDeckStep>? {
        val step = history.lastOrNull() ?: return null
        val restored = picks.indexOfFirst { it.key == step.pick.key }.takeIf { it >= 0 } ?: return null
        return copy(index = restored, history = history.dropLast(1)) to step
    }

    fun appended(more: List<DiscoveryPick>): DiscoveryDeckState {
        val known = picks.mapTo(HashSet()) { it.key }
        val fresh = more.filter { known.add(it.key) }
        return if (fresh.isEmpty()) this else copy(picks = picks + fresh)
    }

    companion object {
        const val UPCOMING = 2
        const val MAX_HISTORY = 30
    }
}

fun discoveryPicks(
    fresh: List<Track>,
    editorial: List<Track>,
    similar: List<DailyPick>,
    knownIds: Set<String>,
    excluded: (Track) -> Boolean
): List<DiscoveryPick> {
    val candidates = similar.map { DiscoveryPick(it.track, DiscoveryPickReason.SimilarTo, it.seed) } +
        fresh.map { DiscoveryPick(it, DiscoveryPickReason.NewRelease) } +
        editorial.map { DiscoveryPick(it, DiscoveryPickReason.Editorial) }
    val seen = HashSet<String>()
    return candidates.filter { pick ->
        pick.track.title.isNotBlank() && pick.track.id !in knownIds && !excluded(pick.track) && seen.add(pick.key)
    }
}

fun homeDiscoveryPicks(discover: DiscoverUiState, feed: HomeFeedState, library: LibraryData): List<DiscoveryPick> {
    val known = (library.history.map { it.track.id } + library.favorites.map { it.id }).toHashSet()
    val excluded = library.excludedArtists.values
    return discoveryPicks(
        fresh = discover.freshCurrents,
        editorial = discover.genres.flatMap { it.tracks },
        similar = feed.daily,
        knownIds = known
    ) { track -> excluded.any { artistCreditMatches(track.artist, it) } }
}

fun discoveryPickKey(track: Track): String = track.title.trim().lowercase(Locale.ROOT) + "|" + track.artist.trim().lowercase(Locale.ROOT)

fun discoveryWeekId(date: LocalDate): String {
    val year = date.get(IsoFields.WEEK_BASED_YEAR)
    val week = date.get(IsoFields.WEEK_OF_WEEK_BASED_YEAR)
    return "levyra-discovery-$year-w${week.toString().padStart(2, '0')}"
}

fun discoveryPreviewStartMs(durationMs: Long): Long =
    if (durationMs <= SHORT_TRACK_MS) 0L else (durationMs - PREVIEW_MS) * 35 / 100

class DiscoveryDeckController(
    private val scope: CoroutineScope,
    private val libraryStore: LibraryStore,
    private val dismissedStore: JsonFileStore<List<String>>,
    private val previewUrl: suspend (Track) -> String,
    private val playerFactory: () -> AudioPlayer,
    private val pauseMain: () -> Boolean,
    private val resumeMain: () -> Unit,
    private val playFullTrack: (Track) -> Unit,
    private val language: () -> String,
    private val today: () -> LocalDate = LocalDate::now,
    private val io: CoroutineDispatcher = Dispatchers.IO
) {
    private val state = MutableStateFlow(DiscoveryDeckState())
    val deck: StateFlow<DiscoveryDeckState> = state.asStateFlow()
    private var dismissed: List<String> = dismissedStore.read()
    private var previewPlayer: AudioPlayer? = null
    private var previewJob: Job? = null
    private var mainPausedByPreview = false
    private var noticeId = 0L

    val weeklyPlaylistId: String get() = discoveryWeekId(today())

    fun open(picks: List<DiscoveryPick>) {
        stopPreview()
        state.value = DiscoveryDeckState(picks = unseen(picks))
    }

    fun append(picks: List<DiscoveryPick>) {
        state.update { it.appended(unseen(picks)) }
    }

    fun save() {
        val pick = state.value.current ?: return
        val added = runCatching {
            libraryStore.ensurePlaylist(weeklyPlaylistId, weeklyPlaylistName())
            libraryStore.addToPlaylist(weeklyPlaylistId, listOf(pick.track.asLibraryEntry()))
        }.isSuccess
        if (!added) {
            notify(DiscoveryNoticeKind.SaveFailed)
            return
        }
        stopPreview()
        state.update { it.decided(saved = true) }
        notify(DiscoveryNoticeKind.Saved)
    }

    fun dismiss() {
        val pick = state.value.current ?: return
        rememberDismissed(dismissed - pick.key + pick.key)
        stopPreview()
        state.update { it.decided(saved = false) }
    }

    fun undo() {
        val (restored, step) = state.value.undone() ?: return
        stopPreview()
        if (step.saved) {
            libraryStore.removeFromPlaylist(weeklyPlaylistId, step.pick.track.id)
        } else {
            rememberDismissed(dismissed - step.pick.key)
        }
        state.update { restored.copy(notice = it.notice) }
    }

    fun togglePreview() {
        val pick = state.value.current ?: return
        if (state.value.previewStatusFor(pick) != DiscoveryPreviewStatus.Idle) stopPreview() else startPreview(pick)
    }

    fun playFull() {
        val pick = state.value.current ?: return
        stopPreview()
        mainPausedByPreview = false
        playFullTrack(pick.track)
    }

    fun close() {
        stopPreview()
        if (mainPausedByPreview) {
            mainPausedByPreview = false
            resumeMain()
        }
        previewPlayer?.let { player ->
            previewPlayer = null
            scope.launch(io) { runCatching { player.close() } }
        }
    }

    fun consumeNotice(id: Long) {
        state.update { if (it.notice?.id == id) it.copy(notice = null) else it }
    }

    fun onMainPlaybackStarted() {
        mainPausedByPreview = false
        stopPreview()
    }

    private fun startPreview(pick: DiscoveryPick) {
        previewJob?.cancel()
        if (pauseMain()) mainPausedByPreview = true
        state.update { it.copy(previewKey = pick.key, previewStatus = DiscoveryPreviewStatus.Loading) }
        previewJob = scope.launch {
            try {
                val url = withContext(io) { previewUrl(pick.track) }
                check(url.isNotBlank())
                val player = previewPlayer ?: withContext(io) { playerFactory() }.also { previewPlayer = it }
                val started = coroutineScope {
                    val outcome = async(start = CoroutineStart.UNDISPATCHED) {
                        withTimeoutOrNull(PREVIEW_START_TIMEOUT_MS) {
                            player.events.first { it is PlayerEvent.Playing || it is PlayerEvent.Failed }
                        }
                    }
                    withContext(io) { player.play(url, discoveryPreviewStartMs(pick.track.durationMs)) }
                    outcome.await()
                }
                check(started is PlayerEvent.Playing)
                state.update { if (it.previewKey == pick.key) it.copy(previewStatus = DiscoveryPreviewStatus.Playing) else it }
                delay(PREVIEW_MS)
                stopPreview()
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (_: Exception) {
                stopPreview()
                notify(DiscoveryNoticeKind.PreviewUnavailable)
            }
        }
    }

    private fun stopPreview() {
        previewJob?.cancel()
        previewJob = null
        if (state.value.previewStatus == DiscoveryPreviewStatus.Idle) return
        previewPlayer?.let { player -> scope.launch(io) { runCatching { player.stop() } } }
        state.update { it.copy(previewKey = "", previewStatus = DiscoveryPreviewStatus.Idle) }
    }

    private fun unseen(picks: List<DiscoveryPick>): List<DiscoveryPick> {
        val hidden = dismissed.toHashSet()
        val saved = libraryStore.current.playlists.firstOrNull { it.id == weeklyPlaylistId }?.tracks.orEmpty()
            .mapTo(HashSet(), ::discoveryPickKey)
        return picks.filterNot { it.key in hidden || it.key in saved }
    }

    private fun rememberDismissed(keys: List<String>) {
        dismissed = keys.takeLast(MAX_DISMISSED)
        val snapshot = dismissed
        scope.launch(io) { runCatching { dismissedStore.write(snapshot) } }
    }

    private fun notify(kind: DiscoveryNoticeKind) {
        noticeId += 1
        state.update { it.copy(notice = DiscoveryNotice(noticeId, kind)) }
    }

    private fun weeklyPlaylistName(): String {
        val code = language()
        val monday = today().with(TemporalAdjusters.previousOrSame(DayOfWeek.MONDAY))
        val date = runCatching {
            monday.format(DateTimeFormatter.ofLocalizedDate(FormatStyle.MEDIUM).withLocale(Locale.forLanguageTag(code)))
        }.getOrDefault(monday.toString())
        return "${com.luc4n3x.levyra.ui.i18n.discoveryCopy(code).weeklyPlaylist} · $date"
    }

    private companion object {
        const val MAX_DISMISSED = 600
        const val PREVIEW_START_TIMEOUT_MS = 12_000L
    }
}

private const val PREVIEW_MS = 30_000L
private const val SHORT_TRACK_MS = 40_000L
