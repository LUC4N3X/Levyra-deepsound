package com.luc4n3x.levyra.nexus.text

import java.util.regex.Matcher

class ReusableRegex(pattern: String, options: Set<RegexOption> = emptySet()) {
    constructor(pattern: String, option: RegexOption) : this(pattern, setOf(option))

    private val nativePattern = Regex(pattern, options).toPattern()
    private val matchers = ThreadLocal.withInitial { nativePattern.matcher("") }

    fun replace(input: CharSequence, replacement: String): String = withMatcher(input) { it.replaceAll(replacement) }

    fun containsMatchIn(input: CharSequence): Boolean = withMatcher(input) { it.find() }

    fun matches(input: CharSequence): Boolean = withMatcher(input) { it.matches() }

    private inline fun <T> withMatcher(input: CharSequence, block: (Matcher) -> T): T {
        val matcher = matchers.get()!!
        return try {
            block(matcher.reset(input))
        } finally {
            matcher.reset("")
        }
    }
}

fun CharSequence.replace(regex: ReusableRegex, replacement: String): String = regex.replace(this, replacement)

fun CharSequence.matches(regex: ReusableRegex): Boolean = regex.matches(this)
