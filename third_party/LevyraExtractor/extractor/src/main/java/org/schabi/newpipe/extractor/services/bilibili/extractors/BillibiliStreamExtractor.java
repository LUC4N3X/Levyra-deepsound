package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import org.apache.commons.lang3.StringEscapeUtils;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.channel.StaffInfoItem;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.*;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.bilibili.BilibiliService;
import org.schabi.newpipe.extractor.services.bilibili.WatchDataCache;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.utils.Utils;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.*;
import static org.schabi.newpipe.extractor.services.bilibili.utils.bv2av;
import static org.schabi.newpipe.extractor.services.bilibili.utils.createQueryString;
import static org.schabi.newpipe.extractor.services.bilibili.utils.formatParamWithPercentSpace;
import static org.schabi.newpipe.extractor.services.bilibili.utils.getDmImgParams;
import static org.schabi.newpipe.extractor.services.bilibili.utils.getWbiResult;

public class BillibiliStreamExtractor extends StreamExtractor {

    private JsonObject watch;
    long cid = 0;
    int duration = 0;
    JsonObject page = null;
    String bvid;
    WatchDataCache watchDataCache;
    private boolean isRoundPlay;
    private JsonObject playData;
    private String liveUrl;
    private JsonObject dataObject;
    private final List<VideoStream> videoOnlyStreams = new ArrayList<>();
    private final List<AudioStream> audioStreams = new ArrayList<>();
    private long playTime;
    private String currentRoundTitle;
    private long nextTimestamp;
    private int isPremiumContent;
    private JsonArray relatedPaidItems;
    private int isPaid;
    private JsonObject premiumData;
    private JsonArray dataArray = new JsonArray();
    private int partitions = 0;
    private JsonArray tagData = new JsonArray();
    private JsonArray partitionData = new JsonArray();
    private JsonArray relatedData = new JsonArray();
    private JsonArray subtitleData = new JsonArray();
    private CancellableCall tagCall = null;
    private CancellableCall partitionCall = null;
    private CancellableCall relatedCall = null;
    private CancellableCall subtitleCall = null;

    public BillibiliStreamExtractor(StreamingService service, LinkHandler linkHandler, WatchDataCache watchDataCache) {
        super(service, linkHandler);
        this.watchDataCache = watchDataCache;
    }

    public static <T> T[] repeat(T[] arr, int newLength) {
        T[] dup = Arrays.copyOf(arr, newLength);
        for (int last = arr.length; last != 0 && last < newLength; last <<= 1) {
            System.arraycopy(dup, 0, dup, last, Math.min(last << 1, newLength) - last);
        }
        return dup;
    }

    private static int[] parseRange(final String range) {
        final String[] parts = range.split("-");
        return new int[]{Integer.parseInt(parts[0]), Integer.parseInt(parts[1])};
    }

    private static int parseFps(final String frameRate) {
        if (frameRate == null || frameRate.isEmpty()) {
            return 0;
        }
        if (frameRate.contains("/")) {
            final String[] parts = frameRate.split("/");
            return (int) (Double.parseDouble(parts[0]) / Double.parseDouble(parts[1]));
        }
        return (int) Double.parseDouble(frameRate);
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return watch.getString("cover_from_user").replace("http:", "https:");
        }
        if (isPremiumContent == 1) {
            return watch.getString("cover").replace("http:", "https:");
        }
        return watch.getString("pic").replace("http:", "https:");
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        if (isPremiumContent == 1) {
            if (premiumData.getObject("up_info") == null) {
                return null;
            }
            return BilibiliChannelLinkHandlerFactory.baseUrl + premiumData.getObject("up_info").getLong("mid");
        }
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return BilibiliChannelLinkHandlerFactory.baseUrl + watch.getLong("uid");
        }
        return BilibiliChannelLinkHandlerFactory.baseUrl + watch.getObject("owner").getLong("mid");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if (isPremiumContent == 1) {
            return Optional.ofNullable(premiumData.getObject("up_info").getString("uname")).orElse("BiliBili");
        }
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return watch.getString("uname");
        }
        return watch.getObject("owner").getString("name");
    }

    @Nonnull
    @Override
    public List<StaffInfoItem> getStaffs() {
        JsonArray staffs = watch.getArray("staff");
        if (!staffs.isEmpty()) {
            return staffs.stream()
                    .map(item -> {
                        JsonObject staff = (JsonObject) item;
                        String staffName = staff.getString("name");
                        String staffTitle = staff.getString("title");
                        String staffThumbnail = staff.getString("face");
                        long staffMid = staff.getLong("mid");
                        String url = BilibiliChannelLinkHandlerFactory.baseUrl + staffMid;
                        return new StaffInfoItem(getServiceId(), url, staffName, staffTitle, staffThumbnail);
                    })
                    .collect(Collectors.toList());
        } else {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public Map<String, String> getStats() {
        JsonObject stat = watch.getObject("stat");
        return stat.entrySet().stream()
                .collect(
                        Collectors.toMap(
                                Map.Entry::getKey,
                                entry -> entry.getValue().toString()
                        )
                );
    }

    public List<AudioStream> getAudioStreams() throws IOException, ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM && !isRoundPlay) {
            return null;
        }
        JsonArray audioObjects = dataObject.getArray("audio");
        if (dataObject.getObject("dolby").getArray("audio").size() != 0) {
            audioObjects.addAll(0, dataObject.getObject("dolby").getArray("audio"));
        }
        if (dataObject.getObject("flac").getObject("audio").size() != 0) {
            audioObjects.add(0, dataObject.getObject("flac").getObject("audio"));
        }
        ArrayList<AudioStream> audioStreamsForDownloader = new ArrayList<>();
        for (int i = 0; i < audioObjects.size(); i++) {
            JsonObject audioObject = audioObjects.getObject(i);
            JsonObject segmentBase = audioObject.getObject("SegmentBase");
            int[] initialization = parseRange(segmentBase.getString("Initialization"));
            int[] indexRange = parseRange(segmentBase.getString("indexRange"));
            audioStreamsForDownloader.add(new AudioStream.Builder().setId("bilibili-" + bvid + "-audio")
                    .setContent(audioObject.getString("base_url"), true).setCodec(audioObject.getString("codecs").split("\\.")[0])
                    .setBitrate(audioObject.getInt("bandwidth"))
                    .setAverageBitrate(audioObject.getInt("bandwidth"))
                    .setInitStart(initialization[0]).setInitEnd(initialization[1])
                    .setIndexStart(indexRange[0]).setIndexEnd(indexRange[1])
                    .setMediaFormat(MediaFormat.M4A).setQuality(getBitrate(audioObject.getInt("id"))).build());
        }
        return audioStreamsForDownloader;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws IOException, ExtractionException {
        return videoOnlyStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() throws IOException, ExtractionException {
        if (isRoundPlay || getStreamType() != StreamType.LIVE_STREAM) {
            if (dataArray.size() > 0) {
                final List<VideoStream> videoStreams = new ArrayList<>();
                for (int j = 0; j < dataArray.size(); j++) {
                    String resolution = BilibiliService.getResolution(dataArray.getObject(j).getInt("id"));
                    videoStreams.add(new VideoStream.Builder().setContent(dataArray.getObject(j).getString("base_url"), true)
                            .setId("bilibili-" + watch.getLong("cid"))
                            .setIsVideoOnly(false).setResolution(resolution)
                            .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP).build());
                }
                return videoStreams;
            }
            return null;
        }
        final List<VideoStream> videoStreams = new ArrayList<>();

        videoStreams.add(new VideoStream.Builder().setContent(liveUrl, true)
                .setId("bilibili-" + watch.getLong("uid") + "-live")
                .setIsVideoOnly(false).setResolution("720p") // not really 720p, we just fetch the best
                .setDeliveryMethod(DeliveryMethod.PROGRESSIVE_HTTP).build());
        return videoStreams;
    }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException {
        return "";
    }

    public void buildStreams() throws ExtractionException {
        if (dataObject.getArray("audio").size() == 0) {
            dataArray = dataObject.getArray("video");
            return;
        }
        buildVideoOnlyStreamsArray();
        buildAudioStreamsArray();
    }

    public void buildAudioStreamsArray() throws ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM && !isRoundPlay) {
            return;
        }
        JsonObject audioObject = dataObject.getArray("audio").getObject(0);
        JsonObject segmentBase = audioObject.getObject("SegmentBase");
        int[] initialization = parseRange(segmentBase.getString("Initialization"));
        int[] indexRange = parseRange(segmentBase.getString("indexRange"));
        JsonArray backupUrls = audioObject.getArray("backupUrl");
        audioStreams.add(new AudioStream.Builder().setId("bilibili-" + bvid + "-audio")
                .setContent(audioObject.getString("baseUrl"), true)
                .setCodec(audioObject.getString("codecs").split("\\.")[0])
                .setBitrate(audioObject.getInt("bandwidth"))
                .setInitStart(initialization[0]).setInitEnd(initialization[1])
                .setIndexStart(indexRange[0]).setIndexEnd(indexRange[1])
                .setMediaFormat(MediaFormat.M4A).setAverageBitrate(192000).build());
        for (int j = 0; j < backupUrls.size(); j++) {
            audioStreams.add(new AudioStream.Builder().setId("bilibili-" + bvid + "-audio")
                    .setContent(backupUrls.getString(j), true)
                    .setCodec(audioObject.getString("codecs").split("\\.")[0])
                    .setBitrate(audioObject.getInt("bandwidth"))
                    .setInitStart(initialization[0]).setInitEnd(initialization[1])
                    .setIndexStart(indexRange[0]).setIndexEnd(indexRange[1])
                    .setMediaFormat(MediaFormat.M4A).setAverageBitrate(192000).build());
        }
    }

    public void buildVideoOnlyStreamsArray() throws ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM && !isRoundPlay) {
            return;
        }
        JsonArray videoArray = dataObject.getArray("video");
        for (int i = 0; i < videoArray.size(); i++) {
            JsonObject object = videoArray.getObject(i);
            int code = object.getInt("id");
            String resolution = BilibiliService.getResolution(code);
            JsonObject segmentBase = object.getObject("SegmentBase");
            int[] initialization = parseRange(segmentBase.getString("Initialization"));
            int[] indexRange = parseRange(segmentBase.getString("indexRange"));
            videoOnlyStreams.add(new VideoStream.Builder().setContent(object.getString("baseUrl"), true)
                    .setMediaFormat(MediaFormat.MPEG_4).setId("bilibili-" + bvid + "-video").setCodec(object.getString("codecs"))
                    .setBitrate(object.getInt("bandwidth"))
                    .setInitStart(initialization[0]).setInitEnd(initialization[1])
                    .setIndexStart(indexRange[0]).setIndexEnd(indexRange[1])
                    .setWidth(object.getInt("width")).setHeight(object.getInt("height"))
                    .setFps(parseFps(object.getString("frameRate")))
                    .setIsVideoOnly(true).setResolution(resolution).build());
        }
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        if (getLinkHandler().getOriginalUrl().contains("live.bilibili.com")) {
            return StreamType.LIVE_STREAM;
        }
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public void onFetchPage(@Nonnull Downloader downloader) throws IOException, ExtractionException {
        watchDataCache.init(getUrl());
        // case: Live
        if (getStreamType() == StreamType.LIVE_STREAM) {
            String response = downloader.get("https://api.live.bilibili.com/room/v1/Room/room_init?id=" + getId()).responseBody();
            try {
                JsonObject responseJson = JsonParser.object().from(response);
                JsonObject data = responseJson.getObject("data");
                String uid = String.valueOf(data.getLong("uid"));
                if (data.size() == 0) {
                    throw new ExtractionException("Can not get live room info. Error message: " + responseJson.getString("msg"));
                }
                response = downloader.get("https://api.live.bilibili.com/room/v1/Room/get_status_info_by_uids?uids[]=" + uid).responseBody();
                watch = JsonParser.object().from(response).getObject("data").getObject(uid);
                watchDataCache.setRoomId(data.getLong("room_id"));
                watchDataCache.setStartTime(data.getLong("live_time"));
                switch (data.getInt("live_status")) {
                    case 0:
                        throw new LiveNotStartException("Live is not started.");
                    case 2:
                        long timestamp = getOriginalUrl().contains("timestamp=") ?
                                Long.parseLong(getOriginalUrl().split("timestamp=")[1].split("&")[0]) : new Date().getTime();
                        isRoundPlay = true;
                        response = downloader.get(
                                String.format("https://api.live.bilibili.com/live/getRoundPlayVideo?room_id=%s&a=%s&type=flv",
                                        data.getLong("room_id"), timestamp)).responseBody();
                        responseJson = JsonParser.object().from(response).getObject("data");
                        if (responseJson.getLong("cid") < 0) {
                            throw new ContentNotAvailableException("Round playing is not available at this moment.");
                        }
                        playTime = responseJson.getLong("play_time");
                        currentRoundTitle = responseJson.getString("title");
                        currentRoundTitle = currentRoundTitle.split("-")[1] + currentRoundTitle.split("-")[2];
                        bvid = responseJson.getString("bvid");
                        response = getDownloader().get("https://api.bilibili.com/x/player/playurl" + "?cid="
                                + responseJson.getLong("cid") + "&bvid=" + bvid
                                + "&fnval=4048&qn=120&fourk=1&try_look=1", getHeaders(getOriginalUrl())).responseBody();
                        playData = JsonParser.object().from(response);
                        dataObject = playData.getObject("data").getObject("dash");
                        buildStreams();
                        nextTimestamp = timestamp + dataObject.getLong("duration") * 1000;
                    case 1:
                        response = getDownloader().get("https://api.live.bilibili.com/room/v1/Room/playUrl?qn=10000&platform=web&cid=" + getId(), getHeaders(getOriginalUrl())).responseBody();
                        liveUrl = JsonParser.object().from(response).getObject("data").getArray("durl").getObject(0).getString("url");
                }
            } catch (JsonParserException e) {
                e.printStackTrace();
            }
            return;
        }

        // case: non-live
        // step 1: fetch metadata
        if (getUrl().contains("bangumi/play/")) {
            isPremiumContent = 1;
            int type = getId().startsWith("ss") ? 0 : 1;
            String response;
            try {
                response = downloader.get("https://api.bilibili.com/pgc/view/web/season?"
                        + (type == 0 ? "season_id=" : "ep_id=") + getId().substring(2), getHeaders(getOriginalUrl())).responseBody();
            } catch (Exception e) {
                throw new ContentNotAvailableException("Unknown reason");
            }

            try {
                premiumData = JsonParser.object().from(response).getObject("result");
                relatedPaidItems = premiumData
                        .getArray("episodes");
                if (type == 0) {
                    watch = relatedPaidItems.getObject(0);
                } else {
                    for (int i = 0; i < relatedPaidItems.size(); i++) {
                        JsonObject temp = relatedPaidItems.getObject(i);
                        if (temp.getString("share_url").endsWith(getId())) {
                            watch = temp;
                            break;
                        }
                    }
                    if (watch == null) {
                        throw new ExtractionException("Not found id in series data");
                    }
                }
                bvid = watch.getString("bvid");
                cid = watch.getLong("cid");
                watchDataCache.setCid(getId(), cid);
                watchDataCache.setBvid(getId(), bvid);
                duration = watch.getInt("duration") / 1000;
                isPaid = watch.getObject("rights").getInt("pay");
            } catch (JsonParserException e) {
                e.printStackTrace();
            }
        } else {
            String url = getLinkHandler().getOriginalUrl();
            bvid = utils.getPureBV(getId());
            url = utils.getUrl(url, bvid);
            String response = downloader.get(url,
                    getLoggedHeadersOrNull(getOriginalUrl(), "ai_subtitle") != null ? getLoggedHeadersOrNull(getOriginalUrl(), "ai_subtitle") : getHeaders(getOriginalUrl())
            ).responseBody();
            try {
                watch = JsonParser.object().from(response).getObject("data");
            } catch (JsonParserException e) {
                e.printStackTrace();
            }
            String pageNumString = Utils.getQueryValue(Utils.stringToURL(getLinkHandler().getUrl()), "p");
            int pageNum = 1;
            if (pageNumString != null) {
                pageNum = Integer.parseInt(pageNumString);
            }
            page = watch.getArray("pages").getObject(pageNum - 1);
            cid = page.getLong("cid");
            watchDataCache.setCid(getId(), cid);
            watchDataCache.setBvid(getId(), bvid);
            duration = page.getInt("duration");
            isPaid = watch.getObject("rights").getInt("pay");
        }

        // step 1.5: other requests, should start early to improve speed
        tagCall = downloader.getAsync(FETCH_TAGS_URL + utils.getPureBV(getId()), getHeaders(getOriginalUrl()), response1 -> {
            try {
                tagData = JsonParser.object().from(response1.responseBody()).getArray("data");
            } catch (JsonParserException e) {
                e.printStackTrace();
            }
        });
        if (isPremiumContent != 1) {
            partitionCall = downloader.getAsync(GET_PARTITION_URL + bvid, getHeaders(getOriginalUrl()), response1 -> {
                try {
                    partitionData = JsonParser.object().from(response1.responseBody()).getArray("data");
                } catch (JsonParserException e) {
                    e.printStackTrace();
                }
            });
            relatedCall = downloader.getAsync(GET_RELATED_URL + bvid, getHeaders(getOriginalUrl()), response1 -> {
                try {
                    relatedData = JsonParser.object().from(response1.responseBody()).getArray("data");
                } catch (JsonParserException e) {
                    e.printStackTrace();
                }
            });
            if (getLoggedHeadersOrNull(getOriginalUrl(), "ai_subtitle") != null) {
                LinkedHashMap<String, String> params = new LinkedHashMap<>();
                params.put("aid", String.valueOf(utils.bv2av(bvid)));
                params.put("cid", String.valueOf(cid));
                params.put("isGaiaAvoided", "false");
                params.put("web_location", "1315873");
                params.putAll(getDmImgParams());
                subtitleCall = downloader.getAsync(
                        getWbiResult(GET_SUBTITLE_META_URL, params),
                        getLoggedHeadersOrNull(getOriginalUrl(), "ai_subtitle"), response1 -> {
                            try {
                                subtitleData = JsonParser.object().from(response1.responseBody()).getObject("data").getObject("subtitle").getArray("subtitles");
                            } catch (JsonParserException e) {
                                e.printStackTrace();
                            }
                        }
                );
            }
        }


        // step 2: fetch stream data
        String baseUrl = isPremiumContent != 1 ? FREE_VIDEO_BASE_URL : PAID_VIDEO_BASE_URL;
        LinkedHashMap<String, String> params = new LinkedHashMap<>();
        params.put("avid", String.valueOf(bv2av(bvid)));
        params.put("bvid", bvid);
        params.put("cid", String.valueOf(cid));

        params.put("qn", "120");
        params.put("fnver", "0");
        params.put("fnval", "4048");
        params.put("fourk", "1");

        Map<String, List<String>> headers = getHeaders(getOriginalUrl());
        if (ServiceList.BiliBili.hasTokens() && ServiceList.BiliBili.getCookieFunctions().contains("high_res")) {
            headers.put("Cookie", Collections.singletonList(ServiceList.BiliBili.getTokens()));
        } else {
            if (isPremiumContent != 1) {
                // https://codeberg.org/NullPointerException/PipePipe/issues/42
                params.put("try_look", "1");
            }
        }

        String finalUrl;
        if (isPremiumContent != 1) {
            params.put("web_location", "1315873");

            params.putAll(getDmImgParams());

            finalUrl = getWbiResult(baseUrl, params);
        } else {
            finalUrl = baseUrl + "?" + createQueryString(params);
        }

        String response = getDownloader().get(finalUrl, headers).responseBody();
        try {
            playData = JsonParser.object().from(response);
            switch (playData.getInt("code")) {
                case 0:
                    break;
                case -10403:
                default:
                    String message = playData.getString("message");
                    if (message.contains("地区")) {
                        throw new GeographicRestrictionException(message);
                    }
                    throw new ContentNotAvailableException(message);
            }
            JsonObject dataParentObject = (isPremiumContent == 1 ? playData.getObject("result").getObject("video_info") : playData.getObject("data"));
            dataObject = dataParentObject.getObject("dash");
            if (dataObject.size() == 0) {
                throw new PaidContentException("Paid content");
                //dataArray = dataParentObject.getArray("durl");
            } else {
                buildStreams();
            }
            if (isPaid == 1 && videoOnlyStreams.size() + audioStreams.size() == 0) {
                throw new PaidContentException("Paid content");
            }
        } catch (JsonParserException e) {
            e.printStackTrace();
        }
    }

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        if (isRoundPlay) {
            return getUploaderName() + "的投稿视频轮播";
        }
        String title = isPremiumContent == 1 ? watch.getString("share_copy") : watch.getString("title");

        if (getStreamType() != StreamType.LIVE_STREAM && isPremiumContent != 1 && watch.getArray("pages").size() > 1) {
            title = "P" + page.getInt("page") + " " + page.getString("part") + " | " + title;
        }
        return StringEscapeUtils.unescapeHtml4(title);
    }

    @Override
    public long getLength() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return -1;
        }
        return duration;
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return watch.getString("face").replace("http:", "https:");
        }
        if (isPremiumContent == 1) {
            try {
                return premiumData.getObject("up_info").getString("avatar").replace("http:", "https:");
            } catch (Exception e) {
                return "https://i2.hdslb.com/bfs/face/0c84b9f4ad546d3f20324809d45fc439a2a8ddab.jpg@240w_240h_1c_1s.webp";
            }

        }
        return watch.getObject("owner").getString("face").replace("http:", "https:");
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return null;
        }
        if (isPremiumContent == 1) {
            return new Description(premiumData.getString("evaluate"), Description.PLAIN_TEXT);
        }
        return new Description(watch.getString("desc"), Description.PLAIN_TEXT);
    }


    @Override
    public long getViewCount() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return watch.getLong("online");
        }
        if (isPremiumContent == 1) {
            return premiumData.getObject("stat").getLong("views");
        }
        return watch.getObject("stat").getLong("view");
    }

    @Override
    public long getLikeCount() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return -1;
        }
        if (isPremiumContent == 1) {
            return premiumData.getObject("stat").getLong("likes");
        }
        return watch.getObject("stat").getLong("like");
    }

    @Nonnull
    @Override
    public List<String> getTags() throws ParsingException {
        waitForCall(tagCall);
        List<String> tags = new ArrayList<>();
        if (getStreamType() == StreamType.LIVE_STREAM) {
            tags = Arrays.asList((watch.getString("tag_name") + "," + watch.getString("tags")).split(","));
        } else {
            for (int i = 0; i < tagData.size(); i++) {
                tags.add(tagData.getObject(i).getString("tag_name"));
            }
        }
        return tags;
    }

    @Override
    public InfoItemsCollector<? extends InfoItem, ? extends InfoItemExtractor> getRelatedItems() throws ParsingException {
        InfoItemsCollector<InfoItem, InfoItemExtractor> collector = new MultiInfoItemsCollector(getServiceId());
        if (isPremiumContent == 1) {
            for (int i = 0; i < relatedPaidItems.size(); i++) {
                collector.commit(new BilibiliPremiumContentInfoItemExtractor(relatedPaidItems.getObject(i)));
            }
            return collector;
        }
        if (getStreamType() == StreamType.LIVE_STREAM) {
            if (isRoundPlay) {
                collector.commit(new BilibiliSameContentInfoItemExtractor(getUploaderName() + "的投稿视频轮播",
                        getUrl() + "?timestamp=" + nextTimestamp, getThumbnailUrl()
                        , getUploaderName(), getViewCount()));
                return collector;
            }
            return null;
        }
        waitForCall(relatedCall);
        try {
            for (int i = 0; i < relatedData.size(); i++) {
                collector.commit(new BilibiliRelatedInfoItemExtractor(relatedData.getObject(i)));
                if (i == 0 && partitions > 1) {
                    collector.commit(new BilibiliPlaylistInfoItemExtractor(watch.getString("title"),
                            GET_PARTITION_URL
                                    + bvid
                                    + "&name="
                                    + watch.getString("title")
                                    + "&thumbnail="
                                    + formatParamWithPercentSpace(getThumbnailUrl())
                                    + "&uploaderName="
                                    + getUploaderName()
                                    + "&uploaderAvatar="
                                    + formatParamWithPercentSpace(getUploaderAvatarUrl())
                                    + "&uploaderUrl="
                                    + formatParamWithPercentSpace(getUploaderUrl()),
                            getThumbnailUrl(), getUploaderName(), partitions));
                }
            }
            if (ServiceList.BiliBili.getFilterTypes().contains("related_item")) {
                collector.applyBlocking(ServiceList.BiliBili.getFilterConfig());
            }
            return collector;
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        return collector;
    }

    @SuppressWarnings("SimpleDateFormat")
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (getStreamType().equals(StreamType.LIVE_STREAM)) {
            return null;
        }
        if (isPremiumContent == 1) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(watch.getLong("pub_time") * 1000));
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(watch.getLong("pubdate") * 1000));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if (getStreamType().equals(StreamType.LIVE_STREAM)) {
            return null;
        }
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }

    @Nonnull
    @Override
    public List<SubtitlesStream> getSubtitlesDefault() throws IOException, ExtractionException {
        if (getLoggedHeadersOrNull(getOriginalUrl(), "ai_subtitle") == null || getStreamType().equals(StreamType.LIVE_STREAM)
                || isPremiumContent == 1) {
            return new ArrayList<>();
        }
        waitForCall(subtitleCall);
        List<SubtitlesStream> subtitlesToReturn = new ArrayList<>();
        for (int i = 0; i < subtitleData.size(); i++) {
            JsonObject subtitlesStream = subtitleData.getObject(i);
            String bccResult = getDownloader()
                    .get("https:" + subtitlesStream
                            .getString("subtitle_url")
                            .replace("http:", "https:"), getHeaders(getOriginalUrl())).responseBody();
            try {
                subtitlesToReturn.add(new SubtitlesStream.Builder()
                        .setContent(utils.bcc2srt(JsonParser.object().from(bccResult)), false)
                        .setMediaFormat(MediaFormat.SRT)
                        .setLanguageCode(subtitlesStream.getString("lan").replace("ai-", ""))
                        .setAutoGenerated(subtitlesStream.getInt("ai_status") != 0)
                        .build());
            } catch (JsonParserException e) {
                throw new RuntimeException(e);
            }
        }
        return subtitlesToReturn;
    }

    @Override
    public long getTimeStamp() throws ParsingException {
        try {
            return Long.parseLong(getUrl().split("#timestamp=")[1]);
        } catch (Exception e) {
            return isRoundPlay ? playTime : 0;
        }

    }

    @Override
    public boolean isSupportComments() throws ParsingException {
        return getStreamType() != StreamType.LIVE_STREAM;
    }

    @Override
    public boolean isSupportRelatedItems() throws ParsingException {
        return getStreamType() != StreamType.LIVE_STREAM;
    }

    @Override
    public boolean isRoundPlayStream() {
        return isRoundPlay;
    }

    @Override
    public long getStartAt() throws ParsingException {
        if (getStreamType() == StreamType.LIVE_STREAM && !isRoundPlay) {
            return watch.getLong("live_time") * 1000;
        }
        return -1;
    }

    @Override
    public InfoItemsCollector<? extends InfoItem, ? extends InfoItemExtractor> getPartitions() throws ParsingException {
        InfoItemsCollector<StreamInfoItem, StreamInfoItemExtractor> collector = new StreamInfoItemsCollector(getServiceId());
        try {
            waitForCall(partitionCall);
            partitions = partitionData.size();
            for (int i = 0; i < partitionData.size(); i++) {
                collector.commit(
                        new BilibiliRelatedInfoItemExtractor(
                                partitionData.getObject(i), bvid, getThumbnailUrl(), String.valueOf(i + 1), getUploaderName(), watch.getLong("pubdate")));
            }
        } catch (ParsingException e) {
            e.printStackTrace();
        }
        return collector;
    }

    private void waitForCall(CancellableCall call) {
        long startTime = System.nanoTime();
        do {
            if (call == null || call.isFinished()) {
                return;
            }
        } while (TimeUnit.NANOSECONDS.toSeconds(System.nanoTime() - startTime) <= 2);
    }

    @Nonnull
    @Override
    public List<Frameset> getFrames() throws ExtractionException {
        if (getStreamType() == StreamType.LIVE_STREAM) {
            return Collections.emptyList();
        }

        try {
            String videoshotUrl = VIDEOSHOT_API_URL + bvid;
            
            String response = getDownloader().get(videoshotUrl, getHeaders(getOriginalUrl())).responseBody();
            JsonObject responseJson = JsonParser.object().from(response);
            
            if (responseJson.getInt("code") != 0) {
                return Collections.emptyList();
            }

            JsonObject data = responseJson.getObject("data");
            if (data == null || data.getArray("image") == null || data.getArray("index") == null) {
                return Collections.emptyList();
            }

            JsonArray imageUrls = data.getArray("image");
            JsonArray timeIndex = data.getArray("index");
            
            if (imageUrls.isEmpty() || timeIndex.isEmpty()) {
                return Collections.emptyList();
            }

            // Get frame properties
            int frameWidth = data.getInt("img_x_size");
            int frameHeight = data.getInt("img_y_size");
            int framesPerPageX = data.getInt("img_x_len", 10);
            int framesPerPageY = data.getInt("img_y_len", 10);
            int totalFrames = timeIndex.size() - 1; // Last index is the end time

            if (frameWidth <= 0 || frameHeight <= 0 || framesPerPageX <= 0
                    || framesPerPageY <= 0 || totalFrames <= 0) {
                return Collections.emptyList();
            }

            // Calculate average duration per frame
            int durationPerFrame = 0;
            if (totalFrames > 1) {
                // Convert seconds to milliseconds and calculate average interval
                int totalDuration = timeIndex.getInt(totalFrames) - timeIndex.getInt(0);
                durationPerFrame = (totalDuration * 1000) / totalFrames;
            }

            if (durationPerFrame <= 0) {
                return Collections.emptyList();
            }

            // Prepare URLs with https protocol
            List<String> urls = new ArrayList<>();
            for (int i = 0; i < imageUrls.size(); i++) {
                String url = imageUrls.getString(i);
                if (url.startsWith("//")) {
                    url = "https:" + url;
                }
                urls.add(url);
            }

            List<Frameset> result = new ArrayList<>();
            result.add(new Frameset(
                    urls,
                    frameWidth,
                    frameHeight,
                    totalFrames,
                    durationPerFrame,
                    framesPerPageX,
                    framesPerPageY
            ));

            return result;

        } catch (IOException | JsonParserException e) {
            throw new ExtractionException("Failed to get video frames", e);
        }
    }
}
