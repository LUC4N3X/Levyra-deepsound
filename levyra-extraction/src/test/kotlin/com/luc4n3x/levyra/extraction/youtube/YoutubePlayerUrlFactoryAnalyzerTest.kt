package com.luc4n3x.levyra.extraction.youtube

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class YoutubePlayerUrlFactoryAnalyzerTest {

    private val playerShape = """
        var _yt_player={};(function(g){var window=this;
        Lt=function(b,W,c){W=W===void 0?"":W;c=c===void 0?"":c;b=new g.VB(b,!0);b.set("alr","yes");c&&(c=${'$'}P(88,5074,Sk(10,1600,c)),b[b2[17]](W,G4(4,124,c)));return b};
        RE=function(b,W,c){c=b.Lb===null?b.resource.Lb(W,c):b.Lb;if(b.Z){var U=new cA(W);U.get("alr")||U.set("alr","yes");}};
        L2m=function(b,W){var c=HT(b,"redirector.googlevideo.com");c.set("alr","yes");c.set("id","")};
        var cfg={signatureTimestamp:20711};
        })(_yt_player);
    """.trimIndent()

    @Test
    fun urlFactoryAnchorYieldsVerifiedClassFormBeforeFactoryInvocation() {
        val result = YoutubePlayerUrlFactoryAnalyzer.discover(playerShape)

        assertEquals(
            listOf(
                YoutubePlayerAnalyzer.buildNExpression("VB"),
                YoutubePlayerUrlFactoryAnalyzer.factoryNExpression("Lt")
            ),
            result.nTransforms.map { it.expression }
        )
        assertEquals(
            listOf(YoutubePlayerUrlFactoryAnalyzer.factorySignatureExpression("Lt")),
            result.signatures.map { it.expression }
        )
        assertTrue(result.nTransforms.first().confidence > result.nTransforms.last().confidence)
    }

    @Test
    fun analyzerRanksUrlClassCandidateFirstForAnchoredPlayers() {
        val candidates = (YoutubePlayerAnalyzer.analyze(playerShape) as YoutubePlayerAnalysis.Candidates).transforms

        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.size <= 6)
        assertEquals(YoutubePlayerAnalyzer.buildNExpression("VB"), candidates.first().nExpression)
        assertEquals(20711, (YoutubePlayerAnalyzer.analyze(playerShape) as YoutubePlayerAnalysis.Candidates).signatureTimestamp)
        assertEquals(candidates.size, candidates.map { it.signatureExpression to it.nExpression }.distinct().size)
    }

    @Test
    fun es6FactoryWithDefaultParametersIsRecognised() {
        val javascript = """
            vA=function(b,W){b.WB(W)&&(b.A=!0)};
            Pq=function(b,W="",c=""){b=new g.AN(b,!0);b.set("alr","yes");c&&(c=vK(28,2172,${'$'}U(4,2874,c)),b[v[10]](W,${'$'}U(19,3820,c)));return b};
            s0=async function(b,W){var c=U0(b,"redirector.googlevideo.com");c.set("alr","yes");c.set("id","1")};
        """.trimIndent()

        val result = YoutubePlayerUrlFactoryAnalyzer.discover(javascript)

        assertEquals(
            listOf(
                YoutubePlayerAnalyzer.buildNExpression("AN"),
                YoutubePlayerUrlFactoryAnalyzer.factoryNExpression("Pq")
            ),
            result.nTransforms.map { it.expression }
        )
        assertEquals(
            listOf(YoutubePlayerUrlFactoryAnalyzer.factorySignatureExpression("Pq")),
            result.signatures.map { it.expression }
        )
    }

    @Test
    fun ambiguousFactoriesProduceNoAnchoredCandidates() {
        val duplicated = playerShape.replace(
            "var cfg=",
            "Mt=function(b,W,c){b=new g.XY(b,!0);b.set(\"alr\",\"yes\");c&&(c=Q(c));return b};var cfg="
        )

        val result = YoutubePlayerUrlFactoryAnalyzer.discover(duplicated)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun factoryWithoutConstructedUrlClassKeepsOnlyFactoryInvocation() {
        val javascript = "Lt=function(b,W,c){b=mk(b);b.set(\"alr\",\"yes\");c&&(c=Q(c));return b};"

        val result = YoutubePlayerUrlFactoryAnalyzer.discover(javascript)

        assertEquals(
            listOf(YoutubePlayerUrlFactoryAnalyzer.factoryNExpression("Lt")),
            result.nTransforms.map { it.expression }
        )
    }

    @Test
    fun alrWritesOutsideTheFactoryShapeAreIgnored() {
        val javascript = """
            RE=function(b,W,c){if(b.Z){var U=new cA(W);U.set("alr","yes");}};
            L2m=function(b,W){var c=HT(b,"x");c.set("alr","yes");c.set("id","")};
            var cfg={signatureTimestamp:20711};
        """.trimIndent()

        val result = YoutubePlayerUrlFactoryAnalyzer.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }

    @Test
    fun discoveryStaysBoundedOnFactoryLikeNoise() {
        val javascript = buildString {
            repeat(2_000) { index ->
                append("f$index=function(a,b,c){")
                append("x".repeat(590))
                append(";a.set(\"alr\",\"no\");c&&c};")
            }
        }

        val result = YoutubePlayerUrlFactoryAnalyzer.discover(javascript)

        assertTrue(result.signatures.isEmpty())
        assertTrue(result.nTransforms.isEmpty())
    }
}
