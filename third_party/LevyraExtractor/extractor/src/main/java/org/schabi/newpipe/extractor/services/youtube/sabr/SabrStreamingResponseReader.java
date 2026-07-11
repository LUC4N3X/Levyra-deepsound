package org.schabi.newpipe.extractor.services.youtube.sabr;

import javax.annotation.Nonnull;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Streaming counterpart of {@link SabrResponseDecoder#decode(byte[])}: parse the UMP envelope from a
 * stream one part at a time, assembling MEDIA segments on the fly (via
 * {@link SabrMediaSegmentCollector.Incremental}) so the big MEDIA payloads are never all held at
 * once. Only the small control parts (everything except the MEDIA payloads) are kept and decoded
 * into a {@link SabrDecodedResponse}. This is what fixes the 4K OOM: peak transient drops from the
 * whole response body (50-150MB) to a single in-flight segment.
 */
public final class SabrStreamingResponseReader {
    private SabrStreamingResponseReader() {
    }

    @Nonnull
    public static Result read(@Nonnull final InputStream in)
            throws SabrProtocolException, IOException {
        return read(in, null);
    }

    @FunctionalInterface
    public interface SegmentConsumer {
        void accept(@Nonnull SabrMediaSegment segment) throws SabrProtocolException;
    }

    @FunctionalInterface
    public interface StoppableSegmentConsumer {
        boolean accept(@Nonnull SabrMediaSegment segment) throws SabrProtocolException;
    }

    /**
     * Streams completed segments directly to {@code segmentConsumer}. When a consumer is supplied,
     * completed segments are not retained by the result, bounding the response reader to one open
     * segment instead of the sum of every segment in the response.
     */
    @Nonnull
    public static Result read(@Nonnull final InputStream in,
                              final SegmentConsumer segmentConsumer)
            throws SabrProtocolException, IOException {
        return readUntil(in, segmentConsumer == null ? null : segment -> {
            segmentConsumer.accept(segment);
            return true;
        });
    }

    @Nonnull
    public static Result readUntil(@Nonnull final InputStream in,
                                   final StoppableSegmentConsumer segmentConsumer)
            throws SabrProtocolException, IOException {
        final List<UmpPart> controlParts = new ArrayList<>();
        final List<String> partSummaries = new ArrayList<>();
        final List<SabrMediaSegment> segments = new ArrayList<>();
        final int[] segmentCount = {0};
        final long[] mediaPayloadBytes = {0};
        final long[] mediaPartPayloadBytes = {0};
        final long[] controlPayloadBytes = {0};
        final long[] totalPayloadBytes = {0};
        // headerId -> total media bytes seen, so the decoded response passes the same integrity check
        // (getIntegrityIssues -> "missing-media") as the buffered path WITHOUT retaining the bytes.
        final Map<Integer, Long> mediaBytesByHeaderId = new HashMap<>();
        final SabrMediaSegmentCollector.Incremental collector =
                new SabrMediaSegmentCollector.Incremental();
        UmpReader.readStreamingUntil(in, (type, payload) -> {
            SabrDecodedResponse.addPartSummary(partSummaries, type, payload.length);
            totalPayloadBytes[0] += payload.length;
            switch (type) {
                case SabrResponseDecoder.MEDIA_HEADER:
                    controlPayloadBytes[0] += payload.length;
                    // small (just the header) -> keep so the decoder records it (observeHeader).
                    controlParts.add(new UmpPart(type, payload.length, payload));
                    try {
                        collector.onMediaHeader(payload);
                    } catch (final SabrProtocolException ignored) {
                        // decodeParts records the malformed header. Following MEDIA is deliberately
                        // left without an open header so integrity recovery requests a clean batch.
                    }
                    break;
                case SabrResponseDecoder.MEDIA:
                    // big payload -> assemble into the open segment, do NOT retain.
                    collector.onMedia(payload);
                    mediaPartPayloadBytes[0] += payload.length;
                    if (payload.length > 0) {
                        final int headerId = payload[0] & 0xff;
                        mediaPayloadBytes[0] += payload.length - 1L;
                        mediaBytesByHeaderId.put(headerId,
                                mediaBytesByHeaderId.getOrDefault(headerId, 0L)
                                        + (payload.length - 1L));
                    }
                    break;
                case SabrResponseDecoder.MEDIA_END: {
                    controlPayloadBytes[0] += payload.length;
                    final SabrMediaSegment segment = collector.onMediaEnd(payload);
                    controlParts.add(new UmpPart(type, payload.length, payload));
                    if (segment != null) {
                        segmentCount[0]++;
                        if (segmentConsumer == null) {
                            segments.add(segment);
                        } else {
                            return segmentConsumer.accept(segment);
                        }
                    }
                    break;
                }
                default:
                    controlPayloadBytes[0] += payload.length;
                    controlParts.add(new UmpPart(type, payload.length, payload));
                    break;
            }
            return true;
        });
        final SabrDecodedResponse decoded = SabrResponseDecoder.decodeParts(controlParts);
        decoded.setPartSummaries(partSummaries);
        for (final Map.Entry<Integer, Long> entry : mediaBytesByHeaderId.entrySet()) {
            decoded.addMediaBytes(entry.getKey(), entry.getValue());
        }
        return new Result(decoded, segments, segmentCount[0], mediaPayloadBytes[0],
                mediaPartPayloadBytes[0], controlPayloadBytes[0], totalPayloadBytes[0]);
    }

    /** The decoded control response plus the segments assembled while streaming. */
    public static final class Result {
        @Nonnull
        private final SabrDecodedResponse decodedResponse;
        @Nonnull
        private final List<SabrMediaSegment> segments;
        private final int segmentCount;
        private final long mediaPayloadBytes;
        private final long mediaPartPayloadBytes;
        private final long controlPayloadBytes;
        private final long totalPayloadBytes;

        Result(@Nonnull final SabrDecodedResponse decodedResponse,
               @Nonnull final List<SabrMediaSegment> segments,
               final int segmentCount,
               final long mediaPayloadBytes,
               final long mediaPartPayloadBytes,
               final long controlPayloadBytes,
               final long totalPayloadBytes) {
            this.decodedResponse = decodedResponse;
            this.segments = segments;
            this.segmentCount = segmentCount;
            this.mediaPayloadBytes = mediaPayloadBytes;
            this.mediaPartPayloadBytes = mediaPartPayloadBytes;
            this.controlPayloadBytes = controlPayloadBytes;
            this.totalPayloadBytes = totalPayloadBytes;
        }

        @Nonnull
        public SabrDecodedResponse getDecodedResponse() {
            return decodedResponse;
        }

        @Nonnull
        public List<SabrMediaSegment> getSegments() {
            return segments;
        }

        public int getSegmentCount() {
            return segmentCount;
        }

        public long getMediaPayloadBytes() {
            return mediaPayloadBytes;
        }

        public long getMediaPartPayloadBytes() {
            return mediaPartPayloadBytes;
        }

        public long getControlPayloadBytes() {
            return controlPayloadBytes;
        }

        public long getTotalPayloadBytes() {
            return totalPayloadBytes;
        }
    }
}
