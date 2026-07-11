package org.schabi.newpipe.extractor.services.bilibili.extractors;

import java.text.SimpleDateFormat;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.apache.commons.lang3.StringEscapeUtils;
import org.jsoup.nodes.Element;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.localization.DateWrapper;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliStreamLinkHandlerFactory;
import org.schabi.newpipe.extractor.stream.StreamInfoItemExtractor;
import org.schabi.newpipe.extractor.stream.StreamType;

import static org.schabi.newpipe.extractor.services.bilibili.utils.getDurationFromString;

public class BilibiliStreamInfoItemExtractor implements StreamInfoItemExtractor {

    protected final JsonObject item;

    public BilibiliStreamInfoItemExtractor(final JsonObject json) {
        item = json;
    }

    @Override
    public String getName() throws ParsingException {
        return StringEscapeUtils.unescapeHtml4(
                item.getString("title")
                        .replace("<em class=\"keyword\">", "")
                        .replace("</em>", ""));
    }

    @Override
    public String getUrl() throws ParsingException {
        BilibiliStreamLinkHandlerFactory factory = new BilibiliStreamLinkHandlerFactory();
        return factory.getUrl(factory.getId(item.getString("arcurl")));
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return "https:" + item.getString("pic");
    }

    @Override
    public StreamType getStreamType() throws ParsingException {
        return StreamType.VIDEO_STREAM;
    }

    @Override
    public long getDuration() throws ParsingException {
        return getDurationFromString(item.getString("duration"));
    }

    @Override
    public long getViewCount() throws ParsingException {
        return item.getLong("play");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return item.getString("author");
    }

    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        return item.getString("upic").replace("http:", "https:");
    }

    @Override
    public String getTextualUploadDate() throws ParsingException {
        return new SimpleDateFormat("yyyy-MM-dd HH:mm:ss").format(new Date(item.getInt("pubdate") * 1000L));
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        return new DateWrapper(LocalDateTime.parse(
                getTextualUploadDate(), DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")).atOffset(ZoneOffset.ofHours(+8)));
    }

}
