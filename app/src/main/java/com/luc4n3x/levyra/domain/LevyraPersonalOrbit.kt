package com.luc4n3x.levyra.domain

object LevyraPersonalOrbit {
    const val DISPLAY_LIMIT = 12

    private val squareArtWidthHeightPattern = Regex("=w\\d+-h\\d+")
    private val squareArtSizePattern = Regex("=s\\d+")
    private val youtubeVideoIdPattern = Regex("^[A-Za-z0-9_-]{11}$")
    private val youtubeVideoUrlPatterns = listOf(
        Regex("[?&]v=([A-Za-z0-9_-]{11})"),
        Regex("youtu\\.be/([A-Za-z0-9_-]{11})"),
        Regex("/(?:shorts|embed|live|vi)/([A-Za-z0-9_-]{11})")
    )
    private val officialArtworkHosts = listOf(
        "mzstatic.com",
        "dzcdn.net",
        "deezer.com/images/cover",
        "i.scdn.co/image",
        "mosaic.scdn.co"
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
            val donor = artworkDonors[identityKey(track)] ?: return clean
            return withoutVideoArtwork(preferAlbumArtwork(clean, donor))
        }

        val playbackHistory = buildList {
            currentTrack?.let { add(it) }
            addAll(recentSearches)
        }
            .asSequence()
            .map(::enriched)
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { identityKey(it) }
            .toList()

        val restoredOrbit = cachedOrbit
            .asSequence()
            .map(::enriched)
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { identityKey(it) }
            .toList()

        val fallbackTracks = donorPool
            .asSequence()
            .map(::enriched)
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { identityKey(it) }
            .toList()

        val localeTracks = fallbackTracks.filter { isLanguagePreferred(it, normalizedLanguage) }
        val neutralTracks = fallbackTracks.filter {
            !isLanguagePreferred(it, normalizedLanguage) && !isClearlyForeignForLanguage(it, normalizedLanguage)
        }
        val foreignFallbackTracks = fallbackTracks.filter { isClearlyForeignForLanguage(it, normalizedLanguage) }
        val orderedFallback = (localeTracks + neutralTracks + foreignFallbackTracks)
            .distinctBy { identityKey(it) }

        val selected = ArrayList<Track>(max)
        val used = HashSet<String>()

        fun addTracks(source: List<Track>, predicate: (Track) -> Boolean = { true }) {
            if (selected.size >= max) return
            source.forEach { track ->
                if (selected.size >= max) return
                val key = identityKey(track)
                if (key !in used && predicate(track)) {
                    selected += track
                    used += key
                }
            }
        }

        addTracks(playbackHistory)
        addTracks(restoredOrbit)
        addTracks(orderedFallback) { track -> isLanguagePreferred(track, normalizedLanguage) && hasSquareAlbumArtwork(track) }
        addTracks(orderedFallback) { track -> !isClearlyForeignForLanguage(track, normalizedLanguage) && hasSquareAlbumArtwork(track) }
        addTracks(orderedFallback) { track -> hasSquareAlbumArtwork(track) }
        addTracks(orderedFallback) { track -> isLanguagePreferred(track, normalizedLanguage) && hasAnyArtwork(track) }
        addTracks(orderedFallback) { track -> !isClearlyForeignForLanguage(track, normalizedLanguage) && hasAnyArtwork(track) }
        addTracks(orderedFallback) { track -> hasAnyArtwork(track) }
        addTracks(orderedFallback) { track -> isLanguagePreferred(track, normalizedLanguage) }
        addTracks(orderedFallback)
        return selected
    }

    fun prepareForOrbit(track: Track, donors: List<Track>): Track {
        val clean = withoutVideoArtwork(track.copy(streamUrl = "", videoStreamUrl = ""))
        val donor = donors
            .asSequence()
            .filter { identityKey(it) == identityKey(track) }
            .filter { hasSquareAlbumArtwork(it) }
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

    fun stableKey(track: Track): String {
        return track.id.takeIf { it.isNotBlank() } ?: "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
    }

    fun identityKey(track: Track): String {
        val title = normalizedMusicText(track.title)
        val artist = normalizedMusicText(track.artist)
        return if (title.isNotBlank() && artist.isNotBlank()) "$title|$artist" else stableKey(track)
    }

    fun hasAnyArtwork(track: Track): Boolean {
        return track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()
    }

    fun hasSquareAlbumArtwork(track: Track): Boolean {
        return sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any(::isSquareAlbumArtworkUrl)
    }

    fun hasVideoFrameArtwork(track: Track): Boolean {
        return sequenceOf(track.thumbnailUrl, track.largeThumbnailUrl)
            .map { it.trim() }
            .filter { it.isNotBlank() }
            .any(::isVideoFrameArtworkUrl)
    }

    fun youtubeFallbackArtwork(track: Track): String? {
        val videoId = youtubeVideoId(track) ?: return null
        return "https://i.ytimg.com/vi/$videoId/hqdefault.jpg"
    }

    fun isVideoFrameArtworkUrl(url: String): Boolean {
        val lower = url.lowercase()
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

    private fun buildArtworkDonors(tracks: List<Track>): Map<String, Track> {
        val map = LinkedHashMap<String, Track>()
        tracks.asSequence()
            .filter { isReliableMusicCandidate(it) }
            .filter { hasSquareAlbumArtwork(it) }
            .forEach { track ->
                val key = identityKey(track)
                val current = map[key]
                if (current == null || artworkScore(track) > artworkScore(current)) map[key] = track
            }
        return map
    }

    private fun artworkScore(track: Track): Int {
        val url = albumArtworkUrl(track).orEmpty().lowercase()
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

    private fun albumArtworkUrl(track: Track): String? {
        return sequenceOf(track.largeThumbnailUrl, track.thumbnailUrl)
            .map { it.trim() }
            .firstOrNull(::isSquareAlbumArtworkUrl)
    }

    private fun youtubeVideoId(track: Track): String? {
        val fromUrl = youtubeVideoUrlPatterns
            .asSequence()
            .mapNotNull { pattern -> pattern.find(track.videoUrl)?.groupValues?.getOrNull(1) }
            .firstOrNull { youtubeVideoIdPattern.matches(it) }
        if (fromUrl != null) return fromUrl
        return track.id.trim().takeIf { youtubeVideoIdPattern.matches(it) }
    }

    private fun isSquareAlbumArtworkUrl(url: String): Boolean {
        if (url.isBlank() || isVideoFrameArtworkUrl(url)) return false
        val lower = url.lowercase()
        if (officialArtworkHosts.any(lower::contains)) return true
        val squareSized = squareArtWidthHeightPattern.containsMatchIn(url) || squareArtSizePattern.containsMatchIn(url)
        return squareSized && (lower.contains("googleusercontent.com") || lower.contains("ggpht.com"))
    }

    private fun normalizedMusicText(value: String): String {
        return value.lowercase()
            .replace(Regex("""\([^)]*\)|\[[^]]*]"""), " ")
            .replace(Regex("""feat\.?|featuring|ft\.?"""), " ")
            .replace(Regex("""official audio|official video|lyrics?|visuali[sz]er|music video"""), " ")
            .replace(Regex("""[^\p{L}\p{M}\p{N}\s]"""), " ")
            .replace(Regex("""\s+"""), " ")
            .trim()
    }

    fun isLanguagePreferred(track: Track, languageCode: String): Boolean {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        if (normalized == "en") return true
        if (track.moodTags.any { it.equals("local", ignoreCase = true) }) return true
        val lookup = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
        val artistMatches = LevyraContentLocales.artistSuggestions(normalized).any { artist ->
            val key = artist.lowercase()
            key.isNotBlank() && lookup.contains(key)
        }
        if (artistMatches) return true
        return languageMarkers(normalized).any { marker -> lookup.contains(marker) }
    }

    fun isClearlyForeignForLanguage(track: Track, languageCode: String): Boolean {
        val normalized = LevyraLanguageCatalog.normalize(languageCode)
        if (normalized == "en" || isLanguagePreferred(track, normalized)) return false
        val lookup = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
        return globalEnglishMarkers.any { marker -> lookup.contains(marker) }
    }

    private fun languageMarkers(languageCode: String): List<String> {
        return when (LevyraLanguageCatalog.normalize(languageCode)) {
            "it" -> listOf(
                "italia",
                "italian",
                "sfera ebbasta",
                "lazza",
                "geolier",
                "marracash",
                "ultimo",
                "annalisa",
                "tedua",
                "ghali",
                "madame",
                "capo plaza",
                "mahmood",
                "fred de palma",
                "ernia",
                "pinguini tattici nucleari",
                "gue",
                "guè",
                "salmo",
                "elodie",
                "irama",
                "thasup",
                "massimo pericolo",
                "fabri fibra",
                "shiva",
                "tony effe",
                "rose villain",
                "angelina mango"
            )
            "es" -> listOf("españa", "espanol", "español", "latino", "reggaeton", "bad bunny", "rosalía", "karol g", "rauw alejandro")
            "fr" -> listOf("france", "français", "francaise", "française", "gazo", "aya nakamura", "ninho", "stromae", "sch")
            "de" -> listOf("deutsch", "deutschland", "apache 207", "raf camora", "luciano", "ufo361", "bonez mc")
            "pt" -> listOf("brasil", "brasileiro", "brasileira", "funk", "sertanejo", "anitta", "matuê", "luísa sonza")
            "nl" -> listOf("nederland", "nederlands", "frenna", "antoon", "boef", "ronnie flex")
            "pl" -> listOf("polsk", "sanah", "taco hemingway", "dawid podsiadło", "quebonafide")
            "ro" -> listOf("românia", "romania", "românesc", "inna", "the motans", "delia", "carla's dreams")
            "el" -> listOf("ελλην", "greek", "snik", "eleni foureira", "helena paparizou", "sakis rouvas")
            "sv" -> listOf("svensk", "sverige", "zara larsson", "veronica maggio", "hov1", "avicii")
            "da" -> listOf("dansk", "danmark", "gilli", "tobias rahim", "mø", "medina")
            "cs" -> listOf("česk", "cesk", "calin", "ewa farna", "ben cristovao", "viktor sheen")
            "uk" -> listOf("україн", "ukrain", "alyona alyona", "kalush", "jerry heil", "okean elzy")
            "ar" -> listOf("عربي", "عربية", "عمرو دياب", "نانسي عجرم", "ويجز", "مروان بابلو", "شيرين", "تامر حسني", "إليسا", "بلقيس")
            "zh" -> listOf("华语", "中文", "国语", "周杰伦", "邓紫棋", "薛之谦", "林俊杰", "陈奕迅", "王菲", "毛不易")
            else -> emptyList()
        }
    }

    private val globalEnglishMarkers = listOf(
        "queen",
        "the weeknd",
        "dua lipa",
        "nirvana",
        "eminem",
        "michael jackson",
        "linkin park",
        "coldplay",
        "imagine dragons",
        "billie eilish",
        "taylor swift",
        "drake",
        "travis scott",
        "post malone",
        "ariana grande",
        "kendrick lamar",
        "bruno mars",
        "harry styles",
        "miley cyrus",
        "glass animals",
        "daft punk",
        "m83",
        "a-ha"
    )

    fun isReliableMusicCandidate(track: Track): Boolean {
        val title = track.title.trim()
        val artist = track.artist.trim()
        if (title.length < 2 || artist.length < 2) return false
        if (artist.equals("YouTube Music", ignoreCase = true) || artist.equals("YouTube", ignoreCase = true)) return false
        return !isLikelyPlaylistOrCompilation(track)
    }

    private fun isLikelyPlaylistOrCompilation(track: Track): Boolean {
        val combined = listOf(track.title, track.artist, track.album).joinToString(" ").lowercase()
        val markers = listOf(
            "playlist",
            "mix",
            "top hit",
            "top hits",
            "hit italiane",
            "canzoni italiane",
            "musica italiana",
            "éxitos",
            "música española",
            "música latina",
            "chansons françaises",
            "musique française",
            "deutsche musik",
            "deutschrap mix",
            "música brasileira",
            "funk brasileiro mix",
            "nederlandse hits",
            "polskie hity",
            "hituri românia",
            "ελληνικά hits",
            "svenska hits",
            "danske hits",
            "české hity",
            "українські хіти",
            "estate mix",
            "summer mix",
            "best of",
            "compilation",
            "classifica",
            "chart",
            "charts",
            "radio edit",
            "sped up",
            "slowed",
            "nightcore"
        )
        return markers.any { combined.contains(it) }
    }
}
