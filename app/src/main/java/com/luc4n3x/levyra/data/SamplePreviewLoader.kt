package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal class SamplePreviewLoader(context: Context) {
    private val resolver = PlaybackResolver.getInstance(context.applicationContext)

    suspend fun load(track: Track): Track? = withContext(Dispatchers.IO) {
        if (track.album != "YouTube Music Samples") return@withContext null
        withTimeoutOrNull(15_000L) {
            val resolved = resolver.prefetch(track, isVideoMode = true) ?: return@withTimeoutOrNull null
            val url = resolved.videoStreamUrl.ifBlank { resolved.streamUrl }.toHttpUrlOrNull() ?: return@withTimeoutOrNull null
            if (url.scheme != "https" || !url.host.endsWith(".googlevideo.com") || url.port != 443 || url.username.isNotEmpty() || url.password.isNotEmpty()) return@withTimeoutOrNull null
            preserveNativeSampleClip(track, resolved).copy(streamUrl = url.toString(), videoStreamUrl = "", playbackManifest = null)
        }
    }
}
