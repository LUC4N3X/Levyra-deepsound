package com.luc4n3x.levyra.ui.youtubemusic

import com.luc4n3x.levyra.ui.components.LevyraBackButton
import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAuth
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicCopy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun YoutubeMusicLoginScreen(copy: YoutubeMusicCopy, checking: Boolean, error: String?, onCookie: (String) -> Unit, onClose: () -> Unit) {
    val currentOnCookie by rememberUpdatedState(onCookie)
    var ready by remember { mutableStateOf(false) }
    var webView by remember { mutableStateOf<WebView?>(null) }
    var webError by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var submittedCookie by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        clearLoginCookies()
        ready = true
    }
    DisposableEffect(Unit) {
        onDispose {
            webView?.stopLoading()
            webView?.destroy()
            CookieManager.getInstance().removeAllCookies { CookieManager.getInstance().flush() }
        }
    }
    BackHandler {
        navigateLoginBack(webView, checking, onClose)
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            TopAppBar(
                title = { Text(copy.login) },
                navigationIcon = { LevyraBackButton(onClick = onClose, contentDescription = copy.back) },
                windowInsets = WindowInsets(0, 0, 0, 0)
            )
            if (checking || loading || !ready) LinearProgressIndicator(Modifier.fillMaxWidth())
            if (checking) Text(copy.checking, Modifier.padding(16.dp))
            val message = error ?: if (webError) copy.networkError else null
            if (message != null) {
                Text(message, Modifier.padding(16.dp), color = MaterialTheme.colorScheme.error)
                TextButton(enabled = !checking, onClick = {
                    submittedCookie = null
                    webError = false
                    retryLogin(webView, currentOnCookie)
                }) { Text(copy.retry) }
            }
            if (ready) AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { context ->
                    createLoginWebView(context, { loading = it; if (it) webError = false }, { webError = true; loading = false }) { cookie ->
                        if (cookie != submittedCookie) {
                            submittedCookie = cookie
                            currentOnCookie(cookie)
                        }
                    }.also { webView = it }
                }
            )
        }
    }
}

private suspend fun clearLoginCookies() {
    suspendCancellableCoroutine { continuation ->
        CookieManager.getInstance().removeAllCookies {
            CookieManager.getInstance().flush()
            if (continuation.isActive) continuation.resume(Unit)
        }
    }
}

private fun navigateLoginBack(webView: WebView?, checking: Boolean, onClose: () -> Unit) {
    if (!checking && webView?.canGoBack() == true) webView.goBack() else onClose()
}

private fun retryLogin(webView: WebView?, onCookie: (String) -> Unit) {
    val web = webView ?: return
    val url = web.url
    if (url != null && YoutubeMusicAuth.isMusicUrl(url)) {
        val cookie = CookieManager.getInstance().getCookie(YoutubeMusicAuth.ORIGIN).orEmpty()
        if (runCatching { YoutubeMusicAuth.authorization(cookie, 1) }.isSuccess) onCookie(cookie) else web.reload()
    } else web.reload()
}

@SuppressLint("SetJavaScriptEnabled")
private fun createLoginWebView(context: android.content.Context, onLoading: (Boolean) -> Unit, onWebError: () -> Unit, onCookie: (String) -> Unit): WebView =
    WebView(context).apply {
        settings.javaScriptEnabled = true
        settings.domStorageEnabled = true
        settings.allowFileAccess = false
        settings.allowContentAccess = false
        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
        CookieManager.getInstance().setAcceptCookie(true)
        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
        webViewClient = YoutubeMusicLoginClient(onLoading, onWebError, onCookie)

        loadUrl(YoutubeMusicAuth.LOGIN_URL)
    }

private class YoutubeMusicLoginClient(
    private val onLoading: (Boolean) -> Unit,
    private val onWebError: () -> Unit,
    private val onCookie: (String) -> Unit
) : WebViewClient() {
    override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean {
        val blocked = request.isForMainFrame && !YoutubeMusicAuth.isSecureWebUrl(request.url.toString())
        if (blocked) onWebError()
        return blocked
    }
    override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
        onLoading(true)
    }
    override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
        if (request.isForMainFrame) onWebError()
    }
    override fun onPageFinished(view: WebView, url: String?) {
        onLoading(false)
        if (url == null || !YoutubeMusicAuth.isMusicUrl(url)) return
        captureCookie(view, 0)
    }

    private fun captureCookie(view: WebView, attempt: Int) {
        if (!YoutubeMusicAuth.isMusicUrl(view.url.orEmpty())) return
        val manager = CookieManager.getInstance()
        manager.flush()
        val cookie = manager.getCookie(YoutubeMusicAuth.ORIGIN).orEmpty()
        if (runCatching { YoutubeMusicAuth.authorization(cookie, 1) }.isSuccess) {
            onCookie(cookie)
        } else if (attempt < MAX_COOKIE_RETRIES) {
            view.postDelayed({ captureCookie(view, attempt + 1) }, COOKIE_RETRY_DELAY_MS * (attempt + 1))
        }
    }

    private companion object {
        const val MAX_COOKIE_RETRIES = 2
        const val COOKIE_RETRY_DELAY_MS = 500L
    }
}
