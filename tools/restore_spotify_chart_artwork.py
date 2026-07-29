from pathlib import Path


def replace_once(path: str, old: str, new: str) -> None:
    file = Path(path)
    content = file.read_text(encoding="utf-8")
    if old not in content:
        raise SystemExit(f"Expected text not found in {path}: {old[:80]!r}")
    file.write_text(content.replace(old, new, 1), encoding="utf-8")


# Android consumes only allowlisted Spotify public CDN artwork and preserves it in playback.
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "import java.io.InputStream\nimport java.nio.charset.StandardCharsets\n",
    "import java.io.InputStream\nimport java.net.URI\nimport java.nio.charset.StandardCharsets\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    " * The source credential and source artwork never enter the app. A single process-wide instance owns\n",
    " * The source credential never enters the app. Public Spotify CDN artwork is carried in the catalog so\n * the Top 50 row and player can display the exact same cover. A single process-wide instance owns\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "            val releaseDate = album?.optString(\"releaseDate\").orEmpty().trim()\n            val identity = chartIdentity(\"$title|$artist\")\n",
    "            val releaseDate = album?.optString(\"releaseDate\").orEmpty().trim()\n            val artwork = sourceArtworkUrl(item.optString(\"artworkUrl\"))\n            val identity = chartIdentity(\"$title|$artist\")\n",
)
replace_once(
    "app/src/main/java/com/luc4n3x/levyra/data/EditorialChartsRepository.kt",
    "                thumbnailUrl = \"\",\n                largeThumbnailUrl = \"\",\n",
    "                thumbnailUrl = artwork,\n                largeThumbnailUrl = artwork,\n",
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
        val allowed = host == "i.scdn.co" || host.endsWith(".scdn.co") || host == "image-cdn-ak.spotifycdn.com"
        return normalized.takeIf { allowed }.orEmpty()
    }

    private fun parseInstant(value: String): Long? {
''',
)

# Python test fixture now represents a real Spotify CDN cover and verifies publication.
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '"images": [{"url": "https://image.example/album.jpg"}],',
    '"images": [{"url": "https://i.scdn.co/image/test-album-cover"}],',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "artworkUrl" not in track\n    assert "artworkUrl" not in track["album"]\n',
    '    assert track["artworkUrl"] == "https://i.scdn.co/image/test-album-cover"\n    assert "artworkUrl" not in track["album"]\n',
)
replace_once(
    "tools/levyra-editorial/tests/test_collector.py",
    '    assert "scdn.co" not in serialized\n',
    '    assert "i.scdn.co/image/test-album-cover" in serialized\n',
)

# Android parser contract: Spotify artwork enters the Track and unsafe hosts are rejected.
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    "    fun parsesCountryChartWithoutSourceArtworkOrFakeIsrc() {\n",
    "    fun parsesCountryChartWithSpotifyArtworkAndWithoutFakeIsrc() {\n",
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    "        assertTrue(track.thumbnailUrl.isBlank())\n        assertTrue(track.largeThumbnailUrl.isBlank())\n",
    "        assertEquals(\"https://i.scdn.co/image/spotify-album-cover\", track.thumbnailUrl)\n        assertEquals(track.thumbnailUrl, track.largeThumbnailUrl)\n",
)
replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialCatalogParserTest.kt",
    '                  "explicit": true\n',
    '                  "explicit": true,\n                  "artworkUrl": "https://i.scdn.co/image/spotify-album-cover"\n',
)

replace_once(
    "app/src/test/java/com/luc4n3x/levyra/data/EditorialArtworkContinuityTest.kt",
    'thumbnail = "https://is1-ssl.mzstatic.com/image/thumb/source/600x600bb.jpg",',
    'thumbnail = "https://i.scdn.co/image/spotify-album-cover",',
)

# Documentation matches the requested source and continuity behavior.
replace_once(
    "tools/levyra-editorial/README.md",
    "- The public catalog contains ranking position, title, artist, album, release date, duration and explicit flag only.\n- Source artwork, source URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android obtains artwork independently and keeps the exact same artwork when the selected row opens in the player.\n",
    "- The public catalog contains ranking position, title, artist, album, release date, duration, explicit flag and the public Spotify CDN artwork URL.\n- Source page URLs, source IDs and unsupported ISRC values are deliberately omitted.\n- Android accepts artwork only from allowlisted HTTPS Spotify CDN hosts and keeps that exact image when the selected row opens in the player.\n",
)

# Remove this one-shot migration script from the resulting commit.
Path(__file__).unlink()
