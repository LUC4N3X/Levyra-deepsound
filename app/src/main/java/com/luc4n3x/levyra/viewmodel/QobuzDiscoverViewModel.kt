package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.qobuz.QobuzEditorialRepository
import com.luc4n3x.levyra.data.qobuz.QobuzPlaylist
import com.luc4n3x.levyra.data.qobuz.QobuzPlaylistDetail
import com.luc4n3x.levyra.data.qobuz.QobuzShelf
import com.luc4n3x.levyra.data.qobuz.QobuzShelfKind
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class QobuzDiscoverTab { Home, EditorPicks, ForYou }

data class QobuzDiscoverUiState(
    val locale: String = "",
    val tab: QobuzDiscoverTab = QobuzDiscoverTab.Home,
    val shelves: Map<QobuzShelfKind, QobuzShelf> = emptyMap(),
    val loading: Boolean = false,
    val failed: Boolean = false
) {
    fun shelf(kind: QobuzShelfKind): QobuzShelf? = shelves[kind]?.takeUnless { it.isEmpty }
}

data class QobuzPlaylistUiState(
    val playlist: QobuzPlaylist,
    val detail: QobuzPlaylistDetail? = null,
    val loading: Boolean = true,
    val failed: Boolean = false
)

class QobuzDiscoverViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QobuzEditorialRepository.get(application)
    private val _state = MutableStateFlow(QobuzDiscoverUiState())
    val state: StateFlow<QobuzDiscoverUiState> = _state.asStateFlow()
    private val _playlist = MutableStateFlow<QobuzPlaylistUiState?>(null)
    val playlist: StateFlow<QobuzPlaylistUiState?> = _playlist.asStateFlow()
    private var loadJob: Job? = null
    private var playlistJob: Job? = null

    fun load(locale: String, force: Boolean = false) {
        if (locale.isBlank()) return
        val current = _state.value
        if (!force && current.locale == locale && (current.loading || current.shelves.isNotEmpty())) return
        loadJob?.cancel()
        _state.update {
            if (it.locale == locale) it.copy(loading = true, failed = false)
            else QobuzDiscoverUiState(locale = locale, tab = it.tab, loading = true)
        }
        loadJob = viewModelScope.launch {
            val cached = SHELVES.mapNotNull { kind -> repository.cachedShelf(kind, locale)?.let { kind to it.shelf } }.toMap()
            if (cached.isNotEmpty()) publish(locale) { it.copy(shelves = it.shelves + cached) }
            val fresh = SHELVES.map { kind -> async { repository.shelf(kind, locale, force)?.let { kind to it.shelf } } }
                .awaitAll().filterNotNull().toMap()
            publish(locale) { state ->
                val merged = state.shelves + fresh
                state.copy(shelves = merged, loading = false, failed = merged.isEmpty())
            }
        }
    }

    fun selectTab(tab: QobuzDiscoverTab) {
        _state.update { state -> state.copy(tab = if (state.tab == tab) QobuzDiscoverTab.Home else tab) }
    }

    fun openPlaylist(playlist: QobuzPlaylist) {
        val locale = _state.value.locale.ifBlank { return }
        playlistJob?.cancel()
        _playlist.value = QobuzPlaylistUiState(playlist)
        playlistJob = viewModelScope.launch {
            val detail = repository.playlist(locale, playlist)
            _playlist.update { open ->
                if (open?.playlist?.id != playlist.id) open
                else open.copy(detail = detail, loading = false, failed = detail == null)
            }
        }
    }

    fun retryPlaylist() {
        _playlist.value?.playlist?.let(::openPlaylist)
    }

    fun closePlaylist() {
        playlistJob?.cancel()
        _playlist.value = null
    }

    private fun publish(locale: String, transform: (QobuzDiscoverUiState) -> QobuzDiscoverUiState) {
        _state.update { state -> if (state.locale == locale) transform(state) else state }
    }

    private companion object {
        val SHELVES = listOf(
            QobuzShelfKind.NewReleases,
            QobuzShelfKind.Playlists,
            QobuzShelfKind.IdealDiscography,
            QobuzShelfKind.Qobuzissime,
            QobuzShelfKind.PressAwards
        )
    }
}
