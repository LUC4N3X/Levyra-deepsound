package com.luc4n3x.levyra.desktop.core.account

import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicDeviceLink
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkClient
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkDevice
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkException
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkOffer
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkPayload
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkResult
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkStatus
import com.luc4n3x.levyra.data.youtubemusic.YoutubeMusicLinkUnreachable
import java.net.ServerSocket
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test

class YoutubeMusicLinkHostTest {
    private val received = mutableListOf<YoutubeMusicLinkPayload>()
    private var signInResult = YoutubeMusicLinkResult(YoutubeMusicLinkStatus.Accepted, listOf("Listener"))
    private var sessionMs = 60_000L
    private val host by lazy {
        YoutubeMusicLinkHost(
            signIn = { payload -> received += payload; signInResult },
            addresses = { listOf(LinkAddress("test", "10.255.255.1")) },
            device = { YoutubeMusicLinkDevice("STUDIO-PC", "Windows 11") },
            sessionMs = sessionMs,
            confirmMs = 5_000
        )
    }

    @After
    fun tearDown() = host.dispose()

    private fun localOffer(): YoutubeMusicLinkOffer {
        val waiting = host.state.value as YoutubeMusicLinkState.Waiting
        val parsed = requireNotNull(YoutubeMusicLinkOffer.parse(waiting.code))
        return YoutubeMusicLinkOffer(listOf("127.0.0.1"), parsed.port, parsed.publicKey, parsed.session)
    }

    private suspend fun awaitState(predicate: (YoutubeMusicLinkState) -> Boolean): YoutubeMusicLinkState =
        withTimeout(5_000) { host.state.first(predicate) }

    @Test
    fun phoneSignsInThroughTheCode() = runBlocking {
        host.start()
        val connection = YoutubeMusicLinkClient.connect(localOffer())
        assertEquals(YoutubeMusicLinkDevice("STUDIO-PC", "Windows 11"), connection.device)
        awaitState { it == YoutubeMusicLinkState.PhoneConnected }
        val result = connection.send(YoutubeMusicLinkPayload("SAPISID=abc; SID=def", "account-1"))
        assertEquals(YoutubeMusicLinkStatus.Accepted, result.status)
        assertEquals(listOf("Listener"), result.accounts)
        assertEquals(YoutubeMusicLinkState.Done(listOf("Listener")), awaitState { it is YoutubeMusicLinkState.Done })
        assertEquals("SAPISID=abc; SID=def", received.single().cookie)
        assertEquals("account-1", received.single().accountId)
    }

    @Test
    fun anotherCodeIsRefusedAndTheQrStays() = runBlocking {
        host.start()
        val waiting = host.state.value
        val real = localOffer()
        val forged = YoutubeMusicLinkOffer(real.hosts, real.port, real.publicKey, YoutubeMusicDeviceLink.newSession())
        try {
            YoutubeMusicLinkClient.connect(forged)
            fail("forged code connected")
        } catch (error: YoutubeMusicLinkException) {
            assertEquals(YoutubeMusicLinkStatus.Rejected, error.status)
        }
        assertEquals(waiting, host.state.value)
        assertTrue(received.isEmpty())
    }

    @Test
    fun cancellingOnThePhoneReturnsToTheQr() = runBlocking {
        host.start()
        val waiting = host.state.value
        val connection = YoutubeMusicLinkClient.connect(localOffer())
        awaitState { it == YoutubeMusicLinkState.PhoneConnected }
        connection.cancel()
        assertEquals(waiting, awaitState { it is YoutubeMusicLinkState.Waiting })
    }

    @Test
    fun failedSignInIsReportedOnBothSides() = runBlocking {
        signInResult = YoutubeMusicLinkResult(YoutubeMusicLinkStatus.SignInFailed, emptyList())
        host.start()
        val result = YoutubeMusicLinkClient.connect(localOffer()).send(YoutubeMusicLinkPayload("SID=x", ""))
        assertEquals(YoutubeMusicLinkStatus.SignInFailed, result.status)
        assertEquals(YoutubeMusicLinkState.Rejected(LinkRejection.SignInFailed), awaitState { it is YoutubeMusicLinkState.Rejected })
    }

    @Test
    fun spentCodeAcceptsNoSecondPhone() = runBlocking {
        host.start()
        val offer = localOffer()
        YoutubeMusicLinkClient.connect(offer).send(YoutubeMusicLinkPayload("SID=x", ""))
        awaitState { it is YoutubeMusicLinkState.Done }
        try {
            YoutubeMusicLinkClient.connect(offer)
            fail("spent code connected")
        } catch (_: YoutubeMusicLinkUnreachable) {
        }
    }

    @Test
    fun codeExpires() = runBlocking {
        sessionMs = 200L
        host.start()
        assertEquals(YoutubeMusicLinkState.Expired, awaitState { it == YoutubeMusicLinkState.Expired })
    }

    @Test
    fun closedPortIsUnreachable() = runBlocking {
        val port = ServerSocket(0).use { it.localPort }
        val offer = YoutubeMusicLinkOffer(listOf("127.0.0.1"), port, YoutubeMusicDeviceLink.newHostKeys().public.encoded, YoutubeMusicDeviceLink.newSession())
        try {
            YoutubeMusicLinkClient.connect(offer)
            fail("closed port connected")
        } catch (_: YoutubeMusicLinkUnreachable) {
        }
    }

    @Test
    fun codeParsingAcceptsOnlyWellFormedOffers() {
        val keys = YoutubeMusicDeviceLink.newHostKeys().public.encoded
        val offer = YoutubeMusicLinkOffer(listOf("192.168.1.20", "100.101.4.9"), 41234, keys, YoutubeMusicDeviceLink.newSession())
        val parsed = YoutubeMusicLinkOffer.parse(offer.uri())!!
        assertEquals(offer.hosts, parsed.hosts)
        assertEquals(offer.port, parsed.port)
        assertTrue(parsed.publicKey.contentEquals(keys))
        assertNull(YoutubeMusicLinkOffer.parse(offer.uri().replace("192.168.1.20", "127.0.0.1")))
        assertNull(YoutubeMusicLinkOffer.parse(offer.uri().replace("192.168.1.20", "example.com")))
        assertNull(YoutubeMusicLinkOffer.parse(offer.uri().replace("v=1", "v=2")))
        assertNull(YoutubeMusicLinkOffer.parse(offer.uri().replace("p=41234", "p=80")))
        assertNull(YoutubeMusicLinkOffer.parse("https://example.com/?h=192.168.1.20"))
    }
}
