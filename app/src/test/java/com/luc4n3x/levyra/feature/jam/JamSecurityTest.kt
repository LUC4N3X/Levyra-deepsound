package com.luc4n3x.levyra.feature.jam

import java.io.BufferedReader
import java.io.StringReader
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class JamSecurityTest {

    @Test
    fun permissionsAllowOnlyTheDocumentedGuestActions() {
        val track = JamTrack("id", "Song", "Artist", 1_000L, "")

        assertFalse(JamAuthorization.allows(JamGuestPermission.HostOnly, JamAction.AddTrack(track)))
        assertFalse(JamAuthorization.allows(JamGuestPermission.AddSongs, JamAction.Next))
        assertTrue(JamAuthorization.allows(JamGuestPermission.AddSongs, JamAction.AddTrack(track)))
        assertTrue(JamAuthorization.allows(JamGuestPermission.Collaborative, JamAction.Next))
        assertTrue(JamAuthorization.allows(JamGuestPermission.Collaborative, JamAction.RemoveTrack("id")))
    }

    @Test
    fun boundedReaderRejectsFramesPastTheLimit() {
        assertNull(BufferedReader(StringReader("x".repeat(9))).readBoundedLine(8))
        assertTrue(BufferedReader(StringReader("hello\n")).readBoundedLine(8) == "hello")
    }

    @Test
    fun jamAuthGeneratesUniqueNonces() {
        val nonce1 = JamAuth.generateNonce()
        val nonce2 = JamAuth.generateNonce()
        assertEquals(32, nonce1.length)
        assertEquals(32, nonce2.length)
        assertNotEquals(nonce1, nonce2)
    }

    @Test
    fun jamAuthMutualChallengeResponseVerifiesMatchingSecret() {
        val secret = "0123456789"
        val hostNonce = JamAuth.generateNonce()
        val guestNonce = JamAuth.generateNonce()

        // Guest calculates proof and host verifies
        val guestProof = JamAuth.computeGuestProof(secret, hostNonce, guestNonce)
        val expectedGuestProof = JamAuth.computeGuestProof(secret, hostNonce, guestNonce)
        assertTrue(JamAuth.verifyProof(expectedGuestProof, guestProof))

        // Host calculates welcome proof and guest verifies
        val hostProof = JamAuth.computeHostProof(secret, hostNonce, guestNonce)
        val expectedHostProof = JamAuth.computeHostProof(secret, hostNonce, guestNonce)
        assertTrue(JamAuth.verifyProof(expectedHostProof, hostProof))
    }

    @Test
    fun jamAuthRejectsMismatchedSecretOrTamperedNonce() {
        val correctSecret = "0123456789"
        val wrongSecret = "9876543210"
        val hostNonce = JamAuth.generateNonce()
        val guestNonce = JamAuth.generateNonce()

        val forgedProof = JamAuth.computeGuestProof(wrongSecret, hostNonce, guestNonce)
        val expectedProof = JamAuth.computeGuestProof(correctSecret, hostNonce, guestNonce)
        assertFalse(JamAuth.verifyProof(expectedProof, forgedProof))

        val tamperedHostNonce = JamAuth.generateNonce()
        val proofWithTamperedNonce = JamAuth.computeGuestProof(correctSecret, tamperedHostNonce, guestNonce)
        assertFalse(JamAuth.verifyProof(expectedProof, proofWithTamperedNonce))
    }

    @Test
    fun jamAuthSeparatesGuestAndHostContexts() {
        val secret = "0123456789"
        val hostNonce = "aabbccddeeff00112233445566778899"
        val guestNonce = "11223344556677889900aabbccddeeff"

        val guestProof = JamAuth.computeGuestProof(secret, hostNonce, guestNonce)
        val hostProof = JamAuth.computeHostProof(secret, hostNonce, guestNonce)
        assertNotEquals(guestProof, hostProof)
        assertFalse(JamAuth.verifyProof(guestProof, hostProof))
    }
}
