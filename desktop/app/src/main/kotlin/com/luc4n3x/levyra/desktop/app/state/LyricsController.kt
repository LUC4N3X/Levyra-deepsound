package com.luc4n3x.levyra.desktop.app.state

import com.luc4n3x.levyra.desktop.core.lyrics.Lyrics
import com.luc4n3x.levyra.desktop.core.lyrics.LyricsRepository
import com.luc4n3x.levyra.desktop.core.model.Track
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class LyricsUiState(
    val trackId: String = "",
    val lyrics: Lyrics? = null,
    val loading: Boolean = false
)

class LyricsController(
    private val scope: CoroutineScope,
    private val repository: LyricsRepository
) {
    private val state = MutableStateFlow(LyricsUiState())
    private var job: Job? = null

    val lyrics: StateFlow<LyricsUiState> = state.asStateFlow()

    fun requestFor(track: Track?) {
        if (track == null) {
            job?.cancel()
            state.value = LyricsUiState()
            return
        }
        val known = state.value
        if (track.id == known.trackId && (known.loading || known.lyrics != null)) return
        job?.cancel()
        state.value = LyricsUiState(trackId = track.id, loading = true)
        job = scope.launch {
            val resolved = try {
                repository.lyricsFor(track)
            } catch (cancellation: CancellationException) {
                throw cancellation
            } catch (error: Exception) {
                null
            }
            if (state.value.trackId == track.id) {
                state.value = LyricsUiState(trackId = track.id, lyrics = resolved, loading = false)
            }
        }
    }
}
