package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItem;
import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.time.Duration;

public class BilibiliLiveBulletCommentsInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    private final JsonArray data;
    private final long startTime;

    public BilibiliLiveBulletCommentsInfoItemExtractor(JsonObject message, long startTime) throws JsonParserException {
        data = message.getArray("info");
        this.startTime = startTime;
    }

    @Override
    public String getCommentText() throws ParsingException {
        return data.getString(1);
    }

    @Override
    public int getArgbColor() throws ParsingException {
        return 0xFF000000 + data.getArray(0).getInt(3);
    }

    @Override
    public BulletCommentsInfoItem.Position getPosition() throws ParsingException {
        switch (data.getArray(0).getInt(1)) {
            case 1:
                return BulletCommentsInfoItem.Position.REGULAR;
            case 4:
                return BulletCommentsInfoItem.Position.BOTTOM;
            default:
                return BulletCommentsInfoItem.Position.TOP;
        }
    }

    @Override
    public double getRelativeFontSize() throws ParsingException {
        return 0.64;
    }

    @Override
    public Duration getDuration() throws ParsingException {
//        return Duration.ofMillis(data.getArray(0).getLong(4) - startTime * 1000);
        return Duration.ZERO;
    }

    @Override
    public boolean isLive() throws ParsingException {
        return true;
    }
}
