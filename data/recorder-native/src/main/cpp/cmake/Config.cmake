include_guard(GLOBAL)

# Configures compiler standards, extensions, and warning diagnostics for native targets.
# - Enforces C++20 standard across native code without compiler extensions.
# - Enables standard compiler warnings (-Wall, -Wextra, -Wpedantic) for Clang and GNU.
function(configure_native_target target_name)
    target_compile_features(${target_name} PRIVATE cxx_std_20)
    set_target_properties(${target_name} PROPERTIES CXX_EXTENSIONS OFF)
    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
        target_compile_options(${target_name} PRIVATE -Wall -Wextra -Wpedantic)
    endif ()
endfunction()


# Manages external third-party dependencies required by native targets.
# - Fetches Google Oboe repository pinned to stable release tag 1.11.0.
# - Uses shallow clone (GIT_SHALLOW TRUE) for fast, bit-for-bit reproducible builds.
# - Employs an idempotency guard (if NOT TARGET oboe) to avoid re-declaring or
#   re-configuring Oboe targets across repeated CMake configuration evaluations.
include(FetchContent)

if (NOT TARGET oboe)
    FetchContent_Declare(
            oboe
            GIT_REPOSITORY https://github.com/google/oboe.git
            GIT_TAG 1.11.0
            GIT_SHALLOW TRUE
    )
    FetchContent_MakeAvailable(oboe)
endif ()

# ==============================================================================
# SECTION 3: Shared Native Library Helper
# ==============================================================================
# Convenience helper function to define standardized shared native libraries.
# - Creates a SHARED library target from specified source files.
# - Applies standard compiler options via configure_native_target().
# - Configures public and private include directories.
# - Links public and private dependency libraries.
function(add_custom_native_library target_name)
    cmake_parse_arguments(
            ARG
            ""
            ""
            "SOURCES;PUBLIC_INCLUDE_DIRS;PRIVATE_INCLUDE_DIRS;PUBLIC_LIBRARIES;PRIVATE_LIBRARIES"
            ${ARGN}
    )

    add_library(${target_name} SHARED ${ARG_SOURCES})

    configure_native_target(${target_name})

    if (ARG_PUBLIC_INCLUDE_DIRS)
        target_include_directories(${target_name} PUBLIC ${ARG_PUBLIC_INCLUDE_DIRS})
    endif ()

    if (ARG_PRIVATE_INCLUDE_DIRS)
        target_include_directories(${target_name} PRIVATE ${ARG_PRIVATE_INCLUDE_DIRS})
    endif ()

    if (ARG_PUBLIC_LIBRARIES)
        target_link_libraries(${target_name} PUBLIC ${ARG_PUBLIC_LIBRARIES})
    endif ()

    if (ARG_PRIVATE_LIBRARIES)
        target_link_libraries(${target_name} PRIVATE ${ARG_PRIVATE_LIBRARIES})
    endif ()
endfunction()
