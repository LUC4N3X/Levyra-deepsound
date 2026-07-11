package org.schabi.newpipe.extractor.services.niconico;

import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.BULLET_COMMENTS;
import static org.schabi.newpipe.extractor.StreamingService.ServiceInfo.MediaCapability.VIDEO;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.channel.ChannelExtractor;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsExtractor;
import org.schabi.newpipe.extractor.kiosk.KioskList;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.linkhandler.LinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistExtractor;
import org.schabi.newpipe.extractor.search.SearchExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoChannelTabExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoCommentsExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoBulletCommentsExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoCommentsCache;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoPlaylistExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoSearchExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoSeriesExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoStreamExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoSuggestionExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoTrendExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoUserExtractor;
import org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoWatchDataCache;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoCommentsLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoPlaylistLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoSearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoTrendLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.niconico.linkHandler.NiconicoUserLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.subscription.SubscriptionExtractor;
import org.schabi.newpipe.extractor.suggestion.SuggestionExtractor;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class NiconicoService extends StreamingService {
    public NiconicoService(final int id) {
        //super(id, "NicoNico", Arrays.asList(VIDEO, COMMENTS, BULLET_COMMENTS));
        super(id, "NicoNico", Arrays.asList(VIDEO, BULLET_COMMENTS));
    }

    public static final String BASE_URL = "https://www.nicovideo.jp";
    public static final String SP_BASE_URL = "https://sp.nicovideo.jp";
    public static final String USER_URL
            = "https://www.nicovideo.jp/user/";
    public static final String WATCH_URL
            = "https://www.nicovideo.jp/watch/";
    public static final String SP_WATCH_URL
            = "https://sp.nicovideo.jp/watch/";
    public static final String LIVE_URL
            = "https://live.nicovideo.jp/watch/";
    public static final String CHANNEL_URL
            = "https://ch.nicovideo.jp/";
    public static final String DAILY_TREND_URL
            = "https://www.nicovideo.jp/ranking/genre/all?term=24h";
    public static final String RECOMMEND_LIVES_URL
            = "https://live.nicovideo.jp/front/api/v1/recommend-contents?recipe=live_top_guest&v=2&site=nicolive&content_meta=true&frontend_id=9";
    public static final String TOP_LIVES_URL
            = "https://live.nicovideo.jp/ranking";
    public static final String SUGGESTION_URL
            = "https://sug.search.nicovideo.jp/suggestion/expand/";
    public static final String RELATED_ITEMS_URL =
            "https://nvapi.nicovideo.jp/v1/recommend?recipeId=video_watch_recommendation&site=nicovideo&_frontendId=6&_frontendVersion=0&videoId=";

    public static final String SEARCH_URL =
            "https://www.nicovideo.jp/search/";

    public static final String LIVE_SEARCH_URL =
            "https://live.nicovideo.jp/search";

    public static final String PLAYLIST_SEARCH_API_URL =
            "https://nvapi.nicovideo.jp/v1/search/list?_frontendId=6&_frontendVersion=0";

    public static final String MYLIST_URL =
            "https://nvapi.nicovideo.jp/v2/mylists/";

    public static final String MYLIST_PAGE_URL =
            "https://www.nicovideo.jp/mylist/";
    public static final String TRENDING_RSS_STR = "^第\\d+位：(.*)$";
    public static final String SMILEVIDEO
            = "(nicovideo\\.jp\\/watch|nico\\.ms)\\/(.+)?";
    public static final String USER_UPLOAD_LIST
            = "(?:www|sp).nicovideo.jp/user/(\\d+)(?:/video)?";

    // generally, Niconico uses Japanese, but some videos have multiple language
    // texts.
    // Use ja-JP locale to get original information of video.
    public static final Localization LOCALE = Localization.fromLocalizationCode("ja-JP");

    @Override
    public String getBaseUrl() {
        return BASE_URL;
    }

    static public Map<String, List<String>> getMylistHeaders(){
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("X-Frontend-Id", Collections.singletonList("6"));
        headers.put("X-Frontend-Version", Collections.singletonList("0"));
        headers.put("Referer", Collections.singletonList("https://www.nicovideo.jp/"));
        headers.put("Origin", Collections.singletonList("https://www.nicovideo.jp"));
        return headers;
    }

    static public Map<String, String> getWebSocketHeaders(){
        Map<String, String> httpHeaders = new HashMap<String, String>();
        httpHeaders.put("Pragma", "no-cache");
        httpHeaders.put("Origin", "https://live.nicovideo.jp");
        httpHeaders.put("Accept-Language", "en-US,en;q=0.9");
        httpHeaders.put("User-Agent", "Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/108.0.0.0 Safari/537.36");
        httpHeaders.put("Upgrade", "websocket");
        httpHeaders.put("Cache-Control", "no-cache");
        httpHeaders.put("Connection", "Upgrade");
        httpHeaders.put("Sec-WebSocket-Version", "13");
        httpHeaders.put("Sec-WebSocket-Extensions", "permessage-deflate; client_max_window_bits");
        if(ServiceList.NicoNico.hasTokens()){
            httpHeaders.put("Cookie", ServiceList.NicoNico.getTokens());
        }
        return httpHeaders;
    }

    static public Map<String, List<String>> getStreamSourceHeaders(String accessKey){
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/119.0"));
        headers.put("Accept", Collections.singletonList("*/*"));
        headers.put("Accept-Language", Collections.singletonList("en-US,en;q=0.5"));
        headers.put("Accept-Encoding", Collections.singletonList("gzip, deflate, br"));
        headers.put("Referer", Collections.singletonList("https://www.nicovideo.jp/"));
        headers.put("Content-Type", Collections.singletonList("application/json"));
        headers.put("X-Request-With", Collections.singletonList("https://www.nicovideo.jp"));
        headers.put("X-Access-Right-Key", Collections.singletonList(accessKey));
        headers.put("X-Frontend-Id", Collections.singletonList("6"));
        headers.put("X-Frontend-Version", Collections.singletonList("0"));
        headers.put("Origin", Collections.singletonList("https://www.nicovideo.jp"));
        headers.put("DNT", Collections.singletonList("1"));
        headers.put("Connection", Collections.singletonList("keep-alive"));
        headers.put("Sec-Fetch-Dest", Collections.singletonList("empty"));
        headers.put("Sec-Fetch-Mode", Collections.singletonList("cors"));
        headers.put("Sec-Fetch-Site", Collections.singletonList("same-site"));
        headers.put("Sec-GPC", Collections.singletonList("1"));
        headers.put("TE", Collections.singletonList("trailers"));
        return headers;
    }

    static public Map<String, List<String>> getPreFetchStreamHeaders(){
        Map<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/119.0"));
        headers.put("Accept", Collections.singletonList("*/*"));
        headers.put("Accept-Language", Collections.singletonList("en-US,en;q=0.5"));
        headers.put("Accept-Encoding", Collections.singletonList("gzip, deflate, br"));
        headers.put("Access-Control-Request-Method", Collections.singletonList("POST"));
        headers.put("Access-Control-Request-Headers", Collections.singletonList("content-type,x-access-right-key,x-frontend-id,x-frontend-version,x-request-with"));
        headers.put("Referer", Collections.singletonList("https://www.nicovideo.jp/"));
        headers.put("Origin", Collections.singletonList("https://www.nicovideo.jp"));
        headers.put("DNT", Collections.singletonList("1"));
        headers.put("Connection", Collections.singletonList("keep-alive"));
        headers.put("Sec-Fetch-Dest", Collections.singletonList("empty"));
        headers.put("Sec-Fetch-Mode", Collections.singletonList("cors"));
        headers.put("Sec-Fetch-Site", Collections.singletonList("same-site"));
        headers.put("Sec-GPC", Collections.singletonList("1"));
        headers.put("TE", Collections.singletonList("trailers"));
        return headers;
    }

    static public Map<String, List<String>> getStreamHeaders(String cookie){
        HashMap<String, List<String>> headers = new HashMap<>();
        headers.put("User-Agent", Collections.singletonList("Mozilla/5.0 (X11; Linux x86_64; rv:109.0) Gecko/20100101 Firefox/119.0"));
        headers.put("Accept", Collections.singletonList("*/*"));
        headers.put("Accept-Language", Collections.singletonList("en-US,en;q=0.5"));
        headers.put("Accept-Encoding", Collections.singletonList("gzip, deflate, br"));
        headers.put("Referer", Collections.singletonList("https://www.nicovideo.jp/"));
        headers.put("Origin", Collections.singletonList("https://www.nicovideo.jp"));
        headers.put("DNT", Collections.singletonList("1"));
        headers.put("Connection", Collections.singletonList("keep-alive"));
        headers.put("Sec-Fetch-Dest", Collections.singletonList("empty"));
        headers.put("Sec-Fetch-Mode", Collections.singletonList("cors"));
        headers.put("Sec-Fetch-Site", Collections.singletonList("same-site"));
        headers.put("Sec-GPC", Collections.singletonList("1"));
        headers.put("Cookie", Collections.singletonList(cookie));
        return headers;
    }

    @Override
    public LinkHandlerFactory getStreamLHFactory() {
        return new NiconicoStreamLinkHandlerFactory();
    }

    @Override
    public ListLinkHandlerFactory getChannelLHFactory() {
        return new NiconicoUserLinkHandlerFactory();
    }

    @Override
    public ListLinkHandlerFactory getChannelTabLHFactory() {
        return null;
    }

    @Override
    public ListLinkHandlerFactory getPlaylistLHFactory() {
        return new NiconicoPlaylistLinkHandlerFactory();
    }

    @Override
    public SearchQueryHandlerFactory getSearchQHFactory() {
        return new NiconicoSearchQueryHandlerFactory();
    }

    @Override
    public ListLinkHandlerFactory getCommentsLHFactory() {
        return new NiconicoCommentsLinkHandlerFactory(new NiconicoStreamLinkHandlerFactory());
    }

    @Override
    public SearchExtractor getSearchExtractor(final SearchQueryHandler queryHandler) {
        return new NiconicoSearchExtractor(this, queryHandler);
    }

    @Override
    public SuggestionExtractor getSuggestionExtractor() {
        return new NiconicoSuggestionExtractor(this);
    }

    @Override
    public SubscriptionExtractor getSubscriptionExtractor() {
        return null;
    }

    @Override
    public KioskList getKioskList() throws ExtractionException {
        final KioskList.KioskExtractorFactory kioskFactory = (streamingService, url, id)
                -> new NiconicoTrendExtractor(
                this,
                new NiconicoTrendLinkHandlerFactory().fromUrl(url), id);

        final KioskList kioskList = new KioskList(this);

        final NiconicoTrendLinkHandlerFactory h = new NiconicoTrendLinkHandlerFactory();

        try {
            kioskList.addKioskEntry(kioskFactory, h, "Trending");
//            kioskList.addKioskEntry(kioskFactory, h, "Recommended Lives");
//            kioskList.addKioskEntry(kioskFactory, h, "Top Lives");
            kioskList.setDefaultKiosk("Trending");
        } catch (final Exception e) {
            throw new ExtractionException(e);
        }

        return kioskList;
    }

    @Override
    public ChannelExtractor getChannelExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new NiconicoUserExtractor(this, linkHandler);
    }

    @Override
    public ChannelTabExtractor getChannelTabExtractor(ListLinkHandler linkHandler) throws ExtractionException {
        return new NiconicoChannelTabExtractor(this, linkHandler);
    }

    @Override
    public PlaylistExtractor getPlaylistExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        if(linkHandler.getUrl().contains("/series/")){
            return new NiconicoSeriesExtractor(this, linkHandler);
        }
        return new NiconicoPlaylistExtractor(this, linkHandler);
    }


    @Override
    public StreamExtractor getStreamExtractor(final LinkHandler linkHandler)
            throws ExtractionException {
        return new NiconicoStreamExtractor(this, linkHandler, getWatchDataCache());
    }

    @Override
    public CommentsExtractor getCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new NiconicoCommentsExtractor(this, linkHandler,
                getWatchDataCache(),
                getCommentsCache());
    }

    private NiconicoCommentsCache commentsCache = null;

    private NiconicoCommentsCache getCommentsCache() {
        if (commentsCache == null) {
            commentsCache = new NiconicoCommentsCache();
        }
        return commentsCache;
    }

    private NiconicoWatchDataCache watchDataCache;

    private NiconicoWatchDataCache getWatchDataCache() {
        if (watchDataCache == null) {
            watchDataCache = new NiconicoWatchDataCache();
        }
        return watchDataCache;
    }

    @Override
    public BulletCommentsExtractor getBulletCommentsExtractor(final ListLinkHandler linkHandler)
            throws ExtractionException {
        return new NiconicoBulletCommentsExtractor(this,
                linkHandler, getWatchDataCache(), getCommentsCache());
    }

    public ListLinkHandlerFactory getBulletCommentsLHFactory() {
        return new NiconicoCommentsLinkHandlerFactory(new NiconicoStreamLinkHandlerFactory());
    }

    @Override
    public BulletCommentsExtractor getBulletCommentsExtractor(final String url)
            throws ExtractionException {
        return getBulletCommentsExtractor(getBulletCommentsLHFactory().fromUrl(url));
    }

    private static final List<Localization> SUPPORTED_LANGUAGES = Localization.listFrom(
            "ja"
    );
    @Override
    public List<Localization> getSupportedLocalizations() {
        return SUPPORTED_LANGUAGES;
    }
}
