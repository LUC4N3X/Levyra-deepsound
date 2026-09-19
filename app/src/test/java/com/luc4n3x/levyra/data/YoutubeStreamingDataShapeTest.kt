package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeStreamingDataShapeTest {

    private fun shape(json: String?): YoutubeStreamingDataShape =
        YoutubeStreamingDataShape.of(json?.let(::JSONObject))

    @Test
    fun directUrlWinsEvenWhenSabrIsAlsoAdvertised() {
        assertEquals(
            YoutubeStreamingDataShape.DIRECT,
            shape(
                """{"serverAbrStreamingUrl":"https://x.googlevideo.com/sabr","adaptiveFormats":[{"itag":251,"url":"https://x.googlevideo.com/videoplayback"}]}"""
            )
        )
    }

    @Test
    fun cipheredFormatsAreNotReportedAsMissingUrls() {
        assertEquals(
            YoutubeStreamingDataShape.CIPHERED,
            shape("""{"adaptiveFormats":[{"itag":251,"signatureCipher":"s=abc&sp=sig&url=https%3A%2F%2Fx"}]}""")
        )
        assertEquals(
            YoutubeStreamingDataShape.CIPHERED,
            shape("""{"formats":[{"itag":18,"cipher":"s=abc&sp=sig&url=https%3A%2F%2Fx"}]}""")
        )
    }

    @Test
    fun sabrOnlyResponseIsRecognised() {
        assertEquals(
            YoutubeStreamingDataShape.SABR_ONLY,
            shape(
                """{"serverAbrStreamingUrl":"https://x.googlevideo.com/sabr","adaptiveFormats":[{"itag":251,"mimeType":"audio/webm"}]}"""
            )
        )
    }

    @Test
    fun manifestOnlyResponsesAreRecognised() {
        assertEquals(
            YoutubeStreamingDataShape.DASH_ONLY,
            shape("""{"dashManifestUrl":"https://manifest.googlevideo.com/dash","hlsManifestUrl":"https://manifest.googlevideo.com/hls"}""")
        )
        assertEquals(
            YoutubeStreamingDataShape.HLS_ONLY,
            shape("""{"hlsManifestUrl":"https://manifest.googlevideo.com/hls"}""")
        )
    }

    @Test
    fun malformedOrMissingStreamingDataIsEmpty() {
        assertEquals(YoutubeStreamingDataShape.EMPTY, shape(null))
        assertEquals(YoutubeStreamingDataShape.EMPTY, shape("""{"adaptiveFormats":"broken","formats":[1,null]}"""))
        assertEquals(YoutubeStreamingDataShape.EMPTY, shape("""{"serverAbrStreamingUrl":"https://x.googlevideo.com/sabr"}"""))
        assertEquals(YoutubeStreamingDataShape.EMPTY, shape("""{"adaptiveFormats":[{"itag":251}]}"""))
    }
}
