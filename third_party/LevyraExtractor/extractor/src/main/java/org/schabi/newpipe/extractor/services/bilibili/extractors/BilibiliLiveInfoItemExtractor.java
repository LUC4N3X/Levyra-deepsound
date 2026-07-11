package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Objects;

import javax.annotation.Nullable;

public class BilibiliLiveInfoItemExtractor implements StreamInfoItemExtractor {

    protected final JsonObject item;
    private final int type;

    public BilibiliLiveInfoItemExtractor(final JsonObject json, int type) {
        item = json;
        this.type = type;
    }

    @Override
    public String getName() throws ParsingException {
        if (item.getInt("live_status") == 2) {
            return getUploaderName() + "的投稿视频轮播";
        }
        return item.getString("title").replace("<em class=\"keyword\">", "").replace("</em>", "");
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://live.bilibili.com/" + item.getLong(type == 0 ? "roomid" : "room_id");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        if (type == 1) {
            return item.getString("cover_from_user");
        }
        return "https:" + item.getString("user_cover");
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
        return item.getLong("online");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return item.getString("uname");
    }

    @Nullable
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (type == 1) {
            return item.getString("face");
        }
        return "https:" + item.getString("uface");
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (type == 1) {
            return null;
        }
        return item.getString("live_time");
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if (type == 1) {
            return null;
        }
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }

    @Override
    public boolean isRoundPlayStream() {
        return type == 1;
    }
}
