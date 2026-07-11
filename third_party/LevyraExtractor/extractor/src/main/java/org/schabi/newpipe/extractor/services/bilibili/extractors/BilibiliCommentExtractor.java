package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.services.bilibili.utils;

import javax.annotation.Nonnull;

import java.io.IOException;
import java.util.LinkedHashMap;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.FETCH_COMMENTS_URL;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.WWW_REFERER;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getDefaultCookies;
import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.getUserAgentHeaders;

public class BilibiliCommentExtractor extends CommentsExtractor {
    JsonObject data = new JsonObject();

    public BilibiliCommentExtractor(StreamingService service, ListLinkHandler uiHandler) {
        super(service, uiHandler);
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        try {
            String url = getUrl();
            String response = isRepliesOfComment(url)
                    ? getDownloader().get(url, getUserAgentHeaders(getLinkHandler().getOriginalUrl())).responseBody()
                    : getDownloader().get(url).responseBody();
            data = JsonParser.object().from(response);
            data = data.getObject("data");
        } catch (JsonParserException e) {
            e.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage() throws IOException, ExtractionException {
        return getPage(new Page(getUrl(), getDefaultCookies()));
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(Page page) throws IOException, ExtractionException {
        String pageUrl = page.getUrl();
        String initialUrl = getUrl();

        JsonArray results;
        if (pageUrl.equals(initialUrl)) { // first page with top comments
            results = data.getArray("top_replies");
            for (int i = 0; i < results.size(); i++) {
                results.getObject(i).put("isTop", true);
            }
            results.addAll(data.getArray("replies"));
        } else { // other pages
            try {
                final String responseJson =
                        isRepliesOfComment(pageUrl)
                                ? getDownloader().get(pageUrl, getUserAgentHeaders(WWW_REFERER)).responseBody()
                                : getDownloader().get(pageUrl).responseBody();
                data = JsonParser.object().from(responseJson).getObject("data");
            } catch (JsonParserException e) {
                e.printStackTrace();
            }
            results = data.getArray("replies");
        }

        if (results == null || results.isEmpty()) { // empty page
            return new InfoItemsPage<>(new CommentsInfoItemsCollector(getServiceId()), null);
        }

        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(getServiceId());
        for (int i = 0; i < results.size(); i++) {
            collector.commit(new BilibiliCommentsInfoItemExtractor(results.getObject(i)));
        }

        if (!isRepliesOfComment(pageUrl)) { //comments
            if (data.getObject("cursor").getBoolean("is_end")) {
                return new InfoItemsPage<>(collector, null);
            }
            //data.cursor.pagination_reply.next_offset
            String offset = data.getObject("cursor").getObject("pagination_reply").getString("next_offset");
            return new InfoItemsPage<>(collector, new Page(utils.getWbiResult(FETCH_COMMENTS_URL, buildNextPageParam(initialUrl, offset)), getDefaultCookies()));
        } else { //replies
            if (9 > results.size() && pageUrl.contains("pn=1")) {
                return new InfoItemsPage<>(collector, null);
            }
            return new InfoItemsPage<>(collector, new Page(utils.getNextPageFromCurrentUrl(pageUrl, "pn", 1), getDefaultCookies()));
        }
    }

    @Override
    public boolean isCommentsDisabled() throws ExtractionException {
        return getId().equals("LIVE");
    }

    private static boolean isRepliesOfComment(String requestUrl) {
        return !requestUrl.contains(FETCH_COMMENTS_URL);
    }

    private static LinkedHashMap<String, String> buildNextPageParam(String url, String offset) {
        return new LinkedHashMap<String, String>() {{
            put("oid", url.split("oid=")[1].split("&")[0]);
            put("type", "1");
            put("mode", "3");
            put("pagination_str", "{\"offset\":\"" + StringEscapeUtils.escapeJson(offset) + "\"}");
            put("plat", "1");
            put("web_location", "1315875");
        }};
    }

}
