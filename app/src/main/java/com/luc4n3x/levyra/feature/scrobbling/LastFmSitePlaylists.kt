package com.luc4n3x.levyra.feature.scrobbling

import android.annotation.SuppressLint
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.webkit.WebView
import android.webkit.WebViewClient
import kotlin.coroutines.resume
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.json.JSONArray

class LastFmSitePlaylists(private val context: Context) {
    private val gate = Mutex()
    private val mainHandler = Handler(Looper.getMainLooper())

    suspend fun playlists(user: String): List<LastFmPlaylist> =
        page(userUrl(user, "playlists"))?.let(LastFmSitePages::playlists).orEmpty()

    suspend fun tracks(user: String, playlistId: String): List<ListenBrainzTrack> =
        page(userUrl(user, "playlists", playlistId))?.let(LastFmSitePages::tracks).orEmpty()

    private fun userUrl(user: String, vararg segments: String): String =
        "https://www.last.fm/".toHttpUrl().newBuilder()
            .addPathSegment("user")
            .addPathSegment(user)
            .apply { segments.forEach(::addPathSegment) }
            .build()
            .toString()

    private suspend fun page(url: String): String? = gate.withLock {
        withTimeoutOrNull(PAGE_TIMEOUT_MS) { withContext(Dispatchers.Main) { load(url) } }
    }

    @SuppressLint("SetJavaScriptEnabled")
    private suspend fun load(url: String): String = suspendCancellableCoroutine { continuation ->
        val view = WebView(context.applicationContext)
        view.settings.javaScriptEnabled = true
        view.settings.blockNetworkImage = true
        view.webViewClient = object : WebViewClient() {
            override fun onPageFinished(loaded: WebView, loadedUrl: String?) {
                loaded.evaluateJavascript(SNAPSHOT_SCRIPT) { raw ->
                    val html = runCatching { JSONArray("[$raw]").optString(0) }.getOrDefault("")
                    if (!continuation.isActive || html.isBlank() || LastFmSitePages.isChallenge(html)) return@evaluateJavascript
                    loaded.stopLoading()
                    loaded.destroy()
                    continuation.resume(html)
                }
            }
        }
        continuation.invokeOnCancellation { mainHandler.post { view.stopLoading(); view.destroy() } }
        view.loadUrl(url)
    }

    private companion object {
        const val PAGE_TIMEOUT_MS = 25_000L
        const val SNAPSHOT_SCRIPT = "document.documentElement.outerHTML"
    }
}
