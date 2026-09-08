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
    fun requiresExactSignatureSourceAndRejectsCompositeInputs() {
        val javascript = """
            var decoded=decodeURIComponent(prefix+cipher.s);
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
    fun sourceAssignmentMustBeTheWholeRightHandSide() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s)+suffix;
            var signed=Wrong(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n")+suffix;
            var rewritten=WrongN(throttle);
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun propertyAssignmentDoesNotCreateALocalTaintedBinding() {
        val javascript = """
            obj.decoded=decodeURIComponent(cipher.s);
            var signed=Wrong(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
    }

    @Test
    fun nSinkMustBelongToTheSameReceiverAsTheGet() {
        val javascript = """
            var throttle=query.get("n");
            var rewritten=WrongN(throttle);
            other.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun variableFlowDoesNotCrossIntoNestedFunctions() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            function nested(decoded){
                var signed=Wrong(decoded);
                query.set(signatureKey,encodeURIComponent(signed));
            }
            var throttle=query.get("n");
            function nestedN(throttle){
                var rewritten=WrongN(throttle);
                query.set("n",rewritten);
            }
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
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
    fun discoveryIgnoresLargeAmountsOfUnrelatedAnchorNoise() {
        val javascript = buildString {
            repeat(100) { index ->
                append("var noise$index=decodeURIComponent(value$index);")
            }
            append("var decoded=decodeURIComponent(cipher.s);")
            append("var signed=SemanticSig(decoded);")
            append("query.set(signatureKey,encodeURIComponent(signed));")
            repeat(100) { index ->
                append("var nNoise$index=\"n\";")
            }
            append("var throttle=query.get(\"n\");")
            append("var rewritten=SemanticN(throttle);")
            append("query.set(\"n\",rewritten);")
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("SemanticSig(INPUT)", result.signatures.first().expression)
        assertEquals("SemanticN(INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun regexLiteralNoiseDoesNotCreateSemanticAnchors() {
        val javascript = """
            var fakeSignature=/decodeURIComponent(cipher.s)/;
            var fakeN=/\.get\("n"\)/;
            var decoded=decodeURIComponent(cipher.s);
            var signed=Actual(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=Throttle(throttle);
            query.set("n",rewritten);
        """.trimIndent()

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("Actual(INPUT)", result.signatures.first().expression)
        assertEquals("Throttle(INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun densePrefixCannotConsumeTheTokenBudgetBeforeTheRealAnchor() {
        val javascript = buildString {
            append("var signatureNoise=")
            repeat(700) { append("a+") }
            append("0,decoded=decodeURIComponent(cipher.s);")
            append("var signed=SemanticSig(decoded);")
            append("query.set(signatureKey,encodeURIComponent(signed));")
            append("var nNoise=")
            repeat(700) { append("a+") }
            append("0,throttle=query.get(\"n\");")
            append("var rewritten=SemanticN(throttle);")
            append("query.set(\"n\",rewritten);")
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertEquals("SemanticSig(INPUT)", result.signatures.first().expression)
        assertEquals("SemanticN(INPUT)", result.nTransforms.first().expression)
    }

    @Test
    fun existingLegacyCandidatesKeepTheirOriginalOrderAndCapacity() {
        val javascript = """
            x&&(a=LegacySig1(4,decodeURIComponent(z)));
            x&&(a=LegacySig2(5,decodeURIComponent(z)));
            x&&(a=LegacySig3(6,decodeURIComponent(z)));
            a.get("n"))&&(b=LegacyN1(b));
            a.get("n"))&&(b=LegacyN2(b));
            a.get("n"))&&(b=LegacyN3(b));
            var decoded=decodeURIComponent(cipher.s);
            var signed=SemanticSig(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=SemanticN(throttle);
            query.set("n",rewritten);
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        val candidates = YoutubePlayerJsAnalyzer.analyzeCandidates("2182a2cc", javascript)

        assertEquals(6, candidates.size)
        assertEquals(
            listOf(
                "LegacySig1(4,INPUT)" to "LegacyN1(INPUT)",
                "LegacySig1(4,INPUT)" to "LegacyN2(INPUT)",
                "LegacySig1(4,INPUT)" to "LegacyN3(INPUT)",
                "LegacySig2(5,INPUT)" to "LegacyN1(INPUT)",
                "LegacySig2(5,INPUT)" to "LegacyN2(INPUT)",
                "LegacySig2(5,INPUT)" to "LegacyN3(INPUT)"
            ),
            candidates.map { it.signatureExpression to it.nExpression }
        )
        assertTrue(candidates.none { it.signatureExpression.contains("Semantic") })
        assertTrue(candidates.none { it.nExpression.contains("Semantic") })
    }

    @Test
    fun semanticDiscoveryOnlyFillsAKindTheLegacyAnalyzerCannotResolve() {
        val javascript = """
            x&&(a=LegacySig(4,decodeURIComponent(z)));
            var throttle=query.get("n");
            var rewritten=SemanticN(throttle);
            query.set("n",rewritten);
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        val candidates = YoutubePlayerJsAnalyzer.analyzeCandidates("2182a2cc", javascript)

        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.all { it.signatureExpression == "LegacySig(4,INPUT)" })
        assertEquals("SemanticN(INPUT)", candidates.first().nExpression)
    }

    @Test
    fun semanticDiscoveryCanResolveBothKindsWhenLegacyHasNoCandidate() {
        val javascript = """
            var decoded=decodeURIComponent(cipher.s);
            var signed=SemanticSig(decoded);
            query.set(signatureKey,encodeURIComponent(signed));
            var throttle=query.get("n");
            var rewritten=SemanticN(throttle);
            query.set("n",rewritten);
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        val candidates = YoutubePlayerJsAnalyzer.analyzeCandidates("2182a2cc", javascript)

        assertTrue(candidates.isNotEmpty())
        assertEquals("SemanticSig(INPUT)", candidates.first().signatureExpression)
        assertEquals("SemanticN(INPUT)", candidates.first().nExpression)
    }

    @Test
    fun discoveryRemainsBoundedOnAnchorHeavyInput() {
        val javascript = buildString {
            repeat(80) { index ->
                append("var d$index=decodeURIComponent(c.s);")
                append("var o$index=F$index(d$index);")
                append("q.set(k,encodeURIComponent(o$index));")
                append("var n$index=q.get(\"n\");")
                append("var r$index=N$index(n$index);")
                append("q.set(\"n\",r$index);")
            }
        }

        val result = YoutubePlayerSemanticAnalyzerV2.discover(javascript)

        assertTrue(result.signatures.size <= 2)
        assertTrue(result.nTransforms.size <= 2)
    }
}
