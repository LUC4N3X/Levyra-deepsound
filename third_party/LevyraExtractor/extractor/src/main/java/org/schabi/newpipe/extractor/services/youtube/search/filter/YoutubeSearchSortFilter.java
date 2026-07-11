package org.schabi.newpipe.extractor.services.youtube.search.filter;

import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.DateFilter;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.ExtraFeatures;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.Extras;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.Features;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.Filters;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.LenFilter;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.SearchRequest;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.SortOrder;
import org.schabi.newpipe.extractor.services.youtube.search.filter.protobuf.TypeFilter;

import java.io.IOException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.util.ArrayList;
import java.util.Objects;

import okio.ByteString;
import static org.schabi.newpipe.extractor.NewPipe.getDownloader;


public final class YoutubeSearchSortFilter {

    private static final String UTF_8 = "UTF_8";
    private String searchParameter = "";

    public YoutubeSearchSortFilter() {
    }

    @SuppressWarnings("NewApi")
    public String encodeSp(final SortOrder sort, final DateFilter date,
                           final TypeFilter type, final LenFilter len,
                           final Features[] features, final ExtraFeatures[] extraFeatures)
            throws IOException {

        final Filters.Builder filtersBuilder = new Filters.Builder();
        if (null != date) {
            filtersBuilder.date((long) date.getValue());
        }

        if (null != type) {
            filtersBuilder.type((long) type.getValue());
        }

        if (null != len) {
            filtersBuilder.length((long) len.getValue());
        }

        if (null != features) {
            for (final Features feature : features) {
                setFeatureState(feature, true, filtersBuilder);
            }
        }

        final SearchRequest.Builder searchRequestBuilder = new SearchRequest.Builder();
        if (null != sort) {
            searchRequestBuilder.sorted((long) sort.getValue());
        }

        if (null != date || null != type || null != len
                || null != features || null != extraFeatures) {
            final Filters filters = filtersBuilder.build();
            searchRequestBuilder.filter(filters);
        }

        if (null != extraFeatures && extraFeatures.length > 0) {
            final Extras.Builder extrasBuilder = new Extras.Builder();
            for (final ExtraFeatures extra : extraFeatures) {
                setExtraState(extra, true, extrasBuilder);
            }
            final Extras extras = extrasBuilder.build();
            searchRequestBuilder.extras(extras);
        }
        final SearchRequest searchRequest = searchRequestBuilder.build();
        try {
            final byte[] protoBufEncoded = searchRequest.encode();
            final ByteString bs = new ByteString(protoBufEncoded);
            final String protoBufEncodedBase64 = bs.base64();
            this.searchParameter
                    = URLEncoder.encode(protoBufEncodedBase64, UTF_8);
        } catch (NoClassDefFoundError e){
            throw new RuntimeException("Base64 error. This shouldn't happen", e);
        }
        if(Objects.equals(this.searchParameter, "EgA%3D")) {
            this.searchParameter = "8AEB"; // https://github.com/InfinityLoop1308/PipePipe/issues/302
        }
        return this.searchParameter;
    }

    @SuppressWarnings("NewApi")
    public SearchRequest decodeSp(final String urlEncodedBase64EncodedSearchParameter)
            throws IOException {
        final String urlDecodedBase64EncodedSearchParameter
                = URLDecoder.decode(urlEncodedBase64EncodedSearchParameter, UTF_8);
        final byte[] decodedSearchParameter
                = ByteString.decodeBase64(urlDecodedBase64EncodedSearchParameter).toByteArray();
        final SearchRequest decodedSearchRequest
                = new SearchRequest.Builder().build().adapter().decode(decodedSearchParameter);

        return decodedSearchRequest;
    }

    public String getSp() {
        return this.searchParameter;
    }

    private void setExtraState(final ExtraFeatures extra,
                               final boolean enable,
                               final Extras.Builder extrasBuilder) {
        switch (extra) {
            case verbatim:
                extrasBuilder.verbatim(enable);
                break;
        }
    }

    private void setFeatureState(final Features feature,
                                 final boolean enable,
                                 final Filters.Builder filtersBuilder) {
        switch (feature) {
            case is_hd:
                filtersBuilder.is_hd(enable);
                break;
            case subtitles:
                filtersBuilder.subtitles(enable);
                break;
            case ccommons:
                filtersBuilder.ccommons(enable);
                break;
            case is_3d:
                filtersBuilder.is_3d(enable);
                break;
            case live:
                filtersBuilder.live(enable);
                break;
            case purchased:
                filtersBuilder.purchased(enable);
                break;
            case is_4k:
                filtersBuilder.is_4k(enable);
                break;
            case is_360:
                filtersBuilder.is_360(enable);
                break;
            case location:
                filtersBuilder.location(enable);
                break;
            case is_hdr:
                filtersBuilder.is_hdr(enable);
                break;
        }
    }

    public static class Builder {
        private final ArrayList<Features> featureList = new ArrayList<>();
        private final ArrayList<Extras> extraList = new ArrayList<>();
        private final YoutubeSearchSortFilter youtubeSearchSortFilter =
                new YoutubeSearchSortFilter();
        private SortOrder sort = null;
        private DateFilter date = null;
        private TypeFilter type = null;
        private LenFilter len = null;

        public Builder() {
        }

        public Builder setSortOrder(final SortOrder sortOrder) {
            this.sort = sortOrder;
            return this;
        }

        public Builder setDateFilter(final DateFilter dateFilter) {
            this.date = dateFilter;
            return this;
        }

        public Builder setTypeFilter(final TypeFilter typeFilter) {
            this.type = typeFilter;
            return this;
        }

        public Builder setLenFilter(final LenFilter lenFilter) {
            this.len = lenFilter;
            return this;
        }

        public Builder addFeature(final Features feature) {
            this.featureList.add(feature);
            return this;
        }

        public Builder addExtra(final Extras extra) {
            this.extraList.add(extra);
            return this;
        }

        public YoutubeSearchSortFilter build() throws IOException {
            final Features[] features
                    = this.featureList.toArray(new Features[this.featureList.size()]);
            final ExtraFeatures[] extras
                    = this.extraList.toArray(new ExtraFeatures[this.extraList.size()]);
            this.youtubeSearchSortFilter
                    .encodeSp(this.sort, this.date, this.type, this.len, features, extras);

            return this.youtubeSearchSortFilter;
        }
    }
}
