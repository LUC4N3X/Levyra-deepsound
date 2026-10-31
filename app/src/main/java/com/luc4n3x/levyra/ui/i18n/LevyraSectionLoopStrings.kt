package com.luc4n3x.levyra.ui.i18n

internal data class SectionLoopCopy(
    val action: String,
    val setEnd: String
)

internal fun LevyraStrings.sectionLoopCopy(): SectionLoopCopy = when (code) {
    "it" -> SectionLoopCopy(action = "Ripeti sezione", setEnd = "tocca per la fine")
    "es" -> SectionLoopCopy(action = "Repetir sección", setEnd = "toca para el final")
    "fr" -> SectionLoopCopy(action = "Boucler une section", setEnd = "touchez pour la fin")
    "de" -> SectionLoopCopy(action = "Abschnitt wiederholen", setEnd = "tippen für Ende")
    "pt" -> SectionLoopCopy(action = "Repetir secção", setEnd = "toque para o fim")
    else -> SectionLoopCopy(action = "Loop section", setEnd = "tap to set end")
}
