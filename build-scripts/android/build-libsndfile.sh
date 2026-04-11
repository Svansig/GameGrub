#!/usr/bin/env bash
# build-libsndfile.sh
# Builds libsndfile 1.2.2 for Android arm64-v8a using NDK r28+ and CMake.
# Outputs libsndfile.so to prebuilt-out/android/arm64-v8a/lib/.
#
# This script is an alternative to the FetchContent CMake integration
# (third_party/libsndfile/CMakeLists.txt). Use it if you need to build
# libsndfile out-of-band (e.g. for cross-checks or CI pre-bake).
#
# Usage:
#   export ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358
#   bash build-scripts/android/build-libsndfile.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PREBUILT_OUT="$REPO_ROOT/prebuilt-out/android/arm64-v8a"
WORK_DIR="/tmp/libsndfile-build"
VERSION="1.2.2"
TARBALL_URL="https://github.com/libsndfile/libsndfile/releases/download/${VERSION}/libsndfile-${VERSION}.tar.xz"
TARBALL_SHA256="3799ca9924d3125038880367bf1468e53a1b7e3686a934f098b7e1d286cdb80e"
TARBALL="/tmp/libsndfile-${VERSION}.tar.xz"

NDK_HOME="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK_HOME" ]]; then
    NDK_HOME="$(find "$HOME/Android/Sdk/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -1)"
fi
if [[ -z "$NDK_HOME" || ! -d "$NDK_HOME" ]]; then
    echo "ERROR: ANDROID_NDK_HOME is not set and could not be auto-detected." >&2
    exit 1
fi
echo "[sndfile] Using NDK: $NDK_HOME"

# ---------------------------------------------------------------------------
# 1. Download and verify
# ---------------------------------------------------------------------------
if [[ ! -f "$TARBALL" ]]; then
    echo "[sndfile] Downloading $TARBALL_URL"
    curl -fsSL "$TARBALL_URL" -o "$TARBALL"
fi
echo "[sndfile] Verifying SHA-256"
echo "$TARBALL_SHA256  $TARBALL" | sha256sum -c

# ---------------------------------------------------------------------------
# 2. Extract
# ---------------------------------------------------------------------------
mkdir -p "$WORK_DIR"
SRC_DIR="$WORK_DIR/libsndfile-${VERSION}"
if [[ ! -d "$SRC_DIR" ]]; then
    tar -xJf "$TARBALL" -C "$WORK_DIR"
fi

# ---------------------------------------------------------------------------
# 3. CMake configure
# ---------------------------------------------------------------------------
BUILD_DIR="$WORK_DIR/build-arm64"
mkdir -p "$BUILD_DIR"

TOOLCHAIN_FILE="$NDK_HOME/build/cmake/android.toolchain.cmake"

cmake -S "$SRC_DIR" -B "$BUILD_DIR" \
    -DCMAKE_TOOLCHAIN_FILE="$TOOLCHAIN_FILE" \
    -DANDROID_ABI=arm64-v8a \
    -DANDROID_PLATFORM=android-33 \
    -DCMAKE_BUILD_TYPE=Release \
    -DBUILD_SHARED_LIBS=ON \
    -DBUILD_EXAMPLES=OFF \
    -DBUILD_TESTING=OFF \
    -DENABLE_CPACK=OFF \
    -DENABLE_PACKAGE_CONFIG=OFF \
    -DINSTALL_PKGCONFIG_MODULE=OFF \
    -DENABLE_EXTERNAL_LIBS=OFF \
    -DENABLE_MPEG=OFF \
    -DCMAKE_SHARED_LINKER_FLAGS="-Wl,-z,max-page-size=16384" \
    -DCMAKE_INSTALL_PREFIX="$PREBUILT_OUT"

# ---------------------------------------------------------------------------
# 4. Build and install
# ---------------------------------------------------------------------------
cmake --build "$BUILD_DIR" --config Release -j"$(nproc)"
cmake --install "$BUILD_DIR" --config Release

echo "[sndfile] Installed to $PREBUILT_OUT"
LIB="$PREBUILT_OUT/lib/libsndfile.so"
if [[ -f "$LIB" ]]; then
    "$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump" -p "$LIB" | grep LOAD
    echo "[sndfile] Verify: each LOAD align must be 2**14 or higher."
else
    echo "WARNING: $LIB not found after install." >&2
fi
