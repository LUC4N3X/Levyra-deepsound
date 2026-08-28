package com.luc4n3x.levyra.data.network

import com.luc4n3x.levyra.domain.LevyraNetworkSettings
import com.luc4n3x.levyra.domain.LevyraNetworkSettingsValidator
import com.luc4n3x.levyra.domain.LevyraNetworkTestOutcome
import java.io.InterruptedIOException
import java.net.ConnectException
import java.net.NoRouteToHostException
import java.net.Proxy
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

object LevyraNetworkTester {
    private const val PROBE_URL = "https://www.youtube.com/generate_204"
    private const val PROBE_TIMEOUT_SECONDS = 10L
    private const val PROXY_AUTH_REQUIRED = 407

    suspend fun test(
        settings: LevyraNetworkSettings,
        proxyPassword: String
    ): LevyraNetworkTestOutcome = withContext(Dispatchers.IO) {
        if (LevyraNetworkSettingsValidator.validate(settings, proxyPassword.isNotEmpty()).isNotEmpty()) {
            return@withContext LevyraNetworkTestOutcome.InvalidConfiguration
        }
        val normalized = settings.normalized()
        val client = OkHttpClient.Builder()
            .connectTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .readTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .callTimeout(PROBE_TIMEOUT_SECONDS, TimeUnit.SECONDS)
            .retryOnConnectionFailure(false)
            .dns(LevyraNetworkConfiguration.buildDns(normalized, proxyPassword))
            .apply {
                val configuredProxy = LevyraNetworkConfiguration.proxyFor(normalized)
                if (configuredProxy != null) {
                    proxy(configuredProxy)
                    LevyraNetworkConfiguration.proxyAuthenticatorFor(normalized, proxyPassword)
                        ?.let(::proxyAuthenticator)
                } else {
                    proxy(Proxy.NO_PROXY)
                }
            }
            .build()
        try {
            val request = Request.Builder().url(PROBE_URL).head().build()
            client.newCall(request).execute().use { response ->
                when {
                    response.code == PROXY_AUTH_REQUIRED -> LevyraNetworkTestOutcome.ProxyAuthenticationFailed
                    response.isSuccessful || response.isRedirect -> LevyraNetworkTestOutcome.Success
                    else -> LevyraNetworkTestOutcome.UnknownError
                }
            }
        } catch (error: CancellationException) {
            throw error
        } catch (error: Throwable) {
            classify(error)
        } finally {
            runCatching { client.connectionPool.evictAll() }
            runCatching { client.dispatcher.executorService.shutdown() }
        }
    }

    internal fun classify(error: Throwable): LevyraNetworkTestOutcome {
        var current: Throwable? = error
        var depth = 0
        while (current != null && depth < MAX_CAUSE_DEPTH) {
            when (current) {
                is UnknownHostException -> return LevyraNetworkTestOutcome.DnsResolutionFailed
                is SSLException -> return LevyraNetworkTestOutcome.TlsFailure
                is SocketTimeoutException -> return LevyraNetworkTestOutcome.Timeout
                is ConnectException,
                is NoRouteToHostException -> return LevyraNetworkTestOutcome.ConnectionRefused
                is InterruptedIOException -> return LevyraNetworkTestOutcome.Timeout
            }
            if (isProxyAuthenticationFailure(current)) return LevyraNetworkTestOutcome.ProxyAuthenticationFailed
            val next = current.cause
            current = if (next === current) null else next
            depth++
        }
        return LevyraNetworkTestOutcome.UnknownError
    }

    private fun isProxyAuthenticationFailure(error: Throwable): Boolean {
        val message = error.message ?: return false
        return message.contains("Failed to authenticate with proxy", ignoreCase = true)
    }

    private const val MAX_CAUSE_DEPTH = 8
}
