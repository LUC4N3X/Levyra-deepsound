package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.compose.runtime.Immutable
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.YoutubeMusicMoodSection
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.data.runCatchingPreservingCancellation
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

@Immutable
data class MoodGenrePageUiState(
    val key: String = "",
    val sections: List<YoutubeMusicMoodSection> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false
)

class MoodGenrePageViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application)
    private val _state = MutableStateFlow(MoodGenrePageUiState())
    val state: StateFlow<MoodGenrePageUiState> = _state.asStateFlow()
    private var loadJob: Job? = null

    fun load(params: String, languageCode: String, force: Boolean = false) {
        val key = "$languageCode:$params"
        val current = _state.value
        if (!force && current.key == key && (current.loading || current.sections.isNotEmpty())) return
        loadJob?.cancel()
        _state.update { it.copy(key = key, sections = it.sections.takeIf { _ -> it.key == key }.orEmpty(), loading = true, failed = false) }
        loadJob = viewModelScope.launch {
            val sections = runCatchingPreservingCancellation { repository.moodPlaylistSections(params, languageCode) }
                .onFailure { Timber.w(it, "mood genre page lookup failed") }
                .getOrNull()
            _state.update { latest ->
                if (latest.key != key) latest else latest.copy(
                    sections = sections?.takeIf { it.isNotEmpty() } ?: latest.sections,
                    loading = false,
                    failed = sections.isNullOrEmpty() && latest.sections.isEmpty()
                )
            }
        }
    }
}
