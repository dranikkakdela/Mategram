#!/usr/bin/env bash
set -euo pipefail

# build_ffmpeg.sh - Optimized for VP8/VP9 video decoding only
# Usage:
#   ./build_ffmpeg.sh [--ndk PATH] [--api N] [--abis "armeabi-v7a arm64-v8a x86 x86_64"] [--jobs N] [--ffmpeg-root PATH]

NDK=${ANDROID_NDK_HOME:-/usr/lib/android-sdk/ndk/21.4.7075529}
API=21
ABIS="armeabi-v7a arm64-v8a x86 x86_64"
JOBS=$(nproc 2>/dev/null || echo 4)
FFMPEG_ROOT=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_usage() {
  cat <<EOF
Usage: $0 [options]
Options:
  --ndk PATH        Android NDK root (default: \$ANDROID_NDK_HOME or ${NDK})
  --api N           Android API level (default: ${API})
  --abis "list"     Space-separated ABI list (default: "${ABIS}")
  --jobs N          Parallel jobs for make (default: ${JOBS})
  --ffmpeg-root PATH   Path to FFmpeg source (default: autodetect)
  -h|--help         Show this help
EOF
}

# parse args
while [[ $# -gt 0 ]]; do
  case "$1" in
    --ndk) NDK="$2"; shift 2;;
    --api) API="$2"; shift 2;;
    --abis) ABIS="$2"; shift 2;;
    --jobs) JOBS="$2"; shift 2;;
    --ffmpeg-root) FFMPEG_ROOT="$2"; shift 2;;
    -h|--help) print_usage; exit 0;;
    *) echo "Unknown arg: $1"; print_usage; exit 1;;
  esac
done

# autodetect FFMPEG_ROOT if not provided
if [[ -z "$FFMPEG_ROOT" ]]; then
  if [[ -d "${SCRIPT_DIR}/third_party/FFmpeg" ]]; then
    FFMPEG_ROOT="${SCRIPT_DIR}/third_party/FFmpeg"
  elif [[ -d "${SCRIPT_DIR}/../third_party/FFmpeg" ]]; then
    FFMPEG_ROOT="${SCRIPT_DIR}/../third_party/FFmpeg"
  elif [[ -d "./third_party/FFmpeg" ]]; then
    FFMPEG_ROOT="$(pwd)/third_party/FFmpeg"
  else
    echo "FFmpeg source not found automatically. Please pass --ffmpeg-root PATH"
    exit 1
  fi
fi

FFMPEG_ROOT="$(cd "${FFMPEG_ROOT}" && pwd)"

echo "NDK: ${NDK}"
echo "API: ${API}"
echo "ABIs: ${ABIS}"
echo "JOBS: ${JOBS}"
echo "FFmpeg root: ${FFMPEG_ROOT}"

if [[ ! -d "${NDK}" ]]; then
  echo "ERROR: NDK not found at ${NDK}"
  exit 1
fi

# toolchain base
#TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/linux-x86_64"
#if [[ ! -d "${TOOLCHAIN}" ]]; then
#  TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt"
#  if [[ ! -d "${TOOLCHAIN}" ]]; then
#    echo "ERROR: NDK toolchain directory not found under ${NDK}"
#    exit 1
#  fi
#fi
OS_NAME="linux-x86_64"
if [[ "$OSTYPE" == "darwin"* ]]; then
  OS_NAME="darwin-x86_64"
elif [[ "$OSTYPE" == "cygwin"* ]]; then
  OS_NAME="windows-x86_64"
fi

TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/${OS_NAME}"

build_one() {
  local ABI="$1"
  local ARCH=""
  local CPU=""
  local CC=""
  local CXX=""
  local CROSS_PREFIX=""
  local EXTRA_CFLAGS=""
  local EXTRA_LDFLAGS=""

  case "${ABI}" in
    armeabi-v7a)
      ARCH="arm"
      CPU="armv7-a"
      CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API}-clang"
      CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API}-clang++"
      CROSS_PREFIX="${TOOLCHAIN}/bin/arm-linux-androideabi-"
      EXTRA_CFLAGS="-march=armv7-a -mfloat-abi=softfp -mfpu=neon -mtune=cortex-a8 -O3"
      EXTRA_LDFLAGS="-Wl,--fix-cortex-a8"
      ;;
    arm64-v8a)
      ARCH="aarch64"
      CPU="generic"
      CC="${TOOLCHAIN}/bin/aarch64-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/aarch64-linux-android${API}-clang++"
      CROSS_PREFIX="${TOOLCHAIN}/bin/aarch64-linux-android-"
      EXTRA_CFLAGS="-O3 -mtune=generic -march=armv8-a+simd"
      EXTRA_LDFLAGS="-Wl,-z,max-page-size=16384"
      ;;
    x86)
      ARCH="x86"
      CPU="i686"
      CC="${TOOLCHAIN}/bin/i686-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/i686-linux-android${API}-clang++"
      CROSS_PREFIX="${TOOLCHAIN}/bin/i686-linux-android-"
      EXTRA_CFLAGS="-march=i686 -mtune=atom -mssse3 -mfpmath=sse -m32 -O3"
      ;;
    x86_64)
      ARCH="x86_64"
      CPU="generic"
      CC="${TOOLCHAIN}/bin/x86_64-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/x86_64-linux-android${API}-clang++"
      CROSS_PREFIX="${TOOLCHAIN}/bin/x86_64-linux-android-"
      EXTRA_CFLAGS="-march=x86-64 -msse4.2 -mpopcnt -m64 -mtune=x86-64 -O3"
      EXTRA_LDFLAGS="-Wl,-z,max-page-size=16384"
      ;;
    *)
      echo "Unsupported ABI: ${ABI}"
      return 1
      ;;
  esac

  if [[ ! -x "${CC}" ]]; then
    echo "ERROR: CC not found or not executable: ${CC}"
    return 1
  fi
  if [[ ! -x "${CXX}" ]]; then
    echo "ERROR: CXX not found or not executable: ${CXX}"
    return 1
  fi

  local BUILD_DIR="${FFMPEG_ROOT}/build/android/${ABI}"
  local INSTALL_DIR="${BUILD_DIR}/install"
  mkdir -p "${BUILD_DIR}"
  pushd "${BUILD_DIR}" >/dev/null

  echo "=== Building FFmpeg for ${ABI} (arch=${ARCH}) in ${BUILD_DIR} ==="

  local AR="${TOOLCHAIN}/bin/llvm-ar"
  local NM="${TOOLCHAIN}/bin/llvm-nm"
  local STRIP="${TOOLCHAIN}/bin/llvm-strip"

  local CONFIGURE_FLAGS=(
    --prefix="${INSTALL_DIR}"
    --target-os=android
    --arch="${ARCH}"
    --cpu="${CPU}"
    --cc="${CC}"
    --cxx="${CXX}"
    --ar="${AR}"
    --nm="${NM}"
    --strip="${STRIP}"
    --cross-prefix="${CROSS_PREFIX}"
    --sysroot="${TOOLCHAIN}/sysroot"
    --enable-cross-compile
    --enable-shared
    --disable-static
    
    # Disable everything by default
    --disable-everything
    
    # Enable only what we need: libavformat, libavcodec, libavutil
    --enable-avformat
    --enable-avcodec
    --enable-avutil
    
    # Enable only VP8/VP9 decoders and parsers
    --enable-decoder=vp8
    --enable-decoder=vp9
    --enable-parser=vp8
    --enable-parser=vp9
    
    # Enable Matroska demultiplexer for WebM support
    --enable-demuxer=matroska
    
    # Disable other components
    --disable-doc
    --disable-programs
    --disable-ffplay
    --disable-ffprobe
    --disable-symver
    --disable-network
    --disable-zlib
    --disable-xlib
    --disable-sdl2
    --disable-encoders
    --disable-muxers
    --disable-filters
    --disable-devices
    --disable-hwaccels
    
    # Optimization flags - removed --enable-small which reduces performance
    --enable-pic
    --enable-neon
    
    # Threading and JNI
    --enable-pthreads
    --enable-jni
    
    --extra-cflags="-O3 -fPIC -D__ANDROID_API__=${API} -I${INSTALL_DIR}/include -march=armv7-a -mtune=cortex-a8 -fvectorize ${EXTRA_CFLAGS}"
    --extra-ldflags="-L${INSTALL_DIR}/lib ${EXTRA_LDFLAGS}"
  )

  if [[ "${ARCH}" == "x86" || "${ARCH}" == "x86_64"  ]]; then
    CONFIGURE_FLAGS+=(--disable-asm)
  else
    CONFIGURE_FLAGS+=(--enable-asm)
  fi

  echo "Running configure..."
  # Fix line endings for configure scripts
  files_to_fix=(
    configure
    libavfilter/allfilters.c
    libavdevice/alldevices.c
    libavformat/allformats.c
    libavcodec/allcodecs.c
    libavcodec/parsers.c
    libavcodec/bitstream_filters.c
    libavcodec/hwaccels.h
    libavformat/protocols.c
    ffbuild/version.sh
    ffbuild/libversion.sh
    ffbuild/pkgconfig_generate.sh
  )
  for f in "${files_to_fix[@]}"; do
    file_path="${FFMPEG_ROOT}/$f"
    if [ -f "$file_path" ]; then
        tr -d '\r' < "$file_path" > "$file_path.tmp" && mv "$file_path.tmp" "$file_path"
        chmod +x "$file_path"
    fi
  done

  bash "${FFMPEG_ROOT}/configure" "${CONFIGURE_FLAGS[@]}" || {
      echo "!!! CONFIGURE FAILED for ${ABI} !!!"
      if [[ -f ffbuild/config.log ]]; then
        echo "----- ffbuild/config.log (last 100 lines) -----"
        tail -n 100 ffbuild/config.log || true
      fi
      popd >/dev/null
      return 1
    }

  echo "Configured. Starting make..."
  make clean
  make -j"${JOBS}" || { echo "make failed for ${ABI}"; popd >/dev/null; return 1; }
  make install || { echo "make install failed for ${ABI}"; popd >/dev/null; return 1; }

  echo "Finished ${ABI}: libs at ${INSTALL_DIR}/lib"
  popd >/dev/null
  return 0
}

# build requested ABIs
for abi in ${ABIS}; do
  build_one "${abi}" || { echo "Build failed for ${abi}"; exit 1; }
done

echo "All requested FFmpeg builds finished."