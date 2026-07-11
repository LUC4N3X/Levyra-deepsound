package org.schabi.newpipe.extractor.services.youtube.extractors;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.extractPlaylistTypeFromPlaylistUrl;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getThumbnailUrlFromInfoItem;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubePlaylistLinkHandlerFactory;

import javax.annotation.Nonnull;

public class YoutubeMixOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject mixInfoItem;

    public YoutubeMixOrPlaylistInfoItemExtractor(final JsonObject mixInfoItem) {
        this.mixInfoItem = mixInfoItem;
    }

    @Override
    public String getName() throws ParsingException {
        final String name = getTextFromObject(mixInfoItem.getObject("title"));
        if (isNullOrEmpty(name)) {
            throw new ParsingException("Could not get name");
        }
        return name;
    }

    @Override
    public String getUrl() throws ParsingException {
        // Mixes and radio renderers expose a ready-to-share "shareUrl"; use it when present.
        final String url = mixInfoItem.getString("shareUrl");
        if (!isNullOrEmpty(url)) {
            return url;
        }
        // Regular channel playlists surfaced in channel tabs (e.g. channel-search results)
        // carry no "shareUrl" but have a "playlistId". Rebuild the canonical playlist URL
        // from it instead of throwing "Could not get url", which used to abort the whole
        // item and surface a spurious error snackbar to the user while the other results
        // kept loading fine (#2546).
        final String playlistId = mixInfoItem.getString("playlistId");
        if (!isNullOrEmpty(playlistId)) {
            try {
                return YoutubePlaylistLinkHandlerFactory.getInstance().getUrl(playlistId);
            } catch (final Exception e) {
                throw new ParsingException("Could not get url", e);
            }
        }
        throw new ParsingException("Could not get url");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        // A thumbnail is optional: some incomplete playlist/mix items surfaced in channel
        // tabs (e.g. channel-search results) carry no usable thumbnail. Return null rather
        // than throwing, so the item still shows up (with a placeholder) instead of being
        // turned into a user-facing error snackbar (#2546).
        try {
            return getThumbnailUrlFromInfoItem(mixInfoItem);
        } catch (final ParsingException e) {
            return null;
        }
    }

    @Override
    public String getUploaderName() throws ParsingException {
        // this will be "YouTube" for mixes
        return YoutubeParsingHelper.getTextFromObject(mixInfoItem.getObject("longBylineText"));
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String countString = YoutubeParsingHelper.getTextFromObject(
                mixInfoItem.getObject("videoCountShortText"));
        if (countString == null) {
            // The item count is optional too: some incomplete playlist/mix items have no
            // "videoCountShortText". Report "unknown" rather than throwing, so the item is
            // still committed instead of bubbling up a spurious error (#2546).
            return ListExtractor.ITEM_COUNT_UNKNOWN;
        }

        try {
            return Integer.parseInt(countString);
        } catch (final NumberFormatException ignored) {
            // un-parsable integer: this is a mix with infinite items and "50+" as count string
            // (though youtube music mixes do not necessarily have an infinite count of songs)
            return ListExtractor.ITEM_COUNT_INFINITE;
        }
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistUrl(getUrl());
    }
}
