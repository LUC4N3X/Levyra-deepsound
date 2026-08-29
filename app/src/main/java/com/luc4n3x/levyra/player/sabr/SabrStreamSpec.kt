package com.luc4n3x.levyra.player.sabr

import java.util.Base64

internal data class SabrStreamSpec(
    val endpointUrl: String,
    val ustreamerConfig: ByteArray,
    val format: SabrFormatId,
    val companionAudioFormat: SabrFormatId?,
    val contentLength: Long,
    val durationMs: Long,
    val videoTrack: Boolean,
    val clientName: Int,
    val clientVersion: String,
    val userAgent: String
) {
    fun toUri(): String = SCHEME_PREFIX + ENCODER.encodeToString(
        buildString {
            appendField(FIELD_ENDPOINT, endpointUrl)
            appendField(FIELD_USTREAMER, ENCODER.encodeToString(ustreamerConfig))
            appendField(FIELD_ITAG, format.itag.toString())
            appendField(FIELD_LAST_MODIFIED, format.lastModified.toString())
            companionAudioFormat?.let {
                appendField(FIELD_AUDIO_ITAG, it.itag.toString())
                appendField(FIELD_AUDIO_LAST_MODIFIED, it.lastModified.toString())
            }
            appendField(FIELD_CONTENT_LENGTH, contentLength.toString())
            appendField(FIELD_DURATION_MS, durationMs.toString())
            appendField(FIELD_VIDEO, if (videoTrack) "1" else "0")
            appendField(FIELD_CLIENT_NAME, clientName.toString())
            appendField(FIELD_CLIENT_VERSION, clientVersion)
            appendField(FIELD_USER_AGENT, userAgent)
        }.toByteArray(Charsets.UTF_8)
    )

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is SabrStreamSpec) return false
        return endpointUrl == other.endpointUrl &&
            ustreamerConfig.contentEquals(other.ustreamerConfig) &&
            format == other.format &&
            companionAudioFormat == other.companionAudioFormat &&
            contentLength == other.contentLength &&
            durationMs == other.durationMs &&
            videoTrack == other.videoTrack &&
            clientName == other.clientName &&
            clientVersion == other.clientVersion &&
            userAgent == other.userAgent
    }

    override fun hashCode(): Int {
        var result = endpointUrl.hashCode()
        result = 31 * result + ustreamerConfig.contentHashCode()
        result = 31 * result + format.hashCode()
        result = 31 * result + (companionAudioFormat?.hashCode() ?: 0)
        result = 31 * result + contentLength.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + videoTrack.hashCode()
        result = 31 * result + clientName
        result = 31 * result + clientVersion.hashCode()
        result = 31 * result + userAgent.hashCode()
        return result
    }

    companion object {
        const val SCHEME_PREFIX = "levyra-sabr://s/"

        private const val FIELD_ENDPOINT = "endpoint"
        private const val FIELD_USTREAMER = "ustreamer"
        private const val FIELD_ITAG = "itag"
        private const val FIELD_LAST_MODIFIED = "lmt"
        private const val FIELD_AUDIO_ITAG = "aitag"
        private const val FIELD_AUDIO_LAST_MODIFIED = "almt"
        private const val FIELD_CONTENT_LENGTH = "clen"
        private const val FIELD_DURATION_MS = "dur"
        private const val FIELD_VIDEO = "video"
        private const val FIELD_CLIENT_NAME = "cname"
        private const val FIELD_CLIENT_VERSION = "cver"
        private const val FIELD_USER_AGENT = "cua"
        private const val MAX_ENCODED_LENGTH = 32 * 1024

        private val ENCODER: Base64.Encoder = Base64.getUrlEncoder().withoutPadding()
        private val DECODER: Base64.Decoder = Base64.getUrlDecoder()

        fun isSabrUri(uri: String): Boolean = uri.startsWith(SCHEME_PREFIX)

        fun parse(uri: String): SabrStreamSpec? {
            if (!isSabrUri(uri)) return null
            val payload = uri.substring(SCHEME_PREFIX.length)
            val encoded = payload.substringBefore('?').substringBefore('#')
            if (encoded.isEmpty() || encoded.length > MAX_ENCODED_LENGTH) return null
            val decoded = runCatching {
                String(DECODER.decode(encoded), Charsets.UTF_8)
            }.getOrNull() ?: return null

            val fields = HashMap<String, String>()
            decoded.lineSequence().forEach { line ->
                val separator = line.indexOf('=')
                if (separator > 0) fields[line.substring(0, separator)] = line.substring(separator + 1)
            }

            val endpoint = fields[FIELD_ENDPOINT].orEmpty()
            if (!SabrEndpoint.isAllowed(endpoint)) return null
            val ustreamer = runCatching {
                DECODER.decode(fields[FIELD_USTREAMER].orEmpty())
            }.getOrNull() ?: return null
            if (ustreamer.isEmpty()) return null

            val itag = fields[FIELD_ITAG]?.toIntOrNull() ?: return null
            val lastModified = fields[FIELD_LAST_MODIFIED]?.toLongOrNull() ?: return null
            val contentLength = fields[FIELD_CONTENT_LENGTH]?.toLongOrNull() ?: return null
            val durationMs = fields[FIELD_DURATION_MS]?.toLongOrNull() ?: return null
            if (itag <= 0 || contentLength <= 0L || durationMs <= 0L) return null

            val audioItag = fields[FIELD_AUDIO_ITAG]?.toIntOrNull()
            val audioLastModified = fields[FIELD_AUDIO_LAST_MODIFIED]?.toLongOrNull()
            val companionAudio = if (audioItag != null && audioItag > 0 && audioLastModified != null) {
                SabrFormatId(audioItag, audioLastModified)
            } else {
                null
            }

            return SabrStreamSpec(
                endpointUrl = endpoint,
                ustreamerConfig = ustreamer,
                format = SabrFormatId(itag, lastModified),
                companionAudioFormat = companionAudio,
                contentLength = contentLength,
                durationMs = durationMs,
                videoTrack = fields[FIELD_VIDEO] == "1",
                clientName = fields[FIELD_CLIENT_NAME]?.toIntOrNull() ?: 0,
                clientVersion = fields[FIELD_CLIENT_VERSION].orEmpty(),
                userAgent = fields[FIELD_USER_AGENT].orEmpty()
            )
        }

        private fun StringBuilder.appendField(name: String, value: String) {
            if (value.any { it == '\n' || it == '\r' }) return
            if (isNotEmpty()) append('\n')
            append(name).append('=').append(value)
        }
    }
}

internal object SabrEndpoint {
    private const val HTTPS_PREFIX = "https://"
    private const val GOOGLEVIDEO_SUFFIX = ".googlevideo.com"
    private const val MAX_URL_LENGTH = 8 * 1024

    fun isAllowed(url: String): Boolean {
        if (url.length <= HTTPS_PREFIX.length || url.length > MAX_URL_LENGTH) return false
        if (!url.startsWith(HTTPS_PREFIX)) return false
        var authorityEnd = url.length
        for (index in HTTPS_PREFIX.length until url.length) {
            val character = url[index]
            if (character == '/' || character == '?' || character == '#') {
                authorityEnd = index
                break
            }
        }
        val authority = url.substring(HTTPS_PREFIX.length, authorityEnd)
        if (authority.isEmpty() || authority.contains('@') || authority.contains(':')) return false
        val host = authority.lowercase().trimEnd('.')
        return host.endsWith(GOOGLEVIDEO_SUFFIX) && host.length > GOOGLEVIDEO_SUFFIX.length
    }
}
