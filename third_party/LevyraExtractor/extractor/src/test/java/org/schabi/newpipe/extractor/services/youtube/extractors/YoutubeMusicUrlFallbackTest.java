package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class YoutubeMusicUrlFallbackTest {
    private static final JsonArray EMPTY_DESCRIPTION = new JsonArray();

    @Test
    void overlayWatchEndpointRecoversMissingPlaylistItemVideoId() throws Exception {
        final JsonObject item = JsonParser.object().from("{"
                + "\"overlay\":{\"musicItemThumbnailOverlayRenderer\":{\"content\":{"
                + "\"musicPlayButtonRenderer\":{\"playNavigationEndpoint\":{"
                + "\"watchEndpoint\":{\"videoId\":\"overlay1234\"}}}}}}"
                + "}");

        final YoutubeMusicSongOrVideoInfoItemExtractor extractor =
                new YoutubeMusicSongOrVideoInfoItemExtractor(item, EMPTY_DESCRIPTION, "songs");

        assertEquals("https://music.youtube.com/watch?v=overlay1234", extractor.getUrl());
    }

    @Test
    void titleNavigationEndpointIsTheLastVideoIdFallback() throws Exception {
        final JsonObject item = JsonParser.object().from("{"
                + "\"flexColumns\":[{\"musicResponsiveListItemFlexColumnRenderer\":{"
                + "\"text\":{\"runs\":[{\"navigationEndpoint\":{"
                + "\"watchEndpoint\":{\"videoId\":\"title123456\"}}}]}}}]"
                + "}");

        final YoutubeMusicSongOrVideoInfoItemExtractor extractor =
                new YoutubeMusicSongOrVideoInfoItemExtractor(item, EMPTY_DESCRIPTION, "songs");

        assertEquals("https://www.youtube.com/watch?v=title123456", extractor.getUrl());
    }
}
