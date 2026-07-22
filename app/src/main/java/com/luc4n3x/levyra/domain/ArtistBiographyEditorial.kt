package com.luc4n3x.levyra.domain

internal data class ArtistBiographyEditorial(
    val lead: String,
    val body: List<String>
)

internal fun artistBiographyEditorial(text: String): ArtistBiographyEditorial {
    val normalized = text
        .replace("\r\n", "\n")
        .replace('\r', '\n')
        .trim()
    val explicitParagraphs = normalized
        .split(Regex("\\n\\s*\\n+"))
        .map { it.replace(Regex("\\s+"), " ").trim() }
        .filter { it.isNotBlank() }
    val paragraphs = if (explicitParagraphs.size >= 2) {
        explicitParagraphs
    } else {
        val sentences = normalized
            .replace(Regex("\\s+"), " ")
            .split(Regex("(?<=[.!?])\\s+(?=[\\p{Lu}\\d])"))
            .map(String::trim)
            .filter { it.isNotBlank() }
        buildList {
            val current = StringBuilder()
            sentences.forEach { sentence ->
                if (current.isNotEmpty() && current.length + sentence.length + 1 > 520) {
                    add(current.toString())
                    current.clear()
                }
                if (current.isNotEmpty()) current.append(' ')
                current.append(sentence)
            }
            if (current.isNotEmpty()) add(current.toString())
        }
    }
    val safeParagraphs = paragraphs.ifEmpty { listOf(normalized) }
    return ArtistBiographyEditorial(
        lead = safeParagraphs.first(),
        body = safeParagraphs.drop(1)
    )
}
