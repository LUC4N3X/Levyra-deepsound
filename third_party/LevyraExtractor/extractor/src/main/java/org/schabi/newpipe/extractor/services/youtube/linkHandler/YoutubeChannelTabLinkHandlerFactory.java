package org.schabi.newpipe.extractor.services.youtube.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.net.URLEncoder;
import java.util.Collections;
import java.util.List;

import javax.annotation.Nonnull;

import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

public final class YoutubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    public static final String SORT_LATEST = "latest";
    public static final String SORT_POPULAR = "popular";
    public static final String SORT_OLDEST = "oldest";

    private static final YoutubeChannelTabLinkHandlerFactory INSTANCE =
            new YoutubeChannelTabLinkHandlerFactory();
    private final FilterGroup.Builder filterBuilder;
    private final Filter availableSortFilter;

    private YoutubeChannelTabLinkHandlerFactory() {
        filterBuilder = new FilterGroup.Builder();

        final int latestFilterId = filterBuilder.addSortItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, SORT_LATEST));
        final int popularFilterId = filterBuilder.addSortItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, SORT_POPULAR));
        final int oldestFilterId = filterBuilder.addSortItem(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, SORT_OLDEST));

        availableSortFilter = new Filter.Builder(new FilterGroup[]{
                filterBuilder.createSortGroup("sortby", true, new FilterItem[]{
                        filterBuilder.getFilterForId(latestFilterId),
                        filterBuilder.getFilterForId(popularFilterId),
                        filterBuilder.getFilterForId(oldestFilterId),
                })
        }).build();
    }

    public static YoutubeChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    public static String getUrlSuffix(final String tab) throws ParsingException {
        switch (tab) {
            case ChannelTabs.VIDEOS:
                return "/videos";
            case ChannelTabs.PLAYLISTS:
                return "/playlists";
            case ChannelTabs.PODCASTS:
                return "/podcasts";
            case ChannelTabs.LIVESTREAMS:
                return "/streams";
            case ChannelTabs.SHORTS:
                return "/shorts";
            case ChannelTabs.CHANNELS:
                return "/channels";
            case ChannelTabs.SEARCH:
                return "/search";
        }
        throw new ParsingException("tab " + tab + " not supported");
    }

    @Override
    public String getUrl(final String id,
                         @Nonnull final List<FilterItem> selectedContentFilter,
                         final List<FilterItem> selectedSortFilter)
            throws ParsingException {
        return "https://www.youtube.com/" + id
                + getUrlSuffix(selectedContentFilter.get(0).getName())
                + getSortUrlQuery(selectedSortFilter);
    }

    private static String getSortUrlQuery(final List<FilterItem> selectedSortFilter)
            throws ParsingException {
        if (selectedSortFilter == null || selectedSortFilter.isEmpty()) {
            return "";
        }

        final String selectedSortFilterName = selectedSortFilter.get(0).getName();
        switch (selectedSortFilterName) {
            case SORT_LATEST:
            case SORT_POPULAR:
            case SORT_OLDEST:
                return "?sort=" + selectedSortFilterName;
            default:
                throw new ParsingException("Unsupported YouTube channel tab sort filter: "
                        + selectedSortFilterName);
        }
    }

    @Override
    public ListLinkHandler fromUrl(final String url, final String baseUrl) throws ParsingException {
        if (url == null) {
            throw new IllegalArgumentException("url may not be null");
        }
        if (!acceptUrl(url)) {
            throw new ParsingException("URL not accepted: " + url);
        }

        final String id = getId(url);
        final String tab = getTabFromUrl(url);
        final List<FilterItem> contentFilter = Collections.singletonList(
                new FilterItem(Filter.ITEM_IDENTIFIER_UNKNOWN, tab));
        String cleanUrl = getUrl(id, contentFilter, null, baseUrl);
        if (ChannelTabs.SEARCH.equals(tab)) {
            cleanUrl = appendSearchQueryIfNeeded(cleanUrl, getSearchQueryFromUrl(url));
        }
        return new ListLinkHandler(url, cleanUrl, id, contentFilter, null);
    }

    @Override
    public Filter getAvailableSortFilter() {
        return availableSortFilter;
    }

    @Override
    public Filter getContentFilterSortFilterVariant(final int contentFilterId) {
        return availableSortFilter;
    }

    @Override
    public FilterItem getFilterItem(final int filterId) {
        return filterBuilder.getFilterForId(filterId);
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return YoutubeChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        try {
            getId(url);
        } catch (final ParsingException e) {
            return false;
        }
        return true;
    }

    public static String getSearchQueryFromUrl(final String url) throws ParsingException {
        try {
            final URL urlObj = Utils.stringToURL(url);
            final String query = firstNonEmptyQueryValue(urlObj, "query", "search_query", "q");
            return query == null ? "" : query;
        } catch (final Exception e) {
            throw new ParsingException("Could not parse channel search query", e);
        }
    }

    public static String appendSearchQueryIfNeeded(final String url,
                                                   final String query) throws ParsingException {
        if (isNullOrEmpty(query)) {
            return url;
        }

        try {
            return url + "?query=" + URLEncoder.encode(query, UTF_8).replace("+", "%20");
        } catch (final Exception e) {
            throw new ParsingException("Could not encode channel search query", e);
        }
    }

    private String getTabFromUrl(final String url) throws ParsingException {
        try {
            final URL urlObj = Utils.stringToURL(url);
            final String[] pathSegments = urlObj.getPath().split("/");
            for (int i = pathSegments.length - 1; i >= 0; i--) {
                switch (pathSegments[i]) {
                    case "videos":
                        return ChannelTabs.VIDEOS;
                    case "playlists":
                        return ChannelTabs.PLAYLISTS;
                    case "podcasts":
                        return ChannelTabs.PODCASTS;
                    case "streams":
                        return ChannelTabs.LIVESTREAMS;
                    case "shorts":
                        return ChannelTabs.SHORTS;
                    case "channels":
                        return ChannelTabs.CHANNELS;
                    case "search":
                        return ChannelTabs.SEARCH;
                }
            }
        } catch (final Exception e) {
            throw new ParsingException("Could not parse channel tab URL: " + e.getMessage(), e);
        }

        return ChannelTabs.VIDEOS;
    }

    private static String firstNonEmptyQueryValue(final URL url,
                                                  final String... names) {
        for (final String name : names) {
            final String value = Utils.getQueryValue(url, name);
            if (!isNullOrEmpty(value)) {
                return value;
            }
        }
        return null;
    }
}
