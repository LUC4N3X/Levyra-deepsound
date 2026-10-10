package com.luc4n3x.levyra.data

import org.json.JSONObject
import org.junit.Assert.assertEquals
import org.junit.Test

class ArtistMonthlyListenersTest {
    @Test
    fun readsOnlyOfficialMonthlyListenerCountField() {
        val header = JSONObject(
            """
            {
              "musicImmersiveHeaderRenderer": {
                "monthlyListenerCount": {
                  "runs": [
                    {"text": "4,8 Mln di ascoltatori mensili"}
                  ]
                },
                "subscriptionButton": {
                  "subscribeButtonRenderer": {
                    "subscriberCountText": {
                      "runs": [{"text": "1,2 Mln di iscritti"}]
                    }
                  }
                }
              }
            }
            """.trimIndent()
        )

        assertEquals(
            "4,8 Mln di ascoltatori mensili",
            extractOfficialMonthlyListeners(header)
        )
    }

    @Test
    fun doesNotPromoteSubscribersOrUnrelatedTextToMonthlyListeners() {
        val header = JSONObject(
            """
            {
              "musicImmersiveHeaderRenderer": {
                "subscriptionButton": {
                  "subscribeButtonRenderer": {
                    "subscriberCountText": {
                      "runs": [{"text": "2,5 Mln di iscritti"}]
                    }
                  }
                },
                "description": {
                  "runs": [{"text": "10 milioni di ascolti"}]
                }
              }
            }
            """.trimIndent()
        )

        assertEquals("", extractOfficialMonthlyListeners(header))
    }
}
