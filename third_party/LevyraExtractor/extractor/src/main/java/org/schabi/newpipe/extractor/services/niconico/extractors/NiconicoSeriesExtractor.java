package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import javax.annotation.Nonnull;
import java.io.IOException;

public class NiconicoSeriesExtractor extends PlaylistExtractor {
    private JsonObject data;
    private String uploaderName;
    private String uploaderUrl;
    private String avatar;
    private int count;
    private String name;
    private int type = 0;

    public NiconicoSeriesExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        Document response = Jsoup.parse(getDownloader().get(getLinkHandler().getUrl(), Localization.DEFAULT).responseBody());
        type = 1;
        uploaderName = response.select("meta[property=profile:username]").attr("content");
        uploaderUrl = response.select("meta[property=og:url]").attr("content").split("/series/")[0];
        avatar = response.select("meta[property=og:image]").attr("content");
        count = Integer.parseInt(response.select("meta[property=og:title]").attr("content").split("（全")[1].split("件）")[0]);
        name = response.select("meta[property=og:description]").attr("content").split("の「")[1].split("（全")[0];
        try {
            data = JsonParser.object().from(response.select("div#js-initial-userpage-data").attr("data-initial-data"));
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return name;
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());

        try {
            JsonArray array = data.getArray("nvapi").getObject(0).getObject("body").getObject("data").getArray("items");
            for (int i = 0; i < array.size(); i++) {
                collector.commit(new NiconicoPlaylistContentItemExtractor(array.getObject(i)));
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        return null;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return uploaderUrl;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return uploaderName;
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return avatar;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return count;
    }
}
