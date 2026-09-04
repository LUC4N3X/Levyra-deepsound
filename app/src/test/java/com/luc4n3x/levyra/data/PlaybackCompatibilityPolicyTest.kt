package com.luc4n3x.levyra.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class PlaybackCompatibilityPolicyTest {
    @Test
    fun bundledPolicyMatchesCurrentWorkingPlaybackOrder() {
        val policy = PlaybackCompatibilityPolicy.bundled()

        assertEquals(
            listOf(
                PlaybackAudioStrategy.REEL_AUDIO,
                PlaybackAudioStrategy.REEL_MUXED,
                PlaybackAudioStrategy.PERSISTED,
                PlaybackAudioStrategy.DIRECT,
                PlaybackAudioStrategy.SEARCH
            ),
            policy.audioStrategies
        )
        assertEquals(
            listOf(
                PlaybackVideoStrategy.PERSISTED,
                PlaybackVideoStrategy.STANDARD,
                PlaybackVideoStrategy.REEL
            ),
            policy.videoStrategies
        )
        assertEquals("21.03.36", policy.androidReelClientVersion)
        assertEquals(
            mapOf("ANDROID_VR" to PlaybackClientOverride(enabled = false)),
            policy.clientOverrides
        )
    }

    @Test
    fun partialPolicyCanSwitchPlaybackWithoutReplacingUnrelatedDefaults() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["REEL_AUDIO", "REEL_MUXED", "DIRECT"],
              "clients": {
                "ANDROID_VR": {
                  "enabled": false
                },
                "ANDROID_MUSIC": {
                  "priority": 0,
                  "requiresPoToken": true,
                  "clientVersion": "8.11.00"
                }
              }
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        parsed!!
        assertEquals(
            listOf(
                PlaybackAudioStrategy.REEL_AUDIO,
                PlaybackAudioStrategy.REEL_MUXED,
                PlaybackAudioStrategy.DIRECT
            ),
            parsed.audioStrategies
        )
        assertEquals(base.videoStrategies, parsed.videoStrategies)
        assertEquals(base.androidReelClientVersion, parsed.androidReelClientVersion)
        assertEquals(false, parsed.clientOverrides.getValue("ANDROID_VR").enabled)
        assertEquals(0, parsed.clientOverrides.getValue("ANDROID_MUSIC").priority)
        assertEquals(true, parsed.clientOverrides.getValue("ANDROID_MUSIC").requiresPoToken)
        assertEquals("8.11.00", parsed.clientOverrides.getValue("ANDROID_MUSIC").clientVersion)
    }

    @Test
    fun emptyClientsPreserveBundledOverrides() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """{"schema":1,"revision":2026082801,"clients":{}}""",
            base
        )

        assertNotNull(parsed)
        assertEquals(base.clientOverrides, parsed!!.clientOverrides)
    }

    @Test
    fun partialClientOverridePreservesBundledEnabledFlag() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026082801,
              "clients": {
                "ANDROID_VR": {
                  "priority": 3
                }
              }
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNotNull(parsed)
        assertEquals(false, parsed!!.clientOverrides.getValue("ANDROID_VR").enabled)
        assertEquals(3, parsed.clientOverrides.getValue("ANDROID_VR").priority)
    }

    @Test
    fun explicitClientEnableOverridesBundledDisable() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026082801,
              "clients": {
                "ANDROID_VR": {
                  "enabled": true
                }
              }
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNotNull(parsed)
        assertEquals(true, parsed!!.clientOverrides.getValue("ANDROID_VR").enabled)
    }

    @Test
    fun policyCanMoveReelAheadOfStandardVideoPath() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "videoStrategy": ["REEL", "STANDARD"]
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertEquals(
            listOf(PlaybackVideoStrategy.REEL, PlaybackVideoStrategy.STANDARD),
            parsed?.videoStrategies
        )
    }

    @Test
    fun policyCanUpdateAndroidReelClientVersion() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "androidReelClientVersion": "21.04.00"
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertEquals("21.04.00", parsed?.androidReelClientVersion)
    }

    @Test
    fun unknownFutureClientOverrideIsIgnoredSafely() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "clients": {
                "FUTURE_CLIENT": {
                  "enabled": false,
                  "priority": 0
                }
              }
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNotNull(parsed)
        assertEquals(false, parsed!!.clientOverrides.getValue("ANDROID_VR").enabled)
        assertFalse(parsed.clientOverrides.containsKey("FUTURE_CLIENT"))
    }

    @Test
    fun unsupportedStrategyIsIgnoredInsteadOfRejectingTheWholeCandidate() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["REMOTE_SCRIPT"]
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        assertFalse(parsed!!.audioStrategies.isEmpty())
        assertEquals(base.audioStrategies, parsed.audioStrategies)
    }

    @Test
    fun unsupportedSchemaRejectsWholeCandidate() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 99,
              "revision": 2026081802
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNull(parsed)
    }

    @Test
    fun fractionalSchemaValueIsRejected() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """{"schema":1.5,"revision":2026081802}""",
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNull(parsed)
    }

    @Test
    fun invalidClientVersionRejectsWholeCandidate() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "clients": {
                "ANDROID_VR": {
                  "clientVersion": "1.65.10 injected header"
                }
              }
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNull(parsed)
    }

    @Test
    fun disablingEveryKnownClientIsRejected() {
        val clients = listOf(
            "VISIONOS",
            "ANDROID_VR",
            "ANDROID_MUSIC",
            "ANDROID",
            "IOS",
            "WEB_REMIX",
            "WEB",
            "WEB_EMBEDDED_PLAYER"
        ).joinToString(",") { "\"$it\":{\"enabled\":false}" }
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """{"schema":1,"revision":2026081802,"clients":{$clients}}""",
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNull(parsed)
    }

    @Test
    fun canonicalCacheRoundTripPreservesPolicy() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["DIRECT", "REEL_MUXED"],
              "videoStrategy": ["REEL"],
              "androidReelClientVersion": "21.04.00",
              "clients": {
                "ANDROID_VR": {
                  "enabled": true,
                  "priority": 2,
                  "requiresPoToken": true
                }
              }
            }
            """.trimIndent(),
            base
        )!!

        val roundTrip = PlaybackCompatibilityPolicyParser.parse(parsed.toJson(), base)

        assertEquals(parsed, roundTrip)
        assertFalse(roundTrip!!.audioStrategies.isEmpty())
    }

    @Test
    fun unknownRemoteStrategyIsSkippedButKnownOnesAreAccepted() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["REEL_MUXED", "REMOTE_SCRIPT", "DIRECT"]
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNotNull(parsed)
        assertEquals(
            listOf(PlaybackAudioStrategy.REEL_MUXED, PlaybackAudioStrategy.DIRECT),
            parsed!!.audioStrategies
        )
    }

    @Test
    fun allUnknownStrategiesFallBackToBaseListInsteadOfEmpty() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["REMOTE_SCRIPT", "ANOTHER_UNKNOWN"]
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        assertFalse(parsed!!.audioStrategies.isEmpty())
        assertEquals(base.audioStrategies, parsed.audioStrategies)
    }

    @Test
    fun expiredPolicyIsTreatedAsExpired() {
        val expired = PlaybackCompatibilityPolicy.bundled().copy(expiresAt = 1_700_000_000_000L)
        val notExpired = PlaybackCompatibilityPolicy.bundled().copy(expiresAt = 4_000_000_000_000L)
        val neverExpires = PlaybackCompatibilityPolicy.bundled().copy(expiresAt = 0L)

        assertTrue(expired.isExpired(nowMs = 1_800_000_000_000L))
        assertFalse(notExpired.isExpired(nowMs = 1_800_000_000_000L))
        assertFalse(neverExpires.isExpired(nowMs = 1_800_000_000_000L))
    }

    @Test
    fun appVersionOutsideSupportedRangeIsRejected() {
        val policy = PlaybackCompatibilityPolicy.bundled().copy(
            minSupportedAppVersion = 10,
            maxSupportedAppVersion = 20
        )

        assertFalse(isAppVersionSupported(policy, appVersionCode = 5))
        assertFalse(isAppVersionSupported(policy, appVersionCode = 25))
        assertTrue(isAppVersionSupported(policy, appVersionCode = 15))
        assertTrue(isAppVersionSupported(policy, appVersionCode = 0))
    }

    @Test
    fun clientWithoutCapabilityBlockFallsBackToEnabledFlag() {
        val disabled = PlaybackClientOverride(enabled = false)
        val enabled = PlaybackClientOverride(enabled = true)
        val unset = PlaybackClientOverride()

        PlaybackClientCapability.entries.forEach { capability ->
            assertFalse(disabled.isCapabilityEnabled(capability))
            assertTrue(enabled.isCapabilityEnabled(capability))
            assertTrue(unset.isCapabilityEnabled(capability))
        }
    }

    @Test
    fun perCapabilityOverrideNarrowsOnlyTheNamedCapabilities() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026090401,
              "clients": {
                "ANDROID_VR": {
                  "enabled": false,
                  "capabilities": { "browse": true, "metadata": true }
                },
                "VISIONOS": {
                  "capabilities": { "streaming": false }
                }
              }
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        parsed!!
        assertFalse(parsed.isClientCapabilityEnabled("ANDROID_VR", PlaybackClientCapability.PLAYER))
        assertFalse(parsed.isClientCapabilityEnabled("ANDROID_VR", PlaybackClientCapability.STREAMING))
        assertTrue(parsed.isClientCapabilityEnabled("ANDROID_VR", PlaybackClientCapability.BROWSE))
        assertTrue(parsed.isClientCapabilityEnabled("ANDROID_VR", PlaybackClientCapability.METADATA))

        assertTrue(parsed.isClientCapabilityEnabled("VISIONOS", PlaybackClientCapability.PLAYER))
        assertFalse(parsed.isClientCapabilityEnabled("VISIONOS", PlaybackClientCapability.STREAMING))

        assertTrue(parsed.isClientCapabilityEnabled("ANDROID", PlaybackClientCapability.PLAYER))
        assertTrue(parsed.isClientCapabilityEnabled("IOS", PlaybackClientCapability.STREAMING))
    }

    @Test
    fun capabilitiesSurviveSerializationRoundTrip() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val policy = base.copy(
            revision = base.revision + 1,
            clientOverrides = mapOf(
                "ANDROID_VR" to PlaybackClientOverride(
                    enabled = false,
                    capabilities = mapOf(
                        PlaybackClientCapability.BROWSE to true,
                        PlaybackClientCapability.METADATA to true
                    )
                )
            )
        )

        val restored = PlaybackCompatibilityPolicyParser.parse(policy.toJson(), base)

        assertNotNull(restored)
        assertEquals(policy.clientOverrides, restored!!.clientOverrides)
    }

    @Test
    fun malformedCapabilityBlockRejectsTheWholePayload() {
        val base = PlaybackCompatibilityPolicy.bundled()

        assertNull(
            PlaybackCompatibilityPolicyParser.parse(
                """
                {
                  "schema": 1,
                  "revision": 2026090402,
                  "clients": { "WEB": { "capabilities": { "player": "yes" } } }
                }
                """.trimIndent(),
                base
            )
        )
        assertNull(
            PlaybackCompatibilityPolicyParser.parse(
                """
                {
                  "schema": 1,
                  "revision": 2026090403,
                  "clients": { "WEB": { "capabilities": true } }
                }
                """.trimIndent(),
                base
            )
        )
    }

    @Test
    fun unknownCapabilityNameIsIgnoredWithoutLosingKnownOnes() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026090404,
              "clients": { "WEB": { "capabilities": { "browse": false, "future": true } } }
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        assertFalse(parsed!!.isClientCapabilityEnabled("WEB", PlaybackClientCapability.BROWSE))
        assertTrue(parsed.isClientCapabilityEnabled("WEB", PlaybackClientCapability.PLAYER))
    }

    @Test
    fun policyRemovingEveryPlayerCapableClientIsRejected() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val clients = base.clientOverrides.keys + setOf(
            "VISIONOS",
            "ANDROID_VR",
            "ANDROID_MUSIC",
            "ANDROID",
            "IOS",
            "WEB_REMIX",
            "WEB",
            "WEB_EMBEDDED_PLAYER"
        )
        val disabled = clients.joinToString(",") { """"$it": { "capabilities": { "player": false } }""" }

        assertNull(
            PlaybackCompatibilityPolicyParser.parse(
                """{ "schema": 1, "revision": 2026090405, "clients": { $disabled } }""",
                base
            )
        )
    }

    @Test
    fun policyLeavingNoClientWithBothPlayerAndStreamingIsRejected() {
        val base = PlaybackCompatibilityPolicy.bundled()
        val clients = setOf(
            "VISIONOS",
            "ANDROID_VR",
            "ANDROID_MUSIC",
            "ANDROID",
            "IOS",
            "WEB_REMIX",
            "WEB",
            "WEB_EMBEDDED_PLAYER"
        )
        val splitCapabilities = clients.mapIndexed { index, client ->
            val disabled = if (index % 2 == 0) "player" else "streaming"
            """"$client": { "capabilities": { "$disabled": false } }"""
        }.joinToString(",")

        assertNull(
            PlaybackCompatibilityPolicyParser.parse(
                """{ "schema": 1, "revision": 2026090406, "clients": { $splitCapabilities } }""",
                base
            )
        )
    }

    @Test
    fun policyKeepingOneCompletePlaybackPathIsAccepted() {
        val base = PlaybackCompatibilityPolicy.bundled()

        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026090407,
              "clients": {
                "VISIONOS": { "capabilities": { "streaming": false } },
                "WEB": { "capabilities": { "player": false } },
                "IOS": { "capabilities": { "player": true, "streaming": true } }
              }
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        assertTrue(parsed!!.isClientCapabilityEnabled("IOS", PlaybackClientCapability.PLAYER))
        assertTrue(parsed.isClientCapabilityEnabled("IOS", PlaybackClientCapability.STREAMING))
        assertFalse(parsed.isClientCapabilityEnabled("VISIONOS", PlaybackClientCapability.STREAMING))
        assertFalse(parsed.isClientCapabilityEnabled("WEB", PlaybackClientCapability.PLAYER))
    }

    @Test
    fun rejectedPolicyLeavesTheCallerOnItsPreviousKnownGoodPolicy() {
        val base = PlaybackCompatibilityPolicy.bundled()

        val rejected = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026090408,
              "clients": {
                "VISIONOS": { "capabilities": { "streaming": false } },
                "ANDROID_VR": { "capabilities": { "streaming": false } },
                "ANDROID_MUSIC": { "capabilities": { "streaming": false } },
                "ANDROID": { "capabilities": { "streaming": false } },
                "IOS": { "capabilities": { "streaming": false } },
                "WEB_REMIX": { "capabilities": { "streaming": false } },
                "WEB": { "capabilities": { "streaming": false } },
                "WEB_EMBEDDED_PLAYER": { "capabilities": { "streaming": false } }
              }
            }
            """.trimIndent(),
            base
        )

        assertNull(rejected)
        assertTrue(base.isClientCapabilityEnabled("IOS", PlaybackClientCapability.STREAMING))
    }

    @Test
    fun androidReelPathsSeeTheStreamingCapabilityOfTheAndroidClient() {
        val base = PlaybackCompatibilityPolicy.bundled()

        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026090409,
              "clients": {
                "ANDROID": { "capabilities": { "streaming": false } }
              }
            }
            """.trimIndent(),
            base
        )

        assertNotNull(parsed)
        assertFalse(parsed!!.isClientCapabilityEnabled("ANDROID", PlaybackClientCapability.STREAMING))
        assertTrue(parsed.isClientCapabilityEnabled("ANDROID", PlaybackClientCapability.PLAYER))
        assertTrue(base.isClientCapabilityEnabled("ANDROID", PlaybackClientCapability.STREAMING))
    }
}
