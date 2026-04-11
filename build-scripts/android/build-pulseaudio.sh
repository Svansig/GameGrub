#!/usr/bin/env bash
# build-pulseaudio.sh
# Rebuilds PulseAudio 13.0 for Android arm64-v8a with 16 KB page-size alignment.
#
# Produces (staged to prebuilt-out/android/arm64-v8a/lib/):
#   libpulsecommon-13.0.so
#   libpulse.so
#   libpulsecore-13.0.so
#   libpulseaudio.so   (daemon binary packaged as .so per Android convention)
#
# Prerequisites:
#   - NDK r28+ installed; ANDROID_NDK_HOME must be set (or auto-detected).
#   - meson >= 1.0, ninja, m4, pkg-config, python3 installed.
#   - libsndfile.so and its sndfile.h must be already installed into the NDK
#     sysroot (done by build-libsndfile.sh or by the CMake FetchContent build).
#   - libltdl.so and ltdl.h must be installed into the NDK sysroot
#     (done by build-libltdl.sh or by the CMake source build).
#
# Source:
#   Official PulseAudio 13.0 tarball from freedesktop.org.
#   SHA-256: 961b23ca1acfd28f2bc87414c27bb40e12436efcf2158d29721b1e89f3f28057
#
# Android compatibility patches applied by this script:
#   1. config.h: unset HAVE_CPUID_H (x86-only) and HAVE_SYS_CAPABILITY_H (no libcap in bionic)
#   2. src/pulsecore/atomic.h: cast void* to (unsigned long)(uintptr_t) in pa_atomic_ptr_store
#   3. src/map-file: remove pa_glib_* and pa_simple_* (symbols live in separate .so files)
#   4. src/daemon/ltdl-bind-now.c: #undef PA_BIND_NOW for __ANDROID__ (vtable API not in our shim)
#   NDK sysroot stubs (installed separately): libintl.h (no-op gettext), ltdl.h (minimal API)
#
# Usage:
#   export ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358
#   bash build-scripts/android/build-pulseaudio.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
PREBUILT_OUT="$REPO_ROOT/prebuilt-out/android/arm64-v8a"
WORK_DIR="/tmp/pulseaudio-13.0-build"
PA_VERSION="13.0"
PA_TARBALL_URL="https://freedesktop.org/software/pulseaudio/releases/pulseaudio-${PA_VERSION}.tar.xz"
PA_SHA256="961b23ca1acfd28f2bc87414c27bb40e12436efcf2158d29721b1e89f3f28057"

NDK_HOME="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK_HOME" ]]; then
    NDK_HOME="$(find "$HOME/Android/Sdk/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -1)"
fi
if [[ -z "$NDK_HOME" || ! -d "$NDK_HOME" ]]; then
    echo "ERROR: ANDROID_NDK_HOME is not set and could not be auto-detected." >&2
    exit 1
fi
echo "[PA] Using NDK: $NDK_HOME"

TOOLCHAIN="$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64"
SYSROOT="$TOOLCHAIN/sysroot"
CLANG="$TOOLCHAIN/bin/aarch64-linux-android33-clang"
if [[ ! -x "$CLANG" ]]; then
    echo "ERROR: clang not found at $CLANG. Check NDK_HOME." >&2
    exit 1
fi

PAGE_SIZE_FLAG="-Wl,-z,max-page-size=16384"

# ---------------------------------------------------------------------------
# 1. Download and verify PA 13.0 tarball
# ---------------------------------------------------------------------------
mkdir -p "$WORK_DIR"
PA_TARBALL="$WORK_DIR/pulseaudio-${PA_VERSION}.tar.xz"
PA_SRC="$WORK_DIR/pulseaudio-${PA_VERSION}"

if [[ ! -f "$PA_TARBALL" ]]; then
    echo "[PA] Downloading PulseAudio ${PA_VERSION}"
    curl -L "$PA_TARBALL_URL" -o "$PA_TARBALL"
fi

echo "[PA] Verifying SHA-256"
echo "${PA_SHA256}  ${PA_TARBALL}" | sha256sum --check

if [[ ! -d "$PA_SRC" ]]; then
    echo "[PA] Extracting"
    tar -xJf "$PA_TARBALL" -C "$WORK_DIR"
fi

# ---------------------------------------------------------------------------
# 2. Apply Android compatibility patches to source
# ---------------------------------------------------------------------------
echo "[PA] Applying Android compatibility patches"

# Patch 1: atomic.h — void* → (unsigned long)(uintptr_t) in pa_atomic_ptr_store
sed -i \
    's/__atomic_store_n(&a->value, p, __ATOMIC_SEQ_CST)/__atomic_store_n(\&a->value, (unsigned long)(uintptr_t)p, __ATOMIC_SEQ_CST)/' \
    "$PA_SRC/src/pulsecore/atomic.h"

# Patch 2: map-file — remove symbols that live in separate .so files
sed -i '/^pa_glib_/d; /^pa_simple_/d' "$PA_SRC/src/map-file"

# Patch 3: ltdl-bind-now.c — disable vtable loader API on Android
if ! grep -q "__ANDROID__" "$PA_SRC/src/daemon/ltdl-bind-now.c"; then
    sed -i '/^#ifdef OS_IS_WIN32/{
n
/^#undef PA_BIND_NOW/{
a\
\
/* Android: bionic has RTLD_NOW but no ltdl vtable API. */\
#ifdef __ANDROID__\
#undef PA_BIND_NOW\
#endif
}
}' "$PA_SRC/src/daemon/ltdl-bind-now.c"
fi

# ---------------------------------------------------------------------------
# 3. Install NDK sysroot stubs (libintl.h, ltdl.h) if not already present
# ---------------------------------------------------------------------------
if [[ ! -f "$SYSROOT/usr/include/libintl.h" ]]; then
    echo "[PA] Installing libintl.h stub"
    cat > "$SYSROOT/usr/include/libintl.h" << 'INTLEOF'
/* Stub libintl.h for Android NDK — no-op translations */
#pragma once
#ifdef __cplusplus
extern "C" {
#endif
static inline const char *gettext(const char *s)                               { return s; }
static inline const char *dgettext(const char *d, const char *s)               { (void)d; return s; }
static inline const char *dcgettext(const char *d, const char *s, int c)       { (void)d; (void)c; return s; }
static inline const char *ngettext(const char *s, const char *p, unsigned long n) { return n == 1 ? s : p; }
static inline const char *dngettext(const char *d, const char *s, const char *p, unsigned long n) { (void)d; return n == 1 ? s : p; }
static inline const char *bindtextdomain(const char *d, const char *dir)      { (void)dir; return d; }
static inline const char *bind_textdomain_codeset(const char *d, const char *cs) { (void)cs; return d; }
static inline const char *textdomain(const char *d)                            { return d; }
#define gettext_noop(s) (s)
#ifdef __cplusplus
}
#endif
INTLEOF
fi

if [[ ! -f "$SYSROOT/usr/include/ltdl.h" ]]; then
    echo "[PA] Installing ltdl.h stub"
    cat > "$SYSROOT/usr/include/ltdl.h" << 'LTDLEOF'
/* Stub ltdl.h for Android NDK */
#pragma once
#include <stddef.h>
#ifdef __cplusplus
extern "C" {
#endif
typedef void *lt_dlhandle;
typedef void *lt_ptr;
typedef void *lt_module;
typedef struct { const char *name; lt_ptr address; } lt_dlsymlist;
extern const lt_dlsymlist lt_preloaded_symbols[];
#define LTDL_SET_PRELOADED_SYMBOLS() do {} while(0)
int          lt_dlinit(void);
int          lt_dlexit(void);
int          lt_dlclose(lt_dlhandle handle);
const char  *lt_dlerror(void);
const char  *lt_dlgetsearchpath(void);
int          lt_dlsetsearchpath(const char *search_path);
lt_dlhandle  lt_dlopenext(const char *filename);
void        *lt_dlsym(lt_dlhandle handle, const char *name);
typedef int (*lt_dlforeachfile_callback_t)(const char *filename, void *data);
int lt_dlforeachfile(const char *search_path, lt_dlforeachfile_callback_t func, void *data);
#define LTDL_SHLIB_EXT ".so"
#ifdef __cplusplus
}
#endif
LTDLEOF
fi

# ---------------------------------------------------------------------------
# 4. Write Meson cross-file
# ---------------------------------------------------------------------------
CROSS_FILE="$WORK_DIR/android-arm64.ini"
PKG_CONFIG_PATH_VAL="$SYSROOT/usr/lib/pkgconfig"
cat > "$CROSS_FILE" << CROSSEOF
[binaries]
c       = '$CLANG'
cpp     = '$TOOLCHAIN/bin/aarch64-linux-android33-clang++'
ar      = '$TOOLCHAIN/bin/llvm-ar'
strip   = '$TOOLCHAIN/bin/llvm-strip'
pkg-config = 'pkg-config'

[host_machine]
system     = 'android'
cpu_family = 'aarch64'
cpu        = 'armv8'
endian     = 'little'

[built-in options]
c_args      = ['-fPIC', '$PAGE_SIZE_FLAG', '--sysroot=$SYSROOT']
c_link_args = ['$PAGE_SIZE_FLAG', '--sysroot=$SYSROOT', '-L$SYSROOT/usr/lib/aarch64-linux-android/33', '-Wl,--undefined-version']
pkg_config_path = ['$PKG_CONFIG_PATH_VAL']
CROSSEOF

# ---------------------------------------------------------------------------
# 5. Configure with Meson
# ---------------------------------------------------------------------------
BUILD_DIR="$WORK_DIR/build-arm64"
rm -rf "$BUILD_DIR"
export PKG_CONFIG_PATH="$PKG_CONFIG_PATH_VAL"

echo "[PA] Configuring Meson build"
meson setup "$BUILD_DIR" "$PA_SRC" \
    --cross-file="$CROSS_FILE" \
    --buildtype=release \
    -Dalsa=disabled -Dasyncns=disabled -Davahi=disabled -Dbluez5=false \
    -Ddbus=disabled -Dfftw=disabled -Dglib=disabled -Dgsettings=disabled \
    -Dgtk=disabled -Dhal-compat=false -Dipv6=false -Djack=disabled \
    -Dlirc=disabled -Dopenssl=disabled -Dorc=disabled -Dsamplerate=disabled \
    -Dsoxr=disabled -Dspeex=disabled -Dsystemd=disabled -Dudev=disabled \
    -Dx11=disabled -Dadrian-aec=true -Dwebrtc-aec=disabled \
    -Ddatabase=simple -Dman=false -Dtests=false

# ---------------------------------------------------------------------------
# 6. Patch generated config.h to disable x86-only / bionic-missing features
# ---------------------------------------------------------------------------
echo "[PA] Patching generated config.h"
sed -i \
    's/#define HAVE_CPUID_H 1/\/* #undef HAVE_CPUID_H -- x86-only *\//' \
    "$BUILD_DIR/config.h"
sed -i \
    's/#define HAVE_SYS_CAPABILITY_H 1/\/* #undef HAVE_SYS_CAPABILITY_H -- not in bionic *\//' \
    "$BUILD_DIR/config.h"

# ---------------------------------------------------------------------------
# 7. Build
# ---------------------------------------------------------------------------
echo "[PA] Building"
ninja -C "$BUILD_DIR" -j"$(nproc)"

# ---------------------------------------------------------------------------
# 8. Verify alignment and stage outputs
# ---------------------------------------------------------------------------
echo "[PA] Verifying 16 KB alignment"
ALL_PASS=true
for f in \
    "$BUILD_DIR/src/pulse/libpulse.so" \
    "$BUILD_DIR/src/libpulsecommon-${PA_VERSION}.so" \
    "$BUILD_DIR/src/pulsecore/libpulsecore-${PA_VERSION}.so" \
    "$BUILD_DIR/src/daemon/pulseaudio"; do
    align=$(readelf -Wl "$f" | awk '/LOAD/{print $NF}' | sort -u | head -1)
    echo "  $(basename $f): $align"
    if [[ "$align" != "0x4000" ]]; then
        echo "  ERROR: expected 0x4000, got $align" >&2
        ALL_PASS=false
    fi
done

if [[ "$ALL_PASS" != "true" ]]; then
    echo "ERROR: alignment check failed." >&2
    exit 1
fi

echo "[PA] Staging outputs"
mkdir -p "$PREBUILT_OUT/lib"
cp "$BUILD_DIR/src/pulse/libpulse.so"                        "$PREBUILT_OUT/lib/"
cp "$BUILD_DIR/src/libpulsecommon-${PA_VERSION}.so"          "$PREBUILT_OUT/lib/"
cp "$BUILD_DIR/src/pulsecore/libpulsecore-${PA_VERSION}.so"  "$PREBUILT_OUT/lib/"
cp "$BUILD_DIR/src/daemon/pulseaudio"                        "$PREBUILT_OUT/lib/libpulseaudio.so"

echo ""
echo "[PA] Build complete. Next steps:"
echo "  1. Copy outputs from prebuilt-out/ to app/src/main/jniLibs/arm64-v8a/"
echo "  2. Run: ./gradlew :app:verifyDebug16KbPageSize"
