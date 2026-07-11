package org.schabi.newpipe.extractor.services.niconico.extractors;

import org.schabi.newpipe.extractor.bulletComments.BulletCommentsInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Date;

import static org.schabi.newpipe.extractor.services.niconico.extractors.NiconicoBulletCommentsExtractor.decodeVarInt;

public class NiconicoBulletCommentsNewInfoItemExtractor  implements BulletCommentsInfoItemExtractor {
    byte[] data;
    long startAt;
    NiconicoBulletCommentsNewInfoItemExtractor(byte[] data, long startAt) {
        this.data = data;
        this.startAt = startAt;
    }
    @Override
    public String getCommentText() throws ParsingException {
        String encodedText = new String(this.data, StandardCharsets.UTF_8);
        try {
            String[] parts = encodedText.split("\u0012");
            String firstPart = parts[0];

            return firstPart.substring(1);
        } catch (Exception e) {
            return "";
        }

    }

    @Override
    public Duration getDuration() throws ParsingException {
//        return Duration.ofMillis(new Date().getTime() - startAt);
        System.out.println(decodeVarInt(this.data, (byte) 0x18));
        return Duration.ofMillis(decodeVarInt(this.data, (byte) 0x18) * 10L + 15000L);
    }

    @Override
    public boolean isLive() throws ParsingException {
        return true;
    }
}
