package com.luc4n3x.levyra.data.hqaudio

internal object AlternativeSearchPlan {
    const val MAX_PASSES = 3

    private val whitespace = Regex("\\s+")
    private val rawArtistSeparators = Regex(
        "\\s*(?:,|&|;|\\sx\\s|\\sfeat\\.?\\s|\\sft\\.?\\s|\\sfeaturing\\s|\\swith\\s)\\s*",
        RegexOption.IGNORE_CASE
    )

    fun queries(query: AlternativeTrackQuery): List<String> {
        val title = clean(query.title)
        val primaryArtist = clean(query.artist).split(rawArtistSeparators).firstOrNull().orEmpty()
        val titleIdentity = AlternativeTrackText.title(query.title)
        val albumIdentity = AlternativeTrackText.album(query.album)
        val includeAlbum = !albumIdentity.isBlank && albumIdentity.core != titleIdentity.core
        val canonical = (listOf(titleIdentity.fullNormalized) + AlternativeTrackText.artistNames(query.artist))
            .joinToString(" ")
        return buildList {
            if (includeAlbum) add("$title $primaryArtist ${clean(query.album)}")
            add("$title $primaryArtist")
            add(canonical)
        }
            .map { it.replace(whitespace, " ").trim() }
            .filter { it.isNotBlank() }
            .distinctBy(AlternativeTrackText::normalize)
            .take(MAX_PASSES)
    }

    private fun clean(value: String): String =
        AlternativeTrackText.decodeHtmlEntities(value).replace(whitespace, " ").trim()
}
