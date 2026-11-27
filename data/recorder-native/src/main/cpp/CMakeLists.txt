cmake_minimum_required(VERSION 3.22.1)

project(CustomAudioRecorder LANGUAGES C CXX)

# With CMake 3.22 the NDK's legacy toolchain path resets CMAKE_SYSTEM_VERSION
# to 1 (see ndk/build/cmake/android-legacy.toolchain.cmake), even though the
# -DCMAKE_SYSTEM_VERSION=29 AGP passes reflects the real min API. Restore the
# true API level so dependencies (e.g. libFLAC) gate 32-bit APIs correctly.
if (ANDROID AND ANDROID_PLATFORM)
    string(REGEX REPLACE "^android-" "" ANDROID_API_LEVEL "${ANDROID_PLATFORM}")
    set(CMAKE_SYSTEM_VERSION "${ANDROID_API_LEVEL}")
    unset(ANDROID_API_LEVEL)
endif ()

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)

# strips everything which is not needed
set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

# Thin link-time optimization, enabled per-target in configure_native_target. The
# explicit link option avoids CMake 3.22's IPO default injecting -fuse-ld=gold,
# which the NDK dropped in r22.
set(CMAKE_C_LINK_OPTIONS_IPO "-flto=thin")
set(CMAKE_CXX_LINK_OPTIONS_IPO "-flto=thin")

include(cmake/Config.cmake)

add_subdirectory(recorder_common)
add_subdirectory(custom_wav)
add_subdirectory(custom_flac)