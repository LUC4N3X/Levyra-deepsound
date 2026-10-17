package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonWriter;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.downloader.StreamingResponse;
import org.schabi.newpipe.extractor.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InterruptedIOException;
import java.net.ConnectException;
import java.net.NoRouteToHostException;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrNodeFallbackTest {
    private static final Localization LOCALIZATION = new Localization("en", "US");
    private static final String QUERY =
            "?expire=1&ei=x&mn=sn-aaa111%2Csn-bbb222%2Csn-ccc333&mvi=3&sig=AB%3D%3D&n=xyz";
    private static final String NODE_URL =
            "https://rr3---sn-aaa111.googlevideo.com/videoplayback" + QUERY;

    private Downloader previousDownloader;
    private YoutubeSabrFormat videoFormat;

    @AfterEach
    void restoreDownloader() {
        if (previousDownloader != null) {
            NewPipe.init(previousDownloader);
            previousDownloader = null;
        }
    }

    @Test
    void alternativesKeepSignedQueryAndOnlySwapAdvertisedNodes() {
        assertEquals(List.of(
                        "https://rr3---sn-bbb222.googlevideo.com/videoplayback" + QUERY,
                        "https://rr3---sn-ccc333.googlevideo.com/videoplayback" + QUERY),
                SabrNodeFallback.alternativeUrls(NODE_URL));
    }

    @Test
    void alternativesRejectUntrustedHostsAndMalformedNodes() {
        assertTrue(SabrNodeFallback.alternativeUrls(
                "https://example.com/videoplayback?mn=sn-bbb222").isEmpty());
        assertTrue(SabrNodeFallback.alternativeUrls(
                "http://rr1---sn-aaa111.googlevideo.com/videoplayback?mn=sn-bbb222").isEmpty());
        assertTrue(SabrNodeFallback.alternativeUrls(
                "https://rr1---sn-aaa111.googlevideo.com:8443/videoplayback?mn=sn-bbb222")
                .isEmpty());
        assertTrue(SabrNodeFallback.alternativeUrls(
                "https://rr1---sn-aaa111.googlevideo.com/videoplayback?expire=1").isEmpty());
        assertTrue(SabrNodeFallback.alternativeUrls(
                "https://rr1---sn-aaa111.googlevideo.com/videoplayback"
                        + "?mn=evil.com%2Fx%2CSN-UPPER%2C..%2C").isEmpty());
        assertEquals(List.of("https://rr1---sn-bbb222.googlevideo.com/videoplayback"
                        + "?mn=sn-bbb222&mvi=bad"),
                SabrNodeFallback.alternativeUrls("https://rr1---sn-aaa111.googlevideo.com"
                        + "/videoplayback?mn=sn-bbb222&mvi=bad"));
    }

    @Test
    void alternativesAreCapped() {
        final List<String> alternatives = SabrNodeFallback.alternativeUrls(
                "https://rr1---sn-a.googlevideo.com/videoplayback"
                        + "?mn=sn-b,sn-c,sn-d,sn-e,sn-f,sn-b");
        assertEquals(SabrNodeFallback.MAX_ALTERNATIVE_NODES, alternatives.size());
    }

    @Test
    void onlyTransportReachabilityFailuresAreRotated() {
        assertTrue(SabrNodeFallback.isNodeUnreachable(new UnknownHostException("dns")));
        assertTrue(SabrNodeFallback.isNodeUnreachable(new ConnectException("refused")));
        assertTrue(SabrNodeFallback.isNodeUnreachable(new NoRouteToHostException("route")));
        assertFalse(SabrNodeFallback.isNodeUnreachable(new SocketTimeoutException("slow")));
        assertFalse(SabrNodeFallback.isNodeUnreachable(new InterruptedIOException("cancel")));
        assertFalse(SabrNodeFallback.isNodeUnreachable(new IOException("Canceled")));
    }

    @Test
    void dnsFailureRetriesSameRequestOnAdvertisedNodeAndKeepsIt() throws Exception {
        final NodeDownloader downloader = new NodeDownloader(
                Set.of("rr3---sn-aaa111.googlevideo.com"), new UnknownHostException("EAI_NODATA"));
        final YoutubeSabrSession session = session(downloader, NODE_URL);
        final SabrSegmentRequest request = SabrSegmentRequest.media(videoFormat, 1);

        session.pumpOnceStreamingForDemand(LOCALIZATION, request);
        session.pumpOnceStreamingForDemand(LOCALIZATION, request);

        assertEquals(List.of("rr3---sn-aaa111.googlevideo.com", "rr3---sn-bbb222.googlevideo.com",
                "rr3---sn-bbb222.googlevideo.com"), downloader.hosts);
        assertTrue(downloader.urls.get(0).contains("rn=0"));
        assertTrue(downloader.urls.get(1).contains("rn=0"));
        assertTrue(downloader.urls.get(2).contains("rn=1"));
        assertTrue(downloader.urls.get(1).contains("&sig=AB%3D%3D&n=xyz"));
        assertTrue(session.getDiagnosticTrace().contains(
                "node_fallback n=0 host=rr3---sn-bbb222.googlevideo.com"));
    }

    @Test
    void exhaustedNodesFailBoundedAndRestoreServerUrl() throws Exception {
        final NodeDownloader downloader = new NodeDownloader(Set.of(
                "rr3---sn-aaa111.googlevideo.com", "rr3---sn-bbb222.googlevideo.com",
                "rr3---sn-ccc333.googlevideo.com"), new ConnectException("refused"));
        final YoutubeSabrSession session = session(downloader, NODE_URL);
        final SabrSegmentRequest request = SabrSegmentRequest.media(videoFormat, 1);

        assertThrows(ConnectException.class,
                () -> session.pumpOnceStreamingForDemand(LOCALIZATION, request));
        assertEquals(3, downloader.hosts.size());
        assertThrows(ConnectException.class,
                () -> session.pumpOnceStreamingForDemand(LOCALIZATION, request));
        assertEquals("rr3---sn-aaa111.googlevideo.com", downloader.hosts.get(3));
        assertEquals(6, downloader.hosts.size());
    }

    @Test
    void nonReachabilityFailureIsNotRetried() throws Exception {
        final IOException timeout = new SocketTimeoutException("read timed out");
        final NodeDownloader downloader = new NodeDownloader(
                Set.of("rr3---sn-aaa111.googlevideo.com"), timeout);
        final YoutubeSabrSession session = session(downloader, NODE_URL);

        final IOException failure = assertThrows(IOException.class,
                () -> session.pumpOnceStreamingForDemand(LOCALIZATION,
                        SabrSegmentRequest.media(videoFormat, 1)));

        assertSame(timeout, failure);
        assertEquals(1, downloader.hosts.size());
    }

    @Test
    void urlWithoutAdvertisedNodesFailsWithoutExtraRequests() throws Exception {
        final NodeDownloader downloader = new NodeDownloader(
                Set.of("rr3---sn-aaa111.googlevideo.com"), new UnknownHostException("dns"));
        final YoutubeSabrSession session = session(downloader,
                "https://rr3---sn-aaa111.googlevideo.com/videoplayback?expire=1");

        assertThrows(UnknownHostException.class,
                () -> session.pumpOnceStreamingForDemand(LOCALIZATION,
                        SabrSegmentRequest.media(videoFormat, 1)));
        assertEquals(1, downloader.hosts.size());
    }

    @Test
    void reloadTokenIsSentInsidePlaybackContextOnlyWhenPresent() throws Exception {
        assertEquals("tok", playbackContext("tok").getObject("reloadPlaybackContext")
                .getObject("reloadPlaybackParams").getString("token"));
        assertFalse(playbackContext(null).has("reloadPlaybackContext"));
        assertFalse(playbackContext("").has("reloadPlaybackContext"));
    }

    @Nonnull
    private static JsonObject playbackContext(@Nullable final String token) throws Exception {
        final JsonBuilder<JsonObject> builder = JsonObject.builder().object("playbackContext");
        YoutubeSabrProbe.appendReloadPlaybackContext(builder, token);
        return JsonParser.object().from(JsonWriter.string(builder.end().done()))
                .getObject("playbackContext");
    }

    @Nonnull
    private YoutubeSabrSession session(@Nonnull final Downloader downloader,
                                       @Nonnull final String streamingUrl) throws Exception {
        if (previousDownloader == null) {
            previousDownloader = NewPipe.getDownloader();
        }
        NewPipe.init(downloader);
        final JsonArray formats = JsonParser.array().from("["
                + "{\"itag\":140,\"lastModified\":\"1\",\"mimeType\":\"audio/mp4\","
                + "\"bitrate\":128000,\"contentLength\":\"1000\","
                + "\"approxDurationMs\":\"60000\"},"
                + "{\"itag\":299,\"lastModified\":\"2\",\"mimeType\":\"video/mp4\","
                + "\"width\":1920,\"height\":1080,\"bitrate\":4000000,"
                + "\"contentLength\":\"2000\",\"approxDurationMs\":\"60000\"}]");
        final List<YoutubeSabrFormat> parsed = YoutubeSabrFormat.fromAdaptiveFormats("video",
                formats);
        final YoutubeSabrInfo info = new YoutubeSabrInfo(YoutubeSabrClientProfile.MWEB, "video",
                "cpn", YoutubeSabrClientProfile.MWEB.getClientVersion(), "visitor",
                streamingUrl, "AA==", parsed, false);
        videoFormat = parsed.get(1);
        return new YoutubeSabrSession(info, parsed.get(0), parsed.get(1), null, null);
    }

    @Nonnull
    private static byte[] okResponse() {
        final ByteArrayOutputStream output = new ByteArrayOutputStream();
        final SabrProto.Writer protection = new SabrProto.Writer();
        protection.writeInt32(1, 1);
        writeUmpPart(output, SabrResponseDecoder.STREAM_PROTECTION_STATUS,
                protection.toByteArray());
        writeUmpPart(output, SabrResponseDecoder.NEXT_REQUEST_POLICY,
                new SabrProto.Writer().toByteArray());
        return output.toByteArray();
    }

    private static void writeUmpPart(@Nonnull final ByteArrayOutputStream output,
                                     final int type,
                                     @Nonnull final byte[] payload) {
        output.write(type);
        output.write(payload.length);
        output.write(payload, 0, payload.length);
    }

    private static final class NodeDownloader extends Downloader {
        @Nonnull private final Set<String> failingHosts;
        @Nonnull private final IOException failure;
        @Nonnull private final List<String> hosts = new ArrayList<>();
        @Nonnull private final List<String> urls = new ArrayList<>();

        private NodeDownloader(@Nonnull final Set<String> failingHosts,
                               @Nonnull final IOException failure) {
            this.failingHosts = failingHosts;
            this.failure = failure;
        }

        @Override
        public StreamingResponse postStreaming(
                final String url,
                @Nullable final Map<String, List<String>> headers,
                @Nullable final byte[] dataToSend,
                @Nullable final Localization localization) throws IOException {
            final String host = URI.create(url).getHost();
            hosts.add(host);
            urls.add(url);
            if (failingHosts.contains(host)) {
                throw failure;
            }
            return new StreamingResponse(200,
                    Collections.singletonMap("Content-Type",
                            Collections.singletonList("application/vnd.yt-ump")),
                    new ByteArrayInputStream(okResponse()));
        }

        @Override
        public Response execute(@Nonnull final Request request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
