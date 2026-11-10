package com.luc4n3x.levyra.nexus.playlistimport

import com.luc4n3x.levyra.nexus.identity.MusicIdentityText

object ImportTransliteration {
    private val cyrillic = "абвгдеёжзийклмнопрстуфхцчшщъыьэюяіїєґ"
    private val latin = listOf(
        "a", "b", "v", "g", "d", "e", "e", "zh", "z", "i", "y", "k", "l", "m", "n", "o",
        "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya",
        "i", "yi", "ye", "g"
    )

    fun latinQuery(value: String): String = buildString {
        MusicIdentityText.normalize(value).forEach { char ->
            val index = cyrillic.indexOf(char)
            append(if (index >= 0) latin[index] else char.toString())
        }
    }

    fun related(left: String, right: String): Boolean {
        if (left.isBlank() || right.isBlank()) return false
        val leftCyrillic = left.any { it in cyrillic }
        val rightCyrillic = right.any { it in cyrillic }
        if (leftCyrillic == rightCyrillic) return false
        val a = variants(latinQuery(left))
        val b = variants(latinQuery(right))
        return a.any { it.length >= 3 && it in b }
    }

    fun romanizedKey(value: String): String {
        val folded = latinQuery(value)
            .replace("shch", "sh")
            .replace("sch", "sh")
            .replace("kh", "h")
            .replace("ts", "c")
            .replace("x", "ks")
            .replace('g', 'h')
            .replace('y', 'i')
            .replace('j', 'i')
        return buildString(folded.length) {
            folded.forEach { char -> if (char == ' ' || isEmpty() || last() != char) append(char) }
        }
    }

    fun containsCyrillic(value: String): Boolean =
        value.any { Character.UnicodeScript.of(it.code) == Character.UnicodeScript.CYRILLIC }

    fun sameRomanized(left: String, right: String): Boolean {
        if (!containsCyrillic(left) && !containsCyrillic(right)) return false
        val key = romanizedKey(left)
        return key.count { it != ' ' } >= 2 && key == romanizedKey(right)
    }

    private fun variants(value: String): Set<String> = setOf(
        value,
        value.replace("kh", "h").replace("shch", "sch").replace("yu", "iu").replace("ya", "ia").replace('y', 'i'),
        value.replace("kh", "h").replace("shch", "sch").replace("yu", "ju").replace("ya", "ja").replace('y', 'j'),
        value.replace("victor", "viktor").replace("tsoy", "tsoi").replace('y', 'i')
    )
}
