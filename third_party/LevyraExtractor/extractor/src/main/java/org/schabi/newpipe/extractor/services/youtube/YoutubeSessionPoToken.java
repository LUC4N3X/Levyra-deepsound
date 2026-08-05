package org.schabi.newpipe.extractor.services.youtube;

import java.util.Objects;

import javax.annotation.Nonnull;

/**
 * A visitor-bound proof-of-origin token and the visitor identity used to mint it.
 */
public final class YoutubeSessionPoToken {
    @Nonnull
    private final String visitorData;
    @Nonnull
    private final String poToken;

    public YoutubeSessionPoToken(@Nonnull final String visitorData,
                                 @Nonnull final String poToken) {
        this.visitorData = Objects.requireNonNull(visitorData);
        this.poToken = Objects.requireNonNull(poToken);
    }

    @Nonnull
    public String getVisitorData() {
        return visitorData;
    }

    @Nonnull
    public String getPoToken() {
        return poToken;
    }
}
