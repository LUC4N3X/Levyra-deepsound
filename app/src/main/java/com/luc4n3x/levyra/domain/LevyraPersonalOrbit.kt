package com.luc4n3x.levyra.domain

object LevyraPersonalOrbit {
    const val DISPLAY_LIMIT = 12

    private val squareArtWidthHeightPattern = Regex("=w\\d+-h\\d+")
    private val squareArtSizePattern = Regex("=s\\d+")

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
        val playedTracks = buildList {
            currentTrack?.let { add(it) }
            addAll(recentSearches)
            addAll(favorites)
        }
            .asSequence()
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .distinctBy { stableKey(it) }
            .toList()
        val playedKeys = playedTracks.mapTo(mutableSetOf()) { stableKey(it) }
        val feedTracks = buildList {
            addAll(tracks)
            addAll(homeSections.flatMap { it.tracks })
            addAll(charts)
            addAll(cachedOrbit)
        }
            .asSequence()
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .toList()
        val localeTracks = feedTracks.filter { isLanguagePreferred(it, normalizedLanguage) }
        val neutralTracks = feedTracks.filter { !isLanguagePreferred(it, normalizedLanguage) && !isClearlyForeignForLanguage(it, normalizedLanguage) }
        val foreignFallbackTracks = feedTracks.filter { isClearlyForeignForLanguage(it, normalizedLanguage) }
        val ordered = buildList {
            addAll(playedTracks)
            addAll(localeTracks)
            addAll(neutralTracks)
            addAll(foreignFallbackTracks)
        }
            .asSequence()
            .filter { isReliableMusicCandidate(it) }
            .distinctBy { stableKey(it) }
            .toList()
        if (ordered.isEmpty()) return emptyList()
        val selected = ArrayList<Track>(max)
        val used = HashSet<String>()

        fun addMatching(predicate: (Track) -> Boolean) {
            if (selected.size >= max) return
            ordered.forEach { track ->
                if (selected.size >= max) return
                val key = stableKey(track)
                if (key !in used && predicate(track)) {
                    selected += track
                    used += key
                }
            }
        }

        addMatching { track -> stableKey(track) in playedKeys && hasSquareAlbumArtwork(track) }
        addMatching { track -> isLanguagePreferred(track, normalizedLanguage) && hasSquareAlbumArtwork(track) }
        addMatching { track -> stableKey(track) in playedKeys && hasAnyArtwork(track) }
        addMatching { track -> isLanguagePreferred(track, normalizedLanguage) && hasAnyArtwork(track) }
        addMatching { track -> stableKey(track) in playedKeys }
        addMatching { track -> isLanguagePreferred(track, normalizedLanguage) }
        addMatching { track -> !isClearlyForeignForLanguage(track, normalizedLanguage) && hasSquareAlbumArtwork(track) }
        addMatching { track -> !isClearlyForeignForLanguage(track, normalizedLanguage) && hasAnyArtwork(track) }
        addMatching { track -> hasSquareAlbumArtwork(track) }
        addMatching { track -> hasAnyArtwork(track) }
        addMatching { true }
        return selected.take(max)
    }

    fun stableKey(track: Track): String {
        return track.id.takeIf { it.isNotBlank() } ?: "${track.title.trim().lowercase()}|${track.artist.trim().lowercase()}"
    }

    fun hasAnyArtwork(track: Track): Boolean {
        return track.thumbnailUrl.isNotBlank() || track.largeThumbnailUrl.isNotBlank()
    }

    fun hasSquareAlbumArtwork(track: Track): Boolean {
        val url = track.thumbnailUrl.ifBlank { track.largeThumbnailUrl }.trim()
        if (url.isBlank()) return false
        val lower = url.lowercase()
        val looksLikeVideoFrame = lower.contains("/vi/") ||
            lower.contains("/vi_webp/") ||
            lower.contains("ytimg.com/an_webp") ||
            lower.contains("hqdefault") ||
            lower.contains("mqdefault") ||
            lower.contains("sddefault") ||
            lower.contains("maxresdefault") ||
            lower.contains("hq720") ||
            lower.endsWith("default.jpg") ||
            lower.endsWith("default.webp")
        if (looksLikeVideoFrame) return false
        return lower.contains("googleusercontent.com") ||
            lower.contains("ggpht.com") ||
            squareArtWidthHeightPattern.containsMatchIn(url) ||
            squareArtSizePattern.containsMatchIn(url)
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
