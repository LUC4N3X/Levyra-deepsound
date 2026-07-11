package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import org.apache.commons.lang3.StringUtils;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import javax.annotation.Nullable;

import static org.schabi.newpipe.extractor.services.bilibili.BilibiliService.LIVE_BASE_URL;

public class BilibiliRecommendLiveInfoItemExtractor implements StreamInfoItemExtractor {
    private final JsonObject data;


    public BilibiliRecommendLiveInfoItemExtractor(JsonObject data) {
        this.data = data;
    }

    @Override
    public String getName() throws ParsingException {
        return this.data.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://" + LIVE_BASE_URL + "/" + data.getLong("roomid");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        String result = data.getString("user_cover");
        if(!StringUtils.isNotBlank(result)){
            result = data.getString("system_cover");
        }
        return result.replace("http:", "https:");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.LIVE_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        return -1;
    }

    @Override
    public long getViewCount() throws ParsingException {
        return this.data.getObject("watched_show").getLong("num");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return this.data.getString("uname");
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        return null;
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return null;
    }
}
