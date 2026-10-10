package org.schabi.newpipe.extractor.services.bandcamp.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;

public final class BandcampFilters extends SearchFiltersBase {

    private static final String ALL = "all";
    private static final String ARTISTS = "artists & labels";
    private static final String ALBUMS = "albums";
    private static final String TRACKS = "tracks";
    private static final String FANS = "fans";

    public BandcampFilters() {
        init();
        build();
    }

    @Override
    public String evaluateSelectedContentFilters() {
        if (selectedSortFilter != null) {
            String sortQuery = "";

            if (selectedContentFilter != null && !selectedContentFilter.isEmpty()) {
                final BandcampContentFilterItem contentItem =
                        // we assume that there is just one content filter
                        (BandcampContentFilterItem) selectedContentFilter.get(0);
                if (contentItem != null) {
                    if (!contentItem.query.isEmpty()) {
                        sortQuery = "&" + contentItem.query;
                    }
                }
            }
            return sortQuery;
        }
        return "";
    }

    @Override
    protected void init() {
        /* content filters */
        final int contentFilterAll = builder.addFilterItem(
                new BandcampContentFilterItem(ALL, ""));
        final int contentFilterArtists = builder.addFilterItem(
                new BandcampContentFilterItem(ARTISTS, "item_type=b"));
        final int contentFilterAlbums = builder.addFilterItem(
                new BandcampContentFilterItem(ALBUMS, "item_type=a"));
        final int contentFilterTracks = builder.addFilterItem(
                new BandcampContentFilterItem(TRACKS, "item_type=t"));
        // TODO no FANS extractor in BandcampSearchExtractor -> no content filter here
        // final int contentFilterFans = builder.addFilterItem(
        //         new BandcampContentFilterItem(FANS, "item_type=f"));

        // 'ALL' this is the default search content filter.
        this.defaultContentFilterId = contentFilterAll;

        /* content filters with sort filters */
        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentFilterAll),
                builder.getFilterForId(contentFilterArtists),
                builder.getFilterForId(contentFilterAlbums),
                builder.getFilterForId(contentFilterTracks),
                // builder.getFilterForId(contentFilterFans),
        }));
    }

    public static class BandcampContentFilterItem extends FilterItem {
        private final String query;

        public BandcampContentFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }
    }
}
