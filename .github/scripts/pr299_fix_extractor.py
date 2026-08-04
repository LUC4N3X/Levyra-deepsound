from pathlib import Path
import re

ROOT = Path('.')


def read(path: str) -> str:
    return (ROOT / path).read_text(encoding='utf-8')


def write(path: str, text: str) -> None:
    (ROOT / path).write_text(text, encoding='utf-8')


def replace_once(text: str, old: str, new: str, label: str) -> str:
    count = text.count(old)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    return text.replace(old, new, 1)


def regex_once(text: str, pattern: str, replacement: str, label: str, flags: int = 0) -> str:
    updated, count = re.subn(pattern, replacement, text, count=1, flags=flags)
    if count != 1:
        raise SystemExit(f'{label}: expected one match, found {count}')
    return updated


# ---------------------------------------------------------------------------
# UMP framing bounds
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/UmpReader.java'
text = read(path)
text = replace_once(
    text,
    'public final class UmpReader {\n    private UmpReader() {',
    'public final class UmpReader {\n'
    '    static final int MAX_UMP_PART_BYTES = 64 * 1024 * 1024;\n\n'
    '    private UmpReader() {',
    'UmpReader part bound constant',
)
text = replace_once(
    text,
    '''            if (type < 0 || size < 0) {
                throw new SabrProtocolException("Invalid UMP part header");
            }
            final BoundedInputStream payload = new BoundedInputStream(in, size);
''',
    '''            if (type < 0 || size < 0) {
                throw new SabrProtocolException("Invalid UMP part header");
            }
            if (size > MAX_UMP_PART_BYTES) {
                throw new SabrProtocolException("UMP part exceeded Host limit: type="
                        + type + ", size=" + size + ", limit=" + MAX_UMP_PART_BYTES);
            }
            final BoundedInputStream payload = new BoundedInputStream(in, size);
''',
    'UmpReader streaming part bound',
)
text = replace_once(
    text,
    '''            if (type < 0 || size < 0) {
                throw new SabrProtocolException("Invalid UMP part header");
            }
            parts.add(new UmpPart(type, size, cursor.readBytes(size)));
''',
    '''            if (type < 0 || size < 0) {
                throw new SabrProtocolException("Invalid UMP part header");
            }
            if (size > MAX_UMP_PART_BYTES) {
                throw new SabrProtocolException("UMP part exceeded Host limit: type="
                        + type + ", size=" + size + ", limit=" + MAX_UMP_PART_BYTES);
            }
            parts.add(new UmpPart(type, size, cursor.readBytes(size)));
''',
    'UmpReader buffered part bound',
)
write(path, text)


# ---------------------------------------------------------------------------
# Streaming response bounds and control accounting
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrStreamingResponseReader.java'
text = read(path)
text = replace_once(
    text,
    '''    private static final int MAX_CONTROL_PARTS = 512;
    private static final long MAX_CONTROL_PAYLOAD_BYTES = 512 * 1024L;
''',
    '''    private static final int MAX_CONTROL_PARTS = 512;
    private static final int MAX_MEDIA_SEGMENTS = 1024;
    private static final long MAX_CONTROL_PAYLOAD_BYTES = 512 * 1024L;
    private static final long MAX_MEDIA_PAYLOAD_BYTES = 256L * 1024 * 1024;
''',
    'streaming response constants',
)
text = replace_once(
    text,
    '''        final int[] segmentCount = {0};
        final long[] mediaPayloadBytes = {0};
''',
    '''        final int[] segmentCount = {0};
        final int[] controlPartCount = {0};
        final long[] mediaPayloadBytes = {0};
''',
    'streaming control counter',
)
old = '''                if (type != mediaProtocol.getMediaPartType()
                        && (controlParts.size() >= MAX_CONTROL_PARTS
                        || controlPayloadBytes[0] + size > MAX_CONTROL_PAYLOAD_BYTES)) {
                    throw new SabrProtocolException("SABR control response exceeded Host limit");
                }
                if (type == mediaProtocol.getHeaderPartType()) {
'''
new = '''                final boolean controlPart = type != mediaProtocol.getMediaPartType()
                        && type != mediaProtocol.getHeaderPartType()
                        && type != mediaProtocol.getEndPartType();
                if (controlPart
                        && (controlPartCount[0] >= MAX_CONTROL_PARTS
                        || controlPayloadBytes[0] + size > MAX_CONTROL_PAYLOAD_BYTES)) {
                    throw new SabrProtocolException("SABR control response exceeded Host limit");
                }
                if (controlPart) {
                    controlPartCount[0]++;
                }
                if (type == mediaProtocol.getMediaPartType()
                        && mediaPartPayloadBytes[0] + size > MAX_MEDIA_PAYLOAD_BYTES) {
                    throw new SabrProtocolException("SABR media response exceeded Host limit: bytes>"
                            + MAX_MEDIA_PAYLOAD_BYTES);
                }
                if (type == mediaProtocol.getHeaderPartType()) {
'''
text = replace_once(text, old, new, 'streaming response bound accounting')
text = replace_once(
    text,
    '''                    if (segment != null) {
                        segmentCount[0]++;
                        maxSegmentBytes[0] = Math.max(maxSegmentBytes[0], segment.getLength());
''',
    '''                    if (segment != null) {
                        segmentCount[0]++;
                        if (segmentCount[0] > MAX_MEDIA_SEGMENTS) {
                            segment.delete();
                            throw new SabrProtocolException(
                                    "SABR response exceeded media segment limit: "
                                            + MAX_MEDIA_SEGMENTS);
                        }
                        maxSegmentBytes[0] = Math.max(maxSegmentBytes[0], segment.getLength());
''',
    'streaming segment count bound',
)
write(path, text)


# ---------------------------------------------------------------------------
# Protection status semantics
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrDecodedResponse.java'
text = read(path)
text = replace_once(
    text,
    '''    public boolean isAttestationRequired() {
        return streamProtectionStatus == SabrStreamProtectionStatus.ATTESTATION_REQUIRED;
    }

    public boolean isAttestationPending() {
        return streamProtectionStatus == SabrStreamProtectionStatus.ATTESTATION_PENDING;
    }
''',
    '''    public boolean isAttestationRequired() {
        return isNoMediaResponse()
                && streamProtectionStatus >= SabrStreamProtectionStatus.ATTESTATION_REQUIRED;
    }

    public boolean isAttestationPending() {
        return isNoMediaResponse()
                && streamProtectionStatus >= SabrStreamProtectionStatus.ATTESTATION_PENDING;
    }
''',
    'stream protection status semantics',
)
write(path, text)


# ---------------------------------------------------------------------------
# Timeout-aware downloader contract
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/downloader/Downloader.java'
text = read(path)
text = replace_once(
    text,
    '''    public StreamingResponse getStreaming(final String url,
                                          @Nullable final Map<String, List<String>> headers,
                                          @Nullable final Localization localization,
                                          final long timeoutMs)
            throws IOException, ReCaptchaException {
        return getStreaming(url, headers, localization);
    }
''',
    '''    public StreamingResponse getStreaming(final String url,
                                          @Nullable final Map<String, List<String>> headers,
                                          @Nullable final Localization localization,
                                          final long timeoutMs)
            throws IOException, ReCaptchaException {
        if (timeoutMs > 0) {
            throw new IOException("Downloader does not implement timeout-aware streaming GET");
        }
        return getStreaming(url, headers, localization);
    }
''',
    'timeout-aware downloader default',
)
write(path, text)


# ---------------------------------------------------------------------------
# State patch collection bounds
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrResponseStatePatch.java'
text = read(path)
text = replace_once(
    text,
    '''    private static final int MAX_FORMATS = 64;
    private static final int MAX_LIVE_METADATA = 16;
    private static final int MAX_CONTEXT_UPDATES = 128;
''',
    '''    private static final int MAX_FORMATS = 64;
    private static final int MAX_LIVE_METADATA = 16;
    private static final int MAX_MEDIA_HEADERS = 512;
    private static final int MAX_CONTEXT_UPDATES = 128;
''',
    'state patch media header bound constant',
)
text = replace_once(
    text,
    '''        if (builder.liveMetadata.size() > MAX_LIVE_METADATA
                || builder.formatMetadata.size() > MAX_FORMATS
                || builder.contextUpdates.size() > MAX_CONTEXT_UPDATES) {
''',
    '''        if (builder.liveMetadata.size() > MAX_LIVE_METADATA
                || builder.formatMetadata.size() > MAX_FORMATS
                || builder.mediaHeaders.size() > MAX_MEDIA_HEADERS
                || builder.contextUpdates.size() > MAX_CONTEXT_UPDATES) {
''',
    'state patch media header bound',
)
write(path, text)


# ---------------------------------------------------------------------------
# Convert policy RuntimeExceptions to extractor-visible protocol failures
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrSessionPolicyHost.java'
text = read(path)
text = replace_once(
    text,
    '''        validateState(state);
        final SabrSessionPolicy.Result result = policy.evaluate(state, event);
        validateResult(state, event, result);
''',
    '''        validateState(state);
        final SabrSessionPolicy.Result result;
        try {
            result = policy.evaluate(state, event);
        } catch (final RuntimeException error) {
            throw new SabrProtocolException("SABR policy evaluation failed", error);
        }
        try {
            validateResult(state, event, result);
        } catch (final RuntimeException error) {
            throw new SabrProtocolException("SABR policy returned invalid state", error);
        }
''',
    'policy runtime failure wrapping',
)
write(path, text)


# ---------------------------------------------------------------------------
# Media header normalization and validation
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrMediaHeader.java'
text = read(path)
old = '''        return new SabrMediaHeader(headerId, videoId, itag, lastModified, xtags, startRange,
                compressionAlgorithm, initSegment, sequenceNumber, bitrateBps, startMs, durationMs,
                contentLength, timeRangeStartTicks, timeRangeDurationTicks, timeRangeTimescale,
                sequenceLastModified);
    }

    @Nonnull
    static SabrMediaHeader decode'''
new = '''        long normalizedStartMs = startMs;
        long normalizedDurationMs = durationMs;
        final boolean hasTimeRange = timeRangeStartTicks >= 0 || timeRangeDurationTicks >= 0;
        if (hasTimeRange && timeRangeTimescale <= 0) {
            throw new IllegalArgumentException("SABR media time range requires a positive timescale");
        }
        if (timeRangeTimescale > 0) {
            if (normalizedStartMs < 0 && timeRangeStartTicks >= 0) {
                normalizedStartMs = timeRangeStartTicks * 1000L / timeRangeTimescale;
            }
            if (normalizedDurationMs < 0 && timeRangeDurationTicks >= 0) {
                normalizedDurationMs = timeRangeDurationTicks * 1000L / timeRangeTimescale;
            }
        }
        if (normalizedStartMs < -1 || normalizedDurationMs < -1 || contentLength < -1) {
            throw new IllegalArgumentException("Invalid SABR media header range");
        }
        return new SabrMediaHeader(headerId, videoId, itag, lastModified, xtags, startRange,
                compressionAlgorithm, initSegment, sequenceNumber, bitrateBps,
                normalizedStartMs, normalizedDurationMs, contentLength, timeRangeStartTicks,
                timeRangeDurationTicks, timeRangeTimescale, sequenceLastModified);
    }

    @Nonnull
    static SabrMediaHeader decode'''
text = replace_once(text, old, new, 'media header normalized validation')
write(path, text)


# ---------------------------------------------------------------------------
# Progressive file safety, bounded legacy materialization and deferred deletion
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrMediaSegment.java'
text = read(path)
text = replace_once(
    text,
    '''import java.io.RandomAccessFile;
''',
    '''import java.io.RandomAccessFile;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
''',
    'media segment concurrency imports',
)
text = replace_once(
    text,
    '''public final class SabrMediaSegment {
    private static final int COPY_BUFFER_SIZE = 8192;
''',
    '''public final class SabrMediaSegment {
    private static final int COPY_BUFFER_SIZE = 8192;
    private static final int MAX_LEGACY_MATERIALIZATION_BYTES = 8 * 1024 * 1024;
    private static final long PROGRESSIVE_WAIT_SLICE_MS = 250L;
    private static final long PROGRESSIVE_STALL_TIMEOUT_MS = 30_000L;
    private static final long DELETE_GRACE_MS = 5_000L;
    private static final ScheduledExecutorService DELETE_EXECUTOR =
            Executors.newSingleThreadScheduledExecutor(runnable -> {
                final Thread thread = new Thread(runnable, "sabr-spool-cleaner");
                thread.setDaemon(true);
                return thread;
            });
''',
    'media segment safety constants',
)
text = replace_once(
    text,
    '''    @Nullable
    private final ProgressiveFileState progressiveState;
''',
    '''    @Nullable
    private final ProgressiveFileState progressiveState;
    private final AtomicBoolean deleteScheduled = new AtomicBoolean(false);
''',
    'media segment deletion flag',
)
text = replace_once(
    text,
    '''        if (data != null) {
            return data;
        }
        try (InputStream input = openStream();
''',
    '''        if (data != null) {
            return data;
        }
        if (length > MAX_LEGACY_MATERIALIZATION_BYTES) {
            throw new IllegalStateException("Disk-backed SABR segment is too large for getData(); "
                    + "use openStream(): bytes=" + length);
        }
        try (InputStream input = openStream();
''',
    'bounded legacy getData',
)
text = replace_once(
    text,
    '''    public void delete() {
        failProgressive(new IOException("SABR media segment was discarded"));
        if (file != null && !file.delete() && file.exists()) {
            file.deleteOnExit();
        }
    }
''',
    '''    public void delete() {
        failProgressive(new IOException("SABR media segment was discarded"));
        if (file != null && deleteScheduled.compareAndSet(false, true)) {
            DELETE_EXECUTOR.schedule(() -> {
                if (file.exists() && !file.delete()) {
                    file.setLastModified(0L);
                }
            }, DELETE_GRACE_MS, TimeUnit.MILLISECONDS);
        }
    }
''',
    'deferred spool deletion',
)
old = '''            int readable = readableBytes(position);
            while (readable <= 0 && !complete && failure == null && !reader.closed) {
                try {
                    wait();
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    final InterruptedIOException interrupted = new InterruptedIOException(
                            "Interrupted waiting for SABR media bytes");
                    interrupted.initCause(e);
                    throw interrupted;
                }
                readable = readableBytes(position);
            }
'''
new = '''            int readable = readableBytes(position);
            int observedBytes = bytesWritten;
            long stallDeadlineNs = System.nanoTime()
                    + TimeUnit.MILLISECONDS.toNanos(PROGRESSIVE_STALL_TIMEOUT_MS);
            while (readable <= 0 && !complete && failure == null && !reader.closed) {
                try {
                    wait(PROGRESSIVE_WAIT_SLICE_MS);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                    final InterruptedIOException interrupted = new InterruptedIOException(
                            "Interrupted waiting for SABR media bytes");
                    interrupted.initCause(e);
                    throw interrupted;
                }
                if (bytesWritten != observedBytes) {
                    observedBytes = bytesWritten;
                    stallDeadlineNs = System.nanoTime()
                            + TimeUnit.MILLISECONDS.toNanos(PROGRESSIVE_STALL_TIMEOUT_MS);
                } else if (System.nanoTime() >= stallDeadlineNs) {
                    failure = new IOException("SABR progressive media producer stalled for "
                            + PROGRESSIVE_STALL_TIMEOUT_MS + " ms");
                    notifyAll();
                    throw failure;
                }
                readable = readableBytes(position);
            }
'''
text = replace_once(text, old, new, 'progressive stall timeout')
text = replace_once(
    text,
    '''        private long position;
        private volatile boolean closed;
''',
    '''        private final Object inputLock = new Object();
        private long position;
        private volatile boolean closed;
''',
    'progressive input lock',
)
old = '''                input.seek(position);
                final int read = input.read(bytes, offset, Math.min(count, available));
                if (read > 0) {
                    position += read;
                    return read;
                }
'''
new = '''                final int read;
                synchronized (inputLock) {
                    if (closed) {
                        throw new IOException("SABR media stream is closed");
                    }
                    input.seek(position);
                    read = input.read(bytes, offset, Math.min(count, available));
                }
                if (read > 0) {
                    position += read;
                    return read;
                }
                if (read < 0) {
                    throw new IOException("SABR spool file is shorter than advertised: position="
                            + position + ", available=" + available);
                }
'''
text = replace_once(text, old, new, 'progressive read EOF and locking')
text = replace_once(
    text,
    '''            position += skipped;
            input.seek(position);
            return skipped;
''',
    '''            position += skipped;
            synchronized (inputLock) {
                if (closed) {
                    throw new IOException("SABR media stream is closed");
                }
                input.seek(position);
            }
            return skipped;
''',
    'progressive skip locking',
)
text = replace_once(
    text,
    '''        public void close() throws IOException {
            if (!closed) {
                closed = true;
                state.signalReaders();
                input.close();
            }
        }
''',
    '''        public void close() throws IOException {
            if (!closed) {
                closed = true;
                state.signalReaders();
                synchronized (inputLock) {
                    input.close();
                }
            }
        }
''',
    'progressive close locking',
)
write(path, text)


# ---------------------------------------------------------------------------
# Segment collector memory bounds and stale spool cleanup
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/SabrMediaSegmentCollector.java'
text = read(path)
text = replace_once(
    text,
    '''public final class SabrMediaSegmentCollector {
    private static final int MIN_PROGRESSIVE_SEGMENT_BYTES = 64 * 1024;
''',
    '''public final class SabrMediaSegmentCollector {
    private static final int MIN_PROGRESSIVE_SEGMENT_BYTES = 64 * 1024;
    private static final int MAX_IN_MEMORY_SEGMENT_BYTES = 32 * 1024 * 1024;
    private static final long STALE_SPOOL_AGE_MS = 24L * 60 * 60 * 1000;
    private static long lastSpoolCleanupMs;
''',
    'collector memory constants',
)
text = replace_once(
    text,
    '''        public Incremental(@Nullable final File spoolDirectory,
                           @Nonnull final SabrMediaProtocol mediaProtocol) {
            this.spoolDirectory = spoolDirectory;
            this.mediaProtocol = mediaProtocol;
        }
''',
    '''        public Incremental(@Nullable final File spoolDirectory,
                           @Nonnull final SabrMediaProtocol mediaProtocol) {
            this.spoolDirectory = spoolDirectory;
            this.mediaProtocol = mediaProtocol;
            cleanupStaleSpoolFiles(spoolDirectory);
        }
''',
    'collector stale cleanup call',
)
text = replace_once(
    text,
    '''            } else if (contentLength >= 0) {
                if (contentLength > Integer.MAX_VALUE) {
                    throw new SabrProtocolException("SABR media segment too large: headerId="
                            + header.getHeaderId() + ", length=" + contentLength);
                }
                fixedData = new byte[(int) contentLength];
''',
    '''            } else if (contentLength >= 0) {
                if (contentLength > MAX_IN_MEMORY_SEGMENT_BYTES) {
                    throw new SabrRecoverableException(
                            "SABR in-memory media segment exceeded Host limit: headerId="
                                    + header.getHeaderId() + ", length=" + contentLength
                                    + ", limit=" + MAX_IN_MEMORY_SEGMENT_BYTES);
                }
                fixedData = new byte[(int) contentLength];
''',
    'collector known length memory bound',
)
text = replace_once(
    text,
    '''        private void ensureLengthFits(final int count) throws SabrProtocolException {
            if (length > Integer.MAX_VALUE - count) {
                throw new SabrProtocolException("SABR media segment too large: headerId="
                        + header.getHeaderId() + ", length>" + Integer.MAX_VALUE);
            }
        }
''',
    '''        private void ensureLengthFits(final int count) throws SabrProtocolException {
            if (length > Integer.MAX_VALUE - count) {
                throw new SabrProtocolException("SABR media segment too large: headerId="
                        + header.getHeaderId() + ", length>" + Integer.MAX_VALUE);
            }
            if (fileOutput == null
                    && length + (long) count > MAX_IN_MEMORY_SEGMENT_BYTES) {
                throw new SabrRecoverableException(
                        "SABR in-memory media segment exceeded Host limit: headerId="
                                + header.getHeaderId() + ", limit="
                                + MAX_IN_MEMORY_SEGMENT_BYTES + ", actual>="
                                + (length + (long) count));
            }
        }
''',
    'collector dynamic memory bound',
)
insert_marker = '''    private static void readFully(@Nonnull final InputStream input,
'''
cleanup = '''    private static synchronized void cleanupStaleSpoolFiles(
            @Nullable final File spoolDirectory) {
        if (spoolDirectory == null || !spoolDirectory.isDirectory()) {
            return;
        }
        final long now = System.currentTimeMillis();
        if (now - lastSpoolCleanupMs < 60L * 60 * 1000) {
            return;
        }
        lastSpoolCleanupMs = now;
        final File[] files = spoolDirectory.listFiles((directory, name) ->
                name.startsWith("sabr-") && name.endsWith(".seg"));
        if (files == null) {
            return;
        }
        for (final File file : files) {
            if (now - file.lastModified() >= STALE_SPOOL_AGE_MS
                    && !file.delete()) {
                file.setLastModified(0L);
            }
        }
    }

'''
text = replace_once(text, insert_marker, cleanup + insert_marker, 'collector stale cleanup helper')
write(path, text)


# ---------------------------------------------------------------------------
# Lowest-video selection must ignore sentinel heights
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/YoutubeSabrInfo.java'
text = read(path)
text = replace_once(
    text,
    '''    public YoutubeSabrFormat findLowestVideoFormat() {
        YoutubeSabrFormat lowest = null;
        for (final YoutubeSabrFormat format : formats) {
            if (format.isVideo() && (lowest == null || format.getHeight() < lowest.getHeight())) {
                lowest = format;
            }
        }
        return lowest;
    }
''',
    '''    public YoutubeSabrFormat findLowestVideoFormat() {
        YoutubeSabrFormat fallback = null;
        YoutubeSabrFormat lowest = null;
        for (final YoutubeSabrFormat format : formats) {
            if (!format.isVideo()) {
                continue;
            }
            if (fallback == null) {
                fallback = format;
            }
            if (format.getHeight() <= 0) {
                continue;
            }
            if (lowest == null || format.getHeight() < lowest.getHeight()) {
                lowest = format;
            }
        }
        return lowest == null ? fallback : lowest;
    }
''',
    'lowest video sentinel handling',
)
write(path, text)


# ---------------------------------------------------------------------------
# Probe must forward requested spool directory on every path
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/YoutubeSabrProbe.java'
text = read(path)
text = replace_once(
    text,
    '''                            ? SabrStreamingResponseReader.readUntil(body, null, null, null,
                            mediaProtocol)
''',
    '''                            ? SabrStreamingResponseReader.readUntil(body, null, null,
                            segmentSpoolDirectory, mediaProtocol)
''',
    'probe no-consumer spool directory',
)
write(path, text)


# ---------------------------------------------------------------------------
# Session lifecycle, backoff, cleanup, URL validation and rotation budgets
# ---------------------------------------------------------------------------
path = 'third_party/LevyraExtractor/extractor/src/main/java/org/schabi/newpipe/extractor/services/youtube/sabr/YoutubeSabrSession.java'
text = read(path)
text = replace_once(
    text,
    '''        final long remainingBackoffMs = getDemandBackoffRemainingMs();
        if (remainingBackoffMs > 0) {
            return DemandResponseResult.NO_REQUEST;
        }
''',
    '''        long remainingBackoffMs;
        while ((remainingBackoffMs = getDemandBackoffRemainingMs()) > 0) {
            sleepBackoff((int) remainingBackoffMs, false);
        }
''',
    'demand backoff busy loop',
)
text = replace_once(
    text,
    '''    public void clearCache() {
        demandBackoffUntilNs = 0;
        cacheClosed = true;
        sessionPolicyHost.close();
        abortInFlightSegments("SABR session cache was cleared", null);
        for (final SabrMediaSegment segment : segmentCache.values()) {
            segment.delete();
        }
        segmentCache.clear();
        cacheOrder.clear();
        cachedBytes = 0;
    }
''',
    '''    public void clearCache() {
        demandBackoffUntilNs = 0;
        abortInFlightSegments("SABR session cache was cleared", null);
        for (final SabrMediaSegment segment : segmentCache.values()) {
            segment.delete();
        }
        segmentCache.clear();
        cacheOrder.clear();
        cachedBytes = 0;
        synchronized (segmentAvailable) {
            segmentAvailable.notifyAll();
        }
    }

    /** Permanently close this session and release policy and media resources. */
    public void close() {
        if (cacheClosed) {
            return;
        }
        cacheClosed = true;
        clearCache();
        sessionPolicyHost.close();
    }
''',
    'repeatable clearCache and terminal close',
)
text = replace_once(
    text,
    '''        final SabrMediaSegment removed = segmentCache.remove(key);
        if (removed != null && !removed.getHeader().isInitSegment()) {
            cacheOrder.remove(key);
            cachedBytes = Math.max(0, cachedBytes - removed.getLength());
            recordTraceDiscard(removed, "explicit");
            removed.delete();
        }
''',
    '''        final SabrMediaSegment removed = segmentCache.remove(key);
        if (removed != null) {
            if (!removed.getHeader().isInitSegment()) {
                cacheOrder.remove(key);
                cachedBytes = Math.max(0, cachedBytes - removed.getLength());
            }
            recordTraceDiscard(removed, "explicit");
            removed.delete();
        }
''',
    'initialization segment deletion',
)
text = replace_once(
    text,
    '''        if (initializationUrl == null || initializationUrl.isEmpty() || start < 0 || end < start
                || end - start >= MAX_INITIALIZATION_BYTES) {
''',
    '''        if (initializationUrl == null || initializationUrl.isEmpty() || start < 0 || end < start
                || end - start >= MAX_INITIALIZATION_BYTES) {
''',
    'initialization range anchor',
)
text = replace_once(
    text,
    '''        if (poToken.length == 0) {
            throw new IOException("Missing PO token for SABR initialization range: itag="
                    + format.getItag());
        }
        final String url = appendQueryParameterIfMissing(initializationUrl, "pot",
''',
    '''        validateSensitiveGoogleVideoUrl(initializationUrl);
        if (poToken.length == 0) {
            throw new IOException("Missing PO token for SABR initialization range: itag="
                    + format.getItag());
        }
        final String url = appendQueryParameterIfMissing(initializationUrl, "pot",
''',
    'initialization URL validation',
)
helper_marker = '''    @Nonnull
    private static String appendQueryParameterIfMissing'''
helper = '''    private static void validateSensitiveGoogleVideoUrl(@Nonnull final String url)
            throws IOException {
        try {
            final URI uri = URI.create(url);
            final String host = uri.getHost();
            if (!"https".equalsIgnoreCase(uri.getScheme()) || host == null
                    || !(host.equals("googlevideo.com")
                    || host.endsWith(".googlevideo.com"))) {
                throw new IOException("SABR token URL escaped the GoogleVideo Host");
            }
        } catch (final IllegalArgumentException error) {
            throw new IOException("Malformed SABR token URL", error);
        }
    }

'''
text = replace_once(text, helper_marker, helper + helper_marker, 'sensitive URL helper')
# Increment rotation budget only after the provider actually invalidates its identity.
text = regex_once(
    text,
    r'(private boolean rotatePendingAttestationIdentity\([^\{]+\{.*?)(\s*)attestationIdentityRotations\+\+;(.*?invalidatePoTokenIdentity\(info\).*?\{\s*return false;\s*\})',
    lambda match: match.group(1) + match.group(2) + match.group(3)
    + match.group(2) + 'attestationIdentityRotations++;',
    'attestation rotation budget',
    flags=re.S,
)
write(path, text)


# ---------------------------------------------------------------------------
# Regression tests for bounds and status semantics
# ---------------------------------------------------------------------------
new_test = ROOT / 'third_party/LevyraExtractor/extractor/src/test/java/org/schabi/newpipe/extractor/services/youtube/sabr/Pr299HardeningTest.java'
new_test.parent.mkdir(parents=True, exist_ok=True)
new_test.write_text('''package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.grack.nanojson.JsonParser;
import org.junit.jupiter.api.Test;
import org.schabi.newpipe.extractor.downloader.CancellableCall;
import org.schabi.newpipe.extractor.downloader.Downloader;
import org.schabi.newpipe.extractor.downloader.Request;
import org.schabi.newpipe.extractor.downloader.Response;
import org.schabi.newpipe.extractor.localization.Localization;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class Pr299HardeningTest {
    @Test
    void rejectsOversizedUmpPartBeforeReadingPayload() {
        final int oversized = UmpReader.MAX_UMP_PART_BYTES + 1;
        final byte[] envelope = new byte[]{1, (byte) 240,
                (byte) oversized, (byte) (oversized >>> 8),
                (byte) (oversized >>> 16), (byte) (oversized >>> 24)};

        assertThrows(SabrProtocolException.class,
                () -> UmpReader.readPayloadsUntil(new ByteArrayInputStream(envelope),
                        (type, size, payload) -> true));
    }

    @Test
    void protectionStatusOnlyBlocksNoMediaAndFutureStatusesRemainProtected() {
        final SabrDecodedResponse mediaBearing = new SabrDecodedResponse();
        mediaBearing.setStreamProtectionStatus(
                SabrStreamProtectionStatus.ATTESTATION_REQUIRED);
        mediaBearing.addMediaHeader(SabrMediaHeader.normalized(1, "video", 299, 1,
                null, 0, 0, false, 1, 1_000_000, 0, 1_000, 1,
                0, 1_000, 1_000, 1));
        assertFalse(mediaBearing.isAttestationRequired());
        assertFalse(mediaBearing.isAttestationPending());

        final SabrDecodedResponse futureNoMedia = new SabrDecodedResponse();
        futureNoMedia.setStreamProtectionStatus(
                SabrStreamProtectionStatus.ATTESTATION_REQUIRED + 1);
        assertTrue(futureNoMedia.isAttestationRequired());
        assertTrue(futureNoMedia.isAttestationPending());
    }

    @Test
    void normalizedHeaderDerivesMillisecondsFromPositiveTimeRange() {
        final SabrMediaHeader header = SabrMediaHeader.normalized(1, "video", 299, 1,
                null, 0, 0, false, 1, 1_000_000, -1, -1, 10,
                2_000, 1_000, 1_000, 1);
        assertEquals(2_000, header.getStartMs());
        assertEquals(1_000, header.getDurationMs());
        assertThrows(IllegalArgumentException.class,
                () -> SabrMediaHeader.normalized(1, "video", 299, 1,
                        null, 0, 0, false, 1, 1_000_000, -1, -1, 10,
                        2_000, 1_000, 0, 1));
    }

    @Test
    void lowestVideoIgnoresUnknownHeightButRetainsFallback() throws Exception {
        final List<YoutubeSabrFormat> formats = YoutubeSabrFormat.fromAdaptiveFormats("video",
                JsonParser.array().from("["
                        + "{\\"itag\\":1,\\"mimeType\\":\\"video/mp4\\",\\"bitrate\\":1},"
                        + "{\\"itag\\":2,\\"mimeType\\":\\"video/mp4\\",\\"height\\":720,\\"bitrate\\":2},"
                        + "{\\"itag\\":3,\\"mimeType\\":\\"video/mp4\\",\\"height\\":360,\\"bitrate\\":3}]"));
        final YoutubeSabrInfo info = new YoutubeSabrInfo(YoutubeSabrClientProfile.ANDROID,
                "video", "cpn", "1", "visitor", "https://example.com/sabr", "config",
                formats, true);
        assertEquals(360, info.findLowestVideoFormat().getHeight());
    }

    @Test
    void timeoutAwareStreamingDefaultCannotSilentlyIgnoreDeadline() {
        final Downloader downloader = new Downloader() {
            @Override
            public Response execute(final Request request) {
                return new Response(200, "OK", null, "", new byte[0], request.url());
            }

            @Override
            public CancellableCall executeAsync(final Request request,
                                                final AsyncCallback callback) {
                throw new UnsupportedOperationException();
            }
        };
        assertThrows(IOException.class, () -> downloader.getStreaming(
                "https://example.com", null, Localization.DEFAULT, 100));
    }

    @Test
    void mediaHeaderCollectionIsBounded() {
        final SabrResponseStatePatch.Builder builder = SabrResponseStatePatch.builder();
        for (int i = 0; i < 513; i++) {
            builder.addMediaHeader(SabrMediaHeader.normalized(i & 0xff, "video", 299, 1,
                    null, 0, 0, false, i, 1_000_000, 0, 1_000, 1,
                    -1, -1, -1, 1));
        }
        assertThrows(IllegalArgumentException.class, builder::build);
    }
}
''', encoding='utf-8')

print('Extractor hardening patch staged successfully')
