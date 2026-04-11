#!/usr/bin/env bash
# build-libltdl.sh
# Downloads GNU Libtool 2.5.4, extracts libltdl sources, and builds
# libltdl.so for Android arm64-v8a using NDK r28+.
#
# Output: prebuilt-out/android/arm64-v8a/lib/libltdl.so
#         prebuilt-out/android/arm64-v8a/include/ltdl.h
#
# libltdl is used by libpulsecore-13.0.so for runtime module loading.
# On Android/bionic, lt_dlopen maps to dlopen from libdl.
#
# Usage:
#   export ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358
#   bash build-scripts/android/build-libltdl.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PREBUILT_OUT="$REPO_ROOT/prebuilt-out/android/arm64-v8a"
WORK_DIR="/tmp/libltdl-build"
VERSION="2.5.4"
TARBALL_URL="https://ftpmirror.gnu.org/libtool/libtool-${VERSION}.tar.gz"
# Verify this SHA-256 matches the actual download before committing.
TARBALL_SHA256="da8ebb2ce4dcf46b90098daf962cffa68f4b4f62ea60f798d0ef12929ede6adf"
TARBALL="/tmp/libtool-${VERSION}.tar.gz"

NDK_HOME="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK_HOME" ]]; then
    NDK_HOME="$(find "$HOME/Android/Sdk/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -1)"
fi
if [[ -z "$NDK_HOME" || ! -d "$NDK_HOME" ]]; then
    echo "ERROR: ANDROID_NDK_HOME is not set and could not be auto-detected." >&2
    exit 1
fi
TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
CC="$TOOLCHAIN/bin/aarch64-linux-android33-clang"
echo "[ltdl] Using CC: $CC"

# ---------------------------------------------------------------------------
# 1. Download and verify
# ---------------------------------------------------------------------------
if [[ ! -f "$TARBALL" ]]; then
    echo "[ltdl] Downloading $TARBALL_URL"
    curl -fsSL "$TARBALL_URL" -o "$TARBALL"
fi
echo "[ltdl] Verifying SHA-256"
echo "$TARBALL_SHA256  $TARBALL" | sha256sum -c

# ---------------------------------------------------------------------------
# 2. Extract and stage libltdl sources
# ---------------------------------------------------------------------------
mkdir -p "$WORK_DIR/src"
SRC_DIR="$WORK_DIR/src"
LIBTOOL_DIR="/tmp/libtool-${VERSION}"
if [[ ! -d "$LIBTOOL_DIR" ]]; then
    tar -xzf "$TARBALL" -C /tmp
fi

# Copy only the core ltdl C sources + headers.
cp "$LIBTOOL_DIR/libltdl/ltdl.c"         "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/lt__dirent.c"   "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/lt__strl.c"     "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/ltdl.h"         "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/lt__private.h"  "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/lt_dlloader.h"  "$SRC_DIR/"
cp "$LIBTOOL_DIR/libltdl/lt__glibc.h"    "$SRC_DIR/" 2>/dev/null || true

# Install Android-specific lt_system.h.
cp "$REPO_ROOT/third_party/libltdl/lt_system_android.h" "$SRC_DIR/lt_system.h"

# ---------------------------------------------------------------------------
# 3. Build with NDK clang directly (no CMake needed for 3 source files)
# ---------------------------------------------------------------------------
BUILD_DIR="$WORK_DIR/build-arm64"
mkdir -p "$BUILD_DIR"

PAGE_SIZE_FLAG="-Wl,-z,max-page-size=16384"
DEFS=(
    -DLTDL_SHLIB_EXT=\".so\"
    -DHAVE_DLFCN_H=1
    -DHAVE_DLOPEN=1
    -DHAVE_LIBDL=1
    -DHAVE_DECL_DLERROR=1
    -DHAVE_DECL_DLOPEN=1
    -DHAVE_DECL_DLCLOSE=1
    -DHAVE_DECL_DLSYM=1
    -DLTDL_OBJDIR=\".libs\"
    -DLT_DLSEARCH_PATH=\"\"
    -DPACKAGE=\"ltdl\"
    -DPACKAGE_VERSION=\"2.5.4\"
    -D_REENTRANT=1
)

echo "[ltdl] Compiling object files"
"$CC" -c -fPIC -O2 -fvisibility=hidden \
    -I "$SRC_DIR" \
    "${DEFS[@]}" \
    -o "$BUILD_DIR/ltdl.o"        "$SRC_DIR/ltdl.c"
"$CC" -c -fPIC -O2 -fvisibility=hidden \
    -I "$SRC_DIR" \
    "${DEFS[@]}" \
    -o "$BUILD_DIR/lt__dirent.o"  "$SRC_DIR/lt__dirent.c"
"$CC" -c -fPIC -O2 -fvisibility=hidden \
    -I "$SRC_DIR" \
    "${DEFS[@]}" \
    -o "$BUILD_DIR/lt__strl.o"    "$SRC_DIR/lt__strl.c"

echo "[ltdl] Linking libltdl.so"
"$CC" -shared -fPIC "$PAGE_SIZE_FLAG" \
    "$BUILD_DIR/ltdl.o" \
    "$BUILD_DIR/lt__dirent.o" \
    "$BUILD_DIR/lt__strl.o" \
    -ldl \
    -o "$BUILD_DIR/libltdl.so"

# ---------------------------------------------------------------------------
# 4. Stage outputs
# ---------------------------------------------------------------------------
mkdir -p "$PREBUILT_OUT/lib" "$PREBUILT_OUT/include"
cp "$BUILD_DIR/libltdl.so" "$PREBUILT_OUT/lib/"
cp "$SRC_DIR/ltdl.h"       "$PREBUILT_OUT/include/"

echo "[ltdl] Output: $PREBUILT_OUT/lib/libltdl.so"
"$TOOLCHAIN/bin/llvm-objdump" -p "$PREBUILT_OUT/lib/libltdl.so" | grep LOAD
echo "[ltdl] Verify: each LOAD align must be 2**14 or higher."
