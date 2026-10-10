package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicChartPageParserTest {

    @Test
    fun selectsTheFirstChartPlaylistCarousel() {
        assertEquals("PL_TOP_ITALY", YoutubeMusicChartPageParser.firstPlaylistId(chartPage))
    }

    @Test
    fun ignoresNonPlaylistCarouselsBeforeCharts() {
        assertEquals("PL_TOP_ITALY", YoutubeMusicChartPageParser.firstPlaylistId(chartPage))
    }

    @Test
    fun stripsTheYoutubeMusicVirtualListPrefix() {
        assertEquals("PL_TOP_ITALY", YoutubeMusicChartPageParser.firstPlaylistId(chartPage))
    }

    @Test
    fun malformedOrUnsupportedPayloadReturnsEmpty() {
        assertEquals("", YoutubeMusicChartPageParser.firstPlaylistId("not json"))
        assertEquals("", YoutubeMusicChartPageParser.firstPlaylistId("""{"contents":{}}"""))
    }

    private val chartPage = """
        {
          "contents": {
            "singleColumnBrowseResultsRenderer": {
              "tabs": [
                {
                  "tabRenderer": {
                    "content": {
                      "sectionListRenderer": {
                        "contents": [
                          {
                            "musicCarouselShelfRenderer": {
                              "contents": [
                                {
                                  "musicTwoRowItemRenderer": {
                                    "title": {
                                      "runs": [
                                        {
                                          "text": "Album popolari",
                                          "navigationEndpoint": {
                                            "browseEndpoint": {
                                              "browseId": "MPREb_album"
                                            }
                                          }
                                        }
                                      ]
                                    }
                                  }
                                }
                              ]
                            }
                          },
                          {
                            "musicCarouselShelfRenderer": {
                              "contents": [
                                {
                                  "musicTwoRowItemRenderer": {
                                    "title": {
                                      "runs": [
                                        {
                                          "text": "Daily Top Music Videos - Italy",
                                          "navigationEndpoint": {
                                            "browseEndpoint": {
                                              "browseId": "VLPL_TOP_ITALY"
                                            }
                                          }
                                        }
                                      ]
                                    }
                                  }
                                },
                                {
                                  "musicTwoRowItemRenderer": {
                                    "navigationEndpoint": {
                                      "browseEndpoint": {
                                        "browseId": "VLPL_SECONDARY"
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
}
