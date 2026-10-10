package org.schabi.newpipe.extractor.levyra;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public final class LevyraFormatPolicy {
    @Nullable
    LevyraSabrPreflight.Format selectAudio(@Nonnull final LevyraSabrPreflight preflight,
                                           @Nonnull final LevyraResolveRequest request) {
        LevyraSabrPreflight.Format best = null;
        int bestScore = Integer.MIN_VALUE;
        for (final LevyraSabrPreflight.Format format : preflight.getFormats()) {
            if (!format.isAudio() || format.getUrl().isEmpty()) {
                continue;
            }
            final int score = scoreAudio(format, request.isPreferMp4Audio());
            if (best == null || score > bestScore) {
                best = format;
                bestScore = score;
            }
        }
        return best;
    }

    @Nullable
    LevyraSabrPreflight.Format selectVideo(@Nonnull final LevyraSabrPreflight preflight,
                                           @Nonnull final LevyraResolveRequest request) {
        LevyraSabrPreflight.Format best = null;
        int bestScore = Integer.MIN_VALUE;
        for (final LevyraSabrPreflight.Format format : preflight.getFormats()) {
            if (!format.isVideo() || format.getUrl().isEmpty()) {
                continue;
            }
            if (format.getHeight() > request.getMaxVideoHeight()) {
                continue;
            }
            final int score = scoreVideo(format);
            if (best == null || score > bestScore) {
                best = format;
                bestScore = score;
            }
        }
        return best;
    }

    private int scoreAudio(@Nonnull final LevyraSabrPreflight.Format format,
                           final boolean preferMp4Audio) {
        final String mime = format.getMimeType().toLowerCase(java.util.Locale.ROOT);
        final boolean mp4 = mime.contains("mp4") || mime.contains("m4a");
        final boolean opus = mime.contains("opus") || mime.contains("webm");
        int score = Math.max(0, format.getBitrate());
        if (format.getBitrate() >= 256_000) {
            score += 2_000_000;
        }
        if (format.isOriginalAudio()) {
            score += 600_000;
        }
        if (preferMp4Audio && mp4) {
            score += 900_000;
        } else if (!preferMp4Audio && opus) {
            score += 500_000;
        }
        return score;
    }

    private int scoreVideo(@Nonnull final LevyraSabrPreflight.Format format) {
        final String mime = format.getMimeType().toLowerCase(java.util.Locale.ROOT);
        int score = Math.max(0, format.getHeight()) * 10_000 + Math.max(0, format.getBitrate());
        if (mime.contains("av01") || mime.contains("av1")) {
            score += 4_000_000;
        } else if (mime.contains("vp9")) {
            score += 3_000_000;
        } else if (mime.contains("mp4") || mime.contains("avc")) {
            score += 2_000_000;
        }
        return score;
    }
}
