package com.luc4n3x.levyra.feature.dearrow

import com.luc4n3x.levyra.domain.Track
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoMetadataEnhancerTest {

    @Test
    fun enhancesTitleAndThumbnailWhenBrandingAvailable() {
        val repository = DeArrowRepository(
            brandingSource = {
                DeArrowBranding(
                    titles = listOf(DeArrowTitle("Enhanced Title", locked = true, votes = 5, original = false)),
                    thumbnails = listOf(DeArrowThumbnail(timestamp = 4.0, locked = true, votes = 5, original = false))
                )
            }
        )
        val enhancer = VideoMetadataEnhancer(repository)
        val original = VideoMetadata("abcdefghijk", "Original Title", "https://original.example/thumb.jpg")

        val enhanced = runBlocking { enhancer.enhance(original) }

        assertEquals("Enhanced Title", enhanced.title)
        assertTrue(enhanced.thumbnailUrl.startsWith("https://dearrow-thumb.ajay.app"))
    }

    @Test
    fun returnsOriginalWhenNoBrandingIsAvailable() {
        val repository = DeArrowRepository(brandingSource = { null })
        val enhancer = VideoMetadataEnhancer(repository)
        val original = VideoMetadata("abcdefghijk", "Original Title", "https://original.example/thumb.jpg")

        val enhanced = runBlocking { enhancer.enhance(original) }

        assertEquals(original, enhanced)
    }

    @Test
    fun returnsOriginalWhenRepositoryIsDisabled() {
        val repository = DeArrowRepository(
            brandingSource = {
                DeArrowBranding(
                    titles = listOf(DeArrowTitle("Should Not Apply", locked = true, votes = 5, original = false)),
                    thumbnails = emptyList()
                )
            },
            enabled = false
        )
        val enhancer = VideoMetadataEnhancer(repository)
        val original = VideoMetadata("abcdefghijk", "Original Title", "https://original.example/thumb.jpg")

        val enhanced = runBlocking { enhancer.enhance(original) }

        assertEquals(original, enhanced)
    }

    @Test
    fun enhancerApiNeverAcceptsMusicalTrackType() {
        val declaredMethods = VideoMetadataEnhancer::class.java.declaredMethods
            .filter { !it.isSynthetic }
        assertTrue("Expected VideoMetadataEnhancer to declare at least one method", declaredMethods.isNotEmpty())

        val parameterTypes = declaredMethods.flatMap { it.parameterTypes.toList() }
        assertFalse(
            "VideoMetadataEnhancer must never accept a musical Track parameter",
            parameterTypes.contains(Track::class.java)
        )
        assertTrue(
            "VideoMetadataEnhancer.enhance must accept VideoMetadata",
            parameterTypes.contains(VideoMetadata::class.java)
        )
    }
}
