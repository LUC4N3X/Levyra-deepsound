package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerSemanticAnalyzerV2Test {
    @Test
    fun followsSignatureAndNValuesAcrossRenamedAssignments() {
        val javascript = """
            var cipher={s:"abc"};
            var decoded=decodeURIComponent(cipher.s);
            var transformed=Qx${'$'}1(7,decoded);
            params.set(sp,encodeURIComponent(transformed));
            var throttled=params.get("n");
            var nOut=Bucket[3](throttled);
            params.set("n",nOut);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Qx${'$'}1(7,INPUT)", result.signatures.first().expression)
        assertEquals("Bucket[3](INPUT)", result.nTransforms.first().expression)
        assertTrue(result.signatures.first().confidence >= 110)
        assertTrue(result.nTransforms.first().confidence >= 110)
    }

    @Test
    fun followsOneHopAliasesInsteadOfDependingOnVariableNames() {
        val javascript = """
            var first=decodeURIComponent(cipher["s"]);
            var alias=first;
            var signed=Renamed(alias);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get('n');
            var throttleAlias=throttle;
            var rewritten=ArrayName[9](throttleAlias);
            query.set('n',rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Renamed(INPUT)", result.signatures.first().expression)
        assertEquals("ArrayName[9](INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun ranksSinkBackedCandidateAheadOfADataFlowDecoy() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var decoy=Noise(decoded);
            consume(decoy);
            var signed=Actual(4,decoded);
            query.set(signatureKey,encodeURIComponent(signed));
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Actual(4,INPUT)", result.signatures.first().expression)
        assertTrue(
            result.signatures
                .firstOrNull { it.expression == "Noise(INPUT)" }
                ?.confidence
                ?.let { it < result.signatures.first().confidence }
                ?: true
        )
    }

    @Test
    fun discoversNestedCallsitesWithoutMatchingAMinifiedLayout() {
        val javascript = """
            query.set(signatureKey,encodeURIComponent(Transform(11,decodeURIComponent(cipher.s))));
            query.set("n",Throttle(query.get("n")));
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Transform(11,INPUT)", result.signatures.first().expression)
        assertEquals("Throttle(INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun ignoresComputedCallableTargetsThatCannotBeInjectedSafely() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var signed=Transforms[key](decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=Transforms[key](throttle);
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun doesNotPromoteCallsWithoutSemanticSourceAndSinkEvidence() {
        val javascript = """
            var decoded=decodeURIComponent(value);
            var maybe=Random(decoded);
            var unrelated=query.get("x");
            var other=Throttle(unrelated);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun requiresSignatureSourceToComeFromSDespiteAValidLookingSink() {
        val javascript = """
            var decoded=decodeURIComponent(value);
            var signed=LooksReal(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
    }

    @Test
    fun doesNotTreatStringLiteralSAsSignatureProperty() {
        val javascript = """
            var decoded=decodeURIComponent(read("s"));
            var signed=LooksReal(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
    }

    @Test
    fun doesNotEraseOperatorsAroundTaintedArguments() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var signed=Actual(decoded+1);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=Throttle(throttle+1);
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun doesNotCollapseNestedTransformChains() {
        val javascript = """
            var signed=Outer(Inner(decodeURIComponent(cipher.s)));
            query.set(signatureKey,encodeURIComponent(signed));
            var rewritten=OuterN(InnerN(query.get("n")));
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun doesNotPromoteDirectTransformWithPostProcessing() {
        val javascript = """
            var signed=Actual(decodeURIComponent(cipher.s))+1;
            query.set(signatureKey,encodeURIComponent(signed));
            var rewritten=Throttle(query.get("n"))+1;
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun ignoresSinkEvidenceAfterTransformOutputIsReassigned() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var signed=Actual(decoded);
            signed=other;
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=Throttle(throttle);
            rewritten=otherN;
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun signatureDiscoveryIgnoresUnrelatedDecodeNoise() {
        val javascript = buildString {
            repeat(100) { index ->
                append("var noise$index=decodeURIComponent(value$index);")
            }
            append("var decoded=decodeURIComponent(cipher.s);")
            append("var signed=SemanticSig(decoded);")
            append("query.set(signatureKey,encodeURIComponent(signed));")
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("SemanticSig(INPUT)", result.signatures.first().expression)
    }

    @Test
    fun nDiscoveryIgnoresUnrelatedNStringNoise() {
        val javascript = buildString {
            repeat(100) { index ->
                append("var noise$index=\"n\";")
            }
            append("var throttle=query.get(\"n\");")
            append("var rewritten=SemanticN(throttle);")
            append("query.set(\"n\",rewritten);")
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("SemanticN(INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun reservesCompleteLegacyPairWhenSemanticCandidatesSaturatePool() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var signed=SemanticSig(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var decoded2=decodeURIComponent(cipher.s);
            var signed2=SemanticSig2(decoded2);
            query.set(signatureKey,encodeURIComponent(signed2));
            x&&(y=LegacySig(4,decodeURIComponent(z)));
            var throttle=query.get("n");
            var rewritten=SemanticN(throttle);
            query.set("n",rewritten);
            var throttle2=query.get("n");
            var rewritten2=SemanticN2(throttle2);
            query.set("n",rewritten2);
            a.get("n"))&&(b=LegacyN[2](b));
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        val candidates = YoutubePlayerJsAnalyzer.analyzeCandidates("2182a2cc", javascript)

        assertTrue(candidates.isNotEmpty())
        assertEquals("SemanticSig(INPUT)", candidates.first().signatureExpression)
        assertEquals("SemanticN(INPUT)", candidates.first().nExpression)
        assertTrue(candidates.any { it.signatureExpression == "SemanticSig2(INPUT)" })
        assertTrue(candidates.any { it.nExpression == "SemanticN2(INPUT)" })
        assertTrue(
            candidates.any {
                it.signatureExpression == "LegacySig(4,INPUT)" &&
                    it.nExpression == "LegacyN[2](INPUT)"
            }
        )
    }

    @Test
    fun discoveryRemainsBoundedOnAnchorHeavyInput() {
        val javascript = buildString {
            repeat(80) { index ->
                append("var d$index=decodeURIComponent(c.s);var o$index=F$index(d$index);q.set(k,encodeURIComponent(o$index));")
                append("var n$index=q.get(\"n\");var r$index=N$index(n$index);q.set(\"n\",r$index);")
            }
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.size <= 4)
        assertTrue(result.nTransforms.size <= 4)
    }
}
