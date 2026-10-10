package com.luc4n3x.levyra.ui

import com.luc4n3x.levyra.ui.i18n.LevyraStrings

internal val LevyraStrings.preloadNextTrack: String
    get() = if (code == "it") "Precarica il brano successivo" else "Preload next track"

internal val LevyraStrings.preloadNextTrackSubtitle: String
    get() = if (code == "it") {
        "Prepara in anticipo il brano successivo per passaggi più rapidi. " +
            "Disattiva per ridurre l'attività di rete in background."
    } else {
        "Prepare the next track in advance for faster transitions. " +
            "Disable to reduce background network activity."
    }
