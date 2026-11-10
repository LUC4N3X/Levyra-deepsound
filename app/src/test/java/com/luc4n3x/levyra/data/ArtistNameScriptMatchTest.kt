package com.luc4n3x.levyra.data

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistNameScriptMatchTest {
    @Test
    fun pinnedArtistNameMatchesTheHeaderInAnotherScriptOrSpelling() {
        assertTrue(artistNameMatches("Okean Elzy", "Океан Ельзи"))
        assertTrue(artistNameMatches("Океан Эльзы", "Океан Ельзи"))
        assertTrue(artistNameMatches("Kino", "Кино"))
        assertTrue(artistNameMatches("Океан Ельзи", "Океан Ельзи"))
    }

    @Test
    fun differentArtistsStillDoNotMatch() {
        assertFalse(artistNameMatches("Okean Elzy", "Кино"))
        assertFalse(artistNameMatches("Земфира", "Zemlyane"))
        assertFalse(artistNameMatches("Gold", "Hold"))
    }
}
