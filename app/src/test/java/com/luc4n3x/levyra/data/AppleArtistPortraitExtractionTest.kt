package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class AppleArtistPortraitExtractionTest {
    private val imagePattern = Regex("""https://is\d+-ssl\.mzstatic\.com/image/thumb/[A-Za-z0-9._/-]{10,180}\.(?:png|jpg|jpeg)""")
    private val sizePattern = Regex("""\d{2,4}x\d{2,4}(?:cw|bb)""")
    private val openGraphPattern = Regex("""<meta[^>]+property=["']og:image["'][^>]+content=["']([^"']+)["']""", RegexOption.IGNORE_CASE)

    private fun reference(html: String): String {
        val candidates = imagePattern.findAll(html).map(MatchResult::value).filter(::isAllowedAppleArtistArtworkUrl).toList()
        if (candidates.isEmpty()) return ""
        val openGraph = openGraphPattern.find(html)?.groupValues?.getOrNull(1)?.takeIf(::isAllowedAppleArtistArtworkUrl)
        val portrait = openGraph ?: candidates.firstOrNull { it.contains("AMCArtistImages", ignoreCase = true) } ?: return ""
        return sizePattern.replace(portrait, "1200x1200bb")
    }

    private val image = "https://is1-ssl.mzstatic.com/image/thumb/AMCArtistImages221/v4/aa/bb/cc/photo.jpg/486x486bb.jpg"
    private val other = "https://is4-ssl.mzstatic.com/image/thumb/Music116/v4/11/22/33/cover.png/300x300bb.png"

    private val pages = listOf(
        "",
        "<html><head></head><body>no images</body></html>",
        "<head><meta property=\"og:image\" content=\"$image\"></head><img src=\"$other\">",
        "<head><META CONTENT=\"x\" PROPERTY='og:image' content='$other'></head><img src=\"$image\">",
        "<head><meta name=\"og:image\" value=\"none\"><meta property=\"og:image\" content=\"$other\"></head>$image",
        "<p>og:image mention without tag</p><img src=\"$other\"><img src=\"$image\">",
        "<img src=\"https://is1-ssl.mzstatic.com/image/thumb/x.jpg\"> https://is https://isZ $image$other",
        "<meta property=\"og:image\" content=\"http://example.com/a.jpg\"><img src=\"$image\">",
        "<div>" + "x".repeat(300_000) + "</div><meta property=\"og:image\" content=\"$image\">" + "y".repeat(300_000) + other,
        "<meta name=\"viewport\" content=\"x\"><script>" + "var a='og:image';".repeat(20_000) + "</script>$image"
    )

    @Test
    fun matchesTheOriginalWholePageRegexExtraction() {
        pages.forEach { html -> assertEquals(reference(html), extractAppleArtistPortrait(html)) }
    }
}
