package org.schabi.newpipe.extractor.stream;

import org.schabi.newpipe.extractor.localization.DateWrapper;

import java.io.Serializable;
import java.time.Duration;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class SongMetadata implements Serializable {
    public static final int TRACK_UNKNOWN = -1;

    @Nonnull
    private final String title;
    @Nonnull
    private final String artist;
    @Nonnull
    private final List<String> performers;
    @Nullable
    private final String composer;
    @Nullable
    private final String genre;
    @Nullable
    private final String album;
    private final int track;
    @Nullable
    private final Duration duration;
    @Nullable
    private final DateWrapper releaseDate;
    @Nullable
    private final String label;
    @Nullable
    private final String copyright;
    @Nullable
    private final String location;

    private SongMetadata(@Nonnull final Builder builder) {
        title = builder.title;
        artist = builder.artist;
        performers = Collections.unmodifiableList(new ArrayList<>(builder.performers));
        composer = builder.composer;
        genre = builder.genre;
        album = builder.album;
        track = builder.track;
        duration = builder.duration;
        releaseDate = builder.releaseDate;
        label = builder.label;
        copyright = builder.copyright;
        location = builder.location;
    }

    @Nonnull
    public String getTitle() {
        return title;
    }

    @Nonnull
    public String getArtist() {
        return artist;
    }

    @Nonnull
    public List<String> getPerformers() {
        return performers;
    }

    @Nullable
    public String getComposer() {
        return composer;
    }

    @Nullable
    public String getGenre() {
        return genre;
    }

    @Nullable
    public String getAlbum() {
        return album;
    }

    public int getTrack() {
        return track;
    }

    @Nullable
    public Duration getDuration() {
        return duration;
    }

    @Nullable
    public DateWrapper getReleaseDate() {
        return releaseDate;
    }

    @Nullable
    public String getLabel() {
        return label;
    }

    @Nullable
    public String getCopyright() {
        return copyright;
    }

    @Nullable
    public String getLocation() {
        return location;
    }

    public static final class Builder {
        @Nonnull
        private final String title;
        @Nonnull
        private final String artist;
        @Nonnull
        private final LinkedHashSet<String> performers = new LinkedHashSet<>();
        @Nullable
        private String composer;
        @Nullable
        private String genre;
        @Nullable
        private String album;
        private int track = TRACK_UNKNOWN;
        @Nullable
        private Duration duration;
        @Nullable
        private DateWrapper releaseDate;
        @Nullable
        private String label;
        @Nullable
        private String copyright;
        @Nullable
        private String location;

        public Builder(@Nonnull final String title, @Nonnull final String artist) {
            this.title = title.trim();
            this.artist = artist.trim();
        }

        @Nonnull
        public Builder setPerformers(@Nonnull final List<String> values) {
            performers.clear();
            values.forEach(this::addPerformer);
            return this;
        }

        @Nonnull
        public Builder addPerformer(@Nonnull final String value) {
            final String cleaned = value.trim();
            if (!cleaned.isEmpty()) {
                performers.add(cleaned);
            }
            return this;
        }

        @Nonnull
        public Builder setComposer(@Nullable final String value) {
            composer = clean(value);
            return this;
        }

        @Nonnull
        public Builder setGenre(@Nullable final String value) {
            genre = clean(value);
            return this;
        }

        @Nonnull
        public Builder setAlbum(@Nullable final String value) {
            album = clean(value);
            return this;
        }

        @Nonnull
        public Builder setTrack(final int value) {
            track = value > 0 ? value : TRACK_UNKNOWN;
            return this;
        }

        @Nonnull
        public Builder setDuration(@Nullable final Duration value) {
            duration = value != null && !value.isNegative() ? value : null;
            return this;
        }

        @Nonnull
        public Builder setReleaseDate(@Nullable final DateWrapper value) {
            releaseDate = value;
            return this;
        }

        @Nonnull
        public Builder setLabel(@Nullable final String value) {
            label = clean(value);
            return this;
        }

        @Nonnull
        public Builder setCopyright(@Nullable final String value) {
            copyright = clean(value);
            return this;
        }

        @Nonnull
        public Builder setLocation(@Nullable final String value) {
            location = clean(value);
            return this;
        }

        @Nonnull
        public SongMetadata build() {
            return new SongMetadata(this);
        }

        @Nullable
        private static String clean(@Nullable final String value) {
            if (value == null) {
                return null;
            }
            final String cleaned = value.trim();
            return cleaned.isEmpty() ? null : cleaned;
        }
    }
}
