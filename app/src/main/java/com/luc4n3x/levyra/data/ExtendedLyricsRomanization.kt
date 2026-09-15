package com.luc4n3x.levyra.data

import java.text.Normalizer

internal data class ExtendedRomanizationResult(
    val text: String,
    val transformedCount: Int
)

internal object ExtendedLyricsRomanization {
    fun romanize(source: String): String = transliterate(source).text

    fun transliterate(source: String): ExtendedRomanizationResult {
        val normalizedSource = Normalizer.normalize(source, Normalizer.Form.NFC)
        var transformedCount = 0
        val text = buildString(normalizedSource.length * 2) {
            var index = 0
            while (index < normalizedSource.length) {
                val codePoint = normalizedSource.codePointAt(index)
                val indic = indicScript(codePoint)
                if (indic != null) {
                    val result = romanizeIndicSyllable(normalizedSource, index, indic)
                    append(result.text)
                    if (result.transformed) transformedCount++
                    index = result.nextIndex
                    continue
                }
                val mapped = when (codePoint) {
                    in 0x0370..0x03FF -> greek[codePoint]
                    in 0x0400..0x052F -> cyrillic[codePoint]
                    in 0x0590..0x05FF -> hebrew[codePoint]
                    in 0x0600..0x06FF, in 0x0750..0x077F, in 0x08A0..0x08FF -> arabic[codePoint]
                    in 0x10A0..0x10FF -> georgian[codePoint]
                    in 0x1C90..0x1CBF -> georgian[codePoint - 0x1C90 + 0x10D0]
                    else -> null
                }
                if (mapped != null) {
                    append(mapped)
                    if (mapped.isNotEmpty()) transformedCount++
                } else {
                    appendCodePoint(codePoint)
                }
                index += Character.charCount(codePoint)
            }
        }
        return ExtendedRomanizationResult(text, transformedCount)
    }

    private fun romanizeIndicSyllable(source: String, start: Int, script: IndicScript): IndicResult {
        val codePoint = source.codePointAt(start)
        script.independentVowels[codePoint]?.let {
            return IndicResult(it, start + Character.charCount(codePoint), transformed = true)
        }
        script.marks[codePoint]?.let {
            return IndicResult(it, start + Character.charCount(codePoint), transformed = true)
        }
        val base = script.consonants[codePoint] ?: return IndicResult(
            codePoint.toChars(),
            start + Character.charCount(codePoint),
            transformed = false
        )
        var index = start + Character.charCount(codePoint)
        var consonant = base
        if (index < source.length && source.codePointAt(index) == script.nukta) {
            consonant = script.nuktaConsonants[codePoint] ?: consonant
            index += Character.charCount(script.nukta)
        }
        if (index < source.length) {
            val next = source.codePointAt(index)
            if (next == script.virama) {
                return IndicResult(consonant, index + Character.charCount(next), transformed = true)
            }
            script.vowelMarks[next]?.let { vowel ->
                return IndicResult(consonant + vowel, index + Character.charCount(next), transformed = true)
            }
        }
        val nextIsBoundary = index >= source.length || source.codePointAt(index).let {
            Character.isWhitespace(it) || isPunctuation(it)
        }
        return IndicResult(consonant + if (nextIsBoundary) "" else script.inherentVowel, index, transformed = true)
    }

    private fun indicScript(codePoint: Int): IndicScript? = when (codePoint) {
        in 0x0900..0x097F -> devanagari
        in 0x0980..0x09FF -> bengali
        in 0x0A00..0x0A7F -> gurmukhi
        else -> null
    }

    private fun isPunctuation(codePoint: Int): Boolean = when (Character.getType(codePoint)) {
        Character.CONNECTOR_PUNCTUATION.toInt(),
        Character.DASH_PUNCTUATION.toInt(),
        Character.START_PUNCTUATION.toInt(),
        Character.END_PUNCTUATION.toInt(),
        Character.INITIAL_QUOTE_PUNCTUATION.toInt(),
        Character.FINAL_QUOTE_PUNCTUATION.toInt(),
        Character.OTHER_PUNCTUATION.toInt() -> true
        else -> false
    }

    private fun Int.toChars(): String = String(Character.toChars(this))

    private data class IndicResult(val text: String, val nextIndex: Int, val transformed: Boolean = false)

    private data class IndicScript(
        val consonants: Map<Int, String>,
        val independentVowels: Map<Int, String>,
        val vowelMarks: Map<Int, String>,
        val marks: Map<Int, String>,
        val virama: Int,
        val nukta: Int,
        val nuktaConsonants: Map<Int, String> = emptyMap(),
        val inherentVowel: String = "a"
    )

    private fun chars(values: String, romanized: List<String>): Map<Int, String> {
        val codePoints = values.codePoints().toArray()
        require(codePoints.size == romanized.size)
        return codePoints.zip(romanized).toMap()
    }

    private val greek = chars(
        "ΑΒΓΔΕΖΗΘΙΚΛΜΝΞΟΠΡΣΤΥΦΧΨΩαβγδεζηθικλμνξοπρσςτυφχψωΆΈΉΊΌΎΏάέήίόύώϊΐϋΰ",
        listOf(
            "A", "V", "G", "D", "E", "Z", "I", "Th", "I", "K", "L", "M", "N", "X", "O", "P", "R", "S", "T", "Y", "F", "Ch", "Ps", "O",
            "a", "v", "g", "d", "e", "z", "i", "th", "i", "k", "l", "m", "n", "x", "o", "p", "r", "s", "s", "t", "y", "f", "ch", "ps", "o",
            "A", "E", "I", "I", "O", "Y", "O", "a", "e", "i", "i", "o", "y", "o", "i", "i", "y", "y"
        )
    )

    private val cyrillic = buildMap {
        putAll(chars(
            "АБВГДЕЁЖЗИЙКЛМНОПРСТУФХЦЧШЩЪЫЬЭЮЯабвгдеёжзийклмнопрстуфхцчшщъыьэюя",
            listOf(
                "A", "B", "V", "G", "D", "E", "Yo", "Zh", "Z", "I", "Y", "K", "L", "M", "N", "O", "P", "R", "S", "T", "U", "F", "Kh", "Ts", "Ch", "Sh", "Shch", "", "Y", "", "E", "Yu", "Ya",
                "a", "b", "v", "g", "d", "e", "yo", "zh", "z", "i", "y", "k", "l", "m", "n", "o", "p", "r", "s", "t", "u", "f", "kh", "ts", "ch", "sh", "shch", "", "y", "", "e", "yu", "ya"
            )
        ))
        putAll(chars(
            "ЄІЇҐЎЂЋЏЉЊЈЌЃЅҚҢӨҮєіїґўђћџљњјќѓѕқңөү",
            listOf(
                "Ye", "I", "Yi", "G", "U", "Dj", "C", "Dz", "Lj", "Nj", "J", "Kj", "Gj", "Dz", "Q", "Ng", "O", "U",
                "ye", "i", "yi", "g", "u", "dj", "c", "dz", "lj", "nj", "j", "kj", "gj", "dz", "q", "ng", "o", "u"
            )
        ))
    }

    private val arabic = buildMap {
        putAll(chars(
            "ءآأؤإئابتثجحخدذرزسشصضطظعغفقكلمنهويىةپچژگڤکھی",
            listOf(
                "'", "aa", "a", "u", "i", "i", "a", "b", "t", "th", "j", "h", "kh", "d", "dh", "r", "z", "s", "sh", "s", "d", "t", "z", "'", "gh", "f", "q", "k", "l", "m", "n", "h", "w", "y", "a", "h", "p", "ch", "zh", "g", "v", "k", "h", "y"
            )
        ))
        putAll(mapOf(
            0x0640 to "", 0x064B to "an", 0x064C to "un", 0x064D to "in",
            0x064E to "a", 0x064F to "u", 0x0650 to "i", 0x0651 to "",
            0x0652 to "", 0x0653 to "", 0x0654 to "", 0x0655 to "", 0x0670 to "a"
        ))
    }

    private val hebrew = buildMap {
        putAll(chars(
            "אבגדהוזחטיךכלםמןנסעףפץצקרשת",
            listOf(
                "a", "v", "g", "d", "h", "v", "z", "kh", "t", "y", "kh", "k", "l", "m", "m", "n", "n", "s", "'", "f", "p", "ts", "ts", "k", "r", "sh", "t"
            )
        ))
        putAll(mapOf(
            0x05B0 to "e", 0x05B4 to "i", 0x05B5 to "e", 0x05B6 to "e",
            0x05B7 to "a", 0x05B8 to "a", 0x05B9 to "o", 0x05BB to "u",
            0x05BC to "", 0x05BD to "", 0x05BF to "", 0x05C1 to "", 0x05C2 to ""
        ))
    }

    private val georgian = chars(
        "აბგდევზთიკლმნოპჟრსტუფქღყშჩცძწჭხჯჰ",
        listOf(
            "a", "b", "g", "d", "e", "v", "z", "t", "i", "k", "l", "m", "n", "o", "p", "zh", "r", "s", "t", "u", "p", "k", "gh", "q", "sh", "ch", "ts", "dz", "ts", "ch", "kh", "j", "h"
        )
    )

    private val devanagari = IndicScript(
        consonants = chars(
            "कखगघङचछजझञटठडढणतथदधनपफबभमयरलवशषसहळ",
            listOf("k", "kh", "g", "gh", "ng", "ch", "chh", "j", "jh", "ny", "t", "th", "d", "dh", "n", "t", "th", "d", "dh", "n", "p", "ph", "b", "bh", "m", "y", "r", "l", "v", "sh", "sh", "s", "h", "l")
        ),
        independentVowels = chars("अआइईउऊऋएऐओऔ", listOf("a", "aa", "i", "ii", "u", "uu", "ri", "e", "ai", "o", "au")),
        vowelMarks = chars("ािीुूृेैोौ", listOf("aa", "i", "ii", "u", "uu", "ri", "e", "ai", "o", "au")),
        marks = mapOf(0x0901 to "n", 0x0902 to "n", 0x0903 to "h"),
        virama = 0x094D,
        nukta = 0x093C,
        nuktaConsonants = mapOf(0x0915 to "q", 0x0916 to "kh", 0x0917 to "gh", 0x091C to "z", 0x0921 to "r", 0x0922 to "rh", 0x092B to "f", 0x092F to "y")
    )

    private val gurmukhi = IndicScript(
        consonants = chars(
            "ਕਖਗਘਙਚਛਜਝਞਟਠਡਢਣਤਥਦਧਨਪਫਬਭਮਯਰਲਵਸ਼ਸਹੜ",
            listOf("k", "kh", "g", "gh", "ng", "ch", "chh", "j", "jh", "ny", "t", "th", "d", "dh", "n", "t", "th", "d", "dh", "n", "p", "ph", "b", "bh", "m", "y", "r", "l", "v", "sh", "s", "h", "r")
        ),
        independentVowels = chars("ਅਆਇਈਉਊਏਐਓਔ", listOf("a", "aa", "i", "ii", "u", "uu", "e", "ai", "o", "au")),
        vowelMarks = chars("ਾਿੀੁੂੇੈੋੌ", listOf("aa", "i", "ii", "u", "uu", "e", "ai", "o", "au")),
        marks = mapOf(0x0A01 to "n", 0x0A02 to "n", 0x0A03 to "h", 0x0A70 to "n", 0x0A71 to ""),
        virama = 0x0A4D,
        nukta = 0x0A3C,
        nuktaConsonants = mapOf(0x0A16 to "kh", 0x0A17 to "gh", 0x0A1C to "z", 0x0A2B to "f", 0x0A32 to "l")
    )

    private val bengali = IndicScript(
        consonants = chars(
            "কখগঘঙচছজঝঞটঠডঢণতথদধনপফবভমযরলশষসহড়ঢ়য়",
            listOf("k", "kh", "g", "gh", "ng", "ch", "chh", "j", "jh", "ny", "t", "th", "d", "dh", "n", "t", "th", "d", "dh", "n", "p", "ph", "b", "bh", "m", "y", "r", "l", "sh", "sh", "s", "h", "r", "rh", "y")
        ),
        independentVowels = chars("অআইঈউঊঋএঐওঔ", listOf("o", "aa", "i", "ii", "u", "uu", "ri", "e", "oi", "o", "ou")),
        vowelMarks = chars("ািীুূৃেৈোৌ", listOf("aa", "i", "ii", "u", "uu", "ri", "e", "oi", "o", "ou")),
        marks = mapOf(0x0981 to "n", 0x0982 to "ng", 0x0983 to "h"),
        virama = 0x09CD,
        nukta = 0x09BC,
        inherentVowel = "o"
    )
}
