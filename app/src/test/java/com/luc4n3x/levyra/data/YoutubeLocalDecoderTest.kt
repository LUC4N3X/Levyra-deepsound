package com.luc4n3x.levyra.data

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
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
    fun timeoutCancellationMarksRuntimeDeadBeforeRethrow() = runBlocking {
        val timeout = try {
            withTimeout(10L) { delay(100L) }
            null
        } catch (error: TimeoutCancellationException) {
            error
        }

        assertNotNull(timeout)
        assertTrue(YoutubeCipherRuntimeFailurePolicy.marksRuntimeDead(requireNotNull(timeout)))
        assertFalse(
            YoutubeCipherRuntimeFailurePolicy.marksRuntimeDead(
                CancellationException("Caller cancelled")
            )
        )
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
    @Test
    fun configParityFixturesMatchUpstreamVerdicts() {
        val accepted = listOf(
            "accept-empty-players.json",
            "accept-entry-with-alias.json"
        )
        val rejected = listOf(
            "reject-alias-collision.json",
            "reject-malformed.json",
            "reject-players-missing.json",
            "reject-root-array.json",
            "reject-schema-version-future.json",
            "reject-schema-version-missing.json",
            "reject-schema-version-string.json",
            "reject-schema-version-zero.json"
        )

        accepted.forEach { name ->
            assertTrue(name, YoutubePlayerConfigParser.parse(parityResource(name)) is YoutubePlayerConfigParseResult.Success)
        }
        rejected.forEach { name ->
            assertTrue(name, YoutubePlayerConfigParser.parse(parityResource(name)) is YoutubePlayerConfigParseResult.Failure)
        }
    }

    @Test
    fun nTemplateMatchesUpstreamGoldenFixture() {
        assertEquals(
            parityResource("n-template-Yx.golden").trim(),
            YoutubePlayerConfigParser.buildNExpression("Yx")
        )
    }

    @Test
    fun configEpochMismatchForcesRuntimeRebuild() {
        assertTrue(
            YoutubeRuntimeReusePolicy.canReuse(
                isDead = false,
                runtimeHash = "2182a2cc",
                requestedHash = "2182a2cc",
                runtimeConfigKey = "2182a2cc",
                requestedConfigKey = "2182a2cc",
                runtimeEpoch = 4L,
                currentEpoch = 4L
            )
        )
        assertFalse(
            YoutubeRuntimeReusePolicy.canReuse(
                isDead = false,
                runtimeHash = "2182a2cc",
                requestedHash = "2182a2cc",
                runtimeConfigKey = "2182a2cc",
                requestedConfigKey = "2182a2cc",
                runtimeEpoch = 4L,
                currentEpoch = 5L
            )
        )
    }

    @Test
    fun unknownAndRejectionRefreshCooldownsAreIndependent() {
        val cooldowns = YoutubeRefreshCooldowns(60_000L, 300_000L)
        val now = 1_000_000L

        assertTrue(cooldowns.claimUnknown(now))
        assertTrue(cooldowns.unknownActive(now + 1L))
        assertFalse(cooldowns.rejectionActive(now + 1L))
        assertTrue(cooldowns.claimRejection(now + 1L))
        assertTrue(cooldowns.rejectionActive(now + 2L))
        cooldowns.resetUnknown()
        assertTrue(cooldowns.claimUnknown(now + 2L))
        assertFalse(cooldowns.claimRejection(now + 2L))
    }

    @Test
    fun futureCooldownStampNeverBlocksRefresh() {
        assertFalse(YoutubePlayerConfigStore.withinWindow(1_000L, 2_000L, 60_000L))
    }

    @Test
    fun automaticAnalyzerExtractsConservativeSignatureAndNFunctions() {
        val javascript = """
            var x=1;
            x&&(y=Ab(4,decodeURIComponent(z)));
            a.get("n"))&&(b=Nz[2](b));
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        val config = YoutubePlayerJsAnalyzer.analyze("2182a2cc", javascript)

        assertNotNull(config)
        assertEquals("Ab(4,INPUT)", config?.signatureExpression)
        assertEquals("Nz[2](INPUT)", config?.nExpression)
        assertEquals(20644, config?.signatureTimestamp)
        assertEquals(YoutubePlayerConfigOrigin.ANALYZED, config?.origin)
    }

    @Test
    fun automaticAnalyzerRejectsAmbiguousSignatureMatches() {
        val javascript = """
            x&&(y=Ab(4,decodeURIComponent(z)));
            q&&(r=Cd(5,decodeURIComponent(s)));
            a.get("n"))&&(b=Nz[2](b));
            var cfg={signatureTimestamp:20644};
        """.trimIndent()

        assertEquals(null, YoutubePlayerJsAnalyzer.analyze("2182a2cc", javascript))
    }

    @Test
    fun atomicWriterReplacesFileWithoutLeavingTemporaryArtifacts() {
        val directory = createTempDir(prefix = "levyra-decoder-")
        try {
            val file = File(directory, "player.js")
            YoutubePlayerConfigStore.writeAtomic(file, "first")
            YoutubePlayerConfigStore.writeAtomic(file, "second")

            assertEquals("second", file.readText())
            assertTrue(directory.listFiles().orEmpty().none { it.name.endsWith(".tmp") || it.name.endsWith(".bak") })
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun cdnRejectionFeedbackOnlyTargetsRecentDecoderBackedStreams() {
        val now = 1_000_000L

        assertTrue(YoutubeLocalDecoderFeedbackPolicy.shouldRefresh("YouTube Web", now, now - 1_000L))
        assertTrue(YoutubeLocalDecoderFeedbackPolicy.shouldRefresh("LevyraExtractor · Opus", now, now - 1_000L))
        assertFalse(YoutubeLocalDecoderFeedbackPolicy.shouldRefresh("YouTube Android VR", now, now - 1_000L))
        assertFalse(YoutubeLocalDecoderFeedbackPolicy.shouldRefresh("YouTube Web", now, now - 11L * 60L * 1000L))
    }

    private fun parityResource(name: String): String {
        val resource = requireNotNull(javaClass.classLoader?.getResource("config-parity/$name"))
        return resource.readText()
    }

}
