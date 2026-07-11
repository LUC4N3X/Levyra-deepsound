package org.schabi.newpipe.extractor.services.peertube.linkHandler;

import org.schabi.newpipe.extractor.search.filter.FilterItem;

import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.exceptions.FoundAdException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.Utils;

import java.net.URL;
import java.util.List;

public final class PeertubeCommentsLinkHandlerFactory extends ListLinkHandlerFactory {

    private static final PeertubeCommentsLinkHandlerFactory INSTANCE
            = new PeertubeCommentsLinkHandlerFactory();
    private static final String COMMENTS_ENDPOINT = "/api/v1/videos/%s/comment-threads";

    private PeertubeCommentsLinkHandlerFactory() {
    }

    public static PeertubeCommentsLinkHandlerFactory getInstance() {
        return INSTANCE;
    }

    @Override
    public String getId(final String url) throws ParsingException, IllegalArgumentException {
        return PeertubeStreamLinkHandlerFactory.getInstance().getId(url); // the same id is needed
    }

    @Override
    public boolean onAcceptUrl(final String url) throws FoundAdException {
        try {
            final URL urlObj = Utils.stringToURL(url);
            if (!Utils.isHTTP(urlObj)) {
                return false;
            }
            final String path = urlObj.getPath();
            return path.startsWith("/videos/") || path.startsWith("/w/");
        } catch (final IllegalArgumentException | java.net.MalformedURLException e) {
            return false;
        }
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return getUrl(id, contentFilter, sortFilter, ServiceList.PeerTube.getBaseUrl());
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter,
                         final String baseUrl) throws ParsingException {
        return baseUrl + String.format(COMMENTS_ENDPOINT, id);
    }

}
