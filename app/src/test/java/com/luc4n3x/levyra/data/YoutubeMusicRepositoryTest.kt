package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class YoutubeMusicRepositoryTest {
    @Test
    fun cleanAlbumDescriptionRemovesWikipediaAttribution() {
        val raw = """
            Mediterraneo è il terzo album in studio del cantautore italiano Bresh, pubblicato il 6 giugno 2025 dalla Epic.
            L'album contiene il singolo La tana del granchio.
            Da
            Wikipedia
            (https://it.wikipedia.org/wiki/Mediterraneo)
            soggetto a Creative Commons Attribution CC-BY-SA 3.0
            (https://creativecommons.org/licenses/by-sa/3.0/)
        """.trimIndent()

        val clean = raw.cleanAlbumDescription()

        assertEquals(
            "Mediterraneo è il terzo album in studio del cantautore italiano Bresh, pubblicato il 6 giugno 2025 dalla Epic. L'album contiene il singolo La tana del granchio.",
            clean
        )
        assertFalse(clean.contains("wikipedia", ignoreCase = true))
        assertFalse(clean.contains("creative commons", ignoreCase = true))
        assertFalse(clean.contains("http", ignoreCase = true))
    }

    @Test
    fun cleanAlbumDescriptionKeepsTextBeforeInlineAttribution() {
        val clean = "Un album caldo e luminoso. Da Wikipedia (https://example.com) CC-BY-SA 3.0"
            .cleanAlbumDescription()

        assertEquals("Un album caldo e luminoso.", clean)
    }

    @Test
    fun artistReferencesUseOrderedSubtitleBrowseIds() {
        val renderer = JSONObject(
            """
            {
              "flexColumns": [
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {"text": "La testa gira"}
                      ]
                    }
                  }
                },
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {
                          "text": "Fred De Palma",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_FRED",
                              "browseEndpointContextSupportedConfigs": {
                                "browseEndpointContextMusicConfig": {
                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                }
                              }
                            }
                          }
                        },
                        {"text": " & "},
                        {
                          "text": "Anitta",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_ANITTA",
                              "browseEndpointContextSupportedConfigs": {
                                "browseEndpointContextMusicConfig": {
                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                }
                              }
                            }
                          }
                        }
                      ]
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val references = YoutubeMusicRepository().extractYoutubeMusicArtistReferences(
            renderer,
            "Fred De Palma & Anitta"
        )

        assertEquals(listOf("Fred De Palma", "Anitta"), references.map { it.name })
        assertEquals(listOf("UC_FRED", "UC_ANITTA"), references.map { it.browseId })
    }
    @Test
    fun artistReferencesIgnoreUnrelatedNestedArtistEndpoints() {
        val renderer = JSONObject(
            """
            {
              "flexColumns": [
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {"text": "Take 5"}
                      ]
                    }
                  }
                },
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {"text": "Brano"},
                        {"text": " • "},
                        {
                          "text": "Shiva",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_SHIVA_OFFICIAL",
                              "browseEndpointContextSupportedConfigs": {
                                "browseEndpointContextMusicConfig": {
                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                }
                              }
                            }
                          }
                        },
                        {"text": " & "},
                        {
                          "text": "Geolier",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_GEOLIER_OFFICIAL",
                              "browseEndpointContextSupportedConfigs": {
                                "browseEndpointContextMusicConfig": {
                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                }
                              }
                            }
                          }
                        },
                        {"text": " • "},
                        {"text": "2026"}
                      ]
                    }
                  }
                }
              ],
              "menu": {
                "menuRenderer": {
                  "items": [
                    {
                      "navigationEndpoint": {
                        "browseEndpoint": {
                          "browseId": "UC_UNRELATED_SHIVA",
                          "browseEndpointContextSupportedConfigs": {
                            "browseEndpointContextMusicConfig": {
                              "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                            }
                          }
                        }
                      },
                      "text": "Shiva"
                    }
                  ]
                }
              }
            }
            """.trimIndent()
        )

        val references = YoutubeMusicRepository().extractYoutubeMusicArtistReferences(
            renderer,
            "Shiva & Geolier"
        )

        assertEquals(listOf("Shiva", "Geolier"), references.map { it.name })
        assertEquals(
            listOf("UC_SHIVA_OFFICIAL", "UC_GEOLIER_OFFICIAL"),
            references.map { it.browseId }
        )
    }

    @Test
    fun directArtistResultUsesRendererBrowseEndpoint() {
        val renderer = JSONObject(
            """
            {
              "navigationEndpoint": {
                "browseEndpoint": {
                  "browseId": "UC_ANNA_OFFICIAL",
                  "browseEndpointContextSupportedConfigs": {
                    "browseEndpointContextMusicConfig": {
                      "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                    }
                  }
                }
              },
              "flexColumns": [
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {"text": "ANNA"}
                      ]
                    }
                  }
                }
              ]
            }
            """.trimIndent()
        )

        val reference = YoutubeMusicRepository().extractYoutubeMusicArtistReference(renderer, "ANNA")

        assertEquals("ANNA", reference?.name)
        assertEquals("UC_ANNA_OFFICIAL", reference?.browseId)
    }

}
