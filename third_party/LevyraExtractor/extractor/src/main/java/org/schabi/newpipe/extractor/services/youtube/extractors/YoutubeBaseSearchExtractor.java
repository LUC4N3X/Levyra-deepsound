package org.schabi.newpipe.extractor.services.youtube.extractors;

import org.schabi.newpipe.extractor.search.filter.FilterItem;

import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandler;
import org.schabi.newpipe.extractor.search.SearchExtractor;

public abstract class YoutubeBaseSearchExtractor extends SearchExtractor {
    public YoutubeBaseSearchExtractor(final StreamingService service,
                                      final SearchQueryHandler linkHandler) {
        super(service, linkHandler);
    }

    @SuppressWarnings("unchecked")
    protected  <T extends FilterItem> T getSelectedContentFilterItem() {
        final FilterItem filterItem = getLinkHandler().getContentFilters().get(0);

        if (filterItem != null) {
            return (T) filterItem;
        }
        throw new RuntimeException("no content filter set");
    }
}
