package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliChannelExtractor;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;

import java.io.IOException;

import javax.annotation.Nonnull;

public class NiconicoChannelTabExtractor extends ChannelTabExtractor {
    public NiconicoChannelTabExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {

    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        if(getLinkHandler().getContentFilters().get(0).getName() == ChannelTabs.VIDEOS) {
            NiconicoUserExtractor extractor = new NiconicoUserExtractor(getService(), getLinkHandler());
            extractor.onFetchPage(getDownloader());
            return (InfoItemsPage<InfoItem>) (InfoItemsPage<?>) extractor.getInitialPage();
        }
        return getPage(new Page(getLinkHandler().getUrl()));
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(Page page) throws IOException, ExtractionException {
        if(getLinkHandler().getContentFilters().get(0).getName() == ChannelTabs.VIDEOS) {
            NiconicoUserExtractor extractor = new NiconicoUserExtractor(getService(), getLinkHandler());
            return (InfoItemsPage<InfoItem>) (InfoItemsPage<?>) extractor.getPage(page);
        }
        try {
            JsonObject data = JsonParser.object().from(getDownloader().get(page.getUrl(), NiconicoService.getMylistHeaders()).responseBody()).getObject("data");
            final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
            if(getTab().equals(ChannelTabs.LIVESTREAMS)){
                JsonArray datalist = data.getArray("programsList");
                for(int i = 0; i< datalist.size(); i++){
                    collector.commit(new NiconicoLiveHistoryInfoItemExtractor(datalist.getObject(i)));
                }
                if(datalist.size() == 0){
                    return new InfoItemsPage<>(collector, null);
                }
                String currentPageString = page.getUrl().split("offset=")[1].split("&")[0];
                int currentPage = Integer.parseInt(currentPageString);
                String nextPage = page.getUrl().replace(String.format("offset=%s", currentPage), String.format("offset=%s", String.valueOf(currentPage + 10)));
                if (ServiceList.NicoNico.getFilterTypes().contains("channels")) {
                    collector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
                }
                return new InfoItemsPage<>(collector, new Page(nextPage));
            }else if(getTab().equals(ChannelTabs.ALBUMS)){
                JsonArray datalist = data.getArray("items");
                for(int i = 0; i< datalist.size(); i++){
                    collector.commit(new NiconicoSeriesInfoItemExtractor(datalist.getObject(i), getUrl().split("&name=")[1]));
                }
                if(datalist.size() == 0){
                    return new InfoItemsPage<>(collector, null);
                }
                String currentPageString = page.getUrl().split("page=")[1].split("&")[0];
                int currentPage = Integer.parseInt(currentPageString);
                String nextPage = page.getUrl().replace(String.format("page=%s", currentPage), String.format("page=%s", String.valueOf(currentPage + 1)));
                if (ServiceList.NicoNico.getFilterTypes().contains("channels")) {
                    collector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
                }
                return new InfoItemsPage<>(collector, new Page(nextPage));
            }else{
                JsonArray datalist = data.getArray("mylists");
                for(int i = 0; i< datalist.size(); i++){
                    collector.commit(new NiconicoPlaylistInfoItemExtractor(datalist.getObject(i)));
                }
                if (ServiceList.NicoNico.getFilterTypes().contains("channels")) {
                    collector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
                }
                return new InfoItemsPage<>(collector, null);
            }
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
    }
}
