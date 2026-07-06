package com.luc4n3x.levyra.domain

object LevyraPersonalOrbit {
    const val DISPLAY_LIMIT = 8

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
        limit: Int = DISPLAY_LIMIT
    ): List<Track> {
        val max = limit.coerceAtLeast(1)
        val candidates = buildList {
            addAll(cachedOrbit)
            currentTrack?.let { add(it) }
            addAll(recentSearches)
            addAll(favorites)
            addAll(tracks)
            addAll(homeSections.flatMap { it.tracks })
            addAll(charts)
        }
            .asSequence()
            .filter { isReliableMusicCandidate(it) }
            .filter { it.title.isNotBlank() && it.artist.isNotBlank() }
            .toList()

        val squareArtwork = candidates
            .asSequence()
            .filter { hasSquareAlbumArtwork(it) }
            .distinctBy { stableKey(it) }
            .take(max)
            .toList()

        if (squareArtwork.size >= max) return squareArtwork

        val usedKeys = squareArtwork.mapTo(mutableSetOf()) { stableKey(it) }
        val fallbackArtwork = candidates
            .asSequence()
            .filter { stableKey(it) !in usedKeys }
            .filter { hasAnyArtwork(it) }
            .distinctBy { stableKey(it) }
            .take(max - squareArtwork.size)
            .toList()

        return squareArtwork + fallbackArtwork
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
