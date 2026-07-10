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
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException
import java.nio.charset.StandardCharsets
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

    private fun clientFor(request: Request): OkHttpClient {
        if (request.followRedirects()) return client
        return client.newBuilder()
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
                "Mozilla/5.0 (Linux; Android 15) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/142.0.0.0 Mobile Safari/537.36"
            )
        }
        if (!request.headers().containsHeader("Accept")) {
            builder.header("Accept", "*/*")
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
