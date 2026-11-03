package org.schabi.newpipe.extractor.services.youtube.sabr;

import com.sun.management.ThreadMXBean;
import java.io.ByteArrayInputStream;
import java.lang.management.ManagementFactory;
import java.util.Arrays;

public final class UmpReaderBenchmark {
    private static final int PARTS = 10_000;
    private static final int SAMPLES = 7;
    private static volatile long checksum;

    private UmpReaderBenchmark() {
    }

    public static void main(final String[] args) throws Exception {
        final byte[] envelope = new byte[PARTS * 66];
        for (int index = 0; index < envelope.length; index += 66) {
            envelope[index] = 21;
            envelope[index + 1] = 64;
            envelope[index + 2] = 7;
        }
        final ThreadMXBean bean = (ThreadMXBean) ManagementFactory.getThreadMXBean();
        bean.setThreadAllocatedMemoryEnabled(true);
        for (final boolean consume : new boolean[]{true, false}) {
            for (int i = 0; i < 5; i++) run(envelope, consume);
            final long[] nanos = new long[SAMPLES];
            final long[] allocations = new long[SAMPLES];
            final long thread = Thread.currentThread().getId();
            for (int i = 0; i < SAMPLES; i++) {
                final long before = bean.getThreadAllocatedBytes(thread);
                final long started = System.nanoTime();
                run(envelope, consume);
                nanos[i] = System.nanoTime() - started;
                allocations[i] = bean.getThreadAllocatedBytes(thread) - before;
            }
            Arrays.sort(nanos);
            Arrays.sort(allocations);
            System.out.printf("mode=%s parts=%d median_ns=%d allocated_bytes=%d checksum=%d%n",
                    consume ? "consume" : "discard", PARTS, nanos[SAMPLES / 2],
                    allocations[SAMPLES / 2], checksum);
        }
    }

    private static void run(final byte[] envelope, final boolean consume) throws Exception {
        UmpReader.readPayloadsUntil(new ByteArrayInputStream(envelope), (type, size, payload) -> {
            if (consume) checksum += payload.readNBytes(size)[0];
            else checksum += size;
            return true;
        });
    }
}
