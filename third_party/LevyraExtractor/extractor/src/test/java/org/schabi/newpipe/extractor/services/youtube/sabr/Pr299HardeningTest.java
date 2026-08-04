package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pr299HardeningTest {
    @Test
    void rejectsOversizedUmpPartBeforeReadingPayload() {
        final int oversized = UmpReader.MAX_UMP_PART_BYTES + 1;
        final byte[] envelope = new byte[]{1, (byte) 240,
                (byte) oversized, (byte) (oversized >>> 8),
                (byte) (oversized >>> 16), (byte) (oversized >>> 24)};

        assertThrows(SabrProtocolException.class,
                () -> UmpReader.readPayloadsUntil(new ByteArrayInputStream(envelope),
                        (type, size, payload) -> true));
    }

    @Test
    void protectionStatusOnlyBlocksNoMediaAndFutureStatusesRemainProtected() {
        final SabrDecodedResponse mediaBearing = new SabrDecodedResponse();
        mediaBearing.setStreamProtectionStatus(
                SabrStreamProtectionStatus.ATTESTATION_REQUIRED);
        mediaBearing.addMediaHeader(SabrMediaHeader.normalized(1, "video", 299, 1,
                null, 0, 0, false, 1, 1_000_000, 0, 1_000, 1,
                0, 1_000, 1_000, 1));
        assertFalse(mediaBearing.isAttestationRequired());
        assertFalse(mediaBearing.isAttestationPending());

        final SabrDecodedResponse futureNoMedia = new SabrDecodedResponse();
        futureNoMedia.setStreamProtectionStatus(
                SabrStreamProtectionStatus.ATTESTATION_REQUIRED + 1);
        assertTrue(futureNoMedia.isAttestationRequired());
        assertTrue(futureNoMedia.isAttestationPending());
    }

    @Test
    void normalizedHeaderDerivesMillisecondsFromPositiveTimeRange() {
        final SabrMediaHeader header = SabrMediaHeader.normalized(1, "video", 299, 1,
                null, 0, 0, false, 1, 1_000_000, -1, -1, 10,
                2_000, 1_000, 1_000, 1);
        assertEquals(2_000, header.getStartMs());
        assertEquals(1_000, header.getDurationMs());
        assertThrows(IllegalArgumentException.class,
                () -> SabrMediaHeader.normalized(1, "video", 299, 1,
                        null, 0, 0, false, 1, 1_000_000, -1, -1, 10,
                        2_000, 1_000, 0, 1));
    }

    @Test
    void lowestVideoIgnoresUnknownHeightButRetainsFallback() throws Exception {
        final List<YoutubeSabrFormat> formats = YoutubeSabrFormat.fromAdaptiveFormats("video",
                JsonParser.array().from("["
                        + "{\"itag\":1,\"mimeType\":\"video/mp4\",\"bitrate\":1},"
                        + "{\"itag\":2,\"mimeType\":\"video/mp4\",\"height\":720,\"bitrate\":2},"
                        + "{\"itag\":3,\"mimeType\":\"video/mp4\",\"height\":360,\"bitrate\":3}]"));
        final YoutubeSabrInfo info = new YoutubeSabrInfo(YoutubeSabrClientProfile.ANDROID,
                "video", "cpn", "1", "visitor", "https://example.com/sabr", "config",
                formats, true);
        assertEquals(360, info.findLowestVideoFormat().getHeight());
    }

    @Test
    void timeoutAwareStreamingDefaultCannotSilentlyIgnoreDeadline() {
        final Downloader downloader = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", null, "", new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(final Request request,
                                                final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        };
        assertThrows(IOException.class, () -> downloader.getStreaming(
                "https://example.com", null, Localization.DEFAULT, 100));
    }

    @Test
    void mediaHeaderCollectionIsBounded() {
        final SabrResponseStatePatch.Builder builder = SabrResponseStatePatch.builder();
        for (int i = 0; i < 513; i++) {
            builder.addMediaHeader(SabrMediaHeader.normalized(i & 0xff, "video", 299, 1,
                    null, 0, 0, false, i, 1_000_000, 0, 1_000, 1,
                    -1, -1, -1, 1));
        }
        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
