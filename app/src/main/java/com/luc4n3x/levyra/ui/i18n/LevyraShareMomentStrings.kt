package com.luc4n3x.levyra.ui.i18n

internal data class ShareMomentCopy(
    val action: String,
    val from: String
)

internal fun LevyraStrings.shareMomentCopy(): ShareMomentCopy = when (code) {
    "it" -> ShareMomentCopy(action = "Condividi da qui", from = "da")
    "es" -> ShareMomentCopy(action = "Compartir desde aquí", from = "desde")
    "fr" -> ShareMomentCopy(action = "Partager à partir d'ici", from = "à partir de")
    "de" -> ShareMomentCopy(action = "Ab hier teilen", from = "ab")
    "pt" -> ShareMomentCopy(action = "Partilhar a partir daqui", from = "a partir de")
    else -> ShareMomentCopy(action = "Share from here", from = "from")
}
