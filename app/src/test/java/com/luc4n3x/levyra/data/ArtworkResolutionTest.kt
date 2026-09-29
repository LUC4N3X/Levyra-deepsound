package com.luc4n3x.levyra.data

import coil3.size.Size
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkResolutionTest {
    @Test
    fun tierFollowsTheDisplayedPixelSize() {
        assertEquals(ArtworkTier.Thumbnail, ArtworkQualityPolicy.tierFor(96))
        assertEquals(ArtworkTier.Thumbnail, ArtworkQualityPolicy.tierFor(256))
        assertEquals(ArtworkTier.Card, ArtworkQualityPolicy.tierFor(300))
        assertEquals(ArtworkTier.Card, ArtworkQualityPolicy.tierFor(512))
        assertEquals(ArtworkTier.Detail, ArtworkQualityPolicy.tierFor(700))
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(1080))
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(2400))
    }

    @Test
    fun unknownSizeKeepsTheFullResolutionBehaviour() {
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(null as Int?))
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(0))
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(Size.ORIGINAL))
    }

    @Test
    fun tierUsesTheLongestResolvedSide() {
        assertEquals(ArtworkTier.Thumbnail, ArtworkQualityPolicy.tierFor(Size(160, 160)))
        assertEquals(ArtworkTier.Card, ArtworkQualityPolicy.tierFor(Size(480, 270)))
        assertEquals(ArtworkTier.Full, ArtworkQualityPolicy.tierFor(Size(1080, 2340)))
    }

    @Test
    fun smallSurfacesDoNotRequestHugeGoogleArtwork() {
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w256-h256-l90-rj",
            ArtworkUrlResolver.resolve("https://lh3.googleusercontent.com/aAbBcC=w1200-h1200-l90-rj", ArtworkTier.Thumbnail)
        )
        assertEquals(
            "https://yt3.ggpht.com/ytc/aAbBcC=s256-c-k-c0x00ffffff-no-rj",
            ArtworkUrlResolver.resolve("https://yt3.ggpht.com/ytc/aAbBcC=s900-c-k-c0x00ffffff-no-rj", ArtworkTier.Thumbnail)
        )
    }

    @Test
    fun largerSurfacesRequestHigherGoogleSources() {
        val small = "https://lh3.googleusercontent.com/aAbBcC=w120-h120-l90-rj"
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w512-h512-l90-rj",
            ArtworkUrlResolver.resolve(small, ArtworkTier.Card)
        )
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w768-h768-l90-rj",
            ArtworkUrlResolver.resolve(small, ArtworkTier.Detail)
        )
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w1200-h1200-l90-rj",
            ArtworkUrlResolver.resolve(small, ArtworkTier.Full)
        )
    }

    @Test
    fun appleArtworkIsSizedPerTier() {
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/abc/256x256bb.jpg",
            ArtworkUrlResolver.resolve("https://is1-ssl.mzstatic.com/image/thumb/Music/abc/1200x1200bb.jpg", ArtworkTier.Thumbnail)
        )
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/example/512x512bb.jpg",
            ArtworkUrlResolver.resolve("https://is1-ssl.mzstatic.com/image/thumb/example/{w}x{h}bb.jpg", ArtworkTier.Card)
        )
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/example/768x768bb-60.jpg",
            ArtworkUrlResolver.resolve("https://is1-ssl.mzstatic.com/image/thumb/example/100x100bb-60.jpg", ArtworkTier.Detail)
        )
    }

    @Test
    fun fixedSizeProvidersOnlyUpgradeWhenTheSurfaceNeedsIt() {
        val spotify = "https://i.scdn.co/image/ab67616d00001e02abc"
        assertEquals(spotify, ArtworkUrlResolver.resolve(spotify, ArtworkTier.Thumbnail))
        assertEquals("https://i.scdn.co/image/ab67616d0000b273abc", ArtworkUrlResolver.resolve(spotify, ArtworkTier.Card))

        val deezer = "https://e-cdns-images.dzcdn.net/images/cover/example/cover_medium/image.jpg"
        assertEquals(deezer, ArtworkUrlResolver.resolve(deezer, ArtworkTier.Thumbnail))
        assertEquals(
            "https://e-cdns-images.dzcdn.net/images/cover/example/cover_big/image.jpg",
            ArtworkUrlResolver.resolve(deezer, ArtworkTier.Card)
        )
        assertEquals(
            "https://e-cdns-images.dzcdn.net/images/cover/example/cover_xl/image.jpg",
            ArtworkUrlResolver.resolve(deezer, ArtworkTier.Detail)
        )
    }

    @Test
    fun unsupportedArtworkUrlsAreLeftUntouchedForEveryTier() {
        val urls = listOf(
            "https://i.ytimg.com/vi/abcdefghijk/hqdefault.jpg",
            "https://c.saavncdn.com/123/Album-English-2024-150x150.jpg",
            "https://example.com/cover.jpg",
            "https://cdn.example/googleusercontent.com/aAbBcC=w640-h360",
            "https://cdn.example/mzstatic.com/image/thumb/Music/abc/300x300bb.jpg",
            "https://lh3.googleusercontent.com/signed=w120-h120?sig=abc",
            "https://lh3.googleusercontent.com/noSizeToken"
        )
        urls.forEach { url ->
            ArtworkTier.entries.forEach { tier ->
                assertEquals("$url @ $tier", url, ArtworkUrlResolver.resolve(url, tier))
            }
        }
    }

    @Test
    fun localAndMalformedArtworkIsLeftUntouchedWithoutCrashing() {
        val values = listOf(
            "",
            "   ",
            "content://media/external/audio/albumart/42",
            "file:///storage/emulated/0/Music/cover.jpg",
            "not a url",
            "https://",
            "https://[broken",
            "http://lh3.googleusercontent.com/aAbBcC=w120-h120"
        )
        values.forEach { value ->
            ArtworkTier.entries.forEach { tier ->
                assertEquals("$value @ $tier", value.trim(), ArtworkUrlResolver.resolve(value, tier))
            }
        }
    }
}
