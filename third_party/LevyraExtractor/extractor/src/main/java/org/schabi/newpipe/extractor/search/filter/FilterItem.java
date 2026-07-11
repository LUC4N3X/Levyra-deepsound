package org.schabi.newpipe.extractor.search.filter;

import java.io.Serializable;

public class FilterItem implements Serializable {
    private final String name;
    private int identifier;
    /**
     * check if a {@link #FilterItem} was build using the {@link Builder}
     */
    private boolean isBuild = false;

    public FilterItem(final int identifier, final String name) {
        this.identifier = identifier;
        this.name = name;
    }

    public static Builder builder(final FilterItem filterItem) {
        return new Builder(filterItem);
    }

    public int getIdentifier() {
        return this.identifier;
    }

    public String getName() {
        return this.name;
    }

    public static class Builder implements Serializable{

        private final FilterItem filterItem;

        public Builder(final FilterItem filterItem) {
            this.filterItem = filterItem;
        }

        public Builder setIdentifier(final int itemIdentifier) {
            filterItem.identifier = itemIdentifier;
            return this;
        }

        public FilterItem build() {
            if (filterItem.isBuild) {
                throw new RuntimeException("filter is already build()");
            }
            if (filterItem.identifier == Filter.ITEM_IDENTIFIER_UNKNOWN) {
                throw new RuntimeException("itemIdentifier is not set");
            }

            filterItem.isBuild = true;
            return filterItem;
        }
    }
}
