#!/usr/bin/env bash
set -euo pipefail

if [[ $# -lt 4 ]]; then
    echo "usage: $0 <output-dir> <ndk-dir> <min-api> <abi>..." >&2
    exit 64
fi

OUTPUT_DIR="$1"
NDK_DIR="$2"
MIN_API="$3"
shift 3
ABIS=("$@")

FFMPEG_VERSION="7.1.5"
FFMPEG_SHA256="de668509caf9e35e3cd162473441fdb29538c6d96ed080292b3cf9e6fc5d558f"
FFMPEG_URL="https://ffmpeg.org/releases/ffmpeg-${FFMPEG_VERSION}.tar.xz"

OBOE_VERSION="1.11.0"
OBOE_SHA256="0ccf2110640a2b4489f1c93a404f2978656b434d8c5dc9cb6258f9cc95c79b15"
OBOE_URL="https://github.com/google/oboe/archive/refs/tags/${OBOE_VERSION}.tar.gz"

FFMPEG_DECODERS=(aac mp3 vorbis opus flac alac pcm_mulaw pcm_alaw ac3 eac3 dca truehd)

STAMP_VALUE="ffmpeg=${FFMPEG_VERSION}:${FFMPEG_SHA256};oboe=${OBOE_VERSION}:${OBOE_SHA256};api=${MIN_API};abis=${ABIS[*]};decoders=${FFMPEG_DECODERS[*]};ndk=$(basename "${NDK_DIR}")"
STAMP_FILE="${OUTPUT_DIR}/.levyra-native-deps"

if [[ -f "${STAMP_FILE}" && "$(cat "${STAMP_FILE}")" == "${STAMP_VALUE}" ]]; then
    echo "Native audio dependencies are up to date."
    exit 0
fi

case "$(uname -s)" in
    Linux) HOST_TAG="linux-x86_64" ;;
    Darwin) HOST_TAG="darwin-x86_64" ;;
    *) echo "Unsupported build host $(uname -s); native audio requires Linux or macOS." >&2; exit 1 ;;
esac

TOOLCHAIN="${NDK_DIR}/toolchains/llvm/prebuilt/${HOST_TAG}/bin"
if [[ ! -d "${TOOLCHAIN}" ]]; then
    echo "NDK toolchain not found at ${TOOLCHAIN}" >&2
    exit 1
fi

sha256_of() {
    if command -v sha256sum >/dev/null 2>&1; then
        sha256sum "$1" | cut -d ' ' -f 1
    else
        shasum -a 256 "$1" | cut -d ' ' -f 1
    fi
}

fetch_verified() {
    local url="$1" sha="$2" target="$3"
    if [[ -f "${target}" && "$(sha256_of "${target}")" == "${sha}" ]]; then
        return 0
    fi
    rm -f "${target}" "${target}.partial"
    curl --fail --location --silent --show-error --proto '=https' --tlsv1.2 \
        --retry 3 --retry-delay 5 --output "${target}.partial" "${url}"
    local actual
    actual="$(sha256_of "${target}.partial")"
    if [[ "${actual}" != "${sha}" ]]; then
        rm -f "${target}.partial"
        echo "Checksum mismatch for ${url}: expected ${sha}, got ${actual}" >&2
        exit 1
    fi
    mv "${target}.partial" "${target}"
}

DOWNLOAD_DIR="${OUTPUT_DIR}/downloads"
WORK_DIR="${OUTPUT_DIR}/work"
mkdir -p "${DOWNLOAD_DIR}"
rm -rf "${WORK_DIR}" "${OUTPUT_DIR}/ffmpeg" "${OUTPUT_DIR}/oboe" "${STAMP_FILE}"
mkdir -p "${WORK_DIR}"

FFMPEG_ARCHIVE="${DOWNLOAD_DIR}/ffmpeg-${FFMPEG_VERSION}.tar.xz"
OBOE_ARCHIVE="${DOWNLOAD_DIR}/oboe-${OBOE_VERSION}.tar.gz"
fetch_verified "${FFMPEG_URL}" "${FFMPEG_SHA256}" "${FFMPEG_ARCHIVE}"
fetch_verified "${OBOE_URL}" "${OBOE_SHA256}" "${OBOE_ARCHIVE}"

tar -xzf "${OBOE_ARCHIVE}" -C "${WORK_DIR}"
mv "${WORK_DIR}/oboe-${OBOE_VERSION}" "${OUTPUT_DIR}/oboe"

tar -xJf "${FFMPEG_ARCHIVE}" -C "${WORK_DIR}"
FFMPEG_SOURCE="${WORK_DIR}/ffmpeg-${FFMPEG_VERSION}"

DECODER_FLAGS=()
for decoder in "${FFMPEG_DECODERS[@]}"; do
    DECODER_FLAGS+=("--enable-decoder=${decoder}")
done

JOBS="$(nproc 2>/dev/null || sysctl -n hw.ncpu 2>/dev/null || echo 4)"

for abi in "${ABIS[@]}"; do
    extra_cflags="-O2 -fPIC"
    extra_ldflags="-Wl,-z,max-page-size=16384"
    asm_flag="--enable-asm"
    case "${abi}" in
        armeabi-v7a)
            arch="arm"; cpu="armv7-a"; triple="armv7a-linux-androideabi"
            extra_cflags="${extra_cflags} -march=armv7-a -mfloat-abi=softfp"
            extra_ldflags="${extra_ldflags} -Wl,--fix-cortex-a8"
            ;;
        arm64-v8a)
            arch="aarch64"; cpu="armv8-a"; triple="aarch64-linux-android"
            ;;
        x86)
            arch="x86"; cpu="i686"; triple="i686-linux-android"
            asm_flag="--disable-asm"
            ;;
        x86_64)
            arch="x86_64"; cpu="x86-64"; triple="x86_64-linux-android"
            asm_flag="--disable-asm"
            ;;
        *)
            echo "Unsupported ABI ${abi}" >&2
            exit 1
            ;;
    esac

    build_dir="${WORK_DIR}/build-${abi}"
    mkdir -p "${build_dir}"
    (
        cd "${build_dir}"
        export PATH="${TOOLCHAIN}:${PATH}"
        "${FFMPEG_SOURCE}/configure" \
            --prefix="${OUTPUT_DIR}/ffmpeg/${abi}" \
            --target-os=android \
            --arch="${arch}" \
            --cpu="${cpu}" \
            --enable-cross-compile \
            --cc="${triple}${MIN_API}-clang" \
            --cxx="${triple}${MIN_API}-clang++" \
            --nm="llvm-nm" \
            --ar="llvm-ar" \
            --ranlib="llvm-ranlib" \
            --strip="llvm-strip" \
            --extra-cflags="${extra_cflags}" \
            --extra-ldflags="${extra_ldflags}" \
            --enable-static \
            --disable-shared \
            --enable-pic \
            --disable-autodetect \
            --disable-debug \
            --disable-doc \
            --disable-programs \
            --disable-everything \
            --disable-avdevice \
            --disable-avformat \
            --disable-swscale \
            --disable-postproc \
            --disable-avfilter \
            --disable-network \
            --disable-symver \
            --disable-v4l2-m2m \
            --disable-vulkan \
            --enable-swresample \
            "${asm_flag}" \
            "${DECODER_FLAGS[@]}"
        make -j"${JOBS}"
        make install-libs install-headers
    )
done

rm -rf "${WORK_DIR}"
printf '%s' "${STAMP_VALUE}" > "${STAMP_FILE}"
echo "Native audio dependencies ready in ${OUTPUT_DIR}"
