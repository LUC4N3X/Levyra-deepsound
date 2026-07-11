package org.schabi.newpipe.extractor.services.niconico.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;
import org.schabi.newpipe.extractor.services.bilibili.search.filter.BilibiliFilters;
import org.schabi.newpipe.extractor.services.niconico.NiconicoService;

import java.util.Optional;

public final class NiconicoFilters extends SearchFiltersBase {

    private static final String ALL = "all";
    private static final String TAGS_ONLY = "tags_only";
    private static final String LIVES = "lives";
    private static final String PLAYLISTS = "playlists";

    public NiconicoFilters() {
        init();
        build();
    }

    @Override
    public String evaluateSelectedFilters(final String searchString) {
        if (selectedSortFilter != null) {
            StringBuilder sortQuery = new StringBuilder();

            final Optional<FilterItem> ascendingFilter = selectedSortFilter.stream()
                    .filter(filterItem -> filterItem instanceof NiconicoSortOrderFilterItem)
                    .findFirst();
            boolean isAscending = ascendingFilter.isPresent();

            if (selectedContentFilter != null && !selectedContentFilter.isEmpty()) {
                final NiconicoFilters.NiconicoContentFilterItem contentItem =
                        // we assume that there is just one content filter
                        (NiconicoFilters.NiconicoContentFilterItem) selectedContentFilter.get(0);
                if (contentItem != null) {
                    if (!contentItem.query.isEmpty()) {
                        sortQuery = new StringBuilder("&" + contentItem.query);
                    }
                }
            }
            for (FilterItem sortItem : selectedSortFilter) {
                if (sortItem instanceof NiconicoSortFilterItem && !((NiconicoFilters.NiconicoSortFilterItem) sortItem).query.isEmpty()) {
                    sortQuery.append("&").append(((NiconicoSortFilterItem) sortItem).getQueryData(isAscending));
                }
            }
            return sortQuery.toString();
        }
        return "";
    }

    @Override
    protected void init() {
        /* content filters */
        final int contentFilterAll = builder.addFilterItem(
                new NiconicoFilters.NiconicoContentFilterItem(ALL, ""));
//        final int contentFilterTagsOnly = builder.addFilterItem(
//                new NiconicoFilters.NiconicoContentFilterItem(TAGS_ONLY, "targets=tagsExact"));
        final int contentFilterLiveRooms = builder.addFilterItem(
                new NiconicoFilters.NiconicoContentFilterItem(LIVES, ""));
        final int contentFilterPlaylists = builder.addFilterItem(
                new NiconicoFilters.NiconicoContentFilterItem(PLAYLISTS, ""));
        this.defaultContentFilterId = contentFilterAll;

        /* content filters with sort filters */
        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentFilterAll),
//                builder.getFilterForId(contentFilterTagsOnly),
//                builder.getFilterForId(contentFilterLiveRooms),
                builder.getFilterForId(contentFilterPlaylists),
        }));

        /* 'Sort by' filter items */
        final int filterHottest = builder.addSortItem(
                new NiconicoSortFilterItem("sort_popular", "sort=h"));
        final int filterViewCount = builder.addSortItem(
                new NiconicoSortFilterItem("sort_view", "sort=v"));
        final int filterBookmarkCount = builder.addSortItem(
                new NiconicoSortFilterItem("sort_bookmark", "sort=m"));
        final int filterLikeCount = builder.addSortItem(
                new NiconicoSortFilterItem("sort_likes", "sort=likeCount"));
//        final int filterCommentCount = builder.addSortItem(
//                new NiconicoSortFilterItem("sort_comments", "sort=commentCount"));
//
//        final int filterLength = builder.addSortItem(
//                new NiconicoSortFilterItem("sort_length", "_sort=lengthSeconds"));

        final int filterPublishAt = builder.addSortItem(
                new NiconicoSortFilterItem("sort_publish_time", "sort=f"));
        final int filterLastCommentedAt = builder.addSortItem(
                new NiconicoSortFilterItem("sort_last_comment_time", "sort=n"));

        final int filterPlaylistMostPopular = builder.addSortItem(
                new NiconicoSortFilterItem("sort_popular", "sortKey=_hotTotalScore"));
        final int filterPlaylistMostVideos = builder.addSortItem(
                new NiconicoSortFilterItem("sort_video_count", "sortKey=videoCount"));
        final int filterPlaylistRecentCreated = builder.addSortItem(
                new NiconicoSortFilterItem("sort_publish_time", "sortKey=startTime"));

        final int filterSortOrderAscending = builder.addSortItem(
                new NiconicoSortOrderFilterItem("sort_ascending"));

        final int filterDurationAll = builder.addSortItem(
                new NiconicoSortFilterItem("all", "")
        );
        final int filterDurationShort = builder.addSortItem(
                new NiconicoSortFilterItem("short_video", "l_range=1")
        );
        final int filterDurationLong = builder.addSortItem(
                new NiconicoSortFilterItem("long_video", "l_range=2")
        );

        final Filter allSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterHottest),
                        builder.getFilterForId(filterViewCount),
//                        builder.getFilterForId(filterCommentCount),
                        builder.getFilterForId(filterBookmarkCount),
                        builder.getFilterForId(filterLikeCount),
//                        builder.getFilterForId(filterLength),
                        builder.getFilterForId(filterPublishAt),
                        builder.getFilterForId(filterLastCommentedAt),
                        builder.getFilterForId(filterPlaylistMostPopular),
                        builder.getFilterForId(filterPlaylistMostVideos),
                        builder.getFilterForId(filterPlaylistRecentCreated)
                }),
                builder.createSortGroup("duration", true, new FilterItem[]{
                        builder.getFilterForId(filterDurationAll),
                        builder.getFilterForId(filterDurationShort),
                        builder.getFilterForId(filterDurationLong),
                }),
                builder.createSortGroup("sortorder", false, new FilterItem[]{
                        builder.getFilterForId(filterSortOrderAscending)
                }),
        }).build();

        final Filter videoSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterHottest),
                        builder.getFilterForId(filterViewCount),
//                        builder.getFilterForId(filterCommentCount),
                        builder.getFilterForId(filterBookmarkCount),
                        builder.getFilterForId(filterLikeCount),
//                        builder.getFilterForId(filterLength),
                        builder.getFilterForId(filterPublishAt),
                        builder.getFilterForId(filterLastCommentedAt),
                }),
                builder.createSortGroup("duration", true, new FilterItem[]{
                        builder.getFilterForId(filterDurationAll),
                        builder.getFilterForId(filterDurationShort),
                        builder.getFilterForId(filterDurationLong),
                }),
                builder.createSortGroup("sortorder", false, new FilterItem[]{
                        builder.getFilterForId(filterSortOrderAscending)
                }),
        }).build();

        final Filter tagSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterViewCount),
//                        builder.getFilterForId(filterCommentCount),
                        builder.getFilterForId(filterBookmarkCount),
                        builder.getFilterForId(filterLikeCount),
//                        builder.getFilterForId(filterLength),
                        builder.getFilterForId(filterPublishAt),
                        builder.getFilterForId(filterLastCommentedAt),
                }),
                builder.createSortGroup("sortorder", false, new FilterItem[]{
                        builder.getFilterForId(filterSortOrderAscending)
                }),
        }).build();

        final Filter playlistSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterPlaylistMostPopular),
                        builder.getFilterForId(filterPlaylistMostVideos),
                        builder.getFilterForId(filterPlaylistRecentCreated),
                })
        }).build();
        addContentFilterSortVariant(-1, allSortFilters);
        addContentFilterSortVariant(contentFilterAll, videoSortFilters);
//        addContentFilterSortVariant(contentFilterTagsOnly, tagSortFilters);
        addContentFilterSortVariant(contentFilterPlaylists, playlistSortFilters);
    }

    private static class NiconicoSortFilterItem extends FilterItem {
        protected final String query;

        NiconicoSortFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }

        public String getQueryData(boolean isAscending) {
            if(query.contains("sortKey")){
                // Playlist sort should not be replaced
                return query;
            }
            if (isAscending) {
                return query + "&order=a";
            } else {
                return query + "&order=d";
            }
        }
    }

    public static class NiconicoContentFilterItem extends FilterItem {
        private final String query;

        public NiconicoContentFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }
    }

    private static class NiconicoSortOrderFilterItem extends FilterItem {
        NiconicoSortOrderFilterItem(final String name) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
        }
    }

}
