package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
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
                YoutubePlayerConfigParser.buildNExpression("VB"),
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
        val candidates = YoutubePlayerJsAnalyzer.analyzeCandidates("4fd832e7", playerShape)

        assertTrue(candidates.isNotEmpty())
        assertTrue(candidates.size <= 6)
        assertEquals(YoutubePlayerConfigParser.buildNExpression("VB"), candidates.first().nExpression)
        assertTrue(candidates.all { it.origin == YoutubePlayerConfigOrigin.ANALYZED })
        assertTrue(candidates.all { it.signatureTimestamp == 20711 })
        assertEquals(candidates.size, candidates.map { it.identity }.distinct().size)
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
                YoutubePlayerConfigParser.buildNExpression("AN"),
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
    fun injectedFactoryExportsReplaceEveryInputPlaceholder() {
        val config = YoutubePlayerJsAnalyzer.analyzeCandidates("4fd832e7", playerShape)
            .first { it.signatureExpression.contains("Lt(") }

        val injected = YoutubePlayerJsSupport.injectExports(playerShape, config)

        assertFalse(injected.contains("INPUT"))
        assertTrue(injected.contains("window.__levyraSig=function(sig){return (function(s){var u=Lt("))
        assertTrue(injected.indexOf("window.__levyraN=") < injected.lastIndexOf("})(_yt_player);"))
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
