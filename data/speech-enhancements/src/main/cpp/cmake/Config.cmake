include_guard(GLOBAL)

include(FetchContent)

if (POLICY CMP0135)
    cmake_policy(SET CMP0135 NEW)
endif ()

function(configure_project_target target_name)
    target_compile_features(${target_name} PRIVATE cxx_std_20)
    set_target_properties(${target_name} PROPERTIES CXX_EXTENSIONS OFF)

    # Section garbage collection is only meaningful for the Android/ELF toolchain; the host
    # linker (ld64) rejects these flags.
    if (ANDROID)
        target_link_options(${target_name} PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)
    endif ()

    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
        target_compile_options(${target_name} PRIVATE -Wall -Wextra -Wpedantic)
    endif ()
endfunction()


set(PFFFT_BUILD_TESTS OFF CACHE BOOL "Disable PFFFT tests" FORCE)
set(PFFFT_BUILD_BENCHMARKS OFF CACHE BOOL "Disable PFFFT benchmarks" FORCE)
set(PFFFT_BUILD_EXAMPLES OFF CACHE BOOL "Disable PFFFT examples" FORCE)
set(PFFFT_USE_TYPE_FLOAT ON CACHE BOOL "Build PFFFT single precision" FORCE)
set(PFFFT_USE_TYPE_DOUBLE OFF CACHE BOOL "Disable PFFFT double precision" FORCE)
set(INSTALL_PFFFT OFF CACHE BOOL "Disable PFFFT installation rules" FORCE)
set(PFFFT_VERSION "1.3.1" CACHE STRING "PFFFT version to fetch")

if (NOT TARGET PFFFT::PFFFT)
    FetchContent_Declare(
            pffft
            GIT_REPOSITORY https://github.com/marton78/pffft.git
            GIT_TAG "v${PFFFT_VERSION}"
            GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(pffft)
endif ()

if (ANDROID AND NOT TARGET onnxruntime)
    set(ONNXRUNTIME_ANDROID_VERSION "1.32.0" CACHE STRING "ONNX Runtime Android AAR version to fetch")
    set(_onnxruntime_aar_url https://repo.maven.apache.org/maven2/com/microsoft/onnxruntime/onnxruntime-android/${ONNXRUNTIME_ANDROID_VERSION}/onnxruntime-android-${ONNXRUNTIME_ANDROID_VERSION}.aar)
    set(_onnxruntime_checksum_url "${_onnxruntime_aar_url}.sha1")
    set(_onnxruntime_checksum_file "${CMAKE_BINARY_DIR}/onnxruntime-android-${ONNXRUNTIME_ANDROID_VERSION}.aar.sha1")
    file(DOWNLOAD "${_onnxruntime_checksum_url}" "${_onnxruntime_checksum_file}"
            STATUS _onnxruntime_checksum_status
            TLS_VERIFY ON
    )
    list(GET _onnxruntime_checksum_status 0 _onnxruntime_checksum_status_code)
    if (NOT _onnxruntime_checksum_status_code EQUAL 0)
        message(FATAL_ERROR
                "Could not retrieve ONNX Runtime ${ONNXRUNTIME_ANDROID_VERSION} AAR checksum from Maven Central: "
                "${_onnxruntime_checksum_status}")
    endif ()
    file(READ "${_onnxruntime_checksum_file}" _onnxruntime_checksum)
    string(STRIP "${_onnxruntime_checksum}" _onnxruntime_checksum)
    string(LENGTH "${_onnxruntime_checksum}" _onnxruntime_checksum_length)
    if (NOT _onnxruntime_checksum MATCHES "^[0-9a-fA-F]+$" OR NOT _onnxruntime_checksum_length EQUAL 40)
        message(FATAL_ERROR "Maven Central returned an invalid SHA-1 checksum for ONNX Runtime ${ONNXRUNTIME_ANDROID_VERSION}.")
    endif ()

    FetchContent_Declare(
            onnxruntime_android
            URL "${_onnxruntime_aar_url}"
            URL_HASH "SHA1=${_onnxruntime_checksum}"
    )
    FetchContent_MakeAvailable(onnxruntime_android)

    set(ONNXRUNTIME_INCLUDE_DIR "${onnxruntime_android_SOURCE_DIR}/headers")

    if (NOT ANDROID_ABI)
        message(FATAL_ERROR "ANDROID_ABI is not set. Configure this project through Android Gradle.")
    endif ()

    set(ONNXRUNTIME_LIBRARY "${onnxruntime_android_SOURCE_DIR}/jni/${ANDROID_ABI}/libonnxruntime.so")

    if (NOT EXISTS "${ONNXRUNTIME_INCLUDE_DIR}/onnxruntime_c_api.h")
        message(FATAL_ERROR "ONNX Runtime Android AAR did not contain headers/onnxruntime_c_api.h.")
    endif ()
    if (NOT EXISTS "${ONNXRUNTIME_LIBRARY}")
        message(FATAL_ERROR
                "ONNX Runtime Android AAR does not contain '${ANDROID_ABI}/libonnxruntime.so'. "
                "The native configuration requires an ABI supported by the AAR.")
    endif ()

    add_library(onnxruntime SHARED IMPORTED GLOBAL)
    set_target_properties(onnxruntime PROPERTIES
            IMPORTED_LOCATION "${ONNXRUNTIME_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${ONNXRUNTIME_INCLUDE_DIR}"
    )
endif ()

# Host: point DFN_ONNXRUNTIME_ROOT at an ONNX Runtime distribution (include/ + lib/).
if (NOT ANDROID AND NOT TARGET onnxruntime AND DFN_ONNXRUNTIME_ROOT)
    find_library(DFN_ONNXRUNTIME_LIBRARY
            NAMES onnxruntime
            PATHS "${DFN_ONNXRUNTIME_ROOT}/lib"
            NO_DEFAULT_PATH)
    if (NOT DFN_ONNXRUNTIME_LIBRARY)
        message(FATAL_ERROR "DFN_ONNXRUNTIME_ROOT is set but no libonnxruntime was found")
    endif ()
    add_library(onnxruntime SHARED IMPORTED GLOBAL)
    set_target_properties(onnxruntime PROPERTIES
            IMPORTED_LOCATION "${DFN_ONNXRUNTIME_LIBRARY}"
            INTERFACE_INCLUDE_DIRECTORIES "${DFN_ONNXRUNTIME_ROOT}/include")
endif ()


# Shared, module-independent test fixtures: reference loaders, stub runners and the
# generated numerical vectors. Test cases themselves live in each module's `test/`.
if (DFN_BUILD_TESTS)
    set(DFN_TEST_INCLUDE_DIR "${CMAKE_CURRENT_LIST_DIR}/../test" CACHE PATH "Shared test fixtures and helpers")
    set(DFN_TEST_MODEL_DIR "" CACHE PATH "Directory with the prepared streaming ONNX graphs")
    set(DFN_TEST_ENHANCER_DATA "" CACHE PATH "Directory with the enhancer reference vectors")
endif ()

if (DFN_BUILD_TESTS AND NOT TARGET GTest::gtest_main)
    set(INSTALL_GTEST OFF CACHE BOOL "Disable GoogleTest installation" FORCE)
    set(BUILD_GMOCK OFF CACHE BOOL "Disable GoogleMock" FORCE)
    FetchContent_Declare(
            googletest
            GIT_REPOSITORY https://github.com/google/googletest.git
            GIT_TAG v1.15.2
            GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(googletest)
endif ()
