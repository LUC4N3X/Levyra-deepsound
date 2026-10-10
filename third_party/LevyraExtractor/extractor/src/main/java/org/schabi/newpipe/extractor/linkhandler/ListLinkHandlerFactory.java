package org.schabi.newpipe.extractor.linkhandler;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.utils.Utils;

import java.util.ArrayList;
import java.util.List;

public abstract class ListLinkHandlerFactory extends LinkHandlerFactory {

    ///////////////////////////////////
    // To Override
    ///////////////////////////////////

    public abstract String getUrl(String id, List<FilterItem> contentFilter, List<FilterItem> sortFilter)
            throws ParsingException;

    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter,
                         final String baseUrl) throws ParsingException {
        return getUrl(id, contentFilter, sortFilter);
    }

    ///////////////////////////////////
    // Logic
    ///////////////////////////////////

    @Override
    public ListLinkHandler fromUrl(final String url) throws ParsingException {
        final String polishedUrl = Utils.followGoogleRedirectIfNeeded(url);
        final String baseUrl = Utils.getBaseUrl(polishedUrl);
        return fromUrl(polishedUrl, baseUrl);
    }

    @Override
    public ListLinkHandler fromUrl(final String url, final String baseUrl) throws ParsingException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }

        return new ListLinkHandler(super.fromUrl(url, baseUrl));
    }

    @Override
    public ListLinkHandler fromId(final String id) throws ParsingException {
        return new ListLinkHandler(super.fromId(id));
    }

    @Override
    public ListLinkHandler fromId(final String id, final String baseUrl) throws ParsingException {
        return new ListLinkHandler(super.fromId(id, baseUrl));
    }

    public ListLinkHandler fromQuery(final String query,
                                     final List<FilterItem> contentFilter,
                                     final List<FilterItem> sortFilter) throws ParsingException {
        final String url = getUrl(query, contentFilter, sortFilter);
        return new ListLinkHandler(url, url, query, contentFilter, sortFilter);
    }

    public ListLinkHandler fromQuery(final String id,
                                     final List<FilterItem> contentFilters,
                                     final List<FilterItem> sortFilter,
                                     final String baseUrl) throws ParsingException {
        final String url = getUrl(id, contentFilters, sortFilter, baseUrl);
        return new ListLinkHandler(url, url, id, contentFilters, sortFilter);
    }


    /**
     * For making ListLinkHandlerFactory compatible with LinkHandlerFactory we need to override
     * this, however it should not be overridden by the actual implementation.
     *
     * @return the url corresponding to id without any filters applied
     */
    public String getUrl(final String id) throws ParsingException {
        return getUrl(id, new ArrayList<>(0), null);
    }

    @Override
    public String getUrl(final String id, final String baseUrl) throws ParsingException {
        return getUrl(id, new ArrayList<>(0), null, baseUrl);
    }

    /**
     * Will returns content filter the corresponding extractor can handle like "channels", "videos",
     * "music", etc.
     *
     * @return filter that can be applied when building a query for getting a list
     */
    public Filter getAvailableContentFilter() {
        return null;
    }

    /**
     * Will returns sort filter the corresponding extractor can handle like "A-Z", "oldest first",
     * "size", etc.
     *
     * @return filter that can be applied when building a query for getting a list
     */
    public Filter getAvailableSortFilter() {
        return null;
    }

    public Filter getContentFilterSortFilterVariant(final int contentFilterId) {
        return null;
    }

    public FilterItem getFilterItem(final int filterId) {
        return null;
    }
}
