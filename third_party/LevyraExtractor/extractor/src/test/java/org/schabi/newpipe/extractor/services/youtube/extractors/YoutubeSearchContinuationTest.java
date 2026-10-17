package org.schabi.newpipe.extractor.services.youtube.extractors;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor.InfoItemsPage;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItem;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper;
import org.schabi.newpipe.extractor.services.youtube.search.filter.YoutubeFilters;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class YoutubeSearchContinuationTest {
    private static final String CLIENT_VERSION = "2.20260101.00.00";
    private static final String SEARCH_URL =
            "https://www.youtube.com/youtubei/v1/search?prettyPrint=false";

    private Downloader previousDownloader;
    private Localization previousLocalization;
    private ContentCountry previousCountry;

    @BeforeEach
    void setUp() {
        previousDownloader = NewPipe.getDownloader();
        previousLocalization = NewPipe.getPreferredLocalization();
        previousCountry = NewPipe.getPreferredContentCountry();
        YoutubeParsingHelper.resetClientVersion();
    }

    @AfterEach
    void tearDown() {
        YoutubeParsingHelper.resetClientVersion();
        NewPipe.init(previousDownloader, previousLocalization, previousCountry);
    }

    @Test
    void continuationsSendClientHeadersAndAdvanceAcrossPages() throws Exception {
        final SearchDownloader downloader = new SearchDownloader(
                continuationResponse(videoRenderer("vid11111111"), "token-2"),
                continuationResponse(videoRenderer("vid22222222"), "token-3"),
                continuationResponse(videoRenderer("vid33333333"), null));
        NewPipe.init(downloader, Localization.DEFAULT, ContentCountry.DEFAULT);
        final YoutubeSearchExtractor extractor = extractor();

        Page page = new Page(SEARCH_URL, "token-1");
        final List<String> urls = new ArrayList<>();
        for (int index = 0; index < 3; index++) {
            final InfoItemsPage<InfoItem> result = extractor.getPageInternal(page);
            urls.addAll(result.getItems().stream().map(InfoItem::getUrl)
                    .collect(Collectors.toList()));
            page = result.getNextPage();
            if (index < 2) {
                assertNotNull(page);
            }
        }

        assertNull(page);
        assertEquals(3, urls.size());
        assertEquals(3, urls.stream().distinct().count());
        assertEquals(3, downloader.searchRequests.size());
        for (final Request request : downloader.searchRequests) {
            assertEquals(SEARCH_URL, request.url());
            final Map<String, List<String>> headers = request.headers();
            assertEquals(List.of("1"), headers.get("X-YouTube-Client-Name"));
            assertEquals(List.of(CLIENT_VERSION), headers.get("X-YouTube-Client-Version"));
            assertEquals(List.of("application/json"), headers.get("Content-Type"));
        }
        assertTrue(new String(downloader.searchRequests.get(1).dataToSend(),
                StandardCharsets.UTF_8).contains("\"continuation\":\"token-2\""));
    }

    @Test
    void emptyOrRepeatedContinuationTokenEndsPagination() throws Exception {
        NewPipe.init(new SearchDownloader(
                        continuationResponse(videoRenderer("vid11111111"), "token-1"),
                        continuationResponse(videoRenderer("vid22222222"), "")),
                Localization.DEFAULT, ContentCountry.DEFAULT);
        final YoutubeSearchExtractor extractor = extractor();

        assertNull(extractor.getPageInternal(new Page(SEARCH_URL, "token-1")).getNextPage());
        assertNull(extractor.getPageInternal(new Page(SEARCH_URL, "token-1")).getNextPage());
    }

    @Test
    void continuationWithoutItemsKeepsNextPage() throws Exception {
        NewPipe.init(new SearchDownloader(continuationResponse(null, "token-2")),
                Localization.DEFAULT, ContentCountry.DEFAULT);

        final InfoItemsPage<InfoItem> result =
                extractor().getPageInternal(new Page(SEARCH_URL, "token-1"));

        assertTrue(result.getItems().isEmpty());
        assertEquals("token-2", result.getNextPage().getId());
    }

    @Test
    void parsesLockupShortsAndGridShelfFormatsWithBoundedNesting() throws Exception {
        final String items = String.join(",",
                "{\"lockupViewModel\":{\"contentType\":\"LOCKUP_CONTENT_TYPE_VIDEO\","
                        + "\"contentId\":\"vid44444444\",\"metadata\":{\"lockupMetadataViewModel\":"
                        + "{\"title\":{\"content\":\"Lockup video\"}}}}}",
                "{\"shortsLockupViewModel\":{\"overlayMetadata\":{\"primaryText\":"
                        + "{\"content\":\"Short\"}},\"onTap\":{\"innertubeCommand\":"
                        + "{\"reelWatchEndpoint\":{\"videoId\":\"sho55555555\"}}}}}",
                "{\"gridShelfViewModel\":{\"contents\":["
                        + "{\"lockupViewModel\":{\"contentType\":\"LOCKUP_CONTENT_TYPE_PODCAST\","
                        + "\"contentId\":\"PLpodcast000\",\"metadata\":{\"lockupMetadataViewModel\":"
                        + "{\"title\":{\"content\":\"Podcast\"}}}}},"
                        + "{\"gridShelfViewModel\":{\"contents\":[" + videoRenderer("vid66666666")
                        + "]}}]}}",
                "{\"lockupViewModel\":{\"contentType\":\"LOCKUP_CONTENT_TYPE_CHANNEL\"}}",
                "{\"gridShelfViewModel\":{}}",
                "\"not-an-object\"");
        NewPipe.init(new SearchDownloader(continuationResponse(items, null)),
                Localization.DEFAULT, ContentCountry.DEFAULT);

        final List<InfoItem> result =
                extractor().getPageInternal(new Page(SEARCH_URL, "token-1")).getItems();

        final List<StreamInfoItem> streams = result.stream()
                .filter(StreamInfoItem.class::isInstance).map(StreamInfoItem.class::cast)
                .collect(Collectors.toList());
        assertEquals(2, streams.size());
        assertEquals("https://www.youtube.com/watch?v=vid44444444", streams.get(0).getUrl());
        assertFalse(streams.get(0).isShortFormContent());
        assertTrue(streams.get(1).isShortFormContent());
        assertEquals(1, result.stream().filter(PlaylistInfoItem.class::isInstance).count());
        assertTrue(result.stream().noneMatch(item -> item.getUrl().contains("vid66666666")));
    }

    @Nonnull
    private static YoutubeSearchExtractor extractor() throws Exception {
        final FilterItem[] items = ServiceList.YouTube.getSearchQHFactory()
                .getAvailableContentFilter()
                .getFilterGroups()[0].filterItems;
        final FilterItem all = Arrays.stream(items)
                .filter(item -> item instanceof YoutubeFilters.YoutubeContentFilterItem)
                .findFirst().orElseThrow();
        return (YoutubeSearchExtractor) ServiceList.YouTube.getSearchExtractor(
                ServiceList.YouTube.getSearchQHFactory().fromQuery("query",
                        List.of(all), Collections.emptyList()));
    }

    @Nonnull
    private static String videoRenderer(@Nonnull final String videoId) {
        return "{\"videoRenderer\":{\"videoId\":\"" + videoId
                + "\",\"title\":{\"runs\":[{\"text\":\"" + videoId + "\"}]}}}";
    }

    @Nonnull
    private static String continuationResponse(final String items, final String nextToken) {
        final StringBuilder json = new StringBuilder(
                "{\"onResponseReceivedCommands\":[{\"appendContinuationItemsAction\":"
                        + "{\"continuationItems\":[{\"itemSectionRenderer\":{\"contents\":[");
        if (items != null) {
            json.append(items);
        }
        json.append("]}}");
        if (nextToken != null) {
            json.append(",{\"continuationItemRenderer\":{\"continuationEndpoint\":"
                    + "{\"continuationCommand\":{\"token\":\"").append(nextToken)
                    .append("\"}}}}");
        }
        return json.append("]}}]}").toString();
    }

    private static final class SearchDownloader extends Downloader {
        private final String[] searchBodies;
        private final List<Request> searchRequests = new ArrayList<>();

        private SearchDownloader(final String... searchBodies) {
            this.searchBodies = searchBodies;
        }

        @Override
        public Response execute(@Nonnull final Request request) throws IOException {
            if (request.url().contains("sw.js_data")) {
                return new Response(200, "OK", Collections.emptyMap(), ")]}'\n[[null,null,[[["
                        + String.join(",", Collections.nCopies(16, "null"))
                        + ",\"" + CLIENT_VERSION + "\"]]]]]", null, request.url());
            }
            if (request.url().contains("/youtubei/v1/search")) {
                final int index = Math.min(searchRequests.size(), searchBodies.length - 1);
                searchRequests.add(request);
                return new Response(200, "OK",
                        Map.of("Content-Type", List.of("application/json")),
                        searchBodies[index], null, request.url());
            }
            throw new IOException("Unexpected request: " + request.url());
        }

        @Override
        public CancellableCall executeAsync(@Nonnull final Request request,
                                            final AsyncCallback callback) {
            throw new UnsupportedOperationException();
        }
    }
}
