package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;

import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class YoutubeChannelReleasesTest {
    @Test
    void releasesTabRoundTripsThroughAlbumsTab() throws Exception {
        assertEquals("/releases", YoutubeChannelTabLinkHandlerFactory.getUrlSuffix(
                ChannelTabs.ALBUMS));
        assertEquals(ChannelTabs.ALBUMS, YoutubeChannelTabLinkHandlerFactory.getInstance()
                .fromUrl("https://www.youtube.com/channel/UCabcdefghijklmnopqrstuv/releases")
                .getContentFilters().get(0).getName());
    }

    @Test
    void releaseAlbumUsesCustomThumbnailAndPlainVideoCount() throws Exception {
        final YoutubeMixOrPlaylistInfoItemExtractor extractor = extractor("{"
                + "\"playlistId\":\"OLAK5uy_album0000000000000000000000000\","
                + "\"title\":{\"simpleText\":\"Album\"},\"videoCount\":\"11\","
                + "\"thumbnailRenderer\":{\"playlistCustomThumbnailRenderer\":{\"thumbnail\":"
                + "{\"thumbnails\":[{\"url\":\"https://i.ytimg.com/small.jpg\"},"
                + "{\"url\":\"https://i.ytimg.com/large.jpg\"}]}}}}");

        assertEquals("Album", extractor.getName());
        assertEquals("https://i.ytimg.com/large.jpg", extractor.getThumbnailUrl());
        assertEquals(11, extractor.getStreamCount());
        assertEquals(PlaylistInfo.PlaylistType.NORMAL, extractor.getPlaylistType());
    }

    @Test
    void streamCountHandlesLocalizedMissingAndOpenEndedCounts() throws Exception {
        assertEquals(12, extractor("{\"videoCountText\":{\"runs\":[{\"text\":\"12\"},"
                + "{\"text\":\" videos\"}]}}").getStreamCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN, extractor("{}").getStreamCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN,
                extractor("{\"videoCount\":\"many\"}").getStreamCount());
        assertEquals(ListExtractor.ITEM_COUNT_UNKNOWN,
                extractor("{\"videoCount\":\"99999999999999\"}").getStreamCount());
        assertEquals(ListExtractor.ITEM_COUNT_INFINITE,
                extractor("{\"videoCount\":\"50+\"}").getStreamCount());
        assertEquals(ListExtractor.ITEM_COUNT_INFINITE,
                extractor("{\"videoCountShortText\":{\"simpleText\":\"50+\"}}")
                        .getStreamCount());
        assertEquals(25, extractor("{\"videoCountShortText\":{\"simpleText\":\"25\"}}")
                .getStreamCount());
    }

    @Test
    void missingThumbnailFormatsStayOptional() throws Exception {
        assertNull(extractor("{\"thumbnail\":{\"thumbnails\":[]},"
                + "\"thumbnailRenderer\":{\"playlistCustomThumbnailRenderer\":{}}}")
                .getThumbnailUrl());
    }

    private static YoutubeMixOrPlaylistInfoItemExtractor extractor(final String json)
            throws Exception {
        final JsonObject item = JsonParser.object().from(json);
        return new YoutubeMixOrPlaylistInfoItemExtractor(item);
    }
}
