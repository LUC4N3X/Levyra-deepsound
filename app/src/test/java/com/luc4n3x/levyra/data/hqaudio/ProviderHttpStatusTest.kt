package com.luc4n3x.levyra.data.hqaudio

import java.io.IOException
import kotlinx.coroutines.runBlocking
import okhttp3.MediaType
import okhttp3.OkHttpClient
import okhttp3.Protocol
import okhttp3.Response
import okhttp3.ResponseBody
import okio.Buffer
import okio.BufferedSource
import okio.Source
import okio.Timeout
import okio.buffer
import org.junit.Assert.assertEquals
import org.junit.Test

class ProviderHttpStatusTest {
    @Test
    fun rateLimitHeadersReturnWithoutWaitingForTheErrorBody() = runBlocking {
        var reads = 0
        val source = object : Source {
            override fun read(sink: Buffer, byteCount: Long): Long {
                reads++
                throw IOException("Error body stalled")
            }

            override fun timeout(): Timeout = Timeout.NONE
            override fun close() = Unit
        }.buffer()
        val client = OkHttpClient.Builder().addInterceptor { chain ->
            Response.Builder()
                .request(chain.request())
                .protocol(Protocol.HTTP_1_1)
                .code(429)
                .message("Too Many Requests")
                .header("Retry-After", "90")
                .body(object : ResponseBody() {
                    override fun contentType(): MediaType? = null
                    override fun contentLength(): Long = 44L
                    override fun source(): BufferedSource = source
                })
                .build()
        }.build()
        try {
            val response = OkHttpProviderExchange { client }.execute(
                ProviderHttpRequest("https://tracks.monochrome.st/track/1", emptyMap(), 8_192)
            )

            assertEquals(429, response.code)
            assertEquals("90", response.header("Retry-After"))
            assertEquals(0, reads)
        } finally {
            client.dispatcher.executorService.shutdown()
            client.connectionPool.evictAll()
        }
    }
}
