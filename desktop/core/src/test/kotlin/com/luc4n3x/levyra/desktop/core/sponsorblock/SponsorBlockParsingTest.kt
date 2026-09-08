package com.luc4n3x.levyra.desktop.core.sponsorblock

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SponsorBlockParsingTest {

    @Test
    fun theRequestedPrefixExposesOnlyFourHexCharacters() {
        val prefix = hashPrefixOf("dQw4w9WgXcQ")
        assertEquals(4, prefix.length)
        assertTrue(prefix.all { character -> character in "0123456789abcdef" })
        assertEquals(prefix, hashPrefixOf("dQw4w9WgXcQ"))
    }

    @Test
    fun onlyTheRequestedVideoIsParsed() {
        val body = """
            [
              {"videoID":"other","segments":[{"segment":[0,10],"category":"sponsor","UUID":"x"}]},
              {"videoID":"wanted","segments":[{"segment":[12.5,30.25],"category":"music_offtopic","UUID":"y"}]}
            ]
        """.trimIndent()
        val segments = parseSponsorSegments(body, "wanted")
        assertEquals(1, segments?.size)
        assertEquals(12_500L, segments?.first()?.startMs)
        assertEquals(30_250L, segments?.first()?.endMs)
        assertEquals("music_offtopic", segments?.first()?.category)
        assertEquals(SPONSOR_SEGMENT_ACTION_SKIP, segments?.first()?.actionType)
    }

    @Test
    fun segmentsAreSortedAndInvalidRangesAreDropped() {
        val body = """
            [{"videoID":"v","segments":[
              {"segment":[40,50],"category":"outro","UUID":"c"},
              {"segment":[10,10],"category":"intro","UUID":"b"},
              {"segment":[1,5],"category":"sponsor","UUID":"a"},
              {"segment":[7],"category":"sponsor","UUID":"d"},
              {"segment":["x","y"],"category":"sponsor","UUID":"e"}
            ]}]
        """.trimIndent()
        val segments = parseSponsorSegments(body, "v")
        assertEquals(listOf("a", "c"), segments?.map { segment -> segment.uuid })
    }

    @Test
    fun anUnknownVideoYieldsNoSegments() {
        val body = """[{"videoID":"other","segments":[{"segment":[0,10],"category":"sponsor"}]}]"""
        assertEquals(emptyList<SponsorSegment>(), parseSponsorSegments(body, "missing"))
    }

    @Test
    fun malformedPayloadsAreRejectedInsteadOfGuessed() {
        assertNull(parseSponsorSegments("not json", "v"))
        assertNull(parseSponsorSegments("{}", "v"))
        assertNull(parseSponsorSegments("""[{"videoID":"v"}]""", "v"))
    }

    @Test
    fun explicitActionTypesArePreserved() {
        val body = """[{"videoID":"v","segments":[{"segment":[0,10],"category":"sponsor","actionType":"mute"}]}]"""
        assertEquals("mute", parseSponsorSegments(body, "v")?.first()?.actionType)
    }

    @Test
    fun missingOrUnsupportedCategoriesAreDropped() {
        val body = """
            [{"videoID":"v","segments":[
              {"segment":[1,5],"category":"sponsor","UUID":"valid"},
              {"segment":[6,10],"category":"","UUID":"blank"},
              {"segment":[11,15],"category":"unknown_category","UUID":"unknown"},
              {"segment":[16,20],"UUID":"missing"}
            ]}]
        """.trimIndent()
        val segments = parseSponsorSegments(body, "v")
        assertEquals(listOf("valid"), segments?.map { it.uuid })
    }
}
