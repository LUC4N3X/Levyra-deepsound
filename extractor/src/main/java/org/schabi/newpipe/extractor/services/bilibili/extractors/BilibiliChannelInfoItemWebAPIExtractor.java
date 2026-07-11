package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;
import java.util.Optional;

import static org.schabi.newpipe.extractor.services.bilibili.utils.getDurationFromString;

public class BilibiliChannelInfoItemWebAPIExtractor implements StreamInfoItemExtractor {

    protected final JsonObject item;
    private final String name;
    private final String face;

    public BilibiliChannelInfoItemWebAPIExtractor(final JsonObject json, String name, String face) {
        item = json;
        this.name = name;
        this.face = face;
    }

    @Override
    public String getName() throws ParsingException {
        return item.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://www.bilibili.com/video/" + item.getString("bvid") + "?p=1";
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return item.getString("pic").replace("http:", "https:");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        if (item.getLong("duration") != 0) {
            return item.getLong("duration");
        }
        return getDurationFromString(item.getString("length"));
    }

    @Override
    public long getViewCount() throws ParsingException {
        return Optional.of(item.getLong("play"))
                .filter(playCount -> playCount > 0)
                .orElseGet(() -> item.getObject("stat").getLong("view"));
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return Optional.ofNullable(item.getString("author")).orElse(name);
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return face;
    }

    @SuppressWarnings("SimpleDateFormat")
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if (item.getInt("created") == 0) {
            return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((item.getInt("pubdate")) * 1000L));
        }
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date((item.getInt("created")) * 1000L));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }


    @Override
    public boolean requiresMembership() throws ParsingException {
        return item.getInt("elec_arc_type") == 1;
    }
}
