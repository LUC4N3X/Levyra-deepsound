package org.schabi.newpipe.extractor.kiosk;

/*
 * Created by Christian Schabesberger on 12.08.17.
 *
 * Copyright (C) Christian Schabesberger 2017 <chris.schabesberger@mailbox.org>
 * KioskInfo.java is part of NewPipe.
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

import org.schabi.newpipe.extractor.InfoItem;
import org.schabi.newpipe.extractor.ListExtractor;
import org.schabi.newpipe.extractor.ListInfo;
import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.stream.StreamInfoItem;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

public final class KioskInfo extends ListInfo<StreamInfoItem> {
    private KioskInfo(final int serviceId, final ListLinkHandler linkHandler, final String name) {
        super(serviceId, linkHandler, name);
    }

    public static ListExtractor.InfoItemsPage<StreamInfoItem> getMoreItems(
            final StreamingService service, final String url, final Page page)
            throws IOException, ExtractionException {
        return toStreamPage(service.getKioskList().getExtractorByUrl(url, page).getPage(page));
    }

    public static KioskInfo getInfo(final String url) throws IOException, ExtractionException {
        return getInfo(NewPipe.getServiceByUrl(url), url);
    }

    public static KioskInfo getInfo(final StreamingService service, final String url)
            throws IOException, ExtractionException {
        final KioskExtractor<? extends InfoItem> extractor =
                service.getKioskList().getExtractorByUrl(url, null);
        extractor.fetchPage();
        return getInfo(extractor);
    }

    /**
     * Get KioskInfo from KioskExtractor
     *
     * @param extractor an extractor where fetchPage() was already got called on.
     */
    public static KioskInfo getInfo(final KioskExtractor<? extends InfoItem> extractor)
            throws ExtractionException {
        final KioskInfo info = new KioskInfo(extractor.getServiceId(),
                extractor.getLinkHandler(), extractor.getName());
        try {
            final ListExtractor.InfoItemsPage<StreamInfoItem> itemsPage =
                    toStreamPage(extractor.getInitialPage());
            info.addAllErrors(itemsPage.getErrors());
            info.setRelatedItems(itemsPage.getItems());
            info.setNextPage(itemsPage.getNextPage());
        } catch (final Exception e) {
            info.addError(e);
        }
        return info;
    }

    private static ListExtractor.InfoItemsPage<StreamInfoItem> toStreamPage(
            final ListExtractor.InfoItemsPage<? extends InfoItem> page) {
        final List<StreamInfoItem> items = page.getItems().stream()
                .filter(StreamInfoItem.class::isInstance)
                .map(StreamInfoItem.class::cast)
                .collect(Collectors.toList());
        return new ListExtractor.InfoItemsPage<>(items, page.getNextPage(), page.getErrors());
    }
}
