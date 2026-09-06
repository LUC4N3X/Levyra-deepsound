package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.comments.CommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.utils.Utils;

import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getTextFromObject;

public class YoutubeLiveChatInfoItemExtractor implements CommentsInfoItemExtractor {

    private static final long MICROSECONDS_PER_MILLISECOND = 1000L;

    @Nonnull
    private final JsonObject renderer;
    @Nonnull
    private final String url;

    public YoutubeLiveChatInfoItemExtractor(@Nonnull final JsonObject renderer,
                                            @Nonnull final String url) {
        this.renderer = renderer;
        this.url = url;
    }

    @Nonnull
    @Override
    public String getUrl() {
        return url;
    }

    @Override
    public String getName() {
        return getUploaderName();
    }

    @Override
    public String getCommentId() {
        return renderer.getString("id", Utils.EMPTY_STRING);
    }

    @Override
    public String getCommentText() {
        return extractLiveChatMessage(renderer.getObject("message"));
    }

    @Override
    public String getUploaderName() {
        return safeTextFromObject(renderer.getObject("authorName"));
    }

    @Override
    public String getUploaderAvatarUrl() {
        return extractLargestThumbnailUrl(renderer.getObject("authorPhoto").getArray("thumbnails"));
    }

    @Override
    public String getUploaderUrl() {
        final String channelId = renderer.getString("authorExternalChannelId",
                Utils.EMPTY_STRING).trim();
        return channelId.isEmpty()
                ? Utils.EMPTY_STRING
                : "https://www.youtube.com/channel/" + channelId;
    }

    @Override
    public boolean isUploaderVerified() {
        return renderer.getArray("authorBadges")
                .stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(badge -> badge.getObject("liveChatAuthorBadgeRenderer")
                        .getObject("icon")
                        .getString("iconType", Utils.EMPTY_STRING))
                .anyMatch(iconType -> "VERIFIED".equals(iconType)
                        || "CHECK_CIRCLE_THICK".equals(iconType)
                        || "OFFICIAL_ARTIST_BADGE".equals(iconType));
    }

    @Override
    public String getTextualUploadDate() {
        return safeTextFromObject(renderer.getObject("timestampText"));
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() {
        final long timestampUsec = parseTimestampUsec(
                renderer.getString("timestampUsec", Utils.EMPTY_STRING));
        if (timestampUsec <= 0) {
            return null;
        }
        return new DateWrapper(OffsetDateTime.ofInstant(
                Instant.ofEpochMilli(timestampUsec / MICROSECONDS_PER_MILLISECOND),
                ZoneOffset.UTC));
    }

    @Override
    public boolean isLiveChat() {
        return true;
    }

    private static long parseTimestampUsec(@Nonnull final String timestampUsec) {
        try {
            return Long.parseLong(timestampUsec.trim());
        } catch (final NumberFormatException e) {
            return -1L;
        }
    }

    @Nonnull
    private static String safeTextFromObject(@Nullable final JsonObject textObject) {
        if (textObject == null || textObject.isEmpty()) {
            return Utils.EMPTY_STRING;
        }
        try {
            final String text = getTextFromObject(textObject);
            return text == null ? Utils.EMPTY_STRING : text.trim();
        } catch (final ParsingException | RuntimeException e) {
            return Utils.EMPTY_STRING;
        }
    }

    @Nonnull
    static String extractLargestThumbnailUrl(@Nullable final JsonArray thumbnails) {
        if (thumbnails == null) {
            return Utils.EMPTY_STRING;
        }
        String best = Utils.EMPTY_STRING;
        int bestWidth = -1;
        for (final Object thumbnail : thumbnails) {
            if (!(thumbnail instanceof JsonObject)) {
                continue;
            }
            final JsonObject thumbnailObject = (JsonObject) thumbnail;
            final String thumbnailUrl = thumbnailObject.getString("url", Utils.EMPTY_STRING).trim();
            if (thumbnailUrl.isEmpty()) {
                continue;
            }
            final int width = thumbnailObject.getInt("width", 0);
            if (width > bestWidth) {
                bestWidth = width;
                best = thumbnailUrl;
            }
        }
        return best;
    }

    @Nonnull
    static String extractLiveChatMessage(@Nullable final JsonObject message) {
        if (message == null || message.isEmpty()) {
            return Utils.EMPTY_STRING;
        }
        final JsonArray runs = message.getArray("runs");
        if (runs.isEmpty()) {
            return message.getString("simpleText", Utils.EMPTY_STRING).trim();
        }
        final StringBuilder builder = new StringBuilder();
        for (final Object run : runs) {
            if (!(run instanceof JsonObject)) {
                continue;
            }
            final JsonObject runObject = (JsonObject) run;
            if (runObject.has("emoji")) {
                builder.append(extractEmojiText(runObject.getObject("emoji")));
            } else {
                builder.append(runObject.getString("text", Utils.EMPTY_STRING));
            }
        }
        return builder.toString().trim();
    }

    @Nonnull
    static String extractEmojiText(@Nullable final JsonObject emoji) {
        if (emoji == null || emoji.isEmpty()) {
            return Utils.EMPTY_STRING;
        }
        final String emojiId = emoji.getString("emojiId", Utils.EMPTY_STRING).trim();
        final boolean customEmoji = emoji.getBoolean("isCustomEmoji", false)
                || emojiId.indexOf('/') >= 0;
        if (!customEmoji && !emojiId.isEmpty()) {
            return emojiId;
        }

        final String shortcut = firstNonBlankString(emoji.getArray("shortcuts"));
        if (shortcut != null) {
            return shortcut;
        }

        final String searchTerm = firstNonBlankString(emoji.getArray("searchTerms"));
        if (searchTerm != null) {
            return ":" + searchTerm + ":";
        }

        final String label = emoji.getObject("image")
                .getObject("accessibility")
                .getObject("accessibilityData")
                .getString("label", Utils.EMPTY_STRING)
                .trim();
        if (!label.isEmpty()) {
            return label;
        }

        return emojiId;
    }

    @Nullable
    private static String firstNonBlankString(@Nullable final JsonArray array) {
        if (array == null) {
            return null;
        }
        for (final Object value : array) {
            if (value instanceof String) {
                final String candidate = ((String) value).trim();
                if (!candidate.isEmpty()) {
                    return candidate;
                }
            }
        }
        return null;
    }
}
