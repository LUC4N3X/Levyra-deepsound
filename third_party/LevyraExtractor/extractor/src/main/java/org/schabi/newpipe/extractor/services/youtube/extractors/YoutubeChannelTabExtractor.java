package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonWriter;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.channel.ChannelTabExtractor;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.linkhandler.ChannelTabs;
import org.schabi.newpipe.extractor.linkhandler.ListLinkHandler;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.services.youtube.YoutubeChannelHelper;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelTabLinkHandlerFactory;
import org.schabi.newpipe.extractor.utils.JsonUtils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.Consumer;

import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.ChannelResponseData;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.DISABLE_PRETTY_PRINT_PARAMETER;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.YOUTUBEI_V1_URL;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.addYoutubeHeaders;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getChannelResponse;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.getValidJsonResponseBody;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.prepareDesktopJsonBuilder;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.resolveChannelId;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

public class YoutubeChannelTabExtractor extends ChannelTabExtractor {
    private JsonObject jsonResponse;
    private JsonObject tabData;

    private String redirectedChannelId;
    @Nullable
    private String visitorData;

    private boolean useVisitorData = false;
    private String channelId;
    @Nullable
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    protected Optional<YoutubeChannelHelper.ChannelHeader> channelHeader;

    public YoutubeChannelTabExtractor(final StreamingService service,
                                      final ListLinkHandler linkHandler) {
        super(service, linkHandler);
        try {
            useVisitorData = getName().equals(ChannelTabs.SHORTS);
        } catch (ParsingException e) {
        }
    }

    private String getChannelTabsParameters() throws ParsingException {
        final String name = getName();
        switch (name) {
            case ChannelTabs.VIDEOS:
                return "EgZ2aWRlb3PyBgQKAjoA";
            case ChannelTabs.SHORTS:
                return "EgZzaG9ydHPyBgUKA5oBAA%3D%3D";
            case ChannelTabs.LIVESTREAMS:
                return "EgdzdHJlYW1z8gYECgJ6AA%3D%3D";
            case ChannelTabs.ALBUMS:
                return "EghyZWxlYXNlc_IGBQoDsgEA";
            case ChannelTabs.PLAYLISTS:
                return "EglwbGF5bGlzdHPyBgQKAkIA";
            case ChannelTabs.PODCASTS:
                return "Eghwb2RjYXN0c_IGBQoDugEA";
            case ChannelTabs.SEARCH:
                return "EgZzZWFyY2jyBgQKAloA";
            default:
                throw new ParsingException("Unsupported channel tab: " + name);
        }
    }

    @Override
    public void onFetchPage(@Nonnull final Downloader downloader) throws IOException,
            ExtractionException {

        final String channelIdFromId = resolveChannelId(super.getId());

        final String params = getChannelTabsParameters();

        final ChannelResponseData data = getChannelResponse(channelIdFromId,
                params, getSearchQuery(), getExtractorLocalization(), getExtractorContentCountry());

        jsonResponse = data.responseJson;
        channelHeader = YoutubeChannelHelper.getChannelHeader(jsonResponse);
        channelId = data.channelId;
        if (useVisitorData) {
            visitorData = jsonResponse.getObject("responseContext").getString("visitorData");
        }
    }

    @Nonnull
    @Override
    public String getUrl() throws ParsingException {
        try {
            final String url = YoutubeChannelTabLinkHandlerFactory.getInstance().getUrl(
                    "channel/" + getId(),
                    Collections.singletonList(new FilterItem(-1, getTab())),
                    getLinkHandler().getSortFilter());
            return YoutubeChannelTabLinkHandlerFactory.appendSearchQueryIfNeeded(
                    url, getSearchQuery());
        } catch (final ParsingException e) {
            return super.getUrl();
        }
    }

    @Nonnull
    @Override
    public String getId() throws ParsingException {
        return YoutubeChannelHelper.getChannelId(channelHeader, jsonResponse, channelId);
    }

    protected String getChannelName() throws ParsingException {
        return YoutubeChannelHelper.getChannelName(
                channelHeader, jsonResponse,
                YoutubeChannelHelper.getChannelAgeGateRenderer(jsonResponse));
    }

    @Nonnull
    @Override
    public InfoItemsPage<InfoItem> getInitialPage() throws IOException, ExtractionException {
        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

        Page nextPage = null;

        if (getTabData() != null) {
            final JsonArray items = getSelectedSortFilterIndex() > 0
                    ? getSortedItemsFrom(tabData)
                    : getInitialItemsFrom(tabData);

            final List<String> channelIds = new ArrayList<>();
            channelIds.add(getChannelName());
            channelIds.add(YoutubeChannelLinkHandlerFactory.getInstance()
                    .getUrl("channel/" + getId()));
            final JsonObject continuation = collectItemsFrom(collector, items, channelIds);

            nextPage = getNextPageFrom(continuation, channelIds);
        }
        if (ServiceList.YouTube.getFilterTypes().contains("channels")) {
            collector.applyBlocking(ServiceList.YouTube.getFilterConfig());
        }
        return new InfoItemsPage<>(collector, nextPage);
    }

    @Nonnull
    private JsonArray getInitialItemsFrom(@Nonnull final JsonObject currentTabData) {
        final JsonObject tabContent = currentTabData.getObject("content");
        JsonArray items = tabContent
                .getObject("sectionListRenderer")
                .getArray("contents").getObject(0).getObject("itemSectionRenderer")
                .getArray("contents").getObject(0).getObject("gridRenderer").getArray("items");

        if (items.isEmpty()) {
            items = tabContent.getObject("richGridRenderer").getArray("contents");

            if (items.isEmpty()) {
                items = tabContent.getObject("sectionListRenderer").getArray("contents");
            }
        }

        return items;
    }

    @Nonnull
    private JsonArray getSortedItemsFrom(@Nonnull final JsonObject currentTabData)
            throws IOException, ExtractionException {
        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                        getExtractorContentCountry())
                        .value("continuation", getSortContinuationToken(currentTabData))
                        .done())
                .getBytes(StandardCharsets.UTF_8);

        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", Collections.singletonList("application/json"));

        final Response response = getDownloader().post(YOUTUBEI_V1_URL + "browse?"
                        + DISABLE_PRETTY_PRINT_PARAMETER, headers, body,
                getExtractorLocalization());

        final JsonObject ajaxJson = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
        return getContinuationItemsFrom(ajaxJson, "reloadContinuationItemsCommand");
    }

    @Nonnull
    private String getSortContinuationToken(@Nonnull final JsonObject currentTabData)
            throws ParsingException {
        final int sortIndex = getSelectedSortFilterIndex();
        final JsonArray chips = currentTabData.getObject("content")
                .getObject("richGridRenderer")
                .getObject("header")
                .getObject("chipBarViewModel")
                .getArray("chips");

        if (chips.size() <= sortIndex) {
            throw new ParsingException("YouTube channel tab sort filter is not available");
        }

        // Some channels expose the sort options as a single "Sort by" dropdown chip
        // (displayType = CHIP_VIEW_MODEL_DISPLAY_TYPE_DROP_DOWN). The actual Latest/Popular/Oldest
        // tokens then live inside that chip's sheet listItems, not in the chipBar directly.
        // Reading chips[sortIndex] in that case picked filter chips (e.g. "Members only", "Public")
        // and returned their continuation tokens, so the sort never applied.
        if (!chips.isEmpty()) {
            final JsonObject firstChip = chips.getObject(0).getObject("chipViewModel");
            if ("CHIP_VIEW_MODEL_DISPLAY_TYPE_DROP_DOWN".equals(firstChip.getString("displayType"))) {
                final JsonArray listItems = firstChip
                        .getObject("tapCommand")
                        .getObject("innertubeCommand")
                        .getObject("showSheetCommand")
                        .getObject("panelLoadingStrategy")
                        .getObject("inlineContent")
                        .getObject("sheetViewModel")
                        .getObject("content")
                        .getObject("listViewModel")
                        .getArray("listItems");
                if (listItems.size() <= sortIndex) {
                    throw new ParsingException("YouTube channel tab sort filter is not available");
                }
                final String token = extractContinuationTokenFromListItem(
                        listItems.getObject(sortIndex).getObject("listItemViewModel"));
                if (isNullOrEmpty(token)) {
                    throw new ParsingException(
                            "Could not get YouTube channel tab sort continuation");
                }
                return token;
            }
        }

        // Flat format: chips[sortIndex] is itself the sort chip.
        if (chips.size() <= sortIndex) {
            throw new ParsingException("YouTube channel tab sort filter is not available");
        }

        final String token = chips.getObject(sortIndex)
                .getObject("chipViewModel")
                .getObject("tapCommand")
                .getObject("innertubeCommand")
                .getObject("continuationCommand")
                .getString("token");

        if (isNullOrEmpty(token)) {
            throw new ParsingException("Could not get YouTube channel tab sort continuation");
        }

        return token;
    }

    /**
     * Walks a sort-dropdown listItem to find its continuationCommand token.
     * The token lives inside {@code commandExecutorCommand.commands[]} (typically alongside an
     * entityUpdateCommand for the chip state). We scan the commands array instead of guessing
     * an index because other commands can sit before the continuationCommand.
     */
    @Nullable
    private static String extractContinuationTokenFromListItem(
            @Nonnull final JsonObject listItemViewModel) {
        final JsonObject innertube = listItemViewModel
                .getObject("rendererContext")
                .getObject("commandContext")
                .getObject("onTap")
                .getObject("innertubeCommand");
        final JsonObject executor = innertube.getObject("commandExecutorCommand");
        if (executor == null) {
            return null;
        }
        final JsonArray commands = executor.getArray("commands");
        for (final Object o : commands) {
            final JsonObject command = (JsonObject) o;
            if (command.has("continuationCommand")) {
                final String token = command
                        .getObject("continuationCommand")
                        .getString("token");
                if (!isNullOrEmpty(token)) {
                    return token;
                }
            }
        }
        return null;
    }

    private int getSelectedSortFilterIndex() throws ParsingException {
        final List<FilterItem> sortFilter = getLinkHandler().getSortFilter();
        if (sortFilter == null || sortFilter.isEmpty()) {
            return 0;
        }

        final String sortFilterName = sortFilter.get(0).getName();
        switch (sortFilterName) {
            case YoutubeChannelTabLinkHandlerFactory.SORT_LATEST:
                return 0;
            case YoutubeChannelTabLinkHandlerFactory.SORT_POPULAR:
                return 1;
            case YoutubeChannelTabLinkHandlerFactory.SORT_OLDEST:
                return 2;
            default:
                throw new ParsingException("Unsupported YouTube channel tab sort filter: "
                        + sortFilterName);
        }
    }

    @Override
    public InfoItemsPage<InfoItem> getPage(final Page page)
            throws IOException, ExtractionException {
        if (page == null || isNullOrEmpty(page.getUrl())) {
            throw new IllegalArgumentException("Page doesn't contain an URL");
        }

        final List<String> channelIds = page.getIds();

        final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());
        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", Collections.singletonList("application/json"));

        final Response response = getDownloader().post(page.getUrl(), headers, page.getBody(),
                getExtractorLocalization());

        final JsonObject ajaxJson = JsonUtils.toJsonObject(getValidJsonResponseBody(response));

        final JsonObject continuation = collectItemsFrom(collector,
                getContinuationItemsFrom(ajaxJson, "appendContinuationItemsAction"), channelIds);
        if (ServiceList.YouTube.getFilterTypes().contains("channels")) {
            collector.applyBlocking(ServiceList.YouTube.getFilterConfig());
        }
        return new InfoItemsPage<>(collector,
                getNextPageFrom(continuation, channelIds));
    }

    @Nonnull
    private JsonArray getContinuationItemsFrom(@Nonnull final JsonObject ajaxJson,
                                               @Nonnull final String commandName)
            throws ParsingException {
        final JsonArray items = new JsonArray();
        addContinuationItemsFrom(ajaxJson, commandName, items);

        if (items.isEmpty() && "appendContinuationItemsAction".equals(commandName)) {
            addContinuationItemsFrom(ajaxJson, "reloadContinuationItemsCommand", items);
        } else if (items.isEmpty() && "reloadContinuationItemsCommand".equals(commandName)) {
            addContinuationItemsFrom(ajaxJson, "appendContinuationItemsAction", items);
        }

        if (items.isEmpty()) {
            throw new ParsingException("Could not get YouTube channel tab continuation items");
        }

        return items;
    }

    private void addContinuationItemsFrom(@Nonnull final Object object,
                                          @Nonnull final String commandName,
                                          @Nonnull final JsonArray targetItems) {
        if (object instanceof JsonObject) {
            final JsonObject jsonObject = (JsonObject) object;
            final JsonArray continuationItems = jsonObject.getObject(commandName)
                    .getArray("continuationItems");
            for (final Object continuationItem : continuationItems) {
                targetItems.add(continuationItem);
            }

            for (final Object value : jsonObject.values()) {
                addContinuationItemsFrom(value, commandName, targetItems);
            }
        } else if (object instanceof JsonArray) {
            for (final Object value : (JsonArray) object) {
                addContinuationItemsFrom(value, commandName, targetItems);
            }
        }
    }

    @Nullable
    private JsonObject getTabData() throws ParsingException {
        if (this.tabData != null) {
            return this.tabData;
        }

        final String urlSuffix = YoutubeChannelTabLinkHandlerFactory.getUrlSuffix(getTab());

        final JsonArray tabs = jsonResponse.getObject("contents")
                .getObject("twoColumnBrowseResultsRenderer")
                .getArray("tabs");

        JsonObject foundTab = null;
        for (final Object tab : tabs) {
            final JsonObject tabRenderer = getTabRenderer((JsonObject) tab);
            if (tabRenderer == null) {
                continue;
            }

            final String tabUrl = tabRenderer.getObject("endpoint")
                    .getObject("commandMetadata").getObject("webCommandMetadata")
                    .getString("url");
            if (tabUrl != null && normalizeTabUrl(tabUrl).endsWith(urlSuffix)) {
                foundTab = tabRenderer;
                break;
            }
        }

        // No tab
        if (foundTab == null) {
            return null;
        }

        // No content
        final JsonArray tabContents = foundTab.getObject("content").getObject("sectionListRenderer")
                .getArray("contents").getObject(0)
                .getObject("itemSectionRenderer").getArray("contents");
        if (tabContents.size() == 1 && tabContents.getObject(0).has("messageRenderer")) {
            return null;
        }

        this.tabData = foundTab;
        return foundTab;
    }

    private void commitPlaylistLockup(@Nonnull final MultiInfoItemsCollector collector,
                                      @Nonnull final JsonObject playlistLockupViewModel,
//                                      @Nonnull final VerifiedStatus channelVerifiedStatus,
                                      @Nullable final String channelName,
                                      @Nullable final String channelUrl) {
        collector.commit(
                new YoutubeMixOrPlaylistLockupInfoItemExtractor(playlistLockupViewModel) {
                    @Override
                    public String getUploaderName() throws ParsingException {
                        return isNullOrEmpty(channelName) ? super.getUploaderName() : channelName;
                    }

//                    @Override
//                    public String getUploaderUrl() throws ParsingException {
//                        return isNullOrEmpty(channelUrl) ? super.getUploaderName() : channelUrl;
//                    }
//
//                    @Override
//                    public boolean isUploaderVerified() throws ParsingException {
//                        switch (channelVerifiedStatus) {
//                            case VERIFIED:
//                                return true;
//                            case UNVERIFIED:
//                                return false;
//                            default:
//                                return super.isUploaderVerified();
//                        }
//                    }
                });
    }



    @Nullable
    private JsonObject collectItemsFrom(@Nonnull final MultiInfoItemsCollector collector,
                                        @Nonnull final JsonArray items,
                                        @Nonnull final List<String> channelIds) throws ParsingException {
        JsonObject continuation = null;

        for (final Object object : items) {
            final JsonObject item = (JsonObject) object;
            final JsonObject optContinuation = collectItem(
                    collector, item, channelIds);
            if (optContinuation != null) {
                continuation = optContinuation;
            }
        }
        return continuation;
    }

    @Nullable
    private JsonObject collectItem(@Nonnull final MultiInfoItemsCollector collector,
                                   @Nonnull final JsonObject item,
                                   @Nonnull final List<String> channelIds) throws ParsingException {
        final Consumer<JsonObject> commitVideo = videoRenderer -> collector.commit(
                new YoutubeStreamInfoItemExtractor(videoRenderer, getTimeAgoParser()) {
                    @Override
                    public String getUploaderName() {
                        return channelIds.get(0);
                    }

                    @Override
                    public String getUploaderUrl() {
                        return channelIds.get(1);
                    }
                });

        if (item.has("gridVideoRenderer")) {
            commitVideo.accept(item.getObject("gridVideoRenderer"));
        } else if (item.has("videoRenderer")) {
            commitVideo.accept(item.getObject("videoRenderer"));
        } else if (item.has("richItemRenderer")) {
            final JsonObject richItem = item.getObject("richItemRenderer").getObject("content");

            if (richItem.has("videoRenderer")) {
                commitVideo.accept(richItem.getObject("videoRenderer"));

            } else if (richItem.has("reelItemRenderer")) {
                commitVideo.accept(richItem.getObject("reelItemRenderer"));
            } else if (richItem.has("shortsLockupViewModel")) {
                collector.commit(new YoutubeShortsInfoItemExtractor(
                        richItem.getObject("shortsLockupViewModel")
                ) {
                    @Override
                    public String getUploaderName() {
                        return channelIds.get(0);
                    }
                });
            } else if (richItem.has("lockupViewModel")) {
                commitLockupItemIfSupported(collector,
                        richItem.getObject("lockupViewModel"), channelIds);
            } else {
                return collectItem(collector, richItem, channelIds);
            }
        } else if (item.has("gridPlaylistRenderer")) {
            collector.commit(new YoutubePlaylistInfoItemExtractor(
                    item.getObject("gridPlaylistRenderer")) {
                @Override
                public String getUploaderName() {
                    return channelIds.get(0);
                }
            });
        } else if (item.has("playlistRenderer")) {
            collector.commit(new YoutubeMixOrPlaylistInfoItemExtractor(
                    item.getObject("playlistRenderer")));
        } else if (item.has("radioRenderer")) {
            collector.commit(new YoutubeMixOrPlaylistInfoItemExtractor(
                    item.getObject("radioRenderer")));
        } else if (item.has("gridChannelRenderer")) {
            collector.commit(new YoutubeChannelInfoItemExtractor(
                    item.getObject("gridChannelRenderer")));
        } else if (item.has("channelRenderer")) {
            collector.commit(new YoutubeChannelInfoItemExtractor(
                    item.getObject("channelRenderer")));
        } else if (item.has("shelfRenderer")) {
            return collectItem(collector, item.getObject("shelfRenderer")
                    .getObject("content"), channelIds);
        } else if (item.has("itemSectionRenderer")) {
            return collectItemsFrom(collector, item.getObject("itemSectionRenderer")
                    .getArray("contents"), channelIds);
        } else if (item.has("horizontalListRenderer")) {
            return collectItemsFrom(collector, item.getObject("horizontalListRenderer")
                    .getArray("items"), channelIds);
        } else if (item.has("expandedShelfContentsRenderer")) {
            return collectItemsFrom(collector, item.getObject("expandedShelfContentsRenderer")
                    .getArray("items"), channelIds);
        } else if (item.has("continuationItemRenderer")) {
            return item.getObject("continuationItemRenderer");
        } else if (item.has("lockupViewModel")) {
            commitLockupItemIfSupported(collector,
                    item.getObject("lockupViewModel"), channelIds);
        }
        return null;
    }

    @Nullable
    private String getSearchQuery() throws ParsingException {
        if (!ChannelTabs.SEARCH.equals(getTab())) {
            return null;
        }

        return YoutubeChannelTabLinkHandlerFactory.getSearchQueryFromUrl(getOriginalUrl());
    }

    @Nullable
    private static JsonObject getTabRenderer(@Nonnull final JsonObject tab) {
        if (tab.has("tabRenderer")) {
            return tab.getObject("tabRenderer");
        }
        if (tab.has("expandableTabRenderer")) {
            return tab.getObject("expandableTabRenderer");
        }
        return null;
    }

    private void commitLockupItemIfSupported(@Nonnull final MultiInfoItemsCollector collector,
                                             @Nonnull final JsonObject lockupViewModel,
                                             @Nonnull final List<String> channelIds) {
        final String contentType = lockupViewModel.getString("contentType");
        if ("LOCKUP_CONTENT_TYPE_PLAYLIST".equals(contentType)
                || "LOCKUP_CONTENT_TYPE_PODCAST".equals(contentType)) {
            String channelName;
            try {
                channelName = getChannelName();
            } catch (final Exception e) {
                channelName = channelIds.get(0);
            }
            commitPlaylistLockup(collector, lockupViewModel, channelName, null);
            return;
        }

        if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(contentType)
                || "LOCKUP_CONTENT_TYPE_EPISODE".equals(contentType)) {
            collector.commit(new YoutubeLockupStreamInfoItemExtractor(lockupViewModel,
                    getTimeAgoParser()) {
                @Override
                public String getUploaderName() {
                    return channelIds.get(0);
                }

                @Override
                public String getUploaderUrl() {
                    return channelIds.get(1);
                }
            });
        }
    }

    @Nonnull
    private static String normalizeTabUrl(@Nonnull final String tabUrl) {
        String normalized = tabUrl;
        final int queryIndex = normalized.indexOf('?');
        if (queryIndex >= 0) {
            normalized = normalized.substring(0, queryIndex);
        }

        final int fragmentIndex = normalized.indexOf('#');
        if (fragmentIndex >= 0) {
            normalized = normalized.substring(0, fragmentIndex);
        }

        while (normalized.endsWith("/")) {
            normalized = normalized.substring(0, normalized.length() - 1);
        }

        return normalized;
    }

    @Nullable
    private Page getNextPageFrom(final JsonObject continuations,
                                 final List<String> channelIds) throws IOException,
            ExtractionException {
        if (isNullOrEmpty(continuations)) {
            return null;
        }

        final JsonObject continuationEndpoint = continuations.getObject("continuationEndpoint");
        final String continuation = continuationEndpoint.getObject("continuationCommand")
                .getString("token");

        final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(getExtractorLocalization(),
                        getExtractorContentCountry())
                        .value("continuation", continuation)
                        .done())
                .getBytes(StandardCharsets.UTF_8);

        return new Page(YOUTUBEI_V1_URL + "browse?"
                + DISABLE_PRETTY_PRINT_PARAMETER, null, channelIds, null, body);
    }
}
