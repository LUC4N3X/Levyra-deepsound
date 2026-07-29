from __future__ import annotations

from pathlib import Path

ROOT = Path(__file__).resolve().parents[1]


def replace_once(relative: str, old: str, new: str) -> None:
    path = ROOT / relative
    content = path.read_text(encoding="utf-8")
    count = content.count(old)
    if count != 1:
        raise SystemExit(f"Expected one match in {relative}, found {count}: {old[:80]!r}")
    path.write_text(content.replace(old, new, 1), encoding="utf-8")


# Publish the source album artwork selected by the editorial playlist.
replace_once(
    "tools/levyra-editorial/levyra_editorial/models.py",
    '                "explicit": self.explicit,\n',
    '                "explicit": self.explicit,\n                "artworkUrl": self.artwork_url,\n',
)

# The Android catalog parser accepts only Spotify image CDN URLs and uses that same URL
# for both thumbnail sizes so the chart row and player start from one immutable artwork.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    " * The source credential and source artwork never enter the app. A single process-wide instance owns\n * the remote refresh, memory snapshot and AtomicFile cache so Home and Android Auto cannot race.\n",
    " * The source credential never enters the app. Source album artwork is intentionally carried through\n * the public catalog and preserved into playback. A single process-wide instance owns the refresh,\n * memory snapshot and AtomicFile cache so Home and Android Auto cannot race.\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    '            val releaseDate = album?.optString("releaseDate").orEmpty().trim()\n            val identity = chartIdentity("$title|$artist")\n',
    '            val releaseDate = album?.optString("releaseDate").orEmpty().trim()\n            val artwork = validatedEditorialArtwork(item.optString("artworkUrl"))\n            val identity = chartIdentity("$title|$artist")\n',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    '                thumbnailUrl = "",\n                largeThumbnailUrl = "",\n',
    '                thumbnailUrl = artwork,\n                largeThumbnailUrl = artwork,\n',
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "    private fun parseArtists(items: JSONArray?): String {\n",
    '''    private fun validatedEditorialArtwork(value: String): String {
        val normalized = value.trim()
        if (!normalized.startsWith("https://", ignoreCase = true)) return ""
        val host = runCatching {
            java.net.URI(normalized).host.orEmpty().lowercase(Locale.ROOT)
        }.getOrDefault("")
        val allowed = host == "scdn.co" || host.endsWith(".scdn.co") ||
            host == "spotifycdn.com" || host.endsWith(".spotifycdn.com")
        return normalized.takeIf { allowed }.orEmpty()
    }

    private fun parseArtists(items: JSONArray?): String {
''',
)

# Do not replace Spotify artwork with Apple/Deezer/Qobuz artwork during chart enrichment.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    " * Replaces cropped YouTube video frames with verified release artwork without changing chart order\n * or the native YouTube playback identity. Cached matches are instant; unresolved entries retain the\n * YouTube thumbnail only when every official provider fails or the strict chart artwork budget ends.\n",
    " * Replaces cropped YouTube video frames with verified release artwork for legacy chart sources.\n * Editorial rows already carry their Spotify album artwork and bypass this resolver so the exact image\n * shown in Top 50 is preserved when playback is resolved.\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    "                    async {\n                        val key = identityKey(track)\n",
    "                    async {\n                        if (preservesEditorialSourceArtwork(track)) return@async\n                        val key = identityKey(track)\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    "        tracks.map { track ->\n            resolved[identityKey(track)]?.applyTo(track) ?: cached(identityKey(track))?.applyTo(track) ?: track\n        }\n    }\n\n    private suspend fun resolve",
    "        tracks.map { track ->\n            if (preservesEditorialSourceArtwork(track)) {\n                track\n            } else {\n                resolved[identityKey(track)]?.applyTo(track) ?: cached(identityKey(track))?.applyTo(track) ?: track\n            }\n        }\n    }\n\n    private fun preservesEditorialSourceArtwork(track: Track): Boolean =\n        track.source.equals(EDITORIAL_SOURCE, ignoreCase = true) &&\n            track.thumbnailUrl.isNotBlank() &&\n            track.largeThumbnailUrl.isNotBlank()\n\n    private suspend fun resolve",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/ChartOfficialArtworkResolver.kt",
    '        const val CACHE_NAME = "levyra_chart_official_artwork"\n',
    '        const val CACHE_NAME = "levyra_chart_official_artwork"\n        const val EDITORIAL_SOURCE = "Levyra Editorial"\n',
)

# Python contract tests now require the Spotify CDN artwork and still reject source IDs/URLs.
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '                        "images": [{"url": "https://image.example/album.jpg"}],\n',
    '                        "images": [{"url": "https://i.scdn.co/image/ab67616d00001e02testalbum"}],\n',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "isrc" not in track\n    assert "artworkUrl" not in track\n    assert "artworkUrl" not in track["album"]\n',
    '    assert "isrc" not in track\n    assert track["artworkUrl"] == "https://i.scdn.co/image/ab67616d00001e02testalbum"\n    assert "artworkUrl" not in track["album"]\n',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "scdn.co" not in serialized\n',
    '    assert "i.scdn.co/image/" in serialized\n',
)

# Android parser and player continuity tests explicitly use Spotify artwork.
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    "    fun parsesCountryChartWithoutSourceArtworkOrFakeIsrc() {\n",
    "    fun parsesCountryChartWithSpotifyArtworkAndNoFakeIsrc() {\n",
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '        assertTrue(track.thumbnailUrl.isBlank())\n        assertTrue(track.largeThumbnailUrl.isBlank())\n',
    '        assertEquals(SPOTIFY_ARTWORK, track.thumbnailUrl)\n        assertEquals(SPOTIFY_ARTWORK, track.largeThumbnailUrl)\n',
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '                   "explicit": true\n',
    '                   "explicit": true,\n                   "artworkUrl": "$SPOTIFY_ARTWORK"\n',
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '        val NOW: Long = Instant.parse("2026-07-29T20:00:00Z").toEpochMilli()\n',
    '        val NOW: Long = Instant.parse("2026-07-29T20:00:00Z").toEpochMilli()\n        const val SPOTIFY_ARTWORK = "https://i.scdn.co/image/ab67616d00001e02testalbum"\n',
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt",
    '            thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",\n',
    '            thumbnail = "https://i.scdn.co/image/ab67616d00001e02testalbum",\n',
)

# Document the intentional source-artwork behavior and direct CDN contact.
replace_once(
    "tools/levyra-editorial/README.md",
    "- The public catalog contains ranking position, title, artist, album, release date, duration and explicit flag only.\n- Source artwork, source URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android obtains artwork independently and keeps the exact same artwork when the selected row opens in the player.\n",
    "- The public catalog contains ranking position, title, artist, album, release date, duration, explicit flag and the Spotify album artwork URL.\n- Source page URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android accepts artwork only from Spotify image CDN hosts and preserves that exact URL when the selected row opens in the player.\n- Devices therefore contact Spotify's image CDN for Top 50 artwork; the authentication service and session credential remain confined to GitHub Actions.\n",
)

print("Spotify artwork continuity patch applied")
# Triggered after workflow registration.
