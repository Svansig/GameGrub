#!/usr/bin/env bash
set -euo pipefail

APK_PATH="${1:-app/build/outputs/apk/debug/app-debug.apk}"

if [[ ! -f "$APK_PATH" ]]; then
  echo "APK not found: $APK_PATH" >&2
  exit 1
fi

ROOT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")/.." && pwd)"

DEFAULT_SDK_ROOT="$HOME/Android/Sdk"

sdk_root_value() {
  if [[ -n "${ANDROID_HOME:-}" ]]; then
    echo "$ANDROID_HOME"
    return 0
  fi
  if [[ -n "${ANDROID_SDK_ROOT:-}" ]]; then
    echo "$ANDROID_SDK_ROOT"
    return 0
  fi
  if [[ -d "$DEFAULT_SDK_ROOT" ]]; then
    echo "$DEFAULT_SDK_ROOT"
    return 0
  fi
  echo ""
}

find_llvm_objdump() {
  if command -v llvm-objdump >/dev/null 2>&1; then
    command -v llvm-objdump
    return 0
  fi

  local ndk_objdump
  ndk_objdump="${ANDROID_NDK_HOME:-}/toolchains/llvm/prebuilt/linux-x86_64/bin/llvm-objdump"
  if [[ -x "$ndk_objdump" ]]; then
    echo "$ndk_objdump"
    return 0
  fi

  local sdk_root
  sdk_root="$(sdk_root_value)"
  if [[ -n "$sdk_root" ]]; then
    local candidate
    candidate="$(find "$sdk_root/ndk" -type f -path '*/toolchains/llvm/prebuilt/*/bin/llvm-objdump' 2>/dev/null | sort -V | tail -n 1 || true)"
    if [[ -n "$candidate" && -x "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  fi

  return 1
}

find_zipalign() {
  if command -v zipalign >/dev/null 2>&1; then
    command -v zipalign
    return 0
  fi

  local sdk_root
  sdk_root="$(sdk_root_value)"
  if [[ -n "$sdk_root" ]]; then
    local candidate
    candidate="$(find "$sdk_root/build-tools" -type f -name zipalign 2>/dev/null | sort -V | tail -n 1 || true)"
    if [[ -n "$candidate" && -x "$candidate" ]]; then
      echo "$candidate"
      return 0
    fi
  fi

  return 1
}

LLVM_OBJDUMP="$(find_llvm_objdump || true)"
if [[ -z "$LLVM_OBJDUMP" ]]; then
  echo "llvm-objdump not found. Install LLVM or set ANDROID_NDK_HOME/ANDROID_SDK_ROOT." >&2
  exit 1
fi

ZIPALIGN="$(find_zipalign || true)"
if [[ -z "$ZIPALIGN" ]]; then
  echo "zipalign not found. Install Android build-tools or set ANDROID_HOME/ANDROID_SDK_ROOT." >&2
  exit 1
fi

WORK_DIR="$(mktemp -d /tmp/gamegrub-16kb-check.XXXXXX)"
trap 'rm -rf "$WORK_DIR"' EXIT

unzip -q "$APK_PATH" -d "$WORK_DIR"

if [[ ! -d "$WORK_DIR/lib/arm64-v8a" ]]; then
  echo "No arm64-v8a libs found in APK: $APK_PATH" >&2
  exit 1
fi

echo "[16KB] Checking ELF LOAD alignment in $APK_PATH"

failures=()
while IFS= read -r -d '' lib; do
  rel="${lib#"$WORK_DIR/"}"
  echo "== $rel =="
  load_lines="$($LLVM_OBJDUMP -p "$lib" | grep 'LOAD' || true)"
  if [[ -z "$load_lines" ]]; then
    echo "  !! could not inspect LOAD segments"
    failures+=("$rel: unable to parse LOAD segments")
    continue
  fi

  echo "$load_lines"

  min_align="$(echo "$load_lines" | sed -nE 's/.*align 2\*\*([0-9]+).*/\1/p' | sort -n | head -n1)"
  if [[ -z "$min_align" ]]; then
    # Fallback for toolchains that print hex align values instead of 2**N.
    min_hex_align="$(readelf -W -l "$lib" | awk '/LOAD/ {print $NF}' | sed 's/^0x//' | sort | head -n1 || true)"
    if [[ -z "$min_hex_align" ]]; then
      failures+=("$rel: missing alignment metadata")
      continue
    fi
    if (( 16#$min_hex_align < 0x4000 )); then
      failures+=("$rel: align 0x$min_hex_align (< 0x4000)")
    fi
    continue
  fi

  if (( min_align < 14 )); then
    failures+=("$rel: align 2**$min_align (< 2**14)")
  fi
done < <(find "$WORK_DIR/lib/arm64-v8a" -maxdepth 1 -type f -name '*.so' -print0 | sort -z)

if (( ${#failures[@]} > 0 )); then
  echo
  echo "[16KB] FAILED ELF alignment checks:"
  printf ' - %s\n' "${failures[@]}"
  exit 1
fi

echo

echo "[16KB] Checking APK zip alignment"
"$ZIPALIGN" -v -c -P 16 4 "$APK_PATH"

echo "[16KB] PASS"



