package org.schabi.newpipe.extractor.search.filter;

import org.schabi.newpipe.extractor.services.youtube.search.filter.YoutubeFilters;

import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deriving class must always call {@link SearchFiltersBase#init()} and {@link SearchFiltersBase#build()}
 * in constructor manually.
 */
public abstract class SearchFiltersBase {

    protected final Map<Integer, Filter> sortFilterVariants = new HashMap<>();
    protected int defaultContentFilterId = Filter.ITEM_IDENTIFIER_UNKNOWN;
    protected Integer[] contentFilters;
    protected FilterGroup.Builder builder = new FilterGroup.Builder();
    protected List<FilterItem> selectedContentFilter = null;
    protected List<FilterItem> selectedSortFilter;
    protected Filter contentFiltersVariant;
    protected List<FilterGroup> contentFiltersList = new LinkedList<>();
    private Filter allSortFiltersVariant = null;

    /**
     * Set the user selected sort filters which the user has selected in the UI.
     *
     * @param selectedSortFilter list with sort filters identifiers
     */
    public void setSelectedSortFilter(final List<FilterItem> selectedSortFilter) {
        this.selectedSortFilter = selectedSortFilter;
    }

    /**
     * Set the selected content filter
     *
     * @param selectedContentFilter the name of the content filter
     */
    public void setSelectedContentFilter(final List<FilterItem> selectedContentFilter) {
        this.selectedContentFilter = selectedContentFilter;
    }

    /**
     * Evaluate content and sort filters. This method should be run after:
     * {@link #setSelectedContentFilter(List)} and {@link #setSelectedSortFilter(List)}
     *
     * @return the query that should be appended to the searchUrl/whatever
     */
    public String evaluateSelectedFilters(final String searchString) {
        return "";
    }

    /**
     * Evaluate content filters. This method should be run after:
     * {@link #setSelectedContentFilter(List)}
     *
     * @return the sortQuery that should be appended to the searchUrl/whatever
     */
    public String evaluateSelectedContentFilters() {
        return "";
    }

    /**
     * Evaluate sort filters. This method should be run after:
     * {@link #setSelectedSortFilter(List)}
     *
     * @return the contentQuery that should be appended to the searchUrl/whatever
     */
    public String evaluateSelectedSortFilters() {
        return "";
    }

    /**
     * create all 'sort' and 'content filter' items and all 'sort filter variants' in this method.
     * See eg. {@link YoutubeFilters#init()}
     */
    protected abstract void init();

    public Integer[] getAvailableContentFilter() {
        return contentFilters;
    }

    protected void build() {
        final Set<Map.Entry<Integer, Filter>> entrySet =
                this.sortFilterVariants.entrySet();
        this.contentFilters = new Integer[entrySet.size()];
        int count = 0;
        for (final Map.Entry<Integer, Filter> entry : entrySet) {
            this.contentFilters[count++] = entry.getKey();

        }

        this.contentFiltersVariant = new Filter.Builder(
                this.contentFiltersList.toArray(new FilterGroup[0]))
                .setNoOfFilters(builder.getNoOfSortFilters()).build();
    }

    /**
     * Add content Filter SortVariants.
     * <p>
     * Calling the method the first time {@variant} will be set
     * as all variants sortFilter when called {@link #getSortFilters()}
     *
     * @param contentFilterId
     * @param variant
     */
    protected void addContentFilterSortVariant(
            final int contentFilterId,
            final Filter variant) {
        if (this.allSortFiltersVariant == null) {
            this.allSortFiltersVariant = variant;
            return ; // First should be the set of all the available filters , not need to really have this group
            // and this will not be shown
        }
        this.sortFilterVariants.put(contentFilterId, variant);
    }

    public Filter getContentFilterSortFilterVariant(
            final int contentFilterName) {
        return this.sortFilterVariants.get(contentFilterName);
    }

    public Filter getSortFilters() {
        return this.allSortFiltersVariant;
    }

    public Filter getContentFilters() {
        return this.contentFiltersVariant;
    }

    public FilterItem getFilterItem(final int filterId) {
        return builder.getFilterForId(filterId);
    }

    protected void addContentFilter(final FilterGroup hds) {
        this.contentFiltersList.add(hds);
    }
}
