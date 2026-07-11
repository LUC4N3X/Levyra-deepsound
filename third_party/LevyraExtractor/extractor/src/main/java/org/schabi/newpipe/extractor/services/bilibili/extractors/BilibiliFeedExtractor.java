package org.schabi.newpipe.extractor.services.bilibili.extractors;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getHeaders;

import java.io.IOException;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import javax.annotation.Nonnull;

public class BilibiliFeedExtractor extends KioskExtractor<StreamInfoItem> {
    public BilibiliFeedExtractor(StreamingService streamingService, ListLinkHandler linkHandler, String kioskId) {
        super(streamingService, linkHandler, kioskId);
    }

    private JsonObject response = new JsonObject();
    private Document document;

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return getId();
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        JsonArray results;
        switch (getId()) {
            case "Recommended Videos":
                results = response.getObject("data").getArray("item");
                for (int i = 0; i < results.size(); i++) {
                    collector.commit(new BilibiliRecommendedVideosInfoItemExtractor(results.getObject(i)));
                }
                break;
            case "Recommended Lives":
                results = response.getObject("data").getArray("list");
                for (int i = 0; i < results.size(); i++) {
                    collector.commit(new BilibiliRecommendLiveInfoItemExtractor(results.getObject(i)));
                }
                break;
            case "Top 100":
                results = response.getObject("data").getArray("list");
                for (int i = 0; i < results.size(); i++) {
                    collector.commit(new BilibiliTrendingInfoItemExtractor(results.getObject(i)));
                }
                break;
        }
        if (ServiceList.BiliBili.getFilterTypes().contains("recommendations")) {
            collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        return null;
    }

    @Override
    public void onFetchPage(Downloader downloader) throws IOException, ExtractionException {
        switch (getId()) {
            case "Recommended Videos":
            default:
                try {
                    response = JsonParser.object().from(getDownloader().get("https://api.bilibili.com/x/web-interface/index/top/rcmd?fresh_type=3", getHeaders(getOriginalUrl())).responseBody());
                } catch (JsonParserException e) {
                    e.printStackTrace();
                }
                break;
            case "Top 100":
                try {
                    response = JsonParser.object().from(downloader.get(getUrl(), getHeaders(getOriginalUrl())).responseBody());
                } catch (JsonParserException e) {
                    throw new RuntimeException(e);
                }
                break;
            case "Recommended Lives":
                try {
                    response = JsonParser.object().from(downloader.get(getUrl() + "&page=1", getHeaders(getOriginalUrl())).responseBody());
                } catch (JsonParserException e) {
                    throw new RuntimeException(e);
                }
                break;
        }
    }

}
