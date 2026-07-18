package com.luc4n3x.levyra.data

import okhttp3.Headers
import okhttp3.RequestBody.Companion.toRequestBody
import com.luc4n3x.levyra.data.network.LevyraHttpClientFactory
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException
import java.nio.charset.StandardCharsets
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicBoolean

object NewPipeRuntime {
    private val initialized = AtomicBoolean(false)

    fun ensure() {
        if (initialized.compareAndSet(false, true)) {
            NewPipe.init(OkHttpNewPipeDownloader(), Localization("it", "IT"), ContentCountry("IT"))
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
        .build()

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
