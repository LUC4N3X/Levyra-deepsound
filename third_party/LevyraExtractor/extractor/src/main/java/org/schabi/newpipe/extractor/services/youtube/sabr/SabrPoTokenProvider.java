package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoToken;
import org.schabi.newpipe.extractor.services.youtube.YoutubeSessionPoTokenProvider;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;

/**
 * Supplies visitor-bound player and video PO tokens for experimental SABR requests.
 */
@FunctionalInterface
public interface SabrPoTokenProvider extends YoutubeSessionPoTokenProvider {
    /**
     * Returns the visitor-bound token pair for a strict SABR player reload. Implementations which
     * support attestation identity rotation must source this pair from the same provider instance
     * whose identity is invalidated by {@link #invalidatePoTokenIdentity(YoutubeSabrInfo)}.
     */
    @Nullable
    @Override
    default YoutubeSessionPoToken getSessionPoToken(
            @Nonnull final String clientName,
            @Nonnull final String clientVersion,
            @Nullable final String userAgent,
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            final boolean loggedIn) throws IOException, ExtractionException {
        return null;
    }

    /**
     * Returns raw PO token bytes for the current SABR session, or {@code null} if unavailable.
     */
    @Nullable
    byte[] getPoToken(@Nonnull YoutubeSabrInfo info,
                      @Nonnull YoutubeSabrStreamState streamState)
            throws IOException, ExtractionException;

    /**
     * Invalidates the visitor identity, generator, and tokens belonging to a rejected
     * attestation epoch. Implementations return {@code true} only when the next player request
     * will use a fresh visitor identity.
     */
    default boolean invalidatePoTokenIdentity(@Nonnull final YoutubeSabrInfo info)
            throws IOException, ExtractionException {
        return false;
    }

}
