package org.schabi.newpipe.extractor.services.youtube.sabr;

import javax.annotation.Nonnull;

public final class SabrMediaSegment {
    @Nonnull
    private final SabrMediaHeader header;
    @Nonnull
    private final byte[] data;

    SabrMediaSegment(@Nonnull final SabrMediaHeader header, @Nonnull final byte[] data) {
        this.header = header;
        // No defensive copy: the collector hands over a freshly built array it does not retain.
        // Media segments reach several MB (4K), so cloning here doubled peak memory and caused OOM
        // under rapid switching. The array is treated as immutable from here on.
        this.data = data;
    }

    @Nonnull
    public SabrMediaHeader getHeader() {
        return header;
    }

    /** Read-only: callers must not mutate the returned array (no defensive copy, for memory). */
    @Nonnull
    public byte[] getData() {
        return data;
    }

    public int getLength() {
        return data.length;
    }
}
