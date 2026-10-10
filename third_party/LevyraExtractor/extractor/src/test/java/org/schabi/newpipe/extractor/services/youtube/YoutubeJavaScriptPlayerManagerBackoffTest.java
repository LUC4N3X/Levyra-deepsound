package org.schabi.newpipe.extractor.services.youtube;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeJavaScriptPlayerManagerBackoffTest {
    private Downloader previousDownloader;

    @AfterEach
    void tearDown() {
        YoutubeApiDecoder.setLocalDecoder(null);
        YoutubeJavaScriptPlayerManager.clearAllCaches();
        if (previousDownloader != null) {
            NewPipe.init(previousDownloader);
            previousDownloader = null;
        }
    }

    @Test
    void definitiveInvalidMetadataResponseBacksOffRemoteRefresh() {
        final CountingDownloader downloader = new CountingDownloader(false);
        installDownloader(downloader);

        assertThrows(ParsingException.class,
                () -> YoutubeJavaScriptPlayerManager.getSignatureTimestamp("video"));
        assertThrows(ParsingException.class,
                () -> YoutubeJavaScriptPlayerManager.getSignatureTimestamp("video"));

        assertEquals(1, downloader.requests.get());
    }

    @Test
    void transientIoFailureIsNotNegativeCached() {
        final CountingDownloader downloader = new CountingDownloader(true);
        installDownloader(downloader);

        assertThrows(ParsingException.class,
                () -> YoutubeJavaScriptPlayerManager.getSignatureTimestamp("video"));
        assertThrows(ParsingException.class,
                () -> YoutubeJavaScriptPlayerManager.getSignatureTimestamp("video"));

        assertEquals(2, downloader.requests.get());
    }

    private void installDownloader(@Nonnull final Downloader downloader) {
        previousDownloader = NewPipe.getDownloader();
        YoutubeApiDecoder.setLocalDecoder(null);
        YoutubeJavaScriptPlayerManager.clearAllCaches();
        NewPipe.init(downloader);
    }

    private static final class CountingDownloader extends Downloader {
        private final boolean failWithIo;
        private final AtomicInteger requests = new AtomicInteger();

        private CountingDownloader(final boolean failWithIo) {
            this.failWithIo = failWithIo;
        }

        @Override
        public Response execute(@Nonnull final Request request)
                throws IOException, ReCaptchaException {
            requests.incrementAndGet();
            if (failWithIo) {
                throw new IOException("transient");
            }
            return new Response(200, "OK", Collections.emptyMap(), "{}", new byte[0],
                    request.url());
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) throws IOException {
            throw new IOException("async not used");
        }
    }
}
