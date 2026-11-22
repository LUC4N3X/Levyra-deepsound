package com.luc4n3x.levyra.nexus.text

import java.util.concurrent.ConcurrentHashMap
import org.junit.Assert.assertEquals
import org.junit.Test

class ReusableRegexTest {
    private val inputs = listOf("", "Song (Live) feat. Artist", "FEAT. Someone", "  spaced   out  ", "Ünïcödé — Title [Remix]", "abcDEF_123-xyz")

    @Test
    fun matchesKotlinRegexAcrossRepeatedCalls() {
        val cases = listOf(
            "\\s+" to emptySet(),
            "\\([^)]*\\)|\\[[^]]*]" to emptySet(),
            "(?i)\\b(?:feat\\.?|ft\\.?|featuring)\\b.*$" to emptySet(),
            "feat\\.?|featuring" to setOf(RegexOption.IGNORE_CASE),
            "[^\\p{L}\\p{M}\\p{N}\\s]" to emptySet(),
            "[A-Za-z0-9_-]+" to emptySet()
        )
        cases.forEach { (pattern, options) ->
            val reference = Regex(pattern, options)
            val reusable = ReusableRegex(pattern, options)
            repeat(2) {
                inputs.forEach { input ->
                    assertEquals(input.replace(reference, "|"), input.replace(reusable, "|"))
                    assertEquals(reference.containsMatchIn(input), reusable.containsMatchIn(input))
                    assertEquals(input.matches(reference), input.matches(reusable))
                }
            }
        }
    }

    @Test
    fun keepsResultsIndependentAcrossThreads() {
        val reusable = ReusableRegex("\\s+")
        val actual = ConcurrentHashMap<Int, String>()
        (1..8).map { index ->
            Thread { repeat(200) { actual[index] = "a  b ".repeat(index).replace(reusable, " ") } }.also(Thread::start)
        }.forEach(Thread::join)
        (1..8).forEach { index -> assertEquals("a  b ".repeat(index).replace(Regex("\\s+"), " "), actual[index]) }
    }
}
