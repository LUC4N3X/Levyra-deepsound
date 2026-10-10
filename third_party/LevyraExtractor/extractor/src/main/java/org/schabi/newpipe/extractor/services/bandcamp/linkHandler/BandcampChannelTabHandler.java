package org.schabi.newpipe.extractor.services.bandcamp.linkHandler;

import com.grack.nanojson.JsonArray;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.Collections;

public class BandcampChannelTabHandler extends ListLinkHandler {
    private final JsonArray discographs;

    public BandcampChannelTabHandler(final String url, final String id, final String tab,
                                     final JsonArray discographs) {
        super(url, url, id, Collections.singletonList(new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, tab)), null);
        this.discographs = discographs;
    }

    public JsonArray getDiscographs() {
        return discographs;
    }
}
