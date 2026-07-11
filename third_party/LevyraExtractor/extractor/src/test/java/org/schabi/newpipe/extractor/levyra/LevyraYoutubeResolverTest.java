package org.schabi.newpipe.extractor.levyra;

import org.junit.Test;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class LevyraYoutubeResolverTest {
    @Test
    public void skipsSabrWhenStreamingIsRequiredButDownloaderCannotStream() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final LevyraYoutubeResolver resolver = new LevyraYoutubeResolver(
                new NonStreamingDownloader(),
                request -> {
                    calls.incrementAndGet();
                    return LevyraSabrPreflight.createForTests(
                            "abc12345678",
                            YoutubeSabrClientProfile.ANDROID,
                            "audio/webm; codecs=\"opus\"",
                            251,
                            0,
                            160_000,
                            "video/mp4; codecs=\"avc1\"",
                            22,
                            720,
                            2_000_000);
                },
                null,
                60_000L);

        final LevyraResolvedStream result = resolver.resolveSabrPreflight(
                LevyraResolveRequest.forVideoId("abc12345678")
                        .setVideoMode(true)
                        .setRequireStreamingDownloader(true)
                        .build());

        assertFalse(result.isResolved());
        assertEquals(0, calls.get());
        assertEquals(LevyraResolvedStream.Source.STREAMING_DOWNLOADER_REQUIRED,
                result.getSource());
        assertFalse(result.getDiagnostics().isStreamingDownloaderSupported());
    }

    @Test
    public void cachesSuccessfulSabrPreflightWithinTtl() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final LevyraYoutubeResolver resolver = resolverWithFetcher(request -> {
            calls.incrementAndGet();
            return samplePreflight(request.getVideoId());
        }, 60_000L);
        final LevyraResolveRequest request = LevyraResolveRequest.forVideoId("abc12345678")
                .setVideoMode(true)
                .build();

        final LevyraResolvedStream first = resolver.resolveSabrPreflight(request);
        final LevyraResolvedStream second = resolver.resolveSabrPreflight(request);

        assertTrue(first.isResolved());
        assertTrue(second.isResolved());
        assertEquals(1, calls.get());
        assertFalse(first.getDiagnostics().isCacheHit());
        assertTrue(second.getDiagnostics().isCacheHit());
    }

    @Test
    public void deduplicatesConcurrentPreflightRequests() throws Exception {
        final AtomicInteger calls = new AtomicInteger();
        final CountDownLatch entered = new CountDownLatch(1);
        final CountDownLatch release = new CountDownLatch(1);
        final LevyraYoutubeResolver resolver = resolverWithFetcher(request -> {
            calls.incrementAndGet();
            entered.countDown();
            try {
                if (!release.await(2, TimeUnit.SECONDS)) {
                    throw new IOException("test timed out");
                }
            } catch (final InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new IOException("test interrupted", e);
            }
            return samplePreflight(request.getVideoId());
        }, 60_000L);
        final LevyraResolveRequest request = LevyraResolveRequest.forVideoId("abc12345678")
                .setVideoMode(true)
                .build();

        final java.util.concurrent.FutureTask<LevyraResolvedStream> first =
                new java.util.concurrent.FutureTask<>(() -> resolver.resolveSabrPreflight(request));
        final java.util.concurrent.FutureTask<LevyraResolvedStream> second =
                new java.util.concurrent.FutureTask<>(() -> resolver.resolveSabrPreflight(request));
        final Thread firstThread = new Thread(first, "first-preflight");
        final Thread secondThread = new Thread(second, "second-preflight");
        firstThread.start();
        assertTrue(entered.await(2, TimeUnit.SECONDS));
        secondThread.start();
        release.countDown();

        assertTrue(first.get(2, TimeUnit.SECONDS).isResolved());
        assertTrue(second.get(2, TimeUnit.SECONDS).isResolved());
        assertEquals(1, calls.get());
    }

    @Test
    public void selectsFastVideoAndAudioFormatsForVideoMode() throws Exception {
        final LevyraYoutubeResolver resolver = resolverWithFetcher(
                request -> samplePreflight(request.getVideoId()), 60_000L);

        final LevyraResolvedStream result = resolver.resolveSabrPreflight(
                LevyraResolveRequest.forVideoId("abc12345678")
                        .setVideoMode(true)
                        .setMaxVideoHeight(720)
                        .build());

        assertTrue(result.isResolved());
        assertEquals(251, result.getAudioItag());
        assertEquals(22, result.getVideoItag());
        assertEquals(720, result.getVideoHeight());
        assertEquals(LevyraResolvedStream.Source.SABR_PREFLIGHT, result.getSource());
    }

    private static LevyraYoutubeResolver resolverWithFetcher(
            final LevyraYoutubeResolver.SabrPreflightFetcher fetcher,
            final long ttlMs) {
        return new LevyraYoutubeResolver(new StreamingDownloader(), fetcher, null, ttlMs);
    }

    private static LevyraSabrPreflight samplePreflight(final String videoId) {
        return LevyraSabrPreflight.createForTests(
                videoId,
                YoutubeSabrClientProfile.ANDROID,
                "audio/webm; codecs=\"opus\"",
                251,
                0,
                160_000,
                "video/mp4; codecs=\"avc1\"",
                22,
                720,
                2_000_000);
    }

    private static class NonStreamingDownloader extends Downloader {
        @Override
        public Response execute(final Request request) {
            throw new UnsupportedOperationException();
        }

        @Override
        public org.schabi.newpipe.extractor.downloader.CancellableCall executeAsync(
                final Request request,
                final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }

    private static final class StreamingDownloader extends NonStreamingDownloader {
        @Override
        public boolean supportsStreamingResponses() {
            return true;
        }
    }
}
