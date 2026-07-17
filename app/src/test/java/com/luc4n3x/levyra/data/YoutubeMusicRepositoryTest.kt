package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
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

    @Test
    fun bareChannelBrowseIdIsNotAcceptedAsArtist() {
        val renderer = JSONObject(
            """
            {
              "flexColumns": [
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {"text": "Estate Mix"}
                      ]
                    }
                  }
                },
                {
                  "musicResponsiveListItemFlexColumnRenderer": {
                    "text": {
                      "runs": [
                        {
                          "text": "Estate Mix",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_CURATOR_CHANNEL"
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
            "Estate Mix"
        )

        assertEquals(emptyList<String>(), references.map { it.browseId })
    }

    @Test
    fun playableSongDoesNotPromoteBareUploaderChannelsToArtists() {
        val renderer = JSONObject(
            """
            {
              "playlistItemData": {
                "videoId": "VIDEO_1"
              },
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
                        {
                          "text": "HIT CANZONI SANREMO 2026",
                          "navigationEndpoint": {
                            "browseEndpoint": {
                              "browseId": "UC_CURATOR_CHANNEL"
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
            "HIT CANZONI SANREMO 2026"
        )

        assertEquals(emptyList<String>(), references.map { it.browseId })
    }

    @Test
    fun artistBrowseEndpointAcceptsChannelIdentifiers() {
        val repository = YoutubeMusicRepository()

        assertTrue(repository.isArtistBrowseEndpoint("UC_DELIA", "MUSIC_PAGE_TYPE_ARTIST"))
        assertTrue(repository.isArtistBrowseEndpoint("UCsRM0YB_dabtEPGPTLcaLQ", ""))
        assertTrue(repository.isArtistBrowseEndpoint("UC_LAZZA", "MUSIC_PAGE_TYPE_USER_CHANNEL"))
    }

    @Test
    fun artistBrowseEndpointRejectsAlbumPlaylistAndVideoIdentifiers() {
        val repository = YoutubeMusicRepository()

        assertFalse(repository.isArtistBrowseEndpoint("MPREb_9nqEki4ZDpP", "MUSIC_PAGE_TYPE_ALBUM"))
        assertFalse(repository.isArtistBrowseEndpoint("MPREb_9nqEki4ZDpP", "MUSIC_PAGE_TYPE_ARTIST"))
        assertFalse(repository.isArtistBrowseEndpoint("VLPLm0mRDwGnW3Q", "MUSIC_PAGE_TYPE_PLAYLIST"))
        assertFalse(repository.isArtistBrowseEndpoint("OLAK5uy_kZbY", "MUSIC_PAGE_TYPE_ALBUM"))
        assertFalse(repository.isArtistBrowseEndpoint("RDAMVMfJ9rUzIMcZQ", ""))
        assertFalse(repository.isArtistBrowseEndpoint("fJ9rUzIMcZQ", ""))
        assertFalse(repository.isArtistBrowseEndpoint("", "MUSIC_PAGE_TYPE_ARTIST"))
    }

    @Test
    fun artistReferenceRejectsAlbumEndpointNestedInActionButton() {
        val header = JSONObject(
            """
            {
              "button": {
                "text": {"runs": [{"text": "Vai all'album"}]},
                "navigationEndpoint": {
                  "browseEndpoint": {
                    "browseId": "MPREb_9nqEki4ZDpP",
                    "browseEndpointContextSupportedConfigs": {
                      "browseEndpointContextMusicConfig": {
                        "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val reference = YoutubeMusicRepository().extractYoutubeMusicArtistReference(header, "Lazza")

        assertNull(reference)
    }

}
