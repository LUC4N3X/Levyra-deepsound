package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.AgeRestrictedContentException;
import org.schabi.newpipe.extractor.exceptions.AntiBotException;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.PaidContentException;
import org.schabi.newpipe.extractor.exceptions.PrivateContentException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubePlayabilityStatusTest {

    private static JsonObject status(final String json) throws Exception {
        return JsonParser.object().from(json);
    }

    @Test
    void okStatusPassesThrough() throws Exception {
        assertNull(YoutubeStreamExtractor.checkPlayabilityStatus(
                status("{\"status\":\"OK\"}"), "dQw4w9WgXcQ"));
    }

    @Test
    void privateVideoIsDetectedFromMessagesEvenWithAReason() {
        assertThrows(PrivateContentException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"LOGIN_REQUIRED\","
                        + "\"reason\":\"Sign in if you've been granted access to this video\","
                        + "\"messages\":[\"This video is private\"]}"), "dQw4w9WgXcQ"));
    }

    @Test
    void privateVideoWithoutReasonIsStillDetected() {
        assertThrows(PrivateContentException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"LOGIN_REQUIRED\","
                        + "\"messages\":[\"This video is private\"]}"), "dQw4w9WgXcQ"));
    }

    @Test
    void currentAgeRestrictionReasonsAreClassifiedAsAgeRestricted() {
        assertThrows(AgeRestrictedContentException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"LOGIN_REQUIRED\","
                        + "\"reason\":\"This video may be inappropriate for some users.\"}"),
                        "dQw4w9WgXcQ"));
        assertThrows(AgeRestrictedContentException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"LOGIN_REQUIRED\","
                        + "\"reason\":\"Sign in to confirm your age\"}"), "dQw4w9WgXcQ"));
    }

    @Test
    void botCheckStaysAntiBotEvenWhenAMessageMentionsAPage() {
        final AntiBotException error = assertThrows(AntiBotException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"LOGIN_REQUIRED\","
                        + "\"reason\":\"Sign in to confirm you’re not a bot. This helps protect our community. Learn more\","
                        + "\"messages\":[\"Please sign in on this page\"]}"),
                        "dQw4w9WgXcQ"));
        assertEquals(AntiBotException.class, error.getClass());
    }

    @Test
    void membersReasonVariantsAreClassifiedAsPaidContent() {
        assertThrows(PaidContentException.class, () ->
                YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                        + "\"status\":\"UNPLAYABLE\","
                        + "\"reason\":\"Join this channel to get access to members content like this video\"}"),
                        "dQw4w9WgXcQ"));
    }

    @Test
    void unknownLoginRequiredReasonFallsBackToContentNotAvailable() {
        final ContentNotAvailableException error = assertThrows(
                ContentNotAvailableException.class, () ->
                        YoutubeStreamExtractor.checkPlayabilityStatus(status("{"
                                + "\"status\":\"LOGIN_REQUIRED\","
                                + "\"reason\":\"Something new\","
                                + "\"messages\":[42, null]}"), "dQw4w9WgXcQ"));
        assertEquals(ContentNotAvailableException.class, error.getClass());
    }
}
