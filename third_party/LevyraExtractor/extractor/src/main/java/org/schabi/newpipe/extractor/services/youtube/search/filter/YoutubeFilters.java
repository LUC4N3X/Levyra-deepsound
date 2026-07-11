package org.schabi.newpipe.extractor.services.youtube.search.filter;

import org.schabi.newpipe.extractor.search.filter.Filter;
import org.schabi.newpipe.extractor.search.filter.FilterGroup;
import org.schabi.newpipe.extractor.search.filter.FilterItem;
import org.schabi.newpipe.extractor.search.filter.SearchFiltersBase;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.DateFilter;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.Features;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.LenFilter;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.SortOrder;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.TypeFilter;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.util.HashMap;
import java.util.Map;


public final class YoutubeFilters extends SearchFiltersBase {
    public static final String UTF_8 = "UTF-8";

    /**
     * 'ALL' this is the default search content filter.
     * It has all sort filters that are available.
     */
    public static final String ALL = "all";
    public static final String VIDEOS = "videos";
    public static final String CHANNELS = "channels";
    public static final String PLAYLISTS = "playlists";
    public static final String MOVIES = "movies";
    public static final String LIVES = "Lives";

    public static final String MUSIC_SONGS = "music_songs";
    public static final String MUSIC_VIDEOS = "music_videos";
    public static final String MUSIC_ALBUMS = "music_albums";
    public static final String MUSIC_PLAYLISTS = "music_playlists";
    public static final String MUSIC_ARTISTS = "music_artists";

    private static final String SEARCH_URL = "https://www.youtube.com/results?search_query=";
    private static final String MUSIC_SEARCH_URL = "https://music.youtube.com/search?q=";

    private final Map<FilterItem, TypeFilter> contentFilterTypeMap = new HashMap<>();

    public YoutubeFilters() {
        init();
        build();
    }

    /**
     * generate the search parameter protobuf 'sp' string that is appended to the search URL.
     *
     * @param contentFilter the type of contentFilter that should be added to the 'sp'
     * @return the protobuf base64 encoded 'sp' parameter
     */
    public String buildSortFilterYoutube(final TypeFilter contentFilter) {
        final YoutubeSearchSortFilter.Builder builder = new YoutubeSearchSortFilter.Builder();
        builder.setTypeFilter(contentFilter);

        for (final FilterItem sortItem : selectedSortFilter) {
            if (sortItem instanceof YoutubeSortOrderSortFilterItem) {
                final SortOrder sortOrder = ((YoutubeSortOrderSortFilterItem) sortItem).get();
                if (null != sortOrder) {
                    builder.setSortOrder(sortOrder);
                }
            } else if (sortItem instanceof YoutubeDateSortFilterItem) {
                final DateFilter dateFilter = ((YoutubeDateSortFilterItem) sortItem).get();
                if (null != dateFilter) {
                    builder.setDateFilter(dateFilter);
                }
            } else if (sortItem instanceof YoutubeLenSortFilterItem) {
                final LenFilter lenFilter = ((YoutubeLenSortFilterItem) sortItem).get();
                if (null != lenFilter) {
                    builder.setLenFilter(lenFilter);
                }
            } else if (sortItem instanceof YoutubeFeatureSortFilterItem) {
                final Features feature = ((YoutubeFeatureSortFilterItem) sortItem).get();
                if (null != feature) {
                    builder.addFeature(feature);
                }
            }
        }

        try {
            return builder.build().getSp();
        } catch (final IOException e) {
            throw new RuntimeException(e);
        }
    }

    @Override
    public String evaluateSelectedFilters(final String searchString) {
        if (selectedContentFilter != null && !selectedContentFilter.isEmpty()) {
            // we assume that there is just one content filter available
            final FilterItem filterItem = selectedContentFilter.get(0);

            if (contentFilterTypeMap.containsKey(filterItem)) {
                // TODO maybe contentFilterTypeMap is not needed.
                // TODO We could store it in filterItem
                final TypeFilter cf = contentFilterTypeMap.get(filterItem);

                final String sp;
                if (selectedSortFilter != null) {
                    sp = buildSortFilterYoutube(cf);
                } else {
                    sp = "";
                }

                if (filterItem instanceof MusicYoutubeContentFilterItem) {
                    final MusicYoutubeContentFilterItem realFilterItem =
                            (MusicYoutubeContentFilterItem) filterItem;
                    try {
                        return realFilterItem.searchUrl
                                + URLEncoder.encode(searchString, UTF_8);
                    } catch (final UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                } else if (filterItem instanceof YoutubeContentFilterItem) {
                    final YoutubeContentFilterItem realFilterItem =
                            (YoutubeContentFilterItem) filterItem;
                    try {
                        realFilterItem.setParams(sp);
                        return realFilterItem.searchUrl
                                + URLEncoder.encode(searchString, UTF_8)
                                + "&sp=" + sp;
                    } catch (final UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            }
        }

        throw new RuntimeException("we have a problem here");
    }

    private int addFilterItem(final FilterItem filter,
                              final TypeFilter correspondingContentFilterType) {
        final int filterId = builder.addFilterItem(filter);
        contentFilterTypeMap.put(filter, correspondingContentFilterType);
        return filterId;
    }

    @Override
    protected void init() {
        /* content filters with sort filters */
        final int contentFilterAllId = addFilterItem(
                new YoutubeContentFilterItem(ALL), null);
        final int contentFilterVideos = addFilterItem(
                new YoutubeContentFilterItem(VIDEOS), TypeFilter.video);
        final int contentFilterChannels = addFilterItem(
                new YoutubeContentFilterItem(CHANNELS), TypeFilter.channel);
        final int contentFilterPlaylists = addFilterItem(
                new YoutubeContentFilterItem(PLAYLISTS), TypeFilter.playlist);
        /* movies are only available for logged in users
        final int contentFilterMovies = addFilterItem(
                new YoutubeContentFilterItem(MOVIES), TypeFilter.movie);
         */

        /* set default content filter */
        this.defaultContentFilterId = contentFilterAllId;

        addContentFilter(builder.createSortGroup(null, true, new FilterItem[]{
                builder.getFilterForId(contentFilterAllId),
                builder.getFilterForId(contentFilterVideos),
                builder.getFilterForId(contentFilterChannels),
                builder.getFilterForId(contentFilterPlaylists),
                //builder.getFilterForId(contentFilterMovies)
        }));

        /* content filters without sort filters */
        addContentFilter(builder.createSortGroup("YouTube Music", true, new FilterItem[]{
                builder.getFilterForId(addFilterItem(
                        new MusicYoutubeContentFilterItem(MUSIC_SONGS,
                                "Eg-KAQwIARAAGAAgACgAMABqChAEEAUQAxAKEAk%3D"
                        ), null)),
                builder.getFilterForId(addFilterItem(
                        new MusicYoutubeContentFilterItem(MUSIC_VIDEOS,
                                "Eg-KAQwIABABGAAgACgAMABqChAEEAUQAxAKEAk%3D"
                        ), null)),
                builder.getFilterForId(addFilterItem(
                        new MusicYoutubeContentFilterItem(MUSIC_ALBUMS,
                                "Eg-KAQwIABAAGAEgACgAMABqChAEEAUQAxAKEAk%3D"
                        ), null)),
                builder.getFilterForId(addFilterItem(
                        new MusicYoutubeContentFilterItem(MUSIC_PLAYLISTS,
                                "Eg-KAQwIABAAGAAgACgBMABqChAEEAUQAxAKEAk%3D"
                        ), null)),
                builder.getFilterForId(addFilterItem(
                        new MusicYoutubeContentFilterItem(MUSIC_ARTISTS,
                                "Eg-KAQwIABAAGAAgASgAMABqChAEEAUQAxAKEAk%3D"
                        ), null))
        }));


        /* sort filters */

        /* 'Sort order' filter items */
        final int filterIdSortByRelevance = builder.addSortItem(
                new YoutubeSortOrderSortFilterItem("sort_relevance", SortOrder.relevance));
        final int filterIdSortByRating = builder.addSortItem(
                new YoutubeSortOrderSortFilterItem("sort_rating", SortOrder.rating));
        final int filterIdSortByViews = builder.addSortItem(
                new YoutubeSortOrderSortFilterItem("sort_view", SortOrder.views));

        /* 'Date' filter items */
        final int filterIdDateAll = builder.addSortItem(
                new YoutubeDateSortFilterItem("all", null));
        final int filterIdDateLastHour = builder.addSortItem(
                new YoutubeDateSortFilterItem("past_hour", DateFilter.hour));
        final int filterIdDateLastDay = builder.addSortItem(
                new YoutubeDateSortFilterItem("past_day", DateFilter.day));
        final int filterIdDateLastWeek = builder.addSortItem(
                new YoutubeDateSortFilterItem("past_week", DateFilter.week));
        final int filterIdDateLastMonth = builder.addSortItem(
                new YoutubeDateSortFilterItem("past_month", DateFilter.month));
        final int filterIdDateLastYear = builder.addSortItem(
                new YoutubeDateSortFilterItem("past_year", DateFilter.year));

        /* 'Duration' filter items */
        final int filterIdDurationAll = builder.addSortItem(
                new YoutubeLenSortFilterItem("all", null));
        final int filterIdDurationShort = builder.addSortItem(
                new YoutubeLenSortFilterItem("short_video", LenFilter.duration_short));
        final int filterIdDurationLong = builder.addSortItem(
                new YoutubeLenSortFilterItem("long_video", LenFilter.duration_long));

        /* 'features' filter items */
        final int filterIdFeatureHd = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("HD", Features.is_hd));
        final int filterIdFeatureSubtitles = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Subtitles", Features.subtitles));
        final int filterIdFeatureCcommons = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Ccommons", Features.ccommons));
        final int filterIdFeature3d = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("3d", Features.is_3d));
        final int filterIdFeatureLive = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Live", Features.live));
        final int filterIdFeaturePurchased = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Purchased", Features.purchased));
        final int filterIdFeature4k = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("4k", Features.is_4k));
        final int filterIdFeature360 = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("360°", Features.is_360));
        final int filterIdFeatureLocation = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Location", Features.location));
        final int filterIdFeatureHdr = builder.addSortItem(
                new YoutubeFeatureSortFilterItem("Hdr", Features.is_hdr));


        final FilterGroup[] videoFilters = new FilterGroup[]{
                builder.createSortGroup("sortby", true, new FilterItem[]{
                        builder.getFilterForId(filterIdSortByRelevance),
                        builder.getFilterForId(filterIdSortByRating),
                        builder.getFilterForId(filterIdSortByViews),
                }),
                builder.createSortGroup("upload_date", true, new FilterItem[]{
                        builder.getFilterForId(filterIdDateAll),
                        builder.getFilterForId(filterIdDateLastHour),
                        builder.getFilterForId(filterIdDateLastDay),
                        builder.getFilterForId(filterIdDateLastWeek),
                        builder.getFilterForId(filterIdDateLastMonth),
                        builder.getFilterForId(filterIdDateLastYear),
                }),
                builder.createSortGroup("duration", true, new FilterItem[]{
                        builder.getFilterForId(filterIdDurationAll),
                        builder.getFilterForId(filterIdDurationShort),
                        builder.getFilterForId(filterIdDurationLong),
                }),
                builder.createSortGroup("features", false, new FilterItem[]{
                        builder.getFilterForId(filterIdFeatureHd),
                        builder.getFilterForId(filterIdFeatureSubtitles),
                        builder.getFilterForId(filterIdFeatureCcommons),
                        builder.getFilterForId(filterIdFeature3d),
                        builder.getFilterForId(filterIdFeatureLive),
                        builder.getFilterForId(filterIdFeaturePurchased),
                        builder.getFilterForId(filterIdFeature4k),
                        builder.getFilterForId(filterIdFeature360),
                        builder.getFilterForId(filterIdFeatureLocation),
                        builder.getFilterForId(filterIdFeatureHdr),
                })
        };

        addContentFilterTypeAndSortVariant(contentFilterAllId,
                new Filter.Builder(videoFilters).build());
        addContentFilterTypeAndSortVariant(contentFilterVideos,
                new Filter.Builder(videoFilters).build());

        addContentFilterTypeAndSortVariant(contentFilterChannels,
                new Filter.Builder(new FilterGroup[]{
                        builder.createSortGroup("sortby", true, new FilterItem[]{
                                builder.getFilterForId(filterIdSortByRelevance),
                                builder.getFilterForId(filterIdSortByRating),
                                builder.getFilterForId(filterIdSortByViews),
                        })
                }).build());

        addContentFilterTypeAndSortVariant(contentFilterPlaylists,
                new Filter.Builder(new FilterGroup[]{
                        builder.createSortGroup("sortby", true, new FilterItem[]{
                                builder.getFilterForId(filterIdSortByRelevance),
                                builder.getFilterForId(filterIdSortByRating),
                                builder.getFilterForId(filterIdSortByViews),
                        })
                }).build());

        /* movies are only available for logged in users
        addContentFilterTypeAndSortVariant(contentFilterMovies,
                new Filter.Builder(new FilterGroup[]{
                        builder.createSortGroup("Sort by", true, new FilterItem[]{
                                builder.getFilterForId(filterIdSortByRelevance),
                                builder.getFilterForId(filterIdSortByRating),
                                builder.getFilterForId(filterIdSortByViews),
                        }),
                        builder.createSortGroup("Upload Date", true, new FilterItem[]{
                                builder.getFilterForId(filterIdDateAll),
                                builder.getFilterForId(filterIdDateLastHour),
                                builder.getFilterForId(filterIdDateLastDay),
                                builder.getFilterForId(filterIdDateLastWeek),
                                builder.getFilterForId(filterIdDateLastMonth),
                                builder.getFilterForId(filterIdDateLastYear),
                        }),
                        builder.createSortGroup("Duration", true, new FilterItem[]{
                                builder.getFilterForId(filterIdDurationAll),
                                builder.getFilterForId(filterIdDurationShort),
                                builder.getFilterForId(filterIdDurationLong),
                        }),
                        builder.createSortGroup("Features", false, new FilterItem[]{
                                builder.getFilterForId(filterIdFeatureHd),
                                builder.getFilterForId(filterIdFeaturePurchased),
                                builder.getFilterForId(filterIdFeature4k),
                                builder.getFilterForId(filterIdFeature360),
                                builder.getFilterForId(filterIdFeatureLocation),
                                builder.getFilterForId(filterIdFeatureHdr),
                        })
                }).build());
         */
    }

    private void addContentFilterTypeAndSortVariant(final int contentFilterId,
                                                    final Filter variant) {
        addContentFilterSortVariant(-1, variant);
        addContentFilterSortVariant(contentFilterId, variant);
    }

    private static class YoutubeSortOrderSortFilterItem extends YoutubeSortFilterItem {
        private final SortOrder sortOrder;

        YoutubeSortOrderSortFilterItem(final String name, final SortOrder sortOrder) {
            super(name);
            this.sortOrder = sortOrder;
        }

        public SortOrder get() {
            return sortOrder;
        }
    }

    private static class YoutubeDateSortFilterItem extends YoutubeSortFilterItem {
        private final DateFilter dateFilter;

        YoutubeDateSortFilterItem(final String name, final DateFilter dateFilter) {
            super(name);
            this.dateFilter = dateFilter;
        }

        public DateFilter get() {
            return this.dateFilter;
        }
    }

    private static class YoutubeLenSortFilterItem extends YoutubeSortFilterItem {
        private final LenFilter lenFilter;

        YoutubeLenSortFilterItem(final String name, final LenFilter lenFilter) {
            super(name);
            this.lenFilter = lenFilter;
        }

        public LenFilter get() {
            return this.lenFilter;
        }
    }

    private static class YoutubeFeatureSortFilterItem extends YoutubeSortFilterItem {
        private final Features feature;

        YoutubeFeatureSortFilterItem(final String name, final Features feature) {
            super(name);
            this.feature = feature;
        }

        public Features get() {
            return this.feature;
        }
    }

    public static class YoutubeSortFilterItem extends FilterItem {

        public YoutubeSortFilterItem(final String name) {
            super(Filter.ITEM_IDENTIFIER_UNKNOWN, name);
        }
    }

    public static class YoutubeContentFilterItem extends YoutubeSortFilterItem {
        public final String searchUrl;
        protected String params;

        public YoutubeContentFilterItem(final String name) {
            super(name);
            this.searchUrl = SEARCH_URL;
            this.params = "";
        }

        public YoutubeContentFilterItem(final String name, final String searchUrl) {
            super(name);
            this.searchUrl = searchUrl;
        }

        public String getParams() {
            return params;
        }

        public void setParams(String params) {
            this.params = params;
        }
    }

    public static class MusicYoutubeContentFilterItem extends YoutubeContentFilterItem {
        public MusicYoutubeContentFilterItem(final String name, final String params) {
            super(name, MUSIC_SEARCH_URL);
            this.params = params;
        }
    }
}
