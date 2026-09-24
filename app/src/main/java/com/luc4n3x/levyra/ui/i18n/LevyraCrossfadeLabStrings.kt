package com.luc4n3x.levyra.ui.i18n

internal data class CrossfadeLabCopy(
    val title: String,
    val outgoing: String,
    val incoming: String,
    val equalPower: String,
    val off: String,
    val fixedDuration: String,
    val adaptiveDuration: String,
    val gaplessRequired: String,
    val sameReleasePolicy: String
)

internal fun crossfadeLabCopy(code: String): CrossfadeLabCopy = when (code) {
    "it" -> CrossfadeLabCopy(
        title = "Curve Lab",
        outgoing = "In uscita",
        incoming = "In entrata",
        equalPower = "Potenza costante",
        off = "Crossfade disattivato",
        fixedDuration = "Durata fissa",
        adaptiveDuration = "AutoMix adatta la durata",
        gaplessRequired = "Attiva la riproduzione gapless per usare il crossfade",
        sameReleasePolicy = "I passaggi consecutivi dello stesso album restano gapless"
    )
    "es" -> CrossfadeLabCopy(
        title = "Laboratorio de curvas",
        outgoing = "Salida",
        incoming = "Entrada",
        equalPower = "Potencia constante",
        off = "Crossfade desactivado",
        fixedDuration = "Duración fija",
        adaptiveDuration = "AutoMix adapta la duración",
        gaplessRequired = "Activa la reproducción sin pausas para usar el crossfade",
        sameReleasePolicy = "Las pistas consecutivas del mismo álbum siguen sin pausas"
    )
    "fr" -> CrossfadeLabCopy(
        title = "Laboratoire de courbes",
        outgoing = "Sortie",
        incoming = "Entrée",
        equalPower = "Puissance constante",
        off = "Fondu enchaîné désactivé",
        fixedDuration = "Durée fixe",
        adaptiveDuration = "AutoMix adapte la durée",
        gaplessRequired = "Activez la lecture sans blanc pour utiliser le fondu enchaîné",
        sameReleasePolicy = "Les pistes consécutives d’un même album restent sans blanc"
    )
    "de" -> CrossfadeLabCopy(
        title = "Kurvenlabor",
        outgoing = "Ausgehend",
        incoming = "Eingehend",
        equalPower = "Konstante Leistung",
        off = "Crossfade deaktiviert",
        fixedDuration = "Feste Dauer",
        adaptiveDuration = "AutoMix passt die Dauer an",
        gaplessRequired = "Aktiviere Gapless-Wiedergabe, um Crossfade zu verwenden",
        sameReleasePolicy = "Aufeinanderfolgende Titel desselben Albums bleiben gapless"
    )
    "pt" -> CrossfadeLabCopy(
        title = "Laboratório de curvas",
        outgoing = "Saída",
        incoming = "Entrada",
        equalPower = "Potência constante",
        off = "Crossfade desativado",
        fixedDuration = "Duração fixa",
        adaptiveDuration = "O AutoMix adapta a duração",
        gaplessRequired = "Ativa a reprodução sem pausas para usar o crossfade",
        sameReleasePolicy = "Faixas consecutivas do mesmo álbum continuam sem pausas"
    )
    else -> CrossfadeLabCopy(
        title = "Curve lab",
        outgoing = "Outgoing",
        incoming = "Incoming",
        equalPower = "Equal power",
        off = "Crossfade off",
        fixedDuration = "Fixed duration",
        adaptiveDuration = "AutoMix adapts the duration",
        gaplessRequired = "Enable gapless playback to use crossfade",
        sameReleasePolicy = "Consecutive tracks from the same album remain gapless"
    )
}
