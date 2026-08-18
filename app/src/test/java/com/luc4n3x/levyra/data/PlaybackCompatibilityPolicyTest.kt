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
        assertTrue(policy.clientOverrides.isEmpty())
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
        assertTrue(parsed!!.clientOverrides.isEmpty())
    }

    @Test
    fun unsupportedStrategyRejectsWholeCandidate() {
        val parsed = PlaybackCompatibilityPolicyParser.parse(
            """
            {
              "schema": 1,
              "revision": 2026081802,
              "audioStrategy": ["REMOTE_SCRIPT"]
            }
            """.trimIndent(),
            PlaybackCompatibilityPolicy.bundled()
        )

        assertNull(parsed)
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
}
