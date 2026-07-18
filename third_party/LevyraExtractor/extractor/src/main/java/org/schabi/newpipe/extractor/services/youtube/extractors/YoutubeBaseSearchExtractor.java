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

    protected <T extends FilterItem> T getSelectedContentFilterItem(final Class<T> filterType) {
        final FilterItem filterItem = getLinkHandler().getContentFilters().get(0);
        if (filterType.isInstance(filterItem)) {
            return filterType.cast(filterItem);
        }
        throw new IllegalStateException("Unexpected content filter type");
    }
}
