package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistPortraitFallbackRepositoryTest {
    @Test
    fun deezerPortraitUsesMatchingArtistExtraLargePicture() {
        val root = JSONObject(
            """
            {"data":[
              {"name":"Someone Else","picture_xl":"https://e-cdns-images.dzcdn.net/images/artist/aaa/1000x1000-000000-80-0-0.jpg"},
              {"name":"Mahmood","picture_xl":"https://e-cdns-images.dzcdn.net/images/artist/bbb/1000x1000-000000-80-0-0.jpg"}
            ]}
            """.trimIndent()
        )

        assertEquals(
            "https://e-cdns-images.dzcdn.net/images/artist/bbb/1000x1000-000000-80-0-0.jpg",
            parseDeezerArtistPortrait(root, "Mahmood")
        )
    }

    @Test
    fun deezerPortraitSkipsPlaceholderAndForeignHosts() {
        val root = JSONObject(
            """
            {"data":[
              {"name":"Mahmood","picture_xl":"https://e-cdns-images.dzcdn.net/images/artist//1000x1000-000000-80-0-0.jpg"},
              {"name":"Mahmood","picture_xl":"https://example.com/mahmood.jpg"}
            ]}
            """.trimIndent()
        )

        assertEquals("", parseDeezerArtistPortrait(root, "Mahmood"))
    }

    @Test
    fun tidalPortraitBuildsResourceUrlFromPictureId() {
        val root = JSONObject(
            """
            {"artists":{"items":[
              {"name":"Laufey","picture":"0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9"}
            ]}}
            """.trimIndent()
        )

        assertEquals(
            "https://resources.tidal.com/images/0a1b2c3d/4e5f/6071/8293/a4b5c6d7e8f9/750x750.jpg",
            parseTidalArtistPortrait(root, "Laufey")
        )
    }

    @Test
    fun tidalPortraitIgnoresMismatchedArtistOrInvalidPicture() {
        val root = JSONObject(
            """
            {"artists":{"items":[
              {"name":"Other Artist","picture":"0a1b2c3d-4e5f-6071-8293-a4b5c6d7e8f9"},
              {"name":"Laufey","picture":"../../etc"}
            ]}}
            """.trimIndent()
        )

        assertEquals("", parseTidalArtistPortrait(root, "Laufey"))
    }
}
