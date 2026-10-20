package com.luc4n3x.levyra.ui.i18n

internal data class PrismCopy(
    val body: String, val appearance: String, val colors: String, val player: String,
    val translucency: String, val night: String, val light: String, val automatic: String,
    val adaptive: String, val reduced: String
)

internal object LevyraPrismStrings {
    private val english = PrismCopy(
        "Artwork in the spotlight. Refined ambient color, frosted controls and a dedicated player.",
        "Appearance", "Colors", "Player layout", "Translucent effects", "Night", "Light",
        "Automatic", "Adaptive", "Reduced"
    )
    private val italian = PrismCopy(
        "La musica in primo piano. Colori ambientali, comandi satinati e un player dedicato.",
        "Aspetto", "Colori", "Layout player", "Effetti traslucidi", "Night", "Light",
        "Automatico", "Adaptive", "Ridotti"
    )
    fun forCode(code: String): PrismCopy = if (code == "it") italian else english
}
