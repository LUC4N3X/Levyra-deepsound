package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.apache.commons.lang3.StringUtils;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;

public class NiconicoPlaylistInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject itemObject;

    public NiconicoPlaylistInfoItemExtractor(JsonObject itemObject) {
        this.itemObject = itemObject;
    }

    @Override
    public String getName() throws ParsingException {
        return StringUtils.defaultIfEmpty(itemObject.getString("name"), itemObject.getString("title"));
    }

    @Override
    public String getUrl() throws ParsingException {
        return String.format("https://www.nicovideo.jp/user/%s/mylist/%s", itemObject.getObject("owner").getString("id"), itemObject.getLong("id"));
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return itemObject.getString("thumbnailUrl");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return itemObject.getObject("owner").getString("name");
    }

    @Override
    public long getStreamCount() throws ParsingException {
        // if itemsCount not exists then use videoCount
        return Math.max(itemObject.getLong("itemsCount"), itemObject.getLong("videoCount"));
    }
}
