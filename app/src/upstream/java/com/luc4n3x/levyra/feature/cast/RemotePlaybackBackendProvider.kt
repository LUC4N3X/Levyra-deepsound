package com.luc4n3x.levyra.feature.cast

import android.content.Context

object RemotePlaybackBackendProvider {
    fun create(context: Context): RemotePlaybackBackend = GoogleCastBackend(context.applicationContext)
}
