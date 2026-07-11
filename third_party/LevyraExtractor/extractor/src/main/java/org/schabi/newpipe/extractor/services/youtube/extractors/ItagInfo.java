package org.schabi.newpipe.extractor.services.youtube.extractors;

import org.schabi.newpipe.extractor.services.youtube.ItagItem;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.Serializable;

/**
 * Class to build easier {@link org.schabi.newpipe.extractor.stream.Stream}s for
 * {@link YoutubeStreamExtractor}.
 *
 * <p>
 * It stores, per stream:
 * <ul>
 *     <li>its content (the URL/the base URL of streams);</li>
 *     <li>whether its content is the URL the content itself or the base URL;</li>
 *     <li>its associated {@link ItagItem};</li>
 *     <li>optionally, an obfuscated signature for batch deobfuscation.</li>
 * </ul>
 * </p>
 */
final class ItagInfo implements Serializable {
    @Nonnull
    private String content;
    @Nonnull
    private final ItagItem itagItem;
    private boolean isUrl;
    @Nullable
    private String obfuscatedSignature;
    @Nullable
    private String audioTrackId;
    @Nullable
    private String audioTrackName;
    @Nullable
    private String audioLocale;

    /**
     * Creates a new {@code ItagInfo} instance.
     *
     * @param content  the content of the stream, which must be not null
     * @param itagItem the {@link ItagItem} associated with the stream, which must be not null
     */
    ItagInfo(@Nonnull final String content,
             @Nonnull final ItagItem itagItem) {
        this.content = content;
        this.itagItem = itagItem;
    }

    /**
     * Sets whether the stream is a URL.
     *
     * @param isUrl whether the content is a URL
     */
    void setIsUrl(final boolean isUrl) {
        this.isUrl = isUrl;
    }

    /**
     * Sets the content (URL) of the stream.
     * This is used for batch deobfuscation to update the URL after processing.
     *
     * @param content the new content
     */
    void setContent(@Nonnull final String content) {
        this.content = content;
    }

    /**
     * Sets the obfuscated signature for batch deobfuscation.
     *
     * @param obfuscatedSignature the obfuscated signature
     */
    void setObfuscatedSignature(@Nullable final String obfuscatedSignature) {
        this.obfuscatedSignature = obfuscatedSignature;
    }

    /**
     * Gets the obfuscated signature.
     *
     * @return the obfuscated signature, or null if not set
     */
    @Nullable
    String getObfuscatedSignature() {
        return obfuscatedSignature;
    }

    /**
     * Gets the content stored in this {@code ItagInfo} instance, which is either the URL to the
     * content itself or the base URL.
     *
     * @return the content stored in this {@code ItagInfo} instance
     */
    @Nonnull
    String getContent() {
        return content;
    }

    /**
     * Gets the {@link ItagItem} associated with this {@code ItagInfo} instance.
     *
     * @return the {@link ItagItem} associated with this {@code ItagInfo} instance, which is not
     * null
     */
    @Nonnull
    ItagItem getItagItem() {
        return itagItem;
    }

    /**
     * Gets whether the content stored is the URL to the content itself or the base URL of it.
     *
     * @return whether the content stored is the URL to the content itself or the base URL of it
     * @see #getContent() for more details
     */
    boolean getIsUrl() {
        return isUrl;
    }

    void setAudioTrackInfo(@Nullable final String audioTrackId,
                           @Nullable final String audioTrackName,
                           @Nullable final String audioLocale) {
        this.audioTrackId = audioTrackId;
        this.audioTrackName = audioTrackName;
        this.audioLocale = audioLocale;
    }

    @Nullable
    String getAudioTrackId() {
        return audioTrackId;
    }

    @Nullable
    String getAudioTrackName() {
        return audioTrackName;
    }

    @Nullable
    String getAudioLocale() {
        return audioLocale;
    }
}
