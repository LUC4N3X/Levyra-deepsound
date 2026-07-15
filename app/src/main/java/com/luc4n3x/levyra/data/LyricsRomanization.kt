package com.luc4n3x.levyra.data

import java.util.Locale

object LyricsRomanizer {
    private val cjkRegex = Regex("[\\u3040-\\u30ff\\u3400-\\u9fff\\uac00-\\ud7af]")
    private val whitespaceRegex = Regex("\\s+")

    fun romanize(text: String): String {
        val source = text.trim()
        if (source.isBlank() || !cjkRegex.containsMatchIn(source)) return ""
        val icu = romanizeWithIcu(source)
        if (icu.isNotBlank() && icu != source) return normalize(icu)
        val fallback = buildString {
            var index = 0
            while (index < source.length) {
                val codePoint = source.codePointAt(index)
                when {
                    codePoint in 0xAC00..0xD7A3 -> append(romanizeHangul(codePoint))
                    codePoint in 0x3040..0x30FF -> {
                        val pair = if (index + 1 < source.length) source.substring(index, index + 2) else ""
                        val pairRomanized = kanaPairs[pair]
                        if (pairRomanized != null) {
                            append(pairRomanized)
                            index += 1
                        } else {
                            append(kanaSingles[source[index]] ?: source[index])
                        }
                    }
                    else -> appendCodePoint(codePoint)
                }
                index += Character.charCount(codePoint)
            }
        }
        return normalize(fallback).takeIf { it.isNotBlank() && it != source }.orEmpty()
    }

    private fun romanizeWithIcu(text: String): String {
        return runCatching {
            val clazz = Class.forName("android.icu.text.Transliterator")
            val instance = clazz.getMethod("getInstance", String::class.java)
                .invoke(null, "Any-Latin; NFD; [:Nonspacing Mark:] Remove; NFC")
            clazz.getMethod("transliterate", String::class.java)
                .invoke(instance, text) as? String
        }.getOrNull().orEmpty()
    }

    private fun romanizeHangul(codePoint: Int): String {
        val syllable = codePoint - 0xAC00
        val initial = syllable / 588
        val medial = (syllable % 588) / 28
        val final = syllable % 28
        return hangulInitials[initial] + hangulMedials[medial] + hangulFinals[final]
    }

    private fun normalize(value: String): String = value
        .replace("’", "'")
        .replace(whitespaceRegex, " ")
        .trim()
        .lowercase(Locale.ROOT)

    private val hangulInitials = arrayOf(
        "g", "kk", "n", "d", "tt", "r", "m", "b", "pp", "s", "ss", "", "j", "jj", "ch", "k", "t", "p", "h"
    )

    private val hangulMedials = arrayOf(
        "a", "ae", "ya", "yae", "eo", "e", "yeo", "ye", "o", "wa", "wae", "oe", "yo", "u", "wo", "we", "wi", "yu", "eu", "ui", "i"
    )

    private val hangulFinals = arrayOf(
        "", "k", "k", "ks", "n", "nj", "nh", "t", "l", "lk", "lm", "lb", "ls", "lt", "lp", "lh", "m", "p", "ps", "t", "t", "ng", "t", "t", "k", "t", "p", "h"
    )

    private val kanaPairs = mapOf(
        "きゃ" to "kya", "きゅ" to "kyu", "きょ" to "kyo", "ぎゃ" to "gya", "ぎゅ" to "gyu", "ぎょ" to "gyo",
        "しゃ" to "sha", "しゅ" to "shu", "しょ" to "sho", "じゃ" to "ja", "じゅ" to "ju", "じょ" to "jo",
        "ちゃ" to "cha", "ちゅ" to "chu", "ちょ" to "cho", "にゃ" to "nya", "にゅ" to "nyu", "にょ" to "nyo",
        "ひゃ" to "hya", "ひゅ" to "hyu", "ひょ" to "hyo", "びゃ" to "bya", "びゅ" to "byu", "びょ" to "byo",
        "ぴゃ" to "pya", "ぴゅ" to "pyu", "ぴょ" to "pyo", "みゃ" to "mya", "みゅ" to "myu", "みょ" to "myo",
        "りゃ" to "rya", "りゅ" to "ryu", "りょ" to "ryo", "キャ" to "kya", "キュ" to "kyu", "キョ" to "kyo",
        "シャ" to "sha", "シュ" to "shu", "ショ" to "sho", "チャ" to "cha", "チュ" to "chu", "チョ" to "cho"
    )

    private val kanaSingles = mapOf(
        'あ' to "a", 'い' to "i", 'う' to "u", 'え' to "e", 'お' to "o", 'か' to "ka", 'き' to "ki", 'く' to "ku", 'け' to "ke", 'こ' to "ko",
        'さ' to "sa", 'し' to "shi", 'す' to "su", 'せ' to "se", 'そ' to "so", 'た' to "ta", 'ち' to "chi", 'つ' to "tsu", 'て' to "te", 'と' to "to",
        'な' to "na", 'に' to "ni", 'ぬ' to "nu", 'ね' to "ne", 'の' to "no", 'は' to "ha", 'ひ' to "hi", 'ふ' to "fu", 'へ' to "he", 'ほ' to "ho",
        'ま' to "ma", 'み' to "mi", 'む' to "mu", 'め' to "me", 'も' to "mo", 'や' to "ya", 'ゆ' to "yu", 'よ' to "yo", 'ら' to "ra", 'り' to "ri",
        'る' to "ru", 'れ' to "re", 'ろ' to "ro", 'わ' to "wa", 'を' to "o", 'ん' to "n", 'が' to "ga", 'ぎ' to "gi", 'ぐ' to "gu", 'げ' to "ge",
        'ご' to "go", 'ざ' to "za", 'じ' to "ji", 'ず' to "zu", 'ぜ' to "ze", 'ぞ' to "zo", 'だ' to "da", 'ぢ' to "ji", 'づ' to "zu", 'で' to "de",
        'ど' to "do", 'ば' to "ba", 'び' to "bi", 'ぶ' to "bu", 'べ' to "be", 'ぼ' to "bo", 'ぱ' to "pa", 'ぴ' to "pi", 'ぷ' to "pu", 'ぺ' to "pe",
        'ぽ' to "po", 'ア' to "a", 'イ' to "i", 'ウ' to "u", 'エ' to "e", 'オ' to "o", 'カ' to "ka", 'キ' to "ki", 'ク' to "ku", 'ケ' to "ke",
        'コ' to "ko", 'サ' to "sa", 'シ' to "shi", 'ス' to "su", 'セ' to "se", 'ソ' to "so", 'タ' to "ta", 'チ' to "chi", 'ツ' to "tsu", 'テ' to "te",
        'ト' to "to", 'ナ' to "na", 'ニ' to "ni", 'ヌ' to "nu", 'ネ' to "ne", 'ノ' to "no", 'ハ' to "ha", 'ヒ' to "hi", 'フ' to "fu", 'ヘ' to "he",
        'ホ' to "ho", 'マ' to "ma", 'ミ' to "mi", 'ム' to "mu", 'メ' to "me", 'モ' to "mo", 'ヤ' to "ya", 'ユ' to "yu", 'ヨ' to "yo", 'ラ' to "ra",
        'リ' to "ri", 'ル' to "ru", 'レ' to "re", 'ロ' to "ro", 'ワ' to "wa", 'ヲ' to "o", 'ン' to "n", 'ガ' to "ga", 'ギ' to "gi", 'グ' to "gu",
        'ゲ' to "ge", 'ゴ' to "go", 'ザ' to "za", 'ジ' to "ji", 'ズ' to "zu", 'ゼ' to "ze", 'ゾ' to "zo", 'ダ' to "da", 'デ' to "de", 'ド' to "do",
        'バ' to "ba", 'ビ' to "bi", 'ブ' to "bu", 'ベ' to "be", 'ボ' to "bo", 'パ' to "pa", 'ピ' to "pi", 'プ' to "pu", 'ペ' to "pe", 'ポ' to "po",
        'ー' to "-", '。' to ".", '、' to ","
    )
}
