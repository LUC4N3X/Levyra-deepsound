package com.luc4n3x.levyra.data

import com.luc4n3x.levyra.domain.AlbumHit
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicAlbumArtistIdentityTest {

    private fun artistRun(text: String, browseId: String): String = """
        {
          "text": "$text",
          "navigationEndpoint": {
            "browseEndpoint": {
              "browseId": "$browseId",
              "browseEndpointContextSupportedConfigs": {
                "browseEndpointContextMusicConfig": {
                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                }
              }
            }
          }
        }
    """.trimIndent()

    private fun albumPage(strapline: String?, secondSubtitle: String): JSONObject {
        val straplineField = strapline?.let { """"straplineTextOne": {"runs": [$it]},""" }.orEmpty()
        return JSONObject(
            """
            {
              "contents": {
                "twoColumnBrowseResultsRenderer": {
                  "tabs": [
                    {
                      "tabRenderer": {
                        "content": {
                          "sectionListRenderer": {
                            "contents": [
                              {
                                "musicResponsiveHeaderRenderer": {
                                  "title": {"runs": [{"text": "SYNTH RELEASE"}]},
                                  "subtitle": {
                                    "runs": [
                                      {"text": "Album"},
                                      {"text": " • "},
                                      {"text": "2026"}
                                    ]
                                  },
                                  "secondSubtitle": {"runs": [{"text": "$secondSubtitle"}]},
                                  $straplineField
                                  "thumbnail": {
                                    "musicThumbnailRenderer": {
                                      "thumbnail": {
                                        "thumbnails": [
                                          {"url": "https://example.test/cover.jpg", "width": 544, "height": 544}
                                        ]
                                      }
                                    }
                                  }
                                }
                              }
                            ]
                          }
                        }
                      }
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )
    }

    private fun searchPage(kind: String): JSONObject = JSONObject(
        """
        {
          "contents": {
            "tabbedSearchResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicShelfRenderer": {
                              "contents": [
                                {
                                  "musicResponsiveListItemRenderer": {
                                    "flexColumns": [
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {"runs": [{"text": "SYNTH RELEASE"}]}
                                        }
                                      },
                                      {
                                        "musicResponsiveListItemFlexColumnRenderer": {
                                          "text": {
                                            "runs": [
                                              {"text": "$kind"},
                                              {"text": " • "},
                                              ${artistRun("Artist One", "UC_SYNTH_ONE")},
                                              {"text": " e "},
                                              ${artistRun("Artist Two", "UC_SYNTH_TWO")},
                                              {"text": " • "},
                                              {"text": "2026"}
                                            ]
                                          }
                                        }
                                      }
                                    ],
                                    "navigationEndpoint": {
                                      "browseEndpoint": {
                                        "browseId": "MPRE_SYNTH_TRUE",
                                        "browseEndpointContextSupportedConfigs": {
                                          "browseEndpointContextMusicConfig": {
                                            "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                                          }
                                        }
                                      }
                                    },
                                    "thumbnail": {
                                      "musicThumbnailRenderer": {
                                        "thumbnail": {
                                          "thumbnails": [
                                            {"url": "https://example.test/cover.jpg", "width": 544, "height": 544}
                                          ]
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            }
                          }
                        ]
                      }
                    }
                  }
                }
              ]
            }
          }
        }
        """.trimIndent()
    )

    private fun seed(
        title: String = "SYNTH RELEASE",
        artist: String = "",
        browseId: String = "",
        artistBrowseId: String = "",
        thumbnailUrl: String = ""
    ) = AlbumHit(
        title = title,
        artist = artist,
        year = "",
        thumbnailUrl = thumbnailUrl,
        query = "",
        browseId = browseId,
        artistBrowseId = artistBrowseId
    )

    @Test
    fun albumHeaderCreditsEveryStraplineArtistAndKeepsTheFirstArtistBrowseId() {
        val page = albumPage(
            strapline = """
                ${artistRun("Artist One", "UC_SYNTH_ONE")},
                {"text": " e "},
                ${artistRun("Artist Two", "UC_SYNTH_TWO")}
            """.trimIndent(),
            secondSubtitle = "11 brani • 47 minuti"
        )

        val header = YoutubeMusicRepository().parseAlbumHeader(page, seed(browseId = "MPRE_SYNTH"))

        assertEquals("Artist One, Artist Two", header.artist)
        assertEquals("UC_SYNTH_ONE", header.artistBrowseId)
    }

    @Test
    fun albumHeaderWithoutStraplineNeverPromotesTheReleaseDurationToArtist() {
        val page = albumPage(strapline = null, secondSubtitle = "47 minuti")

        val header = YoutubeMusicRepository().parseAlbumHeader(page, seed(artist = "Artist One"))

        assertEquals("Artist One", header.artist)
    }

    @Test
    fun localizedDurationTokensAreNeverArtistNames() {
        listOf(
            "47 minuti",
            "47 minutes",
            "1 hour 20 minutes",
            "47 minutos",
            "1 h 20 min",
            "47 Minuten",
            "47 dakika",
            "1 час 20 минут",
            "47分",
            "47분",
            "47 นาที",
            "3 minuti e 9 secondi",
            "3:45",
            "1:03:45"
        ).forEach { token ->
            assertTrue(token, isLocalizedDurationToken(token))
        }

        listOf(
            "47 Cent",
            "2Pac",
            "6ix9ine",
            "Artist One",
            "Artist One e Artist Two",
            "112",
            "16 brani"
        ).forEach { token ->
            assertFalse(token, isLocalizedDurationToken(token))
        }
    }

    @Test
    fun resolvedAlbumCandidateAcceptsACollaborativeCreditOverAnUnrelatedRelease() {
        val album = seed(artist = "Artist One, Artist Two")
        val unrelated = seed(artist = "Unrelated Person", browseId = "MPRE_WRONG")
        val collaboration = seed(artist = "Artist One e Artist Two", browseId = "MPRE_RIGHT")

        val selected = selectResolvedAlbumCandidate(album, listOf(unrelated, collaboration))

        assertEquals("MPRE_RIGHT", selected?.browseId)
    }

    @Test
    fun resolvedAlbumCandidateRejectsASameTitledReleaseByAnotherArtist() {
        val album = seed(artist = "Artist One")
        val unrelated = seed(artist = "Unrelated Person", browseId = "MPRE_WRONG")

        assertNull(selectResolvedAlbumCandidate(album, listOf(unrelated)))
    }

    @Test
    fun albumRecoveryReachesTheTrueAlbumAfterAnUnusableBrowseId() {
        val page = albumPage(
            strapline = """
                ${artistRun("Artist One", "UC_SYNTH_ONE")},
                {"text": " e "},
                ${artistRun("Artist Two", "UC_SYNTH_TWO")}
            """.trimIndent(),
            secondSubtitle = "11 brani • 47 minuti"
        )
        val header = YoutubeMusicRepository().parseAlbumHeader(page, seed(browseId = "MPRE_STALE"))
        val trueAlbum = seed(artist = "Artist One", browseId = "MPRE_TRUE", artistBrowseId = "UC_SYNTH_ONE")

        val recovered = selectAlbumRecoveryCandidate(
            album = header,
            candidates = listOf(trueAlbum),
            excludedBrowseIds = setOf("MPRE_STALE")
        )

        assertEquals("MPRE_TRUE", recovered?.browseId)
    }

    @Test
    fun albumCandidateSearchAsksForAlbumsOnly() {
        val requestedParams = mutableListOf<String>()

        val hits = YoutubeMusicRepository().searchAlbumHits(
            query = "SYNTH RELEASE Artist One, Artist Two album",
            languageCode = "it",
            limit = 12
        ) { _, _, params ->
            requestedParams += params
            if (params == YOUTUBE_MUSIC_ALBUM_SEARCH_PARAMS) searchPage("Album") else searchPage("Brano")
        }

        assertEquals(listOf(YOUTUBE_MUSIC_ALBUM_SEARCH_PARAMS), requestedParams)
        assertEquals("MPRE_SYNTH_TRUE", hits.singleOrNull()?.browseId)
    }
}
