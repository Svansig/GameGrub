#!/usr/bin/env bash
# inspect-vortek.sh
# Investigation script for libvortekrenderer.so provenance.
#
# Searches Winlator source archives and known forks for "vortek" build artifacts
# or source files. Run this to assist with Phase 4 (producer recovery for
# libvortekrenderer.so).
#
# Current status: BLOCKED — no public source found.
# See: todo/REL-020.md
#
# Usage: bash build-scripts/android/inspect-vortek.sh

set -euo pipefail

REPO_ROOT="$(cd "$(dirname "${BASH_SOURCE[0]}")/../.." && pwd)"
LIB="$REPO_ROOT/app/src/main/jniLibs/arm64-v8a/libvortekrenderer.so"
NDK_HOME="${ANDROID_NDK_HOME:-}"
if [[ -z "$NDK_HOME" ]]; then
    NDK_HOME="$(find "$HOME/Android/Sdk/ndk" -maxdepth 1 -mindepth 1 -type d 2>/dev/null | sort -V | tail -1)"
fi
OBJDUMP="$NDK_HOME/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump"

echo "=== libvortekrenderer.so — ELF metadata ==="
"$OBJDUMP" -p "$LIB" | grep -E "LOAD|NEEDED|SONAME"

echo ""
echo "=== Embedded path strings (provenance clues) ==="
strings "$LIB" | grep -E "winlator|vortek|/data/data|build|commit|version" | head -40

echo ""
echo "=== Build-ID ==="
readelf -n "$LIB" 2>/dev/null | grep -A2 "Build ID" || echo "(no Build ID)"

echo ""
echo "=== Summary ==="
echo "  libvortekrenderer.so embeds /data/data/com.winlator/cache/vortek/ paths."
echo "  It was built specifically for the Winlator app (com.winlator package name)."
echo "  No public CMake/Meson/Android.mk build file for 'vortekrenderer' has been"
echo "  located in brunodev85/winlator or any known public Winlator fork."
echo ""
echo "  Potential sources to check manually:"
echo "    1. https://github.com/brunodev85/winlator  (browse all tags/releases)"
echo "    2. Winlator Patreon builds or private fork"
echo "    3. Winlator contributor contact"
echo ""
echo "  Until source is located, libvortekrenderer.so cannot be rebuilt with"
echo "  16 KB alignment. See todo/REL-020.md for the open ticket."
