package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.regex.Pattern;

import javax.annotation.Nullable;

public class NiconicoLiveRecommendVideoExtractor implements StreamInfoItemExtractor {
    private final JsonObject data;
    String uploaderUrl;
    String uploaderName;

    public NiconicoLiveRecommendVideoExtractor(JsonObject data, String uploaderUrl, String uploaderName) {
        this.data = data;
        this.uploaderUrl = uploaderUrl;
        this.uploaderName = uploaderName;
    }

    @Override
    public String getName() throws ParsingException {
        return data.getObject("content_meta").getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        if(getStreamType() == StreamType.LIVE_STREAM){
            return NiconicoService.LIVE_URL + data.getString("id");
        }
        return NiconicoService.WATCH_URL + data.getString("id");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        if(getStreamType() == StreamType.LIVE_STREAM){
            String result = data.getObject("content_meta").getString("live_screenshot_thumbnail_middle");
            return result.length()>0?result:
                    data.getObject("content_meta").getString("thumbnail_url");
        }
        return data.getObject("content_meta").getObject("thumbnail_url").getString("normal");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        if(data.getString("id").startsWith("lv")){
            return StreamType.LIVE_STREAM;
        }
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public boolean isAd() throws ParsingException {
        return false;
    }

    @Override
    public long getDuration() throws ParsingException {
        return data.getObject("content_meta").getLong("length_seconds");
    }

    @Override
    public long getViewCount() throws ParsingException {
        if(getStreamType() == StreamType.LIVE_STREAM){
            return data.getObject("content_meta").getLong("view_counter");
        }
        return data.getObject("content_meta").getLong("view_count");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if(getStreamType() == StreamType.LIVE_STREAM){
            return data.getObject("content_meta").getString("community_text");
        }
        String result = "user/" + data.getObject("content_meta").getLong("author_id");
        if(data.getObject("content_meta").getLong("author_id") == 0){
            result = data.getObject("content_meta").getObject("threads").getObject("channel").getString("channel_id");
        }
        if(uploaderUrl != null && uploaderUrl.contains(result)){
            return uploaderName;
        }
        return result;
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return NiconicoService.USER_URL+ data.getObject("content_meta").getString("author_id");
    }

    @Nullable
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return null;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if(getStreamType() == StreamType.LIVE_STREAM){
            return data.getObject("content_meta").getString("start_time");
        }
        return data.getObject("content_meta").getString("upload_time");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                getTextualUploadDate().split(Pattern.quote("+"))[0].replace("T"," "), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(9)));
    }
}
