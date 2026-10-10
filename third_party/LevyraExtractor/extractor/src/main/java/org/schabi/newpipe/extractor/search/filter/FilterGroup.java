package org.schabi.newpipe.extractor.search.filter;

import java.util.HashMap;
import java.util.Map;

public class FilterGroup {
    public final String groupName;
    public final FilterItem[] filterItems;
    public final int identifier;
    public boolean onlyOneCheckable;

    public FilterGroup(final int identifier, final String groupName,
                       final boolean onlyOneCheckable,
                       final FilterItem[] filterItems) {
        this.identifier = identifier;
        this.groupName = groupName;
        this.onlyOneCheckable = onlyOneCheckable;
        this.filterItems = filterItems;
    }

    public static class Builder {
        public final Map<Integer, FilterItem> filtersMap = new HashMap<>();
        private final Map<String, Integer> filtersSortGroupMap = new HashMap<>();
        public int noOfSortFilters;
        public FilterGroup[] filterGroups = null;
        /**
         * used to give each added filter an unique identifier
         */
        private int identifierIncrement;
        private int noOfContentFilters;

        public Builder() {
            this.noOfSortFilters = 0;
            this.noOfContentFilters = 0;
            this.identifierIncrement = 0;
        }

        public int addSortItem(final FilterItem filter) {
            this.noOfSortFilters++;
            return addItem(filter);
        }

        public int addFilterItem(final FilterItem filter) {
            this.noOfContentFilters++;
            return addItem(filter);
        }

        private int addItem(final FilterItem filter) {
            final FilterItem.Builder filterItemBuilder =
                    FilterItem.builder(filter);
            filterItemBuilder.setIdentifier(this.identifierIncrement);
            filtersMap.put(this.identifierIncrement, filterItemBuilder.build());
            return this.identifierIncrement++;
        }

        public int getNoOfSortFilters() {
            return this.noOfSortFilters;
        }

        public FilterGroup createSortGroup(final String groupName,
                                              final boolean onlyOneCheckable,
                                              final FilterItem[] filterItems) {
            final int identifier;
            if (filtersSortGroupMap.containsKey(groupName)) {
                identifier = filtersSortGroupMap.get(groupName);
            } else {
                identifier = this.identifierIncrement++;
                filtersSortGroupMap.put(groupName, identifier);
            }

            return new FilterGroup(identifier, groupName, onlyOneCheckable, filterItems);
        }

        public FilterItem getFilterForId(final int filterId) {
            return filtersMap.get(filterId);
        }
    }
}
