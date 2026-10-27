include_guard(GLOBAL)

# Sets up safety rules, modern C++20, and size optimizations for a native target
function(configure_native_target target_name)
    # Force C++20 standard
    target_compile_features(${target_name} PRIVATE cxx_std_20)
    set_target_properties(${target_name} PROPERTIES CXX_EXTENSIONS OFF)

    # -Wl,--gc-sections: Removes unused code to shrink file size
    # --exclude-libs,ALL: Hides internal library code so only JNI functions are exposed
    target_link_options(${target_name} PRIVATE -Wl,--gc-sections -Wl,--exclude-libs,ALL)

    # Turn on standard compiler warnings to catch bugs early
    if (CMAKE_CXX_COMPILER_ID MATCHES "Clang|GNU")
        target_compile_options(${target_name} PRIVATE -Wall -Wextra -Wpedantic)
    endif ()
endfunction()


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


# A helper function to easily create shared libraries (.so files) without repeating code
function(add_custom_native_library target_name)
    # Parse the arguments passed into this function (sources, include dirs, libraries)
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
