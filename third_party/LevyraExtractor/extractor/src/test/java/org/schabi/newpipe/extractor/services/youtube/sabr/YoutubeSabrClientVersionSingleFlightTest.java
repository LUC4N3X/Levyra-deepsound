package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper;

import java.io.IOException;
import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.annotation.Nonnull;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSabrClientVersionSingleFlightTest {
    @Test
    @Timeout(5)
    void concurrentCallersShareClientVersionNetworkRequest() throws Exception {
        final Downloader previousDownloader = NewPipe.getDownloader();
        final Localization previousLocalization = NewPipe.getPreferredLocalization();
        final ContentCountry previousCountry = NewPipe.getPreferredContentCountry();
        final BlockingDownloader downloader = new BlockingDownloader();
        final ExecutorService executor = Executors.newFixedThreadPool(2);
        YoutubeParsingHelper.resetClientVersion();
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);
        try {
            final Future<String> first = executor.submit(YoutubeParsingHelper::getClientVersion);
            assertTrue(downloader.requestStarted.await(2, TimeUnit.SECONDS));
            final CountDownLatch secondStarted = new CountDownLatch(1);
            final Future<String> second = executor.submit(() -> {
                secondStarted.countDown();
                return YoutubeParsingHelper.getClientVersion();
            });
            assertTrue(secondStarted.await(2, TimeUnit.SECONDS));

            assertEquals(1, downloader.requestCount.get());
            assertFalse(second.isDone());
            downloader.requestRelease.countDown();

            assertEquals(BlockingDownloader.CLIENT_VERSION,
                    first.get(2, TimeUnit.SECONDS));
            assertEquals(BlockingDownloader.CLIENT_VERSION,
                    second.get(2, TimeUnit.SECONDS));
            assertEquals(1, downloader.requestCount.get());
        } finally {
            downloader.requestRelease.countDown();
            executor.shutdownNow();
            YoutubeParsingHelper.resetClientVersion();
            NewPipe.init(previousDownloader, previousLocalization, previousCountry);
        }
    }

    private static final class BlockingDownloader extends Downloader {
        private static final String CLIENT_VERSION = "2.20260101.00.00";
        private final CountDownLatch requestStarted = new CountDownLatch(1);
        private final CountDownLatch requestRelease = new CountDownLatch(1);
        private final AtomicInteger requestCount = new AtomicInteger();

        @Override
        public Response execute(@Nonnull final Request request)
                throws IOException, ReCaptchaException {
            requestCount.incrementAndGet();
            requestStarted.countDown();
            try {
                requestRelease.await();
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("Interrupted client-version fixture", e);
            }
            return new Response(200, "OK", Collections.emptyMap(),
                    "\"INNERTUBE_CONTEXT_CLIENT_VERSION\":\"" + CLIENT_VERSION + "\"",
                    null, "https://www.youtube.com/sw.js");
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
