package com.luc4n3x.levyra.domain

import com.luc4n3x.levyra.data.fullResolutionArtworkUrl
import com.luc4n3x.levyra.ui.highResolutionPlayerArtworkUrl
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtworkUrlUpgradeTest {
    @Test
    fun squareMusicCoverKeepsItsRenderingOptions() {
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w1200-h1200-l90-rj",
            LevyraPersonalOrbit.upscaledArtworkUrl("https://lh3.googleusercontent.com/aAbBcC=w544-h544-l90-rj")
        )
    }

    @Test
    fun rectangularArtworkKeepsItsAspectRatio() {
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w1200-h675",
            LevyraPersonalOrbit.upscaledArtworkUrl("https://lh3.googleusercontent.com/aAbBcC=w1280-h720")
        )
    }

    @Test
    fun portraitArtworkIsScaledOnTheLongSide() {
        assertEquals(
            "https://yt3.googleusercontent.com/aAbBcC=w675-h1200",
            LevyraPersonalOrbit.upscaledArtworkUrl("https://yt3.googleusercontent.com/aAbBcC=w720-h1280")
        )
    }

    @Test
    fun avatarSizeTokenKeepsTheRemainingCropOptions() {
        assertEquals(
            "https://yt3.ggpht.com/ytc/aAbBcC=s1200-c-k-c0x00ffffff-no-rj",
            LevyraPersonalOrbit.upscaledArtworkUrl("https://yt3.ggpht.com/ytc/aAbBcC=s88-c-k-c0x00ffffff-no-rj")
        )
    }

    @Test
    fun signedUrlsAreLeftExactlyAsTheyAre() {
        val signed = "https://lh3.googleusercontent.com/aAbBcC=w120-h120?sqp=-oaymwEdCJUDENAFSFXyq4qpAw&rs=AOn4CLA=x1"
        assertEquals(signed, LevyraPersonalOrbit.upscaledArtworkUrl(signed))
        assertEquals(signed, fullResolutionArtworkUrl(signed))
        assertEquals(signed, highResolutionPlayerArtworkUrl(signed))
    }

    @Test
    fun youtubeVideoFramesAreNotRewritten() {
        val url = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg?sqp=-oaymwEcCNA&rs=AOn4CLA"
        assertEquals(url, LevyraPersonalOrbit.upscaledArtworkUrl(url))
        assertEquals(url, fullResolutionArtworkUrl(url))
        assertEquals(url, highResolutionPlayerArtworkUrl(url))
    }

    @Test
    fun urlsWithoutSizeTokenAreLeftUntouched() {
        val url = "https://lh3.googleusercontent.com/aAbBcC"
        assertEquals(url, LevyraPersonalOrbit.upscaledArtworkUrl(url))
        assertEquals(url, fullResolutionArtworkUrl(url))
    }

    @Test
    fun fullResolutionPolicyUpgradesKnownArtworkProviders() {
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273abc",
            fullResolutionArtworkUrl("https://i.scdn.co/image/ab67616d00001e02abc")
        )
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/abc/1200x1200bb.jpg",
            fullResolutionArtworkUrl("https://is1-ssl.mzstatic.com/image/thumb/Music/abc/300x300bb.jpg")
        )
        assertEquals(
            "https://e-cdns-images.dzcdn.net/images/cover/example/cover_xl/image.jpg",
            fullResolutionArtworkUrl("https://e-cdns-images.dzcdn.net/images/cover/example/cover_medium/image.jpg")
        )
    }

    @Test
    fun fullResolutionPolicyNeverDownscalesBetterSources() {
        val apple = "https://is1-ssl.mzstatic.com/image/thumb/Music/abc/1400x1400bb.jpg"
        val googleWide = "https://lh3.googleusercontent.com/aAbBcC=w1600-h900-l90-rj"
        val googleSquare = "https://yt3.ggpht.com/ytc/aAbBcC=s1600-c-k-c0x00ffffff-no-rj"

        assertEquals(apple, fullResolutionArtworkUrl(apple))
        assertEquals(googleWide, fullResolutionArtworkUrl(googleWide))
        assertEquals(googleSquare, fullResolutionArtworkUrl(googleSquare))
    }

    @Test
    fun playerArtworkUsesTheSharedFullResolutionPolicy() {
        val url = "https://lh3.googleusercontent.com/aAbBcC=w640-h360"
        assertEquals(fullResolutionArtworkUrl(url), highResolutionPlayerArtworkUrl(url))
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w1200-h675",
            highResolutionPlayerArtworkUrl(url)
        )
    }
}
