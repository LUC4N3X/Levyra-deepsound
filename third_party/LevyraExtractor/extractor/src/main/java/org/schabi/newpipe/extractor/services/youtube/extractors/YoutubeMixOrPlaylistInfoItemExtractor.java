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
import org.schabi.newpipe.extractor.utils.Utils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

public class YoutubeMixOrPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private static final String MIX_TITLE_PREFIX = "Mix - ";
    private static final String ZULU_MIX_TITLE_PREFIX = "Imiksi - ";
    private static final Pattern ABBREVIATED_COUNT =
            Pattern.compile("\\d+(?:[.,]\\d+)?[KMBkmb](?!\\p{L})");
    private static final Pattern GROUPED_COUNT =
            Pattern.compile("\\d+(?:[.,\\u00a0\\u202f]\\d+)*");
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
        if (getPlaylistType() != PlaylistInfo.PlaylistType.NORMAL
                && name.startsWith(ZULU_MIX_TITLE_PREFIX)) {
            return MIX_TITLE_PREFIX + name.substring(ZULU_MIX_TITLE_PREFIX.length());
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
        if (getPlaylistType() != PlaylistInfo.PlaylistType.NORMAL) {
            return "YouTube";
        }
        return YoutubeParsingHelper.getTextFromObject(mixInfoItem.getObject("longBylineText"));
    }

    @Override
    public long getStreamCount() throws ParsingException {
        final String shortCountString = YoutubeParsingHelper.getTextFromObject(
                mixInfoItem.getObject("videoCountShortText"));
        if (shortCountString != null) {
            try {
                return Integer.parseInt(shortCountString);
            } catch (final NumberFormatException ignored) {
                // un-parsable integer: this is a mix with infinite items and "50+" as count string
                // (though youtube music mixes do not necessarily have an infinite count of songs)
                return ListExtractor.ITEM_COUNT_INFINITE;
            }
        }

        String countString = mixInfoItem.getString("videoCount");
        if (isNullOrEmpty(countString)) {
            countString = YoutubeParsingHelper.getTextFromObject(
                    mixInfoItem.getObject("videoCountText"));
        }
        if (isNullOrEmpty(countString)) {
            // The item count is optional too: some incomplete playlist/mix items have no
            // count at all. Report "unknown" rather than throwing, so the item is
            // still committed instead of bubbling up a spurious error (#2546).
            return ListExtractor.ITEM_COUNT_UNKNOWN;
        }
        if (countString.contains("+")) {
            return ListExtractor.ITEM_COUNT_INFINITE;
        }
        if (ABBREVIATED_COUNT.matcher(countString).find()) {
            try {
                return Utils.mixedNumberWordToLong(countString);
            } catch (final NumberFormatException | ParsingException e) {
                return ListExtractor.ITEM_COUNT_UNKNOWN;
            }
        }
        final Matcher number = GROUPED_COUNT.matcher(countString);
        if (!number.find()) {
            return ListExtractor.ITEM_COUNT_UNKNOWN;
        }
        final String[] groups = number.group().split("[.,\\u00a0\\u202f]");
        for (int i = 1; i < groups.length; i++) {
            if (groups[i].length() != 3) {
                return ListExtractor.ITEM_COUNT_UNKNOWN;
            }
        }
        final String digits = String.join("", groups);
        if (digits.length() > 9) {
            return ListExtractor.ITEM_COUNT_UNKNOWN;
        }
        return Long.parseLong(digits);
    }

    @Nonnull
    @Override
    public PlaylistInfo.PlaylistType getPlaylistType() throws ParsingException {
        return extractPlaylistTypeFromPlaylistUrl(getUrl());
    }
}
