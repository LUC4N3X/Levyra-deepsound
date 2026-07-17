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

    @Test
    fun prefersVerifiedArtistSearchThumbnailForHomeShelf() {
        val result = chooseVerifiedArtistShelfThumbnail(
            searchThumbnailUrl = "https://yt3.googleusercontent.com/madame-real-photo",
            headerPortraitUrl = "https://yt3.googleusercontent.com/madame-logo"
        )

        assertEquals("https://yt3.googleusercontent.com/madame-real-photo", result)
    }

    @Test
    fun fallsBackToOfficialHeaderWhenSearchThumbnailIsMissing() {
        val result = chooseVerifiedArtistShelfThumbnail(
            searchThumbnailUrl = "",
            headerPortraitUrl = "https://yt3.googleusercontent.com/marracash-official"
        )

        assertEquals("https://yt3.googleusercontent.com/marracash-official", result)
    }

    @Test
    fun selectsHighestConfidenceExactExternalPortrait() {
        val root = JSONObject(
            """
            {
              "data": [
                {
                  "name": "Madame Piano",
                  "nb_fan": 9000000,
                  "picture_xl": "https://cdn.example.com/wrong.jpg"
                },
                {
                  "name": "Madame",
                  "nb_fan": 150000,
                  "picture_xl": "https://cdn.example.com/madame-small.jpg"
                },
                {
                  "name": "MADAME",
                  "nb_fan": 800000,
                  "picture_xl": "https://cdn.example.com/madame-real.jpg"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(
            "https://cdn.example.com/madame-real.jpg",
            selectExactArtistPortrait(root, "Madame")
        )
    }

    @Test
    fun rejectsNonExactAndInsecureExternalPortraits() {
        val root = JSONObject(
            """
            {
              "data": [
                {
                  "name": "Marracash Tribute",
                  "nb_fan": 900000,
                  "picture_xl": "https://cdn.example.com/tribute.jpg"
                },
                {
                  "name": "Marracash",
                  "nb_fan": 800000,
                  "picture_xl": "http://cdn.example.com/insecure.jpg"
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals("", selectExactArtistPortrait(root, "Marracash"))
    }

}
