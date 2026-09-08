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

    private static final class StubDecoder implements YoutubeJavaScriptDecoder {
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
