package com.luc4n3x.levyra

import android.content.Intent
import androidx.compose.runtime.mutableStateOf
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaIntentParser
import com.luc4n3x.levyra.feature.sharedmedia.SharedMediaRequest

object LevyraLaunchActions {
    private const val EXTRA_SHARED_MEDIA_CONSUMED = "levyra.shared_media_consumed"
    const val EXTRA_SHORTCUT = "levyra.shortcut"
    const val EXTRA_ARTIST = "levyra.open_artist"
    const val SHORTCUT_FAVORITES = "favorites"
    const val SHORTCUT_FLOW = "flow"
    const val SHORTCUT_OFFLINE = "offline"
    const val SHORTCUT_LYRICS = "lyrics"

    val pendingShortcut = mutableStateOf<String?>(null)
    val pendingArtist = mutableStateOf<String?>(null)
    val pendingSharedMedia = mutableStateOf<SharedMediaRequest?>(null)

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
        if (
            !intent.getBooleanExtra(EXTRA_SHARED_MEDIA_CONSUMED, false) &&
            (intent.action == Intent.ACTION_SEND || intent.action == Intent.ACTION_VIEW || intent.action == Intent.ACTION_SEND_MULTIPLE)
        ) {
            SharedMediaIntentParser.parse(intent)?.let { request ->
                pendingSharedMedia.value = request
                intent.putExtra(EXTRA_SHARED_MEDIA_CONSUMED, true)
                intent.removeExtra(Intent.EXTRA_TEXT)
                intent.removeExtra(Intent.EXTRA_HTML_TEXT)
                intent.removeExtra(Intent.EXTRA_SUBJECT)
                intent.removeExtra(Intent.EXTRA_STREAM)
                intent.clipData = null
                intent.setDataAndType(null, null)
            }
        }
    }
}
