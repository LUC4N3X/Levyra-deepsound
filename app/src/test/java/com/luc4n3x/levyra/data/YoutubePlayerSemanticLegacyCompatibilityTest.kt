package com.luc4n3x.levyra.data

import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerSemanticLegacyCompatibilityTest {
    @Test
    fun everyExistingLegacySignatureShapeSuppressesSemanticReplacement() {
        val legacyShapes = listOf(
            "x&&(a=LegacySig1(4,decodeURIComponent(z)));",
            "c&&a.set(sp,encodeURIComponent(LegacySig2(x)));",
            "q&&p.set(sp,encodeURIComponent(LegacySig3(x)));",
            "m=LegacySig4(decodeURIComponent(h.s));",
            "c&&d.set(sp,LegacySig5(x));"
        )

        legacyShapes.forEachIndexed { index, legacy ->
            val javascript = buildString {
                append(legacy)
                append("var decoded=decodeURIComponent(cipher.s);")
                append("var signed=SemanticSig")
                append(index)
                append("(decoded);")
                append("query.set(signatureKey,encodeURIComponent(signed));")
            }

            val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

            assertTrue("legacy signature shape $index must keep semantic signature disabled", result.signatures.isEmpty())
        }
    }

    @Test
    fun everyExistingLegacyNShapeSuppressesSemanticReplacement() {
        val legacyShapes = listOf(
            "a.get(\"n\"))&&(b=LegacyN1(b));",
            "a.get(\"n\")) && (q=LegacyN2[2](q));",
            "LegacyN3=function(x){x=x;enhanced_except_marker}"
        )

        legacyShapes.forEachIndexed { index, legacy ->
            val javascript = buildString {
                append(legacy)
                append("var throttle=query.get(\"n\");")
                append("var rewritten=SemanticN")
                append(index)
                append("(throttle);")
                append("query.set(\"n\",rewritten);")
            }

            val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

            assertTrue("legacy n shape $index must keep semantic n disabled", result.nTransforms.isEmpty())
        }
    }
}
