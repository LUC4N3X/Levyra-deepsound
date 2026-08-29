package com.luc4n3x.levyra.player.sabr

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.TransferListener
import androidx.test.ext.junit.runners.AndroidJUnit4
import java.io.IOException
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Exercises the SABR transport end to end against a scripted server: request building, UMP parsing,
 * byte-accurate seeking, continuation, end of stream, redirects and media-network failover, with no
 * network access.
 */
@RunWith(AndroidJUnit4::class)
class SabrDataSourceTest {
    private val endpoint = "https://rr5---sn-a.googlevideo.com/videoplayback?mn=sn-a,sn-b&sabr=1"
    private val itag = 140
    private val lastModified = 1_766_955_925_572_207L
    private val initSegment = ByteArray(1_019) { (it % 251).toByte() }
    private val firstSegment = ByteArray(4_000) { ((it * 7) % 251).toByte() }
    private val secondSegment = ByteArray(3_000) { ((it * 11) % 251).toByte() }
    private val stream = initSegment + firstSegment + secondSegment

    @Test
    fun readsTheWholeStreamAcrossSeveralRequests() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.media(listOf(chunk(0, 0L, initSegment), chunk(1, 1_019L, firstSegment))),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)

        val length = source.open(DataSpec(Uri.parse(spec().toUri())))
        val read = source.readFully()
        source.close()

        assertEquals(stream.size.toLong(), length)
        assertArrayEquals(stream, read)
        assertEquals(2, factory.requests.size)
    }

    @Test
    fun aForwardSeekTrimsTheSegmentTheServerStartsFrom() {
        val position = 2_000L
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.media(listOf(chunk(0, 1_019L, firstSegment))),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)

        val length = source.open(
            DataSpec.Builder().setUri(Uri.parse(spec().toUri())).setPosition(position).build()
        )
        val read = source.readFully()
        source.close()

        assertEquals(stream.size - position, length)
        assertArrayEquals(stream.copyOfRange(position.toInt(), stream.size), read)
    }

    @Test
    fun aBackwardSeekRestartsFromTheEarlierByteInsteadOfStalling() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.media(listOf(chunk(0, 5_019L, secondSegment))),
                Response.media(listOf(chunk(0, 1_019L, firstSegment))),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)
        source.open(DataSpec.Builder().setUri(Uri.parse(spec().toUri())).setPosition(5_019L).build())
        source.readFully()
        source.close()

        val backward = SabrDataSource(factory)
        backward.open(DataSpec.Builder().setUri(Uri.parse(spec().toUri())).setPosition(1_019L).build())
        val read = backward.readFully()
        backward.close()

        assertArrayEquals(stream.copyOfRange(1_019, stream.size), read)
    }

    @Test
    fun aBoundedRangeStopsAtItsOwnEndOfStream() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(Response.media(listOf(chunk(0, 0L, initSegment), chunk(1, 1_019L, firstSegment))))
        )
        val source = SabrDataSource(factory)

        val length = source.open(
            DataSpec.Builder().setUri(Uri.parse(spec().toUri())).setPosition(0L).setLength(500L).build()
        )
        val read = source.readFully()
        source.close()

        assertEquals(500L, length)
        assertArrayEquals(stream.copyOfRange(0, 500), read)
    }

    @Test
    fun openingPastTheEndOfTheStreamReportsEndOfInputImmediately() {
        val source = SabrDataSource(ScriptedHttpDataSourceFactory(emptyList()))

        val length = source.open(
            DataSpec.Builder()
                .setUri(Uri.parse(spec().toUri()))
                .setPosition(stream.size.toLong())
                .build()
        )
        val read = source.read(ByteArray(16), 0, 16)
        source.close()

        assertEquals(0L, length)
        assertEquals(C.RESULT_END_OF_INPUT, read)
    }

    @Test
    fun anEmptyResponseIsRetriedAndThenGivesUpWithoutLooping() {
        val factory = ScriptedHttpDataSourceFactory(List(8) { Response.media(emptyList()) })
        val source = SabrDataSource(factory)
        source.open(DataSpec(Uri.parse(spec().toUri())))

        val failure = runCatching { source.readFully() }.exceptionOrNull()
        source.close()

        assertTrue(failure is IOException)
        assertTrue(factory.requests.size <= 4)
    }

    @Test
    fun aRedirectMovesTheSessionToTheHostTheServerNames() {
        val redirect = "https://rr9---sn-c.googlevideo.com/videoplayback?mn=sn-c&sabr=1"
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.parts(
                    listOf(SabrPart.SABR_REDIRECT to ProtoWriter().string(1, redirect).toByteArray())
                ),
                Response.media(listOf(chunk(0, 0L, initSegment), chunk(1, 1_019L, firstSegment))),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)

        source.open(DataSpec(Uri.parse(spec().toUri())))
        val read = source.readFully()
        source.close()

        assertArrayEquals(stream, read)
        assertEquals(redirect, factory.requests[1])
    }

    @Test
    fun aRedirectToAForeignHostIsRefused() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.parts(
                    listOf(
                        SabrPart.SABR_REDIRECT to
                            ProtoWriter().string(1, "https://evil.test/steal").toByteArray()
                    )
                )
            )
        )
        val source = SabrDataSource(factory)
        source.open(DataSpec(Uri.parse(spec().toUri())))

        val failure = runCatching { source.readFully() }.exceptionOrNull()
        source.close()

        assertTrue(failure is SabrProtocolException)
        assertEquals(1, factory.requests.size)
    }

    @Test
    fun anUnreachableEndpointFailsOverToTheDeclaredMediaNetwork() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.openFailure(IOException("connection reset")),
                Response.media(listOf(chunk(0, 0L, initSegment), chunk(1, 1_019L, firstSegment))),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)

        source.open(DataSpec(Uri.parse(spec().toUri())))
        val read = source.readFully()
        source.close()

        assertArrayEquals(stream, read)
        assertTrue(factory.requests[0].contains("sn-a"))
        assertTrue(factory.requests[1].contains("sn-b"))
    }

    @Test
    fun aSecurityAnswerIsNeverRetriedOnAnotherMediaNetwork() {
        val forbidden = HttpDataSource.InvalidResponseCodeException(
            403,
            "Forbidden",
            null,
            emptyMap(),
            DataSpec(Uri.parse(endpoint)),
            ByteArray(0)
        )
        val factory = ScriptedHttpDataSourceFactory(listOf(Response.openFailure(forbidden)))
        val source = SabrDataSource(factory)
        source.open(DataSpec(Uri.parse(spec().toUri())))

        val failure = runCatching { source.readFully() }.exceptionOrNull()
        source.close()

        assertTrue(failure is HttpDataSource.InvalidResponseCodeException)
        assertEquals(1, factory.requests.size)
    }

    @Test
    fun aServerErrorFrameStopsTheSessionForTheRecoveryLadder() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(Response.parts(listOf(SabrPart.SABR_ERROR to ByteArray(4))))
        )
        val source = SabrDataSource(factory)
        source.open(DataSpec(Uri.parse(spec().toUri())))

        val failure = runCatching { source.readFully() }.exceptionOrNull()
        source.close()

        assertTrue(failure is SabrProtocolException)
    }

    @Test
    fun mediaBelongingToAnotherFormatIsDiscarded() {
        val factory = ScriptedHttpDataSourceFactory(
            listOf(
                Response.media(
                    listOf(
                        chunk(0, 0L, ByteArray(64), itag = 133),
                        chunk(1, 0L, initSegment),
                        chunk(2, 1_019L, firstSegment)
                    )
                ),
                Response.media(listOf(chunk(0, 5_019L, secondSegment)))
            )
        )
        val source = SabrDataSource(factory)

        source.open(DataSpec(Uri.parse(spec().toUri())))
        val read = source.readFully()
        source.close()

        assertArrayEquals(stream, read)
    }

    private fun SabrDataSource.readFully(): ByteArray {
        val output = ArrayList<Byte>()
        val buffer = ByteArray(1_024)
        while (true) {
            val count = read(buffer, 0, buffer.size)
            if (count == C.RESULT_END_OF_INPUT) break
            for (index in 0 until count) output.add(buffer[index])
        }
        return output.toByteArray()
    }

    private fun spec() = SabrStreamSpec(
        endpointUrl = endpoint,
        ustreamerConfig = byteArrayOf(1, 2, 3),
        format = SabrFormatId(itag, lastModified),
        companionAudioFormat = null,
        contentLength = stream.size.toLong(),
        durationMs = 213_090L,
        videoTrack = false,
        clientName = 5,
        clientVersion = "20.10.4",
        userAgent = "com.google.ios.youtube/20.10.4"
    )

    private fun chunk(
        headerId: Int,
        startDataRange: Long,
        payload: ByteArray,
        itag: Int = this.itag
    ): Pair<ByteArray, ByteArray> {
        val header = ProtoWriter()
            .varint(1, headerId.toLong())
            .varint(3, itag.toLong())
            .varint(4, lastModified)
            .varint(6, startDataRange)
            .varint(14, payload.size.toLong())
            .toByteArray()
        return header to (byteArrayOf(headerId.toByte()) + payload)
    }

    private class Response private constructor(
        val body: ByteArray,
        val openFailure: IOException?
    ) {
        companion object {
            fun parts(parts: List<Pair<Int, ByteArray>>): Response {
                var body = ByteArray(0)
                parts.forEach { (type, payload) -> body += umpPart(type, payload) }
                return Response(body, null)
            }

            fun media(chunks: List<Pair<ByteArray, ByteArray>>): Response = parts(
                chunks.flatMap { (header, payload) ->
                    listOf(SabrPart.MEDIA_HEADER to header, SabrPart.MEDIA to payload)
                }
            )

            fun openFailure(error: IOException): Response = Response(ByteArray(0), error)
        }
    }

    private class ScriptedHttpDataSourceFactory(
        private val responses: List<Response>
    ) : HttpDataSource.Factory {
        val requests = ArrayList<String>()

        override fun createDataSource(): HttpDataSource = ScriptedHttpDataSource(this)

        override fun setDefaultRequestProperties(
            defaultRequestProperties: MutableMap<String, String>
        ): HttpDataSource.Factory = this

        fun next(url: String): Response {
            val index = requests.size
            requests += url
            return responses.getOrElse(index) { Response.media(emptyList()) }
        }
    }

    private class ScriptedHttpDataSource(
        private val factory: ScriptedHttpDataSourceFactory
    ) : HttpDataSource {
        private var body: ByteArray = ByteArray(0)
        private var position = 0

        override fun open(dataSpec: DataSpec): Long {
            val response = factory.next(dataSpec.uri.toString())
            response.openFailure?.let { throw it }
            body = response.body
            position = 0
            return body.size.toLong()
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (position >= body.size) return C.RESULT_END_OF_INPUT
            val count = minOf(length, body.size - position)
            System.arraycopy(body, position, buffer, offset, count)
            position += count
            return count
        }

        override fun close() {
            body = ByteArray(0)
            position = 0
        }

        override fun getUri(): Uri? = null

        override fun addTransferListener(transferListener: TransferListener) = Unit

        override fun getResponseHeaders(): MutableMap<String, MutableList<String>> = mutableMapOf()

        override fun setRequestProperty(name: String, value: String) = Unit

        override fun clearRequestProperty(name: String) = Unit

        override fun clearAllRequestProperties() = Unit

        override fun getResponseCode(): Int = 200
    }
}
