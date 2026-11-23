cmake_minimum_required(VERSION 3.22.1)

project(enhancer_core LANGUAGES C CXX)

set(CMAKE_CXX_STANDARD 20)
set(CMAKE_CXX_STANDARD_REQUIRED ON)
set(CMAKE_CXX_EXTENSIONS OFF)
set(CMAKE_POSITION_INDEPENDENT_CODE ON)
set(BUILD_SHARED_LIBS OFF)

set(CMAKE_C_VISIBILITY_PRESET hidden)
set(CMAKE_CXX_VISIBILITY_PRESET hidden)
set(CMAKE_VISIBILITY_INLINES_HIDDEN ON)

include(cmake/Config.cmake)

# Tests are built from this same project: every module owns a `test/` directory and
# registers its own suites there. The Android build leaves DFN_BUILD_TESTS off.
if (DFN_BUILD_TESTS)
    enable_testing()
    include(GoogleTest)
endif ()

add_subdirectory(dsp)
add_subdirectory(enhancer_core)

# The JNI bridge only exists inside the Android package: it needs the NDK and the
# ONNX Runtime Android AAR.
if (ANDROID)
    add_subdirectory(enhancer_jni)
endif ()

