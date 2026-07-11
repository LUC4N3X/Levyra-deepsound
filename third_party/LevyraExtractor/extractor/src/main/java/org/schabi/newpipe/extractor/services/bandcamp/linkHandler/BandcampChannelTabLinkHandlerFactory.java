package org.schabi.newpipe.extractor.services.bandcamp.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

public final class BandcampChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final BandcampChannelTabLinkHandlerFactory INSTANCE
            = new BandcampChannelTabLinkHandlerFactory();

    // This is not an actual page on the Bandcamp website, but it auto-redirects
    // to the main page and we need a unique URL for the album tab
    public static final String URL_SUFFIX = "/album";

    private BandcampChannelTabLinkHandlerFactory() {
    }

    public static BandcampChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().getId(url);
    }

    public static String getUrlSuffix(final String tab) throws ParsingException {
        switch (tab) {
            case ChannelTabs.TRACKS:
                return "/track";
            case ChannelTabs.ALBUMS:
                return "/album";
        }
        throw new ParsingException("tab " + tab + " not supported");
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter, final List<FilterItem> sortFilter)
            throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0).getName());
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return BandcampChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }


}
