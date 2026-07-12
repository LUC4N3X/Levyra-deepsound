package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class YoutubeLocalDecoderTest {
    @Test
    fun parsesValidatedConfigAndAliases() {
        val result = YoutubePlayerConfigParser.parse(
            """
            {
              "schemaVersion": 1,
              "players": {
                "2182a2cc": {
                  "sig": "ZP(4,7936,INPUT)",
                  "nClass": "SZ",
                  "sts": 20644,
                  "aliases": ["90ed594f"]
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(result is YoutubePlayerConfigParseResult.Success)
        val configs = (result as YoutubePlayerConfigParseResult.Success).configs
        assertEquals(configs["2182a2cc"], configs["90ed594f"])
        assertEquals(20644, configs["2182a2cc"]?.signatureTimestamp)
    }

    @Test
    fun rejectsJavaScriptInjectionInRemoteConfig() {
        val result = YoutubePlayerConfigParser.parse(
            """
            {
              "schemaVersion": 1,
              "players": {
                "2182a2cc": {
                  "sig": "ZP(4,7936,INPUT);alert(1)",
                  "nClass": "SZ",
                  "sts": 20644
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(result is YoutubePlayerConfigParseResult.Success)
        val success = result as YoutubePlayerConfigParseResult.Success
        assertTrue(success.configs.isEmpty())
        assertEquals(listOf("2182a2cc"), success.skippedEntries)
    }

    @Test
    fun rejectsAmbiguousAliasTable() {
        val result = YoutubePlayerConfigParser.parse(
            """
            {
              "schemaVersion": 1,
              "players": {
                "2182a2cc": {
                  "sig": "ZP(4,7936,INPUT)",
                  "nClass": "SZ",
                  "sts": 20644,
                  "aliases": ["90ed594f"]
                },
                "1def3fc2": {
                  "sig": "ZP(4,7936,INPUT)",
                  "nClass": "SZ",
                  "sts": 20644,
                  "aliases": ["90ed594f"]
                }
              }
            }
            """.trimIndent()
        )

        assertTrue(result is YoutubePlayerConfigParseResult.Failure)
    }

    @Test
    fun remoteConfigOverridesBundledEntryWithoutDroppingOthers() {
        val old = YoutubePlayerCipherConfig("2182a2cc", "ZP(4,7936,INPUT)", "SZ", 20644)
        val replacement = YoutubePlayerCipherConfig("2182a2cc", "Ab(1,2,INPUT)", "Cd", 20645)
        val other = YoutubePlayerCipherConfig("1def3fc2", "ZP(4,7936,INPUT)", "SZ", 20644)

        val merged = YoutubePlayerConfigParser.merge(
            bundled = mapOf("2182a2cc" to old, "1def3fc2" to other),
            remote = mapOf("2182a2cc" to replacement)
        )

        assertEquals(replacement, merged["2182a2cc"])
        assertEquals(other, merged["1def3fc2"])
    }

    @Test
    fun remoteOverrideDropsStaleBundledAliases() {
        val old = YoutubePlayerCipherConfig("2182a2cc", "ZP(4,7936,INPUT)", "SZ", 20644)
        val replacement = YoutubePlayerCipherConfig("2182a2cc", "Ab(1,2,INPUT)", "Cd", 20645)

        val merged = YoutubePlayerConfigParser.merge(
            bundled = mapOf("2182a2cc" to old, "90ed594f" to old),
            remote = mapOf("2182a2cc" to replacement, "a0a0a0a0" to replacement)
        )

        assertEquals(replacement, merged["2182a2cc"])
        assertEquals(replacement, merged["a0a0a0a0"])
        assertFalse(merged.containsKey("90ed594f"))
    }

    @Test
    fun playerCacheCleanupDoesNotDeleteRemoteConfigFiles() {
        assertTrue(YoutubePlayerJsSupport.isPlayerJsCacheFile("player_2182a2cc.js"))
        assertFalse(YoutubePlayerJsSupport.isPlayerJsCacheFile("player_configs_remote.json"))
        assertFalse(YoutubePlayerJsSupport.isPlayerJsCacheFile("player_configs_meta.json"))
        assertFalse(YoutubePlayerJsSupport.isPlayerJsCacheFile("current_player.json"))
    }

    @Test
    fun computesEightCharacterPlayerFingerprint() {
        assertEquals("90015098", YoutubePlayerJsSupport.playerFingerprint("abc"))
    }

    @Test
    fun playerFingerprintUsesOnlyFirstTenThousandBytes() {
        val prefix = "a".repeat(10_000)
        assertEquals(
            YoutubePlayerJsSupport.playerFingerprint(prefix + "first"),
            YoutubePlayerJsSupport.playerFingerprint(prefix + "second")
        )
    }

    @Test
    fun validatesNTransformOutputStrictly() {
        assertTrue(YoutubePlayerJsSupport.isValidNTransform("original", "Abc_123-x"))
        assertFalse(YoutubePlayerJsSupport.isValidNTransform("original", "original"))
        assertFalse(YoutubePlayerJsSupport.isValidNTransform("original", "abc"))
        assertFalse(YoutubePlayerJsSupport.isValidNTransform("original", "bad%value"))
    }

    @Test
    fun extractsPlayerHashFromEscapedIframeApiResponse() {
        val hash = YoutubePlayerJsSupport.extractPlayerHash(
            "var x={player_js_url:\"\\/s\\/player\\/2182a2cc\\/www-widgetapi.vflset\\/www-widgetapi.js\"};"
        )

        assertEquals("2182a2cc", hash)
    }

    @Test
    fun injectsValidatedExportsInsidePlayerClosure() {
        val config = YoutubePlayerCipherConfig("2182a2cc", "ZP(4,7936,INPUT)", "SZ", 20644)
        val source = "var before=1;(function(){function ZP(a,b,c){return c}var g={};})(_yt_player);var after=1;"

        val modified = YoutubePlayerJsSupport.injectExports(source, config)

        assertTrue(modified.contains("window.__levyraSig=function(sig){return ZP(4,7936,sig);}"))
        assertTrue(modified.contains("window.__levyraN=function(n)"))
        assertTrue(modified.indexOf("window.__levyraSig") < modified.indexOf("})(_yt_player);"))
    }

    @Test
    fun bundledPlayerConfigRemainsValid() {
        val asset = listOf(
            File("src/main/assets/player_configs.json"),
            File("app/src/main/assets/player_configs.json")
        ).firstOrNull { it.isFile }
        assertNotNull(asset)
        val result = YoutubePlayerConfigParser.parse(requireNotNull(asset).readText())
        assertTrue(result is YoutubePlayerConfigParseResult.Success)
        val success = result as YoutubePlayerConfigParseResult.Success
        assertTrue(success.configs.size >= 100)
        assertNotNull(success.configs["c37d60f8"])
        assertFalse(success.configs.values.any { it.signatureTimestamp <= 0 })
    }
}
