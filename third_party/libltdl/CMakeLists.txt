# third_party/libltdl/CMakeLists.txt
#
# Minimal libltdl shim for Android arm64-v8a.
#
# PulseAudio 13.0 runtime expects this libltdl ABI surface:
#   lt_dlinit, lt_dlexit, lt_dlclose, lt_dlerror, lt_dlgetsearchpath,
#   lt_dlsetsearchpath, lt_dlopenext, lt_dlsym, lt_dlforeachfile
#
# Rather than cross-compiling the full GNU Libtool autotools tree (which
# requires configure-generated headers and argz portability code), this
# target compiles a minimal standalone shim (libltdl_android.c) that maps
# lt_dl* directly to Android bionic's dlopen family.
#
# Source: third_party/libltdl/libltdl_android.c (in-repo, hand-written)
# Reference producer (for provenance): GNU Libtool 2.5.4 libltdl
#   https://www.gnu.org/software/libtool/

cmake_minimum_required(VERSION 3.22.1)

add_library(ltdl SHARED
    ${CMAKE_CURRENT_SOURCE_DIR}/libltdl_android.c
)

target_link_libraries(ltdl dl)

# Keep symbols globally visible; PulseAudio resolves lt_* at runtime via the
# dynamic linker and fails to start if these are hidden.
target_compile_options(ltdl PRIVATE -O2)
