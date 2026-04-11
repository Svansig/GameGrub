# Android 16 KB Native Page-Size Audit (2026-04-10)

## Status
Mostly complete — all libraries resolved except `libvortekrenderer.so`. PulseAudio stack (libpulse, libpulsecommon-13.0, libpulsecore-13.0, libpulseaudio) rebuilt from official PA 13.0 source via Meson + NDK r28 and verified at `0x4000` alignment (2026-04-10). Only `libvortekrenderer.so` remains blocked — no public source exists; contact required with original contributor.

**Crash mitigation applied 2026-04-10**: `libvortekrenderer.so` references the removed private API `ashmemCreateRegion` (dropped in Android API 35), causing a fatal `UnsatisfiedLinkError` on Android 15 devices. Three source changes were made to prevent this crash and add a graceful VirGL fallback — see "Crash Mitigation" section below.

## Root Cause
The app packaged a mixed set of arm64 native libraries:
- some source-built in this repo (or vendored submodule) that can be rebuilt with NDK r28+
- some checked-in prebuilts under `app/src/main/jniLibs/arm64-v8a/` with `LOAD` alignment `0x1000` (4 KB)

Google Play rejects the APK when any packaged arm64 library is not 16 KB-compatible.

## Library Source Mapping and Actions

| Library | Source path (current owner) | Build system | Action taken | Current result |
|---|---|---|---|---|
| `libdummyvk.so` | `app/src/main/cpp/extras/adrenotools/tools/acc-shim/vk_acc_shim.cpp` | CMake via `externalNativeBuild` | Added CMake target `dummyvk`; removed arm64 prebuilt | `0x4000` (pass) |
| `libevshim.so` | `app/src/main/cpp/extras/evshim.c` | CMake via `externalNativeBuild` | Replaced SDL include dependency with local ABI-compatible type declarations; rebuilt and packaged from CMake output | `0x4000` (pass) |
| `libextras.so` | `app/src/main/cpp/extras/gpu_image.c`, `app/src/main/cpp/extras/vulkan.c` | CMake via `externalNativeBuild` | Rebuilt via CMake; removed arm64 prebuilt | `0x4000` (pass) |
| `libhook_impl.so` | `app/src/main/cpp/extras/adrenotools/src/hook/hook_impl.cpp` | CMake via `externalNativeBuild` (submodule) | Rebuilt via CMake; removed arm64 prebuilt | `0x4000` (pass) |
| `libltdl.so` | `third_party/libltdl/libltdl_android.c` (minimal dlopen shim, in-repo) | CMake via `externalNativeBuild` / `add_subdirectory` | Added target `ltdl` in third_party/libltdl/CMakeLists.txt; removed arm64 prebuilt | `0x4000` (pass) |
| `libmain_hook.so` | `app/src/main/cpp/extras/adrenotools/src/hook/main_hook.c` | CMake via `externalNativeBuild` (submodule) | Rebuilt via CMake; removed arm64 prebuilt | `0x4000` (pass) |
| `libpatchelf.so` | `app/src/main/cpp/patchelf/src/patchelf.cc` | CMake via `externalNativeBuild` | Rebuilt via CMake; removed arm64 prebuilt | `0x4000` (pass) |
| `libpulse.so` | Official PulseAudio 13.0 tarball (freedesktop.org SHA-256: `961b23ca...`) | Meson + NDK r28 cross-compile (`build-scripts/android/build-pulseaudio.sh`) | Rebuilt from official PA 13.0 source; prebuilt replaced in jniLibs | `0x4000` (pass) |
| `libpulseaudio.so` | Official PulseAudio 13.0 tarball — daemon binary packaged as .so | Meson + NDK r28 cross-compile (`build-scripts/android/build-pulseaudio.sh`) | Rebuilt from official PA 13.0 source; prebuilt replaced in jniLibs | `0x4000` (pass) |
| `libpulsecommon-13.0.so` | Official PulseAudio 13.0 tarball | Meson + NDK r28 cross-compile (`build-scripts/android/build-pulseaudio.sh`) | Rebuilt from official PA 13.0 source; prebuilt replaced in jniLibs | `0x4000` (pass) |
| `libpulsecore-13.0.so` | Official PulseAudio 13.0 tarball | Meson + NDK r28 cross-compile (`build-scripts/android/build-pulseaudio.sh`) | Rebuilt from official PA 13.0 source; prebuilt replaced in jniLibs | `0x4000` (pass) |
| `libsndfile.so` | `third_party/libsndfile/CMakeLists.txt` (FetchContent → libsndfile 1.2.2 upstream) | CMake via `externalNativeBuild` / `add_subdirectory` + FetchContent | Added target `sndfile` in third_party/libsndfile/CMakeLists.txt; removed arm64 prebuilt | `0x4000` (pass) |
| `libvirglrenderer.so` | `app/src/main/cpp/virglrenderer/*` | CMake via `externalNativeBuild` | Rebuilt via CMake; removed arm64 prebuilt | `0x4000` (pass) |
| `libvortekrenderer.so` | Prebuilt: `app/src/main/jniLibs/arm64-v8a/libvortekrenderer.so` | Prebuilt only | No in-repo producer found | `0x1000` (fail) |
| `libwinlator.so` | `app/src/main/cpp/winlator/*` via target in `app/src/main/cpp/CMakeLists.txt` | CMake via `externalNativeBuild` | Fixed source blockers in `gpu_image.c` and `xconnector_epoll.c`; Gradle now packages source-built output | `0x4000` (pass) |
| `libwinlator_11.so` | `app/src/main/cpp/winlator/*` via target in `app/src/main/cpp/CMakeLists.txt` | CMake via `externalNativeBuild` | Added `winlator_11` source-built target and replaced prebuilt packaging | `0x4000` (pass) |

## Build/Config Changes Applied
- Re-enabled native source build wiring in `app/build.gradle.kts` (`externalNativeBuild` + selected CMake targets).
- Added arm64 linker page-size option (`-Wl,-z,max-page-size=16384`) for CMake tree in:
  - `app/src/main/cpp/CMakeLists.txt`
  - `app/src/main/cpp/extras/CMakeLists.txt`
- Added `dummyvk` CMake target in `app/src/main/cpp/extras/CMakeLists.txt`.
- Added `evshim` CMake target in `app/src/main/cpp/extras/CMakeLists.txt`.
- Expanded `winlator` CMake target sources in `app/src/main/cpp/CMakeLists.txt` so JNI symbol coverage is source-produced.
- Fixed `winlator` source blockers:
  - `app/src/main/cpp/winlator/gpu_image.c`: removed invalid decompiled block, added native handle type definition/includes, fixed format constant.
  - `app/src/main/cpp/winlator/xconnector_epoll.c`: removed duplicate JNI `closeFd` definition.
- Fixed `evshim` source blocker:
  - `app/src/main/cpp/extras/evshim.c`: replaced `SDL2/SDL.h` compile-time dependency with local declarations for dynamically loaded SDL API surface.
- Removed conflicting arm64 prebuilts now replaced by CMake outputs:
  - `libdummyvk.so`, `libevshim.so`, `libextras.so`, `libhook_impl.so`, `libmain_hook.so`, `libpatchelf.so`, `libvirglrenderer.so`, `libwinlator.so`, `libwinlator_11.so`

## Prebuilt Producer Tracing (remaining failing libs)

| Library | Enters build at | Original producer/source evidence | Source available in repo? | Can rebuild now? | Action / blocker |
|---|---|---|---|---|---|
| `libltdl.so` | `app/src/main/jniLibs/arm64-v8a/libltdl.so` (merged by `mergeDebugJniLibFolders`) | Imported as prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no associated producer scripts in repo | No | No | Blocked: add producer module (libtool/ltdl build) to CMake/Gradle graph |
| `libpulse.so` | `app/src/main/jniLibs/arm64-v8a/libpulse.so` | Imported prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no producer scripts tracked | No | No | Blocked: add PulseAudio client library producer |
| `libpulseaudio.so` | `app/src/main/jniLibs/arm64-v8a/libpulseaudio.so` | Imported prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no producer scripts tracked | No | No | Blocked: add PulseAudio server/daemon producer |
| `libpulsecommon-13.0.so` | `app/src/main/jniLibs/arm64-v8a/libpulsecommon-13.0.so` | Imported prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no producer scripts tracked | No | No | Blocked: add PulseAudio common lib producer |
| `libpulsecore-13.0.so` | `app/src/main/jniLibs/arm64-v8a/libpulsecore-13.0.so` | Imported prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no producer scripts tracked | No | No | Blocked: add PulseAudio core lib producer |
| `libsndfile.so` | `app/src/main/jniLibs/arm64-v8a/libsndfile.so` | Imported prebuilt in commit `bdd4b43a` / updated `dad82a8d`; no producer scripts tracked | No | No | Blocked: add libsndfile producer |
| `libvortekrenderer.so` | `app/src/main/jniLibs/arm64-v8a/libvortekrenderer.so` | Imported prebuilt in commit `9f635b03`, updated `eec1e9ad`, `e4e8331b`, `174b90d1` alongside `assets/graphics_driver/vortek-*.tzst` | No native producer in repo | No | Blocked: add vortekrenderer native producer or vendor reproducible build source |
| `libwinlator_11.so` | `app/src/main/jniLibs/arm64-v8a/libwinlator_11.so` | Introduced in commit `174b90d1` with `libvortekrenderer.so`; now replaced by in-repo source target (`winlator_11`) | Yes | Yes | Rebuilt via CMake and packaged from source output |

## Runtime Necessity Evidence (remaining failing libs)

| Library | Runtime evidence | Conclusion |
|---|---|---|
| `libltdl.so` | ELF dependency edge from `libpulseaudio.so` and `libpulsecore-13.0.so` (`readelf -d` shows `NEEDED [libltdl.so]`) | Required when PulseAudio path is active |
| `libpulse.so` | `PulseAudioComponent` launches `nativeLibraryDir + "/libpulseaudio.so"`; `libpulseaudio.so`/`libpulsecore-13.0.so` both `NEEDED [libpulse.so]` | Required when PulseAudio path is active |
| `libpulseaudio.so` | Direct process launch in `app/src/main/java/com/winlator/xenvironment/components/PulseAudioComponent.java` (`command = nativeLibraryDir+"/libpulseaudio.so"`) | Required for pulseaudio audio driver |
| `libpulsecommon-13.0.so` | `NEEDED` by `libpulse.so`, `libpulseaudio.so`, and `libpulsecore-13.0.so` | Required via PulseAudio dependency chain |
| `libpulsecore-13.0.so` | `NEEDED` by `libpulseaudio.so` | Required via PulseAudio dependency chain |
| `libsndfile.so` | `NEEDED` by `libpulse.so`, `libpulseaudio.so`, `libpulsecommon-13.0.so`, `libpulsecore-13.0.so` | Required via PulseAudio dependency chain |
| `libvortekrenderer.so` | `System.loadLibrary("vortekrenderer")` in `VortekRendererComponent`; component is wired in `EnvironmentSetupCoordinator` for `graphicsDriver` in `{vortek, adreno, sd-8-elite}`; default driver is `vortek` in `DefaultVersion` | Required in reachable/default graphics flow |
| `libwinlator_11.so` | `System.loadLibrary("winlator_11")` in both `GPUHelper` and `Drawable`; additionally `libvortekrenderer.so` has `NEEDED [libwinlator_11.so]` | Required and now rebuilt from source |

## Regression Gate Added
- Script: `scripts/verify-16kb-page-size.sh`
  - Extracts APK
  - Verifies arm64 ELF `LOAD` alignments with `llvm-objdump`
  - Verifies APK zip alignment with `zipalign -c -P 16 4`
- Gradle task: `:app:verifyDebug16KbPageSize`
- CI wiring:
  - `.github/workflows/pluvia-pr-check.yml`
  - `.github/workflows/app-release-signed.yml`
  - `.github/workflows/tagged-release.yml`

## Verification Snapshot (debug APK, 2026-04-10 — post Phase 3)
- Build: `./gradlew :app:assembleDebug` -> success
- ELF verification (`scripts/verify-16kb-page-size.sh`) -> fails for one remaining library:
  - `libvortekrenderer.so` — 2**12 (prebuilt, no public source found anywhere)
- Now PASSING (source-built or rebuilt): `libsndfile.so`, `libltdl.so`, `libpulse.so`, `libpulseaudio.so`, `libpulsecommon-13.0.so`, `libpulsecore-13.0.so`, and all previous source targets
- Zipalign verification (`zipalign -v -c -P 16 4 app/build/outputs/apk/debug/app-debug.apk`) -> pass

## Blockers to Full Completion
1. `libvortekrenderer.so` — **only remaining blocker**:
   - Proprietary binary contribution from Utkarsh Dalal (utkarshdalal / utkarsh.dalal@toptal.com)
   - Built with NDK r24 (build 8215888), minSdkVersion 26; statically links adrenotools + libc++ unwind tables
   - Embedded string: `/data/data/com.winlator/cache/vortek/` — Winlator-specific private source
   - **Investigation result**: official Winlator SourceForge archives for versions 9.0, 10.0, and 10.1 do not contain any Vortek source, prebuilt, or Java integration. Forks (SEGAINDEED, Succubussix) also lack Vortek source. Conclusion: **State D — SOURCE ARCHIVE APPEARS INCOMPLETE**. Vortek was always a closed binary contribution never committed to the public source tree.
   - patchelf cannot fix alignment: 3 tightly-packed LOAD segments (0x000000–0x091670) need ~192 KB padding; layout-sensitive internal references would be corrupted
   - Investigation script: `build-scripts/android/inspect-vortek.sh`
   - **Rebuild instruction** (for Utkarsh Dalal):
     ```bash
     # In the vortekrenderer CMakeLists.txt, add:
     target_link_options(vortekrenderer PRIVATE -Wl,-z,max-page-size=16384)
     # Rebuild with NDK r28: ANDROID_NDK_HOME=/path/to/ndk/28.2.13676358
     # Verify: readelf -Wl libvortekrenderer.so | grep LOAD  → all align 0x4000+
     # Also fix: replace ashmemCreateRegion with ASharedMemory_create() for API 35 compatibility
     ```
   - **Next action**: Contact Utkarsh Dalal with rebuild instruction; request binary drop to `app/src/main/jniLibs/arm64-v8a/`

## Producer Discovery Evidence (2026-04-10 continuation)

- Asset archive scan (`pulseaudio-gamenative.tzst`, `graphics_driver/vortek-*.tzst`) found runtime payloads but no buildable producer source trees for failing APK libs.
- Repo-history scan found repeated binary import/update commits for pulse/ltdl/sndfile/vortek artifacts, but no CMake/ndk-build/meson/autotools producer scripts.
- Upstream scans (`brunodev85/winlator` and sampled Winlator forks) did not expose producer source/build scripts for `libvortekrenderer.so` or the pulse/ltdl/sndfile APK prebuilts used here.
- Conclusion remains unchanged: remaining 7 libraries are blocked on missing producer source integration, not on packaging configuration.

## Final Blocked Table

| library | required at runtime? | packaging source | producer available? | removed or rebuilt? | blocker / next ticket |
|---|---|---|---|---|---|
| `libltdl.so` | Yes (NEEDED by libpulsecore-13.0.so) | `third_party/libltdl/libltdl_android.c` — minimal dlopen shim (9 lt_dl* symbols) | Yes — in-repo | **Rebuilt 2026-04-10** — CMake target `ltdl`, prebuilt deleted | `0x4000` (pass) |
| `libsndfile.so` | Yes (NEEDED by all pulse libs) | `third_party/libsndfile/CMakeLists.txt` — FetchContent libsndfile 1.2.2 upstream | Yes — fetched from upstream GitHub | **Rebuilt 2026-04-10** — CMake target `sndfile`, prebuilt deleted | `0x4000` (pass) |
| `libpulse.so` | Yes (NEEDED by libpulseaudio.so, libpulsecore) | Official PulseAudio 13.0 tarball (freedesktop.org) | Script: `build-scripts/android/build-pulseaudio.sh` | **Rebuilt 2026-04-10** — Meson + NDK r28, prebuilt replaced | `0x4000` (pass) |
| `libpulseaudio.so` | Yes (`PulseAudioComponent` direct launch) | Official PulseAudio 13.0 tarball — daemon binary renamed `.so` | Script: `build-scripts/android/build-pulseaudio.sh` | **Rebuilt 2026-04-10** — Meson + NDK r28, prebuilt replaced | `0x4000` (pass) |
| `libpulsecommon-13.0.so` | Yes (dependency graph) | Official PulseAudio 13.0 tarball | Script: `build-scripts/android/build-pulseaudio.sh` | **Rebuilt 2026-04-10** — Meson + NDK r28, prebuilt replaced | `0x4000` (pass) |
| `libpulsecore-13.0.so` | Yes (NEEDED by libpulseaudio.so) | Official PulseAudio 13.0 tarball | Script: `build-scripts/android/build-pulseaudio.sh` | **Rebuilt 2026-04-10** — Meson + NDK r28, prebuilt replaced | `0x4000` (pass) |
| `libvortekrenderer.so` | Yes (default graphics flow via `System.loadLibrary`) | No public source found; embeds `/data/data/com.winlator/cache/vortek/` paths. SourceForge archives 9.0/10.0/10.1 confirmed empty (State D). | No — contact Utkarsh Dalal | Not rebuilt — prebuilt remains | `0x1000` (fail) — see `todo/REL-020.md` |

Until those producers are restored or replaced with compliant artifacts, the Play rejection condition is not fully resolved.

## Crash Mitigation: ashmemCreateRegion on Android 15 (2026-04-10)

`libvortekrenderer.so` uses `ashmemCreateRegion` (private Android API removed in API 35). `dlopen` fails at load time with `UnsatisfiedLinkError`, crashing any session that triggers `VortekRendererComponent` class initialization.

### Changes Applied

| File | Change |
|------|--------|
| `VortekRendererComponent.java` | `System.loadLibrary` moved into guarded `loadNativeLibrary()` that catches `LinkageError`; `isAvailable()` static method added |
| `VortekConfigDialog.java` | Removed `VortekRendererComponent` import; `DEFAULT_VK_MAX_VERSION` now computed inline via `GPUHelper.vkMakeVersion(1,3,128)` — no longer triggers Vortek class init |
| `PresentExtension.java` | Replaced `VortekRendererComponent.destroyTexture(oldTexture)` call with `oldTexture::destroy` method reference — removes hard dependency that triggered Vortek class init |
| `EnvironmentSetupCoordinator.kt` | Vortek component instantiation guarded by `VortekRendererComponent.isAvailable()`; on failure logs warning and falls back to `VirGLRendererComponent` |

### Result
- No fatal crash on API 35+ when Vortek library fails to load
- Sessions with `graphicsDriver == vortek/adreno/sd-8-elite` automatically fall back to VirGL
- Vortek continues to work normally on devices where the library loads successfully

## Producer Integration Summary (2026-04-10)

### Completed
- `third_party/libsndfile/CMakeLists.txt`: FetchContent-based build of libsndfile 1.2.2
  - Pinned: `SHA256=3799ca9924d3125038880367bf1468e53a1b7e3686a934f098b7e1d286cdb80e`
  - Source: `https://github.com/libsndfile/libsndfile/releases/download/1.2.2/libsndfile-1.2.2.tar.xz`
- `third_party/libltdl/libltdl_android.c`: Minimal standalone ltdl shim (5 functions: lt_dlclose, lt_dlerror, lt_dlgetsearchpath, lt_dlopenext, lt_dlsym)
  - Confirmed that libpulsecore-13.0.so only uses these 5 symbols (verified via `strings`)
- `app/src/main/cpp/CMakeLists.txt`: Added `add_subdirectory` for both, with BUILD_SHARED_LIBS cache isolation
- `app/build.gradle.kts`: Added targets `sndfile`, `ltdl`; excluded prebuilts from jniLibs
- Prebuilts deleted: `app/src/main/jniLibs/arm64-v8a/libsndfile.so`, `libltdl.so`

### Completed: PulseAudio stack (2026-04-10 — Phase 3)
- Official PulseAudio 13.0 cross-compiled for Android arm64 using Meson + NDK r28.
- Script: `build-scripts/android/build-pulseaudio.sh` (documents all patches, flags, and verification steps)
- Android compatibility patches applied:
  - `src/pulsecore/atomic.h`: cast `void *` to `(unsigned long)(uintptr_t)p` in `pa_atomic_ptr_store`
  - `config.h`: unset `HAVE_CPUID_H` (x86-only) and `HAVE_SYS_CAPABILITY_H` (not in bionic)
  - `src/map-file`: removed `pa_glib_*` and `pa_simple_*` from version script (in separate .so files)
  - `src/daemon/ltdl-bind-now.c`: `#undef PA_BIND_NOW` for Android (vtable API not in our ltdl shim)
  - NDK sysroot stubs added: `libintl.h` (no-op gettext), `ltdl.h` (minimal types + declarations)
  - ltdl shim expanded with additional symbols used by pulseaudio daemon
  - Cross-file: `--undefined-version` linker flag for LLD version script leniency
- All 4 outputs verified at LOAD align 0x4000 in jniLibs.

### Pending (investigation only — no action)
- `build-scripts/android/inspect-vortek.sh`: Investigation helper for vortekrenderer provenance

### Remaining blocker
- `libvortekrenderer.so`: contact Utkarsh Dalal (utkarsh.dalal@toptal.com) with rebuild instruction in `todo/REL-020.md`

