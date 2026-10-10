package org.schabi.newpipe.extractor.levyra;

import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LevyraResolveRequest {
    private final String videoId;
    private final boolean videoMode;
    private final boolean requireStreamingDownloader;
    private final boolean preferMp4Audio;
    private final int maxVideoHeight;
    private final YoutubeSabrClientProfile profile;
    private final Localization localization;
    private final ContentCountry contentCountry;

    private LevyraResolveRequest(@Nonnull final Builder builder) {
        this.videoId = builder.videoId;
        this.videoMode = builder.videoMode;
        this.requireStreamingDownloader = builder.requireStreamingDownloader;
        this.preferMp4Audio = builder.preferMp4Audio;
        this.maxVideoHeight = builder.maxVideoHeight;
        this.profile = builder.profile;
        this.localization = builder.localization;
        this.contentCountry = builder.contentCountry;
    }

    @Nonnull
    public static Builder forVideoId(@Nonnull final String videoId) {
        return new Builder(videoId);
    }

    @Nonnull
    public String getVideoId() {
        return videoId;
    }

    public boolean isVideoMode() {
        return videoMode;
    }

    public boolean isRequireStreamingDownloader() {
        return requireStreamingDownloader;
    }

    public boolean isPreferMp4Audio() {
        return preferMp4Audio;
    }

    public int getMaxVideoHeight() {
        return maxVideoHeight;
    }

    @Nonnull
    public YoutubeSabrClientProfile getProfile() {
        return profile;
    }

    @Nonnull
    public Localization getLocalization() {
        return localization;
    }

    @Nonnull
    public ContentCountry getContentCountry() {
        return contentCountry;
    }

    @Nonnull
    String cacheKey() {
        return videoId + "|" + profile.name() + "|" + localization.getLocalizationCode() + "|"
                + contentCountry.getCountryCode();
    }

    public static final class Builder {
        private final String videoId;
        private boolean videoMode;
        private boolean requireStreamingDownloader = true;
        private boolean preferMp4Audio;
        private int maxVideoHeight = 1080;
        private YoutubeSabrClientProfile profile = YoutubeSabrClientProfile.ANDROID;
        private Localization localization = Localization.DEFAULT;
        private ContentCountry contentCountry = ContentCountry.DEFAULT;

        private Builder(@Nonnull final String videoId) {
            if (videoId.trim().isEmpty()) {
                throw new IllegalArgumentException("videoId must not be blank");
            }
            this.videoId = videoId.trim();
        }

        @Nonnull
        public Builder setVideoMode(final boolean videoMode) {
            this.videoMode = videoMode;
            return this;
        }

        @Nonnull
        public Builder setRequireStreamingDownloader(final boolean requireStreamingDownloader) {
            this.requireStreamingDownloader = requireStreamingDownloader;
            return this;
        }

        @Nonnull
        public Builder setPreferMp4Audio(final boolean preferMp4Audio) {
            this.preferMp4Audio = preferMp4Audio;
            return this;
        }

        @Nonnull
        public Builder setMaxVideoHeight(final int maxVideoHeight) {
            this.maxVideoHeight = maxVideoHeight <= 0 ? 1080 : maxVideoHeight;
            return this;
        }

        @Nonnull
        public Builder setProfile(@Nullable final YoutubeSabrClientProfile profile) {
            this.profile = profile == null ? YoutubeSabrClientProfile.ANDROID : profile;
            return this;
        }

        @Nonnull
        public Builder setLocalization(@Nullable final Localization localization) {
            this.localization = localization == null ? Localization.DEFAULT : localization;
            return this;
        }

        @Nonnull
        public Builder setContentCountry(@Nullable final ContentCountry contentCountry) {
            this.contentCountry = contentCountry == null ? ContentCountry.DEFAULT : contentCountry;
            return this;
        }

        @Nonnull
        public LevyraResolveRequest build() {
            return new LevyraResolveRequest(this);
        }
    }
}
