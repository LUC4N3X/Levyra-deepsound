package com.luc4n3x.levyra.ui.i18n

internal data class PrismCopy(
    val body: String, val appearance: String, val player: String,
    val translucency: String, val night: String, val light: String, val automatic: String,
    val reduced: String, val torqueBody: String, val accent: String, val cover: String, val red: String,
    val ivoryBody: String, val dark: String, val spotifyBody: String, val appleMusicBody: String
)

internal object LevyraPrismStrings {
    private val english = PrismCopy(
        "Deep black, quiet surfaces and a single raspberry accent. Artwork leads, controls stay calm.",
        "Appearance", "Player layout", "Translucent effects", "Night", "Light",
        "Automatic", "Reduced",
        "Graphite slabs, sharp diagonals and a bold band that takes its color from the cover.",
        "Accent", "From cover", "Red",
        "Bright paper surfaces, deep ink tabs that slide in from the edges and a single rose spark for what is playing.",
        "Dark",
        "Unofficial theme. Near-black surfaces, one green accent, dense shortcut grids and headers and a player tinted by the artwork. Not affiliated with or endorsed by Spotify.",
        "Unofficial theme. White and black system surfaces, one red accent, editorial shelves, a floating tab bar and a player painted by the artwork. Not affiliated with or endorsed by Apple."
    )
    private val italian = PrismCopy(
        "Nero profondo, superfici quiete e un unico accento raspberry. Le copertine guidano, i comandi restano calmi.",
        "Aspetto", "Layout player", "Effetti traslucidi", "Night", "Light",
        "Automatico", "Ridotti",
        "Lastre grafite, diagonali nette e una banda decisa che prende il colore dalla copertina.",
        "Accento", "Dalla copertina", "Rosso",
        "Superfici chiare come carta, linguette d'inchiostro che entrano dai bordi e un solo tocco rosa per ciò che suona.",
        "Scuro",
        "Tema non ufficiale. Superfici quasi nere, un solo accento verde, griglie di scorciatoie compatte e intestazioni e player tinti dalla copertina. Non affiliato né approvato da Spotify.",
        "Tema non ufficiale. Superfici di sistema bianche e nere, un solo accento rosso, scaffali editoriali, una barra delle schede flottante e un player dipinto dalla copertina. Non affiliato né approvato da Apple."
    )
    fun forCode(code: String): PrismCopy = if (code == "it") italian else english
}
