package org.schabi.newpipe.extractor;

import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import javax.annotation.Nullable;
import java.util.*;

/*
 * Created by Christian Schabesberger on 12.02.17.
 *
 * Copyright (C) Christian Schabesberger 2017 <chris.schabesberger@mailbox.org>
 * InfoItemsCollector.java is part of NewPipe.
 *
 * NewPipe is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe.  If not, see <http://www.gnu.org/licenses/>.
 */

public abstract class InfoItemsCollector<I extends InfoItem, E extends InfoItemExtractor>
        implements Collector<I, E> {

    private final List<I> itemList = new ArrayList<>();
    private final List<Throwable> errors = new ArrayList<>();
    private final int serviceId;
    @Nullable
    private final Comparator<I> comparator;

    /**
     * Create a new collector with no comparator / sorting function
     * @param serviceId the service id
     */
    public InfoItemsCollector(final int serviceId) {
        this(serviceId, null);
    }

    /**
     * Create a new collector
     * @param serviceId the service id
     */
    public InfoItemsCollector(final int serviceId, @Nullable final Comparator<I> comparator) {
        this.serviceId = serviceId;
        this.comparator = comparator;
    }

    @Override
    public List<I> getItems() {
        if (comparator != null) {
            itemList.sort(comparator);
        }
        return Collections.unmodifiableList(itemList);
    }

    @Override
    public List<Throwable> getErrors() {
        return Collections.unmodifiableList(errors);
    }

    @Override
    public void reset() {
        itemList.clear();
        errors.clear();
    }

    /**
     * Add an error
     * @param error the error
     */
    protected void addError(final Exception error) {
        errors.add(error);
    }

    /**
     * Add an item
     * @param item the item
     */
    protected void addItem(final I item) {
        itemList.add(item);
    }

    public void addAll(final List<I> items) {
        itemList.addAll(items);
    }

    /**
     * Get the service id
     * @return the service id
     */
    public int getServiceId() {
        return serviceId;
    }

    @Override
    public void commit(final E extractor) {
        try {
            addItem(extract(extractor));
        } catch (final FoundAdException ae) {
            // found an ad. Maybe a debug line could be placed here
        } catch (final ParsingException e) {
            addError(e);
        }
    }

    // Filter methods

    public static class FilterConfig {
        private final ArrayList<String> keywords;
        private final ArrayList<String> channels;
        private final boolean blockShorts;
        private final boolean blockPaidContent;

        public FilterConfig(ArrayList<String> keywords, ArrayList<String> channels, boolean blockShorts, boolean blockPaidContent) {
            this.keywords = keywords != null ? keywords : new ArrayList<>();
            this.channels = channels != null ? channels : new ArrayList<>();
            this.blockShorts = blockShorts;
            this.blockPaidContent = blockPaidContent;
        }
        
        public ArrayList<String> getKeywords() {
            return keywords;
        }
        
        public ArrayList<String> getChannels() {
            return channels;
        }
        
        public boolean isBlockShorts() {
            return blockShorts;
        }
        
        public boolean isBlockPaidContent() {
            return blockPaidContent;
        }
    }
    
    public void applyBlocking(FilterConfig filterConfig) {
        if (filterConfig == null) {
            return;
        }
        
        Iterator<I> iterator = itemList.iterator();
        while (iterator.hasNext()) {
            I item = iterator.next();
            boolean shouldRemove = false;

            if (filterConfig.isBlockShorts() && item instanceof StreamInfoItem && ((StreamInfoItem) item).isShortFormContent()) {
                shouldRemove = true;
            }

            if (!shouldRemove && filterConfig.isBlockPaidContent() && item instanceof StreamInfoItem && ((StreamInfoItem) item).requiresMembership()) {
                shouldRemove = true;
            }

            if (!shouldRemove) {
                // Check keywords
                for (String keyword : filterConfig.getKeywords()) {
                    if (item.getName().toLowerCase().contains(keyword.toLowerCase())) {
                        shouldRemove = true;
                        break;  // No need to check other keywords
                    }
                }
            }

            // Only check channels if we haven't already marked for removal
            if (!shouldRemove) {
                for (String channel : filterConfig.getChannels()) {
                    if (item instanceof StreamInfoItem && ((StreamInfoItem) item).getUploaderName() != null &&
                            ((StreamInfoItem) item).getUploaderName().equals(channel)) {
                        shouldRemove = true;
                        break;  // No need to check other channels
                    }
                }
            }

            // Remove only once if needed
            if (shouldRemove) {
                iterator.remove();
            }
        }
    }
}
