package com.luc4n3x.levyra.viewmodel

import androidx.compose.runtime.Immutable
import com.luc4n3x.levyra.domain.ArtistExclusions
import com.luc4n3x.levyra.domain.MixLabCandidate
import com.luc4n3x.levyra.domain.MixLabEngine
import com.luc4n3x.levyra.domain.MixLabParams
import com.luc4n3x.levyra.domain.MixLabResult
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber

interface MixLabGateway {
    suspend fun candidatePool(): List<MixLabCandidate>
    suspend fun exclusions(): ArtistExclusions
    suspend fun canonicalSources(): List<Track>
    suspend fun saveAsPlaylist(name: String, tracks: List<Track>): String
    fun onSaved(playlistId: String)
}

enum class MixLabStage { Configure, Generating, Preview, Error }

@Immutable
data class MixLabSession(
    val generation: Long,
    val params: MixLabParams,
    val stage: MixLabStage,
    val result: MixLabResult? = null,
    val availableGenres: List<String> = emptyList(),
    val availableArtists: List<Pair<String, String>> = emptyList(),
    val savedPlaylistId: String? = null,
    val saving: Boolean = false,
    val saveFailed: Boolean = false
)

class MixLabController(
    private val scope: CoroutineScope,
    private val gateway: MixLabGateway,
    private val computeDispatcher: CoroutineDispatcher = Dispatchers.Default
) {
    private val _session = MutableStateFlow<MixLabSession?>(null)
    val session: StateFlow<MixLabSession?> = _session.asStateFlow()

    private var generation = 0L
    private var generateJob: Job? = null
    private var saveJob: Job? = null

    fun open(initialParams: MixLabParams = MixLabParams()) {
        generation += 1
        generateJob?.cancel()
        saveJob?.cancel()
        val sessionGeneration = generation
        _session.value = MixLabSession(generation = sessionGeneration, params = initialParams, stage = MixLabStage.Configure)
        scope.launch {
            val (genres, artists) = try {
                withContext(computeDispatcher) {
                    val pool = gateway.candidatePool()
                    val g = pool.flatMap { it.track.moodTags }
                        .map { it.trim().lowercase() }
                        .filter { it.isNotEmpty() && it !in IgnoredGenreTags }
                        .groupingBy { it }
                        .eachCount()
                        .toList()
                        .sortedByDescending { it.second }
                        .take(24)
                        .map { it.first }
                    val a = pool.map { it.track }
                        .mapNotNull { track ->
                            val artist = track.artist.trim()
                            if (artist.isEmpty()) null
                            else {
                                val key = com.luc4n3x.levyra.domain.LevyraPersonalOrbit.artistKeys(track).firstOrNull() ?: artist.lowercase()
                                artist to key
                            }
                        }
                        .distinctBy { it.second }
                        .take(30)
                    g to a
                }
            } catch (error: Exception) {
                if (error is CancellationException) throw error
                emptyList<String>() to emptyList<Pair<String, String>>()
            }
            mutate(sessionGeneration) { it.copy(availableGenres = genres, availableArtists = artists) }
        }
    }

    fun close() {
        generation += 1
        generateJob?.cancel()
        generateJob = null
        saveJob?.cancel()
        saveJob = null
        _session.value = null
    }

    fun updateParams(transform: (MixLabParams) -> MixLabParams) {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(params = transform(it.params)) }
    }

    fun generate() {
        val current = _session.value ?: return
        runGeneration(current.generation, current.params)
    }

    fun regenerate() {
        val current = _session.value ?: return
        if (current.result == null) return
        val nextParams = current.params.copy(seed = current.params.seed + 1)
        mutate(current.generation) { it.copy(params = nextParams) }
        runGeneration(current.generation, nextParams)
    }

    fun tuneParameters() {
        val current = _session.value ?: return
        mutate(current.generation) { it.copy(stage = MixLabStage.Configure, result = null, savedPlaylistId = null, saveFailed = false) }
    }

    private fun runGeneration(sessionGeneration: Long, params: MixLabParams) {
        generateJob?.cancel()
        mutate(sessionGeneration) { it.copy(stage = MixLabStage.Generating, result = null, savedPlaylistId = null, saveFailed = false) }
        generateJob = scope.launch {
            val result = try {
                withContext(computeDispatcher) {
                    val pool = gateway.candidatePool()
                    val exclusions = gateway.exclusions()
                    val canonicalSources = gateway.canonicalSources()
                    MixLabEngine.build(pool = pool, params = params, exclusions = exclusions, canonicalSources = canonicalSources)
                }
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mix Lab generation failed")
                null
            }
            mutate(sessionGeneration) { session ->
                when {
                    result == null -> session.copy(stage = MixLabStage.Error)
                    result.tracks.isEmpty() -> session.copy(stage = MixLabStage.Error, result = result)
                    else -> session.copy(stage = MixLabStage.Preview, result = result)
                }
            }
        }
    }

    fun saveAsPlaylist(name: String) {
        val current = _session.value ?: return
        val tracks = current.result?.tracks.orEmpty()
        if (tracks.isEmpty() || name.isBlank() || current.saving) return
        val sessionGeneration = current.generation
        mutate(sessionGeneration) { it.copy(saving = true, saveFailed = false) }
        saveJob = scope.launch {
            val playlistId = try {
                gateway.saveAsPlaylist(name.trim(), tracks)
            } catch (cancelled: CancellationException) {
                throw cancelled
            } catch (error: Exception) {
                Timber.w(error, "Mix Lab save as playlist failed")
                null
            }
            mutate(sessionGeneration) { session ->
                if (playlistId == null) {
                    session.copy(saving = false, saveFailed = true)
                } else {
                    session.copy(saving = false, saveFailed = false, savedPlaylistId = playlistId)
                }
            }
            if (playlistId != null) gateway.onSaved(playlistId)
        }
    }

    private inline fun mutate(sessionGeneration: Long, transform: (MixLabSession) -> MixLabSession) {
        _session.update { session -> if (session == null || session.generation != sessionGeneration) session else transform(session) }
    }

    companion object {
        private val IgnoredGenreTags = setOf("music", "shared", "imported", "hit", "chart", "local", "offline", "download", "shorts", "video")
    }
}
