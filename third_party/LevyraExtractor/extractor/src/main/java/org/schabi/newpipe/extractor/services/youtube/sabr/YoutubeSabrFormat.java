package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonArray;
import com.grack.nanojson.JsonObject;

import org.schabi.newpipe.extractor.exceptions.ParsingException;
import org.schabi.newpipe.extractor.services.youtube.YoutubeApiDecoder;
import org.schabi.newpipe.extractor.services.youtube.YoutubeJavaScriptPlayerManager;
import org.schabi.newpipe.extractor.services.youtube.YoutubeParsingHelper;
import org.schabi.newpipe.extractor.stream.AudioTrackType;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;

import java.io.Serializable;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class YoutubeSabrFormat implements Serializable {
    private static final long serialVersionUID = 1L;
    private static final Pattern N_QUERY_PATTERN = Pattern.compile("(^|&)n=([^&]+)");
    private static final Pattern N_PATH_PATTERN = Pattern.compile("/n/([^/?#]+)");

    private final int itag;
    private final long lastModified;
    @Nullable
    private final String xtags;
    @Nullable
    private final String mimeType;
    @Nullable
    private final String audioTrackId;
    @Nullable
    private final String audioTrackDisplayName;
    private final boolean audioIsDefault;
    @Nullable
    private final String qualityLabel;
    @Nullable
    private final String audioQuality;
    private final boolean drc;
    private final int width;
    private final int height;
    private final int bitrate;
    private final long contentLength;
    private final long approxDurationMs;
    @Nullable
    private String initializationUrl;
    @Nullable
    private final String initializationUrlTemplate;
    @Nullable
    private final String obfuscatedSignature;
    @Nullable
    private final String signatureParameter;
    @Nullable
    private final String obfuscatedNParameter;
    private final long initRangeStart;
    private final long initRangeEnd;

    private YoutubeSabrFormat(final int itag,
                              final long lastModified,
                              @Nullable final String xtags,
                              @Nullable final String mimeType,
                              @Nullable final String audioTrackId,
                              @Nullable final String audioTrackDisplayName,
                              final boolean audioIsDefault,
                              @Nullable final String qualityLabel,
                              @Nullable final String audioQuality,
                              final boolean drc,
                              final int width,
                              final int height,
                              final int bitrate,
                              final long contentLength,
                              final long approxDurationMs,
                              @Nullable final String initializationUrl,
                              final long initRangeStart,
                              final long initRangeEnd) {
        this(itag, lastModified, xtags, mimeType, audioTrackId, audioTrackDisplayName,
                audioIsDefault, qualityLabel, audioQuality, drc, width, height, bitrate,
                contentLength, approxDurationMs, initializationUrl, initializationUrl, null, null,
                null, initRangeStart, initRangeEnd);
    }

    private YoutubeSabrFormat(final int itag,
                              final long lastModified,
                              @Nullable final String xtags,
                              @Nullable final String mimeType,
                              @Nullable final String audioTrackId,
                              @Nullable final String audioTrackDisplayName,
                              final boolean audioIsDefault,
                              @Nullable final String qualityLabel,
                              @Nullable final String audioQuality,
                              final boolean drc,
                              final int width,
                              final int height,
                              final int bitrate,
                              final long contentLength,
                              final long approxDurationMs,
                              @Nullable final String initializationUrl,
                              @Nullable final String initializationUrlTemplate,
                              @Nullable final String obfuscatedSignature,
                              @Nullable final String signatureParameter,
                              @Nullable final String obfuscatedNParameter,
                              final long initRangeStart,
                              final long initRangeEnd) {
        this.itag = itag;
        this.lastModified = lastModified;
        this.xtags = xtags;
        this.mimeType = mimeType;
        this.audioTrackId = audioTrackId;
        this.audioTrackDisplayName = audioTrackDisplayName;
        this.audioIsDefault = audioIsDefault;
        this.qualityLabel = qualityLabel;
        this.audioQuality = audioQuality;
        this.drc = drc;
        this.width = width;
        this.height = height;
        this.bitrate = bitrate;
        this.contentLength = contentLength;
        this.approxDurationMs = approxDurationMs;
        this.initializationUrl = initializationUrl;
        this.initializationUrlTemplate = initializationUrlTemplate;
        this.obfuscatedSignature = obfuscatedSignature;
        this.signatureParameter = signatureParameter;
        this.obfuscatedNParameter = obfuscatedNParameter;
        this.initRangeStart = initRangeStart;
        this.initRangeEnd = initRangeEnd;
    }

    @Nonnull
    static List<YoutubeSabrFormat> fromAdaptiveFormats(@Nonnull final String videoId,
                                                       @Nullable final JsonArray formats)
            throws ParsingException {
        final List<YoutubeSabrFormat> result = parseAdaptiveFormats(formats);
        return resolveInitializationUrls(videoId, result);
    }

    @Nonnull
    static List<YoutubeSabrFormat> parseAdaptiveFormats(@Nullable final JsonArray formats)
            throws ParsingException {
        final List<YoutubeSabrFormat> result = new ArrayList<>();
        if (formats == null) {
            return result;
        }
        for (int i = 0; i < formats.size(); i++) {
            final JsonObject format = formats.getObject(i);
            if (format != null && format.has("itag")) {
                result.add(fromJson(format));
            }
        }
        return result;
    }

    @Nonnull
    private static YoutubeSabrFormat fromJson(@Nonnull final JsonObject format)
            throws ParsingException {
        final JsonObject audioTrack = format.getObject("audioTrack");
        final JsonObject initRange = format.getObject("initRange");
        final JsonObject indexRange = format.getObject("indexRange");
        final long initRangeStart = initRange == null ? -1 : parseLong(initRange.get("start"));
        long initRangeEnd = initRange == null ? -1 : parseLong(initRange.get("end"));
        if (indexRange != null) {
            final long indexRangeEnd = parseLong(indexRange.get("end"));
            if (indexRangeEnd > initRangeEnd) {
                initRangeEnd = indexRangeEnd;
            }
        }
        final StreamingUrlParts urlParts = StreamingUrlParts.fromJson(format);
        return new YoutubeSabrFormat(
                format.getInt("itag"),
                parseLong(format.get("lastModified")),
                format.getString("xtags"),
                format.getString("mimeType"),
                audioTrack == null ? null : audioTrack.getString("id"),
                audioTrack == null ? null : audioTrack.getString("displayName"),
                audioTrack != null && audioTrack.getBoolean("audioIsDefault", false),
                format.getString("qualityLabel"),
                format.getString("audioQuality"),
                format.getBoolean("isDrc", false),
                format.getInt("width", -1),
                format.getInt("height", -1),
                format.getInt("bitrate", -1),
                parseLong(format.get("contentLength")),
                parseLong(format.get("approxDurationMs")),
                urlParts.isResolved() ? urlParts.url : null,
                urlParts.url,
                urlParts.signature,
                urlParts.signatureParameter,
                urlParts.nParameter,
                initRangeStart, initRangeEnd);
    }

    @Nonnull
    private static List<YoutubeSabrFormat> resolveInitializationUrls(
            @Nonnull final String videoId,
            @Nonnull final List<YoutubeSabrFormat> formats) throws ParsingException {
        final Set<String> signatures = new LinkedHashSet<>();
        final Set<String> nParameters = new LinkedHashSet<>();
        collectDecodeParameters(formats, signatures, nParameters);
        if (signatures.isEmpty() && nParameters.isEmpty()) {
            return formats;
        }
        final YoutubeApiDecoder.BatchDecodeResult decoded =
                YoutubeJavaScriptPlayerManager.deobfuscateBatch(videoId,
                        new ArrayList<>(signatures), new ArrayList<>(nParameters));
        resolveInitializationUrls(formats, decoded);
        return formats;
    }

    static void collectDecodeParameters(@Nonnull final Collection<YoutubeSabrFormat> formats,
                                        @Nonnull final Set<String> signatures,
                                        @Nonnull final Set<String> nParameters) {
        for (final YoutubeSabrFormat format : formats) {
            if (format.obfuscatedSignature != null) {
                signatures.add(format.obfuscatedSignature);
            }
            if (format.obfuscatedNParameter != null) {
                nParameters.add(format.obfuscatedNParameter);
            }
        }
    }

    static void resolveInitializationUrls(@Nonnull final Collection<YoutubeSabrFormat> formats,
                                          @Nonnull final YoutubeApiDecoder.BatchDecodeResult decoded)
            throws ParsingException {
        for (final YoutubeSabrFormat format : formats) {
            format.resolveInitializationUrl(decoded);
        }
    }

    @Nullable
    static String resolveNParameter(@Nullable final String url,
                                    @Nonnull final YoutubeApiDecoder.BatchDecodeResult decoded)
            throws ParsingException {
        if (url == null || url.isEmpty()) {
            return url;
        }
        final UrlComponents components = UrlComponents.parse(url);
        final String encryptedN = extractNParameter(components);
        if (encryptedN == null) {
            return url;
        }
        final String decryptedN = decoded.getNParameters().get(encryptedN);
        if (decryptedN == null) {
            return url;
        }
        final Matcher queryMatcher = N_QUERY_PATTERN.matcher(components.queryOrEmpty());
        if (queryMatcher.find()) {
            final String resolvedQuery = components.queryOrEmpty().substring(0,
                    queryMatcher.start(2)) + urlEncode(decryptedN)
                    + components.queryOrEmpty().substring(queryMatcher.end(2));
            return components.withQuery(resolvedQuery);
        }
        final Matcher pathMatcher = N_PATH_PATTERN.matcher(components.path);
        if (!pathMatcher.find()) {
            return url;
        }
        final String resolvedPath = components.path.substring(0, pathMatcher.start(1))
                + decryptedN + components.path.substring(pathMatcher.end(1));
        return components.withPath(resolvedPath);
    }

    @Nullable
    static String extractNParameter(@Nullable final String url) throws ParsingException {
        if (url == null || url.isEmpty()) {
            return null;
        }
        return extractNParameter(UrlComponents.parse(url));
    }

    @Nullable
    private static String extractNParameter(@Nonnull final UrlComponents components)
            throws ParsingException {
        final Matcher queryMatcher = N_QUERY_PATTERN.matcher(components.queryOrEmpty());
        if (queryMatcher.find()) {
            return urlDecode(queryMatcher.group(2));
        }
        final Matcher pathMatcher = N_PATH_PATTERN.matcher(components.path);
        return pathMatcher.find() ? pathMatcher.group(1) : null;
    }

    private void resolveInitializationUrl(
            @Nonnull final YoutubeApiDecoder.BatchDecodeResult decoded) throws ParsingException {
        String url = initializationUrlTemplate;
        if (url == null || url.isEmpty()) {
            initializationUrl = url;
            return;
        }
        if (obfuscatedSignature != null) {
            final String signature = decoded.getSignatures().get(obfuscatedSignature);
            if (signature == null) {
                initializationUrl = null;
                return;
            }
            url = appendQueryParameter(url, signatureParameter == null
                    ? "signature" : signatureParameter, signature);
        }
        initializationUrl = resolveNParameter(url, decoded);
    }

    @Nonnull
    private static String appendQueryParameter(@Nonnull final String url,
                                               @Nonnull final String name,
                                               @Nonnull final String value)
            throws ParsingException {
        final UrlComponents components = UrlComponents.parse(url);
        final String encodedParameter = urlEncode(name) + '=' + urlEncode(value);
        final String query = components.queryOrEmpty();
        return components.withQuery(query.isEmpty()
                ? encodedParameter : query + '&' + encodedParameter);
    }

    private static final class StreamingUrlParts {
        @Nullable final String url;
        @Nullable final String signature;
        @Nullable final String signatureParameter;
        @Nullable final String nParameter;

        private StreamingUrlParts(@Nullable final String url,
                                  @Nullable final String signature,
                                  @Nullable final String signatureParameter,
                                  @Nullable final String nParameter) {
            this.url = url;
            this.signature = signature;
            this.signatureParameter = signatureParameter;
            this.nParameter = nParameter;
        }

        @Nonnull
        static StreamingUrlParts fromJson(@Nonnull final JsonObject format)
                throws ParsingException {
            String url = format.getString("url");
            String signature = null;
            String signatureParameter = null;
            final String cipherValue = format.has("signatureCipher")
                    ? format.getString("signatureCipher") : format.getString("cipher");
            if ((url == null || url.isEmpty()) && cipherValue != null && !cipherValue.isEmpty()) {
                final Map<String, String> cipher = parseQuery(cipherValue);
                url = cipher.get("url");
                signature = cipher.get("s");
                signatureParameter = cipher.getOrDefault("sp", "signature");
            }
            final String nParameter = extractNParameter(url);
            return new StreamingUrlParts(url, signature, signatureParameter, nParameter);
        }

        boolean isResolved() {
            return signature == null && nParameter == null;
        }
    }

    private static final class UrlComponents {
        @Nonnull
        private final String path;
        @Nullable
        private final String query;

        private UrlComponents(@Nonnull final String path, @Nullable final String query) {
            this.path = path;
            this.query = query;
        }

        @Nonnull
        static UrlComponents parse(@Nonnull final String url) throws ParsingException {
            if (url.indexOf('#') >= 0) {
                throw new ParsingException("SABR URL fragments are not supported");
            }
            final int queryStart = url.indexOf('?');
            if (queryStart < 0) {
                return new UrlComponents(url, null);
            }
            return new UrlComponents(url.substring(0, queryStart),
                    url.substring(queryStart + 1));
        }

        @Nonnull
        String queryOrEmpty() {
            return query == null ? "" : query;
        }

        @Nonnull
        String withPath(@Nonnull final String resolvedPath) {
            return query == null ? resolvedPath : resolvedPath + '?' + query;
        }

        @Nonnull
        String withQuery(@Nonnull final String resolvedQuery) {
            return path + '?' + resolvedQuery;
        }
    }

    @Nonnull
    private static Map<String, String> parseQuery(@Nullable final String value)
            throws ParsingException {
        final Map<String, String> params = new HashMap<>();
        if (value == null || value.isEmpty()) {
            return params;
        }
        for (final String part : value.split("&")) {
            final int equals = part.indexOf('=');
            if (equals <= 0) {
                continue;
            }
            params.put(urlDecode(part.substring(0, equals)),
                    urlDecode(part.substring(equals + 1)));
        }
        return params;
    }

    @Nonnull
    private static String urlEncode(@Nonnull final String value) throws ParsingException {
        try {
            return URLEncoder.encode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            throw new ParsingException("Could not encode SABR signature cipher", e);
        }
    }

    @Nonnull
    private static String urlDecode(@Nonnull final String value) throws ParsingException {
        try {
            return URLDecoder.decode(value, StandardCharsets.UTF_8.name());
        } catch (final UnsupportedEncodingException e) {
            throw new ParsingException("Could not decode SABR signature cipher", e);
        }
    }

    private static long parseLong(@Nullable final Object value) {
        if (value instanceof Number) {
            return ((Number) value).longValue();
        }
        if (value instanceof String) {
            try {
                return Long.parseLong((String) value);
            } catch (final NumberFormatException ignored) {
                return -1;
            }
        }
        return -1;
    }

    public boolean isAudio() {
        return mimeType != null && mimeType.contains("audio");
    }

    public boolean isVideo() {
        return mimeType != null && mimeType.contains("video");
    }

    public int getItag() {
        return itag;
    }

    public long getLastModified() {
        return lastModified;
    }

    @Nullable
    public String getXtags() {
        return xtags;
    }

    @Nullable
    public String getMimeType() {
        return mimeType;
    }

    @Nullable
    public String getAudioTrackId() {
        return audioTrackId;
    }

    @Nullable
    public String getAudioTrackDisplayName() {
        return audioTrackDisplayName;
    }

    public boolean isAudioDefault() {
        return audioIsDefault;
    }

    @Nullable
    public AudioTrackType getAudioTrackType() {
        return YoutubeParsingHelper.extractAudioTrackType(xtags);
    }

    @Nullable
    public String getAudioLocale() {
        final String language = YoutubeParsingHelper.extractXtagsValue(xtags, "lang");
        if (language != null && !language.isEmpty()) {
            return language;
        }
        if (audioTrackId == null || audioTrackId.isEmpty()) {
            return null;
        }
        final int separator = audioTrackId.indexOf('.');
        return separator > 0 ? audioTrackId.substring(0, separator) : audioTrackId;
    }

    public boolean isAutoGeneratedAudio() {
        return "dubbed-auto".equalsIgnoreCase(
                YoutubeParsingHelper.extractXtagsValue(xtags, "acont"));
    }

    public boolean isOriginalAudio() {
        final AudioTrackType type = getAudioTrackType();
        return type == AudioTrackType.ORIGINAL || (type == null && audioIsDefault);
    }

    @Nonnull
    public String formatIdentity() {
        return itag + "|" + safe(audioTrackId) + "|" + safe(getAudioLocale()) + "|"
                + (getAudioTrackType() == null ? "" : getAudioTrackType().name()) + "|"
                + safe(mimeType) + "|" + initRangeStart + "-" + initRangeEnd + "|"
                + lastModified + "|" + safe(xtags);
    }

    @Nonnull
    private static String safe(@Nullable final String value) {
        return value == null ? "" : value;
    }

    @Nullable
    public String getQualityLabel() {
        return qualityLabel;
    }

    @Nullable
    public String getAudioQuality() {
        return audioQuality;
    }

    public boolean isDrc() {
        return drc;
    }

    public int getWidth() {
        return width;
    }

    public int getHeight() {
        return height;
    }

    public int getBitrate() {
        return bitrate;
    }

    public long getContentLength() {
        return contentLength;
    }

    public long getApproxDurationMs() {
        return approxDurationMs;
    }

    @Nullable
    public String getInitializationUrl() {
        return initializationUrl;
    }

    public long getInitRangeStart() {
        return initRangeStart;
    }

    public long getInitRangeEnd() {
        return initRangeEnd;
    }
}
