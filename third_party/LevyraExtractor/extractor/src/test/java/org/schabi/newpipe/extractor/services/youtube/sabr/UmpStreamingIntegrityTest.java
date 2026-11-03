package org.schabi.newpipe.extractor.services.youtube.sabr;

import org.junit.jupiter.api.Test;

import java.io.ByteArrayInputStream;
import java.io.EOFException;
import java.io.InputStream;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class UmpStreamingIntegrityTest {
    @Test
    void payloadMakesProgressWhenBulkReadReturnsZero() {
        final InputStream source = new ByteArrayInputStream(new byte[]{21, 3, 1, 2, 3}) {
            @Override
            public synchronized int read(final byte[] target, final int offset, final int length) {
                return length == 0 ? 0 : available() == 0 ? -1 : 0;
            }
        };
        assertTimeoutPreemptively(Duration.ofSeconds(1), () ->
                UmpReader.readStreaming(source, (type, payload) ->
                        assertArrayEquals(new byte[]{1, 2, 3}, payload)));
    }

    @Test
    void unreadPayloadIsDrainedWithoutLosingTheNextHeader() throws Exception {
        final List<Integer> types = new ArrayList<>();
        UmpReader.readPayloadsUntil(new ByteArrayInputStream(
                new byte[]{21, 3, 1, 2, 3, 22, 1, 9}), (type, size, payload) -> {
                    types.add(type);
                    assertEquals(type == 21 ? 1 : 9, payload.read());
                    return true;
                });
        assertEquals(List.of(21, 22), types);
    }

    @Test
    void emptyReadAtPayloadBoundaryObeysInputStreamContract() throws Exception {
        UmpReader.readPayloadsUntil(new ByteArrayInputStream(new byte[]{22, 0}),
                (type, size, payload) -> {
                    assertEquals(0, payload.read(new byte[1], 0, 0));
                    assertEquals(-1, payload.read());
                    return true;
                });
    }

    @Test
    void truncatedPayloadAndTruncatedHeaderFail() {
        for (final byte[] bytes : List.of(new byte[]{21, 3, 1}, new byte[]{21},
                new byte[]{(byte) 0xF0, 0})) {
            assertThrows(EOFException.class, () -> UmpReader.readStreaming(
                    new ByteArrayInputStream(bytes), (type, payload) -> fail("incomplete part")));
        }
    }

    @Test
    void interruptionIsPreserved() {
        Thread.currentThread().interrupt();
        try {
            assertThrows(java.io.IOException.class, () -> UmpReader.readStreaming(
                    new ByteArrayInputStream(new byte[]{22, 0}), (type, payload) -> fail()));
            assertTrue(Thread.currentThread().isInterrupted());
        } finally {
            Thread.interrupted();
        }
    }
}
