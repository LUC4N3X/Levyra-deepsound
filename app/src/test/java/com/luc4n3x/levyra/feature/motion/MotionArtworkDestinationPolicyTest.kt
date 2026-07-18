package com.luc4n3x.levyra.feature.motion

import okhttp3.HttpUrl.Companion.toHttpUrl
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.InetAddress

class MotionArtworkDestinationPolicyTest {
    @Test
    fun allowsOnlyApprovedProviderMediaHosts() {
        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "tidal-video-cover",
                "https://resources.tidal.com/videos/a/b/c/d/e/1280x1280.mp4".toHttpUrl()
            )
        )
        assertTrue(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "apple-motion",
                "https://video-ssl.itunes.apple.com/itunes-assets/master.m3u8".toHttpUrl()
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "tidal-video-cover",
                "https://example.com/video.mp4".toHttpUrl()
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "apple-motion",
                "http://video-ssl.itunes.apple.com/video.m3u8".toHttpUrl()
            )
        )
        assertFalse(
            MotionArtworkDestinationPolicy.isAllowedUrl(
                "apple-motion",
                "https://video-ssl.itunes.apple.com:8443/video.m3u8".toHttpUrl()
            )
        )
    }

    @Test
    fun rejectsPrivateAndLocalDestinations() {
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("127.0.0.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("192.168.1.20")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("169.254.1.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("100.64.0.1")))
        assertFalse(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("fc00::1")))
        assertTrue(MotionArtworkDestinationPolicy.isPublicAddress(InetAddress.getByName("1.1.1.1")))
    }
}
