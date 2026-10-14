package com.luc4n3x.levyra.data.youtubemusic

import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test

class YoutubeMusicAccountProtocolTest {
    @Test fun authorizationUsesEpochSecondsAndMusicOrigin() {
        assertEquals("SAPISIDHASH 1700000000_7bd63eda096912c88692df43fb4e9dcf08fb421c", YoutubeMusicAuth.authorization("SAPISID=synthetic-test", 1700000000))
    }
    @Test fun secureCookieCanAuthenticateWithoutLegacyCookie() {
        assertEquals(YoutubeMusicAuth.authorization("SAPISID=synthetic-test", 42), YoutubeMusicAuth.authorization("__Secure-3PAPISID=synthetic-test", 42))
    }
    @Test fun cookieValuesKeepEqualsSigns() {
        assertEquals("test=a", YoutubeMusicAuth.cookies("SAPISID=test=a; PREF=hl=en")["SAPISID"])
    }
    @Test fun missingCookieRequiresLogin() {
        assertThrows(YoutubeMusicSessionExpired::class.java) { YoutubeMusicAuth.authorization("PREF=test", 42) }
    }
    @Test fun switcherPreservesBrandPageAndOwningGoogleIndex() {
        val json = JSONObject("""{"code":"SUCCESS","data":{"contents":[{"accountSectionListRenderer":{"contents":[{"accountItemSectionRenderer":{"contents":[{"accountItem":{"accountName":{"simpleText":"Test channel"},"channelHandle":{"simpleText":"@test"},"isSelected":true,"onBehalfOfParameter":"page-test","accountPhoto":{"thumbnails":[{"url":"https://example.com/test"}]},"serviceEndpoint":{"selectActiveIdentityEndpoint":{"supportedTokens":[{"accountSigninToken":{"signinUrl":"/signin?authuser=1&pageid=page-test"}}]}}}}]}}]}}]}}""")
        val account = YoutubeMusicAccountParser.accounts(json).single()
        assertEquals("page-test", account.pageId)
        assertEquals(1, account.authUser)
        assertTrue(account.selected)
        assertEquals("@test", account.handle)
    }
    @Test fun disabledAccountsAreNotSelectable() {
        val json = JSONObject("""{"code":"SUCCESS","data":{"accountItem":{"accountName":{"simpleText":"Disabled"},"isDisabled":true}}}""")
        assertTrue(YoutubeMusicAccountParser.accounts(json).isEmpty())
    }
    @Test fun malformedSwitcherIsNotAnEmptySuccessfulLibrary() {
        assertThrows(YoutubeMusicProtocolException::class.java) { YoutubeMusicAccountParser.accounts(JSONObject("{}")) }
    }
    @Test fun loginCompletionRequiresExactHttpsOrigin() {
        assertTrue(YoutubeMusicAuth.isMusicUrl("https://music.youtube.com/?authuser=1"))
        assertFalse(YoutubeMusicAuth.isMusicUrl("https://music.youtube.com.evil.test/"))
        assertFalse(YoutubeMusicAuth.isMusicUrl("http://music.youtube.com/"))
        assertFalse(YoutubeMusicAuth.isMusicUrl("https://evil.test@music.youtube.com/"))
    }
    @Test fun loginAllowsSecureWebRedirectChain() {
        assertTrue(YoutubeMusicAuth.isSecureWebUrl("https://accounts.youtube.com/accounts/SetSID?continue=https%3A%2F%2Fmusic.youtube.com%2F"))
        assertTrue(YoutubeMusicAuth.isSecureWebUrl("https://accounts.google.it/ServiceLogin"))
        assertTrue(YoutubeMusicAuth.isSecureWebUrl("https://gds.google.com/web/landing?continue=https%3A%2F%2Fmusic.youtube.com%2F"))
        assertTrue(YoutubeMusicAuth.isSecureWebUrl("https://new-login-host.google.com/redirect"))
        assertTrue(YoutubeMusicAuth.isSecureWebUrl("https://temporary-login-redirect.example/continue"))
    }
    @Test fun allowingGoogleSessionBridgeDoesNotAllowCookieCaptureThere() {
        assertFalse(YoutubeMusicAuth.isMusicUrl("https://accounts.youtube.com/accounts/SetSID"))
        assertFalse(YoutubeMusicAuth.isMusicUrl("https://youtube.com/signin"))
    }
    @Test fun loginRedirectPolicyRejectsUnsafeSchemesAndAuthorities() {
        assertFalse(YoutubeMusicAuth.isSecureWebUrl("https://accounts.youtube.com@evil.test/"))
        assertFalse(YoutubeMusicAuth.isSecureWebUrl("http://accounts.youtube.com/accounts/SetSID"))
        assertFalse(YoutubeMusicAuth.isSecureWebUrl("https://accounts.youtube.com:8443/accounts/SetSID"))
        assertFalse(YoutubeMusicAuth.isSecureWebUrl("intent://accounts.youtube.com/"))
        assertFalse(YoutubeMusicAuth.isSecureWebUrl("https:///missing-host"))
    }
    @Test fun continuationReadsGridAndActionShapes() {
        assertEquals("test-page", YoutubeMusicAccountParser.continuation(JSONObject("""{"continuationContents":{"gridContinuation":{"continuations":[{"nextContinuationData":{"continuation":"test-page"}}]}}}""")))
        assertEquals("test-action", YoutubeMusicAccountParser.continuation(JSONObject("""{"onResponseReceivedActions":[{"appendContinuationItemsAction":{"continuationItems":[{"continuationItemRenderer":{"continuationEndpoint":{"continuationCommand":{"token":"test-action"}}}}]}}]}""")))
    }
    @Test fun continuationIgnoresHeaderAndUnselectedTabs() {
        val json = JSONObject("""{"header":{"nextContinuationData":{"continuation":"wrong"}},"contents":{"singleColumnBrowseResultsRenderer":{"tabs":[{"tabRenderer":{"selected":false,"content":{"nextContinuationData":{"continuation":"wrong-tab"}}}},{"tabRenderer":{"selected":true,"content":{"nextContinuationData":{"continuation":"correct"}}}}]}}}""")
        assertEquals("correct", YoutubeMusicAccountParser.continuation(json))
    }
    @Test fun accountIdentityIncludesBrandPage() {
        assertNotEquals(YoutubeMusicAccountParser.identity("@test", "page-a", 0), YoutubeMusicAccountParser.identity("@test", "page-b", 0))
    }
}
