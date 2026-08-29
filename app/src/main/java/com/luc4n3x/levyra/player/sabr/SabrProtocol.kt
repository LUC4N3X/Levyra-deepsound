package com.luc4n3x.levyra.player.sabr

/** UMP part types Levyra acts on. Everything else is skipped without inspection. */
internal object SabrPart {
    const val MEDIA_HEADER = 20
    const val MEDIA = 21
    const val MEDIA_END = 22
    const val SABR_REDIRECT = 43
    const val SABR_ERROR = 44
    const val RELOAD_PLAYER_RESPONSE = 45
}

/**
 * Identifies one media format inside a SABR session. lastModified is the discriminator YouTube uses
 * when the same itag is re-encoded, so it must travel with the itag everywhere.
 */
internal data class SabrFormatId(val itag: Int, val lastModified: Long) {
    fun write(): ProtoWriter = ProtoWriter().varint(1, itag.toLong()).varint(2, lastModified)
}

/** The header that precedes every media payload, describing where it belongs in the byte stream. */
internal data class SabrMediaHeader(
    val headerId: Int,
    val itag: Int,
    val lastModified: Long,
    val startDataRange: Long,
    val contentLength: Long,
    val sequenceNumber: Long,
    val durationMs: Long,
    val isInitSegment: Boolean
)

internal data class SabrBufferedRange(
    val formatId: SabrFormatId,
    val startTimeMs: Long,
    val durationMs: Long,
    val startSegmentIndex: Long,
    val endSegmentIndex: Long
) {
    fun write(): ProtoWriter = ProtoWriter()
        .message(1, formatId.write())
        .varint(2, startTimeMs)
        .varint(3, durationMs)
        .varint(4, startSegmentIndex)
        .varint(5, endSegmentIndex)
}

internal object SabrMessages {
    const val TRACK_TYPES_AUDIO_AND_VIDEO = 0L
    const val TRACK_TYPES_AUDIO_ONLY = 1L

    private const val FIELD_CLIENT_ABR_STATE = 1
    private const val FIELD_SELECTED_FORMAT_IDS = 2
    private const val FIELD_BUFFERED_RANGES = 3
    private const val FIELD_USTREAMER_CONFIG = 5
    private const val FIELD_PREFERRED_AUDIO_FORMAT_IDS = 16
    private const val FIELD_PREFERRED_VIDEO_FORMAT_IDS = 17
    private const val FIELD_STREAMER_CONTEXT = 19

    private const val ABR_FIELD_PLAYER_TIME_MS = 28
    private const val ABR_FIELD_ENABLED_TRACK_TYPES = 40

    private const val CONTEXT_FIELD_CLIENT_INFO = 1
    private const val CLIENT_INFO_FIELD_NAME = 16
    private const val CLIENT_INFO_FIELD_VERSION = 17

    private const val MEDIA_HEADER_FIELD_ID = 1
    private const val MEDIA_HEADER_FIELD_ITAG = 3
    private const val MEDIA_HEADER_FIELD_LAST_MODIFIED = 4
    private const val MEDIA_HEADER_FIELD_START_DATA_RANGE = 6
    private const val MEDIA_HEADER_FIELD_IS_INIT_SEGMENT = 8
    private const val MEDIA_HEADER_FIELD_SEQUENCE_NUMBER = 9
    private const val MEDIA_HEADER_FIELD_DURATION_MS = 10
    private const val MEDIA_HEADER_FIELD_CONTENT_LENGTH = 14

    private const val REDIRECT_FIELD_URL = 1

    /**
     * Builds one VideoPlaybackAbrRequest. [alreadyBuffered] lets a video session declare the paired
     * audio format as fully buffered, which is how the server is told to stop sending audio bytes the
     * audio session already owns.
     */
    fun playbackRequest(
        ustreamerConfig: ByteArray,
        playerTimeMs: Long,
        enabledTrackTypes: Long,
        preferredAudio: SabrFormatId?,
        preferredVideo: SabrFormatId?,
        initializedFormats: List<SabrFormatId>,
        alreadyBuffered: List<SabrBufferedRange>,
        clientName: Int,
        clientVersion: String
    ): ByteArray {
        val request = ProtoWriter()
            .message(
                FIELD_CLIENT_ABR_STATE,
                ProtoWriter()
                    .varint(ABR_FIELD_PLAYER_TIME_MS, playerTimeMs.coerceAtLeast(0L))
                    .varint(ABR_FIELD_ENABLED_TRACK_TYPES, enabledTrackTypes)
            )
        initializedFormats.forEach { request.message(FIELD_SELECTED_FORMAT_IDS, it.write()) }
        alreadyBuffered.forEach { request.message(FIELD_BUFFERED_RANGES, it.write()) }
        request.bytes(FIELD_USTREAMER_CONFIG, ustreamerConfig)
        preferredAudio?.let { request.message(FIELD_PREFERRED_AUDIO_FORMAT_IDS, it.write()) }
        preferredVideo?.let { request.message(FIELD_PREFERRED_VIDEO_FORMAT_IDS, it.write()) }
        request.message(
            FIELD_STREAMER_CONTEXT,
            ProtoWriter().message(
                CONTEXT_FIELD_CLIENT_INFO,
                ProtoWriter()
                    .varint(CLIENT_INFO_FIELD_NAME, clientName.toLong())
                    .string(CLIENT_INFO_FIELD_VERSION, clientVersion)
            )
        )
        return request.toByteArray()
    }

    fun parseMediaHeader(data: ByteArray, length: Int): SabrMediaHeader {
        var headerId = 0
        var itag = 0
        var lastModified = 0L
        var startDataRange = 0L
        var contentLength = 0L
        var sequenceNumber = 0L
        var durationMs = 0L
        var isInitSegment = false
        val reader = ProtoReader(data, 0, length)
        while (reader.next()) {
            if (reader.wireType != PROTO_WIRE_VARINT) {
                reader.skipValue()
                continue
            }
            when (reader.field) {
                MEDIA_HEADER_FIELD_ID -> headerId = reader.readVarintValue().toInt()
                MEDIA_HEADER_FIELD_ITAG -> itag = reader.readVarintValue().toInt()
                MEDIA_HEADER_FIELD_LAST_MODIFIED -> lastModified = reader.readVarintValue()
                MEDIA_HEADER_FIELD_START_DATA_RANGE -> startDataRange = reader.readVarintValue()
                MEDIA_HEADER_FIELD_IS_INIT_SEGMENT -> isInitSegment = reader.readVarintValue() != 0L
                MEDIA_HEADER_FIELD_SEQUENCE_NUMBER -> sequenceNumber = reader.readVarintValue()
                MEDIA_HEADER_FIELD_DURATION_MS -> durationMs = reader.readVarintValue()
                MEDIA_HEADER_FIELD_CONTENT_LENGTH -> contentLength = reader.readVarintValue()
                else -> reader.skipValue()
            }
        }
        if (headerId < 0 || startDataRange < 0L || contentLength < 0L) {
            throw SabrProtocolException("SABR media header out of range")
        }
        return SabrMediaHeader(
            headerId = headerId,
            itag = itag,
            lastModified = lastModified,
            startDataRange = startDataRange,
            contentLength = contentLength,
            sequenceNumber = sequenceNumber,
            durationMs = durationMs,
            isInitSegment = isInitSegment
        )
    }

    fun parseRedirectUrl(data: ByteArray, length: Int): String? {
        val reader = ProtoReader(data, 0, length)
        while (reader.next()) {
            if (reader.field == REDIRECT_FIELD_URL && reader.wireType == PROTO_WIRE_LENGTH_DELIMITED) {
                return reader.readStringValue().takeIf { it.isNotBlank() }
            }
            reader.skipValue()
        }
        return null
    }

    /** The first byte of a media part selects the header it continues. */
    fun mediaHeaderId(data: ByteArray, length: Int): Int {
        if (length <= 0) throw SabrProtocolException("empty SABR media part")
        return data[0].toInt() and 0xFF
    }
}
