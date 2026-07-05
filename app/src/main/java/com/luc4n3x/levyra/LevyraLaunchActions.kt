package com.luc4n3x.levyra

import android.content.Intent
import androidx.compose.runtime.mutableStateOf

object LevyraLaunchActions {
    const val EXTRA_SHORTCUT = "levyra.shortcut"
    const val EXTRA_ARTIST = "levyra.open_artist"
    const val SHORTCUT_FAVORITES = "favorites"
    const val SHORTCUT_FLOW = "flow"
    const val SHORTCUT_OFFLINE = "offline"
    const val SHORTCUT_LYRICS = "lyrics"

    val pendingShortcut = mutableStateOf<String?>(null)
    val pendingArtist = mutableStateOf<String?>(null)

    fun consumeFrom(intent: Intent?) {
        intent ?: return
        intent.getStringExtra(EXTRA_SHORTCUT)?.takeIf { it.isNotBlank() }?.let { value ->
            pendingShortcut.value = value
            intent.removeExtra(EXTRA_SHORTCUT)
        }
        intent.getStringExtra(EXTRA_ARTIST)?.takeIf { it.isNotBlank() }?.let { value ->
            pendingArtist.value = value
            intent.removeExtra(EXTRA_ARTIST)
        }
    }
}
