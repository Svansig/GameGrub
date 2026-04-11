cmake_minimum_required(VERSION 3.22.1)

Project(GameGrub)

if (ANDROID_ABI STREQUAL "arm64-v8a")
    add_link_options("-Wl,-z,max-page-size=16384")
endif()

set(CMAKE_C_FLAGS "${CMAKE_C_FLAGS} -O2 -Wno-unused-function -Wimplicit-function-declaration")

add_subdirectory(adrenotools)

add_library(extras SHARED
        gpu_image.c
        vulkan.c)

target_link_libraries(extras
        log
        android
        EGL
        GLESv2
        GLESv3
        adrenotools)

add_library(dummyvk SHARED
        adrenotools/tools/acc-shim/vk_acc_shim.cpp)

target_link_libraries(dummyvk
        log
        dl)

add_library(evshim SHARED
        evshim.c)

