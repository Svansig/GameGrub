# third_party/libsndfile/CMakeLists.txt
#
# Fetches libsndfile 1.2.2 from upstream and builds a minimal shared library
# for Android arm64-v8a with NDK r28+ 16 KB page-size compliance.
#
# Producer: https://github.com/libsndfile/libsndfile
# Pinned release: 1.2.2  (2023-02-12)
# SHA-256: https://github.com/libsndfile/libsndfile/releases/download/1.2.2/libsndfile-1.2.2.tar.xz

cmake_minimum_required(VERSION 3.22.1)

include(FetchContent)

# libsndfile's SndFileChecks.cmake calls find_package(PythonInterp) to generate
# the version header. On systems where only python3 is in PATH, FindPythonInterp
# fails because it looks for the 'python' binary name.
# Pre-set PYTHON_EXECUTABLE so FindPythonInterp resolves without a search.
if(NOT PYTHON_EXECUTABLE)
    find_program(PYTHON_EXECUTABLE NAMES python3 python)
endif()

FetchContent_Declare(
    sndfile_src
    URL      https://github.com/libsndfile/libsndfile/releases/download/1.2.2/libsndfile-1.2.2.tar.xz
    URL_HASH SHA256=3799ca9924d3125038880367bf1468e53a1b7e3686a934f098b7e1d286cdb80e
)

# Build options — minimal shared lib, no optional codec deps, no tests/programs.
# BUILD_SHARED_LIBS must go into the CMake CACHE so FetchContent's child scope
# inherits it (CMP0077 / option() interplay in CMake 3.22 requires this).
# The main CMakeLists MUST call `unset(BUILD_SHARED_LIBS CACHE)` after
# `add_subdirectory(third_party/libsndfile)` to restore the default so that
# sibling targets like adrenotools stay STATIC.
set(BUILD_SHARED_LIBS      ON  CACHE BOOL "Build sndfile as shared lib" FORCE)
set(BUILD_EXAMPLES         OFF CACHE BOOL "" FORCE)
set(BUILD_TESTING          OFF CACHE BOOL "" FORCE)
set(ENABLE_CPACK           OFF CACHE BOOL "" FORCE)
set(ENABLE_PACKAGE_CONFIG  OFF CACHE BOOL "" FORCE)
set(INSTALL_PKGCONFIG_MODULE OFF CACHE BOOL "" FORCE)

# Disable optional codec backends to avoid pulling in system deps.
set(ENABLE_EXTERNAL_LIBS OFF CACHE BOOL "" FORCE)  # ogg/vorbis/flac/opus
set(ENABLE_MPEG          OFF CACHE BOOL "" FORCE)

FetchContent_MakeAvailable(sndfile_src)

# The upstream target is named 'sndfile'.
# Alias so parent CMake can use sndfile::sndfile.
if(NOT TARGET sndfile::sndfile)
    add_library(sndfile::sndfile ALIAS sndfile)
endif()
