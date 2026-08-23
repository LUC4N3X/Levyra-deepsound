package com.luc4n3x.levyra.domain

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
        assertEquals(signed, highResolutionPlayerArtworkUrl(signed))
    }

    @Test
    fun youtubeVideoFramesAreNotRewritten() {
        val url = "https://i.ytimg.com/vi/dQw4w9WgXcQ/maxresdefault.jpg?sqp=-oaymwEcCNA&rs=AOn4CLA"
        assertEquals(url, LevyraPersonalOrbit.upscaledArtworkUrl(url))
        assertEquals(url, highResolutionPlayerArtworkUrl(url))
    }

    @Test
    fun urlsWithoutSizeTokenAreLeftUntouched() {
        val url = "https://lh3.googleusercontent.com/aAbBcC"
        assertEquals(url, LevyraPersonalOrbit.upscaledArtworkUrl(url))
    }

    @Test
    fun otherProvidersKeepTheirOwnUpgradeRules() {
        assertEquals(
            "https://i.scdn.co/image/ab67616d0000b273abc",
            highResolutionPlayerArtworkUrl("https://i.scdn.co/image/ab67616d00001e02abc")
        )
        assertEquals(
            "https://is1-ssl.mzstatic.com/image/thumb/Music/abc/1200x1200bb.jpg",
            highResolutionPlayerArtworkUrl("https://is1-ssl.mzstatic.com/image/thumb/Music/abc/300x300bb.jpg")
        )
    }

    @Test
    fun playerArtworkUsesTheSharedGoogleRule() {
        assertEquals(
            "https://lh3.googleusercontent.com/aAbBcC=w1200-h675",
            highResolutionPlayerArtworkUrl("https://lh3.googleusercontent.com/aAbBcC=w640-h360")
        )
    }
}
