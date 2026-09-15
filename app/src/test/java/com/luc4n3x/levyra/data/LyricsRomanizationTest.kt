package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class LyricsRomanizationTest {
    @Test
    fun `keeps latin-only lyrics as a single original row`() {
        assertEquals("", LyricsRomanizer.romanize("Already Latin"))
    }

    @Test
    fun `romanizes cyrillic and greek without a dictionary`() {
        assertEquals("privet mir", LyricsRomanizer.romanize("Привет мир"))
        assertEquals("kalimera kosme", LyricsRomanizer.romanize("Καλημέρα κόσμε"))
    }

    @Test
    fun `preserves existing korean and kana romanization`() {
        assertEquals("annyeong", LyricsRomanizer.romanize("안녕"))
        assertEquals("konnichiha", LyricsRomanizer.romanize("こんにちは"))
    }

    @Test
    fun `romanizes devanagari gurmukhi and bengali syllables`() {
        assertEquals("namaste", LyricsRomanizer.romanize("नमस्ते"))
        assertEquals("sat shrii akaal", LyricsRomanizer.romanize("ਸਤ ਸ਼੍ਰੀ ਅਕਾਲ"))
        assertEquals("aami tomaake", LyricsRomanizer.romanize("আমি তোমাকে"))
    }

    @Test
    fun `romanizes arabic hebrew and georgian predictably`() {
        assertEquals("mrhba", LyricsRomanizer.romanize("مرحبا"))
        assertEquals("shlvm", LyricsRomanizer.romanize("שלום"))
        assertEquals("gamarjoba", LyricsRomanizer.romanize("გამარჯობა"))
    }

    @Test
    fun `preserves original punctuation emoji and latin fragments`() {
        val result = LyricsRomanizer.romanize("Hello — мир! 🎵")
        assertTrue(result.startsWith("hello — mir!"))
        assertTrue(result.endsWith("🎵"))
    }
}
