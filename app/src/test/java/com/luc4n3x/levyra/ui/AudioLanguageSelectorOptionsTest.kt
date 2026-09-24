package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.data.AudioLanguageIntelligence
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class AudioLanguageSelectorOptionsTest {
    private val baseOptions = listOf(
        "" to "Original",
        "en" to "English",
        "pt" to "Português"
    )

    @Test
    fun `listed and default preferences keep the base options`() {
        assertSame(baseOptions, audioLanguageSelectorOptions(baseOptions, ""))
        assertSame(baseOptions, audioLanguageSelectorOptions(baseOptions, "en"))
    }

    @Test
    fun `restored regional preference gets its own selected option`() {
        val restored = AudioLanguageIntelligence.normalizeLanguage("en-US")

        val options = audioLanguageSelectorOptions(baseOptions, restored)

        assertEquals("en-us", restored)
        assertEquals(baseOptions.size + 1, options.size)
        assertEquals(1, options.count { (code, _) -> code == restored })
        assertEquals("English (United States)", options.last().second)
    }

    @Test
    fun `regional option label is localized in its own language`() {
        val restored = AudioLanguageIntelligence.normalizeLanguage("pt_BR")

        val options = audioLanguageSelectorOptions(baseOptions, restored)

        assertEquals("pt-br" to "Português (Brasil)", options.last())
    }

    @Test
    fun `unknown restored code stays visible without being rewritten`() {
        val options = audioLanguageSelectorOptions(baseOptions, "zz-qq")

        assertEquals("zz-qq", options.last().first)
        assertEquals(baseOptions, options.dropLast(1))
    }
}
