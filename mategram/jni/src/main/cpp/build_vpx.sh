#!/usr/bin/env bash
set -euo pipefail

NDK=${ANDROID_NDK_HOME:-/usr/lib/android-sdk/ndk/21.4.7075529}
API=21
ABIS="armeabi-v7a arm64-v8a x86 x86_64"
JOBS=$(nproc 2>/dev/null || echo 4)
VPX_ROOT=""
SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"

print_usage() {
  cat <<EOF
Usage: $0 [options]
Options:
  --ndk PATH        Android NDK root (default: \$ANDROID_NDK_HOME or ${NDK})
  --api N           Android API level (default: ${API})
  --abis "list"     Space-separated ABI list (default: "${ABIS}")
  --jobs N          Parallel jobs for make (default: ${JOBS})
  --vpx-root PATH   Path to libvpx source (default: autodetect)
  -h|--help         Show this help
EOF
}

while [[ $# -gt 0 ]]; do
  case "$1" in
    --ndk) NDK="$2"; shift 2;;
    --api) API="$2"; shift 2;;
    --abis) ABIS="$2"; shift 2;;
    --jobs) JOBS="$2"; shift 2;;
    --vpx-root) VPX_ROOT="$2"; shift 2;;
    -h|--help) print_usage; exit 0;;
    *) echo "Unknown arg: $1"; print_usage; exit 1;;
  esac
done

if [[ -z "$VPX_ROOT" ]]; then
  if [[ -d "${SCRIPT_DIR}/third_party/libvpx" ]]; then
    VPX_ROOT="${SCRIPT_DIR}/third_party/libvpx"
  elif [[ -d "${SCRIPT_DIR}/../third_party/libvpx" ]]; then
    VPX_ROOT="${SCRIPT_DIR}/../third_party/libvpx"
  elif [[ -d "./third_party/libvpx" ]]; then
    VPX_ROOT="$(pwd)/third_party/libvpx"
  else
    echo "libvpx source not found automatically. Please pass --vpx-root PATH"
    exit 1
  fi
fi

if [[ ! -d "${NDK}" ]]; then
  echo "ERROR: NDK not found at ${NDK}"
  exit 1
fi

OS_NAME="linux-x86_64"
if [[ "$OSTYPE" == "darwin"* ]]; then
  OS_NAME="darwin-x86_64"
elif [[ "$OSTYPE" == "cygwin"* ]]; then
  OS_NAME="windows-x86_64"
fi

TOOLCHAIN="${NDK}/toolchains/llvm/prebuilt/${OS_NAME}"

build_one() {
  local ABI="$1"
  local TARGET=""
  local BUILD_SUBDIR=""
  local EXTRA_CONFIG_FLAGS=""
  local EXTRA_CFLAGS="-O3 -fPIC"

  case "${ABI}" in
    armeabi-v7a)
      TARGET="armv7-android-gcc"
      BUILD_SUBDIR="armv7-a"
      CC="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API}-clang"
      CXX="${TOOLCHAIN}/bin/armv7a-linux-androideabi${API}-clang++"
      CROSS_PREFIX="arm-linux-androideabi-"
      EXTRA_CONFIG_FLAGS="--enable-neon"
      ;;
    arm64-v8a)
      TARGET="arm64-android-gcc"
      BUILD_SUBDIR="arm64-v8a"
      CC="${TOOLCHAIN}/bin/aarch64-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/aarch64-linux-android${API}-clang++"
      CROSS_PREFIX="aarch64-linux-android-"
      ;;
    x86)
      TARGET="x86-android-gcc"
      BUILD_SUBDIR="i686"
      CC="${TOOLCHAIN}/bin/i686-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/i686-linux-android${API}-clang++"
      CROSS_PREFIX="i686-linux-android-"
      ;;
    x86_64)
      TARGET="x86_64-android-gcc"
      BUILD_SUBDIR="x86_64"
      CC="${TOOLCHAIN}/bin/x86_64-linux-android${API}-clang"
      CXX="${TOOLCHAIN}/bin/x86_64-linux-android${API}-clang++"
      CROSS_PREFIX="x86_64-linux-android-"
      ;;
  esac

  local BUILD_DIR="${VPX_ROOT}/build/${BUILD_SUBDIR}"
  mkdir -p "${BUILD_DIR}"
  pushd "${BUILD_DIR}" >/dev/null

  local AR="${TOOLCHAIN}/bin/llvm-ar"
  local NM="${TOOLCHAIN}/bin/llvm-nm"
  local RANLIB="${TOOLCHAIN}/bin/llvm-ranlib"
  local STRIP="${TOOLCHAIN}/bin/llvm-strip"
  local AS="${CC}"

  if [[ "${ABI}" == "x86" || "${ABI}" == "x86_64" ]]; then
    AS="${TOOLCHAIN}/bin/yasm"
  fi

  # Fix for InterpKernel and vpx_scale missing symbols
  EXTRA_CFLAGS+=" -I${VPX_ROOT}/vpx_dsp -I${VPX_ROOT}/vpx_scale"

  CC="${CC}" CXX="${CXX}" AR="${AR}" NM="${NM}" RANLIB="${RANLIB}" STRIP="${STRIP}" AS="${AS}" \
    "${VPX_ROOT}/configure" \
      --target="${TARGET}" \
      --prefix="$(pwd)/" \
      --disable-examples \
      --disable-tools \
      --disable-unit-tests \
      --disable-docs \
      --enable-vp8 \
      --enable-vp9 \
      --enable-vp9-highbitdepth \
      --enable-static \
      --disable-shared \
      --enable-pic \
      --enable-runtime-cpu-detect \
      --enable-realtime-only \
      ${EXTRA_CONFIG_FLAGS} \
      --extra-cflags="${EXTRA_CFLAGS}"

  make clean
  make -j"${JOBS}"
  make install

  popd >/dev/null
}

for abi in ${ABIS}; do
  build_one "${abi}"
done

echo "Done."
