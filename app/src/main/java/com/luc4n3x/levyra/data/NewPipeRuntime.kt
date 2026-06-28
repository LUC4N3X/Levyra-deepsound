package com.luc4n3x.levyra.data

import okhttp3.Headers.Companion.toHeaders
import okhttp3.OkHttpClient
import okhttp3.RequestBody.Companion.toRequestBody
import org.schabi.newpipe.extractor.NewPipe
import org.schabi.newpipe.extractor.downloader.Downloader
import org.schabi.newpipe.extractor.downloader.Request
import org.schabi.newpipe.extractor.downloader.Response
import org.schabi.newpipe.extractor.localization.ContentCountry
import org.schabi.newpipe.extractor.localization.Localization
import java.io.IOException
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
    private val client = OkHttpClient.Builder()
        .followRedirects(true)
        .followSslRedirects(true)
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(30, TimeUnit.SECONDS)
        .callTimeout(45, TimeUnit.SECONDS)
        .build()

    override fun execute(request: Request): Response {
        val body = request.dataToSend()?.toRequestBody()
        val builder = okhttp3.Request.Builder()
            .url(request.url())
            .method(request.httpMethod(), if (request.httpMethod().equals("GET", true) || request.httpMethod().equals("HEAD", true)) null else body)
            .headers(request.headers().flattenHeaders().toHeaders())
            .header("User-Agent", "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/126.0.0.0 Safari/537.36")
            .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
        client.newCall(builder.build()).execute().use { response ->
            val responseBody = response.body?.string().orEmpty()
            if (response.code == 429) throw IOException("YouTube ha limitato temporaneamente le richieste")
            return Response(
                response.code,
                response.message,
                response.headers.toMultimap(),
                responseBody,
                response.request.url.toString()
            )
        }
    }

    private fun Map<String, List<String>>.flattenHeaders(): Map<String, String> {
        val out = LinkedHashMap<String, String>()
        forEach { (key, values) ->
            if (key.isNotBlank() && values.isNotEmpty()) out[key] = values.joinToString(",")
        }
        return out
    }
}
