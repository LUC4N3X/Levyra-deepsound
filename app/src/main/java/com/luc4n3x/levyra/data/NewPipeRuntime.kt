package com.luc4n3x.levyra.data

import android.content.Context
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import okhttp3.Headers
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.downloader.StreamingResponse
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.ByteArrayInputStream
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object NewPipeRuntime {
    private val initialized = AtomicBoolean(false)
    private val providerInstalled = AtomicBoolean(false)

    @Volatile
    private var applicationContext: Context? = null

    fun ensure(context: Context? = null) {
        context?.applicationContext?.let { applicationContext = it }

        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpNewPipeDownloader(), Localization("it", "IT"), ContentCountry("IT"))
        }

        val appContext = applicationContext ?: return
        if (providerInstalled.compareAndSet(false, true)) {
            try {
                NewPipe.setYoutubeSessionPoTokenProvider(
                    LevyraYoutubeSessionPoTokenProvider(appContext)
                )
            } catch (error: Throwable) {
                providerInstalled.set(false)
                throw error
            }
        }
    }
}

private class OkHttpNewPipeDownloader : Downloader() {
    private val client = LevyraHttpClientFactory.extractor().newBuilder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(25, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .callTimeout(35, TimeUnit.SECONDS)
        .addNetworkInterceptor { chain ->
            validateSensitiveTokenTarget(chain.request())
            chain.proceed(chain.request())
        }
        .build()


    override fun supportsStreamingResponses(): Boolean = true

    override fun getStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?
    ): StreamingResponse = executeStreaming(
        Request.newBuilder()
            .get(url)
            .headers(headers)
            .localization(localization)
            .build(),
        client
    )

    override fun getStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?,
        timeoutMs: Long
    ): StreamingResponse {
        require(timeoutMs > 0L) { "timeoutMs must be positive" }
        val timeoutClient = client.newBuilder()
            .callTimeout(timeoutMs, TimeUnit.MILLISECONDS)
            .build()
        return executeStreaming(
            Request.newBuilder()
                .get(url)
                .headers(headers)
                .localization(localization)
                .build(),
            timeoutClient
        )
    }

    override fun postStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?
    ): StreamingResponse = executeStreaming(
        Request.newBuilder()
            .post(url, dataToSend)
            .headers(headers)
            .localization(localization)
            .build(),
        client
    )

    override fun execute(request: Request): Response {
        client.newCall(toOkHttpRequest(request)).execute().use { response ->
            return toExtractorResponse(response)
        }
    }

    override fun executeAsync(request: Request, callback: Downloader.AsyncCallback): CancellableCall {
        val call = client.newCall(toOkHttpRequest(request))
        val cancellableCall = CancellableCall(call)
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, e: IOException) {
                cancellableCall.setFinished()
                callback.onError(e)
            }

            override fun onResponse(call: okhttp3.Call, response: okhttp3.Response) {
                try {
                    response.use { callback.onSuccess(toExtractorResponse(it)) }
                } catch (error: Exception) {
                    callback.onError(error)
                } finally {
                    cancellableCall.setFinished()
                }
            }
        })
        return cancellableCall
    }

    private fun executeStreaming(
        request: Request,
        httpClient: okhttp3.OkHttpClient
    ): StreamingResponse {
        val response = httpClient.newCall(toOkHttpRequest(request)).execute()
        if (response.code == 429) {
            response.close()
            throw IOException("YouTube ha limitato temporaneamente le richieste")
        }
        val responseBody = response.body
        if (responseBody == null) {
            val headers = response.headers.toMultimap()
            val code = response.code
            response.close()
            return StreamingResponse(code, headers, ByteArrayInputStream(ByteArray(0)))
        }
        return object : StreamingResponse(
            response.code,
            response.headers.toMultimap(),
            responseBody.byteStream()
        ) {
            override fun close() {
                response.close()
            }
        }
    }

    private fun validateSensitiveTokenTarget(request: okhttp3.Request) {
        if (request.url.queryParameter("pot").isNullOrBlank()) return
        val host = request.url.host.lowercase()
        if (!request.url.isHttps ||
            !(host == "googlevideo.com" || host.endsWith(".googlevideo.com"))
        ) {
            throw IOException("Blocked sensitive YouTube token redirect outside GoogleVideo")
        }
    }

    private fun toOkHttpRequest(request: Request): okhttp3.Request {
        val method = request.httpMethod().uppercase()
        val data = request.dataToSend()
        val body = when {
            method == "GET" || method == "HEAD" -> null
            data != null -> data.toRequestBody()
            else -> ByteArray(0).toRequestBody()
        }
        val headers = request.headers().toOkHttpHeaders()
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)
            .headers(headers)

        if (headers["User-Agent"].isNullOrBlank()) {
            builder.header("User-Agent", "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 Chrome/142.0.0.0 Mobile Safari/537.36")
        }
        if (headers["Accept"].isNullOrBlank()) {
            builder.header("Accept", "*/*")
        }
        if (headers["Accept-Language"].isNullOrBlank()) {
            builder.header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
        }

        return builder.build()
    }

    private fun toExtractorResponse(response: okhttp3.Response): Response {
        val responseBody: okhttp3.ResponseBody? = response.body
        val responseBytes = responseBody?.bytes() ?: ByteArray(0)
        val responseText = responseBytes.toString(StandardCharsets.UTF_8)
        if (response.code == 429) {
            throw IOException("YouTube ha limitato temporaneamente le richieste")
        }
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            responseText,
            responseBytes,
            response.request.url.toString()
        )
    }

    private fun Map<String, List<String>>.toOkHttpHeaders(): Headers {
        val builder = Headers.Builder()
        forEach { (name, values) ->
            if (name.isNotBlank()) {
                values.forEach { value ->
                    if (value.isNotBlank()) {
                        builder.add(name, value)
                    }
                }
            }
        }
        return builder.build()
    }
}
