package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.*;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import javax.annotation.Nonnull;

public class NiconicoWatchDataCache {
    String lastId;
    Response response;
    Document page;
    JsonObject watchData;
    WatchDataType watchDataType;
    private String threadServer;
    private String threadId;
    private long startAt;
    private String streamCookie="";

    public enum WatchDataType {
        LOGIN(1),
        GUEST(0);

        private final int value;

        WatchDataType(final int value) {
            this.value = value;
        }

        public int getValue() {
            return value;
        }
    }
    public void invalidate(){
        lastId = null;
    }

    @Nonnull
    public JsonObject refreshAndGetWatchData(final Downloader downloader,
                                             @Nonnull final String id) throws ExtractionException {
        if (lastId != null && lastId.equals(id)) {
            return watchData;
        }

        String url = NiconicoService.WATCH_URL + id;
        if(id.contains("live.nicovideo.jp")){
            url = id;
        }
        try {
            HashMap<String, List<String>> headers = new HashMap<>();
            headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (compatible; Googlebot/2.1; +http://www.google.com/bot.html)"));
            if(ServiceList.NicoNico.hasTokens()){
                headers.put("Cookie", Collections.singletonList(ServiceList.NicoNico.getTokens()));
            }
            response = downloader.get(url, headers, NiconicoService.LOCALE);
        } catch (final IOException | ReCaptchaException e) {
            throw new ExtractionException("Could not get response.", e);
        }
        page = Jsoup.parse(response.responseBody());
        try {
            Element element = page.select("meta[name=\"server-response\"]").first();
            if (element == null) {
                watchDataType = WatchDataType.LOGIN; //need login
                if(response.responseBody().contains("チャンネル会員専用動画")){
                    throw new PaidContentException("Channel member limited videos");
                } else if (response.responseBody().contains("地域と同じ地域からのみ視聴")) {
                    throw new GeographicRestrictionException("Sorry, this video can only be viewed in the same region where it was uploaded.");
                } else if (response.responseBody().contains("この動画を視聴するにはログインが必要です。")) {
                    throw new PaidContentException("This video requires login to view.");
                }
                throw new ContentNotAvailableException(page.select("p.fail-message").text());
            } else {
                watchDataType = WatchDataType.GUEST;
            }
            if (watchDataType == WatchDataType.LOGIN) {
                watchData = JsonParser.object().from(
                        page.getElementsByClass("content WatchAppContainer")
                                .attr("data-video"));
            } else {
                watchData = JsonParser.object().from(
                        element.attr("content")).getObject("data").getObject("response");
            }
        } catch (JsonParserException e) {
            throw new ParsingException("Failed to parse content");
        }

        if (watchData.getString("errorCode") != null && watchData.getString("errorCode").equals("FORBIDDEN")) { //TODO: also for other types such as member limited vids
            switch (watchData.getString("reasonCode")) {
                case "DOMESTIC_VIDEO":
                    throw new GeographicRestrictionException("This video is only available in Japan");
                case "HARMFUL_VIDEO":
                    throw new NeedLoginException("This content need an account to view");
                default:
                    throw new ContentNotAvailableException(watchData.getString("reasonCode"));
            }
        } else if (watchData.getString("okReason") != null && watchData.getString("okReason").equals("PAYMENT_PREVIEW_SUPPORTED")) {
            if (watchData.getObject("payment").getObject("video").getBoolean("isPremium") == true) {
                throw new PaidContentException("This content is limited to premium users");
            } else if (watchData.getObject("payment").getObject("video").getString("billingType").equals("member_only")) {
                throw new PaidContentException("This content is limited to channel members");
            }
        }

        lastId = id;
        return watchData;
    }

    public JsonObject getLastWatchData() {
        return watchData;
    }

    public WatchDataType getLastWatchDataType() {
        return watchDataType;
    }

    public Document getLastPage() {
        return page;
    }

    public Response getLastResponse() {
        return response;
    }

    public String getThreadServer() {
        return threadServer;
    }

    public void setThreadServer(String threadServer) {
        this.threadServer = threadServer;
    }

    public long getStartAt() {
        return startAt;
    }

    public void setStartAt(long startAt) {
        this.startAt = startAt;
    }

    public String getStreamCookie() {
        if(ServiceList.NicoNico.hasTokens()){
            String cookie = ServiceList.NicoNico.getTokens();
            if(cookie.endsWith(";")){
                cookie = cookie.substring(0, cookie.length() - 1);
            }
            cookie += ";" + streamCookie;
            return cookie;
        }
        return streamCookie;
    }

    public void setStreamCookie(String streamCookie) {
        this.streamCookie = streamCookie;
    }
}
