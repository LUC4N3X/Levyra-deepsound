package com.luc4n3x.levyra.feature.jam

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.zxing.common.BitMatrix
import com.google.zxing.qrcode.decoder.Decoder
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class JamQrJoinDeviceTest {
    @Test
    fun decodedInvitationJoinsRealLanSessionAfterHostApproval() = runBlocking<Unit> {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val host = JamController(scope, TestBridge())
        val guest = JamController(scope, TestBridge())
        try {
            host.createJam("QR host", JamGuestPermission.AddSongs, approvalRequired = true)
            val invitation = host.state.value.deepLink
            assertTrue("Host needs a private LAN address", invitation.isNotBlank())
            val payload = decodeInvitation(invitation)
            val joining = scope.launch { guest.joinJam(payload, "QR guest", "112233445566778899aabbccddeeff00") }
            val request = withTimeout(10_000) { host.state.first { it.pending.isNotEmpty() } }.pending.single()
            withTimeout(10_000) { guest.state.first { it.connection == JamConnectionState.AwaitingApproval } }
            host.approveParticipant(request.participantId)
            joining.join()
            val joined = withTimeout(10_000) { guest.state.first { it.connection == JamConnectionState.Connected && it.session != null } }
            assertEquals(host.state.value.session?.sessionId, joined.session?.sessionId)
            assertEquals(2, joined.session?.participants?.size)
            host.endJam()
            withTimeout(10_000) { guest.state.first { it.failure == JamFailure.HostEnded } }
        } finally {
            host.close()
            guest.close()
            scope.cancel()
        }
    }

    @Test
    fun expiredAndInvalidInvitationsReturnExistingJamFailures() = runBlocking {
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
        val host = JamController(scope, TestBridge())
        val guest = JamController(scope, TestBridge())
        try {
            host.createJam("Expired QR host", JamGuestPermission.AddSongs, approvalRequired = false)
            val payload = decodeInvitation(host.state.value.deepLink)
            host.endJam()
            delay(100)
            withTimeout(15_000) { guest.joinJam(payload, "Guest", "112233445566778899aabbccddeeff00") }
            assertEquals(JamFailure.ConnectionFailed, guest.state.value.failure)
            guest.joinJam("https://example.com/", "Guest", "112233445566778899aabbccddeeff00")
            assertEquals(JamFailure.InvalidCode, guest.state.value.failure)
        } finally {
            host.close()
            guest.close()
            scope.cancel()
        }
    }

    private fun decodeInvitation(invitation: String): String {
        val image = JamInvitationQr.encode(invitation)
        val bounds = image.enclosingRectangle
        val cropped = BitMatrix(bounds[2], bounds[3])
        for (y in 0 until bounds[3]) for (x in 0 until bounds[2]) {
            if (image[x + bounds[0], y + bounds[1]]) cropped.set(x, y)
        }
        return Decoder().decode(cropped).text
    }

    private class TestBridge : JamPlayerBridge {
        override fun snapshot() = JamPlaybackSnapshot()
        override suspend fun applyRemoteState(state: JamSessionState) = Unit
        override suspend fun applyAction(action: JamAction) = Unit
    }
}
