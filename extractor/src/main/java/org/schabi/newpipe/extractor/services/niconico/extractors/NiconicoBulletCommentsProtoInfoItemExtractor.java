package org.schabi.newpipe.extractor.services.niconico.extractors;

import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.niconico.protobuf.BulletComment;

import java.time.Duration;

public class NiconicoBulletCommentsProtoInfoItemExtractor implements BulletCommentsInfoItemExtractor {
    BulletComment.MessageItem object;
    long startAt;
    NiconicoBulletCommentsProtoInfoItemExtractor(BulletComment.MessageItem object) {
        this.object = object;
    }
    @Override
    public String getCommentText() throws ParsingException {
        return object.message.text_message.text;
    }

    @Override
    public Duration getDuration() throws ParsingException {
        return Duration.ofMillis(object.message.text_message.time_to_now - 6000);
    }
}
