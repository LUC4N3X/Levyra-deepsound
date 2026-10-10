package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class YoutubeMusicSearchSuggestionsTest {
    @Test
    fun parsesSearchAndHistorySuggestionsInServerOrder() {
        val root = JSONObject(
            """
            {
              "contents": [
                {
                  "searchSuggestionsSectionRenderer": {
                    "contents": [
                      {
                        "historySuggestionRenderer": {
                          "suggestion": {"runs": [{"text": "daft punk"}]},
                          "navigationEndpoint": {"searchEndpoint": {"query": "daft punk"}}
                        }
                      },
                      {
                        "searchSuggestionRenderer": {
                          "suggestion": {"runs": [{"text": "daft punk "}, {"text": "get lucky"}]},
                          "navigationEndpoint": {"searchEndpoint": {"query": "daft punk get lucky"}}
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(
            listOf("daft punk", "daft punk get lucky"),
            parseYoutubeMusicSearchSuggestions(root)
        )
    }

    @Test
    fun fallsBackToRenderedTextAndSkipsMalformedEntries() {
        val root = JSONObject(
            """
            {
              "contents": [
                {
                  "searchSuggestionsSectionRenderer": {
                    "contents": [
                      {
                        "searchSuggestionRenderer": {
                          "suggestion": {"runs": [{"text": "radio"}, {"text": "head"}]},
                          "navigationEndpoint": {"searchEndpoint": {"query": ""}}
                        }
                      },
                      {"unknownRenderer": {}},
                      {
                        "searchSuggestionRenderer": {
                          "suggestion": {"runs": [{"text": "radiohead"}]},
                          "navigationEndpoint": {"searchEndpoint": {"query": "radiohead"}}
                        }
                      }
                    ]
                  }
                }
              ]
            }
            """.trimIndent()
        )

        assertEquals(listOf("radiohead"), parseYoutubeMusicSearchSuggestions(root))
    }

    @Test
    fun returnsEmptyForMissingSuggestionSection() {
        assertEquals(emptyList<String>(), parseYoutubeMusicSearchSuggestions(JSONObject("{}")))
    }
}
