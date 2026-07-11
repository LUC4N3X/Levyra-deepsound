package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonObject;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeBulletCommentsInfoItemExtractor;

public class YoutubeBulletCommentPair {
    private final JsonObject data;
    private final long offsetDuration; // the expected offset of the comment from the start of the video
    public YoutubeBulletCommentPair(JsonObject item, long offsetDuration) {
        this.offsetDuration = offsetDuration;
        this.data = item;
    }

    public JsonObject getData() {
        return data;
    }

    public long getOffsetDuration() {
        return offsetDuration;
    }
}
