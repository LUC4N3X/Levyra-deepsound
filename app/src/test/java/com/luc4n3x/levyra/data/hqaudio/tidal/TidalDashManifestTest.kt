package com.luc4n3x.levyra.data.hqaudio.tidal

import com.luc4n3x.levyra.data.hqaudio.FlacStreamInfo
import com.luc4n3x.levyra.player.offline.TidalDashFlac
import java.io.ByteArrayOutputStream
import java.io.IOException
import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

class TidalDashManifestTest {
    private val base = "https://sp-ad-fa.audio.tidal.com/mediatracks/GisIAx"

    private fun representation(id: String, codecs: String, bandwidth: Int, tag: String = id) =
        """<Representation id="$tag" codecs="$codecs" bandwidth="$bandwidth" audioSamplingRate="44100">""" +
            """<AudioChannelConfiguration schemeIdUri="urn:mpeg:dash:23003:3:audio_channel_configuration:2011" value="2"/>""" +
            """<SegmentTemplate timescale="44100" initialization="$base/$id/0.mp4?sig=1~abc&amp;info=x" media="$base/$id/${'$'}Number${'$'}.mp4?sig=1~abc&amp;info=x" startNumber="1">""" +
            """<SegmentTimeline><S d="176128" r="91"/><S d="96760"/></SegmentTimeline></SegmentTemplate></Representation>"""

    private fun mpd(body: String) = """<?xml version='1.0' encoding='UTF-8'?><MPD xmlns="urn:mpeg:dash:schema:mpd:2011" type="static" mediaPresentationDuration="PT6M9.626S"><Period id="0"><AdaptationSet id="0" contentType="audio" mimeType="audio/mp4">$body</AdaptationSet></Period></MPD>""".toByteArray()

    private val adaptive = mpd(
        representation("HEAACV1", "mp4a.40.5", 96_965) + representation("AACLC", "mp4a.40.2", 321_677) +
            representation("FLAC", "flac", 944_871) + representation("FLAC_HIRES", "flac", 1_638_115, "FLAC_HIRES,44100,24")
    )

    @Test
    fun adaptiveManifestKeepsOnlyTheBestFlacRepresentation() {
        val manifest = TidalDashManifest.parse(adaptive)
        assertEquals(1_638_115, manifest.bandwidth)
        assertEquals(24, manifest.bitDepth)
        assertEquals(93, manifest.segmentCount)
        assertEquals(369_626L, manifest.durationMs)
        assertTrue(manifest.initialization.encodedPath.contains("FLAC_HIRES"))
        val urls = manifest.segmentUrls()
        assertTrue(urls.first().encodedPath.endsWith("/FLAC_HIRES/1.mp4"))
        assertTrue(urls.last().encodedPath.endsWith("/FLAC_HIRES/93.mp4"))
        assertEquals("x", urls.first().queryParameter("info"))
        val rewritten = TidalDashManifest.parse(manifest.toMpd())
        assertEquals(manifest, rewritten)
        assertFalse(manifest.toMpd().toString(Charsets.UTF_8).contains("mp4a"))
    }

    @Test
    fun proxiedManifestRoutesEverySegmentThroughTheSiteProxy() {
        val manifest = TidalDashManifest.parse(adaptive).proxiedThrough("https://media-proxy.odskyler.com".toHttpUrl())
        val reparsed = TidalDashManifest.parse(manifest.toMpd())
        assertEquals(manifest.segmentCount, reparsed.segmentCount)
        (listOf(reparsed.initialization) + reparsed.segmentUrls()).forEach { url ->
            assertEquals("media-proxy.odskyler.com", url.host)
            assertEquals("/proxy/segment", url.encodedPath)
            assertEquals("sp-ad-fa.audio.tidal.com", url.queryParameter("url")!!.toHttpUrl().host)
        }
        assertEquals("x", reparsed.segmentUrls()[4].queryParameter("url")!!.toHttpUrl().queryParameter("info"))
        assertTrue(reparsed.segmentUrls()[4].queryParameter("url")!!.contains("/FLAC_HIRES/5.mp4"))
    }

    @Test
    fun unsafeOrUnplayableManifestsAreRejected() {
        val flac = representation("FLAC", "flac", 944_871)
        listOf(
            mpd(representation("AACLC", "mp4a.40.2", 321_677)),
            mpd("<ContentProtection schemeIdUri=\"urn:mpeg:dash:mp4protection:2011\"/>$flac"),
            mpd(flac.replace("sp-ad-fa.audio.tidal.com", "evil.example.com")),
            mpd(flac.replace("https://", "http://")),
            ("<!DOCTYPE MPD [<!ENTITY x \"y\">]>" + mpd(flac).toString(Charsets.UTF_8)).toByteArray()
        ).forEach { bytes -> assertThrows(Exception::class.java) { TidalDashManifest.parse(bytes) } }
    }

    private fun box(type: String, vararg children: ByteArray): ByteArray {
        val body = children.fold(ByteArray(0)) { acc, part -> acc + part }
        val size = body.size + 8
        return byteArrayOf((size ushr 24).toByte(), (size ushr 16).toByte(), (size ushr 8).toByte(), size.toByte()) +
            type.toByteArray(Charsets.ISO_8859_1) + body
    }

    private fun streamInfo(totalSamples: Long): ByteArray {
        val block = ByteArray(34)
        val sampleRate = 44_100
        block[10] = (sampleRate ushr 12).toByte()
        block[11] = (sampleRate ushr 4).toByte()
        block[12] = (((sampleRate and 0x0F) shl 4) or (1 shl 1)).toByte()
        block[13] = ((15 shl 4) or ((totalSamples ushr 32).toInt() and 0x0F)).toByte()
        block[14] = (totalSamples ushr 24).toByte()
        block[15] = (totalSamples ushr 16).toByte()
        block[16] = (totalSamples ushr 8).toByte()
        block[17] = totalSamples.toByte()
        return byteArrayOf(0x80.toByte(), 0, 0, 34) + block
    }

    private fun init(totalSamples: Long): ByteArray {
        val dfla = box("dfLa", ByteArray(4), streamInfo(totalSamples))
        val flacEntry = box("fLaC", ByteArray(28), dfla)
        val stsd = box("stsd", ByteArray(8), flacEntry)
        return box("ftyp", ByteArray(8)) + box("moov", box("trak", box("mdia", box("minf", box("stbl", stsd)))))
    }

    @Test
    fun fragmentedSegmentsBecomeANativeFlacFile() {
        val output = ByteArrayOutputStream()
        output.write(TidalDashFlac.header(init(16_300_000L)))
        val frames = byteArrayOf(0xFF.toByte(), 0xF8.toByte(), 1, 2, 3)
        TidalDashFlac.writeFrames(box("styp", ByteArray(4)) + box("moof", ByteArray(16)) + box("mdat", frames), output)
        val bytes = output.toByteArray()
        val info = FlacStreamInfo.parse(bytes.copyOfRange(0, 42))!!
        assertEquals(44_100, info.sampleRateHz)
        assertEquals(16, info.bitsPerSample)
        assertTrue(info.durationMatches(369))
        assertTrue(bytes.copyOfRange(bytes.size - frames.size, bytes.size).contentEquals(frames))
        assertThrows(IOException::class.java) { TidalDashFlac.writeFrames(box("moof", ByteArray(16)), ByteArrayOutputStream()) }
        assertThrows(IOException::class.java) { TidalDashFlac.header(box("moov", ByteArray(4))) }
    }
}
