package com.luc4n3x.levyra.domain

import java.util.Locale
import kotlin.math.abs

object LevyraPersonalOrbit {
    const val DISPLAY_LIMIT = 20

    private const val RECORDING_DURATION_TOLERANCE_MS = 12_000L

    private val squareArtWidthHeightPattern = Regex("=w\\d+-h\\d+")
    private val squareArtSizePattern = Regex("=s\\d+")
    private val youtubeVideoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val youtubeVideoUrlPatterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("/(?:shorts|embed|live|vi)/([A-Za-z0-9_-]{11})")
    )
    private val bracketAnnotationPattern = Regex(
        """(?i)[(\[]\s*(?:(?:official\s+)?(?:music\s+)?(?:video|audio)|lyrics?|visuali[sz]er|feat\.?|ft\.?|featuring)[^)\]]*[)\]]"""
    )
    private val trailingFeaturePattern = Regex("""(?i)\b(?:feat\.?|ft\.?|featuring)\b.*$""")
    private val displayAnnotationPattern = Regex(
        """(?i)\b(?:(?:official\s+)?(?:music\s+)?(?:video|audio)|lyrics?|visuali[sz]er)\b"""
    )
    // Android regexes are Unicode-aware by default and reject the unsupported (?U) flag.
    private val artistSeparatorPattern = Regex(
        """(?:(?<=\s)(?:feat\.?|featuring|ft\.?|and|with|e|ed|y|et|und|[,&;+])(?=\s)|(?<=[\p{L}\p{M}\p{N}])[,;&+](?=\s))""",
        RegexOption.IGNORE_CASE
    )
    private val nonMusicWordPattern = Regex("""[^\p{L}\p{M}\p{N}\s]""")
    private val whitespacePattern = Regex("""\s+""")
    private val officialArtworkHosts = listOf(
        "mzstatic.com",
        "dzcdn.net",
        "deezer.com/images/cover",
        "i.scdn.co/image",
        "mosaic.scdn.co"
    )

    private data class RecordingFingerprint(
        val isrc: String,
        val title: String,
        val artists: List<String>,
        val durationMs: Long
    ) {
        val primaryArtist: String = artists.firstOrNull().orEmpty()
        val artistSet: Set<String> = artists.toSet()
    }

    private data class NormalizedTasteSeed(
        val artists: Set<String>,
        val album: String,
        val moodTags: Set<String>
    )

    private data class RankedTrack(
        val track: Track,
        val affinity: Int,
        val metadata: Int,
        val title: String
    )

    fun build(
        currentTrack: Track?,
        recentSearches: List<Track>,
        favorites: List<Track>,
        tracks: List<Track>,
        homeSections: List<HomeSection>,
        charts: List<Track>,
        cachedOrbit: List<Track> = emptyList(),
        limit: Int = DISPLAY_LIMIT,
        languageCode: String = LevyraLanguageCatalog.deviceDefault()
    ): List<Track> {
        val max = limit.coerceAtLeast(1)
        val normalizedLanguage = LevyraLanguageCatalog.normalize(languageCode)
        val donorPool = buildList {
            addAll(charts)
            addAll(tracks)
            addAll(homeSections.flatMap { it.tracks })
            addAll(favorites)
            addAll(cachedOrbit)
            currentTrack?.let { add(it) }
            addAll(recentSearches)
        }
        val artworkDonors = buildArtworkDonors(donorPool)

        fun enriched(track: Track): Track {
            val clean = withoutVideoArtwork(track)
            val donor = artworkDonors[normalizedMusicTitle(track.title)]
                ?.firstOrNull { sameRecording(it, track) }
                ?: return clean
            return withoutVideoArtwork(preferAlbumArtwork(clean, donor))
        }

        fun viable(source: List<Track>): List<Track> = distinctRecordings(
            source.asSequence()
                .map(::enriched)
                .filter(::isReliableMusicCandidate)
                .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
                .toList()
        )

        val playbackHistory = viable(buildList {
            currentTrack?.let { add(it) }
            addAll(recentSearches)
        })
        val restoredOrbit = viable(cachedOrbit)
        val favoritesPool = viable(favorites)
        val fallbackTracks = viable(donorPool)
        val tasteSeeds = distinctRecordings(playbackHistory + favoritesPool + restoredOrbit)
        val normalizedTasteSeeds = tasteSeeds.map { seed ->
            NormalizedTasteSeed(
                artists = normalizedArtists(seed.artist).toSet(),
                album = normalizedMusicTitle(seed.album),
                moodTags = seed.moodTags.map { it.lowercase(Locale.ROOT) }.toSet()
            )
        }

        val orderedFallback = fallbackTracks
            .map { candidate ->
                RankedTrack(
                    track = candidate,
                    affinity = tasteAffinity(candidate, normalizedTasteSeeds, normalizedLanguage),
                    metadata = recordingMetadataScore(candidate),
                    title = normalizedMusicTitle(candidate.title)
                )
            }
            .sortedWith(
                compareByDescending<RankedTrack> { it.affinity }
                    .thenByDescending { it.metadata }
                    .thenBy { it.title }
            )
            .map(RankedTrack::track)

        val selected = ArrayList<Track>(max)

        fun addTracks(source: List<Track>, predicate: (Track) -> Boolean = { true }) {
            if (selected.size >= max) return
            source.forEach { candidate ->
                if (selected.size >= max) return
                if (!predicate(candidate)) return@forEach
                val duplicateIndex = selected.indexOfFirst { sameRecording(it, candidate) }
                if (duplicateIndex < 0) {
                    selected += candidate
                } else {
                    selected[duplicateIndex] = mergeRecordingMetadata(selected[duplicateIndex], candidate)
                }
            }
        }

        addTracks(playbackHistory)
        addTracks(favoritesPool)
        addTracks(restoredOrbit)
        addTracks(orderedFallback) {
            isLanguagePreferred(it, normalizedLanguage) && hasSquareAlbumArtwork(it)
        }
        addTracks(orderedFallback) {
            !isClearlyForeignForLanguage(it, normalizedLanguage) && hasSquareAlbumArtwork(it)
        }
        addTracks(orderedFallback) { hasSquareAlbumArtwork(it) }
        addTracks(orderedFallback) {
            isLanguagePreferred(it, normalizedLanguage) && hasAnyArtwork(it)
        }
        addTracks(orderedFallback) {
            !isClearlyForeignForLanguage(it, normalizedLanguage) && hasAnyArtwork(it)
        }
        addTracks(orderedFallback) { hasAnyArtwork(it) }
        addTracks(orderedFallback)
        return distinctRecordings(selected).take(max)
    }

    fun prepareForOrbit(track: Track, donors: List<Track>): Track {
        val clean = withoutVideoArtwork(track.copy(streamUrl = "", videoStreamUrl = ""))
        val donor = donors.asSequence()
            .filter { sameRecording(it, track) }
            .filter(::hasSquareAlbumArtwork)
            .maxByOrNull(::artworkScore)
            ?: return clean
        return withoutVideoArtwork(preferAlbumArtwork(clean, donor))
    }

    fun withoutVideoArtwork(track: Track): Track {
        val thumbnail = track.thumbnailUrl.takeUnless(::isVideoFrameArtworkUrl).orEmpty()
        val large = track.largeThumbnailUrl.takeUnless(::isVideoFrameArtworkUrl).orEmpty()
        if (thumbnail.isBlank() && large.isBlank()) {
            val fallback = track.largeThumbnailUrl.trim()
                .ifBlank { track.thumbnailUrl.trim() }
                .ifBlank { youtubeFallbackArtwork(track).orEmpty() }
            return if (fallback.isBlank()) track else track.copy(thumbnailUrl = fallback, largeThumbnailUrl = fallback)
        }
        return if (thumbnail == track.thumbnailUrl && large == track.largeThumbnailUrl) {
            track
        } else {
            track.copy(thumbnailUrl = thumbnail, largeThumbnailUrl = large)
        }
    }

    fun stableKey(track: Track): String = track.id.takeIf(String::isNotBlank)
        ?: "${track.title.trim().lowercase(Locale.ROOT)}|${track.artist.trim().lowercase(Locale.ROOT)}"

    fun identityKey(track: Track): String {
        val fingerprint = recordingFingerprint(track)
        val artist = fingerprint.artists.sorted().joinToString("|")
        return if (fingerprint.title.isNotBlank() && artist.isNotBlank()) {
            "$artist|${fingerprint.title}"
        } else {
            stableKey(track)
        }
    }

    fun sameRecording(first: Track, second: Track): Boolean =
        fingerprintsMatch(recordingFingerprint(first), recordingFingerprint(second))

    fun distinctRecordings(tracks: List<Track>): List<Track> {
        val result = ArrayList<Track>(tracks.size)
        val fingerprints = ArrayList<RecordingFingerprint>(tracks.size)
        val isrcIndices = HashMap<String, Int>()
        val titleIndices = HashMap<String, MutableList<Int>>()

        tracks.forEach { candidate ->
            val fingerprint = recordingFingerprint(candidate)
            val existingIndex = fingerprint.isrc.takeIf(String::isNotBlank)
                ?.let(isrcIndices::get)
                ?: titleIndices[fingerprint.title]
                    ?.firstOrNull { index -> fingerprintsMatch(fingerprints[index], fingerprint) }
                ?: -1

            if (existingIndex < 0) {
                val index = result.size
                result += candidate
                fingerprints += fingerprint
                if (fingerprint.isrc.isNotBlank()) isrcIndices[fingerprint.isrc] = index
                titleIndices.getOrPut(fingerprint.title) { ArrayList() }.add(index)
            } else {
                val merged = mergeRecordingMetadata(result[existingIndex], candidate)
                val mergedFingerprint = recordingFingerprint(merged)
                result[existingIndex] = merged
                fingerprints[existingIndex] = mergedFingerprint
                if (mergedFingerprint.isrc.isNotBlank()) isrcIndices[mergedFingerprint.isrc] = existingIndex
            }
        }
        return result
    }

    fun hasAnyArtwork(track: Track): Boolean =
        track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()

    fun hasSquareAlbumArtwork(track: Track): Boolean =
        sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
            .map(String::trim)
            .filter(String::isNotBlank)
            .any(::isSquareAlbumArtworkUrl)

    fun hasVideoFrameArtwork(track: Track): Boolean =
        sequenceOf(track.thumbnailUrl, track.largeThumbnailUrl)
            .map(String::trim)
            .filter(String::isNotBlank)
            .any(::isVideoFrameArtworkUrl)

    fun youtubeFallbackArtwork(track: Track): String? =
        youtubeVideoId(track)?.let { "https://i.ytimg.com/vi/$it/hqdefault.jpg" }

    fun isVideoFrameArtworkUrl(url: String): Boolean {
        val lower = url.lowercase(Locale.ROOT)
        return lower.contains("/vi/") ||
            lower.contains("/vi_webp/") ||
            lower.contains("ytimg.com/an_webp") ||
            lower.contains("hqdefault") ||
            lower.contains("mqdefault") ||
            lower.contains("sddefault") ||
            lower.contains("maxresdefault") ||
            lower.contains("hq720") ||
            lower.endsWith("default.jpg") ||
            lower.endsWith("default.webp")
    }

    fun preferAlbumArtwork(primary: Track, donor: Track): Track {
        val donorArtwork = albumArtworkUrl(donor) ?: return primary
        if (hasSquareAlbumArtwork(primary) && artworkScore(primary) >= artworkScore(donor)) return primary
        return primary.copy(thumbnailUrl = donorArtwork, largeThumbnailUrl = donorArtwork)
    }

    private fun buildArtworkDonors(tracks: List<Track>): Map<String, List<Track>> = tracks
        .asSequence()
        .filter(::isReliableMusicCandidate)
        .filter(::hasSquareAlbumArtwork)
        .groupBy { normalizedMusicTitle(it.title) }
        .mapValues { (_, candidates) -> candidates.sortedByDescending(::artworkScore) }

    private fun artworkScore(track: Track): Int {
        val url = albumArtworkUrl(track).orEmpty().lowercase(Locale.ROOT)
        var score = 0
        if (hasSquareAlbumArtwork(track)) score += 100
        if (url.contains("mzstatic.com")) score += 35
        if (url.contains("dzcdn.net") || url.contains("deezer.com")) score += 30
        if (url.contains("scdn.co")) score += 28
        if (url.contains("googleusercontent.com") || url.contains("ggpht.com")) score += 22
        if (squareArtWidthHeightPattern.containsMatchIn(url) || squareArtSizePattern.containsMatchIn(url)) score += 15
        if (track.largeThumbnailUrl.isNotBlank()) score += 8
        if (track.thumbnailUrl.isNotBlank()) score += 4
        if (hasVideoFrameArtwork(track)) score -= 90
        return score
    }

    private fun albumArtworkUrl(track: Track): String? =
        sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
            .map(String::trim)
            .firstOrNull(::isSquareAlbumArtworkUrl)

    private fun youtubeVideoId(track: Track): String? {
        val fromUrl = youtubeVideoUrlPatterns.asSequence()
            .mapNotNull { it.find(track.videoUrl)?.groupValues?.getOrNull(1) }
            .firstOrNull(youtubeVideoIdPattern::matches)
        if (fromUrl != null) return fromUrl
        val counterpart = track.counterpartVideoId.trim().takeIf(youtubeVideoIdPattern::matches)
        if (counterpart != null) return counterpart
        return track.id.trim().takeIf(youtubeVideoIdPattern::matches)
    }

    private fun isSquareAlbumArtworkUrl(url: String): Boolean {
        if (url.isBlank() || isVideoFrameArtworkUrl(url)) return false
        val lower = url.lowercase(Locale.ROOT)
        if (officialArtworkHosts.any(lower::contains)) return true
        val squareSized = squareArtWidthHeightPattern.containsMatchIn(url) || squareArtSizePattern.containsMatchIn(url)
        return squareSized && (lower.contains("googleusercontent.com") || lower.contains("ggpht.com"))
    }

    private fun recordingFingerprint(track: Track): RecordingFingerprint = RecordingFingerprint(
        isrc = normalizedIsrc(track.isrc),
        title = normalizedMusicTitle(track.title),
        artists = normalizedArtists(track.artist),
        durationMs = track.durationMs.coerceAtLeast(0L)
    )

    private fun fingerprintsMatch(first: RecordingFingerprint, second: RecordingFingerprint): Boolean {
        if (first.isrc.isNotBlank() && second.isrc.isNotBlank()) return first.isrc == second.isrc
        if (first.title.isBlank() || first.title != second.title) return false
        if (first.artistSet.isEmpty() || second.artistSet.isEmpty()) return false
        val artistMatch = first.primaryArtist == second.primaryArtist ||
            first.artistSet.containsAll(second.artistSet) ||
            second.artistSet.containsAll(first.artistSet)
        if (!artistMatch) return false
        if (first.durationMs > 0L && second.durationMs > 0L &&
            abs(first.durationMs - second.durationMs) > RECORDING_DURATION_TOLERANCE_MS
        ) return false
        return true
    }

    private fun normalizedMusicTitle(value: String): String = value
        .lowercase(Locale.ROOT)
        .replace(bracketAnnotationPattern, " ")
        .replace(trailingFeaturePattern, " ")
        .replace(displayAnnotationPattern, " ")
        .replace(nonMusicWordPattern, " ")
        .replace(whitespacePattern, " ")
        .trim()

    private fun normalizedArtists(value: String): List<String> = value
        .lowercase(Locale.ROOT)
        .replace(artistSeparatorPattern, "|")
        .split('|')
        .map { artist -> artist.replace(nonMusicWordPattern, " ").replace(whitespacePattern, " ").trim() }
        .filter(String::isNotBlank)
        .distinct()

    private fun normalizedIsrc(value: String): String =
        value.uppercase(Locale.ROOT).filter(Char::isLetterOrDigit)

    private fun mergeRecordingMetadata(first: Track, second: Track): Track {
        val preferred = if (recordingMetadataScore(second) > recordingMetadataScore(first)) second else first
        val fallback = if (preferred === first) second else first
        return preferred.copy(
            videoUrl = preferred.videoUrl.ifBlank { fallback.videoUrl },
            thumbnailUrl = preferred.thumbnailUrl.ifBlank { fallback.thumbnailUrl },
            largeThumbnailUrl = preferred.largeThumbnailUrl.ifBlank { fallback.largeThumbnailUrl },
            isrc = preferred.isrc.ifBlank { fallback.isrc },
            upc = preferred.upc.ifBlank { fallback.upc },
            releaseDate = preferred.releaseDate.ifBlank { fallback.releaseDate },
            albumBrowseId = preferred.albumBrowseId.ifBlank { fallback.albumBrowseId },
            artistBrowseIds = preferred.artistBrowseIds.ifEmpty { fallback.artistBrowseIds },
            counterpartVideoId = preferred.counterpartVideoId.ifBlank { fallback.counterpartVideoId },
            videoType = preferred.videoType.ifBlank { fallback.videoType },
            metadataProvider = preferred.metadataProvider.ifBlank { fallback.metadataProvider },
            metadataConfidence = maxOf(preferred.metadataConfidence, fallback.metadataConfidence),
            canonicalAlbumUrl = preferred.canonicalAlbumUrl.ifBlank { fallback.canonicalAlbumUrl }
        )
    }

    private fun recordingMetadataScore(track: Track): Int {
        var score = track.metadataConfidence.coerceIn(0, 100)
        if (track.isrc.isNotBlank()) score += 120
        if (track.counterpartVideoId.isNotBlank()) score += 100
        if (track.videoUrl.isNotBlank()) score += 50
        if (track.albumBrowseId.isNotBlank()) score += 35
        if (hasSquareAlbumArtwork(track)) score += 30
        if (track.largeThumbnailUrl.isNotBlank()) score += 12
        return score
    }

    private fun tasteAffinity(
        track: Track,
        seeds: List<NormalizedTasteSeed>,
        languageCode: String
    ): Int {
        if (seeds.isEmpty()) {
            return (if (isLanguagePreferred(track, languageCode)) 80 else 0) + recordingMetadataScore(track)
        }
        val candidateArtists = normalizedArtists(track.artist).toSet()
        val candidateAlbum = normalizedMusicTitle(track.album)
        val candidateMoods = track.moodTags.map { it.lowercase(Locale.ROOT) }.toSet()
        val artistHits = seeds.count { seed -> candidateArtists.intersect(seed.artists).isNotEmpty() }
        val albumHits = seeds.count { seed -> seed.album == candidateAlbum && candidateAlbum.isNotBlank() }
        val moodHits = seeds.sumOf { seed -> candidateMoods.intersect(seed.moodTags).size }
        return artistHits * 180 + albumHits * 70 + moodHits * 18 +
            (if (isLanguagePreferred(track, languageCode)) 55 else 0) +
            recordingMetadataScore(track)
    }

    fun isLanguagePreferred(track: Track, languageCode: String): Boolean {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        if (normalized == "en") return true
        if (track.moodTags.any { it.equals("local", ignoreCase = true) }) return true
        val lookup = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase(Locale.ROOT)
        val artistMatches = LevyraContentLocales.artistSuggestions(normalized).any { artist ->
            val key = artist.lowercase(Locale.ROOT)
            key.isNotBlank() && lookup.contains(key)
        }
        return artistMatches || languageMarkers(normalized).any { marker -> lookup.contains(marker) }
    }

    fun isClearlyForeignForLanguage(track: Track, languageCode: String): Boolean {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        if (normalized == "en" || isLanguagePreferred(track, normalized)) return false
        val lookup = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase(Locale.ROOT)
        return globalEnglishMarkers.any { marker -> lookup.contains(marker) }
    }

    private fun languageMarkers(languageCode: String): List<String> {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        val localeMarkers = when (normalized) {
            "it" -> listOf("italia", "italian", "italiano", "italiana")
            "es" -> listOf("españa", "espanol", "español", "latino", "reggaeton")
            "fr" -> listOf("france", "français", "francaise", "française")
            "de" -> listOf("deutsch", "deutschland", "deutschrap")
            "pt" -> listOf("portugal", "brasil", "brasileiro", "brasileira", "funk", "sertanejo")
            "nl" -> listOf("nederland", "nederlands")
            "pl" -> listOf("polsk", "polska")
            "ro" -> listOf("românia", "romania", "românesc")
            "el" -> listOf("ελλην", "greek")
            "sv" -> listOf("svensk", "sverige")
            "da" -> listOf("dansk", "danmark")
            "cs" -> listOf("česk", "cesk", "česko")
            "uk" -> listOf("україн", "ukrain")
            "ru" -> listOf("русск", "россия", "russian")
            "tr" -> listOf("türk", "turk", "türkiye")
            "ar" -> listOf("عربي", "عربية", "العالم العربي")
            "zh" -> listOf("华语", "中文", "国语", "华人")
            "ja" -> listOf("日本", "邦楽", "日本語", "j-pop")
            "ko" -> listOf("한국", "국내", "한국어", "k-pop")
            "hi" -> listOf("भारत", "भारतीय", "हिंदी", "बॉलीवुड")
            "id" -> listOf("indonesia", "bahasa indonesia", "musik indonesia")
            "vi" -> listOf("việt nam", "nhạc việt", "tiếng việt", "v-pop")
            "th" -> listOf("ไทย", "เพลงไทย", "ภาษาไทย", "t-pop")
            "fil" -> listOf("pilipinas", "pilipino", "tagalog", "opm", "p-pop", "pinoy")
            "he" -> listOf("ישראל", "ישראלי", "ישראלית", "עברית", "מוזיקה ישראלית")
            else -> emptyList()
        }
        return (localeMarkers + LevyraContentLocales.artistSuggestions(normalized).map { it.lowercase(Locale.ROOT) }).distinct()
    }

    private val globalEnglishMarkers = listOf(
        "queen", "the weeknd", "dua lipa", "nirvana", "eminem", "michael jackson",
        "linkin park", "coldplay", "imagine dragons", "billie eilish", "taylor swift",
        "drake", "travis scott", "post malone", "ariana grande", "kendrick lamar",
        "bruno mars", "harry styles", "miley cyrus", "glass animals", "daft punk", "m83", "a-ha"
    )

    fun isReliableMusicCandidate(track: Track): Boolean {
        val title = track.title.trim()
        val artist = track.artist.trim()
        if (title.length < 2 || artist.length < 2) return false
        if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
        return !isLikelyPlaylistOrCompilation(track)
    }

    private fun isLikelyPlaylistOrCompilation(track: Track): Boolean {
        val combined = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase(Locale.ROOT)
        val markers = listOf(
            "playlist", "mix", "top hit", "top hits", "hit italiane", "canzoni italiane",
            "musica italiana", "éxitos", "música española", "música latina", "chansons françaises",
            "musique française", "deutsche musik", "deutschrap mix", "música brasileira",
            "funk brasileiro mix", "nederlandse hits", "polskie hity", "hituri românia",
            "ελληνικά hits", "svenska hits", "danske hits", "české hity", "українські хіти",
            "estate mix", "summer mix", "best of", "compilation", "classifica", "chart", "charts",
            "radio edit", "sped up", "slowed", "nightcore"
        )
        return markers.any { marker -> combined.contains(marker) }
    }
}
