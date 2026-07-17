package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ArtistOfficialArtworkTest {
    @Test
    fun usesPortraitFromOfficialArtistHeader() {
        val header = JSONObject(
            """
            {
              "musicImmersiveHeaderRenderer": {
                "title": {
                  "runs": [{"text": "Lazza"}]
                },
                "thumbnail": {
                  "musicThumbnailRenderer": {
                    "thumbnail": {
                      "thumbnails": [
                        {"url": "https://yt3.googleusercontent.com/artist-small", "width": 160, "height": 160},
                        {"url": "https://yt3.googleusercontent.com/artist-official", "width": 1080, "height": 1080}
                      ]
                    }
                  }
                },
                "backgroundThumbnail": {
                  "musicThumbnailRenderer": {
                    "thumbnail": {
                      "thumbnails": [
                        {"url": "https://yt3.googleusercontent.com/artist-banner", "width": 1920, "height": 1080}
                      ]
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        val artwork = parseArtistHeaderArtwork(header)

        assertEquals("https://yt3.googleusercontent.com/artist-official", artwork.portraitUrl)
        assertEquals("https://yt3.googleusercontent.com/artist-banner", artwork.bannerUrl)
        assertTrue(artwork.portraitUrl != artwork.bannerUrl)
    }

    @Test
    fun ignoresArtworkOutsideOfficialArtistHeader() {
        val root = JSONObject(
            """
            {
              "header": {
                "musicVisualHeaderRenderer": {
                  "title": {
                    "runs": [{"text": "Annalisa"}]
                  },
                  "foregroundThumbnail": {
                    "musicThumbnailRenderer": {
                      "thumbnail": {
                        "thumbnails": [
                          {"url": "https://yt3.googleusercontent.com/annalisa-official", "width": 1024, "height": 1024}
                        ]
                      }
                    }
                  }
                }
              },
              "contents": {
                "musicShelfRenderer": {
                  "contents": [
                    {
                      "musicResponsiveListItemRenderer": {
                        "thumbnail": {
                          "musicThumbnailRenderer": {
                            "thumbnail": {
                              "thumbnails": [
                                {"url": "https://i.ytimg.com/album-cover", "width": 1200, "height": 1200}
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
            """.trimIndent()
        )

        val artwork = parseArtistHeaderArtwork(root.optJSONObject("header"))

        assertEquals("https://yt3.googleusercontent.com/annalisa-official", artwork.portraitUrl)
    }
}
