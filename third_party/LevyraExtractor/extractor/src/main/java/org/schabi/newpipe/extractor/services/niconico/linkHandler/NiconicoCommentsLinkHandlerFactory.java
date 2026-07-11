package org.schabi.newpipe.extractor.services.niconico.linkHandler;

import java.util.List;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandlerFactory;
import org.schabi.newpipe.extractor.search.filter.FilterItem;

public class NiconicoCommentsLinkHandlerFactory extends ListLinkHandlerFactory {
    public NiconicoCommentsLinkHandlerFactory(
            final NiconicoStreamLinkHandlerFactory niconicoStreamLinkHandlerFactory) {
        super();
        this.niconicoStreamLinkHandlerFactory = niconicoStreamLinkHandlerFactory;
    }

    protected NiconicoStreamLinkHandlerFactory niconicoStreamLinkHandlerFactory;

    @Override
    public String getId(final String url) throws ParsingException {
        return niconicoStreamLinkHandlerFactory.getId(url);
    }

    @Override
    public boolean onAcceptUrl(final String url) throws ParsingException {
        return niconicoStreamLinkHandlerFactory.onAcceptUrl(url);
    }

    @Override
    public String getUrl(final String id,
                         final List<FilterItem> contentFilter,
                         final List<FilterItem> sortFilter) throws ParsingException {
        return niconicoStreamLinkHandlerFactory.getUrl(id);
    }
}
