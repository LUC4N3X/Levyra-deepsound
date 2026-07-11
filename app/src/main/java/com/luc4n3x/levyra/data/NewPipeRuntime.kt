package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.ServiceList
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.downloader.StreamingResponse
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.ByteArrayInputStream
import java.io.FilterInputStream
import java.io.IOException
import java.io.InputStream
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object NewPipeRuntime {
    private val initialized = AtomicBoolean(false)

    fun ensure() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpNewPipeDownloader(), Localization("it", "IT"), ContentCountry("IT"))
            ServiceList.YouTube.setLoadingTimeout(12)
        }
    }
}

private class OkHttpNewPipeDownloader : Downloader() {
    private val client: OkHttpClient = LevyraHttpClientFactory.extractor()
    private val streamingClient: OkHttpClient = client.newBuilder()
        .readTimeout(45, TimeUnit.SECONDS)
        .callTimeout(0, TimeUnit.MILLISECONDS)
        .build()

    override fun supportsStreamingResponses(): Boolean = true

    override fun execute(request: Request): Response {
        clientFor(request).newCall(toOkHttpRequest(request)).execute().use { response ->
            return toExtractorResponse(response)
        }
    }

    override fun executeAsync(request: Request, callback: Downloader.AsyncCallback): CancellableCall {
        val call = clientFor(request).newCall(toOkHttpRequest(request))
        val cancellableCall = CancellableCall(call)
        call.enqueue(object : okhttp3.Callback {
            override fun onFailure(call: okhttp3.Call, error: IOException) {
                try {
                    callback.onError(error)
                } finally {
                    cancellableCall.setFinished()
                }
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

    override fun getStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        localization: Localization?
    ): StreamingResponse {
        val request = Request.newBuilder()
            .get(url)
            .headers(headers)
            .localization(localization)
            .build()
        return executeStreaming(request)
    }

    override fun postStreaming(
        url: String,
        headers: Map<String, List<String>>?,
        dataToSend: ByteArray?,
        localization: Localization?
    ): StreamingResponse {
        val request = Request.newBuilder()
            .post(url, dataToSend)
            .headers(headers)
            .localization(localization)
            .build()
        return executeStreaming(request)
    }

    private fun executeStreaming(request: Request): StreamingResponse {
        val response = streamingClientFor(request).newCall(toOkHttpRequest(request)).execute()
        if (response.code == 429) {
            response.close()
            throw IOException("YouTube ha limitato temporaneamente le richieste")
        }
        val headers = response.headers.toMultimap()
        val body = response.body
        if (body == null) {
            val code = response.code
            response.close()
            return StreamingResponse(code, headers, ByteArrayInputStream(ByteArray(0)))
        }
        val stream = ResponseClosingInputStream(body.byteStream(), response)
        return StreamingResponse(response.code, headers, stream)
    }

    private fun clientFor(request: Request): OkHttpClient {
        if (request.followRedirects()) return client
        return client.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private fun streamingClientFor(request: Request): OkHttpClient {
        if (request.followRedirects()) return streamingClient
        return streamingClient.newBuilder()
            .followRedirects(false)
            .followSslRedirects(false)
            .build()
    }

    private fun toOkHttpRequest(request: Request): okhttp3.Request {
        val method = request.httpMethod().uppercase()
        val data = request.dataToSend()
        val body = when {
            method == "GET" || method == "HEAD" -> null
            data != null -> data.toRequestBody()
            else -> ByteArray(0).toRequestBody()
        }
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)
        request.headers().forEach { (name, values) ->
            values.filter { it.isNotBlank() }.forEach { value -> builder.addHeader(name, value) }
        }
        if (!request.headers().containsHeader("User-Agent")) {
            builder.header(
                "User-Agent",
                "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36"
            )
        }
        if (!request.headers().containsHeader("Accept")) {
            builder.header("Accept", "*/*")
        }
        if (!request.headers().containsHeader("Accept-Language")) {
            builder.header("Accept-Language", "it-IT,it;q=0.9,en-US;q=0.8,en;q=0.7")
        }
        return builder.build()
    }

    private fun toExtractorResponse(response: okhttp3.Response): Response {
        val responseBytes = response.body?.bytes() ?: ByteArray(0)
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

    private fun Map<String, List<String>>.containsHeader(name: String): Boolean {
        return keys.any { it.equals(name, ignoreCase = true) }
    }
}

private class ResponseClosingInputStream(
    input: InputStream,
    private val response: okhttp3.Response
) : FilterInputStream(input) {
    override fun close() {
        try {
            super.close()
        } finally {
            response.close()
        }
    }
}
