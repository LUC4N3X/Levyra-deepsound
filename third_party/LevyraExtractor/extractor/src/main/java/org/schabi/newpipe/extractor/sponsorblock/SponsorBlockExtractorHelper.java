package org.schabi.newpipe.extractor.sponsorblock;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.StreamingService;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.services.bilibili.BilibiliService;
import org.schabi.newpipe.extractor.stream.StreamExtractor;
import org.schabi.newpipe.extractor.stream.StreamInfo;
import org.schabi.newpipe.extractor.utils.Utils;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.NoSuchAlgorithmException;
import java.util.ArrayList;

public final class SponsorBlockExtractorHelper {

    private SponsorBlockExtractorHelper() {
    }

    public static SponsorBlockSegment[] getSegments(final StreamExtractor extractor,
                                                    final SponsorBlockApiSettings apiSettings)
            throws UnsupportedEncodingException, ParsingException {
        if (!extractor.getService().getServiceInfo()
                .getMediaCapabilities()
                .contains(StreamingService.ServiceInfo.MediaCapability.SPONSORBLOCK)) {
            return new SponsorBlockSegment[0];
        }

        String videoId = extractor.getId();
        videoId = videoId.split("\\?")[0];

        final ArrayList<String> categoryParamList = new ArrayList<>();

        if (apiSettings.includeSponsorCategory) {
            categoryParamList.add(SponsorBlockCategory.SPONSOR.getApiName());
        }
        if (apiSettings.includeIntroCategory) {
            categoryParamList.add(SponsorBlockCategory.INTRO.getApiName());
        }
        if (apiSettings.includeOutroCategory) {
            categoryParamList.add(SponsorBlockCategory.OUTRO.getApiName());
        }
        if (apiSettings.includeInteractionCategory) {
            categoryParamList.add(SponsorBlockCategory.INTERACTION.getApiName());
        }
        if (apiSettings.includeHighlightCategory) {
            categoryParamList.add(SponsorBlockCategory.HIGHLIGHT.getApiName());
        }
        if (apiSettings.includeSelfPromoCategory) {
            categoryParamList.add(SponsorBlockCategory.SELF_PROMO.getApiName());
        }
        if (apiSettings.includeMusicCategory) {
            categoryParamList.add(SponsorBlockCategory.NON_MUSIC.getApiName());
        }
        if (apiSettings.includePreviewCategory) {
            categoryParamList.add(SponsorBlockCategory.PREVIEW.getApiName());
        }

        if (apiSettings.includeFillerCategory) {
            categoryParamList.add(SponsorBlockCategory.FILLER.getApiName());
        }

        if (categoryParamList.isEmpty()) {
            return new SponsorBlockSegment[0];
        }

        final String categoryParams = Utils.encodeUrlUtf8(
                "[\"" + String.join("\",\"", categoryParamList) + "\"]");

        final String actionParams = Utils.encodeUrlUtf8("[\"skip\",\"poi\"]");

        final String videoIdHash;
        try {
            videoIdHash = Utils.toSha256(videoId);
        } catch (NoSuchAlgorithmException e) {
            return new SponsorBlockSegment[0];
        }

        final String url = getApiUrl(extractor.getServiceId()) + "skipSegments/" + videoIdHash.substring(0, 4)
                + "?categories=" + categoryParams
                + "&actionTypes=" + actionParams
                + "&userAgent=Mozilla/5.0";

        JsonArray responseArray = null;

        try {
            final String responseBody = NewPipe.getDownloader().get(url, url.contains("bsbsb.top")? BilibiliService.getSponsorBlockHeaders(): null).responseBody();

            responseArray = JsonParser.array().from(responseBody);
        } catch (ReCaptchaException | IOException | JsonParserException e) {
            // ignored
        }

        if (responseArray == null) {
            return new SponsorBlockSegment[0];
        }

        final ArrayList<SponsorBlockSegment> result = new ArrayList<>();

        for (final Object obj1 : responseArray) {
            final JsonObject jObj1 = (JsonObject) obj1;

            final String responseVideoId = jObj1.getString("videoID");
            if (!responseVideoId.equals(videoId)) {
                continue;
            }

            final JsonArray segmentArray = (JsonArray) jObj1.get("segments");
            if (segmentArray == null) {
                continue;
            }

            for (final Object obj2 : segmentArray) {
                final JsonObject jObj2 = (JsonObject) obj2;

                final JsonArray segmentInfo = (JsonArray) jObj2.get("segment");
                if (segmentInfo == null) {
                    continue;
                }

                final String uuid = jObj2.getString("UUID");
                final double startTime = segmentInfo.getDouble(0) * 1000;
                final double endTime = segmentInfo.getDouble(1) * 1000;
                final String category = jObj2.getString("category");
                final String action = jObj2.getString("actionType");

                final SponsorBlockSegment sponsorBlockSegment =
                        new SponsorBlockSegment(uuid, startTime, endTime,
                                SponsorBlockCategory.fromApiName(category),
                                SponsorBlockAction.fromApiName(action),
                                extractor.getServiceId());
                result.add(sponsorBlockSegment);
            }
        }

        return result.toArray(new SponsorBlockSegment[0]);
    }

    public static Response submitSponsorBlockSegment(
            final StreamInfo streamInfo,
            final SponsorBlockSegment segment,
            final String apiUrl,
            final String userId)
            throws IOException, ReCaptchaException {
        if (segment.category == SponsorBlockCategory.PENDING) {
            return null;
        }


        String videoId = streamInfo.getId();
        videoId = videoId.split("\\?")[0];

        final String actionType = segment.category == SponsorBlockCategory.HIGHLIGHT
                ? "poi"
                : "skip";

        final double startInSeconds = segment.startTime / 1000.0;
        final double endInSeconds = segment.category == SponsorBlockCategory.HIGHLIGHT
                ? startInSeconds
                : segment.endTime / 1000.0;

        final String url = apiUrl + "skipSegments?"
                + "videoID=" + videoId
                + "&startTime=" + startInSeconds
                + "&endTime=" + endInSeconds
                + "&category=" + segment.category.getApiName()
                + "&userID=" + userId
                + "&userAgent=PipePipe/1.1.0"
                + "&actionType=" + actionType;
        return NewPipe.getDownloader().post(url, apiUrl.contains("bsbsb.top")? BilibiliService.getSponsorBlockHeaders(): null, new byte[0]);
    }

    public static Response submitSponsorBlockSegmentVote(final String uuid,
                                                          final String apiUrl,
                                                          final int vote,
                                                          final String userId)
            throws IOException, ReCaptchaException {
        final String url = apiUrl + "voteOnSponsorTime?"
                + "UUID=" + uuid
                + "&userID=" + userId
                + "&type=" + vote;

        return NewPipe.getDownloader().post(url, apiUrl.contains("bsbsb.top")? BilibiliService.getSponsorBlockHeaders(): null, new byte[0]);
    }

    public static String getApiUrl(StreamInfo streamInfo) {
        return getApiUrl(streamInfo.getServiceId());
    }

    public static String getApiUrl(int serviceId) {
        String apiUrl = "https://sponsor.ajay.app/api/";
        if (serviceId == ServiceList.YouTube.getServiceId()) {
            apiUrl = "https://sponsor.ajay.app/api/";
        } else if (serviceId == ServiceList.BiliBili.getServiceId()) {
            apiUrl = "https://bsbsb.top/api/";
        }
        return apiUrl;
    }
}
