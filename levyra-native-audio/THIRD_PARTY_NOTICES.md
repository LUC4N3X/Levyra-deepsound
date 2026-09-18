# Third-party components in levyra-native-audio

Native code is only built when the Gradle property `levyraNativeAudio=true` is
set and the build is not an F-Droid build. Sources are downloaded by
`native-deps/prepare_native_deps.sh`, verified against pinned SHA-256 digests and
compiled from source. No prebuilt native binaries are used.

| Component | Version | License | Source |
| --- | --- | --- | --- |
| FFmpeg (libavcodec, libavutil, libswresample) | 7.1.5 | LGPL-2.1-or-later | https://ffmpeg.org/releases/ffmpeg-7.1.5.tar.xz |
| Oboe | 1.11.0 | Apache-2.0 | https://github.com/google/oboe/archive/refs/tags/1.11.0.tar.gz |
| AndroidX Media3 FFmpeg decoder extension | 1.11.0 | Apache-2.0 | https://github.com/androidx/media/tree/1.11.0/libraries/decoder_ffmpeg |

FFmpeg is configured with `--disable-everything`, without GPL or nonfree
components, and with these audio decoders only: `aac`, `mp3`, `vorbis`, `opus`,
`flac`, `alac`, `pcm_mulaw`, `pcm_alaw`, `ac3`, `eac3`, `dca`, `truehd`. The
exact configure invocation is in `native-deps/prepare_native_deps.sh`.

The Media3 FFmpeg decoder files under `src/main/java/androidx/media3/decoder/ffmpeg`
and `src/main/cpp/ffmpeg` are copied from the Media3 1.11.0 release. The only
change is in `FfmpegLibrary.java`, where the Checker Framework
`@MonotonicNonNull` annotation is replaced with `androidx.annotation.Nullable`.
The experimental FFmpeg video renderer is not included.
