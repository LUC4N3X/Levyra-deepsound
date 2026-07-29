from __future__ import annotations

from pathlib import Path


def replace_once(relative: str, old: str, new: str) -> None:
    path = Path(relative)
    content = path.read_text(encoding="utf-8")
    count = content.count(old)
    if count != 1:
        raise SystemExit(f"Expected exactly one match in {relative}, found {count}: {old[:100]!r}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")


# Player visibility must never change Home geometry or mutate content above the viewport.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    """    val spotlightCandidate = remember(spotlightCandidates, stableSpotlightId, state.currentTrack?.id) {
        spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull { it.track.id != state.currentTrack?.id }
            ?: spotlightCandidates.firstOrNull()
    }
""",
    """    val spotlightCandidate = remember(spotlightCandidates, stableSpotlightId) {
        spotlightCandidates.firstOrNull { it.track.id == stableSpotlightId }
            ?: spotlightCandidates.firstOrNull()
    }
""",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/ui/LevyraApp.kt",
    "        contentPadding = PaddingValues(top = 8.dp, bottom = if (state.currentTrack != null) 188.dp else 104.dp),\n",
    """        // The mini player overlays Home: reserve one fixed inset so play, pause and close never
        // re-anchor the LazyColumn or move the Top 50 viewport.
        contentPadding = PaddingValues(top = 8.dp, bottom = 188.dp),
""",
)

# Carry only allowlisted public Spotify CDN artwork into Android.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "import java.io.InputStream\nimport java.nio.charset.StandardCharsets\n",
    "import java.io.InputStream\nimport java.net.URI\nimport java.nio.charset.StandardCharsets\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    " * The source credential and source artwork never enter the app. A single process-wide instance owns\n * the remote refresh, memory snapshot and AtomicFile cache so Home and Android Auto cannot race.\n",
    " * The source credential never enters the app. Public Spotify CDN artwork is carried in the catalog\n * so the Top 50 row and player can display the exact same cover. A single process-wide instance owns\n * the remote refresh, memory snapshot and AtomicFile cache so Home and Android Auto cannot race.\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    '            val releaseDate = album?.optString("releaseDate").orEmpty().trim()\n            val identity = chartIdentity("$title|$artist")\n',
    '            val releaseDate = album?.optString("releaseDate").orEmpty().trim()\n            val artwork = sourceArtworkUrl(item.optString("artworkUrl"))\n            val identity = chartIdentity("$title|$artist")\n',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    '                thumbnailUrl = "",\n                largeThumbnailUrl = "",\n',
    '                thumbnailUrl = artwork,\n                largeThumbnailUrl = artwork,\n',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "    private fun parseInstant(value: String): Long? {\n",
    '''    private fun sourceArtworkUrl(value: String): String {
        val normalized = value.trim()
        if (normalized.isBlank()) return ""
        val uri = runCatching { URI(normalized) }.getOrNull() ?: return ""
        val host = uri.host?.lowercase(Locale.ROOT).orEmpty()
        if (!uri.scheme.equals("https", ignoreCase = true) || uri.userInfo != null || uri.port != -1) return ""
        val allowed = host == "i.scdn.co" || host.endsWith(".scdn.co") ||
            host == "image-cdn-ak.spotifycdn.com"
        return normalized.takeIf { allowed }.orEmpty()
    }

    private fun parseInstant(value: String): Long? {
''',
)

# Editorial rows already contain the requested Spotify cover. Do not replace it with
# Apple/Deezer/Qobuz artwork before presenting the Top 50.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartsRepository.kt",
    """    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> {
        val ranked = selectTopSongs(country, limit)
        if (ranked.isEmpty()) return ranked
        return chartArtworkResolver.enrich(ranked, country)
    }
""",
    """    private suspend fun fetchTopSongs(country: String, limit: Int): List<Track> {
        val ranked = selectTopSongs(country, limit)
        if (ranked.isEmpty()) return ranked
        val hasEditorialArtwork = ranked.all { track ->
            track.source.equals("Levyra Editorial", ignoreCase = true) && track.thumbnailUrl.isNotBlank()
        }
        if (hasEditorialArtwork) return ranked
        return chartArtworkResolver.enrich(ranked, country)
    }
""",
)

# Collector contract and tests: publish the Spotify cover, but never source page URLs or IDs.
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '                        "images": [{"url": "https://image.example/album.jpg"}],\n',
    '                        "images": [{"url": "https://i.scdn.co/image/test-album-cover"}],\n',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "isrc" not in track\n    assert "artworkUrl" not in track\n    assert "artworkUrl" not in track["album"]\n',
    '    assert "isrc" not in track\n    assert track["artworkUrl"] == "https://i.scdn.co/image/test-album-cover"\n    assert "artworkUrl" not in track["album"]\n',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "scdn.co" not in serialized\n',
    '    assert "i.scdn.co/image/test-album-cover" in serialized\n',
)

# Android parser and playback tests enforce Spotify artwork from row to player.
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    "    fun parsesCountryChartWithoutSourceArtworkOrFakeIsrc() {\n",
    "    fun parsesCountryChartWithSpotifyArtworkAndWithoutFakeIsrc() {\n",
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '        assertTrue(track.thumbnailUrl.isBlank())\n        assertTrue(track.largeThumbnailUrl.isBlank())\n',
    '        assertEquals("https://i.scdn.co/image/spotify-album-cover", track.thumbnailUrl)\n        assertEquals(track.thumbnailUrl, track.largeThumbnailUrl)\n',
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '                  "explicit": true\n',
    '                  "explicit": true,\n                  "artworkUrl": "https://i.scdn.co/image/spotify-album-cover"\n',
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt",
    '            thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",\n',
    '            thumbnail = "https://i.scdn.co/image/spotify-album-cover",\n',
)

replace_once(
    "tools/levyra-editorial/README.md",
    "- The public catalog contains ranking position, title, artist, album, release date, duration and explicit flag only.\n- Source artwork, source URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android obtains artwork independently and keeps the exact same artwork when the selected row opens in the player.\n",
    "- The public catalog contains ranking position, title, artist, album, release date, duration, explicit flag and the public Spotify CDN artwork URL.\n- Source page URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android accepts artwork only from allowlisted HTTPS Spotify CDN hosts and keeps that exact image when the selected row opens in the player.\n- Devices contact Spotify's image CDN for Top 50 covers; authentication and the session credential remain confined to GitHub Actions.\n",
)

print("Home scroll and Spotify artwork continuity patch applied")
