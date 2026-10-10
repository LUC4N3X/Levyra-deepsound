package org.schabi.newpipe.extractor.services.niconico.extractors;

import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.Page;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.comments.CommentsExtractor;
import org.schabi.newpipe.extractor.comments.CommentsInfoItem;
import org.schabi.newpipe.extractor.comments.CommentsInfoItemsCollector;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;

import java.io.IOException;

import javax.annotation.Nonnull;

public class NiconicoCommentsExtractor extends CommentsExtractor {

    private JsonObject watch;
    private final NiconicoWatchDataCache watchDataCache;
    private final NiconicoCommentsCache commentsCache;

    public NiconicoCommentsExtractor(
            final StreamingService service,
            final ListLinkHandler uiHandler,
            final NiconicoWatchDataCache watchDataCache,
            final NiconicoCommentsCache commentsCache) {
        super(service, uiHandler);
        this.watchDataCache = watchDataCache;
        this.commentsCache = commentsCache;
    }

    @Override
    public void onFetchPage(final @Nonnull Downloader downloader)
            throws IOException, ExtractionException {
        watch = watchDataCache.refreshAndGetWatchData(downloader, getId());
    }

    @Nonnull
    @Override
    public InfoItemsPage<CommentsInfoItem> getInitialPage()
            throws IOException, ExtractionException {
        final CommentsInfoItemsCollector collector = new CommentsInfoItemsCollector(getServiceId());
        for (final JsonObject comment : commentsCache.getComments(watch,
                getDownloader(), getId())) {
            collector.commit(new NiconicoCommentsInfoItemExtractor(comment, getUrl()));
        }
        this.getId();
        return new InfoItemsPage<>(collector, null);
    }

    @Override
    public InfoItemsPage<CommentsInfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        return null;
    }
}
