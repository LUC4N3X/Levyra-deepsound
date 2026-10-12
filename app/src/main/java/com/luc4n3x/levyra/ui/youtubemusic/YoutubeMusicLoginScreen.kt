package com.luc4n3x.levyra.ui.youtubemusic

import android.annotation.SuppressLint
import android.webkit.CookieManager
import android.webkit.WebResourceRequest
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.dp
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicAuth
import com.luc4n3x.levyra.ui.i18n.YoutubeMusicCopy
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

@SuppressLint("SetJavaScriptEnabled")
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
        suspendCancellableCoroutine { continuation ->
            CookieManager.getInstance().removeAllCookies {
                CookieManager.getInstance().flush()
                if (continuation.isActive) continuation.resume(Unit)
            }
        }
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
        val web = webView
        if (!checking && web?.canGoBack() == true) web.goBack() else onClose()
    }
    Surface(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding().imePadding()) {
            TopAppBar(
                title = { Text(copy.login) },
                navigationIcon = { IconButton(onClick = onClose) { Icon(Icons.AutoMirrored.Rounded.ArrowBack, copy.back) } },
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
                    val web = webView
                    val url = web?.url
                    if (url != null && YoutubeMusicAuth.isMusicUrl(url)) {
                        val cookie = CookieManager.getInstance().getCookie(YoutubeMusicAuth.ORIGIN).orEmpty()
                        if (runCatching { YoutubeMusicAuth.authorization(cookie, 1) }.isSuccess) currentOnCookie(cookie) else web.reload()
                    } else web?.reload()
                }) { Text(copy.retry) }
            }
            if (ready) AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f),
                factory = { context ->
                    WebView(context).apply {
                        settings.javaScriptEnabled = true
                        settings.domStorageEnabled = true
                        settings.allowFileAccess = false
                        settings.allowContentAccess = false
                        settings.mixedContentMode = android.webkit.WebSettings.MIXED_CONTENT_NEVER_ALLOW
                        CookieManager.getInstance().setAcceptCookie(true)
                        CookieManager.getInstance().setAcceptThirdPartyCookies(this, true)
                        webViewClient = object : WebViewClient() {
                            override fun shouldOverrideUrlLoading(view: WebView, request: WebResourceRequest): Boolean =
                                request.isForMainFrame && !YoutubeMusicAuth.isLoginUrl(request.url.toString())
                            override fun onPageStarted(view: WebView, url: String?, favicon: android.graphics.Bitmap?) {
                                loading = true
                                webError = false
                            }
                            override fun onReceivedError(view: WebView, request: WebResourceRequest, error: android.webkit.WebResourceError) {
                                if (request.isForMainFrame) { webError = true; loading = false }
                            }
                            override fun onPageFinished(view: WebView, url: String?) {
                                loading = false
                                if (url == null || !YoutubeMusicAuth.isMusicUrl(url)) return
                                val cookie = CookieManager.getInstance().getCookie(YoutubeMusicAuth.ORIGIN).orEmpty()
                                if (cookie == submittedCookie || runCatching { YoutubeMusicAuth.authorization(cookie, 1) }.isFailure) return
                                submittedCookie = cookie
                                currentOnCookie(cookie)
                            }
                        }
                        webView = this
                        loadUrl(YoutubeMusicAuth.LOGIN_URL)
                    }
                }
            )
        }
    }
}
