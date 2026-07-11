package org.schabi.newpipe.extractor.services.niconico.linkHandler;

import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.SearchQueryHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;
import org.schabi.newpipe.extractor.services.niconico.search.filter.NiconicoFilters;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.List;

public class NiconicoSearchQueryHandlerFactory extends SearchQueryHandlerFactory {
    public static final int ITEMS_PER_PAGE = 10;
    private static final String SEARCH_API_URL
            = "https://api.search.nicovideo.jp/api/v2/snapshot/video/contents/search";

    private final NiconicoFilters searchFilters = new NiconicoFilters();

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> selectedContentFilter,
                         final List<FilterItem> selectedSortFilter) throws ParsingException {

        searchFilters.setSelectedSortFilter(selectedSortFilter);
        searchFilters.setSelectedContentFilter(selectedContentFilter);

        String filterQuery = searchFilters.evaluateSelectedFilters(null);

        try {
            if(selectedContentFilter.get(0).getName().equals("lives")){
                return NiconicoService.LIVE_SEARCH_URL + "?keyword=" + URLEncoder.encode(id, UTF_8) + "&page=1";
            } else if(selectedContentFilter.get(0).getName().equals("playlists")){
                return NiconicoService.PLAYLIST_SEARCH_API_URL + "&keyword=" + URLEncoder.encode(id, UTF_8) + filterQuery + "&types=mylist&pageSize=10&page=1";
            } else {
                if(filterQuery.length() > 0) {
                    filterQuery = "?" + filterQuery.substring(1);
                } else {
                    filterQuery = "?sort=h&order=d";
                }
                return NiconicoService.SEARCH_URL + URLEncoder.encode(id, UTF_8) + filterQuery + "&page=1";
            }
        } catch (final UnsupportedEncodingException e) {
            throw new ParsingException("could not encode query.");
        }
    }

    @Override
    public Filter getAvailableContentFilter() {
        return searchFilters.getContentFilters();
    }

    @Override
    public FilterItem getFilterItem(final int filterId) {
        return searchFilters.getFilterItem(filterId);
    }

    @Override
    public Filter getAvailableSortFilter() {
        return searchFilters.getSortFilters();
    }

    @Override
    public Filter getContentFilterSortFilterVariant(final int contentFilterId) {
        return searchFilters.getContentFilterSortFilterVariant(contentFilterId);
    }
}
