package com.luc4n3x.levyra.ui.i18n

internal data class PrismCopy(
    val body: String, val appearance: String, val player: String,
    val translucency: String, val night: String, val light: String, val automatic: String,
    val reduced: String, val torqueBody: String, val accent: String, val cover: String, val red: String
)

internal object LevyraPrismStrings {
    private val english = PrismCopy(
        "Deep black, quiet surfaces and a single raspberry accent. Artwork leads, controls stay calm.",
        "Appearance", "Player layout", "Translucent effects", "Night", "Light",
        "Automatic", "Reduced",
        "Graphite slabs, sharp diagonals and a bold band that takes its color from the cover.",
        "Accent", "From cover", "Red"
    )
    private val italian = PrismCopy(
        "Nero profondo, superfici quiete e un unico accento raspberry. Le copertine guidano, i comandi restano calmi.",
        "Aspetto", "Layout player", "Effetti traslucidi", "Night", "Light",
        "Automatico", "Ridotti",
        "Lastre grafite, diagonali nette e una banda decisa che prende il colore dalla copertina.",
        "Accento", "Dalla copertina", "Rosso"
    )
    fun forCode(code: String): PrismCopy = if (code == "it") italian else english
}
