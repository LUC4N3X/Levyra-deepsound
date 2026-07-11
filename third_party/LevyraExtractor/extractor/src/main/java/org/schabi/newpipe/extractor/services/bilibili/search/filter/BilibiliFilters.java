package org.schabi.newpipe.extractor.services.bilibili.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;

public class BilibiliFilters extends SearchFiltersBase {
    private static final String VIDEOS = "videos";
    private static final String LIVES = "lives";
    private static final String USERS = "channels";
    private static final String ANIME = "animes";
    private static final String MOVIES = "movies_and_tv";

    public BilibiliFilters() {
        init();
        build();
    }

    public String evaluateSelectedContentFilters() {
        StringBuilder sortQuery = new StringBuilder();

        if (selectedContentFilter != null && !selectedContentFilter.isEmpty()) {
            final BilibiliFilters.BilibiliContentFilterItem contentItem =
                    // we assume that there is just one content filter
                    (BilibiliFilters.BilibiliContentFilterItem) selectedContentFilter.get(0);
            if (contentItem != null) {
                if (!contentItem.query.isEmpty()) {
                    sortQuery = new StringBuilder("&" + contentItem.query);
                }
            }
        }
        if(selectedSortFilter != null){
            for (FilterItem sortItem : selectedSortFilter) {
                if (!((BilibiliSortFilterItem) sortItem).query.isEmpty()) {
                    sortQuery.append("&").append(((BilibiliSortFilterItem) sortItem).query);
                }
            }
        }
        return sortQuery.toString();
    }

    @Override
    protected void init() {
        /* content filters */
        final int contentFilterVideos = builder.addFilterItem(
                new BilibiliFilters.BilibiliContentFilterItem(VIDEOS, "search_type=video"));
        final int contentFilterLives = builder.addFilterItem(
                new BilibiliFilters.BilibiliContentFilterItem(LIVES, "search_type=live_room"));
        final int contentFilterUsers = builder.addFilterItem(
                new BilibiliFilters.BilibiliContentFilterItem(USERS, "search_type=bili_user"));
        final int contentFilterAnime = builder.addFilterItem(
                new BilibiliFilters.BilibiliContentFilterItem(ANIME, "search_type=media_bangumi"));
        final int contentFilterMovies = builder.addFilterItem(
                new BilibiliFilters.BilibiliContentFilterItem(MOVIES, "search_type=media_ft"));

        this.defaultContentFilterId = contentFilterVideos;

        /* content filters with sort filters */
        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentFilterVideos),
                builder.getFilterForId(contentFilterLives),
                builder.getFilterForId(contentFilterUsers),
                builder.getFilterForId(contentFilterAnime),
                builder.getFilterForId(contentFilterMovies),
        }));

        final int filterOverallScore = builder.addSortItem(
                new BilibiliSortFilterItem("sort_overall", "order=totalrank")
        );
        final int filterViewCount = builder.addSortItem(
                new BilibiliSortFilterItem("sort_view", "order=click")
        );
        final int filterLatest = builder.addSortItem(
                new BilibiliSortFilterItem("sort_publish_time", "order=pubdate")
        );
        final int filterBulletCommentCount = builder.addSortItem(
                new BilibiliSortFilterItem("sort_bullet_comments", "order=dm")
        );
        final int filterCommentCount = builder.addSortItem(
                new BilibiliSortFilterItem("sort_comments", "order=scores")
        );
        final int filterBookmarkCount = builder.addSortItem(
                new BilibiliSortFilterItem("sort_bookmark", "order=stow")
        );
        final int filterDuartionAll = builder.addSortItem(
                new BilibiliSortFilterItem("all", "duration=0")
        );
        final int filterDuartionShort = builder.addSortItem(
                new BilibiliSortFilterItem("short_video", "duration=1")
        );
        final int filterDuartionMedium = builder.addSortItem(
                new BilibiliSortFilterItem("medium_length", "duration=2")
        );
        final int filterDuartionLong = builder.addSortItem(
                new BilibiliSortFilterItem("long_video", "duration=3")
        );
        final int filterDuartionExtraLong = builder.addSortItem(
                new BilibiliSortFilterItem("extra_long", "duration=4")
        );
        final Filter videoSortFilters = new Filter.Builder(new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterOverallScore),
                        builder.getFilterForId(filterLatest),
                        builder.getFilterForId(filterViewCount),
                        builder.getFilterForId(filterBulletCommentCount),
                        builder.getFilterForId(filterCommentCount),
                        builder.getFilterForId(filterBookmarkCount),
                }),
                builder.createSortGroup("duration", true, new FilterItem[]{
                        builder.getFilterForId(filterDuartionAll),
                        builder.getFilterForId(filterDuartionShort),
                        builder.getFilterForId(filterDuartionMedium),
                        builder.getFilterForId(filterDuartionLong),
                        builder.getFilterForId(filterDuartionExtraLong),
                })
        }).build();
        addContentFilterSortVariant(-1, videoSortFilters);
        addContentFilterSortVariant(contentFilterVideos, videoSortFilters);
    }

    public static class BilibiliContentFilterItem extends FilterItem {
        private final String query;

        public BilibiliContentFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }
    }

    private static class BilibiliSortFilterItem extends FilterItem {
        protected final String query;

        BilibiliSortFilterItem(final String name, final String query) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
            this.query = query;
        }
    }
}
