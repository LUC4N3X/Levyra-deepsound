package org.schabi.newpipe.extractor.services.media_ccc.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;

public final class MediaCCCFilters extends SearchFiltersBase {

    public static final String ALL = "all";
    public static final String CONFERENCES = "conferences";
    public static final String EVENTS = "events";

    public MediaCCCFilters() {
        init();
        build();
    }

    @Override
    public String evaluateSelectedFilters(final String searchString) {
        return null;
    }

    @Override
    protected void init() {
        /* content filters */
        final int contentFilterAll = builder.addFilterItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, ALL));
        final int contentFilterConferences = builder.addFilterItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, CONFERENCES));
        final int contentFilterEvents = builder.addFilterItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, EVENTS));

        // 'ALL' this is the default search content filter.
        this.defaultContentFilterId = contentFilterAll;

        /* content filters with sort filters */
        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentFilterAll),
                builder.getFilterForId(contentFilterConferences),
                builder.getFilterForId(contentFilterEvents),
        }));
    }
}
