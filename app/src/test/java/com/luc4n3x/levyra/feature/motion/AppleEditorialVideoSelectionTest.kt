package com.luc4n3x.levyra.feature.motion

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppleEditorialVideoSelectionTest {

    @Test
    fun `missing editorial video yields nothing`() {
        assertNull(selectAppleEditorialVideo(null))
        assertNull(selectAppleEditorialVideo(JSONObject("{}")))
    }

    @Test
    fun `assets without an https url are rejected`() {
        val selected = selectAppleEditorialVideo(
            JSONObject(
                """
                {"motionDetailSquare": {"video": "http://example.invalid/insecure.m3u8"},
                 "motionDetailTall": {"previewFrame": {"width": 1080, "height": 1920}}}
                """.trimIndent()
            )
        )
        assertNull(selected)
    }

    @Test
    fun `tall asset wins over a larger square asset`() {
        val selected = selectAppleEditorialVideo(
            JSONObject(
                """
                {"motionDetailSquare": {"video": "https://example.invalid/square.m3u8",
                                        "previewFrame": {"width": 2048, "height": 2048}},
                 "motionDetailTall": {"video": "https://example.invalid/tall.m3u8",
                                      "previewFrame": {"width": 1080, "height": 1920}}}
                """.trimIndent()
            )
        )
        assertEquals("https://example.invalid/tall.m3u8", selected?.url)
        assertEquals(1080, selected?.width)
        assertEquals(1920, selected?.height)
    }

    @Test
    fun `the highest resolution asset wins within the same orientation`() {
        val selected = selectAppleEditorialVideo(
            JSONObject(
                """
                {"motionDetailTall": {"video": "https://example.invalid/small.m3u8",
                                      "previewFrame": {"width": 540, "height": 960}},
                 "motionTallVideo3x4": {"hlsUrl": "https://example.invalid/large.m3u8",
                                        "previewFrame": {"width": 1080, "height": 1440}}}
                """.trimIndent()
            )
        )
        assertEquals("https://example.invalid/large.m3u8", selected?.url)
    }

    @Test
    fun `an asset without dimensions is still usable as a fallback`() {
        val selected = selectAppleEditorialVideo(
            JSONObject("""{"motionDetailRaw": {"url": "https://example.invalid/raw.m3u8"}}""")
        )
        assertEquals("https://example.invalid/raw.m3u8", selected?.url)
        assertNull(selected?.width)
        assertNull(selected?.height)
    }

    @Test
    fun `a wide asset loses against an undimensioned one`() {
        val selected = selectAppleEditorialVideo(
            JSONObject(
                """
                {"motionDetailStatic": {"video": "https://example.invalid/wide.m3u8",
                                        "previewFrame": {"width": 1920, "height": 1080}},
                 "motionDetailRaw": {"video": "https://example.invalid/raw.m3u8"}}
                """.trimIndent()
            )
        )
        assertEquals("https://example.invalid/raw.m3u8", selected?.url)
    }
}
