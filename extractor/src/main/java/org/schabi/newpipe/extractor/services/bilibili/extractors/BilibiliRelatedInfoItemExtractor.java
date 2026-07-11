package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.bilibili.utils;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.Objects;

public class BilibiliRelatedInfoItemExtractor implements StreamInfoItemExtractor {

    protected final JsonObject item;
    String id = "";
    String pic = "";
    String p = "1";
    String type = "multiP";
    String name;
    Long pubdate;

    public BilibiliRelatedInfoItemExtractor(final JsonObject json, String id, String pic, String p, String name, Long pubdate) {
        item = json;
        this.id = id;
        this.pic = pic;
        this.p = p;
        this.name = name;
        this.pubdate = pubdate;
    }

    public BilibiliRelatedInfoItemExtractor(final JsonObject json) throws ParsingException {
        item = json;
        type = "related";
        id = item.getString("bvid").equals("") ? utils.av2bv(item.getLong("aid")) : item.getString("bvid");
        pic = item.getString("pic").replace("http", "https");
        pubdate = item.getLong("pubdate");
    }

    @Override
    public String getName() throws ParsingException {
        if (type.equals("related")) {
            return item.getString("title");
        }
        return item.getString("part");
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://www.bilibili.com/video/" + id + "?p=" + p;
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return pic;
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        return item.getLong("duration");
    }

    @Override
    public long getViewCount() throws ParsingException {
        if (type.equals("multiP")) return -1;
        return item.getObject("stat").getLong("view");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        if (type.equals("multiP")) return name;
        return item.getObject("owner").getString("name");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        if (type.equals("multiP")) return null;
        return item.getObject("owner").getString("face").replace("http", "https");
    }

    @SuppressWarnings("SimpleDateFormat")
    @Override
    public String getTextualUploadDate() throws ParsingException {
        if(pubdate == null) return null;
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(pubdate * 1000));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        if(pubdate == null) return null;
        return new DateWrapper(LocalDateTime.parse(
                Objects.requireNonNull(getTextualUploadDate()), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }

}
