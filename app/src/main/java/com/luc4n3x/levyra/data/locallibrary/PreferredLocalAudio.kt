package com.luc4n3x.levyra.data.locallibrary

import android.content.Context
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.content.ContextCompat
import androidx.room.InvalidationTracker
import com.luc4n3x.levyra.data.LevyraPreferences
import com.luc4n3x.levyra.data.collections.isPersonalCollectionTrack
import com.luc4n3x.levyra.data.local.LevyraDatabase
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import com.luc4n3x.levyra.domain.Track
import com.luc4n3x.levyra.feature.radio.isLiveRadio
import com.luc4n3x.levyra.player.PlaybackService
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import timber.log.Timber

class PreferredLocalAudio private constructor(context: Context) {
    private val appContext = context.applicationContext
    private val database = LevyraDatabase.get(appContext)
    private val dao = database.localMediaDao()
    private val preferences = LevyraPreferences(appContext)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val indexMutex = Mutex()
    private val generation = AtomicLong()
    private val libraryRefreshRequested = AtomicBoolean(false)
    private val observerRegistered = AtomicBoolean(false)
    private val decisions = ConcurrentHashMap<String, List<String>>()
    private val unreadableUris: MutableSet<String> = ConcurrentHashMap.newKeySet()

    @Volatile
    private var index: LocalAudioMatchIndex? = null

    init {
        if (enabled()) scope.launch { warmUp() }
    }

    fun enabled(): Boolean = preferences.preferLocalAudio()

    fun setEnabled(value: Boolean) {
        preferences.setPreferLocalAudio(value)
        if (value) scope.launch { warmUp() }
    }

    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(appContext, LocalMediaStoreScanner.requiredPermission()) ==
            PackageManager.PERMISSION_GRANTED

    suspend fun playableMatch(track: Track, isVideoMode: Boolean = false): Track? {
        if (isVideoMode || !enabled() || PlaybackService.remotePlaybackStateFlow.value.connected) return null
        val online = track.withoutPreferredLocalStream()
        if (!eligibleForPreferredLocalAudio(online)) return null
        return withContext(Dispatchers.IO) {
            runCatchingPreservingCancellation { localTrackFor(online) }
                .onFailure { Timber.w(it, "Preferred local audio lookup failed") }
                .getOrNull()
        }
    }

    private suspend fun warmUp() {
        if (!hasPermission()) return
        observeLibraryChanges()
        refreshLibraryOnce()
        runCatchingPreservingCancellation { currentIndex() }
            .onFailure { Timber.w(it, "Preferred local audio index unavailable") }
    }

    private suspend fun localTrackFor(track: Track): Track? {
        if (!hasPermission()) return null
        observeLibraryChanges()
        refreshLibraryOnce()
        val key = decisionKey(track)
        val uris = decisions[key] ?: run {
            val observed = generation.get()
            val found = currentIndex().find(track).map { it.contentUri }
            if (decisions.size >= MAX_DECISIONS) decisions.clear()
            if (observed == generation.get()) decisions[key] = found
            found
        }
        val playable = uris.firstOrNull { it !in rejectedLocalStreams && it !in unreadableUris && readable(it) } ?: return null
        return track.withPreferredLocalStream(playable)
    }

    private fun readable(uri: String): Boolean {
        val openable = runCatching {
            appContext.contentResolver.openFileDescriptor(Uri.parse(uri), "r")?.use { true } ?: false
        }.getOrDefault(false)
        if (!openable) {
            unreadableUris += uri
            LocalLibraryRepository.get(appContext).requestScan(LocalScanMode.Quick, force = true)
        }
        return openable
    }

    private fun observeLibraryChanges() {
        if (!observerRegistered.compareAndSet(false, true)) return
        database.invalidationTracker.addObserver(
            object : InvalidationTracker.Observer(LOCAL_MEDIA_TABLE) {
                override fun onInvalidated(tables: Set<String>) {
                    generation.incrementAndGet()
                    index = null
                    decisions.clear()
                    unreadableUris.clear()
                }
            }
        )
    }

    private fun refreshLibraryOnce() {
        if (libraryRefreshRequested.compareAndSet(false, true)) {
            LocalLibraryRepository.get(appContext).requestScan(LocalScanMode.Quick)
        }
    }

    private suspend fun currentIndex(): LocalAudioMatchIndex {
        index?.let { return it }
        return indexMutex.withLock {
            index ?: run {
                val observed = generation.get()
                LocalAudioMatchIndex(dao.availableMatchRows()).also {
                    if (observed == generation.get()) index = it
                }
            }
        }
    }

    companion object {
        private const val LOCAL_MEDIA_TABLE = "local_media"
        private const val MAX_DECISIONS = 2_048

        @Volatile
        private var instance: PreferredLocalAudio? = null

        fun get(context: Context): PreferredLocalAudio = instance ?: synchronized(this) {
            instance ?: PreferredLocalAudio(context.applicationContext).also { instance = it }
        }
    }
}

private val handedOutLocalStreams: MutableSet<String> = ConcurrentHashMap.newKeySet()
private val handedOutSubstitutions: MutableSet<String> = ConcurrentHashMap.newKeySet()
private val rejectedLocalStreams: MutableSet<String> = ConcurrentHashMap.newKeySet()

fun rejectPreferredLocalStream(uri: String): Boolean {
    if (uri !in handedOutLocalStreams) return false
    rejectedLocalStreams += uri
    return true
}

fun isRejectedPreferredLocalStream(uri: String): Boolean =
    uri in handedOutLocalStreams && uri in rejectedLocalStreams

fun Track.isPreferredLocalSubstitute(): Boolean =
    streamUrl.isNotEmpty() &&
        !source.equals(OFFLINE_SOURCE, ignoreCase = true) &&
        substitutionKey(this) in handedOutSubstitutions

internal fun Track.withPreferredLocalStream(uri: String): Track {
    val substitute = copy(streamUrl = uri, videoStreamUrl = "", playbackManifest = null)
    handedOutLocalStreams += uri
    handedOutSubstitutions += substitutionKey(substitute)
    return substitute
}

fun Track.withoutPreferredLocalStream(): Track =
    if (isPreferredLocalSubstitute()) copy(streamUrl = "", videoStreamUrl = "") else this

private fun substitutionKey(track: Track): String =
    listOf(track.id, track.source, track.streamUrl).joinToString("\u001f")

internal fun eligibleForPreferredLocalAudio(track: Track): Boolean {
    if (track.title.isBlank() || track.source.equals(OFFLINE_SOURCE, ignoreCase = true)) return false
    if (track.source.equals(SPOTIFY_SAMPLES_SOURCE, ignoreCase = true)) return false
    if (track.isPersonalCollectionTrack() || track.isLiveRadio()) return false
    val stream = track.streamUrl.trim()
    return !stream.startsWith("content://", ignoreCase = true) && !stream.startsWith("file://", ignoreCase = true)
}

private fun decisionKey(track: Track): String =
    listOf(track.id, track.title, track.artist, track.albumArtist, track.album, track.durationMs, track.explicit)
        .joinToString("\u001f")

private const val OFFLINE_SOURCE = "Offline"
private const val SPOTIFY_SAMPLES_SOURCE = "Spotify Samples"
