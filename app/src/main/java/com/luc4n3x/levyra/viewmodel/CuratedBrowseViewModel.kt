package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.YoutubeMusicMoodPlaylist
import com.luc4n3x.levyra.data.YoutubeMusicRepository
import com.luc4n3x.levyra.domain.ExploreCategory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import timber.log.Timber

private const val CURATED_BROWSE_FETCH_LIMIT = 40

internal fun curatedBrowseKey(language: String, params: String): String = "$language|$params"

internal class CuratedBrowseViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = YoutubeMusicRepository(application)
    private val _categories = MutableStateFlow<Map<String, List<ExploreCategory>>>(emptyMap())
    val categories: StateFlow<Map<String, List<ExploreCategory>>> = _categories.asStateFlow()
    private val _playlists = MutableStateFlow<Map<String, List<YoutubeMusicMoodPlaylist>>>(emptyMap())
    val playlists: StateFlow<Map<String, List<YoutubeMusicMoodPlaylist>>> = _playlists.asStateFlow()
    private val _failed = MutableStateFlow<Set<String>>(emptySet())
    val failed: StateFlow<Set<String>> = _failed.asStateFlow()
    private val _selected = MutableStateFlow("")
    val selected: StateFlow<String> = _selected.asStateFlow()
    private val _previous = MutableStateFlow("")
    val previous: StateFlow<String> = _previous.asStateFlow()
    private val pending = HashSet<String>()
    private var categoriesLanguage = ""
    private var categoriesJob: Job? = null

    fun load(language: String) {
        if (language.isBlank() || _categories.value[language].orEmpty().isNotEmpty()) return
        if (categoriesLanguage == language && categoriesJob?.isActive == true) return
        categoriesJob?.cancel()
        categoriesLanguage = language
        categoriesJob = viewModelScope.launch {
            val loaded = try {
                repository.moodCategories(language)
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Curated browse categories lookup failed")
                emptyList()
            }
            if (loaded.isNotEmpty()) _categories.update { it + (language to loaded) }
        }
    }

    fun select(language: String, params: String, shown: String) {
        if (shown != params) _previous.value = shown
        _selected.value = params
        request(language, params)
    }

    fun request(language: String, params: String) {
        val key = curatedBrowseKey(language, params)
        if (language.isBlank() || params.isBlank() || key in _playlists.value || !pending.add(key)) return
        _failed.update { it - key }
        viewModelScope.launch {
            try {
                val loaded = repository.moodPlaylists(params, language, CURATED_BROWSE_FETCH_LIMIT)
                if (loaded.isEmpty()) _failed.update { it + key } else _playlists.update { it + (key to loaded) }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Timber.w(error, "Curated browse playlists lookup failed")
                _failed.update { it + key }
            } finally {
                pending.remove(key)
            }
        }
    }
}
