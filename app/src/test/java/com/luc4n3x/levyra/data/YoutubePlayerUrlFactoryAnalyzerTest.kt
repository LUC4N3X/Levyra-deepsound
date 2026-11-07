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
    fun injectedFactoryExportsReplaceEveryInputPlaceholder() {
        val config = YoutubePlayerJsAnalyzer.analyzeCandidates("4fd832e7", playerShape)
            .first { it.signatureExpression.contains("Lt(") }

        val injected = YoutubePlayerJsSupport.injectExports(playerShape, config)

        assertFalse(injected.contains("INPUT"))
        assertTrue(injected.contains("window.__levyraSig=function(sig){return (function(s){var u=Lt("))
        assertTrue(injected.indexOf("window.__levyraN=") < injected.lastIndexOf("})(_yt_player);"))
    }

}
