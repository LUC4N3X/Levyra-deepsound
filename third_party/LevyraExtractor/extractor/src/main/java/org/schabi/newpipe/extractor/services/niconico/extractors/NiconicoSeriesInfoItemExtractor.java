package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.playlist.PlaylistInfoItemExtractor;

public class NiconicoSeriesInfoItemExtractor implements PlaylistInfoItemExtractor {
    private final JsonObject itemObject;
    private final String name;

    public NiconicoSeriesInfoItemExtractor(JsonObject itemObject, String name) {
        this.itemObject = itemObject;
        this.name = name;
    }

    @Override
    public String getName() throws ParsingException {
        return itemObject.getString("title");
    }

    @Override
    public String getUrl() throws ParsingException {
        return "https://www.nicovideo.jp/series/" + itemObject.getLong("id");
    }

    @Override
    public String getThumbnailUrl() throws ParsingException {
        return itemObject.getString("thumbnailUrl");
    }

    @Override
    public String getUploaderName() throws ParsingException {
        return name;
    }

    @Override
    public long getStreamCount() throws ParsingException {
        return itemObject.getInt("itemsCount");
    }
}
