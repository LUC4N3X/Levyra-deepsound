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
        val includeAlbum = !albumIdentity.isBlank &&
            albumIdentity.core != titleIdentity.core &&
            albumIdentity.core !in AlternativeTrackMatcher.untrustedAlbumNames
        val searchableTitle = if (titleIdentity.versionSignature.isEmpty()) titleIdentity.core else titleIdentity.fullNormalized
        val artistNames = AlternativeTrackText.artistNames(query.artist)
        val canonical = (listOf(searchableTitle) + artistNames).joinToString(" ")
        val base = cleanDistinct(
            buildList {
                if (includeAlbum) add("$title $primaryArtist ${clean(query.album)}")
                add("$title $primaryArtist")
                add(canonical)
            }
        )
        val seen = base.mapTo(hashSetOf(), AlternativeTrackText::normalize)
        val fallbacks = cleanDistinct(
            buildList {
                if (artistNames.size > 1) add("$searchableTitle ${artistNames.first()}")
                query.alternateReading?.let { alt ->
                    val altPrimary = clean(alt.artist).split(rawArtistSeparators).firstOrNull().orEmpty()
                    add("${clean(alt.title)} $altPrimary")
                }
            }
        )
            .filterNot { AlternativeTrackText.normalize(it) in seen }
            .take(MAX_PASSES - 1)
        return base.take((MAX_PASSES - fallbacks.size).coerceAtLeast(1)) + fallbacks
    }

    private fun cleanDistinct(raw: List<String>): List<String> =
        raw.map { it.replace(whitespace, " ").trim() }
            .filter { it.isNotBlank() }
            .distinctBy(AlternativeTrackText::normalize)

    fun titleOnly(query: AlternativeTrackQuery): String? {
        val identity = AlternativeTrackText.title(query.title)
        val text = (if (identity.versionSignature.isEmpty()) identity.core else identity.fullNormalized).trim()
        return text.takeIf { it.length >= MIN_TITLE_ONLY_LENGTH && queries(query).none { AlternativeTrackText.normalize(it) == AlternativeTrackText.normalize(text) } }
    }

    private const val MIN_TITLE_ONLY_LENGTH = 4

    private fun clean(value: String): String =
        AlternativeTrackText.decodeHtmlEntities(value).replace(whitespace, " ").trim()
}
