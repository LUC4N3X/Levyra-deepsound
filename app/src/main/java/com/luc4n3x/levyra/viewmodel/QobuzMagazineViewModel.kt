package com.luc4n3x.levyra.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.luc4n3x.levyra.data.qobuz.QobuzEditorialRepository
import com.luc4n3x.levyra.data.qobuz.QobuzMagazinePage
import com.luc4n3x.levyra.data.qobuz.QobuzRubric
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class QobuzMagazineUiState(
    val locale: String = "",
    val section: String = "",
    val pages: Map<String, QobuzMagazinePage> = emptyMap(),
    val rubrics: List<QobuzRubric> = emptyList(),
    val loading: Boolean = false,
    val failed: Boolean = false
) {
    val page: QobuzMagazinePage? get() = pages[section]
}

class QobuzMagazineViewModel(application: Application) : AndroidViewModel(application) {
    private val repository = QobuzEditorialRepository.get(application)
    private val _state = MutableStateFlow(QobuzMagazineUiState())
    val state: StateFlow<QobuzMagazineUiState> = _state.asStateFlow()
    private var job: Job? = null

    fun load(locale: String, force: Boolean = false) {
        if (locale.isBlank()) return
        if (_state.value.locale != locale) _state.value = QobuzMagazineUiState(locale = locale)
        fetch(_state.value.section, force)
    }

    fun selectSection(section: String) {
        val next = if (_state.value.section == section) "" else section
        _state.update { it.copy(section = next) }
        fetch(next, force = false)
    }

    private fun fetch(section: String, force: Boolean) {
        val locale = _state.value.locale.ifBlank { return }
        if (!force && _state.value.pages[section] != null) return
        job?.cancel()
        _state.update { it.copy(loading = true, failed = false) }
        job = viewModelScope.launch {
            val page = repository.magazine(locale, section, force)
            _state.update { state ->
                if (state.locale != locale) return@update state
                val pages = if (page != null) state.pages + (section to page) else state.pages
                state.copy(
                    pages = pages,
                    rubrics = state.rubrics.ifEmpty { page?.rubrics.orEmpty() },
                    loading = false,
                    failed = page == null && pages[state.section] == null
                )
            }
        }
    }
}
