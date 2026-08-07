package com.luc4n3x.levyra.viewmodel

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class FreshCurrentsRequestPolicyTest {
    @Test
    fun sameLanguageActiveRequestCanBeReused() {
        assertTrue(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "it",
                requestedLanguage = "it",
                force = false
            )
        )
    }

    @Test
    fun rapidLanguageRoundTripDoesNotReuseWrongActiveRequest() {
        assertFalse(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "en",
                requestedLanguage = "it",
                force = false
            )
        )
    }

    @Test
    fun forcedRefreshNeverReusesTheActiveRequest() {
        assertFalse(
            shouldReuseFreshCurrentsRequest(
                activeRequestLanguage = "it",
                requestedLanguage = "it",
                force = true
            )
        )
    }
}
