package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.json.JSONException;
import org.json.JSONObject;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.parser.Parser;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.kiosk.KioskExtractor;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;

import javax.annotation.Nonnull;

public class NiconicoTrendExtractor extends KioskExtractor<StreamInfoItem> {
    private JsonArray data;
    private Document document;

    public NiconicoTrendExtractor(final StreamingService streamingService,
                                  final ListLinkHandler linkHandler, final String kioskId) {
        super(streamingService, linkHandler, kioskId);
    }

    @Override
    public void onFetchPage(final @Nonnull Downloader downloader)
            throws IOException, ExtractionException {
        switch (getId()){
            case "Recommended Lives":
                try {
                    data = JsonParser.object().from(downloader.get(getUrl(), getExtractorLocalization()).responseBody()).getObject("data").getArray("values");
                    return ;
                } catch (JsonParserException e) {
                    throw new RuntimeException(e);
                }
            case "Trending":
            default:
                document = Jsoup.parse(getDownloader().get(NiconicoService.DAILY_TREND_URL, getExtractorLocalization()).responseBody());
                return;
            case "Top Lives":
                document = Jsoup.parse(downloader.get(getUrl(), getExtractorLocalization()).responseBody());
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        switch (getId()){
            case "Recommended Lives":
                for(int i = 0; i< data.size(); i++){
                    collector.commit(new NiconicoLiveRecommendVideoExtractor(data.getObject(i), null,  null));
                }
                break;
            case "Trending":
            default:
                final Element data = document.select("meta[name=server-response]").first();
                String escapedContent = data.attr("content");

                // Properly unescape HTML entities
                String unescapedContent = Parser.unescapeEntities(escapedContent, false);
                try {
                    JsonArray dataJson = JsonParser.object().from(unescapedContent).getObject("data")
                            .getObject("response").getObject("$getTeibanRanking").getObject("data")
                            .getArray("items");
                    for(int i = 0; i< dataJson.size(); i++) {
                        collector.commit(new NiconicoSeriesContentItemExtractor(dataJson.getObject(i)));
                    }
                } catch (Exception e) {
                    throw new ParsingException(e.getMessage());
                }

                break;
            case "Top Lives":
                final Elements dataArray = document.select("[class^=___rk-program-card___]");
                for (final Element e : dataArray) {
                    collector.commit(new NiconicoTopLivesInfoItemExtractor(e));
                }
                break;
        }
        if (ServiceList.NicoNico.getFilterTypes().contains("recommendations")) {
            collector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
        }
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return InfoItemsPage.emptyPage();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return getId();
    }
}
