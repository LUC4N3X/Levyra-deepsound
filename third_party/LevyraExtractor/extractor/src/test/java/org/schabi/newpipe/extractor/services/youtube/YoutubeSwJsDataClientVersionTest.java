package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSwJsDataClientVersionTest {

    private static final String CLIENT_VERSION = "2.20260101.00.00";

    private Downloader previousDownloader;
    private Localization previousLocalization;
    private ContentCountry previousCountry;

    private static String swJsDataBody(final String version) {
        return ")]}'\n[[null,null,[[["
                + String.join(",", Collections.nCopies(16, "null"))
                + ",\"" + version + "\"]]]]]";
    }

    @BeforeEach
    void setUp() {
        previousDownloader = NewPipe.getDownloader();
        previousLocalization = NewPipe.getPreferredLocalization();
        previousCountry = NewPipe.getPreferredContentCountry();
        YoutubeParsingHelper.resetClientVersion();
    }

    @AfterEach
    void tearDown() {
        YoutubeParsingHelper.resetClientVersion();
        NewPipe.init(previousDownloader, previousLocalization, previousCountry);
    }

    @Test
    void parsesClientVersionFromValidResponse() throws Exception {
        assertEquals(CLIENT_VERSION, YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(swJsDataBody(CLIENT_VERSION)));
    }

    @Test
    void parsesResponseWithoutAntiXssiPrefix() throws Exception {
        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.extractClientVersionFromSwJsDataResponse(
                swJsDataBody(CLIENT_VERSION).substring(4)));
    }

    @Test
    void rejectsMalformedResponse() {
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(")]}'\n[[[not json"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(""));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse("{\"clientVersion\":\"2.2026.00.00\"}"));
    }

    @Test
    void rejectsChangedOrIncompleteStructure() {
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(")]}'\n[]"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(")]}'\n[[null,null,[[[null]]]]]"));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(
                        ")]}'\n[[null,null,[[[" + String.join(",", Collections.nCopies(16, "null"))
                                + ",{}]]]]]"));
    }

    @Test
    void rejectsImplausibleClientVersion() {
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(swJsDataBody("not-a-version")));
        assertThrows(ParsingException.class, () -> YoutubeParsingHelper
                .extractClientVersionFromSwJsDataResponse(swJsDataBody("2")));

        assertTrue(YoutubeParsingHelper.isValidWebClientVersion(CLIENT_VERSION));
        assertFalse(YoutubeParsingHelper.isValidWebClientVersion(null));
        assertFalse(YoutubeParsingHelper.isValidWebClientVersion(""));
        assertFalse(YoutubeParsingHelper.isValidWebClientVersion("2.2026.00.00"));
    }

    @Test
    void fallsBackToHardcodedVersionWhenNetworkFails() throws Exception {
        final FailingDownloader downloader = new FailingDownloader();
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);

        final String resolved = YoutubeParsingHelper.getClientVersion();

        assertNotNull(resolved);
        assertTrue(YoutubeParsingHelper.isValidWebClientVersion(resolved));
        assertTrue(downloader.requestCount.get() > 0);
    }

    @Test
    void cachesResolvedVersionAcrossCalls() throws Exception {
        final FixtureDownloader downloader = new FixtureDownloader(CLIENT_VERSION);
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);

        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.getClientVersion());
        final int requestsAfterResolution = downloader.requestCount.get();
        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.getClientVersion());

        assertEquals(requestsAfterResolution, downloader.requestCount.get());
    }

    @Test
    void nonBlockingAccessorReturnsFallbackWithoutCachedVersion() {
        NewPipe.init(new FailingDownloader(), Localization.DEFAULT, ContentCountry.DEFAULT);

        assertNull(YoutubeParsingHelper.getCachedClientVersion());
        assertEquals("1.2.3",
                YoutubeParsingHelper.getClientVersionWithoutBlocking("1.2.3"));
    }

    @Test
    void htmlFallbackPublishesAReusableCachedVersion() throws Exception {
        final HtmlFallbackDownloader downloader = new HtmlFallbackDownloader(CLIENT_VERSION);
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);

        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.getClientVersion());
        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.getCachedClientVersion());
        final int requestsAfterResolution = downloader.requestCount.get();
        assertEquals(CLIENT_VERSION,
                YoutubeParsingHelper.getClientVersionWithoutBlocking("1.2.3"));
        assertEquals(requestsAfterResolution, downloader.requestCount.get());
    }

    @Test
    void nonBlockingAccessorReusesCachedVersion() throws Exception {
        final FixtureDownloader downloader = new FixtureDownloader(CLIENT_VERSION);
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);
        assertEquals(CLIENT_VERSION, YoutubeParsingHelper.getClientVersion());

        final int requestsAfterResolution = downloader.requestCount.get();
        assertEquals(CLIENT_VERSION,
                YoutubeParsingHelper.getClientVersionWithoutBlocking("1.2.3"));
        assertEquals(requestsAfterResolution, downloader.requestCount.get());
    }

    private static final class FixtureDownloader extends Downloader {
        private final AtomicInteger requestCount = new AtomicInteger();
        private final String version;

        private FixtureDownloader(final String version) {
            this.version = version;
        }

        @Override
        public Response execute(@Nonnull final Request request) {
            requestCount.incrementAndGet();
            return new Response(200, "OK", Collections.emptyMap(), swJsDataBody(version), null,
                    "https://www.youtube.com/sw.js_data");
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class HtmlFallbackDownloader extends Downloader {
        private final AtomicInteger requestCount = new AtomicInteger();
        private final String version;

        private HtmlFallbackDownloader(final String version) {
            this.version = version;
        }

        @Override
        public Response execute(@Nonnull final Request request) throws IOException {
            requestCount.incrementAndGet();
            if (request.url().contains("sw.js_data")) {
                throw new IOException("sw.js_data unavailable");
            }
            if (request.url().contains("/results?")) {
                final String body = "<html><script>{\"INNERTUBE_CONTEXT_CLIENT_VERSION\":\""
                        + version + "\"}</script></html>";
                return new Response(200, "OK", Collections.emptyMap(), body, null, request.url());
            }
            throw new IOException("Unexpected request: " + request.url());
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class FailingDownloader extends Downloader {
        private final AtomicInteger requestCount = new AtomicInteger();

        @Override
        public Response execute(@Nonnull final Request request)
                throws IOException, ReCaptchaException {
            requestCount.incrementAndGet();
            throw new IOException("Network unavailable");
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
