package com.luc4n3x.levyra.ui.i18n

internal data class MonoMediaCopy(
    val explicit: String,
    val aboutShow: String
)

internal object LevyraMonoMediaStrings {
    private val copies = mapOf(
        "en" to MonoMediaCopy(explicit = "Explicit", aboutShow = "About the show"),
        "it" to MonoMediaCopy(explicit = "Esplicito", aboutShow = "Il programma")
    )

    fun forCode(code: String): MonoMediaCopy = localizedValueOrEnglish(copies, code)
}
