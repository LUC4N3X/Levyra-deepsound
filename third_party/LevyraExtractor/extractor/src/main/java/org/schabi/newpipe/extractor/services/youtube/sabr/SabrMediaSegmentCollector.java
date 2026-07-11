package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.brotli.dec.BrotliInputStream;

import javax.annotation.Nonnull;
import javax.annotation.Nullable;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.zip.GZIPInputStream;

public final class SabrMediaSegmentCollector {
    private SabrMediaSegmentCollector() {
    }

    @Nonnull
    public static List<SabrMediaSegment> collect(@Nonnull final SabrDecodedResponse response)
            throws SabrProtocolException {
        final List<SabrMediaSegment> segments = new ArrayList<>();
        final Map<Integer, OpenSegment> openSegments = new HashMap<>();
        for (final UmpPart part : response.getParts()) {
            final byte[] partData = part.getRawData();
            switch (part.getType()) {
                case SabrResponseDecoder.MEDIA_HEADER:
                    final SabrMediaHeader header = SabrMediaHeader.decode(partData);
                    openSegments.put(header.getHeaderId(), new OpenSegment(header));
                    break;
                case SabrResponseDecoder.MEDIA:
                    if (partData.length > 0) {
                        final OpenSegment openSegment = openSegments.get(partData[0] & 0xff);
                        if (openSegment != null) {
                            openSegment.write(partData, 1, partData.length - 1);
                        }
                    }
                    break;
                case SabrResponseDecoder.MEDIA_END:
                    if (partData.length > 0) {
                        final OpenSegment openSegment = openSegments.remove(partData[0] & 0xff);
                        if (openSegment != null) {
                            segments.add(openSegment.toSegment());
                        }
                    }
                    break;
                default:
                    break;
            }
        }
        return segments;
    }

    @Nullable
    public static SabrMediaSegment find(@Nonnull final SabrDecodedResponse response,
                                        @Nonnull final SabrSegmentRequest request)
            throws SabrProtocolException {
        for (final SabrMediaSegment segment : collect(response)) {
            if (request.matches(segment.getHeader())) {
                return segment;
            }
        }
        return null;
    }

    /**
     * Incremental collector for the streaming path: feed MEDIA_HEADER / MEDIA / MEDIA_END parts as
     * they arrive and get each completed segment back from {@link #onMediaEnd}, so the caller never
     * has to retain all the MEDIA parts at once (that whole-body buffering was the 4K OOM).
     */
    public static final class Incremental {
        private final Map<Integer, OpenSegment> openSegments = new HashMap<>();

        public void onMediaHeader(@Nonnull final byte[] partData) throws SabrProtocolException {
            final SabrMediaHeader header = SabrMediaHeader.decode(partData);
            openSegments.put(header.getHeaderId(), new OpenSegment(header));
        }

        public void onMedia(@Nonnull final byte[] partData) throws SabrProtocolException {
            if (partData.length > 0) {
                final OpenSegment openSegment = openSegments.get(partData[0] & 0xff);
                if (openSegment != null) {
                    openSegment.write(partData, 1, partData.length - 1);
                }
            }
        }

        @Nullable
        public SabrMediaSegment onMediaEnd(@Nonnull final byte[] partData)
                throws SabrProtocolException {
            if (partData.length > 0) {
                final OpenSegment openSegment = openSegments.remove(partData[0] & 0xff);
                if (openSegment != null) {
                    return openSegment.toSegment();
                }
            }
            return null;
        }
    }

    private static final class OpenSegment {
        @Nonnull
        private final SabrMediaHeader header;
        @Nullable
        private final byte[] fixedData;
        @Nullable
        private final ByteArrayOutputStream dynamicData;
        private int length;

        private OpenSegment(@Nonnull final SabrMediaHeader header) throws SabrProtocolException {
            this.header = header;
            final long contentLength = header.getContentLength();
            if (contentLength >= 0) {
                if (contentLength > Integer.MAX_VALUE) {
                    throw new SabrProtocolException("SABR media segment too large: headerId="
                            + header.getHeaderId() + ", length=" + contentLength);
                }
                fixedData = new byte[(int) contentLength];
                dynamicData = null;
            } else {
                fixedData = null;
                dynamicData = new ByteArrayOutputStream();
            }
        }

        private void write(@Nonnull final byte[] bytes, final int offset, final int count)
                throws SabrProtocolException {
            if (count <= 0) {
                return;
            }
            if (fixedData != null) {
                if (length + count > fixedData.length) {
                    throw new SabrRecoverableException("SABR media length overflow: headerId="
                            + header.getHeaderId()
                            + ", expected=" + fixedData.length
                            + ", actual>=" + (length + count));
                }
                System.arraycopy(bytes, offset, fixedData, length, count);
            } else {
                dynamicData.write(bytes, offset, count);
            }
            length += count;
        }

        @Nonnull
        private SabrMediaSegment toSegment() throws SabrProtocolException {
            final byte[] rawBytes;
            if (fixedData != null) {
                rawBytes = fixedData;
            } else {
                rawBytes = dynamicData.toByteArray();
            }
            if (header.getContentLength() >= 0 && length != header.getContentLength()) {
                throw new SabrRecoverableException("SABR media length mismatch: headerId="
                        + header.getHeaderId()
                        + ", expected=" + header.getContentLength()
                        + ", actual=" + length);
            }
            return new SabrMediaSegment(header, maybeDecompress(header, rawBytes));
        }

        @Nonnull
        private static byte[] maybeDecompress(@Nonnull final SabrMediaHeader header,
                                              @Nonnull final byte[] bytes)
                throws SabrProtocolException {
            final int compressionAlgorithm = header.getCompressionAlgorithm();
            if (compressionAlgorithm <= 0) {
                return bytes;
            }
            if (compressionAlgorithm == 1) {
                return gunzip(bytes);
            }
            if (compressionAlgorithm == 2) {
                return brotli(bytes);
            }
            throw new SabrProtocolException("Unsupported SABR media compression: "
                    + compressionAlgorithm);
        }

        @Nonnull
        private static byte[] gunzip(@Nonnull final byte[] bytes) throws SabrProtocolException {
            try (GZIPInputStream input = new GZIPInputStream(new ByteArrayInputStream(bytes));
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            } catch (final IOException e) {
                throw new SabrRecoverableException(
                        "Could not decompress gzip SABR media segment", e);
            }
        }

        @Nonnull
        private static byte[] brotli(@Nonnull final byte[] bytes) throws SabrProtocolException {
            try (BrotliInputStream input = new BrotliInputStream(new ByteArrayInputStream(bytes));
                 ByteArrayOutputStream output = new ByteArrayOutputStream()) {
                final byte[] buffer = new byte[8192];
                int read;
                while ((read = input.read(buffer)) != -1) {
                    output.write(buffer, 0, read);
                }
                return output.toByteArray();
            } catch (final IOException e) {
                throw new SabrRecoverableException(
                        "Could not decompress brotli SABR media segment", e);
            }
        }
    }
}
