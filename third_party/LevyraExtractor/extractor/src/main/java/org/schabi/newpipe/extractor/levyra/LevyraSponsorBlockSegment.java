package org.schabi.newpipe.extractor.levyra;

import javax.annotation.Nonnull;

public final class LevyraSponsorBlockSegment {
    private final String category;
    private final double startTime;
    private final double endTime;

    public LevyraSponsorBlockSegment(@Nonnull final String category, final double startTime, final double endTime) {
        this.category = category;
        this.startTime = startTime;
        this.endTime = endTime;
    }

    @Nonnull
    public String getCategory() {
        return category;
    }

    public double getStartTime() {
        return startTime;
    }

    public double getEndTime() {
        return endTime;
    }
}
