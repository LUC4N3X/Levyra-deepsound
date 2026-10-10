# Levyra Enhanced Audio

## What it does

Levyra Enhanced Audio restores the high-frequency band that a lossy encoder
removed. It only acts when the decoded audio shows a measurable codec cutoff,
and it leaves everything else untouched:

- JioSaavn 320 kbps AAC is already full band (content up to about 20 kHz), so
  it is bypassed and passes through bit-identical.
- Real FLAC, ALAC and PCM sources are bypassed (`Source is already lossless`),
  based on the decoder input format rather than on provider metadata.
- Lower-bitrate lossy sources, for example a 128 kbps AAC fallback with a
  17 kHz cutoff, get the missing band rebuilt.

The feature is on by default and can be switched off in Audio Settings. The
setting persists across app restarts and backups.

Enhanced Audio never changes source labels. An AAC stream stays `AAC`,
`Lossless: No`; the processing state is shown separately in Technical Audio
Info (`Active` / `Bypassed` plus the bypass reason).

## Detection

`EnhancedAudioAnalyzer` keeps a 2048-point FFT of the mono mix, computed about
ten times per second and averaged over time. It looks for a codec cutoff
between 11 kHz and 19.5 kHz and accepts one only when all of these hold:

- the level drops by at least 30 dB across the edge;
- the band above the edge is empty (at least 50 dB below the 1–6 kHz level);
- there is real content just below the edge.

A natural top-end roll-off, silence, or a full-band stream does not meet these
conditions, so nothing is synthesized.

## Synthesis

`DspRestorationEngine` rebuilds the missing band with band replication, the
same idea used by SBR codecs:

1. Band-pass the band just below the cutoff (per channel, so the stereo image
   is preserved).
2. Shift it up by single-sideband modulation, using an IIR Hilbert pair, so it
   lands directly above the cutoff.
3. High-pass the result at the cutoff and mix it in at a level extrapolated
   from the measured spectral slope, minus a 3 dB safety margin, scaled by the
   detection confidence.
4. Ramp the gain per block to avoid zipper noise; a soft-knee limiter only
   touches samples above -0.5 dBFS.

The engine adds no latency, allocates nothing on the audio thread, and costs
about 20–100 µs per 1024-frame block on the JVM.

## Measured results

Measured with the shipped Kotlin engine (not a simulation). Reference:
lossless source. LSD and HF-LSD (above 16 kHz) are in dB, lower is better;
SI-SDR higher is better. The first second, while detection settles, is
excluded.

Synthetic music-like reference (chords, kick, hi-hat up to 20 kHz):

| Input | LSD | HF-LSD | SI-SDR | Clipped samples |
| :--- | ---: | ---: | ---: | ---: |
| AAC 320 → Enhanced | 2.32 → 2.32 | 2.69 → 2.69 | 22.87 → 22.87 | 0 → 0 |
| AAC 128 → Enhanced | 14.13 → 8.15 | 24.90 → 13.22 | 14.64 → 14.48 | 0 → 0 |
| AAC 96 → Enhanced | 16.39 → 9.78 | 27.98 → 15.11 | 11.66 → 11.60 | 0 → 0 |

Real music (a full-band 320 kbps master re-encoded to AAC):

| Input | LSD | HF-LSD | SI-SDR |
| :--- | ---: | ---: | ---: |
| AAC 128 → Enhanced | 11.80 → 9.67 | 20.41 → 16.03 | 24.51 → 24.51 |
| AAC 96 → Enhanced | 13.97 → 13.15 | 23.72 → 21.98 | 21.38 → 21.38 |

The rebuilt band is deliberately quieter than the original master. The engine
cannot recover the lossless master; it restores missing air without claiming
more than that.

## Limits

- On full-band 320 kbps sources there is nothing to restore, so Enhanced Audio
  stays bypassed.
- Media3 1.11 skips the whole audio processor chain when float output is
  enabled, so 24-bit sources are decoded to 16-bit while any Levyra DSP is in
  use. A 24-bit path would require float output only when every DSP stage is
  off.
- Neural restoration (Apollo, North Star) is not embedded: Apollo weights are
  CC BY-SA and target MP3 at 128 kbps or less on a GPU, and North Star weights
  are not published yet.
