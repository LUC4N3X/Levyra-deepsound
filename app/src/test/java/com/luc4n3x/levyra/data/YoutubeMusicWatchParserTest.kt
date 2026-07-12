package com.luc4n3x.levyra.data

import org.json.JSONArray
import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubeMusicWatchParserTest {
    @Test
    fun watchPlaylistExtractsQueueTabsContinuationAndCounterpart() {
        val root = JSONObject(
            """
            {
              "contents": {
                "singleColumnMusicWatchNextResultsRenderer": {
                  "tabbedRenderer": {
                    "watchNextTabbedResultsRenderer": {
                      "tabs": [
                        {
                          "tabRenderer": {
                            "endpoint": {
                              "browseEndpoint": {
                                "browseId": "MPLYt_LYRICS",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {
                                    "pageType": "MUSIC_PAGE_TYPE_TRACK_LYRICS"
                                  }
                                }
                              }
                            }
                          }
                        },
                        {
                          "tabRenderer": {
                            "endpoint": {
                              "browseEndpoint": {
                                "browseId": "MPTR_RELATED",
                                "browseEndpointContextSupportedConfigs": {
                                  "browseEndpointContextMusicConfig": {
                                    "pageType": "MUSIC_PAGE_TYPE_TRACK_RELATED"
                                  }
                                }
                              }
                            }
                          }
                        }
                      ]
                    }
                  }
                }
              },
              "playlist": {
                "playlistPanelRenderer": {
                  "contents": [
                    {
                      "playlistPanelVideoWrapperRenderer": {
                        "primaryRenderer": {
                          "playlistPanelVideoRenderer": {
                            "videoId": "audio_id",
                            "title": {"runs": [{"text": "Audio title"}]},
                            "longBylineText": {
                              "runs": [
                                {
                                  "text": "Artist",
                                  "navigationEndpoint": {
                                    "browseEndpoint": {
                                      "browseId": "UC_ARTIST",
                                      "browseEndpointContextSupportedConfigs": {
                                        "browseEndpointContextMusicConfig": {
                                          "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                        }
                                      }
                                    }
                                  }
                                },
                                {"text": " • "},
                                {
                                  "text": "Album",
                                  "navigationEndpoint": {
                                    "browseEndpoint": {
                                      "browseId": "MPRE_ALBUM",
                                      "browseEndpointContextSupportedConfigs": {
                                        "browseEndpointContextMusicConfig": {
                                          "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                                        }
                                      }
                                    }
                                  }
                                }
                              ]
                            },
                            "lengthText": {"runs": [{"text": "3:42"}]},
                            "thumbnail": {"thumbnails": [{"url": "small", "width": 120, "height": 120}, {"url": "large", "width": 544, "height": 544}]},
                            "navigationEndpoint": {
                              "watchEndpoint": {
                                "videoId": "audio_id",
                                "playlistId": "RDAMVMaudio_id",
                                "watchEndpointMusicSupportedConfigs": {
                                  "watchEndpointMusicConfig": {
                                    "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                                  }
                                }
                              }
                            }
                          }
                        },
                        "counterpart": [
                          {
                            "counterpartRenderer": {
                              "playlistPanelVideoRenderer": {
                                "videoId": "video_id",
                                "title": {"runs": [{"text": "Official video"}]},
                                "longBylineText": {"runs": [{"text": "Artist"}]},
                                "lengthText": {"simpleText": "3:45"},
                                "navigationEndpoint": {
                                  "watchEndpoint": {
                                    "videoId": "video_id",
                                    "watchEndpointMusicSupportedConfigs": {
                                      "watchEndpointMusicConfig": {
                                        "musicVideoType": "MUSIC_VIDEO_TYPE_OMV"
                                      }
                                    }
                                  }
                                }
                              }
                            }
                          }
                        ]
                      }
                    },
                    {
                      "playlistPanelVideoRenderer": {
                        "videoId": "next_id",
                        "title": {"runs": [{"text": "Next song"}]},
                        "longBylineText": {"runs": [{"text": "Next artist"}]},
                        "lengthText": {"simpleText": "4:01"},
                        "navigationEndpoint": {"watchEndpoint": {"videoId": "next_id", "playlistId": "RDAMVMaudio_id"}}
                      }
                    }
                  ],
                  "continuations": [{"nextRadioContinuationData": {"continuation": "CONT_TOKEN"}}]
                }
              }
            }
            """.trimIndent()
        )

        val parsed = YoutubeMusicWatchParser.parseWatchPlaylist(root)

        assertEquals("RDAMVMaudio_id", parsed.playlistId)
        assertEquals("MPLYt_LYRICS", parsed.lyricsBrowseId)
        assertEquals("MPTR_RELATED", parsed.relatedBrowseId)
        assertEquals("CONT_TOKEN", parsed.continuation)
        assertEquals(listOf("audio_id", "next_id"), parsed.tracks.map { it.videoId })
        assertEquals("video_id", parsed.tracks.first().counterpart?.videoId)
        assertEquals("Artist", parsed.tracks.first().artists.first().name)
        assertEquals("Album", parsed.tracks.first().albumTitle)
        assertEquals(222_000L, parsed.tracks.first().durationMs)
        assertEquals("large", parsed.tracks.first().thumbnailUrl)
    }

    @Test
    fun relatedParserKeepsMixedSectionsAndTypedItems() {
        val root = JSONObject(
            """
            {
              "contents": {
                "sectionListRenderer": {
                  "contents": [
                    {
                      "musicCarouselShelfRenderer": {
                        "header": {
                          "musicCarouselShelfBasicHeaderRenderer": {
                            "title": {"runs": [{"text": "You might also like"}]}
                          }
                        },
                        "contents": [
                          {
                            "musicResponsiveListItemRenderer": {
                              "title": {"runs": [{"text": "Related song"}]},
                              "flexColumns": [
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        {"text": "Related song"}
                                      ]
                                    }
                                  }
                                },
                                {
                                  "musicResponsiveListItemFlexColumnRenderer": {
                                    "text": {
                                      "runs": [
                                        {
                                          "text": "Related artist",
                                          "navigationEndpoint": {
                                            "browseEndpoint": {
                                              "browseId": "UC_RELATED",
                                              "browseEndpointContextSupportedConfigs": {
                                                "browseEndpointContextMusicConfig": {
                                                  "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                                }
                                              }
                                            }
                                          }
                                        },
                                        {"text": " • "},
                                        {
                                          "text": "Related album",
                                          "navigationEndpoint": {
                                            "browseEndpoint": {
                                              "browseId": "MPRE_RELATED_ALBUM",
                                              "browseEndpointContextSupportedConfigs": {
                                                "browseEndpointContextMusicConfig": {
                                                  "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                                                }
                                              }
                                            }
                                          }
                                        },
                                        {"text": " • 3:33"}
                                      ]
                                    }
                                  }
                                }
                              ],
                              "thumbnail": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "song-art", "width": 400, "height": 400}]}}},
                              "navigationEndpoint": {
                                "watchEndpoint": {
                                  "videoId": "related_video",
                                  "watchEndpointMusicSupportedConfigs": {
                                    "watchEndpointMusicConfig": {
                                      "musicVideoType": "MUSIC_VIDEO_TYPE_ATV"
                                    }
                                  }
                                }
                              }
                            }
                          },
                          {
                            "musicTwoRowItemRenderer": {
                              "title": {"runs": [{"text": "Related album card"}]},
                              "subtitle": {
                                "runs": [
                                  {"text": "Album • "},
                                  {
                                    "text": "Related artist",
                                    "navigationEndpoint": {
                                      "browseEndpoint": {
                                        "browseId": "UC_ALBUM_ARTIST",
                                        "browseEndpointContextSupportedConfigs": {
                                          "browseEndpointContextMusicConfig": {
                                            "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                                          }
                                        }
                                      }
                                    }
                                  },
                                  {"text": " • 2026"}
                                ]
                              },
                              "thumbnailRenderer": {"musicThumbnailRenderer": {"thumbnail": {"thumbnails": [{"url": "album-art", "width": 512, "height": 512}]}}},
                              "navigationEndpoint": {
                                "browseEndpoint": {
                                  "browseId": "MPRE_CARD",
                                  "browseEndpointContextSupportedConfigs": {
                                    "browseEndpointContextMusicConfig": {
                                      "pageType": "MUSIC_PAGE_TYPE_ALBUM"
                                    }
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
            """.trimIndent()
        )

        val sections = YoutubeMusicWatchParser.parseRelated(root)

        assertEquals(1, sections.size)
        assertEquals("You might also like", sections.first().title)
        assertEquals(2, sections.first().items.size)
        val song = sections.first().items.first()
        assertEquals(YoutubeMusicRelatedType.Song, song.type)
        assertEquals("related_video", song.videoId)
        assertEquals("Related artist", song.artists.first().name)
        assertEquals("Related album", song.albumTitle)
        assertEquals(213_000L, song.durationMs)
        assertEquals("song-art", song.thumbnailUrl)
        val album = sections.first().items.last()
        assertEquals(YoutubeMusicRelatedType.Album, album.type)
        assertEquals("MPRE_CARD", album.browseId)
        assertEquals("2026", album.year)
        assertEquals("album-art", album.thumbnailUrl)
    }

    @Test
    fun lyricsParserReadsTimedMobileModelAndPlainWebFallback() {
        val timed = JSONObject(
            """
            {
              "contents": {
                "elementRenderer": {
                  "newElement": {
                    "type": {
                      "componentType": {
                        "model": {
                          "timedLyricsModel": {
                            "lyricsData": {
                              "sourceMessage": "Source: LyricFind",
                              "timedLyricsData": [
                                {
                                  "lyricLine": "First line",
                                  "cueRange": {
                                    "startTimeMilliseconds": "1000",
                                    "endTimeMilliseconds": "2500"
                                  }
                                },
                                {
                                  "lyricLine": "Second line",
                                  "cueRange": {
                                    "startTimeMilliseconds": "2600",
                                    "endTimeMilliseconds": "4200"
                                  }
                                }
                              ]
                            }
                          }
                        }
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )
        val plain = JSONObject(
            """
            {
              "contents": {
                "musicDescriptionShelfRenderer": {
                  "description": {"runs": [{"text": "First plain line\\nSecond plain line"}]},
                  "footer": {"runs": [{"text": "Source: Musixmatch"}]}
                }
              }
            }
            """.trimIndent()
        )

        val timedResult = YoutubeMusicWatchParser.parseLyrics(timed)
        val plainResult = YoutubeMusicWatchParser.parseLyrics(plain)

        assertTrue(timedResult?.synced == true)
        assertEquals("LyricFind", timedResult?.source)
        assertEquals(1_000L, timedResult?.lines?.first()?.startMs)
        assertEquals(4_200L, timedResult?.lines?.last()?.endMs)
        assertFalse(plainResult?.synced ?: true)
        assertEquals("Musixmatch", plainResult?.source)
        assertEquals(listOf("First plain line", "Second plain line"), plainResult?.lines?.map { it.text })
    }

    @Test
    fun relatedDescriptionPreservesAdjacentRunBoundaries() {
        val root = JSONObject(
            """
            {
              "contents": {
                "musicDescriptionShelfRenderer": {
                  "header": {"runs": [{"text": "About"}]},
                  "description": {
                    "runs": [
                      {"text": "First paragraph"},
                      {"text": "Second paragraph"}
                    ]
                  }
                }
              }
            }
            """.trimIndent()
        )

        val sections = YoutubeMusicWatchParser.parseRelated(root)

        assertEquals(1, sections.size)
        assertEquals("First paragraph\nSecond paragraph", sections.first().description)
    }

    @Test
    fun lowQualityRadioFilterAppliesContextualMarkersWithoutDroppingLegitimateTitles() {
        assertTrue(isLowQualityRadioCandidate("Song Name (Slowed + Reverb)", "Artist"))
        assertTrue(isLowQualityRadioCandidate("Artist reacts to Song Name", "Reaction Channel"))
        assertTrue(isLowQualityRadioCandidate("Song Name Karaoke", "Artist"))
        assertFalse(isLowQualityRadioCandidate("Chain Reaction", "Diana Ross"))
        assertFalse(isLowQualityRadioCandidate("Reaction", "New Order"))
    }

    @Test
    fun parsesDirectBotGuardChallenge() {
        val challenge = JSONArray()
            .put("message-id")
            .put(JSONArray().put("window.BotGuard=function(){}"))
            .put(JSONArray().put("https://www.youtube.com/botguard.js"))
            .put("interpreter-hash")
            .put("program-data")
            .put("BotGuard")
            .put(JSONObject.NULL)
            .put("experiments")
        val raw = JSONArray().put(JSONObject.NULL).put(challenge).toString()

        val parsed = JSONObject(YoutubePoTokenRuntime.parseChallengeData(raw))

        assertEquals("message-id", parsed.getString("messageId"))
        assertEquals("program-data", parsed.getString("program"))
        assertEquals("BotGuard", parsed.getString("globalName"))
        assertEquals("experiments", parsed.getString("clientExperimentsStateBlob"))
        assertEquals(
            "window.BotGuard=function(){}",
            parsed.getJSONObject("interpreterJavascript")
                .getString("privateDoNotAccessOrElseSafeScriptWrappedValue")
        )
    }

    @Test
    fun rotatesGuestSessionForBotAndTokenFailuresButNotGeoRestrictions() {
        assertTrue(YoutubePlaybackSecurity.shouldRotateGuestSession("PO token rejected", null))
        assertTrue(YoutubePlaybackSecurity.shouldRotateGuestSession("Forbidden", 403))
        assertFalse(YoutubePlaybackSecurity.shouldRotateGuestSession("Not available in your country", 403))
    }
    @Test
    fun albumArtistReferencePrefersMatchingHeaderRunOverUnrelatedArtists() {
        val header = JSONObject(
            """
            {
              "subtitle": {
                "runs": [
                  {
                    "text": "The Weeknd",
                    "navigationEndpoint": {
                      "browseEndpoint": {
                        "browseId": "UC_THE_WEEKND",
                        "browseEndpointContextSupportedConfigs": {
                          "browseEndpointContextMusicConfig": {
                            "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                          }
                        }
                      }
                    }
                  },
                  {"text": " • "},
                  {"text": "2025"}
                ]
              },
              "unrelated": {
                "runs": [
                  {
                    "text": "Fedez",
                    "navigationEndpoint": {
                      "browseEndpoint": {
                        "browseId": "UC_FEDEZ",
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
            """.trimIndent()
        )

        val reference = YoutubeMusicRepository().extractYoutubeMusicArtistReference(header, "The Weeknd")

        assertEquals("The Weeknd", reference?.name)
        assertEquals("UC_THE_WEEKND", reference?.browseId)
    }

    @Test
    fun albumSubtitleIgnoresNestedUiActionPayload() {
        val runs = JSONArray(
            """
            [
              {"text": {"runs": [{"text": "Vai all'artista"}]}},
              {"text": " • 2026"}
            ]
            """.trimIndent()
        )

        val normalRuns = JSONArray(
            """
            [
              {"text": "Album"},
              {"text": " • "},
              {"text": "DELIA"},
              {"text": " • "},
              {"text": "2026"}
            ]
            """.trimIndent()
        )

        assertEquals("• 2026", extractYoutubeMusicRunText(runs))
        assertEquals("Album • DELIA • 2026", extractYoutubeMusicRunText(normalRuns))
        assertEquals("", "{\"runs\":[{\"text\":\"Vai all'artista\"}]}".cleanAlbumArtistLabel())
        assertEquals("", "Vai all’artista".cleanAlbumArtistLabel())
        assertEquals("DELIA", "DELIA".cleanAlbumArtistLabel())
    }

    @Test
    fun albumArtistReferenceUsesPreferredArtistForNestedActionText() {
        val header = JSONObject(
            """
            {
              "button": {
                "text": {"runs": [{"text": "Vai all'artista"}]},
                "navigationEndpoint": {
                  "browseEndpoint": {
                    "browseId": "UC_DELIA",
                    "browseEndpointContextSupportedConfigs": {
                      "browseEndpointContextMusicConfig": {
                        "pageType": "MUSIC_PAGE_TYPE_ARTIST"
                      }
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val reference = YoutubeMusicRepository().extractYoutubeMusicArtistReference(header, "DELIA")

        assertEquals("DELIA", reference?.name)
        assertEquals("UC_DELIA", reference?.browseId)
    }

    @Test
    fun albumArtistReferenceFallsBackToOnlyArtistEndpoint() {
        val header = JSONObject(
            """
            {
              "subtitle": {
                "runs": [
                  {
                    "text": "The Weeknd",
                    "navigationEndpoint": {
                      "browseEndpoint": {
                        "browseId": "UC_THE_WEEKND",
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
            """.trimIndent()
        )

        val reference = YoutubeMusicRepository().extractYoutubeMusicArtistReference(header, "")

        assertEquals("The Weeknd", reference?.name)
        assertEquals("UC_THE_WEEKND", reference?.browseId)
    }

    @Test
    fun albumTrackArtistRejectsLocalizedPlayCountMetadata() {
        val repository = YoutubeMusicRepository()

        val artist = repository.selectAlbumTrackArtist(
            tokens = listOf("2,2 Mln riproduzioni", "4:44"),
            fallbackArtist = "Eros Ramazzotti"
        )

        assertEquals("Eros Ramazzotti", artist)
        assertTrue(isYoutubeMusicAlbumTrackMetadata("2,2 Mln riproduzioni"))
        assertTrue(isYoutubeMusicAlbumTrackMetadata("16 Mln riproduzioni"))
        assertTrue(isYoutubeMusicAlbumTrackMetadata("232 Mln riproduzioni"))
    }

    @Test
    fun albumTrackArtistKeepsRealArtistBeforePlayCount() {
        val repository = YoutubeMusicRepository()

        val artist = repository.selectAlbumTrackArtist(
            tokens = listOf("Eros Ramazzotti", "2,2 Mln riproduzioni", "4:44"),
            fallbackArtist = "Fallback Artist"
        )

        assertEquals("Eros Ramazzotti", artist)
        assertFalse(isYoutubeMusicAlbumTrackMetadata("2 Chainz"))
    }

}
