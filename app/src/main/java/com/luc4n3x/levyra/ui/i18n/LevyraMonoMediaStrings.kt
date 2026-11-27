package com.luc4n3x.levyra.ui.i18n

internal data class MonoMediaCopy(
    val explicit: String,
    val aboutShow: String
)

internal object LevyraMonoMediaStrings {
    private val copies = mapOf(
        "en" to MonoMediaCopy(explicit = "Explicit", aboutShow = "About the show"),
        "it" to MonoMediaCopy(explicit = "Esplicito", aboutShow = "Il programma"),
        "ur" to MonoMediaCopy(explicit = "واضح", aboutShow = "شو کے بارے میں"),
        "ta" to MonoMediaCopy(explicit = "வெளிப்படையானது", aboutShow = "நிகழ்ச்சி பற்றி"),
        "te" to MonoMediaCopy(explicit = "స్పష్టమైన", aboutShow = "ప్రదర్శన గురించి"),
        "mr" to MonoMediaCopy(explicit = "स्पष्ट", aboutShow = "शो बद्दल"),
        "gu" to MonoMediaCopy(explicit = "સ્પષ્ટ", aboutShow = "શો વિશે"),
        "pt-BR" to MonoMediaCopy(explicit = "Explícito", aboutShow = "Sobre o espetáculo")
    )

    fun forCode(code: String): MonoMediaCopy = localizedValueOrEnglish(copies, code)
}
