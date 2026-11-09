package com.luc4n3x.levyra.nexus.identity

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class MusicIdentityTextTest {
    @Test
    fun performanceVersionsRemainDistinctWhenCombinedWithAlbumEditions() {
        val versions = listOf("Karaoke", "Cover", "Live", "Acoustic", "Instrumental", "Remix", "Demo", "Reprise", "Reloaded", "3D Audio", "Sped Up", "Slowed", "Reverb")
        val editions = listOf("Deluxe", "Expanded", "Anniversary", "Remastered", "Single", "EP", "Soundtrack")
        versions.forEach { version ->
            editions.forEach { edition ->
                listOf("$version $edition", "$edition $version").forEach { descriptor ->
                    val actual = MusicIdentityText.album("After Hours ($descriptor)")
                    assertNotEquals(descriptor, MusicIdentityText.album("After Hours ($edition)"), actual)
                    assertTrue(descriptor, actual.core.contains(MusicIdentityText.normalize(version)))
                }
            }
        }
    }

    @Test
    fun genericVersionLabelsPreserveSpecificAlbumEditions() {
        listOf("Deluxe", "Expanded", "Anniversary", "Remastered", "Single", "EP", "Soundtrack").forEach { edition ->
            assertEquals(
                edition,
                MusicIdentityText.album("After Hours ($edition)"),
                MusicIdentityText.album("After Hours ($edition Version)")
            )
        }
    }

    @Test
    fun mixedDescriptorsPreserveEditionsAndCanonicalPerformanceIdentity() {
        listOf("Karaoke", "Cover", "Live", "Acoustic", "Remix", "Reprise").forEach { version ->
            val expected = MusicIdentityText.album("After Hours ($version) (Deluxe)")
            listOf("$version Deluxe", "Deluxe $version", "$version Deluxe Version", "$version Deluxe Edition").forEach { descriptor ->
                assertEquals(descriptor, expected, MusicIdentityText.album("After Hours ($descriptor)"))
            }
        }
    }

    @Test
    fun liveRecordingYearsAndVenuesRemainDistinct() {
        val album = MusicIdentityText.album("After Hours (Live at Wembley 2023 Deluxe)")
        assertTrue(album.core.contains("wembley 2023"))
        assertNotEquals(album, MusicIdentityText.album("After Hours (Live at Wembley 2024 Deluxe)"))
        assertNotEquals(album, MusicIdentityText.album("After Hours (Live at Glastonbury 2023 Deluxe)"))
    }

    @Test
    fun unspecifiedVersionsRemainPartOfTheAlbumIdentity() {
        val album = MusicIdentityText.album("After Hours (Festival Version)")
        assertTrue(album.core.contains("festival version"))
        assertTrue(album.editions.isEmpty())
    }
}
