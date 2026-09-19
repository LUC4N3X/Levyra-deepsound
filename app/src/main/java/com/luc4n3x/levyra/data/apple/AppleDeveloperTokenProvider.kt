package com.luc4n3x.levyra.data.apple

import android.content.Context
import android.util.Base64
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import timber.log.Timber
import java.io.IOException
import java.util.concurrent.TimeUnit

class AppleDeveloperTokenProvider private constructor(context: Context) {
    private val client: OkHttpClient = LevyraHttpClientFactory.general(context.applicationContext)
        .newBuilder()
        .connectTimeout(5, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .callTimeout(10, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    private val scriptClient: OkHttpClient = client.newBuilder()
        .followRedirects(false)
        .followSslRedirects(false)
        .build()

    private val tokenMutex = Mutex()
    private var cachedToken: String? = null
    private var tokenExpiresAt: Long = 0L

    suspend fun getDeveloperToken(): String? = tokenMutex.withLock {
        val now = System.currentTimeMillis()
        cachedToken?.takeIf { tokenExpiresAt > now + TOKEN_EXPIRY_MARGIN_MS }?.let {
            return@withLock it
        }

        return withContext(Dispatchers.IO) {
            try {
                val html = requestText(APPLE_BROWSE_URL) ?: return@withContext null
                val scripts = extractScriptUrls(html).take(MAX_TOKEN_SCRIPT_CANDIDATES)
                for (script in scripts) {
                    val source = try {
                        requestTrustedScriptText(script)
                    } catch (error: CancellationException) {
                        throw error
                    } catch (_: Throwable) {
                        null
                    } ?: continue

                    for (match in JWT_REGEX.findAll(source)) {
                        val tokenCandidate = match.value
                        val exp = jwtExpiration(tokenCandidate) ?: continue
                        if (exp > now + TOKEN_EXPIRY_MARGIN_MS) {
                            cachedToken = tokenCandidate
                            tokenExpiresAt = exp
                            Timber.d("Apple developer token acquired, expires in %d min", (exp - now) / 60_000L)
                            return@withContext tokenCandidate
                        }
                    }
                }
                null
            } catch (error: CancellationException) {
                throw error
            } catch (error: Throwable) {
                Timber.w(error, "Failed to retrieve Apple Music developer token")
                null
            }
        }
    }

    suspend fun requireDeveloperToken(): String =
        getDeveloperToken() ?: throw IOException("Apple Music developer token unavailable")

    private fun requestText(url: String): String? {
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", USER_AGENT)
            .header("Accept", "*/*")
            .build()
        val call = client.newCall(request)
        return try {
            call.execute().use { response ->
                if (!response.isSuccessful) null else response.body.string().takeIf(String::isNotBlank)
            }
        } catch (_: IOException) {
            null
        }
    }

    private fun requestTrustedScriptText(url: String): String? {
        var currentUrl = trustedAppleMusicScriptUrl(url)?.toHttpUrlOrNull() ?: return null
        var redirects = 0
        while (redirects <= MAX_TOKEN_SCRIPT_REDIRECTS) {
            val request = Request.Builder()
                .url(currentUrl)
                .header("User-Agent", USER_AGENT)
                .build()
            val call = scriptClient.newCall(request)
            try {
                val response = call.execute()
                response.use { resp ->
                    if (resp.code in SCRIPT_REDIRECT_CODES) {
                        val location = resp.header("Location") ?: return null
                        val next = trustedAppleMusicScriptRedirectUrl(currentUrl.toString(), location)
                            ?.toHttpUrlOrNull() ?: return null
                        currentUrl = next
                        redirects++
                    } else if (resp.isSuccessful) {
                        return resp.body.string().takeIf(String::isNotBlank)
                    } else {
                        return null
                    }
                }
            } catch (_: IOException) {
                return null
            }
        }
        return null
    }

    private fun extractScriptUrls(html: String): List<String> {
        val paths = LinkedHashSet<String>()
        APPLE_SCRIPT_SRC_REGEX.findAll(html).forEach { paths += it.groupValues[1] }
        APPLE_LEGACY_SCRIPT_REGEX.findAll(html).forEach { paths += it.groupValues[1] }
        return paths.mapNotNull(::trustedAppleMusicScriptUrl)
    }

    private fun trustedAppleMusicScriptUrl(value: String): String? {
        val clean = value.trim()
        if (clean.isBlank()) return null
        val candidate = when {
            clean.startsWith("https://music.apple.com/", ignoreCase = true) -> clean
            clean.startsWith("//music.apple.com/", ignoreCase = true) -> "https:$clean"
            clean.startsWith("/") -> "https://music.apple.com$clean"
            "://" !in clean -> "https://music.apple.com/$clean"
            else -> return null
        }
        val url = candidate.toHttpUrlOrNull() ?: return null
        if (
            url.scheme != "https" ||
            url.port != 443 ||
            !url.host.equals("music.apple.com", ignoreCase = true) ||
            url.username.isNotEmpty() ||
            url.password.isNotEmpty()
        ) {
            return null
        }
        return url.toString()
    }

    private fun trustedAppleMusicScriptRedirectUrl(currentUrl: String, location: String): String? {
        val current = currentUrl.toHttpUrlOrNull() ?: return null
        val resolved = current.resolve(location) ?: return null
        return trustedAppleMusicScriptUrl(resolved.toString())
    }

    private fun jwtExpiration(token: String): Long? = runCatching {
        val parts = token.split('.')
        if (parts.size < 2) return@runCatching null
        val payloadBytes = try {
            java.util.Base64.getUrlDecoder().decode(parts[1])
        } catch (_: Throwable) {
            Base64.decode(parts[1], Base64.URL_SAFE or Base64.NO_WRAP or Base64.NO_PADDING)
        }
        val payloadJson = JSONObject(String(payloadBytes, Charsets.UTF_8))
        val expSeconds = payloadJson.optLong("exp", 0L)
        if (expSeconds <= 0L) null else expSeconds * 1000L
    }.getOrNull()

    companion object {
        private const val APPLE_BROWSE_URL = "https://music.apple.com/us/browse"
        private const val TOKEN_EXPIRY_MARGIN_MS = 5L * 60L * 1000L
        private const val MAX_TOKEN_SCRIPT_CANDIDATES = 6
        private const val MAX_TOKEN_SCRIPT_REDIRECTS = 4
        private val SCRIPT_REDIRECT_CODES = setOf(300, 301, 302, 303, 307, 308)
        private const val USER_AGENT = "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/130 Mobile Safari/537.36"
        private val JWT_REGEX = Regex("ey[a-zA-Z0-9_-]+\\.ey[a-zA-Z0-9_-]+\\.[a-zA-Z0-9_-]+")
        private val APPLE_SCRIPT_SRC_REGEX = Regex("""(?i)<script\b[^>]*\bsrc\s*=\s*[\"']([^\"']+\.js(?:\?[^\"']*)?)[\"']""")
        private val APPLE_LEGACY_SCRIPT_REGEX = Regex("[\\\"']([^\\\"']*/assets/index[^\\\"']*\\.js)[\\\"']")

        @Volatile
        private var instance: AppleDeveloperTokenProvider? = null

        fun get(context: Context): AppleDeveloperTokenProvider =
            instance ?: synchronized(this) {
                instance ?: AppleDeveloperTokenProvider(context.applicationContext).also { instance = it }
            }
    }
}
