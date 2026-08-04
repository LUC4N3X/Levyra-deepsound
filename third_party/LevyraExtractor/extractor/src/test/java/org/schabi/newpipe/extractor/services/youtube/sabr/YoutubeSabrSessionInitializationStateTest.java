package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.downloader.StreamingResponse;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrSessionInitializationStateTest {
    private static final int AUDIO_ITAG = 140;
    private static final int VIDEO_ITAG = 299;
    private static final byte[] INITIALIZATION_DATA = {1, 2, 3};

    @Test
    void successfulInitializationFetchRestoresExactRequestState() throws Exception {
        final Fixture fixture = createFixture(new FakeDownloader(initializationResponse(AUDIO_ITAG)));
        final YoutubeSabrStreamState state = fixture.session.getStreamState();
        state.setFullyBuffered(fixture.audioFormat, true);
        state.setRequestTrackMode(YoutubeSabrStreamState.TRACK_MODE_AUDIO_ONLY, true, false);
        state.setPreferredTrackTypes(true, false);
        state.setPlayerTimeMs(12_345);
        state.setBufferedRangesOverride(Collections.emptyList());
        state.setWriteFirstRequestPlaybackState(false);
        state.setWriteTopLevelPlayerTimeMs(true);
        state.setWriteLastManualSelectedResolution(true);

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.initialization(fixture.audioFormat), Localization.DEFAULT);

        assertArrayEquals(INITIALIZATION_DATA, segment.getData());
        assertRequestState(state, 0, false, true, true);
    }

    @Test
    void failedInitializationFetchRestoresExactRequestState() throws Exception {
        final Fixture fixture = createFixture(new FakeDownloader(new IOException("test failure")));
        final YoutubeSabrStreamState state = fixture.session.getStreamState();
        state.setFullyBuffered(fixture.audioFormat, true);
        state.setRequestTrackMode(YoutubeSabrStreamState.TRACK_MODE_AUDIO_ONLY, true, false);
        state.setPreferredTrackTypes(true, false);
        state.setPlayerTimeMs(12_345);
        state.setBufferedRangesOverride(null);
        state.setWriteFirstRequestPlaybackState(true);
        state.setWriteTopLevelPlayerTimeMs(false);
        state.setWriteLastManualSelectedResolution(false);

        assertThrows(IOException.class, () -> fixture.session.fetchSegment(
                SabrSegmentRequest.initialization(fixture.videoFormat), Localization.DEFAULT));

        assertRequestState(state, 1, true, false, false);
    }

    private static void assertRequestState(@Nonnull final YoutubeSabrStreamState state,
                                           final int bufferedRangeCount,
                                           final boolean writeFirstRequestPlaybackState,
                                           final boolean writeTopLevelPlayerTimeMs,
                                           final boolean writeLastManualSelectedResolution) {
        assertEquals(YoutubeSabrStreamState.TRACK_MODE_AUDIO_ONLY,
                state.getEnabledTrackTypesBitfield());
        assertTrue(state.shouldSelectAudioFormat());
        assertFalse(state.shouldSelectVideoFormat());
        assertFalse(state.shouldPreferAudioFormat());
        assertTrue(state.shouldPreferVideoFormat());
        assertEquals(12_345, state.getPlayerTimeMs());
        assertEquals(bufferedRangeCount, state.getBufferedRanges().size());
        assertEquals(writeFirstRequestPlaybackState,
                state.shouldWriteFirstRequestPlaybackState());
        assertEquals(writeTopLevelPlayerTimeMs, state.shouldWriteTopLevelPlayerTimeMs());
        assertEquals(writeLastManualSelectedResolution,
                state.shouldWriteLastManualSelectedResolution());
    }

    @Nonnull
    private static Fixture createFixture(@Nonnull final Downloader downloader) throws Exception {
        NewPipe.init(downloader);
        final JsonArray formats = JsonParser.array().from("["
                + "{\"itag\":140,\"lastModified\":\"1\",\"mimeType\":\"audio/mp4\","
                + "\"bitrate\":128000,\"contentLength\":\"1000\","
                + "\"approxDurationMs\":\"60000\"},"
                + "{\"itag\":299,\"lastModified\":\"2\",\"mimeType\":\"video/mp4\","
                + "\"width\":1920,\"height\":1080,\"bitrate\":4000000,"
                + "\"contentLength\":\"2000\",\"approxDurationMs\":\"60000\"}]");
        final List<YoutubeSabrFormat> parsedFormats =
                YoutubeSabrFormat.fromAdaptiveFormats("video", formats);
        final YoutubeSabrFormat audioFormat = parsedFormats.get(0);
        final YoutubeSabrFormat videoFormat = parsedFormats.get(1);
        final YoutubeSabrInfo info = new YoutubeSabrInfo(
                YoutubeSabrClientProfile.MWEB,
                "video",
                "cpn",
                YoutubeSabrClientProfile.MWEB.getClientVersion(),
                "visitor",
                "https://example.com/sabr",
                "AA==",
                parsedFormats);
        return new Fixture(
                new YoutubeSabrSession(info, audioFormat, videoFormat, null, null),
                audioFormat,
                videoFormat);
    }

    @Nonnull
    private static byte[] initializationResponse(final int itag) {
        final SabrProto.Writer header = new SabrProto.Writer();
        header.writeInt32(1, 1);
        header.writeInt32(3, itag);
        header.writeBool(8, true);
        header.writeUInt64(14, INITIALIZATION_DATA.length);

        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeUmpPart(output, SabrResponseDecoder.MEDIA_HEADER, header.toByteArray());
        final byte[] media = new byte[INITIALIZATION_DATA.length + 1];
        media[0] = 1;
        System.arraycopy(INITIALIZATION_DATA, 0, media, 1, INITIALIZATION_DATA.length);
        writeUmpPart(output, SabrResponseDecoder.MEDIA, media);
        writeUmpPart(output, SabrResponseDecoder.MEDIA_END, new byte[]{1});
        return output.toByteArray();
    }

    private static void writeUmpPart(@Nonnull final ByteArrayOutputStream output,
                                     final int type,
                                     @Nonnull final byte[] payload) {
        if (type >= 128 || payload.length >= 128) {
            throw new IllegalArgumentException("Test UMP part exceeds single-byte encoding");
        }
        output.write(type);
        output.write(payload.length);
        output.write(payload, 0, payload.length);
    }

    private static final class Fixture {
        @Nonnull
        private final YoutubeSabrSession session;
        @Nonnull
        private final YoutubeSabrFormat audioFormat;
        @Nonnull
        private final YoutubeSabrFormat videoFormat;

        private Fixture(@Nonnull final YoutubeSabrSession session,
                        @Nonnull final YoutubeSabrFormat audioFormat,
                        @Nonnull final YoutubeSabrFormat videoFormat) {
            this.session = session;
            this.audioFormat = audioFormat;
            this.videoFormat = videoFormat;
        }
    }

    private static final class FakeDownloader extends Downloader {
        @Nullable
        private final byte[] responseBody;
        @Nullable
        private final IOException failure;

        private FakeDownloader(@Nonnull final byte[] responseBody) {
            this.responseBody = Arrays.copyOf(responseBody, responseBody.length);
            this.failure = null;
        }

        private FakeDownloader(@Nonnull final IOException failure) {
            this.responseBody = null;
            this.failure = failure;
        }

        @Override
        public StreamingResponse postStreaming(
                final String url,
                @Nullable final Map<String, List<String>> headers,
                @Nullable final byte[] dataToSend,
                @Nullable final Localization localization) throws IOException {
            if (failure != null) {
                throw failure;
            }
            return new StreamingResponse(
                    200,
                    Collections.singletonMap("Content-Type",
                            Collections.singletonList("application/vnd.yt-ump")),
                    new ByteArrayInputStream(responseBody));
        }

        @Override
        public Response execute(@Nonnull final Request request)
                throws IOException, ReCaptchaException {
            throw new UnsupportedOperationException();
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback)
                throws IOException, ReCaptchaException {
            throw new UnsupportedOperationException();
        }
    }
}
