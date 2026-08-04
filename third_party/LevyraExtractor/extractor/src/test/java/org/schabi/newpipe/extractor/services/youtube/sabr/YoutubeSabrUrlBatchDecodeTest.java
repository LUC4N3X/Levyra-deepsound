package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.services.youtube.YoutubeApiDecoder;
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptDecoder;
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrUrlBatchDecodeTest {
    private final CountingDecoder decoder = new CountingDecoder();

    @BeforeEach
    void setUp() {
        YoutubeApiDecoder.setLocalDecoder(decoder);
        YoutubeJavaScriptPlayerManager.clearAllCaches();
    }

    @AfterEach
    void tearDown() {
        YoutubeApiDecoder.setLocalDecoder(null);
        YoutubeJavaScriptPlayerManager.clearAllCaches();
    }

    @Test
    void playerResponseDecodesServerAndAllAdaptiveUrlsInOneBatch() throws Exception {
        final JsonArray formats = new JsonArray();
        formats.add(cipherFormat(140, "audio/mp4", "audio-n", "audio-s"));
        formats.add(cipherFormat(134, "video/mp4", "video-n", "video-s"));
        formats.add(directFormat(135, "video/mp4", "unused-n"));
        formats.add(pathCipherFormat(136, "video/mp4", "path-n", "path-s"));

        final JsonObject streamingData = new JsonObject();
        streamingData.put("serverAbrStreamingUrl",
                "https://sabr.test/videoplayback?n=server-n");
        streamingData.put("adaptiveFormats", formats);
        final JsonObject playerResponse = new JsonObject();
        playerResponse.put("streamingData", streamingData);

        final YoutubeSabrInfo info = YoutubeSabrProbe.fromPlayerResponse(
                "video", YoutubeSabrClientProfile.MWEB, "cpn", playerResponse);

        assertEquals(1, decoder.batchCalls);
        assertEquals(3, decoder.lastSignatures.size());
        assertEquals(5, decoder.lastNParameters.size());
        assertTrue(info.getServerAbrStreamingUrl().contains("n=decoded-server-n"));
        assertTrue(info.findFormatByItag(140).getInitializationUrl()
                .contains("sig=decoded-audio-s"));
        assertTrue(info.findFormatByItag(140).getInitializationUrl()
                .contains("n=decoded-audio-n"));
        assertTrue(info.findFormatByItag(134).getInitializationUrl()
                .contains("sig=decoded-video-s"));
        assertTrue(info.findFormatByItag(135).getInitializationUrl()
                .contains("n=decoded-unused-n"));
        assertTrue(info.findFormatByItag(136).getInitializationUrl()
                .contains("/n/decoded-path-n"));
        assertTrue(info.findFormatByItag(136).getInitializationUrl()
                .contains("sig=decoded-path-s"));
    }

    @Nonnull
    private static JsonObject cipherFormat(final int itag,
                                           @Nonnull final String mimeType,
                                           @Nonnull final String n,
                                           @Nonnull final String signature) {
        final JsonObject format = baseFormat(itag, mimeType);
        final String url = "https://adaptive.test/" + itag + "?n=" + n;
        format.put("signatureCipher", "url=" + URLEncoder.encode(url, StandardCharsets.UTF_8)
                + "&s=" + signature + "&sp=sig");
        return format;
    }

    @Nonnull
    private static JsonObject directFormat(final int itag,
                                           @Nonnull final String mimeType,
                                           @Nonnull final String n) {
        final JsonObject format = baseFormat(itag, mimeType);
        format.put("url", "https://adaptive.test/" + itag + "?n=" + n);
        return format;
    }

    @Nonnull
    private static JsonObject pathCipherFormat(final int itag,
                                               @Nonnull final String mimeType,
                                               @Nonnull final String n,
                                               @Nonnull final String signature) {
        final JsonObject format = baseFormat(itag, mimeType);
        format.put("cipher", "url=" + URLEncoder.encode(
                "https://adaptive.test/" + itag + "/n/" + n,
                StandardCharsets.UTF_8)
                + "&s=" + signature + "&sp=sig");
        return format;
    }

    @Nonnull
    private static JsonObject baseFormat(final int itag, @Nonnull final String mimeType) {
        final JsonObject range = new JsonObject();
        range.put("start", "0");
        range.put("end", "100");
        final JsonObject format = new JsonObject();
        format.put("itag", itag);
        format.put("mimeType", mimeType);
        format.put("lastModified", "1");
        format.put("approxDurationMs", "1000");
        format.put("initRange", range);
        format.put("indexRange", range);
        return format;
    }

    private static final class CountingDecoder implements YoutubeJavaScriptDecoder {
        private int batchCalls;
        private List<String> lastSignatures;
        private List<String> lastNParameters;

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
            batchCalls++;
            lastSignatures = signatures;
            lastNParameters = throttlingParameters;
            return new YoutubeApiDecoder.BatchDecodeResult(
                    decodeValues(signatures), decodeValues(throttlingParameters));
        }

        @Nonnull
        private static Map<String, String> decodeValues(@Nullable final List<String> values) {
            final Map<String, String> decoded = new HashMap<>();
            if (values != null) {
                for (final String value : values) {
                    decoded.put(value, "decoded-" + value);
                }
            }
            return decoded;
        }
    }
}
