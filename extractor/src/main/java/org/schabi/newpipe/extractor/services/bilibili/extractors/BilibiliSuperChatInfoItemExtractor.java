package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.time.Duration;

public class BilibiliSuperChatInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    private final JsonObject data;
    private final long startTime;

    public BilibiliSuperChatInfoItemExtractor(JsonObject message, long startTime) throws JsonParserException {
        data = message.getObject("data");
        this.startTime = startTime;
    }

    @Override
    public String getCommentText() throws ParsingException {
        return String.format("(¥%s) ", data.getInt("price")) + data.getString("message");
    }

    @Override
    public int getArgbColor() throws ParsingException {
        return 0xFF000000 + Integer.parseInt(data.getString("background_bottom_color").split("#")[1], 16);
    }

    @Override
    public BulletCommentsInfoItem.Position getPosition() throws ParsingException {
        return BulletCommentsInfoItem.Position.SUPERCHAT;
    }

    @Override
    public double getRelativeFontSize() {
        return 0.64;
    }

    @Override
    public Duration getDuration() throws ParsingException {
        return Duration.ofSeconds(data.getLong("start_time") - startTime);
    }

    @Override
    public boolean isLive() throws ParsingException {
        return true;
    }

    //    @Override
//    public int getLastingTime() {
//        int price = data.getInt("price");
//        if(price > 700){
//            return 60000;
//        } else if () {
//
//        }
//        return data.getInt("ti")
//    }
}
