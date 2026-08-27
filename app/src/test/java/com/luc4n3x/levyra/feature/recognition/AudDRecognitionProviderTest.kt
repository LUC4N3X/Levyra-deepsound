package com.luc4n3x.levyra.feature.recognition

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AudDRecognitionProviderTest {
    @Test fun `success maps a match`() = assertTrue(parseAudDResponse(true, "{\"status\":\"success\",\"result\":{\"title\":\"T\",\"artist\":\"A\"}}") is RecognitionOutcome.Match)
    @Test fun `success with null result is no match`() = assertEquals(RecognitionOutcome.NoMatch, parseAudDResponse(true, "{\"status\":\"success\",\"result\":null}"))
    @Test fun `error status maps network error`() = assertEquals(RecognitionOutcome.Error(RecognitionErrorKind.Network), parseAudDResponse(true, "{\"status\":\"error\"}"))
    @Test fun `wav rejects unsupported sample rate`() = assertNull(audDWav(AudioFingerprint(shortArrayOf(1), 4_000, 1)))
}
