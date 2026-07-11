package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.*;
import org.json.JSONObject;

import org.apache.commons.lang3.StringEscapeUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ContentNotAvailableException;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.LiveNotStartException;
import org.schabi.newpipe.extractor.exceptions.PaidContentException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.services.niconico.M3U8Parser;
import org.schabi.newpipe.extractor.services.niconico.NicoWebSocketClient;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.AudioStream;
import org.schabi.newpipe.extractor.stream.DeliveryMethod;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfoItemsCollector;
import org.schabi.newpipe.extractor.stream.StreamType;
import org.schabi.newpipe.extractor.stream.VideoStream;
import org.schabi.newpipe.extractor.utils.RegexUtils;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPInputStream;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

public class NiconicoStreamExtractor extends StreamExtractor {
    private JsonObject watch;
    private NiconicoWatchDataCache.WatchDataType type;
    private final NiconicoWatchDataCache niconicoWatchDataCache;
    private Document page = null;
    private String liveMessageServer;
    private JsonObject liveData;
    private Document liveResponse;
    private JsonObject liveDataRoot;
    private Map<String, List<String>> streamSources;

    public NiconicoStreamExtractor(final StreamingService service,
                                   final LinkHandler linkHandler,
                                   final NiconicoWatchDataCache niconicoWatchDataCache) {
        super(service, linkHandler);
        this.niconicoWatchDataCache = niconicoWatchDataCache;
    }

    static String decompressGzip(byte[] compressed) throws IOException {
        ByteArrayInputStream bis = new ByteArrayInputStream(compressed);
        GZIPInputStream gis = new GZIPInputStream(bis);
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        byte[] buffer = new byte[1024];
        int len;
        while ((len = gis.read(buffer)) > 0) {
            bos.write(buffer, 0, len);
        }
        gis.close();
        bos.close();
        return bos.toString("UTF-8");
    }

    @Override
    public long getViewCount() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveData.getObject("statistics").getLong("watchCount");
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return watch.getLong("view_counter");
        }
        return watch.getObject("video").getObject("count").getLong("view");
    }

    @Override
    public long getLength() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return -1;
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return watch.getLong("length_seconds");
        }
        return watch.getObject("video").getLong("duration");
    }

    @Override
    public long getTimeStamp() throws ParsingException {
        final Matcher timestamp = Pattern.compile("(?:#|&|\\?)from=(\\d+)").matcher(getOriginalUrl());
        if (!timestamp.find()) {
            return 0;
        }
        return Long.parseLong(timestamp.group(1));
    }

    @Override
    public long getLikeCount() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return -1;
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return watch.getLong("mylist_counter");
        }
        return watch.getObject("video").getObject("count").getLong("like");
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return new Description(liveData.getString("description"), 1);
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return new Description(watch.getString("description"), 1);
        }
        return new Description(watch.getObject("video").getString("description"), 1);
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            if (liveData.getObject("thumbnail").has("huge") && liveData.getObject("thumbnail").getObject("huge").has("s1280x720")) {
                return liveData.getObject("thumbnail").getObject("huge").getString("s1280x720");
            } else if (liveData.getObject("thumbnail").has("large")) {
                return liveData.getObject("thumbnail").getString("large");
            } else {
                return URLDecoder.decode((liveResponse.select("meta[property=og:image]").attr("content")));
            }
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return page.getElementsByClass("thumbnail").attr("src")
                    .replace("http:", "https:");
        }
        return watch.getObject("video").getObject("thumbnail").getString("ogp")
                .replace("http:", "https:");
    }

    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveData.getObject("supplier").getString("pageUrl");
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return "";
        }
        if (isChannel()) {
            return NiconicoService.CHANNEL_URL
                    + watch.getObject("channel").getString("id");
        }
        return NiconicoService.USER_URL + watch.getObject("owner").getLong("id");
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveData.getObject("supplier").getString("name");
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return getName();
        }
        if (isChannel()) {
            String result = watch.getObject("channel").getString("name");
            if (StringUtils.isEmpty(result)) {
                return "Unknown";
            }
            return result;
        }
        return watch.getObject("owner").getString("nickname");
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveResponse.select("div.thumbnail-area > a > img").attr("src");
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return getThumbnailUrl();
        }
        if (isChannel()) {
            String result = watch.getObject("channel")
                    .getObject("thumbnail").getString("url");
            if (StringUtils.isEmpty(result)) {
                return "";
            }
            return result;
        }
        return watch.getObject("owner").getString("iconUrl");
    }

    @Override
    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return Collections.emptyList();
        }
        final List<AudioStream> audioStreams = new ArrayList<>();
        ArrayList<String> audios = (ArrayList<String>) streamSources.get("audio");
        for (String audio : audios) {
            String id = RegexUtils.extract(audio, "audio-(.*?)-\\d+kbps");
            audioStreams.add(new AudioStream.Builder().setId("Niconico-" + getId() + "-audio")
                    .setContent(audio, true)
                    .setMediaFormat(MediaFormat.M4A).setQuality(id.split("-")[2].split("kbps")[0]).build());
        }
        return audioStreams;
    }

    public void getLiveUrl() throws ExtractionException, IOException, JsonParserException {
        String url = getUrl();
        HashMap<String, List<String>> tokens = new HashMap<>();
        if (ServiceList.NicoNico.hasTokens()) {
            tokens.put("Cookie", Collections.singletonList(ServiceList.NicoNico.getTokens()));
        }
        String responseBody = getDownloader().get(url, tokens).responseBody();
        liveResponse = Jsoup.parse(responseBody);
        String result = JsonParser.object().from(liveResponse
                        .select("script#embedded-data").attr("data-props"))
                .getObject("site").getObject("relive").getString("webSocketUrl");
        NicoWebSocketClient nicoWebSocketClient = new NicoWebSocketClient(URI.create(result), NiconicoService.getWebSocketHeaders());
        NicoWebSocketClient.WrappedWebSocketClient webSocketClient = nicoWebSocketClient.getWebSocketClient();
        webSocketClient.connect();
        long startTime = System.nanoTime();
        do {
            String liveUrl = nicoWebSocketClient.getUrl();
            liveMessageServer = nicoWebSocketClient.getServerUrl();
            if (liveUrl != null && liveMessageServer != null) {
                webSocketClient.close();
                return;
            }
        } while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime) <= 10);
        webSocketClient.close();
        if (responseBody.contains("フォロワー限定")) {
            throw new ContentNotAvailableException("The live is for followers only");
        } else if (responseBody.contains("非公開")) {
            throw new ContentNotAvailableException("タイムシフト非公開番組です");
        } else if (responseBody.contains("会員無料")) {
            throw new PaidContentException("Only available for premium users");
        }
        liveDataRoot = JsonParser.object().from(liveResponse.select("script#embedded-data")
                .first().attr("data-props"));
        liveData = liveDataRoot.getObject("program");
        if (getStartAt() - new Date().getTime() > 0) {
            throw new LiveNotStartException("The live is not started yet");
        }
        throw new ExtractionException("Failed to get live url");
    }

    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            final List<VideoStream> videoStreams = new ArrayList<>();
            videoStreams.add(new VideoStream.Builder().setContent(getUrl(), true)
                    .setId("Niconico-" + getId() + "-live").setIsVideoOnly(false)
                    .setResolution("720p").setDeliveryMethod(DeliveryMethod.HLS).build()); // not really 720p, we just fetch the best
            return videoStreams;
        } else {
            return Collections.emptyList();
        }
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws IOException, ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return Collections.emptyList();
        }
        final List<VideoStream> videoStreams = new ArrayList<>();
        ArrayList<String> videos = (ArrayList<String>) streamSources.get("video");
        if (Utils.isNullOrEmpty(videos)) {
            return Collections.emptyList();
        }
        for (String video : videos) {
            String id = RegexUtils.extract(video, "video-(.*?)-\\d+p");
            String resolution = id.split("-")[2];
            videoStreams.add(new VideoStream.Builder()
                    .setContent(video, true).setId("Niconico-" + getId() + '-' + resolution)
                    .setIsVideoOnly(true)
                    .setMediaFormat(MediaFormat.MPEG_4)
                    .setDeliveryMethod(DeliveryMethod.HLS)
                    .setResolution(resolution)
                    .build());
        }
        return videoStreams;
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        if (getUrl().contains("live.nicovideo.jp")) {
            return StreamType.LIVE_STREAM;
        }
        return StreamType.VIDEO_STREAM;
    }

    @Nonnull
    @Override
    public List<String> getTags() throws ParsingException {
        final List<String> tags = new ArrayList<>();
        if (getStreamType() == StreamType.LIVE_STREAM) {
            JsonArray data = liveData.getObject("tag").getArray("list");
            for (int i = 0; i < data.size(); i++) {
                tags.add(data.getObject(i).getString("text"));
            }
            return tags;
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return tags;
        }
        final JsonArray items = watch.getObject("tag").getArray("items");
        return items.stream()
                .filter(s -> s instanceof JsonObject)
                .map(s -> (JsonObject) s)
                .map(s -> s.getString("name"))
                .collect(Collectors.toList());
    }

    @Nullable
    @Override
    public InfoItemsCollector<? extends InfoItem, ? extends InfoItemExtractor> getRelatedItems()
            throws IOException, ExtractionException {
        final StreamInfoItemsCollector collector = new StreamInfoItemsCollector(
                getServiceId());
        if (getStreamType() == StreamType.LIVE_STREAM) {
            String url =
                    "https://live.nicovideo.jp/front/api/v1/recommend-contents" +
                            "?recipe=live_watch_related_contents_user&v=1&site=nicolive&content_meta=true&frontend_id=9&tags=&user_id=";
            String uploaderUrl = getUploaderUrl();
            if (uploaderUrl == null || uploaderUrl.contains("/ch")) {
                url = url.replace("live_watch_related_contents_user", "live_watch_related_contents_channel").replace("user_id", "channel_id");
                url += liveDataRoot.getObject("socialGroup").getString("id");
            } else {
                url += uploaderUrl.split("user/")[1];
            }
            try {
                JsonArray data = JsonParser.object().from(getDownloader().get(url).responseBody()).getObject("data").getArray("values");
                for (int i = 0; i < data.size(); i++) {
                    collector.commit(new NiconicoLiveRecommendVideoExtractor(
                            data.getObject(i), uploaderUrl, getUploaderName()));
                }
                return collector;
            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }
        }
        final String url = NiconicoService.RELATED_ITEMS_URL + getId();
        try {
            JsonArray data = JsonParser.object().from(getDownloader().get(url, NiconicoService.LOCALE).responseBody()).getObject("data").getArray("items");
            for (int i = 0; i < data.size(); i++) {
                if (data.getObject(i).getString("contentType").equals("mylist")) {
                    continue; //TODO: handle playlist here
                }
                collector.commit(new NiconicoPlaylistContentItemExtractor(data.getObject(i), true));
            }
        } catch (JsonParserException e) {
            throw new RuntimeException(e);
        }
        if (ServiceList.NicoNico.getFilterTypes().contains("related_item")) {
            collector.applyBlocking(ServiceList.NicoNico.getFilterConfig());
        }
        return collector;
    }

    @Override
    public void onFetchPage(final @Nonnull Downloader downloader)
            throws IOException, ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            try {
                getLiveUrl();
                liveDataRoot = JsonParser.object().from(liveResponse.select("script#embedded-data")
                        .first().attr("data-props"));
                liveData = liveDataRoot.getObject("program");
                niconicoWatchDataCache.setStartAt(liveData.getLong("beginTime") * 1000);
                niconicoWatchDataCache.setThreadServer(liveMessageServer);
            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }
            return;
        }
        watch = niconicoWatchDataCache.refreshAndGetWatchData(downloader, getId());
        niconicoWatchDataCache.setStartAt(-1);
        niconicoWatchDataCache.setThreadServer(null);

        JsonStringWriter resolutionObject = JsonWriter.string().object()
                .array("outputs");
        String audioResolutionName = watch.getObject("media").getObject("domand").getArray("audios").stream().filter(s -> ((JsonObject) s).getBoolean("isAvailable")).map(s -> (JsonObject) s).findFirst().get().getString("id");
        for (int i = 0; i < watch.getObject("media").getObject("domand").getArray("videos").size(); i++) {
            if (!watch.getObject("media").getObject("domand").getArray("videos").getObject(i).getBoolean("isAvailable")) {
                continue;
            }
            resolutionObject.array()
                    .value(watch.getObject("media").getObject("domand").getArray("videos").getObject(i).getString("id"))
                    .value(audioResolutionName)
                    .end();
        }
//        Response preFetch = downloader.options("https://nvapi.nicovideo.jp/v1/watch/"+ getId() +"/access-rights/hls?actionTrackId=" + watch.getObject("client").getString("watchTrackId"), NiconicoService.getPreFetchStreamHeaders());
        String resolutionObjectString = resolutionObject.end().end().done();
        Response response = null;
        try {
            Map<String, List<String>> headers = NiconicoService.getStreamSourceHeaders(watch.getObject("media").getObject("domand").getString("accessRightKey"));
            headers.put("Cookie", Collections.singletonList(niconicoWatchDataCache.getStreamCookie()));
            response = downloader.post("https://nvapi.nicovideo.jp/v1/watch/" + getId() + "/access-rights/hls?actionTrackId=" + watch.getObject("client").getString("watchTrackId"), headers, resolutionObjectString.getBytes(StandardCharsets.UTF_8));
            if (response.responseCode() / 100 != 2) {
                niconicoWatchDataCache.invalidate();
                throw new ExtractionException("Token expired. Please retry.");
            }
            Matcher matcher = Pattern.compile("(domand_bid=[^;]+)").matcher(response.responseHeaders().get("Set-Cookie").get(0));
            matcher.find();
            String responseBody2 = response.responseBody();
            if (response.getHeader("Content-Encoding") != null && response.getHeader("Content-Encoding").contains("gzip")) {
                try {
                    responseBody2 = decompressGzip(response.rawResponseBody());
                } catch (IOException e) {
                    throw new RuntimeException("Failed to decompress gzip response", e);
                }
            }
            response = downloader.get(new JSONObject(responseBody2).getJSONObject("data").getString("contentUrl"), NiconicoService.getStreamHeaders(niconicoWatchDataCache.getStreamCookie()));
            if (response.responseCode() / 100 == 2) {
                streamSources = M3U8Parser.parseMasterM3U8(utils.decompressBrotli(response.rawResponseBody()), niconicoWatchDataCache.getStreamCookie(), getLength());
            }
            niconicoWatchDataCache.setStreamCookie(matcher.group(0));
            headers.put("Cookie", Collections.singletonList(niconicoWatchDataCache.getStreamCookie()));
            if (response.responseCode() / 100 != 2) {
                response = downloader.post("https://nvapi.nicovideo.jp/v1/watch/" + getId() + "/access-rights/hls?actionTrackId=" + watch.getObject("client").getString("watchTrackId"), headers, resolutionObjectString.getBytes(StandardCharsets.UTF_8));
                String responseBody = response.responseBody();
                if (response.getHeader("Content-Encoding") != null && response.getHeader("Content-Encoding").contains("gzip")) {
                    try {
                        responseBody = decompressGzip(response.rawResponseBody());
                    } catch (IOException e) {
                        throw new RuntimeException("Failed to decompress gzip response", e);
                    }
                }
                response = downloader.get(new JSONObject(responseBody).getJSONObject("data").getString("contentUrl"), NiconicoService.getStreamHeaders(niconicoWatchDataCache.getStreamCookie()));
                if (response.responseCode() / 100 != 2) {
                    throw new ParsingException("Failed to get stream source");
                }
                streamSources = M3U8Parser.parseMasterM3U8(utils.decompressBrotli(response.rawResponseBody()), niconicoWatchDataCache.getStreamCookie(), getLength());
            }

        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        page = niconicoWatchDataCache.getLastPage();
        type = niconicoWatchDataCache.getLastWatchDataType();
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveData.getString("title");
        }
        if (type == NiconicoWatchDataCache.WatchDataType.LOGIN) {
            return watch.getString("title");
        }
        return watch.getObject("video").getString("title");
    }

    private Boolean isChannel() {
        return watch.isNull("owner");
    }

    @Override
    public long getStartAt() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return liveData.getLong("beginTime") * 1000;
        }
        return -1;
    }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return getUrl();
        }
        return null;
    }
}
