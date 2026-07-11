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

public class BilibiliPremiumContentInfoItemExtractor implements StreamInfoItemExtractor {

    private final JsonObject data;

    BilibiliPremiumContentInfoItemExtractor(JsonObject json) {
        data = json;
    }

    @Override
    public String getName() throws ParsingException {
        String result = data.getString("share_copy");
        if (result == null) {
            result = data.getString("title").replace("<em class=\"keyword\">", "").replace("</em>", "");
        }
        return result;
    }

    @Override
    public String getUrl() throws ParsingException {
        String result = data.getString("url");
        return result == null ? data.getString("share_url") : result;
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return data.getString("cover").replace("http:", "https:");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        return data.getLong("duration") / 1000;
    }

    @Override
    public long getViewCount() throws ParsingException {
        return -1;
    }

    @Override
    public String getUploaderName() throws ParsingException {
        try {
            return data.getString("org_title").replace("<em class=\"keyword\">", "").replace("</em>", "");
        } catch (Exception e) {
            return "BiliBili";
        }

    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        return true;
    }

    @SuppressWarnings("SimpleDateFormat")
    @Override
    public String getTextualUploadDate() throws ParsingException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(
                data.getInt("pubtime") != 0 ? data.getInt("pubtime") : data.getInt("pub_time") * 1000L));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }
}
