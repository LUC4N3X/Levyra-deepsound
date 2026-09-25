package com.luc4n3x.levyra.feature.sharedmedia

import com.luc4n3x.levyra.domain.LevyraLanguageCatalog
import com.luc4n3x.levyra.ui.i18n.bulkLinkCaptureCopy
import com.luc4n3x.levyra.ui.i18n.bulkLinkCaptureLocalizationCodes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BulkLinkCaptureTest {

    private val videoId = Regex("[?&]v=([A-Za-z0-9_-]{11})")

    private fun classify(url: String): SharedMediaRequest? {
        if (!url.contains("youtu")) return SharedMediaRequest(rawText = url, url = url, kind = SharedMediaKind.Unsupported)
        val id = videoId.find(url)?.groupValues?.get(1) ?: url.substringAfter("youtu.be/", "")
        if (id.length != 11) return null
        return SharedMediaRequest(
            rawText = url,
            url = "https://www.youtube.com/watch?v=$id",
            kind = SharedMediaKind.Video,
            videoId = id
        )
    }

    @Test
    fun extractsUrlsFromNoisyTextAndTrimsPunctuation() {
        val text = """
            Ascolta questa: https://youtu.be/aaaaaaaaaaa, e poi
            (https://music.youtube.com/watch?v=bbbbbbbbbbb). Grazie!
            https://www.youtube.com/watch?v=ccccccccccc https://example.com/x
        """.trimIndent()

        assertEquals(
            listOf(
                "https://youtu.be/aaaaaaaaaaa",
                "https://music.youtube.com/watch?v=bbbbbbbbbbb",
                "https://www.youtube.com/watch?v=ccccccccccc",
                "https://example.com/x"
            ),
            BulkLinkCapture.extractUrls(text)
        )
    }

    @Test
    fun summaryCountsDuplicatesAndUnrecognizedWithoutFailing() {
        val urls = listOf(
            "https://youtu.be/aaaaaaaaaaa",
            "https://www.youtube.com/watch?v=aaaaaaaaaaa",
            "https://music.youtube.com/watch?v=bbbbbbbbbbb",
            "https://example.com/not-music",
            "https://youtu.be/bad"
        )

        val request = BulkLinkCapture.request(urls, ::classify)

        assertEquals(SharedMediaKind.BulkLinks, request.kind)
        assertEquals(5, request.bulkDetected)
        assertEquals(1, request.bulkDuplicates)
        assertEquals(2, request.bulkUnrecognized)
        assertEquals(
            listOf("https://www.youtube.com/watch?v=aaaaaaaaaaa", "https://www.youtube.com/watch?v=bbbbbbbbbbb"),
            request.bulkUrls
        )
    }

    @Test
    fun linkCountIsBounded() {
        val urls = (0 until BulkLinkCapture.MAX_LINKS + 20).map { "https://youtu.be/${it.toString().padStart(11, 'a')}" }

        val request = BulkLinkCapture.request(urls, ::classify)

        assertEquals(BulkLinkCapture.MAX_LINKS, request.bulkUrls.size)
        assertEquals(20, request.bulkUnrecognized)
    }

    @Test
    fun bulkRequestsWithDifferentLinksHaveDifferentKeys() {
        val first = BulkLinkCapture.request(listOf("https://youtu.be/aaaaaaaaaaa", "https://youtu.be/bbbbbbbbbbb"), ::classify)
        val second = BulkLinkCapture.request(listOf("https://youtu.be/aaaaaaaaaaa", "https://youtu.be/ccccccccccc"), ::classify)

        assertTrue(first.key != second.key)
    }

    @Test
    fun everySupportedLanguageHasBulkCaptureCopy() {
        val supported = LevyraLanguageCatalog.languages.map { it.code }.toSet()

        assertEquals(37, supported.size)
        assertEquals(supported, bulkLinkCaptureLocalizationCodes())
        supported.forEach { code ->
            val copy = bulkLinkCaptureCopy(code)
            assertTrue(code, copy.detected(38).contains("38"))
            assertTrue(code, copy.found(35).contains("35"))
            assertTrue(code, copy.duplicates(2).contains("2"))
            assertTrue(code, copy.unrecognized(1).contains("1"))
            assertTrue(code, copy.resolving(7).contains("7"))
        }
    }
}
