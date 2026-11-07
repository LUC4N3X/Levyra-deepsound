package com.luc4n3x.levyra.feature.discovery

import com.luc4n3x.levyra.domain.Track
import java.util.Locale

internal object DiscoveryFilmScores {
    private val markers = listOf(
        Regex("""\boriginal (motion picture|film|movie|series|television|tv)?\s*(score|soundtrack)\b"""),
        Regex("""\b(motion picture|film|movie) (score|soundtrack)\b"""),
        Regex("""\b(music )?from the (motion picture|film|movie|original motion picture|netflix|series)\b"""),
        Regex("""\bsoundtrack\b"""),
        Regex("""\bo\.?s\.?t\.?\b"""),
        Regex("""\bfilm score\b"""),
        Regex("""\barchive collection\b"""),
        Regex("""\boriginal score\b"""),
        Regex("""(\s-\s|\()from (the )?["“]?\p{L}""")
    )

    private val filmAlbum = Regex("""\bfilms?\b""")

    fun matches(track: Track): Boolean {
        val text = "${track.title} ${track.album}".lowercase(Locale.ROOT)
        return markers.any { it.containsMatchIn(text) } || filmAlbum.containsMatchIn(track.album.lowercase(Locale.ROOT))
    }
}
