package com.luc4n3x.levyra.player.sabr

import android.net.Uri
import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.BaseDataSource
import androidx.media3.datasource.DataSource
import androidx.media3.datasource.DataSpec
import androidx.media3.datasource.HttpDataSource
import androidx.media3.datasource.HttpDataSource.InvalidResponseCodeException
import com.luc4n3x.levyra.player.GooglevideoMediaNetwork
import timber.log.Timber
import java.io.IOException
import java.io.InputStream

@UnstableApi
internal class SabrDataSource(
    private val httpDataSourceFactory: HttpDataSource.Factory,
    private val maxRedirects: Int = MAX_REDIRECTS,
    private val maxAlternateEndpoints: Int = GooglevideoMediaNetwork.MAX_ALTERNATE_CANDIDATES
) : BaseDataSource(true) {

    class Factory(
        private val httpDataSourceFactory: HttpDataSource.Factory
    ) : DataSource.Factory {
        override fun createDataSource(): DataSource = SabrDataSource(httpDataSourceFactory)
    }

    private var assembler: SabrSegmentAssembler? = null

    private var uri: Uri? = null
    private var spec: SabrStreamSpec? = null
    private var endpointUrl: String = ""
    private var redirectsUsed = 0
    private var endpointFailoversUsed = 0
    private var position = 0L
    private var bytesRemaining = 0L
    private var opened = false

    private var httpDataSource: HttpDataSource? = null
    private var umpReader: UmpReader? = null
    private var unproductiveRequests = 0
    private var producedReadableBytes = false
    private var pendingOffset = 0
    private var pendingRemaining = 0

    override fun open(dataSpec: DataSpec): Long {
        val parsed = SabrStreamSpec.parse(dataSpec.uri.toString())
            ?: throw SabrProtocolException("unsupported SABR stream descriptor")
        transferInitializing(dataSpec)
        spec = parsed
        assembler = SabrSegmentAssembler(parsed.format.itag)
        uri = dataSpec.uri
        endpointUrl = parsed.endpointUrl
        redirectsUsed = 0
        endpointFailoversUsed = 0
        unproductiveRequests = 0
        producedReadableBytes = false
        position = dataSpec.position
        val available = (parsed.contentLength - position).coerceAtLeast(0L)
        bytesRemaining = if (dataSpec.length == C.LENGTH_UNSET.toLong()) {
            available
        } else {
            minOf(dataSpec.length, available)
        }
        opened = true
        transferStarted(dataSpec)
        return bytesRemaining
    }

    override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
        if (length == 0) return 0
        if (bytesRemaining <= 0L) return C.RESULT_END_OF_INPUT
        while (pendingRemaining <= 0) {
            if (!advance()) return C.RESULT_END_OF_INPUT
        }
        val payload = umpReader?.partPayload ?: return C.RESULT_END_OF_INPUT
        val count = minOf(length.toLong(), pendingRemaining.toLong(), bytesRemaining).toInt()
        System.arraycopy(payload, pendingOffset, buffer, offset, count)
        pendingOffset += count
        pendingRemaining -= count
        position += count
        bytesRemaining -= count
        bytesTransferred(count)
        return count
    }

    override fun getUri(): Uri? = uri

    override fun close() {
        val wasOpen = opened
        opened = false
        closeResponse()
        assembler = null
        spec = null
        uri = null
        endpointUrl = ""
        position = 0L
        bytesRemaining = 0L
        if (wasOpen) transferEnded()
    }

    private fun advance(): Boolean {
        val current = spec ?: return false
        while (true) {
            if (bytesRemaining <= 0L || position >= current.contentLength) return false
            val reader = umpReader ?: run {
                openResponse(
                    current,
                    sabrPlayerTimeMsFor(
                        contentLength = current.contentLength,
                        durationMs = current.durationMs,
                        position = position,
                        unproductiveAttempts = unproductiveRequests
                    )
                )
                umpReader
            } ?: return false

            val hasPart = try {
                reader.next()
            } catch (error: IOException) {
                closeResponse()
                throw error
            }
            if (!hasPart) {
                closeResponse()
                if (position >= current.contentLength) return false
                if (!producedReadableBytes) {
                    unproductiveRequests++
                    if (unproductiveRequests > MAX_UNPRODUCTIVE_REQUESTS) {
                        throw SabrProtocolException("SABR stopped delivering data before end of stream")
                    }
                } else {
                    unproductiveRequests = 0
                }
                continue
            }

            when (reader.partType) {
                SabrPart.MEDIA_HEADER -> onMediaHeader(reader)
                SabrPart.MEDIA -> if (onMedia(reader)) return true
                SabrPart.SABR_REDIRECT -> onRedirect(reader)
                SabrPart.SABR_ERROR ->
                    throw SabrProtocolException("SABR server reported a stream error")
                SabrPart.RELOAD_PLAYER_RESPONSE ->
                    throw SabrProtocolException("SABR requires a fresh player response")
                else -> Unit
            }
        }
    }

    private fun onMediaHeader(reader: UmpReader) {
        assembler?.onMediaHeader(SabrMessages.parseMediaHeader(reader.partPayload, reader.partLength))
    }

    private fun onMedia(reader: UmpReader): Boolean {
        val window = assembler?.onMedia(
            headerId = SabrMessages.mediaHeaderId(reader.partPayload, reader.partLength),
            payloadLength = reader.partLength - SabrSegmentAssembler.MEDIA_PAYLOAD_OFFSET,
            position = position
        ) ?: return false
        pendingOffset = window.offset
        pendingRemaining = window.length
        producedReadableBytes = true
        return true
    }

    private fun onRedirect(reader: UmpReader) {
        val target = SabrMessages.parseRedirectUrl(reader.partPayload, reader.partLength)
        if (target == null || !SabrEndpoint.isAllowed(target)) {
            throw SabrProtocolException("SABR redirect rejected")
        }
        if (redirectsUsed >= maxRedirects) {
            throw SabrProtocolException("SABR redirect budget exhausted")
        }
        redirectsUsed++
        endpointUrl = target
        closeResponse()
    }

    private fun openResponse(current: SabrStreamSpec, playerTimeMs: Long) {
        var failure: IOException? = null
        for (candidate in endpointCandidates()) {
            try {
                startRequest(current, candidate, playerTimeMs)
                if (candidate != endpointUrl) {
                    endpointFailoversUsed++
                    endpointUrl = candidate
                    Timber.i("SABR endpoint failover applied")
                }
                return
            } catch (error: IOException) {
                closeResponse()
                if (!isEndpointFailure(error)) throw error
                failure = error
            }
        }
        throw failure ?: SabrProtocolException("SABR endpoint unavailable")
    }

    private fun endpointCandidates(): List<String> {
        val remaining = (maxAlternateEndpoints - endpointFailoversUsed).coerceAtLeast(0)
        if (remaining == 0) return listOf(endpointUrl)
        return listOf(endpointUrl) +
            GooglevideoMediaNetwork.alternateUrls(endpointUrl, remaining).filter(SabrEndpoint::isAllowed)
    }

    private fun startRequest(current: SabrStreamSpec, endpoint: String, playerTimeMs: Long) {
        val body = SabrMessages.playbackRequest(
            ustreamerConfig = current.ustreamerConfig,
            playerTimeMs = playerTimeMs,
            enabledTrackTypes = if (current.videoTrack) {
                SabrMessages.TRACK_TYPES_AUDIO_AND_VIDEO
            } else {
                SabrMessages.TRACK_TYPES_AUDIO_ONLY
            },
            preferredAudio = if (current.videoTrack) current.companionAudioFormat else current.format,
            preferredVideo = current.format.takeIf { current.videoTrack },
            initializedFormats = initializedFormats(current),
            alreadyBuffered = suppressedCompanionRanges(current),
            clientName = current.clientName,
            clientVersion = current.clientVersion
        )
        val request = DataSpec.Builder()
            .setUri(endpoint)
            .setHttpMethod(DataSpec.HTTP_METHOD_POST)
            .setHttpBody(body)
            .setHttpRequestHeaders(
                buildMap {
                    put("Content-Type", "application/x-protobuf")
                    put("Accept", "*/*")
                    put("Accept-Encoding", "identity")
                    current.userAgent.takeIf { it.isNotBlank() }?.let { put("User-Agent", it) }
                }
            )
            .build()
        val source = httpDataSourceFactory.createDataSource()
        source.open(request)
        httpDataSource = source
        umpReader = UmpReader(HttpDataSourceInputStream(source))
        assembler?.reset()
        producedReadableBytes = false
        pendingOffset = 0
        pendingRemaining = 0
    }

    private fun suppressedCompanionRanges(current: SabrStreamSpec): List<SabrBufferedRange> {
        val companionAudio = current.companionAudioFormat?.takeIf { current.videoTrack } ?: return emptyList()
        return listOf(
            SabrBufferedRange(
                formatId = companionAudio,
                startTimeMs = 0L,
                durationMs = current.durationMs,
                startSegmentIndex = 1L,
                endSegmentIndex = MAX_SUPPRESSED_SEGMENT_INDEX
            )
        )
    }

    private fun initializedFormats(current: SabrStreamSpec): List<SabrFormatId> {
        val companionAudio = current.companionAudioFormat?.takeIf { current.videoTrack }
        val skipInitSegment = position > 0L
        return when {
            companionAudio != null && skipInitSegment -> listOf(current.format, companionAudio)
            companionAudio != null -> listOf(companionAudio)
            skipInitSegment -> listOf(current.format)
            else -> emptyList()
        }
    }

    private fun isEndpointFailure(error: IOException): Boolean {
        val responseCode = generateSequence(error as Throwable) { it.cause }
            .filterIsInstance<InvalidResponseCodeException>()
            .firstOrNull()
            ?.responseCode
        return GooglevideoMediaNetwork.isEndpointFailure(responseCode)
    }

    private fun closeResponse() {
        umpReader = null
        pendingOffset = 0
        pendingRemaining = 0
        val source = httpDataSource
        httpDataSource = null
        if (source != null) {
            try {
                source.close()
            } catch (error: IOException) {
                Timber.d(error, "SABR response close failed")
            }
        }
    }

    private class HttpDataSourceInputStream(private val source: HttpDataSource) : InputStream() {
        private val single = ByteArray(1)

        override fun read(): Int {
            val count = read(single, 0, 1)
            return if (count == -1) -1 else single[0].toInt() and 0xFF
        }

        override fun read(buffer: ByteArray, offset: Int, length: Int): Int {
            if (length == 0) return 0
            val count = source.read(buffer, offset, length)
            return if (count == C.RESULT_END_OF_INPUT) -1 else count
        }
    }

    private companion object {
        const val MAX_REDIRECTS = 3
        const val MAX_UNPRODUCTIVE_REQUESTS = 2
        const val MAX_SUPPRESSED_SEGMENT_INDEX = 1_000_000L
    }
}
