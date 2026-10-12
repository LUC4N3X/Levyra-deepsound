package com.luc4n3x.levyra.data.youtubemusic

import org.junit.Assert.*
import org.junit.Test

class YoutubeMusicSessionCodecTest {
    private val account = YoutubeMusicAccount("test-id", "Test", "@test", "", "test-page", 1)
    @Test fun roundTripKeepsActiveAccountAndCookieWithoutLoggingIt() {
        val state = YoutubeMusicSavedSessions(listOf(YoutubeMusicSession(account, "SAPISID=synthetic-test")), account.id)
        val result = YoutubeMusicSessionCodec.decode(YoutubeMusicSessionCodec.encode(state))
        assertEquals(account, result.sessions.single().account)
        assertEquals("SAPISID=synthetic-test", result.sessions.single().cookie)
        assertEquals(account.id, result.activeId)
        assertFalse(result.sessions.single().toString().contains("synthetic-test"))
    }
    @Test fun danglingActiveAccountIsDisconnected() {
        val state = YoutubeMusicSavedSessions(listOf(YoutubeMusicSession(account, "SAPISID=synthetic-test")), "missing")
        assertNull(YoutubeMusicSessionCodec.decode(YoutubeMusicSessionCodec.encode(state)).activeId)
    }
    @Test fun removingCurrentAccountDoesNotSilentlySwitchIdentity() {
        val state = YoutubeMusicSavedSessions(listOf(YoutubeMusicSession(account, "SAPISID=synthetic-test")), account.id)
        assertNull(state.without(account.id).activeId)
        assertTrue(state.without(account.id).sessions.isEmpty())
    }
    @Test fun onlySameIdentityGetsReplacedByNewLogin() {
        val another = account.copy(id = "other", pageId = "other-page")
        val state = YoutubeMusicSavedSessions(listOf(YoutubeMusicSession(account, "SAPISID=synthetic-test"), YoutubeMusicSession(another, "SAPISID=old-test")), account.id)
        val updated = state.withLogin(listOf(YoutubeMusicSession(account, "SAPISID=new-test")), account.id)
        assertEquals(2, updated.sessions.size)
        assertEquals("SAPISID=old-test", updated.sessions.single { it.account.id == "other" }.cookie)
    }
}
