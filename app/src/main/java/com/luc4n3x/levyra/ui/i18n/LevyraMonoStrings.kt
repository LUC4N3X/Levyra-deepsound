package com.luc4n3x.levyra.ui.i18n

internal data class MonoCopy(
    val skinBody: String
)

internal object LevyraMonoStrings {
    private val copies = mapOf(
        "en" to MonoCopy(
            skinBody = "Dark minimalism with music up front. Monochrome type, one ice accent and a dock that moves with you."
        ),
        "it" to MonoCopy(
            skinBody = "Minimalismo scuro, musica in primo piano. Tipografia monocromatica, un solo accento ghiaccio e un dock che si muove con te."
        )
    )

    fun forCode(code: String): MonoCopy = localizedValueOrEnglish(copies, code)
}
