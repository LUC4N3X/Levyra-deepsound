package com.luc4n3x.levyra.desktop.core.extractor

import java.io.IOException
import java.nio.charset.StandardCharsets
import okhttp3.Headers
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.downloader.CancellableCall
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response

class DesktopDownloader(
    private val client: OkHttpClient = ExtractorHttp.client
) : Downloader() {

    override fun execute(request: Request): Response {
        client.newCall(toOkHttpRequest(request)).execute().use { response ->
            return toExtractorResponse(response)
        }
    }

    override fun executeAsync(request: Request, callback: AsyncCallback): CancellableCall {
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
        val payload = request.dataToSend()
        val body = when {
            method == "GET" || method == "HEAD" -> null
            payload != null -> payload.toRequestBody()
            else -> ByteArray(0).toRequestBody()
        }
        val headers = request.headers().toOkHttpHeaders()
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(method, body)
            .headers(headers)

        if (headers["User-Agent"].isNullOrBlank()) {
            builder.header("User-Agent", ExtractorHttp.DESKTOP_USER_AGENT)
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
        val bytes = response.body.bytes()
        if (response.code == 429) {
            throw IOException("YouTube ha limitato temporaneamente le richieste")
        }
        return Response(
            response.code,
            response.message,
            response.headers.toMultimap(),
            bytes.toString(StandardCharsets.UTF_8),
            bytes,
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
