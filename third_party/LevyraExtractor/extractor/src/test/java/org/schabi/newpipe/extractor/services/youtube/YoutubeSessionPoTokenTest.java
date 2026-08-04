package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonObject;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrInfo;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrProbe;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSessionPoTokenTest {
    private static final Localization LOCALIZATION = new Localization("en", "US");
    private static final ContentCountry CONTENT_COUNTRY = new ContentCountry("US");

    @AfterEach
    void tearDown() {
        NewPipe.setYoutubeSessionPoTokenProvider(null);
        ServiceList.YouTube.setTokens(null);
    }

    @Test
    void injectsMatchingVisitorDataAndTokenForLoggedOutRequest() throws Exception {
        final AtomicBoolean loggedIn = new AtomicBoolean(true);
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            assertEquals("ANDROID_VR", clientName);
            assertEquals("1.65.10", clientVersion);
            assertEquals("test-user-agent", userAgent);
            loggedIn.set(login);
            return new YoutubeSessionPoToken("visitor-out", "session-token-out");
        });

        final JsonObject body = decorate(playerBody("ANDROID_VR"));

        assertFalse(loggedIn.get());
        assertEquals("visitor-out", body.getObject("context").getObject("client")
                .getString("visitorData"));
        assertEquals("session-token-out", body.getObject("serviceIntegrityDimensions")
                .getString("poToken"));
    }

    @Test
    void reportsLoggedInStateAndInjectsTokenWithoutRemovingPlayerFields() throws Exception {
        ServiceList.YouTube.setTokens("SAPISID=test; __Secure-3PAPISID=test");
        final AtomicBoolean loggedIn = new AtomicBoolean(false);
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            loggedIn.set(login);
            return new YoutubeSessionPoToken("visitor-in", "session-token-in");
        });

        final JsonObject body = decorate(playerBody("WEB"));

        assertTrue(loggedIn.get());
        assertEquals("video", body.getString("videoId"));
        assertEquals("visitor-in", body.getObject("context").getObject("client")
                .getString("visitorData"));
        assertEquals("session-token-in", body.getObject("serviceIntegrityDimensions")
                .getString("poToken"));
    }

    @Test
    void preservesExplicitTokenAndDoesNotCallProvider() throws Exception {
        final byte[] body = ("{\"context\":{\"client\":{\"clientName\":\"WEB\","
                + "\"visitorData\":\"explicit-visitor\"}},\"videoId\":\"video\","
                + "\"serviceIntegrityDimensions\":{\"poToken\":\"explicit-token\"}}")
                .getBytes(StandardCharsets.UTF_8);
        final AtomicInteger calls = new AtomicInteger();
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            calls.incrementAndGet();
            return new YoutubeSessionPoToken("new-visitor", "new-token");
        });

        final byte[] decorated = YoutubeParsingHelper.addSessionPoTokenToPlayerBody(body,
                LOCALIZATION, CONTENT_COUNTRY);

        assertArrayEquals(body, decorated);
        assertEquals(0, calls.get());
    }

    @Test
    void providerFailureKeepsPreviousExtractionRequest() {
        final byte[] body = playerBody("WEB");
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            throw new IllegalStateException("no WebView");
        });

        final byte[] decorated = YoutubeParsingHelper.addSessionPoTokenToPlayerBody(body,
                LOCALIZATION, CONTENT_COUNTRY);

        assertArrayEquals(body, decorated);
    }

    @Test
    void preparedPlayerRequestRetainsTheExactProviderVisitor() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        NewPipe.setYoutubeSessionPoTokenProvider((clientName, clientVersion, userAgent,
                                                   localization, contentCountry, login) -> {
            final int call = calls.incrementAndGet();
            return new YoutubeSessionPoToken("visitor-" + call, "token-" + call);
        });

        final YoutubePlayerRequest request =
                YoutubeParsingHelper.prepareSessionPoTokenPlayerRequest(
                        playerBody("MWEB"), LOCALIZATION, CONTENT_COUNTRY);
        final JsonObject decorated = JsonUtils.toJsonObject(
                new String(request.getBody(), StandardCharsets.UTF_8));

        assertEquals(1, calls.get());
        assertEquals("visitor-1", request.getVisitorData());
        assertEquals("2.test", request.getClientVersion());
        assertEquals("visitor-1", decorated.getObject("context").getObject("client")
                .getString("visitorData"));
        assertEquals("token-1", decorated.getObject("serviceIntegrityDimensions")
                .getString("poToken"));
    }

    @Test
    void sabrInfoKeepsRequestVisitorWhenPlayerResponseDoesNotEchoIt() throws Exception {
        final JsonObject response = JsonUtils.toJsonObject("{\"streamingData\":{"
                + "\"serverAbrStreamingUrl\":\"https://example.com/sabr\","
                + "\"adaptiveFormats\":[]}}");

        final YoutubeSabrInfo info = YoutubeSabrProbe.fromPlayerResponse("video",
                YoutubeSabrClientProfile.MWEB, "cpn", response, "request-visitor");

        assertEquals("request-visitor", info.getVisitorData());
    }

    private static JsonObject decorate(final byte[] body) throws Exception {
        return JsonUtils.toJsonObject(new String(
                YoutubeParsingHelper.addSessionPoTokenToPlayerBody(body, LOCALIZATION,
                        CONTENT_COUNTRY),
                StandardCharsets.UTF_8));
    }

    private static byte[] playerBody(final String clientName) {
        return ("{\"context\":{\"client\":{\"clientName\":\"" + clientName
                + "\",\"clientVersion\":\"" + clientVersion(clientName)
                + "\",\"userAgent\":\"test-user-agent\"}},\"videoId\":\"video\"}")
                .getBytes(StandardCharsets.UTF_8);
    }

    private static String clientVersion(final String clientName) {
        return "ANDROID_VR".equals(clientName) ? "1.65.10" : "2.test";
    }
}
