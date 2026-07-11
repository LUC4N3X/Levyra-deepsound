package org.schabi.newpipe.extractor.levyra;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LevyraResolveDiagnostics {
    private final boolean streamingDownloaderSupported;
    private final boolean cacheHit;
    private final boolean inFlightJoin;
    private final long elapsedMs;
    @Nullable
    private final String fallbackReason;

    LevyraResolveDiagnostics(
            final boolean streamingDownloaderSupported,
            final boolean cacheHit,
            final boolean inFlightJoin,
            final long elapsedMs,
            @Nullable final String fallbackReason) {
        this.streamingDownloaderSupported = streamingDownloaderSupported;
        this.cacheHit = cacheHit;
        this.inFlightJoin = inFlightJoin;
        this.elapsedMs = elapsedMs;
        this.fallbackReason = fallbackReason;
    }

    public boolean isStreamingDownloaderSupported() {
        return streamingDownloaderSupported;
    }

    public boolean isCacheHit() {
        return cacheHit;
    }

    public boolean isInFlightJoin() {
        return inFlightJoin;
    }

    public long getElapsedMs() {
        return elapsedMs;
    }

    @Nonnull
    public String getFallbackReason() {
        return fallbackReason == null ? "" : fallbackReason;
    }
}
