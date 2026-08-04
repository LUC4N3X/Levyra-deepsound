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
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrSessionStreamProtectionTest {
    private static final Localization LOCALIZATION = new Localization("en", "US");

    @Test
    void pendingAttestationDefersBackoffWithoutRefreshingToken() throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING, 20, 2_000));
        final AtomicInteger tokenRequests = new AtomicInteger();
        final Fixture fixture = createFixture(downloader, (info, state) -> {
            tokenRequests.incrementAndGet();
            return new byte[]{9, 9, 9};
        });
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});

        final YoutubeSabrSession.DemandResponseResult result =
                fixture.session.pumpOnceStreamingForDemand(LOCALIZATION,
                        SabrSegmentRequest.media(fixture.videoFormat, 1));

        assertTrue(result.wasRequestPerformed());
        assertEquals(0, result.getSegmentCount());
        assertEquals(0, tokenRequests.get());
        assertEquals(1, downloader.requestCount.get());
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                fixture.session.getMaxStreamProtectionStatus());
        assertTrue(fixture.session.getDemandBackoffRemainingMs() > 0);
        assertTrue(fixture.session.getDiagnosticTrace().contains("protection=2/20"));
    }

    @Test
    void rejectedAttestationTerminatesBeforeBackoff() throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_REQUIRED, 20, 59_000));
        final Fixture fixture = createFixture(downloader, null);

        final SabrProtocolException failure = assertThrows(SabrProtocolException.class,
                () -> fixture.session.pumpOnceStreamingForDemand(LOCALIZATION,
                        SabrSegmentRequest.media(fixture.videoFormat, 1)));

        assertTrue(failure.getMessage().contains("attestation required"));
        assertEquals(1, downloader.requestCount.get());
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                fixture.session.getMaxStreamProtectionStatus());
        assertEquals(0, fixture.session.getDemandBackoffRemainingMs());
        assertTrue(fixture.session.getDiagnosticTrace().contains("protection=3/20"));
    }

    @Test
    void repeatedPendingAttestationFailsAtIndependentDeadLoopLimit() throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING, 20, 0));
        final Fixture fixture = createFixture(downloader, null);
        final SabrSegmentRequest request = SabrSegmentRequest.media(fixture.videoFormat, 1);

        fixture.session.pumpOnceStreamingForDemand(LOCALIZATION, request);
        fixture.session.pumpOnceStreamingForDemand(LOCALIZATION, request);
        final SabrProtocolException failure = assertThrows(SabrProtocolException.class,
                () -> fixture.session.pumpOnceStreamingForDemand(LOCALIZATION, request));

        assertTrue(failure.getMessage().contains("pending without media for 3"));
        assertEquals(3, downloader.requestCount.get());
        assertTrue(fixture.session.getDiagnosticTrace()
                .contains("attestation_pending_no_media count=3"));
    }

    @Test
    void pendingAttestationRotatesVisitorPlayerAndVideoTokenEpoch() throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING, 20, 2_000),
                protectionResponse(1, 0, 0));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger tokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                tokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final YoutubeSabrInfo freshInfo = sabrInfo(
                "fresh-visitor", "https://example.com/fresh-sabr", true);
        final Fixture fixture = createFixture(downloader, tokenProvider,
                (videoId, profile, localization, contentCountry, rotationProvider) -> {
                    assertSame(tokenProvider, rotationProvider);
                    return freshInfo;
                });
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});
        final SabrSegmentRequest request = SabrSegmentRequest.media(fixture.videoFormat, 1);

        fixture.session.pumpOnceStreamingForDemand(LOCALIZATION, request);
        fixture.session.pumpOnceStreamingForDemand(LOCALIZATION, request);

        assertEquals(1, invalidations.get());
        assertEquals(1, tokenRequests.get());
        assertArrayEquals(new byte[]{9, 9, 9}, fixture.session.getStreamState().getPoToken());
        assertEquals(List.of(
                        "https://example.com/sabr?alr=yes&cpn=cpn&rn=0",
                        "https://example.com/fresh-sabr?alr=yes&cpn=cpn&rn=0"),
                downloader.requestUrls);
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                fixture.session.getMaxStreamProtectionStatus());
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation attempt=1 visitorChanged=true bytes=3"));
        assertTrue(fixture.session.getDiagnosticTrace().contains("protection=1/0"));
    }

    @Test
    void fetchSegmentRotatesMediaBearingPendingAttestationBeforeReturningTarget()
            throws Exception {
        final byte[] targetData = new byte[]{4, 5, 6, 7};
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING,
                        20, 2_000, 299, 1, targetData),
                protectionResponse(1, 0, 0));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger tokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                tokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final YoutubeSabrInfo freshInfo = sabrInfo(
                "fresh-visitor", "https://example.com/fresh-sabr", true);
        final Fixture fixture = createFixture(downloader, tokenProvider,
                (videoId, profile, localization, contentCountry,
                 rotationProvider) -> freshInfo);
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});
        fixture.session.getStreamState().ingestInitializationData(
                fixture.videoFormat, new byte[]{0});

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION);

        assertArrayEquals(targetData, segment.getData());
        assertEquals(1, invalidations.get());
        assertEquals(1, tokenRequests.get());
        assertEquals(List.of(
                        "https://example.com/sabr?alr=yes&cpn=cpn&rn=0",
                        "https://example.com/fresh-sabr?alr=yes&cpn=cpn&rn=0"),
                downloader.requestUrls);
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation attempt=1 visitorChanged=true bytes=3"));
        assertTrue(fixture.session.getDiagnosticTrace().contains("protection=1/0"));
        assertEquals(1, fixture.session.getStreamState().getMaxSegment(fixture.videoFormat));
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "ranges=itag=299:seq=1-1:time=0+1000:timescale=1000"));
    }

    @Test
    void pendingAttestationRejectsPlayerReloadWithoutSessionPoToken() throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING, 20, 2_000));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger videoTokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                videoTokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final YoutubeSabrInfo unboundPlayerInfo = sabrInfo(
                "fresh-visitor", "https://example.com/fresh-sabr", false);
        final Fixture fixture = createFixture(downloader, tokenProvider,
                (videoId, profile, localization, contentCountry,
                 rotationProvider) -> unboundPlayerInfo);
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});

        final SabrProtocolException failure = assertThrows(SabrProtocolException.class,
                () -> fixture.session.pumpOnceStreamingForDemand(LOCALIZATION,
                        SabrSegmentRequest.media(fixture.videoFormat, 1)));

        assertTrue(failure.getMessage().contains("player request had no session PO token"));
        assertEquals(1, invalidations.get());
        assertEquals(0, videoTokenRequests.get());
        assertEquals(1, downloader.requestCount.get());
    }

    @Test
    void pendingAttestationRejectsUnchangedVisitorWithoutDiscardingCurrentToken()
            throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionResponse(SabrStreamProtectionStatus.ATTESTATION_PENDING, 20, 2_000));
        final AtomicInteger invalidations = new AtomicInteger();
        final AtomicInteger videoTokenRequests = new AtomicInteger();
        final SabrPoTokenProvider tokenProvider = new SabrPoTokenProvider() {
            @Nullable
            @Override
            public byte[] getPoToken(@Nonnull final YoutubeSabrInfo info,
                                     @Nonnull final YoutubeSabrStreamState streamState) {
                videoTokenRequests.incrementAndGet();
                return new byte[]{9, 9, 9};
            }

            @Override
            public boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info) {
                invalidations.incrementAndGet();
                return true;
            }
        };
        final YoutubeSabrInfo unchangedVisitorInfo = sabrInfo(
                "visitor", "https://example.com/fresh-sabr", true);
        final Fixture fixture = createFixture(downloader, tokenProvider,
                (videoId, profile, localization, contentCountry,
                 rotationProvider) -> unchangedVisitorInfo);
        fixture.session.getStreamState().setPoToken(new byte[]{1, 2, 3});

        fixture.session.pumpOnceStreamingForDemand(LOCALIZATION,
                SabrSegmentRequest.media(fixture.videoFormat, 1));

        assertEquals(1, invalidations.get());
        assertEquals(0, videoTokenRequests.get());
        assertArrayEquals(new byte[]{1, 2, 3}, fixture.session.getStreamState().getPoToken());
        assertTrue(fixture.session.getDemandBackoffRemainingMs() > 0);
        assertTrue(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation_rejected attempt=1 reason=visitor_unchanged"));
        assertFalse(fixture.session.getDiagnosticTrace().contains(
                "attestation_identity_rotation attempt=1"));
    }

    @Test
    void fetchSegmentRejectsMediaBearingRequiredAttestationBeforeReturningTarget()
            throws Exception {
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                        20, 59_000, 299, 1, new byte[]{4, 5, 6, 7}));
        final Fixture fixture = createFixture(downloader, null);

        final SabrProtocolException failure = assertThrows(SabrProtocolException.class,
                () -> fixture.session.fetchSegment(
                        SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION));

        assertTrue(failure.getMessage().contains("attestation required"));
        assertEquals(1, downloader.requestCount.get());
        assertEquals(SabrStreamProtectionStatus.ATTESTATION_REQUIRED,
                fixture.session.getMaxStreamProtectionStatus());
    }

    @Test
    void fetchSegmentDefersMediaBearingBackoffUntilNextNetworkRequest() throws Exception {
        final byte[] targetData = new byte[]{4, 5, 6, 7};
        final FakeDownloader downloader = new FakeDownloader(
                protectionMediaResponse(1, 0, 2_000, 299, 1, targetData));
        final Fixture fixture = createFixture(downloader, null);

        final SabrMediaSegment segment = fixture.session.fetchSegment(
                SabrSegmentRequest.media(fixture.videoFormat, 1), LOCALIZATION);

        assertArrayEquals(targetData, segment.getData());
        assertEquals(1, downloader.requestCount.get());
        assertTrue(fixture.session.getDemandBackoffRemainingMs() > 0);
    }

    @Nonnull
    private static Fixture createFixture(@Nonnull final Downloader downloader,
                                         @Nullable final SabrPoTokenProvider tokenProvider)
            throws Exception {
        return createFixture(downloader, tokenProvider, null);
    }

    @Nonnull
    private static Fixture createFixture(
            @Nonnull final Downloader downloader,
            @Nullable final SabrPoTokenProvider tokenProvider,
            @Nullable final YoutubeSabrSession.PlayerInfoReloader playerInfoReloader)
            throws Exception {
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
        final YoutubeSabrInfo info = sabrInfo("visitor", "https://example.com/sabr");
        final YoutubeSabrSession session = playerInfoReloader == null
                ? new YoutubeSabrSession(
                        info, parsedFormats.get(0), parsedFormats.get(1), tokenProvider, null)
                : new YoutubeSabrSession(info, parsedFormats.get(0), parsedFormats.get(1),
                        tokenProvider, null, playerInfoReloader);
        return new Fixture(session, parsedFormats.get(1));
    }

    @Nonnull
    private static YoutubeSabrInfo sabrInfo(@Nonnull final String visitorData,
                                            @Nonnull final String streamingUrl) throws Exception {
        return sabrInfo(visitorData, streamingUrl, false);
    }

    @Nonnull
    private static YoutubeSabrInfo sabrInfo(@Nonnull final String visitorData,
                                            @Nonnull final String streamingUrl,
                                            final boolean playerPoTokenAttached) throws Exception {
        final JsonArray formats = JsonParser.array().from("["
                + "{\"itag\":140,\"lastModified\":\"1\",\"mimeType\":\"audio/mp4\","
                + "\"bitrate\":128000,\"contentLength\":\"1000\","
                + "\"approxDurationMs\":\"60000\"},"
                + "{\"itag\":299,\"lastModified\":\"2\",\"mimeType\":\"video/mp4\","
                + "\"width\":1920,\"height\":1080,\"bitrate\":4000000,"
                + "\"contentLength\":\"2000\",\"approxDurationMs\":\"60000\"}]");
        return new YoutubeSabrInfo(
                YoutubeSabrClientProfile.MWEB,
                "video",
                "cpn",
                YoutubeSabrClientProfile.MWEB.getClientVersion(),
                visitorData,
                streamingUrl,
                "AA==",
                YoutubeSabrFormat.fromAdaptiveFormats("video", formats),
                playerPoTokenAttached);
    }

    @Nonnull
    private static byte[] protectionResponse(final int status,
                                             final int maxRetries,
                                             final int backoffMs) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeProtectionParts(output, status, maxRetries, backoffMs);
        return output.toByteArray();
    }

    @Nonnull
    private static byte[] protectionMediaResponse(final int status,
                                                  final int maxRetries,
                                                  final int backoffMs,
                                                  final int itag,
                                                  final int sequenceNumber,
                                                  @Nonnull final byte[] mediaData) {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        writeProtectionParts(output, status, maxRetries, backoffMs);

        final SabrProto.Writer header = new SabrProto.Writer();
        header.writeInt32(1, 1);
        header.writeInt32(3, itag);
        header.writeUInt64(6, 0);
        header.writeInt32(9, sequenceNumber);
        header.writeUInt64(10, 4_000_000);
        header.writeUInt64(11, 0);
        header.writeUInt64(12, 1_000);
        header.writeUInt64(14, mediaData.length);
        writeUmpPart(output, SabrResponseDecoder.MEDIA_HEADER, header.toByteArray());

        final byte[] mediaPart = new byte[mediaData.length + 1];
        mediaPart[0] = 1;
        System.arraycopy(mediaData, 0, mediaPart, 1, mediaData.length);
        writeUmpPart(output, SabrResponseDecoder.MEDIA, mediaPart);
        writeUmpPart(output, SabrResponseDecoder.MEDIA_END, new byte[]{1});
        return output.toByteArray();
    }

    private static void writeProtectionParts(@Nonnull final ByteArrayOutputStream output,
                                             final int status,
                                             final int maxRetries,
                                             final int backoffMs) {
        final SabrProto.Writer protection = new SabrProto.Writer();
        protection.writeInt32(1, status);
        protection.writeInt32(2, maxRetries);
        final SabrProto.Writer policy = new SabrProto.Writer();
        policy.writeInt32(4, backoffMs);

        writeUmpPart(output, SabrResponseDecoder.STREAM_PROTECTION_STATUS,
                protection.toByteArray());
        writeUmpPart(output, SabrResponseDecoder.NEXT_REQUEST_POLICY, policy.toByteArray());
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
        @Nonnull private final YoutubeSabrSession session;
        @Nonnull private final YoutubeSabrFormat videoFormat;

        private Fixture(@Nonnull final YoutubeSabrSession session,
                        @Nonnull final YoutubeSabrFormat videoFormat) {
            this.session = session;
            this.videoFormat = videoFormat;
        }
    }

    private static final class FakeDownloader extends Downloader {
        @Nonnull private final byte[][] responseBodies;
        @Nonnull private final AtomicInteger requestCount = new AtomicInteger();
        @Nonnull private final List<String> requestUrls = new java.util.ArrayList<>();

        private FakeDownloader(@Nonnull final byte[]... responseBodies) {
            this.responseBodies = responseBodies.clone();
        }

        @Override
        public StreamingResponse postStreaming(
                final String url,
                @Nullable final Map<String, List<String>> headers,
                @Nullable final byte[] dataToSend,
                @Nullable final Localization localization) {
            final int requestIndex = requestCount.getAndIncrement();
            requestUrls.add(url);
            return new StreamingResponse(
                    200,
                    Collections.singletonMap("Content-Type",
                            Collections.singletonList("application/vnd.yt-ump")),
                    new ByteArrayInputStream(responseBodies[Math.min(
                            requestIndex, responseBodies.length - 1)]));
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
