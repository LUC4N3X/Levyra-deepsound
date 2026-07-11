package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.InfoItemsCollector;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;

public class BilibiliPlaylistExtractor extends PlaylistExtractor {
    public JsonObject data;
    public String type;
    private JsonObject userData;

    public BilibiliPlaylistExtractor(StreamingService service, ListLinkHandler linkHandler) {
        super(service, linkHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        try {
            data = JsonParser.object().from(getDownloader().get(getLinkHandler().getUrl(), getHeaders(getOriginalUrl())).responseBody());
            if (getLinkHandler().getUrl().contains(GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL)) {
                type = "seasons_archives";
            } else if (getLinkHandler().getUrl().contains(GET_SERIES_BASE_URL)) {
                type = "series";
            } else if (getLinkHandler().getUrl().contains(GET_PARTITION_URL)) {
                type = "partition";
                return;
            }
            data = data.getObject("data");
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        return getLinkHandler().getUrl().split("name=")[1].split("&")[0];
    }

    @Nonnull
    @Override
    public InfoItemsPage<StreamInfoItem> getInitialPage() throws IOException, ExtractionException {
        if (type.equals("partition")) {
            InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> collector = new StreamInfoItemsCollector(getServiceId());
            JsonArray relatedArray = data.getArray("data");
            for (int i = 0; i < relatedArray.size(); i++) {
                collector.commit(
                        new BilibiliRelatedInfoItemExtractor(
                                relatedArray.getObject(i), getLinkHandler().getUrl().split("bvid=")[1].split("&")[0],
                                URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8"),
                                String.valueOf(i + 1), getUploaderName(), null));
            }
            return new InfoItemsPage<>(collector, null);
        }
        return getPage(new Page(getUrl() + "&username=" + getUploaderName(), getDefaultCookies()));
    }

    @Override
    public InfoItemsPage<StreamInfoItem> getPage(Page page) throws IOException, ExtractionException {
        // Partitions have only 1 page
        type = getLinkHandler().getUrl().contains("seasons_archives") ? "seasons_archives" : "archives";
        try {
            if (!(page.getUrl().contains("pn=1") || page.getUrl().contains("page_num=1"))) {
                data = JsonParser.object().from(getDownloader().get(page.getUrl(), getHeaders(getOriginalUrl())).responseBody()).getObject("data");
            }
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
        JsonArray results = data.getArray("archives");
        if (results.size() == 0) {
            return new InfoItemsPage<>(new StreamInfoItemsCollector(getServiceId()), null);
        }
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(getServiceId());
        for (int i = 0; i < results.size(); i++) {
            collector.commit(new BilibiliChannelInfoItemWebAPIExtractor(results.getObject(i), getUploaderName(), getUploaderAvatarUrl()));
        }
        return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(page.getUrl(), type.equals("seasons_archives") ? "page_num" : "pn", 1), getDefaultCookies()));
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderUrl=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return BilibiliChannelLinkHandlerFactory.baseUrl + utils.getMidFromRecordApiUrl(getLinkHandler().getUrl());
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if (type.equals("partition")) {
            return getLinkHandler().getUrl().split("uploaderName=")[1].split("&")[0];
        }
        return getUserInfoLazily().getObject("data").getObject("card").getString("name", "[Unknown]");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("uploaderAvatar=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        }
        return getUserInfoLazily().getObject("data").getObject("card").getString("face").replace("http:", "https:");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        if (type.equals("partition")) {
            return data.getArray("data").size();
        }
        return data.getObject("page").getLong("total");
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (type.equals("partition")) {
            try {
                return URLDecoder.decode(getLinkHandler().getUrl().split("thumbnail=")[1].split("&")[0], "UTF-8");
            } catch (UnsupportedEncodingException e) {
                return null;
            }
        } else {
            return data.getObject("meta").getString("cover");
        }
    }

    // Get user metadata JSON lazily, without passible NPE.
    private JsonObject getUserInfoLazily() {
        if (userData != null) return userData;
        try {
            userData = JsonParser.object().from(getDownloader().get(getUserInfoRequestUrl(), getHeaders(getOriginalUrl())).responseBody());
        } catch (Exception e) {
            userData = new JsonObject();
        }
        return userData;
    }

    private String getUserInfoRequestUrl() {
        String uid = null;
        // Get from JSON directly
        if (data != null) {
            uid = data.getObject("meta").getString("mid");
        }
        // Use fallback
        if (uid == null) uid = utils.getMidFromRecordApiUrl(getLinkHandler().getUrl());
        return QUERY_USER_INFO_URL + uid;
    }
}
