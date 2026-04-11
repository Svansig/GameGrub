#!/usr/bin/env bash
# fetch-libltdl.sh
# Downloads GNU Libtool 2.5.4 and extracts the libltdl C sources into
# third_party/libltdl/src/ so the CMake build can compile them.
#
# Run once from the repo root before the first native build.
# The extracted files are small (~50 KB) and should be committed to the repo
# after running this script.
#
# Usage: bash build-scripts/android/fetch-libltdl.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
DEST="$REPO_ROOT/third_party/libltdl/src"
TARBALL_URL="https://ftpmirror.gnu.org/libtool/libtool-2.5.4.tar.gz"
TARBALL_SHA256="da8ebb2ce4dcf46b90098daf962cffa68f4b4f62ea60f798d0ef12929ede6adf"
TARBALL="/tmp/libtool-2.5.4.tar.gz"
EXTRACT_DIR="/tmp/libtool-2.5.4"

if [[ -f "$DEST/ltdl.c" ]]; then
    echo "[libltdl] Sources already present at $DEST, skipping download."
    exit 0
fi

echo "[libltdl] Downloading $TARBALL_URL"
curl -fsSL "$TARBALL_URL" -o "$TARBALL"

echo "[libltdl] Verifying SHA-256"
echo "$TARBALL_SHA256  $TARBALL" | sha256sum -c

echo "[libltdl] Extracting"
tar -xzf "$TARBALL" -C /tmp

mkdir -p "$DEST"

# Copy the core libltdl C sources and headers.
cp "$EXTRACT_DIR/libltdl/ltdl.c"         "$DEST/"
cp "$EXTRACT_DIR/libltdl/lt__dirent.c"   "$DEST/"
cp "$EXTRACT_DIR/libltdl/lt__strl.c"     "$DEST/"
cp "$EXTRACT_DIR/libltdl/ltdl.h"         "$DEST/"
cp "$EXTRACT_DIR/libltdl/lt__glibc.h"    "$DEST/" 2>/dev/null || true
cp "$EXTRACT_DIR/libltdl/lt__private.h"  "$DEST/"
cp "$EXTRACT_DIR/libltdl/lt_dlloader.h"  "$DEST/"
cp "$EXTRACT_DIR/libltdl/lt_system.h"    "$DEST/lt_system_upstream.h"

# Install the Android-specific lt_system.h override (pre-committed in the repo).
cp "$REPO_ROOT/third_party/libltdl/lt_system_android.h" "$DEST/lt_system.h"

echo "[libltdl] Sources staged at $DEST"
echo "[libltdl] Commit the contents of $DEST to the repo."
rm -f "$TARBALL"
rm -rf "$EXTRACT_DIR"
