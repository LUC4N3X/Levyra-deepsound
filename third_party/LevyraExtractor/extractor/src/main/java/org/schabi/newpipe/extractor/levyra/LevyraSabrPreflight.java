package org.schabi.newpipe.extractor.levyra;

import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrFormat;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrInfo;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

public final class LevyraSabrPreflight {
    private final String videoId;
    private final YoutubeSabrClientProfile profile;
    private final List<Format> formats;

    private LevyraSabrPreflight(@Nonnull final String videoId,
                                @Nonnull final YoutubeSabrClientProfile profile,
                                @Nonnull final List<Format> formats) {
        this.videoId = videoId;
        this.profile = profile;
        this.formats = Collections.unmodifiableList(new ArrayList<>(formats));
    }

    @Nonnull
    static LevyraSabrPreflight fromInfo(@Nonnull final YoutubeSabrInfo info) {
        final List<Format> formats = new ArrayList<>();
        for (final YoutubeSabrFormat format : info.getFormats()) {
            formats.add(new Format(
                    format.getItag(),
                    format.getMimeType(),
                    format.getInitializationUrl(),
                    format.getHeight(),
                    format.getBitrate(),
                    format.isAudio(),
                    format.isVideo(),
                    format.isOriginalAudio()));
        }
        return new LevyraSabrPreflight(info.getVideoId(), info.getProfile(), formats);
    }

    @Nonnull
    public static LevyraSabrPreflight createForTests(
            @Nonnull final String videoId,
            @Nonnull final YoutubeSabrClientProfile profile,
            @Nonnull final String audioMime,
            final int audioItag,
            final int audioHeight,
            final int audioBitrate,
            @Nonnull final String videoMime,
            final int videoItag,
            final int videoHeight,
            final int videoBitrate) {
        final List<Format> formats = new ArrayList<>();
        formats.add(new Format(audioItag, audioMime, "https://example.invalid/audio",
                audioHeight, audioBitrate, true, false, true));
        formats.add(new Format(videoItag, videoMime, "https://example.invalid/video",
                videoHeight, videoBitrate, false, true, false));
        return new LevyraSabrPreflight(videoId, profile, formats);
    }

    @Nonnull
    public String getVideoId() {
        return videoId;
    }

    @Nonnull
    public YoutubeSabrClientProfile getProfile() {
        return profile;
    }

    @Nonnull
    List<Format> getFormats() {
        return formats;
    }

    static final class Format {
        private final int itag;
        @Nullable
        private final String mimeType;
        @Nullable
        private final String url;
        private final int height;
        private final int bitrate;
        private final boolean audio;
        private final boolean video;
        private final boolean originalAudio;

        private Format(final int itag,
                       @Nullable final String mimeType,
                       @Nullable final String url,
                       final int height,
                       final int bitrate,
                       final boolean audio,
                       final boolean video,
                       final boolean originalAudio) {
            this.itag = itag;
            this.mimeType = mimeType;
            this.url = url;
            this.height = height;
            this.bitrate = bitrate;
            this.audio = audio;
            this.video = video;
            this.originalAudio = originalAudio;
        }

        int getItag() {
            return itag;
        }

        @Nonnull
        String getMimeType() {
            return mimeType == null ? "" : mimeType;
        }

        @Nonnull
        String getUrl() {
            return url == null ? "" : url;
        }

        int getHeight() {
            return height;
        }

        int getBitrate() {
            return bitrate;
        }

        boolean isAudio() {
            return audio;
        }

        boolean isVideo() {
            return video;
        }

        boolean isOriginalAudio() {
            return originalAudio;
        }
    }
}
