package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeApiDecoderLocalFallbackTest {

    private static final int DECODE_CACHE_MAX_ENTRIES = 512;

    @AfterEach
    void tearDown() {
        YoutubeApiDecoder.setLocalDecoder(null);
        YoutubeJavaScriptPlayerManager.clearAllCaches();
    }

    @Test
    void nTransformationChangesOnlyTheQueryParameter() throws Exception {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0));
        final String url = "https://example.test/n1/media?other=n1&n=n1&tail=n1#n=n1";

        assertEquals("https://example.test/n1/media?other=n1&n=decoded-n1&tail=n1#n=n1",
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated("video", url));
    }

    @Test
    void nestedQuestionMarkDoesNotStartAnotherQuery() throws Exception {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0));
        assertEquals("https://example.test/media?other=?n=n1&n=decoded-n2",
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated(
                        "video", "https://example.test/media?other=?n=n1&n=n2"));
        final String withoutN = "https://example.test/media?other=?n=n1";
        assertEquals(withoutN, YoutubeJavaScriptPlayerManager
                .getUrlWithThrottlingParameterDeobfuscated("video", withoutN));
    }

    @Test
    void fragmentDoesNotContainAThrottlingQueryParameter() throws Exception {
        final String url = "https://example.test/media?x=1#fragment?n=n1";
        assertEquals(url,
                YoutubeJavaScriptPlayerManager.getUrlWithThrottlingParameterDeobfuscated("video", url));
    }

    @Test
    void challengesAreDeduplicatedWithoutBypassingTheLocalDecoderEpoch() throws Exception {
        final List<List<String>> calls = new ArrayList<>();
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0) {
            @Override
            public YoutubeApiDecoder.BatchDecodeResult decodeBatch(final String playerId,
                    final List<String> signatures, final List<String> throttlingParameters) {
                calls.add(new ArrayList<>(signatures));
                return super.decodeBatch(playerId, signatures, throttlingParameters);
            }
        });
        YoutubeApiDecoder.decodeBatch("player", Arrays.asList("s1", "s1", "s2"), null);
        final YoutubeApiDecoder.BatchDecodeResult result = YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1", "s2"), null);

        assertEquals(Arrays.asList(Arrays.asList("s1", "s2"), Arrays.asList("s1", "s2")), calls);
        assertEquals("decoded-s1", result.getSignatures().get("s1"));
        assertEquals("decoded-s2", result.getSignatures().get("s2"));
    }

    @Test
    void localDecoderRemainsTheAuthorityAfterItsGenerationChanges() throws Exception {
        final java.util.concurrent.atomic.AtomicInteger generation = new java.util.concurrent.atomic.AtomicInteger();
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0) {
            @Override
            public YoutubeApiDecoder.BatchDecodeResult decodeBatch(final String playerId,
                    final List<String> signatures, final List<String> throttlingParameters) {
                return new YoutubeApiDecoder.BatchDecodeResult(
                        java.util.Collections.singletonMap("s1", "decoded-" + generation.get()),
                        java.util.Collections.emptyMap());
            }
        });
        assertEquals("decoded-0", YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1"), null).getSignatures().get("s1"));
        generation.incrementAndGet();
        assertEquals("decoded-1", YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1"), null).getSignatures().get("s1"));
    }

    @Test
    void localCancellationDoesNotStartRemoteFallback() {
        final java.util.concurrent.CancellationException expected =
                new java.util.concurrent.CancellationException("cancelled");
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0) {
            @Override
            public YoutubeApiDecoder.BatchDecodeResult decodeBatch(final String playerId,
                    final List<String> signatures, final List<String> throttlingParameters) {
                throw expected;
            }
        });
        org.junit.jupiter.api.Assertions.assertSame(expected,
                assertThrows(java.util.concurrent.CancellationException.class,
                        () -> YoutubeApiDecoder.decodeBatch("player", Arrays.asList("s1"), null)));
        org.junit.jupiter.api.Assertions.assertSame(expected,
                assertThrows(java.util.concurrent.CancellationException.class,
                        () -> YoutubeApiDecoder.decodeSignature("player", "s1")));
    }

    @Test
    void completeLocalBatchIsReturnedAndCached() throws Exception {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0));

        final YoutubeApiDecoder.BatchDecodeResult result = YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1", "s2"), Arrays.asList("n1"));

        assertEquals("decoded-s1", result.getSignatures().get("s1"));
        assertEquals("decoded-s2", result.getSignatures().get("s2"));
        assertEquals("decoded-n1", result.getNParameters().get("n1"));
        assertEquals(3, YoutubeApiDecoder.getCacheSize());
    }

    @Test
    void partialLocalBatchCachesValidValuesBeforeRemoteFallback() throws Exception {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(1));

        assertThrows(ParsingException.class, () -> YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1", "s2"), null));

        assertEquals(1, YoutubeApiDecoder.getCacheSize());
        assertEquals("decoded-s1", YoutubeApiDecoder.decodeSignature("player", "s1"));
    }

    @Test
    void unchangedLocalSignatureIsRejectedAndNeverCached() {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0, true, false));

        assertThrows(ParsingException.class, () -> YoutubeApiDecoder.decodeBatch(
                "player", Arrays.asList("s1"), null));

        assertEquals(0, YoutubeApiDecoder.getCacheSize());
    }

    @Test
    void unchangedLocalNParameterIsRejectedAndNeverCached() {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0, false, true));

        assertThrows(ParsingException.class, () -> YoutubeApiDecoder.decodeBatch(
                "player", null, Arrays.asList("n1")));

        assertEquals(0, YoutubeApiDecoder.getCacheSize());
    }

    @Test
    void decodeCacheStaysBounded() throws Exception {
        YoutubeApiDecoder.setLocalDecoder(new StubDecoder(0));

        for (int batch = 0; batch < 8; batch++) {
            final List<String> signatures = new ArrayList<>();
            for (int i = 0; i < 200; i++) {
                signatures.add("s-" + batch + '-' + i);
            }
            YoutubeApiDecoder.decodeBatch("player", signatures, null);
        }

        assertTrue(YoutubeApiDecoder.getCacheSize() <= DECODE_CACHE_MAX_ENTRIES,
                "cache size was " + YoutubeApiDecoder.getCacheSize());
    }

    private static class StubDecoder implements YoutubeJavaScriptDecoder {
        private final int droppedSignatures;
        private final boolean unchangedSignatures;
        private final boolean unchangedNParameters;

        private StubDecoder(final int droppedSignatures) {
            this(droppedSignatures, false, false);
        }

        private StubDecoder(final int droppedSignatures,
                            final boolean unchangedSignatures,
                            final boolean unchangedNParameters) {
            this.droppedSignatures = droppedSignatures;
            this.unchangedSignatures = unchangedSignatures;
            this.unchangedNParameters = unchangedNParameters;
        }

        @Nonnull
        @Override
        public PlayerData getPlayerData(@Nonnull final String videoId) {
            return new PlayerData("player", 1);
        }

        @Nonnull
        @Override
        public YoutubeApiDecoder.BatchDecodeResult decodeBatch(
                @Nonnull final String playerId,
                @Nullable final List<String> signatures,
                @Nullable final List<String> throttlingParameters) {
            return new YoutubeApiDecoder.BatchDecodeResult(
                    decodeValues(signatures, droppedSignatures, unchangedSignatures),
                    decodeValues(throttlingParameters, 0, unchangedNParameters));
        }

        @Nonnull
        private static Map<String, String> decodeValues(@Nullable final List<String> values,
                                                        final int dropped,
                                                        final boolean unchanged) {
            final Map<String, String> decoded = new HashMap<>();
            if (values != null) {
                for (int i = 0; i < values.size() - dropped; i++) {
                    final String value = values.get(i);
                    decoded.put(value, unchanged ? value : "decoded-" + value);
                }
            }
            return decoded;
        }
    }
}
