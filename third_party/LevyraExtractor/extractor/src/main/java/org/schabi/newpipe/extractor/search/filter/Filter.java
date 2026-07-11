package org.schabi.newpipe.extractor.search.filter;

public final class Filter {

    public static final int ITEM_IDENTIFIER_UNKNOWN = -1;
    private final FilterGroup[] sortGroups;
    private int size = 0;

    private Filter(final FilterGroup[] sortGroups) {
        this.sortGroups = sortGroups;
    }

    public FilterGroup[] getFilterGroups() {
        return sortGroups;
    }

    public static class Builder {
        final Filter filter;

        public Builder(final FilterGroup[] sortGroups) {
            filter = new Filter(sortGroups);
        }

        public Builder setNoOfFilters(final int noOfFilters) {
            filter.size = noOfFilters;
            return this;
        }

        public Filter build() {
            return filter;
        }
    }
}
