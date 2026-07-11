package org.schabi.newpipe.extractor.services.bilibili.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.channel.ChannelInfoItemExtractor;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.bilibili.linkHandler.BilibiliChannelLinkHandlerFactory;

public class BilibiliSearchResultChannelInfoItemExtractor implements ChannelInfoItemExtractor {
    JsonObject data;

    BilibiliSearchResultChannelInfoItemExtractor(JsonObject json) {
        this.data = json;
    }

    @Override
    public String getName() throws ParsingException {
        return data.getString("uname");
    }

    @Override
    public String getUrl() throws ParsingException {
        return BilibiliChannelLinkHandlerFactory.baseUrl + data.getLong("mid");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return "https:" + data.getString("upic");
    }

    @Override
    public String getDescription() throws ParsingException {
        return data.getString("usign");
    }

    @Override
    public long getSubscriberCount() throws ParsingException {
        return data.getLong("fans");
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return data.getLong("videos");
    }

    @Override
    public boolean isVerified() throws ParsingException {
        return false;
    }
}
