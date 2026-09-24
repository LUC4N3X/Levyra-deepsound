package com.luc4n3x.levyra.feature.radio

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class LiveRadioArtworkResolverTest {
    @Test
    fun candidatesPreferTouchIconsThenLargeIconsThenOpenGraphThenSmallIcons() {
        val html = """
            <head>
              <link rel="icon" href="/favicon-16.png" sizes="16x16">
              <meta property="og:image" content="https://cdn.example.org/og.jpg?a=1&amp;b=2">
              <link rel='icon' type='image/png' sizes='192x192' href='img/icon-192.png'>
              <link rel="mask-icon" href="/mask.svg">
              <link rel="icon" href="/logo.svg">
              <link rel="apple-touch-icon-precomposed" href="//static.example.org/touch.png">
              <link rel="icon" href="data:image/png;base64,AAAA">
            </head>
        """.trimIndent()

        val result = liveRadioArtworkCandidates(html, "https://radio.example.org/live/page".toHttpUrl())

        assertEquals(
            listOf(
                "https://static.example.org/touch.png",
                "https://radio.example.org/live/img/icon-192.png",
                "https://cdn.example.org/og.jpg?a=1&b=2",
                "https://radio.example.org/favicon-16.png"
            ),
            result
        )
    }

    @Test
    fun rasterHeaderAcceptsCommonImageFormatsAndRejectsMarkup() {
        assertTrue(isRasterImageHeader(byteArrayOf(0x89.toByte(), 0x50, 0x4E, 0x47, 0x0D, 0x0A)))
        assertTrue(isRasterImageHeader(byteArrayOf(0xFF.toByte(), 0xD8.toByte(), 0xFF.toByte(), 0xE0.toByte())))
        assertTrue(isRasterImageHeader(byteArrayOf(0x00, 0x00, 0x01, 0x00, 0x01, 0x00)))
        assertTrue(isRasterImageHeader("RIFF\u0000\u0000\u0000\u0000WEBP".toByteArray(Charsets.ISO_8859_1)))
        assertFalse(isRasterImageHeader("<svg xmlns".toByteArray()))
        assertFalse(isRasterImageHeader("<!DOCTYPE html>".toByteArray()))
        assertFalse(isRasterImageHeader(byteArrayOf()))
    }
}
