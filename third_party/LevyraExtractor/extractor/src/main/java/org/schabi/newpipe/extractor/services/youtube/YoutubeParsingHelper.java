/*
 * Created by Christian Schabesberger on 02.03.16.
 *
 * Copyright (C) Christian Schabesberger 2016 <chris.schabesberger@mailbox.org>
 * YoutubeParsingHelper.java is part of NewPipe Extractor.
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

package org.schabi.newpipe.extractor.services.youtube;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonBuilder;
import com.grack.nanojson.JsonObject;
import com.grack.nanojson.JsonParser;
import com.grack.nanojson.JsonParserException;
import com.grack.nanojson.JsonWriter;

import org.jsoup.nodes.Entities;
import org.schabi.newpipe.extractor.Image;
import org.schabi.newpipe.extractor.MetaInfo;
import org.schabi.newpipe.extractor.ServiceList;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.exceptions.*;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;
import org.schabi.newpipe.extractor.playlist.PlaylistInfo;
import org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor;
import org.schabi.newpipe.extractor.stream.Description;
import org.schabi.newpipe.extractor.utils.JsonUtils;
import org.schabi.newpipe.extractor.utils.Parser;
import org.schabi.newpipe.extractor.utils.RandomStringFromAlphabetGenerator;
import org.schabi.newpipe.extractor.utils.Utils;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static java.util.Collections.singletonList;
import static org.schabi.newpipe.extractor.NewPipe.getDownloader;
import static org.schabi.newpipe.extractor.services.youtube.ClientsConstants.*;
import static org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor.checkPlayabilityStatus;
import static org.schabi.newpipe.extractor.services.youtube.extractors.YoutubeStreamExtractor.isPlayerResponseNotValid;
import static org.schabi.newpipe.extractor.utils.Utils.HTTP;
import static org.schabi.newpipe.extractor.utils.Utils.HTTPS;
import static org.schabi.newpipe.extractor.utils.Utils.UTF_8;
import static org.schabi.newpipe.extractor.utils.Utils.getStringResultFromRegexArray;
import static org.schabi.newpipe.extractor.utils.Utils.isNullOrEmpty;

public final class
YoutubeParsingHelper {

    private YoutubeParsingHelper() {
    }

    /**
     * The base URL of requests of the {@code WEB} clients to the InnerTube internal API.
     */
    public static final String YOUTUBEI_V1_URL = "https://www.youtube.com/youtubei/v1/";

    /**
     * The base URL of requests of non-web clients to the InnerTube internal API.
     */
    public static final String YOUTUBEI_V1_GAPIS_URL =
            "https://youtubei.googleapis.com/youtubei/v1/";

    /**
     * The base URL of YouTube Music.
     */
    private static final String YOUTUBE_MUSIC_URL = "https://music.youtube.com";

    /**
     * A parameter to disable pretty-printed response of InnerTube requests, to reduce response
     * sizes.
     *
     * <p>
     * Sent in query parameters of the requests.
     * </p>
     **/
    public static final String DISABLE_PRETTY_PRINT_PARAMETER = "prettyPrint=false";

    /**
     * A parameter sent by official clients named {@code contentPlaybackNonce}.
     *
     * <p>
     * It is sent by official clients on videoplayback requests and InnerTube player requests in
     * most cases.
     * </p>
     *
     * <p>
     * It is composed of 16 characters which are generated from
     * {@link #CONTENT_PLAYBACK_NONCE_ALPHABET this alphabet}, with the use of strong random
     * values.
     * </p>
     *
     * @see #generateContentPlaybackNonce()
     */
    public static final String CPN = "cpn";
    public static final String VIDEO_ID = "videoId";

    /**
     * A parameter sent by official clients named {@code contentCheckOk}.
     *
     * <p>
     * Setting it to {@code true} allows us to get streaming data on videos with a warning about
     * what the sensible content they contain.
     * </p>
     */
    public static final String CONTENT_CHECK_OK = "contentCheckOk";

    /**
     * A parameter which may be sent by official clients named {@code racyCheckOk}.
     *
     * <p>
     * What this parameter does is not really known, but it seems to be linked to sensitive
     * contents such as age-restricted content.
     * </p>
     */
    public static final String RACY_CHECK_OK = "racyCheckOk";

    /**
     * The hardcoded client ID used for InnerTube requests with the {@code WEB} client.
     */
    private static final String WEB_CLIENT_ID = "1";

    /**
     * The hardcoded client version of the Android app used for InnerTube requests with this
     * client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app in an APK repository
     * such as <a href="https://www.apkmirror.com/apk/google-inc/youtube/">APKMirror</a>.
     * </p>
     */
    private static final String ANDROID_YOUTUBE_CLIENT_VERSION = "21.03.36";

    /**
     * The InnerTube API key used by the {@code ANDROID} client. Found with the help of
     * reverse-engineering app network requests.
     */
    private static final String ANDROID_YOUTUBE_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w";

    private static final String ANDROID_VR_YOUTUBE_CLIENT_VERSION = "1.65.10";
    private static final String ANDROID_VR_YOUTUBE_KEY = "AIzaSyA8eiZmM1FaDVjRy-df2KTyQ_vz_yYM39w";

    /**
     * The hardcoded client version of the iOS app used for InnerTube requests with this
     * client.
     *
     * <p>
     * It can be extracted by getting the latest release version of the app on
     * <a href="https://apps.apple.com/us/app/youtube-watch-listen-stream/id544007664/">the App
     * Store page of the YouTube app</a>, in the {@code What’s New} section.
     * </p>
     */
    private static final String IOS_YOUTUBE_CLIENT_VERSION = "19.45.4";

    /**
     * The InnerTube API key used by the {@code iOS} client. Found with the help of
     * reverse-engineering app network requests.
     */
    private static final String IOS_YOUTUBE_KEY = "AIzaSyB-63vPrdThhKuerbB2N_l7Kwwcxj6yUAc";

    /**
     * The hardcoded client version used for InnerTube requests with the TV HTML5 embed client.
     */
    private static final String TVHTML5_SIMPLY_EMBED_CLIENT_VERSION = "7.20250923.13.00";
    private static final String WEB_CLIENT_VERSION = "2.20241126.01.00";
    private static final String SAFARI_CLIENT_VERSION = "2.20260114.08.00";
    public static final String WEB_USER_AGENT = "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/134.0.0.0 Safari/537.36,gzip(gfe)";
    public static final String MWEB_USER_AGENT = "Mozilla/5.0 (iPad; CPU OS 16_7_10 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/16.6 Mobile/15E148 Safari/604.1,gzip(gfe)";
    public static final String SAFARI_USER_AGENT = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/605.1.15 (KHTML, like Gecko) Version/15.5 Safari/605.1.15,gzip(gfe)";

    private static String clientVersion;

    private static String youtubeMusicClientVersion;

    private static boolean clientVersionExtracted = false;
    @SuppressWarnings("OptionalUsedAsFieldOrParameterType")
    private static Optional<Boolean> hardcodedClientVersionValid = Optional.empty();

    private static final String[] INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES =
            {"INNERTUBE_CONTEXT_CLIENT_VERSION\":\"([0-9\\.]+?)\"",
                    "innertube_context_client_version\":\"([0-9\\.]+?)\"",
                    "client.version=([0-9\\.]+)"};
    private static final String[] INITIAL_DATA_REGEXES =
            {"window\\[\"ytInitialData\"\\]\\s*=\\s*(\\{.*?\\});",
                    "var\\s*ytInitialData\\s*=\\s*(\\{.*?\\});"};
    private static final String CONTENT_PLAYBACK_NONCE_ALPHABET =
            "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789-_";

    /**
     * The device machine id for the iPhone 13, used to get 60fps with the {@code iOS} client.
     *
     * <p>
     * See <a href="https://gist.github.com/adamawolf/3048717">this GitHub Gist</a> for more
     * information.
     * </p>
     */
    private static final String IOS_DEVICE_MODEL = "iPhone16,2";

    /**
     * Spoofing an iPhone 15 Pro Max running iOS 17.5.1 with the hardcoded version of the iOS app.
     * To be used for the {@code "osVersion"} field in JSON POST requests.
     * <p>
     * The value of this field seems to use the following structure:
     * "iOS major version.minor version.patch version.build version", where
     * "patch version" is equal to 0 if it isn't set
     * The build version corresponding to the iOS version used can be found on
     * <a href="https://theapplewiki.com/wiki/Firmware/iPhone/17.x#iPhone_15_Pro_Max">
     *     https://theapplewiki.com/wiki/Firmware/iPhone/17.x#iPhone_15_Pro_Max</a>
     * </p>
     *
     * @see #IOS_USER_AGENT_VERSION
     */
    private static final String IOS_OS_VERSION = "18.1.0.22B83";

    /**
     * Spoofing an iPhone 15 running iOS 17.5.1 with the hardcoded version of the iOS app. To be
     * used in the user agent for requests.
     *
     * @see #IOS_OS_VERSION
     */
    private static final String IOS_USER_AGENT_VERSION = "18_1_0";

    private static Random numberGenerator = new Random();

    private static final String FEED_BASE_CHANNEL_ID =
            "https://www.youtube.com/feeds/videos.xml?channel_id=";
    private static final String FEED_BASE_USER = "https://www.youtube.com/feeds/videos.xml?user=";
    private static final Pattern C_WEB_PATTERN = Pattern.compile("&c=WEB");
    private static final Pattern C_TVHTML5_SIMPLY_EMBEDDED_PLAYER_PATTERN =
            Pattern.compile("&c=TVHTML5_SIMPLY_EMBEDDED_PLAYER");
    private static final Pattern C_ANDROID_PATTERN = Pattern.compile("&c=(?:ANDROID|ANDROID_VR)");
    private static final Pattern C_IOS_PATTERN = Pattern.compile("&c=IOS");

    private static final Set<String> GOOGLE_URLS = Set.of("google.", "m.google.", "www.google.");
    private static final Set<String> INVIDIOUS_URLS = Set.of("invidio.us", "dev.invidio.us",
            "www.invidio.us", "redirect.invidious.io", "invidious.snopyta.org", "yewtu.be",
            "tube.connect.cafe", "tubus.eduvid.org", "invidious.kavin.rocks", "invidious.site",
            "invidious-us.kavin.rocks", "piped.kavin.rocks", "vid.mint.lgbt", "invidiou.site",
            "invidious.fdn.fr", "invidious.048596.xyz", "invidious.zee.li", "vid.puffyan.us",
            "ytprivate.com", "invidious.namazso.eu", "invidious.silkky.cloud", "ytb.trom.tf",
            "invidious.exonip.de", "inv.riverside.rocks", "invidious.blamefran.net", "y.com.cm",
            "invidious.moomoo.me", "yt.cyberhost.uk");
    private static final Set<String> YOUTUBE_URLS = Set.of("youtube.com", "www.youtube.com",
            "m.youtube.com", "music.youtube.com");

    private static boolean consentAccepted = false;

    public static boolean isGoogleURL(final String url) {
        final String cachedUrl = extractCachedUrlIfNeeded(url);
        try {
            final URL u = new URL(cachedUrl);
            return GOOGLE_URLS.stream().anyMatch(item -> u.getHost().startsWith(item));
        } catch (final MalformedURLException e) {
            return false;
        }
    }

    public static boolean isYoutubeURL(@Nonnull final URL url) {
        return YOUTUBE_URLS.contains(url.getHost().toLowerCase(Locale.ROOT));
    }

    public static boolean isYoutubeServiceURL(@Nonnull final URL url) {
        final String host = url.getHost();
        return host.equalsIgnoreCase("www.youtube-nocookie.com")
                || host.equalsIgnoreCase("youtu.be");
    }

    public static boolean isHooktubeURL(@Nonnull final URL url) {
        final String host = url.getHost();
        return host.equalsIgnoreCase("hooktube.com");
    }

    public static boolean isInvidiousURL(@Nonnull final URL url) {
        return INVIDIOUS_URLS.contains(url.getHost().toLowerCase(Locale.ROOT));
    }

    public static boolean isY2ubeURL(@Nonnull final URL url) {
        return url.getHost().equalsIgnoreCase("y2u.be");
    }

    /**
     * Parses the duration string of the video expecting ":" or "." as separators
     *
     * @return the duration in seconds
     * @throws ParsingException when more than 3 separators are found
     */
    public static int parseDurationString(@Nonnull final String input)
            throws ParsingException, NumberFormatException {
        // If time separator : is not detected, try . instead
        final String[] splitInput = input.contains(":")
                ? input.split(":")
                : input.split("\\.");

        final int[] units = {24, 60, 60, 1};
        final int offset = units.length - splitInput.length;
        if (offset < 0) {
            throw new ParsingException("Error duration string with unknown format: " + input);
        }
        int duration = 0;
        for (int i = 0; i < splitInput.length; i++) {
            duration = units[i + offset] * (duration + convertDurationToInt(splitInput[i]));
        }
        return duration;
    }

    /**
     * Tries to convert a duration string to an integer without throwing an exception.
     * <br/>
     * Helper method for {@link #parseDurationString(String)}.
     * <br/>
     * Note: This method is also used as a workaround for NewPipe#8034 (YT shorts no longer
     * display any duration in channels).
     *
     * @param input The string to process
     * @return The converted integer or 0 if the conversion failed.
     */
    private static int convertDurationToInt(final String input) {
        if (input == null || input.isEmpty()) {
            return 0;
        }

        final String clearedInput = Utils.removeNonDigitCharacters(input);
        try {
            return Integer.parseInt(clearedInput);
        } catch (final NumberFormatException ex) {
            return 0;
        }
    }

    @Nonnull
    public static String getFeedUrlFrom(@Nonnull final String channelIdOrUser) {
        if (channelIdOrUser.startsWith("user/")) {
            return FEED_BASE_USER + channelIdOrUser.replace("user/", "");
        } else if (channelIdOrUser.startsWith("channel/")) {
            return FEED_BASE_CHANNEL_ID + channelIdOrUser.replace("channel/", "");
        } else {
            return FEED_BASE_CHANNEL_ID + channelIdOrUser;
        }
    }

    private static final Pattern LOCALIZED_SCHEDULED_DATE_PATTERN =
            Pattern.compile("\\d{1,2}/\\d{1,2}/\\d{2,4}\\s+\\d{1,2}:\\d{2}");

    public static OffsetDateTime parseDateFrom(final String textualUploadDate)
            throws ParsingException {
        try {
            return OffsetDateTime.parse(textualUploadDate);
        } catch (final DateTimeParseException e) {
            try {
                return LocalDate.parse(textualUploadDate).atStartOfDay().atOffset(ZoneOffset.UTC);
            } catch (final DateTimeParseException e1) {
                final java.util.regex.Matcher m =
                        LOCALIZED_SCHEDULED_DATE_PATTERN.matcher(textualUploadDate);
                if (m.find()) {
                    try {
                        return java.time.LocalDateTime.parse(m.group(),
                                java.time.format.DateTimeFormatter.ofPattern(
                                        "M/d/yy HH:mm", Locale.US))
                                .atOffset(ZoneOffset.UTC);
                    } catch (final DateTimeParseException e2) {
                        // fall through
                    }
                }
                throw new ParsingException("Could not parse date: \"" + textualUploadDate + "\"",
                        e1);
            }
        }
    }

    /**
     * Checks if the given playlist id is a YouTube Mix (auto-generated playlist)
     * Ids from a YouTube Mix start with "RD"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Mix
     */
    public static boolean isYoutubeMixId(@Nonnull final String playlistId) {
        return playlistId.startsWith("RD");
    }

    /**
     * Checks if the given playlist id is a YouTube My Mix (auto-generated playlist)
     * Ids from a YouTube My Mix start with "RDMM"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube My Mix
     */
    public static boolean isYoutubeMyMixId(@Nonnull final String playlistId) {
        return playlistId.startsWith("RDMM");
    }

    /**
     * Checks if the given playlist id is a YouTube Music Mix (auto-generated playlist)
     * Ids from a YouTube Music Mix start with "RDAMVM" or "RDCLAK"
     *
     * @param playlistId the playlist id
     * @return Whether given id belongs to a YouTube Music Mix
     */
    public static boolean isYoutubeMusicMixId(@Nonnull final String playlistId) {
        return playlistId.startsWith("RDAMVM") || playlistId.startsWith("RDCLAK");
    }

    /**
     * Checks if the given playlist id is a YouTube Channel Mix (auto-generated playlist)
     * Ids from a YouTube channel Mix start with "RDCM"
     *
     * @return Whether given id belongs to a YouTube Channel Mix
     */
    public static boolean isYoutubeChannelMixId(@Nonnull final String playlistId) {
        return playlistId.startsWith("RDCM");
    }

    /**
     * Checks if the given playlist id is a YouTube Genre Mix (auto-generated playlist)
     * Ids from a YouTube Genre Mix start with "RDGMEM"
     *
     * @return Whether given id belongs to a YouTube Genre Mix
     */
    public static boolean isYoutubeGenreMixId(@Nonnull final String playlistId) {
        return playlistId.startsWith("RDGMEM");
    }

    /**
     * @param playlistId the playlist id to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty, if the playlistId is not a mix,
     *                          if it is a mix but it's not based on a specific stream (this is the
     *                          case for channel or genre mixes)
     */
    @Nonnull
    public static String extractVideoIdFromMixId(final String playlistId)
            throws ParsingException {
        if (isNullOrEmpty(playlistId)) {
            throw new ParsingException("Video id could not be determined from empty playlist id");

        } else if (isYoutubeMyMixId(playlistId)) {
            return playlistId.substring(4);

        } else if (isYoutubeMusicMixId(playlistId)) {
            return playlistId.substring(6);

        } else if (isYoutubeChannelMixId(playlistId)) {
            // Channel mixes are of the form RMCM{channelId}, so videoId can't be determined
            throw new ParsingException("Video id could not be determined from channel mix id: "
                    + playlistId);

        } else if (isYoutubeGenreMixId(playlistId)) {
            // Genre mixes are of the form RDGMEM{garbage}, so videoId can't be determined
            throw new ParsingException("Video id could not be determined from genre mix id: "
                    + playlistId);

        } else if (isYoutubeMixId(playlistId)) { // normal mix
            if (playlistId.length() != 13) {
                // Stream YouTube mixes are of the form RD{videoId}, but if videoId is not exactly
                // 11 characters then it can't be a video id, hence we are dealing with a different
                // type of mix (e.g. genre mixes handled above, of the form RDGMEM{garbage})
                throw new ParsingException("Video id could not be determined from mix id: "
                    + playlistId);
            }
            return playlistId.substring(2);

        } else { // not a mix
            throw new ParsingException("Video id could not be determined from playlist id: "
                    + playlistId);
        }
    }

    /**
     * @param playlistId the playlist id to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistId (mix playlist
     *         types included)
     * @throws ParsingException if the playlistId is null or empty
     */
    @Nonnull
    public static PlaylistInfo.PlaylistType extractPlaylistTypeFromPlaylistId(
            final String playlistId) throws ParsingException {
        if (isNullOrEmpty(playlistId)) {
            throw new ParsingException("Could not extract playlist type from empty playlist id");
        } else if (isYoutubeMusicMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_MUSIC;
        } else if (isYoutubeChannelMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_CHANNEL;
        } else if (isYoutubeGenreMixId(playlistId)) {
            return PlaylistInfo.PlaylistType.MIX_GENRE;
        } else if (isYoutubeMixId(playlistId)) { // normal mix
            // Either a normal mix based on a stream, or a "my mix" (still based on a stream).
            // NOTE: if YouTube introduces even more types of mixes that still start with RD,
            // they will default to this, even though they might not be based on a stream.
            return PlaylistInfo.PlaylistType.MIX_STREAM;
        } else {
            // not a known type of mix: just consider it a normal playlist
            return PlaylistInfo.PlaylistType.NORMAL;
        }
    }

    /**
     * @param playlistUrl the playlist url to parse
     * @return the {@link PlaylistInfo.PlaylistType} extracted from the playlistUrl's list param
     *         (mix playlist types included)
     * @throws ParsingException if the playlistUrl is malformed, if has no list param or if the list
     *                          param is empty
     */
    public static PlaylistInfo.PlaylistType extractPlaylistTypeFromPlaylistUrl(
            final String playlistUrl) throws ParsingException {
        try {
            return extractPlaylistTypeFromPlaylistId(
                    Utils.getQueryValue(Utils.stringToURL(playlistUrl), "list"));
        } catch (final MalformedURLException e) {
            throw new ParsingException("Could not extract playlist type from malformed url", e);
        }
    }

    private static JsonObject getInitialData(final String html) throws ParsingException {
        try {
            return JsonParser.object().from(getStringResultFromRegexArray(html,
                    INITIAL_DATA_REGEXES, 1));
        } catch (final JsonParserException | Parser.RegexException e) {
            throw new ParsingException("Could not get ytInitialData", e);
        }
    }

    public static boolean isHardcodedClientVersionValid()
            throws IOException, ExtractionException {
        if (hardcodedClientVersionValid.isPresent()) {
            return hardcodedClientVersionValid.get();
        }
        // @formatter:off
        final byte[] body = JsonWriter.string()
                .object()
                .object("context")
                .object("client")
                .value("hl", "en-GB")
                .value("gl", "GB")
                .value("clientName", WEB_CLIENT_NAME)
                .value("clientVersion", WEB_HARDCODED_CLIENT_VERSION)
                .value("platform", DESKTOP_CLIENT_PLATFORM)
                .value("utcOffsetMinutes", 0)
                .end()
                .object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                // TODO: provide a way to enable restricted mode with:
                //  .value("enableSafetyMode", boolean)
                .value("lockedSafetyMode", false)
                .end()
                .end()
                .value("fetchLiveState", true)
                .end().done().getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final Map<String, List<String>> headers = getClientHeaders(WEB_CLIENT_ID, WEB_HARDCODED_CLIENT_VERSION);
        headers.put("Content-Type", singletonList("application/json"));

        // This endpoint is fetched by the YouTube website to get the items of its main menu and is
        // pretty lightweight (around 30kB)
        final Response response = getDownloader().post(
                YOUTUBEI_V1_URL + "guide?" + DISABLE_PRETTY_PRINT_PARAMETER,
                headers, body);
        final String responseBody = response.responseBody();
        final int responseCode = response.responseCode();

        hardcodedClientVersionValid = Optional.of(responseBody.length() > 5000
                && responseCode == 200); // Ensure to have a valid response
        return hardcodedClientVersionValid.get();
    }


    private static void extractClientVersionFromSwJs()
            throws IOException, ExtractionException {
        if (clientVersionExtracted) {
            return;
        }
        final String url = "https://www.youtube.com/sw.js";
        final Map<String, List<String>> headers = getOriginReferrerHeaders("https://www.youtube.com");
        final String response = getDownloader().get(url, headers).responseBody();
        try {
            clientVersion = getStringResultFromRegexArray(response,
                    INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
        } catch (final Parser.RegexException e) {
            throw new ParsingException("Could not extract YouTube WEB InnerTube client version "
                    + "from sw.js", e);
        }
        clientVersionExtracted = true;
    }

    private static void extractClientVersionFromHtmlSearchResultsPage()
            throws IOException, ExtractionException {
        // Don't extract the InnerTube client version if it has been already extracted
        if (clientVersionExtracted) {
            return;
        }

        // Don't provide a search term in order to have a smaller response
        final String url = "https://www.youtube.com/results?search_query=&ucbcb=1";
        final String html = getDownloader().get(url, getCookieHeader()).responseBody();
        final JsonObject initialData = getInitialData(html);
        final JsonArray serviceTrackingParams = initialData.getObject("responseContext")
                .getArray("serviceTrackingParams");

        // Try to get version from initial data first
        final Stream<JsonObject> serviceTrackingParamsStream = serviceTrackingParams.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast);

        clientVersion = getClientVersionFromServiceTrackingParam(
                serviceTrackingParamsStream, "CSI", "cver");

        if (clientVersion == null) {
            try {
                clientVersion = getStringResultFromRegexArray(html,
                        INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
            } catch (final Parser.RegexException ignored) {
            }
        }

        // Fallback to get a shortened client version which does not contain the last two
        // digits
        if (isNullOrEmpty(clientVersion)) {
            clientVersion = getClientVersionFromServiceTrackingParam(
                    serviceTrackingParamsStream, "ECATCHER", "client.version");
        }

        if (clientVersion == null) {
            throw new ParsingException(
                    "Could not extract YouTube WEB InnerTube client version from HTML search results page");
        }

        clientVersionExtracted = true;
    }

    @Nullable
    private static String getClientVersionFromServiceTrackingParam(
            @Nonnull final Stream<JsonObject> serviceTrackingParamsStream,
            @Nonnull final String serviceName,
            @Nonnull final String clientVersionKey) {
        return serviceTrackingParamsStream.filter(serviceTrackingParam ->
                        serviceTrackingParam.getString("service", "")
                                .equals(serviceName))
                .flatMap(serviceTrackingParam -> serviceTrackingParam.getArray("params")
                        .stream())
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(param -> param.getString("key", "")
                        .equals(clientVersionKey))
                .map(param -> param.getString("value"))
                .filter(paramValue -> !isNullOrEmpty(paramValue))
                .findFirst()
                .orElse(null);
    }

    /**
     * Get the client version used by YouTube website on InnerTube requests.
     */
    public static String getClientVersion() throws IOException, ExtractionException {
        if (!isNullOrEmpty(clientVersion)) {
            return clientVersion;
        }

        // Always extract the latest client version, by trying first to extract it from the
        // JavaScript service worker, then from HTML search results page as a fallback, to prevent
        // fingerprinting based on the client version used
        try {
            extractClientVersionFromSwJs();
        } catch (final Exception e) {
            extractClientVersionFromHtmlSearchResultsPage();
        }

        if (clientVersionExtracted) {
            return clientVersion;
        }

        // Fallback to the hardcoded one if it is valid
        if (isHardcodedClientVersionValid()) {
            clientVersion = WEB_HARDCODED_CLIENT_VERSION;
            return clientVersion;
        }

        throw new ExtractionException("Could not get YouTube WEB client version");
    }

    /**
     * <p>
     * <b>Only used in tests.</b>
     * </p>
     *
     * <p>
     * Quick-and-dirty solution to reset global state in between test classes.
     * </p>
     * <p>
     * This is needed for the mocks because in order to reach that state a network request has to
     * be made. If the global state is not reset and the RecordingDownloader is used,
     * then only the first test class has that request recorded. Meaning running the other
     * tests with mocks will fail, because the mock is missing.
     * </p>
     */
    public static void resetClientVersion() {
        clientVersion = null;
        clientVersionExtracted = false;
    }

    /**
     * <p>
     * <b>Only used in tests.</b>
     * </p>
     */
    public static void setNumberGenerator(final Random random) {
        numberGenerator = random;
    }

    public static boolean isHardcodedYoutubeMusicClientVersionValid() throws IOException,
            ReCaptchaException {
        final String url =
                "https://music.youtube.com/youtubei/v1/music/get_search_suggestions?"
                        + DISABLE_PRETTY_PRINT_PARAMETER;

        // @formatter:off
        final byte[] json = JsonWriter.string()
            .object()
                .object("context")
                    .object("client")
                        .value("clientName", WEB_REMIX_CLIENT_NAME)
                        .value("clientVersion", WEB_REMIX_HARDCODED_CLIENT_VERSION)
                        .value("hl", "en-GB")
                        .value("gl", "GB")
                        .value("platform", DESKTOP_CLIENT_PLATFORM)
                        .value("utcOffsetMinutes", 0)
                    .end()
                    .object("request")
                        .array("internalExperimentFlags")
                        .end()
                        .value("useSsl", true)
                    .end()
                    .object("user")
                        // TODO: provide a way to enable restricted mode with:
                        //  .value("enableSafetyMode", boolean)
                        .value("lockedSafetyMode", false)
                    .end()
                .end()
                .value("input", "")
            .end().done().getBytes(StandardCharsets.UTF_8);
        // @formatter:on

        final HashMap<String, List<String>> headers = new HashMap<>(getOriginReferrerHeaders(YOUTUBE_MUSIC_URL));
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, WEB_HARDCODED_CLIENT_VERSION));
        headers.put("Content-Type", singletonList("application/json"));

        final Response response = getDownloader().post(url, headers, json);
        // Ensure to have a valid response
        return response.responseBody().length() > 500 && response.responseCode() == 200;
    }

    public static String getYoutubeMusicClientVersion()
            throws IOException, ReCaptchaException, Parser.RegexException {
        if (!isNullOrEmpty(youtubeMusicClientVersion)) {
            return youtubeMusicClientVersion;
        }
        if (isHardcodedYoutubeMusicClientVersionValid()) {
            youtubeMusicClientVersion = WEB_REMIX_HARDCODED_CLIENT_VERSION;
            return youtubeMusicClientVersion;
        }

        try {
            final String url = "https://music.youtube.com/sw.js";
            final Map<String, List<String>> headers = getOriginReferrerHeaders(YOUTUBE_MUSIC_URL);
            final String response = getDownloader().get(url, headers).responseBody();

            youtubeMusicClientVersion = getStringResultFromRegexArray(response,
                    INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
        } catch (final Exception e) {
            final String url = "https://music.youtube.com/?ucbcb=1";
            final String html = getDownloader().get(url, getCookieHeader()).responseBody();

            youtubeMusicClientVersion = getStringResultFromRegexArray(html,
                    INNERTUBE_CONTEXT_CLIENT_VERSION_REGEXES, 1);
        }

        return youtubeMusicClientVersion;
    }

    @Nullable
    public static String getUrlFromNavigationEndpoint(@Nonnull final JsonObject navigationEndpoint)
            throws ParsingException {
        if (navigationEndpoint.has("showDialogCommand")) {
            // this case needs to be handled before the browseEndpoint,
            // e.g. for hashtags in comments
            JsonObject metadata = navigationEndpoint.getObject("showDialogCommand")
                    .getObject("panelLoadingStrategy")
                    .getObject("inlineContent")
                    .getObject("dialogViewModel")
                    .getObject("customContent")
                    .getObject("listViewModel")
                    .getArray("listItems")
                    .getObject(0)
                    .getObject("listItemViewModel")
                    .getObject("title")
                    .getArray("commandRuns")
                    .getObject(0)
                    .getObject("onTap").getObject("innertubeCommand")
                    .getObject("commandMetadata").getObject("webCommandMetadata");

            if (metadata.isEmpty()) {
                metadata = navigationEndpoint.getObject("showDialogCommand")
                        .getObject("panelLoadingStrategy")
                        .getObject("inlineContent")
                        .getObject("dialogViewModel")
                        .getObject("customContent")
                        .getObject("listViewModel")
                        .getArray("listItems")
                        .getObject(0)
                        .getObject("listItemViewModel")
                        .getObject("rendererContext")
                        .getObject("commandContext")
                        .getObject("onTap").getObject("innertubeCommand")
                        .getObject("commandMetadata").getObject("webCommandMetadata");
            }
            if (metadata.has("url")) {
                return "https://www.youtube.com" + metadata.getString("url");
            }
        }
        if (navigationEndpoint.has("webCommandMetadata")) {
            // this case needs to be handled before the browseEndpoint,
            // e.g. for hashtags in comments
            final JsonObject metadata = navigationEndpoint.getObject("webCommandMetadata");
            if (metadata.has("url")) {
                return "https://www.youtube.com" + metadata.getString("url");
            }
        }
        if (navigationEndpoint.has("urlEndpoint")) {
            String internUrl = navigationEndpoint.getObject("urlEndpoint").getString("url");
            if (internUrl.startsWith("https://www.youtube.com/redirect?")) {
                // remove https://www.youtube.com part to fall in the next if block
                internUrl = internUrl.substring(23);
            }

            if (internUrl.startsWith("/redirect?")) {
                // q parameter can be the first parameter
                internUrl = internUrl.substring(10);
                final String[] params = internUrl.split("&");
                for (final String param : params) {
                    if (param.split("=")[0].equals("q")) {
                        try {
                            return URLDecoder.decode(param.split("=")[1], UTF_8);
                        } catch (final UnsupportedEncodingException e) {
                            return null;
                        }
                    }
                }
            } else if (internUrl.startsWith("http")) {
                return internUrl;
            } else if (internUrl.startsWith("/channel") || internUrl.startsWith("/user")
                    || internUrl.startsWith("/watch")) {
                return "https://www.youtube.com" + internUrl;
            }
        } else if (navigationEndpoint.has("browseEndpoint")) {
            final JsonObject browseEndpoint = navigationEndpoint.getObject("browseEndpoint");
            final String canonicalBaseUrl = browseEndpoint.getString("canonicalBaseUrl");
            final String browseId = browseEndpoint.getString("browseId");

            if (browseId != null) {
                if (browseId.startsWith("UC")) {
                    // All channel IDs are prefixed with UC
                    return "https://www.youtube.com/channel/" + browseId;
                } else if (browseId.startsWith("VL")) {
                    // All playlist IDs are prefixed with VL, which needs to be removed from the
                    // playlist ID
                    return "https://www.youtube.com/playlist?list=" + browseId.substring(2);
                }
            }

            if (!isNullOrEmpty(canonicalBaseUrl)) {
                return "https://www.youtube.com" + canonicalBaseUrl;
            }
        }

        if (navigationEndpoint.has("watchEndpoint")) {
            final StringBuilder url = new StringBuilder();
            url.append("https://www.youtube.com/watch?v=")
                    .append(navigationEndpoint.getObject("watchEndpoint")
                            .getString(VIDEO_ID));
            if (navigationEndpoint.getObject("watchEndpoint").has("playlistId")) {
                url.append("&list=").append(navigationEndpoint.getObject("watchEndpoint")
                        .getString("playlistId"));
            }
            if (navigationEndpoint.getObject("watchEndpoint").has("startTimeSeconds")) {
                url.append("&t=")
                        .append(navigationEndpoint.getObject("watchEndpoint")
                        .getInt("startTimeSeconds"));
            }
            return url.toString();
        }

        if (navigationEndpoint.has("watchPlaylistEndpoint")) {
            return "https://www.youtube.com/playlist?list="
                    + navigationEndpoint.getObject("watchPlaylistEndpoint")
                    .getString("playlistId");
        }

        if (navigationEndpoint.has("commandMetadata")) {
            final JsonObject metadata = navigationEndpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata");
            if (metadata.has("url")) {
                return "https://www.youtube.com" + metadata.getString("url");
            }
        }

        return null;
    }

    /**
     * Get the text from a JSON object that has either a {@code simpleText} or a {@code runs}
     * array.
     *
     * @param textObject JSON object to get the text from
     * @param html       whether to return HTML, by parsing the {@code navigationEndpoint}
     * @return text in the JSON object or {@code null}
     */
    @Nullable
    public static String getTextFromObject(final JsonObject textObject, final boolean html)
            throws ParsingException {
        if (isNullOrEmpty(textObject)) {
            return null;
        }

        if (textObject.has("simpleText")) {
            return textObject.getString("simpleText");
        }

        if (textObject.getArray("runs").isEmpty()) {
            return null;
        }

        final StringBuilder textBuilder = new StringBuilder();
        for (final Object o : textObject.getArray("runs")) {
            final JsonObject run = (JsonObject) o;
            String text = run.getString("text");

            if (html) {
                text = Entities.escape(text);
                if (run.has("navigationEndpoint")) {
                    final String url = getUrlFromNavigationEndpoint(run
                            .getObject("navigationEndpoint"));
                    if (!isNullOrEmpty(url)) {
                        text = "<a href=\"" + url + "\">" + text + "</a>";
                    }
                }

                final boolean bold = run.has("bold")
                        && run.getBoolean("bold");
                final boolean italic = run.has("italics")
                        && run.getBoolean("italics");
                final boolean strikethrough = run.has("strikethrough")
                        && run.getBoolean("strikethrough");

                if (bold) {
                    textBuilder.append("<b>");
                }
                if (italic) {
                    textBuilder.append("<i>");
                }
                if (strikethrough) {
                    textBuilder.append("<s>");
                }

                textBuilder.append(text);

                if (strikethrough) {
                    textBuilder.append("</s>");
                }
                if (italic) {
                    textBuilder.append("</i>");
                }
                if (bold) {
                    textBuilder.append("</b>");
                }
            } else {
                textBuilder.append(text);
            }
        }

        String text = textBuilder.toString();

        if (html) {
            text = text.replaceAll("\\n", "<br>");
            text = text.replaceAll(" {2}", " &nbsp;");
        }

        return text;
    }

    @Nullable
    public static String getTextFromObject(final JsonObject textObject) throws ParsingException {
        return getTextFromObject(textObject, false);
    }

    @Nullable
    public static String getTextAtKey(@Nonnull final JsonObject jsonObject, final String theKey)
            throws ParsingException {
        if (jsonObject.isString(theKey)) {
            return jsonObject.getString(theKey);
        } else {
            return getTextFromObject(jsonObject.getObject(theKey));
        }
    }

    public static String fixThumbnailUrl(@Nonnull final String thumbnailUrl) {
        String result = thumbnailUrl;
        if (result.startsWith("//")) {
            result = result.substring(2);
        }

        if (result.startsWith(HTTP)) {
            result = Utils.replaceHttpWithHttps(result);
        } else if (!result.startsWith(HTTPS)) {
            result = "https://" + result;
        }

        return result;
    }

    public static String getThumbnailUrlFromInfoItem(final JsonObject infoItem)
            throws ParsingException {
        // TODO: Don't simply get the first item, but look at all thumbnails and their resolution
        try {
            JsonArray thumbnails = infoItem.getObject("thumbnail").getArray("thumbnails");
            return fixThumbnailUrl(thumbnails.getObject(thumbnails.size() - 1).getString("url"));
        } catch (final Exception e) {
            // lockupViewModel format
            try {
                JsonArray thumbnails = infoItem.getObject("contentImage")
                        .getObject("thumbnailViewModel")
                        .getObject("image")
                        .getArray("sources");
                return fixThumbnailUrl(thumbnails.getObject(thumbnails.size() - 1).getString("url"));
            } catch (final Exception e2) {
                throw new ParsingException("Could not get thumbnail url", e2);
            }
        }
    }

    /**
     * Get images from a YouTube {@code thumbnails} {@link JsonArray}.
     *
     * <p>
     * The properties of the {@link Image}s created will be set using the corresponding ones of
     * thumbnail items.
     * </p>
     *
     * @param thumbnails a YouTube {@code thumbnails} {@link JsonArray}
     * @return an unmodifiable list of {@link Image}s extracted from the given {@link JsonArray}
     */
    @Nonnull
    public static List<Image> getImagesFromThumbnailsArray(
            @Nonnull final JsonArray thumbnails) {
        return thumbnails.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .filter(thumbnail -> !isNullOrEmpty(thumbnail.getString("url")))
                .map(thumbnail -> {
                    final int height = thumbnail.getInt("height", Image.HEIGHT_UNKNOWN);
                    return new Image(fixThumbnailUrl(thumbnail.getString("url")),
                            height,
                            thumbnail.getInt("width", Image.WIDTH_UNKNOWN),
                            Image.ResolutionLevel.fromHeight(height));
                })
                .collect(Collectors.toList());
    }

    @Nonnull
    public static String getValidJsonResponseBody(@Nonnull final Response response)
            throws ParsingException, MalformedURLException {
        if (response.responseCode() == 404) {
            throw new ContentNotAvailableException("Not found"
                    + " (\"" + response.responseCode() + " " + response.responseMessage() + "\")");
        }

        final String responseBody = response.responseBody();
        if (responseBody.length() < 50) { // Ensure to have a valid response
            throw new ParsingException("JSON response is too short");
        }

        // Check if the request was redirected to the error page.
        final URL latestUrl = new URL(response.latestUrl());
        if (latestUrl.getHost().equalsIgnoreCase("www.youtube.com")) {
            final String path = latestUrl.getPath();
            if (path.equalsIgnoreCase("/oops") || path.equalsIgnoreCase("/error")) {
                throw new ContentNotAvailableException("Content unavailable");
            }
        }

        final String responseContentType = response.getHeader("Content-Type");
        if (responseContentType != null
                && responseContentType.toLowerCase().contains("text/html")) {
            throw new ParsingException("Got HTML document, expected JSON response"
                    + " (latest url was: \"" + response.latestUrl() + "\")");
        }

        return responseBody;
    }

    public static JsonObject getJsonPostResponse(@Nonnull final String endpoint,
                                                 final byte[] body,
                                                 @Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = getYouTubeHeaders();
        headers.put("Content-Type", singletonList("application/json"));

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().post(YOUTUBEI_V1_URL + endpoint + "?"
                        + DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization)));
    }

    public static JsonObject getJsonPostResponse(@Nonnull final String endpoint,
                                                 @Nonnull final List<String> queryParameters,
                                                 final byte[] body,
                                                 @Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = getYouTubeHeaders();
        headers.put("Content-Type", singletonList("application/json"));

        final String queryParametersString;
        if (queryParameters.isEmpty()) {
            queryParametersString = "?" + DISABLE_PRETTY_PRINT_PARAMETER;
        } else {
            queryParametersString = "?" + String.join("&", queryParameters)
                    + "&" + DISABLE_PRETTY_PRINT_PARAMETER;
        }

        return JsonUtils.toJsonObject(getValidJsonResponseBody(
                getDownloader().post(YOUTUBEI_V1_URL + endpoint
                        + queryParametersString, headers, body, localization)));
    }

    public static CancellableCall getJsonPostResponseAsync(final String endpoint,
                                                final byte[] body,
                                                final Localization localization,
                                                final Downloader.AsyncCallback callback)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));

        return getDownloader().postAsync(YOUTUBEI_V1_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization, callback);
    }

    public static JsonObject getLoggedJsonPostResponse(final String endpoint,
                                                 final byte[] body,
                                                 final Localization localization)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));

        addLoggedInHeaders(headers);

        final Response response = getDownloader().post(YOUTUBEI_V1_URL + endpoint + "?" +
                DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization);

        return JsonUtils.toJsonObject(getValidJsonResponseBody(response));
    }

    public static CancellableCall getLoggedJsonPostResponseAsync(final String endpoint,
                                                                 final byte[] body,
                                                                 final Localization localization,
                                                                 final Downloader.AsyncCallback callback)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList("Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/91.0.4472.101 Safari/537.36"));

        addLoggedInHeaders(headers);

        return getDownloader().postAsync(YOUTUBEI_V1_URL + endpoint + "?" +  DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization, callback);
    }


    public static String getJsonPostResponseRaw(final String endpoint,
                                                 final byte[] body,
                                                 final Localization localization)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));

        final Response response = getDownloader().post(YOUTUBEI_V1_URL + endpoint + "?" +
                DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization);

        return getValidJsonResponseBody(response);
    }


    public static JsonObject getJsonAndroidPostResponse(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest) throws IOException, ExtractionException {
        return getMobilePostResponse(endpoint, body, localization,
                getAndroidUserAgent(localization), ANDROID_YOUTUBE_KEY, endPartOfUrlRequest);
    }

    public static CancellableCall getJsonAndroidPostResponseAsync(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest,
            final Downloader.AsyncCallback callback) throws IOException, ExtractionException {
        return getMobilePostResponseAsync(endpoint, body, localization,
                getAndroidUserAgent(localization), ANDROID_YOUTUBE_KEY, endPartOfUrlRequest, callback);
    }

    public static JsonObject getJsonAndroidVRPostResponse(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest) throws IOException, ExtractionException {
        return getMobilePostResponse(endpoint, body, localization,
                getAndroidVRUserAgent(localization), ANDROID_VR_YOUTUBE_KEY, endPartOfUrlRequest);
    }

    public static CancellableCall getJsonAndroidVRPostResponseAsync(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest,
            final Downloader.AsyncCallback callback) throws IOException, ExtractionException {
        return getMobilePostResponseAsync(endpoint, body, localization,
                getAndroidVRUserAgent(localization), ANDROID_VR_YOUTUBE_KEY, endPartOfUrlRequest, callback);
    }

    public static JsonObject getJsonIosPostResponse(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest) throws IOException, ExtractionException {
        return getMobilePostResponse(endpoint, body, localization, getIosUserAgent(localization),
                IOS_YOUTUBE_KEY, endPartOfUrlRequest);
    }

    public static CancellableCall getJsonIosPostResponseAsync(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nullable final String endPartOfUrlRequest,
            final Downloader.AsyncCallback callback) throws IOException, ExtractionException {
        return getMobilePostResponseAsync(endpoint, body, localization, getIosUserAgent(localization),
                IOS_YOUTUBE_KEY, endPartOfUrlRequest, callback);
    }

    private static JsonObject getMobilePostResponse(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nonnull final String userAgent,
            @Nonnull final String innerTubeApiKey,
            @Nullable final String endPartOfUrlRequest) throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(userAgent));
        headers.put("X-Goog-Api-Format-Version", singletonList("2"));

        final String baseEndpointUrl = YOUTUBEI_V1_GAPIS_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        final Response response = getDownloader().post(isNullOrEmpty(endPartOfUrlRequest)
                        ? baseEndpointUrl : baseEndpointUrl + endPartOfUrlRequest,
                headers, body, localization);
        return JsonUtils.toJsonObject(getValidJsonResponseBody(response));
    }

    private static CancellableCall getMobilePostResponseAsync(
            final String endpoint,
            final byte[] body,
            @Nonnull final Localization localization,
            @Nonnull final String userAgent,
            @Nonnull final String innerTubeApiKey,
            @Nullable final String endPartOfUrlRequest,
            final Downloader.AsyncCallback callback) throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(userAgent));
        headers.put("X-Goog-Api-Format-Version", singletonList("2"));

        final String baseEndpointUrl = YOUTUBEI_V1_GAPIS_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER;

        return getDownloader().postAsync(isNullOrEmpty(endPartOfUrlRequest)
                        ? baseEndpointUrl : baseEndpointUrl + endPartOfUrlRequest,
                headers, body, localization, callback);
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareDesktopJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry) throws IOException, ExtractionException {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                .object("client")
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("clientName", WEB_CLIENT_NAME)
                .value("clientVersion", getClientVersion())
                .value("originalUrl", "https://www.youtube.com")
                .value("platform", DESKTOP_CLIENT_PLATFORM)
                .value("utcOffsetMinutes", 0)
                .end()
                .object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                // TODO: provide a way to enable restricted mode with:
                //  .value("enableSafetyMode", boolean)
                .value("lockedSafetyMode", false)
                .end()
                .end();
        // @formatter:on
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareAndroidMobileJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String visitorData) {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                .object("client")
                .value("clientName", "ANDROID")
                .value("clientVersion", ANDROID_YOUTUBE_CLIENT_VERSION)
                .value("clientScreen", "WATCH")
                .value("platform", "MOBILE")
                .value("osName", "Android")
                .value("osVersion", "16")
                .value("visitorData", visitorData)
                /*
                A valid Android SDK version is required to be sure to get a valid player
                response
                If this parameter is not provided, the player response is replaced by an
                error saying the message "The following content is not available on this
                app. Watch this content on the latest version on YouTube" (it was
                previously a 5-minute video with this message)
                See https://github.com/TeamNewPipe/NewPipe/issues/8713
                The Android SDK version corresponding to the Android version used in
                requests is sent
                */
                .value("androidSdkVersion", 36)
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("utcOffsetMinutes", 0)
                .end()
                .object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                // TODO: provide a way to enable restricted mode with:
                //  .value("enableSafetyMode", boolean)
                .value("lockedSafetyMode", false)
                .end()
                .end();
        // @formatter:on
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareAndroidVRJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String visitorData) {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                .object("client")
                .value("clientName", "ANDROID_VR")
                .value("clientVersion", ANDROID_VR_YOUTUBE_CLIENT_VERSION)
                .value("deviceMake", "Oculus")
                .value("deviceModel", "Quest 3")
                .value("clientScreen", "WATCH")
                .value("platform", "MOBILE")
                .value("osName", "Android")
                .value("osVersion", "12L")
                .value("visitorData", visitorData)
                .value("androidSdkVersion", 32)
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("utcOffsetMinutes", 0)
                .end()
                .object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                .value("lockedSafetyMode", false)
                .end()
                .end();
        // @formatter:on
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareIosMobileJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry) {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                .object("client")
                .value("clientName", "IOS")
                .value("clientVersion", IOS_YOUTUBE_CLIENT_VERSION)
                .value("deviceMake",  "Apple")
                // Device model is required to get 60fps streams
                .value("deviceModel", IOS_DEVICE_MODEL)
                .value("platform", "MOBILE")
                .value("osName", "iOS")
                /*
                The value of this field seems to use the following structure:
                "iOS major version.minor version.patch version.build version", where
                "patch version" is equal to 0 if it isn't set
                The build version corresponding to the iOS version used can be found on
                https://theapplewiki.com/wiki/Firmware/iPhone/17.x#iPhone_15
                 */
                .value("osVersion", IOS_OS_VERSION)
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("utcOffsetMinutes", 0)
                .end()
                .object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                // TODO: provide a way to enable restricted mode with:
                //  .value("enableSafetyMode", boolean)
                .value("lockedSafetyMode", false)
                .end()
                .end();
        // @formatter:on
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareWebJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                    .object("client")
                        .value("clientName", "WEB")
                        .value("clientVersion", WEB_CLIENT_VERSION)
                        .value("hl", localization.getLocalizationCode())
                        .value("gl", contentCountry.getCountryCode())
                        .value("utcOffsetMinutes", 0)
                    .end()
                .end();
        // @formatter:on
    }

    public static JsonBuilder<JsonObject> prepareTvHtml5EmbedJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId) {
        // @formatter:off
        return JsonObject.builder()
                .object("context")
                .object("client")
                .value("clientName", "TVHTML5")
                .value("clientVersion", TVHTML5_SIMPLY_EMBED_CLIENT_VERSION)
                .value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("utcOffsetMinutes", 0)
                .end()
                .end();
        // @formatter:on
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareSafariJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry) {
        return JsonObject.builder()
                .object("context")
                    .object("client")
                        .value("utcOffsetMinutes", 0)
                        .value("timeZone", "UTC")
                        .value("hl", localization.getLocalizationCode())
                        .value("gl", contentCountry.getCountryCode())
                        .value("userAgent", SAFARI_USER_AGENT)
                        .value("clientName", "WEB")
                        .value("clientVersion", SAFARI_CLIENT_VERSION)
                    .end()
                .end();
    }

    @Nonnull
    public static byte[] createSafariPlayerBody(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final Integer sts,
            @Nonnull final String contentPlaybackNonce) {
        return JsonWriter.string(
                        prepareSafariJsonBuilder(localization, contentCountry)
                                .object("playbackContext")
                                    .object("contentPlaybackContext")
                                        .value("html5Preference", "HTML5_PREF_WANTS")
                                        .value("signatureTimestamp", sts)
                                    .end()
                                .end()
                                .value(CPN, contentPlaybackNonce)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
    }

    @Nonnull
    public static JsonBuilder<JsonObject> prepareJsonPlayerBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String clientName,
            @Nonnull final String userAgent)
            throws IOException, ExtractionException {
        return JsonObject.builder()
                .object("context")
                    .object("client")
                        .value("utcOffsetMinutes", 0)
                        .value("timeZone", "UTC")
                        .value("hl", localization.getLocalizationCode())
                        .value("gl", contentCountry.getCountryCode())
                        .value("userAgent", userAgent)
                        .value("clientName", clientName)
                        .value("clientVersion", getClientVersion())
                    .end()
                .end();
    }

    @Nonnull
    public static byte[] createJsonPlayerBody(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final Integer sts,
            @Nonnull final String contentPlaybackNonce,
            @Nonnull final String clientName,
            @Nonnull final String userAgent)
            throws IOException, ExtractionException {
        return JsonWriter.string(
                        prepareJsonPlayerBuilder(localization, contentCountry,
                                clientName, userAgent)
                                .object("playbackContext")
                                    .object("contentPlaybackContext")
                                        .value("html5Preference", "HTML5_PREF_WANTS")
                                        .value("signatureTimestamp", sts)
                                    .end()
                                .end()
                                .value(CPN, contentPlaybackNonce)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
    }

    public static CancellableCall getSafariPostResponseAsync(
            final String endpoint,
            final byte[] body,
            final Localization localization,
            final Downloader.AsyncCallback callback) throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(SAFARI_USER_AGENT));
        headers.put("X-YouTube-Client-Name", singletonList("1"));
        headers.put("X-Youtube-Client-Version", singletonList(SAFARI_CLIENT_VERSION));
        addLoggedInHeaders(headers);
        return getDownloader().postAsync(
                YOUTUBEI_V1_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER,
                headers,
                body,
                localization,
                callback);
    }

    public static CancellableCall getJsonPlayerResponseAsync(final String endpoint,
                                                             final byte[] body,
                                                             final Localization localization,
                                                             final String clientId,
                                                             final String userAgent,
                                                             final Downloader.AsyncCallback callback)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(userAgent));
        headers.put("X-YouTube-Client-Name", singletonList(clientId));
        headers.put("X-Youtube-Client-Version", singletonList(getClientVersion()));

        addLoggedInHeaders(headers);

        return getDownloader().postAsync(YOUTUBEI_V1_URL + endpoint + "?" + DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization, callback);
    }

    public static CancellableCall getJsonPlayerResponseAsync(final String endpoint,
                                                             final byte[] body,
                                                             final Localization localization,
                                                             final String clientId,
                                                             final String clientVersion,
                                                             final String userAgent,
                                                             final Downloader.AsyncCallback callback)
            throws IOException, ExtractionException {
        final Map<String, List<String>> headers = new HashMap<>();
        headers.put("Content-Type", singletonList("application/json"));
        headers.put("User-Agent", singletonList(userAgent));
        headers.put("X-YouTube-Client-Name", singletonList(clientId));
        headers.put("X-Youtube-Client-Version", singletonList(clientVersion));
        addLoggedInHeaders(headers);
        return getDownloader().postAsync(YOUTUBEI_V1_URL + endpoint + "?"
                + DISABLE_PRETTY_PRINT_PARAMETER, headers, body, localization, callback);
    }

    public static Response getWebPlayerResponseSync(@Nonnull final String videoId)
            throws IOException, ExtractionException {
        Localization localization = new Localization("en");
        final byte[] body = JsonWriter.string(
                        prepareDesktopJsonBuilder(localization, ContentCountry.DEFAULT)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
        final String url = YOUTUBEI_V1_URL + "player" + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&$fields=microformat,playabilityStatus,storyboards,videoDetails";

        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));
        addLoggedInHeaders(headers);
        return getDownloader().post(url, headers, body, localization);
    }

    public static CancellableCall getWebPlayerResponse(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            final YoutubeStreamExtractor streamExtractor) throws IOException, ExtractionException {
        final byte[] body = JsonWriter.string(
                        prepareDesktopJsonBuilder(localization, contentCountry)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
        final String url = YOUTUBEI_V1_URL + "player" + "?" + DISABLE_PRETTY_PRINT_PARAMETER
                + "&$fields=microformat,playabilityStatus,storyboards,videoDetails";

        final Map<String, List<String>> headers = new HashMap<>();
        addYoutubeHeaders(headers);
        headers.put("Content-Type", singletonList("application/json"));
        addLoggedInHeaders(headers);
        return getDownloader().postAsync(
                url, headers, body, localization, new Downloader.AsyncCallback() {
                    @Override
                    public void onSuccess(Response response) throws ExtractionException {
                        JsonObject webPlayerResponse;
                        try {
                            webPlayerResponse = JsonUtils.toJsonObject(getValidJsonResponseBody(response));
                            if (Objects.equals(webPlayerResponse.getObject("playabilityStatus").getString("status"), "LOGIN_REQUIRED")) {
                                final JsonObject playabilityStatus = webPlayerResponse.getObject("playabilityStatus");
                                final String reason = playabilityStatus.getString("reason");
                                if (reason != null && (reason.contains("age") || reason.contains("inappropriate"))) {
                                    throw new AgeRestrictedContentException("This age-restricted video cannot be watched anonymously");
                                }
                                if (playabilityStatus.has("desktopLegacyAgeGateReason")) {
                                    throw new AgeRestrictedContentException("This age-restricted video cannot be watched anonymously");
                                }
                                throw new AntiBotException(reason);
                            }
                            if (isPlayerResponseNotValid(webPlayerResponse, videoId)) {
                                throw new ExtractionException("Initial WEB player response is not valid");
                            }
//                            // Save the playerResponse from the player endpoint of the desktop internal API because
//                            // the web endpoint may return UNPLAYABLE due to blocking, but metadata is still usable.
//                            streamExtractor.playerResponse = webPlayerResponse;
//
                            // The microformat JSON object of the content is only returned on the WEB client,
                            // so we need to store it instead of getting it directly from the playerResponse
                            streamExtractor.playerMicroFormatRenderer = webPlayerResponse.getObject("microformat")
                                    .getObject("playerMicroformatRenderer");

                           streamExtractor.watchDataCache.startAt = streamExtractor.getStartAt();
                        } catch (Exception e) {
                            e.printStackTrace();
                            streamExtractor.addError(e);
                        }
                    }

                    @Override
                    public void onError(final Exception error) {
                        streamExtractor.addError(error);
                    }
                });
    }

    @Nonnull
    public static byte[] createTvHtml5EmbedPlayerBody(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final Integer sts,
            @Nonnull final String contentPlaybackNonce) {
        // @formatter:off
        return JsonWriter.string(
                        prepareTvHtml5EmbedJsonBuilder(localization, contentCountry, videoId)
                                .object("playbackContext")
                                .object("contentPlaybackContext")
                                // Signature timestamp from the JavaScript base player is needed to get
                                // working obfuscated URLs
                                .value("signatureTimestamp", sts)
                                .value("html5Preference", "HTML5_PREF_WANTS")
                                .end()
                                .end()
                                .value(CPN, contentPlaybackNonce)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
        // @formatter:on
    }

    @Nonnull
    public static byte[] createWebEmbedPlayerBody(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final String videoId,
            @Nonnull final Integer sts,
            @Nonnull final String contentPlaybackNonce) {
        // @formatter:off
        return JsonWriter.string(
                        prepareWebJsonBuilder(localization, contentCountry, videoId)
                                .object("playbackContext")
                                .object("contentPlaybackContext")
                                // Signature timestamp from the JavaScript base player is needed to get
                                // working obfuscated URLs
                                .value("signatureTimestamp", sts)
                                .value("html5Preference", "HTML5_PREF_WANTS")
                                .end()
                                .end()
                                .value(CPN, contentPlaybackNonce)
                                .value(VIDEO_ID, videoId)
                                .value(CONTENT_CHECK_OK, true)
                                .value(RACY_CHECK_OK, true)
                                .done())
                .getBytes(StandardCharsets.UTF_8);
        // @formatter:on
    }


    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the Android
     * client.
     *
     * <p>
     * If the {@link Localization} provided is {@code null}, fallbacks to
     * {@link Localization#DEFAULT the default one}.
     * </p>
     *
     * @param localization the {@link Localization} to set in the user-agent
     * @return the Android user-agent used for InnerTube requests with the Android client,
     * depending on the {@link Localization} provided
     */
    @Nonnull
    public static String getAndroidUserAgent(@Nullable final Localization localization) {
        // Spoofing an Android 14 device with the hardcoded version of the Android app
        return "com.google.android.youtube/" + ANDROID_YOUTUBE_CLIENT_VERSION
                + " (Linux; U; Android 15; "
                + (localization != null ? localization : Localization.DEFAULT).getCountryCode()
                + ") gzip";
    }

    @Nonnull
    public static String getAndroidVRUserAgent(@Nullable final Localization localization) {
        return "com.google.android.apps.youtube.vr.oculus/" + ANDROID_VR_YOUTUBE_CLIENT_VERSION
                + " (Linux; U; Android 12L; eureka-user Build/SQ3A.220605.009.A1) gzip";
    }

    /**
     * Get the user-agent string used as the user-agent for InnerTube requests with the iOS
     * client.
     *
     * <p>
     * If the {@link Localization} provided is {@code null}, fallbacks to
     * {@link Localization#DEFAULT the default one}.
     * </p>
     *
     * @param localization the {@link Localization} to set in the user-agent
     * @return the iOS user-agent used for InnerTube requests with the iOS client, depending on the
     * {@link Localization} provided
     */
    @Nonnull
    public static String getIosUserAgent(@Nullable final Localization localization) {
        // Spoofing an iPhone 15 running iOS 17.5.1 with the hardcoded version of the iOS app
        return "com.google.ios.youtube/" + IOS_YOUTUBE_CLIENT_VERSION
                + "(" + IOS_DEVICE_MODEL + "; U; CPU iOS "
                + IOS_USER_AGENT_VERSION + " like Mac OS X; "
                + (localization != null ? localization : Localization.DEFAULT).getCountryCode()
                + ")";
    }


    /**
     * Add the <code>X-YouTube-Client-Name</code>, <code>X-YouTube-Client-Version</code>,
     * <code>Origin</code>, and <code>Referer</code> headers.
     * @param headers The headers which should be completed
     */
    public static void addYoutubeHeaders(@Nonnull final Map<String, List<String>> headers)
            throws IOException, ExtractionException {

        getYouTubeHeaders().forEach((key, value) ->
                headers.merge(key, value, (existingList, newList) -> {
                    List<String> mergedList = new ArrayList<>(existingList);
                    mergedList.addAll(newList);
                    return mergedList;
                })
        );
    }

    public static void addLoggedInHeaders(@Nonnull final Map<String, List<String>> headers) throws ExtractionException {
        if(ServiceList.YouTube.hasTokens()) {
            headers.put("Cookie", singletonList(ServiceList.YouTube.getTokens()));
            try {
                headers.put("Authorization", singletonList(getAuthorizationHeader(ServiceList.YouTube.getTokens())));
            } catch (Exception e) {
                throw new ExtractionException("Failed to get authorization header", e);
            }
            headers.put("X-Origin", singletonList("https://www.youtube.com"));
            headers.put("DNT", singletonList("1"));
        }
    }

    /**
     * Returns a {@link Map} containing the required YouTube Music headers.
     */
    @Nonnull
    public static Map<String, List<String>> getYoutubeMusicHeaders() {
        final HashMap<String, List<String>> headers = new HashMap<>(getOriginReferrerHeaders(YOUTUBE_MUSIC_URL));
        headers.putAll(getClientHeaders(WEB_REMIX_CLIENT_ID, youtubeMusicClientVersion));
        return headers;
    }

    /**
     * Returns a {@link Map} containing the required YouTube headers, including the
     * <code>CONSENT</code> cookie to prevent redirects to <code>consent.youtube.com</code>
     */
    public static Map<String, List<String>> getYouTubeHeaders()
            throws ExtractionException, IOException {
        final Map<String, List<String>> headers = getClientInfoHeaders();
        headers.put("Cookie", List.of(generateConsentCookie()));
        return headers;
    }


    /**
     * Returns a {@link Map} containing the {@code X-YouTube-Client-Name},
     * {@code X-YouTube-Client-Version}, {@code Origin}, and {@code Referer} headers.
     */
    public static Map<String, List<String>> getClientInfoHeaders()
            throws ExtractionException, IOException {
        final HashMap<String, List<String>> headers = new HashMap<>(getOriginReferrerHeaders("https://www.youtube.com"));
        headers.putAll(getClientHeaders(WEB_CLIENT_ID, getClientVersion()));
        return headers;
    }

    /**
     * Returns an unmodifiable {@link Map} containing the {@code Origin} and {@code Referer}
     * headers set to the given URL.
     *
     * @param url The URL to be set as the origin and referrer.
     */
    public static Map<String, List<String>> getOriginReferrerHeaders(@Nonnull final String url) {
        final List<String> urlList = List.of(url);
        return Map.of("Origin", urlList, "Referer", urlList);
    }

    /**
     * Returns an unmodifiable {@link Map} containing the {@code X-YouTube-Client-Name} and
     * {@code X-YouTube-Client-Version} headers.
     *
     * @param name The X-YouTube-Client-Name value.
     * @param version X-YouTube-Client-Version value.
     */
    static Map<String, List<String>> getClientHeaders(@Nonnull final String name,
                                                      @Nonnull final String version) {
        return Map.of("X-YouTube-Client-Name", List.of(name),
                "X-YouTube-Client-Version", List.of(version));
    }

    /**
     * Create a map with the required cookie header.
     * @return A singleton map containing the header.
     */
    public static Map<String, List<String>> getCookieHeader() {
        return Collections.singletonMap("Cookie", singletonList(generateConsentCookie()));
    }

    /**
     * Add the <code>CONSENT</code> cookie to prevent redirect to <code>consent.youtube.com</code>
     * @param headers the headers which should be completed
     */
    public static void addCookieHeader(@Nonnull final Map<String, List<String>> headers) {
        if (headers.get("Cookie") == null) {
            headers.put("Cookie", Collections.singletonList(generateConsentCookie()));
        } else {
            headers.get("Cookie").add(generateConsentCookie());
        }
    }

    @Nonnull
    public static String generateConsentCookie() {
        return "SOCS=" + (isConsentAccepted()
                // CAISAiAD means that the user configured manually cookies YouTube, regardless of
                // the consent values
                // This value surprisingly allows to extract mixes and some YouTube Music playlists
                // in the same way when a user allows all cookies
                ? "CAISAiAD"
                // CAE= means that the user rejected all non-necessary cookies with the "Reject
                // all" button on the consent page
                : "CAE=");
    }

    public static String extractCookieValue(final String cookieName,
                                            @Nonnull final Response response) {
        final List<String> cookies = response.responseHeaders().get("set-cookie");
        if (cookies == null) {
            return "";
        }

        String result = "";
        for (final String cookie : cookies) {
            final int startIndex = cookie.indexOf(cookieName);
            if (startIndex != -1) {
                result = cookie.substring(startIndex + cookieName.length() + "=".length(),
                        cookie.indexOf(";", startIndex));
            }
        }
        return result;
    }

    /**
     * Shared alert detection function, multiple endpoints return the error similarly structured.
     * <p>
     * Will check if the object has an alert of the type "ERROR".
     * </p>
     *
     * @param initialData the object which will be checked if an alert is present
     * @throws ContentNotAvailableException if an alert is detected
     */
    public static void defaultAlertsCheck(@Nonnull final JsonObject initialData)
            throws ParsingException {
        final JsonArray alerts = initialData.getArray("alerts");
        if (!isNullOrEmpty(alerts)) {
            final JsonObject alertRenderer = alerts.getObject(0).getObject("alertRenderer");
            final String alertText = getTextFromObject(alertRenderer.getObject("text"));
            final String alertType = alertRenderer.getString("type", "");
            if (alertType.equalsIgnoreCase("ERROR")) {
                if (alertText != null
                        && (alertText.contains("This account has been terminated")
                        || alertText.contains("This channel was removed"))) {
                    if (alertText.matches(".*violat(ed|ion|ing).*")
                            || alertText.contains("infringement")) {
                        // Possible error messages:
                        // "This account has been terminated for a violation of YouTube's Terms of
                        //     Service."
                        // "This account has been terminated due to multiple or severe violations of
                        //     YouTube's policy prohibiting hate speech."
                        // "This account has been terminated due to multiple or severe violations of
                        //     YouTube's policy prohibiting content designed to harass, bully or
                        //     threaten."
                        // "This account has been terminated due to multiple or severe violations
                        //     of YouTube's policy against spam, deceptive practices and misleading
                        //     content or other Terms of Service violations."
                        // "This account has been terminated due to multiple or severe violations of
                        //     YouTube's policy on nudity or sexual content."
                        // "This account has been terminated for violating YouTube's Community
                        //     Guidelines."
                        // "This account has been terminated because we received multiple
                        //     third-party claims of copyright infringement regarding material that
                        //     the user posted."
                        // "This account has been terminated because it is linked to an account that
                        //     received multiple third-party claims of copyright infringement."
                        // "This channel was removed because it violated our Community Guidelines."
                        throw new AccountTerminatedException(alertText,
                                AccountTerminatedException.Reason.VIOLATION);
                    } else {
                        throw new AccountTerminatedException(alertText);
                    }
                }
                throw new ContentNotAvailableException("Got error: \"" + alertText + "\"");
            }
        }
    }

    @Nonnull
    public static List<MetaInfo> getMetaInfo(@Nonnull final JsonArray contents)
            throws ParsingException {
        final List<MetaInfo> metaInfo = new ArrayList<>();
        for (final Object content : contents) {
            final JsonObject resultObject = (JsonObject) content;
            if (resultObject.has("itemSectionRenderer")) {
                for (final Object sectionContentObject
                        : resultObject.getObject("itemSectionRenderer").getArray("contents")) {

                    final JsonObject sectionContent = (JsonObject) sectionContentObject;
                    if (sectionContent.has("infoPanelContentRenderer")) {
                        metaInfo.add(getInfoPanelContent(sectionContent
                                .getObject("infoPanelContentRenderer")));
                    }
                    if (sectionContent.has("clarificationRenderer")) {
                        metaInfo.add(getClarificationRendererContent(sectionContent
                                .getObject("clarificationRenderer")
                        ));
                    }

                }
            }
        }
        return metaInfo;
    }

    @Nonnull
    private static MetaInfo getInfoPanelContent(@Nonnull final JsonObject infoPanelContentRenderer)
            throws ParsingException {
        final MetaInfo metaInfo = new MetaInfo();
        final StringBuilder sb = new StringBuilder();
        for (final Object paragraph : infoPanelContentRenderer.getArray("paragraphs")) {
            if (sb.length() != 0) {
                sb.append("<br>");
            }
            sb.append(YoutubeParsingHelper.getTextFromObject((JsonObject) paragraph));
        }
        metaInfo.setContent(new Description(sb.toString(), Description.HTML));
        if (infoPanelContentRenderer.has("sourceEndpoint")) {
            final String metaInfoLinkUrl = YoutubeParsingHelper.getUrlFromNavigationEndpoint(
                    infoPanelContentRenderer.getObject("sourceEndpoint"));
            try {
                metaInfo.addUrl(new URL(Objects.requireNonNull(extractCachedUrlIfNeeded(
                        metaInfoLinkUrl))));
            } catch (final NullPointerException | MalformedURLException e) {
                throw new ParsingException("Could not get metadata info URL", e);
            }

            String metaInfoLinkText = YoutubeParsingHelper.getTextFromObject(
                    infoPanelContentRenderer.getObject("inlineSource"));
            if (isNullOrEmpty(metaInfoLinkText)) {
                metaInfoLinkText = YoutubeParsingHelper.getTextFromObject(
                        infoPanelContentRenderer.getObject("disclaimer"));
            }
            if (isNullOrEmpty(metaInfoLinkText)) {
                throw new ParsingException("Could not get metadata info link text.");
            }
            metaInfo.addUrlText(metaInfoLinkText);
        }

        return metaInfo;
    }

    @Nonnull
    private static MetaInfo getClarificationRendererContent(
            @Nonnull final JsonObject clarificationRenderer) throws ParsingException {
        final MetaInfo metaInfo = new MetaInfo();

        final String title = YoutubeParsingHelper.getTextFromObject(clarificationRenderer
                .getObject("contentTitle"));
        final String text = YoutubeParsingHelper.getTextFromObject(clarificationRenderer
                .getObject("text"));
        if (title == null || text == null) {
            throw new ParsingException("Could not extract clarification renderer content");
        }
        metaInfo.setTitle(title);
        metaInfo.setContent(new Description(text, Description.PLAIN_TEXT));

        if (clarificationRenderer.has("actionButton")) {
            final JsonObject actionButton = clarificationRenderer.getObject("actionButton")
                    .getObject("buttonRenderer");
            try {
                final String url = YoutubeParsingHelper.getUrlFromNavigationEndpoint(actionButton
                        .getObject("command"));
                metaInfo.addUrl(new URL(Objects.requireNonNull(extractCachedUrlIfNeeded(url))));
            } catch (final NullPointerException | MalformedURLException e) {
                throw new ParsingException("Could not get metadata info URL", e);
            }

            final String metaInfoLinkText = YoutubeParsingHelper.getTextFromObject(
                    actionButton.getObject("text"));
            if (isNullOrEmpty(metaInfoLinkText)) {
                throw new ParsingException("Could not get metadata info link text.");
            }
            metaInfo.addUrlText(metaInfoLinkText);
        }

        if (clarificationRenderer.has("secondaryEndpoint") && clarificationRenderer
                .has("secondarySource")) {
            final String url = getUrlFromNavigationEndpoint(clarificationRenderer
                    .getObject("secondaryEndpoint"));
            // Ignore Google URLs, because those point to a Google search about "Covid-19"
            if (url != null && !isGoogleURL(url)) {
                try {
                    metaInfo.addUrl(new URL(url));
                    final String description = getTextFromObject(clarificationRenderer
                            .getObject("secondarySource"));
                    metaInfo.addUrlText(description == null ? url : description);
                } catch (final MalformedURLException e) {
                    throw new ParsingException("Could not get metadata info secondary URL", e);
                }
            }
        }

        return metaInfo;
    }

    /**
     * Sometimes, YouTube provides URLs which use Google's cache. They look like
     * {@code https://webcache.googleusercontent.com/search?q=cache:CACHED_URL}
     *
     * @param url the URL which might refer to the Google's webcache
     * @return the URL which is referring to the original site
     */
    public static String extractCachedUrlIfNeeded(final String url) {
        if (url == null) {
            return null;
        }
        if (url.contains("webcache.googleusercontent.com")) {
            return url.split("cache:")[1];
        }
        return url;
    }

    public static boolean isVerified(final JsonArray badges) {
        if (Utils.isNullOrEmpty(badges)) {
            return false;
        }

        for (final Object badge : badges) {
            final String style = ((JsonObject) badge).getObject("metadataBadgeRenderer")
                    .getString("style");
            if (style != null && (style.equals("BADGE_STYLE_TYPE_VERIFIED")
                    || style.equals("BADGE_STYLE_TYPE_VERIFIED_ARTIST"))) {
                return true;
            }
        }

        return false;
    }

    public static boolean hasArtistOrVerifiedIconBadgeAttachment(
            @Nonnull final JsonArray attachmentRuns) {
        return attachmentRuns.stream()
                .filter(JsonObject.class::isInstance)
                .map(JsonObject.class::cast)
                .anyMatch(attachmentRun -> attachmentRun.getObject("element")
                        .getObject("type")
                        .getObject("imageType")
                        .getObject("image")
                        .getArray("sources")
                        .stream()
                        .filter(JsonObject.class::isInstance)
                        .map(JsonObject.class::cast)
                        .anyMatch(source -> {
                            final String imageName = source.getObject("clientResource")
                                    .getString("imageName");
                            return "CHECK_CIRCLE_FILLED".equals(imageName)
                                    || "AUDIO_BADGE".equals(imageName)
                                    || "MUSIC_FILLED".equals(imageName);
                        }));

    }

    public static String resolveChannelId(final String idOrPath)
            throws ExtractionException, IOException {
        final String[] channelId = idOrPath.split("/");

        if (channelId[0].startsWith("UC")) {
            return channelId[0];
        }

        // If the url is an URL which is not a /channel URL, we need to use the
        // navigation/resolve_url endpoint of the InnerTube API to get the channel id. Otherwise,
        // we couldn't get information about the channel associated with this URL, if there is one.
        if (!channelId[0].equals("channel")) {
            final byte[] body = JsonWriter.string(prepareDesktopJsonBuilder(
                            Localization.DEFAULT, ContentCountry.DEFAULT)
                            .value("url", "https://www.youtube.com/" + idOrPath)
                            .done())
                    .getBytes(UTF_8);

            final JsonObject jsonResponse = getJsonPostResponse("navigation/resolve_url",
                    body, Localization.DEFAULT);

            if (!isNullOrEmpty(jsonResponse.getObject("error"))) {
                final JsonObject errorJsonObject = jsonResponse.getObject("error");
                final int errorCode = errorJsonObject.getInt("code");
                if (errorCode == 404) {
                    throw new ContentNotAvailableException("This channel doesn't exist.");
                } else {
                    throw new ContentNotAvailableException("Got error:\""
                            + errorJsonObject.getString("status") + "\": "
                            + errorJsonObject.getString("message"));
                }
            }

            final JsonObject endpoint = jsonResponse.getObject("endpoint");

            final String webPageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata")
                    .getString("webPageType", "");

            final JsonObject browseEndpoint = endpoint.getObject("browseEndpoint");
            final String browseId = browseEndpoint.getString("browseId", "");

            if (webPageType.equalsIgnoreCase("WEB_PAGE_TYPE_BROWSE")
                    || webPageType.equalsIgnoreCase("WEB_PAGE_TYPE_CHANNEL")
                    && !browseId.isEmpty()) {
                if (!browseId.startsWith("UC")) {
                    throw new ExtractionException("Redirected id is not pointing to a channel");
                }

                return browseId;
            }
        }
        return channelId[1];
    }

    public static final class ChannelResponseData {
        public final JsonObject responseJson;
        public final String channelId;

        public ChannelResponseData(final JsonObject responseJson, final String channelId) {
            this.responseJson = responseJson;
            this.channelId = channelId;
        }
    }

    public static ChannelResponseData getChannelResponse(final String channelId,
                                                          final String params,
                                                          final Localization loc,
                                                          final ContentCountry country)
            throws ExtractionException, IOException {
        return getChannelResponse(channelId, params, null, loc, country);
    }

    public static ChannelResponseData getChannelResponse(final String channelId,
                                                          final String params,
                                                          @Nullable final String query,
                                                          final Localization loc,
                                                          final ContentCountry country)
            throws ExtractionException, IOException {
        String id = channelId;
        JsonObject ajaxJson = null;

        int level = 0;
        while (level < 3) {
            final JsonBuilder<JsonObject> bodyBuilder = prepareDesktopJsonBuilder(
                            loc, country)
                            .value("browseId", id)
                            .value("params", params); // Equal to videos
            if (!isNullOrEmpty(query)) {
                bodyBuilder.value("query", query);
            }

            final byte[] body = JsonWriter.string(bodyBuilder.done())
                    .getBytes(UTF_8);

            final JsonObject jsonResponse = getJsonPostResponse("browse", body, loc);

            if (!isNullOrEmpty(jsonResponse.getObject("error"))) {
                final JsonObject errorJsonObject = jsonResponse.getObject("error");
                final int errorCode = errorJsonObject.getInt("code");
                if (errorCode == 404) {
                    throw new ContentNotAvailableException("This channel doesn't exist.");
                } else if (errorCode == 500) {
                    throw new ParsingException(errorJsonObject.getString("status") + "\": "
                            + errorJsonObject.getString("message"));
                }else {
                    throw new ContentNotAvailableException("Got error:\""
                            + errorJsonObject.getString("status") + "\": "
                            + errorJsonObject.getString("message"));
                }
            }

            final JsonObject endpoint = jsonResponse.getArray("onResponseReceivedActions")
                    .getObject(0)
                    .getObject("navigateAction")
                    .getObject("endpoint");

            final String webPageType = endpoint.getObject("commandMetadata")
                    .getObject("webCommandMetadata")
                    .getString("webPageType", "");

            final String browseId = endpoint.getObject("browseEndpoint").getString("browseId",
                    "");

            if (webPageType.equalsIgnoreCase("WEB_PAGE_TYPE_BROWSE")
                    || webPageType.equalsIgnoreCase("WEB_PAGE_TYPE_CHANNEL")
                    && !browseId.isEmpty()) {
                if (!browseId.startsWith("UC")) {
                    throw new ExtractionException("Redirected id is not pointing to a channel");
                }

                id = browseId;
                level++;
            } else {
                ajaxJson = jsonResponse;
                break;
            }
        }

        if (ajaxJson == null) {
            throw new ExtractionException("Got no channel response");
        }

        defaultAlertsCheck(ajaxJson);

        return new ChannelResponseData(ajaxJson, id);
    }

    /**
     * Generate a content playback nonce (also called {@code cpn}), sent by YouTube clients in
     * playback requests (and also for some clients, in the player request body).
     *
     * @return a content playback nonce string
     */
    @Nonnull
    public static String generateContentPlaybackNonce() {
        return RandomStringFromAlphabetGenerator.generate(
                CONTENT_PLAYBACK_NONCE_ALPHABET, 16, numberGenerator);
    }

    /**
     * Try to generate a {@code t} parameter, sent by mobile clients as a query of the player
     * request.
     *
     * <p>
     * Some researches needs to be done to know how this parameter, unique at each request, is
     * generated.
     * </p>
     *
     * @return a 12 characters string to try to reproduce the {@code} parameter
     */
    @Nonnull
    public static String generateTParameter() {
        return RandomStringFromAlphabetGenerator.generate(
                CONTENT_PLAYBACK_NONCE_ALPHABET, 12, numberGenerator);
    }

    /**
     * Check if the streaming URL is from the YouTube {@code WEB} client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a {@code WEB} streaming URL, false otherwise
     */
    public static boolean isWebStreamingUrl(@Nonnull final String url) {
        return Parser.isMatch(C_WEB_PATTERN, url);
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code TVHTML5_SIMPLY_EMBEDDED_PLAYER}
     * client.
     *
     * @param url the streaming URL on which check if it's a {@code TVHTML5_SIMPLY_EMBEDDED_PLAYER}
     *            streaming URL.
     * @return true if it's a {@code TVHTML5_SIMPLY_EMBEDDED_PLAYER} streaming URL, false otherwise
     */
    public static boolean isTvHtml5SimplyEmbeddedPlayerStreamingUrl(@Nonnull final String url) {
        return Parser.isMatch(C_TVHTML5_SIMPLY_EMBEDDED_PLAYER_PATTERN, url);
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code ANDROID} client.
     *
     * @param url the streaming URL to be checked.
     * @return true if it's a {@code ANDROID} streaming URL, false otherwise
     */
    public static boolean isAndroidStreamingUrl(@Nonnull final String url) {
        return Parser.isMatch(C_ANDROID_PATTERN, url);
    }

    /**
     * Check if the streaming URL is a URL from the YouTube {@code IOS} client.
     *
     * @param url the streaming URL on which check if it's a {@code IOS} streaming URL.
     * @return true if it's a {@code IOS} streaming URL, false otherwise
     */
    public static boolean isIosStreamingUrl(@Nonnull final String url) {
        return Parser.isMatch(C_IOS_PATTERN, url);
    }

    /**
     * Determines how the consent cookie that is required for YouTube, {@code SOCS}, will be
     * generated.
     *
     * <ul>
     *   <li>{@code false} (the default value) will use {@code CAE=};</li>
     *   <li>{@code true} will use {@code CAISAiAD}.</li>
     * </ul>
     *
     * <p>
     * Setting this value to {@code true} is needed to extract mixes and some YouTube Music
     * playlists in some countries such as the EU ones.
     * </p>
     */
    public static void setConsentAccepted(final boolean accepted) {
        consentAccepted = accepted;
    }

    /**
     * Get the value of the consent's acceptance.
     *
     * @see #setConsentAccepted(boolean)
     * @return the consent's acceptance value
     */
    public static boolean isConsentAccepted() {
        return consentAccepted;
    }
    /**
     * Auth
     */
    public static Map<String, String> parseCookies(String cookie) {
        Map<String, String> cookies = new HashMap<>();
        String[] cookiePairs = cookie.split("; ");
        for (String pair : cookiePairs) {
            String[] keyValue = pair.split("=", 2);
            if (keyValue.length == 2) {
                cookies.put(keyValue[0], keyValue[1]);
            } else {
                throw new IllegalArgumentException("Cookie has invalid format");
            }
        }
        return cookies;
    }

    public static String sha1(String input) throws NoSuchAlgorithmException {
        MessageDigest digest = MessageDigest.getInstance("SHA-1");
        byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
        StringBuilder hexString = new StringBuilder();
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) hexString.append('0');
            hexString.append(hex);
        }
        return hexString.toString();
    }

    public static String getAuthorizationHeader(String cookie) throws NoSuchAlgorithmException {
        String ytURL = "https://www.youtube.com";

        Map<String, String> cookies = parseCookies(cookie);
        String sapisid = cookies.get("SAPISID");

        if (sapisid == null) {
            sapisid = cookies.get("__Secure-3PAPISID");
            if (sapisid == null) {
                throw new IllegalArgumentException("SAPISID not found in cookies");
            }
        }

        long currentTimestamp = Instant.now().getEpochSecond();
        String initialData = currentTimestamp + " " + sapisid + " " + ytURL;
        String hash = sha1(initialData);

        return "SAPISIDHASH " + currentTimestamp + "_" + hash;
    }

    @Nonnull
    public static String getVisitorDataFromInnertube(
            @Nonnull final InnertubeClientRequestInfo innertubeClientRequestInfo,
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final Map<String, List<String>> httpHeaders,
            @Nonnull final String innertubeDomainAndVersionEndpoint,
            @Nullable final String embedUrl,
            final boolean useGuideEndpoint) throws IOException, ExtractionException {
        final JsonBuilder<JsonObject> builder = prepareJsonBuilder(
                localization, contentCountry, innertubeClientRequestInfo, embedUrl);

        final byte[] body = JsonWriter.string(builder.done())
                .getBytes(StandardCharsets.UTF_8);

        final String visitorData = JsonUtils.toJsonObject(getValidJsonResponseBody(getDownloader()
                .post(
                        innertubeDomainAndVersionEndpoint
                                + (useGuideEndpoint ? "guide" : "visitor_id") + "?"
                                + DISABLE_PRETTY_PRINT_PARAMETER,
                        httpHeaders, body)))
                .getObject("responseContext")
                .getString("visitorData");

        if (isNullOrEmpty(visitorData)) {
            throw new ParsingException("Could not get visitorData");
        }

        return visitorData;
    }

    @Nonnull
    static JsonBuilder<JsonObject> prepareJsonBuilder(
            @Nonnull final Localization localization,
            @Nonnull final ContentCountry contentCountry,
            @Nonnull final InnertubeClientRequestInfo innertubeClientRequestInfo,
            @Nullable final String embedUrl) {
        final JsonBuilder<JsonObject> builder = JsonObject.builder()
                .object("context")
                .object("client")
                .value("clientName", innertubeClientRequestInfo.clientInfo.clientName)
                .value("clientVersion", innertubeClientRequestInfo.clientInfo.clientVersion)
                .value("clientScreen", innertubeClientRequestInfo.clientInfo.clientScreen)
                .value("platform", innertubeClientRequestInfo.deviceInfo.platform);

        if (innertubeClientRequestInfo.clientInfo.visitorData != null) {
            builder.value("visitorData", innertubeClientRequestInfo.clientInfo.visitorData);
        }

        if (innertubeClientRequestInfo.deviceInfo.deviceMake != null) {
            builder.value("deviceMake", innertubeClientRequestInfo.deviceInfo.deviceMake);
        }
        if (innertubeClientRequestInfo.deviceInfo.deviceModel != null) {
            builder.value("deviceModel", innertubeClientRequestInfo.deviceInfo.deviceModel);
        }
        if (innertubeClientRequestInfo.deviceInfo.osName != null) {
            builder.value("osName", innertubeClientRequestInfo.deviceInfo.osName);
        }
        if (innertubeClientRequestInfo.deviceInfo.osVersion != null) {
            builder.value("osVersion", innertubeClientRequestInfo.deviceInfo.osVersion);
        }
        if (innertubeClientRequestInfo.deviceInfo.androidSdkVersion > 0) {
            builder.value("androidSdkVersion",
                    innertubeClientRequestInfo.deviceInfo.androidSdkVersion);
        }

        builder.value("hl", localization.getLocalizationCode())
                .value("gl", contentCountry.getCountryCode())
                .value("utcOffsetMinutes", 0)
                .end();

        if (embedUrl != null) {
            builder.object("thirdParty")
                    .value("embedUrl", embedUrl)
                    .end();
        }

        builder.object("request")
                .array("internalExperimentFlags")
                .end()
                .value("useSsl", true)
                .end()
                .object("user")
                // TODO: provide a way to enable restricted mode with:
                //  .value("enableSafetyMode", boolean)
                .value("lockedSafetyMode", false)
                .end()
                .end();

        return builder;
    }
}
