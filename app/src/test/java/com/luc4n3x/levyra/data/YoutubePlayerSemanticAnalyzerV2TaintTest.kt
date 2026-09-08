package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerSemanticAnalyzerV2TaintTest {
    @Test
    fun reassignedSourceVariableDoesNotStayTainted() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            decoded=other;
            var signed=Decoy(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            throttle=otherN;
            var rewritten=DecoyN(throttle);
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun inPlaceTransformUsesPreviousTaintThenKeepsDerivedValueTainted() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            decoded=Actual(decoded);
            query.set(signatureKey,encodeURIComponent(decoded));
            var throttle=query.get("n");
            throttle=Throttle(throttle);
            query.set("n",throttle);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Actual(INPUT)", result.signatures.first().expression)
        assertEquals("Throttle(INPUT)", result.nTransforms.first().expression)
    }
}
