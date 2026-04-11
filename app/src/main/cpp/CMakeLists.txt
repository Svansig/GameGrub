cmake_minimum_required(VERSION 3.22.1)

Project(GameGrub)

if (ANDROID_ABI STREQUAL "arm64-v8a")
    add_link_options("-Wl,-z,max-page-size=16384")
endif()

# ============================================================================
# Third-party source builds (16 KB page-size compliant)
#
# Repo root relative to this CMakeLists.txt is four directories up.
# ============================================================================
set(GAMEGRUB_REPO_ROOT "${CMAKE_CURRENT_SOURCE_DIR}/../../../..")

# --- libsndfile 1.2.2 (upstream CMake build via FetchContent) ---
# Produces: libsndfile.so  (replaces prebuilt in jniLibs/arm64-v8a/)
add_subdirectory(
    "${GAMEGRUB_REPO_ROOT}/third_party/libsndfile"
    "${CMAKE_CURRENT_BINARY_DIR}/third_party/libsndfile"
)
# sndfile's CMakeLists sets BUILD_SHARED_LIBS=ON in cache so FetchContent's
# child scope inherits it. Unset it immediately so sibling targets (adrenotools)
# stay STATIC — their CMakeLists omit an explicit library type and default to
# STATIC when BUILD_SHARED_LIBS is not set.
unset(BUILD_SHARED_LIBS CACHE)

# --- libltdl 2.5.4 (minimal source build from GNU Libtool via FetchContent) ---
# Produces: libltdl.so  (replaces prebuilt in jniLibs/arm64-v8a/)
add_subdirectory(
    "${GAMEGRUB_REPO_ROOT}/third_party/libltdl"
    "${CMAKE_CURRENT_BINARY_DIR}/third_party/libltdl"
)

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

#add_subdirectory(proot)
add_subdirectory(virglrenderer)
add_subdirectory(patchelf)
add_subdirectory(extras)

set(WINLATOR_SOURCES
        winlator/arraytools.c
        winlator/drawable.c
        winlator/gpu_helper.c
        winlator/gpu_image.c
        winlator/sysvshared_memory.c
        winlator/xconnector_epoll.c
        winlator/alsa_client.c
        winlator/wine_registry_editor.c
        winlator/patchelf_wrapper.cpp)

add_library(winlator SHARED ${WINLATOR_SOURCES})

add_library(winlator_11 SHARED ${WINLATOR_SOURCES})

target_link_libraries(winlator
                      log
                      android
                      jnigraphics
                      aaudio
                      EGL
                      GLESv2
                      GLESv3)

target_link_libraries(winlator_11
                      log
                      android
                      jnigraphics
                      aaudio
                      EGL
                      GLESv2
                      GLESv3)

