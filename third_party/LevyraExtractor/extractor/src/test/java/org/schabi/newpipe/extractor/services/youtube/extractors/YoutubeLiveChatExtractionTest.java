package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.ServiceList;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeLiveChatExtractionTest {

    private static final String VIDEO_URL = "https://www.youtube.com/watch?v=aBcDeFgHiJk";
    private static final int SERVICE_ID = ServiceList.YouTube.getServiceId();

    private static JsonObject json(final String value) throws JsonParserException {
        return JsonParser.object().from(value);
    }

    private static List<CommentsInfoItem> collect(final JsonObject liveChatContinuation) {
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(SERVICE_ID);
        YoutubeCommentsExtractor.collectLiveChatMessages(liveChatContinuation, collector,
                VIDEO_URL);
        return collector.getItems();
    }

    @Test
    void parsesTextMessageRenderer() throws Exception {
        final List<CommentsInfoItem> items = collect(json("{\"actions\":[{\"addChatItemAction\":"
                + "{\"item\":{\"liveChatTextMessageRenderer\":{"
                + "\"id\":\"ChatMessage1\","
                + "\"message\":{\"runs\":[{\"text\":\"hello world\"}]},"
                + "\"authorName\":{\"simpleText\":\"Someone\"},"
                + "\"authorExternalChannelId\":\"UC1234567890abcdefghijkl\","
                + "\"authorPhoto\":{\"thumbnails\":["
                + "{\"url\":\"https://yt3.ggpht.com/small\",\"width\":32},"
                + "{\"url\":\"https://yt3.ggpht.com/large\",\"width\":64}]},"
                + "\"timestampUsec\":\"1700000000000000\","
                + "\"timestampText\":{\"simpleText\":\"1:23\"},"
                + "\"authorBadges\":[{\"liveChatAuthorBadgeRenderer\":"
                + "{\"icon\":{\"iconType\":\"VERIFIED\"}}}]"
                + "}}}}]}"));

        assertEquals(1, items.size());
        final CommentsInfoItem item = items.get(0);
        assertEquals("ChatMessage1", item.getCommentId());
        assertEquals("hello world", item.getCommentText());
        assertEquals("Someone", item.getUploaderName());
        assertEquals("https://www.youtube.com/channel/UC1234567890abcdefghijkl",
                item.getUploaderUrl());
        assertEquals("https://yt3.ggpht.com/large", item.getUploaderAvatarUrl());
        assertEquals("1:23", item.getTextualUploadDate());
        assertNotNull(item.getUploadDate());
        assertTrue(item.isLiveChat());
    }

    @Test
    void parsesReplayActions() throws Exception {
        final List<CommentsInfoItem> items = collect(json("{\"actions\":[{\"replayChatItemAction\":"
                + "{\"actions\":[{\"addChatItemAction\":{\"item\":"
                + "{\"liveChatTextMessageRenderer\":{\"id\":\"Replay1\","
                + "\"message\":{\"runs\":[{\"text\":\"replayed\"}]},"
                + "\"authorName\":{\"simpleText\":\"Viewer\"}}}}}]}}]}"));

        assertEquals(1, items.size());
        assertEquals("replayed", items.get(0).getCommentText());
        assertTrue(items.get(0).isLiveChat());
    }

    @Test
    void skipsUnknownRenderers() throws Exception {
        final List<CommentsInfoItem> items = collect(json("{\"actions\":["
                + "{\"addChatItemAction\":{\"item\":{\"liveChatPaidMessageRenderer\":"
                + "{\"id\":\"Paid1\"}}}},"
                + "{\"addChatItemAction\":{\"item\":{\"liveChatSomethingBrandNewRenderer\":{}}}},"
                + "{\"markChatItemAsDeletedAction\":{}},"
                + "\"unexpected string action\","
                + "{\"addChatItemAction\":{\"item\":{\"liveChatTextMessageRenderer\":"
                + "{\"id\":\"Text1\",\"message\":{\"runs\":[{\"text\":\"kept\"}]},"
                + "\"authorName\":{\"simpleText\":\"Viewer\"}}}}}]}"));

        assertEquals(1, items.size());
        assertEquals("kept", items.get(0).getCommentText());
    }

    @Test
    void toleratesIncompletePayloads() throws Exception {
        assertTrue(collect(json("{}")).isEmpty());
        assertTrue(collect(json("{\"actions\":[]}")).isEmpty());
        assertTrue(collect(json("{\"actions\":[{\"addChatItemAction\":{}}]}")).isEmpty());
        assertTrue(collect(json("{\"actions\":[{\"replayChatItemAction\":{}}]}")).isEmpty());
        final CommentsInfoItemsCollector nullCollector =
                new CommentsInfoItemsCollector(SERVICE_ID);
        YoutubeCommentsExtractor.collectLiveChatMessages(null, nullCollector, VIDEO_URL);
        assertTrue(nullCollector.getItems().isEmpty());

        final List<CommentsInfoItem> items = collect(json("{\"actions\":[{\"addChatItemAction\":"
                + "{\"item\":{\"liveChatTextMessageRenderer\":{\"id\":\"Bare\"}}}}]}"));
        assertEquals(1, items.size());
        assertEquals("", items.get(0).getCommentText());
        assertEquals("", items.get(0).getUploaderName());
        assertEquals("", items.get(0).getUploaderUrl());
        assertEquals("", items.get(0).getUploaderAvatarUrl());
        assertNull(items.get(0).getUploadDate());
    }

    @Test
    void readsVerifiedAuthorBadge() throws Exception {
        assertTrue(new YoutubeLiveChatInfoItemExtractor(
                json("{\"authorBadges\":[{\"liveChatAuthorBadgeRenderer\":"
                        + "{\"icon\":{\"iconType\":\"VERIFIED\"}}}]}"), VIDEO_URL)
                .isUploaderVerified());
        assertFalse(new YoutubeLiveChatInfoItemExtractor(
                json("{\"authorBadges\":[{\"liveChatAuthorBadgeRenderer\":"
                        + "{\"icon\":{\"iconType\":\"MODERATOR\"}}}]}"), VIDEO_URL)
                .isUploaderVerified());
        assertFalse(new YoutubeLiveChatInfoItemExtractor(json("{}"), VIDEO_URL)
                .isUploaderVerified());
    }

    @Test
    void resolvesStandardEmoji() throws Exception {
        final List<CommentsInfoItem> items = collect(json("{\"actions\":[{\"addChatItemAction\":"
                + "{\"item\":{\"liveChatTextMessageRenderer\":{\"id\":\"Emoji1\","
                + "\"authorName\":{\"simpleText\":\"Viewer\"},"
                + "\"message\":{\"runs\":[{\"text\":\"nice \"},"
                + "{\"emoji\":{\"emojiId\":\"\\uD83D\\uDD25\",\"shortcuts\":[\":fire:\"],"
                + "\"searchTerms\":[\"fire\"]}}]}}}}}]}"));

        assertEquals("nice 🔥", items.get(0).getCommentText());
    }

    @Test
    void fallsBackForCustomEmoji() throws Exception {
        assertEquals(":_levyra:", YoutubeLiveChatInfoItemExtractor.extractEmojiText(
                json("{\"emojiId\":\"UCchannel/abcdef\",\"isCustomEmoji\":true,"
                        + "\"shortcuts\":[\":_levyra:\",\":_alt:\"],"
                        + "\"searchTerms\":[\"levyra\"]}")));

        assertEquals(":levyra:", YoutubeLiveChatInfoItemExtractor.extractEmojiText(
                json("{\"emojiId\":\"UCchannel/abcdef\",\"isCustomEmoji\":true,"
                        + "\"shortcuts\":[\"\",\"   \"],\"searchTerms\":[\"levyra\"]}")));

        assertEquals("levyra emoji", YoutubeLiveChatInfoItemExtractor.extractEmojiText(
                json("{\"emojiId\":\"UCchannel/abcdef\",\"isCustomEmoji\":true,"
                        + "\"image\":{\"accessibility\":{\"accessibilityData\":"
                        + "{\"label\":\"levyra emoji\"}}}}")));

        assertEquals("UCchannel/abcdef", YoutubeLiveChatInfoItemExtractor.extractEmojiText(
                json("{\"emojiId\":\"UCchannel/abcdef\",\"isCustomEmoji\":true}")));

        assertEquals("", YoutubeLiveChatInfoItemExtractor.extractEmojiText(json("{}")));
        assertEquals("", YoutubeLiveChatInfoItemExtractor.extractEmojiText(null));
    }

    @Test
    void extractsInitialContinuation() throws Exception {
        final YoutubeCommentsExtractor.LiveChatContinuation continuation =
                YoutubeCommentsExtractor.extractInitialLiveChatContinuation(
                        json("{\"contents\":{\"twoColumnWatchNextResults\":{\"conversationBar\":"
                                + "{\"liveChatRenderer\":{\"isReplay\":true,\"continuations\":["
                                + "{\"reloadContinuationData\":{\"continuation\":\"TOKEN-1\"}}]"
                                + "}}}}}"));

        assertNotNull(continuation);
        assertEquals("TOKEN-1", continuation.token);
        assertTrue(continuation.replay);
    }

    @Test
    void returnsNoContinuationWhenLiveChatIsUnavailable() throws Exception {
        assertNull(YoutubeCommentsExtractor.extractInitialLiveChatContinuation(null));
        assertNull(YoutubeCommentsExtractor.extractInitialLiveChatContinuation(json("{}")));
        assertNull(YoutubeCommentsExtractor.extractInitialLiveChatContinuation(
                json("{\"contents\":{\"twoColumnWatchNextResults\":{\"conversationBar\":"
                        + "{\"conversationBarRenderer\":{}}}}}")));
        assertNull(YoutubeCommentsExtractor.extractInitialLiveChatContinuation(
                json("{\"contents\":{\"twoColumnWatchNextResults\":{\"conversationBar\":"
                        + "{\"liveChatRenderer\":{\"continuations\":[{}]}}}}}")));
    }

    @Test
    void preservesAndBoundsContinuationPollDelay() throws Exception {
        final YoutubeCommentsExtractor.LiveChatContinuation timed =
                YoutubeCommentsExtractor.extractNextLiveChatContinuationData(
                        json("{\"continuations\":[{\"timedContinuationData\":"
                                + "{\"continuation\":\"TIMED\",\"timeoutMs\":8500}}]}"),
                        false);
        assertNotNull(timed);
        assertEquals("TIMED", timed.token);
        assertEquals(8500L, timed.pollDelayMs);

        final YoutubeCommentsExtractor.LiveChatContinuation bounded =
                YoutubeCommentsExtractor.extractNextLiveChatContinuationData(
                        json("{\"continuations\":[{\"invalidationContinuationData\":"
                                + "{\"continuation\":\"FAST\",\"timeoutMs\":999999}}]}"),
                        true);
        assertNotNull(bounded);
        assertTrue(bounded.replay);
        assertEquals(60000L, bounded.pollDelayMs);
    }

    @Test
    void extractsNextContinuationForEveryKnownType() throws Exception {
        assertEquals("INVALIDATION", YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"timedContinuationData\":"
                        + "{\"continuation\":\"TIMED\"}},"
                        + "{\"invalidationContinuationData\":"
                        + "{\"continuation\":\"INVALIDATION\"}}]}")));

        assertEquals("TIMED", YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"timedContinuationData\":"
                        + "{\"continuation\":\"TIMED\"}}]}")));

        assertEquals("REPLAY", YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"playerSeekContinuationData\":"
                        + "{\"continuation\":\"SEEK\"}},"
                        + "{\"liveChatReplayContinuationData\":"
                        + "{\"continuation\":\"REPLAY\"}}]}")));

        assertEquals("RELOAD", YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"reloadContinuationData\":"
                        + "{\"continuation\":\"RELOAD\"}}]}")));

        assertNull(YoutubeCommentsExtractor.extractNextLiveChatContinuation(null));
        assertNull(YoutubeCommentsExtractor.extractNextLiveChatContinuation(json("{}")));
        assertNull(YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"unknownContinuationData\":"
                        + "{\"continuation\":\"NOPE\"}},\"junk\"]}")));
        assertNull(YoutubeCommentsExtractor.extractNextLiveChatContinuation(
                json("{\"continuations\":[{\"timedContinuationData\":{\"continuation\":\"\"}}]}")));
    }
}
