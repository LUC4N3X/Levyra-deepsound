package org.schabi.newpipe.extractor.downloader;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.exceptions.ReCaptchaException;
import org.schabi.newpipe.extractor.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.List;
import java.util.Map;

/**
 * A base for downloader implementations that NewPipe will use
 * to download needed resources during extraction.
 */
public abstract class Downloader {

    /**
     * Do a GET request to get the resource that the url is pointing to.<br>
     * <br>
     * This method calls {@link #get(String, Map, Localization)} with the default preferred
     * localization. It should only be used when the resource that will be fetched won't be affected
     * by the localization.
     *
     * @param url the URL that is pointing to the wanted resource
     * @return the result of the GET request
     */
    private static Localization defaultLocalization = Localization.DEFAULT;

    /**
     * Whether this downloader overrides streaming response methods with true streaming bodies.
     *
     * <p>The default {@link #getStreaming(String, Map, Localization)} and
     * {@link #postStreaming(String, Map, byte[], Localization)} implementations buffer the whole
     * response before wrapping it in an {@link java.io.InputStream}. Consumers that need low-latency
     * media startup or bounded memory should check this method before using SABR media paths.</p>
     */
    public boolean supportsStreamingResponses() {
        return false;
    }

    public Response get(final String url) throws IOException, ReCaptchaException {
        return get(url, null, defaultLocalization);
    }

    public CancellableCall getAsync(final String url, AsyncCallback callback) throws IOException, ReCaptchaException {
        return getAsync(url, null, defaultLocalization, callback);
    }

    /**
     * Do a GET request to get the resource that the url is pointing to.<br>
     * <br>
     * It will set the {@code Accept-Language} header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param localization the source of the value of the {@code Accept-Language} header
     * @return the result of the GET request
     */
    public Response get(final String url, @Nullable final Localization localization)
            throws IOException, ReCaptchaException {
        return get(url, null, localization);
    }

    /**
     * Do a GET request with the specified headers.
     *
     * @param url     the URL that is pointing to the wanted resource
     * @param headers a list of headers that will be used in the request.
     *                Any default headers <b>should</b> be overridden by these.
     * @return the result of the GET request
     */
    public Response get(final String url, @Nullable final Map<String, List<String>> headers)
            throws IOException, ReCaptchaException {
        return get(url, headers, defaultLocalization);
    }

    public CancellableCall getAsync(final String url, @Nullable final Map<String, List<String>> headers, AsyncCallback callback)
            throws IOException, ReCaptchaException {
        return getAsync(url, headers, defaultLocalization, callback);
    }

    /**
     * Do a GET request with the specified headers.<br>
     * <br>
     * It will set the {@code Accept-Language} header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     *                     Any default headers <b>should</b> be overridden by these.
     * @param localization the source of the value of the {@code Accept-Language} header
     * @return the result of the GET request
     */
    public Response get(final String url,
                        @Nullable final Map<String, List<String>> headers,
                        @Nullable final Localization localization)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder()
                .get(url)
                .headers(headers)
                .localization(localization)
                .build());
    }

    public CancellableCall getAsync(final String url, @Nullable final Map<String, List<String>> headers, @Nullable final Localization localization,
                             final AsyncCallback callback) throws IOException, ReCaptchaException {
        return executeAsync(Request.newBuilder()
                .get(url)
                .headers(headers)
                .localization(localization)
                .build(), callback);
    }

    public StreamingResponse getStreaming(final String url,
                                          @Nullable final Map<String, List<String>> headers,
                                          @Nullable final Localization localization)
            throws IOException, ReCaptchaException {
        final Response response = get(url, headers, localization);
        final byte[] raw = response.rawResponseBody() == null
                ? new byte[0] : response.rawResponseBody();
        return new StreamingResponse(response.responseCode(), response.responseHeaders(),
                new ByteArrayInputStream(raw));
    }

    /**
     * Do a HEAD request.
     *
     * @param url the URL that is pointing to the wanted resource
     * @return the result of the HEAD request
     */
    public Response head(final String url) throws IOException, ReCaptchaException {
        return head(url, null);
    }

    /**
     * Do a HEAD request with the specified headers.
     *
     * @param url     the URL that is pointing to the wanted resource
     * @param headers a list of headers that will be used in the request.
     *                Any default headers <b>should</b> be overridden by these.
     * @return the result of the HEAD request
     */
    public Response head(final String url, @Nullable final Map<String, List<String>> headers)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder()
                .head(url)
                .headers(headers)
                .build());
    }

    /**
     * Do a POST request with the specified headers, sending the data array.
     *
     * @param url        the URL that is pointing to the wanted resource
     * @param headers    a list of headers that will be used in the request.
     *                   Any default headers <b>should</b> be overridden by these.
     * @param dataToSend byte array that will be sent when doing the request.
     * @return the result of the GET request
     */
    public Response post(final String url,
                         @Nullable final Map<String, List<String>> headers,
                         @Nullable final byte[] dataToSend)
            throws IOException, ReCaptchaException {
        return post(url, headers, dataToSend, defaultLocalization);
    }

    /**
     * Do a POST request with the specified headers, sending the data array.
     * <br>
     * It will set the {@code Accept-Language} header to the language of the localization parameter.
     *
     * @param url          the URL that is pointing to the wanted resource
     * @param headers      a list of headers that will be used in the request.
     *                     Any default headers <b>should</b> be overridden by these.
     * @param dataToSend   byte array that will be sent when doing the request.
     * @param localization the source of the value of the {@code Accept-Language} header
     * @return the result of the GET request
     */
    public Response post(final String url,
                         @Nullable final Map<String, List<String>> headers,
                         @Nullable final byte[] dataToSend,
                         @Nullable final Localization localization)
            throws IOException, ReCaptchaException {
        return execute(Request.newBuilder()
                .post(url, dataToSend)
                .headers(headers)
                .localization(localization)
                .build());
    }

    /**
     * Like {@link #post(String, Map, byte[], Localization)} but returns the body as a stream instead
     * of a buffered {@code byte[]}, so a large response (e.g. a SABR media batch, 50-150MB at 4K) is
     * not held whole in memory. The default falls back to the buffered {@code post()} and wraps its
     * body; an implementation that can stream (e.g. over okhttp) should override this. Caller closes.
     */
    public StreamingResponse postStreaming(final String url,
                                           @Nullable final Map<String, List<String>> headers,
                                           @Nullable final byte[] dataToSend,
                                           @Nullable final Localization localization)
            throws IOException, ReCaptchaException {
        final Response response = post(url, headers, dataToSend, localization);
        final byte[] raw = response.rawResponseBody() == null
                ? new byte[0] : response.rawResponseBody();
        return new StreamingResponse(response.responseCode(), response.responseHeaders(),
                new ByteArrayInputStream(raw));
    }

    public CancellableCall postAsync(final String url,
                          @Nullable final Map<String, List<String>> headers,
                          @Nullable final byte[] dataToSend,
                          @Nullable final Localization localization,
                          AsyncCallback callback)
            throws IOException, ReCaptchaException {
        return executeAsync(Request.newBuilder()
                .post(url, dataToSend)
                .headers(headers)
                .localization(localization)
                .build(), callback);
    }

    public Response options(final String url,@Nullable final Map<String, List<String>> headers) throws IOException, ReCaptchaException {
        return execute(Request.newBuilder()
                .options(url)
                .headers(headers)
                .build());
    }
    /**
     * Do a request using the specified {@link Request} object.
     *
     * @return the result of the request
     */
    public abstract Response execute(@Nonnull Request request)
            throws IOException, ReCaptchaException;
    public abstract CancellableCall executeAsync(@Nonnull Request request, AsyncCallback callback)
            throws IOException, ReCaptchaException;

    public interface AsyncCallback {
        void onSuccess(Response response) throws ExtractionException;
        default void onError(Exception e) {
            e.printStackTrace();
        }
    }
}
