package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicAlbumTitleTest {
    @Test
    fun rejectsPlaybackMetricsAndTrackCounts() {
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("23 Mln riproduzioni"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("232 milioni di ascolti"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("1.2B views"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("16 brani"))
        assertFalse(isPlausibleYoutubeMusicAlbumTitle("Reproducciones: 24 mil"))
    }

    @Test
    fun keepsRealNumericAndTextTitles() {
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("Alba"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("1989"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("4:44"))
        assertTrue(isPlausibleYoutubeMusicAlbumTitle("23"))
    }
}
