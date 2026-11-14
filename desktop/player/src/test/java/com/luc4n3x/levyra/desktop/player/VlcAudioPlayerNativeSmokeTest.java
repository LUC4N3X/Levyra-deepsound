package com.luc4n3x.levyra.desktop.player;

import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.function.BooleanSupplier;

public final class VlcAudioPlayerNativeSmokeTest {
    @org.junit.Test
    public void localPlaybackSeekPauseResumeAndClose() throws Exception {
        org.junit.Assume.assumeTrue("Set LEVYRA_NATIVE_SMOKE=1 with VLC installed",
                "1".equals(System.getenv("LEVYRA_NATIVE_SMOKE")));
        Path directory = Files.createDirectories(Path.of("build", "native-smoke"));
        Path fixture = Files.createTempFile(directory, "silence-", ".wav");
        int dataBytes = 8000 * 2 * 8;
        ByteBuffer wave = ByteBuffer.allocate(44 + dataBytes).order(ByteOrder.LITTLE_ENDIAN);
        wave.put("RIFF".getBytes(StandardCharsets.US_ASCII)).putInt(36 + dataBytes);
        wave.put("WAVEfmt ".getBytes(StandardCharsets.US_ASCII)).putInt(16);
        wave.putShort((short) 1).putShort((short) 1).putInt(8000).putInt(16000);
        wave.putShort((short) 2).putShort((short) 16);
        wave.put("data".getBytes(StandardCharsets.US_ASCII)).putInt(dataBytes);
        Files.write(fixture, wave.array());
        VlcAudioPlayer player = null;
        try {
            player = VlcAudioPlayer.Companion.create("", null);
            final VlcAudioPlayer active = player;
            player.setMuted(true);
            player.play(fixture.toUri().toASCIIString(), 0L);
            await(() -> active.isPlaying() && active.positionMs() >= 100L, "local playback");
            if (player.durationMs() < 7900L) throw new AssertionError("duration");
            player.seekTo(2000L);
            await(() -> active.positionMs() >= 1800L, "seek");
            player.pause();
            await(() -> !active.isPlaying(), "pause");
            long paused = player.positionMs();
            Thread.sleep(250L);
            if (Math.abs(player.positionMs() - paused) > 150L) throw new AssertionError("pause drift");
            player.resume();
            await(active::isPlaying, "resume");
            player.stop();
            await(() -> !active.isPlaying(), "stop");
            System.out.println("PASS: Levyra Windows libVLC local WAV playback, seek, pause, resume, stop");
        } finally {
            if (player != null) {
                player.close();
                player.close();
            }
            Files.deleteIfExists(fixture);
        }
    }

    private static void await(BooleanSupplier condition, String label) throws Exception {
        long deadline = System.nanoTime() + 5_000_000_000L;
        while (!condition.getAsBoolean() && System.nanoTime() < deadline) Thread.sleep(25L);
        if (!condition.getAsBoolean()) throw new AssertionError(label);
    }
}
