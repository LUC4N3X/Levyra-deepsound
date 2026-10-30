package com.luc4n3x.levyra.data.hqaudio

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import com.luc4n3x.levyra.data.security.SafeImageUrlPolicy
import java.io.IOException
import java.util.concurrent.TimeUnit
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlinx.coroutines.suspendCancellableCoroutine
import okhttp3.Call
import okhttp3.Callback
import okhttp3.Dns
import okhttp3.HttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.Buffer
import okio.BufferedSource

internal class AudioCredentialsTransport(
    private val clientProvider: () -> OkHttpClient = ::credentialsClient
) {
    suspend fun fetch(): String = suspendCancellableCoroutine { continuation ->
        val request = Request.Builder().url(URL).header("Cache-Control", "no-cache").build()
        val call = clientProvider().newCall(request)
        continuation.invokeOnCancellation { call.cancel() }
        call.enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                if (continuation.isActive) continuation.resumeWithException(e)
            }

            override fun onResponse(call: Call, response: Response) {
                val result = runCatching { response.use {
                    if (it.code != 200 || !allows(it.request.url)) throw IOException("Credential update unavailable")
                    readBounded(it.body.source())
                } }
                if (continuation.isActive) result.fold(continuation::resume, continuation::resumeWithException)
            }
        })
    }

    companion object {
        const val URL = "https://raw.githubusercontent.com/LUC4N3X/Levyra-deepsound/main/catalog/audio-credentials.json"

        fun allows(url: HttpUrl): Boolean = url.toString() == URL

        fun readBounded(source: BufferedSource): String {
            val buffer = Buffer()
            var remaining = AudioCredentials.MAX_DOCUMENT_BYTES + 1L
            while (remaining > 0) {
                val read = source.read(buffer, remaining)
                if (read == -1L) break
                remaining -= read
            }
            if (buffer.size > AudioCredentials.MAX_DOCUMENT_BYTES) throw IOException("Credential update too large")
            return buffer.readUtf8()
        }

        private fun credentialsClient(): OkHttpClient {
            val base = LevyraHttpClientFactory.externalIntegrations()
            return base.newBuilder().connectTimeout(1, TimeUnit.SECONDS).readTimeout(1, TimeUnit.SECONDS)
                .callTimeout(2, TimeUnit.SECONDS).followRedirects(false).followSslRedirects(false)
                .retryOnConnectionFailure(false)
                .dns(Dns { host -> base.dns.lookup(host).also { addresses ->
                    if (addresses.isEmpty() || addresses.any { !SafeImageUrlPolicy.isPublicAddress(it) }) {
                        throw IOException("Blocked credential update destination")
                    }
                } })
                .addNetworkInterceptor { chain ->
                    val address = chain.connection()?.socket()?.inetAddress
                    if (!allows(chain.request().url) || address == null || !SafeImageUrlPolicy.isPublicAddress(address)) {
                        throw IOException("Blocked credential update connection")
                    }
                    chain.proceed(chain.request())
                }.build()
        }
    }
}
