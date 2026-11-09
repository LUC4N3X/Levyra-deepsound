package com.luc4n3x.levyra.data.hqaudio.tidal

import java.io.ByteArrayInputStream
import javax.xml.parsers.DocumentBuilderFactory
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.w3c.dom.Element

internal data class TidalDashManifest(
    val duration: String,
    val timescale: Long,
    val initialization: HttpUrl,
    val mediaTemplate: String,
    val startNumber: Int,
    val timeline: List<Pair<Long, Int>>,
    val sampleRateHz: Int,
    val bandwidth: Int,
    val channels: Int,
    val bitDepth: Int = 0
) {
    val segmentCount: Int get() = timeline.sumOf { it.second + 1 }

    val durationMs: Long get() = timeline.sumOf { (length, repeat) -> length * (repeat + 1) } * 1_000L / timescale

    fun segmentUrls(): List<HttpUrl> = (startNumber until startNumber + segmentCount).map { number ->
        mediaTemplate.replace(NUMBER_TOKEN, number.toString()).toHttpUrl()
    }

    fun proxiedThrough(proxy: HttpUrl): TidalDashManifest = copy(
        initialization = TidalMediaProxy.wrap(proxy, initialization.toString()).toHttpUrl(),
        mediaTemplate = mediaTemplate.split(NUMBER_TOKEN).joinToString(NUMBER_TOKEN) { part ->
            java.net.URLEncoder.encode(part, Charsets.UTF_8.name())
        }.let { TidalMediaProxy.prefix(proxy) + it }
    )

    fun toMpd(): ByteArray = buildString {
        append("<?xml version=\"1.0\" encoding=\"UTF-8\"?>")
        append("<MPD xmlns=\"urn:mpeg:dash:schema:mpd:2011\" profiles=\"urn:mpeg:dash:profile:isoff-main:2011\" type=\"static\" ")
        append("minBufferTime=\"PT2S\" mediaPresentationDuration=\"").append(duration).append("\"><Period id=\"0\">")
        append("<AdaptationSet id=\"0\" contentType=\"audio\" mimeType=\"audio/mp4\" segmentAlignment=\"true\">")
        append("<Representation id=\"FLAC,").append(sampleRateHz).append(',').append(bitDepth)
        append("\" codecs=\"flac\" bandwidth=\"").append(bandwidth)
        append("\" audioSamplingRate=\"").append(sampleRateHz).append("\">")
        append("<AudioChannelConfiguration schemeIdUri=\"urn:mpeg:dash:23003:3:audio_channel_configuration:2011\" value=\"")
        append(channels).append("\"/>")
        append("<SegmentTemplate timescale=\"").append(timescale).append("\" initialization=\"").append(escape(initialization.toString()))
        append("\" media=\"").append(escape(mediaTemplate)).append("\" startNumber=\"").append(startNumber).append("\"><SegmentTimeline>")
        timeline.forEach { (length, repeat) ->
            append("<S d=\"").append(length).append('"')
            if (repeat > 0) append(" r=\"").append(repeat).append('"')
            append("/>")
        }
        append("</SegmentTimeline></SegmentTemplate></Representation></AdaptationSet></Period></MPD>")
    }.toByteArray(Charsets.UTF_8)

    companion object {
        const val NUMBER_TOKEN = "\$Number\$"
        private const val MAX_BYTES = 131_072
        private const val MAX_SEGMENTS = 10_000
        private val forbidden = setOf("ContentProtection", "BaseURL", "Location", "UTCTiming")
        private val isoDuration = Regex("^PT(?:\\d{1,3}H)?(?:\\d{1,4}M)?(?:\\d{1,5}(?:\\.\\d{1,6})?S)?$")

        fun parse(bytes: ByteArray): TidalDashManifest {
            require(bytes.size in 1..MAX_BYTES)
            val text = bytes.toString(Charsets.UTF_8)
            require(!text.contains("<!DOCTYPE", true) && !text.contains("<!ENTITY", true))
            val factory = DocumentBuilderFactory.newInstance().apply {
                isNamespaceAware = false
                isExpandEntityReferences = false
            }
            val root = factory.newDocumentBuilder().parse(ByteArrayInputStream(bytes)).documentElement
            require(root.tagName == "MPD" && root.getAttribute("type") == "static")
            forbidden.forEach { require(root.getElementsByTagName(it).length == 0) }
            val duration = root.getAttribute("mediaPresentationDuration").takeIf { it != "PT" && isoDuration.matches(it) }
                ?: throw IllegalArgumentException("duration")
            val representation = root.elements("Representation").filter { it.getAttribute("codecs") == "flac" }
                .maxByOrNull { it.getAttribute("bandwidth").toIntOrNull() ?: 0 } ?: throw IllegalArgumentException("flac")
            val template = representation.elements("SegmentTemplate").singleOrNull() ?: throw IllegalArgumentException("template")
            val initialization = template.getAttribute("initialization").toHttpUrl()
            val media = template.getAttribute("media")
            require(media.contains(NUMBER_TOKEN) && allowsSegment(initialization) &&
                allowsSegment(media.replace(NUMBER_TOKEN, "1").toHttpUrl()))
            val timeline = template.elements("S").map { segment ->
                val length = segment.getAttribute("d").toLong()
                val repeat = segment.getAttribute("r").ifEmpty { "0" }.toInt()
                require(length > 0L && repeat in 0..MAX_SEGMENTS && !segment.hasAttribute("t"))
                length to repeat
            }
            val channels = representation.elements("AudioChannelConfiguration").firstOrNull()
                ?.getAttribute("value")?.toIntOrNull() ?: 2
            return TidalDashManifest(
                duration = duration,
                timescale = template.getAttribute("timescale").toLong().also { require(it in 1L..1_000_000L) },
                initialization = initialization,
                mediaTemplate = media,
                startNumber = template.getAttribute("startNumber").ifEmpty { "1" }.toInt().also { require(it in 0..1) },
                timeline = timeline,
                sampleRateHz = representation.getAttribute("audioSamplingRate").toInt().also { require(it in 8_000..384_000) },
                bandwidth = representation.getAttribute("bandwidth").toInt().also { require(it > 0) },
                channels = channels.also { require(it in 1..8) },
                bitDepth = representation.getAttribute("id").split(',').getOrNull(2)?.trim()?.toIntOrNull()
                    ?.takeIf { it in 8..32 } ?: 0
            ).also { require(it.segmentCount in 1..MAX_SEGMENTS) }
        }

        private fun allowsSegment(url: HttpUrl) = TidalNetwork.allowsMedia(url) || TidalMediaProxy.accepts(url)

        private fun Element.elements(name: String): List<Element> {
            val nodes = getElementsByTagName(name)
            return (0 until nodes.length).map { nodes.item(it) as Element }
        }

        private fun escape(value: String) = value.replace("&", "&amp;").replace("\"", "&quot;").replace("<", "&lt;")
    }
}
