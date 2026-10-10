package org.schabi.newpipe.extractor.services.peertube.linkHandler;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

import java.util.List;

public final class PeertubeChannelTabLinkHandlerFactory extends ListLinkHandlerFactory {
    private static final PeertubeChannelTabLinkHandlerFactory INSTANCE
            = new PeertubeChannelTabLinkHandlerFactory();

    private PeertubeChannelTabLinkHandlerFactory() {
    }

    public static PeertubeChannelTabLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    private static String getUrlSuffix(final String tab) throws ParsingException {
        switch (tab) {
            case ChannelTabs.VIDEOS:
                return "/videos";
            case ChannelTabs.PLAYLISTS:
                return "/video-playlists";
            case ChannelTabs.CHANNELS:
                return "/video-channels";
        }
        throw new ParsingException("tab " + tab + " not supported");
    }

    @Override
    public String getId(final String url) throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getId(url);
    }

    @Override
    public String getUrl(final String id, final List<FilterItem> contentFilter, final List<FilterItem> sortFilter)
            throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getUrl(id)
                + getUrlSuffix(contentFilter.get(0).getName());
    }

    @Override
    public String getUrl(final String id,  final List<FilterItem> contentFilter, final List<FilterItem> sortFilter,
                         final String baseUrl) throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().getUrl(id, null, null, baseUrl)
                + getUrlSuffix(contentFilter.get(0).getName());
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return PeertubeChannelLinkHandlerFactory.getInstance().onAcceptUrl(url);
    }

}
