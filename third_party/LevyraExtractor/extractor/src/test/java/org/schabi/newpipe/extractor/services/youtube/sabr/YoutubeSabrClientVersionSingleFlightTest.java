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
import java.util.concurrent.atomic.AtomicReference;

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
            final AtomicReference<Thread> secondThread = new AtomicReference<>();
            final Future<String> second = executor.submit(() -> {
                secondThread.set(Thread.currentThread());
                return YoutubeParsingHelper.getClientVersion();
            });
            assertTrue(waitUntilBlocked(secondThread, 2_000));

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

    private static boolean waitUntilBlocked(
            @Nonnull final AtomicReference<Thread> threadReference,
            final long timeoutMs) throws InterruptedException {
        final long deadline = System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(timeoutMs);
        while (System.nanoTime() < deadline) {
            final Thread thread = threadReference.get();
            if (thread != null) {
                final Thread.State state = thread.getState();
                if (state == Thread.State.BLOCKED || state == Thread.State.WAITING
                        || state == Thread.State.TIMED_WAITING) {
                    return true;
                }
            }
            Thread.sleep(10);
        }
        return false;
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
            final String responseBody = ")]}'\n[[null,null,[[["
            + String.join(",", Collections.nCopies(16, "null"))
            + ",\"" + CLIENT_VERSION + "\"]]]]]";
    return new Response(200, "OK", Collections.emptyMap(), responseBody,
            null, "https://www.youtube.com/sw.js_data");
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
