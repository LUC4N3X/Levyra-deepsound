package com.luc4n3x.levyra.nexus.text

import java.util.regex.Matcher

class ReusableRegex(pattern: String, options: Set<RegexOption> = emptySet()) {
    constructor(pattern: String, option: RegexOption) : this(pattern, setOf(option))

    private val nativePattern = Regex(pattern, options).toPattern()
    private val matchers = ThreadLocal.withInitial { nativePattern.matcher("") }

    fun replace(input: CharSequence, replacement: String): String = matcher(input).replaceAll(replacement)

    fun containsMatchIn(input: CharSequence): Boolean = matcher(input).find()

    fun matches(input: CharSequence): Boolean = matcher(input).matches()

    private fun matcher(input: CharSequence): Matcher = matchers.get()!!.reset(input)
}

fun CharSequence.replace(regex: ReusableRegex, replacement: String): String = regex.replace(this, replacement)

fun CharSequence.matches(regex: ReusableRegex): Boolean = regex.matches(this)
