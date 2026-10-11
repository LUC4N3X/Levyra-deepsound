package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.localization.TimeAgoPatternsManager;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class YoutubeCommentsEUVMInfoItemExtractorTest {
    @Test
    void selectsLastAvailableSourceBeforeAuthorFallback() throws ParsingException {
        final JsonArray sources = new JsonArray();
        sources.add(source("https://example.com/first.jpg"));
        sources.add(source("https://example.com/last.jpg"));
        final JsonObject image = new JsonObject();
        image.put("sources", sources);
        final JsonObject avatar = new JsonObject();
        avatar.put("image", image);

        assertEquals("https://example.com/last.jpg",
                extractor(entity(avatar, "https://example.com/fallback.jpg"))
                        .getUploaderAvatarUrl());
    }

    @Test
    void missingImageUsesAuthorFallback() throws ParsingException {
        assertEquals("https://example.com/fallback.jpg",
                extractor(entity(new JsonObject(), "//example.com/fallback.jpg"))
                        .getUploaderAvatarUrl());
    }

    @Test
    void missingAvatarObjectUsesAuthorFallback() throws ParsingException {
        assertEquals("https://example.com/fallback.jpg",
                extractor(entity(null, "https://example.com/fallback.jpg"))
                        .getUploaderAvatarUrl());
    }

    @Test
    void missingSourcesUsesAuthorFallback() throws ParsingException {
        final JsonObject avatar = new JsonObject();
        avatar.put("image", new JsonObject());

        assertEquals("https://example.com/fallback.jpg",
                extractor(entity(avatar, "https://example.com/fallback.jpg"))
                        .getUploaderAvatarUrl());
    }

    @Test
    void emptySourcesUsesAuthorFallback() throws ParsingException {
        final JsonObject image = new JsonObject();
        image.put("sources", new JsonArray());
        final JsonObject avatar = new JsonObject();
        avatar.put("image", image);

        assertEquals("https://example.com/fallback.jpg",
                extractor(entity(avatar, "https://example.com/fallback.jpg"))
                        .getUploaderAvatarUrl());
    }

    @Test
    void missingBothSourcesAndAuthorThumbnailReportsParsingError() {
        assertThrows(ParsingException.class,
                () -> extractor(entity(null, null)).getUploaderAvatarUrl());
    }

    @Test
    void missingAuthorAndAvatarReportsParsingError() {
        assertThrows(ParsingException.class,
                () -> extractor(new JsonObject()).getUploaderAvatarUrl());
    }

    private static JsonObject source(final String url) {
        final JsonObject source = new JsonObject();
        source.put("url", url);
        return source;
    }

    private static JsonObject entity(final JsonObject avatar, final String fallbackUrl) {
        final JsonObject author = new JsonObject();
        if (fallbackUrl != null) {
            author.put("avatarThumbnailUrl", fallbackUrl);
        }
        final JsonObject entity = new JsonObject();
        if (avatar != null) {
            entity.put("avatar", avatar);
        }
        entity.put("author", author);
        return entity;
    }

    private static YoutubeCommentsEUVMInfoItemExtractor extractor(final JsonObject entity) {
        return new YoutubeCommentsEUVMInfoItemExtractor(
                new JsonObject(),
                null,
                entity,
                new JsonObject(),
                "https://www.youtube.com/watch?v=test",
                TimeAgoPatternsManager.getTimeAgoParserFor(Localization.DEFAULT));
    }
}
