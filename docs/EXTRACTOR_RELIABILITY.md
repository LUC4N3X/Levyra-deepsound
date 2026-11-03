# Extractor reliability and measured UMP allocation

## Ownership and scope

Android and Windows consume the source-built `third_party/LevyraExtractor`
composite build. Android additionally owns client policy, health, token handling,
the WebView cipher runtime and its Media3 SABR data source. Windows resolves
extractor audio candidates and passes verified direct streams to libVLC. Download
selection remains separate from Android's playback-only reduced-quality recovery.

This change keeps those owners. It introduces no InnerTubeX dependency, copied
implementation, additional transport, JavaScript interpreter or account mechanism.
Java production sources continue to target Java 17.

## Client recovery contract

Android's normal audio hedge retains the requested quality and health ordering.
One temporarily cooling client may receive one reduced-quality attempt only after
every normal attempt conclusively reports no playable audio. The client must
still be allowed by playback policy and have been suspended for an invalid stream,
rather than a hard HTTP or access failure. Its recovery permit is consumed
atomically, including when the recovered stream fails verification.

Network, HTTP, restriction, cipher, parsing and ambiguous verification failures
suppress this recovery. A null result is inconclusive. Cancellation settles the
whole hedge and propagates. A successful normal result never runs recovery. Video,
offline export and Windows candidate selection retain their existing behavior.
An empty policy allowlist stays empty.

Conclusive format absence does not count as a global client-health failure for
other videos. Existing success, latency, failure and cooldown metrics retain their
current owner and persistence. Reduced quality is a request override; it never
changes the user's audio preference.

## Cipher and player discovery

Shared `n` transformation replaces only the selected query value, preserving
other query parameters, path and fragment. Local Java cancellation propagates
without starting remote fallback. Android distinguishes the decoder's own timeout
from cancellation and thread interruption.

Batch inputs are deduplicated before decoder work. The Android local decoder is
still consulted on every batch because it owns epoch-aware caching and caller
provenance. A shared cache hit must not bypass that contract. Partial successful
results remain available to the existing remote fallback.

The legacy Java player-discovery helper now accepts IAS and ES6 paths, extracts
the embed-discovered hash and downloads that exact script rather than a fixed old
hash. Discovered script downloads are constrained to standard YouTube HTTPS player
paths. This helper currently has no production caller: its tests are maintenance
coverage, not evidence of new Android or Windows live-player support.

Android already uses an IAS source and its existing WebView JavaScript engine.
The instrumented synthetic cipher test exercises ES6 arrow functions and
`Math.pow` for both signature and `n`, including concurrent requests. It does not
demonstrate compatibility with every current YouTube script.

## SABR and UMP

The shared incremental reader reuses one lazily allocated drain buffer per
response and allocates none when consumers exhaust each part. Zero-progress bulk
reads fall back to one-byte reads; truncation and interruption remain errors.
Payload streams respect zero-length reads at their boundary.

Android retains its small reusable UMP buffer and part-size cap. Its reader now
handles zero-progress bulk reads and interruption. Its existing segment assembler
evicts one oldest header instead of clearing all active headers at capacity;
replacing an existing header preserves other segments. Media-range overflow is
rejected before state advances. Existing seek, continuation, redirect and endpoint
failover ownership remains in `SabrDataSource`.

## Reproducing the allocation measurement

From the repository root on Windows, with Java 17:

```powershell
.\gradlew.bat -p third_party/LevyraExtractor :extractor:testClasses
java -cp 'third_party/LevyraExtractor/extractor/build/classes/java/main;third_party/LevyraExtractor/extractor/build/classes/java/test' org.schabi.newpipe.extractor.services.youtube.sabr.UmpReaderBenchmark
```

The standalone test-source benchmark feeds 10,000 64-byte payloads through the
production reader. Each mode warms up five times, then reports the median of seven
samples. `ThreadMXBean` measures allocated bytes on the current thread. The
consumer checksum prevents elimination of observable work. No network is used.

Recorded on Windows, Temurin 17.0.20, comparing the same fixture before and after
the UMP reader change:

| Mode | Before allocated bytes | After allocated bytes | Before median ns | After median ns |
|---|---:|---:|---:|---:|
| Consume every payload | 83,120,048 | 1,040,048 | 5,472,900 | 685,400 |
| Discard payloads | 82,080,048 | 248,256 | 11,919,900 | 503,700 |

Allocation reduction is reproducible for this fixture; elapsed time is sensitive
to JIT, GC and host load. These are reader microbenchmark results, not startup,
network, device-memory, audio-quality or end-to-end playback measurements.

## Optional Windows native check

`VlcAudioPlayerNativeSmokeTest` exercises Levyra's real Windows player with a
generated silent PCM WAV: playback, forward seek, intentional pause, resume, stop
and repeated close. It runs only when explicitly enabled and expects 64-bit VLC
under `C:/Program Files/VideoLAN/VLC`. Without that opt-in it is skipped.

```powershell
$env:LEVYRA_NATIVE_SMOKE = '1'
.\desktop\gradlew.bat -p desktop :player:test --tests '*VlcAudioPlayerNativeSmokeTest' --rerun-tasks
```

This local-file check does not establish online YouTube stream availability.

## External comparison and remaining limits

The [InnerTubeX README](https://github.com/MetrolistGroup/innertubex/blob/main/README.md),
its cipher service, client director, health tracker and SABR processor were read as
capability references. The [Metrolist integration change](https://github.com/MetrolistGroup/Metrolist/commit/e6fea3b36c68841bad21daa6252e1d69ecd3afbe)
motivates checking exhausted alternatives, quality preservation, error boundaries,
IAS selection and `Math.pow`. Its code and recovery architecture are not imported.
InnerTubeX describes bounded compiled-player caches, scoped client health and
seekable SABR; Levyra retains its existing architecture and only changes defects
established by local evidence.

There is no measured claim of overall parity or superiority over InnerTubeX.
Cross-request decoder single-flight, Windows-specific adaptive client recovery,
real-player compatibility across changing script versions, live comparative
latency/reliability benchmarks, sustained physical-device memory and online
Windows/libVLC playback require separate direct evidence. Synthetic byte integrity
and format-order tests do not prove perceived audio quality or live download
availability. Validation results for this patch belong to its pull request and
the generated test reports.
