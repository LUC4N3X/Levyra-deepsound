package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;

import javax.annotation.Nullable;

public class NiconicoLiveHistoryInfoItemExtractor implements StreamInfoItemExtractor {
    private JsonObject data;

    public NiconicoLiveHistoryInfoItemExtractor(JsonObject data){
        this.data = data;
    }
    @Override
    public String getName() throws ParsingException {
        return data.getObject("program").getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return NiconicoService.WATCH_URL + data.getObject("linkedContent").getString("contentId");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return data.getObject("thumbnail").getObject("huge").getString("s352x198");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public boolean isAd() throws ParsingException {
        return false;
    }

    @Override
    public long getDuration() throws ParsingException {
        JsonObject schedule = data.getObject("program").getObject("schedule");
        return schedule.getObject("endTime").getLong("seconds")
                - schedule.getObject("beginTime").getLong("seconds");
    }

    @Override
    public long getViewCount() throws ParsingException {
        return data.getObject("statistics").getObject("viewers").getLong("value");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return data.getObject("programProvider").getString("name");
    }

    @Override
    public String getUploaderUrl() throws ParsingException {
        return data.getObject("programProvider").getString("profileUrl");
    }

    @Nullable
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return data.getObject("programProvider").getObject("icons").getString("uri150x150");
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return false;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(data.getObject("program").getObject("schedule").getObject("endTime").getLong("seconds") * 1000L));
    }

    @Nullable
    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                getTextualUploadDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(9)));
    }
}
