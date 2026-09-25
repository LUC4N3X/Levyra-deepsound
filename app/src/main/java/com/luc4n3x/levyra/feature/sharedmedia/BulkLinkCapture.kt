package com.luc4n3x.levyra.feature.sharedmedia

object BulkLinkCapture {
    const val MAX_LINKS = 100
    const val MAX_TRACKS = 500
    const val RESOLUTION_CONCURRENCY = 4

    private val urlPattern = Regex("https?://[^\\s<>\"']+", RegexOption.IGNORE_CASE)
    private val supportedKinds = setOf(SharedMediaKind.Video, SharedMediaKind.Playlist, SharedMediaKind.Album)

    fun extractUrls(text: String): List<String> = urlPattern.findAll(text)
        .map { it.value.trimEnd('.', ',', ';', ':', '!', ')', ']', '}') }
        .filter { it.length > "https://".length }
        .toList()

    fun request(urls: List<String>, classify: (String) -> SharedMediaRequest?): SharedMediaRequest {
        val unique = LinkedHashSet<String>()
        var duplicates = 0
        var unrecognized = (urls.size - MAX_LINKS).coerceAtLeast(0)
        urls.asSequence().take(MAX_LINKS).forEach { url ->
            val classified = classify(url)
            when {
                classified == null || classified.kind !in supportedKinds || classified.url.isBlank() -> unrecognized += 1
                !unique.add(classified.url) -> duplicates += 1
            }
        }
        return SharedMediaRequest(
            rawText = "",
            url = "",
            kind = SharedMediaKind.BulkLinks,
            bulkUrls = unique.toList(),
            bulkDetected = urls.size,
            bulkDuplicates = duplicates,
            bulkUnrecognized = unrecognized
        )
    }
}
