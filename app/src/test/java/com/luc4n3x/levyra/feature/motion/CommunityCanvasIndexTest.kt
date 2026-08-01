package com.luc4n3x.levyra.feature.motion

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class CommunityCanvasIndexTest {
    @Test
    fun lookupKeysCoverIsrcTrackAndAlbumWithoutNetworkSizedMetadata() {
        val identity = MotionTrackIdentity(
            title = "Don't Stop (Live)",
            artists = listOf("Artist One", "Artist Two"),
            album = "The Album",
            durationMs = 180_000L,
            isrc = "usabc1234567",
            upc = "",
            year = "",
            trackId = "track-id",
            albumId = "album-id"
        )

        assertEquals(
            listOf(
                "i|USABC1234567",
                "t|dont stop live|artist one artist two|the album",
                "a|artist one artist two|the album"
            ),
            communityCanvasLookupKeys(identity)
        )
    }

    @Test
    fun lookupHashAndShardPrefixMatchThePythonIndexer() {
        val key = "t|flowers|miley cyrus|endless summer vacation"

        assertEquals("-K6RoUVvFyzLwspAVDpboQ9Ad9om6Fpv3P29SRtmU08", communityCanvasLookupHash(key))
        assertEquals("f8a", communityCanvasShardPrefix(key, prefixChars = 3))
    }

    @Test
    fun manifestBitmapAvoidsRequestsForMissingShards() {
        val manifest = parseCommunityCanvasIndexManifest(
            """
            {
              "version": 2,
              "hash": "sha256",
              "hashEncoding": "base64url",
              "prefixChars": 2,
              "entryCount": 3,
              "keyCount": 5,
              "shardCount": 5,
              "largestShardBytes": 166,
              "shardBitmap": "QAAAAAAAAAQAAAAgAAAAAAAAAAAAAAAAAAAAABAAAAE="
            }
            """.trimIndent()
        )

        assertNotNull(manifest)
        assertTrue(manifest!!.hasShard("06"))
        assertTrue(manifest.hasShard("f8"))
        assertFalse(manifest.hasShard("ff"))
        assertFalse(manifest.hasShard("f"))
    }

    @Test
    fun compactShardParsesOnlySafeKnownMedia() {
        val rows = parseCommunityCanvasIndexShard(
            """
            {
              "version": 2,
              "items": [
                {
                  "h": "-K6RoUVvFyzLwspAVDpboQ9Ad9om6Fpv3P29SRtmU08",
                  "u": "https://vivimusicanvas.mkmdevilmi.workers.dev/Song/2.mp4",
                  "s": "t",
                  "i": "USSM12209777",
                  "w": 1080,
                  "g": 1920
                },
                {
                  "h": "XW0J48rLsOi5wNCr4BFRQV4hl4Uln4huUif_6VUN2No",
                  "u": "https://vivimusicanvas.mkmdevilmi.workers.dev/Album/9.m3u8",
                  "s": "a"
                },
                {
                  "h": "AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA",
                  "u": "https://example.com/not-allowed.mp4",
                  "s": "t"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(2, rows.size)
        assertEquals(MotionArtworkScope.TRACK, rows[0].scope)
        assertEquals("USSM12209777", rows[0].isrc)
        assertEquals(1080, rows[0].width)
        assertEquals(1920, rows[0].height)
        assertEquals(MotionArtworkScope.ALBUM, rows[1].scope)
    }
}
