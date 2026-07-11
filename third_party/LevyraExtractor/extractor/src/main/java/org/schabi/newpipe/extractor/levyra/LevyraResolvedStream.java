package org.schabi.newpipe.extractor.levyra;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.Collections;
import java.util.List;

public final class LevyraResolvedStream {
    public enum Source {
        SABR_PREFLIGHT,
        STREAM_INFO_FALLBACK,
        STREAMING_DOWNLOADER_REQUIRED,
        UNRESOLVED
    }

    private final Source source;
    private final boolean resolved;
    @Nullable
    private final String audioUrl;
    @Nullable
    private final String videoUrl;
    private final int audioItag;
    private final int videoItag;
    private final int videoHeight;
    private final LevyraResolveDiagnostics diagnostics;
    @Nonnull
    private final List<LevyraSponsorBlockSegment> sponsorBlockSegments;
    private final long likeCount;
    private final long dislikeCount;

    private LevyraResolvedStream(@Nonnull final Builder builder) {
        this.source = builder.source;
        this.resolved = builder.resolved;
        this.audioUrl = builder.audioUrl;
        this.videoUrl = builder.videoUrl;
        this.audioItag = builder.audioItag;
        this.videoItag = builder.videoItag;
        this.videoHeight = builder.videoHeight;
        this.diagnostics = builder.diagnostics;
        this.sponsorBlockSegments = builder.sponsorBlockSegments == null ? Collections.emptyList() : builder.sponsorBlockSegments;
        this.likeCount = builder.likeCount;
        this.dislikeCount = builder.dislikeCount;
    }

    @Nonnull
    static Builder builder(@Nonnull final Source source,
                           @Nonnull final LevyraResolveDiagnostics diagnostics) {
        return new Builder(source, diagnostics);
    }

    public boolean isResolved() {
        return resolved;
    }

    @Nonnull
    public Source getSource() {
        return source;
    }

    @Nonnull
    public String getAudioUrl() {
        return audioUrl == null ? "" : audioUrl;
    }

    @Nonnull
    public String getAudioBootstrapUrl() {
        return getAudioUrl();
    }

    @Nonnull
    public String getVideoUrl() {
        return videoUrl == null ? "" : videoUrl;
    }

    @Nonnull
    public String getVideoBootstrapUrl() {
        return getVideoUrl();
    }

    public int getAudioItag() {
        return audioItag;
    }

    public int getVideoItag() {
        return videoItag;
    }

    public int getVideoHeight() {
        return videoHeight;
    }

    @Nonnull
    public LevyraResolveDiagnostics getDiagnostics() {
        return diagnostics;
    }

    @Nonnull
    public List<LevyraSponsorBlockSegment> getSponsorBlockSegments() {
        return sponsorBlockSegments;
    }

    public long getLikeCount() {
        return likeCount;
    }

    public long getDislikeCount() {
        return dislikeCount;
    }

    static final class Builder {
        private final Source source;
        private final LevyraResolveDiagnostics diagnostics;
        private boolean resolved;
        private String audioUrl;
        private String videoUrl;
        private int audioItag = -1;
        private int videoItag = -1;
        private int videoHeight = -1;
        private List<LevyraSponsorBlockSegment> sponsorBlockSegments = null;
        private long likeCount = -1;
        private long dislikeCount = -1;

        private Builder(@Nonnull final Source source,
                        @Nonnull final LevyraResolveDiagnostics diagnostics) {
            this.source = source;
            this.diagnostics = diagnostics;
        }

        @Nonnull
        Builder audio(@Nullable final String audioUrl, final int audioItag) {
            this.audioUrl = audioUrl;
            this.audioItag = audioItag;
            return this;
        }

        @Nonnull
        Builder video(@Nullable final String videoUrl, final int videoItag, final int videoHeight) {
            this.videoUrl = videoUrl;
            this.videoItag = videoItag;
            this.videoHeight = videoHeight;
            return this;
        }

        @Nonnull
        Builder resolved(final boolean resolved) {
            this.resolved = resolved;
            return this;
        }

        @Nonnull
        Builder sponsorBlockSegments(@Nullable final List<LevyraSponsorBlockSegment> segments) {
            this.sponsorBlockSegments = segments;
            return this;
        }

        @Nonnull
        Builder stats(final long likeCount, final long dislikeCount) {
            this.likeCount = likeCount;
            this.dislikeCount = dislikeCount;
            return this;
        }

        @Nonnull
        LevyraResolvedStream build() {
            return new LevyraResolvedStream(this);
        }
    }
}
