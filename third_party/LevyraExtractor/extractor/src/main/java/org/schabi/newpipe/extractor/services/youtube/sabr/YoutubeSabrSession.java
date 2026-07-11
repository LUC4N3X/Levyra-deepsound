package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.schabi.newpipe.extractor.NewPipe;
import org.schabi.newpipe.extractor.downloader.StreamingResponse;
import org.schabi.newpipe.extractor.exceptions.ExtractionException;
import org.schabi.newpipe.extractor.localization.ContentCountry;
import org.schabi.newpipe.extractor.localization.Localization;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.IOException;
import java.io.InputStream;
import java.io.InterruptedIOException;
import java.util.ArrayDeque;
import java.util.Arrays;
import java.util.Collections;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public final class YoutubeSabrSession {
    private static final int MAX_REQUESTS_PER_SEGMENT = 16;
    private static final int MAX_POLICY_ONLY_RESPONSES_PER_SEGMENT = 3;
    private static final int MAX_REDIRECTS_PER_SESSION = 3;
    // server can ask us to reload the player response (URLs/config expired on a long watch). re-probe
    // and resume in place instead of killing the session. bounded so a reload loop can't run forever.
    private static final int MAX_RELOADS_PER_SESSION = 2;
    private static final int INTEGRITY_RELOAD_AFTER_FAILURES = 2;
    private static final int MAX_INCOMPLETE_MEDIA_RESPONSES = 3;
    // How many times a stale/rejected PO token may be force-re-minted before giving up (token
    // expiry mid-playback). Bounded so a genuinely-rejected token can't loop forever.
    private static final int MAX_PO_TOKEN_REFRESHES = 2;
    private static final int MAX_BACKOFF_MS = 30_000;
    // Cap the cached media bytes so a high-bitrate (4K VP9/AV1) stream can't fill the heap and OOM.
    // 32 MiB ≈ ~50s of 4K video, far more than the read-lag, so forward playback never starves.
    private static final long MAX_CACHE_BYTES = 32L * 1024 * 1024;
    private static final int MIN_CACHED_SEGMENTS = 6;
    private static final int MAX_DIAGNOSTIC_CHARS = 32 * 1024;
    private static final int MAX_INITIALIZATION_BYTES = 16 * 1024 * 1024;
    private static final int MAX_TRACE_EVENTS = 1024;
    @Nonnull
    // not final: a server-requested reload swaps in a freshly probed info (new URL + ustreamer config)
    private YoutubeSabrInfo info;
    @Nonnull
    private final YoutubeSabrFormat audioFormat;
    @Nonnull
    private final YoutubeSabrFormat videoFormat;
    @Nonnull
    private final YoutubeSabrStreamState streamState;
    @Nullable
    private final SabrPoTokenProvider poTokenProvider;
    private final Map<String, SabrMediaSegment> segmentCache = new ConcurrentHashMap<>();
    private final Object segmentAvailable = new Object();
    private String serverAbrStreamingUrl;
    private int requestNumber;
    private int redirectCount;
    private int poTokenRefreshes;
    private int reloads;
    private int consecutiveIntegrityFailures;
    // Insertion order + total bytes of cached MEDIA segments (init segments are never evicted).
    // Mutated only by the single pump thread in pumpOnce; readers only do concurrent-map gets.
    private final Deque<String> cacheOrder = new ArrayDeque<>();
    private final Deque<String> diagnosticEvents = new ArrayDeque<>();
    private int diagnosticChars;
    private long cachedBytes;
    private long peakCachedBytes;
    private volatile long totalResponseBytes;
    private volatile boolean traceEnabled;
    private final Object traceLock = new Object();
    private long traceResponseBytes;
    private long traceMediaPayloadBytes;
    private long traceControlPayloadBytes;
    private long traceUmpOverheadBytes;
    private long traceDiscardedBytes;
    @Nonnull
    private final Deque<String> traceSegments = new ArrayDeque<>();
    @Nonnull
    private final Deque<String> traceDiscards = new ArrayDeque<>();
    // Real play head (ms) fed by the pump, so eviction never drops a segment the player still needs.
    private volatile long playHeadMs;
    // Keep this much already-played media before evicting: the two tracks read slightly apart and a
    // segment ending right at the play head may still be in use (race that starved audio at the edge).
    private static final long EVICT_BEHIND_MS = 10_000;
    // After a seek, collapse the cache to a window of +-this around the target. A large seek leaves the
    // old pre-fetched span disconnected from the new position by a gap; play-head eviction can't drop it
    // (it depends on the reader position, which a seek leaves stale until both tracks read at the
    // target), so the cache held two disjoint spans far over MAX_CACHE_BYTES -> OOM at 4K. The pump
    // re-fetches contiguously from the target, so the out-of-window segments are pure waste.
    private static final long SEEK_KEEP_WINDOW_MS = 8_000;

    public YoutubeSabrSession(@Nonnull final YoutubeSabrInfo info,
                               @Nonnull final YoutubeSabrFormat audioFormat,
                               @Nonnull final YoutubeSabrFormat videoFormat) {
        this(info, audioFormat, videoFormat, null);
    }

    public YoutubeSabrSession(@Nonnull final YoutubeSabrInfo info,
                              @Nonnull final YoutubeSabrFormat audioFormat,
                              @Nonnull final YoutubeSabrFormat videoFormat,
                              @Nullable final SabrPoTokenProvider poTokenProvider) {
        if (!audioFormat.isAudio()) {
            throw new IllegalArgumentException("SABR audio format must be audio: itag="
                    + audioFormat.getItag());
        }
        if (!videoFormat.isVideo()) {
            throw new IllegalArgumentException("SABR video format must be video: itag="
                    + videoFormat.getItag());
        }
        if (audioFormat.getItag() == videoFormat.getItag()) {
            throw new IllegalArgumentException("SABR audio/video formats must be distinct");
        }
        if (info.getServerAbrStreamingUrl() == null || info.getServerAbrStreamingUrl().isEmpty()) {
            throw new IllegalArgumentException("Missing SABR streaming URL");
        }
        this.info = info;
        this.audioFormat = audioFormat;
        this.videoFormat = videoFormat;
        this.streamState = new YoutubeSabrStreamState(audioFormat, videoFormat);
        this.poTokenProvider = poTokenProvider;
        this.serverAbrStreamingUrl = info.getServerAbrStreamingUrl();
    }

    @Nonnull
    public SabrMediaSegment fetchSegment(@Nonnull final SabrSegmentRequest request,
                                         @Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final SabrMediaSegment cachedSegment = segmentCache.get(cacheKey(request));
        if (cachedSegment != null) {
            return cachedSegment;
        }
        failIfKnownOutOfBounds(request);

        boolean targetPrepared = maybePrepareForDistantMediaSegment(request);
        int policyOnlyResponses = 0;
        for (int attempts = 0; attempts < MAX_REQUESTS_PER_SEGMENT; attempts++) {
            final YoutubeSabrProbeResult result = fetchNextResponse(localization,
                    this::ingestAndCacheSegment);
            final SabrDecodedResponse decoded = result.getDecodedResponse();
            final List<String> integrityIssues = decoded.getIntegrityIssues();
            if (!integrityIssues.isEmpty()) {
                if (isRecoverableIncompleteMediaResponse(integrityIssues)) {
                    if (recoverFromIncompleteMediaResponse(localization, decoded)) {
                        continue;
                    }
                    throw new SabrProtocolException("SABR media integrity issue while fetching "
                            + describeRequest(request) + ": " + integrityIssues);
                }
                throw new SabrProtocolException("SABR media integrity issue while fetching "
                        + describeRequest(request) + ": " + integrityIssues);
            }
            consecutiveIntegrityFailures = 0;
            streamState.ingest(decoded);
            final List<SabrMediaSegment> segments = result.getSegments();
            final SabrMediaSegment segment = segmentCache.get(cacheKey(request));
            if (segment != null) {
                return segment;
            }
            failIfKnownOutOfBounds(request);
            if (!targetPrepared) {
                targetPrepared = maybePrepareForDistantMediaSegment(request);
            }
            if (decoded.getSabrErrorDetails() != null) {
                throw new SabrProtocolException("SABR error while fetching "
                        + describeRequest(request) + ": " + decoded.getSabrErrorDetails().summarize());
            }
            if (decoded.isReloadRequested()) {
                if (maybeReload(localization)) {
                    continue;
                }
                throw new SabrProtocolException("SABR requested player reload while fetching "
                        + describeRequest(request) + " (reload budget spent): "
                        + decoded.summarizeNoMediaResponse());
            }
            if (decoded.isProtectionBoundaryNoMediaResponse()) {
                if (applyPoTokenForProtectedResponse()) {
                    if (decoded.getBackoffTimeMs() > 0) {
                        sleepBackoff(decoded.getBackoffTimeMs());
                    }
                    continue;
                }
                throw new SabrProtocolException("SABR protected no-media response while fetching "
                        + describeRequest(request) + ": " + decoded.summarizeNoMediaResponse());
            }
            if (decoded.isPolicyOnlyResponse()) {
                policyOnlyResponses++;
                if (policyOnlyResponses >= MAX_POLICY_ONLY_RESPONSES_PER_SEGMENT) {
                    throw new SabrProtocolException("SABR repeated policy-only responses while fetching "
                            + describeRequest(request) + ": " + decoded.summarizeNoMediaResponse());
                }
            } else if (result.getSegmentCount() > 0) {
                policyOnlyResponses = 0;
            }
            if (decoded.getBackoffTimeMs() > 0) {
                sleepBackoff(decoded.getBackoffTimeMs());
            }
            if (streamState.isComplete()) {
                break;
            }
        }
        throw new SabrProtocolException("Requested SABR segment was not returned: itag="
                + request.getFormat().getItag()
                + (request.isInitializationSegment()
                ? ":init"
                : ":seq=" + request.getSequenceNumber()));
    }

    @Nonnull
    public YoutubeSabrProbeResult fetchNextResponse(@Nonnull final Localization localization)
            throws IOException, ExtractionException {
        return fetchNextResponse(localization, null);
    }

    @Nonnull
    private YoutubeSabrProbeResult fetchNextResponse(
            @Nonnull final Localization localization,
            @Nullable final SabrStreamingResponseReader.SegmentConsumer segmentConsumer)
            throws IOException, ExtractionException {
        return fetchNextResponseUntil(localization, segmentConsumer == null ? null : segment -> {
            segmentConsumer.accept(segment);
            return true;
        });
    }

    @Nonnull
    private YoutubeSabrProbeResult fetchNextResponseUntil(
            @Nonnull final Localization localization,
            @Nullable final SabrStreamingResponseReader.StoppableSegmentConsumer segmentConsumer)
            throws IOException, ExtractionException {
        addDiagnosticEvent("request n=" + requestNumber
                + " playerMs=" + streamState.getRequestPlayerTimeMs()
                + " edgeMs=" + streamState.getMinBufferedEndMs()
                + " poTokenBytes=" + (streamState.getRawPoToken() == null
                ? -1 : streamState.getRawPoToken().length)
                + " ranges=" + streamState.summarizeBufferedRanges());
        final YoutubeSabrProbeResult result;
        final long requestStartNs = System.nanoTime();
        try {
            if (requestNumber == 0) {
                result = segmentConsumer == null
                        ? YoutubeSabrProbe.probeFirstMediaResponse(info, audioFormat, videoFormat,
                        streamState, serverAbrStreamingUrl, localization)
                        : YoutubeSabrProbe.probeFirstMediaResponseStreamingUntil(info, audioFormat,
                        videoFormat, streamState, serverAbrStreamingUrl, segmentConsumer,
                        localization);
            } else {
                result = segmentConsumer == null
                        ? YoutubeSabrProbe.probeFollowUpMediaResponse(info, audioFormat, videoFormat,
                        streamState, requestNumber, serverAbrStreamingUrl, localization)
                        : YoutubeSabrProbe.probeFollowUpMediaResponseStreamingUntil(info, audioFormat,
                        videoFormat, streamState, requestNumber, serverAbrStreamingUrl,
                        segmentConsumer, localization);
            }
        } catch (final IOException | ExtractionException e) {
            addDiagnosticEvent("request_failed n=" + requestNumber
                    + " type=" + e.getClass().getSimpleName());
            throw e;
        }
        addDiagnosticEvent("response n=" + requestNumber
                + " http=" + result.getResponseCode()
                + " contentType=" + result.getContentType()
                + " segments=" + (result.getSegments().isEmpty()
                ? "count=" + result.getSegmentCount() : summarizeSegments(result.getSegments()))
                + " decoded={" + result.getDecodedResponse().summarizeForDiagnostics() + '}');
        totalResponseBytes += result.getResponseBytes();
        recordTraceResponse(result);
        updateBandwidthEstimate(result.getResponseBytes(), System.nanoTime() - requestStartNs);
        if (result.getDecodedResponse().getRedirectUrl() != null
                && !result.getDecodedResponse().getRedirectUrl().isEmpty()) {
            redirectCount++;
            if (redirectCount > MAX_REDIRECTS_PER_SESSION) {
                throw new SabrProtocolException("SABR redirect limit exceeded: redirects="
                        + redirectCount);
            }
            serverAbrStreamingUrl = result.getDecodedResponse().getRedirectUrl();
        }
        requestNumber++;
        return result;
    }

    private void updateBandwidthEstimate(final long responseBytes, final long elapsedNs) {
        if (responseBytes <= 0 || elapsedNs <= 0) {
            return;
        }
        final long sampleBitsPerSecond = responseBytes * 8_000_000_000L / elapsedNs;
        final long previous = streamState.getBandwidthEstimate();
        final long estimate = previous <= 0
                ? sampleBitsPerSecond : (previous * 3 + sampleBitsPerSecond) / 4;
        streamState.setBandwidthEstimate(estimate);
        addDiagnosticEvent("bandwidth sampleBps=" + sampleBitsPerSecond
                + " estimateBps=" + estimate);
    }

    public synchronized void addDiagnosticEvent(@Nonnull final String event) {
        final String bounded = event.length() > MAX_DIAGNOSTIC_CHARS
                ? event.substring(0, MAX_DIAGNOSTIC_CHARS) : event;
        while (!diagnosticEvents.isEmpty()
                && diagnosticChars + bounded.length() > MAX_DIAGNOSTIC_CHARS) {
            diagnosticChars -= diagnosticEvents.removeFirst().length();
        }
        diagnosticEvents.addLast(bounded);
        diagnosticChars += bounded.length();
    }

    @Nonnull
    public synchronized String getDiagnosticTrace() {
        final StringBuilder trace = new StringBuilder();
        for (final String event : diagnosticEvents) {
            if (trace.length() > 0) {
                trace.append(" | ");
            }
            trace.append(event);
        }
        return trace.toString();
    }

    @Nonnull
    private static String summarizeSegments(@Nonnull final List<SabrMediaSegment> segments) {
        if (segments.isEmpty()) {
            return "[]";
        }
        final StringBuilder summary = new StringBuilder("[");
        for (int i = 0; i < segments.size(); i++) {
            if (i > 0) {
                summary.append(',');
            }
            final SabrMediaSegment segment = segments.get(i);
            summary.append(segment.getHeader().getItag()).append(':');
            if (segment.getHeader().isInitSegment()) {
                summary.append("init");
            } else {
                summary.append(segment.getHeader().getSequenceNumber());
            }
        }
        return summary.append(']').toString();
    }

    /**
     * Server asked us to reload the player response (its URLs / ustreamer config expired on a long
     * watch). Re-probe a fresh {@link YoutubeSabrInfo} and swap it in, keeping the stream state
     * (player time, buffered ranges, token) so the next request resumes in place instead of
     * restarting from 0. Bounded by {@link #MAX_RELOADS_PER_SESSION}.
     *
     * @return true if a fresh info was applied and the caller should retry, false if the reload
     *     budget is spent or the re-probe came back unusable.
     */
    private boolean maybeReload(@Nonnull final Localization localization)
            throws IOException, ExtractionException {
        if (reloads >= MAX_RELOADS_PER_SESSION) {
            return false;
        }
        reloads++;
        final ContentCountry contentCountry = localization.getCountryCode().isEmpty()
                ? ContentCountry.DEFAULT
                : new ContentCountry(localization.getCountryCode());
        final YoutubeSabrInfo fresh = YoutubeSabrProbe.fetchSabrInfo(
                info.getVideoId(), info.getProfile(), localization, contentCountry);
        if (fresh.getServerAbrStreamingUrl() == null || fresh.getServerAbrStreamingUrl().isEmpty()) {
            return false;
        }
        info = fresh;
        serverAbrStreamingUrl = fresh.getServerAbrStreamingUrl();
        redirectCount = 0;
        // keep requestNumber > 0 so the next request is a follow-up carrying the current player time
        // and buffered ranges: the new streaming URL resumes in place, not from the start.
        return true;
    }

    /**
     * Server-driven advance: issue one request with the current state and ingest whatever the
     * server returns (segments for either or both formats), instead of demanding one specific
     * per-track segment. A single consumer pumping this keeps audio and video coherent and lets the
     * server feed the track that is behind the play head, so neither track starves the other.
     */
    @Nonnull
    public List<SabrMediaSegment> pumpOnce(@Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final YoutubeSabrProbeResult result = pumpOnceInternal(localization, false);
        return result == null ? Collections.emptyList() : result.getSegments();
    }

    /** Production pump path: consume/cache each segment before reading the next one. */
    public int pumpOnceStreaming(@Nonnull final Localization localization)
            throws IOException, ExtractionException {
        final YoutubeSabrProbeResult result = pumpOnceInternal(localization, true);
        return result == null ? 0 : result.getSegmentCount();
    }

    /**
     * Like {@link #pumpOnceStreaming(Localization)}, but used by callers that are waiting on a
     * concrete segment. Keep consuming the whole response: SABR/UMP response boundaries are part of
     * the protocol state, and closing after the target segment can cut off a following media header
     * whose body/end is still in the same response.
     */
    public int pumpOnceStreamingUntilCached(@Nonnull final Localization localization,
                                            @Nonnull final SabrSegmentRequest target)
            throws IOException, ExtractionException {
        final YoutubeSabrProbeResult result = pumpOnceInternal(localization, true);
        return result == null ? 0 : result.getSegmentCount();
    }

    @Nullable
    private YoutubeSabrProbeResult pumpOnceInternal(@Nonnull final Localization localization,
                                                     final boolean streaming)
            throws IOException, ExtractionException {
        return pumpOnceInternal(localization, streaming ? segment -> {
            ingestAndCacheSegment(segment);
            return true;
        } : null);
    }

    @Nullable
    private YoutubeSabrProbeResult pumpOnceInternal(
            @Nonnull final Localization localization,
            @Nullable final SabrStreamingResponseReader.StoppableSegmentConsumer segmentConsumer)
            throws IOException, ExtractionException {
        final YoutubeSabrProbeResult result = segmentConsumer == null
                ? fetchNextResponse(localization)
                : fetchNextResponseUntil(localization, segmentConsumer);
        final SabrDecodedResponse decoded = result.getDecodedResponse();
        final List<String> integrityIssues = decoded.getIntegrityIssues();
        if (!integrityIssues.isEmpty()) {
            if (isRecoverableIncompleteMediaResponse(integrityIssues)) {
                if (recoverFromIncompleteMediaResponse(localization, decoded)) {
                    return null;
                }
                throw new SabrProtocolException("SABR media integrity issue: " + integrityIssues);
            }
            throw new SabrProtocolException("SABR media integrity issue: " + integrityIssues);
        }
        consecutiveIntegrityFailures = 0;
        streamState.ingest(decoded);
        final List<SabrMediaSegment> segments = result.getSegments();
        if (segmentConsumer == null) {
            for (final SabrMediaSegment segment : segments) {
                ingestAndCacheSegment(segment);
            }
        }
        evictCacheIfNeeded();
        if (decoded.getSabrErrorDetails() != null) {
            throw new SabrProtocolException("SABR error: "
                    + decoded.getSabrErrorDetails().summarize());
        }
        if (decoded.isReloadRequested()) {
            if (maybeReload(localization)) {
                // fresh session applied; this round yields no media, the pump will call us again
                return null;
            }
            throw new SabrProtocolException("SABR requested player reload (reload budget spent): "
                    + decoded.summarizeNoMediaResponse());
        }
        if (decoded.isProtectionBoundaryNoMediaResponse()) {
            // Mint / re-mint the token as soon as SABR reaches the protection boundary. Do not throw
            // on a single no-media round: the server usually clears it next round. The pump keeps
            // trying; the stall watchdog is the real give-up.
            applyPoTokenForProtectedResponse();
        }
        if (result.getSegmentCount() > 0) {
            // A media-bearing response means the current token works and CDN hops are normal: clear
            // the cumulative redirect and token-refresh budgets so a long session isn't capped.
            redirectCount = 0;
            poTokenRefreshes = 0;
        }
        if (decoded.getBackoffTimeMs() > 0) {
            sleepBackoff(decoded.getBackoffTimeMs());
        }
        return result;
    }

    private void ingestAndCacheSegment(@Nonnull final SabrMediaSegment segment) {
        streamState.ingest(segment);
        final String key = cacheKey(segment);
        final SabrMediaSegment previous = segmentCache.put(key, segment);
        synchronized (segmentAvailable) {
            segmentAvailable.notifyAll();
        }
        if (previous == null && !segment.getHeader().isInitSegment()) {
            cacheOrder.addLast(key);
            cachedBytes += segment.getLength();
            peakCachedBytes = Math.max(peakCachedBytes, cachedBytes);
        }
        if (previous == null) {
            recordTraceSegment(segment);
        }
        // Streaming responses may contain many large completed segments. Evict between segments,
        // not only after the whole response has already reached its peak memory use.
        evictCacheIfNeeded();
    }

    /** Drop the oldest cached media segments (furthest behind the play head) to bound memory. */
    /** Fed by the pump every round: the real player position, used to evict only played segments. */
    public void setPlayHeadMs(final long ms) {
        this.playHeadMs = ms;
    }

    /** Total cached media bytes. The pump throttles on this so high-bitrate (4K) can't OOM the heap. */
    public long getCachedBytes() {
        return cachedBytes;
    }

    public long getPeakCachedBytes() {
        return peakCachedBytes;
    }

    /** Raw bytes consumed from all SABR HTTP response bodies in this session. */
    public long getTotalResponseBytes() {
        return totalResponseBytes;
    }

    /**
     * Drop cached media bytes when the owning media period is released. Init segments are cheap to
     * refetch and keeping old period caches alive during rapid video switches can fill the app heap.
     */
    public void clearCache() {
        segmentCache.clear();
        cacheOrder.clear();
        cachedBytes = 0;
    }

    /**
     * Free already-played segments. The pump calls this every round (not just on fetch): when it is
     * byte-throttled it skips {@link #pumpOnce}, so eviction had to run here too or the cache stayed
     * full forever and throttling never released -> playback froze.
     */
    public void evictPlayed() {
        evictCacheIfNeeded();
    }

    private void evictCacheIfNeeded() {
        while (cachedBytes > MAX_CACHE_BYTES && cacheOrder.size() > MIN_CACHED_SEGMENTS) {
            final String oldKey = cacheOrder.peekFirst();
            if (oldKey == null) {
                break;
            }
            final SabrMediaSegment old = segmentCache.get(oldKey);
            // Never evict a segment the play head hasn't passed yet: dropping it (FIFO did) starves
            // the reader, which is sitting exactly on the oldest seq. Stop here and let the cache ride
            // over budget; the pump's read-ahead cushion bounds how far we get.
            if (old != null) {
                final long endMs = old.getHeader().getStartMs() + old.getHeader().getDurationMs();
                if (endMs > playHeadMs - EVICT_BEHIND_MS) {
                    break;
                }
            }
            cacheOrder.pollFirst();
            final SabrMediaSegment removed = segmentCache.remove(oldKey);
            if (removed != null) {
                cachedBytes -= removed.getLength();
                recordTraceDiscard(removed, "cache_limit");
            }
        }
    }

    /**
     * Collapse the cache to a single {@link #SEEK_KEEP_WINDOW_MS} window around a seek target: drop
     * MEDIA segments ending before {@code fromMs - window} or starting after {@code fromMs + window}.
     * Used by both seek directions so the cache can't hold two disjoint spans over the byte cap (the
     * 4K OOM root cause). Unlike {@link #evictCacheIfNeeded} this does NOT depend on the reader
     * position, so it works even when a track is blocked on the (uncached) seek target, which is what
     * deadlocked a forward seek (old span never freed -> heap full -> pump can't fetch the target ->
     * track stays blocked). The pump re-fetches contiguously, so dropped segments are pure waste.
     * Runs on the pump thread (same as cache mutation), so no extra locking is needed.
     */
    private void evictOutsideSeekWindow(final long fromMs) {
        final long lowMs = fromMs - SEEK_KEEP_WINDOW_MS;
        final long highMs = fromMs + SEEK_KEEP_WINDOW_MS;
        final Iterator<String> it = cacheOrder.iterator();
        while (it.hasNext()) {
            final String key = it.next();
            final SabrMediaSegment seg = segmentCache.get(key);
            if (seg == null) {
                continue;
            }
            final long startMs = seg.getHeader().getStartMs();
            final long endMs = startMs + seg.getHeader().getDurationMs();
            if (endMs < lowMs || startMs > highMs) {
                it.remove();
                segmentCache.remove(key);
                cachedBytes -= seg.getLength();
                recordTraceDiscard(seg, "seek_window");
            }
        }
    }

    @Nullable
    public SabrMediaSegment getCachedSegment(@Nonnull final SabrSegmentRequest request) {
        return segmentCache.get(cacheKey(request));
    }

    /**
     * Wait until a streaming response publishes the requested segment. The cache is checked before
     * and while holding the notification monitor so a segment arriving between those two operations
     * cannot leave the reader sleeping for the full timeout.
     */
    @Nullable
    public SabrMediaSegment awaitCachedSegment(@Nonnull final SabrSegmentRequest request,
                                               final long timeoutMs)
            throws InterruptedException {
        SabrMediaSegment segment = getCachedSegment(request);
        if (segment != null || timeoutMs <= 0) {
            return segment;
        }
        synchronized (segmentAvailable) {
            segment = getCachedSegment(request);
            if (segment == null) {
                segmentAvailable.wait(timeoutMs);
                segment = getCachedSegment(request);
            }
        }
        return segment;
    }

    public void discardCachedSegment(@Nonnull final SabrSegmentRequest request) {
        final String key = cacheKey(request);
        final SabrMediaSegment removed = segmentCache.remove(key);
        if (removed != null && !removed.getHeader().isInitSegment()) {
            cacheOrder.remove(key);
            cachedBytes = Math.max(0, cachedBytes - removed.getLength());
            recordTraceDiscard(removed, "explicit");
        }
    }

    public void setTraceEnabled(final boolean traceEnabled) {
        this.traceEnabled = traceEnabled;
    }

    @Nonnull
    public TraceSnapshot getTraceSnapshot() {
        synchronized (traceLock) {
            return new TraceSnapshot(traceResponseBytes, traceMediaPayloadBytes,
                    traceControlPayloadBytes, traceUmpOverheadBytes, traceDiscardedBytes,
                    requestNumber, cachedBytes, peakCachedBytes,
                    new java.util.ArrayList<>(traceSegments),
                    new java.util.ArrayList<>(traceDiscards));
        }
    }

    private void recordTraceResponse(@Nonnull final YoutubeSabrProbeResult result) {
        if (!traceEnabled) {
            return;
        }
        final long umpOverheadBytes = Math.max(0,
                result.getResponseBytes() - result.getTotalPayloadBytes());
        synchronized (traceLock) {
            traceResponseBytes += result.getResponseBytes();
            traceMediaPayloadBytes += result.getMediaPayloadBytes();
            traceControlPayloadBytes += result.getControlPayloadBytes();
            traceUmpOverheadBytes += umpOverheadBytes;
        }
    }

    private void recordTraceSegment(@Nonnull final SabrMediaSegment segment) {
        if (!traceEnabled) {
            return;
        }
        final SabrMediaHeader header = segment.getHeader();
        final String value = "request=" + requestNumber
                + ",itag=" + header.getItag()
                + (header.isInitSegment()
                ? ",init=true"
                : ",seq=" + header.getSequenceNumber())
                + ",startMs=" + header.getStartMs()
                + ",durationMs=" + header.getDurationMs()
                + ",bytes=" + segment.getLength();
        synchronized (traceLock) {
            addBoundedTraceEvent(traceSegments, value);
        }
    }

    private void recordTraceDiscard(@Nonnull final SabrMediaSegment segment,
                                    @Nonnull final String reason) {
        if (!traceEnabled) {
            return;
        }
        final SabrMediaHeader header = segment.getHeader();
        final long bytes = segment.getLength();
        final String value = "request=" + requestNumber
                + ",reason=" + reason
                + ",itag=" + header.getItag()
                + (header.isInitSegment()
                ? ",init=true"
                : ",seq=" + header.getSequenceNumber())
                + ",startMs=" + header.getStartMs()
                + ",durationMs=" + header.getDurationMs()
                + ",bytes=" + bytes;
        synchronized (traceLock) {
            traceDiscardedBytes += bytes;
            addBoundedTraceEvent(traceDiscards, value);
        }
    }

    private static void addBoundedTraceEvent(@Nonnull final Deque<String> events,
                                             @Nonnull final String value) {
        if (events.size() >= MAX_TRACE_EVENTS) {
            events.removeFirst();
        }
        events.addLast(value);
    }

    public static final class TraceSnapshot {
        private final long responseBytes;
        private final long mediaPayloadBytes;
        private final long controlPayloadBytes;
        private final long umpOverheadBytes;
        private final long discardedBytes;
        private final int requestNumber;
        private final long cachedBytes;
        private final long peakCachedBytes;
        @Nonnull
        private final List<String> segments;
        @Nonnull
        private final List<String> discards;

        private TraceSnapshot(final long responseBytes,
                              final long mediaPayloadBytes,
                              final long controlPayloadBytes,
                              final long umpOverheadBytes,
                              final long discardedBytes,
                              final int requestNumber,
                              final long cachedBytes,
                              final long peakCachedBytes,
                              @Nonnull final List<String> segments,
                              @Nonnull final List<String> discards) {
            this.responseBytes = responseBytes;
            this.mediaPayloadBytes = mediaPayloadBytes;
            this.controlPayloadBytes = controlPayloadBytes;
            this.umpOverheadBytes = umpOverheadBytes;
            this.discardedBytes = discardedBytes;
            this.requestNumber = requestNumber;
            this.cachedBytes = cachedBytes;
            this.peakCachedBytes = peakCachedBytes;
            this.segments = Collections.unmodifiableList(segments);
            this.discards = Collections.unmodifiableList(discards);
        }

        public long getResponseBytes() {
            return responseBytes;
        }

        public long getMediaPayloadBytes() {
            return mediaPayloadBytes;
        }

        public long getControlPayloadBytes() {
            return controlPayloadBytes;
        }

        public long getUmpOverheadBytes() {
            return umpOverheadBytes;
        }

        public long getDiscardedBytes() {
            return discardedBytes;
        }

        public int getRequestNumber() {
            return requestNumber;
        }

        public long getCachedBytes() {
            return cachedBytes;
        }

        public long getPeakCachedBytes() {
            return peakCachedBytes;
        }

        @Nonnull
        public List<String> getSegments() {
            return segments;
        }

        @Nonnull
        public List<String> getDiscards() {
            return discards;
        }
    }


    /** True once the requested media segment is known to be past the last segment of the stream. */
    public boolean isBeyondEnd(@Nonnull final SabrSegmentRequest request) {
        if (request.isInitializationSegment()) {
            return false;
        }
        final long endSegment = streamState.getEndSegment(request.getFormat());
        return endSegment > 0 && request.getSequenceNumber() > endSegment;
    }

    public boolean isComplete() {
        return streamState.isComplete();
    }

    /** True once the server has reported this is a live stream (foundation for live support). */
    public boolean isLive() {
        return streamState.isLive();
    }

    /** Latest segment the live edge has reached, or -1 if unknown / not live. */
    public long getLiveHeadSequenceNumber() {
        return streamState.getLiveHeadSequenceNumber();
    }

    /**
     * True when playback has caught up to the live head: a live-aware pump should wait for the head
     * to advance rather than treating an empty response as the end. Always false for VOD.
     */
    public boolean isAtLiveEdge() {
        return streamState.isAtLiveEdge(audioFormat, videoFormat);
    }

    @Nonnull
    public YoutubeSabrStreamState getStreamState() {
        return streamState;
    }

    public int getRequestNumber() {
        return requestNumber;
    }

    public void prepareForMediaSegment(@Nonnull final SabrSegmentRequest request) {
        if (request.isInitializationSegment()) {
            return;
        }
        final YoutubeSabrFormat targetFormat = request.getFormat();
        final YoutubeSabrFormat companionFormat = getCompanionFormat(targetFormat);
        final long targetStartMs = streamState.getSegmentStartMs(targetFormat,
                request.getSequenceNumber());
        streamState.assumeBufferedUntil(targetFormat, request.getSequenceNumber() - 1);
        streamState.assumeBufferedUntil(companionFormat,
                streamState.getSegmentNumberAtOrAfterTimeMs(companionFormat, targetStartMs));
        streamState.setPlayerTimeMs(targetStartMs);
        streamState.clearPlaybackCookie();
    }

    public void prepareForInitialization(@Nonnull final YoutubeSabrFormat format) {
        discardCachedSegment(SabrSegmentRequest.initialization(format));
        streamState.resetInitialization(format);
        streamState.clearPlaybackCookie();
    }

    @Nonnull
    public byte[] fetchInitializationDataFallback(@Nonnull final YoutubeSabrFormat format,
                                                   @Nonnull final Localization localization)
            throws IOException {
        final String url = format.getInitializationUrl();
        final long start = format.getInitRangeStart();
        final long end = format.getInitRangeEnd();
        if (url == null || url.isEmpty() || start < 0 || end < start
                || end - start >= MAX_INITIALIZATION_BYTES) {
            throw new IOException("Invalid SABR initialization fallback: itag="
                    + format.getItag() + ", start=" + start + ", end=" + end);
        }
        final int length = (int) (end - start + 1);
        final Map<String, List<String>> headers = Collections.singletonMap(
                "Range", Collections.singletonList("bytes=" + start + '-' + end));
        try (StreamingResponse response = NewPipe.getDownloader().getStreaming(
                url, headers, localization)) {
            if (response.responseCode() != 206
                    && !(response.responseCode() == 200 && start == 0)) {
                throw new IOException("SABR initialization fallback failed: itag="
                        + format.getItag() + ", status=" + response.responseCode());
            }
            final byte[] data = readExactly(response.body(), length);
            streamState.ingestInitializationData(format, data);
            addDiagnosticEvent("initialization_fallback itag=" + format.getItag()
                    + " status=" + response.responseCode() + " bytes=" + data.length);
            return data;
        } catch (final ExtractionException e) {
            throw new IOException("SABR initialization fallback failed: itag="
                    + format.getItag(), e);
        }
    }

    @Nonnull
    private static byte[] readExactly(@Nonnull final InputStream input, final int length)
            throws IOException {
        final byte[] data = new byte[length];
        int offset = 0;
        while (offset < length) {
            final int read = input.read(data, offset, length - offset);
            if (read < 0) {
                throw new IOException("Truncated SABR initialization fallback: expected="
                        + length + ", actual=" + offset);
            }
            offset += read;
        }
        return data;
    }

    /**
     * Backward seek onto an already-buffered-past segment: like {@link #prepareForMediaSegment} but
     * SHRINKS the buffered head back to the target so the server re-sends it. assumeBufferedUntil
     * only extends, so prepareForMediaSegment can't rewind (the request comes back empty).
     */
    public void prepareForRewind(@Nonnull final SabrSegmentRequest request) {
        if (request.isInitializationSegment()) {
            return;
        }
        final YoutubeSabrFormat targetFormat = request.getFormat();
        final YoutubeSabrFormat companionFormat = getCompanionFormat(targetFormat);
        final long targetStartMs = streamState.getSegmentStartMs(targetFormat,
                request.getSequenceNumber());
        streamState.rewindBufferedTo(targetFormat, request.getSequenceNumber());
        streamState.rewindBufferedTo(companionFormat,
                streamState.getSegmentNumberAtOrAfterTimeMs(companionFormat, targetStartMs));
        streamState.setPlayerTimeMs(targetStartMs);
        streamState.clearPlaybackCookie();
        // Discard the now-disconnected forward span (old play position) so the cache doesn't hold two
        // disjoint spans over the byte cap -> OOM at 4K. The pump re-fetches forward from the target.
        evictOutsideSeekWindow(targetStartMs);
    }

    /**
     * Forward jump (cold seek far past the buffered edge): the opposite of
     * {@link #prepareForRewind}. {@link #prepareForMediaSegment} only extends maxSegment, so the
     * reported range end (the contiguous edge) stayed behind and the next rounds kept filling the
     * skipped span (ping-pong + duplicate re-sends). This moves the buffered head onto the target
     * so the server streams from there and the edge-driven pacing follows naturally.
     */
    public void prepareForForwardJump(@Nonnull final SabrSegmentRequest request) {
        if (request.isInitializationSegment()) {
            return;
        }
        final YoutubeSabrFormat targetFormat = request.getFormat();
        final YoutubeSabrFormat companionFormat = getCompanionFormat(targetFormat);
        final long targetStartMs = streamState.getSegmentStartMs(targetFormat,
                request.getSequenceNumber());
        streamState.jumpBufferedTo(targetFormat, request.getSequenceNumber());
        streamState.jumpBufferedTo(companionFormat,
                streamState.getSegmentNumberAtOrAfterTimeMs(companionFormat, targetStartMs));
        streamState.setPlayerTimeMs(targetStartMs);
        streamState.clearPlaybackCookie();
        // Drop the old span behind the jump target right away (don't wait for the reader to advance:
        // a track blocked on the uncached target keeps reader_tail stale, so play-head eviction never
        // runs and the heap fills -> stuck buffering / OOM at 4K). The pump fetches from the target.
        evictOutsideSeekWindow(targetStartMs);
    }

    private void failIfKnownOutOfBounds(@Nonnull final SabrSegmentRequest request)
            throws SabrProtocolException {
        if (request.isInitializationSegment()) {
            return;
        }
        final long endSegment = streamState.getEndSegment(request.getFormat());
        if (endSegment > 0 && request.getSequenceNumber() > endSegment) {
            throw new SabrProtocolException("Requested SABR segment is beyond end: "
                    + describeRequest(request) + ", endSegment=" + endSegment);
        }
    }

    private boolean maybePrepareForDistantMediaSegment(
            @Nonnull final SabrSegmentRequest request) {
        if (request.isInitializationSegment() || requestNumber == 0) {
            return false;
        }
        final YoutubeSabrFormat format = request.getFormat();
        if (streamState.getEndSegment(format) <= 0) {
            return false;
        }
        if (request.getSequenceNumber() <= streamState.getMaxSegment(format) + 1) {
            return false;
        }
        prepareForMediaSegment(request);
        return true;
    }

    private boolean recoverFromIncompleteMediaResponse(@Nonnull final Localization localization,
                                                       @Nonnull final SabrDecodedResponse decoded)
            throws IOException, ExtractionException {
        consecutiveIntegrityFailures++;
        if (consecutiveIntegrityFailures >= MAX_INCOMPLETE_MEDIA_RESPONSES) {
            return false;
        }
        if (consecutiveIntegrityFailures >= INTEGRITY_RELOAD_AFTER_FAILURES
                && maybeReload(localization)) {
            return true;
        }
        final int backoffMs = decoded.getBackoffTimeMs() > 0
                ? decoded.getBackoffTimeMs()
                : 500 * consecutiveIntegrityFailures;
        sleepBackoff(backoffMs);
        return true;
    }

    private static boolean isRecoverableIncompleteMediaResponse(
            @Nonnull final List<String> integrityIssues) {
        if (integrityIssues.isEmpty()) {
            return false;
        }
        for (final String issue : integrityIssues) {
            if (!issue.startsWith("length-mismatch:")
                    && !issue.startsWith("missing-media-end:")
                    && !issue.startsWith("missing-media:")
                    && !issue.startsWith("media-without-header:")
                    && !issue.startsWith("media-end-without-header:")) {
                return false;
            }
        }
        return true;
    }

    private static void sleepBackoff(final int backoffTimeMs) throws InterruptedIOException {
        // Clamp to [0, MAX_BACKOFF_MS]: a negative (overflowed varint) must not skip the wait, and
        // a huge server backoff must not be honoured verbatim (would stall playback for minutes).
        final long ms = Math.min(Math.max(0, backoffTimeMs), MAX_BACKOFF_MS);
        if (ms == 0) {
            return;
        }
        try {
            Thread.sleep(ms);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new InterruptedIOException("Interrupted during SABR backoff");
        }
    }

    private boolean maybeApplyPoToken(final boolean forceRefresh)
            throws IOException, ExtractionException {
        if (poTokenProvider == null) {
            return false;
        }
        final byte[] current = streamState.getRawPoToken();
        if (current != null && current.length > 0 && !forceRefresh) {
            return false;
        }
        final byte[] poToken = poTokenProvider.getPoToken(info, streamState, forceRefresh);
        if (poToken != null && poToken.length > 0 && !Arrays.equals(poToken, current)) {
            streamState.setPoToken(poToken);
            return true;
        }
        return false;
    }

    /**
     * Handle a status=3 / no-media response: mint a token, or force a bounded re-mint if we already
     * have one but the server still rejects it (expired mid-playback). Returns false if none could
     * be applied (caller treats that as fatal).
     */
    private boolean applyPoTokenForProtectedResponse() throws IOException, ExtractionException {
        if (maybeApplyPoToken(false)) {
            return true;
        }
        if (poTokenRefreshes < MAX_PO_TOKEN_REFRESHES) {
            // Count the attempt regardless of outcome: a force-refresh triggers a ~45s WebView
            // mint, so we bound them even when the server keeps returning the same rejected token.
            poTokenRefreshes++;
            return maybeApplyPoToken(true);
        }
        return false;
    }

    @Nonnull
    private static String describeRequest(@Nonnull final SabrSegmentRequest request) {
        return "itag=" + request.getFormat().getItag()
                + (request.isInitializationSegment()
                ? ":init"
                : ":seq=" + request.getSequenceNumber());
    }

    @Nonnull
    private static String cacheKey(@Nonnull final SabrSegmentRequest request) {
        return request.getFormat().getItag() + ":"
                + (request.isInitializationSegment()
                ? "init"
                : request.getSequenceNumber());
    }

    @Nonnull
    private static String cacheKey(@Nonnull final SabrMediaSegment segment) {
        final SabrMediaHeader header = segment.getHeader();
        return header.getItag() + ":"
                + (header.isInitSegment() ? "init" : header.getSequenceNumber());
    }

    @Nonnull
    private YoutubeSabrFormat getCompanionFormat(@Nonnull final YoutubeSabrFormat targetFormat) {
        if (targetFormat.getItag() == audioFormat.getItag()) {
            return videoFormat;
        }
        if (targetFormat.getItag() == videoFormat.getItag()) {
            return audioFormat;
        }
        throw new IllegalArgumentException("Unknown SABR itag: " + targetFormat.getItag());
    }
}
