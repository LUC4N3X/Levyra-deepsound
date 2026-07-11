package org.schabi.newpipe.extractor.services.niconico.extractors;

import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.Nonnull;

public class NiconicoSearchExtractor extends SearchExtractor {
    private JsonObject searchCollection;

    public NiconicoSearchExtractor(final StreamingService service,
                                   final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(final @Nonnull Downloader downloader)
            throws IOException, ExtractionException {
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPageInternal() throws IOException, ExtractionException {
        return getPage(new Page(getUrl()));
    }

    @Override
    public InfoItemsPage<InfoItem> getPageInternal(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("page does not contain an URL.");
        }

        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));

        final String response = getDownloader().get(
                page.getUrl(), headers, NiconicoService.LOCALE).responseBody();

        if(page.getUrl().contains(NiconicoService.SEARCH_URL)){
            Element serverResponse = Jsoup.parse(response).selectFirst("meta[name=server-response]");
            if (serverResponse == null) {
                throw new ParsingException("Could not find server-response meta tag");
            }
            try {
                JsonObject responseJson = null;
                responseJson = JsonParser.object().from(serverResponse.attr("content"));
                JsonObject data = responseJson.getObject("data").getObject("response")
                        .getObject("$getSearchVideoV2").getObject("data");

                final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

                for (Object item : data.getArray("items")) {
                    collector.commit(new NiconicoSearchContentItemExtractor((JsonObject) item));
                }

                if(data.getArray("items").isEmpty()){
                    return new InfoItemsPage<>(collector, null);
                }

                return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), "page", 1)));
            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }

        } else if (page.getUrl().contains(NiconicoService.LIVE_SEARCH_URL)) {
            Elements lives = Jsoup.parse(response).select("div.program-search-result").first()
                    .select("ul[class*=program-card-list] > li[class*=___program-card___]");
            final MultiInfoItemsCollector collector
                    = new MultiInfoItemsCollector(getServiceId());
            for (final Element e : lives) {
                collector.commit(new NiconicoLiveSearchInfoItemExtractor(e));
            }
            if(lives.size() == 0){
                return new InfoItemsPage<>(collector, null);
            }
            return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), "page", 1)));
        } else if (page.getUrl().contains(NiconicoService.PLAYLIST_SEARCH_API_URL)){
            final MultiInfoItemsCollector collector
                    = new MultiInfoItemsCollector(getServiceId());
            try {
                searchCollection = JsonParser.object().from(response).getObject("data");
                if (searchCollection.getArray("items").size() == 0) {
                    return new InfoItemsPage<>(collector, null);
                }
            } catch (final JsonParserException e) {
                throw new ParsingException("could not parse search results.");
            }
            for (Object item : searchCollection.getArray("items")) {
                collector.commit(new NiconicoPlaylistInfoItemExtractor((JsonObject) item));
            }
            return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), "page", 1)));
        }

        throw new ExtractionException("Failed to extract NicoNico search result, url: " + page.getUrl() + " , this should never happen");
    }
}
