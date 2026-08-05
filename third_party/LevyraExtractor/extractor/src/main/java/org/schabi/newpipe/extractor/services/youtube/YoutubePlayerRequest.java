package org.schabi.newpipe.extractor.services.youtube;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

/**
 * Final player request body together with the visitor identity and client version written into it.
 */
public final class YoutubePlayerRequest {
    @Nonnull
    private final byte[] body;
    @Nullable
    private final String visitorData;
    @Nullable
    private final String clientVersion;

    YoutubePlayerRequest(@Nonnull final byte[] body,
                         @Nullable final String visitorData,
                         @Nullable final String clientVersion) {
        this.body = body;
        this.visitorData = visitorData;
        this.clientVersion = clientVersion;
    }

    @Nonnull
    public byte[] getBody() {
        return body;
    }

    @Nullable
    public String getVisitorData() {
        return visitorData;
    }

    @Nullable
    public String getClientVersion() {
        return clientVersion;
    }
}
