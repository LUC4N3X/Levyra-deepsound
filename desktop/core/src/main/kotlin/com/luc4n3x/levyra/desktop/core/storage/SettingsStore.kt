package com.luc4n3x.levyra.desktop.core.storage

import com.luc4n3x.levyra.desktop.core.model.DesktopSettings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class SettingsStore(private val store: JsonFileStore<DesktopSettings>) {

    private val state = MutableStateFlow(store.read().sanitized())

    val settings: StateFlow<DesktopSettings> = state.asStateFlow()

    val current: DesktopSettings get() = state.value

    fun update(transform: (DesktopSettings) -> DesktopSettings) {
        val updated = transform(state.value).sanitized()
        if (updated == state.value) return
        state.value = updated
        store.write(updated)
    }

    companion object {
        fun create(paths: AppPaths): SettingsStore = SettingsStore(
            JsonFileStore(
                file = paths.settingsFile,
                serializer = DesktopSettings.serializer(),
                defaultValue = { DesktopSettings() }
            )
        )
    }
}
