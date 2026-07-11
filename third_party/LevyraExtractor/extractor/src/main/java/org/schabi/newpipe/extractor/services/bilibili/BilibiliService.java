package org.schabi.newpipe.extractor.services.bilibili;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsExtractor;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliBulletCommentsExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliChannelExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliChannelTabExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliCommentExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliFeedExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliPlaylistExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliSearchExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BilibiliSuggestionExtractor;
import org.schabi.newpipe.extractor.services.bilibili.extractors.BillibiliStreamExtractor;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliBulletCommentsLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliCommentsLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliFeedLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliPlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;
import org.schabi.newpipe.extractor.utils.Utils;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import static org.schabi.newpipe.extractor.NewPipe.getDownloader;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.*;
import static org.schabi.newpipe.extractor.services.bilibili.utils.bytesToHex;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;

public class BilibiliService extends StreamingService {
    private final WatchDataCache watchDataCache;
    private static LinkedHashMap<String, String> defaultLatestCookies = null;

    public static final String WWW_REFERER = "https://www.bilibili.com/";
    public static final String SPACE_REFERER = "https://space.bilibili.com/";
    public static final String LIVE_REFERER = "https://live.bilibili.com/";

    public static String FREE_VIDEO_BASE_URL = "https://api.bilibili.com/x/player/wbi/playurl";
    public static String PAID_VIDEO_BASE_URL = "https://api.bilibili.com/pgc/player/web/v2/playurl";
    public static String LIVE_BASE_URL = "live.bilibili.com";
    public static String QUERY_VIDEO_BULLET_COMMENTS_URL = "https://api.bilibili.com/x/v1/dm/list.so?oid=";
    public static String QUERY_USER_INFO_URL = "https://api.bilibili.com/x/web-interface/card?photo=true&mid=";
    public static String QUERY_LIVEROOM_STATUS_URL = "https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids?uids[]=";
    public static String QUERY_DANMU_INFO_URL = "https://api.live.bilibili.com/xlive/web-room/v1/index/getDanmuInfo?type=0&id=";
    public static String GET_SEASON_ARCHIVES_LIST_RAW_URL = "https://api.bilibili.com/x/polymer/web-space/seasons_archives_list?mid=%s&season_id=%s&sort_reverse=false&name=%s&page_num=1&page_size=30";
    public static String GET_SERIES_RAW_URL = "https://api.bilibili.com/x/series/archives?mid=%s&series_id=%s&only_normal=true&sort=desc&name=%s&pn=1&ps=30";
    public static String GET_SERIES_BASE_URL = "https://api.bilibili.com/x/series/archives";
    public static String GET_SEASON_ARCHIVES_LIST_BASE_URL = "https://api.bilibili.com/x/polymer/web-space/seasons_series_list";
    public static String GET_SEASON_ARCHIVES_ARCHIVE_BASE_URL = "https://api.bilibili.com/x/polymer/web-space/seasons_archives_list";
    public static String GET_SUGGESTION_URL = "https://s.search.bilibili.com/main/suggest?term=";
    public static String GET_RELATED_URL = "https://api.bilibili.com/x/web-interface/archive/related?bvid=";
    public static String COMMENT_REPLIES_URL = "https://api.bilibili.com/x/v2/reply/reply?type=1&ps=10&web_location=333.788&oid=";
    public static String GET_SUBTITLE_META_URL = "https://api.bilibili.com/x/player/wbi/v2";
    public static String QUERY_USER_VIDEOS_WEB_API_URL = "https://api.bilibili.com/x/space/wbi/arc/search";
    public static String QUERY_USER_VIDEOS_SEARCH_API_URL = "https://api.bilibili.com/x/series/recArchivesByKeywords";
    public static String QUERY_USER_VIDEOS_CLIENT_API_URL = "https://app.bilibili.com/x/v2/space/archive/cursor";
    public static String WBI_IMG_URL = "https://api.bilibili.com/x/web-interface/nav";
    public static String GET_PARTITION_URL = "https://api.bilibili.com/x/player/pagelist?bvid=";
    public static String FETCH_COOKIE_URL = "https://api.bilibili.com/x/frontend/finger/spi";
    public static String FETCH_COMMENTS_URL = "https://api.bilibili.com/x/v2/reply/wbi/main";
    public static String FETCH_TAGS_URL = "https://api.bilibili.com/x/web-interface/view/detail/tag?bvid=";
    public final static String FETCH_RECOMMENDED_LIVES_URL = "https://api.live.bilibili.com/xlive/web-interface/v1/second/getUserRecommend?page_size=30&platform=web";
    public static String VIDEOSHOT_API_URL = "https://api.bilibili.com/x/player/videoshot?index=1&bvid=";
    public final static String FETCH_TICKET_URL = "https://api.bilibili.com/bapis/bilibili.api.ticket.v1.Ticket/GenWebTicket";

    public final static String APP_KEY = "1d8b6e7d45233436";
    public final static String APP_SEC = "560c52ccd288fed045859ed18bffd973";
    public final static String APP_PLATFORM = "android";
    public final static String APP_TYPE = "android";

    private static String mapToCookieHeader(LinkedHashMap<String, String> cookies) {
        if (cookies == null || cookies.isEmpty()) {
            return "";
        }
        return cookies.entrySet().stream()
                .map(entry -> entry.getKey() + "=" + entry.getValue())
                .collect(Collectors.joining("; ")); // Join with "; " for readability
    }

    /**
     * Generate a HMAC-SHA256 hash of the given message string using the given key
     * string.
     *
     * @param key     The key string to use for the HMAC-SHA256 hash.
     * @param message The message string to hash.
     * @return The HMAC-SHA256 hash of the given message string using the given key
     * string.
     */
    private static String hmacSha256(String key, String message) {
        Mac mac;
        try {
            mac = Mac.getInstance("HmacSHA256");
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException(e);
        }
        SecretKeySpec secretKeySpec = new SecretKeySpec(key.getBytes(StandardCharsets.UTF_8), "HmacSHA256");
        try {
            mac.init(secretKeySpec);
        } catch (InvalidKeyException e) {
            throw new RuntimeException(e);
        }
        byte[] hash = mac.doFinal(message.getBytes(StandardCharsets.UTF_8));
        return bytesToHex(hash);
    }

    /**
     * Get a Bilibili web ticket for the given CSRF token.
     *
     * @param csrf The CSRF token to use for the web ticket, can be {@code null} or
     *             empty.
     * @see <a href="https://github.com/SocialSisterYi/bilibili-API-collect/blob/master/docs/misc/sign/bili_ticket.md">BiliTicket</a>
     */
    private static Map.Entry<String, Long> getBiliTicket(String csrf, LinkedHashMap<String, String> cookies) throws IOException, JsonParserException {
        // params
        long ts = Instant.now().getEpochSecond();
        String hexSign = hmacSha256("XgwSnGZ1p", "ts" + ts);
        String url = FETCH_TICKET_URL + '?' +
                "key_id=ec02" + '&' +
                "hexsign=" + hexSign + '&' +
                "context[ts]=" + ts + '&' +
                "csrf=" + (csrf == null ? "" : csrf);
        LinkedHashMap<String, List<String>> headers = getUserAgentHeaders(WWW_REFERER);
        headers.put("Cookie", Collections.singletonList(mapToCookieHeader(cookies)));
        Response response;
        try {
            response = getDownloader().post(url, headers, null);
        } catch (ReCaptchaException e) {
            throw new IOException(e);
        }
        JsonObject data = Objects.requireNonNull(JsonParser.object().from(response.responseBody()).getObject("data"), "data");
        String ticket = Objects.requireNonNull(data.getString("ticket"), "ticket");
        long createdAt = data.getLong("created_at");
        if (createdAt <= 0) {
            throw new IllegalArgumentException("created_at: " + createdAt);
        }
        long ttl = data.getLong("ttl");
        if (ttl <= 0) {
            throw new IllegalArgumentException("ttl: " + ttl);
        }
        long expires = createdAt + ttl;
        return new AbstractMap.SimpleEntry<>(ticket, expires);
    }

    /**
     * @see <a href="https://github.com/SocialSisterYi/bilibili-API-collect/issues/933">_uuid</a>
     */
    private static String getFpUuid() {
        final String[] DIGIT_MAP = {
                "1", "2", "3", "4", "5", "6", "7", "8", "9", "A", "B", "C", "D", "E", "F", "10"
        };

        // Get the last 5 digits of the current timestamp
        long t = System.currentTimeMillis() % 100_000;

        // Generate 32 random bytes
        byte[] index = new byte[32];
        ThreadLocalRandom.current().nextBytes(index);

        // Build the main part of the UUID string
        StringBuilder result = new StringBuilder(64);
        List<Integer> hyphenIndices = Arrays.asList(9, 13, 17, 21);

        for (int ii = 0; ii < index.length; ii++) {
            if (hyphenIndices.contains(ii)) {
                result.append('-');
            }
            // Use the lower 4 bits of the random byte as an index into DIGIT_MAP
            result.append(DIGIT_MAP[index[ii] & 0x0f]);
        }

        // Append the formatted timestamp and the suffix
        result.append(String.format("%05d", t));
        result.append("infoc");

        return result.toString();
    }

    static public LinkedHashMap<String, String> getDefaultCookies() throws IOException {
        if (defaultLatestCookies == null || Long.parseLong(defaultLatestCookies.get("bili_ticket_expires")) <= Instant.now().getEpochSecond()) {
            Response response;
            try {
                response = getDownloader().get(FETCH_COOKIE_URL, getUserAgentHeaders(WWW_REFERER));
            } catch (ReCaptchaException e) {
                throw new IOException(e);
            }
            JsonObject data;
            try {
                data = Objects.requireNonNull(JsonParser.object().from(response.responseBody()).getObject("data"), "data");
            } catch (JsonParserException e) {
                throw new IOException(e);
            }
            LinkedHashMap<String, String> cookies = new LinkedHashMap<>();
            cookies.put("buvid3", Objects.requireNonNull(data.getString("b_3"), "b_3"));
            cookies.put("b_nut", String.valueOf(
                    ZonedDateTime.parse(Objects.requireNonNull(response.getHeader("date"), "date"), DateTimeFormatter.RFC_1123_DATE_TIME).toEpochSecond()
            ));
            byte[] randomLsidBytes = new byte[32];
            ThreadLocalRandom.current().nextBytes(randomLsidBytes);
            String randomLsid = String.format("%s_%X", bytesToHex(randomLsidBytes).toUpperCase(Locale.ROOT), System.currentTimeMillis());
            cookies.put("b_lsid", randomLsid);
            cookies.put("_uuid", getFpUuid());
            cookies.put("buvid4", Objects.requireNonNull(data.getString("b_4"), "b_4"));

            try {
                Map.Entry<String, Long> biliTicket = getBiliTicket("", cookies);
                cookies.put("bili_ticket", biliTicket.getKey());
                cookies.put("bili_ticket_expires", String.valueOf(biliTicket.getValue()));
            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }

            byte[] randomBuvidFp = new byte[16];
            ThreadLocalRandom.current().nextBytes(randomBuvidFp);
            cookies.put("buvid_fp", bytesToHex(randomBuvidFp));

            defaultLatestCookies = cookies;
        }
        return defaultLatestCookies;
    }

    static public LinkedHashMap<String, List<String>> getHeaders(String originalUrl) throws IOException {
        LinkedHashMap<String, List<String>> headers = getUserAgentHeaders(originalUrl);
        String cookie = mapToCookieHeader(getDefaultCookies());
        headers.put("Cookie", Collections.singletonList(cookie));
        return headers;
    }

    static public LinkedHashMap<String, List<String>> getUserAgentHeaders(String originalUrl) throws MalformedURLException {
        final LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("User-Agent", Collections.singletonList(DeviceForger.requireRandomDevice().getUserAgent()));
        if (originalUrl != null) {
            String referer = "https://" + Utils.stringToURL(originalUrl).getHost() + "/";
            if (!Arrays.asList(WWW_REFERER, SPACE_REFERER, LIVE_REFERER).contains(referer)) {
                referer = WWW_REFERER;
            }
            headers.put("Referer", Collections.singletonList(referer));
        }
        headers.put("Accept-Language", Collections.singletonList("zh-CN,zh;q=0.9"));
        return headers;
    }

    static public LinkedHashMap<String, String> getWebSocketHeaders() {
        LinkedHashMap<String, String> httpHeaders = new LinkedHashMap<String, String>();
        httpHeaders.put("User-Agent", DeviceForger.requireRandomDevice().getUserAgent());
        httpHeaders.put("Accept", "*/*");
        httpHeaders.put("Accept-Language", "zh-CN,zh;q=0.9");
        httpHeaders.put("Accept-Encoding", "gzip, deflate, br");
        httpHeaders.put("Sec-WebSocket-Version", "13");
        httpHeaders.put("Origin", "https://www.piesocket.com");
        httpHeaders.put("Sec-WebSocket-Extensions", "permessage-deflate");
        httpHeaders.put("Sec-WebSocket-Key", "9cwI/6tCIyNBM4XSsi3jMA==");
        httpHeaders.put("Connection", "keep-alive, Upgrade");
        httpHeaders.put("Sec-Fetch-Dest", "websocket");
        httpHeaders.put("Sec-Fetch-Mode", "websocket");
        httpHeaders.put("Sec-Fetch-Site", "cross-site");
        httpHeaders.put("Pragma", "no-cache");
        httpHeaders.put("Cache-Control", "no-cache");
        httpHeaders.put("Upgrade", "websocket");
        return httpHeaders;
    }

    static public LinkedHashMap<String, List<String>> getSponsorBlockHeaders(){
        final LinkedHashMap<String, List<String>> headers = new LinkedHashMap<>();
        headers.put("Origin", Collections.singletonList("PipePipe"));
        return headers;
    }

    static public LinkedHashMap<String, List<String>> getLoggedHeadersOrNull(String originalUrl, String condition) throws MalformedURLException {
        if (ServiceList.BiliBili.hasTokens() && ServiceList.BiliBili.getCookieFunctions() != null
                && ServiceList.BiliBili.getCookieFunctions().contains(condition)) {
            LinkedHashMap<String, List<String>> headers = getUserAgentHeaders(originalUrl);
            headers.put("Cookie", Collections.singletonList(ServiceList.BiliBili.getTokens()));
            return headers;
        }
        return null;
    }

    static public boolean isBiliBiliDownloadUrl(String url){
        // *.akamaized.net, *.bilivideo.com
        return url.contains("akamaized.net") || url.contains("bilivideo.com");
    }

    static public boolean isBiliBiliUrl(String url){
        return url.contains("bilibili.com");
    }

    static public String getResolution(int code) {
        switch (code) {
            case 127:
                return "8K 超高清";
            case 126:
                return "杜比视界";
            case 125:
                return "HDR 真彩色";
            case 120:
                return "4K 超清";
            case 116:
                return "1080P60 高帧率";
            case 112:
                return "1080P+ 高码率";
            case 80:
                return "1080P 高清";
            case 74:
                return "720P60 高帧率";
            case 64:
                return "720P 高清";
            case 32:
                return "480P 清晰";
            case 16:
                return "360P 流畅";
            case 6:
                return "240P 极速";
            default:
                return "Unknown resolution";
        }
    }

    static public String getBitrate(int code) {
        //30216 	64K
        //30232 	132K
        //30280 	192K
        //30250 	杜比全景声
        //30251 	Hi-Res无损
        switch (code) {
            case 30216:
                return "64K";
            case 30232:
                return "132K";
            case 30280:
                return "192K";
            case 30250:
            case 30255:
                return "杜比全景声";
            case 30251:
                return "Hi-Res无损";
            default:
                return "Unknown bitrate";
        }
    }

    public BilibiliService(int id) {
        super(id, "BiliBili", Arrays.asList(VIDEO, COMMENTS, BULLET_COMMENTS, SPONSORBLOCK));
        watchDataCache = new WatchDataCache();
    }

    @Override
    public String getBaseUrl() {
        return "https://bilibili.com";
    }

    @Override
    public long getFeedFetchInterval() {
        return 10000;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return new BilibiliStreamLinkHandlerFactory();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return new BilibiliChannelLinkHandlerFactory();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return new BilibiliPlaylistLinkHandlerFactory();
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return new BilibiliSearchQueryHandlerFactory();
    }

    @Override
    public SearchExtractor getSearchExtractor(SearchQueryHandler queryHandler) {
        return new BilibiliSearchExtractor(this, queryHandler);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new BilibiliSuggestionExtractor(this);
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList kioskList = new KioskList(this);
        final KioskList.KioskExtractorFactory kioskFactory = (streamingService, url, id) ->
                new BilibiliFeedExtractor(this, new BilibiliFeedLinkHandlerFactory().fromUrl(url), id);
        final BilibiliFeedLinkHandlerFactory h = new BilibiliFeedLinkHandlerFactory();
        try {
            kioskList.addKioskEntry(kioskFactory, h, "Recommended Videos");
            kioskList.addKioskEntry(kioskFactory, h, "Recommended Lives");
            kioskList.addKioskEntry(kioskFactory, h, "Top 100");
            kioskList.setDefaultKiosk("Recommended Videos");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return kioskList;
    }

    @Override
    public ChannelExtractor getChannelExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new BilibiliChannelExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new BilibiliChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new BilibiliPlaylistExtractor(this, linkHandler);
    }

    @Override
    public StreamExtractor getStreamExtractor(LinkHandler linkHandler) throws ExtractionException {
        return new BillibiliStreamExtractor(this, linkHandler, watchDataCache);
    }

    @Override
    public CommentsExtractor getCommentsExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new BilibiliCommentExtractor(this, linkHandler);
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return new BilibiliCommentsLinkHandlerFactory(watchDataCache);
    }

    @Override
    public ListLinkHandlerFactory getBulletCommentsLHFactory() {
        return new BilibiliBulletCommentsLinkHandlerFactory();
    }

    @Override
    public BulletCommentsExtractor getBulletCommentsExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new BilibiliBulletCommentsExtractor(this, linkHandler, watchDataCache);
    }

    @Override
    public BulletCommentsExtractor getBulletCommentsExtractor(final String url)
            throws ExtractionException {
        return getBulletCommentsExtractor(getBulletCommentsLHFactory().fromUrl(url));
    }


    public static final int USER_VIDEO_API_MODE_WEB = 0;
    public static final int USER_VIDEO_API_MODE_SEARCH = 1;
    public static final int USER_VIDEO_API_MODE_CLIENT = 2;
    public static final int SIZE_USER_VIDEO_API_MODE = 3;

    private static int userVideoApiMode = USER_VIDEO_API_MODE_WEB;

    public static int getCurrentVideoApiMode() {
        return userVideoApiMode;
    }

    public static void rotateVideoApiMode() {
        userVideoApiMode = (userVideoApiMode + 1) % SIZE_USER_VIDEO_API_MODE;
    }
}
