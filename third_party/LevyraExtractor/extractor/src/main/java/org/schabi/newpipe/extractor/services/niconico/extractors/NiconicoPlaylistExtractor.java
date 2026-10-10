package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.util.regex.Pattern;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.services.niconico.NiconicoService.*;

public class NiconicoPlaylistExtractor extends PlaylistExtractor {
    JsonObject data;

    public NiconicoPlaylistExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        String playlistId = getLinkHandler().getUrl().split("/mylist/")[1].split(Pattern.quote("?"))[0];
        final String apiUrl = MYLIST_URL + playlistId + "?pageSize=100&page=1";
        String response = downloader.get(apiUrl, getMylistHeaders()).responseBody();
        try {
            data = JsonParser.object().from(response).getObject("data").getObject("mylist");
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return data.getString("name");
    }

    private StreamInfoItemsCollector getCommittedCollector(JsonArray items){
        final StreamInfoItemsCollector collector
                = new StreamInfoItemsCollector(getServiceId());
        for (int i = 0; i< items.size(); i++) {
            collector.commit(new NiconicoPlaylistContentItemExtractor(items.getObject(i)));
        }
        return collector;
    }
    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        JsonArray items = data.getArray("items");
        if(items.size() == 0){
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        String playlistId = getLinkHandler().getUrl().split("/mylist/")[1].split(Pattern.quote("?"))[0];
        final String nextPage = MYLIST_URL + playlistId + "?pageSize=100&page=2";
        return new InfoItemsPage<>(getCommittedCollector(items), new Page(nextPage));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        String response = getDownloader().get(page.getUrl(), getMylistHeaders()).responseBody();
        try {
            data = JsonParser.object().from(response).getObject("data").getObject("mylist");
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
        JsonArray items = data.getArray("items");
        if(items.size() == 0){
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        String currentPageString = page.getUrl().split("page=")[page.getUrl().split("page=").length-1];
        int currentPage = Integer.parseInt(currentPageString);
        String nextPage = page.getUrl().replace(String.format("page=%s", currentPageString), String.format("page=%s", String.valueOf(currentPage + 1)));
        return new InfoItemsPage<>(getCommittedCollector(items), new Page(nextPage));
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        JsonObject owner = data.getObject("owner");
        if(owner.getString("ownerType").equals("user")){
            return USER_URL + owner.getString("id");
        } else {
            return CHANNEL_URL + owner.getString("id");
        }
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return data.getObject("owner").getString("name");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return data.getObject("owner").getString("iconUrl");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return data.getLong("totalItemCount");
    }
}
