package org.schabi.newpipe.extractor.services.niconico.extractors;

import static org.schabi.newpipe.extractor.services.niconico.NiconicoService.CHANNEL_URL;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

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
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.utils.Parser;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;


public class NiconicoUserExtractor extends ChannelExtractor {
    private Document rss;
    private String uploaderName;
    private String uploaderUrl;
    private String uploaderAvatarUrl;
    private JsonObject info;
    private String channel_info;
    private int type; //0 user 1 channel

    public NiconicoUserExtractor(final StreamingService service,
                                 final ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(final @Nonnull Downloader downloader)
            throws IOException, ExtractionException {
        String url = getLinkHandler().getUrl();
        url += "/video?rss=2.0&page=1";
        rss = Jsoup.parse(getDownloader().get(url).responseBody());

        if(url.contains(CHANNEL_URL)){
            type = 1;
            final Document user = Jsoup.parse(getDownloader().get(
                    getLinkHandler().getUrl()).responseBody());
            channel_info = user.select("meta[name=description]").attr("content");
            uploaderName = user.select("meta[property=og:site_name]").attr("content");
            uploaderAvatarUrl = user.select("meta[property=og:image]").attr("content");
            uploaderUrl = getLinkHandler().getUrl();
            return ;
        }

        final Document user = Jsoup.parse(getDownloader().get(
                getLinkHandler().getUrl()).responseBody());

        try {
            info = JsonParser.object()
                    .from(user.getElementById("js-initial-userpage-data")
                            .attr("data-initial-data")).getObject("state");
            final JsonObject infoObj = info.getObject("userDetails").getObject("userDetails")
                    .getObject("user");
            uploaderName = infoObj.getString("nickname");
            uploaderUrl = getLinkHandler().getUrl();
            uploaderAvatarUrl = infoObj.getObject("icons").getString("large");
        } catch (final JsonParserException | NullPointerException e) {
            throw new ExtractionException("could not parse user information.");
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return uploaderName;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final StreamInfoItemsCollector streamInfoItemsCollector =
                new StreamInfoItemsCollector(getServiceId());

        final Elements arrays = rss.select("item");

        for (final Element e : arrays) {
            streamInfoItemsCollector.commit(new NiconicoTrendRSSExtractor(e, uploaderName,
                    uploaderUrl, uploaderAvatarUrl));
        }

        final String currentPageUrl = getLinkHandler().getUrl() + "/video?rss=2.0&page=1";
        if (ServiceList.NicoNico.getFilterTypes().contains("channels")) {
            streamInfoItemsCollector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
        }
        return new InfoItemsPage<>(streamInfoItemsCollector,
                getNextPageFromCurrentUrl(currentPageUrl));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("page does not contain an URL.");
        }

        final StreamInfoItemsCollector streamInfoItemsCollector =
                new StreamInfoItemsCollector(getServiceId());

        final Document response = Jsoup.parse(getDownloader().get(page.getUrl(),
                NiconicoService.LOCALE).responseBody());
        final Elements arrays = response.getElementsByTag("item");

        for (final Element e : arrays) {
            streamInfoItemsCollector.commit(new NiconicoTrendRSSExtractor(e, uploaderName,
                    uploaderUrl, uploaderAvatarUrl));
        }
        if (arrays.size() == 0) {
            return new InfoItemsPage<>(streamInfoItemsCollector,
                    null);
        }
        if (ServiceList.NicoNico.getFilterTypes().contains("channels")) {
            streamInfoItemsCollector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
        }
        return new InfoItemsPage<>(streamInfoItemsCollector,
                getNextPageFromCurrentUrl(page.getUrl()));
    }

    @Override
    public String getAvatarUrl() throws ParsingException {
        return uploaderAvatarUrl;
    }

    @Override
    public String getBannerUrl() throws ParsingException {
        // Niconico does not have user banner.
        return null;
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        if(type == 1){
            return -1;
        }
        return info.getObject("userDetails").getObject("userDetails")
                .getObject("user").getLong("followerCount");
    }

    @Override
    public String getDescription() throws ParsingException {
        if(type == 1){
            return channel_info;
        }
        return Jsoup.parse(info.getObject("userDetails").getObject("userDetails")
                .getObject("user").getString("description")).wholeText();
    }


    private Page getNextPageFromCurrentUrl(final String currentUrl)
            throws ParsingException {
        final String page = "&page=(\\d+?)";
        try {
            final int nowPage = Integer.parseInt(Parser.matchGroup1(page, currentUrl));
            return new Page(currentUrl.replace("&page=" + nowPage, "&page="
                    + (nowPage + 1)));
        } catch (final Parser.RegexException e) {
            throw new ParsingException("could not parse pager.");
        }
    }

    @Nonnull
    @Override
    public List<ListLinkHandler> getTabs() throws ParsingException {
        if(type == 1){
            return Collections.emptyList();
        }
        String id = getLinkHandler().getId().split("user/")[1];
        String mylists = String.format("https://nvapi.nicovideo.jp/v1/users/%s/mylists?sampleItemCount=3",id);
        String series = String.format("https://nvapi.nicovideo.jp/v1/users/%s/series?page=1&pageSize=100&name=%s",id, uploaderName);
        String lives = String.format("https://live.nicovideo.jp/front/api/v1/user-broadcast-history?providerId=%s&providerType=user&isIncludeNonPublic=false&offset=0&limit=10&withTotalCount=true"
        , id);
        return Arrays.asList(
                new ListLinkHandler(getUrl(), getUrl(), getLinkHandler().getId(),
                        Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ChannelTabs.VIDEOS)), null),
                new ListLinkHandler(mylists, mylists, getLinkHandler().getId(),
                        Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ChannelTabs.PLAYLISTS)), null),
                new ListLinkHandler(series, series, getLinkHandler().getId(),
                        Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ChannelTabs.ALBUMS)), null)
//                new ListLinkHandler(lives, lives, getLinkHandler().getId(),
//                        Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ChannelTabs.LIVESTREAMS)), null)
        );
    }
}
