/*
 * Created by Christian Schabesberger on 06.08.15.
 *
 * Copyright (C) Christian Schabesberger 2019 <chris.schabesberger@mailbox.org>
 * YoutubeStreamExtractor.java is part of NewPipe Extractor.
 *
 * NewPipe Extractor is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * NewPipe Extractor is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with NewPipe Extractor. If not, see <https://www.gnu.org/licenses/>.
 */

package org.schabi.newpipe.extractor.services.youtube.extractors;

import com.grack.nanojson.*;
import org.apache.commons.lang3.StringUtils;
import org.json.JSONException;
import org.json.JSONObject;
import org.schabi.newpipe.extractor.*;
import org.schabi.newpipe.extractor.channel.StaffInfoItem;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.*;
import org.schabi.newpipe.extractor.linkhandler.LinkHandler;
import org.schabi.newpipe.extractor.localization.*;
import org.schabi.newpipe.extractor.services.youtube.*;
import org.schabi.newpipe.extractor.services.youtube.linkHandler.YoutubeChannelLinkHandlerFactory;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrClientProfile;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrInfo;
import org.schabi.newpipe.extractor.services.youtube.sabr.YoutubeSabrProbe;
import org.schabi.newpipe.extractor.stream.*;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.utils.Parser;
import org.schabi.newpipe.extractor.utils.SubtitleDeduplicator;
import org.schabi.newpipe.extractor.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static java.util.Collections.singletonList;
import static org.schabi.newpipe.extractor.services.youtube.ItagItem.APPROX_DURATION_MS_UNKNOWN;
import static org.schabi.newpipe.extractor.services.youtube.ItagItem.CONTENT_LENGTH_UNKNOWN;
import static org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper.*;
import static org.schabi.newpipe.extractor.utils.Utils.EMPTY_STRING;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

public class YoutubeStreamExtractor extends StreamExtractor {
    private JSONObject dislikeData;
    private JsonObject playerCaptionsTracklistRenderer;
    /*//////////////////////////////////////////////////////////////////////////
    // Exceptions
    //////////////////////////////////////////////////////////////////////////*/

    public static class DeobfuscateException extends ParsingException {
        DeobfuscateException(final String message, final Throwable cause) {
            super(message, cause);
        }
    }

    /*////////////////////////////////////////////////////////////////////////*/
    public JsonObject playerResponse;
    private JsonObject nextResponse;
    @Nullable
    private JsonObject androidPlayerResponse;
    @Nullable
    private JsonObject safariPlayerResponse;
    @Nullable
    private JsonObject tvHtml5PlayerResponse;

    private JsonObject webStreamingData;
    @Nullable
    private JsonObject mwebStreamingData;
    @Nullable
    private JsonObject androidStreamingData;
    @Nullable
    private JsonObject safariStreamingData;
    @Nullable
    private JsonObject tvHtml5SimplyEmbedStreamingData;
    @Nullable
    private JsonObject configuredStreamingData;
    private String mwebHlsManifestUrl = EMPTY_STRING;

    private JsonObject videoPrimaryInfoRenderer;
    private JsonObject videoSecondaryInfoRenderer;
    public JsonObject playerMicroFormatRenderer;
    private int ageLimit = -1;
    private StreamType streamType;
    private volatile long availableAt = Stream.AVAILABLE_AT_UNKNOWN;
    private volatile double finalWaitSeconds;

    // We need to store the contentPlaybackNonces because we need to append them to videoplayback
    // URLs (with the cpn parameter).
    // Also because a nonce should be unique, it should be different between clients used, so
    // two different strings are used.
    private String webCpn;
    private String mwebCpn;
    private String androidCpn;
    private String safariCpn;
    private String tvHtml5SimplyEmbedCpn;
    private String configuredCpn;

    public WatchDataCache watchDataCache;

    public final ArrayList<Throwable> errors = new ArrayList<>();

    public void addError(final Throwable error) {
        synchronized (errors) {
            errors.add(error);
        }
    }

    public YoutubeStreamExtractor(final StreamingService service, final LinkHandler linkHandler, WatchDataCache watchDataCache) {
        super(service, linkHandler);
        this.watchDataCache = watchDataCache;
        watchDataCache.init(linkHandler.getUrl());
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Impl
    //////////////////////////////////////////////////////////////////////////*/

    @Nonnull
    @Override
    public String getName() throws ParsingException {
        assertPageFetched();
        String title = null;
//
//        try {
//            title = getTextFromObject(getVideoPrimaryInfoRenderer().getObject("title"));
//        } catch (final ParsingException ignored) {
//            // Age-restricted videos cause a ParsingException here
//        }

        if (isNullOrEmpty(title)) {
            title = playerResponse.getObject("videoDetails").getString("title");

            if (isNullOrEmpty(title)) {
                title = getTextFromObject(getVideoPrimaryInfoRenderer().getObject("title"));
            }

            if (isNullOrEmpty(title)) {
                throw new ParsingException("Could not get name");
            }
        }

        return title;
    }

    @Override
    public long getStartAt() throws ParsingException {
        return getUploadDate().offsetDateTime().toEpochSecond() * 1000;
    }

    @Nullable
    @Override
    public String getTextualUploadDate() throws ParsingException {
        watchDataCache.shouldBeLive = true;
        final JsonObject liveDetails = playerMicroFormatRenderer.getObject(
                "liveBroadcastDetails");
        if (!liveDetails.getString("endTimestamp", EMPTY_STRING).isEmpty()) {
            // an ended live stream
            return liveDetails.getString("endTimestamp");
        } else if (!liveDetails.getString("startTimestamp", EMPTY_STRING).isEmpty()) {
            // a running live stream
            return liveDetails.getString("startTimestamp");
        } else if (getStreamType() == StreamType.LIVE_STREAM) {
            // this should never be reached, but a live stream without upload date is valid
            return null;
        }

        watchDataCache.shouldBeLive = false;
        if (!playerMicroFormatRenderer.getString("uploadDate", EMPTY_STRING).isEmpty()) {
            return playerMicroFormatRenderer.getString("uploadDate");
        } else if (!playerMicroFormatRenderer.getString("publishDate", EMPTY_STRING).isEmpty()) {
            return playerMicroFormatRenderer.getString("publishDate");
        }

        final String videoPrimaryInfoRendererDateText =
                getTextFromObject(getVideoPrimaryInfoRenderer().getObject("dateText"));

        if (videoPrimaryInfoRendererDateText != null) {
            if (videoPrimaryInfoRendererDateText.startsWith("Premiered")) {
                final String time = videoPrimaryInfoRendererDateText.substring(13);

                try { // Premiered 20 hours ago
                    final TimeAgoParser timeAgoParser = TimeAgoPatternsManager.getTimeAgoParserFor(
                            Localization.fromLocalizationCode("en"));
                    final OffsetDateTime parsedTime = timeAgoParser.parse(time).offsetDateTime();
                    return DateTimeFormatter.ISO_LOCAL_DATE.format(parsedTime);
                } catch (final Exception ignored) {
                }

                try { // Premiered Feb 21, 2020
                    final LocalDate localDate = LocalDate.parse(time,
                            DateTimeFormatter.ofPattern("MMM dd, yyyy", Locale.ENGLISH));
                    return DateTimeFormatter.ISO_LOCAL_DATE.format(localDate);
                } catch (final Exception ignored) {
                }

                try { // Premiered on 21 Feb 2020
                    final LocalDate localDate = LocalDate.parse(time,
                            DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
                    return DateTimeFormatter.ISO_LOCAL_DATE.format(localDate);
                } catch (final Exception ignored) {
                }
            }

            try {
                // TODO: this parses English formatted dates only, we need a better approach to
                //  parse the textual date
                final LocalDate localDate = LocalDate.parse(videoPrimaryInfoRendererDateText,
                        DateTimeFormatter.ofPattern("dd MMM yyyy", Locale.ENGLISH));
                return DateTimeFormatter.ISO_LOCAL_DATE.format(localDate);
            } catch (final Exception e) {
                throw new ParsingException("Could not get upload date", e);
            }
        }

        throw new ParsingException("Could not get upload date");
    }

    @Override
    public DateWrapper getUploadDate() throws ParsingException {
        final String textualUploadDate = getTextualUploadDate();

        if (isNullOrEmpty(textualUploadDate)) {
            return null;
        }

        return new DateWrapper(YoutubeParsingHelper.parseDateFrom(textualUploadDate), true);
    }

    @Nonnull
    @Override
    public String getThumbnailUrl() throws ParsingException {
        assertPageFetched();
        try {
            final JsonArray thumbnails = playerResponse
                    .getObject("videoDetails")
                    .getObject("thumbnail")
                    .getArray("thumbnails");
            // the last thumbnail is the one with the highest resolution
            final String url = thumbnails
                    .getObject(thumbnails.size() - 1)
                    .getString("url");

            return fixThumbnailUrl(url);
        } catch (final Exception e) {
            throw new ParsingException("Could not get thumbnail url");
        }

    }

    @Nonnull
    @Override
    public List<Image> getThumbnails() throws ParsingException {
        assertPageFetched();
        try {
            return getImagesFromThumbnailsArray(playerResponse.getObject("videoDetails")
                    .getObject("thumbnail")
                    .getArray("thumbnails"));
        } catch (final Exception e) {
            throw new ParsingException("Could not get thumbnails");
        }
    }

    @Nonnull
    @Override
    public Description getDescription() throws ParsingException {
        assertPageFetched();
        // Description with more info on links
        try {
            final String description = getTextFromObject(
                    getVideoSecondaryInfoRenderer().getObject("description"),
                    true);
            if (!isNullOrEmpty(description)) {
                return new Description(description, Description.HTML);
            }
        } catch (final ParsingException ignored) {
            // Age-restricted videos cause a ParsingException here
        }

        String description = playerResponse.getObject("videoDetails")
                .getString("shortDescription");
        if (description == null) {
            final JsonObject descriptionObject = playerMicroFormatRenderer.getObject("description");
            description = getTextFromObject(descriptionObject);
        }

        // Raw non-html description
        return new Description(description, Description.PLAIN_TEXT);
    }

    @Override
    public long getLength() throws ParsingException {
        assertPageFetched();

        try {
            final String duration = playerResponse
                    .getObject("videoDetails")
                    .getString("lengthSeconds");
            return Long.parseLong(duration);
        } catch (final Exception ignored) {
            return getDurationFromFirstAdaptiveFormat(Arrays.asList(
                    androidStreamingData,
                    safariStreamingData,
                    tvHtml5SimplyEmbedStreamingData,
                    configuredStreamingData,
                    webStreamingData,
                    mwebStreamingData));
        }
    }

    private int getDurationFromFirstAdaptiveFormat(@Nonnull final List<JsonObject> streamingDatas)
            throws ParsingException {
        for (final JsonObject streamingData : streamingDatas) {
            if (streamingData == null || streamingData.isEmpty()) {
                continue;
            }
            final JsonArray adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS);
            if (adaptiveFormats == null || adaptiveFormats.isEmpty()) {
                continue;
            }

            final String durationMs = adaptiveFormats.getObject(0)
                    .getString("approxDurationMs");
            try {
                return Math.round(Long.parseLong(durationMs) / 1000f);
            } catch (final NumberFormatException ignored) {
            }
        }

        throw new ParsingException("Could not get duration");
    }

    /**
     * Attempts to parse (and return) the offset to start playing the video from.
     *
     * @return the offset (in seconds), or 0 if no timestamp is found.
     */
    @Override
    public long getTimeStamp() throws ParsingException {
        final long timestamp =
                getTimestampSeconds("((#|&|\\?)t=\\d*h?\\d*m?\\d+s?)");

        if (timestamp == -2) {
            // Regex for timestamp was not found
            return 0;
        }
        return timestamp;
    }

    @Override
    public long getViewCount() throws ParsingException {
        String views = null;

        try {
            views = getTextFromObject(getVideoPrimaryInfoRenderer().getObject("viewCount")
                    .getObject("videoViewCountRenderer").getObject("viewCount"));
        } catch (final ParsingException ignored) {
            // Age-restricted videos cause a ParsingException here
        }

        if (isNullOrEmpty(views)) {
            views = playerResponse.getObject("videoDetails").getString("viewCount");

            if (isNullOrEmpty(views)) {
                throw new ParsingException("Could not get view count");
            }
        }

        if (views.toLowerCase().contains("no views")
                || views.toLowerCase().contains("akukho ukubukwa")
                || views.toLowerCase().contains("akukho kubukwa")) {
            return 0;
        }

        return Long.parseLong(Utils.removeNonDigitCharacters(views));
    }

    @Override
    public long getLikeCount() throws ParsingException {
        assertPageFetched();

        // If ratings are not allowed, there is no like count available
        if (!playerResponse.getObject("videoDetails").getBoolean("allowRatings")) {
            return -1;
        }

        String likesString = null;

        try {
            final List<JsonObject> topLevelButtons = getVideoPrimaryInfoRenderer()
                    .getObject("videoActions")
                    .getObject("menuRenderer")
                    .getArray("topLevelButtons")
                    .stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .collect(Collectors.toList());

            likesString = topLevelButtons.stream()
                    .map(btn -> btn.getObject("segmentedLikeDislikeButtonViewModel")
                            .getObject("likeButtonViewModel")
                            .getObject("likeButtonViewModel")
                            .getObject("toggleButtonViewModel")
                            .getObject("toggleButtonViewModel")
                            .getObject("defaultButtonViewModel")
                            .getObject("buttonViewModel")
                            .getString("accessibilityText"))
                    .filter(Objects::nonNull)
                    .findFirst()
                    .orElse(null);

            // Old - pre Dec 2023 way
            if (likesString == null) {
                likesString = getPreDec2023LikeString(topLevelButtons);
            }

            // If ratings are allowed and the likes string is null, it means that we couldn't
            // extract the (real) like count from accessibility data
            if (likesString == null) {
                throw new ParsingException("Could not get like count from accessibility data");
            }

            // This check only works with English localizations!
            if (likesString.toLowerCase().contains("no likes")) {
                return 0;
            }

            return Integer.parseInt(Utils.removeNonDigitCharacters(likesString));
        } catch (final NumberFormatException nfe) {
            throw new ParsingException("Could not parse \"" + likesString + "\" as an Integer",
                    nfe);
        } catch (final Exception e) {
            throw new ParsingException("Could not get like count", e);
        }
    }

    protected String getPreDec2023LikeString(final List<JsonObject> topLevelButtons)
            throws ParsingException {
        // Try first with the new video actions buttons data structure
        JsonObject likeToggleButtonRenderer = topLevelButtons.stream()
                .map(button -> button.getObject("segmentedLikeDislikeButtonRenderer")
                        .getObject("likeButton")
                        .getObject("toggleButtonRenderer"))
                .filter(toggleButtonRenderer -> !isNullOrEmpty(toggleButtonRenderer))
                .findFirst()
                .orElse(null);

        // Use the old video actions buttons data structure if the new one isn't returned
        if (likeToggleButtonRenderer == null) {
            /*
            In the old video actions buttons data structure, there are 3 ways to detect whether
            a button is the like button, using its toggleButtonRenderer:
            - checking whether toggleButtonRenderer.targetId is equal to watch-like;
            - checking whether toggleButtonRenderer.defaultIcon.iconType is equal to LIKE;
            - checking whether
              toggleButtonRenderer.toggleButtonSupportedData.toggleButtonIdData.id
              is equal to TOGGLE_BUTTON_ID_TYPE_LIKE.
            */
            likeToggleButtonRenderer = topLevelButtons.stream()
                    .map(topLevelButton -> topLevelButton.getObject("toggleButtonRenderer"))
                    .filter(toggleButtonRenderer -> "watch-like".equalsIgnoreCase(
                            toggleButtonRenderer.getString("targetId"))
                            || "LIKE".equalsIgnoreCase(
                            toggleButtonRenderer.getObject("defaultIcon")
                                    .getString("iconType"))
                            || "TOGGLE_BUTTON_ID_TYPE_LIKE".equalsIgnoreCase(
                            toggleButtonRenderer.getObject("toggleButtonSupportedData")
                                    .getObject("toggleButtonIdData")
                                    .getString("id")))
                    .findFirst()
                    .orElseThrow(() -> new ParsingException(
                            "The like button is missing even though ratings are enabled"));
        }

        // Use one of the accessibility strings available (this one has the same path as the
        // one used for comments' like count extraction)
        String likesString = likeToggleButtonRenderer.getObject("accessibilityData")
                .getObject("accessibilityData")
                .getString("label");

        // Use the other accessibility string available which contains the exact like count
        if (likesString == null) {
            likesString = likeToggleButtonRenderer.getObject("accessibility")
                    .getString("label");
        }

        // Last method: use the defaultText's accessibility data, which contains the exact like
        // count too, except when it is equal to 0, where a localized string is returned instead
        if (likesString == null) {
            likesString = likeToggleButtonRenderer.getObject("defaultText")
                    .getObject("accessibility")
                    .getObject("accessibilityData")
                    .getString("label");
        }
        return likesString;
    }


    @Nonnull
    @Override
    public String getUploaderUrl() throws ParsingException {
        assertPageFetched();

        // Don't use the id in the videoSecondaryRenderer object to get real id of the uploader
        // The difference between the real id of the channel and the displayed id is especially
        // visible for music channels and autogenerated channels.
        final String uploaderId = playerResponse.getObject("videoDetails").getString("channelId");
        if (!isNullOrEmpty(uploaderId)) {
            return YoutubeChannelLinkHandlerFactory.getInstance().getUrl("channel/" + uploaderId);
        }

        throw new ParsingException("Could not get uploader url");
    }

    @Nonnull
    @Override
    public String getUploaderName() throws ParsingException {
        assertPageFetched();

        // Don't use the name in the videoSecondaryRenderer object to get real name of the uploader
        // The difference between the real name of the channel and the displayed name is especially
        // visible for music channels and autogenerated channels.
        final String uploaderName = playerResponse.getObject("videoDetails").getString("author");
        if (isNullOrEmpty(uploaderName)) {
            return "Unknown";
        }

        return uploaderName;
    }

    @Override
    public boolean isUploaderVerified() throws ParsingException {
        JsonObject videoOwnerRenderer = getVideoSecondaryInfoRenderer()
                .getObject("owner")
                .getObject("videoOwnerRenderer");
        if (videoOwnerRenderer.has("badges")) {
            return YoutubeParsingHelper.isVerified(videoOwnerRenderer.getArray("badges"));
        }
        JsonObject navigationEndpoint = videoOwnerRenderer.getObject("navigationEndpoint");
        if (navigationEndpoint != null && navigationEndpoint.has("showDialogCommand")) {
            try {
                JsonArray listItems = navigationEndpoint
                        .getObject("showDialogCommand")
                        .getObject("panelLoadingStrategy")
                        .getObject("inlineContent")
                        .getObject("dialogViewModel")
                        .getObject("customContent")
                        .getObject("listViewModel")
                        .getArray("listItems");
                if (!listItems.isEmpty()) {
                    JsonObject firstItem = listItems.getObject(0)
                            .getObject("listItemViewModel")
                            .getObject("title");
                    if (firstItem.has("attachmentRuns")) {
                        return true;
                    }
                }
            } catch (Exception ignored) {}
        }
        return false;
    }

    @Nonnull
    @Override
    public String getUploaderAvatarUrl() throws ParsingException {
        assertPageFetched();

        JsonObject videoOwnerRenderer = getVideoSecondaryInfoRenderer()
                .getObject("owner")
                .getObject("videoOwnerRenderer");

        if (videoOwnerRenderer.has("thumbnail")) {
            JsonArray thumbnails = videoOwnerRenderer
                    .getObject("thumbnail")
                    .getArray("thumbnails");
            final String url = thumbnails
                    .getObject(thumbnails.size() - 1)
                    .getString("url");

            if (isNullOrEmpty(url)) {
                if (ageLimit == NO_AGE_LIMIT) {
                    throw new ParsingException("Could not get uploader avatar URL");
                }
                return EMPTY_STRING;
            }
            return fixThumbnailUrl(url);
        }

        JsonObject navigationEndpoint = videoOwnerRenderer.getObject("navigationEndpoint");
        if (navigationEndpoint != null && navigationEndpoint.has("showDialogCommand")) {
            try {
                JsonArray listItems = navigationEndpoint
                        .getObject("showDialogCommand")
                        .getObject("panelLoadingStrategy")
                        .getObject("inlineContent")
                        .getObject("dialogViewModel")
                        .getObject("customContent")
                        .getObject("listViewModel")
                        .getArray("listItems");
                if (!listItems.isEmpty()) {
                    JsonArray sources = listItems.getObject(0)
                            .getObject("listItemViewModel")
                            .getObject("leadingAccessory")
                            .getObject("avatarViewModel")
                            .getObject("image")
                            .getArray("sources");
                    if (!sources.isEmpty()) {
                        String url = sources.getObject(0).getString("url");
                        if (!isNullOrEmpty(url)) {
                            return fixThumbnailUrl(url);
                        }
                    }
                }
            } catch (Exception ignored) {}
        }

        if (ageLimit == NO_AGE_LIMIT) {
            throw new ParsingException("Could not get uploader avatar URL");
        }
        return EMPTY_STRING;
    }

    @Override
    public long getUploaderSubscriberCount() throws ParsingException {
        final JsonObject videoOwnerRenderer = JsonUtils.getObject(videoSecondaryInfoRenderer,
                "owner.videoOwnerRenderer");
        if (videoOwnerRenderer.has("subscriberCountText")) {
            try {
                return Utils.mixedNumberWordToLong(getTextFromObject(videoOwnerRenderer
                        .getObject("subscriberCountText")));
            } catch (final NumberFormatException e) {
                throw new ParsingException("Could not get uploader subscriber count", e);
            }
        }
        JsonObject navigationEndpoint = videoOwnerRenderer.getObject("navigationEndpoint");
        if (navigationEndpoint != null && navigationEndpoint.has("showDialogCommand")) {
            try {
                JsonArray listItems = navigationEndpoint
                        .getObject("showDialogCommand")
                        .getObject("panelLoadingStrategy")
                        .getObject("inlineContent")
                        .getObject("dialogViewModel")
                        .getObject("customContent")
                        .getObject("listViewModel")
                        .getArray("listItems");
                if (!listItems.isEmpty()) {
                    String subtitle = listItems.getObject(0)
                            .getObject("listItemViewModel")
                            .getObject("subtitle")
                            .getString("content", "");
                    int idx = subtitle.indexOf("•");
                    if (idx >= 0) {
                        String subCount = subtitle.substring(idx + 1).trim();
                        return Utils.mixedNumberWordToLong(subCount);
                    }
                }
            } catch (Exception ignored) {}
        }
        return UNKNOWN_SUBSCRIBER_COUNT;
    }

    @Nonnull
    @Override
    public List<StaffInfoItem> getStaffs() {
        final JsonObject videoOwnerRenderer = getVideoSecondaryInfoRenderer()
                .getObject("owner")
                .getObject("videoOwnerRenderer");
        JsonObject navigationEndpoint = videoOwnerRenderer.getObject("navigationEndpoint");
        if (navigationEndpoint == null || !navigationEndpoint.has("showDialogCommand")) {
            return Collections.emptyList();
        }
        try {
            JsonArray listItems = navigationEndpoint
                    .getObject("showDialogCommand")
                    .getObject("panelLoadingStrategy")
                    .getObject("inlineContent")
                    .getObject("dialogViewModel")
                    .getObject("customContent")
                    .getObject("listViewModel")
                    .getArray("listItems");
            List<StaffInfoItem> result = new ArrayList<>();
            for (Object item : listItems) {
                JsonObject listItemViewModel = ((JsonObject) item).getObject("listItemViewModel");
                String name = listItemViewModel.getObject("title").getString("content", "");
                String avatarUrl = "";
                JsonArray sources = listItemViewModel
                        .getObject("leadingAccessory")
                        .getObject("avatarViewModel")
                        .getObject("image")
                        .getArray("sources");
                if (!sources.isEmpty()) {
                    avatarUrl = sources.getObject(0).getString("url", "");
                }
                String channelUrl = "";
                JsonArray commandRuns = listItemViewModel.getObject("title").getArray("commandRuns");
                if (commandRuns != null && !commandRuns.isEmpty()) {
                    JsonObject browseEndpoint = commandRuns.getObject(0)
                            .getObject("onTap")
                            .getObject("innertubeCommand")
                            .getObject("browseEndpoint");
                    String browseId = browseEndpoint.getString("browseId", "");
                    if (!browseId.isEmpty()) {
                        channelUrl = YoutubeChannelLinkHandlerFactory.getInstance()
                                .getUrl("channel/" + browseId);
                    }
                }
                result.add(new StaffInfoItem(getServiceId(), channelUrl, name, null, avatarUrl));
            }
            return result;
        } catch (Exception e) {
            return Collections.emptyList();
        }
    }

    @Nonnull
    @Override
    public String getDashMpdUrl() throws ParsingException {
        assertPageFetched();
        String dashUrl = getManifestUrl(
                "dash",
                Arrays.asList(safariStreamingData,
                        androidStreamingData,
                        tvHtml5SimplyEmbedStreamingData,
                        configuredStreamingData,
                        webStreamingData,
                        mwebStreamingData));
        if (!dashUrl.isEmpty()) {
            dashUrl = appendQueryParameterIfMissing(dashUrl, "mpd_version", "7");
            dashUrl = deobfuscateManifestUrl(dashUrl);
        }
        return dashUrl;
    }

    @Nonnull
    @Override
    public String getHlsUrl() throws ParsingException {
        assertPageFetched();

        String hlsUrl = getManifestUrl(
                "hls",
                Arrays.asList(safariStreamingData,
                        androidStreamingData,
                        tvHtml5SimplyEmbedStreamingData,
                        configuredStreamingData,
                        webStreamingData,
                        mwebStreamingData));
        if (hlsUrl.isEmpty() && streamType == StreamType.LIVE_STREAM
                && !mwebHlsManifestUrl.isEmpty()) {
            hlsUrl = mwebHlsManifestUrl;
        }

        if (!hlsUrl.isEmpty()) {
            hlsUrl = deobfuscateManifestUrl(hlsUrl);
        }

        return hlsUrl;
    }

    @Nonnull
    private String deobfuscateManifestUrl(@Nonnull final String manifestUrl) throws ParsingException {
        final java.util.regex.Matcher matcher = java.util.regex.Pattern.compile("/n/([^/]+)").matcher(manifestUrl);
        if (matcher.find()) {
            final String encryptedN = matcher.group(1);
            try {
                final String videoId = getId();
                final YoutubeApiDecoder.BatchDecodeResult result =
                        YoutubeJavaScriptPlayerManager.deobfuscateBatch(
                                videoId,
                                null,
                                Collections.singletonList(encryptedN));
                final String decryptedN = result.getNParameters().get(encryptedN);
                if (decryptedN != null) {
                    return manifestUrl.replace("/n/" + encryptedN, "/n/" + decryptedN);
                }
            } catch (final Exception e) {
                throw new ParsingException("Could not deobfuscate manifest URL n parameter", e);
            }
        }
        return manifestUrl;
    }

    @Nonnull
    private static String getManifestUrl(@Nonnull final String manifestType,
                                         @Nonnull final List<JsonObject> streamingDataObjects) {
        final String manifestKey = manifestType + "ManifestUrl";

        return streamingDataObjects.stream()
                .filter(Objects::nonNull)
                .map(streamingDataObject -> streamingDataObject.getString(manifestKey))
                .filter(Objects::nonNull)
                .filter(manifestUrl -> !manifestUrl.isEmpty())
                .findFirst()
                .orElse(EMPTY_STRING);
    }

    // Cache for batch-processed streams
    private List<AudioStream> cachedAudioStreams;
    private List<VideoStream> cachedVideoStreams;
    private List<VideoStream> cachedVideoOnlyStreams;
    private boolean streamsCached = false;

    /**
     * Pre-fetch and batch-process all streams in a single API call.
     * This method collects all audio, video, and video-only streams,
     * then performs batch deobfuscation in one request.
     */
    private void ensureStreamsAreCached() throws ExtractionException {
        if (streamsCached) {
            return;
        }

        assertPageFetched();
        final String videoId = getId();

        try {
            cachedAudioStreams = new ArrayList<>();
            cachedVideoStreams = new ArrayList<>();
            cachedVideoOnlyStreams = new ArrayList<>();

            extractDirectFormats(videoId);

            if (cachedAudioStreams.isEmpty()
                    && cachedVideoStreams.isEmpty()
                    && cachedVideoOnlyStreams.isEmpty()
                    && hasSabrStreamingUrl()) {
                buildSabrStreams(videoId);
            }

            if (cachedAudioStreams.isEmpty() && cachedVideoStreams.isEmpty()) {
                tryExtractHlsStreams(videoId);
            }

            if (cachedAudioStreams.isEmpty()
                    && cachedVideoStreams.isEmpty()
                    && cachedVideoOnlyStreams.isEmpty()) {
                throw new ContentNotSupportedException(
                        "YouTube returned no playable direct, SABR, or HLS streams");
            }

            Collections.sort(cachedAudioStreams,
                    Comparator.comparingInt(AudioStream::getBitrate).reversed());
            streamsCached = true;
        } catch (final ExtractionException e) {
            throw e;
        } catch (final Exception e) {
            throw new ParsingException("Could not get streams", e);
        }
    }

    /**
     * Build session-based SABR streams from a SABR-only response.
     *
     * <p>SABR adaptiveFormats carry no per-format URL: each stream is marked with
     * {@link DeliveryMethod#SABR}, its {@code content} is the serverAbrStreamingUrl (for reference),
     * and {@code isUrl} is false. The client drives a {@code YoutubeSabrSession} from the videoId and
     * the selected itag to fetch media.</p>
     */
    private void buildSabrStreams(@Nonnull final String videoId) {
        final YoutubeSabrInfo sabrInfo = buildSabrInfo(videoId);
        final JsonObject streamingData = getSabrStreamingData();
        if (streamingData == null) {
            return;
        }
        final String serverAbrStreamingUrl =
                streamingData.getString("serverAbrStreamingUrl", EMPTY_STRING);
        final JsonArray adaptiveFormats = streamingData.getArray(ADAPTIVE_FORMATS);
        if (adaptiveFormats == null) {
            return;
        }

        for (int i = 0; i < adaptiveFormats.size(); i++) {
            final JsonObject formatData = adaptiveFormats.getObject(i);
            try {
                final ItagItem itagItem = ItagItem.getItag(formatData.getInt("itag"));
                fillSabrItagItem(itagItem, formatData);
                final String id = String.valueOf(itagItem.id);

                if (itagItem.itagType == ItagItem.ItagType.AUDIO) {
                    final AudioStream.Builder builder = new AudioStream.Builder()
                            .setAvailableAt(getStreamAvailableAt())
                            .setContent(serverAbrStreamingUrl, false)
                            .setMediaFormat(itagItem.getMediaFormat())
                            .setAverageBitrate(itagItem.getAverageBitrate())
                            .setItagItem(itagItem)
                            .setDeliveryMethod(DeliveryMethod.SABR);
                    builder.setDeliveryMethodInfo(sabrInfo);
                    // Multi-track audio: the same itag is served once per language. Carry the track
                    // info so the player can show a language selector, and key the id on (itag,
                    // track) so the languages aren't collapsed into one by the dedup below.
                    String streamId = id;
                    if (formatData.has("audioTrack")) {
                        final JsonObject audioTrack = formatData.getObject("audioTrack");
                        if (audioTrack.has("id")) {
                            final String trackId = audioTrack.getString("id");
                            final String displayName = audioTrack.getString("displayName");
                            final String langPart = trackId.split("\\.")[0];
                            final boolean isOriginal = displayName != null
                                    && (displayName.contains("original")
                                        || displayName.contains("yokuqala"));
                            builder.setAudioTrackId(trackId)
                                    .setAudioTrackName(displayName != null ? displayName
                                            : (isOriginal ? langPart + " (original)" : langPart))
                                    .setAudioLocale(langPart.split("-")[0]);
                            streamId = id + "-" + trackId;
                        }
                    }
                    final String audioStreamId = streamId;
                    final AudioStream stream = builder.setId(audioStreamId).build();
                    // Dedup by id (itag, or itag+track when multi-track), not Stream.equalStats: all
                    // SABR formats share the same MediaFormat/delivery, so equalStats would collapse
                    // every bitrate/codec to one.
                    if (cachedAudioStreams.stream().noneMatch(s -> audioStreamId.equals(s.getId()))) {
                        cachedAudioStreams.add(stream);
                    }
                } else if (itagItem.itagType == ItagItem.ItagType.VIDEO_ONLY) {
                    final String resolution = itagItem.getResolutionString();
                    final VideoStream stream = new VideoStream.Builder()
                            .setAvailableAt(getStreamAvailableAt())
                            .setId(id)
                            .setContent(serverAbrStreamingUrl, false)
                            .setMediaFormat(itagItem.getMediaFormat())
                            .setIsVideoOnly(true)
                            .setItagItem(itagItem)
                            .setResolution(resolution != null ? resolution : EMPTY_STRING)
                            .setDeliveryMethod(DeliveryMethod.SABR)
                            .setDeliveryMethodInfo(sabrInfo)
                            .build();
                    if (cachedVideoOnlyStreams.stream().noneMatch(s -> id.equals(s.getId()))) {
                        cachedVideoOnlyStreams.add(stream);
                    }
                }
            } catch (final Exception e) {
                // Skip unknown itags or malformed formats; do not fail the whole extraction.
            }
        }

        Collections.sort(cachedAudioStreams,
                Comparator.comparingInt(AudioStream::getBitrate).reversed());
    }

    private void extractDirectFormats(@Nonnull final String videoId) throws ParsingException {
        final List<ItagInfo> audioItags = new ArrayList<>();
        final List<ItagInfo> videoItags = new ArrayList<>();
        final List<ItagInfo> videoOnlyItags = new ArrayList<>();
        final Set<JsonObject> processedStreamingData =
                Collections.newSetFromMap(new IdentityHashMap<>());

        for (final StreamingDataSource source : getDirectStreamingDataSources()) {
            if (source.streamingData == null
                    || !processedStreamingData.add(source.streamingData)) {
                continue;
            }
            collectDirectItags(source, ADAPTIVE_FORMATS, ItagItem.ItagType.AUDIO,
                    audioItags);
            collectDirectItags(source, FORMATS, ItagItem.ItagType.VIDEO,
                    videoItags);
            collectDirectItags(source, ADAPTIVE_FORMATS, ItagItem.ItagType.VIDEO_ONLY,
                    videoOnlyItags);
        }

        final List<ItagInfo> allItags = new ArrayList<>(
                audioItags.size() + videoItags.size() + videoOnlyItags.size());
        allItags.addAll(audioItags);
        allItags.addAll(videoItags);
        allItags.addAll(videoOnlyItags);
        deobfuscateDirectUrls(videoId, allItags);

        for (final ItagInfo info : audioItags) {
            final ItagItem item = info.getItagItem();
            final String trackId = info.getAudioTrackId();
            final String id = String.valueOf(item.id)
                    + (isNullOrEmpty(trackId) ? EMPTY_STRING : "-" + trackId);
            final AudioStream.Builder builder = new AudioStream.Builder()
                    .setAvailableAt(getStreamAvailableAt())
                    .setId(id)
                    .setContent(info.getContent(), info.getIsUrl())
                    .setMediaFormat(item.getMediaFormat())
                    .setAverageBitrate(item.getAverageBitrate())
                    .setItagItem(item)
                    .setAudioTrackId(info.getAudioTrackId())
                    .setAudioTrackName(info.getAudioTrackName())
                    .setAudioLocale(info.getAudioLocale());
            if (!info.getIsUrl()
                    || streamType == StreamType.LIVE_STREAM
                    || streamType == StreamType.POST_LIVE_STREAM) {
                builder.setDeliveryMethod(DeliveryMethod.DASH);
            }
            final AudioStream stream = builder.build();
            if (!Stream.containSimilarStream(stream, cachedAudioStreams)) {
                cachedAudioStreams.add(stream);
            }
        }

        for (final ItagInfo info : videoItags) {
            final ItagItem item = info.getItagItem();
            final VideoStream.Builder builder = new VideoStream.Builder()
                    .setAvailableAt(getStreamAvailableAt())
                    .setId(String.valueOf(item.id))
                    .setContent(info.getContent(), info.getIsUrl())
                    .setMediaFormat(item.getMediaFormat())
                    .setIsVideoOnly(false)
                    .setItagItem(item)
                    .setResolution(item.getResolutionString() == null
                            ? EMPTY_STRING : item.getResolutionString());
            if (!info.getIsUrl()
                    || streamType == StreamType.LIVE_STREAM
                    || streamType == StreamType.POST_LIVE_STREAM) {
                builder.setDeliveryMethod(DeliveryMethod.DASH);
            }
            final VideoStream stream = builder.build();
            if (!Stream.containSimilarStream(stream, cachedVideoStreams)) {
                cachedVideoStreams.add(stream);
            }
        }

        for (final ItagInfo info : videoOnlyItags) {
            final ItagItem item = info.getItagItem();
            final VideoStream.Builder builder = new VideoStream.Builder()
                    .setAvailableAt(getStreamAvailableAt())
                    .setId(String.valueOf(item.id))
                    .setContent(info.getContent(), info.getIsUrl())
                    .setMediaFormat(item.getMediaFormat())
                    .setIsVideoOnly(true)
                    .setItagItem(item)
                    .setResolution(item.getResolutionString() == null
                            ? EMPTY_STRING : item.getResolutionString());
            if (!info.getIsUrl()
                    || streamType == StreamType.LIVE_STREAM
                    || streamType == StreamType.POST_LIVE_STREAM) {
                builder.setDeliveryMethod(DeliveryMethod.DASH);
            }
            final VideoStream stream = builder.build();
            if (!Stream.containSimilarStream(stream, cachedVideoOnlyStreams)) {
                cachedVideoOnlyStreams.add(stream);
            }
        }
    }

    private void collectDirectItags(@Nonnull final StreamingDataSource source,
                                    @Nonnull final String formatsKey,
                                    @Nonnull final ItagItem.ItagType wantedType,
                                    @Nonnull final List<ItagInfo> destination) {
        final JsonArray formats = source.streamingData.getArray(formatsKey);
        if (formats == null || formats.isEmpty()) {
            return;
        }

        for (int i = 0; i < formats.size(); i++) {
            final JsonObject format = formats.getObject(i);
            try {
                final ItagItem item = ItagItem.getItag(format.getInt("itag"));
                if (item.itagType != wantedType) {
                    continue;
                }
                final ItagInfo info = createDirectItag(format, item, source.cpn);
                if (info == null) {
                    continue;
                }
                if (wantedType == ItagItem.ItagType.AUDIO && format.has("audioTrack")) {
                    final JsonObject track = format.getObject("audioTrack");
                    final String trackId = track.getString("id", EMPTY_STRING);
                    final String language = trackId.isEmpty()
                            ? EMPTY_STRING : trackId.split("\\.")[0];
                    final String locale = language.isEmpty()
                            ? EMPTY_STRING : language.split("-")[0];
                    info.setAudioTrackInfo(
                            trackId,
                            track.getString("displayName", language),
                            locale);
                }
                destination.add(info);
            } catch (final Exception error) {
                addError(new ParsingException(
                        "Could not parse " + source.name + " itag", error));
            }
        }
    }

    @Nonnull
    private List<StreamingDataSource> getDirectStreamingDataSources() {
        return Arrays.asList(
                new StreamingDataSource("ANDROID_VR", androidStreamingData, androidCpn),
                new StreamingDataSource("Safari", safariStreamingData, safariCpn),
                new StreamingDataSource("TVHTML5", tvHtml5SimplyEmbedStreamingData,
                        tvHtml5SimplyEmbedCpn),
                new StreamingDataSource("configured", configuredStreamingData, configuredCpn),
                new StreamingDataSource("WEB", webStreamingData, webCpn),
                new StreamingDataSource("MWEB", mwebStreamingData, mwebCpn));
    }

    @Nullable
    private ItagInfo createDirectItag(@Nonnull final JsonObject format,
                                      @Nonnull final ItagItem item,
                                      @Nullable final String cpn) throws IOException {
        String url = format.getString("url", EMPTY_STRING);
        String signature = null;
        String signatureParameter = "signature";

        if (url.isEmpty()) {
            final String cipherValue = format.getString("signatureCipher",
                    format.getString("cipher", EMPTY_STRING));
            if (cipherValue.isEmpty()) {
                return null;
            }
            final Map<String, String> cipher = Parser.compatParseMap(cipherValue);
            url = cipher.get("url");
            signature = cipher.get("s");
            if (!isNullOrEmpty(cipher.get("sp"))) {
                signatureParameter = cipher.get("sp");
            }
            if (isNullOrEmpty(url)) {
                return null;
            }
            if (!isNullOrEmpty(signature)) {
                url = appendQueryParameter(url, signatureParameter, "SIGNATURE_PLACEHOLDER");
            }
        }

        if (!isNullOrEmpty(cpn) && !containsQueryParameter(url, CPN)) {
            url = appendQueryParameter(url, CPN, cpn);
        }

        fillSabrItagItem(item, format);
        final ItagInfo info = new ItagInfo(url, item);
        info.setIsUrl(!"FORMAT_STREAM_TYPE_OTF".equalsIgnoreCase(
                format.getString("type", EMPTY_STRING)));
        if (!isNullOrEmpty(signature)) {
            info.setObfuscatedSignature(signature);
        }
        return info;
    }

    @Nonnull
    private static String appendQueryParameter(@Nonnull final String url,
                                               @Nonnull final String key,
                                               @Nonnull final String value) {
        final String separator = url.contains("?") ? "&" : "?";
        return url + separator + key + "=" + value;
    }

    @Nonnull
    private static String appendQueryParameterIfMissing(@Nonnull final String url,
                                                        @Nonnull final String key,
                                                        @Nonnull final String value) {
        if (containsQueryParameter(url, key)) {
            return url;
        }
        return appendQueryParameter(url, key, value);
    }

    private static boolean containsQueryParameter(@Nonnull final String url,
                                                  @Nonnull final String key) {
        return url.matches(".*[?&]" + java.util.regex.Pattern.quote(key) + "=[^&]*.*");
    }

    private static final class StreamingDataSource {
        private final String name;
        @Nullable
        private final JsonObject streamingData;
        @Nullable
        private final String cpn;

        private StreamingDataSource(@Nonnull final String name,
                                    @Nullable final JsonObject streamingData,
                                    @Nullable final String cpn) {
            this.name = name;
            this.streamingData = streamingData;
            this.cpn = cpn;
        }
    }

    private void deobfuscateDirectUrls(@Nonnull final String videoId,
                                       @Nonnull final List<ItagInfo> itags)
            throws ParsingException {
        final LinkedHashSet<String> signatures = new LinkedHashSet<>();
        final LinkedHashSet<String> throttling = new LinkedHashSet<>();
        for (final ItagInfo info : itags) {
            if (!isNullOrEmpty(info.getObfuscatedSignature())) {
                signatures.add(info.getObfuscatedSignature());
            }
            final String n = YoutubeJavaScriptPlayerManager
                    .getThrottlingParameterFromStreamingUrl(info.getContent());
            if (!isNullOrEmpty(n)) {
                throttling.add(n);
            }
        }
        if (signatures.isEmpty() && throttling.isEmpty()) {
            return;
        }
        final YoutubeApiDecoder.BatchDecodeResult result =
                YoutubeJavaScriptPlayerManager.deobfuscateBatch(videoId,
                        new ArrayList<>(signatures), new ArrayList<>(throttling));
        for (final ItagInfo info : itags) {
            String url = info.getContent();
            final String signature = info.getObfuscatedSignature();
            if (!isNullOrEmpty(signature) && result.getSignatures().get(signature) != null) {
                url = url.replace("SIGNATURE_PLACEHOLDER",
                        result.getSignatures().get(signature));
            }
            final String n = YoutubeJavaScriptPlayerManager
                    .getThrottlingParameterFromStreamingUrl(url);
            if (!isNullOrEmpty(n) && result.getNParameters().get(n) != null) {
                url = url.replace(n, result.getNParameters().get(n));
            }
            info.setContent(url);
        }
    }

    @Nullable
    private JsonObject getSabrStreamingData() {
        if (configuredStreamingData == null
                || configuredStreamingData.getArray(ADAPTIVE_FORMATS) == null
                || configuredStreamingData.getArray(ADAPTIVE_FORMATS).isEmpty()) {
            return null;
        }
        return configuredStreamingData;
    }

    @Nullable
    private YoutubeSabrInfo buildSabrInfo(@Nonnull final String videoId) {
        if (playerResponse == null) {
            return null;
        }
        try {
            return YoutubeSabrProbe.fromPlayerResponse(videoId, getSabrClientProfile(),
                    getSabrCpn(), playerResponse);
        } catch (final Exception e) {
            addError(e);
            return null;
        }
    }

    @Nonnull
    private YoutubeSabrClientProfile getSabrClientProfile() {
        if (configuredStreamingData == androidStreamingData) {
            return YoutubeSabrClientProfile.ANDROID_VR;
        }
        if (configuredStreamingData == tvHtml5SimplyEmbedStreamingData) {
            return YoutubeSabrClientProfile.TVHTML5;
        }
        if (configuredStreamingData == safariStreamingData
                || configuredStreamingData == webStreamingData) {
            return YoutubeSabrClientProfile.WEB;
        }
        return YoutubeSabrClientProfile.MWEB;
    }

    @Nonnull
    private String getSabrCpn() {
        return isNullOrEmpty(configuredCpn)
                ? generateContentPlaybackNonce() : configuredCpn;
    }

    private static void fillSabrItagItem(@Nonnull final ItagItem itagItem,
                                         @Nonnull final JsonObject formatData) {
        final String mimeType = formatData.getString("mimeType", EMPTY_STRING);
        final String codec = mimeType.contains("codecs") ? mimeType.split("\"")[1] : EMPTY_STRING;

        itagItem.setBitrate(formatData.getInt("bitrate"));
        itagItem.setWidth(formatData.getInt("width"));
        itagItem.setHeight(formatData.getInt("height"));
        if (formatData.has("initRange")) {
            final JsonObject initRange = formatData.getObject("initRange");
            itagItem.setInitStart(Integer.parseInt(initRange.getString("start", "-1")));
            itagItem.setInitEnd(Integer.parseInt(initRange.getString("end", "-1")));
        }
        if (formatData.has("indexRange")) {
            final JsonObject indexRange = formatData.getObject("indexRange");
            itagItem.setIndexStart(Integer.parseInt(indexRange.getString("start", "-1")));
            itagItem.setIndexEnd(Integer.parseInt(indexRange.getString("end", "-1")));
        }
        itagItem.setQuality(formatData.getString("quality"));
        itagItem.setCodec(codec);
        final int fps = formatData.getInt("fps", -1);
        if (fps != -1) {
            itagItem.setFps(fps);
        }
        if (itagItem.itagType == ItagItem.ItagType.AUDIO) {
            if (formatData.has("audioSampleRate")) {
                itagItem.setSampleRate(Integer.parseInt(formatData.getString("audioSampleRate")));
            }
            itagItem.setAudioChannels(formatData.getInt("audioChannels", 2));
        }
        itagItem.setContentLength(Long.parseLong(formatData.getString("contentLength",
                String.valueOf(CONTENT_LENGTH_UNKNOWN))));
        itagItem.setApproxDurationMs(Long.parseLong(formatData.getString("approxDurationMs",
                String.valueOf(APPROX_DURATION_MS_UNKNOWN))));
    }

    private void tryExtractHlsStreams(final String videoId) throws ExtractionException {
        final String hlsManifestUrl = getHlsManifestUrlFromStreamingData();
        if (hlsManifestUrl.isEmpty()) {
            return;
        }

        try {
            final String deobfuscatedUrl = deobfuscateManifestUrl(hlsManifestUrl);
            final Response response = NewPipe.getDownloader().get(deobfuscatedUrl);
            final String manifestContent = response.responseBody();
            parseHlsMasterManifest(manifestContent, videoId);
        } catch (final Exception e) {
            throw new ParsingException("Could not extract HLS streams", e);
        }
    }

    @Nonnull
    private String getHlsManifestUrlFromStreamingData() {
        for (final JsonObject sd : Arrays.asList(
                safariStreamingData,
                androidStreamingData,
                tvHtml5SimplyEmbedStreamingData,
                configuredStreamingData,
                webStreamingData,
                mwebStreamingData)) {
            if (sd != null) {
                final String url = sd.getString("hlsManifestUrl");
                if (url != null && !url.isEmpty()) {
                    return url;
                }
            }
        }
        return streamType == StreamType.LIVE_STREAM ? mwebHlsManifestUrl : EMPTY_STRING;
    }

    private void parseHlsMasterManifest(@Nonnull final String manifestContent,
                                         final String videoId) throws ParsingException {
        final String[] lines = manifestContent.split("\n");
        final String preferredAudioLanguage = ServiceList.YouTube.getAudioLanguage();

        final Map<String, String> audioTrackUrls = new LinkedHashMap<>();
        final Map<String, String> audioTrackNames = new LinkedHashMap<>();
        final Map<String, String> audioTrackLocales = new LinkedHashMap<>();
        final Map<String, String> audioTrackAconts = new LinkedHashMap<>();

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (!line.startsWith("#EXT-X-STREAM-INF:")) {
                continue;
            }
            if (i + 1 >= lines.length) {
                break;
            }
            final String streamUrl = lines[i + 1].trim();
            if (streamUrl.isEmpty() || streamUrl.startsWith("#")) {
                continue;
            }

            final String resolution = extractHlsStringAttribute(line, "RESOLUTION");
            final String codecs = extractHlsStringAttribute(line, "CODECS");
            final int frameRate = extractHlsAttribute(line, "FRAME-RATE");
            final String audioContentId = extractHlsStringAttribute(line, "YT-EXT-AUDIO-CONTENT-ID");

            String videoCodec = null;
            if (codecs != null) {
                for (final String c : codecs.split(",")) {
                    final String trimmed = c.trim();
                    if (!trimmed.startsWith("mp4a") && !trimmed.startsWith("opus")
                            && !trimmed.startsWith("ac-3") && !trimmed.startsWith("ec-3")) {
                        videoCodec = trimmed;
                        break;
                    }
                }
            }

            String acont = extractHlsXtagsValue(streamUrl, "acont");

            if (resolution != null && !resolution.isEmpty()) {
                final String[] resParts = resolution.split("x");
                final String height = resParts.length == 2 ? resParts[1] + "p" : resolution;
                final String resString = frameRate > 30 ? height + frameRate : height;

                String audioTrackId = null;
                String audioLocale = null;
                String audioTrackName = null;
                if (audioContentId != null && !audioContentId.isEmpty()) {
                    audioTrackId = audioContentId;
                    final String langPart = audioContentId.split("\\.")[0];
                    audioLocale = langPart.split("-")[0];
                    if ("original".equals(acont)) {
                        audioTrackName = langPart + " (original)";
                    } else {
                        audioTrackName = langPart;
                    }

                    if (!audioTrackUrls.containsKey(audioTrackId)
                            || "original".equals(acont)) {
                        audioTrackUrls.put(audioTrackId, streamUrl);
                    }
                    audioTrackNames.put(audioTrackId, audioTrackName);
                    audioTrackLocales.put(audioTrackId, audioLocale);
                    if (acont != null) {
                        audioTrackAconts.put(audioTrackId, acont);
                    }
                }

                final String streamId = "hls-" + videoId + "-" + resString
                        + (audioTrackId != null ? "-" + audioTrackId : "");

                final VideoStream.Builder builder = new VideoStream.Builder()
                        .setAvailableAt(getStreamAvailableAt())
                        .setId(streamId)
                        .setContent(streamUrl, true)
                        .setIsVideoOnly(false)
                        .setResolution(resString)
                        .setDeliveryMethod(DeliveryMethod.HLS);

                if (videoCodec != null) {
                    builder.setCodec(videoCodec);
                }
                if (codecs != null) {
                    if (codecs.contains("avc") || codecs.contains("mp4a")) {
                        builder.setMediaFormat(MediaFormat.MPEG_4);
                    } else if (codecs.contains("vp9") || codecs.contains("vp09")) {
                        builder.setMediaFormat(MediaFormat.WEBM);
                    }
                }

                if (audioTrackId != null) {
                    builder.setAudioTrackId(audioTrackId);
                    builder.setAudioTrackName(audioTrackName);
                    builder.setAudioLocale(audioLocale);
                }

                cachedVideoStreams.add(builder.build());
            }
        }

        for (int i = 0; i < lines.length; i++) {
            final String line = lines[i].trim();
            if (!line.startsWith("#EXT-X-MEDIA:") || !line.contains("TYPE=AUDIO")) {
                continue;
            }
            final String uri = extractHlsStringAttribute(line, "URI");
            if (uri == null || uri.isEmpty()) {
                continue;
            }

            final String groupId = extractHlsStringAttribute(line, "GROUP-ID");
            final String name = extractHlsStringAttribute(line, "NAME");
            final String language = extractHlsStringAttribute(line, "LANGUAGE");

            final AudioStream.Builder audioBuilder = new AudioStream.Builder()
                    .setAvailableAt(getStreamAvailableAt())
                    .setId("hls-" + videoId + "-audio-" + (groupId != null ? groupId : "default")
                            + (language != null ? "-" + language : ""))
                    .setContent(uri, true)
                    .setMediaFormat(MediaFormat.M4A)
                    .setDeliveryMethod(DeliveryMethod.HLS);
            if (name != null) {
                audioBuilder.setAudioTrackName(name);
            }
            if (language != null) {
                audioBuilder.setAudioTrackId(language);
                audioBuilder.setAudioLocale(language.split("-")[0]);
            }
            cachedAudioStreams.add(audioBuilder.build());
        }

        if (cachedAudioStreams.isEmpty() && !audioTrackUrls.isEmpty()) {
            for (final Map.Entry<String, String> entry : audioTrackUrls.entrySet()) {
                final String trackId = entry.getKey();
                final String url = entry.getValue();
                final String trackName = audioTrackNames.get(trackId);
                final String locale = audioTrackLocales.get(trackId);

                final AudioStream.Builder audioBuilder = new AudioStream.Builder()
                        .setAvailableAt(getStreamAvailableAt())
                        .setId("hls-" + videoId + "-audio-" + trackId)
                        .setContent(url, true)
                        .setMediaFormat(MediaFormat.M4A)
                        .setDeliveryMethod(DeliveryMethod.HLS)
                        .setAudioTrackId(trackId)
                        .setAudioLocale(locale);
                if (trackName != null) {
                    audioBuilder.setAudioTrackName(trackName);
                }
                cachedAudioStreams.add(audioBuilder.build());
            }
        }
    }

    @Nullable
    private static String extractHlsXtagsValue(@Nonnull final String url,
                                                 @Nonnull final String key) {
        try {
            final String decoded = java.net.URLDecoder.decode(url, "UTF-8");
            final java.util.regex.Matcher m = java.util.regex.Pattern
                    .compile("xtags=([^/]+)").matcher(decoded);
            if (m.find()) {
                final String xtags = m.group(1);
                for (final String part : xtags.split(":")) {
                    final String[] kv = part.split("=", 2);
                    if (kv.length == 2 && kv[0].equals(key)) {
                        return kv[1];
                    }
                }
            }
        } catch (final Exception ignored) {
        }
        return null;
    }

    private static int extractHlsAttribute(@Nonnull final String line,
                                            @Nonnull final String attribute) {
        final java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile(attribute + "=(\\d+)").matcher(line);
        if (matcher.find()) {
            try {
                return Integer.parseInt(matcher.group(1));
            } catch (final NumberFormatException ignored) {
            }
        }
        return -1;
    }

    @Nullable
    private static String extractHlsStringAttribute(@Nonnull final String line,
                                                     @Nonnull final String attribute) {
        java.util.regex.Matcher matcher = java.util.regex.Pattern
                .compile(attribute + "=\"([^\"]+)\"").matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        matcher = java.util.regex.Pattern
                .compile(attribute + "=([^,\\s]+)").matcher(line);
        if (matcher.find()) {
            return matcher.group(1);
        }
        return null;
    }

    @Override
    public List<AudioStream> getAudioStreams() throws ExtractionException {
        ensureStreamsAreCached();
        return cachedAudioStreams;
    }

    @Override
    public List<VideoStream> getVideoStreams() throws ExtractionException {
        ensureStreamsAreCached();
        return cachedVideoStreams;
    }

    @Override
    public List<VideoStream> getVideoOnlyStreams() throws ExtractionException {
        ensureStreamsAreCached();
        return cachedVideoOnlyStreams;
    }

    @Override
    @Nonnull
    public List<SubtitlesStream> getSubtitlesDefault() throws ParsingException {
        return getSubtitles(MediaFormat.TTML);
    }

    @Override
    @Nonnull
    public List<SubtitlesStream> getSubtitles(final MediaFormat format) throws ParsingException {
        assertPageFetched();

        if(playerCaptionsTracklistRenderer == null) {
            return new ArrayList<>();
        }

        // We cannot store the subtitles list because the media format may change
        final List<SubtitlesStream> subtitlesToReturn = new ArrayList<>();
        final JsonArray captionsArray = playerCaptionsTracklistRenderer.getArray("captionTracks");
        // TODO: use this to apply auto translation to different language from a source language
        // final JsonArray autoCaptionsArray = renderer.getArray("translationLanguages");

        String autoTranslatedSubtitlesLanguage =
                ServiceList.YouTube.getAutoTranslatedSubtitlesLanguage();
        boolean hasFoundAutoTranslatedSubtitlesLanguage = false;
        SubtitlesStream translatableSubtitle = null;
        boolean hasDuplicateEntries = false;

        for (int i = 0; i < captionsArray.size(); i++) {
            final String languageCode = captionsArray.getObject(i).getString("languageCode");
            final String baseUrl = captionsArray.getObject(i).getString("baseUrl");
            final String vssId = captionsArray.getObject(i).getString("vssId");

            if (languageCode != null && baseUrl != null && vssId != null) {
                final boolean isAutoGenerated = vssId.startsWith("a.");
                final String cleanUrl = (baseUrl.startsWith("/")
                        ? "https://www.youtube.com" + baseUrl : baseUrl)
                        // Remove preexisting format if exists
                        .replaceAll("&fmt=[^&]*", "")
                        // Remove translation language
                        .replaceAll("&tlang=[^&]*", "");

                String remoteSubtitleUrl = cleanUrl + "&fmt=" + format.getSuffix();

                if (i == 0) { // only check once
                    hasDuplicateEntries = SubtitleDeduplicator.hasDuplicateEntries(remoteSubtitleUrl);
                }

                subtitlesToReturn.add(new SubtitlesStream.Builder()
                        .setContent(hasDuplicateEntries? SubtitleDeduplicator.getDeduplicatedContent(remoteSubtitleUrl): remoteSubtitleUrl, !hasDuplicateEntries)
                        .setMediaFormat(format)
                        .setLanguageCode(languageCode)
                        .setAutoGenerated(isAutoGenerated)
                        .build());

                hasFoundAutoTranslatedSubtitlesLanguage = hasFoundAutoTranslatedSubtitlesLanguage
                        || languageCode.equals(autoTranslatedSubtitlesLanguage);
                if (translatableSubtitle == null) {
                    translatableSubtitle = subtitlesToReturn.get(subtitlesToReturn.size() - 1);
                }
            }
        }

        if (translatableSubtitle != null
                && !"original".equals(autoTranslatedSubtitlesLanguage)
                && autoTranslatedSubtitlesLanguage != null
                && !hasFoundAutoTranslatedSubtitlesLanguage
                && ServiceList.YouTube.getShowAutoTranslatedSubtitles()) {
            if (!hasDuplicateEntries) {
                String autoTranslatedContent = SubtitleDeduplicator.getValidSubtitleContent(translatableSubtitle.getContent() + "&tlang=" + autoTranslatedSubtitlesLanguage);
                if(autoTranslatedContent != null) {
                    subtitlesToReturn.add(new SubtitlesStream.Builder()
                            .setContent(autoTranslatedContent, false)
                            .setMediaFormat(format)
                            .setLanguageCode(autoTranslatedSubtitlesLanguage)
                            .setAutoGenerated(false)
                            .build());
                }
            }
        }

        return subtitlesToReturn;
    }


    @Override
    public boolean isSupportComments() throws ParsingException {
        return !getStreamType().equals(StreamType.LIVE_STREAM);
    }

    @Override
    public StreamType getStreamType() {
        return streamType;
    }

    public void setStreamType() {
        final JsonObject videoDetails = playerResponse.getObject("videoDetails");
        final JsonObject playabilityStatus = playerResponse.getObject("playabilityStatus");
        if (videoDetails.getBoolean("isLive", false)) {
            streamType = StreamType.LIVE_STREAM;
        } else if (videoDetails.getBoolean("isPostLiveDvr", false)) {
            streamType = StreamType.POST_LIVE_STREAM;
        } else if (playabilityStatus.has("liveStreamability")) {
            if (StringUtils.isBlank(ServiceList.YouTube.getTokens())
                    && !playerResponse.getObject(STREAMING_DATA).has("hlsManifestUrl")
                    && !"tv_downgraded".equals(NewPipe.getYoutubePlayerClient())) {
                streamType = StreamType.VIDEO_STREAM;
            } else {
                streamType = StreamType.LIVE_STREAM;
            }
        } else {
            streamType = StreamType.VIDEO_STREAM;
        }
        watchDataCache.streamType = streamType;
    }

    @Nullable
    @Override
    public MultiInfoItemsCollector getRelatedItems() throws ExtractionException {
        assertPageFetched();

        if (getAgeLimit() != NO_AGE_LIMIT) {
            return null;
        }

        try {
            final MultiInfoItemsCollector collector = new MultiInfoItemsCollector(getServiceId());

            final JsonArray results = nextResponse
                    .getObject("contents")
                    .getObject("twoColumnWatchNextResults")
                    .getObject("secondaryResults")
                    .getObject("secondaryResults")
                    .getArray("results");

            final TimeAgoParser timeAgoParser = getTimeAgoParser();
            results.stream()
                    .filter(JsonObject.class::isInstance)
                    .map(JsonObject.class::cast)
                    .map(result -> {
                        if (result.has("compactVideoRenderer")) {
                            return new YoutubeStreamInfoItemExtractor(
                                    result.getObject("compactVideoRenderer"), timeAgoParser);
                        } else if (result.has("compactRadioRenderer")) {
                            return new YoutubeMixOrPlaylistInfoItemExtractor(
                                    result.getObject("compactRadioRenderer"));
                        } else if (result.has("compactPlaylistRenderer")) {
                            return new YoutubeMixOrPlaylistInfoItemExtractor(
                                    result.getObject("compactPlaylistRenderer"));
                        } else if (result.has("lockupViewModel")) {
                            final JsonObject lockupViewModel = result.getObject("lockupViewModel");
                            if ("LOCKUP_CONTENT_TYPE_PLAYLIST".equals(
                                    lockupViewModel.getString("contentType"))) {
                                return new YoutubeMixOrPlaylistLockupInfoItemExtractor(
                                        lockupViewModel);
                            }
                            else if ("LOCKUP_CONTENT_TYPE_VIDEO".equals(
                                    lockupViewModel.getString("contentType"))){
                                return new YoutubeLockupStreamInfoItemExtractor(lockupViewModel, timeAgoParser);
                            }
                        }
                        return null;
                    })
                    .filter(Objects::nonNull)
                    .forEach(collector::commit);

            if (ServiceList.YouTube.getFilterTypes().contains("related_item")) {
                collector.applyBlocking(ServiceList.YouTube.getFilterConfig());
            }
            return collector;
        } catch (final Exception e) {
            throw new ParsingException("Could not get related videos", e);
        }
    }

    @Override
    public long getDislikeCount() throws ParsingException {
        try {
            return dislikeData.getLong("dislikes");
        } catch (Exception e) {
            return -1;
        }
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getErrorMessage() {
        try {
            return getTextFromObject(playerResponse.getObject("playabilityStatus")
                    .getObject("errorScreen").getObject("playerErrorMessageRenderer")
                    .getObject("reason"));
        } catch (final ParsingException | NullPointerException e) {
            return null; // No error message
        }
    }

    /*//////////////////////////////////////////////////////////////////////////
    // Fetch page
    //////////////////////////////////////////////////////////////////////////*/

    private static final String ADAPTIVE_FORMATS = "adaptiveFormats";
    private static final String STREAMING_DATA = "streamingData";
    private static final String PLAYER = "player";
    private static final String NEXT = "next";

    private synchronized void updateAvailableAt(@Nonnull final JsonObject response) {
        final double[] waitSeconds = {0};
        final JsonArray adPlacements = response.getArray("adPlacements");
        if (adPlacements != null) {
            for (int i = 0; i < adPlacements.size(); i++) {
                final JsonObject placement = adPlacements.getObject(i)
                        .getObject("adPlacementRenderer");
                if ("AD_PLACEMENT_KIND_START".equals(placement.getObject("config")
                        .getObject("adPlacementConfig").getString("kind"))) {
                    addAdWaitSeconds(placement.getObject("renderer"), waitSeconds);
                }
            }
        }
        final JsonArray adSlots = response.getArray("adSlots");
        if (adSlots != null) {
            for (int i = 0; i < adSlots.size(); i++) {
                final JsonObject slot = adSlots.getObject(i).getObject("adSlotRenderer");
                if ("SLOT_TRIGGER_EVENT_BEFORE_CONTENT".equals(slot
                        .getObject("adSlotMetadata").getString("triggerEvent"))) {
                    addAdWaitSeconds(slot.getObject("fulfillmentContent"), waitSeconds);
                }
            }
        }
        availableAt = Math.max(availableAt, (long) Math.ceil(
                System.currentTimeMillis() / 1000.0 + waitSeconds[0]));
        finalWaitSeconds = Math.max(finalWaitSeconds, waitSeconds[0]);
    }

    private static void addAdWaitSeconds(@Nullable final Object value,
                                         @Nonnull final double[] waitSeconds) {
        if (value instanceof JsonObject) {
            final JsonObject object = (JsonObject) value;
            if (object.has("instreamVideoAdRenderer")) {
                final JsonObject renderer = object.getObject("instreamVideoAdRenderer");
                double duration = -1;
                for (final String parameter : renderer.getString("playerVars", EMPTY_STRING)
                        .split("&")) {
                    final String[] pair = parameter.split("=", 2);
                    if (pair.length == 2 && "length_seconds".equals(pair[0])) {
                        try {
                            duration = Double.parseDouble(pair[1]);
                        } catch (final NumberFormatException ignored) {
                        }
                    }
                }
                if (renderer.has("skipOffsetMilliseconds")) {
                    duration = renderer.getDouble("skipOffsetMilliseconds") / 1000;
                }
                if (duration >= 0) {
                    waitSeconds[0] += duration;
                }
            }
            for (final Object child : object.values()) {
                addAdWaitSeconds(child, waitSeconds);
            }
        } else if (value instanceof JsonArray) {
            final JsonArray array = (JsonArray) value;
            for (int i = 0; i < array.size(); i++) {
                addAdWaitSeconds(array.get(i), waitSeconds);
            }
        }
    }

    private long getStreamAvailableAt() {
        return streamType == StreamType.LIVE_STREAM || streamType == StreamType.POST_LIVE_STREAM
                ? Stream.AVAILABLE_AT_UNKNOWN : availableAt;
    }



    @Override
    public void onFetchPage(@Nonnull final Downloader downloader)
            throws IOException, ExtractionException {
        NewPipe.checkWebViewAvailable();

        final String videoId = getId();
        final Localization localization = new Localization("en");
        final ContentCountry contentCountry = getExtractorContentCountry();

        resetFetchState();

        final CancellableCall webPageCall = YoutubeParsingHelper.getWebPlayerResponse(
                localization, contentCountry, videoId, this);
        final CancellableCall androidCall = fetchAndroidVRJsonPlayer(
                contentCountry, localization, videoId);
        final CancellableCall safariCall = fetchSafariJsonPlayer(
                contentCountry, localization, videoId);
        final CancellableCall tvHtml5Call = fetchTvHtml5EmbedJsonPlayer(
                contentCountry, localization, videoId);

        final byte[] body = JsonWriter.string(
                prepareDesktopJsonBuilder(getExtractorLocalization(), contentCountry)
                        .value(VIDEO_ID, videoId)
                        .value(CONTENT_CHECK_OK, true)
                        .value(RACY_CHECK_OK, true)
                        .done())
                .getBytes(StandardCharsets.UTF_8);
        final CancellableCall nextDataCall = getJsonPostResponseAsync(
                NEXT,
                body,
                localization,
                new Downloader.AsyncCallback() {
                    @Override
                    public void onSuccess(final Response response) {
                        try {
                            nextResponse = JsonUtils.toJsonObject(
                                    getValidJsonResponseBody(response));
                        } catch (final Exception error) {
                            addError(error);
                        }
                    }

                    @Override
                    public void onError(final Exception error) {
                        addError(error);
                    }
                });

        if (ServiceList.YouTube.isFetchDislike()) {
            downloader.getAsync(
                    "https://returnyoutubedislikeapi.com/votes?videoId=" + videoId,
                    new Downloader.AsyncCallback() {
                        @Override
                        public void onSuccess(final Response response) throws ExtractionException {
                            try {
                                dislikeData = new JSONObject(getValidJsonResponseBody(response));
                            } catch (final JSONException | MalformedURLException ignored) {
                            }
                        }
                    });
        }

        awaitPlaybackCallsBestEffort(
                new CancellableCall[]{androidCall, safariCall, tvHtml5Call,
                        webPageCall, nextDataCall},
                ServiceList.YouTube.getLoadingTimeout());

        selectPrimaryStreamingData();

        if (!hasAnyPlayableData()) {
            throwIfErrors();
            throw new ContentNotSupportedException(
                    "YouTube returned no playable stream data from Android VR, Safari, or TVHTML5");
        }

        if (playerResponse == null) {
            throwIfErrors();
            throw new ExtractionException("YouTube player response is missing");
        }

        checkPlayabilityStatus(playerResponse.getObject("playabilityStatus"), videoId);
        setStreamType();

        if (streamType == StreamType.LIVE_STREAM
                && getHlsManifestUrlFromStreamingData().isEmpty()) {
            final CancellableCall mwebHlsCall = fetchMwebHlsManifest(
                    contentCountry, localization, videoId);
            awaitCallsBestEffort(
                    new CancellableCall[]{mwebHlsCall},
                    ServiceList.YouTube.getLoadingTimeout());
        }

        System.out.println("YouTube video " + videoId + " wait time: "
                + finalWaitSeconds + " seconds");
    }

    private void resetFetchState() {
        synchronized (errors) {
            errors.clear();
        }
        playerResponse = null;
        nextResponse = null;
        androidPlayerResponse = null;
        safariPlayerResponse = null;
        tvHtml5PlayerResponse = null;
        webStreamingData = null;
        mwebStreamingData = null;
        androidStreamingData = null;
        safariStreamingData = null;
        tvHtml5SimplyEmbedStreamingData = null;
        configuredStreamingData = null;
        playerCaptionsTracklistRenderer = null;
        mwebHlsManifestUrl = EMPTY_STRING;
        cachedAudioStreams = null;
        cachedVideoStreams = null;
        cachedVideoOnlyStreams = null;
        streamsCached = false;
        webCpn = null;
        mwebCpn = null;
        androidCpn = null;
        safariCpn = null;
        tvHtml5SimplyEmbedCpn = null;
        configuredCpn = null;
    }

    private void awaitPlaybackCallsBestEffort(
            @Nonnull final CancellableCall[] calls,
            final long timeoutSeconds) throws ExtractionException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        long firstDirectStreamAt = -1L;
        try {
            while (System.nanoTime() < deadline) {
                final long now = System.nanoTime();
                if (hasAnyDirectOrManifestData() && firstDirectStreamAt < 0L) {
                    firstDirectStreamAt = now;
                }
                if (areAllCallsFinished(calls)) {
                    break;
                }
                if (firstDirectStreamAt > 0L) {
                    final boolean metadataFinished = areTrailingCallsFinished(calls, 2);
                    final long playableElapsed = now - firstDirectStreamAt;
                    if ((metadataFinished
                            && playableElapsed >= TimeUnit.MILLISECONDS.toNanos(750))
                            || playableElapsed >= TimeUnit.SECONDS.toNanos(3)) {
                        break;
                    }
                }
                Thread.sleep(20L);
            }
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new ExtractionException("YouTube extraction interrupted", e);
        } finally {
            cancelCalls(calls);
        }
    }

    private static boolean areTrailingCallsFinished(@Nonnull final CancellableCall[] calls,
                                                    final int count) {
        final int start = Math.max(0, calls.length - count);
        for (int i = start; i < calls.length; i++) {
            final CancellableCall call = calls[i];
            if (call != null && !call.isFinished()) {
                return false;
            }
        }
        return true;
    }

    private static boolean areAllCallsFinished(@Nonnull final CancellableCall[] calls) {
        for (final CancellableCall call : calls) {
            if (call != null && !call.isFinished()) {
                return false;
            }
        }
        return true;
    }

    private void awaitCallsBestEffort(@Nonnull final CancellableCall[] calls,
                                      final long timeoutSeconds) throws ExtractionException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        try {
            for (final CancellableCall call : calls) {
                if (call == null || call.isFinished()) {
                    continue;
                }
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || !call.await(remaining, TimeUnit.NANOSECONDS)) {
                    break;
                }
            }
        } catch (final InterruptedException e) {
            cancelCalls(calls);
            Thread.currentThread().interrupt();
            throw new ExtractionException("YouTube extraction interrupted", e);
        } finally {
            cancelCalls(calls);
        }
    }

    private synchronized void acceptPlayerResponse(
            @Nonnull final String clientName,
            @Nonnull final JsonObject response,
            @Nonnull final String videoId,
            @Nullable final JsonObject streamingData) throws ExtractionException {
        if (isPlayerResponseNotValid(response, videoId)) {
            throw new ExtractionException(clientName + " player response is not valid");
        }
        checkPlayabilityStatus(response.getObject("playabilityStatus"), videoId);
        updateAvailableAt(response);

        if (playerResponse == null
                || (!hasPreferredPlayableData(playerResponse.getObject(STREAMING_DATA))
                && hasPreferredPlayableData(streamingData))) {
            playerResponse = response;
        }

        if (isNullOrEmpty(playerCaptionsTracklistRenderer)) {
            playerCaptionsTracklistRenderer = response
                    .getObject("captions")
                    .getObject("playerCaptionsTracklistRenderer");
        }
        if (isNullOrEmpty(playerMicroFormatRenderer)) {
            playerMicroFormatRenderer = response
                    .getObject("microformat")
                    .getObject("playerMicroformatRenderer");
        }
    }

    private void selectPrimaryStreamingData() {
        if (selectPrimaryDirectStreamingData()) {
            return;
        }
        if (hasPlayableData(androidStreamingData)) {
            setPrimaryStreamingData(androidStreamingData, androidCpn, androidPlayerResponse);
        } else if (hasPlayableData(safariStreamingData)) {
            setPrimaryStreamingData(safariStreamingData, safariCpn, safariPlayerResponse);
        } else if (hasPlayableData(tvHtml5SimplyEmbedStreamingData)) {
            setPrimaryStreamingData(tvHtml5SimplyEmbedStreamingData,
                    tvHtml5SimplyEmbedCpn, tvHtml5PlayerResponse);
        } else if (hasPlayableData(webStreamingData)) {
            setPrimaryStreamingData(webStreamingData, webCpn, null);
        } else if (hasPlayableData(mwebStreamingData)) {
            setPrimaryStreamingData(mwebStreamingData, mwebCpn, null);
        }
    }

    private boolean selectPrimaryDirectStreamingData() {
        if (hasPreferredPlayableData(androidStreamingData)) {
            setPrimaryStreamingData(androidStreamingData, androidCpn, androidPlayerResponse);
            return true;
        }
        if (hasPreferredPlayableData(safariStreamingData)) {
            setPrimaryStreamingData(safariStreamingData, safariCpn, safariPlayerResponse);
            return true;
        }
        if (hasPreferredPlayableData(tvHtml5SimplyEmbedStreamingData)) {
            setPrimaryStreamingData(tvHtml5SimplyEmbedStreamingData,
                    tvHtml5SimplyEmbedCpn, tvHtml5PlayerResponse);
            return true;
        }
        if (hasPreferredPlayableData(webStreamingData)) {
            setPrimaryStreamingData(webStreamingData, webCpn, null);
            return true;
        }
        if (hasPreferredPlayableData(mwebStreamingData)) {
            setPrimaryStreamingData(mwebStreamingData, mwebCpn, null);
            return true;
        }
        return false;
    }

    private void setPrimaryStreamingData(@Nonnull final JsonObject streamingData,
                                         @Nullable final String cpn,
                                         @Nullable final JsonObject response) {
        configuredStreamingData = streamingData;
        configuredCpn = cpn;
        if (response != null) {
            playerResponse = response;
        }
    }

    private boolean hasAnyPlayableData() {
        return hasPlayableData(androidStreamingData)
                || hasPlayableData(safariStreamingData)
                || hasPlayableData(tvHtml5SimplyEmbedStreamingData)
                || hasPlayableData(configuredStreamingData)
                || hasPlayableData(webStreamingData)
                || hasPlayableData(mwebStreamingData);
    }

    private boolean hasAnyDirectOrManifestData() {
        return hasPreferredPlayableData(androidStreamingData)
                || hasPreferredPlayableData(safariStreamingData)
                || hasPreferredPlayableData(tvHtml5SimplyEmbedStreamingData)
                || hasPreferredPlayableData(configuredStreamingData)
                || hasPreferredPlayableData(webStreamingData)
                || hasPreferredPlayableData(mwebStreamingData);
    }

    private static boolean hasPlayableData(@Nullable final JsonObject streamingData) {
        if (streamingData == null || streamingData.isEmpty()) {
            return false;
        }
        return hasPreferredPlayableData(streamingData)
                || !streamingData.getString("serverAbrStreamingUrl", EMPTY_STRING).isEmpty();
    }

    private static boolean hasPreferredPlayableData(
            @Nullable final JsonObject streamingData) {
        if (streamingData == null || streamingData.isEmpty()) {
            return false;
        }
        if (!streamingData.getString("hlsManifestUrl", EMPTY_STRING).isEmpty()
                || !streamingData.getString("dashManifestUrl", EMPTY_STRING).isEmpty()) {
            return true;
        }
        return hasUsableDirectStreams(streamingData);
    }

    private void awaitRequiredCalls(@Nonnull final CancellableCall[] calls,
                                    final long timeoutSeconds) throws ExtractionException {
        final long deadline = System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds);
        try {
            for (final CancellableCall call : calls) {
                final long remaining = deadline - System.nanoTime();
                if (remaining <= 0 || !call.await(remaining, TimeUnit.NANOSECONDS)) {
                    cancelCalls(calls);
                    throwIfErrors();
                    throw new ExtractionException("YouTube requests timed out");
                }
            }
        } catch (final InterruptedException e) {
            cancelCalls(calls);
            Thread.currentThread().interrupt();
            throw new ExtractionException("YouTube extraction interrupted", e);
        }
    }

    private static void cancelCalls(@Nonnull final CancellableCall[] calls) {
        for (final CancellableCall call : calls) {
            if (call != null && !call.isFinished()) {
                call.cancel();
            }
        }
    }

    private void throwIfErrors() throws ExtractionException {
        final List<Throwable> recordedErrors;
        synchronized (errors) {
            recordedErrors = new ArrayList<>(errors);
        }
        for (final Throwable error : recordedErrors) {
            if (error instanceof AntiBotException) {
                throw (AntiBotException) error;
            }
        }
        for (final Throwable error : recordedErrors) {
            if (error instanceof ContentNotAvailableException) {
                throw (ContentNotAvailableException) error;
            }
        }
        if (!recordedErrors.isEmpty()) {
            throw new ExtractionException(recordedErrors.get(0));
        }
    }

    private boolean hasSabrStreamingUrl() {
        final JsonObject streamingData = getSabrStreamingData();
        return streamingData != null
                && !streamingData.getString("serverAbrStreamingUrl", EMPTY_STRING).isEmpty();
    }



    public static JsonObject checkPlayabilityStatus(@Nonnull JsonObject playabilityStatus, String videoId)
            throws ParsingException {
        String status = playabilityStatus.getString("status");
        if (status == null || status.equalsIgnoreCase("ok")) {
            return null;
        }

        final String reason = playabilityStatus.getString("reason");

        if (status.equalsIgnoreCase("login_required")) {
            if (reason == null) {
                final String message = playabilityStatus.getArray("messages").getString(0);
                if (message != null && message.contains("private")) {
                    throw new PrivateContentException("This video is private");
                }
            } else if (reason.contains("age")) {
                throw new AgeRestrictedContentException(
                        "This age-restricted video cannot be watched anonymously");
            }
        }

        if ((status.equalsIgnoreCase("unplayable") || status.equalsIgnoreCase("error"))
                && reason != null) {
            if (reason.contains("Music Premium")) {
                throw new YoutubeMusicPremiumContentException();
            }

            if (reason.contains("payment")) {
                throw new PaidContentException("This video is a paid video");
            }

            if (reason.contains("members-only")) {
                throw new PaidContentException("This video is only available"
                        + " for members of the channel of this video");
            }

            if (reason.contains("unavailable")) {
                final String detailedErrorMessage = getTextFromObject(playabilityStatus
                        .getObject("errorScreen")
                        .getObject("playerErrorMessageRenderer")
                        .getObject("subreason"));
                if (detailedErrorMessage != null && detailedErrorMessage.contains("country")) {
                    throw new GeographicRestrictionException(
                            "This video is not available in client's country.");
                } else {
                    if(detailedErrorMessage != null) {
                        throw new ContentNotAvailableException(detailedErrorMessage);
                    }
                    throw new ContentNotAvailableException(reason);
                }
            }
        }
        if (reason != null && reason.contains("Sign in to confirm")) {
            throw new AntiBotException(reason);
        }

        if (reason != null && reason.contains("This live event will begin in")) {
            throw new LiveNotStartException(reason);
        }

        if (reason != null && reason.contains("Premieres in")) {
            throw new VideoNotReleaseException(reason);
        }

        throw new ContentNotAvailableException("Got error: \"" + reason + "\"");
    }

    private CancellableCall fetchWebJsonPlayer(@Nonnull final ContentCountry contentCountry,
                                             @Nonnull final Localization localization,
                                             @Nonnull final String videoId)
            throws IOException, ExtractionException {
        webCpn = generateContentPlaybackNonce();

        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(Response response) {
                JsonObject webPlayerResponse = null;
                try {
                    webPlayerResponse = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
                    if (isPlayerResponseNotValid(webPlayerResponse, videoId)) {
                        if (webPlayerResponse.toString().contains("Sign in to confirm")) {
                            throw new AntiBotException("Web player response is not valid");
                        }
                        throw new ExtractionException("Web player response is not valid");
                    }

                    YoutubeStreamExtractor.this.playerResponse = webPlayerResponse;
                    updateAvailableAt(webPlayerResponse);

                    final JsonObject streamingData = webPlayerResponse.getObject(STREAMING_DATA);
                    if (!isNullOrEmpty(streamingData)) {
                        webStreamingData = streamingData;
                        configuredStreamingData = streamingData;
                        playerCaptionsTracklistRenderer = webPlayerResponse.getObject("captions")
                                .getObject("playerCaptionsTracklistRenderer");
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    addError(e);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };

        return getJsonPlayerResponseAsync(PLAYER,
                createJsonPlayerBody(localization,
                        contentCountry,
                        videoId,
                        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId),
                        webCpn,
                        "WEB", WEB_USER_AGENT), localization, "1", WEB_USER_AGENT, callback);
    }

    private CancellableCall fetchMwebJsonPlayer(@Nonnull final ContentCountry contentCountry,
                                                  @Nonnull final Localization localization,
                                                  @Nonnull final String videoId)
            throws IOException, ExtractionException {
        mwebCpn = generateContentPlaybackNonce();

        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(Response response) {
                try {
                    final JsonObject mwebPlayerResponse = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
                    if (isPlayerResponseNotValid(mwebPlayerResponse, videoId)) {
                        if (mwebPlayerResponse.toString().contains("Sign in to confirm")) {
                            throw new AntiBotException("MWEB player response is not valid");
                        }
                        throw new ExtractionException("MWEB player response is not valid");
                    }

                    YoutubeStreamExtractor.this.playerResponse = mwebPlayerResponse;
                    updateAvailableAt(mwebPlayerResponse);

                    final JsonObject streamingData = mwebPlayerResponse.getObject(STREAMING_DATA);
                    if (!isNullOrEmpty(streamingData)) {
                        mwebStreamingData = streamingData;
                        configuredStreamingData = streamingData;
                        if (isNullOrEmpty(playerCaptionsTracklistRenderer)) {
                            playerCaptionsTracklistRenderer = mwebPlayerResponse.getObject("captions")
                                    .getObject("playerCaptionsTracklistRenderer");
                        }
                    }
                } catch (Exception e) {
                    e.printStackTrace();
                    addError(e);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };

        return getJsonPlayerResponseAsync(PLAYER,
                createJsonPlayerBody(localization,
                        contentCountry,
                        videoId,
                        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId),
                        mwebCpn,
                        "MWEB",
                        MWEB_USER_AGENT), localization, "2", MWEB_USER_AGENT, callback);
    }

    private CancellableCall fetchMwebHlsManifest(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        final String cpn = generateContentPlaybackNonce();
        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(final Response response) {
                try {
                    final JsonObject mwebPlayerResponse = JsonUtils.toJsonObject(
                            getValidJsonResponseBody(response));
                    if (isPlayerResponseNotValid(mwebPlayerResponse, videoId)) {
                        throw new ExtractionException("MWEB player response is not valid");
                    }
                    mwebHlsManifestUrl = mwebPlayerResponse.getObject(STREAMING_DATA)
                            .getString("hlsManifestUrl", EMPTY_STRING);
                } catch (final Exception e) {
                    addError(e);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };
        return getJsonPlayerResponseAsync(PLAYER,
                createJsonPlayerBody(localization, contentCountry, videoId,
                        YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId), cpn,
                        "MWEB", MWEB_USER_AGENT),
                localization, "2", MWEB_USER_AGENT, callback);
    }

    private CancellableCall fetchAndroidVRJsonPlayer(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        androidCpn = generateContentPlaybackNonce();
        final InnertubeClientRequestInfo requestInfo =
                InnertubeClientRequestInfo.ofAndroidClient();
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(getAndroidUserAgent(localization)));
        headers.put("X-Goog-Api-Format-Version", singletonList("2"));
        final String visitorData = getVisitorDataFromInnertube(
                requestInfo,
                localization,
                contentCountry,
                headers,
                YOUTUBEI_V1_GAPIS_URL,
                null,
                false);
        final byte[] body = JsonWriter.string(
                        prepareAndroidVRJsonBuilder(localization, contentCountry, visitorData)
                                .value(VIDEO_ID, videoId)
                                .value(CPN, androidCpn)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(final Response response) {
                try {
                    final JsonObject androidResponse = JsonUtils.toJsonObject(
                            getValidJsonResponseBody(response));
                    final JsonObject streamingData = androidResponse.getObject(STREAMING_DATA);
                    acceptPlayerResponse(
                            "ANDROID_VR", androidResponse, videoId, streamingData);
                    androidPlayerResponse = androidResponse;
                    if (!isNullOrEmpty(streamingData)) {
                        androidStreamingData = streamingData;
                    }
                } catch (final Exception error) {
                    addError(error);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };
        return getJsonAndroidVRPostResponseAsync(
                PLAYER,
                body,
                localization,
                "&t=" + generateTParameter() + "&id=" + videoId,
                callback);
    }

    private CancellableCall fetchTvHtml5EmbedJsonPlayer(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        tvHtml5SimplyEmbedCpn = generateContentPlaybackNonce();
        final byte[] body = createTvHtml5EmbedPlayerBody(
                localization,
                contentCountry,
                videoId,
                YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId),
                tvHtml5SimplyEmbedCpn);
        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(final Response response) {
                try {
                    final JsonObject tvResponse = JsonUtils.toJsonObject(
                            getValidJsonResponseBody(response));
                    final JsonObject streamingData = tvResponse.getObject(STREAMING_DATA);
                    acceptPlayerResponse("TVHTML5", tvResponse, videoId, streamingData);
                    tvHtml5PlayerResponse = tvResponse;
                    if (!isNullOrEmpty(streamingData)) {
                        tvHtml5SimplyEmbedStreamingData = streamingData;
                    }
                } catch (final Exception error) {
                    addError(error);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };
        return getLoggedJsonPostResponseAsync(PLAYER, body, localization, callback);
    }

    private CancellableCall fetchSafariJsonPlayer(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId) throws IOException, ExtractionException {
        safariCpn = generateContentPlaybackNonce();
        final byte[] body = createSafariPlayerBody(
                localization,
                contentCountry,
                videoId,
                YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId),
                safariCpn);
        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(final Response response) {
                try {
                    final JsonObject safariResponse = JsonUtils.toJsonObject(
                            getValidJsonResponseBody(response));
                    final JsonObject streamingData = safariResponse.getObject(STREAMING_DATA);
                    acceptPlayerResponse("Safari", safariResponse, videoId, streamingData);
                    safariPlayerResponse = safariResponse;
                    if (!isNullOrEmpty(streamingData)) {
                        safariStreamingData = streamingData;
                    }
                } catch (final Exception error) {
                    addError(error);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };
        return getSafariPostResponseAsync(PLAYER, body, localization, callback);
    }

    private static boolean hasUsableDirectStreams(@Nullable final JsonObject streamingData) {
        if (streamingData == null || streamingData.isEmpty()) {
            return false;
        }
        if (!streamingData.getString("hlsManifestUrl", EMPTY_STRING).isEmpty()
                || !streamingData.getString("dashManifestUrl", EMPTY_STRING).isEmpty()) {
            return true;
        }
        for (final String key : Arrays.asList("formats", ADAPTIVE_FORMATS)) {
            final JsonArray formats = streamingData.getArray(key);
            if (formats == null) {
                continue;
            }
            for (int i = 0; i < formats.size(); i++) {
                final JsonObject format = formats.getObject(i);
                if (!format.getString("url", EMPTY_STRING).isEmpty()
                        || !format.getString("signatureCipher", EMPTY_STRING).isEmpty()
                        || !format.getString("cipher", EMPTY_STRING).isEmpty()) {
                    return true;
                }
            }
        }
        return false;
    }

    private CancellableCall fetchConfiguredJsonPlayer(
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Localization localization,
            @Nonnull final String videoId,
            @Nonnull final String selectedClient) throws IOException, ExtractionException {
        final PlayerClient client = PlayerClient.forName(selectedClient);
        configuredCpn = generateContentPlaybackNonce();
        final JsonBuilder<JsonObject> clientBuilder = JsonObject.builder()
                .value("utcOffsetMinutes", 0)
                .value("timeZone", "UTC")
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("userAgent", client.userAgent)
                .value("clientName", client.clientName)
                .value("clientVersion", client.clientVersion);
        if ("android_vr".equals(selectedClient)) {
            clientBuilder.value("deviceMake", "Oculus")
                    .value("deviceModel", "Quest 3")
                    .value("androidSdkVersion", 32)
                    .value("osName", "Android")
                    .value("osVersion", "12L");
            // YouTube only serves ANDROID_VR streaming data when the request carries a visitorData
            // session token (this mirrors the upstream fetchAndroidVRJsonPlayer). Without it the
            // player response comes back with no usable streamingData, so playback never resolves.
            try {
                final java.util.Map<String, java.util.List<String>> vrHeaders =
                        new java.util.HashMap<>();
                vrHeaders.put("Content-Type", singletonList("application/json"));
                vrHeaders.put("User-Agent", singletonList(getAndroidUserAgent(localization)));
                vrHeaders.put("X-Goog-Api-Format-Version", singletonList("2"));
                final String visitorData = getVisitorDataFromInnertube(
                        org.schabi.newpipe.extractor.services.youtube.InnertubeClientRequestInfo
                                .ofAndroidVRClient(),
                        localization, contentCountry, vrHeaders,
                        YOUTUBEI_V1_GAPIS_URL, null, false);
                if (!isNullOrEmpty(visitorData)) {
                    clientBuilder.value("visitorData", visitorData);
                }
            } catch (final Exception visitorDataError) {
                // Best-effort: fall back to a request without visitorData rather than hard-failing.
            }
        }
        final byte[] body = JsonWriter.string(JsonObject.builder()
                .object("context")
                    .value("client", clientBuilder.done())
                .end()
                .object("playbackContext")
                    .object("contentPlaybackContext")
                        .value("html5Preference", "HTML5_PREF_WANTS")
                        .value("signatureTimestamp",
                                YoutubeJavaScriptPlayerManager.getSignatureTimestamp(videoId))
                    .end()
                .end()
                .value(CPN, configuredCpn)
                .value(VIDEO_ID, videoId)
                .value(CONTENT_CHECK_OK, true)
                .value(RACY_CHECK_OK, true)
                .done()).getBytes(StandardCharsets.UTF_8);
        final Downloader.AsyncCallback callback = new Downloader.AsyncCallback() {
            @Override
            public void onSuccess(final Response response) {
                try {
                    final JsonObject configuredResponse = JsonUtils.toJsonObject(
                            getValidJsonResponseBody(response));
                    checkPlayabilityStatus(
                            configuredResponse.getObject("playabilityStatus"), videoId);
                    if (isPlayerResponseNotValid(configuredResponse, videoId)) {
                        throw new ExtractionException(selectedClient + " player response is not valid");
                    }
                    playerResponse = configuredResponse;
                    updateAvailableAt(configuredResponse);
                    final JsonObject streamingData = configuredResponse.getObject(STREAMING_DATA);
                    if (!isNullOrEmpty(streamingData)) {
                        configuredStreamingData = streamingData;
                        playerCaptionsTracklistRenderer = configuredResponse.getObject("captions")
                                .getObject("playerCaptionsTracklistRenderer");
                    }
                } catch (final Exception e) {
                    addError(e);
                }
            }

            @Override
            public void onError(final Exception error) {
                addError(error);
            }
        };
        return getJsonPlayerResponseAsync(PLAYER, body, localization, client.clientId,
                client.clientVersion, client.userAgent, callback);
    }

    private static final class PlayerClient {
        private final String clientName;
        private final String clientVersion;
        private final String clientId;
        private final String userAgent;

        private PlayerClient(final String clientName, final String clientVersion,
                             final String clientId, final String userAgent) {
            this.clientName = clientName;
            this.clientVersion = clientVersion;
            this.clientId = clientId;
            this.userAgent = userAgent;
        }

        private static PlayerClient forName(final String name) {
            switch (name) {
                case "android_vr":
                    return new PlayerClient("ANDROID_VR", "1.65.10", "28",
                            "com.google.android.apps.youtube.vr.oculus/1.65.10 (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip");
                case "tv_simply":
                    return new PlayerClient("TVHTML5_SIMPLY", "1.0", "75", WEB_USER_AGENT);
                case "tv_downgraded":
                    return new PlayerClient("TVHTML5", "5.20260114", "7",
                            "Mozilla/5.0 (ChromiumStylePlatform) Cobalt/Version");
                default:
                    return new PlayerClient("WEB", "2.20260114.08.00", "1",
                            "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)");
            }
        }
    }


    /**
     * Checks whether an additional player response is not valid.
     *
     * <p>
     * If YouTube detect that requests come from a third party client, they may replace the real
     * player response by another one of a video saying that this content is not available on this
     * app and to watch it on the latest version of YouTube.
     * </p>
     *
     * <p>
     * We can detect this by checking whether the video ID of the player response returned is the
     * same as the one requested by the extractor.
     * </p>
     *
     * <p>
     * This behavior has been already observed on the {@code ANDROID} client, see
     * <a href="https://github.com/TeamNewPipe/NewPipe/issues/8713">
     *     https://github.com/TeamNewPipe/NewPipe/issues/8713</a>.
     * </p>
     *
     * @param additionalPlayerResponse an additional response to the one of the {@code HTML5}
     *                                 client used
     * @param videoId                  the video ID of the content requested
     * @return whether the video ID of the player response is not equal to the one requested
     */
    public static boolean isPlayerResponseNotValid(
            @Nonnull final JsonObject additionalPlayerResponse,
            @Nonnull final String videoId) {
        return !videoId.equals(additionalPlayerResponse.getObject("videoDetails")
                .getString("videoId", ""));
    }


    /*//////////////////////////////////////////////////////////////////////////
    // Utils
    //////////////////////////////////////////////////////////////////////////*/

    @Nonnull
    private JsonObject getVideoPrimaryInfoRenderer() {
        if (videoPrimaryInfoRenderer != null) {
            return videoPrimaryInfoRenderer;
        }

        videoPrimaryInfoRenderer = getVideoInfoRenderer("videoPrimaryInfoRenderer");
        return videoPrimaryInfoRenderer;
    }

    @Nonnull
    private JsonObject getVideoSecondaryInfoRenderer() {
        if (videoSecondaryInfoRenderer != null) {
            return videoSecondaryInfoRenderer;
        }

        videoSecondaryInfoRenderer = getVideoInfoRenderer("videoSecondaryInfoRenderer");
        return videoSecondaryInfoRenderer;
    }

    @Nonnull
    private JsonObject getVideoInfoRenderer(@Nonnull final String videoRendererName) {
        return nextResponse.getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents")
                .stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(content -> content.has(videoRendererName))
                .map(content -> content.getObject(videoRendererName))
                .findFirst()
                .orElse(new JsonObject());
    }

    @Nonnull
    @Override
    public List<Frameset> getFrames() throws ExtractionException {
        try {
            final JsonObject storyboards = playerResponse.getObject("storyboards");
            final JsonObject storyboardsRenderer = storyboards.getObject(
                    storyboards.has("playerLiveStoryboardSpecRenderer")
                            ? "playerLiveStoryboardSpecRenderer"
                            : "playerStoryboardSpecRenderer"
            );

            if (storyboardsRenderer == null) {
                return Collections.emptyList();
            }

            final String storyboardsRendererSpec = storyboardsRenderer.getString("spec");
            if (storyboardsRendererSpec == null) {
                return Collections.emptyList();
            }

            final String[] spec = storyboardsRendererSpec.split("\\|");
            final String url = spec[0];
            final List<Frameset> result = new ArrayList<>(spec.length - 1);

            for (int i = 1; i < spec.length; ++i) {
                final String[] parts = spec[i].split("#");
                if (parts.length != 8 || Integer.parseInt(parts[5]) == 0) {
                    continue;
                }
                final int totalCount = Integer.parseInt(parts[2]);
                final int framesPerPageX = Integer.parseInt(parts[3]);
                final int framesPerPageY = Integer.parseInt(parts[4]);
                final String baseUrl = url.replace("$L", String.valueOf(i - 1))
                        .replace("$N", parts[6]) + "&sigh=" + parts[7];
                final List<String> urls;
                if (baseUrl.contains("$M")) {
                    final int totalPages = (int) Math.ceil(totalCount / (double)
                            (framesPerPageX * framesPerPageY));
                    urls = new ArrayList<>(totalPages);
                    for (int j = 0; j < totalPages; j++) {
                        urls.add(baseUrl.replace("$M", String.valueOf(j)));
                    }
                } else {
                    urls = Collections.singletonList(baseUrl);
                }
                result.add(new Frameset(
                        urls,
                        /*frameWidth=*/Integer.parseInt(parts[0]),
                        /*frameHeight=*/Integer.parseInt(parts[1]),
                        totalCount,
                        /*durationPerFrame=*/Integer.parseInt(parts[5]),
                        framesPerPageX,
                        framesPerPageY
                ));
            }
            return result;
        } catch (final Exception e) {
            throw new ExtractionException("Could not get frames", e);
        }
    }

    @Nonnull
    @Override
    public Privacy getPrivacy() {
        return playerMicroFormatRenderer.getBoolean("isUnlisted")
                ? Privacy.UNLISTED
                : Privacy.PUBLIC;
    }

    @Nonnull
    @Override
    public String getCategory() {
        return playerMicroFormatRenderer.getString("category", EMPTY_STRING);
    }

    @Nonnull
    @Override
    public String getLicence() throws ParsingException {
        final JsonObject metadataRowRenderer = getVideoSecondaryInfoRenderer()
                .getObject("metadataRowContainer")
                .getObject("metadataRowContainerRenderer")
                .getArray("rows")
                .getObject(0)
                .getObject("metadataRowRenderer");

        final JsonArray contents = metadataRowRenderer.getArray("contents");
        final String license = getTextFromObject(contents.getObject(0));
        return license != null
                && "Licence".equals(getTextFromObject(metadataRowRenderer.getObject("title")))
                ? license
                : "YouTube license";
    }

    @Override
    public Locale getLanguageInfo() {
        return null;
    }

    @Nonnull
    @Override
    public List<String> getTags() {
        return JsonUtils.getStringListFromJsonArray(playerResponse.getObject("videoDetails")
                .getArray("keywords"));
    }

    @Nonnull
    @Override
    public List<StreamSegment> getStreamSegments() throws ParsingException {

        if (!nextResponse.has("engagementPanels")) {
            return Collections.emptyList();
        }

        final JsonArray segmentsArray = nextResponse.getArray("engagementPanels")
                .stream()
                // Check if object is a JsonObject
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                // Check if the panel is the correct one
                .filter(panel -> "engagement-panel-macro-markers-description-chapters".equals(
                        panel
                                .getObject("engagementPanelSectionListRenderer")
                                .getString("panelIdentifier")))
                // Extract the data
                .map(panel -> panel
                        .getObject("engagementPanelSectionListRenderer")
                        .getObject("content")
                        .getObject("macroMarkersListRenderer")
                        .getArray("contents"))
                .findFirst()
                .orElse(null);

        // If no data was found exit
        if (segmentsArray == null) {
            return Collections.emptyList();
        }

        final long duration = getLength();
        final List<StreamSegment> segments = new ArrayList<>();
        for (final JsonObject segmentJson : segmentsArray.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .map(object -> object.getObject("macroMarkersListItemRenderer"))
                .collect(Collectors.toList())
        ) {
            final int startTimeSeconds = segmentJson.getObject("onTap")
                    .getObject("watchEndpoint").getInt("startTimeSeconds", -1);

            if (startTimeSeconds == -1) {
                throw new ParsingException("Could not get stream segment start time.");
            }
            if (startTimeSeconds > duration) {
                break;
            }

            final String title = getTextFromObject(segmentJson.getObject("title"));
            if (isNullOrEmpty(title)) {
                throw new ParsingException("Could not get stream segment title.");
            }

            final StreamSegment segment = new StreamSegment(title, startTimeSeconds);
            segment.setUrl(getUrl() + "?t=" + startTimeSeconds);
            if (segmentJson.has("thumbnail")) {
                final JsonArray previewsArray = segmentJson
                        .getObject("thumbnail")
                        .getArray("thumbnails");
                if (!previewsArray.isEmpty()) {
                    // Assume that the thumbnail with the highest resolution is at the last position
                    final String url = previewsArray
                            .getObject(previewsArray.size() - 1)
                            .getString("url");
                    segment.setPreviewUrl(fixThumbnailUrl(url));
                }
            }
            segments.add(segment);
        }
        return segments;
    }

    @Nonnull
    @Override
    public List<MetaInfo> getMetaInfo() throws ParsingException {
        return YoutubeParsingHelper.getMetaInfo(nextResponse
                .getObject("contents")
                .getObject("twoColumnWatchNextResults")
                .getObject("results")
                .getObject("results")
                .getArray("contents"));
    }
}
