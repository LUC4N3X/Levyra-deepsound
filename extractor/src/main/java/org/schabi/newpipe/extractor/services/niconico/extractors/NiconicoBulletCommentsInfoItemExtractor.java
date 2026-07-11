package org.schabi.newpipe.extractor.services.niconico.extractors;

import java.util.Date;
import java.util.HashMap;

import org.apache.commons.lang3.StringUtils;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem.Position;

import com.grack.nanojson.JsonObject;

import java.time.Duration;

import javax.annotation.Nonnull;

/*
See https://dic.nicovideo.jp/a/%E3%82%B3%E3%83%A1%E3%83%B3%E3%83%88
 */
public class NiconicoBulletCommentsInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    @Nonnull
    protected JsonObject json;
    @Nonnull
    protected String url;
    @Nonnull
    protected String[] mailStyles;
    private final long startAt;
    private boolean isLive;

    protected final HashMap<String, Integer> colorMap = new HashMap<String, Integer>() {
        {
            put("white", 0xFFFFFF);
            put("red", 0xFF0000);
            put("pink", 0xFF8080);
            put("orange", 0xFFC000);
            put("yellow", 0xFFFF00);
            put("green", 0x00FF00);
            put("cyan", 0x00FFFF);
            put("blue", 0x0000FF);
            put("purple", 0xC000FF);
            put("black", 0x000000);
            // Premium colors
            put("white2", 0xCCCCCC);
            put("niconicoWhite", 0xCCCC99);
            put("red2", 0xCC0033);
            put("truered", 0xCC0033);
            put("pink2", 0xFF33CC);
            put("orange2", 0xFF6600);
            put("passionorange", 0xFF7F00);
            put("yellow2", 0x999900);
            put("madyellow", 0x999900);
            put("green2", 0x00CC66);
            put("elementalgreen", 0x00CC66);
            put("cyan2", 0x00CCCC);
            put("blue2", 0x3399FF);
            put("marineblue", 0x3399FF);
            put("purple2", 0x6633FF);
            put("nobleviolet", 0x6633FF);
            put("black2", 0x666666);
        }
    };

    protected final HashMap<String, Position> positionMap = new HashMap<String, Position>() {
        {
            put("top", Position.TOP);
            put("bottom", Position.BOTTOM);
            put("ue", Position.TOP);
            put("shita", Position.BOTTOM);
        }
    };

    protected final HashMap<String, Double> sizeMap = new HashMap<String, Double>() {
        {
            put("small", 0.5);
            put("big", 0.7);
        }
    };

    NiconicoBulletCommentsInfoItemExtractor(@Nonnull final JsonObject json,
                                            @Nonnull final String url, long startAt, boolean isLive) {
        this.json = json;
        this.url = url;
        this.startAt = startAt;
        this.mailStyles = new String[0];
        this.isLive = isLive;
        if (json.containsKey("mail")) {
            try {
                this.mailStyles = json.getString("mail").split(" ");
            } catch (final Exception e) {
                e.printStackTrace();
            }
        } else if (json.containsKey("commands")){
            try {
                this.mailStyles = json.getArray("commands").toArray(new String[0]);
            } catch (final Exception e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public String getName() throws ParsingException {
        return null;
    }

    @Override
    public String getUrl() throws ParsingException {
        return url;
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return null;
    }

    @Override
    public String getCommentText() throws ParsingException {
        try {
            String text = StringUtils.defaultIfEmpty(json.getString("content"), json.getString("body"));
            if(text.startsWith("/emotion ")){
                text = text.substring(9);
            }
            return text;
        } catch (final Exception e) {
            throw new ParsingException("Could not get comment text", e);
        }
    }

    @Override
    public int getArgbColor() throws ParsingException {
        for (final String style : mailStyles) {
            if (colorMap.containsKey(style)) {
                return colorMap.get(style) + 0xFF000000;
            }
        }
        return 0xFFFFFFFF;
    }

    @Nonnull
    @Override
    public BulletCommentsInfoItem.Position getPosition() throws ParsingException {
        for (final String style : mailStyles) {
            if (positionMap.containsKey(style)) {
                return positionMap.get(style);
            }
        }
        return Position.REGULAR;
    }

    @Override
    public double getRelativeFontSize() throws ParsingException {
        for (final String style : mailStyles) {
            if (sizeMap.containsKey(style)) {
                return sizeMap.get(style);
            }
        }
        return 0.7;
    }

    @Nonnull
    @Override
    public Duration getDuration() throws ParsingException {
        if(isLive){
            return Duration.ofMillis(new Date().getTime() - startAt);
        }
        try {
            if(json.containsKey("vpos")){
                return Duration.ofMillis(json.getLong("vpos", 0) * 10);
            } else {
                return Duration.ofMillis(json.getLong("vposMs", 0));
            }
        } catch (final Exception e) {
           return Duration.ofMillis(new Date().getTime() - startAt);
        }
    }
}
