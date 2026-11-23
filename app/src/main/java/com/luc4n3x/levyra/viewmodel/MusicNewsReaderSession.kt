package com.luc4n3x.levyra.viewmodel

import com.luc4n3x.levyra.data.news.MusicNewsReaderState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

internal object MusicNewsReaderSession {
    private val current = MutableStateFlow<MusicNewsReaderState?>(null)
    val state: StateFlow<MusicNewsReaderState?> = current.asStateFlow()

    fun show(value: MusicNewsReaderState) {
        current.value = value
    }

    fun update(value: MusicNewsReaderState) {
        current.update { open -> if (open?.story?.url == value.story.url) value else open }
    }

    fun close() {
        current.value = null
    }
}
